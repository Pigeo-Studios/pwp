# PWP — правила для агентов и людей

PWP — «Squad в Minecraft» (Forge 1.20.1-47.3.0, Java 17, official mappings, UTF-8). Командная тактика: тикеты, точки
захвата, отряды, ФОБы/ралли, медицина, техника по дивизиям фракций, прогрессия и косметика. Режимы — реестр
`com.pwp.api.match.GameModes` (AAS, Invasion, «Стычка»).

| Что | Где |
|---|---|
| Живой план, этапы, что сделано | `docs/ROADMAP.md` |
| Что проверить в игре | `docs/TEST_CHECKLIST.md` |
| Запуск беты (владельцу) | `docs/LAUNCH_PLAN.md` |
| Сервер одной командой, карта за 10 минут, правила `community` | `README.md` |

История решений и инцидентов — в git (`git log`, старые версии этого файла). Здесь только то, что нужно соблюдать.

Ветка работы — одна: `rewrite/unified` (новая работа — от неё; слитая ветка удаляется, её конец — тег `archive/<ветка>`). В `main` — только по слову владельца.

## Структура

Три мода — одинаковый набор на клиенте, лобби и матч-серверах:

| jar | modid | Состав (пакеты) |
|---|---|---|
| `pwp-core` | `pwpcore` | `com.pwp.api` (общее), `com.pwp.coreclient` (UI, сеть, админ-панель, меню), `com.pwp.coreserver` (ключ core, баны, `community`: жалобы/чат/резерв), `com.pwp.lobby` (только на лобби), `com.pwp.cosmetics`. Один `@Mod` — `com.pwp.api.PwpCoreMod` |
| `pwp-game` | `pwpwarfare` | `com.pigeostudios.pwp.warfare` (матч), `.medicine`, `.limits`, `.movement`, `.drone`, `com.pwp.blastprotection`. Один `@Mod` — `WarfareMod` |
| `sbwchunkload` | `sbwchunkload` | серверный аддон SBW: чанки для снарядов и дронов |

- `pwp-logic` — чистая логика без Minecraft (JUnit 5, `gradlew :pwp-logic:test` ~3 с). Не jar: `src/api`+`src/coreclient` компилируются в pwp-core, `src/warfare` — в pwp-game. Импорты `net.minecraft`/`net.minecraftforge`/`io.netty` там запрещены; Gson — compileOnly. **Новая логика с правилами — сюда, с тестами.**
- `core-service` — HTTP API + MySQL (Javalin), свой Gradle (`core-service/gradlew shadowJar`), тесты `core-service/src/test`.
- `bots/` (TG/Discord, `py run.py`), `pwp-scheduler/` (расписание сервера), `maps/` (карты, киты, техника, дивизии), `tools/` (генераторы, проверки, запуск), `guide/` (гайды игрокам).
- **Неймспейсы не менялись при слиянии:** реестры `pwpwarfare:*`, `pwp_medicine:*`; ассеты/lang `pwp_core_client`, `pwp_lobby`, `pwp_cosmetics`, `pwp_medicine`, `pwpdrone`, `pwpwarfare`. Строка `"pwp_core_client"` — неймспейс, не modid.
- `@Mod.EventBusSubscriber(modid = …)` — только `pwpcore` (в pwp-core) или `pwpwarfare` (в pwp-game), иначе Forge молча не подпишет класс.
- Forge-конфиги бывших модов — с прежними именами файлов (`pwp_medicine-common.toml`, `pwplimit-server.toml`, `pwpblast.toml`).

## Сборка, проверка, деплой

- `gradlew build` — все моды + тесты + spotless (~10–12 мин, запускать в фоне); `gradlew spotlessApply` перед сборкой.
- `gradlew :pwp-logic:test` — после каждого шага чистой логики.
- reobf гарантирует сама сборка (`jar` → `reobfJar` → проверка SRG); готовые jar — `py -3 tools/check_reobf.py`.
- Контент без игры — `py -3 tools/pwp_doctor.py` (киты, оружие по ганпакам, пулы, дивизии, карты, lang); `--displays` — display стволов для скинов.
- Деплой: `tools\pwp-server.ps1 -Build -Server <лобби>,<match_template>` (старые jar → `mods_backup_<дата>`). Полный локальный стенд — `tools\pwp-local.ps1 -Build -Start`, панель управления — `tools\pwp-control.cmd`.
- JDK 17: `-Dorg.gradle.java.home=<JDK17>`; Pwpfiles по умолчанию `../Pwpfiles` (или `PWPFILES_DIR`). Новый compileOnly-жарник — добавить в sparse-checkout CI (`.github/workflows/build.yml`).
- Обфускация релиза — `gradlew obfuscatePwpMods` (`proguard.pro`). Добавил `Class.forName`/рефлексию или Gson-маппинг POJO по именам полей — **добавь keep в `proguard.pro`**. Миксин-пакеты, `@Mod`, enum, `voicechat` уже в keep.
- Лаунчер (отдельный репо): сборка только `npm run tauri build` (голый `cargo build` даёт exe, открывающий localhost). Ганпаки zip — не PowerShell `Compress-Archive` (пути с `\` ломают TaCZ).

## Конвенции кода

### Общее
- Комментарии и документы — на русском. Файлы — UTF-8.
- Пустой `catch` запрещён: минимум `LOG.warn(…, e)`; в покадровом коде — `LogOnce.warn`. `System.out/err` запрещены. Числа из ввода — `Parse.intOr/floatOr`.
- Время на сервере — только `GameClock`: `serverTicks` (в памяти: каналы, таймеры) или `worldTicks` (что сохраняется в данные игрока/SavedData); `realMillis` — для настенного времени (антиспам, очереди). Кулдаун — абсолютная метка «до», перезаписывается, не прибавляется.
- Исключение — `sbwchunkload`: отдельный мод без зависимости от pwp-core, тики — `server.getTickCount()`.
- Клиенту не доверять: C→S несёт только идентификаторы/намерение, сервер сам проверяет право и сам строит предметы/состояние.
- Отказ действия игрока — всегда с причиной (`PlayerFeedback.deny/info`, ключ lang). Молча — только для заведомо битого ввода.
- Кик/`disconnect()` в цикле по `getPlayerList().getPlayers()` — только по копии списка: на серверном потоке игрок удаляется сразу (CME).

### Сеть
- Новый пакет — сеть v2: record `Net.C2S` (с `validate()`) / `Net.S2C`, регистрация через `Net.Registrar` **в конец** канала. Один пакет = одно действие. Большие строки S→C — `Payloads.writeLarge`.
- Каналы: `pwpwarfare:main`=10, `pwp_core_client:network`=9. Удаление/перестановка/новый пакет, без которого старый клиент сломается, — поднять `PROTOCOL_VERSION`.
- Данные для GUI — тип в `DataRequests.register`, не новый пакет.
- Все `Packet*` warfare обязаны быть в `PacketHandler.register()` (иначе краш при первой отправке).

### Видимость по командам (античит карты)
- Состояние матча (SyncGameData, отряды, нок, игроки на карте) — только через `PacketHandler.sendSync/sendToAllClients/syncSquadsAll/syncSquadsTo/syncDowned` и `PacketHandler.viewerKey` → `TeamVisibility`. Прямой `send(ALL, …)` таких пакетов — мап-хак.
- «Видит всё» — только модератор-наблюдатель (`viewerKey(team, spectator||creative, MODERATOR+)`). Не проверять `isSpectator()||isCreative()` напрямую.
- Ники над головой: `HIDE_FOR_OTHER_TEAMS` (врагу никогда), `hideNametags` → `NEVER`.
- Враг, раскрытый способностью (UAV и т.п.), уходит только обезличенным: `MapPlayerInfo.anonymous` (позиция + команда).

### Core-service (HTTP)
- HTTP к core — только `CoreApi`. На главном потоке сервера не ждать: `ServerThread.thenOnServer`; с игроком — `thenForPlayer` (игрок ищется заново по UUID); из пакета игрока — `guardedForPlayer` + `RequestGuard`. `CoreApi.await` — только в фоне.
- Адрес core в модах — `http://127.0.0.1:8080`, не `localhost` (на Windows резолвится в `::1`).
- В core: `/api/v1/admin/*` проверяет один `verifyAdmin` (сессия admin/owner или API-ключ); изменения экономики/матча/модерации/контента — в `AccessPolicy.SERVER_ONLY`; сессия меняет только свой uuid; `/api/v1/public/*` — без ключа, без uuid/IP/портов. IP клиента — `ClientIp.of(ctx)`.
- Сайт и веб-модерация — `core-service/src/main/resources/site/` (отдаёт core на `/` и `/admin/moderation`); только публичное API или `/api/v1/admin/*` с ключом, вывод — `textContent`.
- Новые таблицы — `CREATE TABLE IF NOT EXISTS` в `DatabaseManager` (+ `.sql` в `core-service/sql`). **Удалять таблицы/данные — только с «ок» владельца.** Импорт SQL с кириллицей — `cmd /c "mysql … < file.sql"`, не PowerShell-пайп.

### Данные и состояние
- Состояние PWP у игроков и сущностей — только `WarfareData.of(entity)` + ключи `WarfareKeys` (имя ключа = имя в NBT, не менять). Что переживает смерть — `PlayerCloneRules`.
- Мир матча — реестры `WarfareWorldData` (`match`, `squadRegistry`, `structures`, `markers`, `vehicles`, `mapEdit`); новое поле — в свой реестр вместе с save/load.
- Конфиг сервера — `config/pwp-server.json` (`ServerSettings` + `CommunitySettings`, доступ `ServerRole.settings()`): роль, лобби, шаблон матчей, папка карт, правила публичного сервера (`community`). Баланс боя — Forge-TOML (`pwpwarfare-common.toml`).
- Права: `PwpPermissions.has(player, AccessLevel)` — PLAYER < MODERATOR < ADMIN < OWNER (op 2–3 = ADMIN, op 4 = OWNER, либо роль в БД). Проверка — на сервере в каждом пакете; `isCreative()` правом не является.
- Админ-действие — только через `AdminAction.runLocal/run` (иначе его нет в аудит-логе `logs/pwp-admin-audit.log`).
- Админ-команда (консоль/op) — запись `AdminAudit.recordSource(src, …)`. Строка-ответ сервера в пакете — ключ lang, клиент показывает `I18n.get`.

### UI
- Только `GuiGraphics` и UI-кит `com.pwp.coreclient.ui` (`UIButton`, `UICard`, `UIScrollArea`, `AdminForm`, …) + лейаут `ui/layout` (`Rect`, `Flow`, `Grid`, `TextFit`). Цвета/отступы — `PWPTheme` (warfare — `WTheme` поверх). Публичный API кита меняется только добавлением.
- **Новый экран игрока или админа — карточка в «Меню PWP»** (`PwpHub.register`, lang `pwp_core.hub.<id>.title/.desc`; core — `CoreHubEntries`, warfare — `WarfareHubEntries`). Не новая кнопка паузы, не предмет, не команда (команда — только фолбэк).
- HUD — только зоны `Hud.zones()`/`HudZones` (перекрытий нет по построению, тест на разрешения); гейт `WHud.hudVisible()`.
- Подписи на картах — через `map/LabelPlacer` (`reset` → `block` иконок → `place`), а не голым `drawString`.
- Экраны с `EditBox`: Esc обрабатывать до делегирования в поле. Экраны в превью (`/pwpui`, `-Dpwp.uitest=true`) не шлют пакетов — сеть через слой «источник данных».
- Текст игроку — только ключи lang (`en_us`/`ru_ru`/`uk_ua`), аргументы `%s`, цвет `withStyle`, §-коды запрещены. Новый ключ — сразу во все три языка (`tools/pwp_doctor.py` проверяет).
- Клавиши — `KeyMapping` (переназначаемые); важная клавиша — в `assets/pwp_core_client/controls/whitelist.json`.

### Игровые системы — точки входа
- Доступность кита — только `KitAccess.unavailableReason` (→ `KitLimits`); кит игрока — `KitUtil.getEffectiveKit`; синхронизация — `KitUtil.syncMyKit`.
- Дивизии и пулы техники — `Division`/`DivisionPlan` (pwp-logic) + `WarfareWorldData.applyDivisions`.
- Скины стволов — `WeaponSkin` (`slotType=WEAPON`, `weaponTag=GunId`, `modelPath=display:<id>`) → `WeaponSkinService` ставит TaCZ `GunDisplayId` + `PwpSkin`. GunId скин не меняет никогда.
- Чат — только через `ChatGuard.filter` (фильтр `config/pwp_chat_filter.txt` + антиспам); новый канал чата обязан идти через него.
- Жалобы — `ReportService.report/auto`; история наказаний — `CoreApi.recordPunishment`.
- Подключение игрока к матчу из лобби — только `MatchAllocator.connectOrQueue` (очередь + резервные слоты).
- Сторож матча — `MatchWatchdog`; AFK — `AfkTicker`/`AfkWatch`; тимкилл — `TeamkillLadder`; закрепление техники — `VehicleClaim`.

## Mixins
- Конфиги в `<mod>.mixins.json` (с `refmap`), код в пакетах `mixin`; клиентский — `@OnlyIn(Dist.CLIENT)`, серверный — секция `server`.
- Модуль с миксинами — плагин `org.spongepowered.mixin` + `annotationProcessor mixin:0.8.5:processor` + guard на refmap в jar.
- Не `@Invoker` на генерик-методы, унаследованные подклассами; доступ к полям — `@Accessor`, не `@Shadow`; инжект в унаследованный метод — с owner-квалификацией.
- На чужие моды (SBW/pointblank/TaCZ) — `remap = false`, таргет через compileOnly прод-жарник. Лучше — единая точка на vanilla (`ServerLevel.addFreshEntity` + `instanceof`) вместо миксина в чужой класс.
- `sbwchunkload`: уровень тикета `min(3, simulationDistance)`; свой `TicketType<Integer>`; проверка `isPositionEntityTicking`; сборка только с reobf.

## Античит (лаунчер + core)
- Java-агент никогда не кикает сам: репортит в core и кикает только по ответу `{action: kick}`. Seed-маркеры — телеметрия; кик — правила из БД с severity ≥ `anticheat.banSeverity` (по умолчанию 100 = только отчёты).
- Эвристика нестандартных имён классов — только вне доверенных корней (`-Dac.trusted`).

## НЕ ТРОГАТЬ
- `explosionoverhaul.mixins.json` в корне.
- Секреты: не коммитить, не логировать, не выводить (`core-service/config.json`, API-ключ в `build.bat`/`start.bat`, `bots/config.py`).
- Рабочий стол пользователя: `.bat`, `Desktop\PWP-Server` (живой сервер), прод, содержимое Pwpfiles — без явного запроса.
- Модульные `README.md` могут отставать — истина в коде.

## Этот файл
- Нашёл важное, чего тут нет (новая точка входа, правило сборки, конвенция), — допиши коротко, в нужный раздел.
- Устаревшее — удаляй. Историю и подробности — в `docs/ROADMAP.md` и коммиты, не сюда.
