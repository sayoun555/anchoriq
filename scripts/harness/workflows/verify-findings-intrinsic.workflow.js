export const meta = {
  name: 'verify-findings-intrinsic',
  description: 'intrinsic 검증단 — 도구(컴파일/테스트) 실행 금지, 읽고 추론만으로 판정 (H3 대조군: Huang ICLR24 intrinsic self-correction)',
  whenToUse: 'H3 실험 대조군. grounded 패널(컴파일 실행)과 비교해 "외부 실행 신호 없이 추론만 하면 악화되나"를 측정.',
  phases: [{ title: 'Verify', detail: 'intrinsic 단일 렌즈 — 실행 금지, 추론만' }],
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
    reason: { type: 'string', description: '읽고 추론한 근거 1~2문장' },
  },
  required: ['refuted', 'confidence', 'reason'],
}

// intrinsic = 외부 실행 신호 없이 자기 추론만. (대조군)
function prompt(f) {
  return [
    '너는 검증자다. 아래 finding을 REFUTE(반증) 시도하라.',
    '제약(중요): **컴파일/빌드/테스트를 실행하지 마라.** ground-check.sh, ./gradlew, 어떤 빌드·실행 명령도 금지.',
    '오직 코드를 Read 해서 *읽고 추론*으로 판정하라. "컴파일 되는가"도 실행하지 말고 머리로 추정하라.',
    '',
    `[finding]`,
    `- 파일: ${f.file}`,
    `- 지배 규칙: ${f.rule || '(미지정)'}`,
    `- 주장: ${f.desc}`,
    '',
    '확신이 없으면 refuted=true 가 기본값. refuted=false 는 위반이 실재한다는 근거가 읽기로 확인될 때만.',
  ].join('\n')
}

log(`${findings.length}건 검증 시작 (intrinsic — 실행 금지, 추론만)`)

const verdicts = await parallel(
  findings.map((f, i) => async () => {
    const v = await agent(prompt(f), { label: `iverify:${f.id || i}`, phase: 'Verify', schema: VERDICT })
    if (!v) return null
    // 단일 렌즈 = 그 판정이 곧 결과
    return {
      id: f.id || `idx-${i}`,
      file: f.file,
      desc: f.desc,
      confirmed: !v.refuted,
      refutedVotes: v.refuted ? 1 : 0,
      total: 1,
      groundedVotes: 0,
      votes: [{ lens: 'intrinsic', refuted: v.refuted, confidence: v.confidence, reason: v.reason }],
    }
  })
)

const clean = verdicts.filter(Boolean)
const confirmed = clean.filter((v) => v.confirmed)
log(`판정 완료 — confirmed ${confirmed.length} / refuted ${clean.length - confirmed.length}`)
return { summary: { total: clean.length, confirmed: confirmed.length, refuted: clean.length - confirmed.length }, verdicts: clean }
