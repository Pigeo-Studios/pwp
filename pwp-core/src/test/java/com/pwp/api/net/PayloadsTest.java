package com.pwp.api.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** Payloads зависит от FriendlyByteBuf — тест остаётся в Forge-модуле pwp-api. */
class PayloadsTest {
    @Test
    void largePayloadExceedsOldUtfLimit() {
        String big = "{\"data\":\"" + "я".repeat(40_000) + "\"}"; // > 32767 символов и > 64 КБ байт
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        Payloads.writeLarge(buf, big);
        assertEquals(big, Payloads.readLarge(buf));
    }
}
