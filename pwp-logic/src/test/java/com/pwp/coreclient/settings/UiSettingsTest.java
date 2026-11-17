package com.pwp.coreclient.settings;

import static org.junit.jupiter.api.Assertions.*;

import com.pwp.coreclient.ui.UIAnim;
import com.pwp.coreclient.ui.UIMath;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UiSettingsTest {
    enum Mode {
        HOLD,
        ALWAYS,
        NEVER
    }

    private final List<String> warnings = new ArrayList<>();
    private UiSettings.Category cat;

    @BeforeEach
    void setUp() {
        UiSettings.clearForTests();
        UiSettings.setWarnSink(warnings::add);
        cat = UiSettings.category("t", "t.title", 5);
    }

    @AfterEach
    void tearDown() {
        UiSettings.setWarnSink(null);
        UIAnim.setAnimationsEnabled(true);
    }

    @Test
    void snapClampsAndRoundsToStep() {
        assertEquals(0.35F, UiSettings.snap(0.3449F, 0F, 1F, 0.05F), 1e-6);
        assertEquals(0F, UiSettings.snap(-3F, 0F, 1F, 0.05F));
        assertEquals(1F, UiSettings.snap(7F, 0F, 1F, 0.05F));
        assertEquals(350F, UiSettings.snap(361F, 0F, 1500F, 50F));
        // шаг, не делящий диапазон: max не превышается
        assertEquals(1F, UiSettings.snap(0.99F, 0F, 1F, 0.3F), 1e-6);
        assertEquals(0.9F, UiSettings.snap(0.91F, 0F, 1F, 0.3F), 1e-6);
    }

    @Test
    void fractionRoundTrip() {
        assertEquals(0.5F, UiSettings.toFraction(0.6F, 0.2F, 1F), 1e-6);
        assertEquals(0.6F, UiSettings.fromFraction(0.5F, 0.2F, 1F), 1e-6);
        assertEquals(0F, UiSettings.toFraction(-1F, 0F, 1F));
        assertEquals(1F, UiSettings.fromFraction(2F, 0F, 1F));
    }

    @Test
    void sliderSetSnapsAndNotifies() {
        var s = UiSettings.slider("t.s", cat, "l", 0.2F, 1F, 0.05F, 1F, UiSettings.Format.PERCENT);
        AtomicInteger calls = new AtomicInteger();
        s.onChange(calls::incrementAndGet); // сразу 1 вызов
        s.set(0.512F);
        assertEquals(0.5F, s.get(), 1e-6);
        s.set(0.5F); // то же значение — без уведомления
        s.set(Float.NaN); // игнор
        assertEquals(2, calls.get());
        s.setFraction(0F);
        assertEquals(0.2F, s.get(), 1e-6);
        s.nudge(2);
        assertEquals(0.3F, s.get(), 1e-6);
        assertTrue(UiSettings.isDirty());
        s.reset();
        assertTrue(s.isDefault());
    }

    @Test
    void formats() {
        assertEquals("75%", UiSettings.Format.PERCENT.number(0.75F));
        assertEquals("350", UiSettings.Format.MILLIS.number(350F));
        assertEquals("3.5", UiSettings.Format.SECONDS.number(3.5F));
        assertEquals("1.25×", UiSettings.Format.MULTIPLIER.number(1.25F));
        assertEquals("2×", UiSettings.Format.MULTIPLIER.number(2F));
        assertNotNull(UiSettings.Format.SECONDS.unitKey());
        assertNull(UiSettings.Format.PERCENT.unitKey());
    }

    @Test
    void choiceCyclesAndEncodesById() {
        var c = UiSettings.choice("t.c", cat, "l", Mode.class, Mode.HOLD, "p.");
        c.cycle(-1);
        assertEquals(Mode.NEVER, c.get());
        c.cycle(1);
        assertEquals(Mode.HOLD, c.get());
        assertEquals("p.always", c.optionLabelKey(Mode.ALWAYS));
        c.set(Mode.ALWAYS);
        assertEquals("always", UiSettings.snapshot().get("t.c"));
    }

    @Test
    void loadAppliesValidAndDefaultsBroken() {
        var s = UiSettings.slider("t.s", cat, "l", 0F, 1F, 0.05F, 1F, UiSettings.Format.PERCENT);
        var t = UiSettings.toggle("t.t", cat, "l", true);
        var c = UiSettings.choice("t.c", cat, "l", List.of("a", "b"), "a", "p.");
        s.set(0.5F);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("t.s", "оops"); // не число — дефолт
        raw.put("t.t", false);
        raw.put("t.c", "zzz"); // нет такого варианта — дефолт
        UiSettings.load(raw);
        assertEquals(1F, s.get());
        assertFalse(t.get());
        assertEquals("a", c.get());
        assertEquals(2, warnings.size());
        assertFalse(UiSettings.isDirty());

        raw.clear();
        raw.put("t.s", 5.0); // вне диапазона — дефолт
        raw.put("t.c", "b");
        UiSettings.load(raw);
        assertEquals(1F, s.get());
        assertEquals("b", c.get());
        raw.put("t.s", Double.NaN);
        UiSettings.load(raw);
        assertEquals(4, warnings.size());
    }

    @Test
    void unknownIdsArePreservedAndAppliedOnLateRegistration() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("later.x", 0.25);
        raw.put("gone.mod", "keep");
        UiSettings.load(raw);
        var late = UiSettings.slider("later.x", cat, "l", 0F, 1F, 0.05F, 1F, UiSettings.Format.PERCENT);
        assertEquals(0.25F, late.get(), 1e-6);
        Map<String, Object> out = UiSettings.snapshot();
        assertEquals("keep", out.get("gone.mod"));
        assertEquals(0.25, (Double) out.get("later.x"), 1e-6);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void registryRulesAndCategoryOrder() {
        UiSettings.toggle("t.a", cat, "l", true);
        assertThrows(IllegalArgumentException.class, () -> UiSettings.toggle("t.a", cat, "l", true));
        var other = UiSettings.category("o", "o.title", 1);
        UiSettings.toggle("o.a", other, "l", false);
        UiSettings.category("empty", "e.title", -5); // без настроек — не показывается
        assertEquals(List.of(other, cat), UiSettings.categories());
        assertThrows(
                IllegalArgumentException.class,
                () -> UiSettings.toggle("x", new UiSettings.Category("nope", "n", 0), "l", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> UiSettings.slider("bad", cat, "l", 1F, 0F, 0.1F, 0.5F, UiSettings.Format.PERCENT));
        UiSettings.find("o.a");
        ((UiSettings.Toggle) UiSettings.find("o.a")).set(true);
        UiSettings.resetCategory(other);
        assertFalse(((UiSettings.Toggle) UiSettings.find("o.a")).get());
    }

    @Test
    void animationsOffMakesTransitionsInstant() {
        AtomicLong clock = new AtomicLong(1000);
        UIAnim a = new UIAnim(0F, 200, UIAnim.LINEAR, clock::get);
        a.to(1F);
        assertEquals(0F, a.value(), 1e-6);
        UIAnim.setAnimationsEnabled(false);
        assertEquals(1F, a.value(), 1e-6);
        assertTrue(a.done());
        assertEquals(1F, UIMath.stagger(0, 3, 300, 50, 260));
        UIAnim.setAnimationsEnabled(true);
        assertEquals(0F, UIMath.stagger(0, 3, 300, 50, 260));
    }
}
