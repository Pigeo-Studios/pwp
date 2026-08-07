# -*- coding: utf-8 -*-
"""Процедурная отрисовка иконки и обложки для CurseForge (sbwchunkload).

Стиль: flat military-HUD, тёмный фон, сетка чанков 3x3, янтарный трассер,
циановая подсветка «загруженного» коридора. Без текста и чужих лого.

Выходы (в ту же папку):
  icon_512.png   — основная иконка 512x512
  logo_400.png   — минимальный размер логотипа CurseForge 400x400
  cover_856x300.png — главный баннер страницы проекта 856x300
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter, ImageChops, ImageOps

S = 512

BG_TOP = (13, 16, 24)       # #0D1018
BG_BOT = (8, 10, 15)        # #080A0F
GRID_LINE = (32, 41, 58)    # #20293A
GRID_DIM = (24, 31, 44)
CYAN = (34, 211, 238)       # #22D3EE
AMBER = (255, 176, 32)      # #FFB020
AMBER_GLOW = (255, 122, 0)  # #FF7A00
RED = (239, 68, 68)         # #EF4444
BODY = (170, 180, 198)
BODY_DARK = (92, 102, 118)


def lerp3(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def vertical_gradient(w, h, top, bot):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        d.line([(0, y), (w, y)], fill=lerp3(top, bot, y / (h - 1)))
    return img


def vignette(img, strength=0.42):
    w, h = img.size
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse([-w * 0.35, -h * 0.35, w * 1.35, h * 1.35], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(w * 0.18))
    dark = Image.new("RGB", (w, h), (0, 0, 0))
    img.paste(dark, (0, 0), ImageOps.invert(mask).point(lambda v: int(v * strength)))
    return img


def grain(img, count=3500, max_alpha=28):
    w, h = img.size
    rnd = random.Random(42)
    ov = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(ov)
    for _ in range(count):
        d.point((rnd.randrange(w), rnd.randrange(h)), fill=rnd.randrange(max_alpha))
    noise = Image.new("RGB", (w, h), (255, 255, 255))
    img.paste(noise, (0, 0), ov)
    return img


def glow_layer(size, draw_fn, radius, color):
    """Рисует draw_fn на чёрном слое, размывает и накладывает screen."""
    w, h = size
    layer = Image.new("RGB", (w, h), (0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    layer = layer.filter(ImageFilter.GaussianBlur(radius))
    return layer


def screen(base, layer):
    return ImageChops.screen(base, layer)


def draw_rocket(d, px, py, angle_deg, scale=1.0):
    """Стилизованная ракета, нос по направлению angle_deg (0 = вправо)."""
    a = math.radians(angle_deg)
    ca, sa = math.cos(a), math.sin(a)
    L, w = 52 * scale, 17 * scale

    def t(x, y):
        return (px + x * ca - y * sa, py + x * sa + y * ca)

    # Корпус: нос-стрелка + хвост
    pts = [t(L / 2, 0), t(L / 4, -w), t(-L / 4, -w), t(-L / 4, w), t(L / 4, w)]
    d.polygon(pts, fill=BODY)
    # Тёмная нижняя кромка
    d.line([t(L / 4, -w), t(-L / 4, -w)], fill=BODY_DARK, width=int(2 * scale))
    # Стабилизаторы
    d.polygon([t(-L / 4, -w), t(-L / 2 - 6 * scale, -w - 8 * scale), t(-L / 2, -w)], fill=BODY_DARK)
    d.polygon([t(-L / 4, w), t(-L / 2 - 6 * scale, w + 8 * scale), t(-L / 2, w)], fill=BODY_DARK)
    # Сопло
    c1, c2 = t(-L / 2 - 5 * scale, -4 * scale), t(-L / 2 + 3 * scale, 4 * scale)
    d.ellipse([min(c1[0], c2[0]), min(c1[1], c2[1]), max(c1[0], c2[0]), max(c1[1], c2[1])], fill=(40, 44, 54))


def tracer(img, start, end, control, color=AMBER, glow_color=AMBER_GLOW, width=5, glow_w=16):
    """Кривая Безье с glow-слоем и белым ядром."""
    w, h = img.size
    n = 48
    points = []
    for i in range(n + 1):
        t = i / n
        inv = 1 - t
        x = inv * inv * start[0] + 2 * inv * t * control[0] + t * t * end[0]
        y = inv * inv * start[1] + 2 * inv * t * control[1] + t * t * end[1]
        points.append((x, y))

    def draw_glow(dr):
        dr.line(points, fill=glow_color, width=glow_w, joint="curve")
        dr.line(points, fill=color, width=max(3, glow_w - 8), joint="curve")

    img.paste(screen(img, glow_layer(img.size, draw_glow, glow_w, glow_color)), (0, 0))
    d = ImageDraw.Draw(img)
    d.line(points, fill=color, width=width, joint="curve")
    d.line(points, fill=(255, 250, 235), width=max(1, width // 3), joint="curve")


def chunk_grid(img, x0, y0, cell, cols, rows, corridor, accent=CYAN, dim=GRID_LINE, edge_w=3):
    """Сетка чанков; corridor — список (col,row) подсвеченных ячеек (контур + glow)."""
    w, h = img.size

    def draw_acc(dr):
        for c, r in corridor:
            x, y = x0 + c * cell, y0 + r * cell
            dr.rectangle([x + 2, y + 2, x + cell - 3, y + cell - 3], outline=accent, width=edge_w)

    img.paste(screen(img, glow_layer(img.size, draw_acc, 6, accent)), (0, 0))
    d = ImageDraw.Draw(img)
    for c in range(cols + 1):
        x = x0 + c * cell
        d.line([(x, y0), (x, y0 + rows * cell)], fill=dim, width=2)
    for r in range(rows + 1):
        y = y0 + r * cell
        d.line([(x0, y), (x0 + cols * cell, y)], fill=dim, width=2)
    for c, r in corridor:
        x, y = x0 + c * cell, y0 + r * cell
        d.rectangle([x + 2, y + 2, x + cell - 3, y + cell - 3], outline=accent, width=edge_w)


def make_icon():
    img = vertical_gradient(S, S, BG_TOP, BG_BOT)

    cell = 118
    margin = (S - cell * 3) // 2
    corridor = [(0, 2), (1, 1), (2, 0)]  # диагональ снизу-слева вверх-вправо
    chunk_grid(img, margin, margin, cell, 3, 3, corridor)

    # Трассер по диагонали через центр, в сторону «загруженного» коридора
    p0 = (112, 402)
    p3 = (404, 108)
    vx, vy = p3[0] - p0[0], p3[1] - p0[1]
    length = math.hypot(vx, vy)
    perp = (-vy / length * 55, vx / length * 55)
    p1 = ((p0[0] + p3[0]) / 2 + perp[0], (p0[1] + p3[1]) / 2 + perp[1])
    tracer(img, p0, p3, p1)

    # Ракета в конце трассера (касательная в t=1: 2*(P3-P1))
    ang = math.degrees(math.atan2(2 * (p3[1] - p1[1]), 2 * (p3[0] - p1[0])))
    draw_rocket(ImageDraw.Draw(img), p3[0] + 30, p3[1], ang, scale=1.15)

    # Свечение сопла
    glow = glow_layer(img.size, lambda d: d.ellipse(
        [p3[0] - 14, p3[1] - 14, p3[0] + 14, p3[1] + 14], fill=AMBER_GLOW), 10, AMBER_GLOW)
    img.paste(screen(img, glow), (0, 0))

    img = vignette(img)
    img = grain(img)
    return img


def make_cover():
    W, H = 856, 300
    img = vertical_gradient(W, H, BG_TOP, BG_BOT)

    cell = 88
    cols, rows = 9, 2
    x0 = (W - cols * cell) // 2
    y0 = (H - rows * cell) // 2

    # Левая часть: «застрявший» снаряд (красная граница)
    img.paste(screen(img, glow_layer(img.size, lambda d: d.rectangle(
        [x0 + 2, y0 + 2, x0 + cell - 3, y0 + cell - 3], outline=RED, width=3), 6, RED)), (0, 0))

    # Коридор справа: два ряда чанков по траектории
    corridor = [(c, r) for c in range(3, 8) for r in range(2)]
    chunk_grid(img, x0, y0, cell, cols, rows, corridor)

    # Трассер слева-направо
    p0 = (x0 + cell + 30, y0 + cell)
    p3 = (x0 + 7 * cell + 10, y0 + cell)
    p1 = ((p0[0] + p3[0]) / 2, p0[1] + 24)
    tracer(img, p0, p3, p1, width=4, glow_w=14)

    # Ракета в конце
    draw_rocket(ImageDraw.Draw(img), p3[0] + 26, p3[1], 0, scale=1.0)
    glow = glow_layer(img.size, lambda d: d.ellipse(
        [p3[0] - 12, p3[1] - 12, p3[0] + 12, p3[1] + 12], fill=AMBER_GLOW), 9, AMBER_GLOW)
    img.paste(screen(img, glow), (0, 0))

    img = vignette(img, 0.35)
    img = grain(img, 2200)
    return img


def main():
    random.seed(7)
    icon = make_icon()
    icon.save("icon_512.png")
    icon.resize((400, 400), Image.LANCZOS).save("logo_400.png")
    make_cover().save("cover_856x300.png")
    print("OK: icon_512.png, logo_400.png, cover_856x300.png")


if __name__ == "__main__":
    main()
