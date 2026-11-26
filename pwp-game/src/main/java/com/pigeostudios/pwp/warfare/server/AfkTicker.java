package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.match.AfkWatch;
import com.pwp.api.ServerRole;
import com.pwp.api.config.CommunitySettings;
import com.pwp.api.core.CoreApi;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.time.GameClock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Кик за AFK в матче (этап 10a): раз в секунду позиция и поворот головы каждого игрока → {@link AfkWatch}.
 * Порог — {@code community.afkKickMinutes} в {@code pwp-server.json} (0 — выключено), за
 * {@code afkWarnSeconds} — предупреждение. Не трогаем модераторов и выше, наблюдателей и креатив (админы,
 * камера). Только матч-серверы: в лобби ждать голосования — нормально. Кик записывается в историю (AFK).
 */
public final class AfkTicker {
    private static final Logger LOG = LoggerFactory.getLogger(AfkTicker.class);
    private static final AfkWatch WATCH = new AfkWatch();

    private AfkTicker() {}

    public static void tick(MinecraftServer server) {
        if (GameClock.serverTicks(server) % 20 != 0 || !ServerRole.isMatchServer()) return;
        CommunitySettings c = ServerRole.settings().community();
        int kick = c.afkKickMinutes() * 60;
        if (kick <= 0) return;
        long now = GameClock.realMillis();
        // копия: disconnect() на серверном потоке сразу убирает игрока из списка (executeBlocking →
        // handleDisconnection), обход живого списка дал бы ConcurrentModificationException
        for (ServerPlayer p : java.util.List.copyOf(server.getPlayerList().getPlayers())) {
            String id = p.getStringUUID();
            GameType mode = p.gameMode.getGameModeForPlayer();
            if (mode == GameType.SPECTATOR
                    || mode == GameType.CREATIVE
                    || PwpPermissions.has(p, AccessLevel.MODERATOR)) {
                WATCH.touch(id, now);
                continue;
            }
            switch (WATCH.update(
                    id, p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(), now, c.afkWarnSeconds(), kick)) {
                case WARN -> {
                    int left = WATCH.secondsLeft(id, now, kick);
                    p.sendSystemMessage(Component.translatable("pwpwarfare.msg.afk.warn", left)
                            .withStyle(ChatFormatting.GOLD));
                    p.displayClientMessage(
                            Component.translatable("pwpwarfare.msg.afk.warn", left)
                                    .withStyle(ChatFormatting.GOLD),
                            true);
                }
                case KICK -> {
                    LOG.info(
                            "AFK: {} отключён после {} мин без движения",
                            p.getGameProfile().getName(),
                            c.afkKickMinutes());
                    CoreApi.recordPunishment(id, "AFK", c.afkKickMinutes() + " min idle", null, null);
                    p.connection.disconnect(Component.translatable("pwpwarfare.msg.afk.kick", c.afkKickMinutes()));
                }
                case NONE -> {}
            }
        }
    }

    /** Игрок вышел — забыть; написал в чат/взаимодействовал — считается активностью. */
    public static void forget(ServerPlayer p) {
        WATCH.forget(p.getStringUUID());
    }

    public static void touch(ServerPlayer p) {
        WATCH.touch(p.getStringUUID(), GameClock.realMillis());
    }
}
