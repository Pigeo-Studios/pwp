package com.pwp.coreclient.network;

import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenFactionVotePacket {

    public final int remainingSeconds;
    public final int onlinePlayers;
    public final int totalVotes;
    public final String[] team1Factions;
    public final String[] team2Factions;
    public final int[] team1Votes;
    public final int[] team2Votes;

    public OpenFactionVotePacket(int remainingSeconds, int onlinePlayers, int totalVotes,
                                  String[] team1Factions, String[] team2Factions,
                                  int[] team1Votes, int[] team2Votes) {
        this.remainingSeconds = remainingSeconds;
        this.onlinePlayers = onlinePlayers;
        this.totalVotes = totalVotes;
        this.team1Factions = team1Factions;
        this.team2Factions = team2Factions;
        this.team1Votes = team1Votes;
        this.team2Votes = team2Votes;
    }

    public static void encode(OpenFactionVotePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.remainingSeconds);
        buf.writeInt(msg.onlinePlayers);
        buf.writeInt(msg.totalVotes);
        buf.writeInt(3);
        for (int i = 0; i < 3; i++) {
            buf.writeUtf(msg.team1Factions[i] != null ? msg.team1Factions[i] : "");
            buf.writeInt(msg.team1Votes[i]);
        }
        for (int i = 0; i < 3; i++) {
            buf.writeUtf(msg.team2Factions[i] != null ? msg.team2Factions[i] : "");
            buf.writeInt(msg.team2Votes[i]);
        }
    }

    public static OpenFactionVotePacket decode(FriendlyByteBuf buf) {
        int remainingSeconds = buf.readInt();
        int onlinePlayers = buf.readInt();
        int totalVotes = buf.readInt();
        buf.readInt(); // len = 3
        String[] team1Factions = new String[3];
        int[] team1Votes = new int[3];
        for (int i = 0; i < 3; i++) { team1Factions[i] = buf.readUtf(); team1Votes[i] = buf.readInt(); }
        String[] team2Factions = new String[3];
        int[] team2Votes = new int[3];
        for (int i = 0; i < 3; i++) { team2Factions[i] = buf.readUtf(); team2Votes[i] = buf.readInt(); }
        return new OpenFactionVotePacket(remainingSeconds, onlinePlayers, totalVotes,
                team1Factions, team2Factions, team1Votes, team2Votes);
    }

    public static void handle(OpenFactionVotePacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> PWPLobbyScreen.openFactionVote(msg));
        }
        ctx.get().setPacketHandled(true);
    }
}
