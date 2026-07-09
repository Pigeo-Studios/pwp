/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderGuiOverlayEvent$Post
 *  net.minecraftforge.client.event.RenderGuiOverlayEvent$Pre
 *  net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.example.aas.client;

import com.example.aas.entity.AGS30Entity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="aas", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class AGSScopeOverlay {
    static private final ResourceLocation SCOPE_TEXTURE = new ResourceLocation("aas", "textures/gui/ags_scope.png");

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        AGS30Entity ags;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Entity vehicle = mc.player.getVehicle();
        if (vehicle instanceof AGS30Entity && (ags = (AGS30Entity)vehicle).isAiming() && event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.HELMET.type()) {
            AGS30Entity ags;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            Entity vehicle = mc.player.getVehicle();
            if (vehicle instanceof AGS30Entity && (ags = (AGS30Entity)vehicle).isAiming()) {
                int width = mc.getWindow().getGuiScaledWidth();
                int height = mc.getWindow().getGuiScaledHeight();
                GuiGraphics gui = event.getGuiGraphics();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask((boolean)false);
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.setShaderTexture(0, (ResourceLocation)SCOPE_TEXTURE);
                gui.blit(SCOPE_TEXTURE, 0, 0, width, height, 0.0f, 0.0f, width, height, width, height);
                RenderSystem.depthMask((boolean)true);
                RenderSystem.enableDepthTest();
            }
        }
    }
}

