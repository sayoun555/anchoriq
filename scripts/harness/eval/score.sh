#!/usr/bin/env bash
#
# score.sh — 하네스 eval 채점기 (H1: 도구 grounding vs 순수 읽기)
#
# 데이터셋(ground truth)과 패널 판정(workflow 출력)을 대조해 지표를 계산한다.
#   사용: score.sh <dataset.json> <verdicts.json> [label]
#
# ground truth 의미:
#   "bogus" → 올바른 판정은 refute (confirmed=false). confirmed=true면 false-confirm(hallucination).
#   "real"  → 올바른 판정은 confirm (confirmed=true). confirmed=false면 missed-detection.
#
set -uo pipefail
DATASET="${1:?dataset.json 필요}"
VERDICTS="${2:?verdicts.json 필요}"
LABEL="${3:-config}"

jq -nr --slurpfile ds "$DATASET" --slurpfile vd "$VERDICTS" --arg label "$LABEL" '
  ($ds[0]) as $dataset
  | (($vd[0].verdicts) // ($vd[0].result.verdicts) // []) as $verdicts
  | ($dataset | map({key:.id, value:.groundTruth}) | from_entries) as $gt
  | ($verdicts | map({
        id: .id,
        gt: ($gt[.id] // "unknown"),
        confirmed: .confirmed,
        grounded: (.groundedVotes // 0),
        total: (.total // 0)
    })) as $rows
  | ($rows|length) as $n
  | ($rows | map(select((.gt=="bogus" and .confirmed==false) or (.gt=="real" and .confirmed==true))) | length) as $correct
  | ($rows | map(select(.gt=="bogus" and .confirmed==true)) | length) as $falseConfirm
  | ($rows | map(select(.gt=="real"  and .confirmed==false)) | length) as $missed
  | ($rows | map(.grounded) | add // 0) as $gv
  | ($rows | map(.total) | add // 0) as $tv
  | "── eval: \($label) ──",
    "  N=\($n)  정답=\($correct)/\($n)  (accuracy=\(if $n>0 then (100*$correct/$n|floor) else 0 end)%)",
    "  ❗ false-confirm(없는 위반을 confirm = hallucination) = \($falseConfirm)   ← 낮을수록 좋음 [H1 핵심]",
    "  missed-detection(진짜 위반을 놓침) = \($missed)",
    "  grounding rate(도구 인용 표 비율) = \(if $tv>0 then (100*$gv/$tv|floor) else 0 end)%  (\($gv)/\($tv))",
    "  per-finding: " + ($rows | map("\(.id):\(.gt)→\(if .confirmed then "confirm" else "refute" end)\(if .gt=="bogus" and .confirmed then "✗" else "✓" end)") | join("  "))
'
