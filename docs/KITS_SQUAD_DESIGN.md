# Киты PWP — дизайн по Squad (v3, ростер 13.08.2026, полная раскладка по слотам)

Документ сгенерирован из `maps/kits/*.json` (см. `tools/generate_kits.py`).
Для каждой из 6 фракций перечислены ВСЕ роли (19 базовых) с раскладкой каждого слота:
слот 0 — праймари (ган + обвесы), слот 1 — пистолет, слот 2 — гранаты, слот 3 — СПЕЦ (фирменное оружие роли: труба LAT/HAT, M320 гренадёра, мины сапёра, клеймор снайпера, Игла ПВО), 4–8 — хотбар, 9+ — инвентарь,
33 — доп. заряд, 36/37 — сапоги/штаны, 38 — жилет, 39 — шлем, 42/44/46 — гилли.
Альтернативы оружия лежат внутри кита на слотах 41-43: `__ALT__PRIMARY` (другой ствол),
`__ALT__SPECIAL` (другая труба) или `__ALT__SECONDARY` (другой пистолет) — выбор в меню деплоя.

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

У всех фракций одинаковый набор из 19 ролей — вариантов (Iron/Red Dot/Optic/Javelin) нет, альтернативы оружия (ствол/труба/пистолет) выбираются в меню деплоя внутри роли.

## Категории ролей (лимит «≤3 огневой поддержки на отряд»)

| Категория | Роли | Ограничение |
|---|---|---|
| `DIRECT_COMBAT` | Rifleman, Assault, LMG | нет |
| `FIRE_SUPPORT` | Grenadier, LAT, Marksman | **не больше 3 на отряд** |
| `SPECIALIST` | HAT, HMG, Sniper, Sapper | нет |
| `SUPPORT` | Officer, Medic, Scout, Anti_air, Drone Operator, Mechanic, Mechanic Officer, Pilot, Pilot Officer | нет |

Сервер блокирует 4-й FS-кит в отряде: серым в меню деплоя (`Max 3 Fire Support per Squad`) и при спавне (фолбэк Unassigned).

## Трубы по фракциям (LAT/HAT; альт-трубы в скобках)

| Фракция | LAT | HAT |
|---|---|---|
| usa | `fcl_at4` (альт `fcl_m72`) | `superbwarfare:javelin` |
| russia | `fcl_rpg26` (альт `fcl_rpg7v2`) | `fcl_rpg28` (альт `fcl_pf98`) |
| ukraine | `fcl_at4` (альт `fcl_rpg26`) | `superbwarfare:javelin` |
| nato | `fcl_at4` | `fcl_carlgustafm4` |
| insurgency | `fcl_rpg7v2` (альт `fcl_rpg26`) | `fcl_rpg28` (альт `fcl_pf98`) |
| pmc | `fcl_panzerfaust3` (альт `fcl_at4`) | `fcl_carlgustafm4` |

ПВО у всех фракций: ствол + `superbwarfare:igla_9k38` + 1×ПЗРК-ракета. Гренадёр: ствол + `maxstuff:m320t` + 8×`tacz:40mm` (4 HE + 4 smoke — в TaCZ 40-мм один тип).

---


## США (usa) — 19 китов

### Rifleman — США · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: M4A1 · Альтернативы: M16A4
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m16a4` (M16A4) · режим BURST · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Assault — США · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: HK416D · Альтернативы: MK18
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1), STOCK: `tacz:oem_stock_light` (Лёгкий приклад) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1), STOCK: `tacz:oem_stock_light` (Лёгкий приклад) |  | ✔ |

### Officer — США · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: SR15 · Альтернативы: M4A1
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:sr15` (SR15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), STOCK: `tacz:oem_stock_tactical` (Тактический приклад) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident), STOCK: `tacz:oem_stock_tactical` (Тактический приклад) |  | ✔ |

### Medic — США · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: M4A1 · Альтернативы: G36
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1), STOCK: `tacz:oem_stock_light` (Лёгкий приклад) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1), STOCK: `tacz:oem_stock_light` (Лёгкий приклад) |  | ✔ |

### Grenadier — США · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: M4A1 + M320 · Альтернативы: HK416D
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 8 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |

### LAT — США · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: M4A1 + AT4 · Альтернативы: M72 LAW
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SPECIAL"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_at4` | 1 | AT4 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_at4_rocket` | 1 | fcl_at4_rocket | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_m72_rocket` | 1 | fcl_m72_rocket | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `pointblank:fcl_m72` | 1 | M72 LAW |  |  |

### HAT — США · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: HK416D + Javelin · Альтернативы: SCAR-L
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), STOCK: `tacz:oem_stock_tactical` (Тактический приклад) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:javelin` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:javelin_missile` | 1 | javelin_missile | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), STOCK: `tacz:oem_stock_tactical` (Тактический приклад) |  | ✔ |

### LMG — США · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: M249 SAW · Альтернативы: FN Evolys
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m249` (M249 SAW) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:fn_evolys` (FN Evolys) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — США · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: MG-43 · Альтернативы: FN Evolys
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:fn_evolys` (FN Evolys) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — США · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: Mk14 EBR · Альтернативы: HK417
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk417` (HK417) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки), STOCK: `gucci_attachments:stock_precise` (Приклад Precision-5) |  | ✔ |

### Sniper — США · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: M700 · Альтернативы: MSR
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m700` (M700) · режим SEMI · магазин 5 · обвесы: SCOPE: `tacz:scope_98k` (98k), STOCK: `gucci_attachments:stock_precise` (Приклад Precision-5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 40 | Патроны: `tacz:30_06` (tacz:30_06) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:msr` (MSR) · режим SEMI · магазин 7 · обвесы: SCOPE: `tacz:scope_98k` (98k), STOCK: `gucci_attachments:stock_precise` (Приклад Precision-5) |  | ✔ |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-desert` | 1 | ghillie-helmet-desert |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-desert` | 1 | ghillie-body-desert |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-desert` | 1 | ghillie-legs-desert |  |  |

### Sapper — США · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: MK18 · Альтернативы: M4A1
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:blu_43_mine` | 2 | blu_43_mine | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |

### Scout — США · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: Honey Badger · Альтернативы: MK18
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:honey_badger` (Honey Badger) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1), STOCK: `tacz:oem_stock_light` (Лёгкий приклад) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1), STOCK: `tacz:oem_stock_light` (Лёгкий приклад) |  | ✔ |

### Anti_air — США · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: M4A1 + Игла · Альтернативы: HK416
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 1 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote) |  | ✔ |

### Drone Operator — США · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: HK416 · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Mechanic — США · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: MP7 · Альтернативы: UMP45
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), LASER: `tacz:laser_compact` (Компактный ЛЦУ) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ump45` (UMP45) · режим AUTO · магазин 25 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), LASER: `tacz:laser_compact` (Компактный ЛЦУ) |  | ✔ |

### Mechanic Officer — США · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: HK416 · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Pilot — США · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: MP7 · Альтернативы: P320
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SECONDARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), LASER: `tacz:laser_compact` (Компактный ЛЦУ) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 34 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-multicam` | 1 | Шлем Ops-Core Multicam |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |

### Pilot Officer — США · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: MP7 · Альтернативы: MP5A5
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:jpc` | 1 | Жилет JPC |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk_mp5a5` (MP5A5) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

## Россия (russia) — 19 китов

### Rifleman — Россия · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: АК-74М · Альтернативы: АК-12
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot), MUZZLE: `tacz:muzzle_brake_cyclone_d2` (Пламегаситель Cyclone D2), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot), MUZZLE: `tacz:muzzle_brake_cyclone_d2` (Пламегаситель Cyclone D2), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Assault — Россия · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: АКС-74У · Альтернативы: АК-74М
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Officer — Россия · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: АК-12 · Альтернативы: АК-74М
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:ratnik-10t-wood` | 1 | Шлем Ратник + 10Т |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident) |  | ✔ |

### Medic — Россия · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: АК-74М · Альтернативы: Z-15
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Grenadier — Россия · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: АК-12 + M320 · Альтернативы: АК-74М
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 8 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |

### LAT — Россия · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: АК-74М + РПГ-26 · Альтернативы: АК-47, РПГ-7В2
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__SPECIAL"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 8 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vr` | 1 | fcl_rpg7v2_pg7vr | ✔ |  |
| 9 (Хотбар) | `pointblank:fcl_pgo7` | 1 | fcl_pgo7 |  |  |
| 10 (Инвентарь) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 11 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 12 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2) |  | ✔ |
| 42 (Броня) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS |  |  |

### HAT — Россия · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: АК-12 + RPG-28 · Альтернативы: АК-74М, PF-98
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__SPECIAL"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak12` (АК-12) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg28` | 1 | RPG-28 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg28_rocket` | 1 | fcl_rpg28_rocket | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_pf98_heat` | 1 | fcl_pf98_heat | ✔ |  |
| 8 (Хотбар) | `pointblank:fcl_pf98_he` | 1 | fcl_pf98_he | ✔ |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG) |  | ✔ |
| 42 (Броня) | `pointblank:fcl_pf98` | 1 | PF-98 |  |  |

### LMG — Россия · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: РПК-16 · Альтернативы: РПК-74
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: SCOPE: `rfp:1p87` (simple optic), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk_74` (РПК-74) · режим AUTO · магазин 40 · обвесы: SCOPE: `rfp:1p87` (simple optic), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — Россия · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: 6П41 · Альтернативы: ПКП
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (6П41) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:pkp` (ПКП) · режим AUTO · магазин 70 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — Россия · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: СВД · Альтернативы: СВ-98
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `rfp:1p87` (simple optic), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:sv98` (СВ-98) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_standard_8x` (8x), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Sniper — Россия · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: СВ-98 · Альтернативы: AWP
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:sv98` (СВ-98) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_98k` (98k) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 40 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ai_awp` (AWP) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_98k` (98k) |  | ✔ |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-winter` | 1 | ghillie-helmet-winter |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-winter` | 1 | ghillie-body-winter |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-winter` | 1 | ghillie-legs-winter |  |  |

### Sapper — Россия · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: АК-74 · Альтернативы: АКС-74У
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74` (АК-74) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 3 (Хотбар) | `superbwarfare:tm_62` | 3 | Мина ТМ-62 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Scout — Россия · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: АК-9 · Альтернативы: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak9` (АК-9) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:9x39` (ea:9x39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:9x39` (ea:9x39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:9x39` (ea:9x39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Anti_air — Россия · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: АКС-74У + Игла · Альтернативы: АК-74М
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 1 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

### Drone Operator — Россия · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: АК-74М · Альтернативы: АК-9
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:9x39` (ea:9x39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak9` (АК-9) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Mechanic — Россия · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: Vityaz · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vityas` (Vityaz) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Mechanic Officer — Россия · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: АКС-74УБ · Альтернативы: Vityaz
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74ub` (АКС-74УБ) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:ratnik-10t-wood` | 1 | Шлем Ратник + 10Т |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vityas` (Vityaz) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Pilot — Россия · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: Vityaz · Альтернативы: Глок-17
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SECONDARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vityas` (Vityaz) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:6b47-emr` | 1 | Шлем 6Б47 EMR |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |

### Pilot Officer — Россия · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: АКС-74У · Альтернативы: Vityaz
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:6b45-wood` | 1 | Жилет 6Б45 Woodland |  |  |
| 39 (Броня) | `warbornrenewed:ratnik-10t-wood` | 1 | Шлем Ратник + 10Т |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vityas` (Vityaz) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |

## Украина (ukraine) — 19 китов

### Rifleman — Украина · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: Z-15 · Альтернативы: АК-74М
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_t2` (T2), MUZZLE: `tacz:muzzle_compensator_trident` (Компенсатор Trident) |  | ✔ |

### Assault — Украина · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: АКС-74У · Альтернативы: Z-15
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Officer — Украина · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: Z-15 · Альтернативы: M4A1
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), STOCK: `tacz:oem_stock_tactical` (Тактический приклад) |  | ✔ |

### Medic — Украина · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: Z-15 · Альтернативы: АК-74М
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Grenadier — Украина · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: Z-15 + M320 · Альтернативы: M4A1
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 8 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m4a1` (M4A1) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |

### LAT — Украина · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: Z-15 + AT4 · Альтернативы: АК-74М, РПГ-26
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"], "42": ["__ALT__SPECIAL"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_at4` | 1 | AT4 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_at4_rocket` | 1 | fcl_at4_rocket | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ak74m` (АК-74М) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |
| 42 (Броня) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |

### HAT — Украина · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: Z-15 + Javelin · Альтернативы: SCAR-L
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:javelin` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:javelin_missile` | 1 | javelin_missile | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_l` (SCAR-L) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO) |  | ✔ |

### LMG — Украина · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: РПК-16 · Альтернативы: РПК-74
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk16` (РПК-16) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk_74` (РПК-74) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — Украина · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: RPL-20 · Альтернативы: ПКП
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:rpl20` (RPL-20) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:pkp` (ПКП) · режим AUTO · магазин 70 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — Украина · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: СВД · Альтернативы: Mk14 EBR
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (4x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:mk14` (Mk14 EBR) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO) |  | ✔ |

### Sniper — Украина · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: СВ-98 · Альтернативы: M700
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:sv98` (СВ-98) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_standard_8x` (8x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 40 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:30_06` (tacz:30_06) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m700` (M700) · режим SEMI · магазин 5 · обвесы: SCOPE: `tacz:scope_standard_8x` (8x) |  | ✔ |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — Украина · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: Z-15 · Альтернативы: АКС-74У
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 3 (Хотбар) | `superbwarfare:tm_62` | 3 | Мина ТМ-62 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Scout — Украина · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: АКС-74У · Альтернативы: Honey Badger
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:honey_badger` (Honey Badger) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Anti_air — Украина · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: Z-15 + Игла · Альтернативы: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 1 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

### Drone Operator — Украина · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: Z-15 · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Mechanic — Украина · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: Vityaz · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vityas` (Vityaz) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Mechanic Officer — Украина · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: Z-15 · Альтернативы: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15_a` (Z-15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Pilot — Украина · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: MP7 · Альтернативы: Глок-17
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SECONDARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 34 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-mm14` | 1 | opscore-mm14 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |

### Pilot Officer — Украина · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: Z-15C · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:z15` (Z-15C) · режим SEMI · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-mm14` | 1 | warmor-mm14 |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |

## НАТО (nato) — 19 китов

### Rifleman — НАТО · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: G36 · Альтернативы: AUG
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:g36` (G36) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:aug` (AUG) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `gucci_attachments:grip_vert6` (Вертикальная рукоять) |  | ✔ |

### Assault — НАТО · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: HK416D · Альтернативы: HK G33
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk_g33` (HK G33) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Officer — НАТО · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: SR16 · Альтернативы: HK416D
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:sr16` (SR16) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_exp3` (EXPS3) |  | ✔ |

### Medic — НАТО · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: G36K · Альтернативы: AUG
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:g36k` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:aug` (AUG) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_coyote` (Coyote), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Grenadier — НАТО · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: G36K + M320 · Альтернативы: HK416D
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:g36k` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 8 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `maxstuff:a3_grip` (Рукоять A3) |  | ✔ |

### LAT — НАТО · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: G36K + AT4 · Альтернативы: AUG
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:g36k` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_at4` | 1 | AT4 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_at4_rocket` | 1 | fcl_at4_rocket | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:aug` (AUG) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |

### HAT — НАТО · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: HK416D + Карл Густав M4 · Альтернативы: HK417
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk416d` (HK416D) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), STOCK: `tacz:oem_stock_heavy` (Тяжёлый приклад) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_carlgustafm4` | 1 | Карл Густав M4 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_carlgustaf_heat551crs` | 1 | fcl_carlgustaf_heat551crs | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_carlgustaf_hedp502` | 1 | fcl_carlgustaf_hedp502 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `pointblank:fcl_carlgustaf_scope` | 1 | fcl_carlgustaf_scope |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk417` (HK417) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), STOCK: `tacz:oem_stock_heavy` (Тяжёлый приклад) |  | ✔ |

### LMG — НАТО · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: AUG HBAR · Альтернативы: FN Evolys
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aug_hbar` (AUG HBAR) · режим AUTO · магазин 42 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 12 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:fn_evolys` (FN Evolys) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:scope_acog_ta31` (ACOG), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — НАТО · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: FN FAL · Альтернативы: HK417
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:fn_fal` (FN FAL) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk417` (HK417) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — НАТО · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: HK417 · Альтернативы: SCAR-H
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk417` (HK417) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_h` (SCAR-H) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Sniper — НАТО · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: MSR · Альтернативы: M700
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:msr` (MSR) · режим SEMI · магазин 7 · обвесы: SCOPE: `tacz:scope_mk5hd` (MK5HD), STOCK: `gucci_attachments:stock_precise` (Приклад Precision-5) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 40 | Патроны: `tacz:30_06` (tacz:30_06) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:m700` (M700) · режим SEMI · магазин 5 · обвесы: SCOPE: `tacz:scope_mk5hd` (MK5HD), STOCK: `gucci_attachments:stock_precise` (Приклад Precision-5) |  | ✔ |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — НАТО · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: MK18 · Альтернативы: G36K
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:blu_43_mine` | 2 | blu_43_mine | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:g36k` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Scout — НАТО · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: Honey Badger · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:honey_badger` (Honey Badger) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Anti_air — НАТО · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: HK416 + Игла · Альтернативы: G36K
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 1 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:g36k` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

### Drone Operator — НАТО · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: HK416 · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Mechanic — НАТО · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: MP7 · Альтернативы: UMP9
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), LASER: `tacz:laser_compact` (Компактный ЛЦУ) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ump9` (UMP9) · режим AUTO · магазин 25 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), LASER: `tacz:laser_compact` (Компактный ЛЦУ) |  | ✔ |

### Mechanic Officer — НАТО · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: G36K · Альтернативы: MP5SD
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:g36k` (G36K) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk_mp5sd` (MP5SD) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Pilot — НАТО · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: MP5A5 · Альтернативы: Глок-17
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SECONDARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:hk_mp5a5` (MP5A5) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 34 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:nato-wood-helmet` | 1 | Шлем NATO Woodland |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |

### Pilot Officer — НАТО · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: MP7 · Альтернативы: P320
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SECONDARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:45acp` (.45 ACP) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:nato-wood-chestplate` | 1 | Жилет NATO Woodland |  |  |
| 39 (Броня) | `warbornrenewed:gpngv-nato-wood` | 1 | Шлем ECH + GPNVG-18 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:p320` (P320) · режим SEMI · магазин 12 |  | ✔ |

## Инсургенты (insurgency) — 19 китов

### Rifleman — Инсургенты · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: Тип-56 · Альтернативы: АК-47
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:type56` (Тип-56) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |

### Assault — Инсургенты · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: АК-47 · Альтернативы: АКС-74У
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

### Officer — Инсургенты · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: АК-47 · Альтернативы: Тип-56
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:type56` (Тип-56) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |

### Medic — Инсургенты · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: Тип-56 · Альтернативы: ППШ-41
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:type56` (Тип-56) · режим AUTO · магазин 30 |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 |  | ✔ |

### Grenadier — Инсургенты · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: АК-47 + M320 · Альтернативы: Тип-56
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 8 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:type56` (Тип-56) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |

### LAT — Инсургенты · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: АК-47 + РПГ-7В2 INS · Альтернативы: РПГ-26
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SPECIAL"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg7v2` | 1 | РПГ-7В2 INS |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg7v2_og7v` | 1 | fcl_rpg7v2_og7v | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_rpg7v2_pg7vm` | 1 | fcl_rpg7v2_pg7vm | ✔ |  |
| 8 (Хотбар) | `pointblank:fcl_rpg26_rocket` | 1 | fcl_rpg26_rocket | ✔ |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |
| 41 (Инвентарь) | `pointblank:fcl_rpg26` | 1 | РПГ-26 |  |  |

### HAT — Инсургенты · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: АК-47 + RPG-28 · Альтернативы: PF-98
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SPECIAL"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_rpg28` | 1 | RPG-28 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_rpg28_rocket` | 1 | fcl_rpg28_rocket | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_pf98_heat` | 1 | fcl_pf98_heat | ✔ |  |
| 8 (Хотбар) | `pointblank:fcl_pf98_he` | 1 | fcl_pf98_he | ✔ |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |
| 41 (Инвентарь) | `pointblank:fcl_pf98` | 1 | PF-98 |  |  |

### LMG — Инсургенты · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: РПК · Альтернативы: ДП-27
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:rpk` (РПК) · режим AUTO · магазин 40 · обвесы: GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 20 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 17 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:dp28` (ДП-27) · режим AUTO · магазин 47 · обвесы: GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — Инсургенты · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: ПКП · Альтернативы: 6П41
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:pkp` (ПКП) · режим AUTO · магазин 70 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 50 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:6p41bp` (6П41) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — Инсургенты · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: СКС · Альтернативы: СВД
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:sks_wooden` (СКС) · режим SEMI · магазин 9 · обвесы: SCOPE: `tacz:scope_elcan_4x` (4x) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 10 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:svd` (СВД) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_elcan_4x` (4x) |  | ✔ |

### Sniper — Инсургенты · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: Kar98k · Альтернативы: СВ-98
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:kar98` (Kar98k) · режим SEMI · магазин 4 · обвесы: SCOPE: `tacz:scope_98k` (98k) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 1 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 32 | Патроны: `tacz:792x57` (7.92×57) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x54` (7.62×54R) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:sv98` (СВ-98) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_standard_8x` (8x) |  | ✔ |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-jungle` | 1 | ghillie-helmet-jungle |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-jungle` | 1 | ghillie-body-jungle |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-jungle` | 1 | ghillie-legs-jungle |  |  |

### Sapper — Инсургенты · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: АКС-74У · Альтернативы: ППШ-41
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |
| 3 (Хотбар) | `superbwarfare:tm_62` | 1 | Мина ТМ-62 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:lunge_mine` | 2 | lunge_mine | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 1 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:iotv-black` | 1 | Жилет IOTV Black |  |  |
| 39 (Броня) | `warbornrenewed:pastgt-black` | 1 | Шлем PASGT Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_okp7` (simple red dot) |  | ✔ |

### Scout — Инсургенты · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: ППШ-41 · Альтернативы: АК-47
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 20 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

### Anti_air — Инсургенты · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: АКС-74У + Игла · Альтернативы: Тип-56
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 1 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:type56` (Тип-56) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

### Drone Operator — Инсургенты · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: Тип-56 · Альтернативы: АКС-74У
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:type56` (Тип-56) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (simple optic) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:aks74u` (АКС-74У) · режим AUTO · магазин 30 · обвесы: SCOPE: `rfp:1p87` (simple optic) |  | ✔ |

### Mechanic — Инсургенты · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: ППШ-41 · Альтернативы: Uzi
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 20 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:uzi` (Uzi) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

### Mechanic Officer — Инсургенты · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: АК-47 · Альтернативы: ППШ-41
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:ak47` (АК-47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_uh1` (Holo) |  | ✔ |

### Pilot — Инсургенты · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: Uzi · Альтернативы: Глок-17
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SECONDARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:uzi` (Uzi) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 40 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 34 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:glock_17` (Глок-17) · режим SEMI · магазин 17 |  | ✔ |

### Pilot Officer — Инсургенты · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: ППШ-41 · Альтернативы: Uzi
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `cib:ppsh41` (ППШ-41) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `tacz:cz75` (CZ-75) · режим SEMI · магазин 16 |  | ✔ |
| 2 (Хотбар) | `superbwarfare:rgo_grenade` | 2 | Граната РГО | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x25` (7.62×25) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:uzi` (Uzi) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

## ЧВК (pmc) — 19 китов

### Rifleman — ЧВК · Стрелок — основа отряда — держит линию огня и снабжает бойцов патронами · Оружие: HK416 · Альтернативы: MK18
Категория: `DIRECT_COMBAT` · Лимиты: команда -1 · отряд -1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:ammo_bag` | 1 | Аммо-мешок |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Assault — ЧВК · Штурмовик — штурм в первом эшелоне — прорывает оборону противника в ближнем бою · Оружие: MK47 · Альтернативы: Вектор
Категория: `DIRECT_COMBAT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk47` (MK47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Officer — ЧВК · Офицер — командир отряда — ставит рали, держит рацию и вызывает арту · Оружие: SR15 · Альтернативы: HK416
Категория: `SUPPORT` · Лимиты: команда 6 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:sr15` (SR15) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 6 (Хотбар) | `superbwarfare:artillery_indicator` | 1 | Арт-индикатор |  | ✔ |
| 7 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-fc-b2200-voevoda` | 1 | Шлем Ops-Core + FC-B2200 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Medic — ЧВК · Медик — спасает жизни — бинты, аптечка и подъём бойцов с земли · Оружие: MP7 · Альтернативы: Honey Badger
Категория: `SUPPORT` · Лимиты: команда 10 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 9 | Бинт | ✔ |  |
| 6 (Хотбар) | `pwp_medicine:medkit` | 1 | Аптечка | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:honey_badger` (Honey Badger) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Grenadier — ЧВК · Гренадёр — 40-мм поддержка — накрывает позиции противника из гранатомёта · Оружие: HK416 + M320 · Альтернативы: MK18
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:m320t` (M320) · режим SEMI · магазин 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `tacz:ammo` | 8 | Патроны: `tacz:40mm` (40-мм) | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### LAT — ЧВК · ЛАТ — лёгкая противотанковая — одноразовая труба против лёгкой техники · Оружие: MK18 + Panzerfaust 3 · Альтернативы: AT4
Категория: `FIRE_SUPPORT` · Лимиты: команда 6 · отряд 2 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SPECIAL"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_panzerfaust3` | 1 | Panzerfaust 3 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_panzerfaust3_heat` | 1 | fcl_panzerfaust3_heat | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_panzerfaust3_tandem` | 1 | fcl_panzerfaust3_tandem | ✔ |  |
| 8 (Хотбар) | `pointblank:fcl_at4_rocket` | 1 | fcl_at4_rocket | ✔ |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `pointblank:fcl_at4` | 1 | AT4 |  |  |

### HAT — ЧВК · ХАТ — охота на технику — тяжёлая труба с кумулятивными боеприпасами · Оружие: MK47 + Карл Густав M4 · Альтернативы: SCAR-H
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 3 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk47` (MK47) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO) |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `pointblank:fcl_carlgustafm4` | 1 | Карл Густав M4 |  |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `pointblank:fcl_carlgustaf_heat551crs` | 1 | fcl_carlgustaf_heat551crs | ✔ |  |
| 7 (Хотбар) | `pointblank:fcl_carlgustaf_hedp502` | 1 | fcl_carlgustaf_hedp502 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `pointblank:fcl_carlgustaf_scope` | 1 | fcl_carlgustaf_scope |  |  |
| 10 (Инвентарь) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 11 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:762x39` (7.62×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:scar_h` (SCAR-H) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO) |  | ✔ |

### LMG — ЧВК · Пулемётчик — подавляющий огонь — длинная очередь держит противника в укрытии · Оружие: РПК-74М · Альтернативы: FN Evolys
Категория: `DIRECT_COMBAT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:rpk_74m` (РПК-74М) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:545x39` (5.45×39) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:fn_evolys` (FN Evolys) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### HMG — ЧВК · Тяжёлый пулемёт — тяжёлая огневая точка — крупный калибр против брони и укреплений · Оружие: FN Evolys · Альтернативы: MG-43
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `tacz:fn_evolys` (FN Evolys) · режим AUTO · магазин 75 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `rfp:mg43` (MG-43) · режим AUTO · магазин 100 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Marksman — ЧВК · Марксман — точные выстрелы на дистанции — прикрывает отряд с дальней позиции · Оружие: SCAR-SSR · Альтернативы: HK417
Категория: `FIRE_SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:scar_ssr` (SCAR-SSR) · режим SEMI · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:65creedmoor` (6.5 Creedmoor) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk417` (HK417) · режим AUTO · магазин 20 · обвесы: SCOPE: `tacz:scope_lpvo_1_6` (LPVO), GRIP: `gucci_attachments:grip_bipod1` (Сошки) |  | ✔ |

### Sniper — ЧВК · Снайпер — одна пуля — одна цель — работает из глубокого тыла · Оружие: MSR · Альтернативы: AWP
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:msr` (MSR) · режим SEMI · магазин 7 · обвесы: SCOPE: `tacz:scope_mk5hd` (MK5HD) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 1 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 1 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 40 | Патроны: `tacz:30_06` (tacz:30_06) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:308` (7.62×51 (.308)) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ai_awp` (AWP) · режим SEMI · магазин 10 · обвесы: SCOPE: `tacz:scope_mk5hd` (MK5HD) |  | ✔ |
| 42 (Броня) | `warbornrenewed:ghillie-helmet-desert` | 1 | ghillie-helmet-desert |  |  |
| 44 (Броня) | `warbornrenewed:ghillie-body-desert` | 1 | ghillie-body-desert |  |  |
| 46 (Броня) | `warbornrenewed:ghillie-legs-desert` | 1 | ghillie-legs-desert |  |  |

### Sapper — ЧВК · Сапёр — мины и подрывы — ставит мины и сносит вражеские постройки · Оружие: MK18 AUTO · Альтернативы: Honey Badger
Категория: `SPECIALIST` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18_auto` (MK18 AUTO) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 3 (Хотбар) | `superbwarfare:claymore_mine` | 2 | Клеймор | ✔ |  |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 6 (Хотбар) | `superbwarfare:blu_43_mine` | 2 | blu_43_mine | ✔ |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:detonator` | 1 | Детонатор |  |  |
| 10 (Инвентарь) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 11 (Инвентарь) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 16 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 33 (Доп. заряд) | `superbwarfare:c4_bomb` | 2 | C4 · Control=1 | ✔ | ✔ |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:honey_badger` (Honey Badger) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Scout — ЧВК · Разведчик — глаза отряда — разведывательный дрон и скрытное продвижение · Оружие: Honey Badger · Альтернативы: Вектор
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:honey_badger` (Honey Badger) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:mavic_drone_no_drop` | 1 | uncomplicatedfpv:mavic_drone_no_drop | ✔ |  |
| 6 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `ea:300blk` (.300 BLK) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Anti_air — ЧВК · ПВО — защита неба — ПЗРК сбивает вражескую воздушную технику · Оружие: MP7 + Игла · Альтернативы: HK416
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:igla_9k38` | 1 | ПЗРК/пускач · зарядов в стволе: 1 |  | ✔ |
| 4 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 5 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `superbwarfare:medium_anti_air_missile` | 1 | medium_anti_air_missile | ✔ |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |

### Drone Operator — ЧВК · Оператор дрона — разведка с воздуха — FPV-камикадзе и мавик со сбросом · Оружие: Вектор · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 2 · отряд 1 · мин. отряд 2 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `uncomplicatedfpv:fpv_drone` | 1 | uncomplicatedfpv:fpv_drone | ✔ |  |
| 6 (Хотбар) | `uncomplicatedfpv:mavic_drone_with_drop` | 1 | uncomplicatedfpv:mavic_drone_with_drop | ✔ |  |
| 7 (Хотбар) | `superbwarfare:monitor` | 1 | monitor |  |  |
| 8 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 9 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `pwpwarfare:drone_ammo_pouch` | 2 | pwpwarfare:drone_ammo_pouch |  |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 15 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Mechanic — ЧВК · Механик — ремонт техники — чинит и обслуживает машины на поле боя · Оружие: Вектор · Альтернативы: UMP9
Категория: `SUPPORT` · Лимиты: команда 4 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:vector9` (Вектор) · режим AUTO · магазин 24 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), LASER: `tacz:laser_compact` (Компактный ЛЦУ) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 7 (Хотбар) | `pwpwarfare:entrenching_tool` | 1 | Лопата |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 48 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:ump9` (UMP9) · режим AUTO · магазин 25 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR), LASER: `tacz:laser_compact` (Компактный ЛЦУ) |  | ✔ |

### Mechanic Officer — ЧВК · Механик-офицер — руководит ремонтами — лидер экипажей и ремзоны · Оружие: HK416 · Альтернативы: MK18
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 7 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 9 (Хотбар) | `warbornrenewed:binocular` | 1 | Бинокль |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-fc-b2200-voevoda` | 1 | Шлем Ops-Core + FC-B2200 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mk18` (MK18) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |

### Pilot — ЧВК · Пилот — управляет небом — вертолёты и воздушная поддержка · Оружие: MP7 · Альтернативы: Глок-18
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · нет
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__SECONDARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_rmr_dot` (RMR) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 34 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 39 (Броня) | `warbornrenewed:opscore-black` | 1 | Шлем Ops-Core Black |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:glock_18c` (Глок-18) · режим SEMI · магазин 17 |  | ✔ |

### Pilot Officer — ЧВК · Пилот-офицер — командир экипажа — ведёт группу воздушной техники · Оружие: HK416 · Альтернативы: MP7
Категория: `SUPPORT` · Лимиты: команда 3 · отряд 1 · мин. отряд 0 · лидер
Альтернативы в меню деплоя (`slotSkins`): {"41": ["__ALT__PRIMARY"]}

| Слот | Что | Count | Детали | R | NBT |
|---|---|---|---|---|---|
| 0 (Праймари) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:hk416c` (HK416) · режим AUTO · магазин 30 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
| 1 (Секондари/Труба) | `tacz:modern_kinetic_gun` | 1 | Пистолет: `maxstuff:m17` (M17) · режим SEMI · магазин 17 |  | ✔ |
| 2 (Хотбар) | `lrtactical:throwable` | 2 | Граната: `lrtactical:m67` | ✔ |  |
| 3 (Хотбар) | `superbwarfare:m18_smoke_grenade` | 2 | Дым M18 · Color=11546150 | ✔ |  |
| 4 (Хотбар) | `pwp_medicine:bandage` | 2 | Бинт | ✔ |  |
| 5 (Хотбар) | `superbwarfare:repair_tool` | 1 | Ремнабор · Energy=100000 |  | ✔ |
| 6 (Хотбар) | `pwpwarfare:squad_leader_radio` | 1 | Рация |  |  |
| 8 (Хотбар) | `lrtactical:melee` | 1 | Нож: `lrtactical:dagger` |  |  |
| 10 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 11 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 12 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:556x45` (5.56×45) | ✔ |  |
| 13 (Инвентарь) | `tacz:ammo` | 60 | Патроны: `tacz:46x30` (4.6×30) | ✔ |  |
| 14 (Инвентарь) | `tacz:ammo` | 30 | Патроны: `tacz:9mm` (9×19) | ✔ |  |
| 36 (Броня) | `survival_instinct:military_boots` | 1 | Ботинки |  |  |
| 37 (Броня) | `survival_instinct:military_leggings` | 1 | Штаны |  |  |
| 38 (Броня) | `warbornrenewed:warmor-black` | 1 | Жилет Warmor Black |  |  |
| 39 (Броня) | `warbornrenewed:opscore-fc-b2200-voevoda` | 1 | Шлем Ops-Core + FC-B2200 |  |  |
| 41 (Инвентарь) | `tacz:modern_kinetic_gun` | 1 | Праймари: `maxstuff:mp7` (MP7) · режим AUTO · магазин 40 · обвесы: SCOPE: `tacz:sight_uh1` (Holo), MUZZLE: `tacz:muzzle_silencer_phantom_s1` (Глушитель Phantom S1) |  | ✔ |
