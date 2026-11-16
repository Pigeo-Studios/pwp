package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.client.gui.ui.AnimMap;
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
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Снаряжение выбранного кита: слоты (СТВОЛ/ВТОРИЧКА/ГРАНАТЫ/СПЕЦ) плашкой с предметом,
 * альтернативы раскрываются при наведении (или закреплены кликом), рюкзак — рядом иконок,
 * внизу — описание роли. Поведение кликов прежнее: клик по плашке закрепляет/переключает
 * раскрытие, клик по альтернативе — выбор (DeployData.setSelectedIndex).
 *
 * <p>Раскладка — одна функция {@link #layout} (зависит от позиции мыши: раскрытие по наведению),
 * общая для рисования и клика.
 */
@OnlyIn(Dist.CLIENT)
public class LoadoutPanel {
    private record Alt(int index, Rect rect) {}

    private record SlotRow(
            DeployData.LoadoutSlot slot,
            Rect label,
            Rect bar,
            Rect region,
            List<Alt> alts,
            boolean expanded,
            boolean manual) {}

    private record Layout(DeployData.KitRecord kit, Rect title, List<SlotRow> rows, Rect description) {}

    private final AnimMap expand = new AnimMap();
    private final AnimMap hover = new AnimMap();

    private static Optional<DeployData.KitRecord> kit(String name) {
        return DeployData.kits.stream().filter(k -> k.name().equals(name)).findFirst();
    }

    static Component slotLabel(String label) {
        return switch (label) {
            case "PRIMARY", "SECONDARY", "THROWABLE", "SPECIAL", "BACKPACK" -> Component.translatable(
                    "gui.pwpwarfare.deploy.slot." + label.toLowerCase());
            default -> Component.literal(label);
        };
    }

    private Layout layout(Rect area, String selectedKit, double mx, double my) {
        var ko = kit(selectedKit);
        if (ko.isEmpty()) return null;
        DeployData.KitRecord kit = ko.get();
        Rect title = area.top(WTheme.SLOT_BAR_H + Spacing.XXS);
        int y = title.bottom() + Spacing.XS;
        List<SlotRow> rows = new ArrayList<>();
        for (DeployData.LoadoutSlot slot : kit.loadout()) {
            if (y + WTheme.SLOT_LABEL_H + WTheme.SLOT_BAR_H > area.bottom()) break;
            Rect label = Rect.of(area.x(), y, area.w(), WTheme.SLOT_LABEL_H);
            Rect bar = Rect.of(area.x(), label.bottom(), area.w(), WTheme.SLOT_BAR_H);
            y = bar.bottom() + WTheme.SLOT_GAP;
            boolean pack = slot.label().equals("BACKPACK");
            boolean hasAlt = !pack && slot.hasAlternatives();
            int altsH = hasAlt ? slot.options().size() * (WTheme.SLOT_ALT_H + 1) + Spacing.XXS : 0;
            Rect region = Rect.of(area.x(), label.y(), area.w(), bar.bottom() - label.y() + altsH);
            boolean manual = hasAlt && DeployData.isSlotExpanded(kit.name(), slot.label());
            boolean expanded = hasAlt && (manual || region.contains(mx, my));
            List<Alt> alts = new ArrayList<>();
            if (expanded) {
                int ay = bar.bottom() + 1;
                for (int i = 0; i < slot.options().size(); i++) {
                    alts.add(new Alt(i, Rect.of(area.x() + Spacing.SM, ay, area.w() - Spacing.SM, WTheme.SLOT_ALT_H)));
                    ay += WTheme.SLOT_ALT_H + 1;
                }
                y = ay + Spacing.XXS + WTheme.SLOT_GAP;
            }
            rows.add(new SlotRow(slot, label, bar, region, alts, expanded, manual));
        }
        Rect desc = Rect.of(area.x(), y + Spacing.XS, area.w(), Math.max(0, area.bottom() - y - Spacing.XS));
        return new Layout(kit, title, rows, desc);
    }

    public void render(GuiGraphics g, Rect area, int mx, int my, String selectedKit, float o) {
        Font font = Minecraft.getInstance().font;
        Layout l = layout(area, selectedKit, mx, my);
        if (l == null) {
            WDraw.emptyState(
                    g,
                    font,
                    area,
                    WarfareIcon.BACKPACK,
                    Component.translatable("gui.pwpwarfare.deploy.loadout.none"),
                    Component.translatable("gui.pwpwarfare.deploy.loadout.none_hint"),
                    o);
            return;
        }
        DeployData.KitRecord kit = l.kit();
        boolean avail = kit.available();
        // шапка кита
        Rect t = l.title();
        WDraw.well(g, t, o);
        UIDraw.accentBar(
                g,
                t.x(),
                t.y(),
                t.h(),
                Spacing.ACCENT_BAR_SELECTED,
                UIDraw.fade(avail ? Colors.ACCENT : WTheme.STATUS_BAD, o));
        Component state = avail
                ? Component.translatable("gui.pwpwarfare.deploy.loadout.title")
                : Component.translatable("gui.pwpwarfare.deploy.loadout.unavailable");
        int sw = WDraw.pillWidth(font, state);
        boolean pill = t.w() > sw + Spacing.XXL * 2;
        if (pill)
            WDraw.pill(
                    g,
                    font,
                    t.right() - sw - Spacing.XS,
                    t.centerY() - WDraw.pillHeight(font) / 2,
                    state,
                    avail ? PWPTheme.Text.MUTED : WTheme.STATUS_BAD,
                    o);
        WDraw.text(
                g,
                font,
                Component.literal(DeployData.getDisplayName(kit.name())),
                t.inset(Spacing.SM, 0, pill ? sw + Spacing.SM * 2 : Spacing.XS, 0),
                Align.START,
                avail ? PWPTheme.Colors.TEXT_ACCENT : WTheme.STATUS_BAD,
                true,
                o);

        for (SlotRow row : l.rows()) renderSlot(g, font, kit, row, mx, my, o);

        Rect d = l.description();
        if (d.h() > font.lineHeight * 2
                && kit.description() != null
                && !kit.description().isEmpty()) {
            UIDraw.hline(g, d.x(), d.right(), d.y(), UIDraw.fade(PWPTheme.Styles.Glass.BORDER, o * 0.5F));
            WDraw.paragraph(
                    g,
                    font,
                    Component.literal(kit.description()),
                    d.inset(Spacing.XXS, Spacing.XS, Spacing.XXS, 0),
                    PWPTheme.Spacing.REASON_LINES_MAX,
                    PWPTheme.Text.MUTED,
                    o);
        }
    }

    private void renderSlot(GuiGraphics g, Font font, DeployData.KitRecord kit, SlotRow row, int mx, int my, float o) {
        DeployData.LoadoutSlot slot = row.slot();
        String key = kit.name() + ":" + slot.label();
        WDraw.text(
                g,
                font,
                slotLabel(slot.label()),
                row.label().inset(Spacing.XXS, 0, 0, 0),
                Align.START,
                PWPTheme.Text.CAPTION,
                false,
                o);
        Rect bar = row.bar();
        if (slot.label().equals("BACKPACK")) {
            WDraw.well(g, bar, o);
            int ix = bar.x() + Spacing.XXS;
            int iy = bar.centerY() - WTheme.ITEM_SIZE / 2;
            for (var opt : slot.options()) {
                if (ix + WTheme.ITEM_SIZE > bar.right() - Spacing.XXS) break;
                g.renderFakeItem(opt.stack(), ix, iy);
                g.renderItemDecorations(font, opt.stack(), ix, iy);
                ix += WTheme.ITEM_SIZE + Spacing.XXS;
            }
            return;
        }
        boolean hasAlt = slot.hasAlternatives();
        int sel = selected(kit, slot);
        DeployData.LoadoutOption opt = slot.options().get(sel);
        float hv = hover.get(key, hasAlt && bar.contains(mx, my));
        float ex = expand.get(key, row.expanded());
        WDraw.tile(g, bar, hv, row.manual() ? 1F : ex * 0.5F, 1F, o);
        int iy = bar.centerY() - WTheme.ITEM_SIZE / 2;
        g.renderFakeItem(opt.stack(), bar.x() + Spacing.XS + Spacing.ACCENT_BAR, iy);
        int right = bar.right() - Spacing.XS;
        if (hasAlt) {
            WarfareIcon arrow = row.expanded() ? WarfareIcon.MINUS : WarfareIcon.PLUS;
            WDraw.icon(
                    g,
                    arrow,
                    right - WTheme.ICON_SMALL,
                    bar.centerY() - WTheme.ICON_SMALL / 2,
                    WTheme.ICON_SMALL,
                    UIDraw.fade(Colors.lerp(PWPTheme.Text.MUTED, Colors.ACCENT, Math.max(hv, ex)), o));
            right -= WTheme.ICON_SMALL + Spacing.XS;
        }
        int count = opt.stack().getCount();
        if (count > 0 && (slot.label().equals("PRIMARY") || slot.label().equals("SECONDARY"))) {
            Component c = Component.literal("×" + count);
            int cw = font.width(c);
            UIDraw.text(
                    g,
                    font,
                    c,
                    right - cw,
                    bar.centerY() - font.lineHeight / 2 + 1,
                    UIDraw.fade(PWPTheme.Colors.TEXT_ACCENT, o),
                    false);
            right -= cw + Spacing.XS;
        }
        int tx = bar.x() + Spacing.XS + Spacing.ACCENT_BAR + WTheme.ITEM_SIZE + Spacing.XS;
        WDraw.text(
                g,
                font,
                Component.literal(opt.name()),
                Rect.of(tx, bar.y(), right - tx, bar.h()),
                Align.START,
                PWPTheme.Text.BODY,
                false,
                o);

        if (!row.alts().isEmpty()) {
            float a = o * ex;
            for (Alt alt : row.alts()) {
                Rect r = alt.rect();
                boolean isSel = alt.index() == sel;
                float ah = hover.get(key + "#" + alt.index(), r.contains(mx, my));
                WDraw.tile(g, r, ah, isSel ? 1F : 0F, 1F, a);
                DeployData.LoadoutOption ao = slot.options().get(alt.index());
                g.renderFakeItem(ao.stack(), r.x() + Spacing.XS, r.centerY() - WTheme.ITEM_SIZE / 2);
                int atx = r.x() + Spacing.XS + WTheme.ITEM_SIZE + Spacing.XS;
                WDraw.text(
                        g,
                        font,
                        Component.literal(ao.name()),
                        Rect.of(atx, r.y(), r.right() - atx - Spacing.XS, r.h()),
                        Align.START,
                        isSel ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Text.BODY,
                        false,
                        a);
            }
        }
    }

    private static int selected(DeployData.KitRecord kit, DeployData.LoadoutSlot slot) {
        int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
        if (sel < 0 || sel >= slot.options().size()) sel = slot.defaultIndex();
        return Math.max(0, Math.min(slot.options().size() - 1, sel));
    }

    public boolean mouseClicked(double mx, double my, int btn, Rect area, String selectedKit) {
        if (btn != 0) return false;
        Layout l = layout(area, selectedKit, mx, my);
        if (l == null) return false;
        String kitName = l.kit().name();
        for (SlotRow row : l.rows()) {
            String label = row.slot().label();
            for (Alt alt : row.alts()) {
                if (alt.rect().contains(mx, my)) {
                    DeployData.setSelectedIndex(kitName, label, alt.index());
                    if (!row.manual()) DeployData.setSlotExpanded(kitName, label, true);
                    return true;
                }
            }
            boolean hasAlt = !label.equals("BACKPACK") && row.slot().hasAlternatives();
            Rect head = Rect.of(
                    row.label().x(),
                    row.label().y(),
                    row.bar().w(),
                    row.bar().bottom() - row.label().y());
            if (hasAlt && head.contains(mx, my)) {
                if (row.manual()) DeployData.toggleSlotExpanded(kitName, label);
                else DeployData.setSlotExpanded(kitName, label, true);
                return true;
            }
        }
        return false;
    }
}
