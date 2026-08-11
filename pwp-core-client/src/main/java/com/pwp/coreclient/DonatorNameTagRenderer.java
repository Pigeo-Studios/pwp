package com.pwp.coreclient;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Донат-надмид: рисует «живой» градиент (тиры золото/серебро/платина и роли ADMIN/MODERATOR)
 * вместо ванильного имени. Уровни приходят только с лобби-сервера (PacketDonatorTiers).
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DonatorNameTagRenderer {

    private DonatorNameTagRenderer() {}

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.getType() != EntityType.PLAYER) return;
        String level = DonatorCache.levelOf(entity.getUUID());
        if (level == null) return;
        // Прячем ванильный надмид и рисуем свой
        event.setResult(Event.Result.DENY);
        renderWorldGradient(entity, level, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    @SubscribeEvent
    public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DonatorCache.clear();
    }

    private static void renderWorldGradient(Entity entity, String level, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        double distSq = mc.getEntityRenderDispatcher().distanceToSqr(entity);
        if (distSq > 4096.0) return;

        String text = entity.getName().getString();
        if (text.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.5D, 0.0D);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float scale = 0.025F;
        poseStack.scale(-scale, -scale, scale);

        int totalWidth = font.width(text);
        int x = -totalWidth / 2;
        long now = System.currentTimeMillis();
        Matrix4f mat = new Matrix4f(poseStack.last().pose());
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int w = font.width(ch);
            font.drawInBatch(ch, cursor, 0,
                NameGradient.colorAt(level, cursor + w / 2.0 - x, totalWidth, now), false, mat, buffer,
                Font.DisplayMode.NORMAL, 0, packedLight);
            cursor += w;
        }
        poseStack.popPose();
    }
}
