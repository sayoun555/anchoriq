#!/usr/bin/env bash
#
# session-context.sh — AnchorIQ 하네스 세션 부트스트랩 (SessionStart)
#
# 매 세션 시작 시 "설계 문서 학습 의무" 프로토콜과 현재 plan/ 문서 인덱스를
# 컨텍스트로 주입한다. 문서 목록은 plan/ 디렉토리에서 동적으로 생성 →
# 문서가 늘거나 바뀌어도 하네스가 자동으로 따라간다.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# 설계문서 디렉토리는 harness.config.json 에서 (이식성 — DEPLOY_HARNESS.md)
DOCS_DIR="$(jq -r '.designDocsDir // "plan"' "$SCRIPT_DIR/harness.config.json" 2>/dev/null)"; [[ -z "$DOCS_DIR" || "$DOCS_DIR" == "null" ]] && DOCS_DIR="plan"
PLAN_DIR="$REPO_ROOT/$DOCS_DIR"

BRANCH="$(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"

# 설계 문서 인덱스 (동적 — designDocsDir에서)
if [[ -d "$PLAN_DIR" ]]; then
  DOC_INDEX="$(cd "$PLAN_DIR" && ls -1 *.md 2>/dev/null | sed "s#^#  - ${DOCS_DIR}/#" )"
else
  DOC_INDEX="  (${DOCS_DIR}/ 디렉토리 없음)"
fi

# findings DLT — 미해결 설계 위반을 다음 세션에 재주입 (전파 채널 C)
OPEN_COUNT="$(bash "$SCRIPT_DIR/finding.sh" count-open 2>/dev/null || echo 0)"
if [[ "${OPEN_COUNT:-0}" -gt 0 ]]; then
  OPEN_LIST="$(bash "$SCRIPT_DIR/finding.sh" list --open 2>/dev/null | sed 's/^/  /')"
  FINDINGS_BLOCK="
미해결 설계 위반 (findings DLT — ${OPEN_COUNT}건, 닫기 전까지 매 세션 재노출):
${OPEN_LIST}
  → 처리 후: scripts/harness/finding.sh resolve <id>"
else
  FINDINGS_BLOCK=""
fi

read -r -d '' CTX <<EOF || true
🧭 AnchorIQ 하네스 — 작업 시작 프로토콜 (branch: ${BRANCH})

설계 문서 학습 의무 (CLAUDE.md 최우선 규칙):
  1) plan/AGENTS.md 의 코딩 규칙 15개 + 구현 체크리스트
  2) plan/IMPLEMENTATION_PLAN.md 에서 현재 Phase 확인
  3) 그 Phase가 지정한 plan/ 문서 → 전부 읽은 뒤 코드 작성
  ※ 학습 후 더 나은 방법으로 바꾸는 건 OK(근거 필수). 학습 없이 자기 방식은 금지.

활성 하네스 가드:
  • PreToolUse(Edit|Write): 수정 파일을 지배하는 plan/ 문서를 자동 포인팅
  • PostToolUse(Edit|Write): ① 기계 린트(파일분리·core 모듈의존·시크릿 — warn) ② 의미 리뷰어가 plan/·AGENTS.md 위반 검사 → 위반 시 반려
  • Stop: 백엔드 main 소스의 미완성 마커(stub/TODO/mock) 잔존 시 경고
    (수동/CI: scripts/harness/check-stubs.sh)

소프트 리마인더(강제 안 함):
  • AGENTS 11 — 이번 세션에 문제를 해결(트러블슈팅)했다면 docs/ 에 SCREAMING_SNAKE_CASE.md 로 기록(문제→원인→해결→CS원리).
${FINDINGS_BLOCK}
설계 문서 인덱스:
${DOC_INDEX}
EOF

jq -cn --arg ctx "$CTX" \
  '{hookSpecificOutput:{hookEventName:"SessionStart", additionalContext:$ctx}}'
exit 0
