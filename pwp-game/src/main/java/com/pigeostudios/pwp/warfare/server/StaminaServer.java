package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.movement.StaminaGuard;
import com.pwp.api.ServerRole;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Выносливость на сервере (этап 17f): та же модель, что у {@code StaminaClient}, и те же условия (матч-сервер, не
 * креатив/наблюдатель, не в технике). Бег сверх выносливости дольше секунды (модифицированный клиент) — бег
 * выключается и даётся короткое замедление: клиенту не доверяем. Честный клиент сюда не попадает.
 */
public final class StaminaServer {
    private static final int SLOW_TICKS = 40;
    private static final Map<UUID, StaminaGuard> GUARDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ON_GROUND = new ConcurrentHashMap<>();

    private StaminaServer() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(StaminaServer::onTick);
        MinecraftForge.EVENT_BUS.addListener(StaminaServer::onLogout);
    }

    private static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        UUID id = p.getUUID();
        if (!ServerRole.isMatchServer() || p.isCreative() || p.isSpectator() || p.getVehicle() != null) {
            StaminaGuard g = GUARDS.get(id);
            if (g != null) g.reset();
            return;
        }
        boolean onGround = p.onGround();
        boolean was = ON_GROUND.getOrDefault(id, true);
        ON_GROUND.put(id, onGround);
        boolean jumped = was && !onGround && p.getDeltaMovement().y > 0.1;
        StaminaGuard g = GUARDS.computeIfAbsent(id, k -> new StaminaGuard());
        if (g.tick(p.isSprinting(), p.isCrouching(), jumped)) {
            p.setSprinting(false);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1, false, false, true));
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        GUARDS.remove(e.getEntity().getUUID());
        ON_GROUND.remove(e.getEntity().getUUID());
    }
}
