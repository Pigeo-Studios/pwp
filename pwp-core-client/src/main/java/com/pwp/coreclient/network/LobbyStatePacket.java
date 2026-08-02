package com.pwp.coreclient.network;

import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Единый авторитетный пакет состояния лобби.
 * Сервер шлёт его каждую секунду + при любом изменении + при входе игрока.
 * Клиент ничего не угадывает — вся истина приходит от сервера.
 */
public class LobbyStatePacket {

    // Фазы состояния лобби
    public static final int PHASE_IDLE = 0;
    public static final int PHASE_MAP_VOTE = 1;
    public static final int PHASE_MODE_VOTE = 2;
    public static final int PHASE_FACTION_VOTE = 3;
    public static final int PHASE_MATCH_STARTING = 4;
    public static final int PHASE_MATCH_PLAYING = 5;

    // Общее
    public final int phase;
    public final int remainingSeconds;
    public final int onlinePlayers;
    public final int totalVotes;
    public final String leadingName;
    public final boolean canStartNewMatch;
    public final String policyReason;
    public final String mode; // "aas" | "invasion" — для подписей команд
    public final boolean requestOpen; // true — клиент должен открыть GUI (вход на сервер, /pwp)

    // Голосование за карту
    public final String[] mapNames;
    public final String[] mapDisplayNames;
    public final String[] mapDescriptions;
    public final int[] maxPlayers;
    public final int[] voteCounts;
    public final String[] worldPaths;
    public final String[] blueFactions;
    public final String[] redFactions;

    // Голосование за режим
    public final String[] modeNames;
    public final String[] modeDisplayNames;
    public final String[] modeDescriptions;
    public final int[] modeVoteCounts;

    // Голосование за фракции
    public final String[] team1Factions;
    public final String[] team2Factions;
    public final int[] team1Votes;
    public final int[] team2Votes;

    // Персональный голос игрока (индексы в массивах, -1 = не голосовал)
    public final int myMapVote;
    public final int myModeVote;
    public final int myFaction1;
    public final int myFaction2;

    // Результаты последнего завершённого голосования
    public final String resultMap;
    public final int resultMapVotes;
    public final String resultMode;
    public final int resultModeVotes;
    public final String resultF1;
    public final int resultF1Votes;
    public final String resultF2;
    public final int resultF2Votes;

    // Активный матч
    public final String matchMapDisplay;
    public final String matchModeDisplay;
    public final String matchWorldPath;
    public final String matchBlueFaction;
    public final String matchRedFaction;
    public final String matchStatus;
    public final int matchBlueTickets;
    public final int matchRedTickets;
    public final int matchServerId;
    public final int matchElapsed;
    public final int matchPlayers;
    public final int matchMaxPlayers;
    public final boolean matchCanJoin;

    // Список всех активных матчей
    public final int matchCount;
    public final int[] mServerIds;
    public final String[] mDisplayNames;
    public final String[] mStatuses;
    public final int[] mBlueTickets;
    public final int[] mRedTickets;
    public final int[] mPlayers;
    public final int[] mMaxPlayers;
    public final int[] mElapsed;
    public final String[] mBlueFactions;
    public final String[] mRedFactions;
    public final String[] mWorldPaths;

    public LobbyStatePacket(int phase, int remainingSeconds, int onlinePlayers, int totalVotes,
                            String leadingName, boolean canStartNewMatch, String policyReason, String mode,
                            boolean requestOpen,
                            String[] mapNames, String[] mapDisplayNames, String[] mapDescriptions,
                            int[] maxPlayers, int[] voteCounts, String[] worldPaths,
                            String[] blueFactions, String[] redFactions,
                            String[] modeNames, String[] modeDisplayNames, String[] modeDescriptions,
                            int[] modeVoteCounts,
                            String[] team1Factions, String[] team2Factions,
                            int[] team1Votes, int[] team2Votes,
                            int myMapVote, int myModeVote, int myFaction1, int myFaction2,
                            String resultMap, int resultMapVotes,
                            String resultMode, int resultModeVotes,
                            String resultF1, int resultF1Votes,
                            String resultF2, int resultF2Votes,
                            String matchMapDisplay, String matchModeDisplay, String matchWorldPath,
                            String matchBlueFaction, String matchRedFaction, String matchStatus,
                            int matchBlueTickets, int matchRedTickets, int matchServerId,
                            int matchElapsed, int matchPlayers, int matchMaxPlayers, boolean matchCanJoin,
                            int matchCount, int[] mServerIds, String[] mDisplayNames, String[] mStatuses,
                            int[] mBlueTickets, int[] mRedTickets, int[] mPlayers, int[] mMaxPlayers,
                            int[] mElapsed, String[] mBlueFactions, String[] mRedFactions,
                            String[] mWorldPaths) {
        this.phase = phase;
        this.remainingSeconds = remainingSeconds;
        this.onlinePlayers = onlinePlayers;
        this.totalVotes = totalVotes;
        this.leadingName = leadingName;
        this.canStartNewMatch = canStartNewMatch;
        this.policyReason = policyReason;
        this.mode = mode;
        this.requestOpen = requestOpen;
        this.mapNames = mapNames;
        this.mapDisplayNames = mapDisplayNames;
        this.mapDescriptions = mapDescriptions;
        this.maxPlayers = maxPlayers;
        this.voteCounts = voteCounts;
        this.worldPaths = worldPaths;
        this.blueFactions = blueFactions;
        this.redFactions = redFactions;
        this.modeNames = modeNames;
        this.modeDisplayNames = modeDisplayNames;
        this.modeDescriptions = modeDescriptions;
        this.modeVoteCounts = modeVoteCounts;
        this.team1Factions = team1Factions;
        this.team2Factions = team2Factions;
        this.team1Votes = team1Votes;
        this.team2Votes = team2Votes;
        this.myMapVote = myMapVote;
        this.myModeVote = myModeVote;
        this.myFaction1 = myFaction1;
        this.myFaction2 = myFaction2;
        this.resultMap = resultMap;
        this.resultMapVotes = resultMapVotes;
        this.resultMode = resultMode;
        this.resultModeVotes = resultModeVotes;
        this.resultF1 = resultF1;
        this.resultF1Votes = resultF1Votes;
        this.resultF2 = resultF2;
        this.resultF2Votes = resultF2Votes;
        this.matchMapDisplay = matchMapDisplay;
        this.matchModeDisplay = matchModeDisplay;
        this.matchWorldPath = matchWorldPath;
        this.matchBlueFaction = matchBlueFaction;
        this.matchRedFaction = matchRedFaction;
        this.matchStatus = matchStatus;
        this.matchBlueTickets = matchBlueTickets;
        this.matchRedTickets = matchRedTickets;
        this.matchServerId = matchServerId;
        this.matchElapsed = matchElapsed;
        this.matchPlayers = matchPlayers;
        this.matchMaxPlayers = matchMaxPlayers;
        this.matchCanJoin = matchCanJoin;
        this.matchCount = matchCount;
        this.mServerIds = mServerIds;
        this.mDisplayNames = mDisplayNames;
        this.mStatuses = mStatuses;
        this.mBlueTickets = mBlueTickets;
        this.mRedTickets = mRedTickets;
        this.mPlayers = mPlayers;
        this.mMaxPlayers = mMaxPlayers;
        this.mElapsed = mElapsed;
        this.mBlueFactions = mBlueFactions;
        this.mRedFactions = mRedFactions;
        this.mWorldPaths = mWorldPaths;
    }

    public boolean hasResults() {
        return resultMap != null || resultMode != null || resultF1 != null || resultF2 != null;
    }

    // ====== encode / decode ======

    private static void writeStringArr(FriendlyByteBuf buf, String[] arr) {
        if (arr == null) { buf.writeInt(0); return; }
        buf.writeInt(arr.length);
        for (String s : arr) buf.writeUtf(s != null ? s : "");
    }

    private static void writeIntArr(FriendlyByteBuf buf, int[] arr) {
        if (arr == null) { buf.writeInt(0); return; }
        buf.writeInt(arr.length);
        for (int v : arr) buf.writeInt(v);
    }

    private static String[] readStringArr(FriendlyByteBuf buf) {
        int len = buf.readInt();
        String[] arr = new String[len];
        for (int i = 0; i < len; i++) arr[i] = buf.readUtf();
        return arr;
    }

    private static int[] readIntArr(FriendlyByteBuf buf) {
        int len = buf.readInt();
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) arr[i] = buf.readInt();
        return arr;
    }

    public static void encode(LobbyStatePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.phase);
        buf.writeInt(msg.remainingSeconds);
        buf.writeInt(msg.onlinePlayers);
        buf.writeInt(msg.totalVotes);
        buf.writeUtf(msg.leadingName != null ? msg.leadingName : "");
        buf.writeBoolean(msg.canStartNewMatch);
        buf.writeUtf(msg.policyReason != null ? msg.policyReason : "");
        buf.writeUtf(msg.mode != null ? msg.mode : "");
        buf.writeBoolean(msg.requestOpen);

        writeStringArr(buf, msg.mapNames);
        writeStringArr(buf, msg.mapDisplayNames);
        writeStringArr(buf, msg.mapDescriptions);
        writeIntArr(buf, msg.maxPlayers);
        writeIntArr(buf, msg.voteCounts);
        writeStringArr(buf, msg.worldPaths);
        writeStringArr(buf, msg.blueFactions);
        writeStringArr(buf, msg.redFactions);

        writeStringArr(buf, msg.modeNames);
        writeStringArr(buf, msg.modeDisplayNames);
        writeStringArr(buf, msg.modeDescriptions);
        writeIntArr(buf, msg.modeVoteCounts);

        writeStringArr(buf, msg.team1Factions);
        writeStringArr(buf, msg.team2Factions);
        writeIntArr(buf, msg.team1Votes);
        writeIntArr(buf, msg.team2Votes);

        buf.writeInt(msg.myMapVote);
        buf.writeInt(msg.myModeVote);
        buf.writeInt(msg.myFaction1);
        buf.writeInt(msg.myFaction2);

        buf.writeUtf(msg.resultMap != null ? msg.resultMap : "");
        buf.writeInt(msg.resultMapVotes);
        buf.writeUtf(msg.resultMode != null ? msg.resultMode : "");
        buf.writeInt(msg.resultModeVotes);
        buf.writeUtf(msg.resultF1 != null ? msg.resultF1 : "");
        buf.writeInt(msg.resultF1Votes);
        buf.writeUtf(msg.resultF2 != null ? msg.resultF2 : "");
        buf.writeInt(msg.resultF2Votes);

        buf.writeUtf(msg.matchMapDisplay != null ? msg.matchMapDisplay : "");
        buf.writeUtf(msg.matchModeDisplay != null ? msg.matchModeDisplay : "");
        buf.writeUtf(msg.matchWorldPath != null ? msg.matchWorldPath : "");
        buf.writeUtf(msg.matchBlueFaction != null ? msg.matchBlueFaction : "");
        buf.writeUtf(msg.matchRedFaction != null ? msg.matchRedFaction : "");
        buf.writeUtf(msg.matchStatus != null ? msg.matchStatus : "");
        buf.writeInt(msg.matchBlueTickets);
        buf.writeInt(msg.matchRedTickets);
        buf.writeInt(msg.matchServerId);
        buf.writeInt(msg.matchElapsed);
        buf.writeInt(msg.matchPlayers);
        buf.writeInt(msg.matchMaxPlayers);
        buf.writeBoolean(msg.matchCanJoin);

        buf.writeInt(msg.matchCount);
        writeIntArr(buf, msg.mServerIds);
        writeStringArr(buf, msg.mDisplayNames);
        writeStringArr(buf, msg.mStatuses);
        writeIntArr(buf, msg.mBlueTickets);
        writeIntArr(buf, msg.mRedTickets);
        writeIntArr(buf, msg.mPlayers);
        writeIntArr(buf, msg.mMaxPlayers);
        writeIntArr(buf, msg.mElapsed);
        writeStringArr(buf, msg.mBlueFactions);
        writeStringArr(buf, msg.mRedFactions);
        writeStringArr(buf, msg.mWorldPaths);
    }

    private static String opt(String s) { return s == null || s.isEmpty() ? null : s; }

    public static LobbyStatePacket decode(FriendlyByteBuf buf) {
        int phase = buf.readInt();
        int remainingSeconds = buf.readInt();
        int onlinePlayers = buf.readInt();
        int totalVotes = buf.readInt();
        String leadingName = opt(buf.readUtf());
        boolean canStartNewMatch = buf.readBoolean();
        String policyReason = opt(buf.readUtf());
        String mode = opt(buf.readUtf());
        boolean requestOpen = buf.readBoolean();

        String[] mapNames = readStringArr(buf);
        String[] mapDisplayNames = readStringArr(buf);
        String[] mapDescriptions = readStringArr(buf);
        int[] maxPlayers = readIntArr(buf);
        int[] voteCounts = readIntArr(buf);
        String[] worldPaths = readStringArr(buf);
        String[] blueFactions = readStringArr(buf);
        String[] redFactions = readStringArr(buf);

        String[] modeNames = readStringArr(buf);
        String[] modeDisplayNames = readStringArr(buf);
        String[] modeDescriptions = readStringArr(buf);
        int[] modeVoteCounts = readIntArr(buf);

        String[] team1Factions = readStringArr(buf);
        String[] team2Factions = readStringArr(buf);
        int[] team1Votes = readIntArr(buf);
        int[] team2Votes = readIntArr(buf);

        int myMapVote = buf.readInt();
        int myModeVote = buf.readInt();
        int myFaction1 = buf.readInt();
        int myFaction2 = buf.readInt();

        String resultMap = opt(buf.readUtf());
        int resultMapVotes = buf.readInt();
        String resultMode = opt(buf.readUtf());
        int resultModeVotes = buf.readInt();
        String resultF1 = opt(buf.readUtf());
        int resultF1Votes = buf.readInt();
        String resultF2 = opt(buf.readUtf());
        int resultF2Votes = buf.readInt();

        String matchMapDisplay = opt(buf.readUtf());
        String matchModeDisplay = opt(buf.readUtf());
        String matchWorldPath = opt(buf.readUtf());
        String matchBlueFaction = opt(buf.readUtf());
        String matchRedFaction = opt(buf.readUtf());
        String matchStatus = opt(buf.readUtf());
        int matchBlueTickets = buf.readInt();
        int matchRedTickets = buf.readInt();
        int matchServerId = buf.readInt();
        int matchElapsed = buf.readInt();
        int matchPlayers = buf.readInt();
        int matchMaxPlayers = buf.readInt();
        boolean matchCanJoin = buf.readBoolean();

        int matchCount = buf.readInt();
        int[] mServerIds = readIntArr(buf);
        String[] mDisplayNames = readStringArr(buf);
        String[] mStatuses = readStringArr(buf);
        int[] mBlueTickets = readIntArr(buf);
        int[] mRedTickets = readIntArr(buf);
        int[] mPlayers = readIntArr(buf);
        int[] mMaxPlayers = readIntArr(buf);
        int[] mElapsed = readIntArr(buf);
        String[] mBlueFactions = readStringArr(buf);
        String[] mRedFactions = readStringArr(buf);
        String[] mWorldPaths = readStringArr(buf);

        return new LobbyStatePacket(phase, remainingSeconds, onlinePlayers, totalVotes,
                leadingName, canStartNewMatch, policyReason, mode, requestOpen,
                mapNames, mapDisplayNames, mapDescriptions, maxPlayers, voteCounts, worldPaths,
                blueFactions, redFactions,
                modeNames, modeDisplayNames, modeDescriptions, modeVoteCounts,
                team1Factions, team2Factions, team1Votes, team2Votes,
                myMapVote, myModeVote, myFaction1, myFaction2,
                resultMap, resultMapVotes, resultMode, resultModeVotes,
                resultF1, resultF1Votes, resultF2, resultF2Votes,
                matchMapDisplay, matchModeDisplay, matchWorldPath,
                matchBlueFaction, matchRedFaction, matchStatus,
                matchBlueTickets, matchRedTickets, matchServerId,
                matchElapsed, matchPlayers, matchMaxPlayers, matchCanJoin,
                matchCount, mServerIds, mDisplayNames, mStatuses,
                mBlueTickets, mRedTickets, mPlayers, mMaxPlayers, mElapsed, mBlueFactions, mRedFactions,
                mWorldPaths);
    }

    public static void handle(LobbyStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> PWPLobbyScreen.updateLobbyState(msg));
        }
        ctx.get().setPacketHandled(true);
    }
}
