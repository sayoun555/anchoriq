export const meta = {
  name: 'verify-findings-grounded',
  description: '도구 쥔 검증단 — 각 렌즈가 읽기만 하지 않고 실제 도구(컴파일·검색·MCP·런타임)로 grounding 후 판정 (채널 B 고도화 ①)',
  whenToUse: 'finding을 "코드만 읽고" 판정하던 채널 B를, 실행/검색/MCP/런타임 신호로 grounding해 고신뢰로 판정할 때. CRITIC·Agent-as-a-Judge 근거. env-gated 렌즈(data·behavior)는 앱/DB가 떠 있을 때만 활성.',
  phases: [
    { title: 'Verify', detail: '정적 4렌즈(execution·spec·code·api-currency) + (env시) 런타임 2렌즈(data·behavior)' },
  ],
}

// args 방어적 파싱 (문자열로 와도 처리 — troubleshooting/WORKFLOW_ARGS_PASSED_AS_STRING)
// 형태: [findings...]  또는  { findings:[...], env:true }  (env=true면 런타임 렌즈도 켬)
let _args = args
if (typeof _args === 'string') { try { _args = JSON.parse(_args) } catch (e) { _args = [] } }
const findings = Array.isArray(_args) ? _args : (Array.isArray(_args?.findings) ? _args.findings : [])
const includeEnv = !Array.isArray(_args) && !!_args?.env

if (findings.length === 0) {
  log('검증할 finding이 없다.')
  return { verdicts: [], note: 'no findings' }
}

// 도구 출력을 1차 증거로 요구하는 verdict 스키마
const VERDICT = {
  type: 'object',
  properties: {
    refuted: { type: 'boolean', description: 'finding이 실제 위반이 아니면(거짓양성) true' },
    confidence: { type: 'string', enum: ['high', 'medium', 'low'] },
    toolEvidence: { type: 'string', description: '실제 실행한 도구와 그 출력 인용(예: ground-check 결과, plan-search 절, context7 문서, DB 쿼리 결과, 앱 실행 관찰). 도구를 못 쓰면 "none"' },
    reason: { type: 'string', description: 'toolEvidence에 근거한 판정 사유 1~2문장' },
  },
  required: ['refuted', 'confidence', 'toolEvidence', 'reason'],
}

// 검증 차원별 렌즈 — 각 렌즈가 "읽기"가 아니라 "전용 도구 실행"으로 grounding.
// envGated=true 렌즈는 런타임 환경(앱/DB)이 떠 있어야 의미 — args.env=true일 때만 활성.
// 이게 v-next가 증명한 "정확도가 갈리는 차원"(런타임 데이터/동작)을 메우는 배선이다.
const LENSES = [
  {
    key: 'execution-grounded', dim: 'build/types', tool: 'ground-check.sh', envGated: false,
    instruction:
      '판단 전에 반드시 `bash scripts/harness/ground-check.sh <file>` 를 실행하라(필요시 `--test`). 그 실제 컴파일/테스트 출력을 toolEvidence에 인용하고, 타입·빌드·시그니처 차원의 주장은 추측이 아니라 그 결과로 판정하라. 컴파일 통과면 그 차원은 grounding-clean.',
  },
  {
    key: 'spec-retrieval', dim: 'spec/contract', tool: 'plan-search.sh', envGated: false,
    instruction:
      '`bash scripts/harness/plan-search.sh section <문서> "<키워드>"` 또는 `grep <질의>` 로 지배 spec의 해당 절만 가져와 대조하라(통째 읽기 금지). 가져온 spec 절을 toolEvidence에 인용. 주장이 "존재하지 않는 규칙"을 들먹이면 그 규칙 문구가 plan/ 어디에도 없음을 확인해 refute.',
  },
  {
    key: 'code-reality', dim: 'code path/correctness', tool: 'Read/grep + ground-check', envGated: false,
    agentType: 'harness-code-reviewer',
    instruction:
      '인용 파일을 Read 하고 grep으로 주장된 코드 경로가 실재하는지 직접 확인하라. DDD/계약 정합성(오케스트레이션 vs 도메인로직, 인터페이스, OCP 분기 vs 컬럼투영)을 네 시스템 프롬프트 기준으로 판정. 확인한 파일:라인을 toolEvidence에 인용.',
  },
  {
    key: 'api-currency', dim: 'framework API', tool: 'context7 MCP', envGated: false,
    instruction:
      '코드가 프레임워크/라이브러리 API(Spring·JPA·Next.js 등)를 쓰면, context7 MCP를 ToolSearch로 로드(`mcp__context7__resolve-library-id` → `mcp__context7__query-docs`)해 *최신 문서* 기준으로 deprecated·시그니처 오용·환각 API를 확인하라. 네 학습데이터가 아니라 context7이 반환한 문서를 toolEvidence에 인용. 해당 없으면 "none(no external API)".',
  },
  {
    key: 'security', dim: 'security', tool: 'security-review 에이전트', envGated: false,
    agentType: 'harness-security-reviewer',
    instruction:
      '변경분에 보안 차원(인증/인가 우회·IDOR, 비밀 노출, 인젝션, 불안전 역직렬화, 트랜잭션 무결성)이 있는지 네 시스템 프롬프트 기준으로 데이터 흐름을 따라가 확인하라. 기존 프레임워크 방어(Spring Security·JPA 바인딩)가 막고 있으면 refute. 보안 무관이면 "none(not security)".',
  },
  {
    key: 'runtime-data', dim: 'persisted state', tool: 'Postgres MCP / psql / sqlite', envGated: true,
    agentType: 'harness-runtime-verifier',
    instruction:
      '주장이 *런타임 데이터 상태*에 의존하면(예: 특정 user의 설정 행 존재 여부, 특정 레코드 값) — 이건 코드 추론으로 못 푸는 차원이다 — 실제 데이터스토어를 쿼리해 확인하라. Postgres MCP(ToolSearch로 로드) 또는 `psql`/`sqlite3`. 쿼리문과 결과 값을 toolEvidence에 인용. 데이터 의존이 아니면 "none(not data-dependent)".',
  },
  {
    key: 'behavior-runtime', dim: 'observed behavior', tool: 'verify 스킬 / playwright MCP', envGated: true,
    agentType: 'harness-runtime-verifier',
    instruction:
      '주장이 *실제 실행 동작*에 관한 것이면(엔드포인트 응답, 폼 저장→발송, 렌더 결과), 앱을 구동해 해당 경로를 실제 실행하고 관찰하라(verify 스킬 방법론: 코드 import 말고 표면=CLI/소켓/픽셀까지 가서 실제 출력 캡처). 프론트 e2e면 playwright MCP(ToolSearch). 캡처한 실제 출력을 toolEvidence에 인용. 런타임 동작 주장이 아니면 "none(not behavioral)".',
  },
]

const activeLenses = LENSES.filter((l) => includeEnv || !l.envGated)

function verifyPrompt(f, lens) {
  return [
    '너는 AnchorIQ 하네스의 적대적 검증자다. 아래 finding을 REFUTE(반증)하려 시도하라.',
    '핵심 규칙: 추측으로 판정하지 말고 반드시 너의 렌즈가 지정한 도구를 실제로 실행해 그 출력을 근거로 삼아라.',
    '',
    `[finding]`,
    `- 파일: ${f.file}`,
    `- 지배 규칙: ${f.rule || '(미지정)'}`,
    `- 주장: ${f.desc}`,
    '',
    `[너의 렌즈: ${lens.key} — 차원: ${lens.dim} — 도구: ${lens.tool}]`,
    lens.instruction,
    '',
    'toolEvidence에는 네가 실제로 실행한 도구와 그 출력을 인용하라(도구를 못 썼으면 "none" + 이유).',
    '확신이 없으면 refuted=true 가 기본값(거짓양성으로 닫는 쪽이 코더를 덜 가둔다). refuted=false 는 위반이 실재하고 막을 가치가 있다는 도구 근거가 있을 때만.',
    '단, 네 렌즈 차원이 이 finding과 무관하면(toolEvidence="none(not ...)") 약한 의견으로 두고 confidence=low 로 표기하라 — 무관한 차원이 다수결을 왜곡하지 않게.',
  ].join('\n')
}

log(`${findings.length}건 검증 시작 (각 ${activeLenses.length}렌즈 — 도구 grounding${includeEnv ? ' +런타임' : ', 정적만'})`)

const verdicts = await parallel(
  findings.map((f, i) => async () => {
    const votes = await parallel(
      activeLenses.map((lens) => async () => {
        const opts = {
          label: `gverify:${(f.id || i)}:${lens.key}`,
          phase: 'Verify',
          schema: VERDICT,
        }
        if (lens.agentType) opts.agentType = lens.agentType // 커스텀 검증 에이전트 부착 (code/security/runtime)
        const v = await agent(verifyPrompt(f, lens), opts)
        return v ? { lens: lens.key, dim: lens.dim, ...v } : null
      })
    )
    const cast = votes.filter(Boolean)
    // 무관 차원("none(not ...)" + low)은 다수결에서 제외 — 해당 차원 렌즈만 집계
    const relevant = cast.filter((v) => !(v.confidence === 'low' && /^none\(not /i.test(v.toolEvidence || '')))
    const tally = relevant.length ? relevant : cast
    const refutedVotes = tally.filter((v) => v.refuted).length
    const majority = Math.floor(tally.length / 2) + 1
    const confirmed = refutedVotes < majority // 과반 반증이면 거짓양성으로 폐기
    const grounded = cast.filter((v) => v.toolEvidence && !/^none/i.test(v.toolEvidence)).length
    return {
      id: f.id || `idx-${i}`,
      file: f.file,
      desc: f.desc,
      confirmed,
      refutedVotes,
      total: tally.length,
      groundedVotes: grounded,
      votes: cast.map((v) => ({ lens: v.lens, dim: v.dim, refuted: v.refuted, confidence: v.confidence, toolEvidence: v.toolEvidence, reason: v.reason })),
    }
  })
)

const clean = verdicts.filter(Boolean)
const confirmed = clean.filter((v) => v.confirmed)
const totalGrounded = clean.reduce((s, v) => s + (v.groundedVotes || 0), 0)
log(`판정 완료 — confirmed ${confirmed.length} / refuted ${clean.length - confirmed.length} · 도구 grounding ${totalGrounded}표 · 렌즈 ${activeLenses.map((l) => l.key).join(',')}`)

return {
  summary: { total: clean.length, confirmed: confirmed.length, refuted: clean.length - confirmed.length, groundedVotes: totalGrounded, lenses: activeLenses.map((l) => l.key), envEnabled: includeEnv },
  verdicts: clean,
}
