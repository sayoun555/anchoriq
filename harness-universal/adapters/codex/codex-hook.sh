#!/usr/bin/env bash
#
# codex-hook.sh — Codex CLI 훅 어댑터. stdin의 Codex 훅 JSON을 받아 check.sh를 돌리고
#   Codex 훅 출력 JSON(decision/additionalContext)을 낸다. (apply_patch 후 검증용)
#   Codex는 agent/prompt 훅이 없고 command만 → 이 스크립트가 그 다리.
#   의미 적대자(review.sh, LLM)는 무거워 매 편집엔 안 돌리고 pre-push/CI/수동에.
#
# 사용(hooks.json에서):  codex-hook.sh post   |   codex-hook.sh stop
#
set -uo pipefail
MODE="${1:-post}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || pwd)"
cat >/dev/null 2>&1 || true   # stdin 비우기(JSON은 안 파싱; 작업트리 기준으로 검사 — tool_input 모양 불문)

CHECK=""
for d in .harness harness-universal scripts/harness-universal; do
  [[ -f "$ROOT/$d/check.sh" ]] && { CHECK="$ROOT/$d/check.sh"; break; }
done
[[ -z "$CHECK" ]] && { echo '{"continue":true}'; exit 0; }

cd "$ROOT" || { echo '{"continue":true}'; exit 0; }

if [[ "$MODE" == "stop" ]]; then
  OUT="$(bash "$CHECK" --all 2>&1)"; rc=$?
else
  # post: 방금 편집(apply_patch)으로 바뀐 작업트리 소스 — *수정 + 신규(untracked)* 둘 다
  CHANGED="$( { git diff --name-only --diff-filter=ACM; git ls-files --others --exclude-standard; } 2>/dev/null | sort -u )"
  if [[ -z "$CHANGED" ]]; then echo '{"continue":true}'; exit 0; fi
  OUT="$(bash "$CHECK" $CHANGED 2>&1)"; rc=$?
fi

if [[ $rc -ne 0 ]]; then
  # Codex/Claude 공통 차단 출력: decision=block + 모델 가시 컨텍스트
  jq -nc --arg c "$OUT" '{decision:"block", reason:"하네스 결정론 게이트 실패", hookSpecificOutput:{hookEventName:"PostToolUse", additionalContext:("⛔ 하네스 check 실패 — 커밋 전 수정 필요:\n"+$c)}}' 2>/dev/null \
    || printf '{"decision":"block","reason":"harness check failed"}\n'
  exit 0
fi
echo '{"continue":true}'
exit 0
