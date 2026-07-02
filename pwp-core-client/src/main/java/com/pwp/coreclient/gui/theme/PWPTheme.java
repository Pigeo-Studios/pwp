package com.pwp.coreclient.gui.theme;

import java.awt.Color;

public class PWPTheme {

    private PWPTheme() {}

    // ============================================================
    //  ЦВЕТА
    // ============================================================

    public static class Colors {

        // --- Основная палитра ---
        public static final int PRIMARY        = 0xFF2D5F8A;  // Синий (военный)
        public static final int PRIMARY_LIGHT  = 0xFF3A7BB5;
        public static final int PRIMARY_DARK   = 0xFF1A3A5C;

        public static final int SECONDARY      = 0xFF455A64;  // Серо-синий
        public static final int SECONDARY_LIGHT= 0xFF607D8B;

        // --- Акцент ---
        public static final int ACCENT         = 0xFFF0C040;  // Золотой (XP, валюта, премиум)
        public static final int ACCENT_SOFT    = 0xFFD4A020;
        public static final int ACCENT_GLOW    = 0x66F0C040;  // Полупрозрачное свечение

        // --- Команды ---
        public static final int TEAM_BLUE      = 0xFF4A90D9;
        public static final int TEAM_BLUE_DIM  = 0x664A90D9;
        public static final int TEAM_RED       = 0xFFE53935;
        public static final int TEAM_RED_DIM   = 0x66E53935;

        // --- Состояния ---
        public static final int SUCCESS        = 0xFF4CAF50;  // Зелёный (покупка, +
        public static final int WARNING        = 0xFFFFA726;  // Оранжевый (предупреждение)
        public static final int DANGER         = 0xFFE53935;  // Красный (опасность, -
        public static final int INFO           = 0xFF42A5F5;  // Голубой (инфо)

        // --- Фоны ---
        public static final int BACKGROUND     = 0xFF0D1117;  // Почти чёрный (основной фон)
        public static final int BACKGROUND_DIM = 0x80000000;  // Затемнение
        public static final int SURFACE        = 0xFF1E2433;  // Тёмно-серый (карточки)
        public static final int SURFACE_LIGHT  = 0xFF2A2F3F;  // Карточки при наведении
        public static final int SURFACE_DIM    = 0x661E2433;  // Полупрозрачная карточка
        public static final int SURFACE_TOP    = 0xCC1A1F2E;  // Верхняя панель

        // --- Текст ---
        public static final int TEXT_PRIMARY   = 0xFFE8EAED;  // Белый (основной)
        public static final int TEXT_SECONDARY = 0xFF9AA0A6;  // Серый (второстепенный)
        public static final int TEXT_DIM       = 0xFF5F6368;  // Тёмно-серый (неактивный)
        public static final int TEXT_ACCENT    = 0xFFF0C040;  // Золотой (важные цифры)
        public static final int TEXT_LINK      = 0xFF42A5F5;  // Ссылка

        // --- Границы ---
        public static final int BORDER         = 0xFF2D3142;
        public static final int BORDER_LIGHT   = 0xFF3D4356;
        public static final int BORDER_FOCUS   = 0xFF4A90D9;

        // --- Редкость предметов ---
        public static final int RARITY_COMMON  = 0xFF9E9E9E;  // Серый
        public static final int RARITY_RARE    = 0xFF42A5F5;  // Синий
        public static final int RARITY_EPIC    = 0xFFAB47BC;  // Фиолетовый
        public static final int RARITY_LEGENDARY = 0xFFF0C040; // Золотой
        public static final int RARITY_MYTHIC  = 0xFFE53935;  // Красный

        // --- Градиенты (стартовый цвет) ---
        public static final int GRADIENT_PRIMARY_START = PRIMARY;
        public static final int GRADIENT_PRIMARY_END   = PRIMARY_DARK;
        public static final int GRADIENT_GOLD_START    = 0xFFF0C040;
        public static final int GRADIENT_GOLD_END      = 0xFFD4A020;

        private Colors() {}

        // Хелпер: получить Color из int
        public static Color of(int color) {
            return new Color(color, true);
        }

        // Хелпер: смена альфы
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
        public static final int ICON_SIZE      = 16;
        public static final int AVATAR_SIZE    = 32;
        public static final int ITEM_PREVIEW   = 80;

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
        public static final String COIN        = "\u25CB";  // ○ (сделать текстурой монетки)
        public static final String XP_ICON     = "\u2606";  // ☆
        public static final String LEVEL_ICON  = "\u2690";  // ⚐

        private Icons() {}
    }

    // ============================================================
    //  СТИЛИ КОМПОНЕНТОВ
    // ============================================================

    public static class Styles {

        // Стили кнопок
        public static class Button {
            // Основная (голубая)
            public static final int PRIMARY_BG       = Colors.PRIMARY;
            public static final int PRIMARY_HOVER    = Colors.PRIMARY_LIGHT;
            public static final int PRIMARY_TEXT     = Colors.TEXT_PRIMARY;

            // Акцентная (золотая — для важных действий: купить, улучшить)
            public static final int ACCENT_BG        = Colors.ACCENT;
            public static final int ACCENT_HOVER     = Colors.ACCENT_SOFT;
            public static final int ACCENT_TEXT      = 0xFF1A1A1A;

            // Опасная (красная — удалить, сбросить)
            public static final int DANGER_BG        = Colors.DANGER;
            public static final int DANGER_HOVER     = 0xFFC62828;
            public static final int DANGER_TEXT      = Colors.TEXT_PRIMARY;

            // Тёмная (второстепенные действия)
            public static final int DARK_BG          = Colors.SURFACE;
            public static final int DARK_HOVER       = Colors.SURFACE_LIGHT;
            public static final int DARK_TEXT        = Colors.TEXT_PRIMARY;
        }

        // Стили полей ввода
        public static class Input {
            public static final int BG               = Colors.SURFACE;
            public static final int BG_FOCUS         = Colors.SURFACE_LIGHT;
            public static final int BORDER           = Colors.BORDER;
            public static final int BORDER_FOCUS     = Colors.BORDER_FOCUS;
            public static final int TEXT             = Colors.TEXT_PRIMARY;
            public static final int PLACEHOLDER      = Colors.TEXT_DIM;
        }

        // Стили панелей/карточек
        public static class Panel {
            public static final int BG               = Colors.SURFACE;
            public static final int BG_HOVER          = Colors.SURFACE_LIGHT;
            public static final int BORDER           = Colors.BORDER;
            public static final int BORDER_HOVER     = Colors.BORDER_LIGHT;
            public static final int SHADOW           = Shadows.LEVEL_1;
        }

        // Стили полосы прогресса
        public static class Progress {
            public static final int BG               = 0xFF2A2F3F;
            public static final int FILL_XP          = Colors.ACCENT;
            public static final int FILL_HEALTH      = Colors.SUCCESS;
            public static final int FILL_TICKET_BLUE = Colors.TEAM_BLUE;
            public static final int FILL_TICKET_RED  = Colors.TEAM_RED;
            public static final int FILL_LEVEL       = Colors.INFO;
        }

        // Стили уведомлений (Toast)
        public static class Toast {
            public static final int BG_SUCCESS       = 0xCC2E7D32;  // Зелёный
            public static final int BG_ERROR         = 0xCCC62828;  // Красный
            public static final int BG_INFO          = 0xCC1565C0;  // Синий
            public static final int BG_XP            = 0xCCF0C040;  // Золотой
            public static final int TEXT             = Colors.TEXT_PRIMARY;
        }

        // Стили редкости (для подсветки предметов)
        public static class RarityHighlight {
            public static final int COMMON_BG    = 0x339E9E9E;
            public static final int COMMON_BORDER= 0xFF9E9E9E;
            public static final int RARE_BG      = 0x3342A5F5;
            public static final int RARE_BORDER  = 0xFF42A5F5;
            public static final int EPIC_BG      = 0x33AB47BC;
            public static final int EPIC_BORDER  = 0xFFAB47BC;
            public static final int LEGENDARY_BG = 0x33F0C040;
            public static final int LEGENDARY_BORDER = 0xFFF0C040;
        }

        private Styles() {}
    }
}
