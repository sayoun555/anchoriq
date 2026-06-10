#!/usr/bin/env bash
#
# pack-harness.sh — 하네스 *메커니즘*을 단일 tarball로 패키징(다른 프로젝트에 한 줄 설치).
#
# 묶는 것: 스크립트·워크플로우·config·적대자 프로토콜·.claude(settings+agents)·CI.
# 빼는 것(연구·런타임): eval/·findings.jsonl·docs/harness-engineering·docs/papers.
#   → "메커니즘은 배포, 연구는 홈에" 원칙(COMPOSITION/DEPLOY 참조).
#
# 사용:  bash scripts/harness/pack-harness.sh            → dist/harness-bundle.tar.gz 생성
# 설치(타깃 repo에서):  tar xzf harness-bundle.tar.gz && bash harness-bundle/install.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUT_DIR="$REPO_ROOT/dist"
STAGE="$(mktemp -d)"
BUNDLE="$STAGE/harness-bundle"
trap 'rm -rf "$STAGE"' EXIT

mkdir -p "$BUNDLE/scripts/harness" "$BUNDLE/.claude/agents" "$BUNDLE/.github/workflows"
cd "$REPO_ROOT"

# 1) 메커니즘만 복사 (eval/·findings.jsonl 제외)
for f in scripts/harness/*.sh \
         scripts/harness/harness.config.json scripts/harness/review-protocol.md \
         scripts/harness/README.md scripts/harness/DEPLOY_HARNESS.md; do
  [[ -f "$f" ]] && cp "$f" "$BUNDLE/scripts/harness/"
done
# 워크플로우(수동 오케스트레이션)는 workflows/ 하위로
mkdir -p "$BUNDLE/scripts/harness/workflows"
cp scripts/harness/workflows/*.workflow.js "$BUNDLE/scripts/harness/workflows/" 2>/dev/null || true
cp .claude/settings.json "$BUNDLE/.claude/settings.json"
cp .claude/agents/*.md "$BUNDLE/.claude/agents/" 2>/dev/null || true
[[ -f .github/workflows/harness-gate.yml ]] && cp .github/workflows/harness-gate.yml "$BUNDLE/.github/workflows/"

# 2) 번들 내 install.sh (타깃 repo에서 실행)
cat > "$BUNDLE/install.sh" <<'INSTALL'
#!/usr/bin/env bash
# install.sh — 이 하네스 번들을 현재(또는 인자로 준) 프로젝트에 설치한다.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-$(pwd)}"
[[ -d "$TARGET" ]] || { echo "타깃 디렉토리 없음: $TARGET" >&2; exit 1; }
echo "🧭 하네스 설치 → $TARGET"

mkdir -p "$TARGET/scripts/harness" "$TARGET/.claude/agents" "$TARGET/.github/workflows"
cp -R "$HERE"/scripts/harness/* "$TARGET/scripts/harness/"   # -R: workflows/ 하위까지
cp "$HERE"/.claude/agents/*.md "$TARGET/.claude/agents/" 2>/dev/null || true
chmod +x "$TARGET"/scripts/harness/*.sh 2>/dev/null || true

# settings.json — 기존을 덮지 않는다(그쪽 훅/권한 보존)
if [[ -f "$TARGET/.claude/settings.json" ]]; then
  cp "$HERE/.claude/settings.json" "$TARGET/.claude/settings.harness.json"
  echo "⚠️  기존 .claude/settings.json 발견 → settings.harness.json 으로 뒀다. 그 파일의 \"hooks\" 블록을 기존 settings.json에 직접 병합하라(덮어쓰기 금지)."
else
  cp "$HERE/.claude/settings.json" "$TARGET/.claude/settings.json"
fi
[[ -f "$HERE/.github/workflows/harness-gate.yml" ]] && cp "$HERE/.github/workflows/harness-gate.yml" "$TARGET/.github/workflows/"

cat <<'NEXT'

✅ 파일 복사 완료. 남은 3단계:
  1) scripts/harness/harness.config.json 을 이 프로젝트에 맞게 수정
     (designDocsDir · source · build.compileCommand · governingDoc.map — 제일 중요)
  2) scripts/harness/review-protocol.md(적대자 규칙) + 설계폴더의 AGENTS.md 를 이 프로젝트 설계로
  3) Claude Code에서 /hooks 한 번 열거나 재시작 → 훅 라이브

검증:  echo '{"tool_input":{"file_path":"<이 repo의 파일>"}}' | bash scripts/harness/governing-doc.sh
자세히: scripts/harness/DEPLOY_HARNESS.md
NEXT
INSTALL
chmod +x "$BUNDLE/install.sh"

# 3) 최상단 안내
cat > "$BUNDLE/USAGE.txt" <<'U'
하네스 번들 — 다른 프로젝트에 설치
  tar xzf harness-bundle.tar.gz
  bash harness-bundle/install.sh /path/to/your-project   # 생략 시 현재 디렉토리
그다음: harness.config.json 수정 + /hooks. 상세: scripts/harness/DEPLOY_HARNESS.md
U

# 4) tarball
mkdir -p "$OUT_DIR"
TAR="$OUT_DIR/harness-bundle.tar.gz"
tar -C "$STAGE" -czf "$TAR" harness-bundle
echo "✅ 패키지 생성: $TAR ($(du -h "$TAR" | cut -f1))"
echo "   설치(타깃 repo에서): tar xzf harness-bundle.tar.gz && bash harness-bundle/install.sh [타깃경로]"
