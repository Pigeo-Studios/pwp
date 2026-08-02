package com.pwp.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pwp.core.CoreApplication;
import com.pwp.core.db.PlayerRepository;
import io.javalin.http.Context;
import io.javalin.http.TooManyRequestsResponse;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthMiddleware {

    private static final Logger log = LoggerFactory.getLogger(AuthMiddleware.class);
    private static final Map<String, RateBucket> rateBuckets = new ConcurrentHashMap<>();
    private static long lastCleanup = System.currentTimeMillis();

    private static final String[] PUBLIC_PATHS = {
        "/api/v1/health",
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/verify-2fa",
        "/api/v1/launcher/version",
        "/api/v1/launcher/manifest",
        "/api/v1/launcher/logs",
        "/api/v1/launcher/anticheat/artifact/",
        "/api/v1/launcher/p5/",
        "/api/v1/admin/",
        "/authlib/",
        "/authlib/authserver/",
        "/authlib/sessionserver/",
        "/launcher/files/"
    };

    public static void handle(Context ctx, CoreApplication.ApiConfig apiConfig) {
        String path = ctx.path();

        // Public endpoints — no auth required (rate limit only)
        for (String p : PUBLIC_PATHS) {
            if (path.startsWith(p)) return;
        }

        // IP-based rate limiting
        if (!checkIpRateLimit(ctx.ip(), 300)) {
            log.warn("IP rate limit exceeded: {}", ctx.ip());
            throw new TooManyRequestsResponse("Rate limit exceeded");
        }

        // ── API key auth (server-to-server) ──
        String authHeader = ctx.header("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());
            if (apiConfig.keys != null && Arrays.asList(apiConfig.keys).contains(token)) {
                if (!isBypassKey(token, apiConfig) && !checkRateLimit(token, apiConfig)) {
                    log.warn("Rate limit exceeded for key");
                    throw new TooManyRequestsResponse("Rate limit exceeded");
                }
                return;
            }
        }

        // ── Launcher session HMAC ──
        String signHeader = ctx.header("X-PWP-Sign");
        if (signHeader == null || !signHeader.contains(":")) {
            throw new UnauthorizedResponse("Access denied: launcher required");
        }

        String[] parts = signHeader.split(":", 2);
        if (parts.length != 2) {
            throw new UnauthorizedResponse("Access denied: invalid signature");
        }

        String timestamp = parts[0];
        String signature = parts[1];

        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            if (Math.abs(now - ts) > 30_000) {
                throw new UnauthorizedResponse("Access denied: expired request");
            }
        } catch (NumberFormatException e) {
            throw new UnauthorizedResponse("Access denied: invalid timestamp");
        }

        String signData = timestamp + ":" + path;
        String sessionSecret = findSessionSecret(ctx);

        if (sessionSecret == null) {
            log.warn("No session secret for {} from {} (path={})", ctx.method(), ctx.ip(), path);
            throw new UnauthorizedResponse("Access denied: no valid session");
        }

        if (!hmacVerify(signData, signature, sessionSecret)) {
            log.warn("Invalid signature for {} from {} (path={})", ctx.method(), ctx.ip(), path);
            throw new UnauthorizedResponse("Access denied: invalid signature");
        }
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(key);
            return bytesToHex(mac.doFinal(data.getBytes("UTF-8")));
        } catch (Exception e) {
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static boolean isBypassKey(String token, CoreApplication.ApiConfig apiConfig) {
        if (apiConfig.rateLimitBypassKeys == null) return false;
        for (String k : apiConfig.rateLimitBypassKeys) {
            if (k.equals(token)) return true;
        }
        return false;
    }

    private static boolean checkRateLimit(String key, CoreApplication.ApiConfig apiConfig) {
        int limit = apiConfig.rateLimitPerMinute;
        long now = System.currentTimeMillis();
        if (now - lastCleanup > 300_000) {
            lastCleanup = now;
            rateBuckets.entrySet().removeIf(e -> now - e.getValue().windowStart > 120_000);
        }
        RateBucket bucket = rateBuckets.computeIfAbsent(key, k -> new RateBucket(now));
        synchronized (bucket) {
            if (now - bucket.windowStart > 60_000) { bucket.windowStart = now; bucket.count = 0; }
            bucket.count++;
            return bucket.count <= limit;
        }
    }

    private static final Map<String, RateBucket> ipBuckets = new ConcurrentHashMap<>();

    public static boolean checkIpRateLimit(String ip, int maxPerMinute) {
        long now = System.currentTimeMillis();
        RateBucket bucket = ipBuckets.computeIfAbsent(ip, k -> new RateBucket(now));
        synchronized (bucket) {
            if (now - bucket.windowStart > 60_000) { bucket.windowStart = now; bucket.count = 0; }
            bucket.count++;
            return bucket.count <= maxPerMinute;
        }
    }

    private static boolean hmacVerify(String data, String signature, String secret) {
        String expected = hmacSha256(data, secret);
        return constantTimeEquals(expected, signature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static final Gson GSON = new Gson();

    private static String findSessionSecret(Context ctx) {
        try {
            String body = ctx.body();
            if (body != null && !body.isBlank()) {
                JsonObject json = GSON.fromJson(body, JsonObject.class);
                if (json.has("accessToken")) {
                    return PlayerRepository.findHmacSecretByAccessToken(json.get("accessToken").getAsString());
                }
                if (json.has("access_token")) {
                    return PlayerRepository.findHmacSecretByAccessToken(json.get("access_token").getAsString());
                }
                if (json.has("refreshToken")) {
                    return PlayerRepository.findHmacSecretByRefreshToken(json.get("refreshToken").getAsString());
                }
                if (json.has("refresh_token")) {
                    return PlayerRepository.findHmacSecretByRefreshToken(json.get("refresh_token").getAsString());
                }
            }
            String token = ctx.queryParam("accessToken");
            if (token == null) token = ctx.queryParam("access_token");
            if (token == null) token = ctx.queryParam("token");
            if (token != null) return PlayerRepository.findHmacSecretByAccessToken(token);
        } catch (Exception e) {
            log.debug("Failed to find session secret: {}", e.getMessage());
        }
        return null;
    }

    private static class RateBucket {
        long windowStart; int count;
        RateBucket(long now) { this.windowStart = now; }
    }
}
