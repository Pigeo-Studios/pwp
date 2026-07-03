package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VoteMapPacket {

    public final String mapName;

    public VoteMapPacket(String mapName) {
        this.mapName = mapName;
    }

    public static void encode(VoteMapPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mapName);
    }

    public static VoteMapPacket decode(FriendlyByteBuf buf) {
        return new VoteMapPacket(buf.readUtf());
    }

    public static void handle(VoteMapPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                try {
                    Class<?> vmClass = Class.forName("com.pwp.lobby.VotingManager");
                    java.util.UUID uuid = player.getUUID();
                    vmClass.getMethod("vote", java.util.UUID.class, String.class).invoke(null, uuid, msg.mapName);
                } catch (Exception ignored) {}
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
