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

    public static final BooleanValue FALLBACK_FILTER_ENABLED = BUILDER
        .comment(
            "Fallback protection for explosions that bypass vanilla resistance checks entirely",
            "(fixed-radius block wipes used by some minigame/overhaul mods, e.g. TACZ/SBW).",
            "Main protection now happens via increased explosionResistance (mixin), which already",
            "shrinks craters at the source for normal explosions. This fallback only removes blocks",
            "that still made it into the affected list, so keep its multipliers modest to avoid",
            "double-stacking protection on the same explosion."
        )
        .define("fallbackFilterEnabled", true);

    public static final DoubleValue FALLBACK_SOFT_MULTIPLIER = BUILDER
        .comment("Fallback survival chance for soft terrain, used only by explosions the resistance mixin can't see.")
        .defineInRange("fallbackSoftMultiplier", 1.5, 1.0, 100.0);

    public static final DoubleValue FALLBACK_HARD_MULTIPLIER = BUILDER
        .comment("Fallback survival chance for hard terrain, used only by explosions the resistance mixin can't see.")
        .defineInRange("fallbackHardMultiplier", 1.3, 1.0, 100.0);

    static {
        SPEC = BUILDER.build();
    }
}
