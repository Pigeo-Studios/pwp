package com.pwp.coreclient.ui.hud;

import com.pwp.coreclient.ui.UIDraw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * HP и броня (этап 2): компактная стеклянная плашка в низ-лево рядом с хотбаром — число HP в
 * процентах, полоса HP с делениями и хвостом «отложенного» урона ({@link DamageTrail}), под ней
 * тонкая полоса брони, по низу плашки — линия кулдауна прыжка. Только вид: значения и видимость
 * даёт вызывающий (pwp-limits; в превью — фикстура). Без аллокаций в кадре.
 *
 * <p>Плашка ВСЕГДА занимает нижнее место стека низ-лево (даже скрытая), чтобы БК над ней не прыгал.
 */
@OnlyIn(Dist.CLIENT)
public final class HudVitals {
    /** Ширина места под число HP («100»). */
    private static final String WIDEST = "100";

    private final DamageTrail hp = new DamageTrail(1F);
    private int lastPercent = Integer.MIN_VALUE;
    private String percentText = "";

    /** Мгновенно без хвоста (респавн / первый кадр). */
    public void snap(float hpFrac) {
        hp.snap(hpFrac);
    }

    /**
     * Занять место в стеке низ-лево и нарисовать.
     *
     * @param hpFrac доля HP 0..1
     * @param armorFrac доля брони 0..1
     * @param cooldownFrac прогресс кулдауна прыжка 0..1, &lt;0 — кулдауна нет
     * @param alpha прозрачность 0..1 (0 — место занято, но не рисуем)
     */
    public void render(
            GuiGraphics g, Font font, HudZones z, float hpFrac, float armorFrac, float cooldownFrac, float alpha) {
        hp.update(hpFrac);
        HudZones.Stack st = z.bottomLeftStack();
        int y = st.next(z.u(HudTheme.VITALS_H));
        if (y == HudZones.Stack.NONE || alpha <= 0.01F) return;

        float s = z.scale();
        int w = Math.round(st.area().w() / s);
        int h = HudTheme.VITALS_H;
        HudDraw.begin(g, st.area().x(), y, s);
        float v = hp.value();
        int fill = v <= HudTheme.HP_LOW_FRACTION ? HudTheme.HP_LOW : HudTheme.HP_FILL;
        HudDraw.plate(g, 0, 0, w, h, fill, alpha);

        int tx = HudTheme.PLATE_STRIPE + HudTheme.PAD_X;
        int percent = Math.round(Math.max(0F, Math.min(1F, hpFrac)) * 100F);
        HudDraw.text(
                g,
                font,
                percent(percent),
                tx,
                (h - font.lineHeight) / 2 + 1,
                UIDraw.fade(v <= HudTheme.HP_LOW_FRACTION ? HudTheme.VALUE_DANGER : HudTheme.VALUE, alpha),
                true);

        int bx = tx + font.width(WIDEST) + HudTheme.PAD_X;
        int bw = w - bx - HudTheme.PAD_X;
        int hpY = (h - HudTheme.VITALS_HP_BAR_H - HudTheme.VITALS_BAR_GAP - HudTheme.VITALS_ARMOR_BAR_H) / 2;
        HudDraw.bar(g, bx, hpY, bw, HudTheme.VITALS_HP_BAR_H, v, hp.trail(), HudTheme.VITALS_SEGMENTS, fill, alpha);
        int arY = hpY + HudTheme.VITALS_HP_BAR_H + HudTheme.VITALS_BAR_GAP;
        HudDraw.bar(
                g,
                bx,
                arY,
                bw,
                HudTheme.VITALS_ARMOR_BAR_H,
                armorFrac,
                -1F,
                HudTheme.VITALS_SEGMENTS,
                HudTheme.ARMOR_FILL,
                alpha);

        if (cooldownFrac >= 0F) {
            int cw = Math.round((w - 2) * Math.min(1F, cooldownFrac));
            int cy = h - 1 - HudTheme.VITALS_COOLDOWN_H;
            if (cw > 0)
                g.fill(1, cy, 1 + cw, cy + HudTheme.VITALS_COOLDOWN_H, UIDraw.fade(HudTheme.COOLDOWN_FILL, alpha));
        }
        HudDraw.end(g);
    }

    private String percent(int p) {
        if (p != lastPercent) {
            lastPercent = p;
            percentText = Integer.toString(p);
        }
        return percentText;
    }
}
