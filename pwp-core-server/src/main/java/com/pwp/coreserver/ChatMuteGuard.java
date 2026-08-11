package com.pwp.coreserver;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Кэш-проверка текстового мута (общая для всех модулей, TTL 5с — как у войс-мута). */
public class ChatMuteGuard {

    private static final Map<UUID, Boolean> cached = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> stamps = new ConcurrentHashMap<>();
    private static final long TTL_MS = 5000L;

    public static boolean isChatMuted(String uuid) {
        final UUID u;
        try {
            u = UUID.fromString(uuid);
        } catch (Exception e) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long ts = stamps.get(u);
        if (ts != null && now - ts < TTL_MS) {
            return cached.getOrDefault(u, false);
        }
        try {
            JsonObject r = CoreServerApi.getChatMute(uuid);
            boolean muted = r != null && r.has("success") && r.get("success").getAsBoolean()
                && r.has("data") && !r.get("data").isJsonNull()
                && r.getAsJsonObject("data").has("muted")
                && r.getAsJsonObject("data").get("muted").getAsBoolean();
            cached.put(u, muted);
            stamps.put(u, now);
            return muted;
        } catch (Exception e) {
            CoreServerMod.log.warn("Chat mute check failed for {}: {}", uuid, e.getMessage());
            cached.put(u, false);
            stamps.put(u, now);
            return false;
        }
    }

    public static void invalidate(String uuid) {
        try {
            UUID u = UUID.fromString(uuid);
            cached.remove(u);
            stamps.remove(u);
        } catch (Exception ignored) {}
    }
}
