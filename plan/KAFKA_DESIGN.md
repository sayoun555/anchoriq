# AnchorIQ — Kafka 토픽 설계

> 이벤트 스트리밍 아키텍처 — 하나의 이벤트를 여러 Consumer가 독립 처리

---

## 목차
- [왜 Kafka인가](#왜-kafka인가)
- [토픽 목록](#토픽-목록)
- [파티션 설계](#파티션-설계)
- [메시지 포맷](#메시지-포맷)
- [Consumer Group 설계](#consumer-group-설계)
- [전체 흐름도](#전체-흐름도)
- [Retention 정책](#retention-정책)
- [에러 처리 (Dead Letter Topic)](#에러-처리)
- [모니터링 지표](#모니터링-지표)
- [설정](#설정)

---

## 왜 Kafka인가

### 문제

> AIS 데이터 하나가 들어오면 3곳에서 동시에 처리해야 함

```
선박 위치 업데이트 → Redis GEO 저장 (위치 캐싱)
                  → Neo4j 상태 업데이트 (SAILING/ANCHORED)
                  → AI 이상 탐지 (AIS 끊김? 항로 이탈?)
```

### Kafka 없이 하면?

```java
// Bad: 동기적으로 3개 다 호출
void handleAisData(AisMessage msg) {
    redisService.updatePosition(msg);     // 1번
    neo4jService.updateStatus(msg);       // 2번 — 1번 끝나야 시작
    aiService.detectAnomaly(msg);         // 3번 — 2번 끝나야 시작
}
// 하나 실패하면? 전부 실패? 부분 실패?
// 하나 느리면? 전부 느려짐
```

### Kafka 있으면?

```
Producer → ais-positions 토픽에 넣음 (끝)
    ├→ redis-writer (독립 Consumer) → Redis GEO
    ├→ neo4j-updater (독립 Consumer) → Neo4j
    └→ ai-detector (독립 Consumer) → AI 엔진
```

- 각 Consumer 독립 → 하나 실패해도 나머지 정상
- 각 Consumer 독립 속도 → 느린 놈이 빠른 놈 안 잡음
- 장애 복구 → 실패한 Consumer 재시작하면 밀린 메시지부터 재처리

### 면접 답변

> "하나의 AIS 이벤트를 여러 소비자가 독립적으로 처리하기 위해 Kafka를 선택했습니다.
> 동기 호출 시 발생하는 결합도, 부분 실패, 성능 병목 문제를 해소합니다."

---

## 토픽 목록

| 토픽 | Producer | 메시지 빈도 | 설명 |
|------|----------|------------|------|
| `ais-positions` | AIS WebSocket 수집기 | 초당 수백 건 | 선박 실시간 위치 |
| `weather-events` | Open-Meteo 수집기 | 10분마다 | 기상/태풍 데이터 |
| `sanction-updates` | UN 제재 수집기 | 하루 1~2회 | 제재 목록 변경 |
| `news-events` | GNews 수집기 | 하루 100건 | 해운 뉴스 |
| `market-data` | EIA + Frankfurter 수집기 | 하루 수 회 | 유가/환율 |
| `geopolitical-events` | GDELT 수집기 | 15분마다 | 지정학 이벤트 |
| `port-congestion` | UNCTAD 크롤러 | 30분마다 | 항만 혼잡도 |
| `risk-alerts` | AI 판단 엔진 | 리스크 발생 시 | 리스크 알림 |

---

## 파티션 설계

### 파티션 수

| 토픽 | 파티션 | 이유 |
|------|--------|------|
| `ais-positions` | 3 | 초당 수백 건 → 병렬 처리 필수 |
| `weather-events` | 1 | 10분마다 → 병렬 불필요 |
| `sanction-updates` | 1 | 하루 1~2회 |
| `news-events` | 1 | 하루 100건 |
| `market-data` | 1 | 하루 수 회 |
| `geopolitical-events` | 1 | 15분마다 |
| `port-congestion` | 1 | 30분마다 |
| `risk-alerts` | 2 | 알림은 빠르게 처리해야 함 |

### 파티션 키

> 같은 키 → 같은 파티션 → 순서 보장

| 토픽 | 파티션 키 | 이유 |
|------|----------|------|
| `ais-positions` | `mmsi` | 같은 선박 위치 데이터 순서 보장 (부산→도쿄→오사카 안 꼬임) |
| `risk-alerts` | `vesselImo` | 같은 선박 알림 순서 보장 |
| 나머지 | 없음 (라운드로빈) | 순서 중요하지 않음 |

### 왜 순서가 중요한가 (ais-positions 예시)

```
파티션 키 없으면:
  선박 A 위치: 부산 → 도쿄 → 오사카
  실제 처리:   도쿄 → 부산 → 오사카  ← 순서 꼬임 → Redis에 부산이 최신으로 덮어씀

파티션 키 = mmsi:
  선박 A → 항상 파티션 1로 → 부산 → 도쿄 → 오사카 순서 보장
  선박 B → 항상 파티션 2로 → 독립 처리
```

---

## 메시지 포맷

> 모든 메시지 JSON 형식

### ais-positions

```json
{
  "mmsi": "440123456",
  "imo": "9811000",
  "name": "EVER GIVEN",
  "type": "CONTAINER",
  "flag": "PA",
  "lat": 35.1,
  "lon": 129.0,
  "speed": 12.5,
  "heading": 270,
  "status": "SAILING",
  "timestamp": "2026-03-27T14:30:00Z"
}
```

### weather-events

```json
{
  "zone": "East China Sea",
  "lat": 30.0,
  "lon": 125.0,
  "type": "TYPHOON",
  "name": "TYPHOON_LAN",
  "severity": "HIGH",
  "windSpeed": 45.0,
  "waveHeight": 8.5,
  "forecastPath": [
    {"lat": 30.0, "lon": 125.0, "time": "2026-03-27T14:00:00Z"},
    {"lat": 31.5, "lon": 126.0, "time": "2026-03-27T20:00:00Z"}
  ],
  "timestamp": "2026-03-27T14:30:00Z"
}
```

### sanction-updates

```json
{
  "action": "ADDED",
  "targetType": "VESSEL",
  "targetName": "SUSPICIOUS VESSEL",
  "targetImo": "9999000",
  "referenceNumber": "UNSCR-2375",
  "reason": "UN Security Council Resolution 2375",
  "country": "KP",
  "timestamp": "2026-03-27T00:00:00Z"
}
```

### news-events

```json
{
  "title": "홍해 후티 공격 재개, 아시아-유럽 항로 긴장",
  "source": "Reuters",
  "url": "https://...",
  "publishedAt": "2026-03-27T10:00:00Z",
  "keywords": ["홍해", "후티", "바브엘만데브", "해운"],
  "timestamp": "2026-03-27T14:30:00Z"
}
```

### market-data

```json
{
  "dataType": "OIL_PRICE",
  "indicators": {
    "wti": 78.50,
    "brent": 82.30
  },
  "currency": "USD",
  "timestamp": "2026-03-27T14:00:00Z"
}
```

```json
{
  "dataType": "EXCHANGE_RATE",
  "base": "USD",
  "rates": {
    "KRW": 1350.50,
    "EUR": 0.92,
    "JPY": 150.20
  },
  "timestamp": "2026-03-27T14:00:00Z"
}
```

### geopolitical-events

```json
{
  "eventType": "MILITARY_ACTIVITY",
  "region": "Bab el-Mandeb",
  "lat": 12.5,
  "lon": 43.3,
  "severity": "HIGH",
  "description": "Military vessels detected near Bab el-Mandeb strait",
  "source": "GDELT",
  "timestamp": "2026-03-27T14:15:00Z"
}
```

### port-congestion

```json
{
  "locode": "KRPUS",
  "portName": "Busan",
  "congestionLevel": 85.0,
  "waitingVessels": 12,
  "avgWaitTime": 18.5,
  "timestamp": "2026-03-27T14:00:00Z"
}
```

### risk-alerts

```json
{
  "alertId": "alert-2026032714310001",
  "type": "SANCTION_VESSEL_DETECTED",
  "riskLevel": "HIGH",
  "vesselImo": "9811000",
  "vesselName": "SUSPICIOUS VESSEL",
  "chokepoint": "Hormuz",
  "reason": "이란 연관 회사 소유 선박이 호르무즈 해협 접근 중",
  "recommendedAction": "컴플라이언스팀 즉시 확인",
  "aiConfidence": 0.92,
  "relatedEntities": {
    "company": "Iran Shipping Co.",
    "country": "IR",
    "sanction": "UNSCR-2231"
  },
  "timestamp": "2026-03-27T14:31:00Z"
}
```

---

## Consumer Group 설계

### ais-positions (3개 Consumer Group)

```
ais-positions (파티션 3개)
  ├→ consumer-group: "ais-redis-writer"
  │    → Consumer 3개 (파티션당 1개)
  │    → Redis GEO 위치 저장
  │
  ├→ consumer-group: "ais-neo4j-updater"
  │    → Consumer 1개 (배치 처리, 10초마다 모아서)
  │    → Neo4j 선박 상태 업데이트 (SAILING/ANCHORED/MOORED)
  │
  └→ consumer-group: "ais-anomaly-detector"
       → Consumer 1개
       → AI 이상 탐지 (AIS 끊김, 항로 이탈, 속도 이상)
       → 리스크 발견 시 → risk-alerts 토픽에 발행
```

### weather-events (2개 Consumer Group)

```
weather-events (파티션 1개)
  ├→ consumer-group: "weather-neo4j-updater"
  │    → Neo4j WeatherCondition 업데이트
  │
  └→ consumer-group: "weather-risk-evaluator"
       → AI 기상 리스크 평가
       → 태풍 접근 시 → risk-alerts 토픽에 발행
```

### sanction-updates (2개 Consumer Group)

```
sanction-updates (파티션 1개)
  ├→ consumer-group: "sanction-neo4j-updater"
  │    → Neo4j Sanction 노드 + 관계 업데이트
  │
  └→ consumer-group: "sanction-redis-cache"
       → Redis 제재 선박 목록 캐시 갱신
```

### news-events (2개 Consumer Group)

```
news-events (파티션 1개)
  ├→ consumer-group: "news-es-writer"
  │    → Elasticsearch news 인덱스 저장
  │
  └→ consumer-group: "news-ai-analyzer"
       → AI 뉴스 분석 (해운 영향도 판단)
```

### market-data (1개 Consumer Group)

```
market-data (파티션 1개)
  └→ consumer-group: "market-data-processor"
       → Redis 캐시 업데이트 + Neo4j 시장 데이터 반영
```

### geopolitical-events (2개 Consumer Group)

```
geopolitical-events (파티션 1개)
  ├→ consumer-group: "geopolitical-es-writer"
  │    → Elasticsearch geopolitical-events 인덱스 저장
  │
  └→ consumer-group: "geopolitical-risk-evaluator"
       → 초크포인트/항로 리스크 재평가
       → 위험도 변경 시 → risk-alerts 토픽에 발행
```

### port-congestion (1개 Consumer Group)

```
port-congestion (파티션 1개)
  └→ consumer-group: "port-congestion-updater"
       → Neo4j Port 혼잡도 업데이트 + Redis 캐시 갱신
```

### risk-alerts (3개 Consumer Group)

```
risk-alerts (파티션 2개)
  ├→ consumer-group: "alert-n8n-trigger"
  │    → n8n 웹훅 트리거 (Slack/Email 발송)
  │
  ├→ consumer-group: "alert-es-logger"
  │    → Elasticsearch ai-decisions 인덱스 저장
  │
  └→ consumer-group: "alert-ws-pusher"
       → WebSocket으로 프론트엔드에 실시간 푸시
```

---

## 전체 흐름도

```
[AIS WebSocket] → ais-positions (파티션 3, 키: mmsi)
                    ├→ redis-writer (3 consumers)      → Redis GEO
                    ├→ neo4j-updater (1 consumer)      → Neo4j 상태
                    └→ anomaly-detector (1 consumer)   → AI 이상 탐지
                                                            ↓ 리스크 발견 시
[Open-Meteo]    → weather-events (파티션 1)
                    ├→ neo4j-updater                   → Neo4j 기상
                    └→ risk-evaluator                  → AI 기상 리스크
                                                            ↓ 태풍 접근 시
[UN 제재]       → sanction-updates (파티션 1)
                    ├→ neo4j-updater                   → Neo4j 제재
                    └→ redis-cache                     → Redis 제재 캐시

[GNews]         → news-events (파티션 1)
                    ├→ es-writer                       → Elasticsearch
                    └→ ai-analyzer                     → AI 뉴스 분석

[EIA/Frankfurter]→ market-data (파티션 1)
                    └→ data-processor                  → Redis + Neo4j

[GDELT]         → geopolitical-events (파티션 1)
                    ├→ es-writer                       → Elasticsearch
                    └→ risk-evaluator                  → 초크포인트 리스크
                                                            ↓ 위험도 변경 시
[UNCTAD 크롤러] → port-congestion (파티션 1)
                    └→ congestion-updater              → Neo4j + Redis

                         ↓ 모든 리스크 이벤트 집결

                    risk-alerts (파티션 2, 키: vesselImo)
                    ├→ n8n-trigger                     → Slack/Email 알림
                    ├→ es-logger                       → Elasticsearch 저장
                    └→ ws-pusher                       → 프론트 WebSocket
```

---

## Retention 정책

| 토픽 | 보관 기간 | 이유 |
|------|----------|------|
| `ais-positions` | 1시간 | 실시간 데이터, 위치는 Redis에 저장됨 |
| `weather-events` | 24시간 | 장애 시 재처리 가능성 |
| `sanction-updates` | 7일 | 제재 업데이트 이력 확인 |
| `news-events` | 3일 | 뉴스 재처리 |
| `market-data` | 3일 | |
| `geopolitical-events` | 3일 | |
| `port-congestion` | 24시간 | |
| `risk-alerts` | 7일 | 알림 이력 보관, 디버깅용 |
| `*.DLT` (Dead Letter) | 30일 | 실패 메시지 분석용 |

---

## 에러 처리

### Dead Letter Topic (DLT)

> Consumer가 메시지 처리 3번 실패하면 DLT로 이동

```
정상:
ais-positions → Consumer → Redis 저장 ✅

실패:
ais-positions → Consumer → Redis 저장 ❌ (1초 후 재시도)
                         → Redis 저장 ❌ (1초 후 재시도)
                         → Redis 저장 ❌ (3번째 실패)
                              ↓
                    ais-positions.DLT (Dead Letter Topic)
                              ↓
                    나중에 원인 분석 + 수동 재처리
```

### 구현

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(kafkaTemplate);
    FixedBackOff backOff = new FixedBackOff(1000L, 3); // 1초 간격, 3번 재시도
    return new DefaultErrorHandler(recoverer, backOff);
}
```

### DLT 토픽 자동 생성

| 원본 토픽 | DLT 토픽 |
|----------|---------|
| ais-positions | ais-positions.DLT |
| weather-events | weather-events.DLT |
| risk-alerts | risk-alerts.DLT |
| ... | ... |

---

## 모니터링 지표

| 지표 | 의미 | 위험 신호 | Grafana 연동 |
|------|------|----------|-------------|
| **Consumer Lag** | 아직 안 읽은 메시지 수 | lag 계속 증가 → Consumer가 못 따라감 | ✅ |
| **Throughput** | 초당 메시지 처리량 | 급격한 감소 → 장애 의심 | ✅ |
| **Error Rate** | 처리 실패율 | DLT에 쌓이면 확인 필요 | ✅ |
| **DLT Message Count** | Dead Letter 메시지 수 | 0이 아니면 조사 필요 | ✅ |

> Phase 10 (모니터링)에서 Grafana 대시보드에 Kafka 지표 포함

---

## 설정

### KRaft 모드 (Zookeeper 제거)

> Zookeeper 없이 Kafka 단독 실행 → 메모리 ~500MB 절약

```yaml
# docker-compose.yml
kafka:
  image: confluentinc/cp-kafka:7.6.0
  environment:
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_NODE_ID: 1
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
    KAFKA_LOG_RETENTION_HOURS: 1        # 기본 1시간 (토픽별 오버라이드)
    KAFKA_NUM_PARTITIONS: 1             # 기본 파티션 1개 (토픽별 오버라이드)
```

### Spring Boot 설정

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      auto-offset-reset: latest
      enable-auto-commit: false          # 수동 커밋 (처리 완료 후 커밋)
      max-poll-records: 500              # ais-positions 배치 처리용
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: 1                            # 리더 확인 후 응답 (속도 + 안정성 균형)
    listener:
      concurrency: 3                     # ais-positions Consumer 3개 병렬
```

### 토픽 자동 생성 (Spring Boot)

```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic aisPositions() {
        return TopicBuilder.name("ais-positions")
            .partitions(3)
            .replicas(1)
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofHours(1).toMillis()))
            .build();
    }

    @Bean
    public NewTopic riskAlerts() {
        return TopicBuilder.name("risk-alerts")
            .partitions(2)
            .replicas(1)
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
            .build();
    }

    // ... 나머지 토픽도 동일 패턴
}
```
