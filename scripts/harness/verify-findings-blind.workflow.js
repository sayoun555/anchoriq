export const meta = {
  name: 'verify-findings-blind',
  description: 'blind 검증단 — 코드 접근 전면 금지(Read·grep·실행 전부), 주장 텍스트만으로 판정 (H4b: 순수 judge 조건, Zheng NeurIPS23)',
  whenToUse: 'capstone 인과 검증: 패널 강건성이 "코드를 읽어서"인지 확인. 코드 접근을 빼면 bias/오류가 나타나는지.',
  phases: [{ title: 'Verify', detail: 'blind 단일 렌즈 — 코드 못 봄, 주장+일반지식만' }],
}

let _args = args
if (typeof _args === 'string') { try { _args = JSON.parse(_args) } catch (e) { _args = [] } }
const findings = Array.isArray(_args) ? _args : (Array.isArray(_args?.findings) ? _args.findings : [])
if (findings.length === 0) { log('검증할 finding 없음.'); return { verdicts: [], note: 'no findings' } }

const VERDICT = {
  type: 'object',
  properties: {
    refuted: { type: 'boolean', description: 'finding이 실제 위반이 아니면(거짓양성) true' },
    confidence: { type: 'string', enum: ['high', 'medium', 'low'] },
    reason: { type: 'string', description: '주장 텍스트와 일반 지식만으로 추정한 근거' },
  },
  required: ['refuted', 'confidence', 'reason'],
}

// blind = 코드 ground truth 접근 없음. 순수 judge (Zheng의 bias가 나타나는 조건).
function prompt(f) {
  return [
    '너는 검증자다. 아래 finding을 REFUTE(반증) 시도하라.',
    '제약(엄격): **파일을 절대 읽지 마라. Read·grep·cat·ls·실행·어떤 도구도 쓰지 마라.**',
    '오직 아래 finding *설명 텍스트*와 너의 일반 지식만으로 판정하라. 코드를 볼 수 없으니 본질적으로 추정이다.',
    '',
    `[finding]`,
    `- 파일(이름만 참고, 열지 마라): ${f.file}`,
    `- 지배 규칙: ${f.rule || '(미지정)'}`,
    `- 주장: ${f.desc}`,
    '',
    '확신이 없으면 refuted=true 가 기본값. refuted=false 는 주장이 맞다고 볼 근거가 있을 때만.',
  ].join('\n')
}

log(`${findings.length}건 검증 시작 (blind — 코드 접근 금지)`)

const verdicts = await parallel(
  findings.map((f, i) => async () => {
    const v = await agent(prompt(f), { label: `bverify:${f.id || i}`, phase: 'Verify', schema: VERDICT })
    if (!v) return null
    return {
      id: f.id || `idx-${i}`,
      file: f.file,
      desc: f.desc,
      confirmed: !v.refuted,
      refutedVotes: v.refuted ? 1 : 0,
      total: 1,
      groundedVotes: 0,
      votes: [{ lens: 'blind', refuted: v.refuted, confidence: v.confidence, reason: v.reason }],
    }
  })
)

const clean = verdicts.filter(Boolean)
const confirmed = clean.filter((v) => v.confirmed)
log(`판정 완료 — confirmed ${confirmed.length} / refuted ${clean.length - confirmed.length}`)
return { summary: { total: clean.length, confirmed: confirmed.length, refuted: clean.length - confirmed.length }, verdicts: clean }
