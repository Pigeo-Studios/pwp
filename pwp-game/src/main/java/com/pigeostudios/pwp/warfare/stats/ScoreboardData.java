package com.pigeostudios.pwp.warfare.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.match.ScoreRules;
import com.pigeostudios.pwp.warfare.match.TeamVisibility;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.network.data.DataRequests;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;

/**
 * Табло по Tab (этап 15 ROADMAP), как в Squad: своя команда — по отрядам, с ролью, K/D, оживлениями, очками
 * ({@link ScoreRules}) и пингом; чужая — только ники и очки (ни отрядов, ни ролей: это разведданные). Видимость —
 * {@link TeamVisibility}: модератор-наблюдатель видит обе команды полностью. Тип данных {@code scoreboard}.
 */
public final class ScoreboardData {
    public static final String TYPE = "scoreboard";

    private ScoreboardData() {}

    public static void register() {
        DataRequests.register(TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                return DataRequests.empty();
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                return build(p).toString();
            }
        });
    }

    static JsonObject build(ServerPlayer viewer) {
        WarfareWorldData data = WarfareWorldData.get(viewer.serverLevel());
        String key = PacketHandler.viewerKey(viewer);
        JsonObject o = new JsonObject();
        o.addProperty("mode", data.match.gameMode == null ? "aas" : data.match.gameMode);
        o.addProperty("blueFaction", data.match.blueFaction == null ? "" : data.match.blueFaction);
        o.addProperty("redFaction", data.match.redFaction == null ? "" : data.match.redFaction);
        o.addProperty("blueTickets", data.match.blueTickets);
        o.addProperty("redTickets", data.match.redTickets);
        o.addProperty("viewer", key);
        JsonArray blue = new JsonArray();
        JsonArray red = new JsonArray();
        var stats = MatchStatsTracker.get();
        for (ServerPlayer p : viewer.server.getPlayerList().getPlayers()) {
            String team = p.getTeam() == null ? "" : p.getTeam().getName().toUpperCase(java.util.Locale.ROOT);
            if (!team.equals("BLUE") && !team.equals("RED")) continue;
            boolean full = TeamVisibility.visible(team, key);
            PlayerMatchStats s = stats.isActive() ? stats.getAllPlayers().get(p.getStringUUID()) : null;
            JsonObject e = new JsonObject();
            e.addProperty("name", p.getGameProfile().getName());
            int score = s == null
                    ? 0
                    : ScoreRules.score(
                            s.kills, s.revives, s.captures, s.vehiclesDestroyed, s.suppliesDelivered, s.teamKills);
            e.addProperty("score", score);
            if (full) {
                int squadId = WarfareData.of(p).getInt(WarfareKeys.SQUAD_ID);
                WarfareWorldData.Squad squad = null;
                for (var sq : data.squadRegistry.squads) {
                    if (sq.id == squadId && sq.team.equalsIgnoreCase(team)) squad = sq;
                }
                e.addProperty("squadId", squad == null ? 0 : squad.id);
                e.addProperty("squad", squad == null ? "" : squad.name);
                e.addProperty("leader", squad != null && squad.leader.equals(p.getScoreboardName()));
                e.addProperty("role", WarfareData.of(p).getString(WarfareKeys.CURRENT_KIT));
                e.addProperty("kills", s == null ? 0 : s.kills);
                e.addProperty("deaths", s == null ? 0 : s.deaths);
                e.addProperty("revives", s == null ? 0 : s.revives);
                e.addProperty("ping", p.latency);
                e.addProperty("downed", WarfareData.of(p).getBoolean(WarfareKeys.IS_DOWNED));
            }
            (team.equals("BLUE") ? blue : red).add(e);
        }
        o.add("BLUE", blue);
        o.add("RED", red);
        return o;
    }
}
