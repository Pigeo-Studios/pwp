/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.logging.LogUtils
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.RegisterCommandsEvent
 *  net.minecraftforge.event.server.ServerStartingEvent
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.fml.ModLoadingContext
 *  net.minecraftforge.fml.common.Mod
 *  net.minecraftforge.fml.config.IConfigSpec
 *  net.minecraftforge.fml.config.ModConfig$Type
 *  net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
 *  net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
 *  net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
 *  org.slf4j.Logger
 */
package com.example.aas;

import com.example.aas.block.ModBlocks;
import com.example.aas.client.ClientConfigRegistry;
import com.example.aas.command.ModCommands;
import com.example.aas.config.AASConfig;
import com.example.aas.entity.ModEntities;
import com.example.aas.item.ModCreativeModeTabs;
import com.example.aas.item.ModItems;
import com.example.aas.menu.ModMenuTypes;
import com.example.aas.network.PacketHandler;
import com.example.aas.sound.ModSounds;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(value="aas")
public class AASMod {
    static private final Logger LOGGER = LogUtils.getLogger();

    public AASMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, (IConfigSpec)AASConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, (IConfigSpec)AASConfig.CLIENT_SPEC);
        DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> ClientConfigRegistry::registerConfigScreen);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModSounds.register(modEventBus);
        MinecraftForge.EVENT_BUS.register((Object)this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> PacketHandler.register());
    }

    private void clientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("AAS Server starting...");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
    }
}

