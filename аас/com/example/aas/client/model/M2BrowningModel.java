/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  software.bernie.geckolib.core.animatable.model.CoreGeoBone
 *  software.bernie.geckolib.core.animation.AnimationState
 *  software.bernie.geckolib.model.GeoModel
 */
package com.example.aas.client.model;

import com.example.aas.entity.M2BrowningEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class M2BrowningModel
extends GeoModel<M2BrowningEntity> {
    private static final ResourceLocation WITH_MAG = new ResourceLocation("aas", "geo/m2_browning.geo.json");
    private static final ResourceLocation NO_MAG = new ResourceLocation("aas", "geo/m2_browning_no_magazin.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("aas", "textures/entity/m2_browning.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation("aas", "animations/m2_browning.animation.json");

    public ResourceLocation getModelResource(M2BrowningEntity animatable) {
        return animatable.hasMagazine() ? WITH_MAG : NO_MAG;
    }

    public ResourceLocation getTextureResource(M2BrowningEntity animatable) {
        return TEXTURE;
    }

    public ResourceLocation getAnimationResource(M2BrowningEntity animatable) {
        return ANIMATION;
    }

    public void setCustomAnimations(M2BrowningEntity animatable, long instanceId, AnimationState<M2BrowningEntity> animationState) {
        CoreGeoBone turret = this.getAnimationProcessor().getBone("turret_pivot");
        CoreGeoBone gun = this.getAnimationProcessor().getBone("gun_body");
        if (turret != null) {
            turret.setRotY((float)Math.toRadians(-animatable.getTurretYaw()));
        }
        if (gun != null) {
            gun.setRotZ((float)Math.toRadians(-animatable.getTurretPitch()));
        }
    }
}

