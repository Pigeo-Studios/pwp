package com.pwp.coreclient.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class PWPButton extends Button {

    public enum Style {
        PRIMARY, ACCENT, DANGER, DARK, GHOST
    }

    private Style style;
    private float hoverAnim;
    private long lastTick;
    private float animAlpha = 1.0F;

    public PWPButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, Style.PRIMARY);
    }

    public PWPButton(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
        this.hoverAnim = 0.0F;
        this.lastTick = System.currentTimeMillis();
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

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        if (this.animAlpha < 0.01F) return;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.animAlpha);

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTick) / 50.0F, 4.0F);
        lastTick = now;

        boolean hovered = this.isHovered();
        boolean active = this.active;
        boolean pressed = hovered && active && Minecraft.getInstance().mouseHandler.isLeftPressed();

        float target = hovered && active ? 1.0F : 0.0F;
        if (pressed) target = 0.5F;
        if (target > hoverAnim) {
            hoverAnim = Math.min(hoverAnim + 0.15F * dt, target);
        } else if (target < hoverAnim) {
            hoverAnim = Math.max(hoverAnim - 0.12F * dt, target);
        }

        int bg, border, textColor;

        switch (style) {
            case ACCENT:
                bg = lerpColor(PWPTheme.Styles.Button.ACCENT_BG, PWPTheme.Styles.Button.ACCENT_HOVER, hoverAnim);
                border = PWPTheme.Colors.ACCENT_DIM;
                textColor = PWPTheme.Styles.Button.ACCENT_TEXT;
                break;
            case DANGER:
                bg = lerpColor(PWPTheme.Styles.Button.DANGER_BG, PWPTheme.Styles.Button.DANGER_HOVER, hoverAnim);
                border = PWPTheme.Colors.DANGER;
                textColor = PWPTheme.Styles.Button.DANGER_TEXT;
                break;
            case DARK:
                bg = lerpColor(PWPTheme.Styles.Button.DARK_BG, PWPTheme.Styles.Button.DARK_HOVER, hoverAnim);
                border = hovered ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Styles.Button.DARK_BORDER;
                textColor = hovered ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_PRIMARY;
                break;
            case GHOST:
                bg = hoverAnim > 0.01F ? lerpColor(0x00000000, PWPTheme.Colors.SURFACE_LIGHT, hoverAnim) : 0x00000000;
                border = hoverAnim > 0.01F ? lerpColor(0x00000000, PWPTheme.Colors.BORDER_FOCUS, hoverAnim) : 0x00000000;
                textColor = hoverAnim > 0.01F ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY;
                break;
            default: // PRIMARY
                bg = lerpColor(PWPTheme.Styles.Button.PRIMARY_BG, PWPTheme.Styles.Button.PRIMARY_HOVER, hoverAnim);
                border = active ? (hovered ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Styles.Button.PRIMARY_BORDER) : PWPTheme.Colors.BORDER;
                textColor = active ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY;
                break;
        }

        if (!active) {
            border = PWPTheme.Colors.BORDER;
        }

        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;
        int r = PWPTheme.Spacing.RADIUS_SMALL;

        gui.fill(x + r, y, x + w - r, y + h, bg);
        gui.fill(x, y + r, x + r, y + h - r, bg);
        gui.fill(x + w - r, y + r, x + w, y + h - r, bg);
        gui.fill(x + r, y + r, x + w - r, y + h - r, bg);

        gui.fill(x + r, y, x + w - r, y + 1, border);
        gui.fill(x + r, y + h - 1, x + w - r, y + h, border);
        gui.fill(x, y + r, x + 1, y + h - r, border);
        gui.fill(x + w - 1, y + r, x + w, y + h - r, border);
        gui.fill(x + r, y, x + r + 1, y + 1, border);
        gui.fill(x + w - r - 1, y, x + w - r, y + 1, border);
        gui.fill(x + r, y + h - 1, x + r + 1, y + h, border);
        gui.fill(x + w - r - 1, y + h - 1, x + w - r, y + h, border);

        int textY = y + (h - 8) / 2;
        var mcFont = Minecraft.getInstance().font;
        String msgStr = this.getMessage().getString();
        int textW = mcFont.width(msgStr);

        int shadowColor = (64 << 24) | (0x000000 & 0x00FFFFFF);

        if (textW > w - 10) {
            String shortText = mcFont.plainSubstrByWidth(msgStr, w - 14) + "...";
            gui.drawString(mcFont, shortText, x + 5, textY + 1, shadowColor);
            gui.drawString(mcFont, shortText, x + 5, textY, textColor);
        } else {
            gui.drawCenteredString(mcFont, this.getMessage(), x + w / 2, textY + 1, shadowColor);
            gui.drawCenteredString(mcFont, this.getMessage(), x + w / 2, textY, textColor);
        }

        if (active && pressed) {
            gui.fill(x + 1, y + h - 1, x + w - 1, y + h, PWPTheme.Colors.ACCENT_DIM);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private int lerpColor(int from, int to, float t) {
        if (t <= 0) return from;
        if (t >= 1) return to;
        int a1 = (from >> 24) & 0xFF;
        int r1 = (from >> 16) & 0xFF;
        int g1 = (from >> 8) & 0xFF;
        int b1 = from & 0xFF;
        int a2 = (to >> 24) & 0xFF;
        int r2 = (to >> 16) & 0xFF;
        int g2 = (to >> 8) & 0xFF;
        int b2 = to & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
