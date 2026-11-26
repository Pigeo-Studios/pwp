package com.pigeostudios.pwp.warfare.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.data.FactionVehicleData;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.vehicles.Division;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.core.CoreApi;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.ServerThread;
import com.pwp.coreclient.network.data.AdminAction;
import com.pwp.coreclient.network.data.DataRequests;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Редактор дивизий в F6 (этап 6d ROADMAP; раньше дивизии правились только SQL/API). Работает с фракциями
 * текущего матча: тип данных {@code adminDivisions} — пул техники команды и её дивизии; {@link Save} — записать
 * дивизию в core ({@code PUT /api/v1/factions/{f}/divisions/{id}}), {@link Delete} — удалить. После записи
 * данные фракций перечитываются (как «Перезагрузить данные фракций»), и дивизия сразу доступна в «Матч».
 * Право — ADMIN, проверяет сервер.
 */
public final class DivisionAdmin {
    public static final String DATA_TYPE = "adminDivisions";
    public static final String SAVE = "division.save";
    public static final String DELETE = "division.delete";
    static final String ERR_NO_FACTION = "pwpwarfare.admin.division.error.no_faction";
    static final String ERR_BAD_SPEC = "pwpwarfare.admin.division.error.bad_spec";
    static final String ERR_CORE = "pwpwarfare.admin.division.error.core";
    static final String SAVED = "pwpwarfare.admin.division.saved";
    static final String DELETED = "pwpwarfare.admin.division.deleted";
    /** Сколько машин одной строки максимум (больше слотов в мейне всё равно не бывает). */
    public static final int MAX_PER_VEHICLE = com.pigeostudios.pwp.warfare.vehicles.DivisionSpec.MAX_PER_VEHICLE;

    private DivisionAdmin() {}

    public static void registerData() {
        DataRequests.register(DATA_TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                return DataRequests.empty();
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                JsonObject o = new JsonObject();
                if (!PwpPermissions.has(p, AccessLevel.ADMIN)) {
                    o.addProperty("error", AdminAction.ERR_NO_PERMISSION);
                    return o.toString();
                }
                WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
                o.add("BLUE", team(data, true));
                o.add("RED", team(data, false));
                return o.toString();
            }
        });
    }

    /** {faction, divisions: [Division.toJson], pool: [{name, display, role}]} команды. */
    static JsonObject team(WarfareWorldData data, boolean blue) {
        JsonObject t = new JsonObject();
        String faction = blue ? data.match.blueFaction : data.match.redFaction;
        t.addProperty("faction", faction == null ? "" : faction);
        JsonArray divs = new JsonArray();
        for (Division d : blue ? data.vehicles.blueDivisions : data.vehicles.redDivisions) divs.add(d.toJson());
        t.add("divisions", divs);
        JsonArray pool = new JsonArray();
        Map<String, FactionVehicleData> sorted =
                new TreeMap<>(blue ? data.vehicles.blueFactionVehicles : data.vehicles.redFactionVehicles);
        sorted.forEach((name, v) -> {
            JsonObject e = new JsonObject();
            e.addProperty("name", name);
            e.addProperty("display", v.displayName == null || v.displayName.isEmpty() ? name : v.displayName);
            e.addProperty("role", v.category == null ? "" : v.category.toUpperCase(java.util.Locale.ROOT));
            pool.add(e);
        });
        t.add("pool", pool);
        return t;
    }

    /** Пул команды как «машина → категория» для {@link com.pigeostudios.pwp.warfare.vehicles.DivisionSpec}. */
    static Map<String, String> poolRoles(Map<String, FactionVehicleData> pool) {
        Map<String, String> out = new java.util.HashMap<>();
        pool.forEach((name, v) -> out.put(name, v.category == null ? "" : v.category));
        return out;
    }

    private static String faction(ServerLevel level, String team) {
        WarfareWorldData data = WarfareWorldData.get(level);
        String f = "BLUE".equals(team) ? data.match.blueFaction : data.match.redFaction;
        return f == null || f.isBlank() || "none".equalsIgnoreCase(f) ? null : f;
    }

    /** Записать дивизию: id латиницей, имя, состав «машина:кол-во;…». */
    public record Save(String team, String divisionId, String displayName, String spec) implements Net.C2S {
        public static void encode(Save m, FriendlyByteBuf b) {
            b.writeUtf(m.team, 8);
            b.writeUtf(m.divisionId, 64);
            b.writeUtf(m.displayName, 64);
            b.writeUtf(m.spec, 2000);
        }

        public static Save decode(FriendlyByteBuf b) {
            return new Save(b.readUtf(8), b.readUtf(64), b.readUtf(64), b.readUtf(2000));
        }

        @Override
        public String validate() {
            return Validate.first(
                    Validate.oneOf("team", team, "BLUE", "RED"),
                    Validate.id("divisionId", divisionId, 64),
                    Validate.text("displayName", displayName, 64),
                    Validate.optionalText("spec", spec, 2000));
        }

        @Override
        public void handle(ServerPlayer s) {
            AdminAction.runLocal(s, SAVE, AccessLevel.ADMIN, team + " " + divisionId + " " + spec, () -> {
                ServerLevel level = s.serverLevel();
                String faction = faction(level, team);
                if (faction == null) return ERR_NO_FACTION;
                WarfareWorldData data = WarfareWorldData.get(level);
                var pool = "BLUE".equals(team) ? data.vehicles.blueFactionVehicles : data.vehicles.redFactionVehicles;
                List<Division.Entry> entries =
                        com.pigeostudios.pwp.warfare.vehicles.DivisionSpec.parse(spec, poolRoles(pool));
                if (entries == null) return ERR_BAD_SPEC;
                // режимы и размеры карт прежней дивизии сохраняем (редактор меняет только состав и имя)
                String modes = "";
                String sizes = "";
                for (Division d : "BLUE".equals(team) ? data.vehicles.blueDivisions : data.vehicles.redDivisions) {
                    if (d.id().equals(divisionId)) {
                        JsonObject old = d.toJson();
                        modes = old.has("modes") ? old.get("modes").getAsString() : "";
                        sizes = old.has("mapSizes") ? old.get("mapSizes").getAsString() : "";
                    }
                }
                JsonObject body = new JsonObject();
                body.addProperty("displayName", displayName.trim());
                body.addProperty("modes", modes);
                body.addProperty("mapSizes", sizes);
                body.add("vehicles", Division.vehiclesJson(entries));
                ServerThread.thenForPlayer(CoreApi.saveFactionDivision(faction, divisionId, body), s, (p, resp) -> {
                    if (!CoreApi.isSuccess(resp)) {
                        PlayerFeedback.deny(p, ERR_CORE);
                        return;
                    }
                    PlayerFeedback.info(p, SAVED, displayName.trim());
                    MatchAdmin.reloadFactionData(p.serverLevel());
                });
                return null;
            });
        }
    }

    public record Delete(String team, String divisionId) implements Net.C2S {
        public static void encode(Delete m, FriendlyByteBuf b) {
            b.writeUtf(m.team, 8);
            b.writeUtf(m.divisionId, 64);
        }

        public static Delete decode(FriendlyByteBuf b) {
            return new Delete(b.readUtf(8), b.readUtf(64));
        }

        @Override
        public String validate() {
            return Validate.first(
                    Validate.oneOf("team", team, "BLUE", "RED"), Validate.id("divisionId", divisionId, 64));
        }

        @Override
        public void handle(ServerPlayer s) {
            AdminAction.runLocal(s, DELETE, AccessLevel.ADMIN, team + " " + divisionId, () -> {
                String faction = faction(s.serverLevel(), team);
                if (faction == null) return ERR_NO_FACTION;
                ServerThread.thenForPlayer(CoreApi.deleteFactionDivision(faction, divisionId), s, (p, resp) -> {
                    if (!CoreApi.isSuccess(resp)) {
                        PlayerFeedback.deny(p, ERR_CORE);
                        return;
                    }
                    PlayerFeedback.info(p, DELETED, divisionId);
                    MatchAdmin.reloadFactionData(p.serverLevel());
                });
                return null;
            });
        }
    }

    /** Протокол 9 (26.11.2026) — в самый конец канала, после пакета лимитов. */
    public static int register(net.minecraftforge.network.simple.SimpleChannel ch, int id) {
        return new Net.Registrar(ch, id)
                .c2s(Save.class, Save::encode, Save::decode)
                .c2s(Delete.class, Delete::encode, Delete::decode)
                .nextId();
    }
}
