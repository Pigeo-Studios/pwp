package com.pigeostudios.pwp.warfare.client.renderer;

import com.pigeostudios.pwp.warfare.client.model.M2BrowningModel;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// Рендер сущности M2 Browning с использованием GeckoLib
// Отображает 3D-модель пулемёта в мире
public class M2BrowningRenderer extends GeoEntityRenderer<M2BrowningEntity> {
   public M2BrowningRenderer(Context renderManager) {
      super(renderManager, new M2BrowningModel());
      this.shadowRadius = 0.7F;
   }
}
