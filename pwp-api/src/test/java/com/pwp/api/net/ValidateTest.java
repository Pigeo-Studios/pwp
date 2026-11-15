package com.pwp.api.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class ValidateTest {
    @Test
    void ids() {
        assertNull(Validate.id("skinId", "pwp:ak_gold-01", 64));
        assertNotNull(Validate.id("skinId", "", 64));
        assertNotNull(Validate.id("skinId", "a b", 64));
        assertNotNull(Validate.id("skinId", "x".repeat(65), 64));
        assertNotNull(Validate.id("skinId", "кириллица", 64));
    }

    @Test
    void rangesAndFinite() {
        assertNull(Validate.range("slot", 5, 0, 40));
        assertNotNull(Validate.range("slot", 41, 0, 40));
        assertNotNull(Validate.finite("x", Double.NaN, -1e7, 1e7));
        assertNotNull(Validate.finite("x", Double.POSITIVE_INFINITY, -1e7, 1e7));
        assertNull(Validate.finite("x", 12.5, -1e7, 1e7));
    }

    @Test
    void firstReturnsFirstError() {
        assertNull(Validate.first(null, null));
        assertEquals("b", Validate.first(null, "b", "c"));
        assertNull(Validate.oneOf("t", "BLUE", "BLUE", "RED"));
        assertNotNull(Validate.oneOf("t", "GREEN", "BLUE", "RED"));
        assertNotNull(Validate.oneOf("t", null, "BLUE"));
    }

    @Test
    void largePayloadExceedsOldUtfLimit() {
        String big = "{\"data\":\"" + "я".repeat(40_000) + "\"}"; // > 32767 символов и > 64 КБ байт
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        Payloads.writeLarge(buf, big);
        assertEquals(big, Payloads.readLarge(buf));
    }
}
