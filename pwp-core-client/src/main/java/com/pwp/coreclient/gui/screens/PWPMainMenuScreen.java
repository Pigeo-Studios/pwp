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
import net.minecraft.resources.ResourceLocation;

public class PWPMainMenuScreen extends Screen {

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation("pwp_core_client", "textures/gui/main_menu.png");

    private static final int BTN_WIDTH = 240;
    private static final int BTN_HEIGHT = 34;
    private static final int BTN_GAP = 10;
    private static final int ANIM_DELAY_PER_BTN = 100;
    private static final long ANIM_START_DELAY = 300;

    private long openTime;

    private PWPButton playBtn;
    private PWPButton singleBtn;
    private PWPButton optionsBtn;
    private PWPButton quitBtn;
    private PWPButton langBtn;

    public PWPMainMenuScreen() {
        super(Component.literal("PWP"));
    }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();

        int cx = width / 2;
        int startY = height / 2 + 6;

        playBtn = addRenderableWidget(new PWPButton(
            cx - BTN_WIDTH / 2, startY, BTN_WIDTH, BTN_HEIGHT,
            Component.translatable("pwp_core.main_menu.play"),
            btn -> ClientConnectHandler.connect("pigeo.asuscomm.com", 25565),
            PWPButton.Style.ACCENT
        ));

        singleBtn = addRenderableWidget(new PWPButton(
            cx - BTN_WIDTH / 2, startY + BTN_HEIGHT + BTN_GAP, BTN_WIDTH, BTN_HEIGHT,
            Component.translatable("pwp_core.main_menu.singleplayer"),
            btn -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this)),
            PWPButton.Style.PRIMARY
        ));

        optionsBtn = addRenderableWidget(new PWPButton(
            cx - BTN_WIDTH / 2, startY + (BTN_HEIGHT + BTN_GAP) * 2, BTN_WIDTH, BTN_HEIGHT,
            Component.translatable("pwp_core.main_menu.settings"),
            btn -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options)),
            PWPButton.Style.DARK
        ));

        quitBtn = addRenderableWidget(new PWPButton(
            cx - BTN_WIDTH / 2, startY + (BTN_HEIGHT + BTN_GAP) * 3, BTN_WIDTH, BTN_HEIGHT,
            Component.translatable("pwp_core.main_menu.quit"),
            btn -> Minecraft.getInstance().stop(),
            PWPButton.Style.DANGER
        ));

        langBtn = addRenderableWidget(new PWPButton(
            cx - 30, height - 24, 60, 14,
            Component.literal(""),
            btn -> {
                Minecraft mc = Minecraft.getInstance();
                String curr = mc.options.languageCode;
                mc.options.languageCode = curr.equals("ru_ru") ? "en_us" : "ru_ru";
                mc.options.save();
                mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
                mc.setScreen(new PWPMainMenuScreen());
            },
            PWPButton.Style.GHOST
        ));

        playBtn.visible = false;
        singleBtn.visible = false;
        optionsBtn.visible = false;
        quitBtn.visible = false;
        langBtn.visible = true;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        float elapsed = (float) (System.currentTimeMillis() - openTime);
        int cx = width / 2;
        int cy = height / 2;

        float titleFade = animProgress(elapsed, ANIM_START_DELAY, 500);
        if (titleFade > 0) {
            renderWelcome(gui, cx, cy - 78, Easing.easeOutCubic(Math.min(titleFade, 1)));
        }

        float sepFade = animProgress(elapsed, ANIM_START_DELAY + 300, 300);
        if (sepFade > 0) {
            renderSeparator(gui, cx, cy - 36, Easing.easeOutCubic(Math.min(sepFade, 1)));
        }

        float[] btnFades = new float[4];
        for (int i = 0; i < 4; i++) {
            btnFades[i] = animProgress(elapsed, ANIM_START_DELAY + 450 + i * ANIM_DELAY_PER_BTN, 350);
        }

        playBtn.visible = btnFades[0] >= 1;
        singleBtn.visible = btnFades[1] >= 1;
        optionsBtn.visible = btnFades[2] >= 1;
        quitBtn.visible = btnFades[3] >= 1;

        super.render(gui, mouseX, mouseY, partialTick);

        float vFade = animProgress(elapsed, ANIM_START_DELAY + 450 + 4 * ANIM_DELAY_PER_BTN + 150, 300);
        if (vFade > 0) {
            renderFooter(gui, Easing.easeOutCubic(Math.min(vFade, 1)));
        }

        renderLogo(gui, cx, 32, titleFade);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        gui.blit(BG_TEXTURE, 0, 0, 0, 0, w, h, w, h);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
    }

    private void renderLogo(GuiGraphics gui, int cx, int y, float fade) {
        if (fade <= 0) return;
        int alpha = (int) (Math.min(fade, 1) * 220);
        int color = (alpha << 24) | (PWPTheme.Colors.ACCENT & 0x00FFFFFF);
        var font = Minecraft.getInstance().font;
        gui.drawCenteredString(font, Component.literal("PWP"), cx, y, color);
    }

    private void renderWelcome(GuiGraphics gui, int cx, int y, float fade) {
        Minecraft mc = Minecraft.getInstance();
        var font = mc.font;

        String nickname = mc.player != null ? mc.player.getScoreboardName() : "";
        Component welcome = Component.translatable("pwp_core.main_menu.welcome", nickname);

        int color = withAlpha(PWPTheme.Colors.TEXT_PRIMARY, (int) (fade * 255));
        gui.drawCenteredString(font, welcome, cx, y, color);

        Component subtitle = Component.translatable("pwp_core.main_menu.subtitle");
        int subColor = withAlpha(PWPTheme.Colors.TEXT_SECONDARY, (int) (fade * 180));
        gui.drawCenteredString(font, subtitle, cx, y + 22, subColor);
    }

    private void renderSeparator(GuiGraphics gui, int cx, int y, float fade) {
        int w = 80;
        int alpha = (int) (fade * 160);
        int color = (alpha << 24) | (PWPTheme.Colors.ACCENT & 0x00FFFFFF);
        gui.fill(cx - w / 2, y, cx + w / 2, y + 1, color);
    }

    private void renderFooter(GuiGraphics gui, float fade) {
        Minecraft mc = Minecraft.getInstance();
        var font = mc.font;
        String text = "PWP v1.0.1";
        int color = withAlpha(PWPTheme.Colors.TEXT_DIM, (int) (fade * 140));
        gui.drawCenteredString(font, Component.literal(text), width / 2, height - 14, color);
    }

    private float animProgress(float elapsed, long delay, long duration) {
        float t = (elapsed - delay) / duration;
        return Math.min(t, 1);
    }

    private int withAlpha(int color, int alpha) {
        return (Math.min(255, Math.max(0, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
