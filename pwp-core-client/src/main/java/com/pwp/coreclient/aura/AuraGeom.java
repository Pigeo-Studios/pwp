package com.pwp.coreclient.aura;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Примитивы донат-аур: рисование геометрии напрямую в мир (immediate-mode,
 * без частиц).
 *
 * ВАЖНО (v2): кольца/дуги/спирали больше НЕ используют камерные right/up для
 * расчёта толщины ленты. Раньше нормаль ленты бралась из ориентации камеры
 * (`ribbon(... right, up ...)`), из-за чего при почти осевом взгляде на
 * сегмент проекция схлопывалась в ноль и код проваливался в fallback —
 * соседние сегменты получали разную ориентацию, и кольцо визуально
 * «дёргалось»/скручивалось при повороте камеры.
 *
 * Теперь нормаль ленты считается из ГЕОМЕТРИИ самой фигуры:
 *  - плоские кольца/дуги (flatArc) — ширина по радиусу, лежат в плоскости Y,
 *    как обычная плитка (ringTile), просто с fade и произвольной дугой;
 *  - спираль (helix) — нормаль направлена радиально наружу от оси.
 * Такая геометрия детерминирована в мировых координатах и не зависит от
 * камеры вообще — ничего не крутится и не мигает при орбите вокруг игрока.
 *
 * Камера-фейсинг (billboard) оставлен только там, где он и должен быть —
 * для точечных «свечений» (glowQuad/bodyGlow), которые физически обязаны
 * всегда смотреть на камеру, как спрайт.
 *
 * ВАЖНО (v3, 12.08.2026): в vertex() передаётся матрица поворота камеры из
 * event.getPoseStack() (та же формула XP(xRot)·YP(yRot+180), что и в
 * ванильном рендере мира), а координаты — камера-относительные. Поворот
 * запекается в вершины на CPU, поэтому результат НЕ зависит от ambient
 * ModelViewMat шейдера — он надёжен и под Embeddium/Oculus/ImmediatelyFast
 * (см. урок в AGENTS.md). Стадия — AFTER_PARTICLES, где ambient ModelViewMat
 * равен identity (паттерн маркеров/гост-блоков pwp-warfare).
 *
 * Координаты фигур — ОТНОСИТЕЛЬНО камеры (уже вычтена позиция камеры), это
 * стандартный паттерн для избежания float-погрешностей вдали от мирового
 * центра и само по себе никак не связано с описанной выше проблемой.
 */
public final class AuraGeom {

    public static final double TAU = Math.PI * 2.0;

    private AuraGeom() {}

    // ====== Цвета ======

    public static int argb(float a, float r, float g, float b) {
        int ia = (int) (Mth.clamp(a, 0f, 1f) * 255f);
        int ir = (int) (Mth.clamp(r, 0f, 1f) * 255f);
        int ig = (int) (Mth.clamp(g, 0f, 1f) * 255f);
        int ib = (int) (Mth.clamp(b, 0f, 1f) * 255f);
        return (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    public static int color(int argb, float alphaMul) {
        int a = (int) (((argb >>> 24) & 0xFF) * Mth.clamp(alphaMul, 0f, 1f));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    // ====== Утилиты сглаживания ======

    /** Кубический smoothstep — вместо линейного fade даёт мягкий, а не рваный край. */
    public static double smoothstep(double edge0, double edge1, double x) {
        double t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** easeInOutSine — плавный заход/выход (дыхание, фазовые циклы). */
    public static double easeInOutSine(double t) {
        return 0.5 - 0.5 * Math.cos(Math.PI * Mth.clamp(t, 0.0, 1.0));
    }

    /** easeOutCubic — быстрое начало, мягкое завершение (расширяющиеся пульсы). */
    public static double easeOutCubic(double t) {
        double x = 1.0 - Mth.clamp(t, 0.0, 1.0);
        return 1.0 - x * x * x;
    }

    /**
     * Угловая скорость (рад/тик) для периода в СЕКУНДАХ: время аур — тики
     * (gameTime + partialTick), поэтому полный оборот = secondsPerRev * 20 тиков.
     */
    public static double omega(double secondsPerRev) {
        return TAU / (secondsPerRev * 20.0);
    }

    /** Детерминированный 0..1-хэш по целому (соль фаз/джиттера — без случайности на кадр). */
    public static double hash01(long k) {
        long h = k * 0x9E3779B97F4A7C15L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * (1.0 / 9007199254740992.0);
    }

    /** Доля фазы, приведённая в [0, 1) (обёртка без отрицательных значений). */
    public static double wrapFrac(double x) {
        double r = x % 1.0;
        return r < 0 ? r + 1.0 : r;
    }

    /** Угловое расстояние (0..PI) между двумя углами. */
    public static double angleDiff(double a, double b) {
        double d = (a - b) % TAU;
        if (d > Math.PI) d -= TAU;
        if (d < -Math.PI) d += TAU;
        return Math.abs(d);
    }

    /**
     * Поворот точки p вокруг center и мировой оси axis (Rodrigues). Для
     * направлений (векторов) передавать center = Vec3.ZERO.
     */
    public static Vec3 rotateAround(Vec3 p, Vec3 center, Vec3 axis, double angle) {
        Vec3 v = p.subtract(center);
        Vec3 k = axis.normalize();
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        Vec3 cross = k.cross(v);
        Vec3 kdv = k.scale(k.dot(v));
        return center.add(v.scale(c)).add(cross.scale(s)).add(kdv.scale(1.0 - c));
    }

    // ====== Базовые примитивы ======

    public static void quad(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 b0, Vec3 c, Vec3 d, int argb) {
        int a2 = (argb >>> 24) & 0xFF, r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, bl = argb & 0xFF;
        b.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, a2).endVertex();
        b.vertex(m, (float) b0.x, (float) b0.y, (float) b0.z).color(r, g, bl, a2).endVertex();
        b.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, a2).endVertex();
        b.vertex(m, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, a2).endVertex();
    }

    /** Билборд-квад (камера-фейсинг) — корректно и намеренно только для точечных свечений/спрайтов. */
    public static void glowQuad(VertexConsumer b, Matrix4f m, Vec3 center, Vec3 right, Vec3 up, double size, int argb) {
        Vec3 r = right.scale(size * 0.5);
        Vec3 u = up.scale(size * 0.5);
        quad(b, m, center.subtract(r).subtract(u), center.add(r).subtract(u),
                center.add(r).add(u), center.subtract(r).add(u), argb);
    }

    /**
     * Мировая (НЕ камера-фейсинг) лента между точками a и b с явной нормалью
     * толщины. Нормаль — вектор в мировых координатах, поэтому лента ведёт
     * себя одинаково независимо от угла обзора.
     */
    public static void ribbonWorld(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 b0, Vec3 normal, double width, int argb) {
        Vec3 half = normal.normalize().scale(width * 0.5);
        quad(b, m, a.subtract(half), b0.subtract(half), b0.add(half), a.add(half), argb);
    }

    public static Vec3 polar(Vec3 center, double radius, double y, double angle) {
        return center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    /**
     * Короткий «штрих»-спрайт вдоль мировой оси axis (длина length, толщина
     * size по side). Используется для искр/трейлов: axis — направление
     * вытягивания, side — перпендикулярная ось толщины (камера-up, мировой Y
     * или камера-right в зависимости от ориентации штриха).
     */
    public static void dash(VertexConsumer b, Matrix4f m, Vec3 pos, Vec3 axis, Vec3 side,
                            double length, double size, int argb) {
        Vec3 h = axis.normalize().scale(length * 0.5);
        Vec3 s = side.normalize().scale(size * 0.5);
        quad(b, m, pos.subtract(h).subtract(s), pos.add(h).subtract(s),
                pos.add(h).add(s), pos.subtract(h).add(s), argb);
    }

    // ====== Наклонные орбитальные кольца (произвольная плоскость, всё ещё мировая) ======

    /**
     * Ортонормированный базис плоскости, перпендикулярной normal. Полностью
     * детерминирован по нормали (мировая ось), камера не участвует.
     */
    private static Vec3[] planeBasis(Vec3 normal) {
        Vec3 n = normal.normalize();
        Vec3 refv = Math.abs(n.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = refv.cross(n).normalize();
        Vec3 v = n.cross(u);
        return new Vec3[]{u, v};
    }

    /** Точка на кольце в плоскости, перпендикулярной normal, со смещением вдоль normal. */
    public static Vec3 orbitPoint(Vec3 center, Vec3 normal, double radius, double alongNormal, double angle) {
        Vec3[] basis = planeBasis(normal);
        return center.add(basis[0].scale(Math.cos(angle) * radius))
                .add(basis[1].scale(Math.sin(angle) * radius))
                .add(normal.normalize().scale(alongNormal));
    }

    /**
     * Дуга-полоса в ПРОИЗВОЛЬНОЙ фиксированной в мире плоскости (задаётся
     * normal). Обобщение плоского кольца на наклонные плоскости — позволяет
     * рисовать "гироскоп"/орбитальные кольца под углом друг к другу.
     * Полностью независима от камеры, как и flatArc.
     */
    public static void orbitArc(VertexConsumer b, Matrix4f m, Vec3 center, Vec3 normal, double radius,
                                 double alongNormal, double rot, double arcFraction, int segments,
                                 double width, int argb, boolean fadeEnds) {
        double innerR = radius - width * 0.5;
        double outerR = radius + width * 0.5;
        double span = TAU * arcFraction;
        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / segments;
            double t1 = (double) (i + 1) / segments;
            double a0 = rot + span * t0;
            double a1 = rot + span * t1;
            float alpha = 1f;
            if (fadeEnds) {
                double mid = (t0 + t1) * 0.5;
                alpha = (float) smoothstep(0.0, 0.15, Math.min(mid, 1.0 - mid));
            }
            quad(b, m,
                    orbitPoint(center, normal, innerR, alongNormal, a0),
                    orbitPoint(center, normal, outerR, alongNormal, a0),
                    orbitPoint(center, normal, outerR, alongNormal, a1),
                    orbitPoint(center, normal, innerR, alongNormal, a1),
                    color(argb, alpha));
        }
    }

    private static final Vec3 UP_AXIS = new Vec3(0, 1, 0);

    /**
     * Плоская дуга-полоса в горизонтальной плоскости y (частный случай
     * orbitArc с normal = мировой Y). Ширина по радиусу, мягкий fade на концах.
     */
    public static void flatArc(VertexConsumer b, Matrix4f m, Vec3 center, double radius, double y,
                                double rot, double arcFraction, int segments, double width,
                                int argb, boolean fadeEnds) {
        orbitArc(b, m, center, UP_AXIS, radius, y, rot, arcFraction, segments, width, argb, fadeEnds);
    }

    /** Сегментированное кольцо с зазорами («зубцы сканера») — набор flatArc-дуг. */
    public static void segmentedFlatRing(VertexConsumer b, Matrix4f m, Vec3 center, double radius, double y,
                                          double rot, int count, double arcSpan, double width, int argb) {
        int segPerTooth = 12; // достаточно для гладкой дуги на каждом зубце
        for (int s = 0; s < count; s++) {
            flatArc(b, m, center, radius, y, rot + s * TAU / count, arcSpan, segPerTooth, width, argb, true);
        }
    }

    /**
     * Кольцо-«радар» (v4): count зубцов в ПРОИЗВОЛЬНОЙ плоскости (normal),
     * яркость зубца плавно спадает по угловому расстоянию от rot — вращаясь,
     * кольцо читается как крутящийся радар (уровни яркости 1.0/0.8/0.65...).
     */
    public static void radarRingEx(VertexConsumer b, Matrix4f m, Vec3 center, Vec3 normal, double radius,
                                   double alongNormal, double rot, int count, double arcSpan, double width,
                                   double falloff, int argb) {
        int segPerTooth = 12;
        for (int s = 0; s < count; s++) {
            double head = s * TAU / count;
            double diff = angleDiff(head, rot);
            double bright = (1.0 - falloff) + falloff * (0.5 + 0.5 * Math.cos(diff));
            orbitArc(b, m, center, normal, radius, alongNormal, head, arcSpan, segPerTooth,
                    width, color(argb, (float) bright), true);
        }
    }

    /** Кольцо-«радар» в горизонтальной плоскости (частный случай с normal = мировой Y). */
    public static void radarRing(VertexConsumer b, Matrix4f m, Vec3 center, double radius, double y,
                                 double rot, int count, double arcSpan, double width,
                                 double falloff, int argb) {
        radarRingEx(b, m, center, UP_AXIS, radius, y, rot, count, arcSpan, width, falloff, argb);
    }

    /** Полное плоское кольцо-плитка (innerR..outerR). */
    public static void ringTile(VertexConsumer b, Matrix4f m, Vec3 center, double innerR, double outerR,
                                 double y, double rot, int segments, int argb) {
        for (int i = 0; i < segments; i++) {
            double a0 = TAU * i / segments + rot;
            double a1 = TAU * (i + 1) / segments + rot;
            quad(b, m,
                    polar(center, innerR, y, a0), polar(center, outerR, y, a0),
                    polar(center, outerR, y, a1), polar(center, innerR, y, a1), argb);
        }
    }

    /**
     * Спираль-лента вокруг вертикальной оси: нормаль толщины направлена
     * радиально наружу от оси (в отличие от старой камера-фейсинг версии),
     * поэтому спираль не плющится и не скручивается при повороте камеры.
     */
    public static void helix(VertexConsumer b, Matrix4f m, Vec3 center, double radius, double yBase,
                              double height, double turns, double rot, int segments, double width, int argb) {
        helixEx(b, m, center, radius, yBase, height, turns, rot, segments, width, argb, null, null, 0.0, 0.0, 0.0);
    }

    /**
     * Спираль с двумя доработками (v4, полёт/вспышки):
     *  - tiltAxis/tilt — наклон оси спирали (Rodrigues вокруг горизонтальной
     *    оси, например перпендикулярной направлению полёта);
     *  - tiltPivot (v4.2) — точка в МИРЕ, вокруг которой ось наклоняется.
     *    По умолчанию (null) — center (ноги); для полёта передавать точку на
     *    высоте груди, иначе наклонённая ось проходит через модель игрока;
     *  - proximityRot/flashAmp — яркость ленты растёт, когда её виток сходится
     *    с ДРУГОЙ спиралью. Спирали намотаны в одну сторону (turns одной
     *    величины) — угловое расстояние между ними вдоль высоты ПОСТОЯННО,
     *    поэтому сближение глобальное: dc = angleDiff(rot, proximityRot) без
     *    зависимости от t (бегущая по ленте полоса давала «мерцание»).
     *    Окно ~0.6 рад при относительной скорости ~2.15 рад/с даёт короткий
     *    пик ~0.15 с — «вспышка пересечения» GOLD. flashAmp <= 0 — выключено.
     */
    public static void helixEx(VertexConsumer b, Matrix4f m, Vec3 center, double radius, double yBase,
                               double height, double turns, double rot, int segments, double width, int argb,
                               Vec3 tiltAxis, Vec3 tiltPivot, double tilt, double proximityRot, double flashAmp) {
        boolean tilted = tiltAxis != null && Math.abs(tilt) > 1e-4;
        Vec3 pivot = tiltPivot != null ? tiltPivot : center;
        double boost = 1.0;
        if (flashAmp > 0.0) {
            double dc = angleDiff(rot, proximityRot);
            boost = 1.0 + flashAmp * (1.0 - smoothstep(0.25, 0.9, dc));
        }
        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / segments;
            double t1 = (double) (i + 1) / segments;
            double a0 = rot + t0 * turns * TAU;
            double a1 = rot + t1 * turns * TAU;
            Vec3 a = polar(center, radius, yBase + t0 * height, a0);
            Vec3 b0 = polar(center, radius, yBase + t1 * height, a1);
            if (tilted) {
                a = rotateAround(a, pivot, tiltAxis, tilt);
                b0 = rotateAround(b0, pivot, tiltAxis, tilt);
            }
            double midA = (a0 + a1) * 0.5;
            Vec3 outward = new Vec3(Math.cos(midA), 0.0, Math.sin(midA));
            if (tilted) outward = rotateAround(outward, Vec3.ZERO, tiltAxis, tilt);
            double mid = (t0 + t1) * 0.5;
            double fall = smoothstep(0.0, 0.2, Math.min(mid, 1.0 - mid));
            ribbonWorld(b, m, a, b0, outward, width, color(argb, (float) (fall * boost)));
        }
    }

    /** Точка на спирали (доля t 0..1, rot — текущий угол витка). */
    public static Vec3 helixPoint(Vec3 center, double radius, double yBase, double height,
                                   double turns, double rot, double t) {
        double a = rot + t * turns * TAU;
        return polar(center, radius, yBase + t * height, a);
    }

    /**
     * «Монетки» GOLD: точки-билборды, бегущие вдоль спирали (цикл cycleTicks
     * в тиках), каждая со своим оффсетом; dragX/dragZ — небольшой уход назад
     * при беге (тянутся за игроком).
     * v4.2: tiltAxis/tiltPivot/tilt — те же параметры наклона, что в helixEx,
     * точки вращаются ТЕМ ЖЕ поворотом, что и лента (в полёте монетки лежат
     * на наклонённой спирали, а не «выходят» из неё).
     */
    public static void helixDots(VertexConsumer b, Matrix4f m, Vec3 center, double radius, double yBase,
                                 double height, double turns, double rot, Vec3 tiltAxis, Vec3 tiltPivot,
                                 double tilt, int count, double cycleTicks,
                                 float time, double offset, double dragX, double dragZ,
                                 Vec3 right, Vec3 up, double size, int argb) {
        boolean tilted = tiltAxis != null && Math.abs(tilt) > 1e-4;
        Vec3 pivot = tiltPivot != null ? tiltPivot : center;
        for (int k = 0; k < count; k++) {
            double ph = (time / cycleTicks + (double) k / count + offset) % 1.0;
            double env = Math.sin(ph * Math.PI);
            Vec3 p = helixPoint(center, radius, yBase, height, turns, rot, ph);
            if (tilted) p = rotateAround(p, pivot, tiltAxis, tilt);
            p = p.add(dragX * ph, 0.0, dragZ * ph);
            glowQuad(b, m, p, right, up, size, color(argb, (float) env));
        }
    }

    /** Световые узлы на орбитальном кольце: едут вместе с кольцом, своя мягкая пульсация. */
    public static void orbitDots(VertexConsumer b, Matrix4f m, Vec3 center, Vec3 normal, double radius,
                                 double alongNormal, double rot, int count, float time, double offset,
                                 Vec3 right, Vec3 up, double size, int argb) {
        for (int k = 0; k < count; k++) {
            double a = rot + (double) k / count * TAU;
            double pulse = 0.45 + 0.55 * (0.5 + 0.5 * Math.sin(time / 20.0 * Math.PI * 2.0 / 3.0 + offset * TAU + k * 2.1));
            Vec3 p = orbitPoint(center, normal, radius, alongNormal, a);
            glowQuad(b, m, p, right, up, size, color(argb, (float) pulse));
        }
    }

    /** Расширяющееся пульс-кольцо — теперь плоская мировая дуга. */
    public static void pulseRing(VertexConsumer b, Matrix4f m, Vec3 center, double y, double r0, double r1,
                                  double progress, int segments, double width, int argb) {
        double eased = smoothstep(0.0, 1.0, progress);
        double radius = Mth.lerp(eased, r0, r1);
        flatArc(b, m, center, radius, y, 0.0, 1.0, segments, width, color(argb, (float) (1.0 - progress)), false);
    }

    /** Мягкий ореол вокруг тела: три вложенных билборда (корректно камера-фейсинг). */
    public static void bodyGlow(VertexConsumer b, Matrix4f m, Vec3 pos, Vec3 right, Vec3 up, int argb) {
        Vec3 c = pos.add(0.0, 1.0, 0.0);
        glowQuad(b, m, c, right, up, 1.15, color(argb, 0.22f));
        glowQuad(b, m, c, right, up, 0.70, color(argb, 0.45f));
        glowQuad(b, m, c, right, up, 0.34, argb);
    }

    /**
     * Переиспользуемая "пыль у ног/вокруг тела": частицы-билборды по
     * детерминированной синусоидальной траектории (без случайности и без
     * аккумуляторов — чистая функция времени). rise > 0 — пыль поднимается
     * (тёплые тиры), rise < 0 — оседает вниз (иней/платина).
     *
     * v4: jitter — разброс скоростей отдельных пылинок (±jitter, детерминированный
     * хэш по индексу), offset — общий сдвиг фазы (стаггер по игроку),
     * dragX/dragZ — уход назад по мере подъёма («тянутся за игроком» при беге).
     */
    public static void dustMotes(VertexConsumer b, Matrix4f m, Vec3 base, Vec3 right, Vec3 up,
                                  float time, int count, double radius, double rise,
                                  double speed, double jitter, double offset,
                                  double dragX, double dragZ, double size, int argb) {
        for (int k = 0; k < count; k++) {
            double jk = hash01(k + 7) * 2.0 - 1.0;
            double sp = speed * (1.0 + jitter * jk);
            double phase = ((time * sp) / TAU + (double) k / count + offset) % 1.0;
            double a = k * TAU / count + time * 0.01;
            double r = radius * (0.75 + 0.25 * Math.cos(a * 3.0 + k));
            Vec3 p = base.add(Math.cos(a) * r + dragX * phase, phase * rise, Math.sin(a) * r + dragZ * phase);
            double env = Math.sin(phase * Math.PI); // 0 в начале/конце пути, пик в середине
            glowQuad(b, m, p, right, up, size, color(argb, (float) env));
        }
    }
}
