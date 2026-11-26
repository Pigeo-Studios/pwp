package com.pigeostudios.pwp.warfare.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlock;
import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Голограмма спавнера техники (сам блок невидим, как барьер). Тот, кто настраивает (креатив или спавнер в руке),
 * видит рамку блока и оба таймера сквозь стены, как раньше. Своя команда — только «выезд через …», пока таймер
 * идёт, вблизи и без просвечивания. Врагу — ничего (раньше таймеры чужого мейна были видны сквозь стены).
 */
public class VehicleSpawnerRenderer implements BlockEntityRenderer<VehicleSpawnerBlockEntity> {
    private static final AABB BOX = new AABB(0, 0, 0, 1, 1, 1);
    private static final double TEAM_DIST2 = 32.0 * 32.0;
    private final Font font;

    public VehicleSpawnerRenderer(Context context) {
        this.font = context.getFont();
    }

    private Component formatTime(long seconds) {
        return Component.translatable("pwpwarfare.msg.time.m_s", seconds / 60L, seconds % 60L);
    }

    public void render(
            VehicleSpawnerBlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        double dist2 = player.distanceToSqr(entity.getBlockPos().getCenter());
        if (!VehicleSpawnerBlock.seesSpawners(player)) {
            renderTeamTimer(entity, player, dist2, poseStack, buffer, packedLight);
            return;
        }
        LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), BOX, 1F, 0.8F, 0.2F, 1F);
        if (!(dist2 > 4096.0)) {
            poseStack.pushPose();
            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.mulPose(
                    Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(-0.025F, -0.025F, 0.025F);
            long currentTick = entity.getLevel().getGameTime();
            long timeLeft = Math.max(0L, (entity.targetSpawnTick - currentTick) / 20L);
            Component respawnText;
            int respawnColor;
            if (entity.hasSpawnedOnce && entity.targetSpawnTick > currentTick) {
                respawnText = Component.translatable("pwpwarfare.msg.spawner.respawn_in", this.formatTime(timeLeft));
                respawnColor = 16755200;
            } else {
                respawnText = Component.translatable(
                        "pwpwarfare.msg.spawner.respawn_timer", this.formatTime(entity.respawnTimeSettings));
                respawnColor = 11184810;
            }

            Component initialText;
            int initialColor;
            if (!entity.hasSpawnedOnce && entity.targetSpawnTick > currentTick) {
                initialText = Component.translatable("pwpwarfare.msg.spawner.start_in", this.formatTime(timeLeft));
                initialColor = 5635925;
            } else {
                initialText = Component.translatable(
                        "pwpwarfare.msg.spawner.start_timer", this.formatTime(entity.initialTimeSettings));
                initialColor = 11184810;
            }

            Matrix4f matrix = poseStack.last().pose();
            float bgOpacity = 0.25F;
            float x1 = -this.font.width(respawnText) / 2.0F;
            this.font.drawInBatch(
                    respawnText,
                    x1,
                    -10.0F,
                    respawnColor,
                    false,
                    matrix,
                    buffer,
                    DisplayMode.SEE_THROUGH,
                    (int) (bgOpacity * 255.0F) << 24,
                    packedLight);
            this.font.drawInBatch(
                    respawnText, x1, -10.0F, respawnColor, false, matrix, buffer, DisplayMode.NORMAL, 0, packedLight);
            float x2 = -this.font.width(initialText) / 2.0F;
            this.font.drawInBatch(
                    initialText,
                    x2,
                    0.0F,
                    initialColor,
                    false,
                    matrix,
                    buffer,
                    DisplayMode.SEE_THROUGH,
                    (int) (bgOpacity * 255.0F) << 24,
                    packedLight);
            this.font.drawInBatch(
                    initialText, x2, 0.0F, initialColor, false, matrix, buffer, DisplayMode.NORMAL, 0, packedLight);
            poseStack.popPose();
        }
    }

    /** Своей команде: одна строка «выезд через …», пока таймер идёт, ближе 32 блоков, без просвечивания. */
    private void renderTeamTimer(
            VehicleSpawnerBlockEntity entity,
            net.minecraft.world.entity.player.Player player,
            double dist2,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (dist2 > TEAM_DIST2 || player.getTeam() == null) return;
        String team = VehicleSpawnerBlockEntity.getSpawnerTeam(entity);
        if (!team.equalsIgnoreCase(player.getTeam().getName())) return;
        long now = entity.getLevel().getGameTime();
        if (entity.targetSpawnTick <= now) return;
        Component text = Component.translatable(
                entity.hasSpawnedOnce ? "pwpwarfare.msg.spawner.respawn_in" : "pwpwarfare.msg.spawner.start_in",
                this.formatTime((entity.targetSpawnTick - now) / 20L));
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        this.font.drawInBatch(
                text,
                -this.font.width(text) / 2.0F,
                0.0F,
                0xFFAA00,
                false,
                poseStack.last().pose(),
                buffer,
                DisplayMode.NORMAL,
                0x40000000,
                packedLight);
        poseStack.popPose();
    }
}
