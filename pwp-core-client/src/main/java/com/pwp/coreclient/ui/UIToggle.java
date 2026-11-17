package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Тумблер UI-кита: подпись «Вкл/Выкл» и переключатель справа, ручка плавно переезжает, трек
 * перекрашивается в акцент. Клик по всей области переключает. Значение хранит владелец.
 */
@OnlyIn(Dist.CLIENT)
public class UIToggle extends UIWidget {
    private final BooleanSupplier value;
    private final Runnable onToggle;
    private final UIAnim knob = new UIAnim(0F, Animations.DURATION_KNOB, UIAnim.EASE_OUT_CUBIC);
    private final UIAnim hover = new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);
    private boolean init;

    public UIToggle(BooleanSupplier value, Runnable onToggle) {
        super(0, 0, 0, Spacing.SETTING_ROW_H);
        this.value = value;
        this.onToggle = onToggle;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float o = opacity;
        boolean on = value.getAsBoolean();
        if (!init) {
            knob.snap(on ? 1F : 0F);
            init = true;
        } else knob.to(on ? 1F : 0F);
        float k = knob.value();
        float h = hover.to(enabled && isMouseOver(mouseX, mouseY) ? 1F : 0F).value();
        int sw = Spacing.SWITCH_W;
        int sh = Math.min(height, Spacing.SWITCH_H);
        Rect sr = Rect.of(x + width - sw, y + (height - sh) / 2, sw, sh);
        int trackColor = Colors.lerp(Styles.Setting.TOGGLE_OFF, Styles.Setting.TOGGLE_ON, k);
        if (!enabled) trackColor = Styles.Setting.TRACK_DISABLED;
        UIDraw.rect(g, sr, UIDraw.fade(trackColor, o));
        int border = Colors.lerp(Styles.Control.BORDER, Styles.Control.BORDER_HOVER, h);
        UIDraw.outline(g, sr, UIDraw.fade(border, o));
        int kw = sh - 2 * Spacing.HAIRLINE - Spacing.XXS;
        int kx = sr.x() + Spacing.HAIRLINE + 1 + Math.round(k * (sw - kw - 2 * (Spacing.HAIRLINE + 1)));
        Rect kr = Rect.of(kx, sr.y() + Spacing.HAIRLINE + 1, kw, kw);
        int knobColor =
                enabled ? Colors.lerp(Styles.Setting.KNOB, Styles.Setting.KNOB_HOVER, h) : Styles.Setting.FILL_DISABLED;
        UIDraw.rect(g, kr, UIDraw.fade(knobColor, o));
        var font = font();
        Component label = Component.translatable(on ? "pwp_core.ui_settings.on" : "pwp_core.ui_settings.off");
        Component shown = UIDraw.ellipsize(font, label, Math.max(0, width - sw - Spacing.SM));
        int tc = !enabled ? Styles.Control.TEXT_DISABLED : on ? Styles.Setting.VALUE : Styles.Control.SUBTEXT;
        UIDraw.text(
                g,
                font,
                shown,
                sr.x() - Spacing.SM - font.width(shown),
                y + (height - font.lineHeight) / 2 + 1,
                UIDraw.fade(tc, o),
                false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || button != 0 || !isMouseOver(mx, my)) return false;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        onToggle.run();
        return true;
    }
}
