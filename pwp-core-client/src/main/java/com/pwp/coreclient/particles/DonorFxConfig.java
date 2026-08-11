package com.pwp.coreclient.particles;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Клиентский конфиг донат-FX (аддитивные частицы свечения, штатный партикл-движок).
 * Плотность задаётся в частицах в секунду на игрока уровня; бюджет — максимум
 * спавнов частиц за тик (защита FPS).
 */
public final class DonorFxConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue BUDGET;
    public static final ForgeConfigSpec.IntValue LOD_FAR;

    public static final ForgeConfigSpec.BooleanValue ADMIN_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MODERATOR_ENABLED;
    public static final ForgeConfigSpec.BooleanValue PLATINUM_ENABLED;
    public static final ForgeConfigSpec.BooleanValue GOLD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SILVER_ENABLED;

    public static final ForgeConfigSpec.DoubleValue ADMIN_RATE;
    public static final ForgeConfigSpec.DoubleValue MODERATOR_RATE;
    public static final ForgeConfigSpec.DoubleValue PLATINUM_RATE;
    public static final ForgeConfigSpec.DoubleValue GOLD_RATE;
    public static final ForgeConfigSpec.DoubleValue SILVER_RATE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("donor_fx");
        ENABLED = b.comment("Включить аддитивные эффекты донатеров/ролей в мире").define("enabled", true);
        BUDGET = b.comment("Максимум спавнов частиц за тик (20 тиков/сек, защита FPS)").defineInRange("budget", 24, 1, 200);
        LOD_FAR = b.comment("Дальность показа эффектов (блоки)").defineInRange("lod_far", 32, 8, 128);
        b.pop();
        b.push("levels");
        ADMIN_ENABLED = b.comment("ADMIN — огненный шоу-эффект (искры, кольцо, пульс)").define("admin_enabled", true);
        MODERATOR_ENABLED = b.comment("MODERATOR — холодное сине-голубое свечение").define("moderator_enabled", true);
        PLATINUM_ENABLED = b.comment("PLATINUM — гало над головой и орбиты").define("platinum_enabled", true);
        GOLD_ENABLED = b.comment("GOLD — золотое кольцо-спираль").define("gold_enabled", true);
        SILVER_ENABLED = b.comment("SILVER — мягкий световой шорох").define("silver_enabled", true);
        ADMIN_RATE = b.comment("ADMIN: частиц в секунду").defineInRange("admin_rate", 14.0, 0.0, 100.0);
        MODERATOR_RATE = b.comment("MODERATOR: частиц в секунду").defineInRange("moderator_rate", 5.0, 0.0, 100.0);
        PLATINUM_RATE = b.comment("PLATINUM: частиц в секунду").defineInRange("platinum_rate", 9.0, 0.0, 100.0);
        GOLD_RATE = b.comment("GOLD: частиц в секунду").defineInRange("gold_rate", 7.0, 0.0, 100.0);
        SILVER_RATE = b.comment("SILVER: частиц в секунду").defineInRange("silver_rate", 3.0, 0.0, 100.0);
        b.pop();
        SPEC = b.build();
    }

    private DonorFxConfig() {}

    static boolean enabled(String level) {
        return switch (level) {
            case "ADMIN" -> ADMIN_ENABLED.get();
            case "MODERATOR" -> MODERATOR_ENABLED.get();
            case "PLATINUM" -> PLATINUM_ENABLED.get();
            case "GOLD" -> GOLD_ENABLED.get();
            case "SILVER" -> SILVER_ENABLED.get();
            default -> false;
        };
    }

    static double rate(String level) {
        return switch (level) {
            case "ADMIN" -> ADMIN_RATE.get();
            case "MODERATOR" -> MODERATOR_RATE.get();
            case "PLATINUM" -> PLATINUM_RATE.get();
            case "GOLD" -> GOLD_RATE.get();
            case "SILVER" -> SILVER_RATE.get();
            default -> 0.0;
        };
    }
}
