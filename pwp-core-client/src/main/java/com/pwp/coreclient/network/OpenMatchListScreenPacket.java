package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMatchListScreenPacket {

    public final int count;
    public final String[] mapNames;
    public final String[] displayNames;
    public final String[] statuses;
    public final int[] blueTickets;
    public final int[] redTickets;
    public final int[] playerCounts;
    public final int[] maxPlayers;
    public final int[] elapsedSeconds;
    public final int[] serverIds;
    public final String[] worldPaths;
    public final String[] blueFactions;
    public final String[] redFactions;

    public OpenMatchListScreenPacket(int count,
                                      String[] mapNames, String[] displayNames,
                                      String[] statuses,
                                      int[] blueTickets, int[] redTickets,
                                      int[] playerCounts, int[] maxPlayers,
                                      int[] elapsedSeconds, int[] serverIds,
                                      String[] worldPaths,
                                      String[] blueFactions, String[] redFactions) {
        this.count = count;
        this.mapNames = mapNames;
        this.displayNames = displayNames;
        this.statuses = statuses;
        this.blueTickets = blueTickets;
        this.redTickets = redTickets;
        this.playerCounts = playerCounts;
        this.maxPlayers = maxPlayers;
        this.elapsedSeconds = elapsedSeconds;
        this.serverIds = serverIds;
        this.worldPaths = worldPaths;
        this.blueFactions = blueFactions;
        this.redFactions = redFactions;
    }

    public static void encode(OpenMatchListScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.count);
        for (int i = 0; i < msg.count; i++) {
            buf.writeUtf(msg.mapNames[i]);
            buf.writeUtf(msg.displayNames[i]);
            buf.writeUtf(msg.statuses[i]);
            buf.writeInt(msg.blueTickets[i]);
            buf.writeInt(msg.redTickets[i]);
            buf.writeInt(msg.playerCounts[i]);
            buf.writeInt(msg.maxPlayers[i]);
            buf.writeInt(msg.elapsedSeconds[i]);
            buf.writeInt(msg.serverIds[i]);
            buf.writeUtf(msg.worldPaths[i] != null ? msg.worldPaths[i] : "");
            buf.writeUtf(msg.blueFactions[i] != null ? msg.blueFactions[i] : "");
            buf.writeUtf(msg.redFactions[i] != null ? msg.redFactions[i] : "");
        }
    }

    public static OpenMatchListScreenPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        String[] mapNames = new String[count];
        String[] displayNames = new String[count];
        String[] statuses = new String[count];
        int[] blueTickets = new int[count];
        int[] redTickets = new int[count];
        int[] playerCounts = new int[count];
        int[] maxPlayers = new int[count];
        int[] elapsedSeconds = new int[count];
        int[] serverIds = new int[count];
        String[] worldPaths = new String[count];
        String[] blueFactions = new String[count];
        String[] redFactions = new String[count];

        for (int i = 0; i < count; i++) {
            mapNames[i] = buf.readUtf();
            displayNames[i] = buf.readUtf();
            statuses[i] = buf.readUtf();
            blueTickets[i] = buf.readInt();
            redTickets[i] = buf.readInt();
            playerCounts[i] = buf.readInt();
            maxPlayers[i] = buf.readInt();
            elapsedSeconds[i] = buf.readInt();
            serverIds[i] = buf.readInt();
            worldPaths[i] = buf.readUtf();
            blueFactions[i] = buf.readUtf();
            redFactions[i] = buf.readUtf();
        }

        return new OpenMatchListScreenPacket(count, mapNames, displayNames, statuses,
                blueTickets, redTickets, playerCounts, maxPlayers,
                elapsedSeconds, serverIds, worldPaths, blueFactions, redFactions);
    }

    public static void handle(OpenMatchListScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                try {
                    Class<?> screenClass = Class.forName("com.pwp.lobby.gui.LobbyScreen");
                    screenClass.getMethod("openList", OpenMatchListScreenPacket.class).invoke(null, msg);
                } catch (Exception e) {
                    try {
                        Class<?> fallback = Class.forName("com.pwp.lobby.gui.MatchListScreen");
                        fallback.getMethod("openWithPacket", OpenMatchListScreenPacket.class).invoke(null, msg);
                    } catch (Exception ignored) {}
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}