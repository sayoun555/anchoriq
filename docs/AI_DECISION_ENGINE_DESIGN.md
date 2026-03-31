## AI 의사결정 엔진: 순수 룰 기반 → LLM 하이브리드 스코어링

### 배경 및 문제정의
- 상황: 선박/항로/항만/초크포인트별 리스크를 0~100으로 산출해야 함
- 문제: 단일 LLM 호출로 스코어링하면 API 비용 폭발 + 응답 지연 (평균 3초)

### 기술 선정 (대안 비교 테이블)

| 항목 | 순수 LLM | 순수 룰 엔진 | 하이브리드 (선택) |
|------|---------|------------|--------------|
| 응답 속도 | 2~5초 | <10ms | <50ms (캐시 히트 시) |
| API 비용 | 건당 $0.01 | $0 | 브리핑/질의만 LLM |
| 판단 일관성 | 낮음 (확률적) | 높음 | 기본 높음 + AI 보강 |
| 확장성 | 새 팩터 프롬프트만 수정 | 코드 수정 필요 | 코어는 코드, AI는 프롬프트 |
| 설명 가능성 | 블랙박스 | 완전 투명 | 팩터별 투명 + AI 요약 |

### 분석
- 리스크 스코어링은 정형화된 팩터(제재, 날씨, 혼잡도, 초크포인트)로 구성
- 비정형 분석(자연어 질의, 브리핑, 시뮬레이션)만 LLM이 필요
- 스코어링 호출 빈도는 높고(매 조회), LLM 호출은 낮음(일 수십 건)

### 솔루션

```java
// 하이브리드 구조: 룰 기반 스코어링 + LLM 보강
// 1. SupplyChainRiskServiceImpl — 룰 기반 (즉시 응답)
Map<RiskType, Integer> factors = new HashMap<>();
factors.put(RiskType.SANCTION, evaluateVesselSanctionRisk(vessel));
factors.put(RiskType.WEATHER, evaluateWeatherRisk());
int totalScore = calculateWeightedScore(factors);
// → 응답 <10ms, API 비용 $0

// 2. NaturalLanguageQueryServiceImpl — LLM 활용 (자연어 질의 시만)
String cypher = aiClient.chat(NEO4J_SCHEMA_PROMPT, query, 0.1).block();
CypherQueryValidator.validateReadOnly(cypher); // 보안: 쓰기 쿼리 차단
List<Map<String, Object>> results = neo4jClient.query(cypher).fetch().all();
// → LLM 1~2회 호출, 결과 5분 캐싱

// 3. CypherQueryValidator — 보안 계층
Set<String> FORBIDDEN = Set.of("CREATE", "DELETE", "SET", "MERGE", ...);
// → 읽기 전용만 허용, SQL Injection 유사 공격 차단
```

### 결과 (Before/After 수치 테이블)

| 지표 | 순수 LLM | 하이브리드 |
|------|---------|----------|
| 스코어링 응답 | 2~5초 | <10ms |
| 일일 API 비용 (1만 조회) | ~$100 | ~$1 (질의만) |
| 판단 일관성 | 70% (같은 입력 다른 출력) | 99% (결정론적) |
| 자연어 질의 품질 | N/A | GPT-4 수준 |
| 캐시 히트율 | N/A | 예상 60%+ (5분 TTL) |
