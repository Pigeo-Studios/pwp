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
    private long lastRenderTime;
    private float animAlpha = 1.0F;
    private boolean loading;

    public PWPButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, Style.PRIMARY);
    }

    public PWPButton(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.style = style;
        this.hoverAnim = 0.0F;
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

        float target = (hovered && active) ? 1.0F : 0.0F;
        if (active && hovered && Minecraft.getInstance().mouseHandler.isLeftPressed()) {
            target = 0.5F;
        }

        float speed = 0.15F;
        if (target > hoverAnim) {
            hoverAnim += (target - hoverAnim) * (1 - (float) Math.exp(-speed * dt * 50));
        } else {
            hoverAnim += (target - hoverAnim) * (1 - (float) Math.exp(-speed * dt * 50));
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.animAlpha);

        int bg, border, textColor;

        if (!active) {
            bg = PWPTheme.Styles.Button.DARK_BG;
            border = PWPTheme.Colors.BORDER;
            textColor = PWPTheme.Colors.TEXT_DIM;
        } else {
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
                    textColor = PWPTheme.Colors.TEXT_PRIMARY;
                    break;
                case GHOST:
                    int ghostBg = lerpColor(0x00000000, PWPTheme.Colors.SURFACE_LIGHT, hoverAnim);
                    int ghostBorder = lerpColor(0x00000000, PWPTheme.Colors.BORDER_FOCUS, hoverAnim);
                    border = ghostBorder;
                    bg = ghostBg;
                    textColor = hoverAnim > 0.01F ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY;
                    break;
                default:
                    bg = lerpColor(PWPTheme.Styles.Button.PRIMARY_BG, PWPTheme.Styles.Button.PRIMARY_HOVER, hoverAnim);
                    border = hovered ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Styles.Button.PRIMARY_BORDER;
                    textColor = PWPTheme.Colors.TEXT_PRIMARY;
                    break;
            }
        }

        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;
        int r = PWPTheme.Spacing.RADIUS_SMALL;

        // Pass 1: Fill — full rounded rectangle area
        drawRoundedFill(gui, x, y, w, h, r, bg);

        // Pass 2: Border — outer perimeter, no alpha overlap with fill
        drawRoundedBorder(gui, x, y, w, h, r, border);

        // Loading indicator: thin accent bar at bottom
        if (loading && active) {
            long cycle = now % 1200;
            float loadProgress = (float) cycle / 1200.0F;
            int loadW = Math.max(8, w / 4);
            int loadX = x + (int) ((w - loadW) * loadProgress);
            gui.fill(loadX, y + h - 2, loadX + loadW, y + h, PWPTheme.Colors.ACCENT);
        }

        // Text
        var mcFont = Minecraft.getInstance().font;
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

    private void drawRoundedFill(GuiGraphics gui, int x, int y, int w, int h, int r, int color) {
        gui.fill(x + r, y, x + w - r, y + h, color);
        gui.fill(x, y + r, x + r, y + h - r, color);
        gui.fill(x + w - r, y + r, x + w, y + h - r, color);
        gui.fill(x + r, y + r, x + w - r, y + h - r, color);
    }

    private void drawRoundedBorder(GuiGraphics gui, int x, int y, int w, int h, int r, int color) {
        gui.fill(x + r, y, x + w - r, y + 1, color);
        gui.fill(x + r, y + h - 1, x + w - r, y + h, color);
        gui.fill(x, y + r, x + 1, y + h - r, color);
        gui.fill(x + w - 1, y + r, x + w, y + h - r, color);
        gui.fill(x + r, y, x + r + 1, y + 1, color);
        gui.fill(x + w - r - 1, y, x + w - r, y + 1, color);
        gui.fill(x + r, y + h - 1, x + r + 1, y + h, color);
        gui.fill(x + w - r - 1, y + h - 1, x + w - r, y + h, color);
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
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
