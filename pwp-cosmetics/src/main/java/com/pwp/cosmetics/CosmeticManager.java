package com.pwp.cosmetics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmeticManager {

    private static final Map<UUID, Map<String, String>> playerEquipment = new HashMap<>();

    public static void setEquipment(UUID playerUuid, String slotType, String role, String skinId) {
        playerEquipment.computeIfAbsent(playerUuid, k -> new HashMap<>())
                .put(slotType + ":" + role, skinId);
    }

    public static String getEquipment(UUID playerUuid, String slotType, String role) {
        Map<String, String> slots = playerEquipment.get(playerUuid);
        if (slots == null) return null;
        String skinId = slots.get(slotType + ":" + role);
        if (skinId == null) skinId = slots.get(slotType + ":ALL");
        return skinId;
    }

    public static ItemStack applySkin(Player player, ItemStack item, String slotType) {
        UUID uuid = player.getUUID();
        String role = player.getPersistentData().getString("WARFARE_CurrentKit");
        String skinId = getEquipment(uuid, slotType, role);

        if (skinId != null && SkinRegistry.get(skinId) != null) {
            item.getOrCreateTag().putString("PWP_SkinId", skinId);
        }
        return item;
    }

    public static void clearPlayer(UUID uuid) {
        playerEquipment.remove(uuid);
    }
}
