package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ Р·Р°РїСЂРѕСЃР° РЅР° РїСЂРѕРІРµРґРµРЅРёРµ РіРѕР»РѕСЃРѕРІР°РЅРёСЏ Р·Р° РєРѕРјР°РЅРґРёСЂР°
// РћС‚РїСЂР°РІР»СЏРµС‚СЃСЏ Р»РёРґРµСЂРѕРј РѕС‚СЂСЏРґР° РґР»СЏ РІС‹РґРІРёР¶РµРЅРёСЏ СЃРІРѕРµР№ РєР°РЅРґРёРґР°С‚СѓСЂС‹
public class PacketRequestCMD {
   public static void encode(PacketRequestCMD msg, FriendlyByteBuf buf) {
   }

   public static PacketRequestCMD decode(FriendlyByteBuf buf) {
      return new PacketRequestCMD();
   }

   // Р—Р°РїСѓСЃРєР°РµС‚ РіРѕР»РѕСЃРѕРІР°РЅРёРµ Р·Р° РєРѕРјР°РЅРґРёСЂР°, РµСЃР»Рё РѕРЅРѕ РµС‰С‘ РЅРµ Р°РєС‚РёРІРЅРѕ
   public static void handle(PacketRequestCMD msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.getTeam() != null) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            String pName = player.getScoreboardName();
            String team = player.getTeam().getName().toUpperCase();
            boolean isBlue = team.equals("BLUE");
            boolean alreadyVoting = isBlue ? data.blueCmdVoteActive : data.redCmdVoteActive;
            if (!alreadyVoting) {
               int currentCMD = isBlue ? data.blueCMDId : data.redCMDId;
               if (currentCMD == -1) {
                  if (player.getPersistentData().getBoolean("WARFARE_IsSquadLeader")) {
                     if (isBlue) {
                        data.blueCmdVoteActive = true;
                        data.blueCmdCandidateName = pName;
                        data.blueCmdCandidateId = player.getPersistentData().getInt("WARFARE_SquadID");
                        data.blueCmdVoteTimer = 600;
                        data.blueCmdVotes.clear();
                     } else {
                        data.redCmdVoteActive = true;
                        data.redCmdCandidateName = pName;
                        data.redCmdCandidateId = player.getPersistentData().getInt("WARFARE_SquadID");
                        data.redCmdVoteTimer = 600;
                        data.redCmdVotes.clear();
                     }

                     data.setDirty();
                     PacketHandler.sendToAllClients(player.serverLevel(), data);
                  }
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
