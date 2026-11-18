package com.pwp.coreclient.ui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScreenProjectionTest {
    // камера смотрит вдоль -Z, вверх +Y; right = look × up = +X
    private static final float LX = 0F, LY = 0F, LZ = -1F, UX = 0F, UY = 1F, UZ = 0F;

    @Test
    void pointStraightAheadIsScreenCenter() {
        ScreenProjection p = new ScreenProjection();
        assertTrue(p.project(0, 0, -10, LX, LY, LZ, UX, UY, UZ, 70, 480, 270));
        assertEquals(240, p.x);
        assertEquals(135, p.y);
    }

    @Test
    void rightAndUpMoveMarkerRightAndUp() {
        ScreenProjection p = new ScreenProjection();
        // полуугол 45° (fov 90): tan = 1 → x = 240 + 0.2 * 240
        p.project(2, 3, -10, LX, LY, LZ, UX, UY, UZ, 90, 480, 270);
        assertEquals(288, p.x);
        assertTrue(p.y < 135);
    }

    @Test
    void behindCameraIsHidden() {
        ScreenProjection p = new ScreenProjection();
        assertFalse(p.project(0, 0, 10, LX, LY, LZ, UX, UY, UZ, 70, 480, 270));
        assertFalse(p.visible);
        assertFalse(p.project(0, 0, -0.01, LX, LY, LZ, UX, UY, UZ, 70, 480, 270));
        assertFalse(p.project(Double.NaN, 0, -5, LX, LY, LZ, UX, UY, UZ, 70, 480, 270));
    }

    @Test
    void halfFovIsClampedLikeBefore() {
        ScreenProjection a = new ScreenProjection();
        ScreenProjection b = new ScreenProjection();
        // fov 30 и 60 → полуугол упирается в минимум 30°: одна и та же точка экрана
        a.project(1, 0, -10, LX, LY, LZ, UX, UY, UZ, 30, 480, 270);
        b.project(1, 0, -10, LX, LY, LZ, UX, UY, UZ, 60, 480, 270);
        assertEquals(a.x, b.x);
    }
}
