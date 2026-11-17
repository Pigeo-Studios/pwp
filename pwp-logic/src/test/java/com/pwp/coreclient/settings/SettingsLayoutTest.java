package com.pwp.coreclient.settings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SettingsLayoutTest {
    @Test
    void controlWidthClampsAndKeepsLabelRoom() {
        // широкий ряд — упор в max
        assertEquals(190, SettingsLayout.controlWidth(600, 8, 80, 190, 60));
        // средний — доля
        assertEquals(135, SettingsLayout.controlWidth(300, 8, 80, 190, 60));
        // узкий (scale 4): подписи остаётся labelMin, контрол сжимается ниже min
        assertEquals(72, SettingsLayout.controlWidth(140, 8, 80, 190, 60));
        assertEquals(0, SettingsLayout.controlWidth(40, 8, 80, 190, 60));
    }

    @Test
    void categoryWidth() {
        assertEquals(160, SettingsLayout.categoryWidth(900, 80, 160));
        assertEquals(96, SettingsLayout.categoryWidth(400, 80, 160));
        assertEquals(60, SettingsLayout.categoryWidth(120, 80, 160));
    }

    @Test
    void visibleRows() {
        assertEquals(5, SettingsLayout.visibleRows(108, 20, 2));
        assertEquals(1, SettingsLayout.visibleRows(5, 20, 2));
    }
}
