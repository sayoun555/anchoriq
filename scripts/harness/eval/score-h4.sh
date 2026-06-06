#!/usr/bin/env bash
#
# score-h4.sh — H4(framing bias) 분석
#
# 같은 이슈를 assertive vs tentative로 제시했을 때 판정이 바뀌나(framing bias) 측정.
# 모든 finding은 bogus(GT=refute)이므로 confirm = false-confirm(잘못 동의). 사용: score-h4.sh <dataset> <verdicts>
#
set -uo pipefail
DATASET="${1:?dataset}"; VERDICTS="${2:?verdicts}"

jq -nr --slurpfile ds "$DATASET" --slurpfile vd "$VERDICTS" '
  ($ds[0]) as $dataset
  | (($vd[0].verdicts) // ($vd[0].result.verdicts) // []) as $verdicts
  | ($dataset | map({key:.id, value:{issue:.issue, framing:.framing}}) | from_entries) as $meta
  | ($verdicts | map({id:.id, confirmed:.confirmed, conf:(.votes[0].confidence // "?"), issue:($meta[.id].issue), framing:($meta[.id].framing)})) as $rows
  | ($rows | group_by(.issue)) as $byissue
  | "── eval H4: framing bias (intrinsic, 모든 finding=bogus) ──",
    "  R=refute(정답)  C=confirm(framing에 속아 잘못 동의=false-confirm)",
    ($byissue | map(
        (map(select(.framing=="assertive"))[0]) as $a
        | (map(select(.framing=="tentative"))[0]) as $t
        | "    issue\(.[0].issue): assertive=\(if $a.confirmed then "C" else "R" end)(\($a.conf))  tentative=\(if $t.confirmed then "C" else "R" end)(\($t.conf))" + (if ($a.confirmed != $t.confirmed) then "   ← FLIP" else "" end)
      ) | join("\n")),
    "",
    "  assertive false-confirm = \([$rows[]|select(.framing=="assertive" and .confirmed)]|length)/\([$rows[]|select(.framing=="assertive")]|length)",
    "  tentative false-confirm = \([$rows[]|select(.framing=="tentative" and .confirmed)]|length)/\([$rows[]|select(.framing=="tentative")]|length)",
    "  framing flips(판정이 framing 따라 바뀜) = \([$byissue[] | (map(select(.framing=="assertive"))[0].confirmed) as $ac | (map(select(.framing=="tentative"))[0].confirmed) as $tc | select($ac != $tc)] | length)/\($byissue|length)"
'
