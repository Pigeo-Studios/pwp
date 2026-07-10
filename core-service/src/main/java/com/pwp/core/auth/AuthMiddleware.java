package com.pwp.core.auth;

import com.pwp.core.CoreApplication;
import io.javalin.http.Context;
import io.javalin.http.TooManyRequestsResponse;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthMiddleware {

    private static final Logger log = LoggerFactory.getLogger(AuthMiddleware.class);
    private static final Map<String, RateBucket> rateBuckets = new ConcurrentHashMap<>();
    private static long lastCleanup = System.currentTimeMillis();

    public static void handle(Context ctx, CoreApplication.ApiConfig apiConfig) {
        String path = ctx.path();

        // Health check — always allowed
        if (path.startsWith("/api/v1/health")) return;

        // IP-based rate limiting (100 req/min per IP)
        if (!checkIpRateLimit(ctx.ip(), 100)) {
            log.warn("IP rate limit exceeded: {}", ctx.ip());
            throw new TooManyRequestsResponse("Rate limit exceeded");
        }

        // ── Launcher-only verification ─────────────────────
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

        // Reject requests older than 30 seconds
        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            if (Math.abs(now - ts) > 30_000) {
                throw new UnauthorizedResponse("Access denied: expired request");
            }
        } catch (NumberFormatException e) {
            throw new UnauthorizedResponse("Access denied: invalid timestamp");
        }

        // Verify HMAC — format: timestamp:path, always with static key
        String signData = timestamp + ":" + path;
        String expected = hmacSha256(signData, CoreApplication.config.getLauncherSecret());
        if (!signature.equals(expected)) {
            log.warn("Invalid launcher signature from {} (path={}, expected={}, got={})", ctx.ip(), path, expected, signature);
            throw new UnauthorizedResponse("Access denied: invalid signature");
        }

        // ── API key auth for internal endpoints ────────────
        if (path.startsWith("/api/v1/auth/login") ||
            path.startsWith("/api/v1/auth/register") ||
            path.startsWith("/api/v1/auth/verify-2fa") ||
            path.startsWith("/api/v1/auth/check-") ||
            path.startsWith("/api/v1/auth/send-2fa") ||
            path.startsWith("/api/v1/auth/validate-session") ||
            path.startsWith("/api/v1/auth/refresh") ||
            path.startsWith("/api/v1/auth/heartbeat") ||
            path.startsWith("/api/v1/auth/revoke-sessions") ||
            path.startsWith("/api/v1/auth/confirm-ip") ||
            path.startsWith("/api/v1/auth/check-ip-confirm") ||
            path.startsWith("/api/v1/launcher/") ||
            path.startsWith("/launcher/files/")) {
            return; // launcher-only endpoints — sign check already passed
        }

        // Internal endpoints require API key
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String token = authHeader.substring("Bearer ".length());
        if (apiConfig.keys == null || !Arrays.asList(apiConfig.keys).contains(token)) {
            throw new UnauthorizedResponse("Invalid API key");
        }

        if (!isBypassKey(token, apiConfig) && !checkRateLimit(token, apiConfig)) {
            log.warn("Rate limit exceeded for a key");
            throw new TooManyRequestsResponse("Rate limit exceeded");
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

    private static class RateBucket {
        long windowStart; int count;
        RateBucket(long now) { this.windowStart = now; }
    }
}
