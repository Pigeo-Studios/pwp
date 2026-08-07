package com.pigeostudios.sbwchunkload.corridor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Растеризация траектории снаряда в список чанков. Никакой логики тикетов
 * или трекинга — чистые вычисления: из точки A в точку B по XZ-плоскости
 * собираются ВСЕ ChunkPos, которые пересекает отрезок (не только первый).
 *
 * Тикет держит весь Y-стек чанка, поэтому 3D-растеризация не нужна —
 * достаточно 2D-линии по XZ.
 */
public final class ChunkRasterizer {

    private ChunkRasterizer() {
    }

    /**
     * Коридор от from до to (включительно) с опциональной шириной.
     * Шаг интерполяции гарантирует попадание в каждый пересекаемый чанк.
     *
     * @param width ширина коридора в чанках (1 = линия, 2 = + один слой соседей)
     */
    public static long[] buildCorridor(Vec3 from, Vec3 to, int width) {
        ChunkPos start = new ChunkPos(BlockPos.containing(from));
        ChunkPos end = new ChunkPos(BlockPos.containing(to));

        Set<Long> chunks = new HashSet<>();
        int dx = end.x - start.x;
        int dz = end.z - start.z;
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        if (steps == 0) {
            addWithWidth(chunks, start.x, start.z, width);
            return toArray(chunks);
        }

        // Интерполяция по доминирующей оси: каждый шаг — пересечение границы
        // минимум одного чанка, так что ни один чанк по пути не пропускается.
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = start.x + (int) Math.round(dx * t);
            int z = start.z + (int) Math.round(dz * t);
            addWithWidth(chunks, x, z, width);
        }
        return toArray(chunks);
    }

    private static void addWithWidth(Set<Long> chunks, int chunkX, int chunkZ, int width) {
        int radius = Math.max(1, width) - 1;
        for (int ox = -radius; ox <= radius; ox++) {
            for (int oz = -radius; oz <= radius; oz++) {
                chunks.add(ChunkPos.asLong(chunkX + ox, chunkZ + oz));
            }
        }
    }

    private static long[] toArray(Set<Long> chunks) {
        long[] result = new long[chunks.size()];
        int i = 0;
        for (Long key : chunks) {
            result[i++] = key;
        }
        return result;
    }
}
