# План: полный реворк деплой-экрана с нуля (стенд MAINTEST)

Дата: 07.08.2026. Цель: переписать ВСЕ GUI-классы стенда начисто по Squad-идее
(не патчить текущий баганый код). Иконки — оригинальные Squad (уже скачаны:
88 файлов, конвертированы в PNG, проверены на валидность в jar).

## Корень багов прошлой версии
1. Геометрия считалась заново в каждом методе (рендер/hover/клик — 3 копии одних
   координат) → рассинхрон панелей.
2. Глобальные статические состояния (DeployData.*) + «sticky»-магия в рендере.
3. Overlay-стрип спавнов, не синхронизированный с картой.
4. Тултипы рисовались внутри панелей и перекрывались соседними.

## Новая архитектура
- `DeployLayout` (NEW): единственная раскладка 25/25/50. Record Rect(x,y,w,h).
  Один метод compute(width,height) → все зоны: topBar, tabs, left, roles, spawns,
  chat, dyn, loadoutSlots, soldier, bottomBar. Рендер и клики читают только его.
- `DeployState` (NEW): единое состояние (selectedKit, selectedSpawn, activeTab,
  roleUI/roleFade, hoveredKit, expandedSquads, дропдауны слотов, rulesScroll).
  API слотов: selectedIndex/setSelectedIndex/isSlotExpanded/setSlotExpanded/
  closeAllDropdowns. Панели не пишут друг в друга.
- Сигнатура всех панелей: render(GuiGraphics, Rect, mx, my, DeployState) +
  mouseClicked(...) → результат/действие. Тултипы рисует ЭКРАН в самом конце.

## Переписываемые файлы (с нуля)
1. `DeployLayout.java` (NEW)
2. `DeployState.java` (NEW)
3. `SpawnListPanel.java` (NEW, замена SpawnStripPanel — DELETE старый)
4. `SquadsPanel.java` (rewrite)
5. `RolesPanel.java` (rewrite)
6. `ChatPanel.java` (rewrite, rect-based)
7. `LoadoutPanel.java` (rewrite)
8. `PortraitRenderer.java` (rewrite под Rect)
9. `TestMapRenderer.java` (rewrite рендер-слоя)
10. `TestDeployScreen.java` (rewrite)
11. `DeployData.java` (убрать UI-state statics → DeployState; данные/парсинг/
    display names/icon mapping остаются; добавить firstSafeSpawn())

Остаётся без изменений (рабочее): HoverState, DeploySounds, PWPPanel, PWPButton,
RoundedRect, WeaponPreviewRenderer, WeaponTooltipRenderer, Anim, TaczHelper,
TaczStats, PWPTheme, ClientDataStub, DeployTestMod.

## Детали панелей

### DeployLayout
- TOP_H=34, TAB_H=22, BOT_H=38, INSET=4, GAP=4.
- sqW=25%, cw=25%, dyn=50%.
- roleH = clamp(conH*34%, 190..270); dollH = clamp(conH*40%, 140..240).
- chatH=62 прижата к низу; spawns между roles и chat.

### DeployState
Как выше. animStart — для фейда дропдауна (120мс).

### SquadsPanel
- Шаблон PWPPanel (линия ACCENT + заголовок SQUADS + полупрозрачный фон).
- Commander Pending 34px: рамка ACCENT сверху, ★ COMMANDER PENDING, VOLUNTEER
  (hover → жёлтый фон), HoverState.
- Строка отряда 40px: [квадрат-номер 14][имя/лидер][n/9][JOIN фикс. 46px справа |
  красный замок | шеврон ▼/▲]. Hover → светлее (HoverState), звук HOVER.
- Раскрытие: тонкая зелёная линия слева (SUCCESS 2px), буквы A/B/C слева,
  строки игроков 24px, бейджи SL/FTL/CTL справа.
- Низ: CREATE/LEAVE (зелёная/красная, hover ярче) + INVITE (серый, невидимый,
  активен только лидеру).
- Скролл колесом + тонкий скроллбар при переполнении.
- mouseClicked → "VOLUNTEER"/"JOIN:<id>"/"CREATE"/"LEAVE"/toggle expand.

### RolesPanel
- Заголовок ROLE SELECT + разделитель (прозрачная секция, без рамки).
- Категории: имя + линия-разделитель (COMMAND ─── INFANTRY ───), 2px зазор.
- Ячейки 36×36 (адаптив 36→22): оригинальные жёлтые kit-иконки Squad
  (textures/gui/squad/kits/<squadKitIconFile>.png, 64×64).
- Три состояния: normal (прозрачно) → hover (фон SURFACE_LIGHT + рамка ACCENT,
  плавно) → selected (фон 0x44C8812A + рамка ACCENT). Disabled — иконка серая
  (tint 0.45) + тултип причины с задержкой 400мс.
- Счётчики n/m внизу ячейки (DANGER при полном).
- Счётчик лимита FIRE_SUPPORT на категории (3/9) — мелко справа от заголовка.
- hoveredKit sticky → в DeployState.hoveredKit (кормит превью лоаудаута).
- renderTooltip() вызывается ЭКРАНОМ в конце (поверх всех панелей).
- mouseClicked → имя кита / "" (disabled) / null (мимо); при выборе —
  closeAllDropdowns.

### SpawnListPanel (новая, вместо SpawnStripPanel)
- Заголовок SPAWN SELECT + разделитель; описание режима внизу rect'а.
- Строки 26px: [иконка MAIN/FOB/RALLY Squad][название (A1-2-3)][статус справа].
- hover → темнее (0x2212151A), выбранная — жёлтая рамка + bg 0x44C8812A,
  недоступная — затемнена (alpha 0.4) и не кликается.
- mouseClicked → id / "" (blocked) / null. Экран синхронизирует карту
  (selectedSpawn + centerOn). Никаких overlay-стрипов.

### ChatPanel
- Тонкая строка: [ALL/TEAM/SQD] бокс + инпут, 3 строки истории мелко и тускло.
- TAB — цикл канала, Enter — отправить (в history), Backspace — правка.
- charTyped — кириллица.

### LoadoutPanel
- Заголовок "LOADOUT : <РОЛЬ>" (роль из DeployState.hoveredKit ?: selectedKit).
- PRIMARY — большая карточка 64px: имя, [N] магазины, 3D-превью справа,
  шеврон ▼ если есть альты.
- WEAPON DROPDOWN: открывается РОВНО под Primary (x карточки, y после неё),
  ширина min(270, w), строки 44px: 3D-превью 28 + название + [N]; hover →
  зелёная линия слева (2px SUCCESS); выбранный — рамка ACCENT; фон PANEL_BG_SOLID.
  Открытие одного слота закрывает другие (closeAllDropdowns). Закрытие: клик
  вне дропдауна ИЛИ после выбора. Fade 120мс через animStart.
- SECONDARY/SPECIAL — карточки 30px (иконка предмета + имя + [N] у вторички).
- BACKPACK — сетка 2×3, ячейки 28px, тултип названия с задержкой.
- Статы PRIMARY — реальные TACZ (TaczHelper.getGunStats): Magazine Capacity /
  Rate of Fire / Caliber / Fire Mode; не-TACZ → блок скрыт. Внизу области слотов.
- hoveredWeapon() для 3D-тултипа (зеркало layout — один проход rect'ов).
- mouseClicked: дропдаун → выбор+закрытие+звук WEAPON; карточка с альтами →
  toggle; клик мимо открытого дропдауна → закрыть (вернуть true).

### PortraitRenderer
- Панель PANEL_BG + тонкая рамка, углы 90°.
- Кукла чуть правее центра (x + w*0.58), ступни у низа, scissor.
- Драг ЛКМ вращает по Y (живёт пока ЛКМ зажата), ПКМ — сброс.
- Снизу слева: имя роли (CAPS) + зелёная полоска 3px + описание (перенос строк).
- Смена экипировки игрока на кит (weapon+armor) с restore.

### TestMapRenderer
- Иконки оригинальные Squad (squad/map/*.png, 64×64):
  - спавны: MAIN→map_base, RALLY→map_icon_rp, FOB→map_fob
  - self→map_icon_self (вращается по yRot), игроки→grunt/sqdldr/medic (тины)
  - точки захвата→map_flags + map_attack_defend (стрелка под флагом)
  - техника→tank_vehicle_icon/ifv_vehicle_icon (красный тинт врагам)
- Сетка: 100м тонкие (alpha 0.10) + 500м заметные (alpha 0.30); подписи A.. сверху,
  1.. слева (белые, тонкие, тёмная подложка).
- Координаты A1-2-3 (500м ячейка + 100м суб) — в тултипах/статус-строке/списке.
- Статус-ринг спавна (SUCCESS/WARNING/DANGER), hover → иконка +2px + тултип
  с задержкой 350мс; клик → выбор (заблокированный не выбирается);
  клик по пустому месту — ничего (только драг/зум).
- Зум колесом ×1.2 и кнопки [+]/[−], пан ЛКМ-драгом.
- Состояние (центр/зум) переживает переключение карта⇄лоаудат.

### TestDeployScreen
- computeLayout() один раз на кадр; все рендеры/клики по rect'ам.
- Top bar: DEPLOYMENT слева (заголовок), центр — иконка тикетов + число +
  " · STAGING PHASE mm:ss", справа — флаг Squad (usa/rgf) + USA/RUS.
- Вкладки КОМАНДЫ/ДЕПЛОЙ/ПРАВИЛА: hover — только осветление текста (HoverState),
  активная — белый текст + линия ACCENT 2px снизу; звук HOVER при входе.
- Таб 0 (КОМАНДЫ): две колонки игроков с флагами. Таб 2 (ПРАВИЛА): скролл.
- Bottom bar: CURRENT ROLE + kit-иконка 16 + имя (фикс. позиция) | центр
  RESPAWN IN 00:07 / ВЫБЕРИТЕ ТОЧКУ / ГОТОВ | справа красная SELECT SPAWN
  (активна: таймер 0 ∧ точка ∧ роль доступна; тултип причины при неактивности).
- State machine: hover над rect ролей → roleUI=true; курсор вне dyn →
  roleUI=false; roleFade кросс-фейд 90мс.
- Лоаудат: loadoutSlots rect + soldier rect (роль/описание), fade+slide.
- Карта: dyn rect, статус-строка сверху (точка + A1-2-3 + статус + дистанция).
- Клики по зонам: left→squads (звуки JOIN/LEAVE/SELECT), middle→roles (SELECT)/
  spawns (SELECT+синк карты)/chat, right→карта (выбор спавна, стрип-закрытие)
  или лоаудат (дропдаун/драг куклы/ПКМ-сброс). Клик вне лоаудаута →
  closeAllDropdowns.
- Хоткеи: Esc (дропдаун→закрыть), Enter (деплой/чат), Tab (канал), Backspace
  (чат), 1-9 (выбор роли по порядку), charTyped (кириллица).
- Звуки: HOVER/select/deploy/join/leave/weapon через DeploySounds.

### DeployData (правки)
- Убрать статики UI-состояния (expandedSlots, slotSelections, animStart и их
  методы) — переезжают в DeployState.
- Добавить static String firstSafeSpawn() (для инициализации состояния).
- Оставить: records, списки данных, populate/парсинг, KIT_DISPLAY_NAMES,
  getDisplayName, squadKitIconFile, squadFlagFile, kitIconFileName.

## Иконки (уже готовы, 88 шт в textures/gui/squad/)
- kits/ (жёлтые квадраты, 64×64), roles/ (силуэты), map/, flags/ (128×64),
  vehicles/. Маппинг китов/фракций в DeployData.

## РЕНДЕР ОРУЖИЯ: «настоящая» модель (эталон — верстак TACZ GunSmithTableScreen.renderLeftModel)

Проблема (жалоба: «тултип — ЛОД моделька, расположение конченое»):
1. `pose.scale(s, -s, min(s,60))` — Z сплющен → модель деформирована при yaw (плоская).
2. Текстура без билинейного фильтра (`setFilter(true,false)`) → пикселится = вид «ЛОД».
3. Нет `Lighting.setupForEntityInInventory()` → модель освещена «из мира», не как в инвентаре.
4. Плавающий у курсора тултип (WeaponTooltipRenderer) — в Squad его НЕТ; позиция
   (+12,+14 + кламп) — «ублюдская» по мнению юзера. УДАЛЯЕМ.

Решение — WeaponPreviewRenderer получает режим «настоящая модель» (по байткоду верстака):
1. `RenderDistance.markGuiRenderTimestamp()` (уже есть, оставить).
2. `textureManager.getTexture(display.getModelTexture()).setFilter(true, false)` —
   билинейная фильтрация (через guard-класс TaczHolder: GunDisplayInstance.getModelTexture()).
3. `Lighting.setupForEntityInInventory()` ПЕРЕД renderStatic + `Lighting.setupFor3DItems()`
   ПОСЛЕ (в finally).
4. `pose.scale(s, -s, s)` — БЕЗ z-сквоша (пункт 5 решает глубину).
5. Вокруг рендера `RenderSystem.disableDepthTest()` + скиссор области превью
   (модель поверх панелей, но в границах области; без конкуренции по глубине с GUI).
6. `gui.flush()` после.

Дизайн (вместо курсорного тултипа):
- PRIMARY-карточка = БОЛЬШАЯ ВИТРИНА: модель настоящего качества + статы TACZ.
- Hover по строке дропдауна → витрина ВРЕМЕННО показывает hover-ствол (модель+статы),
  уход — возврат к выбранному. Как «живой комбобокс».
- Строки дропдауна: превью 36px (качество то же).
- WeaponTooltipRenderer.java УДАЛИТЬ, вызов из TestDeployScreen убрать.

## Верификация
1. gradlew build (ОБЯЗАТЕЛЬНО с reobfJar — без него NoSuchMethodError в SRG).
2. Деплой в Prism-инстанс ТЕСТИНГ (copy jar в mods).
3. Проверка в игре клавишей L.

## Заметки
- Строить ТОЛЬКО `gradlew build` (не -x reobfJar) — урок из краша 18:37.
- Комментарии на русском, без магических чисел — константы PWPTheme/DeployLayout.
- SpawnStripPanel.java удалить (заменён SpawnListPanel).
