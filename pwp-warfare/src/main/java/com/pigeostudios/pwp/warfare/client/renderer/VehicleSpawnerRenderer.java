package com.pigeostudios.pwp.warfare.client.renderer;

import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

// Рендер спавнера техники — отображает текстовую информацию
// Показывает время до возрождения и настройки над блоком
public class VehicleSpawnerRenderer implements BlockEntityRenderer<VehicleSpawnerBlockEntity> {
   private final Font font;

   public VehicleSpawnerRenderer(Context context) {
      this.font = context.getFont();
   }

   private String formatTime(long seconds) {
      long m = seconds / 60L;
      long s = seconds % 60L;
      return String.format("%dм %dс", m, s);
   }

   public void render(VehicleSpawnerBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
      if (!(Minecraft.getInstance().player.distanceToSqr(entity.getBlockPos().getCenter()) > 4096.0)) {
         poseStack.pushPose();
         poseStack.translate(0.5, 1.5, 0.5);
         poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
         poseStack.scale(-0.025F, -0.025F, 0.025F);
         long currentTick = entity.getLevel().getGameTime();
         long timeLeft = Math.max(0L, (entity.targetSpawnTick - currentTick) / 20L);
         String respawnText;
         int respawnColor;
         if (entity.hasSpawnedOnce && entity.targetSpawnTick > currentTick) {
            respawnText = "Respawning: " + this.formatTime(timeLeft);
            respawnColor = 16755200;
         } else {
            respawnText = "Respawn Set: " + this.formatTime(entity.respawnTimeSettings);
            respawnColor = 11184810;
         }

         String initialText;
         int initialColor;
         if (!entity.hasSpawnedOnce && entity.targetSpawnTick > currentTick) {
            initialText = "Starting: " + this.formatTime(timeLeft);
            initialColor = 5635925;
         } else {
            initialText = "Initial Set: " + this.formatTime(entity.initialTimeSettings);
            initialColor = 11184810;
         }

         Matrix4f matrix = poseStack.last().pose();
         float bgOpacity = 0.25F;
         float x1 = -this.font.width(respawnText) / 2.0F;
         this.font
            .drawInBatch(
               Component.literal(respawnText),
               x1,
               -10.0F,
               respawnColor,
               false,
               matrix,
               buffer,
               DisplayMode.SEE_THROUGH,
               (int)(bgOpacity * 255.0F) << 24,
               packedLight
            );
         this.font.drawInBatch(Component.literal(respawnText), x1, -10.0F, respawnColor, false, matrix, buffer, DisplayMode.NORMAL, 0, packedLight);
         float x2 = -this.font.width(initialText) / 2.0F;
         this.font
            .drawInBatch(
               Component.literal(initialText),
               x2,
               0.0F,
               initialColor,
               false,
               matrix,
               buffer,
               DisplayMode.SEE_THROUGH,
               (int)(bgOpacity * 255.0F) << 24,
               packedLight
            );
         this.font.drawInBatch(Component.literal(initialText), x2, 0.0F, initialColor, false, matrix, buffer, DisplayMode.NORMAL, 0, packedLight);
         poseStack.popPose();
      }
   }
}
