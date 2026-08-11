package com.pwp.lobby.donate.particles;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Конфиг частиц доната: <config>/pwp_lobby/donator_particles.toml
 * Включение/выключение и плотность эффектов по тирам SILVER/GOLD/PLATINUM.
 */
public class DonatorParticleConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SILVER_ENABLED;
    public static final ForgeConfigSpec.IntValue SILVER_RATE;       // частиц в секунду (2-4)
    public static final ForgeConfigSpec.BooleanValue GOLD_ENABLED;
    public static final ForgeConfigSpec.IntValue GOLD_RATE;         // частиц в секунду (5-8)
    public static final ForgeConfigSpec.BooleanValue PLATINUM_ENABLED;
    public static final ForgeConfigSpec.IntValue PLATINUM_RATE;     // частиц в секунду (8-12)

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.comment("Частицы донат-тиров в лобби (SILVER/GOLD/PLATINUM).").push("silver");
        SILVER_ENABLED = BUILDER.define("enabled", true);
        SILVER_RATE = BUILDER.comment("Частиц в секунду").defineInRange("rate", 3, 1, 20);
        BUILDER.pop();

        BUILDER.push("gold");
        GOLD_ENABLED = BUILDER.define("enabled", true);
        GOLD_RATE = BUILDER.comment("Частиц в секунду").defineInRange("rate", 6, 1, 20);
        BUILDER.pop();

        BUILDER.push("platinum");
        PLATINUM_ENABLED = BUILDER.define("enabled", true);
        PLATINUM_RATE = BUILDER.comment("Частиц в секунду").defineInRange("rate", 10, 1, 20);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
