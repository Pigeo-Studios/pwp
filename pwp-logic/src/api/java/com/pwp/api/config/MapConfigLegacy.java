package com.pwp.api.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Устаревшие ключи {@code map_config.json}, которые больше ничего не делают.
 *
 * <p>Правила матча (ники, голод, реген, сообщения о смерти) задаёт только {@code match_config.json};
 * {@code durationMinutes} и {@code settings.lockDurationMinutes} никогда не применялись (блокировка точки
 * задаётся у каждой точки — {@code capturePoints[].lockDurationMinutes}, это поле живое).
 */
public final class MapConfigLegacy {
    /** Мёртвые ключи верхнего уровня. */
    public static final Set<String> TOP_LEVEL = Set.of("durationMinutes", "vehicles");

    /** Мёртвые ключи секции {@code settings}. */
    public static final Set<String> SETTINGS =
            Set.of("lockDurationMinutes", "hideDeathMessages", "hideNametags", "disableHunger", "disableNaturalRegen");

    private MapConfigLegacy() {}

    /**
     * Пути найденных устаревших ключей ({@code durationMinutes}, {@code settings.hideNametags}…) в порядке
     * входных коллекций.
     *
     * @param topKeys ключи корня JSON
     * @param settingsKeys ключи секции {@code settings} (пусто/null, если секции нет)
     */
    public static List<String> ignoredKeys(Collection<String> topKeys, Collection<String> settingsKeys) {
        List<String> out = new ArrayList<>();
        if (topKeys != null) {
            for (String k : topKeys) if (TOP_LEVEL.contains(k)) out.add(k);
        }
        if (settingsKeys != null) {
            for (String k : settingsKeys) if (SETTINGS.contains(k)) out.add("settings." + k);
        }
        return out;
    }

    /** Текст предупреждения для лога (одна строка на файл). */
    public static String warning(String source, List<String> keys) {
        return "map_config " + source + ": ключи " + String.join(", ", keys)
                + " игнорируются, используйте match_config.json (правила матча) — удалите их из map_config";
    }
}
