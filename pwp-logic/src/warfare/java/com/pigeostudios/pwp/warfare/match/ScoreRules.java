package com.pigeostudios.pwp.warfare.match;

/**
 * Очки за вклад в победу команды (этап 15 ROADMAP), как «счёт» в табло Squad: не только убийства, а всё, что
 * помогает команде — оживления, захваты, уничтоженная техника, доставленное снабжение. Тимкилл отнимает очки.
 * Чистая формула — табло и итоги матча считают одинаково.
 */
public final class ScoreRules {
    public static final int KILL = 10;
    public static final int REVIVE = 15;
    public static final int CAPTURE = 25;
    public static final int VEHICLE_DESTROYED = 30;
    /** За каждые {@link #SUPPLY_UNIT} доставленных единиц снабжения. */
    public static final int SUPPLY = 5;

    public static final int SUPPLY_UNIT = 100;
    public static final int TEAMKILL = -20;

    private ScoreRules() {}

    public static int score(int kills, int revives, int captures, int vehiclesDestroyed, int supplies, int teamKills) {
        int s = kills * KILL
                + revives * REVIVE
                + captures * CAPTURE
                + vehiclesDestroyed * VEHICLE_DESTROYED
                + Math.max(0, supplies) / SUPPLY_UNIT * SUPPLY
                + teamKills * TEAMKILL;
        return Math.max(0, s);
    }
}
