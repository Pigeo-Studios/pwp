# -*- coding: utf-8 -*-
# Генератор справочника аттачментов tacz-паков со статами.
# Читает data/<pack>/data/attachments/*_data.json + index + lang.
# Запуск: py -3 tools/generate_attachments_reference.py
import os, json, collections, re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PWFILES = r"C:\Users\maska\OneDrive\Desktop\Pwpfiles"
TACZ = os.path.join(PWFILES, "tacz")
OUT = os.path.join(BASE, "docs", "ATTACHMENTS_REFERENCE.md")

PACKS = [
    ("tacz", os.path.join(TACZ, "tacz_default_gun")),
    ("maxstuff", os.path.join(TACZ, "maxstuff")),
    ("rfp", os.path.join(TACZ, "rfp")),
    ("gucci_attachments", os.path.join(TACZ, "gucci_vuitton_attachment")),
]

# названия типов
TYPE_RU = {
    "scope": "Прицел", "grip": "Рукоятка", "muzzle": "Дульное", "stock": "Приклад",
    "extended_mag": "Магазин", "ammo_mod": "Аммо-мод", "bayonet": "Штык", "laser": "ЛЦУ",
    "oem_stock": "OEM-приклад", "underbarrel": "Подствольник", "skin": "Скин",
    "light_extended_mag": "Магазин (лёгкий)", "deagle_sight": "Прицел DE", "other": "Прочее",
}


def load_json(p):
    try:
        with open(p, encoding="utf-8") as f:
            return parse_json5(f.read())
    except Exception:
        return None


def parse_json5(txt):
    """tacz-паки используют JSON5 (комментарии // и /* */) — стрипуем их."""
    out = []
    i = 0
    n = len(txt)
    in_str = False
    while i < n:
        c = txt[i]
        if in_str:
            out.append(c)
            if c == "\\" and i + 1 < n:
                out.append(txt[i + 1])
                i += 2
                continue
            if c == '"':
                in_str = False
            i += 1
            continue
        if c == '"':
            in_str = True
            out.append(c)
            i += 1
            continue
        if c == "/" and i + 1 < n and txt[i + 1] == "/":
            while i < n and txt[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and txt[i + 1] == "*":
            i += 2
            while i + 1 < n and not (txt[i] == "*" and txt[i + 1] == "/"):
                i += 1
            i += 2
            continue
        out.append(c)
        i += 1
    return json.loads("".join(out))


def fmt_stats(data):
    out = {}
    def dig(d, path):
        cur = d
        for k in path:
            if not isinstance(cur, dict) or k not in cur:
                return None
            cur = cur[k]
        return cur
    keys = [
        ("Вес", ["weight"]),
        ("ADS", ["ads", "multiplier"]),
        ("Точность(hip)", ["hipFireAccuracy", "multiplier"]),
        ("Точность(ads)", ["adsAccuracy", "multiplier"]),
        ("Разброс", ["inaccuracy", "multiplier"]),
        ("Отдача pitch", ["recoil", "pitch", "multiplier"]),
        ("Отдача yaw", ["recoil", "yaw", "multiplier"]),
        ("Зум", ["zoom"]),
        ("Урон", ["damage", "multiplier"]),
        ("Скорость пули", ["bulletSpeed", "multiplier"]),
        ("Патрон в маг", ["magazineCapacity", "multiplier"]),
        ("Скорость перезарядки", ["reload", "multiplier"]),
        ("Скорость выстрела", ["fireRate", "multiplier"]),
        ("Факел", ["flashlight"]),
    ]
    for label, path in keys:
        v = dig(data, path)
        if v is not None:
            out[label] = v
    # остальные ключи верхнего уровня
    skip = {"weight", "ads", "hipFireAccuracy", "adsAccuracy", "inaccuracy", "recoil", "zoom",
            "damage", "bulletSpeed", "magazineCapacity", "reload", "fireRate", "flashlight"}
    for k, v in data.items():
        if k not in skip and not isinstance(v, (dict, list)):
            out[k] = v
    return out


def main():
    rows = []  # (pack, id, type, name_ru, name_en, stats_str)
    for pack, root in PACKS:
        att_dir = os.path.join(root, "data", pack if pack != "gucci_attachments" else "gucci_attachments", "data", "attachments")
        idx_dir = os.path.join(root, "data", pack if pack != "gucci_attachments" else "gucci_attachments", "index", "attachments")
        lang_en = load_json(os.path.join(root, "assets", pack, "lang", "en_us.json")) or {}
        lang_ru = load_json(os.path.join(root, "assets", pack, "lang", "ru_ru.json")) or {}
        if not os.path.isdir(att_dir):
            continue
        for f in sorted(os.listdir(att_dir)):
            if not f.endswith("_data.json"):
                continue
            iid = f[:-10]
            data = load_json(os.path.join(att_dir, f)) or {}
            idx = load_json(os.path.join(idx_dir, iid + ".json")) or {}
            atype = idx.get("type", "other")
            name_key = idx.get("name") or f"item.{pack}.{iid}"
            name_en = lang_en.get(name_key, "?")
            name_ru = lang_ru.get(name_key, "?")
            stats = fmt_stats(data)
            stats_str = "; ".join(f"{k}: {v}" for k, v in stats.items()) or "—"
            rows.append((pack, iid, atype, name_ru, name_en, stats_str))

    # сортировка по типу, потом по паку
    rows.sort(key=lambda r: (r[2], r[0], r[1]))

    md = ["# Справочник аттачментов tacz-паков (со статами)", ""]
    md.append("> Паки: tacz_default_gun, maxstuff, rfp, gucci_attachments. Статы из `data/attachments/*_data.json`.")
    md.append("")
    cur_type = None
    for pack, iid, atype, name_ru, name_en, stats_str in rows:
        if atype != cur_type:
            cur_type = atype
            md.append(f"## {TYPE_RU.get(atype, atype)} ({atype})")
            md.append("")
            md.append("| ID | Название (RU) | Название (EN) | Статы |")
            md.append("|---|---|---|---|")
        md.append(f"| `{pack}:{iid}` | {name_ru} | {name_en} | {stats_str} |")
    md.append("")

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(md))
    print(f"OK {OUT} | аттачментов: {len(rows)}")


if __name__ == "__main__":
    main()
