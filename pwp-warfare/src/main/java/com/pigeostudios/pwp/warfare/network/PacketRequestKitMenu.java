package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
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
    public static void sendKitMenu(ServerPlayer player, WarfareWorldData data) {
       if (player.getTeam() == null) return;
       String teamName = player.getTeam().getName().toUpperCase();
       String pName = player.getScoreboardName();
       WarfareWorldData.Squad mySquad = null;

       for (WarfareWorldData.Squad s : data.squads) {
          if (s.members.contains(pName)) { mySquad = s; break; }
       }

        boolean amILeader = mySquad != null && mySquad.leader.equals(pName);
        if (!amILeader) amILeader = player.getPersistentData().getBoolean("WARFARE_IsSquadLeader");
       List<PacketOpenPlayerKitMenu.KitDTO> dtoList = new ArrayList<>();

       Map<String, WarfareWorldData.KitInfo> kits = teamName.equals("BLUE") ? data.blueKits : data.redKits;
        // Счётчик моделей оружия (основных стволов) по команде и отряду — лимит повтора одной модели
        Map<String, Integer> teamModels = new HashMap<>();
        Map<String, Integer> squadModels = new HashMap<>();
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
           if (p != player && p.getTeam() != null && p.getTeam().getName().toUpperCase().equals(teamName)) {
              String cKit = p.getPersistentData().getString("WARFARE_CurrentKit");
              String pKit = p.getPersistentData().getString("WARFARE_PendingKit");
              WarfareWorldData.KitInfo k = kits.get(!pKit.isEmpty() ? pKit : cKit);
              if (k == null) continue;
              String gid = k.primaryGunId();
              if (gid == null) continue;
              teamModels.merge(gid, 1, Integer::sum);
              if (mySquad != null && mySquad.members.contains(p.getScoreboardName())) {
                 squadModels.merge(gid, 1, Integer::sum);
              }
           }
        }
        // Порядок: базовые роли из KIT_NAMES, варианты (X (…)) — сразу после своей роли, остальное — в конец
       List<String> order = new ArrayList<>();
       Set<String> added = new HashSet<>();
       for (String base : WarfareWorldData.KIT_NAMES) {
          if (kits.containsKey(base) && added.add(base)) order.add(base);
          List<String> variants = new ArrayList<>();
          for (String k : kits.keySet()) {
             if (k.startsWith(base + " (")) variants.add(k);
          }
          variants.sort(String::compareTo);
          for (String v : variants) {
             if (added.add(v)) order.add(v);
          }
       }
       for (String k : kits.keySet()) {
          if (added.add(k)) order.add(k);
       }

       for (String kitName : order) {
          WarfareWorldData.KitInfo kit = kits.get(kitName);
          if (kit != null && (!kit.isLeaderOnly || amILeader || player.isCreative()) && (kit.maxPerTeam != 0 || kitName.equals("Unassigned"))) {
             int tCount = 0, sCount = 0;
             for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                if (p != player && p.getTeam() != null && p.getTeam().getName().toUpperCase().equals(teamName)) {
                   String cKit = p.getPersistentData().getString("WARFARE_CurrentKit");
                   String pKit = p.getPersistentData().getString("WARFARE_PendingKit");
                   if (cKit.equals(kitName) || pKit.equals(kitName)) {
                      tCount++;
                      if (mySquad != null && mySquad.members.contains(p.getScoreboardName())) sCount++;
                   }
                }
             }

             boolean available = true;
             String reason = "";
             if (kit.maxPerTeam > 0 && tCount >= kit.maxPerTeam) { available = false; reason = "Team Full (" + tCount + "/" + kit.maxPerTeam + ")"; }
             else if (kit.maxPerSquad > 0 && sCount >= kit.maxPerSquad) { available = false; reason = "Squad Full (" + sCount + "/" + kit.maxPerSquad + ")"; }
             else if (kit.minSquadPlayers > 0 && (mySquad == null || mySquad.members.size() < kit.minSquadPlayers)) { available = false; reason = "Need " + kit.minSquadPlayers + " players in Squad"; }
             else if ("FIRE_SUPPORT".equals(kit.category) && ResupplyHandler.countFireSupportOthersInSquad(player, data) >= 3) {
                available = false;
                reason = "Max 3 Fire Support per Squad";
             }

              // Лимит повтора одной модели оружия на отряд/команду
              String gid = kit.primaryGunId();
              if (gid != null && available) {
                 if (squadModels.getOrDefault(gid, 0) >= WarfareWorldData.KitInfo.MAX_SAME_MODEL_PER_SQUAD) {
                    available = false;
                    reason = "Max 2 same weapon per Squad";
                 } else if (teamModels.getOrDefault(gid, 0) >= WarfareWorldData.KitInfo.MAX_SAME_MODEL_PER_TEAM) {
                    available = false;
                    reason = "Max 4 same weapon per Team";
                 }
              }

             String myCurrentKit = player.getPersistentData().getString("WARFARE_PendingKit");
             if (myCurrentKit.isEmpty()) myCurrentKit = player.getPersistentData().getString("WARFARE_CurrentKit");
             boolean isSelected = myCurrentKit.equals(kitName);
             List<ItemStack> kitPreviewItems = new ArrayList<>(kit.inventory);
             PacketOpenPlayerKitMenu.KitDTO dto = new PacketOpenPlayerKitMenu.KitDTO(kitName, kit.category, kit.description, available, reason, isSelected, kitPreviewItems);
             dto.inTeamCount = tCount;
             dto.maxInTeam = kit.maxPerTeam;
             dto.inSquadCount = sCount;
             dto.maxInSquad = kit.maxPerSquad;
             if (kit.slotSkins != null) {
                for (var e : kit.slotSkins.entrySet()) dto.slotSkins.put(e.getKey(), new ArrayList<>(e.getValue()));
             }
             dtoList.add(dto);
          }
       }

        // Если ни PendingKit, ни CurrentKit не заданы — отметим первый доступный кит
        boolean anySelected = false;
        for (var dto : dtoList) {
            if (dto.isSelected) { anySelected = true; break; }
        }
        if (!anySelected) {
            for (var dto : dtoList) {
                if (dto.available) { dto.isSelected = true; break; }
            }
        }

        // Attach player's saved slot selections for persistent UI state
        Map<String, Integer> restoredSelections = new HashMap<>();
        CompoundTag selTag = player.getPersistentData().getCompound("WARFARE_SlotSelections");
        for (String key : selTag.getAllKeys()) {
            restoredSelections.put(key, selTag.getInt(key));
        }

        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
            new PacketOpenPlayerKitMenu(dtoList, restoredSelections));
    }

    public static void handle(PacketRequestKitMenu msg, Supplier<Context> ctx) {
       ctx.get().enqueueWork(() -> {
          ServerPlayer player = ctx.get().getSender();
          if (player != null && player.getTeam() != null) {
             WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
             sendKitMenu(player, data);
          }
       });
       ctx.get().setPacketHandled(true);
    }
}
