# PWP Core Service

Центральный сервис для PWP. Отвечает за все данные: игроки, статистика, валюта, XP, скины, матчи, донаты.

## Технологии
- Java 17
- Javalin (HTTP сервер)
- HikariCP (пул соединений MySQL)
- Gson (JSON)
- Logback (логирование)

## Запуск

```bash
# 1. Убедись, что MySQL запущен и БД pwp_core создана
# 2. Выполни schema.sql для создания таблиц
# 3. Настрой config.json (особенно database.password)
# 4. Запусти:
gradle run
# Или:
java -jar build/libs/core-service.jar config.json
```

## Конфигурация (config.json)

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

## Структура

```
src/main/java/com/pwp/core/
├── CoreApplication.java      — точка входа
├── api/                       — REST контроллеры
│   ├── PlayerController.java
│   ├── CurrencyController.java
│   ├── XpController.java
│   ├── CosmeticsController.java
│   ├── MatchController.java
│   ├── ShopController.java
│   └── DonationController.java
├── db/                        — работа с MySQL
│   ├── DatabaseManager.java   — HikariCP пул
│   ├── PlayerRepository.java
│   ├── CurrencyRepository.java
│   ├── XpRepository.java
│   ├── CosmeticsRepository.java
│   ├── MatchRepository.java
│   ├── ShopRepository.java
│   ├── DonationRepository.java
│   └── LogRepository.java
├── model/                     — классы данных
│   ├── Player.java, PlayerStats.java, PlayerProfile.java
│   ├── CosmeticItem.java, EquipmentSlot.java
│   ├── MatchResult.java, MatchPlayer.java
│   ├── ShopItem.java, DonationTransaction.java
│   ├── OperationLog.java
│   └── ApiResponse.java
└── auth/
    └── AuthMiddleware.java    — проверка API ключей
```
