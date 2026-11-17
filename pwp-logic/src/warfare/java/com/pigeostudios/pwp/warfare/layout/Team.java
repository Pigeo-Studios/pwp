package com.pigeostudios.pwp.warfare.layout;

/** Команда в map_config (ключи {@code BLUE}/{@code RED}, имена констант уходят в JSON — не переименовывать). */
public enum Team {
    BLUE,
    RED;

    /** null — строка не команда. Регистр не важен. */
    public static Team parse(String s) {
        if (s == null) return null;
        for (Team t : values()) {
            if (t.name().equalsIgnoreCase(s.trim())) return t;
        }
        return null;
    }

    public Team enemy() {
        return this == BLUE ? RED : BLUE;
    }
}
