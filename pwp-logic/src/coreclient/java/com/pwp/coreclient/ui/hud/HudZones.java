package com.pwp.coreclient.ui.hud;

import com.pwp.coreclient.ui.layout.Rect;

/**
 * Сетка зон HUD (этап 1 редизайна, docs/ui-redesign/AUDIT_HUD.md): где на экране живёт каждый
 * элемент HUD, чтобы элементы разных модов не налезали друг на друга. Чистая логика без Minecraft.
 *
 * <p>Вход — размер GUI-экрана ({@code guiScaledWidth/Height}) и масштаб HUD (множитель базовых
 * размеров, 0.5..2; при 1 размеры = GUI-пиксели как в аудите). Раскладка считается ОДИН раз на
 * размер экрана ({@link Cache}), покадрово — только сброс стеков ({@link #beginFrame()}) без
 * аллокаций.
 *
 * <p>Зоны:
 *
 * <ul>
 *   <li>верх-центр — ярусы: полоса компаса/тикетов, таймер фазы, баннер события. Таймер и баннер
 *       сужаются до промежутка между боковыми колонками;
 *   <li>левая колонка — один стек {@link #left()} сверху вниз: сначала верхняя группа
 *       (кровотечение, голосование старта), затем {@link Stack#advanceTo} к якорю
 *       {@link #midLeftAnchor()} и средняя группа (радио, голос). Верхняя группа может заходить
 *       ниже якоря — тогда средняя просто начнётся под ней (перекрытий нет);
 *   <li>правая колонка — один стек {@link #right()} (точка захвата, голосование CMD, арта, ноки);
 *   <li>центр — {@link #crosshair()}: кольца у прицела (радиус ≤ половины стороны);
 *   <li>низ-центр — ярусы снизу вверх: хотбар, фидбек, лента уведомлений, контекстный ярус. В
 *       контекстном ярусе показывается ОДИН элемент — победитель {@link ContextTier} по приоритету.
 *       Текст канала дрона (в аудите — «текст под прицелом») живёт тоже здесь, с высшим приоритетом:
 *       при guiScale 4 на 1080p места между прицелом и лентой нет;
 *   <li>низ-лево / низ-право — стеки снизу вверх (HP + БК, припасы); низ выровнен по низу хотбара.
 * </ul>
 *
 * <p>Когда низ-центр не помещается под прицелом (низкий экран), раскладка ужимается по шагам: лента
 * в один слот → без зазоров между ярусами → меньший отступ снизу → контекстный ярус скрыт (высота
 * 0). Блок, который не влез в стек, стек не выдаёт ({@link Stack#NONE}) — лучше скрыть, чем
 * наложить.
 */
public final class HudZones {
    // ------------------------------------------------------------ базовые размеры (HUD-единицы)
    /** Отступ от края экрана (= PWPTheme.Spacing.SM). */
    public static final int EDGE = 8;
    /** Зазор между зонами и блоками стека (= PWPTheme.Spacing.XS). */
    public static final int GAP = 4;

    public static final int TOP_PAD = 4;
    public static final int TOP_BAND_W = 320;
    public static final int TOP_BAND_H = 40;
    public static final int PHASE_W = 200;
    public static final int PHASE_H = 12;
    public static final int BANNER_W = 260;
    public static final int BANNER_H = 46;

    public static final int SIDE_W = 165;
    /** Якорь средней группы левой колонки: столько выше центра экрана. */
    public static final int MID_LEFT_ABOVE_CENTER = 40;

    public static final int CROSSHAIR = 48;

    public static final int BOTTOM_W = 260;
    public static final int BOTTOM_PAD = 10;
    public static final int BOTTOM_PAD_COMPACT = 4;
    public static final int HOTBAR_H = 32;
    public static final int FEEDBACK_H = 16;
    public static final int FEED_SLOT_H = 17;
    public static final int FEED_SLOTS = 2;
    public static final int CONTEXT_H = 24;
    public static final int GAP_FEEDBACK_FEED = 2;
    public static final int GAP_FEED_CONTEXT = 6;

    public static final int BOTTOM_SIDE_W = 100;
    /** Высота низ-лево/низ-право: полоса HP+брони (16) + плашка БК (13) + зазоры. */
    public static final int BOTTOM_SIDE_H = 34;

    public static final float SCALE_MIN = 0.5F;
    public static final float SCALE_MAX = 2F;

    /** Шаг уменьшения масштаба, когда HUD не помещается даже ужатым. */
    public static final float SCALE_STEP = 0.05F;

    private final int width;
    private final int height;
    private final float scale;
    private final float requestedScale;
    private final boolean fits;

    private final Rect topBand;
    private final Rect phaseTimer;
    private final Rect eventBanner;
    private final Rect crosshair;
    private final Rect hotbar;
    private final Rect feedback;
    private final Rect feed;
    private final Rect context;
    private final int feedSlots;
    private final int compactLevel;
    private final Rect bottomLeft;
    private final Rect bottomRight;
    private final Rect leftColumn;
    private final Rect rightColumn;
    private final int midLeftAnchor;

    private final Stack left;
    private final Stack right;
    private final Stack bottomLeftStack;
    private final Stack bottomRightStack;
    private final ContextTier contextTier;

    private HudZones(int guiW, int guiH, float hudScale, float requestedScale) {
        this.width = Math.max(0, guiW);
        this.height = Math.max(0, guiH);
        this.scale = clampScale(hudScale);
        this.requestedScale = requestedScale;
        int edge = u(EDGE);
        int gap = u(GAP);
        int cx = width / 2;

        // ---------------------------------------------------- центр
        int cs = u(CROSSHAIR);
        crosshair = new Rect(cx - cs / 2, height / 2 - cs / 2, cs, cs);

        // ---------------------------------------------------- колонки (ширина)
        // колонки не заходят на зону прицела и друг на друга
        int sideW = Math.max(0, Math.min(u(SIDE_W), (width - 2 * edge - cs) / 2 - gap));
        int leftX = edge;
        int rightX = width - edge - sideW;

        // ---------------------------------------------------- верх-центр
        int bandW = Math.min(u(TOP_BAND_W), Math.max(0, width - 2 * edge));
        topBand = new Rect(cx - bandW / 2, u(TOP_PAD), bandW, u(TOP_BAND_H));
        int between = Math.max(0, rightX - gap - (leftX + sideW + gap));
        int phaseW = Math.min(u(PHASE_W), between);
        // таймер и баннер не опускаются на зону прицела (низкий экран / крупный масштаб)
        int phaseY = topBand.bottom() + gap;
        int phaseH = Math.max(0, Math.min(u(PHASE_H), crosshair.y() - gap - phaseY));
        phaseTimer = new Rect(cx - phaseW / 2, phaseY, phaseW, phaseH);
        int bannerW = Math.min(u(BANNER_W), between);
        int bannerY = phaseTimer.bottom() + gap;
        int bannerH = Math.max(0, Math.min(u(BANNER_H), crosshair.y() - gap - bannerY));
        eventBanner = new Rect(cx - bannerW / 2, bannerY, bannerW, bannerH);
        boolean sidesUnderBand = sideW > 0 && leftX + sideW > topBand.x();
        int sideTop = sidesUnderBand ? topBand.bottom() + gap : edge;

        // ---------------------------------------------------- низ-центр (с ужатием под прицел)
        int bottomW = Math.min(u(BOTTOM_W), Math.max(0, width - 2 * edge));
        int bx = cx - bottomW / 2;
        int level = 0;
        Rect[] tiers;
        while (true) {
            tiers = bottomTiers(bx, bottomW, level);
            Rect top = tiers[3].h() > 0 ? tiers[3] : tiers[2];
            if (top.y() >= crosshair.bottom() || level >= 4) break;
            level++;
        }
        hotbar = tiers[0];
        feedback = tiers[1];
        feed = tiers[2];
        context = tiers[3];
        compactLevel = level;
        fits = (context.h() > 0 ? context.y() : feed.y()) >= crosshair.bottom()
                && phaseTimer.h() > 0
                && eventBanner.h() > 0;
        feedSlots = level >= 1 ? 1 : FEED_SLOTS;
        int bottomCenterTop = context.h() > 0 ? context.y() : feed.y();

        // ---------------------------------------------------- низ-лево / низ-право
        int bsw = Math.max(0, Math.min(u(BOTTOM_SIDE_W), bx - gap - edge));
        int bsh = u(BOTTOM_SIDE_H);
        int bsy = hotbar.bottom() - bsh; // рядом с хотбаром, низ по одной линии
        bottomLeft = new Rect(edge, bsy, bsw, bsh);
        bottomRight = new Rect(width - edge - bsw, bsy, bsw, bsh);

        // ---------------------------------------------------- колонки (высота)
        int leftBottom = columnBottom(leftX, sideW, bottomCenterTop, bx, bottomW, bottomLeft, gap);
        int rightBottom = columnBottom(rightX, sideW, bottomCenterTop, bx, bottomW, bottomRight, gap);
        leftColumn = new Rect(leftX, sideTop, sideW, Math.max(0, leftBottom - sideTop));
        rightColumn = new Rect(rightX, sideTop, sideW, Math.max(0, rightBottom - sideTop));
        midLeftAnchor = Math.max(sideTop, height / 2 - u(MID_LEFT_ABOVE_CENTER));

        left = new Stack(leftColumn, false, gap);
        right = new Stack(rightColumn, false, gap);
        bottomLeftStack = new Stack(bottomLeft, true, u(1));
        bottomRightStack = new Stack(bottomRight, true, u(1));
        contextTier = new ContextTier(context, crosshair.bottom(), gap);
    }

    /**
     * Раскладка для экрана {@code guiW x guiH} (GUI-пиксели) и масштаба HUD. Если HUD не помещается
     * даже полностью ужатым (низ-центр налезает на прицел, верхним ярусам нет места) — масштаб
     * уменьшается шагами {@link #SCALE_STEP} до {@link #SCALE_MIN}; {@link #scale()} — итоговый.
     */
    public static HudZones layout(int guiW, int guiH, float hudScale) {
        float req = clampScale(hudScale);
        HudZones z = new HudZones(guiW, guiH, req, req);
        for (float s = req - SCALE_STEP; !z.fits && s >= SCALE_MIN - 1e-4F; s -= SCALE_STEP) {
            z = new HudZones(guiW, guiH, s, req);
        }
        return z;
    }

    public static float clampScale(float s) {
        if (Float.isNaN(s)) return 1F;
        return Math.max(SCALE_MIN, Math.min(SCALE_MAX, s));
    }

    /** Базовый размер в GUI-пикселях с учётом масштаба HUD. */
    public int u(int base) {
        return Math.round(base * scale);
    }

    /** Ярусы низ-центра: [хотбар, фидбек, лента, контекст]; {@code level} — шаг ужатия 0..4. */
    private Rect[] bottomTiers(int x, int w, int level) {
        int slots = level >= 1 ? 1 : FEED_SLOTS;
        boolean noGaps = level >= 2;
        int pad = level >= 3 ? u(BOTTOM_PAD_COMPACT) : u(BOTTOM_PAD);
        boolean noContext = level >= 4;
        int y = height - pad;
        Rect hb = new Rect(x, y - u(HOTBAR_H), w, u(HOTBAR_H));
        y = hb.y();
        Rect fb = new Rect(x, y - u(FEEDBACK_H), w, u(FEEDBACK_H));
        y = fb.y() - (noGaps ? 0 : u(GAP_FEEDBACK_FEED));
        int feedH = u(FEED_SLOT_H) * slots;
        Rect fd = new Rect(x, y - feedH, w, feedH);
        y = fd.y() - (noGaps ? 0 : u(GAP_FEED_CONTEXT));
        Rect ctx = noContext ? new Rect(x, y, w, 0) : new Rect(x, y - u(CONTEXT_H), w, u(CONTEXT_H));
        return new Rect[] {hb, fb, fd, ctx};
    }

    private static int columnBottom(int colX, int colW, int bottomCenterTop, int bx, int bw, Rect bottomSide, int gap) {
        int limit = bottomSide.y() - gap;
        boolean overBottomCenter = colW > 0 && colX < bx + bw && colX + colW > bx;
        if (overBottomCenter) limit = Math.min(limit, bottomCenterTop - gap);
        return limit;
    }

    /** Сброс стеков и контекстного яруса — раз в кадр перед отрисовкой HUD. Без аллокаций. */
    public void beginFrame() {
        left.reset();
        right.reset();
        bottomLeftStack.reset();
        bottomRightStack.reset();
        contextTier.reset();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Итоговый масштаб (может быть меньше запрошенного на низком экране). */
    public float scale() {
        return scale;
    }

    /** Масштаб, который просили (настройка). */
    public float requestedScale() {
        return requestedScale;
    }

    /** Всё поместилось без наложений (false — только на экранах ниже минимального GUI). */
    public boolean fits() {
        return fits;
    }

    /** Полоса компаса и тикетов (верх-центр). */
    public Rect topBand() {
        return topBand;
    }

    /** Таймер фазы (подготовка Invasion) под полосой компаса. */
    public Rect phaseTimer() {
        return phaseTimer;
    }

    /** Баннер события (уведомления о захвате). */
    public Rect eventBanner() {
        return eventBanner;
    }

    /** Квадрат вокруг прицела: кольца HP цели, кольцо дрона. */
    public Rect crosshair() {
        return crosshair;
    }

    public Rect hotbar() {
        return hotbar;
    }

    public Rect feedback() {
        return feedback;
    }

    public Rect feed() {
        return feed;
    }

    /** Сколько слотов ленты уведомлений помещается (2, на низком экране 1). */
    public int feedSlots() {
        return feedSlots;
    }

    /** Контекстный ярус низ-центра (высота 0 = скрыт: не помещается под прицелом). */
    public Rect context() {
        return context;
    }

    /** Шаг ужатия низ-центра: 0 — полная раскладка, 4 — контекстный ярус скрыт. */
    public int compactLevel() {
        return compactLevel;
    }

    public Rect bottomLeft() {
        return bottomLeft;
    }

    public Rect bottomRight() {
        return bottomRight;
    }

    public Rect leftColumn() {
        return leftColumn;
    }

    public Rect rightColumn() {
        return rightColumn;
    }

    /** Y начала средней группы левой колонки (радио/голос). */
    public int midLeftAnchor() {
        return midLeftAnchor;
    }

    public Stack left() {
        return left;
    }

    public Stack right() {
        return right;
    }

    /** Низ-лево снизу вверх: HP-бар, над ним БК. */
    public Stack bottomLeftStack() {
        return bottomLeftStack;
    }

    /** Низ-право снизу вверх: припасы. */
    public Stack bottomRightStack() {
        return bottomRightStack;
    }

    public ContextTier contextTier() {
        return contextTier;
    }

    // ================================================================ стек

    /**
     * Вертикальный стек блоков внутри области: каждый следующий блок ставится под (или над, для
     * стека снизу вверх) предыдущим с зазором. Блок, который не помещается, не выдаётся.
     */
    public static final class Stack {
        /** «Места нет» — результат {@link #next}. */
        public static final int NONE = Integer.MIN_VALUE;

        private final Rect area;
        private final boolean up;
        private final int gap;
        private int cursor;
        private boolean empty;

        public Stack(Rect area, boolean up, int gap) {
            this.area = area;
            this.up = up;
            this.gap = Math.max(0, gap);
            reset();
        }

        public Rect area() {
            return area;
        }

        public boolean up() {
            return up;
        }

        public void reset() {
            cursor = up ? area.bottom() : area.y();
            empty = true;
        }

        /** Влезет ли блок высотой {@code h}. */
        public boolean fits(int h) {
            if (h < 0 || area.w() <= 0) return false;
            int start = empty ? cursor : (up ? cursor - gap : cursor + gap);
            return up ? start - h >= area.y() : start + h <= area.bottom();
        }

        /** Занять блок высотой {@code h}: Y его верхнего края или {@link #NONE}. */
        public int next(int h) {
            if (!fits(h)) return NONE;
            int start = empty ? cursor : (up ? cursor - gap : cursor + gap);
            empty = false;
            if (up) {
                cursor = start - h;
                return cursor;
            }
            cursor = start + h;
            return start;
        }

        /**
         * Стек сверху вниз: следующий блок не выше {@code y} (якорь группы). Если стек уже ниже —
         * ничего не меняется.
         */
        public void advanceTo(int y) {
            if (up) return;
            int start = empty ? cursor : cursor + gap;
            if (y > start) {
                cursor = Math.min(y, area.bottom());
                empty = true; // зазор к якорю не нужен
            }
        }

        /** Свободное место по высоте для следующего блока. */
        public int remaining() {
            int start = empty ? cursor : (up ? cursor - gap : cursor + gap);
            return Math.max(0, up ? start - area.y() : area.bottom() - start);
        }
    }

    // ================================================================ контекстный ярус

    /**
     * Контекстный ярус низ-центра держит ОДИН элемент. Порядок кадра: {@link #reset()} (делает
     * {@link HudZones#beginFrame()}), затем каждый желающий элемент вызывает {@link #offer}, затем
     * рисует только тот, для кого {@link #shows} = true.
     */
    public static final class ContextTier {
        public static final int NONE = Integer.MIN_VALUE;
        /** Канал установки/перезарядки дрона. */
        public static final int DRONE_CHANNEL = 50;
        /** Прогресс стройки (лопата). */
        public static final int CONSTRUCTION = 40;
        /** Подсказки режима постройки (Build Mode). */
        public static final int PLACEMENT = 30;
        /** Look-at: материалы, статус техники. */
        public static final int LOOK_AT = 20;
        /** Пациент: «тебя лечат». */
        public static final int PATIENT = 10;

        private final Rect area;
        private final int ceiling;
        private final int gap;
        private int best = NONE;
        private int reservedTop = NONE;

        public ContextTier(Rect area) {
            this(area, area.y(), 0);
        }

        /**
         * @param ceiling выше этого Y блок не поднимается (низ зоны прицела)
         * @param gap зазор до занятой части яруса
         */
        public ContextTier(Rect area, int ceiling, int gap) {
            this.area = area;
            this.ceiling = ceiling;
            this.gap = Math.max(0, gap);
        }

        public Rect area() {
            return area;
        }

        /** Ярус помещается на экране (на очень низком экране — нет). */
        public boolean enabled() {
            return area.h() > 0 && area.w() > 0;
        }

        public void reset() {
            best = NONE;
            reservedTop = NONE;
        }

        /**
         * Старый элемент (look-at подсказки, Build Mode — пока рисуются по своим координатам) в
         * этом кадре уже занял ярус до Y {@code top}. Победитель яруса тогда ставится над ним
         * ({@link #place}).
         */
        public void reserve(int top) {
            reservedTop = reservedTop == NONE ? top : Math.min(reservedTop, top);
        }

        public boolean reserved() {
            return reservedTop != NONE;
        }

        /**
         * Y верхнего края блока высотой {@code h} для победителя: в ярусе (по нижней грани), если
         * ярус свободен; над занятой частью, если его занял старый элемент; {@link #NONE} — не
         * влезает под прицел (лучше скрыть, чем наложить).
         */
        public int place(int h) {
            if (!enabled() || h <= 0) return NONE;
            int bottom = reserved() ? Math.min(area.y(), reservedTop) - gap : area.bottom();
            int y = bottom - h;
            return y >= ceiling ? y : NONE;
        }

        public void offer(int priority) {
            if (priority > best) best = priority;
        }

        public int winner() {
            return enabled() ? best : NONE;
        }

        public boolean shows(int priority) {
            return enabled() && best == priority;
        }
    }

    // ================================================================ кэш

    /** Кэш раскладки: пересчёт только при смене размера экрана или масштаба. */
    public static final class Cache {
        private HudZones zones;

        public HudZones get(int guiW, int guiH, float hudScale) {
            float s = clampScale(hudScale);
            HudZones z = zones;
            if (z == null || z.width != guiW || z.height != guiH || z.requestedScale != s) {
                z = layout(guiW, guiH, s);
                zones = z;
            }
            return z;
        }
    }
}
