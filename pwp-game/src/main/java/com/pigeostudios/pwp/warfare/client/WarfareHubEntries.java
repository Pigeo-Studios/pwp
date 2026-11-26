package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.DeployScreen;
import com.pigeostudios.pwp.warfare.client.gui.FactionSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.FactionVehicleSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.KitTeamSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.SkinListScreen;
import com.pigeostudios.pwp.warfare.client.gui.SquadMapScreen;
import com.pigeostudios.pwp.warfare.client.gui.TeamSelectionScreen;
import com.pwp.api.security.AccessLevel;
import com.pwp.coreclient.admin.AdminPanel;
import com.pwp.coreclient.hub.PwpHub;
import com.pwp.coreclient.lobby.LobbyHud;
import com.pwp.coreclient.ui.UIIcon;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Карточки warfare в «Меню PWP» (этап 11): деплой и карта — для матча; редакторы контента (киты фракций,
 * киты команд, техника фракций, каталог скинов) — раздел «Админ», ADMIN+. Раньше редакторы открывались только
 * особыми предметами из креатива — предметы остались, но искать их больше не нужно. Права проверяет сервер
 * в каждом пакете редакторов.
 */
@OnlyIn(Dist.CLIENT)
public final class WarfareHubEntries {
    private WarfareHubEntries() {}

    private static boolean inMatch() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.getConnection() != null && !LobbyHud.isOnLobby();
    }

    private static boolean admin() {
        return AdminPanel.level().atLeast(AccessLevel.ADMIN);
    }

    public static void register() {
        PwpHub.register(
                PwpHub.Section.PLAY,
                "deploy",
                20,
                UIIcon.DIRECT_COMBAT,
                p -> {
                    var mc = Minecraft.getInstance();
                    String team = mc.player != null && mc.player.getTeam() != null
                            ? mc.player.getTeam().getName()
                            : "";
                    boolean valid = team.equalsIgnoreCase("Blue") || team.equalsIgnoreCase("Red");
                    return valid ? new DeployScreen() : new TeamSelectionScreen();
                },
                WarfareHubEntries::inMatch,
                () -> ModKeyBindings.OPEN_SQUAD_MENU_KEY);
        PwpHub.register(
                PwpHub.Section.PLAY,
                "map",
                30,
                UIIcon.SPECIALIST,
                p -> new SquadMapScreen(),
                WarfareHubEntries::inMatch,
                () -> ModKeyBindings.OPEN_MAP_KEY);

        PwpHub.register(
                PwpHub.Section.ADMIN,
                "kits_faction",
                30,
                UIIcon.SUPPORT,
                p -> new FactionSelectScreen(),
                WarfareHubEntries::admin,
                null);
        PwpHub.register(
                PwpHub.Section.ADMIN,
                "kits_team",
                40,
                UIIcon.SUPPORT,
                p -> new KitTeamSelectScreen(),
                WarfareHubEntries::admin,
                null);
        PwpHub.register(
                PwpHub.Section.ADMIN,
                "vehicles",
                50,
                UIIcon.FIRE_SUPPORT,
                p -> new FactionVehicleSelectScreen(),
                WarfareHubEntries::admin,
                null);
        PwpHub.register(
                PwpHub.Section.ADMIN,
                "skins_catalog",
                60,
                UIIcon.PROFILE,
                p -> new SkinListScreen(),
                WarfareHubEntries::admin,
                null);
    }
}
