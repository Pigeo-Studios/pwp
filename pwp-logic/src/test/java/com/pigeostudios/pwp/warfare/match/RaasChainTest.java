package com.pigeostudios.pwp.warfare.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RaasChainTest {
    /** 10 точек на линии x = 0..900, по две на «слой» (z = ±50). */
    private static List<RaasChain.Point> line() {
        List<RaasChain.Point> pts = new ArrayList<>();
        for (int i = 0; i < 10; i++) pts.add(new RaasChain.Point(i, (i / 2) * 200 + 50, i % 2 == 0 ? -50 : 50));
        return pts;
    }

    private static final double[] BLUE = {0, 0};
    private static final double[] RED = {1000, 0};

    @Test
    void onePointPerLayerFromBlueToRed() {
        List<Integer> chain = RaasChain.build(line(), BLUE, RED, 5, new Random(1));
        assertEquals(5, chain.size());
        for (int layer = 0; layer < 5; layer++) {
            int idx = chain.get(layer);
            assertEquals(layer, idx / 2, "точка " + idx + " должна быть из слоя " + layer);
        }
    }

    @Test
    void differentMatchesGiveDifferentChains() {
        Set<List<Integer>> seen = new HashSet<>();
        for (int seed = 0; seed < 20; seed++) seen.add(RaasChain.build(line(), BLUE, RED, 5, new Random(seed)));
        assertTrue(seen.size() > 5, "цепочки должны различаться: " + seen.size());
    }

    @Test
    void reversedMainsReverseTheChain() {
        List<Integer> c = RaasChain.build(line(), RED, BLUE, 5, new Random(3));
        assertEquals(4, c.get(0) / 2, "от мейна RED первым идёт дальний слой");
    }

    @Test
    void fewerPointsThanLayersTakesAllInOrder() {
        List<RaasChain.Point> pts =
                List.of(new RaasChain.Point(0, 800, 0), new RaasChain.Point(1, 100, 0), new RaasChain.Point(2, 400, 0));
        assertEquals(List.of(1, 2, 0), RaasChain.build(pts, BLUE, RED, 5, new Random(7)));
        assertNotEquals(0, RaasChain.build(pts, BLUE, RED, 1, new Random(7)).size());
        assertEquals(List.of(0), RaasChain.build(List.of(new RaasChain.Point(0, 1, 1)), BLUE, BLUE, 5, new Random()));
    }
}
