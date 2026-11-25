package com.pwp.coreclient.gui.devtest;

import com.pwp.coreclient.network.LobbyStatePacket;

/**
 * Построитель моковых {@link LobbyStatePacket} для тест-стенда (через публичный конструктор
 * пакета — сам пакет не меняется). Поля по умолчанию пустые, фикстуры заполняют нужное.
 */
public final class LobbyStateFixture {
    public int phase = LobbyStatePacket.PHASE_IDLE;
    public int remainingSeconds = 45;
    public int onlinePlayers = 24;
    public int totalVotes = 11;
    public String leadingName = "";
    public boolean canStartNewMatch = true;
    public String policyReason = "";
    public String mode = "aas";

    public String[] mapNames = {};
    public String[] mapDisplayNames = {};
    public String[] mapDescriptions = {};
    public int[] maxPlayers = {};
    public int[] voteCounts = {};
    public String[] worldPaths = {};
    public String[] blueFactions = {};
    public String[] redFactions = {};

    public String[] modeNames = {};
    public String[] modeDisplayNames = {};
    public String[] modeDescriptions = {};
    public int[] modeVoteCounts = {};

    public String[] team1Factions = {};
    public String[] team2Factions = {};
    public int[] team1Votes = {};
    public int[] team2Votes = {};

    public int myMapVote = -1, myModeVote = -1, myFaction1 = -1, myFaction2 = -1;
    public String resultMap, resultMode, resultF1, resultF2;
    public int resultMapVotes, resultModeVotes, resultF1Votes, resultF2Votes;
    public String matchMapDisplay = "", matchModeDisplay = "", matchWorldPath = "";
    public String matchBlueFaction = "", matchRedFaction = "", matchStatus = "";
    public int matchBlueTickets, matchRedTickets, matchServerId = -1, matchElapsed, matchPlayers, matchMaxPlayers;
    public boolean matchCanJoin;

    public int[] mServerIds = {};
    public String[] mDisplayNames = {};
    public String[] mStatuses = {};
    public int[] mBlueTickets = {};
    public int[] mRedTickets = {};
    public int[] mPlayers = {};
    public int[] mMaxPlayers = {};
    public int[] mElapsed = {};
    public String[] mBlueFactions = {};
    public String[] mRedFactions = {};
    public String[] mWorldPaths = {};

    public LobbyStatePacket build() {
        return new LobbyStatePacket(
                phase,
                remainingSeconds,
                onlinePlayers,
                totalVotes,
                leadingName,
                canStartNewMatch,
                policyReason,
                mode,
                false,
                mapNames,
                mapDisplayNames,
                mapDescriptions,
                maxPlayers,
                voteCounts,
                worldPaths,
                blueFactions,
                redFactions,
                modeNames,
                modeDisplayNames,
                modeDescriptions,
                modeVoteCounts,
                team1Factions,
                team2Factions,
                team1Votes,
                team2Votes,
                myMapVote,
                myModeVote,
                myFaction1,
                myFaction2,
                resultMap,
                resultMapVotes,
                resultMode,
                resultModeVotes,
                resultF1,
                resultF1Votes,
                resultF2,
                resultF2Votes,
                matchMapDisplay,
                matchModeDisplay,
                matchWorldPath,
                matchBlueFaction,
                matchRedFaction,
                matchStatus,
                matchBlueTickets,
                matchRedTickets,
                matchServerId,
                matchElapsed,
                matchPlayers,
                matchMaxPlayers,
                matchCanJoin,
                mServerIds.length,
                mServerIds,
                mDisplayNames,
                mStatuses,
                mBlueTickets,
                mRedTickets,
                mPlayers,
                mMaxPlayers,
                mElapsed,
                mBlueFactions,
                mRedFactions,
                mWorldPaths);
    }
}
