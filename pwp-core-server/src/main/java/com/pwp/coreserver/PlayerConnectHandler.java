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
    private static final int IP_CONFIRM_TIMEOUT_SEC = 300; // 5 минут

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String uuid = player.getStringUUID();
        String name = player.getScoreboardName();

        new Thread(() -> {
            try {
                // Try server-token verification first
                String serverToken = System.getenv("PWP_SERVER_TOKEN");
                if (serverToken != null && !serverToken.isEmpty()) {
                    String result = postJson("/api/v1/launcher/verify-server-token?token=" + serverToken, "{\"uuid\":\"" + uuid + "\"}");
                    JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                    if (json.get("success").getAsBoolean()) {
                        log.info("{} authenticated via server token", name);
                        return;
                    }
                    log.warn("{} has invalid server token", name);
                }

                // IP verification
                String rawIp = player.connection.connection.getRemoteAddress().toString();
                if (rawIp.startsWith("/")) rawIp = rawIp.substring(1);
                int colon = rawIp.lastIndexOf(':');
                String ip = colon > 0 ? rawIp.substring(0, colon) : rawIp;

                String result = postJson("/api/v1/auth/verify-ip", "{\"uuid\":\"" + uuid + "\",\"ip\":\"" + ip + "\"}");
                JsonObject json = JsonParser.parseString(result).getAsJsonObject();

                if (json.get("success").getAsBoolean()) {
                    log.info("{} IP verified, allowed", name);
                    return;
                }

                // Check if there's a pending confirmId
                Long confirmId = json.has("confirmId") && !json.get("confirmId").isJsonNull()
                    ? json.get("confirmId").getAsLong() : null;

                if (confirmId != null) {
                    log.info("IP confirm pending for {} (confirmId={}), polling {}s...", name, confirmId, IP_CONFIRM_TIMEOUT_SEC);
                    for (int i = 0; i < IP_CONFIRM_TIMEOUT_SEC; i++) {
                        Thread.sleep(1000);
                        String pollResult = getJson("/api/v1/auth/check-ip-confirm?id=" + confirmId);
                        JsonObject pollJson = JsonParser.parseString(pollResult).getAsJsonObject();
                        if (!pollJson.get("success").getAsBoolean()) {
                            String pollError = pollJson.has("error") ? pollJson.get("error").getAsString() : "";
                            if ("pending".equals(pollError)) continue;
                            log.warn("Kicking {}: {}", name, pollError);
                            player.connection.disconnect(Component.literal("\u00A7c\u00A7l\u0414\u043E\u0441\u0442\u0443\u043F \u0437\u0430\u043F\u0440\u0435\u0449\u0451\u043D\n\n\u00A77" + pollError));
                            return;
                        }
                        log.info("IP confirmed for {} (allow)", name);
                        return;
                    }
                    player.connection.disconnect(Component.literal("\u00A7c\u00A7l\u0412\u0440\u0435\u043C\u044F \u043F\u043E\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043D\u0438\u044F \u0438\u0441\u0442\u0435\u043A\u043B\u043E\n\n\u00A77\u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435 \u043F\u043E\u0437\u0436\u0435"));
                } else {
                    String error = json.has("error") ? json.get("error").getAsString() : "Access denied";
                    player.connection.disconnect(Component.literal("\u00A7c\u00A7l\u0414\u043E\u0441\u0442\u0443\u043F \u0437\u0430\u043F\u0440\u0435\u0449\u0451\u043D\n\n\u00A77" + error));
                }
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

    private static String getJson(String path) throws Exception {
        URL url = new URL(CoreServerMod.API_BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + CoreServerMod.API_KEY);
        conn.setRequestProperty("X-PWP-Sign", hmacSign(path.contains("?") ? path.substring(0, path.indexOf('?')) : path));
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }
}
