## @Component 제거 후 @Configuration + @Bean 전환: Bean 생성 분산 → 명시적 중앙 등록으로 의존성 흐름 가시화

### 배경 및 문제정의
- 상황: 초기 구현에서 28개 클래스에 `@Component`, `@Service` 어노테이션을 남발했다. Domain Service, Factory, Gateway 등 모든 Bean이 자기 자신에게 어노테이션을 붙이는 방식이었다.
- 문제:
  - **Bean 생성 로직 분산**: 어떤 Bean이 어떤 의존성을 주입받는지 파악하려면 28개 파일을 전부 열어봐야 했다. "이 Bean은 어디서 생성되지?"라는 질문에 답하려면 `@Component` 스캔 경로를 추적해야 했다.
  - **의존성 주입 흐름 불투명**: `@Autowired`나 `@RequiredArgsConstructor`로 주입받는 빈들의 조합을 한눈에 파악할 수 없었다. 순환 의존성이 발생해도 서버 기동 시에야 발견된다.
  - **테스트 시 교체 어려움**: 특정 Bean만 테스트용으로 교체하려면 `@MockBean`을 쓰거나 전체 Context를 올려야 했다. `@Configuration` 클래스를 테스트 전용으로 만들면 간단히 해결되지만, `@Component`로 등록된 Bean은 자동 스캔에서 제외하기 어렵다.
  - **domain 모듈의 Spring 오염**: DDD 원칙상 domain 레이어는 Spring에 의존하지 않아야 하는데, Domain Service에 `@Service`가 붙어 있으면 순수 도메인 모델이 아니게 된다.

### 기술 선정 (대안 비교 테이블)

| 항목 | @Component 스캔 유지 | @Configuration + @Bean 명시적 등록 |
|------|---------------------|-----------------------------------|
| Bean 생성 위치 | 28개 파일에 분산 | 20개 @Configuration에 중앙화 |
| 의존성 흐름 파악 | 파일 전부 열어봐야 함 | Config 파일만 보면 됨 |
| 도메인 순수성 | @Service 어노테이션 필요 | 순수 POJO (어노테이션 없음) |
| 프로필별 Bean 교체 | @ConditionalOnProfile 개별 추가 | Config 클래스 단위로 교체 |
| 테스트 설정 | @MockBean 또는 전체 Context | 테스트용 Config 클래스로 간단 교체 |
| 순환 의존성 감지 | 런타임 (서버 기동 시) | 컴파일 타임 + Config 파일 리뷰 시 |

**선택: @Configuration + @Bean 명시적 등록**. Spring 공식 문서에서도 "외부 라이브러리 클래스나 도메인 객체는 @Bean으로 등록하라"고 권장한다. @Component 스캔은 편리하지만, 프로젝트 규모가 커지면 Bean 생성 흐름을 추적하기 어려워진다.

### 분석 / CS 원리

**Composition Root 패턴**

Mark Seemann의 Dependency Injection 원칙에서 "Composition Root"는 애플리케이션의 Bean 조립이 일어나는 단일 지점을 말한다. @Configuration 클래스가 바로 이 역할을 수행한다.

```
[Before] Bean 생성이 28개 파일에 흩어짐 → "어디서 조립되는지" 추적 불가
[After]  20개 Config 파일이 Composition Root → "이 파일만 보면 전체 조립 파악 가능"
```

**명시적 의존성 원칙 (Explicit Dependencies Principle)**

Bean이 필요로 하는 의존성을 생성자 파라미터로 명시하면, @Configuration 클래스의 @Bean 메서드에서 "이 Bean은 이것과 저것에 의존한다"가 코드로 문서화된다. `@Autowired` + `@Component` 조합은 이 관계를 숨긴다.

**Domain Service의 순수성**: DDD에서 Domain Service는 여러 Aggregate에 걸치는 비즈니스 로직을 담당한다. `@Service`를 붙이면 Spring에 의존하게 되어, domain 모듈이 Spring 없이 테스트 불가능해진다. @Bean으로 외부에서 등록하면 Domain Service는 순수 POJO를 유지할 수 있다.

### 솔루션 (핵심 코드 블록)

**1. DomainServiceConfig** — 8개 Domain Service를 한 곳에서 등록:

```java
// infrastructure/config/DomainServiceConfig.java
@Configuration
public class DomainServiceConfig {

    @Bean
    public SupplyChainRiskService supplyChainRiskService(
            SanctionRepository sanctionRepository,
            WeatherRepository weatherRepository) {
        return new SupplyChainRiskServiceImpl(sanctionRepository, weatherRepository);
    }

    @Bean
    public SanctionScreeningService sanctionScreeningService(
            SanctionRepository sanctionRepository,
            VesselRepository vesselRepository) {
        return new SanctionScreeningServiceImpl(sanctionRepository, vesselRepository);
    }

    @Bean
    public RouteOptimizationService routeOptimizationService(
            RouteRepository routeRepository) {
        return new RouteOptimizationServiceImpl(routeRepository);
    }

    @Bean
    public AnomalyDetectionService anomalyDetectionService(
            AnomalyRepository anomalyRepository) {
        return new AnomalyDetectionServiceImpl(anomalyRepository);
    }

    @Bean
    public RouteComparisonService routeComparisonService(
            RouteRepository routeRepository,
            PortRepository portRepository,
            SupplyChainRiskService supplyChainRiskService) {
        return new RouteComparisonServiceImpl(
                routeRepository, portRepository, supplyChainRiskService);
    }

    @Bean
    public SubscriptionService subscriptionService(
            SubscriptionRepository subscriptionRepository,
            StringRedisTemplate stringRedisTemplate) {
        return new SubscriptionServiceImpl(subscriptionRepository, stringRedisTemplate);
    }
}
```

**2. PaymentConfig** — Strategy 패턴 Bean 조립이 한눈에 보임:

```java
// infrastructure/config/PaymentConfig.java
@Configuration
public class PaymentConfig {

    @Bean
    public StripePaymentGateway stripePaymentGateway(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.webhook-secret}") String webhookSecret) {
        return new StripePaymentGateway(secretKey, webhookSecret);
    }

    @Bean
    public TossPaymentGateway tossPaymentGateway(
            @Value("${toss.secret-key}") String secretKey,
            @Value("${toss.webhook-secret}") String webhookSecret) {
        return new TossPaymentGateway(secretKey, webhookSecret);
    }

    @Bean
    public PaymentGatewayRouter paymentGatewayRouter(
            StripePaymentGateway stripePaymentGateway,
            TossPaymentGateway tossPaymentGateway) {
        return new PaymentGatewayRouter(stripePaymentGateway, tossPaymentGateway);
    }
}
```

**3. DomainFactoryConfig** — DDD Factory도 명시적 등록:

```java
// infrastructure/config/DomainFactoryConfig.java
@Configuration
public class DomainFactoryConfig {

    @Bean
    public VesselFactory vesselFactory(VesselRepository vesselRepository,
                                        SanctionRepository sanctionRepository) {
        return new VesselFactory(vesselRepository, sanctionRepository);
    }

    @Bean
    public SubscriptionFactory subscriptionFactory(
            SubscriptionRepository subscriptionRepository) {
        return new SubscriptionFactory(subscriptionRepository);
    }

    @Bean
    public WorkflowFactory workflowFactory() {
        return new WorkflowFactory();
    }
}
```

**전체 @Configuration 클래스 20개**: DomainServiceConfig, DomainFactoryConfig, PaymentConfig, SecurityConfig, RedisConfig, WebClientConfig, WebSocketConfig, AsyncConfig, MetricsConfig, OntologyConfig, AnalyticsConfig, CollectorConfig, KafkaProducerConfig, KafkaConsumerConfig, KafkaTopicConfig, PlaywrightConfig, OpenClawConfig, AutomationConfig, N8nConfig, KafkaConsumerConfig(automation)

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| @Component/@Service 남발 클래스 | 28개 | 0개 (Controller, Scheduler 등 Spring 고유 역할만 유지) |
| @Configuration 클래스 | 5개 (Security, Redis 등 인프라만) | 20개 (도메인 + 인프라 전체) |
| Bean 생성 흐름 파악 시간 | 28개 파일 탐색 필요 | Config 파일 1~2개만 확인 |
| Domain Service Spring 의존성 | @Service 어노테이션 필요 | 순수 POJO (Spring 의존 없음) |
| 프로필별 Bean 교체 | 개별 @ConditionalOnProfile | Config 클래스 단위로 교체 |
| 테스트 Bean 교체 | @MockBean + 전체 Context | 테스트용 Config로 즉시 교체 |
| 순환 의존성 감지 | 런타임 (서버 기동 실패 시) | Config 파일 리뷰 시 사전 발견 |
