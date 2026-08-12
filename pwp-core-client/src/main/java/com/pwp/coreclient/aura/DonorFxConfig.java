package com.pwp.coreclient.aura;

import com.pwp.coreclient.donor.DonorLevel;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Клиентский конфиг донат-FX (аддитивный рендер аур в мире, без частиц).
 * Яркость задаётся множителем intensity на уровень (0 — скрыть эффект уровня),
 * дальность — lod_far. Цвета (секция colors) — оверрайд палитры DonorLevel:
 * градиент ников (стопы RRGGBB,RRGGBB или 3 стопа) и цвет свечения.
 */
public final class DonorFxConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue LOD_FAR;

    public static final ForgeConfigSpec.BooleanValue ADMIN_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MODERATOR_ENABLED;
    public static final ForgeConfigSpec.BooleanValue PLATINUM_ENABLED;
    public static final ForgeConfigSpec.BooleanValue GOLD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SILVER_ENABLED;

    public static final ForgeConfigSpec.DoubleValue ADMIN_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue MODERATOR_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue PLATINUM_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue GOLD_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue SILVER_INTENSITY;

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

    /** Тайминги переходов состояний IDLE/MOVING/FLYING/LANDING (секунды) — см. дизайн аур. */
    public static final ForgeConfigSpec.DoubleValue T_IDLE_TO_MOVING;
    public static final ForgeConfigSpec.DoubleValue T_MOVING_TO_IDLE;
    public static final ForgeConfigSpec.DoubleValue T_MOVING_TO_FLYING;
    public static final ForgeConfigSpec.DoubleValue T_FLYING_TO_MOVING;
    public static final ForgeConfigSpec.DoubleValue T_FLYING_TO_LANDING;
    public static final ForgeConfigSpec.DoubleValue T_LANDING_TO_IDLE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("donor_fx");
        ENABLED = b.comment("Включить аддитивные ауры донатеров/ролей в мире").define("enabled", true);
        LOD_FAR = b.comment("Дальность показа аур (блоки)").defineInRange("lod_far", 32, 8, 128);
        b.pop();
        b.push("levels");
        ADMIN_ENABLED = b.comment("ADMIN — красный Scanner: сегментированное кольцо, сканирующие дуги, полоса, пульсы, искры при беге").define("admin_enabled", true);
        MODERATOR_ENABLED = b.comment("MODERATOR — спокойная сине-голубая служебная аура (свечение + медленные глиты)").define("moderator_enabled", true);
        PLATINUM_ENABLED = b.comment("PLATINUM — аура: кольцо-плитка у ног, две встречные спирали, свечение тела").define("platinum_enabled", true);
        GOLD_ENABLED = b.comment("GOLD — золотая вращающаяся спираль-лента вокруг тела").define("gold_enabled", true);
        SILVER_ENABLED = b.comment("SILVER — серебряное свечение тела + всплывающие глиты").define("silver_enabled", true);
        ADMIN_INTENSITY = b.comment("ADMIN: множитель яркости (0 — скрыть эффект)").defineInRange("admin_intensity", 1.0, 0.0, 3.0);
        MODERATOR_INTENSITY = b.comment("MODERATOR: множитель яркости (0 — скрыть эффект)").defineInRange("moderator_intensity", 1.0, 0.0, 3.0);
        PLATINUM_INTENSITY = b.comment("PLATINUM: множитель яркости (0 — скрыть эффект)").defineInRange("platinum_intensity", 1.0, 0.0, 3.0);
        GOLD_INTENSITY = b.comment("GOLD: множитель яркости (0 — скрыть эффект)").defineInRange("gold_intensity", 1.0, 0.0, 3.0);
        SILVER_INTENSITY = b.comment("SILVER: множитель яркости (0 — скрыть эффект)").defineInRange("silver_intensity", 1.0, 0.0, 3.0);
        b.pop();
        b.push("colors");
        ADMIN_STOPS = b.comment("ADMIN: стопы градиента ника, RRGGBB через запятую (2-3)").define("admin_stops", "D50000,FF4D4D,FFD980");
        MODERATOR_STOPS = b.comment("MODERATOR: стопы градиента ника, RRGGBB через запятую (2-3)").define("moderator_stops", "2A5B8F,9FD0FF");
        PLATINUM_STOPS = b.comment("PLATINUM: стопы градиента ника, RRGGBB через запятую (2-3)").define("platinum_stops", "6FB6D6,F6FBFF");
        GOLD_STOPS = b.comment("GOLD: стопы градиента ника, RRGGBB через запятую (2-3)").define("gold_stops", "B8860B,FFF3B0");
        SILVER_STOPS = b.comment("SILVER: стопы градиента ника, RRGGBB через запятую (2-3)").define("silver_stops", "8E9AA6,F3F6F9");
        ADMIN_GLOW = b.comment("ADMIN: цвет свечения аур и ореола ника").define("admin_glow", "FF3030");
        MODERATOR_GLOW = b.comment("MODERATOR: цвет свечения аур и ореола ника").define("moderator_glow", "4A9BFF");
        PLATINUM_GLOW = b.comment("PLATINUM: цвет свечения аур и ореола ника").define("platinum_glow", "A8E4FF");
        GOLD_GLOW = b.comment("GOLD: цвет свечения аур и ореола ника").define("gold_glow", "FFC94D");
        SILVER_GLOW = b.comment("SILVER: цвет свечения аур и ореола ника").define("silver_glow", "9FB4C8");
        b.pop();
        b.push("transitions");
        T_IDLE_TO_MOVING = b.comment("Переход IDLE -> MOVING (секунды)").defineInRange("idle_to_moving", 0.4, 0.05, 3.0);
        T_MOVING_TO_IDLE = b.comment("Переход MOVING -> IDLE (секунды)").defineInRange("moving_to_idle", 0.5, 0.05, 3.0);
        T_MOVING_TO_FLYING = b.comment("Переход MOVING -> FLYING (секунды)").defineInRange("moving_to_flying", 0.9, 0.05, 3.0);
        T_FLYING_TO_MOVING = b.comment("Переход FLYING -> MOVING (секунды)").defineInRange("flying_to_moving", 1.0, 0.05, 3.0);
        T_FLYING_TO_LANDING = b.comment("Переход FLYING -> LANDING (секунды): скорость возврата эффекта на землю").defineInRange("flying_to_landing", 0.65, 0.05, 3.0);
        T_LANDING_TO_IDLE = b.comment("Переход LANDING -> IDLE (секунды): длительность импакта посадки").defineInRange("landing_to_idle", 0.85, 0.05, 3.0);
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

    /** Множитель яркости уровня (0 — эффект скрыт). */
    static double intensity(DonorLevel level) {
        return switch (level) {
            case ADMIN -> ADMIN_INTENSITY.get();
            case MODERATOR -> MODERATOR_INTENSITY.get();
            case PLATINUM -> PLATINUM_INTENSITY.get();
            case GOLD -> GOLD_INTENSITY.get();
            case SILVER -> SILVER_INTENSITY.get();
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

    // ====== Тайминги переходов состояний (секунды) ======

    static double tIdleToMoving() {
        return T_IDLE_TO_MOVING.get();
    }

    static double tMovingToIdle() {
        return T_MOVING_TO_IDLE.get();
    }

    static double tMovingToFlying() {
        return T_MOVING_TO_FLYING.get();
    }

    static double tFlyingToMoving() {
        return T_FLYING_TO_MOVING.get();
    }

    static double tFlyingToLanding() {
        return T_FLYING_TO_LANDING.get();
    }

    static double tLandingToIdle() {
        return T_LANDING_TO_IDLE.get();
    }
}
