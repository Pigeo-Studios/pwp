package com.pigeostudios.pwp.warfare.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.MOD)
// Регистрация клавиш мода: карта, меню отряда, пинг, сброс припасов
public class ModKeyBindings {
   public static final KeyMapping DROP_SUPPLY_KEY = new KeyMapping("key.pwpwarfare.drop_supply", KeyConflictContext.IN_GAME, Type.KEYSYM, 88, "key.categories.pwpwarfare");
   public static final KeyMapping OPEN_SQUAD_MENU_KEY = new KeyMapping(
      "key.pwpwarfare.open_squad_menu", KeyConflictContext.IN_GAME, Type.KEYSYM, 75, "key.categories.pwpwarfare"
   );
   public static final KeyMapping SHOW_MAP_KEY = new KeyMapping("key.pwpwarfare.show_map", KeyConflictContext.IN_GAME, Type.KEYSYM, 258, "key.categories.pwpwarfare");
   public static final KeyMapping SHOW_NICKNAMES_KEY = new KeyMapping(
      "key.pwpwarfare.show_nicknames", KeyConflictContext.UNIVERSAL, Type.KEYSYM, 340, "key.categories.pwpwarfare"
   );
   public static final KeyMapping PLACE_PING_KEY = new KeyMapping("key.pwpwarfare.place_ping", KeyConflictContext.IN_GAME, Type.MOUSE, 2, "key.categories.pwpwarfare");

   @SubscribeEvent
   public static void registerKeys(RegisterKeyMappingsEvent event) {
      event.register(DROP_SUPPLY_KEY);
      event.register(OPEN_SQUAD_MENU_KEY);
      event.register(SHOW_MAP_KEY);
      event.register(SHOW_NICKNAMES_KEY);
      event.register(PLACE_PING_KEY);
   }
}
