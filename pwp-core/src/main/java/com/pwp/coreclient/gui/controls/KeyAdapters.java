package com.pwp.coreclient.gui.controls;

import com.mojang.blaze3d.platform.InputConstants;
import com.pwp.coreclient.controls.ControlsConflicts;
import com.pwp.coreclient.controls.KeyInput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

/** Перевод Minecraft/Forge ↔ чистая модель {@link KeyInput}. */
@OnlyIn(Dist.CLIENT)
public final class KeyAdapters {
    private KeyAdapters() {}

    public static KeyInput toInput(InputConstants.Key key, KeyModifier modifier) {
        if (key == null || key.equals(InputConstants.UNKNOWN)) return KeyInput.UNBOUND;
        KeyInput.Kind kind =
                switch (key.getType()) {
                    case MOUSE -> KeyInput.Kind.MOUSE;
                    case SCANCODE -> KeyInput.Kind.SCANCODE;
                    default -> KeyInput.Kind.KEYSYM;
                };
        return new KeyInput(kind, key.getValue(), toModifier(modifier));
    }

    public static InputConstants.Key toKey(KeyInput in) {
        if (in == null || in.isUnbound()) return InputConstants.UNKNOWN;
        InputConstants.Type type =
                switch (in.kind()) {
                    case MOUSE -> InputConstants.Type.MOUSE;
                    case SCANCODE -> InputConstants.Type.SCANCODE;
                    case KEYSYM -> InputConstants.Type.KEYSYM;
                };
        return type.getOrCreate(in.code());
    }

    public static KeyInput.Modifier toModifier(KeyModifier m) {
        if (m == null) return KeyInput.Modifier.NONE;
        return switch (m) {
            case CONTROL -> KeyInput.Modifier.CONTROL;
            case SHIFT -> KeyInput.Modifier.SHIFT;
            case ALT -> KeyInput.Modifier.ALT;
            case NONE -> KeyInput.Modifier.NONE;
        };
    }

    public static KeyModifier fromModifier(KeyInput.Modifier m) {
        if (m == null) return KeyModifier.NONE;
        return switch (m) {
            case CONTROL -> KeyModifier.CONTROL;
            case SHIFT -> KeyModifier.SHIFT;
            case ALT -> KeyModifier.ALT;
            case NONE -> KeyModifier.NONE;
        };
    }

    /** Клавиша события GLFW, как её строит ванильный {@code KeyBindsScreen}. */
    public static KeyInput fromEvent(int keyCode, int scanCode) {
        return toInput(InputConstants.getKey(keyCode, scanCode), KeyModifier.NONE);
    }

    /** Сама ли клавиша — модификатор Forge (с учётом macOS). */
    public static boolean isModifierKey(KeyInput in) {
        return !in.isUnbound() && KeyModifier.isKeyCodeModifier(toKey(in));
    }

    /**
     * Зажатый сейчас модификатор — как {@code KeyModifier.getActiveModifier} (deprecated в 47.x):
     * первый активный CONTROL → SHIFT → ALT; {@code KeyModifier.X.isActive} = {@code
     * Screen.hasControlDown/hasShiftDown/hasAltDown} (Ctrl на macOS = Cmd).
     */
    public static KeyInput.Modifier heldModifier() {
        if (net.minecraft.client.gui.screens.Screen.hasControlDown()) return KeyInput.Modifier.CONTROL;
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) return KeyInput.Modifier.SHIFT;
        if (net.minecraft.client.gui.screens.Screen.hasAltDown()) return KeyInput.Modifier.ALT;
        return KeyInput.Modifier.NONE;
    }

    /** Подпись назначения, как {@code KeyMapping.getTranslatedKeyMessage} у Forge. */
    public static Component label(KeyInput in) {
        InputConstants.Key key = toKey(in);
        KeyModifier m = fromModifier(in == null ? null : in.modifier());
        return m.getCombinedName(key, key::getDisplayName);
    }

    /** id группы контекста для чистой модели; свои контексты модов — по имени класса. */
    public static String contextId(IKeyConflictContext ctx) {
        if (ctx == KeyConflictContext.IN_GAME) return ControlsConflicts.IN_GAME;
        if (ctx == KeyConflictContext.GUI) return ControlsConflicts.GUI;
        if (ctx == KeyConflictContext.UNIVERSAL || ctx == null) return ControlsConflicts.UNIVERSAL;
        return "custom:" + ctx.getClass().getName() + "#" + ctx;
    }
}
