package com.pigeostudios.pwp.limit.client;

import java.util.HashMap;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "pwplimit", value = {Dist.CLIENT})
public class ClientEvents {
    // Хранилище тика последнего спринт-прыжка для каждого игрока (UUID -> tickCount игрока)
    public static final HashMap<UUID, Integer> sprintJumpCooldown = new HashMap();

    @SubscribeEvent
    // onClientTick: принудительное отключение F3, F3+B, F5 в Survival/Adventure
    // START-фаза выполняется ДО ванильной обработки клавиш (handleKeybinds), поэтому здесь
    // проглатываем нажатие F5 и гасим debug-экран — переключение камеры/мерцание не происходит вообще
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        boolean isRestricted = mc.gameMode.getPlayerMode() == GameType.SURVIVAL || mc.gameMode.getPlayerMode() == GameType.ADVENTURE;
        if (!isRestricted) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            // Поглощаем нажатие F5 до handleKeybinds — камера не уходит в третье лицо даже на 1 тик
            // (раньше сброс был только в END, и при удержании F5 камера дёргалась)
            mc.options.keyTogglePerspective.consumeClick();
            // F3: сбрасываем до обработки клавиш, чтобы удержание F3 не мерцало кадром debug-экрана
            mc.options.renderDebug = false;
        } else {
            if (mc.options.renderDebug) {
                mc.options.renderDebug = false;
            }
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
        if (mc.gameMode.getPlayerMode() == GameType.SURVIVAL || mc.gameMode.getPlayerMode() == GameType.ADVENTURE) {
            if (id.equals(VanillaGuiOverlay.DEBUG_TEXT.id()) || id.equals(VanillaGuiOverlay.PLAYER_LIST.id())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    // onInputUpdate: блокировка прыжка при спринте если кулдаун активен
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        if (!LimitsConfigCache.isJumpCooldownEnabled()) {
            return;
        }
        LocalPlayer player = (LocalPlayer) event.getEntity();
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (event.getInput().jumping && player.isSprinting()) {
            Integer lastJumpTick = sprintJumpCooldown.get(player.getUUID());
            int cooldownTicks = (int) (LimitsConfigCache.getJumpCooldownSeconds() * 20.0);
            if (lastJumpTick != null && player.tickCount - lastJumpTick < cooldownTicks) {
                event.getInput().jumping = false;
            }
        }
    }

    @SubscribeEvent
    // onLivingJump: запись тика прыжка для кулдауна
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!LimitsConfigCache.isJumpCooldownEnabled()) {
            return;
        }
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof LocalPlayer player && !player.isCreative() && !player.isSpectator() && player.isSprinting()) {
            sprintJumpCooldown.put(player.getUUID(), player.tickCount);
        }
    }

    @SubscribeEvent
    // onClientLogout: гигиена кэша кд при выходе — иначе при быстром реконнекте игрок получил бы ложный активный кд
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        sprintJumpCooldown.clear();
    }
}
