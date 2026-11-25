package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.match.CombatRules;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.util.LogOnce;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Удар артиллерии командира. Раньше подтверждение лишь добавляло {@code ActiveStrike}: тикера не
 * было — взрывов не было, а зона на карте висела вечно и копилась в сохранениях.
 *
 * <p>Теперь (как в Squad): подлёт {@link CombatRules#STRIKE_DELAY_TICKS}, затем снаряд каждые
 * {@link CombatRules#STRIKE_INTERVAL_TICKS} в случайную точку круга {@code artStrikeRadius} вокруг
 * цели; по истечении удар снимается и состояние рассылается (только своей команде — TeamVisibility).
 * Ландшафт не ломается ({@code ExplosionInteraction.NONE}): урон — игрокам и технике.
 */
@EventBusSubscriber(modid = "pwpwarfare")
public final class ArtilleryTicker {
    private static final Logger LOG = LoggerFactory.getLogger(ArtilleryTicker.class);
    private static final float SHELL_POWER = 4.0F;

    private ArtilleryTicker() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            WarfareWorldData data = WarfareWorldData.get(level);
            if (data.match.activeStrikes.isEmpty() || data.match.isPaused) continue;
            tick(level, data);
        }
    }

    private static void tick(ServerLevel level, WarfareWorldData data) {
        boolean changed = false;
        int radius = WarfareConfig.ART_STRIKE_RADIUS.get();
        Iterator<WarfareWorldData.ActiveStrike> it = data.match.activeStrikes.iterator();
        while (it.hasNext()) {
            WarfareWorldData.ActiveStrike s = it.next();
            if (s.pos == null || s.ticksLeft > CombatRules.STRIKE_TOTAL_TICKS) {
                s.ticksLeft = Math.min(Math.max(s.ticksLeft, 0), CombatRules.STRIKE_TOTAL_TICKS);
            }
            if (s.pos != null && CombatRules.shellFires(s.ticksLeft)) fireShell(level, s.pos, radius);
            s.ticksLeft--;
            if (s.ticksLeft <= 0 || s.pos == null) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
        }
    }

    private static void fireShell(ServerLevel level, BlockPos center, int radius) {
        double[] d = CombatRules.shellOffset(level.random.nextDouble(), level.random.nextDouble(), radius);
        int x = center.getX() + (int) Math.round(d[0]);
        int z = center.getZ() + (int) Math.round(d[1]);
        BlockPos column = new BlockPos(x, center.getY(), z);
        if (!level.isLoaded(column)) return; // чанк не загружен — снаряд «ушёл в молоко», чанки не грузим
        try {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            level.explode(null, x + 0.5, y + 0.5, z + 0.5, SHELL_POWER, Level.ExplosionInteraction.NONE);
        } catch (RuntimeException e) {
            LogOnce.warn(LOG, "art-shell", "Разрыв снаряда артиллерии не удался", e);
        }
    }
}
