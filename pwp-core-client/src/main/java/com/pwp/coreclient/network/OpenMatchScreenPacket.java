package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMatchScreenPacket {

    public final String mapDisplayName;
    public final String modeDisplayName;
    public final String blueFaction;
    public final String redFaction;
    public final int blueTickets;
    public final int redTickets;
    public final int remainingSeconds;
    public final String status;
    public final int onlinePlayers;
    public final boolean canJoin;

    public OpenMatchScreenPacket(String mapDisplayName, String modeDisplayName,
                                  String blueFaction, String redFaction,
                                  int blueTickets, int redTickets,
                                  int remainingSeconds, String status,
                                  int onlinePlayers, boolean canJoin) {
        this.mapDisplayName = mapDisplayName;
        this.modeDisplayName = modeDisplayName;
        this.blueFaction = blueFaction;
        this.redFaction = redFaction;
        this.blueTickets = blueTickets;
        this.redTickets = redTickets;
        this.remainingSeconds = remainingSeconds;
        this.status = status;
        this.onlinePlayers = onlinePlayers;
        this.canJoin = canJoin;
    }

    public static void encode(OpenMatchScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mapDisplayName);
        buf.writeUtf(msg.modeDisplayName);
        buf.writeUtf(msg.blueFaction);
        buf.writeUtf(msg.redFaction);
        buf.writeInt(msg.blueTickets);
        buf.writeInt(msg.redTickets);
        buf.writeInt(msg.remainingSeconds);
        buf.writeUtf(msg.status);
        buf.writeInt(msg.onlinePlayers);
        buf.writeBoolean(msg.canJoin);
    }

    public static OpenMatchScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenMatchScreenPacket(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void handle(OpenMatchScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                try {
                    Class<?> lobbyScreen = Class.forName("com.pwp.lobby.gui.LobbyScreen");
                    lobbyScreen.getMethod("openMatch", OpenMatchScreenPacket.class).invoke(null, msg);
                } catch (Exception ignored) {}
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
