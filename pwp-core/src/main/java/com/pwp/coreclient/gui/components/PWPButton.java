package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class PWPButton extends AbstractWidget {

    public enum Style {
        PRIMARY,
        ACCENT,
        DANGER,
        DARK,
        GHOST
    }

    public interface OnPress {
        void onPress(PWPButton button);
    }

    private Style style;
    private final OnPress onPress;
    private float hoverAnim;
    private float pressAnim;
    private long lastRenderTime;
    private float animAlpha = 1.0F;
    private boolean loading;

    public PWPButton(int x, int y, int width, Component message, OnPress onPress) {
        this(x, y, width, PWPTheme.Spacing.BUTTON_HEIGHT, message, onPress, Style.PRIMARY);
    }

    public PWPButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, Style.PRIMARY);
    }

    public PWPButton(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.style = style;
        this.hoverAnim = 0.0F;
        this.pressAnim = 0.0F;
        this.lastRenderTime = System.currentTimeMillis();
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public void setAnimAlpha(float alpha) {
        this.animAlpha = Math.max(0, Math.min(1, alpha));
    }

    public float getAnimAlpha() {
        return this.animAlpha;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.loading) return false;
        if (this.active && this.visible && clicked(mouseX, mouseY)) {
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.onPress.onPress(this);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        if (this.animAlpha < 0.01F) return;

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 50.0F, 4.0F);
        lastRenderTime = now;

        boolean hovered = this.isHovered();
        boolean active = this.active;
        boolean pressedNow =
                active && hovered && Minecraft.getInstance().mouseHandler.isLeftPressed();

        float hoverTarget = (hovered && active) ? 1.0F : 0.0F;
        float pressTarget = pressedNow ? 1.0F : 0.0F;
        float speed = 1 - (float) Math.exp(-0.15F * dt * 50);
        hoverAnim += (hoverTarget - hoverAnim) * speed;
        pressAnim += (pressTarget - pressAnim) * speed;

        // Тактический стиль UI-кита (как UIButton): стекло + блик + 1px обводка, акцентная
        // полоса слева при наведении; ACCENT/DANGER — вертикальный градиент. API кнопки прежний.
        float o = this.animAlpha;
        Rect r = Rect.of(this.getX(), this.getY(), this.width, this.height);
        int textColor;
        if (!active) {
            UIDraw.rect(gui, r, UIDraw.fade(Styles.Control.BG_DISABLED, o));
            UIDraw.outline(gui, r, UIDraw.fade(Styles.Control.BORDER_DISABLED, o));
            textColor = Styles.Control.TEXT_DISABLED;
        } else if (style == Style.ACCENT || style == Style.DANGER) {
            boolean accent = style == Style.ACCENT;
            int top = Colors.lerp(
                    accent ? Styles.Control.PRIMARY_TOP : Styles.Control.DANGER_TOP,
                    accent ? Styles.Control.PRIMARY_TOP_HOVER : Styles.Button.DANGER_HOVER,
                    hoverAnim);
            int bottom = Colors.lerp(
                    accent ? Styles.Control.PRIMARY_BOTTOM : Styles.Control.DANGER_BOTTOM,
                    accent ? Styles.Control.PRIMARY_BOTTOM_HOVER : Styles.Button.DANGER_PRESSED,
                    hoverAnim);
            top = Colors.lerp(top, Styles.Control.BG_PRESSED, pressAnim * 0.35F);
            bottom = Colors.lerp(bottom, Styles.Control.BG_PRESSED, pressAnim * 0.35F);
            UIDraw.gradientV(gui, r, UIDraw.fade(top, o), UIDraw.fade(bottom, o));
            UIDraw.hline(gui, r.x() + 1, r.right() - 1, r.y() + 1, UIDraw.fade(Styles.Glass.HIGHLIGHT, o));
            UIDraw.outline(
                    gui, r, UIDraw.fade(accent ? Styles.Control.PRIMARY_BORDER : Styles.Control.DANGER_BORDER, o));
            textColor = accent ? Styles.Control.PRIMARY_TEXT : Styles.Control.TEXT_HOVER;
        } else {
            boolean ghost = style == Style.GHOST;
            int base = ghost ? Colors.withAlpha(Styles.Control.BG, 0) : Styles.Control.BG;
            int bg = Colors.lerp(
                    Colors.lerp(base, Styles.Control.BG_HOVER, hoverAnim), Styles.Control.BG_PRESSED, pressAnim);
            int borderBase = ghost ? Colors.withAlpha(Styles.Control.BORDER, 0) : Styles.Control.BORDER;
            UIDraw.rect(gui, r, UIDraw.fade(bg, o));
            UIDraw.gradientV(
                    gui,
                    r,
                    UIDraw.fade(Styles.Control.SHEEN_TOP, o * (ghost ? hoverAnim : 1F)),
                    UIDraw.fade(Styles.Control.SHEEN_BOTTOM, o));
            UIDraw.outline(gui, r, UIDraw.fade(Colors.lerp(borderBase, Styles.Control.BORDER_HOVER, hoverAnim), o));
            int bar = Math.round(hoverAnim * PWPTheme.Spacing.ACCENT_BAR);
            if (bar > 0) UIDraw.accentBar(gui, r.x(), r.y(), r.h(), bar, UIDraw.fade(Styles.Control.ACCENT_BAR, o));
            textColor = Colors.lerp(
                    ghost ? Styles.Control.SUBTEXT : Styles.Control.TEXT, Styles.Control.TEXT_HOVER, hoverAnim);
        }

        if (loading && active) {
            long cycle = now % 1200;
            int loadW = Math.max(8, r.w() / 4);
            int loadX = r.x() + (int) ((r.w() - loadW) * (cycle / 1200F));
            gui.fill(loadX, r.bottom() - 2, loadX + loadW, r.bottom(), UIDraw.fade(Styles.Loader.FILL, o));
        }

        var font = PWPTheme.Fonts.display();
        Component shown = UIDraw.ellipsize(font, this.getMessage(), r.w() - 2 * PWPTheme.Spacing.SM);
        int shift = Math.round(pressAnim * PWPTheme.Spacing.PRESS_SHIFT);
        int textY = r.y() + (r.h() - font.lineHeight) / 2 + 1 + shift;
        UIDraw.text(
                gui,
                font,
                shown,
                r.centerX() - font.width(shown) / 2,
                textY,
                UIDraw.fade(textColor, o),
                style != Style.ACCENT && active);
    }
}
