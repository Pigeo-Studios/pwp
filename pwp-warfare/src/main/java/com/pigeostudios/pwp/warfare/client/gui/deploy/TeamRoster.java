package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.Scroll;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Вкладка «Команды»: две колонки (союзники / противники) — флаг и название фракции, число
 * игроков, строки игроков с тегом отряда, звезда у командира отряда, «я» — акцентом.
 */
@OnlyIn(Dist.CLIENT)
public class TeamRoster {
    private final Scroll scroll = new Scroll();

    public void scroll(double delta) {
        scroll.wheel(delta);
    }

    /** Флаг фракции 64x36 или null. */
    public static ResourceLocation flag(String faction) {
        if (faction == null || faction.isEmpty() || faction.equalsIgnoreCase("none")) return null;
        return ResourceLocation.tryBuild("pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png");
    }

    public static void drawFlag(GuiGraphics g, ResourceLocation flag, Rect r) {
        if (flag == null) return;
        RenderSystem.enableBlend();
        g.blit(flag, r.x(), r.y(), r.w(), r.h(), 0, 0, 64, 36, 64, 36);
        RenderSystem.disableBlend();
        UIDraw.outline(g, r, PWPTheme.Styles.Glass.BORDER);
    }

    public void render(GuiGraphics g, Rect area, String myTeam, float o) {
        Font font = Minecraft.getInstance().font;
        boolean blue = myTeam.toUpperCase().contains("BLUE");
        List<MapPlayerInfo> all = List.copyOf(ClientData.mapPlayers.values());
        List<MapPlayerInfo> allies = all.stream()
                .filter(p -> p.team != null && p.team.equalsIgnoreCase(myTeam))
                .toList();
        List<MapPlayerInfo> enemies = all.stream()
                .filter(p -> p.team != null && !p.team.equalsIgnoreCase(myTeam))
                .toList();
        int headerH = WDraw.headerHeight(Minecraft.getInstance().getWindow().getGuiScaledHeight());
        int rowsH = Math.max(allies.size(), enemies.size()) * (WTheme.ROW_H + 1);
        int contentH = rowsH + WTheme.FLAG_H + Spacing.SM + headerH;
        scroll.clamp(contentH, area.h());
        Rect[] cols = Flow.row(area, WTheme.GAP * 2, Flow.Justify.START, Flow.flex(1), Flow.flex(1));
        column(
                g,
                font,
                cols[0],
                allies,
                blue ? ClientData.BLUE_FACTION : ClientData.RED_FACTION,
                blue ? ClientData.customBlueName : ClientData.customRedName,
                blue ? WTheme.TEAM_BLUE : WTheme.TEAM_RED,
                true,
                headerH,
                o);
        column(
                g,
                font,
                cols[1],
                enemies,
                blue ? ClientData.RED_FACTION : ClientData.BLUE_FACTION,
                blue ? ClientData.customRedName : ClientData.customBlueName,
                blue ? WTheme.TEAM_RED : WTheme.TEAM_BLUE,
                false,
                headerH,
                o);
    }

    private void column(
            GuiGraphics g,
            Font font,
            Rect r,
            List<MapPlayerInfo> players,
            String faction,
            String name,
            int teamColor,
            boolean ally,
            int headerH,
            float o) {
        Rect content = WDraw.panel(
                g,
                font,
                r,
                headerH,
                ally ? WarfareIcon.TEAM : WarfareIcon.TARGET,
                Component.translatable(
                        ally ? "gui.pwpwarfare.deploy.team.allies" : "gui.pwpwarfare.deploy.team.enemies"),
                Component.literal(String.valueOf(players.size())),
                o);
        // фракция: флаг + название цветом команды
        Rect fac = content.top(WTheme.FLAG_H);
        ResourceLocation fl = flag(faction);
        int tx = fac.x();
        if (fl != null) {
            drawFlag(g, fl, Rect.of(fac.x(), fac.y(), WTheme.FLAG_W, WTheme.FLAG_H));
            tx += WTheme.FLAG_W + Spacing.SM;
        }
        UIDraw.accentBarH(g, fac.x(), fac.bottom() + Spacing.XXS, fac.w(), 1, UIDraw.fade(teamColor, o * 0.6F));
        WDraw.text(
                g,
                font,
                Component.literal(name == null ? "" : name),
                Rect.of(tx, fac.y(), fac.right() - tx, fac.h()),
                Align.START,
                teamColor,
                true,
                o);
        Rect list = content.inset(0, WTheme.FLAG_H + Spacing.SM, 0, 0);
        if (players.isEmpty()) {
            WDraw.emptyState(
                    g,
                    font,
                    list,
                    WarfareIcon.PERSON,
                    Component.translatable("gui.pwpwarfare.deploy.team.no_players"),
                    null,
                    o);
            return;
        }
        var me = Minecraft.getInstance().player;
        String myName = me != null ? me.getScoreboardName() : "";
        g.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        int y = list.y() - scroll.offset();
        int i = 0;
        for (MapPlayerInfo p : players) {
            Rect row = Rect.of(list.x(), y, list.w(), WTheme.ROW_H);
            y += WTheme.ROW_H + 1;
            if (row.bottom() < list.y() || row.y() > list.bottom()) {
                i++;
                continue;
            }
            if (i++ % 2 == 0) UIDraw.rect(g, row, UIDraw.fade(WTheme.ROW_STRIPE, o));
            boolean isMe = p.name.equals(myName);
            if (isMe)
                UIDraw.accentBar(
                        g, row.x(), row.y(), row.h(), Spacing.ACCENT_BAR, UIDraw.fade(PWPTheme.Colors.ACCENT, o));
            WarfareWorldData.Squad sq = squadOf(p.name);
            int right = row.right() - Spacing.XS;
            if (sq != null && sq.leader.equals(p.name)) {
                WDraw.icon(
                        g,
                        WarfareIcon.STAR,
                        right - WTheme.ICON_SMALL,
                        row.centerY() - WTheme.ICON_SMALL / 2,
                        WTheme.ICON_SMALL,
                        UIDraw.fade(WTheme.STATUS_OK, o));
                right -= WTheme.ICON_SMALL + Spacing.XS;
            }
            int nx = row.x() + Spacing.SM;
            if (sq != null) {
                Component tag = Component.literal(sq.name);
                int tw = Math.min(font.width(tag), (right - nx) / 2);
                WDraw.text(g, font, tag, Rect.of(nx, row.y(), tw, row.h()), Align.START, PWPTheme.Text.MUTED, false, o);
                nx += tw + Spacing.XS;
            }
            int color = isMe ? PWPTheme.Colors.TEXT_ACCENT : (ally ? WTheme.ALLY_TEXT : WTheme.ENEMY_TEXT);
            WDraw.text(
                    g,
                    font,
                    Component.literal(p.name),
                    Rect.of(nx, row.y(), right - nx, row.h()),
                    Align.START,
                    color,
                    false,
                    o);
        }
        g.disableScissor();
    }

    private static WarfareWorldData.Squad squadOf(String player) {
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(player)) return s;
        }
        return null;
    }
}
