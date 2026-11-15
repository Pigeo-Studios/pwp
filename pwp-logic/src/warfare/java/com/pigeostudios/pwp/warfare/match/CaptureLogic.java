package com.pigeostudios.pwp.warfare.match;

/**
 * Чистая логика захвата точки за тик и тикетов за захват/нейтрализацию (без Forge, покрыта
 * тестами). Побочные эффекты (уведомления, проверка конца игры, синхронизация) — у вызывающего,
 * в {@code GameLogicEvents.handleTeamInfluence}, в прежнем порядке.
 */
public final class CaptureLogic {
    private CaptureLogic() {}

    /**
     * Точка захвата глазами логики (реализует {@code WarfareWorldData.CapturePoint}). Интерфейс
     * держит логику без Minecraft — она живёт в pwp-logic и тестируется за секунды.
     */
    public interface Point {
        String owner();

        void owner(String owner);

        String capturingTeam();

        void capturingTeam(String team);

        float progress();

        void progress(float progress);

        int captureDeduction();

        int ticketPenalty();

        int ticketGainNeutralize();

        int ticketGainCapture();
    }

    public enum Event {
        /** Прогресс изменился, владелец тот же. */
        NONE,
        /** Нейтральная точка захвачена атакующими (owner уже = атакующий). */
        CAPTURED,
        /**
         * Точка противника нейтрализована (прогресс 0). owner ещё старый — его сбрасывает
         * {@link #resetToNeutral} ПОСЛЕ начисления тикетов и проверки конца игры (как раньше).
         */
        NEUTRALIZED
    }

    /** Изменение тикетов команд. */
    public record Tickets(int blue, int red) {
        public static final Tickets ZERO = new Tickets(0, 0);

        static Tickets forTeam(String team, int amount) {
            return "BLUE".equals(team) ? new Tickets(amount, 0) : new Tickets(0, amount);
        }
    }

    /** Один тик влияния атакующей команды на точку со скоростью {@code speed} (доля за тик). */
    public static Event step(Point point, String attackingTeam, float speed) {
        if (point.owner().equals("NEUTRAL")) {
            if (!point.capturingTeam().equals("NONE") && !point.capturingTeam().equals(attackingTeam)) {
                // чужой прогресс на нейтральной точке сначала сбивается в ноль
                point.progress(point.progress() - speed);
                if (point.progress() <= 0.0F) {
                    point.progress(0.0F);
                    point.capturingTeam("NONE");
                }
                return Event.NONE;
            }
            point.capturingTeam(attackingTeam);
            point.progress(point.progress() + speed);
            if (point.progress() >= 1.0F) {
                point.progress(1.0F);
                point.owner(attackingTeam);
                return Event.CAPTURED;
            }
            return Event.NONE;
        }
        if (point.owner().equals(attackingTeam)) {
            if (point.progress() < 1.0F) point.progress(Math.min(1.0F, point.progress() + speed));
            return Event.NONE;
        }
        point.progress(point.progress() - speed);
        return point.progress() <= 0.0F ? Event.NEUTRALIZED : Event.NONE;
    }

    /** Сброс нейтрализованной точки. */
    public static void resetToNeutral(Point point) {
        point.owner("NEUTRAL");
        point.progress(0.0F);
        point.capturingTeam("NONE");
    }

    /**
     * Основные тикеты за захват (до проверки конца игры): Invasion — атакующим бонус
     * (по умолчанию 100), иначе — защитникам минус captureDeduction.
     */
    public static Tickets onCapturedPrimary(Point point, String attacker, boolean invasion, int invasionBonus) {
        if (invasion) return Tickets.forTeam(attacker, invasionBonus > 0 ? invasionBonus : 100);
        if (point.captureDeduction() > 0) return Tickets.forTeam(opposite(attacker), -point.captureDeduction());
        return Tickets.ZERO;
    }

    /** Проверять ли конец игры после захвата (как раньше: Invasion или есть captureDeduction). */
    public static boolean checkGameOverAfterCapture(Point point, boolean invasion) {
        return invasion || point.captureDeduction() > 0;
    }

    /** Бонус атакующим за захват (gainCap), начисляется после проверки конца игры. */
    public static Tickets onCapturedGain(Point point, String attacker) {
        return point.ticketGainCapture() > 0 ? Tickets.forTeam(attacker, point.ticketGainCapture()) : Tickets.ZERO;
    }

    /** Штраф владельцу за потерю точки (кроме Invasion), до проверки конца игры. */
    public static Tickets onNeutralizedPrimary(Point point, boolean invasion) {
        if (invasion) return Tickets.ZERO;
        return Tickets.forTeam(point.owner(), -point.ticketPenalty());
    }

    /** Бонус атакующим за нейтрализацию (gainNeut), после проверки конца игры. */
    public static Tickets onNeutralizedGain(Point point, String attacker) {
        return point.ticketGainNeutralize() > 0
                ? Tickets.forTeam(attacker, point.ticketGainNeutralize())
                : Tickets.ZERO;
    }

    public static String opposite(String team) {
        return "BLUE".equals(team) ? "RED" : "BLUE";
    }
}
