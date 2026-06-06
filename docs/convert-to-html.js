const fs = require('fs');
const { marked } = require('marked');

const md = fs.readFileSync('CS_FINAL_COMPLETE.md', 'utf-8');
const content = marked.parse(md);

const html = `<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>AnchorIQ CS 완전 가이드</title>
<style>
  @page {
    size: A4;
    margin: 22mm 18mm 22mm 18mm;
  }

  * {
    box-sizing: border-box;
  }

  body {
    font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    font-size: 10.5pt;
    line-height: 1.75;
    color: #1a1a2e;
    background: #fff;
    max-width: 100%;
    margin: 0;
    padding: 0;
  }

  /* ── 표지 ── */
  .cover {
    page-break-after: always;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    min-height: 90vh;
    text-align: center;
    padding: 60px 40px;
  }
  .cover h1 {
    font-size: 32pt;
    font-weight: 800;
    color: #0f3460;
    margin-bottom: 12px;
    letter-spacing: -0.5px;
  }
  .cover .subtitle {
    font-size: 14pt;
    color: #16213e;
    margin-bottom: 40px;
    font-weight: 400;
  }
  .cover .meta {
    font-size: 11pt;
    color: #666;
    line-height: 2;
  }
  .cover .line {
    width: 80px;
    height: 4px;
    background: linear-gradient(90deg, #0f3460, #e94560);
    border-radius: 2px;
    margin: 30px auto;
  }

  /* ── 제목 ── */
  h1 {
    font-size: 22pt;
    font-weight: 800;
    color: #0f3460;
    margin-top: 50px;
    margin-bottom: 16px;
    padding-bottom: 10px;
    border-bottom: 3px solid #0f3460;
    page-break-before: always;
    letter-spacing: -0.3px;
  }
  h1:first-of-type {
    page-break-before: avoid;
  }

  h2 {
    font-size: 16pt;
    font-weight: 700;
    color: #16213e;
    margin-top: 36px;
    margin-bottom: 12px;
    padding-left: 14px;
    border-left: 4px solid #e94560;
  }

  h3 {
    font-size: 12.5pt;
    font-weight: 700;
    color: #1a1a2e;
    margin-top: 28px;
    margin-bottom: 10px;
  }

  h4 {
    font-size: 11pt;
    font-weight: 600;
    color: #333;
    margin-top: 20px;
    margin-bottom: 8px;
  }

  /* ── 본문 ── */
  p {
    margin-bottom: 10px;
    text-align: justify;
    word-break: keep-all;
  }

  strong {
    color: #0f3460;
    font-weight: 700;
  }

  /* ── 코드 ── */
  pre {
    background: #0d1117;
    color: #e6edf3;
    border-radius: 8px;
    padding: 16px 20px;
    font-size: 9pt;
    line-height: 1.6;
    overflow-x: auto;
    margin: 14px 0 18px 0;
    border-left: 4px solid #e94560;
    page-break-inside: avoid;
  }

  code {
    font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
    font-size: 9pt;
  }

  p code, li code, td code {
    background: #f0f4f8;
    color: #d63384;
    padding: 1px 6px;
    border-radius: 4px;
    font-size: 9pt;
  }

  /* ── 테이블 ── */
  table {
    width: 100%;
    border-collapse: collapse;
    margin: 14px 0 18px 0;
    font-size: 9.5pt;
    page-break-inside: avoid;
  }

  thead {
    background: #0f3460;
    color: #fff;
  }

  th {
    padding: 10px 14px;
    text-align: left;
    font-weight: 600;
    font-size: 9.5pt;
  }

  td {
    padding: 9px 14px;
    border-bottom: 1px solid #e8edf2;
    vertical-align: top;
  }

  tbody tr:nth-child(even) {
    background: #f8f9fc;
  }

  tbody tr:hover {
    background: #eef2f7;
  }

  /* ── 인용 (박스) ── */
  blockquote {
    background: linear-gradient(135deg, #f0f4ff 0%, #fef3f5 100%);
    border-left: 4px solid #e94560;
    border-radius: 0 8px 8px 0;
    padding: 14px 20px;
    margin: 16px 0;
    font-style: normal;
    color: #1a1a2e;
  }

  blockquote p {
    margin-bottom: 4px;
  }

  /* ── 리스트 ── */
  ul, ol {
    padding-left: 24px;
    margin-bottom: 12px;
  }

  li {
    margin-bottom: 5px;
  }

  /* ── 구분선 ── */
  hr {
    border: none;
    height: 1px;
    background: linear-gradient(90deg, transparent, #ccc, transparent);
    margin: 30px 0;
  }

  /* ── 키워드 하이라이트 박스 ── */
  .highlight-box {
    background: #fff3cd;
    border: 1px solid #ffc107;
    border-radius: 8px;
    padding: 12px 16px;
    margin: 12px 0;
  }

  /* ── 인쇄 최적화 ── */
  @media print {
    body {
      font-size: 10pt;
    }
    pre {
      font-size: 8.5pt;
      page-break-inside: avoid;
    }
    h1 {
      page-break-before: always;
    }
    h2, h3 {
      page-break-after: avoid;
    }
    table {
      page-break-inside: avoid;
    }
    blockquote {
      page-break-inside: avoid;
    }
  }
</style>
</head>
<body>

<div class="cover">
  <h1 style="page-break-before:avoid; border-bottom:none; font-size:36pt; margin-top:0;">AnchorIQ</h1>
  <div class="subtitle">CS 완전 가이드 — 시스템 동작 흐름</div>
  <div class="line"></div>
  <div class="meta">
    비전공자를 위한 컴퓨터 과학 기초부터<br>
    실제 코드 레벨 동작까지<br><br>
    <strong>네트워크 &middot; HTTP &middot; REST API &middot; Spring Boot &middot; DDD</strong><br>
    <strong>JWT 인증 &middot; Neo4j &middot; Redis &middot; Kafka &middot; Docker</strong><br><br>
    <span style="color:#999;">2026.04</span>
  </div>
</div>

${content}

</body>
</html>`;

fs.writeFileSync('CS_FINAL_COMPLETE.html', html);
console.log('HTML generated: ANCHORIQ_CS_COMPLETE_GUIDE.html');
console.log('Size: ' + (html.length / 1024).toFixed(1) + ' KB');
