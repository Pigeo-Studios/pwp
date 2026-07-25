package com.pwp.coreclient.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class PWPButton extends AbstractWidget {

    public enum Style {
        PRIMARY, ACCENT, DANGER, DARK, GHOST
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
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.onPress.onPress(this);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        if (this.animAlpha < 0.01F) return;

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 50.0F, 4.0F);
        lastRenderTime = now;

        boolean hovered = this.isHovered();
        boolean active = this.active;
        boolean pressedNow = active && hovered && Minecraft.getInstance().mouseHandler.isLeftPressed();

        float hoverTarget = (hovered && active) ? 1.0F : 0.0F;
        float pressTarget = pressedNow ? 1.0F : 0.0F;
        float speed = 1 - (float) Math.exp(-0.15F * dt * 50);
        hoverAnim += (hoverTarget - hoverAnim) * speed;
        pressAnim += (pressTarget - pressAnim) * speed;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.animAlpha);

        int bg, border, textColor;
        boolean showAccentGlow = false;

        if (!active) {
            bg = PWPTheme.Styles.Button.DARK_BG;
            border = PWPTheme.Colors.BORDER;
            textColor = PWPTheme.Colors.TEXT_DIM;
        } else {
            switch (style) {
                case ACCENT: {
                    int base = PWPTheme.Colors.lerp(
                            PWPTheme.Styles.Button.ACCENT_BG, PWPTheme.Styles.Button.ACCENT_HOVER, hoverAnim);
                    bg = PWPTheme.Colors.lerp(base, PWPTheme.Styles.Button.ACCENT_PRESSED, pressAnim);
                    border = PWPTheme.Colors.ACCENT_DIM;
                    textColor = PWPTheme.Styles.Button.ACCENT_TEXT;
                    showAccentGlow = hoverAnim > 0.01F;
                    break;
                }
                case DANGER: {
                    int base = PWPTheme.Colors.lerp(
                            PWPTheme.Styles.Button.DANGER_BG, PWPTheme.Styles.Button.DANGER_HOVER, hoverAnim);
                    bg = PWPTheme.Colors.lerp(base, PWPTheme.Styles.Button.DANGER_PRESSED, pressAnim);
                    border = PWPTheme.Colors.DANGER;
                    textColor = PWPTheme.Styles.Button.DANGER_TEXT;
                    break;
                }
                case DARK: {
                    int base = PWPTheme.Colors.lerp(
                            PWPTheme.Styles.Button.DARK_BG, PWPTheme.Styles.Button.DARK_HOVER, hoverAnim);
                    bg = PWPTheme.Colors.lerp(base, PWPTheme.Styles.Button.DARK_PRESSED, pressAnim);
                    border = hovered ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Styles.Button.DARK_BORDER;
                    textColor = PWPTheme.Colors.TEXT_PRIMARY;
                    break;
                }
                case GHOST: {
                    int ghostBg = PWPTheme.Colors.lerp(0x00000000, PWPTheme.Colors.SURFACE_LIGHT, hoverAnim);
                    int ghostBorder = PWPTheme.Colors.lerp(0x00000000, PWPTheme.Colors.BORDER_FOCUS, hoverAnim);
                    border = ghostBorder;
                    bg = ghostBg;
                    textColor = hoverAnim > 0.01F ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY;
                    break;
                }
                default: {
                    int base = PWPTheme.Colors.lerp(
                            PWPTheme.Styles.Button.PRIMARY_BG, PWPTheme.Styles.Button.PRIMARY_HOVER, hoverAnim);
                    bg = PWPTheme.Colors.lerp(base, PWPTheme.Styles.Button.PRIMARY_PRESSED, pressAnim);
                    border = hovered ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Styles.Button.PRIMARY_BORDER;
                    textColor = PWPTheme.Colors.TEXT_PRIMARY;
                    break;
                }
            }
        }

        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;
        int r = PWPTheme.Spacing.RADIUS_SMALL;

        if (showAccentGlow) {
            int glowColor = PWPTheme.Colors.multiplyAlpha(
                    PWPTheme.Shadows.ACCENT_GLOW_SMALL, Math.max(hoverAnim, pressAnim));
            RoundedRect.glow(gui, x, y, w, h, r, 2, glowColor);
        }

        RoundedRect.fill(gui, x, y, w, h, r, bg);
        RoundedRect.border(gui, x, y, w, h, r, 1, border);

        if (loading && active) {
            long cycle = now % 1200;
            float loadProgress = (float) cycle / 1200.0F;
            int loadW = Math.max(8, w / 4);
            int loadX = x + (int) ((w - loadW) * loadProgress);
            gui.fill(loadX, y + h - 2, loadX + loadW, y + h, PWPTheme.Colors.ACCENT);
        }

        var mcFont = PWPTheme.Fonts.display();
        String msgStr = this.getMessage().getString();
        int textW = mcFont.width(msgStr);
        int textY = y + (h - 8) / 2;

        int shadowColor = 0x40000000;

        if (textW > w - 10) {
            String shortText = mcFont.plainSubstrByWidth(msgStr, w - 14) + "...";
            gui.drawString(mcFont, shortText, x + 5, textY + 1, shadowColor);
            gui.drawString(mcFont, shortText, x + 5, textY, textColor);
        } else {
            gui.drawCenteredString(mcFont, this.getMessage(), x + w / 2, textY + 1, shadowColor);
            gui.drawCenteredString(mcFont, this.getMessage(), x + w / 2, textY, textColor);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
