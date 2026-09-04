# -*- coding: utf-8 -*-
"""بنك الأسئلة بعد التحقُّق وموازنة توزيع الإجابات.
يستورده كل مولِّد (ويب/طباعة) فتبقى الأرقام والحروف متطابقة في كل الصيغ."""
import sys, collections
from questions import SECTIONS

LETTERS = ["A", "B", "C", "D"]

# ---------- تحقُّق صارم قبل التوليد ----------
def validate():
    errs, seen = [], {}
    n = 0
    for code, title, _, qs in SECTIONS:
        for i, (q, opts, ci, exp) in enumerate(qs, 1):
            n += 1
            tag = f"{code}-{i} (سؤال {n})"
            if len(opts) != 4:
                errs.append(f"{tag}: عدد الخيارات {len(opts)} وليس 4")
            if not isinstance(ci, int) or not 0 <= ci <= 3:
                errs.append(f"{tag}: رقم الإجابة الصحيحة خارج المدى: {ci}")
            if len(set(o.strip() for o in opts)) != len(opts):
                errs.append(f"{tag}: خيارات مكرّرة")
            if not exp.strip():
                errs.append(f"{tag}: لا يوجد تفسير")
            key = q.strip()
            if key in seen:
                errs.append(f"{tag}: سؤال مكرّر مع {seen[key]}")
            seen[key] = tag
    return errs, n

# ---------- موازنة توزيع الإجابات (تدوير دائري يحفظ ترتيب الخيارات) ----------
def rebalance():
    import random
    total = sum(len(qs) for _, _, _, qs in SECTIONS)
    targets = [i % 4 for i in range(total)]
    random.Random(20260904).shuffle(targets)
    k = 0
    for si, (code, title, topics, qs) in enumerate(SECTIONS):
        new = []
        for q, opts, ci, exp in qs:
            r = (targets[k] - ci) % 4
            k += 1
            rot = opts[-r:] + opts[:-r] if r else list(opts)
            nci = (ci + r) % 4
            assert rot[nci] == opts[ci], "اختلّ التدوير"
            new.append((q, rot, nci, exp))
        SECTIONS[si] = (code, title, topics, new)

rebalance()

errs, TOTAL = validate()
if errs:
    print("\n".join(errs)); sys.exit(1)


def summary():
    dist = collections.Counter(ci for _, _, _, qs in SECTIONS for _, _, ci, _ in qs)
    print(f"عدد الأسئلة: {TOTAL}")
    print("توزيع الإجابات:", {LETTERS[k]: v for k, v in sorted(dist.items())})
    for code, title, _, qs in SECTIONS:
        print(f"  الدرس {code}: {len(qs)} سؤالًا")
