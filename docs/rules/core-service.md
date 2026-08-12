# Правила: core-service

Часть правил PWP. Полный индекс — корневой `AGENTS.md`.
**Перед изменением кода этого модуля/домена — прочитай этот файл; после изменений (новый модуль/фича, инцидент, фикс, изменённые правила сборки) — обнови его**: текст на русском, метки дат `(дд.мм.2026)`, удаляй устаревшее, если описание разошлось с кодом, секреты/токены не добавлять.

---

Пакет `com.pwp.core`: `api/` (контроллеры Javalin) → `db/` (репозитории MySQL) → `model/` → `auth/` (`AuthMiddleware`). Контроллеры: Auth (authlib-injector + Yggdrasil), AuthLib, Admin, Case (кейсы/лутбоксы), Cosmetics, Currency, Donation, FactionVehicle, HWIDBan, Kit, Launcher (манифест для лаунчера), Match, MatchPolicy, Network, Player, Rank, Reward, Security (2FA), Shop, Skin/SkinV2, VoiceMute, Xp.

## Донат-тиры (NONE/SILVER/GOLD/PLATINUM) (11.08.2026)

Косметические тиры (не P2W): выдача только админом через веб-админку, не покупаются.
- `PlayerRepository.setDonateTier(uuid, tier)` — запись `donate_tier` (после `setRole`).
- `POST /api/v1/admin/set-donate-tier` (`AdminController`): тело `SetDonateTierReq` (`uuid`, `tier` из `NONE/SILVER/GOLD/PLATINUM`, валидация через `List.of(...)`); `NONE` → `setDonateTier(uuid, null)`. Требует админ-токен (`verifyAdmin`).
- Профиль игрока содержит `donateTier` (в `data.player`): отдаётся на `/api/v1/player/{uuid}`, `/player/load`, `/admin/find-user` (поле `donate_tier`) и `/admin/players` (SELECT + мапа). `null` — тира нет.
- Лидерборд `/api/v1/leaderboard` (с 12.08.2026): `PlayerRepository.getLeaderboard` выбирает и маппит `p.role` + `p.donate_tier` в каждый `player` — клиент рендерит градиентные ники/свечение донатеров в лидербордах (`LeaderTabRenderer`/`StatsScreen`). До фикса поля не выбирались — в лидербордах у донатеров был дефолтный белый ник.
- Лобби подтягивает тир+role из `data.player` профиля (см. `pwp-lobby`), боевых бонусов не даёт.
