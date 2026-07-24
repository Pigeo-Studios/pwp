package com.pwp.coreclient.gui.screens;

import com.mojang.blaze3d.platform.NativeImage;
import com.pwp.coreclient.gui.StatsScreen;
import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.*;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PWPLobbyScreen extends Screen {

    private static PWPLobbyScreen instance;

    private static final int TAB_MATCHES = 0;
    private static final int TAB_VOTING = 1;
    private static final int TAB_STATS = 2;

    private int selectedTab;
    private PWPTabs tabs;
    private PWPScrollPanel scrollPanel;
    private PWPToastManager toastManager;

    private final long openTime;
    private boolean closing;
    private long closeStartTime;

    private OpenMatchListScreenPacket listData;
    private OpenVotingScreenPacket voteData;
    private OpenModeVotePacket modeVoteData;
    private OpenMatchScreenPacket matchData;

    private String votedMap;
    private String votedMode;
    private long voteOpenedAt;
    private long modeVoteOpenedAt;

    private StatsScreen statsRenderer;
    private boolean statsInitDone;

    private boolean listLoadFailed;
    private boolean listLoading;

    private static final Map<String, ResourceLocation> imageCache = new HashMap<>();

    public PWPLobbyScreen() {
        super(Component.literal("ЛОББИ"));
        this.openTime = System.currentTimeMillis();
        instance = this;
        if (listData == null) {
            listLoading = true;
        }
    }

    public static void openMatch(OpenMatchScreenPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance != null) {
            instance.matchData = pkt;
            instance.selectedTab = TAB_MATCHES;
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.matchData = pkt;
            s.selectedTab = TAB_MATCHES;
            mc.setScreen(s);
        }
    }

    public static void updateVote(OpenVotingScreenPacket pkt) {
        if (instance == null) return;
        instance.voteData = pkt;
        instance.voteOpenedAt = System.currentTimeMillis();
    }

    public static void openModeVote(OpenModeVotePacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance != null) {
            instance.modeVoteData = pkt;
            instance.modeVoteOpenedAt = System.currentTimeMillis();
            instance.votedMode = null;
            instance.selectedTab = TAB_VOTING;
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.modeVoteData = pkt;
            s.modeVoteOpenedAt = System.currentTimeMillis();
            s.selectedTab = TAB_VOTING;
            mc.setScreen(s);
        }
    }

    public static void openVote(OpenVotingScreenPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance != null) {
            instance.modeVoteData = null;
            instance.voteData = pkt;
            instance.voteOpenedAt = System.currentTimeMillis();
            instance.selectedTab = TAB_VOTING;
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.modeVoteData = null;
            s.voteData = pkt;
            s.voteOpenedAt = System.currentTimeMillis();
            s.selectedTab = TAB_VOTING;
            mc.setScreen(s);
        }
    }

    public static void openList(OpenMatchListScreenPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance != null) {
            instance.listData = pkt;
            instance.listLoading = false;
            instance.listLoadFailed = false;
            instance.selectedTab = TAB_MATCHES;
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.listData = pkt;
            s.listLoading = false;
            s.listLoadFailed = false;
            s.selectedTab = TAB_MATCHES;
            mc.setScreen(s);
        }
    }

    public static void updateList(OpenMatchListScreenPacket pkt) {
        if (instance == null) return;
        instance.listData = pkt;
        instance.listLoading = false;
        instance.listLoadFailed = false;
    }

    public static void resetInstance() {
        instance = null;
        imageCache.clear();
    }

    @Override
    protected void init() {
        super.init();
        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);

        clearWidgets();

        if (statsInitDone && statsRenderer != null) {
            statsRenderer.setPanelSize(width, height);
        }

        tabs = new PWPTabs(cx, PWPPanel.titleHeight() + 8, contentW, selectedTab, idx -> {
            selectedTab = idx;
            if (idx == TAB_MATCHES) {
                initScrollPanel(cx, contentW);
            }
            if (idx == TAB_STATS && !statsInitDone) {
                initStats();
            }
        });
        tabs.setTabs(List.of("Матчи", "Голосование", "Статистика"));
        tabs.getWidgets().forEach(this::addRenderableWidget);

        int panelY = panelTop();
        int panelH = height - panelY - 30;

        if (selectedTab == TAB_MATCHES) {
            initScrollPanel(cx, contentW);
        }

        if (selectedTab == TAB_STATS && !statsInitDone) {
            initStats();
        }
    }

    private void initScrollPanel(int cx, int contentW) {
        int panelY = panelTop();
        int panelH = height - panelY - 30;
        int scrollX = cx - contentW / 2;
        scrollPanel = new PWPScrollPanel(scrollX, panelY, contentW, panelH);
    }

    private void initStats() {
        if (statsRenderer == null) {
            statsRenderer = new StatsScreen();
        }
        statsRenderer.setPanelSize(width, height);
        statsInitDone = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (statsRenderer != null) {
            statsRenderer.tick();
        }
        if (closing) {
            long elapsed = System.currentTimeMillis() - closeStartTime;
            if (elapsed >= 150) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == this) {
                    onCloseImmediate();
                    mc.setScreen(new PWPMainMenuScreen());
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        PWPLayout.renderHeader(gui, "ЛОББИ", width);

        if (tabs != null) {
            tabs.render(gui, mouseX, mouseY, partialTick);
        }

        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);
        int panelY = panelTop();
        int panelH = height - panelY - 30;

        PWPPanel.render(gui, cx - contentW / 2, panelY, contentW, panelH);
        gui.enableScissor(cx - contentW / 2, panelY, cx + contentW / 2, panelY + panelH);

        switch (selectedTab) {
            case TAB_MATCHES -> renderMatchesTab(gui, mouseX, mouseY, cx, panelY, contentW, panelH);
            case TAB_VOTING -> renderVotingTab(gui, mouseX, mouseY, cx, panelY, contentW, panelH);
            case TAB_STATS -> renderStatsTab(gui, mouseX, mouseY, cx, panelY, contentW, panelH);
        }

        gui.disableScissor();

        if (scrollPanel != null && selectedTab == TAB_MATCHES) {
            scrollPanel.renderScrollbar(gui);
        }

        PWPToastManager.render(gui);

        renderOverlayFade(gui);

        int navY = height - 26;
        boolean backHovered = mouseX >= width / 2 - 40 && mouseX <= width / 2 + 40 && mouseY >= navY && mouseY <= navY + 20;
        int backColor = backHovered ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM;
        gui.drawCenteredString(font, Component.literal("< Назад"), width / 2, navY + 6, backColor);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderOverlayFade(GuiGraphics gui) {
        if (closing) {
            long elapsed = System.currentTimeMillis() - closeStartTime;
            float t = Math.min(elapsed / 150.0F, 1);
            float alpha = 1.0F - Easing.easeOutCubic(t);
            gui.fill(0, 0, width, height, PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.BACKGROUND, alpha));
        } else {
            long elapsed = System.currentTimeMillis() - openTime;
            if (elapsed < 150) {
                float t = elapsed / 150.0F;
                float alpha = Easing.easeOutCubic(t);
                gui.fill(0, 0, width, height, PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.BACKGROUND, 1.0F - alpha));
            }
        }
    }

    private void renderMatchesTab(GuiGraphics gui, int mx, int my, int cx, int panelY, int contentW, int panelH) {
        if (listLoading && listData == null) {
            renderSkeletons(gui, cx, panelY, contentW, panelH);
            return;
        }

        if (listLoadFailed) {
            PWPErrorState.render(gui, "Ошибка загрузки", "Проверьте подключение к серверу", cx, panelY + panelH / 2);
            return;
        }

        int bx = cx - contentW / 2;
        int scrollOff = scrollPanel != null ? (int) -scrollPanel.getScrollOffset() : 0;
        int y = panelY + 4 + scrollOff;

        if (matchData != null) {
            y = renderCurrentMatch(gui, bx, y, contentW, mx, my);
            y += 8;
        }

        if (listData == null || listData.count == 0) {
            if (scrollPanel != null) scrollPanel.setContentHeight(0);
            if (matchData == null) {
                String msg = "Нет активных матчей";
                PWPEmptyState.render(gui, msg, "Попробуйте позже или переключитесь на вкладку голосования", cx, panelY + panelH / 2);
            }
            return;
        }

        int entryH = PWPMatchCard.cardHeight();
        int gap = 8;
        int totalH = entryH * listData.count + gap * (listData.count - 1);
        if (scrollPanel != null) {
            scrollPanel.setContentHeight(totalH + 12);
        }

        for (int i = 0; i < listData.count; i++) {
            y = renderMatchEntry(gui, bx, y, contentW, entryH, i, mx, my);
            y += gap;
        }
    }

    private void renderSkeletons(GuiGraphics gui, int cx, int panelY, int contentW, int panelH) {
        long now = System.currentTimeMillis();
        int cardW = Math.min(360, contentW - 16);
        int cardH = PWPMatchCard.cardHeight();
        int gap = 8;
        int cols = Math.max(1, (contentW + gap) / (cardW + gap));
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridX = cx - gridW / 2;
        int limit = 4;

        for (int i = 0; i < limit; i++) {
            int row = i / cols;
            int col = i % cols;
            int sx = gridX + col * (cardW + gap);
            int sy = panelY + 4 + row * (cardH + gap);
            PWPSkeleton.render(gui, sx, sy, cardW, cardH, now + i * 200);
        }
    }

    private int renderCurrentMatch(GuiGraphics gui, int bx, int y, int w, int mx, int my) {
        int h = 72;
        boolean hovered = PWPCard.isHovered(bx, y, w, h, mx, my);
        ResourceLocation preview = getTexture(matchData.mapDisplayName, null, "current_");
        PWPMatchCard.render(gui, bx, y, w, h,
            matchData.mapDisplayName, matchData.modeDisplayName,
            formatDuration(matchData.remainingSeconds),
            matchData.onlinePlayers, 50,
            formatFaction(matchData.blueFaction), formatFaction(matchData.redFaction),
            matchData.blueTickets, matchData.redTickets,
            "PLAYING".equals(matchData.status) ? PWPMatchCard.Status.PLAYING : PWPMatchCard.Status.WAITING,
            preview, true, hovered);
        return y + h;
    }

    private int renderMatchEntry(GuiGraphics gui, int bx, int y, int w, int h, int index, int mx, int my) {
        boolean hovered = PWPMatchCard.isHovered(bx, y, w, h, mx, my);
        boolean isPlaying = "PLAYING".equals(listData.statuses[index]);
        boolean isStarting = "STARTING".equals(listData.statuses[index]);
        int max = listData.maxPlayers[index];
        int cur = listData.playerCounts[index];
        boolean isFull = cur >= max;

        PWPMatchCard.Status status;
        if (isPlaying && isFull) status = PWPMatchCard.Status.FULL;
        else if (isPlaying) status = PWPMatchCard.Status.PLAYING;
        else if (isStarting) status = PWPMatchCard.Status.STARTING;
        else status = PWPMatchCard.Status.WAITING;

        ResourceLocation preview = getTexture(listData.mapNames[index], listData.worldPaths[index], "match_");

        PWPMatchCard.render(gui, bx, y, w, h,
            listData.displayNames[index], "",
            formatDuration(listData.elapsedSeconds[index]),
            cur, max,
            formatFaction(listData.blueFactions[index]), formatFaction(listData.redFactions[index]),
            listData.blueTickets[index], listData.redTickets[index],
            status, preview, false, hovered);
        return y + h;
    }

    private void renderVotingTab(GuiGraphics gui, int mx, int my, int cx, int panelY, int contentW, int panelH) {
        if (modeVoteData != null && modeVoteData.modeNames.length > 0) {
            renderModeVotePanel(gui, mx, my, cx, panelY, contentW, panelH);
            return;
        }

        if (voteData == null || voteData.mapNames.length == 0) {
            PWPEmptyState.render(gui, "Голосование не активно", null, cx, panelY + panelH / 2);
            return;
        }

        int remaining = voteData.remainingSeconds - (int) ((System.currentTimeMillis() - voteOpenedAt) / 1000);
        if (remaining < 0) remaining = 0;
        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        String info = "\u00a77Осталось: \u00a7e" + timeStr + "  \u00a77|  \u00a7e" + voteData.onlinePlayers + "\u00a77 онлайн";
        if (votedMap != null) {
            info += "  \u00a7a\u2714 " + votedMap;
        } else {
            info += "  \u00a7e" + voteData.totalVotes + "\u00a77/" + voteData.onlinePlayers + " проголосовало";
        }
        gui.drawCenteredString(font, Component.literal(info), cx, panelY + 6, PWPTheme.Colors.TEXT_PRIMARY);

        int cardW = Math.min(320, width - 60);
        int cardH = 66;
        int gap = 8;
        int cols = Math.max(1, (contentW + gap) / (cardW + gap));
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridX = cx - gridW / 2;
        int gridY = panelY + 22;
        boolean voteFinished = remaining <= 0;

        for (int i = 0; i < voteData.mapNames.length; i++) {
            int row = i / cols;
            int col = i % cols;
            int ix = gridX + col * (cardW + gap);
            int iy = gridY + row * (cardH + gap);

            boolean sel = voteData.mapNames[i].equals(votedMap);
            boolean isLeader = voteData.mapNames[i].equals(voteData.leaderName) && voteData.totalVotes > 0;
            boolean hover = mx >= ix && mx <= ix + cardW && my >= iy && my <= iy + cardH;
            boolean disabled = voteFinished || (votedMap != null && !sel);

            PWPCard.State state = disabled ? PWPCard.State.DISABLED
                : PWPCard.getState(sel, hover || isLeader);
            PWPCard.render(gui, ix, iy, cardW, cardH, state);

            int imgSize = 48;
            int imgX = ix + 6;
            int imgY = iy + (cardH - imgSize) / 2;
            gui.fill(imgX, imgY, imgX + imgSize, imgY + imgSize, 0xFF000000);

            ResourceLocation tex = getTexture(voteData.mapNames[i], voteData.worldPaths[i], "vote_");
            if (tex != null) {
                gui.blit(tex, imgX + 1, imgY + 1, 0, 0, imgSize - 2, imgSize - 2, imgSize - 2, imgSize - 2);
            }

            var f = Minecraft.getInstance().font;
            int textX = imgX + imgSize + 10;
            int textMaxW = ix + cardW - textX - 6;

            String nameStr = (sel ? "\u00a7e\u2714 " : isLeader ? "\u00a76\u265B " : "") + voteData.mapDisplayNames[i];
            gui.drawString(f, nameStr, textX, iy + 4, 0xFFFFFF);

            if (voteData.mapDescriptions[i] != null && !voteData.mapDescriptions[i].isEmpty()) {
                String desc = f.plainSubstrByWidth(voteData.mapDescriptions[i], textMaxW);
                gui.drawString(f, "\u00a77" + desc, textX, iy + 16, PWPTheme.Colors.TEXT_SECONDARY);
            }

            int votes = voteData.voteCounts[i];
            int barX = textX;
            int barY = iy + 40;
            int barW = ix + cardW - textX - 6;
            int barH = 5;
            int maxV = 0;
            for (int v : voteData.voteCounts) { if (v > maxV) maxV = v; }

            gui.fill(barX, barY, barX + barW, barY + barH, PWPTheme.Styles.Progress.BG);
            if (votes > 0 && maxV > 0) {
                float pct = (float) votes / maxV;
                int fillCol = isLeader ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.ACCENT_DIM;
                gui.fill(barX, barY, barX + (int) (barW * pct), barY + barH, fillCol);
            }

            String voteText = "\u00a7e" + votes + "\u00a77 голосов";
            if (voteData.totalVotes > 0) {
                voteText += " (" + (votes * 100 / voteData.totalVotes) + "%)";
            }
            gui.drawString(f, voteText, barX, barY + barH + 1, PWPTheme.Colors.TEXT_SECONDARY);
        }
    }

    private void renderModeVotePanel(GuiGraphics gui, int mx, int my, int cx, int panelY, int contentW, int panelH) {
        if (modeVoteData == null || modeVoteData.modeNames.length == 0) {
            PWPEmptyState.render(gui, "Голосование за режим не активно", null, cx, panelY + panelH / 2);
            return;
        }

        int remaining = modeVoteData.remainingSeconds - (int) ((System.currentTimeMillis() - modeVoteOpenedAt) / 1000);
        if (remaining < 0) remaining = 0;
        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        String info = "\u00a77Осталось: \u00a7e" + timeStr + "  \u00a77|  \u00a7e" + modeVoteData.onlinePlayers + "\u00a77 онлайн";
        if (votedMode != null) {
            info += "  \u00a7a\u2714 " + votedMode;
        } else {
            info += "  \u00a7e" + modeVoteData.totalVotes + "\u00a77/" + modeVoteData.onlinePlayers + " проголосовало";
        }
        gui.drawCenteredString(font, Component.literal(info), cx, panelY + 6, PWPTheme.Colors.TEXT_PRIMARY);

        int cardW = Math.min(340, width - 40);
        int cardH = 80;
        int gap = 12;
        int totalH = modeVoteData.modeNames.length * (cardH + gap);
        int startY = panelY + 22 + (panelH - 22 - totalH) / 2;
        boolean voteFinished = remaining <= 0;

        for (int i = 0; i < modeVoteData.modeNames.length; i++) {
            int iy = startY + i * (cardH + gap);
            int ix = cx - cardW / 2;

            boolean sel = modeVoteData.modeNames[i].equals(votedMode);
            boolean hover = mx >= ix && mx <= ix + cardW && my >= iy && my <= iy + cardH;
            boolean disabled = voteFinished || (votedMode != null && !sel);

            PWPCard.State state = disabled ? PWPCard.State.DISABLED : PWPCard.getState(sel, hover);
            PWPCard.render(gui, ix, iy, cardW, cardH, state);

            var f = Minecraft.getInstance().font;
            int textX = ix + 14;

            String nameStr = (sel ? "\u00a7e\u2714 " : "\u00a7f") + modeVoteData.modeDisplayNames[i];
            gui.drawString(f, nameStr, textX, iy + 8, 0xFFFFFF);

            if (modeVoteData.modeDescriptions[i] != null && !modeVoteData.modeDescriptions[i].isEmpty()) {
                String d = f.plainSubstrByWidth(modeVoteData.modeDescriptions[i], cardW - 28);
                gui.drawString(f, "\u00a77" + d, textX, iy + 22, PWPTheme.Colors.TEXT_SECONDARY);
            }

            int votes = modeVoteData.voteCounts[i];
            int barX = textX;
            int barY = iy + 52;
            int barW = cardW - 28;
            int maxV = 0;
            for (int v : modeVoteData.voteCounts) { if (v > maxV) maxV = v; }

            gui.fill(barX, barY, barX + barW, barY + 6, PWPTheme.Styles.Progress.BG);
            if (votes > 0 && maxV > 0) {
                float pct = (float) votes / maxV;
                gui.fill(barX, barY, barX + (int) (barW * pct), barY + 6, PWPTheme.Colors.ACCENT);
            }

            String voteText = "\u00a7e" + votes + "\u00a77 vote" + (votes != 1 ? "s" : "");
            if (modeVoteData.totalVotes > 0) {
                voteText += " \u00a77(" + (votes * 100 / modeVoteData.totalVotes) + "%)";
            }
            gui.drawString(f, voteText, barX, barY + 8, PWPTheme.Colors.TEXT_SECONDARY);
        }
    }

    private void renderStatsTab(GuiGraphics gui, int mx, int my, int cx, int panelY, int contentW, int panelH) {
        if (statsRenderer == null) {
            PWPEmptyState.render(gui, "Загрузка...", null, cx, panelY + panelH / 2);
            return;
        }
        statsRenderer.renderContent(gui, mx, my, panelY + 2, panelH - 4);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int navY = height - 26;
        if (mx >= width / 2 - 40 && mx <= width / 2 + 40 && my >= navY && my <= navY + 20) {
            startClose();
            return true;
        }

        if (scrollPanel != null && selectedTab == TAB_MATCHES) {
            if (scrollPanel.mouseClicked(mx, my, button)) return true;
        }

        int cx = width / 2;
        int contentW = PWPLayout.contentWidth(width);
        int panelY = panelTop();
        int panelH = height - panelY - 30;

        switch (selectedTab) {
            case TAB_MATCHES -> {
                if (handleMatchesClick(mx, my, cx, panelY, contentW, panelH)) return true;
            }
            case TAB_VOTING -> {
                if (handleVotingClick(mx, my, cx, panelY, contentW, panelH)) return true;
            }
            case TAB_STATS -> {
                if (statsRenderer != null && statsRenderer.mouseClickedContent(mx, my, button)) return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (scrollPanel != null && selectedTab == TAB_MATCHES) {
            return scrollPanel.mouseScrolled(mx, my, delta) != 0;
        }
        if (selectedTab == TAB_STATS && statsRenderer != null) {
            return statsRenderer.mouseScrolled(mx, my, delta);
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (scrollPanel != null && selectedTab == TAB_MATCHES) {
            if (scrollPanel.mouseDragged(mx, my, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (scrollPanel != null && selectedTab == TAB_MATCHES) {
            if (scrollPanel.mouseReleased(mx, my, button)) return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private boolean handleMatchesClick(double mx, double my, int cx, int panelY, int contentW, int panelH) {
        if (listData == null || listData.count == 0) return false;

        int bx = cx - contentW / 2;
        int entryH = PWPMatchCard.cardHeight();
        int gap = 8;

        double scrollOff = scrollPanel != null ? scrollPanel.getScrollOffset() : 0;
        int startY = panelY + 4;
        if (matchData != null) startY += 72 + 8;

        for (int i = 0; i < listData.count; i++) {
            int y = startY + i * (entryH + gap) - (int) scrollOff;
            if (mx >= bx && mx <= bx + contentW && my >= y && my <= y + entryH) {
                if ("PLAYING".equals(listData.statuses[i])) {
                    int max = listData.maxPlayers[i];
                    int cur = listData.playerCounts[i];
                    if (cur < max) {
                        PacketHandler.INSTANCE.sendToServer(
                            new JoinMatchServerPacket(listData.serverIds[i]));
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleVotingClick(double mx, double my, int cx, int panelY, int contentW, int panelH) {
        if (modeVoteData != null && modeVoteData.modeNames.length > 0) {
            int remaining = modeVoteData.remainingSeconds - (int) ((System.currentTimeMillis() - modeVoteOpenedAt) / 1000);
            if (remaining <= 0 || votedMode != null) return false;

            int cardW = Math.min(340, width - 40);
            int cardH = 80;
            int gap = 12;
            int totalH = modeVoteData.modeNames.length * (cardH + gap);
            int startY = panelY + 22 + (panelH - 22 - totalH) / 2;

            for (int i = 0; i < modeVoteData.modeNames.length; i++) {
                int iy = startY + i * (cardH + gap);
                int ix = cx - cardW / 2;
                if (mx >= ix && mx <= ix + cardW && my >= iy && my <= iy + cardH) {
                    votedMode = modeVoteData.modeNames[i];
                    PacketHandler.INSTANCE.sendToServer(new VoteModePacket(modeVoteData.modeNames[i]));
                    PWPToastManager.show("Голос принят: " + modeVoteData.modeDisplayNames[i], PWPToastManager.ToastType.SUCCESS);
                    return true;
                }
            }
            return false;
        }

        if (voteData == null || voteData.mapNames.length == 0) return false;

        int remaining = voteData.remainingSeconds - (int) ((System.currentTimeMillis() - voteOpenedAt) / 1000);
        if (remaining <= 0 || votedMap != null) return false;

        int cardW = Math.min(320, width - 60);
        int cardH = 66;
        int gap = 8;
        int cols = Math.max(1, (contentW + gap) / (cardW + gap));
        int gridW = cols * cardW + (cols - 1) * gap;
        int gridX = cx - gridW / 2;
        int gridY = panelY + 22;

        for (int i = 0; i < voteData.mapNames.length; i++) {
            int row = i / cols;
            int col = i % cols;
            int ix = gridX + col * (cardW + gap);
            int iy = gridY + row * (cardH + gap);

            if (mx >= ix && mx <= ix + cardW && my >= iy && my <= iy + cardH) {
                votedMap = voteData.mapNames[i];
                PacketHandler.INSTANCE.sendToServer(new VoteMapPacket(voteData.mapNames[i]));
                PWPToastManager.show("Голос принят: " + voteData.mapDisplayNames[i], PWPToastManager.ToastType.SUCCESS);
                return true;
            }
        }
        return false;
    }

    private void startClose() {
        if (closing) return;
        closing = true;
        closeStartTime = System.currentTimeMillis();
    }

    private void onCloseImmediate() {
        instance = null;
        imageCache.clear();
    }

    @Override
    public void onClose() {
        super.onClose();
        instance = null;
        imageCache.clear();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelTop() {
        return PWPPanel.titleHeight() + 34;
    }

    private static ResourceLocation getTexture(String mapName, String worldPath, String prefix) {
        if (mapName == null || worldPath == null || worldPath.isEmpty()) return null;
        return imageCache.computeIfAbsent(prefix + mapName, name -> {
            try {
                Path iconFile = Paths.get(worldPath, "icon.png");
                if (!Files.exists(iconFile)) return null;
                try (FileInputStream fis = new FileInputStream(iconFile.toFile())) {
                    NativeImage img = NativeImage.read(fis);
                    DynamicTexture tex = new DynamicTexture(img);
                    ResourceLocation loc = ResourceLocation.tryParse("pwp_core_client:" + prefix + mapName.replaceAll("[^a-zA-Z0-9_]", "_"));
                    if (loc == null) return null;
                    Minecraft.getInstance().getTextureManager().register(loc, tex);
                    return loc;
                }
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static String formatFaction(String faction) {
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
}
