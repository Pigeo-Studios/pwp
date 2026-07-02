package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ РіРѕР»РѕСЃРѕРІР°РЅРёСЏ Р·Р° РєР°РЅРґРёРґР°С‚Р° РІ РєРѕРјР°РЅРґРёСЂС‹
// РћС‚РїСЂР°РІР»СЏРµС‚СЃСЏ Р»РёРґРµСЂР°РјРё РѕС‚СЂСЏРґРѕРІ РґР»СЏ РіРѕР»РѕСЃРѕРІР°РЅРёСЏ "Р·Р°" РёР»Рё "РїСЂРѕС‚РёРІ"
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

   // РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ РіРѕР»РѕСЃ: РїСЂРѕРІРµСЂСЏРµС‚, СЏРІР»СЏРµС‚СЃСЏ Р»Рё РёРіСЂРѕРє Р»РёРґРµСЂРѕРј РѕС‚СЂСЏРґР°,
   // Рё Р·Р°РїРёСЃС‹РІР°РµС‚ РіРѕР»РѕСЃ Р·Р° РёР»Рё РїСЂРѕС‚РёРІ РєР°РЅРґРёРґР°С‚Р° РІ РєРѕРјР°РЅРґРёСЂС‹
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
