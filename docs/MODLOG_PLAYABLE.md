# MODLOG — rewrite/playable

Журнал по методу universal-modder (mod-any-game). Игра: Minecraft Java 1.20.1, Forge 47.3.0, JDK 17.

## Маршрут
Loader API (Forge-события, SimpleChannel сеть v2) + существующие миксины. Новых миксинов не нужно.
Оракул: игру и серверы НЕ запускаем (ограничение юзера) → сборка `gradlew build -x test`, тесты
`gradlew :pwp-logic:test` / `gradlew test`, reobf-проверка SRG в jar. Проверку в игре делает юзер
по `docs/TEST_CHECKLIST.md`.

## Факты из базы знаний скила (применимо к Forge 1.20.1)
- `ConfigValue.set()+save()` может обрезать TOML, пока его перечитывает watcher (night-config) — касается
  `MatchAdmin.setWaitingSeconds(persist)`; записано в план как P13.
- Канал с явной версией протокола — уже правило проекта.
- Dev-запуск на mojmap, релиз на SRG: проверять reobf jar (уже правило AGENTS.md).

## Окружение
- Python для скриптов: `py -3` (голый `python` — заглушка Store). Lang-правки — скрипт в scratchpad
  `addlang.py <lang_dir> <keys.json>` (json.dump indent 4, ensure_ascii=False; формат файлов совпадает).

## Ход
- 19.11: ветка `rewrite/playable` от `rewrite/internals` (accc88f). Аудит по коду → `docs/PLAYABLE_PLAN.md`.
- 19.11: этапы P1–P6 закоммичены по одному (см. `git log rewrite/internals..rewrite/playable`).
- Gotcha: spotlessApply пишет CRLF, а perl/awk-правки ждут LF → перед скриптовой правкой `sed -i 's/\r$//'`
  (git autocrlf=true, в индексе разницы нет). Многострочные perl-замены по CRLF-файлу молча не срабатывают.
- Gotcha: тест `LogScrub` поймал утечку — маска key=value раньше Bearer оставляла токен после «Bearer».
- `PacketForceCMD` клиентом не отправляется нигде (мёртвый вход) — права/синк исправлены на будущее.
- Проверка: `gradlew build` (spotlessCheck + тесты) зелёный, 332 теста (pwp-logic 312, pwp-api 5,
  pwp-warfare 15), `reobfJar` — в новых классах только SRG (`m_…_`), mojmap нет. Игра/серверы не запускались.
- Стенд: jar заменены на месте (те же версии) в PWP-TestServer, PWP-TestLobby/lobby, match_template, Prism.
- Не сделано: M3 (дельты SyncGameData) — нужен замер трафика в живом матче; разбор `onServerTick`;
  атомарная запись TOML (P13).
