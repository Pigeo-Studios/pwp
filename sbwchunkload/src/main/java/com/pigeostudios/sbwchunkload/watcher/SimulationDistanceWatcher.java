package com.pigeostudios.sbwchunkload.watcher;

import com.pigeostudios.sbwchunkload.ticket.ChunkTicketManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Следит за simulation-distance сервера и сообщает актуальный уровень
 * тикета в ChunkTicketManager. Отдельный сервис: ни Tracker, ни TicketManager
 * сами конфиг сервера не читают.
 *
 * Уровень тикета = min(3, simulationDistance). КЛЮЧЕВОЕ: сущности в 1.20.1
 * тикаются только в чанках с distance <= simulationDistance — уровень выше
 * дал бы загруженный, но «мёртвый» чанк (инцидент 05.08.2026: лобби с
 * sim=2 и level 3). Первый вызов выполняется на 1-м тике (иначе первые
 * выстрелы получили бы уровень -1), дальше — раз в 100 тиков.
 */
@Mod.EventBusSubscriber(modid = "sbwchunkload", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SimulationDistanceWatcher {

    private static int ticketLevel = -1;
    private static int lastSimulationDistance = -1;
    private static int counter = 0;

    private SimulationDistanceWatcher() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();

        if (ticketLevel >= 0 && ++counter % 100 != 0) return;

        int simDistance = server.getPlayerList().getSimulationDistance();
        int level = Math.min(3, Math.max(1, simDistance));
        if (level != ticketLevel || simDistance != lastSimulationDistance) {
            lastSimulationDistance = simDistance;
            ticketLevel = level;
            System.out.println("[sbwchunkload] ticketLevel=" + ticketLevel
                + " (simulationDistance=" + simDistance + ")");
        }
        for (var entry : server.getAllLevels()) {
            ChunkTicketManager.of(entry).setTicketLevel(ticketLevel);
        }
    }
}
