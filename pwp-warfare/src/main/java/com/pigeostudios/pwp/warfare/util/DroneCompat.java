package com.pigeostudios.pwp.warfare.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

// Обёртки над публичным API uncomplicated-fpv / SuperbWarfare. Прямых
// ссылок на их классы в игровом коде нет — только здесь, и все вызовы
// защищены instanceof: если мод не загружен — ничего не падает, дрон
// просто спавнится без лоаута.
public final class DroneCompat {
   private DroneCompat() {
   }

   /** Зарядить дрон боеприпасом через штатный UFPV-механизм (fake-player). */
   public static void quickLoadAttachment(Entity drone, ItemStack stack) {
      if (drone instanceof ru.lavafrai.uncomplicatedfpv.entity.AddonDroneEntity addon) {
         addon.quickLoadAttachment(stack);
      }
   }

   /** Полная энергия при спавне (SBW-совместимые дроны). */
   public static void setEnergyFull(Entity drone) {
      if (drone instanceof com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity vehicle) {
         ru.lavafrai.uncomplicatedfpv.compat.SbwVehicleCompat.setEnergy(vehicle, vehicle.getMaxEnergy());
      }
   }

   /** Дрон под активным управлением (монитор привязан). */
   public static boolean isControlled(Entity drone) {
      if (drone instanceof com.atsuishio.superbwarfare.entity.vehicle.DroneEntity de) {
         String c = de.getEntityData().get(com.atsuishio.superbwarfare.entity.vehicle.DroneEntity.CONTROLLER);
         return c != null && !c.isEmpty() && !c.equals("undefined") && !c.equals("none");
      }
      return false;
   }

   /** Принудительный обрыв связи: монитор дисконнектится, дрон в fail-safe. */
   public static void forceSignalLoss(Entity drone) {
      if (drone instanceof ru.lavafrai.uncomplicatedfpv.entity.AddonDroneEntity addon) {
         addon.handleSignalLoss(null);
      }
   }

   /**
    * Мавик полностью заряжен? (ammo >= maxAmmo из UFPV-синчера).
    * Если заряд неизвестен (мод не загружен / accessor пустой) — false,
    * чтобы не блокировать перезарядку ложным отказом.
    */
   public static boolean isFullyLoaded(Entity drone) {
      if (drone instanceof ru.lavafrai.uncomplicatedfpv.entity.AddonDroneEntity addon) {
         try {
            var data = addon.getEntityData();
            Integer ammo = data.get(ru.lavafrai.uncomplicatedfpv.entity.AddonDroneEntity.getAmmoAccessor());
            Integer max = data.get(ru.lavafrai.uncomplicatedfpv.entity.AddonDroneEntity.getMaxAmmoAccessor());
            if (ammo == null || max == null || max <= 0) return false;
            return ammo >= max;
         } catch (Throwable t) {
            return false;
         }
      }
      return false;
   }
}
