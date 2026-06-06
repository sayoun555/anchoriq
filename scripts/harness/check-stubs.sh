#!/usr/bin/env bash
#
# check-stubs.sh — AnchorIQ 하네스 품질 게이트 (anti-fabrication)
#
# AGENTS.md 규칙 10("정석적 해결 — 임시방편/꼼수/우회 금지")을 결정론적으로 강제한다.
# 백엔드 main 소스에 stub/TODO/mock/placeholder 마커가 있으면 "미완성"으로 간주.
#
# 두 가지 모드:
#   check-stubs.sh           → 사람/CI 모드. 발견 시 목록 출력 + exit 1 (pre-commit/CI 게이트용)
#   check-stubs.sh --hook    → Stop 훅 모드. stdin으로 훅 JSON을 받아, 발견 시
#                              {"decision":"block","reason":...} 를 출력해 에이전트를 다시 깨운다.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CFG="$SCRIPT_DIR/harness.config.json"
cfg() { jq -r "$1" "$CFG" 2>/dev/null; }

# 프로젝트 전용값은 harness.config.json 에서 (이식성 — DEPLOY_HARNESS.md). 없으면 AnchorIQ 기본.
SOURCE_ROOT="$(cfg '.source.root // "backend"')"
MAIN_GLOBS=(); while IFS= read -r _l; do [[ -n "$_l" ]] && MAIN_GLOBS+=("$_l"); done < <(cfg '.source.mainGlobs[]?')
[[ ${#MAIN_GLOBS[@]} -eq 0 ]] && MAIN_GLOBS=("anchoriq-*/src/main/java")
EXTS=(); while IFS= read -r _l; do [[ -n "$_l" ]] && EXTS+=("$_l"); done < <(cfg '.source.extensions[]?')
[[ ${#EXTS[@]} -eq 0 ]] && EXTS=("java")
PATTERN="$(cfg '.rules.stubMarkerPattern // empty')"
[[ -z "$PATTERN" || "$PATTERN" == "null" ]] && PATTERN='(//|/\*|\*)[[:space:]]*(TODO|FIXME|XXX|HACK|stub|STUB|mock|MOCK|placeholder|simulate|임시|미구현|추후|일단)|throw new UnsupportedOperationException|throw new RuntimeException\("Not implemented'

scan() {
  # config의 root+globs+extensions 로 main 소스 스캔(test 제외). 매치 라인을 stdout으로.
  local includes=() dirs=() e g d
  for e in "${EXTS[@]}"; do includes+=(--include="*.$e"); done
  for g in "${MAIN_GLOBS[@]}"; do
    for d in "$REPO_ROOT/$SOURCE_ROOT/"$g/; do [[ -d "$d" ]] && dirs+=("$d"); done
  done
  [[ ${#dirs[@]} -eq 0 ]] && return 0
  grep -rIn -E "$PATTERN" "${includes[@]}" "${dirs[@]}" 2>/dev/null || true
}

MODE="${1:-ci}"

if [[ "$MODE" == "--hook" ]]; then
  # ── Stop 훅 모드 ───────────────────────────────────────────────
  INPUT="$(cat)"
  # 이미 Stop 훅 때문에 재개 중이면 다시 블록하지 않는다 (무한루프 방지)
  if [[ "$(printf '%s' "$INPUT" | jq -r '.stop_hook_active // false')" == "true" ]]; then
    exit 0
  fi

  HITS="$(scan)"
  if [[ -z "$HITS" ]]; then
    exit 0   # 깨끗함 — 조용히 통과
  fi

  COUNT="$(printf '%s\n' "$HITS" | grep -c . || true)"
  # 리포 루트 기준 상대경로 + 라인만 추려서 메시지에 담는다
  LIST="$(printf '%s\n' "$HITS" | sed "s#$REPO_ROOT/##" | head -20)"

  MSG="⚠️ 하네스 품질 게이트(warn): 백엔드 main 소스에 미완성 마커 ${COUNT}건 (AGENTS.md 규칙 10: 임시방편 금지).
${LIST}"

  # ── 기본: WARN (non-blocking) ── 사용자에게만 표시, 턴을 막지 않는다.
  jq -cn --arg msg "$MSG" '{systemMessage:$msg}'

  # ── BLOCK 으로 승격하려면(트리가 마커-free 가 된 뒤 권장): 위 한 줄을 지우고 아래 주석을 해제.
  #   에이전트를 다시 깨워 마커를 실제 구현으로 채우게 강제하는 래칫이 된다.
  # jq -cn --arg reason "$MSG 종료 전 실제 구현으로 채워라." '{decision:"block", reason:$reason}'

  exit 0
fi

# ── 사람 / CI 모드 ────────────────────────────────────────────────
HITS="$(scan)"
if [[ -z "$HITS" ]]; then
  echo "✅ check-stubs: 백엔드 main 소스에 미완성 마커 없음."
  exit 0
fi
COUNT="$(printf '%s\n' "$HITS" | grep -c . || true)"
echo "❌ check-stubs: 미완성 마커 ${COUNT}건 발견 (AGENTS.md 규칙 10 위반)"
echo
printf '%s\n' "$HITS" | sed "s#$REPO_ROOT/##"
exit 1
