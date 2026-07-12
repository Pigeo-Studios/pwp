package com.pwp.blastprotection.mixin;

import com.pwp.blastprotection.config.PWPBlastConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockResistanceMixin {
    @Unique
    private static final TagKey<Block> HARD_BLAST_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("pwpblast", "hard_blast")
    );

    @Unique
    private static final TagKey<Block> GRAVEL_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("minecraft", "gravel")
    );

    @Unique
    private static final TagKey<Block> SNOW_TAG = TagKey.create(
        Registries.BLOCK, new ResourceLocation("minecraft", "snow")
    );

    @Inject(method = "getExplosionResistance", at = @At("RETURN"), cancellable = true)
    private void pwpblast$modifyResistance(CallbackInfoReturnable<Float> cir) {
        if (!PWPBlastConfig.ENABLED.get()) return;

        BlockState state = ((Block) (Object) this).defaultBlockState();
        float original = cir.getReturnValue();

        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) ||
            state.is(GRAVEL_TAG) || state.is(SNOW_TAG)) {
            cir.setReturnValue(original * PWPBlastConfig.SOFT_MULTIPLIER.get().floatValue());
            return;
        }

        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.LOGS) ||
            state.is(BlockTags.PLANKS) || state.is(BlockTags.TERRACOTTA) ||
            state.is(BlockTags.ICE) || state.is(HARD_BLAST_TAG)) {
            cir.setReturnValue(original * PWPBlastConfig.HARD_MULTIPLIER.get().floatValue());
        }
    }
}
