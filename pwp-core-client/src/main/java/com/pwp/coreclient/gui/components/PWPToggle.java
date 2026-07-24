package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class PWPToggle extends AbstractWidget {

    private final Consumer<Boolean> onChange;
    private boolean toggled;
    private float thumbPos;
    private long lastRenderTime;

    public PWPToggle(int x, int y, int width, int height, Component message, boolean toggled, Consumer<Boolean> onChange) {
        super(x, y, width, height, message);
        this.toggled = toggled;
        this.onChange = onChange;
        this.thumbPos = toggled ? 1.0F : 0.0F;
        this.lastRenderTime = System.currentTimeMillis();
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (this.active && this.visible && clicked(mx, my)) {
            this.toggled = !this.toggled;
            if (onChange != null) onChange.accept(this.toggled);
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

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderTime) / 50.0F, 4.0F);
        lastRenderTime = now;

        float target = toggled ? 1.0F : 0.0F;
        float speed = 0.15F;
        thumbPos += (target - thumbPos) * (1 - (float) Math.exp(-dt * speed * 50));

        int x = this.getX();
        int y = this.getY();
        int w = 36;
        int h = 16;

        int trackColor = toggled ? PWPTheme.Colors.ACCENT_DIM : PWPTheme.Colors.SURFACE;
        gui.fill(x, y, x + w, y + h, trackColor);
        gui.fill(x, y, x + w, y + 1, PWPTheme.Colors.BORDER);
        gui.fill(x, y + h - 1, x + w, y + h, PWPTheme.Colors.BORDER);

        int thumbW = 12;
        int thumbX = x + 2 + (int) ((w - thumbW - 4) * thumbPos);
        int thumbColor = toggled ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_SECONDARY;
        gui.fill(thumbX, y + 2, thumbX + thumbW, y + h - 2, thumbColor);

        var font = Minecraft.getInstance().font;
        gui.drawString(font, this.getMessage(), x + w + 8, y + (h - 8) / 2, PWPTheme.Colors.TEXT_PRIMARY);
    }
}
