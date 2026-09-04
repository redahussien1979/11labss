# -*- coding: utf-8 -*-
"""يبني ملخّصًا مبسّطًا (A4) للوحدة 1 «الطاقة الحرارية» بلغة يفهمها ابن العاشرة."""
import html, re
from summary_content import LESSONS, RECAP, BIGIDEA

_esc = html.escape
# الأرقام مع الوحدات اللاتينية (0 K، 1 g، 1°C، 30°N، 20%) تنقلب داخل نص عربي،
# لذا تُعزَل كوحدة واحدة تُكتب من اليسار إلى اليمين.
_UNIT = re.compile(r'(\d+(?:\.\d+)?\s?°?(?:[A-Za-z]+|%)|°[A-Za-z])')


def e(x):
    """تهريب HTML مع عزل الوحدات اللاتينية اتجاهيًّا."""
    return _UNIT.sub(r'<bdi dir="ltr">\1</bdi>', _esc(x))
OUT = "/home/user/11labss/"

FONTS = ('<link rel="preconnect" href="https://fonts.googleapis.com">\n'
         '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>\n'
         '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?'
         'family=Reem+Kufi:wght@400;500;600;700'
         '&family=Tajawal:wght@300;400;500;700;800'
         '&family=IBM+Plex+Mono:wght@400;500;600'
         '&display=swap">')

CSS = """
@page{ size:A4 portrait; }
*{box-sizing:border-box}
html,body{margin:0; padding:0}
:root{
  --ink:#1A2430; --ink-2:#3E4E5C; --ink-3:#6E7F8E;
  --line:#DCE4EB; --tint:#F6F9FB;
  --c1:#0E6E88; --c1-tint:#E4F1F6; --c1-deep:#08556A;
  --c2:#A8620C; --c2-tint:#FBF0E0; --c2-deep:#874E08;
  --c3:#B0301F; --c3-tint:#FBEAE7; --c3-deep:#8C2517;
  --ok:#14713C; --ok-tint:#E6F4EB;
  --accent:var(--c1); --accent-tint:var(--c1-tint); --accent-deep:var(--c1-deep);
}
body{
  background:#fff; color:var(--ink);
  font-family:"Tajawal","Segoe UI",Tahoma,sans-serif;
  font-size:11.2pt; line-height:1.8; direction:rtl;
  -webkit-print-color-adjust:exact; print-color-adjust:exact;
}
h1,h2,h3,h4{font-family:"Reem Kufi","Tajawal",sans-serif; font-weight:600; margin:0}
.k1{--accent:var(--c1); --accent-tint:var(--c1-tint); --accent-deep:var(--c1-deep)}
.k2{--accent:var(--c2); --accent-tint:var(--c2-tint); --accent-deep:var(--c2-deep)}
.k3{--accent:var(--c3); --accent-tint:var(--c3-tint); --accent-deep:var(--c3-deep)}

/* ---------- الغلاف ---------- */
.cover{break-after:page; page-break-after:always}
.hero{background:linear-gradient(115deg,var(--c3) 0%,var(--c2) 34%,var(--c1-deep) 72%,var(--c1) 100%);
  color:#fff; border-radius:14pt; padding:24pt 24pt 22pt; position:relative; overflow:hidden}
.hero::after{content:""; position:absolute; inset:auto auto -80pt -40pt; width:210pt; height:210pt;
  border-radius:50%; border:16pt solid rgba(255,255,255,.10)}
.hero .top{font-size:9pt; opacity:.92; margin-bottom:12pt}
.hero h1{font-size:33pt; line-height:1.14; font-weight:700; margin-bottom:6pt}
.hero p{margin:0; font-size:12.5pt; opacity:.96; max-width:32em; line-height:1.6}
.covergrid{display:grid; grid-template-columns:1fr 1fr 1fr; gap:9pt; margin-top:16pt}
.ccard{border:.8pt solid var(--line); border-top:3.4pt solid var(--accent); border-radius:10pt;
  padding:11pt 11pt 12pt; background:var(--tint)}
.ccard .n{font-family:"IBM Plex Mono",monospace; font-size:16pt; font-weight:600; color:var(--accent)}
.ccard h3{font-size:11.5pt; margin:5pt 0 4pt; line-height:1.35}
.ccard p{margin:0; font-size:9pt; color:var(--ink-2); line-height:1.6}
.bigidea{margin-top:16pt; background:var(--c1-tint); border:.8pt solid var(--c1);
  border-radius:10pt; padding:13pt 15pt}
.bigidea h3{font-size:12pt; color:var(--c1-deep); margin-bottom:4pt}
.bigidea p{margin:0; font-size:10.5pt; color:var(--ink-2); line-height:1.75}
.cover .note{margin-top:14pt; font-size:9pt; color:var(--ink-3); line-height:1.7}

/* ---------- عناوين الدروس ---------- */
section.lesson{break-before:page; page-break-before:always}
.band{background:var(--accent-tint); border-radius:11pt; padding:13pt 15pt;
  border-inline-start:5pt solid var(--accent); margin-bottom:12pt;
  break-after:avoid; page-break-after:avoid}
.band .row{display:flex; align-items:baseline; gap:10pt}
.band .num{font-family:"IBM Plex Mono",monospace; font-size:17pt; font-weight:600; color:var(--accent-deep)}
.band h2{font-size:16pt; color:var(--accent-deep)}
.band .big{margin:6pt 0 0; font-size:11pt; color:var(--ink-2); line-height:1.7; font-weight:500}

/* ---------- الكتل ---------- */
.blk{break-inside:avoid; page-break-inside:avoid; margin-bottom:7pt}
.blk h3{font-size:12.5pt; margin-bottom:4pt; color:var(--accent-deep)}
.blk p{margin:0; line-height:1.8}
.idea{border:.8pt solid var(--line); border-radius:10pt; padding:9pt 12pt; background:#fff}
.fact{border:.8pt solid var(--accent); border-radius:10pt; padding:9pt 12pt; background:var(--accent-tint)}
.warn{border:.8pt solid #C99000; border-radius:10pt; padding:9pt 12pt; background:#FFF7E3}
.warn h3{color:#8A6200}
.pair{display:grid; grid-template-columns:1fr 1fr; gap:9pt}
.pair .p{border:.8pt solid var(--line); border-top:2.6pt solid var(--accent); border-radius:10pt;
  padding:9pt 12pt; background:var(--tint)}
.pair .p h4{font-size:11.5pt; color:var(--accent-deep); margin-bottom:3pt}
.pair .p p{font-size:10.5pt; color:var(--ink-2); line-height:1.72}
.formula{display:grid; gap:7pt}
.frow{display:grid; grid-template-columns:auto 1fr auto; gap:10pt; align-items:center;
  border:.8pt solid var(--line); border-radius:10pt; padding:8pt 12pt; background:var(--tint)}
.frow b{font-size:11.5pt; color:var(--accent-deep); white-space:nowrap}
.frow .eq{font-weight:700; font-size:11pt}
.frow .hint{font-size:9pt; color:var(--ink-3); white-space:nowrap}
table.tbl{width:100%; border-collapse:collapse; font-size:10.5pt}
table.tbl th{background:var(--accent); color:#fff; font-weight:700; padding:5pt 9pt; text-align:start;
  font-size:10pt}
table.tbl th:first-child{border-start-start-radius:8pt}
table.tbl th:last-child{border-start-end-radius:8pt}
table.tbl td{border:.7pt solid var(--line); padding:5pt 9pt}
table.tbl tr:nth-child(even) td{background:var(--tint)}
.math{display:grid; gap:7pt}
.mrow{border:.8pt solid var(--line); border-radius:10pt; padding:8pt 12pt; background:#fff}
.mrow b{display:block; font-size:10.5pt; color:var(--accent-deep)}
.mrow .f{font-family:"IBM Plex Mono",monospace; font-size:11.5pt; font-weight:600; margin:3pt 0;
  background:var(--accent-tint); border-radius:6pt; padding:3pt 9pt; display:inline-block;
  direction:ltr; unicode-bidi:isolate}
.mrow .ex{font-size:9.5pt; color:var(--ink-2)}
.mrow .ex .m{font-family:"IBM Plex Mono",monospace; direction:ltr; unicode-bidi:isolate;
  display:inline-block}
ul.bul{margin:0; padding-inline-start:16pt}
ul.bul li{margin-bottom:4pt; line-height:1.75; font-size:10.8pt}
ul.bul li::marker{color:var(--accent); font-weight:700}
.steps{display:grid; gap:5pt}
.step{display:grid; grid-template-columns:17pt 1fr; gap:9pt; align-items:start;
  border:.8pt solid var(--line); border-radius:9pt; padding:6pt 10pt; background:#fff; font-size:10.6pt;
  line-height:1.65}
.step .b{width:17pt; height:17pt; border-radius:50%; background:var(--accent); color:#fff;
  display:flex; align-items:center; justify-content:center; font-family:"IBM Plex Mono",monospace;
  font-size:8.5pt; font-weight:600; margin-top:2pt}
.compare{display:grid; grid-template-columns:1fr 1fr; gap:9pt}
.cbox{border:.8pt solid var(--line); border-radius:10pt; padding:9pt 12pt; background:var(--tint)}
.cbox h4{font-size:11pt; margin-bottom:2pt}
.cbox .tagline{font-size:9pt; color:var(--accent-deep); font-weight:700; margin-bottom:3pt}
.cbox p{font-size:10.3pt; color:var(--ink-2); margin:0; line-height:1.7}
.chain{display:flex; align-items:center; gap:8pt; flex-wrap:wrap; margin-top:2pt}
.chain .c{background:var(--accent-tint); border:.8pt solid var(--accent); border-radius:999pt;
  padding:5pt 14pt; font-weight:700; font-size:10.5pt; color:var(--accent-deep)}
.chain .ar{color:var(--accent); font-size:14pt; font-weight:700}
figure{margin:0}
figure svg{display:block; width:100%; height:auto}
figcaption{font-size:9pt; color:var(--ink-3); margin-top:5pt; text-align:center}
.figlegend{display:grid; gap:4pt; margin-top:8pt}
.lrow{display:grid; grid-template-columns:10pt auto 1fr; gap:7pt; align-items:baseline;
  font-size:10pt; line-height:1.6}
.lrow .dot{width:10pt; height:10pt; border-radius:50%; align-self:center}
.lrow b{color:var(--ink); white-space:nowrap}
.lrow .v{color:var(--ink-2)}

/* ---------- المراجعة ---------- */
.recap{break-before:page; page-break-before:always}
.recap .head{background:linear-gradient(115deg,var(--c3),var(--c2) 45%,var(--c1) 100%);
  color:#fff; border-radius:11pt; padding:13pt 16pt; margin-bottom:11pt}
.recap .head h2{font-size:17pt}
.recap .head p{margin:2pt 0 0; font-size:10pt; opacity:.95}
.rgrid{display:grid; grid-template-columns:1fr 1fr; gap:6pt}
.ritem{border:.8pt solid var(--line); border-radius:9pt; padding:7pt 11pt; background:#fff;
  break-inside:avoid}
.ritem b{display:block; font-size:10.8pt; color:var(--c1-deep)}
.ritem span{display:block; font-size:9.8pt; color:var(--ink-2); line-height:1.65}
footer.note{margin-top:14pt; padding-top:8pt; border-top:.8pt solid var(--line);
  font-size:9pt; color:var(--ink-3); line-height:1.7}
"""

# ============================= الرسوم =============================
def art_states():
    """ترتيب الجسيمات في المواد الصلبة والسائلة والغازية."""
    def dots(cx, pts, r=5.5):
        return "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="{cx}"/>' for x, y in pts)
    solid = [(x, y) for y in (44, 66, 88, 110) for x in (430, 458, 486, 514, 542)]
    liquid = [(222, 50), (258, 62), (296, 48), (334, 64), (240, 88), (280, 92), (320, 86),
              (356, 100), (232, 116), (272, 118), (312, 116), (350, 60)]
    gas = [(48, 52), (104, 44), (160, 62), (70, 96), (130, 108), (176, 96), (100, 74)]
    streaks = "".join(f'<line x1="{x-12}" y1="{y-8}" x2="{x-3}" y2="{y-2}" '
                      f'stroke="#9BAEBD" stroke-width="2.4" stroke-linecap="round"/>' for x, y in gas)
    return f'''<svg viewBox="0 0 600 152" xmlns="http://www.w3.org/2000/svg" role="img"
 aria-label="ترتيب الجسيمات في المواد الصلبة والسائلة والغازية">
<rect x="404" y="20" width="180" height="106" rx="10" fill="#F2F6F9" stroke="#C3D2DC" stroke-width="1"/>
<rect x="208" y="20" width="180" height="106" rx="10" fill="#F2F6F9" stroke="#C3D2DC" stroke-width="1"/>
<rect x="16"  y="20" width="180" height="106" rx="10" fill="#F2F6F9" stroke="#C3D2DC" stroke-width="1"/>
{dots("#46617A", solid)}{dots("#46617A", liquid)}{streaks}{dots("#46617A", gas)}
<text x="494" y="144" text-anchor="middle" font-family="Tajawal" font-size="13" fill="#3E4E5C">صلبة: تهتز في مكانها</text>
<text x="298" y="144" text-anchor="middle" font-family="Tajawal" font-size="13" fill="#3E4E5C">سائلة: متباعدة قليلًا</text>
<text x="106" y="144" text-anchor="middle" font-family="Tajawal" font-size="13" fill="#3E4E5C">غازية: تنتشر بحرّية</text>
</svg>'''

def art_thermo():
    """ثيرمومتر بسيط — كل النصوص خارج الرسم لتفادي مشكلات اتجاه الكتابة."""
    return '''<svg viewBox="0 0 600 132" xmlns="http://www.w3.org/2000/svg" role="img"
 aria-label="ثيرمومتر عليه علامتا غليان الماء وتجمُّده">
<line x1="252" y1="28" x2="348" y2="28" stroke="#B0301F" stroke-width="1.6" stroke-dasharray="5 4"/>
<line x1="252" y1="86" x2="348" y2="86" stroke="#0E6E88" stroke-width="1.6" stroke-dasharray="5 4"/>
<rect x="286" y="8" width="28" height="98" rx="14" fill="#F6F9FB" stroke="#6E7F8E" stroke-width="1.4"/>
<rect x="293" y="60" width="14" height="52" rx="7" fill="#B0301F"/>
<circle cx="300" cy="114" r="16" fill="#B0301F"/>
<circle cx="252" cy="28" r="4.5" fill="#B0301F"/><circle cx="348" cy="28" r="4.5" fill="#B0301F"/>
<circle cx="252" cy="86" r="4.5" fill="#0E6E88"/><circle cx="348" cy="86" r="4.5" fill="#0E6E88"/>
</svg>'''

def art_ways():
    """الطرائق الثلاث لانتقال الطاقة الحرارية."""
    rays = "".join(f'<line x1="518" y1="58" x2="452" y2="{62 + i*14}" '
                   f'stroke="#B0301F" stroke-width="2.6" stroke-linecap="round"/>' for i in range(3))
    return f'''<svg viewBox="0 0 600 168" xmlns="http://www.w3.org/2000/svg" role="img"
 aria-label="الإشعاع والتوصيل والحمل الحراري">
<rect x="404" y="10" width="180" height="116" rx="10" fill="#FFF" stroke="#DCE4EB" stroke-width="1"/>
<rect x="208" y="10" width="180" height="116" rx="10" fill="#FFF" stroke="#DCE4EB" stroke-width="1"/>
<rect x="16"  y="10" width="180" height="116" rx="10" fill="#FFF" stroke="#DCE4EB" stroke-width="1"/>

<circle cx="540" cy="58" r="20" fill="#E8A33D" stroke="#B0301F" stroke-width="1.5"/>
{rays}
<rect x="418" y="72" width="30" height="26" rx="4" fill="#0E6E88"/>
<text x="494" y="118" text-anchor="middle" font-family="Tajawal" font-size="14" font-weight="700" fill="#8C2517">الإشعاع</text>

<rect x="238" y="46" width="120" height="20" rx="6" fill="#EAF0F4" stroke="#6E7F8E" stroke-width="1"/>
<circle cx="256" cy="56" r="6" fill="#B0301F"/><circle cx="284" cy="56" r="6" fill="#C9702F"/>
<circle cx="312" cy="56" r="6" fill="#A8620C"/><circle cx="340" cy="56" r="6" fill="#0E6E88"/>
<path d="M262 34 L278 34" stroke="#A8620C" stroke-width="2.4" stroke-linecap="round"/>
<path d="M290 34 L306 34" stroke="#A8620C" stroke-width="2.4" stroke-linecap="round"/>
<path d="M318 34 L334 34" stroke="#A8620C" stroke-width="2.4" stroke-linecap="round"/>
<text x="298" y="90" text-anchor="middle" font-family="Tajawal" font-size="12" fill="#6E7F8E">تصطدم فتنقل الطاقة</text>
<text x="298" y="118" text-anchor="middle" font-family="Tajawal" font-size="14" font-weight="700" fill="#874E08">التوصيل</text>

<path d="M52 84 h108 v-46 h-108 z" fill="#DCEBF2" stroke="#0E6E88" stroke-width="1.4"/>
<path d="M76 78 C76 58, 98 58, 98 44" fill="none" stroke="#B0301F" stroke-width="2.6"
 stroke-linecap="round" marker-end="url(#ah)"/>
<path d="M136 44 C136 64, 114 64, 114 78" fill="none" stroke="#0E6E88" stroke-width="2.6"
 stroke-linecap="round" marker-end="url(#ab)"/>
<path d="M62 92 l14 -8 M86 92 l14 -8 M110 92 l14 -8 M134 92 l14 -8"
 stroke="#E8A33D" stroke-width="2.6" stroke-linecap="round"/>
<text x="106" y="118" text-anchor="middle" font-family="Tajawal" font-size="14" font-weight="700" fill="#08556A">الحمل الحراري</text>

<text x="494" y="150" text-anchor="middle" font-family="Tajawal" font-size="12.5" fill="#3E4E5C">بلا لمس، حتى عبر الفراغ</text>
<text x="298" y="150" text-anchor="middle" font-family="Tajawal" font-size="12.5" fill="#3E4E5C">بالتلامس والتصادم</text>
<text x="106" y="150" text-anchor="middle" font-family="Tajawal" font-size="12.5" fill="#3E4E5C">بحركة الموائع</text>
<defs>
<marker id="ah" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto">
<path d="M0 0 L10 5 L0 10 z" fill="#B0301F"/></marker>
<marker id="ab" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto">
<path d="M0 0 L10 5 L0 10 z" fill="#0E6E88"/></marker>
</defs>
</svg>'''

def art_convection():
    """دورة الحمل الحراري — كل النصوص خارج الرسم."""
    return '''<svg viewBox="0 0 600 190" xmlns="http://www.w3.org/2000/svg" role="img"
 aria-label="دورة الحمل الحراري في إناء ماء على موقد">
<path d="M150 30 h300 v122 h-300 z" fill="#DCEBF2" stroke="#0E6E88" stroke-width="2"/>
<rect x="140" y="22" width="320" height="12" rx="6" fill="#B7C6D0"/>
<path d="M226 140 C226 96, 262 96, 262 50" fill="none" stroke="#B0301F" stroke-width="3.6"
 stroke-linecap="round" marker-end="url(#up)"/>
<path d="M374 50 C374 94, 338 94, 338 140" fill="none" stroke="#0E6E88" stroke-width="3.6"
 stroke-linecap="round" marker-end="url(#dn)"/>
<path d="M176 172 l16 -13 M216 172 l16 -13 M256 172 l16 -13 M296 172 l16 -13
 M336 172 l16 -13 M376 172 l16 -13 M416 172 l16 -13"
 stroke="#E8A33D" stroke-width="3.6" stroke-linecap="round"/>
<defs>
<marker id="up" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto">
<path d="M0 0 L10 5 L0 10 z" fill="#B0301F"/></marker>
<marker id="dn" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="5" markerHeight="5" orient="auto">
<path d="M0 0 L10 5 L0 10 z" fill="#0E6E88"/></marker>
</defs>
</svg>'''

ART = {
 "states": (art_states, "الجسيمات موجودة في كل مادة، وتختلف المسافات بينها باختلاف الحالة.", []),
 "thermo": (art_thermo, "المقاييس الثلاثة تصف الشيء نفسه بأرقام مختلفة.", [
     ("#B0301F", "يغلي الماء عند", "212°F · 100°C · 373 K"),
     ("#0E6E88", "يتجمَّد الماء عند", "32°F · 0°C · 273 K"),
     ("#6E7F8E", "الصفر المطلق", "0 K — لا تتحرك الجسيمات ولا تكون لها طاقة حركية")]),
 "ways": (art_ways, "ثلاث طرائق فقط تنتقل بها الطاقة الحرارية.", []),
 "convection": (art_convection, "دورة تتكرَّر حتى يصبح كل الماء عند درجة الحرارة نفسها.", [
     ("#B0301F", "الماء الساخن (السهم الأحمر)", "يتمدَّد ← يصبح أقل كثافةً ← يرتفع"),
     ("#0E6E88", "الماء البارد (السهم الأزرق)", "ينكمش ← يصبح أكثر كثافةً ← يهبط"),
     ("#E8A33D", "الموقد", "يمنح الماء عند القاع طاقة حرارية")]),
}


def figure(name):
    fn, cap, legend = ART[name]
    rows = ""
    if legend:
        items = "".join(
            f'<div class="lrow"><span class="dot" style="background:{c}"></span>'
            f'<b>{e(lab)}</b><span class="v"><bdi>{e(val)}</bdi></span></div>'
            for c, lab, val in legend)
        rows = f'<div class="figlegend">{items}</div>'
    return f'<figure>{fn()}{rows}<figcaption>{e(cap)}</figcaption></figure>'


# ============================= بناء الكتل =============================
def render_block(b):
    k = b["kind"]
    art = figure(b["art"]) if b.get("art") else ""
    if k == "idea":
        return f'<div class="blk idea"><h3>{e(b["head"])}</h3><p>{e(b["body"])}</p>{art}</div>'
    if k == "fact":
        return f'<div class="blk fact"><h3>{e(b["head"])}</h3><p>{e(b["body"])}</p></div>'
    if k == "warn":
        return f'<div class="blk warn"><h3>{e(b["head"])}</h3><p>{e(b["body"])}</p></div>'
    if k == "pair":
        cells = "".join(f'<div class="p"><h4>{e(t)}</h4><p>{e(d)}</p></div>' for t, d in b["items"])
        return (f'<div class="blk"><h3>{e(b["head"])}</h3>'
                f'<div class="pair">{cells}</div></div>')
    if k == "formula":
        rows = "".join(f'<div class="frow"><b>{e(n)}</b><span class="eq">= {e(f)}</span>'
                       f'<span class="hint">{e(h)}</span></div>' for n, f, h in b["rows"])
        return f'<div class="blk"><h3>{e(b["head"])}</h3><div class="formula">{rows}</div></div>'
    if k == "table":
        th = "".join(f"<th>{e(c)}</th>" for c in b["cols"])
        tr = "".join("<tr>" + "".join(f"<td><bdi>{e(c)}</bdi></td>" for c in r) + "</tr>" for r in b["rows"])
        return (f'<div class="blk"><h3>{e(b["head"])}</h3>'
                f'<table class="tbl"><thead><tr>{th}</tr></thead><tbody>{tr}</tbody></table></div>')
    if k == "math":
        rows = "".join(f'<div class="mrow"><b>{e(n)}</b>'
                       f'<span class="f">{_esc(f)}</span>'
                       f'<div class="ex">مثال: <span class="m">{_esc(x)}</span></div></div>'
                       for n, f, x in b["rows"])
        return f'<div class="blk"><h3>{e(b["head"])}</h3><div class="math">{rows}</div></div>'
    if k == "bullets":
        li = "".join(f"<li>{e(t)}</li>" for t in b["items"])
        return f'<div class="blk idea"><h3>{e(b["head"])}</h3><ul class="bul">{li}</ul></div>'
    if k == "steps":
        st = "".join(f'<div class="step"><span class="b">{i}</span><span>{e(t)}</span></div>'
                     for i, t in enumerate(b["items"], 1))
        return f'<div class="blk"><h3>{e(b["head"])}</h3><div class="steps">{st}</div></div>'
    if k == "compare":
        def box(x):
            return f'<div class="cbox"><h4>{e(x[0])}</h4><div class="tagline">{e(x[1])}</div><p>{e(x[2])}</p></div>'
        return (f'<div class="blk"><h3>{e(b["head"])}</h3>'
                f'<div class="compare">{box(b["left"])}{box(b["right"])}</div></div>')
    if k == "chain":
        parts = []
        for i, t in enumerate(b["items"]):
            if i:
                parts.append('<span class="ar">←</span>')
            parts.append(f'<span class="c">{e(t)}</span>')
        return f'<div class="blk idea"><h3>{e(b["head"])}</h3><div class="chain">{"".join(parts)}</div></div>'
    if k == "ways":
        return f'<div class="blk">{art}</div>'
    raise ValueError(k)


def build():
    p = ['<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8">',
         '<title>الطاقة الحرارية — ملخّص مبسّط</title>', FONTS, f'<style>{CSS}</style>',
         '</head><body>']

    # ---------- الغلاف ----------
    p.append('<section class="cover">')
    p.append('<div class="hero"><div class="top">علوم · الوحدة الأولى · الصفحات 6 – 39</div>'
             '<h1>الطاقة الحرارية</h1>'
             '<p>ملخّص الوحدة بلغة سهلة — كل ما تحتاج أن تعرفه عن الحرارة، '
             'وكيف تنتقل، وكيف نستفيد منها.</p></div>')
    p.append('<div class="covergrid">')
    for i, L in enumerate(LESSONS, 1):
        p.append(f'<div class="ccard {L["key"]}"><div class="n">{e(L["code"])}</div>'
                 f'<h3>{e(L["title"])}</h3><p>{e(L["big"])}</p></div>')
    p.append('</div>')
    p.append(f'<div class="bigidea"><h3>الفكرة الرئيسة للوحدة</h3><p>{e(BIGIDEA)}</p></div>')
    p.append('<div class="note">في نهاية الملخّص صفحة مراجعة سريعة فيها كل المصطلحات '
             'المهمّة وتعريفها في سطر واحد.</div>')
    p.append('</section>')

    # ---------- الدروس ----------
    for L in LESSONS:
        p.append(f'<section class="lesson {L["key"]}">')
        p.append('<div class="band"><div class="row">'
                 f'<span class="num">{e(L["code"])}</span><h2>{e(L["title"])}</h2></div>'
                 f'<p class="big">{e(L["big"])}</p></div>')
        for b in L["blocks"]:
            p.append(render_block(b))
        p.append('</section>')

    # ---------- المراجعة ----------
    p.append('<section class="recap"><div class="head"><h2>مراجعة سريعة</h2>'
             '<p>كل مصطلح في الوحدة، وتعريفه في سطر واحد.</p></div><div class="rgrid">')
    for term, dfn in RECAP:
        p.append(f'<div class="ritem"><b>{e(term)}</b><span>{e(dfn)}</span></div>')
    p.append('</div>')
    p.append('<footer class="note">أُعِدَّ هذا الملخّص من محتوى الوحدة 1 «الطاقة الحرارية»: '
             'الدرس 1.1 الطاقة الحرارية ودرجة الحرارة والحرارة، والدرس 1.2 انتقال الطاقة الحرارية، '
             'والدرس 1.3 استخدام الطاقة الحرارية.</footer>')
    p.append('</section></body></html>')
    return "\n".join(p)


if __name__ == "__main__":
    open(OUT + "chap1-summary.html", "w", encoding="utf-8").write(build())
    n = sum(len(L["blocks"]) for L in LESSONS)
    print(f"تم إنشاء: chap1-summary.html — {len(LESSONS)} دروس، {n} كتلة، {len(RECAP)} مصطلحًا")
