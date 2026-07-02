package com.pigeostudios.pwp.medicine.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.medicine.effect.ModEffects;
import com.pigeostudios.pwp.medicine.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="pwp_medicine", value={Dist.CLIENT})
public class ClientHUDHandler {
    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation("pwp_medicine", "textures/gui/medkit_icon.png");
    private static final ResourceLocation BLEEDING_ICON = new ResourceLocation("pwp_medicine", "textures/gui/bleeding_icon.png");
    private static final ResourceLocation BLEEDING_TARGET = new ResourceLocation("pwp_medicine", "textures/gui/bleeding_target.png");
    private static final ResourceLocation SHADER_DESATURATE = new ResourceLocation("shaders/post/desaturate.json");
    private static boolean shaderActive = false;
    private static float bleedAnimProgress = 0.0f;
    private static final float ANIM_SPEED = 0.025f;

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) {
            return;
        }
        float hp = mc.player.getHealth();
        if (hp <= 10.0f) {
            float ticks = (float)mc.player.tickCount + (float)event.getPartialTick();
            float intensity = hp <= 6.0f ? 2.2f : 0.7f;
            float speed = hp <= 6.0f ? 0.12f : 0.07f;
            event.setYaw(event.getYaw() + (float)Math.sin(ticks * speed) * intensity);
            event.setPitch(event.getPitch() + (float)Math.cos(ticks * speed * 0.6f) * intensity);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()) && ClientHUDHandler.getHudTarget(Minecraft.getInstance()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean hasBleeding = mc.player.hasEffect(ModEffects.BLEEDING.get());
        if (hasBleeding && bleedAnimProgress < 1.0f) {
            bleedAnimProgress += 0.025f;
        } else if (!hasBleeding && bleedAnimProgress > 0.0f) {
            bleedAnimProgress -= 0.025f;
        }
        bleedAnimProgress = Math.max(0.0f, Math.min(1.0f, bleedAnimProgress));
        if (bleedAnimProgress > 0.0f) {
            ClientHUDHandler.drawBleedingMenu(event.getGuiGraphics(), mc, bleedAnimProgress);
        }
        ClientHUDHandler.handleGrayscaleShader(mc, mc.player);
        Player target = ClientHUDHandler.getHudTarget(mc);
        float screenX = (float)mc.getWindow().getGuiScaledWidth() / 2.0f;
        float screenY = (float)mc.getWindow().getGuiScaledHeight() / 2.0f;
        if (target != null) {
            ClientHUDHandler.renderHealthHUD(event.getGuiGraphics(), mc, target, target == mc.player, screenX, screenY);
        } else {
            Player healer = ClientHUDHandler.getHealerTargetingMe(mc);
            if (healer != null) {
                float patientY = (float)mc.getWindow().getGuiScaledHeight() - 65.0f;
                ClientHUDHandler.renderHealthHUD(event.getGuiGraphics(), mc, mc.player, false, screenX, patientY);
            }
        }
    }

    private static void drawBleedingMenu(GuiGraphics guiGraphics, Minecraft mc, float progress) {
        MutableComponent text = Component.translatable("gui.pwp_medicine.bleeding");
        int textWidth = mc.font.width((FormattedText)text);
        int width = textWidth + 46;
        int height = 26;
        int x = (int)((float)(-width) + progress * (float)(width + 10));
        int y = 40;
        RenderSystem.enableBlend();
        guiGraphics.fill(x, y, x + width, y + height, -1442840576);
        guiGraphics.fill(x, y, x + 3, y + height, -10496);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(BLEEDING_ICON, x + 8, y + 5, 0.0f, 0.0f, 16, 16, 16, 16);
        guiGraphics.drawString(mc.font, (Component)text, x + 32, y + 9, -1, true);
        RenderSystem.disableBlend();
    }

    private static void renderHealthHUD(GuiGraphics guiGraphics, Minecraft mc, Player target, boolean isSelf, float centerX, float centerY) {
        float healthPercent = Math.max(0.0f, Math.min(1.0f, target.getHealth() / target.getMaxHealth()));
        int redVal = (int)(255.0f * (1.0f - healthPercent));
        int greenVal = (int)(255.0f * healthPercent);
        int hpColor = 0xFF000000 | redVal << 16 | greenVal << 8 | 0;
        ResourceLocation icon = target.hasEffect(ModEffects.BLEEDING.get()) ? BLEEDING_TARGET : ICON_TEXTURE;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)redVal / 255.0f, (float)greenVal / 255.0f, 0.0f, 1.0f);
        guiGraphics.blit(icon, (int)(centerX - 8.0f), (int)(centerY - 8.0f), 0.0f, 0.0f, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (!isSelf) {
            int bgColor = 0x64444444;
            ClientHUDHandler.drawUltraHighQualityArc(guiGraphics, centerX, centerY, 17.0f, 1.0f, bgColor, 1.0f);
            ClientHUDHandler.drawUltraHighQualityArc(guiGraphics, centerX, centerY, 17.0f, healthPercent, hpColor, 1.2f);
        }
    }

    private static Player getHudTarget(Minecraft mc) {
        Player lookTarget;
        Entity entity;
        boolean holdingMedkit;
        if (mc.player == null) {
            return null;
        }
        boolean bl = holdingMedkit = mc.player.getMainHandItem().is(ModItems.MEDKIT.get()) || mc.player.getOffhandItem().is(ModItems.MEDKIT.get());
        if (!holdingMedkit) {
            return null;
        }
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY && (entity = ((EntityHitResult)hit).getEntity()) instanceof Player && ClientHUDHandler.shouldShowHudFor(mc.player, lookTarget = (Player)entity)) {
            return lookTarget;
        }
        if (mc.player.isUsingItem() && mc.player.getUseItem().is(ModItems.MEDKIT.get())) {
            return mc.player;
        }
        return null;
    }

    private static Player getHealerTargetingMe(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        for (Player other : mc.level.players()) {
            HitResult hit;
            if (other == mc.player || !other.isUsingItem() || !other.getUseItem().is(ModItems.MEDKIT.get()) || !(other.distanceTo(mc.player) <= 16.0)) continue;
            Vec3 eyePos = other.getEyePosition();
            Vec3 lookVec = other.getLookAngle();
            Vec3 reachVec = eyePos.add(lookVec.scale(4.0));
            AABB aabb = other.getBoundingBox().inflate(4.0);
            hit = ProjectileUtil.getEntityHitResult(other, eyePos, reachVec, aabb, entity -> entity == mc.player, 4.0);
            if (hit == null || hit.getType() != HitResult.Type.ENTITY || ((EntityHitResult)hit).getEntity() != mc.player) continue;
            return other;
        }
        return null;
    }

    private static boolean shouldShowHudFor(Player localPlayer, Player target) {
        PlayerTeam localTeam = (PlayerTeam)localPlayer.getTeam();
        PlayerTeam targetTeam = (PlayerTeam)target.getTeam();
        return targetTeam == null || localTeam != null && localTeam.isAlliedTo((Team)targetTeam);
    }

    private static void handleGrayscaleShader(Minecraft mc, Player player) {
        float hp = player.getHealth();
        if (hp <= 10.0f) {
            if (!shaderActive) {
                mc.execute(() -> {
                    mc.gameRenderer.loadEffect(SHADER_DESATURATE);
                    shaderActive = true;
                });
            }
        } else if (shaderActive) {
            mc.execute(() -> {
                mc.gameRenderer.shutdownEffect();
                shaderActive = false;
            });
        }
    }

    private static void drawUltraHighQualityArc(GuiGraphics guiGraphics, float x, float y, float radius, float percent, int color, float thickness) {
        if (percent <= 0.0f) {
            return;
        }
        int segments = 360;
        float angleStep = (float)(Math.PI * 2 / (double)segments);
        int i = 0;
        while ((float)i < (float)segments * percent) {
            float angle = (float)i * angleStep - 1.5707964f;
            float px = x + (float)Math.cos(angle) * radius;
            float py = y + (float)Math.sin(angle) * radius;
            guiGraphics.fill((int)(px - thickness / 2.0f), (int)(py - thickness / 2.0f), (int)(px + thickness / 2.0f + 1.0f), (int)(py + thickness / 2.0f + 1.0f), color);
            ++i;
        }
    }
}
