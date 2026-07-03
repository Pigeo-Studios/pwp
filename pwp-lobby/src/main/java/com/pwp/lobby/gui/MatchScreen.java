package com.pwp.lobby.gui;

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
    protected void init() {
        clearWidgets();
        int cx = width / 2;
        int bw = 120;
        int bh = 20;

        if ("VOTING".equals(data.status)) {
            addRenderableWidget(Button.builder(
                    Component.literal("Vote for Grozny"),
                    b -> PacketHandler.INSTANCE.sendToServer(new VoteMapPacket("grozny")))
                    .bounds(cx - bw / 2, height / 2 + 30, bw, bh).build());
        }

        if (data.canJoin) {
            addRenderableWidget(Button.builder(
                    Component.literal("Join Match"),
                    b -> PacketHandler.INSTANCE.sendToServer(new JoinMatchPacket()))
                    .bounds(cx - bw / 2, height / 2 + 55, bw, bh).build());
        }

        addRenderableWidget(Button.builder(
                Component.literal("Close"),
                b -> onClose())
                .bounds(cx - 40, height - 30, 80, bh).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        int cx = width / 2;
        int y = height / 2 - 80;

        gui.drawCenteredString(font, "PWP Match", cx, y, 0xFFD700);
        y += 25;
        gui.drawCenteredString(font, "Map: " + data.mapDisplayName, cx, y, 0xFFFFFF);
        y += 15;
        gui.drawCenteredString(font, "Mode: " + data.modeDisplayName, cx, y, 0xFFFFFF);
        y += 15;
        gui.drawCenteredString(font, data.blueFaction + " vs " + data.redFaction, cx, y, 0xFFFFFF);
        y += 15;
        gui.drawCenteredString(font, "Tickets: " + data.blueTickets + " / " + data.redTickets, cx, y, 0xFFFFFF);
        y += 15;

        String statusText;
        int statusColor;
        switch (data.status) {
            case "VOTING":
                statusText = "Voting (" + data.remainingSeconds + "s)";
                statusColor = 0xFFC040;
                break;
            case "STARTING":
                statusText = "Starting...";
                statusColor = 0xFFFF55;
                break;
            case "PLAYING":
                statusText = "Match in Progress";
                statusColor = 0x55FF55;
                break;
            default:
                statusText = "No Match";
                statusColor = 0xAAAAAA;
                break;
        }
        gui.drawCenteredString(font, "Status: " + statusText, cx, y, statusColor);
        y += 15;
        gui.drawCenteredString(font, "Online: " + data.onlinePlayers, cx, y, 0x888888);
    }

    public static void open(OpenMatchScreenPacket data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (mc.screen instanceof MatchScreen ms) {
                ms.updateData(data);
            } else {
                mc.setScreen(new MatchScreen(data));
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
