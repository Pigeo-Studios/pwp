package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class JoinMatchServerPacket {

    public final int serverId;

    public JoinMatchServerPacket(int serverId) {
        this.serverId = serverId;
    }

    public static void encode(JoinMatchServerPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.serverId);
    }

    public static JoinMatchServerPacket decode(FriendlyByteBuf buf) {
        return new JoinMatchServerPacket(buf.readInt());
    }

    public static void handle(JoinMatchServerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                try {
                    Class<?> maClass = Class.forName("com.pwp.lobby.match.MatchAllocator");
                    maClass.getMethod("joinMatchById", int.class, net.minecraft.server.level.ServerPlayer.class)
                            .invoke(null, msg.serverId, player);
                } catch (Exception ignored) {}
            }
        });
        ctx.get().setPacketHandled(true);
    }
}