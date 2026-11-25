package com.pigeostudios.pwp.warfare.match;

/**
 * Сторож матч-сервера: цикл «лобби → матч → итоги → лобби» не должен вставать из-за матча, который никогда
 * не закончится сам (этап 4 ROADMAP). Лобби держит слот {@code maxMatches}, пока матч-сервер жив, — пустой или
 * бесконечный матч останавливал авто-цикл до ручного вмешательства.
 *
 * <ul>
 *   <li>{@link Action#SHUTDOWN_EMPTY} — на сервере никого {@code emptyMs} подряд после того, как кто-то уже
 *       заходил (до первого входа сервер пуст законно: переброс из лобби, предпрогрев);
 *   <li>{@link Action#SHUTDOWN_NO_SHOW} — никто не зашёл за {@code noShowMs} после того, как сервер перестал
 *       ждать настройки лобби (переброс сорвался);
 *   <li>{@link Action#END_BY_TIME} — бой идёт дольше {@code maxMatchMs}: победа по тикетам, дальше обычный
 *       экран итогов и возврат в лобби.
 * </ul>
 * Каждое действие выдаётся один раз. 0 в лимите — правило выключено. Время — миллисекунды реального времени.
 */
public final class MatchWatchdog {

    public enum Action {
        NONE,
        SHUTDOWN_EMPTY,
        SHUTDOWN_NO_SHOW,
        END_BY_TIME
    }

    /** Вход одного шага (раз в секунду). */
    public record Input(
            long nowMs,
            int online,
            boolean awaitingLobby,
            boolean shuttingDown,
            boolean battleRunning,
            long battleStartedMs) {}

    private final long emptyMs;
    private final long noShowMs;
    private final long maxMatchMs;

    private boolean anyoneJoined;
    private long emptySince = -1;
    private long readySince = -1;
    private boolean fired;
    private boolean timeFired;

    public MatchWatchdog(long emptyMs, long noShowMs, long maxMatchMs) {
        this.emptyMs = Math.max(0, emptyMs);
        this.noShowMs = Math.max(0, noShowMs);
        this.maxMatchMs = Math.max(0, maxMatchMs);
    }

    public Action step(Input in) {
        if (fired || in.shuttingDown()) return Action.NONE;
        if (in.awaitingLobby()) {
            readySince = -1;
            emptySince = -1;
            return Action.NONE;
        }
        if (readySince < 0) readySince = in.nowMs();

        if (in.online() > 0) {
            anyoneJoined = true;
            emptySince = -1;
        } else if (anyoneJoined) {
            if (emptySince < 0) emptySince = in.nowMs();
            if (emptyMs > 0 && in.nowMs() - emptySince >= emptyMs) return fire(Action.SHUTDOWN_EMPTY);
        } else if (noShowMs > 0 && in.nowMs() - readySince >= noShowMs) {
            return fire(Action.SHUTDOWN_NO_SHOW);
        }

        if (!timeFired
                && maxMatchMs > 0
                && in.battleRunning()
                && in.battleStartedMs() > 0
                && in.nowMs() - in.battleStartedMs() >= maxMatchMs) {
            timeFired = true; // дальше — обычный конец матча (экран итогов → лобби), сторож не мешает
            return Action.END_BY_TIME;
        }
        return Action.NONE;
    }

    private Action fire(Action a) {
        fired = true;
        return a;
    }

    /** Кто побеждает по времени: больше тикетов; поровну — BLUE (обычно обороняющаяся сторона AAS-карты). */
    public static boolean blueWinsOnTime(int blueTickets, int redTickets) {
        return blueTickets >= redTickets;
    }
}
