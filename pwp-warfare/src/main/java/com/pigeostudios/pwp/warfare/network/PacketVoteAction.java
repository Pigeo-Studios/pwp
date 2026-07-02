package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет голосования (за/против) при общих голосованиях
// Используется для механик демократического принятия решений
public class PacketVoteAction {
   private final boolean agree;

   public PacketVoteAction(boolean agree) {
      this.agree = agree;
   }

   public static void encode(PacketVoteAction msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.agree);
   }

   public static PacketVoteAction decode(FriendlyByteBuf buf) {
      return new PacketVoteAction(buf.readBoolean());
   }

   // Записывает голос игрока и синхронизирует данные с клиентами
   public static void handle(PacketVoteAction msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.getTeam() != null) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            if (data.voteActive) {
               data.votes.put(player.getUUID(), msg.agree);
               data.setDirty();
               PacketHandler.sendToAllClients(player.serverLevel(), data);
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
