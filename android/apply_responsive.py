"""
Bulk-apply responsive dimension replacements across all mobile app screen & component files.
Replaces hardcoded .dp and .sp values with LocalAppDimens.current.* properties.
"""
import re, os, glob

BASE = r"C:\Users\nawar\Downloads\VS Code ALL Projects\APP\android\app\src\main\java\com\cinevault\app"
IMPORT = "import com.cinevault.app.ui.theme.LocalAppDimens"

DIRS = [
    os.path.join(BASE, "ui", "screen"),
    os.path.join(BASE, "ui", "components"),
]

# Skip backup files
SKIP_SUFFIXES = [".bak", ".old"]

PAD_MAP = {
    2: 'padTiny', 4: 'pad4', 6: 'pad6', 8: 'pad8', 10: 'pad10',
    12: 'pad12', 14: 'pad14', 16: 'pad16', 20: 'pad20', 24: 'pad24', 32: 'pad32'
}

FONT_SIZES = [8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24, 26, 28, 32, 36, 48]
LINE_HEIGHTS = [16, 18, 20, 22, 24]
RADII = [4, 6, 8, 10, 16, 22]

D = "LocalAppDimens.current"

def process(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    orig = content

    # Skip if no dp/sp values
    if '.dp' not in content and '.sp' not in content:
        return False, 0

    # 1. Add import if not present
    if IMPORT not in content:
        last_import_pos = -1
        for m in re.finditer(r'^import .+$', content, re.MULTILINE):
            last_import_pos = m.end()
        if last_import_pos > 0:
            content = content[:last_import_pos] + '\n' + IMPORT + content[last_import_pos:]

    # 2. fontSize = X.sp
    for s in FONT_SIZES:
        content = content.replace(f'fontSize = {s}.sp', f'fontSize = {D}.font{s}')

    # 3. lineHeight = X.sp
    for h in LINE_HEIGHTS:
        content = content.replace(f'lineHeight = {h}.sp', f'lineHeight = {D}.lineHeight{h}')

    # 4. RoundedCornerShape(X.dp) — only for supported radii
    for r in RADII:
        content = content.replace(f'RoundedCornerShape({r}.dp)', f'RoundedCornerShape({D}.radius{r})')

    # 5. spacedBy(X.dp)
    for val, prop in PAD_MAP.items():
        content = content.replace(f'spacedBy({val}.dp)', f'spacedBy({D}.{prop})')

    # 6. strokeWidth = 3.dp
    content = content.replace('strokeWidth = 3.dp', f'strokeWidth = {D}.strokeWidth')

    # 7. .padding(X.dp) — uniform single-value padding (but NOT PaddingValues(X.dp))
    for val, prop in PAD_MAP.items():
        content = re.sub(
            rf'\.padding\({val}\.dp\)',
            f'.padding({D}.{prop})',
            content
        )

    # 8. Named padding parameters: horizontal/vertical/top/bottom/start/end = X.dp
    #    This handles .padding(horizontal = X.dp), PaddingValues(vertical = X.dp), etc.
    for val, prop in PAD_MAP.items():
        content = re.sub(
            rf'((?:horizontal|vertical|top|bottom|start|end)\s*=\s*){val}\.dp\b',
            lambda m, p=prop: f'{m.group(1)}{D}.{p}',
            content
        )

    # 9. PaddingValues(X.dp) — uniform
    for val, prop in PAD_MAP.items():
        content = re.sub(
            rf'PaddingValues\({val}\.dp\)',
            f'PaddingValues({D}.{prop})',
            content
        )

    # 10. Spacer heights/widths for small spacer values
    #     Spacer(modifier = Modifier.height(X.dp)) or Spacer(Modifier.height(X.dp))
    for val, prop in PAD_MAP.items():
        content = re.sub(
            rf'(Spacer\s*\(\s*(?:modifier\s*=\s*)?Modifier\s*\.\s*height\s*\(\s*){val}\.dp(\s*\))',
            lambda m, p=prop: f'{m.group(1)}{D}.{p}{m.group(2)}',
            content
        )
        content = re.sub(
            rf'(Spacer\s*\(\s*(?:modifier\s*=\s*)?Modifier\s*\.\s*width\s*\(\s*){val}\.dp(\s*\))',
            lambda m, p=prop: f'{m.group(1)}{D}.{p}{m.group(2)}',
            content
        )

    changes = 0
    # Count replacements
    for i, (o, n) in enumerate(zip(orig.splitlines(), content.splitlines())):
        if o != n:
            changes += 1
    # Also count added/removed lines
    changes += abs(len(content.splitlines()) - len(orig.splitlines()))

    if content != orig:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True, changes
    return False, 0


# Find all Kotlin files in target dirs
total_files = 0
total_changes = 0
results = []

for d in DIRS:
    if not os.path.isdir(d):
        continue
    for filepath in sorted(glob.glob(os.path.join(d, "**", "*.kt"), recursive=True)):
        # Skip backup files
        if any(filepath.endswith(s) for s in SKIP_SUFFIXES):
            continue
        changed, count = process(filepath)
        if changed:
            total_files += 1
            total_changes += count
            results.append(f"  {os.path.basename(filepath)}: ~{count} lines changed")

print(f"\n=== Responsive Dimensions Applied ===")
print(f"Files updated: {total_files}")
print(f"Approximate lines changed: {total_changes}")
print()
for r in results:
    print(r)
print()
print("Done!")
