package com.pwp.cosmetics;

import java.util.*;
import net.minecraft.world.item.ItemStack;

public class CosmeticManager {

    private static final Map<UUID, Map<String, List<SkinData>>> playerEquipment = new HashMap<>();

    public static class SkinData {
        public final String skinId;
        public final ItemStack item;

        public SkinData(String skinId, ItemStack item) {
            this.skinId = skinId;
            this.item = item;
        }
    }

    public static void setEquipment(UUID playerUuid, String slotType, String role, String skinId, ItemStack item) {
        String key = slotType + ":" + (role != null ? role : "ALL");
        playerEquipment
                .computeIfAbsent(playerUuid, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(new SkinData(skinId, item));
    }

    public static void removeEquipment(UUID playerUuid, String slotType, String role) {
        String key = slotType + ":" + (role != null ? role : "ALL");
        Map<String, List<SkinData>> slots = playerEquipment.get(playerUuid);
        if (slots != null) {
            slots.remove(key);
        }
    }

    public static void setEquipmentSingle(
            UUID playerUuid, String slotType, String role, String skinId, ItemStack item) {
        String key = slotType + ":" + (role != null ? role : "ALL");
        List<SkinData> list = new ArrayList<>();
        if (skinId != null && !skinId.isEmpty()) {
            list.add(new SkinData(skinId, item));
        }
        playerEquipment.computeIfAbsent(playerUuid, k -> new HashMap<>()).put(key, list);
    }

    public static SkinData getEquipment(UUID playerUuid, String slotType, String role) {
        Map<String, List<SkinData>> slots = playerEquipment.get(playerUuid);
        if (slots == null) return null;
        String key = slotType + ":" + (role != null ? role : "ALL");
        List<SkinData> list = slots.get(key);
        if (list == null) list = slots.get(slotType + ":ALL");
        if (list != null && !list.isEmpty()) return list.get(0);
        return null;
    }

    public static SkinData getEquipmentBySkinId(UUID playerUuid, String skinId) {
        Map<String, List<SkinData>> slots = playerEquipment.get(playerUuid);
        if (slots == null) return null;
        for (List<SkinData> list : slots.values()) {
            for (SkinData data : list) {
                if (data.skinId.equals(skinId)) return data;
            }
        }
        return null;
    }

    public static void clearPlayer(UUID uuid) {
        playerEquipment.remove(uuid);
    }
}
