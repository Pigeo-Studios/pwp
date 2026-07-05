package com.pwp.lobby.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.StatsScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.*;
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

public class LobbyScreen extends Screen {

    private static LobbyScreen instance;

    private static final int TAB_MATCH = 0;
    private static final int TAB_VOTE = 1;
    private static final int TAB_LIST = 2;

    private int currentTab = TAB_MATCH;
    private int targetTab = TAB_MATCH;
    private long tabSwitchTime = 0;
    private static final long TAB_ANIM_MS = 200;

    // Match info data
    private OpenMatchScreenPacket matchData;

    // Vote data
    private OpenVotingScreenPacket voteData;
    private int votePage = 0;
    private String votedMap = null;
    private long voteOpenedAt;

    // Match list data
    private OpenMatchListScreenPacket listData;
    private int listPage = 0;
    private int confirmSid = -1;

    private static final Map<String, ResourceLocation> imageCache = new HashMap<>();

    public LobbyScreen() {
        super(Component.literal("PWP"));
        instance = this;
    }

    public static LobbyScreen get() { return instance; }

    // ====== PACKET HANDLERS ======

    public static void openMatch(OpenMatchScreenPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance != null) {
            instance.matchData = pkt;
            if (instance.currentTab != TAB_LIST) {
                instance.switchTab(TAB_MATCH);
            }
        } else {
            LobbyScreen s = new LobbyScreen();
            s.matchData = pkt;
            s.currentTab = TAB_MATCH;
            s.targetTab = TAB_MATCH;
            mc.setScreen(s);
        }
    }

    public static void updateVote(OpenVotingScreenPacket pkt) {
        if (instance == null) return;
        instance.voteData = pkt;
        instance.voteOpenedAt = System.currentTimeMillis();
        if (instance.currentTab == TAB_VOTE) {
            instance.votePage = 0;
        }
    }

    public static void openVote(OpenVotingScreenPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance != null) {
            instance.voteData = pkt;
            instance.voteOpenedAt = System.currentTimeMillis();
            instance.switchTab(TAB_VOTE);
        } else {
            LobbyScreen s = new LobbyScreen();
            s.voteData = pkt;
            s.voteOpenedAt = System.currentTimeMillis();
            s.currentTab = TAB_VOTE;
            s.targetTab = TAB_VOTE;
            mc.setScreen(s);
        }
    }

    public static void openList(OpenMatchListScreenPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance != null) {
            instance.listData = pkt;
            if (instance.currentTab == TAB_LIST) instance.listPage = 0;
            instance.switchTab(TAB_LIST);
        } else {
            LobbyScreen s = new LobbyScreen();
            s.listData = pkt;
            s.currentTab = TAB_LIST;
            s.targetTab = TAB_LIST;
            mc.setScreen(s);
        }
    }

    public static void updateList(OpenMatchListScreenPacket pkt) {
        if (instance == null) return;
        instance.listData = pkt;
    }

    public static void resetInstance() {
        instance = null;
    }

    private void switchTab(int tab) {
        if (tab != currentTab) {
            targetTab = tab;
            tabSwitchTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        instance = null;
        imageCache.clear();
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = width / 2;

        // Tab buttons
        addRenderableWidget(Button.builder(
                Component.literal("Match"),
                b -> switchTab(TAB_MATCH))
                .bounds(cx - 160, 6, 58, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("Vote"),
                b -> { if (voteData != null) switchTab(TAB_VOTE); })
                .bounds(cx - 100, 6, 48, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("Matches"),
                b -> switchTab(TAB_LIST))
                .bounds(cx - 50, 6, 60, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2694 Stats"),
                b -> Minecraft.getInstance().setScreen(new StatsScreen()))
                .bounds(cx + 12, 6, 58, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715"),
                b -> onClose())
                .bounds(cx + 74, 6, 22, 20).build());

        // Vote tab controls
        if (voteData != null && voteData.mapNames.length > 0) {
            int totalVotePages = Math.max(1, (voteData.mapNames.length + 3) / 4);
            if (totalVotePages > 1) {
                int pageY = 30;
                addRenderableWidget(Button.builder(
                        Component.literal("\u25C0"),
                        b -> { if (votePage > 0) votePage--; })
                        .bounds(cx + 52, pageY, 18, 16).build());
                addRenderableWidget(Button.builder(
                        Component.literal("\u25B6"),
                        b -> { if (votePage < totalVotePages - 1) votePage++; })
                        .bounds(cx + 96, pageY, 18, 16).build());
            }
        }

        // List tab controls
        if (listData != null && listData.count > 0) {
            int totalListPages = Math.max(1, (listData.count + 3) / 4);
            if (totalListPages > 1) {
                int pageY = 30;
                addRenderableWidget(Button.builder(
                        Component.literal("\u25C0"),
                        b -> { if (listPage > 0) listPage--; })
                        .bounds(cx + 52, pageY, 18, 16).build());
                addRenderableWidget(Button.builder(
                        Component.literal("\u25B6"),
                        b -> { if (listPage < totalListPages - 1) listPage++; })
                        .bounds(cx + 96, pageY, 18, 16).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        // Animate tab switch
        long elapsed = System.currentTimeMillis() - tabSwitchTime;
        float animProgress = Math.min(1f, elapsed / (float) TAB_ANIM_MS);
        if (animProgress >= 1f) {
            currentTab = targetTab;
        }

        int cx = width / 2;
        String title = currentTab == TAB_MATCH ? "\u2694 PWP MATCH"
                    : currentTab == TAB_VOTE ? "\u2694 MAP VOTE"
                    : "\u2694 ACTIVE MATCHES";
        gui.drawCenteredString(font, "\u00a76" + title, cx, 30, 0xFFFFFF);

        // Animate content alpha
        float alpha = currentTab == targetTab ? 1f : animProgress;
        if (currentTab != targetTab) {
            alpha = 1f - animProgress; // fade out old tab
        }
        int a = Math.max(4, Math.min(255, (int)(alpha * 255)));
        if (a < 4) return;

        RenderSystem.enableBlend();

        if (currentTab == TAB_MATCH) {
            renderMatchTab(gui, mx, my, cx, a);
        } else if (currentTab == TAB_VOTE) {
            renderVoteTab(gui, mx, my, cx, a);
        } else if (currentTab == TAB_LIST) {
            renderListTab(gui, mx, my, cx, a);
        }

        RenderSystem.disableBlend();
    }

    // ====== MATCH TAB ======

    private void renderMatchTab(GuiGraphics gui, int mx, int my, int cx, int a) {
        if (matchData == null) {
            String msg = "\u00a77No match data available";
            gui.drawCenteredString(font, msg, cx, height / 2, 0x7A7D84);
            return;
        }
        int cy = height / 2 - 40;
        int sw = Math.min(260, width - 40);

        int bg = (a << 24) | (PWPTheme.Colors.SURFACE & 0x00FFFFFF);
        int border = (a << 24) | (PWPTheme.Colors.BORDER_ACCENT & 0x00FFFFFF);

        gui.fill(cx - sw / 2, cy - 60, cx + sw / 2, cy + 65, bg);
        gui.fill(cx - sw / 2, cy - 60, cx + sw / 2, cy - 59, border);
        gui.fill(cx - sw / 2, cy + 64, cx + sw / 2, cy + 65, (a << 24) | 0x1E222A);

        int y = cy - 50;
        int textColor = (a << 24) | 0xFFFFFF;
        int secColor = (a << 24) | 0x7A7D84;

        gui.drawCenteredString(font, "\u00a7fMap: \u00a7e" + matchData.mapDisplayName, cx, y, textColor);
        y += 15;
        gui.drawCenteredString(font, "\u00a7fMode: \u00a7e" + matchData.modeDisplayName, cx, y, textColor);
        y += 15;

        String factions = "\u00a79" + matchData.blueFaction + " \u00a77vs \u00a7c" + matchData.redFaction;
        gui.drawCenteredString(font, factions, cx, y, textColor);
        y += 15;

        String tickets = "\u00a79" + matchData.blueTickets + " \u00a77| \u00a7c" + matchData.redTickets;
        gui.drawCenteredString(font, tickets, cx, y, textColor);
        y += 18;

        String statusText;
        int statusColor;
        switch (matchData.status) {
            case "VOTING":
                statusText = "\u25B6 Voting (" + matchData.remainingSeconds + "s)";
                statusColor = 0xFFC040;
                break;
            case "STARTING":
                statusText = "\u25B6 Starting...";
                statusColor = 0xFFFF55;
                break;
            case "PLAYING":
                statusText = "\u25CF Match in Progress";
                statusColor = 0x55FF55;
                break;
            default:
                statusText = "\u25CB No Match";
                statusColor = 0xAAAAAA;
                break;
        }
        gui.drawCenteredString(font, (a == 255 ? "\u00a7" : "") + statusText, cx, y, statusColor);
        y += 13;
        gui.drawCenteredString(font, "\u00a77Online: \u00a7e" + matchData.onlinePlayers, cx, y, secColor);
        y += 16;

        if ("VOTING".equals(matchData.status)) {
            gui.drawCenteredString(font, "\u00a7eClick Vote tab to vote", cx, y + 8, secColor);
        }
        if (matchData.canJoin) {
            gui.drawCenteredString(font, "\u00a7aClick Matches tab to join", cx, y + 8, secColor);
        }
    }

    // ====== VOTE TAB ======

    private void renderVoteTab(GuiGraphics gui, int mx, int my, int cx, int a) {
        if (voteData == null || voteData.mapNames.length == 0) {
            gui.drawCenteredString(font, "\u00a77No vote in progress", cx, height / 2, 0x7A7D84);
            return;
        }

        int remaining = voteData.remainingSeconds - (int)((System.currentTimeMillis() - voteOpenedAt) / 1000);
        if (remaining < 0) remaining = 0;
        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        String timerColor = remaining <= 10 ? "\u00a7c" : (remaining <= 30 ? "\u00a7e" : "\u00a7a");
        String info = timerColor + timeStr + "\u00a77  |  \u00a7e" + voteData.onlinePlayers + "\u00a77 online";
        if (votedMap != null) {
            info += "  \u00a7a\u2714 " + votedMap;
        } else {
            info += "  \u00a7e" + voteData.totalVotes + "\u00a77/" + voteData.onlinePlayers + " voted";
        }
        gui.drawCenteredString(font, info, cx, 46, 0xFFFFFF);

        int cardsPerPage = 4;
        int cardW = Math.min(300, width - 40);
        int cardH = 64;
        int gap = 5;

        int start = votePage * cardsPerPage;
        int end = Math.min(start + cardsPerPage, voteData.mapNames.length);
        int contentH = (end - start) * (cardH + gap);
        int startY = 56 + (height - 56 - contentH - 40) / 2;

        int y = startY;
        for (int i = start; i < end; i++, y += cardH + gap) {
            boolean sel = voteData.mapNames[i].equals(votedMap);
            boolean isLeader = voteData.mapNames[i].equals(voteData.leaderName) && voteData.totalVotes > 0;
            boolean hover = mx >= cx - cardW / 2 && mx <= cx + cardW / 2 && my >= y && my <= y + cardH;
            int bx = cx - cardW / 2;

            int cardBg = (a << 24) | (sel ? 0x002A2010 : (hover ? 0x001A1E26 : 0x0012151A));
            gui.fill(bx, y, bx + cardW, y + cardH, cardBg);

            int borderCol;
            if (sel) borderCol = (a << 24) | 0x00C8812A;
            else if (isLeader) borderCol = (a << 24) | 0x00C8812A;
            else if (hover) borderCol = (a << 24) | 0x00C8812A;
            else borderCol = (a << 24) | 0x001E222A;
            gui.fill(bx, y, bx + cardW, y + 1, borderCol);
            gui.fill(bx, y + cardH - 1, bx + cardW, y + cardH, borderCol);
            gui.fill(bx, y, bx + 1, y + cardH, borderCol);
            gui.fill(bx + cardW - 1, y, bx + cardW, y + cardH, borderCol);

            int imgSize = 48;
            int imgX = bx + 6;
            int imgY = y + (cardH - imgSize) / 2;
            gui.fill(imgX, imgY, imgX + imgSize, imgY + imgSize, (a << 24) | 0x00000000);

            ResourceLocation tex = getTexture(voteData.mapNames[i], voteData.worldPaths[i], "vote_");
            if (tex != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, a / 255f);
                gui.blit(tex, imgX + 1, imgY + 1, 0, 0, imgSize - 2, imgSize - 2, imgSize - 2, imgSize - 2);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            int textX = imgX + imgSize + 10;
            String nameStr = (isLeader ? "\u00a76\u265B " : (sel ? "\u00a7e\u2714 " : "\u00a7f")) + voteData.mapDisplayNames[i];
            gui.drawString(font, nameStr, textX, y + 5, (a << 24) | 0xFFFFFF);

            if (voteData.mapDescriptions[i] != null && !voteData.mapDescriptions[i].isEmpty()) {
                String desc = "\u00a77" + voteData.mapDescriptions[i];
                gui.drawString(font, desc, textX, y + 17, (a << 24) | 0x7A7D84);
            }

            gui.drawString(font, "\u00a77Players: \u00a7e" + voteData.maxPlayers[i], textX, y + 29, (a << 24) | 0x7A7D84);

            int votes = voteData.voteCounts[i];
            int barX = textX;
            int barY = y + 42;
            int barW = bx + cardW - textX - 6;
            int barH = 5;
            int maxVotes = 0;
            for (int j = 0; j < voteData.voteCounts.length; j++) {
                if (voteData.voteCounts[j] > maxVotes) maxVotes = voteData.voteCounts[j];
            }

            gui.fill(barX, barY, barX + barW, barY + barH, (a << 24) | 0x00181C24);
            if (votes > 0 && maxVotes > 0) {
                float pct = (float) votes / maxVotes;
                int fillW = (int) (barW * pct);
                int fillCol = (a << 24) | (isLeader ? 0x00C8812A : 0x008B6220);
                gui.fill(barX, barY, barX + fillW, barY + barH, fillCol);
            }

            String voteText = "\u00a7e" + votes + "\u00a77" + (votes != 1 ? " votes" : " vote");
            if (voteData.totalVotes > 0) {
                int pct = votes * 100 / voteData.totalVotes;
                voteText += " (" + pct + "%)";
            }
            gui.drawString(font, voteText, barX, barY + barH + 1, (a << 24) | 0x7A7D84);
        }
    }

    // ====== LIST TAB ======

    private void renderListTab(GuiGraphics gui, int mx, int my, int cx, int a) {
        if (listData == null || listData.count == 0) {
            gui.drawCenteredString(font, "\u00a77No active matches", cx, height / 2, 0x7A7D84);
            return;
        }
        gui.drawCenteredString(font, "\u00a77Click a match to join", cx, 46, (a << 24) | 0x7A7D84);

        int cardsPerPage = 4;
        int cardW = Math.min(300, width - 40);
        int cardH = 68;
        int gap = 5;

        int start = listPage * cardsPerPage;
        int end = Math.min(start + cardsPerPage, listData.count);
        int contentH = (end - start) * (cardH + gap);
        int startY = 56 + (height - 56 - contentH - 40) / 2;

        int y = startY;
        for (int i = start; i < end; i++, y += cardH + gap) {
            boolean isPlaying = "PLAYING".equals(listData.statuses[i]);
            boolean isStarting = "STARTING".equals(listData.statuses[i]);
            boolean hover = mx >= cx - cardW / 2 && mx <= cx + cardW / 2 && my >= y && my <= y + cardH;
            boolean isConfirm = confirmSid != -1 && listData.serverIds[i] == confirmSid;
            int bx = cx - cardW / 2;

            int cardBg = (a << 24) | (isConfirm ? 0x002A2010 : (hover ? 0x001A1E26 : 0x0012151A));
            gui.fill(bx, y, bx + cardW, y + cardH, cardBg);

            int borderCol;
            if (isConfirm) borderCol = (a << 24) | 0x00C8812A;
            else if (isPlaying) borderCol = (a << 24) | 0x003D7A40;
            else if (hover) borderCol = (a << 24) | 0x00C8812A;
            else borderCol = (a << 24) | 0x001E222A;
            gui.fill(bx, y, bx + cardW, y + 1, borderCol);
            gui.fill(bx, y + cardH - 1, bx + cardW, y + cardH, borderCol);
            gui.fill(bx, y, bx + 1, y + cardH, borderCol);
            gui.fill(bx + cardW - 1, y, bx + cardW, y + cardH, borderCol);

            int imgSize = 48;
            int imgX = bx + 6;
            int imgY = y + (cardH - imgSize) / 2;
            gui.fill(imgX, imgY, imgX + imgSize, imgY + imgSize, (a << 24) | 0x00000000);

            ResourceLocation tex = getTexture(listData.mapNames[i], listData.worldPaths[i], "list_");
            if (tex != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, a / 255f);
                gui.blit(tex, imgX + 1, imgY + 1, 0, 0, imgSize - 2, imgSize - 2, imgSize - 2, imgSize - 2);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            int textX = imgX + imgSize + 10;
            String nameStr = (isPlaying ? "\u00a7a\u25CF " : "\u00a7e\u25B6 ") + listData.displayNames[i];
            gui.drawString(font, nameStr, textX, y + 5, (a << 24) | 0xFFFFFF);

            String elapsed = formatDuration(listData.elapsedSeconds[i]);
            gui.drawString(font, "\u00a77" + elapsed + "  |  \u00a7e" + listData.playerCounts[i] + "\u00a77/" + listData.maxPlayers[i],
                textX, y + 17, (a << 24) | 0x7A7D84);

            String factionStr = "\u00a79" + listData.blueFactions[i] + " \u00a77vs \u00a7c" + listData.redFactions[i];
            gui.drawString(font, factionStr, textX, y + 29, (a << 24) | 0x7A7D84);

            String ticketStr = "\u00a79" + listData.blueTickets[i] + " \u00a77| \u00a7c" + listData.redTickets[i];
            gui.drawString(font, ticketStr, textX, y + 41, (a << 24) | 0x7A7D84);

            if (isConfirm) {
                gui.drawString(font, "\u00a7a\u2714 Click again to join", textX, y + 54, (a << 24) | 0x3D7A40);
            } else if (isPlaying) {
                gui.drawString(font, "\u00a7eClick to join", textX, y + 54, (a << 24) | 0x7A7D84);
            } else {
                gui.drawString(font, "\u00a77Starting...", textX, y + 54, (a << 24) | 0x7A7D84);
            }

            // Full card click detection (no button widget, handle in mouseClicked)
        }
    }

    // ====== CLICK HANDLING ======

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);
        int cx = width / 2;

        if (currentTab == TAB_VOTE && voteData != null) {
            int cardsPerPage = 4;
            int cardW = Math.min(300, width - 40);
            int cardH = 64;
            int gap = 5;
            int start = votePage * cardsPerPage;
            int end = Math.min(start + cardsPerPage, voteData.mapNames.length);
            int contentH = (end - start) * (cardH + gap);
            int startY = 56 + (height - 56 - contentH - 40) / 2;

            int y = startY;
            for (int i = start; i < end; i++, y += cardH + gap) {
                int bx = cx - cardW / 2;
                if (mx >= bx && mx <= bx + cardW && my >= y && my <= y + cardH) {
                    votedMap = voteData.mapNames[i];
                    PacketHandler.INSTANCE.sendToServer(new VoteMapPacket(voteData.mapNames[i]));
                    return true;
                }
            }
        }

        if (currentTab == TAB_LIST && listData != null) {
            int cardsPerPage = 4;
            int cardW = Math.min(300, width - 40);
            int cardH = 68;
            int gap = 5;
            int start = listPage * cardsPerPage;
            int end = Math.min(start + cardsPerPage, listData.count);
            int contentH = (end - start) * (cardH + gap);
            int startY = 56 + (height - 56 - contentH - 40) / 2;

            int y = startY;
            for (int i = start; i < end; i++, y += cardH + gap) {
                int bx = cx - cardW / 2;
                if (mx >= bx && mx <= bx + cardW && my >= y && my <= y + cardH) {
                    int sid = listData.serverIds[i];
                    if (confirmSid != sid) {
                        confirmSid = sid;
                        return true;
                    } else {
                        PacketHandler.INSTANCE.sendToServer(new JoinMatchServerPacket(sid));
                        confirmSid = -1;
                        onClose();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    // ====== UTILITY ======

    private static ResourceLocation getTexture(String mapName, String worldPath, String prefix) {
        if (mapName == null || worldPath == null || worldPath.isEmpty()) return null;
        return imageCache.computeIfAbsent(prefix + mapName, name -> {
            try {
                Path iconFile = Paths.get(worldPath, "icon.png");
                if (!Files.exists(iconFile)) return null;
                try (FileInputStream fis = new FileInputStream(iconFile.toFile())) {
                    com.mojang.blaze3d.platform.NativeImage img =
                            com.mojang.blaze3d.platform.NativeImage.read(fis);
                    DynamicTexture tex = new DynamicTexture(img);
                    ResourceLocation loc = ResourceLocation.tryParse("pwp_lobby:" + prefix + mapName.replaceAll("[^a-zA-Z0-9_]", "_"));
                    if (loc == null) return null;
                    Minecraft.getInstance().getTextureManager().register(loc, tex);
                    return loc;
                }
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static String formatDuration(int secs) {
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