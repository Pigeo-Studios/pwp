package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreserver.CoreServerApi;
import com.pwp.coreserver.CoreServerMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Path;

/**
 * Heartbeat матч-сервера: раз в ~10 секунд сообщает core-service реальный онлайн,
 * тикеты и фазу матча. Лобби опрашивает эти данные и показывает их в GUI.
 */
public class MatchHeartbeatSender {

    private int ticks = 0;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new MatchHeartbeatSender());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!FMLEnvironment.dist.isDedicatedServer()) return;
        if (!CoreServerMod.isMatchServer) return;
        if (++ticks < 200) return;
        ticks = 0;

        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            ServerLevel level = server.overworld();
            if (level == null) return;

            WarfareWorldData data = WarfareWorldData.get(level);
            int online = server.getPlayerList().getPlayers().size();
            int port = server.getPort();
            if (port <= 0) port = 25565;

            String dir = Path.of("").toAbsolutePath().getFileName().toString();
            CoreServerApi.sendHeartbeat(
                    "match_" + dir,
                    online,
                    port,
                    data.currentMapImage != null ? data.currentMapImage : "",
                    data.gameMode != null ? data.gameMode : "aas",
                    data.blueFaction != null ? data.blueFaction : "",
                    data.redFaction != null ? data.redFaction : "",
                    data.blueTickets, data.redTickets,
                    data.isGameStarted ? "PLAYING" : "STARTING",
                    server.getPlayerList().getMaxPlayers(),
                    MatchStatsTracker.get().getStartedAt(),
                    online);
        } catch (Exception e) {
            // не ломаем тик из-за heartbeat
        }
    }
}
