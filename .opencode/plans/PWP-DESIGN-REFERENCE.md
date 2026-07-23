# PWP Design Reference — для дизайнера (Claude/CloaDA)

## ⚙️ Технические ограничения

- **Язык:** Java 17, Minecraft Forge 1.20.1
- **Рендер:** `GuiGraphics` (обёртка над Minecraft рендером)
- **Нет CSS/HTML/XML** — каждый пиксель рисуется вручную через `gui.fill()`, `gui.blit()`, `gui.drawString()`
- **Нет flexbox/grid** — позиционирование абсолютное через `x, y, width, height`
- **Нет DOM/React** — нет перерендера, нет компонентной модели
- **Прозрачность:** `RenderSystem.setShaderColor(1,1,1,alpha)` или кастомный `withAlpha(color, alpha)`
- **Обрезка контента:** `gui.enableScissor(x, y, w, h)` / `gui.disableScissor()`
- **Анимации:** через `System.currentTimeMillis()` + easing-функции
- **Шрифты:** только Minecraft bitmap (`Minecraft.getInstance().font`), можно подключить TTF
- **Иконки:** либо Unicode-символы, либо текстуры через `ResourceLocation`
- **Локализация:** `Component.translatable("key")`, файлы `en_us.json`, `ru_ru.json`

---

## 1. ДИЗАЙН-СИСТЕМА (PWPTheme.java)

Всё централизовано в одном классе. Никаких магических чисел.

### Цвета (ARGB hex)

```java
// Фоны
BACKGROUND      = 0xFF0A0C0E;  // основной чёрный
BACKGROUND_DIM  = 0xCC06080A;  // затемнение

// Панели
SURFACE         = 0xFF12151A;  // карточки
SURFACE_LIGHT   = 0xFF1A1E26;  // ховер
SURFACE_DIM     = 0x6612151A;  // полупрозрачная

// Акцент (оранжевый)
ACCENT          = 0xFFC8812A;  // основной
ACCENT_DIM      = 0xFF8B6220;  // тёмный (обводки)
ACCENT_GLOW     = 0x18C8812A;  // свечение
ACCENT_BORDER   = 0xFF6B4A18;  // граница-тень

// Текст
TEXT_PRIMARY    = 0xFFC8CBCE;  // светло-серый
TEXT_SECONDARY  = 0xFF7A7D84;  // серый
TEXT_DIM        = 0xFF4A4D54;  // неактивный
TEXT_ACCENT     = 0xFFC8812A;  // оранжевый

// Состояния
SUCCESS = 0xFF3D7A40;  // зелёный
DANGER  = 0xFFA53D3D;  // красный
INFO    = 0xFF3D6FA5;  // синий

// Команды
TEAM_BLUE = 0xFF3D6FA5;
TEAM_RED  = 0xFFA53D3D;
```

### Отступы и размеры

```java
Spacing.XXS=2, XS=4, SM=8, MD=12, LG=16, XL=24, XXL=32, HUGE=48
Spacing.RADIUS_SMALL=3, RADIUS_MEDIUM=6, RADIUS_LARGE=10
Spacing.BUTTON_HEIGHT=24, BUTTON_WIDTH=120
```

### Иконки (Unicode)

```java
STAR="★", HEART="❤", CROSS="✖", CHECK="✔",
ARROW_UP="▲", ARROW_DOWN="▼", ARROW_LEFT="◀", ARROW_RIGHT="▶",
CIRCLE="●", DIAMOND="◆", SKULL="☠", STARBURST="✦",
CROWN="♛", GEAR="⚙", CROSSHAIR="⌖", SWORDS="⚔", SHIELD="⛨"
```

### Анимации

```java
DURATION_FAST=100ms, NORMAL=200ms, SLOW=400ms, GLACIAL=800ms
STAGGER_FAST=30ms, NORMAL=60ms, SLOW=100ms
```

---

## 2. КАК РИСУЮТСЯ КНОПКИ (PWPButton.java)

### Стили кнопок

```java
public enum Style { PRIMARY, ACCENT, DANGER, DARK, GHOST }
```

### Полный процесс рендера кнопки (шаг за шагом)

```java
@Override
protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
    // 1. Прозрачность анимации входа
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.animAlpha);

    // 2. Hover-анимация (lerp 0.15f за тик между 0 и 1)
    float dt = Math.min((now - lastTick) / 50.0F, 4.0F);
    float target = hovered && active ? 1.0F : 0.0F;
    if (target > hoverAnim) hoverAnim = Math.min(hoverAnim + 0.15F * dt, target);
    else hoverAnim = Math.max(hoverAnim - 0.12F * dt, target);

    // 3. Выбор цвета по стилю + hoverAnim lerp
    switch (style) {
        case ACCENT:  // Оранжевая залитая
            bg = lerpColor(ACCENT_BG, ACCENT_HOVER, hoverAnim);
            border = ACCENT_DIM;
            textColor = 0xFF0A0C0E;  // тёмный текст
            break;
        case DANGER:  // Красная залитая
            bg = lerpColor(DANGER_BG, DANGER_HOVER, hoverAnim);
            border = DANGER;
            textColor = TEXT_PRIMARY;
            break;
        case DARK:    // Тёмная без бордера
            bg = lerpColor(DARK_BG, DARK_HOVER, hoverAnim);
            border = hovered ? BORDER_FOCUS : DARK_BORDER;
            textColor = hovered ? TEXT_PRIMARY : TEXT_PRIMARY;
            break;
        case GHOST:   // Прозрачная, появляется на ховере
            bg = hoverAnim > 0.01f ? lerpColor(0, SURFACE_LIGHT, hoverAnim) : 0;
            border = hoverAnim > 0.01f ? lerpColor(0, BORDER_FOCUS, hoverAnim) : 0;
            textColor = hoverAnim > 0.01f ? TEXT_PRIMARY : TEXT_SECONDARY;
            break;
        default: // PRIMARY — тёмная с оранжевой обводкой
            bg = lerpColor(PRIMARY_BG, PRIMARY_HOVER, hoverAnim);
            border = active ? (hovered ? BORDER_FOCUS : PRIMARY_BORDER) : BORDER;
            textColor = active ? TEXT_PRIMARY : TEXT_SECONDARY;
            break;
    }

    // 4. Рисование скруглённого прямоугольника (4 fill'а)
    gui.fill(x + r, y, x + w - r, y + h, bg);           // центр
    gui.fill(x, y + r, x + r, y + h - r, bg);           // левый край
    gui.fill(x + w - r, y + r, x + w, y + h - r, bg);   // правый край
    gui.fill(x + r, y + r, x + w - r, y + h - r, bg);   // перекрытие

    // 5. Рисование границы (8 fill'ов — верх, низ, лево, право + углы)
    gui.fill(x + r, y, x + w - r, y + 1, border);
    gui.fill(x + r, y + h - 1, x + w - r, y + h, border);
    gui.fill(x, y + r, x + 1, y + h - r, border);
    gui.fill(x + w - 1, y + r, x + w, y + h - r, border);
    gui.fill(x + r, y, x + r + 1, y + 1, border);
    gui.fill(x + w - r - 1, y, x + w - r, y + 1, border);
    gui.fill(x + r, y + h - 1, x + r + 1, y + h, border);
    gui.fill(x + w - r - 1, y + h - 1, x + w - r, y + h, border);

    // 6. Текст по центру + тень
    gui.drawCenteredString(font, msg, x + w/2, textY + 1, shadowColor);
    gui.drawCenteredString(font, msg, x + w/2, textY, textColor);

    // 7. Индикатор нажатия
    if (active && pressed) gui.fill(x + 1, y + h - 1, x + w - 1, y + h, ACCENT_DIM);
}
```

### lerpColor (интерполяция цвета)

```java
private int lerpColor(int from, int to, float t) {
    int a = (int)(((from>>24)&0xFF) + (((to>>24)&0xFF) - ((from>>24)&0xFF)) * t);
    int r = (int)(((from>>16)&0xFF) + (((to>>16)&0xFF) - ((from>>16)&0xFF)) * t);
    int g = (int)(((from>>8)&0xFF) + (((to>>8)&0xFF) - ((from>>8)&0xFF)) * t);
    int b = (int)((from&0xFF) + ((to&0xFF) - (from&0xFF)) * t);
    return (a<<24) | (r<<16) | (g<<8) | b;
}
```

---

## 3. ГЛАВНОЕ МЕНЮ (PWPMainMenuScreen.java)

### Структура экрана

```
┌─────────────────────────────────┐
│   [BACKGROUND: main_menu.png]    │
│   [OVERLAY: BACKGROUND_DIM]      │
│                                  │
│           PWP (logo)             │  ← height * 0.16f, scale 2.2x, accent
│                                  │
│     Добро пожаловать, {nick}     │  ← height * 0.34f, slide-up 6px
│     Pigeo Warfare Project        │
│                                  │
│       ┌───────────────┐          │
│       │  ▶ PLAY       │ ACCENT   │  ← startY = height * 0.52f
│       ├───────────────┤          │
│       │  ▷ SINGLEPLAY │ PRIMARY  │
│       ├───────────────┤          │
│       │  ⚙ SETTINGS   │ DARK     │
│       ├───────────────┤          │
│       │  ✖ QUIT       │ DANGER   │
│       └───────────────┘          │
│                                  │
│           PWP v1.0.1             │  ← footer
└─────────────────────────────────┘
```

### Анимация входа

```java
// 1. Логотип — fadeIn easeOutCubic, 300ms, без задержки
renderElement(elapsed, delay=0, duration=300, (fade) -> renderLogo());

// 2. Welcome текст — fadeIn + slideUp 6px, easeOutCubic, 300ms, задержка 100ms
renderElement(elapsed, delay=100, duration=300, (fade) -> {
    int slideY = (int)((1 - fade) * 6);
    pose.translate(0, slideY, 0);
    drawCenteredString(welcome, cx, welcomeY, withAlpha(TEXT_PRIMARY, fade*255));
    drawCenteredString(subtitle, cx, welcomeY+16, withAlpha(TEXT_SECONDARY, fade*180));
});

// 3. Кнопки — easeOutBack (с перелётом), slideUp 14px, stagger 80ms
for (int i = 0; i < btns.length; i++) {
    float progress = animProgress(elapsed, 300 + i*80, 250);
    float t = Math.min(progress, 1);
    float eased = Easing.easeOutBack(t);
    int slideY = (int)((1 - eased) * 14);
    btns[i].setAnimAlpha(eased);
    btns[i].visible = true;
    pose.translate(0, slideY, 0);
    btns[i].render(gui, mouseX, mouseY, partialTick);
}

// 4. Footer — fadeIn easeOutCubic, 300ms, задержка после кнопок
renderElement(elapsed, delay=300+4*80+100, duration=300);
```

### Background рендер

```java
// 1. Текстура на весь экран
gui.blit(BG_TEXTURE, 0, 0, 0, 0, width, height, width, height);

// 2. Затемнение поверх
gui.fill(0, 0, width, height, PWPTheme.Colors.BACKGROUND_DIM);
```

### Формула анимации

```java
float animProgress(float elapsed, long delay, long duration) {
    float t = (elapsed - delay) / duration;
    return Math.min(t, 1);
}
```

---

## 4. EASING-ФУНКЦИИ (Easing.java)

```java
linear(t)          // t
easeInQuad(t)      // t²
easeOutQuad(t)     // t(2-t)
easeInOutQuad(t)   // 2t² / -1+(4-2t)t
easeInCubic(t)     // t³
easeOutCubic(t)    // (--t)*t²+1
easeInOutCubic(t)  // 4t³ / (t-1)(2t-2)²+1
easeInExpo(t)      // pow(2,10(t-1))
easeOutExpo(t)     // 1-pow(2,-10t)
easeOutElastic(t)  // пружинистая
easeInElastic(t)   // пружинистая
easeOutBounce(t)   // отскок
easeOutBack(t)     // перелёт (c1=1.70158)
pulse(t)           // 0.5+0.5*sin(t*2π) — пульсация
shake(t)           // sin(t*8π)*(1-t) — качание
```

---

## 5. ПАТТЕРНЫ РЕНДЕРА (общие для всех экранов)

### Паттерн 1: Карточка с бордером

```java
// Заливка
gui.fill(x, y, x+w, y+h, bg);
// Верхний бордер
gui.fill(x, y, x+w, y+1, border);
// Нижний бордер
gui.fill(x, y+h-1, x+w, y+h, border);
// Левый бордер
gui.fill(x, y, x+1, y+h, border);
// Правый бордер
gui.fill(x+w-1, y, x+w, y+h, border);
```

### Паттерн 2: Панель с заголовком

```java
gui.fill(x, y, x+w, y+h, SURFACE);           // фон
gui.fill(x, y, x+w, y+1, BORDER);            // верх
gui.fill(x, y+h-1, x+w, y+h, BORDER);        // низ
gui.fill(x, y, x+1, y+h, BORDER);            // лево
gui.fill(x+w-1, y, x+w, y+h, BORDER);        // право
gui.fill(x+1, y+1, x+w-1, y+titleH+1, SURFACE_LIGHT);  // хедер
gui.drawString(font, title, x+8, y+4, TEXT_ACCENT);      // заголовок
```

### Паттерн 3: Прогресс-бар

```java
gui.fill(trackX, y, trackX+barWidth, y+barHeight, SURFACE_DIM);  // трек
gui.fill(trackX, y, trackX+fillWidth, y+barHeight, ACCENT);       // заполнение
```

### Паттерн 4: Скролл-панель с обрезкой

```java
gui.enableScissor(x, y, x+w, y+h);
// ... рендер контента со смещением scrollOffset ...
gui.disableScissor();
```

### Паттерн 5: Слайдер (сортировка по категориям)

```java
// Категории — кнопки-чипсы
for (int i = 0; i < categories.length; i++) {
    boolean isSel = current.equals(categories[i]);
    int bg = isSel ? ACCENT_DIM : (hover ? SURFACE_LIGHT : SURFACE);
    gui.fill(catX, catY, catX+bw, catY+18, bg);
    // бордеры вокруг чипса
    gui.drawString(font, name, catX+6, catY+5, TEXT_PRIMARY);
    catX += bw + 4;
}
```

---

## 6. СПИННЕР И ЛОАДИНГ (PWPUtils.java)

```java
// 3 точки с пульсацией
for (int i = 0; i < 3; i++) {
    long phaseMs = i * 150L;
    long t = (ageMs + phaseMs) % periodMs;
    float pulse = Easing.pulse((float) t / periodMs);
    float alpha = 0.3f + 0.7f * pulse;
    int color = withAlpha(ACCENT, (int)(alpha * 255));
    gui.fill(cxDot-radius, y-radius, cxDot+radius, y+radius, color);
}
```

---

## 7. ЦЕПОЧКА ВЫЗОВОВ ЭКРАНОВ (Screen Flow)

### Как Minecraft запускает PWP

```
Minecraft запускается
    │
    ├── [Mixin] WindowIconMixin
    │       @Inject(Minecraft.<init>)
    │       → ставит окно "PWP", иконки 16x16 + 32x32
    │
    ├── [Forge Event] CoreClientMod.onScreenOpen()
    │       ScreenEvent.Opening → если TitleScreen → заменяет на PWPMainMenuScreen
    │
    ├── [Mixin] TitleScreenMixin
    │       @Inject(TitleScreen.init) HEAD, cancellable=true
    │       → если screen instanceof TitleScreen → ставит PWPMainMenuScreen, cancel
    │
    ▼
PWPMainMenuScreen
    │
    ├── PLAY → PWPConnectingScreen
    │              → ClientConnectHandler.connect("pigeo.asuscomm.com", 25565)
    │              → Minecraft ConnectScreen (ванильный)
    │              → сервер соединяет → LobbyScreen открывается с сервера
    │
    ├── SINGLEPLAYER → SelectWorldScreen (ванильный)
    ├── SETTINGS → OptionsScreen (ванильный)
    └── QUIT → Minecraft.getInstance().stop()
```

### Загрузка мира (Level transitions)

```
Игрок заходит на сервер / меняет мир
    │
    ├── [Mixin] LevelTransitionMixin
    │       @Inject(Minecraft.setLevel) HEAD
    │       → если mc.level != null && mc.screen == null
    │       → показывает PWPLevelLoadingScreen
    │
    ├── [Mixin] LevelLoadingScreenMixin
    │       @Inject({ LevelLoadingScreen, ReceivingLevelScreen }.render) HEAD + TAIL
    │       → перехватывает renderBackground → ставит loading.png + затемнение
    │       → рисует лого, спиннер, прогресс-бар, советы поверх
    │
    ▼
    LobbyScreen (открывается по пакету с сервера)
    или
    Игровой процесс (HUD)
```

### Цепочка после подключения к серверу

```
PWPMainMenuScreen → PLAY
    → PWPConnectingScreen (кастомный экран "Подключение...")
        → ClientConnectHandler.connect() → Minecraft ConnectScreen (ванильный коннект)
        → Сервер принимает → LevelLoadingScreen (ванильный) + Mixin (loading.png + tips)
        → Сервер шлёт пакет OpenMatchScreenPacket / OpenMatchListScreenPacket / OpenVotingScreenPacket
        → LobbyScreen.openMatch() / .openList() / .openVote()
        → LobbyScreen отображается с табами:
            TAB_MATCHES  — список матчей / текущий матч
            TAB_VOTING   — голосование за карту / режим
            TAB_STATS    — StatsScreen (встроенный)
        → Игрок выбирает матч → JoinMatchServerPacket → новый сервер
        → LevelTransitionMixin → PWPLevelLoadingScreen → игра
```

### Во время игры

```
Игра (gameplay HUD)
    │
    ├── TeamSelectionScreen   — выбор команды (BLUE / RED)
    ├── SquadSelectionScreen  — выбор отряда
    ├── FactionSelectScreen   — выбор фракции
    ├── KitListScreen         — выбор набора
    ├── KitEditorScreen       — редактор набора
    ├── VehicleSpawnerScreen  — спавн техники
    ├── HubRadialScreen       — радиальное меню хаба
    ├── TacticalMapRadialScreen — тактическая карта
    ├── RadioRadialScreen     — радио комнды
    ├── CrateRadialScreen     — ящики с припасами
    ├── DefenseRadialScreen   — оборона
    ├── DownedScreen          — "ранен/истекаю кровью"
    └── VictoryScreen         — экран победы/поражения
```

---

## 8. ЭКРАН ЗАГРУЗКИ МИРА

### PWPLevelLoadingScreen.java (отдельный экран)

```
┌─────────────────────────────────┐
│   [BACKGROUND: loading.png]      │
│   [OVERLAY: BACKGROUND_DIM]      │
│                                  │
│           PWP (logo)             │  ← height * 0.16f
│                                  │
│         ● ● ●                    │  ← спиннер (3 точки)
│       Загрузка мира...           │
│       ▓▓▓▓▓░░░░░░░░░            │  ← прогресс-бар
│                                  │
│   Совет: используйте тактическое │  ← tips (смена каждые 4с)
│   оборудование для победы        │
└─────────────────────────────────┘
```

### LevelLoadingScreenMixin.java (перехват ванильного экрана)

```java
@Mixin({ LevelLoadingScreen.class, ReceivingLevelScreen.class })
public class LevelLoadingScreenMixin {

    // Перехват renderBackground — отменяет ванильный, ставит loading.png
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void pwp_customBackground(GuiGraphics gui, int mx, int my, float pt, CallbackInfo ci) {
        ci.cancel();
        gui.blit(PWP_LOADING_BG, 0, 0, 0, 0, w, h, w, h);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
    }

    // Render HEAD — инициализация tips
    @Inject(method = "render", at = @At("HEAD"))
    private void pwp_onRenderHead(...) {
        pwp_openTime = System.currentTimeMillis();
        pwp_tips = new PWPTipsWidget(width * 0.6f);
    }

    // Render TAIL — рисует лого, спиннер, прогресс, tips поверх
    @Inject(method = "render", at = @At("TAIL"))
    private void pwp_customOverlay(...) {
        PWPUtils.renderLogo(gui, cx, height * 0.16f);
        PWPUtils.renderSpinner(gui, cx, cy - 30, elapsed);
        PWPUtils.renderProgressBar(gui, cx, cy + 30, width * 0.3f, 4, elapsed);
        pwp_tips.render(gui, cx, cy + 55);
    }
}
```

### LevelTransitionMixin.java — показывает PWPLevelLoadingScreen при смене мира

```java
@Mixin(Minecraft.class)
public class LevelTransitionMixin {
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void pwp_onLevelChange(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.screen == null) {
            mc.setScreen(new PWPLevelLoadingScreen());
        }
    }
}
```

---

## 9. ЭКРАН ПОБЕДЫ (VictoryScreen.java)

```
┌─────────────────────────────────┐
│  [GRADIENT: чёрный 0% → 100%]   │  ← 1.5s fade-in
│                                  │
│       ┌─────────────────┐        │
│       │ [FLAG TEXTURE]  │        │  ← флаг победившей фракции
│       └─────────────────┘        │
│                                  │
│       BLUEFOR WINS!              │  ← scale 1.5x, белый, 2s fade-in
│       Victory by ticket bleed    │
│                                  │
│  ┌─────────────────────────┐     │
│  │   MATCH STATS           │     │  ← панель с бордером
│  │ ⚔ Kills: 12  ☠ Deaths: 5│     │
│  │ K/D: 2.40   Score: 4500 │     │
│  │ V.Kills: 3  Captures: 2 │     │
│  └─────────────────────────┘     │
│                                  │
│       [ CONTINUE ]               │  ← кнопка появляется после анимации
└─────────────────────────────────┘
```

Ключевые особенности:
- **Gradient background**: `gui.fillGradient(0, 0, w, h, topBg, bottomBg)` с alpha-анимацией
- **Флаг**: текстура из `textures/gui/flags/`, 80x45px, 8 вариантов (ukraine, russia, usa, nato, bluefor, redfor, insurgency, pmc)
- **Заголовок**: масштабирование `pose.scale(1.5, 1.5, 1)` + drawCenteredString
- **Stats панель**: стандартный `fill()` + бордеры, 3 колонки по 106px
- **Кнопка ContinueButton**: кастомный render с `currentAlpha`, появляется после contentAlpha >= 1

---

## 10. ЭКРАН ВЫБОРА КОМАНДЫ (TeamSelectionScreen.java)

```
┌─────────────────────────────────┐
│  ⚔ ВЫБОР КОМАНДЫ                │
│  ─────────────────               │
│                                  │
│   ┌──────────┐   ┌──────────┐    │
│   │ BLUEFOR  │   │ REDFOR   │    │
│   │ [FLAG]   │   │ [FLAG]   │    │  ← 160x140 карты
│   │ JOIN     │   │ JOIN     │    │
│   │ 5 players│   │ 8 players│    │
│   └──────────┘   └──────────┘    │
│                                  │
└─────────────────────────────────┘
```

Ключевые особенности:
- 2 карты (160x140) с отступом 30px между ними
- Флаг текстура 80x45 внутри каждой карты
- Если команда переполнена — затемнение карты + `alpha=0.3` + "TOO MANY PLAYERS"
- Hover: SURFACE_LIGHT + цвет команды в бордере

---

## 11. ЭКРАН ПОДКЛЮЧЕНИЯ (PWPConnectingScreen.java)

```
┌─────────────────────────────────┐
│   [BACKGROUND: main_menu.png]    │
│   [OVERLAY: BACKGROUND_DIM]      │
│                                  │
│           PWP (logo)             │
│                                  │
│         ● ● ●                    │  ← спиннер
│       Подключение...             │  ← пульсирующий текст
│       ▓▓▓░░░░░░░░░░░            │  ← прогресс-бар
│                                  │
│   Совет: связь с отрядом —       │  ← tips
│   ключ к успеху                  │
│                                  │
│   (если 12s таймаут)             │
│   Не удалось подключиться        │  ← красный текст
│   Сервер недоступен              │
│       [ НАЗАД ]                  │  ← кнопка DARK
└─────────────────────────────────┘
```

- timeout 12 секунд → enterErrorState() → fade-in красной надписи + кнопка "Назад"
- использует те же PWPUtils.renderSpinner, renderProgressBar, renderStatusText

---

## 12. ВИДЖЕТ СОВЕТОВ (PWPTipsWidget.java)

- 15 советов на русском (тактика, оборудование, командная игра)
- Циклическая смена: 4s показа + 300ms fade-out + 300ms fade-in → новый рандомный совет
- При длинном тексте — split через `font.split()` на 2 строки
- Цвет: TEXT_SECONDARY с alpha 180

```java
if (!fading && elapsed > 4000) {
    fading = true;
} else if (fading && elapsed > 300) {
    fading = false;
    currentIndex = random different index;
}
```

---

## 13. ПОЛНЫЙ СПИСОК ВСЕХ ЭКРАНОВ ПРОЕКТА (31 экран)

### core-client (6 экранов)
| Файл | Назначение |
|------|-----------|
| `PWPMainMenuScreen.java` | Главное меню (замена TitleScreen) |
| `PWPConnectingScreen.java` | Экран подключения к серверу |
| `PWPLevelLoadingScreen.java` | Загрузка мира |
| `PWPTipsWidget.java` | Виджет советов (встраивается в другие экраны) |
| `PWPUtils.java` | Утилиты рендера (лого, спиннер, прогресс-бар) |
| `StatsScreen.java` | Статистика (Моя статистика + Лидерборд) |

### lobby (2 экрана)
| Файл | Назначение |
|------|-----------|
| `LobbyScreen.java` | Лобби (табы: матчи, голосование, статистика) |
| `ServerListScreen.java` | "Нет активных матчей" |

### cosmetics (1 экран)
| Файл | Назначение |
|------|-----------|
| `SkinInventoryScreen.java` | Инвентарь скинов (грид 7 колонок, фильтры, экипировка) |

### warfare (30 экранов)
| Файл | Назначение |
|------|-----------|
| `VictoryScreen.java` | Победа/поражение (флаг, stats) |
| `TeamSelectionScreen.java` | Выбор команды (BLUE / RED карты) |
| `SquadSelectionScreen.java` | Выбор отряда |
| `FactionSelectScreen.java` | Выбор фракции |
| `FactionKitListScreen.java` | Список наборов фракции |
| `FactionVehicleListScreen.java` | Список техники фракции |
| `FactionVehicleSelectScreen.java` | Выбор техники фракции |
| `FactionVehicleEditorScreen.java` | Редактор техники фракции |
| `KitEditorScreen.java` | Редактор набора |
| `KitListScreen.java` | Список наборов |
| `KitPreviewScreen.java` | Предпросмотр набора |
| `KitTeamSelectScreen.java` | Выбор команды для набора |
| `KitSkinSelectScreen.java` | Выбор скина для набора |
| `PlayerKitSelectScreen.java` | Выбор набора игроком |
| `HubAmmoScreen.java` | Пополнение боеприпасов |
| `HubRadialScreen.java` | Радиальное меню хаба |
| `CrateRadialScreen.java` | Радиальное меню ящиков |
| `DefenseRadialScreen.java` | Радиальное меню обороны |
| `RadioRadialScreen.java` | Радио команды |
| `TacticalMapRadialScreen.java` | Тактическая карта |
| `SquadMarkerRadialScreen.java` | Маркеры отряда |
| `MapMarkerSelectionScreen.java` | Выбор маркера карты |
| `MapMarkerGridScreen.java` | Сетка маркеров карты |
| `StaticGunRadialScreen.java` | Статичное орудие |
| `VehicleSpawnerScreen.java` | Спавн техники |
| `SquadButton.java` | Кастомная кнопка отряда |
| `DownedScreen.java` | Экран "ранения" |
| `SkinListScreen.java` | Список скинов (админка) |
| `SkinEditorScreen.java` | Редактор скинов (админка) |
| `WarfareMapRenderer.java` | Рендер карты |
| `WarfareConfigScreen.java` | Конфиг варфейра |

---

## 14. АРХИТЕКТУРА ПЕРЕХОДОВ (Mixin'ы + Forge Events)

### Mixin конфиг (pwp_core_client.mixins.json)
```json
{
    "required": true,
    "package": "com.pwp.coreclient.mixin",
    "client": [
        "TitleScreenMixin",        // замена TitleScreen → PWPMainMenuScreen
        "WindowIconMixin",         // кастомная иконка + заголовок окна
        "LevelLoadingScreenMixin", // кастомный фон + overlay на загрузке
        "LevelTransitionMixin"     // PWPLevelLoadingScreen при смене мира
    ]
}
```

### Как заменяется TitleScreen (два механизма страховки)

1. **Forge Event** (CoreClientMod.java):
```java
MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onScreenOpen);
private void onScreenOpen(ScreenEvent.Opening event) {
    if (event.getNewScreen() instanceof TitleScreen) {
        event.setNewScreen(new PWPMainMenuScreen());
    }
}
```

2. **Mixin** (TitleScreenMixin.java) — дублирование на случай если Forge не сработает:
```java
@Inject(method = "init", at = @At("HEAD"), cancellable = true)
private void pwp_replaceWithMainMenu(CallbackInfo ci) {
    if (pwp_initialized) return;
    pwp_initialized = true;
    mc.setScreen(new PWPMainMenuScreen());
    ci.cancel();
}
```

---

## 15. СТРУКТУРА РЕСУРСОВ (Assets)

```
pwp-core-client/
└── src/main/resources/
    ├── assets/pwp_core_client/
    │   ├── textures/gui/
    │   │   ├── main_menu.png      ← фон главного меню
    │   │   └── loading.png        ← фон загрузки
    │   ├── icons/
    │   │   ├── icon_16x16.png     ← иконка окна 16px
    │   │   └── icon_32x32.png     ← иконка окна 32px
    │   └── lang/
    │       ├── en_us.json         ← английский (35 строк)
    │       └── ru_ru.json         ← русский перевод
    └── pwp_core_client.mixins.json ← конфиг миксинов

pwp-lobby/
└── src/main/resources/
    └── assets/pwp_lobby/  (текстуры карт загружаются динамически с диска)

pwp-warfare/
└── src/main/resources/
    └── assets/pwpwarfare/
        └── textures/gui/flags/   ← 8 флагов фракций (80x45px)
            ├── ukraine.png
            ├── russia.png
            ├── usa.png
            ├── nato.png
            ├── bluefor.png
            ├── redfor.png
            ├── insurgency.png
            └── pmc.png
```

### Lang файл (en_us.json) — все UI строки
```json
{
    "pwp_core.ui.loading": "Loading...",
    "pwp_core.ui.back": "Back",
    "pwp_core.main_menu.welcome": "Welcome, %s",
    "pwp_core.main_menu.subtitle": "Pigeo Warfare Project",
    "pwp_core.main_menu.play": "PLAY",
    "pwp_core.main_menu.singleplayer": "SINGLEPLAYER",
    "pwp_core.main_menu.settings": "SETTINGS",
    "pwp_core.main_menu.quit": "QUIT"
}
```

---

## 16. ПРЕДЛАГАЕМАЯ ЦЕНТРАЛИЗОВАННАЯ АРХИТЕКТУРА (для редизайна)

### Проблемы текущей архитектуры
1. **Нет компонентной системы** — каждый экран рисует свои панели/карточки вручную
2. **Дублирование кода** — паттерн "fill + 4 бордера" повторяется в 20+ местах
3. **Смесь стилей** — LobbyScreen использует vanilla Button, а MainMenu — PWPButton
4. **Нет ScreenManager** — переходы между экранами не анимированы
5. **Нет BackgroundController** — каждый экран сам рендерит фон
6. **Нет AudioManager** — звуки на hover/клик не централизованы

### Предлагаемая структура

```
com.pwp.coreclient.gui/
├── theme/
│   └── PWPTheme.java              ← дизайн-система (цвета, шрифты, тени, иконки)
│
├── components/                     ← переиспользуемые компоненты
│   ├── PWPButton.java             ← кнопка (5 стилей)
│   ├── PWPCard.java               ← карточка (с тенью, border-radius, hover)
│   ├── PWPPanel.java              ← панель с заголовком
│   ├── PWPScrollPanel.java        ← скролл-панель
│   ├── PWPToast.java              ← уведомления
│   ├── PWPProgressBar.java        ← прогресс-бар
│   ├── PWPBadge.java              ← бейдж
│   ├── PWPTabGroup.java           ← группа табов
│   ├── PWPInputField.java         ← поле ввода
│   ├── PWPDropdown.java           ← выпадающий список
│   └── PWPToggle.java             ← переключатель
│
├── core/                          ← ядро (менеджеры)
│   ├── PWPScreenManager.java      ← управление экранами + переходы
│   ├── PWPBackgroundManager.java  ← фоны + параллакс + частицы
│   ├── PWPAnimationManager.java   ← централизованные анимации
│   └── PWPAudioManager.java       ← звуки UI
│
├── screens/                       ← экраны
│   ├── PWPMainMenuScreen.java
│   ├── PWPConnectingScreen.java
│   ├── PWPLevelLoadingScreen.java
│   ├── StatsScreen.java
│   └── ...
│
├── animations/
│   ├── Easing.java               ← easing-функции
│   ├── Animation.java            ← класс анимации (start, end, duration, easing)
│   └── Animator.java             ← планировщик анимаций
│
└── effects/                       ← визуальные эффекты
    ├── ParticleBackground.java   ← частицы на фоне
    └── GlowEffect.java           ← свечение
```

### Как это будет работать

```java
// Пример: экран после редизайна
public class PWPMainMenuScreen extends Screen {

    @Override
    protected void init() {
        // Фон теперь управляется централизованно
        PWPBackgroundManager.INSTANCE.setBackground("main_menu");
        PWPBackgroundManager.INSTANCE.setParticles(true);

        // Кнопка через компонент
        addRenderableWidget(new PWPButton(cx - 100, y, 200, 40, "PLAY", PWPButton.Style.ACCENT)
            .withIcon("play")
            .withShadow(Shadows.LEVEL_2)
            .withRipple(true)
            .onClick(() -> {
                PWPScreenManager.INSTANCE.transitionTo(new PWPConnectingScreen(),
                    TransitionType.FADE, 300);
            }));

        // Анимация входа
        PWPScreenManager.INSTANCE.animateIn(this, AnimationType.SLIDE_UP, 500);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        // Фон рисуется автоматически
        PWPBackgroundManager.INSTANCE.render(gui, partialTick);
    }
}
```

### Что нужно реализовать

| Компонент | Описание | Приоритет |
|-----------|----------|-----------|
| **PWPCard** | Карточка с border-radius, shadow, hover, selected, gradient border | ★★★★★ |
| **PWPPanel** | Панель с заголовком, иконкой, content area | ★★★★★ |
| **PWPScreenManager** | Анимированные переходы между экранами (fade, slide, scale) | ★★★★ |
| **PWPBackgroundManager** | Фоны: смена текстур, параллакс, overlay, particles | ★★★★ |
| **PWPToast** | Всплывающие уведомления (success/error/info/xp) | ★★★ |
| **PWPTabGroup** | Табы с анимацией подчёркивания | ★★★ |
| **PWPAnimationManager** | Централизованные анимации с callback | ★★★ |
| **PWPAudioManager** | Звуки на hover/клик/открытие экрана | ★★ |
| **ParticleBackground** | Фоновые частицы (пепел, искры, дым) | ★★★★ |

---

## 17. ЧТО МОЖНО МЕНЯТЬ (гибкость)

```
┌──────────────────────────────────────────┐
│  [Матчи] [Голосование] [Статистика]  [✕] │  ← табы (vanilla Button)
├──────────────────────────────────────────┤
│                                          │
│   ТЕКУЩИЙ МАТЧ (если есть)              │
│   ┌─────────────────────────────────┐    │
│   │ ТЕКУЩИЙ МАТЧ                    │    │
│   │ Map: xxx                         │    │
│   │ Mode: yyy                        │    │
│   │ BLUEFOR vs REDFOR (100 | 80)    │    │
│   └─────────────────────────────────┘    │
│                                          │
│   АКТИВНЫЕ МАТЧИ                        │
│   ┌─────────────────────────────────┐    │
│   │ [img] Название матча            │    │
│   │       12m  |  5/10              │    │
│   │       BLUEFOR vs REDFOR         │    │
│   │       100 | 80                  │    │
│   │       ▶ Нажмите для входа       │    │
│   └─────────────────────────────────┘    │
│   ◀ 1/3 ▶                               │
└──────────────────────────────────────────┘
```

---

## 8. ЭКРАН СТАТИСТИКИ (StatsScreen.java)

```
┌──────────────────────────────────────────┐
│  ⚔ СТАТИСТИКА                            │
│  ─────────────────────                    │
│  [Моя статистика] [Лидеры]               │
├──────────────────────────────────────────┤
│                                          │
│  ┌─────────────────────────────────┐     │
│  │ Nickname  Lv.5 ✦2               │     │
│  │ Матчи: 100                      │     │
│  ├─────────────────────────────────┤     │
│  │ ⚔ БОЙ                           │     │
│  │ Убийства: 500  Смерти: 300      │     │
│  │ K/D: 1.67       Спасения: 20   │     │
│  ├─────────────────────────────────┤     │
│  │ ✦ ТЕХНИКА                       │     │
│  │ Уничтожено: 50  Воздух: 5       │     │
│  └─────────────────────────────────┘     │
└──────────────────────────────────────────┘
```

---

## 9. ИНВЕНТАРЬ СКИНОВ (SkinInventoryScreen.java)

```
┌──────────────────────────────────────────┐
│  [Все][Основное][Втор.][Нож][Холод.][Униф.]│
│                                          │
│        ИНВЕНТАРЬ СКИНОВ                  │
│        ─────────────────                  │
│   ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐  │
│   │🔫│ │🔫│ │🔫│ │🔫│ │🔫│ │🔫│ │🔫│  │  ← 7 колонок
│   └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘  │
│   ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐  │
│   │🔫│ │🔫│ │🔫│ │🔫│ │🔫│ │🔫│ │🔫│  │
│   └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘  │
│     ▲ скролл                             │
│     ▼                                    │
└──────────────────────────────────────────┘
```

Каждый скин:
- `gui.fill()` — фон ячейки (SURFACE)
- `gui.renderOutline()` — рамка редкости (цвет rarityColor)
- `gui.renderItem()` — иконка предмета Minecraft
- Если надет — зелёная рамка + ✔
- Если не в наличии — затемнение + 🔒

---

## 10. ТИПОВАЯ СТРУКТУРА ЭКРАНА (шаблон)

```java
public class ExampleScreen extends Screen {
    private final long openTime;

    public ExampleScreen() {
        super(Component.literal("Title"));
        openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        // addRenderableWidget(new PWPButton(...))
        // addRenderableWidget(Button.builder(...).bounds(...).build())
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);  // текстура + затемнение
        super.render(gui, mx, my, pt);

        float elapsed = System.currentTimeMillis() - openTime;
        int cx = width / 2;
        // кастомный рендер с анимациями
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        gui.blit(BG_TEXTURE, 0, 0, 0, 0, width, height, width, height);
        gui.fill(0, 0, width, height, BACKGROUND_DIM);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
```

---

## 11. ЧТО МОЖНО МЕНЯТЬ (гибкость)

| Что | Как | Сложность |
|-----|-----|-----------|
| **Цвета** | поменять hex в PWPTheme.java | ☆ |
| **Размеры** | поменять числа в Spacing | ☆ |
| **Шрифты** | подключить TTF через Minecraft font API | ★★ |
| **Иконки** | заменить Unicode на текстуры | ★★ |
| **Кнопки** | переписать renderWidget(), добавить градиенты/тени | ★★★ |
| **Анимации** | добавить новые easing или изменить длительности | ★★ |
| **Фоны** | заменить текстуру, добавить шейдеры/частицы | ★★★★ |
| **Карточки** | добавить border-radius, тени, градиенты | ★★★ |
| **Переходы** | добавить cross-fade между экранами | ★★★ |
| **Скролл** | добавить smooth scroll с инерцией | ★★★ |
| **Тулкит** | добавить новые компоненты (radio, checkbox, toggle, dropdown) | ★★★ |

---

## 12. ЧЕГО НЕ ХВАТАЕТ (wishlist для редизайна)

- [ ] Кастомный шрифт (военный/tech/minimal)
- [ ] Текстурные иконки вместо Unicode
- [ ] Кнопки с градиентами и настоящим border-radius
- [ ] Анимированный фон (шейдеры, частицы, parallax)
- [ ] Glassmorphism (blur + полупрозрачность)
- [ ] Переходы между экранами (fade/slide)
- [ ] Toast-уведомления с анимацией
- [ ] Normalized компоненты (input, select, toggle, slider)
- [ ] Smooth scroll с инерцией
- [ ] Ripple-эффект на кнопках
- [ ] Count-up анимация цифр в статистике
- [ ] Анимированные прогресс-бары
- [ ] Тени (drop shadow) через текстуры
- [ ] Кастомные курсоры
- [ ] Звуки на hover/клик
