package com.pigeostudios.pwp.warfare.match;

/**
 * Чистые правила доступности роли (кита) — без Forge, покрыты тестами. Счётчики собирает
 * вызывающий (меню деплоя {@code PacketRequestKitMenu}, применение кита в {@code ResupplyHandler}).
 */
public final class KitLimits {
    /** Сервер не даёт больше 3 ролей категории FIRE_SUPPORT в отряде (меню деплоя + спавн). */
    public static final int MAX_FIRE_SUPPORT_PER_SQUAD = 3;

    public static final String FIRE_SUPPORT = "FIRE_SUPPORT";

    private KitLimits() {}

    /**
     * Входные данные проверки.
     *
     * @param maxPerTeam лимит на команду (0 — роль выключена, &lt;0 — без лимита)
     * @param maxPerSquad лимит на отряд (&lt;=0 — без лимита)
     * @param minSquadPlayers минимум игроков в отряде (&lt;=0 — не нужно)
     * @param teamCount сколько ДРУГИХ игроков команды уже взяли роль
     * @param squadCount сколько ДРУГИХ игроков отряда уже взяли роль
     * @param squadSize размер отряда игрока, -1 — не в отряде
     * @param fireSupportInSquad ролей FIRE_SUPPORT у других участников отряда
     */
    public record Input(
            int maxPerTeam,
            int maxPerSquad,
            int minSquadPlayers,
            String category,
            int teamCount,
            int squadCount,
            int squadSize,
            int fireSupportInSquad) {}

    /** null — роль доступна, иначе причина (строки совпадают с прежними подписями меню). */
    public static String unavailableReason(Input in) {
        if (in.maxPerTeam() > 0 && in.teamCount() >= in.maxPerTeam()) {
            return "Team Full (" + in.teamCount() + "/" + in.maxPerTeam() + ")";
        }
        if (in.maxPerSquad() > 0 && in.squadCount() >= in.maxPerSquad()) {
            return "Squad Full (" + in.squadCount() + "/" + in.maxPerSquad() + ")";
        }
        if (in.minSquadPlayers() > 0 && (in.squadSize() < 0 || in.squadSize() < in.minSquadPlayers())) {
            return "Need " + in.minSquadPlayers() + " players in Squad";
        }
        if (fireSupportCapReached(in.category(), in.fireSupportInSquad())) {
            return "Max " + MAX_FIRE_SUPPORT_PER_SQUAD + " Fire Support per Squad";
        }
        return null;
    }

    /** true — роль FIRE_SUPPORT взять нельзя: в отряде уже максимум таких ролей. */
    public static boolean fireSupportCapReached(String category, int fireSupportInSquad) {
        return FIRE_SUPPORT.equals(category) && fireSupportInSquad >= MAX_FIRE_SUPPORT_PER_SQUAD;
    }
}
