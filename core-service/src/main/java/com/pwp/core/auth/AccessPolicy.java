package com.pwp.core.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Что может сессия игрока (HMAC лаунчера). Раньше любая валидная сессия проходила ко ВСЕМ
 * эндпоинтам: игрок мог начислить себе валюту/XP/скины, сохранить матч, забанить, отключить
 * чужую 2FA — достаточно подставить uuid.
 *
 * <p>Правила: 1) серверные эндпоинты ({@link #SERVER_ONLY}) — только по API-ключу (игровые
 * серверы, боты); 2) изменяющий запрос (POST/PUT/DELETE) сессии может касаться только её
 * владельца: каждый uuid в пути и в полях тела {@link #UUID_FIELDS} должен совпадать с uuid сессии.
 * Чтение (GET) не ограничиваем — профили, лидерборд, скины других игроков лаунчер показывает.
 */
public final class AccessPolicy {
    private AccessPolicy() {}

    /** "МЕТОД регэксп-пути": изменения экономики/матча/модерации/контента — только сервер. */
    static final List<Pattern> SERVER_ONLY = List.of(
            p("POST /api/v1/currency/add"),
            p("POST /api/v1/xp/add"),
            p("POST /api/v1/cosmetics/grant"),
            p("POST /api/v2/skins/grant"),
            p("POST /api/v1/player/(ban|save|create)"),
            p("POST /api/v1/match/save"),
            p("POST /api/v1/launcher/(ban|unban|warn)"),
            p("POST /api/v1/(voicemute|voiceunmute)"),
            p("POST /api/v1/rewards/config/update"),
            p("POST /api/v1/skins/(save|delete)"),
            p("POST /api/v1/cases/save"),
            p("POST /api/v1/kits/faction/[^/]+/bulk"),
            p("(PUT|DELETE) /api/v1/kits/faction/[^/]+/[^/]+"),
            p("(PUT|DELETE) /api/v1/factions/[^/]+/vehicles/[^/]+"),
            p("POST /api/v1/network/(heartbeat|bot-heartbeat|match-policy)"),
            p("POST /api/v1/maps/[^/]+/layouts"),
            p("POST /api/v1/maps/layouts/[^/]+/rollback"));

    static final List<String> UUID_FIELDS = List.of("uuid", "playerUuid", "player_uuid", "targetUuid", "target_uuid");

    private static final Pattern UUID_IN_PATH =
            Pattern.compile("(?<![0-9a-fA-F-])([0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12})(?![0-9a-fA-F-])");

    private static Pattern p(String rule) {
        int sp = rule.indexOf(' ');
        return Pattern.compile("^(" + rule.substring(0, sp) + ") " + rule.substring(sp + 1) + "/?$");
    }

    public static boolean serverOnly(String method, String path) {
        String key = method.toUpperCase(Locale.ROOT) + " " + path;
        for (Pattern rule : SERVER_ONLY) {
            if (rule.matcher(key).matches()) return true;
        }
        return false;
    }

    /**
     * null — разрешено; иначе причина отказа. {@code sessionUuid} — владелец сессии (null —
     * сессия без игрока: тогда изменяющие запросы с чужим uuid тоже запрещены).
     */
    public static String denyReason(String method, String path, String body, String sessionUuid) {
        String m = method.toUpperCase(Locale.ROOT);
        if (serverOnly(m, path)) return "server only";
        if (m.equals("GET") || m.equals("HEAD") || m.equals("OPTIONS")) return null;
        String owner = norm(sessionUuid);
        Matcher pm = UUID_IN_PATH.matcher(path);
        while (pm.find()) {
            if (!norm(pm.group(1)).equals(owner)) return "foreign uuid";
        }
        if (body != null && !body.isBlank() && body.trim().startsWith("{")) {
            JsonObject json;
            try {
                json = JsonParser.parseString(body).getAsJsonObject();
            } catch (RuntimeException e) {
                return null; // не JSON — хэндлер сам ответит ошибкой разбора
            }
            for (String f : UUID_FIELDS) {
                JsonElement v = json.get(f);
                if (v != null && v.isJsonPrimitive() && !v.getAsString().isBlank()
                        && !norm(v.getAsString()).equals(owner)) {
                    return "foreign uuid";
                }
            }
        }
        return null;
    }

    /** uuid без дефисов в нижнем регистре; null → "" (не совпадёт ни с одним uuid). */
    static String norm(String uuid) {
        return uuid == null ? "" : uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }
}
