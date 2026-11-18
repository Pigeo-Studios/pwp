package com.pigeostudios.pwp.limit.client;

/** Значения лимитов, присланные сервером ({@code LimitsConfigPacket}); до прихода — дефолты конфига. */
public final class LimitsConfigCache {
    private static volatile boolean jumpCooldownEnabled = true;
    private static volatile double jumpCooldownSeconds = 1.0;

    private LimitsConfigCache() {}

    public static boolean isJumpCooldownEnabled() {
        return jumpCooldownEnabled;
    }

    public static double getJumpCooldownSeconds() {
        return jumpCooldownSeconds;
    }

    public static void apply(boolean enabled, double seconds) {
        jumpCooldownEnabled = enabled;
        jumpCooldownSeconds = seconds;
    }
}
