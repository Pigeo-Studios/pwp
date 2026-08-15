package com.pwp.coreclient.aura.style;

import com.pwp.coreclient.aura.AuraGeom;
import com.pwp.coreclient.aura.FxState;
import com.pwp.coreclient.donor.DonorLevel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * ADMIN — «RED SCANNER / OVERWATCH»: агрессивный, военный, сразу заметный.
 *
 * Палитра: Dark Red #520000 / Red #A00000 / Bright Red #FF2020 / Hot #FFFFFF.
 *
 *  1. RED RING — 8 сегментов с градацией яркости (1.0/0.8/0.65...): крутящийся
 *     радар, полный оборот ~2.8 с, radius 0.55–0.75;
 *  2. SCANNER ARCS — две красные HUD-дуги вокруг корпуса (radius ~0.9, высота
 *     ~1.0), периоды 3.6 с и 5.2 с, разные направления;
 *  3. VERTICAL SCAN — главный эффект: красная полоса снизу вверх (0 -> 1.5),
 *     цикл 2.3 с, затухающий хвост из двух колец ниже, restart через fade
 *     (бесшовно), бело-красная вспышка при достижении головы;
 *  4. EXPANDING PULSE — импульс вокруг ног раз в ~6 с: R 0.3 -> 1.4 за 1.2 с,
 *     easeOutCubic, fade;
 *  5. VERTICAL SPARKS — редкие быстрые частицы вверх из кольца: короткая
 *     жизнь, яркое начало, быстрое затухание, небольшое отклонение;
 *  6. MOVEMENT TRAIL — 3–4 красные искры позади при беге по интерполированному
 *     (сглаженному) направлению движения, переходы 0.3 с / 0.5 с;
 *  FLIGHT (~0.8 с) — кольцо наклоняется и расширяется, становится орбитой,
 *     дуги — широкие flight-arcs, под игроком короткий энергетический круг,
 *     позади trail, плавно изгибающийся при смене направления;
 *  LANDING (~0.85 с) — кольцо возвращается горизонтально, радиус кратко
 *     расширяется, 6 красных искр расходятся по земле, яркость кратко
 *     повышается, затем эффект возвращается в норму.
 *
 * Корона над головой доработана: циклы в секундах, дыхание лезвий, лёгкий
 * наклон по направлению полёта.
 */
public final class AdminStyle {

    private AdminStyle() {}

    private static final int DARK_RED = 0xFF520000;
    private static final int RED = 0xFFA00000;
    private static final int BRIGHT_RED = 0xFFFF2020;
    private static final int HOT_WHITE = 0xFFFFFFFF;

    private static final Vec3 UP_AXIS = new Vec3(0, 1, 0);

    public static void draw(VertexConsumer b, Matrix4f m, Vec3 pos, FxState st, float time, float i,
                            Vec3 right, Vec3 up, DonorLevel lvl) {
        double fly = st.flyBlend();
        double move = st.moveBlend();
        double land = st.landing();
        double sec = time / 20.0;
        float boost = (float) (1.0 + 0.4 * land); // краткий подъём яркости при посадке
        float fi = i * boost;

        // Сглаженное направление движения из оси наклона FxState (axis ⊥ скорости):
        // ось keep-last — при зависании не обнуляется, при развороте вращается
        // непрерывно через ноль. Направление НЕ щёлкает на 180° и не пропадает.
        double spd = st.speed();
        double ax = st.tiltAxisX();
        double az = st.tiltAxisZ();
        double al = Math.sqrt(ax * ax + az * az);
        Vec3 axis = al > 1e-4 ? new Vec3(ax / al, 0.0, az / al) : null;
        Vec3 fwd = axis == null ? null : new Vec3(az, 0.0, -ax);
        Vec3 back = fwd == null ? null : new Vec3(-fwd.x, 0.0, -fwd.z);
        double speedFade = Math.min(1.0, spd * 20.0); // trail плавно гаснет при остановке

        drawCrown(b, m, pos, st, time, fi, fly, right, up, fwd);

        // --- 1. RED RING: радар из 8 сегментов, оборот ~2.8 с, радиус дышит.
        // В полёте плоскость наклоняется вокруг сглаженной оси (maxTilt ~0.55 рад —
        // умеренная «орбита», не вертикальное кольцо) и расширяется, а ЦЕНТР плавно
        // поднимается к груди (0.85*fly): кольцо не уходит под ноги и не протыкает
        // модель. Наклон непрерывен: завис — кольцо остаётся наклонённым по
        // последнему направлению, разворот — плавный доворот.
        double rr = (0.65 + 0.07 * Math.sin(sec * Math.PI * 2.0 / 8.0))
                * (1.0 + 0.30 * fly) * (1.0 + 0.25 * land);
        double ringRot = time * AuraGeom.omega(2.8);
        Vec3 ringNormal = UP_AXIS;
        Vec3 ringCenter = pos.add(0.0, 0.85 * fly, 0.0);
        if (fly > 0.01 && axis != null) {
            ringNormal = AuraGeom.rotateAround(UP_AXIS, Vec3.ZERO, axis, fly * 0.55).normalize();
        }
        AuraGeom.radarRingEx(b, m, ringCenter, ringNormal, rr, 0.1, ringRot, 8, 0.6, 0.075, 0.55,
                AuraGeom.color(BRIGHT_RED, 0.85f * fi));

        // --- 2. SCANNER ARCS: две дуги вокруг корпуса (периоды 3.6 / 5.2 с, встречные);
        // в полёте — широкие flight-arcs
        double arcRadius = 0.9 * (1.0 + 0.4 * fly);
        double arcWidth = 0.10 * (1.0 + 1.0 * fly);
        Vec3 chest = pos.add(0.0, 1.0, 0.0);
        AuraGeom.orbitArc(b, m, chest, ringNormal, arcRadius, 0.0, time * AuraGeom.omega(3.6),
                0.35, 16, arcWidth, AuraGeom.color(BRIGHT_RED, 0.7f * fi), true);
        AuraGeom.orbitArc(b, m, chest, ringNormal, arcRadius, 0.0, -time * AuraGeom.omega(5.2),
                0.35, 16, arcWidth, AuraGeom.color(RED, 0.65f * fi), true);

        // --- 3. VERTICAL SCAN: полоса снизу вверх за 2.3 с (0->1.5), fade 2.0->2.3,
        // два затухающих хвоста ниже, вспышка при достижении головы.
        // В полёте плавно гаснет — полётный набор это орбита + arcs + круг + trail.
        double flyFade = 1.0 - fly;
        double vt = (sec % 2.3) / 2.3;
        double climbEnd = 2.0 / 2.3;
        double h = 1.5 * Mth.clamp(vt / climbEnd, 0.0, 1.0);
        double env = vt <= climbEnd ? 1.0 : 1.0 - AuraGeom.smoothstep(climbEnd, 1.0, vt);
        float envF = (float) (env * flyFade);
        double bandRot = time * 0.1;
        AuraGeom.flatArc(b, m, pos, 0.6, h, bandRot, 1.0, 48, 0.10,
                AuraGeom.color(BRIGHT_RED, 0.85f * fi * envF), false);
        AuraGeom.flatArc(b, m, pos, 0.6, h - 0.3, bandRot, 1.0, 48, 0.08,
                AuraGeom.color(RED, 0.4f * fi * envF), false);
        AuraGeom.flatArc(b, m, pos, 0.6, h - 0.6, bandRot, 1.0, 48, 0.06,
                AuraGeom.color(DARK_RED, 0.25f * fi * envF), false);
        double headFlash = AuraGeom.smoothstep(0.0, 1.0,
                1.0 - Math.min(Math.abs(sec % 2.3 - 2.0) / 0.4, 1.0));
        if (headFlash > 0.01) {
            AuraGeom.glowQuad(b, m, pos.add(0.0, 1.5, 0.0), right, up, 0.3 + 0.1 * headFlash,
                    AuraGeom.color(HOT_WHITE, (float) (0.8 * fi * headFlash * flyFade)));
        }

        // --- 4. EXPANDING PULSE: раз в ~6 с, R 0.3 -> 1.4 за 1.2 с, easeOutCubic;
        // в полёте гаснет
        double pp = sec % 6.0;
        if (pp < 1.2) {
            double prog = pp / 1.2;
            double pr = Mth.lerp(AuraGeom.easeOutCubic(prog), 0.3, 1.4);
            AuraGeom.flatArc(b, m, pos, pr, 0.6, 0.0, 1.0, 48, 0.05,
                    AuraGeom.color(BRIGHT_RED, (float) (0.8 * fi * (1.0 - prog) * flyFade)), false);
        }

        // --- 5. VERTICAL SPARKS: редкие быстрые искры вверх из кольца; в полёте гаснут
        for (int k = 0; k < 3; k++) {
            double s = (sec + st.hash(30 + k) * 4.0) % 4.0;
            if (s < 0.5) {
                double prog = s / 0.5;
                double a = st.hash(33 + k) * Math.PI * 2.0;
                double sr = 0.72 + st.hash(36 + k) * 0.1;
                Vec3 p = pos.add(Math.cos(a) * sr, 0.1 + prog * 1.1, Math.sin(a) * sr);
                float sa = (float) ((1.0 - prog) * flyFade); // яркое начало, быстрое затухание
                AuraGeom.dash(b, m, p, UP_AXIS, right, 0.16, 0.05,
                        AuraGeom.color(BRIGHT_RED, 0.75f * fi * sa));
            }
        }

        // --- 6. MOVEMENT TRAIL: 3–4 искры позади по сглаженному направлению;
        // в полёте уступает flight-trail (fade), при остановке гаснет по скорости
        if (move > 0.02 && back != null) {
            double fTrailFade = 1.0 - 0.8 * fly;
            for (int k = 0; k < 4; k++) {
                double ph = AuraGeom.wrapFrac(sec / 0.6 + k * 0.25 + st.hash(40 + k));
                double dist = 0.2 + ph * 0.5;
                double off = Math.sin(k * 2.1) * 0.18;
                Vec3 perp = new Vec3(-back.z, 0.0, back.x).scale(off);
                Vec3 p = pos.add(0.0, 0.1 + k * 0.12, 0.0)
                        .add(back.scale(dist * move)).add(perp);
                float a = (float) ((1.0 - ph) * 0.7 * move * i * fTrailFade * speedFade);
                AuraGeom.dash(b, m, p, back, up, 0.12, 0.05, AuraGeom.color(BRIGHT_RED, a));
            }
        }

        // --- FLIGHT: энергетический круг под игроком + красный trail позади.
        // Trail рисуется по последнему направлению (axis keep-last) и гаснет при
        // остановке — без щелчков и исчезновений при пересечении нуля скорости.
        if (fly > 0.02) {
            double circleA = 0.35 * (fly * (1.0 - fly) * 4.0 + 0.15 * fly);
            AuraGeom.ringTile(b, m, pos, 0.85, 1.15, 0.05, 0.0, 48,
                    AuraGeom.color(BRIGHT_RED, (float) (circleA * fi)));
            if (back != null) {
                for (int k = 0; k < 3; k++) {
                    double th = 0.3 + k * 0.35;
                    double len = (0.2 + 0.6 * fly) * (1.0 - k * 0.15);
                    Vec3 p = pos.add(0.0, th, 0.0).add(back.scale(0.15 + k * 0.2));
                    AuraGeom.dash(b, m, p, back, up, len, 0.06,
                            AuraGeom.color(RED, (float) (0.6 * fi * fly * (1.0 - k * 0.2) * speedFade)));
                }
            }
        }

        // --- LANDING: искры расходятся по земле
        if (land > 0.01) {
            for (int k = 0; k < 6; k++) {
                double a = st.hash(50 + k) * Math.PI * 2.0;
                double r = 0.35 + (1.0 - land) * 0.85;
                Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
                Vec3 p = pos.add(dir.x * r, 0.06, dir.z * r);
                AuraGeom.dash(b, m, p, dir, UP_AXIS, 0.14, 0.05,
                        AuraGeom.color(BRIGHT_RED, (float) (0.65 * fi * land)));
            }
        }
    }

    /** Корона: тонкое кольцо + 8 наклонных лезвий с дыханием; в полёте лёгкий наклон вперёд. */
    private static void drawCrown(VertexConsumer b, Matrix4f m, Vec3 pos, FxState st, float time,
                                  float i, double fly, Vec3 right, Vec3 up, Vec3 fwd) {
        double sec = time / 20.0;
        double breath = 0.5 + 0.5 * Math.sin(sec * Math.PI * 2.0 / 5.0 + st.hash(60) * Math.PI * 2.0);
        double bladeBreath = 0.5 + 0.5 * Math.sin(sec * Math.PI * 2.0 / 4.0);
        double crownRot = time * AuraGeom.omega(30.0);

        // Лёгкий наклон по СГЛАЖЕННОМУ направлению (без щелчка при развороте)
        Vec3 tilt = new Vec3(0.0, 0.0, 0.0);
        if (fly > 0.01 && fwd != null) {
            tilt = fwd.scale(0.12 * fly);
        }
        Vec3 headTop = pos.add(0.0, 1.9, 0.0).add(tilt);

        AuraGeom.flatArc(b, m, headTop, 0.30, 0.0, crownRot, 1.0, 32, 0.025,
                AuraGeom.color(BRIGHT_RED, (float) (0.8 * i * (0.7 + 0.3 * breath))), false);
        for (int s = 0; s < 8; s++) {
            double a = crownRot + s * AuraGeom.TAU / 8;
            double len = 0.30 * (0.85 + 0.15 * Math.sin(sec * Math.PI * 2.0 / 4.0 + s * 0.9));
            Vec3 base = AuraGeom.polar(headTop, 0.30, 0.0, a);
            Vec3 tip = AuraGeom.polar(headTop, 0.16, len, a);
            Vec3 outward = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            AuraGeom.ribbonWorld(b, m, base, tip, outward, 0.03,
                    AuraGeom.color(BRIGHT_RED, (float) (0.85 * i * (0.7 + 0.3 * bladeBreath))));
        }
        AuraGeom.glowQuad(b, m, headTop, right, up, 0.14, AuraGeom.color(HOT_WHITE, 0.6f * i));
    }
}
