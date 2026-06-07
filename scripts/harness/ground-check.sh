#!/usr/bin/env bash
#
# ground-check.sh — AnchorIQ 하네스 외부 grounding (의미 적대자 보강)
#
# 연구 근거(docs/harness-engineering/research/RESEARCH_HARNESS_AND_MULTIAGENT.md §2):
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
CFG="$SCRIPT_DIR/harness.config.json"
cfg() { jq -r "$1" "$CFG" 2>/dev/null; }

# 프로젝트 전용값은 harness.config.json 에서 (이식성 — DEPLOY_HARNESS.md)
MODULE_PAT="$(cfg '.build.modulePattern // "anchoriq-(core|api|collector|ai|automation)"')"
BUILD_DIR="$(cfg '.build.workingDir // "backend"')"
COMPILE_TMPL="$(cfg '.build.compileCommand // "./gradlew :{module}:compileJava --console=plain -q"')"
TEST_TMPL="$(cfg '.build.testCommand // "./gradlew :{module}:test --console=plain -q"')"

FILE="${1:-}"
MODE="${2:-compile}"
[[ -z "$FILE" ]] && { echo "GROUNDING: (파일 경로 필요)"; exit 0; }

# 빌드 모듈 추출 (config의 modulePattern)
MODULE="$(printf '%s' "$FILE" | grep -oE "$MODULE_PAT" | head -1)"
if [[ -z "$MODULE" ]]; then
  echo "GROUNDING: (빌드 모듈 아님 — grounding 생략)"
  exit 0
fi

cd "$REPO_ROOT/$BUILD_DIR" || { echo "GROUNDING: ($BUILD_DIR 디렉토리 없음)"; exit 0; }

# {module} 치환 후 실행 (빌드 도구 무관 — config 명령 템플릿).
# ⚠️ 명령은 *커밋된* harness.config.json 에서 온다(신뢰 경계 = config 편집·커밋권). {module}은
#    modulePattern grep으로 제약돼 주입 불가, 외부 입력엔 노출 안 됨. config를 커밋할 수 있으면
#    이미 임의실행 가능하므로 eval은 그 신뢰모델 안에서 허용한다 (A8 — accept-risk).
run_tmpl() { ( eval "${1//\{module\}/$MODULE}" ) 2>&1; }

echo "GROUNDING: 대상 모듈 = :$MODULE"

# 1) 컴파일 (타입/시그니처 차원의 결정론적 신호)
if OUT="$(run_tmpl "$COMPILE_TMPL")"; then
  echo "GROUNDING: ✅ :$MODULE 컴파일 통과 (타입/시그니처 OK — 이 차원은 적대자가 다시 추측할 필요 없음)"
else
  echo "GROUNDING: ❌ :$MODULE 컴파일 실패 — 아래 컴파일 에러가 1차 증거다(LLM 추측보다 우선):"
  printf '%s\n' "$OUT" | grep -E 'error:|error;|\.java:[0-9]' | head -25
  exit 0   # 훅을 막지 않는다(정보 제공). 적대자가 이 에러를 근거로 판단/반려.
fi

# 2) (옵션) 테스트 — 행위 차원. 느려서 기본 비활성.
if [[ "$MODE" == "--test" ]]; then
  echo "GROUNDING: 테스트 실행 중 (:$MODULE) ..."
  if OUT="$(run_tmpl "$TEST_TMPL")"; then
    echo "GROUNDING: ✅ :$MODULE 테스트 통과 (행위 차원 OK)"
  else
    echo "GROUNDING: ❌ :$MODULE 테스트 실패 — 아래가 1차 증거:"
    printf '%s\n' "$OUT" | grep -E 'FAILED|> Task|Assertion|Exception' | head -25
  fi
fi
