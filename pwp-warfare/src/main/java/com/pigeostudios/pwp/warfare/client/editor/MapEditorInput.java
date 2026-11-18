package com.pigeostudios.pwp.warfare.client.editor;

import com.pigeostudios.pwp.warfare.client.NotificationFeed;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pwp.api.security.AccessLevel;
import com.pwp.coreclient.admin.AdminPanel;
import com.pwp.coreclient.ui.UIDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Режим редактора «в мире» (без предмета): пока он включён и экран закрыт, ЛКМ/ПКМ по блоку не ломают и не
 * используют блок, а ставят угол/позицию выбранного объекта черновика ({@link MapEditorSession#worldClick}).
 * Ничего не уходит на сервер до «Применить». Вверху экрана — подсказка с выбранным объектом.
 */
@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
public final class MapEditorInput {
    private MapEditorInput() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (!MapEditorSession.worldMode()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;
        if (!AdminPanel.level().atLeast(AccessLevel.ADMIN)) {
            MapEditorSession.setWorldMode(false);
            return;
        }
        if (!event.isAttack() && !event.isUseItem()) return;
        // режим редактора съедает клики целиком: блок не ломается, предмет не используется
        event.setCanceled(true);
        event.setSwingHand(false);
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos p = bh.getBlockPos();
        String key = MapEditorSession.worldClick(event.isAttack(), p.getX(), p.getY(), p.getZ(), mc.player.getYRot());
        if (key != null) NotificationFeed.push(key, NotificationFeed.SEVERITY_INFO);
    }

    /** Выход с сервера — режим выключается (черновик остаётся до следующего открытия редактора). */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MapEditorSession.setWorldMode(false);
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (!MapEditorSession.worldMode() || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;
        var g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        Component sel = MapEditorScreen.describe(MapEditorSession.draft(), MapEditorSession.selected());
        Component line1 = Component.translatable("pwpwarfare.editor.world.hud", sel);
        Component line2 = Component.translatable(MapEditorScreen.worldHintKey(MapEditorSession.selected()));
        int y = WTheme.MARGIN_MAX;
        UIDraw.centered(g, mc.font, line1, w / 2, y, WTheme.EDITOR_HIGHLIGHT);
        UIDraw.centered(g, mc.font, line2, w / 2, y + WTheme.ROW_H, WTheme.EDITOR_BORDER);
    }
}
