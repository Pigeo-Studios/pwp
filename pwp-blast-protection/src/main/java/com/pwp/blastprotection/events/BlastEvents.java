package com.pwp.blastprotection.events;

import com.pwp.blastprotection.config.PWPBlastConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "pwpblast", bus = Bus.FORGE)
public class BlastEvents {
    private static final TagKey<Block> HARD_BLAST_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("pwpblast", "hard_blast")
    );
    private static final TagKey<Block> GRAVEL_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("minecraft", "gravel")
    );
    private static final TagKey<Block> SNOW_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("minecraft", "snow")
    );

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!PWPBlastConfig.ENABLED.get()) return;

        event.getAffectedBlocks().removeIf(pos ->
            isProtected(event.getLevel().getBlockState(pos))
        );
    }

    private static boolean isProtected(BlockState state) {
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) ||
            state.is(GRAVEL_TAG) || state.is(SNOW_TAG)) {
            return true;
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.LOGS) ||
            state.is(BlockTags.PLANKS) || state.is(BlockTags.TERRACOTTA) ||
            state.is(BlockTags.ICE) || state.is(HARD_BLAST_TAG)) {
            return true;
        }
        return false;
    }
}
