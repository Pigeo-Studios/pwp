/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  software.bernie.geckolib.model.GeoModel
 */
package com.example.aas.client.model;

import com.example.aas.item.EntrenchingToolItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntrenchingToolModel
extends GeoModel<EntrenchingToolItem> {
    public ResourceLocation getModelResource(EntrenchingToolItem animatable) {
        return new ResourceLocation("aas", "geo/entrenching_tool.geo.json");
    }

    public ResourceLocation getTextureResource(EntrenchingToolItem animatable) {
        return new ResourceLocation("aas", "textures/item/entrenching_tool.png");
    }

    public ResourceLocation getAnimationResource(EntrenchingToolItem animatable) {
        return new ResourceLocation("aas", "animations/entrenching_tool.animation.json");
    }
}

