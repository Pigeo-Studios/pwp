package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ СЂР°Р·РјРµС‰РµРЅРёСЏ РјР°СЂРєРµСЂР° РЅР° РєР°СЂС‚Рµ
// РџРѕРґРґРµСЂР¶РёРІР°РµС‚ С‚РёРїС‹: Р°СЂС‚РёР»Р»РµСЂРёР№СЃРєРёР№ Р·Р°РїСЂРѕСЃ, РѕР±С‰РёР№ РјР°СЂРєРµСЂ Рё РґСЂСѓРіРёРµ
public class PacketPlaceMapMarker {
   private final int x;
   private final int z;
   private final String type;

   public PacketPlaceMapMarker(int x, int z, String type) {
      this.x = x;
      this.z = z;
      this.type = type;
   }

   public static void encode(PacketPlaceMapMarker msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.x);
      buf.writeInt(msg.z);
      buf.writeUtf(msg.type);
   }

   public static PacketPlaceMapMarker decode(FriendlyByteBuf buf) {
      return new PacketPlaceMapMarker(buf.readInt(), buf.readInt(), buf.readUtf());
   }

   // РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ СЂР°Р·РјРµС‰РµРЅРёРµ РјР°СЂРєРµСЂР°: РїСЂРѕРІРµСЂСЏРµС‚ РїСЂР°РІР° (РґР»СЏ Р°СЂС‚РёР»Р»РµСЂРёРё),
   // РґРѕР±Р°РІР»СЏРµС‚ РјР°СЂРєРµСЂ РІ РјРёСЂ Рё РѕРїРѕРІРµС‰Р°РµС‚ РєРѕРјР°РЅРґСѓ
   public static void handle(PacketPlaceMapMarker msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.getTeam() != null) {
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            String team = player.getTeam().getName().toUpperCase();
            String pName = player.getScoreboardName();
            if (msg.type.equals("Artillery Request")) {
               if (!player.getPersistentData().getBoolean("WARFARE_IsSquadLeader") && !player.isCreative()) {
                  return;
               }

               WarfareWorldData.ArtStrikeRequest activeReq = team.equals("BLUE") ? data.blueArtRequest : data.redArtRequest;
               if (activeReq != null) {
                  return;
               }

               BlockPos strikePos = new BlockPos(msg.x, 64, msg.z);
               if (team.equals("BLUE")) {
                  data.blueArtRequest = new WarfareWorldData.ArtStrikeRequest(pName, strikePos);
               } else {
                  data.redArtRequest = new WarfareWorldData.ArtStrikeRequest(pName, strikePos);
               }

               data.activeMarkers.add(new WarfareWorldData.MapMarker(strikePos, "Artillery Request", team, level.getGameTime() + 3600L));
               int cmdId = team.equals("BLUE") ? data.blueCMDId : data.redCMDId;

               for (ServerPlayer p : level.players()) {
                  if (p.getTeam() != null && p.getTeam().getName().toUpperCase().equals(team)) {
                     int pSqId = p.getPersistentData().getInt("WARFARE_SquadID");
                     if (p.getPersistentData().getBoolean("WARFARE_IsSquadLeader") || pSqId == cmdId && cmdId != -1) {
                        p.sendSystemMessage(Component.literal("[STRATEGIC] Artillery requested at " + msg.x + ", " + msg.z).withStyle(ChatFormatting.GOLD));
                     }
                  }
               }

               data.setDirty();
               PacketHandler.sendToAllClients(level, data);
            } else {
               data.activeMarkers.add(new WarfareWorldData.MapMarker(new BlockPos(msg.x, 64, msg.z), msg.type, team, level.getGameTime() + 3600L));
               data.setDirty();
               PacketHandler.sendToAllClients(level, data);
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
