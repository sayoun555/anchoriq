#!/usr/bin/env bash
#
# score-h2.sh — H2(렌즈 다양성/다수결) 분석
#
# 3렌즈 1회 실행 출력에서 "단일 렌즈였다면" vs "3렌즈 다수결"을 유도 비교한다.
#   사용: score-h2.sh <dataset.json> <verdicts.json>
#
# 측정: (1) 렌즈 불일치율(split rate) — 렌즈가 갈리지 않으면 다수결은 무의미.
#       (2) clear-GT 항목에서 단일 렌즈별 정확도 vs 다수결 정확도.
#
set -uo pipefail
DATASET="${1:?dataset.json}"
VERDICTS="${2:?verdicts.json}"

jq -nr --slurpfile ds "$DATASET" --slurpfile vd "$VERDICTS" '
  ($ds[0]) as $dataset
  | (($vd[0].verdicts) // ($vd[0].result.verdicts) // []) as $verdicts
  | ($dataset | map({key:.id, value:{gt:.groundTruth, kind:.kind}}) | from_entries) as $meta
  | ($verdicts | map(
        . as $v
        | ($v.votes | map(.refuted)) as $r      # 렌즈별 refuted (true=refute,false=confirm)
        | {
            id: $v.id,
            gt: ($meta[$v.id].gt // "?"),
            kind: ($meta[$v.id].kind // "?"),
            votes: $r,
            split: (($r | unique | length) > 1),
            majorityRefute: ((($r | map(select(.)) | length)) >= (($r|length+1)/2)),
            lensVotes: ($v.votes | map({lens:.lens, refute:.refuted}))
          }
    )) as $rows
  | ($rows|length) as $n
  | ($rows | map(select(.split)) | length) as $splits
  | "── eval H2: 렌즈 다양성/다수결 ──",
    "  N=\($n)  split(렌즈 갈림)=\($splits)  → 불일치율 = \(if $n>0 then (100*$splits/$n|floor) else 0 end)%",
    "  (불일치율 0%면 1렌즈=3렌즈 → 다수결 무의미. 높을수록 다수결이 일함)",
    "",
    "  per-finding [렌즈순서: 출력 votes 순] (R=refute C=confirm):",
    ($rows | map("    \(.id) [\(.kind)/\(.gt)] " + (.lensVotes | map("\(.lens[0:4])=\(if .refute then "R" else "C" end)") | join(" ")) + (if .split then "  ← SPLIT" else "  (unanimous)" end)) | join("\n")),
    "",
    "  clear-GT 항목 단일렌즈 vs 다수결 정확도:",
    ( [$rows[] | select(.kind=="clear")] as $clear
      | if ($clear|length)==0 then "    (clear 항목 없음)"
        else
          ( [range(0; ($clear[0].votes|length))] | map(. as $i
              | ([$clear[] | (.votes[$i]) as $vote | (.gt=="bogus" and $vote==true) or (.gt=="real" and $vote==false)] | map(select(.))|length) as $ok
              | "    lens#\($i): \($ok)/\($clear|length)"
            ) | join("   ") )
          + "   | majority: " + ([$clear[] | (.gt=="bogus" and (.majorityRefute)) or (.gt=="real" and (.majorityRefute|not))] | map(select(.))|length | tostring) + "/" + ($clear|length|tostring)
        end )
'
