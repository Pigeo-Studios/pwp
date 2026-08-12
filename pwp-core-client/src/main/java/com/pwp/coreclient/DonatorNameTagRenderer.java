package com.pwp.coreclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pwp.coreclient.donor.DonorLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Донат-надмид: «живой» градиент (тиры золото/серебро/платина и роли ADMIN/MODERATOR)
 * вместо ванильного имени + мягкий ореол цветом уровня (дилатация на 8 направлений,
 * низкая альфа — в отличие от старой «тени из 4 копий» не даёт двойного ника).
 * Плавный фейд по дистанции 32->64 блока. Уровни приходят только с лобби-сервера.
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DonatorNameTagRenderer {

    private static final double FADE_START = 48.0;
    private static final double FADE_END = 64.0;

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
        DonorLevel lvl = DonorLevel.byName(level);
        if (lvl == null) return;

        // Плавный фейд по дистанции вместо жёсткого обрыва
        double dist = Math.sqrt(mc.getEntityRenderDispatcher().distanceToSqr(entity));
        float fade = dist <= FADE_START ? 1.0f : (float) Math.max(0.0, 1.0 - (dist - FADE_START) / (FADE_END - FADE_START));
        if (fade <= 0.0f) return;

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

        // Ореол: двухкольцевая дилатация (r=1, r=2 с затуханием альфы) цветом уровня —
        // мягкий переход без «пиксель-гепа» однопиксельных копий. Низкие альфы (0x10/0x08)
        // не дают «ступенек» в 1 px и эффекта двойного ника при наложении 24 копий.
        int rgb = lvl.glowColor() & 0xFFFFFF;
        for (int ring = 1; ring <= 2; ring++) {
            int alpha = (int) ((ring == 1 ? 0x10 : 0x08) * fade);
            int glowCol = rgb | (alpha << 24);
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dy = -ring; dy <= ring; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    if (Math.abs(dx) != ring && Math.abs(dy) != ring) continue;
                    font.drawInBatch(text, x + dx, dy, glowCol, false, mat, buffer,
                            Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
                }
            }
        }

        // Градиентный текст поверх
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int w = font.width(ch);
            int c = NameGradient.colorAt(level, cursor + w / 2.0 - x, totalWidth, now);
            int a = (int) ((c >> 24 & 0xFF) * fade);
            c = (c & 0xFFFFFF) | (a << 24);
            font.drawInBatch(ch, cursor, 0, c, false, mat, buffer,
                    Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
            cursor += w;
        }
        poseStack.popPose();
    }
}
