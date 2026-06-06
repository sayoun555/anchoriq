# AnchorIQ Development Harness (Agent Harness Engineering)

> 이 디렉토리는 **재사용 가능한 에이전트 하네스 유닛**이다. 앱 코드가 아니라, *그 앱을 설계와 어긋나지 않게 뽑아내도록* 에이전트(Claude Code)를 구속하는 인프라다. "설계 문서 먼저 읽기 / DDD 레이어 / 임시방편 금지" 같은 규칙을 산문이 아니라 **결정론적 훅**으로 강제하고, 그 설계 결정을 **동료심사 논문**으로 근거화했다.
>
> 다른 프로젝트로 옮기려면 → [`DEPLOY_HARNESS.md`](DEPLOY_HARNESS.md) (이 폴더 + `.claude/` 복사 후 `harness.config.json` 한 장만 교체).

## 무엇을 하나

- **4개 생명주기 훅** (`.claude/settings.json` → 이 디렉토리): SessionStart 컨텍스트 주입 · PreToolUse 지배문서 라우팅 · PostToolUse(기계 린트 + 의미 적대자 리뷰 + 컴파일 grounding) · Stop anti-stub 게이트
- **전파 3채널**: (A) 훅 reason 재주입 · (B) 다수결 검증단 Workflow(멀티에이전트) · (C) findings DLT 원장(세션 간 재노출)
- **컨텍스트 최적화**: 긴 설계문서 통째 주입 대신 섹션만 추출(lost-in-the-middle 회피, 373→13줄)
- **도구 쥔 검증단**: 검증자가 읽기만 하지 않고 실제 컴파일·검색·MCP로 grounding (CRITIC·Agent-as-a-Judge 근거)
- **오케스트레이션**: 대규모 코드 작성을 분해→병렬→통합(`orchestrate-feature`), 가변 배치는 감독자(`supervise-batch`)
- **연구 근거**: 설계 결정을 ICLR/NeurIPS/TACL 논문 16편 + 측정 eval로 뒷받침

## 구성 (이 디렉토리)

| 파일 | 종류 | 역할 |
|------|------|------|
| `harness.config.json` | **설정 contract** | 프로젝트 전용값(설계문서 dir·빌드명령·모듈패턴·docMap·규칙 임계). **배포 시 이것만 교체** |
| `session-context.sh` | `.sh` 훅 | SessionStart — design-first 프로토콜 + 문서 인덱스 + 열린 findings 주입 |
| `governing-doc.sh` | `.sh` 훅 | PreToolUse — 수정 파일 → 지배 설계문서 라우팅(config docMap) |
| `lint-conventions.sh` | `.sh` 훅 | PostToolUse — 기계 컨벤션(파일크기·모듈의존·시크릿) warn |
| `review-protocol.md` | `.md` 적대자 | PostToolUse 의미 리뷰어 프로토콜(DDD/트랜잭션/캡슐화/YAGNI 판단) |
| `ground-check.sh` | `.sh` | 외부 grounding — 대상 모듈 컴파일(config 명령) |
| `check-stubs.sh` | `.sh` 훅/CI | Stop·CI — 미완성 마커(stub/TODO/임시) 게이트 |
| `finding.sh` | `.sh` | findings DLT 원장 CLI (add/list/resolve/count-open) |
| `plan-search.sh` | `.sh` | 설계문서 섹션 추출(toc/section/grep) — 컨텍스트 최적화 |
| `verify-findings*.workflow.js` | Workflow | 채널 B 검증단(grounded/intrinsic/blind/rubric) |
| `orchestrate-feature.workflow.js` | Workflow | 대규모 코드: 분해→병렬→통합 |
| `supervise-batch.workflow.js` | Workflow | 감독자: 가변 배치 동적 분배 |
| `complete-stubs.workflow.js` | Workflow | bounded stub-완성 루프(가드6) |
| `eval/` | dir | 측정 프레임워크(가설·데이터셋·결과·스코어러) |

`.claude/` (커밋됨): `settings.json`(4훅 + 권한) · `agents/`(검증 커스텀 에이전트 3종).

## 이식성 — 규칙은 *데이터*다

프로젝트 전용 규칙은 스크립트에 하드코딩되지 않고 **`harness.config.json`에 선언**된다(코드 ↔ 데이터 분리). "무엇을 강제하나"(config)와 "어떻게 강제하나"(스크립트)가 분리돼, 새 프로젝트엔 config만 교체하면 된다. 판단형 규칙(`review-protocol.md`)과 캐논(`AGENTS.md`)은 별도로 그 프로젝트 것으로 둔다.

## 더 보기

- 1페이지 요약: [`../../HARNESS.md`](../../HARNESS.md)
- 마스터 가이드·연구·로드맵·트러블슈팅: [`../../docs/harness-engineering/`](../../docs/harness-engineering/)
- 배포 절차: [`DEPLOY_HARNESS.md`](DEPLOY_HARNESS.md)
