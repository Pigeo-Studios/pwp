/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.screens.MenuScreens
 *  net.minecraft.client.renderer.entity.ThrownItemRenderer
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.EntityRenderersEvent$RegisterLayerDefinitions
 *  net.minecraftforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
 */
package com.example.aas.client;

import com.example.aas.block.ModBlocks;
import com.example.aas.client.gui.KitEditorScreen;
import com.example.aas.client.gui.VehicleSpawnerScreen;
import com.example.aas.client.renderer.AGS30Renderer;
import com.example.aas.client.renderer.M2BrowningRenderer;
import com.example.aas.client.renderer.M2BulletRenderer;
import com.example.aas.client.renderer.SupplyCrateRenderer;
import com.example.aas.client.renderer.VehicleSpawnerRenderer;
import com.example.aas.entity.ModEntities;
import com.example.aas.menu.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="aas", bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)ModEntities.M2_BROWNING.get(), M2BrowningRenderer::new);
        event.registerEntityRenderer((EntityType)ModEntities.M2_BULLET.get(), M2BulletRenderer::new);
        event.registerEntityRenderer((EntityType)ModEntities.AGS_30.get(), AGS30Renderer::new);
        event.registerBlockEntityRenderer((BlockEntityType)ModBlocks.VEHICLE_SPAWNER_BE.get(), VehicleSpawnerRenderer::new);
        event.registerEntityRenderer((EntityType)ModEntities.AGS_30_GRENADE.get(), context -> new ThrownItemRenderer(context, 0.5f, true));
        event.registerEntityRenderer((EntityType)ModEntities.SUPPLY_CRATE.get(), SupplyCrateRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.m_96206_((MenuType)((MenuType)ModMenuTypes.VEHICLE_SPAWNER_MENU.get()), VehicleSpawnerScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)ModMenuTypes.KIT_EDITOR_MENU.get()), KitEditorScreen::new);
        });
    }
}

