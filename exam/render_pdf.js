const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
(async () => {
  const dir = process.argv[2], out = process.argv[3];
  const html = fs.readdirSync(dir).filter(f=>f.endsWith('.html')).sort().map(f=>fs.readFileSync(path.join(dir,f),'utf8')).join('\n');
  fs.writeFileSync(path.join(path.dirname(out),'training_guide.html'), html);
  const b = await chromium.launch(); const p = await b.newPage();
  await p.setContent(html, {waitUntil:'load'});
  await p.pdf({path: out, format:'A4', printBackground:true, displayHeaderFooter:true,
    headerTemplate:'<div></div>',
    footerTemplate:'<div style="font-size:7.5px;width:100%;padding:0 14mm;color:#5b6575;display:flex;justify-content:space-between"><span>Personal Finance Operating Credit Policy – 100 Exam Questions · Internal</span><span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span></div>',
    margin:{top:'14mm',bottom:'16mm',left:'12mm',right:'12mm'}});
  await b.close();
})();
