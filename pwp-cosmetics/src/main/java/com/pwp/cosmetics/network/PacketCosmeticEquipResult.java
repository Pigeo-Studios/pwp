package com.pwp.cosmetics.network;

import com.pwp.cosmetics.client.CosmeticClientState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер → клиент: результат экипировки/снятия + свежий профиль игрока (JSON core-service). */
public class PacketCosmeticEquipResult {
    private static final int MAX_PROFILE = 262144;

    private final boolean success;
    private final String message;
    private final String profileJson;

    public PacketCosmeticEquipResult(boolean success, String message, String profileJson) {
        this.success = success;
        this.message = message != null ? message : "";
        this.profileJson = profileJson != null ? profileJson : "";
    }

    public static void encode(PacketCosmeticEquipResult msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        buf.writeUtf(msg.message);
        buf.writeUtf(msg.profileJson, MAX_PROFILE);
    }

    public static PacketCosmeticEquipResult decode(FriendlyByteBuf buf) {
        return new PacketCosmeticEquipResult(buf.readBoolean(), buf.readUtf(), buf.readUtf(MAX_PROFILE));
    }

    public static void handle(PacketCosmeticEquipResult msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CosmeticClientState.onEquipResult(msg.success, msg.message, msg.profileJson)));
        ctx.get().setPacketHandled(true);
    }
}
