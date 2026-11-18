# Аудит «внутрянки» PWP (ветка `rewrite/internals`, 18.11.2026)

Состояние: от `main` + `rewrite/gameplay-setup` (включая незакоммиченный этап 5 агента) + `rewrite/controls` (включает `rewrite/hud-style`).
Метод: чтение кода, сборка и тесты. **Игра и серверы не запускались**: TPS/MSPT/FPS и время входа помечены «не замерено».

## Базовые цифры (до оптимизаций)

| Что | Значение |
|---|---|
| `gradlew build -x test` (инкрементально, после слияния) | 37 с, BUILD SUCCESSFUL |
| Юнит-тесты | 319 зелёных (pwp-logic 298, pwp-warfare 16, pwp-api 5) |
| core-service `shadowJar` | 14.3 МБ, собирается |
| jar: warfare / core-client / lobby | 13.9 МБ / 5.2 МБ / 80 КБ; reobf проверен (SRG `m_91268_`, `m_91087_` на месте) |
| TPS/MSPT матча, FPS клиента, время старта/переброса | **не замерено** (нужен spark в живом матче) |

## Итоги слияния (что проверить в игре)

- Клавиши голосований сведены к одному набору: `VOTE_START_*` (F9/F10 «готов») и `VOTE_CMD_*` (F7/F8 командир). Дубли `READY_*`/`CMD_VOTE_*` удалены. **В старом `options.txt` переназначения этих клавиш сбросятся на дефолт.**
- Новый HUD из hud-style + плашка «ПОДГОТОВКА · М:СС»/канал рации (`ClientStaging.renderHud`) из gameplay-setup. **Проверить:** плашка не наезжает на верхний HUD (тикеты/компас), канал рации не дублирует голосовой стек `SideHud`.
- Версии каналов подняты: `pwpwarfare:main` 5, `pwp_core_client:network` 6. Старый клиент получит отказ Forge о несовпадении версий, а не краш.

## Таблица находок

Цена: К — критично, В — высокая, С — средняя, Н — низкая. Риск — риск самой правки.

### core-service + БД

| # | Проблема | Где | Цена | Решение | Риск |
|---|---|---|---|---|---|
| C1 | Весь `/api/v1/admin/` в PUBLIC_PATHS (проверка по `startsWith`). У `set-role` нет `verifyAdmin`: **любой анонимно ставит себе owner** | `auth/AuthMiddleware.java:35`, `api/AdminController.java:101` | К | убрать префикс из PUBLIC_PATHS; `verifyAdmin` в set-role (бот ходит с API-ключом) | Н |
| C2 | `find-user` без `verifyAdmin`: анонимно отдаёт last_ip, telegram_id, роль, 2FA | `api/AdminController.java:27` | К | `verifyAdmin` первой строкой | Н |
| C3 | Код 2FA пишется открытым текстом в `player_logs`, а `/admin/audit` отдаёт логи без авторизации: **обход 2FA** | `api/AuthController.java:132`, `api/NetworkController.java:196` | К | не логировать код; `verifyAdmin` в audit | Н |
| C4 | На каждом старте core: `DROP TABLE` player_logs, password_resets, twofa_codes, trusted_ips, broadcast_log, accounts (забытая разовая миграция) | `db/DatabaseManager.java:218-226` | В (каждый рестарт теряется аудит) | удалить блок | Н |
| C5 | За Caddy `ctx.ip()` всегда 127.0.0.1: лимиты логина/API общие на всех (DoS логина), IP-бан может забанить всех, trusted_ips бесполезен | 29 вызовов `ctx.ip()`, `Caddyfile` | В | хелпер `clientIp()`: X-Forwarded-For только от 127.0.0.1 | С |
| C6 | Нет проверки владельца: игрок со своей HMAC-сессией вызывает `currency/add`, `xp/add`, `cosmetics/grant`, `toggle-2fa` с ЛЮБЫМ uuid | `auth/AuthMiddleware.java:86-121`, `api/CurrencyController.java:11`, `api/AuthController.java:328` | К | атрибут `authKind` (apiKey/session); серверные эндпоинты — только apiKey, пользовательские сверяют uuid с сессией | С (лаунчер/моды должны слать правильную авторизацию) |
| C7 | HMAC подписывает `ts:path` без метода и тела | `auth/AuthMiddleware.java:110` | С | `ts:method:path:sha256(body)` + версия подписи; согласованно с лаунчером | С |
| C8 | Секреты в git: API-ключ, p5Secret, root без пароля; ПДн подписчиков | `core-service/config.json`, `bots/notify_subs.json` | В | ротация ключа и p5Secret (делает юзер), `config.example.json`, файлы в `.gitignore` | Н |
| C9 | `/launcher/logs` публичный, без лимита записей (тело до 64 МБ) | `api/LauncherController.java:189` | С | кап ≤500 записей и размера тела | Н |
| C10 | `/launcher/files`: `readAllBytes` в heap при `-Xmx512M` + info-лог на файл | `api/LauncherController.java:222` | С (OOM при параллельных загрузках) | стриминг + Content-Length, лог в debug | Н |
| C11 | Скриншоты/детекты античита без капа, TTL и ротации | `api/AnticheatController.java:146,296` | С | кап на launch-токен, TTL-чистка в часовом cleanup | Н |
| C12 | ~40 `ALTER ADD COLUMN` на каждом старте пишут warn со стектрейсом | `db/DatabaseManager.java:243-300` | Н (тонут реальные ошибки) | молча ловить 1060/1061; затем версионированные миграции (`sql/`) | Н |
| C13 | Схема в двух местах (`DatabaseManager` + `sql/`); дефолт `players.role`='PLAYER' при валидных user/support/admin/owner | `db/DatabaseManager.java:53` | С | одна система миграций; дефолт 'user' + UPDATE | С |
| C14 | Слушает 0.0.0.0, CORS anyHost (host из конфига не используется) | `CoreApplication.java:45,91` | С (обход Caddy/TLS при проброшенном порте) | `start("127.0.0.1", port)`, CORS по списку | Н |
| C15 | Карты лимитов `ipBuckets`/`loginBuckets` не чистятся | `auth/AuthMiddleware.java:195`, `api/AuthController.java:531` | Н | чистка по TTL | Н |
| C16 | Метрик нет, только `/health` | `CoreApplication.java:51` | С | Micrometer: Hikari, p95 по маршрутам → `/admin/metrics` + простой дашборд | Н |

N+1 не найдено (`saveMatch` пишет батчем, лидерборд одним JOIN). EXPLAIN не запускался: БД не трогали.

### Моды (сервер/сеть)

| # | Проблема | Где | Цена | Решение | Риск |
|---|---|---|---|---|---|
| M1 | **Мап-хак из коробки.** `PacketSyncGameData` уходит ВСЕМ и несёт хабы/ФОБы и активные маркеры ОБЕИХ команд. В Squad вражеские ФОБы и метки не видны | `network/PacketHandler.java:497,533`, ~50 вызовов `sendToAllClients` | В (честность и трафик) | собирать пакет на команду: свои хабы/маркеры + разведанное; наблюдателю — всё | С |
| M2 | `PacketSyncSquads` (весь реестр, включая отряды, маркеры и состав врага) — `ALL` | `events/GameLogicEvents.java:740,1565`, `block/RallyPointBlockEntity.java:61` | В | рассылка по команде; врагу только число отрядов | С |
| M3 | Монолитный SyncGameData (50+ полей, списки) шлётся на любое событие: выбор кита, маркер, голос, стройка | см. M1 | С (трафик ∝ игроки × события; **не замерено**) | разбить на дельты: тикеты/фаза, хабы, маркеры, техника; полный — только при входе | С |
| M4 | `PacketSyncDownedState` — `ALL` | `events/DownedHandler.java:137,205,355`, `GameLogicEvents.java:1602,1800` | Н | `TRACKING_ENTITY_AND_SELF` + своя команда (медикам) | Н |
| M5 | `GameLogicEvents` — бог-класс ~2700 строк: тик матча, голосования, захват, стейджинг, деплой | `events/GameLogicEvents.java` | С (надёжность, правки ломают соседей) | разнести по `match/` (тик захвата, голосования, фаза) без смены поведения | С |
| M6 | Легаси-миграция `WARFARE_*` из persistentData всё ещё копирует и держит старые ключи. Совместимость больше не нужна (юзер разрешил ломать) | `data/state/WarfareMigration.java`, `PlayerCloneRules.java:81` | Н (мусор в NBT) | включить `removeLegacy`, а в следующей версии удалить миграцию | Н |
| M7 | 114 `new ResourceLocation(...)` в клиенте вне `static final` — возможна аллокация в кадре | `pwp-warfare/.../client/**` | Н (**FPS не замерен**) | вынести в константы там, где вызов покадровый | Н |
| M8 | Тикеры, которые идут каждый тик по всем игрокам/уровням: `DroneDeploymentEvents` (все игроки), `GameLogicEvents.onServerTick` (все уровни + `WarfareWorldData.get`) | `events/DroneDeploymentEvents.java:274`, `GameLogicEvents.java:329` | Н (**MSPT не замерен**) | ранний выход без активных каналов; один уровень матча | Н |

Гигиена модов в порядке: `System.out`, `printStackTrace` и пустых `catch` нет, прямой `getTickCount/getGameTime` на сервере 1 случай, `getPersistentData` вне `data/state` 2.

### Боты / шедулер

| # | Проблема | Где | Цена | Решение | Риск |
|---|---|---|---|---|---|
| B1 | Уведомления подписчикам, похоже, мертвы: `scheduler_loop.run` никто не запускает, а `scheduler_status.json` шедулера боты не читают | `bots/run.py:82-99`, `bots/telegram_bot.py:120-150`, `pwp-scheduler/scheduler.py:157,272` | С (вывод по коду) | один источник: боты читают json (атомарная запись) | С |
| B2 | Блокирующие `subprocess.run(netstat)` и `urlopen` в asyncio | `bots/scheduler_loop.py:34,66,180,239` | С (до ~11 с фриза ботов) | `asyncio.to_thread` / async-клиент | Н |
| B3 | `httpx.AsyncClient` создаётся на каждый вызов без закрытия; `except:` без лога | `bots/discord_bot.py:32,40` | Н (утечка сокетов) | один клиент на модуль | Н |
| B4 | `_notify_all` глотает ошибки (заблокировавший бота вечно в рассылке) | `bots/telegram_bot.py:152` | Н | Forbidden → удалить подписчика; лог с анти-спамом | Н |

### Лаунчер
**Не проверен**: репозитория PWP-Launcher на этой машине нет (пути `C:\Users\maska\...` из AGENTS.md здесь не существуют). Нужен путь или клон.

## Сделано

- **Этап 1 (18.11.2026):** C1–C3 — `app.before("/api/v1/admin/*")` → `verifyAdmin` на ВСЕХ админ-маршрутах (сессия admin/owner или API-ключ), код 2FA в лог не пишется; C4 — удалён DROP TABLE на старте; C9 — `/launcher/logs` ≤2 МБ тела и ≤500 записей; C14 — бинд на `server.host` (по умолчанию и в config.json 127.0.0.1). Не в игре: собран `shadowJar`. C8 (ротация ключа/p5Secret) — за юзером.

## План этапов (коммит на этап, без push)

1. **P0 безопасность core** (C1–C4, C9, C14): часа на два, риск низкий, моды и лаунчер не затрагивает. Ротацию ключа и p5Secret (C8) делает юзер.
2. **Сеть матча как в Squad** (M1, M2, M4, затем M3): пакеты по командам, враг не видит ФОБы/маркеры/отряды. Протокол warfare → 6. В игре проверить карту M, деплой и HUD.
3. **core: IP и права** (C5, C6, C10–C13, C15). C6 требует проверить, какой авторизацией ходят моды/лаунчер/боты.
4. **Мёртвый код и надёжность модов** (M5–M8, легаси WARFARE_*).
5. **Боты/шедулер** (B1–B4).
6. **Метрики и дашборд** (C16 + метрики матча из heartbeat).
7. **Лаунчер + подпись HMAC** (C7) — когда будет репозиторий; релиз согласованно с core.
