# Claude Code 하네스 설치 런북 (이 파일을 AI에게 그대로 줘라)

> **AI에게**: 너는 지금 *이 프로젝트*에 범용 하네스(harness-universal)를 **Claude Code**용으로 설치·설정한다. 아래 단계를 **순서대로**, 각 **검증(✓) 통과 후** 다음으로. 막히면 멈추고 사람에게 보고하라. 추측 우회 금지.
>
> ⚠️ **Codex와 다른 결정적 두 가지**: ① Claude 훅은 `.claude/settings.json`에 배선(기존 파일 **병합**, 덮어쓰기 금지) ② **`/hooks` 활성화는 사람만 가능** — 너(AI)는 못 누른다. 그 단계는 사람에게 넘겨라.
>
> 목표: ① 결정론 게이트(stub·secret)를 git훅+편집훅으로 강제 ② **이 스택 footgun을 의미 적대자가 점검**(핵심 가치) ③ CLAUDE.md로 지침 주입.

## 파일 위치 (먼저 — AI가 헤매지 않게)

**하네스 원본(설치 소스)** — 이 머신의 `harness-universal/` 폴더:
- 기본 경로: `/Users/sanghyunyoun/anchoriq/harness-universal/`
- 못 찾으면:
  ```bash
  find ~ /Users -maxdepth 6 -type f -name CLAUDE_SETUP.md -path '*harness-universal*' 2>/dev/null | head -1
  # → 나온 경로의 디렉토리가 HARNESS_SRC
  ```

**설치 후 — 이 프로젝트 안**(전부 repo 루트 `git rev-parse --show-toplevel` 기준):
| 경로 | 무엇 | 너가 손대나 |
|------|------|------------|
| `.harness/` | 하네스 메커니즘 전체 | 읽기만 |
| **`.harness/harness.config.json`** | ★ 스택 설정(source·build·footguns) | **Step 2 편집** |
| `.harness/check.sh` · `review.sh` | 결정론 게이트 · 의미 적대자 | 읽기만 |
| `.harness/session-context.sh` | 연속성 — SessionStart에 progress 재주입 | 읽기만 |
| `progress.md`(루트) | 작업 상태·핸드오프(긴 작업) | 작업하며 갱신 |
| `.harness/adapters/claude/settings.json` | Claude 훅 *템플릿*(여기서 hooks 블록 복사) | 참조 |
| `.harness/adapters/codex/codex-hook.sh` | 훅 다리(Claude·Codex 공용) | 읽기만 |
| `.claude/settings.json` | **Claude 훅 배선(여기에 병합)** | Step 3 병합 |
| `CLAUDE.md`(루트) | Claude가 자동으로 읽는 지침 | Step 4 |
| `.github/workflows/harness-gate.yml` | CI 백스톱 | 자동 |
| `.harness/bin/git` | --no-verify 우회 차단 래퍼 | Step 5(선택) |

> 모든 명령은 **repo 루트에서** 실행하라.

## 0. 전제 확인
```bash
jq --version                     # 없으면 설치(brew install jq / apt-get install -y jq)
git rev-parse --show-toplevel    # git repo 여야 함. 아니면 git init
ls .claude/settings.json 2>/dev/null && echo "기존 settings 있음(병합 대상)" || echo "settings 없음(새로 생성)"
```
✓ jq 있고 git repo다. (Claude Code 자체는 이 세션을 돌리는 도구이므로 별도 설치 확인 불필요.)

## 1. 하네스 설치
`HARNESS_SRC` = 위 "파일 위치"의 원본 경로.
```bash
HARNESS_SRC="/Users/sanghyunyoun/anchoriq/harness-universal"   # 위 find로 확인
[ -f "$HARNESS_SRC/install.sh" ] || { echo "HARNESS_SRC 못 찾음 — find로 경로부터"; exit 1; }
bash "$HARNESS_SRC/install.sh" "$(git rev-parse --show-toplevel)"
```
설치: `.harness/`(전체) · git `core.hooksPath` · `AGENTS.md`(없으면) · CI. (install.sh는 `.claude/settings.json`은 **안 건드린다** — Step 3에서 네가 병합.)
✓ `.harness/check.sh` 와 `.harness/adapters/claude/settings.json` 존재.

## 2. 이 프로젝트에 맞게 `harness.config.json` 채우기 (★ 가장 중요, 네가 판단)
`.harness/harness.config.json`을 이 프로젝트를 스캔해서 수정:
- `source.root` / `extensions` / `mainGlobs` — 실제 소스 위치·언어
- `build.compileCommand` — 이 스택 타입/컴파일 검사(TS `tsc --noEmit` · Python `mypy .`/`ruff check` · Go `go build ./...` · Java `./gradlew compileJava`)
- `rules.stubMarkerPattern` / `secretPattern` — 주석 문법만 언어에 맞게
- **`review.footguns` ← 반드시 이 스택 것으로 교체**(현재 Java/Spring 예시). *컴파일은 되나 런타임에 깨지는* 함정:

  | 스택 | footgun 예 |
  |------|-----------|
  | Python/Django | orm-lazy(N+1) · async-sync · mutable-default(가변 기본인자) |
  | Node/TS | floating-promise(await 없음) · any-escape · unhandled-rejection |
  | Go | goroutine-leak · err-ignored(`_ = err`) · ctx-not-propagated |
  | Java/Spring | @Transactional 자기호출(프록시 우회) · JPA N+1 · Kafka ack 누락 |

  형식: `{ "id":"...", "check":"한 줄 점검 내용", "sev":"high|med" }`
✓ `jq -e '.review.footguns|length>0' .harness/harness.config.json` true. source·build가 맞다.

## 3. Claude 훅 배선 — `.claude/settings.json`에 **병합**(덮어쓰기 금지)
`.harness/adapters/claude/settings.json`의 `hooks` 블록을 기존 `.claude/settings.json`에 합친다(permissions 등 기존 키 보존).
```bash
mkdir -p .claude
if [ -f .claude/settings.json ]; then
  # 기존과 병합(어댑터의 hooks 를 기존 위에 — 기존 hooks 있으면 사람이 확인)
  jq -s '.[0] * {hooks: .[1].hooks}' .claude/settings.json .harness/adapters/claude/settings.json > .claude/settings.json.new \
    && mv .claude/settings.json.new .claude/settings.json
else
  jq '{hooks: .hooks}' .harness/adapters/claude/settings.json > .claude/settings.json
fi
jq -e '.hooks.PostToolUse[0].matcher=="Edit|Write"' .claude/settings.json   # 검증
```
이게 켜는 것:
- **세션 시작마다** → `session-context.sh` = `progress.md`(작업 상태) 재주입(긴/다세션 연속성)
- **편집(Edit/Write)마다** → 결정론 게이트(stub/secret) 즉시 차단
- **턴 끝(Stop)마다** → 결정론 + **의미 적대자**(footgun/DDD)
✓ `.claude/settings.json`에 SessionStart·PostToolUse(`Edit|Write`)·Stop 훅이 있고, 기존 permissions 등이 보존됐다.

> 기존에 다른 hooks가 있었다면 `jq -s '.[0] * .[1]'` 단순 병합은 덮을 수 있다 — 그 경우 **두 hooks를 사람이 직접 합치게** 멈추고 보고하라.

## 4. CLAUDE.md (Claude가 자동으로 읽는 지침)
Claude Code는 `AGENTS.md`가 아니라 **`CLAUDE.md`**를 읽는다(AGENTS.md 지원 보류). 설치 때 생긴 `AGENTS.md`를 import로 연결:
```bash
if [ -f CLAUDE.md ]; then
  grep -q '@AGENTS.md' CLAUDE.md || printf '\n## 하네스 지침\n@AGENTS.md\n' >> CLAUDE.md
else
  printf '# CLAUDE.md\n\n@AGENTS.md\n' > CLAUDE.md
fi
```
(또는 `AGENTS.md` 내용을 CLAUDE.md에 직접 병합. `«»` 부분은 이 프로젝트 값으로.)
✓ 루트 `CLAUDE.md`가 존재하고 AGENTS.md를 가리키거나 지침을 담는다.

## 5. (선택, 강력) `--no-verify` 우회 방어
```bash
echo 'export PATH="'"$(git rev-parse --show-toplevel)"'/.harness/bin:$PATH"' >> ~/.zshrc   # 또는 현재 셸 export
```
추가로 `.claude/settings.json`의 `permissions.deny`에 `"Bash(git commit --no-verify *)"` 등을 넣어도 됨(부분적).
✓ 새 셸에서 `which git` 가 `.harness/bin/git`.

## 6. 검증 — 진짜 막는지
```bash
# (a) 결정론 게이트: stub 심은 파일 → 커밋 차단돼야
printf 'TODO: dummy\n' > .harness_probe.<확장자>   # source.root 아래, 이 스택 확장자
git add -A && git commit -m probe     # ← pre-commit이 막아야 정상
git restore --staged .harness_probe.* 2>/dev/null; rm -f .harness_probe.*

# (b) 편집 훅 시뮬레이션
echo '{"tool_name":"Edit"}' | bash .harness/adapters/codex/codex-hook.sh post < /dev/null
# → 변경된 stub 있으면 {"decision":"block",...}
```
✓ (a) 커밋 차단. (b) block JSON.

## 7. 활성화 (★ 사람만 가능 — 너는 못 한다)
> **여기서 멈추고 사람에게**: "Claude Code에서 `/hooks`를 한 번 열거나(또는 재시작) 훅을 로드해 주세요. 새로 추가한 훅은 세션 시작 때 없던 거라 그래야 라이브가 됩니다."
>
> 이유: Claude Code는 세션 시작 시 존재하던 settings만 감시한다. `/hooks`(UI) 열기 또는 재시작 = AI가 못 하는 사람 동작.

활성화 후 라이브 확인: 소스 파일에 `// TODO`를 넣는 편집을 시켜 → **편집 직후 block**, **턴 끝에 적대자 점검**이면 성공.

---

## 사용법 (설치+활성화 후)
- 평소대로 코딩. 결정론 게이트(stub/secret)는 편집·커밋마다 자동, 의미 적대자(footgun/DDD)는 턴 끝마다 자동.
- **긴 작업이면 `progress.md`에 완료/남은일/막힌점 기록** → SessionStart에 자동 재주입(세션 끊겨도 이어감). 짧은 작업엔 생략(gitignore).
- 적대자 *차단형*: `HARNESS_REVIEW_BLOCK=1`(기본은 표면화·루프방지).
- 수동: `bash .harness/review.sh <파일...>` · 전체: `bash .harness/check.sh --all`.
- CI: `.github/workflows/harness-gate.yml`가 PR마다 백스톱.

## 더 강한 Claude 하네스를 원하면
이 범용판은 적대자가 *턴마다(Stop)*다. **편집마다(per-edit) `agent` 훅 적대자 + governing-doc 라우팅 + findings DLT + plan-search**까지 갖춘 *풀 Claude 하네스*는 AnchorIQ의 [`scripts/harness/`](../scripts/harness/) + [`DEPLOY_HARNESS.md`](../scripts/harness/DEPLOY_HARNESS.md)에 있다(Claude Code 전용). 이식 가능·툴 무관이 우선이면 이 범용판, Claude에서 최대 강도면 풀판.

## 동작 원리
- 강제는 git훅+CI(어느 툴/사람이 고치든 발동). 프롬프트 규칙은 무시되나 훅은 구조적.
- 값의 핵심은 의미 적대자(검증) — 정적 정보 주입은 유능 모델엔 redundant(측정됨).
- 재귀 가드(`HARNESS_REVIEWING`)는 Codex용(LLM CLI 재호출). Claude 어댑터는 command 브리지라 재귀 없음.
- 자세히: `.harness/README.md`.
