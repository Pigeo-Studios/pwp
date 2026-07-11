package com.pwp.coreserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class BanCommand {

    private static final String HMAC_SECRET = "pwp_launcher_secret_2024";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pwpban")
            .requires(s -> s.hasPermission(2))
            .then(Commands.argument("player", StringArgumentType.string())
                .then(Commands.argument("duration", StringArgumentType.string())
                    .suggests((ctx, b) -> {
                        b.suggest("30m"); b.suggest("1h"); b.suggest("6h");
                        b.suggest("12h"); b.suggest("1d"); b.suggest("7d");
                        b.suggest("30d"); b.suggest("perm");
                        return b.buildFuture();
                    })
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> ban(ctx,
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "duration"),
                            StringArgumentType.getString(ctx, "reason")))
                    )
                )
            )
        );
    }

    private static int ban(CommandContext<CommandSourceStack> ctx, String target, String duration, String reason) {
        ctx.getSource().getServer().execute(() -> {
            new Thread(() -> {
                try {
                    JsonObject body = new JsonObject();
                    body.addProperty("target", target);
                    body.addProperty("reason", reason);
                    body.addProperty("duration", duration);
                    String result = postJson("/api/v1/launcher/ban", body.toString());
                    JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                    ctx.getSource().sendSuccess(() ->
                        Component.literal(json.has("error") ? "\u00A7c" + json.get("error").getAsString()
                            : "\u00A7a" + target + " banned (" + (json.has("duration") ? json.get("duration").getAsString() : duration) + ")"),
                        true);
                } catch (Exception e) {
                    ctx.getSource().sendFailure(Component.literal("\u00A7cBan failed: " + e.getMessage()));
                }
            }, "PWP-Ban").start();
        });
        return 1;
    }

    static String postJson(String path, String body) throws Exception {
        URL url = new URL(CoreServerMod.API_BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + CoreServerMod.API_KEY);
        conn.setRequestProperty("X-PWP-Sign", sign(path));
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }

    static String sign(String path) {
        try {
            long ts = System.currentTimeMillis();
            String data = ts + ":" + path;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_SECRET.getBytes("UTF-8"), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return ts + ":" + hex.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
