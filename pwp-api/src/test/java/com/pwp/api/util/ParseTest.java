package com.pwp.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParseTest {
    @Test
    void intOrParsesValidNumbers() {
        assertEquals(42, Parse.intOr("42", -1));
        assertEquals(-7, Parse.intOr(" -7 ", 0));
        assertEquals(5, Parse.intOr("+5", 0));
    }

    @Test
    void intOrFallsBackOnIntermediateInput() {
        // Промежуточный ввод в поле — не ошибка, а значение по умолчанию
        assertEquals(3, Parse.intOr("", 3));
        assertEquals(3, Parse.intOr("-", 3));
        assertEquals(3, Parse.intOr(null, 3));
        assertEquals(3, Parse.intOr("1.5", 3));
        assertEquals(3, Parse.intOr("99999999999", 3)); // переполнение int
    }

    @Test
    void floatAndDouble() {
        assertEquals(1.5f, Parse.floatOr("1.5", 0f));
        assertEquals(-90f, Parse.floatOr("-90", 0f));
        assertEquals(0.25, Parse.doubleOr(".25", 0));
        assertEquals(7.0, Parse.doubleOr("abc", 7.0));
        assertEquals(7.0, Parse.doubleOr("NaN", 7.0));
        assertEquals(7.0, Parse.doubleOr("1e999", 7.0)); // бесконечность
    }

    @Test
    void urlsEncodePathAndQuery() {
        assertEquals("Rifleman%20%28Optic%29", Urls.enc("Rifleman (Optic)"));
        assertEquals("kills%26page%3D9", Urls.enc("kills&page=9")); // инъекция параметров
        assertEquals("", Urls.enc(null));
    }
}
