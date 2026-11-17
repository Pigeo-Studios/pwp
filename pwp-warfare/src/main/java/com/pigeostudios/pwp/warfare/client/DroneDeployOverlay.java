package com.pigeostudios.pwp.warfare.client;

import com.mojang.math.Axis;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIDamp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

// Компактная анимация канала дрона (установка / перезарядка мавика):
// - кольцо прогресса вокруг прицела (16 сегментов, заполняется по прогрессу);
// - ТРИ вращающиеся лопасти-пропеллера (по 120°) — ТОЛЬКО при установке (mode=0),
//   вращение по полному времени мира (getGameTime + partialTick — НЕ partialTick
//   сам по себе: это 0..1 за тик, лопасть «стояла» и дёргалась);
// - компактный маркер-кольцо НАД дроном (y+1.3 — на высоте корпуса, а не на
//   земле: на земле кольцо уезжало под ноги дрону при взгляде сбоку);
// - плашка в контекстном ярусе низ-центра (HudZones.ContextTier, высший приоритет): имя дрона +
//   честный таймер T-Nс + полоса прогресса.
// Прогресс сглаживается клиентским lerp (плавность HUD, реальное время — UIDamp).
// Рисуется ОДИН раз за кадр — после CHAT_PANEL, как лента/фидбек (Post приходит
// на каждый ванильный оверлей: без фильтра кольцо рисовалось 20+ раз за кадр). Авто-скрыл: если
// пакеты не приходят >60 тиков (3с — пакеты идут каждые 5 тиков), канал
// считаем мёртвым и гуишку прячем (дрон пропал/сервер умер).
// Состояние приходит с сервера только игроку канала (PacketDistributor.PLAYER).
@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
public class DroneDeployOverlay {
    private static final UIDamp.Frame FRAME = new UIDamp.Frame();

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
        float progress = Mth.clamp(ClientData.droneDeployDisplayProgress, 0.0F, 1.0F);
        int cx = gui.guiWidth() / 2;
        int cy = gui.guiHeight() / 2;
        // Полное время: тики мира + partialTick — плавное непрерывное вращение.
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + mc.getFrameTime();
        boolean deploying = ClientData.droneDeployMode == 0;

        // Кольцо прогресса вокруг прицела.
        drawRing(gui, cx, cy, 21.0F, progress);

        if (deploying) {
            // Три лопасти-пропеллера (по 120°), 360° за 90 тиков (4.5с).
            float angle = (time % 90.0F) / 90.0F * 360.0F;
            var pose = gui.pose();
            pose.pushPose();
            pose.translate(cx, cy, 0.0F);
            for (int i = 0; i < 3; i++) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(angle + i * 120.0F));
                gui.fill(-4, -24, 4, -13, 0xCCFFFFFF);
                gui.fill(-2, -28, 2, -24, 0x88FFFFFF);
                pose.popPose();
            }
            pose.popPose();
        }

        // Маркер над дроном (на высоте корпуса, только когда дрон в кадре).
        // Максимально дешёвый: статичный пин-квадрат, без пульсации радиуса и
        // без анимации alpha (кольцо с 12 сегментами + пересчёт радиуса на каждый
        // кадр заметно проседало на слабых ПК — юзер: «метка лагает, фпс в 9»).
        int[] screen = projectToScreen(
                mc,
                new Vec3(ClientData.droneDeployX + 0.5, ClientData.droneDeployY + 1.3, ClientData.droneDeployZ + 0.5));
        if (screen != null) {
            gui.fill(screen[0] - 4, screen[1] - 4, screen[0] + 5, screen[1] + 5, 0x66FFFFFF);
            gui.fill(screen[0] - 2, screen[1] - 2, screen[0] + 3, screen[1] + 3, 0xCCFFC96A);
        }

        // Текст под прицелом: имя дрона + таймер.
        int totalTicks = Math.max(1, ClientData.droneDeployTotalTicks);
        int secondsLeft = Math.max(0, (int) Math.ceil((1.0F - progress) * totalTicks / 20.0F));
        WHud.Fmt subFmt = deploying ? SUB_DEPLOY : SUB_RELOAD;
        subFmt.of(secondsLeft);
        String sub = subFmt.string();

        // плашка контекстного яруса низ-центра (раньше — блок под прицелом cy+34..58)
        int accent = deploying ? PWPTheme.Colors.ACCENT : WTheme.HUD_POSITIVE;
        WHud.contextPlate(
                gui,
                PWPTheme.Fonts.display(),
                WHud.CTX_DRONE,
                WarfareIcon.TARGET,
                deploying ? droneName(ClientData.droneDeployTypeId) : MAVIC_RELOAD,
                sub,
                accent,
                progress,
                accent,
                1.0F);
    }

    /** Кольцо прогресса: сегменты заполняются пропорционально progress. */
    private static void drawRing(GuiGraphics gui, int cx, int cy, float radius, float progress) {
        int segments = 12;
        int filled = (int) (segments * progress);
        for (int i = 0; i < filled; i++) {
            double a = i * Math.PI * 2.0 / segments;
            int x = (int) (cx + Math.cos(a) * radius);
            int y = (int) (cy + Math.sin(a) * radius);
            gui.fill(x - 1, y - 1, x + 1, y + 1, 0xCC9AFF6A);
        }
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

    /** 3D -> 2D проекция точки на экран; null если точка за камерой. */
    private static int[] projectToScreen(Minecraft mc, Vec3 worldPos) {
        if (mc.gameRenderer == null || mc.player == null) return null;
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 rel = worldPos.subtract(camPos);

        Vec3 look = new Vec3(camera.getLookVector());
        double dotForward = rel.dot(look);
        if (dotForward < 0.05) return null;

        Vec3 up = new Vec3(camera.getUpVector());
        Vec3 right = look.cross(up);
        double fov =
                Math.toRadians(Math.max(30.0, Math.min(110.0, mc.options.fov().get() / 2.0)));
        double tanFov = Math.tan(fov);

        double rightDist = rel.dot(right);
        double upDist = rel.dot(up);

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int sx = (int) (w / 2.0 + (rightDist / dotForward) * (w / 2.0) / tanFov);
        int sy = (int) (h / 2.0 - (upDist / dotForward) * (h / 2.0) / tanFov);
        return new int[] {sx, sy};
    }
}
