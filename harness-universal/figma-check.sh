#!/usr/bin/env bash
#
# figma-check.sh — Figma 시각 충실도 렌즈 (프론트/UI 전용).
#   생성된 컴포넌트의 *렌더 결과*를 Figma 디자인과 비교(측정 기반)해 어긋남을 잡는다.
#   MCP가 못 주는 검증 절반(일방통행 = 렌더 못 봄)을 채움. 근거: docs/figma-to-code/RESEARCH.md
#
#   엔진: UIMatch(@uimatch/cli, Playwright 기반, ΔE2000 색차 + Design Fidelity Score 0~100).
#   *무겁다*(렌더+측정+dev서버 필요) → 편집마다 아님. 컴포넌트 완성 시/pre-push/CI/수동.
#   백엔드 렌즈와 무관(파일/작업으로 자동 분리) — 이건 UI 작업에만.
#
# 사용:  figma-check.sh [컴포넌트이름]    (생략 시 config의 전 컴포넌트)
#   필요: export FIGMA_ACCESS_TOKEN=figd_...  + 렌더 서버(Storybook/dev) 떠 있어야
#   설정: harness.config.json 의 figma.{fileKey, threshold, components[]}
#   CI: 임계 미달 시 exit 1
#
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG="$SCRIPT_DIR/harness.config.json"
ONLY="${1:-}"

command -v jq >/dev/null 2>&1 || { echo "⚠️ figma-check: jq 필요"; exit 0; }
[[ -f "$CONFIG" ]] || { echo "⚠️ figma-check: harness.config.json 없음"; exit 0; }

# figma 설정 없으면 이 프로젝트는 UI 충실도 검증 대상 아님 → 조용히 통과
if ! jq -e '.figma.components | length > 0' "$CONFIG" >/dev/null 2>&1; then
  echo "ℹ️ figma-check: config에 figma.components 없음 — UI 충실도 검증 비활성(백엔드 프로젝트면 정상)."
  exit 0
fi

# 엔진(UIMatch) + 토큰 확인 — 없으면 *설치/설정 안내*하고 비차단 통과(개발 흐름 안 막음)
if ! command -v uimatch >/dev/null 2>&1 && ! npx --no-install uimatch --version >/dev/null 2>&1; then
  echo "ℹ️ figma-check: UIMatch 미설치 → 시각검증 건너뜀. 설치: npm i -g @uimatch/cli playwright && npx playwright install chromium"
  exit 0
fi
UIMATCH="uimatch"; command -v uimatch >/dev/null 2>&1 || UIMATCH="npx @uimatch/cli"
if [[ -z "${FIGMA_ACCESS_TOKEN:-}" ]]; then
  echo "ℹ️ figma-check: FIGMA_ACCESS_TOKEN 없음 → 건너뜀. export FIGMA_ACCESS_TOKEN=figd_... 후 재시도."
  exit 0
fi

FILE_KEY="$(jq -r '.figma.fileKey // empty' "$CONFIG")"
THRESHOLD="$(jq -r '.figma.threshold // 90' "$CONFIG")"
GATE="$(jq -r '.figma.gate // "component/dev"' "$CONFIG")"

# 컴포넌트 순회 (name/node/url/selector)
fail=0; ran=0
while IFS= read -r row; do
  name="$(jq -r '.name'     <<<"$row")"
  node="$(jq -r '.node'     <<<"$row")"
  url="$(jq -r '.url'       <<<"$row")"
  sel="$(jq -r '.selector // empty' <<<"$row")"
  [[ -n "$ONLY" && "$ONLY" != "$name" ]] && continue
  ran=$((ran+1))
  echo "🎨 [$name] Figma ${FILE_KEY}:${node}  vs  ${url}"
  args=( compare "figma=${FILE_KEY}:${node}" "story=${url}" )
  [[ -n "$sel" ]] && args+=( "selector=${sel}" )
  [[ -n "$GATE" ]] && args+=( "--gate=${GATE}" )
  if $UIMATCH "${args[@]}"; then
    echo "  ✅ [$name] 충실도 게이트 통과(≥${THRESHOLD}, ${GATE})"
  else
    echo "  ⛔ [$name] 충실도 미달 — 위 diff/델타대로 컴포넌트 수정(간격·색·요소)."
    fail=1
  fi
done < <(jq -c '.figma.components[]' "$CONFIG")

[[ $ran -eq 0 && -n "$ONLY" ]] && { echo "figma-check: '$ONLY' 컴포넌트가 config에 없음"; exit 2; }
[[ $fail -eq 1 ]] && { echo "❌ figma-check: 일부 컴포넌트 디자인 미달."; exit 1; }
echo "✅ figma-check: 검사한 ${ran}개 컴포넌트 모두 디자인 충실."
exit 0
