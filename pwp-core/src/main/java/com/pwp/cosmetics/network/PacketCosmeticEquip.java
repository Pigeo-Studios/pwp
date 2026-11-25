package com.pwp.cosmetics.network;

import com.pwp.cosmetics.CosmeticService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Клиент → сервер: запрос экипировать скин (skinId) или снять скин со слота (пустой skinId).
 *
 * <p>Клиент передаёт только идентификаторы; владение проверяет и предмет собирает сервер
 * ({@link CosmeticService}). Заменил PacketSyncCosmeticEquip, который принимал SNBT предмета.
 */
public class PacketCosmeticEquip {
    private static final int MAX_ID = 128;

    private final String slotType;
    private final String role;
    private final String skinId;

    public PacketCosmeticEquip(String slotType, String role, String skinId) {
        this.slotType = slotType != null ? slotType : "";
        this.role = role != null && !role.isEmpty() ? role : "ALL";
        this.skinId = skinId != null ? skinId : "";
    }

    public static void encode(PacketCosmeticEquip msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.slotType, MAX_ID);
        buf.writeUtf(msg.role, MAX_ID);
        buf.writeUtf(msg.skinId, MAX_ID);
    }

    public static PacketCosmeticEquip decode(FriendlyByteBuf buf) {
        return new PacketCosmeticEquip(buf.readUtf(MAX_ID), buf.readUtf(MAX_ID), buf.readUtf(MAX_ID));
    }

    public static void handle(PacketCosmeticEquip msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (msg.skinId.isEmpty()) {
                if (!msg.slotType.isEmpty()) CosmeticService.unequip(player, msg.slotType, msg.role);
            } else {
                CosmeticService.equip(player, msg.skinId, msg.role);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
