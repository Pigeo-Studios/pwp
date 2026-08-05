package com.pigeostudios.pwp.warfare.mixin;

import com.atsuishio.superbwarfare.client.overlay.AmmoBarOverlay;
import com.atsuishio.superbwarfare.client.overlay.RenderContext;
import com.pigeostudios.pwp.warfare.client.ClientSafetyState;
import com.pigeostudios.pwp.warfare.client.NotificationFeed;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Скрытие оружейного HUD SBW (иконка+патроны, низ-центр) при держании ручного
// SBW-оружия (Javelin/Игла), когда он перекрывает ленту уведомлений: в мейн-зоне
// стабильно (стрельба заблокирована), вне зоны — пока в ленте есть записи.
// require=0: при обновлении SBW не крашит клиент.
@OnlyIn(Dist.CLIENT)
@Mixin(AmmoBarOverlay.class)
public abstract class SbwAmmoBarMixin {
   @Inject(
      method = "render(Lcom/atsuishio/superbwarfare/client/overlay/RenderContext;)V",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 0
   )
   private void pwp$blockAmmoBarInMainZone(RenderContext context, CallbackInfo ci) {
      if (ClientSafetyState.inMainZone || NotificationFeed.hasVisibleEntries()) {
         ci.cancel();
      }
   }
}
