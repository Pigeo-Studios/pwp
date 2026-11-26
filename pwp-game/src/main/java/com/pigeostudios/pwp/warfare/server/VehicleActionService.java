package com.pigeostudios.pwp.warfare.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.network.PacketVehicleDriveAnswer;
import com.pigeostudios.pwp.warfare.network.PacketVehicleDriveRequest;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.vehicles.DriveRequests;
import com.pigeostudios.pwp.warfare.vehicles.VehicleActions;
import com.pigeostudios.pwp.warfare.vehicles.VehicleActions.Action;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.time.GameClock;
import com.pwp.coreclient.network.data.DataRequests;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Радиальное меню техники (этап 17a, как в Squad): закрепить за отрядом, «только мой отряд», открепить,
 * запросить вождение, разрешить/отказать. Правила — {@link VehicleActions} (pwp-logic); клиент присылает только
 * id машины и действие, сервер всё пересчитывает сам. Тип данных {@link #DATA_TYPE} — пункты для радиалки.
 * Запросы на вождение общие с прежними ссылками в чате ({@code /vrequest}, {@code /vanswer}).
 */
public final class VehicleActionService {
    public static final String DATA_TYPE = "vehicleActions";
    /** Дальше — не машина «перед тобой» (как дальность взаимодействия). */
    static final double MAX_DIST = 8.0;

    private static final DriveRequests REQUESTS = new DriveRequests();

    private VehicleActionService() {}

    public static void register() {
        DataRequests.register(DATA_TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                return DataRequests.empty();
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                int id = q != null && q.has("entityId") ? q.get("entityId").getAsInt() : -1;
                return describe(p, id).toString();
            }
        });
    }

    /** Пункты радиалки для машины {@code entityId}: {entityId, actions[], requests[{uuid,name}], locked}. */
    static JsonObject describe(ServerPlayer p, int entityId) {
        JsonObject o = new JsonObject();
        o.addProperty("entityId", entityId);
        JsonArray actions = new JsonArray();
        JsonArray requests = new JsonArray();
        Entity v = vehicle(p, entityId);
        if (v != null) {
            VehicleActions.State s = state(p, v);
            for (Action a : VehicleActions.available(s)) actions.add(a.id());
            if (s.squadLeader()) {
                for (DriveRequests.Request r : pendingForLeader(p, v)) {
                    JsonObject e = new JsonObject();
                    e.addProperty("uuid", r.playerId());
                    e.addProperty("name", r.playerName());
                    requests.add(e);
                }
            }
            o.addProperty("locked", s.locked());
        }
        o.add("actions", actions);
        o.add("requests", requests);
        return o;
    }

    /** Машина своей стороны рядом с игроком (или под ним); иначе null. */
    static Entity vehicle(ServerPlayer p, int entityId) {
        Entity v = p.serverLevel().getEntity(entityId);
        if (v == null || !WarfareData.of(v).contains(WarfareKeys.VEHICLE_TEAM)) return null;
        if (p.getVehicle() != v && p.distanceTo(v) > MAX_DIST) return null;
        return v;
    }

    static String team(ServerPlayer p) {
        return p.getTeam() == null ? "" : p.getTeam().getName().toUpperCase(Locale.ROOT);
    }

    static VehicleActions.State state(ServerPlayer p, Entity v) {
        WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
        String team = team(p);
        String vTeam = WarfareData.of(v).getString(WarfareKeys.VEHICLE_TEAM);
        int claim = WarfareData.of(v).getInt(WarfareKeys.VEHICLE_CLAIM_SQUAD);
        String claimTeam = WarfareData.of(v).getString(WarfareKeys.VEHICLE_CLAIM_TEAM);
        boolean alive = claim != 0
                && data.squadRegistry.squads.stream()
                        .anyMatch(s -> s.id == claim && s.team.equalsIgnoreCase(claimTeam));
        int mySquad = WarfareData.of(p).getInt(WarfareKeys.SQUAD_ID);
        boolean leader = WarfareData.of(p).getBoolean(WarfareKeys.IS_SQUAD_LEADER);
        boolean fresh = WarfareData.of(v).getBoolean(WarfareKeys.FRESH_VEHICLE)
                && PacketVehicleDriveRequest.needsLeaderPermission(v);
        boolean pending = leader && !pendingForLeader(p, v).isEmpty();
        return new VehicleActions.State(
                !team.isEmpty() && team.equalsIgnoreCase(vTeam),
                leader,
                // id отрядов уникальны только внутри команды: отряд чужой команды не «мой»
                claimTeam.isEmpty() || claimTeam.equalsIgnoreCase(team) ? mySquad : -1,
                claim,
                alive,
                WarfareData.of(v).getBoolean(WarfareKeys.VEHICLE_LOCKED),
                fresh,
                pending);
    }

    /** Запросы на машину от игроков отряда этого лидера. */
    static java.util.List<DriveRequests.Request> pendingForLeader(ServerPlayer leader, Entity v) {
        int squad = WarfareData.of(leader).getInt(WarfareKeys.SQUAD_ID);
        return REQUESTS.pending(v.getStringUUID(), GameClock.realMillis()).stream()
                .filter(r -> {
                    ServerPlayer req = leader.server.getPlayerList().getPlayer(UUID.fromString(r.playerId()));
                    return req != null
                            && team(req).equals(team(leader))
                            && WarfareData.of(req).getInt(WarfareKeys.SQUAD_ID) == squad;
                })
                .toList();
    }

    /** Запрос отправлен (радиалкой или ссылкой в чате) — лидер увидит его в радиалке. */
    public static void noteRequest(Entity vehicle, ServerPlayer requester) {
        REQUESTS.add(
                vehicle.getStringUUID(),
                requester.getStringUUID(),
                requester.getGameProfile().getName(),
                GameClock.realMillis());
    }

    /** Ответ дан (радиалкой или ссылкой в чате). */
    public static void noteAnswer(Entity vehicle, UUID requester) {
        REQUESTS.remove(vehicle.getStringUUID(), requester.toString());
    }

    public static void forget(ServerPlayer p) {
        REQUESTS.forgetPlayer(p.getStringUUID());
    }

    static void execute(ServerPlayer p, int entityId, Action a, String target) {
        Entity v = vehicle(p, entityId);
        if (v == null) {
            PlayerFeedback.deny(p, "pwpwarfare.vehicle_menu.error.far");
            return;
        }
        VehicleActions.State s = state(p, v);
        if (!VehicleActions.allowed(s, a)) {
            PlayerFeedback.deny(p, "pwpwarfare.vehicle_menu.error.not_allowed");
            return;
        }
        WarfareData d = WarfareData.of(v);
        switch (a) {
            case CLAIM -> {
                d.putInt(WarfareKeys.VEHICLE_CLAIM_SQUAD, WarfareData.of(p).getInt(WarfareKeys.SQUAD_ID));
                d.putString(WarfareKeys.VEHICLE_CLAIM_TEAM, team(p));
                PlayerFeedback.info(p, "pwpwarfare.vehicle_menu.claimed");
            }
            case RELEASE -> {
                d.remove(WarfareKeys.VEHICLE_CLAIM_SQUAD);
                d.remove(WarfareKeys.VEHICLE_CLAIM_TEAM);
                d.remove(WarfareKeys.VEHICLE_LOCKED);
                PlayerFeedback.info(p, "pwpwarfare.vehicle_menu.released");
            }
            case LOCK -> {
                d.putBoolean(WarfareKeys.VEHICLE_LOCKED, true);
                PlayerFeedback.info(p, "pwpwarfare.vehicle_menu.locked");
            }
            case UNLOCK -> {
                d.remove(WarfareKeys.VEHICLE_LOCKED);
                PlayerFeedback.info(p, "pwpwarfare.vehicle_menu.unlocked");
            }
            case REQUEST_DRIVE -> PacketVehicleDriveRequest.handleRequest(p, entityId);
            case APPROVE, DENY -> {
                UUID who = parseUuid(target);
                boolean known = who != null
                        && pendingForLeader(p, v).stream()
                                .anyMatch(r -> r.playerId().equals(who.toString()));
                if (!known) {
                    PlayerFeedback.deny(p, "pwpwarfare.vehicle_menu.error.no_request");
                    return;
                }
                PacketVehicleDriveAnswer.handleAnswer(p, a == Action.APPROVE, entityId, who);
            }
        }
    }

    private static UUID parseUuid(String s) {
        try {
            return s == null || s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** C2S: действие над машиной. Только id машины, действие и (для ответа) UUID просившего. */
    public record Act(int entityId, String action, String target) implements Net.C2S {
        public static void encode(Act m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId);
            b.writeUtf(m.action, 16);
            b.writeUtf(m.target, 36);
        }

        public static Act decode(FriendlyByteBuf b) {
            return new Act(b.readVarInt(), b.readUtf(16), b.readUtf(36));
        }

        @Override
        public String validate() {
            if (Action.parse(action) == null) return "action: " + action;
            return Validate.first(
                    Validate.range("entityId", entityId, 0, Integer.MAX_VALUE),
                    Validate.optionalText("target", target, 36));
        }

        @Override
        public void handle(ServerPlayer sender) {
            execute(sender, entityId, Action.parse(action), target);
        }
    }

    /** Протокол 10 — в самый конец канала (после способностей командира). */
    public static int register(net.minecraftforge.network.simple.SimpleChannel ch, int id) {
        return new Net.Registrar(ch, id)
                .c2s(Act.class, Act::encode, Act::decode)
                .nextId();
    }
}
