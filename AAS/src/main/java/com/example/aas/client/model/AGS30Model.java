/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  software.bernie.geckolib.core.animatable.GeoAnimatable
 *  software.bernie.geckolib.core.animatable.model.CoreGeoBone
 *  software.bernie.geckolib.core.animation.AnimationState
 *  software.bernie.geckolib.model.GeoModel
 */
package com.example.aas.client.model;

import com.example.aas.entity.AGS30Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class AGS30Model
extends GeoModel<AGS30Entity> {
    static private final ResourceLocation WITH_MAG = new ResourceLocation("aas", "geo/ags_30.geo.json");
    static private final ResourceLocation NO_MAG = new ResourceLocation("aas", "geo/ags_30_no_mag.geo.json");
    static private final ResourceLocation TEXTURE = new ResourceLocation("aas", "textures/entity/ags_30.png");
    static private final ResourceLocation ANIMATION = new ResourceLocation("aas", "animations/ags_30.animation.json");

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

    public ResourceLocation getAnimationResource(GeoAnimatable geoAnimatable) {
        return this.getAnimationResource((AGS30Entity)geoAnimatable);
    }

    public ResourceLocation getTextureResource(GeoAnimatable geoAnimatable) {
        return this.getTextureResource((AGS30Entity)geoAnimatable);
    }

    public ResourceLocation getModelResource(GeoAnimatable geoAnimatable) {
        return this.getModelResource((AGS30Entity)geoAnimatable);
    }

    public void setCustomAnimations(GeoAnimatable geoAnimatable, long l, AnimationState animationState) {
        this.setCustomAnimations((AGS30Entity)geoAnimatable, l, (AnimationState<AGS30Entity>)animationState);
    }
}

