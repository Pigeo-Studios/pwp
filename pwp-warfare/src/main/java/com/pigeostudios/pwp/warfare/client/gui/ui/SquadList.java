package com.pigeostudios.pwp.warfare.client.gui.ui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.SquadUIHelper;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.squad.SquadPackets;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Список отрядов команды на ширину колонки (деплой, карта M, выбор отряда): строка отряда —
 * номер, имя «…», занятость N/9, замок, раскрытие и действие (вступить/покинуть/закрыт/полный);
 * раскрытый отряд — участники с иконкой кита и цветом звена. Раскладка — одна функция
 * {@link #layout} для рисования и клика; действия — те же SquadPackets, что раньше.
 */
@OnlyIn(Dist.CLIENT)
public final class SquadList {
    private SquadList() {}

    /** Максимум бойцов в отряде (как в прежнем UI). */
    public static final int MAX_MEMBERS = 9;

    private enum Act {
        NONE,
        JOIN,
        LEAVE,
        LOCKED,
        FULL
    }

    private record Row(
            WarfareWorldData.Squad squad, String member, Rect rect, Rect action, Rect arrow, Rect lock, Act act) {}

    private static final AnimMap HOVER = new AnimMap();
    private static final Map<String, Boolean> ICON_EXISTS = new HashMap<>();

    private static Act actionOf(WarfareWorldData.Squad squad, boolean mine, boolean inSquad) {
        if (mine) return Act.LEAVE;
        if (inSquad) return Act.NONE;
        if (squad.isLocked) return Act.LOCKED;
        if (squad.members.size() >= MAX_MEMBERS) return Act.FULL;
        return Act.JOIN;
    }

    private static Component actionLabel(Act a) {
        return Component.translatable("gui.pwpwarfare.squad.action." + a.name().toLowerCase());
    }

    private static List<Row> layout(Rect area, int topInset, Set<Integer> expanded, Font font) {
        var mc = Minecraft.getInstance();
        List<Row> rows = new ArrayList<>();
        if (mc.player == null || mc.level == null) return rows;
        String me = mc.player.getScoreboardName();
        boolean inSquad = SquadUIHelper.isPlayerInSquad();
        int rh = WTheme.SQUAD_ROW_H;
        int y = area.y() + topInset;
        for (WarfareWorldData.Squad sq : SquadUIHelper.getSortedSquads()) {
            Rect r = Rect.of(area.x(), y, area.w(), rh);
            Act act = actionOf(sq, sq.members.contains(me), inSquad);
            int right = r.right() - Spacing.XXS;
            Rect action = Rect.of(right, r.y(), 0, 0);
            if (act != Act.NONE) {
                int aw = Math.min(font.width(actionLabel(act)) + 2 * Spacing.XS, r.w() / 3);
                action = Rect.of(right - aw, r.y() + 1, aw, rh - 2);
                right = action.x() - Spacing.XXS;
            }
            Rect arrow = Rect.of(right - WTheme.ICON_SMALL - Spacing.XXS, r.y(), WTheme.ICON_SMALL + Spacing.XXS, rh);
            right = arrow.x();
            Rect lock = Rect.of(right, r.y(), 0, rh);
            if (sq.leader.equals(me) || sq.isLocked) {
                lock = Rect.of(right - WTheme.ICON_SMALL - Spacing.XXS, r.y(), WTheme.ICON_SMALL + Spacing.XXS, rh);
            }
            rows.add(new Row(sq, null, r, action, arrow, lock, act));
            y += rh + 1;
            if (expanded.contains(sq.id)) {
                for (String m : SquadUIHelper.getSortedMembers(sq)) {
                    rows.add(new Row(
                            sq,
                            m,
                            Rect.of(area.x() + Spacing.SM, y, area.w() - Spacing.SM, rh - 2),
                            null,
                            null,
                            null,
                            Act.NONE));
                    y += rh - 1;
                }
                y += Spacing.XXS;
            }
            y += Spacing.XXS;
        }
        return rows;
    }

    /** Высота содержимого (для прокрутки). */
    public static int contentHeight(Rect area, int topInset, Set<Integer> expanded) {
        List<Row> rows = layout(area, topInset, expanded, Minecraft.getInstance().font);
        return rows.isEmpty() ? topInset : rows.get(rows.size() - 1).rect().bottom() - area.y();
    }

    public static void render(GuiGraphics g, Rect area, int topInset, int mx, int my, Set<Integer> expanded) {
        Font font = Minecraft.getInstance().font;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String myTeam = SquadUIHelper.getPlayerTeam().toUpperCase();
        int cmdId = myTeam.contains("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
        String me = mc.player.getScoreboardName();
        int idx = 0;
        for (Row row : layout(area, topInset, expanded, font)) {
            Rect r = row.rect();
            if (r.bottom() < area.y() || r.y() > area.bottom()) {
                if (row.member() == null) idx++;
                continue;
            }
            WarfareWorldData.Squad sq = row.squad();
            if (row.member() != null) {
                renderMember(g, font, r, sq, row.member(), me);
                continue;
            }
            idx++;
            boolean cmd = sq.id == cmdId && cmdId != -1;
            boolean mine = sq.members.contains(me);
            float h = HOVER.get("sq" + sq.id, r.contains(mx, my));
            WDraw.tile(g, r, h, mine ? 1F : 0F, 1F, 1F);
            int x = r.x() + Spacing.XS + Spacing.ACCENT_BAR;
            Component num = Component.literal(idx + ".");
            UIDraw.text(g, font, num, x, r.centerY() - font.lineHeight / 2 + 1, PWPTheme.Text.MUTED, false);
            x += font.width(num) + Spacing.XS;
            Component count = Component.literal(sq.members.size() + "/" + MAX_MEMBERS);
            int nameRight = row.lock().x() - Spacing.XS;
            int cw = font.width(count);
            Component name = Component.literal((cmd ? "[CMD] " : "") + sq.name);
            int nameColor = cmd ? PWPTheme.Colors.TEXT_ACCENT : WTheme.STATUS_OK;
            int nw = WDraw.text(
                    g,
                    font,
                    name,
                    Rect.of(x, r.y(), Math.max(0, nameRight - x - cw - Spacing.XS), r.h()),
                    Align.START,
                    nameColor,
                    false,
                    1F);
            int full = sq.members.size() >= MAX_MEMBERS ? WTheme.STATUS_BAD : PWPTheme.Text.MUTED;
            if (nameRight - x - nw - Spacing.XS >= cw) {
                UIDraw.text(g, font, count, x + nw + Spacing.XS, r.centerY() - font.lineHeight / 2 + 1, full, false);
            }
            if (row.lock().w() > 0) {
                WDraw.icon(
                        g,
                        WarfareIcon.LOCK,
                        row.lock().x() + 1,
                        r.centerY() - WTheme.ICON_SMALL / 2,
                        WTheme.ICON_SMALL,
                        sq.isLocked ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Text.MUTED);
            }
            boolean open = expanded.contains(sq.id);
            float ah = HOVER.get("ar" + sq.id, row.arrow().contains(mx, my));
            WDraw.icon(
                    g,
                    open ? WarfareIcon.MINUS : WarfareIcon.PLUS,
                    row.arrow().x() + 1,
                    r.centerY() - WTheme.ICON_SMALL / 2,
                    WTheme.ICON_SMALL,
                    Colors.lerp(open ? Colors.ACCENT : PWPTheme.Text.MUTED, PWPTheme.Text.HEADING, ah));
            if (row.act() != Act.NONE) renderAction(g, font, row, mx, my);
        }
    }

    private static void renderAction(GuiGraphics g, Font font, Row row, int mx, int my) {
        Rect a = row.action();
        boolean live = row.act() == Act.JOIN || row.act() == Act.LEAVE;
        float h = HOVER.get("act" + row.squad().id, live && a.contains(mx, my));
        int color =
                switch (row.act()) {
                    case JOIN -> WTheme.STATUS_OK;
                    case LEAVE -> Colors.lerp(PWPTheme.Text.MUTED, WTheme.STATUS_BAD, h);
                    default -> Colors.TEXT_DIM;
                };
        if (live) {
            UIDraw.rect(g, a, Colors.withAlpha(color, WTheme.PILL_ALPHA + Math.round(0x30 * h)));
            UIDraw.outline(g, a, Colors.withAlpha(color, 0x80));
        }
        WDraw.text(g, font, actionLabel(row.act()), a, Align.CENTER, color, false, 1F);
    }

    private static void renderMember(
            GuiGraphics g, Font font, Rect r, WarfareWorldData.Squad sq, String member, String me) {
        var conn = Minecraft.getInstance().getConnection();
        boolean online = conn != null && conn.getPlayerInfo(member) != null;
        int x = r.x() + Spacing.XS;
        UIDraw.vline(g, r.x(), r.y(), r.bottom(), PWPTheme.Styles.Glass.BORDER);
        String kit = ClientData.playerKits.getOrDefault(member, "Unassigned");
        ResourceLocation tex = kit.isEmpty() || kit.equals("Unassigned") ? null : kitIcon(kit);
        int is = WTheme.ICON_ROW - 2;
        if (tex != null) {
            UIDraw.blitTinted(
                    g, 0xFFFFFFFF, () -> g.blit(tex, r.x() + Spacing.XS, r.centerY() - is / 2, 0, 0, is, is, is, is));
            x += is + Spacing.XS;
        }
        int right = r.right() - Spacing.XXS;
        if (member.equals(sq.leader)) {
            WDraw.icon(
                    g,
                    WarfareIcon.STAR,
                    right - WTheme.ICON_SMALL,
                    r.centerY() - WTheme.ICON_SMALL / 2,
                    WTheme.ICON_SMALL,
                    WTheme.STATUS_OK);
            right -= WTheme.ICON_SMALL + Spacing.XXS;
        }
        int color = memberColor(sq, member, online);
        if (member.equals(me)) color = PWPTheme.Colors.TEXT_ACCENT;
        WDraw.text(
                g, font, Component.literal(member), Rect.of(x, r.y(), right - x, r.h()), Align.START, color, false, 1F);
    }

    private static int memberColor(WarfareWorldData.Squad squad, String member, boolean online) {
        if (!online) return Colors.TEXT_DIM;
        if (member.equals(squad.leader)) return WTheme.STATUS_OK;
        if (member.equals(squad.bravoLeader)) return WTheme.FT_BRAVO_LEAD;
        if (squad.bravoMembers.contains(member)) return WTheme.FT_BRAVO;
        if (member.equals(squad.charlieLeader)) return WTheme.FT_CHARLIE_LEAD;
        if (squad.charlieMembers.contains(member)) return WTheme.FT_CHARLIE;
        return PWPTheme.Text.BODY;
    }

    private static ResourceLocation kitIcon(String kit) {
        String file = DeployData.kitIconFileName(kit);
        ResourceLocation rl = ResourceLocation.tryBuild("pwpwarfare", "textures/gui/kits/" + file + ".png");
        if (rl == null) return null;
        boolean ok = ICON_EXISTS.computeIfAbsent(file, f -> Minecraft.getInstance()
                .getResourceManager()
                .getResource(rl)
                .isPresent());
        return ok ? rl : null;
    }

    /** Клик: вступить/покинуть, раскрыть, замок (лидер), контекстное меню участника — как раньше. */
    public static boolean click(
            Rect area, int topInset, double mx, double my, Set<Integer> expanded, PWPContextMenu menu) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !area.contains(mx, my)) return false;
        String me = mc.player.getScoreboardName();
        for (Row row : layout(area, topInset, expanded, mc.font)) {
            if (!row.rect().contains(mx, my)) continue;
            WarfareWorldData.Squad sq = row.squad();
            if (row.member() != null) {
                if (sq.members.contains(me) && !row.member().equals(me))
                    openMemberMenu(menu, sq, me, row.member(), mx, my);
                return true;
            }
            if (row.act() != Act.NONE && row.action().contains(mx, my)) {
                if (row.act() == Act.LEAVE) PacketHandler.INSTANCE.sendToServer(new SquadPackets.Leave());
                else if (row.act() == Act.JOIN) PacketHandler.INSTANCE.sendToServer(new SquadPackets.Join(sq.id));
            } else if (row.lock().w() > 0 && row.lock().contains(mx, my) && sq.leader.equals(me)) {
                PacketHandler.INSTANCE.sendToServer(new SquadPackets.ToggleLock());
            } else if (!expanded.remove(sq.id)) {
                // клик по строке/стрелке раскрывает (раньше — только стрелка)
                expanded.add(sq.id);
            }
            return true;
        }
        return false;
    }

    private static void openMemberMenu(
            PWPContextMenu menu, WarfareWorldData.Squad sq, String me, String target, double mx, double my) {
        List<String> keys = new ArrayList<>();
        if (sq.leader.equals(me)) {
            keys.addAll(List.of(
                    "promote", "set_bravo", "set_charlie", "add_bravo", "add_charlie", "remove_ft", "kick", "disband"));
        } else if (sq.bravoLeader.equals(me)) {
            keys.addAll(List.of("pass_bravo", "add_bravo", "remove_ft"));
        } else if (sq.charlieLeader.equals(me)) {
            keys.addAll(List.of("pass_charlie", "add_charlie", "remove_ft"));
        }
        if (keys.isEmpty()) return;
        List<String> labels = keys.stream()
                .map(k ->
                        Component.translatable("gui.pwpwarfare.squad.menu." + k).getString())
                .toList();
        menu.show((int) mx, (int) my, labels, i -> {
            if (i < 0 || i >= keys.size()) return;
            switch (keys.get(i)) {
                case "promote" -> PacketHandler.INSTANCE.sendToServer(new SquadPackets.Promote(target));
                case "set_bravo", "pass_bravo" -> PacketHandler.INSTANCE.sendToServer(
                        new SquadPackets.SetFireteamLeader(SquadPackets.Fireteam.BRAVO, target));
                case "set_charlie", "pass_charlie" -> PacketHandler.INSTANCE.sendToServer(
                        new SquadPackets.SetFireteamLeader(SquadPackets.Fireteam.CHARLIE, target));
                case "add_bravo" -> PacketHandler.INSTANCE.sendToServer(
                        new SquadPackets.AddToFireteam(SquadPackets.Fireteam.BRAVO, target));
                case "add_charlie" -> PacketHandler.INSTANCE.sendToServer(
                        new SquadPackets.AddToFireteam(SquadPackets.Fireteam.CHARLIE, target));
                case "remove_ft" -> PacketHandler.INSTANCE.sendToServer(new SquadPackets.RemoveFromFireteam(target));
                case "kick" -> PacketHandler.INSTANCE.sendToServer(new SquadPackets.Kick(sq.id, target));
                case "disband" -> PacketHandler.INSTANCE.sendToServer(new SquadPackets.Disband(sq.id));
                default -> {}
            }
        });
    }
}
