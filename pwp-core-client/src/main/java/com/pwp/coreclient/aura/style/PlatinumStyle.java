package com.pwp.coreclient.aura.style;

import com.pwp.coreclient.aura.AuraGeom;
import com.pwp.coreclient.aura.FxState;
import com.pwp.coreclient.donor.DonorLevel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * PLATINUM — «Ледяной гироскоп»: самый премиальный и сложный донат-эффект.
 *
 *  - три орбитальных кольца вокруг груди (Y ~1.1, R ~0.55): горизонтальное и
 *    два наклонных (нормали (1,1,0), (0,1,1)); периоды 4.5 / 6.0 / 8.0 с,
 *    направления разные — рисунок постоянно меняется;
 *  - световые узлы: 2–3 ярких узла на каждой орбите, едут вместе с кольцом,
 *    своя мягкая пульсация;
 *  - синхронный flash ~раз в 12 с: орбиты «сходятся» — яркость растёт, цвет
 *    лерпится к ice-white, fade ~0.6 с, без резкого переключения;
 *  - отдельное ледяное кольцо у ног с пульсом радиуса 0.5 -> 0.7 -> 0.5;
 *  - иней: 9 частиц, в отличие от Silver/Gold оседают вниз (rise = -0.9),
 *    цикл ~1.6 с, каждая со своим оффсетом 0.18–0.25 с;
 *  - полёт (1.2 с): орбиты расширяются, наклоняются и ускоряются — широкий
 *    ледяной гироскоп; возврат ~1.0 с (тайминг flyBlend из конфига).
 */
public final class PlatinumStyle {

    private PlatinumStyle() {}

    private static final Vec3 AXIS_FLAT = new Vec3(0, 1, 0);
    private static final Vec3 AXIS_TILT_A = new Vec3(1, 1, 0);
    private static final Vec3 AXIS_TILT_B = new Vec3(0, 1, 1);
    private static final double ORBIT_Y = 1.1;
    private static final double ORBIT_RADIUS = 0.55;

    public static void draw(VertexConsumer b, Matrix4f m, Vec3 pos, FxState st, float time, float i,
                            Vec3 right, Vec3 up, DonorLevel lvl) {
        double fly = st.flyBlend();
        double sec = time / 20.0;

        // Синхронный flash каждые 12 с: подъём ~1.3 с, fade ~0.6 с
        double cyc = sec % 12.0;
        double flash;
        if (cyc < 1.3) {
            flash = AuraGeom.easeInOutSine(cyc / 1.3);
        } else if (cyc < 1.9) {
            flash = 1.0 - AuraGeom.easeInOutSine((cyc - 1.3) / 0.6);
        } else {
            flash = 0.0;
        }
        float flashF = (float) flash;
        float fb = 1.0f + 2.0f * flashF; // множитель яркости орбит

        // Полёт: орбиты расширяются, наклоняются (нормали доворачиваются) и ускоряются.
        // Ось наклона — из сглаженной оси FxState (keep-last): плавный доворот при
        // развороте, без щелчка на 180° (v4.2).
        double rMul = 1.0 + 0.4 * fly;
        double spdMul = 1.0 + 0.3 * fly;
        double ax = st.tiltAxisX();
        double az = st.tiltAxisZ();
        double al = Math.sqrt(ax * ax + az * az);
        Vec3 tiltAxis = null;
        double tilt = 0.0;
        if (fly > 0.01 && al > 1e-4) {
            tiltAxis = new Vec3(ax / al, 0.0, az / al);
            tilt = 0.3 * fly;
        }
        Vec3 n1 = AXIS_FLAT;
        Vec3 n2 = AXIS_TILT_A;
        Vec3 n3 = AXIS_TILT_B;
        if (tiltAxis != null) {
            n1 = AuraGeom.rotateAround(n1, Vec3.ZERO, tiltAxis, tilt);
            n2 = AuraGeom.rotateAround(n2, Vec3.ZERO, tiltAxis, tilt);
            n3 = AuraGeom.rotateAround(n3, Vec3.ZERO, tiltAxis, tilt);
        }

        float[] rgb = lvl.rgb();
        float[] c1 = flashRgb(rgb, flashF);
        float[] c2 = flashRgb(rgb, flashF * 0.9f);
        float[] c3 = flashRgb(rgb, flashF * 0.8f);

        Vec3 chest = pos.add(0.0, ORBIT_Y, 0.0);
        AuraGeom.orbitArc(b, m, chest, n1, ORBIT_RADIUS * rMul, 0.0,
                time * AuraGeom.omega(4.5) * spdMul, 1.0, 48, 0.035,
                AuraGeom.argb(0.55f * i * fb, c1[0], c1[1], c1[2]), false);
        AuraGeom.orbitArc(b, m, chest, n2, ORBIT_RADIUS * rMul, 0.0,
                -time * AuraGeom.omega(6.0) * spdMul, 1.0, 48, 0.03,
                AuraGeom.argb(0.45f * i * fb, c2[0], c2[1], c2[2]), false);
        AuraGeom.orbitArc(b, m, chest, n3, ORBIT_RADIUS * rMul, 0.0,
                time * AuraGeom.omega(8.0) * spdMul, 1.0, 48, 0.03,
                AuraGeom.argb(0.4f * i * fb, c3[0], c3[1], c3[2]), false);

        // Световые узлы: 2–3 на орбиту, едут с кольцом
        AuraGeom.orbitDots(b, m, chest, n1, ORBIT_RADIUS * rMul, 0.0,
                (float) (time * AuraGeom.omega(4.5) * spdMul), 3, time, st.hash(20), right, up, 0.11,
                AuraGeom.argb(0.95f * i * fb, c1[0], c1[1], c1[2]));
        AuraGeom.orbitDots(b, m, chest, n2, ORBIT_RADIUS * rMul, 0.0,
                (float) (-time * AuraGeom.omega(6.0) * spdMul), 2, time, st.hash(21), right, up, 0.11,
                AuraGeom.argb(0.9f * i * fb, c2[0], c2[1], c2[2]));
        AuraGeom.orbitDots(b, m, chest, n3, ORBIT_RADIUS * rMul, 0.0,
                (float) (time * AuraGeom.omega(8.0) * spdMul), 3, time, st.hash(22), right, up, 0.11,
                AuraGeom.argb(0.85f * i * fb, c3[0], c3[1], c3[2]));

        // Ледяное кольцо у ног: пульс радиуса 0.5 -> 0.7 -> 0.5 (цикл ~6 с);
        // в полёте приглушается — не спорит с орбитами
        double pulse = 0.5 + 0.5 * Math.sin(sec * Math.PI * 2.0 / 6.0);
        double r = 0.6 + 0.1 * pulse;
        AuraGeom.ringTile(b, m, pos, r - 0.05, r + 0.05, 0.05, time * AuraGeom.omega(30.0), 48,
                AuraGeom.argb((float) (0.28 * i * (0.6 + 0.4 * pulse) * (1.0 - 0.4 * fly)), rgb[0], rgb[1], rgb[2]));

        // Иней: 9 частиц оседают вниз (rise = -0.9), цикл ~1.6 с, оффсеты 0.18–0.25 с;
        // в полёте приглушается
        AuraGeom.dustMotes(b, m, pos, right, up, time, 9, 0.5, -0.9,
                AuraGeom.TAU / (1.6 * 20.0), 0.07, st.hash(23), 0.0, 0.0, 0.07,
                AuraGeom.argb((float) (0.5 * i * (1.0 - 0.6 * fly)), rgb[0], rgb[1], rgb[2]));

        AuraGeom.bodyGlow(b, m, pos, right, up,
                AuraGeom.argb((float) (0.08 * i * (1.0 - 0.4 * fly)), rgb[0], rgb[1], rgb[2]));
    }

    /** Цвет орбит во время flash: лерп к ice-white (без резкого переключения). */
    private static float[] flashRgb(float[] rgb, float flash) {
        float f = flash * 0.85f;
        return new float[]{rgb[0] + (1.0f - rgb[0]) * f, rgb[1] + (1.0f - rgb[1]) * f, rgb[2] + (1.0f - rgb[2]) * f};
    }
}
