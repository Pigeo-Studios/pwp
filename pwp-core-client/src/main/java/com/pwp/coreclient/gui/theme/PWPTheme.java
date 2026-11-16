package com.pwp.coreclient.gui.theme;

public class PWPTheme {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(PWPTheme.class);

    // ============================================================
    //  ЦВЕТА
    // ============================================================

    public static class Colors {

        // ============================================================
        //  Dark Ember — минималистичная тёмная тема с приглушённым
        //  оранжевым акцентом в стиле "теневой обводки"
        // ============================================================

        // --- Фоны ---
        public static final int BACKGROUND = 0xFF0A0C0E; // Самый тёмный (основной фон)
        public static final int BACKGROUND_DIM = 0xCC06080A; // Затемнение/оверлей
        public static final int BACKGROUND_GRADIENT_START = 0xFF080A0D;
        public static final int BACKGROUND_GRADIENT_END = 0xFF0D1015;

        public static final int SURFACE = 0xFF12151A; // Карточки / панели
        public static final int SURFACE_LIGHT = 0xFF1A1E26; // При наведении
        public static final int SURFACE_DIM = 0x6612151A; // Полупрозрачная
        public static final int SURFACE_TOP = 0xCC0E1117; // Верхняя панель/хедер

        // --- Акцент "теневая обводка" (приглушённый оранжевый) ---
        public static final int ACCENT = 0xFFC8812A; // Основной оранжевый
        public static final int ACCENT_DIM = 0xFF8B6220; // Тёмный (для обводок)
        public static final int ACCENT_GLOW = 0x18C8812A; // Очень прозрачное свечение
        public static final int ACCENT_BORDER = 0xFF6B4A18; // Тень-граница
        public static final int ACCENT_SHADOW = 0xFF3A2A0F; // Глубокая тень
        public static final int ACCENT_SOFT = 0xFFA06E22; // Мягкий

        // --- Команды (приглушённые) ---
        public static final int TEAM_BLUE = 0xFF3D6FA5;
        public static final int TEAM_BLUE_DIM = 0x553D6FA5;
        public static final int TEAM_BLUE_DARK = 0xFF1A2E45;
        public static final int TEAM_RED = 0xFFA53D3D;
        public static final int TEAM_RED_DIM = 0x55A53D3D;
        public static final int TEAM_RED_DARK = 0xFF451A1A;

        // --- Состояния (приглушённые) ---
        public static final int SUCCESS = 0xFF3D7A40; // Зелёный
        public static final int SUCCESS_DIM = 0x553D7A40;
        public static final int SUCCESS_LIGHT = 0xFF4CAF50;
        public static final int WARNING = 0xFFC8812A; // Оранжевый = акцент
        public static final int DANGER = 0xFFA53D3D; // Красный
        public static final int DANGER_DIM = 0x55A53D3D;
        public static final int INFO = 0xFF3D6FA5; // Синий

        // --- Текст ---
        public static final int TEXT_PRIMARY = 0xFFC8CBCE; // Мягкий светло-серый
        public static final int TEXT_SECONDARY = 0xFF7A7D84; // Серый
        public static final int TEXT_DIM = 0xFF4A4D54; // Тёмно-серый (неактив)
        public static final int TEXT_ACCENT = 0xFFC8812A; // Оранжевый (важные цифры)

        // --- Границы ---
        public static final int BORDER = 0xFF1E222A; // Едва заметная
        public static final int BORDER_LIGHT = 0xFF2A2F3A; // Светлая
        public static final int BORDER_ACCENT = 0xFF6B4A18; // Оранжевая тень-граница
        public static final int BORDER_FOCUS = 0xFFC8812A; // Оранжевый фокус

        // --- Редкость предметов (приглушённая) ---
        public static final int RARITY_COMMON = 0xFF6A6D73;
        public static final int RARITY_RARE = 0xFF3D6FA5;
        public static final int RARITY_EPIC = 0xFF7A4A8A;
        public static final int RARITY_LEGENDARY = 0xFFC8812A;
        public static final int RARITY_MYTHIC = 0xFFA53D3D;

        // --- Градиенты ---
        public static final int GRADIENT_BG_START = BACKGROUND_GRADIENT_START;
        public static final int GRADIENT_BG_END = BACKGROUND_GRADIENT_END;
        public static final int GRADIENT_ACCENT_START = 0xFFC8812A;
        public static final int GRADIENT_ACCENT_END = 0xFF6B4A18;

        private Colors() {}

        public static int alpha(int argb) {
            return (argb >>> 24) & 0xFF;
        }

        public static int red(int argb) {
            return (argb >>> 16) & 0xFF;
        }

        public static int green(int argb) {
            return (argb >>> 8) & 0xFF;
        }

        public static int blue(int argb) {
            return argb & 0xFF;
        }

        public static int argb(int a, int r, int g, int b) {
            return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }

        public static int withAlpha(int color, int alpha) {
            return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
        }

        public static int multiplyAlpha(int color, float factor) {
            int a = alpha(color);
            int newA = Math.max(0, Math.min(255, Math.round(a * factor)));
            return (newA << 24) | (color & 0x00FFFFFF);
        }

        public static int lerp(int from, int to, float t) {
            if (t <= 0) return from;
            if (t >= 1) return to;
            int a = (int) (alpha(from) + (alpha(to) - alpha(from)) * t);
            int r = (int) (red(from) + (red(to) - red(from)) * t);
            int g = (int) (green(from) + (green(to) - green(from)) * t);
            int b = (int) (blue(from) + (blue(to) - blue(from)) * t);
            return argb(a, r, g, b);
        }
    }

    // ============================================================
    //  ШРИФТЫ
    // ============================================================

    public static class Fonts {

        public static final int SIZE_TINY = 8;
        public static final int SIZE_SMALL = 10;
        public static final int SIZE_NORMAL = 12;
        public static final int SIZE_MEDIUM = 14;
        public static final int SIZE_LARGE = 16;
        public static final int SIZE_XLARGE = 20;
        public static final int SIZE_HUGE = 24;
        public static final int SIZE_TITLE = 32;

        public static final net.minecraft.resources.ResourceLocation DISPLAY_ID =
                new net.minecraft.resources.ResourceLocation("pwp_core_client", "display");

        /**
         * Шрифт заголовков/HUD — ванильный mc.font. TTF-шрифт {@link #DISPLAY_ID} (Inter 9px)
         * в игре нечитаем; в проде его и раньше никто не видел: старый код искал
         * {@code Minecraft.fontManager} рефлексией по mojmap-имени, в SRG поля нет —
         * всегда срабатывал фолбэк на mc.font, а исключение каждый кадр забивало лог
         * (инцидент 16.11.2026). Точка оставлена, чтобы 350 вызовов не трогать.
         */
        public static net.minecraft.client.gui.Font display() {
            return net.minecraft.client.Minecraft.getInstance().font;
        }

        private Fonts() {}
    }

    // ============================================================
    //  РАЗМЕРЫ И ОТСТУПЫ
    // ============================================================

    public static class Spacing {

        public static final int XXS = 2;
        public static final int XS = 4;
        public static final int SM = 8;
        public static final int MD = 12;
        public static final int LG = 16;
        public static final int XL = 24;
        public static final int XXL = 32;
        public static final int HUGE = 48;

        // Скругление углов
        public static final int RADIUS_NONE = 0;
        public static final int RADIUS_SMALL = 3;
        public static final int RADIUS_MEDIUM = 6;
        public static final int RADIUS_ROUND = 999;

        // Размеры компонентов
        public static final int BUTTON_HEIGHT = 24;
        public static final int BUTTON_WIDTH = 120;
        public static final int BUTTON_SMALL_HEIGHT = 18;
        public static final int BUTTON_TAB_HEIGHT = 22;
        public static final int ICON_SIZE = 16;
        public static final int ICON_SIZE_MED = 24;
        public static final int ICON_SIZE_LG = 32;
        public static final int AVATAR_SIZE = 32;
        public static final int ITEM_PREVIEW = 80;
        public static final int SCROLLBAR_WIDTH = 5;
        public static final int PANEL_PADDING = 12;
        public static final int CARD_PADDING = 10;
        public static final int SECTION_GAP = 24;
        public static final int ELEMENT_GAP = 6;
        public static final int GRID_GAP = 8;

        public static final int TOGGLE_WIDTH = 36;
        public static final int TOGGLE_HEIGHT = 16;

        // --- Редизайн 2026-11 (тактический стиль: строгая сетка, 1px линии) ---
        /** Тонкая линия обводки/разделителя. */
        public static final int HAIRLINE = 1;
        /** Акцентная полоса у активного элемента (hover / selected). */
        public static final int ACCENT_BAR = 2;

        public static final int ACCENT_BAR_SELECTED = 3;
        /** Сдвиг контента нажатой кнопки вниз. */
        public static final int PRESS_SHIFT = 1;
        /** Высота шапки стеклянной панели (COMPACT / обычная). */
        public static final int PANEL_HEADER_COMPACT = 18;

        public static final int PANEL_HEADER = 20;
        /** Поле экрана: доля ширины, пределы. */
        public static final float SCREEN_MARGIN_FRACTION = 0.06F;

        public static final int SCREEN_MARGIN_MIN = 12;
        public static final int SCREEN_MARGIN_MAX = 96;
        /** Колонка главного меню: доля ширины экрана, пределы. */
        public static final float MENU_WIDTH_FRACTION = 0.26F;

        public static final int MENU_WIDTH_MIN = 150;
        public static final int MENU_WIDTH_MAX = 300;
        /** Кнопки меню по плотности (COMPACT / NORMAL / LARGE). */
        public static final int MENU_PRIMARY_H_COMPACT = 22;

        public static final int MENU_PRIMARY_H = 30;
        public static final int MENU_PRIMARY_H_LARGE = 34;
        public static final int MENU_BUTTON_H_COMPACT = 20;
        public static final int MENU_BUTTON_H = 22;
        public static final int MENU_BUTTON_H_LARGE = 24;
        public static final int MENU_GAP_COMPACT = 2;
        public static final int MENU_GAP = 4;
        /** Центральная панель (загрузка/дисконнект): доля ширины, пределы. */
        public static final float DIALOG_WIDTH_FRACTION = 0.42F;

        public static final int DIALOG_WIDTH_MIN = 220;
        public static final int DIALOG_WIDTH_MAX = 420;
        /** Индикатор загрузки (неопределённый прогресс): высота и доля бегущего блока. */
        public static final int LOADER_BAR_H = 3;

        public static final int LOADER_BLOCK_DIV = 4;
        /** Размер угла 9-slice текстур (совпадает с tools/generate_ui_textures.py). */
        public static final int FRAME_CORNER = 6;

        public static final int SHADOW_CORNER = 12;
        /** На сколько тень выходит за панель и насколько сдвинута вниз. */
        public static final int SHADOW_SPREAD = 8;

        public static final int SHADOW_DROP = 3;
        /** Верхняя полоса экрана с логотипом (COMPACT / NORMAL / LARGE). */
        public static final int TOP_BAR_H_COMPACT = 18;

        public static final int TOP_BAR_H = 24;
        public static final int TOP_BAR_H_LARGE = 28;
        /** Нижняя полоса экрана (статус/подписи). */
        public static final int FOOTER_H_COMPACT = 14;

        public static final int FOOTER_H = 18;
        /** Строк совета на экране загрузки (COMPACT / прочие). */
        public static final int TIP_LINES_COMPACT = 2;

        public static final int TIP_LINES = 3;
        /** Максимум строк причины дисконнекта. */
        public static final int REASON_LINES_MAX = 8;
        /** Пустое состояние: ширина текста и строк пояснения. */
        public static final int EMPTY_TEXT_MAX_W = 260;

        public static final int EMPTY_HINT_LINES = 3;
        /** Полоса доли голосов у карточки. */
        public static final int SHARE_BAR_H = 2;
        /** Лобби: вкладки, карточки голосования/матчей, таблица лидеров. */
        public static final int LOBBY_TAB_H = 18;

        public static final int LOBBY_TAB_H_LARGE = 22;
        public static final int LOBBY_TAB_PAD = 12;
        public static final int VOTE_CARD_MIN_W = 150;
        public static final int VOTE_CARD_MAX_W = 260;
        public static final int FACTION_COL_MAX_W = 240;
        public static final int RESULT_CARD_MAX_W = 220;
        public static final int MATCH_CARD_MAX_W = 420;
        public static final int INFO_PANEL_MAX_W = 340;
        /** Высота области вкладки, ниже которой лобби ужимается (карточки ниже, без лишнего). */
        public static final int LOBBY_COMPACT_H = 150;

        public static final int VOTE_CARD_MAX_COLS = 4;
        public static final int VOTE_CARD_H = 34;
        public static final int VOTE_CARD_H_LARGE = 40;
        public static final int FACTION_CARD_H = 28;
        public static final int MATCH_CARD_MIN_W = 240;
        public static final int MATCH_CARD_MAX_COLS = 2;
        public static final int RESULT_CARD_MIN_W = 120;
        public static final int RESULT_CARD_H = 56;
        public static final int PROFILE_PANEL_MIN_W = 200;
        /** Минимальная ширина пары «ключ … значение» (две пары в ряд, если влезают). */
        public static final int PROFILE_PAIR_MIN_W = 110;

        public static final int LEADER_ROW_H = 14;
        public static final int LEADER_PILL_H = 16;
        public static final int LEADER_TOP_H = 40;
        /** Ширина таблицы лидеров, с которой показываются колонки K/D и Win%. */
        public static final int LEADER_WIDE_MIN_W = 300;
        /** Колонка таймера голосования (под «00:00»). */
        public static final int VOTE_BAR_H = 4;
        /** Карточки экрана выбора (PWPSelectionScreen): зазор и поле. */
        public static final int SELECTION_MAX_COLS = 6;
        /** Кнопка экрана в галерее тест-стенда UI. */
        public static final int GALLERY_ITEM_MIN_W = 150;
        /** Тосты: максимальная ширина и отступ сверху (ниже верхней полосы экрана). */
        public static final int TOAST_MAX_W = 220;

        public static final int TOAST_TOP = 36;
        /** Плитка скина (иконка предмета 16 + поля) и максимум колонок. */
        public static final int SKIN_TILE = 28;

        public static final int SKIN_MAX_COLS = 12;
        /** Админ-формы (pwp-warfare AdminFormScreen): строка, секция, колонки, подписи, кнопки. */
        public static final int FORM_ROW_H = 18;

        public static final int FORM_SECTION_H = 16;
        public static final int FORM_TOGGLE_W = 64;
        public static final int FORM_COL_MIN_W = 220;
        public static final int FORM_MAX_W = 380;
        public static final int FORM_MAX_W_TWO = 620;
        public static final float FORM_LABEL_FRACTION = 0.45F;
        public static final int FORM_ACTION_MAX_W = 140;
        /** Экран итогов матча (VictoryScreen) и нока (DownedScreen). */
        public static final int VICTORY_PANEL_MAX_W = 420;

        public static final int VICTORY_STAT_MIN_W = 80;
        public static final int VICTORY_STAT_H = 24;
        public static final int DOWNED_PANEL_W = 320;
        public static final float DOWNED_VIGNETTE = 0.35F;

        private Spacing() {}
    }

    // ============================================================
    //  АНИМАЦИИ
    // ============================================================

    public static class Animations {

        public static final int DURATION_INSTANT = 50;
        public static final int DURATION_FAST = 100;
        public static final int DURATION_NORMAL = 200;
        public static final int DURATION_SLOW = 400;
        public static final int DURATION_GLACIAL = 800;

        public static final int STAGGER_FAST = 30;
        public static final int STAGGER_NORMAL = 60;
        public static final int STAGGER_SLOW = 100;

        /** Нажатие кнопки (реакция на mouse down). */
        public static final int DURATION_PRESS = 60;
        /** Вход экрана: длительность появления элемента и сдвиг снизу (px). */
        public static final int ENTER = 260;

        public static final int ENTER_SLIDE = 10;
        /** Период бегущего индикатора загрузки (мс). */
        public static final int LOADER_PERIOD = 1400;

        public static final int FADE_IN = 0;
        public static final int FADE_OUT = 1;
        public static final int SLIDE_UP = 2;
        public static final int SLIDE_DOWN = 3;
        public static final int SLIDE_LEFT = 4;
        public static final int SLIDE_RIGHT = 5;
        public static final int SCALE_IN = 6;
        public static final int SCALE_OUT = 7;
        public static final int BOUNCE = 8;
        public static final int PULSE = 9;
        public static final int SHAKE = 10;

        private Animations() {}
    }

    // ============================================================
    //  ТЕНИ
    // ============================================================

    public static class Shadows {

        public static final int LEVEL_0 = 0x00000000;
        public static final int LEVEL_1 = 0x22000000;
        public static final int LEVEL_2 = 0x44000000;
        public static final int LEVEL_3 = 0x66000000;

        public static final int ACCENT_GLOW_SMALL = 0x22C8812A;
        public static final int ACCENT_GLOW_MEDIUM = 0x44C8812A;
        public static final int ACCENT_GLOW_LARGE = 0x66C8812A;

        private Shadows() {}
    }

    private PWPTheme() {}

    // ============================================================
    //  ТИПОГРАФИКА (ванильный шрифт: строка 9px, масштаб — только целый)
    // ============================================================

    public static class Text {
        /** Межстрочный интервал поверх font.lineHeight. */
        public static final int LINE_GAP = 2;
        /** Целочисленный масштаб логотипа по плотности (COMPACT / NORMAL / LARGE). */
        public static final int LOGO_SCALE_COMPACT = 2;

        public static final int LOGO_SCALE = 4;
        public static final int LOGO_SCALE_LARGE = 5;

        public static final int HEADING = Colors.TEXT_PRIMARY;
        public static final int BODY = Colors.TEXT_PRIMARY;
        public static final int MUTED = Colors.TEXT_SECONDARY;
        public static final int CAPTION = 0xFF8C9097;
        public static final int ACCENT = Colors.TEXT_ACCENT;
        public static final int DANGER = 0xFFD05858;
        public static final int LOGO = Colors.ACCENT;

        private Text() {}
    }

    // ============================================================
    //  ТЕКСТУРЫ UI-кита (генератор tools/generate_ui_textures.py)
    // ============================================================

    public static class Textures {
        private static net.minecraft.resources.ResourceLocation ui(String name) {
            return new net.minecraft.resources.ResourceLocation("pwp_core_client", "textures/gui/ui/" + name + ".png");
        }

        /** Тайл шума 64x64 (повтор через GL_REPEAT, без растяжения). */
        public static final net.minecraft.resources.ResourceLocation NOISE = ui("noise");
        /** Тайл скан-лайнов 64x64 (каждая вторая строка). */
        public static final net.minecraft.resources.ResourceLocation SCANLINES = ui("scanlines");

        public static final int TILE_SIZE = 64;
        /** 9-slice рамка 32x32 с уголками-скобами, угол {@link Spacing#FRAME_CORNER}. */
        public static final net.minecraft.resources.ResourceLocation FRAME = ui("frame");

        public static final int FRAME_SIZE = 32;
        /** 9-slice мягкая тень 32x32, угол {@link Spacing#SHADOW_CORNER}. */
        public static final net.minecraft.resources.ResourceLocation SHADOW = ui("shadow");

        public static final int SHADOW_SIZE = 32;
        /** Атлас иконок 128x32 (ячейки 16x16), индексы — ui.UIIcon. */
        public static final net.minecraft.resources.ResourceLocation ICONS = ui("icons");

        public static final int ICONS_W = 128;
        public static final int ICONS_H = 32;
        public static final int ICON_CELL = 16;

        private Textures() {}
    }

    // ============================================================
    //  СТИЛИ КОМПОНЕНТОВ
    // ============================================================

    public static class Styles {

        public static class Button {
            public static final int PRIMARY_BG = Colors.SURFACE;
            public static final int PRIMARY_BORDER = Colors.BORDER_ACCENT;
            public static final int PRIMARY_HOVER = Colors.SURFACE_LIGHT;
            public static final int PRIMARY_PRESSED = 0xFF242A34;
            public static final int PRIMARY_TEXT = Colors.TEXT_PRIMARY;
            public static final int PRIMARY_DISABLED_TEXT = Colors.TEXT_DIM;

            public static final int ACCENT_BG = Colors.ACCENT;
            public static final int ACCENT_HOVER = Colors.ACCENT_SOFT;
            public static final int ACCENT_PRESSED = Colors.ACCENT_DIM;
            public static final int ACCENT_TEXT = 0xFF0A0C0E;
            public static final int ACCENT_DISABLED = 0x664A4D54;

            public static final int DANGER_BG = Colors.DANGER;
            public static final int DANGER_HOVER = 0xFF803030;
            public static final int DANGER_PRESSED = 0xFF602020;
            public static final int DANGER_TEXT = Colors.TEXT_PRIMARY;

            public static final int DARK_BG = 0xFF181C24;
            public static final int DARK_BORDER = Colors.BORDER;
            public static final int DARK_HOVER = Colors.SURFACE_LIGHT;
            public static final int DARK_PRESSED = 0xFF222834;
            public static final int DARK_TEXT = Colors.TEXT_SECONDARY;
        }

        public static class Tab {
            public static final int BG = Colors.SURFACE;
            public static final int BG_HOVER = Colors.SURFACE_LIGHT;
            public static final int BG_ACTIVE = 0xFF1A1E26;
            public static final int BORDER = Colors.BORDER;
            public static final int BORDER_ACTIVE = Colors.ACCENT;
            public static final int TEXT = Colors.TEXT_SECONDARY;
            public static final int TEXT_ACTIVE = Colors.TEXT_ACCENT;
        }

        public static class Card {
            public static final int BG = Colors.SURFACE;
            public static final int BG_HOVER = Colors.SURFACE_LIGHT;
            public static final int BG_SELECTED = 0xFF2A2010;
            public static final int BORDER = Colors.BORDER;
            public static final int BORDER_HOVER = Colors.BORDER_FOCUS;
            public static final int BORDER_SELECTED = Colors.ACCENT;
            public static final int SHADOW = Shadows.LEVEL_1;
            public static final int GLOW_SELECTED = Shadows.ACCENT_GLOW_SMALL;
        }

        public static class Input {
            public static final int BG = 0xFF0E1117;
            public static final int BG_FOCUS = Colors.SURFACE;
            public static final int BORDER = Colors.BORDER;
            public static final int BORDER_FOCUS = Colors.BORDER_ACCENT;
            public static final int TEXT = Colors.TEXT_PRIMARY;
            public static final int PLACEHOLDER = Colors.TEXT_DIM;
        }

        public static class Panel {
            public static final int BG = Colors.SURFACE;
            public static final int BG_HOVER = Colors.SURFACE_LIGHT;
            public static final int BORDER = Colors.BORDER;
            public static final int BORDER_HOVER = Colors.BORDER_ACCENT;
            public static final int SHADOW = Shadows.LEVEL_2;
        }

        public static class Progress {
            public static final int BG = 0xFF181C24;
            public static final int FILL_XP = Colors.ACCENT;
            public static final int FILL_HEALTH = Colors.SUCCESS;
            public static final int FILL_TICKET_BLUE = Colors.TEAM_BLUE;
            public static final int FILL_TICKET_RED = Colors.TEAM_RED;
            public static final int FILL_LEVEL = Colors.INFO;
            public static final int TRACK_BG = 0x2AC8812A;
        }

        public static class Toast {
            public static final int BG_SUCCESS = 0xCC2A5A2E;
            public static final int BG_ERROR = 0xCC5A2A2A;
            public static final int BG_INFO = 0xCC2A3A5A;
            public static final int BG_XP = 0xCC5A3A1A;
            public static final int TEXT = Colors.TEXT_PRIMARY;
        }

        public static class RarityHighlight {
            public static final int COMMON_BG = 0x226A6D73;
            public static final int COMMON_BORDER = 0xFF4A4D54;
            public static final int RARE_BG = 0x223D6FA5;
            public static final int RARE_BORDER = 0xFF3D6FA5;
            public static final int EPIC_BG = 0x227A4A8A;
            public static final int EPIC_BORDER = 0xFF7A4A8A;
            public static final int LEGENDARY_BG = 0x22C8812A;
            public static final int LEGENDARY_BORDER = 0xFFC8812A;
            public static final int MYTHIC_BG = 0x22A53D3D;
            public static final int MYTHIC_BORDER = 0xFFA53D3D;
        }

        public static class ScrollBar {
            public static final int TRACK = Colors.SURFACE_DIM;
            public static final int THUMB = Colors.ACCENT;
            public static final int THUMB_HOVER = Colors.ACCENT_SOFT;
        }

        public static class Badge {
            public static final int BG_SUCCESS = Colors.SUCCESS;
            public static final int BG_DANGER = Colors.DANGER;
            public static final int BG_WARNING = Colors.WARNING;
            public static final int BG_INFO = Colors.INFO;
            public static final int TEXT = Colors.TEXT_PRIMARY;
            public static final int TEXT_DARK = 0xFF0A0C0E;
        }

        public static class Tooltip {
            public static final int BG = 0xCC0E1117;
            public static final int BORDER = Colors.BORDER_LIGHT;
            public static final int TEXT = Colors.TEXT_PRIMARY;
        }

        public static class Divider {
            public static final int COLOR = Colors.BORDER;
        }

        /** Радиальное меню UI-кита (ui.UIRadialMenu). */
        public static class Radial {
            public static final int OVERLAY = Colors.BACKGROUND_DIM;
            public static final int SECTOR = 0xCC12151A;
            public static final int SECTOR_HOVER = 0xE01A1E26;
            public static final int SECTOR_DISABLED = 0x8812151A;
            public static final int SECTOR_BORDER = Colors.BORDER_LIGHT;
            public static final int SECTOR_ACTIVE_EDGE = Colors.ACCENT;
            public static final int CENTER = Colors.SURFACE;
            public static final int TEXT = Colors.TEXT_PRIMARY;
            public static final int TEXT_DISABLED = Colors.TEXT_DIM;
            /** Внутренний и внешний радиус в долях меньшей стороны экрана. */
            public static final float INNER_RADIUS = 0.08F;

            public static final float OUTER_RADIUS = 0.26F;
        }

        /** Модальное окно и контекстное меню UI-кита. */
        public static class Modal {
            public static final int OVERLAY = Colors.BACKGROUND_DIM;
            public static final int BG = Colors.SURFACE;
            public static final int HEADER_BG = Colors.SURFACE_TOP;
            public static final int BORDER = Colors.BORDER_ACCENT;
            public static final int TITLE = Colors.TEXT_ACCENT;
            public static final int TEXT = Colors.TEXT_PRIMARY;
            public static final int MENU_ITEM_HOVER = Colors.SURFACE_LIGHT;
            public static final int MENU_SEPARATOR = Colors.BORDER;
        }

        // ============================================================
        //  Редизайн 2026-11: тактический стиль (Squad / Hell Let Loose)
        // ============================================================

        /** Стеклянная панель поверх затемнённого мира: полупрозрачно + градиент + 1px обводка. */
        public static class Glass {
            public static final int BG = 0xB80B0E12;
            public static final int BG_STRONG = 0xE00A0C10;
            /** Вертикальный градиент поверх заливки (сверху светлее). */
            public static final int SHEEN_TOP = 0x16FFFFFF;

            public static final int SHEEN_BOTTOM = 0x00FFFFFF;
            public static final int BORDER = 0x40FFFFFF;
            /** Внутренняя 1px линия-хайлайт у верхней кромки. */
            public static final int HIGHLIGHT = 0x1AFFFFFF;
            /** Тинт 9-slice рамки frame.png (уголки-скобы). */
            public static final int FRAME_TINT = 0x8CC8CBCE;

            public static final int HEADER_TOP = 0x40000000;
            public static final int HEADER_BOTTOM = 0x14000000;
            public static final int HEADER_DIVIDER = 0x33FFFFFF;
            /** Тинт тени shadow.png под панелью. */
            public static final int SHADOW_TINT = 0x99000000;
            /** Тинт тайла шума поверх панели. */
            public static final int NOISE_TINT = 0x59FFFFFF;
        }

        /** Фон экрана поверх картинки мира. */
        public static class Backdrop {
            /** Общее затемнение (верх/низ вертикального градиента). */
            public static final int SCRIM_TOP = 0x8C05070A;

            public static final int SCRIM_BOTTOM = 0xD905070A;
            /** Тень от левого края под колонку меню (слева -> прозрачно), доля ширины. */
            public static final int SIDE_SHADE = 0xC0000000;

            public static final float SIDE_SHADE_FRACTION = 0.55F;
            /** Виньетка: цвет края и доля меньшей стороны экрана, на которую она заходит. */
            public static final int VIGNETTE = 0xA8000000;

            public static final float VIGNETTE_FRACTION = 0.28F;
            public static final int NOISE_TINT = 0x73FFFFFF;
            public static final int SCANLINE_TINT = 0x12FFFFFF;
            /** Полосы шапки/подвала экрана. */
            public static final int BAR_TOP = 0xA0000000;

            public static final int BAR_BOTTOM = 0x60000000;
            public static final int BAR_LINE = 0x26FFFFFF;
        }

        /** Кнопки/карточки UI-кита (UIButton, UICard) в тактическом стиле. */
        public static class Control {
            public static final int BG = 0x99101318;
            public static final int BG_HOVER = 0xCC1A1E26;
            public static final int BG_PRESSED = 0xE0080A0D;
            public static final int BG_SELECTED = 0xCC231C10;
            public static final int BG_DISABLED = 0x59101318;
            public static final int SHEEN_TOP = 0x12FFFFFF;
            public static final int SHEEN_BOTTOM = 0x00FFFFFF;
            public static final int BORDER = 0x33FFFFFF;
            public static final int BORDER_HOVER = 0x73FFFFFF;
            public static final int BORDER_SELECTED = Colors.ACCENT_DIM;
            public static final int BORDER_DISABLED = 0x14FFFFFF;
            public static final int ACCENT_BAR = Colors.ACCENT;
            public static final int TEXT = Colors.TEXT_PRIMARY;
            public static final int TEXT_HOVER = 0xFFF0F1F2;
            public static final int TEXT_DISABLED = Colors.TEXT_DIM;
            public static final int SUBTEXT = Colors.TEXT_SECONDARY;
            public static final int ICON = Colors.TEXT_SECONDARY;
            public static final int ICON_HOVER = Colors.ACCENT;
            /** Главная (акцентная) кнопка: вертикальный градиент. */
            public static final int PRIMARY_TOP = 0xF0D08A30;

            public static final int PRIMARY_BOTTOM = 0xF0A0661C;
            public static final int PRIMARY_TOP_HOVER = 0xFFE09A3C;
            public static final int PRIMARY_BOTTOM_HOVER = 0xFFB07424;
            public static final int PRIMARY_BORDER = 0xFFE8B060;
            public static final int PRIMARY_TEXT = 0xFF0A0C0E;
            public static final int DANGER_TOP = 0xE0A53D3D;
            public static final int DANGER_BOTTOM = 0xE0702424;
            public static final int DANGER_BORDER = 0xFFC86060;
        }

        /** Цвета категорий ролей (иконки UIIcon.DIRECT_COMBAT..SUPPORT). */
        public static class Role {
            public static final int DIRECT_COMBAT = 0xFFC8CBCE;
            public static final int FIRE_SUPPORT = 0xFFC8812A;
            public static final int SPECIALIST = 0xFF5F8FC4;
            public static final int SUPPORT = 0xFF4CAF50;
        }

        /** Индикатор загрузки. */
        public static class Loader {
            public static final int TRACK = 0x33FFFFFF;
            public static final int FILL = Colors.ACCENT;
            public static final int FILL_GLOW = 0x40C8812A;
        }

        private Styles() {}
    }
}
