package com.pwp.coreclient.ui.hud;

import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.ui.UIDamp;
import com.pwp.coreclient.ui.UIDraw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Вид хотбара HUD (этап 2): тёмные стеклянные слоты с тонкой рамкой, выбранный — акцентная полоса
 * сверху + подсветка + подъём; полоса плавно переезжает между слотами ({@link UIDamp}, реальное
 * время). Номер слота мелко в углу, счётчик стака — одним шрифтом справа снизу. Только вид: какой
 * слот выбран и когда хотбар показан, решает вызывающий (pwp-movement). Источник предметов — любой
 * {@link Container} (инвентарь игрока; в превью — фикстура). Без аллокаций в кадре.
 */
@OnlyIn(Dist.CLIENT)
public final class HudHotbar {
    private static final String[] NUMBERS = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    /** z поверх иконок предметов (как у ванильных декораций). */
    private static final float TEXT_Z = 200F;
    /** Скорость переезда полосы выбора / подъёма (доля за кадр при 60 FPS). */
    private static final float SELECT_RATE = 0.3F;

    private final UIDamp.Frame frame = new UIDamp.Frame();
    private final float[] lift = new float[HudTheme.SLOTS];
    private final int[] lastCount = new int[HudTheme.SLOTS];
    private final String[] countText = new String[HudTheme.SLOTS];
    private float selX = Float.NaN;

    /** Ширина хотбара в базовых единицах. */
    public static int width() {
        return HudTheme.SLOTS * HudTheme.SLOT + (HudTheme.SLOTS - 1) * HudTheme.SLOT_GAP;
    }

    /** X слота {@code i} относительно центра хотбара (базовые единицы). */
    public static int slotX(int i) {
        return -width() / 2 + i * (HudTheme.SLOT + HudTheme.SLOT_GAP);
    }

    /**
     * Нарисовать хотбар в зоне {@code z.hotbar()}.
     *
     * @param selected выбранный слот 0..8
     * @param alpha общая прозрачность 0..1 (фейд показа)
     * @param slide 0..1 — насколько хотбар выехал снизу (1 — на месте)
     */
    public void render(GuiGraphics g, Font font, HudZones z, Container items, int selected, float alpha, float slide) {
        long dt = frame.dtMs();
        int sel = Math.max(0, Math.min(HudTheme.SLOTS - 1, selected));
        float targetX = slotX(sel);
        if (Float.isNaN(selX)) selX = targetX;
        selX = UIDamp.approach(selX, targetX, SELECT_RATE, dt);
        for (int i = 0; i < HudTheme.SLOTS; i++) {
            lift[i] = UIDamp.approach(lift[i], i == sel ? HudTheme.SLOT_LIFT : 0F, SELECT_RATE, dt);
        }
        if (alpha <= 0.001F) return;

        float s = z.scale();
        HudDraw.begin(g, z.hotbar().centerX(), z.hotbar().bottom(), s);
        // выезд снизу: скрытый хотбар уходит за нижний край на свою высоту + отступ
        float hide = (1F - Math.max(0F, Math.min(1F, slide))) * (HudZones.HOTBAR_H + HudZones.BOTTOM_PAD);
        int base = Math.round(-HudTheme.SLOT + hide);
        int size = HudTheme.SLOT;

        for (int i = 0; i < HudTheme.SLOTS; i++) {
            int x = slotX(i);
            int y = base - Math.round(lift[i]);
            float w = lift[i] / HudTheme.SLOT_LIFT; // 0..1 — насколько слот «выбран»
            int bg = Colors.lerp(HudTheme.SLOT_BG, HudTheme.SLOT_BG_SELECTED, w);
            float bgO = alpha * Hud.plateOpacity();
            g.fill(x, y, x + size, y + size, UIDraw.fade(bg, bgO));
            HudDraw.frame(
                    g,
                    x,
                    y,
                    size,
                    size,
                    UIDraw.fade(Colors.lerp(HudTheme.SLOT_BORDER, HudTheme.SLOT_BORDER_SELECTED, w), alpha));
        }

        // подсветка и акцентная полоса выбора — плавно между слотами
        int sx = Math.round(selX);
        int top = base - HudTheme.SLOT_LIFT;
        g.fillGradient(sx, top, sx + size, top + size, UIDraw.fade(HudTheme.SLOT_GLOW, alpha), 0);
        g.fill(sx, top, sx + size, top + HudTheme.PLATE_STRIPE, UIDraw.fade(HudTheme.ACCENT, alpha));

        if (items != null && alpha > 0.05F) {
            int inset = HudTheme.SLOT_ITEM_INSET;
            for (int i = 0; i < HudTheme.SLOTS && i < items.getContainerSize(); i++) {
                ItemStack stack = items.getItem(i);
                if (stack.isEmpty()) continue;
                int x = slotX(i);
                int y = base - Math.round(lift[i]);
                g.renderItem(stack, x + inset, y + inset);
                // полоса прочности/кулдаун — ванильные, число стака рисуем сами (пустая строка)
                g.renderItemDecorations(font, stack, x + inset, y + inset, "");
            }
        }

        var pose = g.pose();
        pose.pushPose();
        pose.translate(0F, 0F, TEXT_Z);
        for (int i = 0; i < HudTheme.SLOTS; i++) {
            int x = slotX(i);
            int y = base - Math.round(lift[i]);
            int nc = i == sel ? HudTheme.SLOT_NUMBER_SELECTED : HudTheme.SLOT_NUMBER;
            HudDraw.textScaled(g, font, NUMBERS[i], x + 2F, y + 2F, HudTheme.SLOT_NUMBER_SCALE, UIDraw.fade(nc, alpha));
            if (items == null || i >= items.getContainerSize()) continue;
            ItemStack stack = items.getItem(i);
            int count = stack.isEmpty() ? 0 : stack.getCount();
            if (count <= 1) continue;
            String ct = count(i, count);
            HudDraw.text(
                    g,
                    font,
                    ct,
                    x + size - 1 - font.width(ct),
                    y + size - font.lineHeight,
                    UIDraw.fade(HudTheme.SLOT_COUNT, alpha),
                    true);
        }
        pose.popPose();
        HudDraw.end(g);
    }

    /** Строка счётчика слота: пересборка только при смене числа. */
    private String count(int slot, int count) {
        if (countText[slot] == null || lastCount[slot] != count) {
            lastCount[slot] = count;
            countText[slot] = Integer.toString(count);
        }
        return countText[slot];
    }
}
