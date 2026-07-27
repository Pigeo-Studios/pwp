package com.pwp.coreclient.gui.screens;

import com.mojang.blaze3d.platform.NativeImage;
import com.pwp.coreclient.gui.StatsScreen;
import com.pwp.coreclient.gui.components.*;
import com.pwp.coreclient.gui.theme.PWPIcons;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PWPLobbyScreen extends Screen {

    private static PWPLobbyScreen instance;

    private PWPScrollPanel mapScrollPanel;
    private PWPScrollPanel matchScrollPanel;

    private final long openTime;
    private boolean closing;
    private long closeStartTime;
    private int selectedMapIndex;
    private int selectedModeIndex = -1;

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

    private static final int PANEL_TOP = 32;
    private static final int PANEL_BOTTOM_MARGIN = 30;
    private static final int GAP = 4;
    private static final int CARD_H = 50;
    private static final int MODE_CARD_H = 64;

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
        else { PWPLobbyScreen s = new PWPLobbyScreen(); s.matchData = pkt; s.listLoading = false; mc.setScreen(s); }
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
            instance.selectedModeIndex = -1;
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.modeVoteData = pkt;
            s.modeVoteOpenedAt = System.currentTimeMillis();
            s.listLoading = false;
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
            instance.selectedMapIndex = 0;
        } else {
            PWPLobbyScreen s = new PWPLobbyScreen();
            s.modeVoteData = null;
            s.voteData = pkt;
            s.voteOpenedAt = System.currentTimeMillis();
            s.selectedMapIndex = 0;
            s.listLoading = false;
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

        int panelH = height - PANEL_TOP - PANEL_BOTTOM_MARGIN;
        int leftW = (int) (width * 0.27f);
        int scrollX = PWPTheme.Spacing.SM + 4;
        int scrollY = PANEL_TOP + 22;
        int scrollW = leftW - 16;
        int scrollH = panelH - 22 - 4;

        mapScrollPanel = new PWPScrollPanel(scrollX, scrollY, scrollW, scrollH);
        matchScrollPanel = new PWPScrollPanel(scrollX, scrollY, scrollW, scrollH);
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

        int panelH = height - PANEL_TOP - PANEL_BOTTOM_MARGIN;
        int leftW = (int) (width * 0.27f);
        int rightW = (int) (width * 0.27f);
        int centerW = width - leftW - rightW - GAP * 2 - PWPTheme.Spacing.SM;
        int leftX = PWPTheme.Spacing.SM;
        int centerX = leftX + leftW + GAP;
        int rightX = centerX + centerW + GAP;

        if (modeVoteData != null && modeVoteData.modeNames.length > 0) {
            renderModeVote(gui, mouseX, mouseY, leftX, centerX, rightX, PANEL_TOP, leftW, centerW, rightW, panelH);
        } else if (voteData != null && voteData.mapNames.length > 0) {
            renderMapVote(gui, mouseX, mouseY, leftX, centerX, rightX, PANEL_TOP, leftW, centerW, rightW, panelH);
        } else {
            renderMatchListView(gui, mouseX, mouseY, leftX, centerX, rightX, PANEL_TOP, leftW, centerW, rightW, panelH);
        }

        PWPToastManager.render(gui);
        renderOverlayFade(gui);

        int navY = height - 26;
        boolean backHovered = mouseX >= width / 2 - 40 && mouseX <= width / 2 + 40 && mouseY >= navY && mouseY <= navY + 20;
        String backText = "< Назад";
        var font = PWPTheme.Fonts.display();
        gui.drawString(font, Component.literal(backText), width / 2 - font.width(backText) / 2, navY + 6, backHovered ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM, false);

        if (statsOpen && statsRenderer != null) {
            gui.fill(0, 0, width, height, PWPTheme.Colors.BACKGROUND_DIM);
            int statsPanelY = 40;
            int statsH = height - statsPanelY - 30;
            PWPPanel.render(gui, PWPTheme.Spacing.SM, statsPanelY, width - PWPTheme.Spacing.SM * 2, statsH);
            gui.enableScissor(PWPTheme.Spacing.SM, statsPanelY, width - PWPTheme.Spacing.SM * 2, statsPanelY + statsH);
            statsRenderer.renderContent(gui, mouseX, mouseY, statsPanelY + 4, statsH - 8);
            gui.disableScissor();
            gui.drawString(font, Component.literal(backText), width / 2 - font.width(backText) / 2, height - 20, PWPTheme.Colors.TEXT_DIM, false);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    // ===================== MAP VOTING =====================

    private void renderMapVote(GuiGraphics gui, int mx, int my, int lx, int cx, int rx,
                                int panelY, int lw, int cw, int rw, int panelH) {
        renderMapVoteLeft(gui, mx, my, lx, panelY, lw, panelH);
        if (mapScrollPanel != null) mapScrollPanel.renderScrollbar(gui);
        renderMapVoteCenter(gui, mx, my, cx, panelY, cw, panelH);
        renderMapVoteRight(gui, mx, my, rx, panelY, rw, panelH);
    }

    private void renderMapVoteLeft(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        var font = PWPTheme.Fonts.display();
        PWPPanel.render(gui, x, panelY, w, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(x, panelY, x + w, panelY + panelH);

        String title = "Выбор карты";
        gui.drawString(font, Component.literal(title), x + w / 2 - font.width(title) / 2, panelY + 6, PWPTheme.Colors.TEXT_ACCENT, false);
        gui.fill(x + 8, panelY + 16, x + w - 8, panelY + 17, PWPTheme.Colors.BORDER);

        int scrollOff = mapScrollPanel != null ? (int) -mapScrollPanel.getScrollOffset() : 0;
        int ly = panelY + 22 + scrollOff;
        int gap = 6;
        int maxV = 0;
        for (int v : voteData.voteCounts) { if (v > maxV) maxV = v; }

        int totalH = voteData.mapNames.length * (CARD_H + gap) - gap;
        if (mapScrollPanel != null) mapScrollPanel.setContentHeight(totalH + 12);

        for (int i = 0; i < voteData.mapNames.length; i++) {
            boolean selected = (i == selectedMapIndex);
            boolean hovered = mx >= x + 4 && mx <= x + w - 4 && my >= ly && my <= ly + CARD_H;
            PWPCard.State state = PWPCard.getState(selected, hovered, false);
            PWPCard.render(gui, x + 4, ly, w - 8, CARD_H, state);

            PWPIcons.render(gui, PWPIcons.FLAG, x + 12, ly + 6);
            gui.drawString(font, Component.literal(voteData.mapDisplayNames[i]), x + 30, ly + 6, PWPTheme.Colors.TEXT_PRIMARY);

            String votesStr = voteData.voteCounts[i] + " голосов";
            gui.drawString(font, Component.literal(votesStr), x + 30, ly + 20, PWPTheme.Colors.TEXT_DIM);

            int barW = w - 24;
            int barY = ly + 36;
            gui.fill(x + 12, barY, x + 12 + barW, barY + 4, PWPTheme.Styles.Progress.BG);
            if (voteData.voteCounts[i] > 0 && maxV > 0) {
                float pct = (float) voteData.voteCounts[i] / maxV;
                boolean isLeader = voteData.mapNames[i].equals(voteData.leaderName);
                int fillCol = isLeader ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.ACCENT_DIM;
                gui.fill(x + 12, barY, x + 12 + (int) (barW * pct), barY + 4, fillCol);
            }

            ly += CARD_H + gap;
        }

        gui.disableScissor();
    }

    private void renderMapVoteCenter(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        var font = PWPTheme.Fonts.display();
        PWPPanel.render(gui, x, panelY, w, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(x, panelY, x + w, panelY + panelH);

        if (selectedMapIndex < 0 || selectedMapIndex >= voteData.mapNames.length) {
            gui.disableScissor();
            return;
        }

        String mapName = voteData.mapNames[selectedMapIndex];
        String worldPath = selectedMapIndex < voteData.worldPaths.length ? voteData.worldPaths[selectedMapIndex] : null;
        ResourceLocation preview = getTexture(mapName, worldPath, "vote_");

        int previewW = w - 24;
        int previewH = previewW * 9 / 16;
        int maxPreviewH = (int) (panelH * 0.55f);
        if (previewH > maxPreviewH) {
            previewH = maxPreviewH;
            previewW = previewH * 16 / 9;
        }
        int px = x + (w - previewW) / 2;
        int py = panelY + 12;

        if (preview != null) {
            RoundedRect.fill(gui, px - 2, py - 2, previewW + 4, previewH + 4, PWPTheme.Spacing.RADIUS_MEDIUM, PWPTheme.Colors.SURFACE);
            gui.blit(preview, px, py, previewW, previewH, 0, 0, 256, 256, 256, 256);
        } else {
            PWPPanel.render(gui, px, py, previewW, previewH, PWPPanel.Variant.SURFACE);
            String noPrev = "Нет превью";
            int iconSize = 32;
            PWPIcons.render(gui, PWPIcons.FLAG, px + previewW / 2 - iconSize / 2, py + previewH / 2 - iconSize / 2 - 10, iconSize, 80);
            gui.drawString(font, Component.literal(noPrev), px + previewW / 2 - font.width(noPrev) / 2, py + previewH / 2 + 16, PWPTheme.Colors.TEXT_DIM, false);
        }

        int infoY = py + previewH + 14;
        gui.drawString(font, Component.literal(voteData.mapDisplayNames[selectedMapIndex]),
            x + w / 2 - font.width(voteData.mapDisplayNames[selectedMapIndex]) / 2,
            infoY, PWPTheme.Colors.TEXT_PRIMARY, false);

        String maxStr = "Макс. " + voteData.maxPlayers[selectedMapIndex] + " игроков";
        gui.drawString(font, Component.literal(maxStr),
            x + w / 2 - font.width(maxStr) / 2,
            infoY + 12, PWPTheme.Colors.TEXT_SECONDARY, false);

        if (selectedMapIndex < voteData.blueFactions.length && selectedMapIndex < voteData.redFactions.length) {
            int blueCol = PWPTheme.Colors.TEAM_BLUE;
            int redCol = PWPTheme.Colors.TEAM_RED;
            String blueStr = formatFaction(voteData.blueFactions[selectedMapIndex]);
            String redStr = formatFaction(voteData.redFactions[selectedMapIndex]);
            String combined = blueStr + " vs " + redStr;
            int totalW = font.width(combined);
            int startX = x + w / 2 - totalW / 2;
            gui.drawString(font, Component.literal(blueStr), startX, infoY + 24, blueCol, false);
            gui.drawString(font, Component.literal(" vs "), startX + font.width(blueStr), infoY + 24, PWPTheme.Colors.TEXT_DIM, false);
            gui.drawString(font, Component.literal(redStr), startX + font.width(blueStr + " vs "), infoY + 24, redCol, false);
        }

        gui.disableScissor();
    }

    private void renderMapVoteRight(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        var font = PWPTheme.Fonts.display();
        PWPPanel.render(gui, x, panelY, w, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(x, panelY, x + w, panelY + panelH);

        if (selectedMapIndex < 0 || selectedMapIndex >= voteData.mapNames.length) {
            gui.disableScissor();
            return;
        }

        int availableH = panelH - 70;
        int ly = panelY + 8;

        String descTitle = "Описание карты";
        gui.drawString(font, Component.literal(descTitle), x + 8, ly, PWPTheme.Colors.TEXT_ACCENT, false);
        ly += 12;
        gui.fill(x + 8, ly, x + w - 8, ly + 1, PWPTheme.Colors.BORDER);
        ly += 6;

        String desc = voteData.mapDescriptions != null && selectedMapIndex < voteData.mapDescriptions.length
            ? voteData.mapDescriptions[selectedMapIndex] : "";
        if (!desc.isEmpty()) {
            int maxDescW = w - 16;
            List<String> lines = wrapText(font, desc, maxDescW);
            int lineH = 10;
            int maxLines = Math.min(lines.size(), (availableH - (ly - panelY)) / lineH);
            for (int li = 0; li < maxLines; li++) {
                gui.drawString(font, Component.literal(lines.get(li)), x + 8, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
                ly += lineH;
            }
        } else {
            gui.drawString(font, Component.literal("Нет описания"), x + 8, ly, PWPTheme.Colors.TEXT_DIM, false);
        }

        // Timer + button at fixed bottom position
        int btnAreaY = panelY + panelH - 52;
        gui.fill(x + 8, btnAreaY - 6, x + w - 8, btnAreaY - 5, PWPTheme.Colors.BORDER);

        int remaining = voteData.remainingSeconds - (int) ((System.currentTimeMillis() - voteOpenedAt) / 1000);
        if (remaining < 0) remaining = 0;
        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        String infoLine = timeStr + "  " + voteData.onlinePlayers + " онлайн";
        gui.drawString(font, Component.literal(infoLine), x + w / 2 - font.width(infoLine) / 2, btnAreaY, PWPTheme.Colors.TEXT_SECONDARY, false);

        boolean hasVoted = votedMap != null;
        boolean timeUp = remaining <= 0;
        String btnText = hasVoted ? "Голос принят ✓" : "Проголосовать";
        int btnH = PWPTheme.Spacing.BUTTON_HEIGHT;
        int btnW = w - 16;
        int btnX = x + (w - btnW) / 2;
        int btnY = btnAreaY + 14;

        if (hasVoted || timeUp) {
            PWPPanel.render(gui, btnX, btnY, btnW, btnH, PWPPanel.Variant.SURFACE_DIM);
            gui.drawString(font, Component.literal(btnText), btnX + btnW / 2 - font.width(btnText) / 2, btnY + 8, PWPTheme.Colors.TEXT_DIM, false);
        } else {
            RoundedRect.fill(gui, btnX, btnY, btnW, btnH, PWPTheme.Spacing.RADIUS_SMALL, PWPTheme.Colors.ACCENT);
            gui.drawString(font, Component.literal(btnText), btnX + btnW / 2 - font.width(btnText) / 2, btnY + 8, 0xFF0A0C0E, false);
        }

        gui.disableScissor();
    }

    // ===================== MODE VOTING =====================

    private void renderModeVote(GuiGraphics gui, int mx, int my, int lx, int cx, int rx,
                                 int panelY, int lw, int cw, int rw, int panelH) {
        renderModeVoteLeft(gui, mx, my, lx, panelY, lw, panelH);
        if (mapScrollPanel != null) mapScrollPanel.renderScrollbar(gui);
        renderModeVoteCenter(gui, mx, my, cx, panelY, cw, panelH);
        renderModeVoteRight(gui, mx, my, rx, panelY, rw, panelH);
    }

    private void renderModeVoteLeft(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        var font = PWPTheme.Fonts.display();
        PWPPanel.render(gui, x, panelY, w, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(x, panelY, x + w, panelY + panelH);

        String title = "Выбор режима";
        gui.drawString(font, Component.literal(title), x + w / 2 - font.width(title) / 2, panelY + 6, PWPTheme.Colors.TEXT_ACCENT, false);
        gui.fill(x + 8, panelY + 16, x + w - 8, panelY + 17, PWPTheme.Colors.BORDER);

        int scrollOff = mapScrollPanel != null ? (int) -mapScrollPanel.getScrollOffset() : 0;
        int ly = panelY + 22 + scrollOff;
        int gap = 6;
        int maxV = 0;
        for (int v : modeVoteData.voteCounts) { if (v > maxV) maxV = v; }

        int totalH = modeVoteData.modeNames.length * (MODE_CARD_H + gap) - gap;
        if (mapScrollPanel != null) mapScrollPanel.setContentHeight(totalH + 12);

        for (int i = 0; i < modeVoteData.modeNames.length; i++) {
            boolean selected = (i == selectedModeIndex);
            boolean hovered = mx >= x + 4 && mx <= x + w - 4 && my >= ly && my <= ly + MODE_CARD_H;
            PWPCard.State state = PWPCard.getState(selected, hovered, false);
            PWPCard.render(gui, x + 4, ly, w - 8, MODE_CARD_H, state);

            ResourceLocation modeIcon = getModeIcon(modeVoteData.modeNames[i]);
            if (modeIcon != null) {
                gui.blit(modeIcon, x + 12, ly + 6, 0, 0, 16, 16, 16, 16);
            } else {
                int fallback = i == 0 ? PWPIcons.SWORDS : PWPIcons.SKULL;
                PWPIcons.render(gui, fallback, x + 12, ly + 6);
            }

            gui.drawString(font, Component.literal(modeVoteData.modeDisplayNames[i]), x + 30, ly + 6, PWPTheme.Colors.TEXT_PRIMARY);

            String desc = i < modeVoteData.modeDescriptions.length ? modeVoteData.modeDescriptions[i] : "";
            if (!desc.isEmpty()) {
                String shortDesc = font.plainSubstrByWidth(desc, w - 40);
                gui.drawString(font, Component.literal(shortDesc), x + 30, ly + 20, PWPTheme.Colors.TEXT_SECONDARY);
            }

            String votesStr = modeVoteData.voteCounts[i] + " голосов";
            gui.drawString(font, Component.literal(votesStr), x + 30, ly + 34, PWPTheme.Colors.TEXT_DIM);

            int barW = w - 24;
            int barY = ly + 48;
            gui.fill(x + 12, barY, x + 12 + barW, barY + 4, PWPTheme.Styles.Progress.BG);
            if (modeVoteData.voteCounts[i] > 0 && maxV > 0) {
                float pct = (float) modeVoteData.voteCounts[i] / maxV;
                gui.fill(x + 12, barY, x + 12 + (int) (barW * pct), barY + 4, PWPTheme.Colors.ACCENT);
            }

            ly += MODE_CARD_H + gap;
        }

        gui.disableScissor();
    }

    private void renderModeVoteCenter(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        var font = PWPTheme.Fonts.display();
        PWPPanel.render(gui, x, panelY, w, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(x, panelY, x + w, panelY + panelH);

        String voteTitle = "Голосование за режим";
        gui.drawString(font, Component.literal(voteTitle), x + w / 2 - font.width(voteTitle) / 2, panelY + 16, PWPTheme.Colors.TEXT_ACCENT, false);

        if (voteData != null && voteData.leaderName != null) {
            String mapLabel = "Победившая карта:";
            gui.drawString(font, Component.literal(mapLabel), x + w / 2 - font.width(mapLabel) / 2, panelY + 36, PWPTheme.Colors.TEXT_SECONDARY, false);

            String winner = voteData.leaderName;
            for (int i = 0; i < voteData.mapNames.length; i++) {
                if (voteData.mapNames[i].equals(winner)) {
                    winner = voteData.mapDisplayNames[i];
                    break;
                }
            }
            gui.drawString(font, Component.literal(winner), x + w / 2 - font.width(winner) / 2, panelY + 48, PWPTheme.Colors.TEXT_PRIMARY, false);

            ResourceLocation preview = getTexture(voteData.leaderName, null, "vote_win_");
            if (preview == null) {
                for (int i = 0; i < voteData.mapNames.length; i++) {
                    if (voteData.mapNames[i].equals(voteData.leaderName)) {
                        String wp = i < voteData.worldPaths.length ? voteData.worldPaths[i] : null;
                        preview = getTexture(voteData.leaderName, wp, "vote_win_");
                        break;
                    }
                }
            }
            if (preview != null) {
                int prevW = Math.min(w - 40, 240);
                int prevH = prevW * 9 / 16;
                int px = x + (w - prevW) / 2;
                int py = panelY + 64;
                gui.blit(preview, px, py, prevW, prevH, 0, 0, 256, 256, 256, 256);
            }
        }

        int remaining = modeVoteData.remainingSeconds - (int) ((System.currentTimeMillis() - modeVoteOpenedAt) / 1000);
        if (remaining < 0) remaining = 0;
        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        String infoLine = timeStr + "  " + modeVoteData.onlinePlayers + " онлайн";
        gui.drawString(font, Component.literal(infoLine), x + w / 2 - font.width(infoLine) / 2, panelY + panelH - 24, PWPTheme.Colors.TEXT_SECONDARY, false);

        gui.disableScissor();
    }

    private void renderModeVoteRight(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        var font = PWPTheme.Fonts.display();
        PWPPanel.render(gui, x, panelY, w, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(x, panelY, x + w, panelY + panelH);

        if (selectedModeIndex < 0 || selectedModeIndex >= modeVoteData.modeNames.length) {
            String hint = "Выберите режим";
            gui.drawString(font, Component.literal(hint), x + w / 2 - font.width(hint) / 2, panelY + panelH / 2 - 5, PWPTheme.Colors.TEXT_DIM, false);
            gui.disableScissor();
            return;
        }

        int descAreaEnd = panelY + panelH - 60;
        int ly = panelY + 8;

        String descTitle = "Описание режима";
        gui.drawString(font, Component.literal(descTitle), x + 8, ly, PWPTheme.Colors.TEXT_ACCENT, false);
        ly += 12;
        gui.fill(x + 8, ly, x + w - 8, ly + 1, PWPTheme.Colors.BORDER);
        ly += 6;

        String desc = selectedModeIndex < modeVoteData.modeDescriptions.length
            ? modeVoteData.modeDescriptions[selectedModeIndex] : "";
        if (!desc.isEmpty()) {
            int maxDescW = w - 16;
            List<String> lines = wrapText(font, desc, maxDescW);
            int lineH = 10;
            int maxLines = Math.min(lines.size(), (descAreaEnd - ly) / lineH);
            for (int li = 0; li < maxLines; li++) {
                gui.drawString(font, Component.literal(lines.get(li)), x + 8, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
                ly += lineH;
            }
        }

        // Separator + timer + button at bottom
        int btnAreaY = panelY + panelH - 50;
        gui.fill(x + 8, btnAreaY, x + w - 8, btnAreaY + 1, PWPTheme.Colors.BORDER);

        int remaining = modeVoteData.remainingSeconds - (int) ((System.currentTimeMillis() - modeVoteOpenedAt) / 1000);
        if (remaining < 0) remaining = 0;
        String timeStr = String.format("%d:%02d", remaining / 60, remaining % 60);
        String infoLine = timeStr + "  " + modeVoteData.onlinePlayers + " онлайн";
        gui.drawString(font, Component.literal(infoLine), x + w / 2 - font.width(infoLine) / 2, btnAreaY + 6, PWPTheme.Colors.TEXT_SECONDARY, false);

        boolean hasVoted = votedMode != null;
        boolean modeTimeUp = remaining <= 0;
        String btnText = hasVoted ? "Голос принят ✓" : "Проголосовать";
        int btnH = PWPTheme.Spacing.BUTTON_HEIGHT;
        int btnW = w - 16;
        int btnX = x + (w - btnW) / 2;
        int btnY = btnAreaY + 20;

        if (hasVoted || modeTimeUp) {
            PWPPanel.render(gui, btnX, btnY, btnW, btnH, PWPPanel.Variant.SURFACE_DIM);
            gui.drawString(font, Component.literal(btnText), btnX + btnW / 2 - font.width(btnText) / 2, btnY + 8, PWPTheme.Colors.TEXT_DIM, false);
        } else {
            RoundedRect.fill(gui, btnX, btnY, btnW, btnH, PWPTheme.Spacing.RADIUS_SMALL, PWPTheme.Colors.ACCENT);
            gui.drawString(font, Component.literal(btnText), btnX + btnW / 2 - font.width(btnText) / 2, btnY + 8, 0xFF0A0C0E, false);
        }

        gui.disableScissor();
    }

    // ===================== MATCH LIST =====================

    private void renderMatchListView(GuiGraphics gui, int mx, int my, int lx, int cx, int rx,
                                      int panelY, int lw, int cw, int rw, int panelH) {
        var font = PWPTheme.Fonts.display();

        if (listLoading && listData == null && matchData == null) {
            PWPPanel.render(gui, lx, panelY, lw + cw + GAP, panelH, PWPPanel.Variant.SURFACE_DIM);
            gui.enableScissor(lx, panelY, lx + lw + cw + GAP, panelY + panelH);
            renderSkeletons(gui, lx, panelY, lw + cw + GAP, panelH);
            gui.disableScissor();
            return;
        }

        if (listLoadFailed) {
            PWPPanel.render(gui, lx, panelY, lw, panelH, PWPPanel.Variant.SURFACE_DIM);
            String errTitle = "Ошибка загрузки";
            String errSub = "Проверьте подключение к серверу";
            gui.drawString(font, Component.literal(errTitle), lx + lw / 2 - font.width(errTitle) / 2, panelY + panelH / 2 - 10, PWPTheme.Colors.DANGER, false);
            gui.drawString(font, Component.literal(errSub), lx + lw / 2 - font.width(errSub) / 2, panelY + panelH / 2 + 6, PWPTheme.Colors.TEXT_SECONDARY, false);
            return;
        }

        // Left panel: match list
        PWPPanel.render(gui, lx, panelY, lw, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(lx, panelY, lx + lw, panelY + panelH);

        if (matchData != null) {
            String curTitle = "Текущий матч";
            gui.drawString(font, Component.literal(curTitle), lx + lw / 2 - font.width(curTitle) / 2, panelY + 6, PWPTheme.Colors.TEXT_ACCENT, false);
            gui.fill(lx + 8, panelY + 16, lx + lw - 8, panelY + 17, PWPTheme.Colors.BORDER);
        }

        String listTitle = matchData != null ? "Активные матчи" : "Активные матчи";
        if (matchData == null) {
            gui.drawString(font, Component.literal(listTitle), lx + lw / 2 - font.width(listTitle) / 2, panelY + 6, PWPTheme.Colors.TEXT_ACCENT, false);
            gui.fill(lx + 8, panelY + 16, lx + lw - 8, panelY + 17, PWPTheme.Colors.BORDER);
        }

        int scrollOff = matchScrollPanel != null ? (int) -matchScrollPanel.getScrollOffset() : 0;
        int ly = panelY + (matchData != null ? 22 : 22) + scrollOff;
        int entryH = PWPMatchCard.cardHeight();
        int gap = 8;

        if (matchData != null) {
            boolean hovered = PWPCard.isHovered(lx + 4, ly, lw - 8, entryH, mx, my);
            PWPMatchCard.render(gui, lx + 4, ly, lw - 8, entryH,
                matchData.mapDisplayName, matchData.modeDisplayName,
                formatDuration(matchData.remainingSeconds),
                matchData.onlinePlayers, 50,
                formatFaction(matchData.blueFaction), formatFaction(matchData.redFaction),
                matchData.blueTickets, matchData.redTickets,
                "PLAYING".equals(matchData.status) ? PWPMatchCard.Status.PLAYING : PWPMatchCard.Status.WAITING,
                null, true, hovered);
            ly += entryH + gap + 4;
            gui.fill(lx + 8, ly - 4, lx + lw - 8, ly - 3, PWPTheme.Colors.BORDER);
        }

        if (listData == null || listData.count == 0) {
            if (matchScrollPanel != null) matchScrollPanel.setContentHeight(0);
            if (matchData == null) {
                String emptyTitle = "Нет активных матчей";
                gui.drawString(font, Component.literal(emptyTitle), lx + lw / 2 - font.width(emptyTitle) / 2, panelY + panelH / 2 - 8, PWPTheme.Colors.TEXT_SECONDARY, false);
            }
            gui.disableScissor();
            if (matchScrollPanel != null) matchScrollPanel.renderScrollbar(gui);
            return;
        }

        int totalH = entryH * listData.count + gap * (listData.count - 1);
        if (matchScrollPanel != null) matchScrollPanel.setContentHeight(totalH + 12);

        for (int i = 0; i < listData.count; i++) {
            ly = renderMatchEntry(gui, lx + 4, ly, lw - 8, entryH, i, mx, my);
            ly += gap;
        }

        gui.disableScissor();
        if (matchScrollPanel != null) matchScrollPanel.renderScrollbar(gui);

        // Center-right panel: currently active match info
        if (listData != null && listData.count > 0) {
            int rightPanelW = width - lx - lw - GAP - PWPTheme.Spacing.SM;
            renderGlobalMatchInfo(gui, mx, my, lx + lw + GAP, panelY, rightPanelW, panelH);
        } else if (matchData != null) {
            int rightPanelW = width - lx - lw - GAP - PWPTheme.Spacing.SM;
            renderGlobalMatchInfo(gui, mx, my, lx + lw + GAP, panelY, rightPanelW, panelH);
        }
    }

    private void renderGlobalMatchInfo(GuiGraphics gui, int mx, int my, int x, int panelY, int w, int panelH) {
        var font = PWPTheme.Fonts.display();
        PWPPanel.render(gui, x, panelY, w, panelH, PWPPanel.Variant.SURFACE_DIM);
        gui.enableScissor(x, panelY, x + w, panelY + panelH);

        String infoTitle = "Информация о матче";
        gui.drawString(font, Component.literal(infoTitle), x + w / 2 - font.width(infoTitle) / 2, panelY + 6, PWPTheme.Colors.TEXT_ACCENT, false);
        gui.fill(x + 8, panelY + 16, x + w - 8, panelY + 17, PWPTheme.Colors.BORDER);

        if (matchData != null) {
            int ly = panelY + 28;
            gui.drawString(font, Component.literal("Карта: " + matchData.mapDisplayName), x + 12, ly, PWPTheme.Colors.TEXT_PRIMARY, false);
            ly += 12;
            gui.drawString(font, Component.literal("Режим: " + matchData.modeDisplayName), x + 12, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
            ly += 12;
            gui.drawString(font, Component.literal("Статус: " + matchData.status), x + 12, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
            ly += 12;
            gui.drawString(font, Component.literal("Игроков: " + matchData.onlinePlayers + "/50"), x + 12, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
            ly += 12;
            gui.drawString(font, Component.literal("Длительность: " + formatDuration(matchData.remainingSeconds)), x + 12, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
            ly += 20;

            String blueF = formatFaction(matchData.blueFaction);
            String redF = formatFaction(matchData.redFaction);
            gui.drawString(font, Component.literal(blueF + "  " + matchData.blueTickets + " vs " + matchData.redTickets + "  " + redF),
                x + 12, ly, PWPTheme.Colors.TEXT_PRIMARY, false);
        } else if (listData != null && listData.count > 0) {
            int idx = getSelectedMatchIdx();
            if (idx >= 0) {
                int ly = panelY + 28;
                gui.drawString(font, Component.literal("Карта: " + listData.displayNames[idx]), x + 12, ly, PWPTheme.Colors.TEXT_PRIMARY, false);
                ly += 12;
                String statusStr = "WAITING".equals(listData.statuses[idx]) ? "Ожидание" :
                    "STARTING".equals(listData.statuses[idx]) ? "Запуск" : "В игре";
                gui.drawString(font, Component.literal("Статус: " + statusStr), x + 12, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
                ly += 12;
                gui.drawString(font, Component.literal("Игроков: " + listData.playerCounts[idx] + "/" + listData.maxPlayers[idx]), x + 12, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
                ly += 12;
                if (listData.elapsedSeconds[idx] > 0) {
                    gui.drawString(font, Component.literal("Длительность: " + formatDuration(listData.elapsedSeconds[idx])), x + 12, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
                    ly += 12;
                }
                ly += 12;

                String blueF = formatFaction(listData.blueFactions[idx]);
                String redF = formatFaction(listData.redFactions[idx]);
                gui.drawString(font, Component.literal(blueF), x + 12, ly, PWPTheme.Colors.TEAM_BLUE, false);
                gui.drawString(font, Component.literal(" vs "), x + 12 + font.width(blueF), ly, PWPTheme.Colors.TEXT_DIM, false);
                gui.drawString(font, Component.literal(redF), x + 12 + font.width(blueF + " vs "), ly, PWPTheme.Colors.TEAM_RED, false);
                ly += 14;

                String ticketLine = "Билеты: " + listData.blueTickets[idx] + " | " + listData.redTickets[idx];
                gui.drawString(font, Component.literal(ticketLine), x + 12, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
                ly += 20;

                if ("PLAYING".equals(listData.statuses[idx]) && listData.playerCounts[idx] < listData.maxPlayers[idx]) {
                    int btnW = Math.min(w - 24, 160);
                    int btnH = PWPTheme.Spacing.BUTTON_HEIGHT;
                    int btnX = x + (w - btnW) / 2;
                    RoundedRect.fill(gui, btnX, ly, btnW, btnH, PWPTheme.Spacing.RADIUS_SMALL, PWPTheme.Colors.ACCENT);
                    String joinText = "Присоединиться";
                    gui.drawString(font, Component.literal(joinText), btnX + btnW / 2 - font.width(joinText) / 2, ly + 8, 0xFF0A0C0E, false);
                }
            }
        }

        // Stats link at bottom
        int sepY = panelY + panelH - 40;
        gui.fill(x + 8, sepY, x + w - 8, sepY + 1, PWPTheme.Colors.BORDER);
        boolean statsHover = mx >= x + 8 && mx <= x + w - 8 && my >= sepY + 6 && my <= sepY + 26;
        int statsCol = statsHover ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY;
        String statsText = "Статистика \u2192";
        gui.drawString(font, Component.literal(statsText), x + w / 2 - font.width(statsText) / 2, sepY + 10, statsCol, false);

        gui.disableScissor();
    }

    private int getSelectedMatchIdx() {
        return 0;
    }

    // ===================== SKELETONS =====================

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

    private List<String> wrapText(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) { lines.add(""); return lines; }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (font.width(test) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        super.renderBackground(gui);
    }

    private void renderOverlayFade(GuiGraphics gui) {
        if (closing) {
            long elapsed = System.currentTimeMillis() - closeStartTime;
            float t = Math.min(elapsed / 150.0F, 1);
            gui.fill(0, 0, width, height, PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.BACKGROUND, 1.0F - com.pwp.coreclient.gui.animations.Easing.easeOutCubic(t)));
        } else {
            long elapsed = System.currentTimeMillis() - openTime;
            if (elapsed < 150) {
                float t = elapsed / 150.0F;
                gui.fill(0, 0, width, height, PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.BACKGROUND, 1.0F - com.pwp.coreclient.gui.animations.Easing.easeOutCubic(t)));
            }
        }
    }

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

        int panelH = height - PANEL_TOP - PANEL_BOTTOM_MARGIN;
        int leftW = (int) (width * 0.27f);
        int rightW = (int) (width * 0.27f);
        int centerW = width - leftW - rightW - GAP * 2 - PWPTheme.Spacing.SM;
        int leftX = PWPTheme.Spacing.SM;
        int centerX = leftX + leftW + GAP;
        int rightX = centerX + centerW + GAP;

        boolean scrollHandled = false;
        if (modeVoteData != null || voteData != null) {
            scrollHandled = mapScrollPanel != null && mapScrollPanel.mouseClicked(mx, my, button);
        } else {
            scrollHandled = matchScrollPanel != null && matchScrollPanel.mouseClicked(mx, my, button);
        }
        if (scrollHandled) return true;

        if (modeVoteData != null && modeVoteData.modeNames.length > 0) {
            return handleModeVoteClick(mx, my, leftX, centerX, rightX, PANEL_TOP, leftW, rightW, panelH);
        } else if (voteData != null && voteData.mapNames.length > 0) {
            return handleMapVoteClick(mx, my, leftX, centerX, rightX, PANEL_TOP, leftW, rightW, panelH);
        } else {
            return handleMatchListClick(mx, my, leftX, PANEL_TOP, leftW, panelH);
        }
    }

    private boolean handleMapVoteClick(double mx, double my, int lx, int cx, int rx, int panelY, int lw, int rw, int panelH) {
        int scrollOff = mapScrollPanel != null ? (int) -mapScrollPanel.getScrollOffset() : 0;
        int ly = panelY + 22 + scrollOff;
        int gap = 6;
        for (int i = 0; i < voteData.mapNames.length; i++) {
            if (mx >= lx + 4 && mx <= lx + lw - 4 && my >= ly && my <= ly + CARD_H) {
                selectedMapIndex = i;
                return true;
            }
            ly += CARD_H + gap;
        }

        int remaining = voteData.remainingSeconds - (int) ((System.currentTimeMillis() - voteOpenedAt) / 1000);
        if (remaining > 0 && votedMap == null && selectedMapIndex >= 0) {
            int btnW = rw - 16;
            int btnH = PWPTheme.Spacing.BUTTON_HEIGHT;
            int btnX = rx + (rw - btnW) / 2;
            int btnAreaY = panelY + panelH - 38;
            if (mx >= btnX && mx <= btnX + btnW && my >= btnAreaY && my <= btnAreaY + btnH) {
                votedMap = voteData.mapNames[selectedMapIndex];
                PacketHandler.INSTANCE.sendToServer(new VoteMapPacket(voteData.mapNames[selectedMapIndex]));
                PWPToastManager.show("Голос принят: " + voteData.mapDisplayNames[selectedMapIndex], PWPToastManager.ToastType.SUCCESS);
                return true;
            }
        }

        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleModeVoteClick(double mx, double my, int lx, int cx, int rx, int panelY, int lw, int rw, int panelH) {
        int scrollOff = mapScrollPanel != null ? (int) -mapScrollPanel.getScrollOffset() : 0;
        int ly = panelY + 22 + scrollOff;
        int gap = 6;
        for (int i = 0; i < modeVoteData.modeNames.length; i++) {
            if (mx >= lx + 4 && mx <= lx + lw - 4 && my >= ly && my <= ly + MODE_CARD_H) {
                selectedModeIndex = i;
                return true;
            }
            ly += MODE_CARD_H + gap;
        }

        int remaining = modeVoteData.remainingSeconds - (int) ((System.currentTimeMillis() - modeVoteOpenedAt) / 1000);
        if (remaining > 0 && votedMode == null && selectedModeIndex >= 0) {
            int btnW = rw - 16;
            int btnH = PWPTheme.Spacing.BUTTON_HEIGHT;
            int btnX = rx + (rw - btnW) / 2;
            int btnY = panelY + panelH - 30;
            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                votedMode = modeVoteData.modeNames[selectedModeIndex];
                PacketHandler.INSTANCE.sendToServer(new VoteModePacket(modeVoteData.modeNames[selectedModeIndex]));
                PWPToastManager.show("Голос принят: " + modeVoteData.modeDisplayNames[selectedModeIndex], PWPToastManager.ToastType.SUCCESS);
                return true;
            }
        }

        return super.mouseClicked(mx, my, 0);
    }

    private boolean handleMatchListClick(double mx, double my, int lx, int panelY, int lw, int panelH) {
        if (listData == null || listData.count == 0) return false;

        int entryH = PWPMatchCard.cardHeight();
        int gap = 8;
        double scrollOff = matchScrollPanel != null ? matchScrollPanel.getScrollOffset() : 0;
        int startY = panelY + (matchData != null ? 22 : 22);
        if (matchData != null) startY += entryH + gap + 4;

        for (int i = 0; i < listData.count; i++) {
            int y = startY + i * (entryH + gap) - (int) scrollOff;
            if (mx >= lx + 4 && mx <= lx + lw - 4 && my >= y && my <= y + entryH) {
                if ("PLAYING".equals(listData.statuses[i])) {
                    if (listData.playerCounts[i] < listData.maxPlayers[i]) {
                        PacketHandler.INSTANCE.sendToServer(new JoinMatchServerPacket(listData.serverIds[i]));
                    }
                }
                return true;
            }
        }

        // Stats link in info panel
        int rightPanelX = lx + lw + GAP;
        int rightPanelW = width - rightPanelX - PWPTheme.Spacing.SM;
        int sepY = panelY + panelH - 40;
        if (mx >= rightPanelX + 8 && mx <= rightPanelX + rightPanelW - 8 && my >= sepY + 6 && my <= sepY + 26) {
            if (statsRenderer == null) initStats();
            statsOpen = true;
            return true;
        }

        return super.mouseClicked(mx, my, 0);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (modeVoteData != null || voteData != null) {
            if (mx < (int) (width * 0.27f) + PWPTheme.Spacing.SM) {
                return mapScrollPanel != null && mapScrollPanel.mouseScrolled(mx, my, delta) != 0;
            }
        } else {
            if (mx < (int) (width * 0.27f) + PWPTheme.Spacing.SM) {
                return matchScrollPanel != null && matchScrollPanel.mouseScrolled(mx, my, delta) != 0;
            }
        }
        if (statsOpen && statsRenderer != null) return statsRenderer.mouseScrolled(mx, my, delta);
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (mapScrollPanel != null && mapScrollPanel.mouseDragged(mx, my, button, dragX, dragY)) return true;
        if (matchScrollPanel != null && matchScrollPanel.mouseDragged(mx, my, button, dragX, dragY)) return true;
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (mapScrollPanel != null && mapScrollPanel.mouseReleased(mx, my, button)) return true;
        if (matchScrollPanel != null && matchScrollPanel.mouseReleased(mx, my, button)) return true;
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

    private static ResourceLocation getModeIcon(String modeName) {
        if (modeName == null || modeName.isEmpty()) return null;
        return ResourceLocation.tryParse("pwp_core_client:textures/gui/modes/" + modeName + ".png");
    }

    private static ResourceLocation getTexture(String mapName, String worldPath, String prefix) {
        if (mapName == null || mapName.isEmpty()) return null;
        if (worldPath == null || worldPath.isEmpty()) {
            ResourceLocation cached = imageCache.get(prefix + mapName);
            if (cached != null) return cached;
            return null;
        }
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
