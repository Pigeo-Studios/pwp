package com.pwp.coreclient.gui;

import com.google.gson.JsonObject;
import com.pwp.coreclient.gui.screens.PWPChrome;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.screens.tabs.LeaderTabRenderer;
import com.pwp.coreclient.gui.screens.tabs.LobbyBackend;
import com.pwp.coreclient.gui.screens.tabs.LobbyTabView;
import com.pwp.coreclient.gui.screens.tabs.ProfileTabRenderer;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UITabs;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Статистика ({@code /stats}, {@code /pwp stats}): вкладки «Моя статистика» и «Лидеры» —
 * те же представления, что во вкладках лобби ({@link ProfileTabRenderer},
 * {@link LeaderTabRenderer}): один вид и одна вёрстка на оба места. Данные — DataRequest
 * profile / leaderboard (в тест-стенде — фикстуры через {@link #preview}).
 */
public class StatsScreen extends Screen {
    private static final String[] TAB_KEYS = {"pwp_core.stats.tab.mine", "pwp_core.lobby.tab.leaders"};

    private final ProfileTabRenderer profile = new ProfileTabRenderer();
    private final LeaderTabRenderer leaders = new LeaderTabRenderer();
    private final UILayer layer = new UILayer();
    private UITabs tabs;
    private int tab;
    private boolean leadersRequested;
    /** Профиль запрашивается при первом init — после того, как превью подменило источник. */
    private boolean profileRequested;

    private String uuid;

    public StatsScreen() {
        super(Component.translatable("pwp_core.stats.title"));
        var player = Minecraft.getInstance().player;
        uuid = player != null ? player.getStringUUID() : null;
    }

    /** Тест-стенд UI: без запросов на сервер, данные из фикстур (null — пустые состояния). */
    public static StatsScreen preview(
            Supplier<JsonObject> leaderboard, Function<String, JsonObject> profileData, int tab) {
        StatsScreen s = new StatsScreen();
        s.profile.setBackend(LobbyBackend.PREVIEW);
        s.leaders.setBackend(LobbyBackend.PREVIEW);
        s.profile.setPreviewSource(profileData != null ? profileData : u -> null);
        s.leaders.setPreviewSource(leaderboard != null ? leaderboard : () -> null);
        s.uuid = "00000000-0000-0000-0000-000000000000";
        s.select(tab);
        return s;
    }

    private LobbyTabView view() {
        return tab == 0 ? profile : leaders;
    }

    private void select(int t) {
        tab = Math.max(0, Math.min(1, t));
        if (tabs != null) tabs.select(tab, true);
        view().onShow();
        if (tab == 1 && !leadersRequested) {
            leadersRequested = true;
            leaders.requestData("score", 1);
        }
    }

    @Override
    protected void init() {
        if (!profileRequested && uuid != null) {
            profileRequested = true;
            profile.requestProfile(uuid);
        }
        layer.clear();
        Fit.Density d = PWPChrome.density(width, height);
        int m = PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d) + (d == Fit.Density.COMPACT ? Spacing.XS : Spacing.SM);
        int tabH = d.pick(Spacing.LOBBY_TAB_H, Spacing.LOBBY_TAB_H, Spacing.LOBBY_TAB_H_LARGE);
        tabs = new UITabs(
                0,
                0,
                0,
                List.of(Component.translatable(TAB_KEYS[0]), Component.translatable(TAB_KEYS[1])),
                this::select);
        int tw = Math.min(width - 2 * m, tabs.preferredWidth());
        tabs.setBounds(Rect.of(m, top, width - 2 * m, tabH).align(tw, tabH, Align.CENTER, Align.STRETCH));
        tabs.select(tab, false);
        layer.add(tabs);
        int gap = d == Fit.Density.COMPACT ? Spacing.XS : Spacing.MD;
        int contentTop = top + tabH + gap;
        Rect content = Rect.of(m, contentTop, width - 2 * m, height - PWPChrome.footerHeight(d) - gap - contentTop);
        profile.layout(content);
        leaders.layout(content);
    }

    @Override
    public void tick() {
        super.tick();
        profile.tick();
        leaders.tick();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (Minecraft.getInstance().level != null) UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        else PWPRotatingBackground.render(g, 0, 0, width, height);
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, font, width, d, 1F, Component.translatable("pwp_core.stats.title"));
        layer.render(g, mx, my, pt, width, height);
        view().render(g, mx, my, pt);
        PWPChrome.footer(g, font, width, height, d, Component.translatable("pwp_core.stats.hint"), null);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        if (view().mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        view().mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (view().mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
