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

    private static final int TAB_VOTE = 0;
    private static final int TAB_LIST = 1;

    private int currentTab = TAB_LIST;
    private int targetTab = TAB_LIST;
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

    // Scroll
    private double scrollOffset = 0;
    private boolean isScrolling = false;
    private double scrollDragStartY = 0;
    private double scrollDragStartOff = 0;

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
                instance.switchTab(TAB_LIST);
            }
        } else {
            LobbyScreen s = new LobbyScreen();
            s.matchData = pkt;
            s.currentTab = TAB_LIST;
            s.targetTab = TAB_LIST;
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
            scrollOffset = 0;
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

        // Tab: Vote
        addRenderableWidget(Button.builder(
                Component.literal("Vote"),
                b -> { if (voteData != null) switchTab(TAB_VOTE); })
                .bounds(cx - 90, 6, 60, 22).build());

        // Tab: Matches
        addRenderableWidget(Button.builder(
                Component.literal("Matches"),
                b -> switchTab(TAB_LIST))
                .bounds(cx - 26, 6, 68, 22).build());

        // Stats button
        addRenderableWidget(Button.builder(
                Component.literal("\u2694 Stats"),
                b -> Minecraft.getInstance().setScreen(new StatsScreen()))
                .bounds(cx + 46, 6, 60, 22).build());

        // Close button
        addRenderableWidget(Button.builder(
                Component.literal("\u2715"),
                b -> onClose())
                .bounds(cx + 110, 6, 22, 22).build());

        // Vote tab controls
        if (voteData != null && voteData.mapNames.length > 0) {
            int totalVotePages = Math.max(1, (voteData.mapNames.length + 3) / 4);
            if (totalVotePages > 1) {
                int pageY = 32;
                addRenderableWidget(Button.builder(
                        Component.literal("\u25C0"),
                        b -> { if (votePage > 0) votePage--; })
                        .bounds(cx + 40, pageY, 18, 18).build());
                addRenderableWidget(Button.builder(
                        Component.literal("\u25B6"),
                        b -> { if (votePage < totalVotePages - 1) votePage++; })
                        .bounds(cx + 84, pageY, 18, 18).build());
            }
        }

        // List tab controls
        if (listData != null && listData.count > 0) {
            int totalListPages = Math.max(1, (listData.count + 3) / 4);
            if (totalListPages > 1) {
                int pageY = 32;
                addRenderableWidget(Button.builder(
                        Component.literal("\u25C0"),
                        b -> { if (listPage > 0) listPage--; })
                        .bounds(cx + 40, pageY, 18, 18).build());
                addRenderableWidget(Button.builder(
                        Component.literal("\u25B6"),
                        b -> { if (listPage < totalListPages - 1) listPage++; })
                        .bounds(cx + 84, pageY, 18, 18).build());
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
        String title = currentTab == TAB_VOTE ? "\u2694 MAP VOTE" : "\u2694 ACTIVE MATCHES";
        gui.drawCenteredString(font, PWPTheme.Icons.SWORDS + " " + title, cx, 32, PWPTheme.Colors.TEXT_ACCENT);

        // Draw accent line under title
        int titleW = font.width(title) + 20;
        gui.fill(cx - titleW / 2, 42, cx + titleW / 2, 43, PWPTheme.Colors.ACCENT);

        // Animate content alpha
        float alpha = currentTab == targetTab ? 1f : animProgress;
        if (currentTab != targetTab) {
            alpha = 1f - animProgress;
        }
        int a = Math.max(4, Math.min(255, (int)(alpha * 255)));
        if (a < 4) return;

        RenderSystem.enableBlend();

        if (currentTab == TAB_VOTE) {
            renderVoteTab(gui, mx, my, cx, a);
        } else if (currentTab == TAB_LIST) {
            renderListTab(gui, mx, my, cx, a);
        }

        RenderSystem.disableBlend();
    }

    // ====== VOTE TAB ======

    private void renderVoteTab(GuiGraphics gui, int mx, int my, int cx, int a) {
        if (voteData == null || voteData.mapNames.length == 0) {
            gui.drawCenteredString(font, "\u00a77No vote in progress", cx, height / 2, PWPTheme.Colors.TEXT_SECONDARY);
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
        gui.drawCenteredString(font, info, cx, 54, 0xFFFFFF);

        int cardsPerPage = 4;
        int cardW = Math.min(320, width - 60);
        int cardH = 66;
        int gap = 8;

        int start = votePage * cardsPerPage;
        int end = Math.min(start + cardsPerPage, voteData.mapNames.length);
        int contentH = (end - start) * (cardH + gap);
        int startY = 64 + (height - 64 - contentH - 40) / 2;

        int y = startY;
        for (int i = start; i < end; i++, y += cardH + gap) {
            boolean sel = voteData.mapNames[i].equals(votedMap);
            boolean isLeader = voteData.mapNames[i].equals(voteData.leaderName) && voteData.totalVotes > 0;
            boolean hover = mx >= cx - cardW / 2 && mx <= cx + cardW / 2 && my >= y && my <= y + cardH;
            int bx = cx - cardW / 2;

            int cardBg;
            if (sel) cardBg = PWPTheme.Styles.Card.BG_SELECTED;
            else if (hover) cardBg = PWPTheme.Styles.Card.BG_HOVER;
            else cardBg = PWPTheme.Styles.Card.BG;
            gui.fill(bx, y, bx + cardW, y + cardH, PWPTheme.Colors.withAlpha(cardBg, a));

            int borderCol;
            if (sel) borderCol = PWPTheme.Styles.Card.BORDER_SELECTED;
            else if (isLeader) borderCol = PWPTheme.Colors.ACCENT;
            else if (hover) borderCol = PWPTheme.Styles.Card.BORDER_HOVER;
            else borderCol = PWPTheme.Styles.Card.BORDER;
            gui.fill(bx, y, bx + cardW, y + 1, PWPTheme.Colors.withAlpha(borderCol, a));
            gui.fill(bx, y + cardH - 1, bx + cardW, y + cardH, PWPTheme.Colors.withAlpha(borderCol, a));
            gui.fill(bx, y, bx + 1, y + cardH, PWPTheme.Colors.withAlpha(borderCol, a));
            gui.fill(bx + cardW - 1, y, bx + cardW, y + cardH, PWPTheme.Colors.withAlpha(borderCol, a));

            int imgSize = 48;
            int imgX = bx + 8;
            int imgY = y + (cardH - imgSize) / 2;
            gui.fill(imgX, imgY, imgX + imgSize, imgY + imgSize, PWPTheme.Colors.withAlpha(0xFF000000, a));

            ResourceLocation tex = getTexture(voteData.mapNames[i], voteData.worldPaths[i], "vote_");
            if (tex != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, a / 255f);
                gui.blit(tex, imgX + 1, imgY + 1, 0, 0, imgSize - 2, imgSize - 2, imgSize - 2, imgSize - 2);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            int textX = imgX + imgSize + 12;
            int textMaxW = bx + cardW - textX - 8;

            String nameStr = (isLeader ? "\u00a76\u265B " : (sel ? "\u00a7e\u2714 " : "\u00a7f")) + voteData.mapDisplayNames[i];
            gui.drawString(font, nameStr, textX, y + 6, PWPTheme.Colors.withAlpha(0xFFFFFF, a));

            if (voteData.mapDescriptions[i] != null && !voteData.mapDescriptions[i].isEmpty()) {
                String desc = voteData.mapDescriptions[i];
                if (font.width(desc) > textMaxW) {
                    desc = font.plainSubstrByWidth(desc, textMaxW - 4) + "...";
                }
                gui.drawString(font, "\u00a77" + desc, textX, y + 18, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, a));
            }

            String factionLine = "\u00a79" + formatFactionName(voteData.blueFactions[i]) + " \u00a77vs \u00a7c" + formatFactionName(voteData.redFactions[i]);
            int flw = font.width(factionLine);
            if (flw > textMaxW) {
                factionLine = font.plainSubstrByWidth(factionLine, textMaxW - 4) + "...";
            }
            gui.drawString(font, factionLine, textX, y + 30, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, a));

            int votes = voteData.voteCounts[i];
            int barX = textX;
            int barY = y + 44;
            int barW = bx + cardW - textX - 8;
            int barH = 5;
            int maxVotes = 0;
            for (int j = 0; j < voteData.voteCounts.length; j++) {
                if (voteData.voteCounts[j] > maxVotes) maxVotes = voteData.voteCounts[j];
            }

            gui.fill(barX, barY, barX + barW, barY + barH, PWPTheme.Colors.withAlpha(PWPTheme.Styles.Progress.BG, a));
            if (votes > 0 && maxVotes > 0) {
                float pct = (float) votes / maxVotes;
                int fillW = (int) (barW * pct);
                int fillCol = isLeader ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.ACCENT_DIM;
                gui.fill(barX, barY, barX + fillW, barY + barH, PWPTheme.Colors.withAlpha(fillCol, a));
            }

            String voteText = "\u00a7e" + votes + "\u00a77" + (votes != 1 ? " votes" : " vote");
            if (voteData.totalVotes > 0) {
                int pct = votes * 100 / voteData.totalVotes;
                voteText += " (" + pct + "%)";
            }
            gui.drawString(font, voteText, barX, barY + barH + 2, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, a));
        }
    }

    // ====== LIST TAB (includes current match info + active matches list) ======

    private void renderListTab(GuiGraphics gui, int mx, int my, int cx, int a) {
        int contentY = 50;
        int contentW = Math.min(340, width - 60);
        int bx = cx - contentW / 2;

        // Current match info panel (if available)
        if (matchData != null) {
            int panelH = 76;
            int panelBg = PWPTheme.Colors.withAlpha(PWPTheme.Styles.Panel.BG, a);
            int panelBorder = PWPTheme.Colors.withAlpha(PWPTheme.Styles.Panel.BORDER, a);
            int textCol = PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_PRIMARY, a);
            int secCol = PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, a);

            gui.fill(bx, contentY, bx + contentW, contentY + panelH, panelBg);
            gui.fill(bx, contentY, bx + contentW, contentY + 1, panelBorder);
            gui.fill(bx, contentY + panelH - 1, bx + contentW, contentY + panelH, panelBorder);
            gui.fill(bx, contentY, bx + 1, contentY + panelH, panelBorder);
            gui.fill(bx + contentW - 1, contentY, bx + contentW, contentY + panelH, panelBorder);

            int titleCol = PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_ACCENT, a);
            gui.drawCenteredString(font, "\u00a7lCURRENT MATCH", cx, contentY + 4, titleCol);

            int ly = contentY + 18;
            gui.drawCenteredString(font, "\u00a7fMap: \u00a7e" + matchData.mapDisplayName, cx, ly, textCol);
            ly += 12;
            gui.drawCenteredString(font, "\u00a7fMode: \u00a7e" + matchData.modeDisplayName, cx, ly, textCol);
            ly += 12;

            String factionStr = "\u00a79" + formatFactionName(matchData.blueFaction) + " \u00a77vs \u00a7c" + formatFactionName(matchData.redFaction);
            String ticketsStr = "\u00a79" + matchData.blueTickets + " \u00a77| \u00a7c" + matchData.redTickets;
            gui.drawCenteredString(font, factionStr + "  \u00a77(" + ticketsStr + ")", cx, ly, secCol);

            contentY += panelH + 10;
        }

        // Active matches list
        if (listData == null || listData.count == 0) {
            String msg = matchData == null ? "\u00a77No active matches" : "";
            if (!msg.isEmpty()) {
                gui.drawCenteredString(font, msg, cx, contentY + 20, PWPTheme.Colors.TEXT_SECONDARY);
            }
            return;
        }

        gui.drawCenteredString(font, "\u00a77Click a match to join", cx, contentY, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, a));
        contentY += 12;

        int cardsPerPage = 4;
        int cardH = 68;
        int gap = 8;

        int start = listPage * cardsPerPage;
        int end = Math.min(start + cardsPerPage, listData.count);

        int y = contentY;
        for (int i = start; i < end; i++, y += cardH + gap) {
            boolean isPlaying = "PLAYING".equals(listData.statuses[i]);
            boolean isStarting = "STARTING".equals(listData.statuses[i]);
            boolean hover = mx >= bx && mx <= bx + contentW && my >= y && my <= y + cardH;
            boolean isConfirm = confirmSid != -1 && listData.serverIds[i] == confirmSid;

            int cardBg;
            if (isConfirm) cardBg = PWPTheme.Styles.Card.BG_SELECTED;
            else if (hover) cardBg = PWPTheme.Styles.Card.BG_HOVER;
            else cardBg = PWPTheme.Styles.Card.BG;
            gui.fill(bx, y, bx + contentW, y + cardH, PWPTheme.Colors.withAlpha(cardBg, a));

            int borderCol;
            if (isConfirm) borderCol = PWPTheme.Styles.Card.BORDER_SELECTED;
            else if (isPlaying) borderCol = PWPTheme.Colors.SUCCESS;
            else if (hover) borderCol = PWPTheme.Styles.Card.BORDER_HOVER;
            else borderCol = PWPTheme.Styles.Card.BORDER;
            gui.fill(bx, y, bx + contentW, y + 1, PWPTheme.Colors.withAlpha(borderCol, a));
            gui.fill(bx, y + cardH - 1, bx + contentW, y + cardH, PWPTheme.Colors.withAlpha(borderCol, a));
            gui.fill(bx, y, bx + 1, y + cardH, PWPTheme.Colors.withAlpha(borderCol, a));
            gui.fill(bx + contentW - 1, y, bx + contentW, y + cardH, PWPTheme.Colors.withAlpha(borderCol, a));

            int imgSize = 48;
            int imgX = bx + 8;
            int imgY = y + (cardH - imgSize) / 2;
            gui.fill(imgX, imgY, imgX + imgSize, imgY + imgSize, PWPTheme.Colors.withAlpha(0xFF000000, a));

            ResourceLocation tex = getTexture(listData.mapNames[i], listData.worldPaths[i], "list_");
            if (tex != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, a / 255f);
                gui.blit(tex, imgX + 1, imgY + 1, 0, 0, imgSize - 2, imgSize - 2, imgSize - 2, imgSize - 2);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            int textX = imgX + imgSize + 12;
            int textMaxW = bx + contentW - textX - 8;

            String nameStr = (isPlaying ? "\u00a7a\u25CF " : "\u00a7e\u25B6 ") + listData.displayNames[i];
            gui.drawString(font, nameStr, textX, y + 5, PWPTheme.Colors.withAlpha(0xFFFFFF, a));

            String elapsed = formatDuration(listData.elapsedSeconds[i]);
            gui.drawString(font, "\u00a77" + elapsed + "  |  \u00a7e" + listData.playerCounts[i] + "\u00a77/" + listData.maxPlayers[i],
                textX, y + 17, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, a));

            String factionStr = "\u00a79" + formatFactionName(listData.blueFactions[i]) + " \u00a77vs \u00a7c" + formatFactionName(listData.redFactions[i]);
            if (font.width(factionStr) > textMaxW) {
                factionStr = font.plainSubstrByWidth(factionStr, textMaxW - 4) + "...";
            }
            gui.drawString(font, factionStr, textX, y + 29, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, a));

            String ticketStr = "\u00a79" + listData.blueTickets[i] + " \u00a77| \u00a7c" + listData.redTickets[i];
            gui.drawString(font, ticketStr, textX, y + 41, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, a));

            int actionCol = isConfirm ? PWPTheme.Colors.SUCCESS : PWPTheme.Colors.TEXT_SECONDARY;
            String actionStr = isConfirm ? "\u00a7a\u2714 Click again to join"
                : (isPlaying ? "\u00a7eClick to join" : "\u00a77Starting...");
            gui.drawString(font, actionStr, textX, y + 54, PWPTheme.Colors.withAlpha(actionCol, a));
        }

        // Page indicator
        int totalPages = Math.max(1, (listData.count + cardsPerPage - 1) / cardsPerPage);
        if (totalPages > 1) {
            int navY = y + 8;
            gui.drawCenteredString(font, "\u00a77Page " + (listPage + 1) + "/" + totalPages, cx, navY, PWPTheme.Colors.TEXT_SECONDARY);
        }
    }

    // ====== CLICK HANDLING ======

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);
        int cx = width / 2;

        if (currentTab == TAB_VOTE && voteData != null) {
            int cardsPerPage = 4;
            int cardW = Math.min(320, width - 60);
            int cardH = 66;
            int gap = 8;
            int start = votePage * cardsPerPage;
            int end = Math.min(start + cardsPerPage, voteData.mapNames.length);
            int contentH = (end - start) * (cardH + gap);
            int startY = 64 + (height - 64 - contentH - 40) / 2;

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
            int contentW = Math.min(340, width - 60);
            int bx = cx - contentW / 2;
            int cardsPerPage = 4;
            int cardH = 68;
            int gap = 8;

            int baseY = 50;
            if (matchData != null) baseY += 86;

            int start = listPage * cardsPerPage;
            int end = Math.min(start + cardsPerPage, listData.count);

            int y = baseY + 12;
            for (int i = start; i < end; i++, y += cardH + gap) {
                if (mx >= bx && mx <= bx + contentW && my >= y && my <= y + cardH) {
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
