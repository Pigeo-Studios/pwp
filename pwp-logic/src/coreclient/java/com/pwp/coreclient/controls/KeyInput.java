package com.pwp.coreclient.controls;

/**
 * Абстрактная назначенная клавиша без Minecraft: тип ввода + код + модификатор. Экран переводит в
 * неё {@code InputConstants.Key} и {@code KeyModifier} Forge.
 *
 * @param kind тип ввода (клавиатура по символу/по скан-коду/мышь)
 * @param code код GLFW (для мыши — номер кнопки)
 * @param modifier модификатор Forge (Ctrl/Shift/Alt) или {@link Modifier#NONE}
 */
public record KeyInput(Kind kind, int code, Modifier modifier) {
    /** Не назначена: ни с чем не конфликтует. */
    public static final KeyInput UNBOUND = new KeyInput(Kind.KEYSYM, -1, Modifier.NONE);

    public KeyInput {
        if (kind == null) kind = Kind.KEYSYM;
        if (modifier == null) modifier = Modifier.NONE;
    }

    public static KeyInput key(int glfwKey) {
        return new KeyInput(Kind.KEYSYM, glfwKey, Modifier.NONE);
    }

    public static KeyInput key(int glfwKey, Modifier modifier) {
        return new KeyInput(Kind.KEYSYM, glfwKey, modifier);
    }

    public static KeyInput mouse(int button) {
        return new KeyInput(Kind.MOUSE, button, Modifier.NONE);
    }

    /** Код -1 = не назначена (так ваниль хранит {@code InputConstants.UNKNOWN}). */
    public boolean isUnbound() {
        return code < 0;
    }

    /** Та же физическая клавиша (без учёта модификатора). */
    public boolean sameInput(KeyInput other) {
        return other != null && kind == other.kind && code == other.code;
    }

    /** Тип ввода — как {@code InputConstants.Type}. */
    public enum Kind {
        KEYSYM,
        SCANCODE,
        MOUSE
    }

    /** Модификатор — как {@code net.minecraftforge.client.settings.KeyModifier}. */
    public enum Modifier {
        NONE(-1, -1),
        CONTROL(341, 345),
        SHIFT(340, 344),
        ALT(342, 346);

        private final int left;
        private final int right;

        Modifier(int left, int right) {
            this.left = left;
            this.right = right;
        }

        /**
         * Является ли клавиша этим модификатором (левая/правая; коды GLFW KEY_LEFT_*). Как Forge
         * {@code KeyModifier.matches}: сравнивается только код, тип ввода не проверяется. На macOS
         * Forge считает CONTROL клавишей Super (343/347) — см. {@link #matches(KeyInput, boolean)}.
         */
        public boolean matches(KeyInput key) {
            return matches(key, false);
        }

        /** То же с учётом macOS ({@code Minecraft.ON_OSX}): CONTROL = левый/правый Super. */
        public boolean matches(KeyInput key, boolean osx) {
            if (this == NONE || key == null) return false;
            if (osx && this == CONTROL) return key.code() == 343 || key.code() == 347;
            return key.code() == left || key.code() == right;
        }
    }
}
