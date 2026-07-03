package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class JoinMatchPacket {

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
                    maClass.getMethod("joinActiveMatch", net.minecraft.server.level.ServerPlayer.class).invoke(null, player);
                } catch (Exception ignored) {}
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
