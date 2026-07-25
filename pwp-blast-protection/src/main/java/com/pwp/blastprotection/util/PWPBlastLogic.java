package com.pwp.blastprotection.util;

import com.pwp.blastprotection.config.PWPBlastConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ThreadLocalRandom;

public final class PWPBlastLogic {

    public static final TagKey<Block> HARD_BLAST_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("pwpblast", "hard_blast")
    );
    public static final TagKey<Block> GRAVEL_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("minecraft", "gravel")
    );
    public static final TagKey<Block> SNOW_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("minecraft", "snow")
    );

    private PWPBlastLogic() {
    }

    public static boolean shouldProtect(BlockState state) {
        if (!PWPBlastConfig.ENABLED.get()) return false;

        double mult = multiplierFor(state);
        if (mult <= 1.0) return false;

        return ThreadLocalRandom.current().nextDouble() < (mult - 1.0) / mult;
    }

    private static double multiplierFor(BlockState state) {
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.LOGS) ||
            state.is(BlockTags.PLANKS) || state.is(BlockTags.TERRACOTTA) ||
            state.is(BlockTags.ICE) || state.is(HARD_BLAST_TAG)) {
            return PWPBlastConfig.HARD_MULTIPLIER.get();
        }
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) ||
            state.is(GRAVEL_TAG) || state.is(SNOW_TAG)) {
            return PWPBlastConfig.SOFT_MULTIPLIER.get();
        }
        return 1.0;
    }
}
