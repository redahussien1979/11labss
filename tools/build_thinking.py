# -*- coding: utf-8 -*-
"""يبني كرّاسة أسئلة التفكير الناقد (A4) مع الإجابة وتفسيرها."""
import html, re
from thinking_content import SECTIONS, INTRO

_esc = html.escape
# البديل الأول: ما بين {{ }} يُعزَل كاملًا (للوحدات المركّبة مثل J/g·°C).
# البديل الثاني: الأرقام مع الوحدات اللاتينية البسيطة (0 K، 95°F، 20%).
_ISO = re.compile(r'\{\{(.+?)\}\}|(\d+(?:\.\d+)?\s?°?(?:[A-Za-z]+|%)|°[A-Za-z])')


def e(x):
    """تهريب HTML مع عزل كل ما هو لاتيني اتجاهيًّا كي لا ينقلب داخل نص عربي."""
    def rep(m):
        return '<bdi dir="ltr">' + (m.group(1) or m.group(2)) + '</bdi>'
    return _ISO.sub(rep, _esc(x))


OUT = "/home/user/11labss/"
FONTS = ('<link rel="preconnect" href="https://fonts.googleapis.com">\n'
         '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>\n'
         '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?'
         'family=Reem+Kufi:wght@400;500;600;700&family=Tajawal:wght@300;400;500;700;800'
         '&family=IBM+Plex+Mono:wght@400;500;600&display=swap">')

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
  --ok:#14713C; --ok-tint:#E6F4EB; --ok-line:#8FC7A6;
  --accent:var(--c1); --accent-tint:var(--c1-tint); --accent-deep:var(--c1-deep);
}
body{background:#fff; color:var(--ink);
  font-family:"Tajawal","Segoe UI",Tahoma,sans-serif;
  font-size:11pt; line-height:1.78; direction:rtl;
  -webkit-print-color-adjust:exact; print-color-adjust:exact}
h1,h2,h3,h4{font-family:"Reem Kufi","Tajawal",sans-serif; font-weight:600; margin:0}
.k1{--accent:var(--c1); --accent-tint:var(--c1-tint); --accent-deep:var(--c1-deep)}
.k2{--accent:var(--c2); --accent-tint:var(--c2-tint); --accent-deep:var(--c2-deep)}
.k3{--accent:var(--c3); --accent-tint:var(--c3-tint); --accent-deep:var(--c3-deep)}

.cover{break-after:page; page-break-after:always}
.hero{background:linear-gradient(115deg,var(--c3) 0%,var(--c2) 34%,var(--c1-deep) 72%,var(--c1) 100%);
  color:#fff; border-radius:14pt; padding:24pt 24pt 22pt; position:relative; overflow:hidden}
.hero::after{content:""; position:absolute; inset:auto auto -80pt -40pt; width:210pt; height:210pt;
  border-radius:50%; border:16pt solid rgba(255,255,255,.10)}
.hero .top{font-size:9pt; opacity:.92; margin-bottom:12pt}
.hero h1{font-size:32pt; line-height:1.15; font-weight:700; margin-bottom:6pt}
.hero p{margin:0; font-size:12pt; opacity:.96; max-width:32em; line-height:1.6}
.hero .stats{display:flex; gap:7pt; margin-top:14pt; flex-wrap:wrap; position:relative; z-index:1}
.hero .chip{background:rgba(255,255,255,.18); border:.7pt solid rgba(255,255,255,.42);
  border-radius:999pt; padding:3.5pt 11pt; font-size:9pt; font-weight:500}
.howto{margin-top:16pt; border:.8pt solid var(--line); border-radius:10pt; padding:12pt 15pt}
.howto h3{font-size:12pt; margin-bottom:5pt; color:var(--c1-deep)}
.howto p{margin:0; font-size:10.5pt; color:var(--ink-2); line-height:1.8}
.covergrid{display:grid; grid-template-columns:1fr 1fr 1fr; gap:9pt; margin-top:14pt}
.ccard{border:.8pt solid var(--line); border-top:3.4pt solid var(--accent); border-radius:10pt;
  padding:10pt 11pt 11pt; background:var(--tint)}
.ccard .n{font-family:"IBM Plex Mono",monospace; font-size:15pt; font-weight:600; color:var(--accent)}
.ccard h3{font-size:11pt; margin:4pt 0 3pt; line-height:1.35}
.ccard span{font-size:9pt; color:var(--ink-2)}
.cover .note{margin-top:14pt; font-size:9pt; color:var(--ink-3); line-height:1.7}

section.part{break-before:page; page-break-before:always}
.band{background:var(--accent-tint); border-radius:11pt; padding:12pt 15pt;
  border-inline-start:5pt solid var(--accent); margin-bottom:11pt;
  break-after:avoid; page-break-after:avoid}
.band .row{display:flex; align-items:baseline; gap:10pt}
.band .num{font-family:"IBM Plex Mono",monospace; font-size:16pt; font-weight:600; color:var(--accent-deep)}
.band h2{font-size:15pt; color:var(--accent-deep)}
.band .count{margin-inline-start:auto; background:var(--accent); color:#fff; border-radius:999pt;
  padding:2pt 9pt; font-size:8.4pt; font-weight:700; white-space:nowrap}

.item{break-inside:avoid; page-break-inside:avoid; margin-bottom:9pt;
  border:.8pt solid var(--line); border-radius:11pt; padding:10pt 12pt 11pt; background:#fff}
.qline{display:grid; grid-template-columns:20pt 1fr; gap:8pt; align-items:start}
.qn{width:20pt; height:20pt; border-radius:50%; background:var(--accent); color:#fff;
  display:flex; align-items:center; justify-content:center; font-family:"IBM Plex Mono",monospace;
  font-size:8.6pt; font-weight:600; margin-top:1pt}
.qtext{margin:0; font-weight:700; line-height:1.6; font-size:11.2pt}
.ans{margin:8pt 0 0 0; background:var(--ok-tint); border:.8pt solid var(--ok-line);
  border-radius:8pt; padding:7pt 11pt; font-size:10.8pt; line-height:1.65}
.ans b{color:var(--ok); font-weight:700}
.why{margin:6pt 0 0; background:var(--accent-tint); border-radius:8pt;
  border-inline-start:2.6pt solid var(--accent); padding:7pt 11pt;
  font-size:10.2pt; color:var(--ink-2); line-height:1.72}
.why b{color:var(--accent-deep); font-weight:700}
.extra{margin:6pt 0 0; border:.7pt dashed var(--ink-3); border-radius:8pt; padding:6pt 11pt;
  font-size:9.4pt; color:var(--ink-3); line-height:1.65}
.extra b{color:var(--ink-2)}
footer.note{margin-top:14pt; padding-top:8pt; border-top:.8pt solid var(--line);
  font-size:9pt; color:var(--ink-3); line-height:1.7}
"""


def build():
    total = sum(len(qs) for _, _, _, qs in SECTIONS)
    p = ['<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8">',
         '<title>الطاقة الحرارية — أسئلة تفكير ناقد</title>', FONTS,
         f'<style>{CSS}</style>', '</head><body>']

    p.append('<section class="cover">')
    p.append('<div class="hero"><div class="top">علوم · الوحدة الأولى: الطاقة الحرارية · الصفحات 6 – 39</div>'
             '<h1>أسئلة تفكير ناقد</h1>'
             '<p>سؤال ثم إجابته ثم سببها — بلغة سهلة، وكلها من داخل الوحدة.</p>'
             f'<div class="stats"><span class="chip">{total} سؤالًا</span>'
             '<span class="chip">3 أقسام</span>'
             '<span class="chip">إجابة + تفسير لكل سؤال</span></div></div>')
    p.append(f'<div class="howto"><h3>كيف تستفيد من هذه الورقة</h3><p>{e(INTRO)}</p></div>')
    p.append('<div class="covergrid">')
    for num, key, title, qs in SECTIONS:
        p.append(f'<div class="ccard {key}"><div class="n">{e(num)}</div><h3>{e(title)}</h3>'
                 f'<span>{len(qs)} سؤالًا</span></div>')
    p.append('</div>')
    p.append('<div class="note">كل الإجابات مبنية على مبادئ الوحدة نفسها. وحيثما استُعين بمعلومة '
             'من خارج الكتاب، وُضِعت في إطار منفصل ومُيِّزت بوضوح.</div>')
    p.append('</section>')

    n = 0
    for num, key, title, qs in SECTIONS:
        p.append(f'<section class="part {key}">')
        p.append('<div class="band"><div class="row">'
                 f'<span class="num">{e(num)}</span><h2>{e(title)}</h2>'
                 f'<span class="count">{len(qs)} سؤالًا</span></div></div>')
        for it in qs:
            n += 1
            p.append('<article class="item">')
            p.append(f'<div class="qline"><span class="qn">{n}</span>'
                     f'<p class="qtext">{e(it["q"])}</p></div>')
            p.append(f'<div class="ans"><b>الإجابة:</b> {e(it["a"])}</div>')
            p.append(f'<div class="why"><b>لماذا؟</b> {e(it["w"])}</div>')
            if it.get("note"):
                p.append(f'<div class="extra"><b>ملاحظة:</b> {e(it["note"])}</div>')
            p.append('</article>')
        p.append('</section>')

    p.append('<footer class="note">أُعدَّت هذه الأسئلة من محتوى الوحدة 1 «الطاقة الحرارية»: '
             'الدرس 1.1 الطاقة الحرارية ودرجة الحرارة والحرارة، والدرس 1.2 انتقال الطاقة الحرارية، '
             'والدرس 1.3 استخدام الطاقة الحرارية.</footer>')
    p.append('</body></html>')
    return "\n".join(p), total, n


if __name__ == "__main__":
    doc, total, n = build()
    assert total == n, "اختلّ ترقيم الأسئلة"
    open(OUT + "chap1-thinking.html", "w", encoding="utf-8").write(doc)
    print(f"تم إنشاء: chap1-thinking.html — {total} سؤالًا في {len(SECTIONS)} أقسام")
