package com.pigeostudios.pwp.warfare.match;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Способности командира (этап 15 ROADMAP), как в Squad: у команды на каждую — свой кулдаун, вызывает только
 * лидер командного отряда. Время — серверные тики вызывающего ({@code GameClock.serverTicks}).
 *
 * <ul>
 *   <li>{@link Ability#UAV} — разведка с воздуха: враги в радиусе видны на карте команды {@code durationTicks};
 *   <li>{@link Ability#SUPPLY} — сброс ящика снабжения в точку;
 *   <li>{@link Ability#CAS} — авиаудар: серия мощных разрывов вдоль точки после подлёта.
 * </ul>
 */
public final class CommanderRules {
    public enum Ability {
        UAV(20 * 180, 20 * 30, 120),
        SUPPLY(20 * 240, 0, 0),
        CAS(20 * 420, 20 * 8, 25);

        /** Кулдаун на команду, тиков. */
        public final int cooldownTicks;
        /** UAV — длительность разведки; CAS — подлёт до первого разрыва. */
        public final int durationTicks;
        /** UAV — радиус разведки; CAS — полудлина линии удара (блоки). */
        public final int radius;

        Ability(int cooldownTicks, int durationTicks, int radius) {
            this.cooldownTicks = cooldownTicks;
            this.durationTicks = durationTicks;
            this.radius = radius;
        }

        public static Ability parse(String s) {
            if (s == null) return null;
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** Кулдауны команд: «команда|способность» → тик, когда снова можно. */
    public static final class Cooldowns {
        private final Map<String, Long> until = new HashMap<>();

        /** Тиков до готовности (0 — готова). */
        public long remaining(String team, Ability a, long now) {
            Long u = until.get(team + "|" + a);
            return u == null ? 0 : Math.max(0, u - now);
        }

        /** Использовать: true — запущено (кулдаун взведён), false — ещё на кулдауне. */
        public boolean tryUse(String team, Ability a, long now) {
            if (remaining(team, a, now) > 0) return false;
            until.put(team + "|" + a, now + a.cooldownTicks);
            return true;
        }

        public void reset() {
            until.clear();
        }
    }

    private CommanderRules() {}

    /** Точка в радиусе разведки UAV. */
    public static boolean revealed(double px, double pz, double cx, double cz, int radius) {
        double dx = px - cx;
        double dz = pz - cz;
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    /**
     * Точки авиаудара: {@code count} разрывов вдоль линии длиной {@code 2·radius} через центр по направлению
     * {@code yawDeg} (0 — на север, по часовой). Возвращает пары (x, z).
     */
    public static double[][] casLine(double cx, double cz, double yawDeg, int radius, int count) {
        double[][] out = new double[count][2];
        double rad = Math.toRadians(yawDeg);
        double dx = Math.sin(rad);
        double dz = -Math.cos(rad);
        for (int i = 0; i < count; i++) {
            double t = count == 1 ? 0 : -radius + 2.0 * radius * i / (count - 1);
            out[i][0] = cx + dx * t;
            out[i][1] = cz + dz * t;
        }
        return out;
    }
}
