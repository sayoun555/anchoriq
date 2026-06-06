# DETECT — refute-bias가 진짜 위반을 놓치나 (detection 축)

> H4b가 가리킨 핵심: 6실험 내내 *specificity(bogus 거르기)*만 쟀고 — refute-bias가 그걸 거의 공짜로 이긴다 — 정작 **detection(진짜 미묘 위반 catch)** 은 안 봤다. refute-bias가 *진짜* 위반도 반사 refute하면 missed-detection.
> 출처 맥락: Zheng NeurIPS'23(judge bias) + Lightman/Reflexion(검증엔 올바른 신호 필요).

## Run 1 — 2026-06-06

설계: **컴파일 멀쩡한 진짜 DDD 위반을 심음** — `_EvalLeakyService.classifyRisk()`가 리스크 점수 계산+등급 임계값(도메인 정책)을 @Service(Application 레이어)에 inline(규칙 4 위반). + 통제(bogus): `NotificationApplicationServiceImpl.updateSettings()`(실제론 오케스트레이션만).
원본: `detect-grounded-verdicts.json` · `detect-intrinsic-verdicts.json`. (측정 후 fixture 삭제, 빌드 복구.)

| 패널 | detect-1 (real 위반) | detect-2 (bogus 통제) | missed-detection | false-confirm |
|------|----------------------|------------------------|------------------|---------------|
| grounded | **confirm ✓ (3/3)** | refute ✓ (3/3) | **0** | 0 |
| intrinsic | **confirm ✓** | refute ✓ | **0** | 0 |

## 결론

1. **refute-bias가 missed-detection을 *안* 일으켰다.** 패널이 진짜 위반(Application의 도메인 정책)을 **잡고**(confirm), 가짜 주장은 **거름**(refute) — 반사적 refute가 아니라 **판별**한다. H4b의 우려는 *이 명확한 위반*에선 미실현.
2. **grounded가 처음으로 detection에서 실질 값**: execution 렌즈가 **실제 코드베이스에서 `RiskLevel.fromScore()`(도메인에 정석 배치된 동일 로직)를 찾아내** "이 로직의 집은 도메인임을 코드베이스가 스스로 입증"이라 교차검증. intrinsic은 그 precedent 없이도 잡음 → grounding은 *더 강한 근거*, 같은 verdict.
3. **하네스에 좋은 소식**: 검증 패널이 **specificity뿐 아니라 detection도** 한다(적어도 명확한 위반은).

## 한계 / 다음

- 이건 **명확한** 위반(도메인 정책이 대놓고 @Service에)이라 잡혔다. **refute-bias 리스크는 *진짜 borderline*(위반 여부가 전문가도 갈리는) 케이스에서만** 날 것 — 근데 그건 GT 자체가 모호해 만들기 어렵다(모호하면 정답도 모호).
- → detection 실패를 보려면 "명확히 위반인데 *발견이 어려운*"(여러 파일 걸침, 간접 호출 체인 등) 케이스가 필요. 그건 멀티파일 셋업 = 고도화.

## 한 줄

> **detection도 통과 — 패널은 진짜 위반을 잡고 가짜를 거른다(판별).** refute-bias가 명확한 위반을 가리진 않았고, grounding은 detection에서 "코드베이스 precedent 교차검증"이라는 실질 근거를 더했다. 7번째 실험도 "패널은 competent" — 차이는 여전히 *정확도가 아니라 근거/감사*.
