package com.pwp.coreserver.community;

import com.pwp.api.ServerRole;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.time.GameClock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Резервные слоты матч-сервера (этап 10b). Лобби пишет {@code max-players} = публичный лимит + резерв и само
 * держит очередь; этот класс — страховка на случай, когда игрок пришёл мимо очереди (прямой коннект, гонка
 * heartbeat): через 5 с после входа (роль из БД успевает загрузиться) обычный игрок сверх публичного лимита
 * отключается с просьбой вернуться в лобби. Модераторы и выше занимают резерв. Только матч-серверы.
 */
public final class ReservedSlots {
    private static final Logger LOG = LoggerFactory.getLogger(ReservedSlots.class);
    private static final long CHECK_DELAY_TICKS = 100;
    private static final Map<UUID, Long> PENDING = new LinkedHashMap<>();

    private ReservedSlots() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ReservedSlots::onLogin);
        MinecraftForge.EVENT_BUS.addListener(ReservedSlots::onTick);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !ServerRole.isMatchServer()) return;
        if (ServerRole.settings().community().reservedSlots() <= 0) return;
        synchronized (PENDING) {
            PENDING.put(p.getUUID(), GameClock.serverTicks(p.server) + CHECK_DELAY_TICKS);
        }
    }

    private static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        MinecraftServer server = e.getServer();
        long now = GameClock.serverTicks(server);
        int reserved = ServerRole.settings().community().reservedSlots();
        int publicCap = Math.max(1, server.getMaxPlayers() - reserved);
        synchronized (PENDING) {
            for (Iterator<Map.Entry<UUID, Long>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Long> en = it.next();
                if (en.getValue() > now) continue;
                it.remove();
                ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
                if (p == null || PwpPermissions.has(p, AccessLevel.MODERATOR)) continue;
                long regular = server.getPlayerList().getPlayers().stream()
                        .filter(o -> !PwpPermissions.has(o, AccessLevel.MODERATOR))
                        .count();
                if (regular > publicCap) {
                    LOG.info(
                            "Резервные слоты: {} сверх лимита {} — отключён",
                            p.getGameProfile().getName(),
                            publicCap);
                    p.connection.disconnect(Component.translatable("pwp_core.reserved.full"));
                }
            }
        }
    }
}
