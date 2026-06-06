# H4b — 코드 접근 제거 (blind vs read): 강건성의 *원천* 규명

> capstone의 인과 가설("패널이 강건한 건 코드를 *읽어서*")을 직접 검증. H4와 같은 데이터셋(framing × bogus 8건)을 **코드 접근 전면 차단**(Read·grep·실행 금지, 주장 텍스트만)으로 재실행.
> 출처: Zheng NeurIPS'23(judge bias는 순수-judge 조건에서 나타남). 워크플로우: `verify-findings-blind.workflow.js`.

## Run 1 — 2026-06-06

| 조건 | false-confirm | framing flips | confidence 분포 |
|------|---------------|---------------|-----------------|
| **read** (H4, intrinsic) | 0/4 | 0/4 | **7 high · 1 medium** |
| **blind** (코드 차단) | 0/4 | 0/4 | **0 high · 4 medium · 4 low** |

- blind agents는 지시를 지켜 **파일을 안 읽음**(tool_uses 8 = 판정 호출만; reason 전부 "코드를 볼 수 없어 추정").
- 판정(verdict)은 read와 **완전 동일**(8건 다 refute). **confidence만 high→low/medium로 폭락.**

## 결론 — capstone 인과 가설 *수정됨*

1. **예측 빗나감(정보적)**: 코드 접근을 빼도 패널은 **여전히 0 false-confirm.** "강건성=코드 reading"은 **부분적으로 틀렸다.**
2. **진짜 원천 = 3중 중복결정(overdetermined)**:
   - **refute-default bias** ("확신 없으면 refute") — 입증책임을 finding에 지움.
   - **설계 prior** ("core domain model이고 프로젝트가 캡슐화 강제하니 아마 괜찮다").
   - **assertion-skepticism** — *"'명백·확실·반드시'라는 절대적 확신 표현이 과장되어 있어 거짓양성으로 본다."* 단정 framing을 *역으로* false-positive 신호로 씀.
   → 코드 reading은 이 셋 중 *하나*일 뿐. 빼도 나머지 둘이 verdict를 떠받친다.
3. **코드 reading이 *실제로* 산 것 = confidence + auditability**: 판정 정확도가 아니라 **확신도**(high vs low)와 **재현 가능한 근거**. 즉 grounding의 값은 일관되게 "정확도 아닌 신뢰/감사."

## ★ 우리 연구의 사각지대 발견 (자기비판)

6실험 내내 우리는 **specificity(bogus를 refute)만** 측정했다 — 그런데 **refute-default bias가 이걸 거의 공짜로 이긴다.** 정작 **detection(진짜 *미묘한* 위반을 catch)** 은 거의 안 봤다(H3 컴파일에러 1건뿐, 그것도 쉬웠음).

> **refute-bias는 specificity엔 유리하지만 detection엔 *독*이다** — 진짜 위반도 반사적으로 refute하면 missed-detection. 우리가 안 본 그곳이 **하네스의 진짜 리스크.**

## 다음 (진짜 날카로운 실험)

지금까지 = "bogus를 잘 거른다"(쉬움, refute-bias 덕). **미답 = "진짜 미묘한 위반을 놓치나"**:
- 컴파일·실행으로 안 드러나는 **설계 위반**(Application에 도메인 로직 누수, Aggregate 경계 침범 등)을 *진짜로* 심고(GT=real), 패널이 refute-bias 때문에 놓치는지(missed-detection) 측정. ← 이게 detection 축의 결정적 실험.

## 한 줄

> blind도 강건했다 — 강건성의 원천은 코드 reading이 아니라 **refute-bias + 설계 prior + 단정-회의**였다(코드 reading은 confidence/감사만 더함). 그리고 이게 드러낸 진짜 문제: **우리는 6번 내내 쉬운 specificity만 쟀고, refute-bias가 해칠 detection은 안 쟀다.** 다음은 거기다.
