package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.client.gui.SquadMapRenderer;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;

/**
 * «Spawn Select» в Squad-стиле: БЛОК из двух видимых строк (первые 2 точки).
 * Если точек больше — ОТДЕЛЬНАЯ полоска-кнопка под блоком (на всю ширину),
 * клик по ней раскрывает остальные строки ПОВЕРХ чата (чат сжимается на
 * высоту расширения + высоту полоски). Клик по строке — выбор. Статус строки
 * right-align в боксе (не вылезает за край).
 */
public class SpawnPanel {

    private static final int ITEM_H = 24;

    /** Высота отдельной полоски-кнопки «ещё N» / «Свернуть» под блоком. */
    private static final int STRIP_H = 16;

    /** Зазор между блоком и полоской-кнопкой. */
    private static final int STRIP_GAP = 3;
    private static final int visibleRows() { return Math.min(2, DeployData.spawns.size()); }

    private final Anim.ExpandState expand = new Anim.ExpandState();
    private boolean open;

    /** Высота свёрнутого блока (2 видимые строки). */
    public int blockHeight() {
        return visibleRows() * ITEM_H + 2;
    }

    public int render(GuiGraphics gui, int x, int y, int w, int maxH, int mx, int my, String selectedSpawn) {
        var f = PWPTheme.Fonts.display();
        long now = System.currentTimeMillis();

        int rows = visibleRows();
        int collapsedH = rows * ITEM_H + 2;
        int extra = Math.max(0, DeployData.spawns.size() - rows);
        float pr = expand.progress(open, now, 140);
        int grow = (int) (extra * ITEM_H * pr);
        int totalH = collapsedH + grow;

        // ОДИН цельный бокс: свёрнутый блок + раскрытый список — это один бокс,
        // который расширяется вниз (а не «блок 2 строки + отдельная плашка»)
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + totalH;
        int bg = hover ? 0x2212151A : 0xE60E1117;
        RoundedRect.fill(gui, x, y, w, totalH, 5, bg);
        RoundedRect.border(gui, x, y, w, totalH, 5, 1, hover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER);

        gui.enableScissor(x, y, x + w, y + totalH);
        int cy = y + 1;
        for (int i = 0; i < DeployData.spawns.size(); i++) {
            renderItem(gui, x, cy, w, DeployData.spawns.get(i), mx, my, selectedSpawn);
            cy += ITEM_H;
        }
        gui.disableScissor();

        // ПОЛОСКА-КНОПКА под блоком (вариант B): отдельная маленькая плашка на
        // всю ширину бокса, своя рамка/ховер; клик по всей полосе — тумблер.
        int bottom = y + totalH;
        if (extra > 0) {
            bottom += STRIP_GAP;
            boolean stripHover = mx >= x && mx <= x + w && my >= bottom && my <= bottom + STRIP_H;
            RoundedRect.fill(gui, x, bottom, w, STRIP_H, 5, stripHover ? 0x3312151A : 0x2212151A);
            RoundedRect.border(gui, x, bottom, w, STRIP_H, 5, 1,
                stripHover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER);
            String label = open ? "Свернуть" : "ещё " + extra;
            gui.drawString(f, label, x + 8, bottom + 4,
                stripHover ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_SECONDARY, false);
            gui.drawString(f, open ? "\u25B2" : "\u25BC", x + w - 14, bottom + 4,
                stripHover ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_DIM, false);
            bottom += STRIP_H;
        }
        return bottom;
    }

    private void renderItem(GuiGraphics gui, int x, int y, int w,
                            DeployData.SpawnPoint sp, int mx, int my, String sel) {
        var f = PWPTheme.Fonts.display();
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + ITEM_H;
        boolean selected = sel != null && sel.equals(sp.id());
        boolean blocked = isBlocked(sp);

        int bg = selected ? 0x44C8812A : (hover && !blocked ? 0x22FFFFFF : 0);
        RoundedRect.fill(gui, x + 3, y, w - 6, ITEM_H - 2, 4, bg);
        if (selected) RoundedRect.border(gui, x + 3, y, w - 6, ITEM_H - 2, 4, 1, PWPTheme.Colors.ACCENT);

        gui.fill(x + 7, y + 3, x + 15, y + 11, 0xFF1A1E26);
        String letter = sp.id().startsWith("HUB") ? "F" : sp.id().startsWith("RALLY") ? "R" : "M";
        gui.drawCenteredString(f, letter, x + 11, y + 3, blocked ? PWPTheme.Colors.TEXT_DIM : PWPTheme.Colors.ACCENT);

        String gridCoord = SquadMapRenderer.getKP(sp.pos().getX(), sp.pos().getZ());
        String name = sp.name() + " (" + gridCoord + ")";

        String status = statusText(sp);
        int sc = statusColor(sp);
        if (sp.distance() > 0) status += "  " + sp.distance() + "м";
        // Правый край: статус выравнивается внутри бокса, не вылезает
        int lastX = x + w - 6;
        int statusX = lastX - f.width(status);
        int nameMaxX = statusX - 6;
        int maxW = Math.max(10, nameMaxX - (x + 22));
        if (f.width(name) > maxW) name = f.plainSubstrByWidth(name, maxW - 4) + "\u2026";

        int tc = blocked ? PWPTheme.Colors.TEXT_DIM : (selected ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Colors.TEXT_PRIMARY);
        gui.drawString(f, name, x + 20, y + 4, tc, false);
        gui.drawString(f, status, statusX, y + 5, sc, false);
    }

    private static boolean isBlocked(DeployData.SpawnPoint sp) {
        return sp.status() == DeployData.SpawnStatus.BLOCKED
            || sp.status() == DeployData.SpawnStatus.DESTROYED;
    }

    private static String statusText(DeployData.SpawnPoint sp) {
        return switch (sp.status()) {
            case SAFE, HEALTHY -> "БЕЗОПАСНО";
            case COOLDOWN -> "КД";
            case BLOCKED -> "ЗАБЛОКИРОВАНО";
            case DESTROYED -> "УНИЧТОЖЕНО";
        };
    }

    private static int statusColor(DeployData.SpawnPoint sp) {
        return switch (sp.status()) {
            case SAFE, HEALTHY -> PWPTheme.Colors.SUCCESS_LIGHT;
            case COOLDOWN -> PWPTheme.Colors.WARNING;
            default -> PWPTheme.Colors.DANGER;
        };
    }

    public String mouseClicked(double mx, double my, int btn, int x, int y, int w, int maxH) {
        if (btn != 0) return null;
        int rows = visibleRows();
        int extra = Math.max(0, DeployData.spawns.size() - rows);
        float pr = expand.progress(open, System.currentTimeMillis(), 140);
        int totalH = rows * ITEM_H + 2 + (int) (extra * ITEM_H * pr);

        // Полоска-кнопка снизу: клик в ЛЮБОМ месте полосы = раскрыть/свернуть
        if (extra > 0) {
            int sy = y + totalH + STRIP_GAP;
            if (mx >= x && mx <= x + w && my >= sy && my <= sy + STRIP_H) {
                open = !open;
                return "";
            }
        }

        if (mx < x || mx > x + w || my < y || my > y + totalH) return null;

        // Строки 1-2 — выбор спавна
        for (int i = 0; i < rows; i++) {
            if (my >= y + 1 + i * ITEM_H && my <= y + 1 + i * ITEM_H + ITEM_H) {
                DeployData.SpawnPoint sp = DeployData.spawns.get(i);
                if (!isBlocked(sp)) return sp.id();
                return "";
            }
        }
        // Раскрытый список
        if (open) {
            int ly = y + 1 + rows * ITEM_H + 2;
            for (int i = rows; i < DeployData.spawns.size(); i++) {
                if (my >= ly && my <= ly + ITEM_H) {
                    if (!isBlocked(DeployData.spawns.get(i))) return DeployData.spawns.get(i).id();
                    return "";
                }
                ly += ITEM_H;
            }
        }
        return null;
    }

    /**
     * Текущий прирост высоты раскрытого списка (с учётом анимации) — чат
     * вытесняется на эту высоту вниз. 0, если список закрыт. Совпадает с grow
     * в render(): блок расширяется как единый бокс.
     */
    public int openListHeight() {
        long now = System.currentTimeMillis();
        int extra = Math.max(0, DeployData.spawns.size() - visibleRows());
        if (extra <= 0) return 0;
        float pr = expand.progress(open, now, 140);
        if (pr <= 0.01f) return 0;
        return (int) (extra * ITEM_H * pr);
    }

    /**
     * Высота отдельной полоски-кнопки под блоком (зазор + плашка). 0, если
     * все точки помещаются в видимые строки. Чат учитывает её при расчёте
     * своей высоты (полоска всегда видна, когда список расширяем).
     */
    public int buttonStripHeight() {
        int extra = Math.max(0, DeployData.spawns.size() - visibleRows());
        return extra > 0 ? STRIP_GAP + STRIP_H : 0;
    }
}