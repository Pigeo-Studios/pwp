"""PWP doctor: проверка контента без запуска игры.

Что проверяет (ошибка = в игре сломается, предупреждение = стоит посмотреть):
  * lang  — ключи Component.translatable("...pwp...") из кода есть в en_us/ru_ru/uk_ua своего jar;
            ru/uk не отстают от en.
  * kits  — maps/kits/<фракция>.json: читается, у фракции есть роли, слоты кита не повторяются,
            свои предметы (pwpwarfare:/pwp_medicine:) реально зарегистрированы, оружие tacz есть в справочнике.
  * vehicles — maps/vehicles/import.sql: у каждой фракции есть пул, сущность техники есть в справочнике,
            маркеры/предметы в багажнике зарегистрированы.
  * divisions — maps/vehicles/divisions.sql: каждая машина дивизии есть в пуле фракции (иначе DivisionPlan
            молча её пропускает), роль совпадает.
  * maps  — maps/<карта>/map_config.json: фракции карты имеют киты и пул, роли слотов техники покрыты пулом.

Запуск: py -3 tools/pwp_doctor.py   (код выхода 1, если есть ошибки)
"""

import json
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANGS = ("en_us", "ru_ru", "uk_ua")
JARS = {"pwp-core": ROOT / "pwp-core", "pwp-game": ROOT / "pwp-game"}
KNOWN_NS = {
    "minecraft", "pwpwarfare", "pwp_medicine", "tacz", "superbwarfare", "pointblank", "lrtactical",
    "warbornrenewed", "uncomplicatedfpv", "maxstuff", "gucci_attachments", "curios", "fcp", "vvp",
    "dragonrise", "sbw_ew_addon", "superb_extras", "ea", "taczaddon", "taczadditions", "survival_instinct", "cib",
    "rfp", "endlessammo",
}
PWPFILES = Path(__import__("os").environ.get("PWPFILES_DIR") or ROOT.parent / "Pwpfiles")

errors: dict[str, list[str]] = {}
warns: dict[str, list[str]] = {}


def _add(bucket, area, msg, where):
    bucket.setdefault(f"[{area}] {msg}", []).append(where)


def err(area, msg, where=""):
    _add(errors, area, msg, where)


def warn(area, msg, where=""):
    _add(warns, area, msg, where)


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


# ------------------------------------------------------------------ lang
def check_lang():
    key_re = re.compile(r'translatable(?:WithFallback)?\(\s*"([^"]+)"')
    # на клиенте оба jar: ресурсы сливаются, ключ pwp_core.* из pwp-game найдётся
    union = {l: set() for l in LANGS}
    for base in JARS.values():
        for f in (base / "src/main/resources/assets").glob("*/lang/*.json"):
            if f.stem in union:
                try:
                    union[f.stem].update(read_json(f))
                except ValueError:
                    pass
    for jar, base in JARS.items():
        langs = {l: {} for l in LANGS}
        for f in (base / "src/main/resources/assets").glob("*/lang/*.json"):
            if f.stem in langs:
                try:
                    langs[f.stem].update(read_json(f))
                except ValueError as e:
                    err("lang", f"{f.relative_to(ROOT)}: битый JSON ({e})")
        used = {}
        for src in (base / "src/main/java").rglob("*.java"):
            for m in key_re.finditer(src.read_text(encoding="utf-8", errors="replace")):
                used.setdefault(m.group(1), src)
        # pwp-logic (srcDir в этих jar) тоже отдаёт ключи — причины отказов и т.п.
        logic = ROOT / "pwp-logic/src" / ("warfare" if jar == "pwp-game" else "")
        for src in (logic.rglob("*.java") if jar == "pwp-game" else []):
            for m in re.finditer(r'"((?:pwpwarfare|gui\.pwpwarfare)\.[a-z0-9_.]+)"', src.read_text(encoding="utf-8")):
                used.setdefault(m.group(1), src)
        for key, src in sorted(used.items()):
            if "pwp" not in key or key.endswith("."):
                continue
            missing = [l for l in LANGS if key not in union[l]]
            if len(missing) == len(LANGS):
                err("lang", f"ключа «{key}» нет ни в одном языке (игрок увидит сам ключ)", str(src.relative_to(ROOT)))
            elif missing:
                warn("lang", f"«{key}» нет в {', '.join(missing)}", jar)
        en = set(langs["en_us"])
        for l in ("ru_ru", "uk_ua"):
            lag = sorted(en - set(langs[l]))
            if lag:
                warn("lang", f"в {l} нет {len(lag)} ключей en_us (напр. {', '.join(lag[:3])})", jar)


# ------------------------------------------------------------------ реестры и справочники
def registered_names() -> set[str]:
    names = set()
    for src in (ROOT / "pwp-game/src/main/java").rglob("*.java"):
        text = src.read_text(encoding="utf-8", errors="replace")
        if "DeferredRegister" not in text and ".register(\"" not in text:
            continue
        names.update(re.findall(r'\.register\(\s*"([a-z0-9_/]+)"', text))
    return names


def reference_text(*names) -> str:
    out = []
    for n in names:
        p = ROOT / "docs" / n
        if p.exists():
            out.append(p.read_text(encoding="utf-8", errors="replace"))
    return "\n".join(out)


def gunpack_ids() -> set[str]:
    """Id оружия из настоящих ганпаков: Pwpfiles/tacz (zip и папки) + jar модов-паков в Pwpfiles/mods."""
    import zipfile
    rx = re.compile(r"(?:assets|data)/([a-z0-9_]+)/(?:index/|display/|data/)?guns/([a-z0-9_]+?)(?:_display|_data)?\.json$")
    ids = set()
    def take(name):
        m = rx.search(name.replace("\\", "/"))
        if m:
            ids.add(f"{m.group(1)}:{m.group(2)}")
    for d in (PWPFILES / "tacz", PWPFILES / "mods"):
        if not d.exists():
            continue
        for f in d.iterdir():
            if f.suffix in (".zip", ".jar"):
                try:
                    with zipfile.ZipFile(f) as z:
                        for n in z.namelist():
                            take(n)
                except (OSError, zipfile.BadZipFile) as e:
                    warn("kits", f"не читается ганпак {f.name} ({e})")
            elif f.is_dir():
                for p in f.rglob("*.json"):
                    take(str(p.relative_to(f)))
    return ids


def walk_items(node, cb):
    """Обходит дерево NBT предметов: вызывает cb(id, tag) на каждом {"id":..}."""
    if isinstance(node, dict):
        if isinstance(node.get("id"), str):
            cb(node["id"], node.get("tag") or {})
        for v in node.values():
            walk_items(v, cb)
    elif isinstance(node, list):
        for v in node:
            walk_items(v, cb)


def check_item(area, where, item_id, tag, regs, guns_ref):
    ns, _, path = item_id.partition(":")
    if not path:
        err(area, f"предмет без неймспейса «{item_id}»", where)
        return
    if ns in ("pwpwarfare", "pwp_medicine") and path not in regs:
        err(area, f"предмет {item_id} не зарегистрирован в pwp-game", where)
    elif ns not in KNOWN_NS:
        warn(area, f"неизвестный мод «{ns}» ({item_id})", where)
    gun = tag.get("GunId") if isinstance(tag, dict) else None
    if gun and guns_ref and gun not in guns_ref:
        err(area, f"оружия {gun} нет ни в одном ганпаке Pwpfiles — игрок получит пустой ствол", where)


# ------------------------------------------------------------------ kits
def check_kits(regs) -> dict[str, int]:
    guns_ref = gunpack_ids()
    if not guns_ref:
        warn("kits", f"ганпаки не найдены ({PWPFILES}) — оружие китов не проверено")
    roles = {}
    for f in sorted((ROOT / "maps/kits").glob("*.json")):
        try:
            kits = read_json(f)
        except ValueError as e:
            err("kits", f"{f.name}: битый JSON ({e})")
            continue
        roles[f.stem] = len(kits)
        names = set()
        for k in kits:
            name = k.get("kitName", "?")
            where = f"{f.stem}/{name}"
            if name in names:
                err("kits", f"роль повторяется", where)
            names.add(name)
            try:
                items = json.loads(k.get("items") or "[]")
            except ValueError as e:
                err("kits", f"items не JSON ({e})", where)
                continue
            slots = [i.get("slot") for i in items]
            dup = {s for s in slots if slots.count(s) > 1}
            if dup:
                err("kits", f"слоты {sorted(dup)} заняты дважды", where)
            if not any(i.get("slot") == 0 for i in items) and k.get("category") != "SUPPORT":
                warn("kits", f"нет предмета в слоте 0 (СТВОЛ)", where)
            walk_items([i.get("item") for i in items], lambda iid, tag: check_item("kits", where, iid, tag, regs, guns_ref))
    return roles


# ------------------------------------------------------------------ SQL
def sql_rows(path: Path):
    """Кортежи VALUES (...) из INSERT: строки '...' (с '' и \\'), числа, выражения как текст."""
    text = path.read_text(encoding="utf-8-sig")
    rows, i, n = [], 0, len(text)
    while True:
        # «VALUES <пробел>» — начало списка; «VALUES(col)» в ON DUPLICATE KEY UPDATE — не он
        m = re.compile(r"\bVALUES\s").search(text, i)
        if not m:
            return rows
        i = m.end()
        while i < n:
            while i < n and text[i] in " \r\n\t,":
                i += 1
            if i >= n or text[i] != "(":
                break
            i += 1
            row, cur, depth = [], [], 0
            while i < n:
                c = text[i]
                if c == "'":
                    i += 1
                    s = []
                    while i < n:
                        if text[i] == "\\" and i + 1 < n:
                            s.append(text[i + 1]); i += 2; continue
                        if text[i] == "'" and i + 1 < n and text[i + 1] == "'":
                            s.append("'"); i += 2; continue
                        if text[i] == "'":
                            break
                        s.append(text[i]); i += 1
                    cur.append("".join(s))
                elif c == "(":
                    depth += 1; cur.append(c)
                elif c == ")" and depth:
                    depth -= 1; cur.append(c)
                elif c == "," and not depth:
                    row.append("".join(cur).strip()); cur = []
                elif c == ")":
                    row.append("".join(cur).strip()); i += 1
                    break
                else:
                    cur.append(c)
                i += 1
            rows.append(row)
            while i < n and text[i] in " \r\n\t":
                i += 1
            if i < n and text[i] == ";":
                break


def check_vehicles(regs):
    veh_ref = reference_text("VEHICLES_REFERENCE.md")
    pools = defaultdict(dict)  # faction -> vehicle_name -> category
    path = ROOT / "maps/vehicles/import.sql"
    for r in sql_rows(path):
        if len(r) < 9:
            err("vehicles", f"строка пула с {len(r)} полями: {r[:3]}")
            continue
        faction, vname, _disp, vid, *_rest = r
        category, inv = r[7], r[8]
        where = f"{faction}/{vname}"
        if vname in pools[faction]:
            err("vehicles", f"имя машины повторяется в пуле", where)
        pools[faction][vname] = category.upper()
        if veh_ref and vid not in veh_ref:
            warn("vehicles", f"сущности {vid} нет в docs/VEHICLES_REFERENCE.md", where)
        try:
            walk_items(json.loads(inv or "[]"), lambda iid, tag: check_item("vehicles", where, iid, tag, regs, ""))
        except ValueError as e:
            err("vehicles", f"inventory не JSON ({e})", where)
    return pools


def check_divisions(pools):
    path = ROOT / "maps/vehicles/divisions.sql"
    if not path.exists():
        warn("divisions", "maps/vehicles/divisions.sql нет — техника пойдёт из общего пула")
        return
    seen = defaultdict(set)
    for r in sql_rows(path):
        faction, div_id, _disp, _modes, _sizes, vehicles = r[:6]
        seen[faction].add(div_id)
        where = f"{faction}/{div_id}"
        if faction not in pools:
            err("divisions", f"у фракции нет пула техники", where)
            continue
        try:
            entries = json.loads(vehicles)
        except ValueError as e:
            err("divisions", f"vehicles не JSON ({e})", where)
            continue
        for e in entries:
            vn, role = e.get("vehicleName", ""), str(e.get("role", "")).upper()
            if vn not in pools[faction]:
                err("divisions", f"машины «{vn}» нет в пуле {faction} — DivisionPlan её пропустит", where)
            elif pools[faction][vn] != role:
                warn("divisions", f"«{vn}» в пуле {pools[faction][vn]}, в дивизии {role}", where)
    for f in pools:
        if f not in seen:
            warn("divisions", f"у фракции {f} нет дивизий — техника из общего пула")


# ------------------------------------------------------------------ maps
def check_maps(kit_roles, pools):
    for cfg in sorted((ROOT / "maps").glob("*/map_config.json")):
        name = cfg.parent.name
        try:
            m = read_json(cfg)
        except ValueError as e:
            err("maps", f"{name}: битый map_config.json ({e})")
            continue
        factions = set(m.get("availableFactions") or [])
        for t in (m.get("teams") or {}).values():
            if isinstance(t, dict) and t.get("faction"):
                factions.add(t["faction"])
        for f in sorted(factions):
            if f not in kit_roles:
                err("maps", f"{name}: у фракции {f} нет китов (maps/kits/{f}.json)")
            if f not in pools:
                warn("maps", f"{name}: у фракции {f} нет пула техники")
        slot_roles = set()
        for side in (m.get("vehicleSpawners") or {}).values():
            for s in side if isinstance(side, list) else []:
                if isinstance(s, dict) and s.get("role"):
                    slot_roles.add(str(s["role"]).upper())
        for f in sorted(factions & set(pools)):
            miss = slot_roles - set(pools[f].values())
            if miss:
                warn("maps", f"{name}: у {f} нет техники для слотов {sorted(miss)} (слоты будут пустыми)")


def main():
    regs = registered_names()
    check_lang()
    kit_roles = check_kits(regs)
    pools = check_vehicles(regs)
    check_divisions(pools)
    check_maps(kit_roles, pools)

    print(f"Фракции: киты {dict(kit_roles)}; пулы техники {{{', '.join(f'{k}: {len(v)}' for k, v in sorted(pools.items()))}}}")
    def show(tag, bucket):
        for msg, where in bucket.items():
            w = [x for x in where if x]
            tail = f" — {', '.join(w[:4])}{f' и ещё {len(w) - 4}' if len(w) > 4 else ''}" if w else ""
            print(f"{tag} {msg}{tail}")
    show("WARN ", warns)
    show("ERROR", errors)
    print(f"Итог: ошибок {len(errors)}, предупреждений {len(warns)}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.exit(main())
