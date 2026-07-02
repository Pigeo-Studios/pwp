package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Post;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
// Оверлей прицела для автоматического гранатомёта АГС-30
// Отображает текстуру прицела и скрывает стандартный перекрестие
public class AGSScopeOverlay {
   private static final ResourceLocation SCOPE_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/ags_scope.png");

   @SubscribeEvent
   public static void onRenderOverlay(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (mc.player.getVehicle() instanceof AGS30Entity ags && ags.isAiming() && event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onRenderOverlayPost(Post event) {
      if (event.getOverlay() == VanillaGuiOverlay.HELMET.type()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return;
         }

         if (mc.player.getVehicle() instanceof AGS30Entity ags && ags.isAiming()) {
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            GuiGraphics gui = event.getGuiGraphics();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, SCOPE_TEXTURE);
            gui.blit(SCOPE_TEXTURE, 0, 0, width, height, 0.0F, 0.0F, width, height, width, height);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
         }
      }
   }
}
