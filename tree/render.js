const { chromium } = require('playwright');
(async () => { const b = await chromium.launch(); const p = await b.newPage();
  await p.goto('file://' + require('path').resolve(process.argv[2]), {waitUntil:'load'});
  await p.pdf({path: process.argv[3], format:'A4', landscape:true, printBackground:true, margin:{top:'0',bottom:'0',left:'0',right:'0'}});
  await b.close(); })();
