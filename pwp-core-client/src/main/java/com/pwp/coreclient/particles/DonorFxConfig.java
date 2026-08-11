package com.pwp.coreclient.particles;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Клиентский конфиг донат-FX (аддитивные свечения в мире).
 * Раньше настройки жили на сервере (pwp-lobby serverconfig donator/role_particles.toml),
 * рендер был серверными ванильными dust-пикселями. Теперь эффекты рисуются на клиенте
 * (DonorFxRenderer), поэтому и настройки переехали к игроку.
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

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("donor_fx");
        ENABLED = b.comment("Включить аддитивные эффекты донатеров/ролей в мире").define("enabled", true);
        BUDGET = b.comment("Максимум квадов свечений на кадр (защита FPS)").defineInRange("budget", 220, 16, 2048);
        LOD_FAR = b.comment("Дальность показа эффектов (блоки)").defineInRange("lod_far", 48, 8, 128);
        b.pop();
        b.push("levels");
        ADMIN_ENABLED = b.comment("ADMIN — огненный шоу-эффект (кольца, искры, пульсы)").define("admin", true);
        MODERATOR_ENABLED = b.comment("MODERATOR — холодное сине-голубое свечение").define("moderator", true);
        PLATINUM_ENABLED = b.comment("PLATINUM — гало над головой и орбиты").define("platinum", true);
        GOLD_ENABLED = b.comment("GOLD — золотая спираль свечений").define("gold", true);
        SILVER_ENABLED = b.comment("SILVER — мягкий световой шорох").define("silver", true);
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
}
