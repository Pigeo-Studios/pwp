package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;

/** Top bar: timer, tickets, faction, player, [MAP] */
public class TopBar {

    public static final int H = 34;

    public void render(GuiGraphics gui, int x, int y, int w, int h, int mx, int my) {
        var f = PWPTheme.Fonts.display();
        int ty = y + (h - 9) / 2;

        int matchSec = ClientData.isGameStarted && ClientData.matchStartTime > 0
            ? (int)((System.currentTimeMillis() - ClientData.matchStartTime) / 1000) : 0;
        gui.drawString(f, String.format("%02d:%02d", matchSec / 60, matchSec % 60),
            x + 8, ty, PWPTheme.Colors.TEXT_PRIMARY, false);

        int tx = x + 100;
        gui.drawString(f, DeployData.blueFaction.toUpperCase(), tx, ty, PWPTheme.Colors.TEAM_BLUE, false);
        gui.drawString(f, String.valueOf(DeployData.blueTickets), tx + 60, ty, PWPTheme.Colors.TEXT_ACCENT, false);
        gui.drawString(f, "\u22C8", tx + 88, ty, PWPTheme.Colors.TEXT_DIM, false);
        gui.drawString(f, String.valueOf(DeployData.redTickets), tx + 102, ty, PWPTheme.Colors.TEXT_ACCENT, false);
        gui.drawString(f, DeployData.redFaction.toUpperCase(), tx + 132, ty, PWPTheme.Colors.TEAM_RED, false);

        gui.drawCenteredString(f, DeployData.mapName, x + w / 2, ty, PWPTheme.Colors.TEXT_SECONDARY);

        int mbX = x + w - f.width(DeployData.playerName) - 78;
        gui.drawString(f, "[MAP]", mbX, ty, PWPTheme.Colors.TEXT_DIM, false);

        gui.drawString(f, DeployData.playerName, x + w - f.width(DeployData.playerName) - 10, ty,
            PWPTheme.Colors.TEXT_PRIMARY, false);
    }

    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w) {
        var f = PWPTheme.Fonts.display();
        int mbX = x + w - f.width(DeployData.playerName) - 78;
        int ty = y + (H - 9) / 2;
        return mx >= mbX - 4 && mx <= mbX + 40 && my >= ty - 2 && my <= ty + 10 && btn == 0;
    }
}
