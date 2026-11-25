package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientPlacementHandler;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Радиалка стен (рация -> оборона -> стены): стены 1x1/2x2/3x3, колючка, бойница. */
public class WallRadialScreen extends WarfareRadialScreen {
    private final Screen parentScreen;

    public WallRadialScreen(Screen parent) {
        super(Component.translatable("gui.pwpwarfare.radial.walls"));
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
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.wall_1x1"), WarfareIcon.WALL, place(10)),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.wall_2x2"), WarfareIcon.WALL, place(11)),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.wall_3x3"), WarfareIcon.WALL, place(12)),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.wire"), WarfareIcon.WIRE, place(15)),
                Entry.of(
                        Component.translatable("gui.pwpwarfare.radial.item.embrasure"), WarfareIcon.SHIELD, place(16)));
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
