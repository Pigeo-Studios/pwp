/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  software.bernie.geckolib.model.GeoModel
 *  software.bernie.geckolib.renderer.GeoEntityRenderer
 */
package com.example.aas.client.renderer;

import com.example.aas.client.model.AGS30Model;
import com.example.aas.entity.AGS30Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AGS30Renderer
extends GeoEntityRenderer<AGS30Entity> {
    public AGS30Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, (GeoModel)new AGS30Model());
        this.shadowRadius = 0.7f;
    }
}

