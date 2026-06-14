# harness-universal — 툴 무관 에이전트 하네스 (Codex·Cursor·Aider·Claude·사람)

> AnchorIQ의 [Claude Code 전용 하네스](../scripts/harness/)를 **어느 에이전트/툴에서도** 쓰도록 일반화한 판. 근거 조사: [docs/harness-engineering/research/CODEX_AND_PORTABILITY.md](../docs/harness-engineering/research/CODEX_AND_PORTABILITY.md).
>
> 핵심 설계 원리(우리 16실험 결론과 정합): **하네스의 측정된 값은 "미리 알려주기(주입)"가 아니라 "검증·강제"에 있다.** 그래서 강제를 *특정 툴의 훅*이 아니라 **git 훅 + CI**(어느 툴/사람이 고치든 발동)에 둔다.

## 3층 구조

```
① 툴 무관 코어 (AI 툴 없어도 작동)
   AGENTS.md          설계-우선 프로토콜 + 규칙 (Codex·Cursor·Aider·Copilot… 다 읽는 Linux Foundation 표준)
   git-hooks/         pre-commit/pre-push → check.sh (결정론 게이트: stub·secret·size)
   bin/git            --no-verify 우회 차단 래퍼 (PATH 앞에)
   ci/harness-gate.yml  CI 백스톱 — 로컬 우회를 최종 포획
② 툴별 훅 어댑터 (같은 check.sh 재사용, 편집 직후 검사)
   adapters/codex/    .codex/hooks.json (matcher=apply_patch) + codex-hook.sh
   adapters/claude/   .claude/settings.json hooks 블록
③ 의미 적대자 (검증의 핵심 가치)
   review.sh          변경 파일을 review-protocol.md + config.footguns로 LLM(codex/claude 자동감지)에 검토
④ 연속성 (긴/다세션 — 측정상 '주입이 값 나는' 유일 경계)
   session-context.sh SessionStart에 progress.md(작업 상태·핸드오프) 재주입 — 어댑터 SessionStart 훅이 호출
⑤ Figma 시각 렌즈 (UI 프로젝트만 — 백엔드면 자동 비활성)
   figma-check.sh     생성 컴포넌트 *렌더 결과*를 Figma 디자인과 측정-비교(UIMatch). 온디맨드/pre-push/CI(편집마다 X)
```

> **렌즈는 *작업하는 파일에 따라 자동 선택*된다(모드 스위치 아님).** `.java` 백엔드 만지면 DDD 적대자·footgun이, `.tsx` 프론트 만지면 결정론 게이트가 돈다 — 서로 안 끼어든다. **Figma 렌즈는 UI 작업에만**(config `figma.components` 비면 no-op), 그것도 *무거워서* 컴포넌트 완성/pre-push/CI 때만(편집마다 아님).

## 왜 이 형태인가 (연구 근거)
- **AGENTS.md** = [agents.md 표준](https://agents.md/)(Agentic AI Foundation/Linux Foundation). Codex·Cursor·Aider·Copilot·Windsurf·Gemini CLI·Zed 등 31개+ 툴이 읽음. "단일 진실원" 패턴.
- **"프롬프트의 규칙은 권고, 훅의 규칙은 구조적."** 에이전트는 산문 규칙을 무시할 수 있다 → git 훅이 *구조적*으로 강제. 단 `--no-verify`로 우회 가능 → `bin/git` 래퍼 + CI 백스톱이 그 구멍을 막음.
- **Codex도 풍부한 훅**이 있다(PreToolUse/PostToolUse/Stop, matcher=`apply_patch`) — 단 `command` 훅만(agent 훅 없음)이라 의미 적대자는 `review.sh`(LLM CLI 호출)로.

## 설치 (다른 프로젝트에)
```bash
bash install.sh /path/to/your-project
```
넣는 것: `.harness/`(메커니즘 전체) · git `core.hooksPath` 배선 · `AGENTS.md`(없으면) · CI 백스톱.
그다음 **반드시**: `.harness/harness.config.json`을 그 스택에 맞게 — 특히 **`review.footguns`(스택 전용, 교체 필수)** · `build.compileCommand` · `source`. (자세히: install 출력 / 이 파일 아래.)

> **AI에게 그대로 주면 알아서 설치하는 런북**: Codex → [`CODEX_SETUP.md`](CODEX_SETUP.md) · Claude Code → [`CLAUDE_SETUP.md`](CLAUDE_SETUP.md). (각 단계 검증게이트 포함, 파일 위치 맵 포함.)

## 파일
| 파일 | 역할 |
|------|------|
| `check.sh` | 결정론 게이트 — staged/전체/지정 파일에서 stub·secret(BLOCK)·size(WARN). git훅·CI·에디터훅 공용 |
| `review.sh` | 의미 적대자 — LLM CLI 자동감지(codex/claude)로 footgun·DDD 검토 |
| `session-context.sh` | 연속성 — SessionStart에 `progress.md`(작업 상태) 재주입(긴/다세션) |
| `figma-check.sh` | Figma 시각 렌즈(UI만) — 렌더 컴포넌트 vs Figma 디자인 측정-비교(UIMatch). config `figma.components` 채워야 동작 |
| `harness.config.json` | **이식 contract** — 프로젝트 전용값 전부(이것만 교체) |
| `review-protocol.md` | 적대자 프로토콜(판단 규칙) |
| `AGENTS.md` | 범용 지침 템플릿 |
| `git-hooks/{pre-commit,pre-push}` | 결정론 강제(툴 무관) |
| `bin/git` | --no-verify 방어 래퍼 |
| `adapters/{codex,claude}/` | 툴별 훅 배선 |
| `ci/harness-gate.yml` | CI 백스톱 |
| `install.sh` | 타깃에 전부 배선 |

## Claude Code 전용 풀 하네스와의 차이
[`scripts/harness/`](../scripts/harness/)(이 repo)는 Claude Code에 깊게 배선된 *풀* 버전 — governing-doc 라우팅·의미 적대자 *agent* 훅·findings DLT·plan-search 컨텍스트 최적화·오케스트레이션 워크플로우까지. `harness-universal/`는 그 *측정된 핵심 가치*(결정론 게이트 + 의미 적대자)만 **툴 무관**으로 추린 이식판이다.

## 한계 (정직)
- 의미 적대자(`review.sh`)는 LLM CLI가 있어야 동작(없으면 결정론 게이트만). LLM 검토는 확률적.
- git 훅 강제는 `--no-verify`로 우회 가능 → `bin/git` 래퍼(PATH 의존) + CI가 보완. CI가 최종 보루.
- `review.footguns`는 스택 전용 — 새 프로젝트엔 교체 안 하면 헛다리.
- AGENTS.md는 Codex에서 32KiB 초과 시 잘림 → 길면 분할.
