package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.movement.Stamina;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * Выносливость и раскачка прицела в матче (этап 15 ROADMAP), модель — {@link Stamina}. Клиент: бег тратит,
 * на нуле бег выключается до восстановления; при низкой выносливости вид плавно «гуляет» по кругу (амплитуда —
 * {@link Stamina#sway}), стрелять после рывка точно не выйдет. Плашка «Выносливость» в стеке низ-лево — только
 * когда она не полная. Не действует в лобби, в технике, в креативе/наблюдателе и при открытом экране.
 */
@OnlyIn(Dist.CLIENT)
public final class StaminaClient {
    private static final Stamina STAMINA = new Stamina();
    private static final Component LABEL = Component.translatable("gui.pwpwarfare.hud.stamina");
    private static boolean wasOnGround = true;
    private static int t;
    private static float lastYaw;
    private static float lastPitch;

    private StaminaClient() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(StaminaClient::onTick);
        MinecraftForge.EVENT_BUS.addListener(StaminaClient::onHud);
    }

    private static boolean applies(Minecraft mc, LocalPlayer p) {
        return p != null
                && mc.getConnection() != null
                && !com.pwp.coreclient.lobby.LobbyHud.isOnLobby()
                && !p.isCreative()
                && !p.isSpectator()
                && p.getVehicle() == null;
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!applies(mc, p)) {
            STAMINA.reset();
            lastYaw = lastPitch = 0F;
            return;
        }
        boolean onGround = p.onGround();
        boolean jumped = wasOnGround && !onGround && p.getDeltaMovement().y > 0.1;
        wasOnGround = onGround;
        STAMINA.tick(p.isSprinting(), p.isCrouching(), jumped);
        if (!STAMINA.canSprint() && p.isSprinting()) p.setSprinting(false);

        // раскачка: смещение вида по замкнутой кривой, применяется разницей между тиками (без накопления)
        t++;
        float amp = mc.screen == null ? STAMINA.sway() : 0F;
        float yaw = amp * (float) Math.sin(t * 0.13) * 0.8F;
        float pitch = amp * (float) Math.sin(t * 0.21 + 1.0) * 0.5F;
        if (amp > 0F || lastYaw != 0F || lastPitch != 0F) {
            p.setYRot(p.getYRot() + (yaw - lastYaw));
            p.setXRot(Math.max(-90F, Math.min(90F, p.getXRot() + (pitch - lastPitch))));
        }
        lastYaw = yaw;
        lastPitch = pitch;
    }

    private static void onHud(RenderGuiOverlayEvent.Post e) {
        if (!e.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;
        Minecraft mc = Minecraft.getInstance();
        if (!applies(mc, mc.player) || !WHud.hudVisible()) return;
        float v = STAMINA.value();
        if (v >= 0.999F) return;
        boolean low = !STAMINA.canSprint();
        WHud.stackPlate(
                e.getGuiGraphics(),
                PWPTheme.Fonts.display(),
                WHud.zones().bottomLeftStack(),
                WarfareIcon.PERSON,
                LABEL,
                Math.round(v * 100) + "%",
                low ? WTheme.HUD_DANGER : WTheme.HUD_VALUE,
                v,
                low ? WTheme.HUD_DANGER : PWPTheme.Colors.ACCENT,
                1F);
    }
}
