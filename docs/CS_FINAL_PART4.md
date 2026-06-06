# Part 4: 데이터베이스, Kafka, Docker, 전체 흐름

---

# Chapter 7: 데이터베이스

## 7.1 데이터베이스(Database)란?

**데이터베이스란, 데이터를 체계적으로 저장하고, 빠르게 검색할 수 있도록 만든 시스템입니다.**

엑셀 파일에 데이터를 저장하는 것과 비슷하지만, 데이터베이스는 수백만~수억 건의 데이터를 효율적으로 처리하고, 여러 사람이 동시에 접근해도 문제가 없도록 설계되어 있습니다.

AnchorIQ는 4개의 서로 다른 데이터베이스를 사용합니다. 각각의 강점이 다르기 때문입니다.

## 7.2 관계형 데이터베이스(RDBMS)란? — PostgreSQL

**관계형 데이터베이스란, 데이터를 테이블(표) 형태로 저장하고, 테이블 간의 관계를 정의하는 데이터베이스입니다.**

엑셀 시트와 비슷합니다. 행(Row)은 하나의 데이터, 열(Column)은 데이터의 속성입니다.

```
users 테이블:
  +----+--------------------+------------------+------+
  | id | email              | password(해시)    | role |
  +----+--------------------+------------------+------+
  | 1  | admin@anchoriq.com | $2a$10$plac...   | ADMIN|
  | 5  | aitest@anchoriq.com| $2a$10$WKd...    | USER |
  +----+--------------------+------------------+------+
  
  id = Primary Key (PK): 각 행을 고유하게 식별하는 값
```

AnchorIQ에서 PostgreSQL은 **돈과 관련된 데이터**(유저, 결제, 구독)를 저장합니다. 이 데이터는 절대 틀려서는 안 되므로 ACID 트랜잭션이 필요합니다.

### SQL(Structured Query Language)이란?

**SQL이란, 관계형 데이터베이스에서 데이터를 조회, 삽입, 수정, 삭제하기 위한 언어입니다.**

```sql
-- 조회
SELECT email, role FROM users WHERE id = 5;

-- 삽입
INSERT INTO users (email, password, role) VALUES ('new@test.com', '$2a$...', 'USER');

-- 수정
UPDATE subscriptions SET plan = 'PRO' WHERE user_id = 5;

-- 삭제
DELETE FROM bookmarks WHERE id = 7;
```

### 트랜잭션(Transaction)이란?

**트랜잭션이란, 여러 개의 DB 작업을 하나의 단위로 묶어서 "전부 성공하거나 전부 실패하거나"를 보장하는 것입니다.**

```
시나리오: 사용자가 PRO 구독을 결제합니다.

  작업 1: payments 테이블에 결제 기록 저장
  작업 2: subscriptions 테이블에서 plan을 PRO로 변경
  작업 3: users 테이블에서 api_quota를 변경

만약 작업 2에서 에러가 나면?

  트랜잭션 없이:
    작업 1 성공 (돈은 빠져나감)
    작업 2 실패 (아직 FREE)
    → 돈은 냈는데 PRO가 아닌 상태! 고객 불만!

  트랜잭션 있이:
    작업 2 에러 → 전체 롤백(되감기)
    작업 1도 취소됨 (결제 기록 삭제)
    → 마치 아무 일도 없었던 것처럼 원래 상태로
```

### ACID란?

**ACID란, 트랜잭션이 보장해야 하는 4가지 속성입니다.**

```
A — Atomicity (원자성): "전부 성공하거나 전부 실패하거나"
C — Consistency (일관성): "DB 규칙을 항상 만족" (잔액 음수 불가 등)
I — Isolation (격리성): "동시 트랜잭션이 서로 간섭하지 않음"
D — Durability (지속성): "커밋되면 서버가 꺼져도 데이터 보존"
```

### 인덱스(Index)란?

**인덱스란, 데이터를 빠르게 검색하기 위한 별도의 자료구조입니다. 책의 색인(찾아보기)과 같습니다.**

```
인덱스 없이 (Full Table Scan):
  100만 건의 데이터를 처음부터 끝까지 순서대로 찾음
  → 최대 100만 번 비교 → O(n) → 느림

인덱스 있이 (B-Tree 검색):
  정렬된 트리 구조에서 이진 탐색
  → 약 20번 비교로 찾음 → O(log n) → 빠름
  
  100만 번 vs 20번 = 5만 배 차이!
```

**B-Tree란?** 데이터베이스 인덱스에서 가장 많이 사용하는 자료구조로, 데이터를 정렬된 트리 형태로 저장합니다. 트리의 각 노드가 여러 개의 키를 가지므로, 적은 디스크 접근으로 원하는 데이터를 찾을 수 있습니다.

---

## 7.3 그래프 데이터베이스란? — Neo4j

**그래프 데이터베이스란, 데이터를 노드(점)와 관계(선)로 저장하는 데이터베이스입니다. 데이터 간의 "연결"을 빠르게 탐색하는 데 특화되어 있습니다.**

### 왜 관계형 DB만으로는 부족한가

```
질문: "이 선박의 소유 회사가 제재국에 등록되어 있는가?"

관계형 DB (SQL):
  SELECT v.name FROM vessels v
  JOIN companies c ON v.company_id = c.id
  JOIN countries co ON c.country_id = co.id
  JOIN sanctions s ON co.id = s.country_id
  WHERE v.imo = '9863297';
  
  → 3개의 JOIN 필요. JOIN이 많을수록 급격히 느려짐.

그래프 DB (Neo4j Cypher):
  MATCH (v:Vessel {imo:'9863297'})-[:OWNED_BY]->(c:Company)
        -[:REGISTERED_IN]->(co:Country)-[:SANCTIONED_BY]->(s:Sanction)
  RETURN v.name
  
  → JOIN 없이 관계를 따라가기만 하면 됨. 직관적이고 빠름.
```

### 노드(Node)와 관계(Relationship)란?

```
노드 = 점 = 데이터의 실체
  예: (:Vessel {name:"알헤시라스", imo:"9863297"})
      (:Company {name:"HMM"})
      (:Country {name:"이란"})

관계 = 선 = 노드 간의 연결 (방향이 있음)
  예: (알헤시라스)-[:OWNED_BY]->(HMM)
      (HMM)-[:REGISTERED_IN]->(한국)
      (이란)-[:SANCTIONED_BY]->(UN제재)
```

### Cypher란?

**Cypher란, Neo4j에서 데이터를 조회하기 위한 쿼리 언어입니다. SQL의 그래프 버전이라고 생각하면 됩니다.**

ASCII 아트로 그래프 패턴을 표현합니다:

```
(노드)-[:관계]->(다른노드)

MATCH (v:Vessel)-[:OWNED_BY]->(c:Company)
WHERE v.name = "알헤시라스"
RETURN c.name
→ "알헤시라스 선박을 소유한 회사의 이름을 알려줘"
```

### Index-Free Adjacency란?

**Neo4j가 관계 탐색에서 관계형 DB보다 빠른 핵심 이유입니다.**

관계형 DB에서 JOIN은 인덱스를 조회하는 작업입니다 → O(log n). 그래프 DB에서 관계 탐색은 노드가 가진 **물리적 포인터**를 따라가는 것입니다 → O(1).

```
관계형 DB: "A 회사의 선박은?" → companies 인덱스 검색 → O(log n)
Neo4j:     "A 회사의 선박은?" → A 회사 노드의 포인터를 따라감 → O(1)

데이터가 아무리 많아도 관계 탐색 속도가 일정합니다.
```

---

## 7.4 인메모리 캐시란? — Redis

**Redis란, 모든 데이터를 메모리(RAM)에 저장하는 초고속 Key-Value 저장소입니다.**

### 캐시(Cache)란?

**캐시란, 자주 사용하는 데이터를 빠르게 접근할 수 있는 곳에 임시로 저장하는 것입니다.**

```
캐시 없이:
  AI 질의 → OpenClaw 호출(1초) + Neo4j 조회(0.1초) + OpenClaw 호출(1초) = 2.1초
  같은 질문을 또 하면? → 또 2.1초

캐시 있이:
  첫 번째 질의: 2.1초 (결과를 Redis에 저장)
  같은 질문을 또 하면: Redis에서 바로 반환 → 0.001초 (1밀리초)
```

### TTL(Time To Live)이란?

**TTL이란, 캐시된 데이터가 자동으로 삭제되기까지의 시간입니다.**

AnchorIQ에서 AI 질의 캐시의 TTL은 5분입니다. 5분이 지나면 Redis가 자동으로 데이터를 삭제합니다. 이렇게 하면 오래된 데이터가 캐시에 영원히 남아있는 것을 방지합니다.

---

## 7.5 전문 검색 엔진이란? — Elasticsearch

**Elasticsearch란, 대량의 텍스트 데이터에서 키워드를 빠르게 검색하는 엔진입니다.**

PostgreSQL의 `LIKE '%호르무즈%'`도 텍스트 검색이 가능하지만, 데이터가 많으면 매우 느립니다. Elasticsearch는 **역인덱스(Inverted Index)** 구조로 수백만 건의 문서에서도 밀리초 단위로 검색합니다.

AnchorIQ에서: 뉴스 기사 본문 검색, AI 결정 로그 검색에 사용합니다.

### 역인덱스(Inverted Index)란?

```
일반 인덱스 (문서 → 단어):
  문서1: "호르무즈 해협에서 유조선 충돌"
  문서2: "수에즈 운하 봉쇄로 유가 급등"
  "호르무즈"를 검색하려면 모든 문서를 읽어야 함 → 느림

역인덱스 (단어 → 문서):
  "호르무즈" → [문서1]
  "유조선"   → [문서1]
  "수에즈"   → [문서2]
  "유가"     → [문서2]
  "호르무즈"를 검색하면 바로 [문서1] 반환 → 빠름!
```

---

# Chapter 8: Kafka

## 8.1 메시지 큐(Message Queue)란?

**메시지 큐란, 프로그램 사이에서 메시지(데이터)를 중간에 저장하고 전달하는 우체국 같은 시스템입니다.**

보내는 사람(Producer)이 우체국(Kafka)에 편지(메시지)를 맡기면, 받는 사람(Consumer)이 자기 속도에 맞춰 가져갑니다. 보내는 사람과 받는 사람이 직접 만날 필요가 없습니다.

## 8.2 Kafka란?

**Kafka란, 대용량 실시간 데이터를 빠르고 안전하게 전달하는 분산 이벤트 스트리밍 플랫폼입니다.**

### 왜 필요한가

```
Kafka 없이 (직접 호출):
  AIS 수집기 → Redis 저장 → Neo4j 저장 → AI 분석
  문제: 하나가 느리면 전체가 느려짐. 하나가 죽으면 전체가 멈춤.

Kafka 있이:
  AIS 수집기 → Kafka → Redis Consumer (독립)
                     → Neo4j Consumer (독립)
                     → AI Consumer (독립)
  장점: 각각 독립적. 하나가 죽어도 나머지는 동작.
```

### Producer(프로듀서)란?

**메시지를 Kafka에 보내는 프로그램입니다.** AnchorIQ에서: AIS 수집기, 뉴스 수집기 등.

### Consumer(컨슈머)란?

**Kafka에서 메시지를 가져가는 프로그램입니다.** AnchorIQ에서: Redis 저장기, Neo4j 업데이터 등.

### Topic(토픽)이란?

**메시지가 저장되는 카테고리(채널)입니다.** AnchorIQ에서 8개 토픽: ais-positions, risk-alerts 등.

### Partition(파티션)이란?

**토픽을 물리적으로 나눈 단위입니다.** 파티션이 3개면, 3개의 Consumer가 동시에 처리할 수 있어 3배 빨라집니다.

### Offset(오프셋)이란?

**파티션 내에서 메시지의 순서 번호입니다.** Consumer가 "어디까지 읽었는지" 추적하는 데 사용합니다. Consumer가 죽었다가 살아나도 마지막 offset부터 재개하므로 메시지 유실이 없습니다.

### Consumer Group이란?

**같은 목적을 가진 Consumer들의 그룹입니다.** 그룹 내에서 하나의 파티션은 하나의 Consumer만 담당합니다(부하 분산). 다른 그룹은 같은 메시지를 독립적으로 읽습니다(다중 소비).

### DLT(Dead Letter Topic)란?

**처리에 실패한 메시지를 격리 보관하는 특수 토픽입니다.**

```
메시지 처리 실패 → 재시도 3번 → 그래도 실패 → DLT로 이동
→ 전체 파이프라인은 멈추지 않음!
→ DLT의 메시지는 나중에 원인 분석 후 수동 재처리
```

---

# Chapter 9: Docker

## 9.1 Docker란?

**Docker란, 프로그램과 그 프로그램이 실행되는 데 필요한 모든 것(라이브러리, 설정, OS 일부)을 하나의 패키지(컨테이너)로 묶어서, 어디서든 동일하게 실행할 수 있게 해주는 도구입니다.**

### 컨테이너(Container)란?

**컨테이너란, 프로그램을 격리된 환경에서 실행하는 것입니다.** 다른 프로그램의 영향을 받지 않고, 어떤 컴퓨터에서든 똑같이 동작합니다.

도시락 통에 비유하면: 음식(프로그램) + 반찬(라이브러리) + 숟가락(도구)을 통째로 담아서, 집에서든 학교에서든 똑같이 먹을 수 있게 하는 것입니다.

### 이미지(Image)란?

**이미지란, 컨테이너를 만들기 위한 설계도(템플릿)입니다.** 이미지에서 컨테이너를 생성합니다. 하나의 이미지로 여러 컨테이너를 만들 수 있습니다.

```
이미지 = 설계도 (변경 불가)
컨테이너 = 설계도로 만든 실체 (실행 중인 프로그램)

neo4j:5-community (이미지) → 컨테이너 실행 → Neo4j 서버 가동
```

### Docker Compose란?

**Docker Compose란, 여러 컨테이너를 한 번에 정의하고 실행하는 도구입니다.**

AnchorIQ는 PostgreSQL, Neo4j, Redis, Kafka 등 11개 서비스가 필요합니다. 이것을 하나씩 수동으로 실행하는 대신, `docker-compose.yml` 파일 하나로 전부 한 번에 시작할 수 있습니다.

```bash
docker compose --profile full up -d   # 11개 서비스 한 번에 시작
docker compose down                   # 전체 종료
```

### 컨테이너 vs 가상머신(VM)

```
가상머신:
  프로그램 + 전체 운영체제(수 GB) 를 포함
  → 시작 시간: 분 단위, 메모리: GB 단위

컨테이너:
  프로그램 + 필요한 라이브러리만 포함 (호스트 OS 커널 공유)
  → 시작 시간: 초 단위, 메모리: MB 단위

핵심 차이: VM은 하드웨어를 가상화, Docker는 프로세스만 격리.
```

---

# Chapter 10: 전체 흐름 (27단계)

사용자가 "호르무즈 근처에 제재국 선박 있어?" 입력 후 Enter:

```
[네트워크 계층]
 ① DNS: localhost → 127.0.0.1 (즉시, /etc/hosts에서)
 ② TCP: Keep-Alive 연결 재사용 또는 3-Way Handshake
 ③ HTTP: POST 요청 구성 (메서드 + 경로 + 헤더 + 본문)
 ④ 패킷: HTTP 데이터를 패킷으로 분할하여 전송

[서버 수신]
 ⑤ Tomcat: TCP 소켓에서 바이트 수신 → HTTP 메시지 파싱

[보안 필터]
 ⑥ CorsFilter: Origin(localhost:3004) 확인 → 허용
 ⑦ JwtAuthenticationFilter: Cookie에서 JWT 추출
 ⑧ JWT 서명 검증: HMAC-SHA256으로 서명 재계산 → 일치 확인
 ⑨ JWT Payload에서 userId=5, role=USER 추출
 ⑩ AuthorizationFilter: authenticated() 확인 → 통과

[요청 라우팅]
 ⑪ DispatcherServlet: POST /api/ai/query → AiQueryController.query()
 ⑫ 역직렬화: JSON → AiQueryRequest 객체 (Jackson)

[비즈니스 로직]
 ⑬ Application Service: API 쿼터 확인 (PostgreSQL 조회)
 ⑭ Redis 캐시 확인: GET ai:result:{hash} → MISS

[AI 처리 — 1차]
 ⑮ OpenClawClient: HTTP POST to OpenClaw (Temperature 0.1)
 ⑯ AI가 자연어 → Cypher 쿼리 변환
 ⑰ 보안 검증: Cypher에 CREATE/DELETE 없는지 확인

[그래프 DB 조회]
 ⑱ Neo4j: Cypher 실행 — Vessel→Company→Country→Sanction 관계 탐색
 ⑲ 결과: 0건 (조건 충족 선박 없음)

[AI 처리 — 2차]
 ⑳ OpenClawClient: 쿼리 결과를 자연어로 변환 (Temperature 0.7)
 ㉑ AI 응답: "호르무즈 인근에 제재 대상국 관련 선박은 확인되지 않았습니다..."

[후처리]
 ㉒ Redis: 결과 캐싱 (TTL 5분)
 ㉓ PostgreSQL: API 사용량 +1 (트랜잭션으로 ACID 보장)
 ㉔ Elasticsearch: 결정 로그 기록 (비동기)

[응답 반환]
 ㉕ 직렬화: Java 객체 → JSON (Jackson)
 ㉖ HTTP 200 OK 응답 → TCP 패킷으로 전송

[화면 표시]
 ㉗ 브라우저: JSON 파싱 → React 컴포넌트가 화면에 렌더링

소요 시간: ~2~5초 (대부분 AI 2회 호출에서 소요)
```
