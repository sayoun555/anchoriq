# AnchorIQ Frontend Error Report

> 작성일: 2026-03-29 (2차 점검 후 업데이트)
> 테스트 환경: http://localhost:3002 (React + Vite dev server)
> 백엔드: http://localhost:8082 (Spring Boot)
> 테스트 계정: demo@anchoriq.com / demo1234

---

## 심각도 분류

| 등급 | 의미 |
|------|------|
| CRITICAL | 페이지 크래시 (ErrorBoundary 발동, 사용 불가) |
| HIGH | 기능 오작동 (데이터 표시 안 됨, UI 깨짐) |
| MEDIUM | 경고/성능 이슈 |
| LOW | 사소한 UI 이슈 |

---

## 수정 완료 (RESOLVED)

### ~~CRITICAL - Workflows 페이지 크래시~~ RESOLVED
- `Array.isArray(data.data)` 체크 추가됨 → 더 이상 크래시 안 함
- 파일: `frontend/src/features/workflow/WorkflowListPage.tsx:19`

### ~~CRITICAL - Alerts 페이지 크래시~~ RESOLVED
- `Array.isArray(data.data)` 체크 추가됨 → 더 이상 크래시 안 함
- 파일: `frontend/src/features/alerts/AlertRulesPage.tsx:20`

### ~~HIGH - War Room "undefined vessels tracked"~~ RESOLVED
- `dashboard?.totalVessels ?? 0` 폴백 추가됨
- 파일: `frontend/src/features/dashboard/RiskSummaryCard.tsx:19`

### ~~HIGH - War Room Risk Overview 카드 숫자 누락~~ RESOLVED
- `dashboard?.criticalCount ?? 0` 등 폴백 추가됨
- 파일: `frontend/src/features/dashboard/RiskSummaryCard.tsx:10-14`

### ~~HIGH - War Room Active Alerts 숫자 누락~~ RESOLVED
- `dashboard?.activeAlerts ?? 0` 폴백 추가됨
- 파일: `frontend/src/features/dashboard/RiskSummaryCard.tsx:43`

### ~~HIGH - Settings Plan 값 누락~~ RESOLVED
- `user?.plan ?? 'FREE'` 폴백 추가됨
- 파일: `frontend/src/features/settings/SettingsPage.tsx:73`

---

## 이번 세션에서 수정 (FIXED)

### 1. HIGH - Graph Cytoscape `notify` 에러 → FIXED
- **페이지**: `/graph`
- **증상**: `TypeError: Cannot read properties of null (reading 'notify')` + 57개 매핑 경고
- **원인**: React StrictMode 이중 마운트 시 Cytoscape 인스턴스가 destroy된 후에도 애니메이션 프레임 콜백이 실행
- **수정 내용**:
  - `mountedRef` 추가하여 언마운트 후 상태 업데이트 방지
  - cleanup에서 `removeAllListeners()` 호출 후 `destroy()`
  - `cy` 콜백에서 `mountedRef.current` 체크
- **수정 파일**: `frontend/src/features/graph/GraphView.tsx`

### 2. HIGH - 403 에러 시 로그인 리다이렉트 안 됨 → FIXED
- **증상**: 토큰 만료/무효 시 서버가 403 반환 → 빈 대시보드 표시 (로그인 페이지로 이동 안 함)
- **원인**: `authStore.loadUser()`가 401만 처리하고 403은 무시 → `isAuthenticated: true` 유지
- **수정 내용**: 403도 401과 동일하게 토큰 삭제 + 인증 상태 초기화
- **수정 파일**: `frontend/src/stores/authStore.ts`

---

## 남은 이슈 (REMAINING)

### 3. MEDIUM - War Room 데이터 위젯 "No data available"

- **페이지**: `/` (War Room)
- **영향 위젯**: Risk Trend, Port Congestion Ranking, Chokepoint Status
- **증상**: 모든 위젯이 "No data available" 또는 빈 차트 표시
- **원인**: 백엔드 API가 200 응답하지만 빈 데이터 반환 (데이터 수집기 미실행)
- **해결**: Phase 3 (데이터 수집) 또는 Phase 4 (온톨로지) 구현 후 데이터 채워지면 자동 해결
- **참고**: 프론트엔드 코드는 정상 — empty state 처리 완료

### 4. MEDIUM - Map 마커 미표시

- **페이지**: `/map`
- **증상**: Leaflet 지도 렌더링 정상, vessel/chokepoint 마커 없음
- **원인**: `/api/map/vessels`, `/api/map/chokepoints` API가 빈 데이터 반환
- **해결**: Phase 3 데이터 수집 후 자동 해결

### 5. MEDIUM - Recharts width/height -1 경고

- **영향**: War Room, Risk Dashboard의 차트 영역
- **증상**: 콘솔 경고 `The width(-1) and height(-1) of chart should be positive`
- **원인**: `ResponsiveContainer`가 레이아웃 계산 전에 렌더링 시도
- **현재 상태**: `RiskTrendChart.tsx`에 `minHeight: 200px` + `height={200}` 이미 적용됨
- **참고**: 데이터가 채워지면 차트가 정상 표시될 가능성 높음. 프로덕션 빌드에서는 StrictMode 비활성으로 경고 감소

### 6. MEDIUM - API 중복 호출 (dev 모드)

- **영향**: 모든 페이지에서 동일 API 2~4회 호출
- **원인**:
  - React 18 StrictMode → `useEffect` 2회 실행 (dev 모드 전용, 프로덕션에서는 1회)
  - 일부 컴포넌트 리렌더링으로 추가 호출
- **현재 상태**: 기능상 문제 없음, 프로덕션 빌드에서 자동으로 절반 감소
- **개선 방향**: React Query 등 데이터 페칭 라이브러리 도입 시 캐싱/중복 방지 자동 해결

### 7. MEDIUM - Graph 시각적 품질

- **페이지**: `/graph`
- **증상**: 거대한 회색 원으로 노드가 뭉쳐 보임 (개별 노드 식별 어려움)
- **원인**: `cose` 레이아웃의 기본 설정이 대량 노드에 적합하지 않음
- **개선 방향**: 레이아웃 파라미터 조정 (`nodeSpacing`, `idealEdgeLength` 등) 또는 초기 데이터 양 제한

### 8. LOW - Pricing Subscribe 버튼 무반응

- **페이지**: `/pricing`
- **증상**: Subscribe 버튼 클릭 시 아무 동작 없음
- **원인**: 결제 연동(Stripe/Toss) 미구현 (Phase 2)
- **개선 방향**: 미구현 동안 버튼 비활성화 + "Coming Soon" 표시

### 9. LOW - WebSocket 연결 경고

- **증상**: 간헐적 WebSocket 연결 실패 경고
- **원인**: SockJS 기반 연결이 서버 상태에 따라 실패
- **현재 상태**: 기능에 영향 없음

---

## 수정 파일 요약

| 파일 | 수정 내용 | 상태 |
|------|----------|------|
| `frontend/src/features/graph/GraphView.tsx` | mountedRef 추가, cleanup 개선 | FIXED |
| `frontend/src/stores/authStore.ts` | 403 에러 처리 추가 | FIXED |
| `frontend/src/features/workflow/WorkflowListPage.tsx` | Array.isArray 체크 | 이미 수정됨 |
| `frontend/src/features/alerts/AlertRulesPage.tsx` | Array.isArray 체크 | 이미 수정됨 |
| `frontend/src/features/dashboard/RiskSummaryCard.tsx` | ?? 0 폴백 | 이미 수정됨 |
| `frontend/src/features/settings/SettingsPage.tsx` | ?? 'FREE' 폴백 | 이미 수정됨 |
| `frontend/src/hooks/useRisk.ts` | Array.isArray 체크 | 이미 수정됨 |
| `frontend/src/features/dashboard/CongestionRanking.tsx` | Array.isArray 체크 | 이미 수정됨 |

---

## 결론

- **CRITICAL 이슈**: 0개 (전부 해결됨)
- **HIGH 이슈**: 0개 (전부 해결됨)
- **MEDIUM 이슈**: 7개 (대부분 데이터 미삽입 또는 dev 모드 한정)
- **LOW 이슈**: 2개

남은 MEDIUM 이슈 대부분은 Phase 3 (데이터 수집) 구현 후 자동 해결 예상.
