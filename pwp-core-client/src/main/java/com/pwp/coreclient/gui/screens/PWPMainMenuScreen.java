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
    private PWPButton replayBtn;

    private boolean replayModAvailable;
    private boolean replayRecording;
    private int replayTextX, replayTextY, replayTextW, replayTextH;
    private boolean replayHovered;

    public PWPMainMenuScreen() {
        super(Component.literal("PWP"));
        openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();

        detectReplayMod();

        int cx = width / 2;
        int btnW = Math.min(width / 4, 220);
        int playH = 32;
        int otherH = 24;
        int gap = 6;

        int btnCount = replayModAvailable ? 5 : 4;
        int totalH = playH + otherH * (btnCount - 1) + gap * (btnCount - 1);
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
                ClientConnectHandler.connect(serverHost(), 25565);
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

        if (replayModAvailable) {
            replayBtn = addRenderableWidget(new PWPButton(
                cx - btnW / 2, startY + playH + (otherH + gap) * 3 + gap, btnW, otherH,
                Component.literal("REPLAY VIEWER"),
                btn -> openReplayViewer(),
                PWPButton.Style.GHOST
            ));
        }
    }

    private static String serverHost() {
        return System.getProperty("pwp.serverHost", "pigeo.asuscomm.com");
    }

    private void openReplayViewer() {
        try {
            Class<?> rc = Class.forName("com.replaymod.replay.ReplayModReplay");
            Object mod = rc.getField("instance").get(null);
            Class<?> vc = Class.forName("com.replaymod.replay.gui.screen.GuiReplayViewer");
            Object viewer = vc.getConstructor(rc).newInstance(mod);
            vc.getMethod("display").invoke(viewer);
        } catch (Exception ignored) {}
    }

    private void detectReplayMod() {
        replayModAvailable = false;
        try {
            Class.forName("com.replaymod.replay.ReplayModReplay");
            Class<?> rc = Class.forName("com.replaymod.core.ReplayMod");
            Object mod = rc.getField("instance").get(null);
            Object reg = rc.getMethod("getSettingsRegistry").invoke(mod);
            Class<?> sc = Class.forName("com.replaymod.recording.Setting");
            Class<?> sk = Class.forName("com.replaymod.core.SettingsRegistry$SettingKey");
            Object key = sc.getField("RECORD_SERVER").get(null);
            replayRecording = (boolean) reg.getClass()
                .getMethod("get", sk)
                .invoke(reg, key);
            replayModAvailable = true;
        } catch (Exception ignored) {}
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

        if (replayModAvailable) {
            renderReplayToggle(gui, mouseX, mouseY);
        }
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

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && replayModAvailable && replayHovered) {
            toggleReplayRecording();
            return true;
        }
        return super.mouseClicked(mx, my, button);
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
        PWPButton[] btns = replayModAvailable
            ? new PWPButton[]{ playBtn, singleBtn, settingsBtn, quitBtn, replayBtn }
            : new PWPButton[]{ playBtn, singleBtn, settingsBtn, quitBtn };
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

    private void renderReplayToggle(GuiGraphics gui, int mx, int my) {
        var font = PWPTheme.Fonts.display();
        String text = "REPLAY " + (replayRecording ? "ON" : "OFF");
        int tw = font.width(text);
        int tx = width - 16 - tw;
        int ty = 14;
        boolean hv = mx >= tx && mx <= tx + tw && my >= ty && my <= ty + 10;
        int color;
        if (replayRecording) {
            color = hv ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_PRIMARY;
        } else {
            color = hv ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_DIM;
        }
        gui.drawString(font, Component.literal(text), tx, ty, color, false);
        replayTextX = tx; replayTextY = ty; replayTextW = tw; replayTextH = 10;
        replayHovered = hv;
    }

    private void toggleReplayRecording() {
        try {
            Class<?> rc = Class.forName("com.replaymod.core.ReplayMod");
            Object mod = rc.getField("instance").get(null);
            Object reg = rc.getMethod("getSettingsRegistry").invoke(mod);
            Class<?> sc = Class.forName("com.replaymod.recording.Setting");
            Class<?> sk = Class.forName("com.replaymod.core.SettingsRegistry$SettingKey");
            Object key = sc.getField("RECORD_SERVER").get(null);
            replayRecording = !replayRecording;
            reg.getClass()
                .getMethod("set", sk, Object.class)
                .invoke(reg, key, replayRecording);
            reg.getClass().getMethod("save").invoke(reg);
        } catch (Exception ignored) {}
    }

    private static float animFade(float elapsed, long delay, long duration) {
        float t = (elapsed - delay) / (float) duration;
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        return Easing.easeOutCubic(t);
    }
}
