package com.pwp.coreclient.controls;

import static com.pwp.coreclient.controls.ControlsConflicts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ControlsConflictsTest {
    static final int K = 75, R = 82, W = 87, LCTRL = 341;

    static KeyInfo key(String id, KeyInput in, String ctx) {
        return new KeyInfo(id, id, "m", "", true, in, ctx);
    }

    @Test
    void sameKeySameContextConflicts() {
        List<KeyInfo> all = List.of(
                key("key.pwpwarfare.open_squad_menu", KeyInput.key(K), IN_GAME),
                key("key.superbwarfare.active_thermal_imaging", KeyInput.key(K), IN_GAME),
                key("key.forward", KeyInput.key(W), IN_GAME));
        Map<String, List<String>> c = find(all, FORGE);
        assertEquals(
                List.of("key.pwpwarfare.open_squad_menu", "key.superbwarfare.active_thermal_imaging"),
                List.copyOf(c.keySet()));
        assertEquals(List.of("key.superbwarfare.active_thermal_imaging"), c.get("key.pwpwarfare.open_squad_menu"));
    }

    @Test
    void conflictWithInvisibleKey() {
        // Iris K вне белого списка, UNIVERSAL — видимая строка K всё равно краснеет
        KeyInfo squad = key("key.pwpwarfare.open_squad_menu", KeyInput.key(K), IN_GAME);
        KeyInfo iris = key("iris.keybind.toggleShaders", KeyInput.key(K), UNIVERSAL);
        assertEquals(List.of("iris.keybind.toggleShaders"), of(squad, List.of(squad, iris), FORGE));
    }

    @Test
    void unboundNeverConflicts() {
        KeyInfo a = key("a", KeyInput.UNBOUND, IN_GAME);
        KeyInfo b = key("b", KeyInput.UNBOUND, IN_GAME);
        assertTrue(find(List.of(a, b), FORGE).isEmpty());
    }

    @Test
    void contextsMustOverlap() {
        KeyInfo game = key("a", KeyInput.key(K), IN_GAME);
        KeyInfo gui = key("b", KeyInput.key(K), GUI);
        assertFalse(conflicts(game, gui, FORGE));
        // свой предикат: всё пересекается
        assertTrue(conflicts(game, gui, (x, y) -> true));
    }

    @Test
    void inputKindMatters() {
        KeyInfo kb = key("a", new KeyInput(KeyInput.Kind.KEYSYM, 1, KeyInput.Modifier.NONE), IN_GAME);
        KeyInfo mouse = key("b", KeyInput.mouse(1), IN_GAME);
        assertFalse(conflicts(kb, mouse, FORGE));
        assertTrue(conflicts(mouse, key("c", KeyInput.mouse(1), IN_GAME), FORGE));
    }

    @Test
    void modifiers() {
        KeyInfo reload = key("reload", KeyInput.key(R), IN_GAME);
        KeyInfo unload = key("unload", KeyInput.key(R, KeyInput.Modifier.CONTROL), IN_GAME);
        KeyInfo altR = key("altR", KeyInput.key(R, KeyInput.Modifier.ALT), IN_GAME);
        KeyInfo sprint = key("sprint", KeyInput.key(LCTRL), IN_GAME);
        // в игре R и Ctrl+R пересекаются (у одной нет модификатора)
        assertTrue(conflicts(reload, unload, FORGE));
        // разные модификаторы — нет
        assertFalse(conflicts(unload, altR, FORGE));
        // модификатор = клавиша другой (Ctrl+R против спринта на ЛCtrl)
        assertTrue(conflicts(unload, sprint, FORGE));
        // в GUI-контексте R и Ctrl+R различаются
        assertFalse(conflicts(
                key("g1", KeyInput.key(R), GUI), key("g2", KeyInput.key(R, KeyInput.Modifier.CONTROL), GUI), FORGE));
    }

    @Test
    void duplicateIdsAndSelfIgnored() {
        KeyInfo a = key("a", KeyInput.key(K), IN_GAME);
        assertTrue(find(List.of(a, a), FORGE).isEmpty());
    }
}
