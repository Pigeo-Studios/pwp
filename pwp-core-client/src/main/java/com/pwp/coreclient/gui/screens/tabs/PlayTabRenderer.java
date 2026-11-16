package com.pwp.coreclient.gui.screens.tabs;

import com.pwp.coreclient.gui.components.PWPMatchCard;
import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.LobbyStatePacket;
import com.pwp.coreclient.ui.UICard;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.UIScrollArea;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Вкладка «Игра»: активные матчи карточками {@link UICard} (превью карты, статус, время,
 * стороны и тикеты, счётчик игроков в колонке фиксированной ширины). Клик — вход в матч
 * ({@link LobbyBackend#joinMatch}, прежний JoinMatchServerPacket). Данные — только из пакета.
 */
public class PlayTabRenderer implements LobbyTabView {

    public static class MatchEntry {
        public String title;
        public String mode;
        public int elapsedSec;
        public int currentPlayers;
        public int maxPlayers;
        public String blueFaction;
        public String redFaction;
        public int blueTickets;
        public int redTickets;
        public PWPMatchCard.Status status;
        public int serverId;
        public String worldPath;
        public boolean current;

        MatchEntry(
                String title,
                String mode,
                int elapsedSec,
                int current,
                int max,
                String blue,
                String red,
                int bt,
                int rt,
                PWPMatchCard.Status status,
                int serverId,
                String worldPath,
                boolean isCurrent) {
            this.title = title;
            this.mode = mode;
            this.elapsedSec = elapsedSec;
            this.currentPlayers = current;
            this.maxPlayers = max;
            this.blueFaction = blue;
            this.redFaction = red;
            this.blueTickets = bt;
            this.redTickets = rt;
            this.status = status;
            this.serverId = serverId;
            this.worldPath = worldPath;
            this.current = isCurrent;
        }
    }

    private LobbyBackend backend = LobbyBackend.NETWORK;
    private final List<MatchEntry> matches = new ArrayList<>();
    private final UILayer layer = new UILayer();
    private final UIScrollArea grid = new UIScrollArea();
    private final List<UICard> cards = new ArrayList<>();
    private Rect area = Rect.of(0, 0, 0, 0);
    private Rect titleR = area, bodyR = area;
    private long shownAt = System.currentTimeMillis();

    public void setBackend(LobbyBackend backend) {
        this.backend = backend;
    }

    public void applyState(LobbyStatePacket pkt) {
        int before = matches.size();
        matches.clear();
        // Активный матч лобби — первой карточкой
        if (pkt.phase == LobbyStatePacket.PHASE_MATCH_STARTING || pkt.phase == LobbyStatePacket.PHASE_MATCH_PLAYING) {
            matches.add(new MatchEntry(
                    pkt.matchMapDisplay != null ? pkt.matchMapDisplay : "—",
                    pkt.matchModeDisplay != null ? pkt.matchModeDisplay : "",
                    pkt.matchElapsed,
                    pkt.matchPlayers,
                    pkt.matchMaxPlayers,
                    pkt.matchBlueFaction,
                    pkt.matchRedFaction,
                    pkt.matchBlueTickets,
                    pkt.matchRedTickets,
                    pkt.phase == LobbyStatePacket.PHASE_MATCH_STARTING
                            ? PWPMatchCard.Status.STARTING
                            : PWPMatchCard.Status.PLAYING,
                    pkt.matchServerId,
                    pkt.matchWorldPath,
                    true));
        }
        for (int i = 0; i < pkt.matchCount; i++) {
            String status = at(pkt.mStatuses, i);
            matches.add(new MatchEntry(
                    at(pkt.mDisplayNames, i) != null ? at(pkt.mDisplayNames, i) : "—",
                    status != null ? status : "",
                    at(pkt.mElapsed, i),
                    at(pkt.mPlayers, i),
                    at(pkt.mMaxPlayers, i),
                    at(pkt.mBlueFactions, i),
                    at(pkt.mRedFactions, i),
                    at(pkt.mBlueTickets, i),
                    at(pkt.mRedTickets, i),
                    parseStatus(status),
                    pkt.mServerIds != null && i < pkt.mServerIds.length ? pkt.mServerIds[i] : -1,
                    at(pkt.mWorldPaths, i),
                    false));
        }
        if (matches.size() != before || cards.size() != matches.size()) rebuild();
        else update();
    }

    private static String at(String[] a, int i) {
        return a != null && i >= 0 && i < a.length ? a[i] : null;
    }

    private static int at(int[] a, int i) {
        return a != null && i >= 0 && i < a.length ? a[i] : 0;
    }

    private static PWPMatchCard.Status parseStatus(String s) {
        return switch (s != null ? s : "") {
            case "PLAYING" -> PWPMatchCard.Status.PLAYING;
            case "STARTING" -> PWPMatchCard.Status.STARTING;
            case "FULL" -> PWPMatchCard.Status.FULL;
            case "CLOSED" -> PWPMatchCard.Status.CLOSED;
            case "VOTING" -> PWPMatchCard.Status.VOTING;
            case "FINISHED" -> PWPMatchCard.Status.FINISHED;
            default -> PWPMatchCard.Status.WAITING;
        };
    }

    public List<MatchEntry> getMatches() {
        return matches;
    }

    @Override
    public void layout(Rect area) {
        this.area = area;
        rebuild();
    }

    @Override
    public void onShow() {
        shownAt = System.currentTimeMillis();
    }

    private void rebuild() {
        layer.clear();
        grid.clear();
        cards.clear();
        Font font = PWPTheme.Fonts.display();
        Rect[] col = Flow.column(area, 0, Flow.Justify.START, font.lineHeight, Spacing.SM, Flow.flex(1));
        titleR = col[0];
        bodyR = col[2];
        grid.setBounds(bodyR);
        layer.add(grid);
        int n = matches.size();
        if (n > 0) {
            int gap = Spacing.GRID_GAP;
            int cardH = area.h() < Spacing.LOBBY_COMPACT_H ? Spacing.VOTE_CARD_H : Spacing.VOTE_CARD_H_LARGE;
            int cols =
                    Math.min(n, Grid.columnsFor(bodyR.w(), Spacing.MATCH_CARD_MIN_W, gap, Spacing.MATCH_CARD_MAX_COLS));
            int contentW = grid.contentWidth(Grid.height(n, cols, cardH, gap));
            int gridW = Math.min(contentW, cols * Spacing.MATCH_CARD_MAX_W + (cols - 1) * gap);
            Rect[] cells = Grid.cells(Rect.of((contentW - gridW) / 2, 0, gridW, 0), n, cols, cardH, gap);
            int badgeW = font.width("00/00");
            for (int i = 0; i < n; i++) {
                final int idx = i;
                UICard c = new UICard(Component.empty(), () -> join(idx)).badgeWidth(badgeW);
                cards.add(grid.place(c, cells[i]));
            }
        }
        update();
    }

    private void join(int idx) {
        if (idx < 0 || idx >= matches.size()) return;
        int sid = matches.get(idx).serverId;
        if (sid >= 0) backend.joinMatch(sid);
    }

    private void update() {
        for (int i = 0; i < cards.size() && i < matches.size(); i++) {
            MatchEntry m = matches.get(i);
            UICard c = cards.get(i);
            c.label(Component.literal(m.title));
            c.subtitle(subtitle(m))
                    .badge(Component.literal(m.currentPlayers + "/" + m.maxPlayers))
                    .image(PWPLobbyScreen.getTexture("preview", m.worldPath, "match_"), 1, 1);
            c.selected(m.current);
            c.setEnabled(m.serverId >= 0);
            c.tooltip(tooltip(m));
        }
    }

    private static Component statusText(PWPMatchCard.Status s) {
        return Component.translatable("pwp_core.lobby.status." + s.name().toLowerCase());
    }

    private static boolean hasTickets(MatchEntry m) {
        return m.status == PWPMatchCard.Status.PLAYING || m.status == PWPMatchCard.Status.FINISHED;
    }

    /** «Статус · 12:34 · США vs Россия · 100:80» — порядок важности, хвост обрежет «…». */
    private static Component subtitle(MatchEntry m) {
        var c = Component.empty().append(statusText(m.status));
        c.append(" · " + LobbyText.clock(m.elapsedSec));
        if (m.blueFaction != null && !m.blueFaction.isBlank()) {
            c.append(" · ")
                    .append(Component.translatable(
                            "pwp_core.lobby.versus",
                            LobbyText.faction(m.blueFaction),
                            LobbyText.faction(m.redFaction)));
        }
        if (hasTickets(m)) c.append(" · " + m.blueTickets + ":" + m.redTickets);
        return c;
    }

    private static UITooltip.Content tooltip(MatchEntry m) {
        List<Component> lines = new ArrayList<>();
        if (!m.mode.isBlank()) lines.add(Component.literal(m.mode));
        lines.add(statusText(m.status).copy().append(" · " + LobbyText.clock(m.elapsedSec)));
        if (m.blueFaction != null && !m.blueFaction.isBlank()) {
            lines.add(Component.translatable(
                    "pwp_core.lobby.versus", LobbyText.faction(m.blueFaction), LobbyText.faction(m.redFaction)));
        }
        lines.add(Component.translatable("pwp_core.lobby.match.players", m.currentPlayers, m.maxPlayers));
        if (hasTickets(m)) {
            lines.add(Component.translatable("pwp_core.lobby.match.tickets_value", m.blueTickets, m.redTickets));
        }
        if (m.serverId >= 0) lines.add(Component.translatable("pwp_core.lobby.match.click_to_join"));
        return new UITooltip.Content(Component.literal(m.title), lines);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font font = PWPTheme.Fonts.display();
        long elapsed = System.currentTimeMillis() - shownAt;
        float fade = UIMath.stagger(elapsed, 0, 0, 0, Animations.ENTER);
        Component count = Component.literal(String.valueOf(matches.size()));
        int cw = font.width(count);
        UIDraw.sectionTitle(
                g,
                font,
                titleR.withWidth(titleR.w() - cw - Spacing.SM),
                Component.translatable("pwp_core.lobby.play.title"),
                PWPTheme.Text.HEADING,
                fade);
        UIDraw.text(g, font, count, titleR.right() - cw, titleR.y(), UIDraw.fade(Colors.TEXT_SECONDARY, fade), false);
        for (int i = 0; i < cards.size(); i++) {
            float p = UIMath.stagger(elapsed, i, 0, Animations.STAGGER_FAST, Animations.ENTER);
            cards.get(i).setAppearance(p, 0, Math.round((1F - p) * Animations.ENTER_SLIDE));
        }
        if (matches.isEmpty()) {
            UIDraw.emptyState(
                    g,
                    font,
                    bodyR,
                    UIIcon.SERVER,
                    Component.translatable("pwp_core.lobby.play.empty"),
                    Component.translatable("pwp_core.lobby.play.empty_hint"),
                    fade);
        }
        var win = Minecraft.getInstance().getWindow();
        layer.render(g, mx, my, pt, win.getGuiScaledWidth(), win.getGuiScaledHeight());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return layer.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return layer.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return layer.mouseScrolled(mx, my, delta);
    }
}
