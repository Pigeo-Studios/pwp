package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Squad-стиль: левая колонка — PRIMARY (большая карточка), SECONDARY+SPECIAL (пара),
 * РЮКЗАК (мини-строки). Правая половина — 3D-кукла солдата.
 *
 * <p>Единая геометрия: {@link #layout} строит список строк (Row), рендер/ховер/клики
 * ходят по одним и тем же строкам — зоны не могут разъехаться. ВАРИАНТЫ оружия
 * выбираются ТОЛЬКО в 3D-тултипе (карточки вариантов в WeaponTooltipRenderer) —
 * раскрывающихся списков и «шевронов» на карточках больше нет.</p>
 */
public class LoadoutPanel {

    private static final int GAP = 3;
    private static final int PRIMARY_H = 76, PAIR_H = 30, BACKPACK_CELL_H = 30, BACKPACK_COLS = 4;
    private static final float PREVIEW_YAW = 0f, PREVIEW_PITCH = 0f;

    private final WeaponPreviewRenderer preview = new WeaponPreviewRenderer();
    private final PortraitRenderer portrait = new PortraitRenderer();

    public PortraitRenderer portrait() { return portrait; }

    /** Строка раскладки: слоты на одном Y (левая колонка). */
    private static final class Row {
        final int y, h;
        final List<Integer> slots = new ArrayList<>();
        Row(int y, int h) { this.y = y; this.h = h; }
    }

    private static boolean isPair(String label) {
        return label.equals("SECONDARY") || label.equals("SPECIAL");
    }

    /** Информация о слоте под курсором для тултипа (PRIMARY/SECONDARY/SPECIAL). */
    public record SlotInfo(String kitName, String slotLabel, DeployData.LoadoutSlot slot, int selectedIndex) {}

    /** Левая колонка: ширина = gearW (из totalW вычитается доля куклы). */
    private List<Row> layout(DeployData.KitRecord kit, int x, int y, int gearW) {
        List<Row> rows = new ArrayList<>();
        int cy = y;
        int n = kit.loadout().size();
        for (int i = 0; i < n; i++) {
            DeployData.LoadoutSlot slot = kit.loadout().get(i);
            int h;
            if (slot.label().equals("BACKPACK")) h = backpackGridHeight(slot.options().size());
            else if (slot.label().equals("PRIMARY")) h = PRIMARY_H;
            else if (isPair(slot.label()) && i + 1 < n && isPair(kit.loadout().get(i + 1).label())) {
                Row pair = new Row(cy, PAIR_H);
                pair.slots.add(i);
                pair.slots.add(i + 1);
                rows.add(pair);
                cy += PAIR_H + GAP;
                i++;
                continue;
            } else h = 26;

            Row row = new Row(cy, h);
            row.slots.add(i);
            rows.add(row);
            cy += h + GAP;
        }
        return rows;
    }

    /** Высота сетки рюкзака: подсказка-заголовок + ряды ячеек (4 в ряд). */
    private static int backpackGridHeight(int itemCount) {
        int rows = Math.max(1, (itemCount + BACKPACK_COLS - 1) / BACKPACK_COLS);
        return 14 + rows * (BACKPACK_CELL_H + GAP);
    }

    private static int backpackCellW(int w) {
        return (w - (BACKPACK_COLS - 1) * GAP) / BACKPACK_COLS;
    }

    /** gearRatio: доля панели под снаряжение (0.0-1.0), остальное — кукла. */
    private static final float GEAR_RATIO = 0.54f;

    public void render(GuiGraphics gui, int x, int y, int totalW, int maxH, int mx, int my, String selectedKit) {
        var f = PWPTheme.Fonts.display();
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        // Китов по имени нет (устаревший hoveredKit/роль снята) — НЕ рисуем пустую
        // панель: показываем первый доступный «одетый» кит, а не сухой прямоугольник.
        if (kitO.isEmpty()) {
            kitO = DeployData.kits.stream().filter(DeployData.KitRecord::available).findFirst();
        }
        if (kitO.isEmpty()) return;
        var kit = kitO.get();

        int gearW = (int)(totalW * GEAR_RATIO);
        int dollW = totalW - gearW - 2;
        int dollX = x + gearW + 2;

        // Заголовок
        boolean avail = kit.available();
        gui.drawString(f, DeployData.getDisplayName(kit.name()), x + 4, y,
            avail ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Colors.DANGER, false);
        int conY = y + 12;

        // ── Левая колонка: снаряжение ──
        List<Row> rows = layout(kit, x, conY, gearW);

        for (Row row : rows) {
            if (row.y + row.h > conY + maxH) break;
            if (row.y < conY) continue;

            if (row.slots.size() > 1) {
                // Пара: SECONDARY + SPECIAL — две карточки бок о бок
                int cw = (gearW - GAP) / 2;
                int xx = x;
                for (int si : row.slots) {
                    DeployData.LoadoutSlot slot = kit.loadout().get(si);
                    renderMiniCard(gui, xx, row.y, cw, row.h, kit, slot, mx, my, f);
                    xx += cw + GAP;
                }
            } else {
                DeployData.LoadoutSlot slot = kit.loadout().get(row.slots.get(0));
                if (slot.label().equals("BACKPACK")) {
                    renderBackpackGrid(gui, x, row.y, gearW, kit, slot, mx, my, f);
                } else if (slot.label().equals("PRIMARY")) {
                    renderPrimaryCard(gui, x, row.y, gearW, row.h, kit, slot, mx, my, f);
                } else {
                    renderCompactRow(gui, x, row.y, gearW, row.h, kit, slot, mx, my, f);
                }
            }
        }

        // ── Правая половина: 3D-кукла ──
        if (dollW >= 60 && maxH >= 100) {
            RoundedRect.fill(gui, dollX, conY, dollW, maxH - 12, 6, 0xFF0E1117);
            RoundedRect.border(gui, dollX, conY, dollW, maxH - 12, 6, 1, PWPTheme.Colors.BORDER);
            portrait.render(gui, dollX, conY, dollW, maxH - 12, mx, my,
                kit.description(), getPrimaryStack(kit), kit.armor());
        }
    }

    private ItemStack getPrimaryStack(DeployData.KitRecord kit) {
        for (DeployData.LoadoutSlot slot : kit.loadout()) {
            if (!slot.label().equals("PRIMARY")) continue;
            int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
            if (sel < 0 || sel >= slot.options().size()) sel = slot.defaultIndex();
            if (sel >= 0 && sel < slot.options().size()) return slot.options().get(sel).stack();
        }
        return ItemStack.EMPTY;
    }

    // ── PRIMARY: большая карточка с 3D-превью (side-profile вправо) ──
    private void renderPrimaryCard(GuiGraphics gui, int x, int y, int w, int h,
                                   DeployData.KitRecord kit, DeployData.LoadoutSlot slot,
                                   int mx, int my, Font f) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
        if (sel < 0 || sel >= slot.options().size()) sel = slot.defaultIndex();
        DeployData.LoadoutOption opt = slot.options().get(sel);

        RoundedRect.fill(gui, x, y, w, h, 6, hover ? 0x2212151A : PWPTheme.Colors.SURFACE);
        RoundedRect.border(gui, x, y, w, h, 6, 1, hover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER);
        gui.drawString(f, slotLabel(slot.label()), x + 6, y + 3, PWPTheme.Colors.TEXT_DIM, false);

        // Витрина PRIMARY ~2:1 (135×66 при PRIMARY_H=76): компактные стволы не упираются
        // в высоту бокса, как в старом 80×40 (инцидент 13.08.2026 «мелкие стволы мелкие»).
        int pvW = Math.min(135, w / 2);
        preview.render(gui, opt.stack(), x + 10 + pvW / 2, y + h / 2 + 2, pvW, h - 10,
            PREVIEW_YAW, PREVIEW_PITCH);
        int tx = x + pvW + 16;
        drawOptionName(gui, f, opt, tx, y + 14, w - (tx - x) - 16);
        if (slot.hasAlternatives()) {
            gui.drawString(f, "\u25B8 " + slot.options().size() + "\u00D7", x + w - 36, y + 16,
                hover ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_DIM, false);
        }
    }

    // ── SECONDARY / SPECIAL: компактная парная карточка ──
    private void renderMiniCard(GuiGraphics gui, int x, int y, int w, int h,
                                DeployData.KitRecord kit, DeployData.LoadoutSlot slot,
                                int mx, int my, Font f) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
        if (sel < 0 || sel >= slot.options().size()) sel = slot.defaultIndex();
        DeployData.LoadoutOption opt = slot.options().get(sel);

        RoundedRect.fill(gui, x, y, w, h, 5, hover ? 0x2212151A : PWPTheme.Colors.SURFACE);
        RoundedRect.border(gui, x, y, w, h, 5, 1, hover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER);
        gui.drawString(f, slotLabel(slot.label()), x + 4, y + 1, PWPTheme.Colors.TEXT_DIM, false);

        // Пустой слот (вторичка/спец без предмета) — пустая ячейка с «—»
        if (opt.stack().isEmpty()) {
            gui.drawCenteredString(f, "\u2014", x + w / 2, y + h / 2 - 3, PWPTheme.Colors.TEXT_DIM);
            return;
        }

        gui.renderFakeItem(opt.stack(), x + 4, y + 11);
        String name = opt.name();
        int maxW = w - 30;
        if (f.width(name) > maxW) name = f.plainSubstrByWidth(name, maxW - 4) + "\u2026";
        gui.drawString(f, name, x + 20, y + 13, PWPTheme.Colors.TEXT_PRIMARY, false);
        if (slot.hasAlternatives()) {
            gui.drawString(f, "\u25B8", x + w - 10, y + 13,
                hover ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_DIM, false);
        }
    }

    // ── Компактная строка для не-основных одиночных слотов ──
    private void renderCompactRow(GuiGraphics gui, int x, int y, int w, int h,
                                  DeployData.KitRecord kit, DeployData.LoadoutSlot slot,
                                  int mx, int my, Font f) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
        if (sel < 0 || sel >= slot.options().size()) sel = slot.defaultIndex();
        DeployData.LoadoutOption opt = slot.options().get(sel);

        int bg = hover ? 0x1812151A : 0;
        if (bg != 0) gui.fill(x, y, x + w, y + h, bg);
        gui.drawString(f, slotLabel(slot.label()), x + 4, y + 3, PWPTheme.Colors.TEXT_DIM, false);
        if (opt.stack().isEmpty()) {
            gui.drawCenteredString(f, "\u2014", x + w / 2, y + 3, PWPTheme.Colors.TEXT_DIM);
            return;
        }
        gui.renderFakeItem(opt.stack(), x + w - 22, y + 1);
        String name = opt.name();
        int maxW = w - f.width(slotLabel(slot.label())) - 30;
        if (f.width(name) > maxW) name = f.plainSubstrByWidth(name, maxW - 4) + "\u2026";
        gui.drawString(f, name, x + 4 + f.width(slotLabel(slot.label())) + 6, y + 3,
            PWPTheme.Colors.TEXT_PRIMARY, false);
        if (slot.hasAlternatives()) {
            gui.drawString(f, "\u25B8", x + w - 10, y + 3,
                hover ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_DIM, false);
        }
    }

    // ── РЮКЗАК: сетка ячеек 4 в ряд (каждая = 1/4 ширины PRIMARY), следующие слоты ниже.
    // Без текста: только иконка по центру + количество в углу. ──
    private void renderBackpackGrid(GuiGraphics gui, int x, int y, int w,
                                    DeployData.KitRecord kit, DeployData.LoadoutSlot slot,
                                    int mx, int my, Font f) {
        gui.drawString(f, "РЮКЗАК", x + 2, y + 1, PWPTheme.Colors.TEXT_DIM, false);
        gui.fill(x, y + 11, x + w, y + 12, PWPTheme.Colors.BORDER);

        int cw = backpackCellW(w);
        int gy = y + 14;

        int i = 0;
        for (DeployData.LoadoutOption opt : slot.options()) {
            int r = i / BACKPACK_COLS, c = i % BACKPACK_COLS;
            int cx = x + c * (cw + GAP), cy = gy + r * (BACKPACK_CELL_H + GAP);
            boolean hover = mx >= cx && mx <= cx + cw && my >= cy && my <= cy + BACKPACK_CELL_H;

            RoundedRect.fill(gui, cx, cy, cw, BACKPACK_CELL_H, 4,
                hover ? 0x2212151A : PWPTheme.Colors.SURFACE);
            RoundedRect.border(gui, cx, cy, cw, BACKPACK_CELL_H, 4, 1,
                hover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER);

            if (!opt.stack().isEmpty()) {
                gui.renderFakeItem(opt.stack(), cx + (cw - 16) / 2, cy + 2);
            }
            String cnt = "x" + Math.max(1, opt.stack().getCount());
            gui.drawString(f, cnt, cx + cw - f.width(cnt) - 3, cy + BACKPACK_CELL_H - 10,
                PWPTheme.Colors.TEXT_SECONDARY, false);
            i++;
        }
    }

    /** Клик по ячейке рюкзака: индекс выбранного варианта. */
    private boolean backpackCellClicked(int x, int y, int w,
                                        DeployData.KitRecord kit, DeployData.LoadoutSlot slot,
                                        double mx, double my) {
        int cw = backpackCellW(w);
        int gy = y + 14;
        int i = 0;
        for (DeployData.LoadoutOption opt : slot.options()) {
            int r = i / BACKPACK_COLS, c = i % BACKPACK_COLS;
            int cx = x + c * (cw + GAP), cy = gy + r * (BACKPACK_CELL_H + GAP);
            if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + BACKPACK_CELL_H) {
                DeployData.setSelectedIndex(kit.name(), slot.label(), i);
                return true;
            }
            i++;
        }
        return false;
    }

    private void drawOptionName(GuiGraphics gui, Font f, DeployData.LoadoutOption opt, int x, int y, int maxW) {
        String name = opt.name();
        if (f.width(name) > maxW) name = f.plainSubstrByWidth(name, maxW - 4) + "\u2026";
        gui.drawString(f, name, x, y, PWPTheme.Colors.TEXT_PRIMARY, false);
    }

    private static String slotLabel(String label) {
        return switch (label) {
            case "PRIMARY" -> "СТВОЛ";
            case "SECONDARY" -> "ВТОРИЧКА";
            case "THROWABLE" -> "ГРАНАТЫ";
            case "SPECIAL" -> "СПЕЦ";
            default -> label;
        };
    }

    // ── Ховер: слот-инфа для 3D-тултипа вариантов ──

    private boolean isWeaponSlot(String label) {
        return label.equals("PRIMARY") || label.equals("SECONDARY") || label.equals("SPECIAL");
    }

    private SlotInfo hoveredSlotInfo(double mx, double my, int x, int y, int totalW, int maxH, String selectedKit) {
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return null;
        var kit = kitO.get();

        int gearW = (int)(totalW * GEAR_RATIO);
        int conY = y + 12;
        if (mx < x || mx > x + gearW) return null;
        List<Row> rows = layout(kit, x, conY, gearW);

        for (Row row : rows) {
            if (row.y + row.h > conY + maxH) break;
            if (my < row.y || my > row.y + row.h) continue;
            if (row.slots.size() > 1) {
                // Пара SECONDARY+SPECIAL: слот определяется ПОЛОВИНОЙ карточки под курсором,
                // а не первой записью пары (иначе ховер СПЕЦ показывал вторичку)
                int cw = (gearW - GAP) / 2;
                int xx = x;
                for (int si : row.slots) {
                    if (mx >= xx && mx <= xx + cw) {
                        SlotInfo inf = makeSlotInfo(kit, si);
                        if (inf != null) return inf;
                    }
                    xx += cw + GAP;
                }
            } else {
                SlotInfo inf = makeSlotInfo(kit, row.slots.get(0));
                if (inf != null) return inf;
            }
        }
        return null;
    }

    private SlotInfo makeSlotInfo(DeployData.KitRecord kit, int slotIndex) {
        DeployData.LoadoutSlot slot = kit.loadout().get(slotIndex);
        if (slot.label().equals("BACKPACK") || !isWeaponSlot(slot.label())) return null;
        int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
        if (sel < 0 || sel >= slot.options().size()) sel = slot.defaultIndex();
        if (sel < 0 || sel >= slot.options().size() || slot.options().get(sel).stack().isEmpty())
            return null;
        return new SlotInfo(kit.name(), slot.label(), slot, sel);
    }

    /** Stack оружия под курсором для 3D-тултипа (без вариантов — пусто). */
    public ItemStack hoveredWeapon(double mx, double my, int x, int y, int totalW, int maxH, String selectedKit) {
        var info = hoveredSlotInfo(mx, my, x, y, totalW, maxH, selectedKit);
        if (info == null) return ItemStack.EMPTY;
        return info.slot().options().get(info.selectedIndex()).stack();
    }

    /** Полная информация для тултипа (варианты + выбранный). */
    public SlotInfo hoveredSlotFull(double mx, double my, int x, int y, int totalW, int maxH, String selectedKit) {
        return hoveredSlotInfo(mx, my, x, y, totalW, maxH, selectedKit);
    }

    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int totalW, int maxH, String selectedKit) {
        if (btn != 0) return false;
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return false;
        var kit = kitO.get();

        int gearW = (int)(totalW * GEAR_RATIO);
        int conY = y + 12;
        List<Row> rows = layout(kit, x, conY, gearW);

        // Рюкзак не кликабелен — только отображение
        return false;
    }
}