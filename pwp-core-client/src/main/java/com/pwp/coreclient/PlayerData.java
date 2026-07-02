package com.pwp.coreclient;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private static final Map<UUID, CachedProfile> cache = new HashMap<>();

    public static CachedProfile get(UUID uuid) {
        return cache.get(uuid);
    }

    public static void put(UUID uuid, JsonObject profile) {
        CachedProfile cp = new CachedProfile();
        cp.data = profile;
        cp.loadedAt = System.currentTimeMillis();
        cache.put(uuid, cp);
    }

    public static void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public static class CachedProfile {
        public JsonObject data;
        public long loadedAt;

        public long getCoins() {
            return data.has("coins") ? data.get("coins").getAsLong() : 0;
        }

        public long getXp() {
            return data.has("xp") ? data.get("xp").getAsLong() : 0;
        }

        public int getLevel() {
            return data.has("level") ? data.get("level").getAsInt() : 1;
        }
    }
}
