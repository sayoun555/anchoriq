## 수집기 런타임 제어 API 설계: 서버 재시작 필수 → REST API로 실시간 on/off 제어

### 배경 및 문제정의
- 상황: AnchorIQ는 11개 외부 API(AIS, 날씨, 뉴스, 유가, 환율, 제재, 지정학, 항만 혼잡도 등)에서 데이터를 수집한다. 서버가 시작되면 `@Scheduled`로 등록된 전체 수집기가 자동으로 실행되었다.
- 문제:
  - **개발 중 불필요한 API 호출**: 로컬 개발 시 프론트엔드 UI만 작업해도 11개 수집기가 전부 돌아가면서 외부 API를 호출했다.
  - **무료 API 할당량 낭비**: GNews(일 100건), OpenWeatherMap(일 1,000건) 등 무료 플랜의 할당량이 개발 중 소진되었다.
  - **데모 시 제어 불가**: 투자자/면접관 데모 시 특정 수집기만 켜서 보여주고 싶어도, 서버를 재시작하거나 코드를 수정해야 했다.
  - **끄는 방법이 없음**: `@Scheduled` 어노테이션은 동적으로 비활성화할 수 없다. 서버를 내리거나 코드를 주석 처리하는 것이 유일한 방법이었다.

### 기술 선정 (대안 비교 테이블)

| 항목 | 하드코딩 on/off | 환경변수 제어 | REST API 런타임 제어 |
|------|----------------|-------------|---------------------|
| 재시작 필요 | O (코드 수정) | O (재시작 필수) | X (즉시 반영) |
| 세밀한 제어 | X (전체 on/off) | 수집기별 가능 | 수집기별 + 전체 on/off |
| 데모 대응 | 불가 | 불가 (재시작) | 즉시 대응 가능 |
| 상태 모니터링 | 불가 | 불가 | 마지막 실행 시각, 결과, 스케줄 조회 |
| 구현 복잡도 | 낮음 | 낮음 | 중간 (Registry + API) |
| 스레드 안전성 | 해당 없음 | 해당 없음 | ConcurrentHashMap 필요 |

**선택: REST API 런타임 제어**. 환경변수 방식은 재시작이 필요하므로 데모 시나리오를 충족하지 못한다. REST API + CollectorRegistry 조합으로 런타임에 수집기를 개별/전체 제어할 수 있게 설계했다.

### 분석 / CS 원리

**Registry 패턴 + Command 패턴**

수집기 제어 문제의 핵심은 "스케줄링과 실행을 분리"하는 것이다:
- `@Scheduled`는 **트리거**만 담당 (언제 실행할지)
- `CollectorRegistry`는 **게이트키퍼** 역할 (실행 여부 판단)
- 실행은 `executeIfEnabled()` 패턴으로 감싼다

**ConcurrentHashMap을 선택한 이유**: 수집기 상태는 여러 스레드에서 동시에 접근할 수 있다. `@Scheduled` 스레드풀의 작업자 스레드와 REST API 요청 스레드가 동시에 상태를 읽고 쓸 수 있으므로, `ConcurrentHashMap.compute()`로 원자적 업데이트를 보장한다. `synchronized` 블록 대비 lock contention이 낮고, 읽기 작업은 lock-free로 동작한다.

**@ConditionalOnBean 활용**: Kafka가 비활성화된 환경에서는 CollectorRegistry Bean 자체가 생성되지 않으므로, `@ConditionalOnBean(CollectorRegistry.class)`로 Controller와 Scheduler도 자동으로 비활성화된다. 이는 Profile별로 불필요한 Bean을 수동으로 제외하는 것보다 선언적이고 안전하다.

### 솔루션 (핵심 코드 블록)

**1. CollectorRegistry 인터페이스** (core 모듈 — 도메인 레이어):

```java
// domain/operation/collector/service/CollectorRegistry.java
public interface CollectorRegistry {
    void start(CollectorName name);
    void stop(CollectorName name);
    void startAll();
    void stopAll();
    CollectorStatus getStatus(CollectorName name);
    List<CollectorStatus> getAllStatuses();
    boolean isEnabled(CollectorName name);
    void recordResult(CollectorName name, CollectorResult result);
}
```

**2. ConcurrentHashMap 기반 구현체** (collector 모듈):

```java
// collector/registry/CollectorRegistryImpl.java
public class CollectorRegistryImpl implements CollectorRegistry {

    private final AisStreamClient aisStreamClient;
    private final ConcurrentHashMap<CollectorName, CollectorStatus> statusMap;

    public CollectorRegistryImpl(AisStreamClient aisStreamClient,
                                 CollectorAutoStartProperties autoStartProperties,
                                 Map<CollectorName, String> scheduleMap) {
        this.aisStreamClient = aisStreamClient;
        this.statusMap = initializeStatuses(autoStartProperties, scheduleMap);
        applyAutoStart(autoStartProperties);
    }

    @Override
    public void start(CollectorName name) {
        // ConcurrentHashMap.compute()로 원자적 상태 변경
        statusMap.compute(name, (key, status) -> status.withEnabled(true));
        if (name.isAis()) {
            aisStreamClient.connect(); // AIS는 WebSocket 연결까지 제어
        }
    }

    @Override
    public void stop(CollectorName name) {
        statusMap.compute(name, (key, status) -> status.withEnabled(false));
        if (name.isAis()) {
            aisStreamClient.disconnect();
        }
    }

    @Override
    public boolean isEnabled(CollectorName name) {
        CollectorStatus status = statusMap.get(name);
        return status != null && status.enabled();
    }
}
```

**3. executeIfEnabled 패턴** (Scheduler에서 게이트 역할):

```java
// collector/scheduler/CollectorScheduler.java
@Scheduled(cron = "${collector.schedule.weather:0 0 * * * *}")
public void collectWeather() {
    executeIfEnabled(CollectorName.WEATHER, weatherCollector::collect);
}

private void executeIfEnabled(CollectorName name, Runnable task) {
    if (!collectorRegistry.isEnabled(name)) {
        log.debug("Collector [{}] is disabled, skipping", name.value());
        return; // 비활성 수집기는 즉시 리턴
    }
    log.info("Scheduled: {} collection", name.value());
    try {
        task.run();
        collectorRegistry.recordResult(name, CollectorResult.SUCCESS);
    } catch (Exception e) {
        log.error("Collector [{}] failed: {}", name.value(), e.getMessage());
        collectorRegistry.recordResult(name, CollectorResult.FAILED);
    }
}
```

**4. REST API 6개 엔드포인트** (Admin Controller):

```java
// controller/admin/CollectorAdminController.java
@RestController
@RequestMapping("/api/admin/collectors")
@ConditionalOnBean(CollectorRegistry.class)
public class CollectorAdminController {

    @PostMapping("/{name}/start")   // 개별 수집기 시작
    @PostMapping("/{name}/stop")    // 개별 수집기 중지
    @GetMapping("/status")          // 전체 상태 조회
    @GetMapping("/{name}/status")   // 개별 상태 조회
    @PostMapping("/start-all")      // 전체 시작
    @PostMapping("/stop-all")       // 전체 중지
}
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| 수집기 제어 방법 | 서버 재시작 또는 코드 수정 | REST API 6개로 실시간 제어 |
| 수집기 상태 모니터링 | 불가 (로그만) | 마지막 실행 시각, 결과, 스케줄 조회 |
| 개발 중 API 할당량 소진 | 빈번 (전 수집기 자동 실행) | 필요한 수집기만 선택적 기동 |
| 데모 시 대응 시간 | 수분 (서버 재시작) | 즉시 (API 1회 호출) |
| 스레드 안전성 | 해당 없음 (제어 불가) | ConcurrentHashMap으로 원자적 상태 관리 |
| Kafka 비활성 환경 호환 | 예외 발생 가능 | @ConditionalOnBean으로 자동 비활성화 |
