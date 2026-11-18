# Готовность PWP к живым матчам (18.11.2026, ветка `rewrite/internals`)

Карта собрана по коду, ничего не проверено в игре (в `docs/TEST_CHECKLIST.md` отмечен 1 пункт из 67). Пункты с пометкой ✔ подтверждены повторным чтением кода.

## Блокеры для живого матча
| # | Что | Где | Этап |
|---|---|---|---|
| 1 | ✔ Артиллерия КМД: подтверждение добавляет удар, но тикера нет — взрывов нет, зона на карте висит вечно, в сохранениях копится | `network/PacketConfirmArtStrike.java:49`, `world/WarfareWorldData.java:444` (ticksLeft не уменьшается) | G1 |
| 2 | ✔ Подъём из нока мгновенный по ПКМ с аптечкой, без канала и без проверки роли медика | `events/DownedHandler.java:233` | G1 |
| 3 | ✔ Возврат в лобби: `Thread.sleep(2000)` на главном потоке, порт лобби 25565 зашит | `stats/MatchStatsTracker.java:360-362`, `events/GameLogicEvents.java:960` | G1 |
| 4 | Игрок не видит полученные XP/Coins; assists и headshots всегда 0 | `stats/MatchStatsTracker.java:257,261`, `VictoryScreen` | G1 |
| 5 | Комплект Pwpfiles устарел: нет `pwp-api`, `pwp-limits` 1.0.3 без исходников | `Pwpfiles/mods` | релиз (см. `DEPLOY_INTERNALS.md`) |
| 6 | «Опциональные» BOTH-моды (Explosion-Overhaul, Gunfire Overhaul, survival_instinct, tacticalmovement, PassableFoliage) лежат в `mods/`, на сервере стоят — риск отказа входа | `Pwpfiles/mods/.metadata.json` | G2 (решение юзера) |
| 7 | Зависимости в mods.toml не объявлены (cosmetics → core-client; warfare → SBW/TACZ/Curios/GeckoLib; medicine → GeckoLib); GeckoLib компилируется 4.4.9, в проде 4.8.4 | `*/mods.toml`, `pwp-warfare/build.gradle`, `pwp-medicine/build.gradle` | G2 |
| 8 | Одна карта (takmachka) — голосование за карту пустое | `maps/` | контент (юзер) |
| 9 | 299 текстовых литералов вместо lang (игроки видят смесь ru/en, uk нет); у pwp-lobby нет uk_ua | warfare 231, lobby 33, medicine 13 | G3 |
| 10 | Остатки легаси-GUI: WarfareOverlay (Build Mode, RU-литералы), ConfigScreen limits/medicine, TeamSelectionScreen | см. отчёт | G4 |

## Этапы
- **G1 — механики и путь игрока:** артиллерия (залпы по таймеру, истечение, только своей команде), подъём каналом и только медиком, возврат в лобби без блокировки главного потока и с портом из конфига, награды на экране победы, assists/headshots.
- **G2 — сборка модов:** mods.toml-зависимости, GeckoLib 4.8.4, судьба pwp-drone, опциональные BOTH-моды.
- **G3 — локализация:** литералы → lang en/ru/uk, uk_ua лобби, ключи-копии.
- **G4 — GUI:** легаси-экраны и остаток WarfareOverlay на кит.
- **Контент и дизайн (нужны решения юзера):** новые карты, боевой пропуск/престиж/магазин в игре, способности командира кроме артиллерии.

## Сделано
- **G1 (18.11.2026):** артиллерия — `server/ArtilleryTicker` + `match/CombatRules` (подлёт 5 с, 30 снарядов по 0,5 с в круге `artStrikeRadius`, без разрушения ландшафта, удар снимается по таймеру; подтверждение и зона — только своей команде). Подъём — только медик, канал `reviveTimeSeconds` (5 с), рвётся дальше 3 блоков / без аптечки / если медик упал. Порог тимкиллов — `teamkillSpectatorThreshold`. Возврат в лобби — пауза в фоновом потоке, порт `lobbyPort`. Ассисты (`stats/AssistTracker`, окно 15 с) в награды; XP/монеты/ассисты на экране победы (`RewardPackets.MatchRewards`, протокол `pwpwarfare:main` 6). Не в игре: build + 325 тестов.
- **G2 (18.11.2026):** mods.toml — warfare требует geckolib, curios, tacz, superbwarfare; medicine — geckolib; cosmetics — pwp_core_client (вместо NoClassDefFoundError — экран «не хватает мода»). GeckoLib оставлен 4.4.9 на компиляции: API 4.x совместим с 4.8.4 в проде, переход на 4.8.4 — вместе с проверкой рендера в игре. **Ждут решения юзера:** поставлять ли pwp-drone (в репо и Prism есть, в Pwpfiles нет); опциональные BOTH-моды (Explosion-Overhaul, Gunfire Overhaul, survival_instinct, tacticalmovement, PassableFoliage) — сделать обязательными или снять с сервера.

