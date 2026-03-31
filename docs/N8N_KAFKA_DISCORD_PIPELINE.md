## n8n + Kafka + Discord 리스크 알림 파이프라인 구축

### 배경 및 문제정의
- **상황**: AnchorIQ 해운 리스크 플랫폼에서 리스크 이벤트 발생 시 자동으로 알림을 보내야 함
- **요구사항**: Kafka 이벤트 → n8n 워크플로우 → Discord 채널 알림 자동화

### 기술 선정 (대안 비교 테이블)

| 항목 | n8n + Kafka | 백엔드 직접 호출 | AWS SNS/SQS |
|------|-----------|--------------|------------|
| 워크플로우 시각화 | n8n UI에서 드래그앤드롭 | 코드로만 관리 | CloudWatch |
| 채널 확장성 | 노드 추가로 Slack/Email/Discord 등 자유 연결 | 코드 수정 필요 | AWS 서비스 의존 |
| 비용 | 무료 (Self-hosted) | 무료 | 과금 |
| 운영 복잡도 | Docker 1개 | 없음 | AWS 인프라 |

### 아키텍처

```
[AI Decision Engine / Risk Scorer]
         ↓
    Kafka "risk-alerts" 토픽 (파티션 2, 키: vesselImo)
         ↓
    ┌────────────────────────────────────────────┐
    │ Spring Boot Consumer (3개 Consumer Group)   │
    │                                            │
    │ 1. alert-n8n-trigger                       │
    │    → N8nClient.triggerRiskAlert()           │
    │    → WorkflowDomainService.findMatching()   │
    │    → NotificationDispatcher.dispatch()      │
    │                                            │
    │ 2. alert-ws-pusher                         │
    │    → WebSocket /topic/alerts 실시간 푸시     │
    │                                            │
    │ 3. alert-es-logger (ES 비활성화 시 스킵)     │
    │    → Elasticsearch 타임라인 저장             │
    └────────────────────────────────────────────┘
         ↓
    n8n Webhook (http://localhost:5678/webhook/risk-alert)
         ↓
    [HTTP Request → Discord Webhook]
         ↓
    Discord 채널에 알림 메시지 표시
```

### 구현 내용

#### 1. Kafka Consumer 등록 (9개)

| Consumer Group | 토픽 | 역할 |
|---------------|------|------|
| alert-n8n-trigger | risk-alerts | n8n 웹훅 트리거 + 워크플로우 매칭 + 알림 발송 |
| alert-ws-pusher | risk-alerts | WebSocket 실시간 푸시 |
| sanction-neo4j-updater | sanction-updates | 제재 목록 Neo4j 업데이트 |
| sanction-redis-cache | sanction-updates | 제재 목록 Redis 캐시 |
| weather-neo4j-updater | weather-events | 기상 데이터 업데이트 |
| ais-neo4j-updater | ais-positions | AIS 위치 Neo4j |
| ais-redis-writer | ais-positions | AIS 위치 Redis GEO |
| market-data-processor | market-data | 유가/환율 캐시 |
| port-congestion-updater | port-congestion | 항만 혼잡도 업데이트 |

#### 2. n8n 워크플로우 설정

```
노드 구성: [Webhook] → [HTTP Request (Discord)]

Webhook 노드:
  - Method: POST
  - Path: risk-alert
  - URL: http://localhost:5678/webhook/risk-alert

HTTP Request 노드:
  - Method: POST
  - URL: Discord Webhook URL
  - Body: JSON (Expression 모드)
  - Expression:
    ={{ JSON.stringify({
      "content": "🚨 [" + $json.body.riskLevel + "] " + $json.body.type +
        " - 선박: " + $json.body.vesselName +
        " (IMO: " + $json.body.vesselImo +
        ") - 사유: " + $json.body.reason +
        " - 권장: " + $json.body.recommendedAction
    }) }}
```

#### 3. 트러블슈팅 해결 목록

| 문제 | 원인 | 해결 |
|------|------|------|
| Kafka Consumer 미등록 | `@ConditionalOnBean(KafkaTemplate.class)` — Bean 생성 순서 문제 | `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")` 전환 |
| KafkaAutoConfiguration 비활성화 | `application-local.yml`에 exclude 설정 | Kafka exclude 제거 |
| DefaultErrorHandler Bean 충돌 | automation + collector 모듈에서 동일 타입 Bean 2개 | factory 내부에서 직접 생성 |
| n8n OOM (exit code 137) | Docker 메모리 256MB 부족 | 512MB로 상향 |
| Workflow JSONB 저장 실패 | Hibernate 6 + PostgreSQL jsonb 타입 불일치 | `@JdbcTypeCode(SqlTypes.JSON)` 추가 |
| n8n 표현식 `{{ $json.body.riskLevel }}` 미동작 | Edit Fields 노드가 데이터 덮어쓰기 + 표현식 문법 | Edit Fields 삭제 + `JSON.stringify()` 표현식 사용 |

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| Kafka Consumer | 0개 등록 | 9개 등록 |
| risk-alerts 파이프라인 | 미동작 | Kafka → Consumer → n8n → Discord 자동 |
| 워크플로우 CRUD | API 에러 | 생성/조회/수정/삭제/활성화/비활성화 정상 |
| 알림 규칙 CRUD | API 에러 | 생성/조회/수정/삭제 정상 |
| Discord 알림 | 없음 | 리스크 이벤트 시 자동 알림 |
| n8n 워크플로우 | 없음 | Webhook → Discord HTTP Request |

### 디스코드 알림 결과 예시

```
🚨 [CRITICAL] SANCTION_VESSEL_DETECTED
- 선박: SHAHR-E-KORD (IMO: 9167253)
- 사유: 이란 연관 회사 소유 선박 호르무즈 해협 접근
- 권장: 컴플라이언스팀 즉시 확인
```

### 관련 파일

**백엔드:**
- `anchoriq-automation/consumer/RiskAlertN8nConsumer.java` — Kafka Consumer
- `anchoriq-automation/consumer/KafkaConsumerConfig.java` — Consumer 설정
- `anchoriq-automation/n8n/N8nWebhookClient.java` — n8n Webhook 클라이언트
- `anchoriq-automation/notification/NotificationDispatcherImpl.java` — 알림 발송
- `anchoriq-automation/notification/SlackNotifier.java` — Slack/Discord 발송
- `anchoriq-core/domain/operation/workflow/model/Workflow.java` — 워크플로우 엔티티

**인프라:**
- `infra/docker-compose.yml` — n8n (512MB), Kafka (KRaft)
- `application-local.yml` — Kafka bootstrap-servers 설정

**프론트엔드:**
- `frontend/src/features/workflow/WorkflowListPage.tsx` — 워크플로우 관리
- `frontend/src/features/alerts/AlertRulesPage.tsx` — 알림 규칙 관리
