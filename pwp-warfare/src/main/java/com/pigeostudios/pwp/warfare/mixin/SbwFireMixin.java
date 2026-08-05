package com.pigeostudios.pwp.warfare.mixin;

import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.pigeostudios.pwp.warfare.events.MainZoneFireGuard;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Предохранитель мейн-зоны для Superb Warfare. Все пути стрельбы SBW
// (техника через vehicleShoot -> лямбды -> GunData.shoot, ручное оружие,
// миномёт) втекают в единый метод GunItem.shoot(ShootParameters) — отмена
// в HEAD блокирует выстрел целиком: без снаряда, без расхода патрона,
// без звука. require=0: при обновлении SBW миксин тихо не применится,
// страховочная сеть (EntityJoinLevelEvent в MainZoneFireGuard) продолжит
// блокировать снаряды.
@Mixin(GunItem.class)
public abstract class SbwFireMixin {
   @Inject(
      method = "shoot(Lcom/atsuishio/superbwarfare/data/gun/ShootParameters;)V",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 0
   )
   private void pwp$blockFireInMainZone(ShootParameters parameters, CallbackInfo ci) {
      if (MainZoneFireGuard.isFireBlocked(parameters.shooter)) {
         ci.cancel();
         // Показываем игроку плашку «В мейн-зоне стрельба запрещена» (анти-спам на сервере)
         if (parameters.shooter instanceof ServerPlayer sp) {
            MainZoneFireGuard.notifyBlocked(sp);
         }
      }
   }
}
