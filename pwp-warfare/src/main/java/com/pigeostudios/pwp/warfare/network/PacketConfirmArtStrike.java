package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ РїРѕРґС‚РІРµСЂР¶РґРµРЅРёСЏ РёР»Рё РѕС‚РєР»РѕРЅРµРЅРёСЏ Р°СЂС‚РёР»Р»РµСЂРёР№СЃРєРѕРіРѕ СѓРґР°СЂР° РєРѕРјР°РЅРґРёСЂРѕРј
// РћС‚РїСЂР°РІР»СЏРµС‚СЃСЏ РєРѕРјР°РЅРґРёСЂРѕРј РІ РѕС‚РІРµС‚ РЅР° Р·Р°РїСЂРѕСЃ Р°СЂС‚РёР»Р»РµСЂРёРё
public class PacketConfirmArtStrike {
   private final boolean accept;

   public PacketConfirmArtStrike(boolean accept) {
      this.accept = accept;
   }

   public static void encode(PacketConfirmArtStrike msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.accept);
   }

   public static PacketConfirmArtStrike decode(FriendlyByteBuf buf) {
      return new PacketConfirmArtStrike(buf.readBoolean());
   }

   // РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ СЂРµС€РµРЅРёРµ РєРѕРјР°РЅРґРёСЂР°: РїСЂРё РїРѕРґС‚РІРµСЂР¶РґРµРЅРёРё СЃРѕР·РґР°С‘С‚ Р°РєС‚РёРІРЅС‹Р№
   // Р°СЂС‚РёР»Р»РµСЂРёР№СЃРєРёР№ СѓРґР°СЂ РІ СѓРєР°Р·Р°РЅРЅРѕР№ РїРѕР·РёС†РёРё Рё РѕРїРѕРІРµС‰Р°РµС‚ РІСЃРµС… РёРіСЂРѕРєРѕРІ
   public static void handle(PacketConfirmArtStrike msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               ServerPlayer player = ctx.get().getSender();
               if (player != null) {
                  ServerLevel level = player.serverLevel();
                  WarfareWorldData data = WarfareWorldData.get(level);
                  String team = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
                  int mySquadId = player.getPersistentData().getInt("WARFARE_SquadID");
                  boolean isSL = player.getPersistentData().getBoolean("WARFARE_IsSquadLeader");
                  int teamCmdId = team.equals("BLUE") ? data.blueCMDId : data.redCMDId;
                  if (mySquadId == teamCmdId && teamCmdId != -1 && isSL) {
                     WarfareWorldData.ArtStrikeRequest request = team.equals("BLUE") ? data.blueArtRequest : data.redArtRequest;
                     if (request != null) {
                        if (msg.accept) {
                           data.activeStrikes.add(new WarfareWorldData.ActiveStrike(request.pos, team));
                           level.getServer()
                              .getPlayerList()
                              .broadcastSystemMessage(
                                 Component.literal("STRATEGIC: Artillery Strike Confirmed by Commander!")
                                    .withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD}),
                                 false
                              );
                        }

                        if (team.equals("BLUE")) {
                           data.blueArtRequest = null;
                        } else {
                           data.redArtRequest = null;
                        }

                        data.activeMarkers.removeIf(m -> m.type.equals("Artillery Request") && m.team.equals(team));
                        data.setDirty();
                        PacketHandler.sendToAllClients(level, data);
                     }
                  }
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }
}
