# Правила: bots/ (Discord/Telegram/scheduler)

Часть правил PWP. Полный индекс — корневой `AGENTS.md`.
**Перед изменением кода этого модуля/домена — прочитай этот файл; после изменений (новый модуль/фича, инцидент, фикс, изменённые правила сборки) — обнови его**: текст на русском, метки дат `(дд.мм.2026)`, удаляй устаревшее, если описание разошлось с кодом, секреты/токены не добавлять.

---

| `bots/` | Discord-бот (`discord_bot.py`) + Telegram-бот (`telegram_bot.py`) + `scheduler_loop.py`, общий `shared_state`, `client.py` (клиент к core-service API). Запуск: `py run.py`. Токены в `config.py` (gitignored). **Ссылка на лаунчер в TG-боте (GameScreen._launcher) НЕ берётся из БД** (таблица `launcher_versions` заполняется только вручную — устаревает): источник истины — `launcher-version.json` в Pwpfiles (raw.githubusercontent, его же читает сам лаунчер для автообновления), фолбэк — `https://github.com/Pigeo-Studios/Pwpfiles/releases/latest` (вечный редирект на новейший релиз). При добавлении новых релизов ничего править не нужно |
