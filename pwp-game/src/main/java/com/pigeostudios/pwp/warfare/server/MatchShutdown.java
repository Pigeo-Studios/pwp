package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreserver.TransferHosts;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Выключение матч-сервера после матча: таймер «вернуть всех в лобби и halt» (экран победы 15 с) и
 * сигнал остановки от лобби ({@code stop.signal}, проверка раз в секунду). Вынесено из
 * {@code GameLogicEvents.onServerTick} без изменений логики (M5, 19.11.2026).
 */
public final class MatchShutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchShutdown.class);
    private static int timer = -1;
    private static MinecraftServer target = null;

    private MatchShutdown() {}

    /** Через {@code ticks} тиков перенести всех в лобби и остановить сервер. */
    public static void schedule(MinecraftServer server, int ticks) {
        timer = ticks;
        target = server;
    }

    public static boolean returningToLobby() {
        return timer > 0;
    }

    /** Каждый тик сервера (END), из {@code GameLogicEvents.onServerTick}. */
    public static void tick(MinecraftServer server) {
        if (timer > 0) {
            timer--;
            if (timer == 0 && target != null) {
                int lobbyPort = WarfareConfig.LOBBY_PORT.get();
                for (ServerPlayer player : target.getPlayerList().getPlayers()) {
                    // Локальные игроки получают LAN-адрес лобби, внешние — домен
                    String lobbyHost = TransferHosts.resolveTransferHost(player);
                    player.sendSystemMessage(
                            Component.translatable("pwpwarfare.match.return_to_lobby")
                                    .withStyle(ChatFormatting.YELLOW),
                            false);
                    com.pwp.coreclient.network.PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new ConnectToServerPacket(lobbyHost, lobbyPort));
                }
                target.halt(false);
                target = null;
            }
        }

        // Check for admin stop signal from lobby
        // файл проверяется раз в секунду (раньше — Files.exists каждый тик главного потока)
        Path stopSignal = Path.of("stop.signal");
        if (GameClock.serverTicks(server) % 20 == 0 && Files.exists(stopSignal)) {
            LOGGER.info("Stop signal received, shutting down gracefully");
            try {
                MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
                WarfareWorldData matchData = null;
                for (ServerLevel sl : srv.getAllLevels()) {
                    matchData = WarfareWorldData.get(sl);
                    break;
                }
                if (matchData != null) {
                    if (matchData.match.isGameStarted) {
                        String leader = matchData.match.blueTickets > matchData.match.redTickets
                                ? "BLUE"
                                : (matchData.match.redTickets > matchData.match.blueTickets ? "RED" : "NONE");
                        MatchStatsTracker.get()
                                .finalizeMatch(leader, matchData.match.blueTickets, matchData.match.redTickets);
                    } else {
                        MatchStatsTracker tracker = MatchStatsTracker.get();
                        if (tracker.isActive()) {
                            tracker.finalizeMatch("NONE", matchData.match.blueTickets, matchData.match.redTickets);
                        } else if (timer <= 0) {
                            timer = 10;
                            target = srv;
                        }
                    }
                }
                Files.delete(stopSignal);
            } catch (Exception e) {
                LOGGER.warn("Failed to process stop signal: {}", e.getMessage());
            }
        }
    }
}
