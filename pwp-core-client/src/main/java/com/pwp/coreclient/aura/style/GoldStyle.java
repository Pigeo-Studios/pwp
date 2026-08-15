package com.pwp.coreclient.aura.style;

import com.pwp.coreclient.aura.AuraGeom;
import com.pwp.coreclient.aura.FxState;
import com.pwp.coreclient.donor.DonorLevel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * GOLD — «Золотой поток»: богатый, живой, динамичный.
 *
 *  - две неполные 3D-спирали-ленты вокруг тела (высота 0.2 -> 1.7, radius
 *    ~0.55): ~5 с и ~7 с на оборот, направления противоположные, разные фазы —
 *    ленты не исчезают одновременно;
 *  - вспышки пересечения: сегменты обеих лент ярче, когда витки сходятся
 *    (угловое расстояние -> 0), плюс яркие точки в местах пересечения с
 *    короткой пульсацией;
 *  - «монетки»: 10 ярких точек вдоль спиралей, цикл ~2.2 с, свои фазы;
 *  - тёплое кольцо у ног, вращается противоположно основной спирали;
 *  - бег (0.3–0.5 с): ленты/монетки слегка тянутся назад;
 *  - полёт (0.8 с): ось спиралей наклоняется, растягивается, ускоряется,
 *    яркое ядро остаётся перед игроком, позади золотой trail.
 */
public final class GoldStyle {

    private GoldStyle() {}

    private static final double RIBBON_RADIUS = 0.55;
    private static final double RIBBON_BASE = 0.2;
    private static final double RIBBON_HEIGHT = 1.5; // 0.2 -> 1.7
    private static final double RIBBON_TURNS = 1.7;

    /** Точка наклона оси спиралей (v4.2): середина спирали, а не ноги — наклонённые
     *  витки остаются СНАРУЖИ модели игрока (раньше ось наклонялась вокруг ног и
     *  проходила сквозь грудь/голову). */
    private static final double PIVOT_Y = RIBBON_BASE + RIBBON_HEIGHT * 0.5;

    public static void draw(VertexConsumer b, Matrix4f m, Vec3 pos, FxState st, float time, float i,
                            Vec3 right, Vec3 up, DonorLevel lvl) {
        double fly = st.flyBlend();
        double move = st.moveBlend();

        // Полёт: спираль наклоняется, растягивается и ускоряется (тайминг flyBlend ~0.8 с)
        double radius = RIBBON_RADIUS * (1.0 + 0.15 * fly);
        double height = RIBBON_HEIGHT * (1.0 + 0.25 * fly);
        double spdMul = 1.0 + 0.4 * fly;

        // Ось наклона и направление полёта — ТОЛЬКО из сглаженной оси FxState
        // (keep-last, тау 8 тиков): при развороте ось вращается непрерывно, при
        // зависании сохраняет последнее направление — без щелчков на 180° и без
        // скачка ядра/trail на «север» (v4.2).
        double ax = st.tiltAxisX();
        double az = st.tiltAxisZ();
        double al = Math.sqrt(ax * ax + az * az);
        Vec3 tiltAxis = null;
        Vec3 fwd = null;
        double tilt = 0.0;
        if (al > 1e-4) {
            fwd = new Vec3(az / al, 0.0, -ax / al); // ось ⊥ скорости -> направление движения
            if (fly > 0.01) {
                tiltAxis = new Vec3(ax / al, 0.0, az / al);
                tilt = 0.35 * fly;
            }
        }
        Vec3 pivot = pos.add(0.0, PIVOT_Y, 0.0);

        double p1 = st.hash(10) * Math.PI * 2.0;
        double p2 = st.hash(11) * Math.PI * 2.0;
        double rot1 = time * AuraGeom.omega(5.0) * spdMul + p1;
        double rot2 = -time * AuraGeom.omega(7.0) * spdMul + p2;

        // Две неполные спирали-ленты, встречные (5 с / 7 с на оборот), с proximity-вспышками
        AuraGeom.helixEx(b, m, pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot1, 40, 0.055,
                col(lvl, 0.65f * i), tiltAxis, pivot, tilt, rot2, 0.8);
        AuraGeom.helixEx(b, m, pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot2, 40, 0.055,
                col(lvl, 0.55f * i), tiltAxis, pivot, tilt, rot1, 0.6);

        // Вспышка сближения витков: ленты намотаны в одну сторону, сближение глобальное
        // (угловое расстояние rot1-rot2 -> 0). Окно 0.6 рад при относительной скорости
        // ~2.15 рад/с даёт короткий пик ~0.15 с (дизайн), затем плавный fade — без мерцания.
        double delta = AuraGeom.angleDiff(rot1, rot2);
        double env = 1.0 - AuraGeom.smoothstep(0.15, 0.6, delta);
        if (env > 0.01) {
            Vec3 fp = AuraGeom.helixPoint(pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot1, 0.5);
            if (tiltAxis != null) fp = AuraGeom.rotateAround(fp, pivot, tiltAxis, tilt);
            AuraGeom.glowQuad(b, m, fp, right, up, 0.2, col(lvl, (float) (0.9 * i * env)));
        }

        // «Монетки»: по 5 на каждую спираль, спокойный облёт тела ~4.4 с (позиционный
        // цикл вдоль витка), при беге слегка уходят назад; в полёте наклоняются вместе с лентами
        double cycle = 4.4 * 20.0;
        double cdragX = -st.vx() * 0.6 * move;
        double cdragZ = -st.vz() * 0.6 * move;
        AuraGeom.helixDots(b, m, pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot1,
                tiltAxis, pivot, tilt, 5, cycle, time, st.hash(13), cdragX, cdragZ,
                right, up, 0.12, col(lvl, 0.8f * i));
        AuraGeom.helixDots(b, m, pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot2,
                tiltAxis, pivot, tilt, 5, cycle, time, st.hash(14), cdragX, cdragZ,
                right, up, 0.12, col(lvl, 0.8f * i));

        // Тёплое кольцо у ног — вращается противоположно основной спирали
        AuraGeom.flatArc(b, m, pos, 0.44, 0.04, (float) (-time * AuraGeom.omega(40.0)), 1.0, 40, 0.03,
                col(lvl, 0.35f * i), false);

        // Пыль: цикл 2.4 с; в полёте гаснет (её место занимает ядро + trail)
        AuraGeom.dustMotes(b, m, pos, right, up, time, 10, 0.42, 1.6,
                AuraGeom.TAU / (2.4 * 20.0), 0.2, st.hash(15), cdragX * 0.7, cdragZ * 0.7, 0.08,
                col(lvl, (float) (0.6 * i * (1.0 - 0.55 * fly))));

        AuraGeom.bodyGlow(b, m, pos, right, up, col(lvl, (float) (0.07 * i * (1.0 - 0.4 * fly))));

        // Полёт: яркое ядро перед игроком + золотой trail позади (по keep-last
        // направлению — плавный изгиб при повороте, без скачков при остановке)
        if (fly > 0.01) {
            Vec3 fw = fwd != null ? fwd : new Vec3(0.0, 0.0, 1.0);
            Vec3 core = pos.add(fw.x * 0.55, 1.0, fw.z * 0.55);
            AuraGeom.glowQuad(b, m, core, right, up, 0.22, col(lvl, (float) (0.85 * i * fly)));
            Vec3 back = new Vec3(-fw.x, 0.0, -fw.z);
            for (int k = 0; k < 3; k++) {
                double h = 0.4 + k * 0.3;
                double len = (0.2 + 0.5 * fly) * (1.0 - k * 0.15);
                Vec3 p = pos.add(0.0, h, 0.0).add(back.scale(0.12 + k * 0.2));
                AuraGeom.dash(b, m, p, back, up, len, 0.05,
                        col(lvl, (float) (0.6 * i * fly * (1.0 - k * 0.2))));
            }
        }
    }

    private static int col(DonorLevel lvl, float alpha) {
        float[] rgb = lvl.rgb();
        return AuraGeom.argb(alpha, rgb[0], rgb[1], rgb[2]);
    }
}
