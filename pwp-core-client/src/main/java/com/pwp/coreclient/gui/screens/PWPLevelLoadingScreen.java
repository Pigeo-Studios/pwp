package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PWPLevelLoadingScreen extends Screen {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation("pwp_core_client", "textures/gui/loading.png");

    private final long openTime;
    private PWPTipsWidget tips;

    public PWPLevelLoadingScreen() {
        super(Component.literal("Загрузка мира"));
        openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        if (tips == null) {
            tips = new PWPTipsWidget((int) (width * 0.6f));
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        int cx = width / 2;
        int cy = height / 2;
        long elapsed = System.currentTimeMillis() - openTime;

        PWPUtils.renderLogo(gui, cx, (int) (height * 0.16f));
        PWPUtils.renderSpinner(gui, cx, cy - 30, elapsed);
        PWPUtils.renderStatusText(gui, "Загрузка мира...", cx, cy - 10, elapsed);
        PWPUtils.renderProgressBar(gui, cx, cy + 30, (int) (width * 0.3f), 4, elapsed);

        if (tips != null) {
            tips.render(gui, cx, cy + 55);
        }
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        gui.blit(BG_TEXTURE, 0, 0, 0, 0, w, h, w, h);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
