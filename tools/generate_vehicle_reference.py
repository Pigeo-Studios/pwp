# -*- coding: utf-8 -*-
# Генератор справочника техники: SBW / VVP / FCP / DragonRise / SBW-EW.
# Читает lang-файлы (entity.*) модов из Pwpfiles, пишет docs/VEHICLES_REFERENCE.md.
# Запуск: py -3 tools/generate_vehicle_reference.py
import zipfile, json, os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODS = os.path.join(BASE, "..", "Pwpfiles", "mods")
OUT = os.path.join(BASE, "docs", "VEHICLES_REFERENCE.md")

JARS = [
    ("superbwarfare", "superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar"),
    ("vvp", "vvp-beta-1.20.1-0.2.1.jar"),
    ("fcp", "fcp-1.1.0.jar"),
    ("dragonrise_reforge", "[SBW089]dragonrise_reforge-1.4.1.01-hotfix1.jar"),
    ("sbw_ew_addon", "sbw_ew_addon-1.0.4.jar"),
]

# Не-техника: снаряды, мины, дроны, стационарные орудия, декор и т.п.
EXCLUDE_SUBSTR = [
    "shell", "missile", "grenade", "rocket", "bullet", "projectile", "mine",
    "bomb", "decoy", "flare", "smoke", "debris", "rope", "indicator", "marker",
    "trail", "drone", "tower", "coil", "wreck", "senpai", "wheel_chair",
    "target", "medical", "assembling", "generator", "grapeshot", "edd",
    "c4", "mortar", "claymore", "blu_43", "tm_62", "ptkm", "mk_82", "sc_50",
    "sc_250", "agm_65", "kh_39", "prismatic", "mk_42", "mle_1934", "bl_132",
    "hpj_11", "annihilator", "laser", "waveforce", "steel", "dps", "type_63",
    "plz_05", "d30", "hk_gmg", "ags_30", "tow",
]


def read_lang(jar_path, lang_path):
    """Читает lang json; при битых дубликатах ключей выживает последний."""
    with zipfile.ZipFile(jar_path) as z:
        try:
            return json.loads(z.read(lang_path))
        except ValueError:
            raw = z.read(lang_path).decode("utf-8", "replace")
            entries = []
            for line in raw.splitlines():
                line = line.strip().rstrip(",")
                if not line or line.startswith("{"):
                    continue
                if ":" not in line:
                    continue
                k, _, v = line.partition(":")
                k = k.strip().strip('"')
                v = v.strip()
                if v.endswith("}"):
                    v = v[:-1]
                if len(v) >= 2 and v[0] == '"' and v[-1] == '"':
                    v = v[1:-1]
                entries.append((k, v))
            out = {}
            for k, v in entries:
                out[k] = v
            return out


def main():
    mods_dir = os.path.normpath(MODS)
    rows = []  # (modid, entity_id, display_name)
    for modid, jar_name in JARS:
        jar_path = os.path.join(mods_dir, jar_name)
        lang_path = f"assets/{modid}/lang/en_us.json"
        if not os.path.exists(jar_path):
            rows.append((modid, "MISSING_JAR", jar_path))
            continue
        lang = read_lang(jar_path, lang_path)
        prefix = f"entity.{modid}."
        for k, v in lang.items():
            if not k.startswith(prefix):
                continue
            eid = k[len(prefix):]
            low = eid.lower()
            if any(x in low for x in EXCLUDE_SUBSTR):
                continue
            if not isinstance(v, str):
                continue
            rows.append((modid, eid, v))

    rows.sort(key=lambda r: (r[0], r[1]))

    mod_names = {
        "superbwarfare": "Superbwarfare (SBW)",
        "vvp": "VVP (аддон, вертолёты/броня)",
        "fcp": "FCP (аддон, технички/броня)",
        "dragonrise_reforge": "DragonRise Reforge (аддон SBW)",
        "sbw_ew_addon": "SBW EW Addon",
    }

    lines = [
        "# VEHICLES_REFERENCE — каталог техники",
        "",
        "Сгенерировано из lang-файлов модов (`entity.*`). Не-техника (снаряды, мины, "
        "дроны, стационарные орудия, декор) отфильтрована.",
        "",
        "**Внимание:** присутствие в списке НЕ гарантирует, что машина доделана и "
        "рабочая — финальный отбор только тестом в игре (см. `guide/103_vehicles.md`).",
        "",
    ]

    for modid in [m for m, _ in JARS]:
        sub = [r for r in rows if r[0] == modid]
        if not sub:
            continue
        missing = [r for r in sub if r[1] == "MISSING_JAR"]
        if missing:
            lines.append(f"## {mod_names.get(modid, modid)}")
            lines.append("")
            lines.append(f"> JAR не найден: `{missing[0][2]}`")
            lines.append("")
            continue
        lines.append(f"## {mod_names.get(modid, modid)}")
        lines.append("")
        lines.append("| Entity ID | Название |")
        lines.append("|---|---|")
        for _, eid, name in sub:
            lines.append(f"| `{modid}:{eid}` | {name} |")
        lines.append("")

    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    print(f"OK: {len(rows)} сущностей -> {OUT}")


if __name__ == "__main__":
    main()
