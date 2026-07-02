package com.pigeostudios.pwp.warfare.mixin;

import com.pigeostudios.pwp.warfare.client.ClientData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
// Миксин: принудительное отображение частей модели игрока
// Включает все части тела для корректного отображения скинов фракций
public class PlayerModelMixin {
    @Inject(method = "setModelProperties", at = @At("RETURN"), remap = false)
   private void pwpwarfare$forceModelParts(AbstractClientPlayer player, CallbackInfo ci) {
      PlayerRenderer renderer = (PlayerRenderer)(Object)this;
      PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)renderer.getModel();
      Team team = player.getTeam();
      if (team != null) {
         String faction = "none";
         if (team.getName().equalsIgnoreCase("Blue")) {
            faction = ClientData.BLUE_FACTION;
         } else if (team.getName().equalsIgnoreCase("Red")) {
            faction = ClientData.RED_FACTION;
         }

         if (faction != null && !faction.equals("none")) {
            model.head.visible = true;
            model.body.visible = true;
            model.leftArm.visible = true;
            model.rightArm.visible = true;
            model.leftLeg.visible = true;
            model.rightLeg.visible = true;
            model.hat.visible = true;
            model.jacket.visible = true;
            model.leftSleeve.visible = true;
            model.rightSleeve.visible = true;
            model.leftPants.visible = true;
            model.rightPants.visible = true;
         }
      }
   }
}
