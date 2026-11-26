package com.pigeostudios.pwp.warfare.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Расстановка подписей на тактической карте без наложений (этап 12 ROADMAP). Подписи (имена точек, ники,
 * материалы хаба) расставляются по приоритету: для каждой пробуются места у её якоря — снизу, сверху, справа,
 * слева; занято всё (другой подписью или запретной зоной, например иконкой) — подпись не рисуется, иконка
 * остаётся. Вызывать один раз на кадр: {@link #reset} → {@link #block} (иконки) → {@link #place}.
 * Без аллокаций на кадр сверх списка прямоугольников.
 */
public final class LabelPlacer {
    /** Позиция относительно якоря. */
    public enum Side {
        BELOW,
        ABOVE,
        RIGHT,
        LEFT
    }

    /** Итог: левый верхний угол подписи или null (места нет). */
    public record Spot(int x, int y, Side side) {}

    private final List<int[]> taken = new ArrayList<>();
    private int minX = Integer.MIN_VALUE;
    private int minY = Integer.MIN_VALUE;
    private int maxX = Integer.MAX_VALUE;
    private int maxY = Integer.MAX_VALUE;

    /** Новый кадр: область карты на экране (подпись не выходит за неё). */
    public void reset(int x, int y, int w, int h) {
        taken.clear();
        minX = x;
        minY = y;
        maxX = x + w;
        maxY = y + h;
    }

    /** Запретная зона (иконка объекта) — подписи на неё не лезут. */
    public void block(int x, int y, int w, int h) {
        taken.add(new int[] {x, y, x + w, y + h});
    }

    /**
     * Поставить подпись {@code w×h} у якоря {@code (ax, ay)} с отступом {@code gap} от иконки радиуса
     * {@code r}. Возвращает место или null.
     */
    public Spot place(int ax, int ay, int r, int w, int h, int gap) {
        return place(ax, ay, r, w, h, gap, DEFAULT_ORDER);
    }

    /** Порядок мест по умолчанию (имена точек — под флагом). */
    public static final Side[] DEFAULT_ORDER = {Side.BELOW, Side.ABOVE, Side.RIGHT, Side.LEFT};

    /** Ники — над иконкой игрока, как в Squad. */
    public static final Side[] ABOVE_FIRST = {Side.ABOVE, Side.BELOW, Side.RIGHT, Side.LEFT};

    /** То же с порядком мест {@code order}. */
    public Spot place(int ax, int ay, int r, int w, int h, int gap, Side[] order) {
        for (Side s : order) {
            int x;
            int y;
            switch (s) {
                case BELOW -> {
                    x = ax - w / 2;
                    y = ay + r + gap;
                }
                case ABOVE -> {
                    x = ax - w / 2;
                    y = ay - r - gap - h;
                }
                case RIGHT -> {
                    x = ax + r + gap;
                    y = ay - h / 2;
                }
                default -> {
                    x = ax - r - gap - w;
                    y = ay - h / 2;
                }
            }
            if (x < minX || y < minY || x + w > maxX || y + h > maxY) continue;
            if (free(x, y, x + w, y + h)) {
                taken.add(new int[] {x, y, x + w, y + h});
                return new Spot(x, y, s);
            }
        }
        return null;
    }

    private boolean free(int x0, int y0, int x1, int y1) {
        for (int[] t : taken) {
            if (x0 < t[2] && x1 > t[0] && y0 < t[3] && y1 > t[1]) return false;
        }
        return true;
    }

    /** Сколько прямоугольников занято (подписи + зоны) — для тестов и отладки. */
    public int size() {
        return taken.size();
    }
}
