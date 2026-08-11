package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Пиксельный захват реального рендера в bounding box. Вместо измерения дерева
 * Bedrock-кубов (он не учитывает ротации узлов и BEWLR-цепочку и даёт 14 юнитов,
 * а на экране 25.92px — из-за этого прежний фит переполнялся) перехватываем
 * ВСЕ вершины, которые модель реально отправила в буфер, и считаем min/max x/y.
 *
 * <p>Подменяется вместо {@link MultiBufferSource.BufferSource} (superclass — protected
 * конструктор, поэтому наш сабкласс создаётся без проблем; все методы — no-op, так
 * как рендер model.render() пишет в переданный {@link VertexConsumer} напрямую и
 * flush/endBatch не требуются).
 *
 * <p>Для TACZ буфер берётся из глобального {@code RenderBuffers.bufferSource()} —
 * его перехватывает минимиксин {@code RenderBuffersCaptureMixin} через
 * {@link #ACTIVE}. Для прочих (SBW/FCL/vanilla) наш каптчер передаётся напрямую в
 * {@code ItemRenderer.renderStatic}.
 */
@OnlyIn(Dist.CLIENT)
public final class WeaponBoundingBox {

    /** Активный каптчер (thread-local на всякий случай — рендер однопоточный). */
    private static final ThreadLocal<Capture> ACTIVE = new ThreadLocal<>();

    private WeaponBoundingBox() {}

    /** Создать и активировать каптчер (для перехвата глобального буфера TACZ). */
    public static Capture begin() {
        Capture c = new Capture();
        ACTIVE.set(c);
        return c;
    }

    /** Снять активный каптчер. */
    public static void end() {
        ACTIVE.remove();
    }

    /** Для миксина: текущий активный каптчер или null. */
    public static Capture active() {
        return ACTIVE.get();
    }

    public static final class Capture extends MultiBufferSource.BufferSource implements VertexConsumer {
        private float minX = Float.MAX_VALUE;
        private float minY = Float.MAX_VALUE;
        private float maxX = -Float.MAX_VALUE;
        private float maxY = -Float.MAX_VALUE;
        private boolean touched = false;

        private Capture() {
            // База — null-буфер + пустая карта фиксированных буферов: ни один из них в
            // рендере не используется (VertexConsumer-путь пишет напрямую в this).
            super(null, new HashMap<RenderType, BufferBuilder>());
        }

        public boolean isEmpty() {
            return !touched;
        }

        /** Ширина захваченного бокса в пикселях при внешнем масштабе 1. */
        public float width() {
            return maxX - minX;
        }

        /** Высота захваченного бокса в пикселях при внешнем масштабе 1. */
        public float height() {
            return maxY - minY;
        }

        /** Центр бокса по X (для центровки на экране). */
        public float centerX() {
            return (minX + maxX) * 0.5f;
        }

        /** Центр бокса по Y (для центровки на экране). */
        public float centerY() {
            return (minY + maxY) * 0.5f;
        }

        // ───── MultiBufferSource: всё no-op, вершины идут только в VertexConsumer-путь ─────

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return this;
        }

        @Override
        public void endBatch() {}

        @Override
        public void endBatch(RenderType renderType) {}

        @Override
        public void endLastBatch() {}

        // ───── VertexConsumer: считаем минимумы/максимумы отрендеренных пикселей ─────

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            accept((float) x, (float) y);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) { return this; }

        @Override
        public VertexConsumer uv(float u, float v) { return this; }

        @Override
        public VertexConsumer overlayCoords(int u, int v) { return this; }

        @Override
        public VertexConsumer uv2(int u, int v) { return this; }

        @Override
        public VertexConsumer normal(float x, float y, float z) { return this; }

        @Override
        public void endVertex() {}

        @Override
        public void defaultColor(int r, int g, int b, int a) {}

        @Override
        public void unsetDefaultColor() {}

        private void accept(float x, float y) {
            touched = true;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
    }
}
