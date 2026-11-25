package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.hud.CenterHud;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIDamp;
import com.pwp.coreclient.ui.hud.ScreenProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

// Канал дрона (установка / перезарядка мавика). Отрисовка центра — client/hud/CenterHud (этап 5,
// кит HUD): сегментное кольцо прогресса вокруг прицела (16 сегментов, ведущий — частично), ТРИ
// лопасти по 120° — ТОЛЬКО при установке (mode=0); вращение по полному времени мира
// (getGameTime + frameTime — НЕ partialTick сам по себе: это 0..1 за тик, лопасть «стояла» и
// дёргалась); маркер-плашка НАД дроном (y+1.3 — на высоте корпуса, а не на земле) с секундами.
// Плашка в контекстном ярусе низ-центра (HudZones.ContextTier, высший приоритет): имя дрона +
// честный таймер T-Nс + полоса прогресса.
// Прогресс сглаживается клиентским lerp (плавность HUD, реальное время — UIDamp).
// Рисуется ОДИН раз за кадр — после CHAT_PANEL (Post приходит на каждый ванильный оверлей). Авто-скрыл:
// если пакеты не приходят >60 тиков (3с — пакеты идут каждые 5 тиков), канал считаем мёртвым и
// гуишку прячем (дрон пропал/сервер умер).
// Состояние приходит с сервера только игроку канала (PacketDistributor.PLAYER).
@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
public class DroneDeployOverlay {
    private static final UIDamp.Frame FRAME = new UIDamp.Frame();
    private static final CenterHud CENTER = new CenterHud();
    private static final CenterHud.Model MODEL = new CenterHud.Model();
    private static final ScreenProjection PROJECTION = new ScreenProjection();

    private DroneDeployOverlay() {}

    /** Заявка в контекстный ярус (текст канала живёт там, а не под прицелом). */
    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        if (ClientData.droneDeployActive && WHud.hudVisible()) WHud.contextOffer(WHud.CTX_DRONE);
    }

    // LOW: после WarfareOverlay (NORMAL) — старые look-at к этому моменту уже зарезервировали ярус
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;
        if (!ClientData.droneDeployActive) return;

        Minecraft mc = Minecraft.getInstance();
        // Авто-скрыл: канал умер, если пакеты не обновлялись слишком долго.
        if (ClientData.clientTickCounter - ClientData.droneDeployLastUpdate > 60) {
            ClientData.droneDeployActive = false;
            ClientData.droneDeployDisplayProgress = 0.0F;
            return;
        }
        if (!WHud.hudVisible()) return; // общий гейт HUD (F1)

        GuiGraphics gui = event.getGuiGraphics();
        // Плавность: клиентский lerp к серверному прогрессу.
        // 0.12/кадр при 60 FPS (прежний покадровый коэффициент), в реальном времени.
        ClientData.droneDeployDisplayProgress = UIDamp.approach(
                ClientData.droneDeployDisplayProgress,
                Mth.clamp(ClientData.droneDeployProgress, 0.0F, 1.0F),
                0.12F,
                FRAME.dtMs());

        // центр (этап 5): кольцо прогресса, лопасти (только установка), маркер над дроном
        CenterHud.fromClient(MODEL, mc, PROJECTION);
        CENTER.render(gui, PWPTheme.Fonts.display(), WHud.zones(), MODEL);

        // плашка контекстного яруса низ-центра: имя дрона + честный таймер T-Nс + полоса
        boolean deploying = MODEL.deploying;
        WHud.Fmt subFmt = deploying ? SUB_DEPLOY : SUB_RELOAD;
        subFmt.of(MODEL.secondsLeft);
        int accent = deploying ? WTheme.CENTER_DRONE_DEPLOY : WTheme.CENTER_DRONE_RELOAD;
        WHud.contextPlate(
                gui,
                PWPTheme.Fonts.display(),
                WHud.CTX_DRONE,
                WarfareIcon.TARGET,
                deploying ? droneName(ClientData.droneDeployTypeId) : MAVIC_RELOAD,
                subFmt.string(),
                accent,
                MODEL.progress,
                accent,
                1.0F);
    }

    // строки — lang (gui.pwpwarfare.hud.drone.*), компоненты кэшированы
    private static final Component NAME_FPV = Component.translatable("gui.pwpwarfare.hud.drone.fpv");
    private static final Component NAME_MAVIC_DROP = Component.translatable("gui.pwpwarfare.hud.drone.mavic_drop");
    private static final Component NAME_MAVIC_SCOUT = Component.translatable("gui.pwpwarfare.hud.drone.mavic_scout");
    private static final Component NAME_DRONE = Component.translatable("gui.pwpwarfare.hud.drone.generic");
    private static final Component MAVIC_RELOAD = Component.translatable("gui.pwpwarfare.hud.drone.mavic_reload");
    private static final WHud.Fmt SUB_DEPLOY = new WHud.Fmt("gui.pwpwarfare.hud.drone.deploying");
    private static final WHud.Fmt SUB_RELOAD = new WHud.Fmt("gui.pwpwarfare.hud.drone.reloading");

    private static Component droneName(String typeId) {
        return switch (typeId) {
            case "uncomplicatedfpv:fpv_drone" -> NAME_FPV;
            case "uncomplicatedfpv:mavic_drone_with_drop" -> NAME_MAVIC_DROP;
            case "uncomplicatedfpv:mavic_drone_no_drop" -> NAME_MAVIC_SCOUT;
            default -> NAME_DRONE;
        };
    }
}
