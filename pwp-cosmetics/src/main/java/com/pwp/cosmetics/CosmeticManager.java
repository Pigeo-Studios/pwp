package com.pwp.cosmetics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmeticManager {

    private static final Logger log = LoggerFactory.getLogger(CosmeticManager.class);
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
        log.info("setEquipment: uuid={}, slot={}, role={}, skinId={}, hasItem={}", playerUuid, slotType, role, skinId, !item.isEmpty());
        playerEquipment.computeIfAbsent(playerUuid, k -> new HashMap<>())
                .put(slotType + ":" + role, new SkinData(skinId, item));
    }

    public static SkinData getEquipment(UUID playerUuid, String slotType, String role) {
        Map<String, SkinData> slots = playerEquipment.get(playerUuid);
        if (slots == null) {
            log.info("getEquipment: uuid={} NOT FOUND in map", playerUuid);
            return null;
        }
        SkinData data = slots.get(slotType + ":" + role);
        if (data == null) data = slots.get(slotType + ":ALL");
        if (data == null) {
            for (String st : new String[]{"KNIFE", "MELEE", "PRIMARY", "SECONDARY", "UNIFORM"}) {
                data = slots.get(st + ":" + role);
                if (data == null) data = slots.get(st + ":ALL");
                if (data != null) break;
            }
        }
        log.info("getEquipment: uuid={}, slot={}, role={}, found={}", playerUuid, slotType, role, data != null ? data.skinId : null);
        return data;
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
