#!/usr/bin/env bash
#
# governing-doc.sh — 하네스 컨텍스트 라우터 (PreToolUse: Edit|Write)
#
# "설계 문서 학습 의무"를 산문이 아니라 결정론적 주입으로. 수정 파일 경로를 분석해
# 그 파일을 지배하는 설계 문서를 한 줄 포인터로 컨텍스트에 주입한다(non-blocking).
# 매핑(routable 글롭·docMap·확장자별 always)은 harness.config.json(.governingDoc) — 이식성.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CFG="$SCRIPT_DIR/harness.config.json"

INPUT="$(cat)"
FILE="$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // empty')"
[[ -z "$FILE" ]] && exit 0
[[ -f "$CFG" ]] || exit 0   # config 없으면 라우팅 생략(하네스는 config 전제)

# 1) routable 글롭만 라우팅 — 문서 파일명에 키워드가 우연히 들어가 오탐하는 것 방지
#    (troubleshooting/GOVERNING_DOC_OVERMATCHES_DOCS.md)
ROUTABLE=0
while IFS= read -r g; do
  [[ -n "$g" && "$FILE" == $g ]] && { ROUTABLE=1; break; }
done < <(jq -r '.governingDoc.routableGlobs[]?' "$CFG" 2>/dev/null)
[[ $ROUTABLE -eq 1 ]] || exit 0

docs=()

# 2) docMap — 첫 매치 엔트리의 docs (bash case의 first-match-wins 재현)
#    map 전체를 *단일 jq*로 스트리밍해 엔트리당 jq 2회(최대 20회)를 1회로 줄임 (A7).
MATCHED=0; cur_globs=(); cur_docs=()
check_entry() {  # 누적된 한 엔트리를 FILE에 매치 — 첫 매치만 docs에 반영
  (( MATCHED == 1 )) && return 0
  (( ${#cur_globs[@]} == 0 )) && return 0
  local g
  for g in "${cur_globs[@]}"; do
    if [[ "$FILE" == $g ]]; then
      MATCHED=1
      (( ${#cur_docs[@]} > 0 )) && docs+=("${cur_docs[@]}")
      return 0
    fi
  done
  return 0
}
while IFS= read -r line; do
  case "$line" in
    "@@ENTRY@@") check_entry; cur_globs=(); cur_docs=() ;;
    G$'\t'*) cur_globs+=("${line#G$'\t'}") ;;
    D$'\t'*) cur_docs+=("${line#D$'\t'}") ;;
  esac
done < <(jq -r '.governingDoc.map[] | "@@ENTRY@@", (.globs[] | "G\t" + .), (.docs[] | "D\t" + .)' "$CFG" 2>/dev/null)
check_entry  # 마지막 엔트리

# 3) 확장자별 always 문서 (예: .java → AGENTS.md 는 항상)
EXT="${FILE##*.}"
while IFS= read -r d; do
  [[ -n "$d" ]] && docs+=("$d")
done < <(jq -r --arg ext "$EXT" '.governingDoc.alwaysForExtension[$ext][]?' "$CFG" 2>/dev/null)

[[ ${#docs[@]} -eq 0 ]] && exit 0

# 중복 제거 후 " · " 결합 (paste -d 는 멀티문자 구분자 불가 → awk)
JOINED="$(printf '%s\n' "${docs[@]}" | awk '!seen[$0]++{a[++n]=$0} END{for(i=1;i<=n;i++) printf "%s%s",(i>1?" · ":""),a[i]}')"
MSG="📐 설계 준수: 이 파일의 지배 문서 → ${JOINED}. 통째로 읽지 말고 §표시된 절만 가져와라(중간 소실 방지): scripts/harness/plan-search.sh section <문서> '<§키워드>'  ·  넓게 찾을 땐 plan-search.sh grep '<질의>'. (CLAUDE.md 학습 의무)"

jq -cn --arg ctx "$MSG" \
  '{hookSpecificOutput:{hookEventName:"PreToolUse", additionalContext:$ctx}}'
exit 0
