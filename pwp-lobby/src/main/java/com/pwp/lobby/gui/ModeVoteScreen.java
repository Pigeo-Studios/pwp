package com.pwp.lobby.gui;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.OpenModeVotePacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.VoteModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModeVoteScreen extends Screen {

    private static final int CARDS_PER_PAGE = 4;
    private static boolean sessionDismissed = false;
    private static int lastKnownRemaining = -1;

    private String votedMode = null;
    private long openedAt;
    private OpenModeVotePacket packet;

    public ModeVoteScreen(OpenModeVotePacket packet) {
        super(Component.literal("MODE VOTE"));
        this.packet = packet;
        this.openedAt = System.currentTimeMillis();
    }

    public void updatePacket(OpenModeVotePacket pkt) {
        this.packet = pkt;
        this.openedAt = System.currentTimeMillis();
        init();
    }

    public static void dismissSession() {
        sessionDismissed = true;
    }

    @Override
    public void onClose() {
        super.onClose();
        sessionDismissed = true;
    }

    @Override
    protected void init() {
        clearWidgets();
        if (packet == null || packet.modeNames.length == 0) return;

        int cx = width / 2;
        int cardW = Math.min(340, width - 40);
        int cardH = 80;
        int cardGap = 12;

        int startY = 70;
        int y = startY;

        for (int i = 0; i < packet.modeNames.length; i++, y += cardH + cardGap) {
            final int idx = i;
            boolean sel = packet.modeNames[i].equals(votedMode);
            int bx = cx - cardW / 2;

            addRenderableWidget(Button.builder(
                    Component.literal(""),
                    b -> {
                        votedMode = packet.modeNames[idx];
                        PacketHandler.INSTANCE.sendToServer(new VoteModePacket(packet.modeNames[idx]));
                        sessionDismissed = true;
                        onClose();
                    }).bounds(bx, y, cardW, cardH).build());
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        if (packet == null || packet.modeNames.length == 0) {
            gui.drawCenteredString(font, "\u00a7eNo modes available", width / 2, height / 2, PWPTheme.Colors.TEXT_PRIMARY);
            return;
        }

        int cx = width / 2;

        gui.drawCenteredString(font, "\u00a76\u2694 MODE VOTE", cx, 16, PWPTheme.Colors.TEXT_ACCENT);
        int titleW = font.width("MODE VOTE") + 24;
        gui.fill(cx - titleW / 2, 26, cx + titleW / 2, 27, PWPTheme.Colors.ACCENT);

        int remaining = packet.remainingSeconds - (int)((System.currentTimeMillis() - openedAt) / 1000);
        if (remaining < 0) remaining = 0;
        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        String timerColor = remaining <= 10 ? "\u00a7c" : (remaining <= 30 ? "\u00a7e" : "\u00a7a");
        gui.drawCenteredString(font, timerColor + timeStr + "\u00a77  |  \u00a7e" + packet.onlinePlayers + "\u00a77 players online", cx, 36, 0xFFFFFF);

        String voteInfo = votedMode != null
            ? "\u00a7a\u2714 Voted: \u00a7f" + votedMode
            : "\u00a77Click a card to vote  |  \u00a7e" + packet.totalVotes + "\u00a77/" + packet.onlinePlayers + " voted";
        gui.drawCenteredString(font, voteInfo, cx, 50, 0xFFFFFF);

        int cardW = Math.min(340, width - 40);
        int cardH = 80;
        int cardGap = 12;

        int startY = 70;
        int y = startY;

        for (int i = 0; i < packet.modeNames.length; i++, y += cardH + cardGap) {
            boolean sel = packet.modeNames[i].equals(votedMode);
            boolean hover = mx >= cx - cardW / 2 && mx <= cx + cardW / 2 && my >= y && my <= y + cardH;
            int bx = cx - cardW / 2;

            int cardBg;
            if (sel) cardBg = PWPTheme.Styles.Card.BG_SELECTED;
            else if (hover) cardBg = PWPTheme.Styles.Card.BG_HOVER;
            else cardBg = PWPTheme.Styles.Card.BG;
            gui.fill(bx, y, bx + cardW, y + cardH, cardBg);

            int borderColor;
            if (sel) borderColor = PWPTheme.Styles.Card.BORDER_SELECTED;
            else if (hover) borderColor = PWPTheme.Styles.Card.BORDER_HOVER;
            else borderColor = PWPTheme.Styles.Card.BORDER;
            gui.fill(bx, y, bx + cardW, y + 1, borderColor);
            gui.fill(bx, y + cardH - 1, bx + cardW, y + cardH, borderColor);
            gui.fill(bx, y, bx + 1, y + cardH, borderColor);
            gui.fill(bx + cardW - 1, y, bx + cardW, y + cardH, borderColor);

            int textX = bx + 14;
            int textMaxW = bx + cardW - textX - 10;

            String nameStr = (sel ? "\u00a7e\u2714 " : "\u00a7f") + packet.modeDisplayNames[i];
            gui.drawString(font, nameStr, textX, y + 8, 0xFFFFFF);

            if (packet.modeDescriptions[i] != null && !packet.modeDescriptions[i].isEmpty()) {
                String desc = packet.modeDescriptions[i];
                int maxDescW = textMaxW;
                if (font.width(desc) > maxDescW) {
                    String line1 = font.plainSubstrByWidth(desc, maxDescW - 4);
                    String rest = desc.substring(line1.length()).trim();
                    if (!rest.isEmpty()) {
                        String line2 = font.plainSubstrByWidth("\u00a77" + rest, maxDescW - 4);
                        gui.drawString(font, "\u00a77" + line1, textX, y + 22, PWPTheme.Colors.TEXT_SECONDARY);
                        gui.drawString(font, "\u00a77" + line2, textX, y + 34, PWPTheme.Colors.TEXT_SECONDARY);
                    } else {
                        gui.drawString(font, "\u00a77" + line1, textX, y + 28, PWPTheme.Colors.TEXT_SECONDARY);
                    }
                } else {
                    gui.drawString(font, "\u00a77" + desc, textX, y + 28, PWPTheme.Colors.TEXT_SECONDARY);
                }
            }

            int votes = packet.voteCounts[i];
            int barX = textX;
            int barY = y + 52;
            int barW = bx + cardW - textX - 10;
            int barH = 6;
            int maxVotes = 0;
            for (int j = 0; j < packet.voteCounts.length; j++) {
                if (packet.voteCounts[j] > maxVotes) maxVotes = packet.voteCounts[j];
            }

            gui.fill(barX, barY, barX + barW, barY + barH, PWPTheme.Styles.Progress.BG);
            if (votes > 0 && maxVotes > 0) {
                float pct = (float) votes / maxVotes;
                int fillW = (int) (barW * pct);
                int fillColor = (votes == maxVotes && packet.totalVotes > 0) ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.ACCENT_DIM;
                gui.fill(barX, barY, barX + fillW, barY + barH, fillColor);
            }

            String voteText = "\u00a7e" + votes + "\u00a77 vote" + (votes != 1 ? "s" : "");
            if (packet.totalVotes > 0) {
                int pct = votes * 100 / packet.totalVotes;
                voteText += " \u00a77(" + pct + "%)";
            }
            gui.drawString(font, voteText, barX, barY + barH + 2, PWPTheme.Colors.TEXT_SECONDARY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void openWithPacket(OpenModeVotePacket pkt) {
        if (pkt.remainingSeconds > lastKnownRemaining + 10) {
            sessionDismissed = false;
        }
        lastKnownRemaining = pkt.remainingSeconds;

        if (sessionDismissed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen instanceof ModeVoteScreen vs) {
            vs.updatePacket(pkt);
        } else {
            mc.setScreen(new ModeVoteScreen(pkt));
        }
    }

    public static void resetVoteSession() {
        sessionDismissed = false;
        lastKnownRemaining = -1;
    }
}
