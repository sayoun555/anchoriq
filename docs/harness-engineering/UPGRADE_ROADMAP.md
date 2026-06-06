# 하네스 고도화 로드맵 — 논문 근거 기반

> `docs/papers/`(16편, 신뢰도 티어링)의 검증된 발견을 우리 하네스의 구체 컴포넌트 업그레이드로 매핑.
> 관통 원칙 하나: **외부 grounding ↑, LLM 자기판단 ↓** (모든 항목이 이걸로 수렴).

## 6 업그레이드

| # | 업그레이드 | 근거 논문 | 어디에 | 우선 | 상태 |
|---|-----------|----------|--------|------|------|
| ① | **도구 쥔 적대자/패널** — 검증자가 "읽기"만이 아니라 실행/조회로 grounding | CRITIC ICLR'24(도구 빼면 자가검증 악화) · Agent-as-a-Judge Meta'24(도구쓰는 agent-judge 60~70%→90%) | PostToolUse 적대자 · 채널 B 패널 | **높음** | **✅ 배선 완료** — 6렌즈 + 커스텀 에이전트 3종(`.claude/agents/harness-*`) + context7 MCP + env-gated 런타임 렌즈 (`verify-findings-grounded.workflow.js`). v-next로 런타임 렌즈 가치 실증 |
| ② | **궤적·단계 검증** — 최종 코드만이 아니라 diff/과정을 단계별로 | Agent-as-a-Judge(전체 궤적) · Lightman ICLR'24(process>outcome, 단계별 오류위치) | 채널 B 입력을 diff로 | 높음 | ⬜ |
| ③ | **루프/낭비 감지·교정** — 코더가 redundant exploration·looping 하면 중간에 잡아 교정 | SWE-PRM IBM'25(추론시 PRM, SWE-bench +10.6pp) | 새 process-monitor 훅 | 중 | ⬜ |
| ④ | **Reflexion 메모리** — fix 반려 시 *왜 실패했는지*를 DLT에 적재 → 다음 시도가 학습 | Reflexion NeurIPS'23(언어적 피드백을 episodic memory에) | `finding.sh`에 reflection 필드 | 중 | ⬜ |
| ⑤ | **judge 편향 완화** — 패널에 순서 swap·다중 호출 | Zheng NeurIPS'23(position·self-preference bias, swap·2회로 완화) | `verify-findings*.workflow.js` | 중(싸다) | ⬜ |
| ⑥ | **bounded self-repair 루프** — ground-check 종료조건 + max-N + escalate | [SELF_REPAIR_GUARDS.md](SELF_REPAIR_GUARDS.md) (Tier A/B/C) | 새 워크플로우 | 높음(크다) | ⬜(설계만) |

## 스킬스 + MCP — 검증 차원별 (이 세션에 실재)

| 도구 | 종류 | 검증 차원 | 용도 |
|------|------|----------|------|
| `verify` | 스킬 | 행위/런타임 | 앱 실행→동작 관찰 (브라우저→API→데이터→응답) — **런타임 갭 직격** |
| `code-review` | 스킬 | 정합성 | diff correctness 정밀 리뷰 |
| `security-review` | 스킬 | 보안 | 변경분 보안 점검 |
| `vercel-plugin:verification` | 스킬 | 프론트 e2e | Next.js 풀스토리 검증 |
| playwright / chrome-devtools | MCP | 행위 | 폼 저장→트리거→실제 발송 확인 |
| context7 | MCP | 계약/API | 프레임워크 API가 *최신 문서* 기준 맞나 |
| Postgres MCP *(미설정)* | MCP | 데이터 | 실제 영속됐나 SELECT |

**배선 완료 (2026-06-06)** — `verify-findings-grounded.workflow.js`가 **6렌즈 × 전용 도구**로 배선됨. 정적 4렌즈는 항상, 런타임 2렌즈는 `args.env=true`일 때(앱/DB 떠야 의미). 무관 차원 렌즈는 `none(not ...)`+low 로 자기표를 빼 다수결을 왜곡 안 함:
```
execution-grounded → ground-check.sh(컴파일/테스트)         [정적]  ✅ 배선
spec-retrieval     → plan-search.sh(절 추출)               [정적]  ✅ 배선
code-reality       → agentType: harness-code-reviewer       [정적]  ✅ 배선
api-currency       → context7 MCP(최신 API 문서)            [정적]  ✅ 배선
security           → agentType: harness-security-reviewer    [정적]  ✅ 배선
runtime-data       → harness-runtime-verifier + Postgres MCP/psql/sqlite  [env]  ✅ 배선(dormant)
behavior-runtime   → harness-runtime-verifier + verify방법론/playwright   [env]  ✅ 배선(dormant)
```

**스킬 → 커스텀 에이전트 배선법.** 워크플로우 서브에이전트가 bundled 스킬(verify/code-review/security-review)을 `Skill` 툴로 직접 호출하는 건 보장 안 됨 → 2가지로 부착:
- **(a) 스킬 방법론을 에이전트 시스템프롬프트에 내장** — `.claude/agents/harness-{code-reviewer,security-reviewer,runtime-verifier}.md`. runtime-verifier가 verify 스킬의 "런타임 관찰(코드 import 금지, 표면까지 가서 캡처)"을, code/security-reviewer가 각 리뷰 차원을 담음.
- **(b) `agentType`으로 렌즈에 부착** — 워크플로우가 lens.agentType을 `agent(prompt, {agentType, schema})`로 전달. schema와 합성됨(커스텀 프롬프트 + StructuredOutput).
- MCP(context7·playwright·Postgres)는 패널 에이전트가 **ToolSearch로 온디맨드 로드**. Postgres MCP는 미설정 → 설정 전엔 psql/sqlite 폴백(v-next에서 sqlite로 실증).

## 왜 ①이 1순위인가

- **논문이 직접 지지**: CRITIC은 "도구 없는 자가검증은 약하거나 악화", Agent-as-a-Judge는 "도구 쓰는 agent-judge가 plain LLM-judge(60~70%)를 90%로". 즉 검증자에게 도구를 주는 게 가장 근거 강한 업그레이드.
- **런타임 갭을 닫음**: 우리가 못 메운 "코드 정합성까지만, 실제 실행은 안 봄"의 그 1겹.
- **이미 절반 있음**: `ground-check.sh`(컴파일)·`plan-search.sh`(검색)가 있으니, 패널 렌즈가 이걸 쓰게만 하면 됨.

## 우선순위 제안

1. **① 도구 쥔 패널** (시제품 완료) → behavior 렌즈에 `verify`/playwright 추가로 런타임까지
2. **⑤ judge 편향 완화** (싸고 빠름)
3. **⑥ bounded self-repair 루프** (가드 다 갖춤, 종료조건=ground-check)
4. ②③④는 그다음
