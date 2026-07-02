package com.pwp.core.auth;

import com.pwp.core.CoreApplication;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;

import java.util.Arrays;

public class AuthMiddleware {

    public static void handle(Context ctx) {
        if (ctx.path().equals("/api/v1/health")) return;

        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String token = authHeader.substring("Bearer ".length());
        String[] validKeys = CoreApplication.config.api.keys;
        boolean valid = Arrays.asList(validKeys).contains(token);

        if (!valid) {
            throw new UnauthorizedResponse("Invalid API key");
        }
    }
}
