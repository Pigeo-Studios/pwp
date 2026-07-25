package com.pwp.blastprotection.mixin;

import com.pwp.blastprotection.config.PWPBlastConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.pwp.blastprotection.util.PWPBlastLogic.GRAVEL_TAG;
import static com.pwp.blastprotection.util.PWPBlastLogic.HARD_BLAST_TAG;
import static com.pwp.blastprotection.util.PWPBlastLogic.SNOW_TAG;

@Mixin(Block.class)
public abstract class BlockExplosionResistanceMixin {

    @Inject(
        method = "getExplosionResistance()F",
        at = @At("RETURN"),
        cancellable = true
    )
    private void pwp$boostResistance(CallbackInfoReturnable<Float> cir) {
        if (!PWPBlastConfig.ENABLED.get()) return;
        cir.setReturnValue(cir.getReturnValueF() * 2.0f);
    }

    private static float pwp$multiplierFor(BlockState state) {
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.LOGS) ||
            state.is(BlockTags.PLANKS) || state.is(BlockTags.TERRACOTTA) ||
            state.is(BlockTags.ICE) || state.is(HARD_BLAST_TAG)) {
            return PWPBlastConfig.HARD_MULTIPLIER.get().floatValue();
        }
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) ||
            state.is(GRAVEL_TAG) || state.is(SNOW_TAG)) {
            return PWPBlastConfig.SOFT_MULTIPLIER.get().floatValue();
        }
        return 1.0f;
    }
}
