package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.match.MatchWatchdog;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.ServerRole;
import com.pwp.api.time.GameClock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сторож матч-сервера на главном потоке (раз в секунду): правила — чистый {@link MatchWatchdog}, лимиты —
 * {@code emptyMatchShutdownMinutes}/{@code noShowShutdownMinutes}/{@code matchMaxMinutes} в
 * {@code pwpwarfare-common.toml}. Только на матч-сервере ({@link ServerRole#isMatchServer()}): на лобби матч-цикла нет.
 */
public final class MatchWatchdogTicker {
    private static final Logger LOG = LoggerFactory.getLogger(MatchWatchdogTicker.class);
    private static final long MIN = 60_000L;
    private static MatchWatchdog watchdog;

    private MatchWatchdogTicker() {}

    public static void tick(MinecraftServer server) {
        if (GameClock.serverTicks(server) % 20 != 0 || !ServerRole.isMatchServer()) return;
        if (watchdog == null) {
            watchdog = new MatchWatchdog(
                    WarfareConfig.EMPTY_MATCH_SHUTDOWN_MINUTES.get() * MIN,
                    WarfareConfig.NO_SHOW_SHUTDOWN_MINUTES.get() * MIN,
                    WarfareConfig.MATCH_MAX_MINUTES.get() * MIN);
        }
        ServerLevel level = server.overworld();
        WarfareWorldData data = WarfareWorldData.get(level);
        MatchStatsTracker tracker = MatchStatsTracker.get();
        boolean battle = data.match.isGameStarted && tracker.isActive();
        MatchWatchdog.Action action = watchdog.step(new MatchWatchdog.Input(
                GameClock.realMillis(),
                server.getPlayerCount(),
                LobbyHandoff.isAwaitingSettings(),
                MatchShutdown.returningToLobby(),
                battle,
                battle ? tracker.getStartedAt() : 0));
        switch (action) {
            case NONE -> {}
            case SHUTDOWN_EMPTY, SHUTDOWN_NO_SHOW -> {
                LOG.info("Сторож матча: {} — сервер останавливается, слот лобби освобождается", action);
                MatchShutdown.stopGracefully(server);
            }
            case END_BY_TIME -> {
                boolean blue = MatchWatchdog.blueWinsOnTime(data.match.blueTickets, data.match.redTickets);
                LOG.info(
                        "Сторож матча: лимит времени {} мин, победа {} по тикетам ({}:{})",
                        WarfareConfig.MATCH_MAX_MINUTES.get(),
                        blue ? "BLUE" : "RED",
                        data.match.blueTickets,
                        data.match.redTickets);
                GameLogicEvents.forceVictory(level, data, blue);
            }
        }
    }
}
