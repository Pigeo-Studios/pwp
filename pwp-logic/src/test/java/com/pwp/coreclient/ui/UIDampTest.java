package com.pwp.coreclient.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class UIDampTest {

    @Test
    void oneFrameAt60FpsMatchesOldPerFrameLerp() {
        // старый код: x += (1 - x) * 0.2 за кадр; один кадр 60 FPS = 16.667 мс
        float x = 0F;
        for (int i = 0; i < 6; i++) x += (1F - x) * 0.2F; // 6 кадров = 100 мс
        float damped = UIDamp.approach(0F, 1F, 0.2F, 100);
        assertEquals(x, damped, 1e-4);
    }

    @Test
    void resultDoesNotDependOnFrameRate() {
        float fast = 0F;
        for (int i = 0; i < 20; i++) fast = UIDamp.approach(fast, 1F, 0.15F, 5); // 200 FPS, 100 мс
        float slow = 0F;
        for (int i = 0; i < 2; i++) slow = UIDamp.approach(slow, 1F, 0.15F, 50); // 20 FPS, 100 мс
        assertEquals(slow, fast, 1e-4);
    }

    @Test
    void edgeCases() {
        assertEquals(0.3F, UIDamp.approach(0.3F, 1F, 0.2F, 0), 1e-6, "нулевой dt — без сдвига");
        assertEquals(1F, UIDamp.approach(0.3F, 1F, 1F, 16), 1e-6, "коэффициент 1 — сразу цель");
        assertEquals(0.3F, UIDamp.approach(0.3F, 1F, 0F, 16), 1e-6);
        assertEquals(5F, UIDamp.approach(5F, 5F, 0.2F, 16), 1e-6);
    }

    @Test
    void frameClockClampsAndStartsAtZero() {
        AtomicLong now = new AtomicLong(1000);
        UIDamp.Frame f = new UIDamp.Frame(now::get);
        assertEquals(0L, f.dtMs(), "первый кадр");
        now.addAndGet(16);
        assertEquals(16L, f.dtMs());
        now.addAndGet(5000); // свёрнутое окно
        assertEquals(UIDamp.MAX_DT_MS, f.dtMs());
        now.addAndGet(-10); // часы не идут назад, но защита есть
        assertEquals(0L, f.dtMs());
    }
}
