package com.pwp.api.lobby;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.api.lobby.MatchHandoff.Settings;
import com.pwp.api.lobby.MatchHandoff.Stage;
import com.pwp.api.lobby.MatchHandoff.Status;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchHandoffTest {
    private static final Settings S = new Settings("ukraine", "russia", "invasion", 300, 900, true, 1);

    @Test
    void settingsRoundTrip() {
        List<String> err = new ArrayList<>();
        Settings back =
                Settings.fromJson(JsonParser.parseString(S.toJson().toString()).getAsJsonObject(), err);
        assertEquals(S, back);
        assertTrue(err.isEmpty());
    }

    @Test
    void badSettingsRejectedWithReasons() {
        JsonObject o = S.toJson();
        o.addProperty("blueFaction", "../evil");
        o.addProperty("mode", "tdm");
        o.addProperty("redTickets", -5);
        o.remove("patternIndex");
        List<String> err = new ArrayList<>();
        assertNull(Settings.fromJson(o, err));
        assertTrue(err.containsAll(List.of("blueFaction", "mode", "redTickets", "patternIndex")), err.toString());
        assertNull(Settings.fromJson(null, new ArrayList<>()));
    }

    @Test
    void applyToMapConfigKeepsOtherKeys() {
        JsonObject root = JsonParser.parseString(
                        "{\"name\":\"takmachka\",\"teams\":{\"BLUE\":{\"faction\":\"usa\",\"tickets\":1,\"spawn\":{}},"
                                + "\"RED\":{\"faction\":\"nato\",\"tickets\":2}},\"capturePointPatterns\":[{},{}],"
                                + "\"custom\":42}")
                .getAsJsonObject();
        S.applyTo(root);
        JsonObject blue = root.getAsJsonObject("teams").getAsJsonObject("BLUE");
        assertEquals("ukraine", blue.get("faction").getAsString());
        assertEquals(300, blue.get("tickets").getAsInt());
        assertTrue(blue.has("spawn"));
        assertEquals(
                "russia",
                root.getAsJsonObject("teams")
                        .getAsJsonObject("RED")
                        .get("faction")
                        .getAsString());
        assertEquals("invasion", root.get("mode").getAsString());
        assertEquals("INVASION", root.get("modeDisplayName").getAsString());
        assertEquals("RED", root.get("invasionDefender").getAsString());
        assertEquals(1, root.get("capturePointPatternIndex").getAsInt());
        assertEquals(42, root.get("custom").getAsInt());
    }

    @Test
    void applyToSkipsPatternOutOfRangeAndCreatesTeams() {
        JsonObject root = new JsonObject();
        root.add("capturePointPatterns", new JsonArray());
        S.applyTo(root);
        assertFalse(root.has("capturePointPatternIndex"));
        assertEquals(
                "ukraine",
                root.getAsJsonObject("teams")
                        .getAsJsonObject("BLUE")
                        .get("faction")
                        .getAsString());
        new Settings("usa", "russia", "aas", 1, 1, false, -1).applyTo(root);
        assertEquals("BLUE", root.get("invasionDefender").getAsString());
        assertEquals("Advance and Secure", root.get("modeDisplayName").getAsString());
    }

    @Test
    void statusRoundTripAndGarbage() {
        Status st = new Status(Stage.AWAITING_SETTINGS, false, 5, 10, null);
        assertEquals(st, Status.fromJson(st.toJson(123)));
        Status err = new Status(Stage.ERROR, false, 0, 0, "bad_settings");
        assertEquals(err, Status.fromJson(err.toJson(1)));
        assertNull(
                Status.fromJson(JsonParser.parseString("{\"stage\":\"NOPE\"}").getAsJsonObject()));
        assertNull(Status.fromJson(null));
        // done не больше total, отрицательные — 0
        Status clamped = Status.fromJson(
                JsonParser.parseString("{\"stage\":\"READY\",\"prerenderDone\":50,\"prerenderTotal\":10}")
                        .getAsJsonObject());
        assertEquals(10, clamped.prerenderDone());
        assertFalse(clamped.settingsApplied());
    }
}
