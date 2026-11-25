package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientPlacementHandler;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRadioAction;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Радиалка обороны (рация -> оборона): стены, бункер, станция техники, колючка, FOB. */
public class DefenseRadialScreen extends WarfareRadialScreen {
    private final Screen parentScreen;

    public DefenseRadialScreen(Screen parent) {
        super(Component.translatable("gui.pwpwarfare.radial.defense"));
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
                Entry.of(
                        Component.translatable("gui.pwpwarfare.radial.item.walls"),
                        WarfareIcon.WALL,
                        () -> switchTo(new WallRadialScreen(this))),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.bunker"), WarfareIcon.SHIELD, place(17)),
                Entry.of(
                        Component.translatable("gui.pwpwarfare.radial.item.vehicle_station"),
                        WarfareIcon.VEHICLE,
                        place(18)),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.wire"), WarfareIcon.WIRE, place(13)),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.fob"), WarfareIcon.SPAWN_FOB, () -> {
                    PacketHandler.INSTANCE.sendToServer(new PacketRadioAction(14));
                    onClose();
                }));
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
