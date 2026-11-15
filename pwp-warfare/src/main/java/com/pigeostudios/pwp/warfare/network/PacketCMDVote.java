package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет голосования за кандидата в командиры
// Отправляется лидерами отрядов для голосования "за" или "против"
public class PacketCMDVote {
   private final boolean agree;

   public PacketCMDVote(boolean agree) {
      this.agree = agree;
   }

   public static void encode(PacketCMDVote msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.agree);
   }

   public static PacketCMDVote decode(FriendlyByteBuf buf) {
      return new PacketCMDVote(buf.readBoolean());
   }

   // Обрабатывает голос: проверяет, является ли игрок лидером отряда,
   // и записывает голос за или против кандидата в командиры
   public static void handle(PacketCMDVote msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.getTeam() != null) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            String team = player.getTeam().getName().toUpperCase();
            boolean isBlue = team.equals("BLUE");
            if (player.getPersistentData().getBoolean("WARFARE_IsSquadLeader")) {
               if (isBlue) {
                  if (data.blueCmdVoteActive && !player.getScoreboardName().equals(data.blueCmdCandidateName)) {
                     data.blueCmdVotes.put(player.getUUID(), msg.agree);
                  }
               } else if (data.redCmdVoteActive && !player.getScoreboardName().equals(data.redCmdCandidateName)) {
                  data.redCmdVotes.put(player.getUUID(), msg.agree);
               }

               data.setDirty();
               PacketHandler.sendToAllClients(player.serverLevel(), data);
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
