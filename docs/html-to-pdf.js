const puppeteer = require('puppeteer');
const path = require('path');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();

  const filePath = path.resolve(__dirname, 'CS_FINAL_COMPLETE.html');
  await page.goto('file://' + filePath, { waitUntil: 'networkidle0' });

  await page.pdf({
    path: 'CS_FINAL_COMPLETE.pdf',
    format: 'A4',
    margin: { top: '22mm', bottom: '22mm', left: '18mm', right: '18mm' },
    printBackground: true,
    displayHeaderFooter: true,
    headerTemplate: '<div></div>',
    footerTemplate: '<div style="font-size:8pt; color:#999; width:100%; text-align:center; padding:0 20px;"><span class="pageNumber"></span> / <span class="totalPages"></span></div>',
  });

  await browser.close();
  console.log('PDF generated: ANCHORIQ_CS_COMPLETE_GUIDE_STYLED.pdf');
})();
