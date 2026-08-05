package com.pigeostudios.pwp.warfare.mixin;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.pigeostudios.pwp.warfare.events.MainZoneFireGuard;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Глушение звука выстрела техники SBW в мейн-зоне. SbwFireMixin отменяет сам
// выстрел (нет снаряда и расхода патрона), но vehicleShoot всё равно вызывает
// playShootSound3p — и игрок слышал выстрел без выстрела. Этот миксин отменяет
// воспроизведение звука: единая точка (все перегрузки playShootSound3p втекают
// в трёхаргументную), подклассы её не переопределяют. require=0: при обновлении
// SBW миксин тихо не применится — страховка (EntityJoinLevelEvent) останется.
@Mixin(VehicleEntity.class)
public abstract class SbwVehicleEffectsMixin {
   @Inject(
      method = "playShootSound3p(Lnet/minecraft/world/entity/LivingEntity;Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/phys/Vec3;)V",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 0
   )
   private void pwp$blockShootSoundInMainZone(LivingEntity living, GunData data, Vec3 pos, CallbackInfo ci) {
      if (MainZoneFireGuard.isFireBlocked(living)) {
         ci.cancel();
      }
   }
}
