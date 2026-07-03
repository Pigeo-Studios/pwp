package com.pwp.cosmetics;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmeticManager {

    private static final Map<UUID, Map<String, SkinData>> playerEquipment = new HashMap<>();

    public static class SkinData {
        public final String skinId;
        public final ItemStack item;

        public SkinData(String skinId, ItemStack item) {
            this.skinId = skinId;
            this.item = item;
        }
    }

    public static void setEquipment(UUID playerUuid, String slotType, String role, String skinId, ItemStack item) {
        playerEquipment.computeIfAbsent(playerUuid, k -> new HashMap<>())
                .put(slotType + ":" + role, new SkinData(skinId, item));
    }

    public static SkinData getEquipment(UUID playerUuid, String slotType, String role) {
        Map<String, SkinData> slots = playerEquipment.get(playerUuid);
        if (slots == null) return null;
        SkinData data = slots.get(slotType + ":" + role);
        if (data == null) data = slots.get(slotType + ":ALL");
        return data;
    }

    public static SkinData getEquipmentBySkinId(UUID playerUuid, String skinId) {
        Map<String, SkinData> slots = playerEquipment.get(playerUuid);
        if (slots == null) return null;
        for (SkinData data : slots.values()) {
            if (data.skinId.equals(skinId)) return data;
        }
        return null;
    }

    public static ItemStack getSkinItem(UUID playerUuid, String slotType, String role) {
        SkinData data = getEquipment(playerUuid, slotType, role);
        if (data != null && !data.item.isEmpty()) {
            return data.item.copy();
        }
        return ItemStack.EMPTY;
    }

    public static void clearPlayer(UUID uuid) {
        playerEquipment.remove(uuid);
    }
}
