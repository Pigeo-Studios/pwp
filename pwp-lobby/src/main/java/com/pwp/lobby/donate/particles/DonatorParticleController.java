package com.pwp.lobby.donate.particles;

import com.pwp.lobby.donate.DonatorStatusManager;
import com.pwp.lobby.match.MatchAllocator;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Серверные частицы донатеров/ролей в лобби.
 * Эффекты математические (без отдельных Entity), отправка per-viewer пакетами
 * ClientboundParticlePacket — чтобы LOD и общий бюджет считались по каждому зрителю.
 * Приоритет: ADMIN > MODERATOR > PLATINUM > GOLD > SILVER (партикл выбирается один).
 * Отключаются, пока активен матч (MatchAllocator).
 */
public final class DonatorParticleController {

    private static final Logger log = LoggerFactory.getLogger(DonatorParticleController.class);
    private static final double RING = Math.PI * 2.0;

    private static long worldTicks = 0;
    private static final Map<UUID, Double> ACCUM = new HashMap<>();
    private static int budgetLeft = 0;

    private DonatorParticleController() {}

    // Цвета
    private static final Vector3f C_SILVER = new Vector3f(0.45f, 0.45f, 0.45f);
    private static final Vector3f C_GOLD = new Vector3f(1.0f, 0.72f, 0.08f);
    private static final Vector3f C_PLATINUM = new Vector3f(0.435f, 0.714f, 0.84f);
    private static final Vector3f C_PLATINUM_SPARK = new Vector3f(1.0f, 1.0f, 1.0f);
    private static final Vector3f C_MODERATOR = new Vector3f(0.56f, 0.78f, 1.0f);
    private static final Vector3f C_RED_DARK = new Vector3f(0.31f, 0.0f, 0.0f);    // #520000
    private static final Vector3f C_RED = new Vector3f(0.627f, 0.0f, 0.0f);        // #A00000
    private static final Vector3f C_RED_BRIGHT = new Vector3f(1.0f, 0.126f, 0.126f); // #FF2020
    private static final Vector3f C_WHITE = new Vector3f(1.0f, 1.0f, 1.0f);

    /** Тик сервера (раз в тик, из LobbyMod). */
    public static void tick() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (MatchAllocator.hasActiveMatch()) return;
        worldTicks++;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        budgetLeft = RoleParticleConfig.GLOBAL_BUDGET.get();
        int lodFar = RoleParticleConfig.LOD_FAR.get();

        for (ServerPlayer donor : players) {
            String level = levelOf(donor);
            if (level == null) continue;
            try {
                List<ParticleEntry> fx = buildFx(donor, level);
                if (fx.isEmpty()) continue;
                for (ServerPlayer viewer : players) {
                    if (viewer.getUUID().equals(donor.getUUID())) continue;
                    double dist = viewer.distanceTo(donor);
                    if (dist > lodFar) continue;
                    send(viewer, fx, lodFactor(dist, lodFar));
                    if (budgetLeft <= 0) return;
                }
            } catch (Exception e) {
                log.warn("donor particle tick: {}", e.getMessage());
            }
        }
    }

    // ====== Выбор эффекта ======

    private static String levelOf(ServerPlayer p) {
        String role = DonatorStatusManager.roleOf(p).toLowerCase();
        if (role.equals("admin") || role.equals("owner")) {
            return RoleParticleConfig.ADMIN_ENABLED.get() ? "ADMIN" : null;
        }
        if (role.equals("support") || role.equals("moderator")) {
            return RoleParticleConfig.MODERATOR_ENABLED.get() ? "MODERATOR" : null;
        }
        String t = DonatorStatusManager.tierOf(p).toUpperCase();
        switch (t) {
            case "PLATINUM": return DonatorParticleConfig.PLATINUM_ENABLED.get() ? "PLATINUM" : null;
            case "GOLD": return DonatorParticleConfig.GOLD_ENABLED.get() ? "GOLD" : null;
            case "SILVER": return DonatorParticleConfig.SILVER_ENABLED.get() ? "SILVER" : null;
            default: return null;
        }
    }

    private static double rateOf(String level) {
        return switch (level) {
            case "ADMIN" -> RoleParticleConfig.ADMIN_RATE.get();
            case "MODERATOR" -> RoleParticleConfig.MODERATOR_RATE.get();
            case "PLATINUM" -> DonatorParticleConfig.PLATINUM_RATE.get();
            case "GOLD" -> DonatorParticleConfig.GOLD_RATE.get();
            default -> DonatorParticleConfig.SILVER_RATE.get();
        };
    }

    // ====== Генерация позиций ======

    private static List<ParticleEntry> buildFx(ServerPlayer p, String level) {
        int count = spawnCount(p.getUUID(), rateOf(level));
        if (count <= 0) return List.of();

        double x = p.getX(), yBase = p.getY(), z = p.getZ();
        long t = worldTicks;
        List<ParticleEntry> out = new ArrayList<>(count);

        return switch (level) {
            case "SILVER" -> silver(x, yBase, z, out, count);
            case "GOLD" -> gold(x, yBase, z, out, count, t);
            case "PLATINUM" -> platinum(x, yBase, z, out, count, t);
            case "MODERATOR" -> moderator(x, yBase, z, out, count, t);
            default -> admin(p, x, yBase, z, out, count, t);
        };
    }

    private static List<ParticleEntry> silver(double x, double y, double z, List<ParticleEntry> out, int n) {
        ParticleOptions opt = dust(C_SILVER, 1.1f);
        for (int i = 0; i < n; i++) {
            out.add(new ParticleEntry(opt, x + (Math.random() - 0.5) * 0.6, y + Math.random() * 1.6, z + (Math.random() - 0.5) * 0.6));
        }
        return out;
    }

    private static List<ParticleEntry> gold(double x, double y, double z, List<ParticleEntry> out, int n, long t) {
        ParticleOptions spiral = dust(C_GOLD, 1.3f);
        for (int i = 0; i < n; i++) {
            double angle = t * 0.35 + i * 0.9;
            double r = 0.5;
            out.add(new ParticleEntry(spiral,
                x + Math.cos(angle) * r,
                y + 0.7 + 0.3 * Math.sin(angle * 0.7),
                z + Math.sin(angle) * r));
        }
        return out;
    }

    private static List<ParticleEntry> platinum(double x, double y, double z, List<ParticleEntry> out, int n, long t) {
        // Кольцо 0.45 -> 0.70 -> 0.45 за 40 тиков (2с)
        double phase = (t % 40) / 40.0;
        double ringR = 0.45 + 0.25 * (1.0 - Math.abs(2.0 * phase - 1.0));
        ParticleOptions ring = dust(C_PLATINUM, 1.2f);
        ParticleOptions spark = dust(C_PLATINUM_SPARK, 0.8f);
        for (int i = 0; i < n; i++) {
            switch (i % 3) {
                case 0 -> { // кольцо
                    double a = t * 0.05 + (i / 3) * 0.55;
                    out.add(new ParticleEntry(ring, x + Math.cos(a) * ringR, y + 1.0, z + Math.sin(a) * ringR));
                }
                case 1 -> { // спираль 1
                    double a = t * 0.28 + i * 0.55;
                    out.add(new ParticleEntry(ring, x + Math.cos(a) * 0.6, y + 0.8 + 0.4 * Math.sin(a * 0.6), z + Math.sin(a) * 0.6));
                }
                default -> { // спираль 2
                    double a = -t * 0.2 - i * 0.55;
                    out.add(new ParticleEntry(ring, x + Math.cos(a) * 0.45, y + 1.3 + 0.3 * Math.cos(a * 0.8), z + Math.sin(a) * 0.45));
                }
            }
        }
        // последняя — искра
        out.set(out.size() - 1, new ParticleEntry(spark, x + (Math.random() - 0.5) * 1.0, y + 0.5 + Math.random() * 1.4, z + (Math.random() - 0.5) * 1.0));
        return out;
    }

    private static List<ParticleEntry> moderator(double x, double y, double z, List<ParticleEntry> out, int n, long t) {
        ParticleOptions opt = dust(C_MODERATOR, 1.2f);
        for (int i = 0; i < n; i++) {
            double a = t * 0.18 + i * 0.8;
            out.add(new ParticleEntry(opt, x + Math.cos(a) * 0.55, y + 0.9 + 0.35 * Math.sin(a * 0.5), z + Math.sin(a) * 0.55));
        }
        return out;
    }

    private static List<ParticleEntry> admin(ServerPlayer p, double x, double y, double z, List<ParticleEntry> out, int n, long t) {
        ParticleOptions red = dust(C_RED, 1.25f);
        ParticleOptions redBright = dust(C_RED_BRIGHT, 1.25f);
        ParticleOptions redDark = dust(C_RED_DARK, 1.0f);
        ParticleOptions white = dust(C_WHITE, 0.9f);

        // Сегментное кольцо у ног (вращается)
        for (int seg = 0; seg + 2 < n; seg += 3) {
            double a = t * 0.05 + seg * 1.2;
            out.add(new ParticleEntry(red, x + Math.cos(a) * 0.6, y + 0.12, z + Math.sin(a) * 0.6));
            out.add(new ParticleEntry(red, x + Math.cos(a + 0.4) * 0.6, y + 0.12, z + Math.sin(a + 0.4) * 0.6));
        }
        // Scan pulse каждые 12 тиков — яркое кольцо над головой
        if (t % 12 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = (i / 8.0) * RING;
                out.add(new ParticleEntry(redBright, x + Math.cos(a) * 0.75, y + 1.8, z + Math.sin(a) * 0.75));
            }
        }
        // Expanding pulse каждые 40 тиков — радиус 0.4 -> 1.2
        int e = (int) (t % 40);
        if (e < 8) {
            double radius = 0.4 + 0.8 * (e / 8.0);
            for (int i = 0; i < 6; i++) {
                double a = (i / 6.0) * RING + t * 0.1;
                out.add(new ParticleEntry(red, x + Math.cos(a) * radius, y + 0.6, z + Math.sin(a) * radius));
            }
        }
        // Вертикальные спарки
        if (out.size() < n) {
            int sparkCount = Math.min(n - out.size(), 3);
            for (int i = 0; i < sparkCount; i++) {
                out.add(new ParticleEntry(white, x + (Math.random() - 0.5) * 0.8, y + 0.2 + Math.random() * 1.7, z + (Math.random() - 0.5) * 0.8));
            }
        }
        // Трейл за движением
        var vel = p.getDeltaMovement();
        double vx = vel.x * 2.0, vz = vel.z * 2.0;
        if (vx * vx + vz * vz > 1e-4) {
            out.add(new ParticleEntry(redDark, x - vx, y + 0.6, z - vz));
        }
        if (out.size() > n) {
            return out.subList(0, n);
        }
        return out;
    }

    // ====== Дробный счётчик (rate в секунду -> штуки в тик) ======

    private static int spawnCount(UUID uuid, double ratePerSec) {
        double per = ratePerSec / 20.0;
        double a = ACCUM.merge(uuid, per, Double::sum);
        int n = (int) a;
        if (n > 0) ACCUM.put(uuid, a - n);
        return n;
    }

    // ====== Отправка per-viewer ======

    private static double lodFactor(double dist, int far) {
        if (dist <= 16.0) return 1.0;
        if (dist >= far) return 0.0;
        return 0.55; // 16..32 — 50-60% частиц
    }

    private static void send(ServerPlayer viewer, List<ParticleEntry> fx, double factor) {
        int count = Math.min(Math.max(1, (int) Math.ceil(fx.size() * factor)), fx.size());
        count = Math.min(count, budgetLeft);
        if (count <= 0) return;
        budgetLeft -= count;
        for (int i = 0; i < count; i++) {
            ParticleEntry e = fx.get(i);
            viewer.connection.send(new ClientboundLevelParticlesPacket(e.opt, false, e.x, e.y, e.z, 0f, 0f, 0f, 0f, 1));
        }
    }

    private static DustParticleOptions dust(Vector3f color, float scale) {
        return new DustParticleOptions(color, scale);
    }

    private static final class ParticleEntry {
        final ParticleOptions opt;
        final double x, y, z;
        ParticleEntry(ParticleOptions opt, double x, double y, double z) {
            this.opt = opt;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
