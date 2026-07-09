/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.Font$DisplayMode
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.joml.Matrix4f
 */
package com.example.aas.client.renderer;

import com.example.aas.block.VehicleSpawnerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

public class VehicleSpawnerRenderer
implements BlockEntityRenderer<VehicleSpawnerBlockEntity> {
    private final Font font;

    public VehicleSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    private String formatTime(long seconds) {
        long m = seconds / 60L;
        long s = seconds % 60L;
        return String.format("%d\u043c %d\u0441", m, s);
    }

    public void render(VehicleSpawnerBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        int initialColor;
        String initialText;
        int respawnColor;
        String respawnText;
        if (Minecraft.getInstance().player.distanceToSqr(entity.getBlockPos().getCenter()) > 4096.0) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);
        long currentTick = entity.getLevel().getGameTime();
        long timeLeft = Math.max(0L, (entity.targetSpawnTick - currentTick) / 20L);
        if (entity.hasSpawnedOnce && entity.targetSpawnTick > currentTick) {
            respawnText = "Respawning: " + this.formatTime(timeLeft);
            respawnColor = 0xFFAA00;
        } else {
            respawnText = "Respawn Set: " + this.formatTime(entity.respawnTimeSettings);
            respawnColor = 0xAAAAAA;
        }
        if (!entity.hasSpawnedOnce && entity.targetSpawnTick > currentTick) {
            initialText = "Starting: " + this.formatTime(timeLeft);
            initialColor = 0x55FF55;
        } else {
            initialText = "Initial Set: " + this.formatTime(entity.initialTimeSettings);
            initialColor = 0xAAAAAA;
        }
        Matrix4f matrix = poseStack.last().pose();
        float bgOpacity = 0.25f;
        float x1 = (float)(-this.font.width(respawnText)) / 2.0f;
        this.font.drawInBatch((Component)Component.literal((String)respawnText), x1, -10.0f, respawnColor, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, (int)(bgOpacity * 255.0f) << 24, packedLight);
        this.font.drawInBatch((Component)Component.literal((String)respawnText), x1, -10.0f, respawnColor, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        float x2 = (float)(-this.font.width(initialText)) / 2.0f;
        this.font.drawInBatch((Component)Component.literal((String)initialText), x2, 0.0f, initialColor, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, (int)(bgOpacity * 255.0f) << 24, packedLight);
        this.font.drawInBatch((Component)Component.literal((String)initialText), x2, 0.0f, initialColor, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    public void render(BlockEntity blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, int n, int n2) {
        this.render((VehicleSpawnerBlockEntity)blockEntity, f, poseStack, multiBufferSource, n, n2);
    }
}

