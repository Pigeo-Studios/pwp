package com.pwp.testshop.gui;

public class Theme {

    private Theme() {}

    public static class Colors {
        public static final int PRIMARY        = 0xFF2D5F8A;
        public static final int PRIMARY_LIGHT  = 0xFF3A7BB5;
        public static final int PRIMARY_DARK   = 0xFF1A3A5C;
        public static final int ACCENT         = 0xFFF0C040;
        public static final int ACCENT_SOFT    = 0xFFD4A020;
        public static final int ACCENT_GLOW    = 0x66F0C040;
        public static final int BACKGROUND     = 0xFF0D1117;
        public static final int SURFACE        = 0xFF1E2433;
        public static final int SURFACE_LIGHT  = 0xFF2A2F3F;
        public static final int TEXT_PRIMARY   = 0xFFE8EAED;
        public static final int TEXT_SECONDARY = 0xFF9AA0A6;
        public static final int TEXT_DIM       = 0xFF5F6368;
        public static final int TEXT_ACCENT    = 0xFFF0C040;
        public static final int BORDER         = 0xFF2D3142;
        public static final int BORDER_LIGHT   = 0xFF3D4356;
        public static final int BORDER_FOCUS   = 0xFF4A90D9;
        public static final int SUCCESS        = 0xFF4CAF50;
        public static final int DANGER         = 0xFFE53935;
        public static final int RARITY_COMMON  = 0xFF9E9E9E;
        public static final int RARITY_RARE    = 0xFF42A5F5;
        public static final int RARITY_EPIC    = 0xFFAB47BC;
        public static final int RARITY_LEGENDARY = 0xFFF0C040;

        public static int withAlpha(int color, int alpha) {
            return (alpha << 24) | (color & 0x00FFFFFF);
        }

        private Colors() {}
    }

    public static class Spacing {
        public static final int SM  = 8;
        public static final int MD  = 12;
        public static final int LG  = 16;
        public static final int XL  = 24;
        public static final int XXL = 32;
        public static final int HUGE = 48;
        public static final int RADIUS_SMALL  = 3;
        public static final int RADIUS_MEDIUM = 6;
        public static final int RADIUS_LARGE  = 10;
        public static final int BUTTON_HEIGHT = 24;
        public static final int CARD_WIDTH    = 140;
        public static final int CARD_HEIGHT   = 110;

        private Spacing() {}
    }

    public static class Icons {
        public static final String STAR      = "\u2605";
        public static final String HEART     = "\u2764";
        public static final String CROSS     = "\u2716";
        public static final String CHECK     = "\u2714";
        public static final String COIN      = "\u25CB";
        public static final String CROWN     = "\u265B";
        public static final String LIGHTNING = "\u26A1";
        public static final String GEAR      = "\u2699";
        public static final String DAGGER    = "\u2020";
        public static final String SHIELD    = "\u2726";
        public static final String BULLET    = "\u2022";

        private Icons() {}
    }
}
