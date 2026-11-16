package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientPlacementHandler;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Радиалка установок (рация -> установки): ПТРК вверху, РЭБ справа, миномёт внизу, мини-РЭБ
 * слева (те же позиции, что раньше).
 */
public class StaticGunRadialScreen extends WarfareRadialScreen {
    private final Screen parentScreen;

    public StaticGunRadialScreen(Screen parent) {
        super(Component.translatable("gui.pwpwarfare.radial.static_guns"));
        this.parentScreen = parent;
    }

    private Runnable place(int structureId) {
        return () -> {
            ClientPlacementHandler.startPlacing(structureId);
            onClose();
        };
    }

    @Override
    protected List<Entry> entries() {
        return List.of(
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.tow"), WarfareIcon.TARGET, place(23)),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.reb"), WarfareIcon.RADIO, place(24)),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.mortar"), WarfareIcon.GUN, place(22)),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.reb_mini"), WarfareIcon.RADIO, place(25)));
    }

    @Override
    protected boolean radioAnimations() {
        return true;
    }

    @Override
    protected boolean onBack() {
        Minecraft.getInstance().setScreen(parentScreen);
        return true;
    }
}
