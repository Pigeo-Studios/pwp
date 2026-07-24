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
    private boolean statsOpen;

    private boolean listLoadFailed;
    private boolean listLoading;

    private static final Map<String, ResourceLocation> imageCache = new HashMap<>();

    public PWPLobbyScreen() {
        super(Component.literal("Лобби"));
        this.openTime = System.currentTimeMillis();
        instance = this;
        if (listData == null) listLoading = true;
    }

    public static void openMatch(OpenMatchScreenPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance != null) { instance.matchData = pkt; }
        else { PWPLobbyScreen s = new PWPLobbyScreen(); s.matchData = pkt; mc.setScreen(s); }
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
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.modeVoteData = pkt;
            s.modeVoteOpenedAt = System.currentTimeMillis();
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
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.modeVoteData = null;
            s.voteData = pkt;
            s.voteOpenedAt = System.currentTimeMillis();
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
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.listData = pkt;
            s.listLoading = false;
            s.listLoadFailed = false;
            mc.setScreen(s);
        }
    }

    public static void updateList(OpenMatchListScreenPacket pkt) {
        if (instance == null) return;
        instance.listData = pkt;
        instance.listLoading = false;
        instance.listLoadFailed = false;
    }

    public static void resetInstance() { instance = null; imageCache.clear(); }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        if (statsInitDone && statsRenderer != null) statsRenderer.setPanelSize(width, height);

        int leftW = (int) (width * 0.68f);
        int rightW = width - leftW - 8;
        int panelY = 32;
        int panelH = height - panelY - 30;

        scrollPanel = new PWPScrollPanel(PWPTheme.Spacing.SM, panelY, leftW - 8, panelH);
    }

    @Override
    public void tick() {
        super.tick();
        if (statsRenderer != null) statsRenderer.tick();
        if (closing) {
            long elapsed = System.currentTimeMillis() - closeStartTime;
            if (elapsed >= 150) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == this) { onCloseImmediate(); mc.setScreen(new PWPMainMenuScreen()); }
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        PWPLayout.renderHeader(gui, "Лобби", width);

        int leftW = (int) (width * 0.68f);
        int rightW = width - leftW - 8;
        int panelY = 32;
        int panelH = height - panelY - 30;

        PWPPanel.render(gui, PWPTheme.Spacing.SM, panelY, leftW - 8, panelH);
        gui.enableScissor(PWPTheme.Spacing.SM, panelY, PWPTheme.Spacing.SM + leftW - 8, panelY + panelH);
        renderMatchList(gui, mouseX, mouseY, PWPTheme.Spacing.SM, panelY, leftW - 8, panelH);
        gui.disableScissor();

        if (scrollPanel != null) scrollPanel.renderScrollbar(gui);

        int rightX = leftW + 4;
        renderSidebar(gui, mouseX, mouseY, rightX, panelY, rightW, panelH);

        PWPToastManager.render(gui);
        renderOverlayFade(gui);

        int navY = height - 26;
        boolean backHovered = mouseX >= width / 2 - 40 && mouseX <= width / 2 + 40 && mouseY >= navY && mouseY <= navY + 20;
        gui.drawCenteredString(font, Component.literal("< Назад"), width / 2, navY + 6, backHovered ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM);

        if (statsOpen && statsRenderer != null) {
            gui.fill(0, 0, width, height, PWPTheme.Colors.BACKGROUND_DIM);
            int statsPanelY = 40;
            int statsH = height - statsPanelY - 30;
            PWPPanel.render(gui, PWPTheme.Spacing.SM, statsPanelY, width - PWPTheme.Spacing.SM * 2, statsH);
            gui.enableScissor(PWPTheme.Spacing.SM, statsPanelY, width - PWPTheme.Spacing.SM * 2, statsPanelY + statsH);
            statsRenderer.renderContent(gui, mouseX, mouseY, statsPanelY + 4, statsH - 8);
            gui.disableScissor();
            gui.drawCenteredString(font, Component.literal("< Назад"), width / 2, height - 20, PWPTheme.Colors.TEXT_DIM);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderOverlayFade(GuiGraphics gui) {
        if (closing) {
            long elapsed = System.currentTimeMillis() - closeStartTime;
            float t = Math.min(elapsed / 150.0F, 1);
            gui.fill(0, 0, width, height, PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.BACKGROUND, 1.0F - Easing.easeOutCubic(t)));
        } else {
            long elapsed = System.currentTimeMillis() - openTime;
            if (elapsed < 150) {
                float t = elapsed / 150.0F;
                gui.fill(0, 0, width, height, PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.BACKGROUND, 1.0F - Easing.easeOutCubic(t)));
            }
        }
    }

    // ========== LEFT: Match List ==========

    private void renderMatchList(GuiGraphics gui, int mx, int my, int bx, int panelY, int contentW, int panelH) {
        if (listLoading && listData == null) {
            renderSkeletons(gui, bx, panelY, contentW, panelH);
            return;
        }
        if (listLoadFailed) {
            PWPErrorState.render(gui, "Ошибка загрузки", "Проверьте подключение к серверу", bx + contentW / 2, panelY + panelH / 2);
            return;
        }

        int scrollOff = scrollPanel != null ? (int) -scrollPanel.getScrollOffset() : 0;
        int y = panelY + 4 + scrollOff;

        if (matchData != null) {
            y = renderCurrentMatch(gui, bx, y, contentW, mx, my);
            y += 8;
        }

        if (listData == null || listData.count == 0) {
            if (scrollPanel != null) scrollPanel.setContentHeight(0);
            if (matchData == null) {
                PWPEmptyState.render(gui, "Нет активных матчей", "Попробуйте позже", bx + contentW / 2, panelY + panelH / 2);
            }
            return;
        }

        int entryH = PWPMatchCard.cardHeight();
        int gap = 8;
        int totalH = entryH * listData.count + gap * (listData.count - 1);
        if (matchData != null) totalH += 72 + 8;
        if (scrollPanel != null) scrollPanel.setContentHeight(totalH + 12);

        for (int i = 0; i < listData.count; i++) {
            y = renderMatchEntry(gui, bx, y, contentW, entryH, i, mx, my);
            y += gap;
        }
    }

    private void renderSkeletons(GuiGraphics gui, int bx, int panelY, int contentW, int panelH) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            PWPSkeleton.render(gui, bx + 4, panelY + 4 + i * 76, contentW - 8, 68, now + i * 200);
        }
    }

    private int renderCurrentMatch(GuiGraphics gui, int bx, int y, int w, int mx, int my) {
        int h = 72;
        boolean hovered = PWPCard.isHovered(bx, y, w, h, mx, my);
        PWPMatchCard.render(gui, bx, y, w, h,
            matchData.mapDisplayName, matchData.modeDisplayName,
            formatDuration(matchData.remainingSeconds),
            matchData.onlinePlayers, 50,
            formatFaction(matchData.blueFaction), formatFaction(matchData.redFaction),
            matchData.blueTickets, matchData.redTickets,
            "PLAYING".equals(matchData.status) ? PWPMatchCard.Status.PLAYING : PWPMatchCard.Status.WAITING,
            null, true, hovered);
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

    // ========== RIGHT: Sidebar ==========

    private void renderSidebar(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        PWPPanel.render(gui, x, panelY, w, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(x, panelY, x + w, panelY + panelH);

        if (modeVoteData != null && modeVoteData.modeNames.length > 0) {
            renderModeVoteSidebar(gui, mx, my, x, panelY, w, panelH);
        } else if (voteData != null && voteData.mapNames.length > 0) {
            renderVoteSidebar(gui, mx, my, x, panelY, w, panelH);
        } else {
            gui.drawCenteredString(font, Component.literal("Голосование"), x + w / 2, panelY + 12, PWPTheme.Colors.TEXT_DIM);
            gui.drawCenteredString(font, Component.literal("не активно"), x + w / 2, panelY + 24, PWPTheme.Colors.TEXT_DIM);
        }

        int sepY = panelY + panelH - 40;
        gui.fill(x + 8, sepY, x + w - 8, sepY + 1, PWPTheme.Colors.BORDER);
        boolean statsHover = mx >= x + 8 && mx <= x + w - 8 && my >= sepY + 6 && my <= sepY + 26;
        int statsCol = statsHover ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY;
        gui.drawCenteredString(font, Component.literal("Статистика \u2192"), x + w / 2, sepY + 10, statsCol);

        gui.disableScissor();
    }

    private void renderVoteSidebar(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        int remaining = voteData.remainingSeconds - (int) ((System.currentTimeMillis() - voteOpenedAt) / 1000);
        if (remaining < 0) remaining = 0;
        boolean voteFinished = remaining <= 0 || votedMap != null;

        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        gui.drawCenteredString(font, Component.literal("Голосование"), x + w / 2, panelY + 6, PWPTheme.Colors.TEXT_ACCENT);
        gui.drawCenteredString(font, Component.literal(timeStr + "  \u00a7e" + voteData.onlinePlayers + "\u00a77 онлайн"), x + w / 2, panelY + 18, PWPTheme.Colors.TEXT_SECONDARY);

        int ly = panelY + 32;
        int barW = w - 16;
        int maxV = 0;
        for (int v : voteData.voteCounts) { if (v > maxV) maxV = v; }

        for (int i = 0; i < voteData.mapNames.length; i++) {
            boolean sel = voteData.mapNames[i].equals(votedMap);
            boolean isLeader = voteData.mapNames[i].equals(voteData.leaderName) && voteData.totalVotes > 0;
            boolean hover = mx >= x + 8 && mx <= x + w - 8 && my >= ly - 2 && my <= ly + 24;
            boolean disabled = voteFinished && !sel;

            int bg = disabled ? PWPTheme.Colors.SURFACE_DIM : (sel ? 0xFF2A2010 : (hover ? PWPTheme.Colors.SURFACE_LIGHT : 0));
            if (bg != 0) RoundedRect.fill(gui, x + 8, ly - 2, w - 16, 28, PWPTheme.Spacing.RADIUS_SMALL, bg);

            gui.drawString(font, Component.literal(sel ? "\u2714 " : "") + voteData.mapDisplayNames[i], x + 12, ly, 0xFFFFFF);

            int votes = voteData.voteCounts[i];
            int barY = ly + 12;
            gui.fill(x + 12, barY, x + 12 + barW, barY + 4, PWPTheme.Styles.Progress.BG);
            if (votes > 0 && maxV > 0) {
                float pct = (float) votes / maxV;
                int fillCol = isLeader ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.ACCENT_DIM;
                gui.fill(x + 12, barY, x + 12 + (int) (barW * pct), barY + 4, fillCol);
            }
            gui.drawString(font, Component.literal(votes + " \u00a77(" + (votes * 100 / (Math.max(voteData.totalVotes, 1))) + "%)"), x + 12, barY + 5, PWPTheme.Colors.TEXT_SECONDARY);
            ly += 32;
        }
    }

    private void renderModeVoteSidebar(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        int remaining = modeVoteData.remainingSeconds - (int) ((System.currentTimeMillis() - modeVoteOpenedAt) / 1000);
        if (remaining < 0) remaining = 0;
        boolean voteFinished = remaining <= 0 || votedMode != null;

        gui.drawCenteredString(font, Component.literal("Голосование за режим"), x + w / 2, panelY + 6, PWPTheme.Colors.TEXT_ACCENT);
        gui.drawCenteredString(font, Component.literal(String.format("%d:%02d", remaining / 60, remaining % 60) + " \u00a7e" + modeVoteData.onlinePlayers + "\u00a77 онлайн"), x + w / 2, panelY + 18, PWPTheme.Colors.TEXT_SECONDARY);

        int ly = panelY + 36;
        int barW = w - 16;
        int maxV = 0;
        for (int v : modeVoteData.voteCounts) { if (v > maxV) maxV = v; }

        for (int i = 0; i < modeVoteData.modeNames.length; i++) {
            boolean sel = modeVoteData.modeNames[i].equals(votedMode);
            boolean hover = mx >= x + 8 && mx <= x + w - 8 && my >= ly - 2 && my <= ly + 24;
            boolean disabled = voteFinished && !sel;

            int bg = disabled ? PWPTheme.Colors.SURFACE_DIM : (sel ? 0xFF2A2010 : (hover ? PWPTheme.Colors.SURFACE_LIGHT : 0));
            if (bg != 0) RoundedRect.fill(gui, x + 8, ly - 2, w - 16, 28, PWPTheme.Spacing.RADIUS_SMALL, bg);

            gui.drawString(font, Component.literal((sel ? "\u2714 " : "") + modeVoteData.modeDisplayNames[i]), x + 12, ly, 0xFFFFFF);

            int votes = modeVoteData.voteCounts[i];
            int barY = ly + 12;
            gui.fill(x + 12, barY, x + 12 + barW, barY + 4, PWPTheme.Styles.Progress.BG);
            if (votes > 0 && maxV > 0) {
                gui.fill(x + 12, barY, x + 12 + (int) (barW * (float) votes / maxV), barY + 4, PWPTheme.Colors.ACCENT);
            }
            gui.drawString(font, Component.literal(votes + " \u00a77(" + (votes * 100 / (Math.max(modeVoteData.totalVotes, 1))) + "%)"), x + 12, barY + 5, PWPTheme.Colors.TEXT_SECONDARY);
            ly += 32;
        }
    }

    // ========== CLICK ==========

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (statsOpen) {
            if (my >= height - 30 && mx >= width / 2 - 40 && mx <= width / 2 + 40) { statsOpen = false; return true; }
            if (statsRenderer != null && statsRenderer.mouseClickedContent(mx, my, button)) return true;
            statsOpen = false;
            return true;
        }

        int navY = height - 26;
        if (mx >= width / 2 - 40 && mx <= width / 2 + 40 && my >= navY && my <= navY + 20) { startClose(); return true; }

        if (scrollPanel != null && scrollPanel.mouseClicked(mx, my, button)) return true;

        int leftW = (int) (width * 0.68f);
        int rightW = width - leftW - 8;
        int panelY = 32;
        int panelH = height - panelY - 30;

        if (handleMatchesClick(mx, my, PWPTheme.Spacing.SM, panelY, leftW - 8, panelH)) return true;

        int rightX = leftW + 4;
        if (mx >= rightX && mx <= rightX + rightW && my >= panelY && my <= panelY + panelH) {
            int sepY = panelY + panelH - 40;
            if (my >= sepY + 6 && my <= sepY + 26) {
                if (statsRenderer == null) initStats();
                statsOpen = true;
                return true;
            }
            if (handleVoteSidebarClick(mx, my, rightX, panelY, rightW, panelH)) return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private boolean handleMatchesClick(double mx, double my, int bx, int panelY, int contentW, int panelH) {
        if (listData == null || listData.count == 0) return false;
        int entryH = PWPMatchCard.cardHeight();
        int gap = 8;
        double scrollOff = scrollPanel != null ? scrollPanel.getScrollOffset() : 0;
        int startY = panelY + 4;
        if (matchData != null) startY += 72 + 8;

        for (int i = 0; i < listData.count; i++) {
            int y = startY + i * (entryH + gap) - (int) scrollOff;
            if (mx >= bx && mx <= bx + contentW && my >= y && my <= y + entryH) {
                if ("PLAYING".equals(listData.statuses[i])) {
                    if (listData.playerCounts[i] < listData.maxPlayers[i]) {
                        PacketHandler.INSTANCE.sendToServer(new JoinMatchServerPacket(listData.serverIds[i]));
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleVoteSidebarClick(double mx, double my, int x, int panelY, int w, int panelH) {
        if (modeVoteData != null && modeVoteData.modeNames.length > 0) {
            int remaining = modeVoteData.remainingSeconds - (int) ((System.currentTimeMillis() - modeVoteOpenedAt) / 1000);
            if (remaining <= 0 || votedMode != null) return false;
            int ly = panelY + 36;
            for (int i = 0; i < modeVoteData.modeNames.length; i++) {
                if (mx >= x + 8 && mx <= x + w - 8 && my >= ly - 2 && my <= ly + 26) {
                    votedMode = modeVoteData.modeNames[i];
                    PacketHandler.INSTANCE.sendToServer(new VoteModePacket(modeVoteData.modeNames[i]));
                    PWPToastManager.show("Голос принят: " + modeVoteData.modeDisplayNames[i], PWPToastManager.ToastType.SUCCESS);
                    return true;
                }
                ly += 32;
            }
            return false;
        }

        if (voteData == null || voteData.mapNames.length == 0) return false;
        int remaining = voteData.remainingSeconds - (int) ((System.currentTimeMillis() - voteOpenedAt) / 1000);
        if (remaining <= 0 || votedMap != null) return false;

        int ly = panelY + 32;
        for (int i = 0; i < voteData.mapNames.length; i++) {
            if (mx >= x + 8 && mx <= x + w - 8 && my >= ly - 2 && my <= ly + 26) {
                votedMap = voteData.mapNames[i];
                PacketHandler.INSTANCE.sendToServer(new VoteMapPacket(voteData.mapNames[i]));
                PWPToastManager.show("Голос принят: " + voteData.mapDisplayNames[i], PWPToastManager.ToastType.SUCCESS);
                return true;
            }
            ly += 32;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (scrollPanel != null && mx < (int) (width * 0.68f)) {
            return scrollPanel.mouseScrolled(mx, my, delta) != 0;
        }
        if (statsOpen && statsRenderer != null) return statsRenderer.mouseScrolled(mx, my, delta);
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (scrollPanel != null && scrollPanel.mouseDragged(mx, my, button, dragX, dragY)) return true;
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (scrollPanel != null && scrollPanel.mouseReleased(mx, my, button)) return true;
        return super.mouseReleased(mx, my, button);
    }

    private void initStats() {
        if (statsRenderer == null) statsRenderer = new StatsScreen();
        statsRenderer.setPanelSize(width, height);
        statsInitDone = true;
    }

    private void startClose() {
        if (closing) return;
        closing = true; closeStartTime = System.currentTimeMillis();
    }

    private void onCloseImmediate() { instance = null; imageCache.clear(); }

    @Override
    public void onClose() { super.onClose(); instance = null; imageCache.clear(); }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
    @Override
    public boolean isPauseScreen() { return false; }

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
            } catch (Exception e) { return null; }
        });
    }

    private static String formatFaction(String faction) {
        if (faction == null || faction.isEmpty() || faction.equals("none") || faction.equals("bluefor") || faction.equals("redfor"))
            return faction != null ? faction.toUpperCase() : "";
        return faction.replace("_", " ").toUpperCase();
    }

    private static String formatDuration(int secs) {
        int m = secs / 60, s = secs % 60;
        if (m >= 60) return (m / 60) + "h " + (m % 60) + "m";
        return m + "m " + s + "s";
    }
}
