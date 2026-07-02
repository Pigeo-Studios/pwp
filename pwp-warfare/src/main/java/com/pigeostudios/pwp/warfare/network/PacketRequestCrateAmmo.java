package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.item.AGSAmmoItem;
import com.pigeostudios.pwp.warfare.item.M2AmmoItem;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
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

// РџР°РєРµС‚ Р·Р°РїСЂРѕСЃР° Р±РѕРµРїСЂРёРїР°СЃРѕРІ РёР· СЏС‰РёРєР° СЃРЅР°Р±Р¶РµРЅРёСЏ (SupplyCrate)
// РџРѕР·РІРѕР»СЏРµС‚ РїРѕРїРѕР»РЅРёС‚СЊ РєРёС‚ РёР»Рё РІР·СЏС‚СЊ С‚СЏР¶С‘Р»С‹Рµ Р±РѕРµРїСЂРёРїР°СЃС‹
public class PacketRequestCrateAmmo {
   private final int entityId;
   private final int type;

   public PacketRequestCrateAmmo(int entityId, int type) {
      this.entityId = entityId;
      this.type = type;
   }

   public static void encode(PacketRequestCrateAmmo msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.entityId);
      buf.writeInt(msg.type);
   }

   public static PacketRequestCrateAmmo decode(FriendlyByteBuf buf) {
      return new PacketRequestCrateAmmo(buf.readInt(), buf.readInt());
   }

   // РџСЂРѕРІРµСЂСЏРµС‚ РЅР°Р»РёС‡РёРµ СЏС‰РёРєР°, РґРёСЃС‚Р°РЅС†РёСЋ, СЃС‚РѕРёРјРѕСЃС‚СЊ Рё РІС‹РґР°С‘С‚ РїСЂРµРґРјРµС‚С‹
   public static void handle(PacketRequestCrateAmmo msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            if (player.level().getEntity(msg.entityId) instanceof SupplyCrateEntity crate) {
               if (!(player.distanceToSqr(crate) > 64.0)) {
                  int cost = 0;
                  switch (msg.type) {
                     case 0:
                        cost = (Integer)WarfareConfig.HUB_RESUPPLY_COST.get();
                        break;
                     case 1:
                        cost = 20;
                        break;
                     case 2:
                        cost = 15;
                        break;
                     case 3:
                        cost = 20;
                        break;
                     case 4:
                        cost = 50;
                  }

                  if (msg.type == 0) {
                     String kitName = player.getPersistentData().getString("WARFARE_CurrentKit");
                     if (kitName.isEmpty()) {
                        player.sendSystemMessage(Component.literal("No Kit equipped!").withStyle(ChatFormatting.RED));
                     } else {
                        long lastFobUse = player.getPersistentData().getLong("WARFARE_LastFobResupply");
                        long currentTime = player.level().getGameTime();
                        if (!player.isCreative() && currentTime < lastFobUse + 1200L) {
                           player.sendSystemMessage(Component.literal("Kit Resupply is on cooldown!").withStyle(ChatFormatting.RED));
                        } else if (!player.isCreative() && crate.getMaterials() < cost) {
                           player.sendSystemMessage(Component.literal("Not enough Materials in Crate! Need: " + cost).withStyle(ChatFormatting.RED));
                        } else {
                           WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                           String t = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                           WarfareWorldData.KitInfo kit = t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                           if (kit != null) {
                              if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                                 if (!player.isCreative()) {
                                    crate.setMaterials(crate.getMaterials() - cost);
                                    player.getPersistentData().putLong("WARFARE_LastFobResupply", currentTime);
                                 }

                                 player.sendSystemMessage(Component.literal("Kit Resupplied! (-" + cost + " Mats)").withStyle(ChatFormatting.GREEN));
                              } else {
                                 player.sendSystemMessage(Component.literal("Ammo already full!").withStyle(ChatFormatting.YELLOW));
                              }
                           }
                        }
                     }
                  } else if (!player.isCreative() && crate.getMaterials() < cost) {
                     player.sendSystemMessage(Component.literal("Not enough Materials in Crate! Need: " + cost).withStyle(ChatFormatting.RED));
                  } else {
                     boolean success = false;
                     if (msg.type == 1) {
                        ItemStack stack = new ItemStack((ItemLike)ModItems.AGS_AMMO.get());
                        AGSAmmoItem.setAmmo(stack, 30);
                        if (player.getInventory().add(stack)) {
                           success = true;
                        } else {
                           player.drop(stack, false);
                        }

                        success = true;
                     } else if (msg.type == 2) {
                        ItemStack stack = new ItemStack((ItemLike)ModItems.M2_AMMO.get());
                        M2AmmoItem.setAmmo(stack, 200);
                        if (player.getInventory().add(stack)) {
                           success = true;
                        } else {
                           player.drop(stack, false);
                        }

                        success = true;
                     } else if (msg.type == 3) {
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
                           crate.setMaterials(0);
                        }

                        player.sendSystemMessage(Component.literal("Heavy Ammo Resupplied! Crate consumed.").withStyle(ChatFormatting.GREEN));
                     }
                  }
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
