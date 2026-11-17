package com.pwp.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MapConfigLegacyTest {

    @Test
    void findsDeadTopAndSettingsKeys() {
        List<String> keys = MapConfigLegacy.ignoredKeys(
                List.of("name", "durationMinutes", "capturePoints", "vehicles"),
                List.of("respawnTimer", "hideNametags", "deathTicketCost", "lockDurationMinutes"));
        assertEquals(
                List.of("durationMinutes", "vehicles", "settings.hideNametags", "settings.lockDurationMinutes"), keys);
    }

    @Test
    void liveKeysAreNotReported() {
        assertTrue(MapConfigLegacy.ignoredKeys(
                        List.of("name", "capturePoints", "mainZones", "settings"),
                        List.of("respawnTimer", "deathTicketCost"))
                .isEmpty());
    }

    @Test
    void nullSectionsAreSafe() {
        assertTrue(MapConfigLegacy.ignoredKeys(null, null).isEmpty());
        assertEquals(List.of("durationMinutes"), MapConfigLegacy.ignoredKeys(List.of("durationMinutes"), null));
    }

    @Test
    void warningNamesKeysAndMatchConfig() {
        String w = MapConfigLegacy.warning("takmachka", List.of("durationMinutes", "settings.hideNametags"));
        assertTrue(w.contains("durationMinutes, settings.hideNametags"));
        assertTrue(w.contains("match_config.json"));
    }

    @Test
    void matchConfigDefaultsHideDeathMessages() {
        // showDeathMessages=false по умолчанию — сообщений о смерти нет (gamerule выкл, своё добивание тоже)
        assertEquals(false, new MatchConfig().rules.showDeathMessages);
    }
}
