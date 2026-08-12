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

    public static void draw(VertexConsumer b, Matrix4f m, Vec3 pos, FxState st, float time, float i,
                            Vec3 right, Vec3 up, DonorLevel lvl) {
        double fly = st.flyBlend();
        double move = st.moveBlend();
        double sec = time / 20.0;

        // Полёт: спираль наклоняется, растягивается и ускоряется (тайминг flyBlend ~0.8 с)
        double radius = RIBBON_RADIUS * (1.0 + 0.15 * fly);
        double height = RIBBON_HEIGHT * (1.0 + 0.25 * fly);
        double spdMul = 1.0 + 0.4 * fly;
        double spd = st.speed();
        Vec3 tiltAxis = null;
        double tilt = 0.0;
        if (fly > 0.01 && spd > 1e-3) {
            Vec3 fwd = new Vec3(st.vx() / spd, 0.0, st.vz() / spd);
            tiltAxis = new Vec3(-fwd.z, 0.0, fwd.x); // ось вбок — спираль наклоняется по направлению полёта
            tilt = 0.35 * fly;
        }

        double p1 = st.hash(10) * Math.PI * 2.0;
        double p2 = st.hash(11) * Math.PI * 2.0;
        double rot1 = time * AuraGeom.omega(5.0) * spdMul + p1;
        double rot2 = -time * AuraGeom.omega(7.0) * spdMul + p2;

        // Две неполные спирали-ленты, встречные (5 с / 7 с на оборот), с proximity-вспышками
        AuraGeom.helixEx(b, m, pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot1, 40, 0.055,
                col(lvl, 0.65f * i), tiltAxis, tilt, rot2, 1.2);
        AuraGeom.helixEx(b, m, pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot2, 40, 0.055,
                col(lvl, 0.55f * i), tiltAxis, tilt, rot1, 1.0);

        // Точки в местах сближения витков — маленькие вспышки (короткая пульсация)
        double tc0 = AuraGeom.wrapFrac((rot2 - rot1) / (RIBBON_TURNS * 2.0 * AuraGeom.TAU));
        for (int k = 0; k < 4; k++) {
            double tc = AuraGeom.wrapFrac(tc0 + (double) k / (RIBBON_TURNS * 2.0));
            Vec3 cp = AuraGeom.helixPoint(pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot1, tc);
            if (tiltAxis != null) cp = AuraGeom.rotateAround(cp, pos, tiltAxis, tilt);
            double pulse = Math.pow(0.5 + 0.5 * Math.sin(sec * Math.PI * 2.0 / 0.75 + st.hash(12 + k) * Math.PI * 2.0), 3.0);
            AuraGeom.glowQuad(b, m, cp, right, up, 0.16, col(lvl, (float) (0.9 * i * pulse)));
        }

        // «Монетки»: по 5 на каждую спираль, цикл ~2.2 с, при беге уходят назад
        double cycle = 2.2 * 20.0;
        double cdragX = -st.vx() * 2.0 * move;
        double cdragZ = -st.vz() * 2.0 * move;
        AuraGeom.helixDots(b, m, pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot1, 5, cycle,
                time, st.hash(13), cdragX, cdragZ, right, up, 0.12, col(lvl, 0.8f * i));
        AuraGeom.helixDots(b, m, pos, radius, RIBBON_BASE, height, RIBBON_TURNS, rot2, 5, cycle,
                time, st.hash(14), cdragX, cdragZ, right, up, 0.12, col(lvl, 0.8f * i));

        // Тёплое кольцо у ног — вращается противоположно основной спирали
        AuraGeom.flatArc(b, m, pos, 0.44, 0.04, (float) (-time * AuraGeom.omega(40.0)), 1.0, 40, 0.03,
                col(lvl, 0.35f * i), false);

        // Пыль: чаще/ярче, чем у SILVER
        AuraGeom.dustMotes(b, m, pos, right, up, time, 10, 0.42, 1.6,
                AuraGeom.TAU / (1.8 * 20.0), 0.2, st.hash(15), cdragX * 0.6, cdragZ * 0.6, 0.08,
                col(lvl, 0.6f * i));

        AuraGeom.bodyGlow(b, m, pos, right, up, col(lvl, (float) (0.07 * i * (1.0 - 0.4 * fly))));

        // Полёт: яркое ядро перед игроком + золотой trail позади
        if (fly > 0.01) {
            Vec3 fwd = spd > 1e-3 ? new Vec3(st.vx() / spd, 0.0, st.vz() / spd) : new Vec3(0.0, 0.0, 1.0);
            Vec3 core = pos.add(fwd.x * 0.55, 1.0, fwd.z * 0.55);
            AuraGeom.glowQuad(b, m, core, right, up, 0.22, col(lvl, (float) (0.85 * i * fly)));
            Vec3 back = new Vec3(-fwd.x, 0.0, -fwd.z);
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
