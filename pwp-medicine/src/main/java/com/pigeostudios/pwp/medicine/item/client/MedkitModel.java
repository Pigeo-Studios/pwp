package com.pigeostudios.pwp.medicine.item.client;

import com.pigeostudios.pwp.medicine.item.MedkitItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// Geo-модель аптечки: указывает пути к модели, текстуре и анимации.
public class MedkitModel
extends GeoModel<MedkitItem> {
    public ResourceLocation getModelResource(MedkitItem animatable) {
        return new ResourceLocation("pwp_medicine", "geo/medkit.geo.json");
    }

    public ResourceLocation getTextureResource(MedkitItem animatable) {
        return new ResourceLocation("pwp_medicine", "textures/item/medkit_3d.png");
    }

    public ResourceLocation getAnimationResource(MedkitItem animatable) {
        return new ResourceLocation("pwp_medicine", "animations/medkit.animation.json");
    }
}
