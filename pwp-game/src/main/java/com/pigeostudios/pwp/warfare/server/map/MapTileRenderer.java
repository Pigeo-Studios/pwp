package com.pigeostudios.pwp.warfare.server.map;

import com.mojang.serialization.Codec;
import com.pigeostudios.pwp.warfare.map.MapColumnSampler;
import com.pigeostudios.pwp.warfare.map.MapPng;
import com.pigeostudios.pwp.warfare.map.MapShading;
import com.pigeostudios.pwp.warfare.map.MapTileMath;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.ChunkStorage;

/**
 * Рендер тайла уровня 0 (256x256 блоков) из мира сервера БЕЗ генерации и без работы на тике:
 *
 * <ol>
 *   <li>{@link #snapshot} — ГЛАВНЫЙ поток: для загруженных чанков копируются палитры секций
 *       (дёшево, массивы), для незагруженных — асинхронное чтение NBT из region-файла через
 *       IOWorker ({@code chunkMap.read}; несгенерированный чанк = пусто, ничего не создаётся);
 *   <li>{@link Snapshot#render} — ФОНОВЫЙ поток: разбор NBT, колонки, отмывка, PNG.
 * </ol>
 */
final class MapTileRenderer {
    private MapTileRenderer() {}

    /** Формат секций 1.18+ (ниже — старый формат, нужен датафиксер: такие чанки пропускаем). */
    private static final int MIN_DATA_VERSION = 2860;

    private static final Codec<PalettedContainerRO<BlockState>> STATES = PalettedContainer.codecRO(
            Block.BLOCK_STATE_REGISTRY,
            BlockState.CODEC,
            PalettedContainer.Strategy.SECTION_STATES,
            Blocks.AIR.defaultBlockState());

    /** Снимок тайла: секции загруженных чанков + ожидаемые NBT незагруженных. */
    static final class Snapshot {
        final int tx, tz, minY, minSection, sectionCount;
        final PalettedContainerRO<BlockState>[][] loaded;
        final CompletableFuture<Optional<CompoundTag>>[] disk;

        @SuppressWarnings("unchecked")
        Snapshot(int tx, int tz, int minY, int minSection, int sectionCount) {
            this.tx = tx;
            this.tz = tz;
            this.minY = minY;
            this.minSection = minSection;
            this.sectionCount = sectionCount;
            int n = MapTileMath.CHUNKS * MapTileMath.CHUNKS;
            this.loaded = new PalettedContainerRO[n][];
            this.disk = new CompletableFuture[n];
        }

        /** Готов, когда прочитаны все NBT с диска (ошибки чтения = пустые чанки). */
        CompletableFuture<Snapshot> whenRead() {
            CompletableFuture<?>[] all =
                    Arrays.stream(disk).filter(f -> f != null).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(all).thenApply(v -> this);
        }

        /** Фоновый поток: PNG тайла. */
        byte[] render() {
            int w = MapTileMath.TILE;
            int[] rgb = new int[MapTileMath.PIXELS];
            short[] height = new short[MapTileMath.PIXELS];
            byte[] depth = new byte[MapTileMath.PIXELS];
            Arrays.fill(rgb, MapShading.NO_COLOR);
            for (int cz = 0; cz < MapTileMath.CHUNKS; cz++) {
                for (int cx = 0; cx < MapTileMath.CHUNKS; cx++) {
                    int idx = cz * MapTileMath.CHUNKS + cx;
                    PalettedContainerRO<BlockState>[] secs = loaded[idx];
                    if (secs == null && disk[idx] != null) {
                        Optional<CompoundTag> tag = disk[idx].getNow(Optional.empty());
                        secs = tag.map(this::decode).orElse(null);
                    }
                    if (secs == null) continue;
                    MapColumnSampler.sample(secs, minY, rgb, height, depth, w, cx * 16, cz * 16);
                }
            }
            int[] px = new int[MapTileMath.PIXELS];
            MapShading.render(rgb, height, depth, w, w, px);
            return MapPng.encodeIndexed(px, w, w);
        }

        @SuppressWarnings("unchecked")
        private PalettedContainerRO<BlockState>[] decode(CompoundTag tag) {
            if (ChunkStorage.getVersion(tag) < MIN_DATA_VERSION) return null;
            if (!tag.getString("Status").endsWith("full")) return null;
            ListTag list = tag.getList("sections", Tag.TAG_COMPOUND);
            PalettedContainerRO<BlockState>[] out = new PalettedContainerRO[sectionCount];
            boolean any = false;
            for (int i = 0; i < list.size(); i++) {
                CompoundTag sec = list.getCompound(i);
                int idx = sec.getByte("Y") - minSection;
                if (idx < 0 || idx >= sectionCount || !sec.contains("block_states", Tag.TAG_COMPOUND)) continue;
                Optional<PalettedContainerRO<BlockState>> states = STATES.parse(
                                NbtOps.INSTANCE, sec.getCompound("block_states"))
                        .result();
                if (states.isPresent() && states.get().maybeHas(s -> !s.isAir())) {
                    out[idx] = states.get();
                    any = true;
                }
            }
            return any ? out : null;
        }
    }

    /** ГЛАВНЫЙ поток: снимок тайла (копии палитр + запуск чтения NBT). */
    @SuppressWarnings("unchecked")
    static Snapshot snapshot(ServerLevel level, int tx, int tz) {
        Snapshot s = new Snapshot(tx, tz, level.getMinBuildHeight(), level.getMinSection(), level.getSectionsCount());
        WorldBorder border = level.getWorldBorder();
        int baseCx = tx * MapTileMath.CHUNKS, baseCz = tz * MapTileMath.CHUNKS;
        for (int cz = 0; cz < MapTileMath.CHUNKS; cz++) {
            for (int cx = 0; cx < MapTileMath.CHUNKS; cx++) {
                int idx = cz * MapTileMath.CHUNKS + cx;
                ChunkPos pos = new ChunkPos(baseCx + cx, baseCz + cz);
                if (!border.isWithinBounds(pos)) continue;
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk != null) {
                    LevelChunkSection[] sections = chunk.getSections();
                    PalettedContainerRO<BlockState>[] copy = new PalettedContainerRO[s.sectionCount];
                    for (int k = 0; k < sections.length && k < copy.length; k++) {
                        LevelChunkSection sec = sections[k];
                        if (sec != null && !sec.hasOnlyAir())
                            copy[k] = sec.getStates().copy();
                    }
                    s.loaded[idx] = copy;
                } else {
                    // только чтение сохранённого чанка: несгенерированный вернёт Optional.empty()
                    // ошибка чтения (битый region) = пустой чанк
                    s.disk[idx] = level.getChunkSource().chunkMap.read(pos).exceptionally(e -> Optional.empty());
                }
            }
        }
        return s;
    }
}
