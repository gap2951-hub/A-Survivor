"""Warrior_clothes_empty 베이스 프레임 추출 (장비 없는 상태)"""

from PIL import Image
import numpy as np
import os

BASE    = r"C:\Users\gap29\Desktop\A-Survivor\image\전사"
EMPTY   = os.path.join(BASE, "Warrior_animations", "Right_Side", "PNG Sequences", "Warrior_clothes_empty")
OUT_DIR = r"C:\Users\gap29\Desktop\A-Survivor\app\src\main\res\drawable"


def remove_white_bg(img: Image.Image, threshold: int = 240) -> Image.Image:
    img = img.convert("RGBA")
    arr = np.array(img, dtype=np.uint8)
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    arr[(r >= threshold) & (g >= threshold) & (b >= threshold), 3] = 0
    return Image.fromarray(arr, "RGBA")


def extract(anim_folder: str, prefix: str, total: int, indices: list):
    files = sorted([f for f in os.listdir(anim_folder) if f.lower().endswith(".png")])
    assert len(files) == total, f"{anim_folder}: 기대 {total}개, 실제 {len(files)}개"
    for out_idx, frame_idx in enumerate(indices):
        img = Image.open(os.path.join(anim_folder, files[frame_idx])).convert("RGBA")
        img = remove_white_bg(img)
        img = img.resize((256, 256), Image.LANCZOS)
        out = os.path.join(OUT_DIR, f"{prefix}_{out_idx}.png")
        img.save(out, "PNG")
        print(f"  저장: {out}")


print("=== Warrior_clothes_empty 베이스 프레임 추출 ===")
extract(os.path.join(EMPTY, "Idle"),     "warrior_base_idle",   30, [0, 5, 10, 15, 20, 25])
extract(os.path.join(EMPTY, "Walk"),     "warrior_base_walk",   30, [0, 5, 10, 15, 20, 25])
extract(os.path.join(EMPTY, "Attack_1"), "warrior_base_attack", 15, [0, 3, 6, 9, 12])
extract(os.path.join(EMPTY, "Hurt"),     "warrior_base_hurt",   15, [0, 3, 6, 9, 12])
extract(os.path.join(EMPTY, "Died"),     "warrior_base_die",    30, [0, 5, 10, 15, 20, 25])
print("완료!")
