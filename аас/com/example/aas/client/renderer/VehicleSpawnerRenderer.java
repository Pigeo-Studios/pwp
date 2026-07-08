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
import org.joml.Matrix4f;

public class VehicleSpawnerRenderer
implements BlockEntityRenderer<VehicleSpawnerBlockEntity> {
    private final Font font;

    public VehicleSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.m_173586_();
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
        if (Minecraft.m_91087_().f_91074_.m_20238_(entity.m_58899_().m_252807_()) > 4096.0) {
            return;
        }
        poseStack.m_85836_();
        poseStack.m_85837_(0.5, 1.5, 0.5);
        poseStack.m_252781_(Minecraft.m_91087_().m_91290_().m_253208_());
        poseStack.m_85841_(-0.025f, -0.025f, 0.025f);
        long currentTick = entity.m_58904_().m_46467_();
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
        Matrix4f matrix = poseStack.m_85850_().m_252922_();
        float bgOpacity = 0.25f;
        float x1 = (float)(-this.font.m_92895_(respawnText)) / 2.0f;
        this.font.m_272077_((Component)Component.m_237113_((String)respawnText), x1, -10.0f, respawnColor, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, (int)(bgOpacity * 255.0f) << 24, packedLight);
        this.font.m_272077_((Component)Component.m_237113_((String)respawnText), x1, -10.0f, respawnColor, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        float x2 = (float)(-this.font.m_92895_(initialText)) / 2.0f;
        this.font.m_272077_((Component)Component.m_237113_((String)initialText), x2, 0.0f, initialColor, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, (int)(bgOpacity * 255.0f) << 24, packedLight);
        this.font.m_272077_((Component)Component.m_237113_((String)initialText), x2, 0.0f, initialColor, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.m_85849_();
    }
}

