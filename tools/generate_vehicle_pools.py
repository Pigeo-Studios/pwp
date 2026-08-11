# -*- coding: utf-8 -*-
# Генератор пулов техники фракций (Squad-стиль: одна машина на роль).
# Пишет maps/vehicles/import.sql для таблицы faction_vehicles (core-service).
#
# Правила (актуально):
#  - Приоритет выбора машин: FCP -> VVP -> DragonRise (только машины с паттернами sbw/vehicles).
#  - В КАЖДОЙ машине: маркер (слот 0) + заряженная батарея (superbwarfare:large_battery, Energy NBT).
#  - БК: ТОЛЬКО патроны из data-паттернов машины (Weapons.AmmoType), сбалансированные объёмы
#    (магазины x2, пушки 10+10 / 20+20 / 300, НАР/бомбы по Magazine) — без «набивки» слотов.
#  - Никакого мусора: без бинтов, ремнаборов и прочего.
#  - modid строго из lang: vvp / fcp / dragonrise_reforge / superbwarfare.
# Запуск: py -3 tools/generate_vehicle_pools.py
import json, os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, "maps", "vehicles", "import.sql")

# (respawn, initial, marker_item) — respawn после смерти, initial первый спавн
# Лёгкая/рабочая техника появляется сразу (initial=1), имба вводится постепенно
ROLE_META = {
    "SUPPLY":        (90, 1, "supply_marker"),
    "TRANSPORT":     (60, 1, "infantry_vehicle"),
    "MRAP":          (120, 1, "combat_vehicle"),
    "APC":           (180, 1, "apc"),
    "IFV":           (240, 60, "apc"),
    "TANK":          (360, 120, "tank"),
    "ARTILLERY":     (600, 240, "spg"),
    "MORTAR":        (300, 120, "infantry_vehicle"),
    "AA":            (300, 90, "apc"),
    "CAS_FIGHTER":   (900, 300, "cas_fighter"),
    "HELI_TRANSPORT": (180, 1, "helicopter"),
    "HELI_ATTACK":   (600, 240, "cas_helicopter"),
}

# Пул: faction -> {role: [vehicle_id, display_name]}
# Внутри фракции каждая сущность строго в одной роли; межфракционные дубли только
# там, где выбора нет (FMTV, Урал, Камаз, Панцирь, Ми-8) — как в Squad.
POOLS = {
    "russia": {
        "SUPPLY":         ["fcp:ural", "Урал (снабжение)"],
        "TRANSPORT":      ["fcp:uaz", "УАЗ (транспорт)"],
        "MRAP":           ["fcp:gaz_tigr_mg", "ГАЗ Тигр (пулемёт)"],
        "APC":            ["fcp:btr82", "БТР-82А"],
        "IFV":            ["fcp:bmp2", "БМП-2"],
        "TANK":           ["vvp:t90_m", "Т-90М"],
        "ARTILLERY":      ["fcp:ural_grad", "БМ-21 Град"],
        "AA":             ["vvp:pantsir_s1", "Панцирь-С1"],
        "CAS_FIGHTER":    ["dragonrise_reforge:j16", "J-16"],
        "HELI_TRANSPORT": ["vvp:mi_8", "Ми-8МТВ"],
        "HELI_ATTACK":    ["vvp:mi_24", "Ми-24"],
    },
    "ukraine": {
        "SUPPLY":         ["fcp:ural", "Урал (снабжение)"],
        "TRANSPORT":      ["fcp:uaz", "УАЗ (транспорт)"],
        "MRAP":           ["fcp:gaz_tigr", "ГАЗ Тигр"],
        "APC":            ["vvp:btr_4", "БТР-4"],
        "IFV":            ["fcp:bmp1u", "БМП-1У (Конкурс)"],
        "TANK":           ["vvp:oplot", "Т-80 Оплот"],
        "ARTILLERY":      ["fcp:msta", "2С19 Msta-S"],
        "AA":             ["vvp:pantsir_s1", "Панцирь-С1"],
        "CAS_FIGHTER":    ["dragonrise_reforge:f16c", "F-16C"],
        "HELI_TRANSPORT": ["vvp:mi_8", "Ми-8МТВ"],
        "HELI_ATTACK":    ["vvp:mi_28", "Ми-28Н"],
    },
    "usa": {
        "SUPPLY":         ["vvp:fmtv", "FMTV (снабжение)"],
        "TRANSPORT":      ["fcp:hmmwv_armored_unarmed", "HMMWV бронированный"],
        "MRAP":           ["superbwarfare:lav_150", "LAV-150 Commando"],
        "APC":            ["fcp:stryker_m2", "Страйкер M2"],
        "IFV":            ["vvp:bradley", "M3A3 Брэдли"],
        "TANK":           ["vvp:m1a2_sep", "M1A2 SEP V2"],
        "ARTILLERY":      ["fcp:m109", "M109 Paladin"],
        "AA":             ["superbwarfare:lav_ad", "LAV-AD"],
        "CAS_FIGHTER":    ["superbwarfare:a_10a", "A-10 Thunderbolt II"],
        "HELI_TRANSPORT": ["fcp:huey", "UH-1 Huey"],
        "HELI_ATTACK":    ["vvp:ah_64", "AH-64 Апач"],
    },
    "nato": {
        "SUPPLY":         ["vvp:fmtv", "FMTV (снабжение)"],
        "TRANSPORT":      ["dragonrise_reforge:m113", "M113 (транспорт)"],
        "MRAP":           ["fcp:novator", "Novator"],
        "APC":            ["vvp:ajax", "AJAX"],
        "IFV":            ["vvp:puma", "PUMA VJTF"],
        "TANK":           ["vvp:leopard_2a7v", "Leopard 2A7V"],
        "ARTILLERY":      ["fcp:m109", "M109 Paladin"],
        "AA":             ["dragonrise_reforge:l1a2", "Gepard 1A2"],
        "CAS_FIGHTER":    ["dragonrise_reforge:jas39e", "JAS 39E Gripen"],
        "HELI_TRANSPORT": ["vvp:nh_90", "NH-90"],
        "HELI_ATTACK":    ["fcp:viper", "AH-1Z Viper"],
    },
    "insurgency": {
        "SUPPLY":         ["fcp:ural", "Урал (снабжение)"],
        "TRANSPORT":      ["fcp:toyota_hilux", "Toyota Hilux"],
        "MRAP":           ["fcp:uaz_dshka", "УАЗ с ДШК"],
        "APC":            ["fcp:bmp1", "БМП-1"],
        "IFV":            ["fcp:toyota_hilux_bmp", "Техничка с БМП"],
        "TANK":           ["fcp:t72av", "T-72AV"],
        "MORTAR":         ["fcp:toyota_hilux_mortar", "Миномёт на Хилаксе"],
        "AA":             ["dragonrise_reforge:wlhgzu23", "ЗУ-23 на Wuling"],
    },
    "pmc": {
        "SUPPLY":         ["fcp:ural", "Урал (снабжение)"],
        "TRANSPORT":      ["fcp:gaz_tigr", "ГАЗ Тигр (транспорт)"],
        "MRAP":           ["fcp:matv", "MATV"],
        "APC":            ["fcp:lav25", "LAV-25"],
        "IFV":            ["vvp:centauro", "Centauro"],
        "TANK":           ["dragonrise_reforge:leopard2a4", "Leopard 2A4 (списанный)"],
        "AA":             ["dragonrise_reforge:zsu234", "ЗСУ-23-4 Шилка"],
        "CAS_FIGHTER":    ["dragonrise_reforge:av8b", "AV-8B Harrier II"],
        "HELI_TRANSPORT": ["fcp:venom", "Venom"],
        "HELI_ATTACK":    ["fcp:littlebird_armed", "AH-6 Littlebird"],
    },
}

# БК из data-паттернов машин (Weapons.AmmoType/Magazine), сбалансированные объёмы:
# пушки танков 10 AP + 10 HE (Squad), малые пушки 20+20 или 150+150 (Squad 300 на 30мм),
# пулемёты/коаксы = Magazine x2, НАР/бомбы/ПТУР/ЗРК = Magazine (x2 для магазинных).
# Патроны: vvp:item_30mm/item_7_62mm/item_12_7mm/ap_shell/he_shell/agm,
# superbwarfare:small/large_shell_*, rifle_ammo, heavy_ammo, small_rocket, *_missile, medium_aerial_bomb.
AMMO = {
    # --- Танки ---
    "vvp:t90_m":              [("vvp:ap_shell", 10), ("vvp:he_shell", 10), ("vvp:item_12_7mm", 800)],
    "vvp:oplot":              [("vvp:ap_shell", 10), ("vvp:he_shell", 10), ("vvp:item_12_7mm", 800)],
    "vvp:m1a2_sep":           [("vvp:ap_shell", 10), ("vvp:he_shell", 10), ("vvp:item_12_7mm", 800)],
    "vvp:leopard_2a7v":       [("vvp:ap_shell", 10), ("vvp:he_shell", 10), ("vvp:item_12_7mm", 800)],
    "fcp:t72av":              [("superbwarfare:small_shell_ap", 10), ("superbwarfare:small_shell_he", 10), ("superbwarfare:rifle_ammo", 320)],
    "dragonrise_reforge:leopard2a4": [("superbwarfare:large_shell_ap", 10), ("superbwarfare:large_shell_he", 10), ("superbwarfare:rifle_ammo", 800), ("superbwarfare:heavy_ammo", 800)],
    # --- IFV ---
    "fcp:bmp2":               [("superbwarfare:small_shell_ap", 128), ("superbwarfare:small_shell_he", 128), ("superbwarfare:rifle_ammo", 128), ("superbwarfare:medium_anti_ground_missile", 4)],
    "fcp:bmp1u":              [("superbwarfare:small_shell_ap", 128), ("superbwarfare:small_shell_he", 128), ("superbwarfare:rifle_ammo", 128), ("superbwarfare:medium_anti_ground_missile", 4)],
    "fcp:toyota_hilux_bmp":   [("superbwarfare:small_shell_ap", 20), ("superbwarfare:small_shell_he", 20), ("superbwarfare:rifle_ammo", 300)],
    "vvp:bradley":            [("vvp:item_30mm", 300), ("vvp:item_7_62mm", 1200), ("superbwarfare:medium_anti_ground_missile", 4)],
    "vvp:puma":               [("vvp:item_30mm", 300), ("vvp:item_7_62mm", 1200), ("superbwarfare:medium_anti_ground_missile", 4)],
    "vvp:centauro":           [("vvp:ap_shell", 10), ("vvp:he_shell", 10), ("vvp:item_12_7mm", 800)],
    # --- APC ---
    "fcp:btr82":              [("superbwarfare:small_shell_ap", 20), ("superbwarfare:small_shell_he", 20), ("superbwarfare:rifle_ammo", 320)],
    "fcp:bmp1":               [("superbwarfare:small_shell_ap", 20), ("superbwarfare:rifle_ammo", 300), ("superbwarfare:medium_anti_ground_missile", 2)],
    "fcp:stryker_m2":         [("superbwarfare:rifle_ammo", 448)],
    "fcp:lav25":              [("superbwarfare:small_shell_ap", 150), ("superbwarfare:small_shell_he", 150), ("superbwarfare:rifle_ammo", 64)],
    "vvp:btr_4":              [("vvp:item_30mm", 200), ("vvp:item_7_62mm", 1200), ("superbwarfare:medium_anti_ground_missile", 4)],
    "vvp:ajax":               [("vvp:item_30mm", 200), ("vvp:item_7_62mm", 1000), ("vvp:item_12_7mm", 800)],
    # --- MRAP ---
    "fcp:gaz_tigr_mg":        [("superbwarfare:rifle_ammo", 448)],
    "fcp:uaz_dshka":          [("superbwarfare:rifle_ammo", 300)],
    "fcp:matv":               [("superbwarfare:rifle_ammo", 448)],
    "fcp:novator":            [("superbwarfare:rifle_ammo", 448)],
    "superbwarfare:lav_150":  [("superbwarfare:small_shell_ap", 20), ("superbwarfare:small_shell_he", 20), ("superbwarfare:rifle_ammo", 600)],
    # --- AA ---
    "vvp:pantsir_s1":         [("superbwarfare:small_shell_aa", 800), ("superbwarfare:medium_anti_air_missile", 12)],
    "superbwarfare:lav_ad":   [("superbwarfare:small_shell_aa", 800), ("superbwarfare:medium_anti_air_missile", 8)],
    "dragonrise_reforge:l1a2": [("superbwarfare:small_shell_ap", 800), ("superbwarfare:rifle_ammo", 600), ("superbwarfare:medium_anti_air_missile", 4)],
    "dragonrise_reforge:wlhgzu23": [("superbwarfare:small_shell_ap", 120)],
    "dragonrise_reforge:zsu234": [("superbwarfare:small_shell_ap", 800), ("superbwarfare:medium_anti_air_missile", 4)],
    # --- CAS_FIGHTER ---
    "dragonrise_reforge:j16": [("superbwarfare:small_shell_ap", 300), ("superbwarfare:medium_anti_air_missile", 4), ("superbwarfare:small_rocket", 14), ("superbwarfare:medium_aerial_bomb", 4), ("superbwarfare:large_anti_ground_missile", 5)],
    "dragonrise_reforge:f16c": [("superbwarfare:small_shell_ap", 300), ("superbwarfare:medium_aerial_bomb", 4), ("superbwarfare:large_anti_ground_missile", 5), ("superbwarfare:medium_anti_air_missile", 4)],
    "dragonrise_reforge:jas39e": [("superbwarfare:small_shell_ap", 300), ("superbwarfare:medium_anti_air_missile", 4), ("superbwarfare:large_anti_ground_missile", 5)],
    "superbwarfare:a_10a":    [("superbwarfare:small_shell_ap", 300), ("superbwarfare:small_rocket", 10), ("superbwarfare:medium_aerial_bomb", 6), ("superbwarfare:large_anti_ground_missile", 8)],
    "dragonrise_reforge:av8b":[("superbwarfare:small_shell_ap", 300), ("superbwarfare:small_rocket", 20), ("superbwarfare:medium_aerial_bomb", 2), ("superbwarfare:medium_anti_air_missile", 2)],
    # --- HELI_ATTACK ---
    "vvp:mi_24":              [("vvp:item_12_7mm", 600), ("superbwarfare:small_rocket", 16), ("superbwarfare:medium_anti_ground_missile", 4)],
    "vvp:mi_28":              [("vvp:item_30mm", 300), ("superbwarfare:small_rocket", 10), ("vvp:agm", 4)],
    "vvp:ah_64":              [("vvp:item_30mm", 300), ("superbwarfare:small_rocket", 16), ("superbwarfare:large_anti_ground_missile", 4)],
    "fcp:viper":              [("superbwarfare:grenade_40mm", 192), ("superbwarfare:small_rocket", 14), ("superbwarfare:large_anti_ground_missile", 4), ("superbwarfare:medium_anti_ground_missile", 2)],
    "fcp:littlebird_armed":   [("superbwarfare:small_rocket", 8), ("superbwarfare:rifle_ammo", 384)],
    # --- Артиллерия (только FCP) ---
    "fcp:ural_grad":          [("superbwarfare:small_rocket", 30)],
    "fcp:msta":               [("superbwarfare:large_shell_he", 10), ("superbwarfare:large_shell_ap", 6), ("superbwarfare:large_shell_cm", 4), ("superbwarfare:large_shell_wp", 4)],
    "fcp:m109":               [("superbwarfare:large_shell_he", 10), ("superbwarfare:large_shell_ap", 6), ("superbwarfare:large_shell_cm", 4), ("superbwarfare:large_shell_wp", 4)],
    # --- MORTAR ---
    "fcp:toyota_hilux_mortar":[("superbwarfare:mortar_shell", 16)],
    # --- TRANSPORT с оружием ---
    "dragonrise_reforge:m113": [("superbwarfare:heavy_ammo", 360)],
}

# Батареи: 2 боевым/вертолётам/самолёту, 1 логистике/транспорту — у КАЖДОЙ машины
BATTERY_ROLES_2 = {"MRAP", "APC", "IFV", "TANK", "ARTILLERY", "MORTAR", "AA", "CAS_FIGHTER", "HELI_ATTACK"}
BATTERY_ROLES_1 = {"SUPPLY", "TRANSPORT", "HELI_TRANSPORT"}

# ВАЖНО (инцидент 11.08.2026, краш матч-сервера Index 9 out of bounds):
# у ВСЕХ FCP-машин жёсткий инвентарь 9 слотов (INVENTORY_SIZE=9 в конструкторе
# сущности, проверено javap по всем fcp-сущностям), VehicleContainerType из
# конфига FCP игнорируется! С 12.08.2026 расширено до 36 слотов миксином
# FcpVehicleContainerSizeMixin (CamoVehicleBase.getVehicleInventory ->
# vehicleInventorySlots(): минимум 36 для GRID-стиля; require=0 — при обновлении
# FCP тихо вернётся к 9, краша нет). У SBW/VVP/dragonrise контейнер из конфига:
# Small=27, Medium=54 (дефолт), Huge=102.
# Слоты контейнера на машину (без учёта маркера спавнера).
CONTAINER_SLOTS = {
    # FCP: 36 слотов (миксин; 2 батареи + 34 БК)
    "fcp:ural": 36, "fcp:uaz": 36, "fcp:gaz_tigr": 36, "fcp:gaz_tigr_mg": 36,
    "fcp:btr82": 36, "fcp:bmp1": 36, "fcp:bmp1u": 36, "fcp:bmp2": 36,
    "fcp:t72av": 36, "fcp:toyota_hilux": 36, "fcp:toyota_hilux_bmp": 36,
    "fcp:toyota_hilux_mortar": 36, "fcp:ural_grad": 36, "fcp:msta": 36, "fcp:m109": 36,
    "fcp:hmmwv_armored_unarmed": 36, "fcp:stryker_m2": 36, "fcp:matv": 36,
    "fcp:uaz_dshka": 36, "fcp:lav25": 36, "fcp:novator": 36, "fcp:huey": 36, "fcp:venom": 36,
    "fcp:viper": 36, "fcp:littlebird_armed": 36,
    # VVP: из конфигов
    "vvp:fmtv": 102, "vvp:ural": 102,
    # dragonrise: из конфигов
    "dragonrise_reforge:m113": 102, "dragonrise_reforge:wlhgzu23": 102,
    "dragonrise_reforge:l1a2": 27, "dragonrise_reforge:zsu234": 27,
    # superbwarfare: из конфигов
    "superbwarfare:a_10a": 27,
    # остальные (vvp/DR/SBW без поля) = дефолт Medium 54
}

# Стаки патронов SBW (из байткода ModItems): ракеты 16, ЗРК/ПТУР 4,
# крупные ПТУР/авиабомбы 2, снаряды/пули/мины 64 (дефолт).
AMMO_STACK = {
    "small_rocket": 16, "medium_rocket_ap": 16, "medium_rocket_he": 16, "medium_rocket_cm": 16,
    "medium_anti_air_missile": 4, "medium_anti_ground_missile": 4, "small_aerial_bomb": 4,
    "large_anti_ground_missile": 2, "medium_aerial_bomb": 2,
}

def pack_ammo(ammo, max_slots):
    items = []
    for iid, count in ammo:
        stack = AMMO_STACK.get(iid.split(":")[1], 64)
        remaining = count
        while remaining > 0:
            if len(items) >= max_slots:
                return items  # лимит: хвост БК отсекается, батареи сохраняются
            n = min(stack, remaining)
            items.append((iid, n))
            remaining -= n
    return items


def loadout_json(marker, veid, role):
    items = [{"slot": 0, "item": {"id": f"pwpwarfare:blue_{marker}", "Count": 1}}]
    slot = 1
    # Батареи — первыми, у каждой машины гарантированно.
    # Самая большая батарея SBW: superbwarfare:large_battery_pack, ёмкость 20 000 000 (из байткода ModItems).
    n_bat = 2 if role in BATTERY_ROLES_2 else (1 if role in BATTERY_ROLES_1 else 0)
    for _ in range(n_bat):
        items.append({"slot": slot, "item": {"id": "superbwarfare:large_battery_pack", "Count": 1, "tag": {"Energy": 20000000}}})
        slot += 1
    # БК — после батарей, сбалансированные объёмы. Лимит слотов = реальный
    # контейнер машины минус батареи (FCP = 36 слотов после миксина, см. CONTAINER_SLOTS).
    b_max = CONTAINER_SLOTS.get(veid, 54) - n_bat
    for iid, count in pack_ammo(AMMO.get(veid, []), b_max):
        items.append({"slot": slot, "item": {"id": iid, "Count": count}})
        slot += 1
    return json.dumps(items, ensure_ascii=False)


def main():
    lines = ["SET NAMES utf8mb4;",
             "-- PWP: пулы техники фракций (Squad-стиль, одна машина на роль)",
             "-- В каждой машине: маркер + заряженная батарея + БК из паттерна (без мусора).",
             "-- Требует миграцию core-service/sql/migration_add_vehicle_category.sql (колонка category).",
             "",
             "INSERT INTO faction_vehicles (faction, vehicle_name, display_name, vehicle_id, yaw, respawn_time, initial_time, category, inventory) VALUES"]
    rows = []
    for faction in ["russia", "ukraine", "usa", "nato", "insurgency", "pmc"]:
        pool = POOLS.get(faction, {})
        for role in ["SUPPLY", "TRANSPORT", "MRAP", "APC", "IFV", "TANK", "ARTILLERY", "MORTAR", "AA", "CAS_FIGHTER", "HELI_TRANSPORT", "HELI_ATTACK"]:
            if role not in pool:
                continue
            veh_id, display = pool[role]
            respawn, initial, marker = ROLE_META[role]
            inv = loadout_json(marker, veh_id, role)
            vehicle_name = role.lower()
            rows.append(
                f"('{faction}', '{vehicle_name}', '{display}', '{veh_id}', 0, {respawn}, {initial}, '{role}', '{inv}')"
            )
    lines.append(",\n".join(rows) + "\nON DUPLICATE KEY UPDATE\n"
        "display_name=VALUES(display_name), vehicle_id=VALUES(vehicle_id), yaw=VALUES(yaw),\n"
        "respawn_time=VALUES(respawn_time), initial_time=VALUES(initial_time),\n"
        "category=VALUES(category), inventory=VALUES(inventory);")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")
    print(f"OK: {len(rows)} записей -> {OUT}")


if __name__ == "__main__":
    main()
