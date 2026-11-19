package com.pwp.api.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdminLogicTest {

    @Test
    void auditRingKeepsLastAndNewestFirst() {
        AuditTrail t = new AuditTrail(3);
        for (int i = 0; i < 5; i++) t.add(i, "Max", "ADMIN", "a" + i, "", AuditTrail.Result.OK, null);
        List<AuditTrail.Entry> r = t.recent(10);
        assertEquals(3, r.size());
        assertEquals("a4", r.get(0).action());
        assertEquals("a2", r.get(2).action());
        assertEquals(2, t.recent(2).size());
    }

    @Test
    void auditFieldsAreSingleLineAndCapped() {
        AuditTrail t = new AuditTrail(2);
        AuditTrail.Entry e =
                t.add(0, "Max\nEvil | x", "ADMIN", "mod.kick", "x".repeat(500), AuditTrail.Result.DENIED, "");
        assertFalse(e.actor().contains("\n"));
        assertFalse(e.actor().contains("|"));
        assertTrue(e.details().length() <= AuditTrail.FIELD_MAX + 1);
        String line = AuditTrail.format(e);
        assertFalse(line.contains("\n"));
        assertTrue(line.contains("| mod.kick |"));
        assertTrue(line.endsWith("DENIED"));
    }

    @Test
    void auditFormatShowsErrorAndDash() {
        AuditTrail.Entry e =
                new AuditTrail(1).add(0, "A", "OWNER", "match.stop", "", AuditTrail.Result.FAILED, "err.key");
        String line = AuditTrail.format(e);
        assertTrue(line.contains("| - |"));
        assertTrue(line.endsWith("FAILED err.key"));
    }

    @Test
    void scrubMasksSecrets() {
        assertEquals("apiKey=***", LogScrub.scrub("apiKey=abc123"));
        assertEquals("\"password\": \"***\"", LogScrub.scrub("\"password\": \"hunter2\""));
        String auth = LogScrub.scrub("Authorization: Bearer eyJhbGciOi.x.y");
        assertFalse(auth.contains("eyJ"), auth);
        assertTrue(auth.startsWith("Authorization: ***"), auth);
        assertTrue(LogScrub.scrub("X-API-Key: SECRETVALUE").contains("***"));
        assertFalse(LogScrub.scrub("token=zzz").contains("zzz"));
    }

    @Test
    void scrubMasksIpsButKeepsLoopbackAndBlobs() {
        assertEquals("player from x.x.x.x joined", LogScrub.scrub("player from 93.12.4.7 joined"));
        assertEquals("core http://127.0.0.1:8080 down", LogScrub.scrub("core http://127.0.0.1:8080 down"));
        assertEquals("sig ***", LogScrub.scrub("sig " + "a1B2".repeat(10)));
        assertEquals("Can't keep up! 2 ticks", LogScrub.scrub("Can't keep up! 2 ticks"));
    }

    @Test
    void scrubCapsLengthAndFlattens() {
        String s = LogScrub.scrub("a b\nc " + "word ".repeat(200));
        assertFalse(s.contains("\n"));
        assertTrue(s.length() <= LogScrub.MAX_LEN + 1);
        assertEquals("", LogScrub.scrub(null));
    }

    @Test
    void lineRingCollapsesRepeats() {
        LineRing r = new LineRing(3);
        r.add(1, "WARN", "x", "same");
        r.add(2, "WARN", "x", "same");
        r.add(3, "ERROR", "x", "boom");
        r.add(4, "WARN", "x", "same");
        List<LineRing.Line> l = r.recent(10);
        assertEquals(3, l.size());
        assertEquals("same", l.get(0).text());
        assertEquals(1, l.get(0).repeats());
        assertEquals(2, l.get(2).repeats());
        for (int i = 0; i < 10; i++) r.add(10 + i, "WARN", "x", "n" + i);
        assertEquals(3, r.recent(10).size());
        assertEquals("n9", r.recent(1).get(0).text());
    }
}
