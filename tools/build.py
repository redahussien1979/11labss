# -*- coding: utf-8 -*-
"""يبني ورقة الاختبار (HTML) وملف Markdown من بنك الأسئلة نفسه،
بحيث يستحيل أن يختلف مفتاح الإجابات عن الأسئلة."""
import html
from bank import SECTIONS, TOTAL, LETTERS, summary

summary()

e = html.escape


# ---------- HTML ----------
CSS = """
:root{
  --paper:#F2F5F7; --surface:#FFFFFF; --surface-2:#EAEFF2;
  --ink:#111A20; --ink-2:#3C4C57; --ink-3:#6B7C88;
  --rule:#CFD9E0; --rule-soft:#E2E9ED;
  --cool:#12688F; --amber:#A66A12; --ember:#B33A22;
  --verify:#1F7A4D; --verify-bg:#E7F3EC; --verify-rule:#9FCFB4;
  --accent:var(--cool);
  --shadow:0 1px 2px rgba(17,26,32,.06), 0 8px 24px -18px rgba(17,26,32,.5);
}
@media (prefers-color-scheme: dark){
  :root:not([data-theme="light"]){
    --paper:#0D1319; --surface:#141C23; --surface-2:#1B242C;
    --ink:#E6EDF2; --ink-2:#AFC0CC; --ink-3:#7D909D;
    --rule:#2A3742; --rule-soft:#222D36;
    --cool:#5FB4DA; --amber:#E0AC57; --ember:#F0836A;
    --verify:#63C494; --verify-bg:#142C21; --verify-rule:#2E5F45;
    --shadow:0 1px 2px rgba(0,0,0,.5), 0 10px 28px -20px rgba(0,0,0,.9);
  }
}
:root[data-theme="dark"]{
  --paper:#0D1319; --surface:#141C23; --surface-2:#1B242C;
  --ink:#E6EDF2; --ink-2:#AFC0CC; --ink-3:#7D909D;
  --rule:#2A3742; --rule-soft:#222D36;
  --cool:#5FB4DA; --amber:#E0AC57; --ember:#F0836A;
  --verify:#63C494; --verify-bg:#142C21; --verify-rule:#2E5F45;
  --shadow:0 1px 2px rgba(0,0,0,.5), 0 10px 28px -20px rgba(0,0,0,.9);
}

*{box-sizing:border-box}
html{-webkit-text-size-adjust:100%}
body{
  margin:0; background:var(--paper); color:var(--ink);
  font-family:"IBM Plex Sans Arabic","Segoe UI",Tahoma,sans-serif;
  font-size:16.5px; line-height:1.85; direction:rtl;
}
.wrap{max-width:60rem; margin:0 auto; padding:0 clamp(1rem,4vw,2.5rem) 5rem}

/* ---------- رأس الصفحة ---------- */
.masthead{padding:clamp(2.5rem,7vw,4.5rem) 0 1.75rem; border-bottom:1px solid var(--rule)}
.eyebrow{
  font-size:.78rem; font-weight:500; color:var(--ink-3);
  display:flex; gap:.65rem; align-items:center; flex-wrap:wrap; margin-bottom:1rem;
}
.latin{font-family:"IBM Plex Mono",ui-monospace,monospace; font-size:.72rem;
  letter-spacing:.16em; text-transform:uppercase; direction:ltr; unicode-bidi:isolate}
.eyebrow .dot{width:5px;height:5px;border-radius:50%;background:var(--ember)}
h1{
  font-family:"Readex Pro","IBM Plex Sans Arabic",sans-serif; font-weight:600;
  font-size:clamp(2rem,5.5vw,3.1rem); line-height:1.25; margin:0 0 .6rem;
  text-wrap:balance;
}
.standfirst{margin:0; max-width:52ch; color:var(--ink-2); font-size:1.03rem}
.ramp{height:4px; border-radius:2px; margin:1.6rem 0 1.3rem;
  background:linear-gradient(to left,var(--cool),var(--amber),var(--ember))}
.facts{display:flex; flex-wrap:wrap; gap:0 2.25rem; padding:0; margin:0; list-style:none}
.facts div{padding:.55rem 0}
.facts dt{font-size:.76rem; font-weight:500; color:var(--ink-3)}
.facts dd{margin:.15rem 0 0; font-weight:600; font-size:.97rem; font-variant-numeric:tabular-nums}

/* ---------- شريط الأدوات ---------- */
.bar{
  position:sticky; top:0; z-index:20; background:color-mix(in srgb,var(--paper) 88%, transparent);
  backdrop-filter:blur(10px); border-bottom:1px solid var(--rule);
  margin:0 calc(-1 * clamp(1rem,4vw,2.5rem)); padding:.6rem clamp(1rem,4vw,2.5rem);
  display:flex; align-items:center; justify-content:space-between; gap:1rem; flex-wrap:wrap;
}
.bar .state{font-size:.85rem; font-weight:500; color:var(--ink-3)}
button.toggle{
  font-family:"IBM Plex Sans Arabic",sans-serif; font-size:.9rem; font-weight:600;
  color:var(--surface); background:var(--ink); border:1px solid var(--ink);
  border-radius:999px; padding:.42rem 1.15rem; cursor:pointer; transition:opacity .15s;
}
button.toggle:hover{opacity:.85}
button.toggle:focus-visible{outline:3px solid var(--cool); outline-offset:2px}

/* ---------- الأقسام ---------- */
section.lesson{margin-top:3.5rem}
.lesson-head{display:flex; gap:1rem; align-items:baseline; border-bottom:2px solid var(--accent); padding-bottom:.7rem}
.lesson-num{
  font-family:"IBM Plex Mono",monospace; font-size:1.5rem; font-weight:600;
  color:var(--accent); font-variant-numeric:tabular-nums; letter-spacing:-.02em;
}
.lesson-head h2{font-family:"Readex Pro",sans-serif; font-weight:600; font-size:clamp(1.3rem,3.4vw,1.75rem);
  margin:0; line-height:1.3}
.lesson-topics{color:var(--ink-3); font-size:.88rem; margin:.7rem 0 0; line-height:1.9}
.lesson-count{margin-inline-start:auto; font-size:.82rem; font-weight:500;
  color:var(--ink-3); font-variant-numeric:tabular-nums; white-space:nowrap}
#l1{--accent:var(--cool)} #l2{--accent:var(--amber)} #l3{--accent:var(--ember)}

/* ---------- السؤال ---------- */
.q{padding:1.6rem 0; border-bottom:1px solid var(--rule-soft); display:grid;
  grid-template-columns:2.6rem 1fr; gap:0 .9rem; align-items:start}
.q:last-of-type{border-bottom:0}
.qn{font-family:"IBM Plex Mono",monospace; font-variant-numeric:tabular-nums;
  font-size:.95rem; font-weight:600; color:var(--accent); padding-top:.2rem;
  border-inline-end:2px solid var(--accent); padding-inline-end:.5rem; text-align:start}
.qtext{margin:0 0 .95rem; font-weight:600; line-height:1.75}
.opts{display:grid; gap:.4rem; margin:0; padding:0; list-style:none}
@media (min-width:52rem){ .opts{grid-template-columns:1fr 1fr; gap:.4rem .9rem} }
.opt{display:grid; grid-template-columns:1.7rem 1fr 1rem; gap:.55rem; align-items:start;
  padding:.5rem .65rem; border:1px solid transparent; border-radius:7px; line-height:1.65}
.opt .mk{font-weight:700; color:var(--verify); text-align:center; line-height:1.65}
.opt .ltr{font-family:"IBM Plex Mono",monospace; font-size:.8rem; font-weight:600;
  color:var(--ink-3); background:var(--surface-2); border-radius:5px; text-align:center;
  padding:.05rem 0; line-height:1.55}
.opt.right{background:var(--verify-bg); border-color:var(--verify-rule)}
.opt.right .ltr{background:var(--verify); color:#fff}
.opt.right .txt{font-weight:600}
.opt.right .mk::before{content:"✓"}
.why{margin:.95rem 0 0; padding:.75rem .95rem; background:var(--surface);
  border:1px solid var(--rule-soft); border-inline-start:3px solid var(--accent);
  border-radius:7px; font-size:.93rem; color:var(--ink-2); line-height:1.85; box-shadow:var(--shadow)}
.why b{font-size:.83rem; color:var(--accent); display:block; margin-bottom:.15rem; font-weight:700}
.why b .ltr-tag{font-family:"IBM Plex Mono",monospace; font-weight:600}
.ans-line{margin:.75rem 0 0; font-size:.93rem; color:var(--verify); font-weight:600; display:none}
.ans-line span{font-family:"IBM Plex Mono",monospace}

/* حالة الإخفاء: ورقة تدريب */
body.hide .opt.right{background:transparent; border-color:transparent}
body.hide .opt.right .ltr{background:var(--surface-2); color:var(--ink-3)}
body.hide .opt.right .txt{font-weight:400}
body.hide .opt.right .mk::before{content:""}
body.hide .why{display:none}
body.hide .keygrid{filter:blur(6px); user-select:none}

/* ---------- مفتاح الإجابات ---------- */
.key{margin-top:4rem; padding-top:2rem; border-top:2px solid var(--rule)}
.key h2{font-family:"Readex Pro",sans-serif; font-size:1.4rem; margin:0 0 .3rem}
.key p{color:var(--ink-3); font-size:.9rem; margin:0 0 1.2rem}
.keygrid{display:grid; grid-template-columns:repeat(auto-fill,minmax(4.6rem,1fr)); gap:.35rem;
  transition:filter .2s}
.keycell{display:flex; justify-content:space-between; align-items:baseline; gap:.4rem;
  background:var(--surface); border:1px solid var(--rule-soft); border-radius:6px;
  padding:.3rem .55rem; font-family:"IBM Plex Mono",monospace; font-variant-numeric:tabular-nums;
  font-size:.85rem}
.keycell i{font-style:normal; color:var(--ink-3); font-size:.75rem}
.keycell b{color:var(--verify)}

footer{margin-top:3.5rem; padding-top:1.5rem; border-top:1px solid var(--rule);
  color:var(--ink-3); font-size:.85rem; line-height:1.9}

@media print{
  body{background:#fff; color:#000; font-size:11.5pt}
  .bar{display:none}
  .q{break-inside:avoid; page-break-inside:avoid}
  .why{box-shadow:none}
  section.lesson{break-before:page}
  section.lesson:first-of-type{break-before:auto}
}
@media (prefers-reduced-motion:reduce){*{transition:none!important; animation:none!important}}
"""
# إصلاح خطأ مطبعي محتمل في المتغيرات

parts = []
parts.append("<title>الطاقة الحرارية — أسئلة محلولة</title>")
parts.append('<link rel="preconnect" href="https://fonts.googleapis.com">')
parts.append('<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>')
parts.append('<link rel="stylesheet" href="https://fonts.googleapis.com/css2?'
             'family=Readex+Pro:wght@400;500;600&family=IBM+Plex+Sans+Arabic:wght@400;500;600;700'
             '&family=IBM+Plex+Mono:wght@400;500;600&display=swap">')
parts.append(f"<style>{CSS}</style>")
parts.append('<div class="wrap">')

# رأس الصفحة
parts.append(f"""
<header class="masthead">
  <div class="eyebrow"><span class="dot"></span><span>الوحدة 1 · علوم</span><span>·</span>
    <span>الصفحات 6 – 39</span><span>·</span><span class="latin">McGraw-Hill Education</span></div>
  <h1>الطاقة الحرارية — بنك أسئلة محلولة</h1>
  <p class="standfirst">اختبار اختيار من متعدد يغطي دروس الوحدة الثلاثة كاملةً، مع الإجابة الصحيحة
  وتفسيرها لكل سؤال. اضغط زر «إخفاء الإجابات» لتحويل الورقة إلى اختبار تدريبي، ثم أظهِرها للتصحيح.</p>
  <div class="ramp"></div>
  <dl class="facts">
    <div><dt>عدد الأسئلة</dt><dd>{TOTAL}</dd></div>
    <div><dt>الدروس</dt><dd>3 دروس</dd></div>
    <div><dt>الخيارات</dt><dd>A · B · C · D</dd></div>
    <div><dt>مفتاح الإجابات</dt><dd>في نهاية الورقة</dd></div>
  </dl>
</header>

<div class="bar">
  <span class="state" id="state">الإجابات ظاهرة</span>
  <button class="toggle" id="toggle" type="button" aria-pressed="true">إخفاء الإجابات</button>
</div>
""")

n = 0
key_rows = []
for idx, (code, title, topics, qs) in enumerate(SECTIONS, 1):
    parts.append(f'<section class="lesson" id="l{idx}">')
    parts.append('<div class="lesson-head">'
                 f'<span class="lesson-num">{e(code)}</span>'
                 f'<h2>{e(title)}</h2>'
                 f'<span class="lesson-count">{len(qs)} سؤالًا</span></div>')
    parts.append(f'<p class="lesson-topics">{e(topics)}</p>')
    for q, opts, ci, exp in qs:
        n += 1
        key_rows.append((n, LETTERS[ci]))
        parts.append('<article class="q">')
        parts.append(f'<div class="qn">{n}</div>')
        parts.append('<div>')
        parts.append(f'<p class="qtext">{e(q)}</p>')
        parts.append('<ul class="opts">')
        for j, o in enumerate(opts):
            cls = ' class="opt right"' if j == ci else ' class="opt"'
            parts.append(f'<li{cls}><span class="ltr">{LETTERS[j]}</span>'
                         f'<span class="txt">{e(o)}</span><span class="mk"></span></li>')
        parts.append('</ul>')
        parts.append('<div class="why"><b>الإجابة الصحيحة: '
                     f'<span class="ltr-tag">{LETTERS[ci]}</span></b>{e(exp)}</div>')
        parts.append('</div></article>')
    parts.append('</section>')

# مفتاح الإجابات
parts.append('<section class="key"><h2>مفتاح الإجابات</h2>'
             f'<p>الأسئلة من 1 إلى {TOTAL} بالترتيب.</p><div class="keygrid">')
for num, ltr in key_rows:
    parts.append(f'<div class="keycell"><i>{num}</i><b>{ltr}</b></div>')
parts.append('</div></section>')

parts.append("""
<footer>
  أُعدَّت هذه الورقة من محتوى الوحدة 1 «الطاقة الحرارية»: الدرس 1.1 الطاقة الحرارية ودرجة الحرارة والحرارة،
  والدرس 1.2 انتقال الطاقة الحرارية، والدرس 1.3 استخدام الطاقة الحرارية — بما في ذلك التعريفات المميَّزة،
  والأشكال، وصناديق «التأكد من المفاهيم الرئيسة»، وأصل الكلمة، ومهارات الرياضيات، ومراجعة الوحدة.
</footer>
""")

parts.append('</div>')
parts.append("""
<script>
(function(){
  var b=document.body, t=document.getElementById('toggle'), s=document.getElementById('state');
  function apply(hidden){
    b.classList.toggle('hide',hidden);
    t.textContent = hidden ? 'إظهار الإجابات' : 'إخفاء الإجابات';
    t.setAttribute('aria-pressed', String(!hidden));
    s.textContent = hidden ? 'وضع التدريب — الإجابات مخفية' : 'الإجابات ظاهرة';
    try{ localStorage.setItem('heat-mcq-hidden', hidden ? '1' : '0'); }catch(e){}
  }
  try{ if(localStorage.getItem('heat-mcq-hidden')==='1') apply(true); }catch(e){}
  t.addEventListener('click', function(){ apply(!b.classList.contains('hide')); });
})();
</script>
""")

open("/home/user/11labss/chap1-mcq-solved.html", "w", encoding="utf-8").write("\n".join(parts))

# ---------- Markdown ----------
md = ["# الوحدة 1: الطاقة الحرارية — بنك أسئلة اختيار من متعدد محلولة",
      "",
      f"**عدد الأسئلة:** {TOTAL} · **المصدر:** الوحدة 1 «الطاقة الحرارية»، الصفحات 6–39 (McGraw-Hill Education).",
      ""]
n = 0
for code, title, topics, qs in SECTIONS:
    md += [f"## الدرس {code}: {title}", "", f"*{topics}*", ""]
    for q, opts, ci, exp in qs:
        n += 1
        md.append(f"**{n}. {q}**")
        md.append("")
        for j, o in enumerate(opts):
            md.append(f"- {LETTERS[j]}. {o}")
        md.append("")
        md.append(f"> **الإجابة الصحيحة: {LETTERS[ci]}** — {exp}")
        md.append("")
md += ["## مفتاح الإجابات", ""]
md.append(" | ".join(f"{num}:{ltr}" for num, ltr in key_rows))
md.append("")
open("/home/user/11labss/chap1-mcq-solved.md", "w", encoding="utf-8").write("\n".join(md))
print("تم إنشاء الملفين.")
