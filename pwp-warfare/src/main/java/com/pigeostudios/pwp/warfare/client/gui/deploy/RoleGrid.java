package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.client.gui.ui.AnimMap;
import com.pigeostudios.pwp.warfare.client.gui.ui.Scroll;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Сетка ролей деплоя: секции по категориям (иконка и цвет категории), ячейки с иконкой кита,
 * занятостью «N/M» и замком недоступного кита. Лимиты/доступность — как пришли с сервера
 * ({@link DeployData.KitRecord#available()} / {@code reason()}), сама сетка ничего не считает.
 * Причина недоступности и описание роли — в тултипе.
 *
 * <p>Раскладка считается одной функцией {@link #layout} и для рисования, и для клика.
 */
@OnlyIn(Dist.CLIENT)
public class RoleGrid {
    /** Ячейка/заголовок секции в координатах экрана (с учётом прокрутки). */
    private record Cell(DeployData.KitRecord kit, Rect rect) {}

    private record Section(String category, Rect title) {}

    private record Layout(List<Section> sections, List<Cell> cells, int contentH) {}

    private final Scroll scroll = new Scroll();
    private final AnimMap hover = new AnimMap();
    private final AnimMap select = new AnimMap(PWPTheme.Animations.DURATION_NORMAL);
    private final Map<String, Boolean> iconExists = new HashMap<>();
    private String hoveredKit;
    private long hoverSince;

    public void scroll(double delta) {
        scroll.wheel(delta);
    }

    private static Map<String, List<DeployData.KitRecord>> categories() {
        Map<String, List<DeployData.KitRecord>> cats = new LinkedHashMap<>();
        for (DeployData.KitRecord k : DeployData.kits) {
            cats.computeIfAbsent(k.category(), c -> new ArrayList<>()).add(k);
        }
        return cats;
    }

    private Layout layout(Rect area, Font font) {
        int gap = Spacing.XXS;
        int cols =
                Grid.columnsFor(area.w() - WTheme.SCROLLBAR_W - gap, WTheme.ROLE_CELL_MIN, gap, WTheme.ROLE_COLS_MAX);
        int cellW = (area.w() - WTheme.SCROLLBAR_W - gap - (cols - 1) * gap) / Math.max(1, cols);
        int cell = Math.max(WTheme.ROLE_CELL_MIN, Math.min(WTheme.ROLE_CELL_MAX, cellW));
        int titleH = font.lineHeight + Spacing.XXS;
        List<Section> sections = new ArrayList<>();
        List<Cell> cells = new ArrayList<>();
        int y = area.y() - scroll.offset();
        int top = y;
        for (var e : categories().entrySet()) {
            sections.add(new Section(e.getKey(), Rect.of(area.x(), y, area.w() - WTheme.SCROLLBAR_W - gap, titleH)));
            y += titleH + Spacing.XXS;
            List<DeployData.KitRecord> list = e.getValue();
            Rect gridArea =
                    Rect.of(area.x(), y, cols * cell + (cols - 1) * gap, Grid.height(list.size(), cols, cell, gap));
            Rect[] rects = Grid.cells(gridArea, list.size(), cols, cell, gap);
            for (int i = 0; i < rects.length; i++) cells.add(new Cell(list.get(i), rects[i]));
            y += gridArea.h() + Spacing.SM;
        }
        return new Layout(sections, cells, Math.max(0, y - top - Spacing.SM));
    }

    public void render(GuiGraphics g, Rect area, int mx, int my, String selectedKit, float o) {
        Font font = Minecraft.getInstance().font;
        if (DeployData.kits.isEmpty()) {
            WDraw.emptyState(
                    g,
                    font,
                    area,
                    WarfareIcon.PERSON,
                    Component.translatable("gui.pwpwarfare.deploy.roles.empty"),
                    Component.translatable("gui.pwpwarfare.deploy.roles.empty_hint"),
                    o);
            return;
        }
        Layout l = layout(area, font);
        scroll.clamp(l.contentH(), area.h());
        g.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        for (Section s : l.sections()) {
            if (s.title().bottom() < area.y() || s.title().y() > area.bottom()) continue;
            UIIcon ci = UIIcon.forRoleCategory(s.category());
            int color = ci != null ? ci.roleColor() : Colors.ACCENT;
            Rect row = s.title();
            int tx = row.x();
            if (ci != null) {
                UIDraw.icon(
                        g, ci, tx, row.y() + (row.h() - WTheme.ICON_ROW) / 2, WTheme.ICON_ROW, UIDraw.fade(color, o));
                tx += WTheme.ICON_ROW + Spacing.XS;
            }
            WDraw.section(
                    g,
                    font,
                    Rect.of(tx, row.y(), row.right() - tx, row.h()),
                    Component.literal(DeployData.getDisplayName(s.category())),
                    color,
                    o);
        }
        String hov = null;
        DeployData.KitRecord hovKit = null;
        boolean inside = area.contains(mx, my);
        for (Cell c : l.cells()) {
            Rect r = c.rect();
            if (r.bottom() < area.y() || r.y() > area.bottom()) continue;
            boolean h = inside && r.contains(mx, my);
            if (h) {
                hov = c.kit().name();
                hovKit = c.kit();
            }
            renderCell(g, font, r, c.kit(), selectedKit, h, o);
        }
        g.disableScissor();
        WDraw.scrollbar(g, area, l.contentH(), scroll.offset(), o);

        if (hov == null || !hov.equals(hoveredKit)) hoverSince = System.currentTimeMillis();
        hoveredKit = hov;
        if (hovKit != null && System.currentTimeMillis() - hoverSince >= UITooltip.DELAY_MS) {
            List<Component> lines = new ArrayList<>();
            if (!hovKit.available() && !hovKit.reason().isEmpty()) {
                lines.add(Component.literal(hovKit.reason()).withStyle(s -> s.withColor(WTheme.STATUS_BAD)));
            }
            if (hovKit.maxInTeam() > 0) {
                lines.add(Component.translatable(
                        "gui.pwpwarfare.deploy.roles.occupancy", hovKit.inTeamCount(), hovKit.maxInTeam()));
            }
            if (hovKit.description() != null && !hovKit.description().isEmpty()) {
                lines.add(Component.literal(hovKit.description()));
            }
            var mc = Minecraft.getInstance();
            UITooltip.render(
                    g,
                    new UITooltip.Content(Component.literal(DeployData.getDisplayName(hovKit.name())), lines),
                    mx,
                    my,
                    mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight(),
                    o);
        }
    }

    private void renderCell(
            GuiGraphics g, Font font, Rect r, DeployData.KitRecord kit, String sel, boolean hovered, float o) {
        boolean avail = kit.available();
        float h = hover.get(kit.name(), hovered && avail);
        float s = select.get(kit.name(), sel.equals(kit.name()));
        WDraw.tile(g, r, h, s, avail ? 1F : 0F, o);

        UIIcon ci = UIIcon.forRoleCategory(kit.category());
        int catColor = ci != null ? ci.roleColor() : Colors.ACCENT;
        // цветная засечка категории в правом верхнем углу
        UIDraw.rect(
                g, Rect.of(r.right() - Spacing.XS - 1, r.y() + 1, Spacing.XS, Spacing.XXS), UIDraw.fade(catColor, o));

        boolean showCount = kit.maxInTeam() > 0 && r.h() >= WTheme.ROLE_CELL_MIN + Spacing.XS;
        int countH = showCount ? font.lineHeight : 0;
        int is = Math.max(Spacing.ICON_SIZE, Math.min(r.w(), r.h() - countH) - 2 * Spacing.XS);
        is = is >= Spacing.ICON_SIZE_LG
                ? Spacing.ICON_SIZE_LG
                : (is >= Spacing.ICON_SIZE_MED ? Spacing.ICON_SIZE_MED : Spacing.ICON_SIZE);
        int ix = r.centerX() - is / 2;
        int iy = r.y() + Math.max(Spacing.XXS, (r.h() - countH - is) / 2);
        int tint = avail ? Colors.lerp(PWPTheme.Text.BODY, PWPTheme.Colors.TEXT_ACCENT, s) : Colors.TEXT_DIM;
        ResourceLocation tex = kitIcon(kit.name());
        if (tex != null) {
            int size = is;
            UIDraw.blitTinted(
                    g,
                    UIDraw.fade(avail ? 0xFFFFFFFF : Colors.TEXT_SECONDARY, o),
                    () -> g.blit(tex, ix, iy, 0, 0, size, size, size, size));
        } else if (ci != null) {
            UIDraw.icon(g, ci, ix, iy, is, UIDraw.fade(avail ? catColor : Colors.TEXT_DIM, o));
        } else {
            String dn = DeployData.getDisplayName(kit.name());
            UIDraw.centered(
                    g,
                    font,
                    Component.literal(dn.isEmpty() ? "?" : dn.substring(0, 1)),
                    r.centerX(),
                    iy + is / 2,
                    UIDraw.fade(tint, o));
        }
        if (!avail) {
            UIDraw.rect(g, r.inset(1), UIDraw.fade(WTheme.DISABLED_VEIL, o));
            WDraw.icon(
                    g,
                    WarfareIcon.LOCK,
                    r.x() + Spacing.XXS,
                    r.y() + Spacing.XXS,
                    WTheme.ICON_SMALL,
                    UIDraw.fade(WTheme.STATUS_BAD, o));
        }
        if (showCount) {
            boolean full = kit.inTeamCount() >= kit.maxInTeam();
            Component cnt = Component.literal(kit.inTeamCount() + "/" + kit.maxInTeam());
            int cw = font.width(cnt);
            UIDraw.text(
                    g,
                    font,
                    cnt,
                    r.centerX() - cw / 2,
                    r.bottom() - font.lineHeight - 1,
                    UIDraw.fade(full ? WTheme.STATUS_BAD : PWPTheme.Text.MUTED, o),
                    false);
        }
    }

    /** Текстура иконки кита, если она есть в ресурсах (иначе — иконка категории). */
    private ResourceLocation kitIcon(String kitName) {
        String file = DeployData.kitIconFileName(kitName);
        ResourceLocation rl = ResourceLocation.tryBuild("pwpwarfare", "textures/gui/kits/" + file + ".png");
        if (rl == null) return null;
        boolean ok = iconExists.computeIfAbsent(file, f -> Minecraft.getInstance()
                .getResourceManager()
                .getResource(rl)
                .isPresent());
        return ok ? rl : null;
    }

    /**
     * Клик по ячейке. Возвращает имя доступного кита, "" — недоступный кит (как раньше), null —
     * мимо ячеек. Ячейка должна быть видна целиком.
     */
    public String mouseClicked(double mx, double my, int btn, Rect area) {
        if (btn != 0 || !area.contains(mx, my)) return null;
        Layout l = layout(area, Minecraft.getInstance().font);
        for (Cell c : l.cells()) {
            Rect r = c.rect();
            if (r.y() < area.y() || r.bottom() > area.bottom()) continue;
            if (r.contains(mx, my)) {
                if (c.kit().available()) {
                    DeployData.expandedSlots.clear();
                    return c.kit().name();
                }
                return "";
            }
        }
        return null;
    }
}
