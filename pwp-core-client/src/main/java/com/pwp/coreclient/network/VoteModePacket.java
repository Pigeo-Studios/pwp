package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VoteModePacket {

    public final String modeName;

    public VoteModePacket(String modeName) {
        this.modeName = modeName;
    }

    public static void encode(VoteModePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.modeName);
    }

    public static VoteModePacket decode(FriendlyByteBuf buf) {
        return new VoteModePacket(buf.readUtf());
    }

    public static void handle(VoteModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                try {
                    Class<?> lmClass = Class.forName("com.pwp.lobby.LobbyMod");
                    lmClass.getMethod("voteMode", java.util.UUID.class, String.class).invoke(null, player.getUUID(), msg.modeName);
                } catch (Exception ignored) {}
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
