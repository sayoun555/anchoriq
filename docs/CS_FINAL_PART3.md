# Part 3: Spring Boot, DDD, 인증

---

# Chapter 4: Spring Boot

## 4.1 프레임워크(Framework)란?

**프레임워크란, 소프트웨어 개발에 필요한 기본 구조와 도구를 미리 제공하는 뼈대입니다.**

집을 지을 때 비유하면: 프레임워크 없이 개발하는 것은 벽돌을 직접 구워서 집을 짓는 것이고, 프레임워크를 사용하는 것은 이미 기둥과 벽이 세워진 건물 골조 위에 인테리어만 하는 것입니다.

## 4.2 Spring Boot란?

**Spring Boot란, Java로 웹 애플리케이션을 빠르게 만들 수 있게 해주는 프레임워크입니다.**

웹 서버 설정, 데이터베이스 연결, 보안, JSON 처리 등을 자동으로 해주므로, 개발자는 비즈니스 로직(AI 질의, 리스크 분석)에만 집중할 수 있습니다.

## 4.3 어노테이션(Annotation)이란?

**어노테이션이란, Java 코드에 붙이는 특수한 표시(@)로, "이 코드를 이렇게 처리해줘"라고 프레임워크에게 지시하는 것입니다.**

```java
@RestController       // "이 클래스는 HTTP 요청을 처리하는 컨트롤러야"
@RequestMapping("/api/ai")  // "이 컨트롤러는 /api/ai로 시작하는 URL을 담당해"
public class AiQueryController {

    @PostMapping("/query")  // "POST /api/ai/query 요청이 오면 이 메서드를 실행해"
    public ResponseEntity query(
        @RequestBody AiQueryRequest request,  // "HTTP 본문의 JSON을 이 객체로 변환해"
        @AuthenticationPrincipal UserPrincipal user  // "JWT에서 사용자 정보를 넣어줘"
    ) { ... }
}

어노테이션이 없으면?
→ URL 매핑, JSON 변환, 인증 처리를 모두 직접 코드로 작성해야 합니다.
→ 수백 줄의 보일러플레이트 코드가 필요합니다.
```

## 4.4 IoC(Inversion of Control, 제어의 역전)란?

**IoC란, 객체의 생성과 관리를 개발자가 아닌 프레임워크(Spring)가 담당하는 것입니다.**

```
IoC 없이 (개발자가 제어):
  나한테 필요한 것을 내가 직접 만든다.
  
  class AiQueryController {
      private AiService service = new AiServiceImpl(
          new OpenClawClient(...),
          new Neo4jClient(...)
      );
      // 내가 직접 new로 생성. 의존성이 바뀌면 여기도 수정해야 함.
  }

IoC 있이 (프레임워크가 제어):
  나한테 필요한 것을 프레임워크에게 "이것 좀 줘"라고 선언만 한다.
  
  class AiQueryController {
      private final AiService service;  // "이것이 필요해"라고 선언만
      // Spring이 알아서 적절한 구현체를 찾아서 넣어줌
  }
```

"제어의 역전"이란 이름은, 객체를 만드는 제어권이 "개발자 → 프레임워크"로 넘어갔기 때문입니다.

## 4.5 DI(Dependency Injection, 의존성 주입)란?

**DI란, 객체가 필요로 하는 의존성(다른 객체)을 외부에서 넣어주는 것입니다. IoC를 구현하는 구체적인 방법입니다.**

**의존성(Dependency)이란?** 어떤 객체가 동작하기 위해 필요한 다른 객체입니다. AiQueryController가 동작하려면 AiQueryApplicationService가 필요합니다. 이때 AiQueryApplicationService가 AiQueryController의 "의존성"입니다.

**주입(Injection)이란?** 외부에서 객체를 만들어서 넣어주는 것입니다.

```java
@RequiredArgsConstructor  // 생성자를 자동으로 만들어주는 Lombok 어노테이션
public class AiQueryController {
    private final AiQueryApplicationService service;
    //         ↑ 이것이 "의존성"
    //         Spring이 이 인터페이스의 구현체를 찾아서 "주입"해줌
}
```

## 4.6 Bean(빈)이란?

**Bean이란, Spring이 생성하고 관리하는 객체입니다.**

Spring Boot가 시작되면, `@Component`, `@Service`, `@Repository`, `@Controller` 등의 어노테이션이 붙은 클래스를 찾아 객체를 만들고, 이 객체들의 의존성을 연결합니다. 이렇게 Spring이 관리하는 객체를 Bean이라고 합니다.

## 4.7 DispatcherServlet(디스패처 서블릿)이란?

**DispatcherServlet이란, 모든 HTTP 요청을 가장 먼저 받아서 적절한 Controller로 전달하는 Spring MVC의 중앙 허브입니다.**

공항의 안내 데스크와 같습니다. 모든 승객(요청)이 안내 데스크(DispatcherServlet)에 먼저 가면, "A 게이트로 가세요"(Controller A), "B 게이트로 가세요"(Controller B)처럼 적절한 곳으로 안내합니다.

```
모든 HTTP 요청 → DispatcherServlet → 적절한 Controller 메서드 실행

POST /api/ai/query    → AiQueryController.query()
GET  /api/ai/briefing → AiBriefingController.getDailyBriefing()
POST /api/auth/login  → AuthController.login()
```

## 4.8 Filter(필터)란?

**필터란, HTTP 요청이 Controller에 도달하기 전에 거치는 검문소입니다.**

요청을 검사하고, 통과시키거나, 차단하거나, 수정할 수 있습니다. 여러 필터가 체인(사슬)처럼 연결되어 순서대로 실행됩니다.

```
HTTP 요청 → [Filter 1: CORS 확인] → [Filter 2: JWT 검증] → [Filter 3: 권한 확인] → Controller

AnchorIQ의 필터 체인:
  ① CorsFilter: "이 요청이 허용된 출처(localhost:3004)에서 왔는가?"
  ② JwtAuthenticationFilter: "JWT 토큰이 유효한가? 사용자가 누구인가?"
  ③ AuthorizationFilter: "이 사용자가 이 API를 호출할 권한이 있는가?"
```

---

# Chapter 5: DDD (Domain-Driven Design)

## 5.1 DDD란?

**DDD(Domain-Driven Design, 도메인 주도 설계)란, 현실 세계의 비즈니스를 그대로 코드로 표현하는 소프트웨어 설계 방법론입니다.**

"도메인"이란 소프트웨어가 다루는 **비즈니스 영역**입니다. AnchorIQ의 도메인은 "해운 공급망 리스크"입니다. 현실에서 선박이 있고, 회사가 있고, 국가가 있고, 제재가 있듯이, 코드에도 `Vessel`, `Company`, `Country`, `Sanction`이 있습니다.

DDD의 핵심 원칙: **"비즈니스 로직은 Entity 안에 넣는다."**

```
현실: "선박이 자신의 리스크를 평가받는다"
코드: vessel.evaluateRiskScore(sanctionedCountries)
      → 선박(Entity)이 스스로 리스크를 계산 (현실과 일치!)

반대 패턴 (안 좋은 방식):
  RiskCalculator.calculate(vessel, company, country)
  → 외부 유틸리티가 계산 (현실과 동떨어짐)
  → 비즈니스 로직이 여기저기 흩어짐
```

## 5.2 Entity(엔티티)란?

**Entity란, 고유한 식별자(Identity)를 가진 도메인 객체입니다.**

쌍둥이도 주민등록번호가 다르면 다른 사람입니다. 마찬가지로, 선박의 이름이 바뀌어도 IMO 번호가 같으면 같은 선박입니다. 이처럼 **"식별자로 구분되는 것"**이 Entity입니다.

```
Vessel Entity의 식별자: IMO 번호 (전 세계 고유)
User Entity의 식별자: id (DB의 Primary Key)
```

Entity는 **비즈니스 로직을 가집니다.** 단순한 데이터 주머니가 아닙니다.

## 5.3 Value Object(값 객체)란?

**Value Object란, 고유한 식별자가 없고, 값 자체로 동등성을 판단하는 불변 객체입니다.**

만원짜리 두 장은 구분할 필요 없습니다. 둘 다 만 원의 가치를 가집니다. 마찬가지로 `Imo("9863297")` 두 개는 완전히 같은 것입니다. 누가 만들었는지, 언제 만들었는지 상관없이 값이 같으면 같습니다.

```java
public record Imo(String value) {
    public Imo {
        if (value == null || !value.matches("\\d{7}"))
            throw new IllegalArgumentException("IMO는 7자리 숫자여야 합니다");
    }
}

// 왜 String 대신 Imo를 쓰는가?
String imo = "hello";          // 컴파일 OK. 런타임에 문제 발생.
Imo imo = new Imo("hello");   // 즉시 예외! 잘못된 값이 만들어지지 않음.
```

## 5.4 DTO(Data Transfer Object)란?

**DTO란, 레이어 간에 데이터를 운반하기 위한 객체입니다. 비즈니스 로직이 없고, 순수하게 데이터만 담습니다.**

택배 상자와 같습니다. 내용물(데이터)을 담아서 이동시키는 것이 유일한 목적입니다.

```java
// 요청 DTO — 프론트엔드 → 백엔드
public record AiQueryRequest(@NotBlank String query) {}

// 응답 DTO — 백엔드 → 프론트엔드
public record AiQueryResponse(String answer, String cypher, List entities) {}
```

왜 Entity를 직접 반환하지 않고 DTO를 쓰는가?
- Entity에는 `password` 같은 민감 필드가 있을 수 있습니다
- Entity 구조가 바뀌면 API 응답도 바뀌어 프론트가 깨집니다
- API 버전별로 다른 형태의 응답을 제공할 수 있습니다

## 5.5 Aggregate Root(애그리거트 루트)란?

**Aggregate Root란, 관련된 Entity들을 하나로 묶은 그룹의 대장(진입점)입니다. 외부에서는 반드시 Aggregate Root를 통해서만 내부 Entity에 접근합니다.**

```
Vessel (Aggregate Root = 대장)
├── PortCall (입항 기록) — Vessel을 통해서만 접근
├── RouteHistory (항로 이력) — Vessel을 통해서만 접근
└── RiskAssessment (리스크 평가) — Vessel을 통해서만 접근

올바른 접근: vessel.recordPortCall(port)
잘못된 접근: portCallRepository.save(new PortCall(...))
```

## 5.6 Repository(레포지토리)란?

**Repository란, Entity를 저장하고 조회하는 창고 관리자 역할의 인터페이스입니다.**

Domain 레이어에서는 인터페이스(추상)만 정의하고, Infrastructure 레이어에서 구체적인 DB 구현을 합니다. 이렇게 하면 DB를 바꿔도 비즈니스 로직을 수정하지 않아도 됩니다.

## 5.7 DDD 4개 레이어

AnchorIQ의 모든 코드는 4개 레이어로 나뉩니다:

```
Controller ("문 앞 안내원")
  하는 일: HTTP 요청 수신 → Application Service에 위임 → HTTP 응답 반환
  하면 안 되는 일: 비즈니스 판단, DB 접근

Application Service ("지휘관")
  하는 일: 여러 서비스의 실행 순서 조율, 트랜잭션 관리
  하면 안 되는 일: 비즈니스 규칙 구현

Domain ("전문가")
  하는 일: 비즈니스 규칙과 로직 (리스크 계산, 상태 전환 검증)
  특징: 순수 Java만 사용, Spring 어노테이션 없음

Infrastructure ("실무자")
  하는 일: 실제 DB 저장, 외부 API 호출, JWT 처리
  특징: 프레임워크/라이브러리 의존
```

---

# Chapter 6: 인증과 보안

## 6.1 인증(Authentication)이란?

**인증이란, "이 사용자가 본인이 맞는지" 확인하는 과정입니다.**

공항에서 여권을 보여주는 것과 같습니다. "이 사람이 정말 김철수인가?" 실패 시: 401 Unauthorized

## 6.2 인가(Authorization)란?

**인가란, "이 사용자가 이 기능을 사용할 권한이 있는지" 확인하는 과정입니다.**

공항에서 퍼스트클래스 라운지에 들어가려는 것과 같습니다. 여권(인증)은 있지만, 퍼스트클래스 탑승권(권한)이 없으면 못 들어갑니다. 실패 시: 403 Forbidden

```
순서: 인증 먼저 → 인가 다음
  "너 누구야?" (인증) → "김철수구나" (인증 성공)
  → "근데 넌 이 기능 쓸 수 있어?" (인가) → "FREE 플랜이니 안 됨" (인가 실패, 403)
```

## 6.3 JWT(JSON Web Token)란?

**JWT란, 사용자의 인증 정보를 담은 디지털 토큰입니다. 서버가 발급하고, 이후 매 요청에 포함하여 "나는 인증된 사용자다"를 증명합니다.**

놀이공원의 손목 팔찌와 같습니다. 입장할 때(로그인) 팔찌(JWT)를 받고, 놀이기구(API)를 탈 때마다 팔찌를 보여줍니다. 팔찌에는 "이름, 등급, 유효기간"이 적혀있습니다.

### JWT의 구조

JWT는 점(.)으로 구분된 3개의 Base64URL 인코딩된 문자열입니다:

```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjV9.서명값

1. Header (헤더): {"alg": "HS256"}
   "이 토큰은 HMAC-SHA256 알고리즘으로 서명되었다"

2. Payload (내용): {"userId": 5, "email": "aitest@anchoriq.com", "role": "USER", "exp": 1712215375}
   사용자 정보와 만료 시간

3. Signature (서명): HMAC-SHA256(header + payload, 비밀키)
   위조 방지. 비밀키를 아는 서버만 만들 수 있음.
```

### 해시(Hash)란?

**해시란, 어떤 데이터를 고정 길이의 "지문"으로 변환하는 일방향 함수입니다. 입력에서 출력은 가능하지만, 출력에서 입력을 역추적하는 것은 불가능합니다.**

```
SHA-256("hello") → 2cf24dba5fb0a30e26e83b2ac5b9e29e...
SHA-256("hellp") → 완전히 다른 값 (1글자만 바뀌어도!)

특징:
  - 같은 입력 → 항상 같은 출력
  - 출력에서 입력을 알아낼 수 없음 (일방향)
  - 입력이 1비트만 달라도 출력이 완전히 달라짐
```

### JWT 서명 검증 과정

```
서버가 JWT를 받았을 때:

① header + payload 부분을 비밀키로 HMAC-SHA256 해시 재계산
② 계산한 해시값과 토큰의 signature를 비교
③ 같으면 → 이 토큰은 우리 서버가 발급한 진짜 토큰 ✅
④ 다르면 → 누군가 payload를 위조했음 ❌ → 401 반환

해커가 payload의 role을 "USER"에서 "ADMIN"으로 바꾸면?
→ payload가 변경되었으므로 해시를 재계산하면 결과가 달라짐
→ 하지만 해커는 비밀키를 모르므로 새 서명을 만들 수 없음
→ 서버가 검증 시 "서명 불일치" → 거부!
```

### 중요: JWT는 암호화가 아닙니다!

Base64는 "인코딩"(형식 변환)이지 "암호화"(비밀 보호)가 아닙니다. 누구나 Base64를 디코딩하면 payload를 읽을 수 있습니다. JWT가 보장하는 것은 **"이 내용이 변조되지 않았다"**(무결성)이지, "이 내용을 아무도 못 본다"(기밀성)가 아닙니다.

→ 그래서 JWT payload에는 userId, email, role 같은 식별 정보만 넣고, 비밀번호나 카드번호는 절대 넣지 않습니다.

## 6.4 BCrypt란?

**BCrypt란, 비밀번호를 안전하게 해시하기 위한 알고리즘으로, 의도적으로 느리게 설계되어 무차별 대입 공격을 방어합니다.**

```
왜 비밀번호를 해시하는가?
  DB가 해킹당해도 원래 비밀번호를 알 수 없도록 하기 위해.
  
왜 SHA-256이 아니라 BCrypt인가?
  SHA-256: 매우 빠름 → 해커가 1초에 1억 개 비밀번호 시도 가능
  BCrypt:  의도적으로 느림 → 해커가 1초에 10개만 시도 가능
  
  정상 사용자: 로그인 1번 → 0.1초 지연은 문제없음
  해커: 수백만 번 시도 → 0.1초 × 수백만 = 수년
```
