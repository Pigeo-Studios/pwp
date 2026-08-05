package com.pigeostudios.pwp.warfare.mixin;

import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.pigeostudios.pwp.warfare.client.ClientSafetyState;
import com.pigeostudios.pwp.warfare.client.NotificationFeed;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Скрытие счётчика патронов техники SBW (центр, h-65), когда он перекрывает
// ленту уведомлений: в мейн-зоне — стабильно (стрельба заблокирована, боевой
// HUD бесполезен), вне зоны — пока в ленте есть записи (лента имеет приоритет
// над боевым HUD). renderWeaponInfoFirst — статический метод, его вызывают и
// VehicleMainWeaponHudOverlay, и HelicopterHud/LandVehicleHud — один миксин
// покрывает все виды техники. require=0: при обновлении SBW не крашит клиент.
@OnlyIn(Dist.CLIENT)
@Mixin(VehicleMainWeaponHudOverlay.class)
public abstract class SbwVehicleHudMixin {
   @Inject(
      method = "renderWeaponInfoFirst(Lnet/minecraft/client/gui/GuiGraphics;Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;Lnet/minecraft/world/entity/player/Player;Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/client/gui/Font;III)V",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 0
   )
   private static void pwp$blockWeaponHudInMainZone(GuiGraphics gui, VehicleEntity vehicle, Player player, GunData data, Font font, int a, int b, int c, CallbackInfo ci) {
      if (ClientSafetyState.inMainZone || NotificationFeed.hasVisibleEntries()) {
         ci.cancel();
      }
   }
}
