# -*- coding: utf-8 -*-
"""Crops the figures used in the Unit 2 exam straight from the textbook PDF.
Boxes are (page, left, top, right, bottom) measured on a 110-dpi render."""
import base64, io, os, subprocess, sys, tempfile
from PIL import Image

PDF = os.path.join(os.path.dirname(__file__), "..", "..", "unit 2 english 6.pdf")
DPI = 150
SCALE = DPI / 110

BOXES = {
    "arrow_straight": (1, 405, 742, 478, 822),
    "arrow_left":     (1, 505, 742, 592, 822),
    "arrow_right":    (1, 600, 742, 690, 822),
    "arrow_past":     (1, 700, 760, 795, 815),
    "listening_tip":  (1, 352, 690, 830, 978),
    "zoo_map":        (2, 165, 245, 905, 860),
    "schedule":       (2, 95, 960, 445, 1200),
    "word_cloud":     (3, 620, 390, 830, 512),
    "imperative_tip": (3, 108, 870, 775, 1110),
    "bear":           (5, 455, 275, 682, 502),
    "hyena":          (5, 225, 620, 452, 848),
    "giraffe":        (5, 403, 880, 632, 1112),
    "fact_file":      (7, 80, 278, 795, 532),
    "bee_flower":     (9, 472, 215, 692, 458),
    "honeycomb":      (9, 607, 302, 913, 542),
    "beekeeper":      (9, 467, 512, 792, 772),
    "falcon_parts":   (13, 78, 280, 840, 592),
    "hooded_falcon":  (13, 495, 750, 892, 1092),
    "mind_falcon":    (16, 372, 448, 538, 592),
    "service_dog":    (17, 640, 232, 913, 768),
    "eye_chart":      (18, 598, 218, 913, 842),
    "rev_horse":      (21, 78, 293, 312, 527),
    "rev_tiger":      (21, 342, 293, 576, 527),
    "rev_gorilla":    (21, 602, 293, 838, 527),
    "rev_arrows":     (22, 350, 255, 425, 470),
    "hyena_game":     (22, 495, 695, 860, 872),
}

def pages_needed():
    return sorted({b[0] for b in BOXES.values()})

def build():
    out = {}
    with tempfile.TemporaryDirectory() as tmp:
        for p in pages_needed():
            subprocess.run(["pdftoppm", "-r", str(DPI), "-png", "-f", str(p), "-l", str(p),
                            "-singlefile", PDF, os.path.join(tmp, f"p{p}")], check=True)
        for name, (p, l, t, r, b) in BOXES.items():
            im = Image.open(os.path.join(tmp, f"p{p}.png")).convert("RGB")
            crop = im.crop(tuple(round(v * SCALE) for v in (l, t, r, b)))
            buf = io.BytesIO()
            crop.save(buf, "JPEG", quality=82, optimize=True)
            out[name] = "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode()
            if len(sys.argv) > 1:
                crop.save(os.path.join(sys.argv[1], name + ".jpg"), "JPEG", quality=82)
    return out

if __name__ == "__main__":
    figs = build()
    print(len(figs), "figures,", sum(len(v) for v in figs.values()) // 1024, "KB")
