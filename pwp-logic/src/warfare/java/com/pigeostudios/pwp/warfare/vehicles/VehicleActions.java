package com.pigeostudios.pwp.warfare.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Радиальное меню техники (этап 17a, как в Squad): какие действия доступны игроку над машиной своей команды.
 * Решает сервер — и для показа пунктов, и при выполнении (клиент только просит).
 *
 * <ul>
 *   <li>{@link Action#CLAIM} — лидер отряда закрепляет свободную машину за своим отрядом;
 *   <li>{@link Action#RELEASE} — лидер отряда-владельца снимает закрепление (и замок);
 *   <li>{@link Action#LOCK}/{@link Action#UNLOCK} — «только мой отряд»: чужие не садятся даже пассажирами;
 *   <li>{@link Action#REQUEST_DRIVE} — не-лидер просит разрешения водить новую машину специалиста;
 *   <li>{@link Action#APPROVE}/{@link Action#DENY} — лидер отвечает на запросы своего отряда.
 * </ul>
 */
public final class VehicleActions {
    public enum Action {
        CLAIM,
        RELEASE,
        LOCK,
        UNLOCK,
        REQUEST_DRIVE,
        APPROVE,
        DENY;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Action parse(String s) {
            if (s == null) return null;
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * Состояние для решения.
     *
     * @param ownTeam машина команды игрока
     * @param squadLeader игрок — лидер отряда
     * @param playerSquad id отряда игрока, 0 — без отряда
     * @param claimSquad id отряда-владельца, 0 — не закреплена
     * @param claimAlive отряд-владелец ещё существует
     * @param locked «только мой отряд» включено
     * @param needsPermission машина специалиста ещё не выезжала — водить можно только с разрешения лидера
     * @param pendingRequests есть запросы на вождение от отряда этого лидера
     */
    public record State(
            boolean ownTeam,
            boolean squadLeader,
            int playerSquad,
            int claimSquad,
            boolean claimAlive,
            boolean locked,
            boolean needsPermission,
            boolean pendingRequests) {
        boolean claimed() {
            return claimSquad != 0 && claimAlive;
        }

        boolean mine() {
            return claimed() && playerSquad == claimSquad;
        }
    }

    private VehicleActions() {}

    /** Доступные действия в порядке пунктов радиалки (сверху по часовой). */
    public static List<Action> available(State s) {
        List<Action> out = new ArrayList<>();
        if (!s.ownTeam() || s.playerSquad() == 0) return out;
        if (s.squadLeader()) {
            if (!s.claimed()) out.add(Action.CLAIM);
            if (s.mine()) {
                out.add(s.locked() ? Action.UNLOCK : Action.LOCK);
                out.add(Action.RELEASE);
            }
            if (s.pendingRequests()) {
                out.add(Action.APPROVE);
                out.add(Action.DENY);
            }
        } else if (s.needsPermission() && (!s.claimed() || s.mine())) {
            out.add(Action.REQUEST_DRIVE);
        }
        return out;
    }

    public static boolean allowed(State s, Action a) {
        return a != null && available(s).contains(a);
    }
}
