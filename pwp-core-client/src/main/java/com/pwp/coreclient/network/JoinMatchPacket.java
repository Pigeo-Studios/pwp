package com.pwp.coreclient.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class JoinMatchPacket {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(JoinMatchPacket.class);

    public JoinMatchPacket() {}

    public static void encode(JoinMatchPacket msg, FriendlyByteBuf buf) {}

    public static JoinMatchPacket decode(FriendlyByteBuf buf) {
        return new JoinMatchPacket();
    }

    public static void handle(JoinMatchPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                try {
                    Class<?> maClass = Class.forName("com.pwp.lobby.match.MatchAllocator");
                    maClass.getMethod("joinActiveMatch", net.minecraft.server.level.ServerPlayer.class)
                            .invoke(null, player);
                } catch (Exception ignored) {
                    PWP_LOG.warn("JoinMatchPacket.handle: исключение", ignored);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
