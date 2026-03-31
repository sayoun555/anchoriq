## 세션 작업 요약 (2026-03-30 두 번째)

### 1. JWT localStorage → HttpOnly Cookie 전환

**배경:** AGENTS.md 15번 규칙 위반 — Refresh Token이 localStorage에 저장되어 XSS 공격 시 토큰 탈취 가능

**변경 내용:**
- `CookieProvider.java` 생성 — HttpOnly, SameSite=Lax, Path=/ 쿠키 관리
- `AuthController` — 로그인/리프레시 시 Set-Cookie 헤더로 토큰 전송, 응답 body에서 토큰 제거
- `JwtAuthenticationFilter` — Cookie에서 access_token 추출 (Authorization 헤더도 호환 유지)
- `SecurityConfig` — AuthenticationEntryPoint 추가 (401 JSON 응답)
- 프론트 `client.ts` — `withCredentials: true`, localStorage 완전 제거
- 프론트 `authStore.ts` — localStorage 참조 제거
- 프론트 `WebSocketContext.tsx`, `useWebSocket.ts` — Authorization 헤더 제거 (Cookie 자동 전송)

**결과:**
- Access Token: HttpOnly Cookie, Max-Age 5시간 (18000000ms)
- Refresh Token: HttpOnly Cookie, Max-Age 7일
- XSS로 토큰 탈취 불가

---

### 2. React + Vite → Next.js 16 App Router 마이그레이션

**변경 내용:**
- `create-next-app`으로 Next.js 16 프로젝트 생성 (Turbopack)
- `react-router-dom` → Next.js 파일 기반 라우팅 (App Router)
  - `useNavigate()` → `useRouter()` from `next/navigation`
  - `useParams()`, `useSearchParams()` → Next.js 버전
  - `<Link to=` → `<Link href=`
- `import.meta.env.VITE_` → `process.env.NEXT_PUBLIC_`
- Route Group: `(auth)` (로그인/회원가입), `(dashboard)` (메인 레이아웃)
- `ProtectedRoute` → `AuthGuard` 컴포넌트 (useEffect로 router.replace)
- `WebMvcTestConfig.java` — @WebMvcTest용 최소 Application 클래스
- Sidebar — 모바일 반응형 (햄버거 메뉴 + 오버레이)
- Leaflet/Cytoscape — `dynamic import` (SSR 제외)
- `next.config.ts` — API/WS rewrites 프록시 설정

**라우트 구조:**
```
src/app/
  (auth)/login, signup
  (dashboard)/, map, graph, timeline, vessels, vessels/[imo],
    ports, ports/[locode], risk, workflows, workflows/new,
    alerts, pricing, settings
```

---

### 3. Graph 페이지 — Cytoscape 에러 수정 + 한/영 토글

**문제:** `isHeadless`/`notify` TypeError 무한 반복, 노드 라벨 없음, 관계선 없음

**원인:**
- `react-cytoscapejs`가 re-render 시 파괴된 인스턴스 참조
- `useEffect` 의존성 `[graph]`로 무한 루프
- 백엔드 Neo4j 쿼리에서 relationships를 파싱하지 않음
- Neo4j raw 데이터 (`_id`, `_labels`) ↔ 프론트 타입 (`id`, `type`) 불일치

**해결:**
- `react-cytoscapejs` 제거 → `cytoscape` 직접 import + DOM 컨테이너 마운트
- `initedRef`로 1회만 초기화, `[!!graph]` 의존성으로 중복 실행 차단
- `graphRef`로 stale closure 방지
- `buildElements()` — Neo4j raw `_id`/`_labels`/`_startNodeId` 매핑
- `Neo4jGraphExpansionRepository` — relationships 파싱 추가 (`toRelMap`)
- Cypher 쿼리에 `collect(DISTINCT m)` + `collect(DISTINCT r)` 추가
- 한/영 토글 버튼 — 고유명사는 영어 유지, 타입만 한글 (항만/Port)
- NodeDetail 패널 — 속성명 한/영 전환 (`riskLevel` → `위험등급`)
- `edge[label]` 셀렉터로 label 없는 edge 경고 제거
- 배경 클릭 시 `setSelectedNode(null)`, 노드 tap `stopPropagation()`

---

### 4. Map 다크 테마 팝업/툴팁

**문제:** Leaflet 기본 흰색 배경에 밝은 글자색 → 안 보임

**해결:** `globals.css`에 Leaflet 팝업/툴팁 다크 테마 CSS 추가
- `.leaflet-popup-content-wrapper`, `.leaflet-tooltip` — 다크 배경 + 밝은 텍스트

---

### 5. Vessel Detail / Port Detail undefined 수정

- `grossTonnage?.toLocaleString()`, `latitude?.toFixed()` — optional chaining
- `yearBuilt != null ? String() : null` — undefined → `-` 표시
- `InfoRow` value 타입 `string | undefined | null` 허용
- Port Detail — `terminals`, `maxDraft`, `vesselCount`, `avgWaitDays` null-safe
- Card 컴포넌트 — `CardHeader pb-3` 간격 추가

---

### 6. AIS Neo4j Consumer 배치 처리 + Kafka 병목 해결

**문제:** 초당 수백 건 AIS 메시지를 건건이 Neo4j find+save → 커넥션 풀 고갈

**해결:**
- `AisNeo4jConsumer` — ConcurrentHashMap 버퍼에 최신 상태만 유지, 10초마다 배치 flush
- `aisKafkaListenerContainerFactory` — AIS 전용 Consumer Factory, concurrency 3 (파티션 3개 매칭)
- `RedisGeoCleanupScheduler` — 1분마다 만료된 MMSI를 GEO에서 제거 (유령 선박 방지)

---

### 7. Redis 데이터 정책 수정

- `maxmemory 256MB` + `allkeys-lru` 설정 (기존: 0/noeviction)
- `ai:task:*` 찌꺼기 2400개+ 삭제
- Docker Compose에 이미 설정되어 있었으나 런타임에 미적용 상태 → 수동 설정

---

### 8. Workflow 생성 폼 개선

- Schedule & Cooldown 카드 추가 (Active Hours 시간대, Cooldown 재알림 방지)
- Trigger Type 한글 설명 추가 + Port Congestion 추가
- Action에 Discord 추가 + 삭제 버튼
- Preview 카드 (설정 요약 실시간 미리보기)
- `triggerCondition` JSON.stringify로 전송 (백엔드 JSONB 호환)
- 백엔드 `WorkflowCreateRequest` — description, triggerType 필드 추가

---

### 9. 수집기 제어 API + Settings UI

- `CollectorController` — GET /api/collectors, POST start/stop/start-all/stop-all
- `@PreAuthorize("hasRole('ADMIN')")` — 관리자만 접근
- `CollectorName` — displayName 한글 추가 (AIS 선박 위치, 기상 데이터 등)
- Settings 페이지 — 수집기별 토글 스위치, 전체 시작/중지, 상태 표시
- 일반 유저는 수집기 관리 카드 미표시

---

### 10. 기타

- Chokepoint 중복 제거 (8→6개)
- Open-Meteo URL 수정 (`marine-api.open-meteo.com`)
- `@MockBean` → `@MockitoBean` 마이그레이션 (Spring Boot 3.4)
- Kafka를 Docker Compose `core` 프로필에 추가
- 프론트 포트 3004, Access Token 5시간
- `/workflows/new` 라우트 + New Workflow 버튼 Link 연결
- `AuthGuard` — 렌더 중 router.replace → useEffect로 이동 (React 규칙 준수)
