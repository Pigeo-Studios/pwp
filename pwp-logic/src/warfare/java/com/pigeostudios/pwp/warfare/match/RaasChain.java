package com.pigeostudios.pwp.warfare.match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Цепочка точек RAAS (этап 15 ROADMAP): точки проецируются на ось «мейн BLUE → мейн RED», делятся на
 * {@code layers} слоёв по порядку на этой оси, из каждого слоя берётся одна случайная точка. Получается
 * осмысленная линия фронта от мейна к мейну, но каждый матч своя. Невыбранные точки в матче не участвуют.
 * Если точек меньше слоёв — берутся все по порядку оси.
 */
public final class RaasChain {
    /** Сколько слоёв по умолчанию (точек в цепочке). */
    public static final int DEFAULT_LAYERS = 5;

    private RaasChain() {}

    public record Point(int index, double x, double z) {}

    /**
     * Индексы точек цепочки в порядке BLUE → RED. {@code blue}/{@code red} — центры мейнов (x, z); при
     * совпадении мейнов — порядок по исходным индексам.
     */
    public static List<Integer> build(List<Point> points, double[] blue, double[] red, int layers, Random rnd) {
        List<Point> sorted = new ArrayList<>(points);
        double ax = red[0] - blue[0];
        double az = red[1] - blue[1];
        double len = Math.hypot(ax, az);
        if (len < 1e-6) {
            sorted.sort(Comparator.comparingInt(Point::index));
        } else {
            double ux = ax / len;
            double uz = az / len;
            sorted.sort(Comparator.comparingDouble((Point p) -> (p.x() - blue[0]) * ux + (p.z() - blue[1]) * uz)
                    .thenComparingInt(Point::index));
        }
        int n = sorted.size();
        int l = Math.max(1, Math.min(layers, n));
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < l; i++) {
            int from = i * n / l;
            int to = (i + 1) * n / l; // [from, to) — непустой, т.к. l <= n
            out.add(sorted.get(from + rnd.nextInt(to - from)).index());
        }
        return out;
    }
}
