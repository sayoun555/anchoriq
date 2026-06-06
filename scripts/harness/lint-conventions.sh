#!/usr/bin/env bash
#
# lint-conventions.sh — 하네스 기계적 컨벤션 린터 (PostToolUse: Edit|Write)
#
# 적대자(.md)가 "판단"으로 잡는 의미 위반과 달리, 여기선 *기계적으로 확정 가능한*
# 규칙만 grep/카운트로 강제한다. 전부 non-blocking WARN(systemMessage) — soft tripwire.
# 프로젝트 전용값(임계·패턴·모듈)은 harness.config.json 에서 (이식성 — DEPLOY_HARNESS.md).
#   · 파일 분리: maxFileLines 초과 / maxPublicMethods 초과
#   · 모듈 의존: coreModule 이 외부 모듈 import 금지(coreForbiddenImportPattern)
#   · 시크릿 하드코딩: secretPattern 매치(secretExcludePattern 제외)
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CFG="$SCRIPT_DIR/harness.config.json"
cfg() { jq -r "$1" "$CFG" 2>/dev/null; }

SOURCE_ROOT="$(cfg '.source.root // "backend"')"; [[ -z "$SOURCE_ROOT" || "$SOURCE_ROOT" == "null" ]] && SOURCE_ROOT="backend"
EXTS=(); while IFS= read -r _l; do [[ -n "$_l" ]] && EXTS+=("$_l"); done < <(cfg '.source.extensions[]?'); [[ ${#EXTS[@]} -eq 0 ]] && EXTS=("java")
MAX_LINES="$(cfg '.rules.maxFileLines // 200')"; MAX_LINES="${MAX_LINES//[!0-9]/}"; MAX_LINES="${MAX_LINES:-200}"
MAX_METHODS="$(cfg '.rules.maxPublicMethods // 7')"; MAX_METHODS="${MAX_METHODS//[!0-9]/}"; MAX_METHODS="${MAX_METHODS:-7}"
CORE_MODULE="$(cfg '.rules.coreModule // "anchoriq-core"')"; [[ -z "$CORE_MODULE" || "$CORE_MODULE" == "null" ]] && CORE_MODULE="anchoriq-core"
CORE_FORBIDDEN="$(cfg '.rules.coreForbiddenImportPattern // empty')"; [[ -z "$CORE_FORBIDDEN" || "$CORE_FORBIDDEN" == "null" ]] && CORE_FORBIDDEN='^import[[:space:]]+com\.anchoriq\.(api|collector|ai|automation)\.'
SECRET_PAT="$(cfg '.rules.secretPattern // empty')"; [[ -z "$SECRET_PAT" || "$SECRET_PAT" == "null" ]] && SECRET_PAT='(password|passwd|secret|api[_]?key|access[_]?key|private[_]?key|client[_]?secret|token)[[:space:]]*=[[:space:]]*"[^"]{6,}"'
SECRET_EXCL="$(cfg '.rules.secretExcludePattern // empty')"; [[ -z "$SECRET_EXCL" || "$SECRET_EXCL" == "null" ]] && SECRET_EXCL='\$\{|""|changeme|placeholder|example|your[_-]|xxxx|tokentype|Bearer|TODO'

INPUT="$(cat)"
FILE="$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // empty')"
[[ -z "$FILE" || ! -f "$FILE" ]] && exit 0

# 검사 대상: source.root 하위 main 소스 + review 확장자 (test/eval/generated 제외)
# (상대·절대 경로 모두 매치 — leading slash 요구 안 함)
case "$FILE" in *"$SOURCE_ROOT"/*/src/main/*) : ;; *) exit 0 ;; esac
case "$FILE" in *_eval*|*Test.java|*/generated/*|*/test/*) exit 0 ;; esac
_ext="${FILE##*.}"; _ok=0; for e in "${EXTS[@]}"; do [[ "$_ext" == "$e" ]] && _ok=1; done; [[ $_ok -eq 1 ]] || exit 0

warns=()

# ── 파일 크기 (maxFileLines 초과) ─────────────────────────────────
LINES="$(wc -l < "$FILE" 2>/dev/null | tr -d '[:space:]')"; LINES="${LINES:-0}"
if (( LINES > MAX_LINES )); then
  warns+=("📏 파일 ${LINES}줄 (>${MAX_LINES}) — 단일책임으로 분리 검토 (SRP)")
fi

# ── public 메서드 수 (maxPublicMethods 초과, 휴리스틱) ────────────
# @Configuration(=@Bean 팩토리)은 메서드가 많은 게 정상 → 제외해 오탐 방지.
if ! grep -q '@Configuration' "$FILE"; then
  METHODS="$(grep -E '^[[:space:]]*(public|protected)[[:space:]].*\([^)]*\)' "$FILE" 2>/dev/null \
    | grep -vE '\b(class|interface|record|enum)\b' | grep -c .)"; METHODS="${METHODS:-0}"
  if (( METHODS > MAX_METHODS )); then
    warns+=("🔪 public/protected 메서드 ~${METHODS}개 (>${MAX_METHODS}, 휴리스틱) — 책임 분리 검토")
  fi
fi

# ── 모듈 의존 규칙: coreModule 은 외부 모듈 import 금지 ───────────
case "$FILE" in
  *"$CORE_MODULE"/*)
    BAD="$(grep -nE "$CORE_FORBIDDEN" "$FILE" 2>/dev/null | head -3)"
    if [[ -n "$BAD" ]]; then
      warns+=("🚫 ${CORE_MODULE}가 외부 모듈을 import — core는 순수 도메인(외부 모듈 의존 금지). 위반:")
      while IFS= read -r l; do warns+=("     $l"); done <<< "$BAD"
    fi
    ;;
esac

# ── 시크릿 하드코딩 (env 참조 ${..} 가 아닌 리터럴 대입) ───────────
SECRETS="$(grep -niE "$SECRET_PAT" "$FILE" 2>/dev/null | grep -iEv "$SECRET_EXCL" | head -3)"
if [[ -n "$SECRETS" ]]; then
  warns+=("🔑 시크릿 하드코딩 의심 — .env + 환경변수 참조로(예: \${ENV:기본값}). 의심줄(오탐일 수 있음):")
  while IFS= read -r l; do warns+=("     $l"); done <<< "$SECRETS"
fi

# ── 출력 ─────────────────────────────────────────────────────────
[[ ${#warns[@]} -eq 0 ]] && exit 0

REL="${FILE##*"$SOURCE_ROOT"/}"
MSG="⚠️ 하네스 컨벤션(warn) — ${SOURCE_ROOT}/${REL}"$'\n'
for w in "${warns[@]}"; do MSG+="  • ${w}"$'\n'; done

jq -cn --arg msg "$MSG" '{systemMessage:$msg}'
exit 0
