#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Процедурный генератор атласа иконок игрового UI pwp-warfare (чистый Python: zlib + struct).

Переиспользует примитивы и растеризатор tools/generate_ui_textures.py (тот же стиль: белые
силуэты 16x16, цвет задаётся при рисовании через shader color — WarfareIcon / WDraw.icon).
Результат детерминирован.

Выход: pwp-warfare/src/main/resources/assets/pwpwarfare/textures/gui/ui/icons.png  (128x64, 8x4)

Раскладка (индекс = col + row*8) — ДОЛЖНА совпадать с client/gui/ui/WarfareIcon.java:
   0 SPAWN_MAIN  1 SPAWN_RALLY  2 SPAWN_FOB  3 LOCK     4 CLOCK   5 MAP     6 SQUAD   7 CHAT
   8 RULES       9 TEAM        10 TICKET    11 DEPLOY  12 PERSON  13 BACKPACK 14 COMPASS 15 TARGET
  16 WALL       17 WIRE        18 SHIELD    19 GUN     20 CRATE   21 RADIO  22 MARKER  23 PATH
  24 EDIT       25 PLUS        26 MINUS     27 STAR    28 GRID    29 LAYERS 30 TRASH   31 VEHICLE

Запуск: py -3 tools/generate_warfare_ui_textures.py
"""
import importlib.util
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
BASE = os.path.dirname(HERE)
_spec = importlib.util.spec_from_file_location("pwp_ui_tex", os.path.join(HERE, "generate_ui_textures.py"))
K = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(K)

rect, circle, ring, poly, union, minus = K.rect, K.circle, K.ring, K.poly, K.union, K.minus
CELL = K.CELL
OUT = os.path.join(BASE, "pwp-warfare", "src", "main", "resources", "assets", "pwpwarfare", "textures", "gui", "ui")


def line(x0, y0, x1, y1, w):
    def f(x, y):
        vx, vy = x1 - x0, y1 - y0
        l2 = vx * vx + vy * vy or 1e-9
        t = max(0, min(1, ((x - x0) * vx + (y - y0) * vy) / l2))
        px, py = x0 + t * vx, y0 + t * vy
        return (x - px) ** 2 + (y - py) ** 2 <= (w / 2) ** 2
    return f


def spawn_main():  # база: дом
    return minus(union(poly((8, 1.5), (15, 7.5), (1, 7.5)), rect(3, 7, 13, 14.5)), rect(6.5, 10, 9.5, 14.5))


def spawn_rally():  # ралли: флажок-вымпел на древке
    return union(rect(3, 1, 4.6, 15.5), poly((4.6, 1.5), (14.5, 5), (4.6, 8.5)), rect(1.5, 14, 7, 15.5))


def spawn_fob():  # FOB: палатка
    return minus(poly((8, 2), (15.2, 14.5), (0.8, 14.5)), poly((8, 8), (10.5, 14.6), (5.5, 14.6)))


def lock():
    return union(minus(rect(3, 7, 13, 15), rect(7.2, 9.5, 8.8, 12.5)), minus(ring(8, 7, 3, 4.6), rect(0, 7, 16, 16)))


def clock():
    return union(minus(circle(8, 8, 7.2), circle(8, 8, 5.6)), rect(7.2, 3.5, 8.8, 8.8), rect(7.2, 7.2, 11.5, 8.8))


def map_icon():
    return minus(poly((1, 3), (5.5, 1.5), (10.5, 3), (15, 1.5), (15, 13), (10.5, 14.5), (5.5, 13), (1, 14.5)),
                 rect(5, 3, 6, 13), rect(10, 4.5, 11, 14.2))


def squad():
    return union(circle(8, 4.5, 2.6), circle(3.4, 6.5, 2.1), circle(12.6, 6.5, 2.1),
                 rect(5.2, 8, 10.8, 14.5), rect(1.2, 9.5, 5.6, 13.5), rect(10.4, 9.5, 14.8, 13.5))


def chat():
    return union(rect(1.5, 2, 14.5, 11), poly((4, 10.5), (4, 15), (8.5, 10.5)))


def rules():
    return minus(rect(3, 1, 13, 15), rect(5, 4, 11, 5.3), rect(5, 7, 11, 8.3), rect(5, 10, 9.5, 11.3))


def team():
    return union(circle(5.5, 5, 2.6), circle(11, 5.5, 2.4), rect(2, 8.8, 9, 15), rect(9.6, 9.2, 14.5, 14.5))


def ticket():
    return minus(rect(1, 4, 15, 12), circle(1, 8, 2), circle(15, 8, 2), rect(9.5, 5, 10.5, 6.5),
                 rect(9.5, 7.5, 10.5, 8.5), rect(9.5, 9.5, 10.5, 11))


def deploy():  # стрелка вниз на «землю»
    return union(rect(6.5, 1, 9.5, 8), poly((2.5, 7.5), (13.5, 7.5), (8, 12.5)), rect(1.5, 13.5, 14.5, 15))


def person():
    return union(circle(8, 3.8, 2.8), rect(4.5, 7.5, 11.5, 12), rect(5.2, 12, 7.4, 15.5), rect(8.6, 12, 10.8, 15.5))


def backpack():
    return minus(union(rect(3, 4, 13, 15), minus(ring(8, 4.5, 2, 3.5), rect(0, 4.5, 16, 16))),
                 rect(5, 9, 11, 10.2))


def compass():
    return union(minus(circle(8, 8, 7.4), circle(8, 8, 6)), poly((8, 2.5), (10.2, 8), (5.8, 8)),
                 minus(poly((8, 13.5), (10.2, 8), (5.8, 8)), poly((8, 12), (9.2, 8.6), (6.8, 8.6))))


def target():
    return union(ring(8, 8, 5.8, 7.4), ring(8, 8, 2.6, 4.0), circle(8, 8, 1.1))


def wall():
    return minus(rect(1, 3, 15, 13), rect(1, 6.2, 15, 7), rect(1, 9.6, 15, 10.4),
                 rect(7.6, 3, 8.4, 6.2), rect(4, 7, 4.8, 9.6), rect(11, 7, 11.8, 9.6), rect(7.6, 10.4, 8.4, 13))


def wire():
    pts = [(1, 8), (3, 4), (5, 12), (7, 4), (9, 12), (11, 4), (13, 12), (15, 8)]
    segs = [line(*pts[i], *pts[i + 1], 1.5) for i in range(len(pts) - 1)]
    return union(*segs, rect(0.5, 7.4, 15.5, 8.6))


def shield():
    return minus(poly((8, 1), (14.5, 3.5), (13.5, 10), (8, 15.2), (2.5, 10), (1.5, 3.5)),
                 poly((8, 3.2), (12.4, 5), (11.6, 9.5), (8, 12.8)))


def gun():
    return union(rect(1, 6, 13, 8.5), rect(13, 6.6, 15.5, 7.8), poly((1, 8.5), (4.5, 8.5), (3, 12.5), (0.8, 12.5)),
                 rect(6.5, 8.5, 8.5, 12), rect(8.8, 8.5, 9.8, 10), rect(4, 4.5, 7, 6))


def crate():
    return minus(rect(1.5, 3, 14.5, 14), line(2.5, 4, 13.5, 13, 1.2), line(13.5, 4, 2.5, 13, 1.2),
                 rect(1.5, 6, 14.5, 6.01))


def radio():
    return union(minus(rect(3, 6, 13, 15), circle(8, 10.5, 2), rect(9.5, 13, 11.5, 14)), rect(10, 1, 11.4, 6))


def marker():
    pin = union(circle(8, 6, 5), poly((4, 8.5), (12, 8.5), (8, 15.5)))
    return minus(pin, circle(8, 6, 2.2))


def path():
    dots = [circle(2.5 + i * 2.6, 12.5 - i * 1.2, 1.1) for i in range(4)]
    return union(*dots, poly((11.5, 4), (15.5, 2.5), (14, 6.5)), line(11, 6.5, 14.5, 3.5, 1.6))


def edit():
    return union(line(3.5, 12.5, 12, 4, 3), poly((1.5, 14.5), (2.5, 11), (5, 13.5)))


def plus():
    return union(rect(6.8, 2, 9.2, 14), rect(2, 6.8, 14, 9.2))


def minus_icon():
    return rect(2, 6.8, 14, 9.2)


def star():
    pts = []
    for i in range(10):
        ang = math.pi * 2 * i / 10 - math.pi / 2
        r = 7.3 if i % 2 == 0 else 3.0
        pts.append((8 + r * math.cos(ang), 8.4 + r * math.sin(ang)))
    return poly(*pts)


def grid():
    lines = []
    for p in (1, 5.5, 10, 14.5):
        lines.append(rect(p, 1, p + 1, 15.5))
        lines.append(rect(1, p, 15.5, p + 1))
    return union(*lines)


def layers():
    return union(minus(poly((8, 1.5), (15, 5), (8, 8.5), (1, 5)), poly((8, 3.5), (11, 5), (8, 6.5), (5, 5))),
                 minus(poly((1, 8), (8, 11.5), (15, 8), (15, 9.6), (8, 13.1), (1, 9.6)), rect(0, 0, 0, 0)),
                 poly((1, 11.4), (8, 14.9), (15, 11.4), (15, 13), (8, 16), (1, 13)))


def trash():
    return union(rect(2, 3, 14, 4.6), rect(6, 1, 10, 3),
                 minus(poly((3.2, 5.4), (12.8, 5.4), (12, 15), (4, 15)), rect(6, 7, 7, 13.5), rect(9, 7, 10, 13.5)))


def vehicle():
    return union(minus(union(rect(1, 5, 10, 12), rect(10, 7.5, 15, 12)), rect(11, 8.5, 13.5, 10)),
                 circle(4.5, 12.5, 2.2), circle(12, 12.5, 2.2))


ICONS = [
    spawn_main(), spawn_rally(), spawn_fob(), lock(), clock(), map_icon(), squad(), chat(),
    rules(), team(), ticket(), deploy(), person(), backpack(), compass(), target(),
    wall(), wire(), shield(), gun(), crate(), radio(), marker(), path(),
    edit(), plus(), minus_icon(), star(), grid(), layers(), trash(), vehicle(),
]
COLS, ROWS = 8, 4


def atlas():
    px = K.blank(COLS * CELL, ROWS * CELL)
    for i, shape in enumerate(ICONS):
        cell = K.rasterize(shape)
        ox, oy = (i % COLS) * CELL, (i // COLS) * CELL
        for y in range(CELL):
            for x in range(CELL):
                px[oy + y][ox + x] = cell[y][x]
    return px


def main():
    os.makedirs(OUT, exist_ok=True)
    px = atlas()
    path_out = os.path.join(OUT, "icons.png")
    K.write_png(path_out, COLS * CELL, ROWS * CELL, px)
    print(f"OK icons.png {COLS * CELL}x{ROWS * CELL} -> {os.path.relpath(path_out, BASE)}")
    assert len(ICONS) == COLS * ROWS, len(ICONS)
    for i in range(len(ICONS)):
        ox, oy = (i % COLS) * CELL, (i // COLS) * CELL
        filled = sum(1 for y in range(CELL) for x in range(CELL) if px[oy + y][ox + x][3])
        assert 12 < filled < CELL * CELL - 12, f"иконка {i}: {filled} px"
    print("OK icons:", len(ICONS))


if __name__ == "__main__":
    main()
