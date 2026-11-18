package com.pwp.coreclient.hints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pwp.coreclient.hints.HintSelector.Candidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class HintSelectorTest {
    private static Candidate c(String id, int prio, boolean once, boolean active) {
        return new Candidate(id, prio, once, active);
    }

    @Test
    void picksHighestPriorityActive() {
        var st = new HintSelector.State();
        var list = List.of(c("a", 1, false, true), c("b", 5, false, true), c("c", 9, false, false));
        assertEquals("b", HintSelector.update(list, st, 0));
    }

    @Test
    void tieKeepsRegistrationOrder() {
        var st = new HintSelector.State();
        assertEquals("a", HintSelector.update(List.of(c("a", 3, false, true), c("b", 3, false, true)), st, 0));
    }

    @Test
    void higherPriorityDoesNotInterrupt() {
        var st = new HintSelector.State();
        HintSelector.update(List.of(c("a", 1, false, true)), st, 0);
        assertEquals("a", HintSelector.update(List.of(c("a", 1, false, true), c("b", 9, false, true)), st, 500));
    }

    @Test
    void onceHintBecomesSeenAfterShowTime() {
        var st = new HintSelector.State();
        var list = List.of(c("a", 1, true, true));
        HintSelector.update(list, st, 0);
        assertNull(HintSelector.update(list, st, HintSelector.SHOW_MS));
        assertTrue(st.seen.contains("a"));
        assertTrue(st.dirty);
        assertNull(HintSelector.update(list, st, HintSelector.SHOW_MS * 10));
    }

    @Test
    void regularHintSnoozesThenReturns() {
        var st = new HintSelector.State();
        var list = List.of(c("a", 1, false, true));
        HintSelector.update(list, st, 0);
        assertNull(HintSelector.update(list, st, HintSelector.SHOW_MS));
        assertNull(HintSelector.update(list, st, HintSelector.SHOW_MS + HintSelector.SNOOZE_MS - 1));
        assertEquals("a", HintSelector.update(list, st, HintSelector.SHOW_MS + HintSelector.SNOOZE_MS));
        assertFalse(st.seen.contains("a"));
    }

    @Test
    void conditionGoneHidesAndCountsOnceHintAsSeen() {
        var st = new HintSelector.State();
        HintSelector.update(List.of(c("a", 1, true, true)), st, 0);
        assertNull(HintSelector.update(List.of(c("a", 1, true, false)), st, HintSelector.MIN_SEEN_MS));
        assertTrue(st.seen.contains("a"));
    }

    @Test
    void flickerShorterThanMinSeenIsNotCounted() {
        var st = new HintSelector.State();
        HintSelector.update(List.of(c("a", 1, true, true)), st, 0);
        HintSelector.update(List.of(c("a", 1, true, false)), st, 100);
        assertFalse(st.seen.contains("a"));
    }

    @Test
    void gapBetweenHints() {
        var st = new HintSelector.State();
        HintSelector.update(List.of(c("a", 1, false, true)), st, 0);
        var next = List.of(c("a", 1, false, false), c("b", 1, false, true));
        assertNull(HintSelector.update(next, st, 1000));
        assertNull(HintSelector.update(next, st, 1000 + HintSelector.GAP_MS - 1));
        assertEquals("b", HintSelector.update(next, st, 1000 + HintSelector.GAP_MS));
    }

    @Test
    void clearForgetsCurrentButKeepsSeen() {
        var st = new HintSelector.State();
        st.seen.add("x");
        HintSelector.update(List.of(c("a", 1, false, true)), st, 0);
        HintSelector.clear(st);
        assertNull(st.current());
        assertTrue(st.seen.contains("x"));
    }
}
