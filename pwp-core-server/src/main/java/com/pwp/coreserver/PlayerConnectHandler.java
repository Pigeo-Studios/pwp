package com.pwp.coreserver;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerConnectHandler {

    private static final Logger log = LoggerFactory.getLogger(PlayerConnectHandler.class);
    private static final long AUTH_TIMEOUT_MS = 3000;

    private static final Map<String, Long> pendingAuth = new ConcurrentHashMap<>();

    // Cache for Core verify results on match servers (token → expiry)
    private static final Map<String, Long> authCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10_000;

    // Allowed token hashes loaded from allowed_tokens.json (match servers only)
    public static Set<String> allowedTokenHashes = ConcurrentHashMap.newKeySet();
    public static Map<String, Long> allowedTokenAccounts = new ConcurrentHashMap<>(); // tokenHash → accountId

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String uuid = player.getStringUUID();
        String name = player.getScoreboardName();
        log.info("PLAYER LOGIN EVENT — uuid={}, name={}", uuid, name);

        if (CoreServerMod.isMatchServer && !allowedTokenHashes.isEmpty()) {
            log.info("Match server mode — will verify via token hash or Core");
        }

        pendingAuth.put(uuid, System.currentTimeMillis());
        log.info("AUTH PENDING - {} added to queue", name);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pendingAuth.isEmpty()) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long now = System.currentTimeMillis();
        Set<String> toRemove = new HashSet<>();

        for (var entry : pendingAuth.entrySet()) {
            String uuid = entry.getKey();
            long joinMs = entry.getValue();

            if (now - joinMs >= AUTH_TIMEOUT_MS) {
                ServerPlayer player = server.getPlayerList().getPlayer(java.util.UUID.fromString(uuid));
                if (player != null && player.connection != null) {
                    log.warn("AUTH TIMEOUT - {} (uuid={}), elapsedMs={}", player.getScoreboardName(), uuid, now - joinMs);
                    disconnectNow(player, "Войдите через лаунчер");
                }
                toRemove.add(uuid);
            }
        }

        for (String uuid : toRemove) pendingAuth.remove(uuid);
    }

    public static void handleToken(String uuid, String token) {
        if (uuid == null || token == null || token.isEmpty()) {
            log.warn("HANDLE TOKEN — invalid input");
            return;
        }

        log.info("AUTH — uuid={}, tokenLen={}", uuid, token.length());

        // 1. Check local token hash cache (match servers)
        if (CoreServerMod.isMatchServer && checkLocalHash(token, uuid)) {
            pendingAuth.remove(uuid);
            log.info("AUTH ACCEPTED (local hash) — uuid={}", uuid);
            return;
        }

        // 2. Check in-memory auth cache (recent Core verifications)
        Long cached = authCache.get(token);
        if (cached != null && System.currentTimeMillis() < cached) {
            pendingAuth.remove(uuid);
            log.info("AUTH ACCEPTED (cache) — uuid={}", uuid);
            return;
        }

        // 3. Call Core Service /api/v1/launcher/verify
        verifyWithCore(uuid, token);
    }

    private static boolean checkLocalHash(String token, String uuid) {
        try {
            String hash = sha256(token);
            if (allowedTokenHashes.contains(hash)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Local hash check failed: {}", e.getMessage());
        }
        return false;
    }

    private static void verifyWithCore(String uuid, String token) {
        try {
            String body = "{\"token\":\"" + token + "\"}";
            log.info("REST CALL /api/v1/launcher/verify — body length={}", body.length());
            String result = postJson("/api/v1/launcher/verify", body);
            log.info("REST RESPONSE — resultLength={}", result.length());

            var json = com.google.gson.JsonParser.parseString(result).getAsJsonObject();
            if (!json.has("success") || !json.get("success").getAsBoolean()) {
                String err = json.has("error") ? json.get("error").getAsString() : "unknown";
                log.warn("VERIFY FAILED — {}", err);
                return;
            }

            var data = json.getAsJsonObject("data");
            long accountId = data.has("accountId") ? data.get("accountId").getAsLong() : 0;
            String nickname = data.has("nickname") ? data.get("nickname").getAsString() : "?";
            String role = data.has("role") ? data.get("role").getAsString() : "?";

            // Cache the successful verification
            authCache.put(token, System.currentTimeMillis() + CACHE_TTL_MS);

            pendingAuth.remove(uuid);
            log.info("AUTH ACCEPTED (Core) — accountId={}, uuid={}, nickname={}, role={}", accountId, uuid, nickname, role);
        } catch (Exception e) {
            log.error("VERIFY EXCEPTION — {}: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private static ServerPlayer findPlayer(String uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        try {
            return server.getPlayerList().getPlayer(java.util.UUID.fromString(uuid));
        } catch (Exception e) {
            return null;
        }
    }

    private static void disconnectNow(ServerPlayer player, String message) {
        try {
            if (player.connection != null) {
                player.connection.disconnect(Component.literal(message));
            }
        } catch (Exception ignored) {}
    }

    private static final String HMAC_SECRET = "pwp_launcher_secret_2024";

    private static String hmacSign(String path) {
        try {
            long timestamp = System.currentTimeMillis();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(HMAC_SECRET.getBytes("UTF-8"), "HmacSHA256");
            mac.init(key);
            String data = timestamp + ":" + path;
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return timestamp + ":" + hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static HttpURLConnection openConnection(String path) throws Exception {
        URL url = new URL(CoreServerMod.API_BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + CoreServerMod.API_KEY);
        conn.setRequestProperty("X-PWP-Sign", hmacSign(path.contains("?") ? path.substring(0, path.indexOf('?')) : path));
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
    }

    private static String postJson(String path, String body) throws Exception {
        HttpURLConnection conn = openConnection(path);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }
}
