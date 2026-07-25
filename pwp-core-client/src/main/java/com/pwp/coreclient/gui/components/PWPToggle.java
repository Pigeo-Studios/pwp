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

    public PWPToggle(int x, int y, Component message, boolean toggled, Consumer<Boolean> onChange) {
        this(x, y, PWPTheme.Spacing.TOGGLE_WIDTH, PWPTheme.Spacing.TOGGLE_HEIGHT, message, toggled, onChange);
    }

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
        int w = this.width;
        int h = this.height;
        int thumbW = Math.max(6, (int) (h * 0.75F));
        int inset = Math.max(1, h / 8);

        int trackColor = toggled ? PWPTheme.Colors.ACCENT_DIM : PWPTheme.Colors.SURFACE;
        int r = PWPTheme.Spacing.RADIUS_ROUND;
        RoundedRect.fill(gui, x, y, w, h, Math.min(r, h / 2), trackColor);
        RoundedRect.border(gui, x, y, w, h, Math.min(r, h / 2), 1, PWPTheme.Colors.BORDER);

        int thumbX = x + inset + (int) ((w - thumbW - inset * 2) * thumbPos);
        int thumbColor = toggled ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_SECONDARY;
        RoundedRect.fill(gui, thumbX, y + inset, thumbW, h - inset * 2, (h - inset * 2) / 2, thumbColor);

        var font = PWPTheme.Fonts.display();
        gui.drawString(font, this.getMessage(), x + w + PWPTheme.Spacing.SM, y + (h - 8) / 2, PWPTheme.Colors.TEXT_PRIMARY);
    }
}
