# -*- coding: utf-8 -*-
"""يحوِّل نسختَي الطباعة إلى PDF بمقاس A4 مع ترقيم الصفحات."""
from playwright.sync_api import sync_playwright
import pathlib

OUT = pathlib.Path("/home/user/11labss")
CHROME = "/opt/pw-browsers/chromium"

JOBS = [("chap1-mcq-print.html",    "chap1-mcq-solved.pdf",   "الطاقة الحرارية — أسئلة محلولة"),
        ("chap1-mcq-practice.html", "chap1-mcq-practice.pdf", "الطاقة الحرارية — ورقة أسئلة")]

FOOT = ('<div style="width:100%;font-size:7pt;color:#666;padding:0 14mm;'
        'font-family:sans-serif;display:flex;justify-content:space-between;direction:rtl">'
        '<span>{title}</span>'
        '<span>صفحة <span class="pageNumber"></span> من <span class="totalPages"></span></span>'
        '</div>')

with sync_playwright() as pw:
    b = pw.chromium.launch(executable_path=CHROME)
    pg = b.new_page()
    for src, dst, title in JOBS:
        pg.goto((OUT / src).as_uri())
        pg.wait_for_load_state("networkidle")
        pg.evaluate("document.fonts.ready")
        pg.wait_for_timeout(1500)
        pg.pdf(path=str(OUT / dst), format="A4", print_background=True,
               display_header_footer=True,
               header_template='<div></div>',
               footer_template=FOOT.format(title=title),
               margin={"top": "13mm", "bottom": "15mm", "left": "13mm", "right": "13mm"})
        print("تم:", dst)
    b.close()
