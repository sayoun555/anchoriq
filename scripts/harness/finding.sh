#!/usr/bin/env bash
#
# finding.sh — AnchorIQ 하네스 findings 원장 ("DLT", 채널 C)
#
# 적대자(의미 리뷰어)나 게이트가 찾은 설계 위반을 영속화한다.
# 열린(open) finding은 SessionStart 훅이 다음 세션에 다시 주입 → 닫힐 때까지 사라지지 않는다.
# Kafka DLT 비유: 정상 처리 못 한 메시지를 별도 토픽에 쌓아 사람이 처리하게 하는 것과 같다.
#
# 사용:
#   finding.sh add --file F --rule R [--sev high|med|low] --desc "..."   → 한 건 적재, id 출력
#   finding.sh list [--open|--all]                                       → 목록
#   finding.sh resolve <id> [--note "..."]                               → 해결 처리
#   finding.sh count-open                                                → 열린 건수
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LEDGER="$SCRIPT_DIR/findings.jsonl"
touch "$LEDGER"

now() { date -u +%Y-%m-%dT%H:%M:%SZ; }

cmd="${1:-list}"; shift || true

case "$cmd" in
  add)
    file=""; rule=""; sev="med"; desc=""
    while [[ $# -gt 0 ]]; do
      case "$1" in
        --file) file="$2"; shift 2;;
        --rule) rule="$2"; shift 2;;
        --sev)  sev="$2";  shift 2;;
        --desc) desc="$2"; shift 2;;
        *) shift;;
      esac
    done
    [[ -z "$desc" ]] && { echo "finding.sh add: --desc 필수" >&2; exit 2; }
    id="f-$(date +%Y%m%d%H%M%S)-${RANDOM}"
    jq -cn --arg id "$id" --arg ts "$(now)" --arg file "$file" \
           --arg rule "$rule" --arg sev "$sev" --arg desc "$desc" \
      '{id:$id, ts:$ts, status:"open", severity:$sev, file:$file, rule:$rule, desc:$desc}' \
      >> "$LEDGER"
    echo "$id"
    ;;

  resolve)
    id="${1:-}"; shift || true
    note=""
    while [[ $# -gt 0 ]]; do case "$1" in --note) note="$2"; shift 2;; *) shift;; esac; done
    [[ -z "$id" ]] && { echo "finding.sh resolve: <id> 필수" >&2; exit 2; }
    tmp="$(mktemp)"
    jq -c --arg id "$id" --arg ts "$(now)" --arg note "$note" \
      'if .id==$id then .status="resolved" | .resolvedTs=$ts | (if $note!="" then .note=$note else . end) else . end' \
      "$LEDGER" > "$tmp" && mv "$tmp" "$LEDGER"
    echo "resolved: $id"
    ;;

  list)
    scope="${1:---open}"
    # fromjson? : 줄별 파싱, 깨진 줄은 건너뜀 (원장 한 줄이 손상돼도 나머지는 살림)
    if [[ "$scope" == "--all" ]]; then
      jq -rR 'fromjson? | "[\(.status|ascii_upcase)] \(.severity)  \(.id)  \(.file)  — \(.desc)"' "$LEDGER" 2>/dev/null || true
    else
      jq -rR 'fromjson? | select(.status=="open") | "[\(.severity)] \(.id)  \(.file)  — \(.desc)"' "$LEDGER" 2>/dev/null || true
    fi
    ;;

  count-open)
    # list 와 동일한 fromjson? 전략 — 한 줄 손상으로 DLT 전체가 침묵하지 않도록(jq -s 슬럽 금지)
    jq -rR 'fromjson? | select(.status=="open") | .id' "$LEDGER" 2>/dev/null | wc -l | tr -d ' '
    ;;

  *)
    echo "finding.sh: 알 수 없는 명령 '$cmd' (add|list|resolve|count-open)" >&2
    exit 2
    ;;
esac
