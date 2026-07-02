package com.pigeostudios.pwp.warfare.client;

import net.minecraft.client.Minecraft;

// Обработчик отдачи оружия на клиенте
// Плавно возвращает прицел в исходное положение после выстрела
public class RecoilHandler {
   private static float pendingRecovery = 0.0F;

   public static void addRecoil(float pitch) {
      if (!(pitch <= 0.0F)) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.turn(0.0, -pitch);
         }

         pendingRecovery += pitch;
      }
   }

   public static void clientTick() {
      if (pendingRecovery > 0.01F) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return;
         }

         float recoveryStep = pendingRecovery * 0.2F;
         if (recoveryStep < 0.05F) {
            recoveryStep = 0.05F;
         }

         if (recoveryStep > pendingRecovery) {
            recoveryStep = pendingRecovery;
         }

         mc.player.turn(0.0, recoveryStep);
         pendingRecovery -= recoveryStep;
      } else {
         pendingRecovery = 0.0F;
      }
   }
}
