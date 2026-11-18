package com.pwp.coreclient.controls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфликты назначений. На вход — ВСЕ клавиши сборки (в т. ч. вне белого списка), поэтому видимая
 * строка подсвечивается и при столкновении с невидимой клавишей.
 *
 * <p>Правило повторяет Forge 1.20.1 {@code KeyMapping.same} (по памяти, сверить при экране): если
 * контексты пересекаются, то конфликт, когда (1) модификатор одной клавиши — это сама другая
 * клавиша (Ctrl+R против спринта на ЛCtrl), или (2) физическая клавиша та же и модификаторы равны,
 * либо — в игровом контексте — у одной из них модификатора нет. Не назначенная клавиша не
 * конфликтует ни с чем. Порядок результата — порядок входа.
 */
public final class ControlsConflicts {
    private ControlsConflicts() {}

    /** Группы контекста Forge ({@code KeyConflictContext}). */
    public static final String IN_GAME = "in_game";

    public static final String GUI = "gui";
    public static final String UNIVERSAL = "universal";

    /** Пересекаются ли два контекста (передаётся экраном — у модов бывают свои контексты). */
    @FunctionalInterface
    public interface ContextOverlap {
        boolean overlap(String a, String b);
    }

    /** Как в Forge: одинаковые контексты пересекаются, UNIVERSAL пересекается со всеми. */
    public static final ContextOverlap FORGE = (a, b) -> a.equals(b) || UNIVERSAL.equals(a) || UNIVERSAL.equals(b);

    /** Конфликтуют ли две клавиши. */
    public static boolean conflicts(KeyInfo a, KeyInfo b, ContextOverlap overlap) {
        if (a == b || a.id().equals(b.id())) return false;
        KeyInput ka = a.key();
        KeyInput kb = b.key();
        if (ka.isUnbound() || kb.isUnbound()) return false;
        if (!overlap.overlap(a.context(), b.context()) && !overlap.overlap(b.context(), a.context())) return false;
        if (ka.modifier().matches(kb) || kb.modifier().matches(ka)) return true;
        if (!ka.sameInput(kb)) return false;
        if (ka.modifier() == kb.modifier()) return true;
        boolean inGame = overlap.overlap(a.context(), IN_GAME);
        return inGame && (ka.modifier() == KeyInput.Modifier.NONE || kb.modifier() == KeyInput.Modifier.NONE);
    }

    /**
     * Все конфликты: id → id конфликтующих клавиш (в порядке входа). В карте только клавиши с
     * конфликтами; дубли id на входе — учитывается первая.
     */
    public static Map<String, List<String>> find(Collection<KeyInfo> all, ContextOverlap overlap) {
        List<KeyInfo> keys = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (KeyInfo k : all) if (seen.putIfAbsent(k.id(), Boolean.TRUE) == null) keys.add(k);
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (KeyInfo a : keys) {
            if (a.key().isUnbound()) continue;
            List<String> with = new ArrayList<>();
            for (KeyInfo b : keys) if (conflicts(a, b, overlap)) with.add(b.id());
            if (!with.isEmpty()) out.put(a.id(), List.copyOf(with));
        }
        return out;
    }

    /** Конфликты одной клавиши со всеми остальными. */
    public static List<String> of(KeyInfo key, Collection<KeyInfo> all, ContextOverlap overlap) {
        List<String> out = new ArrayList<>();
        for (KeyInfo b : all) if (conflicts(key, b, overlap) && !out.contains(b.id())) out.add(b.id());
        return out;
    }
}
