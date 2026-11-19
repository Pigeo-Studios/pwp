package com.pwp.coreclient.network;

import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.time.GameClock;
import com.pwp.api.util.ServerThread;
import com.pwp.coreclient.CoreClientMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервер: уровень доступа игрока ({@link AccessLevel} = max(op, роль из БД)) → клиенту пакетом
 * {@link AccessLevelPacket}. При входе — сразу по op, затем асинхронно роль из core-service
 * ({@link CoreApi#getPlayerRole}, колбэк через {@link ServerThread#thenForPlayer}) и повторная отправка, если
 * уровень изменился. Смена op ловится опросом раз в 2 с, смена роли в БД — перезапросом раз в 2 мин.
 * core-service недоступен — работает только op (роль не кэшируется).
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AccessSync {
    private static final Logger LOG = LoggerFactory.getLogger(AccessSync.class);
    private static final int CHECK_TICKS = 40;
    private static final int ROLE_REFRESH_TICKS = 2400;

    /** Последний отправленный уровень по UUID. */
    private static final Map<UUID, AccessLevel> SENT = new ConcurrentHashMap<>();

    private AccessSync() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PwpPermissions.forget(player.getUUID());
        push(player);
        fetchRole(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // модератор вышел в режиме наблюдения — вернуть прежний режим (иначе зайдёт спектатором)
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)
            com.pwp.coreclient.network.data.ModerationAction.onLogout(sp);
        UUID id = event.getEntity().getUUID();
        SENT.remove(id);
        PwpPermissions.forget(id);
    }

    /** Хвост WARN/ERROR для вкладки «Сервер» админ-панели (один аппендер на процесс). */
    @SubscribeEvent
    public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        com.pwp.coreclient.network.data.ServerLogTail.install();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SENT.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        long t = GameClock.serverTicks(server);
        if (t % CHECK_TICKS != 0) return;
        boolean refresh = t % ROLE_REFRESH_TICKS == 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            pushIfChanged(p);
            if (refresh) fetchRole(p);
        }
    }

    /** Отправить уровень, если он отличается от отправленного ранее. */
    public static void pushIfChanged(ServerPlayer player) {
        if (PwpPermissions.level(player) != SENT.get(player.getUUID())) push(player);
    }

    private static void push(ServerPlayer player) {
        AccessLevel level = PwpPermissions.level(player);
        SENT.put(player.getUUID(), level);
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new AccessLevelPacket(level.name()));
    }

    private static void fetchRole(ServerPlayer player) {
        ServerThread.thenForPlayer(CoreApi.getPlayerRole(player.getStringUUID()), player, (online, resp) -> {
            AccessLevel role = parseRole(resp);
            if (role == null) {
                LOG.debug(
                        "Роль {} не получена из core-service — уровень только по op",
                        online.getGameProfile().getName());
                return;
            }
            PwpPermissions.setDbRole(online.getUUID(), role);
            pushIfChanged(online);
        });
    }

    /** {@code {data:{role}}} → уровень; нет ответа/роли — null. */
    static AccessLevel parseRole(JsonObject resp) {
        if (resp == null || !resp.has("data") || !resp.get("data").isJsonObject()) return null;
        JsonObject d = resp.getAsJsonObject("data");
        if (!d.has("role") || !d.get("role").isJsonPrimitive()) return null;
        return AccessLevel.fromRole(d.get("role").getAsString());
    }
}
