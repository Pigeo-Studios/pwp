package com.pwp.coreclient.particles;

import com.pwp.coreclient.donor.DonorLevel;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Клиентский конфиг донат-FX (аддитивные частицы свечения, штатный партикл-движок).
 * Плотность задаётся в частицах в секунду на игрока уровня; бюджет — максимум
 * спавнов частиц за тик (защита FPS). Цвета (секция colors) — оверрайд палитры
 * DonorLevel: градиент ников (стопы RRGGBB,RRGGBB или 3 стопа) и цвет свечения.
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

    public static final ForgeConfigSpec.ConfigValue<String> ADMIN_STOPS;
    public static final ForgeConfigSpec.ConfigValue<String> MODERATOR_STOPS;
    public static final ForgeConfigSpec.ConfigValue<String> PLATINUM_STOPS;
    public static final ForgeConfigSpec.ConfigValue<String> GOLD_STOPS;
    public static final ForgeConfigSpec.ConfigValue<String> SILVER_STOPS;

    public static final ForgeConfigSpec.ConfigValue<String> ADMIN_GLOW;
    public static final ForgeConfigSpec.ConfigValue<String> MODERATOR_GLOW;
    public static final ForgeConfigSpec.ConfigValue<String> PLATINUM_GLOW;
    public static final ForgeConfigSpec.ConfigValue<String> GOLD_GLOW;
    public static final ForgeConfigSpec.ConfigValue<String> SILVER_GLOW;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("donor_fx");
        ENABLED = b.comment("Включить аддитивные эффекты донатеров/ролей в мире").define("enabled", true);
        BUDGET = b.comment("Максимум спавнов частиц за тик (20 тиков/сек, защита FPS)").defineInRange("budget", 24, 1, 200);
        LOD_FAR = b.comment("Дальность показа эффектов (блоки)").defineInRange("lod_far", 32, 8, 128);
        b.pop();
        b.push("levels");
        ADMIN_ENABLED = b.comment("ADMIN — красный Scanner: сегментированное кольцо, сканирующие дуги, полоса, импульсы, искры при беге").define("admin_enabled", true);
        MODERATOR_ENABLED = b.comment("MODERATOR — спокойная сине-голубая служебная аура (пыль + небольшая спираль)").define("moderator_enabled", true);
        PLATINUM_ENABLED = b.comment("PLATINUM — аура: кольцо у ног, две встречные спирали, яркие всплески").define("platinum_enabled", true);
        GOLD_ENABLED = b.comment("GOLD — золотая вращающаяся спираль + периодические вспышки").define("gold_enabled", true);
        SILVER_ENABLED = b.comment("SILVER — серебряная пыль вокруг тела, редкие искры").define("silver_enabled", true);
        ADMIN_RATE = b.comment("ADMIN: частиц в секунду").defineInRange("admin_rate", 14.0, 0.0, 100.0);
        MODERATOR_RATE = b.comment("MODERATOR: частиц в секунду").defineInRange("moderator_rate", 5.0, 0.0, 100.0);
        PLATINUM_RATE = b.comment("PLATINUM: частиц в секунду").defineInRange("platinum_rate", 9.0, 0.0, 100.0);
        GOLD_RATE = b.comment("GOLD: частиц в секунду").defineInRange("gold_rate", 7.0, 0.0, 100.0);
        SILVER_RATE = b.comment("SILVER: частиц в секунду").defineInRange("silver_rate", 4.0, 0.0, 100.0);
        b.pop();
        b.push("colors");
        ADMIN_STOPS = b.comment("ADMIN: стопы градиента ника, RRGGBB через запятую (2-3)").define("admin_stops", "D50000,FF4D4D,FFD980");
        MODERATOR_STOPS = b.comment("MODERATOR: стопы градиента ника, RRGGBB через запятую (2-3)").define("moderator_stops", "2A5B8F,9FD0FF");
        PLATINUM_STOPS = b.comment("PLATINUM: стопы градиента ника, RRGGBB через запятую (2-3)").define("platinum_stops", "6FB6D6,F6FBFF");
        GOLD_STOPS = b.comment("GOLD: стопы градиента ника, RRGGBB через запятую (2-3)").define("gold_stops", "B8860B,FFF3B0");
        SILVER_STOPS = b.comment("SILVER: стопы градиента ника, RRGGBB через запятую (2-3)").define("silver_stops", "8E9AA6,F3F6F9");
        ADMIN_GLOW = b.comment("ADMIN: цвет свечения частиц и ореола ника").define("admin_glow", "FF3030");
        MODERATOR_GLOW = b.comment("MODERATOR: цвет свечения частиц и ореола ника").define("moderator_glow", "4A9BFF");
        PLATINUM_GLOW = b.comment("PLATINUM: цвет свечения частиц и ореола ника").define("platinum_glow", "A8E4FF");
        GOLD_GLOW = b.comment("GOLD: цвет свечения частиц и ореола ника").define("gold_glow", "FFC94D");
        SILVER_GLOW = b.comment("SILVER: цвет свечения частиц и ореола ника").define("silver_glow", "9FB4C8");
        b.pop();
        SPEC = b.build();
    }

    private DonorFxConfig() {}

    static boolean enabled(DonorLevel level) {
        return switch (level) {
            case ADMIN -> ADMIN_ENABLED.get();
            case MODERATOR -> MODERATOR_ENABLED.get();
            case PLATINUM -> PLATINUM_ENABLED.get();
            case GOLD -> GOLD_ENABLED.get();
            case SILVER -> SILVER_ENABLED.get();
        };
    }

    static double rate(DonorLevel level) {
        return switch (level) {
            case ADMIN -> ADMIN_RATE.get();
            case MODERATOR -> MODERATOR_RATE.get();
            case PLATINUM -> PLATINUM_RATE.get();
            case GOLD -> GOLD_RATE.get();
            case SILVER -> SILVER_RATE.get();
        };
    }

    public static String stopsCfg(DonorLevel level) {
        return switch (level) {
            case ADMIN -> ADMIN_STOPS.get();
            case MODERATOR -> MODERATOR_STOPS.get();
            case PLATINUM -> PLATINUM_STOPS.get();
            case GOLD -> GOLD_STOPS.get();
            case SILVER -> SILVER_STOPS.get();
        };
    }

    public static String glowCfg(DonorLevel level) {
        return switch (level) {
            case ADMIN -> ADMIN_GLOW.get();
            case MODERATOR -> MODERATOR_GLOW.get();
            case PLATINUM -> PLATINUM_GLOW.get();
            case GOLD -> GOLD_GLOW.get();
            case SILVER -> SILVER_GLOW.get();
        };
    }
}
