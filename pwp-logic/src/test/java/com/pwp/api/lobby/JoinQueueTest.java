package com.pwp.api.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JoinQueueTest {
    @Test
    void fifoWithPriorityFirst() {
        JoinQueue q = new JoinQueue();
        assertEquals(1, q.join("a", false, 0));
        assertEquals(2, q.join("b", false, 10));
        assertEquals(1, q.join("mod", true, 20));
        assertEquals(2, q.position("a"));
        assertEquals(3, q.position("b"));
        assertEquals(0, q.position("nobody"));
        // повторный вход не сбрасывает место
        assertEquals(2, q.join("a", false, 99));
    }

    @Test
    void admitUsesReservedForPriorityThenPublic() {
        JoinQueue q = new JoinQueue();
        q.join("a", false, 0);
        q.join("b", false, 1);
        q.join("mod", true, 2);
        // 0 общих, 1 резервный: проходит только модератор
        assertEquals(List.of("mod"), q.admit(0, 1));
        assertEquals(2, q.size());
        // 1 общий: первый по времени
        assertEquals(List.of("a"), q.admit(1, 0));
        // резерв обычным игрокам не отдаётся
        assertEquals(List.of(), q.admit(0, 5));
        assertTrue(q.contains("b"));
    }

    @Test
    void priorityTakesPublicSlotWhenNoReserve() {
        JoinQueue q = new JoinQueue();
        q.join("a", false, 0);
        q.join("mod", true, 5);
        assertEquals(List.of("mod"), q.admit(1, 0));
    }

    @Test
    void leaveAndExpire() {
        JoinQueue q = new JoinQueue();
        q.join("a", false, 0);
        q.join("b", false, 50_000);
        assertTrue(q.leave("a"));
        assertFalse(q.leave("a"));
        q.join("c", false, 0);
        assertEquals(List.of("c"), q.expire(70_000, 60_000));
        assertEquals(1, q.position("b"));
    }
}
