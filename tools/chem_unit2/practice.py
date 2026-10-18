# -*- coding: utf-8 -*-
"""ورقة تمارين عملية محلولة للوحدة 2 (العناصر والروابط الكيميائية).
كل الإجابات تُحسب هنا من بيانات العناصر (العدد الذري ورقم المجموعة) وبقواعد الكتاب نفسها،
ولا تُكتب يدويًا. الاستخدام:  python3 practice.py  ← يكتب unit2-practice-problems.html في جذر المستودع."""
import base64, math, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))

# ---------- بيانات العناصر المذكورة في الكتاب: الرمز ← (الاسم، العدد الذري، المجموعة، فلز/لافلز/شبه فلز/غاز نبيل) ----------
EL = {
    "H": ("الهيدروجين", 1, 1, "لافلز"), "He": ("الهيليوم", 2, 18, "غاز نبيل"),
    "Li": ("الليثيوم", 3, 1, "فلز"), "Be": ("البريليوم", 4, 2, "فلز"), "B": ("البورون", 5, 13, "شبه فلز"),
    "C": ("الكربون", 6, 14, "لافلز"), "N": ("النيتروجين", 7, 15, "لافلز"), "O": ("الأكسجين", 8, 16, "لافلز"),
    "F": ("الفلور", 9, 17, "لافلز"), "Ne": ("النيون", 10, 18, "غاز نبيل"), "Na": ("الصوديوم", 11, 1, "فلز"),
    "Mg": ("المغنيسيوم", 12, 2, "فلز"), "Al": ("الألمنيوم", 13, 13, "فلز"), "Si": ("السيليكون", 14, 14, "شبه فلز"),
    "P": ("الفوسفور", 15, 15, "لافلز"), "S": ("الكبريت", 16, 16, "لافلز"), "Cl": ("الكلور", 17, 17, "لافلز"),
    "Ar": ("الأرجون", 18, 18, "غاز نبيل"), "K": ("البوتاسيوم", 19, 1, "فلز"), "Ca": ("الكالسيوم", 20, 2, "فلز"),
    "Se": ("السيلينيوم", 34, 16, "لافلز"), "Br": ("البروم", 35, 17, "لافلز"), "Kr": ("الكريبتون", 36, 18, "غاز نبيل"),
    "I": ("اليود", 53, 17, "لافلز"), "Fe": ("الحديد", 26, 8, "فلز"),
}
NOBLE = [("He", 2), ("Ne", 10), ("Ar", 18), ("Kr", 36)]


def name(s): return EL[s][0]
def Z(s): return EL[s][1]
def group(s): return EL[s][2]


def valence(s):
    """قاعدة الكتاب: المجموعات 1 و2 و13–18 ← رقم الآحاد من رقم المجموعة، والهيليوم استثناء (2)."""
    g = group(s)
    assert g in (1, 2) or 13 <= g <= 18, s
    return 2 if s == "He" else g % 10


def shells(s):
    """توزيع الإلكترونات على المستويات للعناصر من 1 إلى 20 (2 ثم 8 ثم 8 ثم الباقي)، كما في أشكال الكتاب."""
    z = Z(s); assert z <= 20, s
    out = []
    for cap in (2, 8, 8, 2):
        if z <= 0: break
        out.append(min(cap, z)); z -= out[-1]
    return out


def unpaired(n):
    """عدد النقاط الوحيدة عند توزيع n نقطة على 4 جوانب، نقطة نقطة (الخطوة 3 في الشكل 5)."""
    if n in (2,) and False: pass
    return n if n <= 4 else 8 - n


def bonds(s):
    return 0 if s == "He" else unpaired(valence(s))


def ion_change(s):
    """+k: يفقد k إلكترونات (المجموعات 1 و2 و13)، −k: يكتسب k (المجموعات 15 و16 و17)."""
    g, v = group(s), valence(s)
    if g in (1, 2, 13): return +v
    if g in (15, 16, 17): return -(8 - v)
    raise ValueError(s)


def noble_like(s):
    k = ion_change(s)
    electrons = Z(s) - k
    for sym, z in NOBLE:
        if z == electrons: return sym
    raise ValueError(s)


def ve(n):
    """عدد إلكترونات التكافؤ بصيغة عربية صحيحة."""
    return {1: "إلكترون تكافؤ واحد", 2: "إلكترونا تكافؤ"}.get(n, f"{n} إلكترونات تكافؤ")


def dots_n(n):
    return {1: "نقطة واحدة", 2: "نقطتان"}.get(n, f"{n} نقاط")


# ---------- عرض ----------
def F(html):  # صيغة أو رمز لاتيني من اليسار إلى اليمين
    return f'<span class="ltr" dir="ltr">{html}</span>'


def formula(parts):
    """parts: [("Na",2),("S",1)] ← Na<sub>2</sub>S"""
    return "".join(f"{s}<sub>{n}</sub>" if n > 1 else s for s, n in parts)


def charge_txt(k):
    """شحنة الأيون كما يكتبها الكتاب: Na⁺ و Mg²⁺ و N³⁻ (الرقم ثم الإشارة، ولا يُكتب الرقم 1)."""
    sign = "+" if k > 0 else "−"
    return (sign if abs(k) == 1 else f"{abs(k)}{sign}")


def ion(s, k):
    return F(f"{s}<sup>{charge_txt(k)}</sup>")


def dot_svg(sym, n, charge=None, size=84):
    """تمثيل نقطي بقاعدة الكتاب: نقطة على كل جانب بالترتيب أعلى ← يمين ← أسفل ← يسار، ثم نقطة ثانية بالترتيب نفسه.
    الهيليوم: زوج واحد في الأعلى كما في الشكل 6."""
    sides = ["t", "r", "b", "l"]
    count = {k: 0 for k in sides}
    if sym == "He":
        count["t"] = 2
    else:
        for i in range(n):
            count[sides[i % 4]] += 1
    w = 46 + 12 * len(sym)
    cx, cy = w / 2, 50
    dots = []
    half = 7 + 6 * len(sym)
    for side, c in count.items():
        if not c: continue
        offs = [0] if c == 1 else [-6, 6]
        for o in offs:
            if side == "t": dots.append((cx + o, cy - 26))
            if side == "b": dots.append((cx + o, cy + 24))
            if side == "r": dots.append((cx + half + 6, cy - 2 + o))
            if side == "l": dots.append((cx - half - 6, cy - 2 + o))
    circles = "".join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="3.6"/>' for x, y in dots)
    ch = f'<text x="{cx + half + 14:.1f}" y="24" font-size="17">{charge}</text>' if charge else ""
    vw = w + (26 if charge else 0)
    return (f'<svg class="dot" viewBox="0 0 {vw:.0f} 100" width="{size * vw / 100:.0f}" height="{size}" role="img" aria-label="{sym}">'
            f'<text x="{cx:.1f}" y="{cy + 11:.1f}" text-anchor="middle" font-size="32" font-weight="700">{sym}</text>{circles}{ch}</svg>')


def dot_words(n, sym=None):
    if sym == "He": return "زوج واحد (نقطتان)"
    u = unpaired(n); p = (n - u) // 2
    parts = []
    if p: parts.append({1: "زوج واحد", 2: "زوجان", 3: "3 أزواج", 4: "4 أزواج"}[p])
    if u: parts.append({1: "نقطة وحيدة", 2: "نقطتان وحيدتان", 3: "3 نقاط وحيدة", 4: "4 نقاط وحيدة"}[u])
    return " + ".join(parts)


# ---------- بناء الأقسام ----------
SECTIONS = []


def section(title, pages, intro, items):
    SECTIONS.append(dict(title=title, pages=pages, intro=intro, items=items))


def P(q, a, worked=False, draw=False, lines=2):
    return dict(q=q, a=a, worked=worked, draw=draw, lines=lines)


# 1) إلكترونات التكافؤ
def s1():
    it = [P(f"كم إلكترون تكافؤ في ذرة <b>الفوسفور</b> {F('P')}؟ (الفوسفور في المجموعة 15)",
            "<b>الخطوة 1:</b> نحدّد رقم المجموعة ← 15. "
            "<b>الخطوة 2:</b> المجموعة من 13 إلى 18، فنأخذ رقم الآحاد ← <b>5</b>. "
            "<b>الإجابة:</b> للفوسفور <b>5</b> إلكترونات تكافؤ.", worked=True)]
    for s in ["Li", "Mg", "B", "Si", "S", "F", "Ar", "K", "Ca", "Br", "Se", "Kr", "I", "He"]:
        g, v = group(s), valence(s)
        if s == "He":
            a = (f"الهيليوم في المجموعة 18، لكنه <b>استثناء</b>: له مستوى طاقة واحد يتسع لإلكترونين فقط، فله <b>إلكترونا تكافؤ</b> فقط (وليس 8).")
        else:
            a = f"المجموعة <b>{g}</b> ← رقم الآحاد <b>{v}</b> ← لذرة {name(s)} <b>{ve(v)}</b>."
        it.append(P(f"كم إلكترون تكافؤ في ذرة <b>{name(s)}</b> {F(s)}؟ ({name(s)} في المجموعة {g})", a, lines=1))
    section("1. إلكترونات التكافؤ من رقم المجموعة", "ص 47",
            "القاعدة: في المجموعات 1 و2 ومن 13 إلى 18، عدد إلكترونات التكافؤ = <b>رقم الآحاد</b> من رقم المجموعة. الاستثناء: الهيليوم (2).", it)


# 2) ترتيب الإلكترونات في المستويات
def s2():
    def arr(s): return " ثم ".join(str(x) for x in shells(s))
    it = [P(f"اكتب ترتيب إلكترونات ذرة <b>الصوديوم</b> {F('Na')} في مستويات الطاقة (العدد الذري 11).",
            "<b>الخطوة 1:</b> عدد الإلكترونات = العدد الذري = 11. "
            "<b>الخطوة 2:</b> المستوى الأول يتسع لإلكترونين ← يبقى 11 − 2 = 9. "
            "<b>الخطوة 3:</b> المستوى الثاني يتسع لثمانية ← يبقى 9 − 8 = 1. "
            "<b>الخطوة 4:</b> الإلكترون الباقي في المستوى الثالث. "
            "<b>الإجابة:</b> 2 ثم 8 ثم 1. والرقم الأخير (1) هو عدد إلكترونات التكافؤ.", worked=True)]
    for s in ["Li", "C", "O", "Ne", "Mg", "Al", "P", "Cl", "Ar", "K"]:
        sh = shells(s)
        steps = []
        left = Z(s)
        for i, x in enumerate(sh):
            if i < len(sh) - 1:
                steps.append(f"المستوى {i + 1}: {x} (يبقى {F(f'{left} − {x} = {left - x}')})")
                left -= x
            else:
                steps.append(f"المستوى {i + 1}: {x}")
        last = sh[-1]
        a = (f"عدد الإلكترونات = {Z(s)}. " + "، ".join(steps) + f". <b>الإجابة:</b> {arr(s)}. "
             f"الرقم الأخير ({last}) = إلكترونات التكافؤ، ويوافق المجموعة {group(s)}.")
        it.append(P(f"اكتب ترتيب إلكترونات ذرة <b>{name(s)}</b> {F(s)} (العدد الذري {Z(s)}).", a, lines=1))
    section("2. ترتيب الإلكترونات في مستويات الطاقة", "ص 46 و67–68",
            "المستوى الأول يتسع لإلكترونين فقط، والثاني حتى 8. نملأ المستويات من الأقرب إلى النواة إلى الأبعد.", it)


# 3) التمثيل النقطي
def s3():
    it = [P(f"ارسم التمثيل النقطي لإلكترونات ذرة <b>النيتروجين</b> {F('N')} (المجموعة 15).",
            "<b>الخطوة 1:</b> المجموعة 15 ← 5 إلكترونات تكافؤ ← 5 نقاط. "
            "<b>الخطوة 2:</b> نضع نقطة واحدة على كل جانب بالترتيب: أعلى، يمين، أسفل، يسار (4 نقاط). "
            "<b>الخطوة 3:</b> النقطة الخامسة نضعها في الأعلى فتكوّن زوجًا. "
            f"<b>النتيجة:</b> {dot_svg('N', 5)} زوج واحد + 3 نقاط وحيدة.", worked=True, draw=True)]
    for s in ["H", "Be", "B", "C", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "S", "Cl", "Ar", "K", "He"]:
        v = valence(s)
        a = (("المجموعة 18، لكن الهيليوم استثناء ← إلكترونا تكافؤ ← نقطتان. " if s == "He" else f"المجموعة {group(s)} ← {ve(v)} ← {dots_n(v)}. ") +
             f"{dot_svg(s, v)} {dot_words(v, s)}.")
        it.append(P(f"ارسم التمثيل النقطي لإلكترونات ذرة <b>{name(s)}</b> {F(s)}.", a, draw=True))
    section("3. رسم التمثيل النقطي للإلكترونات", "ص 48 والشكل 5",
            "نكتب رمز العنصر، ونضع حوله نقطة لكل إلكترون تكافؤ: نقطة واحدة على كل جانب بالترتيب (أعلى، يمين، أسفل، يسار)، ثم نبدأ بالنقطة الثانية بالترتيب نفسه.", it)


# 4) عدد الروابط والاستقرار (الخطوات الخمس في الشكل 5)
def s4():
    it = [P(f"طبّق الخطوات الخمس في الشكل 5 على ذرة <b>الكربون</b> {F('C')}.",
            "(1) رقم المجموعة: 14. (2) إلكترونات التكافؤ: 4. "
            f"(3) التمثيل النقطي: {dot_svg('C', 4)} "
            "(4) هل كل النقاط أزواج؟ لا ← <b>غير مستقر</b> كيميائيًا. "
            "(5) عدد النقاط الوحيدة = <b>4</b> ← يكوّن <b>4 روابط</b>.", worked=True)]
    for s in ["Li", "Mg", "B", "N", "O", "F", "Ne", "Si", "P", "S", "Cl", "Ar"]:
        v, b = valence(s), bonds(s)
        stable = b == 0
        a = (f"(1) المجموعة {group(s)}. (2) {ve(v)}. (3) {dot_svg(s, v, size=64)} {dot_words(v, s)}. "
             f"(4) {'كل النقاط أزواج ← <b>مستقر</b> كيميائيًا' if stable else 'توجد نقاط وحيدة ← <b>غير مستقر</b>'}. "
             f"(5) عدد الروابط = عدد النقاط الوحيدة = <b>{b}</b>.")
        it.append(P(f"طبّق الخطوات الخمس على ذرة <b>{name(s)}</b> {F(s)}: المجموعة؟ إلكترونات التكافؤ؟ التمثيل النقطي؟ مستقر أم لا؟ كم رابطة؟", a, lines=3))
    section("4. عدد الروابط والاستقرار (خطوات الشكل 5)", "ص 48",
            "الخطوات: (1) رقم المجموعة ← (2) إلكترونات التكافؤ ← (3) التمثيل النقطي ← (4) مستقر إذا كانت كل النقاط أزواجًا ← (5) عدد الروابط = عدد النقاط الوحيدة.", it)


# 5) يكتسب أم يفقد؟ ومثل أي غاز نبيل؟
def s5():
    it = [P(f"هل تكتسب ذرة <b>الكلور</b> {F('Cl')} إلكترونات أم تفقدها لتستقر؟ كم إلكترونًا؟ وترتيبها يصبح مثل أي غاز نبيل؟",
            "(1) للكلور 7 إلكترونات تكافؤ، أي قريب جدًا من 8. "
            f"(2) الأسهل أن <b>يكتسب</b> {F('8 − 7 = 1')} إلكترونًا واحدًا. "
            f"(3) إلكتروناته كلها تصبح {F('17 + 1 = 18')}، وهذا عدد إلكترونات <b>الأرجون</b> {F('Ar')}.", worked=True)]
    for s in ["Li", "Na", "K", "Be", "Mg", "Ca", "Al", "N", "O", "F", "S", "Br"]:
        k = ion_change(s); v = valence(s); ng = noble_like(s)
        if k > 0:
            a = (f"(1) له {ve(v)} فقط (فلز). "
                 f"(2) الأسهل أن <b>يفقد {k}</b>. "
                 f"(3) إلكتروناته تصبح {F(f'{Z(s)} − {k} = {Z(s) - k}')}، مثل <b>{name(ng)}</b> {F(ng)}.")
        else:
            g = -k
            a = (f"(1) له {ve(v)} (لافلز)، قريب من 8. "
                 f"(2) الأسهل أن <b>يكتسب {g}</b> ({F(f'8 − {v} = {g}')}). "
                 f"(3) إلكتروناته تصبح {F(f'{Z(s)} + {g} = {Z(s) + g}')}، مثل <b>{name(ng)}</b> {F(ng)}.")
        it.append(P(f"ذرة <b>{name(s)}</b> {F(s)} (العدد الذري {Z(s)}، المجموعة {group(s)}): تكتسب أم تفقد؟ كم إلكترونًا؟ مثل أي غاز نبيل تصبح؟", a, lines=2))
    section("5. يكتسب أم يفقد؟ ومثل أي غاز نبيل يصبح؟", "ص 66–68",
            "الفلزات (المجموعات 1 و2 و13) تفقد إلكترونات تكافؤها. اللافلزات (المجموعات 15 و16 و17) تكتسب ما ينقصها حتى 8.", it)


# 6) شحنة الأيون
def s6():
    it = [P(f"ذرة <b>النيتروجين</b> (7 بروتونات) تكتسب 3 إلكترونات. ما شحنة الأيون؟ وكيف يُكتب؟",
            f"(1) الإلكترونات بعد الاكتساب: {F('7 + 3 = 10')}. "
            f"(2) الشحنة = البروتونات − الإلكترونات = {F('7 − 10 = −3')}. "
            f"(3) يُكتب الأيون: {ion('N', -3)}.", worked=True)]
    for s in ["Na", "K", "Mg", "Ca", "Al", "Li", "O", "S", "F", "Cl", "Br"]:
        k = ion_change(s); p = Z(s); e = p - k
        verb = ("تفقد " if k > 0 else "تكتسب ") + {1: "إلكترونًا واحدًا", 2: "إلكترونين"}.get(abs(k), f"{abs(k)} إلكترونات")
        op = f"{p} − {k} = {e}" if k > 0 else f"{p} + {-k} = {e}"
        a = (f"(1) الإلكترونات بعد التغيّر: {F(op)}. "
             f"(2) الشحنة = {F(f'{p} − {e} = {p - e:+d}'.replace('-', '−'))}. "
             f"(3) يُكتب الأيون: {ion(s, k)}.")
        it.append(P(f"ذرة <b>{name(s)}</b> ({p} {'بروتونًا' if p > 10 else 'بروتونات'}) {verb}. ما شحنة الأيون؟ وكيف يُكتب؟", a, lines=1))
    section("6. حساب شحنة الأيون", "ص 68",
            "الشحنة = عدد البروتونات − عدد الإلكترونات. الفقد يعطي شحنة موجبة (+)، والاكتساب يعطي شحنة سالبة (−).", it)


# 7) صيغة المركّب الأيوني بنقل الإلكترونات
def s7():
    def solve(m, x):
        a, b = ion_change(m), -ion_change(x)
        l = a * b // math.gcd(a, b)
        nm, nx = l // a, l // b
        return a, b, nm, nx
    it = [P(f"ما صيغة المركّب الأيوني الناتج عن <b>الصوديوم</b> و<b>الكبريت</b> (كبريتيد الصوديوم في النشاط الاستكشافي)؟",
            "(1) الصوديوم يعطي <b>إلكترونًا واحدًا</b>. (2) الكبريت يحتاج <b>إلكترونين</b>. "
            "(3) نحتاج ذرتَي صوديوم لتعطيا إلكترونين لذرة كبريت واحدة. "
            f"<b>الإجابة:</b> {F(formula([('Na', 2), ('S', 1)]))}.", worked=True)]
    pairs = [("Na", "Cl"), ("K", "Cl"), ("Li", "F"), ("Mg", "O"), ("Ca", "S"), ("K", "Br"),
             ("Li", "O"), ("K", "S"), ("Ca", "Cl"), ("Mg", "Cl"), ("Al", "Cl"), ("Mg", "N"), ("Al", "O")]
    for m, x in pairs:
        a, b, nm, nx = solve(m, x)
        fm = formula([(m, nm), (x, nx)])
        def w(n): return {1: "إلكترونًا واحدًا", 2: "إلكترونين", 3: "3 إلكترونات"}[n]
        if nm == 1 and nx == 1:
            how = f"ما يعطيه = ما يحتاجه، فذرة من كل عنصر تكفي."
        else:
            def cnt(k): return {1: "ذرة واحدة", 2: "ذرتان"}.get(k, f"{k} ذرات")
            how = (f"يجب أن يتساوى عدد الإلكترونات المُعطاة والمأخوذة: {F(f'{nm} × {a} = {nx} × {b} = {nm * a}')}، "
                   f"أي {cnt(nm)} من {name(m)} و{cnt(nx)} من {name(x)}.")
        ans = (f"(1) {name(m)} يعطي {w(a)} ← {ion(m, a)}. (2) {name(x)} يحتاج {w(b)} ← {ion(x, -b)}. (3) {how} "
               f"<b>الإجابة:</b> {F(fm)}.")
        tag = " <b>(تحدٍّ)</b>" if nm > 1 and nx > 1 else ""
        it.append(P(f"ما صيغة المركّب الأيوني الناتج عن <b>{name(m)}</b> {F(m)} و<b>{name(x)}</b> {F(x)}؟{tag}", ans, lines=2))
    section("7. صيغة المركّب الأيوني بنقل الإلكترونات", "ص 65–68",
            "الفلز يعطي إلكترونات تكافؤه، واللافلز يأخذ ما ينقصه. نختار أقل عدد من الذرات بحيث يكون <b>ما يعطيه الفلز = ما يحتاجه اللافلز</b>.", it)


# 8) قراءة الصيغة الكيميائية
def parse(fml):
    return [(s, int(n) if n else 1) for s, n in re.findall(r"([A-Z][a-z]?)(\d*)", fml)]


def s8():
    def show(fml): return F(re.sub(r"(\d+)", r"<sub>\1</sub>", fml))
    it = [P(f"كم ذرة من كل عنصر في الصيغة {show('CO2')}؟ وكم ذرة في المجموع؟",
            f"(1) الرمز بلا رقم = ذرة واحدة ← الكربون ({F('C')}) ← <b>1</b>. (2) الرقم الصغير بعد الرمز = عدد ذراته ← الأكسجين ({F('O')}) ← <b>2</b>. "
            f"(3) المجموع {F('1 + 2 = 3')} ذرات.", worked=True)]
    for fml in ["H2O", "SO2", "NH3", "C12H22O11", "N2", "Cl2", "O2", "NaCl", "Na2S", "CaCl2", "Al2O3"]:
        parts = parse(fml)
        each = "، ".join(f"{name(s)} ({F(s)}) ← <b>{n}</b>" for s, n in parts)
        tot = sum(n for _, n in parts)
        ssum = " + ".join(str(n) for _, n in parts)
        a = f"{each}. المجموع: " + (F(f"{ssum} = {tot}") if len(parts) > 1 else f"<b>{tot}</b>") + "."
        it.append(P(f"كم ذرة من كل عنصر في الصيغة {show(fml)}؟ وكم ذرة في المجموع؟", a, lines=1))
    section("8. قراءة الصيغة الكيميائية", "ص 58 و60",
            "الرمز يخبرنا بالعنصر، والرقم الصغير بعده (الرقم السفلي) بعدد ذراته. الرمز بلا رقم يعني ذرة واحدة.", it)


# 9) كتابة الصيغة من الوصف
def s9():
    it = [P("يحتوي جزيء ثاني أكسيد الكبريت على ذرة كبريت وذرتَي أكسجين. اكتب صيغته.",
            f"(1) كبريت واحد ← نكتب {F('S')} بلا رقم. (2) أكسجين اثنان ← نكتب {F('O')} ومعه 2. "
            f"<b>الإجابة:</b> {F('SO<sub>2</sub>')}.", worked=True)]
    data = [("ذرة كربون وذرتا أكسجين (ثاني أكسيد الكربون)", [("C", 1), ("O", 2)]),
            ("ذرتا هيدروجين وذرة أكسجين (الماء)", [("H", 2), ("O", 1)]),
            ("ذرة نيتروجين و3 ذرات هيدروجين (الأمونيا)", [("N", 1), ("H", 3)]),
            ("ذرتا كلور مرتبطتان معًا (غاز الكلور)", [("Cl", 2)]),
            ("ذرتا نيتروجين مرتبطتان معًا (غاز النيتروجين)", [("N", 2)]),
            ("12 ذرة كربون و22 ذرة هيدروجين و11 ذرة أكسجين (سكر المائدة)", [("C", 12), ("H", 22), ("O", 11)]),
            ("صوديوم وكلور بنسبة 1 إلى 1 (ملح الطعام)", [("Na", 1), ("Cl", 1)]),
            ("كالسيوم واحد لكل ذرتَي كلور", [("Ca", 1), ("Cl", 2)])]
    for desc, parts in data:
        expl = "، ".join((f"{F(s)} بلا رقم" if n == 1 else f"{F(s)} ومعه {n}") for s, n in parts)
        it.append(P(f"اكتب الصيغة الكيميائية: {desc}.", f"{expl}. <b>الإجابة:</b> {F(formula(parts))}.", lines=1))
    section("9. كتابة الصيغة الكيميائية من الوصف", "ص 60 و62",
            "نكتب رمز كل عنصر، ونضع بعده رقمًا صغيرًا بعدد ذراته، ولا نكتب الرقم 1.", it)


# 10) الروابط التساهمية
def s10():
    it = [P(f"في جزيء النيتروجين {F('N<sub>2</sub>')}: كم زوجًا من الإلكترونات تتشارك الذرتان؟ وما نوع الرابطة؟",
            "(1) لكل نيتروجين 5 إلكترونات تكافؤ = زوج + 3 نقاط وحيدة، فينقصه 3. "
            "(2) تشارك كل ذرة بنقاطها الوحيدة الثلاث ← <b>3 أزواج</b>. "
            "(3) ثلاثة أزواج = رابطة <b>ثلاثية</b>. "
            f"(4) التحقّق: كل ذرة لها الآن {F('5 + 3 = 8')}.", worked=True)]
    rows = [
        ("H<sub>2</sub>", "لكل هيدروجين إلكترون واحد ويحتاج إلكترونًا واحدًا ليصبح له 2", "زوج واحد", "رابطة أحادية واحدة", "كل هيدروجين له 1 + 1 = 2 (مثل الهيليوم)"),
        ("Cl<sub>2</sub>", "لكل كلور 7 إلكترونات تكافؤ وينقصه 1", "زوج واحد", "رابطة أحادية واحدة", "كل كلور له 7 + 1 = 8"),
        ("O<sub>2</sub>", "لكل أكسجين 6 إلكترونات تكافؤ وينقصه 2", "زوجان", "رابطة ثنائية واحدة (O=O)", "كل أكسجين له 6 + 2 = 8"),
        ("H<sub>2</sub>O", "الأكسجين ينقصه 2، وكل هيدروجين ينقصه 1", "زوجان (زوج مع كل هيدروجين)", "رابطتان أحاديتان", "الأكسجين 6 + 2 = 8، وكل هيدروجين 1 + 1 = 2"),
        ("CO<sub>2</sub>", "الكربون له 4 وينقصه 4، وكل أكسجين ينقصه 2", "4 أزواج (زوجان مع كل أكسجين)", "رابطتان ثنائيتان (O=C=O)", "الكربون 4 + 4 = 8، وكل أكسجين 6 + 2 = 8"),
        ("NH<sub>3</sub>", "النيتروجين ينقصه 3، وكل هيدروجين ينقصه 1", "3 أزواج (زوج مع كل هيدروجين)", "3 روابط أحادية", "النيتروجين 5 + 3 = 8، وكل هيدروجين 1 + 1 = 2"),
    ]
    for fml, need, pairs, kind, check in rows:
        chk = re.sub(r"(\d+ \+ \d+ = \d+)", lambda m: F(m.group(1)), check)
        it.append(P(f"في جزيء {F(fml)}: كم زوجًا من الإلكترونات مشتركًا؟ وما نوع الروابط؟",
                    f"(1) {need}. (2) عدد الأزواج المشتركة: <b>{pairs}</b>. (3) نوع الروابط: <b>{kind}</b>. (4) التحقّق: {chk}.", lines=2))
    it.append(P(f"رتّب الروابط في {F('H<sub>2</sub>')} و{F('O<sub>2</sub>')} و{F('N<sub>2</sub>')} من الأقوى إلى الأضعف.",
                f"كلما زادت الأزواج المشتركة زادت القوة: {F('N<sub>2</sub>')} (ثلاثية، 3 أزواج) ← {F('O<sub>2</sub>')} (ثنائية، زوجان) ← {F('H<sub>2</sub>')} (أحادية، زوج واحد).", lines=1))
    section("10. الروابط التساهمية: عدد الأزواج ونوع الرابطة", "ص 57–58",
            "كل ذرة لافلز تشارك بنقاطها الوحيدة. زوج واحد = رابطة أحادية، زوجان = ثنائية، 3 أزواج = ثلاثية. وتصل كل ذرة إلى 8 (والهيدروجين إلى 2).", it)


# 11) قطبي أم غير قطبي
def s11():
    it = [P(f"هل جزيء الماء {F('H<sub>2</sub>O')} قطبي أم غير قطبي؟ ولماذا؟",
            "(1) الذرتان المرتبطتان مختلفتان: أكسجين وهيدروجين. (2) الأكسجين يجذب الإلكترونات المشتركة أقوى. "
            "(3) يصبح الأكسجين سالبًا جزئيًا والهيدروجين موجبًا جزئيًا. <b>الإجابة: قطبي</b>.", worked=True)]
    for fml, nm in [("H<sub>2</sub>", "الهيدروجين"), ("O<sub>2</sub>", "الأكسجين"), ("N<sub>2</sub>", "النيتروجين"), ("Cl<sub>2</sub>", "الكلور")]:
        it.append(P(f"هل جزيء {F(fml)} قطبي أم غير قطبي؟ ولماذا؟",
                    f"الذرتان كلتاهما ذرتا {nm.replace('ال', '', 1)}، أي <b>متماثلتان</b>، فتجذبان الإلكترونات المشتركة <b>بالتساوي</b>. <b>الإجابة: غير قطبي</b>.", lines=1))
    it.append(P("سكبت سكرًا في كوب ماء، وزيتًا في كوب آخر. أيهما يذوب؟ ولماذا؟",
                "<b>السكر يذوب</b> لأن السكر والماء كلاهما قطبي. <b>الزيت لا يذوب</b> لأنه غير قطبي والماء قطبي. القاعدة: «الشبيه يذيب الشبيه».", lines=2))
    section("11. قطبي أم غير قطبي؟", "ص 59",
            "إذا كانت الذرتان المرتبطتان <b>متماثلتين</b> تُجذب الإلكترونات بالتساوي ← غير قطبي. وفي الماء يجذب الأكسجين الإلكترونات أكثر ← قطبي.", it)


# 12) نوع الرابطة
def s12():
    def kind(a, b):
        ta, tb = EL[a][3], EL[b][3]
        if ta == "فلز" and tb == "فلز": return "فلزية"
        if "فلز" in (ta, tb) and "لافلز" in (ta, tb): return "أيونية"
        return "تساهمية"
    it = [P(f"ما نوع الرابطة المتوقّعة بين <b>الصوديوم</b> و<b>الكلور</b>؟",
            "(1) الصوديوم فلز، والكلور لافلز. (2) فلز + لافلز ← ينتقل إلكترون من الفلز إلى اللافلز. <b>الإجابة: رابطة أيونية</b>.", worked=True)]
    for a, b in [("H", "O"), ("Al", "Al"), ("C", "O"), ("K", "Br"), ("N", "H"), ("Mg", "O"), ("Fe", "Fe"), ("Cl", "Cl"), ("Ca", "S"), ("Li", "O")]:
        k = kind(a, b)
        why = {"فلزية": "فلز + فلز ← تتجمّع إلكترونات التكافؤ في بحر من الإلكترونات",
               "أيونية": "فلز + لافلز ← ينتقل الإلكترون من الفلز إلى اللافلز",
               "تساهمية": "لافلز + لافلز ← تتشارك الذرتان الإلكترونات"}[k]
        same = a == b
        q = f"ما نوع الرابطة بين ذرات <b>{name(a)}</b> بعضها مع بعض؟" if same else f"ما نوع الرابطة المتوقّعة بين <b>{name(a)}</b> و<b>{name(b)}</b>؟"
        it.append(P(q, f"{name(a)} {EL[a][3]}{'' if same else f'، و{name(b)} {EL[b][3]}'}. {why}. <b>الإجابة: رابطة {k}</b>.", lines=1))
    props = [("مادة صلبة لامعة، توصل الكهرباء جيدًا، ويمكن طرقها لوحًا، ولا تذوب في الماء.", "فلزية",
              "اللمعان وتوصيل الكهرباء والطرق ألواحًا كلها صفات الفلزات (بحر الإلكترونات)."),
             ("بلورات صلبة هشّة، درجة انصهارها مرتفعة، تذوب في الماء، ومحلولها يوصل الكهرباء.", "أيونية",
              "البلورات الهشّة والانصهار المرتفع وتوصيل المحلول للكهرباء صفات المركّبات الأيونية (مثل الملح)."),
             ("سائل في درجة حرارة الغرفة، درجة غليانه منخفضة، ولا يوصل الكهرباء جيدًا.", "تساهمية",
              "الغليان المنخفض وضعف التوصيل صفات المركّبات التساهمية (مثل الماء).")]
    for d, k, why in props:
        it.append(P(f"مادة مجهولة: {d} ما نوع الرابطة فيها على الأرجح؟", f"{why} <b>الإجابة: رابطة {k}</b>.", lines=1))
    section("12. ما نوع الرابطة؟", "ص 57 و66–70 والجدول 1",
            "لافلز + لافلز ← <b>تساهمية</b>. فلز + لافلز ← <b>أيونية</b>. فلز + فلز ← <b>فلزية</b>.", it)


# 13) النسبة المئوية لتغيّر نصف القطر (أرقام الكتاب فقط)
def pct(atom, ion_r):
    d = ion_r - atom
    r = round(d / atom, 2)
    return d, r, round(r * 100)


def s13():
    def fmt(x): return f"{x:.2f}".replace("-", "−")
    def sgn(x): return (f"+{x}" if x > 0 else f"{x}").replace("-", "−")
    d, r, p = pct(186, 102)
    it = [P(f"نصف قطر ذرة الصوديوم 186 pm، ونصف قطر أيون {ion('Na', 1)} يساوي 102 pm. ما النسبة المئوية لتغيّر نصف القطر؟",
            f"(1) نطرح: {F(f'102 − 186 = {sgn(d)} pm')}. (2) نقسم على نصف قطر الذرة: {F(f'{sgn(d)} ÷ 186 = {fmt(r)}')}. "
            f"(3) نضرب في 100: {F(f'{fmt(r)} × 100 = {sgn(p)}%')}. الإشارة سالبة ← الأيون <b>أصغر</b> بنحو 45%.", worked=True)]
    data = [("O", "الأكسجين", 73, 140, -2), ("Al", "الألمنيوم", 143, 54, 3), ("I", "اليود", 133, 216, -1), ("K", "البوتاسيوم", 227, 133, 1)]
    for s, nm, a, i, k in data:
        d, r, p = pct(a, i)
        res = "زيادة (الأيون <b>أكبر</b>)" if p > 0 else "انخفاض (الأيون <b>أصغر</b>)"
        it.append(P(f"نصف قطر ذرة {nm} {F(s)} يساوي {a} pm، ونصف قطر أيون {ion(s, k)} يساوي {i} pm. ما النسبة المئوية لتغيّر نصف القطر؟",
                    f"(1) {F(f'{i} − {a} = {sgn(d)} pm')}. (2) {F(f'{sgn(d)} ÷ {a} ≈ {fmt(r)}')}. (3) {F(f'{fmt(r)} × 100 = {sgn(p)}%')}. "
                    f"<b>الإجابة:</b> {res} بنحو {abs(p)}%.", lines=2))
    it.append(P("من نتائج هذا القسم: أيّ الأيونات أصبحت أكبر من ذرّاتها؟ وأيّها أصغر؟ وماذا تلاحظ؟",
                f"<b>أكبر:</b> {ion('O', -2)} و{ion('I', -1)} (أيونات سالبة اكتسبت إلكترونات). "
                f"<b>أصغر:</b> {ion('Na', 1)} و{ion('Al', 3)} و{ion('K', 1)} (أيونات موجبة فقدت إلكترونات). "
                "<b>الملاحظة:</b> في هذه الأمثلة، الأيون الموجب أصغر من ذرته، والأيون السالب أكبر.", lines=2))
    section("13. النسبة المئوية لتغيّر نصف القطر", "ص 69 و72 و77 (أرقام الكتاب)",
            "الخطوات: (1) اطرح: نصف قطر الأيون − نصف قطر الذرة. (2) اقسم الفرق على نصف قطر الذرة. (3) اضرب في 100. القيمة السالبة = انخفاض، والموجبة = زيادة.", it)


for f in (s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13):
    f()

# ---------- تحقّق آلي من بعض الحقائق المعروفة (يتوقف البناء إن اختلّت القواعد) ----------
assert [valence(s) for s in ("Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "He")] == [1, 2, 3, 4, 5, 6, 7, 8, 2]
assert shells("Na") == [2, 8, 1] and shells("Cl") == [2, 8, 7] and shells("K") == [2, 8, 8, 1]
assert [bonds(s) for s in ("Be", "C", "N", "Ar")] == [2, 4, 3, 0]          # الشكل 5
assert ion_change("N") == -3 and noble_like("Na") == "Ne" and noble_like("Cl") == "Ar"
assert [pct(*x)[2] for x in ((186, 102), (73, 140), (143, 54), (133, 216), (227, 133))] == [-45, 92, -62, 62, -41]


# ---------- الصفحة ----------
def font_faces():
    pkg = os.path.join(HERE, "fonts")
    css = []
    for sub, rng in (("arabic", "U+0600-06FF,U+0750-077F,U+08A0-08FF,U+FB50-FDFF,U+FE70-FEFF,U+200C-200E"),
                     ("latin", "U+0000-00FF,U+2000-206F,U+2070-209F,U+2190-21FF,U+2212,U+2248,U+00D7,U+00F7")):
        for w in (400, 700, 800):
            p = os.path.join(pkg, f"cairo-{sub}-{w}-normal.woff2")
            b = base64.b64encode(open(p, "rb").read()).decode()
            css.append(f"@font-face{{font-family:'Cairo';font-weight:{w};src:url(data:font/woff2;base64,{b}) format('woff2');unicode-range:{rng};}}")
    return "\n".join(css)


CSS = r"""
:root{--paper:#FFF9F0;--ink:#1E2430;--ink2:#4A5568;--rule:#E6DCCB;--ans:#0B7A43;--ans-bg:#EAF8F0;--ans-rule:#9BD8B7;--w:#7B2CBF;--w-bg:#F3E8FF;--ex:#1E66D0;--ex-bg:#EAF1FF}
*{box-sizing:border-box}
html,body{font-size:10pt}
body{margin:0;background:var(--paper);color:var(--ink);font-family:'Cairo',system-ui,sans-serif;line-height:1.75}
.wrap{max-width:900px;margin:0 auto;padding:18px 16px 60px}
.ltr{direction:ltr;unicode-bidi:isolate;display:inline-block}
header.cover{background:linear-gradient(135deg,#2BB673 0%,#1E88C8 55%,#6A3FD0 100%);color:#fff;border-radius:18px;padding:22px 24px}
header.cover h1{margin:0;font-size:1.9rem;font-weight:800;line-height:1.35}
header.cover p{margin:.35rem 0 0;font-size:1.05rem}
.toolbar{position:sticky;top:0;z-index:5;display:flex;gap:8px;justify-content:center;padding:8px 0;background:var(--paper)}
.toolbar button{font:inherit;font-weight:700;border:0;border-radius:999px;padding:6px 16px;background:#1E2430;color:#fff;cursor:pointer}
section.sec{margin-top:24px}
.sec-head{border-radius:14px;color:#fff;padding:10px 16px;margin-bottom:8px}
.sec-head h2{margin:0;font-size:1.3rem}
.sec-head span{opacity:.92}
.rule{background:#fff;border:2px dashed var(--rule);border-radius:12px;padding:8px 14px;margin-bottom:10px}
.p{background:#fff;border:1px solid var(--rule);border-inline-start:6px solid var(--ex);border-radius:12px;padding:10px 14px 12px;margin:10px 0;break-inside:avoid}
.p.worked{border-inline-start-color:var(--w);background:#FCF9FF}
.ph{display:flex;gap:6px;align-items:center;margin-bottom:4px}
.num{background:var(--ex);color:#fff;border-radius:8px;min-width:2.2em;text-align:center;font-weight:800;padding:0 6px}
.worked .num{background:var(--w)}
.badge{border-radius:999px;padding:0 10px;font-size:.85rem;font-weight:700;background:var(--ex-bg);color:var(--ex)}
.worked .badge{background:var(--w-bg);color:var(--w)}
.q{font-weight:700;font-size:1.02rem}
.space{margin-top:6px;background:repeating-linear-gradient(to bottom,transparent 0,transparent 1.55em,#C9D6E3 1.55em,#C9D6E3 calc(1.55em + 1px))}
.drawbox{margin-top:6px;height:96px;border:2px dashed #C9D6E3;border-radius:10px;display:flex;align-items:center;justify-content:center;color:#9AA9B6}
.ans{margin-top:8px;background:var(--ans-bg);border:1px solid var(--ans-rule);border-radius:10px;padding:6px 12px}
.ans .k{font-weight:800;color:var(--ans)}
svg.dot{vertical-align:middle;margin:0 6px;background:#fff;border-radius:8px}
svg.dot text{font-family:'Cairo',sans-serif;fill:#1E2430}
svg.dot circle{fill:#C62839}
body.hide .p:not(.worked) .ans{display:none}
body:not(.hide) .space,body:not(.hide) .drawbox{display:none}
@media print{@page{size:A4;margin:12mm 11mm} body{background:#fff} .toolbar{display:none} .wrap{padding:0;max-width:none}
 .sec-head,header.cover,.num,.badge,svg.dot circle{-webkit-print-color-adjust:exact;print-color-adjust:exact}}
"""
COLORS = ["#1E66D0", "#0A8F7E", "#E0661B", "#7B2CBF", "#C62839", "#2E7D32", "#1565C0", "#AD1457", "#6D4C41", "#00838F", "#5E35B1", "#EF6C00", "#37474F"]


def build_html():
    body, n, total, worked = [], 0, 0, 0
    for i, sec in enumerate(SECTIONS):
        c = COLORS[i % len(COLORS)]
        body.append(f'<section class="sec"><div class="sec-head" style="background:{c}"><h2>{sec["title"]}</h2><span>{sec["pages"]}</span></div>'
                    f'<div class="rule">💡 {sec["intro"]}</div>')
        for it in sec["items"]:
            n += 1; total += 1; worked += it["worked"]
            cls = "p worked" if it["worked"] else "p"
            badge = "مثال محلول" if it["worked"] else "تمرين"
            space = '<div class="drawbox">ارسم هنا</div>' if it["draw"] else f'<div class="space" style="height:{it["lines"] * 1.6 + .2}em"></div>'
            key = "✔ الحل خطوة بخطوة:" if it["worked"] else "✔ الحل:"
            body.append(f'<article class="{cls}"><div class="ph"><span class="num">{n}</span><span class="badge">{badge}</span></div>'
                        f'<div class="q">{it["q"]}</div>{"" if it["worked"] else space}'
                        f'<div class="ans"><span class="k">{key}</span> {it["a"]}</div></article>')
        body.append("</section>")
    print(f"تمارين: {total} (منها {worked} أمثلة محلولة) في {len(SECTIONS)} قسمًا")
    return f"""<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>تمارين الروابط الكيميائية</title><style>{font_faces()}{CSS}</style></head><body><div class="wrap">
<header class="cover"><h1>تمارين عملية محلولة: الوحدة 2 — العناصر والروابط الكيميائية</h1>
<p>{total - worked} تمرينًا و{worked} أمثلة محلولة في {len(SECTIONS)} مهارة من الكتاب: إلكترونات التكافؤ، والتمثيل النقطي، وشحنة الأيون، والصيغ، والروابط، والنسبة المئوية لتغيّر نصف القطر.</p></header>
<div class="toolbar"><button type="button" onclick="document.body.classList.toggle('hide');this.textContent=document.body.classList.contains('hide')?'إظهار الحلول':'إخفاء الحلول'">إخفاء الحلول</button>
<button type="button" onclick="window.print()">طباعة</button></div>
<p>في بداية كل قسم <b>مثال محلول</b> بالبنفسجي يشرح الطريقة، ثم <b>تمارين</b> بالأزرق تحت كل منها حلّه. اضغط «إخفاء الحلول» ليحلّ ابنك أولًا (تظهر سطور وأماكن للرسم)، ثم أظهر الحلول للتصحيح.</p>
{''.join(body)}</div></body></html>"""


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "..", "..", "unit2-practice-problems.html")
    open(out, "w", encoding="utf-8").write(build_html())
    print("كُتب:", os.path.abspath(out))
