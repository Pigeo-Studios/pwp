package com.pwp.coreclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * База элементов UI-кита. Только {@link GuiGraphics} и абсолютные координаты; все цвета/отступы —
 * из {@code PWPTheme}. Экран хранит виджеты (обычно в {@link UILayer}) и пересылает им события.
 * Состояние для отображения приходит с сервера DTO — виджеты игровую логику не считают.
 */
@OnlyIn(Dist.CLIENT)
public abstract class UIWidget {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible = true;
    protected boolean enabled = true;
    protected UITooltip.Content tooltip;

    protected UIWidget(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }

    public UIWidget setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        return this;
    }

    /** Рисование; вызывается каждый кадр, только если {@link #visible}. */
    public abstract void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    /** true — клик обработан (дальше не передаётся). */
    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean isMouseOver(double mx, double my) {
        return visible && mx >= x && my >= y && mx < x + width && my < y + height;
    }

    public UIWidget tooltip(UITooltip.Content content) {
        this.tooltip = content;
        return this;
    }

    public UITooltip.Content tooltip() {
        return tooltip;
    }

    public UIWidget setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public UIWidget setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }
}
