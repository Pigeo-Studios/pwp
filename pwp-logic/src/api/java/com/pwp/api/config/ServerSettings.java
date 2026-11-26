package com.pwp.api.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Locale;

/**
 * Единый конфиг сервера PWP — {@code config/pwp-server.json} (с 25.11.2026, этап 3 ROADMAP). Одинаковый файл
 * на лобби и матч-серверах; всё, что раньше было зашито в код или жило в {@code config/pwp_lobby.json}.
 *
 * <pre>
 * {
 *   "role": "auto",                       // auto | lobby | match; auto — папка match_* = матч, иначе лобби
 *   "lobby": {"autostart": true, "maxMatches": 2, "minPlayers": 1, "prewarm": false},
 *   "matchServers": {"template": "../PWP-Server/match_template", "basePort": 25566},
 *   "mapsDir": "",                        // пусто — авто: ../maps, ../PWP-Server/maps, maps
 *   "community": {...}                    // правила публичного сервера, см. CommunitySettings
 * }
 * </pre>
 *
 * Игровые настройки (баланс, кулдауны) остаются в Forge-TOML ({@code pwpwarfare-common.toml} и
 * {@code serverconfig/*.toml}): у них есть горячая перезагрузка и синхронизация клиентам.
 */
public record ServerSettings(
        Role role,
        LobbySettings lobby,
        boolean prewarm,
        String matchTemplate,
        int basePort,
        String mapsDir,
        CommunitySettings community) {

    public enum Role {
        AUTO,
        LOBBY,
        MATCH;

        static Role parse(String s) {
            if (s == null) return null;
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static final int PORT_MIN = 1024;
    public static final int PORT_MAX = 65000;

    /** Значения по умолчанию = прежнее поведение (шаблон и порты были зашиты в ServerManager). */
    public static final ServerSettings DEFAULT = new ServerSettings(
            Role.AUTO,
            LobbySettings.DEFAULT,
            false,
            "../PWP-Server/match_template",
            25566,
            "",
            CommunitySettings.DEFAULT);

    public ServerSettings {
        if (role == null) role = Role.AUTO;
        if (lobby == null) lobby = LobbySettings.DEFAULT;
        if (matchTemplate == null || matchTemplate.isBlank()) matchTemplate = DEFAULT_TEMPLATE;
        if (mapsDir == null) mapsDir = "";
        if (community == null) community = CommunitySettings.DEFAULT;
        basePort = Math.max(PORT_MIN, Math.min(PORT_MAX, basePort));
    }

    private static final String DEFAULT_TEMPLATE = "../PWP-Server/match_template";

    public ServerSettings withLobby(LobbySettings v) {
        return new ServerSettings(role, v, prewarm, matchTemplate, basePort, mapsDir, community);
    }

    public ServerSettings withPrewarm(boolean v) {
        return new ServerSettings(role, lobby, v, matchTemplate, basePort, mapsDir, community);
    }

    /**
     * Роль с учётом авто-определения: {@code dirName} — имя папки сервера (матч-серверы лобби поднимает в
     * {@code match_NN}).
     */
    public boolean isMatchServer(String dirName) {
        return switch (role) {
            case MATCH -> true;
            case LOBBY -> false;
            case AUTO -> dirName != null && dirName.startsWith("match_");
        };
    }

    /** JSON поверх {@code base} (неизвестные ключи сохраняются); base может быть null. */
    public JsonObject toJson(JsonObject base) {
        JsonObject o = base == null ? new JsonObject() : base.deepCopy();
        o.addProperty("role", role.name().toLowerCase(Locale.ROOT));
        JsonObject l = lobby.toJson();
        l.addProperty("prewarm", prewarm);
        o.add("lobby", l);
        JsonObject m = o.has("matchServers") && o.get("matchServers").isJsonObject()
                ? o.getAsJsonObject("matchServers")
                : new JsonObject();
        m.addProperty("template", matchTemplate);
        m.addProperty("basePort", basePort);
        o.add("matchServers", m);
        o.addProperty("mapsDir", mapsDir);
        o.add("community", community.toJson());
        return o;
    }

    /** Разбор файла; битые поля — дефолт + запись в {@code warnings}. null — дефолт целиком. */
    public static ServerSettings fromJson(JsonObject o, List<String> warnings) {
        if (o == null) return DEFAULT;
        Role role = DEFAULT.role;
        JsonElement r = o.get("role");
        if (r != null) {
            Role parsed = r.isJsonPrimitive() ? Role.parse(r.getAsString()) : null;
            if (parsed == null) warnings.add("role: expected auto|lobby|match, got " + r);
            else role = parsed;
        }
        LobbySettings lobby = LobbySettings.DEFAULT;
        boolean prewarm = DEFAULT.prewarm;
        JsonElement le = o.get("lobby");
        if (le != null) {
            if (le.isJsonObject()) {
                JsonObject lo = le.getAsJsonObject();
                lobby = LobbySettings.fromJson(lo, warnings);
                prewarm = bool(lo, "prewarm", DEFAULT.prewarm, "lobby.", warnings);
            } else {
                warnings.add("lobby: expected object, got " + le);
            }
        }
        String template = DEFAULT.matchTemplate;
        int basePort = DEFAULT.basePort;
        JsonElement me = o.get("matchServers");
        if (me != null) {
            if (me.isJsonObject()) {
                JsonObject mo = me.getAsJsonObject();
                template = str(mo, "template", template, "matchServers.", warnings);
                basePort = integer(mo, "basePort", basePort, "matchServers.", warnings);
            } else {
                warnings.add("matchServers: expected object, got " + me);
            }
        }
        String maps = str(o, "mapsDir", DEFAULT.mapsDir, "", warnings);
        CommunitySettings community = CommunitySettings.DEFAULT;
        JsonElement ce = o.get("community");
        if (ce != null) {
            if (ce.isJsonObject()) community = CommunitySettings.fromJson(ce.getAsJsonObject(), warnings);
            else warnings.add("community: expected object, got " + ce);
        }
        return new ServerSettings(role, lobby, prewarm, template, basePort, maps, community);
    }

    /** Миграция: содержимое старого {@code config/pwp_lobby.json} (autostart/maxMatches/minPlayers/prewarm). */
    public static ServerSettings fromLegacyLobby(JsonObject legacy, List<String> warnings) {
        if (legacy == null) return DEFAULT;
        return DEFAULT.withLobby(LobbySettings.fromJson(legacy, warnings))
                .withPrewarm(bool(legacy, "prewarm", false, "", warnings));
    }

    private static boolean bool(JsonObject o, String key, boolean def, String prefix, List<String> warnings) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean();
        warnings.add(prefix + key + ": expected true/false, got " + e);
        return def;
    }

    private static String str(JsonObject o, String key, String def, String prefix, List<String> warnings) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) return e.getAsString();
        warnings.add(prefix + key + ": expected string, got " + e);
        return def;
    }

    private static int integer(JsonObject o, String key, int def, String prefix, List<String> warnings) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isNumber()) {
                double d = p.getAsDouble();
                if (d == Math.rint(d) && !Double.isInfinite(d)) return (int) d;
            }
        }
        warnings.add(prefix + key + ": expected integer, got " + e);
        return def;
    }
}
