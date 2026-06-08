# COMPLIANCE — 하네스 주입이 *설계 준수율*을 바꾸나 (프로세스 거버넌스 측정)

> 사전등록: [`../dataset-compliance.json`](../dataset-compliance.json) (생성 전 작성 — post-hoc 합리화 방지).
> 올바른 기준: 이전 GENQUALITY는 *결과 품질*을 쟀다(틀린 기준). 우리 하네스는 *프로세스 거버넌스* 목적이므로, 올바른 측정 = "주입이 설계 *준수*를 바꾸나".
> 측정 차원: ARCHITECTURE §Application — 핵심 비즈니스 로직이 *도메인*에 있으면 COMPLIANT, *Application 서비스에 inline*이면 VIOLATION.
> 원시 산출물: 4개 생성물의 전체 코드는 세션 subagent transcript에 보존됨(agentId a2f380…/aafa76…/adf0ef…/afb6fd…). 아래에 *준수를 결정하는* Application-service 코드를 인용.

## Run 1 — 2026-06-08 · 2작업 × 2arm, 블라인드(코드 직접)

| | comp-1 (리스크 집계) | comp-2 (플랜 제한 판단) | 준수율 |
|---|---|---|---|
| **Arm A (governing-doc 규칙 주입)** | ✅ 준수 | ✅ 준수 | **2/2** |
| **Arm B (주입 없음)** | ✅ 준수 | ✅ 준수 | **2/2** |

**차이 0 — 양쪽 다 100% 준수.**

### 증거 (준수를 결정하는 Application-service 위치)
- comp-1 A: 로직 → `RiskWeightPolicy.aggregate()`(VO) + `RiskSignals`(VO) + `VesselRiskAggregationServiceImpl`(도메인서비스). App `VesselRiskScoringServiceImpl` = 입력 포장 후 위임만.
- comp-1 **B(주입X)**: 로직 → `RiskAggregationServiceImpl`(core 도메인서비스) + `RiskWeights`/`RiskSignals`(VO). App `RiskAggregationApplicationServiceImpl` = 검증 + 위임만.
- comp-2 A: 로직 → `PlanFeaturePolicyImpl`(도메인서비스) + `PlanQuota`(VO). App = 위임만.
- comp-2 **B(주입X)**: 로직 → `FeatureAccessPolicyImpl`(도메인서비스) + `AccessDecision`(VO). App `FeatureAccessApplicationServiceImpl` = DTO변환 + 위임만(@Transactional 없음까지 정확).

→ **Arm B(주입 없음)도 4/4로 도메인에 로직 배치.** 에이전트가 "following the project's DDD/OOP/SOLID rules"라며 기존 코드(`RiskScore`·`Plan`·`Feature`)를 탐색해 컨벤션을 *스스로 추론*했다.

## 결론 — 천장(null result), 그리고 *우리 capstone과 동형*

1. **타깃 주입의 *준수* 한계효용 ≈ 0 (이 규칙에선).** governing-doc이 ARCHITECTURE §Application을 명시 주입해도, *앰비언트 베이스라인*(repo의 CLAUDE.md + 기존 DDD 코드 + 모델의 일반 DDD 지식)이 *이미 충분*해 Arm B도 준수했다.
2. **검증-측 capstone과 같은 경계**: 모델은 *추론 가능한* 규칙(잘 알려진 DDD)을 앰비언트만으로 이미 따른다 → 거기선 주입이 *redundant*. 하네스의 한계효용은 *추론 불가한 프로젝트-특정 규칙*(특정 Tier 배정·특정 계약 등 모델이 못 추측하는 것)에만 날 것 — v-next의 경계와 *동형*. **나는 추론 가능한 규칙을 골라서 천장을 쳤다.**
3. **정직한 함의(따가움)**: 이 측정은 하네스의 *준수 거버넌스 가치를 입증하지 못했다* — 오히려 *redundant일 수 있음*을 시사한다. governing-doc 주입의 진짜 값은 (a) 모델이 못 추론하는 *특정 계약*을 짚어줄 때, 또는 (b) 앰비언트가 약한 *대규모/긴 세션*에서 드리프트를 막을 때일 텐데 — 둘 다 이번에 *측정 안 했다*.

## ⚠️ 한계 (GENQUALITY의 죄를 반복 안 하려고 명시)
- **n=2, 둘 다 천장.** "차 0"은 동등성 증명이 아니라 *검정력 부족* — 더 큰 신호를 줄 *추론 불가 규칙*으로 안 쟀다.
- **앰비언트 누수**: Arm B도 repo 안에서 돌아 CLAUDE.md·기존코드를 받음 → 이건 "무-하네스"가 아니라 "*앰비언트 vs 앰비언트+타깃주입*" 비교다. 진짜 "무-하네스"(빈 컨텍스트)였다면 Arm B가 덜 준수했을 수 있다 → **측정된 한계효용은 *과소평가일 수도, 정확할 수도* — 모른다.**
- **규칙 선택 편향**: "Application=오케스트레이션만"은 잘 알려진 DDD. 진짜 AnchorIQ-특정·비추론 규칙을 골랐다면 결과가 달랐을 수 있다.
- **준수 ≠ 품질**: GENQUALITY가 보였듯 과준수는 과설계가 될 수 있다(여기선 준수만 봄).

## 한 줄

> 잘 알려진 DDD 규칙에선 **타깃 주입이 앰비언트 베이스라인 대비 준수율을 *못 올렸다*(둘 다 100%)** — 모델+앰비언트가 이미 충분. 검증-측 capstone과 *동형의 경계*: 하네스의 값은 *추론 가능한* 것엔 redundant, *추론 불가한 프로젝트-특정 계약*에만 날 것(미측정). 정직하게 — 이 측정은 하네스의 거버넌스 가치를 **입증 못 했고, redundant 가능성을 시사**한다.
