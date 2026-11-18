package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.client.WarfareOverlay;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.ui.UIDamp;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.hud.HudDraw;
import com.pwp.coreclient.ui.hud.HudTheme;
import com.pwp.coreclient.ui.hud.HudZones;
import com.pwp.coreclient.ui.hud.SideList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Боковые стеки HUD матча (этап 4 редизайна HUD): все панели — стеклянная плашка с шапкой (иконка
 * {@link WarfareIcon} + заголовок + значение справа), полосой прогресса и подсказкой клавиш из
 * {@link net.minecraft.client.KeyMapping}.
 *
 * <ul>
 *   <li>Левый стек {@link HudZones#left()} сверху вниз: [баннер кровотечения pwp-medicine — он
 *       занимает место раньше всех, в {@code RenderGuiEvent.Pre}, см. {@link WHud#leftClaim}] →
 *       голосование за старт; затем якорь {@link HudZones#midLeftAnchor()} и голос: говорящие по
 *       радио, затем в отряде.
 *   <li>Правый стек {@link HudZones#right()} по приоритету: голосование за командира → запрос арты
 *       → список раненых союзников у медика. Открыта боковая карта — правый стек ужимается до места
 *       над ней ({@link HudZones#rightBeside}), а то, что туда не влезло, переносится в левый стек
 *       (перед голосом): голосования/арта остаются видны.
 * </ul>
 *
 * <p>Не помещается — не рисуется (длинный список режется до «+N», см. {@link SideList}). Данные —
 * {@link Model} (в матче наполняет {@link #fromClient}, в превью — фикстура); рендерер пакетов не
 * шлёт и ClientData не читает. Без аллокаций в кадре: строки — из кэшей ({@link WHud.Fmt}/{@link
 * WHud.Clock}/{@link WHud.KeyHint}/{@link WHud.Fit}), строки списков — переиспользуемые {@link Row}.
 */
@OnlyIn(Dist.CLIENT)
public final class SideHud {
    private static final String HUD = "gui.pwpwarfare.hud.";

    private static final ResourceLocation VOICE_ICON =
            new ResourceLocation("pwpwarfare", "textures/gui/voice_icon.png");
    private static final ResourceLocation VOICE_ICON_RADIO =
            new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_radio.png");
    private static final ResourceLocation VOICE_ICON_STAR =
            new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_star.png");

    private static final Component VOTE_TITLE = Component.translatable(HUD + "vote.title");
    private static final Component CMD_TITLE = Component.translatable(HUD + "cmd_vote.header");
    private static final Component CMD_RULE = Component.translatable(HUD + "cmd_vote.rule");
    private static final Component CMD_WAITING = Component.translatable(HUD + "cmd_vote.waiting");
    private static final Component ART_TITLE = Component.translatable(HUD + "art.title");
    private static final Component MEDIC_TITLE = Component.translatable(HUD + "medic.title");

    /** Голос строки: нет / за / против. */
    public static final int VOTE_NONE = 0;

    public static final int VOTE_YES = 1;
    public static final int VOTE_NO = 2;

    /** Строка списка (игрок голосования, раненый, говорящий). Переиспользуется. */
    public static final class Row {
        public String name = "";
        public int vote;
        public boolean self;
        public int distance;
        public boolean radio;
        public boolean cmd;
    }

    /** Список строк фиксированной ёмкости: без аллокаций в кадре. */
    public static final class Rows {
        private final Row[] rows;
        private int count;

        public Rows(int capacity) {
            rows = new Row[capacity];
            for (int i = 0; i < capacity; i++) rows[i] = new Row();
        }

        public void clear() {
            count = 0;
        }

        /** Следующая строка или null, если ёмкость кончилась. */
        public Row add() {
            if (count >= rows.length) return null;
            Row r = rows[count++];
            r.name = "";
            r.vote = VOTE_NONE;
            r.self = false;
            r.distance = 0;
            r.radio = false;
            r.cmd = false;
            return r;
        }

        public Row get(int i) {
            return rows[i];
        }

        public int size() {
            return count;
        }

        /** Голосов «за». */
        public int yes() {
            int n = 0;
            for (int i = 0; i < count; i++) if (rows[i].vote == VOTE_YES) n++;
            return n;
        }

        /** Сортировка вставками по дистанции (раненые): без аллокаций, список маленький. */
        void sortByDistance() {
            for (int i = 1; i < count; i++) {
                Row r = rows[i];
                int j = i - 1;
                while (j >= 0 && rows[j].distance > r.distance) {
                    rows[j + 1] = rows[j];
                    j--;
                }
                rows[j + 1] = r;
            }
        }

        /** Сортировка вставками: радио выше отряда, внутри — по имени (строки не прыгают). */
        void sortVoice() {
            for (int i = 1; i < count; i++) {
                Row r = rows[i];
                int j = i - 1;
                while (j >= 0 && voiceAfter(rows[j], r)) {
                    rows[j + 1] = rows[j];
                    j--;
                }
                rows[j + 1] = r;
            }
        }

        private static boolean voiceAfter(Row a, Row b) {
            if (a.radio != b.radio) return !a.radio;
            return a.name.compareTo(b.name) > 0;
        }
    }

    /** Данные боковых стеков на кадр. Поля переиспользуются (без аллокаций). */
    public static final class Model {
        /** Голосование за старт (данные остаются после конца — для выезда панели). */
        public boolean vote;

        public int voteSeconds;
        public String blueName = "";
        public String redName = "";
        public boolean blueReady;
        public boolean redReady;
        public final Rows voteRows = new Rows(WTheme.SIDE_MAX_ROWS);

        /** Голосование за командира (видят лидеры отрядов команды). */
        public boolean cmd;

        public String cmdCandidate = "";
        /** Кандидат — я (голосовать нельзя, ждём). */
        public boolean cmdSelf;

        public final Rows cmdRows = new Rows(WTheme.SIDE_MAX_ROWS);

        /** Запрос арты (командиру). */
        public boolean art;

        public String artFrom = "";
        public int artX;
        public int artZ;
        /** Остаток окна ответа 0..1. */
        public float artLeft;

        /** Список раненых союзников (медик). */
        public boolean medic;

        public final Rows downed = new Rows(WTheme.SIDE_MAX_ROWS);

        /** Говорящие (радио + отряд). */
        public final Rows voice = new Rows(WTheme.SIDE_MAX_ROWS);

        /** Боковая карта открыта или выезжает: прямоугольник в конечном положении (GUI-пиксели). */
        public boolean mapOpen;

        public int mapX;
        public int mapY;
        public int mapW;
        public int mapH;
    }

    // ------------------------------------------------------------ кэши рендерера
    private final UIDamp.Frame frame = new UIDamp.Frame();
    private float voteSlide;
    private float cmdSlide;
    private float artSlide;
    private final WHud.Clock voteClock = new WHud.Clock();
    private final WHud.Fmt blueReady = new WHud.Fmt(HUD + "vote.ready");
    private final WHud.Fmt blueWaiting = new WHud.Fmt(HUD + "vote.waiting");
    private final WHud.Fmt redReady = new WHud.Fmt(HUD + "vote.ready");
    private final WHud.Fmt redWaiting = new WHud.Fmt(HUD + "vote.waiting");
    private final WHud.Fmt voteTally = new WHud.Fmt(HUD + "value_of");
    private final WHud.Fmt cmdTally = new WHud.Fmt(HUD + "value_of");
    private final WHud.Fmt cmdCandidate = new WHud.Fmt(HUD + "cmd_vote.candidate");
    private final WHud.Fmt artFrom = new WHud.Fmt(HUD + "art.from");
    private final WHud.Fmt artPos = new WHud.Fmt(HUD + "art.pos");
    private final WHud.Fmt more = new WHud.Fmt(HUD + "side.more");
    private final WHud.KeyHint voteKeys =
            new WHud.KeyHint(HUD + "vote.keys", ModKeyBindings.VOTE_START_YES_KEY, ModKeyBindings.VOTE_START_NO_KEY);
    private final WHud.KeyHint cmdKeys =
            new WHud.KeyHint(HUD + "vote.keys", ModKeyBindings.VOTE_CMD_YES_KEY, ModKeyBindings.VOTE_CMD_NO_KEY);
    private final WHud.KeyHint artKeys =
            new WHud.KeyHint(HUD + "art.keys", ModKeyBindings.ART_CONFIRM_KEY, ModKeyBindings.ART_DENY_KEY);
    private final WHud.Fmt[] distFmt = new WHud.Fmt[WTheme.SIDE_MAX_ROWS];
    /** Кэши обрезки строк — свои у каждой панели (иначе панели перебивали бы кэш друг другу). */
    private final Texts voteText = new Texts();

    private final Texts cmdText = new Texts();
    private final Texts artText = new Texts();
    private final Texts medicText = new Texts();
    private final Texts voiceText = new Texts();

    // результат place(): куда встал блок
    private HudZones.Stack placedStack;
    private int placedY;
    private int placedVisible;

    public SideHud() {
        for (int i = 0; i < distFmt.length; i++) distFmt[i] = new WHud.Fmt(HUD + "medic.distance");
    }

    /** Кэши обрезки текста одной панели: шапка, до четырёх строк, строки списка. */
    private static final class Texts {
        final WHud.Fit title = new WHud.Fit();
        final WHud.Fit[] lines = {new WHud.Fit(), new WHud.Fit(), new WHud.Fit(), new WHud.Fit()};
        final WHud.Fit[] rows = new WHud.Fit[WTheme.SIDE_MAX_ROWS];

        Texts() {
            for (int i = 0; i < rows.length; i++) rows[i] = new WHud.Fit();
        }
    }

    // ================================================================ наполнение из ClientData

    /** Заполнить модель из ClientData для {@code mc.player} (под гейтом HUD). */
    public static void fromClient(Model m, Minecraft mc) {
        Player me = mc.player;
        Team team = me.getTeam();
        String teamName = team != null ? team.getName() : "NEUTRAL";
        boolean inTeam = !"NEUTRAL".equalsIgnoreCase(teamName);
        boolean blue = "BLUE".equalsIgnoreCase(teamName);
        String myName = me.getScoreboardName();

        // голосование за старт: игроки моей команды
        m.vote = ClientData.voteActive;
        if (m.vote) {
            m.voteSeconds = Math.max(0, ClientData.voteTimer);
            m.blueName = ClientData.customBlueName;
            m.redName = ClientData.customRedName;
            m.blueReady = ClientData.blueReady;
            m.redReady = ClientData.redReady;
            m.voteRows.clear();
            for (MapPlayerInfo info : ClientData.mapPlayers.values()) {
                if (!info.team.equalsIgnoreCase(teamName)) continue;
                Row r = m.voteRows.add();
                if (r == null) break;
                r.name = info.name;
                r.self = info.name.equals(myName);
                r.vote = vote(ClientData.votes, info.uuid);
            }
        }

        // голосование за командира: лидеры отрядов моей команды, кроме кандидата
        m.cmd = inTeam && (blue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive);
        if (m.cmd) {
            String candidate = blue ? ClientData.blueCmdCandidateName : ClientData.redCmdCandidateName;
            Map<UUID, Boolean> votes = blue ? ClientData.blueCmdVotes : ClientData.redCmdVotes;
            m.cmdCandidate = candidate;
            m.cmdSelf = myName.equals(candidate);
            m.cmdRows.clear();
            for (MapPlayerInfo info : ClientData.mapPlayers.values()) {
                if (!info.team.equalsIgnoreCase(teamName) || !info.isLeader || info.name.equals(candidate)) continue;
                Row r = m.cmdRows.add();
                if (r == null) break;
                r.name = info.name;
                r.self = info.name.equals(myName);
                r.vote = vote(votes, info.uuid);
            }
        }

        // запрос арты: командиру (лидеру отряда CMD) и в creative — как раньше
        m.art = false;
        if (inTeam) {
            int timer = blue ? ClientData.blueArtTimer : ClientData.redArtTimer;
            String requester = blue ? ClientData.blueArtReqName : ClientData.redArtReqName;
            BlockPos pos = blue ? ClientData.blueArtPos : ClientData.redArtPos;
            if (timer > 0 && requester != null && !requester.isEmpty() && pos != null) {
                int squad = WarfareData.of(me).getInt(WarfareKeys.SQUAD_ID);
                boolean leader = WarfareData.of(me).getBoolean(WarfareKeys.IS_SQUAD_LEADER);
                int cmdSquad = blue ? ClientData.blueCMDId : ClientData.redCMDId;
                if (me.isCreative() || leader && squad != -1 && squad == cmdSquad) {
                    m.art = true;
                    m.artFrom = requester;
                    m.artX = pos.getX();
                    m.artZ = pos.getZ();
                    m.artLeft = Math.min(1F, timer / (float) WTheme.SIDE_ART_WINDOW_TICKS);
                }
            }
        }

        // раненые союзники у медика (только своя команда, ближе SIDE_MEDIC_RANGE)
        m.downed.clear();
        m.medic = team != null && "Medic".equalsIgnoreCase(ClientData.myCurrentKit);
        if (m.medic) {
            for (Integer id : ClientData.DOWNED_PLAYERS) {
                Entity e = mc.level.getEntity(id);
                if (!(e instanceof Player downed) || downed == me) continue;
                Team dt = downed.getTeam();
                if (dt == null || !team.isAlliedTo(dt)) continue;
                int d = (int) me.distanceTo(downed);
                if (d >= WTheme.SIDE_MEDIC_RANGE) continue;
                Row r = m.downed.add();
                if (r == null) break;
                r.name = downed.getScoreboardName();
                r.distance = d;
            }
            m.downed.sortByDistance();
            m.medic = m.downed.size() > 0;
        }

        // говорящие: истёкшие (нет пакета дольше SIDE_VOICE_TIMEOUT_MS) удаляются здесь, не в отрисовке
        m.voice.clear();
        long now = System.currentTimeMillis();
        collectVoice(m.voice, ClientData.RADIO_SPEAKERS, true, now);
        collectVoice(m.voice, ClientData.SQUAD_SPEAKERS, false, now);
        m.voice.sortVoice();

        m.mapOpen = ClientData.isMapOpen || ClientData.mapTransition > WTheme.SIDE_SLIDE_EPS;
        if (m.mapOpen) {
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            m.mapW = WarfareOverlay.sideMapWidth(sh);
            m.mapH = WarfareOverlay.sideMapHeight(sh);
            m.mapX = WarfareOverlay.sideMapX(sw, sh);
            m.mapY = WarfareOverlay.sideMapY(sh);
        }
    }

    private static int vote(Map<UUID, Boolean> votes, UUID id) {
        Boolean v = votes.get(id);
        if (v == null) return VOTE_NONE;
        return v ? VOTE_YES : VOTE_NO;
    }

    private static void collectVoice(Rows out, Map<String, Long> speakers, boolean radio, long now) {
        if (speakers.isEmpty()) return;
        Iterator<Map.Entry<String, Long>> it = speakers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() > WTheme.SIDE_VOICE_TIMEOUT_MS) {
                it.remove(); // ConcurrentHashMap: удаление через итератор безопасно
                continue;
            }
            Row r = out.add();
            if (r == null) return;
            r.name = e.getKey();
            r.radio = radio;
            r.cmd = isCmd(e.getKey());
        }
    }

    /** Говорящий — из отряда командира своей команды. */
    private static boolean isCmd(String name) {
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(name)) {
                int cmd = "BLUE".equalsIgnoreCase(s.team) ? ClientData.blueCMDId : ClientData.redCMDId;
                return s.id == cmd;
            }
        }
        return false;
    }

    // ================================================================ отрисовка

    /**
     * Нарисовать боковые стеки (стеки уже сброшены за кадр; баннер кровотечения, если есть, уже
     * занял верх левого стека в {@code RenderGuiEvent.Pre}).
     */
    public void render(GuiGraphics g, Font font, HudZones z, Model m) {
        long dt = frame.dtMs();
        voteSlide = slide(voteSlide, m.vote, dt);
        cmdSlide = slide(cmdSlide, m.cmd, dt);
        artSlide = slide(artSlide, m.art, dt);

        HudZones.Stack left = z.left();
        HudZones.Stack right = m.mapOpen ? z.rightBeside(m.mapX, m.mapY, m.mapW, m.mapH) : z.right();
        // при открытой карте не влезшее справа переносится влево (до группы голоса)
        HudZones.Stack spill = m.mapOpen ? left : null;

        if (voteSlide > 0F) startVote(g, font, z, left, null, m, voteSlide);
        if (cmdSlide > 0F) cmdVote(g, font, z, right, spill, m, cmdSlide);
        if (artSlide > 0F) art(g, font, z, right, spill, m, artSlide);
        if (m.medic) medic(g, font, z, right, spill, m);

        if (m.voice.size() > 0) {
            left.advanceTo(z.midLeftAnchor());
            voice(g, font, z, left, m);
        }
    }

    /** Деления полосы голосования: по голосу на деление, если голосующих немного. */
    private static int segments(int count) {
        return count <= WTheme.SIDE_BAR_MAX_SEGMENTS ? Math.max(1, count) : 1;
    }

    private static float slide(float v, boolean on, long dt) {
        float t = on ? 1F : 0F;
        float n = UIDamp.approach(v, t, WTheme.SIDE_SLIDE_RATE, dt);
        if (Math.abs(n - t) < WTheme.SIDE_SLIDE_EPS) n = t;
        return n;
    }

    // ---------------------------------------------------------------- размещение

    /** Ширина панели стека в базовых единицах. */
    private static int stackWidth(HudZones z, HudZones.Stack st, int max) {
        return Math.min(max, Math.round(st.area().w() / z.scale()));
    }

    /** Свободное место стека в базовых единицах (с запасом на округление масштаба). */
    private static int spaceBase(HudZones z, HudZones.Stack st) {
        int rem = st.remaining();
        if (rem <= 0 || st.area().w() <= 0) return -1;
        return (int) Math.floor((rem - 0.5F) / z.scale());
    }

    /**
     * Занять блок панели: постоянная часть {@code fixed} + строки {@code row} (базовые единицы).
     * Сначала {@code primary}, не влезло — {@code spill}. Результат — {@link #placedStack}/{@link
     * #placedY}/{@link #placedVisible}; false — панель не рисуется.
     */
    private boolean place(
            HudZones z, HudZones.Stack primary, HudZones.Stack spill, int fixed, int row, int count, int minRows) {
        if (tryPlace(z, primary, fixed, row, count, minRows)) return true;
        return spill != null && spill != primary && tryPlace(z, spill, fixed, row, count, minRows);
    }

    private boolean tryPlace(HudZones z, HudZones.Stack st, int fixed, int row, int count, int minRows) {
        int space = spaceBase(z, st);
        if (space < 0) return false;
        int v = SideList.visible(space, fixed, row, count, minRows);
        if (v == SideList.NONE) return false;
        int y = st.next(z.u(SideList.height(fixed, row, v, count)));
        if (y == HudZones.Stack.NONE) return false;
        placedStack = st;
        placedY = y;
        placedVisible = v;
        return true;
    }

    /** Начать панель шириной {@code w} в занятом блоке; выезд от своего края экрана. */
    private void beginPanel(GuiGraphics g, HudZones z, int w, float slide) {
        HudZones.Stack st = placedStack;
        float s = z.scale();
        boolean leftSide = st.area().centerX() < z.width() / 2;
        float x = leftSide ? st.area().x() : st.area().right() - w * s;
        float hidden = leftSide ? -(w * s) - 1F : z.width() + 1F;
        HudDraw.begin(g, x + (hidden - x) * (1F - slide), placedY, s);
    }

    // ---------------------------------------------------------------- общие части панели

    private static int headH(Font font) {
        return font.lineHeight + 2 * HudTheme.PAD_Y;
    }

    private static int lineH(Font font) {
        return font.lineHeight + WTheme.SIDE_LINE_GAP;
    }

    private static int barH() {
        return WTheme.SIDE_BAR_H + 2 * HudTheme.PAD_Y;
    }

    /** Шапка: иконка + заголовок (обрезается) + значение справа; разделитель снизу. */
    private void header(
            GuiGraphics g,
            Font font,
            int w,
            WarfareIcon icon,
            Component title,
            WHud.Fit fit,
            String value,
            int valueColor,
            int accent,
            float o) {
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int ty = HudTheme.PAD_Y + 1;
        WHud.icon(g, icon, pad, ty, WTheme.HUD_ICON, UIDraw.fade(accent, o));
        int tx = pad + WTheme.HUD_ICON + WTheme.HUD_ICON_GAP;
        int vw = value == null ? 0 : font.width(value);
        int vx = w - HudTheme.PAD_X - vw;
        int maxW = Math.max(0, (value == null ? w - HudTheme.PAD_X : vx - HudTheme.PAD_X) - tx);
        String t = fit.of(font, title, maxW);
        HudDraw.text(g, font, t, tx, ty, UIDraw.fade(accent, o), true);
        if (value != null) HudDraw.text(g, font, value, vx, ty, UIDraw.fade(valueColor, o), true);
        int dy = headH(font) - 1;
        g.fill(pad, dy, w - HudTheme.PAD_X, dy + 1, UIDraw.fade(HudTheme.PLATE_BORDER, o));
    }

    /** Строка текста панели, обрезается по ширине. */
    private static void line(GuiGraphics g, Font font, WHud.Fit fit, String s, int y, int w, int color, float o) {
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        String t = fit.of(font, s, Math.max(0, w - pad - HudTheme.PAD_X));
        HudDraw.text(g, font, t, pad, y, UIDraw.fade(color, o), false);
    }

    private static void line(GuiGraphics g, Font font, WHud.Fit fit, Component c, int y, int w, int color, float o) {
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        String t = fit.of(font, c, Math.max(0, w - pad - HudTheme.PAD_X));
        HudDraw.text(g, font, t, pad, y, UIDraw.fade(color, o), false);
    }

    /** Полоса прогресса панели с необязательной отметкой порога {@code mark} (0..1, &lt;0 — нет). */
    private static void bar(GuiGraphics g, int y, int w, float frac, int segments, int fill, float mark, float o) {
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int bw = w - pad - HudTheme.PAD_X;
        int by = y + HudTheme.PAD_Y;
        HudDraw.bar(g, pad, by, bw, WTheme.SIDE_BAR_H, frac, -1F, segments, fill, o);
        if (mark >= 0F) {
            int mx = pad + Math.round(bw * Math.min(1F, mark));
            g.fill(mx, by - 1, mx + 1, by + WTheme.SIDE_BAR_H + 1, UIDraw.fade(HudTheme.VALUE, o));
        }
    }

    /** Строки голосования: метка голоса + имя (своё — акцентом); «+N», если не все влезли. */
    private void voteRows(GuiGraphics g, Font font, Rows rows, WHud.Fit[] fits, int visible, int y, int w, float o) {
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int rh = lineH(font);
        int box = WTheme.SIDE_VOTE_MARK;
        int nameX = pad + box + WTheme.HUD_ICON_GAP + 1;
        int maxW = Math.max(0, w - HudTheme.PAD_X - nameX);
        for (int i = 0; i < visible; i++) {
            Row r = rows.get(i);
            int ry = y + i * rh;
            int by = ry + (font.lineHeight - box) / 2;
            voteMark(g, pad, by, box, r.vote, o);
            String name = fits[i].of(font, r.name, maxW);
            HudDraw.text(g, font, name, nameX, ry, UIDraw.fade(r.self ? HudTheme.ACCENT : HudTheme.VALUE, o), true);
        }
        moreLine(g, font, rows.size() - visible, pad, y + visible * rh, o);
    }

    /** Метка голоса: за — заливка «позитив», против — «опасно», нет голоса — пустая рамка. */
    private static void voteMark(GuiGraphics g, int x, int y, int size, int vote, float o) {
        switch (vote) {
            case VOTE_YES -> g.fill(x, y, x + size, y + size, UIDraw.fade(WTheme.HUD_POSITIVE, o));
            case VOTE_NO -> g.fill(x, y, x + size, y + size, UIDraw.fade(WTheme.HUD_DANGER, o));
            default -> HudDraw.frame(g, x, y, size, size, UIDraw.fade(HudTheme.LABEL, o));
        }
    }

    private void moreLine(GuiGraphics g, Font font, int hidden, int x, int y, float o) {
        if (hidden <= 0) return;
        more.of(hidden);
        HudDraw.text(g, font, more.string(), x, y, UIDraw.fade(HudTheme.LABEL, o), false);
    }

    // ---------------------------------------------------------------- голосование за старт

    private void startVote(
            GuiGraphics g, Font font, HudZones z, HudZones.Stack st, HudZones.Stack spill, Model m, float slide) {
        int lh = lineH(font);
        // шапка + полоса + 2 строки статуса команд + подсказка + низ
        int fixed = headH(font) + barH() + 2 * lh + lh + HudTheme.PAD_Y;
        int count = m.voteRows.size();
        if (!place(z, st, spill, fixed, lh, count, 0)) return;
        int w = stackWidth(z, placedStack, WTheme.SIDE_PANEL_W);
        int h = SideList.height(fixed, lh, placedVisible, count);
        float o = 1F;
        beginPanel(g, z, w, slide);
        HudDraw.plate(g, 0, 0, w, h, HudTheme.ACCENT, o);
        header(
                g,
                font,
                w,
                WarfareIcon.CLOCK,
                VOTE_TITLE,
                voteText.title,
                voteClock.of(m.voteSeconds),
                HudTheme.VALUE,
                HudTheme.ACCENT,
                o);
        int y = headH(font);
        int yes = m.voteRows.yes();
        bar(g, y, w, count > 0 ? yes / (float) count : 0F, segments(count), WTheme.HUD_POSITIVE, -1F, o);
        voteTally.of(yes, count);
        y += barH();
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        // счётчик «за / всего» моей команды — справа от строки BLUE
        String tally = voteTally.string();
        int tw = font.width(tally);
        HudDraw.text(g, font, tally, w - HudTheme.PAD_X - tw, y, UIDraw.fade(HudTheme.LABEL, o), false);
        WHud.Fmt bf = m.blueReady ? blueReady : blueWaiting;
        bf.of(m.blueName);
        teamLine(
                g,
                font,
                voteText.lines[0],
                pad,
                y,
                w - tw - HudTheme.PAD_X,
                WTheme.HUD_TEAM_BLUE,
                bf.string(),
                m.blueReady,
                o);
        y += lh;
        WHud.Fmt rf = m.redReady ? redReady : redWaiting;
        rf.of(m.redName);
        teamLine(g, font, voteText.lines[1], pad, y, w, WTheme.HUD_TEAM_RED, rf.string(), m.redReady, o);
        y += lh;
        voteRows(g, font, m.voteRows, voteText.rows, placedVisible, y, w, o);
        y += lh * SideList.rows(placedVisible, count);
        line(g, font, voteText.lines[2], voteKeys.string(), y, w, HudTheme.LABEL, o);
        HudDraw.end(g);
    }

    /** Строка статуса команды: точка цвета команды + «Имя: ГОТОВЫ/ОЖИДАНИЕ» (до правого края {@code w}). */
    private static void teamLine(
            GuiGraphics g,
            Font font,
            WHud.Fit fit,
            int x,
            int y,
            int w,
            int team,
            String text,
            boolean ready,
            float o) {
        int box = WTheme.SIDE_VOTE_MARK;
        int by = y + (font.lineHeight - box) / 2;
        g.fill(x, by, x + box, by + box, UIDraw.fade(team, o));
        int tx = x + box + WTheme.HUD_ICON_GAP + 1;
        int maxW = Math.max(0, w - HudTheme.PAD_X - tx);
        String s = fit.of(font, text, maxW);
        HudDraw.text(g, font, s, tx, y, UIDraw.fade(ready ? WTheme.HUD_POSITIVE : WTheme.HUD_DANGER, o), true);
    }

    // ---------------------------------------------------------------- голосование за командира

    private void cmdVote(
            GuiGraphics g, Font font, HudZones z, HudZones.Stack st, HudZones.Stack spill, Model m, float slide) {
        int lh = lineH(font);
        // шапка + кандидат + правило + полоса + подсказка + низ
        int fixed = headH(font) + 2 * lh + barH() + lh + HudTheme.PAD_Y;
        int count = m.cmdRows.size();
        if (!place(z, st, spill, fixed, lh, count, 0)) return;
        int w = stackWidth(z, placedStack, WTheme.SIDE_PANEL_W);
        int h = SideList.height(fixed, lh, placedVisible, count);
        float o = 1F;
        int yes = m.cmdRows.yes();
        cmdTally.of(yes, count);
        beginPanel(g, z, w, slide);
        HudDraw.plate(g, 0, 0, w, h, HudTheme.ACCENT, o);
        header(
                g,
                font,
                w,
                WarfareIcon.STAR,
                CMD_TITLE,
                cmdText.title,
                cmdTally.string(),
                HudTheme.VALUE,
                HudTheme.ACCENT,
                o);
        int y = headH(font);
        cmdCandidate.of(m.cmdCandidate);
        line(g, font, cmdText.lines[0], cmdCandidate.string(), y, w, HudTheme.VALUE, o);
        y += lh;
        line(g, font, cmdText.lines[1], CMD_RULE, y, w, HudTheme.LABEL, o);
        y += lh;
        bar(
                g,
                y,
                w,
                count > 0 ? yes / (float) count : 0F,
                segments(count),
                WTheme.HUD_POSITIVE,
                WTheme.SIDE_CMD_PASS,
                o);
        y += barH();
        voteRows(g, font, m.cmdRows, cmdText.rows, placedVisible, y, w, o);
        y += lh * SideList.rows(placedVisible, count);
        if (m.cmdSelf) line(g, font, cmdText.lines[2], CMD_WAITING, y, w, WTheme.HUD_POSITIVE, o);
        else line(g, font, cmdText.lines[2], cmdKeys.string(), y, w, HudTheme.LABEL, o);
        HudDraw.end(g);
    }

    // ---------------------------------------------------------------- запрос арты

    private void art(
            GuiGraphics g, Font font, HudZones z, HudZones.Stack st, HudZones.Stack spill, Model m, float slide) {
        int lh = lineH(font);
        // шапка + «от» + координаты + полоса окна ответа + подсказка + низ
        int fixed = headH(font) + 2 * lh + barH() + lh + HudTheme.PAD_Y;
        if (!place(z, st, spill, fixed, 0, 0, 0)) return;
        int w = stackWidth(z, placedStack, WTheme.SIDE_PANEL_W);
        float o = 1F;
        beginPanel(g, z, w, slide);
        HudDraw.plate(g, 0, 0, w, fixed, WTheme.HUD_DANGER, o);
        header(g, font, w, WarfareIcon.TARGET, ART_TITLE, artText.title, null, HudTheme.VALUE, WTheme.HUD_DANGER, o);
        int y = headH(font);
        artFrom.of(m.artFrom);
        line(g, font, artText.lines[0], artFrom.string(), y, w, HudTheme.VALUE, o);
        y += lh;
        artPos.of(m.artX, m.artZ);
        line(g, font, artText.lines[1], artPos.string(), y, w, HudTheme.LABEL, o);
        y += lh;
        bar(g, y, w, m.artLeft, 1, WTheme.HUD_DANGER, -1F, o);
        y += barH();
        line(g, font, artText.lines[2], artKeys.string(), y, w, HudTheme.LABEL, o);
        HudDraw.end(g);
    }

    // ---------------------------------------------------------------- раненые у медика

    private void medic(GuiGraphics g, Font font, HudZones z, HudZones.Stack st, HudZones.Stack spill, Model m) {
        int lh = lineH(font);
        int fixed = headH(font) + HudTheme.PAD_Y + 1;
        int count = m.downed.size();
        if (!place(z, st, spill, fixed, lh, count, 1)) return;
        int w = stackWidth(z, placedStack, WTheme.SIDE_LIST_W);
        int h = SideList.height(fixed, lh, placedVisible, count);
        float o = 1F;
        beginPanel(g, z, w, 1F);
        HudDraw.plate(g, 0, 0, w, h, WTheme.HUD_MEDIC, o);
        header(g, font, w, WarfareIcon.PLUS, MEDIC_TITLE, medicText.title, null, HudTheme.VALUE, WTheme.HUD_MEDIC, o);
        int y = headH(font) + 1;
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        for (int i = 0; i < placedVisible; i++) {
            Row r = m.downed.get(i);
            distFmt[i].of(r.distance);
            String d = distFmt[i].string();
            int dw = font.width(d);
            int dx = w - HudTheme.PAD_X - dw;
            HudDraw.text(g, font, d, dx, y, UIDraw.fade(HudTheme.LABEL, o), false);
            String name = medicText.rows[i].of(font, r.name, Math.max(0, dx - HudTheme.PAD_X - pad));
            HudDraw.text(g, font, name, pad, y, UIDraw.fade(HudTheme.VALUE, o), true);
            y += lh;
        }
        moreLine(g, font, count - placedVisible, pad, y, o);
        HudDraw.end(g);
    }

    // ---------------------------------------------------------------- голос

    /** Говорящие: по плашке на строку (радио — акцент, отряд — «позитив»), «эквалайзер» справа. */
    private void voice(GuiGraphics g, Font font, HudZones z, HudZones.Stack st, Model m) {
        int rowH = WHud.plateHeight(font);
        int row = rowH + WTheme.SIDE_VOICE_GAP;
        int count = m.voice.size();
        if (!place(z, st, null, 0, row, count, 1)) return;
        float s = z.scale();
        int maxW = stackWidth(z, placedStack, WTheme.SIDE_VOICE_W);
        int pad = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int eqW = WTheme.SIDE_EQ_BARS * 2;
        long now = System.currentTimeMillis();
        for (int i = 0; i < placedVisible; i++) {
            Row r = m.voice.get(i);
            int color = r.radio ? HudTheme.ACCENT : WTheme.HUD_POSITIVE;
            int nameX = pad + WTheme.HUD_ICON + WTheme.HUD_ICON_GAP;
            int nameMax = Math.max(0, maxW - nameX - WTheme.HUD_ICON_GAP - eqW - HudTheme.PAD_X);
            String name = voiceText.rows[i].of(font, r.name, nameMax);
            int w = Math.min(maxW, nameX + font.width(name) + WTheme.HUD_ICON_GAP + eqW + HudTheme.PAD_X);
            HudDraw.begin(g, placedStack.area().x(), placedY + z.u(i * row), s);
            HudDraw.plate(g, 0, 0, w, rowH, color, 1F);
            int ty = HudTheme.PAD_Y + 1;
            ResourceLocation icon = r.cmd ? VOICE_ICON_STAR : (r.radio ? VOICE_ICON_RADIO : VOICE_ICON);
            HudDraw.icon(g, icon, 0, 0, 1, 1, 1, pad, ty, WTheme.HUD_ICON, color);
            HudDraw.text(g, font, name, nameX, ty, color, true);
            equalizer(g, w - HudTheme.PAD_X - eqW, ty, font.lineHeight - 1, color, now + i * WTheme.SIDE_EQ_ROW_MS);
            HudDraw.end(g);
        }
        if (count > placedVisible) {
            HudDraw.begin(g, placedStack.area().x(), placedY + z.u(placedVisible * row), s);
            moreLine(g, font, count - placedVisible, pad, HudTheme.PAD_Y + 1, 1F);
            HudDraw.end(g);
        }
    }

    /** Три столбика «уровня голоса» (реальное время, без аллокаций). */
    private static void equalizer(GuiGraphics g, int x, int y, int h, int color, long t) {
        for (int i = 0; i < WTheme.SIDE_EQ_BARS; i++) {
            double ph = ((t + i * WTheme.SIDE_EQ_PHASE_MS) % WTheme.SIDE_EQ_PERIOD_MS)
                    / (double) WTheme.SIDE_EQ_PERIOD_MS
                    * Math.PI
                    * 2.0;
            int bh = Math.max(1, Math.round(h * (0.35F + 0.65F * (0.5F + 0.5F * (float) Math.sin(ph)))));
            int bx = x + i * 2;
            g.fill(bx, y + h - bh, bx + 1, y + h, color);
        }
    }
}
