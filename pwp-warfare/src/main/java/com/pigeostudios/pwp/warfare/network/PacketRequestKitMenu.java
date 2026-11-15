package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;

// Пакет запроса меню выбора кита
// Отправляется клиентом для получения списка доступных китов
public class PacketRequestKitMenu {
    public static void encode(PacketRequestKitMenu msg, FriendlyByteBuf buf) {}

    public static PacketRequestKitMenu decode(FriendlyByteBuf buf) {
        return new PacketRequestKitMenu();
    }

    // Собирает список доступных китов с учётом лимитов и отправляет клиенту
    public static void sendKitMenu(ServerPlayer player, WarfareWorldData data) {
        if (player.getTeam() == null) return;
        String teamName = player.getTeam().getName().toUpperCase();
        boolean amILeader = KitAccess.isLeader(player, data);
        List<com.pigeostudios.pwp.warfare.match.KitLimits.Holder> others = KitAccess.others(player, data);
        List<PacketOpenPlayerKitMenu.KitDTO> dtoList = new ArrayList<>();

        Map<String, WarfareWorldData.KitInfo> kits = teamName.equals("BLUE") ? data.blueKits : data.redKits;
        // Порядок: базовые роли из KIT_NAMES, варианты (X (…)) — сразу после своей роли, остальное — в конец
        List<String> order = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (String base : WarfareWorldData.KIT_NAMES) {
            if (kits.containsKey(base) && added.add(base)) order.add(base);
            List<String> variants = new ArrayList<>();
            for (String k : kits.keySet()) {
                if (k.startsWith(base + " (")) variants.add(k);
            }
            variants.sort(String::compareTo);
            for (String v : variants) {
                if (added.add(v)) order.add(v);
            }
        }
        for (String k : kits.keySet()) {
            if (added.add(k)) order.add(k);
        }

        for (String kitName : order) {
            WarfareWorldData.KitInfo kit = kits.get(kitName);
            if (kit != null
                    && (!kit.isLeaderOnly || amILeader)
                    && (kit.maxPerTeam != 0 || kitName.equals("Unassigned"))) {
                // Единый путь проверки (KitAccess → KitLimits), что и при выборе/спавне
                String unavailable = KitAccess.unavailableReason(player, data, kitName, kit, others);
                boolean available = unavailable == null;
                String reason = available ? "" : unavailable;

                String myCurrentKit = WarfareData.of(player).getString(WarfareKeys.PENDING_KIT);
                if (myCurrentKit.isEmpty())
                    myCurrentKit = WarfareData.of(player).getString(WarfareKeys.CURRENT_KIT);
                boolean isSelected = myCurrentKit.equals(kitName);
                List<ItemStack> kitPreviewItems = new ArrayList<>(kit.inventory);
                PacketOpenPlayerKitMenu.KitDTO dto = new PacketOpenPlayerKitMenu.KitDTO(
                        kitName, kit.category, kit.description, available, reason, isSelected, kitPreviewItems);
                if (kit.slotSkins != null) {
                    for (var e : kit.slotSkins.entrySet()) dto.slotSkins.put(e.getKey(), new ArrayList<>(e.getValue()));
                }
                dtoList.add(dto);
            }
        }

        // Attach player's saved slot selections for persistent UI state
        Map<String, Integer> restoredSelections = new HashMap<>();
        CompoundTag selTag = WarfareData.of(player).getCompound(WarfareKeys.SLOT_SELECTIONS);
        for (String key : selTag.getAllKeys()) {
            restoredSelections.put(key, selTag.getInt(key));
        }

        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player), new PacketOpenPlayerKitMenu(dtoList, restoredSelections));
    }

    public static void handle(PacketRequestKitMenu msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getTeam() != null) {
                WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                sendKitMenu(player, data);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
