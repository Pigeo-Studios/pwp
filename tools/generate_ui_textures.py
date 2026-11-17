#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Процедурный генератор текстур UI-кита pwp-core-client (чистый Python: zlib + struct).

Все текстуры МОНОХРОМНЫЕ (белый/чёрный + альфа): цвет задаёт PWPTheme через
GuiGraphics.setColor (shader color), поэтому палитра меняется без перегенерации.
Результат детерминирован (фиксированный seed) — повторный запуск даёт те же байты.

Выход: pwp-core-client/src/main/resources/assets/pwp_core_client/textures/gui/ui/
  noise.png      64x64  — тайл шума (белые/чёрные точки низкой альфы), рисуется повтором
  scanlines.png  64x64  — тайл скан-лайнов (каждая 2-я строка чёрная, низкая альфа)
  frame.png      32x32  — 9-slice рамка (угол 6): 1px линия + уголки-скобы; центр пустой
  shadow.png     32x32  — 9-slice мягкая тень (угол 12), края однородны вдоль длины
  icons.png      128x32 — атлас иконок 16x16 (8x2), белые силуэты под shader color

Раскладка атласа (индекс = col + row*8) — ДОЛЖНА совпадать с ui/UIIcon.java:
  0 PLAY  1 PROFILE  2 SETTINGS  3 QUIT  4 SINGLEPLAYER  5 WARNING  6 BACK  7 REPLAY
  8 DIRECT_COMBAT  9 FIRE_SUPPORT  10 SPECIALIST  11 SUPPORT  12 INFO  13 CHECK  14 CLOSE  15 SERVER

Виньетка текстурой НЕ делается: при nearest-фильтрации GUI растянутая виньетка даёт
ступени — она рисуется градиентами (UIDraw.vignette).

Запуск: py -3 tools/generate_ui_textures.py
"""
import math
import os
import random
import struct
import zlib

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(
    BASE, "pwp-core-client", "src", "main", "resources", "assets", "pwp_core_client", "textures", "gui", "ui"
)

CELL = 16
SUPERSAMPLE = 4  # 4x4 сэмплов на пиксель, порог покрытия 50% — чёткий пиксель-силуэт


# ---------------------------------------------------------------- PNG

def write_png(path, w, h, pixels):
    """pixels: список строк, каждая — список (r, g, b, a)."""
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter: none
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def blank(w, h):
    return [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]


# ---------------------------------------------------------------- тайлы

def noise():
    rnd = random.Random(0x5057)  # "PW"
    px = blank(64, 64)
    for y in range(64):
        for x in range(64):
            v = rnd.random()
            if v < 0.5:
                px[y][x] = (255, 255, 255, rnd.randint(0, 26))
            else:
                px[y][x] = (0, 0, 0, rnd.randint(0, 34))
    return px


def scanlines():
    px = blank(64, 64)
    for y in range(0, 64, 2):
        for x in range(64):
            px[y][x] = (0, 0, 0, 255)  # итоговая альфа — из PWPTheme (shader color)
    return px


def frame():
    """32x32, угол 6. Линия 1px (альфа 110) + уголки-скобы длиной 6 (альфа 255)."""
    size, corner = 32, 6
    px = blank(size, size)
    line = (255, 255, 255, 110)
    bracket = (255, 255, 255, 255)
    for i in range(size):
        for (x, y) in ((i, 0), (i, size - 1), (0, i), (size - 1, i)):
            px[y][x] = line
    for i in range(corner):
        for (x, y) in (
            (i, 0), (0, i),
            (size - 1 - i, 0), (size - 1, i),
            (i, size - 1), (0, size - 1 - i),
            (size - 1 - i, size - 1), (size - 1, size - 1 - i),
        ):
            px[y][x] = bracket
    return px


def shadow():
    """32x32, угол 12: тень прямоугольника (внутренняя область 12..20) с мягким спадом."""
    size, corner = 32, 12
    px = blank(size, size)
    lo, hi = corner, size - corner
    for y in range(size):
        for x in range(size):
            cx = x + 0.5
            cy = y + 0.5
            dx = max(lo - cx, 0, cx - hi)
            dy = max(lo - cy, 0, cy - hi)
            d = math.hypot(dx, dy)
            t = max(0.0, 1.0 - d / corner)
            a = int(round(255 * (t ** 2.2)))
            px[y][x] = (0, 0, 0, a)
    return px


# ---------------------------------------------------------------- иконки (векторно -> пиксели)

def poly_inside(pts, x, y):
    inside = False
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        if (y1 > y) != (y2 > y):
            xi = x1 + (y - y1) * (x2 - x1) / (y2 - y1)
            if x < xi:
                inside = not inside
    return inside


def rect(x0, y0, x1, y1):
    return lambda x, y: x0 <= x < x1 and y0 <= y < y1


def circle(cx, cy, r):
    return lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def ring(cx, cy, r0, r1):
    return lambda x, y: r0 * r0 <= (x - cx) ** 2 + (y - cy) ** 2 <= r1 * r1


def poly(*pts):
    return lambda x, y: poly_inside(pts, x, y)


def union(*fs):
    return lambda x, y: any(f(x, y) for f in fs)


def minus(a, *bs):
    return lambda x, y: a(x, y) and not any(b(x, y) for b in bs)


def gear():
    def f(x, y):
        dx, dy = x - 8, y - 8
        r = math.hypot(dx, dy)
        if r < 2.4:
            return False
        if r <= 5.0:
            return True
        if r <= 7.2:
            ang = math.atan2(dy, dx)
            phase = (ang / (2 * math.pi) * 8) % 1.0
            return 0.3 < phase < 0.7
        return False
    return f


def power():
    def f(x, y):
        dx, dy = x - 8, y - 8.5
        r = math.hypot(dx, dy)
        on_ring = 4.6 <= r <= 6.4
        ang = math.degrees(math.atan2(dx, -dy))  # 0 = вверх
        if on_ring and abs(ang) > 38:
            return True
        return 7 <= x < 9 and 1.5 <= y < 8.5
    return f


def crosshair():
    return union(
        ring(8, 8, 3.7, 5.7),
        rect(7, 0.5, 9, 4.5), rect(7, 11.5, 9, 15.5),
        rect(0.5, 7, 4.5, 9), rect(11.5, 7, 15.5, 9),
        circle(8, 8, 1.6),
    )


def burst():
    pts = []
    for i in range(16):
        ang = math.pi * 2 * i / 16 - math.pi / 2
        r = 7.4 if i % 2 == 0 else 3.4
        pts.append((8 + r * math.cos(ang), 8 + r * math.sin(ang)))
    return poly(*pts)


def rocket():
    return union(
        poly((8, 0.8), (5, 5), (11, 5)),          # нос
        rect(5, 5, 11, 11.5),                      # корпус
        poly((5, 8.5), (2, 13.5), (5, 12)),        # стабилизатор левый
        poly((11, 8.5), (14, 13.5), (11, 12)),     # стабилизатор правый
        rect(6.5, 12.5, 9.5, 15),                  # сопло
    )


def cross():
    return union(rect(6, 1.5, 10, 14.5), rect(1.5, 6, 14.5, 10))


def flag():
    return union(rect(2.5, 1, 4, 15.5), poly((4, 1.5), (14, 1.5), (11.2, 5.2), (14, 9), (4, 9)))


def warning():
    tri = poly((8, 0.8), (15.4, 14.6), (0.6, 14.6))
    return minus(tri, rect(7, 5, 9, 10.5), rect(7, 11.5, 9, 13.5))


def arrow_back():
    return union(poly((1.5, 8), (7.5, 2), (7.5, 14)), rect(7, 6, 14.5, 10))


def record():
    return union(ring(8, 8, 5.4, 7.2), circle(8, 8, 3.4))


def info():
    return minus(circle(8, 8, 7.2), rect(7, 6.5, 9, 12.5), rect(7, 3.5, 9, 5.5))


def check():
    def thick_line(x0, y0, x1, y1, w):
        def f(x, y):
            vx, vy = x1 - x0, y1 - y0
            l2 = vx * vx + vy * vy
            t = max(0, min(1, ((x - x0) * vx + (y - y0) * vy) / l2))
            px, py = x0 + t * vx, y0 + t * vy
            return (x - px) ** 2 + (y - py) ** 2 <= (w / 2) ** 2
        return f
    return union(thick_line(2, 8.5, 6, 12.5, 2.6), thick_line(6, 12.5, 14, 3.5, 2.6))


def close():
    def diag(sign):
        def f(x, y):
            d = (y - 8) - sign * (x - 8)
            return abs(d) <= 1.5 and 2 <= x <= 14 and 2 <= y <= 14
        return f
    return union(diag(1), diag(-1))


def server():
    return union(
        minus(rect(2, 2, 14, 7), rect(10, 4, 12, 5)),
        minus(rect(2, 9, 14, 14), rect(10, 11, 12, 12)),
    )


def profile():
    head = circle(8, 5.2, 3.2)
    body = lambda x, y: ((x - 8) / 6.2) ** 2 + ((y - 15.5) / 6.0) ** 2 <= 1 and y < 15
    return union(head, minus(body, circle(8, 5.2, 4.4)))  # зазор между головой и плечами


def play():
    return poly((4, 2.5), (4, 13.5), (13.5, 8))


def keyboard():
    """Клавиатура: корпус с рядами клавиш-прорезей и пробелом."""
    holes = []
    for row, y in enumerate((5, 8)):
        for i in range(5):
            holes.append(rect(2.5 + i * 2.4, y, 3.9 + i * 2.4, y + 1.6))
    holes.append(rect(4.5, 11, 11.5, 12.6))
    return minus(rect(0.8, 3, 15.2, 14), *holes)


def mods():
    """Моды: четыре блока 2x2, правый верхний сдвинут (как «сборка»)."""
    return union(
        rect(1.5, 8.5, 7.5, 14.5), rect(8.5, 8.5, 14.5, 14.5),
        rect(1.5, 1.5, 7.5, 7.5), rect(9.5, 0.8, 15.2, 6.5),
    )


def sliders():
    """Ползунки: три линии с ручками на разных позициях."""
    return union(
        rect(1, 3, 15, 4.4), rect(3, 1, 6, 6.4),
        rect(1, 7.4, 15, 8.8), rect(9.5, 5.4, 12.5, 10.8),
        rect(1, 11.8, 15, 13.2), rect(5.5, 9.8, 8.5, 15.2),
    )


ICONS = [
    play(), profile(), gear(), power(), flag(), warning(), arrow_back(), record(),
    crosshair(), burst(), rocket(), cross(), info(), check(), close(), server(),
    keyboard(), mods(), sliders(),
]


def rasterize(shape):
    cell = blank(CELL, CELL)
    n = SUPERSAMPLE
    for y in range(CELL):
        for x in range(CELL):
            hits = 0
            for sy in range(n):
                for sx in range(n):
                    if shape(x + (sx + 0.5) / n, y + (sy + 0.5) / n):
                        hits += 1
            if hits * 2 >= n * n:
                cell[y][x] = (255, 255, 255, 255)
    return cell


def icons():
    cols, rows = 8, 3
    px = blank(cols * CELL, rows * CELL)
    for i, shape in enumerate(ICONS):
        cell = rasterize(shape)
        ox, oy = (i % cols) * CELL, (i // cols) * CELL
        for y in range(CELL):
            for x in range(CELL):
                px[oy + y][ox + x] = cell[y][x]
    return px


def main():
    os.makedirs(OUT, exist_ok=True)
    outputs = {
        "noise.png": (64, 64, noise()),
        "scanlines.png": (64, 64, scanlines()),
        "frame.png": (32, 32, frame()),
        "shadow.png": (32, 32, shadow()),
        "icons.png": (128, 48, icons()),
    }
    for name, (w, h, px) in outputs.items():
        path = os.path.join(OUT, name)
        write_png(path, w, h, px)
        print(f"OK {name} {w}x{h} -> {os.path.relpath(path, BASE)}")
    # контроль: каждая иконка не пустая и не залита целиком
    atlas = outputs["icons.png"][2]
    for i in range(len(ICONS)):
        ox, oy = (i % 8) * CELL, (i // 8) * CELL
        filled = sum(1 for y in range(CELL) for x in range(CELL) if atlas[oy + y][ox + x][3])
        assert 20 < filled < CELL * CELL - 20, f"иконка {i}: {filled} px"
    print("OK icons:", len(ICONS))


if __name__ == "__main__":
    main()
