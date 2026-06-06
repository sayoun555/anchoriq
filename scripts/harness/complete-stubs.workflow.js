export const meta = {
  name: 'complete-stubs',
  description: 'bounded stub-완성 루프(가드6+7) — 미완성 마커(stub/TODO/임시)를 감지→설계 결정성 분류→스펙으로 정해지는 것만 bounded 루프[구현→ground-check 컴파일→독립 적대자→reward-hack 감시]로 완성. 미결 설계는 사람에게 에스컬레이트.',
  whenToUse: 'check-stubs가 미완성 마커를 경고할 때, 그중 *스펙이 구현을 결정하는* 것을 자동 완성하고 싶을 때. 설계 결정이 필요한 stub은 완성하지 않고 에스컬레이트(design-first 보호).',
  phases: [
    { title: 'Detect', detail: 'check-stubs로 마커 수집 + 스펙-결정 vs 설계-필요 분류' },
    { title: 'Complete', detail: '스펙-결정 마커를 bounded 루프로 완성(구현→컴파일→적대자→reward-hack)' },
  ],
}

const MAX_ATTEMPTS = (typeof args === 'object' && args && args.maxAttempts) || 3

// ── 분류 스키마 (Detect) ──────────────────────────────────────────
const DETECT = {
  type: 'object',
  properties: {
    stubs: {
      type: 'array',
      description: '발견된 미완성 마커. decidable=true 는 plan/ 스펙이 구현을 명확히 정하는 것(자동완성 가능), false 는 미결 설계 결정이 필요한 것(에스컬레이트).',
      items: {
        type: 'object',
        properties: {
          file: { type: 'string' },
          marker: { type: 'string', description: '마커 위치(메서드/라인) + 무엇이 미완성인지' },
          decidable: { type: 'boolean', description: 'plan/ 스펙이 구현을 결정하면 true, 설계 결정 필요면 false' },
          specRef: { type: 'string', description: '구현을 지배하는 plan/ 문서·절 (decidable=true일 때)' },
        },
        required: ['file', 'marker', 'decidable', 'specRef'],
      },
    },
    summary: { type: 'string' },
  },
  required: ['stubs', 'summary'],
}

// ── 구현/게이트 스키마는 *평면 2~3필드* (무거운 도구작업 뒤 StructuredOutput 실패 회피) ──
const IMPL = {
  type: 'object',
  properties: {
    changed: { type: 'boolean', description: '실제로 파일을 수정했는가' },
    report: { type: 'string', description: '무엇을 어떻게 구현했는지 + 어느 파일' },
  },
  required: ['changed', 'report'],
}
const GATE = {
  type: 'object',
  properties: {
    pass: { type: 'boolean', description: '컴파일 통과 + 설계 위반 없음 + 마커가 실제 구현으로 채워짐' },
    gamed: { type: 'boolean', description: 'reward-hacking 감지(마커만 삭제·빈 본문·throw·테스트 약화 등 게이트 속이기)면 true' },
    reason: { type: 'string', description: 'pass/gamed 판정 근거 + (실패 시) 다음 시도가 고쳐야 할 점 1~2문장' },
  },
  required: ['pass', 'gamed', 'reason'],
}

// ── ① 감지 + 분류 ────────────────────────────────────────────────
phase('Detect')
log('① 미완성 마커 감지 + 설계 결정성 분류…')
const detected = await agent(
  [
    '너는 미완성 코드 마커를 감지·분류하는 에이전트다.',
    '`bash scripts/harness/check-stubs.sh` 를 실행해 백엔드 main 소스의 미완성 마커(stub/TODO/mock/"// 임시")를 수집하라.',
    '각 마커에 대해 해당 파일과 주변 코드, 그리고 그 파일을 지배하는 plan/ 문서를 읽고(plan-search.sh 활용) 분류하라:',
    '- decidable=true: plan/ 스펙이 *구현을 명확히 결정*한다(예: "TODO: DB_INIT_SCRIPTS의 스키마대로 매핑"). 자동 완성 가능.',
    '- decidable=false: *미결 설계 결정*이 필요하다(예: "TODO: 캐싱 전략 결정"). 추측하면 design-first 위반 → 에스컬레이트.',
    'specRef엔 구현을 지배하는 plan/ 문서·절을 적어라(decidable=true일 때).',
    '확신이 없으면 decidable=false(보수적으로 — 잘못 자동완성하느니 사람에게).',
  ].join('\n'),
  { label: 'detect-stubs', phase: 'Detect', schema: DETECT }
)

const stubs = (detected && Array.isArray(detected.stubs)) ? detected.stubs : []
if (stubs.length === 0) {
  log('미완성 마커 없음 — 완성할 것이 없다.')
  return { detected: 0, completed: [], escalated: [], note: 'no stubs' }
}
const decidable = stubs.filter((s) => s.decidable)
const needsDesign = stubs.filter((s) => !s.decidable)
log(`마커 ${stubs.length}건 — 완성가능 ${decidable.length}, 설계필요(에스컬레이트) ${needsDesign.length}`)

// ── ② bounded 완성 루프 (stub마다 순차, 시도 간 Reflexion) ───────
phase('Complete')
const results = []
for (let i = 0; i < decidable.length; i++) {
  const stub = decidable[i]
  let done = false
  let lastReason = '' // Reflexion: 직전 실패 사유를 다음 시도에 주입
  for (let attempt = 1; attempt <= MAX_ATTEMPTS && !done; attempt++) {
    // (a) 구현 — design-first + grounded
    const impl = await agent(
      [
        '너는 미완성 마커 하나를 *스펙대로* 완성하는 코더다. 임시방편/꼼수 금지(정석).',
        `대상 파일: ${stub.file}`,
        `미완성: ${stub.marker}`,
        `지배 스펙: ${stub.specRef} — 구현 전 plan-search.sh로 해당 절을 읽고 그 설계를 따르라.`,
        attempt > 1 ? `\n[직전 시도 실패 사유 — 이번엔 반드시 고쳐라]\n${lastReason}` : '',
        '\n마커(TODO/stub/"// 임시")를 *실제 구현으로 대체*하라. 마커만 지우거나 빈 본문/throw 로 때우지 마라(그건 부정으로 잡힌다).',
        '수정 후 `bash scripts/harness/ground-check.sh ' + stub.file + '` 로 컴파일을 확인하고 깨지면 고쳐라.',
      ].join('\n'),
      { label: `impl:${stub.file.split('/').pop()}:a${attempt}`, phase: 'Complete', schema: IMPL }
    )
    // (b) 독립 게이트 — 컴파일 + 적대자 + reward-hack 감시 (구현자 ≠ 검증자)
    const gate = await agent(
      [
        '너는 독립 게이트다(구현하지 않은 검증자). 방금 완성됐다는 마커가 *진짜로* 정석 완성됐는지 판정하라.',
        `대상 파일: ${stub.file}`,
        `원래 미완성: ${stub.marker}`,
        `지배 스펙: ${stub.specRef}`,
        '',
        '반드시 도구로 grounding하라:',
        '1. `bash scripts/harness/ground-check.sh ' + stub.file + '` — 컴파일 통과해야 pass 가능.',
        '2. 파일을 Read 해 마커(TODO/stub/임시)가 *실제 구현*으로 대체됐는지 확인. plan-search.sh로 스펙 부합 확인.',
        '3. reward-hack 감시(gamed=true 로): 마커만 삭제하고 구현 안 함 / 빈 본문 / return null·throw UnsupportedOperation 로 때움 / 기존 테스트 삭제·assertion 약화.',
        '',
        'pass=true 는 ① 컴파일 통과 ② 스펙대로 실제 구현 ③ reward-hack 아님 — 셋 다일 때만. reason에 근거 + (실패 시) 다음 시도가 고칠 점을 적어라.',
      ].join('\n'),
      { label: `gate:${stub.file.split('/').pop()}:a${attempt}`, phase: 'Complete', schema: GATE }
    )
    if (gate && gate.pass && !gate.gamed) {
      done = true
      lastReason = gate.reason
    } else {
      lastReason = (gate && gate.gamed ? 'REWARD-HACK 감지: ' : '') + (gate ? gate.reason : '게이트 무응답')
    }
    log(`[${stub.file.split('/').pop()}] 시도 ${attempt}/${MAX_ATTEMPTS}: ${done ? '✅ 완성' : '↻ ' + lastReason.slice(0, 50)}`)
  }
  results.push({ file: stub.file, completed: done, reason: lastReason, attempts: MAX_ATTEMPTS })
}

const completed = results.filter((r) => r.completed)
const failed = results.filter((r) => !r.completed)
log(`완료 — 완성 ${completed.length}/${decidable.length} · 미완(에스컬레이트) ${failed.length} · 설계필요 ${needsDesign.length}`)

return {
  detected: stubs.length,
  completed: completed.map((r) => r.file),
  failed: failed.map((r) => ({ file: r.file, reason: r.reason })),       // bound 소진 → 사람에게
  escalated: needsDesign.map((s) => ({ file: s.file, why: s.marker })),  // 설계 결정 필요 → 사람에게
}
