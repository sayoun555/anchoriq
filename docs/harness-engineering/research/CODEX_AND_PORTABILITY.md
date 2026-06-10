# Codex & 범용 이식성 — 딥서치 자료 (2026-06-10)

> 목적: 하네스를 OpenAI Codex CLI + *어느 에이전트에서도* 쓸 수 있게 만들기 위한 사실 조사. 공식 출처 우선(블로그는 보조). 결론: **범용 하네스 = AGENTS.md(툴 무관 지침) + git 훅/CI(툴 무관 강제) + 툴별 훅 어댑터(Codex/Claude)**, 우리 연구 결론("값은 검증·강제에")과 정합.

## 1. Codex CLI도 풍부한 훅 시스템이 있다 (Claude Code와 거의 동형)

[Codex Hooks 공식 문서](https://developers.openai.com/codex/hooks):

- **이벤트**: `SessionStart`(startup/resume/clear/compact) · `SubagentStart` · **`PreToolUse`**(Bash·`apply_patch`·MCP 가로챔) · `PermissionRequest` · **`PostToolUse`** · `PreCompact` · `PostCompact` · `UserPromptSubmit` · `SubagentStop` · **`Stop`**.
- **배선 위치**: `~/.codex/hooks.json`, `~/.codex/config.toml`(인라인 `[[hooks.PreToolUse]]`), `<repo>/.codex/hooks.json`, `<repo>/.codex/config.toml`, 플러그인 매니페스트, 엔터프라이즈 `requirements.toml`.
- **핸들러 스키마(Claude Code와 사실상 동일)**: `{ "hooks": { "PreToolUse": [ { "matcher": "^Bash$", "hooks": [ {"type":"command","command":"...","timeout":30,"statusMessage":"..."} ] } ] } }`. `type`은 **`command`만 실행**(agent/prompt 없음). `commandWindows`로 플랫폼 분기. `timeout` 기본 600s.
- **stdin JSON**: `session_id` · `cwd` · `hook_event_name` · `model` · `permission_mode` · `turn_id`, 이벤트별로 `tool_name`·`tool_input`·`tool_use_id` 등. **Claude Code와 같은 snake_case 모양.**
- **stdout JSON(제어)**: `decision:"block"` + `reason`, `hookSpecificOutput.additionalContext`(모델 가시 텍스트), `permissionDecision:"allow|deny|ask"`, `updatedInput`. **exit 0=성공, exit 2=차단(stderr에 사유).** → Claude Code 출력과 호환.

**함의**: 우리 하네스 `.sh` 훅(governing-doc·check-stubs·lint·session-context)은 *같은 stdin/stdout JSON*을 쓰므로 Codex에 거의 그대로 이식 가능. **차이 둘**:
1. 에디터 툴이 `Edit|Write`가 아니라 **`apply_patch`** → matcher 변경.
2. **`agent`/`prompt` 훅 없음(command만)** → 우리 *의미 적대자*(Claude에선 agent 훅)를 **LLM CLI를 호출하는 command 훅**(`codex exec` / `claude -p`)으로 변환해야.

⚠️ **버전 주의**: [최신 Codex는 0.139.0(2026-06-09)](https://developers.openai.com/codex/changelog). 로컬 설치본 **v0.58.0**은 구형 — 훅 지원 불확실 + 미서명(Gatekeeper "악성코드" 오탐) 가능성. **codex 업데이트 시 훅+서명 둘 다 해소될 것.**

## 2. AGENTS.md = 진짜 범용 지침 레이어 (Linux Foundation 표준)

[agents.md](https://agents.md/) — **Agentic AI Foundation(Linux Foundation)** 스튜어드(OpenAI Codex·Amp·Google Jules·Cursor·Factory 공동).

- **읽는 툴**: Codex · Cursor · Aider · GitHub Copilot · Windsurf · Gemini CLI · Zed · Warp · Devin · goose · opencode · Junie · VS Code 등 광범위. **Claude Code만 CLAUDE.md**(AGENTS.md 지원 보류 → import로 연결).
- **[Codex의 처리](https://developers.openai.com/codex/guides/agents-md)**: 디렉토리별 우선순위 `AGENTS.override.md > AGENTS.md > project_doc_fallback_filenames`. 루트→cwd로 내려가며 **디렉토리당 1개씩, 빈 줄로 연결, 가까운 게 override**. **`project_doc_max_bytes` 기본 32 KiB — 초과 시 잘림**(분할 권장).
- **[모범 패턴](https://www.mdfile.exchange/compare/agents-md-vs-cursor-rules)**: 루트 `AGENTS.md`(~200줄)를 **단일 진실원**, 툴별 파일(.cursorrules·CLAUDE.md)은 "see AGENTS.md" 한 줄 포인터.

## 3. git 훅 = 유일하게 *툴 독립적·구조적* 강제 (+ 우회 방어)

[pydevtools](https://pydevtools.com/handbook/how-to/how-to-stop-ai-agents-from-bypassing-pre-commit-hooks/) · [jonesrussell](https://jonesrussell.github.io/blog/git-hooks-ai-agents/) · [wilddog64](https://dev.to/wilddog64/i-built-the-guardrails-into-the-repo-not-the-prompt-4n3l) 공통 결론 — **우리 ADVERSARY/DRIFT 실험 결론과 동일**:

> **"프롬프트의 규칙은 권고, 훅의 규칙은 구조적." "AGENTS.md=가이드라인, git 훅=강제. 둘 다 필요."**

**치명 함정 — 에이전트의 훅 우회**: `git commit --no-verify` / `-n` / `git stash` / quiet 플래그. (기록: Claude Opus가 deny 규칙+CLAUDE.md 무시하고 6커밋 연속 우회.) **방어(효과순)**:
1. **PATH-shim `git` 래퍼**(`~/bin/git` 또는 repo `bin/`) — `--no-verify`/`-n` 감지 시 차단. *어느 에이전트든* 막는 가장 범용 수단.
2. **PreToolUse 훅**으로 `--no-verify` 명령 차단(Claude/Codex 공통).
3. **CI 백스톱** — `pre-commit run --all-files`(또는 우리 check.sh) on push/PR. 로컬 우회 전부 포획.
4. permissions.deny(prefix 매칭 한계) · AGENTS.md 문구(권고).
- **pre-commit 프레임워크**가 표준 툴 무관 훅 매니저(언어 무관·버전관리 공유).

## 4. 도출된 범용 하네스 아키텍처 (3층)

```
① 툴 무관 코어 (AI 툴 없어도 작동)
   AGENTS.md(설계-우선 프로토콜+규칙, ≤32KiB)
   + git 훅(pre-commit/pre-push → 결정론 게이트 check.sh)
   + git 래퍼(--no-verify 방어)
   + CI 백스톱(harness-gate)
② 툴별 훅 어댑터 (같은 .sh/config 재사용, 더 풍부)
   .claude/settings.json (Edit|Write matcher)  ·  .codex/hooks.json (apply_patch matcher, 적대자=codex exec command 훅)
③ 의미 적대자 (검증)
   review.sh — LLM CLI 자동감지(codex exec / claude -p) + CI 스텝
```

**왜 이게 정답인가**: 우리 16실험 결론 = *값은 "미리 알려주기(주입)"가 아니라 "검증·강제"에*. 결정론 게이트를 **git훅+CI**로 깔면 *어느 툴/사람이 고치든* 발동하고(주입처럼 모델 자율성에 안 기댐), 적대자는 LLM CLI 무관하게 호출. AGENTS.md는 31개+ 툴이 읽는 단일 진실원.

## 출처

- Codex Hooks: https://developers.openai.com/codex/hooks
- Codex AGENTS.md 가이드: https://developers.openai.com/codex/guides/agents-md
- Codex Config Reference: https://developers.openai.com/codex/config-reference
- Codex Changelog(최신 0.139.0): https://developers.openai.com/codex/changelog
- AGENTS.md 표준: https://agents.md/
- AGENTS.md PR/거버넌스 정리: https://prpm.dev/blog/agents-md-deep-dive
- git 훅 우회 방어: https://pydevtools.com/handbook/how-to/how-to-stop-ai-agents-from-bypassing-pre-commit-hooks/
- git 훅 가드레일: https://jonesrussell.github.io/blog/git-hooks-ai-agents/ · https://dev.to/wilddog64/i-built-the-guardrails-into-the-repo-not-the-prompt-4n3l
- AGENTS.md vs Cursor rules(모범 패턴): https://www.mdfile.exchange/compare/agents-md-vs-cursor-rules
- Codex 훅 PR(#11067): https://github.com/openai/codex/pull/11067
