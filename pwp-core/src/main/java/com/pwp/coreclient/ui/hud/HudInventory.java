package com.pwp.coreclient.ui.hud;

import com.pwp.coreclient.settings.CoreUiSettings;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Этап 6 HUD: фон ванильного инвентаря и сундуков в тактическом стиле PWP (стекло, слоты-плашки,
 * подписи). Меняется ТОЛЬКО фон и подписи: позиции слотов (берутся из {@link Slot#x}/{@link Slot#y}),
 * imageWidth/Height, leftPos/topPos, клики и книга рецептов остаются ванильными.
 *
 * <p>Белый список — точные классы {@link InventoryScreen}, {@link ContainerScreen}, {@link
 * ShulkerBoxScreen}; подклассы и модовые экраны не трогаются. Выключатель — настройка {@code
 * hud.inventory_style} (по умолчанию выкл). Вызовы — из миксинов {@code InventoryScreenMixin},
 * {@code ContainerScreenBgMixin}, {@code AbstractContainerScreenMixin}.
 */
@OnlyIn(Dist.CLIENT)
public final class HudInventory {
    private HudInventory() {}

    private static final Component TITLE = Component.translatable("pwp_core.inventory.title");
    private static final Component ARMOR = Component.translatable("pwp_core.inventory.armor");
    private static final Component CRAFTING = Component.translatable("pwp_core.inventory.crafting");

    /** Кэш прямоугольников панели/шапки: пересоздаются только при смене размеров (без аллокаций в кадре). */
    private static Rect panel;

    private static Rect header;

    /** Включён ли PWP-стиль для этого экрана (точный класс из белого списка). */
    public static boolean styled(Screen screen) {
        if (!CoreUiSettings.HUD_INVENTORY_STYLE.get() || screen == null) return false;
        Class<?> c = screen.getClass();
        return c == InventoryScreen.class || c == ContainerScreen.class || c == ShulkerBoxScreen.class;
    }

    // ================================================================ инвентарь игрока

    /**
     * Фон {@link InventoryScreen}: стекло + шапка «Инвентарь» над панелью, рамка куклы с подписью
     * «Броня», подпись «Крафт», стрелка крафта, плашки слотов, разделитель хотбара, кукла как в
     * ванили. {@code doll == null} (превью без мира) — кукла не рисуется.
     */
    public static void renderPlayer(
            GuiGraphics g,
            Font font,
            List<Slot> slots,
            int left,
            int top,
            int w,
            int h,
            float xMouse,
            float yMouse,
            LivingEntity doll) {
        int headTop = top - HudTheme.INV_HEADER_H;
        boolean withHeader = headTop >= 0;
        int panelTop = withHeader ? headTop : top;
        glass(g, left, panelTop, w, h + (top - panelTop));
        if (withHeader) {
            header = cached(header, left, headTop, w, HudTheme.INV_HEADER_H);
            UIDraw.header(g, font, header, null, TITLE, HudTheme.INV_TITLE, 1F);
        }

        if (slots.size() <= InventoryMenu.SHIELD_SLOT) {
            // Чужая раскладка меню — только плашки слотов.
            slotPlates(g, slots, left, top, -1);
            return;
        }

        // Рамка куклы: между колонкой брони и слотом щита, по высоте колонки брони (как в ванили).
        Slot armorFirst = slots.get(InventoryMenu.ARMOR_SLOT_START);
        Slot armorLast = slots.get(InventoryMenu.ARMOR_SLOT_END - 1);
        Slot shield = slots.get(InventoryMenu.SHIELD_SLOT);
        int dx1 = left + armorFirst.x + HudTheme.INV_ITEM + HudTheme.INV_SLOT_OUTSET + HudTheme.INV_GAP;
        int dx2 = left + shield.x - HudTheme.INV_SLOT_OUTSET - HudTheme.INV_GAP;
        int dy1 = top + armorFirst.y;
        int dy2 = top + armorLast.y + HudTheme.INV_ITEM;
        if (dx2 > dx1 && dy2 > dy1) {
            g.fill(dx1, dy1, dx2, dy2, HudTheme.INV_DOLL_BG);
            HudDraw.frame(g, dx1, dy1, dx2 - dx1, dy2 - dy1, HudTheme.INV_DOLL_BORDER);
            int lx = dx1 + HudTheme.INV_GAP;
            int maxW = dx2 - lx - HudTheme.INV_GAP;
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, ARMOR, maxW),
                    lx,
                    dy1 + HudTheme.INV_GAP,
                    HudTheme.INV_LABEL,
                    false);
        }

        // Подпись «Крафт» над сеткой 2x2 (на месте ванильной подписи).
        Slot craft = slots.get(InventoryMenu.CRAFT_SLOT_START);
        Slot result = slots.get(InventoryMenu.RESULT_SLOT);
        int cx = left + craft.x - HudTheme.INV_SLOT_OUTSET;
        int cy = top + craft.y - HudTheme.INV_SLOT_OUTSET - font.lineHeight;
        int craftMaxW = left + w - cx - HudTheme.INV_GAP;
        UIDraw.text(g, font, UIDraw.ellipsize(font, CRAFTING, craftMaxW), cx, cy, HudTheme.INV_LABEL, false);

        // Стрелка крафта: от правого края сетки до рамки результата.
        Slot craftRight = slots.get(InventoryMenu.CRAFT_SLOT_START + 1);
        int ax1 = left + craftRight.x + HudTheme.INV_ITEM + HudTheme.INV_SLOT_OUTSET + HudTheme.INV_GAP * 2;
        int ax2 = left + result.x - HudTheme.INV_RESULT_OUTSET - HudTheme.INV_GAP * 2;
        int ay = top + result.y + HudTheme.INV_ITEM / 2;
        arrow(g, ax1, ax2, ay);

        // Разделитель основного инвентаря и хотбара (в ванильном зазоре между рядами).
        Slot hotbar = slots.get(InventoryMenu.USE_ROW_SLOT_START);
        Slot lastRow = slots.get(InventoryMenu.INV_SLOT_END - 1);
        int gapTop = top + lastRow.y + HudTheme.INV_ITEM + HudTheme.INV_SLOT_OUTSET;
        int gapBottom = top + hotbar.y - HudTheme.INV_SLOT_OUTSET;
        if (gapBottom > gapTop) {
            int y = (gapTop + gapBottom) / 2;
            UIDraw.hline(g, left + hotbar.x, left + w - hotbar.x, y, HudTheme.INV_DIVIDER);
        }

        slotPlates(g, slots, left, top, InventoryMenu.RESULT_SLOT);

        if (doll != null) {
            int footX = (dx1 + dx2 + 1) / 2;
            int footY = dy2 - HudTheme.INV_DOLL_FOOT_INSET;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g,
                    footX,
                    footY,
                    HudTheme.INV_DOLL_SCALE,
                    footX - xMouse,
                    footY - HudTheme.INV_DOLL_EYE - yMouse,
                    doll);
        }
    }

    // ================================================================ сундук / шалкер

    /**
     * Фон {@link ContainerScreen}/{@link ShulkerBoxScreen}: стекло, шапка с названием контейнера
     * (высотой до первого ряда слотов), подпись инвентаря игрока на ванильной позиции с
     * разделителем, плашки слотов.
     */
    public static void renderContainer(
            GuiGraphics g,
            Font font,
            List<Slot> slots,
            int left,
            int top,
            int w,
            int h,
            Component title,
            Component inventoryTitle,
            int inventoryLabelX,
            int inventoryLabelY) {
        glass(g, left, top, w, h);
        int firstY = slots.isEmpty() ? 0 : slots.get(0).y;
        int headerH = firstY - HudTheme.INV_SLOT_OUTSET - HudTheme.INV_GAP;
        if (headerH > 0) {
            header = cached(header, left, top, w, headerH);
            UIDraw.header(g, font, header, null, title, HudTheme.INV_TITLE, 1F);
        }
        if (inventoryTitle != null) {
            int lx = left + inventoryLabelX;
            int ly = top + inventoryLabelY;
            int maxW = w - inventoryLabelX * 2;
            Component shown = UIDraw.ellipsize(font, inventoryTitle, maxW);
            UIDraw.text(g, font, shown, lx, ly, HudTheme.INV_LABEL, false);
            int lineX = lx + font.width(shown) + HudTheme.INV_GAP * 2;
            int lineEnd = left + w - inventoryLabelX;
            if (lineEnd > lineX) UIDraw.hline(g, lineX, lineEnd, ly + font.lineHeight / 2, HudTheme.INV_DIVIDER);
        }
        slotPlates(g, slots, left, top, -1);
    }

    // ================================================================ примитивы

    private static void glass(GuiGraphics g, int x, int y, int w, int h) {
        panel = cached(panel, x, y, w, h);
        UIDraw.glass(g, panel, 1F, true);
    }

    /** Плашки всех активных слотов; {@code resultIndex} — слот результата (крупная рамка с акцентом). */
    private static void slotPlates(GuiGraphics g, List<Slot> slots, int left, int top, int resultIndex) {
        int size = HudTheme.INV_ITEM + HudTheme.INV_SLOT_OUTSET * 2;
        for (int i = 0, n = slots.size(); i < n; i++) {
            Slot s = slots.get(i);
            if (!s.isActive()) continue;
            if (i == resultIndex) {
                int o = HudTheme.INV_RESULT_OUTSET;
                int rs = HudTheme.INV_ITEM + o * 2;
                int rx = left + s.x - o;
                int ry = top + s.y - o;
                g.fill(rx, ry, rx + rs, ry + rs, HudTheme.INV_SLOT_BG);
                HudDraw.frame(g, rx, ry, rs, rs, HudTheme.INV_RESULT_BORDER);
                continue;
            }
            int x = left + s.x - HudTheme.INV_SLOT_OUTSET;
            int y = top + s.y - HudTheme.INV_SLOT_OUTSET;
            g.fill(x, y, x + size, y + size, HudTheme.INV_SLOT_BG);
            g.fill(x + 1, y + 1, x + size - 1, y + 2, HudTheme.INV_SLOT_HIGHLIGHT);
            HudDraw.frame(g, x, y, size, size, HudTheme.INV_SLOT_BORDER);
        }
    }

    /** Стрелка вправо: древко + треугольный наконечник из вертикальных отрезков. */
    private static void arrow(GuiGraphics g, int x1, int x2, int y) {
        int head = HudTheme.INV_ARROW_HEAD;
        if (x2 - x1 <= head) return;
        int half = HudTheme.INV_ARROW_THICK / 2;
        g.fill(x1, y - half, x2 - head, y - half + HudTheme.INV_ARROW_THICK, HudTheme.INV_ARROW);
        for (int i = 0; i < head; i++) {
            int x = x2 - head + i;
            int r = head - i;
            g.fill(x, y - r, x + 1, y + r, HudTheme.INV_ARROW);
        }
    }

    private static Rect cached(Rect r, int x, int y, int w, int h) {
        if (r != null && r.x() == x && r.y() == y && r.w() == w && r.h() == h) return r;
        return Rect.of(x, y, Math.max(0, w), Math.max(0, h));
    }
}
