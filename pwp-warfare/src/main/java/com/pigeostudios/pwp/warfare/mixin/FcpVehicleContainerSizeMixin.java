package com.pigeostudios.pwp.warfare.mixin;

import frontline.combat.fcp.entity.vehicle.CamoVehicleBase;
import frontline.combat.fcp.entity.vehicle.VehicleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Расширение контейнера техники FCP с 9 до 36 слотов.
// Все FCP-машины (~80 сущностей) наследуют CamoVehicleBase, а контейнер
// создаётся лениво в единой точке getVehicleInventory() через приватный
// vehicleInventorySlots(): для GRID-стиля это округление вверх до кратного 9
// от INVENTORY_SIZE (жёстко 9 в конструкторе каждого подкласса). Инцидент
// 11.08.2026: генератор пулов писал 33 слота БК -> IndexOutOfBounds краш
// матч-сервера. Редирект на вызов vehicleInventorySlots() даёт всем
// машинам 36 слотов (2 батареи + 34 слота БК) без правки 80 классов.
// NONE/BULK-стили и нулевые размеры не трогаем — поведение оригинала.
// require=0: при обновлении FCP тихо вернёмся к 9 слотам, краша нет —
// страховка insertItemIntoSlot в VehicleSpawnerBlockEntity молча пропустит
// лишние слоты.
@Mixin(CamoVehicleBase.class)
public abstract class FcpVehicleContainerSizeMixin {
   @Redirect(
      method = "getVehicleInventory",
      at = @At(value = "INVOKE", target = "Lfrontline/combat/fcp/entity/vehicle/CamoVehicleBase;vehicleInventorySlots()I"),
      remap = false,
      require = 0
   )
   private int pwp$expandedSlotCount(CamoVehicleBase self) {
      VehicleInventory.InventoryStyle style = self.inventoryStyle();
      if (style == VehicleInventory.InventoryStyle.NONE) {
         return 0;
      }
      int size = Math.max(0, self.inventorySize());
      if (size == 0) {
         return 0;
      }
      if (style == VehicleInventory.InventoryStyle.BULK) {
         return Math.max(1, self.bulkChannels()) * size;
      }
      // GRID: оригинальное округление до кратного 9, но минимум 36 слотов
      int grid = ((size + 8) / 9) * 9;
      return Math.max(grid, 36);
   }
}
