package com.pigeostudios.pwp.warfare.vehicles;

import java.util.Locale;

/**
 * Кто водит технику специалистов (как роль «Экипаж/Пилот» в Squad). Раньше эти правила были скопированы в
 * четыре класса (посадка, проверка водителя, запрос лидеру, подсказка HUD); теперь — одно место.
 */
public final class CrewRules {
    private CrewRules() {}

    private static boolean air(String t) {
        return t.equals("HELICOPTER") || t.contains("CAS") || t.contains("SUPPLY HELICOPTER");
    }

    private static boolean armor(String t) {
        return t.equals("TANK") || t.equals("APC") || t.equals("MOBILE ZU");
    }

    /** Машина специалиста: водить может только экипаж/пилот и (новую) — с разрешения лидера отряда. */
    public static boolean isSpecialist(String vehicleType) {
        if (vehicleType == null) return false;
        String t = vehicleType.toUpperCase(Locale.ROOT);
        return air(t) || armor(t);
    }

    /** Подходит ли кит игрока к машине: авиация — пилот, броня — механик, остальное — любой. */
    public static boolean hasCorrectKit(String vehicleType, String kit) {
        if (vehicleType == null) return true;
        String t = vehicleType.toUpperCase(Locale.ROOT);
        String k = kit == null ? "" : kit;
        if (air(t)) return k.equals("Pilot") || k.equals("Pilot Officer");
        if (armor(t)) return k.equals("Mechanic") || k.equals("Mechanic Officer");
        return true;
    }
}
