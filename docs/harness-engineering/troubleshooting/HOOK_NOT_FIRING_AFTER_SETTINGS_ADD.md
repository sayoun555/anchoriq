# 새로 추가한 훅이 같은 세션에서 안 먹는다

## 문제

`.claude/settings.json`을 처음 만들어 `SessionStart`/`PreToolUse`/`Stop` 훅을 정의했는데, 같은 세션에서 파일을 편집해도 훅이 발화하지 않았다. 스크립트를 직접 파이프 테스트하면 정상 동작하고, `jq -e`로 스키마도 통과하는데 라이브에서만 조용하다.

## 원인

Claude Code의 설정 워처(watcher)는 **세션이 시작될 때 이미 존재하던 settings 파일이 있던 디렉토리**만 감시한다. 이 리포는 세션 시작 시점에 `.claude/settings.local.json`만 있었고 `.claude/settings.json`은 **이번 세션에서 새로 생성**했다. 워처가 그 신규 파일을 감시 목록에 넣지 못해, 변경이 런타임에 반영되지 않았다.

- `SessionStart` 훅은 구조상 "이미 지나간" 이벤트라 당연히 이번 세션엔 못 탄다.
- `PreToolUse`/`Stop`도 워처가 신규 settings.json을 안 잡으면 로드되지 않는다.

## 해결

1. 스크립트 자체는 합성 stdin으로 **파이프 테스트**해 정확성을 분리 검증한다(훅 배선과 무관하게 로직이 맞는지 확정).
   ```bash
   echo '{"source":"startup"}' | bash scripts/harness/session-context.sh | jq .
   ```
2. settings 스키마는 `jq -e`로 검증(깨진 JSON은 그 파일의 **모든** 설정을 조용히 무력화하므로 필수).
   ```bash
   jq -e '.hooks.PreToolUse[] | select(.matcher=="Edit|Write") | .hooks[].command' .claude/settings.json
   ```
3. 라이브 적용은 사용자가 `/hooks`를 한 번 열거나(설정 리로드) Claude Code를 재시작해야 한다. 이건 에이전트가 대신 못 한다(`/hooks`는 사용자 UI).

## CS 원리

**파일 워칭(inotify/FSEvents)의 등록 시점 문제.** 워처는 "지금 존재하는 경로 집합"을 기준으로 감시 핸들을 건다. 감시 시작 이후에 *새로 생긴* 형제 파일은, 부모 디렉토리를 감시하지 않는 한 이벤트를 못 받는다. 일반화하면 **"감시 대상은 감시 시작 전에 존재해야 한다"** — 설정 핫리로드, 플러그인 자동발견 등에서 반복되는 함정. 그래서 많은 시스템이 "설정 변경 후 리로드/재시작"을 명시적 단계로 둔다.
