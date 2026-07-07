# PWP — Полная документация системы

## Архитектура

```
ТГ бот ─HTTP──▶ Core Service (Java) ──▶ MySQL (pwp_core)
                     ▲
  Лаунчер ───────────┘
                     │
              Game Servers (Forge)
```

---

## 1. Telegram Bot (`bots/`)

**Язык:** Python 3.14  
**Фреймворк:** python-telegram-bot 21.10  
**Запуск:** `py bots\run.py`

### Файлы

| Файл | Строк | Назначение |
|------|-------|-----------|
| `telegram_bot.py` | 74 | Точка входа TG бота. Регистрирует хендлеры + файловые команды |
| `discord_bot.py` | 53 | Discord бот (только отправка + дамп каналов) |
| `auth_handlers.py` | 830 | **Вся логика:** политика, регистрация, профиль, пароль, 2FA, админка |
| `config.py` | 23 | Чтение .env, настройки |
| `client.py` | 56 | CLI для отправки сообщений через ботов (для админа / ИИ) |
| `run.py` | 25 | Запуск обоих ботов одновременно |
| `.env` | 5 | Токены (не в git) |
| `requirements.txt` | 3 | Зависимости |

### Команды TG

| Команда | Описание |
|---------|---------|
| `/start` | Проверка → политика приватности → главное меню |
| `/register` | Ручной запуск регистрации |
| `/help` | Список команд |
| `/ping` | pong |
| `/myid` | Показать Telegram ID |

### Главное меню (кнопки)

```
👤 Профиль      🔑 Пароль
🛡 2FA          📥 Лаунчер
⚙️ Админ-панель (только admin/ADMIN_IDS)
```

### CLI (client.py)

```
py bots\client.py send tg <chat_id> <text>   — отправить в TG
py bots\client.py send ds <channel_id> <text> — отправить в Discord
py bots\client.py channels                    — список каналов Discord
```

Бот читает файлы из `bots/commands/` — client.py пишет туда JSON, бот забирает и отправляет.

### Настройка (.env)

```
TELEGRAM_TOKEN=...               # Токен TG бота
DISCORD_TOKEN=...                # Токен Discord бота
CORE_API_URL=http://127.0.0.1:8080  # Адрес Core Service
CORE_API_KEY=pwp_server_key_change_me # API ключ
ADMIN_IDS=1048950532             # Telegram ID админов (через запятую)
```

---

## 2. Core Service (`core-service/`)

**Язык:** Java 17  
**Фреймворк:** Javalin 6.4 + HikariCP + MySQL  
**Сборка:** `gradlew shadowJar`  
**Запуск:** `java -jar build/libs/core-service-1.0.0.jar config.json`  
**Порт:** 8080  
**Аутентификация:** Bearer token в заголовке `Authorization`

### Модели (`model/`)

| Файл | Поля |
|------|------|
| `Account.java` | id, uuid, login, email, passwordHash(bcrypt), telegramId, role, hwid, privacyPolicyAccepted, launcher2faEnabled, isBanned, banReason, lastIp, lastLogin, registeredAt |
| `LoginSession.java` | id, accountId, token, ip, createdAt, expiresAt |
| `TwoFaCode.java` | id, accountId, code(6 digits), ip, createdAt, expiresAt, used |
| `PasswordReset.java` | id, accountId, adminId, status(pending/approved/rejected), createdAt, resolvedAt |
| `Player.java` | Существующая модель игрока (Minecraft UUID) |
| `ApiResponse.java` | {success, error, data} — стандартный ответ |

### Репозитории (`db/`)

| Файл | Назначение |
|------|-----------|
| `AccountRepository.java` | Все операции с аккаунтами: CRUD, сессии, 2FA коды, IP белый список, сброс пароля, логи, рассылка |

### API Эндпоинты

#### Публичные (Auth)

| Метод | Путь | Описание |
|-------|------|---------|
| GET | `/api/v1/health` | Health check (без auth) |
| GET | `/api/v1/auth/check-login?login=x` | Проверка свободен ли логин |
| GET | `/api/v1/auth/check-email?email=x` | Проверка свободен ли email |
| POST | `/api/v1/auth/register` | Регистрация: {login, email, password, telegramId} |
| POST | `/api/v1/auth/accept-privacy` | Принять политику: {accountId} |
| POST | `/api/v1/auth/login` | Вход: {login, password} → token или 2fa_required |
| POST | `/api/v1/auth/verify-2fa` | Подтвердить 2FA: {accountId, code} → token |
| POST | `/api/v1/auth/confirm-login` | Подтвердить IP из TG: {telegramId, ip} |
| GET | `/api/v1/auth/profile?account_id=X` | Профиль |
| GET | `/api/v1/auth/profile-by-tg?telegram_id=X` | Профиль по TG ID |
| POST | `/api/v1/auth/change-password` | Смена: {accountId, oldPassword, newPassword} |
| POST | `/api/v1/auth/forgot-password` | Запрос сброса: {accountId} → resetId |
| POST | `/api/v1/auth/set-password-after-reset` | Новый пароль: {accountId, newPassword} |
| POST | `/api/v1/auth/toggle-2fa` | {accountId, enabled} |
| POST | `/api/v1/auth/send-2fa` | Сгенерировать 2FA код: {accountId} |
| GET | `/api/v1/auth/validate-session?token=X` | Проверка сессии (для игровых серверов) |

#### Админские

| Метод | Путь | Описание |
|-------|------|---------|
| POST | `/api/v1/admin/find-user` | Поиск по логину/UUID/TG/email: {query} |
| POST | `/api/v1/admin/ban` | {accountId, reason} |
| POST | `/api/v1/admin/unban` | {accountId} |
| POST | `/api/v1/admin/set-role` | {accountId, role} — user/support/admin/owner |
| GET | `/api/v1/admin/pending-resets` | Список заявок на сброс |
| POST | `/api/v1/admin/resolve-reset` | {resetId, adminId, status} |
| GET | `/api/v1/admin/logs?limit=20&offset=0` | Логи действий |
| POST | `/api/v1/admin/broadcast` | {adminId, message} — рассылка всем в TG |
| GET | `/api/v1/admin/stats` | Статистика |

### База данных

**MySQL 8.0+**, БД: `pwp_core`

#### Таблицы (новые, account system)

| Таблица | Назначение |
|---------|-----------|
| `accounts` | Аккаунты пользователей: uuid, login, email, password_hash(bcrypt), telegram_id, role, hwid, privacy flags, 2fa, ban, ip, даты |
| `trusted_ips` | Белый список IP для 2FA (account_id + ip) |
| `sessions` | Сессии входа: token, ip, expires_at |
| `twofa_codes` | Коды 2FA (6 цифр, 5 мин срок) |
| `password_resets` | Заявки на сброс пароля (pending → approved/rejected) |
| `account_logs` | Аудит действий |
| `broadcast_log` | История рассылок |

#### Миграция

`core-service/sql/migration_add_accounts.sql` — 7 таблиц + ALTER players ADD telegram_id

---

## 3. Потоки пользователя

### Регистрация

```
/start → политика → Принять → ввод логина → проверка API → ввод email → 
проверка API → ввод пароля (8+ символов, загл. буква, цифра) → 
подтверждение пароля → POST /auth/register → POST /auth/accept-privacy → 
главное меню
```

### Вход в лаунчер

```
Логин + пароль → POST /auth/login

[2FA выкл]      → сразу token → сессия 24ч
[2FA вкл + IP доверенный] → сразу token
[2FA вкл + IP новый] → 2fa_required → код в TG → 
  кнопки [✅ Да, это я] [❌ Нет] → POST /auth/confirm-login → 
  POST /auth/verify-2fa → token
```

### Сброс пароля

```
/forgotpassword → POST /auth/forgot-password → 
  админу уведомление с [✅ Одобрить] [❌ Отклонить] →
  админ одобряет → POST /admin/resolve-reset →
  пользователю: "введите новый пароль" →
  POST /auth/set-password-after-reset
```

### ADMIN_IDS bypass

Если Telegram ID в `ADMIN_IDS`:
- Пропускает политику приватности
- Пропускает регистрацию
- Роль: admin
- Если нет аккаунта в БД → авто-создание с ролью admin

---

## 4. Архитектура 2FA

```
Launcher          Core Service              TG Bot           User
  │                    │                      │                │
  │ POST /login ──────▶│                      │                │
  │◀─ {2fa:true,       │                      │                │
  │    account_id}      │                      │                │
  │                    │ POST /send-2fa ──────▶│                │
  │                    │   (через API-key)     │── 📨 код ─────▶│
  │                    │                      │◀─ ✅ Да ──────│
  │                    │◀─ /confirm-login ─────│                │
  │                    │   (IP в trusted_ips)  │                │
  │ POST /verify-2fa ─▶│                      │                │
  │◀─ {token} ────────│                      │                │
```

---

## 5. Minecraft моды (что менять)

### pwp-core-client — нужно добавить (2 файла)

1. **`network/AuthTokenPacket.java`** (C2S): передаёт session token при входе на сервер
2. **`CoreClientMod.java`**: читать `--authToken` из JVM args (лаунчер передаёт)

Серверная часть: при получении пакета вызывает `GET /auth/validate-session?token=X`. Если невалиден → кик.

---

## 6. Лаунчер (что менять)

`.NET 8 WPF` — `launcher/PWP Launcher/`

Нужно добавить:
- Экран логина (login + password)
- Если 2FA — поле для 6-значного кода
- Сохранять session token
- Передавать `--authToken <token>` при запуске Minecraft

---

## 7. Безопасность

- **Пароли**: bcrypt (12 раундов), никто не читает
- **Токены**: в `.env` (в .gitignore)
- **API ключи**: `rateLimitBypassKeys` для админского ключа
- **Сессии**: случайный 128-символьный токен, 24ч жизни
- **2FA**: 6-значный код, 5 мин срок, одноразовый
- **IP контроль**: незнакомый IP → 2FA или уведомление
