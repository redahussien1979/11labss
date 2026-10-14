# -*- coding: utf-8 -*-
"""أدوات بناء بنك الأسئلة.
p    : رقم صفحة الكتاب التي يستند إليها السؤال.
book : True إذا كان السؤال منقولًا من الكتاب نفسه (يُحفظ ترتيب خياراته كما في الكتاب).
fig  : اسم الشكل المقصوص من الكتاب (انظر figures.py)."""


def L(s):
    """نص لاتيني/رياضي يُعرض من اليسار إلى اليمين داخل جملة عربية."""
    return f'<span class="ltr" dir="ltr">{s}</span>'


def M(p, text, opts, ans, exp, fig=None, book=False):
    """اختيار من متعدد. ans: رقم الخيار الصحيح بدءًا من 0، أو قائمة إذا قبل الكتاب أكثر من إجابة."""
    return dict(kind="mcq", p=p, text=text, opts=opts, ans=ans, exp=exp, fig=fig, book=book)


def T(p, text, ans, exp, fig=None, book=False):
    """صح أم خطأ. ans: True أو False. exp: الشرح (والتصحيح إذا كانت العبارة خطأ)."""
    return dict(kind="tf", p=p, text=text, ans=ans, exp=exp, fig=fig, book=book)


def W(p, text, ans, fig=None, book=False):
    """سؤال كتابي. ans: الإجابة النموذجية مشروحة ببساطة."""
    return dict(kind="wr", p=p, text=text, ans=ans, fig=fig, book=book)
