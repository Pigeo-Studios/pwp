package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Итоги матча в тактическом стиле: мир за атмосферным затемнением (без сплошной заливки),
 * по центру — флаг победителя в рамке с полосой цвета команды, «<команда> — победа!»
 * (целочисленный масштаб), подпись и стеклянная панель личной статистики плитками
 * ({@link Grid}, колонки по ширине). «Продолжить» становится активной после проявления (как
 * раньше). Конструктор прежний.
 */
public class VictoryScreen extends Screen {
    private static final long BG_MS = 400;
    private static final long CONTENT_DELAY_MS = 400;
    private static final long CONTENT_MS = 800;

    private final String winnerName;
    private final String winnerFaction;
    private final String subText;
    private final boolean isBlueWinner;
    private final long openTime;
    private final int matchKills, matchDeaths;
    private final int matchVehicleKills, matchVehiclesDestroyed, matchAirVehiclesDestroyed;
    private final int matchCaptures, matchRevives, matchHeadshots, matchScore;
    private final int matchDurationSec;
    private final UILayer layer = new UILayer();
    private UIButton continueButton;
    private Rect flagR = Rect.of(0, 0, 0, 0), titleR = flagR, subR = flagR, statsR = flagR;
    private int titleScale = 2;

    public VictoryScreen(
            String winnerName,
            String winnerFaction,
            String subText,
            boolean isBlueWinner,
            int matchKills,
            int matchDeaths,
            int matchVehicleKills,
            int matchVehiclesDestroyed,
            int matchAirVehiclesDestroyed,
            int matchCaptures,
            int matchRevives,
            int matchHeadshots,
            int matchScore,
            int matchDurationSec) {
        super(Component.translatable("gui.pwpwarfare.victory.title"));
        this.winnerName = winnerName;
        this.winnerFaction = winnerFaction;
        this.subText = subText;
        this.isBlueWinner = isBlueWinner;
        this.matchKills = matchKills;
        this.matchDeaths = matchDeaths;
        this.matchVehicleKills = matchVehicleKills;
        this.matchVehiclesDestroyed = matchVehiclesDestroyed;
        this.matchAirVehiclesDestroyed = matchAirVehiclesDestroyed;
        this.matchCaptures = matchCaptures;
        this.matchRevives = matchRevives;
        this.matchHeadshots = matchHeadshots;
        this.matchScore = matchScore;
        this.matchDurationSec = matchDurationSec;
        this.openTime = System.currentTimeMillis();
    }

    private Component[][] stats() {
        String kd = String.format("%.2f", matchDeaths > 0 ? matchKills / (double) matchDeaths : (double) matchKills);
        return new Component[][] {
            stat("kills", String.valueOf(matchKills)),
            stat("deaths", String.valueOf(matchDeaths)),
            stat("kd", kd),
            stat("revives", String.valueOf(matchRevives)),
            stat("score", String.format("%,d", matchScore)),
            stat("time", duration(matchDurationSec)),
            stat("vehicle_kills", String.valueOf(matchVehicleKills)),
            stat("vehicles_destroyed", String.valueOf(matchVehiclesDestroyed + matchAirVehiclesDestroyed)),
            stat("captures", String.valueOf(matchCaptures)),
            // хедшоты не считаются (всегда 0) — вместо них ассисты и награды из RewardPackets
            stat("assists", pending(ClientData.lastMatchAssists)),
            stat("xp", pending(ClientData.lastMatchXp)),
            stat("coins", pending(ClientData.lastMatchCoins)),
        };
    }

    /** «…» пока сервер не прислал награды. */
    private static String pending(long v) {
        return v < 0 ? "…" : String.format("%,d", v);
    }

    private static Component[] stat(String key, String value) {
        return new Component[] {Component.translatable("gui.pwpwarfare.victory.stat." + key), Component.literal(value)};
    }

    @Override
    protected void init() {
        layer.clear();
        int m = Spacing.SM;
        Rect area = Rect.of(m, m, width - 2 * m, height - 2 * m);
        int lh = font.lineHeight;
        Component win = winText();
        titleScale = font.width(win) * 2 <= area.w() && height >= 240 ? 2 : 1;
        int flagH = height < 240 ? Spacing.XL : Spacing.HUGE;
        int flagW = flagH * 16 / 9;
        int statCols = Grid.columnsFor(
                Math.min(area.w(), Spacing.VICTORY_PANEL_MAX_W) - 2 * Spacing.SM,
                Spacing.VICTORY_STAT_MIN_W,
                Spacing.XS,
                5);
        int statRows = (stats().length + statCols - 1) / statCols;
        int statsH = WDraw.headerHeight(height)
                + 2 * Spacing.SM
                + Grid.height(stats().length, statCols, Spacing.VICTORY_STAT_H, Spacing.XS);
        int btnH = PWPTheme.Spacing.BUTTON_HEIGHT;
        Rect[] col = Flow.column(
                area,
                0,
                Flow.Justify.CENTER,
                flagH,
                Spacing.SM,
                (lh - 1) * titleScale,
                Spacing.XS,
                lh,
                Spacing.MD,
                statsH,
                Spacing.MD,
                btnH);
        flagR = col[0].align(flagW, col[0].h(), Align.CENTER, Align.STRETCH);
        titleR = col[2];
        subR = col[4];
        statsR = col[6].align(Math.min(area.w(), Spacing.VICTORY_PANEL_MAX_W), col[6].h(), Align.CENTER, Align.STRETCH);
        continueButton =
                layer.add(new UIButton(Component.translatable("gui.pwpwarfare.victory.continue"), this::onClose)
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.PLAY));
        int bw = Math.min(area.w(), Math.max(Spacing.BUTTON_WIDTH, continueButton.preferredWidth()));
        continueButton.setBounds(col[8].align(bw, btnH, Align.CENTER, Align.STRETCH));
        continueButton.setEnabled(false);
        if (statRows == 0) statsR = Rect.of(0, 0, 0, 0);
    }

    private Component winText() {
        return Component.translatable("gui.pwpwarfare.victory.winner", winnerName);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long elapsed = System.currentTimeMillis() - openTime;
        float bg = UIMath.stagger(elapsed, 0, 0, 0, BG_MS);
        float content = UIMath.stagger(elapsed, 0, CONTENT_DELAY_MS, 0, CONTENT_MS);
        if (bg > 0F) {
            UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
            // полоса цвета команды за заголовком: от центра в стороны гаснет
            int team = isBlueWinner ? Colors.TEAM_BLUE : Colors.TEAM_RED;
            Rect band = Rect.of(0, flagR.y() - Spacing.SM, width, titleR.bottom() - flagR.y() + 2 * Spacing.SM);
            int c = UIDraw.fade(Colors.withAlpha(team, 0x55), bg);
            UIDraw.gradientH(g, band.left(width / 2), Colors.withAlpha(team, 0), c);
            UIDraw.gradientH(g, band.rightStrip(width - width / 2), c, Colors.withAlpha(team, 0));
        }
        if (content > 0.01F) {
            int slide = Math.round((1F - content) * PWPTheme.Animations.ENTER_SLIDE);
            renderFlag(g, flagR.translate(0, slide), content);
            Component win = UIDraw.ellipsize(font, winText(), titleR.w() / titleScale);
            int tw = font.width(win) * titleScale;
            UIDraw.textScaled(
                    g,
                    font,
                    win,
                    titleR.centerX() - tw / 2,
                    titleR.y() + slide,
                    titleScale,
                    UIDraw.fade(PWPTheme.Text.HEADING, content),
                    true);
            Component sub = UIDraw.ellipsize(font, Component.literal(subText != null ? subText : ""), subR.w());
            UIDraw.text(
                    g,
                    font,
                    sub,
                    subR.centerX() - font.width(sub) / 2,
                    subR.y() + slide,
                    UIDraw.fade(PWPTheme.Text.MUTED, content),
                    false);
            renderStats(g, statsR.translate(0, slide), content);
        }
        if (content >= 1F && !continueButton.isEnabled()) continueButton.setEnabled(true);
        continueButton.setAppearance(content, 0, 0);
        layer.render(g, mx, my, pt, width, height);
    }

    private void renderFlag(GuiGraphics g, Rect r, float o) {
        int team = isBlueWinner ? Colors.TEAM_BLUE : Colors.TEAM_RED;
        UIDraw.shadow(g, r, o);
        ResourceLocation tex = flag(winnerFaction);
        if (tex != null) UIDraw.image(g, tex, r, UIDraw.fade(0xFFFFFFFF, o));
        else UIDraw.rect(g, r, UIDraw.fade(team, o));
        UIDraw.outline(g, r.outset(1), UIDraw.fade(PWPTheme.Styles.Glass.FRAME_TINT, o));
        UIDraw.rect(g, Rect.of(r.x(), r.bottom() + Spacing.XXS, r.w(), Spacing.ACCENT_BAR), UIDraw.fade(team, o));
    }

    private void renderStats(GuiGraphics g, Rect r, float o) {
        if (r.h() <= 0) return;
        Rect c = WDraw.panel(
                g,
                font,
                r,
                WDraw.headerHeight(height),
                WarfareIcon.TICKET,
                Component.translatable("gui.pwpwarfare.victory.stats"),
                null,
                o);
        Component[][] s = stats();
        int cols = Grid.columnsFor(c.w(), Spacing.VICTORY_STAT_MIN_W, Spacing.XS, 5);
        Rect[] cells = Grid.cells(c, s.length, cols, Spacing.VICTORY_STAT_H, Spacing.XS);
        for (int i = 0; i < s.length; i++) {
            Rect cell = cells[i];
            if (cell.bottom() > c.bottom() + 1) break;
            WDraw.well(g, cell, o);
            Rect in = cell.inset(Spacing.XS, Spacing.XXS);
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, s[i][0], in.w()),
                    in.x(),
                    in.y() + 1,
                    UIDraw.fade(PWPTheme.Text.CAPTION, o),
                    false);
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, s[i][1], in.w()),
                    in.x(),
                    in.bottom() - font.lineHeight + 1,
                    UIDraw.fade(PWPTheme.Text.ACCENT, o),
                    true);
        }
    }

    private static String duration(int secs) {
        if (secs < 60) return secs + "s";
        int m = secs / 60, s = secs % 60;
        if (m >= 60) return (m / 60) + "h " + (m % 60) + "m";
        return m + "m " + s + "s";
    }

    private static ResourceLocation flag(String faction) {
        if (faction == null || faction.isBlank() || faction.equals("none")) return null;
        return switch (faction.toLowerCase()) {
            case "ukraine", "russia", "usa", "nato", "bluefor", "redfor", "insurgency", "pmc" -> new ResourceLocation(
                    "pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png");
            default -> null;
        };
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
