package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSquadMarker;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Тактическая радиалка карты: маркеры команды / врага (сетка иконок), маркеры отряда; клик в
 * центр — пинг (тип 6), как раньше.
 */
public class TacticalMapRadialScreen extends WarfareRadialScreen {
    private final int wx;
    private final int wz;
    private final Screen previousScreen;

    public TacticalMapRadialScreen(int x, int z, Screen previousScreen) {
        super(Component.translatable("gui.pwpwarfare.radial.tactical"));
        this.wx = x;
        this.wz = z;
        this.previousScreen = previousScreen;
    }

    @Override
    protected List<Entry> entries() {
        return List.of(
                Entry.of(
                                Component.translatable("gui.pwpwarfare.radial.item.team"),
                                WarfareIcon.TEAM,
                                () -> openGrid("gui.pwpwarfare.radial.team_markers", teamMarkers()))
                        .accent(WTheme.MARKER_TEAM),
                Entry.of(
                                Component.translatable("gui.pwpwarfare.radial.item.enemy"),
                                WarfareIcon.TARGET,
                                () -> openGrid("gui.pwpwarfare.radial.enemy_markers", enemyMarkers()))
                        .accent(WTheme.MARKER_ENEMY),
                Entry.of(
                                Component.translatable("gui.pwpwarfare.radial.item.squad"),
                                WarfareIcon.SQUAD,
                                () -> Minecraft.getInstance()
                                        .setScreen(new SquadMarkerRadialScreen(wx, wz, previousScreen)))
                        .accent(WTheme.MARKER_SQUAD));
    }

    @Override
    protected Runnable centerAction() {
        return () -> {
            PacketHandler.INSTANCE.sendToServer(new PacketSquadMarker(wx, wz, 6));
            Minecraft.getInstance().setScreen(previousScreen);
        };
    }

    @Override
    protected Component centerText() {
        return Component.translatable("gui.pwpwarfare.radial.item.ping");
    }

    private void openGrid(String title, Map<String, ResourceLocation> markers) {
        Minecraft.getInstance().setScreen(new MapMarkerGridScreen(wx, wz, title, markers, previousScreen));
    }

    private static ResourceLocation icon(String name) {
        return new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + name + ".png");
    }

    private static Map<String, ResourceLocation> teamMarkers() {
        Map<String, ResourceLocation> m = new LinkedHashMap<>();
        m.put("Supply Request", icon("supply_request_marker"));
        m.put("Artillery Request", icon("artillery_request_marker"));
        return m;
    }

    private static Map<String, ResourceLocation> enemyMarkers() {
        Map<String, ResourceLocation> m = new LinkedHashMap<>();
        m.put("Infantry", icon("infantry_marker"));
        m.put("Sniper", icon("sniper_marker"));
        m.put("HAT", icon("hat_marker"));
        m.put("Mortar", icon("mortar_marker"));
        m.put("TOW", icon("tow_marker"));
        m.put("Enemy HUB", icon("hub_marker"));
        m.put("Enemy Rally", icon("rally_marker"));
        m.put("Combat Vehicle", icon("combat_vehicle_marker"));
        m.put("Infantry Vehicle", icon("infantry_vehicle_marker"));
        m.put("APC", icon("apc_marker"));
        m.put("Tank", icon("tank_marker"));
        m.put("Helicopter", icon("helicopter_marker"));
        m.put("CAS Heli", icon("cas_helicopter_marker"));
        m.put("CAS Fighter", icon("cas_fighter_marker"));
        m.put("Mobile ZU", icon("mobile_zu_marker"));
        m.put("Supply Truck", icon("supply_truck_marker"));
        return m;
    }
}
