package com.pwp.api.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Единый реестр игровых режимов (этап 7 ROADMAP): лобби (голосование), матч (правила захвата, техника), админка.
 * Новый режим — запись здесь, а не новые ветки {@code "invasion".equals(...)} по коду. Различия режимов — данные
 * {@link Mode}; особые правила Invasion (блокировка точек, бонус атакующим) остаются в матче.
 *
 * <ul>
 *   <li>{@code aas} — Advance and Secure: точки по цепочке приоритетов, техника есть;
 *   <li>{@code invasion} — защитник держит все точки, атакующий берёт по цепочке;
 *   <li>{@code skirmish} — «Стычка» для малого онлайна и маленьких карт: любая точка в любом порядке, без техники.
 *       Предлагается в голосовании, только если карта объявила его в {@code modes} map_config.
 *   <li>{@code raas} — Random AAS: как AAS, но цепочка точек каждый матч новая (по одной точке из каждого «слоя»
 *       между мейнами, см. {@code RaasChain}); одна карта — разные бои.
 *   <li>{@code training} — «Стрельбище»: тренировка без счёта — тикеты не тратятся, лимиты ролей сняты, быстрый
 *       респаун. Только если карта объявила {@code modes.training}.
 * </ul>
 */
public final class GameModes {

    /** Порядок захвата точек. */
    public enum Order {
        /** Цепочка AAS: точку с приоритетом N можно брать, только владея точкой N−1 своей нумерации. */
        CHAIN,
        /** Invasion: цепочка для атакующего, защитник нейтральные не берёт. */
        DEFEND,
        /** Любая точка в любой момент. */
        FREE
    }

    /**
     * Режим. {@code displayName}/{@code description} — подписи лобби (как прежние MODE_* лобби, не переведены);
     * {@code optIn} — предлагать в голосовании, только если карта объявила режим.
     */
    public record Mode(
            String id, String displayName, String description, Order order, boolean vehicles, boolean optIn) {}

    public static final Mode AAS = new Mode(
            "aas",
            "Advance and Secure",
            "Обе команды на равных. Захватывайте точки в порядке очереди, чтобы обнулить тикеты противника. Есть"
                    + " ticket bleed — команда без точек теряет тикеты.",
            Order.CHAIN,
            true,
            false);
    public static final Mode INVASION = new Mode(
            "invasion",
            "INVASION",
            "Одна команда защищает все точки, вторая штурмует. Захваченные точки блокируются. Нет ticket bleed."
                    + " Атакующие получают +100 билетов за захват, защитники теряют всё при потере последней точки.",
            Order.DEFEND,
            true,
            false);
    public static final Mode SKIRMISH = new Mode(
            "skirmish",
            "СТЫЧКА",
            "Малый режим для небольшого онлайна: без техники, любые точки в любом порядке. Держите больше точек —"
                    + " у врага тают тикеты.",
            Order.FREE,
            false,
            true);

    public static final Mode RAAS = new Mode(
            "raas",
            "RAAS",
            "Random Advance and Secure: как AAS, но цепочка точек каждый матч своя — одна карта, разные бои.",
            Order.CHAIN,
            true,
            false);
    public static final Mode TRAINING = new Mode(
            "training",
            "СТРЕЛЬБИЩЕ",
            "Тренировка: любые роли без лимитов, техника, тикеты не тратятся, быстрый респаун. Попробуй всё без"
                    + " давления матча.",
            Order.FREE,
            true,
            true);

    private static final List<Mode> ALL = List.of(AAS, INVASION, SKIRMISH, RAAS, TRAINING);

    /** Тренировка: без счёта и лимитов ролей. */
    public static boolean isTraining(String id) {
        return TRAINING.id().equalsIgnoreCase(id == null ? "" : id.trim());
    }

    /** RAAS: цепочка точек случайная на матч. */
    public static boolean isRaas(String id) {
        return RAAS.id().equalsIgnoreCase(id == null ? "" : id.trim());
    }

    /** Респаун в тренировке, секунд. */
    public static final int TRAINING_RESPAWN_SECONDS = 3;

    private GameModes() {}

    public static List<Mode> all() {
        return ALL;
    }

    /** Режим по id (регистр не важен); неизвестный — null. */
    public static Mode byId(String id) {
        if (id == null) return null;
        String k = id.trim().toLowerCase(Locale.ROOT);
        for (Mode m : ALL) if (m.id().equals(k)) return m;
        return null;
    }

    /** Режим по id; неизвестный/пустой — AAS (как прежнее поведение по умолчанию). */
    public static Mode orAas(String id) {
        Mode m = byId(id);
        return m == null ? AAS : m;
    }

    /** Минимум тикетов малого режима. */
    public static final int SMALL_MODE_MIN_TICKETS = 100;

    /**
     * Тикеты режима по умолчанию из тикетов карты (этап 7b): у малого режима без техники («Стычка») бой короче —
     * половина тикетов карты (не меньше {@link #SMALL_MODE_MIN_TICKETS}); AAS/Invasion — как в карте.
     */
    public static int ticketsFor(String modeId, int mapTickets) {
        Mode m = orAas(modeId);
        if (m.vehicles()) return mapTickets;
        return Math.max(SMALL_MODE_MIN_TICKETS, mapTickets / 2);
    }

    /** Режимы голосования для карты: все не-optIn + optIn, объявленные картой ({@code map_config.modes}). */
    public static List<Mode> votable(Collection<String> mapModes) {
        List<Mode> out = new ArrayList<>();
        for (Mode m : ALL) {
            if (!m.optIn()) {
                out.add(m);
                continue;
            }
            if (mapModes == null) continue;
            for (String s : mapModes) {
                if (m.id().equalsIgnoreCase(s)) {
                    out.add(m);
                    break;
                }
            }
        }
        return out;
    }

    /** Состояние точки для правила захвата. */
    public record PointState(int bluePriority, int redPriority, String owner, boolean invLocked) {
        int priority(String team) {
            return "BLUE".equals(team) ? bluePriority : redPriority;
        }
    }

    /**
     * Может ли {@code team} захватывать {@code target} (прежняя логика GameLogicEvents.canCapture для AAS/Invasion
     * + порядок FREE). {@code defender} — защитник Invasion.
     */
    public static boolean canCapture(
            Mode mode, PointState target, String team, Collection<PointState> all, String defender) {
        if (mode.order() == Order.FREE) return true;
        if (mode.order() == Order.DEFEND) {
            // взятую атакующим точку прежний владелец не отбивает; нейтральные защитник не берёт
            if (target.invLocked() && !team.equals(target.owner())) return false;
            if ("NEUTRAL".equals(target.owner()) && team.equals(defender)) return false;
        }
        int current = target.priority(team);
        if (current <= 1) return true;
        int required = current - 1;
        for (PointState p : all) {
            if (p.priority(team) == required && team.equals(p.owner())) return true;
        }
        return false;
    }
}
