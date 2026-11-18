package com.pwp.coreclient.ui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SideListTest {

    @Test
    void allRowsWhenTheyFit() {
        assertEquals(5, SideList.visible(100, 40, 12, 5, 0));
        assertEquals(0, SideList.visible(40, 40, 12, 0, 0));
        assertEquals(100, SideList.height(40, 12, 5, 5));
    }

    @Test
    void lastSlotBecomesMoreLineWhenListOverflows() {
        // место под 4 строки, строк 10: 3 видимых + «+7»
        int v = SideList.visible(40 + 48, 40, 12, 10, 0);
        assertEquals(3, v);
        assertEquals(4, SideList.rows(v, 10));
        assertEquals(88, SideList.height(40, 12, v, 10));
    }

    @Test
    void hiddenWhenFixedPartDoesNotFit() {
        assertEquals(SideList.NONE, SideList.visible(39, 40, 12, 0, 0));
        assertEquals(SideList.NONE, SideList.visible(-5, 40, 12, 3, 0));
        assertEquals(0, SideList.rows(SideList.NONE, 3));
    }

    @Test
    void hiddenWhenNoRoomEvenForMoreLine() {
        assertEquals(SideList.NONE, SideList.visible(45, 40, 12, 3, 0));
    }

    @Test
    void minRowsHidesPanelInsteadOfShowingTooFew() {
        // место под 2 строки: 1 видимая + «+N», а нужно минимум 2
        assertEquals(SideList.NONE, SideList.visible(40 + 24, 40, 12, 6, 2));
        assertEquals(2, SideList.visible(40 + 36, 40, 12, 6, 2));
        // список короче минимума — показывается весь
        assertEquals(1, SideList.visible(40 + 12, 40, 12, 1, 3));
    }

    @Test
    void resultAlwaysFitsSpace() {
        for (int space = 0; space < 200; space++) {
            for (int count = 0; count < 20; count++) {
                for (int min = 0; min < 4; min++) {
                    int v = SideList.visible(space, 30, 11, count, min);
                    if (v == SideList.NONE) continue;
                    assertTrue(SideList.height(30, 11, v, count) <= space, space + "/" + count + "/" + min);
                    assertTrue(v <= count);
                }
            }
        }
    }
}
