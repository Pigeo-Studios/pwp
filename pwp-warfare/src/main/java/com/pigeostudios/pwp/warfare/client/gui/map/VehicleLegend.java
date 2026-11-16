package com.pigeostudios.pwp.warfare.client.gui.map;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.SquadMapRenderer;
import com.pigeostudios.pwp.warfare.client.gui.ui.AnimMap;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Легенда техники карты M: по командам — тип техники с иконкой и «живых/всего» (цвет: все целы /
 * часть потеряна / ни одной), тултип — боеготовность и таймеры респавна своей команды. Заголовок
 * рисует панель-владелец; тултип возвращается вызывающему (рисуется вне scissor).
 */
@OnlyIn(Dist.CLIENT)
public final class VehicleLegend {
    private final AnimMap hover = new AnimMap();

    private record Group(String team, String type, int alive, int total, boolean allSpawned) {}

    private static List<Group> groups() {
        List<Group> out = new ArrayList<>();
        if (ClientData.clientSpawners == null) return out;
        for (String team : new String[] {"BLUE", "RED"}) {
            Map<String, int[]> counts = new LinkedHashMap<>();
            Map<String, Boolean> spawned = new LinkedHashMap<>();
            for (WarfareWorldData.SpawnerInfo s : ClientData.clientSpawners) {
                if (!s.team.equalsIgnoreCase(team)) continue;
                int[] c = counts.computeIfAbsent(s.type, k -> new int[2]);
                c[1]++;
                if (s.isAlive) c[0]++;
                spawned.merge(s.type, s.hasSpawnedOnce, Boolean::logicalAnd);
            }
            for (var e : counts.entrySet()) {
                out.add(new Group(
                        team, e.getKey(), e.getValue()[0], e.getValue()[1], spawned.getOrDefault(e.getKey(), false)));
            }
        }
        return out;
    }

    public static boolean isEmpty() {
        return ClientData.clientSpawners == null || ClientData.clientSpawners.isEmpty();
    }

    /** Рисует легенду в области; возвращает тултип наведённой строки или null. */
    public UITooltip.Content render(GuiGraphics g, Rect area, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        int rh = WTheme.SQUAD_ROW_H;
        int y = area.y();
        String team = null;
        UITooltip.Content tip = null;
        for (Group gr : groups()) {
            if (!gr.team().equals(team)) {
                team = gr.team();
                boolean blue = team.equals("BLUE");
                String faction = SquadMapRenderer.factionName(blue ? ClientData.BLUE_FACTION : ClientData.RED_FACTION);
                Component name = faction != null
                        ? Component.literal(faction)
                        : Component.translatable(blue ? "gui.pwpwarfare.map.team_blue" : "gui.pwpwarfare.map.team_red");
                WDraw.section(
                        g,
                        font,
                        Rect.of(area.x(), y, area.w(), font.lineHeight + Spacing.XS),
                        name,
                        blue ? WTheme.TEAM_BLUE : WTheme.TEAM_RED,
                        1F);
                y += font.lineHeight + Spacing.XS + Spacing.XXS;
            }
            Rect r = Rect.of(area.x(), y, area.w(), rh);
            y += rh + 1;
            if (r.y() > area.bottom()) continue;
            boolean over = area.contains(mx, my) && r.contains(mx, my);
            float h = hover.get(gr.team() + gr.type(), over);
            if (h > 0.01F) UIDraw.rect(g, r, UIDraw.fade(WTheme.ROW_HOVER, h));
            ResourceLocation icon = SquadMapRenderer.vehicleIcon(gr.type());
            int is = WTheme.ICON_ROW - 2;
            UIDraw.blitTinted(
                    g,
                    0xFFFFFFFF,
                    () -> g.blit(icon, r.x() + Spacing.XXS, r.centerY() - is / 2, is, is, 0, 0, 16, 16, 16, 16));
            Component cnt = Component.literal(gr.alive() + "/" + gr.total());
            int cw = font.width(cnt);
            int color = gr.alive() == 0
                    ? (gr.allSpawned() ? WTheme.STATUS_BAD : WTheme.TEAM_BLUE)
                    : (gr.alive() < gr.total() ? WTheme.STATUS_WARN : WTheme.STATUS_OK);
            UIDraw.text(
                    g, font, cnt, r.right() - cw - Spacing.XXS, r.centerY() - font.lineHeight / 2 + 1, color, false);
            int tx = r.x() + Spacing.XXS + is + Spacing.XS;
            WDraw.text(
                    g,
                    font,
                    Component.literal(gr.type()),
                    Rect.of(tx, r.y(), r.right() - tx - cw - Spacing.XS, r.h()),
                    Align.START,
                    PWPTheme.Text.BODY,
                    false,
                    1F);
            if (over) tip = tooltip(gr);
        }
        return tip;
    }

    private static UITooltip.Content tooltip(Group gr) {
        var mc = Minecraft.getInstance();
        String myTeam = mc.player != null && mc.player.getTeam() != null
                ? mc.player.getTeam().getName()
                : "";
        long now = mc.level != null ? mc.level.getGameTime() : 0;
        List<Component> lines = new ArrayList<>();
        boolean neverSpawned = false;
        int alive = 0;
        List<String> timers = new ArrayList<>();
        for (WarfareWorldData.SpawnerInfo s : ClientData.clientSpawners) {
            if (!s.team.equalsIgnoreCase(gr.team()) || !s.type.equals(gr.type())) continue;
            if (s.isAlive) alive++;
            if (!s.hasSpawnedOnce) neverSpawned = true;
            if (myTeam.equalsIgnoreCase(gr.team()) && !s.isAlive && s.targetSpawnTick > now) {
                long sec = (s.targetSpawnTick - now) / 20;
                String t = sec >= 60 ? (sec / 60) + ":" + String.format("%02d", sec % 60) : sec + "s";
                timers.add(Component.translatable(
                                s.hasSpawnedOnce
                                        ? "gui.pwpwarfare.map.vehicle.respawn"
                                        : "gui.pwpwarfare.map.vehicle.appears",
                                t)
                        .getString());
            }
        }
        timers.sort(null);
        lines.add(Component.translatable("gui.pwpwarfare.map.vehicle.ready", alive)
                .withStyle(st -> st.withColor(Colors.SUCCESS_LIGHT)));
        if (neverSpawned) lines.add(Component.translatable("gui.pwpwarfare.map.vehicle.not_spawned"));
        int max = WTheme.LEGEND_TIMER_LINES;
        for (int i = 0; i < Math.min(max, timers.size()); i++) lines.add(Component.literal(timers.get(i)));
        if (timers.size() > max) lines.add(Component.literal("…"));
        return new UITooltip.Content(Component.literal(gr.type()), lines);
    }
}
