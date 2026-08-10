package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Персистентный 3D-тултип оружия: наведение на СТВОЛ/ВТОРИЧКУ/СПЕЦ →
 * рядом с курсором карточки ВАРИАНТОВ этого слота (вертикально, сверху вниз):
 * у каждой — название сверху и мини-3D именно этой модели.
 *
 * <p>Пин: ЛКМ по слоту оружия в лоадауте фиксирует тултип ({@link #pin()}),
 * мышь можно свободно вести к карточкам вариантов. ЛКМ по карточке — выбор
 * + закрытие; ЛКМ/ПКМ мимо — отпин + закрытие.</p>
 *
 * <p>Глубина: перед фоном тултипа вся область очищается по depth —
 * модели лоадаута (Z=250) не «просвечивают» сквозь непрозрачный фон.
 * Blend-функция явно ставится в SRC_ALPHA (аддитивный бленд от рендера
 * сущностей не затекает в GUI-фон).</p>
 */
public class WeaponTooltipRenderer {

    private static final int PAD = 4;
    private static final int CARD_H = 46;
    private static final int NAME_H = 11;
    private static final int MODEL_H = CARD_H - NAME_H - 2;
    private static final int WIDTH = 168;
    private static final int OFFSET_X = 12, OFFSET_Y = 14;
    private static final long FADE_IN_MS = 100, FADE_OUT_MS = 80;

    private final WeaponPreviewRenderer preview = new WeaponPreviewRenderer();
    private float fade;
    private int tooltipX, tooltipY, tooltipW, tooltipH;

    // ── Текущая панель ──
    private String kitName;
    private String slotLabel;
    private List<DeployData.LoadoutOption> variants;
    private int selectedIndex;
    private boolean active;
    private ItemStack activeStack;

    /** Тултип закреплён кликом — не гаснет при уводе мыши со слота. */
    private boolean pinned;
    private int pinnedX, pinnedY;

    /** Единая точка входа: слот под курсором (или null — тултип гаснет, если не запинен). */
    public void updateSlot(String kitName, String slotLabel, List<DeployData.LoadoutOption> options, int selected, ItemStack stack) {
        if (pinned) return; // цель заморожена пином
        this.kitName = kitName;
        this.slotLabel = slotLabel;
        this.variants = options;
        this.selectedIndex = selected;
        this.activeStack = stack;
        this.active = options != null || (stack != null && !stack.isEmpty());
    }

    public String kit() { return kitName; }
    public String slot() { return slotLabel; }
    public List<DeployData.LoadoutOption> variantsCopy() { return variants; }
    public boolean isPinned() { return pinned; }

    public ItemStack stackOf(int idx) {
        if (variants == null || idx < 0 || idx >= variants.size()) return ItemStack.EMPTY;
        return variants.get(idx).stack();
    }

    /** Тултип сейчас рисуется (для mouseClicked). */
    public boolean isActive() { return active; }

    /** Курсор над панелью тултипа (фриз таргета в DeployScreen). */
    public boolean contains(double mx, double my) {
        if (!active) return false;
        return mx >= tooltipX && mx <= tooltipX + tooltipW && my >= tooltipY && my <= tooltipY + tooltipH;
    }

    /** Клик по карточке варианта: индекс или -1. */
    public int variantHit(double mx, double my) {
        if (!active || variants == null || variants.isEmpty()) return -1;
        if (!contains(mx, my)) return -1;
        int cy = tooltipY + PAD;
        for (int i = 0; i < variants.size(); i++) {
            if (my >= cy && my <= cy + CARD_H) return i;
            cy += CARD_H;
        }
        return -1;
    }

    /** Запинить тултип (ЛКМ по слоту оружия). Таргет больше не обновляется ховером.
     *  Позиция фиксируется — тултип не следует за курсором. */
    public void pin(int cursorX, int cursorY) {
        if (!active) return;
        pinned = true;
        pinnedX = cursorX + OFFSET_X;
        pinnedY = cursorY + OFFSET_Y;
    }

    /** Отпинить и закрыть. */
    public void unpin() {
        pinned = false;
        closeNow();
    }

    /** Закрыть сразу (выбор сделан / ПКМ) — без fade-out. */
    public void closeNow() {
        active = false;
        pinned = false;
        fade = 0f;
    }

    public void clearSlot() {
        active = false;
        pinned = false;
        fade = 0f;
        variants = null;
        kitName = null;
        slotLabel = null;
    }

    public void render(GuiGraphics gui, double mx, double my) {
        float target = active ? 1f : 0f;
        long duration = target > fade ? FADE_IN_MS : FADE_OUT_MS;
        fade = target > fade
            ? Math.min(target, fade + 16f / duration)
            : Math.max(target, fade - 16f / duration);
        if (!active && fade < 0.01f) { clearSlot(); return; }
        if (fade < 0.01f) return;

        int n = variants != null && active ? Math.max(1, variants.size()) : 1;
        int w = WIDTH;
        int h = PAD * 2 + n * CARD_H;

        int x, y;
        if (pinned) {
            x = pinnedX; y = pinnedY;
        } else {
            x = (int) Math.round(mx) + OFFSET_X;
            y = (int) Math.round(my) + OFFSET_Y;
        }
        int sw = gui.guiWidth(), sh = gui.guiHeight();
        if (x + w > sw) x = Math.max(4, sw - w - 4);
        if (y + h > sh) y = Math.max(4, sh - h - 4);
        tooltipX = x; tooltipY = y; tooltipW = w; tooltipH = h;

        // Альфа панели считается ЯВНО в цвете и схлопывается в opaque быстрее контента:
        // при любом fade ≥ 0.67 фон уже полностью непрозрачный — просвечивания нет.
        int bgA = (int) (255f * Math.min(1f, fade * 1.5f));
        int bgColor = (bgA << 24) | 0x000E1116;
        int bdA = (int) (255f * Math.min(1f, fade * 1.2f));
        int bdColor = (bdA << 24) | (PWPTheme.Colors.BORDER & 0x00FFFFFF);

        // Стираем depth на ВСЮ область тултипа (до фона) — модели лоадаута (Z=250)
        // не «протекают» сквозь фон. Blend — явно SRC_ALPHA: аддитивный бленд
        // от рендера сущностей не затекает в GUI-фон (fill с alpha=255 обязан
        // полностью перекрывать фон, а не складываться с ним).
        gui.enableScissor(x, y, x + w, y + h);
        try {
            RenderSystem.clear(256, Minecraft.ON_OSX);
        } finally {
            gui.disableScissor();
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        try {
            RoundedRect.fill(gui, x, y, w, h, 6, bgColor);
            RoundedRect.border(gui, x, y, w, h, 6, 1, bdColor);

            if (active && variants != null) {
                int cy = y + PAD;
                for (int i = 0; i < variants.size(); i++) {
                    renderVariantCard(gui, x, cy, w, variants.get(i), i == selectedIndex, mx, my);
                    cy += CARD_H;
                }
            } else {
                // Нет вариантов — одиночная 3D-модель
                if (activeStack != null && !activeStack.isEmpty()) {
                    gui.enableScissor(x + PAD, y + PAD, x + w - PAD, y + h - PAD);
                    try {
                        RenderSystem.clear(256, Minecraft.ON_OSX);
                        var p = preview.presetFor(activeStack);
                        preview.render(gui, activeStack, x + w / 2f, y + h / 2f, w - PAD * 2, h - PAD * 2,
                            p.yaw(), p.pitch(), p.zRot());
                    } finally {
                        gui.disableScissor();
                    }
                }
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }

    /** Карточка варианта: название сверху, мини-3D модели снизу. */
    private void renderVariantCard(GuiGraphics gui, int x, int y, int w,
                                   DeployData.LoadoutOption opt, boolean selected, double mx, double my) {
        var f = PWPTheme.Fonts.display();
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + CARD_H;

        int bg = selected ? 0x242C8812 : (hover ? 0x2212151A : 0);
        RoundedRect.fill(gui, x + 3, y + 1, w - 6, CARD_H - 1, 5, bg);
        if (selected) RoundedRect.border(gui, x + 3, y + 1, w - 6, CARD_H - 1, 5, 2, PWPTheme.Colors.ACCENT);
        else if (hover) RoundedRect.border(gui, x + 3, y + 1, w - 6, CARD_H - 1, 5, 1, PWPTheme.Colors.BORDER_FOCUS);

        // Название сверху
        String name = opt.name();
        int maxW = w - 24;
        if (f.width(name) > maxW) name = f.plainSubstrByWidth(name, maxW - 4) + "\u2026";
        gui.drawString(f, name, x + 8, y + 3, selected ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Colors.TEXT_PRIMARY, false);

        // 3D-модель ниже (зона карточки — свой clear глубины, карточки не протекают)
        int pvY = y + NAME_H + 2;
        int pvH = MODEL_H;
        if (!opt.stack().isEmpty()) {
            gui.enableScissor(x + 3, pvY, x + w - 3, pvY + pvH);
            try {
                RenderSystem.clear(256, Minecraft.ON_OSX);
                var pr = preview.presetFor(opt.stack());
                preview.render(gui, opt.stack(), x + w / 2f, pvY + pvH / 2f, w - 16, pvH - 2,
                    pr.yaw(), pr.pitch(), pr.zRot());
            } finally {
                gui.disableScissor();
            }
        }
    }
}
