const { chromium } = require('playwright');
(async () => { const b = await chromium.launch(); const p = await b.newPage();
  await p.goto('file://' + require('path').resolve(process.argv[2]), {waitUntil:'load'});
  await p.pdf({path: process.argv[3], format:'A4', printBackground:true, displayHeaderFooter:true, headerTemplate:'<div></div>',
    footerTemplate:'<div style="font-size:7px;width:100%;padding:0 9mm;color:#5b6575;display:flex;justify-content:space-between"><span>PF Credit Policy – Critical Points · Internal</span><span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span></div>',
    margin:{top:'9mm',bottom:'11mm',left:'9mm',right:'9mm'}});
  await b.close(); })();
