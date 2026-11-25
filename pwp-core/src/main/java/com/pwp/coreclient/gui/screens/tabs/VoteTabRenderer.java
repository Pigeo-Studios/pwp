package com.pwp.coreclient.gui.screens.tabs;

import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.network.LobbyStatePacket;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UICard;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.UIProgressBar;
import com.pwp.coreclient.ui.UIScrollArea;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Вкладка «Голосование». Фазы приходят ТОЛЬКО от сервера (LobbyStatePacket.phase):
 * карта / режим / фракции — карточки {@link UICard} в адаптивной сетке ({@link Grid#columnsFor}),
 * ожидание, итоги (поля result* пакета), идёт матч. Сверху — заголовок, таймер в колонке
 * фиксированной ширины, «проголосовало X из Y» с полосой {@link UIProgressBar}, лидер.
 *
 * <p>Клик по карточке — выбор (selected + акцентная полоса), кнопка «Голосовать» отправляет
 * прежние команды ({@code votemap / votemode / votefaction / pwp}) через {@link LobbyBackend}.
 * Голос игрока (галочка у счётчика) — из пакета (myMapVote...), после клика — сразу локально,
 * следующий пакет подтверждает.
 */
public class VoteTabRenderer implements LobbyTabView {

    private enum Phase {
        IDLE,
        MAP,
        MODE,
        FACTION,
        FINISHED,
        MATCH
    }

    /** Блокировка «Нового голосования» сразу после показа итогов (мс). */
    private static final long FINISHED_LOCK_MS = 600;

    private static final ResourceLocation MODE_AAS =
            new ResourceLocation("pwp_core_client", "textures/gui/modes/aas.png");
    private static final ResourceLocation MODE_INVASION =
            new ResourceLocation("pwp_core_client", "textures/gui/modes/invasion.png");

    private LobbyBackend backend = LobbyBackend.NETWORK;
    private LobbyStatePacket st;
    private Phase phase = Phase.IDLE;
    private long phaseEnter = System.currentTimeMillis();
    private int lastPacketPhase = -1;

    private int selMap = -1, selMode = -1, selF1 = -1, selF2 = -1;
    private int votMap = -1, votMode = -1, votF1 = -1, votF2 = -1;

    private final UILayer layer = new UILayer();
    private final UIScrollArea grid = new UIScrollArea();
    private final List<UICard> cards = new ArrayList<>();
    private final UIProgressBar turnout = new UIProgressBar(0, 0, 0, 0);
    private UIButton voteButton;
    private UIButton newVoteButton;

    private Rect area = Rect.of(0, 0, 0, 0);
    private Rect titleR = area, infoR = area, bodyR = area;
    private final Rect[] teamLabelR = {area, area};
    private Phase builtPhase;
    private int builtCount = -1;

    public void setBackend(LobbyBackend backend) {
        this.backend = backend;
    }

    // ================================================================ состояние

    /** Единственный источник истины — сервер (пакет раз в секунду + при изменениях). */
    public void applyState(LobbyStatePacket pkt) {
        st = pkt;
        boolean phaseChanged = pkt.phase != lastPacketPhase;
        lastPacketPhase = pkt.phase;
        if (phaseChanged) {
            // выбор сбрасывается только при смене фазы — иначе секундная синхронизация затирает выделение
            selMap = selMode = selF1 = selF2 = -1;
        }
        votMap = pkt.myMapVote;
        votMode = pkt.myModeVote;
        votF1 = pkt.myFaction1;
        votF2 = pkt.myFaction2;
        Phase next =
                switch (pkt.phase) {
                    case LobbyStatePacket.PHASE_MAP_VOTE -> Phase.MAP;
                    case LobbyStatePacket.PHASE_MODE_VOTE -> Phase.MODE;
                    case LobbyStatePacket.PHASE_FACTION_VOTE -> Phase.FACTION;
                    case LobbyStatePacket.PHASE_MATCH_STARTING, LobbyStatePacket.PHASE_MATCH_PLAYING -> Phase.MATCH;
                    default -> pkt.hasResults() ? Phase.FINISHED : Phase.IDLE;
                };
        if (next != phase) {
            phase = next;
            phaseEnter = System.currentTimeMillis();
        }
        if (phase != builtPhase || optionCount() != builtCount) rebuild();
        else update();
    }

    private int optionCount() {
        if (st == null) return 0;
        return switch (phase) {
            case MAP -> len(st.mapNames);
            case MODE -> len(st.modeNames);
            case FACTION -> 6;
            default -> 0;
        };
    }

    private static int len(Object[] a) {
        return a == null ? 0 : a.length;
    }

    private static String at(String[] a, int i) {
        return a != null && i >= 0 && i < a.length ? a[i] : null;
    }

    private static int at(int[] a, int i) {
        return a != null && i >= 0 && i < a.length ? a[i] : 0;
    }

    // ================================================================ раскладка

    @Override
    public void layout(Rect area) {
        this.area = area;
        rebuild();
    }

    @Override
    public void onShow() {
        phaseEnter = System.currentTimeMillis();
    }

    private boolean compact() {
        return area.h() < Spacing.LOBBY_COMPACT_H;
    }

    private int buttonHeight() {
        return compact() ? Spacing.MENU_BUTTON_H_COMPACT : Spacing.BUTTON_HEIGHT;
    }

    private void rebuild() {
        builtPhase = phase;
        builtCount = optionCount();
        layer.clear();
        grid.clear();
        cards.clear();
        Font font = font();
        int lh = font.lineHeight;
        int btnH = buttonHeight();
        boolean voting = phase == Phase.MAP || phase == Phase.MODE || phase == Phase.FACTION;

        if (voting) {
            int labels = phase == Phase.FACTION ? lh + Spacing.XS : 0;
            Rect[] col = Flow.column(
                    area,
                    0,
                    Flow.Justify.START,
                    lh,
                    Spacing.XS,
                    lh,
                    Spacing.XXS,
                    Spacing.VOTE_BAR_H,
                    Spacing.SM,
                    labels,
                    Flow.flex(1),
                    Spacing.SM,
                    btnH);
            titleR = col[0];
            infoR = col[2];
            turnout.setBounds(col[4]);
            Rect labelsR = col[6];
            bodyR = col[7];
            layer.add(turnout);
            grid.setBounds(bodyR);
            layer.add(grid);
            voteButton = new UIButton(Component.translatable("pwp_core.lobby.vote.submit"), this::vote)
                    .style(UIButton.Style.ACCENT)
                    .icon(UIIcon.CHECK);
            int bw = Math.min(col[9].w(), Math.max(Spacing.BUTTON_WIDTH + Spacing.XXL, voteButton.preferredWidth()));
            voteButton.setBounds(col[9].align(bw, btnH, Align.CENTER, Align.STRETCH));
            layer.add(voteButton);
            if (phase == Phase.FACTION) buildFactions(labelsR);
            else buildOptions();
        } else {
            Rect[] col = Flow.column(area, 0, Flow.Justify.START, lh, Spacing.SM, Flow.flex(1), Spacing.SM, btnH);
            titleR = col[0];
            infoR = Rect.of(area.x(), titleR.y(), 0, 0);
            bodyR = phase == Phase.FINISHED
                    ? col[2]
                    : Rect.of(area.x(), col[2].y(), area.w(), area.bottom() - col[2].y());
            if (phase == Phase.FINISHED) {
                newVoteButton = new UIButton(
                                Component.translatable("pwp_core.lobby.vote.new"), () -> backend.sendCommand("pwp"))
                        .style(UIButton.Style.ACCENT);
                int bw = Math.min(
                        col[4].w(), Math.max(Spacing.BUTTON_WIDTH + Spacing.XXL, newVoteButton.preferredWidth()));
                newVoteButton.setBounds(col[4].align(bw, btnH, Align.CENTER, Align.STRETCH));
                layer.add(newVoteButton);
            }
        }
        update();
    }

    /** Карточки карт/режимов: сетка по ширине, карточки не шире VOTE_CARD_MAX_W, по центру. */
    private void buildOptions() {
        int n = optionCount();
        if (n == 0) return;
        int gap = Spacing.GRID_GAP;
        int cardH = compact() ? Spacing.VOTE_CARD_H : Spacing.VOTE_CARD_H_LARGE;
        int cols = Math.min(n, Grid.columnsFor(bodyR.w(), Spacing.VOTE_CARD_MIN_W, gap, Spacing.VOTE_CARD_MAX_COLS));
        int contentW = grid.contentWidth(Grid.height(n, cols, cardH, gap));
        int gridW = Math.min(contentW, cols * Spacing.VOTE_CARD_MAX_W + (cols - 1) * gap);
        Rect inner = Rect.of((contentW - gridW) / 2, 0, gridW, Grid.height(n, cols, cardH, gap));
        Rect[] cells = Grid.cells(inner, n, cols, cardH, gap);
        int badgeW = font().width("999");
        for (int i = 0; i < n; i++) {
            final int idx = i;
            UICard c = new UICard(Component.empty(), () -> select(idx)).badgeWidth(badgeW);
            cards.add(grid.place(c, cells[i]));
        }
    }

    /** Две колонки команд по 3 карточки фракций. */
    private void buildFactions(Rect labelsR) {
        int gap = Spacing.MD;
        int cardH = Spacing.FACTION_CARD_H;
        int colH = 3 * cardH + 2 * Spacing.XS;
        int contentW = grid.contentWidth(colH);
        int colW = Math.min((contentW - gap) / 2, Spacing.FACTION_COL_MAX_W);
        Rect[] cols = Flow.row(Rect.of(0, 0, contentW, colH), gap, Flow.Justify.CENTER, colW, colW);
        int badgeW = font().width("999");
        for (int team = 0; team < 2; team++) {
            teamLabelR[team] = Rect.of(bodyR.x() + cols[team].x(), labelsR.y(), cols[team].w(), font().lineHeight);
            Rect[] rows = Flow.column(cols[team], Spacing.XS, Flow.Justify.START, cardH, cardH, cardH);
            for (int r = 0; r < 3; r++) {
                final int t = team, row = r;
                UICard c = new UICard(Component.empty(), () -> selectFaction(t, row)).badgeWidth(badgeW);
                cards.add(grid.place(c, rows[r]));
            }
        }
    }

    /** Обновить содержимое карточек и кнопок без пересоздания (анимации hover/selected живут). */
    private void update() {
        if (st == null) return;
        int online = Math.max(0, st.onlinePlayers);
        turnout.progress(online > 0 ? Math.min(1F, st.totalVotes / (float) online) : 0F, true);
        switch (phase) {
            case MAP -> updateMaps();
            case MODE -> updateModes();
            case FACTION -> updateFactions();
            default -> {}
        }
        if (voteButton != null) {
            boolean canVote = canVote();
            boolean voted =
                    switch (phase) {
                        case MAP -> votMap >= 0;
                        case MODE -> votMode >= 0;
                        case FACTION -> votF1 >= 0 || votF2 >= 0;
                        default -> false;
                    };
            voteButton.label(
                    Component.translatable(voted ? "pwp_core.lobby.vote.change" : "pwp_core.lobby.vote.submit"));
            voteButton.setEnabled(canVote);
        }
    }

    private void updateMaps() {
        int total = 0;
        for (int i = 0; i < cards.size(); i++) total += at(st.voteCounts, i);
        for (int i = 0; i < cards.size(); i++) {
            UICard c = cards.get(i);
            String name = at(st.mapDisplayNames, i);
            if (name == null) name = at(st.mapNames, i);
            int votes = at(st.voteCounts, i);
            String blue = at(st.blueFactions, i), red = at(st.redFactions, i);
            String desc = at(st.mapDescriptions, i);
            Component sub = blue != null && !blue.isBlank()
                    ? Component.translatable("pwp_core.lobby.versus", LobbyText.faction(blue), LobbyText.faction(red))
                    : Component.literal(desc != null ? desc : "");
            c.label(Component.literal(name != null ? name : "—"));
            c.subtitle(sub)
                    .badge(Component.literal(String.valueOf(votes)))
                    .marked(i == votMap)
                    .share(total > 0 ? votes / (float) total : 0F, Styles.Loader.FILL);
            c.selected(i == selMap);
            ResourceLocation preview = PWPLobbyScreen.getTexture("preview", at(st.worldPaths, i), "vote_");
            c.image(preview, 1, 1);
            c.tooltip(tooltip(name, desc, at(st.maxPlayers, i)));
        }
    }

    private void updateModes() {
        int total = 0;
        for (int i = 0; i < cards.size(); i++) total += at(st.modeVoteCounts, i);
        for (int i = 0; i < cards.size(); i++) {
            UICard c = cards.get(i);
            String id = at(st.modeNames, i);
            String name = at(st.modeDisplayNames, i);
            if (name == null) name = id;
            String desc = at(st.modeDescriptions, i);
            int votes = at(st.modeVoteCounts, i);
            c.label(Component.literal(name != null ? name : "—"));
            c.subtitle(Component.literal(desc != null ? desc : ""))
                    .badge(Component.literal(String.valueOf(votes)))
                    .marked(i == votMode)
                    .share(total > 0 ? votes / (float) total : 0F, Styles.Loader.FILL);
            c.selected(i == selMode);
            ResourceLocation img =
                    "invasion".equalsIgnoreCase(id) ? MODE_INVASION : "aas".equalsIgnoreCase(id) ? MODE_AAS : null;
            c.image(img, 1, 1);
            c.tooltip(tooltip(name, desc, 0));
        }
    }

    private void updateFactions() {
        for (int team = 0; team < 2; team++) {
            String[] ids = team == 0 ? st.team1Factions : st.team2Factions;
            int[] votes = team == 0 ? st.team1Votes : st.team2Votes;
            int total = 0;
            for (int r = 0; r < 3; r++) total += at(votes, r);
            int sel = team == 0 ? selF1 : selF2;
            int vot = team == 0 ? votF1 : votF2;
            int color = team == 0 ? Colors.TEAM_BLUE : Colors.TEAM_RED;
            for (int r = 0; r < 3; r++) {
                UICard c = cards.get(team * 3 + r);
                String id = at(ids, r);
                boolean present = id != null && !id.isBlank();
                int v = at(votes, r);
                c.label(LobbyText.faction(id));
                c.badge(Component.literal(String.valueOf(v)))
                        .marked(r == vot)
                        .share(total > 0 ? v / (float) total : 0F, color)
                        .image(present ? LobbyText.flag(id) : null, LobbyText.FLAG_W, LobbyText.FLAG_H);
                c.selected(r == sel);
                c.setEnabled(present);
            }
        }
    }

    private static UITooltip.Content tooltip(String name, String desc, int maxPlayers) {
        List<Component> lines = new ArrayList<>();
        if (desc != null && !desc.isBlank()) lines.add(Component.literal(desc));
        if (maxPlayers > 0) lines.add(Component.translatable("pwp_core.lobby.vote.max_players", maxPlayers));
        return new UITooltip.Content(Component.literal(name != null ? name : "—"), lines);
    }

    // ================================================================ действия

    private void select(int idx) {
        if (phase == Phase.MAP) selMap = idx;
        else if (phase == Phase.MODE) selMode = idx;
        update();
    }

    private void selectFaction(int team, int row) {
        if (team == 0) selF1 = row;
        else selF2 = row;
        update();
    }

    /** Правила те же, что раньше: голос только при новом выборе; фракции — обе стороны. */
    private boolean canVote() {
        if (st == null) return false;
        return switch (phase) {
            case MAP -> selMap >= 0 && selMap != votMap && notBlank(at(st.mapNames, selMap));
            case MODE -> selMode >= 0 && selMode != votMode && notBlank(at(st.modeNames, selMode));
            case FACTION -> selF1 >= 0
                    && selF2 >= 0
                    && !(selF1 == votF1 && selF2 == votF2)
                    && notBlank(at(st.team1Factions, selF1))
                    && notBlank(at(st.team2Factions, selF2));
            default -> false;
        };
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private void vote() {
        if (!canVote()) return;
        switch (phase) {
            case MAP -> {
                backend.sendCommand("votemap " + at(st.mapNames, selMap));
                votMap = selMap;
            }
            case MODE -> {
                backend.sendCommand("votemode " + at(st.modeNames, selMode));
                votMode = selMode;
            }
            case FACTION -> {
                backend.sendCommand("votefaction " + at(st.team1Factions, selF1) + " " + at(st.team2Factions, selF2));
                votF1 = selF1;
                votF2 = selF2;
            }
            default -> {}
        }
        update();
    }

    // ================================================================ рисование

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font font = font();
        long elapsed = System.currentTimeMillis() - phaseEnter;
        float fade = UIMath.stagger(elapsed, 0, 0, 0, Animations.ENTER);

        for (int i = 0; i < cards.size(); i++) {
            float p = UIMath.stagger(elapsed, i, 0, Animations.STAGGER_FAST, Animations.ENTER);
            cards.get(i).setAppearance(p, 0, Math.round((1F - p) * Animations.ENTER_SLIDE));
        }
        if (newVoteButton != null) {
            newVoteButton.setEnabled(elapsed >= FINISHED_LOCK_MS);
            newVoteButton.setAppearance(fade, 0, 0);
        }

        switch (phase) {
            case MAP, MODE, FACTION -> renderVoteHeader(g, font, fade);
            case IDLE -> renderIdle(g, font, fade);
            case FINISHED -> renderFinished(g, font, elapsed, fade);
            case MATCH -> renderMatch(g, font, fade);
        }
        if (phase == Phase.FACTION) renderTeamLabels(g, font, fade);
        layer.render(
                g,
                mx,
                my,
                pt,
                Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                Minecraft.getInstance().getWindow().getGuiScaledHeight());
        if (phase == Phase.MAP || phase == Phase.MODE) {
            if (cards.isEmpty()) {
                UIDraw.emptyState(
                        g,
                        font,
                        bodyR,
                        UIIcon.INFO,
                        Component.translatable("pwp_core.lobby.vote.no_options"),
                        null,
                        fade);
            }
        }
    }

    private Component phaseTitle() {
        return Component.translatable(
                switch (phase) {
                    case MAP -> "pwp_core.lobby.vote.title_map";
                    case MODE -> "pwp_core.lobby.vote.title_mode";
                    case FACTION -> "pwp_core.lobby.vote.title_faction";
                    case FINISHED -> "pwp_core.lobby.vote.title_finished";
                    case MATCH -> st != null && st.phase == LobbyStatePacket.PHASE_MATCH_STARTING
                            ? "pwp_core.lobby.match.starting"
                            : "pwp_core.lobby.match.playing";
                    default -> "pwp_core.lobby.vote.title";
                });
    }

    /** Заголовок + таймер (фиксированная колонка «00:00») + явка и лидер. */
    private void renderVoteHeader(GuiGraphics g, Font font, float fade) {
        int timerW = font.width("00:00");
        String clock = LobbyText.clock(st != null ? st.remainingSeconds : 0);
        boolean urgent = st != null && st.remainingSeconds <= 10;
        int tx = titleR.right() - timerW + (timerW - font.width(clock));
        UIDraw.text(
                g,
                font,
                Component.literal(clock),
                tx,
                titleR.y(),
                UIDraw.fade(urgent ? PWPTheme.Text.DANGER : PWPTheme.Text.ACCENT, fade),
                true);
        Component left = Component.translatable("pwp_core.lobby.vote.remaining");
        int lw = font.width(left);
        int titleRight = titleR.right() - timerW - Spacing.SM;
        if (titleR.w() - timerW > lw + Spacing.XXL * 3) {
            UIDraw.text(g, font, left, titleRight - lw, titleR.y(), UIDraw.fade(PWPTheme.Text.CAPTION, fade), false);
            titleRight -= lw + Spacing.SM;
        }
        UIDraw.sectionTitle(
                g, font, titleR.withWidth(titleRight - titleR.x()), phaseTitle(), PWPTheme.Text.HEADING, fade);

        if (st == null) return;
        Component votes = Component.translatable("pwp_core.lobby.vote.turnout", st.totalVotes, st.onlinePlayers);
        int vw = Math.min(font.width(votes), infoR.w() / 2);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, votes, vw),
                infoR.x(),
                infoR.y(),
                UIDraw.fade(PWPTheme.Text.MUTED, fade),
                false);
        if (notBlank(st.leadingName)) {
            Component lead = Component.translatable("pwp_core.lobby.vote.leading", st.leadingName);
            Component shown = UIDraw.ellipsize(font, lead, infoR.w() - vw - Spacing.LG);
            UIDraw.text(
                    g,
                    font,
                    shown,
                    infoR.right() - font.width(shown),
                    infoR.y(),
                    UIDraw.fade(PWPTheme.Text.MUTED, fade),
                    false);
        }
    }

    private void renderTeamLabels(GuiGraphics g, Font font, float fade) {
        boolean inv = st != null && "invasion".equalsIgnoreCase(st.mode);
        String[] keys = inv
                ? new String[] {"pwp_core.lobby.team.attack", "pwp_core.lobby.team.defend"}
                : new String[] {"pwp_core.lobby.team.one", "pwp_core.lobby.team.two"};
        int[] colors = {Colors.TEAM_BLUE, Colors.TEAM_RED};
        for (int t = 0; t < 2; t++) {
            Rect r = teamLabelR[t];
            UIDraw.accentBarH(g, r.x(), r.bottom() + 1, r.w(), Spacing.HAIRLINE, UIDraw.fade(colors[t], fade));
            Component label = UIDraw.ellipsize(font, Component.translatable(keys[t]), r.w());
            UIDraw.text(g, font, label, r.x(), r.y(), UIDraw.fade(colors[t], fade), true);
        }
    }

    private void renderIdle(GuiGraphics g, Font font, float fade) {
        UIDraw.sectionTitle(g, font, titleR, phaseTitle(), PWPTheme.Text.HEADING, fade);
        Component hint = st != null && !st.canStartNewMatch && notBlank(st.policyReason)
                ? Component.literal(st.policyReason)
                : st != null && st.minPlayers > 0 && st.onlinePlayers < st.minPlayers
                        // голосование ждёт людей: «Ждём игроков: M/N»
                        ? Component.translatable("pwp_core.lobby.vote.waiting_players", st.onlinePlayers, st.minPlayers)
                        : Component.translatable("pwp_core.lobby.vote.idle_hint");
        UIDraw.emptyState(g, font, bodyR, UIIcon.INFO, Component.translatable("pwp_core.lobby.vote.idle"), hint, fade);
    }

    /** Итоги: три плитки (карта / режим / стороны) из полей result* пакета. */
    private void renderFinished(GuiGraphics g, Font font, long elapsed, float fade) {
        UIDraw.sectionTitle(g, font, titleR, phaseTitle(), PWPTheme.Text.HEADING, fade);
        if (st == null) return;
        int gap = Spacing.GRID_GAP;
        int tileH = Spacing.RESULT_CARD_H;
        int cols = Grid.columnsFor(bodyR.w(), Spacing.RESULT_CARD_MIN_W, gap, 3);
        int rows = (3 + cols - 1) / cols;
        int gridW = Math.min(bodyR.w(), cols * Spacing.RESULT_CARD_MAX_W + (cols - 1) * gap);
        int gh = Grid.height(3, cols, tileH, gap);
        Rect inner = bodyR.align(gridW, Math.min(gh, bodyR.h()), Align.CENTER, Align.CENTER);
        // если три плитки не влезают по высоте — ужимаем высоту плитки, не вылезая за область
        int fitH = rows > 0 ? (inner.h() - (rows - 1) * gap) / rows : tileH;
        Rect[] cells = Grid.cells(inner, 3, cols, Math.min(tileH, fitH), gap);
        Component sides = Component.translatable(
                "pwp_core.lobby.versus", LobbyText.faction(st.resultF1), LobbyText.faction(st.resultF2));
        renderResult(
                g,
                font,
                cells[0],
                "pwp_core.lobby.result.map",
                text(st.resultMap),
                Component.translatable("pwp_core.lobby.result.votes", st.resultMapVotes),
                stagger(elapsed, 0));
        renderResult(
                g,
                font,
                cells[1],
                "pwp_core.lobby.result.mode",
                modeName(st.resultMode),
                Component.translatable("pwp_core.lobby.result.votes", st.resultModeVotes),
                stagger(elapsed, 1));
        renderResult(
                g,
                font,
                cells[2],
                "pwp_core.lobby.result.sides",
                sides,
                Component.literal(st.resultF1Votes + " : " + st.resultF2Votes),
                stagger(elapsed, 2));
    }

    private static float stagger(long elapsed, int i) {
        return UIMath.stagger(elapsed, i, 0, Animations.STAGGER_SLOW * 2, Animations.ENTER);
    }

    private static Component text(String s) {
        return Component.literal(notBlank(s) ? s : "—");
    }

    private static Component modeName(String id) {
        if (!notBlank(id)) return Component.literal("—");
        String key = "pwp_core.mode." + id.toLowerCase();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(id);
    }

    private void renderResult(
            GuiGraphics g, Font font, Rect r, String captionKey, Component value, Component votes, float o) {
        if (o <= 0F || r.h() <= 0) return;
        Rect tile = r.translate(0, Math.round((1F - o) * Animations.ENTER_SLIDE));
        UIDraw.glass(g, tile, o, false);
        UIDraw.accentBar(
                g, tile.x(), tile.y(), tile.h(), Spacing.ACCENT_BAR, UIDraw.fade(Styles.Control.ACCENT_BAR, o));
        Rect in = tile.inset(Spacing.SM + Spacing.ACCENT_BAR, Spacing.XS, Spacing.SM, Spacing.XS);
        int lh = font.lineHeight;
        Rect[] rows = Flow.column(in, Spacing.XXS, Flow.Justify.CENTER, lh, lh, lh);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, Component.translatable(captionKey), in.w()),
                in.x(),
                rows[0].y(),
                UIDraw.fade(PWPTheme.Text.ACCENT, o),
                false);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, value, in.w()),
                in.x(),
                rows[1].y(),
                UIDraw.fade(PWPTheme.Text.HEADING, o),
                true);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, votes, in.w()),
                in.x(),
                rows[2].y(),
                UIDraw.fade(PWPTheme.Text.MUTED, o),
                false);
    }

    /** Идёт/запускается матч: панель с картой, режимом, сторонами и тикетами. */
    private void renderMatch(GuiGraphics g, Font font, float fade) {
        UIDraw.sectionTitle(g, font, titleR, phaseTitle(), PWPTheme.Text.HEADING, fade);
        if (st == null) return;
        boolean starting = st.phase == LobbyStatePacket.PHASE_MATCH_STARTING;
        int lh = font.lineHeight + Spacing.XS;
        List<Component[]> rows = new ArrayList<>();
        rows.add(new Component[] {Component.translatable("pwp_core.lobby.match.map"), text(st.matchMapDisplay)});
        if (notBlank(st.matchModeDisplay)) {
            rows.add(new Component[] {Component.translatable("pwp_core.lobby.match.mode"), text(st.matchModeDisplay)});
        }
        if (notBlank(st.matchBlueFaction)) {
            rows.add(new Component[] {
                Component.translatable("pwp_core.lobby.match.sides"),
                Component.translatable(
                        "pwp_core.lobby.versus",
                        LobbyText.faction(st.matchBlueFaction),
                        LobbyText.faction(st.matchRedFaction))
            });
            if (!starting) {
                rows.add(new Component[] {
                    Component.translatable("pwp_core.lobby.match.tickets"),
                    Component.literal(st.matchBlueTickets + " : " + st.matchRedTickets)
                });
            }
        }
        Component hint = Component.translatable(
                starting ? "pwp_core.lobby.match.hint_starting" : "pwp_core.lobby.match.hint_playing");
        int pad = Spacing.SM;
        int pw = Math.min(bodyR.w(), Spacing.INFO_PANEL_MAX_W);
        var hintLines = UIDraw.wrap(font, hint, pw - 2 * pad, 2);
        int ph = 2 * pad + rows.size() * lh + Spacing.XS + UIDraw.linesHeight(font, hintLines.size());
        Rect panel = bodyR.align(pw, Math.min(ph, bodyR.h()), Align.CENTER, Align.START);
        UIDraw.glass(g, panel, fade, false);
        Rect in = panel.inset(pad);
        int y = in.y();
        for (Component[] row : rows) {
            if (y + font.lineHeight > in.bottom()) break;
            UIDraw.keyValue(g, font, Rect.of(in.x(), y, in.w(), font.lineHeight), row[0], row[1], fade);
            y += lh;
        }
        UIDraw.lines(
                g,
                font,
                hintLines,
                Rect.of(in.x(), y + Spacing.XS, in.w(), in.bottom() - y),
                false,
                UIDraw.fade(PWPTheme.Text.CAPTION, fade),
                false);
    }

    // ================================================================ ввод

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

    private static Font font() {
        return PWPTheme.Fonts.display();
    }
}
