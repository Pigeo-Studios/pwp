package com.pigeostudios.pwp.warfare.client.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.layout.Box;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits.Kind;
import com.pigeostudios.pwp.warfare.layout.LayoutEdits.Ref;
import com.pigeostudios.pwp.warfare.layout.MapLayout;
import com.pigeostudios.pwp.warfare.layout.Shapes;
import com.pigeostudios.pwp.warfare.layout.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Контуры черновика редактора в мире — ТОЛЬКО у редактора (черновик живёт на его клиенте): мейны (цвет
 * команды), точки (бокс или цилиндр), граница, спавны, слоты техники (со стрелкой yaw), выбранный объект —
 * отдельным цветом, «показать где» — столб. Высота контуров обрезана вокруг игрока (зоны обычно -64..320).
 */
@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
public final class MapEditorWorldRenderer {
    private static final int CIRCLE_SEGMENTS = 48;

    private MapEditorWorldRenderer() {}

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!MapEditorSession.worldMode()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MapLayout l = MapEditorSession.draft();
        Ref sel = MapEditorSession.selected();
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        double py = mc.player.getY();
        double y0 = py - WTheme.EDITOR_WORLD_BELOW;
        double y1 = py + WTheme.EDITOR_WORLD_ABOVE;

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (int i = 0; i < l.mainZones().size(); i++) {
            MapLayout.MainZoneDef z = l.mainZones().get(i);
            int c = isSel(sel, Kind.MAIN, i)
                    ? WTheme.EDITOR_SELECTED
                    : z.teamEnum() == Team.RED ? WTheme.EDITOR_MAIN_RED : WTheme.EDITOR_MAIN_BLUE;
            zone(pose, vc, z.box(), z.shape(), y0, y1, c);
        }
        for (int i = 0; i < l.points().size(); i++) {
            MapLayout.CapturePointDef p = l.points().get(i);
            zone(
                    pose,
                    vc,
                    p.box(),
                    p.shape(),
                    y0,
                    y1,
                    isSel(sel, Kind.POINT, i) ? WTheme.EDITOR_SELECTED : WTheme.EDITOR_POINT);
        }
        MapLayout.Border b = l.border();
        if (b != null && b.complete()) {
            double h = b.half();
            box(
                    pose,
                    vc,
                    new AABB(b.centerX() - h, y0, b.centerZ() - h, b.centerX() + h, y1, b.centerZ() + h),
                    isSel(sel, Kind.BORDER, 0) ? WTheme.EDITOR_SELECTED : WTheme.EDITOR_BORDER);
        }
        for (Team t : Team.values()) {
            MapLayout.TeamSpawn s = l.spawn(t);
            if (s == null) continue;
            int c = isSel(sel, Kind.SPAWN, t.ordinal())
                    ? WTheme.EDITOR_SELECTED
                    : t == Team.RED ? WTheme.EDITOR_MAIN_RED : WTheme.EDITOR_MAIN_BLUE;
            double sy = s.y() != null ? s.y() : py;
            box(pose, vc, new AABB(s.x(), sy, s.z(), s.x() + 1, sy + 2, s.z() + 1), c);
        }
        for (int i = 0; i < l.vehicleSlots().size(); i++) {
            MapLayout.VehicleSlot v = l.vehicleSlots().get(i);
            int c = isSel(sel, Kind.SLOT, i)
                    ? WTheme.EDITOR_SELECTED
                    : v.team() == Team.RED ? WTheme.EDITOR_MAIN_RED : WTheme.EDITOR_MAIN_BLUE;
            double x = v.pos().x() + 0.5;
            double y = v.pos().y();
            double z = v.pos().z() + 0.5;
            box(pose, vc, new AABB(x - 1.5, y, z - 1.5, x + 1.5, y + 2, z + 1.5), c);
            // стрелка направления (yaw Minecraft: 0 = +Z, 90 = -X)
            double rad = Math.toRadians(v.effYaw());
            line(pose, vc, x, y + 1, z, x - Math.sin(rad) * 3, y + 1, z + Math.cos(rad) * 3, c);
        }
        double[] hl = MapEditorSession.highlight();
        if (hl != null) {
            box(
                    pose,
                    vc,
                    new AABB(hl[0] - 0.5, py - 32, hl[1] - 0.5, hl[0] + 0.5, py + 96, hl[1] + 0.5),
                    WTheme.EDITOR_HIGHLIGHT);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static boolean isSel(Ref sel, Kind k, int i) {
        return sel != null && sel.kind() == k && sel.index() == i;
    }

    /** Зона: бокс или вписанный цилиндр (как в игре), по высоте — пересечение с полосой вокруг игрока. */
    private static void zone(PoseStack pose, VertexConsumer vc, Box b, String shape, double y0, double y1, int c) {
        double lo = Math.max(b.minY(), y0);
        double hi = Math.min(b.maxY() + 1, y1);
        if (hi <= lo) {
            lo = y0;
            hi = y0 + 1;
        }
        if (!Shapes.isCylinder(shape)) {
            box(pose, vc, new AABB(b.minX(), lo, b.minZ(), b.maxX() + 1, hi, b.maxZ() + 1), c);
            return;
        }
        double cx = b.centerX() + 0.5;
        double cz = b.centerZ() + 0.5;
        double r = Math.max(0.5, b.radius());
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a0 = Math.PI * 2 * i / CIRCLE_SEGMENTS;
            double a1 = Math.PI * 2 * (i + 1) / CIRCLE_SEGMENTS;
            double x0 = cx + Math.cos(a0) * r, z0 = cz + Math.sin(a0) * r;
            double x1 = cx + Math.cos(a1) * r, z1 = cz + Math.sin(a1) * r;
            line(pose, vc, x0, lo, z0, x1, lo, z1, c);
            line(pose, vc, x0, hi, z0, x1, hi, z1, c);
            if (i % (CIRCLE_SEGMENTS / 8) == 0) line(pose, vc, x0, lo, z0, x0, hi, z0, c);
        }
    }

    private static void box(PoseStack pose, VertexConsumer vc, AABB a, int c) {
        LevelRenderer.renderLineBox(pose, vc, a, r(c), g(c), bl(c), 1.0F);
    }

    private static void line(
            PoseStack pose,
            VertexConsumer vc,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            int c) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float dx = (float) (x1 - x0), dy = (float) (y1 - y0), dz = (float) (z1 - z0);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4F) return;
        dx /= len;
        dy /= len;
        dz /= len;
        vc.vertex(m, (float) x0, (float) y0, (float) z0)
                .color(r(c), g(c), bl(c), 1.0F)
                .normal(n, dx, dy, dz)
                .endVertex();
        vc.vertex(m, (float) x1, (float) y1, (float) z1)
                .color(r(c), g(c), bl(c), 1.0F)
                .normal(n, dx, dy, dz)
                .endVertex();
    }

    private static float r(int c) {
        return ((c >> 16) & 0xFF) / 255F;
    }

    private static float g(int c) {
        return ((c >> 8) & 0xFF) / 255F;
    }

    private static float bl(int c) {
        return (c & 0xFF) / 255F;
    }
}
