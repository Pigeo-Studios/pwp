package com.pwp.api.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChatFilterTest {
    private final ChatFilter f = ChatFilter.defaults();

    @Test
    void masksSwearWordKeepingLengthAndRest() {
        ChatFilter.Result r = f.apply("ну ты сука, иди сюда");
        assertTrue(r.masked());
        assertEquals("ну ты ****, иди сюда", r.text());
    }

    @Test
    void catchesDisguisedSpelling() {
        assertTrue(f.apply("xyй").masked());
        assertTrue(f.apply("Х.У.Й").masked());
        assertTrue(f.apply("пиииздец").masked());
        assertTrue(f.apply("cyka").masked());
        assertTrue(f.apply("FUCK this").masked());
        assertTrue(f.apply("заебали").masked());
    }

    @Test
    void ordinaryWordsWithBadSubstringsStayClean() {
        for (String ok : List.of(
                "оскорблять",
                "употреблять",
                "застрахуем",
                "хлебала",
                "жидкость",
                "небо",
                "сукно",
                "психология",
                "блюдо",
                "колебания",
                "команда держит точку B",
                "")) {
            ChatFilter.Result r = f.apply(ok);
            assertFalse(r.masked(), ok);
            assertEquals(ok, r.text());
        }
    }

    @Test
    void customRulesAndComments() {
        ChatFilter c = new ChatFilter(List.of("# comment", "", "=лох", "^тупо", "дурак"));
        assertEquals(3, c.size());
        assertTrue(c.apply("лох").masked());
        assertFalse(c.apply("лохматый").masked());
        assertTrue(c.apply("тупица").masked() == false);
        assertTrue(c.apply("тупорылый").masked());
        assertTrue(c.apply("ну и прдурак").masked());
    }

    @Test
    void emptyRulesPassEverything() {
        ChatFilter c = new ChatFilter(List.of());
        assertFalse(c.apply("сука").masked());
    }

    @Test
    void rateLimitsSpamAndRepeats() {
        ChatRate r = new ChatRate();
        assertNull(r.check("a", "привет", 0, 3));
        assertEquals(ChatRate.REPEAT, r.check("a", "ПРИВЕТ!!", 1000, 3));
        assertNull(r.check("a", "раз", 1100, 3));
        assertNull(r.check("a", "два", 1200, 3));
        assertEquals(ChatRate.SPAM, r.check("a", "три", 1300, 3));
        // окно прошло — снова можно; другой игрок независим
        assertNull(r.check("a", "четыре", 6000, 3));
        assertNull(r.check("b", "привет", 1000, 3));
        // повтор после 30 с — можно
        assertNull(r.check("b", "привет", 1000 + ChatRate.REPEAT_MS, 3));
        // 0 — без лимита частоты
        for (int i = 0; i < 20; i++) assertNull(r.check("c", "m" + (char) ('a' + i), i, 0));
    }
}
