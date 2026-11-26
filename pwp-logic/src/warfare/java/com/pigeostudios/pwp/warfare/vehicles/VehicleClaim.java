package com.pigeostudios.pwp.warfare.vehicles;

/**
 * Закрепление техники за отрядом, как в Squad (этап 10 ROADMAP). Решение принимается только при посадке
 * ВОДИТЕЛЕМ (первый пассажир); пассажиром садиться можно всегда.
 *
 * <ul>
 *   <li>Машина свободна (нет закрепления или отряд-владелец распался) — водит любой; у игрока есть отряд —
 *       машина закрепляется за ним.
 *   <li>Машина закреплена за живым отрядом — водить может только он (модераторы и выше — всегда).
 * </ul>
 */
public final class VehicleClaim {
    public enum Decision {
        ALLOW,
        CLAIM,
        DENY
    }

    private VehicleClaim() {}

    /**
     * @param claimSquad id отряда-владельца, 0 — не закреплена
     * @param claimSquadAlive отряд-владелец ещё существует в той же команде
     * @param playerSquad id отряда игрока, 0 — без отряда
     */
    public static Decision decide(int claimSquad, boolean claimSquadAlive, int playerSquad, boolean staff) {
        boolean claimed = claimSquad != 0 && claimSquadAlive;
        if (claimed) {
            if (playerSquad == claimSquad || staff) return Decision.ALLOW;
            return Decision.DENY;
        }
        return playerSquad != 0 ? Decision.CLAIM : Decision.ALLOW;
    }
}
