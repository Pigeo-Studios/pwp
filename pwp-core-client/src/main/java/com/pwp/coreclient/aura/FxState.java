package com.pwp.coreclient.aura;

import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * Плавные состояния движения игрока для донат-аур (v4): IDLE / MOVING / FLYING /
 * LANDING — не дискретные режимы, а непрерывные blend-факторы, сглаживаемые по
 * таймингам переходов из конфига (секция transitions, секунды). Обновление по
 * кадру из интерполированной позиции — никаких скачков при движении/полёте.
 *
 * Скорость считается по дельте ИНТЕРПОЛИРОВАННЫХ позиций между кадрами (это и
 * есть «интерполированное направление движения» из дизайна) и дополнительно
 * сглаживается экспоненциально — trail не дёргается и плавно изгибается при
 * смене направления.
 *
 * Все значения детерминированы: фазы/оффсеты эффектов берутся из hash(UUID) —
 * без случайности на кадр (см. запреты дизайна).
 */
public final class FxState {

    private final long seedBits;

    private double moveBlend;      // 0..1 — насколько игрок движется (бег/ходьба)
    private double flyBlend;       // 0..1 — насколько игрок в воздухе (полёт/падение)
    private double landing;        // 1 сразу после приземления -> 0 когда фаза завершена
    private double landingLeft;    // оставшиеся секунды фазы посадки (0 — не в фазе)

    private double vx, vz;         // сглаженная горизонтальная скорость (блоки/тик)
    private double lastIx, lastIz;
    private float lastTime = -1f;

    private int offGroundTicks;
    private boolean wasFlying;

    public FxState(UUID uuid) {
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        this.seedBits = hi ^ (lo * 0x9E3779B97F4A7C15L) ^ 0x5DEECE66DL;
    }

    /** Обновление по кадру: time — тики (gameTime + partialTick), ix/iz — интерполированная позиция. */
    public void update(float time, boolean onGround, double ix, double iz) {
        double dtTicks = lastTime < 0 ? 1.0 : Math.max(time - lastTime, 1.0 / 20.0);

        // Интерполированная скорость по дельте позиций между кадрами + экспоненциальное сглаживание
        double cvx = lastTime < 0 ? 0.0 : (ix - lastIx) / dtTicks;
        double cvz = lastTime < 0 ? 0.0 : (iz - lastIz) / dtTicks;
        double kv = 1.0 - Math.exp(-dtTicks / 4.0); // тау ~4 тика
        vx += (cvx - vx) * kv;
        vz += (cvz - vz) * kv;

        // MOVING: гистерезис по горизонтальной скорости (0.06 вкл / 0.03 выкл)
        double speed = Math.sqrt(vx * vx + vz * vz);
        boolean moving = speed > (moveBlend > 0.1 ? 0.03 : 0.06);
        moveBlend = smoothToward(moveBlend, moving ? 1.0 : 0.0, dtTicks,
                moving ? DonorFxConfig.tIdleToMoving() : DonorFxConfig.tMovingToIdle());

        // FLYING: грейс в несколько тиков — прыжок не вспыхивает как полёт
        offGroundTicks = onGround ? 0 : offGroundTicks + 1;
        boolean flying = offGroundTicks > 3;

        // Посадка: при касании земли после полёта запускается фаза LANDING,
        // в ней кольцо возвращается горизонтально быстрее (tFlyingToLanding)
        if (!flying && wasFlying) {
            landing = 1.0;
            landingLeft = DonorFxConfig.tLandingToIdle();
        }
        wasFlying = flying;
        double flyRate = landing > 0.0 ? DonorFxConfig.tFlyingToLanding() : DonorFxConfig.tFlyingToMoving();
        flyBlend = smoothToward(flyBlend, flying ? 1.0 : 0.0, dtTicks, flyRate);
        if (landingLeft > 0.0) {
            landingLeft -= dtTicks / 20.0;
            landing = landingLeft <= 0.0 ? 0.0
                    : landingLeft / DonorFxConfig.tLandingToIdle();
        }

        lastIx = ix;
        lastIz = iz;
        lastTime = time;
    }

    /** Экспоненциальное сглаживание: длина перехода = seconds (конфиг). */
    private static double smoothToward(double cur, double target, double dtTicks, double seconds) {
        double tau = Math.max(seconds, 0.05) * 20.0; // тики
        double k = 1.0 - Math.exp(-dtTicks / tau);
        return cur + (target - cur) * k;
    }

    public double moveBlend() {
        return moveBlend;
    }

    public double flyBlend() {
        return flyBlend;
    }

    /** Прогресс фазы LANDING: 1 сразу после приземления, 0 когда завершена. */
    public double landing() {
        return landing;
    }

    /** Сглаженная горизонтальная скорость (блоки/тик) — интерполированное направление движения. */
    public double vx() {
        return vx;
    }

    public double vz() {
        return vz;
    }

    public double speed() {
        return Math.sqrt(vx * vx + vz * vz);
    }

    /** Детерминированный 0..1-хэш по целому (фаза/оффсет эффекта) — «псевдослучайный offset» дизайна. */
    public double hash(int k) {
        return AuraGeom.hash01(seedBits ^ (k * 0x9E3779B97F4A7C15L) ^ 0x100000001B3L);
    }
}
