# Redis GEO TTL/Cleanup 레이스 컨디션: 선박 위치 0건 → 17,000+건 유지

## 배경 및 문제정의

AIS 수집 파이프라인이 Kafka → `AisRedisConsumer` → Redis GEO(`vessels:positions`)로 선박 위치를 저장하고, `RedisGeoCleanupScheduler`가 오래된 데이터를 주기적으로 정리하는 구조였다. 데이터가 저장되자마자 사라지는 현상이 발생했다.

`AisRedisConsumer`는 GEO 저장 후 `vessels:timestamp:{mmsi}`에 TTL 30초를 설정. `RedisGeoCleanupScheduler`는 1분마다 실행되어 timestamp 키가 만료된 MMSI를 GEO에서 `ZREM`으로 삭제. **TTL(30초) < Cleanup 주기(60초)** 이므로, Cleanup 실행 시점에 거의 모든 timestamp가 만료 상태 → GEO 전체 삭제.

## 기술 선정 (대안 비교)

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| TTL 제거 (영구 보관) | 삭제 문제 없음 | 메모리 무한 증가, 비활성 선박 잔류 | X |
| TTL을 Cleanup 주기보다 길게 설정 | 간단, 레이스 제거 | TTL/주기 비율 관리 필요 | **O** |
| Cleanup에서 TTL 잔여시간 확인 후 삭제 | 정밀한 제어 | `TTL` 명령 N번 호출 → O(N) 부하 | X |
| Redis Keyspace Notification으로 이벤트 기반 삭제 | 실시간 정리 | 설정 복잡, pub/sub 신뢰성 이슈 | X |

## 분석

### 초기 판단

"수집기가 데이터를 안 보내는 것 아닌가?" → Kafka Consumer 로그 확인 시 정상 consume. Redis `MONITOR` 명령으로 실시간 확인 시 `GEOADD` 직후 `ZREM`이 따라오는 패턴 발견.

### 실제 원인: TTL과 Cleanup 주기의 타이밍 역전

```
Timeline:
T=0s   AisRedisConsumer: GEOADD vessels:positions ... mmsi_123
T=0s   AisRedisConsumer: SET vessels:timestamp:mmsi_123 EX 30
T=30s  Redis: DEL vessels:timestamp:mmsi_123 (TTL 만료, 자동 삭제)
T=60s  Scheduler: SCAN vessels:timestamp:* → mmsi_123 없음 → ZREM vessels:positions mmsi_123
       ↑ 30초 전에 만료됐으므로 "비활성"으로 판단 → 삭제
```

### CS 원리: TTL, Eviction, Redis GEO 내부 구조

**TTL과 Eviction 전략**: Redis는 키 만료를 두 가지 방식으로 처리한다. (1) **Lazy Expiration** - 클라이언트가 키에 접근할 때 만료 확인 후 삭제. (2) **Active Expiration** - 매 100ms마다 랜덤 20개 키를 샘플링하여 만료된 키 삭제. 만료된 키가 25% 이상이면 즉시 반복. 이 확률적 방식 때문에 TTL 만료 후 실제 삭제까지 수백ms 지연이 있을 수 있으나, `EXISTS` 검사 시에는 만료된 키를 즉시 없는 것으로 처리한다.

**Redis GEO 내부 구조**: `GEOADD`는 내부적으로 Sorted Set(`ZADD`)으로 구현된다. 좌표 `(longitude, latitude)`를 52비트 Geohash로 인코딩하여 score로 저장한다.

```
GEOADD vessels:positions 129.05 35.10 "mmsi_440123456"
→ 내부: ZADD vessels:positions 4065562751.0 "mmsi_440123456"
                                ↑ geohash(129.05, 35.10)의 52비트 정수값
```

Geohash는 Z-order curve(Morton code)를 사용하여 2D 좌표를 1D 정수로 매핑한다. 인접한 좌표는 인접한 정수값을 가지므로 `GEORADIUS`가 Sorted Set의 range query로 O(log(N)+M) 효율을 달성한다.

**Race Condition**: 이 문제는 전통적인 멀티스레드 레이스 컨디션과 다르다. 두 개의 독립된 타이머(TTL 만료, Scheduler 실행)가 **공유 자원(GEO 키)**에 대해 서로 다른 시점에 동작하면서 발생하는 **temporal race condition**이다. 단일 스레드 Redis에서도 클라이언트 측 스케줄링 타이밍 차이로 발생한다.

**Stale Data vs Freshness 트레이드오프**: TTL을 짧게 하면 freshness가 높지만 데이터 소실 위험 증가. TTL을 길게 하면 stale data가 남지만 가용성 증가. AIS 위치 데이터는 5분 이내면 "실시간"으로 간주되므로 TTL 5분이 적절한 균형점이다.

## 솔루션

TTL을 Cleanup 주기보다 충분히 길게 설정하여 레이스 윈도우를 제거.

```java
// AisRedisConsumer.java - TTL 5분으로 변경
private static final Duration POSITION_TTL = Duration.ofMinutes(5);

private void saveToRedisGeo(AisPositionMessage message) {
    Point point = new Point(message.getLongitude(), message.getLatitude());
    redisTemplate.opsForGeo()
        .add("vessels:positions", point, String.valueOf(message.getMmsi()));

    // timestamp 키에 TTL 5분 설정 (cleanup 주기 6분보다 짧지만 충분한 여유)
    String timestampKey = "vessels:timestamp:" + message.getMmsi();
    redisTemplate.opsForValue()
        .set(timestampKey, String.valueOf(Instant.now().toEpochMilli()), POSITION_TTL);
}
```

```java
// RedisGeoCleanupScheduler.java - 주기 6분으로 변경
@Scheduled(fixedRate = 360_000)  // 6분 (TTL 5분 + 1분 여유)
public void cleanupStalePositions() {
    Set<String> activeKeys = redisTemplate.keys("vessels:timestamp:*");
    Set<String> geoMembers = redisTemplate.opsForZSet()
        .range("vessels:positions", 0, -1);

    if (geoMembers == null) return;

    Set<String> activeMmsiSet = activeKeys.stream()
        .map(key -> key.replace("vessels:timestamp:", ""))
        .collect(Collectors.toSet());

    // timestamp가 만료된(=키가 없는) MMSI만 GEO에서 제거
    geoMembers.stream()
        .filter(mmsi -> !activeMmsiSet.contains(mmsi))
        .forEach(mmsi -> redisTemplate.opsForGeo()
            .remove("vessels:positions", mmsi));
}
```

핵심 공식: **TTL < Cleanup 주기 < TTL + 수집 간격**. 수집이 TTL 내에 갱신하면 timestamp가 리프레시되므로 활성 선박은 절대 삭제되지 않는다.

## 결과 (Before / After)

| 지표 | Before | After |
|------|--------|-------|
| GEO TTL | 30초 | 5분 |
| Cleanup 주기 | 1분 | 6분 |
| `GEORADIUS` 조회 결과 | 0척 | 17,000+척 |
| 비활성 선박 정리 | 전부 삭제 (과잉 정리) | 5분 이상 미갱신 선박만 정리 |
| 데이터 freshness | N/A (데이터 없음) | 5분 이내 실시간 |
