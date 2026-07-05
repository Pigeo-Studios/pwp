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
        int bw = 140;
        int bh = 22;

        if ("VOTING".equals(data.status)) {
            addRenderableWidget(Button.builder(
                    Component.literal("Vote for " + data.mapDisplayName),
                    b -> PacketHandler.INSTANCE.sendToServer(new VoteMapPacket(data.mapDisplayName)))
                    .bounds(cx - bw / 2, height / 2 + 50, bw, bh).build());
        }

        if (data.canJoin) {
            addRenderableWidget(Button.builder(
                    Component.literal("JOIN MATCH"),
                    b -> PacketHandler.INSTANCE.sendToServer(new JoinMatchPacket()))
                    .bounds(cx - bw / 2, height / 2 + 50, bw, bh).build());
        }

        addRenderableWidget(Button.builder(
                Component.literal("STATISTICS"),
                b -> Minecraft.getInstance().setScreen(new StatsScreen()))
                .bounds(cx - 90, height - 28, 80, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Close"),
                b -> onClose())
                .bounds(cx - 8, height - 28, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        int cx = width / 2;
        int cy = height / 2;
        int sw = Math.min(260, width - 40);

        gui.fill(cx - sw / 2, cy - 90, cx + sw / 2, cy + 35, PWPTheme.Colors.SURFACE);
        gui.fill(cx - sw / 2, cy - 90, cx + sw / 2, cy - 89, PWPTheme.Colors.BORDER_ACCENT);
        gui.fill(cx - sw / 2, cy + 34, cx + sw / 2, cy + 35, PWPTheme.Colors.BORDER);
        gui.fill(cx - sw / 2, cy - 90, cx - sw / 2 + 1, cy + 35, PWPTheme.Colors.BORDER);
        gui.fill(cx + sw / 2 - 1, cy - 90, cx + sw / 2, cy + 35, PWPTheme.Colors.BORDER);

        int y = cy - 80;

        gui.drawCenteredString(font, "\u2694 PWP MATCH", cx, y, PWPTheme.Colors.TEXT_ACCENT);
        y += 20;

        gui.drawCenteredString(font, "\u00a77Map: \u00a7f" + data.mapDisplayName, cx, y, 0xFFFFFF);
        y += 13;
        gui.drawCenteredString(font, "\u00a77Mode: \u00a7f" + data.modeDisplayName, cx, y, 0xFFFFFF);
        y += 13;

        String factionStr = "\u00a79" + data.blueFaction + " \u00a77vs \u00a7c" + data.redFaction;
        gui.drawCenteredString(font, factionStr, cx, y, 0xFFFFFF);
        y += 13;

        String ticketStr = data.blueFaction.toUpperCase() + " " + data.blueTickets + " \u00a77| \u00a7c" + data.redTickets + " " + data.redFaction.toUpperCase();
        gui.drawCenteredString(font, ticketStr, cx, y, 0xFFFFFF);
        y += 16;

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
                statusColor = 0x55FF55;
                break;
            default:
                statusText = "\u00a77\u25CB No Match";
                statusColor = 0xAAAAAA;
                break;
        }
        gui.drawCenteredString(font, statusText, cx, y, statusColor);
        y += 13;
        gui.drawCenteredString(font, "\u00a77Online: \u00a7e" + data.onlinePlayers, cx, y, 0x888888);
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