# PWP Warfare

**Forge 1.20.1 | Зависимости: GeckoLib 4.4.9, Curios 5.14.1, Simple Voice Chat API 2.6.20**

---

## Команды

Все op (2), префикс: `/pwpwarfare`

| Команда | Описание |
|---|---|
| `gamestart <true\|false>` | Старт/стоп |
| `deathtimer <сек>` | Время респавна |
| `deathtickets <кол-во>` | Стоимость смерти в тикетах |
| `teamtickets <blue\|red> <кол-во>` | Тикеты команде |
| `clearsquad <blue\|red>` | Очистить отряды |
| `teamjoin <blue\|red> <игрок>` | Переместить игрока |
| `teamspawn <blue\|red\|none> <pos>` | Точка спавна |
| `addpoint <shape> <pos1> <pos2> <name> <bluePrio> <redPrio> <timeMin> <penalty> <captureDeduct> <lockMin>` | Точка захвата |
| `removepoint <name>` | Удалить точку |
| `pointclear <name>` | Сбросить захват |
| `pointcapture <blue\|red> <name>` | Принудительный захват |
| `mainzone <blue\|red> <cube\|cylinder> <pos1> <pos2>` | Зона главной базы |
| `removemainzone <blue\|red>` | Удалить зону |
| `map setimage <name>` | Карта |
| `map setcenter <x> <z>` | Центр карты |
| `map setsize <блоки>` | Размер карты |
| `fraction <blue\|red> <faction>` | Фракция |
| `warn <игрок> <сообщение>` | Предупреждение |
| `cleartenkill <игрок>` | Сбросить тимкиллы |
| `votestart <true\|false>` | Голосование |

Клиент: `/compass scale <1|2|3>`

---

## Игровой режим

- Две команды: BLUE и RED.
- Тикеты (default 800). Тратятся со смертью игрока или техники.
- Точки захвата с приоритетами, временем захвата, штрафами и блокировкой.
- Победа: обнуление тикетов противника или захват всех его точек.
- Голосование за старт, автостарт через `voteAutoStartTime` мин.

---

## Отряды

- Хранятся в `WarfareWorldData.squads`. Синхронизация через `PacketSyncSquads`.
- Приглашения/вступление/выход через GUI и пакеты (`PacketSquadAction`).
- Лидер отряда ставит маркеры на карте (`PacketSquadMarker`).
- Глоу соклановцев (рендер).
- Чат отряда.

---

## Нокаут (Downed)

- `enableKnockout`: true — включён.
- При летальном уроне игрок падает (6 HP, поза SWIMMING, звук).
- Оживление: ПКМ предметом из `reviveItem` (default `pwp_medicine:medkit`). Предмет тратится.
- Таймер: `maxDownedTimeSeconds` (default 180). По истечении — `forceGiveUp()`.
- Кулдаун после поднятия: `reviveCooldownSeconds` (default 120) — повторная смерть мгновенна.
- Сдача (`WARFARE_GivingUp`): при получении урона в 0 HP в дауне или таймер.
- Тимкиллы: счётчик, предупреждения, бан на базу (5 мин), спектатор (8+).
- Очистка наказания: `/pwpwarfare cleartenkill <игрок>`.

---

## Техника

- Предметы-маркеры: `VehicleMarkerItem` с полями team/type/penalty/maxMats.
- Типы: APC, Tank, Helicopter, Combat Vehicle, Infantry Vehicle, Boat, Motorcycle, CAS Helicopter, CAS Fighter, Static/Mobile ZU — для каждой команды.
- Спавнер: `VehicleSpawnerBlockEntity` — читает маркер, хранит таймер возрождения.
- Техника в мире отслеживается через `markedVehicles` (UUID, team, тип, penalty).
- При уничтожении (wreck) — снятие тикетов с команды и удаление из markedVehicles.
- Настройки: `preventVehicleInventoryAccess`, `requireSpecialistToDrive`, `preventEnemyVehicleEntry`.

---

## Постройка

- **Hub (FOB)**: `HubBlock` + `HubBlockEntity`. Радиус стройки `hubBuildRadius`. Требует crate рядом (`hubPlacementRequiresCrate`). Респавн стоит `hubSpawnMaterialCost` материалов. Пополнениеkit стоит `hubResupplyCost`.
- **Rally Point**: `RallyPointBlock`, таймер жизни, дым.
- **Wall**: `WallBlock` + `WallBlockEntity`.
- **Barbed Wire**: `BarbedWireBlock` + `BarbedWireBlockEntity`.
- **M2 Browning**: `M2ConstructionBlock`, `M2AmmoStackBlock`. Статичный пулемёт.
- **AGS-30**: `AGSConstructionBlock`, `AGSAmmoStackBlock`. Гранатомёт.
- **TOW**: `TOWConstructionBlock`, `TOWMissileStackBlock`. ПТУР.
- **Mortar**: `MortarConstructionBlock`, `MortarShellStackBlock`. Миномёт.
- **Ghost Block**: призрачный блок для стройки.
- **Main Supply**: `MainSupplyBlock` — регенерация в радиусе `mainSupplyHealRadius`.

Все стройки через радиальные меню (`DefenseRadialScreen`, `HubRadialScreen`, `StaticGunRadialScreen`).

Ограничения: `minHubDistance`, `minRallyPointDistance`, `maxHubsPerTeam`, `hubBlockEnemyCount`, `rallyBlockEnemyCount`.

---

## Защита главной базы

Настраивается через `/pwpwarfare mainzone <team> <cube|cylinder> <pos1> <pos2>`.

- **До игры:** игрок за пределами зоны — телепорт на спавн.
- **Во время игры:**
  - Союзники — свободно.
  - Враг: предупреждение, 10 сек на выход, затем kill().
  - Техника врага: то же самое.
  - Незаклеймённая техника, пули, гранаты — discard().
  - Таймер не сбрасывается при быстром выходе — 10 сек вне зоны для сброса.

---

## Артиллерия

- `ArtStrikeRequest`: target, count, radius.
- `ActiveStrike`: позиция, таймер, звук, частицы.
- Кулдаун: `artStrikeCooldownMinutes` (default 30). Радиус: `artStrikeRadius` (default 20).

---

## Карта

- Тактическая карта (`WarfareMapRenderer`): текстура, игроки, точки, маркеры, HUB, Rally.
- Своя команда — синие/красные иконки, враги — серые. Только свой отряд — маркеры.
- Маркеры: Move, Attack, Defend, Build.
- Размер/центр/изображение карты настраиваются командами `map setimage/setcenter/setsize`.

---

## Конфиг

`<мир>/serverconfig/pwpwarfare-server.toml`

| Опция | Тип | Дефолт | Описание |
|---|---|---|---|
| `preventBlockBreaking` | bool | true | Запрет ломания блоков |
| `allowBreakingDefenses` | bool | true | Ломать стены/колючку |
| `preventAllItemDrops` | bool | true | Запрет выброса предметов |
| `preventVehicleInventoryAccess` | bool | true | Блок инвентаря техники |
| `preventEnemyVehicleEntry` | bool | true | Запрет входа во вражескую технику |
| `requireSpecialistToDrive` | bool | true | Только пилоты/механики за рулём |
| `requireOfficerForSL` | bool | true | Лидеру нужен офицерский набор |
| `enableKnockout` | bool | true | Нокаут |
| `reviveItem` | string | `pwp_medicine:medkit` | Предмет для поднятия |
| `reviveCooldownSeconds` | int | 120 | Кулдаун поднятия |
| `maxDownedTimeSeconds` | int | 180 | Время до смерти в нокауте |
| `autoGiveSlRadio` | bool | false | Автовыдача рации лидеру |
| `hubPlacementRequiresCrate` | bool | true | FOB требует ящик |
| `hubSpawnCostsMaterials` | bool | true | Респавн на FOB тратит материалы |
| `agsProjectileDestruction` | bool | true | Взрыв гранат АГС |
| `ammoStackDestruction` | bool | true | Взрыв амуниции |
| `lowTicketsSiren` | bool | true | Сирена при 50 тикетах |
| `diggingSpeedMultiplier` | double | 1.0 | Скорость копания |
| `hubResupplyCost` | int | 10 | Цена пополнения на FOB |
| `hubSpawnMaterialCost` | int | 10 | Цена респавна на FOB |
| `supplyTruckCrates` | int | 2 | Ящиков в грузовике |
| `supplyCrateMaterials` | int | 50 | Материалов в ящике |
| `mainSupplyHealing` | bool | true | Реген на главной базе |
| `mainSupplyHealRadius` | int | 5 | Радиус регена |
| `artStrikeCooldownMinutes` | int | 30 | Кулдаун арты |
| `artStrikeRadius` | int | 20 | Радиус арты |
| `minHubDistance` | int | 150 | Мин. расстояние между FOB |
| `minRallyPointDistance` | int | 150 | Мин. расстояние между Rally |
| `maxHubsPerTeam` | int | 8 | Макс FOB на команду |
| `hubBlockRadius` | int | 40 | Радиус блокировки FOB |
| `rallyBlockRadius` | int | 40 | Радиус блокировки Rally |
| `hubBuildRadius` | int | 50 | Радиус стройки вокруг FOB |
| `crateBuildRadius` | int | 50 | Радиус стройки вокруг ящика |
| `hubBlockEnemyCount` | int | 3 | Врагов для блокировки FOB |
| `rallyBlockEnemyCount` | int | 1 | Врагов для блокировки Rally |
| `hubSoundRadius` | int | 15 | Радиус звука FOB |
| `blueTeamCustomName` | string | `BLUEFOR` | Название синих |
| `redTeamCustomName` | string | `REDFOR` | Название красных |
| `voteAutoStartTime` | int | 10 | Автостарт через N мин |
| `voteRequiredPercentage` | int | 100 | % готовых для старта |

Клиент: `compassScale` (1-3, default 2) — размер компаса.
