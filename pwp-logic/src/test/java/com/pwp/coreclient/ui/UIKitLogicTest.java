package com.pwp.coreclient.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class UIKitLogicTest {

    @Test
    void animReachesTargetByRealTimeNotTicks() {
        AtomicLong now = new AtomicLong(1000);
        UIAnim a = new UIAnim(0F, 200, UIAnim.LINEAR, now::get);
        assertEquals(0F, a.value());
        a.to(1F);
        now.addAndGet(100);
        assertEquals(0.5F, a.value(), 1e-4);
        now.addAndGet(100);
        assertEquals(1F, a.value(), 1e-4);
        assertTrue(a.done());
    }

    @Test
    void retargetContinuesFromCurrentValue() {
        AtomicLong now = new AtomicLong(0);
        UIAnim a = new UIAnim(0F, 100, UIAnim.LINEAR, now::get);
        a.to(1F);
        now.addAndGet(50); // 0.5
        a.to(0F); // назад с 0.5, без скачка
        assertEquals(0.5F, a.value(), 1e-4);
        now.addAndGet(50);
        assertEquals(0.25F, a.value(), 1e-4);
        a.snap(0.7F);
        assertEquals(0.7F, a.value(), 1e-4);
        assertFalse(a.to(0.7F).progress() < 1F, "та же цель не перезапускает анимацию");
    }

    @Test
    void radialSectorsClockwiseFromTop() {
        // 4 сектора: 0 — вверх, 1 — вправо, 2 — вниз, 3 — влево
        assertEquals(0, UIMath.radialSector(100, 100, 100, 40, 4, 10, 80));
        assertEquals(1, UIMath.radialSector(100, 100, 160, 100, 4, 10, 80));
        assertEquals(2, UIMath.radialSector(100, 100, 100, 160, 4, 10, 80));
        assertEquals(3, UIMath.radialSector(100, 100, 40, 100, 4, 10, 80));
        // граница сектора 0/1 на 45° — уже сектор 1
        assertEquals(1, UIMath.radialSector(100, 100, 150, 50, 4, 10, 80));
    }

    @Test
    void radialDeadZones() {
        assertEquals(-1, UIMath.radialSector(100, 100, 102, 101, 4, 10, 80), "центр");
        assertEquals(-1, UIMath.radialSector(100, 100, 300, 100, 4, 10, 80), "за внешним радиусом");
        assertEquals(-1, UIMath.radialSector(100, 100, 100, 40, 0, 10, 80), "нет секторов");
    }

    @Test
    void scrollClampAndThumb() {
        assertEquals(0, UIMath.clampScroll(-5, 500, 100));
        assertEquals(400, UIMath.clampScroll(1000, 500, 100));
        assertEquals(0, UIMath.clampScroll(50, 80, 100), "контент меньше окна");
        assertEquals(20, UIMath.thumbHeight(500, 100, 16));
        assertEquals(16, UIMath.thumbHeight(5000, 100, 16), "не меньше минимума");
        assertEquals(0, UIMath.thumbOffset(0, 500, 100, 20));
        assertEquals(80, UIMath.thumbOffset(400, 500, 100, 20));
    }

    @Test
    void keepInsideAndScaled() {
        assertEquals(190, UIMath.keepInside(250, 100, 300, 10));
        assertEquals(10, UIMath.keepInside(-20, 100, 300, 10));
        // 1280x720 vs 1920x1080 в GUI-пикселях при scale 2: 640x360 и 960x540
        assertEquals(216, UIMath.scaled(640, 360, 0.6F, 200, 360));
        assertEquals(324, UIMath.scaled(960, 540, 0.6F, 200, 360));
        assertEquals(200, UIMath.scaled(320, 180, 0.6F, 200, 360), "минимум на маленьком экране");
    }
}
