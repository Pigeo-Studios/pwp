package com.pwp.cosmetics.network;

import com.pwp.cosmetics.CosmeticManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncCosmeticEquip {
    private final String slotType;
    private final String role;
    private final String skinId;
    private final String itemSnbt;
    private final boolean unequip;

    public PacketSyncCosmeticEquip(String slotType, String role, String skinId, String itemSnbt) {
        this.slotType = slotType;
        this.role = role;
        this.skinId = skinId;
        this.itemSnbt = itemSnbt;
        this.unequip = skinId == null || skinId.isEmpty();
    }

    public static void encode(PacketSyncCosmeticEquip msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.slotType);
        buf.writeUtf(msg.role != null ? msg.role : "ALL");
        buf.writeUtf(msg.skinId != null ? msg.skinId : "");
        buf.writeUtf(msg.itemSnbt != null ? msg.itemSnbt : "");
        buf.writeBoolean(msg.unequip);
    }

    public static PacketSyncCosmeticEquip decode(FriendlyByteBuf buf) {
        String slotType = buf.readUtf();
        String role = buf.readUtf();
        String skinId = buf.readUtf();
        String itemSnbt = buf.readUtf();
        boolean unequip = buf.readBoolean();
        return new PacketSyncCosmeticEquip(slotType, role,
            unequip ? "" : skinId,
            unequip ? "" : itemSnbt);
    }

    public static void handle(PacketSyncCosmeticEquip msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (msg.unequip) {
                CosmeticManager.removeEquipment(player.getUUID(), msg.slotType, msg.role);
            } else {
                ItemStack skinItem = ItemStack.EMPTY;
                if (!msg.itemSnbt.isEmpty() && msg.itemSnbt.startsWith("{")) {
                    try {
                        CompoundTag tag = TagParser.parseTag(msg.itemSnbt);
                        skinItem = ItemStack.of(tag);
                    } catch (Exception ignored) {}
                }
                CosmeticManager.setEquipment(player.getUUID(), msg.slotType, msg.role,
                    msg.skinId.isEmpty() ? null : msg.skinId, skinItem);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
