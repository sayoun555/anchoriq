# AnchorIQ 하네스 엔지니어링 — 한눈에 이해하기

> **이 디렉토리는 "AnchorIQ 앱을 어떻게 만들었나"가 아니라, "그 앱을 설계와 어긋나지 않게 뽑아낸 도구(에이전트 하네스) 자체"를 다룬다.**
> 이 문서 하나만 읽어도 전체를 이해할 수 있게 정리했다. 더 깊은 내용은 각 섹션의 링크로.

---

## 1. 이게 뭐야? (3문장)

AI 에이전트(Claude Code)로 큰 코드베이스를 만들면 두 가지가 무너진다 — **설계 드리프트**(설계 문서를 안 읽고 자기 식대로 구현)와 **은밀한 미완성**(stub/`// 임시` 코드가 빌드 통과한 채 남음).
그래서 "설계 먼저 읽어라 / 임시방편 금지" 같은 규칙을 **문서에 글로 적는 대신, 자동으로 강제되는 훅(hook)으로** 만들었다.
이 강제 장치 묶음 = **하네스**. 코드는 여전히 에이전트가 쓰고, 하네스는 그 코딩 과정을 **감싸서 검사·교정**한다.

```
체크리스트(글로 적은 규칙)  →  지키면 좋은 것  →  잊으면 그냥 통과
하네스(코드로 강제)         →  강제되는 것     →  잊으면 자동으로 잡힘
```

## 2. 전체 구조 (구조도)

### (a) 코드 한 줄 만들 때, 하네스가 끼어드는 순서

```
[세션 시작]
   └─ SessionStart 훅 ──▶ session-context.sh
        "설계 먼저 읽어라" 프로토콜 + plan/ 문서 목록 + 미해결 위반(DLT) 주입

[작업: 예) "Neo4j 리포지토리 추가"]

[코드 쓰기 직전] Edit/Write 호출 순간
   └─ PreToolUse 훅 ──▶ governing-doc.sh
        파일 경로 분석 → "이 파일은 plan/ARCHITECTURE.md §Domain Service를 따른다"
        + "통째로 읽지 말고 plan-search.sh 로 그 절만"

[에이전트가 파일 작성]

[코드 쓴 직후] 파일 저장 순간
   └─ PostToolUse 훅 ──▶ 의미 적대자(agent) + review-protocol.md
        ① ground-check.sh 로 실제 컴파일(외부 증거 확보)
        ② plan-search.sh 로 지배 문서 해당 절만 읽고 대조
        ③ DDD/트랜잭션/계약 위반? → 위반 시 반려 + DLT 적재 / 깨끗하면 통과

[종료 직전]
   └─ Stop 훅 ──▶ check-stubs.sh
        백엔드에 stub/TODO/mock/// 임시 남았나 검사 → 있으면 경고
```

### (b) 디렉토리 구조

```
.claude/
  settings.json            팀 하네스: 4개 훅 + 권한 (git 추적)
  settings.local.json      개인 설정 (gitignore, 시크릿 금지)

scripts/harness/           ← 하네스의 실제 코드
  session-context.sh       SessionStart: 프로토콜·문서목록·DLT 주입
  governing-doc.sh         PreToolUse:  경로→지배 설계문서 라우팅
  review-protocol.md       PostToolUse 적대자의 리뷰 규칙(프롬프트)
  ground-check.sh          적대자용 외부 grounding (실제 컴파일/테스트)
  plan-search.sh           긴 설계문서에서 필요한 절/청크만 추출
  check-stubs.sh           Stop + CI: 미완성 마커 게이트
  finding.sh               위반 원장(DLT) CLI
  findings.jsonl           원장 데이터 (gitignore, 런타임)
  verify-findings.workflow.js  다수결 검증단 (멀티에이전트 Workflow)

docs/harness-engineering/  ← 지금 보는 문서들
  README.md                          이 문서 (마스터 가이드)
  HOOKS_AND_SCRIPTS.md               훅·스크립트 상세 레퍼런스
  ADVERSARY_AND_PROPAGATION.md       적대자 + 전파 3채널(A/B/C)
  CONTEXT_OPTIMIZATION.md            긴 문서 중간소실 방지
  RESEARCH_HARNESS_AND_MULTIAGENT.md 딥리서치(출처 21개) — 설계 근거
  troubleshooting/                   빌드 중 겪은 실제 문제 8건

HARNESS.md                 루트 1페이지 요약
```

### (c) 5개 레이어로 보면

```
① 지시      CLAUDE.md · plan/AGENTS.md          (무엇을 지킬지)
② 지식      plan/ 설계문서 21개                  (근거 자료)
③ 메모리    ~/.claude/.../memory/                (세션 간 기억)
④ 활성 가드  .claude/settings.json 훅 → scripts/  (강제 — 핵심)
⑤ 가드레일  권한 allow/deny                      (안전 경계)
```
①②③은 원래 있던 것, **이번에 ④⑤를 만들어 "글로 적은 규칙"을 "강제되는 시스템"으로 승격**했다.

## 3. 부품별 설명 (각 훅·스크립트가 하는 일)

| 부품 | 시점 | 하는 일 |
|------|------|---------|
| `session-context.sh` | 세션 시작 | 설계-우선 프로토콜 + plan/ 목록 + 미해결 위반 주입 |
| `governing-doc.sh` | 쓰기 직전 | 파일→지배 설계문서 한 줄 안내 (코드 파일만) |
| 의미 적대자 + `review-protocol.md` | 쓴 직후 | DDD/계약 위반 검사 → 반려 (멀티 검증 아님, 단일) |
| `ground-check.sh` | 적대자 판단 전 | **실제 컴파일** → 에러를 1차 증거로 (LLM 추측보다 우선) |
| `plan-search.sh` | 문서 읽을 때 | 373줄 문서 대신 관련 13줄 절만 (중간소실 방지) |
| `check-stubs.sh` | 종료 직전 / CI | stub/`// 임시` 마커 검사 |
| `finding.sh` + `findings.jsonl` | 항상 | 위반 원장(DLT) — 닫을 때까지 매 세션 재노출 |
| `verify-findings.workflow.js` | 수동/주기 | 다수결 검증단 — 쌓인 위반을 3렌즈 적대자가 재판정 |

## 4. 전파 3채널 — 적대자가 찾은 걸 어떻게 전달하나

적대자는 누구에게 메일을 안 보낸다. **다른 에이전트의 컨텍스트에 자기 결과를 주입**한다. → [ADVERSARY_AND_PROPAGATION.md](ADVERSARY_AND_PROPAGATION.md)

- **A (즉시 반려)** — 훅이 `reason`을 코더 컨텍스트에 재주입 → 그 자리에서 수정
- **B (다수결)** — Workflow가 3렌즈 적대자로 재판정 → 거짓양성 폐기 *(멀티에이전트는 여기만)*
- **C (영속)** — findings DLT에 적재 → 다음 세션에 다시 떠올림 (Kafka DLT와 동형)

## 5. 트러블슈팅 — 빌드 중 실제로 겪은 문제 (요약)

전부 진짜 겪은 것. 형식: 문제→원인→해결→CS원리. → [troubleshooting/](troubleshooting/)

| # | 문제 | 한 줄 교훈 |
|---|------|-----------|
| 1 | 새 훅이 같은 세션에 안 먹음 | 워처는 세션 시작 시 있던 설정만 감시 → `/hooks` 리로드 |
| 2 | Stop 게이트 blocking이 모든 턴 막음 | warn 기본 + `stop_hook_active` 재진입 가드 |
| 3 | `// 임시` 한국어 마커 누락·오탐 | 코멘트 앵커 정규식 + 신호 강도별 차등 |
| 4 | `paste -d ' · '`가 구분자 뒤섞음 | POSIX 툴은 구분자를 문자 리스트로 순환 → awk |
| 5 | 라우터가 `*JWT*.md` 문서를 코드로 오인 | 분류 전 확장자 게이팅 |
| 6 | settings.local.json 평문 JWT 누적 | 시크릿 제거 + 팀/개인 계층 분리 |
| 7 | PostToolUse 적대자가 매 편집 비쌈 | 빠른 종료·모델·입도 노브 |
| 8 | 검증단 워크플로우 args 문자열로 와서 0초 종료 | 직렬화 경계에서 JSON.parse 방어 |

## 6. 어디서 뭘 읽나 (읽는 순서)

1. **이 문서** — 전체 그림 (지금 여기)
2. [HOOKS_AND_SCRIPTS.md](HOOKS_AND_SCRIPTS.md) — 각 부품 입출력·사용법
3. [ADVERSARY_AND_PROPAGATION.md](ADVERSARY_AND_PROPAGATION.md) — 적대자·전파·다수결
4. [CONTEXT_OPTIMIZATION.md](CONTEXT_OPTIMIZATION.md) — 긴 문서 중간소실 대응
5. [RESEARCH_HARNESS_AND_MULTIAGENT.md](RESEARCH_HARNESS_AND_MULTIAGENT.md) — 왜 이렇게 설계했나(논문·기업 근거)
6. [troubleshooting/](troubleshooting/) — 막혔을 때

## 7. 운영 명령 (자주 쓰는 것)

```bash
# 미완성 마커 검사 (CI/커밋 전)
bash scripts/harness/check-stubs.sh

# 위반 원장 보기 / 닫기
bash scripts/harness/finding.sh list --open
bash scripts/harness/finding.sh resolve <id>

# 설계문서에서 필요한 절만
bash scripts/harness/plan-search.sh section ARCHITECTURE.md "Domain Service"

# 훅을 새로 추가/수정했으면 /hooks 를 한 번 열거나 재시작해야 라이브
```

## 한 줄 요약

**Session(준비) → Pre(쓰기 직전 안내) → Post(쓴 직후 적대자+컴파일 검사) → Stop(종료 전 게이트)** 로 코딩을 감싸고, 잡힌 위반은 **A(즉시 반려)·B(다수결)·C(영속 원장)** 로 전파한다. 단일에이전트 가드레일 + 멀티에이전트 검증을 결합한, [연구로 뒷받침되는](RESEARCH_HARNESS_AND_MULTIAGENT.md) 구조다.
