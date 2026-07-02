package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ Р·Р°РїСЂРѕСЃР° РјРµРЅСЋ РІС‹Р±РѕСЂР° РєРёС‚Р°
// РћС‚РїСЂР°РІР»СЏРµС‚СЃСЏ РєР»РёРµРЅС‚РѕРј РґР»СЏ РїРѕР»СѓС‡РµРЅРёСЏ СЃРїРёСЃРєР° РґРѕСЃС‚СѓРїРЅС‹С… РєРёС‚РѕРІ
public class PacketRequestKitMenu {
   public static void encode(PacketRequestKitMenu msg, FriendlyByteBuf buf) {
   }

   public static PacketRequestKitMenu decode(FriendlyByteBuf buf) {
      return new PacketRequestKitMenu();
   }

   // РЎРѕР±РёСЂР°РµС‚ СЃРїРёСЃРѕРє РґРѕСЃС‚СѓРїРЅС‹С… РєРёС‚РѕРІ СЃ СѓС‡С‘С‚РѕРј Р»РёРјРёС‚РѕРІ Рё РѕС‚РїСЂР°РІР»СЏРµС‚ РєР»РёРµРЅС‚Сѓ
   public static void handle(PacketRequestKitMenu msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.getTeam() != null) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            String teamName = player.getTeam().getName().toUpperCase();
            String pName = player.getScoreboardName();
            WarfareWorldData.Squad mySquad = null;

            for (WarfareWorldData.Squad s : data.squads) {
               if (s.members.contains(pName)) {
                  mySquad = s;
                  break;
               }
            }

            boolean amILeader = mySquad != null && mySquad.leader.equals(pName);
            List<PacketOpenPlayerKitMenu.KitDTO> dtoList = new ArrayList<>();

            for (String kitName : WarfareWorldData.KIT_NAMES) {
               WarfareWorldData.KitInfo kit = teamName.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
               if (kit != null && (!kit.isLeaderOnly || amILeader || player.isCreative()) && (kit.maxPerTeam != 0 || kitName.equals("Unassigned"))) {
                  int tCount = 0;
                  int sCount = 0;

                  for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                     if (p != player && p.getTeam() != null && p.getTeam().getName().toUpperCase().equals(teamName)) {
                        String cKit = p.getPersistentData().getString("WARFARE_CurrentKit");
                        String pKit = p.getPersistentData().getString("WARFARE_PendingKit");
                        if (cKit.equals(kitName) || pKit.equals(kitName)) {
                           tCount++;
                           if (mySquad != null && mySquad.members.contains(p.getScoreboardName())) {
                              sCount++;
                           }
                        }
                     }
                  }

                  boolean available = true;
                  String reason = "";
                  if (kit.maxPerTeam > 0 && tCount >= kit.maxPerTeam) {
                     available = false;
                     reason = "Team Full (" + tCount + "/" + kit.maxPerTeam + ")";
                  } else if (kit.maxPerSquad > 0 && sCount >= kit.maxPerSquad) {
                     available = false;
                     reason = "Squad Full (" + sCount + "/" + kit.maxPerSquad + ")";
                  } else if (kit.minSquadPlayers > 0 && (mySquad == null || mySquad.members.size() < kit.minSquadPlayers)) {
                     available = false;
                     reason = "Need " + kit.minSquadPlayers + " players in Squad";
                  }

                  List<ItemStack> kitPreviewItems = new ArrayList<>(kit.inventory);
                  dtoList.add(new PacketOpenPlayerKitMenu.KitDTO(kitName, available, reason, kitPreviewItems));
               }
            }

            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketOpenPlayerKitMenu(dtoList));
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
