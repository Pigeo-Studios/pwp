package com.pwp.lobby.donate.particles;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Конфиг частиц ролей: <config>/pwp_lobby/role_particles.toml
 * Эффекты MODERATOR/ADMIN + общий бюджет частиц в тик и LOD.
 */
public class RoleParticleConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue MODERATOR_ENABLED;
    public static final ForgeConfigSpec.IntValue MODERATOR_RATE;    // частиц в секунду (3-6)
    public static final ForgeConfigSpec.BooleanValue ADMIN_ENABLED;
    public static final ForgeConfigSpec.IntValue ADMIN_RATE;        // частиц в секунду (10-15)
    public static final ForgeConfigSpec.IntValue GLOBAL_BUDGET;     // лимит частиц в тик на всех
    public static final ForgeConfigSpec.IntValue LOD_FAR;           // дистанция полного отсутствия

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.comment("Частицы ролей (MODERATOR/ADMIN) — RED SCANNER для админов.").push("moderator");
        MODERATOR_ENABLED = BUILDER.define("enabled", true);
        MODERATOR_RATE = BUILDER.comment("Частиц в секунду").defineInRange("rate", 4, 1, 20);
        BUILDER.pop();

        BUILDER.push("admin");
        ADMIN_ENABLED = BUILDER.define("enabled", true);
        ADMIN_RATE = BUILDER.comment("Частиц в секунду").defineInRange("rate", 12, 1, 20);
        BUILDER.pop();

        BUILDER.comment("Общие лимиты.").push("limits");
        GLOBAL_BUDGET = BUILDER.comment("Максимум частиц, отправляемых за тик всем зрителям").defineInRange("globalBudget", 120, 10, 500);
        LOD_FAR = BUILDER.comment("Дистанция (блоки), дальше которой частицы не видны. 0-16 полная, 16-32 снижение, 32+ ничего").defineInRange("lodFar", 32, 8, 96);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
