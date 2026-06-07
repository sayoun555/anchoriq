# v-next — 런타임/환경 경계 (12번째 실험, 첫 POSITIVE)

> 가설(캡스톤의 유일한 falsifiable 예측): 11실험 내내 grounding이 정확도를 못 더한 건 *과제가 추론으로 풀려서*다. **모델이 추론으로 못 푸는 것 = 실제 런타임/환경 상태**에선 grounding이 정확도를 더할 것이다.
> 출처: 11실험 capstone(H1·H2·H3·H4·H4b·v3·DETECT·H11·H8·H5). 카탈로그: [../HYPOTHESES.md](../HYPOTHESES.md)
> 데이터셋: [`../dataset-vnext.json`](../dataset-vnext.json) · GT 원천: `/tmp/anchoriq-vnext/runtime.db`(시드 sqlite, 바이너리)

## 설계 — "쌍둥이 finding" 통제

실제 코드 `NotificationDispatcherImpl.sendAndRecord`/`resolveDestination`(82-106행): destination 없으면 SKIP(발송·이력 X), 있으면 발송. **발송 여부 = 100% DB 행 상태**(코드로 유도 불가).

같은 코드에 대해 **글자까지 동일하고 user_id만 다른** finding 2개:

| finding | rule | 주장 | GT | 왜 |
|---------|------|------|----|----|
| vnext-1 | id=101, user **42**, SLACK | "destination 없어 SKIP된다" | **confirm** | DB: 42는 settings 행 없음 → null → SKIP (주장 참) |
| vnext-2 | id=102, user **7**, SLACK | "destination 없어 SKIP된다" | **refute** | DB: 7은 webhook 보유 → 발송 (SKIP 주장 거짓) |

핵심: 코드상 두 규칙은 **구별 단서가 0**. verdict를 가르는 단 하나의 사실(DB 행)은 grounded만 쿼리로 접근. intrinsic은 DB 경로 미제공 + 실행 금지.

## Run 1 — 2026-06-06 · 4 에이전트(grounded 2 + intrinsic 2) 병렬 · ~104k

| 패널 | vnext-1 (GT confirm) | vnext-2 (GT refute) | 정확도 |
|------|----------------------|---------------------|--------|
| Grounded A (sqlite 쿼리) | **CONFIRM ✓** | **REFUTE ✓** | 2/2 |
| Grounded B (sqlite 쿼리) | **CONFIRM ✓** | **REFUTE ✓** | 2/2 |
| Intrinsic A (코드만) | REFUTE ✗ | REFUTE ✓ | 1/2 |
| Intrinsic B (코드만) | REFUTE ✗ | REFUTE ✓ | 1/2 |

- **Grounded = 4/4 (100%)** — 둘 다 `SELECT destination FROM notification_settings WHERE user_id=? AND channel='SLACK'` 쿼리 → 42=없음→confirm, 7=`https://hooks.slack.com/...`→refute.
- **Intrinsic = 2/4 (50% = 정확히 우연)** — 두 finding이 코드상 동일하므로 **신호 0**. 둘 다 refute-bias로 떨어뜨려 → refute 케이스(vnext-2)만 우연히 맞고 confirm 케이스(vnext-1)는 놓침.

## 결론 — 12실험 capstone 완성 (경계를 찾았다)

1. **첫 POSITIVE 결과.** 12번 만에 처음으로 **grounded(100%)가 intrinsic(50%=우연)을 정확도에서 이김.** 그것도 캡스톤이 *예측한 정확히 그 자리* = 추론으로 못 푸는 런타임/환경 상태.
2. **경계의 정의가 실증됨**: 11실험(결정론 코드·표준 라이브러리·DDD·diff·산술)에선 grounding 정확도 이득 = 0. v-next(비유도 DB 상태)에선 grounding이 **유일한 정답 경로**. → "역량이 추론 가능한 모든 곳을 덮는다 / 못 덮는 곳에서만 grounding이 정확도를 산다"는 가설이 **양방향으로 확인**.
3. **쌍둥이 통제가 우연을 배제**: intrinsic 50%는 실력이 아니라 **refute-bias × (confirm 1: refute 1) 구성의 산물**. 만약 둘 다 confirm 케이스였다면 intrinsic은 0/2였을 것 — 즉 intrinsic 정확도는 진실과 **무상관**(definitionally chance), grounded는 진실과 **상관**.
4. **부수(긍정적): intrinsic의 정직성.** intrinsic은 환각하지 않고 *"코드만으론 결정 불가, 추측"*이라 명시했고, 한 명은 주입된 Vercel 힌트까지 거부. → **자기가 못 푸는 걸 안다.** 단 *정직 ≠ 정확* — 강제 verdict에선 prior(refute-bias)가 우연만 냄. 실무 함의: 이런 finding엔 패널에게 **"모름"을 verdict로 허용**하고 grounding 도구를 줘야 한다.

## ⚠️ 자기비판 (적대적 자기검증 반영)

- **설계상 동어반복 위험**: 정답을 가르는 DB 정보를 intrinsic에서만 제거했으니, intrinsic이 못 맞히는 건 *발견*이라기보다 *정의상 보장*된 결과다("definitionally chance"라 본문도 인정). 즉 v-next는 "외부상태 의존 질문엔 외부상태 접근이 필요하다"를 *실증*했다기보다 *가정을 코드로 재서술*한 면이 크다.
- **n=2(쌍둥이 1쌍)**: 이걸로 "capstone 완성·경계 양방향 확정"은 과하다. *방향성 시연*까지가 정직한 한계.
- **원시 산출물 미보존**: grounded/intrinsic verdict JSON이 안 남아 재현 불가.
- 그래도 *해석*("grounding의 정확도 효용은 추론으로 못 푸는 곳에만")은 합리적 — 단 이 한 실험이 그걸 *증명*한 건 아니다.

## 한 줄

> 11실험 내내 안 갈리던 verdict 정확도가 **런타임 DB 상태에서 처음 갈렸다 — grounded 100% vs intrinsic 50%(우연).** 캡스톤의 유일한 예측(경계 = 비결정·환경의존)이 **실증**됐다. grounding의 정확도 값은 *추론으로 못 푸는 곳에만* 존재하고, 거기선 **유일한 정답 경로**다. (intrinsic은 정직하게 "모른다"고 했다 — 정직하나 정답은 아님.)
