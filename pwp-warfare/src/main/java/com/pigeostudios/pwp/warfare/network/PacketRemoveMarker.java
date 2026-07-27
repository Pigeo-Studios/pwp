package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.MarkerClientCache;
import com.pigeostudios.pwp.warfare.server.MarkerManager;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketRemoveMarker {
    private final UUID id;
    public PacketRemoveMarker(UUID id) { this.id = id; }

    public static void encode(PacketRemoveMarker msg, FriendlyByteBuf buf) { buf.writeUUID(msg.id); }
    public static PacketRemoveMarker decode(FriendlyByteBuf buf) { return new PacketRemoveMarker(buf.readUUID()); }

    public static void handle(PacketRemoveMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                MarkerManager.removeById(msg.id);
                MarkerManager.broadcastRemoval(msg.id);
            } else {
                MarkerClientCache.remove(msg.id);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
