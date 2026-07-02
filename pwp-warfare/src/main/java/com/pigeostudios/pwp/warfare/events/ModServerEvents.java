package com.pigeostudios.pwp.warfare.events;

import com.pigeostudios.pwp.warfare.network.PacketSquadChat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
// Серверные события мода
// Обрабатывает чат и направляет сообщения в систему связи отряда
public class ModServerEvents {
   @SubscribeEvent
   public static void onServerChat(ServerChatEvent event) {
      ServerPlayer player = event.getPlayer();
      String message = event.getMessage().getString();
      if (!message.startsWith("/")) {
         event.setCanceled(true);
         PacketSquadChat.processChat(player, message, 1);
      }
   }
}
