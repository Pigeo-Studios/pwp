package com.pigeostudios.pwp.warfare.mixin;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Страховка к гейту в DroneDeploymentEvents.onEntityInteract: если UFPV
// вызывает подключение монитора НЕ через Forge-событие interact (свой пакет/
// прямой вызов), блокируем на самой точке входа beginRemoteControl.
// Пока дрон устанавливается (WARFARE_DroneDeploying) — монитор не привяжется.
// Чужой мод: remap=false, имя метода = рантайм-имя (не оверрайд vanilla).
@Mixin(ru.lavafrai.uncomplicatedfpv.entity.AddonDroneEntity.class)
public class DroneConnectMixin {

    @Inject(method = "beginRemoteControl", at = @At("HEAD"), cancellable = true, remap = false)
    private void pwpwarfare$blockConnectWhileDeploying(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (WarfareData.of(self).getBoolean(WarfareKeys.DRONE_DEPLOYING)) {
            cir.setReturnValue(false);
        }
    }
}
