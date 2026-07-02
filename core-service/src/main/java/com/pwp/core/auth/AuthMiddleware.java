package com.pwp.core.auth;

import com.pwp.core.CoreApplication;
import io.javalin.http.Context;
import io.javalin.http.TooManyRequestsResponse;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthMiddleware {

    private static final Logger log = LoggerFactory.getLogger(AuthMiddleware.class);

    private static final Map<String, RateBucket> rateBuckets = new ConcurrentHashMap<>();
    private static long lastCleanup = System.currentTimeMillis();

    public static void handle(Context ctx, CoreApplication.ApiConfig apiConfig) {
        if (ctx.path().equals("/api/v1/health")) return;

        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String token = authHeader.substring("Bearer ".length());
        boolean valid = Arrays.asList(apiConfig.keys).contains(token);

        if (!valid) {
            throw new UnauthorizedResponse("Invalid API key");
        }

        if (!checkRateLimit(token, apiConfig)) {
            log.warn("Rate limit exceeded for key {}", token.substring(0, Math.min(8, token.length())));
            throw new TooManyRequestsResponse("Rate limit exceeded");
        }
    }

    private static boolean checkRateLimit(String key, CoreApplication.ApiConfig apiConfig) {
        int limit = apiConfig.rateLimitPerMinute;
        long now = System.currentTimeMillis();

        // Evict stale buckets every 5 minutes
        if (now - lastCleanup > 300_000) {
            lastCleanup = now;
            rateBuckets.entrySet().removeIf(e ->
                now - e.getValue().windowStart > 120_000
            );
        }

        RateBucket bucket = rateBuckets.computeIfAbsent(key, k -> new RateBucket(now));

        synchronized (bucket) {
            if (now - bucket.windowStart > 60_000) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            bucket.count++;
            return bucket.count <= limit;
        }
    }

    private static class RateBucket {
        long windowStart;
        int count;

        RateBucket(long now) {
            this.windowStart = now;
        }
    }
}
