package com.pigeostudios.pwp.warfare.util;

import com.pigeostudios.pwp.warfare.match.TeamVisibility;
import net.minecraft.world.entity.Entity;

/**
 * Команда сущности в одном формате: {@code "BLUE"}/{@code "RED"} или пусто (по scoreboard-команде, регистр
 * любой; правило — {@link TeamVisibility#normalize}). Одна точка вместо прежних копий в StagingService,
 * FactionDataLoader, VehicleActionService, TacticalActions и HUD. Работает на сервере и на клиенте.
 */
public final class Teams {
    public static final String NEUTRAL = "NEUTRAL";

    private Teams() {}

    /** "BLUE", "RED" или "" (без команды / прочая команда). */
    public static String of(Entity e) {
        if (e == null || e.getTeam() == null) return "";
        String t = TeamVisibility.normalize(e.getTeam().getName());
        return t == null ? "" : t;
    }

    /** То же, но вместо пустого — {@link #NEUTRAL}. */
    public static String orNeutral(Entity e) {
        String t = of(e);
        return t.isEmpty() ? NEUTRAL : t;
    }
}
