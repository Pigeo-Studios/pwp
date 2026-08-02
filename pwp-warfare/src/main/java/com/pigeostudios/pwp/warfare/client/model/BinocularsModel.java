package com.pigeostudios.pwp.warfare.client.model;

import com.pigeostudios.pwp.warfare.item.BinocularsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// 3D-модель бинокля (GeckoLib) для отображения в руке
public class BinocularsModel extends GeoModel<BinocularsItem> {
   public ResourceLocation getModelResource(BinocularsItem animatable) {
      return new ResourceLocation("pwpwarfare", "geo/binoculars.geo.json");
   }

   public ResourceLocation getTextureResource(BinocularsItem animatable) {
      return new ResourceLocation("pwpwarfare", "textures/item/binoculars.png");
   }

   public ResourceLocation getAnimationResource(BinocularsItem animatable) {
      return new ResourceLocation("pwpwarfare", "animations/binoculars.animation.json");
   }
}
