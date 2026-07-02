package com.pigeostudios.pwp.warfare.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Реестр всех сущностей мода
// Регистрация типов сущностей через DeferredRegister Forge
public class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "pwpwarfare");
   public static final RegistryObject<EntityType<M2BrowningEntity>> M2_BROWNING = ENTITY_TYPES.register(
      "m2_browning",
      () -> Builder.<M2BrowningEntity>of(M2BrowningEntity::new, MobCategory.MISC).sized(1.5F, 1.5F).build(new ResourceLocation("pwpwarfare", "m2_browning").toString())
   );
   public static final RegistryObject<EntityType<M2BulletEntity>> M2_BULLET = ENTITY_TYPES.register(
      "m2_bullet",
      () -> Builder.<M2BulletEntity>of(M2BulletEntity::new, MobCategory.MISC)
         .sized(0.1F, 0.1F)
         .clientTrackingRange(128)
         .updateInterval(20)
         .setShouldReceiveVelocityUpdates(true)
         .build(new ResourceLocation("pwpwarfare", "m2_bullet").toString())
   );
   public static final RegistryObject<EntityType<AGS30Entity>> AGS_30 = ENTITY_TYPES.register(
      "ags_30", () -> Builder.<AGS30Entity>of(AGS30Entity::new, MobCategory.MISC).sized(1.5F, 1.5F).build(new ResourceLocation("pwpwarfare", "ags_30").toString())
   );
   public static final RegistryObject<EntityType<AGS30GrenadeEntity>> AGS_30_GRENADE = ENTITY_TYPES.register(
      "ags_30_grenade",
      () -> Builder.<AGS30GrenadeEntity>of(AGS30GrenadeEntity::new, MobCategory.MISC)
         .sized(0.1F, 0.1F)
         .clientTrackingRange(512)
         .updateInterval(10)
         .setShouldReceiveVelocityUpdates(true)
         .build(new ResourceLocation("pwpwarfare", "ags_30_grenade").toString())
   );
   public static final RegistryObject<EntityType<SupplyCrateEntity>> SUPPLY_CRATE = ENTITY_TYPES.register(
      "supply_crate",
      () -> Builder.<SupplyCrateEntity>of(SupplyCrateEntity::new, MobCategory.MISC)
         .sized(0.9F, 0.9F)
         .clientTrackingRange(64)
         .updateInterval(10)
         .setShouldReceiveVelocityUpdates(true)
         .build(new ResourceLocation("pwpwarfare", "supply_crate").toString())
   );

   // Регистрация всех типов сущностей в шине Forge
   public static void register(IEventBus eventBus) {
      ENTITY_TYPES.register(eventBus);
   }
}
