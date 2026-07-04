package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientSkinManager;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncPlayerSkin {
    public final UUID playerUUID;
    public final String faction;
    public final String kitName;

    public PacketSyncPlayerSkin(UUID playerUUID, String faction, String kitName) {
        this.playerUUID = playerUUID;
        this.faction = faction != null ? faction : "";
        this.kitName = kitName != null ? kitName : "";
    }

    public static void encode(PacketSyncPlayerSkin msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUUID);
        buf.writeUtf(msg.faction);
        buf.writeUtf(msg.kitName);
    }

    public static PacketSyncPlayerSkin decode(FriendlyByteBuf buf) {
        return new PacketSyncPlayerSkin(buf.readUUID(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketSyncPlayerSkin msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSkinManager.handlePacket(msg)));
        ctx.get().setPacketHandled(true);
    }
}
