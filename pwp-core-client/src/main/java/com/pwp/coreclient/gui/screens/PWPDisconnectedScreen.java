package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PWPDisconnectedScreen extends Screen {

    private final Component reason;
    private long openTime;

    public PWPDisconnectedScreen(Component reason) {
        super(Component.literal("Соединение разорвано"));
        this.reason = reason;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        PWPRotatingBackground.render(gui, 0, 0, w, h);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;
        var font = PWPTheme.Fonts.display();

        long elapsed = System.currentTimeMillis() - openTime;

        var pose = gui.pose();

        // PWP Logo
        pose.pushPose();
        pose.translate(cx, (int) (h * 0.12f), 0);
        pose.scale(1.6f, 1.6f, 1f);
        gui.drawString(font, Component.literal("PWP"), -font.width("PWP") / 2, 0, PWPTheme.Colors.ACCENT, false);
        pose.popPose();

        float fade = Math.min(elapsed / 250.0F, 1f);
        gui.setColor(1, 1, 1, fade);

        // Content box
        String reasonStr = reason.getString();
        int reasonW = font.width(reasonStr);
        int boxW = Math.min(360, Math.max(200, reasonW + 40));
        boxW = Math.min(boxW, w - 40);
        int boxH = 60;
        int boxY = cy - 30;
        PWPUtils.renderBox(gui, cx, boxY, boxW, boxH);

        String title = "Соединение разорвано";
        gui.drawString(font, Component.literal(title), cx - font.width(title) / 2, cy - 20, PWPTheme.Colors.DANGER, false);

        if (!reasonStr.isEmpty()) {
            int maxW = (int) (w * 0.55f);
            if (font.width(reasonStr) > maxW) {
                reasonStr = font.plainSubstrByWidth(reasonStr, maxW - 4) + "...";
            }
            gui.drawString(font, Component.literal(reasonStr), cx - font.width(reasonStr) / 2, cy + 4, PWPTheme.Colors.TEXT_SECONDARY, false);
        }
        gui.setColor(1, 1, 1, 1);

        super.render(gui, mx, my, pt);

        if (fade >= 1 && children().isEmpty()) {
            addRenderableWidget(new PWPButton(
                cx - 80, cy + 50, 160, 28,
                Component.literal("Вернуться в меню"),
                btn -> Minecraft.getInstance().setScreen(new PWPMainMenuScreen()),
                PWPButton.Style.ACCENT
            ));
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
