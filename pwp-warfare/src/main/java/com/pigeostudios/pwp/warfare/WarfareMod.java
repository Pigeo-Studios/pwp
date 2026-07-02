package com.pigeostudios.pwp.warfare;

import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.client.ClientConfigRegistry;
import com.pigeostudios.pwp.warfare.command.ModCommands;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.ModEntities;
import com.pigeostudios.pwp.warfare.item.ModCreativeModeTabs;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.menu.ModMenuTypes;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// Главный класс мода PWP: Warfare
// Регистрирует все компоненты мода: блоки, предметы, сущности, звуки, меню, конфиг
@Mod("pwpwarfare")
public class WarfareMod {
   private static final Logger LOGGER = LogUtils.getLogger();

   // Конструктор мода - регистрирует все компоненты и конфиги
   public WarfareMod() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      modEventBus.addListener(this::commonSetup);
      modEventBus.addListener(this::clientSetup);
      ModLoadingContext.get().registerConfig(Type.COMMON, WarfareConfig.SPEC);
      ModLoadingContext.get().registerConfig(Type.CLIENT, WarfareConfig.CLIENT_SPEC);
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientConfigRegistry::registerConfigScreen);
      ModItems.register(modEventBus);
      ModBlocks.register(modEventBus);
      ModEntities.register(modEventBus);
      ModCreativeModeTabs.register(modEventBus);
      ModMenuTypes.register(modEventBus);
      ModSounds.register(modEventBus);
      MinecraftForge.EVENT_BUS.register(this);
   }

   // Общая настройка - регистрирует сетевые пакеты
   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(() -> PacketHandler.register());
   }

   // Настройка клиента (пока пусто)
   private void clientSetup(FMLClientSetupEvent event) {
   }

   @SubscribeEvent
   // Вызывается при старте сервера
   public void onServerStarting(ServerStartingEvent event) {
      LOGGER.info("PWP Warfare Server starting...");
   }

   @SubscribeEvent
   // Регистрирует команды мода
   public void onRegisterCommands(RegisterCommandsEvent event) {
      ModCommands.register(event.getDispatcher());
   }
}
