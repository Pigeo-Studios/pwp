# -*- coding: utf-8 -*-
# Генератор справочника ID предметов: Pointblank (FCL) / Pointblank / SuperbWarfare.
# Читает lang-файлы модов из Pwpfiles/PWP-Server, пишет docs/GUN_ID_REFERENCE.md.
# Запуск: py -3 tools/generate_gun_reference.py
import zipfile, json, os, collections

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PWFILES = r"C:\Users\maska\OneDrive\Desktop\Pwpfiles"
SERVER = r"C:\Users\maska\OneDrive\Desktop\PWP-Server"

FCL_ZIP = os.path.join(PWFILES, "pointblank", "fcl-ext-0.1.zip")
PB_JAR = os.path.join(SERVER, "mods", "pointblank-forge-1.20.1-1.11.1.jar")
SBW_JAR = os.path.join(SERVER, "mods", "superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar")
OUT = os.path.join(BASE, "docs", "GUN_ID_REFERENCE.md")


def lang_of(zip_path, lang_path):
    with zipfile.ZipFile(zip_path) as z:
        return json.loads(z.read(lang_path))


def item_names(lang, prefix):
    out = {}
    for k, v in lang.items():
        if k.startswith(prefix) and isinstance(v, str):
            out[k[len(prefix):]] = v
    return out


def used_in_kits():
    used = set()
    kits_dir = os.path.join(BASE, "maps", "kits")
    for f in os.listdir(kits_dir):
        if not f.endswith(".json"):
            continue
        with open(os.path.join(kits_dir, f), encoding="utf-8") as fh:
            for k in json.load(fh):
                for it in json.loads(k["items"]):
                    used.add(it["item"]["id"])
    return used


def table(rows, ns, used):
    out = ["| ID | Название |", "|---|---|"]
    for iid, name in rows:
        u = " ✔" if f"{ns}:{iid}" in used else ""
        out.append(f"| `{ns}:{iid}` | {name}{u} |")
    return "\n".join(out)


def section(title, ids, pool, ns, used):
    rows = [(i, pool[i]) for i in ids if i in pool]
    if not rows:
        return ""
    return f"### {title}\n\n{table(rows, ns, used)}\n"


# FCL: пускачи (item id из assets/pointblank/items/)
fcl_ids = []
with zipfile.ZipFile(FCL_ZIP) as z:
    prefix = "assets/pointblank/items/"
    for n in z.namelist():
        if n.startswith(prefix) and n.endswith(".json"):
            fcl_ids.append(n[len(prefix):-5])
fcl_ids.sort()
fcl_lang = lang_of(FCL_ZIP, "assets/pointblank/lang/en_us.json")
fcl_names = item_names(fcl_lang, "item.pointblank.")

pb_items = item_names(lang_of(PB_JAR, "assets/pointblank/lang/en_us.json"), "item.pointblank.")
sbw_items = item_names(lang_of(SBW_JAR, "assets/superbwarfare/lang/en_us.json"), "item.superbwarfare.")

PB_CAT = collections.OrderedDict()
PB_CAT["Штурмовые винтовки (AR)"] = ["m4a1", "m4a1mod1", "m4sopmodii", "m16a1", "hk416", "ak47", "ak74", "ak12", "an94", "aug", "g36c", "g36k", "g3", "g41", "scarl", "star15", "xm7", "xm3", "ar57", "sl8"]
PB_CAT["DMR"] = ["mk14ebr", "uar10", "wa2000"]
PB_CAT["Пулемёты (MG)"] = ["m249", "mk48", "lamg", "m134minigun", "aughbar"]
PB_CAT["Снайперки (SR)"] = ["l96a1", "ballista", "gm6lynx", "c14", "m950"]
PB_CAT["Дробовики (SG)"] = ["m1014", "m870", "m590", "spas12", "hs12", "citoricxs", "aa12"]
PB_CAT["Пистолеты-пулемёты (PDW)"] = ["tmp", "mp5", "mp7", "p90", "ro635", "ump45", "vector"]
PB_CAT["Пистолеты (HG)"] = ["m1911a1", "m9", "glock17", "glock18", "deserteagle", "mk23", "p30l", "rhino", "tti_viper"]
PB_CAT["Гранатомёты / ПТРК"] = ["at4", "smaw", "javelin", "m32mgl", "gp25", "m203launcher", "fn40", "m870modshotgun", "ulg99cannon", "xm29", "grenade20mm", "grenade40mm"]
PB_CAT["Прицелы"] = ["acog", "aimpoint", "aimpoint_t2", "delta", "drake_scope", "eaglescope", "hamr", "hawk_scope", "hi_red", "hi_red_zoom", "holographic", "holographic558", "holographic_em", "moa", "moa_hg", "operatorreflex", "precision_scope", "rspec", "spear", "spearblack", "specter", "srs", "wolf_scope", "cantedrail", "railriser"]
PB_CAT["Дульные устройства"] = ["ak_suppressor", "ar_suppressor", "ar_suppressor_tan", "hp_suppressor", "rf_suppressor", "sg_suppressor", "smg_suppressor", "ar_muzzlebrake", "smg_muzzlebrake", "p30l_compensator", "xm7_suppressor"]
PB_CAT["Рукоятки / приклады"] = ["foregrip", "foregrip_tan", "shortgrip", "stubbygrip", "stubbygriptan", "heragrip", "ak_romgrip", "glockstock", "m9_stock", "m16a3kit", "star15mod3receiver"]
PB_CAT["Патроны"] = ["ammo12gauge", "ammo338lapua", "ammo357", "ammo45acp", "ammo46", "ammo50ae", "ammo50bmg", "ammo545", "ammo556", "ammo57", "ammo68", "ammo762", "ammo762x51", "ammo9mm", "ammocreative", "ammolasercharge", "grenade20mm", "grenade40mm", "grenade"]
PB_CAT["Материалы / прочее"] = ["guninternals", "gunmetal_ingot", "gunmetal_mesh", "gunmetal_nugget", "motor", "processor"]

SBW_CAT = collections.OrderedDict()
SBW_CAT["ПЗРК / ПТРК"] = ["igla_9k38", "javelin", "tow_deployer", "tow_missile"]
SBW_CAT["Миномёты / арта"] = ["mortar_deployer", "mortar_shell", "mortar_shell_wp", "potion_mortar_shell", "artillery_indicator"]
SBW_CAT["Ракеты / бомбы / снаряды"] = ["javelin_missile", "medium_anti_air_missile", "medium_anti_ground_missile", "small_rocket", "micro_missile", "small_aerial_bomb", "medium_aerial_bomb", "rpg_rocket_standard", "rpg_rocket_tbg", "large_shell_ap", "large_shell_cm", "large_shell_gs", "large_shell_he", "large_shell_wp", "medium_rocket_ap", "medium_rocket_cm", "medium_rocket_he", "small_shell_aa", "small_shell_ap", "small_shell_gs", "small_shell_he"]
SBW_CAT["Мины / заряды"] = ["c4_bomb", "claymore_mine", "tm_62", "blu_43_mine", "lunge_mine", "ptkm_1r", "edd", "detonator"]
SBW_CAT["Гранаты / инструменты"] = ["hand_grenade", "rgo_grenade", "m18_smoke_grenade", "grenade_40mm", "parachute", "defuser", "crowbar", "military_shovel", "repair_tool", "thermal_imaging_goggles", "vehicle_reset_kit", "vehicle_damage_analyzer"]
SBW_CAT["Дроны / деплоеры"] = ["swarm_drone", "drone", "target_deployer", "dps_generator_deployer", "monitor"]
SBW_CAT["Штурмовые винтовки"] = ["m_4", "hk_416", "ak_47", "ak_12", "qbz_191", "qbz_95", "ql_1031", "sks", "bocek", "devotion"]
SBW_CAT["Снайперки"] = ["awm", "m_98b", "hunting_rifle", "mosin_nagant", "k_98", "marlin", "sentinel", "ntw_20", "svd", "mk_14"]
SBW_CAT["Пулемёты"] = ["m_2_hb", "m_60", "rpk", "minigun", "beast"]
SBW_CAT["ПП / дробовики"] = ["vector", "mp_5", "mp_443", "aa_12", "m_870", "taser"]
SBW_CAT["Пистолеты"] = ["glock_17", "glock_18", "m_1911"]
SBW_CAT["Спец-оружие"] = ["insidious", "secondary_cataclysm", "trachelium", "super_star_shooter", "homemade_shotgun", "m_79", "rpg"]
SBW_CAT["Патроны / аммо"] = ["handgun_ammo", "rifle_ammo", "shotgun_ammo", "sniper_ammo", "heavy_ammo", "ammo_box", "creative_ammo_box", "handgun_ammo_box", "rifle_ammo_box", "shotgun_ammo_box", "sniper_ammo_box", "ap_bullet", "he_bullet", "jhp_bullet", "incendiary_bullet", "phosphorus_flame_bullet", "poisonous_bullet", "riot_bullet", "silver_bullet", "blade_bullet", "phase_penetrating_bullet", "buckshot", "cupid_arrow", "taser_electrode"]
SBW_CAT["Броня"] = ["ru_helmet_6b47", "ru_chest_6b43", "us_helmet_pasgt", "us_chest_iotv", "ge_helmet_m_35"]

used = used_in_kits()

md = ["# Справочник ID предметов: Pointblank (FCL), SuperbWarfare", ""]
md.append("> Сгенерировано из lang-файлов модов. `✔` — предмет используется в текущих китах.")
md.append("> Регенерация: `py -3 tools/generate_gun_reference.py`")
md.append("")
md.append("## 1. Pointblank — FCL-пускачи (трубы), пак `fcl-ext-0.1.zip`")
md.append("")
md.append(table([(i, fcl_names.get(i, "?")) for i in fcl_ids], "pointblank", used))
md.append("")
md.append("## 2. Pointblank — основной мод (1.11.1)")
md.append("")
for cat, ids in PB_CAT.items():
    s = section(cat, ids, pb_items, "pointblank", used)
    if s:
        md.append(s)
md.append("## 3. SuperbWarfare (SBW 0.8.9)")
md.append("")
for cat, ids in SBW_CAT.items():
    s = section(cat, ids, sbw_items, "superbwarfare", used)
    if s:
        md.append(s)

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(md))
print(f"OK {OUT}")
