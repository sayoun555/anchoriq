# AnchorIQ — 인증 + 결제 설계

> 이메일/비밀번호 로그인 + Stripe/Toss 멀티 결제 게이트웨이 + 구독 플랜

---

## 목차
- [인증](#인증)
- [결제](#결제)
- [구독 플랜](#구독-플랜)

---

## 인증

### 방식

| 항목 | 결정 |
|------|------|
| 로그인 | 이메일 + 비밀번호 |
| 토큰 | JWT (Access + Refresh) |
| 보안 | Spring Security |
| OAuth2 | 미적용 (추후 확장 가능) |

### 흐름

```
[회원가입] → 이메일/비밀번호 → BCrypt 해싱 → PostgreSQL 저장
[로그인]   → 이메일/비밀번호 → 검증 → JWT Access Token (15분) + Refresh Token (7일) 발급
[인증]     → Authorization: Bearer {accessToken} → Spring Security Filter 검증
[갱신]     → Refresh Token → 새 Access Token 발급
```

### 환경변수

```properties
# .env (절대 커밋 금지)
JWT_SECRET=${JWT_SECRET}
JWT_ACCESS_EXPIRATION=900000       # 15분
JWT_REFRESH_EXPIRATION=604800000   # 7일
```

---

## 결제

### 멀티 결제 게이트웨이

> 해외 고객 → Stripe (USD, 글로벌 카드)
> 한국 고객 → Toss Payments (KRW, 한국 카드)

둘 다 테스트 모드 → 실제 돈 안 나감.

### Strategy 패턴 적용

```java
// 인터페이스
public interface PaymentGateway {
    PaymentResult processPayment(PaymentRequest request);
    PaymentResult cancelPayment(String paymentId);
    SubscriptionResult createSubscription(SubscriptionRequest request);
    SubscriptionResult cancelSubscription(String subscriptionId);
}

// Stripe 구현체
@Component
public class StripeGateway implements PaymentGateway { ... }

// Toss 구현체
@Component
public class TossGateway implements PaymentGateway { ... }

// 분기 로직
@Component
public class PaymentGatewayRouter {
    public PaymentGateway resolve(Currency currency) {
        return switch (currency) {
            case KRW -> tossGateway;
            default -> stripeGateway;
        };
    }
}
```

### 환경변수

```properties
# .env (절대 커밋 금지)
STRIPE_SECRET_KEY=${STRIPE_SECRET_KEY}
STRIPE_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}
TOSS_SECRET_KEY=${TOSS_SECRET_KEY}
TOSS_WEBHOOK_SECRET=${TOSS_WEBHOOK_SECRET}
```

---

## 구독 플랜

| 기능 | Free | Pro | Enterprise |
|------|------|-----|------------|
| 대시보드 열람 | ✅ | ✅ | ✅ |
| AI 질의 | 일 5건 | 무제한 | 무제한 |
| 실시간 알림 | ❌ | ✅ | ✅ |
| n8n 워크플로우 | ❌ | ✅ | ✅ |
| What-if 시뮬레이션 | ❌ | ❌ | ✅ |
| API 접근 | ❌ | ❌ | ✅ |
| 커스텀 온톨로지 | ❌ | ❌ | ✅ |

### 플랜 제한 체크

```
Free 플랜 "일 5건 AI 질의" 체크 흐름:

1. API 요청 들어옴
2. Spring Security Filter → JWT에서 userId 추출
3. Redis INCR → api:usage:{userId}:{date} 증가
4. 5건 초과? → 403 Forbidden + "Pro 플랜으로 업그레이드하세요"
5. 자정에 카운터 리셋 (Redis TTL)
```

### Domain Service

```java
// 구독 도메인 서비스 — 플랜별 기능 제한 판단
public class SubscriptionDomainService {
    public boolean canUseFeature(User user, Feature feature) {
        return user.getSubscription().getPlan().supports(feature);
    }
}
```

---

## 외부 API 키 관리 (Enterprise)

> Enterprise 플랜 사용자에게 외부 연동용 API 키 발급

### 흐름

```
1. Enterprise 구독 확인
2. POST /api/external/api-key/regenerate → 키 생성
3. 키 해시만 DB 저장 (원본은 최초 1회만 표시)
4. 외부 요청 시 Authorization: Bearer {apiKey} → 해시 대조
5. 요청마다 last_used_at 갱신
```

### 보안

```
- API 키 원본은 DB에 저장하지 않음 (BCrypt 해싱)
- 키 재발급 시 이전 키 즉시 무효화
- Rate Limiting 별도 적용 (분당 60건)
```

### 환경변수

```properties
# .env (절대 커밋 금지)
API_KEY_SECRET=${API_KEY_SECRET}
```
