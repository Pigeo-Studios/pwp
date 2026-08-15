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
            float alpha = 0f;
            // Экран «ВЫ МЕРТВЫ» держится 4 секунды, затем фейд-аут 1 секунда (всего 5с)
            if (elapsed < 4000) alpha = 1f;
            else if (elapsed < 5000) alpha = 1f - (float)(elapsed - 4000) / 1000f;
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
            ClientData.deployRequested = true;
            // Экран деплоя: войс снова доступен (локал мёртвому режет сервер)
            com.pigeostudios.pwp.warfare.network.PacketHandler.INSTANCE.sendToServer(
                new com.pigeostudios.pwp.warfare.network.PacketVoiceDeathState(false)
            );
            Minecraft.getInstance().setScreen(new DeployScreen());
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    // Экран смерти закрывается только при оживлении или переходом в деплой — не по ESC
    @Override public boolean shouldCloseOnEsc() { return false; }
}
