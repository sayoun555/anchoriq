## 항만 혼잡도: 외부 크롤링 의존 -> AIS 자체 계산 + UNCTAD 기준선 이중 구조

### 배경 및 문제정의
- 상황: UNCTAD Port Tracker는 분기 통계만 제공하며, 실시간 혼잡도 데이터가 아님. Playwright 기반 크롤링은 봇 차단에 취약하고 페이지 구조 변경 시 즉시 실패함.
- 문제: 실시간 혼잡도를 외부 크롤링에 전적으로 의존하면, 데이터 업데이트 주기(1일)가 길고 크롤링 실패 시 혼잡도 정보가 완전히 단절됨. 항만 혼잡도는 공급망 리스크 판단의 핵심 지표로, 10분 단위 갱신이 필요함.

### 기술 선정 (대안 비교 테이블)

| 항목 | UNCTAD 크롤링 (Before) | MarineTraffic API | AIS 자체 계산 (After) |
|------|----------------------|-------------------|---------------------|
| 실시간성 | X (분기 통계) | O (유료 API) | O (10분 갱신) |
| 안정성 | 낮음 (봇 차단, 구조 변경) | 높음 (공식 API) | 높음 (자체 데이터) |
| 비용 | 무료 | 유료 ($500+/월) | 무료 (AISstream 무료 티어) |
| 외부 의존도 | 높음 | 높음 | 낮음 (자체 엔진) |
| 포폴 어필 | 낮음 (단순 크롤링) | 낮음 (API 호출) | 높음 (자체 혼잡도 엔진) |
| 데이터 정확도 | 분기 평균값 | 실시간 | 실시간 선박 위치 기반 |

**선택: AIS 자체 계산 + UNCTAD 기준선 이중 구조**
- AIS 데이터는 이미 수집/저장 중이므로 추가 비용 없음
- UNCTAD는 크롤링 대신 통계 다운로더로 전환, 분기 1회만 실행하여 기준선 역할
- 두 계층을 결합하여 "실시간 혼잡도 + 평균 대비 비율"이라는 복합 지표 제공

### 분석

기존 방식은 Playwright로 UNCTAD 웹사이트를 크롤링하여 항만 혼잡도를 수집했다. 이 방식의 근본적 한계:

1. **데이터 지연**: UNCTAD 통계는 분기 단위로 갱신됨. 크롤링 주기를 1일로 설정해도 원본 데이터 자체가 오래된 값
2. **봇 차단**: UNCTAD 사이트가 Cloudflare 등 WAF를 적용하면 크롤링 즉시 실패
3. **구조 의존성**: HTML 테이블 구조가 변경되면 파서가 깨짐

반면 AIS 데이터는 이미 AISstream WebSocket으로 실시간 수신되어 Redis GEO에 저장되고 있었다. 이 데이터에는 선박의 위치(좌표)와 항해 상태(NavigationalStatus: ANCHORED=1, MOORED=5)가 포함된다. 항만 좌표 반경 5km 이내의 ANCHORED/MOORED 선박 수를 세면 곧 혼잡도가 된다.

Redis GEO의 GEORADIUS 명령은 O(N+log(M)) 복잡도로 공간 검색을 수행하므로, 수만 개 선박 위치에서도 밀리초 단위로 반경 내 선박을 조회할 수 있다.

### 솔루션

**이중 구조 아키텍처:**

```
계층 1: 실시간 (10분 갱신)
  AIS WebSocket -> Kafka -> Redis GEO (vessels:positions)
                                 |
  Scheduler (10분) -> PortCongestionCalculatorImpl
                          |
                          +-- GEORADIUS(항만좌표, 5km) -> 인근 선박 조회
                          +-- vessels:status:{mmsi} -> ANCHORED/MOORED 분류
                          +-- congestionIndex = min(100, anchored*10 + moored*3)
                          +-- baseline:{locode} -> UNCTAD 기준선 비율 계산
                          |
                          v
                    CongestionReport -> Kafka(port-congestion) -> Neo4j/Redis 갱신

계층 2: 기준선 (분기 1회)
  UNCTAD DataCentre API -> UncladStatisticsDownloader
                                |
                                v
                          Redis baseline:{locode} (TTL 90일)
```

**핵심 구현 - 혼잡도 계산 공식:**
```java
// 정박 대기 선박(ANCHORED)은 항만 혼잡의 주요 지표 -> 가중치 10
// 접안 선박(MOORED)은 정상 운영 -> 가중치 3
double congestionIndex = Math.min(100.0, (anchored * 10.0) + (moored * 3.0));

// UNCTAD 기준선 대비 비율 (예: 1.35 = 35% 초과)
double baseline = getBaseline(locode); // Redis에서 조회
double ratio = baseline > 0 ? (anchored + moored) / baseline : 1.0;
```

**선박 상태 저장 (AisRedisConsumer 확장):**
```java
// 기존: 위치만 저장
redisTemplate.opsForGeo().add("vessels:positions", point, mmsi);

// 추가: 상태도 저장 (혼잡도 계산 시 ANCHORED/MOORED 필터링용)
redisTemplate.opsForValue().set("vessels:status:" + mmsi, status, TTL);
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before (UNCTAD 크롤링) | After (이중 구조) |
|------|----------------------|------------------|
| 업데이트 주기 | 1일 (크롤링 실패 시 없음) | 10분 (실시간) |
| 외부 의존성 | 높음 (봇 차단 위험) | 낮음 (자체 AIS 데이터) |
| 데이터 정확도 | 분기 통계 평균값 | 실시간 선박 위치 기반 |
| 장애 복원력 | UNCTAD 다운 시 데이터 단절 | AIS 수신 중이면 항상 계산 가능 |
| Playwright 의존 | 필수 (100MB+ 바이너리) | 불필요 (제거 가능) |
| 정보 풍부도 | 혼잡도 수치 1개 | 정박/접안 수, 혼잡도 지수, 기준선 비율, 심각도 |
| API 응답 데이터 | `{ congestionLevel }` | `{ anchoredVessels, mooredVessels, congestionIndex, baselineRatio, severity }` |
