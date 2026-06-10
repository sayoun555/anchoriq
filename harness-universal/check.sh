#!/usr/bin/env bash
#
# check.sh — 결정론 게이트 (툴 무관). git 훅·CI·에디터 훅 어디서나 호출.
#   stub 마커·하드코딩 시크릿 = BLOCK(exit 1) · 파일 크기 = WARN.
#   의미 위반(DDD·footgun 등)은 review.sh(LLM 적대자)가 본다.
#
# 사용:
#   check.sh                 # staged 파일만 (pre-commit)
#   check.sh --all           # 전체 main 소스 (pre-push/CI)
#   check.sh <file>...       # 지정 파일 (에디터 훅)
#   환경: HARNESS_STRICT=1 이면 WARN도 BLOCK
#
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG="$SCRIPT_DIR/harness.config.json"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || exit 0

[[ -f "$CONFIG" ]] || { echo "⚠️ check.sh: harness.config.json 없음 ($CONFIG) — 통과(fail-open 회피 위해 설정 필수)"; exit 0; }
command -v jq >/dev/null 2>&1 || { echo "⚠️ check.sh: jq 필요(설치 후 재시도) — 통과"; exit 0; }

cfg() { jq -r "$1 // empty" "$CONFIG" 2>/dev/null; }

EXTS="$(cfg '.source.extensions | join("|")')"; [[ -z "$EXTS" ]] && EXTS="java|kt|ts|tsx|py|go|rb|rs|js"
SRC_ROOT="$(cfg '.source.root')"
STUB_RE="$(cfg '.rules.stubMarkerPattern')"
SECRET_RE="$(cfg '.rules.secretPattern')"
SECRET_EXC="$(cfg '.rules.secretExcludePattern')"
MAXLINES="$(cfg '.rules.maxFileLines')"; [[ -z "$MAXLINES" ]] && MAXLINES=400
# main 글롭(test 등 제외) — 비면 root 하위 전체
MAIN_GLOBS="$(jq -r '.source.mainGlobs[]? ' "$CONFIG" 2>/dev/null)"

is_source() {  # $1=path → 0이면 검사 대상 main 소스
  local f="$1" ext="${1##*.}"
  echo "$ext" | grep -qE "^($EXTS)$" || return 1
  case "$f" in *[Tt]est.* | *test/* | *tests/* | */__tests__/*) return 1 ;; esac
  [[ -n "$SRC_ROOT" ]] && case "$f" in "$SRC_ROOT"/*) : ;; *) return 1 ;; esac
  if [[ -n "$MAIN_GLOBS" ]]; then
    local ok=1 g
    while IFS= read -r g; do [[ -z "$g" ]] && continue; case "$f" in *$g* ) ok=0; break;; esac; done <<EOF
$MAIN_GLOBS
EOF
    [[ $ok -eq 0 ]] || return 1
  fi
  return 0
}

# --- 대상 파일 수집 ---
FILES=()
if [[ "${1:-}" == "--all" ]]; then
  while IFS= read -r f; do is_source "$f" && FILES+=("$f"); done < <(git ls-files 2>/dev/null)
elif [[ $# -gt 0 ]]; then
  for f in "$@"; do [[ -f "$f" ]] && is_source "$f" && FILES+=("$f"); done
else
  while IFS= read -r f; do [[ -f "$f" ]] && is_source "$f" && FILES+=("$f"); done < <(git diff --cached --name-only --diff-filter=ACM 2>/dev/null)
fi
[[ ${#FILES[@]} -eq 0 ]] && { echo "✅ check: 검사할 소스 변경 없음"; exit 0; }

block=0; warn=0
for f in "${FILES[@]}"; do
  # 1) stub/미완성 마커 — BLOCK
  if [[ -n "$STUB_RE" ]] && grep -nIE "$STUB_RE" "$f" >/tmp/.hk_stub 2>/dev/null; then
    echo "⛔ [stub] $f"; sed 's/^/      /' /tmp/.hk_stub | head -3; block=1
  fi
  # 2) 하드코딩 시크릿 — BLOCK
  if [[ -n "$SECRET_RE" ]]; then
    if grep -nIE "$SECRET_RE" "$f" 2>/dev/null | { [[ -n "$SECRET_EXC" ]] && grep -vE "$SECRET_EXC" || cat; } | grep -q .; then
      echo "⛔ [secret] $f — 하드코딩 의심(.env+환경변수로)"; block=1
    fi
  fi
  # 3) 파일 크기 — WARN
  n=$(wc -l < "$f" 2>/dev/null | tr -d ' '); [[ -n "$n" && "$n" -gt "$MAXLINES" ]] && { echo "⚠️ [size] $f ($n>$MAXLINES줄 — 분리 고려)"; warn=1; }
done
rm -f /tmp/.hk_stub

[[ $warn -eq 1 && "${HARNESS_STRICT:-0}" == "1" ]] && block=1
if [[ $block -eq 1 ]]; then
  echo "❌ check 실패 — 위 BLOCK 항목 수정 후 재커밋. (의미 검토는 review.sh)"; exit 1
fi
[[ $warn -eq 1 ]] && echo "✅ check 통과 (경고 있음)" || echo "✅ check 통과"
exit 0
