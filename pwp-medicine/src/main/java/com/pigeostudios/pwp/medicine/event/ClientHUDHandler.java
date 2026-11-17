package com.pigeostudios.pwp.medicine.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.medicine.effect.ModEffects;
import com.pigeostudios.pwp.medicine.item.ModItems;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
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

@Mod.EventBusSubscriber(
        modid = "pwp_medicine",
        value = {Dist.CLIENT})
public class ClientHUDHandler {
    private static final ResourceLocation ICON_TEXTURE =
            new ResourceLocation("pwp_medicine", "textures/gui/medkit_icon.png");
    private static final ResourceLocation BLEEDING_ICON =
            new ResourceLocation("pwp_medicine", "textures/gui/bleeding_icon.png");
    private static final ResourceLocation BLEEDING_TARGET =
            new ResourceLocation("pwp_medicine", "textures/gui/bleeding_target.png");
    private static final ResourceLocation SHADER_DESATURATE = new ResourceLocation("shaders/post/desaturate.json");
    private static boolean shaderActive = false;
    private static float bleedAnimProgress = 0.0f;
    // Выезд баннера кровотечения — в РЕАЛЬНОМ времени: прежние 0.025/кадр при 60 FPS
    // = полный выезд за ~667 мс (раньше при 144 FPS выезжал вдвое быстрее).
    private static final float BLEED_ANIM_MS = 1000.0f / 60.0f / 0.025f;
    private static long bleedAnimLastMs = -1L;
    private static final float ANIM_SPEED = 0.025f;

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) {
            return;
        }
        float hp = mc.player.getHealth();
        if (hp <= 10.0f) {
            float ticks = (float) mc.player.tickCount + (float) event.getPartialTick();
            float intensity = hp <= 6.0f ? 2.2f : 0.7f;
            float speed = hp <= 6.0f ? 0.12f : 0.07f;
            event.setYaw(event.getYaw() + (float) Math.sin(ticks * speed) * intensity);
            event.setPitch(event.getPitch() + (float) Math.cos(ticks * speed * 0.6f) * intensity);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())
                && ClientHUDHandler.getHudTarget(Minecraft.getInstance()) != null) {
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
        long nowMs = Util.getMillis();
        long dtMs = bleedAnimLastMs < 0L ? 0L : Math.max(0L, Math.min(100L, nowMs - bleedAnimLastMs));
        bleedAnimLastMs = nowMs;
        float step = dtMs / BLEED_ANIM_MS;
        if (hasBleeding && bleedAnimProgress < 1.0f) {
            bleedAnimProgress += step;
        } else if (!hasBleeding && bleedAnimProgress > 0.0f) {
            bleedAnimProgress -= step;
        }
        bleedAnimProgress = Math.max(0.0f, Math.min(1.0f, bleedAnimProgress));
        // общий гейт HUD warfare (F1 скрывает); ч/б шейдер нока — вне гейта (эффект картинки)
        boolean hud = WHud.hudVisible();
        if (hud && bleedAnimProgress > 0.0f) {
            ClientHUDHandler.drawBleedingMenu(event.getGuiGraphics(), mc, bleedAnimProgress);
        }
        ClientHUDHandler.handleGrayscaleShader(mc, mc.player);
        if (!hud) return;
        Player target = ClientHUDHandler.getHudTarget(mc);
        float screenX = (float) mc.getWindow().getGuiScaledWidth() / 2.0f;
        float screenY = (float) mc.getWindow().getGuiScaledHeight() / 2.0f;
        if (target != null) {
            ClientHUDHandler.renderHealthHUD(event.getGuiGraphics(), mc, target, target == mc.player, screenX, screenY);
        } else if (patientHealer != null) {
            ClientHUDHandler.renderPatient(event.getGuiGraphics(), mc, patientHealer);
        }
    }

    // ------------------------------------------------------------ пациент (контекстный ярус HUD)
    /** Кто лечит меня в этом кадре (считается один раз в RenderGuiEvent.Pre). */
    private static Player patientHealer;

    private static final WHud.Fmt PATIENT_TITLE = new WHud.Fmt("gui.pwp_medicine.hud.patient");
    private static final WHud.Fmt PATIENT_HP = new WHud.Fmt("gui.pwpwarfare.hud.percent");

    /** Заявка «меня лечат» в контекстный ярус низ-центра (раньше — иконка на h-65 поверх ленты). */
    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        patientHealer = null;
        if (mc.player == null || !WHud.hudVisible()) return;
        if (ClientHUDHandler.getHudTarget(mc) != null) return;
        patientHealer = ClientHUDHandler.getHealerTargetingMe(mc);
        if (patientHealer != null) WHud.contextOffer(WHud.CTX_PATIENT);
    }

    private static void renderPatient(GuiGraphics g, Minecraft mc, Player healer) {
        float hp = Math.max(0.0f, Math.min(1.0f, mc.player.getHealth() / Math.max(1.0f, mc.player.getMaxHealth())));
        PATIENT_HP.of(Math.round(hp * 100.0f));
        WHud.contextPlate(
                g,
                mc.font,
                WHud.CTX_PATIENT,
                WarfareIcon.PLUS,
                PATIENT_TITLE.of(healer.getGameProfile().getName()),
                PATIENT_HP.string(),
                WTheme.HUD_VALUE,
                hp,
                WTheme.HUD_MEDIC,
                1.0f);
    }

    /** Подпись баннера кровотечения: компонент кэширован (не создаётся в кадре). */
    private static final Component BLEEDING_TEXT = Component.translatable("gui.pwp_medicine.bleeding");

    private static void drawBleedingMenu(GuiGraphics guiGraphics, Minecraft mc, float progress) {
        Component text = BLEEDING_TEXT;
        int textWidth = mc.font.width((FormattedText) text);
        int width = textWidth + 46;
        int height = 26;
        int x = (int) ((float) (-width) + progress * (float) (width + 10));
        int y = 40;
        RenderSystem.enableBlend();
        guiGraphics.fill(x, y, x + width, y + height, 0xCC06080A);
        guiGraphics.fill(x, y, x + 3, y + height, 0xFFA53D3D);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(BLEEDING_ICON, x + 8, y + 5, 0.0f, 0.0f, 16, 16, 16, 16);
        guiGraphics.drawString(mc.font, (Component) text, x + 32, y + 9, -1, true);
        RenderSystem.disableBlend();
    }

    private static void renderHealthHUD(
            GuiGraphics guiGraphics, Minecraft mc, Player target, boolean isSelf, float centerX, float centerY) {
        float healthPercent = Math.max(0.0f, Math.min(1.0f, target.getHealth() / target.getMaxHealth()));
        int redVal = (int) (255.0f * (1.0f - healthPercent));
        int greenVal = (int) (255.0f * healthPercent);
        int hpColor = 0xFF000000 | redVal << 16 | greenVal << 8 | 0;
        ResourceLocation icon = target.hasEffect(ModEffects.BLEEDING.get()) ? BLEEDING_TARGET : ICON_TEXTURE;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float) redVal / 255.0f, (float) greenVal / 255.0f, 0.0f, 1.0f);
        guiGraphics.blit(icon, (int) (centerX - 8.0f), (int) (centerY - 8.0f), 0.0f, 0.0f, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (!isSelf) {
            int bgColor = 0x64444444;
            ClientHUDHandler.drawUltraHighQualityArc(guiGraphics, centerX, centerY, 17.0f, 1.0f, bgColor, 1.0f);
            ClientHUDHandler.drawUltraHighQualityArc(
                    guiGraphics, centerX, centerY, 17.0f, healthPercent, hpColor, 1.2f);
        }
    }

    private static Player getHudTarget(Minecraft mc) {
        Player lookTarget;
        Entity entity;
        boolean holdingMedkit;
        if (mc.player == null) {
            return null;
        }
        boolean bl = holdingMedkit = mc.player.getMainHandItem().is(ModItems.MEDKIT.get())
                || mc.player.getOffhandItem().is(ModItems.MEDKIT.get());
        if (!holdingMedkit) {
            return null;
        }
        HitResult hit = mc.hitResult;
        if (hit != null
                && hit.getType() == HitResult.Type.ENTITY
                && (entity = ((EntityHitResult) hit).getEntity()) instanceof Player
                && ClientHUDHandler.shouldShowHudFor(mc.player, lookTarget = (Player) entity)) {
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
            if (other == mc.player
                    || !other.isUsingItem()
                    || !other.getUseItem().is(ModItems.MEDKIT.get())
                    || !(other.distanceTo(mc.player) <= 16.0)) continue;
            Vec3 eyePos = other.getEyePosition();
            Vec3 lookVec = other.getLookAngle();
            Vec3 reachVec = eyePos.add(lookVec.scale(4.0));
            AABB aabb = other.getBoundingBox().inflate(4.0);
            hit = ProjectileUtil.getEntityHitResult(other, eyePos, reachVec, aabb, entity -> entity == mc.player, 4.0);
            if (hit == null
                    || hit.getType() != HitResult.Type.ENTITY
                    || ((EntityHitResult) hit).getEntity() != mc.player) continue;
            return other;
        }
        return null;
    }

    private static boolean shouldShowHudFor(Player localPlayer, Player target) {
        PlayerTeam localTeam = (PlayerTeam) localPlayer.getTeam();
        PlayerTeam targetTeam = (PlayerTeam) target.getTeam();
        return targetTeam == null || localTeam != null && localTeam.isAlliedTo((Team) targetTeam);
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

    private static void drawUltraHighQualityArc(
            GuiGraphics guiGraphics, float x, float y, float radius, float percent, int color, float thickness) {
        if (percent <= 0.0f) {
            return;
        }
        int segments = 360;
        float angleStep = (float) (Math.PI * 2 / (double) segments);
        int i = 0;
        while ((float) i < (float) segments * percent) {
            float angle = (float) i * angleStep - 1.5707964f;
            float px = x + (float) Math.cos(angle) * radius;
            float py = y + (float) Math.sin(angle) * radius;
            guiGraphics.fill(
                    (int) (px - thickness / 2.0f),
                    (int) (py - thickness / 2.0f),
                    (int) (px + thickness / 2.0f + 1.0f),
                    (int) (py + thickness / 2.0f + 1.0f),
                    color);
            ++i;
        }
    }
}
