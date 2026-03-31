## Phase 6 자동화 아키텍처: 이벤트 기반 리스크 알림 파이프라인 구현

### 배경 및 문제정의
- 상황: AnchorIQ의 AI 엔진이 리스크 이벤트를 감지하면, 실시간으로 사용자에게 알리고 자동 액션을 실행해야 한다
- 문제: 리스크 이벤트 → 알림 발송 → 타임라인 기록이 동기적으로 처리되면 하나의 실패가 전체를 블로킹한다

### 기술 선정 (대안 비교 테이블)

| 항목 | 동기 처리 (REST 직접 호출) | 이벤트 기반 (Kafka Consumer Group) |
|------|---------------------------|-----------------------------------|
| 결합도 | 높음 — n8n/ES/WS 모두 직접 의존 | 낮음 — 각 Consumer 독립 |
| 부분 실패 | 하나 실패 시 전체 실패 | 각 Consumer 독립 재시도 |
| 확장성 | 새 채널 추가 시 기존 코드 수정 필요 | 새 Consumer Group 추가만으로 확장 |
| 순서 보장 | 보장됨 | 같은 vesselImo 파티션 키로 보장 |

**선택: Kafka Consumer Group 기반 이벤트 처리**

### 분석
- risk-alerts 토픽을 3개 Consumer Group이 독립 소비:
  - `alert-n8n-trigger`: 워크플로우 매칭 + n8n 트리거 + 알림 발송
  - `alert-es-logger`: Elasticsearch 타임라인 이력 저장
  - `alert-ws-pusher`: WebSocket으로 프론트엔드 실시간 푸시
- DDD 원칙 적용: Entity가 비즈니스 판단 (matchesEvent, matches), Service는 오케스트레이션만
- 3번 재시도 후 DLT 이동으로 메시지 유실 방지

### 솔루션

```java
// 풍부한 도메인 모델 — Entity가 매칭 판단을 직접 수행
public boolean matchesEvent(String eventType, String riskLevel) {
    if (!this.status.isActive()) return false;
    return this.triggerCondition.contains(eventType)
            || this.triggerCondition.contains(riskLevel);
}

// Kafka Consumer — 수동 커밋, DLT 에러 핸들러
@KafkaListener(topics = "risk-alerts", groupId = "alert-n8n-trigger",
        containerFactory = "riskAlertListenerContainerFactory")
public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
    RiskAlertEvent event = parseEvent(record.value());
    n8nClient.triggerRiskAlert(event);
    processMatchingWorkflows(event);
    notificationDispatcher.dispatch(event);
    ack.acknowledge(); // 수동 커밋
}
```

### 결과 (설계 목표 달성)

| 지표 | 목표 | 달성 |
|------|------|------|
| Consumer Group 독립 처리 | 3개 | 3개 (n8n, ES, WS) |
| DLT 재시도 | 3회 → DLT | 구현 완료 |
| 알림 채널 | Slack + Email | Strategy 패턴으로 구현 |
| DDD 풍부한 도메인 | Entity에 비즈니스 로직 | Workflow.matchesEvent(), NotificationRule.matches() |
| 인터페이스 기반 설계 | 모든 Service 인터페이스 분리 | N8nClient, Notifier, TimelineService 등 |
