## Lombok @RequiredArgsConstructor + @Nullable 생성자 생성 과정과 Spring Bean 주입

### 배경 및 문제정의
- **상황**: `CollectorController`에서 `@RequiredArgsConstructor` + `@Nullable CollectorRegistry` 조합 사용. Settings 페이지에서 `GET /api/collectors` 호출 시 404 NOT_FOUND 반환
- **증상**: 인증 없이 요청 → 401 (Security가 차단), 인증 후 요청 → 404 (핸들러 매핑 없음). 즉, Spring MVC에 `CollectorController`의 핸들러 매핑이 등록되지 않음
- **프론트 표시**: "Kafka 미연결 — 수집기를 사용하려면 Kafka를 시작하세요" (collectors API 실패 시 fallback 메시지)

### Lombok @RequiredArgsConstructor 생성자 생성 과정

#### 1. 컴파일 타임 AST 조작
Lombok은 Java 컴파일러(javac)의 Annotation Processing API를 사용하여 **컴파일 타임에 AST(Abstract Syntax Tree)를 직접 조작**한다.

```
소스 코드 파싱 → AST 생성 → Lombok Annotation Processor 실행 → AST 수정 → 바이트코드 생성
```

#### 2. @RequiredArgsConstructor의 필드 선택 기준
Lombok은 다음 조건에 해당하는 필드만 생성자 파라미터로 포함한다:
- `final` 필드 (초기화되지 않은 것)
- `@NonNull` 어노테이션이 붙은 non-final 필드

```java
@RequiredArgsConstructor
public class Example {
    private final String required;          // ✅ 생성자 포함 (final, 미초기화)
    private final String initialized = "";  // ❌ 제외 (final이지만 초기화됨)
    private String optional;                // ❌ 제외 (final 아님)
    @NonNull private String nonNull;        // ✅ 생성자 포함 (@NonNull)
    @Nullable private final String nullable; // ✅ 생성자 포함 (final, 미초기화)
}
```

#### 3. @Nullable 어노테이션 전파 (핵심)
Lombok은 필드의 어노테이션을 생성자 파라미터로 **자동 복사**한다. 단, 해당 어노테이션이 `@Target(ElementType.PARAMETER)`를 포함해야 한다.

```java
// org.springframework.lang.Nullable의 정의
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {}
```

→ `PARAMETER` 타겟이 있으므로 Lombok이 생성자 파라미터에 복사함.

#### 4. 생성된 바이트코드 (javap -v로 확인)
```
// CollectorController의 생성자
public com.anchoriq.api.controller.collector.CollectorController(
    com.anchoriq.core.domain.operation.collector.service.CollectorRegistry
);

RuntimeVisibleParameterAnnotations:
  parameter 0:
    0: org.springframework.lang.Nullable    ← 필드에서 복사됨
```

#### 5. Spring의 @Nullable 인식
Spring의 `AutowiredAnnotationBeanPostProcessor`는 생성자 파라미터의 `@Nullable`을 확인하여:
- `@Nullable` 있음 → 해당 타입 Bean이 없으면 **null 주입** (Bean 생성 성공)
- `@Nullable` 없음 → 해당 타입 Bean이 없으면 **UnsatisfiedDependencyException** (Bean 생성 실패)

### 분석: CollectorController 404 원인

#### Bean 체인 분석
```
CollectorController
  └── @Nullable CollectorRegistry (인터페이스)
        └── CollectorRegistryImpl (@ConditionalOnProperty: spring.kafka.bootstrap-servers)
              └── AisStreamClient (인터페이스)
                    └── AisStreamWebSocketClient (@ConditionalOnProperty: spring.kafka.bootstrap-servers)
                          └── AisKafkaProducer (@ConditionalOnProperty: spring.kafka.bootstrap-servers)
```

#### 확인된 사실
1. `spring.kafka.bootstrap-servers=localhost:29092` 설정 존재 → `@ConditionalOnProperty` 조건 충족
2. Kafka 9개 Consumer Group 정상 연결 확인 → Kafka 인프라 정상
3. `javap -v` 결과 `@Nullable`이 생성자 파라미터에 전파됨 → Lombok 동작 정상
4. `@SpringBootApplication(scanBasePackages = "com.anchoriq")` → 컴포넌트 스캔 범위 정상
5. 인증 후 요청 시 404 (`NoResourceFoundException`) → 컨트롤러 핸들러 매핑 미등록

#### 가능한 원인 (조사 중)
1. **Bean 생성 실패 사일런트 스킵**: `@PreAuthorize("hasRole('ADMIN')")` 클래스 레벨 적용 시 CGLIB 프록시 생성 과정에서 실패할 수 있음
2. **빌드 아티팩트 불일치**: 앱 시작 시점(12:16)과 클래스 컴파일 시점(17:20) 차이. 시작 시 해당 클래스가 없었을 가능성
3. **CollectorRegistry 빈 생성 실패**: `AisStreamClient` → `AisStreamWebSocketClient` 체인에서 숨겨진 실패

### 검증 방법
```bash
# 1. actuator beans 엔드포인트 활성화 후 확인
# application-local.yml:
management.endpoints.web.exposure.include: health,prometheus,info,beans

# 2. 앱 재시작 후 시작 로그에서 확인
grep -i "collector" logs/spring.log

# 3. 디버그 로그로 Bean 생성 과정 추적
logging.level.org.springframework.beans.factory: DEBUG
```

### 핵심 CS 원리

| 개념 | 설명 |
|------|------|
| AST (Abstract Syntax Tree) | 소스 코드의 구문 구조를 트리로 표현. Lombok은 이를 직접 조작하여 코드 생성 |
| Annotation Processing | Java 컴파일러가 어노테이션을 처리하는 확장 포인트. Lombok은 이를 활용 |
| @Target 메타 어노테이션 | 어노테이션이 적용될 수 있는 위치를 제한. Lombok의 전파 기준 |
| CGLIB Proxy | Spring이 `@PreAuthorize` 등 AOP를 적용하기 위해 생성하는 서브클래스 프록시 |
| @ConditionalOnProperty | Spring Boot가 프로퍼티 존재/값을 기준으로 Bean 생성 여부를 결정하는 조건부 등록 |
