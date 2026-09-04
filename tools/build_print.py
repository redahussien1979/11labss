# -*- coding: utf-8 -*-
"""يبني نسختين للطباعة (A4) من بنك الأسئلة نفسه:
  chap1-mcq-print.html     ← النسخة المحلولة: ملوَّنة، بغلاف وخطوط عربية أنيقة
  chap1-mcq-practice.html  ← ورقة الطالب: أبيض وأسود، موفِّرة للحبر
تُحوَّل بعد ذلك إلى PDF بواسطة Chromium."""
import html
from bank import SECTIONS, TOTAL, LETTERS, summary

e = html.escape
OUT = "/home/user/11labss/"

FONTS = ('<link rel="preconnect" href="https://fonts.googleapis.com">\n'
         '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>\n'
         '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?'
         'family=Reem+Kufi:wght@400;500;600;700'
         '&family=Readex+Pro:wght@400;500;600'
         '&family=Tajawal:wght@300;400;500;700;800'
         '&family=IBM+Plex+Sans+Arabic:wght@400;500;600;700'
         '&family=IBM+Plex+Mono:wght@400;500;600'
         '&display=swap">')

# ============================ النسخة الملوَّنة ============================
CSS_FANCY = """
@page{ size:A4 portrait; }
*{box-sizing:border-box}
html,body{margin:0; padding:0}
:root{
  --ink:#1A2430; --ink-2:#41505F; --ink-3:#6E7F8E;
  --line:#DCE4EB; --tint:#F6F9FB;
  --c1:#0E6E88; --c1-tint:#E4F1F6; --c1-deep:#08556A;
  --c2:#A8620C; --c2-tint:#FBF0E0; --c2-deep:#874E08;
  --c3:#B0301F; --c3-tint:#FBEAE7; --c3-deep:#8C2517;
  --ok:#14713C; --ok-tint:#E6F4EB; --ok-line:#8FC7A6;
  --accent:var(--c1); --accent-tint:var(--c1-tint); --accent-deep:var(--c1-deep);
}
body{
  background:#fff; color:var(--ink);
  font-family:"Tajawal","IBM Plex Sans Arabic","Segoe UI",Tahoma,sans-serif;
  font-size:10pt; line-height:1.68; direction:rtl;
  -webkit-print-color-adjust:exact; print-color-adjust:exact;
}
h1,h2,h3,.display{font-family:"Reem Kufi","Tajawal",sans-serif; font-weight:600}
.mono{font-family:"IBM Plex Mono",monospace}

/* ---------------- الغلاف ---------------- */
.cover{break-after:page; page-break-after:always}
.hero{
  background:linear-gradient(115deg,var(--c3) 0%,var(--c2) 34%,var(--c1-deep) 72%,var(--c1) 100%);
  color:#fff; border-radius:12pt; padding:20pt 22pt 18pt; position:relative; overflow:hidden;
}
.hero::after{
  content:""; position:absolute; inset:auto auto -70pt -30pt; width:190pt; height:190pt;
  border-radius:50%; border:14pt solid rgba(255,255,255,.10);
}
.hero .top{display:flex; justify-content:space-between; align-items:baseline; gap:10pt;
  font-size:8.4pt; opacity:.92; margin-bottom:12pt}
.hero .top .latin{font-family:"IBM Plex Mono",monospace; font-size:7.4pt; letter-spacing:.14em;
  text-transform:uppercase; direction:ltr; unicode-bidi:isolate}
.hero h1{font-size:30pt; line-height:1.15; margin:0 0 5pt; font-weight:700; letter-spacing:0}
.hero .sub{margin:0; font-size:11pt; font-weight:400; opacity:.95; max-width:30em}
.hero .stats{display:flex; gap:7pt; margin-top:14pt; flex-wrap:wrap; position:relative; z-index:1}
.hero .chip{background:rgba(255,255,255,.18); border:.7pt solid rgba(255,255,255,.42);
  border-radius:999pt; padding:3.5pt 11pt; font-size:9pt; font-weight:500}
.hero .chip b{font-weight:700; font-variant-numeric:tabular-nums}

.cover h2.section{font-size:12.5pt; margin:14pt 0 7pt; color:var(--ink);
  display:flex; align-items:center; gap:8pt}
.cover h2.section::after{content:""; flex:1; height:.8pt; background:var(--line)}
.cards{display:grid; grid-template-columns:1fr 1fr 1fr; gap:9pt}
.card{border:.8pt solid var(--line); border-radius:9pt; padding:9pt 9pt 10pt;
  border-top:3.2pt solid var(--accent); background:var(--tint)}
.card.k1{--accent:var(--c1); --accent-tint:var(--c1-tint)}
.card.k2{--accent:var(--c2); --accent-tint:var(--c2-tint)}
.card.k3{--accent:var(--c3); --accent-tint:var(--c3-tint)}
.card .num{font-family:"IBM Plex Mono",monospace; font-size:15pt; font-weight:600;
  color:var(--accent); line-height:1}
.card h3{font-size:11pt; margin:5pt 0 5pt; line-height:1.35; color:var(--ink)}
.card p{margin:0; font-size:8.2pt; color:var(--ink-2); line-height:1.62}
.card .n{display:inline-block; margin-top:8pt; background:var(--accent-tint); color:var(--accent);
  border-radius:999pt; padding:1.5pt 8pt; font-size:8pt; font-weight:700;
  font-variant-numeric:tabular-nums}
.howto{margin-top:12pt; border:.8pt solid var(--line); border-radius:9pt; padding:9pt 12pt 10pt;
  background:#fff}
.howto h3{font-size:10.5pt; margin:0 0 6pt}
.howto ul{margin:0; padding-inline-start:14pt; font-size:8.8pt; color:var(--ink-2); line-height:1.7}
.howto li::marker{color:var(--c2)}
.legend{display:flex; gap:13pt; flex-wrap:wrap; margin-top:8pt; font-size:8.6pt; color:var(--ink-2);
  align-items:center}
.legend .sw{display:inline-grid; grid-template-columns:auto auto; gap:5pt; align-items:center}
.legend .pill{background:var(--ok-tint); border:.8pt solid var(--ok-line); border-radius:5pt;
  padding:1pt 7pt; color:var(--ok); font-weight:700; font-size:8.4pt}
.gloss{display:grid; grid-template-columns:repeat(4,1fr); gap:5pt}
.term{border:.7pt solid var(--line); border-top:2.2pt solid var(--accent); border-radius:6pt;
  padding:4.5pt 7pt 5pt; background:#fff}
.term.k1{--accent:var(--c1)} .term.k2{--accent:var(--c2)} .term.k3{--accent:var(--c3)}
.term b{display:block; font-size:8.8pt; font-weight:700; color:var(--ink); line-height:1.35}
.term span{display:block; font-family:"IBM Plex Mono",monospace; font-size:6.6pt;
  color:var(--ink-3); unicode-bidi:isolate; margin-top:2pt}
.cover .credit{margin-top:10pt; font-size:8pt; color:var(--ink-3); line-height:1.7}

/* ---------------- عناوين الدروس ---------------- */
section.lesson{break-before:page; page-break-before:always}
#l1{--accent:var(--c1); --accent-tint:var(--c1-tint); --accent-deep:var(--c1-deep)}
#l2{--accent:var(--c2); --accent-tint:var(--c2-tint); --accent-deep:var(--c2-deep)}
#l3{--accent:var(--c3); --accent-tint:var(--c3-tint); --accent-deep:var(--c3-deep)}
.lesson-band{background:var(--accent-tint); border-radius:9pt; padding:10pt 12pt;
  border-inline-start:4pt solid var(--accent); margin-bottom:10pt;
  break-after:avoid; page-break-after:avoid}
.lesson-band .row{display:flex; align-items:baseline; gap:9pt}
.lesson-band .num{font-family:"IBM Plex Mono",monospace; font-size:15pt; font-weight:600;
  color:var(--accent-deep)}
.lesson-band h2{font-size:14pt; margin:0; color:var(--accent-deep)}
.lesson-band .count{margin-inline-start:auto; background:var(--accent); color:#fff;
  border-radius:999pt; padding:2pt 9pt; font-size:8.2pt; font-weight:700;
  font-variant-numeric:tabular-nums; white-space:nowrap}
.lesson-band p{margin:5pt 0 0; font-size:8.4pt; color:var(--ink-2); line-height:1.65}

/* ---------------- الأسئلة ---------------- */
.q{display:grid; grid-template-columns:19pt 1fr; gap:0 7pt; align-items:start;
  padding:7pt 8pt 7pt 8pt; margin-bottom:5pt;
  border:.7pt solid var(--line); border-radius:8pt; background:#fff;
  break-inside:avoid; page-break-inside:avoid}
.qn{display:flex; align-items:center; justify-content:center;
  width:19pt; height:19pt; border-radius:50%; background:var(--accent); color:#fff;
  font-family:"IBM Plex Mono",monospace; font-variant-numeric:tabular-nums;
  font-size:8.4pt; font-weight:600; margin-top:1pt}
.qtext{margin:0 0 5pt; font-weight:700; line-height:1.55; color:var(--ink)}
.opts{display:grid; grid-template-columns:1fr 1fr; gap:3pt 9pt; margin:0; padding:0; list-style:none}
.opt{display:grid; grid-template-columns:13pt 1fr 10pt; gap:5pt; align-items:start;
  padding:2.4pt 5pt; border:.7pt solid transparent; border-radius:6pt; line-height:1.5}
.opt .ltr{font-family:"IBM Plex Mono",monospace; font-size:7.6pt; font-weight:600;
  color:var(--ink-3); background:var(--tint); border:.6pt solid var(--line);
  border-radius:3.5pt; text-align:center; line-height:1.65}
.opt .mk{text-align:center; font-weight:700; color:var(--ok)}
.opt.right{background:var(--ok-tint); border-color:var(--ok-line)}
.opt.right .ltr{background:var(--ok); color:#fff; border-color:var(--ok)}
.opt.right .txt{font-weight:700; color:#0E3D22}
.opt.right .mk::before{content:"✓"}
.why{margin:6pt 0 0; padding:5pt 8pt; background:var(--accent-tint);
  border-radius:6pt; border-inline-start:2.6pt solid var(--accent);
  font-size:8.6pt; color:var(--ink-2); line-height:1.6}
.why .lab{color:var(--accent-deep); font-weight:700}
.why .tag{font-family:"IBM Plex Mono",monospace; font-weight:600; background:var(--accent);
  color:#fff; border-radius:3pt; padding:0 4.5pt; margin-inline-start:4pt}

/* ---------------- مفتاح الإجابات ---------------- */
.key{break-before:page; page-break-before:always}
.key .head{background:linear-gradient(115deg,var(--c1),var(--c2) 58%,var(--c3));
  color:#fff; border-radius:9pt; padding:11pt 14pt; margin-bottom:11pt}
.key .head h2{font-size:15pt; margin:0}
.key .head p{margin:2pt 0 0; font-size:8.8pt; opacity:.94}
.keygrid{display:grid; grid-template-columns:repeat(10,1fr); gap:3pt}
.keycell{display:flex; justify-content:space-between; align-items:center; gap:3pt;
  border:.7pt solid var(--line); border-radius:5pt; padding:2.4pt 4pt; background:#fff;
  font-variant-numeric:tabular-nums; font-size:8.2pt}
.keycell i{font-style:normal; color:var(--ink-3); font-size:7.4pt;
  font-family:"IBM Plex Mono",monospace}
.keycell b{font-family:"IBM Plex Mono",monospace; font-weight:600; font-size:8pt;
  color:#fff; background:var(--ok); border-radius:3pt; padding:0 4.5pt}
footer.note{margin-top:14pt; padding-top:7pt; border-top:.7pt solid var(--line);
  font-size:8pt; color:var(--ink-3); line-height:1.65}
"""

# ============================ ورقة الطالب (أبيض وأسود) ============================
CSS_PLAIN = """
@page{ size:A4 portrait; }
*{box-sizing:border-box}
html,body{margin:0; padding:0}
body{
  background:#fff; color:#000;
  font-family:"IBM Plex Sans Arabic","Noto Naskh Arabic","Segoe UI",Tahoma,sans-serif;
  font-size:9.9pt; line-height:1.62; direction:rtl;
  -webkit-print-color-adjust:exact; print-color-adjust:exact;
}
.masthead{border-bottom:1.6pt solid #000; padding-bottom:7pt; margin-bottom:12pt}
.kicker{display:flex; justify-content:space-between; align-items:baseline; gap:10pt;
  font-size:8pt; color:#4a4a4a; margin-bottom:5pt}
.kicker .latin{font-family:"IBM Plex Mono",monospace; font-size:7.2pt; letter-spacing:.12em;
  text-transform:uppercase; direction:ltr; unicode-bidi:isolate}
h1{font-family:"Readex Pro","IBM Plex Sans Arabic",sans-serif; font-weight:600;
  font-size:19pt; line-height:1.28; margin:0 0 4pt; text-wrap:balance}
.sub{margin:0; font-size:9.4pt; color:#333; max-width:52em}
.meta{display:flex; flex-wrap:wrap; gap:0 16pt; margin:8pt 0 0; padding:0; list-style:none;
  font-size:8.6pt; color:#333}
.meta b{font-weight:600; font-variant-numeric:tabular-nums}
.namebar{display:flex; gap:14pt; margin-top:9pt; font-size:9.2pt}
.namebar span{flex:1; border-bottom:.7pt solid #000; padding-bottom:2pt}
section.lesson{break-before:page; page-break-before:always}
section.lesson:first-of-type{break-before:auto; page-break-before:auto}
.lesson-head{display:flex; align-items:baseline; gap:8pt; border-bottom:1.2pt solid #000;
  padding-bottom:4pt; margin-bottom:3pt; break-after:avoid; page-break-after:avoid}
.lesson-num{font-family:"IBM Plex Mono",monospace; font-size:13pt; font-weight:600;
  font-variant-numeric:tabular-nums}
.lesson-head h2{font-family:"Readex Pro",sans-serif; font-weight:600; font-size:13pt; margin:0}
.lesson-count{margin-inline-start:auto; font-size:8.4pt; color:#444;
  font-variant-numeric:tabular-nums; white-space:nowrap}
.lesson-topics{font-size:8.4pt; color:#444; margin:0 0 8pt; line-height:1.65;
  break-after:avoid; page-break-after:avoid}
.q{display:grid; grid-template-columns:16pt 1fr; gap:0 6pt; align-items:start;
  padding:6pt 0; border-bottom:.4pt solid #c9c9c9;
  break-inside:avoid; page-break-inside:avoid}
.q:last-of-type{border-bottom:0}
.qn{font-family:"IBM Plex Mono",monospace; font-variant-numeric:tabular-nums;
  font-size:9pt; font-weight:600; text-align:start; padding-top:1pt}
.qtext{margin:0 0 4pt; font-weight:600; line-height:1.55}
.opts{display:grid; grid-template-columns:1fr 1fr; gap:2.5pt 10pt; margin:0; padding:0; list-style:none}
.opt{display:grid; grid-template-columns:12pt 1fr 9pt; gap:4pt; align-items:start;
  padding:1.6pt 3pt; border:.8pt solid transparent; border-radius:3pt; line-height:1.5}
.opt .ltr{font-family:"IBM Plex Mono",monospace; font-size:7.6pt; font-weight:600;
  border:.6pt solid #666; border-radius:2pt; text-align:center; line-height:1.6}
.opt .mk{font-weight:700; text-align:center}
.key{break-before:page; page-break-before:always}
.key h2{font-family:"Readex Pro",sans-serif; font-size:13pt; margin:0 0 2pt;
  border-bottom:1.2pt solid #000; padding-bottom:4pt}
.key p{font-size:8.6pt; color:#444; margin:5pt 0 8pt}
.keygrid{display:grid; grid-template-columns:repeat(10,1fr); gap:2.5pt}
.keycell{display:flex; justify-content:space-between; align-items:baseline; gap:3pt;
  border:.5pt solid #888; border-radius:2.5pt; padding:2pt 4pt;
  font-family:"IBM Plex Mono",monospace; font-variant-numeric:tabular-nums; font-size:8.2pt}
.keycell i{font-style:normal; color:#666; font-size:7.4pt}
.keycell.blank{height:16pt}
footer.note{margin-top:12pt; padding-top:6pt; border-top:.5pt solid #999;
  font-size:8pt; color:#555; line-height:1.6}
"""

CREDIT = ('أُعدَّت هذه الورقة من محتوى الوحدة 1 «الطاقة الحرارية»: الدرس 1.1 الطاقة الحرارية '
          'ودرجة الحرارة والحرارة، والدرس 1.2 انتقال الطاقة الحرارية، والدرس 1.3 استخدام الطاقة الحرارية.')


def build(solved: bool) -> str:
    css = CSS_FANCY if solved else CSS_PLAIN
    title = "الطاقة الحرارية — أسئلة محلولة" if solved else "الطاقة الحرارية — ورقة أسئلة"
    p = ['<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8">',
         f'<title>{title}</title>', FONTS, f'<style>{css}</style>', '</head><body>']

    if solved:
        # ---------- الغلاف ----------
        p.append('<section class="cover">')
        p.append('<div class="hero">'
                 '<div class="top"><span>علوم · الوحدة الأولى · الصفحات 6 – 39</span>'
                 '<span class="latin">McGraw-Hill Education</span></div>'
                 '<h1>الطاقة الحرارية</h1>'
                 '<p class="sub">بنك أسئلة اختيار من متعدد — محلولة مع تفسير كل إجابة</p>'
                 '<div class="stats">'
                 f'<span class="chip"><b>{TOTAL}</b> سؤالًا</span>'
                 '<span class="chip"><b>3</b> دروس</span>'
                 '<span class="chip">أربعة خيارات لكل سؤال</span>'
                 '<span class="chip">مفتاح إجابات كامل</span>'
                 '</div></div>')
        p.append('<h2 class="section">محتويات الوحدة</h2><div class="cards">')
        for i, (code, ttl, topics, qs) in enumerate(SECTIONS, 1):
            p.append(f'<div class="card k{i}"><div class="num">{e(code)}</div>'
                     f'<h3>{e(ttl)}</h3><p>{e(topics)}</p>'
                     f'<span class="n">{len(qs)} سؤالًا</span></div>')
        p.append('</div>')
        p.append('<div class="howto"><h3>كيف تُقرأ هذه الورقة</h3><ul>'
                 '<li>الإجابة الصحيحة مظلَّلة باللون الأخضر وأمامها علامة ✓، وحرفها داخل مربّع أخضر.</li>'
                 '<li>أسفل كل سؤال صندوق ملوَّن بلون الدرس يشرح سبب صحّة الإجابة.</li>'
                 '<li>لكل درس لون خاص به يتدرّج من البارد إلى الساخن مع تسلسل الوحدة.</li>'
                 '<li>مفتاح الإجابات كاملًا في الصفحة الأخيرة، للتصحيح السريع.</li>'
                 '</ul>'
                 '<div class="legend">'
                 '<span class="sw"><span class="pill">B ✓</span><span>الإجابة الصحيحة</span></span>'
                 '<span class="sw"><span class="pill" style="background:var(--c1-tint);'
                 'border-color:var(--c1);color:var(--c1)">1.1</span><span>الطاقة ودرجة الحرارة</span></span>'
                 '<span class="sw"><span class="pill" style="background:var(--c2-tint);'
                 'border-color:var(--c2);color:var(--c2)">1.2</span><span>انتقال الطاقة</span></span>'
                 '<span class="sw"><span class="pill" style="background:var(--c3-tint);'
                 'border-color:var(--c3);color:var(--c3)">1.3</span><span>استخدام الطاقة</span></span>'
                 '</div></div>')
        p.append('<h2 class="section">مفردات الوحدة</h2><div class="gloss">'
                 '<div class="term k1"><b>الطاقة الحرارية</b><span>thermal energy</span></div><div class="term k1"><b>درجة الحرارة</b><span>temperature</span></div><div class="term k1"><b>الحرارة</b><span>heat</span></div><div class="term k2"><b>الإشعاع</b><span>radiation</span></div><div class="term k2"><b>التوصيل</b><span>conduction</span></div><div class="term k2"><b>موصّل للحرارة</b><span>thermal conductor</span></div><div class="term k2"><b>عازل للحرارة</b><span>thermal insulator</span></div><div class="term k2"><b>الحرارة النوعية</b><span>specific heat</span></div><div class="term k2"><b>التمدُّد الحراري</b><span>thermal expansion</span></div><div class="term k2"><b>الانكماش الحراري</b><span>thermal contraction</span></div><div class="term k2"><b>الحمل الحراري</b><span>convection</span></div><div class="term k2"><b>تيارات الحمل</b><span>convection current</span></div><div class="term k3"><b>جهاز تسخين</b><span>heating appliance</span></div><div class="term k3"><b>منظِّم الحرارة</b><span>thermostat</span></div><div class="term k3"><b>ثلاجة</b><span>refrigerator</span></div><div class="term k3"><b>محرك حراري</b><span>heat engine</span></div>'
                 '</div>')
        p.append(f'<p class="credit">{e(CREDIT)}</p>')
        p.append('</section>')
    else:
        p.append('<header class="masthead">'
                 '<div class="kicker"><span>الوحدة 1 · علوم · الصفحات 6 – 39</span>'
                 '<span class="latin">McGraw-Hill Education</span></div>'
                 '<h1>الطاقة الحرارية — ورقة أسئلة</h1>'
                 '<p class="sub">اختبار اختيار من متعدد يغطي دروس الوحدة الثلاثة كاملةً. '
                 'اختر إجابةً واحدة لكل سؤال، ودوِّن حرفها في ورقة الإجابة الموجودة في الصفحة الأخيرة.</p>'
                 '<ul class="meta">'
                 f'<li>عدد الأسئلة: <b>{TOTAL}</b></li><li>عدد الدروس: <b>3</b></li>'
                 f'<li>الخيارات: <b>A · B · C · D</b></li><li>الدرجة: <b>… / {TOTAL}</b></li></ul>'
                 '<div class="namebar"><span>الاسم:</span><span>التاريخ:</span><span>الصف:</span></div>'
                 '</header>')

    n, key = 0, []
    for i, (code, ttl, topics, qs) in enumerate(SECTIONS, 1):
        p.append(f'<section class="lesson" id="l{i}">')
        if solved:
            p.append('<div class="lesson-band"><div class="row">'
                     f'<span class="num">{e(code)}</span><h2>{e(ttl)}</h2>'
                     f'<span class="count">{len(qs)} سؤالًا</span></div>'
                     f'<p>{e(topics)}</p></div>')
        else:
            p.append('<div class="lesson-head">'
                     f'<span class="lesson-num">{e(code)}</span><h2>{e(ttl)}</h2>'
                     f'<span class="lesson-count">{len(qs)} سؤالًا</span></div>'
                     f'<p class="lesson-topics">{e(topics)}</p>')
        for q, opts, ci, exp in qs:
            n += 1
            key.append((n, LETTERS[ci]))
            p.append(f'<article class="q"><div class="qn">{n}</div><div>')
            p.append(f'<p class="qtext">{e(q)}</p><ul class="opts">')
            for j, o in enumerate(opts):
                cls = "opt right" if (solved and j == ci) else "opt"
                p.append(f'<li class="{cls}"><span class="ltr">{LETTERS[j]}</span>'
                         f'<span class="txt">{e(o)}</span><span class="mk"></span></li>')
            p.append('</ul>')
            if solved:
                p.append('<div class="why"><span class="lab">الإجابة الصحيحة'
                         f'<span class="tag">{LETTERS[ci]}</span></span> — {e(exp)}</div>')
            p.append('</div></article>')
        p.append('</section>')

    p.append('<section class="key">')
    if solved:
        p.append('<div class="head"><h2>مفتاح الإجابات</h2>'
                 f'<p>الأسئلة من 1 إلى {TOTAL} بالترتيب.</p></div><div class="keygrid">')
        for num, ltr in key:
            p.append(f'<div class="keycell"><i>{num}</i><b>{ltr}</b></div>')
    else:
        p.append('<h2>ورقة الإجابة</h2>'
                 '<p>اكتب حرف الإجابة (A أو B أو C أو D) أمام رقم كل سؤال.</p><div class="keygrid">')
        for num, _ in key:
            p.append(f'<div class="keycell blank"><i>{num}</i><b></b></div>')
    p.append('</div>')
    p.append(f'<footer class="note">{e(CREDIT)}</footer>')
    p.append('</section></body></html>')
    return "\n".join(p)


if __name__ == "__main__":
    summary()
    for solved, name in ((True, "chap1-mcq-print.html"), (False, "chap1-mcq-practice.html")):
        open(OUT + name, "w", encoding="utf-8").write(build(solved))
        print("تم إنشاء:", name)
