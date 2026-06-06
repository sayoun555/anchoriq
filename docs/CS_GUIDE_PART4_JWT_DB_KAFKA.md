# AnchorIQ CS 완전 가이드 — Part 4: JWT 인증, 데이터베이스, Kafka

---

# Chapter 5: JWT 인증 — 사용자를 어떻게 식별하는가

## 5.1 인증(Authentication)과 인가(Authorization)의 차이

이 두 개념은 자주 혼동되지만 완전히 다릅니다.

```
인증 (Authentication) — "너 누구야?"

  공항에서 여권을 보여주는 것.
  "이 사람이 본인이 맞는지" 확인하는 과정.
  
  AnchorIQ에서:
  - 로그인 시 이메일+비밀번호로 본인 확인
  - 이후 요청에서 JWT 토큰으로 본인 확인
  - 실패 시: 401 Unauthorized


인가 (Authorization) — "너 이거 해도 돼?"

  공항에서 퍼스트클래스 라운지에 들어가려는 것.
  "이 사람이 이 서비스를 이용할 권한이 있는지" 확인하는 과정.
  
  AnchorIQ에서:
  - USER 역할인데 /api/admin/** 접근 → 권한 없음
  - FREE 플랜인데 What-if 시뮬레이션 → 권한 없음
  - 실패 시: 403 Forbidden


순서: 인증 먼저 → 인가 다음

  "너 누구야?" (인증)
  → "아, aitest@anchoriq.com이구나" (인증 성공)
  → "근데 너 이 기능 쓸 수 있어?" (인가)
  → "USER 역할이고 FREE 플랜이니까... 안 됨" (인가 실패, 403)
```

---

## 5.2 JWT의 내부 구조

### JWT를 실제로 분해해보기

AnchorIQ에서 발급된 실제 JWT:

```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjUsImVtYWlsIjoiYWl0ZXN0QGFuY2hvcmlxLmNvbSIsInJvbGUiOiJVU0VSIiwidHlwZSI6ImFjY2VzcyIsImlhdCI6MTcxMjE5NzM3NSwiZXhwIjoxNzEyMjE1Mzc1fQ.abc123signature

이것은 3개의 부분을 점(.)으로 연결한 것입니다:

부분 1: eyJhbGciOiJIUzI1NiJ9
부분 2: eyJ1c2VySWQiOjUs...
부분 3: abc123signature
```

각 부분을 Base64URL 디코딩하면:

```
[부분 1: Header — 어떤 알고리즘으로 서명했는지]

  Base64URL 디코딩: {"alg":"HS256"}
  
  alg = Algorithm(알고리즘)
  HS256 = HMAC + SHA-256
  
  HMAC이란?
    Hash-based Message Authentication Code.
    "비밀키를 사용하여 메시지의 무결성을 검증하는 방법"
    
    비유: 편지에 빨간 도장을 찍는 것.
    도장(비밀키)을 가진 사람만 같은 도장을 찍을 수 있으므로,
    도장이 맞으면 "이 편지는 진짜"라고 확인할 수 있음.
    
  SHA-256이란?
    Secure Hash Algorithm, 256비트.
    어떤 길이의 입력이든 256비트(32바이트) 고정 길이 해시값으로 변환.
    
    특징:
    - 같은 입력 → 항상 같은 출력 (결정적)
    - 입력이 1비트만 달라도 출력이 완전히 달라짐 (눈사태 효과)
    - 출력에서 입력을 역추적할 수 없음 (일방향)
    
    예:
    SHA-256("hello") → 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
    SHA-256("hellp") → 완전히 다른 값 (1글자만 바뀌어도)


[부분 2: Payload — 사용자 정보 (Claims)]

  Base64URL 디코딩:
  {
    "userId": 5,
    "email": "aitest@anchoriq.com",
    "role": "USER",
    "type": "access",
    "iat": 1712197375,
    "exp": 1712215375
  }
  
  각 필드(Claim)의 의미:
  
  userId: 5
    데이터베이스에서 이 사용자의 Primary Key.
    서버가 "어떤 사용자의 요청인지" 식별하는 데 사용.
    
  email: "aitest@anchoriq.com"
    로깅, 표시용.
    
  role: "USER"
    권한 수준. USER 또는 ADMIN.
    SecurityConfig에서:
      .requestMatchers("/api/admin/**").hasRole("ADMIN")
    → ADMIN만 접근 가능한 경로를 구분.
    
  type: "access"
    토큰 종류. access(API 호출용) 또는 refresh(갱신용).
    JwtAuthenticationFilter에서:
      if (jwtTokenProvider.isRefreshToken(token)) → 인증 안 함
    → refresh 토큰으로 API를 호출하는 것을 방지.
    
  iat (Issued At): 1712197375
    토큰 발급 시각. Unix Timestamp (1970-01-01부터의 초).
    2026-04-04 12:42:55 (UTC)
    
  exp (Expiration): 1712215375
    토큰 만료 시각.
    iat + 18000초 = iat + 5시간
    → 이 시간 이후에는 토큰이 무효화됨.
    → 서버가 검증할 때 현재 시각 > exp이면 → 인증 거부.


[부분 3: Signature — 위조 방지 서명]

  서명 생성 과정:
  
  ① header와 payload를 Base64URL로 인코딩
     base64url(header) = "eyJhbGciOiJIUzI1NiJ9"
     base64url(payload) = "eyJ1c2VySWQiOjUs..."
     
  ② 점(.)으로 연결
     "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjUs..."
     
  ③ 비밀키와 함께 HMAC-SHA256 해시
     HMAC-SHA256(
       "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjUs...",
       "anchoriq-jwt-secret-key-2026-must-be-at-least-256-bits-long!!"
     )
     → 고정 길이 해시값 (서명)
     
  ④ 서명을 Base64URL로 인코딩하여 세 번째 부분으로 추가
  
  
  서명 검증 과정 (서버가 토큰을 받았을 때):
  
  ① 토큰을 점(.)으로 분리
  ② header + payload 부분을 같은 비밀키로 HMAC-SHA256 재계산
  ③ 재계산한 서명과 토큰의 서명을 비교
     - 같으면 → 위조되지 않음 ✅
     - 다르면 → 위조됨 ❌ (누군가 payload를 변경했음)
  
  
  왜 위조가 불가능한가?
  
  해커가 payload의 role을 "USER"에서 "ADMIN"으로 바꾸면:
  ① payload가 변경됨
  ② 하지만 해커는 비밀키를 모르므로 새 서명을 만들 수 없음
  ③ 서버가 검증할 때: 재계산한 서명 ≠ 토큰의 서명 → 거부!
  
  비밀키를 아는 사람만 유효한 서명을 만들 수 있습니다.
  비밀키는 서버에만 있으므로, 서버만 토큰을 발급할 수 있습니다.
```

### 중요: JWT는 암호화가 아닙니다!

```
흔한 오해: "JWT는 안전하니까 비밀번호를 넣어도 되겠지?"
→ 절대 안 됩니다!

Base64는 인코딩(형식 변환)이지 암호화가 아닙니다.
누구나 Base64를 디코딩하여 payload를 읽을 수 있습니다.

jwt.io 같은 사이트에서 토큰을 붙여넣으면 즉시 내용이 보입니다.

JWT가 보장하는 것:
  ✅ 무결성: "이 토큰의 내용이 변조되지 않았다" (서명으로 검증)
  ✅ 인증: "이 토큰을 발급한 건 우리 서버다" (비밀키로 서명)
  ❌ 기밀성: "이 토큰의 내용을 아무도 못 본다" (보장하지 않음!)

→ userId, email, role 같은 식별 정보만 넣음.
→ 비밀번호, 카드번호 등은 절대 넣지 않음.
```

---

## 5.3 비밀번호 해싱 — BCrypt

### 왜 비밀번호를 그대로 저장하면 안 되는가

```
만약 비밀번호를 DB에 평문(Plain Text)으로 저장한다면:

  users 테이블:
  | email              | password    |
  | admin@anchoriq.com | admin123!   |  ← 그대로 저장!
  
  문제:
  1. DB가 해킹당하면 모든 비밀번호가 노출
  2. 같은 비밀번호를 다른 사이트에도 쓰는 사람 → 연쇄 피해
  3. 내부 직원(DBA)도 비밀번호를 볼 수 있음
```

### 해시(Hash)로 해결

```
해시란: 원본 데이터를 고정 길이의 "지문"으로 변환하는 것.
        원본 → 지문은 가능하지만, 지문 → 원본은 불가능(일방향).

  "Test1234!" → BCrypt → "$2a$10$WKd.J2kf..."
  
  users 테이블:
  | email              | password                                               |
  | aitest@anchoriq.com| $2a$10$WKd.J2kfGWgyjl8VnSieBOWZTbtHRJBSJIoUpqXOoAHjwu|
  
  DB가 해킹당해도 원래 비밀번호를 알 수 없음!
```

### BCrypt의 내부 구조

```
$2a$10$WKd.J2kfGWgyjl8VnSieBOWZTbtHRJBSJIoUpqXOoAHjwu9jXzAhG

$2a$     → BCrypt 알고리즘 식별자
10$      → Cost Factor (작업 인수)
           2^10 = 1024회 반복 해시
           → 의도적으로 느리게 만듦!
WKd.J2kfGWgyjl8VnSieBO → Salt (22문자, 랜덤 생성)
                          같은 비밀번호라도 Salt가 다르면 해시 결과가 다름
                          → Rainbow Table 공격 방어
WZTbtHRJBSJIoUpqXOoAHjwu9jXzAhG → 해시 결과
```

### BCrypt가 SHA-256보다 나은 이유

```
SHA-256:
  속도: ~1억 회/초 (GPU)
  → 해커가 1초에 1억 개의 비밀번호를 시도할 수 있음
  → "abc123" 같은 간단한 비밀번호는 몇 초 만에 뚫림

BCrypt (Cost 10):
  속도: ~10회/초
  → 해커가 1초에 10개만 시도 가능
  → "abc123"도 뚫는 데 수일~수주 소요

BCrypt가 의도적으로 느린 이유:
  정상 사용자는 로그인을 1번 하므로 100ms 지연은 문제없음.
  하지만 해커는 수백만 번 시도해야 하므로 100ms × 수백만 = 수년.
```

---

# Chapter 6: 데이터베이스 — 데이터를 어디에 어떻게 저장하는가

## 6.1 관계형 데이터베이스 (PostgreSQL)

### 테이블, 행, 열

```
users 테이블:

  +----+--------------------+------------------+------+
  | id | email              | password(해시)    | role |     ← 열(Column) = 속성
  +----+--------------------+------------------+------+
  | 1  | admin@anchoriq.com | $2a$10$placeho.. | ADMIN|     ← 행(Row) = 데이터 한 건
  | 2  | test@anchoriq.com  | $2a$10$WKd.J2k..| USER |
  | 5  | aitest@anchoriq.com| $2a$10$WKd.J2k..| USER |
  +----+--------------------+------------------+------+

  id: Primary Key (PK) — 각 행을 고유하게 식별하는 값
      자동 증가 (1, 2, 3, 4, 5...)
      
  같은 email은 두 번 존재할 수 없음 (UNIQUE 제약조건)
```

### ACID 트랜잭션 — 왜 돈을 다루는 데이터에 PostgreSQL을 쓰는가

```
시나리오: 사용자가 PRO 구독을 결제합니다.

  ① payments 테이블에 결제 기록 INSERT
  ② subscriptions 테이블의 plan을 FREE → PRO로 UPDATE
  ③ users 테이블의 api_quota를 5 → 무제한으로 UPDATE

만약 ②에서 에러가 나면?

  ACID 없이:
    ① 결제 기록 저장됨 ✅ (돈은 빠져나감)
    ② 구독 변경 실패 ❌ (아직 FREE)
    ③ 실행 안 됨 ❌
    → 돈은 냈는데 PRO가 아닌 상태! 고객 불만!

  ACID 있이 (@Transactional):
    ② 에러 발생 → 전체 롤백
    ① 도 취소됨 (결제 기록 삭제)
    → 마치 아무 일도 없었던 것처럼 원래 상태로 복구


각 ACID 속성:

  A (Atomicity, 원자성):
    "전부 성공하거나 전부 실패하거나"
    ①②③이 하나의 단위. 중간 상태 없음.

  C (Consistency, 일관성):
    "규칙을 항상 만족"
    잔액은 음수가 될 수 없음, 이메일은 고유해야 함 등
    트랜잭션 전후로 DB 규칙이 항상 유지됨.

  I (Isolation, 격리성):
    "동시 트랜잭션이 서로 간섭하지 않음"
    A 사용자가 결제하는 동안 B 사용자가 같은 구독을 조회하면?
    → B는 A의 중간 상태를 볼 수 없음 (커밋 전 데이터 안 보임)

  D (Durability, 지속성):
    "커밋되면 영구 보존"
    커밋 직후 서버가 꺼져도 데이터는 디스크에 안전하게 저장됨.
    WAL(Write-Ahead Log)로 보장.
```

### 인덱스 — B-Tree

```
문제: 100만 명의 사용자 중 email이 "aitest@anchoriq.com"인 사람을 찾기

인덱스 없이:
  행 1: admin@... → 아닌데
  행 2: test@... → 아닌데
  행 3: test2@... → 아닌데
  ...
  행 999,999: other@... → 아닌데
  행 1,000,000: aitest@... → 찾았다!
  → 최악의 경우 100만 번 비교 → O(n)

인덱스 있이 (B-Tree):
  
  B-Tree는 정렬된 트리 구조입니다.
  도서관의 카드 목록과 같습니다.
  
  찾고자 하는 값: "aitest@anchoriq.com"
  
         레벨 0:         [M]
                        / \
         레벨 1:     [E]   [S]
                    / \    / \
         레벨 2: [A-D][F-L][N-R][T-Z]
                 /
         레벨 3: [aa..] [ab..] [ai..] → 찾았다!
  
  비교 횟수: 4번 (트리의 높이)
  → O(log n) = log₂(1,000,000) ≈ 20번
  
  100만 번 vs 20번 = 50,000배 빠름!
```

---

## 6.2 그래프 데이터베이스 (Neo4j)

### 왜 관계형 DB만으로는 부족한가

```
문제: "이 선박의 소유 회사가 제재국에 등록되어 있는가?"

관계형 DB (SQL):
  SELECT v.name
  FROM vessels v
  JOIN companies c ON v.company_id = c.id       ← JOIN 1
  JOIN countries co ON c.country_id = co.id     ← JOIN 2
  JOIN sanctions s ON co.id = s.country_id      ← JOIN 3
  WHERE v.imo = '9863297' AND s.active = true;
  
  3개의 JOIN이 필요합니다.
  JOIN이 늘어날수록 성능이 급격히 저하됩니다.
  
  4-hop 질의 (선박→회사→국가→제재→UN결의안):
  → 4개의 JOIN → 수백만 행이면 수 초 소요
  
  "모든 제재국 기업 소유 선박 중 호르무즈를 지나는 것": 
  → 6개의 JOIN → 매우 느림


그래프 DB (Neo4j Cypher):
  MATCH (v:Vessel {imo: '9863297'})
        -[:OWNED_BY]->(c:Company)
        -[:REGISTERED_IN]->(co:Country)
        -[:SANCTIONED_BY]->(s:Sanction)
  WHERE s.active = true
  RETURN v.name
  
  JOIN이 없습니다. 관계를 "따라가기만" 합니다.
  
  Neo4j의 Index-Free Adjacency:
  각 노드가 인접 노드의 물리적 포인터를 직접 가지고 있습니다.
  포인터를 따라가는 것은 O(1)입니다 (상수 시간).
  4-hop = O(4) = 상수 시간. 데이터가 아무리 많아도 같은 속도.
```

---

## 6.3 Redis — 인메모리 캐시

### 왜 캐시가 필요한가

```
AI 질의 처리 시간:
  OpenClaw AI 1차 호출: ~1초
  Neo4j 쿼리 실행: ~0.1초
  OpenClaw AI 2차 호출: ~1초
  총: ~2.2초

같은 질문을 5분 이내에 다시 하면?
  → 또 2.2초 기다려야 함
  → AI에 불필요한 부하
  → 사용자 경험 나쁨

Redis 캐시 적용 후:
  첫 번째 질의: 2.2초 (정상 처리 + 결과를 Redis에 저장)
  5분 이내 같은 질의: ~1ms (Redis에서 바로 반환!)
  
  2200ms → 1ms = 2,200배 빠름!
```

### Redis가 빠른 이유

```
PostgreSQL/Neo4j: 디스크(SSD)에 데이터 저장
  디스크 읽기: ~0.1ms (SSD 기준)

Redis: 메모리(RAM)에 데이터 저장
  메모리 읽기: ~0.0001ms
  
  100배~1000배 빠릅니다.
  
  대신 단점:
  - 메모리는 비쌈 (디스크 대비)
  - 서버 꺼지면 데이터 사라짐 (영속성 약함)
  → 그래서 "캐시"로 사용. 사라져도 다시 계산하면 됨.
```

### AnchorIQ에서 Redis 사용

```java
// AiQueryApplicationServiceImpl.java

// 캐시 키: 질의 문자열의 해시코드
String cacheKey = "ai:result:" + query.hashCode();

// 캐시 확인
String cached = redisTemplate.opsForValue().get(cacheKey);
if (cached != null) {
    // 캐시 히트! AI 호출 없이 바로 반환 (~1ms)
    return parseCachedResult(cached);
}

// 캐시 미스 → AI 호출 (~2.2초)
Map<String, Object> result = queryService.executeQuery(query);

// 결과를 Redis에 저장 (TTL 5분)
redisTemplate.opsForValue().set(cacheKey, serialize(result), 5, TimeUnit.MINUTES);
// 5분 후 자동 삭제 → 오래된 데이터가 캐시에 남지 않음
```

---

# Chapter 7: Kafka — 이벤트 기반 아키텍처

## 7.1 Kafka가 해결하는 문제

```
문제: AIS에서 선박 위치 데이터가 들어오면, 
      Redis(실시간 위치), Neo4j(온톨로지), AI(이상 탐지)
      세 곳에 동시에 전달해야 합니다.

직접 호출 방식 (Kafka 없이):
  
  AisCollector {
      void onPositionReceived(AisData data) {
          redisService.updatePosition(data);    // 1. Redis 저장
          neo4jService.updateVessel(data);      // 2. Neo4j 업데이트
          aiService.detectAnomaly(data);        // 3. AI 분석
      }
  }
  
  문제점:
  ① Neo4j가 느리면? → Redis와 AI도 기다려야 함 (병목)
  ② AI 서비스가 죽으면? → 전체 파이프라인 멈춤 (장애 전파)
  ③ 새로운 소비자 추가 시? → AisCollector 코드 수정 필요 (결합)
  ④ 데이터 유실 → Neo4j 저장 실패하면 그 데이터 영원히 사라짐


Kafka 방식:
  
  AisCollector → [Kafka Topic: ais-positions] → Consumer A (Redis)
                                                Consumer B (Neo4j)  
                                                Consumer C (AI)
  
  장점:
  ① Neo4j가 느려도 Redis와 AI는 독립적으로 처리 (디커플링)
  ② AI 서비스가 죽어도 Kafka에 메시지가 남아있어 나중에 재처리 (내구성)
  ③ 새 소비자 추가 → Consumer D만 추가하면 됨 (확장성)
  ④ Kafka가 메시지를 디스크에 보관 → 유실 없음 (영속성)
```

### Kafka의 핵심 개념

```
[Producer] → [Topic] → [Consumer]

Producer (생산자):
  메시지를 Kafka에 보내는 쪽.
  AnchorIQ: AIS 수집기, 뉴스 수집기, 날씨 수집기 등

Topic (토픽):
  메시지가 저장되는 카테고리(채널).
  AnchorIQ: ais-positions, risk-alerts, news-events 등 8개

Consumer (소비자):
  메시지를 Kafka에서 가져가는 쪽.
  AnchorIQ: Redis 저장기, Neo4j 업데이터, AI 분석기 등

비유: 우체국 시스템
  Producer = 편지를 보내는 사람
  Topic = 우편함 (카테고리별로 분류)
  Consumer = 편지를 가져가는 사람
  
  보내는 사람과 받는 사람이 직접 만날 필요 없음.
  보내는 사람은 우편함에 넣기만 하면 됨.
  받는 사람은 자기 속도로 가져가면 됨.
```

### Partition과 Offset

```
Topic: ais-positions (3개 파티션)

  Partition 0: [위치1] [위치4] [위치7]  ...
                off=0   off=1   off=2

  Partition 1: [위치2] [위치5] [위치8]  ...
                off=0   off=1   off=2

  Partition 2: [위치3] [위치6] [위치9]  ...
                off=0   off=1   off=2

파티션(Partition):
  토픽을 물리적으로 나눈 것.
  병렬 처리를 위해 존재합니다.
  
  파티션이 3개이면?
  → Consumer 3개가 각각 1개 파티션을 담당
  → 3배 빠른 처리!

오프셋(Offset):
  파티션 내에서 메시지의 순서 번호.
  "어디까지 읽었는지" 추적.
  
  Consumer가 off=2까지 읽었다면,
  다음에는 off=3부터 읽으면 됨.
  
  Consumer가 중간에 죽었다가 살아나도?
  → 마지막으로 커밋한 offset부터 재개
  → 메시지 유실 없음!

Key 기반 파티셔닝:
  같은 Key의 메시지는 항상 같은 파티션으로 갑니다.
  
  AnchorIQ: Key = MMSI (선박 식별 번호)
  → 같은 선박의 위치 업데이트는 항상 같은 파티션
  → 한 선박의 위치가 순서대로 처리됨을 보장
```

### Consumer Group

```
같은 토픽을 여러 목적으로 소비해야 할 때:

Consumer Group "redis-writer":
  Consumer A → Partition 0     
  Consumer B → Partition 1, 2  
  → Redis에 선박 위치 저장

Consumer Group "neo4j-updater":
  Consumer C → Partition 0
  Consumer D → Partition 1
  Consumer E → Partition 2
  → Neo4j 온톨로지 업데이트

Consumer Group "ai-analyzer":
  Consumer F → Partition 0, 1, 2
  → AI 이상 탐지

같은 그룹 내: 하나의 파티션은 하나의 Consumer만 읽음 (부하 분산)
다른 그룹 간: 같은 메시지를 독립적으로 읽음 (다중 소비)

→ AIS 데이터 하나가 Redis, Neo4j, AI에 모두 전달됨
→ 각각 독립적으로 자기 속도에 맞춰 처리
```

### Dead Letter Topic (DLT) — 실패 메시지 처리

```
Consumer가 메시지 처리에 실패하면?

AnchorIQ의 정책:
  1차 시도 실패 → 1초 대기 → 재시도
  2차 시도 실패 → 1초 대기 → 재시도
  3차 시도 실패 → DLT(Dead Letter Topic)로 이동

  [ais-positions] → Consumer 처리 실패 (3번) → [ais-positions.DLT]
  
  DLT에 쌓인 메시지:
  → 운영자가 원인 분석 (DB 다운? 데이터 형식 오류?)
  → 원인 해결 후 수동 재처리
  
  핵심: 하나의 실패가 전체 파이프라인을 멈추지 않음!
```

---

# Chapter 8: Docker — 어디서든 동일하게 실행

## 8.1 Docker가 해결하는 문제

```
"제 컴퓨터에서는 되는데요..."

원인:
  개발자 A의 맥북: Java 21, Neo4j 5.15, Redis 7.2
  개발자 B의 윈도우: Java 17, Neo4j 5.10, Redis 7.0
  운영 서버: Java 21, Neo4j 5.12, Redis 7.1
  
  → 버전 차이, 설정 차이, OS 차이로 동작이 달라짐

Docker의 해결:
  "프로그램 + 필요한 모든 것(라이브러리, 설정, OS)"을
  하나의 이미지(Image)로 패키징.
  
  어디서 실행하든 100% 동일한 환경.
```

### 컨테이너 vs 가상머신

```
가상머신 (VM):
  ┌────────────────────┐
  │    Application     │
  │    Libraries       │
  │    Guest OS        │ ← 전체 OS 포함 (2~10 GB)
  │    (Ubuntu 22.04)  │
  └────────────────────┘
  │    Hypervisor      │ ← 하드웨어 가상화
  │    Host OS         │
  │    Hardware        │
  
  시작: 분 단위
  크기: GB 단위
  격리: 완전 (별도 OS)

Docker 컨테이너:
  ┌────────────────────┐
  │    Application     │
  │    Libraries       │ ← OS 커널은 Host와 공유 (50~500 MB)
  └────────────────────┘
  │    Docker Engine   │ ← 프로세스 격리
  │    Host OS         │
  │    Hardware        │
  
  시작: 초 단위
  크기: MB 단위
  격리: 프로세스 수준 (같은 OS 커널 공유)

핵심 차이:
  VM: 하드웨어를 가상화 → 전체 OS 복제 → 무거움
  Docker: OS 커널 공유 → 프로세스만 격리 → 가벼움
```

### AnchorIQ Docker Compose

```yaml
# docker-compose.yml (11개 서비스)

services:
  postgresql:
    image: postgres:16          # PostgreSQL 16 이미지
    ports: ["5433:5432"]        # 호스트:5433 → 컨테이너:5432
    environment:
      POSTGRES_DB: anchoriq
      POSTGRES_USER: anchoriq
      POSTGRES_PASSWORD: anchoriq_dev_2026
    volumes:
      - pgdata:/var/lib/postgresql/data  # 데이터 영속화
    # 컨테이너를 삭제해도 데이터는 볼륨에 남아있음

  neo4j:
    image: neo4j:5-community
    ports:
      - "7474:7474"             # Neo4j 브라우저 (웹 UI)
      - "7687:7687"             # Bolt 프로토콜 (Cypher 쿼리)
    environment:
      NEO4J_AUTH: neo4j/neo4j_dev_2026

  redis:
    image: redis:7-alpine       # alpine = 경량 Linux 기반
    ports: ["6380:6379"]
    # 왜 6380? 호스트에 다른 Redis가 6379에서 이미 돌고 있을 수 있으므로

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    ports: ["29092:29092"]
    environment:
      KAFKA_PROCESS_ROLES: broker,controller  # KRaft 모드 (Zookeeper 없음)
      KAFKA_NUM_PARTITIONS: 3                 # 기본 3 파티션

# docker compose --profile core up -d
# → PostgreSQL + Redis만 시작

# docker compose --profile full up -d
# → 11개 전부 시작
```

---

# 부록: 전체 흐름 27단계 (종합)

```
사용자가 "호르무즈 근처에 제재국 선박 있어?" 입력 후 Enter:

[네트워크]
 ① DNS: localhost → 127.0.0.1 (/etc/hosts에서 즉시, 0ms)
 ② TCP: 이미 Keep-Alive 연결이 있으면 재사용 (0ms)
        없으면 3-Way Handshake (SYN→SYN-ACK→ACK, ~0.1ms)
 ③ HTTP: POST 요청 메시지 구성 및 전송

[Tomcat]
 ④ TCP 소켓에서 바이트 스트림 읽기
 ⑤ HTTP 메시지 파싱 → HttpServletRequest 객체 생성

[Filter Chain]
 ⑥ CorsFilter: Origin 확인, CORS 헤더 추가
 ⑦ JwtAuthenticationFilter: Cookie에서 JWT 추출
 ⑧ JWT 서명 검증 (HMAC-SHA256 재계산 → 비교)
 ⑨ JWT에서 userId=5, role=USER 추출 → SecurityContext 설정
 ⑩ AuthorizationFilter: authenticated() 확인 → 통과

[DispatcherServlet]
 ⑪ HandlerMapping: POST /api/ai/query → AiQueryController.query()
 ⑫ Jackson: JSON → AiQueryRequest 역직렬화 (Deserialization)
 ⑬ @AuthenticationPrincipal: SecurityContext에서 UserPrincipal 추출

[Controller → Application Service]
 ⑭ 쿼터 확인: PostgreSQL에서 구독 정보 조회 → API 사용 가능 확인
 ⑮ Redis 캐시 확인: GET ai:result:{hash} → MISS

[AI Service — 1차 호출]
 ⑯ OpenClawClient: HTTP POST to 127.0.0.1:18789/v1/chat/completions
     System Prompt: Neo4j 스키마 + 규칙, Temperature: 0.1
 ⑰ OpenClaw → OpenAI 엔진: 자연어 → Cypher 쿼리 변환
 ⑱ CypherQueryValidator: 읽기 전용 검증 (CREATE/DELETE 차단)

[Neo4j]
 ⑲ Bolt 프로토콜(7687)로 Cypher 실행
 ⑳ Vessel→OWNED_BY→Company→REGISTERED_IN→Country→SANCTIONED_BY→Sanction
     + PASSES_THROUGH→Chokepoint(Hormuz) 관계 탐색
     결과: 0건

[AI Service — 2차 호출]
 ㉑ OpenClawClient: 결과를 자연어로 변환, Temperature: 0.7
 ㉒ "현재 조회 결과상 호르무즈 인근에 제재 대상국 관련 선박은..."

[후처리]
 ㉓ Redis: SET ai:result:{hash} → TTL 5분
 ㉔ PostgreSQL: API 사용량 +1 (@Transactional, ACID 보장)
 ㉕ Elasticsearch: 로그 기록 (비동기, Tier 3 — 실패 무시)

[응답]
 ㉖ Jackson: Map → JSON 직렬화 (Serialization)
 ㉗ Tomcat: HTTP 200 OK 응답 → TCP 전송 → 브라우저 수신 → 화면 렌더링

소요 시간: ~2~5초 (대부분 AI 2회 호출에서)
```

---

이 가이드에서 다룬 모든 주제:

| Part | 주제 | 핵심 개념 |
|------|------|----------|
| 1 | 네트워크 | IP, 포트, DNS 6단계, TCP 3-Way Handshake, Nginx |
| 2 | HTTP/REST | 요청/응답 구조, GET/POST, 상태코드, Cookie, CORS, REST 원칙 |
| 3 | Spring/DDD | IoC/DI, Bean 생명주기, MVC 파이프라인, DDD 4레이어, Entity/VO/DTO |
| 4 | 인증/DB/Kafka | JWT 구조/서명, BCrypt, ACID, B-Tree, Neo4j, Redis 캐시, Kafka |
