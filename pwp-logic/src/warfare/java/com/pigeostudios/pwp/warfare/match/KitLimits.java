package com.pigeostudios.pwp.warfare.match;

import java.util.List;
import java.util.function.Function;

/**
 * Чистые правила доступности роли (кита) — без Forge, покрыты тестами. ЕДИНСТВЕННЫЙ путь проверки:
 * меню деплоя, выбор кита и применение кита при спавне (PendingKit и CurrentKit) вызывают
 * {@link #unavailableReason}; счётчики собирает {@code KitAccess} (pwp-warfare) через
 * {@link #occupancy}/{@link #fireSupportInSquad}.
 *
 * <p>«Занятая» роль: у другого игрока команды она CurrentKit ИЛИ PendingKit. Поэтому лимит
 * никогда не превышается: из двух игроков, ожидающих одну роль с последним местом, её получит
 * не больше одного.
 */
public final class KitLimits {
    /** Сервер не даёт больше 3 ролей категории FIRE_SUPPORT в отряде (меню деплоя + спавн). */
    public static final int MAX_FIRE_SUPPORT_PER_SQUAD = 3;

    public static final String FIRE_SUPPORT = "FIRE_SUPPORT";

    /** Кит «без роли» доступен всегда, лимиты к нему не применяются. */
    public static final String UNASSIGNED = "Unassigned";

    private KitLimits() {}

    /**
     * Входные данные проверки.
     *
     * @param kitName имя кита ({@link #UNASSIGNED} — доступен всегда)
     * @param maxPerTeam лимит на команду (0 — роль выключена, &lt;0 — без лимита)
     * @param maxPerSquad лимит на отряд (&lt;=0 — без лимита)
     * @param minSquadPlayers минимум игроков в отряде (&lt;=0 — не нужно)
     * @param leaderOnly роль только для лидера отряда
     * @param isLeader игрок — лидер отряда
     * @param teamCount сколько ДРУГИХ игроков команды уже заняли роль ({@link #occupancy})
     * @param squadCount сколько ДРУГИХ игроков отряда уже заняли роль
     * @param squadSize размер отряда игрока, -1 — не в отряде
     * @param fireSupportInSquad ролей FIRE_SUPPORT у других участников отряда
     */
    public record Input(
            String kitName,
            int maxPerTeam,
            int maxPerSquad,
            int minSquadPlayers,
            String category,
            boolean leaderOnly,
            boolean isLeader,
            int teamCount,
            int squadCount,
            int squadSize,
            int fireSupportInSquad) {}

    /** null — роль доступна, иначе причина по-английски (лог, тесты; игроку — {@link #reasonOf} + lang). */
    public static String unavailableReason(Input in) {
        Reason r = reasonOf(in);
        return r == null ? null : r.english();
    }

    /**
     * Причина недоступности с lang-ключом и аргументами — для показа игроку на его языке.
     * Ключи: {@code pwpwarfare.kit.reason.<code>} (en/ru/uk в pwp-warfare).
     */
    public record Reason(String code, int... args) {
        public static final String KEY_PREFIX = "pwpwarfare.kit.reason.";
        /** Разделитель полей в строке для сети/меню: {@code key|arg|arg}. */
        public static final char SEP = '|';

        public String key() {
            return KEY_PREFIX + code;
        }

        public String english() {
            return switch (code) {
                case "leader_only" -> "Squad Leader only";
                case "disabled" -> "Role Disabled";
                case "team_full" -> "Team Full (" + args[0] + "/" + args[1] + ")";
                case "squad_full" -> "Squad Full (" + args[0] + "/" + args[1] + ")";
                case "need_squad" -> "Need " + args[0] + " players in Squad";
                case "fire_support" -> "Max " + args[0] + " Fire Support per Squad";
                default -> code;
            };
        }

        /** Строка {@code key|a|b} — уходит клиенту вместо готового текста. */
        public String encoded() {
            StringBuilder b = new StringBuilder(key());
            for (int a : args) b.append(SEP).append(a);
            return b.toString();
        }

        /** Обратный разбор {@link #encoded()}: [ключ, аргументы...]; не наш формат — null. */
        public static String[] decode(String s) {
            if (s == null || !s.startsWith(KEY_PREFIX)) return null;
            return s.split("\\|", -1);
        }
    }

    /** null — роль доступна, иначе причина (единое правило для меню, выбора и спавна). */
    public static Reason reasonOf(Input in) {
        if (UNASSIGNED.equals(in.kitName())) return null;
        if (in.leaderOnly() && !in.isLeader()) return new Reason("leader_only");
        if (in.maxPerTeam() == 0) return new Reason("disabled");
        if (in.maxPerTeam() > 0 && in.teamCount() >= in.maxPerTeam()) {
            return new Reason("team_full", in.teamCount(), in.maxPerTeam());
        }
        if (in.maxPerSquad() > 0 && in.squadCount() >= in.maxPerSquad()) {
            return new Reason("squad_full", in.squadCount(), in.maxPerSquad());
        }
        if (in.minSquadPlayers() > 0 && (in.squadSize() < 0 || in.squadSize() < in.minSquadPlayers())) {
            return new Reason("need_squad", in.minSquadPlayers());
        }
        if (fireSupportCapReached(in.category(), in.fireSupportInSquad())) {
            return new Reason("fire_support", MAX_FIRE_SUPPORT_PER_SQUAD);
        }
        return null;
    }

    /** true — роль FIRE_SUPPORT взять нельзя: в отряде уже максимум таких ролей. */
    public static boolean fireSupportCapReached(String category, int fireSupportInSquad) {
        return FIRE_SUPPORT.equals(category) && fireSupportInSquad >= MAX_FIRE_SUPPORT_PER_SQUAD;
    }

    /**
     * Другой игрок той же команды (сам проверяемый в список не входит).
     *
     * @param currentKit CurrentKit ("" — нет)
     * @param pendingKit PendingKit ("" — нет)
     * @param sameSquad в том же отряде, что и проверяемый (false, если проверяемый без отряда)
     */
    public record Holder(String currentKit, String pendingKit, boolean sameSquad) {
        public Holder {
            currentKit = currentKit != null ? currentKit : "";
            pendingKit = pendingKit != null ? pendingKit : "";
        }

        /** Занимает ли игрок роль: она у него текущая ИЛИ ожидающая. */
        public boolean occupies(String kitName) {
            return currentKit.equals(kitName) || pendingKit.equals(kitName);
        }

        /** Кит, с которым игрок пойдёт в бой: PendingKit, иначе CurrentKit. */
        public String effectiveKit() {
            return !pendingKit.isEmpty() ? pendingKit : currentKit;
        }
    }

    /** Сколько других игроков заняли роль: в команде и в отряде проверяемого. */
    public record Occupancy(int team, int squad) {}

    public static Occupancy occupancy(List<Holder> others, String kitName) {
        int team = 0, squad = 0;
        for (Holder h : others) {
            if (!h.occupies(kitName)) continue;
            team++;
            if (h.sameSquad()) squad++;
        }
        return new Occupancy(team, squad);
    }

    /** Ролей FIRE_SUPPORT у других участников отряда (по киту, с которым они пойдут в бой). */
    public static int fireSupportInSquad(List<Holder> others, Function<String, String> categoryOf) {
        int count = 0;
        for (Holder h : others) {
            if (h.sameSquad() && FIRE_SUPPORT.equals(categoryOf.apply(h.effectiveKit()))) count++;
        }
        return count;
    }
}
