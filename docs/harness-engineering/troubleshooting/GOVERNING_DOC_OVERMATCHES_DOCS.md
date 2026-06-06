# 경로 라우터가 문서 파일명을 코드로 오인한다

## 문제

이 troubleshooting 디렉토리의 `PLAINTEXT_JWT_IN_SETTINGS.md`를 작성하는 순간, PreToolUse 라우터(`governing-doc.sh`)가 발화하며 엉뚱한 안내를 주입했다:

```
📐 설계 준수: 이 파일을 지배하는 문서 → plan/AUTH_PAYMENT.md(JWT/Stripe·Toss/플랜) · plan/TRANSACTION_DESIGN.md(Tier1 강한일관성).
```

마크다운 문서인데 인증/결제 설계 문서를 가리켰다. (역설적으로, 이건 하네스가 **라이브로 작동한다는 증거**이기도 했다.)

## 원인

`governing-doc.sh`의 매핑은 경로 **부분 문자열**로 동작한다:
```bash
*payment*|*Jwt*|*JWT*|*/security/*) add "plan/AUTH_PAYMENT.md" ...
```
파일명 `PLAINTEXT_JWT_IN_SETTINGS.md`에 `JWT`가 들어 있어 매칭됐다. 라우터가 "코드 파일"이라는 전제 없이 **모든 경로**에 패턴을 적용한 게 원인. 확장자/디렉토리 가드가 없었다.

## 해결

case 매핑 **앞에** 코드/설정 파일 가드를 둔다. 그 외(문서 등)는 즉시 통과:

```bash
case "$FILE" in
  *.java|*.kt|*.ts|*.tsx|*.yml|*.yaml|*.gradle|*.gradle.kts|*Dockerfile|*docker-compose*) : ;;
  *) exit 0 ;;   # 코드/설정이 아니면 라우팅 안 함
esac
```

검증:
```bash
# .md (JWT 포함 파일명) → 침묵
echo '{"tool_input":{"file_path":"docs/.../PLAINTEXT_JWT_IN_SETTINGS.md"}}' | bash scripts/harness/governing-doc.sh
# .java 컨트롤러 → 정상 라우팅
echo '{"tool_input":{"file_path":"backend/.../VesselController.java"}}'    | bash scripts/harness/governing-doc.sh
```

## CS 원리

**부분 문자열 매칭은 컨텍스트를 모른다.** 분류기(라우터)에 입력 도메인 제약을 걸지 않으면, 의도한 모집단(코드) 밖의 입력(문서)에 규칙이 새어 적용된다 — 본질적으로 false positive다. 해결의 정석은 **분류 전에 입력을 게이팅**하는 것(타입/확장자/네임스페이스로 모집단을 먼저 좁힘). 라우팅 규칙을 더 정교하게 만드는 것보다, "무엇이 분류 대상인가"를 먼저 정의하는 게 싸고 견고하다. 컴파일러가 파일 확장자로 처리기를 고르고, MIME 타입으로 핸들러를 라우팅하는 것과 같은 원리.
