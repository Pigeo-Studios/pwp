package com.pwp.api.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RateLimiterTest {
    @Test
    void oncePerIntervalPerKey() {
        AtomicLong now = new AtomicLong(1000);
        RateLimiter r = new RateLimiter(500, now::get);
        assertTrue(r.tryAcquire("a"));
        assertFalse(r.tryAcquire("a"));
        assertTrue(r.tryAcquire("b"), "ключи независимы");
        now.addAndGet(499);
        assertFalse(r.tryAcquire("a"));
        now.addAndGet(1);
        assertTrue(r.tryAcquire("a"));
    }

    @Test
    void mapDoesNotGrowUnbounded() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter r = new RateLimiter(10, now::get);
        for (int i = 0; i < RateLimiter.MAX_KEYS * 3; i++) {
            assertTrue(r.tryAcquire("k" + i));
        }
        // даже без хода времени новые ключи принимаются (карта сбрасывается), старые — тоже
        assertTrue(r.tryAcquire("new"));
    }
}
