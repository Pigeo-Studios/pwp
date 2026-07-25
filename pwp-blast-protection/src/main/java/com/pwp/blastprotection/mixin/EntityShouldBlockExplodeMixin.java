package com.pwp.blastprotection.mixin;

import com.pwp.blastprotection.util.PWPBlastLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityShouldBlockExplodeMixin {

    @Inject(
        method = "shouldBlockExplode(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)Z",
        at = @At("RETURN"),
        cancellable = true
    )
    private void pwp$gateExplosion(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (PWPBlastLogic.shouldProtect(state)) {
            cir.setReturnValue(false);
        }
    }
}
