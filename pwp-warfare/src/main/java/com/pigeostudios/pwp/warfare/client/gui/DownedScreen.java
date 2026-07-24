package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.network.PacketDownedAction;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class DownedScreen extends Screen {

    private static final ResourceLocation VIGNETTE = new ResourceLocation("pwpwarfare", "textures/misc/vignette.png");
    private PWPButton callMedicButton;
    private final long screenOpenTime;
    private long lastMedicCallTime;

    public DownedScreen() {
        super(Component.translatable("gui.pwpwarfare.downed.title"));
        screenOpenTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int bottomY = height - 55;

        addRenderableWidget(new PWPButton(cx - 130, bottomY, 120, 24,
            Component.literal("Сдаться"),
            b -> { PacketHandler.INSTANCE.sendToServer(new PacketDownedAction(1)); onClose(); },
            PWPButton.Style.DANGER));

        callMedicButton = addRenderableWidget(new PWPButton(cx + 10, bottomY, 120, 24,
            Component.literal("Вызвать медика"),
            b -> {
                long now = System.currentTimeMillis();
                if (now - lastMedicCallTime >= 15000) {
                    PacketHandler.INSTANCE.sendToServer(new PacketDownedAction(0));
                    lastMedicCallTime = now;
                }
            }, PWPButton.Style.PRIMARY));
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderVignette(gui);

        int cx = width / 2;
        int boxX = cx - 170;
        int boxY = height - 155;
        int boxW = 340;
        int boxH = 130;

        PWPPanel.render(gui, boxX, boxY, boxW, boxH, PWPPanel.Variant.ACCENT_BORDER);

        long now = System.currentTimeMillis();
        float pulse = 0.7f + 0.3f * (float) Math.sin((now - screenOpenTime) * 0.005);
        RenderSystem.setShaderColor(pulse, 0.2f, 0.2f, 1f);
        gui.drawCenteredString(font, Component.literal("\u2764"), cx, boxY + 16, PWPTheme.Colors.DANGER);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        Component allyStatus = getAllyDistanceStatus();
        gui.drawCenteredString(font, allyStatus, cx, boxY + 50, PWPTheme.Colors.TEXT_PRIMARY);

        int maxSeconds = WarfareConfig.MAX_DOWNED_TIME_SECONDS.get();
        long remaining = maxSeconds - (now - screenOpenTime) / 1000;
        if (remaining < 0) remaining = 0;
        float pct = (float) remaining / maxSeconds;

        int barW = 280;
        int barH = 6;
        int barX = cx - barW / 2;
        int barY = boxY + 68;

        gui.fill(barX, barY, barX + barW, barY + barH, PWPTheme.Styles.Progress.BG);
        int fillColor = pct > 0.3f ? PWPTheme.Colors.SUCCESS : (pct > 0.15f ? PWPTheme.Colors.WARNING : PWPTheme.Colors.DANGER);
        gui.fill(barX, barY, barX + (int) (barW * pct), barY + barH, fillColor);

        gui.drawCenteredString(font, Component.literal("Кровотечение: " + remaining + "с"), cx, boxY + 78, PWPTheme.Colors.TEXT_SECONDARY);

        long cd = 15000 - (now - lastMedicCallTime);
        if (cd > 0) {
            callMedicButton.setMessage(Component.literal("Медик через " + (cd / 1000 + 1) + "с"));
            callMedicButton.active = false;
        } else {
            callMedicButton.setMessage(Component.literal("Вызвать медика"));
            callMedicButton.active = true;
        }

        super.render(gui, mx, my, pt);
    }

    private Component getAllyDistanceStatus() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return Component.translatable("gui.pwpwarfare.downed.no_allies");

        double minDist = Double.MAX_VALUE;
        Player closest = null;
        for (Player other : mc.level.players()) {
            if (other == mc.player || other.isSpectator() || !other.isAlive()) continue;
            if (mc.player.getTeam() == null || other.getTeam() != mc.player.getTeam()) continue;
            double d = mc.player.distanceTo(other);
            if (d < minDist) { minDist = d; closest = other; }
        }
        if (closest != null && minDist <= 250) {
            return Component.literal(closest.getScoreboardName() + " — " + (int) minDist + "м");
        }
        return Component.literal("Рядом нет союзников");
    }

    private void renderVignette(GuiGraphics gui) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.8f, 0f, 0f, 0.05f);
        gui.blit(VIGNETTE, 0, 0, 0, 0, width, height, width, height);
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.8f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
