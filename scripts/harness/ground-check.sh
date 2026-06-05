#!/usr/bin/env bash
#
# ground-check.sh — AnchorIQ 하네스 외부 grounding (의미 적대자 보강)
#
# 연구 근거(docs/harness-engineering/RESEARCH_HARNESS_AND_MULTIAGENT.md §2):
#   "LLM이 LLM을 평가하는 self-critique는 약하다. 외부 검증 신호(컴파일/테스트)가 핵심."
#   (Reflexion 80→91%, Renze&Guven +14.6% — 모두 외부 grounding에 의존)
#
# 의미 적대자(PostToolUse)가 코드를 '추측'으로 판단하기 전에, 대상 모듈을 실제로
# 컴파일해 그 결과를 1차 증거로 쓰게 한다. 컴파일 실패 에러 > LLM의 의견.
#
# 사용:
#   ground-check.sh <file_path>            # 해당 모듈 컴파일 (기본)
#   ground-check.sh <file_path> --test     # 컴파일 + 해당 모듈 테스트 (느림)
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FILE="${1:-}"
MODE="${2:-compile}"
[[ -z "$FILE" ]] && { echo "GROUNDING: (파일 경로 필요)"; exit 0; }

# 백엔드 Gradle 모듈 추출
MODULE="$(printf '%s' "$FILE" | grep -oE 'anchoriq-(core|api|collector|ai|automation)' | head -1)"
if [[ -z "$MODULE" ]]; then
  echo "GROUNDING: (백엔드 Gradle 모듈 아님 — grounding 생략)"
  exit 0
fi

cd "$REPO_ROOT/backend" || { echo "GROUNDING: (backend 디렉토리 없음)"; exit 0; }

run() { ./gradlew "$@" --console=plain -q 2>&1; }

echo "GROUNDING: 대상 모듈 = :$MODULE"

# 1) 컴파일 (타입/시그니처 차원의 결정론적 신호)
if OUT="$(run ":$MODULE:compileJava")"; then
  echo "GROUNDING: ✅ :$MODULE:compileJava 통과 (타입/시그니처 OK — 이 차원은 적대자가 다시 추측할 필요 없음)"
else
  echo "GROUNDING: ❌ :$MODULE:compileJava 실패 — 아래 컴파일 에러가 1차 증거다(LLM 추측보다 우선):"
  printf '%s\n' "$OUT" | grep -E 'error:|error;|\.java:[0-9]' | head -25
  exit 0   # 훅을 막지 않는다(정보 제공). 적대자가 이 에러를 근거로 판단/반려.
fi

# 2) (옵션) 테스트 — 행위 차원. 느려서 기본 비활성.
if [[ "$MODE" == "--test" ]]; then
  echo "GROUNDING: 테스트 실행 중 (:$MODULE:test) ..."
  if OUT="$(run ":$MODULE:test")"; then
    echo "GROUNDING: ✅ :$MODULE:test 통과 (행위 차원 OK)"
  else
    echo "GROUNDING: ❌ :$MODULE:test 실패 — 아래가 1차 증거:"
    printf '%s\n' "$OUT" | grep -E 'FAILED|> Task|Assertion|Exception' | head -25
  fi
fi
