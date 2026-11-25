package com.pwp.coreclient.gui.screens.tabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.network.ClientResponseCache;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.UIScrollList;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Вкладка «Лидеры»: карточка лидера (кроме тесных экранов), переключатель категории
 * (кнопки-«пилюли», если влезают в строку, иначе «‹ КАТЕГОРИЯ ›»), таблица с прокруткой.
 * Колонки фиксированной ширины (#, значение, K/D, Win%) + гибкое имя; на узком экране
 * K/D и Win% скрываются. Своя строка подсвечена. Данные — ответ DataRequest "leaderboard".
 */
public class LeaderTabRenderer implements LobbyTabView {

    public enum State {
        LOADING,
        LOADED
    }

    private static final String[] CAT_KEYS = {
        "score", "kills", "kd", "wins", "winrate", "captures", "damage", "healing", "playtime"
    };

    static final class Entry implements Comparable<Entry> {
        final String name, uuid;
        final int kills, deaths, wins, losses, captures, revives, vehicleKills;
        final int damage, healing, score, playtimeSec, kd100, wr100;
        int catVal;

        Entry(JsonObject json) {
            JsonObject p = json.has("player") ? json.getAsJsonObject("player") : json;
            JsonObject s = json.has("stats") ? json.getAsJsonObject("stats") : json;
            name = p.has("nickname") ? p.get("nickname").getAsString() : "—";
            uuid = p.has("uuid") ? p.get("uuid").getAsString() : "";
            kills = getInt(s, "kills");
            deaths = getInt(s, "deaths");
            wins = getInt(s, "wins");
            losses = getInt(s, "losses");
            captures = getInt(s, "captures");
            revives = getInt(s, "revives");
            vehicleKills = getInt(s, "vehicleKills");
            damage = getInt(s, "damageDealt");
            healing = getInt(s, "healingDone");
            playtimeSec = getInt(s, "playtimeSeconds");
            score = kills * 100 + vehicleKills * 150 + captures * 200 + revives * 75 + healing;
            kd100 = deaths > 0 ? (int) (kills * 1000f / deaths) : (kills * 1000);
            wr100 = (wins + losses) > 0 ? (int) (wins * 10000f / (wins + losses)) : 0;
        }

        @Override
        public int compareTo(Entry o) {
            return Integer.compare(o.catVal, this.catVal);
        }

        private static int getInt(JsonObject o, String k) {
            return o.has(k) ? o.get(k).getAsInt() : 0;
        }

        String kd() {
            return String.format("%.2f", deaths > 0 ? kills / (double) deaths : (double) kills);
        }

        String winrate() {
            return (wins + losses) > 0 ? String.format("%.0f%%", wins * 100f / (wins + losses)) : "0%";
        }
    }

    private LobbyBackend backend = LobbyBackend.NETWORK;
    /** Источник ответа: в игре — кэш ответа сервера; в тест-стенде — фикстура. */
    private Supplier<JsonObject> source = () -> ClientResponseCache.leaderboardData;

    private Runnable consumeSource = () -> ClientResponseCache.leaderboardData = null;

    private State state = State.LOADING;
    private final List<Entry> entries = new ArrayList<>();
    private int sortCat = 0;
    private long shownAt = System.currentTimeMillis();

    private final UILayer layer = new UILayer();
    private final List<UIButton> pills = new ArrayList<>();
    private UIButton catLabel;
    private final UIScrollList<Entry> table = new UIScrollList<>(0, 0, 0, 0, Spacing.LEADER_ROW_H, this::renderRow);
    private Rect area = Rect.of(0, 0, 0, 0);
    private Rect topR = area, headerR = area, bodyR = area;
    private boolean wideColumns;

    public void setBackend(LobbyBackend backend) {
        this.backend = backend;
    }

    /** Тест-стенд: данные из фикстуры (null — «пусто»), без запросов на сервер. */
    public void setPreviewSource(Supplier<JsonObject> source) {
        this.source = source;
        this.consumeSource = () -> {};
    }

    public void requestData(String orderBy, int page) {
        state = State.LOADING;
        entries.clear();
        table.setItems(entries);
        backend.requestLeaderboard(orderBy, page);
    }

    public void tick() {
        if (state != State.LOADING) return;
        JsonObject data = source.get();
        if (data == null) return;
        JsonArray players = data.has("players") ? data.getAsJsonArray("players") : new JsonArray();
        entries.clear();
        for (JsonElement e : players) entries.add(new Entry(e.getAsJsonObject()));
        state = State.LOADED;
        consumeSource.run();
        sortBy(sortCat);
    }

    private void sortBy(int cat) {
        sortCat = cat;
        for (Entry e : entries) {
            e.catVal = switch (cat) {
                case 1 -> e.kills;
                case 2 -> e.kd100;
                case 3 -> e.wins;
                case 4 -> e.wr100;
                case 5 -> e.captures;
                case 6 -> e.damage;
                case 7 -> e.healing;
                case 8 -> e.playtimeSec;
                default -> e.score;
            };
        }
        Collections.sort(entries);
        table.setItems(entries);
        for (int i = 0; i < pills.size(); i++) pills.get(i).selected(i == cat);
        if (catLabel != null) catLabel.label(catName(cat));
    }

    private static Component catName(int cat) {
        return Component.translatable("pwp_core.lobby.leaders.cat." + CAT_KEYS[cat]);
    }

    // ================================================================ раскладка

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
        pills.clear();
        catLabel = null;
        Font font = PWPTheme.Fonts.display();
        boolean compact = area.h() < Spacing.LOBBY_COMPACT_H;
        int topH = compact ? 0 : Spacing.LEADER_TOP_H;
        int pillH = Spacing.LEADER_PILL_H;
        Rect[] col = Flow.column(
                area,
                0,
                Flow.Justify.START,
                topH,
                compact ? 0 : Spacing.SM,
                pillH,
                Spacing.SM,
                font.lineHeight,
                Spacing.XXS,
                Flow.flex(1));
        topR = col[0];
        Rect pillRow = col[2];
        headerR = col[4];
        bodyR = col[6];

        // пилюли в одну строку, если влезают; иначе «‹ категория ›»
        int[] widths = new int[CAT_KEYS.length];
        for (int i = 0; i < CAT_KEYS.length; i++) widths[i] = font.width(catName(i)) + 2 * Spacing.SM;
        if (Flow.naturalLength(Spacing.XXS, widths) <= pillRow.w()) {
            Rect[] cells = Flow.row(pillRow, Spacing.XXS, Flow.Justify.CENTER, widths);
            for (int i = 0; i < CAT_KEYS.length; i++) {
                final int cat = i;
                UIButton b = new UIButton(catName(i), () -> sortBy(cat))
                        .style(UIButton.Style.GHOST)
                        .selected(i == sortCat);
                b.setBounds(cells[i]);
                pills.add(layer.add(b));
            }
        } else {
            int arrow = pillH + Spacing.XS;
            Rect[] cells = Flow.row(pillRow, Spacing.XXS, Flow.Justify.START, arrow, Flow.flex(1), arrow);
            UIButton prev = new UIButton(
                            Component.literal("‹"), () -> sortBy((sortCat + CAT_KEYS.length - 1) % CAT_KEYS.length))
                    .style(UIButton.Style.DARK);
            UIButton next = new UIButton(Component.literal("›"), () -> sortBy((sortCat + 1) % CAT_KEYS.length))
                    .style(UIButton.Style.DARK);
            catLabel = new UIButton(catName(sortCat), () -> sortBy((sortCat + 1) % CAT_KEYS.length))
                    .style(UIButton.Style.GHOST)
                    .selected(true);
            prev.setBounds(cells[0]);
            catLabel.setBounds(cells[1]);
            next.setBounds(cells[2]);
            layer.add(prev);
            layer.add(catLabel);
            layer.add(next);
        }
        table.setBounds(bodyR);
        layer.add(table);
        wideColumns = bodyR.w() >= Spacing.LEADER_WIDE_MIN_W;
    }

    /** Колонки строки: #, имя (гибкое), значение, [K/D, Win%]. */
    private Rect[] columns(Rect row) {
        Font font = PWPTheme.Fonts.display();
        int rank = font.width("000") + Spacing.SM;
        int val = font.width("00 000 000");
        int small = font.width("00.00") + Spacing.SM;
        Rect in = row.inset(Spacing.XS, 0);
        return wideColumns
                ? Flow.row(in, Spacing.SM, Flow.Justify.START, rank, Flow.flex(1), val, small, small)
                : Flow.row(in, Spacing.SM, Flow.Justify.START, rank, Flow.flex(1), val);
    }

    // ================================================================ рисование

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font font = PWPTheme.Fonts.display();
        float fade = UIMath.stagger(System.currentTimeMillis() - shownAt, 0, 0, 0, Animations.ENTER);
        if (state == State.LOADING) {
            UIDraw.emptyState(g, font, area, UIIcon.INFO, Component.translatable("pwp_core.ui.loading"), null, fade);
            return;
        }
        if (entries.isEmpty()) {
            UIDraw.emptyState(
                    g,
                    font,
                    area,
                    UIIcon.PROFILE,
                    Component.translatable("pwp_core.lobby.leaders.empty"),
                    Component.translatable("pwp_core.lobby.leaders.empty_hint"),
                    fade);
            return;
        }
        if (topR.h() > 0) renderTop(g, font, entries.get(0), fade);
        renderHeader(g, font, fade);
        var win = Minecraft.getInstance().getWindow();
        layer.render(g, mx, my, pt, win.getGuiScaledWidth(), win.getGuiScaledHeight());
    }

    private void renderTop(GuiGraphics g, Font font, Entry top, float fade) {
        UIDraw.glass(g, topR, fade, false);
        UIDraw.accentBar(
                g,
                topR.x(),
                topR.y(),
                topR.h(),
                Spacing.ACCENT_BAR_SELECTED,
                UIDraw.fade(Styles.Control.ACCENT_BAR, fade));
        Rect in = topR.inset(Spacing.SM + Spacing.ACCENT_BAR_SELECTED, Spacing.XS, Spacing.SM, Spacing.XS);
        int lh = font.lineHeight;
        Rect[] rows = Flow.column(in, Spacing.XXS, Flow.Justify.CENTER, lh, lh, lh);
        Component caption = Component.translatable("pwp_core.lobby.leaders.top", catName(sortCat));
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, caption, in.w()),
                in.x(),
                rows[0].y(),
                UIDraw.fade(PWPTheme.Text.ACCENT, fade),
                false);
        Component value = Component.literal(formatCatValue(sortCat, top));
        int vw = font.width(value);
        UIDraw.text(g, font, value, in.right() - vw, rows[1].y(), UIDraw.fade(PWPTheme.Text.ACCENT, fade), true);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, Component.literal(top.name), in.w() - vw - Spacing.SM),
                in.x(),
                rows[1].y(),
                UIDraw.fade(PWPTheme.Text.HEADING, fade),
                true);
        Component detail = Component.translatable("pwp_core.lobby.leaders.detail", top.kills, top.wins, top.winrate());
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, detail, in.w()),
                in.x(),
                rows[2].y(),
                UIDraw.fade(PWPTheme.Text.MUTED, fade),
                false);
    }

    private void renderHeader(GuiGraphics g, Font font, float fade) {
        Rect[] c = columns(headerR.withWidth(bodyR.w() - Spacing.SCROLLBAR_WIDTH - Spacing.XXS));
        int color = UIDraw.fade(PWPTheme.Text.CAPTION, fade);
        UIDraw.text(g, font, Component.literal("#"), c[0].x(), headerR.y(), color, false);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, Component.translatable("pwp_core.lobby.leaders.player"), c[1].w()),
                c[1].x(),
                headerR.y(),
                color,
                false);
        rightText(g, font, UIDraw.ellipsize(font, catName(sortCat), c[2].w()), c[2], headerR.y(), color);
        if (c.length > 3) {
            rightText(g, font, Component.literal("K/D"), c[3], headerR.y(), color);
            rightText(g, font, Component.literal("Win%"), c[4], headerR.y(), color);
        }
        UIDraw.hline(
                g, headerR.x(), headerR.right(), headerR.bottom() + 1, UIDraw.fade(Styles.Glass.HEADER_DIVIDER, fade));
    }

    private static void rightText(GuiGraphics g, Font font, Component t, Rect col, int y, int color) {
        UIDraw.text(g, font, t, col.right() - font.width(t), y, color, false);
    }

    private void renderRow(GuiGraphics g, Entry e, int x, int y, int w, int h, boolean hovered, boolean selected) {
        Font font = PWPTheme.Fonts.display();
        int index = entries.indexOf(e);
        Rect row = Rect.of(x, y, w, h);
        var player = Minecraft.getInstance().player;
        boolean me = player != null && !e.uuid.isEmpty() && e.uuid.equals(player.getStringUUID());
        if (me) {
            UIDraw.rect(g, row, Styles.Control.BG_SELECTED);
            UIDraw.accentBar(g, x, y, h, Spacing.ACCENT_BAR, Styles.Control.ACCENT_BAR);
        } else if (hovered) {
            UIDraw.rect(g, row, Styles.Control.BG_HOVER);
        } else if (index % 2 == 0) {
            UIDraw.rect(g, row, Styles.Control.BG_DISABLED);
        }
        Rect[] c = columns(row);
        int ty = y + (h - font.lineHeight) / 2 + 1;
        int rank = index + 1;
        int rc =
                switch (rank) {
                    case 1 -> PWPTheme.Colors.RARITY_LEGENDARY;
                    case 2 -> PWPTheme.Text.HEADING;
                    case 3 -> PWPTheme.Colors.ACCENT_SOFT;
                    default -> PWPTheme.Text.CAPTION;
                };
        UIDraw.text(g, font, Component.literal(String.valueOf(rank)), c[0].x(), ty, rc, false);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, Component.literal(e.name), c[1].w()),
                c[1].x(),
                ty,
                me ? PWPTheme.Text.ACCENT : PWPTheme.Text.BODY,
                false);
        rightText(
                g,
                font,
                UIDraw.ellipsize(font, Component.literal(formatCatValue(sortCat, e)), c[2].w()),
                c[2],
                ty,
                PWPTheme.Text.BODY);
        if (c.length > 3) {
            rightText(g, font, Component.literal(e.kd()), c[3], ty, Colors.TEXT_SECONDARY);
            rightText(g, font, Component.literal(e.winrate()), c[4], ty, Colors.TEXT_SECONDARY);
        }
    }

    private static String formatCatValue(int cat, Entry e) {
        return switch (cat) {
            case 1 -> String.format("%,d", e.kills);
            case 2 -> e.kd();
            case 3 -> String.valueOf(e.wins);
            case 4 -> e.winrate();
            case 5 -> String.valueOf(e.captures);
            case 6 -> String.format("%,d", e.damage);
            case 7 -> String.format("%,d", e.healing);
            case 8 -> Component.translatable("pwp_core.lobby.leaders.hours", e.playtimeSec / 3600)
                    .getString();
            default -> String.format("%,d", e.score);
        };
    }

    // ================================================================ ввод

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return state == State.LOADED && !entries.isEmpty() && layer.mouseClicked(mx, my, button);
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
