#!/usr/bin/env bash
#
# install-to.sh — 이 repo의 하네스를 *다른 로컬 프로젝트*에 한 줄로 설치.
#   사용:  bash scripts/harness/install-to.sh /path/to/new-project
# (원격/다른 머신이면 pack-harness.sh 로 tarball 만들어 옮긴다.)
#
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET="${1:?사용법: bash scripts/harness/install-to.sh <타깃 프로젝트 경로>}"
[[ -d "$TARGET" ]] || { echo "타깃 디렉토리 없음: $TARGET" >&2; exit 1; }

echo "🧭 하네스 설치 → $TARGET"
mkdir -p "$TARGET/scripts/harness" "$TARGET/.claude/agents" "$TARGET/.github/workflows"

# 메커니즘만 (eval/·findings.jsonl·연구 문서 제외)
for f in "$SCRIPT_DIR"/*.sh \
         "$SCRIPT_DIR/harness.config.json" "$SCRIPT_DIR/review-protocol.md" \
         "$SCRIPT_DIR/README.md" "$SCRIPT_DIR/DEPLOY_HARNESS.md"; do
  [[ -f "$f" ]] && cp "$f" "$TARGET/scripts/harness/"
done
# 워크플로우(수동 오케스트레이션)는 workflows/ 하위로
mkdir -p "$TARGET/scripts/harness/workflows"
cp "$SCRIPT_DIR"/workflows/*.workflow.js "$TARGET/scripts/harness/workflows/" 2>/dev/null || true
cp "$REPO_ROOT/.claude/agents/"*.md "$TARGET/.claude/agents/" 2>/dev/null || true
chmod +x "$TARGET"/scripts/harness/*.sh 2>/dev/null || true

# 기존 settings.json 은 덮지 않는다(타깃 훅/권한 보존)
if [[ -f "$TARGET/.claude/settings.json" ]]; then
  cp "$REPO_ROOT/.claude/settings.json" "$TARGET/.claude/settings.harness.json"
  echo "⚠️ 기존 .claude/settings.json 보존 → settings.harness.json (그 hooks 블록을 직접 병합)"
else
  cp "$REPO_ROOT/.claude/settings.json" "$TARGET/.claude/settings.json"
fi
[[ -f "$REPO_ROOT/.github/workflows/harness-gate.yml" ]] && cp "$REPO_ROOT/.github/workflows/harness-gate.yml" "$TARGET/.github/workflows/"

echo "✅ 설치 완료. 이제 단 둘:"
echo "   1) 그 프로젝트의 Claude Code에:  \"DEPLOY_HARNESS.md대로 harness.config.json 채워줘\""
echo "   2) /hooks 한 번 열기 (또는 재시작)"
