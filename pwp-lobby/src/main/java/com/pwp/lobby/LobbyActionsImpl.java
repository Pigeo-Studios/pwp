package com.pwp.lobby;

import com.pwp.api.lobby.LobbyActions;
import com.pwp.lobby.match.MatchAllocator;
import net.minecraft.server.level.ServerPlayer;

/** Реализация {@link LobbyActions} для пакетов core-client (переход в матч из GUI лобби). */
final class LobbyActionsImpl implements LobbyActions {
    @Override
    public void joinMatchById(int matchId, ServerPlayer player) {
        MatchAllocator.joinMatchById(matchId, player);
    }
}
