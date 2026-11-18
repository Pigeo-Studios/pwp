package com.pwp.api.core;

import static com.pwp.api.util.Urls.enc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.pwp.api.util.ServerThread;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Единственный HTTP-клиент PWP к core-service (заменил {@code CoreServerApi} и клиентский
 * {@code CoreAPI}).
 *
 * <p>Все методы асинхронные: возвращают {@link CompletableFuture}, который завершается
 * разобранным JSON-ответом при коде 200/201 и {@code null} при ошибке сети/сервера
 * (ошибка уже залогирована). Future никогда не завершается исключением.
 *
 * <p>Результат на главный поток сервера — через
 * {@link ServerThread#thenOnServer}. Блокирующее ожидание {@link #await} разрешено только
 * в фоновых потоках: на главном потоке сервера оно бросает исключение.
 *
 * <p>TLS — стандартная проверка сертификатов JVM (никакого trust-all).
 * Конфигурируется сервером через {@link #configure}; на клиенте не используется.
 */
public final class CoreApi {
    private static final Logger LOG = LoggerFactory.getLogger(CoreApi.class);
    private static final Gson GSON = new Gson();
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, new DaemonFactory());
    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(TIMEOUT).executor(EXECUTOR).build();

    // 127.0.0.1, не localhost: Java на Windows резолвит localhost в ::1, а core слушает IPv4 loopback
    private static volatile String baseUrl = "http://127.0.0.1:8080";
    private static volatile String apiKey = "";

    private CoreApi() {}

    public static void configure(String url, String key) {
        baseUrl = url;
        apiKey = key;
    }

    /**
     * Блокирующее ожидание результата. Только для фоновых потоков: на главном потоке
     * сервера бросает {@link IllegalStateException} (правило «нет синхронного HTTP на тике»).
     */
    public static <T> T await(CompletableFuture<T> future) {
        if (ServerThread.isServerThread()) {
            throw new IllegalStateException(
                    "CoreApi.await на главном потоке сервера — используйте ServerThread.thenOnServer");
        }
        return future.join();
    }

    // ====== PLAYER ======

    public static CompletableFuture<JsonObject> loadPlayer(String uuid) {
        return post("/api/v1/player/load", map("uuid", uuid));
    }

    public static CompletableFuture<JsonObject> createPlayer(String uuid, String nickname) {
        return post("/api/v1/player/create", map("uuid", uuid, "nickname", nickname));
    }

    public static CompletableFuture<JsonObject> saveStats(String uuid, JsonObject stats) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid);
        body.add("stats", stats);
        return post("/api/v1/player/save", body);
    }

    public static CompletableFuture<JsonObject> getPlayerProfile(String uuid) {
        return get("/api/v1/player/" + enc(uuid));
    }

    // ====== BANS ======

    /** Проверка банов онлайна: сервер передаёт игроков, получает кого кикать. */
    public static CompletableFuture<JsonObject> checkBans(JsonArray players) {
        JsonObject body = new JsonObject();
        body.add("players", players);
        return post("/api/v1/launcher/ban-check", body);
    }

    /** Бан игрока (target — ник или UUID, duration — 30m/1h/7d/perm). */
    public static CompletableFuture<JsonObject> banPlayer(String target, String reason, String duration) {
        return post("/api/v1/launcher/ban", map("target", target, "reason", reason, "duration", duration));
    }

    public static CompletableFuture<JsonObject> unbanPlayer(String target) {
        return post("/api/v1/launcher/unban", map("target", target));
    }

    // ====== CURRENCY / XP / RANKS ======

    public static CompletableFuture<JsonObject> addCurrency(String uuid, long amount, String reason) {
        return post("/api/v1/currency/add", map("uuid", uuid, "amount", amount, "reason", reason));
    }

    public static CompletableFuture<JsonObject> spendCurrency(String uuid, long amount, String itemId) {
        return post("/api/v1/currency/spend", map("uuid", uuid, "amount", amount, "itemId", itemId));
    }

    public static CompletableFuture<JsonObject> addXp(String uuid, long amount, String reason) {
        return post("/api/v1/xp/add", map("uuid", uuid, "amount", amount, "reason", reason));
    }

    public static CompletableFuture<JsonObject> checkRank(String uuid) {
        return post("/api/v1/ranks/check", map("uuid", uuid));
    }

    public static CompletableFuture<JsonObject> getPlayerRanks(String uuid) {
        return get("/api/v1/ranks/player/" + enc(uuid));
    }

    // ====== MATCH / REWARDS ======

    public static CompletableFuture<JsonObject> saveMatch(JsonObject matchData) {
        return post("/api/v1/match/save", matchData);
    }

    public static CompletableFuture<JsonObject> calculateRewards(
            String uuid,
            String team,
            String winner,
            int kills,
            int assists,
            int vehicleKills,
            int captures,
            int revives,
            int headshots,
            int durationMinutes) {
        return post(
                "/api/v1/rewards/calculate",
                map(
                        "uuid",
                        uuid,
                        "team",
                        team,
                        "winner",
                        winner,
                        "kills",
                        kills,
                        "assists",
                        assists,
                        "vehicleKills",
                        vehicleKills,
                        "captures",
                        captures,
                        "revives",
                        revives,
                        "headshots",
                        headshots,
                        "durationMinutes",
                        durationMinutes));
    }

    // ====== COSMETICS / SKINS ======

    /** Выдача предмета. Только для серверной логики наград/кейсов — никогда по запросу клиента. */
    public static CompletableFuture<JsonObject> grantItem(String uuid, String skinId, String source) {
        return post("/api/v1/cosmetics/grant", map("uuid", uuid, "skinId", skinId, "source", source));
    }

    /** Экипировка предмета. core-service сам проверяет, что itemUuid принадлежит игроку. */
    public static CompletableFuture<JsonObject> equipItem(String uuid, String itemUuid, String slotType, String role) {
        return post(
                "/api/v1/cosmetics/equip", map("uuid", uuid, "itemUuid", itemUuid, "slotType", slotType, "role", role));
    }

    public static CompletableFuture<JsonObject> unequipItem(String uuid, String slotType, String role) {
        return post("/api/v1/cosmetics/unequip", map("uuid", uuid, "slotType", slotType, "role", role));
    }

    public static CompletableFuture<JsonObject> getCosmetics(String uuid) {
        return get("/api/v1/cosmetics/" + enc(uuid));
    }

    public static CompletableFuture<JsonObject> getSkins() {
        return get("/api/v1/skins");
    }

    public static CompletableFuture<JsonObject> getSkinsBySlot(String slotType) {
        return get("/api/v1/skins/slot/" + enc(slotType));
    }

    public static CompletableFuture<JsonObject> getSkinsByWeapon(String weaponTag) {
        return get("/api/v1/skins/weapon/" + enc(weaponTag));
    }

    public static CompletableFuture<JsonObject> saveSkin(
            String skinId,
            String name,
            String description,
            String slotType,
            String weaponTag,
            String rarity,
            String modelPath) {
        return post(
                "/api/v1/skins/save",
                map(
                        "skinId",
                        skinId,
                        "name",
                        name,
                        "description",
                        description,
                        "slotType",
                        slotType,
                        "weaponTag",
                        weaponTag,
                        "rarity",
                        rarity,
                        "modelPath",
                        modelPath,
                        "enabled",
                        true));
    }

    public static CompletableFuture<JsonObject> deleteSkin(String skinId) {
        return post("/api/v1/skins/delete", map("skinId", skinId));
    }

    // ====== CASES ======

    public static CompletableFuture<JsonObject> getCases() {
        return get("/api/v1/cases");
    }

    public static CompletableFuture<JsonObject> openCase(String uuid, String caseId) {
        return post("/api/v1/cases/open", map("uuid", uuid, "caseId", caseId));
    }

    // ====== LEADERBOARD ======

    public static CompletableFuture<JsonObject> getLeaderboard(String orderBy, int page, int limit) {
        return get("/api/v1/leaderboard?orderBy=" + enc(orderBy) + "&page=" + page + "&limit=" + limit);
    }

    /** Роль игрока в БД: {@code {data: {uuid, role}}} (user/support/admin/owner). Игрока нет — ошибка. */
    public static CompletableFuture<JsonObject> getPlayerRole(String uuid) {
        return get("/api/v1/player/" + enc(uuid) + "/role");
    }

    public static CompletableFuture<JsonObject> getPlayerRank(String uuid, String orderBy) {
        return get("/api/v1/player/" + enc(uuid) + "/rank?orderBy=" + enc(orderBy));
    }

    // ====== NETWORK ======

    public static CompletableFuture<JsonObject> getMatchPolicy() {
        return get("/api/v1/network/match-policy");
    }

    public static CompletableFuture<JsonObject> setMatchPolicy(boolean canStart, String reason) {
        MatchPolicy.invalidate();
        return post("/api/v1/network/match-policy", map("canStartNewMatch", canStart, "reason", reason));
    }

    /** Статус всех серверов из core-service (heartbeat'ы) — для живого онлайна матчей. */
    public static CompletableFuture<JsonObject> fetchServersStatus() {
        return get("/api/v1/network/status");
    }

    /** Heartbeat сервера (лобби или матч). Тело собирает {@link Heartbeat}. */
    public static CompletableFuture<JsonObject> sendHeartbeat(Heartbeat heartbeat) {
        return post("/api/v1/network/heartbeat", heartbeat.toJson());
    }

    // ====== KITS ======

    public static CompletableFuture<JsonObject> getFactions() {
        return get("/api/v1/kits/factions");
    }

    public static CompletableFuture<JsonObject> getFactionKits(String faction) {
        return get("/api/v1/kits/faction/" + enc(faction.toLowerCase()));
    }

    public static CompletableFuture<JsonObject> getFactionKit(String faction, String kitName) {
        return get("/api/v1/kits/faction/" + enc(faction.toLowerCase()) + "/" + enc(kitName));
    }

    public static CompletableFuture<JsonObject> saveFactionKit(String faction, String kitName, JsonObject kitData) {
        return put("/api/v1/kits/faction/" + enc(faction.toLowerCase()) + "/" + enc(kitName), kitData);
    }

    public static CompletableFuture<JsonObject> bulkSaveFactionKits(String faction, JsonArray kits) {
        return post("/api/v1/kits/faction/" + enc(faction.toLowerCase()) + "/bulk", kits);
    }

    // ====== FACTION VEHICLES ======

    public static CompletableFuture<JsonObject> getFactionVehicles(String faction) {
        return get("/api/v1/factions/" + enc(faction.toLowerCase()) + "/vehicles");
    }

    public static CompletableFuture<JsonObject> getFactionVehicle(String faction, String vehicleName) {
        return get("/api/v1/factions/" + enc(faction.toLowerCase()) + "/vehicles/" + enc(vehicleName));
    }

    public static CompletableFuture<JsonObject> saveFactionVehicle(
            String faction, String vehicleName, JsonObject data) {
        return put("/api/v1/factions/" + enc(faction.toLowerCase()) + "/vehicles/" + enc(vehicleName), data);
    }

    public static CompletableFuture<JsonObject> deleteFactionVehicle(String faction, String vehicleName) {
        return send("DELETE", "/api/v1/factions/" + enc(faction.toLowerCase()) + "/vehicles/" + enc(vehicleName), null);
    }

    // ====== MAP LAYOUTS (этап 3: редактор карты) ======

    /** Версии раскладки карты без JSON: {@code {data: [{id, mapName, authorUuid, authorName, createdAt, comment}]}}. */
    public static CompletableFuture<JsonObject> listMapLayouts(String mapName) {
        return get("/api/v1/maps/" + enc(mapName) + "/layouts");
    }

    /** Последняя версия с JSON: {@code {data: {id, …, json}}}; версий нет — {@code {data: null}}. */
    public static CompletableFuture<JsonObject> getLatestMapLayout(String mapName) {
        return get("/api/v1/maps/" + enc(mapName) + "/layouts/latest");
    }

    public static CompletableFuture<JsonObject> getMapLayout(long id) {
        return get("/api/v1/maps/layouts/" + id);
    }

    /** Новая версия; {@code json} — раскладка в формате MapLayoutCodec (строкой). Ответ — сохранённая версия. */
    public static CompletableFuture<JsonObject> saveMapLayout(
            String mapName, String json, String authorUuid, String authorName, String comment) {
        return post(
                "/api/v1/maps/" + enc(mapName) + "/layouts",
                map("json", json, "authorUuid", authorUuid, "authorName", authorName, "comment", comment));
    }

    /** Откат = новая версия-копия {@code id}. Ответ — новая версия с JSON. */
    public static CompletableFuture<JsonObject> rollbackMapLayout(long id, String authorUuid, String authorName) {
        return post(
                "/api/v1/maps/layouts/" + id + "/rollback", map("authorUuid", authorUuid, "authorName", authorName));
    }

    // ====== VOICE MUTE ======

    public static CompletableFuture<JsonObject> voiceMute(
            String uuid, String mutedByUuid, String mutedByNickname, String reason, int durationMinutes) {
        return post(
                "/api/v1/voicemute",
                map(
                        "uuid",
                        uuid,
                        "mutedByUuid",
                        mutedByUuid,
                        "mutedByNickname",
                        mutedByNickname,
                        "reason",
                        reason != null ? reason : "",
                        "durationMinutes",
                        durationMinutes));
    }

    public static CompletableFuture<JsonObject> voiceUnmute(String uuid) {
        return post("/api/v1/voiceunmute", map("uuid", uuid));
    }

    public static CompletableFuture<JsonObject> getVoiceMute(String uuid) {
        return get("/api/v1/voicemute/" + enc(uuid));
    }

    public static CompletableFuture<JsonObject> getVoiceMutes() {
        return get("/api/v1/voicemutes");
    }

    // ====== ПОМОЩНИКИ РАЗБОРА ОТВЕТА ======

    /** true, если ответ core-service успешный ({@code success: true}). */
    public static boolean isSuccess(JsonObject response) {
        return response != null
                && response.has("success")
                && response.get("success").getAsBoolean();
    }

    /** Объект {@code data} ответа или null. */
    public static JsonObject dataObject(JsonObject response) {
        if (response == null || !response.has("data") || !response.get("data").isJsonObject()) return null;
        return response.getAsJsonObject("data");
    }

    /** Массив {@code data} ответа или null. */
    public static JsonArray dataArray(JsonObject response) {
        if (response == null || !response.has("data") || !response.get("data").isJsonArray()) return null;
        return response.getAsJsonArray("data");
    }

    // ====== HTTP ======

    private static CompletableFuture<JsonObject> get(String path) {
        return send("GET", path, null);
    }

    private static CompletableFuture<JsonObject> post(String path, Object body) {
        return send("POST", path, body);
    }

    private static CompletableFuture<JsonObject> put(String path, Object body) {
        return send("PUT", path, body);
    }

    private static CompletableFuture<JsonObject> send(String method, String path, Object body) {
        HttpRequest request;
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8);
            request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method(method, publisher)
                    .build();
        } catch (IllegalArgumentException e) {
            LOG.warn("Core API: некорректный запрос {} {}", method, path, e);
            return CompletableFuture.completedFuture(null);
        }
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> parse(method, path, response))
                .exceptionally(error -> {
                    Throwable cause =
                            error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                    LOG.warn("Core API {} {} не выполнен: {}", method, path, cause.toString());
                    return null;
                });
    }

    private static JsonObject parse(String method, String path, HttpResponse<String> response) {
        int code = response.statusCode();
        if (code != 200 && code != 201) {
            LOG.warn("Core API {} {} вернул {}: {}", method, path, code, response.body());
            return null;
        }
        try {
            return GSON.fromJson(response.body(), JsonObject.class);
        } catch (JsonParseException e) {
            LOG.warn("Core API {} {}: ответ не JSON-объект", method, path, e);
            return null;
        }
    }

    static JsonObject map(Object... keysValues) {
        JsonObject obj = new JsonObject();
        for (int i = 0; i < keysValues.length; i += 2) {
            String key = (String) keysValues[i];
            Object val = keysValues[i + 1];
            if (val instanceof String s) obj.addProperty(key, s);
            else if (val instanceof Number n) obj.addProperty(key, n);
            else if (val instanceof Boolean b) obj.addProperty(key, b);
        }
        return obj;
    }

    private static final class DaemonFactory implements java.util.concurrent.ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "pwp-core-api-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
