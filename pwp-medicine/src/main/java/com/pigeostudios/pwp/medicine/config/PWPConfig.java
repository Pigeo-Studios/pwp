package com.pigeostudios.pwp.medicine.config;

import net.minecraftforge.common.ForgeConfigSpec;

// Конфигурация мода PWP Medicine.
// Хранит настройки: время применения аптечки, порог и шанс кровотечения.
public class PWPConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    // Время (в тиках) между применениями аптечки (20 тиков = 1 секунда). По умолчанию: 40 (2 сек)
    public static final ForgeConfigSpec.IntValue MEDKIT_APPLY_TIME;
    // Минимальный урон, при котором может начаться кровотечение. По умолчанию: 3.0 ХП
    public static final ForgeConfigSpec.DoubleValue BLEEDING_DAMAGE_THRESHOLD;
    // Шанс получения кровотечения (от 0.0 до 1.0, где 0.2 = 20%). По умолчанию: 0.2
    public static final ForgeConfigSpec.DoubleValue BLEEDING_CHANCE;

    static {
        BUILDER.push("PWP Medicine Settings");
        MEDKIT_APPLY_TIME = BUILDER.comment("Время (в тиках) между применениями аптечки (20 тиков = 1 секунда). По умолчанию: 40 (2 сек)").defineInRange("medkitApplyTime", 40, 1, 72000);
        BLEEDING_DAMAGE_THRESHOLD = BUILDER.comment("Минимальный урон, при котором может начаться кровотечение. По умолчанию: 3.0 ХП").defineInRange("bleedingDamageThreshold", 3.0, 0.0, 1000.0);
        BLEEDING_CHANCE = BUILDER.comment("Шанс получения кровотечения (от 0.0 до 1.0, где 0.2 - это 20%). По умолчанию: 0.2").defineInRange("bleedingChance", 0.2, 0.0, 1.0);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
