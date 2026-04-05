"""Ultra-Premium Dark Grey + Gold Subscription Screen Generator.

Generates a 1080x2400 background PNG with:
- Dark grey (#121212 → #1A1A1A) gradient background
- Gold (#D4AF37) accent colors throughout
- Embedded actual PNG icons from drawable-nodpi
- 2×2 plan grid, 3 action rows, bottom pay bar
"""
from PIL import Image, ImageDraw, ImageFont
import os

RES = os.path.join(os.path.dirname(__file__), "app", "src", "main", "res")
NODPI = os.path.join(RES, "drawable-nodpi")
os.makedirs(NODPI, exist_ok=True)

# ── Screen ──
W = 1080
H = 2400

# ── Dark Grey + Gold Palette ──
BG_TOP        = (18, 18, 18)      # #121212
BG_BOT        = (26, 26, 26)      # #1A1A1A
CARD_BG_T     = (31, 31, 31)      # #1F1F1F
CARD_BG_B     = (36, 36, 36)      # #242424
GOLD          = (212, 175, 55)    # #D4AF37
GOLD_SEC      = (201, 162, 39)    # #C9A227
GOLD_SOFT     = (230, 197, 90)    # #E6C55A
GOLD_DIM      = (160, 130, 40)
WHITE         = (255, 255, 255)
WHITE_87      = (222, 222, 222)
WHITE_60      = (153, 153, 153)
WHITE_38      = (97, 97, 97)
GREEN_BADGE   = (34, 197, 94)
TEXT_DARK     = (18, 12, 0)
BAR_TOP       = (22, 22, 22)
BAR_BOT       = (18, 18, 18)


def lerp(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(min(len(c1), len(c2))))


def rounded_mask(w, h, r):
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, w, h], r, fill=255)
    return m


def draw_card(img, x, y, w, h, r=20):
    """Dark gradient card with subtle white border."""
    card = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for row in range(h):
        t = row / max(h - 1, 1)
        c = lerp(CARD_BG_T, CARD_BG_B, t)
        ImageDraw.Draw(card).line([(0, row), (w, row)], fill=(*c, 255))
    card.putalpha(rounded_mask(w, h, r))
    img.alpha_composite(card, (x, y))
    border = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle([0, 0, w - 1, h - 1], r, outline=(255, 255, 255, 28), width=1)
    img.alpha_composite(border, (x, y))


def draw_gold_btn(img, x, y, w, h, r=20):
    """Gold gradient rounded button."""
    btn = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for row in range(h):
        t = row / max(h - 1, 1)
        c = lerp(GOLD_SOFT, GOLD_SEC, t)
        ImageDraw.Draw(btn).line([(0, row), (w, row)], fill=(*c, 255))
    btn.putalpha(rounded_mask(w, h, r))
    img.alpha_composite(btn, (x, y))


def font(name, size):
    try:
        return ImageFont.truetype(name, size)
    except:
        return ImageFont.load_default()


def make_screen():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    # ── Full dark-grey gradient background ──
    for y in range(H):
        c = lerp(BG_TOP, BG_BOT, y / H)
        ImageDraw.Draw(img).line([(0, y), (W, y)], fill=(*c, 255))

    # ── Subtle gold glow at top ──
    for y in range(220):
        a = int(14 * (1 - y / 220))
        ImageDraw.Draw(img).line([(0, y), (W, y)], fill=(*GOLD_DIM, a))

    draw = ImageDraw.Draw(img)

    # ── Fonts ──
    f_icon_lbl   = font("arial.ttf", 26)
    f_plan_lbl   = font("arial.ttf", 34)
    f_rupee      = font("arialbd.ttf", 40)
    f_price_lg   = font("arialbd.ttf", 76)
    f_price_sm   = font("arialbd.ttf", 64)
    f_original   = font("arial.ttf", 30)
    f_per_mo     = font("arial.ttf", 28)
    f_badge      = font("arialbd.ttf", 22)
    f_discount   = font("arialbd.ttf", 24)
    f_act_lbl    = font("arial.ttf", 36)
    f_act_icon   = font("arialbd.ttf", 44)
    f_bot_price  = font("arialbd.ttf", 56)
    f_bot_plan   = font("arial.ttf", 30)
    f_bot_valid  = font("arial.ttf", 26)
    f_pay        = font("arialbd.ttf", 40)

    CY = 50  # running Y cursor

    # ══════════════════════════════════════
    # ── Benefits Icon Row (real PNGs) ──
    # ══════════════════════════════════════
    ben_h = 260
    pad = 32
    draw_card(img, pad, CY, W - pad * 2, ben_h, r=24)

    icons = [
        ("ic_prem_unlimited.png", "Unlimited"),
        ("ic_prem_first.png", "Premium\nFirst"),
        ("ic_prem_no_ads.png", "No Ads"),
        ("ic_prem_fhd.png", "FHD\n1080P"),
        ("ic_prem_exclusive.png", "Premium\nExclusive"),
    ]
    ico_sz = 96
    col_w = (W - pad * 2) // 5
    ico_y = CY + 30

    for i, (fname, label) in enumerate(icons):
        cx = pad + col_w // 2 + i * col_w
        ix, iy = cx - ico_sz // 2, ico_y

        path = os.path.join(NODPI, fname)
        if os.path.exists(path):
            ic = Image.open(path).convert("RGBA").resize((ico_sz, ico_sz), Image.LANCZOS)
            img.alpha_composite(ic, (ix, iy))
        else:
            ImageDraw.Draw(img).ellipse([ix, iy, ix + ico_sz, iy + ico_sz],
                                         outline=(*GOLD_DIM, 100), width=2)

        draw = ImageDraw.Draw(img)
        ly = iy + ico_sz + 12
        for li, line in enumerate(label.split("\n")):
            bb = draw.textbbox((0, 0), line, font=f_icon_lbl)
            draw.text((cx - (bb[2] - bb[0]) // 2, ly + li * 30),
                      line, fill=WHITE_60, font=f_icon_lbl)

    CY += ben_h + 40

    # ══════════════════════════════════════
    # ── Plan Cards (2×2) ──
    # ══════════════════════════════════════
    plans = [
        {"name": "1 Month",   "price": 159,  "orig": 182,  "disc": 10, "badge": None,           "mo": 1},
        {"name": "3 Months",  "price": 459,  "orig": 543,  "disc": 15, "badge": "Most popular", "mo": 3},
        {"name": "6 Months",  "price": 829,  "orig": 1038, "disc": 20, "badge": None,           "mo": 6},
        {"name": "12 Months", "price": 1529, "orig": 2042, "disc": 25, "badge": "Best Value",   "mo": 12},
    ]
    gap = 24
    cw = (W - pad * 2 - gap) // 2
    ch = 340
    card_pos = []

    for idx, p in enumerate(plans):
        r, c = idx // 2, idx % 2
        cx = pad + c * (cw + gap)
        cy = CY + r * (ch + gap)
        card_pos.append((cx, cy, cx + cw, cy + ch))

        draw_card(img, cx, cy, cw, ch, r=20)
        draw = ImageDraw.Draw(img)

        ix = cx + 28
        iy = cy + 22

        # Badge
        if p["badge"]:
            bc = GREEN_BADGE if p["badge"] == "Most popular" else GOLD
            bt = p["badge"]
            bb = draw.textbbox((0, 0), bt, font=f_badge)
            bw, bh = bb[2] - bb[0] + 18, bb[3] - bb[1] + 12
            pill = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
            pd = ImageDraw.Draw(pill)
            pd.rounded_rectangle([0, 0, bw, bh], 8, fill=bc)
            pd.text((9, 4), bt, fill=WHITE, font=f_badge)
            img.alpha_composite(pill, (ix, iy))
            draw = ImageDraw.Draw(img)

        # Discount (top-right)
        dt = f"{p['disc']}% Off"
        db = draw.textbbox((0, 0), dt, font=f_discount)
        dw, dh = db[2] - db[0] + 22, db[3] - db[1] + 14
        dx = cx + cw - 28 - dw
        dp = Image.new("RGBA", (dw, dh), (0, 0, 0, 0))
        ImageDraw.Draw(dp).rounded_rectangle([0, 0, dw, dh], 8, fill=(255, 255, 255, 18))
        ImageDraw.Draw(dp).text((11, 5), dt, fill=GOLD, font=f_discount)
        img.alpha_composite(dp, (dx, iy))
        draw = ImageDraw.Draw(img)

        # Plan name
        ny = iy + 58
        draw.text((ix, ny), p["name"], fill=WHITE_87, font=f_plan_lbl)

        # Price (gold)
        py = ny + 52
        draw.text((ix, py + 4), "₹", fill=GOLD, font=f_rupee)
        rb = draw.textbbox((0, 0), "₹", font=f_rupee)
        px = ix + rb[2] - rb[0] + 2
        pf = f_price_lg if p["price"] < 1000 else f_price_sm
        draw.text((px, py - 12), str(p["price"]), fill=GOLD, font=pf)

        # Original (strikethrough)
        pb = draw.textbbox((0, 0), str(p["price"]), font=pf)
        ox = px + pb[2] - pb[0] + 16
        ot = f"₹{p['orig']}"
        draw.text((ox, py + 10), ot, fill=WHITE_38, font=f_original)
        ob = draw.textbbox((ox, py + 10), ot, font=f_original)
        sly = (ob[1] + ob[3]) // 2
        draw.line([(ox, sly), (ob[2], sly)], fill=WHITE_38, width=2)

        # Per month
        if p["mo"] > 1:
            pm = p["price"] // p["mo"]
            draw.text((ix, py + 78), f"₹{pm}/month", fill=GOLD_SOFT, font=f_per_mo)

    CY = card_pos[-1][3] + 50

    # ══════════════════════════════════════
    # ── Action Rows ──
    # ══════════════════════════════════════
    actions = [
        ("[M]", "Buy Premium Code"),
        ("[¥]", "Activate with Premium Code"),
        ("□", "Help Center"),
    ]
    ah = 110
    aw = W - pad * 2
    act_pos = []

    for i, (ic, lbl) in enumerate(actions):
        ax = pad
        ay = CY + i * (ah + 20)
        act_pos.append((ax, ay, ax + aw, ay + ah))

        draw_card(img, ax, ay, aw, ah, r=18)
        draw = ImageDraw.Draw(img)
        draw.text((ax + 30, ay + 28), ic, fill=GOLD, font=f_act_icon)
        draw.text((ax + 110, ay + 34), lbl, fill=WHITE_87, font=f_act_lbl)
        draw.text((ax + aw - 55, ay + 32), ">", fill=GOLD_DIM, font=f_act_lbl)

    CY = act_pos[-1][3] + 40

    # ══════════════════════════════════════
    # ── Bottom Pay Bar ──
    # ══════════════════════════════════════
    bot_h = 220
    bot_y = H - bot_h

    bar = Image.new("RGBA", (W, bot_h), (0, 0, 0, 0))
    for row in range(bot_h):
        c = lerp(BAR_TOP, BAR_BOT, row / max(bot_h - 1, 1))
        ImageDraw.Draw(bar).line([(0, row), (W, row)], fill=(*c, 255))
    img.alpha_composite(bar, (0, bot_y))
    draw = ImageDraw.Draw(img)

    # Gold separator
    draw.line([(0, bot_y), (W, bot_y)], fill=(*GOLD_DIM, 50), width=2)

    # Price & info (left)
    draw.text((44, bot_y + 28), "₹159", fill=GOLD, font=f_bot_price)
    draw.text((44, bot_y + 100), "Selected Plan: 1 Month", fill=WHITE_60, font=f_bot_plan)
    draw.text((44, bot_y + 142), "Valid until 06 May,2026", fill=WHITE_38, font=f_bot_valid)

    # PAY NOW gold button (right)
    pay_w, pay_h = 320, 115
    pay_x = W - 44 - pay_w
    pay_y = bot_y + 46
    draw_gold_btn(img, pay_x, pay_y, pay_w, pay_h, r=22)
    draw = ImageDraw.Draw(img)
    pt = "PAY NOW"
    tb = draw.textbbox((0, 0), pt, font=f_pay)
    tw, th = tb[2] - tb[0], tb[3] - tb[1]
    draw.text((pay_x + pay_w // 2 - tw // 2, pay_y + pay_h // 2 - th // 2 - 2),
              pt, fill=TEXT_DARK, font=f_pay)

    # ── Save ──
    out = os.path.join(NODPI, "premium_screen_bg.png")
    img.save(out, optimize=True)
    print(f"✓ Ultra-Premium screen saved: {out}")
    print(f"  Size: {W}x{H}")

    print("\n── Plan Card Positions ──")
    for i, (x0, y0, x1, y1) in enumerate(card_pos):
        print(f"  Plan {i} ({plans[i]['name']}): ({x0},{y0}) -> ({x1},{y1})")

    print("\n── Action Row Positions ──")
    for i, (x0, y0, x1, y1) in enumerate(act_pos):
        print(f"  Action {i} ({actions[i][1]}): ({x0},{y0}) -> ({x1},{y1})")

    print(f"\n── Bottom Bar ──")
    print(f"  Y: {bot_y}  H: {bot_h}")
    print(f"  PAY: ({pay_x},{pay_y}) -> ({pay_x+pay_w},{pay_y+pay_h})")


if __name__ == "__main__":
    make_screen()
