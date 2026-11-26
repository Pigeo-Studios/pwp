package com.pigeostudios.pwp.warfare.match;

/**
 * Кто что видит из состояния матча (как в Squad): чужие ФОБы/хабы, маркеры, техника, отряды
 * и запросы арты врагу НЕ отправляются. Клиент и раньше скрывал их в UI, но получал — читу
 * хватало прочитать пакет. Решает сервер, по этому правилу; клиентские фильтры остаются как есть.
 *
 * <p>Зритель описывается ключом: {@link #ALL} (наблюдатель — креатив/спектатор), {@code "BLUE"},
 * {@code "RED"} или {@link #NONE} (без команды — видит только нейтральное).
 */
public final class TeamVisibility {
    public static final String ALL = "ALL";
    public static final String NONE = "NONE";

    private TeamVisibility() {}

    /** Ключ зрителя по имени scoreboard-команды ("Blue"/"Red", регистр любой) и признаку наблюдателя. */
    public static String viewerKey(String teamName, boolean observer) {
        if (observer) return ALL;
        String t = normalize(teamName);
        return t == null ? NONE : t;
    }

    /**
     * Ключ зрителя с учётом прав (этап 12): видеть ВСЁ может только наблюдатель-модератор (MODERATOR+ в
     * спектаторе/креативе). Игрок в спектаторе по другой причине (наказан за тимкилл, ждёт) — видит только
     * свою команду: иначе он «наблюдатель со всей картой» и диктует позиции врагов своим в войс.
     */
    public static String viewerKey(String teamName, boolean observerMode, boolean staff) {
        return viewerKey(teamName, observerMode && staff);
    }

    /** Видна ли зрителю вещь, принадлежащая команде {@code ownerTeam}. Нейтральное/без команды — видно всем. */
    public static boolean visible(String ownerTeam, String viewerKey) {
        if (ALL.equals(viewerKey)) return true;
        String owner = normalize(ownerTeam);
        if (owner == null) return true;
        return owner.equals(viewerKey);
    }

    /** "BLUE"/"RED" или null для пустого, "NEUTRAL", "NONE" и прочего. */
    public static String normalize(String team) {
        if (team == null) return null;
        if (team.equalsIgnoreCase("BLUE")) return "BLUE";
        if (team.equalsIgnoreCase("RED")) return "RED";
        return null;
    }
}
