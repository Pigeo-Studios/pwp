package com.pwp.coreclient.gui.screens;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.*;
import com.pwp.coreclient.gui.components.PWPToastManager;
import com.pwp.coreclient.gui.theme.PWPIcons;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.*;
import com.pwp.coreclient.gui.screens.tabs.*;
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
import java.util.Map;

public class PWPLobbyScreen extends Screen {

    private static PWPLobbyScreen instance;
    private static final Map<String, ResourceLocation> imageCache = new HashMap<>();

    private static final String[] TAB_NAMES = { "ИГРА", "ГОЛОСОВАНИЕ", "ЛИДЕРЫ", "ПРОФИЛЬ" };
    private static final int TAB_PADDING = 14;

    private LobbyTab currentTab = LobbyTab.IGRA;
    private int hoveredTab = -1;

    private final PlayTabRenderer playTab = new PlayTabRenderer();
    private final VoteTabRenderer voteTab = new VoteTabRenderer();
    private final LeaderTabRenderer leaderTab = new LeaderTabRenderer();
    private final ProfileTabRenderer profileTab = new ProfileTabRenderer();

    private OpenMatchListScreenPacket listData;
    private OpenMatchScreenPacket matchData;

    private final long openTime;
    private long tabSwitchTime = -1;
    private LobbyTab tabSwitchFrom;

    public PWPLobbyScreen() {
        super(Component.literal("PWP Лобби"));
        openTime = System.currentTimeMillis();
        instance = this;
    }
    @Override
    public void onClose() {
        super.onClose();
        instance = null;
    }

    public static void openMatch(OpenMatchScreenPacket pkt) {
        ensureOpenOrCreate(s -> { s.matchData = pkt; s.playTab.setData(pkt, s.listData); });
    }

    private static void ensureOpenOrCreate(java.util.function.Consumer<PWPLobbyScreen> init) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (instance == null || instance.isMinecraftScreenInvalid()) {
            PWPLobbyScreen s = new PWPLobbyScreen();
            init.accept(s);
            mc.setScreen(s);
        } else {
            init.accept(instance);
        }
    }

    private boolean isMinecraftScreenInvalid() {
        return Minecraft.getInstance().screen != this;
    }

    public static void openVote(OpenVotingScreenPacket pkt) {
        PWPToastManager.show("Голосование за карту началось!", PWPToastManager.ToastType.INFO);
        if (instance == null || instance.isMinecraftScreenInvalid()) {
            ensureOpenOrCreate(s -> { s.currentTab = LobbyTab.GOLOSOVANIE; s.voteTab.initVoteData(pkt); });
        } else {
            instance.currentTab = LobbyTab.GOLOSOVANIE;
            instance.voteTab.updateVoteData(pkt);
        }
    }

    public static void openModeVote(OpenModeVotePacket pkt) {
        PWPToastManager.show("Голосование за режим!", PWPToastManager.ToastType.INFO);
        if (instance == null || instance.isMinecraftScreenInvalid()) {
            ensureOpenOrCreate(s -> { s.currentTab = LobbyTab.GOLOSOVANIE; s.voteTab.initModeVoteData(pkt); });
        } else {
            instance.currentTab = LobbyTab.GOLOSOVANIE;
            instance.voteTab.updateModeVoteData(pkt);
        }
    }

    public static void openFactionVote(OpenFactionVotePacket pkt) {
        PWPToastManager.show("Голосование за фракции!", PWPToastManager.ToastType.INFO);
        if (instance == null || instance.isMinecraftScreenInvalid()) {
            ensureOpenOrCreate(s -> { s.currentTab = LobbyTab.GOLOSOVANIE; s.voteTab.initFactionVoteData(pkt); });
        } else {
            instance.currentTab = LobbyTab.GOLOSOVANIE;
            instance.voteTab.updateFactionVoteData(pkt);
        }
    }

    public static void openList(OpenMatchListScreenPacket pkt) {
        ensureOpenOrCreate(s -> { s.listData = pkt; s.playTab.setData(null, pkt); });
    }

    public static void updateList(OpenMatchListScreenPacket pkt) {
        if (instance == null) return;
        instance.listData = pkt;
        instance.playTab.setData(instance.matchData, pkt);
    }

    public static void resetInstance() { instance = null; imageCache.clear(); }

    public static ResourceLocation getTexture(String mapName, String worldPath, String prefix) {
        if (worldPath == null || worldPath.isEmpty() || !Files.exists(Paths.get(worldPath, "icon.png"))) return null;
        String key = prefix + mapName;
        if (imageCache.containsKey(key)) return imageCache.get(key);
        try {
            NativeImage img = NativeImage.read(new FileInputStream(Paths.get(worldPath, "icon.png").toFile()));
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation rl = new ResourceLocation("pwp_core_client", "map_preview/" + key);
            Minecraft.getInstance().getTextureManager().register(rl, tex);
            imageCache.put(key, rl);
            return rl;
        } catch (Exception e) { return null; }
    }
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();

        gui.fill(0, 0, width, height, PWPTheme.Colors.BACKGROUND);
        renderVignette(gui);
        renderTopBar(gui);
        renderTabs(gui, mouseX, mouseY);
        renderTabContent(gui, mouseX, mouseY);
        renderTabSwitchOverlay(gui, now);
        renderBottomBar(gui);
        renderOpenFade(gui, now);
        PWPToastManager.render(gui);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderVignette(GuiGraphics gui) {
        int edge = Math.min(width, height) / 3;
        gui.fillGradient(0, 0, width, edge, 0xCC06080A, 0x00000000);
        gui.fillGradient(0, height - edge, width, height, 0x00000000, 0xCC06080A);
    }

    private void renderTopBar(GuiGraphics gui) {
        var font = PWPTheme.Fonts.display();
        gui.drawString(font, Component.literal("PWP"), 20, 16, PWPTheme.Colors.TEXT_ACCENT, false);
        String online = "ОНЛАЙН: " + (matchData != null ? matchData.onlinePlayers : 0);
        gui.drawString(font, Component.literal(online), width - 20 - font.width(online), 16, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.fill(16, 36, width - 16, 37, PWPTheme.Colors.BORDER);
    }

    private void renderTabs(GuiGraphics gui, int mouseX, int mouseY) {
        var font = PWPTheme.Fonts.display();
        int tabY = 48;
        int totalW = 0;
        for (String n : TAB_NAMES) totalW += font.width(n) + TAB_PADDING * 2;
        int startX = (width - totalW) / 2;
        hoveredTab = -1;
        int cx = startX;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tabW = font.width(TAB_NAMES[i]) + TAB_PADDING * 2;
            boolean active = currentTab.ordinal() == i;
            boolean hovered = mouseX >= cx && mouseX <= cx + tabW && mouseY >= tabY && mouseY <= tabY + 20;
            if (hovered) hoveredTab = i;
            int color = active ? PWPTheme.Colors.TEXT_ACCENT : (hovered ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY);
            gui.drawString(font, Component.literal(TAB_NAMES[i]), cx + TAB_PADDING, tabY + 4, color, false);
            if (active) gui.fill(cx + TAB_PADDING, tabY + 20, cx + tabW - TAB_PADDING, tabY + 22, PWPTheme.Colors.ACCENT);
            cx += tabW;
        }
    }

    private void renderTabContent(GuiGraphics gui, int mouseX, int mouseY) {
        switch (currentTab) {
            case IGRA -> playTab.render(gui, width, height, mouseX, mouseY);
            case GOLOSOVANIE -> voteTab.render(gui, width, height, mouseX, mouseY);
            case LIDERY -> leaderTab.render(gui, width, height, mouseX, mouseY);
            case PROFIL -> profileTab.render(gui, width, height, mouseX, mouseY);
        }
    }

    private void renderTabSwitchOverlay(GuiGraphics gui, long now) {
        if (tabSwitchTime < 0) return;
        long elapsed = now - tabSwitchTime;
        if (elapsed < 400) {
            float t = Math.min(elapsed / 400f, 1);
            float f = t < 0.5f ? t * 2 : 1 - (t - 0.5f) * 2;
            int alpha = Math.min(255, Math.max(0, (int)(f * 128)));
            if (alpha > 0) gui.fill(16, 44, width - 16, height - 16, (alpha << 24) | 0x06080A);
        } else { tabSwitchTime = -1; }
    }

    private void renderBottomBar(GuiGraphics gui) {
        var font = PWPTheme.Fonts.display();
        String status = "ПОДКЛЮЧЕНО  |  PWP ЛОББИ";
        gui.fill(0, height - 14, width, height, PWPTheme.Colors.SURFACE_TOP);
        gui.drawString(font, Component.literal(status), width / 2 - font.width(status) / 2, height - 10, PWPTheme.Colors.TEXT_DIM, false);
    }

    private void renderOpenFade(GuiGraphics gui, long now) {
        long elapsed = now - openTime;
        if (elapsed >= 200) return;
        float t = elapsed / 200f;
        float eased = t < 0.5f ? 2*t*t : -1 + (4-2*t)*t;
        int alpha = (int)((1 - eased) * 255);
        if (alpha > 0) gui.fill(0, 0, width, height, (alpha << 24) | 0x06080A);
    }

    @Override
    public void tick() {
        super.tick();
        leaderTab.tick();
        profileTab.tick();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (System.currentTimeMillis() - openTime < 200) return false;
        if (button == 0) {
            if (hoveredTab >= 0 && hoveredTab < TAB_NAMES.length) {
                LobbyTab newTab = LobbyTab.values()[hoveredTab];
                if (newTab != currentTab && tabSwitchTime < 0) {
                    tabSwitchFrom = currentTab;
                    currentTab = newTab;
                    tabSwitchTime = System.currentTimeMillis();
                    if (newTab == LobbyTab.LIDERY) leaderTab.requestData("score", 1);
                    if (newTab == LobbyTab.PROFIL && Minecraft.getInstance().player != null) {
                        profileTab.requestProfile(Minecraft.getInstance().player.getStringUUID());
                    }
                }
                return true;
            }
            // Tab content clicks
            if (currentTab == LobbyTab.IGRA) {
                int hi = playTab.getHoveredIndex();
                if (hi >= 0) {
                    int sid = playTab.getServerId(hi);
                    if (sid >= 0) {
                        PacketHandler.INSTANCE.sendToServer(new JoinMatchServerPacket(sid));
                        return true;
                    }
                }
            }
            if (currentTab == LobbyTab.GOLOSOVANIE) {
                return voteTab.mouseClicked(mx, my);
            }
            if (currentTab == LobbyTab.LIDERY) {
                return leaderTab.mouseClicked(mx, my);
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
    @Override public boolean isPauseScreen() { return false; }
}