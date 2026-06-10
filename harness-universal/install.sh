#!/usr/bin/env bash
#
# install.sh — 범용 하네스를 타깃 프로젝트에 설치(툴 무관).
#   bash install.sh [<타깃 repo 경로>]   (생략 시 현재 디렉토리)
#
# 설치하는 것:
#   .harness/                         하네스 전체(check.sh·review.sh·config·protocol·어댑터·git래퍼)
#   git core.hooksPath → .harness/git-hooks   (pre-commit/pre-push 결정론 게이트)
#   AGENTS.md(없으면 템플릿 복사)       범용 지침(Codex·Cursor·Aider…가 읽음)
#   .github/workflows/harness-gate.yml CI 백스톱(없으면 복사)
# 안내(수동): git 래퍼 PATH · Codex/Claude 훅 어댑터 · footgun/config 교체
#
set -euo pipefail
SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-$(pwd)}"
TARGET="$(cd "$TARGET" && pwd)"
[[ -d "$TARGET/.git" ]] || { echo "⚠️ 타깃이 git repo 아님($TARGET) — git 훅 강제가 안 된다. 계속하려면 git init 먼저." >&2; }

echo "🧭 범용 하네스 → $TARGET"

# 1) .harness/ 복사 (eval·findings·연구 제외 — 메커니즘만)
mkdir -p "$TARGET/.harness"
for item in check.sh review.sh harness.config.json review-protocol.md AGENTS.md README.md CODEX_SETUP.md CLAUDE_SETUP.md git-hooks bin adapters ci; do
  [[ -e "$SRC/$item" ]] && cp -R "$SRC/$item" "$TARGET/.harness/"
done
chmod +x "$TARGET/.harness/"*.sh "$TARGET/.harness/git-hooks/"* "$TARGET/.harness/bin/git" "$TARGET/.harness/adapters/codex/"*.sh 2>/dev/null || true

# 2) git 훅 배선 (core.hooksPath) — 기존 커스텀 훅 있으면 보존+안내
if [[ -d "$TARGET/.git" ]]; then
  EXIST_HP="$(git -C "$TARGET" config --local core.hooksPath || true)"
  if [[ -n "$EXIST_HP" && "$EXIST_HP" != ".harness/git-hooks" ]]; then
    echo "⚠️ 기존 core.hooksPath='$EXIST_HP' 발견 → 덮지 않음. .harness/git-hooks/{pre-commit,pre-push}를 그쪽에 체이닝하라."
  else
    git -C "$TARGET" config --local core.hooksPath .harness/git-hooks
    echo "✅ git core.hooksPath → .harness/git-hooks"
  fi
fi

# 3) AGENTS.md — 없으면 템플릿, 있으면 보존+안내
if [[ -f "$TARGET/AGENTS.md" ]]; then
  echo "⚠️ 기존 AGENTS.md 보존 → .harness/AGENTS.md 의 §2(강제) 블록을 병합하라."
else
  cp "$SRC/AGENTS.md" "$TARGET/AGENTS.md"
  echo "✅ AGENTS.md 템플릿 복사 (« » 부분 채울 것)"
fi

# 4) CI 백스톱 — 없으면 복사
if [[ -f "$TARGET/.github/workflows/harness-gate.yml" ]]; then
  echo "ℹ️ harness-gate.yml 이미 있음 — 건너뜀"
else
  mkdir -p "$TARGET/.github/workflows"
  cp "$SRC/ci/harness-gate.yml" "$TARGET/.github/workflows/harness-gate.yml"
  echo "✅ CI 백스톱 복사"
fi

cat <<NEXT

✅ 코어 설치 완료. 남은 단계(수동, 중요도순):

  1) harness.config.json 을 이 프로젝트에 맞게  (.harness/harness.config.json)
     - source(언어·경로) · build.compileCommand · rules(stub/secret 패턴)
     - ★ review.footguns ← *스택 전용*이라 반드시 교체 (현재 Java/Spring 예시)
     - governing.map(파일→설계문서)
     자세히: .harness/README.md · DEPLOY 절

  2) --no-verify 우회 방어 (선택, 강력):  PATH 앞에 .harness/bin 추가
       export PATH="$TARGET/.harness/bin:\$PATH"     # git 래퍼가 --no-verify 차단

  3) 에디터 훅(편집 직후 검사 — 선택):
     - Codex:  cp .harness/adapters/codex/hooks.json .codex/hooks.json   (matcher=apply_patch)
     - Claude Code:  .harness/adapters/claude/settings.json 의 hooks 블록을 .claude/settings.json 에 병합 → /hooks

  3층 구조: AGENTS.md(지침) · git훅+CI(결정론 강제) · review.sh(의미 적대자, LLM 자동감지).
  검증:  echo 'TODO: x' > /tmp/t.java; bash .harness/check.sh /tmp/t.java   # stub 잡히면 OK
NEXT
