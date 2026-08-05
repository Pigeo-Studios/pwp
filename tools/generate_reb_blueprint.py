#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор моделей строительных макетов uncomplicated-fpv (reb / reb_mini).

Дизайн (с 06.08.2026): макеты РЭБ — ЗЕЛЁНЫЕ/КРАСНЫЕ кубы БЕЗ текстур в форме
модели: геометрия кубов берётся из .geo.json (origin/size/rotation), текстура
однотонная (как wall_ghost/wall_invalid). Никакой UV-развёртки — текстура на
макете не нужна (решение юзера).

Обработка геометрии:
- кубы с rotation/pivot бейкаются в AABB (поворот 8 вершин вокруг pivot,
  порядок X -> Y -> Z) — Java-блокмодели не умеют многокоординатные повороты;
- тонкие кубы с поворотом (пропеллеры) пропускаются целиком;
- нулевая толщина без поворота -> 0.02 (Java-модель не умеет from == to);
- у всех кубов эмитятся все 6 граней с [0,0,16,16] (текстура однотонная).

Модели: reb_construction(.json/_invalid.json) — куб-форма большого РЭБ,
reb_mini_construction(.json/_invalid.json) — куб-форма мини-РЭБ.
Текстуры: однотонные 64x64 RGBA (зелёный (87,182,7) / красный (237,0,0),
alpha 140 — как у wall_ghost/wall_invalid), генерируются чистым Python
(zlib + struct), pythonnet НЕ нужен.

Запуск: py -3 tools/generate_reb_blueprint.py
"""
import json
import math
import os
import shutil
import struct
import zlib
import zipfile

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TMP = os.path.join(os.environ.get("TEMP", "/tmp"), "opencode")
ASSETS = os.path.join(BASE, "pwp-warfare", "src", "main", "resources", "assets", "pwpwarfare")
OUT_MODELS = os.path.join(ASSETS, "models", "block")
OUT_TEXTURES = os.path.join(ASSETS, "textures", "block")

UFPV_JAR = os.path.join(BASE, "..", "Pwpfiles", "mods", "uncomplicated-fpv-1.0.0.jar")

GREEN = (87, 182, 7, 140)
RED = (237, 0, 0, 140)

MIN_THICKNESS = 0.02


def extract_from_jar():
    """Распаковывает geo.json РЭБ из жарника UFPV в %TEMP%\\opencode."""
    if not os.path.exists(UFPV_JAR):
        print(f"WARN: UFPV jar не найден: {UFPV_JAR}")
        return False
    os.makedirs(TMP, exist_ok=True)
    wanted = {"reb_max.geo.json", "reb_mini.geo.json"}
    found = set()
    with zipfile.ZipFile(UFPV_JAR) as z:
        for name in z.namelist():
            base = os.path.basename(name)
            if base in wanted and base not in found:
                with z.open(name) as src, open(os.path.join(TMP, base), "wb") as dst:
                    shutil.copyfileobj(src, dst)
                found.add(base)
    for missing in wanted - found:
        print(f"WARN: {missing} не найден в жарнике")
    return len(found) > 0


def rot_vec(v, deg):
    """Поворот вектора вокруг осей X, Y, Z (порядок X -> Y -> Z, как в bedrock)."""
    x, y, z = v
    rx, ry, rz = (math.radians(a) for a in deg)
    cosx, sinx = math.cos(rx), math.sin(rx)
    y, z = y * cosx - z * sinx, y * sinx + z * cosx
    cosy, siny = math.cos(ry), math.sin(ry)
    x, z = x * cosy + z * siny, -x * siny + z * cosy
    cosz, sinz = math.cos(rz), math.sin(rz)
    x, y = x * cosz - y * sinz, x * sinz + y * cosz
    return x, y, z


def bake_rotation(origin, size, pivot, rotation):
    """Поворачивает 8 вершин куба вокруг pivot и возвращает AABB (min, max)."""
    mins = [float("inf")] * 3
    maxs = [-float("inf")] * 3
    for corner in ((0, 0, 0), (1, 0, 0), (0, 1, 0), (0, 0, 1), (1, 1, 0), (1, 0, 1), (0, 1, 1), (1, 1, 1)):
        pt = (
            origin[0] + size[0] * corner[0],
            origin[1] + size[1] * corner[1],
            origin[2] + size[2] * corner[2],
        )
        r = rot_vec((pt[0] - pivot[0], pt[1] - pivot[1], pt[2] - pivot[2]), rotation)
        for i in range(3):
            w = pivot[i] + r[i]
            mins[i] = min(mins[i], w)
            maxs[i] = max(maxs[i], w)
    return tuple(mins), tuple(maxs)


def load_cubes(geo_path):
    """Читает геометрию кубов из geo.json (origin/size/rotation/pivot)."""
    with open(geo_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    geom = data["minecraft:geometry"][0]
    cubes = []
    for bone in geom.get("bones", []):
        for cube in bone.get("cubes", []):
            origin = [float(x) for x in cube["origin"]]
            size = [float(x) for x in cube["size"]]
            if all(abs(s) < 0.001 for s in size):
                continue
            rotation = [float(a) for a in cube.get("rotation", [0, 0, 0])]
            pivot = [float(p) for p in cube.get("pivot", [])] or [
                origin[0] + size[0] / 2.0,
                origin[1] + size[1] / 2.0,
                origin[2] + size[2] / 2.0,
            ]
            cubes.append({"origin": origin, "size": size, "rotation": rotation, "pivot": pivot})
    return cubes


def prepare_cubes(raw_cubes):
    """Толщина для нулевых осей, бейк поворотов в AABB, пропуск тонких повёрнутых."""
    prepared = []
    dropped = 0
    for cube in raw_cubes:
        origin = cube["origin"]
        size = cube["size"]
        rotation = cube["rotation"]
        pivot = cube["pivot"]
        rotated = any(abs(a) > 0.001 for a in rotation)
        thin_axis = [i for i, s in enumerate(size) if abs(s) < 0.001]
        if thin_axis and rotated:
            dropped += 1
            continue
        if thin_axis:
            size = list(size)
            for i in thin_axis:
                size[i] = MIN_THICKNESS
        if rotated:
            origin, size = bake_rotation(origin, size, pivot, rotation)
            origin = list(origin)
            size = [size[i] - origin[i] for i in range(3)]
        prepared.append((tuple(origin), tuple(size)))
    return prepared, dropped


def normalize(cubes):
    """Сдвиг модели в блок: x/z с запасом 1, y с нуля."""
    min_x = min(c[0][0] for c in cubes)
    min_y = min(c[0][1] for c in cubes)
    min_z = min(c[0][2] for c in cubes)
    shift = (-min_x + 1.0, -min_y, -min_z + 1.0)
    return [((o[0] + shift[0], o[1] + shift[1], o[2] + shift[2]), s) for o, s in cubes]


def fit_into_block(cubes):
    """Масштаб: если модель больше блока (16), вписываем её равномерно в блок.

    Ось Y масштабируется от земли (y=0), оси X/Z — вокруг центра 8.
    """
    max_x = max(o[0] + s[0] for o, s in cubes)
    max_y = max(o[1] + s[1] for o, s in cubes)
    max_z = max(o[2] + s[2] for o, s in cubes)
    extent = max(max_x, max_y, max_z)
    if extent <= 16.0:
        return cubes
    scale = 16.0 / max_y
    if max_x > 8.0:
        scale = min(scale, 8.0 / (max_x - 8.0))
    if max_z > 8.0:
        scale = min(scale, 8.0 / (max_z - 8.0))
    scaled = []
    for (ox, oy, oz), (sx, sy, sz) in cubes:
        scaled.append((
            (8.0 + (ox - 8.0) * scale, oy * scale, 8.0 + (oz - 8.0) * scale),
            (sx * scale, sy * scale, sz * scale),
        ))
    print(f"масштаб {scale:.3f} (extent {extent:.1f} > 16)")
    return scaled


def build_model(cubes, texture):
    elements = []
    for i, (origin, size) in enumerate(cubes):
        x, y, z = origin
        sx, sy, sz = size
        faces = {face: {"uv": [0, 0, 16, 16], "texture": "#all"}
                 for face in ("down", "up", "north", "south", "west", "east")}
        elements.append({
            "name": f"part{i}",
            "from": [round(v, 4) for v in (x, y, z)],
            "to": [round(v, 4) for v in (x + sx, y + sy, z + sz)],
            "faces": faces,
        })
    return {
        "parent": "block/block",
        "render_type": "minecraft:translucent",
        "textures": {
            "all": texture,
            "particle": texture,
        },
        "elements": elements,
    }


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


def main():
    os.makedirs(OUT_MODELS, exist_ok=True)
    os.makedirs(OUT_TEXTURES, exist_ok=True)

    if not extract_from_jar():
        print("Останавливаюсь: geo.json РЭБ не распакованы из жарника")
        return

    variants = [
        # (имя geo, имя модели/текстуры, цвет ghost/invalid)
        ("reb_max", "reb_construction", GREEN, RED),
        ("reb_mini", "reb_mini_construction", GREEN, RED),
    ]
    for geo_name, out_name, ghost_color, invalid_color in variants:
        geo_path = os.path.join(TMP, f"{geo_name}.geo.json")
        if not os.path.exists(geo_path):
            print(f"SKIP {geo_name}: {geo_path} not found")
            continue

        cubes, dropped = prepare_cubes(load_cubes(geo_path))
        cubes = normalize(cubes)
        cubes = fit_into_block(cubes)
        if dropped:
            print(f"{geo_name}: пропущено повёрнутых тонких кубов: {dropped}")

        for suffix, texture, color in (("", f"pwpwarfare:block/{out_name}", ghost_color),
                                       ("_invalid", f"pwpwarfare:block/{out_name}_invalid", invalid_color)):
            model_path = os.path.join(OUT_MODELS, f"{out_name}{suffix}.json")
            with open(model_path, "w", encoding="utf-8") as f:
                json.dump(build_model(cubes, texture), f, ensure_ascii=False, indent=2)
            write_png(os.path.join(OUT_TEXTURES, f"{out_name}{suffix}.png"), 64, color)
        print(f"models {out_name}.json / {out_name}_invalid.json ({len(cubes)} cubes из geo.json, однотонные текстуры)")


if __name__ == "__main__":
    main()
