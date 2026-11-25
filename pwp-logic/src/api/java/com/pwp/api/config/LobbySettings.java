package com.pwp.api.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;

/**
 * Настройки лобби-сервера, которые админ меняет из панели/команд: автостарт голосования, лимит
 * одновременных матчей, минимум игроков для старта. Секция lobby файла {@code config/pwp-server.json} (см. ServerSettings)
 * (JsonObject вручную, НЕ Gson-POJO — переживает обфускацию). Неизвестные ключи не нужны: файл
 * пишет только лобби. Значения вне диапазона прижимаются, битые — дефолт + предупреждение.
 */
public record LobbySettings(boolean autostart, int maxMatches, int minPlayers) {
    public static final int MAX_MATCHES_MIN = 1;
    public static final int MAX_MATCHES_MAX = 16;
    public static final int MIN_PLAYERS_MIN = 1;
    public static final int MIN_PLAYERS_MAX = 100;

    /** Прежнее поведение (было зашито в коде): автостарт вкл, 2 матча, 1 игрок. */
    public static final LobbySettings DEFAULT = new LobbySettings(true, 2, 1);

    public LobbySettings {
        maxMatches = clamp(maxMatches, MAX_MATCHES_MIN, MAX_MATCHES_MAX);
        minPlayers = clamp(minPlayers, MIN_PLAYERS_MIN, MIN_PLAYERS_MAX);
    }

    public LobbySettings withAutostart(boolean v) {
        return new LobbySettings(v, maxMatches, minPlayers);
    }

    public LobbySettings withMaxMatches(int v) {
        return new LobbySettings(autostart, v, minPlayers);
    }

    public LobbySettings withMinPlayers(int v) {
        return new LobbySettings(autostart, maxMatches, v);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("autostart", autostart);
        o.addProperty("maxMatches", maxMatches);
        o.addProperty("minPlayers", minPlayers);
        return o;
    }

    /** Разбор файла; null — дефолт целиком. Замечания (битые поля) добавляются в {@code warnings}. */
    public static LobbySettings fromJson(JsonObject o, List<String> warnings) {
        if (o == null) return DEFAULT;
        boolean auto = bool(o, "autostart", DEFAULT.autostart, warnings);
        int max = integer(o, "maxMatches", DEFAULT.maxMatches, warnings);
        int min = integer(o, "minPlayers", DEFAULT.minPlayers, warnings);
        return new LobbySettings(auto, max, min);
    }

    private static boolean bool(JsonObject o, String key, boolean def, List<String> warnings) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean();
        warnings.add(key + ": expected true/false, got " + e);
        return def;
    }

    private static int integer(JsonObject o, String key, int def, List<String> warnings) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isNumber()) {
                double d = p.getAsDouble();
                if (d == Math.rint(d) && !Double.isInfinite(d))
                    return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, d));
            }
        }
        warnings.add(key + ": expected integer, got " + e);
        return def;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
