#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор китов PWP (Squad-адаптация v3, ростер 13.08.2026).
Дизайн: docs/KITS_SQUAD_DESIGN.md
Выход: maps/kits/<faction>.json (массив KitDefinition для /api/v1/kits/faction/{faction}/bulk)
       maps/kits/import.sql (INSERT ... ON DUPLICATE KEY UPDATE для прямого mysql)

v3 (ростер):
  - роль -> класс оружия -> конкретная модель -> боезапас -> комплект обвесов -> ALT
  - ALT — полноценная альтернатива: другой ствол (__ALT__PRIMARY, слот 41),
    другая труба (__ALT__SPECIAL, слот 42) или другой пистолет (__ALT__SECONDARY, слот 43)
  - боезапас — часть баланса (стаки по 60 с остатком): Rifleman 210, Assault 240,
    Medic/Officer 180, PDW 150-180, DMR 50-70, Sniper 30-50, LMG 240-320, HMG 240-350
  - обвесы по ролям: прицел/дульник/рукоять/приклад меняют назначение ствола
  - ножи: lrtactical:dagger (без скинов cs2_wt)
  - M320 — maxstuff:m320t + 8x40-мм (4 HE / 4 smoke — в TaCZ 40мм один тип)
  - ALT-стволы другого калибра получают 1 стак (60) своих патронов
  - пистолетные патроны 30 (24-36 по правилам ростера; пилоты 34)

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

# ---------------------------------------------------------------- стволы
# name: (GunId, ammoId, magSize, fireMode, canStock)  canStock — платформа принимает приклад
GUNS = {
    "M4A1":        ("tacz:m4a1",        "tacz:556x45", 30, "AUTO", True),
    "M16A4":       ("tacz:m16a4",       "tacz:556x45", 30, "BURST", True),
    "HK416D":      ("tacz:hk416d",      "tacz:556x45", 30, "AUTO", True),
    "HK416":       ("maxstuff:hk416c",  "tacz:556x45", 30, "AUTO", True),
    "G36":         ("maxstuff:g36",     "tacz:556x45", 30, "AUTO", True),
    "G36K":        ("tacz:g36k",        "tacz:556x45", 30, "AUTO", True),
    "AUG":         ("tacz:aug",         "tacz:556x45", 30, "AUTO", True),
    "AUG HBAR":    ("maxstuff:aug_hbar","tacz:556x45", 42, "AUTO", True),
    "SCAR-L":      ("tacz:scar_l",      "tacz:556x45", 30, "AUTO", True),
    "SCAR-H":      ("tacz:scar_h",      "tacz:308",    20, "AUTO", True),
    "HK417":       ("maxstuff:hk417",   "tacz:308",    20, "AUTO", True),
    "MK18":        ("maxstuff:mk18",    "tacz:556x45", 30, "AUTO", True),
    "MK18 AUTO":   ("maxstuff:mk18_auto","tacz:556x45",30, "AUTO", True),
    "SR15":        ("maxstuff:sr15",    "tacz:556x45", 30, "AUTO", True),
    "SR16":        ("maxstuff:sr16",    "tacz:556x45", 30, "AUTO", True),
    "HK G33":      ("maxstuff:hk_g33",  "tacz:556x45", 30, "AUTO", True),
    "MK47":        ("maxstuff:mk47",    "tacz:762x39", 30, "AUTO", False),
    "M249 SAW":    ("tacz:m249",        "tacz:556x45", 75, "AUTO", False),
    "FN Evolys":   ("tacz:fn_evolys",   "tacz:308",    75, "AUTO", False),
    "FN FAL":      ("tacz:fn_fal",      "tacz:308",    20, "AUTO", True),
    "Honey Badger":("maxstuff:honey_badger","ea:300blk",30, "AUTO", True),
    "UMP45":       ("tacz:ump45",       "tacz:45acp",  25, "AUTO", False),
    "UMP9":        ("maxstuff:ump9",    "tacz:9mm",    25, "AUTO", False),
    "MP5A5":       ("tacz:hk_mp5a5",    "tacz:9mm",    30, "AUTO", False),
    "MP5SD":       ("maxstuff:hk_mp5sd","tacz:9mm",    30, "AUTO", False),
    "MP7":         ("maxstuff:mp7",     "tacz:46x30",  40, "AUTO", False),
    "Вектор":      ("maxstuff:vector9", "tacz:9mm",    24, "AUTO", False),
    "Uzi":         ("tacz:uzi",         "tacz:9mm",    20, "AUTO", False),
    "SCAR-SSR":    ("maxstuff:scar_ssr","ea:65creedmoor",20, "SEMI", True),
    "АК-74М":      ("maxstuff:ak74m",   "tacz:545x39", 30, "AUTO", False),
    "АК-74":       ("maxstuff:ak74",    "tacz:545x39", 30, "AUTO", False),
    "АК-12":       ("maxstuff:ak12",    "tacz:545x39", 30, "AUTO", False),
    "АКС-74У":     ("maxstuff:aks74u",  "tacz:545x39", 30, "AUTO", False),
    "АКС-74УБ":    ("maxstuff:aks74ub", "tacz:545x39", 30, "AUTO", False),
    "АК-47":       ("tacz:ak47",        "tacz:762x39", 30, "AUTO", False),
    "АК-9":        ("maxstuff:ak9",     "ea:9x39",     20, "AUTO", False),
    "РПК":         ("tacz:rpk",         "tacz:762x39", 40, "AUTO", False),
    "РПК-16":      ("maxstuff:rpk16",   "tacz:545x39", 40, "AUTO", False),
    "РПК-74":      ("maxstuff:rpk_74",  "tacz:545x39", 40, "AUTO", False),
    "РПК-74М":     ("maxstuff:rpk_74m", "tacz:545x39", 40, "AUTO", False),
    "Vityaz":      ("maxstuff:vityas",  "tacz:9mm",    30, "AUTO", False),
    "Тип-56":      ("cib:type56",       "tacz:762x39", 30, "AUTO", False),
    "СВД":         ("cib:svd",          "tacz:762x54", 10, "SEMI", False),
    "СВ-98":       ("cib:sv98",         "tacz:762x54", 10, "SEMI", False),
    "ПКП":         ("cib:pkp",          "tacz:762x54", 70, "AUTO", False),
    "MG-43":       ("rfp:mg43",         "tacz:308",    100, "AUTO", False),
    "6П41":        ("rfp:6p41bp",       "tacz:762x54", 100, "AUTO", False),
    "RPL-20":      ("rfp:rpl20",        "tacz:545x39", 75, "AUTO", False),
    "Mk14 EBR":    ("tacz:mk14",        "tacz:308",    10, "SEMI", False),
    "M700":        ("tacz:m700",        "tacz:30_06",  5,  "SEMI", True),
    "MSR":         ("maxstuff:msr",     "tacz:30_06",  7,  "SEMI", True),
    "AWP":         ("maxstuff:ai_awp",  "tacz:308",    10, "SEMI", False),
    "M95":         ("tacz:m95",         "tacz:50bmg",  5,  "SEMI", False),
    "Z-15":        ("maxstuff:z15_a",   "tacz:545x39", 30, "AUTO", False),
    "Z-15C":       ("maxstuff:z15",     "tacz:545x39", 30, "SEMI", False),
    "Kar98k":      ("tacz:kar98",       "tacz:792x57", 4,  "SEMI", False),
    "ППШ-41":      ("cib:ppsh41",       "tacz:762x25", 20, "AUTO", False),
    "СКС":         ("maxstuff:sks_wooden","tacz:762x39",9, "SEMI", False),
    "ДП-27":       ("maxstuff:dp28",    "tacz:762x54", 47, "AUTO", False),
    "M320":        ("maxstuff:m320t",   "tacz:40mm",   1,  "SEMI", False),
}

PISTOLS = {
    "M1911":   ("tacz:m1911",         "tacz:45acp", 7,  "SEMI"),
    "Глок-18": ("maxstuff:glock_18c", "tacz:9mm",   17, "SEMI"),
    "Глок-17": ("tacz:glock_17",      "tacz:9mm",   17, "SEMI"),
    "P320":    ("tacz:p320",          "tacz:45acp", 12, "SEMI"),
    "CZ-75":   ("tacz:cz75",          "tacz:9mm",   16, "SEMI"),
    "M17":     ("maxstuff:m17",       "tacz:9mm",   17, "SEMI"),
}

# ---------------------------------------------------------------- обвесы
# ключи ростера -> id атачмента (None — нет обвеса)
SCOPES = {
    None: None,
    "T2":       "tacz:sight_t2",
    "EXPS3":    "tacz:sight_exp3",
    "Coyote":   "tacz:sight_coyote",
    "OKP7":     "tacz:sight_okp7",
    "RMR":      "tacz:sight_rmr_dot",
    "Holo":     "tacz:sight_uh1",
    "ACOG":     "tacz:scope_acog_ta31",
    "LPVO":     "tacz:scope_lpvo_1_6",
    "MK5HD":    "tacz:scope_mk5hd",
    "8x":       "tacz:scope_standard_8x",
    "98k":      "tacz:scope_98k",
    "4x":       "tacz:scope_elcan_4x",
    "1П87":     "rfp:1p87",
    "simple optic":   "rfp:1p87",
    "simple red dot": "tacz:sight_okp7",
    "iron":     None,
    "iron/Holo": None,
}

MUZZLES = {
    None: None,
    "Trident":    "tacz:muzzle_compensator_trident",
    "suppressor": "tacz:muzzle_silencer_phantom_s1",
    "brake":      "tacz:muzzle_brake_cyclone_d2",
}

GRIPS = {
    None: None,
    "vert6": "gucci_attachments:grip_vert6",
    "bipod": "gucci_attachments:grip_bipod1",
    "bipod1": "gucci_attachments:grip_bipod1",
    "A3 grip": "maxstuff:a3_grip",
}

STOCKS = {
    None: None,
    "light stock":    "tacz:oem_stock_light",
    "tactical stock": "tacz:oem_stock_tactical",
    "heavy stock":    "tacz:oem_stock_heavy",
    "precise stock":  "gucci_attachments:stock_precise",
}

LASERS = {
    None: None,
    "laser":         "tacz:laser_compact",
    "compact laser": "tacz:laser_compact",
}

SMOKE_COLOR = 11546150
PISTOL_AMMO_DEFAULT = 30      # 24-36 по правилам ростера
PILOT_PISTOL_AMMO = 34        # пилоты: «150 + 34»
GL40MM = 8                    # гренадёр: 4 HE + 4 smoke (в TaCZ 40мм один тип)
ALT_AMMO_STACK = 60           # патроны альт-ствола другого калибра
ALT_PISTOL_AMMO_STACK = 30

# ---------------------------------------------------------------- хелперы
RESUPPLY_IDS = {
    "tacz:ammo", "lrtactical:throwable",
    "superbwarfare:m18_smoke_grenade", "superbwarfare:rgo_grenade", "superbwarfare:hand_grenade",
    "pwp_medicine:bandage", "pwp_medicine:medkit",
    "superbwarfare:claymore_mine", "superbwarfare:tm_62", "superbwarfare:c4_bomb",
    "superbwarfare:swarm_drone",
    "uncomplicatedfpv:fpv_drone", "uncomplicatedfpv:mavic_drone_no_drop", "uncomplicatedfpv:mavic_drone_with_drop",
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

def tacz_gun(gun_id, mag, fire_mode, scope=None, muzzle=None, grip=None, stock=None, laser=None, display_name=None):
    tag = {
        "HasBulletInBarrel": 1,
        "GunId": gun_id,
        "GunFireMode": fire_mode,
        "GunCurrentAmmoCount": mag,
    }
    if display_name:
        tag["display"] = {"Name": '{"text":"%s"}' % display_name}
    def att(att_id):
        return item("tacz:attachment", 1, {"AttachmentId": att_id})
    if scope:  tag["AttachmentSCOPE"] = att(scope)
    if muzzle: tag["AttachmentMUZZLE"] = att(muzzle)
    if grip:   tag["AttachmentGRIP"] = att(grip)
    if stock:  tag["AttachmentSTOCK"] = att(stock)
    if laser:  tag["AttachmentLASER"] = att(laser)
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

def knife():
    # Нож без скинов: lrtactical:dagger (LesRaisins, базовый кинжал)
    return item("lrtactical:melee", 1, {"MeleeWeaponId": "lrtactical:dagger"})

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

# ---------------------------------------------------------------- трубы
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
    "Javelin":        ("superbwarfare:javelin",         [("superbwarfare:javelin_missile", 1)],              None),
    "IGLA":           ("superbwarfare:igla_9k38",       [("superbwarfare:medium_anti_air_missile", 1)],      None),
}

# ---------------------------------------------------------------- категории
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

# ---------------------------------------------------------------- ростер v3
# роль -> конфиг. Поля:
#   gun        — ствол (ключ GUNS)
#   ammo       — боезапас основного калибра (шт)
#   scope/muzzle/grip/stock/laser — обвесы (ключи словарей выше)
#   pistol     — пистолет (ключ PISTOLS); по умолчанию фракционный
#   pistol_ammo — патроны пистолета (по умолчанию 30, пилоты 34)
#   tube       — труба/M320/IGLA (ключ TUBES или "M320")
#   alt        — список альтернатив: {gun?|tube?|pistol?, scope?, muzzle?, grip?, stock?}
#                (обвесы наследуются от дефолта, если не заданы)
FACTIONS = {
    "usa": {
        "ru": "США", "motto": "Точность и огневая мощь",
        "frag": frag, "helmet": "warbornrenewed:opscore-multicam",
        "vest": "warbornrenewed:jpc", "helmet_officer": "warbornrenewed:gpngv-nato-wood",
        "ghillie": "desert", "pistol": "M17",
        "no_armor_roles": set(),
        "roles": {
            "Rifleman":         {"gun": "M4A1", "ammo": 210, "scope": "T2", "muzzle": "Trident", "grip": "vert6",
                                 "alt": [{"gun": "M16A4", "scope": "ACOG"}]},
            "Assault":          {"gun": "HK416D", "ammo": 240, "scope": "Coyote", "muzzle": "suppressor", "stock": "light stock",
                                 "alt": [{"gun": "MK18", "scope": "RMR"}]},
            "Officer":          {"gun": "SR15", "ammo": 180, "scope": "EXPS3", "muzzle": "Trident", "stock": "tactical stock",
                                 "alt": [{"gun": "M4A1", "scope": "ACOG"}]},
            "Medic":            {"gun": "M4A1", "ammo": 180, "scope": "Holo", "muzzle": "suppressor", "stock": "light stock",
                                 "alt": [{"gun": "G36", "scope": "Coyote"}]},
            "Grenadier":        {"gun": "M4A1", "ammo": 180, "tube": "M320", "scope": "Coyote", "grip": "A3 grip",
                                 "alt": [{"gun": "HK416D"}]},
            "LAT":              {"gun": "M4A1", "ammo": 180, "tube": "AT4", "scope": "T2", "grip": "vert6",
                                 "alt": [{"tube": "M72 LAW"}]},
            "HAT":              {"gun": "HK416D", "ammo": 150, "tube": "Javelin", "scope": "ACOG", "stock": "tactical stock",
                                 "alt": [{"gun": "SCAR-L"}]},
            "LMG":              {"gun": "M249 SAW", "ammo": 300, "scope": "EXPS3", "grip": "bipod1",
                                 "alt": [{"gun": "FN Evolys"}]},
            "HMG":              {"gun": "MG-43", "ammo": 300, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "FN Evolys"}]},
            "Marksman":         {"gun": "Mk14 EBR", "ammo": 60, "scope": "LPVO", "grip": "bipod", "stock": "precise stock",
                                 "alt": [{"gun": "HK417"}]},
            "Sniper":           {"gun": "M700", "ammo": 40, "scope": "98k", "stock": "precise stock",
                                 "alt": [{"gun": "MSR"}]},
            "Sapper":           {"gun": "MK18", "ammo": 180, "scope": "RMR", "muzzle": "suppressor", "grip": "A3 grip",
                                 "alt": [{"gun": "M4A1"}]},
            "Scout":            {"gun": "Honey Badger", "ammo": 180, "scope": "RMR", "muzzle": "suppressor", "stock": "light stock",
                                 "alt": [{"gun": "MK18", "muzzle": "suppressor"}]},
            "Anti_air":         {"gun": "M4A1", "ammo": 150, "tube": "IGLA", "scope": "Coyote",
                                 "alt": [{"gun": "HK416"}]},
            "Drone Operator":   {"gun": "HK416", "ammo": 150, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
            "Mechanic":         {"gun": "MP7", "ammo": 180, "scope": "RMR", "laser": "compact laser",
                                 "alt": [{"gun": "UMP45"}]},
            "Mechanic Officer": {"gun": "HK416", "ammo": 180, "scope": "T2", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
            "Pilot":            {"gun": "MP7", "ammo": 150, "pistol": "Глок-17", "pistol_ammo": PILOT_PISTOL_AMMO,
                                 "scope": "RMR", "laser": "laser", "alt": [{"pistol": "P320"}]},
            "Pilot Officer":    {"gun": "MP7", "ammo": 180, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP5A5"}]},
        },
    },
    "russia": {
        "ru": "Россия", "motto": "Огневая мощь и живучесть",
        "frag": rgo, "helmet": "warbornrenewed:6b47-emr",
        "vest": "warbornrenewed:6b45-wood", "helmet_officer": "warbornrenewed:ratnik-10t-wood",
        "ghillie": "winter", "pistol": "Глок-18",
        "no_armor_roles": set(),
        "roles": {
            "Rifleman":         {"gun": "АК-74М", "ammo": 210, "scope": "OKP7", "muzzle": "brake", "grip": "vert6",
                                 "alt": [{"gun": "АК-12"}]},
            "Assault":          {"gun": "АКС-74У", "ammo": 240, "scope": "RMR", "muzzle": "suppressor", "stock": "light stock",
                                 "alt": [{"gun": "АК-74М"}]},
            "Officer":          {"gun": "АК-12", "ammo": 180, "scope": "T2", "muzzle": "Trident", "stock": "tactical stock",
                                 "alt": [{"gun": "АК-74М", "scope": "ACOG"}]},
            "Medic":            {"gun": "АК-74М", "ammo": 180, "scope": "Coyote", "muzzle": "suppressor",
                                 "alt": [{"gun": "Z-15"}]},
            "Grenadier":        {"gun": "АК-12", "ammo": 180, "tube": "M320", "scope": "OKP7", "grip": "A3 grip",
                                 "alt": [{"gun": "АК-74М"}]},
            "LAT":              {"gun": "АК-74М", "ammo": 180, "tube": "РПГ-26", "scope": "T2",
                                 "alt": [{"gun": "АК-47", "tube": "РПГ-7В2"}]},
            "HAT":              {"gun": "АК-12", "ammo": 150, "tube": "RPG-28", "scope": "ACOG", "stock": "heavy stock",
                                 "alt": [{"gun": "АК-74М", "tube": "PF-98"}]},
            "LMG":              {"gun": "РПК-16", "ammo": 300, "scope": "1П87", "grip": "bipod1",
                                 "alt": [{"gun": "РПК-74"}]},
            "HMG":              {"gun": "6П41", "ammo": 300, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "ПКП"}]},
            "Marksman":         {"gun": "СВД", "ammo": 60, "scope": "1П87", "grip": "bipod",
                                 "alt": [{"gun": "СВ-98", "scope": "8x"}]},
            "Sniper":           {"gun": "СВ-98", "ammo": 40, "scope": "98k",
                                 "alt": [{"gun": "AWP"}]},
            "Sapper":           {"gun": "АК-74", "ammo": 180, "scope": "OKP7", "muzzle": "suppressor",
                                 "alt": [{"gun": "АКС-74У"}]},
            "Scout":            {"gun": "АК-9", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "АКС-74У"}]},
            "Anti_air":         {"gun": "АКС-74У", "ammo": 150, "tube": "IGLA", "scope": "RMR",
                                 "alt": [{"gun": "АК-74М"}]},
            "Drone Operator":   {"gun": "АК-74М", "ammo": 150, "scope": "OKP7", "muzzle": "suppressor",
                                 "alt": [{"gun": "АК-9"}]},
            "Mechanic":         {"gun": "Vityaz", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
            "Mechanic Officer": {"gun": "АКС-74УБ", "ammo": 180, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "Vityaz"}]},
            "Pilot":            {"gun": "Vityaz", "ammo": 180, "scope": "RMR",
                                 "alt": [{"pistol": "Глок-17"}]},
            "Pilot Officer":    {"gun": "АКС-74У", "ammo": 180, "scope": "OKP7",
                                 "alt": [{"gun": "Vityaz"}]},
        },
    },
    "ukraine": {
        "ru": "Украина", "motto": "Универсальность",
        "frag": rgo, "helmet": "warbornrenewed:opscore-mm14",
        "vest": "warbornrenewed:warmor-mm14", "helmet_officer": "warbornrenewed:gpngv-nato-wood",
        "ghillie": "jungle", "pistol": "Глок-17",
        "no_armor_roles": set(),
        "roles": {
            "Rifleman":         {"gun": "Z-15", "ammo": 210, "scope": "T2", "muzzle": "Trident",
                                 "alt": [{"gun": "АК-74М"}]},
            "Assault":          {"gun": "АКС-74У", "ammo": 240, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "Z-15"}]},
            "Officer":          {"gun": "Z-15", "ammo": 180, "scope": "ACOG", "stock": "tactical stock",
                                 "alt": [{"gun": "M4A1"}]},
            "Medic":            {"gun": "Z-15", "ammo": 180, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "АК-74М"}]},
            "Grenadier":        {"gun": "Z-15", "ammo": 180, "tube": "M320", "scope": "ACOG", "grip": "A3 grip",
                                 "alt": [{"gun": "M4A1"}]},
            "LAT":              {"gun": "Z-15", "ammo": 180, "tube": "AT4", "scope": "Holo",
                                 "alt": [{"gun": "АК-74М", "tube": "РПГ-26"}]},
            "HAT":              {"gun": "Z-15", "ammo": 150, "tube": "Javelin", "scope": "LPVO",
                                 "alt": [{"gun": "SCAR-L"}]},
            "LMG":              {"gun": "РПК-16", "ammo": 300, "scope": "ACOG", "grip": "bipod",
                                 "alt": [{"gun": "РПК-74"}]},
            "HMG":              {"gun": "RPL-20", "ammo": 300, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "ПКП"}]},
            "Marksman":         {"gun": "СВД", "ammo": 60, "scope": "4x",
                                 "alt": [{"gun": "Mk14 EBR", "scope": "LPVO"}]},
            "Sniper":           {"gun": "СВ-98", "ammo": 40, "scope": "8x",
                                 "alt": [{"gun": "M700"}]},
            "Sapper":           {"gun": "Z-15", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "АКС-74У"}]},
            "Scout":            {"gun": "АКС-74У", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "Honey Badger"}]},
            "Anti_air":         {"gun": "Z-15", "ammo": 150, "tube": "IGLA", "scope": "RMR",
                                 "alt": [{"gun": "АКС-74У"}]},
            "Drone Operator":   {"gun": "Z-15", "ammo": 150, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
            "Mechanic":         {"gun": "Vityaz", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
            "Mechanic Officer": {"gun": "Z-15", "ammo": 180, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "АКС-74У"}]},
            "Pilot":            {"gun": "MP7", "ammo": 180, "pistol_ammo": PILOT_PISTOL_AMMO, "scope": "RMR",
                                 "alt": [{"pistol": "Глок-17"}]},
            "Pilot Officer":    {"gun": "Z-15C", "ammo": 180, "scope": "Holo",
                                 "alt": [{"gun": "MP7"}]},
        },
    },
    "nato": {
        "ru": "НАТО", "motto": "Дисциплина и техника",
        "frag": frag, "helmet": "warbornrenewed:nato-wood-helmet",
        "vest": "warbornrenewed:nato-wood-chestplate", "helmet_officer": "warbornrenewed:gpngv-nato-wood",
        "ghillie": "jungle", "pistol": "P320",
        "no_armor_roles": set(),
        "roles": {
            "Rifleman":         {"gun": "G36", "ammo": 210, "scope": "Holo", "grip": "vert6",
                                 "alt": [{"gun": "AUG", "scope": "ACOG"}]},
            "Assault":          {"gun": "HK416D", "ammo": 240, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "HK G33"}]},
            "Officer":          {"gun": "SR16", "ammo": 180, "scope": "EXPS3",
                                 "alt": [{"gun": "HK416D"}]},
            "Medic":            {"gun": "G36K", "ammo": 180, "scope": "Coyote", "muzzle": "suppressor",
                                 "alt": [{"gun": "AUG"}]},
            "Grenadier":        {"gun": "G36K", "ammo": 180, "tube": "M320", "scope": "ACOG", "grip": "A3 grip",
                                 "alt": [{"gun": "HK416D"}]},
            "LAT":              {"gun": "G36K", "ammo": 180, "tube": "AT4", "scope": "Holo",
                                 "alt": [{"gun": "AUG"}]},
            "HAT":              {"gun": "HK416D", "ammo": 150, "tube": "Карл Густав M4", "scope": "ACOG", "stock": "heavy stock",
                                 "alt": [{"gun": "HK417"}]},
            "LMG":              {"gun": "AUG HBAR", "ammo": 252, "scope": "ACOG", "grip": "bipod",
                                 "alt": [{"gun": "FN Evolys"}]},
            "HMG":              {"gun": "FN FAL", "ammo": 240, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "HK417", "scope": "LPVO"}]},
            "Marksman":         {"gun": "HK417", "ammo": 60, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "SCAR-H"}]},
            "Sniper":           {"gun": "MSR", "ammo": 40, "scope": "MK5HD", "stock": "precise stock",
                                 "alt": [{"gun": "M700"}]},
            "Sapper":           {"gun": "MK18", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "G36K"}]},
            "Scout":            {"gun": "Honey Badger", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
            "Anti_air":         {"gun": "HK416", "ammo": 150, "tube": "IGLA", "scope": "RMR",
                                 "alt": [{"gun": "G36K"}]},
            "Drone Operator":   {"gun": "HK416", "ammo": 150, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
            "Mechanic":         {"gun": "MP7", "ammo": 180, "scope": "RMR", "laser": "laser",
                                 "alt": [{"gun": "UMP9"}]},
            "Mechanic Officer": {"gun": "G36K", "ammo": 180, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP5SD"}]},
            "Pilot":            {"gun": "MP5A5", "ammo": 180, "pistol_ammo": PILOT_PISTOL_AMMO, "scope": "RMR",
                                 "alt": [{"pistol": "Глок-17"}]},
            "Pilot Officer":    {"gun": "MP7", "ammo": 180, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"pistol": "P320"}]},
        },
    },
    "insurgency": {
        "ru": "Инсургенты", "motto": "Ржавый, но живучий",
        "frag": rgo, "helmet": "warbornrenewed:pastgt-black",
        "vest": "warbornrenewed:iotv-black", "helmet_officer": "warbornrenewed:pastgt-black",
        "ghillie": "jungle", "pistol": "CZ-75",
        "no_armor_roles": {"Rifleman", "Assault", "Grenadier", "Scout", "Anti_air",
                           "Drone Operator", "Mechanic", "Mechanic Officer", "Pilot", "Pilot Officer"},
        "roles": {
            "Rifleman":         {"gun": "Тип-56", "ammo": 240, "scope": "OKP7",
                                 "alt": [{"gun": "АК-47"}]},
            "Assault":          {"gun": "АК-47", "ammo": 240, "scope": "RMR", "stock": "light stock",
                                 "alt": [{"gun": "АКС-74У"}]},
            "Officer":          {"gun": "АК-47", "ammo": 210, "scope": "Holo",
                                 "alt": [{"gun": "Тип-56"}]},
            "Medic":            {"gun": "Тип-56", "ammo": 180, "scope": "iron/Holo",
                                 "alt": [{"gun": "ППШ-41"}]},
            "Grenadier":        {"gun": "АК-47", "ammo": 210, "tube": "M320", "scope": "OKP7",
                                 "alt": [{"gun": "Тип-56"}]},
            "LAT":              {"gun": "АК-47", "ammo": 210, "tube": "РПГ-7В2 INS", "scope": "iron/Holo",
                                 "alt": [{"tube": "РПГ-26"}]},
            "HAT":              {"gun": "АК-47", "ammo": 180, "tube": "RPG-28", "scope": "OKP7",
                                 "alt": [{"tube": "PF-98"}]},
            "LMG":              {"gun": "РПК", "ammo": 320, "grip": "bipod",
                                 "alt": [{"gun": "ДП-27"}]},
            "HMG":              {"gun": "ПКП", "ammo": 350, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "6П41"}]},
            "Marksman":         {"gun": "СКС", "ammo": 70, "scope": "4x",
                                 "alt": [{"gun": "СВД", "scope": "4x"}]},
            "Sniper":           {"gun": "Kar98k", "ammo": 32, "scope": "98k",
                                 "alt": [{"gun": "СВ-98", "scope": "8x"}]},
            "Sapper":           {"gun": "АКС-74У", "ammo": 180, "scope": "simple red dot",
                                 "alt": [{"gun": "ППШ-41"}]},
            "Scout":            {"gun": "ППШ-41", "ammo": 200, "scope": "RMR",
                                 "alt": [{"gun": "АК-47"}]},
            "Anti_air":         {"gun": "АКС-74У", "ammo": 150, "tube": "IGLA", "scope": "RMR",
                                 "alt": [{"gun": "Тип-56"}]},
            "Drone Operator":   {"gun": "Тип-56", "ammo": 150, "scope": "simple optic",
                                 "alt": [{"gun": "АКС-74У"}]},
            "Mechanic":         {"gun": "ППШ-41", "ammo": 200, "scope": "RMR",
                                 "alt": [{"gun": "Uzi"}]},
            "Mechanic Officer": {"gun": "АК-47", "ammo": 180, "scope": "Holo",
                                 "alt": [{"gun": "ППШ-41"}]},
            "Pilot":            {"gun": "Uzi", "ammo": 160, "pistol_ammo": PILOT_PISTOL_AMMO, "scope": "RMR",
                                 "alt": [{"pistol": "Глок-17"}]},
            "Pilot Officer":    {"gun": "ППШ-41", "ammo": 180, "scope": "RMR",
                                 "alt": [{"gun": "Uzi"}]},
        },
    },
    "pmc": {
        "ru": "ЧВК", "motto": "Профи и дорого",
        "frag": frag, "helmet": "warbornrenewed:opscore-black",
        "vest": "warbornrenewed:warmor-black", "helmet_officer": "warbornrenewed:opscore-fc-b2200-voevoda",
        "ghillie": "desert", "pistol": "M17",
        "no_armor_roles": set(),
        "roles": {
            "Rifleman":         {"gun": "HK416", "ammo": 210, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "MK18"}]},
            "Assault":          {"gun": "MK47", "ammo": 210, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "Вектор"}]},
            "Officer":          {"gun": "SR15", "ammo": 180, "scope": "LPVO", "muzzle": "suppressor",
                                 "alt": [{"gun": "HK416"}]},
            "Medic":            {"gun": "MP7", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "Honey Badger"}]},
            "Grenadier":        {"gun": "HK416", "ammo": 180, "tube": "M320", "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "MK18"}]},
            "LAT":              {"gun": "MK18", "ammo": 180, "tube": "Panzerfaust 3", "scope": "RMR",
                                 "alt": [{"tube": "AT4"}]},
            "HAT":              {"gun": "MK47", "ammo": 180, "tube": "Карл Густав M4", "scope": "LPVO",
                                 "alt": [{"gun": "SCAR-H"}]},
            "LMG":              {"gun": "РПК-74М", "ammo": 300, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "FN Evolys"}]},
            "HMG":              {"gun": "FN Evolys", "ammo": 300, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "MG-43"}]},
            "Marksman":         {"gun": "SCAR-SSR", "ammo": 60, "scope": "LPVO", "grip": "bipod",
                                 "alt": [{"gun": "HK417"}]},
            "Sniper":           {"gun": "MSR", "ammo": 40, "scope": "MK5HD",
                                 "alt": [{"gun": "AWP"}]},
            "Sapper":           {"gun": "MK18 AUTO", "ammo": 240, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "Honey Badger"}]},
            "Scout":            {"gun": "Honey Badger", "ammo": 180, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "Вектор"}]},
            "Anti_air":         {"gun": "MP7", "ammo": 150, "tube": "IGLA", "scope": "RMR",
                                 "alt": [{"gun": "HK416"}]},
            "Drone Operator":   {"gun": "Вектор", "ammo": 150, "scope": "RMR", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
            "Mechanic":         {"gun": "Вектор", "ammo": 168, "scope": "RMR", "laser": "laser",
                                 "alt": [{"gun": "UMP9"}]},
            "Mechanic Officer": {"gun": "HK416", "ammo": 180, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "MK18"}]},
            "Pilot":            {"gun": "MP7", "ammo": 180, "pistol_ammo": PILOT_PISTOL_AMMO, "scope": "RMR",
                                 "alt": [{"pistol": "Глок-18"}]},
            "Pilot Officer":    {"gun": "HK416", "ammo": 180, "scope": "Holo", "muzzle": "suppressor",
                                 "alt": [{"gun": "MP7"}]},
        },
    },
}

# ---------------------------------------------------------------- сборка кита
def att_key(conf, key, default=None):
    v = conf.get(key)
    return default if v is None else v

def build_gun_item(gun_name, scope, muzzle, grip, stock, laser, display_name=None):
    gid, ammo_id, mag, mode, can_stock = GUNS[gun_name]
    stock_id = STOCKS[stock] if stock is not None else None
    if stock_id and not can_stock:
        stock_id = None  # платформа не принимает приклад — не вешаем
    return tacz_gun(gid, mag, mode,
                    SCOPES[scope], MUZZLES[muzzle], GRIPS[grip], stock_id, LASERS[laser],
                    display_name=display_name)

def split_ammo(total, per_stack=60):
    stacks = []
    while total > 0:
        n = min(per_stack, total)
        stacks.append(n)
        total -= n
    return stacks

def build_kit(faction_key, faction, role, cfg):
    meta = ROLES_META[role]
    gun_name = cfg.get("gun")
    tube_type = cfg.get("tube")
    frags = 1 if role in ("Sniper", "Marksman") else 2
    items = []
    special_name = None
    pistol_ammo_id = None

    # --- основное (слот 0)
    main_item = None
    main_ammo_id = None
    if gun_name == "Igla":
        main, rockets, sight = TUBES["IGLA"]
        items.append(slot(0, sbw_gun(main)))
        main_ammo_id = None
    elif tube_type == "IGLA":
        # ПВО: ствол + Игла на слоте 3
        main_item = build_gun_item(gun_name, cfg.get("scope"), cfg.get("muzzle"),
                                   cfg.get("grip"), cfg.get("stock"), cfg.get("laser"))
        items.append(slot(0, main_item))
        main_ammo_id = GUNS[gun_name][1]
        special_name = "Игла"
    elif tube_type == "M320":
        main_item = build_gun_item(gun_name, cfg.get("scope"), cfg.get("muzzle"),
                                   cfg.get("grip"), cfg.get("stock"), cfg.get("laser"))
        items.append(slot(0, main_item))
        main_ammo_id = GUNS[gun_name][1]
        special_name = "M320"
    elif tube_type:
        main_item = build_gun_item(gun_name, cfg.get("scope"), cfg.get("muzzle"),
                                   cfg.get("grip"), cfg.get("stock"), cfg.get("laser"))
        items.append(slot(0, main_item))
        main_ammo_id = GUNS[gun_name][1]
    else:
        main_item = build_gun_item(gun_name, cfg.get("scope"), cfg.get("muzzle"),
                                   cfg.get("grip"), cfg.get("stock"), cfg.get("laser"))
        items.append(slot(0, main_item))
        main_ammo_id = GUNS[gun_name][1]

    # --- труба / M320 / Игла (слот 3 — СПЕЦ)
    if tube_type and tube_type not in ("M320", "IGLA"):
        main, rockets, sight = TUBES[tube_type]
        if tube_type == "Javelin":
            items.append(slot(3, sbw_gun(main)))
        else:
            items.append(slot(3, item(main, 1)))
        for i, (rid, cnt) in enumerate(rockets):
            items.append(slot(6 + i, item(rid, cnt)))
        if sight:
            items.append(slot(free_slot(items, 9), item(sight, 1)))
        special_name = tube_type
    elif tube_type == "M320":
        items.append(slot(3, tacz_gun("maxstuff:m320t", 1, "SEMI")))
        items.append(slot(6, ammo("tacz:40mm", GL40MM)))
        special_name = "M320"
    elif tube_type == "IGLA":
        main, rockets, sight = TUBES["IGLA"]
        items.append(slot(3, sbw_gun(main)))
        items.append(slot(9, item(rockets[0][0], rockets[0][1])))

    # --- пистолет (слот 1), если роль его носит
    no_pistol = role in ("Rifleman", "Grenadier", "LAT", "HAT", "Sapper")
    if not no_pistol:
        pname = cfg.get("pistol") or faction["pistol"]
        pid, pam, pmag, _ = PISTOLS[pname]
        items.append(slot(1, tacz_pistol(pid, pmag)))
        pistol_ammo_id = pam

    # --- альтернативы (слоты 41+): ствол / труба / пистолет
    slot_skins = {}
    alt_names = []
    alt_ammo_ids = []
    alt_idx = 41
    for a in cfg.get("alt", []):
        label = None
        if "gun" in a:
            agun = a["gun"]
            if agun == gun_name and tube_type:
                # альт только меняет трубу — обработано ниже по "tube"
                pass
            else:
                ascope = a.get("scope", cfg.get("scope"))
                amuzzle = a.get("muzzle", cfg.get("muzzle"))
                agrip = a.get("grip", cfg.get("grip"))
                astock = a.get("stock", cfg.get("stock"))
                alaser = a.get("laser", cfg.get("laser"))
                items.append(slot(alt_idx, build_gun_item(agun, ascope, amuzzle, agrip, astock, alaser,
                                                          display_name=f"{agun} (ALT)")))
                slot_skins[str(alt_idx)] = ["__ALT__PRIMARY"]
                alt_names.append(agun)
                aid = GUNS[agun][1]
                if aid != main_ammo_id:
                    alt_ammo_ids.append(aid)
                alt_idx += 1
        if "tube" in a and tube_type:
            tmain, trockets, tsight = TUBES[a["tube"]]
            items.append(slot(alt_idx, item(tmain, 1)))
            slot_skins[str(alt_idx)] = ["__ALT__SPECIAL"]
            alt_names.append(a["tube"])
            # ракеты альт-трубы в рюкзак
            for i, (rid, cnt) in enumerate(trockets):
                items.append(slot(free_slot(items, 6 + i), item(rid, cnt)))
            if tsight:
                items.append(slot(free_slot(items, 9), item(tsight, 1)))
            alt_idx += 1
        if "pistol" in a:
            pid, pam, pmag, _ = PISTOLS[a["pistol"]]
            items.append(slot(alt_idx, tacz_pistol(pid, pmag)))
            slot_skins[str(alt_idx)] = ["__ALT__SECONDARY"]
            alt_names.append(a["pistol"])
            if pam != pistol_ammo_id:
                alt_ammo_ids.append(pam)
            alt_idx += 1

    # --- слоты 2-8 (снаряга)
    if role == "Officer":
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, smoke(2)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, item("pwpwarfare:squad_leader_radio", 1)))
        items.append(slot(6, item("superbwarfare:artillery_indicator", 1)))
        items.append(slot(7, bandage(2)))
        items.append(slot(8, knife()))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Medic":
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, smoke(2)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(9)))
        items.append(slot(6, item("pwp_medicine:medkit", 1)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife()))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Sapper":
        if faction_key in ("usa", "nato", "pmc"):
            items.append(slot(3, item("superbwarfare:claymore_mine", 2)))
            items.append(slot(6, item("superbwarfare:blu_43_mine", 2)))
        elif faction_key in ("russia", "ukraine"):
            items.append(slot(3, item("superbwarfare:tm_62", 3)))
        else:
            items.append(slot(3, item("superbwarfare:tm_62", 1)))
            items.append(slot(6, item("superbwarfare:lunge_mine", 2)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(2)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife()))
        items.append(slot(9, item("superbwarfare:detonator", 1)))
        items.append(slot(10, item("warbornrenewed:binocular", 1)))
        if faction_key != "insurgency":
            items.append(slot(11, item("superbwarfare:repair_tool", 1, {"Energy": 100000})))
        c4_count = 1 if faction_key == "insurgency" else 2
        items.append(slot(33, item("superbwarfare:c4_bomb", c4_count, {"Control": 1})))
    elif role in ("Mechanic", "Mechanic Officer"):
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("superbwarfare:repair_tool", 1, {"Energy": 100000})))
        items.append(slot(8, knife()))
        if role == "Mechanic Officer":
            items.append(slot(6, item("pwpwarfare:squad_leader_radio", 1)))
            items.append(slot(7, smoke(2)))
            items.append(slot(9, item("warbornrenewed:binocular", 1)))
        else:
            items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
    elif role in ("Pilot", "Pilot Officer"):
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("superbwarfare:repair_tool", 1, {"Energy": 100000})))
        items.append(slot(8, knife()))
        if role == "Pilot Officer":
            items.append(slot(6, item("pwpwarfare:squad_leader_radio", 1)))
    elif role == "Scout":
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("uncomplicatedfpv:mavic_drone_no_drop", 1)))
        items.append(slot(6, item("superbwarfare:monitor", 1)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife()))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Drone Operator":
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("uncomplicatedfpv:fpv_drone", 1)))
        items.append(slot(6, item("uncomplicatedfpv:mavic_drone_with_drop", 1)))
        items.append(slot(7, item("superbwarfare:monitor", 1)))
        items.append(slot(8, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(10, item("pwpwarfare:drone_ammo_pouch", 2)))
        items.append(slot(9, knife()))
    elif role == "Anti_air":
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(2)))
        items.append(slot(8, knife()))
    elif role == "Rifleman":
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(5, item("pwpwarfare:ammo_bag", 1)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife()))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role == "Grenadier":
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(2)))
        items.append(slot(7, item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(8, knife()))
        items.append(slot(9, item("warbornrenewed:binocular", 1)))
    elif role in ("LAT", "HAT"):
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(2)))
        items.append(slot(free_slot(items, 8), knife()))
        items.append(slot(free_slot(items, 7), item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(free_slot(items, 9), item("warbornrenewed:binocular", 1)))
    elif role == "Sniper":
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, item("superbwarfare:claymore_mine", 1)))
        items.append(slot(4, smoke(2)))
        items.append(slot(5, bandage(2)))
        items.append(slot(free_slot(items, 8), knife()))
        items.append(slot(free_slot(items, 7), item("pwpwarfare:entrenching_tool", 1)))
        items.append(slot(free_slot(items, 9), item("warbornrenewed:binocular", 1)))
    else:
        # Assault / LMG / HMG / Marksman
        items.append(slot(2, faction["frag"](frags)))
        items.append(slot(3, smoke(2)))
        items.append(slot(4, bandage(2)))
        items.append(slot(free_slot(items, 8), knife()))
        items.append(slot(free_slot(items, 7), item("pwpwarfare:entrenching_tool", 1)))
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
        if meta[3]:
            items.append(slot(38, armor(faction["vest"])))
            items.append(slot(39, armor(faction["helmet_officer"])))
        elif role in LIGHT_ROLES:
            items.append(slot(39, armor(faction["helmet"])))
        else:
            items.append(slot(38, armor(faction["vest"])))
            items.append(slot(39, armor(faction["helmet"])))

    # --- патроны (в свободные слоты, начиная с 10)
    used = {s["slot"] for s in items}
    def next_ammo_slot():
        i = 10
        while i in used:
            i += 1
        return i
    if main_ammo_id and main_ammo_id != "tacz:40mm":
        for n in split_ammo(cfg.get("ammo", 180)):
            s_i = next_ammo_slot()
            items.append(slot(s_i, ammo(main_ammo_id, n)))
            used.add(s_i)
    for alt_aid in alt_ammo_ids:
        s_i = next_ammo_slot()
        items.append(slot(s_i, ammo(alt_aid, ALT_AMMO_STACK)))
        used.add(s_i)
    if pistol_ammo_id:
        pn = cfg.get("pistol_ammo", PISTOL_AMMO_DEFAULT)
        s_i = next_ammo_slot()
        items.append(slot(s_i, ammo(pistol_ammo_id, pn)))
        used.add(s_i)

    items.sort(key=lambda x: x["slot"])
    items_json = json.dumps(items, ensure_ascii=False)
    desc = f"{faction['ru']} · {ROLE_RU[role]} — {DESC_PHRASE[role]}"
    weapon_line = gun_name if special_name is None else f"{gun_name} + {special_name}"
    desc += f" · Оружие: {weapon_line}"
    if alt_names:
        desc += f" · Альтернативы: {', '.join(alt_names)}"
    return {
        "faction": faction_key,
        "kitName": role,
        "category": CATEGORY[role],
        "description": desc,
        "leaderOnly": meta[3],
        "maxPerTeam": meta[0],
        "maxPerSquad": meta[1],
        "minSquadPlayers": meta[2],
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
        for role, cfg in faction["roles"].items():
            kits.append(build_kit(faction_key, faction, role, cfg))
        out = os.path.join(OUT_DIR, f"{faction_key}.json")
        with open(out, "w", encoding="utf-8") as f:
            json.dump(kits, f, ensure_ascii=False, indent=2)
        print(f"OK {faction_key}: {len(kits)} китов -> {out}")
        for k in kits:
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
