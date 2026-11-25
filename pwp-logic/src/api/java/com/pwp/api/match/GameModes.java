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

    private static final List<Mode> ALL = List.of(AAS, INVASION, SKIRMISH);

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
