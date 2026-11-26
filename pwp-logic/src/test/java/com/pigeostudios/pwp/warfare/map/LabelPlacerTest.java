package com.pigeostudios.pwp.warfare.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LabelPlacerTest {
    private static boolean overlap(LabelPlacer.Spot a, LabelPlacer.Spot b, int w, int h) {
        return a.x() < b.x() + w && a.x() + w > b.x() && a.y() < b.y() + h && a.y() + h > b.y();
    }

    @Test
    void firstLabelGoesBelowThenNeighbourMovesAside() {
        LabelPlacer p = new LabelPlacer();
        p.reset(0, 0, 400, 300);
        LabelPlacer.Spot a = p.place(200, 150, 6, 40, 9, 2);
        assertEquals(LabelPlacer.Side.BELOW, a.side());
        assertEquals(new LabelPlacer.Spot(180, 158, LabelPlacer.Side.BELOW), a);
        // соседняя точка в 4 px: снизу занято — встаёт сверху
        LabelPlacer.Spot b = p.place(204, 150, 6, 40, 9, 2);
        assertEquals(LabelPlacer.Side.ABOVE, b.side());
        assertFalse(overlap(a, b, 40, 9));
    }

    @Test
    void neverOverlapsEvenInDenseCluster() {
        Random r = new Random(42);
        LabelPlacer p = new LabelPlacer();
        p.reset(0, 0, 300, 200);
        List<LabelPlacer.Spot> placed = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            LabelPlacer.Spot s = p.place(140 + r.nextInt(20), 90 + r.nextInt(20), 3, 30, 6, 1);
            if (s != null) placed.add(s);
        }
        for (int i = 0; i < placed.size(); i++) {
            for (int j = i + 1; j < placed.size(); j++) {
                assertFalse(overlap(placed.get(i), placed.get(j), 30, 6), i + " / " + j);
            }
            LabelPlacer.Spot s = placed.get(i);
            assertFalse(s.x() < 0 || s.y() < 0 || s.x() + 30 > 300 || s.y() + 6 > 200, "за краем карты");
        }
        // 200 подписей в клочке 20×20 не влезут — часть скрыта, а не наложена
        assertFalse(placed.size() == 200);
    }

    @Test
    void customOrderPutsNickAbove() {
        LabelPlacer p = new LabelPlacer();
        p.reset(0, 0, 100, 100);
        LabelPlacer.Spot s = p.place(50, 50, 3, 20, 6, 1, LabelPlacer.ABOVE_FIRST);
        assertEquals(LabelPlacer.Side.ABOVE, s.side());
        assertEquals(50 - 3 - 1 - 6, s.y());
    }

    @Test
    void respectsBlockedIconsAndMapEdges() {
        LabelPlacer p = new LabelPlacer();
        p.reset(0, 0, 100, 100);
        p.block(40, 52, 20, 20); // иконка под якорем
        LabelPlacer.Spot s = p.place(50, 45, 5, 20, 8, 2);
        assertNotNull(s);
        assertEquals(LabelPlacer.Side.ABOVE, s.side());
        // у самого края слева и снизу — уходит вправо/вверх, но не за край
        LabelPlacer.Spot edge = p.place(2, 90, 1, 20, 8, 1);
        assertNotNull(edge);
        assertEquals(LabelPlacer.Side.RIGHT, edge.side());
        // в самом углу места нет ни с одной стороны — подпись скрыта, а не вылезла за край
        assertNull(p.place(2, 98, 1, 20, 8, 1));
        assertFalse(edge.x() < 0 || edge.y() + 8 > 100);
        // подпись шире карты — места нет
        assertNull(p.place(50, 50, 1, 200, 8, 1));
    }
}
