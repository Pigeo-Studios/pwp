package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.cosmetics.CosmeticManager;
import java.util.*;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

// РћР±СЂР°Р±РѕС‚С‡РёРє РїРѕРїРѕР»РЅРµРЅРёСЏ Р·Р°РїР°СЃРѕРІ: РІС‹РґР°С‡Р° РєРёС‚РѕРІ, СЂРµСЃР°РїРїР»Р°Р№ РїСЂРµРґРјРµС‚РѕРІ,
// СЂР°Р±РѕС‚Р° СЃ Curios API Рё РїСЂРёРјРµРЅРµРЅРёРµ РѕС‚Р»РѕР¶РµРЅРЅС‹С… РєРёС‚РѕРІ РїСЂРё СЂРµСЃРїР°РІРЅРµ
public class ResupplyHandler {
    // Р’С‹РґР°С‘С‚ РёРіСЂРѕРєСѓ РїРѕР»РЅС‹Р№ РЅР°Р±РѕСЂ РїСЂРµРґРјРµС‚РѕРІ РёР· СѓРєР°Р·Р°РЅРЅРѕРіРѕ РєРёС‚Р°
   public static void applyKitToPlayer(ServerPlayer player, WarfareWorldData.KitInfo kit) {
      Inventory pInv = player.getInventory();
      pInv.clearContent();
      clearCurios(player);
      Map<Integer, CompoundTag> savedTags = GameLogicEvents.PERSISTENT_NBT_STORAGE.remove(player.getUUID());

      for (int i = 0; i < 49; i++) {
         ItemStack kitStack = (ItemStack)kit.inventory.get(i);
         if (!kitStack.isEmpty()) {
            ItemStack itemToGive = kitStack.copy();
            if (i < kit.saveNbtFlags.length && kit.saveNbtFlags[i] && savedTags != null && savedTags.containsKey(i)) {
               itemToGive.setTag(savedTags.get(i));
            }

            if (ModList.get().isLoaded("pwp_cosmetics")) {
                String currentKit = player.getPersistentData().getString("WARFARE_CurrentKit");
                List<String> allowed = kit.slotSkins != null ? kit.slotSkins.get(i) : null;
                if (allowed != null && !allowed.isEmpty()) {
                    com.pwp.cosmetics.CosmeticManager.SkinData skinData = null;
                    if (allowed.get(0).startsWith("__CAT__")) {
                        String catType = allowed.get(0).substring(7);
                        skinData = CosmeticManager.getEquipment(player.getUUID(), catType, currentKit);
                    } else {
                        for (String skinId : allowed) {
                            skinData = CosmeticManager.getEquipmentBySkinId(player.getUUID(), skinId);
                            if (skinData != null) break;
                        }
                    }
                    if (skinData != null && !skinData.item.isEmpty()) {
                        itemToGive = skinData.item.copy();
                    }
                }
            }

            if (i < 41) {
               pInv.setItem(i, itemToGive);
            } else {
               boolean equipped = false;
               if (ModList.get().isLoaded("curios")) {
                  equipped = tryEquipInCurios(player, itemToGive);
               }

               if (!equipped && !pInv.add(itemToGive)) {
                  player.drop(itemToGive, false);
               }
            }
         }
      }

      player.getPersistentData().putString("WARFARE_CurrentKit", kit.name);
   }

    public static void clearCurios(ServerPlayer player) {
      if (ModList.get().isLoaded("curios")) {
         try {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
               for (ICurioStacksHandler entry : handler.getCurios().values()) {
                  IDynamicStackHandler stacks = entry.getStacks();

                  for (int i = 0; i < stacks.getSlots(); i++) {
                     stacks.setStackInSlot(i, ItemStack.EMPTY);
                  }
               }
            });
         } catch (Exception var2) {
         }
      }
   }

   private static boolean tryEquipInCurios(ServerPlayer player, ItemStack stack) {
      try {
         return CuriosApi.getCuriosInventory(player).map(handler -> {
            for (Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
               IDynamicStackHandler stacksHandler = entry.getValue().getStacks();

               for (int i = 0; i < stacksHandler.getSlots(); i++) {
                  if (stacksHandler.getStackInSlot(i).isEmpty() && stacksHandler.isItemValid(i, stack)) {
                     stacksHandler.setStackInSlot(i, stack.copy());
                     stack.setCount(0);
                     return true;
                  }
               }
            }

            return false;
         }).orElse(false);
      } catch (Exception e) {
         return false;
      }
   }

   // РџРѕРїРѕР»РЅСЏРµС‚ РЅРµРґРѕСЃС‚Р°СЋС‰РёРµ РїСЂРµРґРјРµС‚С‹ СЌРєРёРїРёСЂРѕРІРєРё РїРѕ С„Р»Р°РіР°Рј resupplyFlags
   public static boolean resupplyPlayer(ServerPlayer player, WarfareWorldData.KitInfo kit, boolean isAmmoBagSource) {
      boolean gaveSomething = false;
      Inventory pInv = player.getInventory();
      List<ItemStack> processedItems = new ArrayList<>();

      for (int i = 0; i < 49; i++) {
         if (kit.resupplyFlags[i]) {
            ItemStack targetStack = (ItemStack)kit.inventory.get(i);
            if (!targetStack.isEmpty() && (!isAmmoBagSource || targetStack.getItem() != ModItems.AMMO_BAG.get())) {
               boolean alreadyProcessed = false;

               for (ItemStack processed : processedItems) {
                  if (ItemStack.isSameItemSameTags(processed, targetStack)) {
                     alreadyProcessed = true;
                     break;
                  }
               }

               if (!alreadyProcessed) {
                  processedItems.add(targetStack);
                  int totalKitNeeds = 0;

                  for (int k = 0; k < 49; k++) {
                     if (kit.resupplyFlags[k]) {
                        ItemStack kStack = (ItemStack)kit.inventory.get(k);
                        if (ItemStack.isSameItemSameTags(kStack, targetStack)) {
                           totalKitNeeds += kStack.getCount();
                        }
                     }
                  }

                  int totalPlayerHas = 0;

                  for (int j = 0; j < pInv.getContainerSize(); j++) {
                     ItemStack s = pInv.getItem(j);
                     if (ItemStack.isSameItemSameTags(s, targetStack)) {
                        totalPlayerHas += s.getCount();
                     }
                  }

                  if (ModList.get().isLoaded("curios")) {
                     totalPlayerHas += countInCurios(player, targetStack);
                  }

                  int deficit = totalKitNeeds - totalPlayerHas;
                  if (deficit > 0) {
                     gaveSomething = true;

                     while (deficit > 0) {
                        int toGive = Math.min(deficit, targetStack.getMaxStackSize());
                        ItemStack addStack = targetStack.copy();
                        addStack.setCount(toGive);
                        if (!pInv.add(addStack)) {
                           player.drop(addStack, false);
                        }

                        deficit -= toGive;
                     }
                  }
               }
            }
         }
      }

      return gaveSomething;
   }

   private static int countInCurios(ServerPlayer player, ItemStack target) {
      return CuriosApi.getCuriosInventory(player).map(handler -> {
         int count = 0;

         for (ICurioStacksHandler entry : handler.getCurios().values()) {
            IDynamicStackHandler stacks = entry.getStacks();

            for (int i = 0; i < stacks.getSlots(); i++) {
               ItemStack s = stacks.getStackInSlot(i);
               if (ItemStack.isSameItemSameTags(s, target)) {
                  count += s.getCount();
               }
            }
         }

         return count;
      }).orElse(0);
   }

   // РџСЂРёРјРµРЅСЏРµС‚ РѕС‚Р»РѕР¶РµРЅРЅС‹Р№ РєРёС‚ (WARFARE_PendingKit) РїСЂРё РІРѕР·СЂРѕР¶РґРµРЅРёРё РёРіСЂРѕРєР°
   public static void tryApplyPendingKit(ServerPlayer player, WarfareWorldData data) {
      String pending = player.getPersistentData().getString("WARFARE_PendingKit");
      if (!pending.isEmpty()) {
         String teamName = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
         if (!teamName.isEmpty()) {
            WarfareWorldData.KitInfo kit = teamName.equals("BLUE") ? data.blueKits.get(pending) : data.redKits.get(pending);
            if (kit != null) {
               if (pending.equals("Unassigned")) {
                  applyKitToPlayer(player, kit);
                  player.getPersistentData().remove("WARFARE_PendingKit");
                  PacketHandler.sendToAllClients(player.serverLevel(), data);
               } else {
                  int teamCount = 0;
                  int squadCount = 0;
                  String pName = player.getScoreboardName();
                  WarfareWorldData.Squad mySquad = null;

                  for (WarfareWorldData.Squad s : data.squads) {
                     if (s.members.contains(pName)) {
                        mySquad = s;
                        break;
                     }
                  }

                  for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                     if (p != player && p.getTeam() != null && p.getTeam().getName().toUpperCase().equals(teamName)) {
                        String otherKit = p.getPersistentData().getString("WARFARE_CurrentKit");
                        if (otherKit.equals(pending)) {
                           teamCount++;
                           if (mySquad != null && mySquad.members.contains(p.getScoreboardName())) {
                              squadCount++;
                           }
                        }
                     }
                  }

                  boolean allowed = true;
                  if (kit.maxPerTeam > 0 && teamCount >= kit.maxPerTeam) {
                     allowed = false;
                  }

                  if (kit.maxPerSquad > 0 && squadCount >= kit.maxPerSquad) {
                     allowed = false;
                  }

                  if (kit.minSquadPlayers > 0 && (mySquad == null || mySquad.members.size() < kit.minSquadPlayers)) {
                     allowed = false;
                  }

                  if (kit.isLeaderOnly && (mySquad == null || !mySquad.leader.equals(pName))) {
                     allowed = false;
                  }

                  if (allowed) {
                     applyKitToPlayer(player, kit);
                  } else {
                     player.sendSystemMessage(Component.literal("Kit " + pending + " is full or blocked! Spawning as Unassigned.").withStyle(ChatFormatting.RED));
                     WarfareWorldData.KitInfo unassigned = teamName.equals("BLUE") ? data.blueKits.get("Unassigned") : data.redKits.get("Unassigned");
                     if (unassigned != null) {
                        applyKitToPlayer(player, unassigned);
                     }
                  }

                  player.getPersistentData().remove("WARFARE_PendingKit");
                  PacketHandler.sendToAllClients(player.serverLevel(), data);
               }
            }
         }
      }
   }

   public static void giveWalkieTalkie(ServerPlayer player) {
      if (!WarfareConfig.AUTO_GIVE_WALKIETALKIE.get()) return;
      Item walkieItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("walkietalkie", "netherite_walkietalkie"));
      if (walkieItem == null) return;
      boolean hasWalkie = false;
      for (ItemStack stack : player.getInventory().items) {
         if (stack.getItem() == walkieItem) { hasWalkie = true; break; }
      }
      if (!hasWalkie && player.getOffhandItem().getItem() == walkieItem) {
         hasWalkie = true;
      }
      if (!hasWalkie) {
         ItemStack walkieStack = new ItemStack(walkieItem);
         if (!player.getInventory().add(walkieStack)) {
            player.drop(walkieStack, false);
         }
      }
   }
}
