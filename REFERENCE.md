# PWP — Полный справочник проекта

## 1. Архитектура

```
  Игрок (Minecraft Forge Client)
         │
         ▼
  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
  │   Лобби      │     │  Матч-сервер │     │  Матч-сервер │
  │  (порт 25565)│     │ (порт 25566) │     │ (порт 25567) │
  └──────┬───────┘     └──────┬───────┘     └──────┬───────┘
         │                    │                    │
         └──────────┬─────────┴─────────┬──────────┘
                    │ HTTP (Bearer auth)
                    ▼
           ┌──────────────┐
           │ Core Service │  ← отдельное Java-приложение
           │  (порт 8080) │
           └──────┬───────┘
                  │ JDBC (HikariCP)
                  ▼
           ┌──────────────┐
           │    MySQL     │
           │  pwp_core    │
           └──────────────┘
                  ▲
                  │ HTTPS
           ┌──────┴──────┐
           │    Сайт     │
           │  (React/?)  │
           └─────────────┘
```

## 2. Модули (папки)

| Папка | Тип | Язык | Назначение |
|-------|-----|------|-----------|
| `core-service/` | Standalone | Java 17 | REST API сервер, бизнес-логика, MySQL |
| `pwp-core-client/` | Forge mod | Java 17 | HTTP клиент к Core Service, кеш игроков, переключение серверов |
| `pwp-cosmetics/` | Forge mod | Java 17 | Система скинов (ножи, оружие, форма, эффекты) |
| `pwp-lobby/` | Forge mod | Java 17 | Лобби, голосование, управление матч-серверами |
| `pwp-warfare/` | Forge mod | Java 17 | Игровой режим AAS (существующий) |
| `pwp-medicine/` | Forge mod | Java 17 | Медицина (существующий, без изменений) |
| `pwp-movement/` | Forge mod | Java 17 | Движение (существующий, без изменений) |
| `pwp-limits/` | Forge mod | Java 17 | Ограничения (существующий, без изменений) |
| `server-template/` | Шаблон | — | Копируется для каждого нового матч-сервера |

## 3. Core Service (core-service/)

### Запуск
```bash
cd core-service
gradle run
# Или:
java -jar build/libs/core-service.jar config.json
```

### Конфиг (config.json)
```json
{
    "server": { "port": 8080, "host": "0.0.0.0" },
    "database": {
        "host": "127.0.0.1", "port": 3306, "name": "pwp_core",
        "user": "root", "password": "",
        "poolSize": 10, "maxLifetimeMs": 1800000
    },
    "api": {
        "keys": ["pwp_server_key_change_me"],
        "rateLimitPerMinute": 100
    }
}
```

### Структура файлов
```
core-service/src/main/java/com/pwp/core/
├── CoreApplication.java      ← main(), загрузка конфига, старт Javalin
├── api/                      ← REST контроллеры
│   ├── PlayerController.java      /api/v1/player/*
│   ├── CurrencyController.java    /api/v1/currency/*
│   ├── XpController.java          /api/v1/xp/*
│   ├── CosmeticsController.java   /api/v1/cosmetics/*
│   ├── MatchController.java       /api/v1/match/*
│   ├── ShopController.java        /api/v1/shop
│   └── DonationController.java    /api/v1/donate/*
├── db/                       ← Работа с MySQL
│   ├── DatabaseManager.java       HikariCP пул
│   ├── PlayerRepository.java
│   ├── CurrencyRepository.java
│   ├── XpRepository.java          Авторасчёт уровня (level * 1000 XP)
│   ├── CosmeticsRepository.java
│   ├── MatchRepository.java       Batch-сохранение матча в 1 транзакции
│   ├── ShopRepository.java
│   ├── DonationRepository.java
│   └── LogRepository.java
├── model/                    ← POJO классы
│   ├── Player.java, PlayerStats.java, PlayerProfile.java
│   ├── CosmeticItem.java, EquipmentSlot.java
│   ├── MatchResult.java, MatchPlayer.java
│   ├── ShopItem.java, DonationTransaction.java, OperationLog.java
│   └── ApiResponse.java          { success, error, data }
└── auth/
    └── AuthMiddleware.java        Bearer token проверка
```

### API эндпоинты

Требуют заголовок: `Authorization: Bearer <api-key>`

#### Здоровье
```
GET /api/v1/health → { "status": "ok" }
```

#### Игроки
```
POST /api/v1/player/load       { uuid } → PlayerProfile
POST /api/v1/player/create     { uuid, nickname } → ok
POST /api/v1/player/save       { uuid, stats } → ok
GET  /api/v1/player/{uuid}     → PlayerProfile (для сайта)
GET  /api/v1/leaderboard       ?orderBy=kills&page=1&limit=50 → топ
POST /api/v1/player/ban        { uuid, reason }
```

#### Валюта
```
POST /api/v1/currency/add      { uuid, amount, reason } → { balance }
POST /api/v1/currency/spend    { uuid, amount, itemId } → { balance } | error
GET  /api/v1/currency/{uuid}   → { balance, totalEarned, totalSpent }
```

#### XP / уровни
```
POST /api/v1/xp/add            { uuid, amount, reason } → { xp, level, leveledUp }
GET  /api/v1/xp/{uuid}         → { xp, level, prestige }
```

Формула уровня: `level * 1000 XP`. При достижении — `leveledUp: true`.

#### Скины / косметика
```
GET    /api/v1/cosmetics/{uuid}   → [CosmeticItem, ...]
POST   /api/v1/cosmetics/equip    { uuid, itemUuid, slotType, role } → ok
POST   /api/v1/cosmetics/grant    { uuid, skinId, source } → CosmeticItem
GET    /api/v1/equipment/{uuid}   → [EquipmentSlot, ...]
GET    /api/v1/shop               → [ShopItem, ...]
```

#### Матчи
```
POST /api/v1/match/save        MatchResult → { matchId }
```

MatchResult:
```json
{
    "mapName": "fools_road",
    "mode": "AAS",
    "teamBlueScore": 500,
    "teamRedScore": 0,
    "winner": "BLUE",
    "durationSeconds": 1800,
    "startedAt": "2026-07-02 14:00:00",
    "endedAt": "2026-07-02 14:30:00",
    "players": [
        { "uuid": "...", "team": "BLUE", "kills": 15, "deaths": 5,
          "assists": 3, "score": 1200, "vehicleKills": 2,
          "captures": 1, "revives": 0, "shotsFired": 120,
          "shotsHit": 45, "damageDealt": 1500.5,
          "healingDone": 0, "suppliesDelivered": 0,
          "longestKill": 85.3, "role": "Medic", "squadId": 1,
          "wasSquadLeader": false }
    ]
}
```

#### Донат
```
POST /api/v1/donate/process   { uuid, itemId, amount, currency, paymentId, signature } → { transactionId, itemUuid }
```

## 4. Таблицы MySQL (12 групп)

| # | Таблица | Назначение |
|---|---------|-----------|
| 1 | `players` | UUID, nickname, донат-тир, роль, бан |
| 2 | `player_stats` | kills, deaths, assists, headshots, vehicleKills, captures, revives, healingDone, suppliesDelivered, timeAsSL/CMD, 25 полей |
| 3 | `player_currency` | coins, totalEarned, totalSpent |
| 4 | `player_xp` | xp, level, prestige |
| 5 | `rank_definitions` + `player_ranks` | 20 рангов (Recruit→Marshal), XP пороги, открытые киты |
| 6 | `vehicle_unlocks` + `player_vehicle_unlocks` | Техника, требования по рангу/XP/монетам |
| 7 | `kit_unlocks` | Какие киты на каком ранге открываются |
| 8 | `achievements` + `player_achievements` | 20+ ачивок, прогресс, награды |
| 9 | `seasons` + `player_season_progress` | Боевой пропуск, уровни, free/premium |
| 10 | `match_history` + `match_players` + `combat_log` | Матчи, игроки в матче, каждое убийство |
| 11 | `player_cosmetics` + `player_equipment` + `shop_items` | Скины, экипировка по ролям, магазин |
| 12 | `operation_logs` + `donation_transactions` + `api_keys` | Логирование, донаты, ключи API |

## 5. Forge моды

### 5.1 pwp-core-client

**Назначение:** HTTP клиент к Core Service + общие утилиты.

**Файлы:**
```
pwp-core-client/src/main/java/com/pwp/coreclient/
├── CoreClientMod.java          ← @Mod("pwp_core_client")
├── CoreAPI.java                ← Статический фасад: CoreAPI.saveMatch(), CoreAPI.addXp(), ...
├── PlayerData.java             ← Кеш профилей игроков (Map<UUID, CachedProfile>)
├── CoreApiClient.java          ← Низкоуровневый HTTP клиент
└── network/
    └── ConnectToServerPacket.java  ← Пакет: "подключись к другому серверу"
```

**CoreAPI методы (безопасные — возвращают null при ошибке):**
```java
CoreAPI.loadPlayer(uuid)
CoreAPI.createPlayer(uuid, nickname)
CoreAPI.saveStats(uuid, stats)
CoreAPI.addCurrency(uuid, amount, reason)
CoreAPI.spendCurrency(uuid, amount, itemId)
CoreAPI.addXp(uuid, amount, reason)
CoreAPI.saveMatch(matchData)
CoreAPI.grantItem(uuid, skinId, source)
CoreAPI.equipItem(uuid, itemUuid, slotType, role)
```

**ConnectToServerPacket:**
- Сервер шлёт клиенту этот пакет
- Клиент открывает экран подключения к новому IP:Port
- Используется: лобби → матч, матч → лобби

### 5.2 pwp-cosmetics

**Назначение:** Скины для ножей, оружия, формы, эффектов.

**Файлы:**
```
pwp-cosmetics/src/main/java/com/pwp/cosmetics/
├── CosmeticsMod.java           ← @Mod("pwp_cosmetics")
├── SkinRegistry.java           ← Реестр всех скинов (id, name, slot, rarity, modelPath)
├── CosmeticManager.java        ← applySkin(player, item, slotType) — накладывает скин
└── gui/
    └── CosmeticsScreen.java
```

**Слоты скинов:** KNIFE, PRIMARY, SECONDARY, UNIFORM, HEADGEAR, PATCH, EFFECT, VOICE, ANIMATION

**Редкости:** COMMON→RARE→EPIC→LEGENDARY→MYTHIC

**Экипировка по ролям:** Медик → красный нож, Снайпер → чёрный нож и т.д.

### 5.3 pwp-lobby

**Назначение:** Сервер-лобби, голосование, управление матчами.

**Файлы:**
```
pwp-lobby/src/main/java/com/pwp/lobby/
├── LobbyMod.java               ← @Mod("pwp_lobby")
├── ServerManager.java          ← Запуск/остановка матч-серверов (ProcessBuilder)
├── VotingManager.java          ← Голосование за карту
├── maps/
│   ├── MapConfig.java          ← POJO: name, maxPlayers, spawns, capturePoints, ...
│   └── MapRegistry.java        ← Загрузка JSON карт из server-template/maps/
├── match/
│   └── MatchAllocator.java     ← Очередь, 80% fill check, запуск матча
└── gui/
    ├── VotingScreen.java
    └── ShopScreen.java
```

**Логика MatchAllocator:**
1. Игроки заходят в лобби → `playerJoined(uuid)`
2. Игроки голосуют за карту
3. Голосование завершено → `enqueuePlayer()`
4. `tryAllocate()` проверяет:
   - Есть ли уже матч с < 80% заполнением? → добавить туда
   - Есть ли уже матч на этой карте? → ждать
   - Хватает ли игроков (minPlayers)? → запустить новый сервер
5. `startMatch(map)` → ServerManager копирует шаблон, правит порт, запускает Forge

### 5.4 pwp-warfare (изменения)

**Добавлено в существующий код:**
```
pwp-warfare/src/main/java/com/pigeostudios/pwp/warfare/stats/
├── MatchStatsTracker.java     ← Трекер матча (в памяти)
└── PlayerMatchStats.java      ← POJO статистики одного игрока за матч
```

**Точки интеграции:**
```
GameLogicEvents.java:
  └── onEntityDeath()        → MatchStatsTracker.recordKill() / recordTeamKill()
  └── executeVictory()       → MatchStatsTracker.finalizeMatch(winner, blueScore, redScore)
  └── countdown end          → MatchStatsTracker.startMatch(mapName, "AAS")

ResupplyHandler.java:
  └── applyKitToPlayer()     → CosmeticManager.applySkin() (TODO)
```

## 6. Дизайн-система (pwp-core-client)

```
pwp-core-client/src/main/java/com/pwp/coreclient/gui/
├── theme/
│   └── PWPTheme.java         ← 50+ цветов, шрифты, размеры, тени, иконки, стили
└── animations/
    └── Easing.java           ← 15 функций плавности
```

**Цветовая схема "Dark Ember":**
```
Фон:    #0A0C0E (почти чёрный)
Карты:  #12151A
Акцент: #C8812A (приглушённый оранжевый)
Текст:  #C8CBCE (мягкий серо-белый)
Команды: #3D6FA5 (синий) / #A53D3D (красный)
```

**PWPTheme.Styles содержит:**
- Button.PRIMARY / ACCENT / DANGER / DARK
- Input, Panel, Progress, Toast, RarityHighlight

## 7. Progression system

### XP начисление
```
Kill:       +50 XP  +10 Coins
Assist:     +25 XP  +5 Coins
Capture:    +100 XP +25 Coins
Revive:     +75 XP  +15 Coins
Vehicle kill: +150 XP +30 Coins
Win:        +200 XP +50 Coins
Per minute: +10 XP  +1 Coin
```

### Ранги (20 штук)
```
1:  Recruit      0 XP          — Rifleman
5:  Corporal     5,000 XP      — LMG, Marksman
10: First Sergeant 35,000 XP   — Sniper
15: First Lieutenant 100,000 XP — Commander
20: Marshal      300,000 XP    — всё открыто
```

### Формула уровня
`level * 1000 XP` для следующего уровня. Престиж после 100.

## 8. Пул матч-серверов

**Шаблон:** `server-template/` — чистая папка Forge сервера без мира.

**При запуске матча:**
1. `server-template/` копируется в `match_01/`
2. `server.properties` — заменяются `${PORT}`, `${LEVEL}`, `${MAX_PLAYERS}`
3. Запускается `java -jar forge.jar nogui`
4. Лобби ждёт пока сервер ответит на ping
5. Игроки получают `ConnectToServerPacket(ip, port)`

**Конфиги карт:** `server-template/maps/*.json`
```json
{
    "name": "fools_road",
    "maxPlayers": 100,
    "minPlayers": 10,
    "image": "map1",
    "BLUE": { "faction": "usa", "tickets": 800 },
    "RED": { "faction": "russia", "tickets": 800 },
    "spawns": [ { "team": "BLUE", "x": 500, "z": 500 } ],
    "capturePoints": [ { "name": "Village", "x": 50, "z": 50, "radius": 30 } ],
    "mapBounds": { "centerX": 0, "centerZ": 0, "sizeBlocks": 2048 }
}
```

## 9. Поток игрока

```
1. Запуск Minecraft → подключение к Лобби (IP:25565)
2. Лобби: игроки бегают, видят друг друга, статистика
3. Голосование: выбор карты (30 сек)
4. MatchAllocator: проверяет свободный сервер или запускает новый
5. ConnectToServerPacket → игрок на матч-сервере
6. Матч: AAS, захват точек, убийства, ревивы
7. MatchStatsTracker: все действия в памяти
8. Матч закончен → executeVictory() → finalizeMatch() → CoreAPI.saveMatch()
9. Core Service: INSERT match_history + match_players (одна транзакция)
10. ConnectToServerPacket → игрок обратно в лобби
11. Лобби: обновлённая статистика, XP, Coins
```

## 10. Безопасность

| Что | Как |
|-----|-----|
| API ключи | Bearer token в заголовке |
| Клиент→БД | Никогда. Только через Core Service |
| Сайт→БД | Никогда. Только через Core Service API |
| Rate limit | 100 запросов/минуту (заглушка, реализовать в AuthMiddleware) |
| Валидация | Сервер проверяет баланс перед списанием |
| Логирование | Все операции в operation_logs |
