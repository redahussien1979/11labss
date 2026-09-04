# -*- coding: utf-8 -*-
"""يبني نسختين صالحتين للطباعة (A4، أبيض وأسود) من بنك الأسئلة نفسه:
  chap1-mcq-print.html     ← النسخة المحلولة (إجابات + تفسير + مفتاح)
  chap1-mcq-practice.html  ← ورقة الطالب (بدون إجابات + ورقة إجابة فارغة)
تُحوَّل بعد ذلك إلى PDF بواسطة Chromium."""
import html, sys
from bank import SECTIONS, TOTAL, LETTERS, summary

e = html.escape
OUT = "/home/user/11labss/"

CSS = """
@page{ size:A4 portrait; }
*{box-sizing:border-box}
html,body{margin:0; padding:0}
body{
  background:#fff; color:#000;
  font-family:"IBM Plex Sans Arabic","Noto Naskh Arabic","Segoe UI",Tahoma,sans-serif;
  font-size:9.9pt; line-height:1.62; direction:rtl;
  -webkit-print-color-adjust:exact; print-color-adjust:exact;
}
.sheet{padding:0}

/* ---------- ترويسة الصفحة الأولى ---------- */
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

/* ---------- الدروس ---------- */
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

/* ---------- الأسئلة ---------- */
.q{display:grid; grid-template-columns:16pt 1fr; gap:0 6pt; align-items:start;
  padding:6pt 0; border-bottom:.4pt solid #c9c9c9;
  break-inside:avoid; page-break-inside:avoid}
.q:last-of-type{border-bottom:0}
.qn{font-family:"IBM Plex Mono",monospace; font-variant-numeric:tabular-nums;
  font-size:9pt; font-weight:600; text-align:start; padding-top:1pt}
.qtext{margin:0 0 4pt; font-weight:600; line-height:1.55}
.opts{display:grid; grid-template-columns:1fr 1fr; gap:2.5pt 10pt;
  margin:0; padding:0; list-style:none}
.opt{display:grid; grid-template-columns:12pt 1fr 9pt; gap:4pt; align-items:start;
  padding:1.6pt 3pt; border:.8pt solid transparent; border-radius:3pt; line-height:1.5}
.opt .ltr{font-family:"IBM Plex Mono",monospace; font-size:7.6pt; font-weight:600;
  border:.6pt solid #666; border-radius:2pt; text-align:center; padding:0; line-height:1.6}
.opt .mk{font-weight:700; text-align:center}
.opt.right{border-color:#000}
.opt.right .ltr{background:#000; color:#fff; border-color:#000}
.opt.right .txt{font-weight:700}
.opt.right .mk::before{content:"✓"}
.why{margin:5pt 0 1pt; padding:3.5pt 6pt; border:.5pt solid #999;
  border-inline-start:2.2pt solid #000; border-radius:3pt;
  font-size:8.5pt; color:#222; line-height:1.6}
.why b{font-weight:700}
.why .tag{font-family:"IBM Plex Mono",monospace; font-weight:700}

/* ---------- مفتاح/ورقة الإجابات ---------- */
.key{break-before:page; page-break-before:always}
.key h2{font-family:"Readex Pro",sans-serif; font-size:13pt; margin:0 0 2pt;
  border-bottom:1.2pt solid #000; padding-bottom:4pt}
.key p{font-size:8.6pt; color:#444; margin:5pt 0 8pt}
.keygrid{display:grid; grid-template-columns:repeat(10,1fr); gap:2.5pt}
.keycell{display:flex; justify-content:space-between; align-items:baseline; gap:3pt;
  border:.5pt solid #888; border-radius:2.5pt; padding:2pt 4pt;
  font-family:"IBM Plex Mono",monospace; font-variant-numeric:tabular-nums; font-size:8.2pt}
.keycell i{font-style:normal; color:#666; font-size:7.4pt}
.keycell b{font-weight:700}
.keycell.blank{height:16pt}
footer.note{margin-top:12pt; padding-top:6pt; border-top:.5pt solid #999;
  font-size:8pt; color:#555; line-height:1.6}
"""

HEAD = ('<meta charset="utf-8">\n'
        '<link rel="preconnect" href="https://fonts.googleapis.com">\n'
        '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>\n'
        '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?'
        'family=Readex+Pro:wght@400;500;600&family=IBM+Plex+Sans+Arabic:wght@400;500;600;700'
        '&family=IBM+Plex+Mono:wght@400;500;600&display=swap">\n'
        f'<style>{CSS}</style>')


def build(solved: bool) -> str:
    p = ['<!doctype html><html lang="ar" dir="rtl"><head>',
         f'<title>{"الطاقة الحرارية — أسئلة محلولة" if solved else "الطاقة الحرارية — ورقة أسئلة"}</title>',
         HEAD, '</head><body><div class="sheet">']

    p.append('<header class="masthead">')
    p.append('<div class="kicker"><span>الوحدة 1 · علوم · الصفحات 6 – 39</span>'
             '<span class="latin">McGraw-Hill Education</span></div>')
    if solved:
        p.append('<h1>الطاقة الحرارية — بنك أسئلة محلولة</h1>')
        p.append('<p class="sub">اختبار اختيار من متعدد يغطي دروس الوحدة الثلاثة كاملةً. '
                 'الإجابة الصحيحة مُحاطة بإطار وحرفها مظلَّل، ويليها تفسير موجز مستمَد من الكتاب. '
                 'مفتاح الإجابات كاملًا في الصفحة الأخيرة.</p>')
    else:
        p.append('<h1>الطاقة الحرارية — ورقة أسئلة</h1>')
        p.append('<p class="sub">اختبار اختيار من متعدد يغطي دروس الوحدة الثلاثة كاملةً. '
                 'اختر إجابةً واحدة لكل سؤال، ودوِّن حرفها في ورقة الإجابة الموجودة في الصفحة الأخيرة.</p>')
    p.append('<ul class="meta">'
             f'<li>عدد الأسئلة: <b>{TOTAL}</b></li>'
             '<li>عدد الدروس: <b>3</b></li>'
             '<li>الخيارات: <b>A · B · C · D</b></li>'
             f'<li>{"مفتاح الإجابات: <b>الصفحة الأخيرة</b>" if solved else "الدرجة: <b>… / " + str(TOTAL) + "</b>"}</li>'
             '</ul>')
    if not solved:
        p.append('<div class="namebar"><span>الاسم:</span><span>التاريخ:</span><span>الصف:</span></div>')
    p.append('</header>')

    n = 0
    key = []
    for i, (code, title, topics, qs) in enumerate(SECTIONS, 1):
        p.append(f'<section class="lesson" id="l{i}">')
        p.append('<div class="lesson-head">'
                 f'<span class="lesson-num">{e(code)}</span><h2>{e(title)}</h2>'
                 f'<span class="lesson-count">{len(qs)} سؤالًا</span></div>')
        p.append(f'<p class="lesson-topics">{e(topics)}</p>')
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
                p.append('<div class="why"><b>الإجابة الصحيحة: '
                         f'<span class="tag">{LETTERS[ci]}</span></b> — {e(exp)}</div>')
            p.append('</div></article>')
        p.append('</section>')

    p.append('<section class="key">')
    if solved:
        p.append('<h2>مفتاح الإجابات</h2>'
                 f'<p>الأسئلة من 1 إلى {TOTAL} بالترتيب.</p><div class="keygrid">')
        for num, ltr in key:
            p.append(f'<div class="keycell"><i>{num}</i><b>{ltr}</b></div>')
    else:
        p.append('<h2>ورقة الإجابة</h2>'
                 f'<p>اكتب حرف الإجابة (A أو B أو C أو D) أمام رقم كل سؤال.</p><div class="keygrid">')
        for num, _ in key:
            p.append(f'<div class="keycell blank"><i>{num}</i><b></b></div>')
    p.append('</div>')
    p.append('<footer class="note">أُعدَّت هذه الورقة من محتوى الوحدة 1 «الطاقة الحرارية»: '
             'الدرس 1.1 الطاقة الحرارية ودرجة الحرارة والحرارة، والدرس 1.2 انتقال الطاقة الحرارية، '
             'والدرس 1.3 استخدام الطاقة الحرارية.</footer>')
    p.append('</section></div></body></html>')
    return "\n".join(p)


if __name__ == "__main__":
    summary()
    for solved, name in ((True, "chap1-mcq-print.html"), (False, "chap1-mcq-practice.html")):
        open(OUT + name, "w", encoding="utf-8").write(build(solved))
        print("تم إنشاء:", name)
