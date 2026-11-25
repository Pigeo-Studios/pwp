package com.pigeostudios.pwp.warfare.map;

import com.pwp.api.util.LogOnce;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Колонки чанка для карты: верхний блок с цветом MapColor (блоки с {@link MapColor#NONE} —
 * стекло, трава-растения и т.п. — пропускаются, как у ванильной карты), его высота и глубина воды.
 *
 * <p>Работает по КОПИЯМ секций ({@link PalettedContainerRO}), поэтому сервер вызывает его на
 * фоновом потоке (снапшот/диск), а клиент — на своём потоке по загруженным чанкам. Цвет блока
 * берётся без мира ({@link EmptyBlockGetter}) — безопасно вне главного потока.
 */
public final class MapColumnSampler {
    private static final Logger LOG = LoggerFactory.getLogger(MapColumnSampler.class);
    /** Кэш цвета по состоянию блока: RGB или {@link MapShading#NO_COLOR} для NONE. */
    private static final Map<BlockState, Integer> COLORS = new ConcurrentHashMap<>();

    private MapColumnSampler() {}

    /**
     * Заполняет колонки чанка в массивы тайла шириной {@code w} начиная с ({@code ox}, {@code oz}).
     * {@code sections[i]} — секция с минимальным Y {@code minY + 16*i} (null — пустая/неизвестна).
     * Колонка без единого видимого блока получает {@link MapShading#NO_COLOR}.
     */
    public static void sample(
            PalettedContainerRO<BlockState>[] sections,
            int minY,
            int[] rgb,
            short[] height,
            byte[] depth,
            int w,
            int ox,
            int oz) {
        int top = sections.length - 1;
        while (top >= 0 && sections[top] == null) top--;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = (oz + z) * w + ox + x;
                rgb[i] = MapShading.NO_COLOR;
                height[i] = 0;
                depth[i] = 0;
                column(sections, top, minY, x, z, rgb, height, depth, i);
            }
        }
    }

    private static void column(
            PalettedContainerRO<BlockState>[] sections,
            int top,
            int minY,
            int x,
            int z,
            int[] rgb,
            short[] height,
            byte[] depth,
            int i) {
        int waterTop = Integer.MIN_VALUE;
        for (int si = top; si >= 0; si--) {
            PalettedContainerRO<BlockState> sec = sections[si];
            if (sec == null) {
                if (waterTop != Integer.MIN_VALUE) {
                    finishWater(waterTop, minY + si * 16 + 15, rgb, height, depth, i);
                    return;
                }
                continue;
            }
            for (int ly = 15; ly >= 0; ly--) {
                int y = minY + si * 16 + ly;
                BlockState st = sec.get(x, ly, z);
                if (waterTop != Integer.MIN_VALUE) {
                    if (st.getFluidState().is(FluidTags.WATER)) continue;
                    finishWater(waterTop, y, rgb, height, depth, i);
                    return;
                }
                if (st.isAir()) continue;
                if (st.getFluidState().is(FluidTags.WATER)) {
                    waterTop = y;
                    continue;
                }
                int c = color(st);
                if (c == MapShading.NO_COLOR) continue;
                rgb[i] = c;
                height[i] = (short) y;
                return;
            }
        }
        if (waterTop != Integer.MIN_VALUE) finishWater(waterTop, minY - 1, rgb, height, depth, i);
    }

    private static void finishWater(int waterTop, int bottomY, int[] rgb, short[] height, byte[] depth, int i) {
        rgb[i] = MapColor.WATER.col;
        height[i] = (short) waterTop;
        depth[i] = (byte) Math.max(1, Math.min(255, waterTop - bottomY));
    }

    static int color(BlockState st) {
        Integer c = COLORS.get(st);
        if (c != null) return c;
        int v;
        try {
            MapColor mc = st.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            v = mc == null || mc == MapColor.NONE ? MapShading.NO_COLOR : mc.col;
        } catch (RuntimeException e) {
            // чужой блок, которому нужен мир для цвета — считаем невидимым
            LogOnce.warn(LOG, "MapColumnSampler.color", "Цвет карты блока недоступен без мира: " + st, e);
            v = MapShading.NO_COLOR;
        }
        COLORS.put(st, v);
        return v;
    }
}
