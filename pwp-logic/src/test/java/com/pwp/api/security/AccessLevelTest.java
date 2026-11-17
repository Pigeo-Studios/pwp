package com.pwp.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccessLevelTest {
    @Test
    void opMapping() {
        assertEquals(AccessLevel.PLAYER, AccessLevel.fromOp(0));
        assertEquals(AccessLevel.PLAYER, AccessLevel.fromOp(1));
        assertEquals(AccessLevel.ADMIN, AccessLevel.fromOp(2));
        assertEquals(AccessLevel.ADMIN, AccessLevel.fromOp(3));
        assertEquals(AccessLevel.OWNER, AccessLevel.fromOp(4));
    }

    @Test
    void roleMapping() {
        assertEquals(AccessLevel.PLAYER, AccessLevel.fromRole("user"));
        assertEquals(AccessLevel.MODERATOR, AccessLevel.fromRole("Support"));
        assertEquals(AccessLevel.ADMIN, AccessLevel.fromRole("admin"));
        assertEquals(AccessLevel.OWNER, AccessLevel.fromRole(" OWNER "));
        assertEquals(AccessLevel.PLAYER, AccessLevel.fromRole("hacker"));
        assertEquals(AccessLevel.PLAYER, AccessLevel.fromRole(null));
    }

    @Test
    void combineTakesMax() {
        assertEquals(AccessLevel.MODERATOR, AccessLevel.combine(0, AccessLevel.MODERATOR));
        assertEquals(AccessLevel.ADMIN, AccessLevel.combine(2, AccessLevel.MODERATOR));
        assertEquals(AccessLevel.OWNER, AccessLevel.combine(2, AccessLevel.OWNER));
        // core-service недоступен — только op
        assertEquals(AccessLevel.ADMIN, AccessLevel.combine(3, null));
        assertEquals(AccessLevel.PLAYER, AccessLevel.combine(0, null));
    }

    @Test
    void ordering() {
        assertTrue(AccessLevel.OWNER.atLeast(AccessLevel.ADMIN));
        assertTrue(AccessLevel.MODERATOR.atLeast(AccessLevel.MODERATOR));
        assertFalse(AccessLevel.MODERATOR.atLeast(AccessLevel.ADMIN));
        assertFalse(AccessLevel.PLAYER.atLeast(AccessLevel.MODERATOR));
    }

    @Test
    void parseIsSafe() {
        assertEquals(AccessLevel.ADMIN, AccessLevel.parse("admin"));
        assertEquals(AccessLevel.PLAYER, AccessLevel.parse("GOD"));
        assertEquals(AccessLevel.PLAYER, AccessLevel.parse(null));
    }
}
