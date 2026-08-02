#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор китов PWP (Squad-адаптация v2).
Дизайн: docs/KITS_SQUAD_DESIGN.md
Выход: maps/kits/<faction>.json (массив KitDefinition для /api/v1/kits/faction/{faction}/bulk)
       maps/kits/import.sql (INSERT ... ON DUPLICATE KEY UPDATE для прямого mysql)

v2: Squad-шаблон:
  - варианты китов: Rifleman/Officer ×3 (Iron/Red Dot/Optic), Medic/LAT ×2, HAT/LMG/HMG ×2, Marksman ×2
  - пистолеты только у половины ролей (нет у Rifleman/Grenadier/LAT/HAT/Sapper)
  - трубачи: винтовка (слот 0) + труба (слот 1, вторая рука как в Squad)
  - гренадёр: винтовка + M320 + 10×40-мм
  - у всех: фраг ×2, дым ×2, бинт ×2, бинокль (слот 9), лопата (кроме лидеров/пилотов/ПВО)
  - медик: бинты ×9 + аптечка, maxPerSquad 2; офицер: рация + арт-индикатор, без лопаты
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
    "Officer":          (9, 1, 0, True),
    "Medic":            (9, 2, 0, False),
    "Grenadier":        (4, 1, 0, False),
    "LAT":              (4, 2, 0, False),
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
# scope-ключ: None = му́шка, "reddot", "optic" (выбор прицела — из FACTION_SCOPES)
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
                 ("Red Dot", "ред-дот", "reddot", True,  2)],
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
    "AWP":         ("tacz:ai_awp",      "tacz:338",    5,  "SEMI", False),
    "MRAD":        ("maxstuff:mrad",    "ea:416barrett", 10, "SEMI", False),
    "Kar98k":      ("tacz:kar98",       "tacz:792x57", 4,  "SEMI", False),
    "ППШ-41":      ("cib:ppsh41",       "tacz:762x25", 20, "AUTO", False),
    "MP7":         ("maxstuff:mp7",     "tacz:46x30",  40, "AUTO", False),
    "MP5A5":       ("tacz:hk_mp5a5",    "tacz:9mm",    30, "AUTO", False),
    "Вектор":      ("maxstuff:vector9", "tacz:9mm",    24, "AUTO", False),
    "M320":        ("tacz:m320",        "tacz:40mm",   1,  "SEMI", False),
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
    # ракеты пускачей (FCL/SBW)
    "pointblank:fcl_m72_rocket", "pointblank:fcl_rpg26_rocket", "pointblank:fcl_at4_rocket",
    "pointblank:fcl_rpg7v2_og7v", "pointblank:fcl_rpg7v2_pg7vm", "pointblank:fcl_rpg7v2_pg7vr",
    "pointblank:fcl_smaw_heaa", "pointblank:fcl_smaw_hedm",
    "pointblank:fcl_carlgustaf_he448", "pointblank:fcl_carlgustaf_heat551crs",
    "pointblank:fcl_carlgustaf_heat758", "pointblank:fcl_carlgustaf_hedp502",
    "superbwarfare:javelin_missile", "superbwarfare:medium_anti_air_missile",
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

def tacz_gun(gun_id, mag, fire_mode, scope=None, muzzle=None, grip=None, stock=None):
    tag = {
        "HasBulletInBarrel": 1,
        "GunId": gun_id,
        "GunFireMode": fire_mode,
        "GunCurrentAmmoCount": mag,
    }
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
            "Sapper":           ("MK18 MOD", None,  True,  None),
            "Scout":            ("MK18 MOD", None,  False, None),
            "Anti_air":         ("Igla",    None,   False, "IGLA"),
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
            "Sapper":           ("АКС-74У", None,   True,  None),
            "Scout":            ("АКС-74У", None,   False, None),
            "Anti_air":         ("Igla",    None,   False, "IGLA"),
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
        "helmet": "warbornrenewed:nato-wood-helmet",
        "vest": "warbornrenewed:nato-wood-chestplate",
        "helmet_officer": "warbornrenewed:gpngv-nato-wood",
        "ghillie": "jungle",
        "pistol": "Глок-17",
        "reddot": "OKP-7", "optic": "1П87",
        "assault_scope": "EXPS3", "grenadier_scope": "OKP-7",
        "lmg_scope": "OKP-7", "marksman_scope": "ELCAN-4x", "sniper_scope": "8x",
        "scout_scope": None, "drone_scope": "OKP-7", "sapper_scope": "OKP-7",
        "roles": {
            "Rifleman":         ("АК-74М",  None,   True,  None),
            "Assault":          ("HK416",   None,   False, None),
            "Officer":          ("HK416",   None,   False, None),
            "Medic":            ("АК-74М",  None,   False, None),
            "Grenadier":        ("АК-74М",  None,   False, "M320"),
            "LAT":              ("АК-74М",  None,   True,  "LAT"),
            "HAT":              ("АК-74М",  None,   True,  "HAT"),
            "LMG":              ("РПК-16",  None,   False, None),
            "HMG":              ("PKP 6П41", None,  False, None),
            "Marksman":         ("СВД",     None,   False, None),
            "Sniper":           ("СВ-98",   None,   False, None),
            "Sapper":           ("АКС-74У", None,   True,  None),
            "Scout":            ("АКС-74У", None,   False, None),
            "Anti_air":         ("Igla",    None,   False, "IGLA"),
            "Drone Operator":   ("АКС-74У", None,   False, None),
            "Mechanic":         ("АКС-74У", None,   False, None),
            "Mechanic Officer": ("АКС-74У", None,   False, None),
            "Pilot":            ("АКС-74У", None,   False, None),
            "Pilot Officer":    ("АКС-74У", None,   False, None),
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
            "LMG":              ("M249 SAW", None,  False, None),
            "HMG":              ("MG-43",   None,   False, None),
            "Marksman":         ("Mk14 EBR", None,  False, None),
            "Sniper":           ("AWP",     None,   False, None),
            "Sapper":           ("MP5A5",   None,   True,  None),
            "Scout":            ("MP7",     None,   False, None),
            "Anti_air":         ("Igla",    None,   False, "IGLA"),
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
            "Marksman":         ("СВД",     "ELCAN-4x", False, None),
            "Sniper":           ("Kar98k",  "98k",  False, None),
            "Sapper":           ("ППШ-41",  None,   True,  None),
            "Scout":            ("АКС-74У", None,   False, None),
            "Anti_air":         ("Igla",    None,   False, "IGLA"),
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
            "Assault":          ("SCAR-L",  None,   False, None),
            "Officer":          ("HK416",   None,   False, None),
            "Medic":            ("SCAR-L",  None,   False, None),
            "Grenadier":        ("HK416",   None,   False, "M320"),
            "LAT":              ("HK416",   None,   True,  "LAT"),
            "HAT":              ("HK416",   None,   True,  "HAT"),
            "LMG":              ("M249 SAW", None,  False, None),
            "HMG":              ("MG-43",   None,   False, None),
            "Marksman":         ("Mk14 EBR", None,  False, None),
            "Sniper":           ("MRAD",    None,   False, None),
            "Sapper":           ("MP7",     None,   True,  None),
            "Scout":            ("Вектор",  None,   False, None),
            "Anti_air":         ("Igla",    None,   False, "IGLA"),
            "Drone Operator":   ("MP7",     None,   False, None),
            "Mechanic":         ("Вектор",  None,   False, None),
            "Mechanic Officer": ("Вектор",  None,   False, None),
            "Pilot":            ("MP7",     None,   False, None),
            "Pilot Officer":    ("MP7",     None,   False, None),
        },
    },
}

# ---------------------------------------------------------------- трубы
# LAT/HAT по фракциям (винтовка в слоте 0, труба в слоте 1)
LAT_LAUNCHERS = {
    "usa":       {"Iron": "AT4", "Optic": "M72 LAW"},
    "nato":      {"Iron": "AT4", "Optic": "M72 LAW"},
    "pmc":       {"Iron": "M72 LAW", "Optic": "AT4"},
    "russia":    {"Iron": "РПГ-26", "Optic": "РПГ-26"},
    "ukraine":   {"Iron": "РПГ-26", "Optic": "РПГ-26"},
    "insurgency": {"Iron": "РПГ-7В2 INS", "Optic": "РПГ-7В2 INS"},
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
    "РПГ-7В2 INS":    ("pointblank:fcl_rpg7v2",         [("pointblank:fcl_rpg7v2_og7v", 2),
                                                         ("pointblank:fcl_rpg7v2_pg7vm", 2)],                None),
    "SMAW":           ("pointblank:fcl_smaw",           [("pointblank:fcl_smaw_heaa", 1),
                                                         ("pointblank:fcl_smaw_hedm", 1)],                   "pointblank:fcl_smaw_scope"),
    "Карл Густав M4": ("pointblank:fcl_carlgustafm4",   [("pointblank:fcl_carlgustaf_heat551crs", 1),
                                                         ("pointblank:fcl_carlgustaf_hedp502", 1)],          "pointblank:fcl_carlgustaf_scope"),
    "IGLA":           ("superbwarfare:igla_9k38",       [("superbwarfare:medium_anti_air_missile", 2)],      None),
}

# HAT INS: ОГ×2 + ПГ-7ВМ×1 + ПГ-7ВР×1 + прицел (как Squad INS)
TUBES["РПГ-7В2 INS HAT"] = ("pointblank:fcl_rpg7v2", [("pointblank:fcl_rpg7v2_og7v", 2),
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
    "Rifleman": "основа отряда", "Assault": "штурм в первом эшелоне", "Officer": "командир отряда и рация",
    "Medic": "спасает жизни", "Grenadier": "40-мм поддержка", "LAT": "лёгкая противотанковая",
    "HAT": "охота на технику", "LMG": "подавляющий огонь", "HMG": "тяжёлая огневая точка",
    "Marksman": "точные выстрелы на дистанции", "Sniper": "одна пуля — одна цель", "Sapper": "мины и подрывы",
    "Scout": "глаза отряда", "Anti_air": "защита неба", "Drone Operator": "разведка с воздуха",
    "Mechanic": "ремонт техники", "Mechanic Officer": "руководит ремонтами", "Pilot": "управляет небом",
    "Pilot Officer": "командир экипажа",
}

def ammo_stacks_for(role, gun_name):
    if role in ("LMG",):
        return 6
    if role == "HMG":
        return 8
    if role in ("Marksman", "Sniper"):
        return 2 if gun_name != "Kar98k" else 1
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
        sk = faction["grenadier_scope"]
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
        muzzle = MUZZLE_SIL if silencer else (MUZZLE_COMP if (is_ar or is_mg) else None)
        grip = GRIP_BIPOD if is_mg else (GRIP_VERT if (is_ar or gun_name in STOCKLESS_AR) else None)
        stock = STOCK_HK416 if gun_name == "HK416" else (STOCK_AR if (is_ar and gun_name not in STOCKLESS_AR) else None)
        items.append(slot(0, tacz_gun(gid, mag, mode, scope_id, muzzle, grip, stock)))
        has_launcher = tube_type is not None
        launcher = tube_type

    # --- альтернатива PRIMARY (слот 41): выбор прицела прямо в меню деплоя (__ALT__PRIMARY)
    slot_skins = {}
    alt_scope = None
    alt_needed = False
    if gun_name != "Igla":
        if role in ("Rifleman", "Officer", "Medic"):
            if variant == "Red Dot":
                alt_scope, alt_needed = SCOPES[faction["optic"]], True
            elif variant == "Optic" and faction["reddot"]:
                alt_scope, alt_needed = SCOPES[faction["reddot"]], True
            elif variant == "Iron" and faction["reddot"]:
                alt_scope, alt_needed = SCOPES[faction["reddot"]], True
        elif role == "LMG":
            if variant == "Iron" and faction["lmg_scope"]:
                alt_scope, alt_needed = SCOPES[faction["lmg_scope"]], True
            elif variant == "Optic":
                alt_scope, alt_needed = None, True
        elif role == "HMG":
            if variant == "Iron":
                alt_scope, alt_needed = SCOPES[faction["optic"]], True
            elif variant == "Optic":
                alt_scope, alt_needed = None, True
        if alt_needed:
            items.append(slot(41, tacz_gun(gid, mag, mode, alt_scope, muzzle, grip, stock)))
            slot_skins["41"] = ["__ALT__PRIMARY"]

    # --- труба / M320 (слот 1 — SECONDARY, как в Squad) / пистолет
    pistol_ammo_id = None
    if has_launcher and gun_name != "Igla":
        if launcher == "M320":
            items.append(slot(1, item("tacz:m320", 1)))
        else:
            tube_name = None
            if launcher == "LAT":
                tube_name = LAT_LAUNCHERS[faction_key][variant or "Iron"]
            else:
                tube_name, hat_kind = HAT_LAUNCHERS[faction_key]
                if hat_kind == "PG7_INS":
                    tube_name = "РПГ-7В2 INS HAT"
            main, rockets, sight = TUBES[tube_name]
            items.append(slot(1, item(main, 1)))
            for i, (rid, cnt) in enumerate(rockets):
                items.append(slot(5 + i, item(rid, cnt)))
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
    frags = 4 if role == "Assault" else (2 if variant not in ("Optic", "Suppressed") else 1)
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
        if variant in ("Iron", "Red Dot"):
            items.append(slot(9, item("warbornrenewed:binocular", 1)))
        # без лопаты
    elif role == "Medic":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(9)))
        items.append(slot(6, item("pwp_medicine:medkit", 1)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        if variant == "Red Dot":
            items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Sapper":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("superbwarfare:claymore_mine", 2)))
        items.append(slot(6, item("superbwarfare:tm_62", 2)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        items.append(slot(9, item("superbwarfare:detonator", 1)))
        items.append(slot(10, item("warbornrenewed:binocular", 1)))
        if faction_key != "insurgency":
            items.append(slot(11, item("superbwarfare:repair_tool", 1, {"Energy": 100000})))
        items.append(slot(33, item("superbwarfare:c4_bomb", 2, {"Control": 1})))
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
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Drone Operator":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("superbwarfare:swarm_drone", 1)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
    elif role == "Anti_air":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(8, knife(faction["knife"])))
        # без лопаты
    elif role == "Rifleman":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("pwpwarfare:ammo_bag", 1)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        if variant in ("Iron", "Red Dot"):
            items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Grenadier":
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, ammo("tacz:40mm", 10)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife(faction["knife"])))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    else:
        # Assault / LMG / HMG / Marksman / Sniper / LAT / HAT
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(free_slot(items, 8), knife(faction["knife"])))
        items.append(slot(free_slot(items, 7), item("pwpwarfare:entrenching_tool", 1)))
        if role in ("Assault", "LMG", "HMG", "Marksman", "Sniper"):
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
            if role in VARIANTS and faction_key != "insurgency":
                for suffix, ru, _, _, _ in VARIANTS[role]:
                    kits.append(build_kit(faction_key, faction, role, suffix, ru))
            else:
                kits.append(build_kit(faction_key, faction, role, None, None))
        out = os.path.join(OUT_DIR, f"{faction_key}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(kits, f, ensure_ascii=False, indent=2)
        print(f"OK {faction_key}: {len(kits)} китов -> {out}")
        for k in kits:
            items_esc = k["items"].replace("'", "''")
            desc_esc = k["description"].replace("'", "''")
            skins_esc = k["slotSkins"].replace("'", "''")
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
