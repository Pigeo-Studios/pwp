package com.pigeostudios.pwp.warfare.mixin;

import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
// Миксин: изменение чувствительности мыши при прицеливании на турелях
// Уменьшает чувствительность при использовании AGS-30 и M2 Browning
public class MouseSensitivityMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"), remap = false)
   private Object pwpwarfare$modifySensitivity(OptionInstance<?> instance) {
      Object value = instance.get();
      Minecraft mc = Minecraft.getInstance();
      if (instance == mc.options.sensitivity()) {
         Double sensitivity = (Double)value;
         if (mc.player != null) {
            Entity vehicle = mc.player.getVehicle();
            if (vehicle instanceof AGS30Entity ags && ags.isAiming()) {
               return sensitivity * 0.25;
            }

            if (vehicle instanceof M2BrowningEntity m2 && m2.isAiming()) {
               return sensitivity * 0.5;
            }
         }

         return sensitivity;
      } else {
         return value;
      }
   }
}
