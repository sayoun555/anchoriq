## 세션 작업 요약 (2026-03-30)

### 1. Map 선박 위치 — DEMO 하드코딩 → Redis GEO 연동

**배경:** `MapApplicationServiceImpl.getVesselPositions()`가 `DEMO_POSITIONS` 배열에서 좌표를 가져와 하드코딩된 위치를 반환하고 있었음. Phase 3(데이터 수집) 이전에도 Redis GEO 기반으로 동작해야 함.

**변경 내용:**
- `DemoDataInitializer`에 `seedRedisGeoPositions()` 추가 — 25척 선박 위치를 Redis GEO에 시딩
  - `vessels:positions` (GEO), `vessels:heading:{mmsi}`, `vessels:speed:{mmsi}`, `vessels:status:{mmsi}` 키 생성
  - TTL 24시간 (데모 데이터용, AIS 실시간은 30초)
- `MapApplicationServiceImpl.getVesselPositions()` — Neo4j 선박 목록 + Redis GEO 좌표 조인으로 교체
  - MMSI 기준으로 Neo4j Vessel ↔ Redis GEO position 매핑
  - heading/speed도 Redis에서 조회

**변경 파일:**
- `anchoriq-collector/.../DemoDataInitializer.java` — Redis GEO 시딩 추가
- `anchoriq-collector/.../CollectorConfig.java` — DemoDataInitializer Bean에 RedisTemplate 주입
- `anchoriq-api/.../MapApplicationServiceImpl.java` — 전면 재작성 (Redis GEO 기반)

---

### 2. Map 히트맵 — 빈 리스트 → Redis GEO 기반 선박 밀집도

**배경:** `getHeatmapData()`가 `List.of()`를 반환하고 있었음.

**변경 내용:**
- 9개 주요 해역 핫스팟(싱가포르, 말라카, 상하이, 홍콩, 부산, 호르무즈, 수에즈, 바브엘만데브, 대만해협)에 대해 Redis GEO `GEORADIUS`로 반경 내 선박 수를 집계
- 선박이 없는 경우 기본 강도값(radius/100) 제공

**변경 파일:**
- `anchoriq-api/.../MapApplicationServiceImpl.java` — `getHeatmapData()` 구현

---

### 3. 리스크 트렌드 API — placeholder → 30일치 트렌드 데이터

**배경:** `RiskScoreApplicationServiceImpl.getRiskTrends()`가 `{"message": "Risk trends data"}`만 반환.

**변경 내용:**
- 현재 선박 리스크 분포(CRITICAL/HIGH/MEDIUM/LOW)를 기준으로 30일치 시계열 데이터 생성
- `period`, `data[]`, `summary` 구조로 응답
- DashboardApplicationServiceImpl의 `getRiskTrend()`와 동일한 패턴 (시드 고정 랜덤)

**변경 파일:**
- `anchoriq-api/.../RiskScoreApplicationServiceImpl.java` — `getRiskTrends()` 구현

---

### 4. Open-Meteo 수집기 URL 수정

**배경:** Open-Meteo Marine API의 올바른 URL은 `marine-api.open-meteo.com`이지만 `api.open-meteo.com/v1/marine`으로 설정되어 있어 404 반환.

**변경 파일:**
- `anchoriq-collector/.../OpenMeteoCollector.java` — BASE_URL 수정

---

### 5. @MockBean → @MockitoBean 마이그레이션

**배경:** Spring Boot 3.4에서 `@MockBean`이 `org.springframework.boot.test.mock.bean`에서 deprecated 됨. `org.springframework.test.context.bean.override.mockito.MockitoBean`으로 이전 필요.

**변경 내용:**
1. 3개 테스트 클래스에서 `@MockBean` → `@MockitoBean` 전환 + import 변경
2. `@WebMvcTest`에서 `AnchoriqApplication`의 `@EnableJpaRepositories`가 JPA 컨텍스트를 로드하는 문제 해결:
   - `WebMvcTestConfig.java` 테스트용 최소 Application 클래스 생성 (JPA/Neo4j 어노테이션 제외)
3. `SecurityConfig`를 `@Import`로 명시적 로드하여 실제 Security 필터 체인 테스트
4. `JwtAuthenticationFilter`를 `@MockitoBean` 대신 실제 Bean으로 사용 (JwtTokenProvider만 Mock)
5. `SecurityConfig`에 `AuthenticationEntryPoint` 추가 — 인증 실패 시 403 → 401 응답으로 정석화
6. VesselControllerTest의 Mock 시그니처를 실제 컨트롤러(`findAll()`)에 맞게 수정
7. PageResponse 래핑에 맞게 JSON Path 수정 (`$.data[0]` → `$.data.content[0]`)

**변경 파일:**
- `anchoriq-api/src/test/.../AuthControllerTest.java`
- `anchoriq-api/src/test/.../VesselControllerTest.java`
- `anchoriq-api/src/test/.../RiskControllerTest.java`
- `anchoriq-api/src/test/.../WebMvcTestConfig.java` (신규)
- `anchoriq-api/.../SecurityConfig.java` — AuthenticationEntryPoint 추가

**결과:** core 단위 테스트 + api 컨트롤러 슬라이스 테스트 전부 통과.

---

### 6. Chokepoint 중복 제거 (8 → 6개)

**배경:** Neo4j에 `Bab el-Mandeb` / `Bab-el-Mandeb`, `Taiwan Strait` / `Taiwan` 중복 데이터 존재.

**변경 내용:**
- Neo4j Cypher로 중복 노드 DETACH DELETE
- NaturalEarthLoader의 6개 시드 데이터는 정상 (중복은 이전 세션에서 다른 이름으로 시딩된 것)

**결과:** Chokepoint 6개 정상 (Hormuz, Malacca, Bab-el-Mandeb, Suez, Taiwan, Panama)

---

### 7. 프론트엔드 포트 변경 + CORS 업데이트

- `vite.config.ts` port: 3002 → 3004
- `SecurityConfig.java` CORS allowedOrigins에 `http://localhost:3004` 추가

---

### E2E API 검증 결과 (10개 엔드포인트)

| API | 상태 | 데이터 |
|-----|------|--------|
| Dashboard Summary | ✅ | 25선박, 20항만, 3 high risk |
| Map Vessels | ✅ | 25척 Redis GEO 좌표 |
| Map Heatmap | ✅ | 9개 핫스팟 밀집도 |
| Risk Trends | ✅ | 30일치 시계열 |
| Ontology Stats | ✅ | 86노드, 34관계 |
| Chokepoints | ✅ | 6개 (중복 제거 후) |
| Vessels Paginated | ✅ | 25척, 페이지네이션 동작 |
| Dashboard Risk Trend | ✅ | 30일치 |
| Top Risks | ✅ | 이란 선박 최고 100점 |
| Congestion Ranking | ✅ | 상하이 1위 |

---

### 빌드 상태

```
./gradlew build -x test → BUILD SUCCESSFUL
./gradlew :anchoriq-core:test → BUILD SUCCESSFUL (도메인 단위 테스트)
./gradlew :anchoriq-api:test --tests "com.anchoriq.api.controller.*" → BUILD SUCCESSFUL (컨트롤러 슬라이스 테스트)
```
