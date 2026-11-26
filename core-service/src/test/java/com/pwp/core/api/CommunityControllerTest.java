package com.pwp.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.core.auth.AccessPolicy;
import com.pwp.core.auth.LinkCodes;
import com.pwp.core.backup.BackupPlan;
import io.javalin.http.BadRequestResponse;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommunityControllerTest {
    static final String A = "0f8fad5b-d9cb-469f-a165-70867728950e";
    static final String B = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void reportIsValidatedAndCleaned() {
        var r = CommunityController.parseReport(json("{\"server\":\"match_01\",\"reporterUuid\":\"" + A
                + "\",\"reporterName\":\"Steve\",\"targetUuid\":\"" + B
                + "\",\"targetName\":\"Griefer_1\",\"reason\":\"TEAMKILL\",\"details\":\"line\\u0007two\"}"));
        assertEquals("teamkill", r.reason);
        assertEquals("line two", r.details);
        assertThrows(BadRequestResponse.class, () -> CommunityController.parseReport(json("{\"reporterUuid\":\"" + A
                + "\",\"reporterName\":\"Steve\",\"targetUuid\":\"" + A + "\",\"targetName\":\"Steve\",\"reason\":\"grief\"}")));
        assertThrows(BadRequestResponse.class, () -> CommunityController.parseReport(json("{\"reporterUuid\":\"" + A
                + "\",\"reporterName\":\"bad name!\",\"targetUuid\":\"" + B + "\",\"targetName\":\"x\",\"reason\":\"grief\"}")));
        assertThrows(BadRequestResponse.class, () -> CommunityController.parseReport(json("{\"reporterUuid\":\"" + A
                + "\",\"reporterName\":\"Steve\",\"targetUuid\":\"" + B + "\",\"targetName\":\"x\",\"reason\":\"drop table\"}")));
    }

    @Test
    void writesAreServerOnly() {
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v1/reports"));
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v1/punishments"));
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v1/discord/code"));
        assertTrue(AccessPolicy.serverOnly("POST", "/api/v1/discord/link"));
        assertFalse(AccessPolicy.serverOnly("GET", "/api/v1/discord/by-uuid/" + A));
    }

    @Test
    void linkCodesAreOneTimeAndExpire() {
        LinkCodes c = new LinkCodes(new SecureRandom());
        String code = c.issue(A, 0);
        assertEquals(6, code.length());
        assertEquals(A, c.redeem(code, 1000));
        assertNull(c.redeem(code, 1000), "одноразовый");
        String late = c.issue(B, 0);
        assertNull(c.redeem(late, LinkCodes.TTL_MS + 1), "истёк");
        // новый код игрока заменяет старый
        String first = c.issue(A, 0);
        String second = c.issue(A, 0);
        assertEquals(1, c.size(0));
        if (!first.equals(second)) assertNull(c.redeem(first, 0));
        assertEquals(A, c.redeem(second, 0));
        assertNull(c.redeem(null, 0));
    }

    @Test
    void backupScheduleAndRotation() {
        String db = "pwp_core";
        LocalDateTime night = LocalDateTime.of(2026, 11, 26, 4, 10);
        assertEquals("pwp_core-20261126-0410.sql.gz", BackupPlan.fileName(db, night));
        assertEquals(LocalDate.of(2026, 11, 26), BackupPlan.dateOf(db, "pwp_core-20261126-0410.sql.gz"));
        assertNull(BackupPlan.dateOf(db, "other-20261126-0410.sql.gz"));
        assertNull(BackupPlan.dateOf(db, "pwp_core-2026-11-26.sql"));
        assertFalse(BackupPlan.isDue(night.withHour(3), 4, List.of(), db), "рано");
        assertTrue(BackupPlan.isDue(night, 4, List.of("pwp_core-20261125-0400.sql.gz"), db));
        assertFalse(BackupPlan.isDue(night, 4, List.of("pwp_core-20261126-0400.sql.gz"), db), "уже есть");
        assertTrue(BackupPlan.isDue(night.withHour(23), 4, List.of(), db), "пропустили 4:00 — догоняем");

        List<String> files = new ArrayList<>();
        for (int d = 1; d <= 20; d++) files.add(String.format("pwp_core-202611%02d-0400.sql.gz", d));
        files.add("notes.txt");
        List<String> del = BackupPlan.toDelete(files, db, LocalDate.of(2026, 11, 20), 14, 3);
        assertEquals(5, del.size()); // 1..5 ноября старше 14 дней (граница — 6-е)
        assertTrue(del.contains("pwp_core-20261101-0400.sql.gz"));
        assertFalse(del.contains("pwp_core-20261106-0400.sql.gz"));
        // сервер лежал месяц: старые, но последние 3 остаются
        List<String> old = List.of("pwp_core-20260101-0400.sql.gz", "pwp_core-20260102-0400.sql.gz",
                "pwp_core-20260103-0400.sql.gz", "pwp_core-20260104-0400.sql.gz");
        List<String> del2 = BackupPlan.toDelete(old, db, LocalDate.of(2026, 11, 20), 14, 3);
        assertEquals(List.of("pwp_core-20260101-0400.sql.gz"), del2);
        assertNotEquals(0, old.size());
    }
}
