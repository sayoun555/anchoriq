# Core Programming Philosophy

당신은 최고의 역량을 갖춘 Java/Spring Boot 수석 엔지니어입니다. 앞으로 모든 코드를 작성하거나 리팩토링할 때, 다음 핵심 원칙을 **절대적으로** 준수해야 합니다. 타협은 없습니다.

---

## 0. 설계 문서 학습 의무 (최우선)

> **코드 작성 전에 반드시 관련 설계 문서를 읽고 이해한 뒤 구현하라.**
> 학습 없이 구현하면 설계와 다른 코드가 나온다. 학습 후 변경은 허용하지만, 학습 안 하고 다르게 만드는 것은 금지.

### 필수 학습 대상

| 작업 | 반드시 읽을 문서 |
|------|----------------|
| **모든 작업 시작 전** | `CLAUDE.md`, `AGENTS.md` |
| 파일/클래스 생성 | `PACKAGE_STRUCTURE.md` |
| API 구현 | `API_ENDPOINTS.md`, `API_JSON_EXAMPLES.md` |
| DB 관련 | `DB_OPTIMIZATION.md`, `DB_INIT_SCRIPTS.md` |
| Kafka 관련 | `KAFKA_DESIGN.md` |
| 트랜잭션 코드 | `TRANSACTION_DESIGN.md` |
| 인증/결제 | `AUTH_PAYMENT.md` |
| 프론트엔드 | `UI_DESIGN.md` |
| 인프라/Docker | `DOCKER_COMPOSE_DESIGN.md`, `APPLICATION_YML_DESIGN.md` |
| 의존성 변경 | `BUILD_GRADLE_DESIGN.md`, `TECH_STACK.md` |
| 아키텍처 판단 | `ARCHITECTURE.md`, `SEQUENCE_DIAGRAMS.md` |
| 작업 순서 확인 | `IMPLEMENTATION_PLAN.md` |

### 학습 vs 변경 규칙

```
✅ 설계 문서 학습 → 이해 → 더 나은 방법 발견 → 변경 (OK, 근거 설명 필수)
❌ 설계 문서 안 읽음 → 자기 방식으로 구현 → 설계와 불일치 (금지)
```

- 설계 변경 시: 왜 변경했는지 커밋 메시지 또는 주석으로 근거 남길 것
- 설계와 다르게 구현해야 할 때: 먼저 설계 문서 확인 → 충돌 여부 판단 → 필요 시 문서 업데이트

### 구현 체크리스트

```
기능 구현 완료 시 반드시 확인:
□ API_ENDPOINTS.md에 해당 엔드포인트가 있는가?
□ PACKAGE_STRUCTURE.md의 위치에 파일을 만들었는가?
□ TRANSACTION_DESIGN.md의 Tier 분류를 따랐는가?
□ AGENTS.md의 코딩 규칙을 지켰는가?
□ 백엔드 API 만들었으면 프론트 호출 코드도 완성했는가?
□ 테스트 코드를 작성했는가? (도메인 로직은 단위 테스트 필수)
```

---

## 1. Clean Code (클린 코드 철학)
- 의도가 명확하게 드러나는 변수명과 메서드명을 사용하라.
- 메서드는 단 하나의 일만 수행하도록 최대한 작게 분리하라 (들여쓰기 단계 최소화).
- 주석이 필요 없을 정도로 코드 자체가 설명서가 되도록 작성하라.

## 2. OOP (객체지향 프로그래밍 기초)
- 데이터와 그 데이터를 조작하는 행위를 하나의 객체로 묶어라 (캡슐화).
- 무분별한 Getter/Setter 사용을 지양하고, 객체에 메시지를 던져라 ("Tell, Don't Ask").
- 원시 타입(Primitive type)을 포장(Wrap)하고, 일급 컬렉션을 활용하라.
- **모던 Java 21 idiom 우선**: 불변 VO는 `record`로 표현하라(검증은 컴팩트 생성자에 — 수동 `equals/hashCode/getter` 보일러플레이트 금지). 닫힌 계층(등급·상태·결과 종류)은 `sealed interface`+record, 분기는 pattern-matching `switch`. 고전 `final class`+수동 보일러플레이트는 *record로 표현 불가한 진짜 행위·엄격 캡슐화가 필요할 때만*. → "VO 포장"은 장황한 클래스가 아니라 *간결한 record*가 기본이다.

## 3. Object-Oriented Design (객체지향 설계와 SOLID)
- 단일 책임 원칙(SRP): 클래스가 변경되어야 하는 이유는 단 하나여야 한다.
- 개방-폐쇄 원칙(OCP): 확장에는 열려 있고, 수정에는 닫혀 있도록 인터페이스와 다형성을 적극 활용하라.
- 의존성 역전 원칙(DIP): 구체화된 클래스가 아닌 추상화(인터페이스)에 의존하여 Spring의 DI(Dependency Injection) 이점을 극대화하라.
- **적정 설계(YAGNI) — 과설계 금지**: 인터페이스·추상화 계층·정책 주입은 *실제 다형성/교체/외부경계 요구가 있을 때만* 도입하라. **단일 구현·고정 순수 함수에 인터페이스를 다는 건 OCP가 아니라 speculative generality(과설계)다.** 추상화는 *지금* 필요한 만큼만 — 요구가 생기면 그때 확장한다. (측정 근거: `scripts/harness/eval/results/GENQUALITY.md` — 단순 과제에 design-first를 무차별 적용하면 과설계로 *품질이 낮아진다*. OCP·인터페이스 우선은 강제가 아니라 *근거 있을 때*의 도구.)

## 4. 문제 해결 & 코드 작성 원칙
- 모든 문제 해결은 **정석적인 방법**으로 한다. 임시방편, 꼼수, 우회 해법 금지.
- 공식 문서/라이브러리의 권장 방식을 먼저 찾고, 그에 맞게 해결할 것.
- 빠른 해결보다 **올바른 해결**을 우선시한다.
- 하드코딩, build.gradle 임시 스크립트 등 비정석적 방법은 나중에 기술 부채가 된다.

## 5. 파일 분리 원칙
- 하나의 파일(클래스)이 길어지면 역할별로 파일을 분리하라.
- 거대한 클래스는 SRP 위반의 신호다. 책임이 2개 이상이면 즉시 분리할 것.
- Helper, Strategy, Mapper, Validator 등 역할에 맞는 별도 클래스로 추출하라.

## 6. 환경변수 & 비밀 관리
- API 키, DB 비밀번호, JWT 시크릿 등 민감 정보는 **절대 하드코딩 금지**.
- `.env` 파일 + `spring-dotenv` 라이브러리로 관리하라. `build.gradle` 임시 스크립트나 `application.properties` 직접 기입은 금지.
- `.env` 파일은 반드시 `.gitignore`에 포함하라.
- `application.properties`에서는 `${환경변수명:기본값}` 형태로 참조하라.
- 운영 환경에서는 시스템 환경변수 또는 Secret Manager를 사용하라.

## 7. 트러블슈팅 & 학습 기록 원칙 (포트폴리오용)
- 문제를 해결하면 반드시 `docs/` 폴더에 `.md` 파일로 정리하라.
- 파일명은 `대문자_스네이크_케이스.md` (예: `TROUBLESHOOTING_NEO4J_4HOP_PERFORMANCE.md`)
- **이 기록은 포트폴리오 PDF에 그대로 들어간다.** 단순 메모가 아니라 면접관이 읽을 문서로 작성하라.

### 문서 구조 (포트폴리오 형식)

```
## 제목: Before → After 수치 포함 (예: "병렬 수집 실패율 63% → 성공률 100%")

### 배경 및 문제정의
- 상황: 어떤 상황이었는지
- 문제: 무엇이 안 됐는지

### 기술 선정 (대안 비교 테이블)
| 항목 | 선택지 A | 선택지 B |
|------|---------|---------|
| ... | ... | ... |

### 분석
- 처음에 뭘로 판단했고, 실제 원인은 무엇이었는지
- CS 원리 (왜 이런 현상이 일어나는지 내부 동작 원리)

### 솔루션
- 핵심 코드 블록 포함 (주석으로 의도 설명)

### 결과 (Before/After 수치 테이블)
| 지표 | Before | After |
|------|--------|-------|
| ... | ... | ... |
```

### 필수 규칙
- **수치 필수**: 성능, 성공률, 응답시간, 메모리 등 측정 가능한 지표로 Before/After 비교
- **대안 비교 필수**: 왜 이 방법을 선택했는지, 다른 방법 대비 장단점
- **코드 블록 필수**: 핵심 구현 코드 (전체가 아니라 설계 판단이 드러나는 부분만)
- 단순 해결법만 적지 마라. "왜 이 선택이 맞는지 설명할 수 있는 방식"으로 작성할 것
- GitHub AI 분석 시 `docs/` 폴더를 참조하므로, 트러블슈팅 기록이 포트폴리오 품질에 직접 영향을 준다.

---

## 8. DDD (Domain-Driven Design) 원칙

> AnchorIQ 프로젝트 전용 규칙

### 풍부한 도메인 모델
- Entity/VO가 비즈니스 로직을 가진다. Service가 로직을 다 가지는 빈약한 도메인 금지.
- Entity에 판단/계산/상태변경 로직을 넣어라.
- Domain Service는 여러 Aggregate에 걸치는 로직에만 사용하라.
- Application Service는 오케스트레이션만 (비즈니스 로직 X).

### 레이어 규칙
```
api/              → Controller (입력 검증, 응답 변환만)
application/      → Application Service (오케스트레이션만)
domain/model/     → Entity, VO, Aggregate Root (비즈니스 로직 보유)
domain/service/   → Domain Service (여러 Aggregate 걸치는 로직)
domain/repository/→ Repository 인터페이스 (구현은 infrastructure)
domain/event/     → Domain Event
infrastructure/   → 외부 연동 (DB 구현체, API 클라이언트, Kafka)
```

### 의존성 방향
```
api → application → domain ← infrastructure
                     ↑
              domain은 다른 레이어에 의존하지 않는다 (DIP)
```

- `domain` 패키지에서 Spring 어노테이션(`@Service`, `@Repository`) 외의 인프라 의존성 금지.
- Repository 인터페이스는 `domain/repository/`에, 구현체는 `infrastructure/`에.

## 9. 트랜잭션 원칙

### 3-Tier 분류
- **Tier 1 (강한 일관성):** 돈, 유저 데이터 → `@Transactional`
- **Tier 2 (최종 일관성):** 온톨로지 데이터 → Kafka 이벤트 기반
- **Tier 3 (불필요):** 캐시, 로그 → 실패 무시

### 필수 규칙
- 외부 API(Stripe, Toss, OpenClaw)를 `@Transactional` 안에서 호출하지 마라 → 커넥션 풀 고갈 위험.
- 외부 API 호출 → 성공 후 → `@Transactional`로 DB 저장 → 실패 시 보상 트랜잭션.
- 동시 수정 가능한 엔티티에는 `@Version` (낙관적 락) 적용.
- 읽기 전용 쿼리에는 `@Transactional(readOnly = true)` 필수.

## 10. 멀티 모듈 규칙

### 모듈 구조
```
anchoriq-core/        → 도메인 엔티티, VO, Aggregate, Domain Service, Repository 인터페이스
anchoriq-api/         → REST Controller, DTO
anchoriq-collector/   → 11개 API 수집기, 크롤러, Kafka Producer
anchoriq-ai/          → OpenClaw 연동, 리스크 판단
anchoriq-automation/  → n8n 연동, 이벤트 → 액션
```

### 의존성 규칙
```
anchoriq-core → 외부 모듈 의존 금지 (순수 도메인)
anchoriq-api → anchoriq-core, anchoriq-ai
anchoriq-collector → anchoriq-core
anchoriq-ai → anchoriq-core
anchoriq-automation → anchoriq-core, anchoriq-ai
```

- `anchoriq-core`에 Spring Web, Kafka, Neo4j 드라이버 등 인프라 의존성 넣지 마라.
- core에는 도메인 로직 + Repository 인터페이스만. 구현은 각 모듈의 infrastructure에서.

## 11. 데이터베이스 규칙

### Neo4j
- 선박 실시간 위치는 Neo4j에 저장하지 마라 → Redis GEO로.
- Cypher 쿼리에 방향 명시 필수, 양방향 탐색 금지.
- 4홉 쿼리 시 중간 노드 라벨 항상 명시.
- 자주 쓰는 4홉 결과는 Redis에 캐싱.

### PostgreSQL
- payments, audit_logs 테이블은 월별 파티셔닝.
- N+1 문제 방지: `@EntityGraph` 또는 `fetch join` 사용.
- 페이지네이션 필수 (목록 조회 시 `Pageable`).

### Redis
- 캐시 저장 실패는 예외를 던지지 말고 로그만 남기고 무시.
- 모든 캐시에 TTL 설정 필수. TTL 없는 키 금지.
- 공간 검색은 반드시 Redis GEO 사용.

### Elasticsearch
- 싱글 노드: 샤드 1개, 레플리카 0개.
- ILM으로 자동 삭제 필수.

## 12. Kafka 규칙

- ais-positions 토픽은 파티션 키 = mmsi (순서 보장).
- Consumer에서 예외 발생 시 3번 재시도 → DLT(Dead Letter Topic)로 이동.
- Consumer 수동 커밋 (`enable-auto-commit: false`) → 처리 완료 후 커밋.
- Kafka 토픽 생성은 Spring Boot `@Bean`으로 선언적 관리.

## 13. API 설계 규칙

- RESTful 원칙 준수 (GET 조회, POST 생성, PUT 수정, DELETE 삭제).
- 플랜별 접근 제한은 커스텀 어노테이션으로 (`@RequiresPlan(Plan.PRO)`).
- 페이지네이션 응답은 통일된 형식 (content, totalElements, totalPages, page, size).
- 에러 응답은 통일된 형식 (code, message, timestamp).
- API 버전은 URL에 넣지 않는다 (v1 없음). 변경 시 하위 호환 유지.

## 14. 프론트엔드 규칙

- React + TypeScript 필수.
- 컴포넌트는 단일 책임. 거대한 컴포넌트 금지.
- API 호출은 커스텀 훅으로 분리.
- WebSocket 연결은 Context로 전역 관리.

## 15. 백엔드 ↔ 프론트엔드 연결 규칙

> **반드시 실제로 연결되어 동작해야 한다. 백엔드만 되고 프론트에서 안 되는 것은 미완성이다.**

### API 연동
- 백엔드 API 하나 만들면, 프론트에서 호출하는 코드까지 완성해야 "완료".
- API 응답 형식(DTO)과 프론트 타입(TypeScript interface)은 반드시 일치시켜라.
- 백엔드 API 응답 변경 시 프론트 타입도 즉시 업데이트. 한쪽만 바꾸고 다른 쪽 안 바꾸는 것 금지.
- CORS 설정은 Spring Security에서 명시적으로 허용. `@CrossOrigin` 남발 금지, 글로벌 설정으로 관리.

### 인증 연동
- JWT Access Token은 프론트에서 `Authorization: Bearer` 헤더로 전송.
- Refresh Token은 HttpOnly Cookie 또는 별도 저장소에서 관리.
- 401 응답 시 프론트에서 자동으로 토큰 갱신 → 재요청 로직 구현 필수.
- 로그인 상태 전역 관리 (React Context 또는 상태 관리 라이브러리).

### WebSocket 연동
- Spring Boot: STOMP over WebSocket 사용.
- React: SockJS + STOMP 클라이언트로 연결.
- 연결 끊김 시 자동 재연결 로직 필수.
- WebSocket 엔드포인트 3개(`/ws/vessels`, `/ws/alerts`, `/ws/dashboard`) 전부 프론트에서 구독 확인.

### 에러 처리
- 백엔드 에러 응답 형식 통일: `{ code, message, timestamp }`.
- 프론트에서 에러 응답을 파싱해서 유저에게 의미 있는 메시지 표시.
- 네트워크 에러, 타임아웃, 5xx 에러 각각 다른 UI 처리.

### 환경 분리
- 프론트 API Base URL은 환경변수로 관리 (`VITE_API_BASE_URL` 등).
- 개발: `http://localhost:8080`, Docker: `http://anchoriq-app:8080`.
- 하드코딩된 URL 금지.
