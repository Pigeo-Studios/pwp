package com.pigeostudios.pwp.warfare.mixin;

import com.pigeostudios.pwp.warfare.client.ClientSafetyState;
import com.pigeostudios.pwp.warfare.client.NotificationFeed;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Предохранитель мейн-зоны для pointblank (FCL-трубы: РПГ-7В2/РПГ-26/M72/AT4/SMAW/Карл Густав).
// pointblank стреляет клиент-авторитетно: tryFire — входная точка попытки выстрела.
// Отмена в HEAD = выстрел не начинается, патрон не тратится. require=0: при обновлении
// pointblank миксин тихо не применится — серверная страховка (EntityJoinLevelEvent)
// всё равно не даст снаряду вылететь.
@OnlyIn(Dist.CLIENT)
@Mixin(GunItem.class)
public abstract class PointblankFireMixin {
   @Inject(
      method = "tryFire(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)Z",
      at = @At("HEAD"),
      cancellable = true,
      remap = false,
      require = 0
   )
   private void pwp$blockFireInMainZone(LocalPlayer player, ItemStack stack, Entity target, CallbackInfoReturnable<Boolean> cir) {
      if (ClientSafetyState.inMainZone) {
         cir.setReturnValue(false);
         NotificationFeed.push("safety.mainzone.blocked", NotificationFeed.SEVERITY_DANGER);
      }
   }
}
