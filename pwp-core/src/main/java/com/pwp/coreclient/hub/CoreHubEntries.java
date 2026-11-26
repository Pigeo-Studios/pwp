package com.pwp.coreclient.hub;

import com.pwp.api.security.AccessLevel;
import com.pwp.coreclient.admin.AdminPanel;
import com.pwp.coreclient.gui.StatsScreen;
import com.pwp.coreclient.gui.screens.PWPCommunityScreen;
import com.pwp.coreclient.gui.screens.PWPControlsScreen;
import com.pwp.coreclient.gui.screens.PWPHowToPlayScreen;
import com.pwp.coreclient.gui.screens.PWPShopScreen;
import com.pwp.coreclient.gui.screens.PWPUiSettingsScreen;
import com.pwp.coreclient.lobby.LobbyHud;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.cosmetics.gui.SkinInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Карточки «Меню PWP» от core (warfare добавляет свои — деплой, карта, редакторы контента). */
@OnlyIn(Dist.CLIENT)
public final class CoreHubEntries {
    private CoreHubEntries() {}

    private static boolean online() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getConnection() != null && !mc.isLocalServer();
    }

    public static void register() {
        // ИГРА
        PwpHub.register(
                PwpHub.Section.PLAY,
                "lobby",
                10,
                UIIcon.SERVER,
                parent -> {
                    Minecraft.getInstance().setScreen(null);
                    LobbyHud.openLobby();
                    return null;
                },
                LobbyHud::isOnLobby,
                () -> LobbyHud.LOBBY_KEY);
        PwpHub.register(PwpHub.Section.PLAY, "howto", 90, UIIcon.INFO, PWPHowToPlayScreen::new, null, null);
        PwpHub.register(
                PwpHub.Section.COMMUNITY,
                "news",
                5,
                UIIcon.INFO,
                com.pwp.coreclient.gui.screens.PWPNewsScreen::new,
                null,
                null);

        // ПРОФИЛЬ
        PwpHub.register(PwpHub.Section.PROFILE, "stats", 10, UIIcon.PROFILE, p -> new StatsScreen(), null, null);
        PwpHub.register(
                PwpHub.Section.PROFILE, "arsenal", 20, UIIcon.SPECIALIST, p -> new SkinInventoryScreen(), null, null);
        PwpHub.register(
                PwpHub.Section.PROFILE, "shop", 30, UIIcon.CHECK, PWPShopScreen::new, CoreHubEntries::online, null);

        // СООБЩЕСТВО
        PwpHub.register(
                PwpHub.Section.COMMUNITY,
                "community",
                10,
                UIIcon.WARNING,
                PWPCommunityScreen::new,
                CoreHubEntries::online,
                null);

        // НАСТРОЙКИ
        PwpHub.register(PwpHub.Section.SETTINGS, "ui", 10, UIIcon.SLIDERS, PWPUiSettingsScreen::new, null, null);
        PwpHub.register(PwpHub.Section.SETTINGS, "controls", 20, UIIcon.KEYBOARD, PWPControlsScreen::new, null, null);
        PwpHub.register(
                PwpHub.Section.SETTINGS,
                "options",
                30,
                UIIcon.SETTINGS,
                p -> new OptionsScreen(p, Minecraft.getInstance().options),
                null,
                null);
        PwpHub.register(
                PwpHub.Section.SETTINGS,
                "mods",
                40,
                UIIcon.MODS,
                net.minecraftforge.client.gui.ModListScreen::new,
                null,
                null);

        // АДМИН
        PwpHub.register(
                PwpHub.Section.ADMIN,
                "admin_panel",
                10,
                UIIcon.SERVER,
                p -> {
                    AdminPanel.open(p);
                    return null;
                },
                AdminPanel::hasAccess,
                () -> AdminPanel.ADMIN_KEY);
        PwpHub.register(
                PwpHub.Section.ADMIN,
                "map_editor",
                20,
                UIIcon.SETTINGS,
                p -> {
                    AdminPanel.openMapEditor(p);
                    return null;
                },
                () -> AdminPanel.level().atLeast(AccessLevel.ADMIN) && AdminPanel.mapEditorAvailable(),
                null);
    }
}
