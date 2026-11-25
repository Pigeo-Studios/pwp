# -*- coding: utf-8 -*-
# Генератор стартовых дивизий (составов) фракций — этап 6 ROADMAP.
# Пишет maps/vehicles/divisions.sql для таблицы faction_divisions (core-service создаёт её сам при старте).
# Машины берутся из пулов tools/generate_vehicle_pools.py (vehicle_name = роль в нижнем регистре).
#
# Как работает дивизия: слот техники на карте задаёт роль, дивизия — какая машина и СКОЛЬКО слотов роли
# заняты (count). Слоты роли сверх count — пустые. Фракция без дивизий играет прежним общим пулом.
# Импорт: cmd /c "mysql -u <user> -p pwp_core < maps\vehicles\divisions.sql"  (НЕ PowerShell-пайпом — кириллица)
# Запуск: py -3 tools/generate_divisions.py
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_vehicle_pools import POOLS  # noqa: E402

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, "maps", "vehicles", "divisions.sql")

# id: (название, режимы CSV, размеры карт CSV, {роль: число машин})
DIVISIONS = {
    "combined": ("Общевойсковая", "", "MEDIUM,LARGE", {
        "SUPPLY": 2, "TRANSPORT": 2, "MRAP": 2, "APC": 1, "IFV": 1, "TANK": 1,
        "AA": 1, "CAS_FIGHTER": 1, "HELI_TRANSPORT": 1, "HELI_ATTACK": 1,
    }),
    "mechanized": ("Механизированная", "", "MEDIUM,LARGE", {
        "SUPPLY": 2, "TRANSPORT": 1, "MRAP": 1, "APC": 2, "IFV": 2, "TANK": 1,
    }),
    "light": ("Лёгкая пехота", "", "SMALL,MEDIUM", {
        "SUPPLY": 1, "TRANSPORT": 2, "MRAP": 2,
    }),
}


def sql_str(s):
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    rows = []
    for faction, pool in POOLS.items():
        for div_id, (name, modes, sizes, counts) in DIVISIONS.items():
            vehicles = [
                {"role": role, "vehicleName": role.lower(), "count": n, "respawnSeconds": 0}
                for role, n in counts.items()
                if role in pool
            ]
            if not vehicles:
                continue
            rows.append("(" + ", ".join([
                sql_str(faction), sql_str(div_id), sql_str(name), sql_str(modes), sql_str(sizes),
                sql_str(json.dumps(vehicles, ensure_ascii=False)), "UNIX_TIMESTAMP() * 1000",
            ]) + ")")
    lines = [
        "SET NAMES utf8mb4;",
        "-- PWP: стартовые дивизии фракций (tools/generate_divisions.py). Повторный импорт обновляет записи.",
        "USE pwp_core;",
        "INSERT INTO faction_divisions (faction, division_id, display_name, modes, map_sizes, vehicles, updated_at) VALUES",
        ",\n".join(rows),
        "ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), modes=VALUES(modes),",
        "map_sizes=VALUES(map_sizes), vehicles=VALUES(vehicles), updated_at=VALUES(updated_at);",
    ]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")
    print(f"OK: {len(rows)} дивизий -> {OUT}")


if __name__ == "__main__":
    main()
