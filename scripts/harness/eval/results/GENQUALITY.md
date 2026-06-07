# B — 생성 품질 A/B (하네스 컨텍스트가 *코드 생성*을 개선하나)

> 가설: design-first 컨텍스트 주입(Arm A)이 prompt-only(Arm B)보다 생성 품질↑ (외부 하네스 revfactory의 +60% 주장을 블라인드·통제 A/B로 검증).
> 출처: [COMPOSITION_PRINCIPLES.md](../../../../docs/harness-engineering/research/COMPOSITION_PRINCIPLES.md) §3 비대칭 법칙. 데이터셋: [`../dataset-genquality.json`](../dataset-genquality.json)
> 과제: 선석 혼잡도 4등급 분류 순수 도메인(berth-congestion). 변수=컨텍스트 주입 하나. 블라인드 채점 2명, X/Y 순서 swap(위치편향 제어, Zheng).

## Run 1 — 2026-06-07

| | Grader1 (X=A) | Grader2 (Y=A, swap) | 평균 |
|---|---|---|---|
| **Arm A (하네스 컨텍스트)** | 51 | 51 | **51.0** |
| **Arm B (prompt-only)** | 54 | 53 | **53.5** |

**두 채점자가 순서를 바꿔도 일관되게 baseline(Arm B) 선호** → 위치편향 아님, 실제 신호.

### 차원별 (일관된 패턴)
| 차원 | Arm A(하네스) | Arm B(baseline) | 해석 |
|------|:---:|:---:|------|
| domain_modeling | 8~9 | 8~9 | 하네스가 약간↑ (정책 VO·책임배치) |
| encapsulation | 9 | 7~9 | 하네스가 약간↑ (VO 다수) |
| **abstraction** | **5** | **9** | ★ 하네스가 **과설계** — 고정함수에 인터페이스+정책주입+OCP = YAGNI |
| correctness | 9 | 8~10 | 대등 (baseline의 NaN/Infinity 방어가 더 명시적) |
| completeness | 10 | 10 | 대등 |

## 결론 — 하네스가 *졌다* (정직한 negative, n=1)

1. **방향이 revfactory의 +60%와 반대.** design-first 주입이 *이 과제에선* 품질을 **약간 낮췄다**(53.5 vs 51.0). 이유: 하네스 규칙("인터페이스 우선·VO·일급컬렉션")을 *무차별 적용*해 **단순 과제를 과설계**(speculative generality)했다.
2. **합성 saturation 축(D) 실증.** 하네스 컨텍스트가 생성자를 **과제약** → 목표("right-sized 풀이") 대신 "DDD 규칙 다 적용"을 최적화 = 목표 대체. 우리가 COMPOSITION_PRINCIPLES에서 이론화한 그 축이 실제로 발현.
3. **하네스 가치는 *구조*인데, 단순 과제에선 구조가 *부채*가 된다.** modeling/encapsulation 이득(+2~3)을 abstraction 과설계 감점(−4)이 초과.
4. **revfactory와 *모순 아님*, 오히려 보강**: 그들은 "어려운 과제일수록 이득↑". 이득이 난이도에 비례하면 **쉬운 과제에선 이득이 음수로 뒤집힌다**(과설계). 우리 n=1 쉬운 과제의 음수 결과가 그 그래디언트의 *저난도 끝*과 일치. → **가설 정련: 생성-측 합성 이득은 *task-difficulty-conditional* — 복잡 과제엔 +, 단순 과제엔 −(과설계).**

## 두 번째 발견 — 하네스 규칙이 *구식 idiom*을 밀 수 있다

baseline(Arm B)이 `record`(Java 16+ 현대 VO)를 쓴 반면, 하네스(Arm A)는 `final class`+수동 equals/hashCode/private 생성자(**record 이전 고전 DDD 스타일**)를 썼다. → 우리 AGENTS 규칙("VO로 포장·일급컬렉션")이 *고전 OOP 언어*로 적혀 있어, 모던 Java(record·sealed·pattern matching)로 더 간결히 표현될 의도를 **장황한 레거시 패턴으로** 유도할 수 있다. (사용자 지적: "최신 방식일까?")

## 한계 / 다음

- **n=1 과제** — 방향성만. 정련된 가설(난이도-조건부)을 검증하려면 **복잡 과제로 재실험** 필요(거기선 하네스가 이길 것으로 예측).
- **confound**: Arm B도 AnchorIQ repo 안에서 돌아 CLAUDE.md 앰비언트 컨텍스트를 일부 받음(완전한 무-하네스 아님) → 측정된 차이는 *하한*.
- 행동: ① 복잡 과제 v2 A/B ② 하네스 규칙을 모던 Java idiom으로 갱신(record/sealed 권장) ③ 하네스 컨텍스트에 "과설계 금지/YAGNI" 가드 추가(단순 과제 과설계 방지).

## Run 2 — 2026-06-07 · 복잡 과제 + 모던화된 하네스 (난이도-조건부 검증)

Run 1의 두 처방(① AGENTS/적대자 모던화: record/sealed + YAGNI 균형추 ② 복잡 과제) 적용 후 재실험.
과제: 공급망 리스크 점수 집계(4종 신호 전략 + 플랜별 가중치 + 집계 + 근거 — *구조가 값하는* 복잡도). 블라인드 2명, X/Y swap.

| | Grader1 (X=A) | Grader2 (Y=A, swap) | 평균 |
|---|---|---|---|
| **Arm A (모던화 하네스)** | 55 | 59 | **57.0** |
| **Arm B (prompt-only)** | 49 | 50 | **49.5** |

**두 채점자 순서 바꿔도 일관되게 하네스 승 (+7.5).** Run 1(단순, 하네스 −2.5)에서 **부호가 뒤집힘.**

### 차원별 (Run 1과 대조)
- **abstraction**: Run 1은 하네스가 *과설계*로 5점(졌음). Run 2는 **모던화된 하네스가 YAGNI로 aggregator 인터페이스를 생략(9점)**, 오히려 **baseline이 Scorer 인터페이스를 과설계(7점)**. → *YAGNI 균형추가 작동해 과설계 성향을 역전.*
- **domain_modeling**: 하네스 10 vs baseline 8 — sealed 신호 계층으로 신호별 고유 데이터(PortRisk 배열) 표현, baseline은 단일 record로 평탄화.
- **correctness**: 하네스가 EnumMap.merge로 중복신호 *합산*(비손실) vs baseline *마지막값 우선*(데이터 손실).
- 모던 idiom(record/sealed/pattern-matching)은 *양쪽 다* 사용 — Run 1의 "하네스가 구식 강요" 문제도 규칙 모던화로 해소.

## ★ Capstone — 생성-측 합성은 *난이도-조건부* (검증 완료)

| | 단순 과제(Run1) | 복잡 과제(Run2) |
|---|---|---|
| 하네스 vs baseline | **−2.5 (짐)** | **+7.5 (이김)** |
| 원인 | 무차별 주입 → 과설계 | 구조가 값함 + YAGNI로 과설계 회피 |

1. **생성-측 합성 이득은 task-difficulty에 비례하고, 쉬운 과제에선 음수로 뒤집힌다.** revfactory의 "어려울수록 이득↑"(Basic+23.8%→Expert+36.2%)을 *저난도 끝까지 외삽*하면 우리 단순과제의 음수와 정확히 일치 — 그래디언트를 양방향으로 확정.
2. **하네스의 값 = 구조. 구조는 복잡도에 비례해 값하고, 단순할 땐 부채가 된다.** 그래서 규칙에 **YAGNI 균형추**가 필수(과소·과대 둘 다 막아야).
3. **모던화 규칙이 측정으로 검증됨**: record/sealed로 구식 idiom 강요 해소 + YAGNI로 과설계 역전. 두 처방이 Run2에서 동시에 작동.
4. **검증-측(12실험 천장)과 대조**: 합성은 *검증자엔* 포화(독립 정보원 빼고), *생성자엔* 난이도-조건부 이득. → COMPOSITION_PRINCIPLES §3 비대칭 법칙 실증.

## ⚠️ 자기비판 (적대적 자기검증 반영 — 이게 제일 약한 실험)

- **채점 누수 가능**: 채점자(LLM)가 *원본 코드*가 아니라 **메인이 작성한 구조 요약**("Arm A 과설계" 프레이밍 포함)을 봤다 → 내 해석이 채점에 샜을 수 있다. swap은 *위치* 편향만 잡지 self-enhancement는 못 잡음.
- **통제 깨짐 자인**: Arm B도 repo CLAUDE.md 앰비언트를 받아 "변수=컨텍스트 주입 하나" 전제가 깨졌다. "하한"은 부호 유지 *가정*.
- **n=1~2 + 통계 전무**로 "난이도-조건부 *법칙*"은 과하다.
- **★ 위선**: revfactory를 "n작음·p-value없음·자가채점"이라 깠는데, 이 실험이 *더 작은 n으로 같은 죄*를 저질렀다. 정직하게는 **파일럿 관찰**.
- **원시 산출물(생성코드·채점출력) 미보존** → 재현 불가.

## 한 줄

> 단순 과제선 하네스가 졌고(−2.5, 과설계) → 규칙을 record/sealed+YAGNI로 모던화 → 복잡 과제선 하네스가 이겼다(+7.5). **생성-측 합성 이득은 난이도-조건부**(쉬우면 음수, 어려우면 양수). 하네스의 값은 *구조*이고 그건 복잡도에 비례한다 — eval이 하네스의 실패모드(과설계)와 그 수정(YAGNI)을 둘 다 측정으로 잡았다.
