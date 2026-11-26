package com.pigeostudios.pwp.warfare.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Состав дивизии из редактора F6 (этап 6d): строка {@code "машина:кол-во;…"} → записи {@link Division.Entry}.
 * Роль машины берётся из пула фракции ({@code машина → категория}), а не от клиента; неизвестная машина или
 * количество вне 0..{@link #MAX_PER_VEHICLE} — null (отказ целиком); нули пропускаются.
 */
public final class DivisionSpec {
    public static final int MAX_PER_VEHICLE = 5;

    private DivisionSpec() {}

    public static List<Division.Entry> parse(String spec, Map<String, String> poolRoles) {
        List<Division.Entry> out = new ArrayList<>();
        if (spec == null || spec.isBlank()) return out;
        for (String part : spec.split(";")) {
            if (part.isBlank()) continue;
            int c = part.lastIndexOf(':');
            if (c <= 0) return null;
            String name = part.substring(0, c).trim();
            int count;
            try {
                count = Integer.parseInt(part.substring(c + 1).trim());
            } catch (NumberFormatException e) {
                return null;
            }
            if (!poolRoles.containsKey(name) || count < 0 || count > MAX_PER_VEHICLE) return null;
            if (count == 0) continue;
            String role = poolRoles.get(name);
            role = role == null || role.isBlank() ? "TRANSPORT" : role.toUpperCase(Locale.ROOT);
            out.add(new Division.Entry(role, name, count, 0));
        }
        return out;
    }
}
