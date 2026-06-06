# 리서치: 하네스 엔지니어링 + 멀티에이전트 엔지니어링

> 딥리서치 하네스(fan-out 검색 → 출처 fetch → 클레임당 3표 적대 검증 → 합성)로 생성. 2026-06-05.
> **통계**: 5각도 · 21개 출처 fetch(20개 1차) · 99개 클레임 추출 → 25개 검증 → **23 confirmed / 2 killed**.
> 출처 우선순위: arXiv peer-reviewed/프리프린트 + AI 기업 공식 엔지니어링 자료. 신뢰 낮은 블로그는 표시.

## 결론 (TL;DR)

"**단일 하네스(가드레일) + 멀티에이전트(검증)를 결합해야 production-grade가 된다**"는 명제는 1차 증거로 입증된다. **단 결정적 조건**: 멀티에이전트는 *검증·병렬 탐색* 같은 고가치·병렬화 가능 작업에만, **코딩 자체엔 쓰지 않는다**(Anthropic·Cognition 합의). 기반은 *단일 선형 에이전트 + 외부 grounding 가드레일*이어야 한다.

결정적 출처 — Anthropic, *Effective harnesses for long-running agents*: "**frontier 코딩 모델(Opus 4.5)조차** Agent SDK로 여러 컨텍스트 윈도우를 루프 돌려도, high-level 프롬프트만으론 production-quality 웹앱을 **못 만든다.**" 자사 flagship의 한계를 공개(마케팅 역방향) → 신뢰도 높음.

---

## 1. 단일 에이전트 / 컨텍스트 — "긴 문서 통째 주입"은 정량적으로 나쁘다

| 발견 | 출처 | 검증 |
|------|------|------|
| **Lost in the middle**: U자 곡선. 내용 불변, **위치만 옮겨도** 멀티홉 QA 20~30점 하락 | Liu et al., TACL 2024 — `arXiv:2307.03172` | 3-0 |
| **입력 길이 자체가 해롭다**: 100% 완벽 검색 + 무관 토큰 마스킹해도 길이만 늘면 **13.9~85% 저하** | Amazon, EMNLP 2025 Findings — `arXiv:2510.05381` | 3-0 |
| long-context 전용 모델조차 **면제 안 됨**(Claude는 가장 느리게 저하되나 면제 X) | `2307.03172` + Found in the Middle `2406.16008` | 3-0 |
| 원인: 트랜스포머 n² attention, 유한한 **"attention budget"** → 컨텍스트는 "한계수익 체감하는 유한 자원" | Anthropic, *Effective context engineering* | 3-0 |
| Anthropic의 context engineering 정의 = "추론 중 최적 토큰 집합을 큐레이션·유지하는 전략" + context rot 명시 인정 | 〃 | 3-0 |

**함의**: `plan/` 21개 문서를 통째로 컨텍스트에 넣는 것은 suboptimal. → 본 리포 `scripts/harness/plan-search.sh`(섹션 추출/검색)가 대응.

**완화 기법 — 반전**: 학술 보정법 `found-in-the-middle`(`2406.16008`)은 **white-box(attention 가중치 접근 필요)** → **Claude/GPT API에선 적용 불가**. 따라서 실무에선 **black-box 기법(검색·재배치·계층요약·청크 순서)만** 가능 → 본 리포의 섹션 추출 방식이 올바른 선택.

## 2. 자기검증/리플렉션 — 효과 있으나 "외부 신호"가 전제

| 발견 | 출처 | 검증 |
|------|------|------|
| Reflexion: HumanEval pass@1 **80% → 91%**(가중치 업데이트 없이, 언어적 피드백을 episodic memory에) | NeurIPS 2023 — `arXiv:2303.11366` | 3-0 |
| 리플렉션이 풍부할수록 이득↑: Retry +4.1% vs Solution +13.9% vs Composite **+14.6%** | Renze & Guven — `arXiv:2405.06682` | 3-0 |

**⚠️ caveat(검증이 짚은 것)**: 위 향상은 **순수 self-critique가 아니라 외부 grounding**(단위 테스트 실행, 정답 정보 간접 누출)에 의존. *"self-reflection이 9개 모델에서 통계적으로 유의하게 개선"*이라는 강한 일반화는 **검증에서 죽음(1-2 refute).**

→ **교훈: LLM이 LLM을 평가하는 self-critique는 약하다. 외부 검증 신호(테스트/컴파일/스키마/툴 결과)가 핵심이다.**

## 3. 멀티에이전트 — 강력하나 경계가 분명

| 발견 | 출처 | 검증 |
|------|------|------|
| orchestrator-workers: 하위작업을 예측 못 하는 복잡 작업에 적합 | Anthropic, *Building effective agents* | 3-0 |
| breadth-first 리서치에서 멀티 > 단일 **90.2%** | Anthropic, *Multi-agent research system* | **2-1** |
| …단 **자가보고·비재현 내부 eval**, **~15배 토큰**, BrowseComp 분산의 **80%가 토큰량으로 설명** | 〃 | 3-0 |
| **공유 컨텍스트/상호의존 많은 도메인(=대부분의 코딩)엔 부적합** | Anthropic + Cognition *Don't Build Multi-Agents* | 3-0 |
| 멀티에이전트 14개 실패모드 카탈로그, **hand-off 시 컨텍스트 손실** 지배적(1,600+ 트레이스) | MAST, Cemri 2025 — `arXiv:2503.13657` | 3-0 |
| 검증 패턴: self-consistency 다수결(`arXiv:2203.11171`), multi-agent debate(`arXiv:2305.14325`) | — | 3-0 |
| 단순함이 기본: 정당화될 때만 복잡성 추가, "에이전트를 안 만드는 것"이 답일 수도 | Anthropic, *Building effective agents* | 3-0 |
| agent-specific 실패 모드 = compounding errors(오류 누적) → 가드레일·샌드박스 테스트 필수 | 〃 | 3-0 |

→ **90.2% 우위는 "구조" 덕이 아니라 상당 부분 "토큰 더 씀" 덕**이고, Anthropic·Cognition **둘 다** "코딩엔 멀티에이전트 쓰지 마라"에 동의.

## 4. 결합이 production-grade의 조건 — 결정적 증거

| 발견 | 출처 | 검증 |
|------|------|------|
| Opus 4.5조차 프롬프트만으론 production 웹앱 실패 → 엔지니어링된 하네스(initializer + coding agent) 필요 | Anthropic, *Effective harnesses for long-running agents* | 3-0 |
| **압축(compaction)만으론 불충분** → 명시적 외부 상태(progress 파일 + git history)로 새 컨텍스트에서 상태 재구성 | 〃 | 3-0 |

→ 본 리포 findings DLT(`finding.sh` + 매 세션 재주입) + memory 파일이 이 "압축 넘어 외부 상태" 패턴에 해당.

## 5. AnchorIQ 하네스 채점 (연구 권고 대비)

| 연구 권고 | 본 리포 구현 | 판정 |
|------|------|------|
| 긴 문서 통째 금지 → 검색/섹션(black-box) | `plan-search.sh`(373→13줄) | ✅ |
| 압축만으론 부족 → 명시적 외부 상태 | findings DLT + memory 재주입 | ✅ |
| **코딩엔 멀티 X, 검증엔 멀티 O** | 코딩=단일 / 채널 B 다수결=검증에만 | ✅ |
| 단순함 기본, 정당화될 때만 복잡성 | 훅은 grep 등 경량, 멀티는 검증 한정 | ✅ |
| **외부 grounding > LLM self-critique** | check-stubs(grep)·테스트는 grounding / PostToolUse 의미 적대자는 LLM self-critique | △ → `ground-check.sh`로 컴파일 grounding 추가(보강) |

## 6. 실무 권고

**(a) 긴 설계문서 중간 소실 최적화**
1. 통째 주입 금지 → 섹션/검색(`plan-search.sh`)
2. 핵심은 컨텍스트 **앞·끝**에 배치(U자 회피)
3. 계층적 요약 + 외부 상태 파일로 영속화
4. white-box 보정(found-in-the-middle)은 API에서 불가 → black-box로만

**(b) 단일 가드레일 + 멀티 검증 결합**
1. 기반 = **단일 선형 에이전트 + 외부 grounding 가드레일**(컴파일/테스트/스키마/훅)
2. 멀티에이전트는 **고가치·병렬화 가능한 검증에만**(코딩 자체엔 X)
3. 다수결/LLM-judge는 **외부 신호가 받쳐줄 때** 신뢰

## 7. 한계 (신뢰성 차원 — 솔직히)

- **죽은 클레임 2개**: ① "self-reflection 9개 모델 유의 개선" ② "U자는 내재적 attention 편향 탓". → 위치-편향 **현상은 강건하나 정확한 인과 메커니즘은 논쟁 중**(cf. `arXiv:2510.10276`: 정보손실보다 retrieval 수요+causal 아키텍처 탓 주장).
- 멀티 90.2%는 **토큰량 교란** 미통제(동일 예산이면 단일 우위 시사: `arXiv:2510.26585`).
- **OpenAI/DeepMind 1차 클레임이 검증에서 생존 못 함** → 다관점 균형이 **Anthropic ↔ Cognition 축에 편중**.
- found-in-the-middle은 closed API 적용 불가 → 실무에선 black-box 대체.

### 미해결 질문
- closed API에서 긴 문서 중간소실을 줄이는 black-box 기법(분할·재배치·계층요약·RAG 청크순서) 간 **정량 비교 벤치마크 부족**.
- 단일 가드레일 + 멀티 다수결/LLM-judge 결합 아키텍처의 **end-to-end 신뢰성·비용 곡선 1차 측정 부재**.
- equal-token-budget 통제 시 멀티에이전트 순수 아키텍처 기여의 정량화 미해결.

## 출처 (1차 20 / 블로그 1)

**arXiv (peer-reviewed/프리프린트)**
- `2307.03172` Liu et al., *Lost in the Middle* (TACL 2024)
- `2510.05381` *Context Length Alone Hurts LLM Performance Despite Perfect Retrieval* (EMNLP 2025 Findings, Amazon)
- `2406.16008` *Found in the Middle* (ACL 2024 Findings)
- `2303.11366` *Reflexion* (NeurIPS 2023)
- `2405.06682` Renze & Guven, *Self-Reflection in LLM Agents*
- `2503.13657` *Why Do Multi-Agent LLM Systems Fail?* (MAST, Cemri 2025)
- `2203.11171` *Self-Consistency* · `2305.14325` *Multi-Agent Debate* · `2509.05396` · `2508.17536`

**AI 기업 공식 (1차)**
- Anthropic: *Effective context engineering for AI agents* · *Building effective agents* · *Multi-agent research system* · ***Effective harnesses for long-running agents*** · *Writing tools for agents*
- Cognition, *Don't Build Multi-Agents* (cognition.ai/blog)
- OpenAI: *Agents SDK — Guardrails* · *A Practical Guide to Building AI Agents*

**블로그(낮은 신뢰, 표시)**: LangChain, *Reflection Agents* (blog.langchain.com)
