package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Токены игрового UI pwp-warfare. Это РАСШИРЕНИЕ {@link PWPTheme} (базовые цвета/отступы — оттуда):
 * здесь только то, что специфично для экранов матча (деплой, тактическая карта, радиалки,
 * редакторы). Сам PWPTheme не трогаем — его параллельно меняет владелец pwp-core-client.
 */
@OnlyIn(Dist.CLIENT)
public final class WTheme {
    private WTheme() {}

    // ------------------------------------------------------------ текстуры
    public static final ResourceLocation ICONS = new ResourceLocation("pwpwarfare", "textures/gui/ui/icons.png");
    public static final int ICONS_W = 128;
    public static final int ICONS_H = 64;
    public static final int ICON_CELL = 16;

    // ------------------------------------------------------------ общие размеры
    /** Внешнее поле экрана (доля ширины, с пределами). */
    public static final float MARGIN_FRACTION = 0.012F;

    public static final int MARGIN_MIN = Spacing.XS;
    public static final int MARGIN_MAX = Spacing.LG;
    /** Зазор между колонками/панелями. */
    public static final int GAP = Spacing.XS;
    /** Строка списка (игрок, правило), высота поля ввода, малая кнопка. */
    public static final int ROW_H = 12;

    public static final int FIELD_H = 14;
    public static final int SMALL_BUTTON_H = 14;
    /** Иконка в строке/ячейке (кратно 16 — чётко только 16; 8/12 допустимы для мелких меток). */
    public static final int ICON_SMALL = 8;

    public static final int ICON_ROW = 12;
    /** Вкладки верхней полосы: поле слева/справа от подписи и толщина подчёркивания. */
    public static final int TAB_PAD = Spacing.MD;

    public static final int TAB_UNDERLINE = 2;
    /** Тонкая полоса прогресса (таймер деплоя, припасы). */
    public static final int BAR_H = 2;
    /** Ширина полосы прокрутки. */
    public static final int SCROLLBAR_W = 2;
    /** Шаг прокрутки колесом, px. */
    public static final int SCROLL_STEP = 18;
    /** Пустое состояние: максимум строк пояснения. */
    public static final int EMPTY_HINT_LINES = 3;

    // ------------------------------------------------------------ деплой
    public static final float DEPLOY_SQUADS_WEIGHT = 22;
    public static final float DEPLOY_CENTER_WEIGHT = 34;
    public static final float DEPLOY_RIGHT_WEIGHT = 44;
    /** Доля центральной колонки под сетку ролей (остальное — точки спавна). */
    public static final float DEPLOY_ROLES_FRACTION = 0.56F;
    /** Нижняя часть колонки отрядов: строка «создать отряд» + чат. */
    public static final int DEPLOY_CHAT_MIN_H = 48;

    public static final float DEPLOY_CHAT_FRACTION = 0.34F;
    public static final int ROLE_CELL_MIN = 30;
    public static final int ROLE_CELL_MAX = 46;
    public static final int ROLE_COLS_MAX = 6;
    public static final int SPAWN_CARD_H = 30;
    public static final int SPAWN_CARD_H_LARGE = 36;
    /** Строки снаряжения: подпись слота, плашка предмета, альтернатива. */
    public static final int SLOT_LABEL_H = 10;

    public static final int SLOT_BAR_H = 20;
    public static final int SLOT_ALT_H = 16;
    public static final int SLOT_GAP = 3;
    public static final int ITEM_SIZE = 16;
    /** Кнопка деплоя: ширина (доля и пределы). */
    public static final float DEPLOY_BUTTON_FRACTION = 0.2F;

    public static final int DEPLOY_BUTTON_MIN_W = 96;
    public static final int DEPLOY_BUTTON_MAX_W = 180;
    /** Флаг фракции 64x36 в шапке. */
    public static final int FLAG_W = 24;

    public static final int FLAG_H = 14;
    /** Портрет: масштаб модели игрока (доля меньшей стороны и пределы). */
    public static final int PORTRAIT_SCALE_MIN = 40;

    public static final int PORTRAIT_SCALE_MAX = 120;

    // ------------------------------------------------------------ цвета
    public static final int STATUS_OK = Colors.SUCCESS_LIGHT;
    public static final int STATUS_WARN = Colors.WARNING;
    public static final int STATUS_BAD = PWPTheme.Text.DANGER;
    public static final int TEAM_BLUE = Colors.TEAM_BLUE;
    public static final int TEAM_RED = Colors.TEAM_RED;
    /** Подложка «ячейки-слота» (иконка в квадрате, поле ввода). */
    public static final int WELL = 0x80050709;

    public static final int WELL_BORDER = 0x26FFFFFF;
    /** Затемнение недоступного элемента поверх него. */
    public static final int DISABLED_VEIL = 0x66000000;
    /** Подложка «пилюли» статуса (цвет статуса с этой альфой). */
    public static final int PILL_ALPHA = 0x33;
    /** Ряд-«зебра» в списках. */
    public static final int ROW_STRIPE = 0x0AFFFFFF;

    public static final int ROW_HOVER = 0x14FFFFFF;
    public static final int SCROLL_TRACK = 0x1AFFFFFF;
    public static final int SCROLL_THUMB = Colors.ACCENT_DIM;
    /** Цвет «чужой» (вражеской) строки игрока. */
    public static final int ENEMY_TEXT = 0xFFD2A0A0;

    public static final int ALLY_TEXT = Styles.Control.TEXT;
    /** Полоса таймера деплоя. */
    public static final int TIMER_TRACK = Styles.Loader.TRACK;

    public static final int TIMER_FILL = Colors.ACCENT;

    // ------------------------------------------------------------ радиалки
    /** Внешний радиус: доля меньшей стороны экрана с пределами. */
    public static final float RADIAL_OUTER_FRACTION = 0.3F;

    public static final int RADIAL_OUTER_MIN = 64;
    public static final int RADIAL_OUTER_MAX = 150;
    /** Внутренний радиус = внешний × доля. */
    public static final float RADIAL_INNER_RATIO = 0.38F;
    /** Мёртвая зона в центре (px), как у прежних радиалок. */
    public static final int RADIAL_DEADZONE = 10;
    /** Зазор между секторами (градусы), рост наведённого сектора, толщина акцентной кромки. */
    public static final double RADIAL_GAP_DEG = 1.5;

    public static final int RADIAL_GROW = 4;
    public static final int RADIAL_EDGE = 2;
    public static final int RADIAL_CENTER_INSET = 3;
    /** Ширина кольца, с которой иконки пунктов рисуются 24px. */
    public static final int RADIAL_BIG_ICON_MIN = 52;

    public static final int RADIAL_SCRIM_TOP = 0x5005070A;
    public static final int RADIAL_SCRIM_BOTTOM = 0x8005070A;
    public static final int RADIAL_RING_SHADOW = 0x66000000;
    /** Цвета категорий тактических маркеров/приказов. */
    public static final int MARKER_TEAM = Colors.SUCCESS_LIGHT;

    public static final int MARKER_ENEMY = PWPTheme.Text.DANGER;
    public static final int MARKER_SQUAD = 0xFFE8D25A;
    public static final int ORDER_MOVE = Colors.SUCCESS_LIGHT;
    public static final int ORDER_ATTACK = Colors.ACCENT;
    public static final int ORDER_DEFEND = 0xFF6F9BD8;
    public static final int ORDER_BUILD = 0xFFD86F9B;

    // ------------------------------------------------------------ тактическая карта
    public static final int MAP_BG = 0xFF101411;
    public static final int MAP_BG_NOISE = 0x40FFFFFF;
    /** Сетка: квадраты 300 (сплошные) и 100 (пунктир, появляется при приближении). */
    public static final int MAP_GRID_MAJOR = 0x38FFFFFF;

    public static final int MAP_GRID_MINOR = 0x1CFFFFFF;
    public static final double MAP_SUBGRID_MAX_BPP = 2.2;
    public static final double MAP_SUBGRID_FADE = 0.4;
    public static final int MAP_DASH = 2;
    /** Вне играбельной зоны: маска, цвет у самого края, ширина градиентной полосы. */
    public static final int MAP_OUT_MASK = 0xC8050608;

    public static final int MAP_OUT_EDGE = 0x5A050608;
    public static final int MAP_EDGE_BAND = 14;
    /** Линия границы зоны: свечение, линия, бегущий пунктир (длина штриха и период шага). */
    public static final int MAP_BORDER_GLOW = 0x55C8812A;

    public static final int MAP_BORDER_LINE = 0xCCC8812A;
    public static final int MAP_BORDER_DASH_COLOR = 0xFFF0C070;
    public static final int MAP_BORDER_DASH = 6;
    public static final long MAP_BORDER_DASH_MS = 120L;
    /** Подписи сетки и плашки HUD карты. */
    public static final int MAP_LABEL_BG = 0xB0060809;

    public static final int MAP_LABEL_BORDER = 0x26FFFFFF;
    public static final int MAP_LABEL_TEXT = 0xFFE2E4E6;
    /** Место под колонку подписей строк слева (плашка координат не наезжает). */
    public static final int MAP_LABEL_COL_W = 18;
    /** Шкала масштаба: целевой диапазон длины, px. */
    public static final int MAP_SCALE_MIN_PX = 24;

    public static final int MAP_SCALE_MAX_PX = 110;
    /** Внутренняя тень по верхнему/нижнему краю карты. */
    public static final int MAP_INNER_SHADE = 18;

    public static final int MAP_INNER_SHADE_COLOR = 0x50000000;
    /** Экран карты (M): ширины боковых панелей (доля и пределы). */
    public static final float MAP_LEFT_FRACTION = 0.2F;

    public static final float MAP_RIGHT_FRACTION = 0.17F;
    public static final int MAP_SIDE_MIN_W = 96;
    public static final int MAP_LEFT_MAX_W = 220;
    public static final int MAP_RIGHT_MAX_W = 190;
    /** Сетка тактических маркеров. */
    public static final float MARKER_PANEL_FRACTION = 0.6F;

    public static final int MARKER_PANEL_MIN_W = 200;
    public static final int MARKER_PANEL_MAX_W = 460;
    public static final int MARKER_CARD_MIN_W = 72;
    public static final int MARKER_CARD_H = 56;
    public static final int MARKER_COLS_MAX = 5;

    // ------------------------------------------------------------ сетки карточек
    public static final int CARD_MIN_W = 92;
    public static final int CARD_MAX_W = 130;
    public static final int CARD_H = 64;
    public static final int CARD_COLS_MAX = 5;
    public static final int CARD_PANEL_MIN_W = 200;
    public static final int CARD_PANEL_MAX_W = 660;
    /** Высота области под заглушку «пусто/загрузка». */
    public static final int CARD_EMPTY_H = 80;
    /** Кнопка-действие в углу карточки. */
    public static final int CARD_ACTION = 12;

    // ------------------------------------------------------------ отряды
    public static final int SQUAD_ROW_H = 14;
    /** Цвета звеньев (Браво/Чарли) в списке отряда. */
    public static final int FT_BRAVO_LEAD = Colors.INFO;

    public static final int FT_BRAVO = 0xFFAAD4AA;
    public static final int FT_CHARLIE_LEAD = Colors.TEXT_ACCENT;
    public static final int FT_CHARLIE = 0xFF88BBFF;
    /** Тултип легенды техники: максимум строк таймеров. */
    public static final int LEGEND_TIMER_LINES = 8;
    /** Объекты карты: кольцо захвата, линия порядка точек, материалы FOB. */
    public static final int MAP_OBJECTIVE_R = 11;

    public static final int MAP_RING_W = 2;
    public static final int MAP_RING_TRACK = 0x66000000;
    public static final int MAP_RING_NEUTRAL = 0xFFB0B4B8;
    public static final int MAP_LATTICE = 0x80E2E4E6;
    public static final int MAP_MATS_TEXT = 0xFFFFAA00;
    /** Контекстное меню карты (маркеры/пути). */
    public static final int CTX_BG = 0xE00A0C10;

    public static final int CTX_CMD = 0xFFFFCC00;
    /** Слот предпросмотра кита (16 + рамка). */
    public static final int PREVIEW_SLOT = 18;
    /** Редкость скинов (как в прежнем списке скинов). */
    public static final int RARITY_COMMON = 0xFF6A6D73;

    public static final int RARITY_UNCOMMON = 0xFF3D6FA5;
    public static final int RARITY_RARE = 0xFF7A4A8A;
    public static final int RARITY_EPIC = 0xFFC8812A;
    public static final int RARITY_LEGENDARY = 0xFFA53D3D;
    public static final int RARITY_MYTHIC = 0xFFFFD700;

    // ------------------------------------------------------------ контейнерные экраны (редактор кита, спавнер)
    /**
     * Размеры панелей контейнеров. Слоты ставит меню в координатах относительно угла панели —
     * менять размер можно, но геометрия меню (KitEditorMenu/VehicleSpawnerMenu) должна влезать.
     */
    public static final int KIT_EDITOR_W = 256;

    public static final int KIT_EDITOR_H = 286;
    public static final int SPAWNER_W = 196;
    public static final int SPAWNER_H = 245;
    /** Шапка контейнерного экрана (над первыми полями). */
    public static final int CONTAINER_HEADER_H = 16;
    /** Подложка слота: 16 + рамка 1px. */
    public static final int SLOT_WELL = 18;
    /** Метка категории над слотом кита. */
    public static final int SLOT_TAG_H = 8;

    public static final int SLOT_TAG_BG = 0xC0060809;
    /** Цвета категорий слотов кита: основное, вторичка, граната, спец, рюкзак. */
    public static final int KIT_TAG_PRIMARY = Colors.ACCENT;

    public static final int KIT_TAG_SECONDARY = Colors.INFO;
    public static final int KIT_TAG_THROWABLE = Colors.WARNING;
    public static final int KIT_TAG_SPECIAL = 0xFFFF8800;
    public static final int KIT_TAG_BACKPACK = PWPTheme.Text.MUTED;
    /** Пометки слота кита поверх предмета: пополнение (СКМ) и сохранение NBT (Shift+СКМ). */
    public static final int KIT_RESUPPLY_VEIL = 0x6000FF40;

    public static final int KIT_SAVE_NBT_VEIL = 0x6000C8FF;
    public static final int KIT_LEGEND_SWATCH = 6;
    /** Узкое числовое поле (лимиты кита). */
    public static final int NUMBER_FIELD_W = 24;
    /** Кнопки «−/+» и поворота у числового поля спавнера. */
    public static final int STEP_BUTTON_W = 14;
}
