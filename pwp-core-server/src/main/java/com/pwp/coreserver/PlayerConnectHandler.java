package com.pwp.coreserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class PlayerConnectHandler {

    private static final Logger log = LoggerFactory.getLogger(PlayerConnectHandler.class);
    private static final String HMAC_SECRET = "pwp_launcher_secret_2024";

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String uuid = player.getStringUUID();
        String name = player.getScoreboardName();

        new Thread(() -> {
            try {
                // Try server-token verification first (env is set by launcher for client process)
                String serverToken = System.getenv("PWP_SERVER_TOKEN");
                if (serverToken != null && !serverToken.isEmpty()) {
                    String result = postJson("/api/v1/launcher/verify-server-token?token=" + serverToken, uuid);
                    JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                    if (json.get("success").getAsBoolean()) {
                        log.info("{} authenticated via server token", name);
                        return; // allowed
                    }
                    log.warn("{} has invalid server token", name);
                }

                // Try IP verification fallback
                String rawIp = player.connection.connection.getRemoteAddress().toString();
                if (rawIp.startsWith("/")) rawIp = rawIp.substring(1);
                int colon = rawIp.lastIndexOf(':');
                String ip = colon > 0 ? rawIp.substring(0, colon) : rawIp;

                String result = postJson("/api/v1/auth/verify-ip", "{\"uuid\":\"" + uuid + "\",\"ip\":\"" + ip + "\"}");
                JsonObject json = JsonParser.parseString(result).getAsJsonObject();

                if (!json.get("success").getAsBoolean()) {
                    String error = json.has("error") ? json.get("error").getAsString() : "IP not verified";
                    player.connection.disconnect(Component.literal("\u00A7c\u00A7l\u0414\u043E\u0441\u0442\u0443\u043F \u0437\u0430\u043F\u0440\u0435\u0449\u0451\u043D\n\n\u00A77" + error));
                    return;
                }

                log.info("{} ({}) IP verified, allowed", name, uuid);
            } catch (Exception e) {
                log.error("Auth error for {}: {}", name, e.getMessage());
            }
        }, "PWP-Auth").start();
    }

    private static String hmacSign(String path) {
        try {
            long timestamp = System.currentTimeMillis();
            String data = timestamp + ":" + path;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(HMAC_SECRET.getBytes("UTF-8"), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return timestamp + ":" + hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String postJson(String path, String body) throws Exception {
        URL url = new URL(CoreServerMod.API_BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + CoreServerMod.API_KEY);
        conn.setRequestProperty("X-PWP-Sign", hmacSign(path.contains("?") ? path.substring(0, path.indexOf('?')) : path));
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
}
