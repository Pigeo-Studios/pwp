package com.pwp.coreserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CoreServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerConnectHandler {

    private static final Logger log = LoggerFactory.getLogger(PlayerConnectHandler.class);

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String uuid = player.getStringUUID();
        String rawIp = player.connection.connection.getRemoteAddress().toString();
        if (rawIp.startsWith("/")) rawIp = rawIp.substring(1);
        int colon = rawIp.lastIndexOf(':');
        final String ip = colon > 0 ? rawIp.substring(0, colon) : rawIp;

        new Thread(() -> {
            try {
                String result = verifyIp(uuid, ip);
                JsonObject json = JsonParser.parseString(result).getAsJsonObject();

                boolean success = json.get("success").getAsBoolean();

                if (!success) {
                    String error = json.has("error") ? json.get("error").getAsString() : "IP not verified";
                    log.warn("Kicking {} ({}) reason: {}", player.getScoreboardName(), uuid, error);
                    player.connection.disconnect(Component.literal("\u00A7c\u00A7l\u0414\u043E\u0441\u0442\u0443\u043F \u0437\u0430\u043F\u0440\u0435\u0449\u0451\u043D\n\n\u00A77" + error));
                    return;
                }

                Long confirmId = json.has("confirmId") && !json.get("confirmId").isJsonNull()
                    ? json.get("confirmId").getAsLong() : null;

                if (confirmId != null) {
                    log.info("IP verification pending for {} (confirmId={}), polling...", player.getScoreboardName(), confirmId);
                    for (int i = 0; i < 30; i++) {
                        Thread.sleep(1000);
                        String pollResult = pollConfirm(confirmId);
                        JsonObject pollJson = JsonParser.parseString(pollResult).getAsJsonObject();
                        if (!pollJson.get("success").getAsBoolean()) {
                            String pollError = pollJson.has("error") ? pollJson.get("error").getAsString() : "";
                            if ("pending".equals(pollError)) continue;
                            log.warn("Kicking {} reason: {}", player.getScoreboardName(), pollError);
                            player.connection.disconnect(Component.literal("\u00A7c\u00A7l\u0414\u043E\u0441\u0442\u0443\u043F \u0437\u0430\u043F\u0440\u0435\u0449\u0451\u043D\n\n\u00A77" + pollError));
                            return;
                        }
                        log.info("IP verified for {} (allow)", player.getScoreboardName());
                        return;
                    }
                    player.connection.disconnect(Component.literal("\u00A7c\u00A7l\u0412\u0440\u0435\u043C\u044F \u043F\u043E\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043D\u0438\u044F \u0438\u0441\u0442\u0435\u043A\u043B\u043E\n\n\u00A77\u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439\u0442\u0435 \u043F\u043E\u0437\u0436\u0435"));
                }
            } catch (Exception e) {
                log.error("IP verification error for {}: {}", player.getScoreboardName(), e.getMessage());
            }
        }, "PWP-IP-Verify").start();
    }

    private static String verifyIp(String uuid, String ip) throws Exception {
        URL url = new URL(CoreServerMod.API_BASE + "/api/v1/auth/verify-ip");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        String body = String.format("{\"uuid\":\"%s\",\"ip\":\"%s\"}", uuid, ip);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }

    private static String pollConfirm(long confirmId) throws Exception {
        URL url = new URL(CoreServerMod.API_BASE + "/api/v1/auth/check-ip-confirm?id=" + confirmId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }
}
