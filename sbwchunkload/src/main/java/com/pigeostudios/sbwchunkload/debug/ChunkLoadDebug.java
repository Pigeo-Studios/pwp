package com.pigeostudios.sbwchunkload.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.pigeostudios.sbwchunkload.api.ProjectileTracker;
import com.pigeostudios.sbwchunkload.classifier.ProjectileClassifier;
import com.pigeostudios.sbwchunkload.config.ChunkLoadingConfig;
import com.pigeostudios.sbwchunkload.drone.DroneTracker;
import com.pigeostudios.sbwchunkload.ticket.ChunkTicketManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.text.DecimalFormat;

/**
 * Debug-инструментарий: команда /sbwchunkload status (permission 2) —
 * сразу видно, работает ли система: сколько снарядов трекается, сколько
 * чанков держится тикетами, сколько реальных обновлений тикетов в секунду.
 */
@Mod.EventBusSubscriber(modid = "sbwchunkload", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChunkLoadDebug {

    private static final DecimalFormat FMT = new DecimalFormat("#,##0");

    private ChunkLoadDebug() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("sbwchunkload")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(ChunkLoadDebug::status))
                .then(Commands.literal("config").executes(ChunkLoadDebug::config))
        );
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StringBuilder sb = new StringBuilder();
        sb.append("§6[sbwchunkload] §fстатус:\n");

        ServerLevel level = source.getLevel();
        ChunkTicketManager manager = ChunkTicketManager.of(level);

        double refreshesPerSec = manager.totalRefreshes() / 20.0;
        sb.append("  трекается снарядов: §e").append(ProjectileTracker.trackedCount()).append("\n");
        sb.append("  трекается дронов: §e").append(DroneTracker.trackedCount()).append("\n");
        sb.append("  в расписании (bucket'ы): §e").append(ProjectileTracker.scheduledCount()).append("\n");
        sb.append("  класс-кэш классификатора: §e").append(ProjectileClassifier.cacheSize()).append("\n");
        sb.append("  тикетов в кэше: §e").append(manager.cache().size())
            .append(" §7(активных: §e").append(manager.cache().activeCount()).append("§7)\n");
        sb.append("  уровень тикета: §e").append(manager.ticketLevel()).append("\n");
        sb.append("  refresh'ей тикетов: §e").append(FMT.format(manager.totalRefreshes()))
            .append(" §7(~").append(FMT.format(refreshesPerSec)).append("/сек)\n");
        sb.append("  пропусков smart: §e").append(FMT.format(manager.totalSkipsSmart()))
            .append(" §7(чанк уже тикается)\n");
        sb.append("  пропусков дедуп: §e").append(FMT.format(manager.totalSkipsDedup())).append("\n");
        sb.append("  пропусков граница (world border): §e").append(FMT.format(manager.totalSkipsBorder())).append("\n");
        sb.append("  пропусков лимит тикетов: §e").append(FMT.format(manager.totalSkipsLimit())).append("\n");
        sb.append("  пересчётов коридоров: §e").append(FMT.format(ProjectileTracker.updatesProcessed()))
            .append(" §7(пропущено: §e").append(FMT.format(ProjectileTracker.updatesSkipped())).append("§7)\n");
        sb.append("  удалено снарядов: §e").append(FMT.format(ProjectileTracker.removals())).append("\n");
        sb.append("  снятий: граница §e").append(FMT.format(ProjectileTracker.droppedBorder()))
            .append("§7 | лимит дальности §e").append(FMT.format(ProjectileTracker.droppedDistance()))
            .append("§7 | возраст §e").append(FMT.format(ProjectileTracker.droppedAge()))
            .append("§7 | стационар §e").append(FMT.format(ProjectileTracker.droppedStationary())).append("\n");
        sb.append("  не зарегистрировано (лимит трека): §e").append(FMT.format(ProjectileTracker.limitSkips())).append("\n");

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int config(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StringBuilder sb = new StringBuilder("§6[sbwchunkload] §fконфиг:\n");
        sb.append("  enabled: §e").append(ChunkLoadingConfig.ENABLED.get()).append("\n");
        sb.append("  smartChunkLoading: §e").append(ChunkLoadingConfig.SMART_CHUNK_LOADING.get()).append("\n");
        sb.append("  adaptiveLookahead: §e").append(ChunkLoadingConfig.ADAPTIVE_LOOKAHEAD.get())
            .append(" §7(mult: §e").append(ChunkLoadingConfig.PREDICTION_MULTIPLIER.get()).append("§7)\n");
        sb.append("  corridorWidth: §e").append(ChunkLoadingConfig.CORRIDOR_WIDTH.get()).append("\n");
        sb.append("  ticketTTL: §e").append(ChunkLoadingConfig.TICKET_TTL_TICKS.get()).append(" тиков\n");
        sb.append("  лимиты: дальность §e").append(ChunkLoadingConfig.MAX_TRACK_DISTANCE_BLOCKS.get())
            .append("§7 блоков | возраст §e").append(ChunkLoadingConfig.MAX_TRACK_AGE_TICKS.get())
            .append("§7 тиков | трек §e").append(ChunkLoadingConfig.MAX_TRACKED_PROJECTILES.get())
            .append("§7 | тикеты §e").append(ChunkLoadingConfig.MAX_ACTIVE_TICKETS.get())
            .append("§7 | стационар §e").append(ChunkLoadingConfig.STATIONARY_THRESHOLD.get()).append("\n");
        sb.append("  bullet: §e").append(ChunkLoadingConfig.BULLET_INTERVAL.get())
            .append("т [").append(ChunkLoadingConfig.BULLET_MIN_LOOKAHEAD.get())
            .append("-").append(ChunkLoadingConfig.BULLET_MAX_LOOKAHEAD.get()).append("]\n");
        sb.append("  rocket: §e").append(ChunkLoadingConfig.ROCKET_INTERVAL.get())
            .append("т [").append(ChunkLoadingConfig.ROCKET_MIN_LOOKAHEAD.get())
            .append("-").append(ChunkLoadingConfig.ROCKET_MAX_LOOKAHEAD.get()).append("]\n");
        sb.append("  heavy: §e").append(ChunkLoadingConfig.HEAVY_INTERVAL.get())
            .append("т [").append(ChunkLoadingConfig.HEAVY_MIN_LOOKAHEAD.get())
            .append("-").append(ChunkLoadingConfig.HEAVY_MAX_LOOKAHEAD.get()).append("]\n");
        sb.append("  ballistic: §e").append(ChunkLoadingConfig.BALLISTIC_INTERVAL.get())
            .append("т [").append(ChunkLoadingConfig.BALLISTIC_MIN_LOOKAHEAD.get())
            .append("-").append(ChunkLoadingConfig.BALLISTIC_MAX_LOOKAHEAD.get()).append("]\n");
        sb.append("  drone: §e").append(ChunkLoadingConfig.DRONE_INTERVAL.get())
            .append("т lookahead §e").append(ChunkLoadingConfig.DRONE_LOOKAHEAD.get())
            .append("§7 | лимит дронов §e").append(ChunkLoadingConfig.MAX_TRACKED_DRONES.get()).append("\n");
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}
