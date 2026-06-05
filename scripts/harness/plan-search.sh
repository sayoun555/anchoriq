#!/usr/bin/env bash
#
# plan-search.sh — AnchorIQ 하네스 설계문서 검색/섹션 추출 (컨텍스트 최적화)
#
# 긴 plan/ 문서를 통째로 컨텍스트에 올리면 "중간 소실(lost-in-the-middle)"이 생긴다.
# 통째 읽기 대신 (1) 목차만, (2) 한 섹션만, (3) 질의-관련 청크만 가져온다.
#
# 사용:
#   plan-search.sh toc <doc>                # 문서의 헤더(목차)만
#   plan-search.sh section <doc> <키워드>   # 키워드 헤더의 섹션만 (하위 절 포함)
#   plan-search.sh grep <질의>              # plan/ 전체에서 관련 라인 검색
#
# <doc>는 "ARCHITECTURE.md" / "plan/ARCHITECTURE.md" / 절대경로 모두 허용.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PLAN_DIR="$REPO_ROOT/plan"

resolve_doc() {
  local d="$1"
  if [[ -f "$d" ]]; then echo "$d"; return 0; fi
  if [[ -f "$PLAN_DIR/$d" ]]; then echo "$PLAN_DIR/$d"; return 0; fi
  if [[ -f "$PLAN_DIR/${d#plan/}" ]]; then echo "$PLAN_DIR/${d#plan/}"; return 0; fi
  return 1
}

cmd="${1:-}"; shift || true

case "$cmd" in
  toc)
    doc="$(resolve_doc "${1:-}")" || { echo "plan-search: 문서 못 찾음: ${1:-}" >&2; exit 2; }
    grep -nE '^#{1,6} ' "$doc" || echo "(헤더 없음)"
    ;;

  section)
    doc="$(resolve_doc "${1:-}")" || { echo "plan-search: 문서 못 찾음: ${1:-}" >&2; exit 2; }
    kw="${2:-}"
    [[ -z "$kw" ]] && { echo "plan-search section: <키워드> 필요" >&2; exit 2; }
    out="$(awk -v kw="$kw" '
      function hlevel(s){ if (match(s, /^#+/)) return RLENGTH; return 0 }
      BEGIN { lk = tolower(kw); inSec=0; startLvl=0 }
      {
        lvl = hlevel($0)
        if (lvl > 0) {
          if (inSec && lvl <= startLvl) inSec = 0
          if (!inSec && index(tolower($0), lk) > 0) { inSec = 1; startLvl = lvl }
        }
        if (inSec) print
      }
    ' "$doc")"
    if [[ -z "$out" ]]; then
      echo "(\"$kw\" 헤더 못 찾음 — 목차 확인: plan-search.sh toc $(basename "$doc"))" >&2
      exit 1
    fi
    printf '%s\n' "$out"
    ;;

  grep)
    q="${1:-}"
    [[ -z "$q" ]] && { echo "plan-search grep: <질의> 필요" >&2; exit 2; }
    res="$(grep -rIn -i --include="*.md" -- "$q" "$PLAN_DIR" 2>/dev/null | sed "s#$REPO_ROOT/##" | head -40)"
    if [[ -z "$res" ]]; then echo "(\"$q\" plan/에서 못 찾음)"; else printf '%s\n' "$res"; fi
    ;;

  *)
    echo "plan-search.sh: toc <doc> | section <doc> <키워드> | grep <질의>" >&2
    exit 2
    ;;
esac
