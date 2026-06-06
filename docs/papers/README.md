# 논문 모음 — self-repair 루프 가드 & 에이전트 검증

> `docs/harness-engineering/SELF_REPAIR_GUARDS.md`의 근거 논문 원문(PDF).
> **신뢰도 최우선** 기준으로 게재지·심사 상태를 출처별로 직접 확인해 티어링했다(검색 요약이 아니라 arXiv 초록 원문 확인).

## ⚠️ 신뢰도 먼저 읽기

- **Tier A (높음)** = 동료심사(peer-review) 통과 + 상위 학회/저널(ICLR/NeurIPS/TACL) + 다수 인용. → 가드의 **뼈대**로 쓸 것.
- **Tier B (최신·준신뢰)** = 2024~25, **유명 랩(DeepMind·Meta·IBM 등) + 인정 벤치마크(SWE-bench·MATH·DevAI)**. 일부는 학회 채택 보고. → 최신 corroboration으로 **수치까지 인용 가능**(단 단독 단정은 Tier A로).
- **Tier C (낮음~중)** = **2025~26 arXiv 프리프린트, 미심사.** 일부 단독저자. → **방향 corroboration용**, 수치·주장을 단독 근거로 쓰지 말 것.
- **최신 ↔ 신뢰도 트레이드오프**: 가장 신뢰도 높은 건 2023~24(심사에 시간 걸림). 2025~26은 아직 프리프린트뿐 → "최신"을 원하면 신뢰도를 양보해야 함. 이 점을 가드 문서에 명시했다.

## Tier A — 동료심사 (뼈대)

| 파일 | 논문 | 게재지 | 핵심 |
|------|------|--------|------|
| `2310.01798_…ICLR2024.pdf` | LLMs Cannot Self-Correct Reasoning Yet (Huang et al., Google DeepMind) | **ICLR 2024** | 외부 피드백 없으면 self-correct 실패, **때론 성능 악화** |
| `2406.01297_…TACL2024.pdf` | When Can LLMs Actually Correct Their Own Mistakes? (Kamoi et al.) | **TACL 2024** | 비판적 서베이: intrinsic 자가수정 자주 성능↓, 기존 연구 **과대평가** |
| `2303.11366_…NeurIPS2023.pdf` | Reflexion (Shinn et al.) | **NeurIPS 2023** | 외부(단위테스트) 신호 기반 반성 → HumanEval 80→91% |
| `2304.05128_…ICLR2024.pdf` | Teaching LLMs to Self-Debug (Chen et al.) | **ICLR 2024** | 실행 결과로 디버그, 단위테스트 있으면 +12% |
| `2305.20050_…ICLR2024.pdf` | Let's Verify Step by Step (Lightman et al., OpenAI) | **ICLR 2024** | **process 검증 > outcome** (단계별 오류 위치) |
| `2306.05685_…NeurIPS2023.pdf` | Judging LLM-as-a-Judge (Zheng et al.) | **NeurIPS 2023** | LLM 심판의 position·verbosity·self-enhancement bias + 완화(순서 swap·2회 호출) |
| `2203.11171_…ICLR2023.pdf` | Self-Consistency (Wang et al.) | **ICLR 2023** | 다수 샘플 **다수결**로 정답 선택 (+17.9% GSM8K) — 채널 B 패널의 뿌리 |
| `2305.11738_…ICLR2024.pdf` | CRITIC: LLMs Can Self-Correct with Tool-Interactive Critiquing (Gou et al.) | **ICLR 2024** | **도구(검색 API) 없이는 자가수정이 악화** — "외부 grounding 필수"의 직접 증거 |

## Tier B — 최신·준신뢰 (2024~25, 유명 랩 + 인정 벤치마크)

| 파일 | 논문 | 출처/소속 | 핵심 |
|------|------|----------|------|
| `2409.12917_…ICLR2025.pdf` | SCoRe: Training LMs to Self-Correct via RL (Kumar, Agarwal et al.) | **Google DeepMind**, ICLR 2025 보고(arXiv Comments 미기재) | 멀티턴 온라인 RL로 자가수정 학습 → MATH +15.6%, HumanEval +9.1% (SFT의 붕괴 문제 해결) |
| `2509.02360_…2025.pdf` | Course-Correcting SWE Agents with PRMs / SWE-PRM (Gandhi, Tsay et al.) | **IBM Research**, 2025 프리프린트 | 추론시 **Process Reward Model**이 궤적 오류를 실시간 교정 → SWE-bench Verified **40.0→50.6%** (+10.6pp), 교정비용 ~$0.2 |
| `2410.10934_…Meta2024.pdf` | Agent-as-a-Judge: Evaluate Agents with Agents (Zhuge et al.) | **Meta**, 2024 프리프린트 | 결과만 보는 LLM-judge(인간일치 60~70%) 대신 **전체 궤적 평가** → 90% 일치, DevAI 벤치 |

## Tier C — 최신 프리프린트 (미심사, 방향 참조만)

| 파일 | 논문 | 상태 | 주의 |
|------|------|------|------|
| `2604.10800_…verify-before-you-fix.pdf` | Verify Before You Fix (Gajjar) | arXiv 2026, **NeurIPS'26 제출(미채택)**, 단독저자 | "실행검증 전 수정금지"; ⚠️ 검증 빼면 불필요수정 **+131.7%** (앞서 잘못 인용한 61/73 아님) |
| `2605.01471_…practical-limits.pdf` | Practical Limits of Autonomous Test Repair (Lee) | arXiv 2026, 미심사, 단독저자 | bounded autonomy; 38% 산출물 실패, **assertion 약화·테스트 삭제 같은 reward hacking** 관찰 |
| `2606.01416_…self-healing-orchestrators.pdf` | Self-Healing Agentic Orchestrators (Babu & Agrawal) | arXiv 2026, 미심사 | "reliability=bounded runtime control"; 98.8% vs retry 94.5% (단일 벤치) |
| `2601.04171_…agentic-rubrics.pdf` | Agentic Rubrics as Contextual Verifiers (Raghavendra et al.) | arXiv 2026, 미심사 | execution-**free** 검증 루브릭(우리 grounding과 결 다름), SWE-Bench +3.5pp |
| `2508.00083_…survey-code-gen.pdf` | Survey on Code Generation w/ LLM Agents (Dong et al., 유명저자) | arXiv 2025, "work in progress" | ⚠️ 초록에서 내가 인용한 *검증* 내용 미확인 — 원문 정독 전 단독 인용 금지 |

## 메타 (정직)

- 처음에 `verify-findings`/딥서치 하네스로 검증하려 했으나 **워크플로우가 실패**(StructuredOutput 에러). 그래서 출처를 **수동 WebFetch로 게재지 직접 확인**해 티어링했다.
- 급한 1차 검색 요약에 **수치 오류(131.7→61/73 오기)·오귀속(survey)**이 있었고, 이 README가 그 정정본이다.
- Tier C는 "최신이라 흥미롭지만 미검증". 결론은 **Tier A로만 단정**하고 Tier C는 보조로.
