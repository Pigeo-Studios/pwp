package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.animations.Easing;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPLayout;
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
    private static final int BG_W = 1920;
    private static final int BG_H = 1080;

    private final long openTime;
    private boolean connecting;

    private enum ServerStatus { UNKNOWN, ONLINE, OFFLINE, MAINTENANCE }
    private ServerStatus serverStatus = ServerStatus.UNKNOWN;

    private PWPButton playBtn;
    private PWPButton singleBtn;
    private PWPButton settingsBtn;
    private PWPButton quitBtn;

    public PWPMainMenuScreen() {
        super(Component.literal("PWP"));
        openTime = System.currentTimeMillis();
    }

    public void setServerStatus(ServerStatus status) {
        this.serverStatus = status;
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

        float welcomeFade = animFade(elapsed, 200, 300);
        if (welcomeFade > 0) {
            renderWelcome(gui, cx, welcomeFade);
        }

        renderButtons(gui, mouseX, mouseY, partialTick, elapsed);

        float statusFade = animFade(elapsed, 600, 300);
        if (statusFade > 0) {
            renderServerStatus(gui, cx, statusFade);
        }

        float footerFade = animFade(elapsed, 500, 300);
        if (footerFade > 0) {
            gui.setColor(1, 1, 1, footerFade);
            PWPLayout.renderFooter(gui, "PWP v1.0.1", width, height);
            gui.setColor(1, 1, 1, 1);
        }
    }

    private void renderServerStatus(GuiGraphics gui, int cx, float fade) {
        int playH = 32;
        int otherH = 24;
        int gap = 6;
        int totalBtnH = playH + otherH * 3 + gap * 3;
        int startY = (int) (height * 0.52f);
        if (startY + totalBtnH > height - 40) {
            startY = height - 40 - totalBtnH;
        }
        int btnBottom = startY + totalBtnH;
        int y = btnBottom + 20;
        if (y + 24 > height - 20) {
            y = btnBottom + 8;
        }

        gui.setColor(1, 1, 1, fade);

        String text;
        int color;
        switch (serverStatus) {
            case ONLINE -> { text = "РЎРµСЂРІРµСЂ: ONLINE"; color = PWPTheme.Colors.SUCCESS; }
            case OFFLINE -> { text = "РЎРµСЂРІРµСЂ РЅРµРґРѕСЃС‚СѓРїРµРЅ"; color = PWPTheme.Colors.DANGER; }
            case MAINTENANCE -> { text = "РўРµС…РЅРёС‡РµСЃРєРёРµ СЂР°Р±РѕС‚С‹"; color = PWPTheme.Colors.WARNING; }
            default -> { text = "РџСЂРѕРІРµСЂРєР° РїРѕРґРєР»СЋС‡РµРЅРёСЏ..."; color = PWPTheme.Colors.TEXT_DIM; }
        }

        gui.drawString(font, Component.literal(text), cx - font.width(text) / 2, y, color, false);

        if (serverStatus == ServerStatus.OFFLINE || serverStatus == ServerStatus.MAINTENANCE) {
            gui.drawString(font, Component.literal("РџРѕРїСЂРѕР±СѓР№С‚Рµ РїРѕР·Р¶Рµ"), cx - font.width("РџРѕРїСЂРѕР±СѓР№С‚Рµ РїРѕР·Р¶Рµ") / 2, y + 12, PWPTheme.Colors.TEXT_DIM, false);
        }

        gui.setColor(1, 1, 1, 1);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        float scale = Math.max((float) w / BG_W, (float) h / BG_H);
        int drawW = Math.round(BG_W * scale);
        int drawH = Math.round(BG_H * scale);
        int drawX = (w - drawW) / 2;
        int drawY = (h - drawH) / 2;

        gui.blit(BG_TEXTURE, drawX, drawY, drawW, drawH, 0, 0, BG_W, BG_H, BG_W, BG_H);
        gui.fill(0, 0, w, h, PWPTheme.Colors.BACKGROUND_DIM);
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

    private void renderWelcome(GuiGraphics gui, int cx, float fade) {
        var font = PWPTheme.Fonts.display();
        Minecraft mc = Minecraft.getInstance();

        String nickname = mc.player != null ? mc.player.getScoreboardName() : null;
        String welcome = nickname != null
            ? Component.translatable("pwp_core.main_menu.welcome", nickname).getString()
            : "Р”РѕР±СЂРѕ РїРѕР¶Р°Р»РѕРІР°С‚СЊ!";

        int welcomeY = (int) (height * 0.34f);
        int slideY = (int) ((1 - fade) * 6);

        var pose = gui.pose();
        pose.pushPose();
        pose.translate(0, slideY, 0);

        gui.drawString(font, Component.literal(welcome), cx - font.width(welcome) / 2, welcomeY, PWPTheme.Colors.TEXT_PRIMARY, false);
        String subStr = Component.translatable("pwp_core.main_menu.subtitle").getString();
        gui.drawString(font, Component.translatable("pwp_core.main_menu.subtitle"), cx - font.width(subStr) / 2, welcomeY + 16, PWPTheme.Colors.TEXT_SECONDARY, false);

        pose.popPose();
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
