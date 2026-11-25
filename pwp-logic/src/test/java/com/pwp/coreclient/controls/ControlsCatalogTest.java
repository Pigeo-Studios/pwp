package com.pwp.coreclient.controls;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ControlsCatalogTest {
    static final String JSON =
            """
            {
              "categories": [
                {"id": "voice", "titleKey": "cat.voice", "order": 20},
                {"id": "move", "titleKey": "cat.move", "order": 10},
                {"id": "empty", "titleKey": "cat.empty", "order": 30}
              ],
              "keys": [
                {"id": "key.jump", "category": "move", "order": 20},
                {"id": "key.forward", "category": "move", "order": 10},
                {"id": "key.lean", "category": "move", "order": 20, "advanced": true, "hintKey": "h.lean"},
                {"id": "key.ptt", "category": "voice", "order": 10},
                {"id": "key.missing", "category": "voice", "order": 20},
                {"id": "key.jump", "category": "voice"},
                {"id": "key.bad", "category": "nope"},
                {"category": "move"}
              ]
            }
            """;

    static KeyInfo k(String id) {
        return new KeyInfo(id, "L " + id, "minecraft", "X", true, KeyInput.key(65), ControlsConflicts.IN_GAME);
    }

    @Test
    void parseOrdersAndCollectsIssues() {
        ControlsCatalog c = ControlsCatalog.parse(JSON);
        assertEquals(
                List.of("move", "voice", "empty"),
                c.categories().stream().map(ControlsCatalog.Category::id).toList());
        // order 20 у jump и lean — порядок файла сохраняется
        assertEquals(
                List.of("key.forward", "key.jump", "key.lean", "key.ptt", "key.missing"),
                c.entries().stream().map(ControlsCatalog.Entry::id).toList());
        assertEquals(3, c.issues().size()); // дубль, неизвестная категория, без id
        ControlsCatalog.Entry lean = c.entries().get(2);
        assertTrue(lean.advanced());
        assertEquals("h.lean", lean.hintKey());
    }

    @Test
    void buildSkipsMissingAndEmptyCategories() {
        ControlsCatalog c = ControlsCatalog.parse(JSON);
        List<KeyInfo> present = List.of(k("key.forward"), k("key.jump"), k("key.lean"), k("key.ptt"), k("key.other"));
        List<ControlsGroup> groups = c.build(present, key -> "T:" + key);
        assertEquals(2, groups.size());
        assertEquals("T:cat.move", groups.get(0).title());
        assertEquals(
                List.of("key.forward", "key.jump"),
                groups.get(0).basic().stream().map(ControlsRow::id).toList());
        assertEquals(
                List.of("key.lean"),
                groups.get(0).advanced().stream().map(ControlsRow::id).toList());
        assertEquals(
                List.of("key.ptt"),
                groups.get(1).rows().stream().map(ControlsRow::id).toList());
        assertEquals("T:cat.voice", groups.get(1).rows().get(0).categoryLabel());
    }

    @Test
    void retainDropsAbsent() {
        ControlsCatalog c = ControlsCatalog.parse(JSON).retain(Set.of("key.ptt", "key.lean"));
        assertEquals(
                List.of("key.lean", "key.ptt"),
                c.entries().stream().map(ControlsCatalog.Entry::id).toList());
    }

    @Test
    void badJsonThrows() {
        assertThrows(IllegalArgumentException.class, () -> ControlsCatalog.parse("[1,2]"));
        assertThrows(IllegalArgumentException.class, () -> ControlsCatalog.parse("{oops"));
        ControlsCatalog empty = ControlsCatalog.parse("{}");
        assertTrue(empty.entries().isEmpty());
        assertEquals(2, empty.issues().size());
    }

    /** Боевой файл ресурсов core-client: разбирается без замечаний, решения аудита на месте. */
    @Test
    void realWhitelist() throws Exception {
        Path p = Path.of(
                "..",
                "pwp-core",
                "src",
                "main",
                "resources",
                "assets",
                "pwp_core_client",
                "controls",
                "whitelist.json");
        ControlsCatalog c;
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            c = ControlsCatalog.parse(r);
        }
        assertEquals(List.of(), c.issues());
        assertEquals(8, c.categories().size());
        Map<String, ControlsCatalog.Entry> byId =
                c.entries().stream().collect(Collectors.toMap(ControlsCatalog.Entry::id, e -> e));
        assertEquals(c.entries().size(), new HashSet<>(byId.keySet()).size());
        assertFalse(byId.get("key.tactical.leanleft").advanced());
        assertTrue(byId.get("key.leaningornot.lean_left").advanced());
        assertTrue(byId.get("key.parcool.Vault").advanced());
        assertEquals("movement", byId.get("key.parcool.Vault").category());
        assertFalse(byId.get("key.pwp_core.lobby_menu").advanced());
        assertTrue(byId.containsKey("key.pwp_core.admin_panel"));
        for (int i = 1; i <= 9; i++) assertTrue(byId.containsKey("key.hotbar." + i));
    }
}
