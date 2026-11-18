package com.pigeostudios.pwp.warfare.stats;

import com.pwp.api.time.GameClock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Ассисты: кто ранил жертву за последние {@link #WINDOW_TICKS} до убийства (кроме самого убийцы).
 * Раньше в награды всегда уходил 0. Состояние — в памяти (serverTicks), жертва чистится при смерти.
 */
@EventBusSubscriber(modid = "pwpwarfare")
public final class AssistTracker {
    static final long WINDOW_TICKS = 15 * 20L;
    /** жертва → (атакующий → serverTicks последнего урона) */
    private static final Map<UUID, Map<UUID, Long>> DAMAGE = new HashMap<>();

    private AssistTracker() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim) return;
        if (victim.getTeam() != null && victim.getTeam().isAlliedTo(attacker.getTeam())) return; // по своим — не ассист
        DAMAGE.computeIfAbsent(victim.getUUID(), k -> new HashMap<>())
                .put(attacker.getUUID(), GameClock.serverTicks(victim.server));
    }

    /** Атакующие, которым засчитать ассист за убийство {@code victim} игроком {@code killer}; запись жертвы сбрасывается. */
    static java.util.List<UUID> takeAssists(ServerPlayer victim, UUID killer) {
        Map<UUID, Long> hits = DAMAGE.remove(victim.getUUID());
        if (hits == null) return java.util.List.of();
        long now = GameClock.serverTicks(victim.server);
        java.util.List<UUID> out = new java.util.ArrayList<>();
        hits.forEach((who, at) -> {
            if (!who.equals(killer) && now - at <= WINDOW_TICKS) out.add(who);
        });
        return out;
    }

    static void clear() {
        DAMAGE.clear();
    }
}
