# PWP — ROADMAP (единственный живой план)

Ветка работы — `rewrite/unified` (одна; старые ветки — теги `archive/*`). `main` — прежний снимок, сливать только по слову
владельца. Подробный журнал этапов 0–17 с обоснованиями — в git: `git show 46f5b41:docs/ROADMAP.md`; таблица ревью —
`git show 46f5b41:docs/REVIEW.md`. Сверка со Squad — `docs/SQUAD_PARITY.md`, что проверить в игре — `docs/TEST_CHECKLIST.md`.

Проверка каждого шага: `gradlew build` (тесты + spotless) зелёный, `py -3 tools/check_reobf.py` OK, `py -3 tools/pwp_doctor.py`
без ошибок, для core — `core-service/gradlew test shadowJar`; коммит. **В игре ничего не проверялось** — проверяет владелец.

## Где остановился и следующий шаг
«Продолжи по docs/ROADMAP.md» — сверху вниз, каждый пункт — свой коммит.
1. **16c — весь контент в `content/`.** Первый шаг: `tools/pwp_content.py export` — из `maps/kits/*.json`, `maps/vehicles/*.sql`
   и генераторов дивизий в TOML (`content/factions/<ф>/kits/*.toml`, `vehicles.toml`, `divisions/*.toml`) с ТОЧНЫМ обратным
   ходом (`build` → те же SQL/JSON, тест на совпадение байт); потом `push`/`pull` через admin API core. Проверку ссылок
   переиспользовать из `pwp_doctor.py`.
2. **16b — площадки техники вместо невидимого блока** (позиция, курс, размер S/M/L/HELI; проверка «площадка занята»; таймеры
   своей техники на карте и в деплое; миграция блока в площадку). Рискованно без игры — после прохода `TEST_CHECKLIST` E–K.

## Состояние (✅ сделано в коде)
| Этап | Что | Где |
|---|---|---|
| 1–3 | 3 jar вместо 12 (`pwp-core`, `pwp-game`, `sbwchunkload`), один конфиг `config/pwp-server.json` | AGENTS.md «Структура» |
| 4 | Цикл матча сам: лобби → голосование → матч → итоги → лобби; сторож пустых/бесконечных матчей | `MatchWatchdog` |
| 5 | Обучение новичка | `Tutorial`, `TutorialClient` |
| 6 | Дивизии фракций + редактор в F6 | `Division`, `DivisionPlan`, `DivisionAdmin` |
| 7, 15 | Режимы: AAS, Invasion, RAAS, «Стычка», «Стрельбище» | `GameModes`, `RaasChain` |
| 8 | Магазин за Coins | `ShopData`, core `/api/v1/shop/buy` |
| 10 | Публичный сервер: жалобы, AFK, тимкиллы, чат, сидинг, очередь, VIP-слоты, закрепление техники, Discord, бэкап БД | `community` в `pwp-server.json` |
| 11 | «Меню PWP», скины стволов | `PwpHub`, `WeaponSkinService` |
| 12 | Карта без утечек врагу и наложений | `TeamVisibility`, `LabelPlacer` |
| 13–14 | Разбор бог-классов, сайт и веб-модерация | `core-service/.../site` |
| 15 | Табло, командир, выносливость | `ScoreboardData`, `CommanderAbilities`, `Stamina` |
| ревью | 4 бага, правила AGENTS, −45 мёртвых методов, копипаст | `git log --grep=cleanup` |
| 16a | Спавнер техники невидим, столкновение как у барьера, таймер только своим | `VehicleSpawnerBlock` |
| 17a | Радиалка техники J: закрепить, «только мой отряд», запрос и разрешение вождения | `VehicleActions`, `VehicleActionService` |
| 17b | Новости: core + сайт + веб-модерация + экран в игре (лаунчеру — `GET /api/v1/public/news`) | `NewsController`, `PWPNewsScreen` |
| 17c | Стройка: каталог, разделы, цены, лимиты у ФОБа, запрет в мейне врага, HUD «ФОБ: N» | `build/Structure`, `BuildRadialScreen` |
| 17d | Тактическая радиалка Y (коммо-роза): враг здесь, запросы, приказы, стройка, техника, командир | `TacticalRose`, `TacticalRadialScreen` |
| 17e | Тревога «ФОБ подкапывает враг» | `HubBlockEntity.alertDigging` |
| 17f | Выносливость проверяет сервер | `StaminaGuard`, `StaminaServer` |
| 17g | Подавление | `Suppression`, `SuppressionClient` |
| 17h | Ремонт в поле — SBW `repair_tool` уже в китах экипажа и саперов | `maps/kits` |
| опт. | Тики/сеть/БД: игроки карты кодируются раз на команду (было — на каждого игрока 10 раз в секунду); индекс `players.nickname`; кэш prepared statements в пуле; **баг:** место игрока в топе было всегда 1 | `GameLogicEvents`, `DatabaseManager`, `PlayerRepository.rankSql` |
| 17i–j | Одна подсказка у прицела (техника, ФОБ с лопатой и без, враг, ящик, раненый…) и шпаргалка H | `LookPrompt`, `LookPromptHud`, `KeyHelpOverlay` |

Каналы: `pwp_core_client:network` = 9, `pwpwarfare:main` = 11.

## Риски, которые не трогали (и почему)
- CAS/сброс командира можно вызвать на мейн врага — вопрос баланса, решает владелец.
- Кулдауны командира не сбрасываются, если матч перезапускают на том же сервере (обычно матч = новый сервер).
- Очередь в полный матч может пропустить на 1–2 игрока больше до следующего heartbeat — страхует `ReservedSlots`.
- Декомпозиция `GameLogicEvents.onServerTick`, `ModCommands.register`, `LobbyMod`, `DeployScreen`, `SquadMapRenderer` — после
  проверки в игре: порядок действий тика и UI проверяются только глазами.

## Решения (принятые без владельца)
- Частота рассылки игроков карты (10 раз в секунду) и тик защиты мейна (каждый тик, таймер 200 тиков) не менялись: это поведение.
  Если замер MSPT/трафика (`LAUNCH_PLAN.md`, п. 6) покажет нагрузку — первое, что снижать: карта до 4–5 раз в секунду.
- Неиспользуемые ключи lang удаляются только с доказательством grep литерала и динамических префиксов.
- UI-кит `com.pwp.coreclient.ui` не урезается (API только растёт); `Easing` — цельная библиотека, оставлена.
- Лимит 8 мин миномёта не включали, а удалили: он не был подписан никогда, включение — новое поведение.
- Лидер отряда строит без рации в руке (как в Squad); рация тоже работает.
- Своего ремонтного ключа нет: дубль SBW `repair_tool`.
- `core-service/gradle.properties` — JDK этой машины, как в корне; на другой — `-Dorg.gradle.java.home=…`.

## Ждёт ок владельца
- Удаление старых веток на GitHub (`rewrite/stage-1` … `review/cleanup`) — их концы уже в тегах `archive/*`.
- Таблицы и данные БД не удалялись (только новые: `news`, `faction_divisions`, `player_reports`, `discord_links`).

## Сделать владельцу (вне репо)
- **Деплой:** `tools\pwp-server.ps1 -Build -Server <лобби>,<match_template>` — 3 jar на лобби, шаблон матча и клиентов. Протокол
  warfare = 11: старый клиент не войдёт — обновить всех через лаунчер.
- **core-service:** пересобрать (`core-service\gradlew shadowJar`) — таблицы создадутся сами.
- **Pwpfiles/mods:** убрать старые `pwp-*.jar` (api, core-client, core-server, lobby, cosmetics, warfare, medicine, movement,
  limits, blast-protection, drone), положить `pwp-core`, `pwp-game` → `gen.ps1` → подпись манифеста.
- **Лаунчер:** удалять из `mods/` jar, которых нет в манифесте (иначе дубли modid → краш); ленту новостей брать из
  `GET /api/v1/public/news?limit=` (поля `title`, `body`, `tag`, `imageUrl`, `pinned`, `createdAt`).
- **Боты:** в `bots/config.py` задать `TG_REPORTS_CHAT_ID`, `DISCORD_REPORTS_CHANNEL_ID` (по желанию `DISCORD_LINKED_ROLE_ID`).
- **Контент:** малая карта под «Стычку», вторая/третья карта, ресурспак скинов стволов (в ганпаках по одному display на ствол).
- Пройти `docs/TEST_CHECKLIST.md` в игре.
