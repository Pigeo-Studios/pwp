package com.pwp.coreclient.controls;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Мод-владелец клавиши по её имени и категории ({@code KeyMapping.getName()/getCategory()}): у
 * Forge нет поля «чей это KeyMapping», поэтому ищем загруженный мод среди сегментов имени
 * ({@code key.tacz.aim.desc} → tacz, {@code iris.keybind.x} → iris), затем категории
 * ({@code key.categories.voicechat} → voicechat). Ничего не нашлось и категория ванильная —
 * Minecraft; иначе {@code null} (экран показывает переведённую категорию).
 */
public final class KeyOwner {
    /** Служебные сегменты имён, которые не бывают id мода. */
    private static final Set<String> SKIP =
            Set.of("key", "keys", "keybind", "keybinds", "keybinding", "keyinfo", "categories", "category", "desc");

    /** Категории ванильных клавиш ({@code KeyMapping.CATEGORY_*}). */
    public static final Set<String> VANILLA_CATEGORIES = Set.of(
            "key.categories.movement",
            "key.categories.misc",
            "key.categories.multiplayer",
            "key.categories.gameplay",
            "key.categories.inventory",
            "key.categories.ui",
            "key.categories.creative");

    public static final String MINECRAFT = "minecraft";

    private KeyOwner() {}

    /**
     * id мода-владельца или {@code null}.
     *
     * @param isMod загружен ли мод с таким id
     */
    public static String modId(String name, String category, Predicate<String> isMod) {
        String fromName = firstMod(name, isMod);
        if (fromName != null) return fromName;
        String fromCat = firstMod(category, isMod);
        if (fromCat != null) return fromCat;
        if (category != null && VANILLA_CATEGORIES.contains(category)) return MINECRAFT;
        return null;
    }

    /** Отображаемое имя мода; мод не найден — {@code fallback}. */
    public static String display(
            String name,
            String category,
            Predicate<String> isMod,
            Function<String, String> displayName,
            String fallback) {
        String id = modId(name, category, isMod);
        if (id == null) return fallback;
        String d = displayName == null ? null : displayName.apply(id);
        return d == null || d.isBlank() ? id : d;
    }

    private static String firstMod(String s, Predicate<String> isMod) {
        if (s == null || s.isEmpty()) return null;
        for (String part : s.split("\\.")) {
            if (part.isEmpty() || SKIP.contains(part)) continue;
            if (isMod.test(part)) return part;
        }
        return null;
    }
}
