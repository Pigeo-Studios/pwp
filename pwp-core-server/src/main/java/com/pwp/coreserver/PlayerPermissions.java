package com.pwp.coreserver;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

public class PlayerPermissions {

    public static void autoOpIfAdmin(ServerPlayer player) {
        try {
            JsonObject data = CoreServerApi.loadPlayer(player.getStringUUID());
            if (data == null || !data.has("data")) return;
            JsonObject profile = data.getAsJsonObject("data");
            if (!profile.has("player")) return;
            String role = profile.getAsJsonObject("player").get("role").getAsString();
            if ("admin".equalsIgnoreCase(role)) {
                player.server.getPlayerList().op(player.getGameProfile());
            }
        } catch (Exception e) {
            System.err.println("[PWP] autoOpIfAdmin failed: " + e.getMessage());
        }
    }
}
