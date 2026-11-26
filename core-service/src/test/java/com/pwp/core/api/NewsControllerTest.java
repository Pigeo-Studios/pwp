package com.pwp.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.BadRequestResponse;
import org.junit.jupiter.api.Test;

class NewsControllerTest {
    private static JsonObject j(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void validNewsWithDefaults() {
        var n = NewsController.parse(j("{\"title\":\" Патч 2.1 \",\"body\":\"Радиалка Y\"}"));
        assertEquals("Патч 2.1", n.title());
        assertEquals("news", n.tag());
        assertTrue(n.published());
        assertFalse(n.pinned());
        assertEquals("", n.imageUrl());
    }

    @Test
    void hiddenPinnedUpdate() {
        var n = NewsController.parse(j(
                "{\"title\":\"t\",\"body\":\"b\",\"tag\":\"UPDATE\",\"pinned\":true,\"published\":false,"
                        + "\"imageUrl\":\"https://pwp.example/i.png\"}"));
        assertEquals("update", n.tag());
        assertTrue(n.pinned());
        assertFalse(n.published());
    }

    @Test
    void rejectsBadInput() {
        assertThrows(BadRequestResponse.class, () -> NewsController.parse(j("{\"title\":\"\",\"body\":\"b\"}")));
        assertThrows(BadRequestResponse.class, () -> NewsController.parse(j("{\"title\":\"t\",\"body\":\"\"}")));
        assertThrows(
                BadRequestResponse.class,
                () -> NewsController.parse(j("{\"title\":\"t\",\"body\":\"b\",\"tag\":\"spam\"}")));
        assertThrows(
                BadRequestResponse.class,
                () -> NewsController.parse(j("{\"title\":\"t\",\"body\":\"b\",\"imageUrl\":\"javascript:alert(1)\"}")));
        assertThrows(
                BadRequestResponse.class,
                () -> NewsController.parse(j("{\"title\":\"" + "x".repeat(121) + "\",\"body\":\"b\"}")));
    }
}
