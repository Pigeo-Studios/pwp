# -*- coding: utf-8 -*-
# Генератор справочника статов оружия: tacz-паки (maxstuff/rfp/tacz_default) + SBW + FCL-пускачи.
# Запуск: py -3 tools/generate_gun_stats_reference.py
import os, json, re, collections, zipfile

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PWFILES = r"C:\Users\maska\OneDrive\Desktop\Pwpfiles"
SERVER = r"C:\Users\maska\OneDrive\Desktop\PWP-Server"
TACZ = os.path.join(PWFILES, "tacz")
OUT = os.path.join(BASE, "docs", "GUN_STATS_REFERENCE.md")

SBW_JAR = os.path.join(SERVER, "mods", "superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar")
FCL_ZIP = os.path.join(PWFILES, "pointblank", "fcl-ext-0.1.zip")
TACZ_JAR = os.path.join(SERVER, "mods", "tacz-1.20.1-1.1.8-hotfix.jar")


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


# ---------------- tacz-паки ----------------
def tacz_guns():
    """{gun_id: (pack, data)} — папки Pwpfiles + ядро TACZ из jar"""
    out = {}
    packs = [
        ("tacz", os.path.join(TACZ, "tacz_default_gun")),
        ("maxstuff", os.path.join(TACZ, "maxstuff")),
        ("rfp", os.path.join(TACZ, "rfp")),
    ]
    for pack, root in packs:
        gdir = os.path.join(root, "data", pack, "data", "guns")
        if not os.path.isdir(gdir):
            continue
        for f in sorted(os.listdir(gdir)):
            if not f.endswith("_data.json"):
                continue
            gid = f[:-10]
            d = load_json(os.path.join(gdir, f))
            if d:
                out.setdefault(gid, (pack, d))
    # ядро TACZ (чистые tacz:-стволы): assets/tacz/custom/tacz_default_gun/... в jar мода
    if os.path.exists(TACZ_JAR):
        with zipfile.ZipFile(TACZ_JAR) as z:
            prefix = "assets/tacz/custom/tacz_default_gun/data/tacz/data/guns/"
            for n in z.namelist():
                if n.startswith(prefix) and n.endswith("_data.json"):
                    gid = os.path.basename(n)[:-10]
                    try:
                        d = json.loads(z.read(n))
                    except Exception:
                        continue
                    if d and gid not in out:
                        out[gid] = ("tacz", d)
    return out


def tacz_stats(data):
    def g(*path):
        cur = data
        for k in path:
            if not isinstance(cur, dict) or k not in cur:
                return None
            cur = cur[k]
        return cur
    b = g("bullet") or {}
    dmg = b.get("damage")
    if isinstance(dmg, dict):
        dmg = "мульти"
    return {
        "ammo": g("ammo"),
        "mag": g("ammo_amount"),
        "rpm": g("rpm"),
        "mode": g("fire_mode"),
        "damage": dmg,
        "speed": b.get("speed"),
        "gravity": b.get("gravity"),
        "pierce": b.get("pierce"),
        "head": (b.get("extra_damage") or {}).get("head_shot_multiplier"),
        "armor_ignore": (b.get("extra_damage") or {}).get("armor_ignore"),
    }


# ---------------- SBW ----------------
def sbw_guns():
    out = {}
    with zipfile.ZipFile(SBW_JAR) as z:
        for n in z.namelist():
            if n.startswith("data/superbwarfare/sbw/guns/") and n.endswith(".json"):
                gid = os.path.basename(n)[:-5]
                out[gid] = json.loads(z.read(n))
    return out


# ---------------- FCL ----------------
def fcl_guns():
    out = {}
    with zipfile.ZipFile(FCL_ZIP) as z:
        lang = json.loads(z.read("assets/pointblank/lang/en_us.json"))
        for n in z.namelist():
            if n.startswith("assets/pointblank/items/") and n.endswith(".json"):
                gid = os.path.basename(n)[:-5]
                d = json.loads(z.read(n))
                d["_name"] = lang.get("item.pointblank." + gid, "?")
                out[gid] = d
    return out


def main():
    md = ["# Справочник статов оружия: tacz-паки / SBW / FCL-пускачи", ""]
    md.append("> Данные из `*_data.json` паков и модов. Символы: mag — магазин, rpm — темп стрельбы, HS — множитель в голову.")
    md.append("")

    # 1) tacz-паки
    guns = tacz_guns()
    md.append("## 1. tacz-паки (maxstuff / rfp / tacz_default_gun)")
    md.append("")
    md.append("| ID | Пак | Патрон | Маг | RPM | Режим | Урон | HS | Скорость пули | Пробой |")
    md.append("|---|---|---|---|---|---|---|---|---|---|")
    for gid in sorted(guns):
        pack, d = guns[gid]
        s = tacz_stats(d)
        md.append(f"| `{pack}:{gid}` | {pack} | {s['ammo'] or '?'} | {s['mag'] or '?'} | {s['rpm'] or '?'} | {s['mode'] or '?'} | {s['damage'] or '?'} | {s['head'] or '?'} | {s['speed'] or '?'} | {s['pierce'] or '?'} |")
    md.append("")

    # 2) SBW
    sbw = sbw_guns()
    md.append("## 2. SuperbWarfare (sbw/guns)")
    md.append("")
    md.append("| ID | Тип | Патрон | Маг | RPM | Урон | HS | Скорость | Разброс | Отдача X/Y | Перезарядка | Вес |")
    md.append("|---|---|---|---|---|---|---|---|---|---|---|---|")
    for gid in sorted(sbw):
        d = sbw[gid]
        md.append(f"| `{gid}` | {d.get('GunType','?')} | {d.get('AmmoType','?')} | {d.get('Magazine','?')} | {d.get('RPM','?')} | {d.get('Damage','?')} | {d.get('Headshot','?')} | {d.get('Velocity','?')} | {d.get('Spread','?')} | {d.get('RecoilX','?')}/{d.get('RecoilY','?')} | {d.get('NormalReloadTime','?')}/{d.get('EmptyReloadTime','?')} | {d.get('Weight','?')} |")
    md.append("")

    # 3) FCL
    fcl = fcl_guns()
    md.append("## 3. Pointblank FCL-пускачи (fcl-ext)")
    md.append("")
    md.append("| ID | Название | Урон | RPM | Патрон в ствол | Режим |")
    md.append("|---|---|---|---|---|---|")
    for gid in sorted(fcl):
        d = fcl[gid]
        if d.get("type") != "Gun":
            continue
        md.append(f"| `{gid}` | {d.get('_name','?')} | {d.get('damage','?')} | {d.get('rpm','?')} | {d.get('maxAmmoCapacity','?')} | {','.join(d.get('fireModes',['?']))} |")
    md.append("")

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(md))
    print(f"OK {OUT} | tacz: {len(guns)}, sbw: {len(sbw)}, fcl: {len(fcl)}")


if __name__ == "__main__":
    main()
