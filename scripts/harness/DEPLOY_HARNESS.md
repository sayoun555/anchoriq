# 하네스 배포 — 다른 프로젝트에 옮기기

> 이 하네스는 **config 주도**라, 새 프로젝트엔 *복사 + `harness.config.json` 교체*로 전개한다.
> 핵심 원칙: **"무엇을 강제하나"(config) ↔ "어떻게 강제하나"(스크립트)** 분리. 규칙은 사라지지 않고 *그 프로젝트용으로 다시 선언*된다.

## 빠른 길 — 3가지 전개 방법

| 상황 | 방법 |
|------|------|
| **다른 머신/새 repo** | GitHub 릴리즈 번들 다운로드 (아래) |
| **같은 머신 로컬 타깃** | `bash scripts/harness/install-to.sh <타깃경로>` (압축 불필요) |
| **수동/세밀 제어** | 아래 "절차" 4파일 직접 복사 |

### 릴리즈 번들 (가장 흔한 길)

```bash
# 대상 프로젝트 루트에서 — 받기+풀기+배치 한 줄:
curl -sL https://github.com/sayoun555/anchoriq/releases/download/harness-v1.0.0/harness-bundle.tar.gz | tar xz
bash harness-bundle/install.sh        # scripts/harness/ · .claude/ · CI 게이트를 제자리로
```

그다음 **반드시** 두 가지:
1. `harness.config.json`을 이 프로젝트에 맞게 (AI에게 "이 문서대로 채워줘" — 맨 아래 섹션)
2. **`/hooks` 한 번** (또는 Claude Code 재시작) → 훅이 *로드*돼야 라이브. ← 이게 활성 스위치, AI가 못 누른다.

> 기존 `.claude/settings.json`이 있으면 `install.sh`가 덮지 않고 `settings.harness.json`으로 둔다 → 그 `"hooks"` 블록만 기존 settings에 병합.

## 무엇이 옮겨지나

```
복사 대상 → 새 repo
  scripts/harness/            # 하네스 유닛 전체(스크립트·워크플로우·eval·이 문서)
  .claude/settings.json       # 4훅 + 권한
  .claude/agents/             # 검증 커스텀 에이전트 3종
  .github/workflows/harness-gate.yml   # (선택) CI 게이트
```

## 절차 (사람 또는 AI 에이전트)

### 1. 복사
위 4개를 새 repo에 복사.

### 2. `scripts/harness/harness.config.json` 교체 — *이 한 장이 프로젝트 전용값 전부*

| 필드 | 무엇 | AnchorIQ 예 |
|------|------|------------|
| `designDocsDir` | 설계 문서 디렉토리 | `plan` |
| `source.root` / `mainGlobs` / `extensions` | 소스 위치·main 글롭·검사 확장자 | `backend` / `["anchoriq-*/src/main/java"]` / `["java"]` |
| `build.modulePattern` | 파일경로→빌드 모듈 추출 regex | `anchoriq-(core\|api\|...)` |
| `build.compileCommand` / `testCommand` | `{module}` 치환 컴파일/테스트 명령 | `./gradlew :{module}:compileJava ...` |
| `rules.*` | maxFileLines·maxPublicMethods·coreModule·시크릿/stub 패턴 | 200·7·`anchoriq-core`·… |
| `governingDoc.routableGlobs` | 라우팅 대상 확장자(문서 오탐 방지) | `["*.java", …]` |
| `governingDoc.map` | **경로 글롭 → 지배 문서** (가장 중요) | controller→API_ENDPOINTS … |
| `governingDoc.alwaysForExtension` | 확장자별 항상 붙는 문서 | `java → AGENTS.md` |

> `map`은 **첫 매치 우선**(위→아래). 새 프로젝트의 레이어/패키지 컨벤션에 맞춰 글롭과 문서명을 다시 쓴다.

### 3. 하드코딩 아닌 *판단/프로토콜* 부분 손보기
- `review-protocol.md` — 의미 적대자가 검사하는 규칙(DDD·트랜잭션·캡슐화·계약). 그 프로젝트의 설계 규칙으로 조정.
- `session-context.sh` — design-first 프로토콜 prose의 캐논 문서명(예: `AGENTS.md`, `IMPLEMENTATION_PLAN.md`)을 그 프로젝트 것으로.
- `AGENTS.md`(설계문서 dir 안) — 코딩 규칙 캐논. 프로젝트별로.

### 4. 활성화
`.claude/settings.json`이 새로 생겼으니 `/hooks` 한 번 열거나 Claude Code 재시작 → 훅 라이브.

### 5. (선택) CI 게이트
`.github/workflows/harness-gate.yml`의 컴파일 명령을 프로젝트 빌드에 맞게. `check-stubs.sh`는 config를 읽으니 그대로.

## 검증 (배포 후 동작 확인)

```bash
# 라우팅이 새 프로젝트 문서를 가리키나
echo '{"tool_input":{"file_path":"<새 repo의 컨트롤러 경로>"}}' | bash scripts/harness/governing-doc.sh | jq -r .hookSpecificOutput.additionalContext
# 미완성 마커 스캔이 도나
bash scripts/harness/check-stubs.sh
# 컴파일 grounding이 도나
bash scripts/harness/ground-check.sh <새 repo의 소스파일>
```

## AI 에이전트로 배포할 때

이 하네스는 *생성기*가 아니라 *복사+설정* 모델이다. 에이전트에게: **"DEPLOY_HARNESS.md 절차대로, 이 프로젝트 구조를 스캔해 `harness.config.json`을 채우고 활성화하라"** 고 지시하면, 에이전트가 코드베이스를 보고 docMap·모듈패턴·빌드명령을 채운다. (config가 편집 면을 *한 곳*으로 좁혀, 에이전트가 범용 메커니즘을 깨먹을 위험을 줄인다.)

## 무엇이 *안* 옮겨지나 (홈 repo에 남음)

- `docs/harness-engineering/`(연구·로드맵·트러블슈팅) · `docs/papers/`(논문) · `scripts/harness/eval/`(측정) — 메커니즘의 *근거/지식*이지 배포 짐이 아니다. 배포된 하네스는 이 연구로 *뒷받침*되지만 파일을 짊어지지 않는다.

## 새 버전 릴리즈 내기 (홈 repo 관리자)

번들은 `pack-harness.sh`가 만든다 — *메커니즘만* 담고 연구·런타임(`eval/`·`findings.jsonl`·`docs/`)은 제외.

```bash
bash scripts/harness/pack-harness.sh                 # → dist/harness-bundle.tar.gz
gh release create harness-v1.0.1 dist/harness-bundle.tar.gz \
  --title "AnchorIQ 하네스 v1.0.1" --notes "변경점 …" --latest
# 기존 릴리즈 자산만 교체할 땐:
gh release upload harness-v1.0.0 dist/harness-bundle.tar.gz --clobber
```

번들 내부 구조: `harness-bundle/{scripts/harness, .claude/{settings.json,agents}, .github/workflows, install.sh, USAGE.txt}`. 받는 쪽은 `install.sh`만 실행하면 된다.
