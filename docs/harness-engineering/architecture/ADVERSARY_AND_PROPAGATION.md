# 부정적(적대) 에이전트와 전파 3채널

## 왜 적대자가 필요한가

기계 검사기(grep)와 추론 적대자(LLM)는 잡는 게 다르다.

```
check-stubs.sh (grep)   →  "// 임시" 라는 글자       →  구문(syntactic)
의미 리뷰어 (agent)      →  "Application 레이어에       →  의미(semantic)
                            비즈니스 로직이 샜다"
```

`check-stubs.sh`는 빠르고 싸지만 "이 코드가 Aggregate 경계를 넘었다", "이 Kafka consumer는 멱등하지 않다" 같은 **의미 위반**은 못 잡는다. 그건 grep이 아니라 **추론하는 적대자**의 일이다.

완성형 하네스 = `기계 게이트(빠름·쌈)` + `의미 적대자(느림·비쌈·똑똑함)`.

## 적대자는 "어떻게 전파"하나

적대자는 누구에게 메일을 보내지 않는다. **자기 출력을 다른 에이전트의 컨텍스트에 강제로 주입**한다. 전파(propagation) = "루프를 닫는 것". 채널은 3개고 전부 Claude Code 원시 기능에 매핑된다.

```
적대자가 위반/버그를 찾음
   │
   ├─(A) 세션 내 재주입 ── 훅 stdout JSON → 코더 컨텍스트에 system-reminder로 박힘
   │     • PostToolUse/Stop 훅이 {"decision":"block","reason":"<위반>"} 반환
   │       → 코더가 reason을 받고 다시 깨어나 고친다
   │     • (asyncRewake:true → 백그라운드 리뷰 후 깨움 — *개념*, 이 리포 settings.json엔 미설정)
   │     • type:"agent" 훅 → 적대자를 라이프사이클에 내장 (기본 Haiku, model로 Sonnet 지정 가능)
   │       단 'block'은 LLM 판단 기반 best-effort이지 결정론적 차단이 아니다
   │
   ├─(B) 에이전트 간 ── 구조화된 return value(schema)를 오케스트레이터가 수집
   │     • find → verify(N명 refute) → 다수결 → confirmed → synthesize
   │     • 전파 = "반환값이 호출 트리를 타고 올라가 부모가 dedup/투표"
   │     • 이 리포에 *구현됨*(verify-findings*.workflow.js) — 단 훅이 자동 트리거하지 않고
   │       **사람이 수동 호출**: finding.sh list → 워크플로우 실행 → resolve
   │
   └─(C) 영속/세션 간 ── findings를 원장 파일 + 메모리에 적재
         • finding.sh add → findings.jsonl (DLT)
         • 다음 세션의 SessionStart 훅이 열린 finding을 다시 주입
         • = Kafka DLT 와 동형. 정상 처리 못 한 것을 별도에 쌓아 사람이 처리.
```

한 줄: **적대자의 결과는 (A) reason/additionalContext로 코더 컨텍스트에 재주입되거나, (B) 구조화 반환값으로 오케스트레이터가 라우팅하거나, (C) 원장+메모리에 적재돼 미래 세션이 읽는다.**

## 이 리포의 구현

| 채널 | 구현 | 상태 (정직하게) |
|------|------|------|
| A | PostToolUse `type:"agent"` 의미 리뷰어 → `reason`/finding 적재 | ✅ 작동 — 단 **LLM 판단 기반**(결정론 block 보장 아님) |
| B | 다수결 검증단 Workflow → 반환값을 메인 루프가 DLT에 반영 | ✅ 구현·1회 실측 — 단 **수동 호출**(훅이 자동 트리거 X) |
| C | `finding.sh` → `findings.jsonl` → SessionStart 재주입 | ✅ 작동 (원장 손상줄도 fromjson?로 견딤) |

> 정직한 주의: B는 "활성 채널"처럼 보이지만 *자동화된 파이프라인이 아니라 수동 절차*다. A의 "block"도 의미 리뷰어가 *판단*해야 발생하는 확률적 반려이지, check-stubs/컴파일 같은 결정론적 차단이 아니다.

### 채널 A — 의미 리뷰어 (PostToolUse agent)

`.claude/settings.json`의 `type:"agent"` 훅이 [`scripts/harness/review-protocol.md`](../../../scripts/harness/review-protocol.md)를 따라 방금 쓴 백엔드 `.java`를 리뷰한다. 위반이면 `finding.sh add`로 적재(C) + block(A). 깨끗하면 통과.

프로토콜 핵심: **고신뢰·구체적 위반만 block**, 확신 없으면 통과. (false positive가 코더를 가두는 게 더 나쁘다 — 적대자 검증의 기본 규율.)

**외부 grounding(연구 보강)**: LLM이 LLM을 판단하는 self-critique는 약하다(`RESEARCH_HARNESS_AND_MULTIAGENT.md` §2 — Reflexion·Renze&Guven 향상이 전부 외부 신호 의존). 그래서 적대자는 판단 **전에** `scripts/harness/ground-check.sh <file>`로 대상 모듈을 실제 컴파일한다. 컴파일 에러가 나오면 **그 에러가 1차 증거**(LLM 추측보다 우선)이고, 통과하면 타입/시그니처 차원은 재추측하지 않는다. 즉 결정론적 신호로 의미 판단을 grounding.

### 채널 C — findings DLT 원장

```bash
# 적재 (적대자나 사람이)
bash scripts/harness/finding.sh add \
  --file "backend/.../FooApplicationService.java" \
  --rule "ARCHITECTURE.md §Application=오케스트레이션만" \
  --sev high \
  --desc "Application 서비스에 리스크 계산 도메인 로직 누수"

# 열린 것 확인 / 해결
bash scripts/harness/finding.sh list --open
bash scripts/harness/finding.sh resolve <id> --note "RiskScoringDomainService로 이동"
```

`findings.jsonl` 한 줄:
```json
{"id":"f-20260605154440-26581","ts":"2026-06-05T15:44:40Z","status":"open","severity":"high","file":"...","rule":"ARCHITECTURE.md §Application","desc":"..."}
```

**열린 finding은 닫힐 때까지 매 세션 SessionStart에서 재노출**된다 → 휘발하지 않고 사람이 처리할 때까지 압박이 유지된다. 이게 DLT의 본질: 실패를 삼키지 않고 가시화한다.

### 채널 B — 다수결 검증단 (Workflow)

**스크립트**: [`scripts/harness/workflows/verify-findings.workflow.js`](../../../scripts/harness/workflows/verify-findings.workflow.js).
DLT의 열린 finding을, finding마다 **3개의 서로 다른 렌즈**를 가진 적대자가 인용 코드+지배 문서를 직접 읽고 **반증(refute) 시도** → 과반(2/3) 반증이면 거짓양성으로 폐기, 살아남으면 confirmed.

```
verify-findings.workflow.js
  parallel(findings)                       ← finding마다 동시에
    └ parallel(LENSES 3)                    ← 3렌즈가 동시에 반증 시도
        • code-reality   : 인용 파일을 읽어 위반이 코드에 실재하나
        • spec-contract  : 지배 plan/ 문서 계약을 정말 어겼나
        • severity-skeptic: 막을 가치가 있나, 수용 가능한 부채인가
    → 과반 반증이면 confirmed=false
  반환 verdicts → 메인 루프가 finding.sh resolve 로 DLT 반영 (B→C)
```

> 워크플로우 스크립트는 파일시스템 접근이 없다. 그래서 메인 루프가 `finding.sh list`로 열린 finding을 읽어 `args`로 넘기고, 결과를 받아 `finding.sh resolve`로 닫는다. (전파 = "반환값이 호출 트리를 타고 올라가 메인 루프가 라우팅".)

**실제 실행 결과**(2026-06-05, 6 에이전트·192k 토큰·78초):

| finding | 판정 | 패널 |
|---------|------|------|
| Notifier 전략패턴 누락 의심 | **거짓양성** | 3/3 만장일치 반증 — 실제론 인터페이스+Map 디스패치(정석 전략패턴), automation 모듈이라 규칙 대상도 아님 |
| 임시 destination 처리 (규칙10) | **거짓양성(과반)** | 2/3 반증 — DB 스키마에 destination 컬럼이 없어 스펙 준수. 단 code-reality 1명이 "실제 발송 실패 가능" 소수의견(별도 기능 버그로 확인 권장) |

다수결의 가치가 그대로 드러난 실행이다: 의심(finding)을 무비판 수용하지 않고, **코드를 직접 읽어 거짓양성을 걸러내며**, 렌즈 간 불일치(소수의견)로 *놓칠 뻔한 실제 버그 후보*까지 표면화했다. 관점 다양화가 없으면 못 잡는 신호다.

## 설계 결정

- **A→C 연결(best-effort)**: 의미 리뷰어(agent 훅)는 도구를 쓸 수 있어 `finding.sh`를 직접 호출해 적재한다. 단 이 전체 경로가 **LLM 판단 + Bash 가용성에 의존하는 best-effort**다 — 리뷰어가 위반을 *판단*해야 block이 나오고(결정론 보장 아님), DLT 적재(C)가 누락되면 코더가 수동으로 `finding.sh add` 한다. (결정론적 게이트는 check-stubs·컴파일 쪽이고, 이 의미 채널은 확률적이다.)
- **적대자 ≠ 나그(nag)**: 기본값을 "통과"로 둔 건 의도다. 막는 비용이 통과시키는 비용보다 클 때만 막는다.
- **DLT는 메커니즘만 추적, 데이터는 무시**: `findings.jsonl`은 gitignore. 원장 CLI·SessionStart 배선(메커니즘)은 버전관리.
