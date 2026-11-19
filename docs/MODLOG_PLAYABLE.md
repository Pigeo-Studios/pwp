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
