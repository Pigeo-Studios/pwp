package com.pwp.coreclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            return data != null && data.has("coins") ? data.get("coins").getAsLong() : 0;
        }

        public long getXp() {
            return data != null && data.has("xp") ? data.get("xp").getAsLong() : 0;
        }

        public int getLevel() {
            return data != null && data.has("level") ? data.get("level").getAsInt() : 1;
        }

        public int getPrestige() {
            return data != null && data.has("prestige") ? data.get("prestige").getAsInt() : 0;
        }

        public List<CosmeticEntry> getCosmetics() {
            List<CosmeticEntry> list = new ArrayList<>();
            if (data == null || !data.has("cosmetics")) return list;
            JsonArray arr = data.get("cosmetics").getAsJsonArray();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                CosmeticEntry ce = new CosmeticEntry();
                ce.itemUuid = obj.get("itemUuid").getAsString();
                ce.skinId = obj.get("skinId").getAsString();
                ce.slotType = obj.get("slotType").getAsString();
                ce.rarity = obj.get("rarity").getAsString();
                ce.source = obj.get("source").getAsString();
                ce.equipped = obj.has("equipped") && obj.get("equipped").getAsBoolean();
                list.add(ce);
            }
            return list;
        }
    }

    public static class CosmeticEntry {
        public String itemUuid;
        public String skinId;
        public String slotType;
        public String rarity;
        public String source;
        public boolean equipped;
    }
}
