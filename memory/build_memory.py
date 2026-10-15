"""Build the PF Policy Memory Kit: printable PDF source (memory_kit.html) and offline flashcard app."""
import json, html, pathlib
from cards import CARDS

HERE = pathlib.Path(__file__).parent
TOPIC_COL = {"Segments":"#0e7c86","Schemes":"#6b3fa0","Min income":"#0b7a4b","Max finance":"#1f5fbf",
 "CBUAE limits":"#0b3d6b","Age":"#14a06a","Scorecard & AECB":"#c0392b","History":"#a93226",
 "Holiday & EMI":"#0e7c86","Special products":"#b86e00","Relief":"#8e44ad","Restricted":"#c0392b",
 "Documents":"#5b6575","Delegations":"#0b3d6b"}

def e(s): return html.escape(s)

CSS = """
@page{size:A4 landscape;margin:8mm 9mm 10mm}
*{box-sizing:border-box}
body{margin:0;font-family:"DejaVu Sans",Arial,sans-serif;color:#1d2430;background:#fff;font-size:8.4pt;line-height:1.35}
.pg{break-after:page;position:relative}
.pg:last-child{break-after:auto}
h1{margin:0 0 3px;font-size:16pt;color:#0b3d6b} h2{margin:8px 0 4px;font-size:11pt;color:#0b3d6b}
.sub{color:#5b6575;font-size:8pt;margin-bottom:6px}
.cover{height:186mm;border-radius:14px;background:linear-gradient(140deg,#0b7a4b,#0e7c86 45%,#1f5fbf);color:#fff;padding:16mm 16mm;display:flex;flex-direction:column;justify-content:space-between}
.cover h1{color:#fff;font-size:30pt;line-height:1.1}
.cover .steps{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}
.cover .st{background:rgba(255,255,255,.15);border-radius:10px;padding:10px 12px;font-size:9.4pt}
.cover .st b{display:block;font-size:12pt;margin-bottom:3px}
/* mind map */
.map{position:relative;width:1065px;height:650px;margin-top:2px}
.map svg{position:absolute;left:0;top:0}
.center{position:absolute;left:425px;top:265px;width:215px;height:120px;border-radius:60px;background:linear-gradient(135deg,#0b7a4b,#1f5fbf);color:#fff;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;font-weight:bold;font-size:13pt;box-shadow:0 2px 6px rgba(0,0,0,.2)}
.center small{font-size:7.5pt;font-weight:normal;opacity:.95}
.br{position:absolute;width:300px;border:2.5px solid var(--c);border-radius:10px;background:#fff;padding:0 0 4px;font-size:7.7pt}
.br h3{margin:0 0 3px;background:var(--c);color:#fff;font-size:9pt;padding:3px 8px;border-radius:7px 7px 0 0}
.br ul{margin:0;padding:0 6px 0 18px}.br li{margin:1px 0}
/* hooks */
.hooks{display:grid;grid-template-columns:1fr 1fr 1fr;gap:6px}
.hk{border-left:6px solid var(--c);background:#f7f9fb;border-radius:8px;padding:5px 9px;break-inside:avoid}
.hk .code{font-family:"DejaVu Sans Mono",monospace;font-weight:bold;font-size:11pt;color:var(--c)}
.hk .t{font-weight:bold;font-size:8.6pt}
.hk table{border-collapse:collapse;width:100%;font-size:7.6pt;margin-top:2px}
.hk td{border-bottom:1px dotted #c9d1db;padding:1px 3px}
/* flashcards */
table.fc{border-collapse:collapse;width:100%;font-size:8pt}
table.fc td,table.fc th{border:1px solid #c9d1db;padding:3px 5px;vertical-align:top}
table.fc th{background:#0b3d6b;color:#fff;text-align:left}
table.fc tr{break-inside:avoid}
td.num{width:26px;text-align:center;font-weight:bold;color:#fff}
td.q{width:40%} td.a{background:#f2faf6;border-left:3px dashed #0b7a4b}
td.box{width:78px;font-size:7pt;color:#5b6575;white-space:nowrap}
.sched{border-collapse:collapse;width:100%;font-size:8.6pt}
.sched td,.sched th{border:1px solid #c9d1db;padding:5px 7px;vertical-align:top}
.sched th{background:#0b7a4b;color:#fff;text-align:left}
.tip{background:#fff6c2;border-left:4px solid #e0a800;border-radius:6px;padding:6px 10px;margin-top:8px}
"""

BRANCHES = [
 # (x, y, color, title, items)
 (5,  5,  "#14a06a","① WHO &amp; AGE",["Min age <b>21</b> (18 cash-secured)","UAEN 18–21: DIB salary, lower of <b>10x / 250K</b>, no PDC","Max at maturity: Expat <b>60</b> (65 if ≥ 30K)","UAEN <b>65</b> · pension only <b>75</b> · Private Banking <b>75</b>","GCC nationals = <b>Expat</b>"]),
 (5,  168,"#0e7c86","② WHERE IS THE SALARY? → SEGMENT",["DIB + TML <b>A/B/C</b> + STL → <b>STL</b>","DIB + TML <b>D / Non-TML</b> → <b>Salaried/Privileged</b>","Other bank → <b>PDC TML</b> (A–D) / <b>PDC NTML</b>","<b>3 salaries</b>, latest in DIB (DIB segments)","DIB staff = Cat A · BFI = Cat B"]),
 (5,  336,"#1f5fbf","③ HOW MUCH? (min income &amp; max)",["Min income: DIB A/B/C <b>3K</b> · D/NTML <b>5K</b> · PDC <b>5K</b>","Local: DIB <b>5M · 2M · 800K</b> · PDC <b>800K · 200K</b>","Expat: DIB <b>3M · 1M · 500K/200K</b> · PDC <b>300K · 150K</b>","Contactless: Local <b>750K</b> · Expat <b>350K</b>","Min finance <b>10K</b> fresh · <b>5K</b> top-up"]),
 (5,  500,"#0b3d6b","④ CBUAE HARD LIMITS",["DBR <b>50%</b> salary · <b>30%</b> pension","Multiples <b>20x</b> · <b>15x</b> MOD","Tenure <b>48M</b> · <b>60M</b> MOD (min 6M)","Security cheque ≤ <b>120%</b> · HF applicant: no PF <b>2 months</b>","Final amount = <b>LOWEST</b> cap"]),
 (760,5,  "#c0392b","⑤ CHECKS (AECB &amp; history)",["Auto reject: score <b>300</b> · PDC &lt; <b>541</b> · Expat overdue &gt; <b>10K</b>","Current bucket &gt; 0 → stop · never <b>B4</b> in 12M","Expat reject multiples: High <b>0x</b>; Medium <b>12/14/20-15/20x</b>","Returned cheques 3M: <b>2</b> (STL A/B/C, LS, TD) · <b>1</b> others","Codes: <b>8</b> all restricted · <b>2</b> Expats restricted"]),
 (760,168,"#6b3fa0","⑥ SCHEMES",["<b>Fresh</b> (min 10K)","<b>LC top-up</b>: after <b>12M</b>; earlier if &lt; 20x used or income <b>+AED 200</b>; NTML <b>3 EMIs</b>","<b>LS buy-out</b>: TML <b>A–D</b> + staff · settled facility ≤ <b>29 DPD</b>","<b>No LC</b> for PDC Non-TML","<b>Sukuk Pack</b>: with AF/HF, same docs"]),
 (760,336,"#b86e00","⑦ SPECIAL PRODUCTS",["TD: <b>50/100/95/90%</b> of block · <b>100M</b> / <b>10M</b>","Auto Sukuk: <b>30%</b> STL / <b>20%</b> PDC &amp; NTML · <b>200K/50K</b>","HF Sukuk: ancillary costs only · 1st EMI ≤ <b>90 days</b>","Cross sell: income <b>8K</b> · PF <b>350K/1M</b> · aggregate <b>500K/1M</b> · <b>30 days</b>","Private Banking: <b>20x</b> · Wajaha <b>1M</b> · Ayaan <b>500K</b>"]),
 (760,500,"#8e44ad","⑧ RELIEF &amp; RESTRICTED",["Relief: ≤ <b>89 DPD</b> · salary ≤ <b>40 days</b> · liability ≥ <b>10K</b>","All facilities → <b>1</b> · no cap · no cash · no fees · <b>Arrangement</b>, Stage 2","New finance after <b>12</b> payments","<b>Closed</b>: Self-Employed (Oct 2022)","<b>Restricted</b>: Stateless, Marsoom, domestic workers, outsource, auditors"]),
]

HOOKS = [
 ("#0e7c86","D-P-S-B-R-X","The 6 segment branches",[("D","DIB salary (STL · Salaried/Privileged)"),("P","PDC – salary in other bank"),("S","Secured / relationship (TD · Private Banking)"),("B","Bundled (Auto/HF Sukuk Pack · Cross Sell)"),("R","Relief"),("X","Closed / restricted")]),
 ("#0b3d6b","50-30 · 20-15 · 48-60","CBUAE hard limits (no deviation)",[("50 / 30","DBR salary / pension"),("20 / 15","Multiples non-MOD / MOD"),("48 / 60","Tenure months non-MOD / MOD"),("120%","Max security cheque")]),
 ("#0b7a4b","3 · 5 · 5 · 5 · 8","Minimum income",[("3K","DIB salary TML A/B/C"),("5K","DIB salary TML D"),("5K","DIB salary Non-TML"),("5K","PDC (TML &amp; Non-TML)"),("8K","Asset cross sell")]),
 ("#1f5fbf","5 – 2 – 0.8 | 0.8 – 0.2","Local max finance (millions)",[("5M / 2M / 800K","DIB salary A/B/C · D · NTM"),("800K / 200K","PDC TML · NTM")]),
 ("#1f5fbf","3 – 1 – ½ | 0.3 – 0.15","Expat max finance (millions)",[("3M / 1M","DIB salary A/B/C · D"),("500K / 200K","DIB Non-TML low risk · med/high risk"),("300K / 150K","PDC TML · NTM")]),
 ("#14a06a","21 · 60/65 · 65/75","Age",[("21","Minimum (18 cash-secured)"),("60 / 65","Expat max (65 if income ≥ 30K)"),("65 / 75","UAEN max (75 pension only; PB 75)")]),
 ("#c0392b","300 · 541 · 10K","AECB auto-reject",[("300","Score 300 → reject (all)"),("541","PDC score below 541 → reject"),("10K","Expat overdue above 10,000 → reject")]),
 ("#c0392b","0 · 12 · 14 · 20/15 · 20","Expat reject multiples by income",[("High","0x hard reject"),("Medium &lt;5K","12x"),("Medium 5–8K","14x"),("Medium 8–15K","20x DIB Salaried/LS · 15x others"),("Medium ≥15K / Low","20x")]),
 ("#0e7c86","120 · 240-180-180 · 180-150-150-120-90","Payment holiday (days)",[("120","DIB staff"),("240 / 180 / 180","UAEN: DIB ≥25K / DIB &lt;25K / PDC"),("180 / 150","Expat DIB ≥35K / &lt;35K"),("150 / 120 / 90","Expat PDC ≥35K / 10–35K / &lt;10K")]),
 ("#b86e00","50 · 100 · 95 · 90","Against TD – % of block",[("50%","Salaried, annual income ≤ 60K"),("100%","Salaried &gt; 60K · Self-employed"),("95%","Non-resident"),("90%","SIP-3D Maximizer")]),
 ("#b86e00","8 · 10 · 350/1M · 500/1M · 30","Asset cross sell",[("8K","Min income"),("10K","Min PF"),("350K / 1M","Max PF Expat / National"),("500K / 1M","Aggregate Expat / National"),("30","Days approval validity")]),
 ("#8e44ad","89 · 40 · 10K · 12","Finance Payment Relief",[("89","Max DPD (Bucket 4+ → SAM)"),("40","Days – salary credited within"),("10K","Minimum liability"),("12","Regular payments before new finance (3 if income improved)")]),
 ("#1f5fbf","175/500 · 750/350","Digital &amp; contactless",[("175K / 500K","Digital auto-pass Expat / UAEN"),("750K / 350K","Contactless cap Local / Expat")]),
 ("#6b3fa0","12 · 20x · +200 · 3","Top-up (LC) before 12 months",[("12","Months normal wait"),("&lt;20x","Previous finance below 20x → allowed"),("+200","Income up by AED 200"),("3","Non-TML: instalments paid")]),
]

def mindmap():
    cx, cy = 532, 325
    paths = []
    boxes = []
    for x,y,c,t,items in BRANCHES:
        bx = x+300 if x < 400 else x
        by = y+70
        mx = (cx+bx)/2
        paths.append(f'<path d="M{cx},{cy} C{mx},{cy} {mx},{by} {bx},{by}" stroke="{c}" stroke-width="5" fill="none" stroke-linecap="round" opacity=".85"/>')
        lis = "".join(f"<li>{i}</li>" for i in items)
        boxes.append(f'<div class="br" style="left:{x}px;top:{y}px;--c:{c}"><h3>{t}</h3><ul>{lis}</ul></div>')
    svg = f'<svg width="1065" height="650" viewBox="0 0 1065 650">{"".join(paths)}</svg>'
    return f'<div class="map">{svg}{"".join(boxes)}<div class="center">PERSONAL<br>FINANCE<small>Credit Policy · 8 branches</small></div></div>'

def hooks():
    out = []
    for c,code,title,rows in HOOKS:
        tr = "".join(f"<tr><td style='width:34%'><b>{a}</b></td><td>{b}</td></tr>" for a,b in rows)
        out.append(f'<div class="hk" style="--c:{c}"><div class="code">{code}</div><div class="t">{title}</div><table>{tr}</table></div>')
    return '<div class="hooks" style="zoom:.9">' + "".join(out) + "</div>"

def flashcards():
    rows=[]; cur=None
    for i,(t,q,a) in enumerate(CARDS,1):
        c = TOPIC_COL[t]
        if t != cur:
            cur = t
            rows.append(f'<tr><th colspan="4" style="background:{c}">{e(t)}</th></tr>')
        rows.append(f'<tr><td class="num" style="background:{c}">{i}</td><td class="q">{e(q)}</td><td class="a">{e(a)}</td><td class="box">☐ D1 ☐ D3 ☐ D7<br>☐ D14 ☐ D21</td></tr>')
    return '<table class="fc"><thead><tr><th>#</th><th>Question (cover the answer column)</th><th>Answer</th><th>Recall ✓</th></tr></thead><tbody>' + "".join(rows) + "</tbody></table>"

SCHEDULE = """<table class="sched"><tr><th style="width:13%">Day</th><th>What to do (15–25 minutes)</th><th style="width:28%">Tool</th></tr>
<tr><td><b>Day 1</b></td><td>Read the mind map branch by branch. Say each branch out loud from memory, then check. Learn the 14 memory hooks.</td><td>Mind map · Memory hooks</td></tr>
<tr><td><b>Day 2</b></td><td>Go through all flashcards once. Cover the answer, say it, then check. Tick ✓ only if fully correct.</td><td>Flashcards / app</td></tr>
<tr><td><b>Day 3</b></td><td>Repeat only the cards you missed + redraw the mind map on blank paper from memory.</td><td>App "Due" mode · blank paper</td></tr>
<tr><td><b>Day 5</b></td><td>Exam practice: Parts A–E of the 100-question exam. Note every wrong answer → back to its flashcard.</td><td>100 Exam Questions</td></tr>
<tr><td><b>Day 7</b></td><td>All flashcards again (second full round). Recite the 14 hooks without looking.</td><td>App · Hooks</td></tr>
<tr><td><b>Day 10</b></td><td>Exam practice: Parts F–J. Use the decision tree to classify 10 imaginary customers.</td><td>Exam · Segments tree p.2</td></tr>
<tr><td><b>Day 14</b></td><td>Missed cards only + redraw the mind map. Target: 90% of cards in box 4–5 in the app.</td><td>App · blank paper</td></tr>
<tr><td><b>Day 21</b></td><td>Final check: full exam without looking at answers. Target ≥ 90 / 100. Re-study only weak topics.</td><td>100 Exam Questions</td></tr>
<tr><td><b>Monthly</b></td><td>10-minute refresh: hooks + app "Due" cards. Update the cards whenever a new credit circular is issued.</td><td>App</td></tr>
</table>"""

def build_pdf_html():
    n = len(CARDS)
    return f"""<!doctype html><html><head><meta charset="utf-8"><title>PF Policy Memory Kit</title><style>{CSS}</style></head><body>
<div class="pg"><div class="cover"><div><div style="font-size:9pt;letter-spacing:1px;opacity:.9">STUDY KIT • INTERNAL USE</div>
<h1>Personal Finance<br>Credit Policy<br>Memory Kit</h1>
<div style="font-size:12pt;margin-top:6px">Remember the whole policy with 3 proven techniques</div></div>
<div class="steps"><div class="st"><b>1 · See it – Mind map</b>The whole policy on one page in 8 branches. Builds the structure your memory hangs facts on.</div>
<div class="st"><b>2 · Code it – Memory hooks</b>14 number codes (e.g. <b>50-30 · 20-15 · 48-60</b>) that compress every key limit.</div>
<div class="st"><b>3 · Test it – Active recall + spaced repetition</b>{n} flashcards reviewed on Days 1-3-7-14-21. Testing yourself is far more effective than re-reading.</div></div>
<div style="font-size:8pt;opacity:.95">Based on DIB Personal Finance Operating Credit Policy (ref. 2026-07-0005 | 002306) and Credit Circular effective 15 Feb 2026. Companion files: Training Guide · 100 Exam Questions · Segments Tree · Critical Points · offline flashcard app.</div></div></div>

<div class="pg"><h1>Mind Map – the whole policy on one page</h1><div class="sub">Start at the centre and go clockwise from top-left: Who → Where → How much → Limits, then Checks → Schemes → Special products → Relief &amp; restricted. Redraw it from memory on blank paper to test yourself.</div>{mindmap()}</div>

<div class="pg"><h1>Memory Hooks – 14 codes for every key number</h1><div class="sub">Learn the code first, then what each number means. Recite them aloud – each code unlocks a whole table.</div>{hooks()}</div>

<div class="pg"><h1>Flashcards – {n} cards (active recall)</h1><div class="sub">Fold the page or cover the green column. Answer out loud, then check. Tick the day box only if your answer was fully correct. Same cards are in the offline app <b>PF_Flashcards_App.html</b>.</div>{flashcards()}</div>

<div class="pg"><h1>21-Day Spaced Repetition Plan</h1><div class="sub">Short, spaced sessions beat one long session. Review just before you would forget.</div>{SCHEDULE}
<div class="tip"><b>Rules that make it work:</b> (1) Always try to answer <i>before</i> looking. (2) A card is "known" only if every number is correct. (3) Missed cards come back sooner – known cards come back later. (4) Explain a rule to a colleague in your own words – teaching is the strongest test. (5) When a new credit circular is issued, update the matching card the same day.</div></div>
</body></html>"""

APP = r"""<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>PF Policy Flashcards</title>
<style>
:root{--bg:#f4f7fb;--card:#fff;--ink:#1d2430;--muted:#5b6575;--line:#d5dbe3;--brand:#0b3d6b;--ok:#0b7a4b;--no:#c0392b}
@media (prefers-color-scheme:dark){:root{--bg:#10151c;--card:#1a222d;--ink:#e6ebf2;--muted:#9aa6b6;--line:#2c3644;--brand:#5d9be0;--ok:#3cc08a;--no:#ef6b5c}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font-family:system-ui,-apple-system,"Segoe UI",Arial,sans-serif}
header{padding:14px 16px 6px;max-width:760px;margin:0 auto}
h1{font-size:20px;margin:0 0 2px;color:var(--brand)}.muted{color:var(--muted);font-size:13px}
.bar{max-width:760px;margin:8px auto;padding:0 16px;display:flex;flex-wrap:wrap;gap:8px;align-items:center}
select,button{font:inherit;font-size:14px;border-radius:10px;border:1px solid var(--line);background:var(--card);color:var(--ink);padding:8px 12px;cursor:pointer}
.boxes{max-width:760px;margin:6px auto;padding:0 16px;display:grid;grid-template-columns:repeat(5,1fr);gap:6px}
.boxes div{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:6px;text-align:center;font-size:12px}
.boxes b{display:block;font-size:18px}
.wrap{max-width:760px;margin:10px auto;padding:0 16px}
.card{background:var(--card);border:1px solid var(--line);border-radius:16px;padding:22px 20px;min-height:260px;box-shadow:0 2px 10px rgba(0,0,0,.06)}
.topic{display:inline-block;color:#fff;border-radius:12px;padding:2px 10px;font-size:12px;font-weight:600}
.q{font-size:20px;font-weight:600;margin:14px 0;line-height:1.35}
.a{font-size:17px;line-height:1.45;border-top:2px dashed var(--line);padding-top:12px;margin-top:12px;display:none}
.a.show{display:block}
.actions{display:flex;gap:10px;margin-top:14px;flex-wrap:wrap}
.actions button{flex:1;min-width:140px;padding:12px;font-weight:600}
.btn-no{border-color:var(--no);color:var(--no)}.btn-ok{border-color:var(--ok);color:var(--ok)}.btn-flip{background:var(--brand);color:#fff;border-color:var(--brand)}
.done{text-align:center;padding:30px 10px}
.prog{height:8px;background:var(--line);border-radius:6px;overflow:hidden;margin-top:8px}.prog i{display:block;height:100%;background:var(--ok)}
footer{max-width:760px;margin:10px auto 30px;padding:0 16px;font-size:12px;color:var(--muted)}
</style></head><body>
<header><h1>PF Credit Policy – Flashcards</h1>
<div class="muted">Active recall + spaced repetition (Leitner 5 boxes). Answer in your head, flip, then mark honestly. Keys: <b>Space</b> flip · <b>1</b> missed · <b>2</b> knew it.</div></header>
<div class="bar"><select id="topic"></select>
<select id="mode"><option value="due">Due cards first</option><option value="all">All cards (shuffled)</option><option value="weak">Weak only (box 1–2)</option></select>
<button id="reset" title="Clear progress on this device">Reset progress</button></div>
<div class="boxes" id="boxes"></div>
<div class="wrap"><div class="card" id="card"></div><div class="prog"><i id="pbar"></i></div></div>
<footer>Box 1 = review every session · Box 2 = after 1 day · Box 3 = after 3 days · Box 4 = after 7 days · Box 5 = after 14 days. Progress is saved only in this browser. Source: DIB PF Operating Credit Policy (2026-07-0005 | 002306) + circular 15 Feb 2026. Internal use.</footer>
<script>
const CARDS = __CARDS__;
const COLORS = __COLORS__;
const DAYS=[0,0,1,3,7,14], KEY="pf-flashcards-v1";
let state={}; try{state=JSON.parse(localStorage.getItem(KEY)||"{}")}catch(e){state={}}
function save(){try{localStorage.setItem(KEY,JSON.stringify(state))}catch(e){}}
function st(i){return state[i]||{box:1,due:0}}
const today=()=>Math.floor(Date.now()/86400000);
const topicSel=document.getElementById("topic"),modeSel=document.getElementById("mode");
topicSel.innerHTML='<option value="">All topics ('+CARDS.length+')</option>'+[...new Set(CARDS.map(c=>c[0]))].map(t=>'<option>'+t+'</option>').join("");
let queue=[],pos=0,flipped=false;
function build(){const t=topicSel.value,m=modeSel.value;let idx=CARDS.map((c,i)=>i).filter(i=>!t||CARDS[i][0]===t);
 if(m==="weak")idx=idx.filter(i=>st(i).box<=2);
 if(m==="due"){const d=today();const due=idx.filter(i=>st(i).due<=d);idx=due.length?due:idx;idx.sort((a,b)=>st(a).box-st(b).box||Math.random()-.5)}
 else idx.sort(()=>Math.random()-.5);
 queue=idx;pos=0;flipped=false;render()}
function boxes(){const n=[0,0,0,0,0];CARDS.forEach((c,i)=>n[st(i).box-1]++);
 document.getElementById("boxes").innerHTML=n.map((v,i)=>'<div><b>'+v+'</b>Box '+(i+1)+'</div>').join("");
 const known=n[3]+n[4];document.getElementById("pbar").style.width=(100*known/CARDS.length)+"%"}
function esc(s){return s.replace(/[&<>]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;"}[c]))}
function render(){boxes();const el=document.getElementById("card");
 if(pos>=queue.length){el.innerHTML='<div class="done"><h2>Session complete ✔</h2><p class="muted">'+queue.length+' cards reviewed. Come back tomorrow for due cards, or switch mode/topic.</p><button onclick="build()">Start again</button></div>';return}
 const i=queue[pos],c=CARDS[i],s=st(i);
 el.innerHTML='<span class="topic" style="background:'+(COLORS[c[0]]||"#0b3d6b")+'">'+esc(c[0])+'</span> <span class="muted">Card '+(pos+1)+' / '+queue.length+' · box '+s.box+'</span>'+
 '<div class="q">'+esc(c[1])+'</div><div class="a'+(flipped?' show':'')+'">'+esc(c[2])+'</div>'+
 '<div class="actions">'+(flipped?'<button class="btn-no" onclick="mark(0)">✗ Missed (1)</button><button class="btn-ok" onclick="mark(1)">✓ Knew it (2)</button>':'<button class="btn-flip" onclick="flip()">Show answer (Space)</button>')+'</div>'}
function flip(){flipped=true;render()}
function mark(ok){const i=queue[pos],s=st(i);s.box=ok?Math.min(5,s.box+1):1;s.due=today()+DAYS[s.box];state[i]=s;save();pos++;flipped=false;render()}
document.addEventListener("keydown",e=>{if(e.target.tagName==="SELECT")return;if(e.code==="Space"){e.preventDefault();if(!flipped)flip()}else if(flipped&&e.key==="1")mark(0);else if(flipped&&e.key==="2")mark(1)});
topicSel.onchange=build;modeSel.onchange=build;
document.getElementById("reset").onclick=()=>{if(confirm("Clear all progress on this device?")){state={};save();build()}};
build();
</script></body></html>"""

(HERE/"memory_kit.html").write_text(build_pdf_html())
(HERE/"PF_Flashcards_App.html").write_text(APP.replace("__CARDS__", json.dumps(CARDS, ensure_ascii=False)).replace("__COLORS__", json.dumps(TOPIC_COL)))
print("cards:", len(CARDS))
