#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор блок-моделей для строительных макетов uncomplicated-fpv (.geo.json).

UV-развёртка: берём per-face UV из geo.json (формат {"uv": [u,v], "uv_size": [du,dv]})
и кладём на face-элементы Minecraft block model. Формат [u0,v0,u1,v1] (плоский
список) тоже поддерживается.

Ориентация: GeckoLib (чем рендерится собранная сущность) раскладывает текстуру
по граням в bedrock-конвенции — у боковых граней (north/south/east/west) ось u
зеркальна относительно Java-блокмоделей, у up/down зеркальна ось v (выведено из
байткода BakedModelFactory$VertexSet/GeoQuad.build geckolib 4.4.9). Поэтому:
u-флип для боковых граней, v-флип для up/down, относительно texture_width/height
из geo. Отрицательные uv_size (стандарт для down-граней) обрабатываются флипом.

Обработка структуры:
- кубы с нулевой толщиной по одной оси без поворота -> толщина 0.02 (Java-модель
  не умеет from == to), грани с вырожденным UV-прямоугольником не эмитятся;
- кубы с rotation/pivot -> бейк в AABB (поворот 8 вершин вокруг pivot, порядок
  X,Y,Z) — Java-блокмодели не умеют многокоординатные повороты; тонкие кубы с
  поворотом (пропеллеры и т.п.) пропускаются целиком;
- грани, которых нет в geo, не эмитятся (в bedrock отсутствие грани = не рисуется).

Текстуры: оригинальные reb_max.png (128x128) / reb_mini.png (64x64) из UFPV
копируются в %TEMP%\\opencode (распаковка из жарника встроена в скрипт);
ghost/invalid модели используют тонированные копии (зелёная/красная) тех же UV.

Запуск: py -3 tools/generate_reb_blueprint.py
"""
import json
import math
import os
import shutil
import zipfile

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TMP = os.path.join(os.environ.get("TEMP", "/tmp"), "opencode")
ASSETS = os.path.join(BASE, "pwp-warfare", "src", "main", "resources", "assets", "pwpwarfare")
OUT_MODELS = os.path.join(ASSETS, "models", "block")
OUT_TEXTURES = os.path.join(ASSETS, "textures", "block")

UFPV_JAR = os.path.join(BASE, "..", "Pwpfiles", "mods", "uncomplicated-fpv-1.0.0.jar")

SIDE_FACES = ("north", "south", "east", "west")

# Указатель толщины для нулевых осей (не трогаем ненулевые оси)
MIN_THICKNESS = 0.02


def extract_from_jar():
    """Распаковывает geo.json и текстуры РЭБ из жарника UFPV в %TEMP%\\opencode."""
    if not os.path.exists(UFPV_JAR):
        print(f"WARN: UFPV jar не найден: {UFPV_JAR}")
        return False
    os.makedirs(TMP, exist_ok=True)
    wanted = {"reb_max.geo.json", "reb_mini.geo.json", "reb_max.png", "reb_mini.png"}
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


def parse_face_rect(face_uv):
    """Возвращает (u, v, du, dv) из per-face UV любого формата или None."""
    if isinstance(face_uv, dict) and "uv" in face_uv and "uv_size" in face_uv:
        u, v = face_uv["uv"]
        du, dv = face_uv["uv_size"]
        return float(u), float(v), float(du), float(dv)
    if isinstance(face_uv, (list, tuple)) and len(face_uv) == 4:
        u0, v0, u1, v1 = face_uv
        return float(u0), float(v0), float(u1) - float(u0), float(v1) - float(v0)
    return None


def to_java_uv(face, u, v, du, dv, tex_w, tex_h):
    """Bedrock/GeckoLib -> Java UV. Боковые грани: u-флип; up/down: v-флип."""
    u1, u2 = u, u + du
    v1, v2 = v, v + dv
    if face in SIDE_FACES:
        u1, u2 = u2, u1
    else:
        v1, v2 = v2, v1
    return [round(u1, 2), round(v1, 2), round(u2, 2), round(v2, 2)]


def is_degenerate(uv):
    """UV-прямоугольник с нулевым размером по любой оси — не эмитим."""
    return abs(uv[2] - uv[0]) < 0.001 or abs(uv[3] - uv[1]) < 0.001


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
    """Читает кубы из geo.json: origin, size, per-face UV, rotation/pivot."""
    with open(geo_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    geom = data["minecraft:geometry"][0]
    desc = geom.get("description", {})
    tex_w = int(desc.get("texture_width", 64))
    tex_h = int(desc.get("texture_height", 64))
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
            faces = {}
            for face in ("north", "east", "south", "west", "up", "down"):
                rect = parse_face_rect(cube.get("uv", {}).get(face))
                if rect is None:
                    continue
                uv = to_java_uv(face, *rect, tex_w, tex_h)
                if is_degenerate(uv):
                    continue
                faces[face] = uv
            cubes.append({"origin": origin, "size": size, "rotation": rotation, "pivot": pivot, "faces": faces})
    return tex_w, tex_h, cubes


def normalize(cubes):
    """Сдвиг модели в блок: x/z с запасом 1, y с нуля."""
    min_x = min(c[0][0] for c in cubes)
    min_y = min(c[0][1] for c in cubes)
    min_z = min(c[0][2] for c in cubes)
    shift = (-min_x + 1.0, -min_y, -min_z + 1.0)
    return [((o[0] + shift[0], o[1] + shift[1], o[2] + shift[2]), s, uv) for o, s, uv in cubes]


def build_model(cubes, texture):
    elements = []
    for i, (origin, size, uv) in enumerate(cubes):
        x, y, z = origin
        sx, sy, sz = size
        to = (x + sx, y + sy, z + sz)
        faces = {}
        for face in ("north", "east", "south", "west", "up", "down"):
            if face in uv:
                faces[face] = {"uv": uv[face], "texture": "#all"}
        elements.append({
            "name": f"part{i}",
            "from": [round(v, 4) for v in (x, y, z)],
            "to": [round(v, 4) for v in to],
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


def make_tint(src_path, dst_path, tint):
    """Тонировка оригинала (зелёная ghost / красная invalid), альфа сохраняется."""
    try:
        from System.Drawing import Bitmap, Color, ImageFormat  # py -3 + pythonnet
        bmp = Bitmap(src_path)
        for y in range(bmp.Height):
            for x in range(bmp.Width):
                p = bmp.GetPixel(x, y)
                if p.A == 0:
                    continue
                r = min(255, int(p.R * 0.6 + tint[0] * 0.4))
                g = min(255, int(p.G * 0.6 + tint[1] * 0.4))
                b = min(255, int(p.B * 0.6 + tint[2] * 0.4))
                bmp.SetPixel(x, y, Color.FromArgb(p.A, r, g, b))
        bmp.Save(dst_path, ImageFormat.Png)
        bmp.Dispose()
        return True
    except Exception as e:
        print(f"WARN: тонировка не удалась ({e})")
        return False


def main():
    os.makedirs(OUT_MODELS, exist_ok=True)
    os.makedirs(OUT_TEXTURES, exist_ok=True)

    if not extract_from_jar():
        print("Останавливаюсь: исходники РЭБ не распакованы из жарника")
        return

    variants = [
        ("reb_max", "reb_construction", "reb_max", 128),
        ("reb_mini", "reb_mini_construction", "reb_mini", 64),
    ]
    for geo_name, out_name, tex_name, tex_size in variants:
        geo_path = os.path.join(TMP, f"{geo_name}.geo.json")
        if not os.path.exists(geo_path):
            print(f"SKIP {geo_name}: {geo_path} not found")
            continue

        tex_w, tex_h, raw_cubes = load_cubes(geo_path)
        prepared = []
        dropped = 0
        for cube in raw_cubes:
            origin, size, rotation, pivot, faces = (
                cube["origin"], cube["size"], cube["rotation"], cube["pivot"], cube["faces"],
            )
            if not faces:
                continue
            rotated = any(abs(a) > 0.001 for a in rotation)
            thin_axis = [i for i, s in enumerate(size) if abs(s) < 0.001]
            if thin_axis and rotated:
                # Тонкие повёрнутые детали (пропеллеры/антенны) в Java-модель не переносим
                dropped += 1
                continue
            if thin_axis:
                # Нулевая толщина -> минимальная, иначе грань вырожденная
                size = list(size)
                for i in thin_axis:
                    size[i] = MIN_THICKNESS
            if rotated:
                # Бейк поворота в AABB (приближение для голограммы)
                origin, size = bake_rotation(origin, size, pivot, rotation)
                origin = list(origin)
                size = [size[i] - origin[i] for i in range(3)]
            prepared.append((tuple(origin), tuple(size), faces))

        if dropped:
            print(f"{geo_name}: пропущено повёрнутых тонких кубов: {dropped}")

        cubes = normalize(prepared)

        # Ghost (зелёная) и invalid (красная) тонировки оригинала
        src_tex = os.path.join(TMP, f"{geo_name}.png")
        ghost = os.path.join(OUT_TEXTURES, f"{out_name}.png")
        invalid = os.path.join(OUT_TEXTURES, f"{out_name}_invalid.png")
        if os.path.exists(src_tex):
            make_tint(src_tex, ghost, (0, 220, 110))
            make_tint(src_tex, invalid, (220, 60, 40))
            print(f"tints {out_name}.png / {out_name}_invalid.png")
        else:
            print(f"WARN: {src_tex} не найден — тонировки не обновлены")

        # Модели: ghost и invalid — те же UV, разные текстуры
        ghost_model = os.path.join(OUT_MODELS, f"{out_name}.json")
        invalid_model = os.path.join(OUT_MODELS, f"{out_name}_invalid.json")
        with open(ghost_model, "w", encoding="utf-8") as f:
            json.dump(build_model(cubes, f"pwpwarfare:block/{out_name}"), f, ensure_ascii=False, indent=2)
        with open(invalid_model, "w", encoding="utf-8") as f:
            json.dump(build_model(cubes, f"pwpwarfare:block/{out_name}_invalid"), f, ensure_ascii=False, indent=2)
        print(f"models {out_name}.json / {out_name}_invalid.json ({len(cubes)} cubes, UV из geo.json)")


if __name__ == "__main__":
    main()
