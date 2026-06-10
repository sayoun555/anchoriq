#!/usr/bin/env bash
#
# codex-hook.sh — Codex CLI 훅 어댑터. Codex 훅 JSON(stdin)을 받아 하네스를 돌리고
#   Codex 훅 출력 JSON(decision/additionalContext)을 낸다.
#   PostToolUse(편집마다) = 결정론 게이트(check.sh, 빠름)
#   Stop(턴마다)          = 결정론 + *의미 적대자*(review.sh→LLM) — 측정된 핵심 가치
#
# 재귀 가드: review.sh가 codex exec를 부르면 그 내부 codex가 자기 Stop 훅에서 또 review를
#   부르는 무한 재귀가 난다. review.sh가 LLM 호출에 HARNESS_REVIEWING=1 을 심고, 여기서
#   그걸 보면 즉시 no-op → 재귀 차단.
#
# 사용:  codex-hook.sh post   |   codex-hook.sh stop
#
set -uo pipefail

# ── 재귀 가드 (최우선) ──
if [[ "${HARNESS_REVIEWING:-0}" == "1" ]]; then echo '{"continue":true}'; exit 0; fi

MODE="${1:-post}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || pwd)"
cat >/dev/null 2>&1 || true   # stdin(훅 JSON) 비움 — 작업트리 기준으로 검사하므로 파싱 불필요

DIR=""
for d in .harness harness-universal scripts/harness-universal; do
  [[ -f "$ROOT/$d/check.sh" ]] && { DIR="$ROOT/$d"; break; }
done
[[ -z "$DIR" ]] && { echo '{"continue":true}'; exit 0; }
cd "$ROOT" || { echo '{"continue":true}'; exit 0; }

# 작업트리의 변경 소스(수정 + 신규 untracked)
changed() { { git diff --name-only --diff-filter=ACM; git ls-files --others --exclude-standard; } 2>/dev/null | sort -u; }

emit_block() {  # $1 = additionalContext
  jq -nc --arg c "$1" '{decision:"block", reason:"하네스 위반 — 수정 필요", hookSpecificOutput:{hookEventName:"PostToolUse", additionalContext:$c}}' 2>/dev/null \
    || printf '{"decision":"block","reason":"harness violation"}\n'
}
emit_surface() {  # $1 = additionalContext (비차단, 모델에 보이게)
  jq -nc --arg c "$1" '{continue:true, hookSpecificOutput:{additionalContext:$c}}' 2>/dev/null || printf '{"continue":true}\n'
}

if [[ "$MODE" == "post" ]]; then
  CH="$(changed)"; [[ -z "$CH" ]] && { echo '{"continue":true}'; exit 0; }
  OUT="$(bash "$DIR/check.sh" $CH 2>&1)"; rc=$?
  [[ $rc -ne 0 ]] && { emit_block "⛔ 결정론 게이트 실패(커밋 전 수정):\n$OUT"; exit 0; }
  echo '{"continue":true}'; exit 0
fi

# ── Stop: 결정론 + 의미 적대자 ──
OUT_C="$(bash "$DIR/check.sh" --all 2>&1)"; rc_c=$?
CH="$(changed)"
rc_r=0; OUT_R=""; review_violation=0
if [[ -n "$CH" && -f "$DIR/review.sh" ]]; then
  OUT_R="$(bash "$DIR/review.sh" $CH 2>&1)"; rc_r=$?
  echo "$OUT_R" | grep -qiE '\[(high|HIGH|med|MED|critical)\]|위반 있음|violation' && review_violation=1
fi

CTX=""
[[ $rc_c -ne 0 ]] && CTX="⛔ 결정론 게이트:\n$OUT_C\n"
[[ $review_violation -eq 1 ]] && CTX="${CTX}🔎 의미 적대자(review):\n$OUT_R\n"

if [[ $rc_c -ne 0 || $rc_r -ne 0 ]]; then          # 결정론 실패 OR review 차단모드 → 하드 block
  emit_block "$CTX"; exit 0
elif [[ $review_violation -eq 1 ]]; then           # review 위반(기본 비차단) → 표면화(루프 방지)
  emit_surface "$CTX"; exit 0
fi
echo '{"continue":true}'; exit 0
