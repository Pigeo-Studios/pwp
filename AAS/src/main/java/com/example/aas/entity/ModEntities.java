/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EntityType$Builder
 *  net.minecraft.world.entity.MobCategory
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.RegistryObject
 */
package com.example.aas.entity;

import com.example.aas.entity.AGS30Entity;
import com.example.aas.entity.AGS30GrenadeEntity;
import com.example.aas.entity.M2BrowningEntity;
import com.example.aas.entity.M2BulletEntity;
import com.example.aas.entity.SupplyCrateEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    static final public DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create((IForgeRegistry)ForgeRegistries.ENTITY_TYPES, (String)"aas");
    static final public RegistryObject<EntityType<M2BrowningEntity>> M2_BROWNING = ENTITY_TYPES.register("m2_browning", () -> EntityType.Builder.of(M2BrowningEntity::new, (MobCategory)MobCategory.MISC).sized(1.5f, 1.5f).build(new ResourceLocation("aas", "m2_browning").toString()));
    static final public RegistryObject<EntityType<M2BulletEntity>> M2_BULLET = ENTITY_TYPES.register("m2_bullet", () -> EntityType.Builder.of(M2BulletEntity::new, (MobCategory)MobCategory.MISC).sized(0.1f, 0.1f).clientTrackingRange(128).updateInterval(20).setShouldReceiveVelocityUpdates(true).build(new ResourceLocation("aas", "m2_bullet").toString()));
    static final public RegistryObject<EntityType<AGS30Entity>> AGS_30 = ENTITY_TYPES.register("ags_30", () -> EntityType.Builder.of(AGS30Entity::new, (MobCategory)MobCategory.MISC).sized(1.5f, 1.5f).build(new ResourceLocation("aas", "ags_30").toString()));
    static final public RegistryObject<EntityType<AGS30GrenadeEntity>> AGS_30_GRENADE = ENTITY_TYPES.register("ags_30_grenade", () -> EntityType.Builder.of(AGS30GrenadeEntity::new, (MobCategory)MobCategory.MISC).sized(0.1f, 0.1f).clientTrackingRange(512).updateInterval(10).setShouldReceiveVelocityUpdates(true).build(new ResourceLocation("aas", "ags_30_grenade").toString()));
    static final public RegistryObject<EntityType<SupplyCrateEntity>> SUPPLY_CRATE = ENTITY_TYPES.register("supply_crate", () -> EntityType.Builder.of(SupplyCrateEntity::new, (MobCategory)MobCategory.MISC).sized(0.9f, 0.9f).clientTrackingRange(64).updateInterval(10).setShouldReceiveVelocityUpdates(true).build(new ResourceLocation("aas", "supply_crate").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}

