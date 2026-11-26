package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.warfare.client.gui.BuildRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

/**
 * Плашка «ФОБ: N мат.» в стеке низ-лево (этап 17c, как в Squad): пока игрок в радиусе стройки своего ФОБа, видно,
 * сколько материалов на стройку и снабжение. Красная — меньше цены самой дешёвой стены. Данные — те же
 * {@code clientHubs}, что у радиалки стройки ({@link BuildRadialScreen#nearFobMaterials}).
 */
@OnlyIn(Dist.CLIENT)
public final class FobHud {
    private static final Component LABEL = Component.translatable("gui.pwpwarfare.hud.fob");

    private FobHud() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(FobHud::onHud);
    }

    private static void onHud(RenderGuiOverlayEvent.Post e) {
        if (!e.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator() || !WHud.hudVisible()) return;
        if (com.pwp.coreclient.lobby.LobbyHud.isOnLobby()) return;
        int m = BuildRadialScreen.nearFobMaterials();
        if (m < 0) return;
        boolean low = m < com.pigeostudios.pwp.warfare.build.Structure.WALL_1X1.cost;
        WHud.stackPlate(
                e.getGuiGraphics(),
                PWPTheme.Fonts.display(),
                WHud.zones().bottomLeftStack(),
                WarfareIcon.SPAWN_FOB,
                LABEL,
                String.valueOf(m),
                low ? WTheme.HUD_DANGER : WTheme.HUD_VALUE,
                -1F,
                low ? WTheme.HUD_DANGER : PWPTheme.Colors.ACCENT,
                1F);
    }
}
