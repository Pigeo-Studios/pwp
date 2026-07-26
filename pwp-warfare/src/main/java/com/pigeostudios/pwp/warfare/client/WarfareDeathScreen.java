package com.pigeostudios.pwp.warfare.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.gui.DeployScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WarfareDeathScreen extends Screen {

    private boolean deployOpened;

    public WarfareDeathScreen(Component cause, boolean hardcore) {
        super(Component.literal("ВЫ МЕРТВЫ"));
        ClientData.globalDeathTimestamp = System.currentTimeMillis();
        ClientData.deathFadeStartTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        gui.fill(0, 0, width, height, 0xFF000000);

        if (ClientData.deathFadeStartTime != 0L) {
            long elapsed = System.currentTimeMillis() - ClientData.deathFadeStartTime;
            float alpha = 0f;
            if (elapsed < 1000) alpha = 1f;
            else if (elapsed < 2000) alpha = 1f - (float)(elapsed - 1000) / 1000f;
            else ClientData.deathFadeStartTime = 0L;

            if (alpha > 0) {
                RenderSystem.enableBlend();
                gui.pose().pushPose();
                gui.pose().translate(0, 0, 1000);
                int a = (int)(alpha * 200);
                gui.fill(0, 0, width, height, a << 24);
                gui.drawCenteredString(PWPTheme.Fonts.display(), "ВЫ МЕРТВЫ",
                    width / 2, height / 2 - 20, ((int)(alpha * 255) << 24) | 0xFF4444);
                gui.pose().popPose();
                RenderSystem.disableBlend();
            }
        }

        if (!deployOpened && ClientData.deathFadeStartTime == 0L) {
            deployOpened = true;
            ClientData.deathFadePlayed = true;
            Minecraft.getInstance().setScreen(new DeployScreen());
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
