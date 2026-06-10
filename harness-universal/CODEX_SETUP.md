# Codex 하네스 설치 런북 (이 파일을 AI에게 그대로 줘라)

> **AI에게**: 너는 지금 *이 프로젝트*에 범용 하네스(harness-universal)를 Codex CLI용으로 설치·설정하는 작업을 한다. 아래 단계를 **순서대로 실행**하고, 각 단계의 **검증(✓)이 통과해야 다음으로 넘어간다**. 막히면 멈추고 사람에게 무엇이 왜 막혔는지 보고하라. 추측으로 우회하지 마라.
>
> 목표: ① 결정론 게이트(stub·secret)를 git 훅+편집 훅으로 강제 ② **이 스택 전용 footgun을 의미 적대자가 점검**(핵심 가치) ③ AGENTS.md로 지침 주입.

## 0. 전제 확인
```bash
codex --version            # 훅은 최신 Codex 필요. 0.13x 이상 권장(0.58 등 구형이면 'npm i -g @openai/codex@latest')
jq --version               # 없으면 설치 (brew install jq / apt-get install -y jq)
git rev-parse --show-toplevel   # git repo 여야 함. 아니면 'git init' 먼저
```
✓ codex·jq 있고 git repo다. (codex가 Gatekeeper "악성코드"로 막히면: 시스템 설정 > 개인정보 보호 및 보안 > "확인 없이 열기" 한 번 — 공식 @openai/codex라 미서명 오탐.)

## 1. 하네스 설치
`harness-universal/`가 이 머신 어딘가에 있다(예: AnchorIQ repo). 그 경로를 찾아 install 한다.
```bash
# HARNESS_SRC = harness-universal 폴더 경로 (찾거나 사람에게 물어라)
HARNESS_SRC="/Users/sanghyunyoun/anchoriq/harness-universal"   # ← 실제 경로로
bash "$HARNESS_SRC/install.sh" "$(git rev-parse --show-toplevel)"
```
이게 설치: `.harness/`(전체) · git `core.hooksPath` · `AGENTS.md`(없으면) · `.github/workflows/harness-gate.yml`.
✓ `.harness/check.sh` 와 `.harness/adapters/codex/codex-hook.sh` 가 존재한다.

## 2. 이 프로젝트에 맞게 `harness.config.json` 채우기 (★ 가장 중요, 네가 판단)
`.harness/harness.config.json`을 **이 프로젝트를 스캔해서** 수정하라:
- `source.root` / `source.extensions` / `source.mainGlobs` — 이 repo의 실제 소스 위치·언어. (예: Python→`{"root":"src","extensions":["py"],"mainGlobs":[]}`)
- `build.compileCommand` — 이 스택의 타입/컴파일 검사. (예: TS→`tsc --noEmit`, Python→`mypy .` 또는 `ruff check`, Go→`go build ./...`)
- `rules.stubMarkerPattern` / `secretPattern` — 보통 그대로 OK(주석 문법만 언어에 맞게).
- **`review.footguns` ← 반드시 이 스택 것으로 교체.** 현재는 Java/Spring 예시다. *컴파일은 되나 런타임에 깨지는* 이 스택의 함정을 적어라:

  | 스택 | footgun 예 (id: check) |
  |------|------------------------|
  | Python/Django | `orm-lazy`: 루프 안 lazy 쿼리/N+1 · `async-sync`: async 함수를 sync 컨텍스트서 호출 · `mutable-default`: 가변 기본인자 |
  | Node/TS | `floating-promise`: await 없는 Promise · `unhandled-rejection` · `any-escape`: any로 타입검사 우회 |
  | Go | `goroutine-leak`: 종료 안 되는 goroutine · `err-ignored`: `_ = err` · `ctx-not-propagated` |
  | Rust | `unwrap-panic`: 프로덕션 unwrap · `block-on-in-async` |

  형식: `{ "id":"...", "check":"무엇을 점검하는지 한 줄", "sev":"high|med" }`.
✓ `jq -e '.review.footguns|length>0' .harness/harness.config.json` 가 true. `source`·`build`가 이 프로젝트와 맞다.

## 3. Codex 훅 배선
```bash
mkdir -p .codex
cp .harness/adapters/codex/hooks.json .codex/hooks.json
```
이게 켜는 것(Codex 자동):
- **편집(apply_patch)마다** → `codex-hook.sh post` = 결정론 게이트(stub/secret) 즉시 차단
- **턴 끝(Stop)마다** → `codex-hook.sh stop` = 결정론 + **의미 적대자**(footgun/DDD를 LLM이 검토)
✓ `jq -e '.hooks.PostToolUse[0].matcher=="apply_patch"' .codex/hooks.json` true.

## 4. AGENTS.md (Codex가 자동으로 읽는 지침)
설치 때 루트에 `AGENTS.md`가 없었으면 템플릿이 복사됐다. 있다면 `.harness/AGENTS.md`의 **§2(강제)** 블록을 기존 것에 병합하라. `«»` 부분을 이 프로젝트 값(설계문서 위치·빌드·테스트 명령)으로 채워라. **32KiB 넘으면 Codex가 자르니** 간결히.
✓ 루트 `AGENTS.md`가 존재하고 이 프로젝트를 반영한다.

## 5. (선택, 강력) `--no-verify` 우회 방어
에이전트가 `git commit --no-verify`로 훅을 건너뛰는 걸 막으려면 PATH 앞에 래퍼를 둔다:
```bash
echo 'export PATH="'"$(git rev-parse --show-toplevel)"'/.harness/bin:$PATH"' >> ~/.zshrc   # 또는 현재 셸에 export
```
✓ `which git` 가 `.harness/bin/git` 를 가리킨다(새 셸).

## 6. 검증 — 진짜 막는지 (이걸로 "설치 성공" 입증)
```bash
# (a) 결정론 게이트: stub 심은 파일 → 커밋 차단돼야
printf 'TODO: dummy\n' > .harness_probe.<이_스택_확장자>   # 예: .py / .ts / .go (source.root 아래 둘 것)
git add -A && git commit -m probe    # ← pre-commit이 막아야 정상(커밋 안 됨)
git restore --staged .harness_probe.* 2>/dev/null; rm -f .harness_probe.*

# (b) Codex 편집 훅 시뮬레이션
echo '{"tool_name":"apply_patch"}' | bash .harness/adapters/codex/codex-hook.sh post < /dev/null
# → 변경된 stub 파일 있으면 {"decision":"block",...} 가 나와야
```
✓ (a) 커밋이 차단된다(stub). (b) block JSON이 나온다.

## 7. 라이브 확인 (사람 또는 너가 Codex 세션에서)
Codex로 소스 파일에 `// TODO` 같은 미완성 마커를 넣는 편집을 시켜라. **편집 직후 훅이 block**을 내고, **턴 끝(Stop)에 의미 적대자가 footgun을 점검**하면 — 설치 성공.
✓ 편집 훅 block 확인 + Stop 때 적대자 동작 확인.

---

## 사용법 (설치 후 일상)
- **그냥 평소대로 코딩한다.** 결정론 게이트(stub/secret)는 편집·커밋마다 자동. 의미 적대자(footgun/DDD)는 턴 끝마다 자동.
- 의미 적대자를 *차단형*으로(위반 시 멈춤) 쓰려면 환경변수 `HARNESS_REVIEW_BLOCK=1`. 기본은 *표면화*(보고만, 루프 방지).
- 수동 검토: `bash .harness/review.sh <파일...>` (LLM CLI 자동감지).
- 전체 점검: `bash .harness/check.sh --all`.
- CI: `.github/workflows/harness-gate.yml`가 PR마다 결정론 게이트를 돌린다(로컬 우회 백스톱).

## 동작 원리 (왜 이렇게)
- **강제는 git훅+CI**(어느 툴/사람이 고치든 발동) — 프롬프트 규칙은 무시되지만 훅은 구조적.
- **값의 핵심은 의미 적대자**(검증) — 정적 정보 주입은 유능 모델엔 redundant(측정됨). 그래서 footgun/DDD 검토에 무게.
- **재귀 가드**: 적대자가 codex를 부를 때 `HARNESS_REVIEWING=1`을 심어 내부 codex 훅이 또 적대자를 부르는 무한루프를 막는다.
- 자세히: `.harness/README.md` · `harness.config.json` 주석.
