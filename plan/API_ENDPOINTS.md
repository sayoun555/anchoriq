# AnchorIQ — API 엔드포인트 목록

> 총 174개 API + 3개 WebSocket 엔드포인트

---

## 목차
- [인증](#인증)
- [결제/구독](#결제구독)
- [선박](#선박)
- [항만](#항만)
- [항로/초크포인트](#항로초크포인트)
- [리스크](#리스크)
- [AI](#ai)
- [온톨로지](#온톨로지)
- [제재](#제재)
- [기상/해양](#기상해양)
- [이상 탐지](#이상-탐지)
- [시장 데이터](#시장-데이터)
- [뉴스/지정학](#뉴스지정학)
- [지도](#지도)
- [대시보드 위젯](#대시보드-위젯)
- [워크플로우](#워크플로우)
- [알림](#알림)
- [시뮬레이션](#시뮬레이션)
- [내보내기](#내보내기)
- [글로벌 검색](#글로벌-검색)
- [즐겨찾기](#즐겨찾기)
- [비교](#비교)
- [유저](#유저)
- [외부 API](#외부-api)
- [관리자](#관리자)
- [데이터/시스템](#데이터시스템)
- [WebSocket](#websocket)

---

## 인증

`/api/auth` — 5개

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/auth/signup` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인 → JWT 발급 | X |
| POST | `/api/auth/refresh` | Access Token 갱신 | Refresh Token |
| POST | `/api/auth/logout` | 로그아웃 (Refresh Token 무효화) | O |
| GET | `/api/auth/me` | 내 정보 조회 | O |

---

## 결제/구독

`/api/payments` — 7개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/payments/plans` | 구독 플랜 목록 (Free/Pro/Enterprise) | X | |
| POST | `/api/payments/subscribe` | 구독 결제 (Stripe or Toss) | O | |
| POST | `/api/payments/cancel` | 구독 취소 | O | |
| GET | `/api/payments/history` | 결제 이력 (페이지네이션) | O | |
| GET | `/api/payments/subscription` | 현재 구독 상태 | O | |
| POST | `/api/payments/webhook/stripe` | Stripe 웹훅 수신 | X (서명 검증) | |
| POST | `/api/payments/webhook/toss` | Toss 웹훅 수신 | X (서명 검증) | |

---

## 선박

`/api/vessels` — 13개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/vessels` | 선박 목록 (필터: type, flag, riskLevel) | O | Free |
| GET | `/api/vessels/{imo}` | 선박 상세 | O | Free |
| GET | `/api/vessels/{imo}/risk` | 선박 리스크 정보 | O | Free |
| GET | `/api/vessels/{imo}/relationships` | 선박 온톨로지 관계 (4홉) | O | Free |
| GET | `/api/vessels/{imo}/history` | 선박 위치 이동 이력 | O | Pro |
| GET | `/api/vessels/{imo}/route` | 현재 항로 + 예상 도착 | O | Pro |
| GET | `/api/vessels/{imo}/events` | 선박 관련 이벤트 이력 | O | Pro |
| GET | `/api/vessels/near` | 특정 좌표 반경 N km 선박 (Redis GEO) | O | Free |
| GET | `/api/vessels/sanctioned` | 제재 연관 선박 목록 | O | Pro |
| GET | `/api/vessels/statistics` | 선박 통계 (타입별, 국적별, 리스크별) | O | Free |
| POST | `/api/vessels/watchlist` | 관심 선박 등록 | O | Pro |
| DELETE | `/api/vessels/watchlist/{imo}` | 관심 선박 해제 | O | Pro |
| GET | `/api/vessels/watchlist` | 내 관심 선박 목록 | O | Pro |

---

## 항만

`/api/ports` — 8개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/ports` | 항만 목록 (필터: country, congestionLevel) | O | Free |
| GET | `/api/ports/{locode}` | 항만 상세 | O | Free |
| GET | `/api/ports/{locode}/congestion` | 항만 혼잡도 | O | Free |
| GET | `/api/ports/{locode}/vessels` | 현재 정박 선박 목록 | O | Free |
| GET | `/api/ports/{locode}/history` | 혼잡도 변화 이력 | O | Pro |
| GET | `/api/ports/{locode}/eta` | 입항 예정 선박 목록 | O | Pro |
| GET | `/api/ports/statistics` | 항만 통계 (혼잡도 랭킹) | O | Free |
| GET | `/api/ports/ranking` | 혼잡도 높은 항만 TOP 10 | O | Free |

---

## 항로/초크포인트

`/api/routes`, `/api/chokepoints` — 7개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/routes` | 항로 목록 | O | Free |
| GET | `/api/routes/{id}/chokepoints` | 항로별 초크포인트 | O | Free |
| GET | `/api/chokepoints` | 초크포인트 6개 현황 | O | Free |
| GET | `/api/chokepoints/{name}` | 초크포인트 상세 (리스크, 통과 선박 수) | O | Free |
| GET | `/api/chokepoints/{name}/vessels` | 현재 통과 중인 선박 목록 | O | Free |
| GET | `/api/chokepoints/{name}/traffic` | 통과 트래픽 추이 (차트용) | O | Pro |
| GET | `/api/chokepoints/{name}/incidents` | 최근 사건 이력 | O | Free |

---

## 리스크

`/api/risk` — 14개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/risk/dashboard` | 워룸 대시보드 (HIGH/MEDIUM/LOW 건수) | O | Free |
| GET | `/api/risk/alerts` | 실시간 알림 목록 (페이지네이션) | O | Pro |
| GET | `/api/risk/alerts/{id}` | 알림 상세 | O | Pro |
| POST | `/api/risk/alerts/{id}/acknowledge` | 알림 확인 처리 | O | Pro |
| POST | `/api/risk/alerts/{id}/dismiss` | 알림 무시 처리 | O | Pro |
| GET | `/api/risk/timeline` | 타임라인 뷰 (이벤트→판단→액션) | O | Pro |
| GET | `/api/risk/heatmap` | 리스크 히트맵 데이터 (지도용) | O | Free |
| GET | `/api/risk/score/vessel/{imo}` | 선박별 리스크 스코어 (0~100) | O | Free |
| GET | `/api/risk/score/route/{id}` | 항로별 리스크 스코어 | O | Free |
| GET | `/api/risk/score/chokepoint/{name}` | 초크포인트별 리스크 스코어 | O | Free |
| GET | `/api/risk/score/port/{locode}` | 항만별 리스크 스코어 | O | Free |
| GET | `/api/risk/trends` | 리스크 추세 (일별/주별 그래프) | O | Pro |
| GET | `/api/risk/report/daily` | 일일 리스크 리포트 | O | Pro |
| GET | `/api/risk/report/weekly` | 주간 리스크 리포트 | O | Enterprise |

---

## AI

`/api/ai` — 13개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| POST | `/api/ai/query` | 자연어 질의 | O | Free (일 5건) |
| GET | `/api/ai/briefing` | 오늘의 리스크 브리핑 | O | Pro |
| POST | `/api/ai/whatif` | What-if 시뮬레이션 | O | Enterprise |
| GET | `/api/ai/decisions` | AI 판단 이력 (Elasticsearch) | O | Pro |
| POST | `/api/ai/report/generate` | AI 리포트 생성 | O | Enterprise |
| GET | `/api/ai/report/{id}` | 생성된 리포트 조회 | O | Enterprise |
| GET | `/api/ai/recommendations` | AI 추천 액션 목록 | O | Pro |
| POST | `/api/ai/recommendations/{id}/apply` | 추천 액션 실행 | O | Enterprise |
| GET | `/api/ai/usage` | 내 AI 사용량 (잔여 횟수) | O | Free |

---

## 온톨로지

`/api/ontology` — 8개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/ontology/graph` | 그래프 뷰 데이터 (노드 + 관계) | O | Free |
| GET | `/api/ontology/graph/{nodeId}/expand` | 노드 클릭 → 관계 펼침 | O | Free |
| GET | `/api/ontology/search` | 엔티티 검색 | O | Free |
| GET | `/api/ontology/path` | 두 엔티티 간 최단 관계 경로 | O | Pro |
| GET | `/api/ontology/sanctions/network` | 제재 연관 네트워크 그래프 | O | Pro |
| GET | `/api/ontology/company/{name}/vessels` | 회사 소유 선박 목록 | O | Free |
| GET | `/api/ontology/country/{code}/vessels` | 국적별 선박 목록 | O | Free |
| GET | `/api/ontology/statistics` | 온톨로지 통계 (노드 수, 관계 수) | O | Free |

---

## 제재

`/api/sanctions` — 8개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/sanctions/list` | 전체 제재 목록 | O | Free |
| GET | `/api/sanctions/{id}` | 제재 상세 | O | Free |
| GET | `/api/sanctions/vessels` | 제재 연관 선박 | O | Pro |
| GET | `/api/sanctions/companies` | 제재 연관 회사 | O | Pro |
| GET | `/api/sanctions/countries` | 제재 대상 국가 | O | Free |
| GET | `/api/sanctions/check/vessel/{imo}` | 선박 제재 여부 스크리닝 | O | Pro |
| GET | `/api/sanctions/check/company/{name}` | 회사 제재 여부 스크리닝 | O | Pro |
| GET | `/api/sanctions/updates` | 최근 제재 변경 이력 | O | Pro |

---

## 기상/해양

`/api/weather` — 5개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/weather/current` | 해역별 현재 기상 | O | Free |
| GET | `/api/weather/typhoons` | 활성 태풍 목록 | O | Free |
| GET | `/api/weather/typhoons/{id}` | 태풍 상세 (경로, 예상 영향) | O | Free |
| GET | `/api/weather/forecast/{zone}` | 해역 기상 예보 | O | Pro |
| GET | `/api/weather/impact/vessels` | 기상 영향 받는 선박 목록 | O | Pro |

---

## 이상 탐지

`/api/anomaly` — 5개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/anomaly/ais-off` | AIS 신호 소실 선박 목록 | O | Pro |
| GET | `/api/anomaly/route-deviation` | 항로 이탈 선박 목록 | O | Pro |
| GET | `/api/anomaly/speed-change` | 비정상 속도 변화 선박 | O | Pro |
| GET | `/api/anomaly/dark-vessels` | 장기 미확인 선박 (다크 쉽) | O | Pro |
| GET | `/api/anomaly/history` | 이상 탐지 이력 | O | Pro |

---

## 시장 데이터

`/api/market` — 8개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/market/oil/current` | 현재 유가 (WTI, Brent) | O | Free |
| GET | `/api/market/oil/history` | 유가 변동 이력 (차트용) | O | Free |
| GET | `/api/market/oil/impact` | 유가 변동이 항로별 운임에 미치는 영향 | O | Pro |
| GET | `/api/market/exchange/current` | 현재 환율 (USD, KRW, EUR) | O | Free |
| GET | `/api/market/exchange/history` | 환율 변동 이력 (차트용) | O | Free |
| GET | `/api/market/exchange/convert` | 환율 계산 (amount, from, to) | O | Free |
| GET | `/api/market/freight-index` | 운임 지수 (유가 + 환율 + 리스크 조합) | O | Pro |
| GET | `/api/market/cost/route/{id}` | 항로별 예상 운항 비용 | O | Enterprise |

---

## 뉴스/지정학

`/api/news`, `/api/geopolitical` — 7개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/news` | 해운 뉴스 검색 (Elasticsearch) | O | Free |
| GET | `/api/news/{id}` | 뉴스 상세 | O | Free |
| GET | `/api/geopolitical/events` | 지정학 이벤트 목록 | O | Free |
| GET | `/api/geopolitical/events/{id}` | 이벤트 상세 | O | Free |
| GET | `/api/geopolitical/hotspots` | 분쟁 핫스팟 지도 데이터 | O | Free |
| GET | `/api/geopolitical/impact/routes` | 지정학이 영향 주는 항로 | O | Pro |
| GET | `/api/geopolitical/impact/chokepoints` | 지정학이 영향 주는 초크포인트 | O | Pro |

---

## 지도

`/api/map` — 6개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/map/vessels` | 지도용 선박 위치 (경량 데이터) | O | Free |
| GET | `/api/map/chokepoints` | 지도용 초크포인트 좌표 + 상태 | O | Free |
| GET | `/api/map/eez` | EEZ 경계 GeoJSON | O | Free |
| GET | `/api/map/routes` | 항로 라인 GeoJSON | O | Free |
| GET | `/api/map/heatmap` | 리스크 히트맵 타일 | O | Free |
| GET | `/api/map/cluster` | 선박 클러스터링 (줌 레벨별) | O | Free |

---

## 대시보드 위젯

`/api/dashboard` — 8개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/dashboard/summary` | 요약 (선박 수, 알림 수, 리스크 현황) | O | Free |
| GET | `/api/dashboard/chokepoint-status` | 초크포인트 6개 실시간 상태 | O | Free |
| GET | `/api/dashboard/top-risks` | TOP 10 리스크 | O | Free |
| GET | `/api/dashboard/recent-events` | 최근 이벤트 피드 | O | Free |
| GET | `/api/dashboard/vessel-count-by-type` | 선박 타입별 수 | O | Free |
| GET | `/api/dashboard/vessel-count-by-flag` | 국적별 선박 수 | O | Free |
| GET | `/api/dashboard/risk-trend` | 리스크 추세 차트 데이터 | O | Pro |
| GET | `/api/dashboard/congestion-ranking` | 항만 혼잡도 랭킹 | O | Free |

---

## 워크플로우

`/api/workflows` — 8개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/workflows` | 내 워크플로우 목록 | O | Pro |
| GET | `/api/workflows/{id}` | 워크플로우 상세 | O | Pro |
| POST | `/api/workflows` | 워크플로우 생성 (트리거 조건 + 액션) | O | Pro |
| PUT | `/api/workflows/{id}` | 워크플로우 수정 | O | Pro |
| DELETE | `/api/workflows/{id}` | 워크플로우 삭제 | O | Pro |
| POST | `/api/workflows/{id}/activate` | 워크플로우 활성화 | O | Pro |
| POST | `/api/workflows/{id}/deactivate` | 워크플로우 비활성화 | O | Pro |
| GET | `/api/workflows/{id}/executions` | 워크플로우 실행 이력 | O | Pro |

---

## 알림

`/api/notifications` — 8개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/notifications/settings` | 알림 설정 조회 | O | Pro |
| PUT | `/api/notifications/settings` | 알림 설정 변경 (Slack/Email) | O | Pro |
| GET | `/api/notifications/history` | 알림 발송 이력 | O | Pro |
| POST | `/api/notifications/test` | 테스트 알림 발송 | O | Pro |
| GET | `/api/notifications/rules` | 내 알림 규칙 목록 | O | Pro |
| POST | `/api/notifications/rules` | 알림 규칙 생성 | O | Pro |
| PUT | `/api/notifications/rules/{id}` | 알림 규칙 수정 | O | Pro |
| DELETE | `/api/notifications/rules/{id}` | 알림 규칙 삭제 | O | Pro |

---

## 시뮬레이션

`/api/ai/whatif` — 4개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/ai/whatif/history` | 내 시뮬레이션 이력 | O | Enterprise |
| GET | `/api/ai/whatif/{id}` | 시뮬레이션 결과 상세 | O | Enterprise |
| DELETE | `/api/ai/whatif/{id}` | 시뮬레이션 삭제 | O | Enterprise |
| GET | `/api/ai/whatif/templates` | 시뮬레이션 템플릿 | O | Enterprise |

---

## 내보내기

`/api/export` — 4개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/export/risk-report/pdf` | 리스크 리포트 PDF | O | Pro |
| GET | `/api/export/vessels/csv` | 선박 목록 CSV | O | Pro |
| GET | `/api/export/alerts/csv` | 알림 이력 CSV | O | Pro |
| GET | `/api/export/ai-briefing/pdf` | AI 브리핑 PDF | O | Pro |

---

## 글로벌 검색

`/api/search` — 2개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/search` | 통합 검색 (선박, 항만, 회사, 국가) | O | Free |
| GET | `/api/search/suggestions` | 검색 자동완성 | O | Free |

---

## 즐겨찾기

`/api/bookmarks` — 3개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| POST | `/api/bookmarks` | 즐겨찾기 추가 (항만/항로/초크포인트) | O | Free |
| GET | `/api/bookmarks` | 내 즐겨찾기 목록 | O | Free |
| DELETE | `/api/bookmarks/{id}` | 즐겨찾기 삭제 | O | Free |

---

## 비교

`/api/compare` — 2개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| POST | `/api/compare/routes` | 항로 비교 (거리, 리스크, 비용) | O | Pro |
| POST | `/api/compare/ports` | 항만 비교 (혼잡도, 리스크) | O | Pro |

---

## 유저

`/api/users` — 4개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| PUT | `/api/users/profile` | 프로필 수정 | O | Free |
| PUT | `/api/users/password` | 비밀번호 변경 | O | Free |
| DELETE | `/api/users/account` | 계정 탈퇴 | O | Free |
| GET | `/api/users/activity` | 내 활동 이력 | O | Free |

---

## 외부 API

`/api/external` — 4개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/external/vessels` | 외부 연동용 선박 API | O | Enterprise |
| GET | `/api/external/risk` | 외부 연동용 리스크 API | O | Enterprise |
| GET | `/api/external/api-key` | API 키 조회 | O | Enterprise |
| POST | `/api/external/api-key/regenerate` | API 키 재발급 | O | Enterprise |

---

## 관리자

`/api/admin` — 10개

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/admin/users` | 유저 목록 | ADMIN |
| GET | `/api/admin/users/{id}` | 유저 상세 | ADMIN |
| PUT | `/api/admin/users/{id}/role` | 유저 권한 변경 | ADMIN |
| PUT | `/api/admin/users/{id}/subscription` | 유저 구독 변경 | ADMIN |
| GET | `/api/admin/stats` | 시스템 통계 | ADMIN |
| GET | `/api/admin/api-usage` | 전체 API 사용량 | ADMIN |
| GET | `/api/admin/data-pipeline/status` | 데이터 파이프라인 상태 | ADMIN |
| POST | `/api/admin/data-pipeline/trigger/{source}` | 수동 수집 트리거 | ADMIN |
| GET | `/api/admin/kafka/lag` | Kafka Consumer Lag | ADMIN |
| GET | `/api/admin/cache/stats` | Redis 캐시 통계 | ADMIN |

---

## 데이터/시스템

3개

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/data-status` | 각 데이터 소스 최종 갱신 시각 | O |
| GET | `/actuator/health` | 헬스체크 | X |
| GET | `/actuator/prometheus` | Prometheus 메트릭 | X (내부) |

---

## 감사 로그

1개

| Method | URL | 설명 | 인증 | 플랜 |
|--------|-----|------|------|------|
| GET | `/api/audit/logs` | 감사 로그 (로그인, 조회, 액션 이력) | O | Enterprise |

---

## WebSocket

3개

| URL | 설명 | 플랜 |
|-----|------|------|
| `/ws/vessels` | 선박 위치 실시간 업데이트 | Pro |
| `/ws/alerts` | 리스크 알림 실시간 푸시 | Pro |
| `/ws/dashboard` | 대시보드 실시간 갱신 | Free |

---

## 총 집계

| 도메인 | 수 |
|--------|-----|
| 인증 | 5 |
| 결제/구독 | 7 |
| 선박 | 13 |
| 항만 | 8 |
| 항로/초크포인트 | 7 |
| 리스크 | 14 |
| AI | 13 |
| 온톨로지 | 8 |
| 제재 | 8 |
| 기상/해양 | 5 |
| 이상 탐지 | 5 |
| 시장 데이터 | 8 |
| 뉴스/지정학 | 7 |
| 지도 | 6 |
| 대시보드 | 8 |
| 워크플로우 | 8 |
| 알림 | 8 |
| 시뮬레이션 | 4 |
| 내보내기 | 4 |
| 글로벌 검색 | 2 |
| 즐겨찾기 | 3 |
| 비교 | 2 |
| 유저 | 4 |
| 외부 API | 4 |
| 관리자 | 10 |
| 데이터/시스템 | 3 |
| 감사 로그 | 1 |
| WebSocket | 3 |
| **총** | **174 + 3 WS = 177** |
