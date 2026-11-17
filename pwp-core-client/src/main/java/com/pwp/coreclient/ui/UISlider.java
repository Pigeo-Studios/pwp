package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Ползунок UI-кита: трек, заливка акцентом до ручки, ручка; справа — подпись значения (живое
 * превью при перетаскивании). Клик по треку ставит значение и начинает перетаскивание (экран
 * пересылает {@code mouseDragged}). Значение хранит владелец: {@code fraction} читает долю
 * 0..1, {@code onFraction} пишет её (привязка к шагу — у владельца).
 */
@OnlyIn(Dist.CLIENT)
public class UISlider extends UIWidget {
    private final DoubleSupplier fraction;
    private final DoubleConsumer onFraction;
    private final Supplier<Component> valueText;
    private boolean dragging;
    private final UIAnim hover = new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);
    private final UIAnim knob = new UIAnim(0F, Animations.DURATION_KNOB, UIAnim.EASE_OUT_CUBIC);
    private boolean knobInit;

    public UISlider(DoubleSupplier fraction, DoubleConsumer onFraction, Supplier<Component> valueText) {
        super(0, 0, 0, Spacing.SETTING_ROW_H);
        this.fraction = fraction;
        this.onFraction = onFraction;
        this.valueText = valueText;
    }

    public boolean isDragging() {
        return dragging;
    }

    /** Область трека (без колонки значения). */
    private Rect track() {
        int valueW = Math.min(Spacing.SETTING_VALUE_W, width / 3);
        int pad = Spacing.SLIDER_KNOB_W / 2;
        return Rect.of(x + pad, y, Math.max(1, width - valueW - Spacing.SM - 2 * pad), height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float o = opacity;
        float f = (float) Math.max(0, Math.min(1, fraction.getAsDouble()));
        if (!knobInit || dragging) {
            knob.snap(f);
            knobInit = true;
        } else knob.to(f);
        boolean hovered = enabled && (dragging || isMouseOver(mouseX, mouseY));
        float h = hover.to(hovered ? 1F : 0F).value();
        Rect t = track();
        int cy = y + height / 2;
        int th = Spacing.SLIDER_TRACK_H;
        int trackColor = enabled ? Styles.Setting.TRACK : Styles.Setting.TRACK_DISABLED;
        UIDraw.rect(g, Rect.of(t.x(), cy - th / 2, t.w(), th), UIDraw.fade(trackColor, o));
        int kx = t.x() + Math.round(knob.value() * t.w());
        int fill = enabled ? Styles.Setting.FILL : Styles.Setting.FILL_DISABLED;
        UIDraw.rect(g, Rect.of(t.x(), cy - th / 2, kx - t.x(), th), UIDraw.fade(fill, o));
        int kw = Spacing.SLIDER_KNOB_W;
        int kh = Math.min(height, Spacing.SLIDER_KNOB_H);
        Rect k = Rect.of(kx - kw / 2, cy - kh / 2, kw, kh);
        int knobColor = Colors.lerp(Styles.Setting.KNOB, Styles.Setting.KNOB_HOVER, h);
        if (!enabled) knobColor = Styles.Setting.FILL_DISABLED;
        UIDraw.rect(g, k, UIDraw.fade(knobColor, o));
        UIDraw.outline(g, k, UIDraw.fade(Styles.Setting.KNOB_BORDER, o));
        // значение справа
        var font = font();
        Component v = valueText.get();
        int valueW = Math.min(Spacing.SETTING_VALUE_W, width / 3);
        Component shown = UIDraw.ellipsize(font, v, valueW);
        int vc = enabled ? Styles.Setting.VALUE : Styles.Control.TEXT_DISABLED;
        UIDraw.text(
                g, font, shown, x + width - font.width(shown), cy - font.lineHeight / 2 + 1, UIDraw.fade(vc, o), false);
    }

    private void setFrom(double mx) {
        Rect t = track();
        onFraction.accept(Math.max(0, Math.min(1, (mx - t.x()) / t.w())));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || button != 0 || !isMouseOver(mx, my)) return false;
        Rect t = track();
        if (mx > t.right() + Spacing.SLIDER_KNOB_W) return true; // клик по подписи значения
        dragging = true;
        setFrom(mx);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging || button != 0) return false;
        setFrom(mx);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = dragging;
        if (button == 0) dragging = false;
        return was && button == 0;
    }
}
