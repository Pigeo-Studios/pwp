package com.pwp.coreclient.particles;

import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.DonatorCache;
import com.pwp.coreclient.donor.DonorLevel;
import com.pwp.coreclient.donor.FxPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Клиентский спавн донат-FX: по DonatorCache знаем уровни игроков, уровень = взвешенный
 * набор FxPattern (см. STYLES), каждая частица — через штатный партикл-движок.
 * Дробный аккумулятор — по (игрок, паттерн): доли паттернов не теряются и не дают
 * «залпов». Гигиена: записи игроков вне level.players() чистятся каждый тик, накопление
 * ограничено. Бюджет — честный round-robin со сдвигом начала обхода. LOD — плавное
 * затухание rate от 16 блоков до LOD_FAR, без ступенек.
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DonorParticleSpawner {

    /** Накопленные доли частиц: uuid игрока -> паттерн -> дробное число к спавну. */
    private static final Map<UUID, Map<FxPattern, Double>> ACCUM = new HashMap<>();

    /** Кап накопления на паттерн (защита от «залпа» после паузы/лаг-спайка). */
    private static final double ACCUM_CAP = 3.0;

    /** Кап спавнов на одного игрока за тик. */
    private static final int PER_PLAYER_CAP = 4;

    /** Зона LOD без затухания (внутри — полная плотность). */
    private static final double LOD_FULL = 16.0;

    /** Взвешенные паттерны уровней. Паттерны — синглтоны уровня (ключи аккумуляторов). */
    private static final Map<DonorLevel, LinkedHashMap<FxPattern, Integer>> STYLES = buildStyles();

    private DonorParticleSpawner() {}

    private static Map<DonorLevel, LinkedHashMap<FxPattern, Integer>> buildStyles() {
        Map<DonorLevel, LinkedHashMap<FxPattern, Integer>> m = new EnumMap<>(DonorLevel.class);
        m.put(DonorLevel.SILVER, style(
                // Серебряная пыль вокруг тела + редкие восходящие искры (раз в ~4.5с вспышка)
                entry(new FxPattern.Cloud(PwpParticleTypes.SILVER.get(), 0.35, 0.1, 1.5, 0.03, 0.01), 4),
                entry(new FxPattern.Gated(new FxPattern.SparkUp(PwpParticleTypes.SILVER.get(), 0.05, 0.16, 0.30), 90, 12, 4), 1)));
        m.put(DonorLevel.GOLD, style(
                // Золотая вращающаяся спираль вокруг тела + периодические вспышки (раз в ~4.5с)
                entry(new FxPattern.Orbit(PwpParticleTypes.GOLD.get(), 0.55, 0.5, 1.15, 0.24, 0.02, 0.35), 4),
                entry(new FxPattern.Gated(new FxPattern.Burst(PwpParticleTypes.GOLD.get(), 0.22, 0.15, 0.4, 1.1), 90, 10, 5), 1)));
        m.put(DonorLevel.PLATINUM, style(
                // Аура: кольцо у ног + две встречные спирали + редкие яркие всплески (раз в ~6.5с)
                entry(new FxPattern.Ring(PwpParticleTypes.PLATINUM.get(), 0.5, 0.15, 0.35, 0, 0), 1),
                entry(new FxPattern.Orbit(PwpParticleTypes.PLATINUM.get(), 0.55, 0.5, 1.0, 0.26, 0.015, 0.4), 2),
                entry(new FxPattern.Orbit(PwpParticleTypes.PLATINUM.get(), 0.55, 0.5, 1.0, -0.26, 0.015, 0.2), 2),
                entry(new FxPattern.Gated(new FxPattern.Burst(PwpParticleTypes.PLATINUM.get(), 0.3, 0.2, 0.5, 1.4), 130, 8, 6), 1)));
        m.put(DonorLevel.MODERATOR, style(
                // Спокойная служебная аура: лёгкая пыль + небольшая спираль, без агрессивных эффектов
                entry(new FxPattern.Cloud(PwpParticleTypes.MODERATOR.get(), 0.4, 0.15, 1.4, 0.025, 0.01), 3),
                entry(new FxPattern.Orbit(PwpParticleTypes.MODERATOR.get(), 0.35, 0.4, 0.8, 0.18, 0.01, 0.3), 2)));
        m.put(DonorLevel.ADMIN, style(
                // Агрессивный Scanner: сегментированное кольцо у ног, вращающиеся дуги,
                // полоса сканирования снизу вверх, резкие импульсы наружу, искры при беге
                entry(new FxPattern.Ring(PwpParticleTypes.ADMIN.get(), 0.7, 0.1, 0.5, 8, 0.45), 2),
                entry(new FxPattern.ScanArc(PwpParticleTypes.ADMIN.get(), 0.85, 0.9, 1.3, 0.55, 0.4, 2), 2),
                entry(new FxPattern.ScanBand(PwpParticleTypes.ADMIN.get(), 0.55, 0.55, 0.018, 2.2), 1),
                entry(new FxPattern.Gated(new FxPattern.PulseRing(PwpParticleTypes.ADMIN.get(), 0.45, 0.15, 0.7), 40, 6, 7), 1),
                entry(new FxPattern.MotionSparks(PwpParticleTypes.ADMIN.get(), 0.25, 0.14), 2)));
        return m;
    }

    private static LinkedHashMap<FxPattern, Integer> style(FxPattern.Weighted... entries) {
        LinkedHashMap<FxPattern, Integer> m = new LinkedHashMap<>();
        for (FxPattern.Weighted e : entries) m.put(e.pattern(), e.weight());
        return m;
    }

    private static FxPattern.Weighted entry(FxPattern pattern, int weight) {
        return new FxPattern.Weighted(pattern, weight);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!DonorFxConfig.ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        ClientLevel level = (ClientLevel) mc.level;
        java.util.List<? extends Player> players = level.players();
        if (players.isEmpty()) return;

        int far = DonorFxConfig.LOD_FAR.get();
        double farD = far;
        int budget = DonorFxConfig.BUDGET.get();
        long t = level.getGameTime();
        int spawned = 0;

        // Гигиена: записи игроков вне level.players() чистим ДО бюджет-break,
        // чтобы у хвоста очереди аккумуляторы не стирались каждый тик
        Set<UUID> uuids = new HashSet<>(players.size());
        for (Player p : players) uuids.add(p.getUUID());
        ACCUM.keySet().retainAll(uuids);

        // Честный round-robin: начало обхода сдвигается каждый тик
        int start = (int) (t % players.size());
        for (int k = 0; k < players.size() && spawned < budget; k++) {
            Player p = players.get((start + k) % players.size());
            DonorLevel lvl = DonorLevel.byName(DonatorCache.levelOf(p.getUUID()));
            if (lvl == null || !DonorFxConfig.enabled(lvl)) continue;
            double dist = Math.sqrt(p.distanceToSqr(mc.player));
            if (dist > farD) continue;
            // Плавное затухание плотности от LOD_FULL до LOD_FAR
            double fade = dist <= LOD_FULL ? 1.0 : 1.0 - (dist - LOD_FULL) / (farD - LOD_FULL);
            if (fade <= 0.0) continue;
            double rate = DonorFxConfig.rate(lvl) * fade / 20.0;

            LinkedHashMap<FxPattern, Integer> styles = STYLES.get(lvl);
            Map<FxPattern, Double> acc = ACCUM.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
            acc.keySet().retainAll(styles.keySet());

            // Сумма динамических долей (паттерны с share=0 сейчас не участвуют)
            long totalW = 0;
            for (Map.Entry<FxPattern, Integer> e : styles.entrySet()) {
                totalW += (long) e.getKey().share(level, p, t) * e.getValue();
            }
            if (totalW == 0) continue;

            int perPlayer = Math.min(PER_PLAYER_CAP, budget - spawned);
            for (Map.Entry<FxPattern, Integer> e : styles.entrySet()) {
                FxPattern pat = e.getKey();
                int share = pat.share(level, p, t);
                if (share <= 0) continue;
                double accV = acc.merge(pat, rate * share * e.getValue() / totalW, Double::sum);
                if (accV >= 1.0) {
                    int n = Math.min((int) Math.min(accV, ACCUM_CAP), perPlayer);
                    acc.put(pat, Math.min(accV - n, ACCUM_CAP));
                    if (n > 0) {
                        pat.emit(level, p, level.random, n, t);
                        spawned += n;
                        perPlayer -= n;
                        if (perPlayer <= 0 || spawned >= budget) break;
                    }
                }
            }
        }
    }
}
