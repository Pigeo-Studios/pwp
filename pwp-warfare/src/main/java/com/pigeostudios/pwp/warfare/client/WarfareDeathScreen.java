package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.DeployScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран смерти матча в стиле кита: тёмный фон, красная виньетка, крупный заголовок «Вы мертвы»,
 * акцентная линия и подпись «открывается меню развёртывания». Поведение прежнее: 1 с заголовок, 1 с
 * фейд, затем {@link DeployScreen}; игрок ожил — экран закрывается. Таймер — {@code
 * ClientData.deathFadeStartTime} (его же читают ClientEvents).
 *
 * <p>Превью тест-стенда ({@link #preview()}): ClientData не трогает, деплой не открывает — фейд
 * идёт по кругу.
 */
public class WarfareDeathScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.pwpwarfare.death.title");
    private static final Component SUBTITLE = Component.translatable("gui.pwpwarfare.death.redeploy");

    private final boolean preview;
    private final long previewStart = Util.getMillis();
    private boolean deployOpened;

    public WarfareDeathScreen(Component cause, boolean hardcore) {
        this(false);
        ClientData.globalDeathTimestamp = System.currentTimeMillis();
        ClientData.deathFadeStartTime = System.currentTimeMillis();
    }

    private WarfareDeathScreen(boolean preview) {
        super(TITLE);
        this.preview = preview;
    }

    /** Тест-стенд UI: без побочных эффектов, заголовок и фейд по кругу. */
    public static WarfareDeathScreen preview() {
        return new WarfareDeathScreen(true);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        gui.fill(0, 0, width, height, WTheme.DEATH_BG);

        if (preview) {
            long t = (Util.getMillis() - previewStart) % WTheme.DEATH_PREVIEW_CYCLE_MS;
            drawTitle(gui, alphaAt(t));
            return;
        }

        // Страховка: игрок ожил (респавн прошёл) — экран смерти больше не нужен
        var p = Minecraft.getInstance().player;
        if (p != null && !p.isDeadOrDying()) {
            ClientData.deathFadeStartTime = 0L;
            ClientData.deployRequested = false;
            ClientData.awaitingRespawn = false;
            Minecraft.getInstance().setScreen(null);
            return;
        }

        if (ClientData.deathFadeStartTime != 0L) {
            long elapsed = System.currentTimeMillis() - ClientData.deathFadeStartTime;
            if (elapsed >= WTheme.DEATH_HOLD_MS + WTheme.DEATH_FADE_MS) {
                ClientData.deathFadeStartTime = 0L;
            } else {
                drawTitle(gui, alphaAt(elapsed));
            }
        }

        if (!deployOpened && ClientData.deathFadeStartTime == 0L) {
            deployOpened = true;
            ClientData.deathFadePlayed = true;
            ClientData.deployRequested = true;
            Minecraft.getInstance().setScreen(new DeployScreen());
        }
    }

    /** Прозрачность заголовка: 1 до HOLD, затем линейно до 0 за FADE (прежние тайминги). */
    private static float alphaAt(long elapsed) {
        if (elapsed < WTheme.DEATH_HOLD_MS) return 1F;
        if (elapsed >= WTheme.DEATH_HOLD_MS + WTheme.DEATH_FADE_MS) return 0F;
        return 1F - (elapsed - WTheme.DEATH_HOLD_MS) / (float) WTheme.DEATH_FADE_MS;
    }

    private void drawTitle(GuiGraphics g, float a) {
        if (a <= 0F) return;
        UIDraw.vignette(g, width, height, UIDraw.fade(WTheme.DEATH_VIGNETTE, a), WTheme.DEATH_VIGNETTE_FRACTION);
        int scale = WTheme.DEATH_TITLE_SCALE;
        int lh = font.lineHeight;
        int titleW = font.width(TITLE) * scale;
        int blockH = lh * scale + Spacing.SM + WTheme.DEATH_LINE_H + Spacing.SM + lh;
        int y = (height - blockH) / 2;
        UIDraw.textScaled(g, font, TITLE, (width - titleW) / 2, y, scale, UIDraw.fade(WTheme.DEATH_TITLE, a), true);
        y += lh * scale + Spacing.SM;
        int lineW = Math.min(width - 2 * Spacing.SM, Math.max(titleW, WTheme.DEATH_LINE_W));
        UIDraw.accentBarH(
                g, (width - lineW) / 2, y, lineW, WTheme.DEATH_LINE_H, UIDraw.fade(PWPTheme.Colors.DANGER, a));
        y += WTheme.DEATH_LINE_H + Spacing.SM;
        UIDraw.centered(g, font, SUBTITLE, width / 2, y + lh / 2, UIDraw.fade(WTheme.DEATH_SUB, a));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
