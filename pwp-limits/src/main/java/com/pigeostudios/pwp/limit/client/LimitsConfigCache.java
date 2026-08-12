package com.pigeostudios.pwp.limit.client;

// Клиентский кэш значений лимитов, приходящих от сервера (LimitsConfigPacket).
// Все клиентские механики (кд прыжка, HUD) читают только этот кэш, а не конфиг напрямую:
// SERVER-конфиг Forge не передаётся клиенту по сети, поэтому в мультиплеере источник истины — сервер.
public final class LimitsConfigCache {
    // Дефолты совпадают с дефолтами ModConfig (актуальны, пока не пришёл пакет от сервера)
    private static boolean jumpCooldownEnabled = true;
    private static double jumpCooldownSeconds = 1.0;

    private LimitsConfigCache() {
    }

    public static boolean isJumpCooldownEnabled() {
        return jumpCooldownEnabled;
    }

    public static double getJumpCooldownSeconds() {
        return jumpCooldownSeconds;
    }

    public static void apply(boolean enabled, double seconds) {
        jumpCooldownEnabled = enabled;
        jumpCooldownSeconds = Math.max(0.1, seconds);
    }
}
