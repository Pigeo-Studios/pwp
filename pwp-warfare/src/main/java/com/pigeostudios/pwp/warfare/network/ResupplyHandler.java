package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

// Обработчик пополнения запасов: выдача китов, ресапплай предметов,
// работа с Curios API и применение отложенных китов при респавне
public class ResupplyHandler {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(ResupplyHandler.class);
    // Выдаёт игроку полный набор предметов из указанного кита
    public static void applyKitToPlayer(ServerPlayer player, WarfareWorldData.KitInfo kit) {
        Inventory pInv = player.getInventory();
        pInv.clearContent();
        clearCurios(player);
        Map<Integer, CompoundTag> savedTags = GameLogicEvents.PERSISTENT_NBT_STORAGE.remove(player.getUUID());

        for (int i = 0; i < 49; i++) {
            ItemStack kitStack = (ItemStack) kit.inventory.get(i);
            if (kitStack.isEmpty()) continue;
            // Альтернативные слоты (__ALT__ в slotSkins) — только выбор в меню деплоя, не выдаются
            if (isAltSlot(kit, i)) continue;
            {
                ItemStack itemToGive = kitStack.copy();
                if (i < kit.saveNbtFlags.length
                        && kit.saveNbtFlags[i]
                        && savedTags != null
                        && savedTags.containsKey(i)) {
                    itemToGive.setTag(savedTags.get(i));
                }

                if (ModList.get().isLoaded("pwp_cosmetics")) {
                    String currentKit = WarfareData.of(player).getString(WarfareKeys.CURRENT_KIT);
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
                            if (kitStack.hasTag()) {
                                boolean sameItem = kitStack.getItem() == skinData.item.getItem();
                                boolean cosmeticHasTag = itemToGive.hasTag();
                                if (!sameItem || cosmeticHasTag) {
                                    net.minecraft.nbt.CompoundTag merged = new net.minecraft.nbt.CompoundTag();
                                    for (String key : kitStack.getTag().getAllKeys()) {
                                        merged.put(
                                                key, kitStack.getTag().get(key).copy());
                                    }
                                    if (cosmeticHasTag) {
                                        for (String key : itemToGive.getTag().getAllKeys()) {
                                            merged.put(
                                                    key,
                                                    itemToGive.getTag().get(key).copy());
                                        }
                                    }
                                    itemToGive.setTag(merged);
                                }
                            }
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

        WarfareData.of(player).putString(WarfareKeys.CURRENT_KIT, kit.name);
        // Синхронизируем применённый кит клиенту (ClientData.myCurrentKit)
        com.pigeostudios.pwp.warfare.events.KitUtil.syncMyKit(player);
        // Apply slot selections (alternatives chosen in deploy screen)
        applySlotSelections(player, kit);

        PacketHandler.broadcastPlayerSkin(player);
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
            } catch (Exception ex) {
                com.pwp.api.util.LogOnce.warn(
                        PWP_LOG, "ResupplyHandler:117", "ResupplyHandler.clearCurios: исключение", ex);
            }
        }
    }

    private static boolean tryEquipInCurios(ServerPlayer player, ItemStack stack) {
        try {
            return CuriosApi.getCuriosInventory(player)
                    .map(handler -> {
                        for (Entry<String, ICurioStacksHandler> entry :
                                handler.getCurios().entrySet()) {
                            IDynamicStackHandler stacksHandler =
                                    entry.getValue().getStacks();

                            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                                if (stacksHandler.getStackInSlot(i).isEmpty() && stacksHandler.isItemValid(i, stack)) {
                                    stacksHandler.setStackInSlot(i, stack.copy());
                                    stack.setCount(0);
                                    return true;
                                }
                            }
                        }

                        return false;
                    })
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    // Пополняет недостающие предметы экипировки по флагам resupplyFlags
    public static boolean resupplyPlayer(ServerPlayer player, WarfareWorldData.KitInfo kit, boolean isAmmoBagSource) {
        boolean gaveSomething = false;
        Inventory pInv = player.getInventory();
        List<ItemStack> processedItems = new ArrayList<>();

        for (int i = 0; i < 49; i++) {
            if (kit.resupplyFlags[i]) {
                ItemStack targetStack = (ItemStack) kit.inventory.get(i);
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
                                ItemStack kStack = (ItemStack) kit.inventory.get(k);
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
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> {
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
                })
                .orElse(0);
    }

    // Применяет отложенный кит (WARFARE_PendingKit) при возрождении игрока
    private static void applySlotSelections(ServerPlayer player, WarfareWorldData.KitInfo kit) {
        CompoundTag selTag = WarfareData.of(player).getCompound(WarfareKeys.SLOT_SELECTIONS);
        if (selTag.isEmpty() || kit.slotSkins == null || kit.slotSkins.isEmpty()) return;

        // Slot index for each target label
        Map<String, Integer> targetSlots = Map.of("PRIMARY", 0, "SECONDARY", 1, "THROWABLE", 2, "SPECIAL", 3);
        String prefix = "__ALT__";

        for (String label : targetSlots.keySet()) {
            if (!selTag.contains(label)) continue;
            int desiredIdx = selTag.getInt(label);
            if (desiredIdx <= 0) continue;

            // Find the desiredIdx-th __ALT__label slot in slotSkins
            // Индекс из меню деплоя: 1 = первая альтернатива, 2 = вторая (0 = дефолт, пропускается выше)
            int found = -1, count = 0;
            for (var e : kit.slotSkins.entrySet()) {
                for (String skinVal : e.getValue()) {
                    if (skinVal.equals(prefix + label)) {
                        if (count == desiredIdx - 1) {
                            found = e.getKey();
                            break;
                        }
                        count++;
                    }
                }
                if (found >= 0) break;
            }

            if (found >= 0 && found < 49) {
                int tgt = targetSlots.get(label);
                ItemStack altStack = kit.inventory.get(found);
                if (!altStack.isEmpty()) {
                    player.getInventory().setItem(tgt, altStack.copy());
                }
            }
        }
    }

    // Альтернативный слот кита (метка __ALT__ в slotSkins) — только для выбора в меню деплоя
    private static boolean isAltSlot(WarfareWorldData.KitInfo kit, int slot) {
        if (kit.slotSkins == null) return false;
        List<String> meta = kit.slotSkins.get(slot);
        if (meta == null) return false;
        for (String s : meta) {
            if (s.startsWith("__ALT__")) return true;
        }
        return false;
    }

    // Считает в отряде игрока других членов с китом категории FIRE_SUPPORT (текущим или отложенным).
    // Squad-правило: не больше 3 огневой поддержки на отряд (сам игрок при выборе FS-кита занимает слот).
    public static int countFireSupportOthersInSquad(ServerPlayer player, WarfareWorldData data) {
        String pName = player.getScoreboardName();
        String teamName = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
        if (teamName.isEmpty()) return 0;
        Map<String, WarfareWorldData.KitInfo> kits = teamName.equals("BLUE") ? data.blueKits : data.redKits;
        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
            if (s.members.contains(pName)) {
                int count = 0;
                for (String member : s.members) {
                    if (member.equals(pName)) continue;
                    ServerPlayer p = player.server.getPlayerList().getPlayerByName(member);
                    if (p == null) continue;
                    String pen = WarfareData.of(p).getString(WarfareKeys.PENDING_KIT);
                    String cur = WarfareData.of(p).getString(WarfareKeys.CURRENT_KIT);
                    WarfareWorldData.KitInfo k = kits.get(!pen.isEmpty() ? pen : cur);
                    if (k != null && "FIRE_SUPPORT".equals(k.category)) count++;
                }
                return count;
            }
        }
        return 0;
    }

    public static void tryApplyPendingKit(ServerPlayer player, WarfareWorldData data) {
        String pending = WarfareData.of(player).getString(WarfareKeys.PENDING_KIT);
        if (!pending.isEmpty()) {
            String teamName =
                    player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
            if (!teamName.isEmpty()) {
                WarfareWorldData.KitInfo kit =
                        teamName.equals("BLUE") ? data.blueKits.get(pending) : data.redKits.get(pending);
                if (kit != null) {
                    if (pending.equals("Unassigned")) {
                        applyKitToPlayer(player, kit);
                        WarfareData.of(player).remove(WarfareKeys.PENDING_KIT);
                        PacketHandler.sendToAllClients(player.serverLevel(), data);
                    } else {
                        int teamCount = 0;
                        int squadCount = 0;
                        String pName = player.getScoreboardName();
                        WarfareWorldData.Squad mySquad = null;

                        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
                            if (s.members.contains(pName)) {
                                mySquad = s;
                                break;
                            }
                        }

                        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                            if (p != player
                                    && p.getTeam() != null
                                    && p.getTeam().getName().toUpperCase().equals(teamName)) {
                                String otherKit = WarfareData.of(p).getString(WarfareKeys.CURRENT_KIT);
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

                        if (kit.minSquadPlayers > 0
                                && (mySquad == null || mySquad.members.size() < kit.minSquadPlayers)) {
                            allowed = false;
                        }

                        if (kit.isLeaderOnly && (mySquad == null || !mySquad.leader.equals(pName))) {
                            allowed = false;
                        }

                        if (allowed
                                && com.pigeostudios.pwp.warfare.match.KitLimits.fireSupportCapReached(
                                        kit.category, countFireSupportOthersInSquad(player, data))) {
                            allowed = false;
                        }

                        if (allowed) {
                            applyKitToPlayer(player, kit);
                        } else {
                            player.sendSystemMessage(
                                    Component.literal("Кит " + pending + " полон или заблокирован! Спавн без кита.")
                                            .withStyle(ChatFormatting.RED));
                            WarfareWorldData.KitInfo unassigned = teamName.equals("BLUE")
                                    ? data.blueKits.get("Unassigned")
                                    : data.redKits.get("Unassigned");
                            if (unassigned != null) {
                                applyKitToPlayer(player, unassigned);
                            }
                        }

                        WarfareData.of(player).remove(WarfareKeys.PENDING_KIT);
                        PacketHandler.sendToAllClients(player.serverLevel(), data);
                    }
                }
            }
        }
    }
}
