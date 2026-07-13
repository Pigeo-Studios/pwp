package com.pwp.coreclient.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenModeVotePacket {

    public final int remainingSeconds;
    public final int onlinePlayers;
    public final int totalVotes;
    public final String[] modeNames;
    public final String[] modeDisplayNames;
    public final String[] modeDescriptions;
    public final int[] voteCounts;

    public OpenModeVotePacket(int remainingSeconds, int onlinePlayers, int totalVotes,
                               String[] modeNames, String[] modeDisplayNames,
                               String[] modeDescriptions, int[] voteCounts) {
        this.remainingSeconds = remainingSeconds;
        this.onlinePlayers = onlinePlayers;
        this.totalVotes = totalVotes;
        this.modeNames = modeNames;
        this.modeDisplayNames = modeDisplayNames;
        this.modeDescriptions = modeDescriptions;
        this.voteCounts = voteCounts;
    }

    public static void encode(OpenModeVotePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.remainingSeconds);
        buf.writeInt(msg.onlinePlayers);
        buf.writeInt(msg.totalVotes);
        int len = msg.modeNames.length;
        buf.writeInt(len);
        for (int i = 0; i < len; i++) {
            buf.writeUtf(msg.modeNames[i]);
            buf.writeUtf(msg.modeDisplayNames[i]);
            buf.writeUtf(msg.modeDescriptions[i] != null ? msg.modeDescriptions[i] : "");
            buf.writeInt(msg.voteCounts[i]);
        }
    }

    public static OpenModeVotePacket decode(FriendlyByteBuf buf) {
        int remainingSeconds = buf.readInt();
        int onlinePlayers = buf.readInt();
        int totalVotes = buf.readInt();
        int len = buf.readInt();
        String[] modeNames = new String[len];
        String[] modeDisplayNames = new String[len];
        String[] modeDescriptions = new String[len];
        int[] voteCounts = new int[len];
        for (int i = 0; i < len; i++) {
            modeNames[i] = buf.readUtf();
            modeDisplayNames[i] = buf.readUtf();
            modeDescriptions[i] = buf.readUtf();
            voteCounts[i] = buf.readInt();
        }
        return new OpenModeVotePacket(remainingSeconds, onlinePlayers, totalVotes,
                modeNames, modeDisplayNames, modeDescriptions, voteCounts);
    }

    public static void handle(OpenModeVotePacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> {
                try {
                    Class<?> lobbyScreen = Class.forName("com.pwp.lobby.gui.LobbyScreen");
                    lobbyScreen.getMethod("openModeVote", OpenModeVotePacket.class).invoke(null, msg);
                } catch (Exception ignored) {}
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
