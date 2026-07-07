package com.pwp.lobby.gui;

import com.pwp.coreclient.gui.StatsScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.JoinMatchServerPacket;
import com.pwp.coreclient.network.OpenMatchListScreenPacket;
import com.pwp.coreclient.network.PacketHandler;
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

public class MatchListScreen extends Screen {

    private static final int CARDS_PER_PAGE = 4;
    private static final Map<String, ResourceLocation> imageCache = new HashMap<>();
    private static boolean sessionDismissed = false;

    private int page = 0;
    private OpenMatchListScreenPacket packet;
    private long openedAt;
    private int confirmSid = -1;

    public MatchListScreen(OpenMatchListScreenPacket packet) {
        super(Component.literal("ACTIVE MATCHES"));
        this.packet = packet;
        this.openedAt = System.currentTimeMillis();
    }

    public void updatePacket(OpenMatchListScreenPacket pkt) {
        this.packet = pkt;
        this.openedAt = System.currentTimeMillis();
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
        if (packet == null || packet.count == 0) return;

        int cx = width / 2;
        int cardW = Math.min(320, width - 40);
        int cardH = 72;
        int cardGap = 5;

        int start = page * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, packet.count);

        int contentH = (end - start) * (cardH + cardGap);
        int startY = 55 + (height - 55 - contentH - 40) / 2;

        int y = startY;
        for (int i = start; i < end; i++, y += cardH + cardGap) {
            final int idx = i;
            int bx = cx - cardW / 2;

            addRenderableWidget(Button.builder(
                    Component.literal(""),
                    b -> {
                        int sid = packet.serverIds[idx];
                        if (confirmSid != sid) {
                            confirmSid = sid;
                            init();
                        } else {
                            PacketHandler.INSTANCE.sendToServer(new JoinMatchServerPacket(sid));
                            confirmSid = -1;
                            sessionDismissed = true;
                            onClose();
                        }
                    }).bounds(bx, y, cardW, cardH).build());
        }

        int totalPages = Math.max(1, (packet.count + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
        int navY = startY + Math.min(CARDS_PER_PAGE, packet.count) * (cardH + cardGap) + 8;

        if (totalPages > 1) {
            String pageLabel = "Page " + (page + 1) + "/" + totalPages;
            int pw = font.width(pageLabel);
            int plX = cx - pw / 2;

            if (page > 0) {
                addRenderableWidget(Button.builder(
                        Component.literal("\u25C0"),
                        b -> { page--; init(); })
                        .bounds(plX - 48, navY, 38, 20).build());
            }
            if (page < totalPages - 1) {
                addRenderableWidget(Button.builder(
                        Component.literal("\u25B6"),
                        b -> { page++; init(); })
                        .bounds(plX + pw + 10, navY, 38, 20).build());
            }
        }

        addRenderableWidget(Button.builder(
                Component.literal("\u2694 STATS"),
                b -> Minecraft.getInstance().setScreen(new StatsScreen()))
                .bounds(cx - 106, height - 28, 50, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Close"),
                b -> onClose())
                .bounds(cx - 50, height - 28, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        if (packet == null || packet.count == 0) {
            gui.drawCenteredString(font, "\u00a77No active matches", width / 2, height / 2, 0xFFFFFF);
            return;
        }

        int cx = width / 2;
        gui.drawCenteredString(font, "\u00a76\u2694 ACTIVE MATCHES", cx, 10, 0xFFFFFF);
        gui.drawCenteredString(font, "\u00a77Click a match to join", cx, 24, 0x7A7D84);

        int cardW = Math.min(320, width - 40);
        int cardH = 72;
        int cardGap = 5;

        int start = page * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, packet.count);
        int contentH = (end - start) * (cardH + cardGap);
        int startY = 55 + (height - 55 - contentH - 40) / 2;

        int y = startY;
        for (int i = start; i < end; i++, y += cardH + cardGap) {
            boolean isPlaying = "PLAYING".equals(packet.statuses[i]);
            boolean isStarting = "STARTING".equals(packet.statuses[i]);
            boolean hover = mx >= cx - cardW / 2 && mx <= cx + cardW / 2 && my >= y && my <= y + cardH;
            boolean isConfirm = confirmSid != -1 && packet.serverIds[i] == confirmSid;
            int bx = cx - cardW / 2;

            gui.fill(bx, y, bx + cardW, y + cardH,
                isConfirm ? 0xFF2A2010 : (hover ? PWPTheme.Colors.SURFACE_LIGHT : PWPTheme.Colors.SURFACE));

            int borderColor = isConfirm ? PWPTheme.Colors.ACCENT : (isPlaying ? PWPTheme.Colors.SUCCESS : (hover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER));
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

            int textX = imgX + imgSize + 12;
            int textMaxW = bx + cardW - textX - 8;

            String nameStr = (isPlaying ? "\u00a7a\u25CF " : "\u00a7e\u25B6 ") + packet.displayNames[i];
            gui.drawString(font, nameStr, textX, y + 5, 0xFFFFFF);

            String elapsed = formatDuration(packet.elapsedSeconds[i]);
            gui.drawString(font, "\u00a77" + elapsed + "  |  \u00a7e" + packet.playerCounts[i] + "\u00a77/" + packet.maxPlayers[i], textX, y + 17, 0x7A7D84);

            String factionStr = "\u00a79" + formatFactionName(packet.blueFactions[i]) + " \u00a77vs \u00a7c" + formatFactionName(packet.redFactions[i]);
            gui.drawString(font, factionStr, textX, y + 29, 0x7A7D84);

            String ticketStr = "\u00a79" + packet.blueTickets[i] + " \u00a77| \u00a7c" + packet.redTickets[i];
            gui.drawString(font, ticketStr, textX, y + 41, 0x7A7D84);

            if (isConfirm) {
                gui.drawString(font, "\u00a7a\u2714 Click again to join", textX, y + 56, PWPTheme.Colors.SUCCESS);
            } else if (isPlaying) {
                gui.drawString(font, "\u00a7eClick to join", textX, y + 56, PWPTheme.Colors.TEXT_DIM);
            } else {
                gui.drawString(font, "\u00a77Starting...", textX, y + 56, PWPTheme.Colors.TEXT_DIM);
            }
        }

        int totalPages = Math.max(1, (packet.count + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
        int navY = startY + Math.min(CARDS_PER_PAGE, packet.count) * (cardH + cardGap) + 8;
        if (totalPages > 1) {
            gui.drawCenteredString(font, "\u00a77Page " + (page + 1) + "/" + totalPages, cx, navY + 4, 0x7A7D84);
        }
    }

    private static String formatFactionName(String faction) {
        if (faction == null || faction.isEmpty() || faction.equals("none") || faction.equals("bluefor") || faction.equals("redfor")) {
            return faction != null ? faction.toUpperCase() : "";
        }
        return faction.replace("_", " ").toUpperCase();
    }

    private static String formatDuration(int secs) {
        int m = secs / 60;
        int s = secs % 60;
        if (m >= 60) return (m / 60) + "h " + (m % 60) + "m";
        return m + "m " + s + "s";
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
                    ResourceLocation loc = ResourceLocation.tryParse("pwp_lobby:match_" + name.replaceAll("[^a-zA-Z0-9_]", "_"));
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

    public static void openWithPacket(OpenMatchListScreenPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (pkt.count == 0) {
            sessionDismissed = false;
            return;
        }

        if (sessionDismissed) return;

        if (mc.screen instanceof MatchListScreen ms) {
            ms.updatePacket(pkt);
        } else {
            mc.setScreen(new MatchListScreen(pkt));
        }
    }
}