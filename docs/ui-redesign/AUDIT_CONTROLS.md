# Аудит клавиш (KeyMapping) сборки PWP — 17.11.2026

Цель: белый список клавиш для экрана «Управление» PWP. Источники: код `pwp-*` (ветка `rewrite/controls` + незакоммиченное в основном дереве `pwp`),
байткод прод-jar'ов `Pwpfiles/mods` (javap по классам, где вызывается `KeyMapping.<init>`), lang-файлы jar'ов, ванильные `Options.keyMappings` 1.20.1 (по памяти).
Всего ~160 клавиш: ваниль 34, наши 15 (13 warfare + 2 core-client), сторонние 111 в 25 модах.
Дефолты — коды GLFW из байткода; «—» = не назначена (`InputConstants.UNKNOWN`/-1); «?» = не установлено. Модификатор указан как `Alt+`/`Ctrl+`.
Контекст Forge (`KeyConflictContext`) указан только там, где он не IN_GAME или важен для конфликтов.

Важные факты:
- Прод-jar'ы `pwp-warfare-1.0.1`/`pwp-core-client-1.0.1` старее исходников: в проде нет голосований/арты (6 клавиш), а лобби там — `key.pwp_lobby.open_menu` на **Tab** (UNIVERSAL).
  В исходниках `rewrite/controls` клавиши лобби нет вообще; в основном дереве (незакоммичено) — `key.pwp_core.lobby_menu` J и `key.pwp_core.admin_panel` F6.
- Ваниль: F3/F5 принудительно гасятся `pwp-limits` (`pwp-limits/.../limit/client/ClientEvents.java:29`) в Survival/Adventure.
- Не регистрируют своих клавиш: `uncomplicated-fpv` (читает `superbwarfare` FIRE и др. из `DroneInputHandler`), `superb_extras` (парашют на существующих клавишах),
  `vvp` (в lang есть 6 `key.vvp.*`, но конструктора KeyMapping в jar нет — мёртвые строки), `sbw_mavic_extender`, `superbwarfaredronehud`, `CameraOverhaul`, `suppressionmod`, `Gunfire Overhaul`.
- Конфликты дефолтов (одна клавиша — несколько действий в бою): **K** (наш деплой, SBW тепловизор, Iris вкл. шейдеров, KeybindAtlas, лазер taczadditions),
  **G** (командное радио, выбор режима огня tacz, Curios, группы voicechat), **B** (радио отряда, режим огня pointblank, РЭБ-рюкзак WRB), **X** (ящик снабжения, SBW interact/захват цели, рывок экзоскелета),
  **R** (перезарядка tacz/SBW/pointblank + ловушки FPV + ParCool + Iris reload), **V** (ближний бой tacz/SBW, зум tacz, ловушки SBW, прицел pointblank, меню voicechat, двигатель dragonrise),
  **Tab** (карта, список игроков, быстрая смена оружия taczaddon), **Q/E** (наклоны двух модов vs выбросить/инвентарь), **1/2/3** (Explosion Overhaul vs хотбар), **F6/F8** (FCP vs админка/голосование), **PgUp/PgDn** (арта vs чувствительность SBW), **ЛAlt** (выход из техники vs Incremental Lean).

| id | мод | дефолт | категория мода | нужна PWP-игроку | категория экрана |
|---|---|---|---|---|---|
| key.forward | minecraft | W | key.categories.movement | да | Движение |
| key.back | minecraft | S | key.categories.movement | да | Движение |
| key.left | minecraft | A | key.categories.movement | да | Движение |
| key.right | minecraft | D | key.categories.movement | да | Движение |
| key.jump | minecraft | Space | key.categories.movement | да | Движение |
| key.sneak | minecraft | ЛShift | key.categories.movement | да | Движение |
| key.sprint | minecraft | ЛCtrl | key.categories.movement | да | Движение |
| key.attack | minecraft | ЛКМ | key.categories.gameplay | да | Бой и оружие |
| key.use | minecraft | ПКМ | key.categories.gameplay | да | Бой и оружие |
| key.pickItem | minecraft | СКМ | key.categories.gameplay | нет | — |
| key.drop | minecraft | Q | key.categories.inventory | редко | Бой и оружие |
| key.inventory | minecraft | E | key.categories.inventory | да | Интерфейс |
| key.swapOffhand | minecraft | F | key.categories.inventory | редко | Бой и оружие |
| key.hotbar.1 … key.hotbar.9 (9 шт.) | minecraft | 1 … 9 | key.categories.inventory | да | Бой и оружие |
| key.saveToolbarActivator | minecraft | C | key.categories.creative | нет | — |
| key.loadToolbarActivator | minecraft | X | key.categories.creative | нет | — |
| key.chat | minecraft | T | key.categories.multiplayer | да | Отряд и связь |
| key.command | minecraft | / | key.categories.multiplayer | редко | Отряд и связь |
| key.playerlist | minecraft | Tab | key.categories.multiplayer | да | Интерфейс |
| key.socialInteractions | minecraft | P | key.categories.multiplayer | нет | — |
| key.screenshot | minecraft | F2 | key.categories.misc | редко | Интерфейс |
| key.togglePerspective | minecraft | F5 | key.categories.misc | нет (блок pwp-limits) | — |
| key.smoothCamera | minecraft | — | key.categories.misc | нет | — |
| key.fullscreen | minecraft | F11 | key.categories.misc | редко | Интерфейс |
| key.spectatorOutlines | minecraft | — | key.categories.misc | нет | — |
| key.advancements | minecraft | L | key.categories.misc | нет | — |
| key.pwpwarfare.open_squad_menu | pwp-warfare | K | key.categories.pwpwarfare | да | Отряд и связь |
| key.pwpwarfare.show_map | pwp-warfare | Tab | key.categories.pwpwarfare | да | Интерфейс |
| key.pwpwarfare.place_ping | pwp-warfare | СКМ | key.categories.pwpwarfare | да | Отряд и связь |
| key.pwpwarfare.show_nicknames | pwp-warfare | ЛShift (UNIVERSAL) | key.categories.pwpwarfare | да | Отряд и связь |
| key.pwpwarfare.drop_supply | pwp-warfare | X | key.categories.pwpwarfare | да | Отряд и связь |
| key.pwpwarfare.squad_ptt | pwp-warfare | B | key.categories.pwpwarfare | да | Голос |
| key.pwpwarfare.command_ptt | pwp-warfare | G | key.categories.pwpwarfare | да | Голос |
| key.pwpwarfare.vote_start_yes | pwp-warfare | F9 | key.categories.pwpwarfare | да | Отряд и связь |
| key.pwpwarfare.vote_start_no | pwp-warfare | F10 | key.categories.pwpwarfare | да | Отряд и связь |
| key.pwpwarfare.vote_cmd_yes | pwp-warfare | F7 | key.categories.pwpwarfare | да | Отряд и связь |
| key.pwpwarfare.vote_cmd_no | pwp-warfare | F8 | key.categories.pwpwarfare | да | Отряд и связь |
| key.pwpwarfare.art_confirm | pwp-warfare | PgUp | key.categories.pwpwarfare | редко (командир) | Отряд и связь |
| key.pwpwarfare.art_deny | pwp-warfare | PgDn | key.categories.pwpwarfare | редко (командир) | Отряд и связь |
| key.pwp_core.lobby_menu | pwp-core-client (незакоммичено) | J | key.categories.pwp | да | Интерфейс |
| key.pwp_core.admin_panel | pwp-core-client (незакоммичено) | F6 | key.categories.pwp | нет (только админ) | — |
| key.pwp_lobby.open_menu | pwp-core-client 1.0.1 (только прод-jar) | Tab (UNIVERSAL) | key.categories.pwp | нет (уходит при обновлении) | — |
| key.tacz.shoot.desc | tacz | ЛКМ | key.category.tacz | да | Бой и оружие |
| key.tacz.aim.desc | tacz | ПКМ | key.category.tacz | да | Прицел и оптика |
| key.tacz.reload.desc | tacz | R | key.category.tacz | да | Бой и оружие |
| key.tacz.fire_select.desc | tacz | G | key.category.tacz | да | Бой и оружие |
| key.tacz.interact.desc | tacz | O | key.category.tacz | да | Бой и оружие |
| key.tacz.melee.desc | tacz | V | key.category.tacz | редко | Бой и оружие |
| key.tacz.zoom.desc | tacz | V | key.category.tacz | редко | Прицел и оптика |
| key.tacz.inspect.desc | tacz | H | key.category.tacz | редко | Бой и оружие |
| key.tacz.crawl.desc | tacz | C | key.category.tacz | редко (?) | Движение |
| key.tacz.refit.desc | tacz | Z | key.category.tacz | нет | — |
| key.tacz.open_config.desc | tacz | Alt+T | key.category.tacz | нет | — |
| key.reduceSensitivity | tacz-tweaks | — | key.category.tacz | нет | — |
| key.tiltGun | tacz-tweaks | — | key.category.tacz | нет | — |
| key.unload | tacz-tweaks | Ctrl+R | key.category.tacz | нет | — |
| key.taczadditions.toggle_laser | taczadditions | K | key.categories.taczadditions | редко | Прицел и оптика |
| key.taczaddon.switch_gun.desc | taczaddon | Tab | key.categories.taczaddon | редко | Бой и оружие |
| key.pointblack.reload | pointblank | R | key.categories.pointblank | да | Бой и оружие |
| key.pointblack.firemode | pointblank | B | key.categories.pointblank | редко | Бой и оружие |
| key.pointblack.scope_switch | pointblank | V | key.categories.pointblank | редко | Прицел и оптика |
| key.pointblack.inspect | pointblank | I | key.categories.pointblank | нет | — |
| key.pointblack.attachments | pointblank | Y | key.categories.pointblank | нет | — |
| key.pointblack.settings | pointblank | Alt+Y | key.categories.pointblank | нет | — |
| key.lrtactical.normal_attack.desc | lrtactical | ЛКМ | key.category.lrtactical | редко | Бой и оружие |
| key.lrtactical.sp_attack.desc | lrtactical | ПКМ | key.category.lrtactical | редко | Бой и оружие |
| key.superbwarfare.fire | superbwarfare | ЛКМ | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.hold_zoom | superbwarfare | ПКМ | key.categories.superbwarfare | да | Прицел и оптика |
| key.superbwarfare.switch_zoom | superbwarfare | — | key.categories.superbwarfare | редко | Прицел и оптика |
| key.superbwarfare.breath | superbwarfare | ЛCtrl | key.categories.superbwarfare | да | Прицел и оптика |
| key.superbwarfare.reload | superbwarfare | R | key.categories.superbwarfare | да | Бой и оружие |
| key.superbwarfare.fire_mode | superbwarfare | N | key.categories.superbwarfare | да | Бой и оружие |
| key.superbwarfare.melee | superbwarfare | V | key.categories.superbwarfare | редко | Бой и оружие |
| key.superbwarfare.sensitivity_increase | superbwarfare | PgUp | key.categories.superbwarfare | редко | Прицел и оптика |
| key.superbwarfare.sensitivity_reduce | superbwarfare | PgDn | key.categories.superbwarfare | редко | Прицел и оптика |
| key.superbwarfare.move_forward | superbwarfare | W | key.categories.superbwarfare | редко (дубль W) | Техника |
| key.superbwarfare.move_backward | superbwarfare | S | key.categories.superbwarfare | редко (дубль S) | Техника |
| key.superbwarfare.move_left | superbwarfare | A | key.categories.superbwarfare | редко (дубль A) | Техника |
| key.superbwarfare.move_right | superbwarfare | D | key.categories.superbwarfare | редко (дубль D) | Техника |
| key.superbwarfare.move_space | superbwarfare | Space | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.move_shift | superbwarfare | ЛShift | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.move_ctrl | superbwarfare | ЛCtrl | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.dismount | superbwarfare | ЛAlt | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.free_camera | superbwarfare | C | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.change_ammo_forward | superbwarfare | → | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.change_ammo_backward | superbwarfare | ← | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.change_fire_mode_forward | superbwarfare | ↑ | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.change_fire_mode_backward | superbwarfare | ↓ | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.release_decoy | superbwarfare | V | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.vehicle_seek | superbwarfare | X | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.active_thermal_imaging | superbwarfare | K | key.categories.superbwarfare | да | Техника |
| key.superbwarfare.mark | superbwarfare | СКМ | key.categories.superbwarfare | редко | Техника |
| key.superbwarfare.interact | superbwarfare | X | key.categories.superbwarfare | редко | Техника |
| key.superbwarfare.unload | superbwarfare | — | key.categories.superbwarfare | нет | — |
| key.superbwarfare.edit_mode | superbwarfare | H | key.categories.superbwarfare | нет | — |
| key.superbwarfare.config | superbwarfare | Alt+O | key.categories.superbwarfare | нет | — |
| dragonrise_reforge.keyinfo.enginechangekey | dragonrise_reforge | V | dragonrise_reforge | редко | Техника |
| key.sbw_ew_addon.release_flares | sbw_ew_addon | R | key.categories.sbw_ew_addon | да | Дрон |
| key.sbw_ew_addon.hack_drone | sbw_ew_addon | Y | key.categories.sbw_ew_addon | редко | Дрон |
| key.warbornrenewed.nvg_toggle | WRB-Armor | N | key.categories.warbornrenewed | да | Прицел и оптика |
| key.warbornrenewed.reb_backpack_toggle | WRB-Armor | B | key.categories.warbornrenewed | редко | Дрон |
| key.push_to_talk | voicechat | — (назначается онбордингом) | key.categories.voicechat | да | Голос |
| key.whisper | voicechat | — | key.categories.voicechat | редко | Голос |
| key.mute_microphone | voicechat | M | key.categories.voicechat | да | Голос |
| key.disable_voice_chat | voicechat | N | key.categories.voicechat | редко | Голос |
| key.voice_chat | voicechat | V | key.categories.voicechat | редко | Голос |
| key.voice_chat_settings | voicechat | — | key.categories.voicechat | редко | Голос |
| key.voice_chat_adjust_volumes | voicechat | — | key.categories.voicechat | редко | Голос |
| key.voice_chat_group | voicechat | G | key.categories.voicechat | нет (отряды PWP) | — |
| key.hide_icons | voicechat | H | key.categories.voicechat | нет | — |
| key.voice_chat_toggle_recording | voicechat | — | key.categories.voicechat | нет | — |
| key.tactical.leanleft | tacticalmovement | Q | keys.categories.tactical | да (спорно: дубль leaningornot) | Движение |
| key.tactical.leanright | tacticalmovement | E | keys.categories.tactical | да (спорно: дубль leaningornot) | Движение |
| key.leaningornot.lean_left | leaningornot | Q | key.categories.leaningornot | да (спорно: дубль tacticalmovement) | Движение |
| key.leaningornot.lean_right | leaningornot | E | key.categories.leaningornot | да (спорно: дубль tacticalmovement) | Движение |
| key.leaningornot.left_alt | leaningornot | ЛAlt | key.categories.leaningornot | нет | — |
| key.leaningornot.increment_left | leaningornot | A | key.categories.leaningornot | нет | — |
| key.leaningornot.increment_right | leaningornot | D | key.categories.leaningornot | нет | — |
| key.survival_instinct.exo_suit_dash | survival_instinct | X | key.categories.movement | редко | Движение |
| key.parcool.Vault | ParCool | ПКМ | key.categories.parcool | редко (?) | Движение |
| key.parcool.Crawl | ParCool | C | key.categories.parcool | редко (?) | Движение |
| key.parcool.Breakfall | ParCool | R | key.categories.parcool | редко (?) | Движение |
| key.parcool.Dodge | ParCool | R | key.categories.parcool | нет (?) | — |
| key.parcool.FastRun | ParCool | ЛCtrl | key.categories.parcool | нет (?) | — |
| key.parcool.WallJump | ParCool | Space | key.categories.parcool | нет (?) | — |
| key.parcool.ClingToCliff | ParCool | ПКМ | key.categories.parcool | нет (?) | — |
| key.parcool.HangDown | ParCool | ПКМ | key.categories.parcool | нет (?) | — |
| key.parcool.WallSlide | ParCool | ПКМ | key.categories.parcool | нет (?) | — |
| key.parcool.RideZipline | ParCool | ПКМ | key.categories.parcool | нет | — |
| key.parcool.HorizontalWallRun | ParCool | R | key.categories.parcool | нет | — |
| key.parcool.HideInBlock | ParCool | C | key.categories.parcool | нет | — |
| key.parcool.Flipping | ParCool | — | key.categories.parcool | нет | — |
| key.parcool.QuickTurn | ParCool | — | key.categories.parcool | нет | — |
| key.parcool.Enable | ParCool | Ctrl+P (UNIVERSAL) | key.categories.parcool | нет | — |
| key.parcool.openSetting | ParCool | Alt+P (UNIVERSAL) | key.categories.parcool | нет | — |
| key.curios.open.desc | curios | G | key.curios.category | нет | — |
| key.explosionoverhaul.accept_scan | Explosion-Overhaul | 1 | key.categories.explosionoverhaul | нет | — |
| key.explosionoverhaul.decline_scan | Explosion-Overhaul | 2 | key.categories.explosionoverhaul | нет | — |
| key.explosionoverhaul.info_scan | Explosion-Overhaul | 3 | key.categories.explosionoverhaul | нет | — |
| key.fcp.vehicle_debug | fcp | F6 | key.categories.fcp | нет | — |
| key.fcp.open_config | fcp | F8 | key.categories.fcp | нет | — |
| iris.keybind.reload | oculus | R (контекст UNIVERSAL) | iris.keybinds | нет | — |
| iris.keybind.toggleShaders | oculus | K (контекст UNIVERSAL) | iris.keybinds | нет | — |
| iris.keybind.shaderPackSelection | oculus | O (контекст UNIVERSAL) | iris.keybinds | редко | Интерфейс |
| iris.keybind.wireframe | oculus | — (?) | iris.keybinds | нет | — |
| key.keybindatlas.open_keyboard | KeybindAtlas | K | key.categories.misc | нет (заменяется экраном PWP) | — |
| key.tctcore.tc_tcore_key | TCTcore | H | key.categories.ui | нет (?) | — |
| key.kiwi.builders_button2 | Kiwi | ? | key.categories.gameplay | нет | — |
| key.entityculling.toggle | entityculling | — | text.entityculling.title | нет | — |
| key.entityculling.toggleBoxes | entityculling | — | text.entityculling.title | нет | — |
| key.modernfix.config | modernfix | — (UNIVERSAL) | key.modernfix | нет | — |
| key.komodo.kmodo_debug.toggle | komodo | — | key.categories.komodo.debug | нет | — |
| key.komodo.kmodo_debug.dump | komodo | — | key.categories.komodo.debug | нет | — |

Примечания к таблице:
- Контекст Iris: конструктор `KeyMapping(String, Type, int, String)` без контекста = UNIVERSAL (по умолчанию Forge), поэтому K/R Iris конфликтуют с игровыми K/R по-настоящему.
- ParCool: роль в PWP не выяснена (включён ли паркур на сервере) — оценки «?» поставлены по смыслу. `key.parcool.ClimbPoles` есть в lang, но не регистрируется.
- `key.superbwarfare.launch` есть в lang SBW, но в `ModKeyMappings` не регистрируется.
- Где регистрируются: SBW — `com/atsuishio/superbwarfare/init/ModKeyMappings` (id = `key.superbwarfare.` + имя), voicechat — `de/maxhenkel/voicechat/voice/client/KeyEvents`,
  tacz — `com/tacz/guns/client/input/*Key`, Iris — `net/irisshaders/iris/Iris`.

## Основные (предложение)

Порядок показа в экране «Управление» (белый список «да»; «редко» — под раскрывающимся «Дополнительно» в своей категории).

**Движение:** key.forward, key.back, key.left, key.right, key.jump, key.sprint, key.sneak, key.tactical.leanleft, key.tactical.leanright (либо пара leaningornot — оставить один мод).
Дополнительно: key.tacz.crawl.desc, key.survival_instinct.exo_suit_dash, key.parcool.Vault, key.parcool.Crawl, key.parcool.Breakfall.

**Бой и оружие:** key.attack, key.tacz.shoot.desc, key.use, key.tacz.reload.desc, key.superbwarfare.reload, key.pointblack.reload, key.tacz.fire_select.desc, key.superbwarfare.fire_mode, key.tacz.interact.desc, key.hotbar.1 … key.hotbar.9.
Дополнительно: key.tacz.melee.desc, key.superbwarfare.melee, key.tacz.inspect.desc, key.pointblack.firemode, key.taczaddon.switch_gun.desc, key.lrtactical.normal_attack.desc, key.lrtactical.sp_attack.desc, key.drop, key.swapOffhand.

**Прицел и оптика:** key.tacz.aim.desc, key.superbwarfare.hold_zoom, key.superbwarfare.breath, key.warbornrenewed.nvg_toggle.
Дополнительно: key.tacz.zoom.desc, key.pointblack.scope_switch, key.superbwarfare.switch_zoom, key.taczadditions.toggle_laser, key.superbwarfare.sensitivity_increase, key.superbwarfare.sensitivity_reduce.

**Техника:** key.superbwarfare.fire, key.superbwarfare.move_ctrl, key.superbwarfare.move_shift, key.superbwarfare.move_space, key.superbwarfare.dismount, key.superbwarfare.free_camera, key.superbwarfare.change_ammo_forward, key.superbwarfare.change_ammo_backward, key.superbwarfare.change_fire_mode_forward, key.superbwarfare.change_fire_mode_backward, key.superbwarfare.vehicle_seek, key.superbwarfare.release_decoy, key.superbwarfare.active_thermal_imaging.
Дополнительно: key.superbwarfare.move_forward, key.superbwarfare.move_backward, key.superbwarfare.move_left, key.superbwarfare.move_right, key.superbwarfare.mark, key.superbwarfare.interact, dragonrise_reforge.keyinfo.enginechangekey.

**Дрон:** key.sbw_ew_addon.release_flares.
Дополнительно: key.sbw_ew_addon.hack_drone, key.warbornrenewed.reb_backpack_toggle.
(Управление FPV/мавиком идёт через клавиши SBW «Техника» — `uncomplicated-fpv` своих не имеет.)

**Отряд и связь:** key.pwpwarfare.open_squad_menu, key.pwpwarfare.place_ping, key.pwpwarfare.drop_supply, key.pwpwarfare.show_nicknames, key.chat, key.pwpwarfare.vote_start_yes, key.pwpwarfare.vote_start_no, key.pwpwarfare.vote_cmd_yes, key.pwpwarfare.vote_cmd_no.
Дополнительно: key.pwpwarfare.art_confirm, key.pwpwarfare.art_deny, key.command.

**Голос:** key.push_to_talk, key.pwpwarfare.squad_ptt, key.pwpwarfare.command_ptt, key.mute_microphone.
Дополнительно: key.whisper, key.disable_voice_chat, key.voice_chat, key.voice_chat_settings, key.voice_chat_adjust_volumes.

**Интерфейс:** key.pwpwarfare.show_map, key.inventory, key.playerlist, key.pwp_core.lobby_menu.
Дополнительно: key.screenshot, key.fullscreen, iris.keybind.shaderPackSelection.
