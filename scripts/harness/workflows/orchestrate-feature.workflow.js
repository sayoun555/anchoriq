export const meta = {
  name: 'orchestrate-feature',
  description: '대규모 코드 작성 오케스트레이터 — 작업을 파일-disjoint 독립 단위로 분해 → 병렬 구현(쓰기만) → 단일 통합자가 컴파일로 봉합 → 적대 검증. orchestrator-worker(Anthropic) + "독립 단위만 병렬, 하나가 통합"(Cognition).',
  whenToUse: '여러 파일/모듈에 걸친 큰 기능을 구현할 때. 상호의존 코드 공동작성이 아니라, *독립적으로 쪼개지는* 단위를 병렬화하고 한 통합자가 봉합. 단일 코더로 충분한 작은 작업엔 쓰지 말 것.',
  phases: [
    { title: 'Decompose', detail: '작업을 파일-disjoint 단위 + 단위간 계약으로 분해' },
    { title: 'Implement', detail: '단위별 코더가 병렬로 자기 파일만 작성(쓰기 전용, design-first)' },
    { title: 'Integrate', detail: '단일 통합자가 ground-check 컴파일로 이음새 봉합' },
    { title: 'Review', detail: '적대자가 통합 결과를 spec 대비 검증' },
  ],
}

// args: { task: "<기능 설명>", docs?: ["plan/X.md"...], modules?: ["anchoriq-api"...] }  또는 문자열 task
let _args = args
if (typeof _args === 'string') { try { _args = JSON.parse(_args) } catch (e) { _args = { task: _args } } }
const task = _args?.task || (typeof _args === 'string' ? _args : '')
const docHints = Array.isArray(_args?.docs) ? _args.docs : []
if (!task) { log('task가 없다. args.task에 기능 설명을 넘겨라.'); return { error: 'no task' } }

// ── ① 분해 ────────────────────────────────────────────────
const DECOMPOSE = {
  type: 'object',
  properties: {
    units: {
      type: 'array',
      description: '독립 작업 단위. targetFiles는 단위 간 절대 겹치지 않아야 함(겹치면 한 단위로 합쳐라).',
      items: {
        type: 'object',
        properties: {
          id: { type: 'string' },
          title: { type: 'string' },
          targetFiles: { type: 'array', items: { type: 'string' }, description: '이 단위가 생성/수정할 파일 경로(다른 단위와 disjoint)' },
          exposes: { type: 'string', description: '이 단위가 제공하는 공개 계약(클래스/메서드 시그니처, 인터페이스)' },
          dependsOn: { type: 'array', items: { type: 'string' }, description: '의존하는 다른 단위 id(계약만 의존, 코드 X)' },
          specRefs: { type: 'array', items: { type: 'string' }, description: '지배하는 plan/ 문서·절' },
        },
        required: ['id', 'title', 'targetFiles', 'exposes', 'dependsOn', 'specRefs'],
      },
    },
    sharedContracts: { type: 'string', description: '모든 단위가 지켜야 할 인터페이스/시그니처 계약을 한곳에 명시(병렬 코더들이 이음새에서 합의하도록)' },
    fileDisjoint: { type: 'boolean', description: '모든 단위의 targetFiles가 서로 겹치지 않음을 확인했는가' },
    integrationNotes: { type: 'string', description: '통합 시 주의할 이음새·순서' },
  },
  required: ['units', 'sharedContracts', 'fileDisjoint', 'integrationNotes'],
}

phase('Decompose')
log('① 작업 분해 중…')
const plan = await agent(
  [
    '너는 대규모 구현의 오케스트레이터다. 아래 작업을 *병렬로 구현 가능한 독립 단위*로 분해하라.',
    '규칙:',
    '- 단위 간 targetFiles는 절대 겹치지 마라(겹치면 한 단위로 합쳐 — 병렬 쓰기 충돌 방지).',
    '- 단위 간 의존은 *코드*가 아니라 *계약(인터페이스/시그니처)*으로만. 그 계약을 sharedContracts에 먼저 못박아 모든 단위가 같은 이음새를 쓰게 하라.',
    '- 각 단위에 지배하는 plan/ 설계문서·절(specRefs)을 붙여라. (design-first: 구현 전 그 문서를 읽어야 한다.)',
    '- AnchorIQ 모듈 의존규칙(core는 외부의존 금지 등)과 DDD 레이어를 존중하라.',
    '',
    `[작업]\n${task}`,
    docHints.length ? `\n[참고 설계문서]\n${docHints.join('\n')}` : '',
    '',
    '먼저 plan/ 관련 문서를 plan-search.sh로 훑고(통째 읽기 금지), 코드베이스 구조를 grep/Read로 파악한 뒤 분해하라.',
  ].join('\n'),
  { label: 'decompose', phase: 'Decompose', schema: DECOMPOSE }
)

if (!plan || !Array.isArray(plan.units) || plan.units.length === 0) {
  log('분해 실패 — 단위가 없다.'); return { error: 'decompose produced no units', plan }
}
if (!plan.fileDisjoint) {
  log('⚠️ 분해자가 파일 disjoint를 보장하지 못함 — 병렬 쓰기 충돌 위험. 통합자가 직렬 봉합으로 처리.')
}
log(`분해 완료 — ${plan.units.length}개 독립 단위: ${plan.units.map((u) => u.id).join(', ')}`)

// ── ② 병렬 구현 (쓰기 전용 — 컴파일은 통합자가) ─────────────
const IMPL = {
  type: 'object',
  properties: {
    id: { type: 'string' },
    filesWritten: { type: 'array', items: { type: 'string' } },
    summary: { type: 'string', description: '무엇을 어떻게 구현했는지 1~3문장' },
    deviations: { type: 'string', description: 'sharedContracts에서 벗어난 점이나 통합자가 알아야 할 가정(없으면 "none")' },
  },
  required: ['id', 'filesWritten', 'summary', 'deviations'],
}

phase('Implement')
log(`② ${plan.units.length}개 단위 병렬 구현(쓰기 전용)…`)
const built = await parallel(
  plan.units.map((u) => async () =>
    agent(
      [
        '너는 한 독립 단위를 구현하는 코더다. **너의 targetFiles만** 작성/수정하라(다른 파일 건들지 마 — 병렬 충돌 방지).',
        '절대 규칙(하네스): 코드 쓰기 전에 specRefs의 plan/ 문서를 `bash scripts/harness/plan-search.sh section <문서> "<키워드>"`로 해당 절만 읽고 그 설계를 따르라. 임시방편/꼼수 금지(정석).',
        '컴파일은 하지 마라(gradle 락·동시쓰기 회피) — 통합 단계가 일괄 컴파일한다. 너는 sharedContracts를 정확히 지켜 *시그니처가 맞는* 코드를 쓰는 데 집중하라.',
        '',
        `[단위 ${u.id}] ${u.title}`,
        `- 작성할 파일(이것만): ${u.targetFiles.join(', ')}`,
        `- 이 단위가 제공할 계약(exposes): ${u.exposes}`,
        `- 의존 계약(dependsOn): ${u.dependsOn.join(', ') || '없음'}`,
        `- 지배 문서(specRefs): ${u.specRefs.join(', ') || '(분해자 미지정 — plan/에서 찾아라)'}`,
        '',
        `[모든 단위 공유 계약 — 반드시 준수]\n${plan.sharedContracts}`,
      ].join('\n'),
      // worktree 격리 안 함: 파일 disjoint라 메인 트리 동시쓰기 충돌 없고, 통합자가 메인 트리에서 봉합해야 하므로
      { label: `impl:${u.id}`, phase: 'Implement', schema: IMPL }
    )
  )
)
const impls = built.filter(Boolean)
log(`구현 완료 — ${impls.length}/${plan.units.length} 단위`)

// ── ③ 통합 (단일 컨텍스트 + ground-check 컴파일 봉합) ───────
// schema 단순화(평면 2필드) — 무거운 도구작업 뒤 복잡 schema는 StructuredOutput 실패
// (troubleshooting/WORKFLOW_COMPLEX_SCHEMA_STRUCTUREDOUTPUT.md). 구조는 report 산문에 담음.
const INTEGRATE = {
  type: 'object',
  properties: {
    compiles: { type: 'boolean', description: 'ground-check 컴파일이 최종 통과했는가' },
    report: { type: 'string', description: '컴파일한 모듈 + 봉합한 이음새(시그니처·import·의존) + 남은 문제를 한 문단으로' },
  },
  required: ['compiles', 'report'],
}

phase('Integrate')
log('③ 단일 통합자가 봉합·컴파일…')
const integration = await agent(
  [
    '너는 단일 통합자다(orchestrator-worker의 통합 단계). 병렬 코더들이 각자 자기 파일을 *이미 메인 트리에* 작성했다(파일 disjoint). 네 일은 그것들이 하나로 맞물리는지 봉합하는 것.',
    '`git diff --stat` 로 실제 작성된 변경을 확인하고, 이음새(시그니처·import·모듈 의존)가 어긋난 곳을 맞춰라. 단위가 자기 파일을 빠뜨렸으면 그 단위 결과(summary)대로 채워라.',
    '반드시 `bash scripts/harness/ground-check.sh <대표파일>` 로 영향받은 각 모듈을 컴파일하고, 에러가 나면 그 컴파일 출력을 1차 증거로 이음새를 고쳐 통과시켜라.',
    'AnchorIQ 모듈 의존규칙·DDD 레이어를 위반하는 봉합은 금지(정석으로).',
    '',
    '[공유 계약]\n' + plan.sharedContracts,
    '\n[통합 주의]\n' + plan.integrationNotes,
    '\n[각 단위 구현 결과]\n' + impls.map((r) => `- ${r.id}: files=[${(r.filesWritten || []).join(', ')}] / ${r.summary} / deviations: ${r.deviations}`).join('\n'),
  ].join('\n'),
  { label: 'integrate', phase: 'Integrate', schema: INTEGRATE }
)

// ── ④ 적대 검증 ───────────────────────────────────────────
// 중첩 객체 배열 → 문자열 배열로 평탄화 (StructuredOutput 실패 회피)
const REVIEW = {
  type: 'object',
  properties: {
    verdict: { type: 'string', enum: ['clean', 'issues'] },
    findings: {
      type: 'array',
      items: { type: 'string' },
      description: '확신 있는 위반만. 각 항목 "심각도 | 파일 | 위반내용 (지배규칙)" 형식. 없으면 빈 배열.',
    },
  },
  required: ['verdict', 'findings'],
}

phase('Review')
log('④ 통합 결과 적대 검증…')
const review = await agent(
  [
    '너는 하네스의 적대적 검증자다. 방금 통합된 구현을 spec 대비 검증하라.',
    '`git diff --stat` 로 변경 파일을 보고, 의심 파일을 Read + `bash scripts/harness/ground-check.sh <file>` 로 컴파일을 확인하라.',
    'DDD/계약/모듈의존 위반, 미완성(stub/TODO/임시), 시그니처 불일치를 찾아라. 추측 말고 도구로 grounding.',
    '확신 있는 위반만 findings에 담아라(거짓양성 회피).',
    '',
    `[원래 작업]\n${task}`,
    `\n[공유 계약]\n${plan.sharedContracts}`,
    `\n[통합 보고]\ncompiles=${integration?.compiles} / ${integration?.report || '(보고 없음)'}`,
  ].join('\n'),
  { label: 'review', phase: 'Review', schema: REVIEW }
)

log(`완료 — 단위 ${impls.length} · 컴파일 ${integration?.compiles ? '✅' : '❌'} · 검증 ${review?.verdict} (${review?.findings?.length || 0} findings)`)

return {
  task,
  units: plan.units.map((u) => ({ id: u.id, title: u.title, targetFiles: u.targetFiles })),
  sharedContracts: plan.sharedContracts,
  implementations: impls,
  integration,
  review,
}
