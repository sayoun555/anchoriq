# 하네스 엔지니어링 트러블슈팅

하네스를 만드는 동안 **실제로 부딪힌** 문제들. 형식: 문제 → 원인 → 해결 → CS 원리.
(AnchorIQ 앱 자체의 트러블슈팅은 상위 `docs/`의 다른 문서 참고. 여기는 *하네스*에 한정.)

| 문서 | 한 줄 |
|------|-------|
| [HOOK_NOT_FIRING_AFTER_SETTINGS_ADD](HOOK_NOT_FIRING_AFTER_SETTINGS_ADD.md) | 새로 만든 settings.json 훅이 같은 세션에 안 먹음 → 워처 등록 시점 / `/hooks` 리로드 |
| [STOP_HOOK_BLOCKING_FOOTGUN](STOP_HOOK_BLOCKING_FOOTGUN.md) | Stop 게이트를 block으로 두면 모든 턴이 막힘·무한루프 → warn 기본 + `stop_hook_active` 가드 |
| [CHECK_STUBS_KOREAN_FALSE_POSITIVE](CHECK_STUBS_KOREAN_FALSE_POSITIVE.md) | `// 임시` 한국어 마커 누락·오탐 → 코멘트 앵커 정규식 + 신호 강도별 차등 |
| [PASTE_MULTICHAR_DELIMITER_BUG](PASTE_MULTICHAR_DELIMITER_BUG.md) | `paste -d ' · '`가 구분자를 순환·뒤섞음 → awk 결합 |
| [GOVERNING_DOC_OVERMATCHES_DOCS](GOVERNING_DOC_OVERMATCHES_DOCS.md) | 라우터가 `*JWT*.md` 문서를 코드로 오인 → 분류 전 확장자 게이팅 |
| [PLAINTEXT_JWT_IN_SETTINGS](PLAINTEXT_JWT_IN_SETTINGS.md) | settings.local.json에 평문 JWT 누적 → 시크릿 제거 + 정책/계층 분리 |
| [AGENT_HOOK_COST_ON_EVERY_EDIT](AGENT_HOOK_COST_ON_EVERY_EDIT.md) | PostToolUse 적대자가 매 편집마다 비쌈 → 빠른 종료·모델·입도 노브 |
| [WORKFLOW_ARGS_PASSED_AS_STRING](WORKFLOW_ARGS_PASSED_AS_STRING.md) | 검증단 워크플로우가 args를 문자열로 받아 0초 종료 → 직렬화 경계 파싱 가드 |
