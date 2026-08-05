package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет чата: общий (ALL), командный (TEAM) и отрядный (SQUAD)
// Позволяет игрокам общаться в различных каналах
public class PacketSquadChat {
   private final String message;
   private final int mode;

   public PacketSquadChat(String message, int mode) {
      this.message = message;
      this.mode = mode;
   }

   public static void encode(PacketSquadChat msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.message);
      buf.writeInt(msg.mode);
   }

   public static PacketSquadChat decode(FriendlyByteBuf buf) {
      return new PacketSquadChat(buf.readUtf(), buf.readInt());
   }

   public static void handle(PacketSquadChat msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer sender = ctx.get().getSender();
         if (sender != null) {
            processChat(sender, msg.message, msg.mode);
         }
      });
      ctx.get().setPacketHandled(true);
   }

   // Отправляет сообщение в соответствующий канал: mode 0-ALL, 1-TEAM, 2-SQUAD
   public static void processChat(ServerPlayer sender, String message, int mode) {
      String senderName = sender.getScoreboardName();
      String senderTeam = sender.getTeam() != null ? sender.getTeam().getName() : "NEUTRAL";
      if (mode == 0) {
          MutableComponent formattedMessage = Component.literal("[ВСЕ] ")
            .withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal(senderName + ": ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(message).withStyle(ChatFormatting.LIGHT_PURPLE));
         sender.server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
      } else if (mode == 1) {
         if (senderTeam.equals("NEUTRAL")) {
             sender.sendSystemMessage(Component.literal("Вы не в команде!").withStyle(ChatFormatting.RED));
            return;
         }

          MutableComponent formattedMessage = Component.literal("[КОМАНДА] ")
            .withStyle(ChatFormatting.BLUE)
            .append(Component.literal(senderName + ": ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(message).withStyle(ChatFormatting.BLUE));

         for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
            if (p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(senderTeam)) {
               p.sendSystemMessage(formattedMessage);
            }
         }
      } else if (mode == 2) {
         WarfareWorldData data = WarfareWorldData.get(sender.serverLevel().getServer().overworld());
         WarfareWorldData.Squad playerSquad = null;

         for (WarfareWorldData.Squad s : data.squads) {
            if (s.members.contains(senderName)) {
               playerSquad = s;
               break;
            }
         }

         if (playerSquad == null) {
             sender.sendSystemMessage(Component.literal("Вы не в отряде!").withStyle(ChatFormatting.RED));
            return;
         }

          MutableComponent formattedMessage = Component.literal("[ОТРЯД] ")
            .withStyle(ChatFormatting.GREEN)
            .append(Component.literal(senderName + ": ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(message).withStyle(ChatFormatting.GREEN));

         for (String memberName : playerSquad.members) {
            ServerPlayer member = sender.server.getPlayerList().getPlayerByName(memberName);
            if (member != null) {
               member.sendSystemMessage(formattedMessage);
            }
         }
      }
   }
}
