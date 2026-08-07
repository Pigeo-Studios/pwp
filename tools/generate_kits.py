#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор китов PWP (Squad-адаптация v2).
Дизайн: docs/KITS_SQUAD_DESIGN.md
Выход: maps/kits/<faction>.json (массив KitDefinition для /api/v1/kits/faction/{faction}/bulk)
       maps/kits/import.sql (INSERT ... ON DUPLICATE KEY UPDATE для прямого mysql)

v2: Squad-шаблон:
  - роли-варианты свернуты: 19 базовых ролей на фракцию (как у инсургентов), без суффиксов Iron/Red Dot/Optic/Javelin
  - альтернативы оружия — внутри кита на слотах 41-42 с маркерами __ALT__PRIMARY/__ALT__SPECIAL (выбор в меню деплоя):
    Rifleman/Officer: ред-дот + оптика; Medic: оптика (дефолт ред-дот); LMG/HMG: оптика;
    Marksman: глушитель; HAT: винтовка с ред-дотом. Альт-трубы не делаем: у всех труб разный БП
    (у LAT/HAT труба всегда одна — только стволы с одинаковым БП получают альты)
  - пистолеты только у половины ролей (нет у Rifleman/Grenadier/LAT/HAT/Sapper)
  - трубачи: винтовка (слот 0) + труба (слот 3 — СПЕЦ, фирменное оружие роли)
  - гренадёр: винтовка + M320 + 10?40-мм
  - у всех: фраг ?2, дым ?2, бинт ?2, бинокль (слот 9), лопата (кроме лидеров/пилотов/ПВО)
  - медик: бинты ?9 + аптечка, maxPerSquad 2; офицер: рация + арт-индикатор, без лопаты
  - фракционные трубы по истории; исправлены патроны (M2HB=50bmg, MRAD=ea:416barrett, P320=45acp)

Формат предметов — как в PacketSaveFactionKit (pwp-warfare):
  items = JSON-строка массива {slot, resupply, saveNbt, item:{id, Count, tag}}
  tacz-стволы: item "tacz:modern_kinetic_gun" + tag {GunId, GunFireMode, GunCurrentAmmoCount, HasBulletInBarrel, Attachment*}
  обвесы: item "tacz:attachment" + tag {AttachmentId}
  патроны: item "tacz:ammo" + tag {AmmoId}
  нож: item "lrtactical:melee" + tag {MeleeWeaponId}
  граната: item "lrtactical:throwable" + tag {ThrowableId}
"""
import json
import os
import uuid

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(BASE, "maps", "kits")

# ---------------------------------------------------------------- роли
# role: (maxPerTeam, maxPerSquad, minSquadPlayers, leader)
ROLES_META = {
    "Rifleman":         (-1, -1, 0, False),
    "Assault":          (6, 1, 0, False),
    "Officer":          (6, 1, 0, True),
    "Medic":            (10, 2, 0, False),
    "Grenadier":        (4, 1, 0, False),
    "LAT":              (6, 2, 0, False),
    "HAT":              (2, 1, 3, False),
    "LMG":              (4, 1, 0, False),
    "HMG":              (2, 1, 2, False),
    "Marksman":         (4, 1, 0, False),
    "Sniper":           (2, 1, 0, False),
    "Sapper":           (2, 1, 2, False),
    "Scout":            (4, 1, 0, False),
    "Anti_air":         (2, 1, 2, False),
    "Drone Operator":   (2, 1, 2, False),
    "Mechanic":         (4, 1, 0, False),
    "Mechanic Officer": (3, 1, 0, True),
    "Pilot":            (3, 1, 0, False),
    "Pilot Officer":    (3, 1, 0, True),
}

# Варианты китов: роль -> [(суффикс EN, подпись RU, прицел-ключ, бинокль, фрагов)]
# scope-ключ: None = му?шка, "reddot", "optic" (выбор прицела — из FACTION_SCOPES)
# Marksman "Suppressed" — как Optic + глушитель
VARIANTS = {
    "Rifleman": [("Iron", "мушка",     None,     True,  2),
                 ("Red Dot", "ред-дот", "reddot", True,  2),
                 ("Optic", "оптика",    "optic",  False, 1)],
    "Officer":  [("Iron", "мушка",     None,     True,  2),
                 ("Red Dot", "ред-дот", "reddot", True,  2),
                 ("Optic", "оптика",    "optic",  False, 1)],
    "Medic":    [("Red Dot", "ред-дот", "reddot", True,  2),
                 ("Optic", "оптика",    "optic",  False, 1)],
    "LAT":      [("Iron", "мушка",     None,     True,  2),
                 ("Optic", "оптика",    "optic",  False, 1)],
    "HAT":      [("Iron", "мушка",     None,     True,  2),
                 ("Red Dot", "ред-дот", "reddot", True,  2),
                 ("Javelin", "Джавелин", None,    False, 1)],
    "Grenadier": [("Optic", "оптика",   "optic",  True,  1)],
    "LMG":      [("Iron", "мушка",     None,     True,  2),
                 ("Optic", "оптика",    "optic",  False, 1)],
    "HMG":      [("Iron", "мушка",     None,     True,  2),
                 ("Optic", "оптика",    "optic",  False, 1)],
    "Marksman": [("Optic", "оптика",    "optic",  True,  1),
                 ("Suppressed", "с глушителем", "optic", True, 1)],
}

# Лимиты для варианта (переопределения к ROLES_META)
VARIANT_LIMITS = {
    ("Rifleman", "Optic"): (3, 3),   # maxPerSquad 3, minSquadPlayers 3
}

# ---------------------------------------------------------------- стволы
# name: (GunId, ammoId, magSize, fireMode, isAR)  isAR — можно ставить AR-сток
GUNS = {
    "M4A1":        ("tacz:m4a1",        "tacz:556x45", 30, "AUTO", True),
    "MK18 MOD":    ("maxstuff:mk18",    "tacz:556x45", 30, "AUTO", True),
    "HK416":       ("maxstuff:hk416c",  "tacz:556x45", 30, "AUTO", True),
    "G36K":        ("maxstuff:g36",     "tacz:556x45", 30, "AUTO", True),
    "SCAR-L":      ("tacz:scar_l",      "tacz:556x45", 30, "AUTO", True),
    "M249 SAW":    ("tacz:m249",        "tacz:556x45", 75, "AUTO", False),
    "АК-74М":      ("maxstuff:ak74m",   "tacz:545x39", 30, "AUTO", False),
    "АК-12":       ("maxstuff:ak12",    "tacz:545x39", 30, "AUTO", False),
    "АКС-74У":     ("maxstuff:aks74u",  "tacz:545x39", 30, "AUTO", False),
    "РПК-16":      ("maxstuff:rpk16",   "tacz:545x39", 40, "AUTO", False),
    "АК-47":       ("tacz:ak47",        "tacz:762x39", 30, "AUTO", False),
    "Тип-56":      ("cib:type56",       "tacz:762x39", 30, "AUTO", False),
    "РПК":         ("tacz:rpk",         "tacz:762x39", 40, "AUTO", False),
    "СВД":         ("cib:svd",          "tacz:762x54", 10, "SEMI", False),
    "СВ-98":       ("cib:sv98",         "tacz:762x54", 10, "SEMI", False),
    "MG-43":       ("rfp:mg43",         "tacz:308",    100, "AUTO", False),
    "PKP 6П41":    ("rfp:6p41bp",       "tacz:762x54", 100, "AUTO", False),
    "ПКП":         ("cib:pkp",          "tacz:762x54", 70, "AUTO", False),
    "Mk14 EBR":    ("tacz:mk14",        "tacz:308",    10, "SEMI", False),
    "M700":        ("tacz:m700",        "tacz:30_06",  5,  "SEMI", False),
    "AWP":         ("maxstuff:ai_awp",  "tacz:308",    10, "SEMI", False),
    "MRAD":        ("maxstuff:mrad",    "ea:416barrett", 10, "SEMI", False),
    "Z-15":        ("maxstuff:z15_a",   "tacz:545x39", 30, "AUTO", False),
    "Kar98k":      ("tacz:kar98",       "tacz:792x57", 4,  "SEMI", False),
    "ППШ-41":      ("cib:ppsh41",       "tacz:762x25", 20, "AUTO", False),
    "MP7":         ("maxstuff:mp7",     "tacz:46x30",  40, "AUTO", False),
    "MP5A5":       ("tacz:hk_mp5a5",    "tacz:9mm",    30, "AUTO", False),
    "Вектор":      ("maxstuff:vector9", "tacz:9mm",    24, "AUTO", False),
    "HK416D":      ("tacz:hk416d",      "tacz:556x45", 30, "AUTO", True),
    "MK47":        ("maxstuff:mk47",    "tacz:762x39", 30, "AUTO", False),
    "HK417":       ("maxstuff:hk417",   "tacz:308",    20, "AUTO", True),
    "MK11":        ("maxstuff:mk11",    "tacz:308",    20, "SEMI", False),
    "FN Evolys":   ("tacz:fn_evolys",   "tacz:308",    75, "AUTO", False),
    "LWMMG":       ("rfp:lwmmg",        "rfp:nm338",   75, "AUTO", False),
    "M95":         ("tacz:m95",         "tacz:50bmg",  5,  "SEMI", False),
    "MSR":         ("maxstuff:msr",     "tacz:30_06",  7,  "SEMI", False),
    "ДП-27":       ("maxstuff:dp28",    "tacz:762x54", 47, "AUTO", False),
    "СКС":         ("maxstuff:sks_wooden", "tacz:762x39", 9, "SEMI", False),
    "M320":        ("maxstuff:m320t",  "tacz:40mm",   1,  "SEMI", False),
}

PISTOLS = {
    "M1911":   ("tacz:m1911",         "tacz:45acp", 7,  "SEMI"),
    "Глок-18": ("maxstuff:glock_18c", "tacz:9mm",   17, "SEMI"),
    "Глок-17": ("tacz:glock_17",      "tacz:9mm",   17, "SEMI"),
    "P320":    ("tacz:p320",          "tacz:45acp", 12, "SEMI"),
    "CZ-75":   ("tacz:cz75",          "tacz:9mm",   16, "SEMI"),
    "M17":     ("maxstuff:m17",       "tacz:9mm",   17, "SEMI"),
}

SCOPES = {
    "T2":      "tacz:sight_t2",
    "EXPS3":   "tacz:sight_exp3",
    "Coyote":  "tacz:sight_coyote",
    "OKP-7":   "tacz:sight_okp7",
    "RMR":     "tacz:sight_rmr_dot",
    "Holosun": "gucci_attachments:scope_holosun",
    "ACOG":    "tacz:scope_acog_ta31",
    "LPVO":    "tacz:scope_lpvo_1_6",
    "MK5 HD":  "tacz:scope_mk5hd",
    "QMK-152": "tacz:scope_qmk152",
    "8x":      "tacz:scope_standard_8x",
    "98k":     "tacz:scope_98k",
    "ELCAN-4x": "tacz:scope_elcan_4x",
    "1П87":    "rfp:1p87",
}

MUZZLE_COMP = "tacz:muzzle_compensator_trident"
MUZZLE_SIL  = "tacz:muzzle_silencer_phantom_s1"   # ptilopsis — pistol-тег, не встаёт на винтовки
GRIP_VERT   = "gucci_attachments:grip_vert6"
GRIP_BIPOD  = "gucci_attachments:grip_bipod1"
STOCK_AR    = "tacz:stock_carbon_bone_c5"
STOCK_HK416 = "tacz:oem_stock_tactical"           # hk416c принимает только oem_stock_tactical
STOCKLESS_AR = {"G36K"}                            # g36 без стока (нет тега stock)

SMOKE_COLOR = 11546150

# ---------------------------------------------------------------- хелперы
# Предметы, которые мейн-блок ресапплая (AmmoBagBlock/ящики/техника) должен пополнять
RESUPPLY_IDS = {
    "tacz:ammo", "lrtactical:throwable",
    "superbwarfare:m18_smoke_grenade", "superbwarfare:rgo_grenade", "superbwarfare:hand_grenade",
    "pwp_medicine:bandage", "pwp_medicine:medkit",
    "superbwarfare:claymore_mine", "superbwarfare:tm_62", "superbwarfare:c4_bomb",
    "superbwarfare:swarm_drone",
    # дроны uncomplicated-fpv (деплой по кулдауну — пополнение даёт только предмет)
    "uncomplicatedfpv:fpv_drone", "uncomplicatedfpv:mavic_drone_no_drop", "uncomplicatedfpv:mavic_drone_with_drop",
    # ракеты пускачей (FCL/SBW)
    "pointblank:fcl_m72_rocket", "pointblank:fcl_rpg26_rocket", "pointblank:fcl_at4_rocket",
    "pointblank:fcl_rpg7v2_og7v", "pointblank:fcl_rpg7v2_pg7vm", "pointblank:fcl_rpg7v2_pg7vr",
    "pointblank:fcl_smaw_heaa", "pointblank:fcl_smaw_hedm",
    "pointblank:fcl_carlgustaf_he448", "pointblank:fcl_carlgustaf_heat551crs",
    "pointblank:fcl_carlgustaf_heat758", "pointblank:fcl_carlgustaf_hedp502",
    "pointblank:fcl_panzerfaust3_heat", "pointblank:fcl_panzerfaust3_tandem",
    "pointblank:fcl_rpg28_rocket",
    "pointblank:fcl_pf98_heat", "pointblank:fcl_pf98_he", "pointblank:fcl_pf98_hei",
    "superbwarfare:javelin_missile", "superbwarfare:medium_anti_air_missile",
    "superbwarfare:blu_43_mine", "superbwarfare:lunge_mine",
}

# Предметы, чей NBT сохраняется при смерти (пушки с обвесами/магазином, заряды, цели арты)
SAVE_NBT_IDS = {
    "tacz:modern_kinetic_gun", "superbwarfare:javelin", "superbwarfare:igla_9k38",
    "superbwarfare:repair_tool", "superbwarfare:artillery_indicator", "superbwarfare:c4_bomb",
}

def item(item_id, count=1, tag=None):
    it = {"id": item_id, "Count": count}
    if tag:
        it["tag"] = tag
    return it

def slot(idx, it):
    return {
        "slot": idx,
        "resupply": it["id"] in RESUPPLY_IDS,
        "saveNbt": it["id"] in SAVE_NBT_IDS,
        "item": it,
    }

def free_slot(items, prefer):
    used = {s["slot"] for s in items}
    i = prefer
    while i in used:
        i += 1
    return i

def tacz_gun(gun_id, mag, fire_mode, scope=None, muzzle=None, grip=None, stock=None, display_name=None):
    tag = {
        "HasBulletInBarrel": 1,
        "GunId": gun_id,
        "GunFireMode": fire_mode,
        "GunCurrentAmmoCount": mag,
    }
    if display_name:
        # Приписка к имени ствола (как в Squad: "M4A1 (Red Dot)") — видна в меню деплоя и в инвентаре
        tag["display"] = {"Name": '{"text":"%s"}' % display_name}
    def att(att_id):
        return item("tacz:attachment", 1, {"AttachmentId": att_id})
    if scope:  tag["AttachmentSCOPE"] = att(scope)
    if muzzle: tag["AttachmentMUZZLE"] = att(muzzle)
    if grip:   tag["AttachmentGRIP"] = att(grip)
    if stock:  tag["AttachmentSTOCK"] = att(stock)
    return item("tacz:modern_kinetic_gun", 1, tag)

def tacz_pistol(gun_id, mag):
    return tacz_gun(gun_id, mag, "SEMI")

def ammo(ammo_id, count):
    return item("tacz:ammo", count, {"AmmoId": ammo_id})

def frag(count=1):
    return item("lrtactical:throwable", count, {"ThrowableId": "lrtactical:m67"})

def rgo(count=1):
    return item("superbwarfare:rgo_grenade", count)

def smoke(count):
    return item("superbwarfare:m18_smoke_grenade", count, {"Color": SMOKE_COLOR})

def knife(knife_id):
    return item("lrtactical:melee", 1, {"MeleeWeaponId": knife_id})

def bandage(count=2):
    return item("pwp_medicine:bandage", count)

def sbw_gun(item_id, ammo_count=1):
    u = uuid.uuid4().int
    return item(item_id, 1, {
        "Perks": {},
        "GunData": {
            "UUID": [u >> 96 & 0xFFFFFFFF, u >> 64 & 0xFFFFFFFF, u >> 32 & 0xFFFFFFFF, u & 0xFFFFFFFF],
            "Ammo": ammo_count,
        },
        "Attachments": {},
    })

def armor(item_id):
    return item(item_id, 1, {"Damage": 0, "HideFlags": 2})

# ---------------------------------------------------------------- фракции
# roles: роль -> (ствол, прицел по умолчанию, без_пистолета, спец-тип трубы)
#   спец-тип трубы: None (нет), "LAT", "HAT", "IGLA", "M320"
FACTIONS = {
    "usa": {
        "ru": "США", "motto": "Точность и огневая мощь",
        "frag": frag, "knife": "cs2_wt:bayonet", "knife_officer": "cs2_wt:bayoauto",
        "helmet": "warbornrenewed:opscore-multicam",
        "vest": "warbornrenewed:jpc",
        "helmet_officer": "warbornrenewed:gpngv-nato-wood",
        "ghillie": "desert",
        "pistol": "M17",
        "reddot": "T2", "optic": "ACOG",
        "assault_scope": "EXPS3", "grenadier_scope": "T2",
        "lmg_scope": "Coyote", "marksman_scope": "ACOG", "sniper_scope": "QMK-152",
        "scout_scope": "Holosun", "drone_scope": "T2", "sapper_scope": "T2",
        "roles": {
            "Rifleman":         ("M4A1",    None,   True,  None),
            "Assault":          ("MK18 MOD", None,   False, None),
            "Officer":          ("M4A1",    None,   False, None),
            "Medic":            ("M4A1",    None,   False, None),
            "Grenadier":        ("M4A1",    None,   False, "M320"),
            "LAT":              ("M4A1",    None,   True,  "LAT"),
            "HAT":              ("M4A1",    None,   True,  "HAT"),
            "LMG":              ("M249 SAW", None,  False, None),
            "HMG":              ("MG-43",   None,   False, None),
            "Marksman":         ("Mk14 EBR", None,  False, None),
            "Sniper":           ("M700",    None,   False, None),
            "Sapper":           ("MK18 MOD", None,  False, None),
            "Scout":            ("MK18 MOD", None,  False, None),
            "Anti_air":         ("MK18 MOD", None,  False, "IGLA"),
            "Drone Operator":   ("MK18 MOD", None,  False, None),
            "Mechanic":         ("MP7",     None,   False, None),
            "Mechanic Officer": ("MP7",     None,   False, None),
            "Pilot":            ("MP5A5",   None,   False, None),
            "Pilot Officer":    ("MP5A5",   None,   False, None),
        },
    },
    "russia": {
        "ru": "Россия", "motto": "Огневая мощь и живучесть",
        "frag": rgo, "knife": "cs2_wt:karambit", "knife_officer": "cs2_wt:karobsidian",
        "helmet": "warbornrenewed:6b47-emr",
        "vest": "warbornrenewed:6b45-wood",
        "helmet_officer": "warbornrenewed:ratnik-10t-wood",
        "ghillie": "winter",
        "pistol": "Глок-18",
        "reddot": "OKP-7", "optic": "1П87",
        "assault_scope": "1П87", "grenadier_scope": "OKP-7",
        "lmg_scope": "OKP-7", "marksman_scope": "ELCAN-4x", "sniper_scope": "8x",
        "scout_scope": None, "drone_scope": "OKP-7", "sapper_scope": "OKP-7",
        "roles": {
            "Rifleman":         ("АК-74М",  None,   True,  None),
            "Assault":          ("АК-12",   None,   False, None),
            "Officer":          ("АК-12",   None,   False, None),
            "Medic":            ("АК-74М",  None,   False, None),
            "Grenadier":        ("АК-74М",  None,   False, "M320"),
            "LAT":              ("АК-74М",  None,   True,  "LAT"),
            "HAT":              ("АК-74М",  None,   True,  "HAT"),
            "LMG":              ("РПК-16",  None,   False, None),
            "HMG":              ("PKP 6П41", None,  False, None),
            "Marksman":         ("СВД",     None,   False, None),
            "Sniper":           ("СВ-98",   None,   False, None),
            "Sapper":           ("АКС-74У", None,   False, None),
            "Scout":            ("АКС-74У", None,   False, None),
            "Anti_air":         ("АКС-74У", None,   False, "IGLA"),
            "Drone Operator":   ("АКС-74У", None,   False, None),
            "Mechanic":         ("АКС-74У", None,   False, None),
            "Mechanic Officer": ("АКС-74У", None,   False, None),
            "Pilot":            ("АКС-74У", None,   False, None),
            "Pilot Officer":    ("АКС-74У", None,   False, None),
        },
    },
    "ukraine": {
        "ru": "Украина", "motto": "Универсальность",
        "frag": rgo, "knife": "cs2_wt:m9", "knife_officer": "cs2_wt:m9emerald",
        "helmet": "warbornrenewed:opscore-mm14",
        "vest": "warbornrenewed:warmor-mm14",
        "helmet_officer": "warbornrenewed:gpngv-nato-wood",
        "ghillie": "jungle",
        "pistol": "Глок-17",
        "reddot": "OKP-7", "optic": "1П87",
        "assault_scope": "EXPS3", "grenadier_scope": "OKP-7",
        "lmg_scope": "OKP-7", "marksman_scope": "ELCAN-4x", "sniper_scope": "8x",
        "scout_scope": None, "drone_scope": "OKP-7", "sapper_scope": "OKP-7",
        "roles": {
            "Rifleman":         ("АК-74М",  None,   True,  None),
            "Assault":          ("HK416D",  None,   False, None),
            "Officer":          ("HK416D",  None,   False, None),
            "Medic":            ("АК-74М",  None,   False, None),
            "Grenadier":        ("АК-74М",  None,   False, "M320"),
            "LAT":              ("АК-74М",  None,   True,  "LAT"),
            "HAT":              ("АК-74М",  None,   True,  "HAT"),
            "LMG":              ("РПК-16",  None,   False, None),
            "HMG":              ("PKP 6П41", None,  False, None),
            "Marksman":         ("Mk14 EBR", None, False, None),
            "Sniper":           ("M95",     None,   False, None),
            "Sapper":           ("Z-15",    None,   False, None),
            "Scout":            ("Z-15",    None,   False, None),
            "Anti_air":         ("Z-15",    None,   False, "IGLA"),
            "Drone Operator":   ("Z-15",    None,   False, None),
            "Mechanic":         ("Z-15",    None,   False, None),
            "Mechanic Officer": ("Z-15",    None,   False, None),
            "Pilot":            ("Z-15",    None,   False, None),
            "Pilot Officer":    ("Z-15",    None,   False, None),
        },
    },
    "nato": {
        "ru": "НАТО", "motto": "Дисциплина и техника",
        "frag": frag, "knife": "cs2_wt:m9", "knife_officer": "cs2_wt:buemerald",
        "helmet": "warbornrenewed:nato-wood-helmet",
        "vest": "warbornrenewed:nato-wood-chestplate",
        "helmet_officer": "warbornrenewed:gpngv-nato-wood",
        "ghillie": "jungle",
        "pistol": "P320",
        "reddot": "T2", "optic": "ACOG",
        "assault_scope": "ACOG", "grenadier_scope": "T2",
        "lmg_scope": "Coyote", "marksman_scope": "ACOG", "sniper_scope": "MK5 HD",
        "scout_scope": "Holosun", "drone_scope": "T2", "sapper_scope": "T2",
        "roles": {
            "Rifleman":         ("G36K",    None,   True,  None),
            "Assault":          ("SCAR-L",  None,   False, None),
            "Officer":          ("G36K",    None,   False, None),
            "Medic":            ("G36K",    None,   False, None),
            "Grenadier":        ("G36K",    None,   False, "M320"),
            "LAT":              ("G36K",    None,   True,  "LAT"),
            "HAT":              ("G36K",    None,   True,  "HAT"),
            "LMG":              ("FN Evolys", None, False, None),
            "HMG":              ("LWMMG",   None,   False, None),
            "Marksman":         ("HK417",   None,   False, None),
            "Sniper":           ("AWP",     None,   False, None),
            "Sapper":           ("MP5A5",   None,   False, None),
            "Scout":            ("MP7",     None,   False, None),
            "Anti_air":         ("MP7",     None,   False, "IGLA"),
            "Drone Operator":   ("MP7",     None,   False, None),
            "Mechanic":         ("MP7",     None,   False, None),
            "Mechanic Officer": ("MP7",     None,   False, None),
            "Pilot":            ("MP5A5",   None,   False, None),
            "Pilot Officer":    ("MP5A5",   None,   False, None),
        },
    },
    "insurgency": {
        "ru": "Инсургенты", "motto": "Ржавый, но живучий",
        "frag": rgo, "knife": "cs2_wt:stiletto", "knife_officer": "cs2_wt:stiletto",
        "helmet": "warbornrenewed:pastgt-black",
        "vest": "warbornrenewed:iotv-black",
        "helmet_officer": "warbornrenewed:pastgt-black",
        "ghillie": "jungle",
        "pistol": "CZ-75",
        "reddot": None, "optic": None,
        "assault_scope": None, "grenadier_scope": None,
        "lmg_scope": None, "marksman_scope": "ПСО-1", "sniper_scope": None,
        "scout_scope": None, "drone_scope": None, "sapper_scope": None,
        # роли без брони (бомжи)
        "no_armor_roles": {"Rifleman", "Assault", "Grenadier", "Scout", "Anti_air",
                           "Drone Operator", "Mechanic", "Mechanic Officer", "Pilot", "Pilot Officer"},
        "roles": {
            "Rifleman":         ("АК-47",   None,   False, None),
            "Assault":          ("Тип-56",  None,   False, None),
            "Officer":          ("АК-47",   "1П87", False, None),
            "Medic":            ("АК-47",   None,   False, None),
            "Grenadier":        ("АК-47",   None,   False, "M320"),
            "LAT":              ("АК-47",   None,   True,  "LAT"),
            "HAT":              ("АК-47",   None,   True,  "HAT"),
            "LMG":              ("РПК",     None,   False, None),
            "HMG":              ("ПКП",     None,   False, None),
            "Marksman":         ("СКС",     "ELCAN-4x", False, None),
            "Sniper":           ("Kar98k",  "98k",  False, None),
            "Sapper":           ("ППШ-41",  None,   False, None),
            "Scout":            ("АКС-74У", None,   False, None),
            "Anti_air":         ("АКС-74У", None,   False, "IGLA"),
            "Drone Operator":   ("АКС-74У", None,   False, None),
            "Mechanic":         ("ППШ-41",  None,   False, None),
            "Mechanic Officer": ("ППШ-41",  None,   False, None),
            "Pilot":            ("ППШ-41",  None,   False, None),
            "Pilot Officer":    ("ППШ-41",  None,   False, None),
        },
    },
    "pmc": {
        "ru": "ЧВК", "motto": "Профи и дорого",
        "frag": frag, "knife": "cs2_wt:talon", "knife_officer": "cs2_wt:m9sapphire",
        "helmet": "warbornrenewed:opscore-black",
        "vest": "warbornrenewed:warmor-black",
        "helmet_officer": "warbornrenewed:opscore-fc-b2200-voevoda",
        "ghillie": "desert",
        "pistol": "M17",
        "reddot": "T2", "optic": "LPVO",
        "assault_scope": "EXPS3", "grenadier_scope": "T2",
        "lmg_scope": "Coyote", "marksman_scope": "LPVO", "sniper_scope": "QMK-152",
        "scout_scope": "Holosun", "drone_scope": "T2", "sapper_scope": "T2",
        "roles": {
            "Rifleman":         ("HK416",   None,   True,  None),
            "Assault":          ("MK47",    None,   False, None),
            "Officer":          ("HK416",   None,   False, None),
            "Medic":            ("SCAR-L",  None,   False, None),
            "Grenadier":        ("HK416",   None,   False, "M320"),
            "LAT":              ("HK416",   None,   True,  "LAT"),
            "HAT":              ("HK416",   None,   True,  "HAT"),
            "LMG":              ("M249 SAW", None,  False, None),
            "HMG":              ("MG-43",   None,   False, None),
            "Marksman":         ("MK11",    None,   False, None),
            "Sniper":           ("MSR",     None,   False, None),
            "Sapper":           ("MP7",     None,   False, None),
            "Scout":            ("Вектор",  None,   False, None),
            "Anti_air":         ("MP7",     None,   False, "IGLA"),
            "Drone Operator":   ("MP7",     None,   False, None),
            "Mechanic":         ("Вектор",  None,   False, None),
            "Mechanic Officer": ("Вектор",  None,   False, None),
            "Pilot":            ("MP7",     None,   False, None),
            "Pilot Officer":    ("MP7",     None,   False, None),
        },
    },
}

# ---------------------------------------------------------------- трубы
# LAT/HAT по фракциям (винтовка в слоте 0, труба в слоте 3 — СПЕЦ)
LAT_LAUNCHERS = {
    "usa":       {"Iron": "AT4",           "Optic": "M72 LAW"},
    "nato":      {"Iron": "M72 LAW",       "Optic": "Panzerfaust 3"},
    "pmc":       {"Iron": "M72 LAW",       "Optic": "RPG-28"},
    "russia":    {"Iron": "РПГ-26",        "Optic": "РПГ-26"},
    "ukraine":   {"Iron": "РПГ-26",        "Optic": "РПГ-26"},
    "insurgency": {"Iron": "РПГ-26",       "Optic": "PF-98"},
}

HAT_LAUNCHERS = {
    "usa":       ("Карл Густав M4", "CG"),
    "pmc":       ("Карл Густав M4", "CG"),
    "nato":      ("SMAW", "SMAW"),
    "russia":    ("РПГ-7В2", "PG7"),
    "ukraine":   ("РПГ-7В2", "PG7"),
    "insurgency": ("РПГ-7В2 INS", "PG7_INS"),
}

# name -> (основной предмет, ракеты[(id, count)...], прицел)
TUBES = {
    "M72 LAW":        ("pointblank:fcl_m72",            [("pointblank:fcl_m72_rocket", 1)],                  None),
    "РПГ-26":         ("pointblank:fcl_rpg26",          [("pointblank:fcl_rpg26_rocket", 1)],                None),
    "AT4":            ("pointblank:fcl_at4",            [("pointblank:fcl_at4_rocket", 1)],                  None),
    "РПГ-7В2":        ("pointblank:fcl_rpg7v2",         [("pointblank:fcl_rpg7v2_pg7vm", 1),
                                                         ("pointblank:fcl_rpg7v2_pg7vr", 1)],                 "pointblank:fcl_pgo7"),
    "РПГ-7В2 INS":    ("pointblank:fcl_rpg7v2",         [("pointblank:fcl_rpg7v2_og7v", 1),
                                                         ("pointblank:fcl_rpg7v2_pg7vm", 1)],                None),
    "SMAW":           ("pointblank:fcl_smaw",           [("pointblank:fcl_smaw_heaa", 1),
                                                         ("pointblank:fcl_smaw_hedm", 1)],                   "pointblank:fcl_smaw_scope"),
    "Карл Густав M4": ("pointblank:fcl_carlgustafm4",   [("pointblank:fcl_carlgustaf_heat551crs", 1),
                                                         ("pointblank:fcl_carlgustaf_hedp502", 1)],          "pointblank:fcl_carlgustaf_scope"),
    "Panzerfaust 3":  ("pointblank:fcl_panzerfaust3",   [("pointblank:fcl_panzerfaust3_heat", 1),
                                                         ("pointblank:fcl_panzerfaust3_tandem", 1)],         None),
    "RPG-28":         ("pointblank:fcl_rpg28",          [("pointblank:fcl_rpg28_rocket", 1)],               None),
    "PF-98":          ("pointblank:fcl_pf98",           [("pointblank:fcl_pf98_heat", 1),
                                                         ("pointblank:fcl_pf98_he", 1)],                    None),
    "Javelin":        ("superbwarfare:javelin",         [("superbwarfare:javelin_missile", 2)],              None),
    "IGLA":           ("superbwarfare:igla_9k38",       [("superbwarfare:medium_anti_air_missile", 2)],      None),
}

# HAT INS: ОГ?1 + ПГ-7ВМ?1 + ПГ-7ВР?1 + прицел (как Squad INS)
TUBES["РПГ-7В2 INS HAT"] = ("pointblank:fcl_rpg7v2", [("pointblank:fcl_rpg7v2_og7v", 1),
                                                      ("pointblank:fcl_rpg7v2_pg7vm", 1),
                                                      ("pointblank:fcl_rpg7v2_pg7vr", 1)], "pointblank:fcl_pgo7")

# Категории ролей (как в Squad: 4 типа; FIRE_SUPPORT лимитируется 3 на отряд)
# LMG (Optic) — 2-й пулемётчик = огневая поддержка (как 2-й AR в Squad)
CATEGORY = {
    "Rifleman": "DIRECT_COMBAT", "Assault": "DIRECT_COMBAT",
    "Officer": "SUPPORT", "Medic": "SUPPORT",
    "Grenadier": "FIRE_SUPPORT", "LAT": "FIRE_SUPPORT",
    "HAT": "SPECIALIST", "LMG": "DIRECT_COMBAT", "HMG": "SPECIALIST",
    "Marksman": "FIRE_SUPPORT", "Sniper": "SPECIALIST", "Sapper": "SPECIALIST",
    "Scout": "SUPPORT", "Anti_air": "SUPPORT", "Drone Operator": "SUPPORT",
    "Mechanic": "SUPPORT", "Mechanic Officer": "SUPPORT",
    "Pilot": "SUPPORT", "Pilot Officer": "SUPPORT",
}

# ---------------------------------------------------------------- сборка кита
LIGHT_ROLES = {"Scout", "Pilot", "Pilot Officer", "Mechanic", "Mechanic Officer",
               "Drone Operator", "Anti_air"}

ROLE_RU = {
    "Rifleman": "Стрелок", "Assault": "Штурмовик", "Officer": "Офицер", "Medic": "Медик",
    "Grenadier": "Гренадёр", "LAT": "ЛАТ", "HAT": "ХАТ", "LMG": "Пулемётчик", "HMG": "Тяжёлый пулемёт",
    "Marksman": "Марксман", "Sniper": "Снайпер", "Sapper": "Сапёр", "Scout": "Разведчик",
    "Anti_air": "ПВО", "Drone Operator": "Оператор дрона", "Mechanic": "Механик",
    "Mechanic Officer": "Механик-офицер", "Pilot": "Пилот", "Pilot Officer": "Пилот-офицер",
}

DESC_PHRASE = {
    "Rifleman": "основа отряда — держит линию огня и снабжает бойцов патронами",
    "Assault": "штурм в первом эшелоне — прорывает оборону противника в ближнем бою",
    "Officer": "командир отряда — ставит рали, держит рацию и вызывает арту",
    "Medic": "спасает жизни — бинты, аптечка и подъём бойцов с земли",
    "Grenadier": "40-мм поддержка — накрывает позиции противника из гранатомёта",
    "LAT": "лёгкая противотанковая — одноразовая труба против лёгкой техники",
    "HAT": "охота на технику — тяжёлая труба с кумулятивными боеприпасами",
    "LMG": "подавляющий огонь — длинная очередь держит противника в укрытии",
    "HMG": "тяжёлая огневая точка — крупный калибр против брони и укреплений",
    "Marksman": "точные выстрелы на дистанции — прикрывает отряд с дальней позиции",
    "Sniper": "одна пуля — одна цель — работает из глубокого тыла",
    "Sapper": "мины и подрывы — ставит мины и сносит вражеские постройки",
    "Scout": "глаза отряда — разведывательный дрон и скрытное продвижение",
    "Anti_air": "защита неба — ПЗРК сбивает вражескую воздушную технику",
    "Drone Operator": "разведка с воздуха — FPV-камикадзе и мавик со сбросом",
    "Mechanic": "ремонт техники — чинит и обслуживает машины на поле боя",
    "Mechanic Officer": "руководит ремонтами — лидер экипажей и ремзоны",
    "Pilot": "управляет небом — вертолёты и воздушная поддержка",
    "Pilot Officer": "командир экипажа — ведёт группу воздушной техники",
}

def ammo_stacks_for(role, gun_name):
    if role in ("LMG",):
        return 6
    if role == "HMG":
        return 8
    if role in ("Marksman", "Sniper"):
        return 2
    if role in ("Mechanic", "Mechanic Officer", "Pilot", "Pilot Officer"):
        return 3
    if gun_name in ("ППШ-41", "MP7", "MP5A5", "Вектор"):
        return 3
    return 4

def main_scope(faction, role, variant, fixed_scope):
    # fixed_scope — прицел из конфига роли (для ролей без вариантов: офицер INS, марксман INS и т.д.)
    if fixed_scope:
        return SCOPES[fixed_scope]
    if role in ("Assault", "Sapper", "Scout", "Drone Operator"):
        key = role.lower().replace(" ", "_")
        sk = {"assault": faction["assault_scope"], "sapper": faction["sapper_scope"],
              "scout": faction["scout_scope"], "drone_operator": faction["drone_scope"]}[key]
        return SCOPES[sk] if sk else None
    if role == "Grenadier":
        # вариант Optic — прицел крупнее (ACOG/1П87), базовый — ред-дот
        sk = faction["optic"] if variant == "Optic" else faction["grenadier_scope"]
        return SCOPES[sk] if sk else None
    if role == "LMG":
        # как в Squad: 1-й пулемётчик без оптики, 2-й (Optic) — с прицелом
        if variant == "Optic":
            sk = faction["lmg_scope"]
            return SCOPES[sk] if sk else None
        return None
    if role == "HMG":
        # Iron-вариант HMG: без прицела; Optic — ACOG-стиль (для Запада 1П87 для РФ — через optic-ключ)
        if variant == "Optic":
            return SCOPES[faction["optic"]]
        return None
    if role == "Marksman":
        return SCOPES[faction["marksman_scope"]]
    if role == "Sniper":
        return SCOPES[faction["sniper_scope"]]
    if role == "Medic" and variant is None and faction.get("reddot"):
        # свернутый кит: медик по умолчанию с ред-дотом (как старый вариант Red Dot)
        return SCOPES[faction["reddot"]]
    # роли с вариантами (Rifleman/Officer/Medic/LAT/HAT): ключ варианта
    if variant == "Red Dot":
        return SCOPES[faction["reddot"]]
    if variant == "Optic":
        return SCOPES[faction["optic"]]
    if variant == "Suppressed":
        return SCOPES[faction["marksman_scope"]]
    return None

def build_kit(faction_key, faction, role, variant, variant_ru):
    meta = ROLES_META[role]
    r = faction["roles"][role]
    gun_name, fixed_scope, no_pistol, tube_type = r
    suppressed = variant == "Suppressed" or role in ("Sapper", "Scout") or (role == "Assault" and faction_key != "insurgency")
    scope_id = main_scope(faction, role, variant, fixed_scope)
    items = []
    special_name = None  # труба/M320/Игла для строки «Оружие»

    # --- основное (слот 0)
    if gun_name == "Igla":
        main, rockets, sight = TUBES["IGLA"]
        items.append(slot(0, sbw_gun(main)))
        items.append(slot(9, item(rockets[0][0], rockets[0][1])))
        has_launcher = True
        launcher = "IGLA"
    else:
        gid, ammo_id, mag, mode, is_ar = GUNS[gun_name]
        silencer = suppressed and gun_name not in ("ППШ-41", "АКС-74У")
        is_mg = role in ("LMG", "HMG")
        vector = gun_name == "Вектор"
        # Вектор без атачментов слаб: ставим разрешённые (maxstuff:a3_grip / supressed_brake)
        muzzle = "maxstuff:supressed_brake" if vector else (MUZZLE_SIL if silencer else (MUZZLE_COMP if (is_ar or is_mg) else None))
        grip = GRIP_BIPOD if (is_mg and gun_name != "LWMMG") else ("maxstuff:a3_grip" if vector else (GRIP_VERT if (is_ar or gun_name in STOCKLESS_AR) else None))
        stock = STOCK_HK416 if gun_name == "HK416" else (STOCK_AR if (is_ar and gun_name not in STOCKLESS_AR) else None)
        items.append(slot(0, tacz_gun(gid, mag, mode, scope_id, muzzle, grip, stock)))
        has_launcher = tube_type is not None
        launcher = tube_type

    # --- альтернативы оружия внутри кита (слоты 41+, __ALT__): выбор в меню деплоя
    # Роли-варианты (Iron/Red Dot/Optic/Javelin) свернуты: одна роль, альты пушек/труб внутри.
    # Альт-стволы получают приписку к имени (как в Squad): "M4A1 (Red Dot)", "M4A1 (Optic)"
    slot_skins = {}
    alt_names = []
    desc_alts = []
    if gun_name != "Igla" and faction_key != "insurgency":

        def alt_primary(scope_id, slot_idx, suffix, desc_ru=None):
            items.append(slot(slot_idx, tacz_gun(gid, mag, mode, scope_id, muzzle, grip, stock,
                                                 display_name=f"{gun_name} ({suffix})")))
            slot_skins[str(slot_idx)] = ["__ALT__PRIMARY"]
            alt_names.append(suffix)
            if desc_ru:
                desc_alts.append(desc_ru)

        if role in ("Rifleman", "Officer"):
            # альтернативы прицелов: ред-дот + оптика (дефолт — мушка)
            if faction.get("reddot"):
                alt_primary(SCOPES[faction["reddot"]], 41, "Red Dot", "ред-дот")
            if faction.get("optic"):
                alt_primary(SCOPES[faction["optic"]], 42, "Optic", "оптика")
        elif role == "Medic":
            # дефолт — ред-дот (см. main_scope), альтернатива — оптика
            if faction.get("optic"):
                alt_primary(SCOPES[faction["optic"]], 41, "Optic", "оптика")
        elif role == "LMG":
            # дефолт — без прицела, альтернатива — оптика (2-й пулемётчик)
            if faction.get("lmg_scope"):
                alt_primary(SCOPES[faction["lmg_scope"]], 41, "Optic", "оптика")
        elif role == "HMG":
            if faction.get("optic"):
                alt_primary(SCOPES[faction["optic"]], 41, "Optic", "оптика")
        elif role == "Marksman":
            # альтернатива — с глушителем
            items.append(slot(41, tacz_gun(gid, mag, mode, SCOPES[faction["marksman_scope"]], MUZZLE_SIL, grip, stock,
                                           display_name=f"{gun_name} (Suppressed)")))
            slot_skins["41"] = ["__ALT__PRIMARY"]
            alt_names.append("Suppressed")
            desc_alts.append("с глушителем")
        elif role == "HAT":
            # альтернатива — винтовка с ред-дотом (труба одна, вариант Iron)
            if faction.get("reddot"):
                alt_primary(SCOPES[faction["reddot"]], 41, "Red Dot", "ред-дот")

    # --- труба / M320 / Игла (слот 3 — СПЕЦ, как в Squad: фирменное оружие роли) / пистолет
    pistol_ammo_id = None
    if has_launcher and gun_name != "Igla":
        if launcher == "M320":
            # M320 — maxstuff-пушка (tacz:m320 не существует): tacz-предмет modern_kinetic_gun + GunId
            items.append(slot(3, tacz_gun("maxstuff:m320t", 1, "SEMI")))
            special_name = "M320"
        elif launcher == "IGLA":
            main, rockets, sight = TUBES["IGLA"]
            items.append(slot(3, sbw_gun(main)))
            items.append(slot(9, item(rockets[0][0], rockets[0][1])))
            # ПВО несёт пистолет в вторичке (как раньше, когда Игла была на слоте 0)
            pid, pam, pmag, _ = PISTOLS[faction["pistol"]]
            items.append(slot(1, tacz_pistol(pid, pmag)))
            pistol_ammo_id = pam
            special_name = "Игла"
        else:
            tube_name = None
            if launcher == "LAT":
                tube_name = LAT_LAUNCHERS[faction_key][variant or "Iron"]
            else:
                if variant == "Javelin":
                    tube_name = "Javelin"
                else:
                    tube_name, hat_kind = HAT_LAUNCHERS[faction_key]
                    if hat_kind == "PG7_INS":
                        tube_name = "РПГ-7В2 INS HAT"
            special_name = "РПГ-7В2" if tube_name == "РПГ-7В2 INS HAT" else tube_name
            main, rockets, sight = TUBES[tube_name]
            if tube_name == "Javelin":
                # SBW-формат (GunData UUID), как у Иглы
                items.append(slot(3, sbw_gun(main)))
            else:
                items.append(slot(3, item(main, 1)))
            for i, (rid, cnt) in enumerate(rockets):
                items.append(slot(6 + i, item(rid, cnt)))
            if sight:
                items.append(slot(free_slot(items, 9), item(sight, 1)))
    elif not no_pistol and gun_name != "Igla":
        pid, pam, pmag, _ = PISTOLS[faction["pistol"]]
        items.append(slot(1, tacz_pistol(pid, pmag)))
        pistol_ammo_id = pam
    elif gun_name == "Igla":
        pid, pam, pmag, _ = PISTOLS[faction["pistol"]]
        items.append(slot(1, tacz_pistol(pid, pmag)))
        pistol_ammo_id = pam

    # --- слоты 2-8 (снаряга)
    # фрагов по варианту: оптические/глушёные варианты несут на один меньше
    frags = 2 if variant not in ("Optic", "Suppressed", "Javelin") else 1
    if role in ("Sniper", "Marksman"):
        frags = 1

    items.append(slot(2, faction["frag"](frags)))

    if role == "Officer":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, item("pwpwarfare:squad_leader_radio", 1)))
        items.append(slot(6, item("superbwarfare:artillery_indicator", 1)))
        items.append(slot(7, bandage(2)))
        items.append(slot(8, knife(faction["knife_officer"])))
        if variant is None or variant in ("Iron", "Red Dot"):
            items.append(slot(9, item("warbornrenewed:binocular", 1)))
        # без лопаты
    elif role == "Medic":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(9)))
        items.append(slot(6, item("pwp_medicine:medkit", 1)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        if variant is None or variant == "Red Dot":
            items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Sapper":
        # мины — фирменное снаряжение сапёра (СПЕЦ, слот 3), дым и бинт сдвигаются
        if faction_key in ("usa", "nato", "pmc"):
            items.append(slot(3, item("superbwarfare:claymore_mine", 2)))       # M18A1 — западное
            items.append(slot(6, item("superbwarfare:blu_43_mine", 2)))         # BLU-43B Dragontooth вместо советской ТМ-62
        elif faction_key in ("russia", "ukraine"):
            items.append(slot(3, item("superbwarfare:tm_62", 3)))               # 3?ТМ-62, чисто советский арсенал
        else:  # insurgency
            items.append(slot(3, item("superbwarfare:tm_62", 1)))               # меньше, "бедная" фракция
            items.append(slot(6, item("superbwarfare:lunge_mine", 2)))          # импровизированная ПТ-мина, флейвор
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(2)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        items.append(slot(9, item("superbwarfare:detonator", 1)))
        items.append(slot(10, item("warbornrenewed:binocular", 1)))
        if faction_key != "insurgency":
            items.append(slot(11, item("superbwarfare:repair_tool", 1, {"Energy": 100000})))
        c4_count = 1 if faction_key == "insurgency" else 2
        items.append(slot(33, item("superbwarfare:c4_bomb", c4_count, {"Control": 1})))
    elif role == "Mechanic" or role == "Mechanic Officer":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("superbwarfare:repair_tool", 1, {"Energy": 100000})))
        items.append(slot(8, knife(faction["knife_officer"] if role == "Mechanic Officer" else faction["knife"])))
        if role == "Mechanic Officer":
            items.append(slot(6, item("pwpwarfare:squad_leader_radio", 1)))
            items.append(slot(7, smoke(2)))
            items.append(slot(9, item("warbornrenewed:binocular", 1)))
            # без лопаты
        else:
            items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
    elif role == "Pilot" or role == "Pilot Officer":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("superbwarfare:repair_tool", 1, {"Energy": 100000})))
        items.append(slot(8, knife(faction["knife_officer"] if role == "Pilot Officer" else faction["knife"])))
        if role == "Pilot Officer":
            items.append(slot(6, item("pwpwarfare:squad_leader_radio", 1)))
        # без лопаты
    elif role == "Scout":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("uncomplicatedfpv:mavic_drone_no_drop", 1)))  # развед-мавик
        items.append(slot(6, item("superbwarfare:monitor", 1)))                # пульт управления
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Drone Operator":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("uncomplicatedfpv:fpv_drone", 1)))           # FPV-камикадзе
        items.append(slot(6, item("uncomplicatedfpv:mavic_drone_with_drop", 1)))  # мавик со сбросом
        items.append(slot(7, item("superbwarfare:monitor", 1)))                # пульт управления
        items.append(slot(8, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(10, item("pwpwarfare:drone_ammo_pouch", 2)))  # дрон-подсумок: перезарядка мавика
        items.append(slot(9, knife(faction["knife"])))
    elif role == "Anti_air":
        # Игла на слоте 3 (СПЕЦ, фирменное оружие ПВО), дым и бинт сдвигаются
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(2)))
        items.append(slot(8, knife(faction["knife"])))
        # без лопаты
    elif role == "Rifleman":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("pwpwarfare:ammo_bag", 1)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        if variant is None or variant in ("Iron", "Red Dot"):
            items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Grenadier":
        # M320 на слоте 3 (СПЕЦ), дым и бинт сдвигаются
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(2)))
        items.append(slot(6, ammo("tacz:40mm", 10)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    else:
        # Assault / LMG / HMG / Marksman / Sniper / LAT / HAT
        if role in ("LAT", "HAT"):
            # труба уже на слоте 3 (СПЕЦ) — дым и бинт сдвигаются
            items.append(slot(4, smoke(2)))
            items.append(slot(5, bandage(2)))
            items.append(slot(free_slot(items, 8), knife(faction["knife"])))
            items.append(slot(free_slot(items, 7), item("pwpwarfare:entrenching_tool", 1)))
            if variant is None or variant in ("Iron", "Red Dot"):
                items.append(slot(free_slot(items, 9), item("warbornrenewed:binocular", 1)))
        elif role == "Sniper":
            # клеймор — фирменное снаряжение снайпера (СПЕЦ, слот 3), дым и бинт сдвигаются
            items.append(slot(3, item("superbwarfare:claymore_mine", 1)))
            items.append(slot(4, smoke(2)))
            items.append(slot(5, bandage(2)))
            items.append(slot(free_slot(items, 8), knife(faction["knife"])))
            items.append(slot(free_slot(items, 7), item("pwpwarfare:entrenching_tool", 1)))
            items.append(slot(free_slot(items, 9), item("warbornrenewed:binocular", 1)))
        else:
            items.append(slot(3, smoke(2)))
            items.append(slot(4, bandage(2)))
            items.append(slot(free_slot(items, 8), knife(faction["knife"])))
            items.append(slot(free_slot(items, 7), item("pwpwarfare:entrenching_tool", 1)))
            if role in ("Assault", "LMG", "HMG", "Marksman"):
                items.append(slot(free_slot(items, 9), item("warbornrenewed:binocular", 1)))
            elif variant in ("Iron", "Red Dot"):
                items.append(slot(free_slot(items, 9), item("warbornrenewed:binocular", 1)))

    # --- броня
    items.append(slot(36, armor("survival_instinct:military_boots")))
    items.append(slot(37, armor("survival_instinct:military_leggings")))

    no_armor = role in faction.get("no_armor_roles", set())
    if role == "Sniper":
        g = faction["ghillie"]
        items.append(slot(42, armor(f"warbornrenewed:ghillie-helmet-{g}")))
        items.append(slot(44, armor(f"warbornrenewed:ghillie-body-{g}")))
        items.append(slot(46, armor(f"warbornrenewed:ghillie-legs-{g}")))
    elif not no_armor:
        if meta[3]:  # лидер
            items.append(slot(38, armor(faction["vest"])))
            items.append(slot(39, armor(faction["helmet_officer"])))
        elif role in LIGHT_ROLES:
            items.append(slot(39, armor(faction["helmet"])))
        else:
            items.append(slot(38, armor(faction["vest"])))
            items.append(slot(39, armor(faction["helmet"])))

    # --- патроны (в свободные слоты, начиная с 10; броня/снаряга не трогаются)
    used = {s["slot"] for s in items}
    def next_ammo_slot():
        i = 10
        while i in used:
            i += 1
        return i
    if gun_name != "Igla":
        gid, ammo_id, mag, mode, is_ar = GUNS[gun_name]
        if ammo_id != "tacz:40mm":
            n = ammo_stacks_for(role, gun_name)
            for _ in range(n):
                s_i = next_ammo_slot()
                items.append(slot(s_i, ammo(ammo_id, 60)))
                used.add(s_i)
    if pistol_ammo_id:
        s_i = next_ammo_slot()
        items.append(slot(s_i, ammo(pistol_ammo_id, 60)))
        used.add(s_i)

    items.sort(key=lambda x: x["slot"])
    items_json = json.dumps(items, ensure_ascii=False)
    kit_name = role if not variant else f"{role} ({variant})"
    category = "FIRE_SUPPORT" if (role == "LMG" and variant == "Optic") else CATEGORY[role]
    max_team, max_squad, min_squad = meta[0], meta[1], meta[2]
    if (role, variant) in VARIANT_LIMITS:
        max_squad, min_squad = VARIANT_LIMITS[(role, variant)]
    desc = f"{faction['ru']} · {ROLE_RU[role]} — {DESC_PHRASE[role]}"
    weapon_line = gun_name if special_name is None else f"{gun_name} + {special_name}"
    desc += f" · Оружие: {weapon_line}"
    if desc_alts:
        desc += f" · Альтернативы: {', '.join(desc_alts)}"
    if variant:
        desc += f" · вариант: {variant_ru} (альтернатива)"
    return {
        "faction": faction_key,
        "kitName": kit_name,
        "category": category,
        "description": desc,
        "leaderOnly": meta[3],
        "maxPerTeam": max_team,
        "maxPerSquad": max_squad,
        "minSquadPlayers": min_squad,
        "items": items_json,
        "slotSkins": json.dumps(slot_skins),
    }

# ---------------------------------------------------------------- main
def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    sql_lines = [
        "SET NAMES utf8mb4;",
        "INSERT INTO kit_definitions (faction, kit_name, category, description, leader_only, max_per_team, max_per_squad, min_squad_players, items, slot_skins) VALUES",
    ]
    values = []
    for faction_key, faction in FACTIONS.items():
        kits = []
        for role in ROLES_META:
            # роли-варианты свернуты: одна базовая роль, альтернативы оружия внутри (__ALT__ на 41-42)
            kits.append(build_kit(faction_key, faction, role, None, None))
        out = os.path.join(OUT_DIR, f"{faction_key}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(kits, f, ensure_ascii=False, indent=2)
        print(f"OK {faction_key}: {len(kits)} китов -> {out}")
        for k in kits:
            # MySQL съедает backslash-escape: \" внутри JSON (display.Name) превращается в " — удваиваем
            def sql_esc(s):
                return s.replace("\\", "\\\\").replace("'", "''")
            items_esc = sql_esc(k["items"])
            desc_esc = sql_esc(k["description"])
            skins_esc = sql_esc(k["slotSkins"])
            values.append(
                f"('{k['faction']}', '{k['kitName']}', '{k['category']}', '{desc_esc}', "
                f"{1 if k['leaderOnly'] else 0}, {k['maxPerTeam']}, {k['maxPerSquad']}, {k['minSquadPlayers']}, "
                f"'{items_esc}', '{skins_esc}')"
            )
    sql_lines.append(",\n".join(values))
    sql_lines.append("ON DUPLICATE KEY UPDATE category=VALUES(category), description=VALUES(description), "
                     "leader_only=VALUES(leader_only), max_per_team=VALUES(max_per_team), "
                     "max_per_squad=VALUES(max_per_squad), min_squad_players=VALUES(min_squad_players), "
                     "items=VALUES(items), slot_skins=VALUES(slot_skins);")
    with open(os.path.join(OUT_DIR, "import.sql"), "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines))
    print(f"OK import.sql -> {os.path.join(OUT_DIR, 'import.sql')}")

if __name__ == "__main__":
    main()
