package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConnectToServerPacket {

    public final String host;
    public final int port;

    public ConnectToServerPacket(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void encode(ConnectToServerPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.host);
        buf.writeInt(msg.port);
    }

    public static ConnectToServerPacket decode(FriendlyByteBuf buf) {
        return new ConnectToServerPacket(buf.readUtf(), buf.readInt());
    }

    public static void handle(ConnectToServerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> ClientConnectHandler.connect(msg.host, msg.port));
        }
        ctx.get().setPacketHandled(true);
    }
}
