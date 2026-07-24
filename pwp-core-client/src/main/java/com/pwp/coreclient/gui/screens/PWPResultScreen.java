package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.components.PWPLayout;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PWPResultScreen extends Screen {

    private final boolean isWin;
    private final String winnerName;
    private final String matchSummary;
    private final int matchKills, matchDeaths, matchScore, matchDurationSec;

    public PWPResultScreen(boolean isWin, String winnerName, String matchSummary,
                           int matchKills, int matchDeaths, int matchScore, int matchDurationSec) {
        super(Component.literal(isWin ? "ПОБЕДА" : "ПОРАЖЕНИЕ"));
        this.isWin = isWin;
        this.winnerName = winnerName;
        this.matchSummary = matchSummary;
        this.matchKills = matchKills;
        this.matchDeaths = matchDeaths;
        this.matchScore = matchScore;
        this.matchDurationSec = matchDurationSec;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(new PWPButton(
            width / 2 - 70, (int) (height * 0.72f), 140, 28,
            Component.literal("Продолжить"),
            btn -> onClose(),
            PWPButton.Style.ACCENT
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        int accentColor = isWin ? PWPTheme.Colors.SUCCESS : PWPTheme.Colors.DANGER;
        int cx = width / 2;
        int iconY = (int) (height * 0.25f);

        var font = minecraft.font;
        var pose = gui.pose();

        pose.pushPose();
        pose.translate(cx, iconY, 0);
        pose.scale(3.0f, 3.0f, 1.0f);
        gui.drawCenteredString(font, Component.literal(isWin ? "V" : "X"), 0, 0, accentColor);
        pose.popPose();

        pose.pushPose();
        pose.translate(cx, iconY + 50, 0);
        pose.scale(1.5f, 1.5f, 1.0f);
        gui.drawCenteredString(font, Component.literal(isWin ? "ПОБЕДА" : "ПОРАЖЕНИЕ"), 0, 0, accentColor);
        pose.popPose();

        if (isWin && winnerName != null) {
            gui.drawCenteredString(font, Component.literal(winnerName), cx, iconY + 78, PWPTheme.Colors.TEXT_PRIMARY);
        }

        int panelW = Math.min(320, (int) (width * 0.6f));
        int panelY = (int) (height * 0.54f);
        int panelH = 80;
        int panelX = PWPLayout.centerX(width, panelW);

        PWPPanel.render(gui, panelX, panelY, panelW, panelH);

        String kdStr = matchDeaths == 0 ? String.valueOf(matchKills) : String.format("%.2f", (double) matchKills / matchDeaths);
        gui.drawString(font, Component.literal("K/D: " + kdStr), panelX + 10, panelY + 8, PWPTheme.Colors.TEXT_PRIMARY);
        gui.drawString(font, Component.literal("Счёт: " + matchScore), panelX + 10, panelY + 20, PWPTheme.Colors.TEXT_PRIMARY);
        gui.drawString(font, Component.literal("Время: " + formatDuration(matchDurationSec)), panelX + 10, panelY + 32, PWPTheme.Colors.TEXT_SECONDARY);

        if (matchSummary != null && !matchSummary.isEmpty()) {
            gui.drawString(font, Component.literal(matchSummary), panelX + 10, panelY + 48, PWPTheme.Colors.TEXT_SECONDARY);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private String formatDuration(int secs) {
        if (secs < 60) return secs + "s";
        int m = secs / 60;
        int s = secs % 60;
        if (m >= 60) return (m / 60) + "h " + (m % 60) + "m";
        return m + "m " + s + "s";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
