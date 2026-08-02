# PWP — карта проекта для агентов

PWP — тактический шутер «Squad в Minecraft» (Forge 1.20.1, Java 17), мультимодульный Gradle-проект (`com.pigeostudios.pwp`). Игра про командную тактику, а не киллы: тикеты, точки захвата, отряды, фобы/ралики, медицина, техника 4 фракций, прогрессия (Coins/XP/20 рангов/престижи/скины/боевой пропуск). Режимы: AAS (обе команды захватывают точки по порядку) и Invasion (асимметричный).

## Репозитории экосистемы (вне этого репо)

- `Pigeo-Studios/pwp` — **этот** репозиторий: код модов + core-service + боты + карты
- `Pigeo-Studios/Pwpfiles` (локально `C:\Users\maska\OneDrive\Desktop\Pwpfiles`) — релизный: собранные jar, libraries, шейдеры, ресурспаки, `gen.ps1` (генерация манифеста для лаунчера)
- `Pigeo-Studios/PWP-Launcher` (локально `C:\Users\maska\OneDrive\Desktop\PWP-Launcher`) — код лаунчера: Tauri v2 + React 18 + TS + Vite + Tailwind, v4.x
- `Pigeo-Studios/pwp-scheduler` (локально `C:\Users\maska\OneDrive\Desktop\SERVER STARTAP`) — Python-шедулер сервера
- `C:\Users\maska\OneDrive\Desktop\PWP-Server` — живой сервер (вне git). Пайплайн релиза и запуска описан в `build.bat` / `start.bat` на рабочем столе (не редактировать без явного запроса)

## Карта модулей

| Модуль | Что делает |
|---|---|
| `pwp-core-client` | Клиентское ядро: UI-тема `PWPTheme.java`, HUD, рендер через `GuiGraphics` |
| `pwp-core-server` | Серверное ядро |
| `pwp-lobby` | Лобби: голосование за карту/режим/фракции, старт игры (в match-серверах отсутствует). Состояние для GUI сервер шлёт единым `LobbyStatePacket` (pwp-core-client/network) каждую секунду + при изменениях + при входе — клиент ничего не вычисляет сам. Голосования не пересекаются (гвард `LobbyMod.isAnyVoteActive()`), политика запуска матчей проверяется при старте голосования и матча |
| `pwp-warfare` | Основной контент: техника, оружие (M2 Browning, AGS-30, миномёт), блоки (`vehicle_spawner`, `rally`, `fob`, `hub_block`, `game_start_trigger`, ящики снабжения), анимации/гео, команды `/pwpwarfare`. Зависимости: GeckoLib 4.4.9, Curios 5.14.1, Simple Voice Chat API 2.6.20 |
| `pwp-medicine` | Медицина: бинты/аптечки (lang: en/ru/uk), кровотечение, нок/гивап |
| `pwp-movement` | Клиентские движения: стойки, присед, камера, FOV |
| `pwp-limits` | Серверные лимиты/правила боя: кулдаун спринт-прыжка, скрытие HUD, блокировка F3/F5. Конфиг: `<мир>/serverconfig/pwplimit-server.toml` |
| `pwp-cosmetics` | Косметика: скины, эмоции |
| `pwp-blast-protection` | Защита от взрывов (реалистичные радиусы, теги блоков) |
| `pwp-drone` | Дроны, FPV-шейдеры (shaders/program, shaders/post) |
| `core-service` | Отдельный Java-сервис: Javalin (HTTP) + HikariCP/MySQL (`pwp_core`) + Gson + Logback. Порт :8080. API экономика/XP/ранги/скины/матчи/манифест лаунчера |
| `bots/` | Discord-бот (`discord_bot.py`) + Telegram-бот (`telegram_bot.py`) + `scheduler_loop.py`, общий `shared_state`, `client.py` (клиент к core-service API). Запуск: `py run.py`. Токены в `config.py` (gitignored) |
| `maps/` | Конфиги карт (`map_config.json`, пока одна: `takmachka`) |
| `docs/` | Дизайн-доки: `PROGRESSION_SYSTEM.md` (валюты, ранги, скины), `FUTURE_MECHANICS.md` |
| `guide/` | Игровые гайды: роли, техника, командир, режимы, сленг |
| `.opencode/plans/` | Дизайн-референсы: UI (PWP-DESIGN-REFERENCE.md), лаунчер (PWP-LAUNCHER-DESIGN-REFERENCE.md) |

## core-service: структура

Пакет `com.pwp.core`: `api/` (контроллеры Javalin) → `db/` (репозитории MySQL) → `model/` → `auth/` (`AuthMiddleware`). Контроллеры: Auth (authlib-injector + Yggdrasil), AuthLib, Admin, Case (кейсы/лутбоксы), Cosmetics, Currency, Donation, FactionVehicle, HWIDBan, Kit, Launcher (манифест для лаунчера), Match, MatchPolicy, Network, Player, Rank, Reward, Security (2FA), Shop, Skin/SkinV2, VoiceMute, Xp.

## Инфраструктура / запуск

- MySQL (XAMPP), БД `pwp_core`; core-service на :8080; Caddy — HTTPS-прокси `https://pigeo.asuscomm.com` → localhost:8080 (Caddyfile в корне); MC-сервер :25565
- Discord RPC: лаунчер слушает `127.0.0.1:42157` (`POST /status` с `{"state":"lobby|match|playing|idle","nickname":...,"faction":...}`) и ставит статус в Discord. Мод шлёт статус через `LauncherStatusReporter` (pwp-warfare/client): `handleSyncGameData` постит lobby/match (своя фракция против вражеской), отключение от сервера — playing
- Матч-серверы поднимаются пер-матч из `match_template/` (директории `match_*` gitignored)
- Сборка: `gradlew build -x test` (все моды), `gradlew shadowJar` (в `core-service`), `gradlew runClient` (dev-клиент)
- Требования: JDK 17 (путь прописан в `gradle.properties`), Gradle 8.14 (wrapper), Forge 1.20.1-47.3.0, official mappings, кодировка UTF-8
- Киты: дизайн в `docs/KITS_SQUAD_DESIGN.md` (Squad-адаптация, 6 фракций: usa/russia/ukraine/nato/insurgency/pmc, bluefor/redfor — заглушки). Генератор `tools/generate_kits.py` (Python, запуск `py -3`) → `maps/kits/<faction>.json` (KitDefinition для `/api/v1/kits/faction/{faction}/bulk`) + `maps/kits/import.sql`. Полная раскладка по слотам — `tools/generate_kits_doc.py` (пишет в `docs/KITS_SQUAD_DESIGN.md`). Варианты китов: Rifleman/Officer ×3 (Iron/Red Dot/Optic), Medic/LAT ×2, HAT/LMG/HMG ×2, Marksman ×2 (у инсургентов вариантов нет). Категории ролей: `DIRECT_COMBAT`/`FIRE_SUPPORT`/`SPECIALIST`/`SUPPORT`; сервер блокирует >3 FIRE_SUPPORT на отряд (меню деплоя + спавн). Меню деплоя перебирает все загруженные киты (не хардкод `KIT_NAMES`), API китов — источник истины (старые NBT-киты очищаются при загрузке). Формат предметов — как в `PacketSaveFactionKit` (tacz: `modern_kinetic_gun`+`GunId`/`AttachmentId`, патроны `tacz:ammo`+`AmmoId`, нож `lrtactical:melee`+`MeleeWeaponId`). Оружие: tacz-паки (cib/maxstuff/rfp) + FCL-пускачи из `pointblank/fcl-ext-0.1.zip` (РПГ-7В2/РПГ-26/M72/AT4/SMAW/Карл Густав) + SBW (только Javelin/Игла/C4/гранаты); броня — только `warbornrenewed:*`. Пистолетные патроны: P320 — `.45 ACP`; M2HB — `tacz:50bmg` (не 127x108); MRAD — `ea:416barrett`

## Конвенции кода

- UI только через `GuiGraphics`: `fill()`, `blit()`, `drawString()`, `enableScissor()`, абсолютное позиционирование x/y — никакого HTML/CSS/flexbox
- Все цвета/отступы/типографика из дизайн-системы `PWPTheme.java` (ARGB, `Spacing.*`, `TextStyle.*`) — никаких магических чисел
- Локализация: `assets/<modid>/lang/{en_us,ru_ru,uk_ua}.json` + `Component.translatable("key")`
- Mixins: конфиги в `src/main/resources/<mod>.mixins.json`, код в пакетах `mixin`; клиентский код — `@OnlyIn(Dist.CLIENT)`
- Комментарии и документация — на русском

## Важно: НЕ ТРОГАТЬ

- `explosionoverhaul.mixins.json` в корне — не удалять, не редактировать (не путать с `pwpblast.mixins.json` в модуле blast-protection)
- Секреты: не коммитить, не логировать, не выводить в ответах. `core-service/config.json` содержит database.password (исторически закоммичен — при работе с ним быть осторожным). API-ключ лежит в `build.bat`/`start.bat` на десктопе
- Модульные `README.md` (pwp-*/core-service) могут отставать от кода — истина в коде
- Рабочий стол пользователя (batch-файлы, папки репозиториев) — не трогать без явного запроса

## Обновление AGENTS.md

- Если нашёл что-то важное, чего тут нет (новый модуль/контроллер/команда, изменённые правила сборки, новая конвенция) — дополни этот файл сам, не спрашивая
- Держи карту модулей, команды сборки и список «не трогать» актуальными
- Удаляй устаревшее, если видишь, что описание больше не совпадает с кодом
- Секреты/токены в этот файл не добавлять никогда
