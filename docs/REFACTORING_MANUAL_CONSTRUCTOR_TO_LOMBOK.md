## 수동 생성자 48개 → @RequiredArgsConstructor 일괄 리팩토링

### 배경 및 문제정의
- **상황**: Spring Bean 클래스(Service, Controller, Repository)에서 생성자 주입을 사용하되, 수동 `this.xxx = xxx` 생성자와 Lombok `@RequiredArgsConstructor`가 혼재
- **문제**: 48개 파일이 수동 생성자를 사용 중. 프로젝트 내 일관성 위반, 불필요한 보일러플레이트 코드 증가

### 기술 선정 (대안 비교 테이블)

| 항목 | @RequiredArgsConstructor | 수동 생성자 | @Autowired 필드 주입 |
|------|------------------------|-----------|-------------------|
| Spring 권장 여부 | 권장 (생성자 주입) | 권장 (생성자 주입) | 비권장 |
| 불변성 보장 | final 필드 강제 | final 가능 | final 불가 |
| 보일러플레이트 | 최소 (1줄) | 파라미터 수 × 2줄 | 최소 |
| 테스트 용이성 | 생성자로 Mock 주입 | 동일 | 리플렉션 필요 |
| @Nullable 지원 | 필드 어노테이션 복사 | 직접 명시 | @Autowired(required=false) |
| 일관성 | 프로젝트 표준 | 혼재 시 일관성 깨짐 | 다른 패턴 |

### 분석
- **초기 판단**: `@Nullable` 의존성이 있는 클래스에서 수동 생성자가 필요하다고 판단하여 수동으로 작성한 것으로 추정
- **실제 원인**: Lombok `@RequiredArgsConstructor`는 `@Nullable` 어노테이션을 생성자 파라미터에 자동 복사함. 수동 생성자가 필요 없었음
- **CS 원리**: Lombok은 컴파일 타임에 AST를 조작하여 생성자를 생성한다. `@Nullable`, `@NonNull` 등 JSR-305/Spring 어노테이션은 필드에서 파라미터로 전파된다. Spring의 `AutowiredAnnotationBeanPostProcessor`는 생성자 파라미터의 `@Nullable`을 확인하여 해당 Bean이 없으면 null을 주입한다.

### 솔루션

```java
// Before (수동 생성자 — 15줄)
@Slf4j
@Service
public class AiQueryApplicationServiceImpl {
    private final NaturalLanguageQueryService queryService;
    private final BriefingService briefingService;
    @Nullable
    private final AiDecisionLogConsumer aiDecisionLogConsumer;

    public AiQueryApplicationServiceImpl(
            NaturalLanguageQueryService queryService,
            BriefingService briefingService,
            @Nullable AiDecisionLogConsumer aiDecisionLogConsumer) {
        this.queryService = queryService;
        this.briefingService = briefingService;
        this.aiDecisionLogConsumer = aiDecisionLogConsumer;
    }
}

// After (@RequiredArgsConstructor — 생성자 자동 생성)
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQueryApplicationServiceImpl {
    private final NaturalLanguageQueryService queryService;
    private final BriefingService briefingService;
    @Nullable
    private final AiDecisionLogConsumer aiDecisionLogConsumer;
}
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| 수동 생성자 파일 수 | 48개 | 0개 |
| 제거된 보일러플레이트 라인 | ~350줄 | 0줄 |
| @RequiredArgsConstructor 사용률 | 약 60% | 100% |
| 빌드 결과 | 성공 | 성공 |

### 영향 범위

- Application Service: 14개 파일
- Controller: 14개 파일
- Repository (Infrastructure): 19개 파일
- 기타 (JwtTokenProvider, BusinessMetrics): 1개 파일
