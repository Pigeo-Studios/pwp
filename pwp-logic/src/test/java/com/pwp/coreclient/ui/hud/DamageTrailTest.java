package com.pwp.coreclient.ui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pwp.coreclient.ui.UIAnim;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DamageTrailTest {
    private final AtomicLong now = new AtomicLong(10_000);

    @AfterEach
    void restore() {
        UIAnim.setAnimationsEnabled(true);
    }

    /** n кадров по 16 мс к цели. */
    private void run(DamageTrail t, float target, int frames) {
        for (int i = 0; i < frames; i++) {
            now.addAndGet(16);
            t.update(target);
        }
    }

    @Test
    void damageDropsFillButTrailHoldsThenDrains() {
        DamageTrail t = new DamageTrail(1F, now::get);
        t.update(1F);
        run(t, 0.6F, 10); // 160 мс — внутри паузы
        assertTrue(t.value() < 0.65F, "заливка почти упала: " + t.value());
        assertEquals(1F, t.trail(), 1e-6, "хвост стоит на старом значении");
        run(t, 0.6F, 25); // ещё 400 мс — пауза кончилась, хвост стекает
        assertTrue(t.trail() < 1F && t.trail() > t.value(), "хвост стекает: " + t.trail());
        run(t, 0.6F, 200);
        assertEquals(0.6F, t.value(), 1e-6);
        assertEquals(0.6F, t.trail(), 1e-6, "хвост догнал");
    }

    @Test
    void repeatedHitRestartsHoldFromTopOfTrail() {
        DamageTrail t = new DamageTrail(1F, now::get);
        t.update(1F);
        run(t, 0.8F, 20);
        run(t, 0.5F, 5); // второй удар во время паузы
        assertEquals(1F, t.trail(), 1e-6, "хвост от верхней точки первого удара");
        run(t, 0.5F, 20); // 320 мс от второго удара — всё ещё пауза
        assertEquals(1F, t.trail(), 1e-6);
    }

    @Test
    void healGrowsWithoutTrail() {
        DamageTrail t = new DamageTrail(0.3F, now::get);
        t.update(0.3F);
        run(t, 0.9F, 3);
        assertTrue(t.value() > 0.3F && t.value() < 0.9F, "рост плавный: " + t.value());
        assertEquals(t.value(), t.trail(), 1e-6, "при лечении хвоста нет");
        run(t, 0.9F, 200);
        assertEquals(0.9F, t.value(), 1e-6);
    }

    @Test
    void animationsOffSnaps() {
        UIAnim.setAnimationsEnabled(false);
        DamageTrail t = new DamageTrail(1F, now::get);
        run(t, 0.4F, 1);
        assertEquals(0.4F, t.value(), 1e-6);
        assertEquals(0.4F, t.trail(), 1e-6);
    }

    @Test
    void clampsAndNaN() {
        DamageTrail t = new DamageTrail(5F, now::get);
        assertEquals(1F, t.value(), 1e-6);
        t.snap(Float.NaN);
        assertEquals(0F, t.value(), 1e-6);
        run(t, -3F, 5);
        assertEquals(0F, t.value(), 1e-6);
        assertEquals(0F, t.trail(), 1e-6);
    }
}
