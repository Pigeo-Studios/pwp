package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.hud.HudTheme;
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

    // ------------------------------------------------------------ деплой «как в Squad» (с 17.11.2026)
    /** Колонки вкладки деплоя: отряды | карта + точки | роли + снаряжение. */
    public static final float DEPLOY_SQ_SQUADS_WEIGHT = 20;

    public static final float DEPLOY_SQ_MAP_WEIGHT = 44;
    public static final float DEPLOY_SQ_RIGHT_WEIGHT = 36;
    /** Доля колонки карты под список точек спавна (под картой). */
    public static final float DEPLOY_SQ_SPAWNS_FRACTION = 0.27F;
    /** Доля правой колонки под сетку ролей (остальное — снаряжение + кукла). */
    public static final float DEPLOY_SQ_ROLES_FRACTION = 0.48F;
    /** Крупная кнопка деплоя справа внизу. */
    public static final float DEPLOY_CTA_FRACTION = 0.24F;

    public static final int DEPLOY_CTA_MIN_W = 130;
    public static final int DEPLOY_CTA_MAX_W = 240;
    /** Точки спавна на карте деплоя: радиус кольца, выбранной, зона клика (px). */
    public static final int DEPLOY_SPAWN_RING_R = 8;

    public static final int DEPLOY_SPAWN_RING_SEL_R = 11;
    public static final int DEPLOY_SPAWN_HIT_R = 15;
    /** Период пульса кольца выбранной точки, мс. */
    public static final long DEPLOY_SPAWN_PULSE_MS = 1400;
    /** Кольцо точки: обычная / под курсором / выбранная / недоступная. */
    public static final int DEPLOY_SPAWN_RING = Colors.multiplyAlpha(PWPTheme.Text.HEADING, 0.55F);

    public static final int DEPLOY_SPAWN_RING_HOVER = PWPTheme.Text.HEADING;
    public static final int DEPLOY_SPAWN_RING_SELECTED = Colors.ACCENT;
    public static final int DEPLOY_SPAWN_RING_OFF = PWPTheme.Text.DANGER;
    /** Зум карты деплоя: минимум (блоков/px) и запас отдаления сверх «вся карта». */
    public static final double DEPLOY_MAP_MIN_BPP = 0.25;

    public static final double DEPLOY_MAP_FIT_SLACK = 1.5;
    /** Кукла: начальный поворот (градусы) и чувствительность перетаскивания (градусов на px). */
    public static final float PORTRAIT_DEFAULT_YAW = 25F;

    public static final float PORTRAIT_DRAG_DEG_PER_PX = 1.4F;
    /** Шаги потока деплоя внизу: кружок номера шага. */
    public static final int DEPLOY_STEP_DOT = 10;

    public static final int DEPLOY_STEP_DONE = Colors.SUCCESS_LIGHT;
    public static final int DEPLOY_STEP_TODO = Colors.WARNING;
    public static final int DEPLOY_STEP_IDLE = PWPTheme.Text.MUTED;

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
    /** Карта грузится (MapLayers.placeholder/loading): тёмно-зелёный топо-фон, шум, шиммер, полоса. */
    public static final int MAP_LOADING_BG = 0xFF17221B;

    public static final int MAP_LOADING_NOISE = 0x30A8C8A0;
    public static final int MAP_LOADING_SHIMMER = 0x18E8F4E0;
    public static final int MAP_LOADING_SHIMMER_MIN_W = 40;
    public static final long MAP_LOADING_SHIMMER_MS = 1800L;
    public static final int MAP_LOADING_BAR = 0xFFC8812A;
    public static final int MAP_LOADING_BAR_TRACK = 0x66000000;
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

    // ------------------------------------------------------------ скины кита по слотам
    public static final int SKIN_SELECT_MAX_W = 560;

    public static final int SKIN_SELECT_MAX_H = 360;
    /** Доля ширины под колонку слотов (остальное — категории и скины выбранного слота). */
    public static final float SKIN_SLOTS_FRACTION = 0.42F;

    public static final int SKIN_SLOT_ROW_H = 22;
    /** Минимальная ширина плитки слота, при которой слоты идут в две колонки. */
    public static final int SKIN_SLOT_MIN_W = 96;

    public static final int SKIN_CAT_H = 16;
    public static final int SKIN_ROW_H = 16;

    // ------------------------------------------------------------ HUD матча (кит WHud поверх HudTheme core-client)
    // Масштаб HUD — настройка CoreUiSettings.HUD_SCALE (Hud.zones()); общие токены — HudTheme.
    public static final int HUD_PLATE_BG = HudTheme.PLATE_BG;

    public static final int HUD_PLATE_BORDER = HudTheme.PLATE_BORDER;
    public static final int HUD_PLATE_STRIPE = HudTheme.PLATE_STRIPE;
    public static final int HUD_PLATE_PAD_X = HudTheme.PAD_X;
    public static final int HUD_PLATE_PAD_Y = HudTheme.PAD_Y;
    public static final int HUD_LABEL = HudTheme.LABEL;
    public static final int HUD_VALUE = HudTheme.VALUE;
    public static final int HUD_BAR_TRACK = HudTheme.BAR_TRACK;
    public static final int HUD_BAR_FILL = Colors.ACCENT;
    public static final int HUD_BAR_TICK = HudTheme.BAR_TICK;
    public static final int HUD_BAR_BORDER = HudTheme.BAR_BORDER;
    /** Обводка текста поверх мира (компас, подписи у прицела). */
    public static final int HUD_TEXT_OUTLINE = 0xFF000000;
    /** Иконка в счётчике/плашке HUD и зазор до текста. */
    public static final int HUD_ICON = HudTheme.ICON;

    public static final int HUD_ICON_GAP = HudTheme.ICON_GAP;
    /** Значение «опасно» (0 патронов, пустой грузовик, вражеская постройка). */
    public static final int HUD_DANGER = HudTheme.VALUE_DANGER;
    /** «Идёт загрузка/пополнение» (грузовик у снабжения). */
    public static final int HUD_POSITIVE = Colors.SUCCESS_LIGHT;
    /** Пациент: «вас лечат». */
    public static final int HUD_MEDIC = Colors.SUCCESS_LIGHT;

    // ------------------------------------------------------------ HUD верх (этап 3: client/hud/TopHud)
    // Размеры — базовые HUD-единицы внутри HudZones.topBand()/top() (масштаб HUD применяет HudDraw.begin).
    /** Цвета команд на HUD: светлее токенов меню — читаются поверх мира. */
    public static final int HUD_TEAM_BLUE = Colors.lerp(TEAM_BLUE, 0xFFFFFFFF, 0.22F);

    public static final int HUD_TEAM_RED = Colors.lerp(TEAM_RED, 0xFFFFFFFF, 0.18F);
    public static final int HUD_TEAM_NEUTRAL = HudTheme.LABEL;
    /** Подкраска половин полосы захвата (BLUE слева, RED справа). */
    public static final float HUD_TEAM_TINT = 0.28F;

    /** Лента компаса: ширина, высота, видимый сектор (градусы). */
    public static final int TOP_COMPASS_W = 200;

    public static final int TOP_COMPASS_H = 24;
    public static final float TOP_COMPASS_RANGE = 90F;
    /** Подложка ленты режется на столько вертикальных полос с фейдом к краям. */
    public static final int TOP_COMPASS_SLICES = 20;

    public static final int TOP_COMPASS_BG = 0x9A050709;
    public static final int TOP_COMPASS_LINE = 0x40FFFFFF;
    public static final int TOP_TICK = 0xE6E8E9EB;
    public static final int TOP_TICK_MINOR = 0x8CE8E9EB;
    public static final int TOP_TICK_MAJOR_H = 5;
    public static final int TOP_TICK_MINOR_H = 2;
    /** Подписи: Y строки, масштаб чисел градусов (стороны света — в полный размер). */
    public static final int TOP_LABEL_Y = 6;

    public static final float TOP_NUMBER_SCALE = 0.6667F;
    public static final int TOP_CARDINAL = Colors.ACCENT;
    public static final int TOP_NUMBER = 0xD0B8BCC2;
    /** Метки на ленте (точки, приказы, пинги, ноки): Y и размер иконки. */
    public static final int TOP_MARK_Y = 15;

    public static final int TOP_MARK = 9;
    public static final int TOP_MARK_SMALL = 7;
    /** Центральный маркер (треугольник сверху) и плашка азимута под ним. */
    public static final int TOP_CENTER = Colors.ACCENT;

    public static final int TOP_CENTER_TRI = 3;
    public static final int TOP_BEARING_W = 24;
    public static final int TOP_BEARING_H = 11;
    public static final int TOP_BEARING_Y = 3;
    /** Зазор между лентой компаса и полосой тикетов. */
    public static final int TOP_ROW_GAP = 2;

    /** Полоса тикетов: плашка команды (флаг + число), таймер матча между ними. */
    public static final int TOP_TICKET_W = 46;

    public static final int TOP_TIMER_W = 40;
    public static final int TOP_SCORE_H = 13;
    public static final int TOP_SCORE_GAP = 2;
    public static final int TOP_FLAG_W = 12;
    public static final int TOP_FLAG_H = 7;
    /** Плавное изменение числа тикетов (доля пути за кадр при 60 FPS) и скачок без анимации. */
    public static final float TOP_TICKET_RATE = 0.12F;

    public static final int TOP_TICKET_SNAP = 300;
    /** Мигание числа при «кровотечении» тикетов. */
    public static final long TOP_BLEED_PULSE_MS = 900L;

    /** Стек верх-центра: таймер Invasion, точка захвата, баннер события. */
    public static final int TOP_PHASE_W = 150;

    public static final int TOP_POINT_W = 180;
    public static final int TOP_POINT_H = 24;
    public static final int TOP_POINT_BAR_H = 4;
    public static final int TOP_POINT_CHEVRONS = 3;
    public static final int TOP_POINT_CHEVRON_STEP = 4;
    public static final int TOP_BANNER_W = 210;
    public static final int TOP_BANNER_H = 22;
    public static final int TOP_BANNER_BAR_H = 2;
    /** Баннер: держится duration + HOLD, затем гаснет за FADE. */
    public static final long TOP_BANNER_HOLD_MS = 2000L;

    public static final long TOP_BANNER_FADE_MS = 1000L;
    /** Мигание метки нока на компасе. */
    public static final long TOP_BLINK_MS = 1200L;
}
