package com.pigeostudios.pwp.warfare.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreserver.CoreServerApi;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketOpenFactionKitEditor {
   private final String faction;
   private final String kitName;

   public PacketOpenFactionKitEditor(String faction, String kitName) {
      this.faction = faction;
      this.kitName = kitName;
   }

   public static void encode(PacketOpenFactionKitEditor msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.faction);
      buf.writeUtf(msg.kitName);
   }

   public static PacketOpenFactionKitEditor decode(FriendlyByteBuf buf) {
      return new PacketOpenFactionKitEditor(buf.readUtf(), buf.readUtf());
   }

   public static void handle(PacketOpenFactionKitEditor msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               ServerPlayer player = ctx.get().getSender();
               if (player != null && player.isCreative()) {
                  WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                  WarfareWorldData.KitInfo kit = null;

                  if (msg.faction.equalsIgnoreCase(data.blueFaction) && data.blueKits.containsKey(msg.kitName)) {
                     kit = data.blueKits.get(msg.kitName);
                  } else if (msg.faction.equalsIgnoreCase(data.redFaction) && data.redKits.containsKey(msg.kitName)) {
                     kit = data.redKits.get(msg.kitName);
                  } else {
                     kit = loadFromApi(msg.faction, msg.kitName);
                  }

                  if (kit == null) {
                     kit = new WarfareWorldData.KitInfo(msg.kitName);
                  }

                  final WarfareWorldData.KitInfo finalKit = kit;
                  NetworkHooks.openScreen(
                     player,
                     new MenuProvider() {
                        public Component getDisplayName() {
                           return Component.literal("Faction Kit: " + finalKit.name);
                        }

                        public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                           SimpleContainer container = new SimpleContainer(49);

                           for (int i = 0; i < 49; i++) {
                              container.setItem(i, ((ItemStack)finalKit.inventory.get(i)).copy());
                           }

                           return new KitEditorMenu(
                              id,
                              inv,
                              container,
                              msg.faction,
                              finalKit.name,
                              finalKit.isLeaderOnly,
                              finalKit.maxPerTeam,
                              finalKit.maxPerSquad,
                              finalKit.minSquadPlayers,
                              finalKit.resupplyFlags,
                              finalKit.saveNbtFlags,
                              finalKit.slotSkins
                           );
                        }
                     },
                     buf -> {
                        buf.writeUtf(msg.faction);
                        buf.writeUtf(msg.kitName);
                        buf.writeBoolean(finalKit.isLeaderOnly);
                        buf.writeInt(finalKit.maxPerTeam);
                        buf.writeInt(finalKit.maxPerSquad);
                        buf.writeInt(finalKit.minSquadPlayers);

                        for (boolean f : finalKit.resupplyFlags) {
                           buf.writeBoolean(f);
                        }

                         for (boolean f : finalKit.saveNbtFlags) {
                            buf.writeBoolean(f);
                         }

                         Map<Integer, List<String>> skins = finalKit.slotSkins;
                         buf.writeInt(skins.size());
                         for (Map.Entry<Integer, List<String>> e : skins.entrySet()) {
                            buf.writeInt(e.getKey());
                            buf.writeInt(e.getValue().size());
                            for (String s : e.getValue()) buf.writeUtf(s);
                         }
                      }
                  );
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }

   private static WarfareWorldData.KitInfo loadFromApi(String faction, String kitName) {
      try {
         JsonObject response = CoreServerApi.getFactionKit(faction, kitName);
         if (response != null && response.has("data")) {
            JsonObject data = response.getAsJsonObject("data");
            if (data != null && data.has("kitName")) {
               return WarfareWorldData.KitInfo.loadFromJson(data);
            }
         }
      } catch (Exception e) {
      }
      return null;
   }
}
