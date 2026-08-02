package com.pwp.coreserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Проверяет баны через core-service и кикает забаненных:
 * раз в 20с весь онлайн + сразу при входе игрока (страховка от обхода authlib).
 */
public class BanEnforcer {

    private static final int POLL_INTERVAL_TICKS = 400; // 20 секунд
    private int ticks;

    public BanEnforcer() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++ticks < POLL_INTERVAL_TICKS) return;
        ticks = 0;
        MinecraftServer server = event.getServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        List<PlayerRef> players = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            players.add(new PlayerRef(p.getStringUUID(), p.getGameProfile().getName(), safeIp(p)));
        }
        new Thread(() -> {
            List<Kick> kicks = fetchKicks(players);
            if (!kicks.isEmpty()) {
                server.execute(() -> kickAll(server, kicks));
            }
        }, "pwp-ban-check").start();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        MinecraftServer server = player.server;
        List<PlayerRef> one = new ArrayList<>();
        one.add(new PlayerRef(player.getStringUUID(), player.getGameProfile().getName(), safeIp(player)));
        new Thread(() -> {
            List<Kick> kicks = fetchKicks(one);
            if (!kicks.isEmpty()) {
                server.execute(() -> kickAll(server, kicks));
            }
        }, "pwp-ban-check-join").start();
    }

    private static List<Kick> fetchKicks(List<PlayerRef> players) {
        List<Kick> kicks = new ArrayList<>();
        try {
            JsonArray arr = new JsonArray();
            for (PlayerRef ref : players) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", ref.uuid);
                o.addProperty("nickname", ref.name);
                o.addProperty("ip", ref.ip != null ? ref.ip : "");
                arr.add(o);
            }
            JsonObject resp = CoreServerApi.checkBans(arr);
            if (resp == null || !resp.has("data")) return kicks;
            JsonObject data = resp.getAsJsonObject("data");
            if (!data.has("kicks")) return kicks;
            for (JsonElement e : data.get("kicks").getAsJsonArray()) {
                JsonObject k = e.getAsJsonObject();
                if (!k.has("uuid") || k.get("uuid").isJsonNull()) continue;
                String reason = k.has("reason") && !k.get("reason").isJsonNull()
                        ? k.get("reason").getAsString() : "";
                kicks.add(new Kick(k.get("uuid").getAsString(), reason));
            }
        } catch (Exception e) {
            CoreServerMod.log.warn("ban-check failed: {}", e.getMessage());
        }
        return kicks;
    }

    private static void kickAll(MinecraftServer server, List<Kick> kicks) {
        for (Kick k : kicks) {
            ServerPlayer p = null;
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                if (sp.getStringUUID().equals(k.uuid)) { p = sp; break; }
            }
            if (p == null) continue;
            String reason = k.reason.isEmpty() ? "Вы забанены" : k.reason;
            p.connection.disconnect(Component.literal("\u00A7cВы забанены: " + reason));
            CoreServerMod.log.info("Kicked banned player {}: {}", p.getGameProfile().getName(), reason);
        }
    }

    private static String safeIp(ServerPlayer p) {
        try {
            return p.getIpAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private static class PlayerRef {
        final String uuid;
        final String name;
        final String ip;

        PlayerRef(String uuid, String name, String ip) {
            this.uuid = uuid;
            this.name = name;
            this.ip = ip;
        }
    }

    private static class Kick {
        final String uuid;
        final String reason;

        Kick(String uuid, String reason) {
            this.uuid = uuid;
            this.reason = reason;
        }
    }
}
