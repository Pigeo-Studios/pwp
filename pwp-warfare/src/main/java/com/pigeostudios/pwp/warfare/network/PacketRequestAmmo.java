package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.item.AGSAmmoItem;
import com.pigeostudios.pwp.warfare.item.M2AmmoItem;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.registries.ForgeRegistries;

// РџР°РєРµС‚ Р·Р°РїСЂРѕСЃР° Р±РѕРµРїСЂРёРїР°СЃРѕРІ РёР· С…Р°Р±Р° (FOB)
// РџРѕРґРґРµСЂР¶РёРІР°РµС‚ РїРѕРїРѕР»РЅРµРЅРёРµ РєРёС‚Р°, Р±РѕРµРїСЂРёРїР°СЃС‹ РґР»СЏ M2, РђР“РЎ, РјРёРЅРѕРјС‘С‚Р° Рё TOW
public class PacketRequestAmmo {
   private final BlockPos pos;
   private final int type;

   public PacketRequestAmmo(BlockPos pos, int type) {
      this.pos = pos;
      this.type = type;
   }

   public static void encode(PacketRequestAmmo msg, FriendlyByteBuf buf) {
      buf.writeBlockPos(msg.pos);
      buf.writeInt(msg.type);
   }

   public static PacketRequestAmmo decode(FriendlyByteBuf buf) {
      return new PacketRequestAmmo(buf.readBlockPos(), buf.readInt());
   }

   // РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ Р·Р°РїСЂРѕСЃ: РїСЂРѕРІРµСЂСЏРµС‚ РґРёСЃС‚Р°РЅС†РёСЋ, СЃС‚РѕРёРјРѕСЃС‚СЊ, РєСѓР»РґР°СѓРЅ Рё РІС‹РґР°С‘С‚ РїСЂРµРґРјРµС‚С‹
   public static void handle(PacketRequestAmmo msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            if (!(player.distanceToSqr(msg.pos.getX(), msg.pos.getY(), msg.pos.getZ()) > 64.0)) {
               if (player.level().getBlockEntity(msg.pos) instanceof HubBlockEntity hub) {
                  int cost = 0;
                  int cooldownTime = 0;
                  boolean isOnCooldown = false;
                  switch (msg.type) {
                     case 0:
                        String pendingKit = player.getPersistentData().getString("WARFARE_PendingKit");
                        String currentKitName = player.getPersistentData().getString("WARFARE_CurrentKit");
                        boolean hasPending = !pendingKit.isEmpty();
                        String targetKit = hasPending ? pendingKit : currentKitName;
                        if (!targetKit.isEmpty() && !targetKit.equals("Unassigned")) {
                           long lastFobUse = player.getPersistentData().getLong("WARFARE_LastFobResupply");
                           long currentTime = player.level().getGameTime();
                           if (!player.isCreative() && currentTime < lastFobUse + 1200L) {
                              long secondsLeft = (lastFobUse + 1200L - currentTime) / 20L;
                              player.sendSystemMessage(Component.literal("Resupply on cooldown: " + secondsLeft + "s").withStyle(ChatFormatting.RED));
                              return;
                           }

                           cost = (Integer)WarfareConfig.HUB_RESUPPLY_COST.get();
                           if (!player.isCreative() && hub.getMaterials() < cost) {
                              player.sendSystemMessage(Component.literal("Not enough Materials! Need: " + cost).withStyle(ChatFormatting.RED));
                              return;
                           }

                           WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                           if (hasPending) {
                              ResupplyHandler.tryApplyPendingKit(player, data);
                              if (!player.isCreative()) {
                                 hub.consumeMaterials(cost);
                                 player.getPersistentData().putLong("WARFARE_LastFobResupply", currentTime);
                              }

                              player.level().sendBlockUpdated(msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                              player.sendSystemMessage(Component.literal("New Kit Equipped! (-" + cost + " Mats)").withStyle(ChatFormatting.GREEN));
                           } else {
                              String t = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                              WarfareWorldData.KitInfo kit = t.equals("BLUE") ? data.blueKits.get(currentKitName) : data.redKits.get(currentKitName);
                              if (kit != null) {
                                 if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                                    if (!player.isCreative()) {
                                       hub.consumeMaterials(cost);
                                       player.getPersistentData().putLong("WARFARE_LastFobResupply", currentTime);
                                    }

                                    player.level().sendBlockUpdated(msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                                    player.sendSystemMessage(Component.literal("Kit Resupplied! (-" + cost + " Mats)").withStyle(ChatFormatting.GREEN));
                                 } else {
                                    player.sendSystemMessage(Component.literal("Ammo already full!").withStyle(ChatFormatting.YELLOW));
                                 }
                              }
                           }

                           return;
                        }

                        player.sendSystemMessage(Component.literal("No Kit equipped!").withStyle(ChatFormatting.RED));
                        return;
                     case 3:
                        cost = 20;
                        cooldownTime = 1200;
                        if (hub.cooldownMortar > 0) {
                           isOnCooldown = true;
                        }
                        break;
                     case 4:
                        cost = 50;
                        cooldownTime = 2400;
                        if (hub.cooldownTOW > 0) {
                           isOnCooldown = true;
                        }
                  }

                  if (!player.isCreative() && isOnCooldown) {
                     player.sendSystemMessage(Component.literal("Supply on Cooldown!").withStyle(ChatFormatting.RED));
                     return;
                  }

                  if (!player.isCreative() && hub.getMaterials() < cost) {
                     player.sendSystemMessage(Component.literal("Not enough Construction Materials! Need: " + cost).withStyle(ChatFormatting.RED));
                     return;
                  }

                  boolean success = false;
                  if (msg.type == 3) {
                     Item mortarItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
                     if (mortarItem == null || mortarItem == Items.AIR) {
                        mortarItem = Items.ARROW;
                     }

                     ItemStack stack = new ItemStack(mortarItem, 8);
                     if (player.getInventory().add(stack)) {
                        success = true;
                     } else {
                        player.drop(stack, false);
                     }

                     success = true;
                  } else if (msg.type == 4) {
                     Item towItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "medium_anti_ground_missile"));
                     if (towItem == null || towItem == Items.AIR) {
                        towItem = Items.SPECTRAL_ARROW;
                     }

                     ItemStack stack = new ItemStack(towItem, 2);
                     if (player.getInventory().add(stack)) {
                        success = true;
                     } else {
                        player.drop(stack, false);
                     }

                     success = true;
                  }

                  if (success) {
                     if (!player.isCreative()) {
                        hub.consumeMaterials(cost);
                        hub.setCooldown(msg.type, cooldownTime);
                     }

                     player.sendSystemMessage(Component.literal("Resupplied! (-" + cost + " Mats)").withStyle(ChatFormatting.GREEN));
                     player.level().sendBlockUpdated(msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                  }
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
