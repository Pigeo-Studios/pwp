package com.pigeostudios.pwp.warfare.client.model;

import com.pigeostudios.pwp.warfare.item.EntrenchingToolItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// 3D-модель сапёрной лопатки (GeckoLib) для отображения в руке
public class EntrenchingToolModel extends GeoModel<EntrenchingToolItem> {
   public ResourceLocation getModelResource(EntrenchingToolItem animatable) {
      return new ResourceLocation("pwpwarfare", "geo/entrenching_tool.geo.json");
   }

   public ResourceLocation getTextureResource(EntrenchingToolItem animatable) {
      return new ResourceLocation("pwpwarfare", "textures/item/entrenching_tool.png");
   }

   public ResourceLocation getAnimationResource(EntrenchingToolItem animatable) {
      return new ResourceLocation("pwpwarfare", "animations/entrenching_tool.animation.json");
   }
}
