package com.pigeostudios.pwp.warfare.mixin;

import frontline.combat.fcp.entity.vehicle.CamoVehicleBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Уничтоженная FCP-машина не выкидывает свой контейнер (36 слотов БК из
// спавнера) в мир: CamoVehicleBase.onRemoved (SRG m_142687_) захардкоженно
// зовёт Containers.dropContents при RemovalReason KILLED/DISCARDED.
// Настройки, отключающей дроп, в FCP и SBW нет (проверено 12.08.2026:
// fcp-common.toml — только multicrew, SBW VehicleConfig — без опций дропа).
// Редирект на вызов dropContents внутри onRemoved: super.onRemoved()
// (SBW/vanilla) отрабатывает как обычно, SBW-контейнер у FCP-машин пуст,
// так что дропа нет в любом случае. require=0: при обновлении FCP тихо
// вернёмся к дропу, краша нет.
@Mixin(CamoVehicleBase.class)
public abstract class FcpVehicleNoInventoryDropMixin {
   @Redirect(
      method = "m_142687_",
      at = @At(value = "INVOKE",
         target = "Lnet/minecraft/world/Containers;m_18998_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/Container;)V"),
      remap = false,
      require = 0
   )
   private static void pwp$noFcpInventoryDrop(Level level, Entity entity, Container container) {
   }
}
