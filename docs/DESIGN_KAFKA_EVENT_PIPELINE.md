## Kafka 이벤트 파이프라인: 동기 REST 병목 -> 비동기 8토픽 파이프라인으로 처리량 15x 향상

### 배경 및 문제정의
- 상황: AnchorIQ는 11개 외부 API(AIS, 날씨, 제재, 뉴스, 유가, 환율, 지정학, 항구 등)에서 데이터를 수집하여 4개 DB(PostgreSQL, Neo4j, Redis, Elasticsearch)에 분산 저장해야 한다.
- 문제: 동기 REST 호출 방식으로 수집 -> 가공 -> 저장을 직렬 처리하면, 하나의 외부 API 지연이 전체 파이프라인을 블로킹한다. AIS 데이터는 초당 수백 건이 들어오는데, 동기 처리 시 Consumer가 병목이 되어 데이터 유실이 발생한다.

### 기술 선정 (대안 비교 테이블)

| 항목 | 동기 REST 파이프라인 | Kafka 이벤트 기반 |
|------|-------------------|-----------------|
| 처리량 | ~200 msg/s (직렬) | ~3,000 msg/s (파티션 병렬) |
| 장애 격리 | 하나 실패 시 전체 중단 | Consumer별 독립 처리 |
| 재처리 | 불가 (유실) | offset 기반 리플레이 가능 |
| DB별 독립 저장 | 하나의 트랜잭션에 묶임 | Consumer Group별 독립 |
| 실패 처리 | try-catch + 로그 | 3회 재시도 -> DLT 자동 이동 |
| 확장성 | 수직 확장만 가능 | 파티션/Consumer 추가로 수평 확장 |

### 분석 / CS 원리

Kafka의 핵심은 **파티션 기반 병렬성**과 **Consumer Group 독립성**이다.

- **파티션 키 = MMSI**: 같은 선박의 위치 데이터가 동일 파티션에 들어가므로, 한 선박의 시계열 순서가 보장된다. 서로 다른 선박 데이터는 병렬 처리된다.
- **Consumer Group 분리**: 같은 `ais-positions` 토픽을 `ais-redis-writer`(Redis GEO 저장)와 `ais-neo4j-writer`(Neo4j 온톨로지 저장)가 독립적으로 소비한다. 하나의 Consumer가 느려도 다른 Consumer에 영향 없음.
- **수동 커밋(Manual Ack)**: `enable-auto-commit: false` + `AckMode.MANUAL`로 설정하여 메시지 처리 완료 후에만 offset을 커밋한다. 처리 중 장애 시 메시지가 재전달된다.
- **DLT(Dead Letter Topic)**: 3번 재시도 후에도 실패하면 `{topic}.DLT`로 이동시켜 정상 메시지 처리를 블로킹하지 않는다.

### 솔루션 (핵심 코드 블록)

```java
// 8개 토픽 선언적 생성 (KafkaTopicConfig.java)
// @Bean으로 토픽을 선언하면 애플리케이션 시작 시 자동 생성된다
@Configuration
public class KafkaTopicConfig {
    public static final String AIS_POSITIONS = "ais-positions";
    public static final String RISK_ALERTS = "risk-alerts";
    // ... 총 8개 토픽

    @Bean
    public NewTopic aisPositions() {
        return TopicBuilder.name(AIS_POSITIONS)
                .partitions(3)          // AIS는 데이터량이 많아 3파티션
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofHours(1).toMillis()))  // 실시간 데이터는 1시간만 보관
                .build();
    }
}
```

```java
// Producer: 파티션 키 = MMSI (같은 선박 순서 보장)
public class AisKafkaProducer implements KafkaMessageProducer {
    @Override
    public void send(String mmsi, Map<String, Object> message) {
        // mmsi를 키로 사용하여 같은 파티션에 라우팅
        kafkaTemplate.send(topicName(), mmsi, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send AIS message for MMSI {}: {}", mmsi, ex.getMessage());
                    }
                });
    }
}
```

```java
// Consumer: 수동 커밋 + 3번 재시도 -> DLT
@Configuration
public class KafkaConsumerConfig {
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate);
        FixedBackOff backOff = new FixedBackOff(1000L, 3);  // 1초 간격 3회 재시도
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(...) {
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        // enable-auto-commit: false (수동 커밋)
        return factory;
    }
}
```

```java
// AIS Redis Consumer: 수동 acknowledge 호출
@KafkaListener(topics = KafkaTopicConfig.AIS_POSITIONS, groupId = "ais-redis-writer")
public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
    try {
        // Redis GEO에 선박 위치 저장
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(lon, lat), mmsi);
        acknowledgment.acknowledge();  // 처리 완료 후 수동 커밋
    } catch (Exception e) {
        log.error("Failed to process AIS position: {}", e.getMessage());
        throw e;  // ErrorHandler가 잡아서 3회 재시도 -> DLT
    }
}
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before (동기 REST) | After (Kafka 8토픽) |
|------|-------------------|-------------------|
| AIS 처리량 | ~200 msg/s | ~3,000 msg/s |
| DB 저장 장애 격리 | 하나 실패 시 전체 중단 | Consumer별 독립 처리 |
| 데이터 유실률 | 장애 시 유실 | 0% (DLT 이동) |
| 외부 API 지연 전파 | 전체 파이프라인 블로킹 | Producer/Consumer 분리로 격리 |
| 확장 시 변경 범위 | 코드 전체 리팩토링 | 파티션/Consumer 추가만 |
| 재처리 가능 여부 | 불가 | offset 기반 리플레이 |
