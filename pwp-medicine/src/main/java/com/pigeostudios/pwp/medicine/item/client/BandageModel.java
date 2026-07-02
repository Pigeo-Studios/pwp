package com.pigeostudios.pwp.medicine.item.client;

import com.pigeostudios.pwp.medicine.item.BandageItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// Geo-модель бинта: указывает пути к модели, текстуре и анимации.
public class BandageModel
extends GeoModel<BandageItem> {
    public ResourceLocation getModelResource(BandageItem animatable) {
        return new ResourceLocation("pwp_medicine", "geo/bandage.geo.json");
    }

    public ResourceLocation getTextureResource(BandageItem animatable) {
        return new ResourceLocation("pwp_medicine", "textures/item/bandage_3d.png");
    }

    public ResourceLocation getAnimationResource(BandageItem animatable) {
        return new ResourceLocation("pwp_medicine", "animations/bandage.animation.json");
    }
}
