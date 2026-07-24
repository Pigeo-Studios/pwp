package com.pwp.coreclient.gui.animations;

public class PWPAnim {

    private final long startTime;
    private final long durationMs;
    private final EasingFunction easing;

    @FunctionalInterface
    public interface EasingFunction {
        float apply(float t);
    }

    public PWPAnim(long startTime, long durationMs, EasingFunction easing) {
        this.startTime = startTime;
        this.durationMs = Math.max(1, durationMs);
        this.easing = easing;
    }

    public PWPAnim(long durationMs, EasingFunction easing) {
        this(System.currentTimeMillis(), durationMs, easing);
    }

    public float getProgress(long now) {
        float t = (float) (now - startTime) / durationMs;
        return Math.min(Math.max(t, 0), 1);
    }

    public float getEased(long now) {
        return easing.apply(getProgress(now));
    }

    public boolean isFinished(long now) {
        return now - startTime >= durationMs;
    }

    public void reset(long now) {
        // extension point
    }

    public static float deltaLerp(float current, float target, float dt, float speed) {
        if (Math.abs(current - target) < 0.001F) return target;
        return current + (target - current) * (1 - (float) Math.exp(-dt * speed));
    }
}
