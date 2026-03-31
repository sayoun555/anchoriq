## 인터페이스 기반 Application Service 설계: concrete class 직접 의존 → 인터페이스 추출로 DIP 달성

### 배경 및 문제정의
- 상황: AnchorIQ 프로젝트 초기에 23개 Application Service가 전부 concrete class로만 존재했다. Controller가 `AuthApplicationServiceImpl` 같은 구현체를 직접 주입받아 사용하고 있었다.
- 문제:
  - **DIP(의존성 역전 원칙) 위반**: 상위 모듈(Controller)이 하위 모듈(Service 구현체)에 직접 의존. 구현 변경 시 Controller까지 영향을 받는 구조.
  - **테스트 시 Mock 어려움**: 인터페이스가 없으면 Mockito로 Mock 객체를 만들 때 concrete class를 Mock해야 하므로, final 메서드나 생성자 의존성 문제가 발생한다.
  - **구현체 교체 불가**: 프로필별(local/docker/test) 다른 구현체를 주입하는 것이 불가능하다. 예를 들어 테스트 환경에서 외부 API를 호출하지 않는 Stub 구현체로 교체할 수 없다.

### 기술 선정 (대안 비교 테이블)

| 항목 | Concrete Class 유지 | 인터페이스 추출 + Impl 패턴 |
|------|--------------------|-----------------------------|
| DIP 준수 | X - 구현체 직접 의존 | O - 추상화에 의존 |
| Mock 테스트 | 어려움 (concrete Mock) | 용이 (인터페이스 Mock) |
| 구현체 교체 | 불가 (클래스 변경 필요) | @Profile, @Conditional로 자유 교체 |
| 코드량 | 적음 (파일 1개) | 약간 증가 (인터페이스 + 구현체) |
| Spring DI 활용 | 제한적 | 최대화 (다형성 활용) |
| 초기 설계 비용 | 낮음 | 약간 높음 |

**선택: 인터페이스 추출 + Impl 패턴**. 코드량이 약간 증가하지만, DIP 준수와 테스트 용이성이 포트폴리오 프로젝트의 설계 품질을 결정한다. Spring Framework 자체가 인터페이스 기반 DI를 권장하며, 이는 OCP(개방-폐쇄 원칙)와도 부합한다.

### 분석 / CS 원리

**의존성 역전 원칙 (Dependency Inversion Principle)**

Robert C. Martin의 SOLID 원칙 중 DIP는 "고수준 모듈이 저수준 모듈에 의존하면 안 된다. 둘 다 추상화에 의존해야 한다"고 말한다.

```
[Before] Controller → ServiceImpl (고수준이 저수준에 직접 의존)
[After]  Controller → Service(interface) ← ServiceImpl (둘 다 추상화에 의존)
```

Spring의 DI 컨테이너는 인터페이스 타입으로 Bean을 조회하므로, 인터페이스를 기준으로 설계하면:
1. **JDK Dynamic Proxy** 생성이 가능해져 AOP(@Transactional, @Cacheable 등)가 더 자연스럽게 동작한다.
2. **Bean 교체**가 `@Profile`이나 `@ConditionalOnProperty`만으로 가능해진다.
3. **테스트 격리**가 쉬워진다 — Mockito는 인터페이스에 대해 더 안정적으로 Proxy를 생성한다.

### 솔루션 (핵심 코드 블록)

**1. 인터페이스 정의** — 계약(Contract)만 선언한다:

```java
// AuthApplicationService.java (인터페이스)
package com.anchoriq.api.application.auth;

public interface AuthApplicationService {

    UserResponse signup(SignupRequest request);

    AuthTokenResponse login(LoginRequest request);

    AuthTokenResponse refresh(String refreshToken);

    void logout(Long userId);

    UserResponse getMe(Long userId);
}
```

**2. 구현체** — 인터페이스를 구현하고 비즈니스 오케스트레이션 로직을 담는다:

```java
// AuthApplicationServiceImpl.java (구현체)
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationServiceImpl implements AuthApplicationService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFactory subscriptionFactory;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateException("Email already registered: " + request.email());
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(Email.of(request.email()), encodedPassword, request.name());
        User savedUser = userRepository.save(user);

        // Free 플랜 구독 자동 생성 — Factory 패턴 활용
        Subscription subscription = subscriptionFactory.createFreeSubscription(savedUser.getId());
        subscriptionRepository.save(subscription);

        log.info("User registered: email={}", request.email());
        return UserResponse.from(savedUser);
    }

    // ... login, refresh, logout, getMe 구현
}
```

**3. Controller는 인터페이스에만 의존**:

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 인터페이스 타입으로 주입 — 구현체를 모른다
    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }
}
```

**적용 범위**: 30개 Application Service 인터페이스를 추출하고, 동일한 패턴을 전체 도메인에 적용했다.

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| Application Service 인터페이스 수 | 0개 | 30개 |
| DIP 위반 Controller | 23개 (100%) | 0개 (0%) |
| Mock 테스트 작성 난이도 | concrete Mock 필요 | 인터페이스 Mock으로 즉시 가능 |
| 구현체 교체 가능 여부 | 불가 (하드코딩) | @Profile/@Conditional로 자유 교체 |
| Spring AOP 호환성 | CGLIB Proxy (제한적) | JDK Dynamic Proxy (완전 호환) |
| 의존성 방향 | Controller → Impl (구현 의존) | Controller → Interface ← Impl (추상화 의존) |
