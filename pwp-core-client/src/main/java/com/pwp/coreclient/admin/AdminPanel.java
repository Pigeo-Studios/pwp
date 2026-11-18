package com.pwp.coreclient.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.pwp.api.security.AccessLevel;
import com.pwp.coreclient.gui.devtest.UiGallery;
import com.pwp.coreclient.gui.screens.PWPPauseScreen;
import com.pwp.coreclient.network.data.AdminInfo;
import com.pwp.coreclient.network.data.ClientDataResponses;
import com.pwp.coreclient.ui.UIToast;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Админ-панель PWP — единая точка входа (клавиша F6 «Админ-панель» и блок кнопок в меню паузы).
 *
 * <p>Доступ: уровень {@link AccessLevel} приходит с сервера пакетом {@code AccessLevelPacket} (при входе
 * и при смене op/роли) и ответом {@code adminInfo}. Ниже MODERATOR — ни кнопок, ни клавиши. Это только
 * скрытие UI: каждое действие сервер проверяет заново.
 *
 * <p>Реестр вкладок: моды регистрируют {@link #registerTab} (id, ключ заголовка, порядок, минимальный
 * уровень, фабрика, условие по разделам сервера). Встроенные: «Модерация» (MODERATOR), «Лобби» (ADMIN,
 * только лобби-сервер); warfare — «Матч»/«Карта» (ADMIN).
 */
@OnlyIn(Dist.CLIENT)
public final class AdminPanel {
    private static final Logger LOG = LoggerFactory.getLogger(AdminPanel.class);

    /** F6 свободна в ванили и наших модах (F7–F10 — голосования warfare). */
    public static final KeyMapping ADMIN_KEY = new KeyMapping(
            "key.pwp_core.admin_panel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "key.categories.pwp");

    /** Повторный запрос adminInfo не чаще (мс). */
    private static final long REFRESH_MS = 2000L;

    /** Что известно о сервере и игроке при построении вкладок. */
    public record Context(AccessLevel level, Set<String> features, boolean preview) {
        public Context {
            features = Set.copyOf(features);
            level = level == null ? AccessLevel.PLAYER : level;
        }

        public boolean has(String feature) {
            return features.contains(feature);
        }

        public boolean atLeast(AccessLevel required) {
            return level.atLeast(required);
        }
    }

    /** Описание вкладки в реестре. */
    public record TabSpec(
            String id,
            String titleKey,
            int order,
            AccessLevel minLevel,
            Function<Context, AdminTab> factory,
            Predicate<Context> visibleIf) {}

    private static final List<TabSpec> TABS = new CopyOnWriteArrayList<>();

    private static AccessLevel level = AccessLevel.PLAYER;
    private static Set<String> features = Set.of();
    private static boolean openOnReply;
    private static long lastRefresh;

    private AdminPanel() {}

    /**
     * Регистрирует (или заменяет по id) вкладку. Порядок — {@code order} (меньше — выше).
     *
     * @param minLevel минимальный уровень доступа (вкладка не видна ниже него)
     * @param visibleIf условие по разделам сервера; null — всегда
     */
    public static void registerTab(
            String id,
            String titleKey,
            int order,
            AccessLevel minLevel,
            Function<Context, AdminTab> factory,
            Predicate<Context> visibleIf) {
        TABS.removeIf(t -> t.id().equals(id));
        TABS.add(new TabSpec(id, titleKey, order, minLevel, factory, visibleIf == null ? c -> true : visibleIf));
    }

    /** Видимые вкладки для контекста, по порядку. */
    public static List<TabSpec> tabs(Context ctx) {
        List<TabSpec> out = new ArrayList<>();
        for (TabSpec t : TABS) {
            try {
                if (ctx.atLeast(t.minLevel()) && t.visibleIf().test(ctx)) out.add(t);
            } catch (RuntimeException e) {
                LOG.warn("AdminPanel: условие видимости вкладки {} упало", t.id(), e);
            }
        }
        out.sort(Comparator.comparingInt(TabSpec::order).thenComparing(TabSpec::id));
        return out;
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(AdminPanel::onRegisterKeys);
        MinecraftForge.EVENT_BUS.addListener(AdminPanel::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(AdminPanel::onLoggingIn);
        MinecraftForge.EVENT_BUS.addListener(AdminPanel::onLoggingOut);
        ClientDataResponses.listen(AdminPanel::onData);
        // встроенные вкладки core-client
        registerTab(
                "moderation",
                "pwp_core.admin.tab.moderation",
                10,
                AccessLevel.MODERATOR,
                c -> c.preview()
                        ? new ModerationTab(ModerationTab.fixture(UiGallery.emptyData()))
                        : new ModerationTab(),
                c -> c.has(AdminInfo.FEATURE_MODERATION));
        registerTab(
                "lobby",
                "pwp_core.admin.tab.lobby",
                30,
                AccessLevel.ADMIN,
                c -> new LobbyAdminTab(
                        c.atLeast(AccessLevel.OWNER),
                        c.preview() ? LobbyAdminTab.fixture(UiGallery.emptyData()) : null),
                c -> c.has(AdminInfo.FEATURE_LOBBY));
    }

    /** Уровень доступа, сообщённый сервером. */
    public static AccessLevel level() {
        return level;
    }

    /** Есть ли у игрока хоть какой-то админ-доступ (кнопки паузы, клавиша). */
    public static boolean hasAccess() {
        return level.atLeast(AccessLevel.MODERATOR);
    }

    public static Set<String> features() {
        return features;
    }

    /** Открывашка редактора карты (ставит warfare, этап 3): parent → экран редактора. */
    private static volatile java.util.function.Function<Screen, Screen> mapEditor;

    public static void setMapEditor(java.util.function.Function<Screen, Screen> opener) {
        mapEditor = opener;
    }

    /** Редактор доступен: мод его зарегистрировал, уровень ADMIN+ и сервер объявил раздел «Карта». */
    public static boolean mapEditorAvailable() {
        return mapEditor != null
                && level.atLeast(AccessLevel.ADMIN)
                && features.contains(com.pwp.coreclient.network.data.AdminInfo.FEATURE_MAP);
    }

    /** Открыть редактор карты (права всё равно проверяет сервер в каждом пакете). */
    public static void openMapEditor(Screen parent) {
        java.util.function.Function<Screen, Screen> f = mapEditor;
        if (f == null) return;
        Minecraft.getInstance().setScreen(f.apply(parent));
    }

    /** Спросить сервер (не чаще раза в 2 с). */
    public static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastRefresh < REFRESH_MS) return;
        lastRefresh = now;
        AdminBackend.NETWORK.request(AdminInfo.TYPE, "");
    }

    /** Открыть панель: доступ и разделы известны — сразу, иначе — после ответа сервера. */
    public static void open(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (hasAccess() && !features.isEmpty()) {
            mc.setScreen(new AdminPanelScreen(parent, new Context(level, features, false), AdminBackend.NETWORK));
            return;
        }
        openOnReply = true;
        lastRefresh = 0L;
        refresh();
    }

    /** Пакет уровня от сервера (главный поток клиента). */
    public static void onLevel(String name) {
        AccessLevel next = AccessLevel.parse(name);
        if (next == level) return;
        boolean hadAccess = hasAccess();
        level = next;
        // разделы зависят от уровня — перезапросить
        lastRefresh = 0L;
        if (hasAccess()) refresh();
        else features = Set.of();
        Minecraft mc = Minecraft.getInstance();
        if (hadAccess != hasAccess() && mc.screen instanceof PWPPauseScreen p) p.resize(mc, p.width, p.height);
        // права отобрали, пока панель открыта, — закрыть
        if (!hasAccess() && mc.screen instanceof AdminPanelScreen) mc.setScreen(null);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent e) {
        e.register(ADMIN_KEY);
    }

    private static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        while (ADMIN_KEY.consumeClick()) {
            // игрок без доступа: клавиша ничего не делает (и не шлёт запросов)
            if (hasAccess() && Minecraft.getInstance().screen == null) open(null);
        }
        if (Minecraft.getInstance().screen instanceof AdminPanelScreen s) s.tickTab();
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn e) {
        // уровень НЕ сбрасываем: AccessLevelPacket сервера может прийти раньше этого события
        openOnReply = false;
        lastRefresh = 0L;
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        reset();
    }

    private static void reset() {
        level = AccessLevel.PLAYER;
        features = Set.of();
        openOnReply = false;
        lastRefresh = 0L;
    }

    private static void onData(String type, String json) {
        if (AdminInfo.TYPE.equals(type)) {
            boolean hadAccess = hasAccess();
            parseInfo(json);
            Minecraft mc = Minecraft.getInstance();
            if (hadAccess != hasAccess() && mc.screen instanceof PWPPauseScreen p) p.resize(mc, p.width, p.height);
            if (openOnReply) {
                openOnReply = false;
                if (hasAccess()) {
                    if (mc.screen == null || mc.screen instanceof PWPPauseScreen) open(mc.screen);
                } else {
                    UIToast.show(
                            "admin_denied",
                            Component.translatable("pwp_core.admin.error.no_permission"),
                            UIToast.Kind.ERROR,
                            4000L);
                }
            }
        }
        if (Minecraft.getInstance().screen instanceof AdminPanelScreen s) s.onData(type, json);
    }

    private static void parseInfo(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            level = AccessLevel.parse(o.has("level") ? o.get("level").getAsString() : null);
            Set<String> f = new TreeSet<>();
            if (o.has("features") && o.get("features").isJsonArray()) {
                for (JsonElement e : o.getAsJsonArray("features")) f.add(e.getAsString());
            }
            features = Set.copyOf(f);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            LOG.warn("AdminPanel: битый ответ adminInfo", e);
            level = AccessLevel.PLAYER;
            features = Set.of();
        }
    }
}
