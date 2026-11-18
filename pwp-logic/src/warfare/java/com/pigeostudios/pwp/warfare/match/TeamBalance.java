package com.pigeostudios.pwp.warfare.match;

/**
 * Баланс команд при входе и смене команды (чистая логика, без Forge). Счётчики — игроки ДРУГИХ
 * команд без самого игрока.
 */
public final class TeamBalance {
    /** Команда считается переполненной, если в ней уже на столько больше игроков, чем в другой. */
    public static final int MAX_DIFF = 2;

    public static final String BLUE = "Blue";
    public static final String RED = "Red";

    private TeamBalance() {}

    /** Меньшая команда; поровну — синяя (детерминированно, тестируемо). */
    public static String smaller(int blueOthers, int redOthers) {
        return redOthers < blueOthers ? RED : BLUE;
    }

    /** Можно ли вступить в {@code target} («Blue»/«Red», регистр не важен). */
    public static boolean canJoin(String target, int blueOthers, int redOthers) {
        boolean blue = BLUE.equalsIgnoreCase(target);
        int t = blue ? blueOthers : redOthers;
        int o = blue ? redOthers : blueOthers;
        return t < o + MAX_DIFF;
    }

    /**
     * Нормализация запроса клиента: «BLUE»/«RED» → имя команды, «AUTO» → меньшая, иначе null
     * (неизвестный ввод отклоняется).
     */
    public static String resolve(String requested, int blueOthers, int redOthers) {
        if (requested == null) return null;
        if (BLUE.equalsIgnoreCase(requested)) return BLUE;
        if (RED.equalsIgnoreCase(requested)) return RED;
        if ("AUTO".equalsIgnoreCase(requested)) return smaller(blueOthers, redOthers);
        return null;
    }
}
