# HARNESS.md — AnchorIQ 에이전트 하네스 엔지니어링

> **이 문서는 "무엇을 만들었나"가 아니라 "어떻게 일관되게 만들었나"를 다룬다.**
> AnchorIQ 코드베이스(Spring Boot 멀티모듈 + Next.js)는 산출물이고,
> 그 산출물을 설계와 어긋나지 않게 뽑아낸 **개발 하네스(harness)** 자체를 하나의 엔지니어링 결과물로 정의한다.

> **전체 문서**: [`docs/harness-engineering/`](docs/harness-engineering/) — 개념·훅 레퍼런스·적대자와 전파·컨텍스트 최적화·딥리서치·self-repair 가드·고도화 로드맵·트러블슈팅 + [`docs/papers/`](docs/papers/)(근거 논문 16편). 이 파일은 1페이지 요약.

## 1. 문제 정의

LLM 에이전트로 대규모 코드베이스를 만들면 두 가지가 무너진다.

1. **설계 드리프트** — 21개 설계 문서(`plan/`)를 정해놔도, 에이전트가 매번 읽지 않고 자기 방식으로 구현한다.
2. **은밀한 미완성** — "되는 것처럼" 보이는 stub/mock/`// 임시` 코드가 빌드는 통과한 채 남는다.

`CLAUDE.md`·`AGENTS.md`에 "반드시 설계 문서를 읽어라", "임시방편 금지"라고 **산문으로** 적어두는 것만으로는 부족하다.
산문 규칙은 모델이 지켜주길 *바라는* 것이고, 하네스는 *강제하는* 것이다. 이 둘의 차이가 이 프로젝트의 핵심이다.

```
체크리스트(산문)  →  지키면 좋은 것  →  모델이 잊으면 그대로 통과
하네스(코드)      →  강제되는 것     →  잊으면 컨텍스트 주입 / 게이트가 잡음
```

## 2. 하네스 레이어

모델을 둘러싼 다섯 레이어. 각 레이어가 결정론적으로 작동한다.

```
┌─────────────────────────────────────────────────────────────┐
│ ① 지시 레이어   CLAUDE.md · plan/AGENTS.md · frontend/AGENTS.md │  ← 레이어드/스코프 분리
│ ② 지식베이스    plan/ 설계문서 21개 (계약·아키텍처·트랜잭션)        │  ← context engineering
│ ③ 영속 메모리   ~/.claude/.../memory/ (세션 간 상태)              │  ← 누적 학습
│ ④ 활성 가드     .claude/settings.json hooks → scripts/harness/   │  ← 강제 (이 문서의 핵심)
│ ⑤ 가드레일      permissions allow/deny                          │  ← 안전 경계
└─────────────────────────────────────────────────────────────┘
```

①②③은 원래 있던 자산이고, **이 작업에서 ④⑤를 엔지니어링해 "산문 규칙"을 "강제되는 시스템"으로 승격**했다.

## 3. 활성 가드 (Hooks)

`.claude/settings.json`에 3개의 훅을 건다. 모두 `scripts/harness/` 아래의 bash 스크립트를 호출한다.

| 이벤트 | 스크립트 | 강제하는 규칙 | 동작 |
|--------|---------|--------------|------|
| `SessionStart` | `session-context.sh` | CLAUDE.md "설계 문서 학습 의무" | design-first 프로토콜 + `plan/` 문서 인덱스 + **열린 findings**를 컨텍스트로 주입 (동적 생성) |
| `PreToolUse` (Edit\|Write) | `governing-doc.sh` | CLAUDE.md "구현 전 설계 문서 참조" | 수정 파일 경로를 지배 `plan/` 문서로 매핑해 한 줄 포인터 주입 (non-blocking, 코드 파일만) |
| `PostToolUse` (Edit\|Write) | `review-protocol.md` (agent) | DDD/트랜잭션/인터페이스/계약 준수 | **의미 적대자**가 plan/·AGENTS.md 대조 → 위반 시 반려(채널 A) + DLT 적재(채널 C) |
| `Stop` | `check-stubs.sh --hook` | AGENTS.md 규칙 10 "임시방편 금지" | 종료 직전 백엔드 main 소스의 미완성 마커 검사, 잔존 시 경고 (기본 warn) |

> **부정적(적대) 에이전트와 전파**는 별도 문서로: [docs/harness-engineering/ADVERSARY_AND_PROPAGATION.md](docs/harness-engineering/ADVERSARY_AND_PROPAGATION.md). 적대자의 결과는 (A) `reason`으로 코더 컨텍스트 재주입, (B) 구조화 반환값으로 오케스트레이터 라우팅, (C) findings DLT 원장 → 다음 세션 재주입, 세 채널로 전파된다.

### 3-1. `session-context.sh` — 세션 부트스트랩

세션이 시작될 때마다 다음을 컨텍스트로 주입한다.
- design-first 3단계 프로토콜(AGENTS.md → IMPLEMENTATION_PLAN.md → 해당 Phase 문서)
- 현재 git 브랜치
- **`plan/`에서 동적으로 생성한 설계 문서 인덱스** — 문서가 늘거나 이름이 바뀌어도 하네스가 자동으로 따라간다(하드코딩 아님).

### 3-2. `governing-doc.sh` — 컨텍스트 라우터

`Edit`/`Write` 직전에 대상 파일 경로를 분석해 그 파일을 지배하는 설계 문서를 알려준다. 매핑 예:

| 파일 경로 패턴 | 지배 문서 |
|---------------|----------|
| `*infrastructure*neo4j*` | `ARCHITECTURE.md`(온톨로지) · `DB_OPTIMIZATION.md` |
| `*Consumer*.java` / `*kafka*` | `KAFKA_DESIGN.md` · `TRANSACTION_DESIGN.md`(Tier2) |
| `*controller*` | `API_ENDPOINTS.md` · `API_JSON_EXAMPLES.md` |
| `*payment*` / `*Jwt*` | `AUTH_PAYMENT.md` · `TRANSACTION_DESIGN.md`(Tier1) |
| `*application*Service*` | `ARCHITECTURE.md`(오케스트레이션만) · `TRANSACTION_DESIGN.md` |
| `*.java` (전부) | `AGENTS.md`(코딩 규칙 15) — 항상 추가 |

non-blocking이다(항상 allow + 컨텍스트만 추가). 코드 외 파일은 조용히 통과한다.

### 3-3. `check-stubs.sh` — anti-fabrication 품질 게이트

두 가지 모드로 동작한다.

```bash
# 사람/CI 모드: 발견 시 목록 + exit 1 (pre-commit / CI 게이트)
bash scripts/harness/check-stubs.sh

# Stop 훅 모드: 발견 시 systemMessage 경고 (non-blocking)
echo '{}' | bash scripts/harness/check-stubs.sh --hook
```

탐지 마커: `// TODO|FIXME|XXX|HACK`, `stub|mock|placeholder|simulate`, `임시|미구현|추후|일단`, `throw new UnsupportedOperationException`, `Not implemented`.
코멘트 앵커 기반이라 식별자 오탐을 피한다. `stop_hook_active`를 검사해 **무한 재호출을 방지**한다.

> **warn vs block** — 기본은 `warn`(턴을 막지 않음)이다. 현재 트리에는 실제 `// 임시` 마커가 1건 있어(`NotificationDispatcherImpl.java:79`, destination 해석 임시 처리) blocking으로 두면 매 턴이 막힌다. 트리를 마커-free로 만든 뒤에는 스크립트의 주석 한 줄을 바꿔 **blocking 래칫**으로 승격할 수 있다(회귀 시 에이전트를 다시 깨워 강제 수정).

## 4. 가드레일 (Permissions)

`.claude/settings.json` (팀 공통, 커밋됨):
- **allow** — 일반화된 25개 규칙(`./gradlew *`, `docker compose *`, `pnpm *`, `scripts/harness/*`, 읽기 전용 유틸 등). 누적된 200줄 일회성 명령을 정리한 결과.
- **deny** — 안전 경계: `rm -rf /*`, `git push --force *`, `.env` 읽기 차단(시크릿 노출 방지).

`.claude/settings.local.json` (개인, gitignore됨):
- 개인 도구(pandoc/weasyprint, pkill/lsof, MCP 브라우저 도구 등)만. **평문 JWT 토큰 3건 제거**, 150여 개 누적 항목을 42개로 정리.

## 5. 디렉토리

```
.claude/
  settings.json          # 팀 하네스 (hooks + 권한) — git 추적
  settings.local.json    # 개인 오버라이드 — gitignore
scripts/harness/
  session-context.sh           # SessionStart
  governing-doc.sh             # PreToolUse(Edit|Write)
  review-protocol.md           # PostToolUse 의미 적대자 프로토콜
  check-stubs.sh               # Stop + CI 게이트
  finding.sh / findings.jsonl  # findings DLT (채널 C)
  verify-findings.workflow.js  # 다수결 검증단 (채널 B)
  verify-findings-grounded.workflow.js  # 도구 쥔 검증단 (고도화 ①)
  plan-search.sh               # 설계문서 섹션/검색 (컨텍스트 최적화)
  ground-check.sh              # 외부 grounding (적대자 판단 전 컴파일/테스트)
HARNESS.md               # 이 문서 (1페이지 요약)
docs/harness-engineering/  # 전체 문서 + troubleshooting
```

`.gitignore`는 `.claude/*`를 무시하되 `!.claude/settings.json`만 예외로 추적한다 → 팀 하네스는 버전 관리, 개인/비밀은 제외.

## 6. 운영

- **활성화**: 훅은 세션 시작 시점에 존재하던 설정만 감시된다. `settings.json`을 새로 추가했다면 `/hooks`를 한 번 열거나 Claude Code를 재시작해야 라이브가 된다.
- **CI 게이트**: `bash scripts/harness/check-stubs.sh` (exit 1 on 마커) → pre-commit 훅이나 CI 파이프라인에 그대로 연결 가능.
- **검증**: 각 스크립트는 합성 stdin으로 파이프 테스트해 exit code와 부수효과를 확인한 뒤 배선했다.

## 7. 설계 결정과 한계

- **warn 우선** — 게이트는 기본 non-blocking. "되는 코드"를 막아 사용자를 가두는 것보다, 드러내고 사용자가 판단하게 한다. 진짜 회귀 방지가 필요하면 blocking으로 승격.
- **경로→문서 매핑은 휴리스틱** — 새 모듈 패턴이 생기면 `governing-doc.sh`의 `case`에 추가해야 한다. 매핑이 곧 하네스의 지식이므로 의도적으로 코드로 노출했다.
- **아직 없는 것**: 반복 가능한 eval(예: "빈 모듈 → DDD 스켈레톤이 규칙을 지키는가"를 자동 채점). 다음 단계.

## 8. 요약: before → after

| 규칙 | before (산문) | after (하네스) |
|------|--------------|---------------|
| 설계 문서 먼저 읽기 | CLAUDE.md 문장 | SessionStart 주입 + PreToolUse 경로별 라우팅 |
| 임시방편 금지 | AGENTS.md 규칙 10 | Stop 게이트 + CI exit code |
| 권한/시크릿 위생 | (누적 allowlist, JWT 평문) | 정리된 allow/deny + 시크릿 제거 |
