## 결제 Strategy 패턴: if-else 분기 제거 -> OCP 준수 통화 기반 게이트웨이 라우팅

### 배경 및 문제정의
- 상황: AnchorIQ는 글로벌 SaaS로 USD(Stripe)와 KRW(Toss Payments) 두 가지 결제 게이트웨이를 지원해야 한다. 향후 JPY(Paygent), EUR(Adyen) 등 추가 가능성이 있다.
- 문제: `if (currency == "KRW") { toss.charge() } else { stripe.charge() }` 식의 분기 코드는 게이트웨이 추가 시마다 기존 코드를 수정해야 하고, 결제 로직과 라우팅 로직이 뒤섞여 테스트와 유지보수가 어렵다.

### 기술 선정 (대안 비교 테이블)

| 항목 | if-else 분기 | Strategy 패턴 + Router |
|------|------------|----------------------|
| 새 게이트웨이 추가 시 | 기존 코드 수정 (OCP 위반) | 새 클래스 추가만 (OCP 준수) |
| 단위 테스트 | 모든 분기를 하나의 메서드에서 테스트 | 각 Strategy 독립 테스트 |
| 코드 복잡도 | 분기가 늘수록 cyclomatic complexity 증가 | 각 Strategy가 단일 책임 |
| 의존성 관리 | Stripe/Toss SDK가 하나의 클래스에 혼재 | 각 Strategy에 격리 |
| 보상 트랜잭션 | 분기마다 refund 로직 중복 | 인터페이스 통일로 일관된 처리 |

### 분석 / CS 원리

**OCP(Open-Closed Principle)**: 확장에는 열려있고 수정에는 닫혀있어야 한다. Strategy 패턴은 각 알고리즘(결제 게이트웨이)을 별도 클래스로 캡슐화하고, 공통 인터페이스를 통해 호출하므로 새 게이트웨이 추가 시 기존 코드를 변경하지 않는다.

**DIP(Dependency Inversion Principle)**: Application Service는 구체 클래스(StripePaymentGateway)가 아닌 인터페이스(PaymentGateway)에 의존한다. Router가 런타임에 통화 기반으로 적절한 구현체를 선택한다.

**트랜잭션 분리 원칙**: 외부 API(Stripe/Toss)를 @Transactional 안에서 호출하면 커넥션 풀이 고갈될 수 있다. 결제는 트랜잭션 밖에서 수행하고, 성공 후 별도 @Transactional에서 DB 저장, 실패 시 보상 트랜잭션(refund)을 실행한다.

### 솔루션 (핵심 코드 블록)

```java
// 1. 도메인 인터페이스 (core 모듈 - 인프라 의존 없음)
public interface PaymentGateway {
    PaymentGatewayResult charge(PaymentGatewayRequest request);
    void refund(String gatewayPaymentId);
    boolean verifyWebhookSignature(String payload, String signature);

    record PaymentGatewayRequest(BigDecimal amount, String currency,
                                  String description, String customerEmail) {}
    record PaymentGatewayResult(String gatewayPaymentId, boolean success, String message) {
        public static PaymentGatewayResult success(String id) { ... }
        public static PaymentGatewayResult failure(String msg) { ... }
    }
}
```

```java
// 2. 통화 기반 라우터 (infrastructure)
public class PaymentGatewayRouter {
    private final StripePaymentGateway stripePaymentGateway;
    private final TossPaymentGateway tossPaymentGateway;

    public PaymentGateway resolve(Currency currency) {
        return switch (currency) {
            case KRW -> tossPaymentGateway;
            case USD -> stripePaymentGateway;
            // 새 통화 추가 시 여기에 case만 추가
        };
    }
}
```

```java
// 3. @Configuration + @Bean으로 명시적 등록 (@Component 남발 방지)
@Configuration
public class PaymentConfig {
    @Bean
    public StripePaymentGateway stripePaymentGateway(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.webhook-secret}") String webhookSecret) {
        return new StripePaymentGateway(secretKey, webhookSecret);
    }
    @Bean
    public TossPaymentGateway tossPaymentGateway(...) { ... }
    @Bean
    public PaymentGatewayRouter paymentGatewayRouter(...) { ... }
}
```

```java
// 4. 보상 트랜잭션 패턴 (외부 API를 @Transactional 밖에서 호출)
public SubscriptionResponse processSubscription(Long userId, SubscribeRequest request) {
    PaymentGateway gateway = paymentGatewayRouter.resolve(currency);

    // Step 1: 외부 결제 (트랜잭션 밖)
    PaymentGatewayResult result = gateway.charge(new PaymentGatewayRequest(...));

    // Step 2: DB 저장 (별도 @Transactional Bean에서 처리)
    try {
        return paymentTransactionService.savePaymentAndActivateSubscription(...);
    } catch (Exception e) {
        // Step 3: 보상 트랜잭션 - DB 실패 시 환불
        gateway.refund(result.gatewayPaymentId());
        throw new PaymentFailedException("Refund initiated", e);
    }
}
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before (if-else) | After (Strategy + Router) |
|------|-----------------|--------------------------|
| 새 게이트웨이 추가 시 변경 파일 | 3~5개 (기존 코드 수정) | 1개 (새 클래스 추가) |
| 결제 로직 cyclomatic complexity | 8+ (분기 중첩) | 1 (각 Strategy) |
| 외부 API @Transactional 내 호출 | 있음 (커넥션 풀 위험) | 없음 (분리) |
| 단위 테스트 독립성 | 불가 (모든 SDK 모킹 필요) | 가능 (각 Strategy 독립) |
| 보상 트랜잭션 | 분기마다 중복 구현 | 인터페이스 통일 |
