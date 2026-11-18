package com.pwp.coreclient.gui.screens;

import com.mojang.blaze3d.platform.NativeImage;
import com.pwp.coreclient.gui.components.PWPToastManager;
import com.pwp.coreclient.gui.screens.tabs.LeaderTabRenderer;
import com.pwp.coreclient.gui.screens.tabs.LobbyBackend;
import com.pwp.coreclient.gui.screens.tabs.LobbyTab;
import com.pwp.coreclient.gui.screens.tabs.LobbyTabView;
import com.pwp.coreclient.gui.screens.tabs.PlayTabRenderer;
import com.pwp.coreclient.gui.screens.tabs.ProfileTabRenderer;
import com.pwp.coreclient.gui.screens.tabs.VoteTabRenderer;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.LobbyStatePacket;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UITabs;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Rect;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Лобби PWP: верхняя полоса (логотип + онлайн), вкладки ИГРА / ГОЛОСОВАНИЕ / ЛИДЕРЫ /
 * ПРОФИЛЬ, контент вкладки ({@link LobbyTabView}) в области под вкладками, подвал. Всё
 * состояние — из {@link LobbyStatePacket} (сервер шлёт раз в секунду и при изменениях);
 * раскладка — в {@link #init()} через Rect (пересчёт при каждом изменении окна/GUI scale).
 */
public class PWPLobbyScreen extends Screen {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(PWPLobbyScreen.class);

    private static PWPLobbyScreen instance;
    private static final Map<String, ResourceLocation> imageCache = new HashMap<>();

    private static final String[] TAB_KEYS = {
        "pwp_core.lobby.tab.play", "pwp_core.lobby.tab.vote", "pwp_core.lobby.tab.leaders", "pwp_core.lobby.tab.profile"
    };
    private static final long OPEN_FADE_MS = 200;

    private LobbyTab currentTab = LobbyTab.IGRA;
    private final UILayer layer = new UILayer();
    private UITabs tabs;
    private Rect content;

    // вкладки
    private final PlayTabRenderer playTab = new PlayTabRenderer();
    private final VoteTabRenderer voteTab = new VoteTabRenderer();
    private final LeaderTabRenderer leaderTab = new LeaderTabRenderer();
    private final ProfileTabRenderer profileTab = new ProfileTabRenderer();

    private LobbyStatePacket lastState;
    private int onlinePlayers = 0;
    private int prevPacketPhase = -1;

    // GUI не открывается принудительно, если игрок сам закрыл его в этой фазе (закрытие привязано к фазе голосования)
    private static int closedPhase = -1;
    private static int lastSeenPhase = -1;

    private final long openTime;
    private long tabSwitchTime = -1;
    /** Тест-стенд UI: ничего не отправляется на сервер, данные — из фикстур. */
    private boolean preview;

    private String previewUuid;

    public PWPLobbyScreen() {
        super(Component.translatable("pwp_core.lobby.title"));
        openTime = System.currentTimeMillis();
        instance = this;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (preview) return;
        closedPhase = lastSeenPhase;
        instance = null;
    }

    /**
     * Экран лобби для тест-стенда UI ({@code /pwpui}): действия уходят в {@link LobbyBackend#PREVIEW}
     * (в сеть ничего не отправляется), лидеры/профиль — из переданных фикстур (null — «пусто»),
     * автооткрытие/статика экземпляра не трогаются.
     */
    public static PWPLobbyScreen preview(
            LobbyStatePacket state,
            java.util.function.Supplier<com.google.gson.JsonObject> leaderboard,
            java.util.function.Function<String, com.google.gson.JsonObject> profile,
            LobbyTab tab) {
        PWPLobbyScreen prev = instance;
        PWPLobbyScreen s = new PWPLobbyScreen();
        instance = prev; // превью не участвует в авто-открытии по пакетам
        s.preview = true;
        s.previewUuid = "00000000-0000-0000-0000-000000000000";
        s.playTab.setBackend(LobbyBackend.PREVIEW);
        s.voteTab.setBackend(LobbyBackend.PREVIEW);
        s.leaderTab.setBackend(LobbyBackend.PREVIEW);
        s.profileTab.setBackend(LobbyBackend.PREVIEW);
        s.leaderTab.setPreviewSource(leaderboard != null ? leaderboard : () -> null);
        s.profileTab.setPreviewSource(profile != null ? profile : uuid -> null);
        s.applyState(state, false);
        if (tab != null) {
            s.currentTab = LobbyTab.IGRA;
            s.switchTab(tab);
        }
        return s;
    }

    /** Единственный источник данных GUI — серверный state-пакет (каждую секунду + при изменениях). */
    public static void updateLobbyState(LobbyStatePacket pkt) {
        boolean screenOpen = instance != null && !instance.isMinecraftScreenInvalid();
        // Закрытие привязано к фазе: как только фаза сменилась — забываем, что игрок закрывал GUI
        if (pkt.phase != closedPhase) closedPhase = -1;

        if (!screenOpen) {
            boolean open = pkt.requestOpen;
            if (open) {
                // Вход на сервер: открыть, но уважать явный отказ игрока в этом голосовании
                if (isVotePhase(pkt.phase) && !hasVotedInPhase(pkt) && closedPhase == pkt.phase) {
                    open = false;
                }
            } else {
                // Экран закрыт: не спамим по КД. Открываем только при переходе в фазу матча
                // или если игрок уже голосовал в этой фазе (показать прогресс/итоги)
                open = pkt.phase != lastSeenPhase
                        && closedPhase != pkt.phase
                        && (isMatchPhase(pkt.phase) || (isVotePhase(pkt.phase) && hasVotedInPhase(pkt)));
            }
            lastSeenPhase = pkt.phase;
            if (open) {
                ensureOpenOrCreate(s -> s.applyState(pkt, false));
            }
        } else {
            lastSeenPhase = pkt.phase;
            instance.applyState(pkt, true);
        }
    }

    private static boolean isVotePhase(int phase) {
        return phase == LobbyStatePacket.PHASE_MAP_VOTE
                || phase == LobbyStatePacket.PHASE_MODE_VOTE
                || phase == LobbyStatePacket.PHASE_FACTION_VOTE;
    }

    private static boolean isMatchPhase(int phase) {
        return phase == LobbyStatePacket.PHASE_MATCH_STARTING || phase == LobbyStatePacket.PHASE_MATCH_PLAYING;
    }

    private static boolean hasVotedInPhase(LobbyStatePacket pkt) {
        return switch (pkt.phase) {
            case LobbyStatePacket.PHASE_MAP_VOTE -> pkt.myMapVote >= 0;
            case LobbyStatePacket.PHASE_MODE_VOTE -> pkt.myModeVote >= 0;
            case LobbyStatePacket.PHASE_FACTION_VOTE -> pkt.myFaction1 >= 0 || pkt.myFaction2 >= 0;
            default -> false;
        };
    }

    private void applyState(LobbyStatePacket pkt, boolean screenWasOpen) {
        // Автовыбор вкладки при первом состоянии (вход/автооткрытие) — ручной выбор игрока не перезаписываем
        if (lastState == null) {
            currentTab = switch (pkt.phase) {
                case LobbyStatePacket.PHASE_MAP_VOTE,
                        LobbyStatePacket.PHASE_MODE_VOTE,
                        LobbyStatePacket.PHASE_FACTION_VOTE -> LobbyTab.GOLOSOVANIE;
                default -> LobbyTab.IGRA;
            };
            if (tabs != null) tabs.select(currentTab.ordinal(), false);
        }
        lastState = pkt;
        onlinePlayers = pkt.onlinePlayers;
        playTab.applyState(pkt);
        voteTab.applyState(pkt);

        // Тост — только если игрок уже сидит в GUI (иначе экран откроется сам и тост будет дублем)
        if (screenWasOpen && prevPacketPhase >= 0 && pkt.phase != prevPacketPhase) {
            String key =
                    switch (pkt.phase) {
                        case LobbyStatePacket.PHASE_MAP_VOTE -> "pwp_core.lobby.toast.map_vote";
                        case LobbyStatePacket.PHASE_MODE_VOTE -> "pwp_core.lobby.toast.mode_vote";
                        case LobbyStatePacket.PHASE_FACTION_VOTE -> "pwp_core.lobby.toast.faction_vote";
                        default -> null;
                    };
            if (key != null) {
                PWPToastManager.show(Component.translatable(key).getString(), PWPToastManager.ToastType.INFO);
            }
        }
        prevPacketPhase = pkt.phase;
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

    /**
     * Открыть лобби явно (клавиша «Меню лобби», кнопка в паузе, старт голосования) с
     * последним состоянием из {@link LobbyStatePacket}; снимает «игрок закрыл в этой фазе».
     */
    public static void openManually(LobbyStatePacket pkt) {
        closedPhase = -1;
        ensureOpenOrCreate(s -> {
            if (pkt != null) s.applyState(pkt, false);
        });
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        // та же клавиша «Меню лобби» закрывает экран (если не печатаем в поле ввода)
        if (!preview
                && com.pwp.coreclient.lobby.LobbyHud.LOBBY_KEY.matches(key, scan)
                && !(getFocused() instanceof net.minecraft.client.gui.components.EditBox)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    private boolean isMinecraftScreenInvalid() {
        return Minecraft.getInstance().screen != this;
    }

    public static void resetInstance() {
        instance = null;
        imageCache.clear();
        lastSeenPhase = -1;
        closedPhase = -1;
    }

    public static ResourceLocation getTexture(String mapName, String worldPath, String prefix) {
        if (worldPath == null || worldPath.isEmpty()) return null;
        // Ключ по worldPath — у каждой карты своя иконка (mapName всегда "preview")
        String key = prefix + Integer.toHexString(worldPath.hashCode());
        if (imageCache.containsKey(key)) return imageCache.get(key);
        if (!Files.exists(Paths.get(worldPath, "icon.png"))) {
            imageCache.put(key, null); // не проверять диск каждый кадр
            return null;
        }
        try (FileInputStream in =
                new FileInputStream(Paths.get(worldPath, "icon.png").toFile())) {
            NativeImage img = NativeImage.read(in);
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation rl = new ResourceLocation("pwp_core_client", "map_preview/" + key);
            Minecraft.getInstance().getTextureManager().register(rl, tex);
            imageCache.put(key, rl);
            return rl;
        } catch (Exception e) {
            PWP_LOG.warn("PWPLobbyScreen.getTexture: не загружено превью {}", worldPath, e);
            imageCache.put(key, null); // битый файл не перечитывать каждый кадр
            return null;
        }
    }

    @Override
    protected void init() {
        super.init();
        layer.clear();
        Fit.Density d = PWPChrome.density(width, height);
        int m = PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d) + (d == Fit.Density.COMPACT ? Spacing.XS : Spacing.SM);
        int tabH = d.pick(Spacing.LOBBY_TAB_H, Spacing.LOBBY_TAB_H, Spacing.LOBBY_TAB_H_LARGE);
        List<Component> labels = new ArrayList<>();
        for (String k : TAB_KEYS) labels.add(Component.translatable(k));
        tabs = new UITabs(0, 0, 0, labels, i -> switchTab(LobbyTab.values()[i]));
        int tabW = Math.min(width - 2 * m, tabs.preferredWidth());
        tabs.setBounds(Rect.of(m, top, width - 2 * m, tabH).align(tabW, tabH, Align.CENTER, Align.STRETCH));
        tabs.select(currentTab.ordinal(), false);
        layer.add(tabs);

        int gap = d == Fit.Density.COMPACT ? Spacing.XS : Spacing.MD;
        int contentTop = top + tabH + gap;
        int bottom = height - PWPChrome.footerHeight(d) - gap;
        content = Rect.of(m, contentTop, width - 2 * m, bottom - contentTop);
        for (LobbyTabView v : views()) v.layout(content);
    }

    private LobbyTabView[] views() {
        return new LobbyTabView[] {playTab, voteTab, leaderTab, profileTab};
    }

    private LobbyTabView view() {
        return views()[currentTab.ordinal()];
    }

    private void switchTab(LobbyTab newTab) {
        if (newTab == currentTab) return;
        currentTab = newTab;
        tabSwitchTime = System.currentTimeMillis();
        if (tabs != null) tabs.select(newTab.ordinal(), true);
        view().onShow();
        if (newTab == LobbyTab.LIDERY) leaderTab.requestData("score", 1);
        if (newTab == LobbyTab.PROFIL) {
            if (preview) profileTab.requestProfile(previewUuid);
            else if (Minecraft.getInstance().player != null) {
                profileTab.requestProfile(Minecraft.getInstance().player.getStringUUID());
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPRotatingBackground.render(gui, 0, 0, width, height);
        PWPChrome.topBar(gui, font, width, d, 1F, Component.translatable("pwp_core.lobby.online", onlinePlayers));
        layer.render(gui, mouseX, mouseY, partialTick, width, height);
        view().render(gui, mouseX, mouseY, partialTick);
        renderTabSwitchFade(gui, now);
        // этап 5: состояние подготовки матч-сервера — справа в подвале
        PWPChrome.footer(
                gui,
                font,
                width,
                height,
                d,
                Component.translatable("pwp_core.lobby.status"),
                com.pwp.coreclient.lobby.LobbyHud.prepLine(com.pwp.coreclient.lobby.LobbyHud.lastState()));
        renderOpenFade(gui, now);
        PWPToastManager.render(gui);
    }

    /** Смена вкладки: контент проявляется из затемнения (без вспышки на весь экран). */
    private void renderTabSwitchFade(GuiGraphics gui, long now) {
        if (tabSwitchTime < 0 || content == null) return;
        long elapsed = now - tabSwitchTime;
        if (elapsed >= PWPTheme.Animations.DURATION_NORMAL) {
            tabSwitchTime = -1;
            return;
        }
        float t = elapsed / (float) PWPTheme.Animations.DURATION_NORMAL;
        UIDraw.rect(gui, content.outset(Spacing.XS), UIDraw.fade(PWPTheme.Colors.BACKGROUND_DIM, 1F - t));
    }

    private void renderOpenFade(GuiGraphics gui, long now) {
        long elapsed = now - openTime;
        if (elapsed >= OPEN_FADE_MS) return;
        float t = elapsed / (float) OPEN_FADE_MS;
        UIDraw.rect(gui, Rect.screen(width, height), UIDraw.fade(PWPTheme.Colors.BACKGROUND, 1F - t));
    }

    @Override
    public void tick() {
        super.tick();
        leaderTab.tick();
        profileTab.tick();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (System.currentTimeMillis() - openTime < OPEN_FADE_MS) return false;
        if (layer.mouseClicked(mx, my, button)) return true;
        if (view().mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        view().mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (view().mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
