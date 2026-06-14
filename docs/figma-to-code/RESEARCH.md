# Figma MCP → 코드화 — 딥서치 자료 (누적)

> 질문: "Figma MCP로 진짜 좋은 성능 가능한가? 매번 실패하던데." 공식 + 실전 출처로 정직하게.
> 이 문서는 *계속 추가*한다(새 자료는 맨 아래 "추가 자료"에).

## TL;DR (얼마큼 되나 — 정직)

- **원샷 "디자인→완벽한 코드"는 아니다.** Figma 공식도 명시: MCP는 *Figma↔IDE 다리*지 "한 번에 프로덕션 코드" 도구가 아님. 생성물은 **시작점**, 수동 보정 필수.
- **나이브하게 쓰면 실패한다** — 경험 많은 개발자들이 *85~90% 부정확*을 보고(공식 서버 베타). 네가 "매번 실패"하는 게 비정상이 아니라 *기본값*이다.
- **제대로 셋업하면 좋아진다** — Code Connect + 디자인토큰 + 좋은 파일구조면 **초기 개발 시간 50~70% 절감**(품질 유지). 투자 회수는 보통 2~3개 기능 구현 후.
- 한 줄: **"마법이 아니라 *컨텍스트가 필요한 주니어 엔지니어*처럼 다뤄라."** 셋업이 성능을 결정한다.

## 왜 매번 실패하나 (실패 모드)

1. **★ 일방통행 — AI가 자기 코드의 *렌더 결과*를 못 본다.** 생성 후 "이게 실제로 디자인처럼 보이나, 다른 파일의 전역 스타일이 덮었나, 컴포넌트가 깨졌나"를 *확인할 수단이 없음*. → 이게 가장 근본적. (우리 하네스 원리와 직결 — 아래 §연결.)
2. **컨텍스트 윈도우 초과** — 복잡한 Figma 페이지의 디자인 데이터는 거대해서 한 호출에 안 들어가 *잘림(truncation)*.
3. **복잡한 디자인 시스템에 약함** — Code Connect 있어도 복잡 셋업이면 성능 저하.
4. **컨텍스트 오염(context poisoning)** — 공식 MCP가 *처방적(prescriptive)* React+Tailwind를 `leading-[22.126px]` 같은 임의값으로 내보내면, AI가 *네 디자인 시스템 클래스 대신 그 임의값을 흉내냄*.
5. **나쁜 입력 = 나쁜 출력** — 레이어 네이밍 엉망·auto layout 없음·detached instance·절대좌표면 코드도 엉망.
6. 연결 실패·메모리 과다 등 기술적 불안정(베타).

## 두 가지 MCP (뭘 쓰냐가 다름)

| | 공식 Figma Dev Mode MCP | Framelink / GLips `figma-context-mcp` |
|---|---|---|
| 비용 | **Dev/Full seat 필요**(유료) | **무료·오픈소스**, 아무 계정 |
| 접속 | 원격 `mcp.figma.com/mcp`(권장) 또는 데스크탑 `localhost:3845` | Figma API 사용 |
| 출력 | *처방적* — React+Tailwind 코드 직접(임의값 → 오염 위험) | *서술적(descriptive)* — 구조/스타일 데이터만 → AI가 적절한 클래스 선택 |
| 도구 | get_code · get_design_context(변수·컴포넌트·레이아웃) · get_variable_defs · get_code_connect_map · 캔버스 쓰기 등 | **`get_figma_data`**(구조·스타일·레이아웃) · **`download_figma_images`**(이미지 자산) |
| 강점 | Code Connect 네이티브 연동 | 오염 회피(서술적), 무료 |

> **이 프로젝트에 붙은 건 Framelink 계열**(`get_figma_data`·`download_figma_images`) — *서술적*이라 오염엔 강하지만, *Code Connect 컴포넌트 매핑*은 공식 쪽이 강함.

## 좋은 성능의 5개 레버 (실패→성공)

1. **Figma 파일 구조** (입력 품질 = 출력 품질):
   - ✅ **auto layout 전부**(컨테이너·입력·버튼) — CSS Flexbox로 직역됨
   - ✅ **변수/디자인 토큰**(간격·색·타이포) — 한 곳 바꾸면 전체 반영, AI가 안정적으로 해석
   - ✅ **컴포넌트 + 베리언트**(default/full-width/loading) — 중복 대신 *상태*로
   - ✅ **명확한 레이어 네이밍 + 계층** (Rename It 같은 플러그인)
   - ❌ detached instance · 절대좌표/수동 패딩 · 어수선한 구조
2. **Code Connect** (정확도 최대 레버) — Figma 컴포넌트 ↔ *네 repo의 실제 코드 컴포넌트* 매핑. 그러면 Dev Mode/MCP가 *진짜 프로덕션 스니펫*을 반환(자동생성 대신).
3. **디자인 토큰에 code syntax** — 변수에 `var(--color-bg-primary)` 연결. **네이밍 정확 일치**(`primary.default`면 Figma도 `primary.default`, `Primary/Main` 금지).
4. **코드베이스 컨텍스트 주입** — 컴포넌트 폴더마다 *사용 예 2~3개 담은 README*, AI용 마크다운 온보딩 문서, *프롬프트에 까다로운 요구 명시*("이 카드는 loading 상태 필요"). "마법이 아니라 컨텍스트 필요한 주니어처럼."
5. **서술적 출력 선호** — Framelink로 오염 회피, 또는 공식 쓸 땐 임의값을 디자인시스템 클래스로 정규화하는 규칙.

## 우리 하네스와의 연결 (★ 핵심 인사이트)

**실패 모드 #1(일방통행 — 렌더 결과 못 봄)은 어떤 MCP도 안 고쳐준다.** 그리고 그게 *정확히 우리 하네스 연구의 빈칸*이다:
- 우리 결론: *"검증의 값은 런타임/비추론 경계에"* (v-next). figma→코드 충실도는 *렌더한 픽셀*을 봐야 알 수 있는 **런타임 비추론 정보** — 코드만 봐선 추론 불가.
- → **figma용 하네스 = 시각 grounding 렌즈**: 생성 컴포넌트를 *렌더(Playwright/Chrome DevTools MCP) → 스크린샷 → Figma 디자인(figma MCP 이미지)과 비교 → 어긋남 적발*. 이게 MCP가 못 주는 "검증" 절반.
- 단 시각 검증은 노이즈 큼(허용오차·LLM 시각판정 확률적) — "명백한 어긋남"(요소 누락·색 완전 다름)엔 강하고 미세 간격엔 약함.

## ★★ 진짜 해법 — *측정 기반 자기교정 루프* (네 실패 #1의 답)

검증된 해법. 핵심 반전: **"스크린샷 시각 비교"가 아니라 *숫자 측정*이다.** ([vadim.blog 2026](https://vadim.blog/pixel-perfect-playwright-figma-mcp))

루프(3단계, *기계적*):
1. **디자인 스펙 읽기** — Figma MCP/REST로 폰트크기·간격·구조를 *픽셀값*으로
2. **렌더 출력 측정** — headless **Playwright를 "측정 도구"로**: `getComputedStyle()`·`getBoundingClientRect()`로 실제 픽셀값 추출 (스크린샷 diff 아님!)
3. **숫자 델타로 교정** — "heading 28px→24px, gap 4px→8px" 처럼 *구체 수치 목표*를 AI에 피드백

> *"이 루프는 기계적이다. 교정에서 미적 판단을 제거한다 — 숫자가 뭐가 틀렸는지 말한다."* → **LLM 시각판정(확률적)보다 결정론적** = 우리 v-next 결론과 일치, 오히려 더 셈.

**현실 체크**(같은 글): Figma MCP **rate limit 빡셈**(스타터 6콜/월) + stateless → **REST API가 신뢰 fallback** · Playwright MCP resize 후 컨텍스트 손실 · **토큰 매핑 갭**(`space-6`이 24px 아닐 수도) · LLM *반응형 공간추론* 약함 · **첫 패스엔 시각비교 금지**(스펙으로 먼저 생성→그다음 측정) · 쓸 곳=랜딩/마케팅/디자인시스템준수, 건너뛸 곳=내부 대시보드.

## 전용 도구 (바로 쓰는 시각 QA)

- **[UIMatch](https://github.com/kosaki08/uimatch)** — Playwright 기반. *Figma(fileKey:nodeId) vs 렌더 UI* 비교: 픽셀 + **ΔE2000 색차** + **Design Fidelity Score(0~100)** + CI 품질게이트. **Claude Code용 skills 내장** → 우리 하네스 렌즈로 바로 붙일 수 있음.
  ```bash
  npm i -g @uimatch/cli playwright && npx playwright install chromium
  export FIGMA_ACCESS_TOKEN="figd_..."
  npx @uimatch/cli compare figma=<fileKey>:<nodeId> story=http://localhost:6006/?path=/story/button selector="#root button"
  ```
- **[Applitools Eyes Figma 플러그인](https://applitools.com/solutions/figma/)** — Visual AI 디자인 vs 코드(상용).

## 최강 대안 툴 (실측, 2026)

| 툴 | 강점 | 약점 |
|---|---|---|
| **Builder.io Visual Copilot/Fusion** | **시맨틱 컴포넌트 매핑 — 네 기존 React 컴포넌트에**(코드베이스 통합), 최다 프레임워크. 디자인시스템 팀 최적 | 셋업 |
| **Locofy** | 깔끔한 컴포넌트 구조·flexbox, 모바일·멀티프레임워크 | 복잡 반응형 약함, 고정 px |
| **Anima** | (시각 OK) | *literal*(절대좌표·flat HTML="스크린샷을 HTML로"), 코드 나쁨 |
| **Kombai** | 무료 300크레딧/월·Pro $20 | — |
| **Figma Make** | Figma 자체(2026.2, **OpenAI 협업**), 챗 편집+MCP+실제 디자인시스템 | 신생 |

> **Codex 쓰는 너**: Figma MCP가 **2026.2 OpenAI 협업으로 Codex 직접 연동**(픽셀 export 아닌 *라이브 컴포넌트 계층·토큰·Code Connect 매핑*). [Codex+Figma MCP 워크플로우](https://codex.danielvaughan.com/2026/03/27/codex-cli-figma-mcp-design-to-code/).

## 최신 공식 (2026)
- 도구: `get_design_context`(선택영역 구조화 React+Tailwind=시작점) · `get_variable_defs`(변수·토큰) · **Skills**(도구 사용 순서 가이드, [figma/mcp-server-guide](https://github.com/figma/mcp-server-guide))
- **원격 MCP `mcp.figma.com/mcp` 권장**(데스크탑 localhost:3845보다). 산업: 제품팀 **65%+** AI 코드생성 사용.

## ★ 프론트 *생성* 렌즈 (검증 말고 생성을 잘하게 — 하네스에 배선됨: `presets/frontend.footguns.json` + AGENTS §2.6)

Figma→코드는 ①생성(프론트 작업)+②검증(figma-check). 생성을 잘하게 하는 footgun(컴파일은 되나 *런타임 깨짐/느려짐/디자인 이탈*):

**하이드레이션**(Next App Router AI 코드 #1 실패, [oneuptime 2026](https://oneuptime.com/blog/post/2026-01-24-fix-hydration-mismatch-errors-nextjs/view)):
- render 중 `window`/`document`/`localStorage` → useEffect · `new Date()`/`Math.random()` → useEffect/`useId` · hook/브라우저API 쓰면 `'use client'` · `<div>` in `<p>` 금지.

**Context poisoning**(디자인 이탈, [eslint-plugin-tailwindcss](https://github.com/francoismassart/eslint-plugin-tailwindcss/blob/master/docs/rules/no-arbitrary-value.md)):
- Tailwind 임의값 `text-[#333]`·`leading-[22.126px]`·`w-[437px]` = 토큰 우회. `no-arbitrary-value` 규칙으로 강제(`border-4` not `border-[4px]`). Figma MCP 출력 그대로 베끼지 말 것. + 기존 컴포넌트 재사용(variant 확장).

**성능**([Vercel 공식 React Best Practices](https://vercel.com/blog/introducing-react-best-practices), 영향순):
- CRITICAL: 비동기 워터폴 제거(순차 await→`Promise.all`, 600ms) · 번들↓(300KB > useMemo 미세최적). 무거운 렌더(차트·md)는 **Server Component**로, `'use client'` 경계 최소.
- 재렌더: `useState(()=>JSON.parse(...))` lazy init, 루프 합치기. INP>200ms면 JS 줄이고 defer.
- **`npx skills add vercel-labs/agent-skills`** → 이 규칙들을 AGENTS.md에 컴파일(Claude Code 등 에이전트가 자동 적발).

→ **결정론 보강**: `eslint-plugin-tailwindcss`(no-arbitrary-value) + tsc + Vercel skills를 `build.compileCommand`/lint에. **의미**: 위 footgun을 적대자(review.sh)가. **검증**: figma-check.

## ★ 놓쳤던 것 (2차 보강)

**① 접근성(a11y) — AI 생성코드가 *기본으로 빠뜨리는* 큰 축** ([LogRocket](https://blog.logrocket.com/ai-has-an-accessibility-problem/)). *"별도 프롬프트 없으면 a11y는 AI 코드의 기본이 아니다"*(AI는 *인기 있는* 답을 내는데, 사람이 a11y를 기본으로 안 쓰니 AI도 안 씀). 구체:
- div/span+onClick(키보드 안 됨) → `<button>`/`<a>` 네이티브 · 커스텀 모달 → `<dialog>`(포커스·ESC 내장)
- `<img>` alt · `<input>`↔`<label for=id>` · 아이콘버튼 aria-label · 모달 `role=dialog`+`aria-modal`+`aria-labelledby`
- 토글요소 autofocus·포커스트랩 · `:focus-visible` · 동적콘텐츠 `aria-live` · `prefers-reduced-motion`
- **결정론 도구: `eslint-plugin-jsx-a11y`.** → 하네스 footgun에 추가됨(a11y-* 3개).

**② Design2Code 벤치마크 (Stanford/NAACL 2025, 엄밀)** ([arxiv 2403.03163](https://arxiv.org/abs/2403.03163)) — *최초의 실세계 디자인→코드 벤치마크*, **484 웹페이지** + 자동지표 + 사람평가. GPT-4V/4o·Gemini·Claude 평가, **GPT-4V 최고**. 핵심: 모델이 *(a) 시각 요소 회수(요소 누락) + (b) 레이아웃 생성*에서 가장 약함. **방법론 교훈(우리와 직결): 도메인-작성 루브릭 > LLM-작성 루브릭(kappa 0.60 vs 0.46)** = 우리 하네스의 "결정론 측정·도메인 규칙" 철학을 학술이 뒷받침.

**③ 반응형·테밍 (LLM 약점)** ([디자인시스템 컬렉티브 2025/26](https://www.designsystemscollective.com/design-system-mastery-with-figma-variables-the-2025-2026-best-practice-playbook-da0500ca0e66)) — Figma **variable modes**(light/dark/breakpoint) → 시맨틱 토큰 aliasing(`color-bg-primary` per 모드)·반응형 토큰(`breakpoint-md=768`). AI는 반응형 공간추론이 약하니 *명시*. → 하네스 footgun에 theming 추가됨.

**④ 에이전틱 빌더 (최신, 2026)** — **v0(Vercel)**: 프론트 전용, *프로덕션 React+Tailwind*, Next/Vercel 생태계 — *너한테 가장 맞음*. **Lovable**: 풀스택 MVP(React+Supabase). **Bolt**: 빠른 프로토타입. **Stitch(Google)**: 디자인 목업. (Onlook=신생 비주얼 에디터, 문서 적음.) 선택: 목업→Stitch, 프로덕션 컴포넌트→v0, 앱→Lovable, 빠른 풀스택→Bolt.

## ★ 4차 보강 (보안·데이터·Code Connect·토큰 파이프라인·현장교훈)

**⑤ 보안·데이터·hooks footgun — "AI 코드 40% 보안결함", 빠졌던 축** ([9 patterns](https://theroadtoenterprise.com/blog/vibe-coding-vs-production-coding-react) · [보안감사](https://vitalii4reva.medium.com/40-of-ai-generated-code-has-security-flaws-your-code-review-wont-catch-them-0a02a13848fe)):
- **데이터페칭 useEffect = AI 최다 안티패턴** — abort(언마운트 누수)·캐시·재시도·에러·dedup 없음 → **TanStack Query / Server Component**.
- **XSS**: `dangerouslySetInnerHTML` 미살균 → DOMPurify/제거. **클라 API키 하드코딩** → 서버 프록시.
- rules-of-hooks(조건부 hook·stale closure), 인라인 함수가 memo() 깸(useCallback), optimistic UI 롤백 없음, 중복 유틸. → footgun 4개 추가(server-state·security·rules-of-hooks·i18n).

**⑥ Code Connect 셋업 HOW (정확도 최대 레버)** ([CLI](https://developers.figma.com/docs/code-connect)):
```bash
npm i -g @figma/code-connect@latest          # Node 18+ + Figma 토큰
npx figma connect --token=$TOKEN             # 대화형: 컴포넌트 디렉토리·디자인파일 매핑
npx figma connect publish --token=$TOKEN     # Dev Mode/MCP에 *진짜 프로덕션 스니펫* 노출
```
UI(시각·빠름·언어무관) vs **CLI(정밀·property 매핑·동적 예제)**. React/Web Components/SwiftUI/Compose. **Storybook 통합** 권장.

**⑦ 디자인 토큰 파이프라인 (오염 방지 인프라)** ([DTCG 가이드](https://tasteprofile.io/blog/w3c-dtcg-design-tokens-practical-guide)):
- **W3C DTCG 표준 2025.10 안정화** — 벤더 종속 줄이는 토큰 JSON 표준. 신규 시스템은 DTCG로.
- 파이프라인: **Tokens Studio(Figma 플러그인)** → JSON → GitHub → **Style Dictionary(Amazon)** → CSS 변수/Tailwind config. → 임의값(context poisoning) 원천 차단.

**⑧ 현장 교훈 (production 팀, 2026)** ([925studios](https://www.925studios.co/blog/top-figma-to-code-agencies-production-2026)): **"AI 핸드오프는 지름길이 아니라 *규율*에 보상한다."** 깨끗한 컴포넌트 시스템 팀 = **50~70% 절감**, ad-hoc Figma 파일(체계적 네이밍·구조 없음) = **이득 0**. 실제 제약 = rate limit·Code Connect 투자·Figma 위생. **Codex+Figma MCP가 2026 가장 실질적 다리.** Figma Make는 이제 로컬 코드에서도.

## 출처

- Figma 공식 — MCP 서버 가이드: https://help.figma.com/hc/en-us/articles/32132100833559-Guide-to-the-Figma-MCP-server
- Figma 공식 — Dev Mode MCP 소개 블로그: https://www.figma.com/blog/introducing-figma-mcp-server/
- Figma 공식 — 개발자 문서: https://developers.figma.com/docs/figma-mcp-server/
- Figma 공식 — Code Connect: https://help.figma.com/hc/en-us/articles/23920389749655-Code-Connect
- Figma 공식 — MCP 서버 가이드 repo(skills·code-connect-setup): https://github.com/figma/mcp-server-guide
- 실측 테스트/벤더 비교(정량은 약함): https://aimultiple.com/design-to-code
- Builder.io — Design to Code with Figma MCP(오염·실전): https://www.builder.io/blog/figma-mcp-server
- LogRocket — Figma 파일 구조법: https://blog.logrocket.com/ux-design/design-to-code-with-figma-mcp/
- Framelink/GLips figma-context-mcp(서술적 MCP): https://github.com/glips/figma-context-mcp · https://www.framelink.ai/
- Locofy — Figma MCP 단점(엔터프라이즈): https://www.locofy.ai/resources/figma-mcp-disadvantages
- Kombai — Figma MCP 가이드·한계: https://kombai.com/figma/figma-mcp-guide/
- alexbobes — CTO 가이드(2026): https://alexbobes.com/tech/figma-mcp-the-cto-guide-to-design-to-code-in-2026/
- ★ vadim.blog — Pixel-Perfect Playwright+Figma MCP(측정 루프, 무엇이 실제로 됨): https://vadim.blog/pixel-perfect-playwright-figma-mcp
- ★ UIMatch — Figma vs 렌더 UI 시각 QA(ΔE2000·DFS·Claude skills): https://github.com/kosaki08/uimatch
- Applitools — Figma 시각 테스트: https://applitools.com/solutions/figma/
- Figma MCP + Claude Code + Playwright MCP 경험기: https://javascript.plainenglish.io/experience-story-figma-mcp-claude-code-playwright-68b20bb0f8ce
- Codex CLI + Figma MCP 워크플로우: https://codex.danielvaughan.com/2026/03/27/codex-cli-figma-mcp-design-to-code/
- 툴 비교(Builder/Locofy/Anima 실측): https://www.sixtythirtyten.co/blog/from-figma-to-code-ai-design-to-dev-workflows-in-2026
- Figma 릴리즈 노트(최신 추적): https://www.figma.com/release-notes/
- ★ Vercel — React Best Practices(성능 규칙, AGENTS 컴파일): https://vercel.com/blog/introducing-react-best-practices
- ESLint — no-arbitrary-value(토큰 강제): https://github.com/francoismassart/eslint-plugin-tailwindcss/blob/master/docs/rules/no-arbitrary-value.md
- Next.js 하이드레이션 mismatch 원인·예방(2026): https://oneuptime.com/blog/post/2026-01-24-fix-hydration-mismatch-errors-nextjs/view
- Evil Martians — Tailwind 카오스 방지 5원칙: https://evilmartians.com/chronicles/5-best-practices-for-preventing-chaos-in-tailwind-css
- Next.js 프로덕션 최적화 체크리스트: https://nextjs.org/docs/app/guides/production-checklist
- ★ Design2Code 벤치마크(Stanford/NAACL 2025): https://arxiv.org/abs/2403.03163 · https://aclanthology.org/2025.naacl-long.199/
- ★ AI 코드 접근성 문제(a11y footgun): https://blog.logrocket.com/ai-has-an-accessibility-problem/
- eslint-plugin-jsx-a11y(결정론 a11y): https://github.com/jsx-eslint/eslint-plugin-jsx-a11y
- Figma variables/테밍 플레이북(2025/26): https://www.designsystemscollective.com/design-system-mastery-with-figma-variables-the-2025-2026-best-practice-playbook-da0500ca0e66
- 에이전틱 빌더 비교(v0/Lovable/Bolt 2026): https://www.nxcode.io/resources/news/vibe-design-tools-compared-stitch-v0-lovable-2026
- ★ 9 patterns that fail in production(AI React footgun): https://theroadtoenterprise.com/blog/vibe-coding-vs-production-coding-react
- AI 코드 보안결함 40%: https://vitalii4reva.medium.com/40-of-ai-generated-code-has-security-flaws-your-code-review-wont-catch-them-0a02a13848fe
- Code Connect 개발자 문서(CLI): https://developers.figma.com/docs/code-connect
- W3C DTCG 디자인 토큰 실전 가이드: https://tasteprofile.io/blog/w3c-dtcg-design-tokens-practical-guide
- Figma→코드 production 에이전시 현장(규율>지름길): https://www.925studios.co/blog/top-figma-to-code-agencies-production-2026
- Figma Make, 로컬 코드에서: https://www.figma.com/blog/figma-make-now-on-your-local-code/

## 추가 자료 (네가 찾아온 것 — 계속 추가)

<!-- 새 자료: 제목 · URL · 한 줄 요약 -->
