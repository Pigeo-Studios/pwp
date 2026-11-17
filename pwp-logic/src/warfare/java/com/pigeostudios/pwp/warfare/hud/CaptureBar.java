package com.pigeostudios.pwp.warfare.hud;

/**
 * Полоса захвата точки на HUD (этап 3 редизайна HUD) — «перетягивание каната»: левая половина —
 * BLUE, правая — RED, центр — нейтраль. Чистая логика без Minecraft: положение маркера, направление
 * движения и состояние точки по данным {@code PacketSyncPoint} (owner, capturingTeam, progress,
 * captureRate, isContested, isLocked).
 *
 * <p>Сервер ({@code GameLogicEvents}) шлёт {@code capturingTeam} = команда, которая СЕЙЧАС влияет
 * на точку («NONE», если никто), а {@code captureRate} &gt; 0 — захват/укрепление, &lt; 0 —
 * нейтрализация чужого прогресса. Нейтральная точка с прогрессом и без влияния не знает, чей
 * прогресс, — тогда вызывающий передаёт запомненную команду ({@code lastTeam}).
 */
public final class CaptureBar {
    private CaptureBar() {}

    public static final String BLUE = "BLUE";
    public static final String RED = "RED";
    public static final String NEUTRAL = "NEUTRAL";
    public static final String NONE = "NONE";

    /** Состояние точки для подписи и иконки. */
    public enum State {
        /** Заблокирована (порядок AAS или таймер блокировки). */
        LOCKED,
        /** Обе команды на точке без перевеса 2:1. */
        CONTESTED,
        /** Нейтральную точку захватывают. */
        CAPTURING,
        /** Сбивают чужой прогресс / чужую точку. */
        NEUTRALIZING,
        /** Владелец добирает прогресс до 100%. */
        SECURING,
        /** Точка чья-то, никто не влияет. */
        HELD,
        /** Нейтральная, никто не влияет. */
        NEUTRAL
    }

    /**
     * Команда, на чьей стороне прогресс: владелец, иначе команда-захватчик, иначе {@code lastTeam};
     * {@link #NONE}, если не известно.
     */
    public static String side(String owner, String capturingTeam, String lastTeam) {
        if (BLUE.equals(owner) || RED.equals(owner)) return owner;
        if (BLUE.equals(capturingTeam) || RED.equals(capturingTeam)) return capturingTeam;
        if (BLUE.equals(lastTeam) || RED.equals(lastTeam)) return lastTeam;
        return NONE;
    }

    /**
     * Положение маркера в [−1, 1]: −1 — полностью BLUE, +1 — полностью RED, 0 — нейтраль.
     *
     * @param side результат {@link #side}
     */
    public static float signed(String side, float progress) {
        float p = Float.isNaN(progress) ? 0F : Math.max(0F, Math.min(1F, progress));
        if (BLUE.equals(side)) return -p;
        if (RED.equals(side)) return p;
        return 0F;
    }

    /**
     * Куда движется маркер: −1 — к BLUE, +1 — к RED, 0 — стоит. Влияющая команда тянет маркер к
     * своей стороне и при захвате, и при нейтрализации чужого прогресса.
     */
    public static int direction(String capturingTeam, int rate) {
        if (rate == 0) return 0;
        if (BLUE.equals(capturingTeam)) return -1;
        if (RED.equals(capturingTeam)) return 1;
        return 0;
    }

    /** Сила влияния для шевронов: |rate| в пределах 0..{@code max}. */
    public static int strength(int rate, int max) {
        return Math.min(Math.abs(rate), Math.max(0, max));
    }

    public static State state(String owner, int rate, boolean contested, boolean locked) {
        if (locked) return State.LOCKED;
        if (contested) return State.CONTESTED;
        boolean neutral = !BLUE.equals(owner) && !RED.equals(owner);
        if (rate < 0) return State.NEUTRALIZING;
        if (rate > 0) return neutral ? State.CAPTURING : State.SECURING;
        return neutral ? State.NEUTRAL : State.HELD;
    }
}
