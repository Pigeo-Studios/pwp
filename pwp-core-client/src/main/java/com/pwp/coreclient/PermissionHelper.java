package com.pwp.coreclient;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionHelper {

    private static final Logger log = LoggerFactory.getLogger(PermissionHelper.class);

    public static void autoOpIfAdmin(ServerPlayer player) {
        if (player == null || player.server == null) return;

        var gameProfile = player.getGameProfile();
        var oplist = player.server.getPlayerList();

        if (oplist.isOp(gameProfile)) return;

        JsonObject data = CoreAPI.loadPlayer(player.getStringUUID());
        if (data == null || !data.has("data")) return;

        JsonObject profile = data.getAsJsonObject("data");
        if (!profile.has("player")) return;

        JsonObject p = profile.getAsJsonObject("player");
        String role = p.has("role") ? p.get("role").getAsString() : "PLAYER";

        if ("admin".equalsIgnoreCase(role)) {
            oplist.op(gameProfile);
            log.info("Auto-opped admin {} ({})", player.getName().getString(), player.getStringUUID());
        }
    }
}
