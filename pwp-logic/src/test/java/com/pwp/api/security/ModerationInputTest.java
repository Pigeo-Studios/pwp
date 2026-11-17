package com.pwp.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModerationInputTest {
    @Test
    void names() {
        assertTrue(ModerationInput.validName("Steve_01"));
        assertFalse(ModerationInput.validName("bad name"));
        assertFalse(ModerationInput.validName("@a"));
        assertFalse(ModerationInput.validName("x".repeat(17)));
        assertFalse(ModerationInput.validName(""));
    }

    @Test
    void durations() {
        assertTrue(ModerationInput.validDuration("30m"));
        assertTrue(ModerationInput.validDuration("7d"));
        assertTrue(ModerationInput.validDuration("perm"));
        assertFalse(ModerationInput.validDuration("0m"));
        assertFalse(ModerationInput.validDuration("5y"));
        assertFalse(ModerationInput.validDuration("1h; op me"));
    }

    @Test
    void textCannotInjectCommands() {
        assertEquals("a b", ModerationInput.cleanText("a\n\nb", "-"));
        assertEquals("red", ModerationInput.cleanText("§cred", "-"));
        assertEquals("-", ModerationInput.cleanText("   ", "-"));
        assertEquals(
                ModerationInput.TEXT_MAX,
                ModerationInput.cleanText("x".repeat(500), "-").length());
    }

    @Test
    void commands() {
        assertEquals("kick Steve griefing", ModerationInput.kick("Steve", "griefing"));
        assertEquals("pwpban Steve 1d -", ModerationInput.ban("Steve", "1d", ""));
        assertEquals("pwpwarfare teamjoin blue Steve", ModerationInput.teamJoin("Steve", "BLUE"));
        assertEquals("pwpwarfare voicemute Steve 15 spam", ModerationInput.voiceMute("Steve", 15, "spam"));
        assertEquals("pwpwarfare cleartenkill Steve", ModerationInput.clearTeamkill("Steve"));
        assertEquals("pwpwarfare warn Steve stop", ModerationInput.warn("Steve", "stop"));
    }
}
