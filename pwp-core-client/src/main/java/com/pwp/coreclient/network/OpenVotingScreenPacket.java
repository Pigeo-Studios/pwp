package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenVotingScreenPacket {

    public OpenVotingScreenPacket() {}

    public static void encode(OpenVotingScreenPacket msg, FriendlyByteBuf buf) {}

    public static OpenVotingScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenVotingScreenPacket();
    }

    public static void handle(OpenVotingScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                try {
                    Class<?> votingScreenClass = Class.forName("com.pwp.lobby.gui.VotingScreen");
                    votingScreenClass.getConstructor().newInstance();
                } catch (Exception ignored) {}
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
