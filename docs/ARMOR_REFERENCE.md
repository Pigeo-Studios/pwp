# Справочник брони: Warborn Renewed (WRB) + SuperbWarfare

> WRB 0.4.1 (`WRB-Armor-0.4.1.jar`). Статы извлечены декомпиляцией bytecode (javap).

## Материалы WRB (ModArmorMaterials)

| Материал | Прочность × | Защита (boots/legs/chest/helm) | Чары | Твёрдость | Отдача-резист |
|---|---|---|---|---|---|
| LEATHER | 5 | 0 / 0 / 0 / 0 | 0 | 0.0 | 0.0 |
| KEVLAR | 20 | 1 / 3 / 5 / 2 | 15 | 0.5 | 0.0 |
| CERAMIC | 30 | 2 / 5 / 7 / 3 | 12 | 2.5 | 0.05 |
| AR500_STEEL | 40 | 2 / 4 / 6 / 2 | 10 | 2.0 | 0.1 |
| UHMWPE | 45 | 3 / 6 / 8 / 3 | 18 | 3.0 | 0.08 |
| COMPOSITE | 55 | 3 / 7 / 9 / 4 | 15 | 4.0 | 0.15 |
| TITANIUM | 60 | 4 / 7 / 9 / 4 | 20 | 4.5 | 0.2 |

Базовые прочности слотов: boots 13 / legs 15 / chest 16 / helm 11; итоговая прочность = база × множитель материала.

## Куски брони WRB (WarbornArmorSets.class)

| Кусок | Материал | bulletResistance | blastResistance | protectionClass | movementSpeed |
|---|---|---|---|---|---|
| `6b45-desert` | UHMWPE | 0.55 | 0.5 | 5 | -0.08 |
| `6b45-wood` | UHMWPE | 0.55 | 0.5 | 5 | -0.08 |
| `6b47-atacsfg` | KEVLAR | 0.35 | 0.02 | 1 | -0.02 |
| `6b47-desert` | KEVLAR | 0.35 | 0.02 | 1 | -0.02 |
| `6b47-emr` | KEVLAR | 0.35 | 0.02 | 1 | -0.02 |
| `6b47-fc-b2200-sso` | KEVLAR | 0.4 | 0.02 | 2 | -0.02 |
| `6b47-green` | KEVLAR | 0.35 | 0.02 | 1 | -0.02 |
| `6b47-winteremr` | KEVLAR | 0.35 | 0.02 | 1 | -0.02 |
| `arm_bandage` | LEATHER | ? | ? | ? | ? |
| `ghillie-body-desert` | LEATHER | 0 | ? | 0 | 0 |
| `ghillie-body-jungle` | LEATHER | 0 | ? | 0 | 0 |
| `ghillie-body-winter` | LEATHER | 0 | ? | 0 | 0 |
| `ghillie-helmet-desert` | LEATHER | 0 | ? | 0 | 0 |
| `ghillie-helmet-jungle` | LEATHER | 0 | ? | 0 | 0 |
| `ghillie-helmet-winter` | LEATHER | 0 | ? | 0 | 0 |
| `ghillie-legs-desert` | LEATHER | 0 | ? | 0 | 0 |
| `ghillie-legs-jungle` | LEATHER | 0 | ? | 0 | 0 |
| `ghillie-legs-winter` | LEATHER | 0 | ? | 0 | 0 |
| `gpngv-nato-desert` | KEVLAR | 0.4 | 0.02 | 3 | -0.03 |
| `gpngv-nato-wood` | KEVLAR | 0.4 | 0.02 | 3 | -0.03 |
| `iotv-black` | CERAMIC | 0.5 | 0.4 | 4 | -0.07 |
| `iotv-desert` | CERAMIC | 0.5 | 0.4 | 4 | -0.07 |
| `iotv-green` | CERAMIC | 0.5 | 0.4 | 4 | -0.07 |
| `iotv-multicam` | CERAMIC | 0.5 | 0.4 | 4 | -0.07 |
| `iotv-ucp` | CERAMIC | 0.5 | 0.4 | 4 | -0.07 |
| `iotv-white` | CERAMIC | 0.5 | 0.4 | 4 | -0.07 |
| `jpc` | AR500_STEEL | 0.55 | 0.2 | 4 | -0.05 |
| `jpc-desert` | AR500_STEEL | 0.55 | 0.2 | 4 | -0.05 |
| `leg_bandage` | LEATHER | ? | ? | ? | ? |
| `nato-sand-chestplate` | CERAMIC | 0.5 | 0.25 | 4 | -0.06 |
| `nato-sand-helmet` | KEVLAR | 0.4 | 0.02 | 3 | -0.03 |
| `nato-wood-chestplate` | CERAMIC | 0.5 | 0.25 | 4 | -0.06 |
| `nato-wood-helmet` | KEVLAR | 0.4 | 0.02 | 3 | -0.03 |
| `opscore-atacsfg` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `opscore-black` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `opscore-desert` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `opscore-emr` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `opscore-fc-b2200-voevoda` | KEVLAR | 0.45 | 0.03 | 3 | -0.02 |
| `opscore-green` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `opscore-mm14` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `opscore-multicam` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `opscore-standard` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `opscore-white` | KEVLAR | 0.4 | 0.03 | 2 | -0.02 |
| `panama-atacsfg` | LEATHER | 0 | ? | 0 | 0 |
| `panama-desert` | LEATHER | 0 | ? | 0 | 0 |
| `panama-emr` | LEATHER | 0 | ? | 0 | 0 |
| `panama-green` | LEATHER | 0 | ? | 0 | 0 |
| `panama-multicam` | LEATHER | 0 | ? | 0 | 0 |
| `panama-ucp` | LEATHER | 0 | ? | 0 | 0 |
| `panama-white` | LEATHER | 0 | ? | 0 | 0 |
| `pastgt-black` | KEVLAR | 0.3 | 0.02 | 1 | -0.02 |
| `pastgt-blue` | KEVLAR | 0.3 | 0.02 | 1 | -0.02 |
| `pastgt-desert` | KEVLAR | 0.3 | 0.02 | 1 | -0.02 |
| `pastgt-green` | KEVLAR | 0.3 | 0.02 | 1 | -0.02 |
| `pastgt-mm14` | KEVLAR | 0.3 | 0.02 | 1 | -0.02 |
| `pastgt-multicam` | KEVLAR | 0.3 | 0.02 | 1 | -0.02 |
| `pastgt-white` | KEVLAR | 0.3 | 0.02 | 1 | -0.02 |
| `press-helmet` | KEVLAR | 0.3 | 0.02 | 1 | -0.01 |
| `press-vest` | CERAMIC | 0.5 | 0.3 | 4 | -0.02 |
| `ratnik-10t-desert` | KEVLAR | 0.35 | 0.02 | 2 | -0.03 |
| `ratnik-10t-wood` | KEVLAR | 0.35 | 0.02 | 2 | -0.03 |
| `uwin` | CERAMIC | 0.6 | 0.1 | 5 | -0.04 |
| `uwin-desert` | CERAMIC | 0.6 | 0.1 | 5 | -0.04 |
| `warmor-black` | COMPOSITE | 0.6 | 0.45 | 5 | -0.06 |
| `warmor-desert` | COMPOSITE | 0.6 | 0.45 | 5 | -0.06 |
| `warmor-green` | COMPOSITE | 0.6 | 0.45 | 5 | -0.06 |
| `warmor-mm14` | COMPOSITE | 0.6 | 0.45 | 5 | -0.06 |
| `warmor-multicam` | COMPOSITE | 0.6 | 0.45 | 5 | -0.06 |
| `warmor-ucp` | COMPOSITE | 0.6 | 0.45 | 5 | -0.06 |
| `warmor-white` | COMPOSITE | 0.6 | 0.45 | 5 | -0.06 |

## Предметы WRB (registry id = `warbornrenewed:<id>`)

| ID | Название (EN) | Название (RU) |
|---|---|---|
| `6b47-atacsfg` | 6B47 Helmet (ATACS-FG) | Шлем 6Б47 (АТАКС-ФГ) |
| `6b47-desert` | 6B47 Helmet (Desert) | Шлем 6Б47 (Пустынный) |
| `6b47-emr` | 6B47 Helmet (EMR) | Шлем 6Б47 (ЕМР) |
| `6b47-green` | 6B47 Helmet (Olive) | Шлем 6Б47 (Оливковый) |
| `6b47-sso-helmet` | ? | ? |
| `6b47-winter` | ? | ? |
| `arm_bandage` | Arm Bandage | Повязка на руку |
| `binocular` | Binoculars | Бинокль |
| `fma` | ? | ? |
| `ghillie-body-desert` | Ghillie Suit Top (Desert) | Маскхалат Куртка (Пустынный) |
| `ghillie-body-jungle` | Ghillie Suit Top (Jungle) | Маскхалат Куртка (Джунгли) |
| `ghillie-body-winter` | Ghillie Suit Top (Winter) | Маскхалат Куртка (Зимняя) |
| `ghillie-helmet-desert` | Ghillie Hood (Desert) | Маскхалат Капюшон (Пустынный) |
| `ghillie-helmet-jungle` | Ghillie Hood (Jungle) | Маскхалат Капюшон (Джунгли) |
| `ghillie-helmet-winter` | Ghillie Hood (Winter) | Маскхалат Капюшон (Зимний) |
| `ghillie-legs-desert` | Ghillie Suit Pants (Desert) | Маскхалат Штаны (Пустынные) |
| `ghillie-legs-jungle` | Ghillie Suit Pants (Jungle) | Маскхалат Штаны (Джунгли) |
| `ghillie-legs-winter` | Ghillie Suit Pants (Winter) | Маскхалат Штаны (Зимние) |
| `gpngv-nato-helmet-desert` | ? | ? |
| `gpngv-nato-helmet-woodland` | ? | ? |
| `iotv-black` | IOTV Vest (Black) | Бронежилет IOTV (Чёрный) |
| `iotv-desert` | IOTV Vest (Desert) | Бронежилет IOTV (Пустынный) |
| `iotv-green` | IOTV Vest (Olive) | Бронежилет IOTV (Оливковый) |
| `iotv-multicam` | IOTV Vest (Multicam) | Бронежилет IOTV (Мультикам) |
| `iotv-ucp` | IOTV Vest (UCP) | Бронежилет IOTV (UCP) |
| `jpc` | Crye JPC Plate Carrier | Разгрузка Crye JPC |
| `jpc-desert` | Crye JPC Plate Carrier (Desert) | Разгрузка Crye JPC (Пустынная) |
| `nato-sand-chestplate` | NATO Body Armor (Sand) | Бронежилет NATO (Песочный) |
| `nato-sand-helmet` | NATO Helmet (Sand) | Шлем NATO (Песочный) |
| `nato-wood-chestplate` | NATO Body Armor (Woodland) | Бронежилет NATO (Лесной) |
| `nato-wood-helmet` | NATO Helmet (Woodland) | Шлем NATO (Лесной) |
| `opscore` | ? | ? |
| `opscore-atacsfg` | Ops-Core FAST Helmet (ATACS-FG) | Шлем Ops-Core FAST (АТАКС-ФГ) |
| `opscore-black` | Ops-Core FAST Helmet (Black) | Шлем Ops-Core FAST (Чёрный) |
| `opscore-desert` | Ops-Core FAST Helmet (Desert) | Шлем Ops-Core FAST (Пустынный) |
| `opscore-emr` | Ops-Core FAST Helmet (EMR) | Шлем Ops-Core FAST (ЕМР) |
| `opscore-green` | Ops-Core FAST Helmet (Olive) | Шлем Ops-Core FAST (Оливковый) |
| `opscore-mm14` | Ops-Core FAST Helmet (MM14) | Шлем Ops-Core FAST (ММ14) |
| `opscore-multicam` | Ops-Core FAST Helmet (Multicam) | Шлем Ops-Core FAST (Мультикам) |
| `opscore_atacs_helmet` | ? | ? |
| `panama-atacsfg` | Boonie Hat (ATACS-FG) | Панама (АТАКС-ФГ) |
| `panama-desert` | Boonie Hat (Desert) | Панама (Пустынная) |
| `panama-emr` | Boonie Hat (EMR) | Панама (ЕМР) |
| `panama-green` | Boonie Hat (Olive) | Панама (Оливковая) |
| `panama-multicam` | Boonie Hat (Multicam) | Панама (Мультикам) |
| `panama-ucp` | Boonie Hat (UCP) | Панама (UCP) |
| `pastgt-black` | PASGT Helmet (Black) | Шлем PASGT (Чёрный) |
| `pastgt-blue` | PASGT Helmet (UN Blue) | Шлем PASGT (ООН) |
| `pastgt-desert` | PASGT Helmet (Desert) | Шлем PASGT (Пустынный) |
| `pastgt-green` | PASGT Helmet (Olive) | Шлем PASGT (Оливковый) |
| `pastgt-mm14` | PASGT Helmet (MM14) | Шлем PASGT (ММ14) |
| `pastgt-multicam` | PASGT Helmet (Multicam) | Шлем PASGT (Мультикам) |
| `press-chestplate` | ? | ? |
| `press-helmet` | Press Helmet | Шлем «Пресса» |
| `ratnik-chest` | ? | ? |
| `ratnik-chest-desert` | ? | ? |
| `ratnik-helmet` | ? | ? |
| `ratnik-helmet-desert` | ? | ? |
| `ratnik-sand-chestplate` | ? | ? |
| `ratnik-sand-helmet` | ? | ? |
| `ratnik10t` | ? | ? |
| `ratnik10tdesert` | ? | ? |
| `reb-backpack-desert` | REB Backpack (Desert) | Рюкзак РЭБ (Пустынный) |
| `reb-backpack-emr` | REB Backpack (EMR) | Рюкзак РЭБ (ЕМР) |
| `reb-backpack-green` | REB Backpack (Olive) | Рюкзак РЭБ (Оливковый) |
| `reb-backpack-multicam` | REB Backpack (Multicam) | Рюкзак РЭБ (Мультикам) |
| `spcs` | ? | ? |
| `spcs-desert` | ? | ? |
| `uwin` | UWIN Plate Carrier | Бронежилет UWIN |
| `uwin-desert` | UWIN Plate Carrier (Desert) | Бронежилет UWIN (Пустынный) |
| `voevoda-atacs-chestplate` | ? | ? |
| `warmor-black` | Warmor Gen 3 (Black) | Бронежилет Warmor Gen 3 (Чёрный) |
| `warmor-desert` | Warmor Gen 3 (Desert) | Бронежилет Warmor Gen 3 (Пустынный) |
| `warmor-green` | Warmor Gen 3 (Olive) | Бронежилет Warmor Gen 3 (Оливковый) |
| `warmor-mm14` | Warmor Gen 3 (MM14) | Бронежилет Warmor Gen 3 (ММ14) |
| `warmor-multicam` | Warmor Gen 3 (Multicam) | Бронежилет Warmor Gen 3 (Мультикам) |
| `warmor-ucp` | Warmor Gen 3 (UCP) | Бронежилет Warmor Gen 3 (UCP) |

## Броня SuperbWarfare

| ID | Название |
|---|---|
| `superbwarfare:ru_helmet_6b47` | Russian 6b47 Helmet |
| `superbwarfare:ru_chest_6b43` | Russian 6b43 Chest |
| `superbwarfare:us_helmet_pasgt` | US PASGT Helmet |
| `superbwarfare:us_chest_iotv` | US IOTV Chest |
| `superbwarfare:ge_helmet_m_35` | German M35 Helmet |
