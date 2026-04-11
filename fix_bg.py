from PIL import Image
import numpy as np
from collections import deque

def remove_bg(filepath, brightness_threshold=180, defringe_passes=3, defringe_threshold=120):
    """
    Professional background removal:
    1. Convert RGB->RGBA
    2. Flood fill from ALL edges: any pixel with brightness > threshold = transparent
    3. Defringe: iteratively erode light pixels at the transparent/opaque boundary
    """
    img = Image.open(filepath).convert('RGBA')
    arr = np.array(img, dtype=np.uint8)
    h, w = arr.shape[:2]
    print(f"  Input: {w}x{h}, mode={img.mode}")
    
    # ── Step 1: Flood fill from all edges using AVERAGE BRIGHTNESS ──
    visited = np.zeros((h, w), dtype=bool)
    queue = deque()
    
    for x in range(w):
        queue.append((0, x)); visited[0, x] = True
        queue.append((h-1, x)); visited[h-1, x] = True
    for y in range(1, h-1):
        queue.append((y, 0)); visited[y, 0] = True
        queue.append((y, w-1)); visited[y, w-1] = True
    
    removed_flood = 0
    while queue:
        cy, cx = queue.popleft()
        r, g, b, a = int(arr[cy,cx,0]), int(arr[cy,cx,1]), int(arr[cy,cx,2]), int(arr[cy,cx,3])
        if a == 0:
            # Already transparent, expand neighbors
            for dy, dx in [(-1,0),(1,0),(0,-1),(0,1),(-1,-1),(-1,1),(1,-1),(1,1)]:
                ny, nx = cy + dy, cx + dx
                if 0 <= ny < h and 0 <= nx < w and not visited[ny, nx]:
                    visited[ny, nx] = True
                    queue.append((ny, nx))
        else:
            brightness = (r + g + b) / 3.0
            if brightness > brightness_threshold:
                arr[cy, cx, 3] = 0
                removed_flood += 1
                for dy, dx in [(-1,0),(1,0),(0,-1),(0,1),(-1,-1),(-1,1),(1,-1),(1,1)]:
                    ny, nx = cy + dy, cx + dx
                    if 0 <= ny < h and 0 <= nx < w and not visited[ny, nx]:
                        visited[ny, nx] = True
                        queue.append((ny, nx))
    
    print(f"  Flood fill (brightness>{brightness_threshold}): removed {removed_flood} pixels")
    
    # ── Step 2: Defringe - erode light pixels at boundary ──
    for i in range(defringe_passes):
        alpha = arr[:,:,3]
        trans = alpha == 0
        
        # Find opaque pixels adjacent to transparent (8-connected)
        adj = np.zeros_like(trans)
        adj[1:, :] |= trans[:-1, :]
        adj[:-1, :] |= trans[1:, :]
        adj[:, 1:] |= trans[:, :-1]
        adj[:, :-1] |= trans[:, 1:]
        adj[1:, 1:] |= trans[:-1, :-1]
        adj[1:, :-1] |= trans[:-1, 1:]
        adj[:-1, 1:] |= trans[1:, :-1]
        adj[:-1, :-1] |= trans[1:, 1:]
        
        br = (arr[:,:,0].astype(float) + arr[:,:,1].astype(float) + arr[:,:,2].astype(float)) / 3.0
        thresh = defringe_threshold + (defringe_passes - 1 - i) * 20
        to_remove = adj & (alpha > 0) & (br > thresh)
        count = np.sum(to_remove)
        arr[to_remove, 3] = 0
        print(f"  Defringe {i+1} (brightness>{thresh}): removed {count} pixels")
    
    Image.fromarray(arr).save(filepath, optimize=True)
    total_t = np.sum(arr[:,:,3] == 0)
    print(f"  Result: {total_t}/{h*w} transparent ({100*total_t/(h*w):.1f}%)")

# app_logo: sharp boundary at ~brightness 100. Use threshold=180 (very safe)
print("=== app_logo.png ===")
remove_bg(
    'android/app/src/main/res/drawable-nodpi/app_logo.png',
    brightness_threshold=180,
    defringe_passes=3,
    defringe_threshold=100
)

# how_much_other_earned: cream glow at 200-255, bubble body at ~138-192
# Use threshold=175 to catch ALL cream + glow, plus heavy defringe + auto-crop
print("\n=== how_much_other_earned.png ===")
remove_bg(
    'android/app/src/main/res/drawable-nodpi/how_much_other_earned.png',
    brightness_threshold=175,
    defringe_passes=8,
    defringe_threshold=90
)

# Auto-crop to content bounding box (remove transparent margins)
from PIL import Image
import numpy as np
img = Image.open('android/app/src/main/res/drawable-nodpi/how_much_other_earned.png')
arr = np.array(img)
alpha = arr[:,:,3]
rows = np.any(alpha > 0, axis=1)
cols = np.any(alpha > 0, axis=0)
if np.any(rows) and np.any(cols):
    rmin, rmax = np.where(rows)[0][[0, -1]]
    cmin, cmax = np.where(cols)[0][[0, -1]]
    # Tight crop with 2px padding
    rmin = max(0, rmin - 2)
    rmax = min(arr.shape[0]-1, rmax + 2)
    cmin = max(0, cmin - 2)
    cmax = min(arr.shape[1]-1, cmax + 2)
    cropped = arr[rmin:rmax+1, cmin:cmax+1]
    Image.fromarray(cropped).save('android/app/src/main/res/drawable-nodpi/how_much_other_earned.png', optimize=True)
    print(f"  Auto-cropped: {arr.shape[1]}x{arr.shape[0]} -> {cropped.shape[1]}x{cropped.shape[0]}")

print("\nAll done!")
