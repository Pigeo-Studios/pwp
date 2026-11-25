package com.pigeostudios.pwp.warfare.tutorial;

/**
 * Обучение новичка (этап 5 ROADMAP): пять шагов первого матча — команда → отряд → роль → деплой → захват
 * точки. Прогресс только вперёд: выполненный шаг не возвращается (вышел из отряда — обучение не начинается
 * заново). Состояние берётся из клиента снимком {@link Snapshot}; показ, клавиши и хранение «пройдено» — у
 * вызывающего ({@code client/TutorialClient}).
 */
public final class Tutorial {

    public enum Step {
        TEAM,
        SQUAD,
        ROLE,
        DEPLOY,
        CAPTURE,
        DONE;

        /** Номер шага 1..{@link #TOTAL}; у DONE — TOTAL. */
        public int number() {
            return Math.min(ordinal() + 1, TOTAL);
        }

        /** Ключ текста подсказки шага: {@code gui.pwpwarfare.tutorial.<шаг>}. */
        public String langKey() {
            return "gui.pwpwarfare.tutorial." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final int TOTAL = 5;

    /**
     * Снимок клиента. {@code battle} — бой идёт (после GO); {@code outsideMain} — живой и вне своего мейна;
     * {@code onPoint} — стоит в зоне точки захвата.
     */
    public record Snapshot(
            boolean hasTeam, boolean inSquad, boolean hasKit, boolean battle, boolean outsideMain, boolean onPoint) {}

    private Step step;

    public Tutorial() {
        this(Step.TEAM);
    }

    public Tutorial(Step start) {
        this.step = start == null ? Step.TEAM : start;
    }

    public Step step() {
        return step;
    }

    public boolean done() {
        return step == Step.DONE;
    }

    /** Продвинуть по снимку (сразу через несколько шагов, если уже выполнены). Возвращает текущий шаг. */
    public Step update(Snapshot s) {
        while (step != Step.DONE && satisfied(step, s)) step = Step.values()[step.ordinal() + 1];
        return step;
    }

    /** Пропустить обучение целиком. */
    public void skip() {
        step = Step.DONE;
    }

    static boolean satisfied(Step st, Snapshot s) {
        return switch (st) {
            case TEAM -> s.hasTeam();
            case SQUAD -> s.inSquad();
            case ROLE -> s.hasKit();
            case DEPLOY -> s.battle() && s.outsideMain();
            case CAPTURE -> s.battle() && s.onPoint();
            case DONE -> true;
        };
    }
}
