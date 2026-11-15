package com.pwp.api.core;

import com.google.gson.JsonObject;

/**
 * Тело heartbeat'а сервера для core-service. Поля матча необязательны:
 * лобби без активного матча шлёт только server/online.
 */
public final class Heartbeat {
    private final String server;
    private final int online;
    private int port;
    private JsonObject match;

    public Heartbeat(String server, int online) {
        this.server = server;
        this.online = online;
    }

    /** Порт матч-сервера — лобби сверяет по нему матч. */
    public Heartbeat port(int port) {
        this.port = port;
        return this;
    }

    public Heartbeat match(String mapName, String mode, String blueFaction, String redFaction,
            int blueScore, int redScore, String phase, int maxPlayers, long matchStartedAt, int matchPlayers) {
        JsonObject m = new JsonObject();
        m.addProperty("mapName", mapName);
        m.addProperty("mode", nz(mode));
        m.addProperty("blueFaction", nz(blueFaction));
        m.addProperty("redFaction", nz(redFaction));
        m.addProperty("blueScore", blueScore);
        m.addProperty("redScore", redScore);
        m.addProperty("phase", nz(phase));
        m.addProperty("maxPlayers", maxPlayers);
        m.addProperty("matchStartedAt", matchStartedAt);
        m.addProperty("matchPlayers", matchPlayers);
        this.match = mapName != null ? m : null;
        return this;
    }

    JsonObject toJson() {
        JsonObject body = CoreApi.map("server", server, "online", online);
        if (port > 0) body.addProperty("port", port);
        // Формат core-service плоский: поля матча лежат на верхнем уровне тела
        if (match != null) {
            for (String key : match.keySet()) body.add(key, match.get(key));
        }
        return body;
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }
}
