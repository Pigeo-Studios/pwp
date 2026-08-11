package com.pwp.coreclient.donor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Декларативный паттерн-движок донат-FX. Уровень = взвешенный набор паттернов
 * (см. DonorParticleSpawner.STYLES): каждый паттерн знает, как красиво рисовать
 * свою часть эффекта на штатном партикл-движке (level.addParticle).
 * share() — динамическая доля уровня (0 = сейчас не спавнить, >1 — импульсный
 * множитель для фазовых всплесков). Параметры калибруются в коде паттерна,
 * цвета/плотность — конфигом.
 */
public interface FxPattern {

    /** Множитель доли в текущий момент (0 = не спавнить, N — усиление в фазе). */
    default int share(ClientLevel level, Player p, long gameTime) {
        return 1;
    }

    /** Спавн n частиц эффекта относительно игрока. */
    void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime);

    /** Пара (паттерн, вес) для сборки стилей уровней. */
    record Weighted(FxPattern pattern, int weight) {}

    /** Фазовая обёртка: паттерн активен (с множителем mult) только в окне every/len, иначе 0. */
    record Gated(FxPattern inner, int every, int len, int mult) implements FxPattern {
        @Override
        public int share(ClientLevel level, Player p, long gameTime) {
            if (inner.share(level, p, gameTime) <= 0) return 0;
            return every <= 0 || gameTime % every < len ? mult : 0;
        }

        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            inner.emit(level, p, rnd, n, gameTime);
        }
    }

    /** Облако мотесов вокруг тела: медленно всплывает с лёгким дрейфом (пыль). */
    record Cloud(SimpleParticleType type, double radius, double yMin, double ySpan, double rise, double drift) implements FxPattern {
        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            double x = p.getX(), y = p.getY(), z = p.getZ();
            for (int i = 0; i < n; i++) {
                level.addParticle(type,
                        x + (rnd.nextDouble() - 0.5) * radius * 2.0,
                        y + yMin + rnd.nextDouble() * ySpan,
                        z + (rnd.nextDouble() - 0.5) * radius * 2.0,
                        (rnd.nextDouble() - 0.5) * drift,
                        rise * (0.5 + rnd.nextDouble()),
                        (rnd.nextDouble() - 0.5) * drift);
            }
        }
    }

    /** Винтовая орбита: частицы на спирали, тангенциальная скорость даёт вращение, подъём — спираль. */
    record Orbit(SimpleParticleType type, double radius, double yBase, double heightSpan, double angVel, double rise, double phaseSpeed) implements FxPattern {
        private static final double TAU = Math.PI * 2.0;

        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            double x = p.getX(), y = p.getY(), z = p.getZ();
            for (int i = 0; i < n; i++) {
                double a = rnd.nextDouble() * TAU;
                double h = yBase + ((a + gameTime * 0.05 * phaseSpeed) % TAU) / TAU * heightSpan;
                double tx = -Math.sin(a), tz = Math.cos(a);
                level.addParticle(type,
                        x + tx * radius, y + h, z + tz * radius,
                        tx * angVel * radius, rise, tz * angVel * radius);
            }
        }
    }

    /** Плоское кольцо (сегментированное при segments > 0, arcSpan — доля дуги сегмента). */
    record Ring(SimpleParticleType type, double radius, double y, double angVel, int segments, double arcSpan) implements FxPattern {
        private static final double TAU = Math.PI * 2.0;

        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            double x = p.getX(), y0 = p.getY(), z = p.getZ();
            for (int i = 0; i < n; i++) {
                double a;
                if (segments > 0) {
                    int s = rnd.nextInt(segments);
                    a = s * TAU / segments + (rnd.nextDouble() - 0.5) * arcSpan * TAU / segments;
                } else {
                    a = rnd.nextDouble() * TAU;
                }
                double tx = -Math.sin(a), tz = Math.cos(a);
                level.addParticle(type,
                        x + tx * radius, y0 + y, z + tz * radius,
                        tx * angVel * radius, 0.012, tz * angVel * radius);
            }
        }
    }

    /** Вращающиеся сканирующие дуги: count дуг sweep'а по кругу на высоте y с вертикальным разбросом. */
    record ScanArc(SimpleParticleType type, double radius, double yBase, double ySpan, double angVel, double arcLen, int count) implements FxPattern {
        private static final double TAU = Math.PI * 2.0;

        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            double x = p.getX(), y = p.getY(), z = p.getZ();
            for (int i = 0; i < n; i++) {
                double base = gameTime * angVel + rnd.nextInt(count) * TAU / count;
                double a = base + (rnd.nextDouble() - 0.5) * arcLen;
                double tx = -Math.sin(a), tz = Math.cos(a);
                level.addParticle(type,
                        x + tx * radius, y + yBase + (rnd.nextDouble() - 0.5) * ySpan, z + tz * radius,
                        tx * angVel * radius, 0.0, tz * angVel * radius);
            }
        }
    }

    /** Вертикальная полоса сканирования снизу вверх (y = (t*speed) mod height). */
    record ScanBand(SimpleParticleType type, double width, double depth, double speed, double height) implements FxPattern {
        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            double x = p.getX(), y = p.getY(), z = p.getZ();
            double h = (gameTime * speed) % height;
            for (int i = 0; i < n; i++) {
                level.addParticle(type,
                        x + (rnd.nextDouble() - 0.5) * width,
                        y + 0.15 + h,
                        z + (rnd.nextDouble() - 0.5) * depth,
                        0.0, 0.006, 0.0);
            }
        }
    }

    /** Резкий импульс наружу: частицы от центра с радиальной скоростью (пульс-кольцо). */
    record PulseRing(SimpleParticleType type, double impulse, double startRadius, double y) implements FxPattern {
        private static final double TAU = Math.PI * 2.0;

        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            double x = p.getX(), y0 = p.getY(), z = p.getZ();
            for (int i = 0; i < n; i++) {
                double a = rnd.nextDouble() * TAU;
                double v = impulse * (0.6 + rnd.nextDouble() * 0.8);
                level.addParticle(type,
                        x + Math.cos(a) * startRadius, y0 + y + (rnd.nextDouble() - 0.5) * 0.15, z + Math.sin(a) * startRadius,
                        Math.cos(a) * v, 0.02, Math.sin(a) * v);
            }
        }
    }

    /** Восходящие искры: редкие, из области у ног. */
    record SparkUp(SimpleParticleType type, double xSpan, double speedMin, double speedMax) implements FxPattern {
        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            double x = p.getX(), y = p.getY(), z = p.getZ();
            for (int i = 0; i < n; i++) {
                level.addParticle(type,
                        x + (rnd.nextDouble() - 0.5) * xSpan, y + 0.1, z + (rnd.nextDouble() - 0.5) * xSpan,
                        (rnd.nextDouble() - 0.5) * 0.015,
                        speedMin + rnd.nextDouble() * (speedMax - speedMin),
                        (rnd.nextDouble() - 0.5) * 0.015);
            }
        }
    }

    /** Всплеск: радиальные яркие частицы из точки (периодические вспышки). */
    record Burst(SimpleParticleType type, double impulse, double startRadius, double yMin, double ySpan) implements FxPattern {
        private static final double TAU = Math.PI * 2.0;

        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            double x = p.getX(), y = p.getY(), z = p.getZ();
            for (int i = 0; i < n; i++) {
                double a = rnd.nextDouble() * TAU;
                double v = impulse * (0.6 + rnd.nextDouble() * 0.8);
                level.addParticle(type,
                        x + Math.cos(a) * startRadius, y + yMin + rnd.nextDouble() * ySpan, z + Math.sin(a) * startRadius,
                        Math.cos(a) * v, 0.03, Math.sin(a) * v);
            }
        }
    }

    /** Искры при движении: доля 0 в покое, при беге — быстрые искры сзади по ходу движения. */
    record MotionSparks(SimpleParticleType type, double xSpan, double speed) implements FxPattern {
        @Override
        public int share(ClientLevel level, Player p, long gameTime) {
            return p.getDeltaMovement().horizontalDistanceSqr() > 0.05 ? 1 : 0;
        }

        @Override
        public void emit(ClientLevel level, Player p, RandomSource rnd, int n, long gameTime) {
            var m = p.getDeltaMovement();
            double x = p.getX(), y = p.getY(), z = p.getZ();
            for (int i = 0; i < n; i++) {
                level.addParticle(type,
                        x - m.x * 0.25 + (rnd.nextDouble() - 0.5) * xSpan, y + 0.1 + rnd.nextDouble() * 0.3, z - m.z * 0.25 + (rnd.nextDouble() - 0.5) * xSpan,
                        -m.x * speed + (rnd.nextDouble() - 0.5) * 0.02,
                        0.04 + rnd.nextDouble() * 0.04,
                        -m.z * speed + (rnd.nextDouble() - 0.5) * 0.02);
            }
        }
    }
}
