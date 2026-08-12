package com.pwp.coreclient.aura.style;

import com.pwp.coreclient.aura.AuraGeom;
import com.pwp.coreclient.aura.FxState;
import com.pwp.coreclient.donor.DonorLevel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * MODERATOR — «Пост дозора»: технологичный служебный статус.
 *
 *  - 4 световых столба вокруг игрока (radius 0.62, y 0.05 -> 1.5) на углах
 *    0/90/180/270, вращаются вместе: полный оборот ~9 с;
 *  - огоньки на вершинах: billboard проходит низ -> верх -> fade -> низ,
 *    цикл ~2.5 с, фазы 0 / 0.625 / 1.25 / 1.875 — непрерывная волна;
 *  - последовательный scan каждые 6 с: активация столбов 1 -> 2 -> 3 -> 4
 *    с задержкой ~0.15 с;
 *  - тонкое голубое кольцо у ног, медленное вращение;
 *  - бег: столбы слегка наклоняются назад (0.3–0.5 с);
 *  - полёт (0.9 с): столбы вытягиваются, наклоняются и замыкаются верхним
 *    кольцом — голубой каркас; при посадке — обратно.
 */
public final class ModeratorStyle {

    private ModeratorStyle() {}

    private static final double PILLAR_RADIUS = 0.62;
    private static final double PILLAR_BASE = 0.05;
    private static final double PILLAR_TOP = 1.5;

    public static void draw(VertexConsumer b, Matrix4f m, Vec3 pos, FxState st, float time, float i,
                            Vec3 right, Vec3 up, DonorLevel lvl) {
        double fly = st.flyBlend();
        double move = st.moveBlend();
        double sec = time / 20.0;

        AuraGeom.bodyGlow(b, m, pos, right, up, col(lvl, 0.08f * i));

        // Тонкое голубое кольцо у ног, медленное вращение (~40 с)
        AuraGeom.flatArc(b, m, pos, 0.5, 0.04, (float) (time * AuraGeom.omega(40.0)), 1.0, 40, 0.025,
                col(lvl, 0.4f * i), false);

        // Полёт: столбы вытягиваются (y -> 2.0) и наклоняются; бег — лёгкий наклон назад
        double topY = PILLAR_TOP + 0.5 * fly;
        double spd = st.speed();
        double backX = 0.0;
        double backZ = 0.0;
        if (spd > 1e-3) {
            backX = -st.vx() / spd;
            backZ = -st.vz() / spd;
        }
        double shift = 0.25 * move + 0.55 * fly;

        double rot = time * AuraGeom.omega(9.0);
        for (int k = 0; k < 4; k++) {
            double a = rot + k * AuraGeom.TAU / 4.0;
            Vec3 outward = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 topP = AuraGeom.polar(pos, PILLAR_RADIUS, topY, a)
                    .add(backX * shift, 0.0, backZ * shift);

            // Последовательный scan каждые 6 с: активация 1->2->3->4, задержка ~0.15 с
            double t0 = k * 0.15;
            double env = (sec % 6.0) >= t0 && (sec % 6.0) < t0 + 0.5
                    ? Math.sin(((sec % 6.0) - t0) / 0.5 * Math.PI) : 0.0;
            double boost = 1.0 + 0.9 * env;

            AuraGeom.ribbonWorld(b, m, AuraGeom.polar(pos, PILLAR_RADIUS, PILLAR_BASE, a),
                    topP, outward, 0.05, col(lvl, (float) (0.45 * i * boost)));

            // Огонёк: низ -> верх -> fade -> низ за 2.5 с, фазы k*0.625 — волна
            double orbPh = AuraGeom.wrapFrac(sec / 2.5 + k * 0.625);
            double orbY = PILLAR_BASE + orbPh * (topY - PILLAR_BASE);
            Vec3 orbP = AuraGeom.polar(pos, PILLAR_RADIUS, orbY, a)
                    .add(backX * shift * orbPh, 0.0, backZ * shift * orbPh);
            float orbA = (float) Math.sin(orbPh * Math.PI);
            AuraGeom.glowQuad(b, m, orbP, right, up, 0.12,
                    col(lvl, 0.7f * i * orbA * (float) boost));
        }

        // Полёт: «голубой каркас» — соединительное кольцо наверху
        if (fly > 0.02) {
            AuraGeom.flatArc(b, m, pos, PILLAR_RADIUS, topY, rot, 1.0, 40, 0.03,
                    col(lvl, (float) (0.35 * i * fly)), false);
        }
    }

    private static int col(DonorLevel lvl, float alpha) {
        float[] rgb = lvl.rgb();
        return AuraGeom.argb(alpha, rgb[0], rgb[1], rgb[2]);
    }
}
