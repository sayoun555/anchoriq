# eval 결과 — 인덱스

> 가설별 측정 결과는 **각각 별도 파일**(`results/H?.md`). 이 파일은 한 줄 요약 인덱스.
> 가설 카탈로그: [HYPOTHESES.md](HYPOTHESES.md) · 프레임워크: [README.md](README.md)

> ## ⚠️ 신뢰도 한계 (정직하게 — 적대적 자기검증 결과 반영)
> 아래 결론들은 **n=1~2 파일럿**이다. "법칙·capstone·확정" 같은 표현은 *과하며*, 정확히는 **"가설과 부합하는 방향의 관찰"**까지가 한계다.
> - **통계 부재**: p-value·신뢰구간·반복·분산 없음.
> - **원시 산출물 미보존**: 결론을 떠받치는 **v-next·GENQUALITY**의 verdict JSON·생성코드·채점 출력이 안 남아 **재현 불가**. (약한 negative 실험들만 verdict JSON 보유.)
> - **단일 작성자**가 실험·채점·서술 일원화 → 자기참조 편향 가능(특히 GENQUALITY 채점이 메인의 *요약*을 봄).
> - **자기모순 인정**: [EXTERNAL_HARNESS_COMPARISON](../../../docs/harness-engineering/research/EXTERNAL_HARNESS_COMPARISON.md)에서 revfactory를 "n작음·통계없음·자가측정"이라 비판했는데, **우리 GENQUALITY가 더 작은 n으로 같은 죄**를 저질렀다. "우리 eval이 더 엄밀"은 통제 설계(블라인드·대조군)엔 맞지만 *표본·재현성*엔 성립 안 한다.
> - **천장 효과**: "11실험 negative"의 다수는 *동등성 증명*이 아니라 **검정력 부족(과제가 너무 쉬워 양쪽 천장)**. "차 0"은 "같다"의 증거가 아니다.

| 가설 | 결과 파일 | 한 줄 결과 | 상태 |
|------|----------|-----------|------|
| **H1** 도구 grounding vs 읽기 | [results/H1.md](results/H1.md) | 정확도 차 없음(둘 다 4/4). 이득=추적가능성, 비용 +24% 시간. confound(read도 grep 씀) 발견 → v2 필요 | 미지지 |
| **H2** 렌즈 다양성/다수결 | [results/H2.md](results/H2.md) | 렌즈 불일치 0%(borderline까지 만장일치) → 다수결 무의미. 단 *근거*는 다양(evidence diversity) | 미지지 |
| **H3** grounding 제거 시 악화 | [results/H3.md](results/H3.md) | intrinsic(컴파일 안 함)이 grounded와 동일 2/2. 에러가 모델 prior 안이라 추론으로 충분. grounding 값=감사증거+비용 3x | 미지지 |
| **H4** framing bias | [results/H4.md](results/H4.md) | framing bias 미검출(0 flip). 패널이 단정 framing을 "과장"이라 반박 | 미지지 |
| **H4b** 코드 접근 제거(blind) | [results/H4b.md](results/H4b.md) | blind도 0 false-confirm — 강건성 원천=refute-bias+설계prior+단정회의(코드reading 아님). 코드는 confidence만 high→low. *사각지대 발견: detection 미측정* | 인과 수정 |
| **v3** runtime 계산(실행 vs 암산) | [results/V3.md](results/V3.md) | 20스텝 계산도 intrinsic이 *분해 암산*으로 정확. 천장 못 깸 — 역량 경계가 예상보다 멀다 | 미지지 |
| **DETECT** 진짜 위반 탐지(refute-bias 리스크) | [results/DETECT.md](results/DETECT.md) | 진짜 DDD 위반(Application의 도메인 로직) **잡음**(2/2, missed 0). refute-bias가 명확한 위반은 안 가림 — 패널이 *판별*. grounded는 코드베이스 precedent 교차검증 | 양호(미지지 아님) |
| **H11** 루브릭 vs 자유서술 | [results/H11.md](results/H11.md) | verdict 동일(4건 refute). 단 '규칙 문구 인용' 단계가 *존재하지 않는 규칙 든 finding(h2-5)* 적발 — 값=감사가능+근거강제 | 미지지(verdict) |
| **H8** 실행/테스트 vs 정적(라이브러리 edge) | [results/H8.md](results/H8.md) | Java split trailing-empty도 모델이 앎 → 둘 다 refute. 천장. + fixture 주석이 정답 leak한 오염 잡아 보정 | 미지지 |
| **H5** 궤적(diff) vs 최종상태 | [results/H5.md](results/H5.md) | diff도 정확 추론(현재상태 anchoring 없음). 둘 다 정답. 부수: 패널이 PR/diff 리뷰에도 쓸 수 있음 | 미지지 |
| **v-next** 런타임/환경 경계 ★ | [results/VNEXT.md](results/VNEXT.md) | **첫 POSITIVE.** 쌍둥이 finding(user_id만 다름)에서 grounded **2/2(100%)** vs intrinsic **2/4(우연)**. DB 상태가 verdict를 가르자 처음으로 정확도가 갈림 — 캡스톤 예측 실증 | **지지(경계)** |
| **B 생성품질** A/B ★ | [results/GENQUALITY.md](results/GENQUALITY.md) | *검증*이 아니라 *생성* 측정(사각지대). 단순과제 하네스 **−2.5(과설계)** → 규칙 모던화(record/sealed+YAGNI) → 복잡과제 **+7.5(승)**. **생성 합성=난이도-조건부** | **조건부 지지** |
| **COMPLIANCE** 설계 준수율(주입 vs 앰비언트) | [results/COMPLIANCE.md](results/COMPLIANCE.md) | 추론 가능 DDD 규칙: 타깃 주입·앰비언트 **둘 다 100%(null)**. 앰비언트(CLAUDE.md+기존코드)가 이미 충분 → 주입 redundant | 미지지 |
| **BOUNDARY** 비추론 임의계약(주입 vs 앰비언트) ★ | [results/BOUNDARY.md](results/BOUNDARY.md) | 의도적 비추론(Kafka 파티션·그룹명·DLT) 골라도 **둘 다 5/5(null)** — 그 값이 *기존 코드에 정적 존재*해 앰비언트가 회수. **"정적 소스에 있으면 redundant" 경계 확정** | 미지지(경계강화) |
| **DRIFT** "문서 무시 시 하네스 강세?" PreToolUse vs PostToolUse ★★ | [results/DRIFT.md](results/DRIFT.md) | 사용자 가설 검증. 생성-측 드리프트 0(Opus·Haiku 모두 준수, 리마인더 무관). **그러나 PostToolUse 적대자가 Haiku의 self-invocation 트랜잭션 버그를 블라인드 적발(힌트제거 후에도 #1)** — 리마인더가 못 막은 걸 적대자가 잡음 | **지지(적대자=양수, 주입=0)** |
| **ADVERSARY** 적대자 recall/오탐률 (판별 쌍 8) ★★ | [results/ADVERSARY.md](results/ADVERSARY.md) | 검증 절반의 신뢰도 측정. **recall 4/4**(tell 없는 footgun S2·추론형 과설계 S3 포함), **하드 오탐 0**(YAGNI 무인터페이스·record VO 안 물림), 의도 축 판별 4/4. eval 전체 *최강 positive*. (C1 샘플오염·n=8·의미판단단독 하한) | **지지(검증 절반 신뢰도 높음)** |
| H6·H7·H10 | (예정·천장 예상) | [HYPOTHESES.md](HYPOTHESES.md) | ⬜ |

## ★ 메타 결론 (12실험) — 경계를 찾았다

> **11실험 = 일관된 negative**(grounding 정확도 이득 0) **+ v-next = 첫 positive**(grounding이 유일한 정답 경로). 두 결과가 합쳐져 **경계를 양방향으로 확정**한다.

- **추론으로 풀리는 모든 것**(결정론 코드·표준 라이브러리·DDD 위반·diff·20스텝 산술) → 패널은 도구·렌즈수·grounding·framing·코드접근 어느 축을 흔들어도 정확. grounding의 값 = **auditability/confidence**(정확도 아님).
- **추론으로 못 푸는 것**(실제 DB/런타임 상태) → grounded 100% vs intrinsic 우연. grounding이 **유일한 정답 경로.** intrinsic은 정직하게 "모른다"고 하나 정답은 못 냄(refute-bias = 우연).
- **실무 함의 (확정)**: 검증 패널에 grounding 도구는 *모든 finding의 정확도용*이 아니라 **(a) 감사/신뢰**(추론 가능한 것) **+ (b) 환경의존 finding의 정확도용**(런타임 상태 필요한 것)으로 배치. 그리고 환경의존 finding엔 **"모름" verdict를 허용**해 refute-bias의 우연 정답을 막아야.

## 메타 결론 (7실험, 이전 — 보존) — capstone

0. **H4b·DETECT 추가 핵심**: (a) 패널 강건성의 원천은 *코드 reading*이 아니라 **refute-bias + 설계 prior + 단정-회의**(blind도 강건). 코드 reading은 **confidence/감사**만 더함. (b) refute-bias가 **detection을 안 망쳤다** — 진짜 DDD 위반을 잡고 가짜를 거름(판별). 하네스가 specificity뿐 아니라 *detection도* 함(명확한 위반은).
1. **H1·H2·H3·H4·v3 전부 "미지지"** — 정확도/판정 차이 0. 일관된 negative result.
2. **공통 원인 = 모델이 *결정론적·readable 코드*를 매우 신뢰성 있게 추론한다**: accuracy·lens수·grounding·framing·심지어 20스텝 계산(v3) 어느 축을 흔들어도, **reading/추론이 verdict를 지배**. v3에선 noise를 깔았는데도 모델이 *분해 지름길*로 정확히 풀었다 — **역량 경계가 예상보다 훨씬 멀다.**
3. **측정된 grounding 이득 = auditability(정확도 아님)**: 정확도는 같아도 grounded는 **재현 가능한 증거**(실제 jshell 실행+Python 교차검증, javac 재현)를 남김. 종종 **비용 초과**(H3 토큰 3배).
4. **최종 가설(날카로워짐)**: **grounding의 *정확도* 이득은 결정론적 코드엔 없고, "모델이 추론으로 못 푸는" 진짜 경계 = 비결정성(동시성/타이밍/난수)·환경의존(DB/네트워크/라이브러리 버전)·진짜 난해 계산에서만 난다.** 이건 단위 fixture로 못 만들고 **실행 환경**이 필요 → "고도화 테스트 나중에 한번에"의 본질 과제(v-next).
5. **실무 함의**: 검증 패널엔 grounding을 *정확도용*이 아니라 **감사/신뢰용**으로 쓰고, *정확도*가 필요한 곳은 **비결정·환경 의존 동작**(테스트 실행이 진짜 필요한)에 집중.

## 비용 누적 (참고)

| run | 에이전트 | 토큰 | 시간 |
|-----|---------|------|------|
| H1 read | 12 | 297.7k | 59s |
| H1 grounded | 12 | 311.0k | 73s |
| H2 grounded | 15 | 393.4k | 103s |
| H3 grounded | 6 | 150.0k | 84s |
| H3 intrinsic | 2 | 46.7k | 35s |
| H4 intrinsic | 8 | 191.5k | 57s |
| v3 grounded | 3 | 65.0k | 31s |
| v3 intrinsic | 1 | 20.9k | 19s |
| H4b blind | 8 | 167.1k | 27s |
| DETECT grounded | 6 | 156.3k | 60s |
| DETECT intrinsic | 2 | 47.3k | 29s |
| H11 rubric | 4 | 126.6k | 90s |
| (H11 rubric 1차 실패) | 4 | 0 (StructuredOutput 누락) | 306s |
| H8 grounded | 3 | 65.6k | 35s |
| H8 intrinsic (×2, 오염 보정) | 2 | 41.6k | 38s |
| H5 intrinsic | 2 | 46.3k | 28s |
