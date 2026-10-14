# -*- coding: utf-8 -*-
"""يقصّ أشكال الوحدة 2 (العناصر والروابط الكيميائية) من ملف الكتاب نفسه.
الإحداثيات (الصفحة، يسار، أعلى، يمين، أسفل) مقيسة على صفحة الكتاب بدقتها الأصلية 72 dpi."""
import base64, io, os, subprocess, sys, tempfile
from PIL import Image

PDF = os.path.join(os.path.dirname(__file__), "..", "..", "electorics unit.pdf")

BOXES = {
    "ptable":     (6, 225, 130, 1085, 475),
    "lithium":    (6, 205, 1000, 510, 1410),
    "fluorine":   (7, 255, 960, 1260, 1570),
    "fig4":       (8, 130, 880, 1160, 1595),
    "fig5":       (9, 250, 140, 1262, 815),
    "fig6":       (10, 120, 1150, 1185, 1585),
    "vsum1":      (11, 350, 340, 1200, 725),
    "mgcl":       (12, 170, 355, 550, 535),
    "fig7":       (16, 150, 140, 1140, 475),
    "fig8":       (17, 400, 1110, 1262, 1580),
    "fig9":       (18, 160, 1200, 1140, 1585),
    "fig10":      (19, 250, 1200, 1252, 1580),
    "cl_dot":     (21, 455, 395, 560, 495),
    "salt_lake":  (23, 70, 830, 1350, 1600),
    "fig11":      (26, 150, 1050, 1150, 1570),
    "fig12":      (27, 280, 160, 1242, 520),
    "fig13":      (28, 130, 785, 548, 995),
    "table1":     (29, 255, 645, 1262, 1590),
    "vsum3":      (30, 240, 360, 1090, 740),
    "concept":    (33, 130, 840, 1160, 1560),
    "boron":      (34, 1100, 578, 1260, 830),
    "be_atom":    (34, 960, 1100, 1120, 1220),
    "he_atom":    (34, 400, 790, 580, 910),
    "n3":         (35, 720, 540, 1100, 630),
    "k_atom":     (36, 815, 600, 1010, 780),
    "nacl_opts":  (36, 150, 300, 640, 1400),
    "water4":     (37, 780, 505, 1060, 745),
    "sip":        (37, 220, 780, 510, 990),
    "rust":       (37, 135, 355, 622, 558),
    "boat":       (3, 600, 700, 1050, 1350),
    "puzzle":     (13, 60, 300, 830, 1000),
}

def build(dump_dir=None):
    out = {}
    with tempfile.TemporaryDirectory() as tmp:
        for p in sorted({b[0] for b in BOXES.values()}):
            subprocess.run(["pdftoppm", "-r", "72", "-png", "-f", str(p), "-l", str(p),
                            "-singlefile", PDF, os.path.join(tmp, f"p{p}")], check=True)
        for name, (p, l, t, r, b) in BOXES.items():
            crop = Image.open(os.path.join(tmp, f"p{p}.png")).convert("RGB").crop((l, t, r, b))
            if crop.width < 400:   # أشكال صغيرة: تكبير بسيط لوضوح النقاط
                f = 400 / crop.width
                crop = crop.resize((400, round(crop.height * f)), Image.LANCZOS)
            buf = io.BytesIO()
            crop.save(buf, "JPEG", quality=84, optimize=True)
            out[name] = "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode()
            if dump_dir:
                crop.save(os.path.join(dump_dir, name + ".jpg"), "JPEG", quality=84)
    return out

if __name__ == "__main__":
    figs = build(sys.argv[1] if len(sys.argv) > 1 else None)
    print(len(figs), "figures,", sum(len(v) for v in figs.values()) // 1024, "KB")
