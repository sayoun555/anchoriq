# settings.local.json에 평문 JWT 토큰이 쌓여 있었다

## 문제

기존 `.claude/settings.local.json`의 권한 allowlist를 점검하니, 작업 중 자동 누적된 항목 사이에 **평문 JWT 액세스 토큰 3건**이 그대로 박혀 있었다:

```
"Bash(TOKEN=\"<JWT 액세스 토큰 평문 — 문서상 생략>\" curl -s .../api/auth/me ...)"
```

allowlist는 150여 개 항목으로 비대했고, 그중 다수가 일회성 명령(특정 PID `kill`, 특정 `sed` 한 줄, 깨진 `__NEW_LINE_` 잔여물)이었다.

## 원인

권한 프롬프트에서 "허용"을 누를 때마다 **그 명령 전체 문자열**이 allowlist에 그대로 적재된다. 명령에 토큰을 인라인했으면(`TOKEN="<평문 토큰>" curl ...`) 토큰까지 통째로 저장된다. allowlist가 "정책"이 아니라 "수락 로그"로 자라난 결과다.

토큰들은 만료된 로컬 테스트 토큰이라 위험도는 낮지만(localhost·expired), "엔지니어링된 하네스"라면 시크릿이 설정 파일에 평문으로 남아선 안 된다.

## 해결

1. **시크릿 제거**: 토큰을 인라인한 모든 항목 삭제. 검증:
   ```bash
   grep -rIn -E "eyJ|TOKEN=|Bearer " .claude/   # → 0건이어야 함
   ```
2. **누적 로그 → 정책으로 정리**: 일회성 명령 150여 개를 일반화 규칙로 압축(`Bash(./gradlew *)`, `Bash(docker compose *)` 등). 개인 도구만 `settings.local.json`에 남기고(42개), 팀 공통 규칙은 커밋되는 `settings.json`으로 이동.
3. **계층 분리 + gitignore**: `settings.json`(팀, 커밋) vs `settings.local.json`(개인, gitignore). `.gitignore`를 `.claude/*` + `!.claude/settings.json`로 바꿔 **팀 하네스만 추적, 개인/비밀은 제외**.
4. **가드레일 추가**: `deny`에 `Read(./**/.env)`를 넣어 시크릿이 모델 컨텍스트로 새는 경로를 원천 차단.

## CS 원리

**Secret hygiene & 최소 권한.** 시크릿은 코드/설정과 분리(12-factor의 config-in-env)되어야 하고, 명령줄 인라인은 셸 히스토리·프로세스 목록(`ps`)·설정 로그 등 **여러 곳에 사본을 남기는** 안티패턴이다(환경변수나 헤더 파일을 쓰는 이유). 또한 권한 모델은 **수락의 누적**이 아니라 **의도된 정책**이어야 한다 — allowlist가 로그처럼 자라면 최소 권한이 깨지고 감사가 불가능해진다. "추적되는 팀 정책 / 무시되는 개인·비밀"의 계층 분리는 설정 관리의 기본 형태다.
