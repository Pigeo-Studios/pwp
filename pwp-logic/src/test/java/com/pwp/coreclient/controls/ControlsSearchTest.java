package com.pwp.coreclient.controls;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ControlsSearchTest {
    static ControlsRow row(String id, String label, String cat, String key, String mod) {
        return new ControlsRow(id, label, mod, "c", cat, key, true, false, null);
    }

    static final List<ControlsGroup> GROUPS = List.of(
            new ControlsGroup(
                    new ControlsCatalog.Category("move", "k", 10),
                    "Движение",
                    List.of(
                            row("key.forward", "Вперёд", "Движение", "W", "Minecraft"),
                            row("key.lean", "Наклон влево", "Движение", "Q", "Tactical Movement"))),
            new ControlsGroup(
                    new ControlsCatalog.Category("voice", "k", 20),
                    "Голос",
                    List.of(
                            row("key.ptt", "Push to talk", "Голос", "Caps Lock", "Simple Voice Chat"),
                            row("key.uk", "Ґрати на рації", "Голос", "G", "PWP"))));

    static String ranges(List<int[]> r) {
        StringBuilder sb = new StringBuilder();
        for (int[] a : r) sb.append('[').append(a[0]).append(',').append(a[1]).append(')');
        return sb.toString();
    }

    @Test
    void emptyQueryReturnsAll() {
        assertSame(GROUPS, ControlsSearch.filter(GROUPS, ""));
        assertSame(GROUPS, ControlsSearch.filter(GROUPS, "   "));
        assertSame(GROUPS, ControlsSearch.filter(GROUPS, null));
        assertEquals(List.of(), ControlsSearch.highlight("Вперёд", ""));
    }

    @Test
    void russianCaseInsensitiveAndYo() {
        List<ControlsGroup> r = ControlsSearch.filter(GROUPS, "ВПЕРЕД");
        assertEquals(1, r.size());
        assertEquals("key.forward", r.get(0).rows().get(0).id());
        assertEquals("[0,6)", ranges(ControlsSearch.highlight("Вперёд", "вперед")));
    }

    @Test
    void englishAndUkrainian() {
        assertEquals(
                "key.ptt",
                ControlsSearch.filter(GROUPS, "TALK").get(0).rows().get(0).id());
        assertEquals(
                "key.uk",
                ControlsSearch.filter(GROUPS, "ґрати").get(0).rows().get(0).id());
        assertEquals("[0,2)", ranges(ControlsSearch.highlight("Ґрати на рації", "ҐР")));
    }

    @Test
    void matchesByCategoryKeyAndMod() {
        // по категории — вся группа
        assertEquals(2, ControlsSearch.filter(GROUPS, "голос").get(0).rows().size());
        // по имени клавиши
        assertEquals(
                "key.ptt",
                ControlsSearch.filter(GROUPS, "caps").get(0).rows().get(0).id());
        // по моду
        assertEquals(
                "key.lean",
                ControlsSearch.filter(GROUPS, "tactical").get(0).rows().get(0).id());
        // все слова должны найтись (в разных полях)
        assertEquals(
                "key.lean",
                ControlsSearch.filter(GROUPS, "наклон q").get(0).rows().get(0).id());
        assertTrue(ControlsSearch.filter(GROUPS, "наклон w").isEmpty());
    }

    @Test
    void noMatches() {
        assertTrue(ControlsSearch.filter(GROUPS, "zzz").isEmpty());
        assertEquals(List.of(), ControlsSearch.highlight("Вперёд", "zzz"));
    }

    @Test
    void multipleOccurrencesAndMerge() {
        assertEquals("[0,2)[3,5)[9,11)", ranges(ControlsSearch.highlight("на на ко на", "на")));
        // непересекающиеся вхождения
        assertEquals("[0,4)", ranges(ControlsSearch.highlight("aaaa", "aa")));
        // слова сливаются в один диапазон
        assertEquals("[0,6)", ranges(ControlsSearch.highlight("Наклон влево", "накл лон")));
        assertEquals("[0,6)[7,12)", ranges(ControlsSearch.highlight("Наклон влево", "влево наклон")));
    }

    @Test
    void foldKeepsLength() {
        String s = "İStanbul ЁЖ Ґ";
        assertEquals(s.length(), ControlsSearch.fold(s).length());
    }
}
