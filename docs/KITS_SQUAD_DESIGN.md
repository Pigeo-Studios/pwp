# Киты PWP — дизайн по Squad (v2.1, полная раскладка по слотам)

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
| Officer | 6 | 1 | 0 | ✔ |
| Medic | 10 | 2 | 0 | |
| Grenadier | 4 | 1 | 0 | |
| LAT | 6 | 2 | 0 | |
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


## США (usa) — 19 китов

### Rifleman — США · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: M4A1 · Альтернативы: ред-дот, оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Assault — США · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: MK18 MOD
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Officer — США · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: M4A1 · Альтернативы: ред-дот, оптика
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayoauto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Medic — США · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: M4A1 · Альтернативы: оптика
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Grenadier — США · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: M4A1 + M320
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### LAT — США · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: M4A1 + AT4
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_at4` | 1 | AT4 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_at4_rocket` | 1 | fcl_at4_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### HAT — США · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: M4A1 + Карл Густав M4 · Альтернативы: ред-дот
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_carlgustafm4` | 1 | Карл Густав M4 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_carlgustaf_heat551crs` | 1 | fcl_carlgustaf_heat551crs | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_carlgustaf_hedp502` | 1 | fcl_carlgustaf_hedp502 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `pointblank:fcl_carlgustaf_scope` | 1 | fcl_carlgustaf_scope |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### LMG — США · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: M249 SAW · Альтернативы: оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — США · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: MG-43 · Альтернативы: оптика
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 18 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — США · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: Mk14 EBR · Альтернативы: с глушителем
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |

### Sniper — США · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: M700
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m700` (M700) · режим SEMI · магазин 5 · обвесы: SCOPE: `tacz:scope_qmk152` (QMK-152) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:30_06` (tacz:30_06) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:30_06` (tacz:30_06) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-desert` | 1 | ghillie-helmet-desert |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-desert` | 1 | ghillie-body-desert |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-desert` | 1 | ghillie-legs-desert |  |  |

### Sapper — США · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: MK18 MOD
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:blu_43_mine` | 2 | blu_43_mine | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Scout — США · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: MK18 MOD
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: SCOPE: `gucci_attachments:scope_holosun` (Holosun), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Anti_air — США · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: MK18 MOD + Игла
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Drone Operator — США · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: MK18 MOD
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Mechanic — США · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Mechanic Officer — США · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayoauto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |

### Pilot — США · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: MP5A5
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk_mp5a5` (MP5A5) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Pilot Officer — США · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: MP5A5
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk_mp5a5` (MP5A5) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayoauto` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |

## Россия (russia) — 19 китов

### Rifleman — Россия · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: АК-74М · Альтернативы: ред-дот, оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Assault — Россия · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: АК-12
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Officer — Россия · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: АК-12 · Альтернативы: ред-дот, оптика
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karobsidian` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:ratnik-10t-wood` | 1 | Шлем Ратник + 10Т |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Medic — Россия · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: АК-74М · Альтернативы: оптика
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Grenadier — Россия · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: АК-74М + M320
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### LAT — Россия · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: АК-74М + РПГ-26
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### HAT — Россия · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: АК-74М + РПГ-7В2 · Альтернативы: ред-дот
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |

### LMG — Россия · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: РПК-16 · Альтернативы: оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — Россия · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: PKP 6П41 · Альтернативы: оптика
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 18 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — Россия · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: СВД · Альтернативы: с глушителем
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |

### Sniper — Россия · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: СВ-98
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:sv98` (СВ-98) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_standard_8x` (8x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-winter` | 1 | ghillie-helmet-winter |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-winter` | 1 | ghillie-body-winter |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-winter` | 1 | ghillie-legs-winter |  |  |

### Sapper — Россия · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: АКС-74У
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:tm_62` | 3 | Мина ТМ-62 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Scout — Россия · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Anti_air — Россия · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: АКС-74У + Игла
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Drone Operator — Россия · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Mechanic — Россия · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Mechanic Officer — Россия · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karobsidian` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:ratnik-10t-wood` | 1 | Шлем Ратник + 10Т |  |  |

### Pilot — Россия · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Pilot Officer — Россия · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karobsidian` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:ratnik-10t-wood` | 1 | Шлем Ратник + 10Т |  |  |

## Украина (ukraine) — 19 китов

### Rifleman — Украина · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: АК-74М · Альтернативы: ред-дот, оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Assault — Украина · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: HK416D
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### Officer — Украина · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: HK416D · Альтернативы: ред-дот, оптика
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9emerald` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Medic — Украина · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: АК-74М · Альтернативы: оптика
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Grenadier — Украина · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: АК-74М + M320
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### LAT — Украина · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: АК-74М + РПГ-26
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### HAT — Украина · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: АК-74М + РПГ-7В2 · Альтернативы: ред-дот
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |

### LMG — Украина · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: РПК-16 · Альтернативы: оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — Украина · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: PKP 6П41 · Альтернативы: оптика
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 18 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — Украина · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: Mk14 EBR · Альтернативы: с глушителем
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |

### Sniper — Украина · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: M95
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m95` (M95) · режим SEMI · магазин 5 · обвесы: SCOPE: `tacz:scope_standard_8x` (8x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:50bmg` (.50 BMG) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:50bmg` (.50 BMG) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — Украина · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: Z-15
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:tm_62` | 3 | Мина ТМ-62 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### Scout — Украина · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: Z-15
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### Anti_air — Украина · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: Z-15 + Игла
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### Drone Operator — Украина · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: Z-15
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### Mechanic — Украина · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: Z-15
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### Mechanic Officer — Украина · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: Z-15
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9emerald` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |

### Pilot — Украина · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: Z-15
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |

### Pilot Officer — Украина · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: Z-15
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9emerald` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |

## НАТО (nato) — 19 китов

### Rifleman — НАТО · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: G36K · Альтернативы: ред-дот, оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Assault — НАТО · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: SCAR-L
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Officer — НАТО · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: G36K · Альтернативы: ред-дот, оптика
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:buemerald` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Medic — НАТО · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: G36K · Альтернативы: оптика
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Grenadier — НАТО · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: G36K + M320
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### LAT — НАТО · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: G36K + M72 LAW
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_m72` | 1 | M72 LAW |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_m72_rocket` | 1 | fcl_m72_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### HAT — НАТО · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: G36K + SMAW · Альтернативы: ред-дот
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_smaw` | 1 | SMAW |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_smaw_heaa` | 1 | fcl_smaw_heaa | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_smaw_hedm` | 1 | fcl_smaw_hedm | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `pointblank:fcl_smaw_scope` | 1 | fcl_smaw_scope |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### LMG — НАТО · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: FN Evolys · Альтернативы: оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:fn_evolys` (FN Evolys) · режим AUTO · магазин 75 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:fn_evolys` (FN Evolys) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — НАТО · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: LWMMG · Альтернативы: оптика
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:lwmmg` (LWMMG) · режим AUTO · магазин 75 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `rfp:nm338` (rfp:nm338) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `rfp:nm338` (rfp:nm338) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `rfp:nm338` (rfp:nm338) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `rfp:nm338` (rfp:nm338) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `rfp:nm338` (rfp:nm338) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `rfp:nm338` (rfp:nm338) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `rfp:nm338` (rfp:nm338) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `rfp:nm338` (rfp:nm338) | ✔ |  |
| 18 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:lwmmg` (LWMMG) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident) |  | ✔ |

### Marksman — НАТО · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: HK417 · Альтернативы: с глушителем
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk417` (HK417) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk417` (HK417) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Sniper — НАТО · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: AWP
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ai_awp` (AWP) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_mk5hd` (MK5 HD) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — НАТО · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: MP5A5
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk_mp5a5` (MP5A5) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:blu_43_mine` | 2 | blu_43_mine | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Scout — НАТО · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `gucci_attachments:scope_holosun` (Holosun), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Anti_air — НАТО · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: MP7 + Игла
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Drone Operator — НАТО · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_t2` (T2) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Mechanic — НАТО · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Mechanic Officer — НАТО · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:buemerald` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |

### Pilot — НАТО · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: MP5A5
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk_mp5a5` (MP5A5) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Pilot Officer — НАТО · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: MP5A5
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk_mp5a5` (MP5A5) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:buemerald` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |

## Инсургенты (insurgency) — 19 китов

### Rifleman — Инсургенты · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: АК-47
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Assault — Инсургенты · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: Тип-56
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:type56` (Тип-56) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Officer — Инсургенты · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: АК-47
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Medic — Инсургенты · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: АК-47
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Grenadier — Инсургенты · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: АК-47 + M320
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### LAT — Инсургенты · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: АК-47 + РПГ-26
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### HAT — Инсургенты · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: АК-47 + РПГ-7В2
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_og7v` | 1 | fcl_rpg7v2_og7v | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 8 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 11 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 12 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### LMG — Инсургенты · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: РПК
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:rpk` (РПК) · режим AUTO · магазин 40 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### HMG — Инсургенты · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: ПКП
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:pkp` (ПКП) · режим AUTO · магазин 70 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 18 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Marksman — Инсургенты · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: СКС
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:sks_wooden` (СКС) · режим SEMI · магазин 9 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Sniper — Инсургенты · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: Kar98k
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:kar98` (Kar98k) · режим SEMI · магазин 4 · обвесы: SCOPE: `tacz:scope_98k` (98k) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:792x57` (7.92×57) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:792x57` (7.92×57) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — Инсургенты · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: ППШ-41
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:tm_62` | 1 | Мина ТМ-62 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:lunge_mine` | 2 | lunge_mine | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 1 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Scout — Инсургенты · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Anti_air — Инсургенты · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: АКС-74У + Игла
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Drone Operator — Инсургенты · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Mechanic — Инсургенты · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: ППШ-41
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Mechanic Officer — Инсургенты · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: ППШ-41
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Pilot — Инсургенты · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: ППШ-41
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Pilot Officer — Инсургенты · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: ППШ-41
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

## ЧВК (pmc) — 19 китов

### Rifleman — ЧВК · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: HK416 · Альтернативы: ред-дот, оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### Assault — ЧВК · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: MK47
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk47` (MK47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Officer — ЧВК · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: HK416 · Альтернативы: ред-дот, оптика
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9sapphire` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-fc-b2200-voevoda` | 1 | Шлем Ops-Core + FC-B2200 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 42 (Броня) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### Medic — ЧВК · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: SCAR-L · Альтернативы: оптика
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Grenadier — ЧВК · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: HK416 + M320
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### LAT — ЧВК · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: HK416 + M72 LAW
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_m72` | 1 | M72 LAW |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_m72_rocket` | 1 | fcl_m72_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### HAT — ЧВК · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: HK416 + Карл Густав M4 · Альтернативы: ред-дот
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_carlgustafm4` | 1 | Карл Густав M4 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_carlgustaf_heat551crs` | 1 | fcl_carlgustaf_heat551crs | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_carlgustaf_hedp502` | 1 | fcl_carlgustaf_hedp502 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `pointblank:fcl_carlgustaf_scope` | 1 | fcl_carlgustaf_scope |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### LMG — ЧВК · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: M249 SAW · Альтернативы: оптика
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — ЧВК · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: MG-43 · Альтернативы: оптика
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 18 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — ЧВК · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: MK11 · Альтернативы: с глушителем
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk11` (MK11) · режим SEMI · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk11` (MK11) · режим SEMI · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |

### Sniper — ЧВК · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: MSR
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:msr` (MSR) · режим SEMI · магазин 7 · обвесы: SCOPE: `tacz:scope_qmk152` (QMK-152) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:30_06` (tacz:30_06) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:30_06` (tacz:30_06) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-desert` | 1 | ghillie-helmet-desert |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-desert` | 1 | ghillie-body-desert |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-desert` | 1 | ghillie-legs-desert |  |  |

### Sapper — ЧВК · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: MP7
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:blu_43_mine` | 2 | blu_43_mine | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Scout — ЧВК · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: Вектор
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 · обвесы: SCOPE: `gucci_attachments:scope_holosun` (Holosun), MUZZLE: `maxstuff:supressed_brake` (maxstuff:supressed_brake), GRIP: `maxstuff:a3_grip` (maxstuff:a3_grip) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Anti_air — ЧВК · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: MP7 + Игла
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Drone Operator — ЧВК · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_t2` (T2) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Mechanic — ЧВК · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: Вектор
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 · обвесы: MUZZLE: `maxstuff:supressed_brake` (maxstuff:supressed_brake), GRIP: `maxstuff:a3_grip` (maxstuff:a3_grip) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Mechanic Officer — ЧВК · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: Вектор
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 · обвесы: MUZZLE: `maxstuff:supressed_brake` (maxstuff:supressed_brake), GRIP: `maxstuff:a3_grip` (maxstuff:a3_grip) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9sapphire` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-fc-b2200-voevoda` | 1 | Шлем Ops-Core + FC-B2200 |  |  |

### Pilot — ЧВК · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Pilot Officer — ЧВК · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: MP7
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9sapphire` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-fc-b2200-voevoda` | 1 | Шлем Ops-Core + FC-B2200 |  |  |
