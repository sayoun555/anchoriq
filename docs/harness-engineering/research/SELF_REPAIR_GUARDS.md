# 자율 self-repair 루프 — 가드(guardrails)

> "검증자가 버그를 찾으면 자동으로 고치고 다시 검증하는 루프"를 만들 때 **무엇으로 구속해야 하는가**.
> 근거는 동료심사 출처(Tier A) 위주. 원문 PDF·신뢰도 티어는 [`docs/papers/`](../../papers/README.md).

## 핵심 명제

자율 수정 루프의 과제는 **자율성을 최대화하는 게 아니라 bound(한정)하는 것**이다. 그리고 루프의 판정은 **LLM 자기판단이 아니라 외부 신호(컴파일·테스트·도구 결과)에 grounding**되어야 한다. 그렇지 않으면 루프는 자원만 태우거나, **고칠수록 더 나빠진다.**

## 왜 — 자가수정의 근본 한계 (Tier A 증거)

- **외부 피드백 없는 self-correction은 실패하고 때론 악화된다.** "LLMs struggle to self-correct their responses without external feedback, and at times, their performance even degrades after self-correction." — Huang et al., **ICLR 2024** (`2310.01798`).
- **비판적 서베이도 같은 결론**: intrinsic self-correction은 자주 성능을 떨어뜨리고, 기존 연구는 불공정 평가로 자가수정을 **과대평가**했다. — Kamoi et al., **TACL 2024** (`2406.01297`).
- **LLM 심판(judge)은 편향**된다: position·verbosity·self-enhancement bias. — Zheng et al., **NeurIPS 2023** (`2306.05685`).

→ 즉 "LLM이 LLM을 평가하고 LLM이 고치는" 순수 루프는 신뢰할 수 없다. 가드가 필요하다.

## 가드 7 — 각 항목에 동료심사 근거

| # | 가드 | 근거(Tier) | 우리 하네스 |
|---|------|-----------|------------|
| 1 | **외부 피드백 필수** — 루프 판정을 LLM 자기판단에 맡기지 말고 외부 신호로 | Huang ICLR'24 · Kamoi TACL'24 · **CRITIC ICLR'24**(도구 빼면 악화) (A) | ✅ ground-check(컴파일)·check-stubs(grep) |
| 2 | **실행/테스트 grounding 게이트** — 수정 수락 전 실행으로 확인 | Chen ICLR'24 (self-debug +12% w/ tests) · Reflexion NeurIPS'23 (A) | ✅ `ground-check.sh` (옵션 `--test`) |
| 3 | **process(단계) 검증 > outcome** — 최종만이 아니라 중간 단계도 | Lightman ICLR'24 (A) | ✅ 파일 단위 PostToolUse 검사(턴 끝이 아니라 매 변경) |
| 4 | **독립 verifier + judge bias 완화 + 궤적 평가** — verifier ≠ generator, 순서 swap·다중, 결과만 아니라 *과정*을 봄 | Zheng NeurIPS'23 (A) · **Agent-as-a-Judge(Meta'24)** 궤적평가 인간일치 90% vs 60~70% (B) | ✅ 채널 B 패널(검증자≠코더) + 3개 다른 렌즈 |
| 5 | **다수결/다중 샘플** — 단일 판단 금지, 집계 | Wang ICLR'23 self-consistency (A) | ✅ 채널 B 과반(2/3) 판정 |
| 6 | **bounded 반복 + 수렴기준 + 과정보상 교정** — 횟수 cap, "done" 정의, 추론시 교정·rollback | **SWE-PRM(IBM'25)** PRM 교정 SWE-bench 40→50.6% · **SCoRe(DeepMind, ICLR'25)** RL 자가수정 (B) + `2606.01416`(C) | ⬜ 미구현 (자동 루프 자체가 없음) |
| 7 | **reward-hacking 감시** — 수정자가 게이트를 속이기(테스트 약화·삭제) 감시 | `2605.01471`(C: assertion 약화·테스트 삭제 관찰) | ⬜ 회귀 가드 필요 |

> 6·7은 **Tier C(미심사 프리프린트)** 근거라 "방향은 맞지만 수치는 미검증"으로 다룬다.

## 우리 하네스는 이미 1~5를 충족 — 6·7만 남음

연구가 권하는 가드 중 **외부 grounding(1·2), 단계 검증(3), 독립·다수결 검증(4·5)**은 이미 있다. 빠진 건 **자동 루프 자체**인데 — 이건 **일부러 안 만들었다.** 이유가 바로 위 Tier A 증거: 자율 루프는 grounding·bound 없이는 악화되므로, **사람/메인 에이전트를 게이트로 남기는 게** 현재로선 더 안전.

## 만약 self-repair 루프를 추가한다면 (설계)

```
while (attempts < MAX_N) {                 # 가드6: bounded
    fix = 수정()
    if (!ground-check 통과) { attempts++; continue }   # 가드1·2: 외부 grounding 게이트
    verdict = 채널B_패널(독립·다수결)          # 가드4·5
    if (verdict.clean && !회귀(테스트수↓·약화)) return 성공   # 가드7: reward-hacking 감시
    attempts++
}
escalate_to_human()                        # 수렴 실패 → 사람에게
```
핵심: **종료조건 = "외부 게이트 통과 + 독립 검증 clean + 회귀 없음"**, 그 외엔 bound 후 escalate. LLM "스스로 됐다고 판단"은 종료조건이 **아니다.**

## 신뢰도 ↔ 최신 (정직)

- **뼈대(가드 1~5)는 Tier A(2023~24 동료심사)** 로 단정 — 이 주제의 핵심 결론은 여기서 안 흔들림.
- **최신을 "조금만 양보"해서 Tier B 확보**: 2024~25 + 유명 랩(DeepMind·IBM·Meta) + 인정 벤치마크(MATH·SWE-bench·DevAI). 우려와 달리 **최신이면서도 꽤 신뢰할 수 있는** 근거가 있었다 — 가드 4·6을 Tier B 수치로 보강.
- **Tier C(2026 단독저자 프리프린트)는 방향 참조로만** (가드 7 등).
- 교훈: "최신=무조건 저신뢰"는 아님. **유명 랩 + 인정 벤치마크 + 학회 채택 여부**로 최신 중에서도 신뢰 가능한 걸 골라낼 수 있다.

## 출처 (전문·티어는 [`docs/papers/`](../../papers/README.md))

**Tier A (동료심사)**: Huang ICLR'24 `2310.01798` · Kamoi TACL'24 `2406.01297` · Reflexion NeurIPS'23 `2303.11366` · Chen ICLR'24 `2304.05128` · Lightman ICLR'24 `2305.20050` · Zheng NeurIPS'23 `2306.05685` · Wang ICLR'23 `2203.11171` · **CRITIC ICLR'24 `2305.11738`**
**Tier B (최신·준신뢰, 유명 랩)**: SCoRe DeepMind/ICLR'25 `2409.12917` · SWE-PRM IBM'25 `2509.02360` · Agent-as-a-Judge Meta'24 `2410.10934`
**Tier C (프리프린트, 방향 참조)**: `2604.10800` · `2605.01471` · `2606.01416` · `2601.04171` · `2508.00083`
