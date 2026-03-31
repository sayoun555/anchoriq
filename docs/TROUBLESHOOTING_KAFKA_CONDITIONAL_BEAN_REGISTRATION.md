## Kafka Consumer/Producer Bean 미등록 → @ConditionalOnProperty 전환으로 해결

### 배경 및 문제정의
- **상황**: Kafka Docker 컨테이너가 실행 중인데 Spring Boot 앱에서 Kafka Consumer/Producer Bean이 등록되지 않음
- **문제**: `@ConditionalOnBean(KafkaTemplate.class)` 조건이 충족되지 않아 모든 Kafka 관련 Bean 생성 건너뜀

### 기술 선정 (대안 비교 테이블)

| 항목 | @ConditionalOnProperty | @ConditionalOnBean | @ConditionalOnClass | 무조건 등록 |
|------|----------------------|-------------------|--------------------|-----------|
| 동작 시점 | 프로퍼티 파싱 시 (초기) | Bean 생성 후 (늦음) | 클래스 로딩 시 | 항상 |
| 순서 의존성 | 없음 | Bean 생성 순서에 의존 | 없음 | 없음 |
| Kafka 미기동 대응 | 프로퍼티 없으면 건너뜀 | 불확실 | 클래스만 있으면 시도 | 앱 기동 실패 |
| Spring 공식 권장 | 권장 | 주의 필요 | 라이브러리 존재 체크용 | 비권장 |

### 분석
- **초기 판단**: `spring-kafka` 의존성이 runtime classpath에 없어서 `KafkaAutoConfiguration`이 비활성화된 것으로 추정
- **실제 원인**: `@ConditionalOnBean(KafkaTemplate.class)` 사용 시 Bean 생성 순서 문제 발생. Spring Boot는 auto-configuration을 `@Configuration` 클래스보다 나중에 처리하므로, `KafkaTemplate` Bean이 아직 생성되지 않은 시점에 `@ConditionalOnBean` 평가가 이루어짐
- **CS 원리**: Spring의 Bean 생성 순서는 `@Configuration` 클래스가 먼저, `@AutoConfiguration` (auto-configuration)이 나중에 처리된다. `@ConditionalOnBean`은 현재까지 생성된 Bean만 확인하므로, auto-configuration으로 생성될 Bean을 참조하면 조건이 false로 평가된다. 반면 `@ConditionalOnProperty`는 `Environment` 프로퍼티를 확인하므로 Bean 생성 순서와 무관하게 동작한다.

### 솔루션

```java
// Before — Bean 생성 순서에 의존 (불안정)
@Configuration
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaConsumerConfig { ... }

// After — 프로퍼티 존재 여부로 판단 (안정)
@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaConsumerConfig { ... }
```

추가 수정:
- `DefaultErrorHandler` Bean 충돌 해결: 2개 모듈(automation, collector)에서 같은 타입 Bean 등록 → factory 메서드 내부에서 직접 생성하여 Bean 노출 제거

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| Kafka Consumer 등록 수 | 0개 | 9개 (alert-n8n, alert-ws + 7 collector) |
| risk-alerts 토픽 구독 | 미구독 | 2개 Consumer Group 구독 |
| n8n Webhook 트리거 | 불가 | 동작 확인 |
| 전체 파이프라인 | 미동작 | Kafka → Consumer → n8n + 알림 + WS → 성공 |
| 수정 파일 수 | - | 4개 (KafkaConsumerConfig ×2, CollectorConfig, Consumer ×2) |
