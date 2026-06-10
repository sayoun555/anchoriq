#!/usr/bin/env bash
#
# review.sh — 의미 적대자(검증). 변경 파일을 review-protocol.md 기준으로 LLM에 검토시킨다.
#   LLM CLI 자동감지: codex(exec) → claude(-p) → 없으면 안내만.
#   결정론 게이트(check.sh)가 못 잡는 DDD·트랜잭션·footgun 등 *의미* 위반용.
#
# 사용:  review.sh [<file>...]   (없으면 staged 변경 파일)
#   환경: HARNESS_REVIEW_BLOCK=1 이면 위반 발견 시 exit 1 (기본은 보고만, exit 0)
#         HARNESS_LLM=codex|claude 로 강제 지정 가능
#
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || pwd)"
PROTO="$SCRIPT_DIR/review-protocol.md"
CONFIG="$SCRIPT_DIR/harness.config.json"
cd "$REPO_ROOT" || exit 0

# 1) LLM CLI 감지
LLM="${HARNESS_LLM:-}"
if [[ -z "$LLM" ]]; then
  command -v codex >/dev/null 2>&1 && LLM=codex
  [[ -z "$LLM" ]] && command -v claude >/dev/null 2>&1 && LLM=claude
fi
if [[ -z "$LLM" ]]; then
  echo "ℹ️ review.sh: LLM CLI(codex/claude) 없음 → 의미 검토 건너뜀."
  echo "   설치 후 또는 CI에서: review-protocol.md로 변경 파일을 검토하라."
  exit 0
fi

# 2) 대상 파일
FILES=()
if [[ $# -gt 0 ]]; then FILES=("$@"); else
  while IFS= read -r f; do [[ -f "$f" ]] && FILES+=("$f"); done < <(git diff --cached --name-only --diff-filter=ACM 2>/dev/null)
fi
[[ ${#FILES[@]} -eq 0 ]] && { echo "✅ review: 변경 파일 없음"; exit 0; }

# 3) 프롬프트 구성 (프로토콜 + config footgun + 변경 파일)
TMP="$(mktemp)"; trap 'rm -f "$TMP"' EXIT
{
  echo "너는 PostToolUse 의미 적대자다. 아래 프로토콜과 footgun 체크리스트로 변경 파일을 검토하라."
  echo "위반이 있으면 [심각도] 파일:정황 — 무엇이 왜 틀렸고 정석은. 없으면 '위반 없음'. 스타일 취향은 위반 아님; 확신 없으면 통과."
  echo; echo "=== 검토 프로토콜 ==="; cat "$PROTO" 2>/dev/null
  if [[ -f "$CONFIG" ]] && command -v jq >/dev/null 2>&1; then
    echo; echo "=== 이 스택의 footgun (컴파일은 되나 런타임에 깨짐 — 각각 점검) ==="
    jq -r '.review.footguns[]? | "- [\(.sev)] \(.id): \(.check)"' "$CONFIG" 2>/dev/null
  fi
  echo; echo "=== 검토 대상 파일 ==="
  for f in "${FILES[@]}"; do echo; echo "----- $f -----"; sed 's/^/  /' "$f" 2>/dev/null; done
} > "$TMP"

echo "🔎 review.sh: $LLM 로 ${#FILES[@]}개 파일 의미 검토 중..."
case "$LLM" in
  codex)  OUT="$(codex exec - < "$TMP" 2>&1)" || OUT="$(codex exec "$(cat "$TMP")" 2>&1)" ;;
  claude) OUT="$(claude -p < "$TMP" 2>&1)" ;;
esac
echo "$OUT"

if echo "$OUT" | grep -qiE '위반 없음|no violation|위반 없'; then exit 0; fi
if echo "$OUT" | grep -qE '\[(high|HIGH|med|MED|critical)\]|위반 있음|violation'; then
  echo "⚠️ review: 의미 위반 보고됨(위 참조)."
  [[ "${HARNESS_REVIEW_BLOCK:-0}" == "1" ]] && exit 1
fi
exit 0
