package com.pigeostudios.pwp.warfare.client.model;

import com.pigeostudios.pwp.warfare.item.RallyItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

// Модель радиостанции командира отряда (GeckoLib)
// Имеет анимацию раскладывания и сброс трансформации в третьем лице
public class SquadRadioModel extends GeoModel<RallyItem> {
   public ResourceLocation getModelResource(RallyItem animatable) {
      return new ResourceLocation("pwpwarfare", "geo/squad_leader_radio.geo.json");
   }

   public ResourceLocation getTextureResource(RallyItem animatable) {
      return new ResourceLocation("pwpwarfare", "textures/item/squad_leader_radio.png");
   }

   public ResourceLocation getAnimationResource(RallyItem animatable) {
      return new ResourceLocation("pwpwarfare", "animations/squad_leader_radio.animation.json");
   }

   public void setCustomAnimations(RallyItem animatable, long instanceId, AnimationState<RallyItem> animationState) {
      super.setCustomAnimations(animatable, instanceId, animationState);
      if (Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
         CoreGeoBone root = this.getAnimationProcessor().getBone("root");
         if (root != null) {
            root.setPosX(0.0F);
            root.setPosY(0.0F);
            root.setPosZ(0.0F);
            root.setRotX(0.0F);
            root.setRotY(0.0F);
            root.setRotZ(0.0F);
         }

         CoreGeoBone body = this.getAnimationProcessor().getBone("body");
         if (body != null) {
            body.setPosX(0.0F);
            body.setPosY(0.0F);
            body.setPosZ(0.0F);
            body.setRotX(0.0F);
            body.setRotY(0.0F);
            body.setRotZ(0.0F);
         }
      }
   }
}
