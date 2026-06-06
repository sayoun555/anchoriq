# AnchorIQ CS 완전 가이드 — Part 3: Spring Boot와 DDD

---

# Chapter 4: Spring Boot — 웹 애플리케이션의 뼈대

## 4.1 Spring Boot가 해결하는 문제

웹 애플리케이션을 만들려면 수많은 것들이 필요합니다:

```
□ 웹 서버 (HTTP 요청을 받아야 하니까)
□ 라우팅 (어떤 URL이 어떤 코드로 가야 하는지)
□ JSON 파싱 (요청 데이터를 자바 객체로 바꿔야 하니까)
□ 데이터베이스 연결 (데이터를 저장해야 하니까)
□ 커넥션 풀 (DB 연결을 효율적으로 관리)
□ 보안 (인증, 인가, 암호화)
□ 로깅 (뭐가 일어나는지 기록)
□ 설정 관리 (환경별 다른 설정)
□ 에러 처리 (예외가 터졌을 때)
□ ... 수십 가지 더
```

이것을 하나하나 직접 구현하면 "비즈니스 로직(AI 질의, 리스크 분석)"을 작성하기도 전에 수개월이 걸립니다.

**Spring Boot는 위의 모든 것을 "자동 설정(Auto Configuration)"으로 제공합니다.**

```
직접 하면:
  ① Tomcat 다운로드 → 설치 → 설정 파일 작성 → WAR 빌드 → 배포
  ② JDBC 드라이버 로드 → Connection 생성 → Statement 생성 → ResultSet 처리 → Connection 반환
  ③ Jackson 라이브러리 설정 → ObjectMapper 생성 → 날짜 포맷 설정 → ...
  
Spring Boot 쓰면:
  ① build.gradle에 의존성 한 줄 추가
  ② application.yml에 설정 몇 줄
  ③ 끝. 나머지는 자동.
```

---

## 4.2 IoC와 DI — Spring의 핵심 원리

### IoC (Inversion of Control) — 제어의 역전

"제어의 역전"이라는 이름이 어려운데, 사실 간단한 개념입니다.

```
일반적인 프로그래밍 (개발자가 제어):

  내가 필요한 것을 내가 직접 만듭니다.
  
  class AiQueryController {
      // 내가 직접 new로 생성
      private AiQueryService service = new AiQueryServiceImpl();
      
      // AiQueryServiceImpl 안에서도 직접 생성
      // private Neo4jClient client = new Neo4jClient("bolt://localhost:7687");
      // private OpenClawClient ai = new OpenClawClient("http://127.0.0.1:18789");
      // private RedisTemplate redis = new RedisTemplate(...);
  }
  
  문제점:
  1. AiQueryServiceImpl이 변경되면 Controller도 수정해야 함
  2. 테스트할 때 진짜 Neo4j, Redis가 있어야 함 (단위 테스트 불가)
  3. 객체 생성 순서를 개발자가 관리해야 함 (A가 B를 쓰고, B가 C를 쓰고...)
  

IoC (Spring이 제어):

  내가 필요한 것을 Spring에게 "이것 좀 줘"라고 요청합니다.
  Spring이 알아서 만들어서 넣어줍니다.
  
  @RestController
  @RequiredArgsConstructor  // "이 필드들을 누군가 넣어줘"
  class AiQueryController {
      private final AiQueryApplicationService service;
      // ↑ Spring이 알아서 AiQueryApplicationServiceImpl을 넣어줌
      // 내가 new를 하지 않음!
  }
  
  장점:
  1. Controller는 "인터페이스"만 알면 됨 (구현체가 뭔지 몰라도 됨)
  2. 테스트할 때 Mock 객체를 쉽게 넣을 수 있음
  3. 객체 생성/소멸을 Spring이 자동 관리
```

왜 "역전"이라고 부르는가?

```
원래: 개발자가 객체를 생성하고, 의존성을 연결하는 "제어권"을 가짐
역전: Spring 컨테이너가 이 "제어권"을 가져감
     개발자는 "이것이 필요해"라고 선언만 함

제어의 방향이 "개발자 → 프레임워크"에서 "프레임워크 → 개발자"로 뒤집힘.
이것이 "제어의 역전(Inversion of Control)".
```

### DI (Dependency Injection) — 의존성 주입

DI는 IoC를 구현하는 구체적인 방법입니다. "필요한 것을 외부에서 넣어주는 것"입니다.

AnchorIQ의 실제 코드로 봅시다:

```java
// AiQueryController.java
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor            // ← Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
public class AiQueryController {
    
    private final AiQueryApplicationService aiQueryApplicationService;
    // ↑ 인터페이스 타입으로 선언. 구현체가 뭔지 Controller는 모름.
    
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<AiQueryResponse>> query(
            @RequestBody AiQueryRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        Map<String, Object> result = 
            aiQueryApplicationService.handleQuery(request.query(), user.userId());
        // ↑ 인터페이스의 메서드를 호출. 실제로는 AiQueryApplicationServiceImpl이 실행됨.
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```

Lombok의 `@RequiredArgsConstructor`가 자동으로 생성하는 코드:

```java
// Lombok이 컴파일 시 자동 생성하는 생성자
public AiQueryController(AiQueryApplicationService aiQueryApplicationService) {
    this.aiQueryApplicationService = aiQueryApplicationService;
}

// Spring이 이 생성자를 보고:
// "아, AiQueryApplicationService 구현체가 필요하구나.
//  내가 등록된 Bean 중에서 이 인터페이스를 구현한 걸 찾아서 넣어줄게."
// → AiQueryApplicationServiceImpl을 찾아서 주입
```

### Bean — Spring이 관리하는 객체

Spring이 생성하고 관리하는 객체를 "Bean"이라고 합니다. Bean으로 등록하는 방법:

```java
// 방법 1: @Component 계열 어노테이션
@Controller   // 웹 요청을 받는 클래스
@Service      // 비즈니스 로직 클래스
@Repository   // 데이터 접근 클래스
@Component    // 기타 클래스

// 방법 2: @Configuration + @Bean
@Configuration
public class OpenClawConfig {
    @Bean
    public WebClient openClawWebClient() {
        return WebClient.builder()
            .baseUrl("http://127.0.0.1:18789")
            .defaultHeader("Authorization", "Bearer " + token)
            .build();
    }
    // 이 메서드가 반환하는 WebClient 객체가 Bean으로 등록됨
}
```

### Bean 생명주기 — Spring 시작 시 무슨 일이 벌어지는가

```
./gradlew :anchoriq-api:bootRun 실행 시:

[1단계] 컴포넌트 스캔 (Component Scan)
  Spring이 프로젝트의 모든 클래스를 스캔합니다.
  @Component, @Service, @Repository, @Controller, @Configuration이
  붙은 클래스를 찾아 "Bean 후보"로 등록합니다.
  
  AnchorIQ에서 찾는 Bean들:
  - AiQueryController (@RestController)
  - AiQueryApplicationServiceImpl (@Service)
  - NaturalLanguageQueryServiceImpl (@Service)
  - OpenClawClient (@Component)
  - Neo4jVesselRepository (@Repository)
  - SecurityConfig (@Configuration)
  - JwtAuthenticationFilter (@Component)
  - ... 수십 개

[2단계] 의존성 그래프 분석
  각 Bean이 어떤 다른 Bean을 필요로 하는지 분석합니다.
  
  AiQueryController
    └→ AiQueryApplicationService (인터페이스)
        → AiQueryApplicationServiceImpl을 찾음
           └→ NaturalLanguageQueryService
              → NaturalLanguageQueryServiceImpl
                 └→ AiClient (인터페이스)
                    → OpenClawClient를 찾음
                       └→ WebClient (openClawWebClient Bean)
                 └→ Neo4jClient
           └→ SubscriptionService
           └→ StringRedisTemplate

[3단계] Bean 인스턴스 생성 (의존성 트리의 잎부터)
  
  ① WebClient 생성 (의존하는 것 없음 — 잎 노드)
  ② Neo4jClient 생성
  ③ OpenClawClient 생성 (WebClient 주입)
  ④ NaturalLanguageQueryServiceImpl 생성 (OpenClawClient, Neo4jClient 주입)
  ⑤ SubscriptionServiceImpl 생성
  ⑥ StringRedisTemplate 생성
  ⑦ AiQueryApplicationServiceImpl 생성 (④⑤⑥ 주입)
  ⑧ AiQueryController 생성 (⑦ 주입)
  
  가장 안쪽(의존성이 없는 것)부터 바깥쪽(의존성이 많은 것)으로 생성.
  마치 레고를 조립할 때 작은 블록부터 끼우는 것과 같습니다.

[4단계] 초기화 콜백
  @PostConstruct가 붙은 메서드 실행.
  "Bean이 다 만들어진 후 해야 할 초기화 작업"

[5단계] 서버 시작
  내장 Tomcat이 8080(또는 설정된) 포트에서 LISTEN 시작.
  
  콘솔 출력:
  "Started AnchoriqApplication in 15.2s (process running for 16.1s)"
  
  이 시점부터 HTTP 요청을 받을 수 있습니다.
```

---

## 4.3 Spring MVC 요청 처리 파이프라인

HTTP 요청이 Controller에 도달하기까지 여러 단계를 거칩니다. 공항의 입국 심사와 비슷합니다.

```
비행기 착륙 (HTTP 요청 도착)
    │
    ▼
세관 검사 (Filter Chain)
    "위험물이 없는지, 입국 자격이 있는지 확인"
    │
    ▼
입국 심사대 (DispatcherServlet)
    "어느 게이트로 안내할지 결정"
    │
    ▼
해당 게이트 (Controller 메서드)
    "실제 업무 처리"
    │
    ▼
출국 심사 (응답 변환)
    "결과를 포장해서 내보냄"
```

### 상세 단계

```
[1] Tomcat이 HTTP 요청을 수신
    
    Tomcat은 Spring Boot에 내장된 웹 서버입니다.
    8080 포트에서 TCP 소켓을 LISTEN하고 있다가,
    TCP 연결이 들어오면 HTTP 메시지를 파싱합니다.
    
    TCP 바이트 스트림:
    "POST /api/ai/query HTTP/1.1\r\nHost: localhost:8080\r\n..."
    
    이것을 Java의 HttpServletRequest 객체로 변환합니다:
    - getMethod() → "POST"
    - getRequestURI() → "/api/ai/query"
    - getHeader("Content-Type") → "application/json"
    - getCookies() → [Cookie(name="access_token", value="eyJ...")]
    - getInputStream() → {"query": "호르무즈..."} (바이트 스트림)


[2] Filter Chain (필터 체인)
    
    필터는 "요청이 Controller에 도달하기 전에 거치는 검문소"입니다.
    여러 개의 필터가 순서대로 실행됩니다.
    각 필터는 요청을 검사하고, 통과시키거나 차단할 수 있습니다.
    
    AnchorIQ의 필터 실행 순서:
    
    ┌─────────────────────────────────────────────────┐
    │ Filter 1: CorsFilter                             │
    │                                                  │
    │ Origin 헤더를 확인합니다.                          │
    │ "이 요청이 허용된 출처에서 온 것인가?"               │
    │                                                  │
    │ Origin: http://localhost:3004                     │
    │ → SecurityConfig의 allowedOrigins에 포함됨 → 통과 │
    │                                                  │
    │ 응답 헤더에 CORS 관련 헤더를 추가합니다:            │
    │ Access-Control-Allow-Origin: http://localhost:3004│
    │ Access-Control-Allow-Credentials: true            │
    └───────────────────┬─────────────────────────────┘
                        │ 통과
                        ▼
    ┌─────────────────────────────────────────────────┐
    │ Filter 2: JwtAuthenticationFilter (커스텀)       │
    │                                                  │
    │ JWT 토큰을 추출하고 검증합니다.                    │
    │                                                  │
    │ 순서:                                            │
    │ ① Cookie에서 "access_token" 찾기                 │
    │   → Cookie: access_token=eyJhbGciOiJIUzI1NiJ9...│
    │   → 토큰 발견!                                   │
    │                                                  │
    │ ② 토큰 유효성 검증                               │
    │   - HMAC-SHA256 서명 검증 (비밀키로 재계산)       │
    │   - 만료 시간(exp) 확인                          │
    │   - 토큰 타입이 "access"인지 확인                 │
    │     (refresh 토큰으로 API 호출하는 것 방지)       │
    │                                                  │
    │ ③ 토큰에서 사용자 정보 추출                       │
    │   - userId: 5                                    │
    │   - email: "aitest@anchoriq.com"                 │
    │   - role: "USER"                                 │
    │                                                  │
    │ ④ UserPrincipal 객체 생성                        │
    │   new UserPrincipal(5, "aitest@...", "USER")     │
    │                                                  │
    │ ⑤ Spring SecurityContext에 인증 정보 저장         │
    │   SecurityContextHolder                          │
    │     .getContext()                                 │
    │     .setAuthentication(authToken);               │
    │                                                  │
    │ ⑥ 다음 필터로 전달                               │
    │   filterChain.doFilter(request, response);       │
    │                                                  │
    │ ※ 토큰이 없거나 유효하지 않으면?                  │
    │   → 인증 정보를 설정하지 않고 그냥 다음으로 넘김   │
    │   → 나중에 AuthorizationFilter에서 401 처리      │
    └───────────────────┬─────────────────────────────┘
                        │ 통과
                        ▼
    ┌─────────────────────────────────────────────────┐
    │ Filter 3: AuthorizationFilter                    │
    │                                                  │
    │ SecurityContext에 인증 정보가 있는지 확인합니다.    │
    │                                                  │
    │ /api/ai/query는 SecurityConfig에서:              │
    │   .anyRequest().authenticated()                  │
    │ 로 설정되어 있으므로 인증이 필요합니다.             │
    │                                                  │
    │ SecurityContext에 UserPrincipal이 있음 → 통과!    │
    │                                                  │
    │ 만약 없었다면 (토큰이 없거나 만료):               │
    │ → authenticationEntryPoint가 호출되어            │
    │   401 JSON 응답을 바로 반환하고 여기서 끝남       │
    └───────────────────┬─────────────────────────────┘
                        │ 통과
                        ▼

[3] DispatcherServlet (프론트 컨트롤러)
    
    모든 HTTP 요청이 도달하는 중앙 허브입니다.
    Spring MVC의 핵심 컴포넌트입니다.
    
    하는 일:
    ① HandlerMapping에게 "이 URL을 처리할 Controller를 찾아줘" 요청
    
       RequestMappingHandlerMapping이 어노테이션을 스캔:
       POST /api/ai/query
       → @RestController @RequestMapping("/api/ai") 
         → @PostMapping("/query")
       → AiQueryController.query() 메서드를 찾음!
    
    ② HandlerAdapter에게 "이 메서드를 실행해줘" 요청
    
       RequestMappingHandlerAdapter가 메서드 실행을 준비:
       
       a) @RequestBody 처리:
          HTTP Body의 JSON 문자열을 Java 객체로 변환합니다.
          
          Jackson ObjectMapper가 수행:
          {"query": "호르무즈 근처에 제재국 선박 있어?"}
          
          1. JSON 문자열을 파싱하여 트리 구조로 변환
          2. AiQueryRequest record의 필드와 JSON 키를 매칭
             "query" → AiQueryRequest.query
          3. 값을 할당하여 객체 생성
             new AiQueryRequest("호르무즈 근처에 제재국 선박 있어?")
          
          이 과정을 "역직렬화(Deserialization)"라고 합니다.
          JSON(문자열) → Java 객체 방향.
          
       b) @AuthenticationPrincipal 처리:
          SecurityContext에서 인증 정보를 가져옵니다.
          Filter 2에서 저장한 UserPrincipal(5, "aitest@...", "USER")
          
       c) @Valid 처리 (있는 경우):
          Bean Validation 실행.
          @NotBlank가 query 필드에 있으면:
          - null이면 → MethodArgumentNotValidException → 400
          - ""이면   → MethodArgumentNotValidException → 400
          - " "이면  → MethodArgumentNotValidException → 400


[4] Controller 메서드 실행
    
    AiQueryController.query(request, user) 실행
    → aiQueryApplicationService.handleQuery(query, userId) 호출
    → ... (DDD 레이어를 따라 처리 — 다음 섹션에서 상세 설명)
    → 결과 Map<String, Object> 반환


[5] 응답 변환 및 반환
    
    Controller가 ResponseEntity를 반환하면:
    
    ① Java 객체 → JSON 변환 (직렬화, Serialization)
       Jackson ObjectMapper가 수행:
       ApiResponse {success=true, data={answer="현재...", cypher="MATCH...", entities=[]}}
       → {"success":true,"data":{"answer":"현재...","cypher":"MATCH...","entities":[]}}
    
    ② HTTP 응답 메시지 구성
       HTTP/1.1 200 OK
       Content-Type: application/json
       (보안 헤더들...)
       
       {"success":true,"data":{...}}
    
    ③ Filter Chain (역순)으로 통과
       필터들이 응답 헤더를 추가할 수 있음
       (보안 헤더, CORS 헤더 등)
    
    ④ Tomcat이 TCP 소켓을 통해 클라이언트에게 전송
```

---

## 4.4 DDD (Domain-Driven Design) — AnchorIQ의 설계 철학

### DDD란 무엇이고 왜 필요한가

소프트웨어가 복잡해지면 코드를 어떻게 구조화할지가 중요해집니다. DDD는 **"현실 세계의 비즈니스를 그대로 코드로 표현하자"**는 설계 철학입니다.

```
현실 세계:
  "선박은 회사가 소유하고, 회사는 국가에 등록되어 있고,
   국가는 제재를 받을 수 있다.
   선박의 리스크는 이 관계들을 종합하여 평가한다."

DDD 코드:
  Vessel.evaluateRiskScore(sanctionedCountries)
  → 선박이 자신의 리스크를 스스로 평가 (현실처럼!)
  
비-DDD 코드:
  RiskCalculationUtil.calculate(vessel, company, country, sanctions)
  → 외부 유틸리티가 계산 (현실과 동떨어짐)
```

DDD의 핵심 원칙: **"비즈니스 로직은 Entity 안에 넣는다."**

AnchorIQ에서 "선박의 리스크 점수는 선박 스스로 계산한다":

```java
// Vessel.java — 도메인 Entity (비즈니스 로직 보유!)
public class Vessel extends AggregateRoot {
    
    private Imo imo;            // IMO 번호 (Value Object)
    private Flag flag;          // 국적 (Value Object)
    private VesselType type;    // 선종 (Enum)
    private VesselStatus status;// 상태 (Enum)
    private int riskScore;      // 리스크 점수
    private Company company;    // 소유 회사
    
    /**
     * 리스크 점수 평가 — 핵심 비즈니스 로직
     * 
     * 이 메서드가 Entity 안에 있는 것이 DDD의 핵심입니다.
     * "선박아, 네 리스크 점수를 평가해봐" (Tell, Don't Ask)
     * 
     * 반대 패턴 (안 좋은 방식):
     *   int score = 0;
     *   if (vessel.getFlag().equals("IR")) score += 20;  // Getter로 꺼내서 외부에서 판단
     *   → 비즈니스 로직이 여기저기 흩어짐
     */
    public int evaluateRiskScore(
            Set<String> sanctionedCountryCodes,
            Set<String> highRiskFlags) {
        
        int score = 0;
        
        // 규칙 1: 제재국 기업 소유 → +40점
        // 이 판단은 Vessel이 자신의 company를 알고 있으므로 직접 할 수 있음
        if (isRegisteredInSanctionedCountry(sanctionedCountryCodes)) {
            score += 40;
        }
        
        // 규칙 2: 편의치적(고위험 깃발) → +20점
        // Flag Value Object가 자신의 값을 제공
        if (flag != null && highRiskFlags.contains(flag.value())) {
            score += 20;
        }
        
        // 규칙 3: 선령에 따른 리스크
        int age = calculateAge();
        if (age >= 20) score += 15;      // 20년 이상: +15
        else if (age >= 15) score += 10; // 15년 이상: +10
        
        // 규칙 4: 유조선은 화물 위험도 추가 → +10점
        if (isTanker()) score += 10;
        
        // 규칙 5: AIS 이상 상태 → +15점
        if (status == VesselStatus.NOT_UNDER_COMMAND) score += 15;
        
        int newScore = Math.min(score, 100); // 최대 100점
        
        // 점수가 변경되었으면 도메인 이벤트 발행
        if (this.riskScore != newScore) {
            this.riskScore = newScore;
            registerEvent(new RiskScoreChangedEvent(this.imo, newScore));
            // → Kafka Consumer가 이 이벤트를 감지하여 후속 처리
        }
        
        return this.riskScore;
    }
}
```

### DDD 4개 레이어

AnchorIQ의 모든 모듈은 4개 레이어로 나뉩니다. 각 레이어의 역할이 명확히 분리되어 있습니다.

```
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  Controller (api/ 패키지)                                    │
│  ─────────────────────                                       │
│  역할: "문 앞 안내원"                                         │
│                                                              │
│  하는 일:                                                    │
│  ① HTTP 요청을 받는다 (@PostMapping, @GetMapping)            │
│  ② 요청 데이터를 변환한다 (@RequestBody → Java 객체)          │
│  ③ Application Service에 위임한다                             │
│  ④ 결과를 HTTP 응답으로 변환한다 (Java 객체 → JSON)           │
│                                                              │
│  하면 안 되는 일:                                             │
│  ✗ 비즈니스 로직 (리스크 계산, 쿼터 확인 등)                   │
│  ✗ 직접 DB 접근                                              │
│  ✗ 외부 API 직접 호출                                        │
│                                                              │
│  코드 예시: AiQueryController.java                           │
│    @PostMapping("/query")                                    │
│    public ResponseEntity<ApiResponse> query(request, user) { │
│        Map result = service.handleQuery(                     │
│            request.query(), user.userId());  // 위임만!      │
│        return ResponseEntity.ok(ApiResponse.success(result));│
│    }                                                         │
│    // 5줄. 판단 없음. 받고 → 넘기고 → 반환.                   │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Application Service (application/ 패키지)                   │
│  ─────────────────────────────────                           │
│  역할: "지휘관" — 순서를 정하고 위임                           │
│                                                              │
│  하는 일:                                                    │
│  ① 트랜잭션 경계 관리                                        │
│  ② 여러 서비스의 실행 순서 조율 (오케스트레이션)               │
│  ③ 인프라 서비스 호출 (캐시, 로깅 등)                         │
│                                                              │
│  하면 안 되는 일:                                             │
│  ✗ 비즈니스 판단 ("리스크가 높은가?" 같은 판단)                │
│  ✗ 도메인 규칙 구현                                          │
│                                                              │
│  코드 예시: AiQueryApplicationServiceImpl.java               │
│    public Map handleQuery(String query, Long userId) {       │
│        checkApiQuota(userId);           // 1. 쿼터 확인      │
│        String cached = redis.get(key);  // 2. 캐시 확인      │
│        if (cached != null) return parse(cached);             │
│                                                              │
│        Map result = queryService.executeQuery(query); // 3. 실행│
│                                                              │
│        redis.set(key, result, 5, MINUTES);  // 4. 캐싱       │
│        subscriptionService.incrementApiUsage(userId); // 5. 사용량│
│        logger.logDecision(query, result);   // 6. 로깅       │
│                                                              │
│        return result;                                        │
│    }                                                         │
│    // 순서 조율만! "쿼터가 얼마인지", "캐시 전략이 뭔지"는    │
│    // 여기서 판단하지 않고 각 서비스에 위임.                   │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Domain (domain/ 패키지)                                     │
│  ────────────────────                                        │
│  역할: "전문가" — 비즈니스 규칙을 아는 곳                      │
│                                                              │
│  포함하는 것:                                                 │
│  - Entity: 고유 식별자가 있는 객체 (Vessel, Company, Port)    │
│  - Value Object: 값으로 동등성을 판단하는 불변 객체 (Imo, Flag)│
│  - Domain Service: 여러 Entity에 걸친 로직                    │
│  - Repository 인터페이스: 저장/조회 추상화                     │
│                                                              │
│  핵심 규칙:                                                   │
│  ✓ 비즈니스 로직은 여기에! (vessel.evaluateRiskScore())       │
│  ✓ 순수 Java만 사용 (Spring 어노테이션 없음!)                 │
│  ✓ 외부 의존성 없음 (DB, HTTP, Kafka 모름)                    │
│                                                              │
│  왜 순수 Java만?                                             │
│  → 단위 테스트를 DB 없이 할 수 있음                           │
│  → 프레임워크를 바꿔도 비즈니스 로직은 그대로                  │
│  → 가장 중요한 코드가 가장 안전한 곳에 있음                    │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Infrastructure (infrastructure/ 패키지)                     │
│  ──────────────────────────────────                          │
│  역할: "실무자" — 외부 시스템과 실제 통신                      │
│                                                              │
│  포함하는 것:                                                 │
│  - Repository 구현체: Neo4jVesselRepository                  │
│  - API 클라이언트: OpenClawClient                            │
│  - 보안: JwtAuthenticationFilter, SecurityConfig             │
│  - 설정: OpenClawConfig                                      │
│                                                              │
│  하는 일:                                                    │
│  ① DB에 실제로 저장/조회 (Neo4j, PostgreSQL, Redis)           │
│  ② 외부 API 실제 호출 (OpenClaw AI)                          │
│  ③ JWT 토큰 생성/검증                                        │
│  ④ Kafka 메시지 발행/소비                                    │
│                                                              │
│  Domain 레이어의 인터페이스를 구현:                            │
│  VesselRepository (인터페이스) ← Neo4jVesselRepository (구현) │
│  AiClient (인터페이스) ← OpenClawClient (구현)               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Entity vs Value Object vs DTO

```
┌─────────────────────────────────────────────────────────────┐
│ Entity (엔티티)                                               │
│                                                              │
│ "고유한 정체성(Identity)을 가진 객체"                          │
│                                                              │
│ 현실: 쌍둥이도 서로 다른 사람. 이름이 같아도 다른 사람.         │
│ 코드: IMO 번호가 같으면 같은 선박. 이름이 바뀌어도 같은 선박.   │
│                                                              │
│ 특징:                                                        │
│ - 고유 식별자가 있음 (Vessel의 IMO, User의 ID)                │
│ - 속성이 변할 수 있음 (선박 이름 변경, 상태 변경)              │
│ - 비즈니스 로직을 가짐 (evaluateRiskScore)                    │
│ - 생명주기가 있음 (생성 → 변경 → 삭제)                        │
│                                                              │
│ 예: Vessel, Company, User, Port, Subscription                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Value Object (값 객체)                                        │
│                                                              │
│ "값 자체가 의미인 객체. 값이 같으면 같은 것."                   │
│                                                              │
│ 현실: 만원짜리 두 장은 구분할 필요 없음. 둘 다 만 원.          │
│ 코드: Imo("9863297") 두 개는 완전히 같은 것.                  │
│                                                              │
│ 특징:                                                        │
│ - 고유 식별자 없음                                            │
│ - 불변(Immutable) — 한 번 만들면 값을 바꿀 수 없음             │
│ - 동등성(Equality) = 모든 속성이 같으면 같은 것                │
│ - 생성 시 검증 — 잘못된 값은 만들 수 없음                      │
│                                                              │
│ 왜 String 대신 Value Object를 쓰는가?                        │
│                                                              │
│   String imo = "not-a-valid-imo";                            │
│   // 컴파일 OK. 런타임에 문제 발생. 어디서 잘못됐는지 찾기 어려움│
│                                                              │
│   Imo imo = new Imo("not-a-valid-imo");                      │
│   // 즉시 IllegalArgumentException! 문제를 바로 발견.         │
│                                                              │
│ 코드:                                                        │
│   public record Imo(String value) {                          │
│       public Imo {                                           │
│           if (value == null || !value.matches("\\d{7}"))     │
│               throw new IllegalArgumentException(            │
│                   "IMO는 7자리 숫자여야 합니다: " + value);    │
│       }                                                      │
│   }                                                          │
│                                                              │
│ 예: Imo, Mmsi, Flag, Email, RiskScore, Coordinate            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ DTO (Data Transfer Object)                                    │
│                                                              │
│ "레이어 간 데이터를 운반하는 가방"                              │
│                                                              │
│ 현실: 택배 상자. 내용물을 담아서 이동시키는 것이 목적.          │
│ 코드: HTTP 요청/응답의 JSON을 담는 것이 목적.                  │
│                                                              │
│ 특징:                                                        │
│ - 비즈니스 로직 없음 (그냥 데이터 담는 그릇)                   │
│ - Controller ↔ 외부 세계 통신에만 사용                        │
│ - Domain Entity를 직접 노출하지 않기 위해 사용                 │
│                                                              │
│ 왜 Entity를 직접 반환하지 않는가?                              │
│   ① 보안: Entity에는 password 같은 민감 필드가 있을 수 있음    │
│   ② 결합도: Entity 구조가 바뀌면 API 응답도 바뀜              │
│   ③ 유연성: API 버전별로 다른 형태의 응답 제공 가능            │
│                                                              │
│ 코드:                                                        │
│   // 요청 DTO                                                │
│   public record AiQueryRequest(                              │
│       @NotBlank String query                                 │
│   ) {}                                                       │
│                                                              │
│   // 응답 DTO                                                │
│   public record AiQueryResponse(                             │
│       String answer,                                         │
│       List<Map<String, Object>> entities,                    │
│       String cypher,                                         │
│       int remainingQueries                                   │
│   ) {}                                                       │
│                                                              │
│ Entity에는 없는 remainingQueries가 DTO에는 있음.              │
│ → API 사용자에게 필요한 정보를 유연하게 구성 가능.             │
└─────────────────────────────────────────────────────────────┘
```

### Aggregate Root — 관련 객체의 대장

```
문제: Vessel이 PortCall(입항 기록)을 여러 개 가지고 있을 때,
      외부에서 PortCall을 직접 수정하면 Vessel의 일관성이 깨질 수 있음.

예: 선박이 항해 중(SAILING)인데, 누군가 직접 PortCall을 추가하면?
    → "항해 중인 선박이 항구에 정박" → 모순!

해결: Vessel을 Aggregate Root로 지정.
      PortCall에 대한 모든 접근은 반드시 Vessel을 통해.

  // 올바른 방법 (Aggregate Root를 통해)
  vessel.recordPortCall(port, arrivalTime);
  // Vessel이 자신의 상태(SAILING→MOORED)를 확인한 후 PortCall 추가
  // 상태가 맞지 않으면 예외 발생 → 일관성 보장

  // 잘못된 방법 (Aggregate Root 우회)
  portCallRepository.save(new PortCall(vessel, port, time));
  // Vessel의 상태를 확인하지 않고 직접 저장 → 일관성 깨짐!
```

### Repository 패턴 — 왜 인터페이스와 구현체를 분리하는가

```
[Domain 레이어 — core 모듈]
  
  public interface VesselRepository {
      Optional<Vessel> findByImo(Imo imo);
      void save(Vessel vessel);
  }
  
  이 인터페이스는 "선박을 저장하고 조회할 수 있다"는 것만 정의합니다.
  어떤 DB를 쓰는지는 모릅니다. Neo4j인지, PostgreSQL인지, 파일인지.
  
  → core 모듈은 어떤 외부 라이브러리에도 의존하지 않습니다!
  → 순수 Java만으로 작성되어 있습니다.


[Infrastructure 레이어 — api 모듈]
  
  @Repository
  public interface Neo4jVesselRepository extends Neo4jRepository<Neo4jVesselNode, Long> {
      Optional<Neo4jVesselNode> findByImo(String imo);
  }
  
  이것이 Neo4j를 사용하는 실제 구현체입니다.
  Spring Data Neo4j에 의존합니다.


왜 이렇게 나누는가?

  1. 테스트 용이성:
     테스트할 때 진짜 Neo4j 없이도 테스트 가능.
     Mock 객체를 주입하면 됨:
     
     VesselRepository mockRepo = mock(VesselRepository.class);
     when(mockRepo.findByImo(imo)).thenReturn(Optional.of(testVessel));
     
  2. DB 교체 가능:
     Neo4j를 다른 DB로 바꿔도 Domain 코드는 안 건드림.
     Infrastructure의 구현체만 교체.
     
  3. 의존성 방향:
     Domain(안쪽) ← Infrastructure(바깥쪽)
     
     안쪽이 바깥쪽을 모르는 구조.
     이것이 DIP(Dependency Inversion Principle, 의존성 역전 원칙).
     SOLID의 D입니다.
```

---

이 Part 3에서 다룬 것:
- Spring Boot가 해결하는 문제
- IoC/DI의 원리와 AnchorIQ에서의 실제 동작
- Bean 생명주기 (컴포넌트 스캔 → 의존성 해결 → 인스턴스 생성)
- Spring MVC 요청 처리 파이프라인 (Tomcat → Filter Chain → DispatcherServlet → Controller)
- DDD 4개 레이어의 역할과 코드 예시
- Entity, Value Object, DTO의 차이
- Aggregate Root와 Repository 패턴
