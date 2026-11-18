package com.pwp.lobby;

import com.google.gson.JsonObject;
import com.pwp.api.config.LobbySettings;
import com.pwp.api.lobby.LobbyActions;
import com.pwp.api.security.PwpPermissions;
import com.pwp.coreclient.network.data.DataRequests;
import com.pwp.lobby.maps.MapRegistry;
import com.pwp.lobby.match.MatchAllocator;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;

/**
 * Реализация {@link LobbyActions} для пакетов core-client: переход в матч из GUI лобби и действия
 * вкладки «Лобби» админ-панели. Ошибки — ключи перевода {@code pwp_core.admin.lobby.error.*}.
 */
final class LobbyActionsImpl implements LobbyActions {
    /** Тип данных вкладки «Лобби» (клиент: {@code LobbyAdminTab.TYPE}). */
    static final String ADMIN_TYPE = "adminLobby";

    @Override
    public void joinMatchById(int matchId, ServerPlayer player) {
        MatchAllocator.joinMatchById(matchId, player);
    }

    @Override
    public String adminStartVote(ServerPlayer admin) {
        if (MatchAllocator.hasActiveMatch()) return "pwp_core.admin.lobby.error.match_active";
        if (LobbyMod.isAnyVoteActive()) return "pwp_core.admin.lobby.error.vote_active";
        if (MapRegistry.getVotable().isEmpty()) return "pwp_core.admin.lobby.error.no_maps";
        if (!com.pwp.api.core.MatchPolicy.canStartNewMatch()) return "pwp_core.admin.lobby.error.policy";
        String err = VotingManager.startVoting();
        if (err != null) return err;
        return VotingManager.isActive() ? null : "pwp_core.admin.error.internal";
    }

    @Override
    public String adminStopVote(ServerPlayer admin) {
        boolean any = LobbyMod.stopAllVotes();
        return any ? null : "pwp_core.admin.lobby.error.no_vote";
    }

    @Override
    public String adminSaveSettings(ServerPlayer admin, LobbySettings settings, boolean persist) {
        boolean saved = ServerConfig.apply(settings, persist);
        LobbyMod.broadcastLobbyState();
        return saved ? null : "pwp_core.admin.lobby.error.save_failed";
    }

    /** Тип {@code adminLobby}: состояние лобби для админ-панели (не-админу — только ошибка). */
    static void registerData() {
        DataRequests.register(ADMIN_TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                return DataRequests.empty();
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                JsonObject o = new JsonObject();
                if (!PwpPermissions.isAdmin(p)) {
                    o.addProperty("error", "pwp_core.admin.error.no_permission");
                    return o.toString();
                }
                String phase;
                int remaining = 0;
                if (VotingManager.isActive()) {
                    phase = "map_vote";
                    remaining = VotingManager.getRemainingSeconds();
                } else if (LobbyMod.isModeVoteActive()) {
                    phase = "mode_vote";
                    remaining = LobbyMod.getModeVoteRemainingSeconds();
                } else if (FactionVotingManager.isActive()) {
                    phase = "faction_vote";
                    remaining = FactionVotingManager.getRemainingSeconds();
                } else if (MatchAllocator.hasActiveMatch()) {
                    phase = "match";
                } else {
                    phase = "idle";
                }
                o.addProperty("phase", phase);
                o.addProperty("remaining", Math.max(0, remaining));
                o.addProperty("lobbyPlayers", MatchAllocator.getLobbyPlayerCount());
                o.addProperty("activeMatches", MatchAllocator.getActiveMatches().size());
                o.add("settings", ServerConfig.get().toJson());
                o.addProperty("persisted", ServerConfig.isPersisted());
                return o.toString();
            }
        });
    }
}
