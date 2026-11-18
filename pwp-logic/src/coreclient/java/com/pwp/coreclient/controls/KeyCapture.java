package com.pwp.coreclient.controls;

/**
 * Ожидание новой клавиши после клика по кнопке назначения (без Minecraft; экран переводит события
 * GLFW в {@link KeyInput}).
 *
 * <ul>
 *   <li>Esc — отмена (назначение не меняется);
 *   <li>Backspace/Delete без модификатора — снять назначение;
 *   <li>клавиша-модификатор (Ctrl/Shift/Alt) запоминается: следующая клавиша получит этот
 *       модификатор (как {@code KeyModifier.getActiveModifier} у Forge), а если модификатор
 *       отпустить, ничего не нажав, назначается он сам (как ванильный {@code
 *       KeyBindsScreen.keyReleased});
 *   <li>любая другая клавиша или кнопка мыши назначается с зажатым модификатором.
 * </ul>
 */
public final class KeyCapture {
    public static final int KEY_ESCAPE = 256;
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_DELETE = 261;

    /** Итог события. */
    public sealed interface Result permits Waiting, Cancel, Unbind, Bind {}

    /** Ждём дальше. */
    public record Waiting() implements Result {}

    /** Отмена: оставить как было. */
    public record Cancel() implements Result {}

    /** Снять назначение. */
    public record Unbind() implements Result {}

    /** Назначить. */
    public record Bind(KeyInput key) implements Result {}

    private static final Result WAITING = new Waiting();

    private String target;
    private KeyInput pendingModifier;

    /** Начать ожидание для клавиши {@code id}. */
    public void start(String id) {
        target = id;
        pendingModifier = null;
    }

    public boolean active() {
        return target != null;
    }

    /** id клавиши, для которой ждём, или {@code null}. */
    public String target() {
        return target;
    }

    /** Модификатор, зажатый в ожидании (подпись «Ctrl + …»), или {@code null}. */
    public KeyInput pendingModifier() {
        return pendingModifier;
    }

    public void stop() {
        target = null;
        pendingModifier = null;
    }

    /**
     * Нажатие клавиши.
     *
     * @param key нажатая клавиша (её модификатор игнорируется)
     * @param isModifierKey это сама клавиша-модификатор ({@code KeyModifier.isKeyCodeModifier})
     * @param held модификатор, зажатый сейчас ({@code KeyModifier.getActiveModifier})
     */
    public Result press(KeyInput key, boolean isModifierKey, KeyInput.Modifier held) {
        if (!active()) return WAITING;
        boolean keyboard = key.kind() != KeyInput.Kind.MOUSE;
        if (keyboard && key.code() == KEY_ESCAPE) return finish(new Cancel());
        if (isModifierKey) {
            pendingModifier = new KeyInput(key.kind(), key.code(), KeyInput.Modifier.NONE);
            return WAITING;
        }
        KeyInput.Modifier mod = held == null ? KeyInput.Modifier.NONE : held;
        boolean erase = keyboard && (key.code() == KEY_BACKSPACE || key.code() == KEY_DELETE);
        if (erase && mod == KeyInput.Modifier.NONE) return finish(new Unbind());
        return finish(new Bind(new KeyInput(key.kind(), key.code(), mod)));
    }

    /** Отпускание клавиши: отпустили запомненный модификатор — назначаем его самого. */
    public Result release(KeyInput key) {
        if (!active() || pendingModifier == null || !pendingModifier.sameInput(key)) return WAITING;
        return finish(new Bind(pendingModifier));
    }

    /** Кнопка мыши ({@code button} — номер GLFW). */
    public Result mouse(int button, KeyInput.Modifier held) {
        if (!active()) return WAITING;
        KeyInput.Modifier mod = held == null ? KeyInput.Modifier.NONE : held;
        return finish(new Bind(new KeyInput(KeyInput.Kind.MOUSE, button, mod)));
    }

    private Result finish(Result r) {
        stop();
        return r;
    }
}
