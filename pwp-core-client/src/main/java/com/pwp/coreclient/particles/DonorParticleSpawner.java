package com.pwp.coreclient.particles;

import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.DonatorCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Клиентский спавн донат-FX: по DonatorCache знаем уровни игроков и спавним частицы
 * свечения через штатный партикл-движок (level.addParticle) — без серверных пакетов.
 * Дробный аккумулятор по игроку (rate/сек -> штуки в тик), LOD по дистанции,
 * бюджет частиц на тик. Дизайн траекторий задаёт Spawner, поведение частицы — GlowParticle.
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DonorParticleSpawner {

    private static final double RING = Math.PI * 2.0;
    private static final Map<UUID, Double> ACCUM = new HashMap<>();

    private DonorParticleSpawner() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!DonorFxConfig.ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        ClientLevel level = (ClientLevel) mc.level;

        int far = DonorFxConfig.LOD_FAR.get();
        long farSq = (long) far * far;
        int budget = DonorFxConfig.BUDGET.get();
        int spawned = 0;

        for (Player p : level.players()) {
            String lvl = DonatorCache.levelOf(p.getUUID());
            if (lvl == null || !DonorFxConfig.enabled(lvl)) continue;
            double distSq = p.distanceToSqr(mc.player);
            if (distSq > farSq) continue;
            // Дальше 16 блоков — вдвое реже
            double rate = DonorFxConfig.rate(lvl) * (distSq < 256.0 ? 1.0 : 0.5);
            double accum = ACCUM.merge(p.getUUID(), rate / 20.0, Double::sum);
            int n = (int) accum;
            if (n <= 0) continue;
            ACCUM.put(p.getUUID(), accum - n);
            spawned += spawn(level, p, lvl, n);
            if (spawned >= budget) break;
        }
    }

    // ====== Паттерны эффектов по уровням ======

    private static int spawn(ClientLevel level, Player p, String lvl, int n) {
        return switch (lvl) {
            case "SILVER" -> silver(level, p, n);
            case "GOLD" -> gold(level, p, n);
            case "PLATINUM" -> platinum(level, p, n);
            case "MODERATOR" -> moderator(level, p, n);
            case "ADMIN" -> admin(level, p, n);
            default -> 0;
        };
    }

    /** SILVER — мягкий световой шорох: медленно всплывающие мотесы в облачке вокруг тела. */
    private static int silver(ClientLevel level, Player p, int n) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        var rnd = level.random;
        for (int i = 0; i < n; i++) {
            level.addParticle(PwpParticleTypes.SILVER.get(),
                    x + (rnd.nextDouble() - 0.5) * 0.6,
                    y + 0.2 + rnd.nextDouble() * 1.4,
                    z + (rnd.nextDouble() - 0.5) * 0.6,
                    (rnd.nextDouble() - 0.5) * 0.01,
                    0.02 + rnd.nextDouble() * 0.02,
                    (rnd.nextDouble() - 0.5) * 0.01);
        }
        return n;
    }

    /** GOLD — вращающееся кольцо-спираль свечений (тангенциальная скорость даёт орбиту). */
    private static int gold(ClientLevel level, Player p, int n) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        var rnd = level.random;
        long t = level.getGameTime();
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * RING;
            double r = 0.55;
            double h = 0.5 + 0.35 * Math.sin(a * 0.7 + t * 0.05);
            level.addParticle(PwpParticleTypes.GOLD.get(),
                    x + Math.cos(a) * r, y + h, z + Math.sin(a) * r,
                    -Math.sin(a) * 0.22, 0.01, Math.cos(a) * 0.22);
        }
        return n;
    }

    /** PLATINUM — пульсирующее гало над головой + орбитальные точки. */
    private static int platinum(ClientLevel level, Player p, int n) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        var rnd = level.random;
        long t = level.getGameTime();
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * RING;
            if (rnd.nextDouble() < 0.65) {
                double r = 0.45 + 0.1 * Math.abs(Math.sin(t * 0.05));
                level.addParticle(PwpParticleTypes.PLATINUM.get(),
                        x + Math.cos(a) * r, y + 1.85, z + Math.sin(a) * r,
                        -Math.sin(a) * 0.12, 0.0, Math.cos(a) * 0.12);
            } else {
                double r = 0.6;
                double h = 0.9 + 0.35 * Math.sin(a * 0.8 + t * 0.1);
                level.addParticle(PwpParticleTypes.PLATINUM.get(),
                        x + Math.cos(a) * r, y + h, z + Math.sin(a) * r,
                        -Math.sin(a) * 0.3, 0.01, Math.cos(a) * 0.3);
            }
        }
        return n;
    }

    /** MODERATOR — холодное облако мотесов вокруг тела. */
    private static int moderator(ClientLevel level, Player p, int n) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        var rnd = level.random;
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * RING;
            double r = 0.5 * rnd.nextDouble();
            level.addParticle(PwpParticleTypes.MODERATOR.get(),
                    x + Math.cos(a) * r, y + 0.3 + rnd.nextDouble() * 1.3, z + Math.sin(a) * r,
                    (rnd.nextDouble() - 0.5) * 0.015, 0.015, (rnd.nextDouble() - 0.5) * 0.015);
        }
        return n;
    }

    /** ADMIN — огненное шоу: восходящие искры, кольцо у ног, периодический пульс. */
    private static int admin(ClientLevel level, Player p, int n) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        var rnd = level.random;
        long t = level.getGameTime();
        boolean pulsePhase = t % 40 < 5;
        for (int i = 0; i < n; i++) {
            int roll = rnd.nextInt(3);
            double a = rnd.nextDouble() * RING;
            if (pulsePhase && roll == 2) {
                // Расширяющееся кольцо-пульс
                double r = 0.15 + rnd.nextDouble() * 0.2;
                double v = 0.35 + rnd.nextDouble() * 0.3;
                level.addParticle(PwpParticleTypes.ADMIN.get(),
                        x + Math.cos(a) * r, y + 0.7, z + Math.sin(a) * r,
                        Math.cos(a) * v, 0.02, Math.sin(a) * v);
            } else if (roll == 0) {
                // Восходящая искра
                level.addParticle(PwpParticleTypes.ADMIN.get(),
                        x + (rnd.nextDouble() - 0.5) * 0.3, y + 0.15, z + (rnd.nextDouble() - 0.5) * 0.3,
                        (rnd.nextDouble() - 0.5) * 0.02, 0.05 + rnd.nextDouble() * 0.04,
                        (rnd.nextDouble() - 0.5) * 0.02);
            } else {
                // Сегменты огненного кольца у ног
                double r = 0.65;
                level.addParticle(PwpParticleTypes.ADMIN.get(),
                        x + Math.cos(a) * r, y + 0.1, z + Math.sin(a) * r,
                        -Math.sin(a) * 0.3, 0.02, Math.cos(a) * 0.3);
            }
        }
        return n;
    }
}
