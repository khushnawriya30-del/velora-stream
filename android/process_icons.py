from PIL import Image
import os

folder = r"c:\Users\nawar\Downloads\VS Code ALL Projects\APP\android\app\src\main\res\drawable-nodpi"
icons = [
    "ic_prem_unlimited.png",
    "ic_prem_fhd.png",
    "ic_prem_no_ads.png",
    "ic_prem_first.png",
    "ic_prem_exclusive.png",
]

for name in icons:
    path = os.path.join(folder, name)
    img = Image.open(path).convert("RGBA")
    w, h = img.size
    pixels = img.load()
    
    changed = 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            # Remove white/near-white background
            if r > 230 and g > 230 and b > 230:
                pixels[x, y] = (r, g, b, 0)
                changed += 1
    
    # Auto-crop
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
    
    # Resize to 256x256 for app icons (they're way too big at 1000+px)
    img = img.resize((256, 256), Image.LANCZOS)
    
    img.save(path)
    fw, fh = img.size
    fsize = os.path.getsize(path)
    print(f"{name}: removed {changed} white pixels, final {fw}x{fh}, {fsize:,} bytes")

print("\nDone! All icons processed.")
