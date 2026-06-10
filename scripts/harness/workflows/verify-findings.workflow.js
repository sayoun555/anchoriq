export const meta = {
  name: 'verify-findings',
  description: '다수결 검증단 — DLT 열린 finding을 3렌즈 적대 패널이 반증 시도, 과반으로 confirm/refute (전파 채널 B)',
  whenToUse: 'findings DLT에 쌓인 위반 후보를 고신뢰로 분류할 때. 싼 게이트/의미 리뷰어의 거짓양성을 정리한다.',
  phases: [
    { title: 'Verify', detail: 'finding당 3명(code-reality·spec-contract·severity-skeptic)이 반증 시도' },
  ],
}

// args = 검증할 finding 배열: [{id, file, rule, desc, severity}, ...]
// (메인 루프가 `finding.sh list`로 읽어 넘긴다 — 워크플로우 스크립트는 파일시스템 접근 불가)
// args가 문자열로 들어오는 경우(JSON 인코딩 전달)도 방어적으로 파싱한다.
let _args = args
if (typeof _args === 'string') {
  try { _args = JSON.parse(_args) } catch (e) { _args = [] }
}
const findings = Array.isArray(_args) ? _args : (Array.isArray(_args?.findings) ? _args.findings : [])

if (findings.length === 0) {
  log('검증할 열린 finding이 없다.')
  return { verdicts: [], note: 'no open findings' }
}

// 적대적 검증자가 돌려줄 구조
const VERDICT = {
  type: 'object',
  properties: {
    refuted: { type: 'boolean', description: 'finding이 실제 위반이 아니면(거짓양성) true' },
    confidence: { type: 'string', enum: ['high', 'medium', 'low'] },
    reason: { type: 'string', description: '근거 1~2문장 (인용 파일/지배 문서 기반)' },
  },
  required: ['refuted', 'confidence', 'reason'],
}

// 세 개의 다른 렌즈 — 같은 검증을 3번 하는 게 아니라, 서로 다른 실패 모드를 본다
const LENSES = [
  {
    key: 'code-reality',
    instruction: '인용된 파일(file)을 Read 해서, 주장된 위반이 코드에 실재하는지 본다. 주석/설명일 뿐 실제 동작은 멀쩡하면 refuted=true.',
  },
  {
    key: 'spec-contract',
    instruction: '지배 plan/ 문서(rule에 명시)를 Read 해서, 이게 정말 그 계약/규칙 위반인지 본다. 문서가 허용하는 범위면 refuted=true.',
  },
  {
    key: 'severity-skeptic',
    instruction: '위반이 맞더라도 작업을 막아야 할 수준인지 의심한다. 동작하는 코드의 수용 가능한 기술부채(예: 의도적 임시 처리 + 후속 계획 명시)면 refuted=true.',
  },
]

function verifyPrompt(f, lens) {
  return [
    '너는 AnchorIQ 하네스의 적대적 검증자다. 아래 finding을 REFUTE(반증)하려 시도하라.',
    '',
    `[finding]`,
    `- 파일: ${f.file}`,
    `- 지배 규칙: ${f.rule || '(미지정)'}`,
    `- 주장: ${f.desc}`,
    `- 신고 심각도: ${f.severity || 'med'}`,
    '',
    `[너의 렌즈: ${lens.key}]`,
    lens.instruction,
    '',
    '필요하면 인용 파일과 plan/ 문서를 Read 해서 근거를 확인하라.',
    '확신이 없으면 refuted=true 가 기본값이다(거짓양성으로 닫는 쪽이 코더를 덜 가둔다).',
    'refuted=false 는 위반이 실재하고 막을 가치가 있다는 구체적 근거가 있을 때만.',
  ].join('\n')
}

log(`${findings.length}건 검증 시작 (각 3렌즈 패널)`)

const verdicts = await parallel(
  findings.map((f, i) => async () => {
    const votes = await parallel(
      LENSES.map((lens) => async () => {
        const v = await agent(verifyPrompt(f, lens), {
          label: `verify:${(f.id || i)}:${lens.key}`,
          phase: 'Verify',
          schema: VERDICT,
        })
        return v ? { lens: lens.key, ...v } : null
      })
    )
    const cast = votes.filter(Boolean)
    const refutedVotes = cast.filter((v) => v.refuted).length
    // 3명 중 과반(>=2) 반증이면 거짓양성으로 판정
    const confirmed = !(refutedVotes >= 2)
    return {
      id: f.id || `idx-${i}`,
      file: f.file,
      rule: f.rule,
      desc: f.desc,
      confirmed,
      refutedVotes,
      total: cast.length,
      votes: cast.map((v) => ({ lens: v.lens, refuted: v.refuted, confidence: v.confidence, reason: v.reason })),
    }
  })
)

const clean = verdicts.filter(Boolean)
const confirmed = clean.filter((v) => v.confirmed)
const refuted = clean.filter((v) => !v.confirmed)
log(`판정 완료 — confirmed ${confirmed.length} / refuted(거짓양성) ${refuted.length}`)

// 메인 루프가 이 결과로 finding.sh resolve 를 적용한다(채널 B→C 반영).
return {
  summary: { total: clean.length, confirmed: confirmed.length, refuted: refuted.length },
  verdicts: clean,
}
