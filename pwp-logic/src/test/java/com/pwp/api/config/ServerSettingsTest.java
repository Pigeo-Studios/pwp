package com.pwp.api.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerSettingsTest {

    @Test
    void defaultsKeepOldHardcodedBehaviour() {
        ServerSettings d = ServerSettings.DEFAULT;
        assertEquals("../PWP-Server/match_template", d.matchTemplate());
        assertEquals(25566, d.basePort());
        assertEquals(LobbySettings.DEFAULT, d.lobby());
        assertFalse(d.prewarm());
        assertEquals(ServerSettings.Role.AUTO, d.role());
    }

    @Test
    void roleAutoDetectsMatchFolder() {
        ServerSettings d = ServerSettings.DEFAULT;
        assertTrue(d.isMatchServer("match_03"));
        assertFalse(d.isMatchServer("lobby"));
        assertFalse(d.isMatchServer(null));
        List<String> w = new ArrayList<>();
        ServerSettings forced = ServerSettings.fromJson(
                JsonParser.parseString("{\"role\":\"MATCH\"}").getAsJsonObject(), w);
        assertTrue(forced.isMatchServer("lobby"));
        assertTrue(w.isEmpty());
        ServerSettings lobby = ServerSettings.fromJson(
                JsonParser.parseString("{\"role\":\"lobby\"}").getAsJsonObject(), w);
        assertFalse(lobby.isMatchServer("match_01"));
    }

    @Test
    void roundTripKeepsUnknownKeys() {
        JsonObject o = JsonParser.parseString("{\"custom\":5,\"matchServers\":{\"extra\":true,\"basePort\":30000}}")
                .getAsJsonObject();
        List<String> w = new ArrayList<>();
        ServerSettings s = ServerSettings.fromJson(o, w);
        assertEquals(30000, s.basePort());
        JsonObject out = s.toJson(o);
        assertEquals(5, out.get("custom").getAsInt());
        assertTrue(out.getAsJsonObject("matchServers").get("extra").getAsBoolean());
        assertEquals(s, ServerSettings.fromJson(out, w));
        assertTrue(w.isEmpty(), w.toString());
    }

    @Test
    void brokenFieldsFallBackWithWarnings() {
        JsonObject o = JsonParser.parseString("{\"role\":\"boss\",\"lobby\":{\"minPlayers\":\"x\",\"prewarm\":1},"
                        + "\"matchServers\":{\"basePort\":1.5},\"mapsDir\":7}")
                .getAsJsonObject();
        List<String> w = new ArrayList<>();
        ServerSettings s = ServerSettings.fromJson(o, w);
        assertEquals(ServerSettings.Role.AUTO, s.role());
        assertEquals(LobbySettings.DEFAULT.minPlayers(), s.lobby().minPlayers());
        assertFalse(s.prewarm());
        assertEquals(25566, s.basePort());
        assertEquals("", s.mapsDir());
        assertEquals(5, w.size(), w.toString());
    }

    @Test
    void fileMigratesLegacyLobbyAndCreatesNewFile(@TempDir Path dir) throws Exception {
        Files.writeString(
                dir.resolve("pwp_lobby.json"),
                "{\"autostart\":false,\"maxMatches\":3,\"minPlayers\":4,\"prewarm\":true}",
                StandardCharsets.UTF_8);
        List<String> log = new ArrayList<>();
        ServerSettingsFile f = new ServerSettingsFile(dir);
        ServerSettings s = f.load(true, log::add);
        assertFalse(s.lobby().autostart());
        assertEquals(3, s.lobby().maxMatches());
        assertEquals(4, s.lobby().minPlayers());
        assertTrue(s.prewarm());
        assertTrue(Files.isRegularFile(dir.resolve("pwp-server.json")));
        assertTrue(Files.isRegularFile(dir.resolve("pwp_lobby.json")), "старый файл не удаляем");
        assertEquals(1, log.size(), log.toString());
        // второй запуск читает уже новый файл
        ServerSettings again = new ServerSettingsFile(dir).load(true, log::add);
        assertEquals(s, again);
    }

    @Test
    void brokenFileIsNotOverwritten(@TempDir Path dir) throws Exception {
        Path p = dir.resolve("pwp-server.json");
        Files.writeString(p, "{oops", StandardCharsets.UTF_8);
        List<String> log = new ArrayList<>();
        ServerSettings s = new ServerSettingsFile(dir).load(true, log::add);
        assertEquals(ServerSettings.DEFAULT, s);
        assertEquals("{oops", Files.readString(p, StandardCharsets.UTF_8));
        assertEquals(1, log.size());
    }

    @Test
    void noCreateOnClient(@TempDir Path dir) {
        new ServerSettingsFile(dir).load(false, m -> {});
        assertFalse(Files.exists(dir.resolve("pwp-server.json")));
    }
}
