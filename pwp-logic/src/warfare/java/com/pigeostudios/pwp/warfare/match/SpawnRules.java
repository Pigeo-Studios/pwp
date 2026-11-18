package com.pigeostudios.pwp.warfare.match;

/**
 * Чистые правила запроса деплоя (без Forge, покрыты тестами): формат типа спавна от клиента и
 * «можно ли сейчас спавниться». Применяет {@code PacketRespawnRequest}.
 */
public final class SpawnRules {
    /** Максимальная длина типа спавна по сети. */
    public static final int MAX_TYPE_LENGTH = 64;
    /** Границы мира по X/Z (ванильный {@code Level.MAX_LEVEL_SIZE}). */
    public static final int MAX_XZ = 30_000_000;
    /** Границы BlockPos по Y (12 бит упаковки). */
    public static final int MIN_Y = -2048;

    public static final int MAX_Y = 2047;

    private SpawnRules() {}

    /** Разобранный тип спавна: MAIN, RALLY или HUB с координатами блока ФОБ. */
    public record SpawnType(Kind kind, int x, int y, int z) {
        public enum Kind {
            MAIN,
            RALLY,
            HUB
        }
    }

    /** null — строка не является корректным типом спавна. */
    public static SpawnType parse(String type) {
        if (type == null || type.isEmpty() || type.length() > MAX_TYPE_LENGTH) return null;
        if (type.equals("MAIN")) return new SpawnType(SpawnType.Kind.MAIN, 0, 0, 0);
        if (type.equals("RALLY")) return new SpawnType(SpawnType.Kind.RALLY, 0, 0, 0);
        if (!type.startsWith("HUB:")) return null;
        String[] parts = type.split(":", -1);
        if (parts.length != 4) return null;
        Long x = parseCoord(parts[1]);
        Long y = parseCoord(parts[2]);
        Long z = parseCoord(parts[3]);
        if (x == null || y == null || z == null) return null;
        if (Math.abs(x) > MAX_XZ || Math.abs(z) > MAX_XZ || y < MIN_Y || y > MAX_Y) return null;
        return new SpawnType(SpawnType.Kind.HUB, x.intValue(), y.intValue(), z.intValue());
    }

    /** null — тип корректен, иначе причина отказа (для Net.C2S.validate). */
    public static String validateType(String type) {
        if (type == null || type.isEmpty()) return "type: пусто";
        if (type.length() > MAX_TYPE_LENGTH) return "type: длиннее " + MAX_TYPE_LENGTH;
        return parse(type) == null ? "type: недопустимый тип спавна" : null;
    }

    /** Целое со знаком из 1–9 цифр (без пробелов и плюса); иначе null. */
    private static Long parseCoord(String s) {
        int start = s.startsWith("-") ? 1 : 0;
        int digits = s.length() - start;
        if (digits < 1 || digits > 9) return null;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return null;
        }
        return Long.parseLong(s);
    }

    /**
     * Состояние игрока в момент запроса деплоя.
     *
     * @param dead игрок мёртв и ждёт возрождения (экран деплоя после смерти)
     * @param spectator режим наблюдателя (ждёт деплоя на экране выбора)
     * @param downed в ноке — запрос деплоя означает «сдаться» (обрабатывается отдельно)
     * @param teamKillPunished наблюдатель-наказание за тимкилл — деплой запрещён
     */
    public record PlayerState(boolean dead, boolean spectator, boolean downed, boolean teamKillPunished) {}

    /**
     * null — деплой разрешён (или, для нока, разрешена сдача), иначе ключ локализации причины отказа. Живой
     * игрок на поле не может телепортироваться к рали/ФОБ и получить новый кит по запросу.
     */
    public static String denyReason(PlayerState s) {
        if (s.teamKillPunished()) return "pwpwarfare.msg.spawn.deny_teamkill";
        if (s.downed()) return null;
        if (s.dead() || s.spectator()) return null;
        return "pwpwarfare.msg.spawn.deny_alive";
    }
}
