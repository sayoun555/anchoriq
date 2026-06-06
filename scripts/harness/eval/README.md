# 하네스 eval — 검증을 *측정*한다

> 하네스 엔지니어링을 일화(anecdote)에서 **증거**로 바꾸는 측정 장치.
> v1은 **H1: 도구 grounding 검증 > 순수 읽기 검증** 을 측정한다.

## 왜

지금까지의 주장("grounded 패널이 더 낫다")은 전부 n=1 관찰이다. eval은 라벨된 데이터셋으로 패널을 돌려 **수치로** 비교한다.

## H1 (이번에 측정)

> 검증자에게 **도구(컴파일·grep·검색)** 를 주면, *그럴듯하지만 거짓인* 위반 주장을 **순수 읽기보다 덜 hallucinate**한다(= false-confirm↓).

근거 문헌: CRITIC ICLR'24(도구 없으면 자가검증 악화) · Agent-as-a-Judge Meta'24.

## 데이터셋 (`dataset.json`)

라벨된 finding 모음. 각 항목에 `groundTruth`:
- `"bogus"` = 거짓 주장 → 올바른 판정은 **refute**. confirm하면 **false-confirm(hallucination)**.
- `"real"` = 실제 위반 → 올바른 판정은 **confirm**. refute하면 **missed-detection**.

v1은 **그럴듯하지만 거짓인(bogus)** 4건으로 구성 — *이미 고쳐진 버그를 "아직 있다"고 주장*하는 식. 순수-읽기 에이전트는 spec·git history·prior에 끌려 잘못 confirm하기 쉽고, 도구 에이전트는 `grep conditionValue=0`·컴파일로 정확히 refute하는지 본다. ground truth는 우리가 검증한 깨끗한 코드라 신뢰 가능.

## 절차

```
# 1. 두 패널을 같은 데이터셋으로 실행 (메인 에이전트가 Workflow로)
Workflow(verify-findings.workflow.js,          args=dataset)  → read-verdicts.json
Workflow(verify-findings-grounded.workflow.js, args=dataset)  → grounded-verdicts.json

# 2. 채점
bash score.sh dataset.json results/read-verdicts.json     read
bash score.sh dataset.json results/grounded-verdicts.json grounded
```

## 지표

| 지표 | 의미 | 방향 |
|------|------|------|
| **false-confirm** | 없는 위반을 confirm(hallucination) | 낮을수록 ↑ **[H1 핵심]** |
| missed-detection | 진짜 위반을 놓침 | 낮을수록 ↑ |
| accuracy | ground truth 일치 | 높을수록 ↑ |
| grounding rate | 판정이 실제 도구 출력을 인용한 비율 | (grounded 패널) |
| cost | 토큰·에이전트·시간 | (workflow usage) |

## 한계 (정직)

- **v1은 specificity(거짓양성 저항)만 측정** — 데이터셋이 전부 bogus. detection(진짜 위반 탐지)은 라벨된 real 위반이 필요(v2: 모듈 내 fixture seed).
- **패널의 기본 bias = refute** → 쉬운 bogus는 둘 다 100% refute해 신호가 안 날 수 있다. 그땐 "이 난이도에선 차이 없음 → 더 어려운 케이스 필요"가 결과.
- n이 작다(4) → 경향만. 결론은 n 키운 뒤.

## 결과 기록

실행 결과는 `results/`에 verdicts + score 로그로 남긴다. 측정값은 [RESULTS.md](RESULTS.md)에 누적.
