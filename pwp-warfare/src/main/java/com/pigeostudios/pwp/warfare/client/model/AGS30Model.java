package com.pigeostudios.pwp.warfare.client.model;

import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

// Модель автоматического гранатомёта АГС-30 (GeckoLib)
// Управляет анимациями поворота турели и ствола
public class AGS30Model extends GeoModel<AGS30Entity> {
   private static final ResourceLocation WITH_MAG = new ResourceLocation("pwpwarfare", "geo/ags_30.geo.json");
   private static final ResourceLocation NO_MAG = new ResourceLocation("pwpwarfare", "geo/ags_30_no_mag.geo.json");
   private static final ResourceLocation TEXTURE = new ResourceLocation("pwpwarfare", "textures/entity/ags_30.png");
   private static final ResourceLocation ANIMATION = new ResourceLocation("pwpwarfare", "animations/ags_30.animation.json");

   public ResourceLocation getModelResource(AGS30Entity animatable) {
      return animatable.hasMagazine() ? WITH_MAG : NO_MAG;
   }

   public ResourceLocation getTextureResource(AGS30Entity animatable) {
      return TEXTURE;
   }

   public ResourceLocation getAnimationResource(AGS30Entity animatable) {
      return ANIMATION;
   }

   public void setCustomAnimations(AGS30Entity animatable, long instanceId, AnimationState<AGS30Entity> animationState) {
      CoreGeoBone turret = this.getAnimationProcessor().getBone("turret_pivot");
      CoreGeoBone gun = this.getAnimationProcessor().getBone("gun_body");
      if (turret != null) {
         turret.setRotY((float)Math.toRadians(-animatable.getTurretYaw()));
      }

      if (gun != null) {
         gun.setRotX((float)Math.toRadians(animatable.getTurretPitch()));
      }
   }
}
