package com.pwp.api.match;

import java.util.ArrayList;
import java.util.List;

/**
 * Сидинг (этап 10 ROADMAP): при малом онлайне сервер не должен запускать большой бой на пол-команды. Онлайн
 * лобби меньше {@code seedingBelow} — в голосовании остаются только малые режимы (без техники, как «Стычка»),
 * если карта их объявила; иначе список не меняется (лучше обычный бой, чем никакого).
 */
public final class Seeding {
    private Seeding() {}

    public static boolean active(int online, int seedingBelow) {
        return seedingBelow > 0 && online < seedingBelow;
    }

    public static List<GameModes.Mode> modes(List<GameModes.Mode> votable, int online, int seedingBelow) {
        if (!active(online, seedingBelow)) return votable;
        List<GameModes.Mode> small = new ArrayList<>();
        for (GameModes.Mode m : votable) if (!m.vehicles()) small.add(m);
        return small.isEmpty() ? votable : List.copyOf(small);
    }
}
