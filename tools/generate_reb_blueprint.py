#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор моделей строительных макетов uncomplicated-fpv (reb / reb_mini).

Дизайн (с 06.08.2026): макеты РЭБ — простые полупрозрачные боксы без текстур,
как у остальных построек (стены/колючка): зелёный для валидного макета,
красный для invalid. Никакой развёртки UV из geo.json — юзер решил, что
настоящая текстура РЭБ на макете не нужна.

Модели: reb_construction(.json/_invalid.json) — куб 16^3,
reb_mini_construction(.json/_invalid.json) — куб 16x13.2x16 (высота мини-РЭБ).
Текстуры: однотонные 64x64 RGBA (зелёный (87,182,7) / красный (237,0,0),
alpha 140 — как у wall_ghost/wall_invalid), генерируются чистым Python
(zlib + struct), pythonnet НЕ нужен.

Запуск: py -3 tools/generate_reb_blueprint.py
"""
import json
import os
import struct
import zlib

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(BASE, "pwp-warfare", "src", "main", "resources", "assets", "pwpwarfare")
OUT_MODELS = os.path.join(ASSETS, "models", "block")
OUT_TEXTURES = os.path.join(ASSETS, "textures", "block")

GREEN = (87, 182, 7, 140)
RED = (237, 0, 0, 140)


def write_png(path, size, rgba):
    """Однотонная PNG (прозрачность из альфа-канала), без внешних библиотек."""
    w = h = size
    row = b"\x00" + bytes(rgba) * w
    raw = row * h
    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)
    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    with open(path, "wb") as f:
        f.write(png)


def box_model(texture, to_y):
    """Простая модель-бокс с одной текстурой на все грани."""
    return {
        "parent": "block/block",
        "render_type": "minecraft:translucent",
        "textures": {
            "all": texture,
            "particle": texture,
        },
        "elements": [
            {
                "from": [0, 0, 0],
                "to": [16, to_y, 16],
                "faces": {face: {"uv": [0, 0, 16, 16], "texture": "#all"}
                          for face in ("down", "up", "north", "south", "west", "east")},
            }
        ],
    }


def main():
    os.makedirs(OUT_MODELS, exist_ok=True)
    os.makedirs(OUT_TEXTURES, exist_ok=True)

    variants = [
        # (имя модели, имя текстуры, высота бокса в 16-х, цвет)
        ("reb_construction", "reb_construction", 16.0, GREEN),
        ("reb_construction_invalid", "reb_construction_invalid", 16.0, RED),
        ("reb_mini_construction", "reb_mini_construction", 13.2, GREEN),
        ("reb_mini_construction_invalid", "reb_mini_construction_invalid", 13.2, RED),
    ]
    for model_name, tex_name, height, color in variants:
        model_path = os.path.join(OUT_MODELS, f"{model_name}.json")
        with open(model_path, "w", encoding="utf-8") as f:
            json.dump(box_model(f"pwpwarfare:block/{tex_name}", height), f, ensure_ascii=False, indent=2)
        write_png(os.path.join(OUT_TEXTURES, f"{tex_name}.png"), 64, color)
        print(f"model {model_name}.json + texture {tex_name}.png (box 16x{height}x16)")


if __name__ == "__main__":
    main()
