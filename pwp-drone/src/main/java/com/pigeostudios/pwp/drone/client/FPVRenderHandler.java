package com.pigeostudios.pwp.drone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "pwpdrone", value = Dist.CLIENT)
public class FPVRenderHandler {

    @SubscribeEvent
    public static void onGuiPost(RenderGuiEvent.Post event) {
        if (!FPVState.isInFPV()) {
            FPVShaderHandler.reset();
            return;
        }

        FPVShaderHandler.loadIfNeeded();

        Entity drone = FPVState.getLinkedDrone();
        float dist;

        if (drone != null && drone.isAlive()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                dist = (float) mc.player.distanceTo(drone);
            } else {
                dist = 999f;
            }
        } else {
            dist = 999f;
        }

        FPVShaderHandler.renderWithDist(event.getPartialTick(), dist);
    }

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!FPVState.isInFPV()) return;

        VanillaGuiOverlay overlay = switch (event.getOverlay().id().getPath()) {
            case "hotbar" -> VanillaGuiOverlay.HOTBAR;
            case "crosshair" -> VanillaGuiOverlay.CROSSHAIR;
            case "player_health" -> VanillaGuiOverlay.PLAYER_HEALTH;
            case "armor_level" -> VanillaGuiOverlay.ARMOR_LEVEL;
            case "food_level" -> VanillaGuiOverlay.FOOD_LEVEL;
            case "air_level" -> VanillaGuiOverlay.AIR_LEVEL;
            case "experience_bar" -> VanillaGuiOverlay.EXPERIENCE_BAR;
            default -> null;
        };

        if (overlay != null) {
            event.setCanceled(true);
        }
    }
}
