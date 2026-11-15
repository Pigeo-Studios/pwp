# PWP — инвентаризация и план переписывания

Цель: переписать проект грамотно, ничего не потеряв из механик, UI и моделей.
Правило всех этапов: **старый код удаляется только после того, как новый покрывает все пункты
чек-листа этой фичи** (раздел «Инвентаризация»). Отмечаем галочками по мере переноса.

---

## 0. Текущее состояние (снимок 15.11.2026)

| Модуль | Файлов | Строк | Оценка |
|---|---|---|---|
| pwp-warfare | 248 | ~40 000 | монолит, ~половина файлов — декомпилят (`var23`, 3 пробела) |
| pwp-core-client | 56 | 6 857 | UI лобби + серверные пакеты (!) + дубль API-клиента |
| pwp-lobby | 8 | 2 108 | `LobbyMod` 779 строк |
| pwp-core-server | 7 | 976 | HTTP-клиент, баны |
| pwp-medicine | 17 | 1 097 | норм, мелкие правки |
| pwp-cosmetics / limits / drone / blast / movement | 5–7 | 178–697 | норм |
| Тесты | 0 | 0 | |

Системные проблемы:
- 375 обращений к `persistentData`, 64 строковых ключа `WARFARE_*` — нетипизированное состояние игрока.
- `WarfareWorldData` (1626 строк) — всё состояние матча в одном `SavedData`.
- `GameLogicEvents` (2509), `WarfareOverlay` (1568), `SquadMapRenderer` (1460), `ModCommands` (1168).
- `PacketBuildRequest`: постройки — цепочка `if (structureId == 10..25)`, у каждой своя копипаста.
- 50 пустых `catch`, 175 захардкоженных цветов в клиенте мимо `PWPTheme`.
- Синхронный HTTP на главном потоке сервера; выдача любого скина клиентским пакетом; права через `isCreative()`; глобальный trust-all SSL.

---

## 1. Инвентаризация (что обязано пережить переписывание)

### 1.1 Матч / режимы
- [ ] Режимы AAS (точки по порядку) и Invasion (асимметрия, `invasiondefend`)
- [ ] Тикеты команд, death tickets, таймер смерти, пауза, `gamestart`/`votestart`/`ready`
- [ ] Точки захвата (`addpoint/removepoint/pointcapture/pointclear`), «lattice»-линии на карте
- [ ] Мейн-зоны (`mainzone/removemainzone`) + предохранитель стрельбы (`MainZoneFireGuard`, 5 миксинов + страховка снарядов)
- [ ] Победа (`executeVictory`, `VictoryScreen`), статистика матча (`MatchStatsTracker` → core-service), возврат в лобби
- [ ] Арт-удар командира (`ActiveStrike`, `ArtStrikeRequest`, `PacketConfirmArtStrike`), вызов CMD (`makecmd`, `PacketCMDVote/RequestCMD/ForceCMD`)
- [ ] Heartbeat матча, `game_start_trigger`, `MatchConfigLoader` (match_config.json)

### 1.2 Отряды / роли / киты
- [ ] Отряды: создание/вход/лидер/распад/чат (`PacketSquadAction`, `PacketSquadChat`, `SquadSelectionScreen`, `SquadContextMenu`)
- [ ] Киты из API (19 ролей, 6 фракций, `__ALT__PRIMARY` слоты 41–42, категории, лимит 3 FIRE_SUPPORT на отряд, лимиты ролей)
- [ ] `KitUtil.getEffectiveKit` (Pending ?: Current), `syncMyKit`, выбор/применение кита, пополнение
- [ ] Редакторы китов: `KitEditorScreen/Menu`, `FactionKitListScreen`, `PacketSaveFactionKit`, `PacketPasteKit/Team`, `kit_setup`/`faction_kit_setup`
- [ ] Скины китов (`KitSkinSelectScreen`, `slotSkins`). Косметика (этап 1): экипировка `PacketCosmeticEquip` → `CosmeticService` (владение, сервер собирает предмет из `modelPath`), загрузка при входе `CosmeticService.loadForPlayer`, ответ `PacketCosmeticEquipResult` с профилем; один скин на слот (`setEquipmentSingle`), как ключ `(uuid, slot_type, role)` в БД

### 1.3 Деплой / респавн
- [ ] `DeployScreen` (TopBar, RoleGrid, LoadoutPanel, LoadoutEditor, SpawnPanel, PortraitRenderer, TaczHelper)
- [ ] Спавн на мейне / ралли / хабе, `PacketRespawnRequest`, `WarfareDeathScreen`
- [ ] Нок/гивап: `DownedHandler`, `DownedScreen`, `PacketDownedAction`, медик-индикаторы (+ pwp-medicine: бинты, аптечки, кровотечение)

### 1.4 Логистика: материалы и патроны
- [ ] Хаб (`hub_block`): материалы 200 старт / 3000 макс, синк, звук, иконка на карте, `HubRadialScreen`
- [ ] Main supply, supply truck (`SupplyTruckMarkerItem`, `PacketVehicleStatus`), `SupplyCrateEntity`, `PacketDropCrate`, `CrateRadialScreen`
- [ ] Патроны: `ammo_bag`, `ResupplyHandler`, `PacketRequestAmmo/CrateAmmo/VehicleAmmo`, Shift+ПКМ подсказки
- [ ] Стаки боекомплекта: `ags_ammo_stack`, `m2_ammo_stack`, `mortar_shell_stack`, `tow_missile_stack`, `M2AmmoItem`, `AGSAmmoItem`

### 1.5 Постройки (сейчас structureId 10–25)
- [ ] Стена, полу-стена (`wall_block`, `wall_slab_block`), колючка (`barbed_wire`), маскировочная сеть (`camo_net`), комбинированные (id 15–18)
- [ ] Ралли (`blue_rally/red_rally`, `RallyItem`, анимация/модель `ModelRallyPoint`)
- [ ] Хаб, станция техники (`vehicle_station`)
- [ ] Эмплейсменты: M2 (`M2BrowningEntity`), AGS-30 (`AGS30Entity`, `AGSScopeOverlay`), миномёт, TOW, РЭБ / мини-РЭБ (UFPV)
- [ ] Макеты: `GhostBlock`, зелёный/красный `*_invalid`, `ClientPlacementHandler`, копание лопатой (`EntrenchingToolItem`), коллизии, лимиты на команду
- [ ] Радиалки: `DefenseRadialScreen`, `WallRadialScreen`, `StaticGunRadialScreen`, `HubRadialScreen`, `CrateRadialScreen`, `RadioRadialScreen`, `SquadMarkerRadialScreen`, `TacticalMapRadialScreen`

### 1.6 Техника
- [ ] `vehicle_spawner` + `VehicleSpawnerScreen/Menu`, пулы фракций из БД (`FactionVehicleData`, `maps/vehicles`), `vehicleSpawners.{BLUE,RED}` в map_config
- [ ] Маркеры техники (blue/red apc, tank, heli, boat, moto, cas, zu, spg, light_supply)
- [ ] Посадка по киту (`PacketVehicleDriveRequest/Answer`, `vrequest/vanswer`), выброс, `PacketVehicleShoot`
- [ ] Редакторы: `FactionVehicleEditor/List/SelectScreen`, `PacketSaveFactionVehicle`

### 1.7 Дроны
- [ ] Канальный деплой FPV/Mavic, кулдаун в мировом времени, лимиты, отмена по движению, зомби-дроны, авто-деспаун, чанк-страховка
- [ ] Подсумок перезарядки мавика, `DroneDeployOverlay`, `DroneConnectMixin`
- [ ] pwp-drone шейдеры FPV, sbwchunkload `DroneTracker`, sbw_mavic_extender

### 1.8 Тактическая карта и метки
- [ ] `SquadMapScreen` + `SquadMapRenderer`: текстура карты (`textures/gui/maps/<map>.png`, `setimage/setcenter/setsize`), сетка/кейпады, зум/пан
- [ ] Слои: мейны, точки, lattice, арт-зоны, постройки, техника, игроки (иконки ролей/медик/свой), метки командира/отряда (move/attack/defend/build), ромбы отряда, пинги, пути
- [ ] `MarkerManager`, `PathManager`, `MapMarker`, `SquadMarker`, `MarkerClientCache`, `PathCache`, `MapMarkerGridScreen`, `PacketPlace*/Remove*/Sync*`
- [ ] Бинокль (`BinocularsItem`), радио лидера (`squad_leader_radio`), пинг клавишей

### 1.9 HUD / UI
- [ ] `WarfareOverlay` (тикеты, точки, компас, ники, отряд), `NotificationFeed`, `FeedbackMessageBlock` (+ `GuiOverlayMessageMixin`), скрытие SBW HUD в зоне
- [ ] Лобби (pwp-core-client): MainMenu, Lobby, Selection, Loading, Stats, вкладки Play/Leader/Vote, `LobbyStatePacket`
- [ ] Выбор команды/фракции (`TeamSelectionScreen`, `FactionSelectScreen`, `KitTeamSelectScreen`), `WarfareConfigScreen`
- [ ] Голос: `WarfareVoicechatPlugin`, squad/command PTT, индикаторы голоса, voicemute (этап 1: проверка мута — неблокирующий кэш 5 с с фоновым обновлением; команды мута — асинхронно)
- [ ] Клавиши: drop_supply, open_squad_menu, show_map, show_nicknames, place_ping, squad_ptt, command_ptt

### 1.10 Модели / анимации / ассеты
- [ ] GeckoLib: M2, AGS-30, бинокль, лопата, радио, ралли (7 geo, 5 animations), рендереры + `ClientItemExtensions`
- [ ] 106 моделей блоков/предметов, 528 текстур (иконки карты, флаги, киты, скины 8 фракций), 36 звуков (голоса фракций), 3 шейдера
- [ ] Рендер персонажа: `PlayerModelMixin` (setModelProperties), `FirstPersonArmFixMixin`, `RecoilHandler`, `MouseSensitivityMixin`, `ClientSkinManager`
- [ ] Lang en/ru/uk

---

## 2. Чего не хватает (новое, в план)

- Полный набор построек как в Squad (данные-ориентированный реестр, ниже)
- Контекстные окна (радиальное меню по ПКМ/клавише на объекте: хаб, ящик, техника, игрок, постройка)
- Материалы и патроны как отдельные ресурсы хаба/ящика/грузовика (сейчас только материалы у хаба)
- Хабы/FOB и их радиус на карте, видимость построек на карте
- Нормальная карта (без костыля), нормальные метки
- Нормальный рендер персонажа и анимации
- «Два типа» — уточнить у автора (что именно)

---

## 3. Этапы (по порядку, каждый — рабочий билд)

### Этап 1. Фундамент и безопасность — ✅ сделан (ветка `rewrite/stage-1`)
- [x] Сборка в облаке/CI: `.github/workflows/build.yml` (windows-latest, JDK 17, прод-жарники sparse-checkout из Pwpfiles, секрет `PWPFILES_TOKEN`). Локально `gradlew build -x test` зелёный.
- [x] Spotless (palantir-java-format) + переименование `varN` — отдельный коммит; `spotlessCheck` входит в `build`.
- [x] Пустые `catch` → лог (104 шт., моды + core-service; в покадровом коде — `LogOnce`); разбор ввода — `Parse.*` без исключений; `System.err/out` → SLF4J.
- [x] Безопасность: владение скином проверяет сервер (`CosmeticService`), предмет скина собирает сервер (`SkinCatalog`); права редакторов/отладки — `PwpPermissions.isAdmin` (op 2+); глобальный trust-all убран.
- [x] Один асинхронный HTTP-клиент `CoreApi` (`java.net.http` + `CompletableFuture` → `ServerThread.thenOnServer`). Удалены `CoreServerApi`, `CoreAPI`, `PacketApiAction`, `PacketSyncCosmeticEquip`, мёртвый `PlayerPermissions`.
- [x] Модуль `pwp-api` (CoreApi, Heartbeat, MatchPolicy, ServerThread, PwpPermissions, Parse, LogOnce, Urls); core-client не зависит от core-server.
- [x] Экранирование URL (`Urls.enc` для пути и query — `getLeaderboard orderBy` и др.).
- [x] Исправлены «кракозябры» (UTF-8, прочитанный как cp1251) в 35 файлах — видимые игроку строки.
- [x] Канал `pwpwarfare` объявлялся с литералом `"1"` мимо `PROTOCOL_VERSION` — исправлено, версия 2. `pwp_cosmetics` — 2.
- [x] Юнит-тесты pwp-api (8 шт.).

**Изменения поведения этапа 1 (проверить в игре):**
- Обычный игрок не может надеть некупленный скин (раньше меню выдавало любой скин бесплатно). Админ (op 2+) — может: сервер выдаёт предмет с источником `admin`.
- Экраны скинов/техники (`SkinInventoryScreen`, `SkinEditor/SkinList`, `FactionVehicleList`) раньше ходили в core-service напрямую с клиента на `localhost:8080` — у удалённых игроков не работали. Теперь всё через сервер.
- Редакторы китов/техники/скинов, спавнер техники, отладочные пакеты, вставка китов — только op 2+ (креатив без op больше не даёт доступа).
- Киты/техника фракций грузятся асинхронно после применения map_config (в фазе ожидания 300 с), затем рассылается синхронизация.
- Повторный вход больше не дублирует экипировку косметики (`CosmeticManager` очищается перед загрузкой).

**Найдено на этапе 1, перенесено в следующие этапы:**
- `MatchStatsTracker.scheduleServerShutdown`: `Thread.sleep(2000)` на главном потоке перед `server.halt` и статистика без ретрая — блок «Цикл сессий».
- `PacketDataResponse`/`PacketSendData` пишут JSON в `writeUtf` (лимит 32767): большой каталог скинов/техники может не влезть — этап 2 (сетевой слой v2, DTO вместо сырого JSON).
- Две параллельные системы запросов данных GUI: `coreclient.PacketDataRequest/Response` и `warfare.PacketRequestData/PacketSendData` (+ `ClientResponseCache` и `ClientData`) — свести в одну на этапе 2.
- Рефлексия core-client → pwp-lobby (`Class.forName` в Vote*/JoinMatch*-пакетах) — заменить на интерфейс в pwp-api (этап 2 / цикл сессий).
- В core-service часть пустых catch была ожидаемым фолбэком (version.txt, git describe) — теперь пишут warn при старте; при переписывании сервиса заменить на явные проверки.

### Этап 2. Ядро данных — ✅ сделан (ветка `rewrite/stage-2` от `rewrite/stage-1`)
- [x] Capability `PlayerWarfareData` (`pwpwarfare:player_data`) вместо ключей `WARFARE_*` в persistentData игрока. Единая точка — `WarfareData.of(entity)`: игрок → capability, прочие сущности (техника, дроны) → persistentData как раньше. Реестр всех 61 ключа с типом и значением по умолчанию — `data/state/WarfareKeys` (+ динамический `driveRequestCooldown(id)`); строковые `"WARFARE_*"` вне него запрещены. Имена ключей в NBT прежние.
- [x] Миграция: при первом обращении к данным игрока и при входе (`PlayerLoggedInEvent`, HIGHEST) все `WARFARE_*` из persistentData КОПИРУЮТСЯ в capability один раз (маркер `pwp_migrated_from_persistent`, значение в capability не перетирается). Старые ключи остаются в persistentData ещё одну версию — для отката на старый мод; в следующей версии — `WarfareMigration.removeLegacy`.
- [x] Клон (`PlayerCloneRules`): смерть — отряд, лидер, киты, кулдауны ресапплая/респавна (как раньше; по байткоду Forge `restoreFrom` копирует только `PlayerPersisted`) + маркер миграции; возврат из Энда — всё, кроме каналов дронов и `InMainZone` (раньше терялись кулдаун дрона, тимкиллы, ограничение базы — исправлено). Нок сбрасывается в обоих случаях.
- [x] `WarfareWorldData` → `MatchState`, `SquadRegistry`, `StructureRegistry`, `MarkerRegistry`, `VehicleRegistry` (`world/`). Каждый реестр владеет полями и сам пишет/читает свои ключи в КОРНЕ тега — формат прежний, старые миры грузятся без миграции (включая `MainSupplies` → `VehicleStations`). Обращения `data.X` → `data.<реестр>.X`. Киты фракций (`blueKits/redKits`) пока в `WarfareWorldData` — переедут в `KitService` (этап 3).
- [x] `GameClock` (pwp-api) — единственный источник времени на сервере: `serverTicks` (сбрасывается при рестарте — только для состояния в памяти) и `worldTicks` (level.dat — для всего сохраняемого). Все серверные `getTickCount()/getGameTime()` в pwp-warfare переведены без смены часов (кулдаун дронов — мировое время, канал деплоя — серверный тик). `Cooldown` — чистая логика «до» (перезапись, а не прибавление).
- [x] Сеть v2 (`pwp-api/net`): `Net.C2S/S2C` (record-DTO, `validate()` до логики, обработка только живого отправителя, исключения логируются), `Net.Registrar` (новые — в конец), `Validate`, `Payloads` (строки до 1 МБ вместо `writeUtf` 32767). Протоколы: `pwpwarfare:main`=4 (после аудита), `pwp_core_client:network`=3.
  - Отряды: `PacketSquadAction` (12 действий по номеру) → `squad/SquadPackets.*` (по пакету на действие) + `squad/SquadActions` (метод на действие, логика без изменений).
  - Админ-действия: `PacketAction(action, json)` → `AdminAction.SaveSkin/DeleteSkin/DeleteFactionVehicle` (типизированные поля, op 2+ на сервере).
- [x] Перенесено из этапа 1: две системы запросов данных → одна (`core-client network/data`: `DataRequest`/`DataResponse` + серверный реестр `DataRequests`, warfare регистрирует `factionKits`, клиентский `ClientDataResponses` раскладывает ответы в кэши core-client и подписчиков warfare); лимит 32767 снят (`Payloads`); рефлексия core-client → lobby заменена интерфейсом `pwp-api/lobby/LobbyActions` (keep-правила сняты); мёртвые `VoteMap/VoteMode/VoteFaction/JoinMatchPacket` удалены (GUI голосует командами).
- [x] Чистые правила + тесты: `match/CaptureLogic` (захват/нейтрализация за тик, тикеты AAS/Invasion в прежнем порядке побочных эффектов), `match/KitLimits` (лидерская роль, выключенная роль, команда/отряд/мин. отряд/3 FIRE_SUPPORT, подсчёт занятых ролей). Единственный путь проверки — `network/KitAccess` → `KitLimits.unavailableReason` для меню деплоя, выбора кита (`PacketSelectKit`) и выдачи кита при спавне (PendingKit и CurrentKit). До аудита этапа 2 так было только в меню: спавн держал ручную копию проверок (считал лишь CurrentKit других, лидерство — без флага `IsSquadLeader`), ветка CurrentKit лимиты не проверяла вовсе.
- [x] Модуль `pwp-logic` (чистый `java-library` без ForgeGradle): чистые классы и их тесты, `gradlew :pwp-logic:test` ~3 с вместо ~10 мин. Исходники подключены `srcDir` в pwp-api/pwp-warfare/pwp-core-client.
- [x] `mods.toml`: warfare объявляет pwp_core_client и pwp_cosmetics, lobby — pwp_core_client и pwp_cosmetics, medicine — pwpwarfare (раньше без них — `NoClassDefFoundError` при неполном комплекте модов).
- [x] Отложенный деплой: откладывается только запрос, уже прошедший проверку состояния (`SpawnRules`: мёртв/наблюдатель/нок). Снимок (команда, режим наблюдателя, кит); при загрузке китов игрок ищется по UUID, снимок сверяется (`DeferredDeploy`), затем `PacketRespawnRequest.process` — точка спавна проверяется заново, кит выдаётся через `KitAccess` (лимиты, лидерская роль, FIRE_SUPPORT; занято — Unassigned). Ожидание отменяется при выходе, смене кита и смене команды; при перезапуске матча (`FactionDataLoader.start`) ждущие получают «Матч перезапущен — нажмите «Деплой» снова», при остановке сервера состояние загрузчика очищается.
- [x] Чек-лист ручной проверки этапов 1–2 — `docs/TEST_CHECKLIST.md`.
- [x] Юнит-тесты (до выноса в pwp-logic): pwp-api 20 (Parse, CoreApi data, RequestGuard, Cooldown, Validate/Payloads), pwp-warfare 22 (миграция и формат ключей, формат реестров, захват, лимиты китов), pwp-core-client — UI-кит (анимации, геометрия радиалки/скролла/адаптива).
- [x] UI-кит 6.1 (`pwp-core-client/ui`): `UIWidget`, `UILayer` (порядок событий, контекстное меню/модалка/тултип поверх), `UIPanel`, `UIButton` (стили PWPButton), `UIIconButton`, `UITabs`, `UIScrollList`, `UITooltip`, `UIRadialMenu` (одно меню + `UIAction` от провайдера), `UIContextMenu`, `UIModal`, `UIProgressBar`, `UIToast`; `UIAnim` (easing, реальное время), `UIMath` (чистая геометрия), `UIDraw`. Цвета — только `PWPTheme` (добавлены `Styles.Radial`, `Styles.Modal`). Существующие экраны не переводились.

**Аудит ветки `rewrite/stage-2` (исправления, по коммиту на пункт):**
- `PacketRespawnRequest` → сеть v2: `validate()` (тип `MAIN|RALLY|HUB:x:y:z`, ≤ 64 символов, координаты в границах мира — `SpawnRules.parse`); спавн только мёртвому/наблюдателю, нок — сдача, наблюдатель-тимкиллер — отказ (`SpawnRules.denyReason`); живой игрок больше не телепортируется к рали/ФОБ и не получает кит повторно; антиспам 1 запрос/с. `pwpwarfare:main`=4.
- `PacketSelectKit` → сеть v2: `validate()` (`kitName` — `Validate.label` ≤ 64: имена китов содержат пробел/скобки, «Drone Operator», поэтому не `Validate.id`; `slotSelections` ≤ 16, ключи `Validate.id` ≤ 32, значения [0, 64]); сервер проверяет кит у команды игрока и его доступность до записи PendingKit.
- Лимиты китов — один путь (`KitAccess` → `KitLimits`), см. выше.
- `FactionDataLoader`: очистка на `ServerStoppedEvent` (одиночная игра — повторное открытие мира в том же JVM).
- `PlayerCloneRules`: возврат из Энда НЕ переносит каналы дронов (`DroneDeploy*`/`DroneReload*`, кроме `DroneDeployCd`) и `InMainZone`.
- Откат: при смерти ключи `SURVIVE_DEATH` дублируются в persistentData нового игрока (`PlayerCloneRules.mirrorToLegacy`, пока `WarfareMigration.LEGACY_KEYS_KEPT`) — удалить вместе с `removeLegacy`.
- Логи отказов `Net.handleC2S`/`DataRequests` — `RateLimiter` (раз в 10 с на игрока, без стека для клиентского ввода); `page` лидерборда [1, 1000]; `LobbyActions.joinActiveMatch` удалён; текст кика в `SquadActions.kick`.

**Доработки этапа 1, сделанные в начале этапа 2 (ветка `rewrite/stage-1`):**
- Колбэки HTTP: `ServerThread.thenForPlayer` — игрок берётся заново по UUID, офлайн — колбэк не вызывается (экип/снятие скина, загрузка косметики, запросы данных, админ-действия, редактор/сохранение китов и техники, мут голоса).
- Антиспам `RequestGuard`: кулдаун + один запрос в полёте на игрока/тип.
- `FactionDataLoader`: киты/техника фракций грузятся с повтором раз в 10 с; деплой до загрузки китов откладывается («Загрузка китов…») и выполняется автоматически.
- Ключ API читается только на выделенном сервере (раньше статик-поле `CoreServerMod` читало `PWP_API_KEY`/`pwpcore.txt` и на клиенте). ProGuard: keep полей `MapConfig` (лобби) и `MatchConfig` (матч) — Gson по именам полей не был защищён.

**Найдено на этапе 2, перенесено дальше:**
- Остальные многодейственные пакеты (`PacketDownedAction`, `PacketRadioAction`, `PacketVoteAction`, строковые типы маркеров) и все прочие пакеты pwpwarfare (~60) ещё на `registerMessage` со старыми классами — переводить на `Net.C2S/S2C` вместе с переносом их фич (этапы 3–5).
- Два клиентских кэша одних и тех же данных (`coreclient.ClientResponseCache` и `warfare.ClientData`) — транспорт уже один; кэши свести при переводе экранов на UI-кит (этап 6).
- `GameLogicEvents`: рефлексия `getMethod("getHealth"/"getLastDamageSource")` на сущностях техники SBW — заменить адаптером `compat/` (этап 7).
- Клиентский код ещё читает `level.getGameTime()` напрямую для анимаций (HUD/оверлеи) — допустимо (клиентское время отрисовки), правило GameClock — для серверной логики.
- `WarfareWorldData` всё ещё держит киты фракций и методы `loadKitsFromApi`/`parseConfigVehicleSpawners` — этап 3 (`KitService`, `VehicleService`).

**Изменения поведения этапа 2 (проверить в игре):**
- Деплой до загрузки китов не теряется: сообщение «Загрузка китов…», спавн выполнится сам.
- Живой игрок на поле не может «задеплоиться» (меню по K): «Вы уже на поле — деплой доступен только после смерти». Выбор кита живым (резерв на следующий спавн) работает, но занятый/чужой/лидерский кит отклоняется сразу.
- Занятая к спавну роль (в т.ч. CurrentKit после смерти) → спавн с Unassigned и причиной в чате.
- Повторные быстрые клики (экип скина, сохранение кита/техники, запросы данных) игнорируются/показывают «Подождите…».
- Ответы каталогов (скины/техника/киты) больше не упираются в 32767 символов.
- Старые клиенты не подключатся к новому серверу (версии каналов подняты) — обновление через лаунчер.

### Этап 3. Матч, отряды, киты, деплой
- `GameLogicEvents` → `match/` (тикеты, захват, AAS/Invasion, победа), `squad/`, `kit/`, `deploy/`.
- `KitService`: один источник (API), один путь применения, кеш, ALT-слоты — без NBT-костылей.
- Деплой/респавн/нок через сервисы; `DeployScreen` на новых DTO.

### Этап 4. Логистика и постройки
- `StructureDef` (данные: стоимость, время копания, футпринт, модель, лимиты, кто может ставить) + единый `BuildService` вместо `if structureId==N`.
- Ресурсы хаба/ящика/грузовика: материалы + патроны; радиус FOB; разбор.
- Перенос всех существующих построек, затем — недостающие.
- Каркас контекстных окон (одно радиальное меню, разные провайдеры действий) — заменяет 8 радиалок.

### Этап 5. Тактическая карта и метки
- Карта: генерация текстуры из мира (серверная команда рендерит топ-даун в PNG/тайлы, клиент получает) вместо ручной картинки; сетка Squad (A1-K9 + кейпады).
- Единый `MarkerService` (метки командира/отряда, пинги, пути, вражеские споты) вместо 4 параллельных систем.
- Слои карты как отдельные классы; хабы/FOB-радиусы/постройки/техника.

### Этап 6. UI, HUD, рендер, анимации
- UI-кит на `PWPTheme` (кнопки, панели, списки, радиалка, тултипы) — все экраны на нём, 0 магических цветов.
- `WarfareOverlay` → набор независимых HUD-элементов.
- Рендер персонажа (позы, руки от 1-го лица, портреты) — переписать миксины чисто; анимации GeckoLib.
- Лобби-экраны на новом UI-ките.

### Этап 7. Техника, дроны, интеграции
- `compat/` — адаптеры SBW/TACZ/pointblank/UFPV/voicechat за интерфейсами; миксины только там.
- Дроны и техника на новых сервисах; удаление «страховок на страховки» после тестов.
- Обновить AGENTS.md (вычистить историю инцидентов, оставить правила).
