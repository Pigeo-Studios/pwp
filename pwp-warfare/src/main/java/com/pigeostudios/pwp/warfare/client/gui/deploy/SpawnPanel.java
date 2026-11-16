package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.client.gui.SquadMapRenderer;
import com.pigeostudios.pwp.warfare.client.gui.ui.AnimMap;
import com.pigeostudios.pwp.warfare.client.gui.ui.Scroll;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Список точек спавна: карточка = иконка типа (основа/ралли/FOB), имя и квадрат карты, пилюля
 * статуса, припасы и дистанция. Заблокированные/уничтоженные — приглушены, клик по ним
 * сбрасывает выбор (как раньше). Пусто — заглушка.
 */
@OnlyIn(Dist.CLIENT)
public class SpawnPanel {
    private record Card(DeployData.SpawnPoint sp, Rect rect) {}

    private final Scroll scroll = new Scroll();
    private final AnimMap hover = new AnimMap();
    private final AnimMap select = new AnimMap(PWPTheme.Animations.DURATION_NORMAL);

    public void scroll(double delta) {
        scroll.wheel(delta);
    }

    private static int cardH(Rect area) {
        return area.h() >= 200 ? WTheme.SPAWN_CARD_H_LARGE : WTheme.SPAWN_CARD_H;
    }

    private List<Card> layout(Rect area) {
        int h = cardH(area);
        int gap = Spacing.XXS;
        List<Card> out = new ArrayList<>();
        int y = area.y() - scroll.offset();
        for (DeployData.SpawnPoint sp : DeployData.spawns) {
            out.add(new Card(sp, Rect.of(area.x(), y, area.w() - WTheme.SCROLLBAR_W - gap, h)));
            y += h + gap;
        }
        return out;
    }

    private static int contentH(Rect area) {
        int n = DeployData.spawns.size();
        return n == 0 ? 0 : n * cardH(area) + (n - 1) * Spacing.XXS;
    }

    static boolean blocked(DeployData.SpawnPoint sp) {
        return sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;
    }

    static WarfareIcon typeIcon(String id) {
        if (id.startsWith("HUB")) return WarfareIcon.SPAWN_FOB;
        if (id.startsWith("RALLY")) return WarfareIcon.SPAWN_RALLY;
        return WarfareIcon.SPAWN_MAIN;
    }

    static Component statusText(DeployData.SpawnStatus st) {
        return Component.translatable(
                "gui.pwpwarfare.deploy.spawn.status." + st.name().toLowerCase());
    }

    static int statusColor(DeployData.SpawnStatus st) {
        return switch (st) {
            case SAFE, HEALTHY -> WTheme.STATUS_OK;
            case COOLDOWN -> WTheme.STATUS_WARN;
            case BLOCKED, DESTROYED -> WTheme.STATUS_BAD;
        };
    }

    public void render(GuiGraphics g, Rect area, int mx, int my, String selectedSpawn, float o) {
        Font font = Minecraft.getInstance().font;
        if (DeployData.spawns.isEmpty()) {
            WDraw.emptyState(
                    g,
                    font,
                    area,
                    WarfareIcon.SPAWN_MAIN,
                    Component.translatable("gui.pwpwarfare.deploy.spawn.empty"),
                    Component.translatable("gui.pwpwarfare.deploy.spawn.empty_hint"),
                    o);
            return;
        }
        scroll.clamp(contentH(area), area.h());
        boolean inside = area.contains(mx, my);
        g.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        for (Card c : layout(area)) {
            if (c.rect().bottom() < area.y() || c.rect().y() > area.bottom()) continue;
            renderCard(g, font, c, inside && c.rect().contains(mx, my), selectedSpawn, o);
        }
        g.disableScissor();
        WDraw.scrollbar(g, area, contentH(area), scroll.offset(), o);
    }

    private void renderCard(GuiGraphics g, Font font, Card c, boolean hovered, String sel, float o) {
        DeployData.SpawnPoint sp = c.sp();
        Rect r = c.rect();
        boolean off = blocked(sp);
        float h = hover.get(sp.id(), hovered && !off);
        float s = select.get(sp.id(), sel.equals(sp.id()));
        WDraw.tile(g, r, h, s, off ? 0F : 1F, o);

        int pad = Spacing.XS;
        int is = r.h() - 2 * pad >= Spacing.ICON_SIZE + 2 * Spacing.XXS ? Spacing.ICON_SIZE : WTheme.ICON_ROW;
        Rect well = Rect.of(r.x() + pad + Spacing.ACCENT_BAR, r.y() + pad, r.h() - 2 * pad, r.h() - 2 * pad);
        WDraw.well(g, well, o);
        int ic = off ? Colors.TEXT_DIM : Colors.lerp(Colors.ACCENT, PWPTheme.Text.HEADING, s);
        WDraw.icon(g, typeIcon(sp.id()), well.centerX() - is / 2, well.centerY() - is / 2, is, UIDraw.fade(ic, o));

        Rect text = Rect.of(
                well.right() + Spacing.SM, r.y() + pad, r.right() - well.right() - Spacing.SM - pad, r.h() - 2 * pad);
        int lh = font.lineHeight + 1;
        Rect line1 = text.top(lh);
        Rect line2 = text.bottomStrip(lh);
        // строка 1: имя (квадрат) ... пилюля статуса справа
        Component status = statusText(sp.status());
        int pw = WDraw.pillWidth(font, status);
        boolean pillFits = text.w() > pw + Spacing.XXL;
        if (pillFits) WDraw.pill(g, font, line1.right() - pw, line1.y() - 1, status, statusColor(sp.status()), o);
        String kp = SquadMapRenderer.getKP(sp.pos().getX(), sp.pos().getZ());
        int nameColor = off ? Colors.TEXT_DIM : Colors.lerp(PWPTheme.Text.HEADING, PWPTheme.Colors.TEXT_ACCENT, s);
        WDraw.text(
                g,
                font,
                Component.literal(sp.name() + "  ")
                        .append(Component.literal(kp).withStyle(st -> st.withColor(PWPTheme.Text.MUTED))),
                line1.withWidth(line1.w() - (pillFits ? pw + Spacing.XS : 0)),
                Align.START,
                nameColor,
                false,
                o);
        // строка 2: статус (если не влез пилюлей) • припасы • дистанция
        Component info = pillFits ? Component.empty() : status.copy();
        if (sp.supplies() > 0) {
            if (!info.getString().isEmpty()) info = info.copy().append("  •  ");
            info = info.copy().append(Component.translatable("gui.pwpwarfare.deploy.spawn.supplies", sp.supplies()));
        }
        if (sp.distance() > 0) {
            if (!info.getString().isEmpty()) info = info.copy().append("  •  ");
            info = info.copy().append(Component.translatable("gui.pwpwarfare.deploy.spawn.distance", sp.distance()));
        }
        if (text.h() >= 2 * lh) {
            WDraw.text(
                    g,
                    font,
                    info,
                    line2,
                    Align.START,
                    pillFits ? PWPTheme.Text.MUTED : statusColor(sp.status()),
                    false,
                    o);
        }
    }

    /** id выбранной точки, "" — заблокированная (сброс выбора, как раньше), null — мимо. */
    public String mouseClicked(double mx, double my, int btn, Rect area) {
        if (btn != 0 || !area.contains(mx, my)) return null;
        for (Card c : layout(area)) {
            if (c.rect().contains(mx, my)) return blocked(c.sp()) ? "" : c.sp().id();
        }
        return null;
    }
}
