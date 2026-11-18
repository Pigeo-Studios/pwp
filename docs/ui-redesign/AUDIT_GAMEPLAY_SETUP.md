# Аудит: ручная подготовка карты и матча, команды, конфиги, онбординг

Дата: 17.11.2026. Ветка `rewrite/ui-redesign`. Режим: только чтение кода, игра не запускалась.
Обозначения: **[факт]** — видно в коде по указанной строке; **[вывод]** — моё заключение, в игре не проверено.

## 0. Ключевые цифры

| Что | Сколько |
|---|---|
| Серверных корневых команд | 12 (`/pwpwarfare`, `/pwp`, `/ready`, `/vrequest`, `/vanswer`, `/votemap`, `/votemode`, `/votefaction`, `/pwpban`, `/pwpunban`, `/sbwchunkload`; `/pwp` регистрируют 2 мода) |
| Клиентских корневых команд | 4 (`/stats`, `/pwp stats`, `/pwpui`, `/compass`) |
| Листовых сабкоманд `/pwpwarfare` | 27 (у `addpoint` — 13 позиционных аргументов) |
| Сабкоманд `/pwp server` (лобби) | 9 (start ×4 формы, stop, stopvote, autostart on/off, maxmatches, status) |
| Ключей ForgeConfigSpec | 97 (warfare COMMON 53 + CLIENT 1, sbwchunkload 32, blast 6, medicine 3, limits 2) |
| Полей верхнего уровня `map_config.json` | 18 (+ 9 точек × 12 полей, 24 слота техники × 5 полей) |
| Мёртвых полей `map_config.json` | 9 (см. §2.2) |
| KeyMapping (переназначаемых) | 7; захардкоженных клавиш — 8 (M, F7, F8, F9, F10, PgUp, PgDn, F9-creative) |
| Админ-GUI | 6 (редакторы китов ×2, техники фракций, скинов, спавнера техники, конфиг warfare); редактора карты/точек/мейнов НЕТ |

## 1. Команды

### 1.1 `/pwpwarfare` (pwp-warfare, `command/ModCommands.java`, права op 2 — `:59-79`, `:902`)

| Сабкоманда | Что делает | Автодоп. | Ошибки / замечания |
|---|---|---|---|
| `gamestart <bool>` | true — отсчёт (`startGameCountdown`), false — стоп матча (`:1025`) | нет (bool) | ок |
| `gamemode <mode>` | aas/invasion, в invasion все точки отдаёт защитнику (`:1047`) | да (aas/invasion) | **любая опечатка молча = aas** (`:1050`) |
| `invasiondefend <team>` | защитник Invasion (`:1070`) | да | не blue → молча RED |
| `deathtimer <sec>` | таймер респавна (`:1096`) | нет | ок |
| `deathtickets <n>` | цена смерти в тикетах (`:1149`) | нет | ок |
| `clearsquad <team>` | распускает отряды и КМД команды (`:1548`) | да | неверная команда — «успех», ничего не сделано |
| `teamtickets <team> <n>` | тикеты команды (`:1125`) | да | неверная команда — сообщение «успех» |
| `mainzone <team> <cube\|cylinder> <pos1> <pos2>` | защищённая мейн-зона, одна на команду (`:279`, `:1500`) | да | для cylinder `pos1` = ЦЕНТР, `pos2` = край; в `map_config` те же поля = углы бокса (`WarfareMod.java:171`). Клиентам не синхронизирует |
| `removemainzone <team>` | удалить мейн-зону (`:1531`) | да | **зарегистрирована дважды** (`:349`, `:379`); при неудаче всё равно `return 1` |
| `teamjoin <team> <player>` | перевести игрока в команду, ТП на мейн если матч идёт (`:1160`) | да | ок |
| `addpoint <shape> <pos1> <pos2> <name> <bluePrio> <redPrio> <timeMin> <penalty> <captureDeduct> <lockMin> <gainNeut> <gainCap>` | добавить точку захвата в SavedData (`:449`, `:1210`) | только shape | 13 аргументов без подсказок; нет проверки дубля имени и цепочки приоритетов; в `map_config` не пишет |
| `removepoint <name>` | удалить точку (`:1254`) | да (имена точек) | ок |
| `pointclear <name>` | точку в нейтраль (`:1273`) | да | ок |
| `pointcapture <team> <name>` | принудительный захват (`:1296`) | да | ок, единственная с проверкой команды |
| `teamspawn <blue\|red\|none> <pos>` | спавн мейна на измерение (`:1333`) | да | неверная команда — «успех» |
| `map setimage <img>` | PNG карты (`:711`) | да (файлы `maps/`) | ок |
| `map setcenter <x> <z>` | центр карты (`:762`) | нет | **не двигает world border**, в отличие от `mapBounds` в конфиге (`WarfareMod.java:154`) |
| `map setsize <blocks>` | размер карты (`:802`) | нет | то же — border не трогает |
| `fraction <team> <faction\|clear>` | фракция команды (`:826`, `:1109`) | да | **нет ни одного сообщения** (ни успеха, ни ошибки) |
| `cleartenkill <player>` | снять наказание за тимкилл (`:847`, `:1581`) | игрок | опечатка в имени («tenkill») |
| `votestart <bool>` | голосование «готовы» (F9/F10, `/ready`) (`:852`) | нет | единственный способ запустить это голосование |
| `voicemute <player> [min] [reason]`, `voicemute list`, `voiceunmute <player>` | мут голосового чата через core-service (`:873-899`, `:1368-1444`) | игрок | ок |
| `warn <player> <msg>` | титр-предупреждение (`:599`, `:1352`) | игрок | ок |
| `pause` | пауза матча (тикеты, захват, FF) (`:894`, `:1618`) | — | ок |
| `makecmd <squadId>` | назначить КМД (`:903`, `:1590`) | нет (id отряда не подсказываются) | ок |

Общие проблемы файла: 1660 строк, код после декомпиляции с отступом >300 символов (`:77`); сообщения вперемешку ru/en, литералы вместо `translatable`; ошибки через `sendSuccess` вместо `sendFailure`.

### 1.2 Прочие серверные команды

| Команда | Модуль, файл | Права | Что делает | Автодоп. | Замечания |
|---|---|---|---|---|---|
| `/pwp join` | lobby, `LobbyMod.java:469` | все | войти в активный матч | — | ок |
| `/pwp server start [map [mode [blue red]]]` | lobby, `LobbyMod.java:477-635` | op 2 | полный цикл голосований / форс карты / форс режима / мгновенный матч | map, mode | blue/red без подсказок и, похоже, без проверки фракции; фолбэк фракций захардкожен `usa/russia` (`:~575`) |
| `/pwp server stop / stopvote / status` | `LobbyMod.java:636-728` | op 2 | стоп матча / голосования / статус | — | ок |
| `/pwp server autostart on\|off`, `maxmatches <n>` | `LobbyMod.java:672-695` | op 2 | автостарт, лимит матчей | — | хранится только в памяти (`ServerConfig.java:5-6`) — после рестарта лобби сброс |
| `/votemap <name>`, `/votemode <mode>` | `LobbyMod.java:729`, `:756` | все | голос (дубль GUI) | да | ок |
| `/votefaction <blue> <red>` | `LobbyMod.java:776` | все | голос за пару фракций | **нет** | всегда «Голос отдан», ввод не проверяется |
| `/pwp inv` | warfare, `ModCommands.java:910` | все | открыть инвентарь скинов | — | ок |
| `/pwp respawn` | warfare, `ModCommands.java:916` | все | самоубийство/гивап с КД | — | ок |
| `/ready` | warfare, `ModCommands.java:958` | все | голос «готов» | — | дубль F9 |
| `/vrequest [id]`, `/vanswer <approve\|deny> <id> <uuid>` | warfare, `ModCommands.java:974`, `:994` | все | запрос/ответ на вождение техники (кликабельный чат `InteractionEvents.java:589`, `PacketVehicleDriveRequest.java:79`) | — | битый UUID → необработанное исключение (`:1004`) |
| `/pwpban <player> <dur> <reason>`, `/pwpunban <player>` | core-server, `BanCommand.java:15`, `UnbanCommand.java:14` | op 2 | бан через core-service | длительность | игрок — строка, без подсказок онлайн-ников; ошибки нормальные |
| `/sbwchunkload status\|config` | sbwchunkload, `ChunkLoadDebug.java:33` | op 2 | отладка тикетов | — | ок |

### 1.3 Клиентские команды

| Команда | Файл | Что делает | Замечания |
|---|---|---|---|
| `/stats`, `/pwp stats` | core-client, `ClientStatsCommand.java:18-23` | экран статистики | ок |
| `/pwpui [open <id>\|next\|list]` | core-client, `devtest/UiTestCommand.java:42` | тест-стенд UI | только dev/`-Dpwp.uitest` |
| `/compass scale <1-3>` | warfare, `command/ClientCommands.java:22` | размер компаса | дублирует то, что логично держать в `WarfareUiSettings` |

В pwp-medicine, pwp-cosmetics, pwp-limits, pwp-movement, pwp-drone команд нет.

## 2. Конфиги

### 2.1 Файлы и кто их читает

| Файл | Где лежит | Читает | Когда |
|---|---|---|---|
| `map_config.json` | `<мир>/`, `world/` или корень сервера | warfare `WarfareMod.applyMapConfig` (`WarfareMod.java:89-340`), ручной JsonObject | каждый старт сервера (`configApplied` не сохраняется в NBT — `MatchState.java:37`) |
| тот же `map_config.json` | `maps/<карта>/` | lobby `MapRegistry.java:40` → Gson-POJO `lobby/maps/MapConfig.java` | старт лобби; перед матчем `MatchAllocator.java:100-145` переписывает фракции/тикеты/режим/защитника/паттерн |
| `match_config.json` | корень матч-сервера (из `match_template/`) | warfare `MatchConfigLoader.java:16` (POJO `MatchConfig`) + lobby `ServerManager.java:135` (второй POJO `ServerMatchConfig`) | старт; лобби генерирует из него `superbwarfare-server.toml` |
| `config/pwpwarfare-common.toml` | Forge COMMON | `WarfareConfig.java` (53 ключа: геймплей, баланс, фракции, дроны, РЭБ) | старт; есть GUI `WarfareConfigScreen` |
| `config/pwpwarfare-client.toml` | Forge CLIENT | `compassScale` | |
| `<мир>/serverconfig/sbwchunkload-server.toml` | SERVER | 32 ключа | |
| `<мир>/serverconfig/pwpblast.toml` | SERVER | 6 ключей | |
| `<мир>/serverconfig/pwplimit-server.toml` | SERVER | 2 ключа (прыжок) | |
| `config/pwp_medicine-common.toml` | COMMON | 3 ключа | есть GUI `PWPConfigScreen` |
| `config/pwp_ui_client.json` | клиент | `UiSettingsFile` | |
| `config/pwpcore.txt` | сервер | API-ключ (`CoreServerMod.java:59`) | |
| `config/fpvdrone/fpvdrone.json` | сторонний | UFPV | |
| `maps/kits/*.json`, `maps/kits/import.sql` | репо | генератор `tools/generate_kits.py` → БД | вручную `mysql < import.sql` |
| `maps/vehicles/import.sql` | репо | `tools/generate_vehicle_pools.py` → БД | вручную |

### 2.2 Поля `map_config.json` (takmachka)

| Поле | Warfare читает | Lobby читает | Комментарий |
|---|---|---|---|
| `name`, `displayName`, `description`, `image`, `version`, `maxPlayers`, `minPlayers` | `image`, `name` (лог) | да | ок |
| `mode`, `modeDisplayName` | `mode` (`:297`) | да | лобби переписывает |
| `durationMinutes` | нет | POJO есть | [вывод] не используется как лимит матча |
| `availableFactions` | нет | да (`LobbyMod.java:909`) | нужно ровно 6 и строго 3+3 (`FactionVotingManager.java:55-57`) |
| `modes.invasion.{attackerTickets,defenderTickets}` | нет | да (`LobbyMod.java:942`) | |
| `modes.invasion.{captureBonus,setupPhaseSeconds}` | да (`:310-322`) | captureBonus | |
| `teams.{BLUE,RED}.{faction,tickets}` | да (`:124-135`) | да | |
| `teams.*.spawn` | да (`:211-233`) | да | = центр мейн-зоны (BLUE −1131/−812 — середина бокса мейна) |
| `settings.respawnTimer`, `settings.deathTicketCost` | да | да | |
| `settings.lockDurationMinutes` | **нет** | в POJO, не используется | мёртвое |
| `settings.hideDeathMessages/hideNametags/disableHunger/disableNaturalRegen` | **нет** | нет | **мёртвые**: эти правила берутся из `match_config.json` (`MatchConfigLoader.java:52-59`) |
| `mainZones[]` (`team, shape, pos1, pos2`) | да, очищает и перезаписывает (`:162-180`) | POJO | цилиндр = вписанный круг бокса (`WarfareWorldData.java:964-970`) |
| `capturePoints[]` (`name, shape, pos1, pos2, bluePriority, redPriority, captureTimeMinutes, ticketPenalty, captureDeduction, lockDurationMinutes`, опц. `gainNeutralizeTickets/gainCaptureTickets`) | **только если в SavedData точек нет** (`:203`) | POJO | у всех 9 точек `redPriority = 10 − bluePriority` |
| `capturePointPatterns[]` + `capturePointPatternIndex` | да (`:182-201`) | да | в takmachka не используется |
| `vehicleSpawners.{BLUE,RED}[]` (`role\|vehicleName, x,y,z, yaw`, опц. `fixed`) | да → ставит блок `vehicle_spawner` (`WarfareWorldData.java:193-280`) | нет | 12 слотов на команду, координаты руками |
| `mapBounds.{centerX,centerZ,sizeBlocks}` | да + world border (`:146-156`) | POJO | 2532 = ровно бокс от мейна до мейна |
| `invasionDefender` | да | пишет лобби | |
| `vehicles` (POJO лобби) | — | поле есть, в JSON нет | мёртвое |

### 2.3 Где одно и то же хранится в нескольких местах

| Данные | map_config | SavedData (`WarfareWorldData`) | Блоки в мире / BE | Команда | Кто побеждает |
|---|---|---|---|---|---|
| Точки захвата | `capturePoints` | `match.capturePoints` | — | `addpoint` | **SavedData**, если непусто; конфиг молча игнорируется (`WarfareMod.java:203`) |
| Мейн-зоны | `mainZones` | `structures.mainZones` | — | `mainzone` | **конфиг** (clear на каждом старте) — команда до рестарта |
| Спавн мейна | `teams.*.spawn` | `match.blue/redSpawns` | — | `teamspawn` | конфиг перезаписывает |
| Граница/центр карты | `mapBounds` | `match.mapCenter*/mapSizeBlocks` | world border в level.dat | `map setcenter/setsize` | конфиг; команда border не меняет |
| Фракции/тикеты/режим | `teams`, `mode` | `match.*` | — | `fraction`, `teamtickets`, `gamemode` | конфиг (его переписывает лобби) |
| Спавнеры техники | `vehicleSpawners` | `vehicles.configVehicleSpawners` | BE `VehicleSpawnerBlockEntity` (vehicleName, yaw, respawn/initial time) + GUI `VehicleSpawnerScreen` | — | конфиг перезаписывает BE на старте, кроме `fixed` |
| Триггеры старта (редстоун) | — | `structures.triggerBlocks` | блок `game_start_trigger` | — | только мир (ставит админ руками, `InteractionEvents.java:243`) |
| Main Supply | — | — | блок `main_supply` | — | только мир, ставится руками |
| Правила (hunger, nametags, drops…) | `settings.*` (мёртвые) | `match.*` | gamerules level.dat | — | `match_config.json`; плюс `applyMapConfig` второй раз ставит gamerules из старых значений `match.*` (`WarfareMod.java:253-288`) |
| Киты / пулы техники | — | NBT-фолбэк без core | — | — | БД; пишут и генератор (`import.sql`), и редакторы в игре (`FactionKitSetupItem`, `FactionVehicleSetupItem`) — [вывод] реимпорт затирает правки из игры |
| Server/SBW-настройки | — | — | — | — | `match_config.json` читают два разных POJO (`MatchConfig` и `ServerManager$ServerMatchConfig`) |

## 3. Ручные шаги подготовки (как сейчас)

| Шаг | Как делается | Нужен админ | Источник |
|---|---|---|---|
| Мир карты | папка мира по `worldPath`, копируется robocopy в каждый матч целиком вместе с SavedData | да | `ServerManager.java:108-118` |
| Мейн-зоны | вписать 2 бокса в `map_config.json` (или `/pwpwarfare mainzone` — до рестарта) | да | §2.3 |
| Спавн мейна | вписать `teams.*.spawn` руками (по факту центр мейна) | да | |
| Точки захвата | вписать 9 точек с координатами бокса и двумя приоритетами; цепочка должна идти строго 1..N без дыр (иначе захват ломается молча: дефолт 10, `GameLogicEvents.java:1545-1553`, `:2433-2442`) | да | |
| Порядок AAS | вручную через `bluePriority/redPriority` | да | |
| Спавнеры техники | 24 слота координат+yaw в JSON; машину подставляет пул фракции по `role` | да | |
| Хабы (FOB) | строят игроки (`HubBlock`); штрафы 10/30 тикетов захардкожены (`HubBlock.java:102`, `:131`) | нет | |
| Ралли | ставят СЛ | нет | |
| Main Supply, Vehicle Station, триггеры старта | поставить блоки в мире руками | да | |
| Граница | `mapBounds` руками (центр+размер) | да | |
| PNG карты | файл в `maps/` + `image` | да | |
| Матч | лобби: голосование карта → режим → фракции (`LobbyMod.java:800+`), старт `MatchAllocator.startMatch`, перекидывание `TransferHosts`; в матче фаза ожидания 300 с захардкожена (`WarfareMod.java:332-333`) → отсчёт | автостарт или `/pwp server start` | |
| Голосование «готовы» | только `/pwpwarfare votestart true` — на матч-серверах фактически не используется | да | |

## 4. Что можно вывести автоматически / что лишнее

| Предложение | Основание | Тип |
|---|---|---|
| Порядок AAS из одного списка `order` (или по расстоянию от мейна BLUE): `blue = i+1`, `red = N−i` | во всех 9 точках `red = 10 − blue` | факт по данным |
| Валидатор цепочки приоритетов при загрузке (дубли, дыры, дефолт 10) | `GameLogicEvents.java:1545` ищет ровно `prio−1` | факт |
| Спавн мейна = центр мейн-зоны, Y — по поверхности | совпадает в takmachka | факт по данным |
| `mapBounds` = бокс всех мейнов+точек + отступ | 2532 = ровно мейн-к-мейну | факт по данным |
| Слоты техники — от маркер-блоков/сущностей в мире (раскладка админом в игре), а не координаты в JSON | сейчас конфиг всё равно ставит блоки | вывод |
| Один источник правды для точек: либо map_config, либо SavedData — убрать «если пусто» | `WarfareMod.java:203` | факт |
| Удалить мёртвые поля `settings.hide*/disable*/lockDurationMinutes`, `durationMinutes`, POJO `vehicles` | §2.2 | факт |
| Удалить дубль `removemainzone` | `ModCommands.java:349/379` | факт |
| Исправить инверсию `hideDeathMessages = showDeathMessages` | `MatchConfigLoader.java:53`: при `showDeathMessages=true` свои сообщения о смерти скрыты, при `false` выключен gamerule — сообщений нет никогда | факт |
| `map setcenter/setsize` должны двигать world border, как конфиг | `ModCommands.java:762-825` vs `WarfareMod.java:154` | факт |
| Сохранять `autostart/maxmatches` лобби на диск | `ServerConfig.java:5-6` | факт |
| Два POJO для `match_config.json` → один в pwp-api | `MatchConfig.java`, `ServerManager.java:391` | факт |
| Фаза ожидания 300 с и штрафы FOB — в конфиг | `WarfareMod.java:333`, `HubBlock.java:102/131` | факт |

**Существующие админ-GUI** (все открываются предметами-«сетапами», клиентская проверка `PwpPermissions.isAdmin`): `KitSetupItem` → `KitTeamSelectScreen/KitListScreen/KitEditorScreen` (NBT-киты команды, легаси-фолбэк); `FactionKitSetupItem` → `FactionSelectScreen/FactionKitListScreen` (киты в БД); `FactionVehicleSetupItem` → `FactionVehicleSelect/List/EditorScreen`; `SkinSetupItem` → `SkinListScreen/SkinEditorScreen`; ПКМ админом по `vehicle_spawner` → `VehicleSpawnerScreen` (`VehicleSpawnerBlock.java:43`); `WarfareConfigScreen` (Forge mod-config, `ClientConfigRegistry.java:13`); общий `admin/AdminFormScreen`. **Редактора карты (точки, мейны, порядок AAS, спавны, граница) нет** — только JSON и 13-аргументная команда.

## 5. Онбординг игрока

### 5.1 Путь

| Этап | Что видит игрок | Неочевидно |
|---|---|---|
| Лобби | `PWPLobbyScreen` открывается сам по `LobbyStatePacket` (`PWPLobbyScreen.java:121-150`) | закрыл Esc — снова откроется только при смене фазы; клавиши или команды «открыть лобби» нет [вывод] |
| Голосование | вкладка Vote (или `/votemap`, `/votemode`, `/votefaction`) | — |
| Перекидывание в матч | авто, `/pwp join` если опоздал | команда нигде не подсказывается в GUI [вывод] |
| Вход в матч без команды | ADVENTURE + чат «Выберите команду, чтобы начать игру!» (`GameLogicEvents.java:1579`) | **не сказано, что нажать K** |
| Выбор команды | K → `TeamSelectionScreen` (`ClientEvents.java:503-512`) | |
| Ожидание 300 с | титр «Ожидание игроков» каждую секунду | что делать в это время, не сказано |
| Отряд/роль/спавн | K → `DeployScreen` (отряды, карта, роли), деплой только мёртвым | живой по K не деплоится (решение 15.11.2026) — нужно ли объяснение в UI |
| Голосования в матче | F9/F10 — старт, F7/F8 — КМД, PgUp/PgDn — подтверждение арты | захардкожены, нет в «Управлении» |
| Админ без матча | «PWP Warfare на паузе. /pwpwarfare gamestart true» (`GameLogicEvents.java:1583`) | только op |

### 5.2 Клавиши

| Действие | Клавиша | Переназначаемая | Файл | Конфликт |
|---|---|---|---|---|
| Сброс ящика (в технике) | X | да | `ModKeyBindings.java:15` | |
| Меню отряда / выбор команды | K | да | `:17` | |
| Карта (удерживать) | TAB | да | `:19` | ванильный список игроков (TAB) |
| Ники | Left Shift (UNIVERSAL) | да | `:21` | присед |
| Пинг | СКМ | да | `:27` | ванильный pick block |
| PTT отряда / командный | B / G | да | `:29`, `:31` | |
| Тактическая карта | M | **нет** | `ClientEvents.java:517` | |
| Голос КМД за/против | F7/F8 | **нет** | `:467`, `:474` | |
| Голос старта за/против | F9/F10 | **нет** | `:487`, `:492` | F9 в креативе = `PacketDebugFill` (`:529`) |
| Арта да/нет | PgUp/PgDn | **нет** | `:451`, `:455` | |

Туториалов или подсказок в игре нет. Гайды `guide/*.md` (10 файлов) — в формате Discord, в мод не встроены.

## 6. Топ-10 болей (по убыванию)

1. Нет редактора карты: точки, мейны, порядок AAS, спавны, граница, слоты техники — только JSON руками и `addpoint` с 13 аргументами.
2. Два источника правды для точек: если в мире есть точки из SavedData, `capturePoints` из `map_config` молча игнорируются (`WarfareMod.java:203`); мейны — наоборот.
3. Цепочка AAS-приоритетов не проверяется: дыра или дубль ломают захват молча; red/blue задаются руками, хотя выводятся из одного списка.
4. Инверсия `hideDeathMessages = showDeathMessages` (`MatchConfigLoader.java:53`): сообщения о смерти не показываются никогда.
5. Мёртвые поля в `map_config` (`settings.hide*/disable*`, `lockDurationMinutes`, `durationMinutes`) — админ правит, эффекта нет.
6. Команды молча «успешны» при неверном вводе (`gamemode`, `fraction` вообще без ответа, `teamtickets`, `teamspawn`, `clearsquad`, `removemainzone` возвращает 1); `votefaction` без подсказок и проверки.
7. `map setcenter/setsize` не двигают world border, `mainzone` не синхронизирует клиентов: результат команды расходится с поведением конфига.
8. Онбординг: «Выберите команду» без указания K; ожидание 300 с без инструкций; 8 захардкоженных клавиш (M, F7–F10, PgUp/PgDn) не видны в «Управлении», TAB/Shift/СКМ конфликтуют с ванилью.
9. Ручные блоки в мире (`game_start_trigger`, `main_supply`, спавнеры) нигде не описаны в конфиге — нельзя проверить готовность карты без захода в мир.
10. Настройки лобби (`autostart`, `maxmatches`) живут только в памяти; `match_config.json` читают два разных POJO; ожидание 300 с и штрафы FOB захардкожены.

## 7. Не проверено

- Всё выше — по коду; в игре ничего не запускалось.
- Как ведёт себя `/pwp server start <map> <mode> <blue> <red>` с несуществующей фракцией — код дочитан не полностью.
- Затирает ли реимпорт `import.sql` правки китов/техники из игровых редакторов — вывод по архитектуре, не по данным БД.
- Есть ли в лобби способ открыть `PWPLobbyScreen` после Esc (кроме смены фазы) — отдельной клавиши в коде не найдено.
