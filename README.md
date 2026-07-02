# PWP (Pigeo Studios Warfare Project)

Minecraft Forge 1.20.1 мод-комплекс, превращающий Minecraft в тактический шутер (Squad-like).

## Архитектура

```
┌──────────────────────────────────────────────────────┐
│                    ОДИН ПК / VPS                     │
│                                                      │
│  ┌──────────────────────────────────────────────────┐│
│  │              PWP Core Service                    ││
│  │  (отдельное Java-приложение, порт 8080)           ││
│  │  REST API для всех игровых серверов и сайта       ││
│  └────────────────────┬─────────────────────────────┘│
│                       │                              │
│          ┌────────────┼────────────┐                 │
│          ▼            ▼            ▼                 │
│  ┌────────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Лобби      │ │ Матч-1   │ │ Матч-2   │           │
│  │ (порт 25565)│ │(порт 25566)│(порт 25567)│          │
│  └────────────┘ └──────────┘ └──────────┘           │
│                                                      │
│  ┌──────────────────────────────────────────────────┐│
│  │              MySQL (pwp_core)                    ││
│  │  Единственная БД. Данные игроков, статистика,     ││
│  │  валюта, XP, скины, матчи, донаты.               ││
│  └──────────────────────────────────────────────────┘│
│                                                      │
│           Сайт (React) ── HTTPS ──→ Core API         │
└──────────────────────────────────────────────────────┘
```

## Состав проекта

| Модуль | Тип | Назначение |
|--------|-----|-----------|
| `core-service/` | Standalone Java | REST API, MySQL, бизнес-логика |
| `pwp-core-client/` | Forge mod | HTTP клиент к Core Service |
| `pwp-cosmetics/` | Forge mod | Система скинов (ножи, оружие, форма) |
| `pwp-lobby/` | Forge mod | Лобби, голосование, управление матчами |
| `pwp-warfare/` | Forge mod | Игровой режим (AAS) |
| `pwp-medicine/` | Forge mod | Медицина |
| `pwp-movement/` | Forge mod | Движение |
| `pwp-limits/` | Forge mod | Ограничения |

## Запуск

1. **MySQL**: создать БД `pwp_core`, выполнить `core-service/sql/schema.sql`
2. **Core Service**: `cd core-service && gradle run`
3. **Лобби**: запустить Forge сервер с модами `pwp-core-client`, `pwp-cosmetics`, `pwp-lobby`
4. **Матч-сервер**: запускается автоматически лобби из `server-template/`

## API Endpoints

Все запросы требуют заголовок: `Authorization: Bearer <api-key>`

### Игроки
- `POST /api/v1/player/load` — загрузить профиль игрока
- `POST /api/v1/player/create` — создать/обновить игрока
- `POST /api/v1/player/save` — сохранить статистику
- `GET /api/v1/player/{uuid}` — полный профиль (для сайта)
- `GET /api/v1/leaderboard` — топ игроков

### Валюта
- `POST /api/v1/currency/add` — начислить валюту
- `POST /api/v1/currency/spend` — списать валюту (проверка余额)
- `GET /api/v1/currency/{uuid}` — баланс

### XP
- `POST /api/v1/xp/add` — добавить опыт (авто-расчёт уровня)
- `GET /api/v1/xp/{uuid}` — XP, уровень, престиж

### Скины
- `GET /api/v1/cosmetics/{uuid}` — инвентарь скинов
- `POST /api/v1/cosmetics/equip` — экипировать скин (по ролям)
- `POST /api/v1/cosmetics/grant` — выдать скин
- `GET /api/v1/equipment/{uuid}` — текущая экипировка
- `GET /api/v1/shop` — магазин

### Матчи
- `POST /api/v1/match/save` — сохранить результат матча (batch)

### Донат
- `POST /api/v1/donate/process` — обработать донат

## Безопасность
- **Клиент НЕ имеет доступа к БД** — только через серверный API
- Аутентификация по API-ключам (Bearer token)
- Rate limiting: 100 запросов/минуту
- Все операции логируются в таблицу `operation_logs`
