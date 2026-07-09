package com.pwp.lobby.gui;

import com.pwp.coreclient.gui.StatsScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.OpenVotingScreenPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.VoteMapPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class VotingScreen extends Screen {

    private static final int CARDS_PER_PAGE = 4;
    private static final Map<String, ResourceLocation> imageCache = new HashMap<>();
    private static boolean sessionDismissed = false;
    private static int lastKnownRemaining = -1;

    private int page = 0;
    private String votedMap = null;
    private long openedAt;
    private OpenVotingScreenPacket packet;

    public VotingScreen(OpenVotingScreenPacket packet) {
        super(Component.literal("MAP VOTE"));
        this.packet = packet;
        this.openedAt = System.currentTimeMillis();
    }

    public void updatePacket(OpenVotingScreenPacket pkt) {
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
        if (packet == null || packet.mapNames.length == 0) return;

        int cx = width / 2;
        int cardW = Math.min(340, width - 40);
        int cardH = 70;
        int cardGap = 8;

        int start = page * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, packet.mapNames.length);

        int contentH = (end - start) * (cardH + cardGap);
        int startY = 56 + (height - 56 - contentH - 40) / 2;

        int y = startY;
        for (int i = start; i < end; i++, y += cardH + cardGap) {
            final int idx = i;
            boolean sel = packet.mapNames[i].equals(votedMap);
            int bx = cx - cardW / 2;

            addRenderableWidget(Button.builder(
                    Component.literal(""),
                    b -> {
                        votedMap = packet.mapNames[idx];
                        PacketHandler.INSTANCE.sendToServer(new VoteMapPacket(packet.mapNames[idx]));
                        sessionDismissed = true;
                        onClose();
                    }).bounds(bx, y, cardW, cardH).build());
        }

        int totalPages = Math.max(1, (packet.mapNames.length + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
        int navY = startY + Math.min(CARDS_PER_PAGE, packet.mapNames.length) * (cardH + cardGap) + 10;

        if (totalPages > 1) {
            String pageLabel = "Page " + (page + 1) + "/" + totalPages;
            int pw = font.width(pageLabel);
            int plX = cx - pw / 2;

            if (page > 0) {
                addRenderableWidget(Button.builder(
                        Component.literal("\u25C0"),
                        b -> { page--; init(); })
                        .bounds(plX - 50, navY, 38, 20).build());
            }
            if (page < totalPages - 1) {
                addRenderableWidget(Button.builder(
                        Component.literal("\u25B6"),
                        b -> { page++; init(); })
                        .bounds(plX + pw + 12, navY, 38, 20).build());
            }
        }

        addRenderableWidget(Button.builder(
                Component.literal("\u2694 STATS"),
                b -> Minecraft.getInstance().setScreen(new StatsScreen()))
                .bounds(cx - 110, height - 30, 56, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Close"),
                b -> onClose())
                .bounds(cx - 50, height - 30, 100, 22).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        if (packet == null || packet.mapNames.length == 0) {
            gui.drawCenteredString(font, "\u00a7eNo maps available", width / 2, height / 2, PWPTheme.Colors.TEXT_PRIMARY);
            return;
        }

        int cx = width / 2;

        gui.drawCenteredString(font, "\u00a76\u2694 MAP VOTE", cx, 12, PWPTheme.Colors.TEXT_ACCENT);
        int titleW = font.width("MAP VOTE") + 24;
        gui.fill(cx - titleW / 2, 22, cx + titleW / 2, 23, PWPTheme.Colors.ACCENT);

        int remaining = packet.remainingSeconds - (int)((System.currentTimeMillis() - openedAt) / 1000);
        if (remaining < 0) remaining = 0;
        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        String timerColor = remaining <= 10 ? "\u00a7c" : (remaining <= 30 ? "\u00a7e" : "\u00a7a");
        gui.drawCenteredString(font, timerColor + timeStr + "\u00a77  |  \u00a7e" + packet.onlinePlayers + "\u00a77 players online", cx, 30, 0xFFFFFF);

        String voteInfo = votedMap != null
            ? "\u00a7a\u2714 Voted: \u00a7f" + votedMap
            : "\u00a77Click a card to vote  |  \u00a7e" + packet.totalVotes + "\u00a77/" + packet.onlinePlayers + " voted";
        gui.drawCenteredString(font, voteInfo, cx, 44, 0xFFFFFF);

        int cardW = Math.min(340, width - 40);
        int cardH = 70;
        int cardGap = 8;

        int start = page * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, packet.mapNames.length);
        int contentH = (end - start) * (cardH + cardGap);
        int startY = 56 + (height - 56 - contentH - 40) / 2;

        int y = startY;
        for (int i = start; i < end; i++, y += cardH + cardGap) {
            boolean sel = packet.mapNames[i].equals(votedMap);
            boolean isLeader = packet.mapNames[i].equals(packet.leaderName) && packet.totalVotes > 0;
            boolean hover = mx >= cx - cardW / 2 && mx <= cx + cardW / 2 && my >= y && my <= y + cardH;
            int bx = cx - cardW / 2;

            int cardBg;
            if (sel) cardBg = PWPTheme.Styles.Card.BG_SELECTED;
            else if (hover) cardBg = PWPTheme.Styles.Card.BG_HOVER;
            else cardBg = PWPTheme.Styles.Card.BG;
            gui.fill(bx, y, bx + cardW, y + cardH, cardBg);

            int borderColor;
            if (sel) borderColor = PWPTheme.Styles.Card.BORDER_SELECTED;
            else if (isLeader) borderColor = PWPTheme.Colors.ACCENT;
            else if (hover) borderColor = PWPTheme.Styles.Card.BORDER_HOVER;
            else borderColor = PWPTheme.Styles.Card.BORDER;
            gui.fill(bx, y, bx + cardW, y + 1, borderColor);
            gui.fill(bx, y + cardH - 1, bx + cardW, y + cardH, borderColor);
            gui.fill(bx, y, bx + 1, y + cardH, borderColor);
            gui.fill(bx + cardW - 1, y, bx + cardW, y + cardH, borderColor);

            int imgSize = 52;
            int imgX = bx + 8;
            int imgY = y + (cardH - imgSize) / 2;
            gui.fill(imgX, imgY, imgX + imgSize, imgY + imgSize, 0xFF000000);
            gui.fill(imgX + 1, imgY + 1, imgX + imgSize - 1, imgY + imgSize - 1, 0xFF1A1E26);

            ResourceLocation tex = getMapTexture(packet.mapNames[i], packet.worldPaths[i]);
            if (tex != null) {
                try {
                    gui.blit(tex, imgX + 1, imgY + 1, 0, 0, imgSize - 2, imgSize - 2, imgSize - 2, imgSize - 2);
                } catch (Exception ignored) {}
            }

            int textX = imgX + imgSize + 14;
            int textMaxW = bx + cardW - textX - 10;

            String nameStr = (isLeader ? "\u00a76\u265B " : (sel ? "\u00a7e\u2714 " : "\u00a7f")) + packet.mapDisplayNames[i];
            gui.drawString(font, nameStr, textX, y + 6, 0xFFFFFF);

            if (packet.mapDescriptions[i] != null && !packet.mapDescriptions[i].isEmpty()) {
                String desc = packet.mapDescriptions[i];
                int maxDescW = textMaxW;
                if (font.width(desc) > maxDescW) {
                    String line1 = font.plainSubstrByWidth(desc, maxDescW - 4);
                    String rest = desc.substring(line1.length()).trim();
                    String line2;
                    if (!rest.isEmpty()) {
                        line2 = font.plainSubstrByWidth("\u00a77" + rest, maxDescW - 4);
                        gui.drawString(font, "\u00a77" + line1, textX, y + 20, PWPTheme.Colors.TEXT_SECONDARY);
                        gui.drawString(font, "\u00a77" + line2, textX, y + 32, PWPTheme.Colors.TEXT_SECONDARY);
                    } else {
                        gui.drawString(font, "\u00a77" + line1, textX, y + 26, PWPTheme.Colors.TEXT_SECONDARY);
                    }
                } else {
                    gui.drawString(font, "\u00a77" + desc, textX, y + 26, PWPTheme.Colors.TEXT_SECONDARY);
                }
            }

            int votes = packet.voteCounts[i];
            int barX = textX;
            int barY = y + 48;
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
                int fillColor = isLeader ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.ACCENT_DIM;
                gui.fill(barX, barY, barX + fillW, barY + barH, fillColor);
            }

            String voteText = "\u00a7e" + votes + "\u00a77 vote" + (votes != 1 ? "s" : "");
            if (packet.totalVotes > 0) {
                int pct = votes * 100 / packet.totalVotes;
                voteText += " \u00a77(" + pct + "%)";
            }
            gui.drawString(font, voteText, barX, barY + barH + 2, PWPTheme.Colors.TEXT_SECONDARY);
        }

        int totalPages = Math.max(1, (packet.mapNames.length + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
        int navY = startY + Math.min(CARDS_PER_PAGE, packet.mapNames.length) * (cardH + cardGap) + 8;
        if (totalPages > 1) {
            gui.drawCenteredString(font, "\u00a77Page " + (page + 1) + "/" + totalPages, cx, navY + 4, PWPTheme.Colors.TEXT_SECONDARY);
        }
    }

    private static ResourceLocation getMapTexture(String mapName, String worldPath) {
        if (mapName == null || worldPath == null || worldPath.isEmpty()) return null;
        return imageCache.computeIfAbsent(mapName, name -> {
            try {
                Path iconFile = Paths.get(worldPath, "icon.png");
                if (!Files.exists(iconFile)) return null;
                try (FileInputStream fis = new FileInputStream(iconFile.toFile())) {
                    com.mojang.blaze3d.platform.NativeImage img =
                            com.mojang.blaze3d.platform.NativeImage.read(fis);
                    DynamicTexture tex = new DynamicTexture(img);
                    ResourceLocation loc = ResourceLocation.tryParse("pwp_lobby:map_" + name.replaceAll("[^a-zA-Z0-9_]", "_"));
                    if (loc == null) return null;
                    Minecraft.getInstance().getTextureManager().register(loc, tex);
                    return loc;
                }
            } catch (Exception e) {
                return null;
            }
        });
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void openWithPacket(OpenVotingScreenPacket pkt) {
        if (pkt.remainingSeconds > lastKnownRemaining + 10) {
            sessionDismissed = false;
        }
        lastKnownRemaining = pkt.remainingSeconds;

        if (sessionDismissed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen instanceof VotingScreen vs) {
            vs.updatePacket(pkt);
        } else {
            mc.setScreen(new VotingScreen(pkt));
        }
    }

    public static void resetVoteSession() {
        sessionDismissed = false;
        lastKnownRemaining = -1;
    }
}
