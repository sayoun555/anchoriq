# AnchorIQ - 결정사항 정리

> 선박/해운 공급망 리스크 탐지 및 액션 자동화 플랫폼

---

## 프로젝트 개요
- **이름**: AnchorIQ
- **부제**: 선박/해운 공급망 리스크 탐지 및 액션 자동화 플랫폼
- **목적**: 포트폴리오 (실 서비스 X)
- **개발**: 솔로, 바이브코딩
- **컨셉**: 팔란티어 Foundry/Gotham 스타일

---

## 기술 스택
- **Backend**: Spring Boot (Java)
- **Frontend**: React
- **Graph DB**: Neo4j Community Edition (무료, 온톨로지)
- **RDBMS**: PostgreSQL (유저/결제/구독/트랜잭션)
- **Search**: Elasticsearch (뉴스/로그)
- **Cache**: Redis (실시간 선박 위치 캐싱)
- **Streaming**: Kafka (실시간 이벤트)
- **AI**: OpenClaw (OpenAI 엔진 게이트웨이)
- **Automation**: n8n (워크플로우 자동화)
- **Infra**: Docker Compose

---

## 데이터 소스 (전부 무료, 한국 접근 가능)

| # | 데이터 | API | 비고 |
|---|--------|-----|------|
| 1 | 선박 위치 + 정보 | AISstream.io | 무료 웹소켓, 실시간 |
| 2 | 항만 정보 | UN/LOCODE | 벌크 다운로드 + 서드파티 API |
| 3 | 항만 혼잡도 | UNCTAD Port Tracker | 크롤링 |
| 4 | 날씨/태풍/해양 | Open-Meteo | 완전 무료, 키 불필요 |
| 5 | 제재 목록 | UN Security Council + OpenSanctions | XML/JSON 공개 |
| 6 | 뉴스 | GNews | 무료 티어 (100건/일) |
| 7 | 유가 | EIA API | 무료, 키 발급 |
| 8 | 환율 | Frankfurter.app | 완전 무료, 키 불필요 |
| 9 | 지정학 리스크 | GDELT | 무료, 키 불필요 |
| 10 | 해양 경계(EEZ) | Marine Regions | GeoJSON |
| 11 | 초크포인트 지리 | Natural Earth / OSM | 정적 데이터 |

---

## 온톨로지 모델

```
Vessel ── owns ──▶ Company ── registered ──▶ Country
  │                   │                         │
  ├── docked_at ▶ Port├── sanctioned_by ▶ UN    ├── geopolitical_risk
  │                   │                         ├── conflict_zone
  ▼                   ▼                         │
Route ──passes──▶ Chokepoint(수에즈, 호르무즈, 말라카)
  │
  ▼
SeaZone ── weather ──▶ Condition
         ── jurisdiction ──▶ EEZ(배타적경제수역)
```

---

## 데이터 파이프라인 (Foundry 스타일)

```
[Raw Layer]        AIS신호, 뉴스, 날씨, 제재목록, 유가, 환율
     ↓ 수집/정제
[Clean Layer]      정규화된 엔티티 매핑
     ↓ 융합
[Ontology Layer]   Neo4j 그래프 — 엔티티 간 관계 자동 연결
     ↓ 분석
[Logic Layer]      리스크 스코어링 + 이상 탐지 + LLM 추론
     ↓ 행동
[Action Layer]     n8n 워크플로우 자동 실행
```

---

## AI 의사결정 시나리오
- 태풍 접근 + 항만 혼잡도 높음 → 대체 항로/항만 추천
- 선박 지연 + 납기 임박 → 자동 알림
- AIS 신호 소실 → 제재 위반 의심 알림
- 자연어 질의: "호르무즈 해협 근처 제재 연관 선박?"
- 매일 아침 리스크 서머리 자동 생성
- What-if 시뮬레이션: "수에즈 3일 봉쇄 시 영향?"

---

## 인증 + 결제
- **로그인**: Spring Security + JWT + OAuth2 (Google/GitHub/Kakao 중 선택 예정)
- **결제**: Toss Payments 또는 Stripe (테스트 모드)
- **구독 플랜**:
  - Free — 대시보드 열람, 일 5건 AI 질의
  - Pro — 실시간 알림, 무제한 AI 질의, n8n 워크플로우
  - Enterprise — What-if 시뮬레이션, API 접근, 커스텀 온톨로지

---

## UI (팔란티어 Gotham 감성)
- 지도 뷰 — 선박 실시간 위치 + 리스크 히트맵 + 초크포인트
- 그래프 뷰 — 엔티티 클릭 → 관계 펼침
- 타임라인 뷰 — 이벤트 → AI 판단 → 액션 실행 이력
- 워룸 대시보드 — 실시간 알림 피드 + 리스크 현황판
- 자연어 질의 바

---

## 데이터 규모
- **범위**: 아시아 발착 전체 (수만 척)
- **홉 깊이**: 4홉
- **초크포인트**: 6개 (호르무즈, 말라카, 바브엘만데브, 수에즈, 대만해협, 파나마)

---

## DB 최적화 전략

### Neo4j (온톨로지 그래프)

**인덱스:**
| 노드 | 필드 | 인덱스 | 이유 |
|------|------|--------|------|
| Vessel | IMO | ✅ | 선박 조회 시작점 |
| Vessel | MMSI | ✅ | AIS 데이터 매핑 키 |
| Vessel | 선박 타입 | ✅ | 필터링용, 변경 안 됨 |
| Vessel | 국적(flag) | ✅ | 필터링용, 변경 안 됨 |
| Port | UN/LOCODE | ✅ | 고정 데이터 |
| Company | 회사명 | ✅ | 제재 조회용 |
| Country | ISO 코드 | ✅ | 제재국 필터 |
| SeaZone | 위치(lat/lon) | ✅ | 공간 검색 (고정 데이터) |
| Sanction | 제재 번호 | ✅ | 거의 안 바뀜 |
| Vessel | 위치(lat/lon) | ❌ | **Redis GEO로 처리** (초당 수백 건 쓰기) |
| Chokepoint | 이름 | ❌ | 6개뿐, 풀스캔 OK |

**4홉 쿼리 최적화:**
- 방향 지정: 양방향 탐색 금지, 항상 관계 방향 명시
- 라벨 필터링: 중간 노드에 라벨 항상 명시
- 조기 필터링: 시작 노드에서 WHERE 조건
- 결과 캐싱: 제재 조회, AI 브리핑 → Redis 캐싱

### PostgreSQL (유저/결제/구독)

**인덱스:**
| 테이블 | 필드 | 이유 |
|--------|------|------|
| users | email | 로그인 조회 |
| users | oauth_provider + oauth_id | 소셜 로그인 |
| subscriptions | user_id + status | 구독 상태 조회 |
| payments | user_id + created_at | 결제 이력 |
| api_usage | user_id + date | Free 플랜 제한 체크 |

**파티셔닝:** payments 테이블 월별 파티셔닝

**커넥션 풀 (HikariCP):**
```yaml
maximum-pool-size: 30
minimum-idle: 10
connection-timeout: 3000
idle-timeout: 600000
```

**쿼리 최적화:** N+1 방지 (fetch join), 페이지네이션 (Pageable), 읽기 전용 트랜잭션

### Elasticsearch (뉴스/AI 로그)

**인덱스 2개만:**
| 인덱스 | 데이터 | 보관 |
|--------|--------|------|
| news | GNews 뉴스 | 90일 (ILM 자동 삭제) |
| ai-decisions | AI 판단 로그 | 180일 (ILM 자동 삭제) |

- 샤드 1개, 레플리카 0개 (싱글 노드)
- nori 플러그인 (한국어 형태소 분석)
- 커넥션 풀: 20

### Redis (실시간 캐싱)

**캐싱 대상 + TTL:**
| 데이터 | 자료구조 | TTL |
|--------|---------|-----|
| 선박 위치 | GEO | 30초 |
| 제재 선박 목록 | SET | 1시간 |
| AI 판단 결과 | STRING | 5분 |
| 항만 혼잡도 | STRING | 30분 |
| API 사용량 카운터 | STRING + INCR | 자정 리셋 |

**공간 검색:** Redis GEO로 "호르무즈 반경 50km 선박" 같은 쿼리 처리

**Eviction:** maxmemory 256mb, allkeys-lru

**장애 대비:** Redis 죽어도 Neo4j/PostgreSQL 직접 조회로 fallback

**커넥션 풀:** 20

### 커넥션 풀 총괄

| DB | 풀 사이즈 | 이유 |
|----|----------|------|
| PostgreSQL | 30 | 메인 트랜잭션 |
| Neo4j | 30 | 4홉 쿼리 시간 소요 |
| Elasticsearch | 20 | 단순 검색 위주 |
| Redis | 20 | 단순 GET/SET, 빠른 반환 |
| **총합** | **100** | |

---

## TODO (아직 미결정)
- [x] DB 최적화 전략
- [ ] OAuth2 소셜 로그인 제공자 선택
- [ ] 결제 시스템 선택 (Toss vs Stripe)
- [ ] 배포 전략
- [ ] 모노레포 vs 멀티레포
- [ ] 프로젝트 구조 / 모듈 분리
- [ ] 구현 순서 / 페이즈 계획
