# Правила: обфускация pwp-модов (ProGuard)

Часть правил PWP. Полный индекс — корневой `AGENTS.md`.
**Перед изменением кода этого модуля/домена — прочитай этот файл; после изменений (новый модуль/фича, инцидент, фикс, изменённые правила сборки) — обнови его**: текст на русском, метки дат `(дд.мм.2026)`, удаляй устаревшее, если описание разошлось с кодом, секреты/токены не добавлять.

---

- Пайплайн: `build-obfuscate.bat` на рабочем столе = копия `build.bat` + шаг `gradlew obfuscatePwpMods` (корневой `build.gradle`, конфиг `proguard.pro`). Обычный `build.bat` НЕ обфусцирует. Результат: `PWP/build/obfuscated/*.jar` + `mapping.txt` (для retrace стектрейсов прода)
- Режим: `-dontshrink -dontoptimize` — только переименование имён; библиотеки: `forge-<ver>-srg.jar` из кэша ForgeGradle + прод-жарники geckolib/curios/voicechat из Pwpfiles (без них ProGuard переименует override-методы vanilla/GeckoLib — игра молча сломается)
- Keep-правила (что нельзя переименовывать): миксин-пакеты целиком (рефмап), `@Mod` классы, рефлексия лобби (`LobbyMod.voteMode`, `VotingManager.vote`, `FactionVotingManager.vote`, `MatchAllocator.joinActiveMatch/joinMatchById`), поля `ServerManager$ServerMatchConfig` (Gson-маппинг match_config.json по именам полей), константы всех enum (`.name()` уходит строками клиенту — `MatchPhase.STARTING/PLAYING/ENDING`), пакет `voicechat` (ASM-скан Forge по `@ForgeVoicechatPlugin`)
- ПРАВИЛО НА БУДУЩЕЕ: добавил `Class.forName`/`getMethod` по имени на наши классы или Gson-маппинг POJO по именам полей — ОБЯЗАН добавить keep-запись в `proguard.pro`, иначе в обфусцированном релизе сломается молча. Новые миксины в существующих пакетах покрываются автоматически; новый модуль — добавить в `MODS_LIST` в build.bat и в `pwpModules` в корневом `build.gradle`
