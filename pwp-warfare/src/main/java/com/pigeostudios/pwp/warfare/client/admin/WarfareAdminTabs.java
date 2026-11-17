package com.pigeostudios.pwp.warfare.client.admin;

import com.pwp.api.security.AccessLevel;
import com.pwp.coreclient.admin.AdminPanel;
import com.pwp.coreclient.gui.devtest.UiGallery;
import com.pwp.coreclient.network.data.AdminInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Вкладки warfare в админ-панели core-client: «Матч» и «Карта» (ADMIN+, раздел сервера match/map). */
@OnlyIn(Dist.CLIENT)
public final class WarfareAdminTabs {
    private WarfareAdminTabs() {}

    public static void register() {
        AdminPanel.registerTab(
                "match",
                "pwpwarfare.admin.tab.match",
                20,
                AccessLevel.ADMIN,
                c -> new MatchAdminTab(
                        c.atLeast(AccessLevel.OWNER),
                        c.preview() ? AdminMatchData.fixture(UiGallery.emptyData()) : null),
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
