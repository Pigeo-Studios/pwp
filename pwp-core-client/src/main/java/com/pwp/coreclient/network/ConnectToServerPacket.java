package com.pwp.coreclient.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        mc.level.disconnect();
                        mc.clearLevel();
                    }
                    ServerData sd = new ServerData("PWP Match", msg.host + ":" + msg.port, false);
                    ServerAddress sa = new ServerAddress(msg.host, msg.port);
                    ConnectScreen.startConnecting(new TitleScreen(), mc, sa, sd, false);
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
