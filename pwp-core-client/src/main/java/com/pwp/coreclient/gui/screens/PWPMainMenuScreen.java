package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.ClientConnectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public class PWPMainMenuScreen extends Screen {

    private final long openTime;
    private boolean connecting;

    private PWPButton playBtn;
    private PWPButton singleBtn;
    private PWPButton settingsBtn;
    private PWPButton quitBtn;

    public PWPMainMenuScreen() {
        super(Component.literal("PWP"));
        openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();

        int cx = width / 2;
        int btnW = Math.min(width / 4, 220);
        int playH = 32;
        int otherH = 24;
        int gap = 6;

        int totalH = playH + otherH * 3 + gap * 3;
        int startY = (int) (height * 0.52f);
        if (startY + totalH > height - 40) {
            startY = height - 40 - totalH;
        }

        playBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY, btnW, playH,
            Component.translatable("pwp_core.main_menu.play"),
            btn -> {
                if (connecting) return;
                connecting = true;
                Minecraft.getInstance().setScreen(new PWPLoadingScreen(PWPLoadingScreen.Context.CONNECTING));
                ClientConnectHandler.connect("pigeo.asuscomm.com", 25565);
            },
            PWPButton.Style.ACCENT
        ));

        singleBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + playH + gap, btnW, otherH,
            Component.translatable("pwp_core.main_menu.singleplayer"),
            btn -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this)),
            PWPButton.Style.DARK
        ));

        settingsBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + playH + (otherH + gap) * 1 + gap, btnW, otherH,
            Component.translatable("pwp_core.main_menu.settings"),
            btn -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options)),
            PWPButton.Style.DARK
        ));

        quitBtn = addRenderableWidget(new PWPButton(
            cx - btnW / 2, startY + playH + (otherH + gap) * 2 + gap, btnW, otherH,
            Component.translatable("pwp_core.main_menu.quit"),
            btn -> Minecraft.getInstance().stop(),
            PWPButton.Style.GHOST
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);

        long elapsed = System.currentTimeMillis() - openTime;
        int cx = width / 2;

        float logoFade = animFade(elapsed, 0, 300);
        if (logoFade > 0) {
            renderLogo(gui, cx, (int) (height * 0.16f), logoFade);
        }

        renderButtons(gui, mouseX, mouseY, partialTick, elapsed);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        PWPRotatingBackground.render(gui, 0, 0, w, h);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void renderLogo(GuiGraphics gui, int cx, int y, float fade) {
        gui.setColor(1, 1, 1, fade);
        var font = PWPTheme.Fonts.display();
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(cx, y, 0);
        float scale = PWPTheme.Fonts.SIZE_TITLE / 8f / 2f;
        pose.scale(scale, scale, 1f);
        gui.drawString(font, Component.literal("PWP"), -font.width("PWP") / 2, 0, PWPTheme.Colors.ACCENT, false);
        pose.popPose();
        gui.setColor(1, 1, 1, 1);
    }

    private void renderButtons(GuiGraphics gui, int mouseX, int mouseY, float partialTick, float elapsed) {
        PWPButton[] btns = { playBtn, singleBtn, settingsBtn, quitBtn };
        int startDelay = 300;

        for (int i = 0; i < btns.length; i++) {
            PWPButton btn = btns[i];
            if (btn == null) continue;

            float progress = animFade(elapsed, startDelay + i * 50, 250);
            if (progress <= 0) { btn.visible = false; continue; }

            float eased = Math.min(progress, 1);
            int slideY = (int) ((1 - eased) * 12);
            btn.setAnimAlpha(eased);
            btn.visible = true;

            var pose = gui.pose();
            pose.pushPose();
            pose.translate(0, slideY, 0);
            btn.render(gui, mouseX, mouseY, partialTick);
            pose.popPose();
        }
    }

    private static float animFade(float elapsed, long delay, long duration) {
        float t = (elapsed - delay) / (float) duration;
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        return Easing.easeOutCubic(t);
    }
}
