package com.pwp.coreclient.controls;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Логика экрана «Управление» (этап 3): ожидание клавиши, владелец, плоский список. */
class ControlsScreenLogicTest {
    static final int R = 82, LCTRL = 341, LSHIFT = 340;

    // ---- KeyCapture ----

    @Test
    void escCancelsAndStops() {
        KeyCapture c = new KeyCapture();
        c.start("key.forward");
        assertInstanceOf(KeyCapture.Cancel.class, c.press(KeyInput.key(256), false, KeyInput.Modifier.NONE));
        assertFalse(c.active());
    }

    @Test
    void backspaceAndDeleteUnbindOnlyWithoutModifier() {
        KeyCapture c = new KeyCapture();
        c.start("a");
        assertInstanceOf(KeyCapture.Unbind.class, c.press(KeyInput.key(259), false, KeyInput.Modifier.NONE));
        c.start("a");
        assertInstanceOf(KeyCapture.Unbind.class, c.press(KeyInput.key(261), false, null));
        c.start("a");
        var r = c.press(KeyInput.key(259), false, KeyInput.Modifier.CONTROL);
        assertEquals(new KeyCapture.Bind(KeyInput.key(259, KeyInput.Modifier.CONTROL)), r);
    }

    @Test
    void modifierThenKeyBindsCombo() {
        KeyCapture c = new KeyCapture();
        c.start("unload");
        assertInstanceOf(KeyCapture.Waiting.class, c.press(KeyInput.key(LCTRL), true, KeyInput.Modifier.CONTROL));
        assertTrue(c.active());
        assertEquals(KeyInput.key(LCTRL), c.pendingModifier());
        var r = c.press(KeyInput.key(R), false, KeyInput.Modifier.CONTROL);
        assertEquals(new KeyCapture.Bind(KeyInput.key(R, KeyInput.Modifier.CONTROL)), r);
        assertFalse(c.active());
        // отпускание после назначения ничего не делает
        assertInstanceOf(KeyCapture.Waiting.class, c.release(KeyInput.key(LCTRL)));
    }

    @Test
    void modifierAloneBindsOnRelease() {
        KeyCapture c = new KeyCapture();
        c.start("sprint");
        c.press(KeyInput.key(LCTRL), true, KeyInput.Modifier.CONTROL);
        // отпускание другой клавиши — ждём дальше
        assertInstanceOf(KeyCapture.Waiting.class, c.release(KeyInput.key(LSHIFT)));
        assertEquals(new KeyCapture.Bind(KeyInput.key(LCTRL)), c.release(KeyInput.key(LCTRL)));
    }

    @Test
    void mouseWithModifierAndInactiveIgnored() {
        KeyCapture c = new KeyCapture();
        assertInstanceOf(KeyCapture.Waiting.class, c.mouse(0, KeyInput.Modifier.NONE));
        c.start("aim");
        assertEquals(
                new KeyCapture.Bind(new KeyInput(KeyInput.Kind.MOUSE, 1, KeyInput.Modifier.SHIFT)),
                c.mouse(1, KeyInput.Modifier.SHIFT));
        // кнопка мыши с номером 256 — не Esc
        c.start("x");
        assertInstanceOf(KeyCapture.Bind.class, c.press(KeyInput.mouse(256), false, null));
    }

    // ---- KeyOwner ----

    @Test
    void ownerFromNameThenCategoryThenVanilla() {
        Set<String> mods = Set.of("minecraft", "tacz", "iris", "voicechat", "dragonrise_reforge");
        assertEquals("tacz", KeyOwner.modId("key.tacz.aim.desc", "key.categories.tacz", mods::contains));
        assertEquals("iris", KeyOwner.modId("iris.keybind.toggleShaders", "iris.keybinds", mods::contains));
        assertEquals("voicechat", KeyOwner.modId("key.push_to_talk", "key.categories.voicechat", mods::contains));
        assertEquals(
                "dragonrise_reforge",
                KeyOwner.modId("dragonrise_reforge.keyinfo.enginechangekey", "x", mods::contains));
        assertEquals("minecraft", KeyOwner.modId("key.forward", "key.categories.movement", mods::contains));
        // мода нет (удалён из сборки) и категория не ванильная
        assertNull(KeyOwner.modId("key.gone.thing", "key.categories.gone", mods::contains));
        assertEquals(
                "Fallback",
                KeyOwner.display("key.gone.thing", "key.categories.gone", mods::contains, id -> "X", "Fallback"));
        assertEquals("TaCZ", KeyOwner.display("key.tacz.aim.desc", "c", mods::contains, id -> "TaCZ", "f"));
        assertEquals("tacz", KeyOwner.display("key.tacz.aim.desc", "c", mods::contains, id -> "", "f"));
    }

    // ---- ControlsList ----

    static ControlsRow row(String id, boolean adv) {
        return new ControlsRow(id, id, "m", "c", "Cat", "K", true, adv, null);
    }

    static ControlsGroup group(String id, ControlsRow... rows) {
        return new ControlsGroup(new ControlsCatalog.Category(id, id, 0), id, List.of(rows));
    }

    @Test
    void flattenCollapsedExpandedAndSearching() {
        List<ControlsGroup> gs =
                List.of(group("move", row("a", false), row("b", true), row("c", true)), group("ui", row("d", false)));
        List<ControlsList.Item> closed = ControlsList.flatten(gs, false, Set.of());
        assertEquals(
                List.of(
                        new ControlsList.Header(gs.get(0), 3),
                        new ControlsList.Row(gs.get(0).rows().get(0)),
                        new ControlsList.AdvancedToggle("move", 2, false),
                        new ControlsList.Header(gs.get(1), 1),
                        new ControlsList.Row(gs.get(1).rows().get(0))),
                closed);
        List<ControlsList.Item> open = ControlsList.flatten(gs, false, Set.of("move"));
        assertEquals(7, open.size());
        assertEquals(new ControlsList.AdvancedToggle("move", 2, true), open.get(2));
        List<ControlsList.Item> search = ControlsList.flatten(gs, true, Set.of());
        assertEquals(6, search.size());
        assertTrue(search.stream().noneMatch(i -> i instanceof ControlsList.AdvancedToggle));
        assertEquals(4, ControlsList.rowCount(gs));
        assertTrue(ControlsList.flatten(List.of(), false, null).isEmpty());
    }
}
