package com.pigeostudios.pwp.warfare.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.CoreAPI;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.registries.ForgeRegistries;

public class PacketSaveFactionKit {
   private final String faction;
   private final String kitName;
   private final boolean isLeader;
   private final int maxTeam;
   private final int maxSquad;
   private final int minSquadPlayers;
   private final boolean[] resupplyFlags;
   private final boolean[] nbtFlags;
   private final Map<Integer, List<String>> slotSkins;

   public PacketSaveFactionKit(
      String faction, String kitName, boolean isLeader, int maxTeam, int maxSquad, int minSquadPlayers,
      boolean[] resupplyFlags, boolean[] nbtFlags, Map<Integer, List<String>> slotSkins
   ) {
      this.faction = faction;
      this.kitName = kitName;
      this.isLeader = isLeader;
      this.maxTeam = maxTeam;
      this.maxSquad = maxSquad;
      this.minSquadPlayers = minSquadPlayers;
      this.resupplyFlags = resupplyFlags;
      this.nbtFlags = nbtFlags;
      this.slotSkins = slotSkins != null ? slotSkins : new HashMap<>();
   }

   public static void encode(PacketSaveFactionKit msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.faction);
      buf.writeUtf(msg.kitName);
      buf.writeBoolean(msg.isLeader);
      buf.writeInt(msg.maxTeam);
      buf.writeInt(msg.maxSquad);
      buf.writeInt(msg.minSquadPlayers);

      for (int i = 0; i < 49; i++) {
         buf.writeBoolean(msg.resupplyFlags[i]);
      }

      for (int i = 0; i < 49; i++) {
         buf.writeBoolean(msg.nbtFlags[i]);
      }

      buf.writeInt(msg.slotSkins.size());
      for (Map.Entry<Integer, List<String>> e : msg.slotSkins.entrySet()) {
         buf.writeInt(e.getKey());
         buf.writeInt(e.getValue().size());
         for (String s : e.getValue()) buf.writeUtf(s);
      }
   }

   public static PacketSaveFactionKit decode(FriendlyByteBuf buf) {
      String f = buf.readUtf();
      String k = buf.readUtf();
      boolean l = buf.readBoolean();
      int mt = buf.readInt();
      int ms = buf.readInt();
      int minP = buf.readInt();
      boolean[] f1 = new boolean[49];

      for (int i = 0; i < 49; i++) {
         f1[i] = buf.readBoolean();
      }

      boolean[] f2 = new boolean[49];

      for (int i = 0; i < 49; i++) {
         f2[i] = buf.readBoolean();
      }

      Map<Integer, List<String>> skins = new HashMap<>();
      int skinCount = buf.readInt();
      for (int i = 0; i < skinCount; i++) {
         int slot = buf.readInt();
         int listSize = buf.readInt();
         List<String> ids = new ArrayList<>();
         for (int j = 0; j < listSize; j++) ids.add(buf.readUtf());
         skins.put(slot, ids);
      }

      return new PacketSaveFactionKit(f, k, l, mt, ms, minP, f1, f2, skins);
   }

   public static void handle(PacketSaveFactionKit msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.isCreative() && player.containerMenu instanceof KitEditorMenu menu) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());

            JsonArray itemsArray = new JsonArray();
            for (int i = 0; i < 49; i++) {
               ItemStack stack = menu.kitInventory.getItem(i);
               if (!stack.isEmpty()) {
                  JsonObject itemJson = new JsonObject();
                  itemJson.addProperty("slot", i);
                  itemJson.addProperty("resupply", msg.resupplyFlags[i]);
                  itemJson.addProperty("saveNbt", msg.nbtFlags[i]);

                  JsonObject itemData = new JsonObject();
                  ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
                  itemData.addProperty("id", registryName != null ? registryName.toString() : "minecraft:air");
                  itemData.addProperty("Count", stack.getCount());

                  if (stack.hasTag()) {
                     try {
                        CompoundTag tag = stack.getTag();
                        JsonObject tagJson = nbtToJson(tag);
                        if (tagJson != null) itemData.add("tag", tagJson);
                     } catch (Exception ignored) {}
                  }

                  itemJson.add("item", itemData);
                  itemsArray.add(itemJson);
               }
            }

            JsonObject slotSkinsJson = new JsonObject();
            for (Map.Entry<Integer, List<String>> e : msg.slotSkins.entrySet()) {
               JsonArray list = new JsonArray();
               for (String s : e.getValue()) list.add(s);
               slotSkinsJson.add(String.valueOf(e.getKey()), list);
            }

            JsonObject kitPayload = new JsonObject();
            kitPayload.addProperty("kitName", msg.kitName);
            kitPayload.addProperty("leaderOnly", msg.isLeader);
            kitPayload.addProperty("maxPerTeam", msg.maxTeam);
            kitPayload.addProperty("maxPerSquad", msg.maxSquad);
            kitPayload.addProperty("minSquadPlayers", msg.minSquadPlayers);
            kitPayload.addProperty("items", itemsArray.toString());
            kitPayload.addProperty("slotSkins", slotSkinsJson.toString());

            CoreAPI.saveFactionKit(msg.faction, msg.kitName, kitPayload);

            WarfareWorldData.KitInfo kit = null;
            if (msg.faction.equalsIgnoreCase(data.blueFaction)) {
               kit = data.blueKits.get(msg.kitName);
            } else if (msg.faction.equalsIgnoreCase(data.redFaction)) {
               kit = data.redKits.get(msg.kitName);
            }

            if (kit != null) {
               kit.isLeaderOnly = msg.isLeader;
               kit.maxPerTeam = msg.maxTeam;
               kit.maxPerSquad = msg.maxSquad;
               kit.minSquadPlayers = msg.minSquadPlayers;
               kit.resupplyFlags = msg.resupplyFlags;
               kit.saveNbtFlags = msg.nbtFlags;
               kit.slotSkins = msg.slotSkins;
               for (int i = 0; i < 49; i++) {
                  kit.inventory.set(i, menu.kitInventory.getItem(i).copy());
               }
               data.setDirty();
               PacketHandler.sendToAllClients(player.serverLevel(), data);
            }

            player.displayClientMessage(Component.translatable("gui.pwpwarfare.kit_editor.saved"), true);
         }
      });
      ctx.get().setPacketHandled(true);
   }

   private static JsonObject nbtToJson(CompoundTag tag) {
      if (tag == null || tag.isEmpty()) return null;
      JsonObject json = new JsonObject();
      for (String key : tag.getAllKeys()) {
         var base = tag.get(key);
         if (base instanceof CompoundTag) {
            JsonObject child = nbtToJson((CompoundTag) base);
            if (child != null) json.add(key, child);
         } else if (base instanceof net.minecraft.nbt.StringTag) {
            json.addProperty(key, base.getAsString());
         } else if (base instanceof net.minecraft.nbt.IntTag) {
            json.addProperty(key, ((net.minecraft.nbt.IntTag) base).getAsInt());
         } else if (base instanceof net.minecraft.nbt.ByteTag) {
            json.addProperty(key, ((net.minecraft.nbt.ByteTag) base).getAsByte());
         } else if (base instanceof net.minecraft.nbt.ShortTag) {
            json.addProperty(key, ((net.minecraft.nbt.ShortTag) base).getAsShort());
         } else if (base instanceof net.minecraft.nbt.LongTag) {
            json.addProperty(key, ((net.minecraft.nbt.LongTag) base).getAsLong());
         } else if (base instanceof net.minecraft.nbt.FloatTag) {
            json.addProperty(key, ((net.minecraft.nbt.FloatTag) base).getAsFloat());
         } else if (base instanceof net.minecraft.nbt.DoubleTag) {
            json.addProperty(key, ((net.minecraft.nbt.DoubleTag) base).getAsDouble());
         } else if (base instanceof net.minecraft.nbt.ListTag listTag) {
            JsonArray arr = new JsonArray();
            for (int i = 0; i < listTag.size(); i++) {
               var elem = listTag.get(i);
               if (elem instanceof CompoundTag) {
                  JsonObject child = nbtToJson((CompoundTag) elem);
                  if (child != null) arr.add(child);
               } else {
                  arr.add(elem.getAsString());
               }
            }
            json.add(key, arr);
         }
      }
      return json;
   }
}
