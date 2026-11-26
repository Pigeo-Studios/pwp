package com.pigeostudios.pwp.warfare.match;

import java.util.HashMap;
import java.util.Map;

/**
 * Кик за AFK в матче (этап 10 ROADMAP). Вызывающий раз в секунду сообщает «двигался ли игрок» (позиция,
 * поворот головы); после {@code kickSeconds} без активности — KICK, за {@code warnSeconds} до этого — один раз
 * WARN. Любая активность сбрасывает счёт и разрешает новое предупреждение. Время — мс вызывающего.
 */
public final class AfkWatch {
    public enum Action {
        NONE,
        WARN,
        KICK
    }

    private static final class State {
        double x, y, z;
        float yaw, pitch;
        long lastActive;
        boolean warned;
    }

    private final Map<String, State> states = new HashMap<>();

    /**
     * Отметить позицию игрока. {@code kickSeconds <= 0} — AFK-кик выключен (всегда NONE). Порог движения —
     * 0.1 блока или 1° поворота.
     */
    public Action update(
            String id,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            long nowMs,
            int warnSeconds,
            int kickSeconds) {
        State s = states.get(id);
        if (s == null) {
            s = new State();
            states.put(id, s);
            set(s, x, y, z, yaw, pitch, nowMs);
            return Action.NONE;
        }
        boolean moved = (s.x - x) * (s.x - x) + (s.y - y) * (s.y - y) + (s.z - z) * (s.z - z) > 0.01
                || Math.abs(wrap(s.yaw - yaw)) > 1F
                || Math.abs(s.pitch - pitch) > 1F;
        if (moved) {
            set(s, x, y, z, yaw, pitch, nowMs);
            return Action.NONE;
        }
        if (kickSeconds <= 0) return Action.NONE;
        long idle = nowMs - s.lastActive;
        if (idle >= kickSeconds * 1000L) {
            states.remove(id);
            return Action.KICK;
        }
        if (!s.warned && warnSeconds > 0 && idle >= (long) Math.max(0, kickSeconds - warnSeconds) * 1000L) {
            s.warned = true;
            return Action.WARN;
        }
        return Action.NONE;
    }

    /** Секунд до кика (для текста предупреждения); 0 — игрока нет. */
    public int secondsLeft(String id, long nowMs, int kickSeconds) {
        State s = states.get(id);
        if (s == null) return 0;
        return (int) Math.max(0, (kickSeconds * 1000L - (nowMs - s.lastActive) + 999) / 1000);
    }

    /** Активность не по движению (чат, команда, взаимодействие) или выход игрока. */
    public void touch(String id, long nowMs) {
        State s = states.get(id);
        if (s != null) {
            s.lastActive = nowMs;
            s.warned = false;
        }
    }

    public void forget(String id) {
        states.remove(id);
    }

    private static void set(State s, double x, double y, double z, float yaw, float pitch, long now) {
        s.x = x;
        s.y = y;
        s.z = z;
        s.yaw = yaw;
        s.pitch = pitch;
        s.lastActive = now;
        s.warned = false;
    }

    private static float wrap(float d) {
        d %= 360F;
        if (d > 180F) d -= 360F;
        if (d < -180F) d += 360F;
        return d;
    }
}
