package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSquadMarker;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Маркеры отряда: слой приказов (по умолчанию) — СТРОИТЬ вверху, ДВИГАТЬСЯ справа, АТАКОВАТЬ
 * внизу, ОБОРОНЯТЬ слева (типы 3/0/1/2, как раньше); главный слой — команда/враг (типы 4/5)
 * и переход к приказам.
 */
public class SquadMarkerRadialScreen extends WarfareRadialScreen {
    private final int targetX;
    private final int targetZ;
    private final Screen previousScreen;
    private int currentLayer = 1;

    public SquadMarkerRadialScreen(int x, int z, Screen previousScreen) {
        super(Component.translatable("gui.pwpwarfare.radial.markers"));
        this.targetX = x;
        this.targetZ = z;
        this.previousScreen = previousScreen;
    }

    private Runnable send(int type) {
        return () -> {
            PacketHandler.INSTANCE.sendToServer(new PacketSquadMarker(targetX, targetZ, type));
            Minecraft.getInstance().setScreen(previousScreen);
        };
    }

    @Override
    protected List<Entry> entries() {
        if (currentLayer == 0) {
            return List.of(
                    Entry.of(Component.translatable("gui.pwpwarfare.radial.item.team"), WarfareIcon.TEAM, send(4))
                            .accent(WTheme.MARKER_TEAM),
                    Entry.of(Component.translatable("gui.pwpwarfare.radial.item.enemy"), WarfareIcon.TARGET, send(5))
                            .accent(WTheme.MARKER_ENEMY),
                    Entry.of(
                                    Component.translatable("gui.pwpwarfare.radial.item.squad"),
                                    WarfareIcon.SQUAD,
                                    () -> currentLayer = 1)
                            .accent(WTheme.MARKER_SQUAD));
        }
        return List.of(
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.build"), WarfareIcon.WALL, send(3))
                        .accent(WTheme.ORDER_BUILD),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.move"), WarfareIcon.PATH, send(0))
                        .accent(WTheme.ORDER_MOVE),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.attack"), WarfareIcon.TARGET, send(1))
                        .accent(WTheme.ORDER_ATTACK),
                Entry.of(Component.translatable("gui.pwpwarfare.radial.item.defend"), WarfareIcon.SHIELD, send(2))
                        .accent(WTheme.ORDER_DEFEND));
    }
}
