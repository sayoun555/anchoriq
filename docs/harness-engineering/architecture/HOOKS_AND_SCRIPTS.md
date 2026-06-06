# 훅 & 스크립트 레퍼런스

`.claude/settings.json`의 4개 훅과 `scripts/harness/`의 5개 스크립트, 그리고 권한 정책의 입출력·동작을 정리한다.

모든 훅 스크립트는 자기 위치에서 리포 루트를 역산한다(`SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)`) → cwd와 무관하게 동작. settings.json은 스크립트를 `$CLAUDE_PROJECT_DIR` 기준으로 호출한다(머신 절대경로 하드코딩 없음).

---

## 1. SessionStart → `session-context.sh`

**역할**: 매 세션 시작 시 design-first 프로토콜 + `plan/` 문서 인덱스 + **열린 findings**를 컨텍스트로 주입.

**출력**(stdout JSON):
```json
{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"🧭 ..."}}
```
- 문서 인덱스는 `ls plan/*.md`로 **동적 생성** → 문서가 늘거나 이름이 바뀌어도 자동 추종(하드코딩 아님).
- `finding.sh count-open`이 0보다 크면 열린 finding 목록을 함께 주입(전파 채널 C).

**테스트**:
```bash
echo '{"source":"startup"}' | bash scripts/harness/session-context.sh | jq -r '.hookSpecificOutput.additionalContext'
```

---

## 2. PreToolUse(Edit|Write) → `governing-doc.sh`

**역할**: 코드를 쓰기 **직전**, 대상 파일을 지배하는 `plan/` 문서를 한 줄로 알려준다. non-blocking(항상 allow + 컨텍스트만 추가).

**입력**: 훅 JSON의 `.tool_input.file_path`.
**매핑**(발췌):

| 경로 패턴 | 지배 문서 |
|----------|----------|
| `*infrastructure*neo4j*` | ARCHITECTURE.md · DB_OPTIMIZATION.md |
| `*Consumer*.java` / `*kafka*` | KAFKA_DESIGN.md · TRANSACTION_DESIGN.md(Tier2) |
| `*controller*` | API_ENDPOINTS.md · API_JSON_EXAMPLES.md |
| `*payment*` / `*Jwt*` | AUTH_PAYMENT.md · TRANSACTION_DESIGN.md(Tier1) |
| `*application*Service*` | ARCHITECTURE.md(오케스트레이션만) · TRANSACTION_DESIGN.md |
| `*.java` (전부) | AGENTS.md — 항상 |

코드 외 파일은 출력 없이 통과.

**테스트**:
```bash
echo '{"tool_name":"Edit","tool_input":{"file_path":"backend/.../controller/VesselController.java"}}' \
  | bash scripts/harness/governing-doc.sh | jq -r '.hookSpecificOutput.additionalContext'
```

> `governing-doc.sh`는 "이 문서를 **읽어라**"라고 *알려주고*, 아래 PostToolUse 적대자는 "너 그 문서대로 **안 했다**"라고 *잡아낸다*. 알림 → 강제의 한 단계 진화.

---

## 3. PostToolUse(Edit|Write) → 의미 리뷰어 (agent 훅)

**역할**: 코드를 쓴 **직후**, 추론하는 적대자가 방금 파일을 지배 `plan/` 문서 + AGENTS.md와 대조. grep이 못 잡는 *의미* 위반(DDD 레이어, 트랜잭션 경계, 인터페이스 우선, 캡슐화, 계약)을 잡는다.

**구성**: `.claude/settings.json`의 `type:"agent"` 훅. 프롬프트는 짧고, 실제 리뷰 규칙은 버전관리되는 [`scripts/harness/review-protocol.md`](../../../scripts/harness/review-protocol.md)에 분리.

**전파**:
- 위반 발견 → `finding.sh add ...`로 **DLT 적재**(채널 C) → `ok=false`로 **block**, `reason`이 코더 컨텍스트에 재주입(채널 A).
- 깨끗하면 `ok=true` 통과.

**오탐 방지**: "고신뢰·구체적 위반만 block, 확신 없으면 통과"가 프로토콜의 기본값. false positive로 코더를 가두는 게 더 나쁘다.

**비용 노브**: PostToolUse는 매 Edit/Write에 발화한다. 프로토콜 0단계에서 백엔드 main `.java`가 아니면 즉시 통과시켜 비용을 줄인다. 모델은 기본 Haiku(저비용/고빈도) — DDD 적발률을 높이려면 settings.json 훅에 `"model":"claude-sonnet-4-6"`을 추가. 더 줄이려면 matcher를 `Write`(신규 파일)로 좁히거나, Stop 시점 배치 리뷰로 옮긴다. → [troubleshooting/AGENT_HOOK_COST_ON_EVERY_EDIT.md](../troubleshooting/AGENT_HOOK_COST_ON_EVERY_EDIT.md)

---

## 4. Stop → `check-stubs.sh --hook`

**역할**: 종료 직전, 백엔드 main 소스의 미완성 마커(`// TODO|FIXME|stub|mock|placeholder|simulate|임시|미구현` 등, `throw new UnsupportedOperationException`)를 검사. AGENTS.md 규칙 10("임시방편 금지")의 결정론적 강제.

**두 모드**:
```bash
bash scripts/harness/check-stubs.sh           # 사람/CI: 발견 시 목록 + exit 1
echo '{}' | bash scripts/harness/check-stubs.sh --hook   # Stop 훅: 발견 시 systemMessage 경고
```

**warn vs block**: 기본은 **warn**(턴을 막지 않음). 코멘트 앵커 기반 패턴이라 식별자 오탐은 피하지만, `// 임시` 같은 한국어 설명 코멘트는 오탐 여지가 있어(→ troubleshooting) blocking을 기본값으로 두지 않았다. `stop_hook_active`를 검사해 **무한 재호출을 방지**한다. 트리가 마커-free가 되면 스크립트 주석 한 줄로 blocking 래칫으로 승격 가능.

---

## 5. `finding.sh` — findings DLT 원장 CLI

채널 C(영속 전파)의 저장소. 상세는 [ADVERSARY_AND_PROPAGATION.md](ADVERSARY_AND_PROPAGATION.md).

```bash
finding.sh add --file F --rule R [--sev high|med|low] --desc "..."   # 적재 → id 출력
finding.sh list [--open|--all]                                       # 목록
finding.sh resolve <id> [--note "..."]                               # 해결
finding.sh count-open                                                # 열린 건수
```
저장 포맷: `findings.jsonl` 한 줄당 `{id,ts,status,severity,file,rule,desc}`. `resolve`는 `status="resolved"`로 갱신. 런타임 상태라 gitignore.

---

## 6. `plan-search.sh` — 설계문서 섹션/검색 (컨텍스트 최적화)

긴 `plan/` 문서를 통째로 컨텍스트에 올리면 "중간 소실(lost-in-the-middle)"이 생긴다. 통째 읽기 대신 관련 절/청크만 가져온다. 상세: [CONTEXT_OPTIMIZATION.md](CONTEXT_OPTIMIZATION.md).

```bash
plan-search.sh toc <doc>                  # 목차(헤더)만
plan-search.sh section <doc> "<키워드>"   # 그 절만 (하위 절 포함). 예: ARCHITECTURE 373줄 → "Domain Service" 13줄
plan-search.sh grep "<질의>"              # plan/ 전체에서 관련 라인
```
`governing-doc.sh`(PreToolUse)와 `review-protocol.md`(PostToolUse 적대자)가 이 도구로 **지배 문서의 해당 절만** 읽도록 배선돼 있다.

---

## 7. `ground-check.sh` — 외부 grounding (적대자 보강)

연구(`RESEARCH_HARNESS_AND_MULTIAGENT.md` §2)에 따르면 LLM self-critique는 약하고 **외부 신호(컴파일/테스트)가 핵심**이다. 의미 적대자가 추측으로 판단하기 전에 대상 모듈을 실제 컴파일해 결정론적 증거를 확보한다.

```bash
ground-check.sh <file>          # 파일 경로 → Gradle 모듈 추출 → :module:compileJava
ground-check.sh <file> --test   # + :module:test (행위 신호, 느림)
```
- 컴파일 **실패** → 에러가 1차 증거(LLM 추측보다 우선) → 적대자가 그걸로 반려.
- 컴파일 **통과** → 타입/시그니처 차원은 깨끗 → 적대자가 그 차원을 재추측 안 함(오탐 방지).
- 모듈 아닌 파일(문서 등)은 grounding 생략. 훅을 막지 않는 정보 제공형.

`review-protocol.md` §1.5에서 적대자가 판단 전에 호출하도록 배선.

---

## 권한 정책 (`.claude/settings.json` `permissions`)

**allow** — 일반화된 규칙(머신 비종속). 누적된 200줄 일회성 명령을 25개 규칙으로 정리:
`./gradlew *`, `docker compose *`, `pnpm *`, `bash scripts/harness/*`, 읽기전용 유틸(`grep/find/ls/cat/jq`), `git status|log|diff|branch`, `redis-cli/cypher-shell/psql`, `WebSearch` 등.

**deny** — 안전 경계(가드레일):
- `rm -rf /*`, `rm -rf ~/*` — 파괴적 삭제
- `git push --force *`, `git push -f *` — 강제 푸시
- `.env` 읽기 차단 — 시크릿이 모델 컨텍스트로 새지 않게

`settings.json`(팀, 커밋) vs `settings.local.json`(개인, gitignore) 분리. 후자에서 **평문 JWT 토큰 3건 제거**, 150여 항목 → 42개 정리. → [troubleshooting/PLAINTEXT_JWT_IN_SETTINGS.md](../troubleshooting/PLAINTEXT_JWT_IN_SETTINGS.md)
