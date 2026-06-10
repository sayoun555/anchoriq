export const meta = {
  name: 'supervise-batch',
  description: '감독자(Supervisor) 패턴 — 가변·대량 독립 작업을 *런타임에 발견*하고 동적 분배. 감독자가 scope대로 대상을 스캔→work-list 생성→워커 병렬 처리(동시성 cap=동적 디스패치)→단일 컴파일 검증. orchestrate-feature(정해진 기능의 정적 분해)와 달리 항목 수가 가변·미지.',
  whenToUse: '같은 변환/작업을 *여러 독립 대상*(패턴 매칭 파일들·여러 모듈 등)에 적용할 때, 그 대상을 미리 모를 때(런타임 발견). 단일 기능이면 orchestrate-feature, 정해진 소수면 직접.',
  phases: [
    { title: 'Survey', detail: '감독자가 scope대로 작업 항목 발견 + 독립성·전체수 보고 (동적)' },
    { title: 'Dispatch', detail: '워커가 항목별 병렬 처리(쓰기만, cap=동적 부하분산)' },
    { title: 'Verify', detail: '단일 에이전트가 일괄 컴파일로 검증 (병렬 gradle 락 회피)' },
  ],
}

// args: { task: "각 항목에 적용할 작업", scope: "항목 발견 기준(스캔법)", maxItems?: 40 }
let _args = args
if (typeof _args === 'string') { try { _args = JSON.parse(_args) } catch (e) { _args = { task: _args } } }
const task = _args?.task || ''
const scope = _args?.scope || ''
const MAX_ITEMS = _args?.maxItems || 40
if (!task || !scope) {
  log('task와 scope 둘 다 필요 (task=각 항목에 할 일, scope=항목 발견 기준).')
  return { error: 'task/scope required' }
}

// ── 스키마는 평면 (무거운 도구작업 뒤 StructuredOutput 실패 회피 — troubleshooting 참조) ──
const SURVEY = {
  type: 'object',
  properties: {
    items: {
      type: 'array',
      description: '발견된 독립 작업 항목. target은 항목 간 겹치지 않아야(보통 파일 경로). 같은 파일 건드릴 둘은 한 항목으로.',
      items: {
        type: 'object',
        properties: {
          target: { type: 'string', description: '작업 대상(보통 파일 경로)' },
          note: { type: 'string', description: '이 항목에서 무엇을 할지 한 줄' },
        },
        required: ['target', 'note'],
      },
    },
    totalFound: { type: 'integer', description: 'scope에 맞는 전체 항목 수(cap 적용 전)' },
    disjoint: { type: 'boolean', description: 'target들이 서로 겹치지 않음을 확인했는가' },
  },
  required: ['items', 'totalFound', 'disjoint'],
}
const WORK = {
  type: 'object',
  properties: {
    done: { type: 'boolean', description: '배정된 항목 작업을 완료했는가' },
    report: { type: 'string', description: '무엇을 했는지 한두 줄(다른 대상 안 건드림 확인 포함)' },
  },
  required: ['done', 'report'],
}
const VERIFY = {
  type: 'object',
  properties: {
    compiles: { type: 'boolean', description: '영향받은 모듈 일괄 컴파일이 통과했는가' },
    summary: { type: 'string', description: '컴파일 결과 + 깨진 항목/이음새 + 남은 문제 요약' },
  },
  required: ['compiles', 'summary'],
}

// ── ① 감독자: 작업 항목 런타임 발견 (동적) ──────────────────────
phase('Survey')
log(`① 감독자가 작업 항목 발견 중…`)
const survey = await agent(
  [
    '너는 배치 작업의 감독자다. 아래 scope에 맞는 *작업 항목*을 런타임에 발견(스캔)하라.',
    `[scope — 항목 발견 기준]\n${scope}`,
    `[task — 각 항목에 적용할 작업]\n${task}`,
    '',
    'grep/find/ls로 코드베이스를 스캔해 대상을 나열하라. 규칙:',
    '- 항목은 *독립*이어야(보통 서로 다른 파일). 같은 파일을 건드릴 둘은 한 항목으로 합쳐 — 병렬 충돌 방지.',
    '- totalFound에 scope에 맞는 *전체* 수를 정확히 보고.',
    `- 항목이 ${MAX_ITEMS}개를 넘으면 가장 관련 깊은 ${MAX_ITEMS}개만 items에 담되 totalFound엔 전체 수를(드롭 투명 — silent truncation 금지).`,
    '- 확신 안 서는 대상은 빼라(보수적).',
  ].join('\n'),
  { label: 'survey', phase: 'Survey', schema: SURVEY }
)

let items = survey && Array.isArray(survey.items) ? survey.items : []
if (items.length === 0) {
  log('발견된 작업 항목 없음 — 처리할 게 없다.')
  return { found: survey?.totalFound || 0, processed: 0, succeeded: [], failed: [], note: 'no items' }
}
if (items.length > MAX_ITEMS) items = items.slice(0, MAX_ITEMS)
const totalFound = survey.totalFound || items.length
const dropped = Math.max(0, totalFound - items.length)
log(`발견 ${totalFound}개 · 처리 ${items.length}개${dropped ? ` · 드롭 ${dropped}(cap=${MAX_ITEMS}, 재실행 필요 — no silent truncation)` : ''}${survey.disjoint === false ? ' · ⚠️ disjoint 미확인' : ''}`)

// ── ② 워커: 항목별 병렬 처리 (쓰기만 — 병렬 gradle 락 회피) ──────
phase('Dispatch')
log(`② 워커 ${items.length}개 동적 분배(동시성 cap이 부하분산)…`)
const results = await parallel(
  items.map((it, i) => async () => {
    const r = await agent(
      [
        '너는 배치 워커다. **배정된 하나의 대상만** 처리하라(다른 대상 건들지 마 — 병렬 충돌 방지). 임시방편/꼼수 금지(정석).',
        `[대상] ${it.target}`,
        `[할 일] ${it.note}`,
        `[전체 작업 맥락] ${task}`,
        '',
        '코드 변경이면 design-first: `bash scripts/harness/plan-search.sh`로 지배 plan/ 문서 절을 먼저 확인한 뒤 적용하라.',
        '컴파일은 하지 마라(병렬 gradle 락 회피) — 마지막 Verify 단계가 일괄 컴파일한다. 시그니처가 맞는 정확한 코드를 쓰는 데 집중.',
      ].join('\n'),
      { label: `work:${(it.target || '').split('/').pop() || i}`, phase: 'Dispatch', schema: WORK }
    )
    return r ? { target: it.target, done: !!r.done, report: r.report } : { target: it.target, done: false, report: '워커 무응답' }
  })
)
const clean = results.filter(Boolean)
const workerDone = clean.filter((r) => r.done)
const workerFailed = clean.filter((r) => !r.done)
log(`구현 완료 — 성공 ${workerDone.length}/${clean.length} · 실패 ${workerFailed.length}`)

// ── ③ 단일 검증: 일괄 컴파일 (병렬 gradle 락 회피) ───────────────
phase('Verify')
const verify = await agent(
  [
    '너는 배치 검증자다. 방금 여러 워커가 독립 대상들을 수정했다. 일괄로 컴파일해 깨진 곳이 없는지 확인하라.',
    '`git diff --stat` 로 실제 변경을 보고, 영향받은 백엔드 모듈을 `bash scripts/harness/ground-check.sh <대표파일>` 또는 `cd backend && ./gradlew compileJava` 로 컴파일하라.',
    '컴파일 에러가 나면 그 출력을 1차 증거로, 어느 항목이 깼는지 summary에 적어라.',
    '',
    `[전체 작업] ${task}`,
    `[처리된 대상]\n${clean.map((r) => `- ${r.target}: ${r.done ? 'ok' : 'FAILED'}`).join('\n')}`,
  ].join('\n'),
  { label: 'verify-batch', phase: 'Verify', schema: VERIFY }
)

log(`완료 — 성공 ${workerDone.length}/${clean.length} · 컴파일 ${verify?.compiles ? '✅' : '❌'}${dropped ? ` · 미처리(cap) ${dropped}` : ''}`)

return {
  scope,
  task,
  found: totalFound,
  processed: clean.length,
  dropped,
  compiles: verify?.compiles ?? null,
  succeeded: workerDone.map((r) => r.target),
  failed: workerFailed.map((r) => ({ target: r.target, report: r.report })),
  verifySummary: verify?.summary || '',
}
