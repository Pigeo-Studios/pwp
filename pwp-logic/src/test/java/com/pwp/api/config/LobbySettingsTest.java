package com.pwp.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LobbySettingsTest {
    @Test
    void roundTrip() {
        LobbySettings s = new LobbySettings(false, 3, 8);
        List<String> w = new ArrayList<>();
        assertEquals(s, LobbySettings.fromJson(s.toJson(), w));
        assertTrue(w.isEmpty());
    }

    @Test
    void missingFileOrKeysGiveDefaults() {
        List<String> w = new ArrayList<>();
        assertEquals(LobbySettings.DEFAULT, LobbySettings.fromJson(null, w));
        assertEquals(LobbySettings.DEFAULT, LobbySettings.fromJson(new JsonObject(), w));
        assertTrue(w.isEmpty());
    }

    @Test
    void brokenValuesFallBackWithWarning() {
        JsonObject o = JsonParser.parseString("{\"autostart\":\"yes\",\"maxMatches\":2.5,\"minPlayers\":[1]}")
                .getAsJsonObject();
        List<String> w = new ArrayList<>();
        assertEquals(LobbySettings.DEFAULT, LobbySettings.fromJson(o, w));
        assertEquals(3, w.size());
    }

    @Test
    void outOfRangeIsClamped() {
        LobbySettings s = new LobbySettings(true, 0, 1000);
        assertEquals(LobbySettings.MAX_MATCHES_MIN, s.maxMatches());
        assertEquals(LobbySettings.MIN_PLAYERS_MAX, s.minPlayers());
        assertEquals(LobbySettings.MAX_MATCHES_MAX, s.withMaxMatches(99).maxMatches());
        assertEquals(1, s.withMinPlayers(-4).minPlayers());
        JsonObject o = JsonParser.parseString("{\"maxMatches\":1e12}").getAsJsonObject();
        assertEquals(
                LobbySettings.MAX_MATCHES_MAX,
                LobbySettings.fromJson(o, new ArrayList<>()).maxMatches());
    }
}
