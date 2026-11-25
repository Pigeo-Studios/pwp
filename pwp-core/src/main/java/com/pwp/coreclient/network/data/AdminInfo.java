package com.pwp.coreclient.network.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Тип {@code adminInfo}: уровень доступа игрока + какие разделы админ-панели есть на этом сервере. Уровень
 * клиенту приходит и пакетом {@code AccessLevelPacket}; это лишь скрытие UI — каждое действие сервер
 * проверяет заново ({@link PwpPermissions#has}).
 *
 * <p>Разделы ({@code features}) объявляют серверные моды: warfare — {@code match}/{@code map}, лобби —
 * {@code lobby} (только на лобби-сервере), core-client — {@code moderation} (всегда).
 */
public final class AdminInfo {
    public static final String TYPE = "adminInfo";
    public static final String FEATURE_LOBBY = "lobby";
    public static final String FEATURE_MATCH = "match";
    public static final String FEATURE_MAP = "map";
    public static final String FEATURE_MODERATION = "moderation";

    private static final Set<String> FEATURES = ConcurrentHashMap.newKeySet();

    static {
        FEATURES.add(FEATURE_MODERATION);
    }

    private AdminInfo() {}

    /** Объявить раздел панели на этом сервере (вызов из commonSetup серверного мода). */
    public static void addFeature(String feature) {
        FEATURES.add(feature);
    }

    static final DataRequests.Handler HANDLER = new DataRequests.Handler() {
        @Override
        public CompletableFuture<JsonObject> fetch(ServerPlayer player, JsonObject params) {
            return DataRequests.empty();
        }

        @Override
        public String toJson(ServerPlayer player, JsonObject params, JsonObject response) {
            return build(PwpPermissions.level(player), FEATURES).toString();
        }
    };

    /** JSON ответа; ниже MODERATOR список разделов не отдаётся. */
    static JsonObject build(AccessLevel level, Set<String> features) {
        JsonObject o = new JsonObject();
        o.addProperty("level", level.name());
        JsonArray arr = new JsonArray();
        if (level.atLeast(AccessLevel.MODERATOR)) new TreeSet<>(features).forEach(arr::add);
        o.add("features", arr);
        return o;
    }
}
