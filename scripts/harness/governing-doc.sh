#!/usr/bin/env bash
#
# governing-doc.sh — AnchorIQ 하네스 컨텍스트 라우터 (PreToolUse: Edit|Write)
#
# CLAUDE.md의 "설계 문서 학습 의무"를 산문이 아니라 결정론적 주입으로 바꾼다.
# 수정하려는 파일 경로를 분석해, 그 파일을 지배하는 plan/ 설계 문서를
# 한 줄 포인터로 컨텍스트에 주입한다. non-blocking (항상 allow, 컨텍스트만 추가).
#
set -uo pipefail

INPUT="$(cat)"
FILE="$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // empty')"
[[ -z "$FILE" ]] && exit 0

# 코드/설정 파일에만 라우팅한다. 문서(.md 등) 파일명에 우연히 키워드(JWT, payment…)가
# 들어가 오탐하는 것을 막는다. (troubleshooting/GOVERNING_DOC_OVERMATCHES_DOCS.md 참고)
case "$FILE" in
  *.java|*.kt|*.ts|*.tsx|*.yml|*.yaml|*.gradle|*.gradle.kts|*Dockerfile|*docker-compose*) : ;;
  *) exit 0 ;;
esac

docs=()           # 매칭된 plan/ 문서들
add() { docs+=("$1"); }

case "$FILE" in
  # ── 인프라 구현체 (DDD: infrastructure 레이어) ──
  *infrastructure*neo4j*|*infrastructure*ontology*|*infrastructure*graph*)
    add "plan/ARCHITECTURE.md(§온톨로지/Aggregate)"; add "plan/DB_OPTIMIZATION.md(Neo4j)";;
  *infrastructure*kafka*|*Consumer*.java|*Producer*.java|*consumer*|*producer*)
    add "plan/KAFKA_DESIGN.md(토픽/Consumer Group/DLT)"; add "plan/TRANSACTION_DESIGN.md(Tier2 최종일관성)";;
  *infrastructure*redis*|*redis*Geo*|*Geo*redis*)
    add "plan/DB_OPTIMIZATION.md(Redis GEO)";;
  *infrastructure*elasticsearch*|*infrastructure*search*)
    add "plan/DB_OPTIMIZATION.md(Elasticsearch)";;
  *repository*|*infrastructure*persistence*)
    add "plan/DB_OPTIMIZATION.md(스키마/인덱스)"; add "plan/PACKAGE_STRUCTURE.md(Repository 인터페이스)";;

  # ── API / Controller ──
  *controller*|*/api/*Controller*.java|*/web/*)
    add "plan/API_ENDPOINTS.md(엔드포인트 계약)"; add "plan/API_JSON_EXAMPLES.md(요청/응답 예시)";;

  # ── 인증 / 결제 / 구독 ──
  *account*payment*|*payment*|*subscription*|*account*auth*|*/security/*|*Jwt*|*JWT*)
    add "plan/AUTH_PAYMENT.md(JWT/Stripe·Toss/플랜)"; add "plan/TRANSACTION_DESIGN.md(Tier1 강한일관성)";;

  # ── Application 레이어 (오케스트레이션) ──
  *application*Service*.java|*/application/*)
    add "plan/ARCHITECTURE.md(§Application=오케스트레이션만)"; add "plan/TRANSACTION_DESIGN.md(@Transactional 경계)";;

  # ── Domain 레이어 (풍부한 모델) ──
  *domain*service*|*DomainService*.java)
    add "plan/ARCHITECTURE.md(§Domain Service)";;
  *domain*model*|*domain*.java)
    add "plan/ARCHITECTURE.md(§Entity/VO·비즈니스 로직은 도메인에)";;

  # ── 설정 / 인프라 파일 ──
  *application.yml|*application-*.yml|*application.yaml)
    add "plan/APPLICATION_YML_DESIGN.md(3 프로필 + .env)";;
  *build.gradle*|*settings.gradle*)
    add "plan/BUILD_GRADLE_DESIGN.md(멀티모듈 의존성)";;
  *docker-compose*|*Dockerfile*)
    add "plan/DOCKER_COMPOSE_DESIGN.md(서비스/포트/프로필)";;

  # ── 프론트엔드 ──
  *frontend/src/*.tsx|*frontend/src/*.ts)
    add "plan/UI_DESIGN.md(팔란티어 감성 뷰)"; add "frontend/AGENTS.md(Next.js: node_modules/next/dist/docs 먼저 읽기)";;
esac

# .java 파일이면 코딩 규칙(AGENTS.md)은 항상 적용
case "$FILE" in
  *.java) add "plan/AGENTS.md(Clean Code/OOP/SOLID/DDD 규칙 15)";;
esac

# 매칭된 문서가 없으면 조용히 통과 (코드 외 파일 등)
[[ ${#docs[@]} -eq 0 ]] && exit 0

# 중복 제거 후 " · " 로 한 줄 결합 (paste -d 는 멀티문자 구분자 불가 → awk 사용)
JOINED="$(printf '%s\n' "${docs[@]}" | awk '!seen[$0]++{a[++n]=$0} END{for(i=1;i<=n;i++) printf "%s%s",(i>1?" · ":""),a[i]}')"
MSG="📐 설계 준수: 이 파일의 지배 문서 → ${JOINED}. 통째로 읽지 말고 §표시된 절만 가져와라(중간 소실 방지): scripts/harness/plan-search.sh section <문서> '<§키워드>'  ·  넓게 찾을 땐 plan-search.sh grep '<질의>'. (CLAUDE.md 학습 의무)"

jq -cn --arg ctx "$MSG" \
  '{hookSpecificOutput:{hookEventName:"PreToolUse", additionalContext:$ctx}}'
exit 0
