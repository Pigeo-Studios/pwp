package com.pigeostudios.pwp.warfare.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.WarfareOverlay;
import com.pigeostudios.pwp.warfare.client.WarfareUiSettings;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.hud.CaptureBar;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.ui.UIDamp;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.hud.CompassMath;
import com.pwp.coreclient.ui.hud.Hud;
import com.pwp.coreclient.ui.hud.HudDraw;
import com.pwp.coreclient.ui.hud.HudTheme;
import com.pwp.coreclient.ui.hud.HudZones;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Верх экрана матча (этап 3 редизайна HUD, тактический стиль Squad/HLL).
 *
 * <ul>
 *   <li>Полоса {@link HudZones#topBand()}: лента компаса (деления 5°/15°, стороны света, градусы,
 *       фейд краёв, треугольник и плашка азимута в центре, метки {@link CompassMarks} в нижнем ряду
 *       ленты); под ней — полоса тикетов: BLUE (флаг + число) | таймер матча | RED. Число тикетов
 *       меняется плавно, при «кровотечении» мигает красным.
 *   <li>Стек {@link HudZones#top()} сверху вниз: таймер подготовки Invasion → виджет точки захвата
 *       (имя, состояние с иконкой, полоса «перетягивания» в цветах команд с маркером и шевронами
 *       влияния) → баннер «захвачено/нейтрализовано». Не влезло — не рисуется (без наложений).
 * </ul>
 *
 * <p>Данные — {@link Model} (в матче наполняет {@link #fromClient}, в превью — фикстура), сам
 * рендерер пакетов не шлёт и ClientData не читает. Без аллокаций в кадре: строки — из кэшей
 * ({@link CompassMath}, {@link WHud.Num}/{@link WHud.Clock}/{@link WHud.Fmt}), длинные имена режутся
 * один раз при смене. Цвета — {@link WTheme}/{@link HudTheme}.
 */
@OnlyIn(Dist.CLIENT)
public final class TopHud {
    private static final String HUD = "gui.pwpwarfare.hud.";
    /** Размер текстур флагов фракций (textures/gui/flags/*.png). */
    private static final int FLAG_TEX_W = 64;

    private static final int FLAG_TEX_H = 36;
    private static final String[] CARDINAL_KEYS = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};

    private static final Component INV_SETUP = Component.translatable(HUD + "invasion.setup_label");
    private static final Component INV_BLOCKADE = Component.translatable(HUD + "invasion.blockade_label");
    private static final Component ST_LOCKED = Component.translatable(HUD + "point.blocked");
    private static final Component ST_CONTESTED = Component.translatable(HUD + "point.state.contested");
    private static final Component ST_CAPTURING = Component.translatable(HUD + "point.state.capturing");
    private static final Component ST_NEUTRALIZING = Component.translatable(HUD + "point.state.neutralizing");
    private static final Component ST_SECURING = Component.translatable(HUD + "point.state.securing");
    private static final Component ST_HELD = Component.translatable(HUD + "point.state.held");
    private static final Component ST_NEUTRAL = Component.translatable(HUD + "point.state.neutral");

    /** Данные верха на один кадр. Поля переиспользуются (без аллокаций). */
    public static final class Model {
        /** Компас включён (настройка «Компас»). */
        public boolean compass = true;
        /** Yaw Minecraft с partialTick. */
        public float yaw;

        public final CompassMarks marks = new CompassMarks(64);

        /** Полоса тикетов видна (видимость прежняя: creative/spectator — геймплей не меняем). */
        public boolean tickets;

        public int blueTickets;
        public int redTickets;
        public boolean blueBleeding;
        public boolean redBleeding;
        public String blueFaction = "none";
        public String redFaction = "none";
        /** Секунды с начала матча; &lt;0 — матч не идёт. */
        public int matchSeconds = -1;

        /** Таймер подготовки Invasion. */
        public boolean invasion;

        public boolean invasionDefender;
        public int invasionSeconds;

        /** Игрок на точке захвата. */
        public boolean point;

        public String pointName = "";
        public String owner = CaptureBar.NEUTRAL;
        public String capturingTeam = CaptureBar.NONE;
        public float progress;
        public int rate;
        public boolean contested;
        public boolean locked;
        public String need = "";

        /** Баннер «захвачено/нейтрализовано». */
        public boolean note;

        public String noteTeam = CaptureBar.BLUE;
        public String noteTeamName = "";
        public String notePoint = "";
        public boolean noteNeutralized;
        public float noteProgress;
        public float noteAlpha;
    }

    // ------------------------------------------------------------ кэши рендерера
    private final UIDamp.Frame frame = new UIDamp.Frame();
    private float blueShown = Float.NaN;
    private float redShown = Float.NaN;
    private final WHud.Num blueNum = new WHud.Num();
    private final WHud.Num redNum = new WHud.Num();
    private final WHud.Clock matchClock = new WHud.Clock();
    private final WHud.Clock invasionClock = new WHud.Clock();
    private final WHud.Fmt needFmt = new WHud.Fmt(HUD + "point.need");
    private final WHud.Fmt capturedFmt = new WHud.Fmt(HUD + "capture.captured");
    private final WHud.Fmt neutralizedFmt = new WHud.Fmt(HUD + "capture.neutralized");
    private final String[] cardinals = new String[CARDINAL_KEYS.length];
    private String cardinalLang;
    /** Последняя команда, влиявшая на точку (у нейтральной точки без влияния сервер её не шлёт). */
    private String lastSidePoint = "";

    private String lastSideTeam = CaptureBar.NONE;
    private final WHud.Fit pointNameFit = new WHud.Fit();
    private final WHud.Fit noteFit = new WHud.Fit();
    private Component noteSrc;
    private String noteUpper = "";

    // ================================================================ наполнение из ClientData

    /** Заполнить модель из ClientData для игрока {@code mc.player} (под гейтом HUD). */
    public static void fromClient(Model m, Minecraft mc, float partialTick) {
        Player p = mc.player;
        m.compass = WarfareUiSettings.COMPASS.get();
        m.yaw = p.getViewYRot(partialTick);
        if (m.compass) m.marks.collect(mc);
        else m.marks.clear();

        m.tickets = p.isCreative() || p.isSpectator();
        m.blueTickets = ClientData.BLUE_TICKETS;
        m.redTickets = ClientData.RED_TICKETS;
        m.blueBleeding = ClientData.blueBleeding;
        m.redBleeding = ClientData.redBleeding;
        m.blueFaction = ClientData.BLUE_FACTION;
        m.redFaction = ClientData.RED_FACTION;
        m.matchSeconds = ClientData.matchStartTime > 0L
                ? (int) ((System.currentTimeMillis() - ClientData.matchStartTime) / 1000L)
                : -1;

        m.invasion = ClientData.invasionSetupActive && ClientData.invasionSetupTimer > 0;
        if (m.invasion) {
            String team = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "";
            m.invasionDefender = ClientData.invasionDefender.equalsIgnoreCase(team);
            m.invasionSeconds = ClientData.invasionSetupTimer;
        }

        m.point = ClientData.isInsidePoint && insideAnyPoint(p);
        if (m.point) {
            m.pointName = ClientData.pointName;
            m.owner = ClientData.pointOwner;
            m.capturingTeam = ClientData.pointCapturingTeam;
            m.progress = ClientData.pointProgress;
            m.rate = ClientData.pointCaptureRate;
            m.contested = ClientData.isContested;
            m.locked = ClientData.isLocked;
            m.need = ClientData.nextObjectiveName;
        }

        // баннер: первое живое уведомление (как раньше), истёкшие удаляются
        m.note = false;
        long now = System.currentTimeMillis();
        for (ClientData.CaptureNotification n : ClientData.captureNotifications) {
            long elapsed = now - n.startTime;
            long hold = n.duration + WTheme.TOP_BANNER_HOLD_MS;
            if (elapsed > hold + WTheme.TOP_BANNER_FADE_MS) {
                ClientData.captureNotifications.remove(n); // CopyOnWriteArrayList — обход по снимку
                continue;
            }
            m.note = true;
            m.noteTeam = n.team;
            m.noteTeamName = "BLUE".equalsIgnoreCase(n.team) ? ClientData.customBlueName : ClientData.customRedName;
            m.notePoint = n.pointName;
            m.noteNeutralized = n.isNeutralized;
            m.noteProgress = n.duration <= 0L ? 1F : Math.min(1F, elapsed / (float) n.duration);
            m.noteAlpha = elapsed > hold ? 1F - (elapsed - hold) / (float) WTheme.TOP_BANNER_FADE_MS : 1F;
            break;
        }
    }

    /** Прежний гейт виджета точки: рамка игрока пересекает зону какой-либо точки. */
    private static boolean insideAnyPoint(Player p) {
        if (ClientData.allCapturePoints == null) return false;
        for (WarfareWorldData.CapturePoint cp : ClientData.allCapturePoints) {
            if (cp.area != null && p.getBoundingBox().intersects(cp.area)) return true;
        }
        return false;
    }

    // ================================================================ отрисовка

    /** Нарисовать верх: полоса компаса/тикетов и стек верх-центра (стеки уже сброшены за кадр). */
    public void render(GuiGraphics g, Font font, HudZones z, Model m) {
        long dt = frame.dtMs();
        Rect band = z.topBand();
        float s = z.scale();
        int bw = Math.round(band.w() / s);
        if (bw > 0 && band.h() > 0 && (m.compass || m.tickets)) {
            HudDraw.begin(g, band.x(), band.y(), s);
            int y = 0;
            if (m.compass) {
                compass(g, font, bw, m);
                y = WTheme.TOP_COMPASS_H + WTheme.TOP_ROW_GAP;
            }
            if (m.tickets) score(g, font, bw, y, m, dt);
            HudDraw.end(g);
        }
        if (m.invasion) invasion(g, font, z, m);
        if (m.point) point(g, font, z, m);
        if (m.note) banner(g, font, z, m);
    }

    // ---------------------------------------------------------------- компас

    private void compass(GuiGraphics g, Font font, int bw, Model m) {
        int cw = Math.min(WTheme.TOP_COMPASS_W, bw);
        int h = WTheme.TOP_COMPASS_H;
        int cx = bw / 2;
        int x0 = cx - cw / 2;
        float half = cw / 2F;
        float range = WTheme.TOP_COMPASS_RANGE;
        float halfRange = range / 2F;
        float heading = CompassMath.bearing(m.yaw);

        // подложка: полосы с фейдом к краям (мягкие края без текстуры)
        float bgO = Hud.plateOpacity();
        int slices = WTheme.TOP_COMPASS_SLICES;
        for (int i = 0; i < slices; i++) {
            int sx0 = x0 + cw * i / slices;
            int sx1 = x0 + cw * (i + 1) / slices;
            float a = CompassMath.edgeFade(((sx0 + sx1) / 2F - cx) / half);
            if (a <= 0F) continue;
            g.fill(sx0, 0, sx1, h, UIDraw.fade(WTheme.TOP_COMPASS_BG, a * bgO));
            g.fill(sx0, h - 1, sx1, h, UIDraw.fade(WTheme.TOP_COMPASS_LINE, a));
        }

        // деления 5° / 15°
        int first = CompassMath.firstTick(heading, halfRange, CompassMath.MINOR_STEP);
        for (int d = first; d <= heading + halfRange; d += CompassMath.MINOR_STEP) {
            float off = CompassMath.offset(d - heading, cw, range);
            float a = CompassMath.edgeFade(off / half);
            if (a <= 0F) continue;
            int x = Math.round(cx + off);
            if (CompassMath.tickKind(d) == 2) {
                g.fill(x, 0, x + 1, WTheme.TOP_TICK_MAJOR_H, UIDraw.fade(WTheme.TOP_TICK, a));
            } else {
                g.fill(x, 0, x + 1, WTheme.TOP_TICK_MINOR_H, UIDraw.fade(WTheme.TOP_TICK_MINOR, a));
            }
        }

        // подписи: стороны света в полный размер, градусы — мельче (один проход в своём масштабе)
        refreshCardinals();
        int chipHalf = WTheme.TOP_BEARING_W / 2 + 1;
        int major = CompassMath.firstTick(heading, halfRange, CompassMath.MAJOR_STEP);
        float ns = WTheme.TOP_NUMBER_SCALE;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0F, WTheme.TOP_LABEL_Y + 1, 0F);
        pose.scale(ns, ns, 1F);
        for (int d = major; d <= heading + halfRange; d += CompassMath.MAJOR_STEP) {
            if (CompassMath.cardinalIndex(d) >= 0) continue;
            float off = CompassMath.offset(d - heading, cw, range);
            if (Math.abs(off) < chipHalf) continue; // под плашкой азимута
            float a = CompassMath.edgeFade(off / half);
            String t = CompassMath.degreeText(d);
            int tx = Math.round((cx + off) / ns) - font.width(t) / 2;
            HudDraw.text(g, font, t, tx, 0, UIDraw.fade(WTheme.TOP_NUMBER, a), false);
        }
        pose.popPose();
        for (int d = major; d <= heading + halfRange; d += CompassMath.MAJOR_STEP) {
            int ci = CompassMath.cardinalIndex(d);
            if (ci < 0) continue;
            float off = CompassMath.offset(d - heading, cw, range);
            if (Math.abs(off) < chipHalf) continue;
            float a = CompassMath.edgeFade(off / half);
            String t = cardinals[ci];
            HudDraw.text(
                    g,
                    font,
                    t,
                    Math.round(cx + off) - font.width(t) / 2,
                    WTheme.TOP_LABEL_Y,
                    UIDraw.fade(WTheme.TOP_CARDINAL, a),
                    true);
        }

        // метки (нижний ряд ленты)
        long now = CompassMarks.nowMs();
        CompassMarks marks = m.marks;
        for (int i = 0; i < marks.count(); i++) {
            float off = CompassMath.offset(CompassMath.delta(marks.bearing(i), heading), cw, range);
            float a = CompassMath.edgeFade(off / half) * marks.alpha(i, now);
            if (a < 0.05F) continue;
            int sz = marks.size(i);
            int mx = Math.round(cx + off) - sz / 2;
            int my = WTheme.TOP_MARK_Y + (WTheme.TOP_MARK - sz) / 2;
            if (marks.texture(i) != null) {
                texture(g, marks.texture(i), mx, my, sz, a);
            } else {
                WHud.icon(g, marks.icon(i), mx, my, sz, UIDraw.fade(marks.color(i), a));
            }
        }

        // центр: треугольник сверху и плашка азимута
        int tri = WTheme.TOP_CENTER_TRI;
        for (int r = 0; r < tri; r++) {
            int hw = tri - 1 - r;
            g.fill(cx - hw, r, cx + hw + 1, r + 1, WTheme.TOP_CENTER);
        }
        int chipX = cx - WTheme.TOP_BEARING_W / 2;
        HudDraw.plateBg(g, chipX, WTheme.TOP_BEARING_Y, WTheme.TOP_BEARING_W, WTheme.TOP_BEARING_H, 1F);
        String b = CompassMath.bearingText(heading);
        HudDraw.text(
                g,
                font,
                b,
                cx - font.width(b) / 2 + 1,
                WTheme.TOP_BEARING_Y + (WTheme.TOP_BEARING_H - font.lineHeight) / 2 + 1,
                HudTheme.VALUE,
                false);
    }

    private void refreshCardinals() {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        if (lang.equals(cardinalLang) && cardinals[0] != null) return;
        cardinalLang = lang;
        for (int i = 0; i < CARDINAL_KEYS.length; i++) {
            cardinals[i] =
                    Component.translatable(HUD + "compass." + CARDINAL_KEYS[i]).getString();
        }
    }

    // ---------------------------------------------------------------- тикеты + таймер

    private void score(GuiGraphics g, Font font, int bw, int y, Model m, long dt) {
        int tw = WTheme.TOP_TICKET_W;
        int gap = WTheme.TOP_SCORE_GAP;
        int total = 2 * tw + WTheme.TOP_TIMER_W + 2 * gap;
        int x = bw / 2 - total / 2;
        int h = WTheme.TOP_SCORE_H;
        blueShown = tween(blueShown, m.blueTickets, dt);
        redShown = tween(redShown, m.redTickets, dt);
        ticket(g, font, x, y, tw, h, false, blueNum.of(Math.round(blueShown)), m.blueBleeding, m.blueFaction);
        int tx = x + tw + gap;
        HudDraw.plateBg(g, tx, y, WTheme.TOP_TIMER_W, h, 1F);
        String clock = matchClock.of(Math.max(0, m.matchSeconds));
        int iw = WTheme.HUD_ICON + WTheme.HUD_ICON_GAP;
        int cx = tx + (WTheme.TOP_TIMER_W - iw - font.width(clock)) / 2;
        int ty = y + (h - font.lineHeight) / 2 + 1;
        int timerColor = m.matchSeconds < 0 ? HudTheme.LABEL : HudTheme.VALUE;
        WHud.icon(g, WarfareIcon.CLOCK, cx, y + (h - WTheme.HUD_ICON) / 2, WTheme.HUD_ICON, HudTheme.LABEL);
        HudDraw.text(g, font, clock, cx + iw, ty, timerColor, false);
        ticket(
                g,
                font,
                tx + WTheme.TOP_TIMER_W + gap,
                y,
                tw,
                h,
                true,
                redNum.of(Math.round(redShown)),
                m.redBleeding,
                m.redFaction);
    }

    private static float tween(float shown, int target, long dt) {
        if (Float.isNaN(shown) || Math.abs(shown - target) > WTheme.TOP_TICKET_SNAP) return target;
        float v = UIDamp.approach(shown, target, WTheme.TOP_TICKET_RATE, dt);
        return Math.abs(v - target) < 0.5F ? target : v;
    }

    /** Плашка команды: полоска цвета команды с внешней стороны, флаг и число тикетов. */
    private static void ticket(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int w,
            int h,
            boolean red,
            String num,
            boolean bleeding,
            String faction) {
        int team = red ? WTheme.HUD_TEAM_RED : WTheme.HUD_TEAM_BLUE;
        HudDraw.plateBg(g, x, y, w, h, 1F);
        int stripeX = red ? x + w - HudTheme.PLATE_STRIPE : x;
        g.fill(stripeX, y, stripeX + HudTheme.PLATE_STRIPE, y + h, team);
        int inner = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        ResourceLocation flag = WarfareOverlay.getFlagTexture(faction);
        int fy = y + (h - WTheme.TOP_FLAG_H) / 2;
        int fx = red ? x + w - inner - WTheme.TOP_FLAG_W : x + inner;
        if (flag != null) {
            RenderSystem.enableBlend();
            g.blit(
                    flag,
                    fx,
                    fy,
                    WTheme.TOP_FLAG_W,
                    WTheme.TOP_FLAG_H,
                    0F,
                    0F,
                    FLAG_TEX_W,
                    FLAG_TEX_H,
                    FLAG_TEX_W,
                    FLAG_TEX_H);
            RenderSystem.disableBlend();
        } else {
            g.fill(fx, fy, fx + WTheme.TOP_FLAG_W, fy + WTheme.TOP_FLAG_H, team);
        }
        int color = HudTheme.VALUE;
        if (bleeding) {
            long now = CompassMarks.nowMs();
            double ph = (now % WTheme.TOP_BLEED_PULSE_MS) / (double) WTheme.TOP_BLEED_PULSE_MS * Math.PI * 2.0;
            color = Colors.lerp(HudTheme.VALUE, HudTheme.VALUE_DANGER, 0.5F + 0.5F * (float) Math.sin(ph));
        }
        int nw = font.width(num);
        int nx = red ? x + inner : x + w - inner - nw;
        HudDraw.text(g, font, num, nx, y + (h - font.lineHeight) / 2 + 1, color, true);
    }

    // ---------------------------------------------------------------- стек верх-центра

    /** Ширина блока стека в базовых единицах (не шире стека). */
    private static int stackWidth(HudZones z, int base) {
        return Math.min(base, Math.round(z.top().area().w() / z.scale()));
    }

    /** Занять блок {@code w x h} (базовые единицы) по центру стека; false — не влез. */
    private static boolean begin(GuiGraphics g, HudZones z, int w, int h) {
        HudZones.Stack st = z.top();
        int y = st.next(z.u(h));
        if (y == HudZones.Stack.NONE || w <= 0) return false;
        float s = z.scale();
        HudDraw.begin(g, st.area().centerX() - w * s / 2F, y, s);
        return true;
    }

    private void invasion(GuiGraphics g, Font font, HudZones z, Model m) {
        int w = stackWidth(z, WTheme.TOP_PHASE_W);
        int h = WHud.plateHeight(font);
        if (!begin(g, z, w, h)) return;
        int accent = m.invasionDefender ? WTheme.HUD_POSITIVE : WTheme.HUD_DANGER;
        HudDraw.plate(g, 0, 0, w, h, accent, 1F);
        int tx = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int ty = HudTheme.PAD_Y + 1;
        WHud.icon(g, m.invasionDefender ? WarfareIcon.CLOCK : WarfareIcon.LOCK, tx, ty, WTheme.HUD_ICON, accent);
        tx += WTheme.HUD_ICON + WTheme.HUD_ICON_GAP;
        UIDraw.text(g, font, m.invasionDefender ? INV_SETUP : INV_BLOCKADE, tx, ty, HudTheme.VALUE, false);
        String t = invasionClock.of(m.invasionSeconds);
        HudDraw.text(g, font, t, w - HudTheme.PAD_X - font.width(t), ty, accent, true);
        HudDraw.end(g);
    }

    private void point(GuiGraphics g, Font font, HudZones z, Model m) {
        boolean needLine = m.locked && m.need != null && !m.need.isEmpty();
        int w = stackWidth(z, WTheme.TOP_POINT_W);
        int h = WTheme.TOP_POINT_H + (needLine ? font.lineHeight : 0);
        if (!begin(g, z, w, h)) return;

        if (!CaptureBar.NONE.equals(m.capturingTeam)) {
            lastSidePoint = m.pointName;
            lastSideTeam = m.capturingTeam;
        }
        String memory = m.pointName.equals(lastSidePoint) ? lastSideTeam : CaptureBar.NONE;
        String side = CaptureBar.side(m.owner, m.capturingTeam, memory);
        CaptureBar.State state = CaptureBar.state(m.owner, m.rate, m.contested, m.locked);
        int sideColor = CompassMarks.teamColor(side);
        int actorColor = CompassMarks.teamColor(m.capturingTeam);
        WarfareIcon icon;
        Component label;
        int stateColor;
        switch (state) {
            case LOCKED -> {
                icon = WarfareIcon.LOCK;
                label = ST_LOCKED;
                stateColor = HudTheme.VALUE_DANGER;
            }
            case CONTESTED -> {
                icon = WarfareIcon.TEAM;
                label = ST_CONTESTED;
                stateColor = HudTheme.ACCENT;
            }
            case CAPTURING -> {
                icon = WarfareIcon.TARGET;
                label = ST_CAPTURING;
                stateColor = actorColor;
            }
            case NEUTRALIZING -> {
                icon = WarfareIcon.TARGET;
                label = ST_NEUTRALIZING;
                stateColor = actorColor;
            }
            case SECURING -> {
                icon = WarfareIcon.SHIELD;
                label = ST_SECURING;
                stateColor = sideColor;
            }
            case HELD -> {
                icon = WarfareIcon.SHIELD;
                label = ST_HELD;
                stateColor = sideColor;
            }
            default -> {
                icon = WarfareIcon.SHIELD;
                label = ST_NEUTRAL;
                stateColor = WTheme.HUD_TEAM_NEUTRAL;
            }
        }

        HudDraw.plate(g, 0, 0, w, h, sideColor, 1F);
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int ty = HudTheme.PAD_Y + 1;
        WHud.icon(g, icon, pad, ty, WTheme.HUD_ICON, stateColor);
        int nameX = pad + WTheme.HUD_ICON + WTheme.HUD_ICON_GAP;
        int lw = font.width(label);
        int lx = w - HudTheme.PAD_X - lw;
        UIDraw.text(g, font, label, lx, ty, stateColor, true);
        String name = pointNameFit.of(font, m.pointName, Math.max(0, lx - HudTheme.PAD_X - nameX));
        HudDraw.text(g, font, name, nameX, ty, HudTheme.VALUE, true);

        // полоса «перетягивания»: BLUE слева, RED справа, маркер от центра
        int bh = WTheme.TOP_POINT_BAR_H;
        int bx = pad;
        int bwid = w - pad - HudTheme.PAD_X;
        int by = ty + font.lineHeight + HudTheme.PAD_Y + 1;
        float o = m.locked ? 0.5F : 1F;
        int mid = bx + bwid / 2;
        g.fill(bx, by, bx + bwid, by + bh, UIDraw.fade(HudTheme.BAR_TRACK, o));
        g.fill(bx, by, mid, by + bh, UIDraw.fade(WTheme.HUD_TEAM_BLUE, WTheme.HUD_TEAM_TINT * o));
        g.fill(mid, by, bx + bwid, by + bh, UIDraw.fade(WTheme.HUD_TEAM_RED, WTheme.HUD_TEAM_TINT * o));
        float signed = CaptureBar.signed(side, m.progress);
        int mx = mid + Math.round(signed * (bwid / 2F));
        if (mx != mid) g.fill(Math.min(mid, mx), by, Math.max(mid, mx), by + bh, UIDraw.fade(sideColor, o));
        WHud.border(g, bx, by, bwid, bh, UIDraw.fade(HudTheme.BAR_BORDER, o));
        g.fill(mid, by - 1, mid + 1, by + bh + 1, UIDraw.fade(HudTheme.BAR_TICK, o)); // нейтраль
        g.fill(mx, by - 2, mx + 1, by + bh + 2, UIDraw.fade(HudTheme.VALUE, o)); // маркер

        // шевроны влияния — в сторону движения маркера
        int dir = CaptureBar.direction(m.capturingTeam, m.rate);
        int n = CaptureBar.strength(m.rate, WTheme.TOP_POINT_CHEVRONS);
        if (dir != 0 && !m.locked) {
            int cy = by + bh / 2;
            for (int i = 0; i < n; i++) {
                int px = mx + dir * (WTheme.TOP_POINT_CHEVRON_STEP * (i + 1));
                chevron(g, px, cy, dir, UIDraw.fade(HudTheme.VALUE, 0.85F - 0.2F * i));
            }
        }

        if (needLine) {
            needFmt.of(m.need);
            HudDraw.text(g, font, needFmt.string(), pad, by + bh + HudTheme.PAD_Y + 1, HudTheme.LABEL, false);
        }
        HudDraw.end(g);
    }

    /** Шеврон 3x5 острием в сторону {@code dir} (−1 влево, +1 вправо); (x, cy) — острие. */
    private static void chevron(GuiGraphics g, int x, int cy, int dir, int color) {
        if (Colors.alpha(color) == 0) return;
        for (int c = 0; c < 3; c++) {
            int col = x - dir * c;
            g.fill(col, cy - c, col + 1, cy + c + 1, color);
        }
    }

    private void banner(GuiGraphics g, Font font, HudZones z, Model m) {
        int w = stackWidth(z, WTheme.TOP_BANNER_W);
        int h = WTheme.TOP_BANNER_H;
        float o = Math.max(0F, Math.min(1F, m.noteAlpha));
        // место в стеке занимаем всегда (без «прыжка» соседей на фейде), рисуем при видимой альфе
        if (!begin(g, z, w, h)) return;
        int team = CompassMarks.teamColor(m.noteTeam);
        HudDraw.plate(g, 0, 0, w, h, team, o);
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int ty = (h - HudTheme.PAD_Y - WTheme.TOP_BANNER_BAR_H - font.lineHeight) / 2 + 1;
        WHud.icon(
                g,
                m.noteNeutralized ? WarfareIcon.MINUS : WarfareIcon.TARGET,
                pad,
                ty,
                WTheme.HUD_ICON,
                UIDraw.fade(team, o));
        int tx = pad + WTheme.HUD_ICON + WTheme.HUD_ICON_GAP;
        WHud.Fmt fmt = m.noteNeutralized ? neutralizedFmt : capturedFmt;
        Component c = fmt.of(m.noteTeamName, m.notePoint);
        if (c != noteSrc) {
            noteSrc = c;
            noteUpper = c.getString().toUpperCase();
        }
        String text = noteFit.of(font, noteUpper, Math.max(0, w - HudTheme.PAD_X - tx));
        HudDraw.text(g, font, text, tx, ty, UIDraw.fade(HudTheme.VALUE, o), true);
        int barY = h - HudTheme.PAD_Y - WTheme.TOP_BANNER_BAR_H;
        HudDraw.bar(g, pad, barY, w - pad - HudTheme.PAD_X, WTheme.TOP_BANNER_BAR_H, m.noteProgress, -1F, 1, team, o);
        HudDraw.end(g);
    }

    private static void texture(GuiGraphics g, ResourceLocation tex, int x, int y, int size, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, a);
        g.blit(tex, x, y, size, size, 0F, 0F, 1, 1, 1, 1);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }
}
