package com.pigeostudios.pwp.medicine.event;

import com.pigeostudios.pwp.medicine.effect.ModEffects;
import com.pigeostudios.pwp.medicine.item.ModItems;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
    /** Место баннера кровотечения в левом стеке HUD на этот кадр (занято в RenderGuiEvent.Pre). */
    private static int bleedY = WHud.SIDE_NONE;

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
        // общий гейт HUD warfare (F1 скрывает); ч/б шейдер нока — вне гейта (эффект картинки)
        boolean hud = WHud.hudVisible();
        if (hud && bleedY != WHud.SIDE_NONE) {
            ClientHUDHandler.drawBleedingMenu(event.getGuiGraphics(), bleedAnimProgress);
        }
        ClientHUDHandler.handleGrayscaleShader(mc, mc.player);
        if (!hud) return;
        Player target = ClientHUDHandler.getHudTarget(mc);
        if (target != null) {
            // центр занят каналом дрона (высший приоритет) — кольца не накладываются
            if (!WHud.centerBusy()) {
                ClientHUDHandler.renderHealthHUD(event.getGuiGraphics(), mc, target, target == mc.player);
            }
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
        bleedY = WHud.SIDE_NONE;
        if (mc.player == null) return;
        updateBleedAnim(mc.player.hasEffect(ModEffects.BLEEDING.get()));
        if (!WHud.hudVisible()) return;
        // баннер кровотечения — первым в левом стеке HUD (выше голосований и голоса; этап 4)
        if (bleedAnimProgress > 0.0f) bleedY = WHud.leftClaim(WHud.font());
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

    /** Выезд/уезд баннера в реальном времени (раз в кадр, в RenderGuiEvent.Pre). */
    private static void updateBleedAnim(boolean hasBleeding) {
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
    }

    /** Баннер кровотечения — плашка-тревога левого стека HUD (кит WHud, место занято в Pre). */
    private static void drawBleedingMenu(GuiGraphics guiGraphics, float progress) {
        long period = WTheme.SIDE_ALERT_PULSE_MS;
        double ph = (Util.getMillis() % period) / (double) period * Math.PI * 2.0;
        float pulse = 0.5f + 0.5f * (float) Math.sin(ph);
        WHud.sideAlert(
                guiGraphics,
                WHud.font(),
                bleedY,
                BLEEDING_ICON,
                WarfareIcon.PLUS,
                BLEEDING_TEXT,
                WTheme.HUD_DANGER,
                progress,
                pulse);
    }

    /** Процент HP цели на плашке справа от кольца (строка пересобирается только при смене). */
    private static final WHud.Fmt TARGET_HP = new WHud.Fmt("gui.pwpwarfare.hud.percent");

    /**
     * Кольцо HP цели у прицела (этап 5, кит HUD): иконка аптечки/кровотечения в центре цветом HP,
     * сегментное кольцо (сегменты как у своей полосы HP) и плашка «N%» справа. Своё лечение — только
     * иконка, как раньше. Центр занят каналом дрона — не рисуется (вызывающий проверяет).
     */
    private static void renderHealthHUD(GuiGraphics g, Minecraft mc, Player target, boolean isSelf) {
        float hp = Math.max(0.0f, Math.min(1.0f, target.getHealth() / Math.max(1.0f, target.getMaxHealth())));
        int color = hp < WTheme.CENTER_HP_LOW_FRACTION ? WTheme.CENTER_HP_LOW : WTheme.CENTER_HP_FILL;
        ResourceLocation icon = target.hasEffect(ModEffects.BLEEDING.get()) ? BLEEDING_TARGET : ICON_TEXTURE;
        WHud.crosshairIcon(g, icon, WTheme.CENTER_HP_ICON, color);
        if (isSelf) return;
        WHud.crosshairRing(g, WTheme.CENTER_HP_R, WTheme.CENTER_HP_SEGMENTS, hp, color, 1.0f);
        TARGET_HP.of(Math.round(hp * 100.0f));
        WHud.crosshairValue(g, WHud.font(), WTheme.CENTER_HP_R, TARGET_HP.string(), color, 1.0f);
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
}
