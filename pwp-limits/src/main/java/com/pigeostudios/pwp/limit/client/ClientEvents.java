package com.pigeostudios.pwp.limit.client;

import com.pigeostudios.pwp.limit.ModConfig;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="pwplimit", value={Dist.CLIENT})
public class ClientEvents {
    // Хранилище времени последнего прыжка для каждого игрока (UUID -> время в мс)
    public static final HashMap<UUID, Long> sprintJumpCooldown = new HashMap();

    @SubscribeEvent
    // onClientTick: принудительное отключение F3, F3+B, F5 в Survival/Adventure
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        boolean isRestricted = mc.gameMode.getPlayerMode() == GameType.SURVIVAL || mc.gameMode.getPlayerMode() == GameType.ADVENTURE;
        if (isRestricted) {
            mc.options.renderDebug = false;
            if (mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
                mc.getEntityRenderDispatcher().setRenderHitBoxes(false);
            }
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation id = event.getOverlay().id();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())
                || id.equals(VanillaGuiOverlay.HOTBAR.id())
                || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())
                || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) {
            event.setCanceled(true);
            return;
        }
        if (id.equals(VanillaGuiOverlay.CROSSHAIR.id())
                && !mc.player.isSpectator()) {
            event.setCanceled(true);
            return;
        }
        if ((mc.gameMode.getPlayerMode() == GameType.SURVIVAL || mc.gameMode.getPlayerMode() == GameType.ADVENTURE)
                && id.equals(VanillaGuiOverlay.DEBUG_TEXT.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    // onInputUpdate: блокировка прыжка при спринте если кулдаун активен
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        if (!((Boolean)ModConfig.ENABLE_JUMP_COOLDOWN.get()).booleanValue()) {
            return;
        }
        LocalPlayer player = (LocalPlayer)event.getEntity();
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (event.getInput().jumping && player.isSprinting()) {
            Long lastJump = sprintJumpCooldown.get(player.getUUID());
            long cooldownMs = (long)((Double)ModConfig.JUMP_COOLDOWN_SECONDS.get() * 1000.0);
            if (lastJump != null && System.currentTimeMillis() - lastJump < cooldownMs) {
                event.getInput().jumping = false;
            }
        }
    }

    @SubscribeEvent
    // onLivingJump: запись времени прыжка для кулдауна
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LocalPlayer player;
        if (!((Boolean)ModConfig.ENABLE_JUMP_COOLDOWN.get()).booleanValue()) {
            return;
        }
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof LocalPlayer && !(player = (LocalPlayer)livingEntity).isCreative() && !player.isSpectator() && player.isSprinting()) {
            sprintJumpCooldown.put(player.getUUID(), System.currentTimeMillis());
        }
    }
}
