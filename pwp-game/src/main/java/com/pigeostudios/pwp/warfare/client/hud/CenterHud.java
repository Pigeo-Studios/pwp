package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.hud.HudDraw;
import com.pwp.coreclient.ui.hud.HudZones;
import com.pwp.coreclient.ui.hud.RingMath;
import com.pwp.coreclient.ui.hud.ScreenProjection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Центр экрана HUD (этап 5): канал дрона — сегментное кольцо прогресса у прицела, три лопасти
 * (только установка, mode 0) и маркер над дроном с плашкой секунд. Плашка канала (имя + T-N с +
 * полоса) живёт в контекстном ярусе низ-центра — её рисует {@code DroneDeployOverlay}.
 *
 * <p>Как {@link TopHud}/{@link SideHud}: {@link Model} наполняет {@link #fromClient} в матче и
 * фикстура в превью ({@code /pwpui open hud}) — код отрисовки один. Без аллокаций в кадре: поворот
 * — переиспользуемые кватернионы, проекция маркера — {@link ScreenProjection}, строки — кэш {@link
 * WHud.Fmt}.
 */
@OnlyIn(Dist.CLIENT)
public final class CenterHud {

    /** Данные кадра (наполняются в {@link #fromClient} или фикстурой превью). */
    public static final class Model {
        /** Канал дрона идёт (иначе центр warfare пуст). */
        public boolean drone;
        /** Установка (mode 0, с лопастями) или перезарядка мавика (mode 1). */
        public boolean deploying;
        /** Сглаженный прогресс 0..1. */
        public float progress;
        /** Непрерывное время для лопастей: тики мира + доля кадра. */
        public float time;
        /** Сколько секунд осталось (для плашки у маркера). */
        public int secondsLeft;
        /** Маркер над дроном виден (точка перед камерой), координаты — GUI-пиксели. */
        public boolean marker;

        public int markerX;
        public int markerY;
    }

    private static final Quaternionf SPIN = new Quaternionf();
    private static final Quaternionf THIRD = new Quaternionf();
    private final WHud.Fmt seconds = new WHud.Fmt("gui.pwpwarfare.hud.drone.marker");

    /**
     * Канал дрона из {@link ClientData}: прогресс уже сглажен вызывающим ({@code
     * ClientData.droneDeployDisplayProgress}), время — {@code getGameTime() + frameTime} (НЕ
     * partialTick сам по себе — иначе лопасть «стоит» и дёргается).
     */
    public static void fromClient(Model m, Minecraft mc, ScreenProjection proj) {
        m.drone = ClientData.droneDeployActive;
        if (!m.drone) {
            m.marker = false;
            return;
        }
        m.deploying = ClientData.droneDeployMode == 0;
        m.progress = RingMath.clampFrac(ClientData.droneDeployDisplayProgress);
        m.time = (mc.level != null ? mc.level.getGameTime() : 0) + mc.getFrameTime();
        m.secondsLeft = RingMath.secondsLeft(m.progress, ClientData.droneDeployTotalTicks);
        m.marker = false;
        if (mc.gameRenderer == null || mc.player == null) return;
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 cp = cam.getPosition();
        Vector3f look = cam.getLookVector();
        Vector3f up = cam.getUpVector();
        m.marker = proj.project(
                ClientData.droneDeployX + 0.5 - cp.x,
                ClientData.droneDeployY + WTheme.CENTER_MARKER_LIFT - cp.y,
                ClientData.droneDeployZ + 0.5 - cp.z,
                look.x(),
                look.y(),
                look.z(),
                up.x(),
                up.y(),
                up.z(),
                mc.options.fov().get(),
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight());
        m.markerX = proj.x;
        m.markerY = proj.y;
    }

    public void render(GuiGraphics g, Font font, HudZones z, Model m) {
        if (!m.drone) return;
        int accent = m.deploying ? WTheme.CENTER_DRONE_DEPLOY : WTheme.CENTER_DRONE_RELOAD;
        float s = z.scale();
        HudDraw.begin(g, z.width() / 2F, z.height() / 2F, s);
        HudDraw.ring(g, 0F, 0F, WTheme.CENTER_DRONE_R, WTheme.CENTER_DRONE_SEGMENTS, m.progress, accent, 1F);
        if (m.deploying) blades(g, RingMath.spin(m.time, WTheme.CENTER_BLADE_SPIN_TICKS), accent);
        HudDraw.end(g);
        if (m.marker) marker(g, font, m, accent, s);
    }

    /** Три лопасти по 120° вокруг центра: тень, лопасть, кончик-акцент. Одна пара push/pop. */
    private static void blades(GuiGraphics g, float angle, int accent) {
        int hw = WTheme.CENTER_BLADE_W / 2;
        int tw = WTheme.CENTER_BLADE_TIP_W / 2;
        int y0 = -(WTheme.CENTER_BLADE_INNER + WTheme.CENTER_BLADE_LEN);
        int y1 = -WTheme.CENTER_BLADE_INNER;
        int tip0 = y0 - WTheme.CENTER_BLADE_TIP;
        var pose = g.pose();
        pose.pushPose();
        pose.mulPose(SPIN.rotationZ(angle));
        THIRD.rotationZ((float) (RingMath.TAU / WTheme.CENTER_BLADES));
        for (int i = 0; i < WTheme.CENTER_BLADES; i++) {
            g.fill(-hw - 1, tip0 - 1, hw + 1, y1 + 1, WTheme.CENTER_BLADE_SHADOW);
            g.fill(-hw, y0, hw, y1, WTheme.CENTER_BLADE);
            g.fill(-tw, tip0, tw, y0, accent);
            pose.mulPose(THIRD);
        }
        pose.popPose();
    }

    /**
     * Маркер над дроном: квадрат-плашка с рамкой и ядром цвета канала, справа — плашка «N с».
     * Статичный (без пульсации — на слабых ПК покадровая анимация маркера проседала).
     */
    private void marker(GuiGraphics g, Font font, Model m, int accent, float s) {
        int size = WTheme.CENTER_MARKER;
        int core = WTheme.CENTER_MARKER_CORE;
        HudDraw.begin(g, m.markerX, m.markerY, s);
        int x0 = -size / 2;
        HudDraw.plateBg(g, x0, x0, size, size, 1F);
        HudDraw.frame(g, x0, x0, size, size, UIDraw.fade(accent, WTheme.CENTER_MARKER_FRAME));
        g.fill(-core / 2, -core / 2, -core / 2 + core, -core / 2 + core, accent);
        seconds.of(m.secondsLeft);
        WHud.plate(
                g,
                font,
                size - size / 2 + WTheme.CENTER_MARKER_GAP,
                -WHud.plateHeight(font) / 2,
                null,
                seconds.string(),
                accent,
                1F);
        HudDraw.end(g);
    }
}
