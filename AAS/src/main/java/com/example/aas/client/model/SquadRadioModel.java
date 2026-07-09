/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.CameraType
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.ResourceLocation
 *  software.bernie.geckolib.core.animatable.GeoAnimatable
 *  software.bernie.geckolib.core.animatable.model.CoreGeoBone
 *  software.bernie.geckolib.core.animation.AnimationState
 *  software.bernie.geckolib.model.GeoModel
 */
package com.example.aas.client.model;

import com.example.aas.item.RallyItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class SquadRadioModel
extends GeoModel<RallyItem> {
    public ResourceLocation getModelResource(RallyItem animatable) {
        return new ResourceLocation("aas", "geo/squad_leader_radio.geo.json");
    }

    public ResourceLocation getTextureResource(RallyItem animatable) {
        return new ResourceLocation("aas", "textures/item/squad_leader_radio.png");
    }

    public ResourceLocation getAnimationResource(RallyItem animatable) {
        return new ResourceLocation("aas", "animations/squad_leader_radio.animation.json");
    }

    public void setCustomAnimations(RallyItem animatable, long instanceId, AnimationState<RallyItem> animationState) {
        super.setCustomAnimations((GeoAnimatable)animatable, instanceId, animationState);
        if (Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            CoreGeoBone body;
            CoreGeoBone root = this.getAnimationProcessor().getBone("root");
            if (root != null) {
                root.setPosX(0.0f);
                root.setPosY(0.0f);
                root.setPosZ(0.0f);
                root.setRotX(0.0f);
                root.setRotY(0.0f);
                root.setRotZ(0.0f);
            }
            if ((body = this.getAnimationProcessor().getBone("body")) != null) {
                body.setPosX(0.0f);
                body.setPosY(0.0f);
                body.setPosZ(0.0f);
                body.setRotX(0.0f);
                body.setRotY(0.0f);
                body.setRotZ(0.0f);
            }
        }
    }

    public ResourceLocation getAnimationResource(GeoAnimatable geoAnimatable) {
        return this.getAnimationResource((RallyItem)geoAnimatable);
    }

    public ResourceLocation getTextureResource(GeoAnimatable geoAnimatable) {
        return this.getTextureResource((RallyItem)geoAnimatable);
    }

    public ResourceLocation getModelResource(GeoAnimatable geoAnimatable) {
        return this.getModelResource((RallyItem)geoAnimatable);
    }

    public void setCustomAnimations(GeoAnimatable geoAnimatable, long l, AnimationState animationState) {
        this.setCustomAnimations((RallyItem)geoAnimatable, l, (AnimationState<RallyItem>)animationState);
    }
}

