# AnchorIQ — 프로젝트 개요

> 선박/해운 공급망 리스크 탐지 및 액션 자동화 플랫폼
> 팔란티어 Foundry/Gotham 스타일 포트폴리오 프로젝트

---

## 목차
- [프로젝트 정보](#프로젝트-정보)
- [기술 스택](#기술-스택)
- [데이터 소스](#데이터-소스)
- [데이터 규모](#데이터-규모)
- [문서 구조](#문서-구조)

---

## 프로젝트 정보

| 항목 | 내용 |
|------|------|
| **이름** | AnchorIQ |
| **부제** | 선박/해운 공급망 리스크 탐지 및 액션 자동화 플랫폼 |
| **목적** | 포트폴리오 (실 서비스 X) |
| **개발** | 솔로, 바이브코딩 |
| **컨셉** | 팔란티어 Foundry/Gotham 스타일 |

---

## 기술 스택

| 레이어 | 기술 | 역할 |
|--------|------|------|
| Backend | Spring Boot (Java) | 온톨로지 엔진 + API |
| Frontend | React | 대시보드 (지도/그래프/타임라인) |
| Graph DB | Neo4j Community Edition | 온톨로지 관계 그래프 |
| RDBMS | PostgreSQL | 유저/결제/구독/트랜잭션 |
| Search | Elasticsearch | 뉴스/AI 판단 로그 전문 검색 |
| Cache | Redis | 실시간 선박 위치 + 캐싱 |
| Streaming | Kafka | 실시간 이벤트 스트리밍 |
| AI | OpenClaw (OpenAI 엔진 게이트웨이) | 리스크 판단 + 의사결정 |
| Automation | n8n | 워크플로우 자동화 |
| Infra | Docker Compose | 원클릭 실행 |

---

## 데이터 소스

> 전부 무료, 한국에서 접근 가능 확인 완료

| # | 데이터 | API | 방식 | 비고 |
|---|--------|-----|------|------|
| 1 | 선박 위치 + 정보 | AISstream.io | WebSocket | 무료, 키 발급 |
| 2 | 항만 정보 | UN/LOCODE | 벌크 + REST | 서드파티 API 활용 |
| 3 | 항만 혼잡도 | UNCTAD Port Tracker | 크롤링 | 직접 구현 |
| 4 | 날씨/태풍/해양 | Open-Meteo | REST | 완전 무료, 키 불필요 |
| 5 | 제재 목록 | UN Security Council + OpenSanctions | XML/JSON | 공개 데이터 |
| 6 | 뉴스 | GNews | REST | 무료 티어 (100건/일) |
| 7 | 유가 | EIA API | REST | 무료, 키 발급 |
| 8 | 환율 | Frankfurter.app | REST | 완전 무료, 키 불필요 |
| 9 | 지정학 리스크 | GDELT | REST | 무료, 15분 갱신 |
| 10 | 해양 경계(EEZ) | Marine Regions | GeoJSON | 정적 다운로드 |
| 11 | 초크포인트 지리 | Natural Earth / OSM | GeoJSON | 정적 다운로드 |

---

## 데이터 규모

| 항목 | 결정 | 근거 |
|------|------|------|
| **범위** | 아시아 발착 전체 | 수만 척, 포폴에서 "실무급" 인상 |
| **홉 깊이** | 4홉 | AI 의사결정 시나리오 전부 커버 |
| **초크포인트** | 6개 | 호르무즈, 말라카, 바브엘만데브, 수에즈, 대만해협, 파나마 |

### 왜 아시아 발착 전체인가?

- 전 세계 → DB 무거움, 솔로 개발에 과함
- 특정 항로 3개만 → 가벼워서 최적화 의미 없음
- **아시아 발착 전체** → 적당한 무게감 + 6개 초크포인트 전부 커버 + "아시아 해운 허브 관점" 스토리

### 왜 4홉인가?

```
1홉: 선박 → 항만 (기본 조회)
2홉: 선박 → 항만 → 날씨 (단순 연결)
3홉: 선박 → 회사 → 국가 → 제재 (제재 조회)
4홉: 선박 → 회사 → 국가 → 제재 + 초크포인트 리스크 (AI 의사결정 가능) ← 여기
6홉: 모회사 자회사 딥 분석 (소유구조 데이터 없으면 의미 없음)
```

---

## 문서 구조

```
/anchoriq/
  PROJECT_OVERVIEW.md        ← 현재 파일 (개요 + 스택 + 데이터)
  ARCHITECTURE.md            ← 온톨로지 모델 + 데이터 파이프라인 + 시스템 아키텍처
  DB_OPTIMIZATION.md         ← Neo4j/PostgreSQL/Elasticsearch/Redis 최적화 전략
  AUTH_PAYMENT.md            ← 인증 + 결제 + 구독 설계
  UI_DESIGN.md               ← 팔란티어 감성 대시보드 설계
  AI_DECISION_ENGINE.md      ← AI 의사결정 시나리오 + OpenClaw 연동
  DATA_PIPELINE.md           ← 11개 API 수집/정제/융합 상세
  IMPLEMENTATION_PLAN.md     ← 구현 순서 / 페이즈 계획
```

---

## TODO (미결정 항목)

- [x] 기술 스택
- [x] 데이터 소스 (11개 확정)
- [x] 데이터 규모 (아시아 발착, 4홉)
- [x] DB 최적화 전략
- [ ] OAuth2 소셜 로그인 제공자 선택
- [ ] 결제 시스템 선택 (Toss vs Stripe)
- [ ] 배포 전략
- [ ] 모노레포 vs 멀티레포
- [ ] 프로젝트 구조 / 모듈 분리
- [ ] 구현 순서 / 페이즈 계획
