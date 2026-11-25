# PWP — ROADMAP (единственный живой план)

Ветка: `rewrite/unified` (от `rewrite/playable`, 25.11.2026). Старые планы (`REWRITE_PLAN`, `PLAYABLE_PLAN`,
`AUDIT_INTERNALS`, `GAMEPLAY_READINESS`, `DEPLOY_INTERNALS`, `MODLOG_PLAYABLE`, `AGENT_PROMPT`, промпты
`ui-redesign/`) слиты сюда и удалены — их история в git (`git show rewrite/playable:docs/<файл>`).

Проверка каждого этапа: `gradlew build` (с тестами) зелёный + reobf jar проверен (SRG-имена в миксинах) +
коммит. **В игре не проверялось** — что проверить руками, сказано в конце каждого этапа и в
`docs/TEST_CHECKLIST.md`.

Статус: ✅ сделано · 🔄 в работе · ⬜ не начато · ⏸ ждёт решения пользователя.

---

## 1. Аудит (25.11.2026)

### Что есть
| Модуль | .java | Строк | Что | Судьба |
|---|---|---|---|---|
| pwp-warfare | 317 | 59 300 | весь матч: отряды, ФОБ, техника, дроны, HUD, редактор карты, админка матча | → `pwp-game` (ядро) |
| pwp-core-client | 127 | 18 100 | UI-кит, меню, лобби-GUI, сеть данных GUI, админ-панель | → `pwp-core` |
| pwp-logic | 117 | 12 200 | чистая логика + 300+ тестов (не jar, `srcDir` в потребителей) | остаётся (тесты за секунды) |
| core-service | 74 | 9 600 | HTTP API + MySQL (отдельный shadowJar) | остаётся |
| pwp-lobby | 9 | 3 100 | голосования, аллокатор матчей, ServerManager | → `pwp-core` (сам выключается на `match_*`) |
| sbwchunkload | 19 | 2 000 | чанк-лоадинг снарядов/дронов SBW, server-only, своя версия | остаётся отдельным jar |
| pwp-api | 13 | 1 200 | CoreApi, сеть v2, права, время | → `pwp-core` |
| pwp-medicine | 15 | 1 200 | бинты/аптечки, кровотечение (реестр `pwp_medicine:*`) | → `pwp-game` |
| pwp-cosmetics | 10 | 1 100 | скины, экипировка (лобби и warfare зависят от него) | → `pwp-core` |
| pwp-limits | 8 | 400 | спринт-прыжок, F3/F5, свой канал | → `pwp-game` |
| pwp-core-server | 5 | 400 | ключ CoreApi, баны | → `pwp-core` |
| pwp-drone | 5 | 360 | FPV-шейдеры (клиент) | → `pwp-game` (client-only код под Dist) |
| pwp-blast-protection | 7 | 260 | радиусы взрывов, миксины | → `pwp-game` |
| pwp-movement | 2 | 160 | логика хотбара | → `pwp-game` |
| bots/, pwp-scheduler/ | py | — | TG/Discord + шедулер | остаются (экосистема) |

Итого сейчас **12 jar** модов PWP на клиенте/серверах, 4 сетевых канала, 7 файлов конфигов сервера.

### Топ-проблемы
1. **12 jar + зависимости между ними** (`mods.toml`), деплой = 12 файлов в 3 места, любой забытый = краш
   `NoClassDefFoundError` или отказ входа. 4 канала с отдельными версиями протокола.
2. **Конфиг сервера размазан**: `pwpwarfare-common.toml`, `pwp_medicine-common.toml`, `pwplimit-server.toml`,
   `pwpblast.toml`, `sbwchunkload-server.toml`, `pwp_lobby.json`, `pwpcore.txt` (+ `match_config.json`,
   `map_config.json`).
3. **Техника — общий пул фракции** (`faction_vehicles`), нет «дивизий/составов» под карту и режим, как в Squad.
4. **Режимы захардкожены** (AAS/Invasion ветками в `GameLogicEvents` 2400 строк, `CaptureLogic`), маленьких
   режимов нет, движка режимов нет.
5. **Новичка никто не ведёт**: есть система подсказок (`Hints`) и «Как играть», но нет пошагового обучения
   «команда → отряд → роль → деплой → точка».
6. **Мёртвый код и мусор** (проверено grep-ом): пакеты, которые никто не шлёт (`PacketDebugSpawnRally`,
   `PacketForceCMD`, `PacketRequestVehicleAmmo`), экраны только в фикстурах (`PlayerKitSelectScreen` — заменён
   `DeployScreen`, `TacticalMapRadialScreen`), классы без ссылок (`ModelRallyPoint`, `UIIconButton`,
   core `AccountRepository`, `OperationLog`), пустые mixin-конфиги medicine/movement, вложенные gradle-wrapper'ы
   в 4 модулях, закоммиченные `__pycache__`, `screen_methods.txt`, файл `0`, 13 устаревших docs.
7. **Цикл матча не проверен вживую** целиком (чек-лист `rewrite/playable` не пройден); путь шаблона
   матча зашит (`ServerManager.TEMPLATE_PATH = "../PWP-Server/match_template"`), предпрогрев по умолчанию выкл.
8. `gradle.properties` хранит JDK чужой машины (`org.gradle.java.home`) — на этой машине сборка без
   `-Dorg.gradle.java.home=...` не стартует.
9. `GameLogicEvents` — бог-класс 2400 строк (тик матча, голосования, захват, деплой), правки рискованные.

### Что НЕ трогаем
`explosionoverhaul.mixins.json`, секреты, рабочий стол и .bat, `Desktop\PWP-Server`, прод, Pwpfiles.
Таблицы/данные БД не удаляются без «ок» пользователя (только новые миграции).

---

## 2. Целевая структура

```
pwp-logic/      чистая логика + тесты (не jar): src/api, src/coreclient → pwp-core; src/warfare → pwp-game
pwp-core/       jar «PWP Core»   modid pwpcore     (BOTH) api + core-client + core-server + lobby + cosmetics
pwp-game/       jar «PWP Game»   modid pwpwarfare  (BOTH) warfare + medicine + limits + movement + blast + drone
sbwchunkload/   jar              modid sbwchunkload (server) — независимый аддон SBW, своя версия
core-service/   HTTP API + БД (shadowJar), bots/, pwp-scheduler/ — экосистема
```

**3 jar вместо 12, одинаковый набор на клиенте, лобби и матч-серверах.**

Обоснование отличий от предложенного варианта:
- `pwp-cosmetics` — в **core**, а не в game: лобби (без матча) экипирует скины и зависит от него; в game он
  создал бы зависимость core → game.
- `pwp-drone` — в **game**: 5 файлов клиентского кода, на сервере безвреден (Dist-гейт), отдельный jar не
  окупается. `sbwchunkload` остаётся отдельно: это самостоятельный аддон SBW со своей версией и server-only
  миксинами, на клиентах не нужен.
- Лобби в core безопасно: `LobbyMod` уже выключается сам в папке `match_*` (тот же признак, что у
  `CoreServerMod.isMatchServer`); дополнительно — только на выделенном сервере.
- Главный modid game = **`pwpwarfare`**: это неймспейс блоков/предметов/сущностей в мирах, в БД китов и в
  `maps/*.json`. Смена = миграция всех миров и БД без проверки в игре — не окупается.

### Миграция при слиянии (этап 2)
| Что | Решение | Почему безопасно |
|---|---|---|
| Реестры | `pwpwarfare:*` без изменений; `pwp_medicine:*` (предметы/эффекты/звуки) регистрируются тем же `DeferredRegister` с неймспейсом `pwp_medicine` из мода `pwpwarfare` | Forge 47.3.0 `GameData.checkPrefix` только пишет debug-лог при чужом неймспейсе (проверено по байткоду) — ID в мирах и китах не меняются |
| Ассеты и lang | папки `assets/pwp_core_client`, `pwp_cosmetics`, `pwp_lobby`, `pwp_medicine`, `pwpdrone`, `pwpwarfare` переезжают как есть | ресурсы ищутся по неймспейсу, не по modid; lang-ключи не меняются |
| NBT | ключи не меняются (WarfareKeys, capability, SavedData) | capability/SavedData привязаны к своим ResourceLocation, не к modid |
| Конфиги Forge | имена файлов фиксируются явно: `pwp_medicine-common.toml`, `pwplimit-server.toml`, `pwpblast.toml` | без явного имени Forge взял бы `pwpwarfare-*.toml` и настройки «сбросились» бы |
| Конфиги JSON | `pwp_lobby.json`, `pwp_ui_client.json`, `pwpcore.txt` — пути прежние | — |
| `@EventBusSubscriber(modid=…)` | все `modid` → новый главный modid своего jar | иначе Forge молча НЕ подпишет класс (фильтр по modid контейнера) |
| `ModList.isLoaded("pwpcore"/"pwp_medicine"/"pwp_cosmetics")` | → прямые проверки (`CoreApi.isConfigured()`, всегда true) | после слияния эти modid всегда есть |
| Сеть | один канал на jar: `pwp_core_client:network` (+ пакеты cosmetics в конец), `pwpwarfare:main` (+ пакеты limits в конец); версии протокола **8** | один протокол на jar; старые клиенты получают отказ версии, а не краш |
| Миксины | конфиги остаются отдельными файлами, в манифесте jar — список `MixinConfigs`; один рефмап на jar | guard в `jar` проверяет все конфиги и рефмап |
| Мир, сохранённый старыми модами | Forge на выделенном сервере пишет WARN «мод pwp_medicine отсутствует» и грузит мир | реестровых пропусков нет (неймспейсы прежние) |
| Лаунчер / Pwpfiles | список jar меняется (см. раздел 5) | — |

---

## 3. Этапы

| # | Этап | Готово, когда… | Риск | Размер | Статус |
|---|---|---|---|---|---|
| 0 | Аудит + этот план | ROADMAP.md в репо | — | S | ✅ |
| 1 | Чистка без смены поведения | мёртвые классы/пакеты/экраны, пустые mixin-конфиги, вложенные wrapper'ы, `__pycache__`, мусорные файлы и устаревшие docs удалены; сборка+тесты зелёные | Н | S | ✅ |
| 2a | Слияние `pwp-core` | один jar `pwp-core` (modid `pwpcore`), один `@Mod`, канал core v8 с пакетами cosmetics; lobby/core-server выключены на клиенте | С | M | ✅ |
| 2b | Слияние `pwp-game` | один jar `pwp-game` (modid `pwpwarfare`), канал warfare v8 с пакетами limits, имена конфигов прежние; ProGuard/CI обновлены; reobf проверен | С | M | ✅ |
| 3 | Один конфиг сервера | `config/pwp-server.json`: роль (auto/lobby/match), лобби, шаблон и порты матчей, папка карт; старый `pwp_lobby.json` переносится один раз | Н | S | ✅ |
| 4 | Цикл матча сам | трассировка «лобби → голосование → матч → итоги → лобби»: ручных шагов нет; матч, который не кончится сам (пустой / никто не пришёл / бесконечный), освобождает слот лобби | С | S | ✅ |
| 5 | Обучение новичка | чистый `Tutorial` (шаги команда → отряд → роль → деплой → точка) с тестами + показ через `Hints`; «пропустить», раз на аккаунт | Н | S | ⬜ |
| 6 | Дивизии/составы фракций | модель `Division` (фракция, режимы, размер карты, слоты техники с количеством/респавном, лимиты ролей) в pwp-logic + тесты; миграция БД (новые таблицы, старые не трогаем) + API core; выбор в лобби/редакторе; спавнер берёт технику из состава, фолбэк — прежний пул | В | L (3 подэтапа) | ⬜ |
| 7 | Маленькие режимы | интерфейс режима (правила тикетов/точек/спавна) + AAS/Invasion через него без смены поведения + 1–2 малых режима (одна точка, без техники) для карт ≤ 200×200 | В | L | ⬜ |
| 8 | Экосистема | магазин/донаты в игре через существующие `ShopController`/`DonationController` (GUI в core, без новых денег в моде), статус сети для TG/Discord из heartbeat | С | M | ⬜ |
| 9 | Документация | README (сервер одной командой, карта за 10 минут), AGENTS.md под 3 jar, TEST_CHECKLIST | Н | S | ⬜ |

Порядок жёсткий: 1 → 2 → 3 → 4 → 5, дальше 6/7/8. Каждый этап — свой коммит (подэтапы — свои).

---

## 4. Журнал

- 25.11.2026 — этап 0: аудит, план. Базовая сборка `rewrite/playable` зелёная (1 мин 52 с инкрементально).
- 25.11.2026 — этап 1 ✅: удалены пакеты без отправителей `PacketDebugSpawnRally`, `PacketForceCMD`,
  `PacketRequestVehicleAmmo` (канал `pwpwarfare:main` → **8**, индексы сдвинулись), экраны только из фикстур
  `PlayerKitSelectScreen` (заменён DeployScreen), `TacticalMapRadialScreen`, классы без ссылок `ModelRallyPoint`,
  `UIIconButton`, core `AccountRepository`/`OperationLog`; пустые `pwp_medicine/pwp_movement.mixins.json`
  (не были в манифесте — не грузились); вложенные gradle-wrapper'ы и `.gitignore` 4 модулей; `__pycache__`
  из индекса; `screen_methods.txt`, `0`; 10 устаревших docs. `gradle.properties` → JDK этой машины.
  Новый `tools/check_reobf.py` — проверка SRG/рефмапа/MixinConfigs во всех jar. 335 тестов зелёные.
  **Проверить в игре:** ничего нового — поведение не менялось (галерея `/pwpui` без двух фикстур).
  Дальше — этап 2a.
- 25.11.2026 — этап 2a ✅: `pwp-core` (jar `pwp-core-2.0.0.jar`, modid `pwpcore`) = бывшие pwp-api,
  pwp-core-client, pwp-core-server, pwp-lobby, pwp-cosmetics (`git mv`, пакеты прежние). Один `@Mod` —
  `com.pwp.api.PwpCoreMod`, части создаются в прежнем порядке; лобби — только выделенный сервер вне `match_*`
  (`com.pwp.api.ServerRole`, общий признак для core-server и лобби). Пакеты косметики — в конце канала
  `pwp_core_client:network` (протокол **8**), свой канал `pwp_cosmetics:main` удалён. `ModList.isLoaded("pwpcore")`
  в warfare → `CoreApi.isConfigured()` (в dev-одиночке теперь честно «core нет»), проверка косметики убрана.
  «Управление»: старые неймспейсы клавиш сопоставлены новым модам. Убраны отладочные `log.error` из CoreClientMod.
  Остальные моды зависят от `pwpcore` [2.0,). **Проверить в игре:** главное меню/лобби/F6 открываются;
  экипировка скина в лобби (пакет косметики по новому каналу); в лобби идут голосования, на матч-сервере лобби
  молчит (в логе нет «Lobby…» строк); вход на сервер со старым клиентом — отказ версии канала.
- 25.11.2026 — этап 2b ✅: `pwp-game` (jar `pwp-game-2.0.0.jar`, modid `pwpwarfare`) = pwp-warfare + medicine +
  limits + movement + blast-protection + drone. Один `@Mod` — `WarfareMod`, остальные создаются из него
  (`PWPMedicine`, `PWPLimit`, `PWPMovement`, `BlastProtectionMod`; `PWPDroneMod` был пустым — удалён).
  Реестры `pwp_medicine:*`, ассеты `pwp_medicine`/`pwpdrone`, data `pwp_medicine`/`pwpblast` — прежние.
  Конфиги с прежними именами: `pwp_medicine-common.toml`, `pwplimit-server.toml`, `pwpblast.toml`. Пакет
  лимитов — в конце `pwpwarfare:main` (протокол **8**), канал `pwplimit:network` удалён. Миксины blast —
  второй конфиг в том же jar, общий рефмап `pwpwarfare.refmap.json`; `BlockExplosionResistanceMixin` не был
  в конфиге (не применялся) — удалён. `@EventBusSubscriber(modid=…)` бывших модов → `pwpwarfare`.
  Сборка: **reobf-гарантия** в корневом `build.gradle` (`jar` всегда завершается `reobfJar`, после него —
  проверка SRG-имён; прямой `gradlew :pwp-core:jar` раньше оставлял mojmap-jar — воспроизведено и
  исправлено); гигиена `build/libs` (старые jar удаляются); путь Pwpfiles по умолчанию — `../Pwpfiles`
  рядом с репо; тесты pwp-logic видят боевые файлы как входы (пути после переезда были сломаны, а тест
  считался up-to-date). Итог: **3 jar** — `pwp-core-2.0.0`, `pwp-game-2.0.0`, `sbwchunkload-2.2.1`.
  **Проверить в игре:** бинт/аптечка/кровотечение (предметы в старых китах находятся), спринт-прыжок и
  блок F3/F5 (+ правка `pwplimit-server.toml` на лету доходит до клиента), взрывы (блоки `hard_blast`
  держат), FPV-шейдер дрона, хотбар; в логе сервера нет «Missing registry entries».
  Дальше — этап 3 (один конфиг сервера).
- 25.11.2026 — этап 3 ✅: **`config/pwp-server.json`** — единый файл сервера (чистые `ServerSettings` +
  `ServerSettingsFile` в pwp-logic, 7 тестов): `role` auto|lobby|match (auto = папка `match_*`, как раньше),
  `lobby` {autostart, maxMatches, minPlayers, prewarm}, `matchServers` {template, basePort} (раньше зашиты в
  `ServerManager`: `../PWP-Server/match_template`, 25566 — дефолты те же), `mapsDir` (пусто = прежний
  авто-поиск). Создаётся с дефолтами при первом старте выделенного сервера; нет файла, но есть старый
  `pwp_lobby.json` — значения переносятся (старый файл не удаляется, больше не читается); битый файл не
  перезаписывается. Неизвестные ключи сохраняются. `ServerRole` (core) — единственная точка доступа.
  Осталось в Forge-TOML (осознанно): игровой баланс `pwpwarfare-common.toml`, `pwp_medicine-common.toml`,
  `serverconfig/pwplimit-server.toml`, `pwpblast.toml`, `sbwchunkload-server.toml` — у них горячая
  перезагрузка и синхронизация клиентам, дефолты рабочие; ключ API — по-прежнему `PWP_API_KEY`/`config/pwpcore.txt`
  (секрет, отдельно от общего файла). **Проверить в игре:** после старта лобби в `config/` появился
  `pwp-server.json` со значениями из прежнего `pwp_lobby.json`; «Сохранить навсегда» в F6 → «Лобби» пишет в
  него; матч стартует из шаблона как раньше.
  Дальше — этап 4 (цикл матча сам).
- 25.11.2026 — этап 4 ✅. Трассировка по коду: лобби уже само стартует голосование (вход игрока, конец матча,
  раз в 5 с; `LobbyCycle.autoVoteAllowed`: автостарт + minPlayers + пауза), подготовка — 1 повтор и ABORT с паузой,
  мёртвый матч-сервер чистится (`MatchAllocator.tick` → `matchEnded` → следующее голосование), после победы
  матч сам возвращает всех в лобби и гасится. **Дыра:** матч-сервер, который не кончается сам, держал слот
  `maxMatches` вечно — все вышли из матча / никто не дошёл после переброса / тикеты не кончаются → авто-цикл
  стоял до ручного стопа. Исправлено: чистый `match/MatchWatchdog` (pwp-logic, 7 тестов) +
  `server/MatchWatchdogTicker` (раз в секунду, только матч-сервер): пусто 3 мин после того, как кто-то заходил →
  корректная остановка (итоги по тикетам → halt); никто не зашёл за 15 мин после готовности (предпрогрев не
  считается) → остановка; бой дольше 120 мин → победа по тикетам (поровну — BLUE) и обычный экран итогов.
  Ключи `emptyMatchShutdownMinutes`/`noShowShutdownMinutes`/`matchMaxMinutes` в `pwpwarfare-common.toml`
  (0 = выкл.). Тело обработки `stop.signal` вынесено в `MatchShutdown.stopGracefully` (логика прежняя).
  **Проверить в игре:** все вышли из матча → через ~3 мин + ~3 мин (итоги) сервер гаснет, лобби пишет «матч
  завершён» и при игроках стартует голосование; `matchMaxMinutes=1` на тест-сервере → через минуту боя экран
  победы.
  Дальше — этап 5 (обучение новичка).

---

## 5. Что сделать вне репо (пользователь)

- **build.bat / start.bat** (рабочий стол): `MODS_LIST` после этапа 2 = `pwp-core`, `pwp-game`, `sbwchunkload`.
- **Pwpfiles/mods**: удалить старые `pwp-*.jar` (api, core-client, core-server, lobby, cosmetics, warfare,
  medicine, movement, limits, blast-protection, drone), положить `pwp-core-*.jar`, `pwp-game-*.jar` →
  `gen.ps1` → подпись манифеста.
- **Лаунчер (PWP-Launcher)**: если в коде лаунчера есть белый список имён jar или проверка «обязательные
  моды PWP» — заменить на `pwp-core`/`pwp-game`; лаунчер должен удалять из `mods/` jar, которых нет в
  манифесте (иначе у игроков останутся старые `pwp-warfare-*.jar` → дубли modid → краш).
- **Серверы** (лобби, `match_template`): тот же набор из 3 jar.
