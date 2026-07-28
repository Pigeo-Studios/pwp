package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketSaveKit {
   private final String team;
   private final String kitName;
   private final String category;
   private final String description;
   private final boolean isLeader;
   private final int maxTeam;
   private final int maxSquad;
   private final int minSquadPlayers;
   private final boolean[] resupplyFlags;
   private final boolean[] nbtFlags;
   private final Map<Integer, List<String>> slotSkins;

   public PacketSaveKit(
      String team, String kitName, String category, String description, boolean isLeader, int maxTeam, int maxSquad, int minSquadPlayers,
      boolean[] resupplyFlags, boolean[] nbtFlags, Map<Integer, List<String>> slotSkins
   ) {
      this.team = team;
      this.kitName = kitName;
      this.category = category;
      this.description = description;
      this.isLeader = isLeader;
      this.maxTeam = maxTeam;
      this.maxSquad = maxSquad;
      this.minSquadPlayers = minSquadPlayers;
      this.resupplyFlags = resupplyFlags;
      this.nbtFlags = nbtFlags;
      this.slotSkins = slotSkins != null ? slotSkins : new HashMap<>();
   }

   public static void encode(PacketSaveKit msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.team);
      buf.writeUtf(msg.kitName);
      buf.writeUtf(msg.category);
      buf.writeUtf(msg.description);
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

   public static PacketSaveKit decode(FriendlyByteBuf buf) {
      String t = buf.readUtf();
      String k = buf.readUtf();
      String cat = buf.readUtf();
      String desc = buf.readUtf();
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

      return new PacketSaveKit(t, k, cat, desc, l, mt, ms, minP, f1, f2, skins);
   }

   public static void handle(PacketSaveKit msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null && player.isCreative() && player.containerMenu instanceof KitEditorMenu menu) {
            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
            WarfareWorldData.KitInfo kit = msg.team.equals("BLUE") ? data.blueKits.get(msg.kitName) : data.redKits.get(msg.kitName);
            if (kit != null) {
               kit.isLeaderOnly = msg.isLeader;
               kit.maxPerTeam = msg.maxTeam;
               kit.maxPerSquad = msg.maxSquad;
               kit.minSquadPlayers = msg.minSquadPlayers;
               kit.category = msg.category;
               kit.description = msg.description;
               kit.resupplyFlags = msg.resupplyFlags;
               kit.saveNbtFlags = msg.nbtFlags;
               kit.slotSkins = msg.slotSkins;

               for (int i = 0; i < 49; i++) {
                  kit.inventory.set(i, menu.kitInventory.getItem(i).copy());
               }

                data.setDirty();
                PacketHandler.sendToAllClients(player.serverLevel(), data);

                // Broadcast updated kit menu to all players on the same team
                for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                   if (p.getTeam() != null && p.getTeam().getName().toUpperCase().equals(msg.team)) {
                      PacketRequestKitMenu.sendKitMenu(p, data);
                   }
                }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
