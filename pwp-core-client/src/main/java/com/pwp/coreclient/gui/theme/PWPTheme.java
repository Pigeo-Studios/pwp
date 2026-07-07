package com.pwp.coreclient.gui.theme;

import java.awt.Color;

public class PWPTheme {

    private PWPTheme() {}

    // ============================================================
    //  ЦВЕТА
    // ============================================================

    public static class Colors {

        // ============================================================
        //  Dark Ember — минималистичная тёмная тема с приглушённым
        //  оранжевым акцентом в стиле "теневой обводки"
        // ============================================================

        // --- Фоны ---
        public static final int BACKGROUND      = 0xFF0A0C0E;  // Самый тёмный (основной фон)
        public static final int BACKGROUND_DIM  = 0xCC06080A;  // Затемнение/оверлей
        public static final int BACKGROUND_GRADIENT_START = 0xFF080A0D;
        public static final int BACKGROUND_GRADIENT_END   = 0xFF0D1015;

        public static final int SURFACE         = 0xFF12151A;  // Карточки / панели
        public static final int SURFACE_LIGHT   = 0xFF1A1E26;  // При наведении
        public static final int SURFACE_DIM     = 0x6612151A;  // Полупрозрачная
        public static final int SURFACE_TOP     = 0xCC0E1117;  // Верхняя панель/хедер

        // --- Акцент "теневая обводка" (приглушённый оранжевый) ---
        public static final int ACCENT          = 0xFFC8812A;  // Основной оранжевый
        public static final int ACCENT_DIM      = 0xFF8B6220;  // Тёмный (для обводок)
        public static final int ACCENT_GLOW     = 0x18C8812A;  // Очень прозрачное свечение
        public static final int ACCENT_BORDER   = 0xFF6B4A18;  // Тень-граница
        public static final int ACCENT_SHADOW   = 0xFF3A2A0F;  // Глубокая тень
        public static final int ACCENT_SOFT     = 0xFFA06E22;  // Мягкий

        // --- Команды (приглушённые) ---
        public static final int TEAM_BLUE       = 0xFF3D6FA5;
        public static final int TEAM_BLUE_DIM   = 0x553D6FA5;
        public static final int TEAM_BLUE_DARK  = 0xFF1A2E45;
        public static final int TEAM_RED        = 0xFFA53D3D;
        public static final int TEAM_RED_DIM    = 0x55A53D3D;
        public static final int TEAM_RED_DARK   = 0xFF451A1A;

        // --- Состояния (приглушённые) ---
        public static final int SUCCESS         = 0xFF3D7A40;  // Зелёный
        public static final int SUCCESS_DIM     = 0x553D7A40;
        public static final int SUCCESS_LIGHT   = 0xFF4CAF50;
        public static final int WARNING         = 0xFFC8812A;  // Оранжевый = акцент
        public static final int DANGER          = 0xFFA53D3D;  // Красный
        public static final int DANGER_DIM      = 0x55A53D3D;
        public static final int INFO            = 0xFF3D6FA5;  // Синий

        // --- Текст ---
        public static final int TEXT_PRIMARY    = 0xFFC8CBCE;  // Мягкий светло-серый
        public static final int TEXT_SECONDARY  = 0xFF7A7D84;  // Серый
        public static final int TEXT_DIM        = 0xFF4A4D54;  // Тёмно-серый (неактив)
        public static final int TEXT_ACCENT     = 0xFFC8812A;  // Оранжевый (важные цифры)

        // --- Границы ---
        public static final int BORDER          = 0xFF1E222A;  // Едва заметная
        public static final int BORDER_LIGHT    = 0xFF2A2F3A;  // Светлая
        public static final int BORDER_ACCENT   = 0xFF6B4A18;  // Оранжевая тень-граница
        public static final int BORDER_FOCUS    = 0xFFC8812A;  // Оранжевый фокус

        // --- Редкость предметов (приглушённая) ---
        public static final int RARITY_COMMON   = 0xFF6A6D73;
        public static final int RARITY_RARE     = 0xFF3D6FA5;
        public static final int RARITY_EPIC     = 0xFF7A4A8A;
        public static final int RARITY_LEGENDARY= 0xFFC8812A;
        public static final int RARITY_MYTHIC   = 0xFFA53D3D;

        // --- Градиенты ---
        public static final int GRADIENT_BG_START    = BACKGROUND_GRADIENT_START;
        public static final int GRADIENT_BG_END      = BACKGROUND_GRADIENT_END;
        public static final int GRADIENT_ACCENT_START= 0xFFC8812A;
        public static final int GRADIENT_ACCENT_END  = 0xFF6B4A18;

        private Colors() {}

        public static Color of(int color) {
            return new Color(color, true);
        }

        public static int withAlpha(int color, int alpha) {
            return (alpha << 24) | (color & 0x00FFFFFF);
        }
    }

    // ============================================================
    //  ШРИФТЫ
    // ============================================================

    public static class Fonts {

        public static final String DEFAULT = "mojang";  // Стандартный Minecraft шрифт

        // Размеры
        public static final int SIZE_TINY        = 8;
        public static final int SIZE_SMALL       = 10;
        public static final int SIZE_NORMAL      = 12;
        public static final int SIZE_MEDIUM      = 14;
        public static final int SIZE_LARGE       = 16;
        public static final int SIZE_XLARGE      = 20;
        public static final int SIZE_HUGE        = 24;
        public static final int SIZE_TITLE       = 32;

        private Fonts() {}
    }

    // ============================================================
    //  РАЗМЕРЫ И ОТСТУПЫ
    // ============================================================

    public static class Spacing {

        public static final int XXS = 2;
        public static final int XS  = 4;
        public static final int SM  = 8;
        public static final int MD  = 12;
        public static final int LG  = 16;
        public static final int XL  = 24;
        public static final int XXL = 32;
        public static final int HUGE = 48;

        // Скругление углов
        public static final int RADIUS_NONE    = 0;
        public static final int RADIUS_SMALL   = 3;
        public static final int RADIUS_MEDIUM  = 6;
        public static final int RADIUS_LARGE   = 10;
        public static final int RADIUS_ROUND   = 999;

        // Размеры компонентов
        public static final int BUTTON_HEIGHT  = 24;
        public static final int BUTTON_WIDTH   = 120;
        public static final int BUTTON_SMALL_HEIGHT = 18;
        public static final int BUTTON_TAB_HEIGHT   = 22;
        public static final int ICON_SIZE      = 16;
        public static final int ICON_SIZE_MED  = 24;
        public static final int ICON_SIZE_LG   = 32;
        public static final int AVATAR_SIZE    = 32;
        public static final int ITEM_PREVIEW   = 80;
        public static final int SCROLLBAR_WIDTH = 5;
        public static final int PANEL_PADDING  = 12;
        public static final int CARD_PADDING   = 10;
        public static final int SECTION_GAP    = 24;
        public static final int ELEMENT_GAP    = 6;
        public static final int GRID_GAP       = 8;

        private Spacing() {}
    }

    // ============================================================
    //  АНИМАЦИИ
    // ============================================================

    public static class Animations {

        // Длительности (мс)
        public static final int DURATION_INSTANT  = 50;
        public static final int DURATION_FAST     = 100;
        public static final int DURATION_NORMAL   = 200;
        public static final int DURATION_SLOW     = 400;
        public static final int DURATION_GLACIAL  = 800;

        // Задержки для каскадных анимаций
        public static final int STAGGER_FAST    = 30;
        public static final int STAGGER_NORMAL  = 60;
        public static final int STAGGER_SLOW    = 100;

        // Типы анимаций
        public static final int FADE_IN    = 0;
        public static final int FADE_OUT   = 1;
        public static final int SLIDE_UP   = 2;
        public static final int SLIDE_DOWN = 3;
        public static final int SLIDE_LEFT = 4;
        public static final int SLIDE_RIGHT= 5;
        public static final int SCALE_IN   = 6;
        public static final int SCALE_OUT  = 7;
        public static final int BOUNCE     = 8;
        public static final int PULSE      = 9;
        public static final int SHAKE      = 10;

        private Animations() {}
    }

    // ============================================================
    //  ТЕНИ
    // ============================================================

    public static class Shadows {

        public static final int LEVEL_0 = 0x00000000;
        public static final int LEVEL_1 = 0x22000000;  // Лёгкая
        public static final int LEVEL_2 = 0x44000000;  // Средняя
        public static final int LEVEL_3 = 0x66000000;  // Глубокая

        // Оранжевая тень-свечение (теневая обводка)
        public static final int ACCENT_GLOW_SMALL = 0x22C8812A;  // Маленькое
        public static final int ACCENT_GLOW_MEDIUM= 0x44C8812A;  // Среднее
        public static final int ACCENT_GLOW_LARGE = 0x66C8812A;  // Большое

        private Shadows() {}
    }

    // ============================================================
    //  ИКОНКИ (Unicode / символы)
    // ============================================================

    public static class Icons {

        public static final String STAR        = "\u2605";  // ★
        public static final String STAR_OUTLINE= "\u2606";  // ☆
        public static final String HEART       = "\u2764";  // ❤
        public static final String CROSS       = "\u2716";  // ✖
        public static final String CHECK       = "\u2714";  // ✔
        public static final String ARROW_UP    = "\u25B2";  // ▲
        public static final String ARROW_DOWN  = "\u25BC";  // ▼
        public static final String ARROW_LEFT  = "\u25C0";  // ◀
        public static final String ARROW_RIGHT = "\u25B6";  // ▶
        public static final String TRIANGLE    = "\u25B6";  // ▶
        public static final String CIRCLE      = "\u25CF";  // ●
        public static final String SQUARE      = "\u25A0";  // ■
        public static final String DIAMOND     = "\u25C6";  // ◆
        public static final String SKULL       = "\u2620";  // ☠
        public static final String STARBURST   = "\u2726";  // ✦
        public static final String DAGGER      = "\u2020";  // †
        public static final String CROWN       = "\u265B";  // ♛
        public static final String BULLET      = "\u2022";  // •
        public static final String INFINITY    = "\u221E";  // ∞
        public static final String LIGHTNING   = "\u26A1";  // ⚡
        public static final String GEAR        = "\u2699";  // ⚙
        public static final String FLAG        = "\u2691";  // ⚑
        public static final String CROSSHAIR   = "\u2316";  // ⌖
        public static final String PLUS        = "+";
        public static final String MINUS       = "-";
        public static final String COIN        = "\u25CB";  // ○
        public static final String XP_ICON     = "\u2606";  // ☆
        public static final String LEVEL_ICON  = "\u2690";  // ⚐
        public static final String TROPHY      = "\u265B";  // ♛
        public static final String SWORDS      = "\u2694";  // ⚔
        public static final String SHIELD      = "\u26E8";  // ⛨

        private Icons() {}
    }

    // ============================================================
    //  СТИЛИ КОМПОНЕНТОВ
    // ============================================================

    public static class Styles {

        // Стили кнопок
        public static class Button {
            // Основная (тёмная с оранжевой обводкой)
            public static final int PRIMARY_BG       = Colors.SURFACE;
            public static final int PRIMARY_BORDER   = Colors.BORDER_ACCENT;
            public static final int PRIMARY_HOVER    = Colors.SURFACE_LIGHT;
            public static final int PRIMARY_PRESSED  = 0xFF242A34;
            public static final int PRIMARY_TEXT     = Colors.TEXT_PRIMARY;
            public static final int PRIMARY_DISABLED_TEXT = Colors.TEXT_DIM;

            // Акцентная (оранжевая — купить, улучшить, важно)
            public static final int ACCENT_BG        = Colors.ACCENT;
            public static final int ACCENT_HOVER     = Colors.ACCENT_SOFT;
            public static final int ACCENT_PRESSED   = Colors.ACCENT_DIM;
            public static final int ACCENT_TEXT      = 0xFF0A0C0E;
            public static final int ACCENT_DISABLED  = 0x664A4D54;

            // Опасная (красная)
            public static final int DANGER_BG        = Colors.DANGER;
            public static final int DANGER_HOVER     = 0xFF803030;
            public static final int DANGER_PRESSED   = 0xFF602020;
            public static final int DANGER_TEXT      = Colors.TEXT_PRIMARY;

            // Тёмная (второстепенная, без обводки)
            public static final int DARK_BG          = 0xFF181C24;
            public static final int DARK_BORDER      = Colors.BORDER;
            public static final int DARK_HOVER       = Colors.SURFACE_LIGHT;
            public static final int DARK_PRESSED     = 0xFF222834;
            public static final int DARK_TEXT        = Colors.TEXT_SECONDARY;
        }

        // Стили табов (вкладок)
        public static class Tab {
            public static final int BG               = Colors.SURFACE;
            public static final int BG_HOVER         = Colors.SURFACE_LIGHT;
            public static final int BG_ACTIVE        = 0xFF1A1E26;
            public static final int BORDER           = Colors.BORDER;
            public static final int BORDER_ACTIVE    = Colors.ACCENT;
            public static final int TEXT             = Colors.TEXT_SECONDARY;
            public static final int TEXT_ACTIVE      = Colors.TEXT_ACCENT;
        }

        // Стили карточек
        public static class Card {
            public static final int BG               = Colors.SURFACE;
            public static final int BG_HOVER         = Colors.SURFACE_LIGHT;
            public static final int BG_SELECTED      = 0xFF2A2010;
            public static final int BORDER           = Colors.BORDER;
            public static final int BORDER_HOVER     = Colors.BORDER_FOCUS;
            public static final int BORDER_SELECTED  = Colors.ACCENT;
            public static final int SHADOW           = Shadows.LEVEL_1;
        }

        // Стили полей ввода
        public static class Input {
            public static final int BG               = 0xFF0E1117;
            public static final int BG_FOCUS         = Colors.SURFACE;
            public static final int BORDER           = Colors.BORDER;
            public static final int BORDER_FOCUS     = Colors.BORDER_ACCENT;
            public static final int TEXT             = Colors.TEXT_PRIMARY;
            public static final int PLACEHOLDER      = Colors.TEXT_DIM;
        }

        // Стили панелей/карточек
        public static class Panel {
            public static final int BG               = Colors.SURFACE;
            public static final int BG_HOVER          = Colors.SURFACE_LIGHT;
            public static final int BORDER           = Colors.BORDER;
            public static final int BORDER_HOVER     = Colors.BORDER_ACCENT;
            public static final int SHADOW           = Shadows.LEVEL_2;
        }

        // Стили полосы прогресса
        public static class Progress {
            public static final int BG               = 0xFF181C24;
            public static final int FILL_XP          = Colors.ACCENT;
            public static final int FILL_HEALTH      = Colors.SUCCESS;
            public static final int FILL_TICKET_BLUE = Colors.TEAM_BLUE;
            public static final int FILL_TICKET_RED  = Colors.TEAM_RED;
            public static final int FILL_LEVEL       = Colors.INFO;
            public static final int TRACK_BG         = 0x2AC8812A;
        }

        // Стили уведомлений (Toast)
        public static class Toast {
            public static final int BG_SUCCESS       = 0xCC2A5A2E;
            public static final int BG_ERROR         = 0xCC5A2A2A;
            public static final int BG_INFO          = 0xCC2A3A5A;
            public static final int BG_XP            = 0xCC5A3A1A;
            public static final int TEXT             = Colors.TEXT_PRIMARY;
        }

        // Стили редкости (приглушённые, для подсветки)
        public static class RarityHighlight {
            public static final int COMMON_BG    = 0x226A6D73;
            public static final int COMMON_BORDER= 0xFF4A4D54;
            public static final int RARE_BG      = 0x223D6FA5;
            public static final int RARE_BORDER  = 0xFF3D6FA5;
            public static final int EPIC_BG      = 0x227A4A8A;
            public static final int EPIC_BORDER  = 0xFF7A4A8A;
            public static final int LEGENDARY_BG = 0x22C8812A;
            public static final int LEGENDARY_BORDER = 0xFFC8812A;
            public static final int MYTHIC_BG    = 0x22A53D3D;
            public static final int MYTHIC_BORDER= 0xFFA53D3D;
        }

        // Стили скроллбара
        public static class ScrollBar {
            public static final int TRACK           = Colors.SURFACE_DIM;
            public static final int THUMB           = Colors.ACCENT;
            public static final int THUMB_HOVER     = Colors.ACCENT_SOFT;
        }

        // Стили бейджей/меток
        public static class Badge {
            public static final int BG_SUCCESS      = Colors.SUCCESS;
            public static final int BG_DANGER       = Colors.DANGER;
            public static final int BG_WARNING      = Colors.WARNING;
            public static final int BG_INFO         = Colors.INFO;
            public static final int TEXT            = Colors.TEXT_PRIMARY;
            public static final int TEXT_DARK       = 0xFF0A0C0E;
        }

        // Стили тултипов
        public static class Tooltip {
            public static final int BG              = 0xCC0E1117;
            public static final int BORDER          = Colors.BORDER_LIGHT;
            public static final int TEXT            = Colors.TEXT_PRIMARY;
        }

        // Стили разделителей
        public static class Divider {
            public static final int COLOR          = Colors.BORDER;
        }

        private Styles() {}
    }
}
