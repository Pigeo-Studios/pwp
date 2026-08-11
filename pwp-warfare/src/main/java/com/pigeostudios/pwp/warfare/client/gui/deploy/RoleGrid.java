package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.*;

/**
 * Squad-стиль: каждая категория — заголовок + горизонтальные ряды иконок.
 * Иконки не меньше 22px; если ряд не помещается — перенос на следующий.
 * Высота бокса = ровно по контенту (заголовки + ряды). Sticky hover:
 * {@link #hoveredKit} живёт, пока не наведён другой кит.
 *
 * <p>Ховер по ВСЕМ ролям (включая недоступные) — превью лоадаута перестраивается.
 * Клик по недоступной ничего не выбирает. На иконках НИЧЕГО не пишется.
 * Тултип роли рисуется ОТДЕЛЬНО (последним слоем экрана) — методом
 * {@link #renderTooltip(gui)}.</p>
 */
public class RoleGrid {

    private static final int HEADER_H = 13;
    private static final int ICON_SIZE = 28;
    private static final int MIN_CELL = 22;
    private static final int GAP = 4;
    private static final Set<String> missingIcons = new HashSet<>();

    public String hoveredKit;
    private int scrollOff;
    private int scrollMax;

    // Роль под курсором в последнем render() — для тултипа на верхнем слое
    private DeployData.KitRecord tipKit;
    private int tipX, tipY, tipCell;

    private Map<String, List<DeployData.KitRecord>> cachedCats;
    private int cacheVersion = -1;

    private Map<String, List<DeployData.KitRecord>> getCategories() {
        if (cachedCats == null || DeployData.kits.hashCode() != cacheVersion) {
            cachedCats = new LinkedHashMap<>();
            for (DeployData.KitRecord k : DeployData.kits)
                cachedCats.computeIfAbsent(k.category(), c -> new ArrayList<>()).add(k);
            cacheVersion = DeployData.kits.hashCode();
        }
        return cachedCats;
    }

    /** Сколько иконок в ряду: адаптивно по ширине, минимум MIN_CELL на иконку. */
    private static int countPerRow(int count, int w) {
        int maxPerRow = Math.max(1, (w - 8 + GAP) / (MIN_CELL + GAP));
        return Math.min(count, maxPerRow);
    }

    /** Размер иконки: подогнать под perRow в ширину w, максимум ICON_SIZE, минимум MIN_CELL. */
    private static int cellSize(int w, int perRow) {
        return Math.max(MIN_CELL, Math.min(ICON_SIZE, (w - 8 - (perRow - 1) * GAP) / perRow));
    }

    /** Полная высота контента без учёта обрезки (для расчёта бокса ролей в DeployScreen).
     *  Считает по РЕАЛЬНОМУ cellSize(w, pr), как render() — иначе высота недооценивается
     *  (MIN_CELL=22 < фактических 24-28px), нижний ряд ролей режется скиссором, а
     *  спавн-бокс «заезжает» на иконки. */
    public int contentHeight(int w) {
        int h = 0;
        for (var e : getCategories().entrySet()) {
            int n = e.getValue().size();
            int pr = countPerRow(n, w);
            int rows = (n + pr - 1) / pr;
            int cell = cellSize(w, pr);
            h += HEADER_H + GAP;
            h += rows * (cell + GAP) - GAP;
        }
        return h;
    }

    public boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void render(GuiGraphics gui, int x, int y, int w, int maxH, int mx, int my, String selectedKit) {
        var f = PWPTheme.Fonts.display();
        int totalH = contentHeight(w);
        scrollMax = Math.max(0, totalH - maxH);
        scrollOff = Mth.clamp(scrollOff, 0, scrollMax);

        gui.enableScissor(x, y, x + w, y + maxH);
        int cy = y - scrollOff;
        boolean anyHover = false;

        for (var e : getCategories().entrySet()) {
            String catName = e.getKey();
            List<DeployData.KitRecord> list = e.getValue();
            int pr = countPerRow(list.size(), w);
            int cell = cellSize(w, pr);
            int rows = (list.size() + pr - 1) / pr;

            if (cy + HEADER_H > y && cy < y + maxH) {
                gui.fill(x, cy, x + w, cy + HEADER_H, 0x1412181A);
                gui.drawString(f, DeployData.getDisplayName(catName), x + 4, cy + 2,
                    PWPTheme.Colors.TEXT_ACCENT, false);
            }
            cy += HEADER_H + GAP;

            for (int r = 0; r < rows; r++) {
                int rowY = cy + r * (cell + GAP);
                if (rowY + cell > y && rowY < y + maxH) {
                    for (int ci = 0; ci < pr; ci++) {
                        int idx = r * pr + ci;
                        if (idx >= list.size()) break;
                        DeployData.KitRecord kit = list.get(idx);
                        int cx = x + 4 + ci * (cell + GAP);
                        boolean sel = selectedKit.equals(kit.name());
                        boolean hover = mx >= cx && mx <= cx + cell && my >= rowY && my <= rowY + cell;
                        // Ховер по ВСЕМ ролям (недоступные тоже перестраивают превью лоадаута)
                        if (hover) { anyHover = true; hoveredKit = kit.name(); }
                        if (hover) { tipKit = kit; tipX = cx; tipY = rowY; tipCell = cell; }

                        int bg = sel ? 0x44C8812A : (hover ? 0x2212151A : 0);
                        if (bg != 0) RoundedRect.fill(gui, cx, rowY, cell, cell, 4, bg);
                        // Чёткая обводка выбранной роли (авто-скролла нет — только маркер)
                        if (sel) RoundedRect.border(gui, cx, rowY, cell, cell, 4, 2, PWPTheme.Colors.ACCENT);

                        int iconSize = Math.max(12, cell - 6);
                        int iconX = cx + (cell - iconSize) / 2;
                        int iconY = rowY + (cell - iconSize) / 2 - 1;
                        RenderSystem.enableBlend();
                        if (!kit.available()) RenderSystem.setShaderColor(0.45f, 0.45f, 0.45f, 1f);
                        String iconName = DeployData.kitIconFileName(kit.name());
                        if (!missingIcons.contains(iconName)) {
                            ResourceLocation ic = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + iconName + ".png");
                            if (Minecraft.getInstance().getResourceManager().getResource(ic).isPresent()) {
                                gui.blit(ic, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                            } else {
                                missingIcons.add(iconName);
                                drawIconFallback(gui, f, kit, cx, rowY, cell);
                            }
                        } else {
                            drawIconFallback(gui, f, kit, cx, rowY, cell);
                        }
                        RenderSystem.setShaderColor(1, 1, 1, 1);
                    }
                }
            }
            cy += rows * (cell + GAP) - GAP;

            // Причина недоступности выбранной роли — справа от заголовка
            for (DeployData.KitRecord kit : list) {
                if (selectedKit.equals(kit.name()) && !kit.available()) {
                    String reason = kit.reason() != null && !kit.reason().isEmpty() ? kit.reason() : "НЕДОСТУПНО";
                    gui.drawString(f, reason, x + w - f.width(reason) - 6, y + 2,
                        PWPTheme.Colors.DANGER, false);
                }
            }
        }
        gui.disableScissor();
        // Тултип живёт только пока курсор НАД иконкой: ушёл — тултип гаснет
        // (иначе «висел» вечно и вылезал на других вкладках)
        if (!anyHover) tipKit = null;
    }

    /** Короткий тултип роли — вызывается ПОСЛЕДНИМ слоем в DeployScreen.render(). */
    public void renderTooltip(GuiGraphics gui) {
        if (tipKit == null) return;
        var f = PWPTheme.Fonts.display();
        DeployData.KitRecord kit = tipKit;
        int cx = tipX, cy = tipY, cell = tipCell;

        List<String> lines = new ArrayList<>();
        lines.add(DeployData.getDisplayName(kit.name()));
        if (kit.available()) lines.add("ДОСТУПНО");
        else lines.add(kit.reason() != null && !kit.reason().isEmpty() ? kit.reason() : "НЕДОСТУПНО");

        int w = 0;
        for (String ln : lines) w = Math.max(w, f.width(ln) + 16);
        int h = lines.size() * 10 + 7;

        int tx = cx + cell + 3;
        int ty = cy + cell + 3;
        int gw = gui.guiWidth(), gh = gui.guiHeight();
        if (tx + w > gw - 2) tx = Math.max(2, cx - w - 3);
        if (ty + h > gh - 2) ty = Math.max(2, cy - h - 3);

        gui.fill(tx, ty, tx + w, ty + h, 0xE60E1117);
        gui.renderOutline(tx, ty, w, h, PWPTheme.Colors.BORDER);
        int ly = ty + 4;
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            int col = i == 0 ? PWPTheme.Colors.TEXT_ACCENT
                : (kit.available() ? PWPTheme.Colors.SUCCESS : PWPTheme.Colors.DANGER);
            gui.drawString(f, ln, tx + 4, ly, col, false);
            ly += 10;
        }
    }

    public String mouseClicked(double mx, double my, int btn, int x, int y, int w, int maxH) {
        if (btn != 0) return null;
        int cy = y - scrollOff;
        for (var c : getCategories().entrySet()) {
            var list = c.getValue();
            int pr = countPerRow(list.size(), w);
            int cell = cellSize(w, pr);
            int rows = (list.size() + pr - 1) / pr;
            cy += HEADER_H + GAP;
            for (int r = 0; r < rows; r++) {
                int rowY = cy + r * (cell + GAP);
                for (int ci = 0; ci < pr; ci++) {
                    int idx = r * pr + ci;
                    if (idx >= list.size()) break;
                    DeployData.KitRecord kit = list.get(idx);
                    int cx = x + 4 + ci * (cell + GAP);
                    if (mx >= cx && mx <= cx + cell && my >= rowY && my <= rowY + cell) {
                        // Клик по недоступной роли — ничего не выбираем (только ховер-превью)
                        if (!kit.available()) return "";
                        DeployData.expandedSlots.clear();
                        hoveredKit = kit.name();
                        return kit.name();
                    }
                }
            }
            cy += rows * (cell + GAP) - GAP;
        }
        return null;
    }

    public boolean mouseScrolled(double mx, double my, double delta, int x, int y, int w, int maxH) {
        if (!isHovered(mx, my, x, y, w, maxH)) return false;
        scrollOff = Mth.clamp(scrollOff - (int) delta * 16, 0, scrollMax);
        return true;
    }

    private void drawIconFallback(GuiGraphics gui, Font f, DeployData.KitRecord kit, int cx, int cy, int cell) {
        String name = DeployData.getDisplayName(kit.name());
        String letter = name.isEmpty() ? "?" : name.substring(0, 1);
        gui.drawCenteredString(f, letter, cx + cell / 2, cy + cell / 2 - 3,
            kit.available() ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM);
    }
}
