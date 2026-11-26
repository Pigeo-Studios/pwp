# Ревью кода (26.11.2026, ветка `review/cleanup`)

Это ревью, а не новые фичи. В игре не проверялось. Каждую находку доказывают ссылка на код и команда.
Статус: ✅ исправлено · ⏸ оставлено (почему — в столбце «Что делать») · ℹ️ только к сведению.

Серьёзность: **В** — краш, утечка врагу или потеря данных; **С** — нарушение правил AGENTS.md с риском;
**Н** — слоп или косметика.

## 1. Граф зависимостей

Его строит скрипт: импорты, FQCN в коде и простые имена внутри пакета, классы `pwp-core`, `pwp-game`, `pwp-logic`.

| Проверка | Итог | Доказательство |
|---|---|---|
| `pwp-core` → `pwp-game` | **0 ссылок** | `grep -rln "com.pigeostudios\|com.pwp.blastprotection" pwp-core/src` — пусто |
| `pwp-logic` без `net.minecraft`/`net.minecraftforge`/`io.netty` | **0** | `grep -rn "import net.minecraft\|import net.minecraftforge\|import io.netty" pwp-logic/src` — пусто |
| `com.pwp.api` → `coreclient`/`coreserver`/`lobby`/`cosmetics` | только `PwpCoreMod` (собирает части мода) | граф пакетов |
| `lobby` → `coreclient.network` | пакеты канала core (`ConnectToServerPacket`, `LobbyStatePacket`) — общий код, не клиентский | граф пакетов |
| Серверный класс → клиентский (`@OnlyIn(CLIENT)`, `client.*`, `import net.minecraft.client`) | 56 ссылок, **все под гардом**: `DistExecutor`, ветка `isClientSide`, обработчик S2C (выполняется только на клиенте), `FMLEnvironment.dist == CLIENT` в `CoreClientMod`, `initializeClient` у предметов | список ниже |

Мягкие ссылки (рабочие, потому что HotSpot разрешает классы лениво). Чинить их не нужно. Новый код пусть идёт через `DistExecutor`:
- `InteractionEvents:232`: `ClientData.isGameStarted` в ветке `level.isClientSide`;
- `RallyItem:65`: `ClientHooks.tryOpenRadioMenu` в `else`-ветке клиента;
- `ModItems` → `BandageItem`/`MedkitItem`: импорт `BlockEntityWithoutLevelRenderer` только в `initializeClient`.

## 2. Находки

| # | Файл:строка | Кат. | Сер. | Суть | Что делать | Доказательство | Статус |
|---|---|---|---|---|---|---|---|
| 1 | `pwp-game/.../server/AfkTicker.java:39,62` | баг | **В** | AFK-кик идёт внутри `for` по `getPlayerList().getPlayers()`. `disconnect()` на серверном потоке зовёт `executeBlocking(handleDisconnection)`, а тот выполняется сразу, удаляет игрока из того же `ArrayList` → `ConcurrentModificationException` в тике сервера (если кикнут не последний в списке) | обходить копию списка | `ServerGamePacketListenerImpl.disconnect` (1.20.1) → `BlockableEventLoop.executeBlocking`: на своём потоке `task.run()` | ✅ |
| 2 | `pwp-game/.../server/CommanderAbilities.java:62` + `GameLogicEvents.java:320` | утечка | **В** | Разведка UAV отдаёт врагов полной `MapPlayerInfo`: ник, UUID, id отряда, лидер, в технике ли, место. ROADMAP обещает «красные точки без ников». Клиент прячет ник у пешего врага, но у врага в технике подписывает ник водителя (`SquadMapRenderer:686`, ветка `vehicleGroups`) | сервер обезличивает: позиция и команда, вместо ника — `?N`, UUID — синтетический | код `revealedFor` возвращает исходные объекты | ✅ + тест |
| 3 | `pwp-game/.../network/PacketHandler.java:410,499` | утечка / мёртвый код | С | Две перегрузки `sendToAllClients`: 12 параметров (`PacketDistributor.ALL` с полным `SyncGameData` — это мап-хак) и `(WarfareWorldData, bool…)`. Вызовов у обеих нет | удалить | grep по `sendToAllClients(`: все вызовы — `(level, data)`; сборка | ✅ |
| 4 | `pwp-game/.../WarfareMod.java:385` | баг | Н | RAAS вырезает невыбранные точки из `data.match.capturePoints`. Если точки взяты из SavedData мира (в `map_config` их нет), они теряются из мира навсегда | RAAS — только при точках из `map_config`, иначе цепочка AAS и предупреждение | `WarfareMod:296` — ветка «keeping … from world SavedData» | ✅ |
| 5 | `pwp-core/.../coreserver/BanCommand.java:42-47`, `UnbanCommand.java:22-27`, `BanEnforcer.java:98` | правило | С | Коды `§`, текст на русском и английском прямо в коде, нет записи в аудит-лог. Бан из консоли и бан лестницей тимкиллов не видны во вкладке «Сервер» | ключи lang (en/ru/uk) + `AdminAudit.recordSource` | `grep -rn '§\|\\u00A7' pwp-core/src/main` | ✅ |
| 6 | `pwp-core/.../cosmetics/CosmeticService.java:72,91,97` | правило | С | Тексты отказа экипировки — русские строки. `SkinInventoryScreen:327` показывает их как есть | сервер шлёт ключ lang, клиент — `I18n.get`. Старый текст тоже показывается: для неизвестного ключа `I18n.get` возвращает саму строку, протокол не меняется | grep | ✅ |
| 7 | `pwp-core/.../coreserver/community/CommunityCommands.java:129` | правило | С | Админ-чат `/a` идёт мимо `ChatGuard`. По AGENTS.md любой канал чата обязан проходить через него | `ChatGuard.filter` для игрока (модераторы и так без антиспама, так что меняется только фильтр мата) | код | ✅ |
| 8 | `pwp-game/.../events/InteractionEvents.java:632` | правило | С | Закрепление техники пускает любого в креативе как модератора (`isCreative()`), а `isCreative()` правом не является | только `PwpPermissions.has(MODERATOR)` | AGENTS.md «Права» | ✅ |
| 9 | 14 серверных файлов (`MatchAllocator`, `ServerManager`, `VotingManager`, `FactionVotingManager`, `LobbyMod`, `MatchPolicy`, `SkinCatalog`, `MatchStatsTracker`, `MapTileService`, `MapOverviewService`, `MapPrerender`, `WarfareVoicechatPlugin`, `GameLogicEvents`, `AdminAudit`) | правило | Н | `System.currentTimeMillis()` вместо `GameClock.realMillis()`. Везде это настенное время: TTL, таймеры голосований, длительность матча | механическая замена, `realMillis()` = `System.currentTimeMillis()` | `GameClock.java:46` | ✅ |
| 10 | `events/PlayerItemEvents.java:187,197,203`, `GameLogicEvents.java:806,1220,1440`, `PlayerLifecycleEvents.java:251` | слоп | Н | 7 методов без вызовов. До разбора `split_java.py` они были `private` и так же мёртвые (javac молчал), `split_java` сделал их package-private. `enforceMortarShellLimit(PlayerTickEvent)` ни разу не был подписан (нет `@SubscribeEvent` с порта `005e6c8`): лимит 8 мин миномёта не работал никогда | удалить | `grep -rnw <имя>` — 1 вхождение (объявление); `git show a4fb43d~1:…GameLogicEvents.java` | ✅ |
| 11 | `pwp-core/.../api/core/CoreApi.java` | слоп | Н | 12 методов HTTP без вызовов: `createPlayer`, `getPlayerProfile`, `spendCurrency`, `getPlayerRanks`, `getSkinsBySlot`, `getSkinsByWeapon`, `getCases`, `openCase`, `getPlayerRank`, `setMatchPolicy`, `bulkSaveFactionKits`, `getMapLayout` | удалить из клиента API, эндпоинты core не трогать | grep по репо, включая `bots/`, `tools/`, `proguard.pro` | ✅ |
| 12 | `lobby/VotingManager`, `LobbyMod.hasVotedMode`, `ServerManager.getActiveCount`, `MapRegistry.getBestFit`, `MatchAllocator.queuePosition`, `CosmeticManager.getEquipmentList/getSkinItem`, `PlayerMatchStats.getKD`, `PlayerWarfareData.copyTag`, `RequestGuard.isInFlight`, `SquadUIHelper` (6 методов), `SquadMapRenderer.hitPath/hitServerPath/toAlpha`, `SquadContextMenu.hitIdx` | слоп | Н | методы без вызовов | удалить. Не UI-кит: публичный API кита `com.pwp.coreclient.ui` по правилу только растёт | grep | ✅ |
| 13 | `pwp-game/.../client/hud/StaminaClient.java` | доверие клиенту | С | Выносливость считает только клиент: модифицированный клиент бегает без ограничений | серверная проверка — это новая механика (сервер должен видеть спринт и гасить его), а значит фича. Оставлено | код | ⏸ |
| 14 | `CommanderAbilities.use` | геймплей | С | CAS и сброс снабжения можно вызвать в любую загруженную точку, в том числе на мейн врага (спавн-килл) | правило «не в мейн» — решение по балансу; записано в «Решения», не трогал | код | ⏸ |
| 15 | `CommanderAbilities` `COOLDOWNS`/`REVEALS` | память | Н | Статические, между матчами не сбрасываются. Это допустимо, пока матч = новый сервер. На тест-стенде с перезапуском матча на том же сервере кулдауны переносятся | ℹ️ | код | ⏸ |
| 16 | `MatchAllocator.connectOrQueue` | гонка | Н | Локальный `playerCount++` перетирается следующим heartbeat, а тот запаздывает → в матч может пройти на 1–2 игрока больше. Страховку даёт `ReservedSlots` матч-сервера | ℹ️ по задумке | код | ⏸ |
| 17 | `CommunityCommands /pwpskin try` | правило | Н | `hasPermission(2)` и не через `AdminAction`. Команда меняет только NBT ствола в руке самого админа (примерка) | оставлено: не действие над другими | код | ⏸ |
| 18 | `sbwchunkload/*` | правило | ℹ️ | `server.getTickCount()` вместо `GameClock`. Отдельный мод без зависимости от `pwp-core`, `GameClock` ему недоступен | исключение — в AGENTS.md | `sbwchunkload/build.gradle` | ✅ (док) |
| 19 | `pwp-core/.../coreclient/mixin/ScreenInvoker.java` | сборка | ℹ️ | Предупреждение AP «Unable to locate obfuscation mapping for addRenderableWidget». Запись в рефмап вписывает `build.gradle`, её проверяет `check_reobf.py` | ℹ️ | лог сборки | — |
| 20 | `docs/ROADMAP.md` (этап 15, «Что дальше») | док | Н | «Красные точки без ников» в пакете не выполнялось (см. №2); нумерация 0,1,2,3,5 | исправить | — | ✅ |

Проверено и в порядке:
- `ReportService`: кулдаун, `forget` при выходе, HTTP через `thenOnServer`.
- `ChatGuard`/`ChatRate`/`ChatFilter`: синхронизация, `forget`.
- `TeamkillLadder` в `DownedHandler`: кик бана — одиночный, не в цикле.
- `JoinQueue`: приоритеты, `expire`.
- `ReservedSlots`: кик внутри цикла по своей `PENDING`, а не по списку игроков.
- `VehicleClaim`: приоритет `HIGH` после `HIGHEST`, отменённые посадки не трогает.
- `WeaponSkinService`: очистка при выходе, `GunId` не меняется.
- `TeamVisibility`/`viewerKey`: всё видит только модератор-наблюдатель.
- `ScoreboardData`: враг получает только ник и очки.
- `DivisionAdmin`: `AdminAction.runLocal`, роль берёт сервер.
- `RaasChain`, `LabelPlacer`: чистая логика с тестами.
- Разбор `GameLogicEvents`: все 18 `@SubscribeEvent` на месте.
- Сайт: только `textContent`, нет `innerHTML`.

## 3. Мёртвые классы

Скрипт искал классы без входящих ссылок, плюс проверку имени в json/pro/py/gradle. **Ни одного не нашлось**: всё убрал этап 1.

## 4. Декомпозиция больших классов (предложение, не делалось)

| Класс | Предложение | Почему не сейчас |
|---|---|---|
| `GameLogicEvents.onServerTick` (~650 строк) | вынести блоки по `globalTick % N` в `MatchTick.{syncMapPlayers, vehicleMarkers, capture, invasionSetup}` с теми же аргументами | порядок побочных эффектов внутри тика важен (синк после захвата). Без проверки в игре — риск; ROADMAP уже ставит это «после проверки в игре» |
| `ModCommands.register` (одно выражение на 830 строк) | разрезать по `literal(...)` на `registerMatch/registerMap/registerAdmin(d)` | механически безопасно только с инструментом, который разбирает дерево brigadier; вручную легко потерять `.requires` |
| `DeployScreen`, `SquadMapRenderer` | UI | проверяются только глазами |
| `WarfareWorldData` | уже разбит на реестры (`match`, `squadRegistry`, …) | — |
| `LobbyMod` (~1300 строк) | вынести голосование режимов в `ModeVoting` по образцу `VotingManager` | меняет порядок тиков лобби; после проверки цикла матча в игре |
