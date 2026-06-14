# AGENTS.md — 이 저장소의 에이전트 지침 (템플릿)

> 이 파일은 **agents.md 표준**(Linux Foundation Agentic AI Foundation)이다. Codex·Cursor·Aider·Copilot·Windsurf·Gemini CLI·Zed 등이 자동으로 읽는다. Claude Code는 CLAUDE.md에서 이 파일을 `@import`하면 된다.
> **이식 시: 아래 `«»` 부분을 이 프로젝트 값으로 채우고, 강제는 `harness.config.json`이 담당한다.** (≤32KiB 유지 — Codex가 그 이상은 자른다. 길면 하위 디렉토리로 분할.)

## 0. 설계 우선 (코드 전에 학습 — 예외 없음)
코드 한 줄 쓰기 전에 «설계 문서 디렉토리(예: plan/ docs/)»의 관련 문서를 읽는다. 학습 후 더 나은 방법으로 바꾸는 건 OK(근거 필수). **학습 없이 자기 방식은 금지.** 어떤 파일이 어떤 설계 문서의 지배를 받는지는 `harness.config.json`의 `governingDoc.map` 참조.

## 1. 코딩 원칙 (보편 — 프로젝트에 맞게 조정)
- **Clean Code**: 의도가 드러나는 네이밍, 메서드 단일 책임, 주석 없이 읽히는 코드.
- **OOP**: 캡슐화, Tell-Don't-Ask, getter/setter 남발 금지, 원시 타입 포장(VO), 일급 컬렉션.
- **SOLID**: SRP(길어지면 분리)·OCP(인터페이스+다형성)·DIP(추상화 의존).
- **DDD**(해당 시): 풍부한 도메인 모델, 비즈니스 로직은 Entity/도메인에, Application 서비스는 오케스트레이션만.
- **인터페이스 우선**(*실제* 다형성/교체/외부경계가 있을 때만 — 단일 구현·고정 순수함수에 인터페이스 얹는 *과설계*는 금지, YAGNI).
- **정석적 해결**: 임시방편·꼼수·우회 금지. 공식 권장 방식.
- **시크릿**: 비밀번호·키·토큰 하드코딩 절대 금지 → 환경변수/시크릿 매니저.

## 2. 강제(하네스) — 이건 권고가 아니라 *구조*다
이 repo엔 결정론 게이트가 배선돼 있다. **프롬프트의 규칙은 권고, 훅의 규칙은 구조적.**
- **커밋 시** `pre-commit` 훅이 `.harness/check.sh`를 돌린다 → *미완성 마커(stub/TODO/mock)·하드코딩 시크릿이 있으면 커밋이 차단된다*. 우회(`--no-verify`/`-n`) 금지 — 막혀 있고(git 래퍼), 막혀도 **CI에서 다시 잡힌다.**
- **편집 직후**(Codex/Claude 훅 설치 시) 같은 검사가 돈다.
- **의미 검토**: `harness.config.json`의 `review.footguns`에 적힌 *컴파일은 되나 런타임에 깨지는* 스택 함정(예: Spring `@Transactional` 자기호출, JPA N+1 등 — **프로젝트마다 교체**)을 의미 적대자(`review.sh`)가 점검. 이게 하네스의 *측정된 핵심 가치*(검증)다.

→ 훅이 실패하면 **우회하지 말고 원인을 고쳐라.**

## 2.5 연속성 (긴/다세션 작업)
긴 작업은 `progress.md`(=`harness.config.json`의 `state.progressFile`)에 **완료 / 남은 일 / 막힌 점 / 다음 단계**를 적어두며 진행하라. SessionStart에 `session-context.sh`가 이걸 *재주입*하므로, 세션이 끊겨도(컨텍스트 소실/압축) 이어갈 수 있다. 짧은 작업엔 비워둬도 무해. (정적 지침은 이 AGENTS.md, *세션 간 상태*는 progress.md — 역할 분리.)

## 2.6 프론트엔드 생성 (Figma→코드 — UI 프로젝트만)
디자인을 코드로 옮길 때 *생성*은 아래 규칙으로, *검증*은 `figma-check.sh`(렌더 vs 디자인 측정)로. 근거: `docs/figma-to-code/RESEARCH.md`. footgun은 `presets/frontend.footguns.json`.
- **디자인 토큰만, 임의값 금지** — `text-[#333]`·`leading-[22.126px]` 같은 Tailwind 임의값은 *디자인시스템 우회(context poisoning)*. config 토큰/유틸리티로. **Figma MCP가 뱉은 임의값을 그대로 베끼지 마라.** 결정론 보강: `eslint-plugin-tailwindcss`의 `no-arbitrary-value`.
- **기존 컴포넌트 재사용** — 디자인시스템에 있으면 새로 만들지 말고 재사용(병렬 구조 난립 금지). variant로 확장. (Code Connect 정신.)
- **하이드레이션 안전(Next App Router)** — render 중 window/document/localStorage·`new Date()`/`Math.random()` 금지(→useEffect/useId). hook·브라우저 API 쓰면 `'use client'` 명시. 잘못된 HTML 중첩(`<div>` in `<p>`) 금지.
- **성능** — 무거운 렌더(차트·마크다운)는 Server Component로(번들↓), `'use client'` 경계 최소화. render마다 무거운 계산은 `useState(()=>…)` lazy init. 순차 await 워터폴 대신 `Promise.all`. (Vercel React Best Practices: `npx skills add vercel-labs/agent-skills`.)
- **접근성(a11y) — AI가 기본으로 빠뜨리는 축, 의식적으로** — div/span+onClick 말고 `<button>`/`<a>`(키보드·role 내장), 모달은 `<dialog>`. `<img>` alt·`<input>` `<label for>`·아이콘버튼 aria-label·동적콘텐츠 aria-live·`prefers-reduced-motion`. 결정론: `eslint-plugin-jsx-a11y`.
- **테밍/반응형** — 하드코딩 색 대신 *시맨틱 토큰*(다크모드·멀티브랜드). Figma variable modes(light/dark/breakpoint) → CSS var. 반응형은 LLM 약점이라 명시적으로.
- **검증** — 컴포넌트 완성 후 `bash .harness/figma-check.sh <컴포넌트>` 로 렌더 vs Figma 측정 비교(델타로 수정). 첫 패스엔 시각비교 말고 스펙으로 생성→그다음 측정.

## 3. 빌드 / 테스트
- 빌드: `«빌드 명령»`  ·  테스트: `«테스트 명령»`  (정식은 `harness.config.json`의 `build.*`)
- 커밋 전 로컬에서 한 번: `bash .harness/check.sh`

## 4. 커밋 / PR
- 작은 단위로 커밋. 메시지는 *무엇을·왜*. 
- `--no-verify`로 훅 건너뛰지 않는다.
- 트러블슈팅을 했다면 «docs 디렉토리»에 기록한다.

---
*프로젝트 전용 값은 전부 `.harness/harness.config.json` 한 곳에. 이 AGENTS.md는 사람과 모든 에이전트가 읽는 단일 진실원이다.*
