package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
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

// Пакет открытия редактора набора (кита) для администратора
// Отправляется при редактировании кита через консоль/интерфейс
public class PacketOpenKitEditor {
   private final String team;
   private final String kitName;

   public PacketOpenKitEditor(String team, String kitName) {
      this.team = team;
      this.kitName = kitName;
   }

   public static void encode(PacketOpenKitEditor msg, FriendlyByteBuf buf) {
      buf.writeUtf(msg.team);
      buf.writeUtf(msg.kitName);
   }

   public static PacketOpenKitEditor decode(FriendlyByteBuf buf) {
      return new PacketOpenKitEditor(buf.readUtf(), buf.readUtf());
   }

   // Открывает GUI редактора кита для указанного набора экипировки
   public static void handle(PacketOpenKitEditor msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               ServerPlayer player = ctx.get().getSender();
               if (player != null && player.isCreative()) {
                  WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                  final WarfareWorldData.KitInfo kit = msg.team.equals("BLUE") ? data.blueKits.get(msg.kitName) : data.redKits.get(msg.kitName);
                  if (kit != null) {
                     NetworkHooks.openScreen(
                        player,
                        new MenuProvider() {
                           public Component getDisplayName() {
                              return Component.literal("Edit Kit: " + kit.name);
                           }

                           public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                              SimpleContainer container = new SimpleContainer(49);

                              for (int i = 0; i < 49; i++) {
                                 container.setItem(i, ((ItemStack)kit.inventory.get(i)).copy());
                              }

                               return new KitEditorMenu(
                                  id,
                                  inv,
                                  container,
                                  msg.team,  // faction
                                  msg.team,  // team
                                  kit.name,
                                 kit.category,
                                 kit.description != null ? kit.description : "",
                                 kit.isLeaderOnly,
                                 kit.maxPerTeam,
                                 kit.maxPerSquad,
                                 kit.minSquadPlayers,
                                 kit.resupplyFlags,
                                 kit.saveNbtFlags,
                                 kit.slotSkins
                              );
                           }
                        },
                         buf -> {
                             buf.writeUtf(msg.team);   // faction
                             buf.writeUtf(msg.team);   // team
                             buf.writeUtf(msg.kitName);
                            buf.writeUtf(kit.category != null ? kit.category : "INFANTRY");
                            buf.writeUtf(kit.description != null ? kit.description : "");
                            buf.writeBoolean(kit.isLeaderOnly);
                           buf.writeInt(kit.maxPerTeam);
                           buf.writeInt(kit.maxPerSquad);
                           buf.writeInt(kit.minSquadPlayers);

                           for (boolean f : kit.resupplyFlags) {
                              buf.writeBoolean(f);
                           }

                            for (boolean f : kit.saveNbtFlags) {
                               buf.writeBoolean(f);
                            }

                            Map<Integer, List<String>> skins = kit.slotSkins;
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
            }
         );
      ctx.get().setPacketHandled(true);
   }
}
