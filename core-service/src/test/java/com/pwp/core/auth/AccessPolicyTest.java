package com.pwp.core.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccessPolicyTest {
    static final String ME = "0f8fad5b-d9cb-469f-a165-70867728950e";
    static final String OTHER = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    @Test
    void economyAndModerationAreServerOnly() {
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v1/currency/add"));
        assertTrue(AccessPolicy.serverOnly("post", "/api/v1/xp/add"));
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v2/skins/grant"));
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v1/match/save"));
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v1/player/ban"));
        assertTrue(AccessPolicy.serverOnly("PUT", "/api/v1/kits/faction/usa/Rifleman"));
        assertTrue(AccessPolicy.serverOnly("DELETE", "/api/v1/factions/usa/vehicles/m1a2"));
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v1/maps/takmachka/layouts"));
        assertTrue(AccessPolicy.serverOnly("PUT", "/api/v1/factions/usa/divisions/mech"));
        assertTrue(AccessPolicy.serverOnly("DELETE", "/api/v1/factions/usa/divisions/mech"));
        assertFalse(AccessPolicy.serverOnly("GET", "/api/v1/factions/usa/divisions"));
        assertEquals("server only", AccessPolicy.denyReason("POST", "/api/v1/currency/add", "{}", ME));
    }

    @Test
    void readsAndOwnActionsAreAllowed() {
        assertFalse(AccessPolicy.serverOnly("GET", "/api/v1/kits/faction/usa/Rifleman"));
        assertFalse(AccessPolicy.serverOnly("POST", "/api/v1/currency/spend"));
        assertNull(AccessPolicy.denyReason("GET", "/api/v1/currency/" + OTHER, null, ME));
        assertNull(AccessPolicy.denyReason("POST", "/api/v1/cosmetics/equip", "{\"uuid\":\"" + ME + "\"}", ME));
        // uuid без дефисов и в другом регистре — тот же игрок
        assertNull(AccessPolicy.denyReason(
                "POST", "/api/v1/cases/open", "{\"uuid\":\"" + ME.replace("-", "").toUpperCase() + "\"}", ME));
        assertNull(AccessPolicy.denyReason("POST", "/api/v1/auth/logout", "{\"accessToken\":\"x\"}", ME));
        assertNull(AccessPolicy.denyReason("POST", "/api/v1/auth/logout", "not json", ME));
    }

    @Test
    void foreignUuidIsDenied() {
        assertEquals("foreign uuid",
                AccessPolicy.denyReason("POST", "/api/v1/auth/toggle-2fa", "{\"uuid\":\"" + OTHER + "\"}", ME));
        assertEquals("foreign uuid",
                AccessPolicy.denyReason("POST", "/api/v1/currency/spend", "{\"playerUuid\":\"" + OTHER + "\"}", ME));
        assertEquals("foreign uuid", AccessPolicy.denyReason("DELETE", "/api/v1/things/" + OTHER, null, ME));
        // сессия без игрока не может менять чужое
        assertEquals("foreign uuid",
                AccessPolicy.denyReason("POST", "/api/v1/cosmetics/equip", "{\"uuid\":\"" + ME + "\"}", null));
    }
}
