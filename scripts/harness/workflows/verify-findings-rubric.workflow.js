export const meta = {
  name: 'verify-findings-rubric',
  description: 'rubric 검증단 — 자유서술 대신 고정 체크리스트(구조화 루브릭)로 판정 (H11: Agentic Rubrics 2601)',
  whenToUse: 'H11: 구조화 루브릭이 자유서술 검증과 다른/일관된 판정을 주는지. free-form grounded와 같은 findings로 비교.',
  phases: [{ title: 'Verify', detail: 'rubric 단일 렌즈 — 6항목 체크리스트' }],
}

let _args = args
if (typeof _args === 'string') { try { _args = JSON.parse(_args) } catch (e) { _args = [] } }
const findings = Array.isArray(_args) ? _args : (Array.isArray(_args?.findings) ? _args.findings : [])
if (findings.length === 0) { log('검증할 finding 없음.'); return { verdicts: [], note: 'no findings' } }

// 출력 schema는 단순하게(3필드) — 복잡 schema + heavy 도구작업 시 StructuredOutput 누락 실패 회피.
// 루브릭 6항목은 프롬프트(과정)로 강제하고, 그 답은 rubric 텍스트에 요약한다.
const VERDICT = {
  type: 'object',
  properties: {
    refuted: { type: 'boolean', description: 'finding이 거짓양성이면 true' },
    confidence: { type: 'string', enum: ['high', 'medium', 'low'] },
    rubric: { type: 'string', description: '6항목 체크리스트 결과를 한 줄씩 요약(cited/compiles/ruleText/violates/worthBlocking + 종합)' },
  },
  required: ['refuted', 'confidence', 'rubric'],
}

function prompt(f) {
  return [
    '너는 AnchorIQ 하네스 검증자다. 자유 서술 대신 **아래 고정 루브릭 6항목을 순서대로 도구로 채워라.**',
    '1) cited: 인용 파일/라인이 실재하나 — Read/grep으로 확인.',
    '2) compiles: 컴파일/타입 통과하나 — scripts/harness/ground-check.sh <file>.',
    '3) ruleText: 지배 규칙의 *실제 문구* — scripts/harness/plan-search.sh section/grep으로 인용.',
    '4) violates: 코드가 그 규칙을 *실제로* 어기나 — 코드와 ruleText 대조.',
    '5) worthBlocking: 막을 가치 있나(수용 가능한 부채가 아닌가).',
    '6) refuted: 종합 판정. violates=false 또는 worthBlocking=false 면 refuted=true(거짓양성).',
    '',
    '출력: 1~5 항목을 도구로 확인한 결과를 rubric 필드에 한 줄씩 요약하고, refuted/confidence로 종합 판정하라. (출력 schema는 단순하니 끝에 반드시 구조화 출력을 호출하라.)',
    '',
    `[finding]`,
    `- 파일: ${f.file}`,
    `- 지배 규칙: ${f.rule || '(미지정)'}`,
    `- 주장: ${f.desc}`,
  ].join('\n')
}

log(`${findings.length}건 검증 시작 (rubric — 6항목 체크리스트)`)

const verdicts = await parallel(
  findings.map((f, i) => async () => {
    const v = await agent(prompt(f), { label: `rverify:${f.id || i}`, phase: 'Verify', schema: VERDICT })
    if (!v) return null
    return {
      id: f.id || `idx-${i}`,
      file: f.file,
      desc: f.desc,
      confirmed: !v.refuted,
      refutedVotes: v.refuted ? 1 : 0,
      total: 1,
      groundedVotes: 1,
      votes: [{ lens: 'rubric', refuted: v.refuted, confidence: v.confidence, reason: v.rubric }],
    }
  })
)

const clean = verdicts.filter(Boolean)
const confirmed = clean.filter((v) => v.confirmed)
log(`판정 완료 — confirmed ${confirmed.length} / refuted ${clean.length - confirmed.length}`)
return { summary: { total: clean.length, confirmed: confirmed.length, refuted: clean.length - confirmed.length }, verdicts: clean }
