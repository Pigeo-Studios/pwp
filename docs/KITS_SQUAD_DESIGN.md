# Киты PWP — дизайн по Squad (v2.1, полная раскладка по слотам)

Документ сгенерирован из `maps/kits/*.json` (см. `tools/generate_kits.py`).
Для каждой из 6 фракций перечислены ВСЕ роли и варианты (альтернативы) с раскладкой каждого слота:
слот 0 — праймари (ган + обвесы), слот 1 — пистолет или труба, 2–8 — хотбар, 9+ — инвентарь,
33 — доп. заряд, 36/37 — сапоги/штаны, 38 — жилет, 39 — шлем, 42/44/46 — гилли.

Метки: `R` — ресапплай из мейн-блока (resupply), `NBT` — сохранение NBT при смерти (saveNbt).
`bluefor`/`redfor` — заглушки, не трогаем.

## Лимиты ролей (команда / отряд / мин. отряд) и варианты

| Роль | Варианты | Команда | Отряд | Мин. | Лидер |
|---|---|---|---|---|---|
| Rifleman | (Iron) / (Red Dot) / (Optic) | ∞ | ∞/∞/3 | 0/0/3 | |
| Assault | — | 6 | 1 | 0 | |
| Officer | (Iron) / (Red Dot) / (Optic) | 9 | 1 | 0 | ✔ |
| Medic | (Red Dot) / (Optic) | 9 | 2 | 0 | |
| Grenadier | — | 4 | 1 | 0 | |
| LAT | (Iron) / (Optic) | 4 | 2 | 0 | |
| HAT | (Iron) / (Red Dot) | 2 | 1 | 3 | |
| LMG | (Iron) / (Optic) | 4 | 1 | 0 | |
| HMG | (Iron) / (Optic) | 2 | 1 | 2 | |
| Marksman | (Optic) / (Suppressed) | 4 | 1 | 0 | |
| Sniper | — | 2 | 1 | 0 | |
| Sapper | — | 2 | 1 | 2 | |
| Scout | — | 4 | 1 | 0 | |
| Anti_air | — | 2 | 1 | 2 | |
| Drone Operator | — | 2 | 1 | 2 | |
| Mechanic | — | 4 | 1 | 0 | |
| Mechanic Officer | — | 3 | 1 | 0 | ✔ |
| Pilot | — | 3 | 1 | 0 | |
| Pilot Officer | — | 3 | 1 | 0 | ✔ |

Инсургенты — без вариантов (по одному киту на роль, как в Squad INS).

## Категории ролей (лимит «≤3 огневой поддержки на отряд»)

| Категория | Роли | Ограничение |
|---|---|---|
| `DIRECT_COMBAT` | Rifleman (все вар.), Assault, LMG (Iron) | нет |
| `FIRE_SUPPORT` | Grenadier, LAT (все вар.), **LMG (Optic)**, Marksman (все вар.) | **не больше 3 на отряд** |
| `SPECIALIST` | HAT, HMG, Sniper, Sapper | нет |
| `SUPPORT` | Officer, Medic, Scout, Anti_air, Drone Operator, Mechanic, Mechanic Officer, Pilot, Pilot Officer | нет |

Сервер блокирует 4-й FS-кит в отряде: серым в меню деплоя (`Max 3 Fire Support per Squad`) и при спавне (фолбэк Unassigned).

## Трубы по фракциям

| Фракция | LAT | HAT |
|---|---|---|
| usa | Iron: `fcl_at4` + 1×ракета · Optic: `fcl_m72` + 1×ракета | `fcl_carlgustafm4` + 1×HEAT + 1×HEDP + прицел |
| russia | `fcl_rpg26` + 1×ракета (оба варианта) | `fcl_rpg7v2` + 1×ПГ-7ВМ + 1×ПГ-7ВР + `fcl_pgo7` |
| ukraine | `fcl_rpg26` + 1×ракета (оба варианта) | `fcl_rpg7v2` + 1×ПГ-7ВМ + 1×ПГ-7ВР + `fcl_pgo7` |
| nato | Iron: `fcl_at4` + 1×ракета · Optic: `fcl_m72` + 1×ракета | `fcl_smaw` + 1×HEAA + 1×HEDM + прицел |
| insurgency | `fcl_rpg7v2` + 2×ОГ-7В + 2×ПГ-7ВМ | `fcl_rpg7v2` + 2×ОГ-7В + 1×ПГ-7ВМ + 1×ПГ-7ВР + `fcl_pgo7` |
| pmc | Iron: `fcl_m72` + 1×ракета · Optic: `fcl_at4` + 1×ракета | `fcl_carlgustafm4` + 1×HEAT + 1×HEDP + прицел |

---


## США (usa) — 29 китов

### Rifleman (Iron) — США · Стрелок — основа отряда · вариант: мушка (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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

### Rifleman (Red Dot) — США · Стрелок — основа отряда · вариант: ред-дот (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Rifleman (Optic) — США · Стрелок — основа отряда · вариант: оптика (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд 3 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Assault — США · Штурмовик — штурм в первом эшелоне
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 4 | Граната: `lrtactical:m67` | ✔ |  |
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

### Officer (Iron) — США · Офицер — командир отряда и рация · вариант: мушка (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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

### Officer (Red Dot) — США · Офицер — командир отряда и рация · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Officer (Optic) — США · Офицер — командир отряда и рация · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayoauto` |  |  |
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

### Medic (Red Dot) — США · Медик — спасает жизни · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
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

### Medic (Optic) — США · Медик — спасает жизни · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Grenadier — США · Гренадёр — 40-мм поддержка
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:m320` | 1 | M320 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
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

### LAT (Iron) — США · ЛАТ — лёгкая противотанковая · вариант: мушка (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_at4` | 1 | AT4 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_at4_rocket` | 1 | fcl_at4_rocket | ✔ |  |
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

### LAT (Optic) — США · ЛАТ — лёгкая противотанковая · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_m72` | 1 | M72 LAW |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_m72_rocket` | 1 | fcl_m72_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### HAT (Iron) — США · ХАТ — охота на технику · вариант: мушка (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_carlgustafm4` | 1 | Карл Густав M4 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_carlgustaf_heat551crs` | 1 | fcl_carlgustaf_heat551crs | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_carlgustaf_hedp502` | 1 | fcl_carlgustaf_hedp502 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `pointblank:fcl_carlgustaf_scope` | 1 | fcl_carlgustaf_scope |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### HAT (Red Dot) — США · ХАТ — охота на технику · вариант: ред-дот (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_carlgustafm4` | 1 | Карл Густав M4 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_carlgustaf_heat551crs` | 1 | fcl_carlgustaf_heat551crs | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_carlgustaf_hedp502` | 1 | fcl_carlgustaf_hedp502 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `pointblank:fcl_carlgustaf_scope` | 1 | fcl_carlgustaf_scope |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### LMG (Iron) — США · Пулемётчик — подавляющий огонь · вариант: мушка (альтернатива)
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

### LMG (Optic) — США · Пулемётчик — подавляющий огонь · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG (Iron) — США · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: мушка (альтернатива)
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

### HMG (Optic) — США · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: оптика (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman (Optic) — США · Марксман — точные выстрелы на дистанции · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

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

### Marksman (Suppressed) — США · Марксман — точные выстрелы на дистанции · вариант: с глушителем (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
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

### Sniper — США · Снайпер — одна пуля — одна цель
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m700` (M700) · режим SEMI · магазин 5 · обвесы: SCOPE: `tacz:scope_qmk152` (QMK-152) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
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

### Sapper — США · Сапёр — мины и подрывы
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 6 (Хотбар) | `superbwarfare:tm_62` | 2 | Мина ТМ-62 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Scout — США · Разведчик — глаза отряда
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: SCOPE: `gucci_attachments:scope_holosun` (Holosun), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
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
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Anti_air — США · ПВО — защита неба
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Drone Operator — США · Оператор дрона — разведка с воздуха
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18 MOD) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:swarm_drone` | 1 | Дрон | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:bayonet` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |

### Mechanic — США · Механик — ремонт техники
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

### Mechanic Officer — США · Механик-офицер — руководит ремонтами
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

### Pilot — США · Пилот — управляет небом
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

### Pilot Officer — США · Пилот-офицер — командир экипажа
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

## Россия (russia) — 29 китов

### Rifleman (Iron) — Россия · Стрелок — основа отряда · вариант: мушка (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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

### Rifleman (Red Dot) — Россия · Стрелок — основа отряда · вариант: ред-дот (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Rifleman (Optic) — Россия · Стрелок — основа отряда · вариант: оптика (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд 3 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |

### Assault — Россия · Штурмовик — штурм в первом эшелоне
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 4 | Граната РГО | ✔ |  |
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

### Officer (Iron) — Россия · Офицер — командир отряда и рация · вариант: мушка (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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

### Officer (Red Dot) — Россия · Офицер — командир отряда и рация · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Officer (Optic) — Россия · Офицер — командир отряда и рация · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karobsidian` |  |  |
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

### Medic (Red Dot) — Россия · Медик — спасает жизни · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
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

### Medic (Optic) — Россия · Медик — спасает жизни · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |

### Grenadier — Россия · Гренадёр — 40-мм поддержка
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:m320` | 1 | M320 |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
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

### LAT (Iron) — Россия · ЛАТ — лёгкая противотанковая · вариант: мушка (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
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

### LAT (Optic) — Россия · ЛАТ — лёгкая противотанковая · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### HAT (Iron) — Россия · ХАТ — охота на технику · вариант: мушка (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### HAT (Red Dot) — Россия · ХАТ — охота на технику · вариант: ред-дот (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### LMG (Iron) — Россия · Пулемётчик — подавляющий огонь · вариант: мушка (альтернатива)
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

### LMG (Optic) — Россия · Пулемётчик — подавляющий огонь · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG (Iron) — Россия · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: мушка (альтернатива)
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

### HMG (Optic) — Россия · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: оптика (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman (Optic) — Россия · Марксман — точные выстрелы на дистанции · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

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

### Marksman (Suppressed) — Россия · Марксман — точные выстрелы на дистанции · вариант: с глушителем (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
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

### Sniper — Россия · Снайпер — одна пуля — одна цель
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:sv98` (СВ-98) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_standard_8x` (8x) |  | ✔ |
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
| 42 (Броня) | `warbornrenewed:ghillie-helmet-winter` | 1 | ghillie-helmet-winter |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-winter` | 1 | ghillie-body-winter |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-winter` | 1 | ghillie-legs-winter |  |  |

### Sapper — Россия · Сапёр — мины и подрывы
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 6 (Хотбар) | `superbwarfare:tm_62` | 2 | Мина ТМ-62 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Scout — Россия · Разведчик — глаза отряда
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
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
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Anti_air — Россия · ПВО — защита неба
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Drone Operator — Россия · Оператор дрона — разведка с воздуха
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:swarm_drone` | 1 | Дрон | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:karambit` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |

### Mechanic — Россия · Механик — ремонт техники
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

### Mechanic Officer — Россия · Механик-офицер — руководит ремонтами
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

### Pilot — Россия · Пилот — управляет небом
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

### Pilot Officer — Россия · Пилот-офицер — командир экипажа
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

## Украина (ukraine) — 29 китов

### Rifleman (Iron) — Украина · Стрелок — основа отряда · вариант: мушка (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |

### Rifleman (Red Dot) — Украина · Стрелок — основа отряда · вариант: ред-дот (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Rifleman (Optic) — Украина · Стрелок — основа отряда · вариант: оптика (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд 3 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |

### Assault — Украина · Штурмовик — штурм в первом эшелоне
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 4 | Граната РГО | ✔ |  |
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Officer (Iron) — Украина · Офицер — командир отряда и рация · вариант: мушка (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### Officer (Red Dot) — Украина · Офицер — командир отряда и рация · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### Officer (Optic) — Украина · Офицер — командир отряда и рация · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9emerald` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### Medic (Red Dot) — Украина · Медик — спасает жизни · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |

### Medic (Optic) — Украина · Медик — спасает жизни · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |

### Grenadier — Украина · Гренадёр — 40-мм поддержка
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:m320` | 1 | M320 |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### LAT (Iron) — Украина · ЛАТ — лёгкая противотанковая · вариант: мушка (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### LAT (Optic) — Украина · ЛАТ — лёгкая противотанковая · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (1П87) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### HAT (Iron) — Украина · ХАТ — охота на технику · вариант: мушка (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### HAT (Red Dot) — Украина · ХАТ — охота на технику · вариант: ред-дот (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### LMG (Iron) — Украина · Пулемётчик — подавляющий огонь · вариант: мушка (альтернатива)
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### LMG (Optic) — Украина · Пулемётчик — подавляющий огонь · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG (Iron) — Украина · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: мушка (альтернатива)
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG (Optic) — Украина · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: оптика (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: SCOPE: `rfp:1p87` (1П87), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (PKP 6П41) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman (Optic) — Украина · Марксман — точные выстрелы на дистанции · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Marksman (Suppressed) — Украина · Марксман — точные выстрелы на дистанции · вариант: с глушителем (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Sniper — Украина · Снайпер — одна пуля — одна цель
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:sv98` (СВ-98) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_standard_8x` (8x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — Украина · Сапёр — мины и подрывы
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 6 (Хотбар) | `superbwarfare:tm_62` | 2 | Мина ТМ-62 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Scout — Украина · Разведчик — глаза отряда
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
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
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Anti_air — Украина · ПВО — защита неба
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Drone Operator — Украина · Оператор дрона — разведка с воздуха
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (OKP-7) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:swarm_drone` | 1 | Дрон | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Mechanic — Украина · Механик — ремонт техники
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
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
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Mechanic Officer — Украина · Механик-офицер — руководит ремонтами
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |

### Pilot — Украина · Пилот — управляет небом
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
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
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Pilot Officer — Украина · Пилот-офицер — командир экипажа
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
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
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |

## НАТО (nato) — 29 китов

### Rifleman (Iron) — НАТО · Стрелок — основа отряда · вариант: мушка (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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

### Rifleman (Red Dot) — НАТО · Стрелок — основа отряда · вариант: ред-дот (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Rifleman (Optic) — НАТО · Стрелок — основа отряда · вариант: оптика (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд 3 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Assault — НАТО · Штурмовик — штурм в первом эшелоне
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 4 | Граната: `lrtactical:m67` | ✔ |  |
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

### Officer (Iron) — НАТО · Офицер — командир отряда и рация · вариант: мушка (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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

### Officer (Red Dot) — НАТО · Офицер — командир отряда и рация · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Officer (Optic) — НАТО · Офицер — командир отряда и рация · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:buemerald` |  |  |
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

### Medic (Red Dot) — НАТО · Медик — спасает жизни · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
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

### Medic (Optic) — НАТО · Медик — спасает жизни · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Grenadier — НАТО · Гренадёр — 40-мм поддержка
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:m320` | 1 | M320 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
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

### LAT (Iron) — НАТО · ЛАТ — лёгкая противотанковая · вариант: мушка (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_at4` | 1 | AT4 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_at4_rocket` | 1 | fcl_at4_rocket | ✔ |  |
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

### LAT (Optic) — НАТО · ЛАТ — лёгкая противотанковая · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_m72` | 1 | M72 LAW |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_m72_rocket` | 1 | fcl_m72_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### HAT (Iron) — НАТО · ХАТ — охота на технику · вариант: мушка (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_smaw` | 1 | SMAW |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_smaw_heaa` | 1 | fcl_smaw_heaa | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_smaw_hedm` | 1 | fcl_smaw_hedm | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `pointblank:fcl_smaw_scope` | 1 | fcl_smaw_scope |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### HAT (Red Dot) — НАТО · ХАТ — охота на технику · вариант: ред-дот (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_smaw` | 1 | SMAW |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_smaw_heaa` | 1 | fcl_smaw_heaa | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_smaw_hedm` | 1 | fcl_smaw_hedm | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `pointblank:fcl_smaw_scope` | 1 | fcl_smaw_scope |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### LMG (Iron) — НАТО · Пулемётчик — подавляющий огонь · вариант: мушка (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
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
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### LMG (Optic) — НАТО · Пулемётчик — подавляющий огонь · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG (Iron) — НАТО · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: мушка (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
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
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 18 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG (Optic) — НАТО · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: оптика (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
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
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 18 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman (Optic) — НАТО · Марксман — точные выстрелы на дистанции · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG) |  | ✔ |
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

### Marksman (Suppressed) — НАТО · Марксман — точные выстрелы на дистанции · вариант: с глушителем (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
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

### Sniper — НАТО · Снайпер — одна пуля — одна цель
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ai_awp` (AWP) · режим SEMI · магазин 5 · обвесы: SCOPE: `tacz:scope_mk5hd` (MK5 HD) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:338` (.338 Lapua) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:338` (.338 Lapua) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — НАТО · Сапёр — мины и подрывы
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk_mp5a5` (MP5A5) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 6 (Хотбар) | `superbwarfare:tm_62` | 2 | Мина ТМ-62 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Scout — НАТО · Разведчик — глаза отряда
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `gucci_attachments:scope_holosun` (Holosun), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
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

### Anti_air — НАТО · ПВО — защита неба
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Drone Operator — НАТО · Оператор дрона — разведка с воздуха
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_t2` (T2) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:swarm_drone` | 1 | Дрон | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |

### Mechanic — НАТО · Механик — ремонт техники
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

### Mechanic Officer — НАТО · Механик-офицер — руководит ремонтами
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

### Pilot — НАТО · Пилот — управляет небом
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

### Pilot Officer — НАТО · Пилот-офицер — командир экипажа
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

### Rifleman — Инсургенты · Стрелок — основа отряда
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
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Assault — Инсургенты · Штурмовик — штурм в первом эшелоне
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:type56` (Тип-56) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 4 | Граната РГО | ✔ |  |
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

### Officer — Инсургенты · Офицер — командир отряда и рация
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер

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
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Medic — Инсургенты · Медик — спасает жизни
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет

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
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Grenadier — Инсургенты · Гренадёр — 40-мм поддержка
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:m320` | 1 | M320 |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### LAT — Инсургенты · ЛАТ — лёгкая противотанковая
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg7v2_og7v` | 2 | fcl_rpg7v2_og7v | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 2 | fcl_rpg7v2_pg7vm | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### HAT — Инсургенты · ХАТ — охота на технику
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS HAT |  |  |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_rpg7v2_og7v` | 2 | fcl_rpg7v2_og7v | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### LMG — Инсургенты · Пулемётчик — подавляющий огонь
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

### HMG — Инсургенты · Тяжёлый пулемёт — тяжёлая огневая точка
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

### Marksman — Инсургенты · Марксман — точные выстрелы на дистанции
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (ELCAN-4x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Sniper — Инсургенты · Снайпер — одна пуля — одна цель
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:kar98` (Kar98k) · режим SEMI · магазин 4 · обвесы: SCOPE: `tacz:scope_98k` (98k) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:792x57` (7.92×57) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — Инсургенты · Сапёр — мины и подрывы
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 6 (Хотбар) | `superbwarfare:tm_62` | 2 | Мина ТМ-62 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |

### Scout — Инсургенты · Разведчик — глаза отряда
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
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

### Anti_air — Инсургенты · ПВО — защита неба
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Drone Operator — Инсургенты · Оператор дрона — разведка с воздуха
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:swarm_drone` | 1 | Дрон | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:stiletto` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |

### Mechanic — Инсургенты · Механик — ремонт техники
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

### Mechanic Officer — Инсургенты · Механик-офицер — руководит ремонтами
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

### Pilot — Инсургенты · Пилот — управляет небом
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

### Pilot Officer — Инсургенты · Пилот-офицер — командир экипажа
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

## ЧВК (pmc) — 29 китов

### Rifleman (Iron) — ЧВК · Стрелок — основа отряда · вариант: мушка (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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

### Rifleman (Red Dot) — ЧВК · Стрелок — основа отряда · вариант: ред-дот (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### Rifleman (Optic) — ЧВК · Стрелок — основа отряда · вариант: оптика (альтернатива)
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд 3 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### Assault — ЧВК · Штурмовик — штурм в первом эшелоне
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 4 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
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

### Officer (Iron) — ЧВК · Офицер — командир отряда и рация · вариант: мушка (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

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

### Officer (Red Dot) — ЧВК · Офицер — командир отряда и рация · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |

### Officer (Optic) — ЧВК · Офицер — командир отряда и рация · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:m9sapphire` |  |  |
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

### Medic (Red Dot) — ЧВК · Медик — спасает жизни · вариант: ред-дот (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
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

### Medic (Optic) — ЧВК · Медик — спасает жизни · вариант: оптика (альтернатива)
Категория: `SUPPORT` · Лимиты: команда 9 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:stock_carbon_bone_c5` (Сток Carbon Bone C5) |  | ✔ |

### Grenadier — ЧВК · Гренадёр — 40-мм поддержка
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:m320` | 1 | M320 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `tacz:ammo` | 10 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
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

### LAT (Iron) — ЧВК · ЛАТ — лёгкая противотанковая · вариант: мушка (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_m72` | 1 | M72 LAW |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_m72_rocket` | 1 | fcl_m72_rocket | ✔ |  |
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

### LAT (Optic) — ЧВК · ЛАТ — лёгкая противотанковая · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 2 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_at4` | 1 | AT4 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_at4_rocket` | 1 | fcl_at4_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### HAT (Iron) — ЧВК · ХАТ — охота на технику · вариант: мушка (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_carlgustafm4` | 1 | Карл Густав M4 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_carlgustaf_heat551crs` | 1 | fcl_carlgustaf_heat551crs | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_carlgustaf_hedp502` | 1 | fcl_carlgustaf_hedp502 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `pointblank:fcl_carlgustaf_scope` | 1 | fcl_carlgustaf_scope |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### HAT (Red Dot) — ЧВК · ХАТ — охота на технику · вариант: ред-дот (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять), STOCK: `tacz:oem_stock_tactical` (tacz:oem_stock_tactical) |  | ✔ |
| 1 (Секондари/Труба) | `pointblank:fcl_carlgustafm4` | 1 | Карл Густав M4 |  |  |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pointblank:fcl_carlgustaf_heat551crs` | 1 | fcl_carlgustaf_heat551crs | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_carlgustaf_hedp502` | 1 | fcl_carlgustaf_hedp502 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `pointblank:fcl_carlgustaf_scope` | 1 | fcl_carlgustaf_scope |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### LMG (Iron) — ЧВК · Пулемётчик — подавляющий огонь · вариант: мушка (альтернатива)
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

### LMG (Optic) — ЧВК · Пулемётчик — подавляющий огонь · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG (Iron) — ЧВК · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: мушка (альтернатива)
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

### HMG (Optic) — ЧВК · Тяжёлый пулемёт — тяжёлая огневая точка · вариант: оптика (альтернатива)
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
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
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman (Optic) — ЧВК · Марксман — точные выстрелы на дистанции · вариант: оптика (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO) |  | ✔ |
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

### Marksman (Suppressed) — ЧВК · Марксман — точные выстрелы на дистанции · вариант: с глушителем (альтернатива)
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
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

### Sniper — ЧВК · Снайпер — одна пуля — одна цель
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mrad` (MRAD) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_qmk152` (QMK-152) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:416barrett` (.416 Barrett) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:416barrett` (.416 Barrett) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-desert` | 1 | ghillie-helmet-desert |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-desert` | 1 | ghillie-body-desert |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-desert` | 1 | ghillie-legs-desert |  |  |

### Sapper — ЧВК · Сапёр — мины и подрывы
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 6 (Хотбар) | `superbwarfare:tm_62` | 2 | Мина ТМ-62 | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Scout — ЧВК · Разведчик — глаза отряда
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 · обвесы: SCOPE: `gucci_attachments:scope_holosun` (Holosun), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (tacz:muzzle_silencer_phantom_s1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
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

### Anti_air — ЧВК · ПВО — защита неба
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 2 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Drone Operator — ЧВК · Оператор дрона — разведка с воздуха
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_t2` (T2) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:swarm_drone` | 1 | Дрон | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `cs2_wt:talon` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |

### Mechanic — ЧВК · Механик — ремонт техники
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 |  | ✔ |
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

### Mechanic Officer — ЧВК · Механик-офицер — руководит ремонтами
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 |  | ✔ |
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

### Pilot — ЧВК · Пилот — управляет небом
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

### Pilot Officer — ЧВК · Пилот-офицер — командир экипажа
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
