package com.pwp.coreclient.aura.style;

import com.pwp.coreclient.aura.AuraGeom;
import com.pwp.coreclient.aura.FxState;
import com.pwp.coreclient.donor.DonorLevel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * SILVER — «Серебряный след»: тихий, аккуратный, лёгкий premium.
 *
 *  - тонкое кольцо у ног (radius 0.42, width 0.02), полный оборот ~5.2 минуты —
 *    практически неподвижное медленное смещение;
 *  - 9 пылинок-билбордов: стартуют около ног, поднимаются до груди (radius
 *    ~0.30, подъём ~1.3), цикл 2.0–2.8 с, появляются не одновременно
 *    (стаггер + джиттер циклов), при ходьбе слегка тянутся назад;
 *  - ореол: 3 вложенных билборда, лёгкое дыхание, цикл ~3.5 с;
 *  - полёт (0.6 с, тайминг flyBlend из конфига): пыль вытягивается в короткий
 *    серебряный trail позади; при посадке плавно возвращается.
 */
public final class SilverStyle {

    private SilverStyle() {}

    public static void draw(VertexConsumer b, Matrix4f m, Vec3 pos, FxState st, float time, float i,
                            Vec3 right, Vec3 up, DonorLevel lvl) {
        double fly = st.flyBlend();
        double move = st.moveBlend();
        double sec = time / 20.0;

        // Ореол с дыханием (цикл ~3.5 с); в полёте слегка гаснет, уступая trail
        double breath = 0.5 + 0.5 * Math.sin(sec * Math.PI * 2.0 / 3.5 + st.hash(1) * Math.PI * 2.0);
        AuraGeom.bodyGlow(b, m, pos, right, up,
                col(lvl, (float) (0.09 * i * (0.6 + 0.4 * breath) * (1.0 - 0.3 * fly))));

        // Тонкое кольцо у ног: оборот ~5.2 минуты (312 с)
        AuraGeom.flatArc(b, m, pos, 0.42, 0.03, (float) (time * AuraGeom.omega(312.0)), 1.0, 40, 0.02,
                col(lvl, 0.18f * i), false);

        // Пыль: 9 шт, radius ~0.30, подъём 1.3, цикл ~2.4 с (2.0–2.8 с с джиттером);
        // при беге слегка тянется назад, в полёте гаснет — её место занимает trail
        double drag = 0.7 * move;
        double dragX = -st.vx() * drag;
        double dragZ = -st.vz() * drag;
        AuraGeom.dustMotes(b, m, pos, right, up, time, 9, 0.30, 1.3,
                AuraGeom.TAU / (2.4 * 20.0), 0.18, st.hash(2), dragX, dragZ, 0.09,
                col(lvl, (float) (0.55 * i * (1.0 - 0.55 * fly))));

        // Полёт: короткий серебряный trail позади (вытянутые штрихи)
        if (fly > 0.01) {
            double spd = st.speed();
            Vec3 back = spd > 1e-3
                    ? new Vec3(-st.vx() / spd, 0.0, -st.vz() / spd)
                    : new Vec3(0.0, 0.0, -1.0);
            for (int k = 0; k < 3; k++) {
                double h = 0.35 + k * 0.3;
                double len = (0.15 + 0.45 * fly) * (1.0 - k * 0.15);
                double a = (0.5 + 0.35 * st.hash(3 + k)) * fly * i;
                Vec3 p = pos.add(0.0, h, 0.0).add(back.scale(0.1 + k * 0.18));
                AuraGeom.dash(b, m, p, back, up, len, 0.045, col(lvl, (float) a));
            }
        }
    }

    private static int col(DonorLevel lvl, float alpha) {
        float[] rgb = lvl.rgb();
        return AuraGeom.argb(alpha, rgb[0], rgb[1], rgb[2]);
    }
}
