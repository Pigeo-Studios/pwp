#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Генератор полной раскладки китов по слотам -> docs/KITS_SQUAD_DESIGN.md
Читает maps/kits/<faction>.json (результат generate_kits.py) и описывает
каждую роль/вариант по слотам: предмет, Count, теги (GunId, обвесы, патроны).
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_kits as gen

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KITS_DIR = os.path.join(BASE, "maps", "kits")
OUT = os.path.join(BASE, "docs", "KITS_SQUAD_DESIGN.md")

# ---------------------------------------------------------------- справочники имён
GUN_NAME = {v[0]: k for k, v in gen.GUNS.items()}
PISTOL_NAME = {v[0]: k for k, v in gen.PISTOLS.items()}
SCOPE_NAME = {v: k for k, v in gen.SCOPES.items()}
TUBE_NAME = {v[0]: k for k, v in gen.TUBES.items()}

AMMO_NAME = {
    "tacz:556x45": "5.56×45", "tacz:545x39": "5.45×39", "tacz:762x39": "7.62×39",
    "tacz:762x54": "7.62×54R", "tacz:762x25": "7.62×25", "tacz:308": "7.62×51 (.308)",
    "tacz:338": ".338 Lapua", "tacz:50bmg": ".50 BMG", "tacz:792x57": "7.92×57",
    "tacz:9mm": "9×19", "tacz:45acp": ".45 ACP", "tacz:46x30": "4.6×30",
    "tacz:40mm": "40-мм", "tacz:57x28": "5.7×28", "rfp:127x108": "12.7×108",
    "ea:416barrett": ".416 Barrett", "ea:300blk": ".300 BLK", "ea:65creedmoor": "6.5 Creedmoor",
}

ITEM_NAME = {
    "pwpwarfare:ammo_bag": "Аммо-мешок", "pwpwarfare:entrenching_tool": "Лопата",
    "pwpwarfare:squad_leader_radio": "Рация", "superbwarfare:artillery_indicator": "Арт-индикатор",
    "superbwarfare:repair_tool": "Ремнабор", "superbwarfare:swarm_drone": "Дрон",
    "superbwarfare:detonator": "Детонатор", "superbwarfare:c4_bomb": "C4",
    "superbwarfare:tm_62": "Мина ТМ-62", "superbwarfare:claymore_mine": "Клеймор",
    "superbwarfare:m18_smoke_grenade": "Дым M18", "superbwarfare:rgo_grenade": "Граната РГО",
    "pwp_medicine:bandage": "Бинт", "pwp_medicine:medkit": "Аптечка",
    "warbornrenewed:binocular": "Бинокль", "lrtactical:throwable": "Граната",
    "lrtactical:melee": "Нож", "tacz:ammo": "Патроны", "tacz:m320": "M320",
    "survival_instinct:military_boots": "Ботинки", "survival_instinct:military_leggings": "Штаны",
    "superbwarfare:igla_9k38": "ПЗРК Игла-9К38", "superbwarfare:javelin": "Javelin",
}

ARMOR_NAME = {
    "warbornrenewed:opscore-multicam": "Шлем Ops-Core Multicam",
    "warbornrenewed:opscore-black": "Шлем Ops-Core Black",
    "warbornrenewed:jpc": "Жилет JPC", "warbornrenewed:6b47-emr": "Шлем 6Б47 EMR",
    "warbornrenewed:6b45-wood": "Жилет 6Б45 Woodland",
    "warbornrenewed:nato-wood-helmet": "Шлем NATO Woodland",
    "warbornrenewed:nato-wood-chestplate": "Жилет NATO Woodland",
    "warbornrenewed:pastgt-black": "Шлем PASGT Black", "warbornrenewed:iotv-black": "Жилет IOTV Black",
    "warbornrenewed:warmor-black": "Жилет Warmor Black",
    "warbornrenewed:gpngv-nato-wood": "Шлем ECH + GPNVG-18",
    "warbornrenewed:ratnik-10t-wood": "Шлем Ратник + 10Т",
    "warbornrenewed:opscore-fc-b2200-voevoda": "Шлем Ops-Core + FC-B2200",
}

ATT_NAME = {
    "tacz:muzzle_compensator_trident": "Компенсатор Trident",
    "tacz:muzzle_silencer_ptilopsis": "Глушитель Ptilopsis",
    "gucci_attachments:grip_vert6": "Вертикальная рукоять",
    "gucci_attachments:grip_bipod1": "Сошки",
    "tacz:stock_carbon_bone_c5": "Сток Carbon Bone C5",
}

def item_label(item):
    """Человекочитаемое описание предмета: название + теги."""
    iid = item["id"]
    tag = item.get("tag", {})
    parts = []
    if iid == "tacz:modern_kinetic_gun":
        gid = tag.get("GunId", "?")
        name = GUN_NAME.get(gid) or PISTOL_NAME.get(gid) or gid
        kind = "Пистолет" if gid in PISTOL_NAME else "Праймари"
        s = f"{kind}: `{gid}` ({name})"
        mode = tag.get("GunFireMode", "?")
        ammo_c = tag.get("GunCurrentAmmoCount", "?")
        s += f" · режим {mode} · магазин {ammo_c}"
        atts = []
        for slot_key, label in [("AttachmentSCOPE", "SCOPE"), ("AttachmentMUZZLE", "MUZZLE"),
                                ("AttachmentGRIP", "GRIP"), ("AttachmentSTOCK", "STOCK"),
                                ("AttachmentLASER", "LASER")]:
            a = tag.get(slot_key)
            if a and isinstance(a, dict) and "tag" in a:
                aid = a["tag"].get("AttachmentId", "?")
                aname = SCOPE_NAME.get(aid) or ATT_NAME.get(aid) or aid
                atts.append(f"{label}: `{aid}` ({aname})")
        if atts:
            s += " · обвесы: " + ", ".join(atts)
        return s
    if iid == "tacz:ammo":
        aid = tag.get("AmmoId", "?")
        return f"Патроны: `{aid}` ({AMMO_NAME.get(aid, aid)})"
    if iid == "lrtactical:throwable":
        return f"Граната: `{tag.get('ThrowableId', '?')}`"
    if iid == "lrtactical:melee":
        return f"Нож: `{tag.get('MeleeWeaponId', '?')}`"
    if iid == "superbwarfare:m18_smoke_grenade":
        return f"Дым M18 · Color={tag.get('Color', '?')}"
    if iid == "superbwarfare:repair_tool":
        return f"Ремнабор · Energy={tag.get('Energy', '?')}"
    if iid == "superbwarfare:c4_bomb":
        return f"C4 · Control={tag.get('Control', '?')}"
    if iid in ("superbwarfare:igla_9k38", "superbwarfare:javelin"):
        gd = tag.get("GunData", {})
        return f"ПЗРК/пускач · зарядов в стволе: {gd.get('Ammo', '?')}"
    if iid.startswith("pointblank:fcl_"):
        return TUBE_NAME.get(iid, iid.replace("pointblank:", ""))
    if iid.startswith("warbornrenewed:ghillie"):
        return iid.replace("warbornrenewed:", "")
    if iid in ARMOR_NAME:
        return ARMOR_NAME[iid]
    return ITEM_NAME.get(iid, iid.replace("pointblank:", "").replace("superbwarfare:", "").replace("warbornrenewed:", ""))

def slot_label(slot):
    if slot == 0: return "Праймари"
    if slot == 1: return "Секондари/Труба"
    if slot in (36, 37, 38, 39, 42, 44, 46): return "Броня"
    if slot == 33: return "Доп. заряд"
    if slot >= 10: return "Инвентарь"
    return "Хотбар"

# ---------------------------------------------------------------- генерация
FACTION_RU = {"usa": "США", "russia": "Россия", "ukraine": "Украина",
              "nato": "НАТО", "insurgency": "Инсургенты", "pmc": "ЧВК"}

HEADER = """# Киты PWP — дизайн по Squad (v2.1, полная раскладка по слотам)

Документ сгенерирован из `maps/kits/*.json` (см. `tools/generate_kits.py`).
Для каждой из 6 фракций перечислены ВСЕ роли (19 базовых, без вариантов-суффиксов) с раскладкой каждого слота:
слот 0 — праймари (ган + обвесы), слот 1 — пистолет, слот 2 — гранаты, слот 3 — СПЕЦ (фирменное оружие роли: труба LAT/HAT, M320 гренадёра, мины сапёра, клеймор снайпера, Игла ПВО), 4–8 — хотбар, 9+ — инвентарь,
33 — доп. заряд, 36/37 — сапоги/штаны, 38 — жилет, 39 — шлем, 42/44/46 — гилли.
Альтернативы оружия (прицелы/трубы) лежат внутри кита на слотах 41-42 с маркерами `__ALT__PRIMARY`/`__ALT__SPECIAL` — выбор в меню деплоя.

Метки: `R` — ресапплай из мейн-блока (resupply), `NBT` — сохранение NBT при смерти (saveNbt).
`bluefor`/`redfor` — заглушки, не трогаем.

## Лимиты ролей (команда / отряд / мин. отряд)

| Роль | Команда | Отряд | Мин. | Лидер |
|---|---|---|---|---|
| Rifleman | ∞ | ∞ | 0 | |
| Assault | 6 | 1 | 0 | |
| Officer | 9 | 1 | 0 | ✔ |
| Medic | 9 | 2 | 0 | |
| Grenadier | 4 | 1 | 0 | |
| LAT | 4 | 2 | 0 | |
| HAT | 2 | 1 | 3 | |
| LMG | 4 | 1 | 0 | |
| HMG | 2 | 1 | 2 | |
| Marksman | 4 | 1 | 0 | |
| Sniper | 2 | 1 | 0 | |
| Sapper | 2 | 1 | 2 | |
| Scout | 4 | 1 | 0 | |
| Anti_air | 2 | 1 | 2 | |
| Drone Operator | 2 | 1 | 2 | |
| Mechanic | 4 | 1 | 0 | |
| Mechanic Officer | 3 | 1 | 0 | ✔ |
| Pilot | 3 | 1 | 0 | |
| Pilot Officer | 3 | 1 | 0 | ✔ |

У всех фракций одинаковый набор из 19 ролей — вариантов (Iron/Red Dot/Optic/Javelin) нет, альтернативы оружия выбираются в меню деплоя внутри роли.

## Категории ролей (лимит «≤3 огневой поддержки на отряд»)

| Категория | Роли | Ограничение |
|---|---|---|
| `DIRECT_COMBAT` | Rifleman, Assault, LMG | нет |
| `FIRE_SUPPORT` | Grenadier, LAT, **LMG (при выборе оптики в деплое)**, Marksman | **не больше 3 на отряд** |
| `SPECIALIST` | HAT, HMG, Sniper, Sapper | нет |
| `SUPPORT` | Officer, Medic, Scout, Anti_air, Drone Operator, Mechanic, Mechanic Officer, Pilot, Pilot Officer | нет |

Сервер блокирует 4-й FS-кит в отряде: серым в меню деплоя (`Max 3 Fire Support per Squad`) и при спавне (фолбэк Unassigned).

## Трубы по фракциям

| Фракция | LAT | HAT |
|---|---|---|
| usa | `fcl_at4` + 1×ракета | `fcl_carlgustafm4` + 1×HEAT + 1×HEDP + прицел |
| russia | `fcl_rpg26` + 1×ракета | `fcl_rpg7v2` + 1×ПГ-7ВМ + 1×ПГ-7ВР + `fcl_pgo7` |
| ukraine | `fcl_rpg26` + 1×ракета | `fcl_rpg7v2` + 1×ПГ-7ВМ + 1×ПГ-7ВР + `fcl_pgo7` |
| nato | `fcl_m72` + 1×ракета | `fcl_smaw` + 1×HEAA + 1×HEDM + прицел |
| insurgency | `fcl_rpg26` + 1×ракета | `fcl_rpg7v2` + 2×ОГ-7В + 1×ПГ-7ВМ + 1×ПГ-7ВР + `fcl_pgo7` |
| pmc | `fcl_m72` + 1×ракета | `fcl_carlgustafm4` + 1×HEAT + 1×HEDP + прицел |

У каждой роли труба одна (у всех труб разный боеприпас, поэтому альт-труб нет — альты только у стволов с тем же БП).

---

"""

def render_kit(fac_ru, k):
    lines = []
    name = k["kitName"]
    leader = "лидер" if k["leaderOnly"] else "нет"
    desc = k["description"]
    cat = k.get("category", "INFANTRY")
    lines.append(f"### {name} — {desc}")
    lines.append(f"Категория: `{cat}` · Лимиты: команда {k['maxPerTeam']} · отряд {k['maxPerSquad']} · мин. отряд {k['minSquadPlayers']} · {leader}")
    if k.get("slotSkins") and k["slotSkins"] != "{}":
        lines.append(f"Альтернативы в меню деплоя (`slotSkins`): {k['slotSkins']}")
    lines.append("")
    lines.append("| Слот | Что | Count | Детали | R | NBT |")
    lines.append("|---|---|---|---|---|---|")
    for s in sorted(json.loads(k["items"]), key=lambda x: x["slot"]):
        it = s["item"]
        label = item_label(it)
        lines.append(f"| {s['slot']} ({slot_label(s['slot'])}) | `{it['id']}` | {it.get('Count', 1)} | {label} | {'✔' if s['resupply'] else ''} | {'✔' if s['saveNbt'] else ''} |")
    return "\n".join(lines)

def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sections = [HEADER]
    for fac in ["usa", "russia", "ukraine", "nato", "insurgency", "pmc"]:
        kits = json.load(open(os.path.join(KITS_DIR, f"{fac}.json"), encoding="utf-8"))
        fac_ru = FACTION_RU[fac]
        sections.append(f"## {fac_ru} ({fac}) — {len(kits)} китов\n")
        for k in kits:
            sections.append(render_kit(fac_ru, k))
            sections.append("")
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(sections))
    print(f"OK -> {OUT}")

if __name__ == "__main__":
    main()
