package com.pwp.lobby.gui;

import com.pwp.coreclient.gui.StatsScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.JoinMatchPacket;
import com.pwp.coreclient.network.OpenMatchScreenPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.VoteMapPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MatchScreen extends Screen {

    private static boolean sessionDismissed = false;

    private OpenMatchScreenPacket data;

    public MatchScreen(OpenMatchScreenPacket data) {
        super(Component.literal("PWP Match"));
        this.data = data;
    }

    public void updateData(OpenMatchScreenPacket data) {
        this.data = data;
        init();
    }

    @Override
    public void onClose() {
        super.onClose();
        sessionDismissed = true;
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = width / 2;
        int bw = 150;
        int bh = 22;

        if ("VOTING".equals(data.status)) {
            addRenderableWidget(Button.builder(
                    Component.literal("Vote for " + data.mapDisplayName),
                    b -> PacketHandler.INSTANCE.sendToServer(new VoteMapPacket(data.mapDisplayName)))
                    .bounds(cx - bw / 2, height / 2 + 55, bw, bh).build());
        }

        if (data.canJoin) {
            addRenderableWidget(Button.builder(
                    Component.literal("JOIN MATCH"),
                    b -> PacketHandler.INSTANCE.sendToServer(new JoinMatchPacket()))
                    .bounds(cx - bw / 2, height / 2 + 55, bw, bh).build());
        }

        addRenderableWidget(Button.builder(
                Component.literal("\u2694 STATISTICS"),
                b -> Minecraft.getInstance().setScreen(new StatsScreen()))
                .bounds(cx - 96, height - 30, 80, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Close"),
                b -> onClose())
                .bounds(cx - 14, height - 30, 70, 22).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        int cx = width / 2;
        int cy = height / 2;
        int panelW = Math.min(280, width - 40);
        int panelH = 120;
        int px = cx - panelW / 2;
        int py = cy - 60;

        gui.fill(px, py, px + panelW, py + panelH, PWPTheme.Styles.Panel.BG);
        gui.fill(px, py, px + panelW, py + 1, PWPTheme.Colors.BORDER_ACCENT);
        gui.fill(px, py + panelH - 1, px + panelW, py + panelH, PWPTheme.Styles.Panel.BORDER);
        gui.fill(px, py, px + 1, py + panelH, PWPTheme.Styles.Panel.BORDER);
        gui.fill(px + panelW - 1, py, px + panelW, py + panelH, PWPTheme.Styles.Panel.BORDER);

        int y = py + 8;
        gui.drawCenteredString(font, PWPTheme.Icons.SWORDS + " PWP MATCH", cx, y, PWPTheme.Colors.TEXT_ACCENT);

        y += 18;
        gui.drawCenteredString(font, "\u00a77Map: \u00a7f" + data.mapDisplayName, cx, y, PWPTheme.Colors.TEXT_PRIMARY);
        y += 13;
        gui.drawCenteredString(font, "\u00a77Mode: \u00a7f" + data.modeDisplayName, cx, y, PWPTheme.Colors.TEXT_PRIMARY);
        y += 13;

        String factionStr = "\u00a79" + formatFactionName(data.blueFaction) + " \u00a77vs \u00a7c" + formatFactionName(data.redFaction);
        gui.drawCenteredString(font, factionStr, cx, y, PWPTheme.Colors.TEXT_PRIMARY);
        y += 13;

        String ticketStr = "\u00a79" + data.blueTickets + " \u00a77| \u00a7c" + data.redTickets;
        gui.drawCenteredString(font, ticketStr, cx, y, PWPTheme.Colors.TEXT_PRIMARY);
        y += 14;

        String statusText;
        int statusColor;
        switch (data.status) {
            case "VOTING":
                statusText = "\u00a7e\u25B6 Voting \u00a77(" + data.remainingSeconds + "s)";
                statusColor = 0xFFC040;
                break;
            case "STARTING":
                statusText = "\u00a7e\u25B6 Starting...";
                statusColor = 0xFFFF55;
                break;
            case "PLAYING":
                statusText = "\u00a7a\u25CF Match in Progress";
                statusColor = PWPTheme.Colors.SUCCESS_LIGHT;
                break;
            default:
                statusText = "\u00a77\u25CB No Match";
                statusColor = PWPTheme.Colors.TEXT_SECONDARY;
                break;
        }
        gui.drawCenteredString(font, statusText, cx, y, statusColor);
        y += 12;
        gui.drawCenteredString(font, "\u00a77Online: \u00a7e" + data.onlinePlayers, cx, y, PWPTheme.Colors.TEXT_SECONDARY);
    }

    private static String formatFactionName(String faction) {
        if (faction == null || faction.isEmpty() || faction.equals("none") || faction.equals("bluefor") || faction.equals("redfor")) {
            return faction != null ? faction.toUpperCase() : "";
        }
        return faction.replace("_", " ").toUpperCase();
    }

    public static void open(OpenMatchScreenPacket data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (sessionDismissed) return;
        if (mc.screen instanceof MatchScreen ms) {
            ms.updateData(data);
        } else {
            mc.setScreen(new MatchScreen(data));
        }
    }

    public static void resetDismissed() {
        sessionDismissed = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
