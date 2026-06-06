# 워크플로우 subagent가 StructuredOutput을 안 부르고 종료한다

## 문제

`Workflow`로 돌린 일부 멀티에이전트 작업이 결과 없이 실패했다:

```
parallel[0] failed: agent({schema}): subagent completed without calling StructuredOutput (after 2 in-conversation nudges)
```

- **실패**: deep-research(×2), verify-findings-**rubric**(×1)
- **성공**: verify-findings(read) · -grounded · -intrinsic · -blind (수십 회)

그리고 비슷한 시점에 별개로 `API Error: 529 Overloaded ... Rate limited`도 떴다.

## 원인 — 사실 *두 가지*가 섞임

### (1) 출력 schema 복잡 + 도구작업 과다 → 강제 구조화 출력 누락 (자초)

`agent({schema})`는 끝에 StructuredOutput 도구를 *반드시* 호출해야 한다. 그런데 실패한 것들은 공통적으로 **출력 schema가 복잡 + 도구작업이 길었다.**

| 워크플로우 | 출력 schema required 필드 | 1건당 작업 | 결과 |
|-----------|--------------------------|-----------|------|
| rubric | **7** (cited·compiles·ruleText·violates·worthBlocking·refuted·confidence) | 6항목 × 도구(Read·ground-check·plan-search) | ❌ |
| deep-research | 복잡(claim/verdict schema) | 수십 검색·fetch | ❌ ×2 |
| grounded | 4 | 렌즈당 도구 몇 개 | ✅ |
| intrinsic / blind / read | 3 | 가벼움 | ✅ |

**트랜스크립트 증거**(실패한 rubric subagent): 23개 메시지·34 도구호출(약 5분)을 한 뒤 마지막 assistant 메시지가 **`stop_reason: "stop_sequence"`** — 즉 **StructuredOutput을 호출하지 않고 일반 종료.** 2회 nudge로도 복구 안 됨. 성공한 것들(≤4필드)은 같은 일이 없었다.

→ **긴 도구작업 끝에 에이전트의 "턴 종료"가 강제 출력 호출보다 먼저 트리거된다. 출력 schema가 복잡할수록(채울 게 많을수록) 빠뜨릴 확률↑.**

### (2) 529 Overloaded (서버측, 별개)

`529 Overloaded / Rate limited`는 **Anthropic 서버 일시 과부하**이지 usage limit이 아니다. **워크플로우를 동시에 2개씩 + 서브에이전트 다수를 연속으로 던진 것**(한 세션 10+ 워크플로우)이 순간 요청량을 키워 부추겼다.

## 해결

1. **출력 schema를 단순하게(3~4 required 필드).** 구조화가 필요하면 **체크리스트를 *프롬프트*(과정)로 강제**하고, 그 답은 단일 `reason`/`rubric` *텍스트 필드*에 요약시킨다. (rubric을 7필드 → 3필드로 고치니 해결.)
2. **무거운 워크플로우를 동시에 던지지 말 것.** 병렬 2개 + 다수 subagent는 529를 부른다. **직렬로(하나씩)**, 529가 뜨면 **잠깐 대기 후 재시도**(전형적 backoff).

## 사례 추가 (2026-06-07) — orchestrate-feature의 ③통합·④검증

`orchestrate-feature.workflow.js`를 실제 기능(ETA 지연 등급)에 처음 돌렸을 때, **분해·병렬구현(①②)은 성공**했는데 **통합·검증(③④)이 최종 결과에서 `null`** 로 나왔다(`integration.compiles = None`, `review.verdict = None`).

| 단계 | 출력 schema | 결과 |
|------|------------|------|
| ① decompose | units 배열(중첩) | ✅ (작업이 *읽기* 위주, 출력 직전 도구작업 적음) |
| ② implement ×2 | 4필드 평면 | ✅ |
| ③ integrate | **4필드** (compiles·modulesChecked배열·fixesApplied·remainingIssues) + 무거운 작업(git diff·gradle 컴파일) | ❌ null |
| ④ review | **중첩 객체 배열**(findings[{file,severity,issue,rule}]) + 도구작업 | ❌ null |

- 트랜스크립트: 실패한 ③④ 에이전트에 `stop_sequence` 33~43회. (단 *성공한* 구현자에도 비슷한 카운트가 있어 transcript만으론 단정 불가 — **확정 신호는 "가장 복잡한 schema를 가진 두 단계만 null"** 이라는 점.)
- **중요 뉘앙스**: 워크플로우의 *실제 작업은 성공*했다(코드 13파일 생성, core·api 양 모듈 수동 컴파일 ✅). **깨진 건 self-reporting 레이어**다 — 산출물은 멀쩡한데 그걸 *구조화해서 보고*하는 마지막 한 스텝이 유실됐다. → 즉 이 버그는 "결과가 틀림"이 아니라 "결과를 못 알려줌"으로 나타나, **사용자에겐 '실패'처럼 보이지만 산출물은 정상**일 수 있다(반드시 외부에서 검증할 것).

**고침**: ③을 2필드(`compiles`,`report`), ④를 *문자열 배열*(`findings: string[]`, 중첩 객체 제거)로 단순화. 구조 정보는 산문 필드에 담아 마지막 출력 표면적을 최소화 — 위 (1)과 동일 처방.

> 일반화: **무거운 단계(컴파일·검토·통합)일수록 출력 schema를 가장 단순하게 둬라.** 작업이 무거운 자리 = StructuredOutput 누락 위험이 가장 큰 자리.

## CS 원리

- **강제 도구 호출(forced tool use)의 신뢰성은 컨텍스트 길이·작업량에 반비례한다.** 긴 에이전트 turn에서는 모델이 "할 일을 다 했다"고 판단해 *종료*가 최종 출력 단계보다 먼저 발화할 수 있다 — 직렬화 경계에서 타입이 유실되는 것(troubleshooting/WORKFLOW_ARGS_PASSED_AS_STRING)과 같은 계열의 "경계에서 약속이 깨짐". 대응은 **출력 표면적 최소화**(필드 수↓)로 마지막 한 스텝을 확실하게 만드는 것.
- **클라이언트 rate-limit ≠ 서버 overload(529).** 429(네 한도)와 529(서버 용량)는 원인·대응이 다르다. 529는 **지수 backoff + 동시성 제한(백프레셔)** 으로 대응한다 — 더 던질수록 악화되므로 *덜* 던지는 게 해법.
