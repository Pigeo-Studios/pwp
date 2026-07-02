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
            ctx.get().enqueueWork(() -> {
                try {
                    Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
                    Object mc = mcClass.getMethod("getInstance").invoke(null);
                    Object level = mcClass.getMethod("getLevel").invoke(mc);
                    if (level != null) {
                        level.getClass().getMethod("disconnect").invoke(level);
                        mcClass.getMethod("clearLevel").invoke(mc);
                    }
                    Object sd = Class.forName("net.minecraft.client.multiplayer.ServerData")
                            .getConstructor(String.class, String.class, boolean.class)
                            .newInstance("PWP Match", msg.host + ":" + msg.port, false);
                    Object sa = Class.forName("net.minecraft.client.multiplayer.resolver.ServerAddress")
                            .getConstructor(String.class, int.class)
                            .newInstance(msg.host, msg.port);
                    Object screen = Class.forName("net.minecraft.client.gui.screens.TitleScreen")
                            .getConstructor().newInstance();
                    Class<?> csClass = Class.forName("net.minecraft.client.gui.screens.ConnectScreen");
                    csClass.getMethod("startConnecting",
                            Class.forName("net.minecraft.client.gui.screens.Screen"),
                            mcClass,
                            Class.forName("net.minecraft.client.multiplayer.resolver.ServerAddress"),
                            Class.forName("net.minecraft.client.multiplayer.ServerData"),
                            boolean.class)
                            .invoke(null, screen, mc, sa, sd, false);
                } catch (Exception ignored) {}
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
