# Деплой `rewrite/internals` (с машины сервера)

Для агента или человека на машине боевого сервера (`C:\Users\maska\OneDrive\Desktop\...`): там лежат ключ подписи манифеста, боевая БД и живой сервер. Ветка: `rewrite/internals`, что в ней — `docs/AUDIT_INTERNALS.md`, раздел «Сделано».

**Главное: клиент и серверы обновляются вместе.** Версии каналов подняты (`pwpwarfare:main` 5, `pwp_core_client:network` 6), поэтому старый клиент не зайдёт на новый сервер, и наоборот.

## 0. Подготовка
1. Игроков предупредить, остановить лобби, матчи, ботов и шедулер (bat на рабочем столе правит только пользователь).
2. Бэкап: дамп БД `pwp_core` (`cmd /c "mysqldump ... pwp_core > backup.sql"`, не через PowerShell-пайп), копия мира лобби и `match_template`, копия текущих jar из `PWP-Server` и `Pwpfiles/mods`.

## 1. Сборка
```
git fetch && git checkout rewrite/internals
gradlew build -x test
gradlew test
gradlew reobfJar
cd core-service && ..\gradlew shadowJar
```
Проверить SRG: `unzip -p pwp-core-client/build/libs/pwp-core-client-*.jar com/pwp/coreclient/mixin/WindowIconMixin.class | grep -a m_91268_` — должно найтись.

## 2. core-service
1. Заменить `core-service-1.0.0.jar`.
2. В боевом `config.json` выставить `"server": { "host": "127.0.0.1", ... }`: core теперь слушает адрес из конфига, снаружи ходят только через Caddy. Если к :8080 кто-то ходит напрямую с другой машины — оставить `0.0.0.0` (тогда C14 не действует).
3. Запустить и проверить `https://pigeo.asuscomm.com/api/v1/health`, затем страницу `https://pigeo.asuscomm.com/admin/dashboard` (ввести API-ключ).
4. На первом старте core сам выполнит: `players.role` DEFAULT `'user'`, `UPDATE players SET role='user' WHERE role IN ('', 'PLAYER') OR role IS NULL`. Таблицы больше НЕ дропаются на старте.

**Важно (18.11.2026):** моды теперь ходят на `http://127.0.0.1:8080` (было `localhost` — под Forge он резолвится в `::1`, и при бинде core на 127.0.0.1 лобби теряло core: «пустые» профили). Обновлять core-service и pwp-api/pwp-core-server ВМЕСТЕ.

## 3. Серверные моды
Заменить jar из `*/build/libs` в `mods` лобби и в `match_template/mods` (и в уже созданных `match_*`, если они переиспользуются): pwp-api, pwp-blast-protection, pwp-core-client, pwp-core-server, pwp-cosmetics, pwp-limits (см. п. 4.3), pwp-lobby (только лобби), pwp-medicine, pwp-movement, pwp-warfare, pwp-drone (где он есть), sbwchunkload (только серверы). Старые копии `sbwchunkload-*.jar` удалить.

## 4. Клиент (Pwpfiles → лаунчер)
1. В `Pwpfiles/mods` заменить jar на новые и **добавить `pwp-api-1.0.0.jar`** (сейчас его там нет, а от него зависят warfare, lobby и core-client — без него игра не запустится).
2. Закоммитить и уже лежащие в индексе удаления `tacz/endlessammo.zip`, `gucci_vuitton_attachment.zip`, `maxstuff.zip`, `tacz_default_gun.zip` — это битые паки (инцидент 16.11.2026).
3. **`pwp-limits`:** в Pwpfiles лежит `1.0.3` (в нём есть `LimitsConfigPacket`/`ServerEvents`/`LimitsConfigCache`), а в репо исходники только `1.0.2`, без этого кода. Решить до релиза: найти исходники 1.0.3 и закоммитить их в репо, либо сознательно откатиться на 1.0.2 (на серверах и у клиентов должна быть ОДНА версия).
4. `powershell -File gen.ps1` → `manifest.json` + `manifest_seed.sql`.
5. Подписать: `node <PWP-Launcher>\scripts\sign-file.mjs manifest.json` (ключ `%APPDATA%\.pwplauncher\.private\ed25519-private.pem`) → `manifest.json.sig`.
6. `manifest_seed.sql` выполнить в боевой БД: `cmd /c "mysql ... pwp_core < manifest_seed.sql"`.
7. Commit + push Pwpfiles.

## 5. Боты и шедулер
Обновить `bots/` и `pwp-scheduler/scheduler.py` из ветки. `bots/scheduler_loop.py` удалён (не использовался). Боты читают `bots/scheduler_status.json` от `pwp-scheduler`. Если `message_id_file` шедулера лежит не в `bots/`, задать env `PWP_SCHEDULER_STATUS=<путь к scheduler_status.json>`. Запустить `py run.py` и шедулер.

## 6. Проверка после релиза
- Вход через лаунчер, 2FA, профиль, магазин, кейсы, скины. Отказ доступа — строка `Session denied (server only|foreign uuid)` в логе core; путь добавить в правила `auth/AccessPolicy` или поправить вызов.
- Матч вдвоём (по игроку в каждой команде): на карте M и в деплое свои ФОБы, маркеры и техника видны, чужих нет; медик видит раненого союзника на компасе; креатив видит всё.
- РЭБ: только через стройку, демонтаж только своей командой и без возврата предмета.
- Плашка «ПОДГОТОВКА · М:СС» не налезает на верхний HUD.
- Дашборд: у лобби и матча есть MSPT/TPS — снять цифры для `AUDIT_INTERNALS.md`.
- Сменить API-ключ и `p5Secret` (они в git) — в `config.json`, `config/pwpcore.txt` серверов и `bots/config.py`.

## Откат
Вернуть jar и `config.json` из бэкапа п. 0, в Pwpfiles сделать `git revert` релизного коммита (+ заново `gen.ps1` и подпись), БД — из дампа.
