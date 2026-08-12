package com.pwp.lobby.donate;

import com.google.gson.JsonObject;
import com.pwp.coreserver.CoreServerApi;
import com.pwp.coreclient.network.PacketDonatorTiers;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.lobby.match.MatchAllocator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Донат-тиры и роли в лобби (источник — core-service профиль игрока).
 * Тиры храним в persistentData игрока, рефрешим при входе и раз в 5 минут (чтобы
 * изменения из админки подтягивались без перелогина). При недоступности core-service —
 * fail-open: игрок просто не получает статус, вход не блокируется.
 * Флай — только для GOLD/PLATINUM и только в фазе лобби (не при активном матче).
 */
public final class DonatorStatusManager {

    private static final Logger log = LoggerFactory.getLogger(DonatorStatusManager.class);

    public static final String TIER_TAG = "PWP_DonateTier";
    public static final String ROLE_TAG = "PWP_Role";

    public static final String TIER_SILVER = "SILVER";
    public static final String TIER_GOLD = "GOLD";
    public static final String TIER_PLATINUM = "PLATINUM";

    private static final int REFRESH_INTERVAL_TICKS = 20 * 60 * 5; // 5 минут
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pwp-donator-fetch");
        t.setDaemon(true);
        return t;
    });

    private static int refreshTicks = 0;

    private DonatorStatusManager() {}

    // ====== Доступ к статусу (сервер) ======

    public static String tierOf(ServerPlayer p) {
        return p.getPersistentData().getString(TIER_TAG);
    }

    public static boolean isFlightTier(ServerPlayer p) {
        String t = tierOf(p);
        return TIER_GOLD.equals(t) || TIER_PLATINUM.equals(t);
    }

    public static String roleOf(ServerPlayer p) {
        return p.getPersistentData().getString(ROLE_TAG);
    }

    /** Display-уровень роли (ADMIN/MODERATOR) или пусто, если роли нет. */
    public static String displayRole(ServerPlayer p) {
        String r = roleOf(p);
        if (r == null || r.isEmpty()) return "";
        return switch (r.toLowerCase()) {
            case "admin", "owner" -> "ADMIN";
            case "support", "moderator" -> "MODERATOR";
            default -> "";
        };
    }

    // ====== Вход / фетч ======

    public static void onPlayerJoin(ServerPlayer player) {
        fetchAsync(player);
        broadcastDonators();
        refreshFlight(player);
    }

    /** Фетч в фоновом потоке, результат применяем на серверном потоке. Fail-open при ошибке. */
    private static void fetchAsync(ServerPlayer player) {
        String uuid = player.getStringUUID();
        var server = player.getServer();
        if (server == null) return;
        EXECUTOR.submit(() -> {
            try {
                JsonObject resp = CoreServerApi.getPlayerProfile(uuid);
                JsonObject data = resp != null && resp.has("data") ? resp.getAsJsonObject("data") : resp;
                if (data == null || !data.has("player")) return;
                JsonObject pl = data.getAsJsonObject("player");
                String tier = jsonStrNotNull(pl, "donateTier");
                String role = jsonStrNotNull(pl, "role");
                server.execute(() -> {
                    ServerPlayer online = server.getPlayerList().getPlayer(player.getUUID());
                    if (online == null) return;
                    applyStatus(online, tier, role);
                });
            } catch (Exception e) {
                log.warn("donate profile fetch failed for {}: {}", uuid, e.getMessage());
            }
        });
    }

    private static void applyStatus(ServerPlayer p, String tier, String role) {
        CompoundTag tag = p.getPersistentData();
        String norm = tier == null ? "" : tier.toUpperCase();
        // "NONE" трактуем как отсутствие тира (колонка БД хранит строку "NONE", а не NULL)
        if (!norm.isEmpty() && !"NONE".equals(norm)) tag.putString(TIER_TAG, norm);
        else tag.remove(TIER_TAG);
        if (!role.isEmpty()) tag.putString(ROLE_TAG, role);
        else tag.remove(ROLE_TAG);
        refreshFlight(p);
        broadcastDonators();
    }

    /** Применение статуса из админ-команды /pwp donate: тир + роль ("user"/null — снять роль). */
    public static void applyDonateStatus(ServerPlayer p, String tier, String role) {
        String r = (role == null || "user".equalsIgnoreCase(role)) ? "" : role;
        applyStatus(p, tier, r);
    }

    // ====== Периодический рефреш ======

    public static void serverTick() {
        if (++refreshTicks < REFRESH_INTERVAL_TICKS) return;
        refreshTicks = 0;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            fetchAsync(p);
        }
    }

    // ====== Рассылка клиентам ======

    /** Полный снапшот донатеров и ролей лобби (tier != NONE или роль ADMIN/MODERATOR) всем игрокам. */
    public static void broadcastDonators() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        List<String> uuids = new ArrayList<>();
        List<String> tiers = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String t = tierOf(p);
            String r = displayRole(p);
            if ((t.isEmpty() || "NONE".equalsIgnoreCase(t)) && r.isEmpty()) continue;
            uuids.add(p.getStringUUID());
            tiers.add(t);
            roles.add(r);
        }
        PacketDonatorTiers pkt = new PacketDonatorTiers(uuids.toArray(new String[0]), tiers.toArray(new String[0]), roles.toArray(new String[0]));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), pkt);
            } catch (Exception ignored) {}
        }
    }

    // ====== Флай ======

    private static boolean flightAllowed(ServerPlayer p) {
        if (MatchAllocator.hasActiveMatch()) return false;
        return isFlightTier(p);
    }

    /** Обновляет полёт по текущему статусу; вызывает правильные сетты + синк клиенту. */
    public static void refreshFlight(ServerPlayer p) {
        Abilities ab = p.getAbilities();
        // Креатив и спектатор летают по своему режиму — донат-логика их не трогает
        // (иначе секундная проверка в лобби снимала бы mayfly у креатива)
        GameType gt = p.gameMode.getGameModeForPlayer();
        if (gt == GameType.CREATIVE || gt == GameType.SPECTATOR) return;
        boolean allow = flightAllowed(p);
        if (allow && !ab.mayfly) {
            ab.mayfly = true;
            ab.flying = true;
        } else if (!allow && ab.mayfly) {
            ab.mayfly = false;
            ab.flying = false;
        } else {
            return;
        }
        p.onUpdateAbilities();
        p.connection.send(new ClientboundPlayerAbilitiesPacket(ab));
    }

    public static void refreshAllFlight() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                refreshFlight(p);
            } catch (Exception ignored) {}
        }
    }

    private static String jsonStrNotNull(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return "";
        return o.get(key).getAsString();
    }
}
