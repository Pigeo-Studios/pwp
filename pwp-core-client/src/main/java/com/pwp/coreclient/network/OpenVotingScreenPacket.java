package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenVotingScreenPacket {

    public OpenVotingScreenPacket() {}

    public static void encode(OpenVotingScreenPacket msg, FriendlyByteBuf buf) {}

    public static OpenVotingScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenVotingScreenPacket();
    }

    public static void handle(OpenVotingScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        Class<?> screenClass = Class.forName("com.pwp.lobby.gui.VotingScreen");
                        screenClass.getMethod("open").invoke(null);
                    } catch (Exception e) {
                        // pwp-lobby not installed on client
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
