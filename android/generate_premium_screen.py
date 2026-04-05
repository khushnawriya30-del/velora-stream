"""Generate the full Premium Payment Screen PNG matching the provided design."""
from PIL import Image, ImageDraw, ImageFont
import os

RES = os.path.join(os.path.dirname(__file__), "app", "src", "main", "res")
NODPI = os.path.join(RES, "drawable-nodpi")
os.makedirs(NODPI, exist_ok=True)

# ── Screen dimensions (standard mobile) ──
W = 1080
H = 2160  # Full screen height

# ── Colors ──
BG_TOP = (11, 15, 26)
BG_BOT = (18, 24, 38)
CARD_BG = (22, 27, 46)
CARD_BORDER = (255, 255, 255, 15)
GOLD = (212, 175, 55)
GOLD_LIGHT = (245, 215, 110)
GOLD_TEXT = (245, 197, 24)
GOLD_DIM = (139, 115, 40)
TEXT_WHITE = (255, 255, 255)
TEXT_DIM = (255, 255, 255, 128)
TEXT_FAINT = (255, 255, 255, 90)
GREEN_BADGE = (34, 197, 94)
ICON_BG = (30, 34, 51)
ICON_BORDER = (139, 115, 40, 40)
ACTION_BG = (22, 27, 46)
BOTTOM_BG = (26, 28, 40)
PAY_GOLD_1 = (212, 175, 55)
PAY_GOLD_2 = (245, 197, 24)
PAY_GOLD_3 = (184, 150, 30)


def lerp(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(min(len(c1), len(c2))))


def draw_rounded_rect(draw, xy, r, fill=None, outline=None, width=1):
    x0, y0, x1, y1 = xy
    if fill:
        draw.rounded_rectangle(xy, r, fill=fill)
    if outline:
        draw.rounded_rectangle(xy, r, outline=outline, width=width)


def draw_gradient_rect(img, xy, r, c1, c2, alpha=255):
    x0, y0, x1, y1 = xy
    w, h = x1 - x0, y1 - y0
    overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    for y in range(h):
        t = y / max(h - 1, 1)
        c = lerp(c1, c2, t)
        od.line([(0, y), (w, y)], fill=(*c, alpha))
    mask = Image.new("L", (w, h), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([0, 0, w, h], r, fill=255)
    overlay.putalpha(mask)
    img.alpha_composite(overlay, (x0, y0))


def make_premium_screen():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    # ── Background gradient ──
    for y in range(H):
        t = y / H
        c = lerp(BG_TOP, BG_BOT, t)
        ImageDraw.Draw(img).line([(0, y), (W, y)], fill=(*c, 255))

    draw = ImageDraw.Draw(img)

    # ── Fonts ──
    try:
        font_icon_label = ImageFont.truetype("arial.ttf", 28)
        font_plan_label = ImageFont.truetype("arial.ttf", 34)
        font_price_sym = ImageFont.truetype("arialbd.ttf", 38)
        font_price = ImageFont.truetype("arialbd.ttf", 72)
        font_original = ImageFont.truetype("arial.ttf", 30)
        font_per_month = ImageFont.truetype("arial.ttf", 28)
        font_badge = ImageFont.truetype("arialbd.ttf", 22)
        font_discount = ImageFont.truetype("arialbd.ttf", 24)
        font_action = ImageFont.truetype("arial.ttf", 36)
        font_action_icon = ImageFont.truetype("arial.ttf", 40)
        font_bottom_price = ImageFont.truetype("arialbd.ttf", 56)
        font_bottom_plan = ImageFont.truetype("arial.ttf", 30)
        font_bottom_valid = ImageFont.truetype("arial.ttf", 26)
        font_pay = ImageFont.truetype("arialbd.ttf", 40)
    except:
        f = ImageFont.load_default()
        font_icon_label = font_plan_label = font_price_sym = font_price = f
        font_original = font_per_month = font_badge = font_discount = f
        font_action = font_action_icon = font_bottom_price = f
        font_bottom_plan = font_bottom_valid = font_pay = f

    Y = 40  # current Y position tracker

    # ══════════════════════════════════════════════
    # ── Benefits Icons Row ──
    # ══════════════════════════════════════════════
    benefits_y = Y
    benefits_h = 240
    benefits_box = [32, benefits_y, W - 32, benefits_y + benefits_h]

    # Benefits background
    draw_gradient_rect(img, benefits_box, 24, (30, 34, 51), (24, 28, 40))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle(benefits_box, 24, outline=(139, 115, 40, 25), width=1)

    benefits = [
        ("∞", "Unlimited"),
        ("★", "Premium\nFirst"),
        ("AD", "No Ads"),
        ("HD", "FHD\n1080P"),
        ("♛", "Premium\nExclusive"),
    ]

    icon_size = 100
    icon_y = benefits_y + 30
    spacing = W // 5
    for i, (icon_text, label) in enumerate(benefits):
        cx = spacing // 2 + i * spacing
        # Icon circle
        ix = cx - icon_size // 2
        iy = icon_y
        # Circle with gold tint bg
        circle_overlay = Image.new("RGBA", (icon_size, icon_size), (0, 0, 0, 0))
        cd = ImageDraw.Draw(circle_overlay)
        cd.ellipse([0, 0, icon_size, icon_size], fill=(40, 35, 20, 200))
        cd.ellipse([2, 2, icon_size - 2, icon_size - 2], outline=(212, 175, 55, 80), width=2)
        img.alpha_composite(circle_overlay, (ix, iy))
        draw = ImageDraw.Draw(img)

        # Icon text centered in circle
        icon_font = ImageFont.truetype("arialbd.ttf", 32) if icon_text not in ("∞", "♛") else ImageFont.truetype("arialbd.ttf", 40)
        try:
            pass
        except:
            icon_font = ImageFont.load_default()
        bbox = draw.textbbox((0, 0), icon_text, font=icon_font)
        tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
        draw.text((cx - tw // 2, iy + icon_size // 2 - th // 2 - 2), icon_text, fill=GOLD_TEXT, font=icon_font)

        # Label below
        label_y = iy + icon_size + 12
        for li, line in enumerate(label.split("\n")):
            bbox = draw.textbbox((0, 0), line, font=font_icon_label)
            tw = bbox[2] - bbox[0]
            draw.text((cx - tw // 2, label_y + li * 32), line, fill=(200, 200, 216, 180), font=font_icon_label)

    Y = benefits_y + benefits_h + 40

    # ══════════════════════════════════════════════
    # ── Plan Cards (2x2 Grid) ──
    # ══════════════════════════════════════════════
    plans = [
        {"label": "1 Month", "price": 159, "original": 182, "discount": 10, "badge": None, "months": 1},
        {"label": "3 Months", "price": 459, "original": 543, "discount": 15, "badge": "Most popular", "months": 3},
        {"label": "6 Months", "price": 829, "original": 1038, "discount": 20, "badge": None, "months": 6},
        {"label": "12 Months", "price": 1529, "original": 2042, "discount": 25, "badge": "Best Value", "months": 12},
    ]

    card_w = (W - 32 * 2 - 24) // 2  # padding 32 each side, 24 gap
    card_h = 320
    card_positions = []

    for idx, plan in enumerate(plans):
        row = idx // 2
        col = idx % 2
        cx = 32 + col * (card_w + 24)
        cy = Y + row * (card_h + 24)
        card_positions.append((cx, cy, cx + card_w, cy + card_h))

        # Card background
        draw_gradient_rect(img, (cx, cy, cx + card_w, cy + card_h), 20, CARD_BG, (18, 23, 38))
        draw = ImageDraw.Draw(img)

        # Card border (subtle)
        draw.rounded_rectangle([cx, cy, cx + card_w, cy + card_h], 20, outline=(255, 255, 255, 15), width=1)

        # Badge + Discount row
        inner_x = cx + 24
        inner_y = cy + 20

        if plan["badge"]:
            badge_color = GREEN_BADGE if plan["badge"] == "Most popular" else GOLD
            badge_text = plan["badge"]
            bbox = draw.textbbox((0, 0), badge_text, font=font_badge)
            bw = bbox[2] - bbox[0] + 16
            bh = bbox[3] - bbox[1] + 10
            draw.rounded_rectangle([inner_x, inner_y, inner_x + bw, inner_y + bh], 8, fill=badge_color)
            draw.text((inner_x + 8, inner_y + 3), badge_text, fill=TEXT_WHITE, font=font_badge)

        # Discount tag (right side)
        disc_text = f"{plan['discount']}% Off"
        bbox = draw.textbbox((0, 0), disc_text, font=font_discount)
        dw = bbox[2] - bbox[0] + 20
        dh = bbox[3] - bbox[1] + 12
        dx = cx + card_w - 24 - dw
        draw.rounded_rectangle([dx, inner_y, dx + dw, inner_y + dh], 8, fill=(255, 255, 255, 20))
        draw.text((dx + 10, inner_y + 4), disc_text, fill=GOLD_TEXT, font=font_discount)

        # Plan label
        label_y = inner_y + 55
        draw.text((inner_x, label_y), plan["label"], fill=(255, 255, 255, 200), font=font_plan_label)

        # Price row: ₹XXX  ₹original(strikethrough)
        price_y = label_y + 50
        draw.text((inner_x, price_y), "₹", fill=TEXT_WHITE, font=font_price_sym)
        # Large price number
        sym_bbox = draw.textbbox((0, 0), "₹", font=font_price_sym)
        price_x = inner_x + sym_bbox[2] - sym_bbox[0] + 4
        draw.text((price_x, price_y - 18), str(plan["price"]), fill=TEXT_WHITE, font=font_price)

        # Original price (strikethrough)
        price_bbox = draw.textbbox((0, 0), str(plan["price"]), font=font_price)
        orig_x = price_x + price_bbox[2] - price_bbox[0] + 14
        orig_text = f"₹{plan['original']}"
        draw.text((orig_x, price_y + 8), orig_text, fill=(255, 255, 255, 100), font=font_original)
        # Strikethrough line
        orig_bbox = draw.textbbox((orig_x, price_y + 8), orig_text, font=font_original)
        line_y = (orig_bbox[1] + orig_bbox[3]) // 2
        draw.line([(orig_x, line_y), (orig_bbox[2], line_y)], fill=(255, 255, 255, 100), width=2)

        # Per-month (for multi-month plans)
        if plan["months"] > 1:
            per_month = plan["price"] // plan["months"]
            pm_y = price_y + 70
            draw.text((inner_x, pm_y), f"₹{per_month}/month", fill=(245, 197, 24, 200), font=font_per_month)

    Y = card_positions[-1][3] + 50

    # ══════════════════════════════════════════════
    # ── Action Rows ──
    # ══════════════════════════════════════════════
    actions = [
        ("[M]", "Buy Premium Code"),
        ("[¥]", "Activate with Premium Code"),
        ("□", "Help Center"),
    ]

    action_h = 110
    for i, (icon, label) in enumerate(actions):
        ax = 32
        ay = Y + i * (action_h + 20)
        aw = W - 64

        # Action row background
        draw_gradient_rect(img, (ax, ay, ax + aw, ay + action_h), 18, ACTION_BG, (20, 24, 38))
        draw = ImageDraw.Draw(img)
        draw.rounded_rectangle([ax, ay, ax + aw, ay + action_h], 18, outline=(255, 255, 255, 15), width=1)

        # Icon
        draw.text((ax + 28, ay + 30), icon, fill=(255, 255, 255, 150), font=font_action_icon)

        # Label
        draw.text((ax + 100, ay + 34), label, fill=(255, 255, 255, 220), font=font_action)

        # Chevron right
        chev_x = ax + aw - 50
        chev_y = ay + action_h // 2
        draw.text((chev_x, chev_y - 18), ">", fill=(255, 255, 255, 80), font=font_action)

    Y = Y + 3 * (action_h + 20) + 20

    # Fill remaining space
    # (leave room for bottom bar which will be overlaid in Compose)

    # ══════════════════════════════════════════════
    # ── Bottom Pay Bar ──
    # ══════════════════════════════════════════════
    bottom_h = 200
    bottom_y = H - bottom_h

    # Bottom bar background
    draw_gradient_rect(img, (0, bottom_y, W, H), 0, (26, 28, 40), (20, 22, 31))
    draw = ImageDraw.Draw(img)

    # Top border line
    draw.line([(0, bottom_y), (W, bottom_y)], fill=(139, 115, 40, 50), width=1)

    # Price text (left side)
    draw.text((40, bottom_y + 24), "₹159", fill=TEXT_WHITE, font=font_bottom_price)
    draw.text((40, bottom_y + 90), "Selected Plan: 1 Month", fill=(255, 255, 255, 130), font=font_bottom_plan)
    draw.text((40, bottom_y + 130), "Valid until 06 May,2026", fill=(255, 255, 255, 90), font=font_bottom_valid)

    # PAY NOW button (right side)
    pay_x = W - 360
    pay_y = bottom_y + 40
    pay_w = 320
    pay_h = 110
    # Gold gradient button
    for py_offset in range(pay_h):
        t = py_offset / max(pay_h - 1, 1)
        c = lerp(PAY_GOLD_1, PAY_GOLD_2, t)
        draw.line([(pay_x, pay_y + py_offset), (pay_x + pay_w, pay_y + py_offset)], fill=(*c, 255))
    # Round the button corners with mask
    btn_overlay = Image.new("RGBA", (pay_w, pay_h), (0, 0, 0, 0))
    btn_d = ImageDraw.Draw(btn_overlay)
    for py_offset in range(pay_h):
        t = py_offset / max(pay_h - 1, 1)
        c = lerp(PAY_GOLD_1, PAY_GOLD_2, t)
        btn_d.line([(0, py_offset), (pay_w, py_offset)], fill=(*c, 255))
    btn_mask = Image.new("L", (pay_w, pay_h), 0)
    bmd = ImageDraw.Draw(btn_mask)
    bmd.rounded_rectangle([0, 0, pay_w, pay_h], 20, fill=255)
    btn_overlay.putalpha(btn_mask)

    # Clear the rectangular area first, then paste rounded button
    clear = Image.new("RGBA", (pay_w, pay_h), (0, 0, 0, 0))
    for py_offset in range(pay_h):
        t = py_offset / max(pay_h - 1, 1)
        c = lerp((26, 28, 40), (20, 22, 31), t)
        ImageDraw.Draw(clear).line([(0, py_offset), (pay_w, py_offset)], fill=(*c, 255))
    img.paste(clear, (pay_x, pay_y))
    img.alpha_composite(btn_overlay, (pay_x, pay_y))
    draw = ImageDraw.Draw(img)

    # PAY NOW text
    pay_text = "PAY NOW"
    bbox = draw.textbbox((0, 0), pay_text, font=font_pay)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    draw.text((pay_x + pay_w // 2 - tw // 2, pay_y + pay_h // 2 - th // 2 - 2), pay_text, fill=(26, 18, 0), font=font_pay)

    # ── Save ──
    out_path = os.path.join(NODPI, "premium_screen_bg.png")
    img.save(out_path)
    print(f"✓ Premium screen saved: {out_path}")
    print(f"  Size: {W}x{H}")

    # Also save card position data for reference
    print("\n── Card Positions (for clickable overlays) ──")
    for i, (x0, y0, x1, y1) in enumerate(card_positions):
        print(f"  Plan {i}: ({x0},{y0}) -> ({x1},{y1})  [{x1-x0}x{y1-y0}]")
    print(f"\n── Action Rows ──")
    for i in range(3):
        ay = Y - 20 - (3 - i) * (action_h + 20)
        print(f"  Action {i}: y={ay} h={action_h}")
    print(f"\n── Bottom Bar ──")
    print(f"  Y: {bottom_y}, H: {bottom_h}")
    print(f"  PAY button: ({pay_x},{pay_y}) -> ({pay_x+pay_w},{pay_y+pay_h})")


if __name__ == "__main__":
    make_premium_screen()
