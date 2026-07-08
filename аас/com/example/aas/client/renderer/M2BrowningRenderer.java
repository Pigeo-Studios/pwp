/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  software.bernie.geckolib.model.GeoModel
 *  software.bernie.geckolib.renderer.GeoEntityRenderer
 */
package com.example.aas.client.renderer;

import com.example.aas.client.model.M2BrowningModel;
import com.example.aas.entity.M2BrowningEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class M2BrowningRenderer
extends GeoEntityRenderer<M2BrowningEntity> {
    public M2BrowningRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, (GeoModel)new M2BrowningModel());
        this.f_114477_ = 0.7f;
    }
}

