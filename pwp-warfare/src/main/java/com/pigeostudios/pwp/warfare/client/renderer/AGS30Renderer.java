package com.pigeostudios.pwp.warfare.client.renderer;

import com.pigeostudios.pwp.warfare.client.model.AGS30Model;
import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// Рендер сущности АГС-30 с использованием GeckoLib
// Отображает 3D-модель гранатомёта в мире
public class AGS30Renderer extends GeoEntityRenderer<AGS30Entity> {
   public AGS30Renderer(Context renderManager) {
      super(renderManager, new AGS30Model());
      this.shadowRadius = 0.7F;
   }
}
