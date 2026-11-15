package com.pwp.coreclient.ui;

import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Кнопка UI-кита: стили как у PWPButton (PRIMARY/ACCENT/DANGER/DARK/GHOST), плавный hover. */
@OnlyIn(Dist.CLIENT)
public class UIButton extends UIWidget {
    public enum Style {
        PRIMARY,
        ACCENT,
        DANGER,
        DARK,
        GHOST
    }

    protected Component label;
    protected Style style = Style.PRIMARY;
    protected Runnable onPress;
    protected final UIAnim hover = new UIAnim(0F, Animations.DURATION_FAST, UIAnim.EASE_OUT_CUBIC);

    public UIButton(int x, int y, int width, int height, Component label, Runnable onPress) {
        super(x, y, width, height);
        this.label = label;
        this.onPress = onPress;
    }

    /** Кнопка стандартной высоты {@link Spacing#BUTTON_HEIGHT}. */
    public UIButton(int x, int y, int width, Component label, Runnable onPress) {
        this(x, y, width, Spacing.BUTTON_HEIGHT, label, onPress);
    }

    public UIButton style(Style style) {
        this.style = style;
        return this;
    }

    public UIButton label(Component label) {
        this.label = label;
        return this;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = enabled && isMouseOver(mouseX, mouseY);
        float t = hover.to(hovered ? 1F : 0F).value();
        int bg;
        int border;
        int text;
        switch (style) {
            case ACCENT -> {
                bg = enabled
                        ? Colors.lerp(Styles.Button.ACCENT_BG, Styles.Button.ACCENT_HOVER, t)
                        : Styles.Button.ACCENT_DISABLED;
                border = Colors.ACCENT_DIM;
                text = Styles.Button.ACCENT_TEXT;
            }
            case DANGER -> {
                bg = Colors.lerp(Styles.Button.DANGER_BG, Styles.Button.DANGER_HOVER, t);
                border = Colors.DANGER;
                text = Styles.Button.DANGER_TEXT;
            }
            case DARK -> {
                bg = Colors.lerp(Styles.Button.DARK_BG, Styles.Button.DARK_HOVER, t);
                border = hovered ? Colors.BORDER_FOCUS : Styles.Button.DARK_BORDER;
                text = Colors.TEXT_PRIMARY;
            }
            case GHOST -> {
                bg = Colors.lerp(Colors.withAlpha(Colors.SURFACE_LIGHT, 0), Colors.SURFACE_LIGHT, t);
                border = Colors.lerp(Colors.withAlpha(Colors.BORDER_FOCUS, 0), Colors.BORDER_FOCUS, t);
                text = t > 0.01F ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY;
            }
            default -> {
                bg = Colors.lerp(Styles.Button.PRIMARY_BG, Styles.Button.PRIMARY_HOVER, t);
                border = enabled ? (hovered ? Colors.BORDER_FOCUS : Styles.Button.PRIMARY_BORDER) : Colors.BORDER;
                text = enabled ? Styles.Button.PRIMARY_TEXT : Styles.Button.PRIMARY_DISABLED_TEXT;
            }
        }
        if (!enabled && style != Style.ACCENT) text = Styles.Button.PRIMARY_DISABLED_TEXT;
        UIDraw.box(g, x, y, width, height, Spacing.RADIUS_SMALL, bg, border);
        renderContent(g, text);
    }

    /** Содержимое кнопки (по умолчанию — подпись по центру). */
    protected void renderContent(GuiGraphics g, int textColor) {
        Component shown = UIDraw.ellipsize(font(), label, width - 2 * Spacing.SM);
        UIDraw.centered(g, font(), shown, x + width / 2, y + height / 2 + 1, textColor);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!enabled || button != 0 || !isMouseOver(mx, my)) return false;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (onPress != null) onPress.run();
        return true;
    }
}
