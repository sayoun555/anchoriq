# 검증단 워크플로우가 0초 만에 "검증할 게 없음"으로 끝난다

## 문제

`verify-findings.workflow.js`(다수결 검증단)를 finding 2건을 `args`로 넘겨 실행했는데, **에이전트가 0명 돌고 7ms 만에** 종료했다:

```json
{"verdicts":[],"note":"no open findings"}
```

스크립트는 멀쩡한데 입력을 못 받은 것처럼 보였다.

## 원인

스크립트의 입력 가드가:
```js
const findings = Array.isArray(args) ? args : []
```
인데, `args`가 **배열이 아니라 JSON 문자열**로 들어왔다. Workflow 도구에 `args`를 넘길 때, 배열을 그대로 전달한다고 생각했지만 직렬화 경로에서 **문자열로 인코딩**돼 도착했다(`"[{...},{...}]"`). `Array.isArray("[...]")`는 `false` → 빈 배열 → 즉시 early return.

Workflow 문서가 명시한 함정 그대로다: *"Pass arrays/objects as actual JSON values, NOT as a JSON-encoded string — a stringified list reaches the script as one string, so `args.filter`/`args.map` throw."*

## 해결

스크립트를 **방어적으로** 만들어 문자열로 와도 파싱한다(전달 경로에 무관하게 견고):
```js
let _args = args
if (typeof _args === 'string') {
  try { _args = JSON.parse(_args) } catch (e) { _args = [] }
}
const findings = Array.isArray(_args) ? _args : (Array.isArray(_args?.findings) ? _args.findings : [])
```
재실행하니 정상 동작 — 6 에이전트(finding 2 × 렌즈 3), 192k 토큰, 78초, confirmed 0 / refuted 2.

## CS 원리

**직렬화 경계(serialization boundary)에서의 타입 손실.** 프로세스/도구 경계를 넘는 값은 인코딩(보통 JSON 문자열)됐다가 디코딩된다. 한쪽이 "객체를 보냈다"고 믿어도 다른 쪽은 문자열을 받을 수 있다 — 분산 시스템에서 흔한 "타입은 와이어를 건너지 못한다" 문제. 정석 해법은 **경계에서 입력을 정규화(parse/validate)**하는 것: 신뢰할 수 없는 입력은 받는 쪽에서 타입을 강제(coerce)하고 스키마로 검증한다(Postel의 법칙 — "보내는 건 엄격히, 받는 건 관대히"). 워크플로우/큐/웹훅 입력은 항상 받는 쪽에서 `typeof` 분기 + 파싱 가드를 두는 게 안전하다.
