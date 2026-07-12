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
        .comment("Blast resistance multiplier for soft terrain (dirt, sand, gravel, snow)")
        .defineInRange("softMultiplier", 8.0, 1.0, 100.0);

    public static final DoubleValue HARD_MULTIPLIER = BUILDER
        .comment("Blast resistance multiplier for stone, wood, and building materials")
        .defineInRange("hardMultiplier", 4.0, 1.0, 100.0);

    static {
        SPEC = BUILDER.build();
    }
}
