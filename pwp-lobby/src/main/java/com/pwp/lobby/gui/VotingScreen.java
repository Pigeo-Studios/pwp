package com.pwp.lobby.gui;

import com.pwp.lobby.maps.MapConfig;
import com.pwp.lobby.maps.MapRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class VotingScreen extends Screen {

    private static final int MAPS_PER_PAGE = 5;
    private int page = 0;
    private String votedMap = null;

    private List<MapConfig> maps;

    public VotingScreen() {
        super(Component.translatable("pwp_lobby.voting.title"));
        maps = MapRegistry.getVotable();
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = width / 2;
        int y = 40;
        int bw = 220;
        int bh = 30;

        int start = page * MAPS_PER_PAGE;
        int end = Math.min(start + MAPS_PER_PAGE, maps.size());

        for (int i = start; i < end; i++, y += bh + 4) {
            MapConfig map = maps.get(i);
            String label = map.displayName + "  (" + map.maxPlayers + "p)";
            boolean sel = map.name.equals(votedMap);

            addRenderableWidget(Button.builder(
                    Component.literal((sel ? "> " : "") + label),
                    b -> {
                        votedMap = map.name;
                        Minecraft.getInstance().player.connection.sendChat(
                                "/votemap " + map.name);
                        init();
                    }).bounds(cx - bw / 2, y, bw, bh).build());
        }

        if (maps.size() > MAPS_PER_PAGE) {
            if (page > 0)
                addRenderableWidget(Button.builder(Component.literal("<"),
                        b -> { page--; init(); }).bounds(cx - 110, y + 10, 40, 20).build());
            if (end < maps.size())
                addRenderableWidget(Button.builder(Component.literal(">"),
                        b -> { page++; init(); }).bounds(cx + 70, y + 10, 40, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Close"),
                b -> onClose()).bounds(cx - 40, height - 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);
        gui.drawCenteredString(font, Component.translatable("pwp_lobby.voting.title"), width / 2, 15, 0xFFFFFF);
        if (votedMap != null)
            gui.drawCenteredString(font, "Voted: " + votedMap, width / 2, height - 45, 0xFFC040);
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.setScreen(new VotingScreen());
    }
}
