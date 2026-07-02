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
                    Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
                    Object mc = mcClass.getMethod("getInstance").invoke(null);
                    Object player = mcClass.getMethod("getPlayer").invoke(mc);
                    Object connection = player.getClass().getMethod("getConnection").invoke(player);
                    connection.getClass().getMethod("sendChat", String.class)
                        .invoke(connection, "/pwp votes");
                } catch (Exception e) {
                    // Silent fail
                }
            })
        );
        ctx.get().setPacketHandled(true);
    }
}
