package com.pwp.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommunitySettingsTest {
    @Test
    void roundTripThroughServerSettings() {
        List<String> w = new ArrayList<>();
        JsonObject o = ServerSettings.DEFAULT.toJson(null);
        assertTrue(o.has("community"));
        ServerSettings back = ServerSettings.fromJson(o, w);
        assertEquals(CommunitySettings.DEFAULT, back.community());
        assertTrue(w.isEmpty(), w.toString());
    }

    @Test
    void oldFileWithoutSectionGetsDefaults() {
        List<String> w = new ArrayList<>();
        ServerSettings s = ServerSettings.fromJson(
                JsonParser.parseString("{\"role\":\"lobby\"}").getAsJsonObject(), w);
        assertEquals(CommunitySettings.DEFAULT, s.community());
        assertTrue(w.isEmpty());
    }

    @Test
    void brokenFieldsFallBackAndAreReported() {
        List<String> w = new ArrayList<>();
        CommunitySettings c = CommunitySettings.fromJson(
                JsonParser.parseString("{\"afkKickMinutes\":\"x\",\"chatFilter\":false,\"reservedSlots\":999}")
                        .getAsJsonObject(),
                w);
        assertEquals(CommunitySettings.DEFAULT.afkKickMinutes(), c.afkKickMinutes());
        assertFalse(c.chatFilter());
        assertEquals(50, c.reservedSlots());
        assertEquals(1, w.size());
    }

    @Test
    void withersKeepCommunity() {
        CommunitySettings c = new CommunitySettings(0, 0, 0, 0, 0, 0, false, 0, false);
        ServerSettings s =
                new ServerSettings(ServerSettings.Role.AUTO, LobbySettings.DEFAULT, false, "t", 25566, "", c);
        assertEquals(c, s.withPrewarm(true).community());
        assertEquals(c, s.withLobby(LobbySettings.DEFAULT).community());
    }
}
