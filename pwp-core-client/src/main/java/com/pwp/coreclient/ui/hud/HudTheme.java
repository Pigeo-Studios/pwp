package com.pwp.coreclient.ui.hud;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;

/**
 * Токены HUD матча (этап 2 редизайна HUD) поверх {@link PWPTheme}: общие для всех модов, которые
 * рисуют низ экрана (pwp-movement — хотбар, pwp-limits — HP/броня, pwp-warfare — плашки и
 * контекстный ярус). Размеры — в базовых HUD-единицах (при масштабе HUD 1 = GUI-пиксели);
 * масштаб применяется через {@link HudDraw#begin}. Сам PWPTheme правит владелец core-client.
 */
public final class HudTheme {
    private HudTheme() {}

    // ------------------------------------------------------------ подложки (тактическое «стекло»)
    /** Подложка плашки: почти чёрное стекло (альфа умножается на настройку «Фон HUD-плашек»). */
    public static final int PLATE_BG = 0xB0050709;
    /** Верхний 1px блик плашки (как у UIDraw.glass). */
    public static final int PLATE_HIGHLIGHT = 0x14FFFFFF;
    /** Тонкая рамка плашки. */
    public static final int PLATE_BORDER = 0x26FFFFFF;
    /** Акцентная полоска слева у плашки. */
    public static final int PLATE_STRIPE = 2;

    public static final int PAD_X = Spacing.XS;
    public static final int PAD_Y = Spacing.XXS;

    // ------------------------------------------------------------ текст
    public static final int LABEL = PWPTheme.Text.MUTED;
    public static final int VALUE = PWPTheme.Text.HEADING;
    public static final int VALUE_DANGER = PWPTheme.Text.DANGER;
    public static final int ACCENT = Colors.ACCENT;

    // ------------------------------------------------------------ полосы
    public static final int BAR_TRACK = 0x99101317;
    public static final int BAR_TICK = 0xCC050709;
    public static final int BAR_BORDER = 0x33FFFFFF;
    /** Хвост «отложенного» урона — светлый, поверх трека. */
    public static final int BAR_TRAIL = 0xCCE8D9C0;

    public static final int HP_FILL = Colors.SUCCESS_LIGHT;
    public static final int HP_LOW = Colors.DANGER;
    /** Доля HP, ниже которой полоса красная. */
    public static final float HP_LOW_FRACTION = 0.3F;

    public static final int ARMOR_FILL = 0xFF8FA3B8;
    public static final int COOLDOWN_FILL = Colors.ACCENT;

    // ------------------------------------------------------------ хотбар
    public static final int SLOT = 20;
    public static final int SLOT_GAP = Spacing.XXS;
    public static final int SLOTS = 9;
    /** Подъём выбранного слота. */
    public static final int SLOT_LIFT = 3;
    /** Отступ иконки предмета (16px) внутри слота. */
    public static final int SLOT_ITEM_INSET = (SLOT - 16) / 2;

    public static final int SLOT_BG = 0x9905070A;
    public static final int SLOT_BG_SELECTED = 0xCC14100A;
    public static final int SLOT_BORDER = 0x22FFFFFF;
    public static final int SLOT_BORDER_SELECTED = 0x99C8812A;
    /** Мягкая подсветка выбранного слота (градиент от акцентной полосы вниз). */
    public static final int SLOT_GLOW = 0x40C8812A;

    public static final int SLOT_NUMBER = 0xB07A7D84;
    public static final int SLOT_NUMBER_SELECTED = 0xFFC8812A;
    public static final int SLOT_COUNT = PWPTheme.Text.HEADING;
    /** Номер слота рисуется вполовину (мелко, как в Squad). */
    public static final float SLOT_NUMBER_SCALE = 0.5F;

    // ------------------------------------------------------------ HP / броня (низ-лево)
    public static final int VITALS_H = 16;
    public static final int VITALS_HP_BAR_H = 5;
    public static final int VITALS_ARMOR_BAR_H = 3;
    public static final int VITALS_BAR_GAP = 2;
    public static final int VITALS_SEGMENTS = 10;
    /** Кулдаун прыжка: линия по низу плашки. */
    public static final int VITALS_COOLDOWN_H = 1;

    // ------------------------------------------------------------ плашки-значения и контекстный ярус
    public static final int ICON = 8;
    public static final int ICON_GAP = Spacing.XXS;
    public static final int VALUE_PLATE_BAR_H = 1;
    public static final int CONTEXT_W = 170;
    public static final int CONTEXT_H = 20;
    public static final int CONTEXT_BAR_H = 3;
}
