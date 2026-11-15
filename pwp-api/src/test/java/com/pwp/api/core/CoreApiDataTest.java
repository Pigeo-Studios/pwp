package com.pwp.api.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class CoreApiDataTest {
    @Test
    void heartbeatWithoutMatchIsMinimal() {
        JsonObject body = new Heartbeat("lobby", 12).toJson();
        assertEquals("lobby", body.get("server").getAsString());
        assertEquals(12, body.get("online").getAsInt());
        assertFalse(body.has("port"));
        assertFalse(body.has("mapName"));
    }

    @Test
    void heartbeatMatchFieldsAreFlat() {
        // Формат core-service: поля матча на верхнем уровне тела (как в прежнем CoreServerApi)
        JsonObject body = new Heartbeat("match_1", 30)
                .port(25601)
                .match("takmachka", "aas", "usa", null, 200, 150, "PLAYING", 80, 1000L, 30)
                .toJson();
        assertEquals(25601, body.get("port").getAsInt());
        assertEquals("takmachka", body.get("mapName").getAsString());
        assertEquals("", body.get("redFaction").getAsString());
        assertEquals(150, body.get("redScore").getAsInt());
        assertEquals("PLAYING", body.get("phase").getAsString());
    }

    @Test
    void responseHelpers() {
        JsonObject ok =
                JsonParser.parseString("{\"success\":true,\"data\":{\"a\":1}}").getAsJsonObject();
        JsonObject arr =
                JsonParser.parseString("{\"success\":true,\"data\":[1,2]}").getAsJsonObject();
        assertTrue(CoreApi.isSuccess(ok));
        assertFalse(CoreApi.isSuccess(null));
        assertEquals(1, CoreApi.dataObject(ok).get("a").getAsInt());
        assertNull(CoreApi.dataObject(arr));
        assertEquals(2, CoreApi.dataArray(arr).size());
        assertNull(CoreApi.dataArray(null));
    }

    @Test
    void matchPolicyKeepsLastKnownValueWhenServiceDown() {
        MatchPolicy.apply(JsonParser.parseString("{\"canStartNewMatch\":false,\"reason\":\"тех. работы\"}")
                .getAsJsonObject());
        assertFalse(MatchPolicy.cachedCanStart());
        assertEquals("тех. работы", MatchPolicy.cachedReason());

        MatchPolicy.apply(null); // core-service недоступен — значение не сбрасывается
        assertFalse(MatchPolicy.cachedCanStart());

        MatchPolicy.apply(JsonParser.parseString("{\"canStartNewMatch\":true,\"reason\":null}")
                .getAsJsonObject());
        assertTrue(MatchPolicy.cachedCanStart());
        assertNull(MatchPolicy.cachedReason());
    }
}
