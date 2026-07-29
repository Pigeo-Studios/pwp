package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VoteFactionPacket {

    public final String blueFaction;
    public final String redFaction;

    public VoteFactionPacket(String blueFaction, String redFaction) {
        this.blueFaction = blueFaction;
        this.redFaction = redFaction;
    }

    public static void encode(VoteFactionPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.blueFaction);
        buf.writeUtf(msg.redFaction);
    }

    public static VoteFactionPacket decode(FriendlyByteBuf buf) {
        return new VoteFactionPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(VoteFactionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                try {
                    Class<?> vmClass = Class.forName("com.pwp.lobby.FactionVotingManager");
                    vmClass.getMethod("vote", java.util.UUID.class, String.class, String.class)
                           .invoke(null, player.getUUID(), msg.blueFaction, msg.redFaction);
                } catch (Exception ignored) {}
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
