package com.pigeostudios.pwp.warfare.client.gui.ui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Иконки атласа {@code assets/pwpwarfare/textures/gui/ui/icons.png} (128x64, ячейки 16x16, белые
 * силуэты — цвет задаётся при рисовании, {@link WDraw#icon}). Порядок = индекс ячейки; ДОЛЖЕН
 * совпадать со списком ICONS в {@code tools/generate_warfare_ui_textures.py}.
 */
@OnlyIn(Dist.CLIENT)
public enum WarfareIcon {
    SPAWN_MAIN,
    SPAWN_RALLY,
    SPAWN_FOB,
    LOCK,
    CLOCK,
    MAP,
    SQUAD,
    CHAT,
    RULES,
    TEAM,
    TICKET,
    DEPLOY,
    PERSON,
    BACKPACK,
    COMPASS,
    TARGET,
    WALL,
    WIRE,
    SHIELD,
    GUN,
    CRATE,
    RADIO,
    MARKER,
    PATH,
    EDIT,
    PLUS,
    MINUS,
    STAR,
    GRID,
    LAYERS,
    TRASH,
    VEHICLE;

    private static final int COLUMNS = WTheme.ICONS_W / WTheme.ICON_CELL;

    public int u() {
        return (ordinal() % COLUMNS) * WTheme.ICON_CELL;
    }

    public int v() {
        return (ordinal() / COLUMNS) * WTheme.ICON_CELL;
    }
}
