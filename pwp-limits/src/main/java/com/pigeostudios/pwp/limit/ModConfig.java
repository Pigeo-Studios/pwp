package com.pigeostudios.pwp.limit;

import net.minecraftforge.common.ForgeConfigSpec;

// Конфигурация мода: включение/отключение кулдауна прыжка и его длительность
public class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    // Включить кулдаун на прыжок во время спринта
    public static final ForgeConfigSpec.BooleanValue ENABLE_JUMP_COOLDOWN;
    // Длительность кулдауна в секундах (0.1 - 60.0, по умолчанию 3.0)
    public static final ForgeConfigSpec.DoubleValue JUMP_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("General Settings");
        ENABLE_JUMP_COOLDOWN = BUILDER.define("enableJumpCooldown", true);
        JUMP_COOLDOWN_SECONDS = BUILDER.defineInRange("jumpCooldownSeconds", 3.0, 0.1, 60.0);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
