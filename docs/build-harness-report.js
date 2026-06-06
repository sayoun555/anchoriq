// HARNESS_ENGINEERING_REPORT.md → 스타일 HTML → (Mermaid를 PNG로 래스터화) → PDF
// 이유: mermaid 라벨은 foreignObject라 화면엔 나오지만 puppeteer page.pdf()에선 빈칸으로 찍힘.
// → 화면에서 SVG를 PNG로 스크린샷해 <img>로 박은 뒤 PDF 생성(인쇄에 확실히 찍힘).
const fs = require('fs');
const path = require('path');
const { marked } = require('marked');
const puppeteer = require('puppeteer');

const DIR = __dirname;
const SRC = path.join(DIR, 'HARNESS_ENGINEERING_REPORT.md');
const OUT_HTML = path.join(DIR, 'HARNESS_ENGINEERING_REPORT.html');
const OUT_PDF = path.join(DIR, 'HARNESS_ENGINEERING_REPORT.pdf');
const MERMAID_JS = path.join(DIR, 'node_modules/mermaid/dist/mermaid.min.js');

let md = fs.readFileSync(SRC, 'utf-8');
const blocks = [];
md = md.replace(/```mermaid\s*\n([\s\S]*?)```/g, (_, code) => {
  blocks.push(code.trim());
  return `\n\n<!--MERMAIDBLOCK${blocks.length - 1}-->\n\n`;
});
let content = marked.parse(md);
blocks.forEach((code, i) => {
  content = content.split(`<!--MERMAIDBLOCK${i}-->`).join(`<div class="mermaid">\n${code}\n</div>`);
});

const css = `
  @page { size: A4; margin: 20mm 16mm 20mm 16mm; }
  * { box-sizing: border-box; }
  body { font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    font-size: 10.5pt; line-height: 1.7; color: #1a1a2e; background: #fff; margin: 0; }
  .cover { page-break-after: always; display: flex; flex-direction: column; justify-content: center;
    align-items: center; min-height: 92vh; text-align: center; padding: 60px 40px; }
  .cover .badge { font-size: 10pt; letter-spacing: 3px; color: #e94560; font-weight: 700; margin-bottom: 24px; }
  .cover h1 { font-size: 30pt; font-weight: 800; color: #0f3460; margin: 0 0 14px; letter-spacing: -0.5px; border: none; }
  .cover .subtitle { font-size: 13pt; color: #16213e; margin-bottom: 36px; font-weight: 400; }
  .cover .line { width: 80px; height: 4px; background: linear-gradient(90deg, #0f3460, #e94560); border-radius: 2px; margin: 26px auto; }
  .cover .meta { font-size: 10.5pt; color: #555; line-height: 2; }
  h1 { font-size: 21pt; font-weight: 800; color: #0f3460; margin-top: 46px; margin-bottom: 16px;
    padding-bottom: 9px; border-bottom: 3px solid #0f3460; page-break-before: always; letter-spacing: -0.3px; }
  h1:first-of-type { page-break-before: avoid; }
  h2 { font-size: 15pt; font-weight: 700; color: #16213e; margin-top: 32px; margin-bottom: 11px;
    padding-left: 13px; border-left: 4px solid #e94560; }
  h3 { font-size: 12pt; font-weight: 700; color: #1a1a2e; margin-top: 24px; margin-bottom: 9px; }
  p { margin-bottom: 9px; word-break: keep-all; }
  strong { color: #0f3460; font-weight: 700; }
  pre { background: #0d1117; color: #e6edf3; border-radius: 8px; padding: 14px 18px; font-size: 8.5pt;
    line-height: 1.55; overflow-x: auto; margin: 12px 0 16px; border-left: 4px solid #e94560; page-break-inside: avoid; }
  code { font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace; font-size: 8.5pt; }
  p code, li code, td code { background: #f0f4f8; color: #d63384; padding: 1px 5px; border-radius: 4px; font-size: 8.5pt; }
  table { width: 100%; border-collapse: collapse; margin: 12px 0 16px; font-size: 9pt; page-break-inside: avoid; }
  thead { background: #0f3460; color: #fff; }
  th { padding: 8px 12px; text-align: left; font-weight: 600; font-size: 9pt; }
  td { padding: 7px 12px; border-bottom: 1px solid #e8edf2; vertical-align: top; }
  tbody tr:nth-child(even) { background: #f8f9fc; }
  blockquote { background: linear-gradient(135deg, #f0f4ff 0%, #fef3f5 100%); border-left: 4px solid #e94560;
    border-radius: 0 8px 8px 0; padding: 12px 18px; margin: 14px 0; color: #1a1a2e; page-break-inside: avoid; }
  blockquote p { margin-bottom: 4px; }
  ul, ol { padding-left: 22px; margin-bottom: 11px; }
  li { margin-bottom: 4px; }
  hr { border: none; height: 1px; background: linear-gradient(90deg, transparent, #ccc, transparent); margin: 26px 0; }
  .mermaid { text-align: center; margin: 18px 0 22px; padding: 12px; background: #fbfcfe;
    border: 1px solid #e8edf2; border-radius: 10px; page-break-inside: avoid; }
  .mermaid img { max-width: 100%; max-height: 232mm; height: auto; display: block; margin: 0 auto; }
  @media print { h2, h3 { page-break-after: avoid; } }
`;

const html = `<!DOCTYPE html>
<html lang="ko"><head><meta charset="UTF-8">
<title>AnchorIQ 하네스 엔지니어링 보고서</title>
<style>${css}</style>
</head><body>
<div class="cover">
  <div class="badge">AGENT HARNESS ENGINEERING</div>
  <h1>AnchorIQ 하네스 엔지니어링</h1>
  <div class="subtitle">설계 규칙을 강제 훅과 적대 검증으로 — 기능 · 흐름 · 실증</div>
  <div class="line"></div>
  <div class="meta">
    4개 훅 &middot; 전파 3채널 &middot; 6렌즈 검증 패널<br>
    대규모 코드 오케스트레이션 &middot; eval 12실험 capstone<br>
    <strong>동료심사 논문 16편 근거 &middot; 측정 기반</strong><br><br>
    <span style="color:#999;">2026.06</span>
  </div>
</div>
${content}
<script src="file://${MERMAID_JS}"></script>
</body></html>`;

fs.writeFileSync(OUT_HTML, html);
console.log('HTML 생성:', (html.length / 1024).toFixed(1), 'KB · mermaid 블록', blocks.length, '개');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new', args: ['--no-sandbox', '--disable-setuid-sandbox'] });
  const page = await browser.newPage();
  await page.setViewport({ width: 1100, height: 1400, deviceScaleFactor: 2 });
  await page.goto('file://' + OUT_HTML, { waitUntil: 'load', timeout: 60000 });
  // 1) mermaid 렌더(화면)
  await page.evaluate(async () => {
    window.mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'loose' });
    await window.mermaid.run({ querySelector: '.mermaid', suppressErrors: true });
  });
  await new Promise((r) => setTimeout(r, 800));
  // 2) 각 svg를 PNG(2x)로 스크린샷 → base64
  const svgs = await page.$$('.mermaid svg');
  const pngs = [];
  for (const el of svgs) { pngs.push(await el.screenshot({ encoding: 'base64' })); }
  // 3) .mermaid 내용을 <img>로 교체(foreignObject 인쇄 문제 회피)
  await page.evaluate((pngs) => {
    document.querySelectorAll('.mermaid').forEach((d, i) => {
      if (pngs[i]) d.innerHTML = `<img src="data:image/png;base64,${pngs[i]}">`;
    });
  }, pngs);
  await new Promise((r) => setTimeout(r, 300));
  await page.pdf({
    path: OUT_PDF, format: 'A4',
    margin: { top: '20mm', bottom: '20mm', left: '16mm', right: '16mm' },
    printBackground: true, displayHeaderFooter: true, headerTemplate: '<div></div>',
    footerTemplate: '<div style="font-size:8pt; color:#999; width:100%; text-align:center; padding:0 20px;"><span class="pageNumber"></span> / <span class="totalPages"></span></div>',
  });
  await browser.close();
  console.log('PDF 생성 완료:', OUT_PDF, '| 다이어그램', pngs.length, '개 PNG 래스터화');
})();
