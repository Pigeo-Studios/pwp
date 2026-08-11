# PWP — карта проекта для агентов

PWP — тактический шутер «Squad в Minecraft» (Forge 1.20.1, Java 17), мультимодульный Gradle-проект (`com.pigeostudios.pwp`). Игра про командную тактику, а не киллы: тикеты, точки захвата, отряды, фобы/ралики, медицина, техника 4 фракций, прогрессия (Coins/XP/20 рангов/престижи/скины/боевой пропуск). Режимы: AAS (обе команды захватывают точки по порядку) и Invasion (асимметричный).

## Как агенты работают с правилами

Объёмные правила модулей и доменов живут НЕ в этом файле, а в `docs/rules/*.md`. Загрузка ленивая — по надобности.

- **Перед изменением кода** в модуле/домене — прочитай его файл правил через Read (пути — в таблице ниже и в списке доменов).
- **После изменений** (новый модуль/фича, инцидент, фикс, изменённые правила сборки/структура) — **обнови тот же файл правил**: текст на русском, метки дат `(дд.мм.2026)`, удаляй устаревшее, если описание разошлось с кодом; секреты/токены не добавлять.
- В корневом AGENTS.md держи только общее (конвенции, «не трогать», навигация) — детали модулей не дублировать, их место в файлах правил.

## Репозитории экосистемы (вне этого репо)

- `Pigeo-Studios/pwp` — **этот** репозиторий: код модов + core-service + боты + карты
- `Pigeo-Studios/Pwpfiles` (локально `C:\Users\maska\OneDrive\Desktop\Pwpfiles`) — релизный: собранные jar, libraries, шейдеры, ресурспаки, `gen.ps1` (генерация манифеста для лаунчера)
- `Pigeo-Studios/PWP-Launcher` (локально `C:\Users\maska\OneDrive\Desktop\PWP-Launcher`) — код лаунчера: Tauri v2 + React 18 + TS + Vite + Tailwind, v4.x
- `Pigeo-Studios/pwp-scheduler` (локально `C:\Users\maska\OneDrive\Desktop\SERVER STARTAP`) — Python-шедулер сервера
- `C:\Users\maska\OneDrive\Desktop\PWP-Server` — живой сервер (вне git). Пайплайн релиза и запуска описан в `build.bat` / `start.bat` на рабочем столе (не редактировать без явного запроса)

## Карта модулей

| Модуль | Что делает | Правила |
|---|---|---|
| `pwp-core-client` | Клиентское ядро: UI-тема `PWPTheme.java`, HUD, рендер через `GuiGraphics` | — |
| `pwp-core-server` | Серверное ядро | — |
| `pwp-lobby` | Лобби: голосование за карту/режим/фракции, старт игры (в match-серверах отсутствует). Состояние для GUI сервер шлёт единым `LobbyStatePacket` (pwp-core-client/network) каждую секунду + при изменениях + при входе — клиент ничего не вычисляет сам. Голосования не пересекаются (гвард `LobbyMod.isAnyVoteActive()`), политика запуска матчей проверяется при старте голосования и матча | — |
| `pwp-warfare` | Основной контент: техника, оружие (M2 Browning, AGS-30, миномёт), блоки (`vehicle_spawner`, `rally`, `fob`, `hub_block`, `game_start_trigger`, ящики снабжения), анимации/гео, команды `/pwpwarfare`. Также: деплой-система дронов, РЭБ-стройка, мейн-зона/фидбеки, деплой-UI | `docs/rules/warfare.md` |
| `pwp-medicine` | Медицина: бинты/аптечки (lang: en/ru/uk), кровотечение, нок/гивап | — |
| `pwp-movement` | Клиентские движения: стойки, присед, камера, FOV | — |
| `pwp-limits` | Серверные лимиты/правила боя: кулдаун спринт-прыжка, скрытие HUD, блокировка F3/F5. Конфиг: `<мир>/serverconfig/pwplimit-server.toml` | — |
| `pwp-cosmetics` | Косметика: скины, эмоции | — |
| `pwp-blast-protection` | Защита от взрывов (реалистичные радиусы, теги блоков) | — |
| `pwp-drone` | Дроны, FPV-шейдеры (shaders/program, shaders/post). Серверная логика дронов — в pwp-warfare | — |
| sbw_mavic_extender (v1.1.1) | Сторонний мод [SBW] Mavic drone extender (BOTH): клиентский стриминг чанков для оператора дрона | `docs/rules/mavic-extender.md` |
| `sbwchunkload` | Серверный аддон чанк-лодинга снарядов и дронов (проектайл-трекер, drone-трекер, тикеты, гигиена jar) | `docs/rules/sbwchunkload.md` |
| `bots/` | Discord- и Telegram-боты + `scheduler_loop.py`, общий `shared_state`, клиент к core-service API | `docs/rules/bots.md` |
| `maps/` | Конфиги карт, спавны/пулы техники, размеры инвентаря техники и цепочка слотов спавнера | `docs/rules/vehicles.md` |
| `docs/` | Дизайн-доки: `PROGRESSION_SYSTEM.md` (валюты, ранги, скины), `FUTURE_MECHANICS.md`. Справочники предметов (генерируются): `GUN_ID_REFERENCE.md` (id+названия pointblank/FCL/SBW), `GUN_STATS_REFERENCE.md` (статы tacz/SBW/FCL), `ATTACHMENTS_REFERENCE.md` (аттачменты tacz-паков со статами), `ARMOR_REFERENCE.md` (броня WRB со статами из bytecode + SBW), `VEHICLES_REFERENCE.md` (каталог сущностей техники SBW/VVP/FCP из lang-джаров) | — |
| `tools/` | Генераторы: `generate_kits.py` (киты → `maps/kits/*.json` + `import.sql`), `generate_kits_doc.py` (дизайн-док), `generate_gun_reference.py`, `generate_gun_stats_reference.py`, `generate_attachments_reference.py`, `generate_armor_reference.py` (справочники в `docs/`; armor-генератор декомпилирует WRB через javap, JDK 17), `generate_vehicle_reference.py` (каталог техники из джаров → `docs/VEHICLES_REFERENCE.md`), `generate_vehicle_pools.py` (пулы фракций → `maps/vehicles/import.sql`) | — |
| `guide/` | Игровые гайды: роли, техника, командир, режимы, сленг | — |
| `.opencode/plans/` | Дизайн-референсы: UI (PWP-DESIGN-REFERENCE.md), лаунчер (PWP-LAUNCHER-DESIGN-REFERENCE.md) | — |

## Донат-тиры и роль-статусы (лобби)

Косметические тиры `NONE/SILVER/GOLD/PLATINUM` (не P2W): выдача только админом через веб-админку. Контур (11.08.2026):
- core-service: `POST /api/v1/admin/set-donate-tier`, поле `donateTier` в профиле (`/player/{uuid}`, `/player/load`, `/admin/find-user`, `/admin/players`) — см. `docs/rules/core-service.md`.
- dashboard (админка «Пользователи»): блок «Донат-тир» с Select.
- pwp-lobby `donate/`: `DonatorStatusManager` (тир+role из core-service профиля, флай GOLD/PLATINUM только в лобби) и `particles/`: `DonatorParticleConfig`/`RoleParticleConfig` (serverconfig `pwp_lobby/donator_particles.toml`, `role_particles.toml`) + `DonatorParticleController` (эффекты/бюджет/LOD, приоритет ADMIN > MODERATOR > PLATINUM > GOLD > SILVER).
- pwp-core-client: `PacketDonatorTiers`+`DonatorCache`, `NameGradient`, `DonatorNameTagRenderer` (градиентный надмид), свечение имён в `LeaderTabRenderer`/`StatsScreen`.

### Домены вне модульной карты

- **core-service** (структура, контроллеры) — `docs/rules/core-service.md`
- **Инфраструктура / запуск** (MySQL/Caddy, Discord RPC, матч-серверы, optional-моды, transfer-хосты, сборка, требования) — `docs/rules/infrastructure.md`
- **Лаунчер** (сборка через tauri, сессия/503/access-token) — `docs/rules/launcher.md`
- **Киты** (генератор, слоты, лимиты, метки) — `docs/rules/kits.md`
- **Обфускация pwp-модов (ProGuard)** — `docs/rules/proguard.md`
- **Античит** (архитектура v2) — `docs/rules/anticheat.md`

## core-service: структура

Пакет `com.pwp.core`: `api/` (контроллеры Javalin) → `db/` (репозитории MySQL) → `model/` → `auth/` (`AuthMiddleware`). Контроллеры: Auth (authlib-injector + Yggdrasil), AuthLib, Admin, Case (кейсы/лутбоксы), Cosmetics, Currency, Donation, FactionVehicle, HWIDBan, Kit, Launcher (манифест для лаунчера), Match, MatchPolicy, Network, Player, Rank, Reward, Security (2FA), Shop, Skin/SkinV2, VoiceMute, Xp. Подробнее в `docs/rules/core-service.md` — там же правила обновления.

## Глобальные конвенции кода

- UI только через `GuiGraphics`: `fill()`, `blit()`, `drawString()`, `enableScissor()`, абсолютное позиционирование x/y — никакого HTML/CSS/flexbox
- Все цвета/отступы/типографика из дизайн-системы `PWPTheme.java` (ARGB, `Spacing.*`, `TextStyle.*`) — никаких магических чисел
- Локализация: `assets/<modid>/lang/{en_us,ru_ru,uk_ua}.json` + `Component.translatable("key")`
- Mixins: конфиги в `src/main/resources/<mod>.mixins.json`, код в пакетах `mixin`; клиентский код — `@OnlyIn(Dist.CLIENT)`
- Mixin-модули обязаны подключать плагин `org.spongepowered.mixin` (mixingradle 0.7-SNAPSHOT в buildscript) + `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'`, объявлять `refmap` в mixins.json и иметь guard на `jar` (сборка падает, если рефмапа нет в jar — см. `pwp-core-client/build.gradle`). Без рефмапа миксины по mojmap-именам не находят таргеты в проде (SRG) — краш клиента (был инцидент 02.08.2026: `LevelTransitionMixin` на `setLevel`)
- Mixin AP ломается на генерик-методах, наследуемых подклассами (`Screen.addRenderableWidget` дублируется в tsrg ForgeGradle): НЕ использовать `@Invoker` для таких методов (ScreenInvoker удалён); доступ к полям цели — через `@Accessor` (рефмап-запись генерируется стабильно, `ConnectScreenAccessor`/`DisconnectedScreenAccessor`), НЕ через `@Shadow` (записей в рефмапе нет). Инжект в унаследованный метод — только с owner-квалификацией (`method = "Lnet/.../Screen;mouseClicked(DDI)Z"`)
- Миксины на ЧУЖИЕ моды (SBW/pointblank): Mixin AP не имеет SRG/MCP-маппингов для их методов и падает с ошибкой «Unable to locate obfuscation mapping» — на `@Inject` обязателен `remap = false`, таргет-класс — прямой `@Mixin(Класс.class)` через `compileOnly files(...)` прод-жарник. ПРЕДПОЧТИТЕЛЬНО вместо миксинов на чужие классы — единая точка входа на vanilla (`ServerLevel.addFreshEntity` + `instanceof`-проверки по интерфейсам чужих модов + серверный тикер) — так покрываются любые будущие аддоны без новых миксинов (пример: `sbwchunkload` v1.1+). Server-only миксины класть в секцию `server` mixins.json
- Комментарии и документация — на русском

## Важно: НЕ ТРОГАТЬ

- `explosionoverhaul.mixins.json` в корне — не удалять, не редактировать (не путать с `pwpblast.mixins.json` в модуле blast-protection)
- Секреты: не коммитить, не логировать, не выводить в ответах. `core-service/config.json` содержит database.password (исторически закоммичен — при работе с ним быть осторожным). API-ключ лежит в `build.bat`/`start.bat` на десктопе
- Модульные `README.md` (pwp-*/core-service) могут отставать от кода — истина в коде
- Рабочий стол пользователя (batch-файлы, папки репозиториев) — не трогать без явного запроса

## Обновление документов (AGENTS.md и docs/rules/)

- Если нашёл что-то важное, чего нет в правилах (новый модуль/контроллер/команда, изменённые правила сборки, новая конвенция) — дополни **файл правил соответствующего модуля/домена** сам, не спрашивая (пути — в таблице «Карта модулей» и списке доменов выше)
- Держи карту модулей, команды сборки и список «не трогать» актуальными
- Удаляй устаревшее, если видишь, что описание больше не совпадает с кодом
- Секреты/токены в файлы правил не добавлять никогда
