package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenVotingScreenPacket {

    public final int remainingSeconds;
    public final int onlinePlayers;
    public final int totalVotes;
    public final String leaderName;
    public final String[] mapNames;
    public final String[] mapDisplayNames;
    public final String[] mapDescriptions;
    public final int[] maxPlayers;
    public final int[] voteCounts;
    public final String[] worldPaths;
    public final String[] blueFactions;
    public final String[] redFactions;

    public OpenVotingScreenPacket(int remainingSeconds, int onlinePlayers, int totalVotes,
                                   String leaderName,
                                   String[] mapNames, String[] mapDisplayNames,
                                   String[] mapDescriptions, int[] maxPlayers,
                                   int[] voteCounts, String[] worldPaths,
                                   String[] blueFactions, String[] redFactions) {
        this.remainingSeconds = remainingSeconds;
        this.onlinePlayers = onlinePlayers;
        this.totalVotes = totalVotes;
        this.leaderName = leaderName;
        this.mapNames = mapNames;
        this.mapDisplayNames = mapDisplayNames;
        this.mapDescriptions = mapDescriptions;
        this.maxPlayers = maxPlayers;
        this.voteCounts = voteCounts;
        this.worldPaths = worldPaths;
        this.blueFactions = blueFactions;
        this.redFactions = redFactions;
    }

    public static void encode(OpenVotingScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.remainingSeconds);
        buf.writeInt(msg.onlinePlayers);
        buf.writeInt(msg.totalVotes);
        buf.writeUtf(msg.leaderName != null ? msg.leaderName : "");

        int len = msg.mapNames.length;
        buf.writeInt(len);
        for (int i = 0; i < len; i++) {
            buf.writeUtf(msg.mapNames[i]);
            buf.writeUtf(msg.mapDisplayNames[i]);
            buf.writeUtf(msg.mapDescriptions[i] != null ? msg.mapDescriptions[i] : "");
            buf.writeInt(msg.maxPlayers[i]);
            buf.writeInt(msg.voteCounts[i]);
            buf.writeUtf(msg.worldPaths[i] != null ? msg.worldPaths[i] : "");
            buf.writeUtf(msg.blueFactions[i] != null ? msg.blueFactions[i] : "");
            buf.writeUtf(msg.redFactions[i] != null ? msg.redFactions[i] : "");
        }
    }

    public static OpenVotingScreenPacket decode(FriendlyByteBuf buf) {
        int remainingSeconds = buf.readInt();
        int onlinePlayers = buf.readInt();
        int totalVotes = buf.readInt();
        String leaderName = buf.readUtf();
        if (leaderName.isEmpty()) leaderName = null;

        int len = buf.readInt();
        String[] mapNames = new String[len];
        String[] mapDisplayNames = new String[len];
        String[] mapDescriptions = new String[len];
        int[] maxPlayers = new int[len];
        int[] voteCounts = new int[len];
        String[] worldPaths = new String[len];
        String[] blueFactions = new String[len];
        String[] redFactions = new String[len];

        for (int i = 0; i < len; i++) {
            mapNames[i] = buf.readUtf();
            mapDisplayNames[i] = buf.readUtf();
            mapDescriptions[i] = buf.readUtf();
            maxPlayers[i] = buf.readInt();
            voteCounts[i] = buf.readInt();
            worldPaths[i] = buf.readUtf();
            blueFactions[i] = buf.readUtf();
            redFactions[i] = buf.readUtf();
        }

        return new OpenVotingScreenPacket(remainingSeconds, onlinePlayers, totalVotes,
                leaderName, mapNames, mapDisplayNames, mapDescriptions,
                maxPlayers, voteCounts, worldPaths, blueFactions, redFactions);
    }

    public static void handle(OpenVotingScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                try {
                    Class<?> lobbyScreen = Class.forName("com.pwp.lobby.gui.LobbyScreen");
                    lobbyScreen.getMethod("openVote", OpenVotingScreenPacket.class).invoke(null, msg);
                } catch (Exception e) {
                    try {
                        Class<?> votingScreen = Class.forName("com.pwp.lobby.gui.VotingScreen");
                        votingScreen.getMethod("resetVoteSession").invoke(null);
                        votingScreen.getMethod("openWithPacket", OpenVotingScreenPacket.class).invoke(null, msg);
                    } catch (Exception ignored) {}
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}