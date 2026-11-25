package com.pwp.coreclient.gui.screens.tabs;

import com.google.gson.JsonObject;
import com.pwp.coreclient.PlayerData;
import com.pwp.coreclient.PlayerData.CachedProfile;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIAnim;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Вкладка «Профиль»: шапка (ник + уровень), секции статистики (бой / матчи / техника /
 * поддержка) стеклянными панелями в 1–2 колонки (раскладка «кирпичиком»), строки
 * «ключ … значение» по 1–2 в ряд по ширине. Контент прокручивается колесом. Пустые
 * состояния: загрузка, профиль недоступен (нет ответа {@link #TIMEOUT_MS}). Данные — ответ
 * DataRequest "profile" из {@link PlayerData}.
 */
public class ProfileTabRenderer implements LobbyTabView {

    public enum State {
        LOADING,
        LOADED,
        UNAVAILABLE
    }

    /** Нет ответа дольше — показываем «профиль недоступен». */
    private static final long TIMEOUT_MS = 8000;

    private LobbyBackend backend = LobbyBackend.NETWORK;
    private Function<String, JsonObject> source = uuid -> {
        CachedProfile cp = PlayerData.get(java.util.UUID.fromString(uuid));
        return cp != null ? cp.data : null;
    };

    private State state = State.LOADING;
    private JsonObject profileData;
    private String currentUuid;
    private long requestedAt = System.currentTimeMillis();
    private long shownAt = System.currentTimeMillis();
    private Rect area = Rect.of(0, 0, 0, 0);
    private double scrollTarget;
    private final UIAnim scroll = new UIAnim(0F, 120, UIAnim.EASE_OUT_CUBIC);
    private int contentHeight;

    public void setBackend(LobbyBackend backend) {
        this.backend = backend;
    }

    /** Тест-стенд: профиль из фикстуры (null — «недоступен» после таймаута). */
    public void setPreviewSource(Function<String, JsonObject> source) {
        this.source = source;
    }

    public void requestProfile(String uuid) {
        currentUuid = uuid;
        state = State.LOADING;
        requestedAt = System.currentTimeMillis();
        backend.requestProfile();
    }

    public void tick() {
        if (state != State.LOADING || currentUuid == null) return;
        JsonObject data = source.apply(currentUuid);
        if (data != null) {
            profileData = data;
            state = State.LOADED;
        } else if (System.currentTimeMillis() - requestedAt > TIMEOUT_MS) {
            state = State.UNAVAILABLE;
        }
    }

    public State state() {
        return state;
    }

    @Override
    public void layout(Rect area) {
        this.area = area;
    }

    @Override
    public void onShow() {
        shownAt = System.currentTimeMillis();
    }

    // ================================================================ данные

    private record Section(String titleKey, List<Component[]> rows) {}

    private List<Section> sections() {
        JsonObject st = profileData.has("stats") ? profileData.getAsJsonObject("stats") : profileData;
        int kills = i(st, "kills"), deaths = i(st, "deaths"), matches = i(st, "matchesPlayed"), wins = i(st, "wins");
        List<Section> out = new ArrayList<>();
        out.add(new Section(
                "pwp_core.profile.section.combat",
                rows(
                        "kills", "" + kills,
                        "deaths", "" + deaths,
                        "kd", String.format("%.2f", deaths > 0 ? kills / (double) deaths : (double) kills),
                        "revives", "" + i(st, "revives"),
                        "vehicle_kills", "" + i(st, "vehicleKills"),
                        "captures", "" + i(st, "captures"),
                        "best_streak", "" + i(st, "bestKillStreak"),
                        "team_kills", "" + i(st, "teamKills"))));
        out.add(new Section(
                "pwp_core.profile.section.matches",
                rows(
                        "played", "" + matches,
                        "wins", "" + wins,
                        "losses", "" + i(st, "losses"),
                        "winrate", matches > 0 ? String.format("%.0f%%", wins * 100f / matches) : "0%",
                        "playtime", playtime(i(st, "playtimeSeconds")),
                        "kills_per_match", matches > 0 ? String.format("%.1f", kills / (double) matches) : "0",
                        "best_win_streak", "" + i(st, "bestWinStreak"),
                        "win_streak", "" + i(st, "currentWinStreak"))));
        out.add(new Section(
                "pwp_core.profile.section.vehicles",
                rows(
                        "vehicles_destroyed", "" + i(st, "vehiclesDestroyed"),
                        "air_destroyed", "" + i(st, "airVehiclesDestroyed"))));
        double longest = st.has("longestKill") ? st.get("longestKill").getAsDouble() : 0;
        out.add(new Section(
                "pwp_core.profile.section.support",
                rows(
                        "damage", String.format("%,d", i(st, "damageDealt")),
                        "healing", String.format("%,d", i(st, "healingDone")),
                        "supplies", "" + i(st, "suppliesDelivered"),
                        "longest_kill",
                                Component.translatable("pwp_core.profile.meters", Math.round(longest))
                                        .getString())));
        return out;
    }

    private static List<Component[]> rows(String... kv) {
        List<Component[]> out = new ArrayList<>();
        for (int k = 0; k + 1 < kv.length; k += 2) {
            out.add(new Component[] {
                Component.translatable("pwp_core.profile.stat." + kv[k]), Component.literal(kv[k + 1])
            });
        }
        return out;
    }

    private static String playtime(int s) {
        int h = s / 3600;
        if (h >= 24)
            return Component.translatable("pwp_core.profile.days_hours", h / 24, h % 24)
                    .getString();
        return Component.translatable("pwp_core.profile.hours_minutes", h, s % 3600 / 60)
                .getString();
    }

    private static int i(JsonObject o, String k) {
        return o.has(k) ? o.get(k).getAsInt() : 0;
    }

    private static String str(JsonObject o, String k, String d) {
        return o != null && o.has(k) ? o.get(k).getAsString() : d;
    }

    // ================================================================ рисование

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font font = PWPTheme.Fonts.display();
        float fade = UIMath.stagger(System.currentTimeMillis() - shownAt, 0, 0, 0, Animations.ENTER);
        if (state == State.LOADING) {
            UIDraw.emptyState(
                    g, font, area, UIIcon.PROFILE, Component.translatable("pwp_core.gui.loading_profile"), null, fade);
            return;
        }
        if (state == State.UNAVAILABLE || profileData == null) {
            UIDraw.emptyState(
                    g,
                    font,
                    area,
                    UIIcon.PROFILE,
                    Component.translatable("pwp_core.profile.unavailable"),
                    Component.translatable("pwp_core.profile.unavailable_hint"),
                    fade);
            return;
        }
        JsonObject player = profileData.has("player") ? profileData.getAsJsonObject("player") : null;
        String name = str(player, "nickname", "—");
        int level = i(profileData, "level");

        // шапка: ник слева, «УР. N» справа
        int lh = font.lineHeight;
        Rect head = area.top(lh);
        Component lvl = Component.translatable("pwp_core.profile.level", Math.max(1, level));
        int lw = font.width(lvl);
        UIDraw.text(g, font, lvl, head.right() - lw, head.y(), UIDraw.fade(PWPTheme.Text.ACCENT, fade), true);
        UIDraw.sectionTitle(
                g,
                font,
                head.withWidth(head.w() - lw - Spacing.SM),
                Component.literal(name),
                PWPTheme.Text.HEADING,
                fade);

        Rect view = area.below(lh, Spacing.SM);
        List<Section> sections = sections();
        int gap = Spacing.GRID_GAP;
        int cols =
                Grid.columnsFor(view.w() - Spacing.SCROLLBAR_WIDTH - Spacing.XS, Spacing.PROFILE_PANEL_MIN_W, gap, 2);
        int innerW = view.w() - (contentHeight > view.h() ? Spacing.SCROLLBAR_WIDTH + Spacing.XS : 0);
        int[] widths = new int[cols];
        java.util.Arrays.fill(widths, -1);
        int[][] spans = Flow.spans(innerW, gap, Flow.Justify.START, widths);

        int off = Math.round(scroll.to((float) UIMath.clampScroll(scrollTarget, contentHeight, view.h()))
                .value());
        g.enableScissor(view.x(), view.y(), view.right(), view.bottom());
        int[] colY = new int[cols];
        int rowH = lh + Spacing.XS;
        int pad = Spacing.SM;
        for (int si = 0; si < sections.size(); si++) {
            Section s = sections.get(si);
            int c = 0;
            for (int k = 1; k < cols; k++) if (colY[k] < colY[c]) c = k; // в самую короткую колонку
            int pw = spans[c][1];
            int perRow = pw - 2 * pad >= 2 * Spacing.PROFILE_PAIR_MIN_W ? 2 : 1;
            int lines = (s.rows().size() + perRow - 1) / perRow;
            int ph = Spacing.PANEL_HEADER_COMPACT + pad + lines * rowH;
            Rect panel = Rect.of(view.x() + spans[c][0], view.y() + colY[c] - off, pw, ph);
            colY[c] += ph + gap;
            if (panel.bottom() < view.y() || panel.y() > view.bottom()) continue;
            float o = fade
                    * UIMath.stagger(
                            System.currentTimeMillis() - shownAt, si, 0, Animations.STAGGER_NORMAL, Animations.ENTER);
            UIDraw.glass(g, panel, o, false);
            Rect header = panel.top(Spacing.PANEL_HEADER_COMPACT)
                    .inset(Spacing.HAIRLINE, Spacing.HAIRLINE, Spacing.HAIRLINE, 0);
            UIDraw.header(g, font, header, null, Component.translatable(s.titleKey()), PWPTheme.Text.ACCENT, o);
            Rect body = panel.below(Spacing.PANEL_HEADER_COMPACT, 0).inset(pad, pad / 2, pad, 0);
            int[] pw2 = perRow == 2 ? new int[] {-1, -1} : new int[] {-1};
            int[][] cells = Flow.spans(body.w(), Spacing.MD, Flow.Justify.START, pw2);
            for (int r = 0; r < s.rows().size(); r++) {
                int line = r / perRow, cell = r % perRow;
                Rect rr = Rect.of(body.x() + cells[cell][0], body.y() + line * rowH, cells[cell][1], lh);
                if (line % 2 == 1 && cell == 0) {
                    UIDraw.rect(
                            g,
                            Rect.of(body.x() - Spacing.XXS, rr.y() - 1, body.w() + Spacing.XS, rowH),
                            UIDraw.fade(Styles.Control.BG_DISABLED, o));
                }
                UIDraw.keyValue(g, font, rr, s.rows().get(r)[0], s.rows().get(r)[1], o);
            }
        }
        g.disableScissor();
        int total = 0;
        for (int y : colY) total = Math.max(total, y - gap);
        contentHeight = Math.max(0, total);
        if (contentHeight > view.h()) {
            int sbw = Spacing.SCROLLBAR_WIDTH;
            int tx = view.right() - sbw;
            UIDraw.rect(g, Rect.of(tx, view.y(), sbw, view.h()), Styles.Loader.TRACK);
            int thumb = UIMath.thumbHeight(contentHeight, view.h(), Spacing.LG);
            int ty = view.y() + UIMath.thumbOffset(scroll.value(), contentHeight, view.h(), thumb);
            UIDraw.rect(g, Rect.of(tx, ty, sbw, thumb), Styles.ScrollBar.THUMB);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!area.contains(mx, my) || state != State.LOADED) return false;
        scrollTarget = UIMath.clampScroll(scrollTarget - delta * Spacing.XXL, contentHeight, area.h());
        return true;
    }
}
