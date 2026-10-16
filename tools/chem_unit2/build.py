# -*- coding: utf-8 -*-
"""يبني اختبار الوحدة 2 (العناصر والروابط الكيميائية) بصيغة HTML من بنك الأسئلة،
مع الإجابة والشرح بعد كل سؤال، والأشكال مقصوصة من ملف الكتاب نفسه.
الاستخدام:  python3 build.py <ملف_الإخراج.html>"""
import base64, collections, glob, html, io, os, random, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from figures import build as build_figures
from q1 import INTRO, L21
from q2 import L22
from q3 import L23
from q4 import REVIEW

LET = ["A", "B", "C", "D"]

SECTIONS = [
    ("intro", "مقدمة الوحدة: كيف تُكوِّن الذرات روابط؟", "ص 40–43", INTRO),
    ("l21", "الدرس 2.1 — الإلكترونات ومستويات الطاقة", "ص 44–51", L21),
    ("l22", "الدرس 2.2 — المركَّبات والصيغ الكيميائية والروابط التساهمية", "ص 54–62", L22),
    ("l23", "الدرس 2.3 — الروابط الأيونية والفلزية", "ص 64–72", L23),
    ("rev", "دليل الدراسة ومراجعة الوحدة والاختبار المعياري", "ص 74–79", REVIEW),
]

FIGS = build_figures()


# ---------- تحقُّق صارم ----------
def validate():
    errs, seen = [], {}
    for code, _, _, qs in SECTIONS:
        for i, q in enumerate(qs, 1):
            tag = f"{code}#{i}"
            if not q["text"].strip():
                errs.append(f"{tag}: نص فارغ")
            if q["fig"] and q["fig"] not in FIGS:
                errs.append(f"{tag}: شكل غير موجود {q['fig']}")
            if q["kind"] == "mcq":
                o = q["opts"]
                if len(o) != 4 or len({x.strip() for x in o}) != 4:
                    errs.append(f"{tag}: الخيارات يجب أن تكون 4 مختلفة")
                ans = q["ans"] if isinstance(q["ans"], list) else [q["ans"]]
                if not ans or any(not isinstance(a, int) or not 0 <= a <= 3 for a in ans):
                    errs.append(f"{tag}: رقم الإجابة خطأ")
                if not q["exp"].strip():
                    errs.append(f"{tag}: لا يوجد شرح")
            elif q["kind"] == "tf":
                if not isinstance(q["ans"], bool) or not q["exp"].strip():
                    errs.append(f"{tag}: صح/خطأ ناقص")
            elif not q["ans"].strip():
                errs.append(f"{tag}: لا توجد إجابة")
            key = (q["kind"], q["text"].strip())
            if key in seen:
                errs.append(f"{tag}: مكرّر مع {seen[key]}")
            seen[key] = tag
    return errs


# ---------- توزيع الإجابات الصحيحة على A–D في أسئلتي فقط (أسئلة الكتاب تبقى كما طُبعت) ----------
def rebalance():
    mine = [q for _, _, _, qs in SECTIONS for q in qs if q["kind"] == "mcq" and not q["book"]]
    targets = [i % 4 for i in range(len(mine))]
    random.Random(20261014).shuffle(targets)
    for q, t in zip(mine, targets):
        ci = q["ans"]
        r = (t - ci) % 4
        rot = q["opts"][-r:] + q["opts"][:-r] if r else list(q["opts"])
        assert rot[(ci + r) % 4] == q["opts"][ci]
        q["opts"], q["ans"] = rot, (ci + r) % 4


errs = validate()
if errs:
    print("\n".join(errs)); sys.exit(1)
rebalance()
assert not validate()

ALL = [q for _, _, _, qs in SECTIONS for q in qs]
TOTAL = len(ALL)
kinds = collections.Counter(q["kind"] for q in ALL)
books = sum(q["book"] for q in ALL)
dist = collections.Counter(q["ans"] for q in ALL if q["kind"] == "mcq" and not q["book"])
print(f"عدد الأسئلة: {TOTAL}  (اختيار: {kinds['mcq']}، صح/خطأ: {kinds['tf']}، كتابي: {kinds['wr']}، من الكتاب: {books})")
print("توزيع الإجابات في أسئلتي:", {LET[k]: v for k, v in sorted(dist.items())})
tf = collections.Counter(q["ans"] for q in ALL if q["kind"] == "tf")
print("صح/خطأ:", {"صح": tf[True], "خطأ": tf[False]})
for code, title, _, qs in SECTIONS:
    print(f"  {code}: {len(qs)}")


# ---------- الخطوط (Cairo مضمَّن حتى يعمل الملف بلا إنترنت وعند الطباعة) ----------
def font_faces():
    pkg = os.path.join(HERE, "fonts")
    css = []
    for sub, rng in (("arabic", "U+0600-06FF,U+0750-077F,U+08A0-08FF,U+FB50-FDFF,U+FE70-FEFF,U+200C-200E,U+2010-2011,U+204F,U+2E41"),
                     ("latin", "U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+2000-206F,U+2074,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD")):
        for w in (400, 700, 800):
            p = os.path.join(pkg, f"cairo-{sub}-{w}-normal.woff2")
            if os.path.exists(p):
                b = base64.b64encode(open(p, "rb").read()).decode()
                css.append(f"@font-face{{font-family:'Cairo';font-style:normal;font-weight:{w};font-display:swap;"
                           f"src:url(data:font/woff2;base64,{b}) format('woff2');unicode-range:{rng};}}")
    return "\n".join(css)


def fig_css():
    out = []
    for name, uri in FIGS.items():
        w, h = Image.open(io.BytesIO(base64.b64decode(uri.split(",", 1)[1]))).size
        maxw = min(w, 600)
        out.append(f".f-{name}{{background-image:url({uri});aspect-ratio:{w}/{h};width:min(100%,{maxw}px)}}")
    return "\n".join(out)


CSS = r"""
:root{
  --paper:#FFF9F0; --ink:#1E2430; --ink2:#4A5568; --rule:#E6DCCB;
  --mcq:#2563EB; --mcq-bg:#EAF1FF; --tf:#0F9D58; --tf-bg:#E6F6EE; --wr:#E8710A; --wr-bg:#FFF1E3;
  --book:#7B2CBF; --book-bg:#F3E8FF; --ans:#0B7A43; --ans-bg:#EAF8F0; --ans-rule:#9BD8B7;
  --c-intro:#7B2CBF; --c-l21:#1E66D0; --c-l22:#0A8F7E; --c-l23:#E0661B; --c-rev:#C62839;
}
*{box-sizing:border-box}
html{font-size:10pt}
body{margin:0;background:var(--paper);color:var(--ink);font-family:'Cairo',system-ui,sans-serif;line-height:1.75;font-size:10pt}
.wrap{max-width:900px;margin:0 auto;padding:18px 16px 60px}
.ltr{direction:ltr;unicode-bidi:isolate;display:inline-block}
header.cover{background:linear-gradient(135deg,#FFB627 0%,#FF7A3D 55%,#E2335A 100%);color:#fff;border-radius:18px;padding:22px 24px;box-shadow:0 8px 24px -12px rgba(0,0,0,.4)}
header.cover h1{margin:0;font-size:1.9rem;font-weight:800;line-height:1.35}
header.cover p{margin:.35rem 0 0;font-size:1.05rem}
.stats{display:flex;flex-wrap:wrap;gap:8px;margin-top:14px}
.chip{background:rgba(255,255,255,.22);border:1px solid rgba(255,255,255,.5);border-radius:999px;padding:2px 12px;font-weight:700}
.howto{background:#fff;border:2px dashed var(--rule);border-radius:14px;padding:12px 16px;margin:16px 0}
.howto h2{margin:0 0 4px;font-size:1.1rem}
.howto ul{margin:0;padding-inline-start:20px}
.legend{display:flex;flex-wrap:wrap;gap:8px;margin-top:8px}
.toolbar{position:sticky;top:0;z-index:5;display:flex;gap:8px;justify-content:center;padding:8px 0;background:var(--paper)}
.toolbar button{font:inherit;font-weight:700;border:0;border-radius:999px;padding:6px 16px;background:#1E2430;color:#fff;cursor:pointer}
.toc{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:8px;margin:12px 0 4px}
.toc a{display:block;text-decoration:none;color:#fff;border-radius:12px;padding:8px 12px;font-weight:700}
section.sec{margin-top:26px}
.sec-head{border-radius:14px;color:#fff;padding:12px 18px;margin-bottom:12px}
.sec-head h2{margin:0;font-size:1.3rem;line-height:1.4}
.sec-head span{opacity:.9;font-size:.95rem}
.q{background:#fff;border:1px solid var(--rule);border-inline-start:6px solid var(--kc);border-radius:12px;padding:10px 14px 12px;margin:10px 0;break-inside:avoid}
.q.mcq{--kc:var(--mcq)} .q.tf{--kc:var(--tf)} .q.wr{--kc:var(--wr)}
.qhead{display:flex;flex-wrap:wrap;align-items:center;gap:6px;margin-bottom:4px}
.num{background:var(--kc);color:#fff;border-radius:8px;min-width:2.2em;text-align:center;font-weight:800;padding:0 6px}
.badge{border-radius:999px;padding:0 10px;font-size:.85rem;font-weight:700}
.b-mcq{background:var(--mcq-bg);color:var(--mcq)} .b-tf{background:var(--tf-bg);color:var(--tf)} .b-wr{background:var(--wr-bg);color:var(--wr)}
.b-book{background:var(--book-bg);color:var(--book)} .b-page{background:#F1F3F6;color:var(--ink2)}
.qtext{font-weight:700;font-size:1.02rem}
.fig.small{max-width:min(100%,380px);max-height:260px}
.fig{max-height:420px}
.fig{background-repeat:no-repeat;background-size:contain;background-position:center;margin:8px auto;border-radius:8px;border:1px solid var(--rule);background-color:#fff;-webkit-print-color-adjust:exact;print-color-adjust:exact}
ol.opts{list-style:none;margin:6px 0 0;padding:0;display:grid;grid-template-columns:1fr 1fr;gap:4px 14px}
ol.opts li{display:flex;gap:6px;align-items:baseline}
.let{flex:none;display:inline-flex;align-items:center;justify-content:center;width:1.55em;height:1.55em;border-radius:50%;background:var(--mcq);color:#fff;font-weight:800;font-size:.85rem;line-height:1;margin-inline-end:4px;-webkit-print-color-adjust:exact;print-color-adjust:exact}
ol.opts li{align-items:center}
.tfline{margin-top:6px;font-weight:700;color:var(--ink2)}
.lines{margin-top:6px;height:3.2em;background:repeating-linear-gradient(to bottom,transparent 0,transparent 1.55em,#C9D6E3 1.55em,#C9D6E3 calc(1.55em + 1px))}
.ans{margin-top:8px;background:var(--ans-bg);border:1px solid var(--ans-rule);border-radius:10px;padding:6px 12px}
.ans .k{font-weight:800;color:var(--ans)}
.ans .why{color:#22313F}
body.hide-ans .ans{display:none}
body:not(.hide-ans) .lines{display:none}
footer{margin-top:30px;text-align:center;color:var(--ink2);font-size:.9rem}
@media (max-width:620px){ol.opts{grid-template-columns:1fr} header.cover h1{font-size:1.5rem}}
@media print{
  @page{size:A4;margin:12mm 11mm}
  body{background:#fff}
  .toolbar{display:none}
  .wrap{max-width:none;padding:0}
  .q{box-shadow:none}
  section.sec{break-before:page;margin-top:0}
  .fig{max-width:min(100%,430px);max-height:300px}
  .fig.small{max-width:min(100%,300px);max-height:200px}
  .sec-head,header.cover,.toc a,.num,.badge{-webkit-print-color-adjust:exact;print-color-adjust:exact}
}
"""

e = html.escape
import re
_TAG = re.compile(r"(<[^>]+>)")
_SUP = "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻₀₁₂₃₄₅₆₇₈₉"
_LAT = re.compile(r"[A-Za-z][A-Za-z0-9" + _SUP + r"]*")
# رمز كيميائي متبوع بأرقام سفلية/علوية مكتوبة بوسوم HTML، مثل N<sup>3−</sup> أو C<sub>12</sub>H<sub>22</sub>O<sub>11</sub>
_FORM = re.compile(r"(?:[A-Z][a-z]?(?:<su[bp]>[^<]*</su[bp]>)?)+(?<=</sub>|</sup>)")


_NUM = r"[−+-]?\d+(?:\.\d+)?(?:\s?(?:pm|%))?"
_MATH = re.compile(_NUM + r"(?:\s*[−+×÷=≈-]\s*" + _NUM + r")+")


def bidi(s):
    """يعزل كل رمز لاتيني أو صيغة كيميائية أو عملية حسابية داخل النص العربي حتى لا ينقلب ترتيبها."""
    parts = _TAG.split(s)
    s = "".join(p if p.startswith("<") else _MATH.sub(lambda m: f'<span class="ltr" dir="ltr">{m.group(0)}</span>', p) for p in parts)
    s = _FORM.sub(lambda m: f'<span class="ltr" dir="ltr">{m.group(0)}</span>', s)
    out, depth = [], 0
    for part in _TAG.split(s):
        if part.startswith("<"):
            out.append(part)
            if part.startswith('<span class="ltr"'):
                depth += 1
            elif part.startswith("</span") and depth:
                depth -= 1
        elif depth:
            out.append(part)
        else:
            part = re.sub(r"(?<![\u0600-\u06FF])و(?=[A-Za-z])", "و ", part)   # «وNa» ← «و Na» لوضوح القراءة
            out.append(_LAT.sub(lambda m: f"<bdi>{m.group(0)}</bdi>", part))
    return "".join(out)


def pdf_page(p):
    """رقم الصفحة في ملف «electorics unit.pdf» المقابل لرقم صفحة الكتاب المطبوع."""
    if 40 <= p <= 51:
        return p - 39
    if 54 <= p <= 72:
        return p - 41
    if 74 <= p <= 79:
        return p - 42
    raise ValueError(f"صفحة كتاب غير موجودة في الملف: {p}")


def page_label(p):
    return f"ص {p} في الكتاب (صفحة {pdf_page(p)} في ملف PDF)"


def kind_badge(k):
    return {"mcq": '<span class="badge b-mcq">اختيار من متعدد</span>',
            "tf": '<span class="badge b-tf">صح أم خطأ</span>',
            "wr": '<span class="badge b-wr">سؤال كتابي</span>'}[k]


def render_q(n, q, small=False):
    h = [f'<article class="q {q["kind"]}" id="q{n}"><div class="qhead"><span class="num">{n}</span>{kind_badge(q["kind"])}']
    if q["book"]:
        h.append(f'<span class="badge b-book">سؤال من الكتاب — {page_label(q["p"])}</span>')
    else:
        h.append(f'<span class="badge b-page">المرجع: {page_label(q["p"])}</span>')
    h.append(f'</div><div class="qtext">{bidi(q["text"])}</div>')
    if q["fig"]:
        h.append(f'<div class="fig f-{q["fig"]}{' small' if small else ''}" role="img" aria-label="شكل من الكتاب"></div>')
    if q["kind"] == "mcq":
        h.append('<ol class="opts">' + "".join(f'<li><span class="let">{LET[i]}</span><span>{bidi(o)}</span></li>'
                                              for i, o in enumerate(q["opts"])) + "</ol>")
        ans = q["ans"] if isinstance(q["ans"], list) else [q["ans"]]
        key = " و ".join(f'<span class="let">{LET[a]}</span> {bidi(q["opts"][a])}' for a in ans)
        h.append(f'<div class="ans"><div><span class="k">✔ الإجابة: </span>{key}</div><div class="why"><span class="k">الشرح: </span>{bidi(q["exp"])}</div></div>')
    elif q["kind"] == "tf":
        h.append('<div class="tfline">ضع علامة:  ( صح )   أو   ( خطأ )</div>')
        k = "✔ صح" if q["ans"] else "✘ خطأ"
        h.append(f'<div class="ans"><div><span class="k">الإجابة: </span><b>{k}</b></div><div class="why"><span class="k">الشرح: </span>{bidi(q["exp"])}</div></div>')
    else:
        h.append('<div class="lines"></div>')
        h.append(f'<div class="ans"><div class="k">✔ الإجابة النموذجية:</div><div class="why">{bidi(q["ans"])}</div></div>')
    h.append("</article>")
    return "".join(h)


def build_html():
    body, toc, n = [], [], 0
    for code, title, pages, qs in SECTIONS:
        toc.append(f'<a href="#{code}" style="background:var(--c-{code})">{e(title)}<br><small>{pages} · {len(qs)} سؤالًا</small></a>')
        body.append(f'<section class="sec" id="{code}"><div class="sec-head" style="background:var(--c-{code})"><h2>{e(title)}</h2><span>{pages} · {len(qs)} سؤالًا</span></div>')
        recent = []
        for q in qs:
            n += 1
            body.append(render_q(n, q, small=bool(q["fig"]) and q["fig"] in recent))
            recent = (recent + [q["fig"]])[-3:]
        body.append("</section>")
    return f"""<!doctype html>
<html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>اختبار العناصر والروابط</title>
<style>{font_faces()}
{CSS}
{fig_css()}</style></head>
<body><div class="wrap">
<header class="cover"><h1>اختبار شامل محلول: الوحدة 2 — العناصر والروابط الكيميائية</h1>
<p>الدروس 2.1 و2.2 و2.3، ودليل الدراسة، ومراجعة الوحدة، والتدريب على الاختبار المعياري (صفحات الكتاب 40–79).</p>
<div class="stats"><span class="chip">{TOTAL} سؤالًا</span><span class="chip">{kinds['mcq']} اختيار من متعدد</span>
<span class="chip">{kinds['tf']} صح أم خطأ</span><span class="chip">{kinds['wr']} سؤالًا كتابيًا</span><span class="chip">{books} سؤالًا من الكتاب نفسه</span></div></header>
<div class="howto"><h2>كيف تستخدم هذا الاختبار؟</h2><ul>
<li>اقرأ السؤال وحاول الإجابة أولًا، ثم انظر إلى <b>الإجابة والشرح</b> في المربع الأخضر تحته.</li>
<li>للتدريب دون رؤية الإجابات اضغط زر «إخفاء الإجابات» (تظهر سطور للكتابة). ويمكن طباعة الاختبار بالحالتين.</li>
<li>بجانب كل سؤال رقم صفحته: الرقم المطبوع أسفل صفحة الكتاب، ورقم الصفحة نفسها في ملف PDF (يبدأ الملف من صفحة الكتاب 40). والأشكال مأخوذة من الكتاب نفسه.</li></ul>
<div class="legend">{kind_badge('mcq')}{kind_badge('tf')}{kind_badge('wr')}<span class="badge b-book">سؤال من الكتاب</span><span class="badge b-page">المرجع: صفحة الكتاب</span></div></div>
<div class="toolbar"><button type="button" onclick="document.body.classList.toggle('hide-ans');this.textContent=document.body.classList.contains('hide-ans')?'إظهار الإجابات':'إخفاء الإجابات'">إخفاء الإجابات</button>
<button type="button" onclick="window.print()">طباعة</button></div>
<nav class="toc">{''.join(toc)}</nav>
{''.join(body)}
<footer>بالتوفيق! 🌟 — أُعدّ هذا الاختبار من محتوى الوحدة 2 «العناصر والروابط الكيميائية».</footer>
</div></body></html>"""


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "..", "..", "unit2-chemical-bonds-exam.html")
    page = build_html()
    with open(out, "w", encoding="utf-8") as f:
        f.write(page)
    print("كُتب:", out, f"({len(page.encode()) // 1024} KB)")
