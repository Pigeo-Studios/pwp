package com.pigeostudios.pwp.warfare.client.admin;

import com.pigeostudios.pwp.warfare.admin.PlayerAdminPackets;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pwp.api.security.AccessLevel;
import com.pwp.coreclient.admin.AdminPanel;
import com.pwp.coreclient.admin.ModerationTab;
import com.pwp.coreclient.gui.devtest.UiGallery;
import com.pwp.coreclient.network.data.AdminInfo;
import com.pwp.coreclient.ui.UIIcon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Вкладки warfare в админ-панели core-client: «Матч» и «Карта» (ADMIN+, раздел сервера match/map). */
@OnlyIn(Dist.CLIENT)
public final class WarfareAdminTabs {
    private WarfareAdminTabs() {}

    private static void addPlayerAction(
            String id, String key, UIIcon icon, boolean confirm, java.util.function.Function<String, Object> packet) {
        ModerationTab.addAction(new ModerationTab.ExtraAction(
                id,
                "pwpwarfare.admin.player." + key,
                icon,
                confirm,
                AccessLevel.ADMIN,
                name -> PacketHandler.INSTANCE.sendToServer(packet.apply(name))));
    }

    public static void register() {
        // кнопки вкладки «Игроки» (core-client): сервер warfare объявляет их в adminPlayers
        addPlayerAction(
                PlayerAdminPackets.RESET_KIT, "reset_kit", UIIcon.REPLAY, true, PlayerAdminPackets.ResetKit::new);
        addPlayerAction(
                PlayerAdminPackets.RESET_COOLDOWNS,
                "reset_cooldowns",
                UIIcon.REPLAY,
                false,
                PlayerAdminPackets.ResetCooldowns::new);
        addPlayerAction(
                PlayerAdminPackets.GIVE_LEADER,
                "give_leader",
                UIIcon.CHECK,
                false,
                n -> new PlayerAdminPackets.Leader(n, true));
        addPlayerAction(
                PlayerAdminPackets.TAKE_LEADER,
                "take_leader",
                UIIcon.CLOSE,
                true,
                n -> new PlayerAdminPackets.Leader(n, false));
        AdminPanel.registerTab(
                "match",
                "pwpwarfare.admin.tab.match",
                20,
                AccessLevel.ADMIN,
                c -> new MatchAdminTab(
                        c.atLeast(AccessLevel.OWNER),
                        c.preview() ? AdminMatchData.fixture(UiGallery.emptyData()) : null),
                c -> c.has(AdminInfo.FEATURE_MATCH));
        // этап 6d: редактор состава дивизий фракций матча
        AdminPanel.registerTab(
                "divisions",
                "pwpwarfare.admin.tab.divisions",
                30,
                AccessLevel.ADMIN,
                c -> new DivisionAdminTab(c.preview() ? DivisionAdminTab.fixture(UiGallery.emptyData()) : null),
                c -> c.has(AdminInfo.FEATURE_MATCH));
        AdminPanel.registerTab(
                "map",
                "pwpwarfare.admin.tab.map",
                40,
                AccessLevel.ADMIN,
                c -> new MapAdminTab(c.preview() ? AdminMatchData.fixture(UiGallery.emptyData()) : null),
                c -> c.has(AdminInfo.FEATURE_MAP));
    }
}
