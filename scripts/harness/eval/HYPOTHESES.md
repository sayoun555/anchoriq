# 가설 카탈로그 — 논문 16편에서 뽑은 테스트 가능 명제

> H1은 하나일 뿐. `docs/papers/`의 각 논문은 **우리 하네스에서 측정 가능한 별개 가설**을 준다.
> 각 항목: 출처 → 논문 주장 → 우리 가설 → 우리 하네스에서 테스트하는 법 → 비용/상태.

## ★ 측정 결과 요약 (2026-06-06, 11실험) — 상세: [RESULTS.md](RESULTS.md)

| 가설 | 측정 결과 | 상태 |
|------|----------|------|
| H1 도구 grounding vs 읽기 | 정확도 차 0 (둘 다 4/4). 이득=추적가능성 | 미지지 |
| H2 렌즈 1 vs 3 | 렌즈 불일치 0% → 다수결 무의미 | 미지지 |
| H3 grounding 제거(컴파일에러) | intrinsic도 Lombok 추론으로 잡음 | 미지지 |
| H4 framing bias | 0 flip, 단정 framing을 "과장"이라 반박 | 미지지 |
| H4b 코드접근 제거(blind) | blind도 강건 → 원천=refute-bias+prior(코드reading 아님), 코드는 confidence만 | 인과 수정 |
| v3 runtime 산술 | 20스텝도 분해 암산으로 정확 | 미지지 |
| DETECT 진짜 위반 탐지 | 진짜 DDD 위반 잡음(missed 0) — 판별함 | 양호 |
| H8 라이브러리 edge | Java split도 모델이 앎 (+fixture leak 오염 보정) | 미지지 |
| H5 궤적(diff) | diff도 정확 추론. 부수: PR리뷰 가능 | 미지지 |
| H11 루브릭 vs 자유서술 | verdict 동일. 단 '규칙 인용'이 비존재 규칙 적발 | 미지지(verdict) |
| **v-next 런타임/환경 경계** ★ | **grounded 100% vs intrinsic 우연 — 첫 정확도 신호** | **지지(경계 실증)** |
| H6·H7·H10 | 미측정 (천장 예상) | ⬜ |

**Capstone (검증완료, 12실험)**: 코드 검증에서 capable LLM 패널은 **추론으로 풀 수 있는 모든 것**(결정론 코드·표준 라이브러리·DDD 위반·diff)에 **극도로 강건**하다. 11실험 verdict 정확도 차 = **0**. grounding/구조의 *측정된* 값은 **정확도가 아니라 감사가능성·confidence**다(신호: confidence H4b·비존재규칙 H11).

**경계 실증(v-next, 첫 positive)**: 모델이 *추론으로 못 푸는* 것 — **환경의존(실제 DB 행 상태)** — 에선 grounded **100%**, intrinsic **우연(50%)**. 글자까지 같고 user_id만 다른 쌍둥이 finding으로 통제 → intrinsic은 구분 단서 0(정직하게 "모른다"하나 refute-bias=우연). **grounding의 정확도 값은 *추론으로 못 푸는 곳에만* 존재하고, 거기선 유일한 정답 경로다.** ([results/VNEXT.md](results/VNEXT.md))

> 경계가 닫혔다: 11 negative(추론 가능 영역) + 1 positive(환경의존 영역) = grounding의 정확도 효용 위치를 **양방향으로 확정**.

---

## 검증 신뢰성 계열

### H1 — 도구 grounding vs 순수 읽기  ✅ *측정함(Run 1, 미지지)*
- 출처: CRITIC ICLR'24(도구 없으면 자가검증 악화)
- 가설: 검증자에 도구를 주면 false-confirm↓
- 테스트: `verify-findings` vs `verify-findings-grounded`, 같은 데이터셋
- 결과: 이 난이도에선 정확도 차 없음(둘 다 4/4). confound·추적가능성 발견 → v2 필요. [RESULTS.md](RESULTS.md)

### H2 — 렌즈 다양성/다수결 수
- 출처: Wang ICLR'23 self-consistency(다수 샘플 다수결 > greedy)
- 가설: 3렌즈 > 1렌즈, 5렌즈 ≥ 3렌즈 (탐지율·false-confirm)
- 테스트: 워크플로우 LENSES 배열을 1/3/5로 ablation, 같은 데이터셋
- 비용: 싸다(렌즈 수만 바꿈). **다음 후보**

### H3 — 자기검증 없는 grounding 제거 시 악화
- 출처: Huang ICLR'24 · Kamoi TACL'24(외부 신호 없으면 자가수정 악화)
- 가설: PostToolUse 적대자에서 ground-check를 빼면(순수 self-critique) 판정 품질↓
- 테스트: review-protocol에서 §1.5(ground-check) on/off A/B
- 비용: 중. 적대자 변종 필요

### H4 — judge 편향 + 디바이어싱
- 출처: Zheng NeurIPS'23(position·verbosity·self-enhancement bias; swap·2회로 완화)
- 가설: (a) 패널이 finding 제시 순서/표현에 흔들린다 (b) **에이전트가 자기 코드를 더 관대히 판정**한다 (c) swap-order로 완화된다
- 테스트: 같은 finding을 순서/표현 바꿔 재제시 → 판정 일관성 측정; 코더=검증자 vs 분리 비교
- 비용: 중. **싼 디바이어싱(swap)부터**

## 검증 *대상* 계열 (무엇을 보나)

### H5 — 궤적 vs 최종상태 검증
- 출처: Agent-as-a-Judge Meta'24(전체 궤적 평가 → 인간일치 60~70%→90%)
- 가설: 검증자가 *diff+추론 과정*을 보면 최종 코드만 볼 때보다 탐지율↑
- 테스트: 입력을 "최종 파일" vs "diff+커밋메시지"로 바꿔 A/B
- 비용: 중. 입력 변경

### H6 — process(단계) vs outcome(종료) 검증
- 출처: Lightman ICLR'24(process supervision > outcome, 오류 위치 정밀)
- 가설: PostToolUse(파일마다) 검증이 Stop(턴 끝) 일괄 검증보다 위반을 더/빨리 잡는다
- 테스트: 같은 작업을 PostToolUse-on vs Stop-only로, 탐지 시점·누락 비교
- 비용: 중

## 수정/루프 계열

### H7 — Reflexion 메모리
- 출처: Reflexion NeurIPS'23(언어적 반성을 episodic memory에 → 시도마다 개선)
- 가설: fix 반려 시 *왜 실패했는지*를 DLT에 적재해 다음 시도에 주입하면 성공률↑
- 테스트: `finding.sh`에 reflection 필드 추가, 반려→재시도 루프에서 with/without 비교
- 비용: 중. 로드맵 ④

### H8 — 테스트 생성 > 정적 리뷰
- 출처: Chen ICLR'24(self-debug; "정답 코드보다 유용한 테스트 생성이 더 쉽다")
- 가설: 새 코드에 대해 *테스트를 자동 생성·실행*하는 게 읽기 리뷰보다 버그를 더 잡는다
- 테스트: 적대자가 ground-check `--test` 대신 *테스트 생성 후 실행* 하는 변종 vs 정적 리뷰
- 비용: 높음. 새 능력

### H9 — bounded vs unbounded + reward-hacking
- 출처: Practical Limits 2605(reward hacking: assertion 약화·테스트 삭제) · Self-Healing 2606(bounded > retry-only)
- 가설: 자율 self-repair 루프는 (a) bound 없으면 발산 (b) **게이트를 속이려 테스트를 약화**한다
- 테스트: 로드맵 ⑥ 루프 구현 후, 종료 전후 테스트 수·assertion 강도 변화 감시
- 비용: 높음. 로드맵 ⑥·⑦

### H10 — process-monitor(루프/낭비 감지)
- 출처: SWE-PRM IBM'25(추론시 PRM이 redundant exploration·looping 교정 → SWE-bench +10.6pp)
- 가설: 코더가 같은 곳을 맴돌면 감지·개입하는 게 결과를 개선한다
- 테스트: 편집 시퀀스에서 반복 패턴 감지 훅 + 개입 on/off
- 비용: 높음. 로드맵 ③

### H11 — 실행 vs 루브릭(execution-free) 검증
- 출처: Agentic Rubrics 2601(execution-free 체크리스트 검증 +3.5pp)
- 가설: 구조화된 루브릭(체크리스트) 검증이 자유서술 검증보다 일관적
- 테스트: 적대자 프롬프트를 자유서술 vs 고정 루브릭으로 A/B
- 비용: 싸다(프롬프트만)

---

## 우선순위 (비용 대비 신호)

| 순위 | 가설 | 왜 |
|------|------|-----|
| 1 | **H2(렌즈 수)** · **H11(루브릭)** · **H4-swap** | 싸다(설정/프롬프트만), 즉시 측정 |
| 2 | **H1 v2**(더 어렵게+도구제한) · **H3**(grounding ablation) | confound 해결, H1 신호 확보 |
| 3 | H5·H6(검증 대상/시점) | 중간 |
| 4 | H7·H8·H9·H10 | 새 능력 필요, 임팩트 큼 |

## 메타

데이터(논문 16편)의 진짜 값 = **이 11개 가설.** H1 하나가 아니라, 각 논문이 "우리 testbed에서 측정 가능한 질문"을 하나씩 줬다. eval 프레임워크가 있으니 이제 **하나씩 반증/지지**해 나가면 된다.
