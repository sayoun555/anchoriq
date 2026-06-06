# 대규모 코드 작성 오케스트레이션

> 큰 기능을 *여러 파일/모듈*에 걸쳐 구현할 때, 단일 코더의 컨텍스트 압박을 줄이면서 멀티에이전트 코딩의 실패모드(공유 코드 상태 충돌)는 피하는 패턴.
> 구현: [`scripts/harness/orchestrate-feature.workflow.js`](../../scripts/harness/orchestrate-feature.workflow.js). 근거: [RESEARCH_HARNESS_AND_MULTIAGENT.md](RESEARCH_HARNESS_AND_MULTIAGENT.md).

## 왜 — 순진한 멀티에이전트 코딩은 품질을 떨어뜨린다

여러 에이전트가 **상호의존 코드를 동시에** 고치면 결정이 충돌하고 봉합이 안 된다 — Cognition "Don't Build Multi-Agents", Anthropic 합의. 그래서:

- **상호의존 코드 공동작성 = 단일 에이전트(선형, 공유 컨텍스트)가 더 신뢰성 높다.**
- **멀티가 정석인 경우 = 서로 의존 없는 *독립 단위*** → 병렬 작성 후 **하나의 통합자가 봉합**. (= orchestrator-worker 패턴, Anthropic multi-agent research system.)

이 워크플로우는 후자를 구현한다.

## 4단계 설계

```
① Decompose (1 에이전트)
   작업 → 파일-disjoint 독립 단위[] + 단위간 계약(sharedContracts)
   · targetFiles는 단위 간 절대 안 겹침(겹치면 한 단위로 합침 → 병렬 쓰기 충돌 원천 차단)
   · 의존은 코드가 아니라 *계약(인터페이스/시그니처)*으로만 → 이음새를 미리 못박음
   · 각 단위에 지배 plan/ 문서(specRefs) 부착 (design-first)

② Implement (N 에이전트, 병렬)
   각 단위가 *자기 targetFiles만* 작성. design-first(plan-search로 절 읽기) + sharedContracts 준수.
   · 컴파일 안 함 (gradle 락·동시쓰기 회피) — 시그니처 맞추기에만 집중
   · 파일 disjoint라 메인 트리 동시쓰기에도 충돌 없음

③ Integrate (1 에이전트, 단일 컨텍스트)
   메인 트리에 모인 변경을 ground-check(컴파일)로 봉합. 이음새(시그니처·import·모듈의존) 수정.
   · "하나의 선형 컨텍스트가 통합" = Cognition 정석. 컴파일 에러가 봉합의 1차 증거.

④ Review (적대자)
   통합 결과를 spec 대비 적대 검증(git diff + ground-check). 확신 있는 위반만 findings로.
```

## 핵심 신뢰성 결정

| 결정 | 이유 |
|------|------|
| **병렬은 쓰기만, 컴파일은 통합자가 한 번에** | 병렬 gradle은 데몬 락 충돌. 동시 컴파일 무의미. |
| **파일 disjoint 강제(분해 단계)** | 멀티에이전트 코딩의 핵심 실패모드(공유 파일 충돌)를 *원천 제거* |
| **단일 통합자 + 컴파일 게이트** | 이음새 불일치를 외부신호(컴파일)로 잡아 봉합 — LLM 추측 아님 |
| **단위마다 specRefs + design-first** | 대규모에서 드리프트(설계 이탈) 방지 — 하네스의 컨텍스트 엔지니어링 |

## 호출

```js
Workflow({ name: 'orchestrate-feature', args: {
  task: "알림 채널에 Webhook(범용) 채널 추가 — 엔티티·리포·Notifier·설정·컨트롤러·프론트 타입까지",
  docs: ["plan/ARCHITECTURE.md", "plan/PACKAGE_STRUCTURE.md"],   // 선택: 참고 설계문서 힌트
} })
```
반환: `{ units, sharedContracts, implementations[], integration{compiles,fixesApplied}, review{findings,verdict} }`.

## 언제 쓰고, 언제 쓰지 마라

- **써라**: 여러 파일/모듈에 걸친 큰 기능, *독립적으로 쪼개지는* 작업.
- **쓰지 마라**: 단일 코더로 충분한 작은 변경(오버헤드만 큼), 또는 단위들이 *공유 결정/같은 파일*에 강하게 얽혀 분해가 안 되는 작업(이건 단일 선형 코더가 맞음).

## 한계 / 다음 업그레이드

- 현재 병렬 코더는 **메인 트리 직접 쓰기**(파일 disjoint 가정에 의존). 분해자가 disjoint를 잘못 판정하면 클로버 위험 — 통합자 컴파일 게이트가 사후에 잡지만, *사전* 격리는 아님.
- **v2(더 강한 격리)**: 병렬 코더를 worktree 격리로 돌리고, 각자 `git diff`를 구조화 출력으로 반환 → 통합자가 `git apply`로 메인에 병합. 충돌을 사전 격리하나 diff 전달 비용·복잡도↑. 신뢰성 더 필요할 때.
- 검증 단계(④)는 단일 적대자. 더 엄격히 하려면 채널 B 다수결 패널([verify-findings-grounded](../../scripts/harness/verify-findings-grounded.workflow.js))로 교체 가능.
