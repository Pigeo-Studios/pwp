package com.pwp.blastprotection.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

public class PWPBlastConfig {
    public static final Builder BUILDER = new Builder();
    public static ForgeConfigSpec SPEC;

    public static final BooleanValue ENABLED = BUILDER
        .comment("Master toggle for enhanced blast resistance")
        .define("enabled", true);

    public static final DoubleValue SOFT_MULTIPLIER = BUILDER
        .comment("Block survival chance for soft terrain (dirt, sand, gravel, snow). Higher = more blocks survive.")
        .defineInRange("softMultiplier", 2.67, 1.0, 100.0);

    public static final DoubleValue HARD_MULTIPLIER = BUILDER
        .comment("Block survival chance for stone, wood, and building materials. Higher = more blocks survive.")
        .defineInRange("hardMultiplier", 2.0, 1.0, 100.0);

    static {
        SPEC = BUILDER.build();
    }
}
