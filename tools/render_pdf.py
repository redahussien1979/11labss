# -*- coding: utf-8 -*-
"""يحوِّل نسختَي الطباعة إلى PDF بمقاس A4 مع ترقيم الصفحات."""
from playwright.sync_api import sync_playwright
import pathlib

OUT = pathlib.Path("/home/user/11labss")
CHROME = "/opt/pw-browsers/chromium"

# (مصدر، وجهة، عنوان التذييل، لون، حجم الخط، الهامش الأفقي)
# ورقة الطالب تحتفظ بقيَم التذييل الأصلية حرفيًّا كي تبقى مطابقة للنسخة المُسلَّمة.
JOBS = [("chap1-mcq-print.html",    "chap1-mcq-solved.pdf",   "الطاقة الحرارية — أسئلة محلولة", "#0E6E88", "7.5pt", "13mm"),
        ("chap1-mcq-practice.html", "chap1-mcq-practice.pdf", "الطاقة الحرارية — ورقة أسئلة",  "#666",    "7pt",   "14mm")]

def foot(title, color, size, pad):
    return ('<div style="width:100%;font-size:' + size + ';color:' + color + ';padding:0 ' + pad + ';'
            'font-family:sans-serif;display:flex;justify-content:space-between;direction:rtl">'
            '<span>' + title + '</span>'
            '<span>صفحة <span class="pageNumber"></span> من <span class="totalPages"></span></span>'
            '</div>')

with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path=CHROME)
    pg = b.new_page()
    for src, dst, title, color, size, pad in JOBS:
        pg.goto((OUT / src).as_uri())
        pg.wait_for_load_state("networkidle")
        pg.evaluate("document.fonts.ready")
        pg.wait_for_timeout(1500)
        pg.pdf(path=str(OUT / dst), format="A4", print_background=True,
               display_header_footer=True,
               header_template='<div></div>',
               footer_template=foot(title, color, size, pad),
               margin={"top": "13mm", "bottom": "15mm", "left": "13mm", "right": "13mm"})
        print("تم:", dst)
    b.close()
