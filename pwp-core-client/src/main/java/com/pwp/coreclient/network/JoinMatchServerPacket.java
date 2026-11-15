package com.pwp.coreclient.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class JoinMatchServerPacket {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(JoinMatchServerPacket.class);

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
                } catch (Exception ignored) {
                    PWP_LOG.warn("JoinMatchServerPacket.handle: исключение", ignored);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
