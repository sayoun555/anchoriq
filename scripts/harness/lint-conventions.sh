#!/usr/bin/env bash
#
# lint-conventions.sh — AnchorIQ 하네스 기계적 컨벤션 린터 (PostToolUse: Edit|Write)
#
# 적대자(.md)가 "판단"으로 잡는 의미 위반과 달리, 여기선 *기계적으로 확정 가능한*
# AGENTS 규칙만 grep/카운트로 강제한다. 전부 non-blocking WARN(systemMessage) —
# 코더에게 보이되 턴을 막지 않는다(soft tripwire). 메우는 갭:
#   · AGENTS 6  파일 분리: 200줄 초과 / public 메서드 7개 초과
#   · 모듈 의존: anchoriq-core 가 외부 모듈(api/collector/ai/automation) import 금지
#   · AGENTS 9  시크릿 하드코딩: secret/password/token 등에 문자열 리터럴 직접 대입
#
set -uo pipefail

INPUT="$(cat)"
FILE="$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // empty')"
[[ -z "$FILE" || ! -f "$FILE" ]] && exit 0

# 백엔드 main .java 만 검사 (test/generated/eval fixture 제외)
case "$FILE" in
  *backend/*/src/main/java/*.java) : ;;
  *) exit 0 ;;
esac
case "$FILE" in
  *_eval*|*Test.java|*/generated/*) exit 0 ;;
esac

warns=()

# ── AGENTS 6: 파일 크기 (200줄 초과) ──────────────────────────────
LINES="$(wc -l < "$FILE" 2>/dev/null | tr -d '[:space:]')"; LINES="${LINES:-0}"
if (( LINES > 200 )); then
  warns+=("📏 파일 ${LINES}줄 (>200) — AGENTS 6: 단일책임으로 분리 검토 (SRP)")
fi

# ── AGENTS 6: public 메서드 수 (7개 초과, 휴리스틱) ───────────────
# @Configuration(=@Bean 팩토리)은 메서드가 많은 게 정상 → 제외해 오탐 방지.
if ! grep -q '@Configuration' "$FILE"; then
  METHODS="$(grep -E '^[[:space:]]*(public|protected)[[:space:]].*\([^)]*\)' "$FILE" 2>/dev/null \
    | grep -vE '\b(class|interface|record|enum)\b' | grep -c .)"; METHODS="${METHODS:-0}"
  if (( METHODS > 7 )); then
    warns+=("🔪 public/protected 메서드 ~${METHODS}개 (>7, 휴리스틱) — AGENTS 6: 책임 분리 검토")
  fi
fi

# ── 모듈 의존 규칙: core 는 외부 모듈 import 금지 ────────────────
case "$FILE" in
  *anchoriq-core/*)
    BAD="$(grep -nE '^import[[:space:]]+com\.anchoriq\.(api|collector|ai|automation)\.' "$FILE" 2>/dev/null | head -3)"
    if [[ -n "$BAD" ]]; then
      warns+=("🚫 anchoriq-core가 외부 모듈을 import — core는 순수 도메인(외부 모듈 의존 금지). 위반:")
      while IFS= read -r l; do warns+=("     $l"); done <<< "$BAD"
    fi
    ;;
esac

# ── AGENTS 9: 시크릿 하드코딩 (env 참조 ${..} 가 아닌 리터럴 대입) ─
# 'Bearer' 같은 상수 타입값·빈 문자열·플레이스홀더는 제외해 오탐을 줄인다.
SECRETS="$(grep -niE '(password|passwd|secret|api[_]?key|access[_]?key|private[_]?key|client[_]?secret|token)[[:space:]]*=[[:space:]]*"[^"]{6,}"' "$FILE" 2>/dev/null \
  | grep -iEv '\$\{|""|changeme|placeholder|example|your[_-]|xxxx|tokentype|Bearer|TODO' | head -3)"
if [[ -n "$SECRETS" ]]; then
  warns+=("🔑 시크릿 하드코딩 의심 — AGENTS 9: .env + spring-dotenv(\${ENV:기본값})로. 의심줄(오탐일 수 있음):")
  while IFS= read -r l; do warns+=("     $l"); done <<< "$SECRETS"
fi

# ── 출력 ─────────────────────────────────────────────────────────
[[ ${#warns[@]} -eq 0 ]] && exit 0

REL="${FILE##*backend/}"
MSG="⚠️ 하네스 컨벤션(warn) — backend/${REL}"$'\n'
for w in "${warns[@]}"; do MSG+="  • ${w}"$'\n'; done

jq -cn --arg msg "$MSG" '{systemMessage:$msg}'
exit 0
