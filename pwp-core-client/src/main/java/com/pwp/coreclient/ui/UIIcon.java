package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.gui.theme.PWPTheme.Textures;

/**
 * Иконки атласа {@code textures/gui/ui/icons.png} (16x16, белые силуэты — цвет задаётся при
 * рисовании через {@link UIDraw#icon}). Порядок = индекс ячейки; ДОЛЖЕН совпадать с
 * {@code tools/generate_ui_textures.py} (список ICONS).
 */
public enum UIIcon {
    PLAY,
    PROFILE,
    SETTINGS,
    QUIT,
    SINGLEPLAYER,
    WARNING,
    BACK,
    REPLAY,
    DIRECT_COMBAT,
    FIRE_SUPPORT,
    SPECIALIST,
    SUPPORT,
    INFO,
    CHECK,
    CLOSE,
    SERVER,
    KEYBOARD,
    MODS,
    SLIDERS;

    private static final int COLUMNS = Textures.ICONS_W / Textures.ICON_CELL;

    public int u() {
        return (ordinal() % COLUMNS) * Textures.ICON_CELL;
    }

    public int v() {
        return (ordinal() / COLUMNS) * Textures.ICON_CELL;
    }

    /**
     * Иконка категории роли по имени категории из DTO кита
     * ({@code DIRECT_COMBAT/FIRE_SUPPORT/SPECIALIST/SUPPORT}); неизвестная — null.
     */
    public static UIIcon forRoleCategory(String category) {
        if (category == null) return null;
        return switch (category) {
            case "DIRECT_COMBAT" -> DIRECT_COMBAT;
            case "FIRE_SUPPORT" -> FIRE_SUPPORT;
            case "SPECIALIST" -> SPECIALIST;
            case "SUPPORT" -> SUPPORT;
            default -> null;
        };
    }

    /** Цвет категории роли из темы (для иконок ролей); для прочих иконок — цвет текста. */
    public int roleColor() {
        return switch (this) {
            case DIRECT_COMBAT -> Styles.Role.DIRECT_COMBAT;
            case FIRE_SUPPORT -> Styles.Role.FIRE_SUPPORT;
            case SPECIALIST -> Styles.Role.SPECIALIST;
            case SUPPORT -> Styles.Role.SUPPORT;
            default -> Styles.Control.TEXT;
        };
    }
}
