package com.pwp.coreserver;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Автоматический OP для админов из БД (роль admin/owner) — иначе команды /pwp недоступны. */
public class PlayerPermissions {

    public static void autoOpIfAdminAsync(MinecraftServer server, String uuid) {
        new Thread(() -> {
            try {
                JsonObject data = CoreServerApi.loadPlayer(uuid);
                if (data == null || !data.has("data")) return;
                JsonObject profile = data.getAsJsonObject("data");
                if (!profile.has("player")) return;
                String role = profile.getAsJsonObject("player").get("role").getAsString();
                if (!"admin".equalsIgnoreCase(role) && !"owner".equalsIgnoreCase(role)) return;
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
                    if (player != null) {
                        server.getPlayerList().op(player.getGameProfile());
                        CoreServerMod.log.info("Auto-OPed admin {}", player.getGameProfile().getName());
                    }
                });
            } catch (Exception e) {
                System.err.println("[PWP] autoOpIfAdmin failed: " + e.getMessage());
            }
        }, "pwp-autoop").start();
    }
}
