#!/usr/bin/env bash
#
# session-context.sh — SessionStart 연속성 주입 (툴 무관).
#   harness.config.json의 state.progressFile(기본 progress.md)을 다음 세션에 재주입한다.
#   긴/다세션 작업에서 컨텍스트가 끊겨도 "어디까지 했는지"를 이어가게(과거 세션 상태는
#   코드·문서로 추론 불가 → 주입이 값 나는 비추론 경계).
#   Codex/Claude의 SessionStart 훅이 이 스크립트를 부른다(둘 다 같은 JSON 스키마).
#
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || pwd)"
CONFIG="$SCRIPT_DIR/harness.config.json"

PROGRESS_FILE="progress.md"
if [[ -f "$CONFIG" ]] && command -v jq >/dev/null 2>&1; then
  pf="$(jq -r '.state.progressFile // "progress.md"' "$CONFIG" 2>/dev/null)"
  [[ -n "$pf" && "$pf" != "null" ]] && PROGRESS_FILE="$pf"
fi

# 진행 상태 없으면 주입 안 함(no-op) — 짧은 작업엔 무해
if [[ ! -s "$ROOT/$PROGRESS_FILE" ]]; then echo '{"continue":true}'; exit 0; fi

read -r -d '' CTX <<EOF || true
🧭 하네스 연속성 — 이전 세션 핸드오프 (${PROGRESS_FILE}):
$(sed 's/^/  /' "$ROOT/$PROGRESS_FILE" | head -50)

→ 작업하며 ${PROGRESS_FILE}를 갱신하라(완료 / 남은 일 / 막힌 점 / 다음 단계). 세션이 끊겨도 이어가게.
(설계 규칙·footgun 등 정적 지침은 AGENTS.md 참조. 이 주입은 *세션 간 상태*만.)
EOF

command -v jq >/dev/null 2>&1 \
  && jq -cn --arg ctx "$CTX" '{hookSpecificOutput:{hookEventName:"SessionStart", additionalContext:$ctx}}' \
  || printf '{"continue":true}\n'
exit 0
