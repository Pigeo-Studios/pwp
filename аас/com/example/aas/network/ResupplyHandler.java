/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.fml.ModList
 *  top.theillusivec4.curios.api.CuriosApi
 *  top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler
 *  top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler
 */
package com.example.aas.network;

import com.example.aas.events.GameLogicEvents;
import com.example.aas.item.ModItems;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class ResupplyHandler {
    public static void applyKitToPlayer(ServerPlayer player, AASWorldData.KitInfo kit) {
        Inventory pInv = player.m_150109_();
        pInv.m_6211_();
        ResupplyHandler.clearCurios(player);
        Map<Integer, CompoundTag> savedTags = GameLogicEvents.PERSISTENT_NBT_STORAGE.remove(player.m_20148_());
        for (int i = 0; i < 49; ++i) {
            ItemStack kitStack = (ItemStack)kit.inventory.get(i);
            if (kitStack.m_41619_()) continue;
            ItemStack itemToGive = kitStack.m_41777_();
            if (i < kit.saveNbtFlags.length && kit.saveNbtFlags[i] && savedTags != null && savedTags.containsKey(i)) {
                itemToGive.m_41751_(savedTags.get(i));
            }
            if (i < 41) {
                pInv.m_6836_(i, itemToGive);
                continue;
            }
            boolean equipped = false;
            if (ModList.get().isLoaded("curios")) {
                equipped = ResupplyHandler.tryEquipInCurios(player, itemToGive);
            }
            if (equipped || pInv.m_36054_(itemToGive)) continue;
            player.m_36176_(itemToGive, false);
        }
        player.getPersistentData().m_128359_("AAS_CurrentKit", kit.name);
    }

    public static void clearCurios(ServerPlayer player) {
        if (ModList.get().isLoaded("curios")) {
            try {
                CuriosApi.getCuriosInventory((LivingEntity)player).ifPresent(handler -> {
                    for (ICurioStacksHandler entry : handler.getCurios().values()) {
                        IDynamicStackHandler stacks = entry.getStacks();
                        for (int i = 0; i < stacks.getSlots(); ++i) {
                            stacks.setStackInSlot(i, ItemStack.f_41583_);
                        }
                    }
                });
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private static boolean tryEquipInCurios(ServerPlayer player, ItemStack stack) {
        try {
            return CuriosApi.getCuriosInventory((LivingEntity)player).map(handler -> {
                for (Map.Entry entry : handler.getCurios().entrySet()) {
                    IDynamicStackHandler stacksHandler = ((ICurioStacksHandler)entry.getValue()).getStacks();
                    for (int i = 0; i < stacksHandler.getSlots(); ++i) {
                        if (!stacksHandler.getStackInSlot(i).m_41619_() || !stacksHandler.isItemValid(i, stack)) continue;
                        stacksHandler.setStackInSlot(i, stack.m_41777_());
                        stack.m_41764_(0);
                        return true;
                    }
                }
                return false;
            }).orElse(false);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean resupplyPlayer(ServerPlayer player, AASWorldData.KitInfo kit, boolean isAmmoBagSource) {
        boolean gaveSomething = false;
        Inventory pInv = player.m_150109_();
        ArrayList<ItemStack> processedItems = new ArrayList<ItemStack>();
        for (int i = 0; i < 49; ++i) {
            int deficit;
            ItemStack targetStack;
            if (!kit.resupplyFlags[i] || (targetStack = (ItemStack)kit.inventory.get(i)).m_41619_() || isAmmoBagSource && targetStack.m_41720_() == ModItems.AMMO_BAG.get()) continue;
            boolean alreadyProcessed = false;
            for (ItemStack processed : processedItems) {
                if (!ItemStack.m_150942_((ItemStack)processed, (ItemStack)targetStack)) continue;
                alreadyProcessed = true;
                break;
            }
            if (alreadyProcessed) continue;
            processedItems.add(targetStack);
            int totalKitNeeds = 0;
            for (int k = 0; k < 49; ++k) {
                ItemStack kStack;
                if (!kit.resupplyFlags[k] || !ItemStack.m_150942_((ItemStack)(kStack = (ItemStack)kit.inventory.get(k)), (ItemStack)targetStack)) continue;
                totalKitNeeds += kStack.m_41613_();
            }
            int totalPlayerHas = 0;
            for (int j = 0; j < pInv.m_6643_(); ++j) {
                ItemStack s = pInv.m_8020_(j);
                if (!ItemStack.m_150942_((ItemStack)s, (ItemStack)targetStack)) continue;
                totalPlayerHas += s.m_41613_();
            }
            if (ModList.get().isLoaded("curios")) {
                totalPlayerHas += ResupplyHandler.countInCurios(player, targetStack);
            }
            if ((deficit = totalKitNeeds - totalPlayerHas) <= 0) continue;
            gaveSomething = true;
            while (deficit > 0) {
                int toGive = Math.min(deficit, targetStack.m_41741_());
                ItemStack addStack = targetStack.m_41777_();
                addStack.m_41764_(toGive);
                if (!pInv.m_36054_(addStack)) {
                    player.m_36176_(addStack, false);
                }
                deficit -= toGive;
            }
        }
        return gaveSomething;
    }

    private static int countInCurios(ServerPlayer player, ItemStack target) {
        return CuriosApi.getCuriosInventory((LivingEntity)player).map(handler -> {
            int count = 0;
            for (ICurioStacksHandler entry : handler.getCurios().values()) {
                IDynamicStackHandler stacks = entry.getStacks();
                for (int i = 0; i < stacks.getSlots(); ++i) {
                    ItemStack s = stacks.getStackInSlot(i);
                    if (!ItemStack.m_150942_((ItemStack)s, (ItemStack)target)) continue;
                    count += s.m_41613_();
                }
            }
            return count;
        }).orElse(0);
    }

    public static void tryApplyPendingKit(ServerPlayer player, AASWorldData data) {
        AASWorldData.KitInfo kit;
        String teamName;
        String pending = player.getPersistentData().m_128461_("AAS_PendingKit");
        if (pending.isEmpty()) {
            return;
        }
        String string = teamName = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "";
        if (teamName.isEmpty()) {
            return;
        }
        AASWorldData.KitInfo kitInfo = kit = teamName.equals("BLUE") ? data.blueKits.get(pending) : data.redKits.get(pending);
        if (kit == null) {
            return;
        }
        if (pending.equals("Unassigned")) {
            ResupplyHandler.applyKitToPlayer(player, kit);
            player.getPersistentData().m_128473_("AAS_PendingKit");
            PacketHandler.sendToAllClients(player.m_284548_(), data);
            return;
        }
        int teamCount = 0;
        int squadCount = 0;
        String pName = player.m_6302_();
        AASWorldData.Squad mySquad = null;
        for (AASWorldData.Squad s : data.squads) {
            if (!s.members.contains(pName)) continue;
            mySquad = s;
            break;
        }
        for (ServerPlayer p : player.f_8924_.m_6846_().m_11314_()) {
            String otherKit;
            if (p == player || p.m_5647_() == null || !p.m_5647_().m_5758_().toUpperCase().equals(teamName) || !(otherKit = p.getPersistentData().m_128461_("AAS_CurrentKit")).equals(pending)) continue;
            ++teamCount;
            if (mySquad == null || !mySquad.members.contains(p.m_6302_())) continue;
            ++squadCount;
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
            ResupplyHandler.applyKitToPlayer(player, kit);
        } else {
            AASWorldData.KitInfo unassigned;
            player.m_213846_((Component)Component.m_237113_((String)("Kit " + pending + " is full or blocked! Spawning as Unassigned.")).m_130940_(ChatFormatting.RED));
            AASWorldData.KitInfo kitInfo2 = unassigned = teamName.equals("BLUE") ? data.blueKits.get("Unassigned") : data.redKits.get("Unassigned");
            if (unassigned != null) {
                ResupplyHandler.applyKitToPlayer(player, unassigned);
            }
        }
        player.getPersistentData().m_128473_("AAS_PendingKit");
        PacketHandler.sendToAllClients(player.m_284548_(), data);
    }
}

