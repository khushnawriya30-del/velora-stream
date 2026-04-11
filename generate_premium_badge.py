"""Generate premium crown badge PNG with transparent background."""
from PIL import Image, ImageDraw, ImageFont
import math

SIZE = 256
img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Crown colors
GOLD = (255, 193, 7, 255)       # Primary gold
GOLD_DARK = (218, 165, 32, 255) # Darker gold for shading
GOLD_LIGHT = (255, 223, 100, 255) # Lighter gold for highlights

# Draw crown base (rounded rectangle at bottom)
base_top = 155
base_bottom = 195
base_left = 48
base_right = 208
draw.rounded_rectangle([base_left, base_top, base_right, base_bottom], radius=8, fill=GOLD)
# Highlight strip on base
draw.rounded_rectangle([base_left+4, base_top+4, base_right-4, base_top+14], radius=4, fill=GOLD_LIGHT)

# Crown body - 5 points
crown_bottom = 160
crown_mid = 95
crown_top = 40

# Crown polygon points (5 peaks)
points = [
    (base_left, crown_bottom),      # Bottom-left
    (45, crown_mid),                # Left inner
    (55, crown_top + 15),           # Left peak
    (80, crown_mid + 10),           # Left-inner valley
    (105, crown_top),               # Left-center peak
    (128, crown_mid),               # Center valley
    (151, crown_top),               # Right-center peak
    (176, crown_mid + 10),          # Right-inner valley
    (201, crown_top + 15),          # Right peak
    (211, crown_mid),               # Right inner
    (base_right, crown_bottom),     # Bottom-right
]

draw.polygon(points, fill=GOLD)

# Add highlight to crown body
highlight_points = [
    (base_left + 10, crown_bottom - 5),
    (55, crown_mid + 5),
    (60, crown_top + 25),
    (85, crown_mid + 15),
    (108, crown_top + 10),
    (128, crown_mid + 5),
    (148, crown_top + 10),
    (171, crown_mid + 15),
    (196, crown_top + 25),
    (201, crown_mid + 5),
    (base_right - 10, crown_bottom - 5),
]
draw.polygon(highlight_points, fill=GOLD_LIGHT)

# Jewels on crown peaks (small circles)
jewel_positions = [(55, crown_top + 15), (105, crown_top), (151, crown_top), (201, crown_top + 15)]
jewel_r = 8
for jx, jy in jewel_positions:
    draw.ellipse([jx - jewel_r, jy - jewel_r, jx + jewel_r, jy + jewel_r], fill=(255, 255, 255, 220))
    # Inner jewel
    inner_r = 5
    draw.ellipse([jx - inner_r, jy - inner_r, jx + inner_r, jy + inner_r], fill=GOLD_LIGHT)

# Center jewel (larger, with V concept)
cx, cy = 128, crown_top + 3
draw.ellipse([cx - 12, cy - 12, cx + 12, cy + 12], fill=(255, 255, 255, 240))
draw.ellipse([cx - 9, cy - 9, cx + 9, cy + 9], fill=GOLD)

# Draw "V" letter on base
try:
    font = ImageFont.truetype("arial.ttf", 28)
except:
    font = ImageFont.load_default()

# V on the crown base
v_x, v_y = 128, 165
draw.text((v_x, v_y), "V", fill=(40, 40, 40, 255), font=font, anchor="mm")

# Add subtle outer glow by creating a slightly larger version
# Save at multiple sizes
badge_128 = img.resize((128, 128), Image.LANCZOS)
badge_64 = img.resize((64, 64), Image.LANCZOS)

# Save the main badge
output_path = r"android\app\src\main\res\drawable-nodpi\premium_badge.png"
badge_128.save(output_path, 'PNG')

# Also save a small version for card overlay
badge_small_path = r"android\app\src\main\res\drawable-nodpi\premium_badge_small.png"
badge_64.save(badge_small_path, 'PNG')

print(f"Created {output_path} (128x128)")
print(f"Created {badge_small_path} (64x64)")
