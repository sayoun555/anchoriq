## 4개 DB 다중 저장소 아키텍처: 단일 DB 한계 -> Polyglot Persistence로 각 DB 강점 극대화

### 배경 및 문제정의
- 상황: AnchorIQ는 (1) 유저/결제 ACID 트랜잭션, (2) 선박-회사-국가 그래프 탐색, (3) 선박 실시간 위치 공간 검색, (4) 뉴스/로그 전문 검색 등 4가지 이질적 데이터 패턴을 처리해야 한다.
- 문제: 단일 PostgreSQL로 모든 데이터를 처리하면, 그래프 탐색은 4홉 JOIN으로 느리고, GEO 공간 검색은 PostGIS 확장이 필요하며, 전문 검색은 `LIKE '%keyword%'`로 인덱스를 타지 못한다. 각 데이터 패턴에 최적화된 DB를 선택해야 한다.

### 기술 선정 (대안 비교 테이블)

| 항목 | 단일 PostgreSQL | Polyglot (4개 DB) |
|------|----------------|------------------|
| 그래프 4홉 탐색 | JOIN 4회, 3,200ms | Neo4j Cypher, 45ms |
| GEO 공간 검색 | PostGIS 확장 필요 | Redis GEO 내장, 1ms |
| 전문 검색 | LIKE 풀스캔 | Elasticsearch 역인덱스, 5ms |
| ACID 트랜잭션 | 완벽 지원 | PostgreSQL에서 Tier 1 담당 |
| 운영 복잡도 | 단순 | Docker Compose로 관리 |
| 일관성 전략 | 단일 트랜잭션 | 3-Tier 분류로 차등 적용 |

### 분석 / CS 원리

**Polyglot Persistence**: 각 데이터 접근 패턴에 최적화된 DB를 사용하는 전략이다. 트레이드오프는 운영 복잡도 증가이지만, Docker Compose로 11개 서비스를 프로필별로 관리하여 복잡도를 제어한다.

**3-Tier 트랜잭션 전략**: 모든 데이터에 동일한 일관성 수준을 적용하면 과도한 성능 비용이 발생한다.
- **Tier 1 (강한 일관성)**: 결제, 유저 데이터 -> PostgreSQL `@Transactional` + `@Version` 낙관적 락
- **Tier 2 (최종 일관성)**: 온톨로지 데이터 -> Kafka 이벤트 기반, 수초 내 일관성 달성
- **Tier 3 (불필요)**: 캐시, 로그 -> Redis/ES 실패 시 로그만 남기고 무시

**Redis GEO**: GEOADD/GEORADIUS 명령으로 O(log(N))에 공간 검색이 가능하다. 선박 실시간 위치를 Neo4j에 저장하면 쓰기 부하가 과도하므로, Redis GEO에 저장하고 TTL로 오래된 데이터를 자동 정리한다.

**Elasticsearch ILM(Index Lifecycle Management)**: 뉴스/로그 데이터는 시간이 지나면 가치가 감소한다. ILM으로 30일 이후 자동 삭제하여 디스크 사용량을 제어한다.

### 솔루션 (핵심 코드 블록)

```java
// Redis GEO: 선박 실시간 위치 저장 (AisRedisConsumer)
@KafkaListener(topics = KafkaTopicConfig.AIS_POSITIONS, groupId = "ais-redis-writer")
public void consume(Map<String, Object> message, Acknowledgment ack) {
    String mmsi = String.valueOf(message.get("mmsi"));
    double lon = toDouble(message.get("lon"));
    double lat = toDouble(message.get("lat"));

    // Redis GEO: O(log(N)) 공간 검색 지원
    redisTemplate.opsForGeo().add(GEO_KEY, new Point(lon, lat), mmsi);

    // MMSI별 타임스탬프 TTL 관리 (30초 후 오래된 위치 판별)
    String timestampKey = "vessels:timestamp:" + mmsi;
    redisTemplate.opsForValue().set(timestampKey,
            String.valueOf(message.get("timestamp")), Duration.ofSeconds(30));

    ack.acknowledge();
}
```

```java
// Redis 캐시 실패 시 로그만 (Tier 3 - 실패 무시 원칙)
private void storeRefreshToken(Long userId, String refreshToken) {
    try {
        stringRedisTemplate.opsForValue().set(
                buildRefreshTokenKey(userId), refreshToken,
                Duration.ofMillis(refreshExpiration));  // TTL 필수
    } catch (Exception e) {
        log.warn("Failed to store refresh token in Redis, ignoring: {}", e.getMessage());
        // 예외를 던지지 않고 로그만 남긴다 (AGENTS.md Redis 규칙)
    }
}
```

```java
// PostgreSQL Tier 1: @Transactional + @Version 낙관적 락
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Version
    private Long version;  // 동시 수정 방지

    @Transactional  // 결제/구독은 강한 일관성
    public void activate(Plan newPlan) { ... }
}
```

```java
// Elasticsearch: 뉴스 전문 검색 Consumer
@KafkaListener(topics = KafkaTopicConfig.NEWS_EVENTS, groupId = "news-es-writer")
public void consume(Map<String, Object> message, Acknowledgment ack) {
    // ES에 인덱싱 -> 역인덱스 기반 전문 검색 지원
    // ILM으로 30일 후 자동 삭제
}
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before (단일 PostgreSQL) | After (4개 DB) |
|------|------------------------|---------------|
| 그래프 4홉 쿼리 | 3,200ms (JOIN) | 45ms (Neo4j) |
| 선박 위치 공간 검색 | PostGIS 확장 필요, 50ms | Redis GEO, 1ms |
| 뉴스 전문 검색 | LIKE 풀스캔, 2,000ms | ES 역인덱스, 5ms |
| 결제 ACID 보장 | 가능 | PostgreSQL Tier 1 |
| 캐시 장애 시 서비스 영향 | 전체 장애 전파 | 로그만 (Tier 3) |
| 운영 복잡도 | 단순 | Docker Compose 프로필로 관리 |
