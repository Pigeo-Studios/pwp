/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.DeathScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.client.gui.screens.inventory.InventoryScreen
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.ClientChatReceivedEvent
 *  net.minecraftforge.client.event.ComputeFovModifierEvent
 *  net.minecraftforge.client.event.InputEvent$Key
 *  net.minecraftforge.client.event.InputEvent$MouseButton$Pre
 *  net.minecraftforge.client.event.InputEvent$MouseScrollingEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.client.event.ScreenEvent$Opening
 *  net.minecraftforge.client.event.ViewportEvent$ComputeCameraAngles
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.EventPriority
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  org.joml.Matrix4f
 *  org.lwjgl.glfw.GLFW
 */
package com.example.aas.client;

import com.example.aas.client.AASDeathScreen;
import com.example.aas.client.ClientData;
import com.example.aas.client.ModKeyBindings;
import com.example.aas.client.RecoilHandler;
import com.example.aas.client.gui.DownedScreen;
import com.example.aas.client.gui.MapMarkerGridScreen;
import com.example.aas.client.gui.PlayerKitSelectScreen;
import com.example.aas.client.gui.SquadSelectionScreen;
import com.example.aas.client.gui.TeamSelectionScreen;
import com.example.aas.config.AASConfig;
import com.example.aas.entity.AGS30Entity;
import com.example.aas.entity.M2BrowningEntity;
import com.example.aas.item.SupplyTruckMarkerItem;
import com.example.aas.item.VehicleMarkerItem;
import com.example.aas.network.PacketApplyMarker;
import com.example.aas.network.PacketCMDVote;
import com.example.aas.network.PacketConfirmArtStrike;
import com.example.aas.network.PacketDebugFill;
import com.example.aas.network.PacketDropCrate;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketPlacePing;
import com.example.aas.network.PacketToggleAim;
import com.example.aas.network.PacketVehicleShoot;
import com.example.aas.network.PacketVoteAction;
import com.example.aas.world.AASWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="aas", value={Dist.CLIENT})
public class ClientEvents {
    static private final ResourceLocation PING_TEXTURE = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye.png");
    static private final ResourceLocation MOVE_TEXTURE = new ResourceLocation("aas", "textures/gui/map_icons/marker_move.png");
    static private final ResourceLocation MOVE_TEX_BRAVO = new ResourceLocation("aas", "textures/gui/map_icons/marker_move_bravo.png");
    static private final ResourceLocation PING_TEX_BRAVO = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye_bravo.png");
    static private final ResourceLocation MOVE_TEX_CHARLIE = new ResourceLocation("aas", "textures/gui/map_icons/marker_move_charlie.png");
    static private final ResourceLocation PING_TEX_CHARLIE = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye_charlie.png");
    static private boolean lastSquadState = false;
    static private boolean lastCommandState = false;
    static private float downedCameraProgress = 0.0f;
    static private float initialPitchOnFall = 0.0f;
    static private boolean wasDownedLastFrame = false;

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        ClientData.addChatMessage(event.getMessage());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!(!mc.player.isAlive() || mc.player.getPersistentData().getBoolean("AAS_IsDowned") || mc.screen instanceof AASDeathScreen || ClientData.globalDeathTimestamp == 0L || mc.screen instanceof PlayerKitSelectScreen || mc.screen instanceof MapMarkerGridScreen)) {
            ClientData.globalDeathTimestamp = 0L;
            ClientData.deathFadePlayed = false;
        }
        if (ClientData.voteActive && ClientData.voteTimer > 0 && mc.level.getGameTime() % 20L == 0L) {
            --ClientData.voteTimer;
        }
        RecoilHandler.clientTick();
        Entity vehicle = mc.player.getVehicle();
        if (vehicle instanceof M2BrowningEntity || vehicle instanceof AGS30Entity) {
            boolean isLeftBtnDown;
            long windowId = mc.getWindow().getWindow();
            boolean bl = isLeftBtnDown = GLFW.glfwGetMouseButton((long)windowId, 0) == 1;
            if (isLeftBtnDown && mc.screen == null) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketVehicleShoot());
                while (mc.options.keyAttack.consumeClick()) {
                }
            }
        }
        if (mc.player.getPersistentData().getBoolean("AAS_IsDowned") && mc.player.isAlive()) {
            mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
            if (mc.screen == null) {
                mc.setScreen((Screen)new DownedScreen());
            }
        } else if (mc.player.getPersistentData().contains("AAS_DownedYaw")) {
            mc.player.getPersistentData().remove("AAS_DownedYaw");
            mc.player.getPersistentData().remove("AAS_DownedPitch");
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        double delta;
        Minecraft mc = Minecraft.getInstance();
        if (ClientData.isMapOpen && mc.screen == null && (delta = event.getScrollDelta()) != 0.0) {
            ClientData.zoomMap(delta);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        BlockPos mPos;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        String myName = mc.player.getScoreboardName();
        AASWorldData.Squad mySquad = null;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            mySquad = s;
            break;
        }
        if (mySquad == null) {
            return;
        }
        boolean isSL = mySquad.leader.equals(myName);
        boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
        boolean isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        long gameTime = mc.level.getGameTime();
        float blinkAlpha = 0.45f + (float)Math.sin((float)gameTime * 0.4f) * 0.25f;
        if (mySquad.pingPos != null && gameTime < mySquad.pingExpiry) {
            ClientEvents.render3DMarker(poseStack, cameraPos, mySquad.pingPos, PING_TEXTURE, mc, blinkAlpha, 0.5);
        }
        if (mySquad.bravoPingPos != null && gameTime < mySquad.bravoPingExpiry && (isSL || isBravo)) {
            ClientEvents.render3DMarker(poseStack, cameraPos, mySquad.bravoPingPos, PING_TEX_BRAVO, mc, blinkAlpha, 0.5);
        }
        if (mySquad.charliePingPos != null && gameTime < mySquad.charliePingExpiry && (isSL || isCharlie)) {
            ClientEvents.render3DMarker(poseStack, cameraPos, mySquad.charliePingPos, PING_TEX_CHARLIE, mc, blinkAlpha, 0.5);
        }
        if (mySquad.marker != null && mySquad.marker.type == 0 && gameTime < mySquad.marker.expiryTick) {
            mPos = new BlockPos(mySquad.marker.x, mySquad.marker.y, mySquad.marker.z);
            ClientEvents.render3DMarker(poseStack, cameraPos, mPos, MOVE_TEXTURE, mc, 1.0f, 0.5);
        }
        if (mySquad.bravoMarker != null && mySquad.bravoMarker.type == 0 && gameTime < mySquad.bravoMarker.expiryTick) {
            mPos = new BlockPos(mySquad.bravoMarker.x, mySquad.bravoMarker.y, mySquad.bravoMarker.z);
            ClientEvents.render3DMarker(poseStack, cameraPos, mPos, MOVE_TEX_BRAVO, mc, 1.0f, 0.5);
        }
        if (mySquad.charlieMarker != null && mySquad.charlieMarker.type == 0 && gameTime < mySquad.charlieMarker.expiryTick) {
            mPos = new BlockPos(mySquad.charlieMarker.x, mySquad.charlieMarker.y, mySquad.charlieMarker.z);
            ClientEvents.render3DMarker(poseStack, cameraPos, mPos, MOVE_TEX_CHARLIE, mc, 1.0f, 0.5);
        }
    }

    private static void tryPlacePingLogic(Minecraft mc) {
        String myName = mc.player.getScoreboardName();
        boolean canPing = false;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(myName) && !s.bravoLeader.equals(myName) && !s.charlieLeader.equals(myName)) continue;
            canPing = true;
            break;
        }
        if (canPing || mc.player.isCreative()) {
            boolean isShift = Screen.hasShiftDown();
            PacketHandler.INSTANCE.sendToServer((Object)new PacketPlacePing(isShift));
            mc.player.playSound((SoundEvent)SoundEvents.UI_BUTTON_CLICK.get(), 0.3f, 1.5f);
        }
    }

    private static void render3DMarker(PoseStack poseStack, Vec3 cameraPos, BlockPos pos, ResourceLocation texture, Minecraft mc, float alpha, double heightOffset) {
        poseStack.pushPose();
        double renderX = (double)pos.getX() + 0.5 - cameraPos.x;
        double renderY = (double)pos.getY() + heightOffset - cameraPos.y;
        double renderZ = (double)pos.getZ() + 0.5 - cameraPos.z;
        poseStack.translate(renderX, renderY, renderZ);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float scale = 0.025f;
        double distSq = mc.player.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
        if (distSq > 100.0) {
            scale *= (float)(Math.sqrt(distSq) / 10.0);
        }
        scale = Math.min(scale, 0.25f);
        poseStack.scale(-scale, -scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderTexture(0, (ResourceLocation)texture);
        RenderSystem.setShader(GameRenderer::m_172820_);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int a = (int)(alpha * 255.0f);
        buf.vertex(matrix, -8.0f, -8.0f, 0.0f).uv(0.0f, 0.0f).color(255, 255, 255, a).endVertex();
        buf.vertex(matrix, -8.0f, 8.0f, 0.0f).uv(0.0f, 1.0f).color(255, 255, 255, a).endVertex();
        buf.vertex(matrix, 8.0f, 8.0f, 0.0f).uv(1.0f, 1.0f).color(255, 255, 255, a).endVertex();
        buf.vertex(matrix, 8.0f, -8.0f, 0.0f).uv(1.0f, 0.0f).color(255, 255, 255, a).endVertex();
        tesselator.end();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        HitResult result;
        ItemStack stack;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        Entity vehicle = mc.player.getVehicle();
        if (event.getButton() == 0 && (vehicle instanceof M2BrowningEntity || vehicle instanceof AGS30Entity)) {
            event.setCanceled(true);
        }
        if (ModKeyBindings.PLACE_PING_KEY.matchesMouse(event.getButton()) && event.getAction() == 1 && mc.screen == null) {
            ClientEvents.tryPlacePingLogic(mc);
            event.setCanceled(true);
        }
        if (event.getButton() == 1 && event.getAction() == 1 && (vehicle instanceof M2BrowningEntity || vehicle instanceof AGS30Entity)) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketToggleAim());
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == 2 && event.getAction() == 1 && ((stack = mc.player.getMainHandItem()).getItem() instanceof VehicleMarkerItem || stack.getItem() instanceof SupplyTruckMarkerItem) && (result = mc.hitResult) != null && result.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityResult = (EntityHitResult)result;
            Entity target = entityResult.getEntity();
            PacketHandler.INSTANCE.sendToServer((Object)new PacketApplyMarker(target.getId()));
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            AGS30Entity ags;
            M2BrowningEntity m2;
            Entity vehicle = mc.player.getVehicle();
            if (vehicle instanceof M2BrowningEntity && (m2 = (M2BrowningEntity)vehicle).isAiming()) {
                event.setNewFovModifier(event.getFovModifier() * 0.6666667f);
            } else if (vehicle instanceof AGS30Entity && (ags = (AGS30Entity)vehicle).isAiming()) {
                event.setNewFovModifier(event.getFovModifier() * 0.33333334f);
            }
        }
    }

    @SubscribeEvent
    public static void onOpenGui(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof DeathScreen && !(event.getScreen() instanceof AASDeathScreen)) {
            if (ClientData.globalDeathTimestamp == 0L) {
                ClientData.globalDeathTimestamp = System.currentTimeMillis();
            }
            ClientData.deathFadeStartTime = System.currentTimeMillis();
            ClientData.deathFadePlayed = true;
            Component cause = null;
            if (Minecraft.getInstance().player != null) {
                cause = Minecraft.getInstance().player.getCombatTracker().getDeathMessage();
            }
            event.setNewScreen((Screen)new AASDeathScreen(cause, false));
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        boolean isCandidate;
        String myCandidateName;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (event.getKey() == 266 && event.getAction() == 1) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketConfirmArtStrike(true));
        }
        if (event.getKey() == 267 && event.getAction() == 1) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketConfirmArtStrike(false));
        }
        String myTeam = mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";
        boolean isBlue = myTeam.equals("BLUE");
        boolean myTeamCmdActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        String string = myCandidateName = isBlue ? ClientData.blueCmdCandidateName : ClientData.redCmdCandidateName;
        if (myTeamCmdActive && mc.screen == null && !(isCandidate = mc.player.getScoreboardName().equals(myCandidateName))) {
            if (event.getKey() == 296 && event.getAction() == 1) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketCMDVote(true));
                mc.player.displayClientMessage((Component)Component.literal((String)"Voted: YES").withStyle(ChatFormatting.GREEN), true);
                return;
            }
            if (event.getKey() == 297 && event.getAction() == 1) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketCMDVote(false));
                mc.player.displayClientMessage((Component)Component.literal((String)"Voted: NO").withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        if (ModKeyBindings.PLACE_PING_KEY.matches(event.getKey(), event.getScanCode()) && event.getAction() == 1) {
            ClientEvents.tryPlacePingLogic(mc);
            return;
        }
        if (ClientData.voteActive && mc.screen == null && event.getAction() == 1) {
            if (event.getKey() == 298) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketVoteAction(true));
                return;
            }
            if (event.getKey() == 299) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketVoteAction(false));
                return;
            }
        }
        if (event.getAction() == 1 && ModKeyBindings.DROP_SUPPLY_KEY.matches(event.getKey(), event.getScanCode()) && mc.player.getVehicle() != null) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketDropCrate());
        }
        if (event.getAction() == 1 && ModKeyBindings.OPEN_SQUAD_MENU_KEY.matches(event.getKey(), event.getScanCode()) && mc.screen == null) {
            boolean isValidTeam;
            String teamName = mc.player.getTeam() != null ? mc.player.getTeam().getName() : "";
            boolean bl = isValidTeam = teamName.equalsIgnoreCase("Blue") || teamName.equalsIgnoreCase("Red");
            if (!isValidTeam) {
                mc.setScreen((Screen)new TeamSelectionScreen());
            } else {
                mc.setScreen((Screen)new SquadSelectionScreen());
            }
        }
        if (ModKeyBindings.SHOW_MAP_KEY.matches(event.getKey(), event.getScanCode())) {
            if (event.getAction() == 1) {
                ClientData.isMapOpen = true;
            } else if (event.getAction() == 0) {
                ClientData.isMapOpen = false;
            }
        }
        if (event.getKey() == 298 && event.getAction() == 1 && mc.player.isCreative() && mc.screen == null) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketDebugFill());
            return;
        }
    }

    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (((Boolean)AASConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()).booleanValue() && !mc.player.isCreative() && mc.player.getVehicle() != null && (event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof AbstractContainerScreen)) {
            event.setCanceled(true);
            mc.player.displayClientMessage((Component)Component.literal((String)"Inventory is disabled while inside a vehicle!").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        boolean isDowned;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean bl = isDowned = mc.player.getPersistentData().getBoolean("AAS_IsDowned") && mc.player.isAlive();
        if (isDowned) {
            if (!wasDownedLastFrame) {
                initialPitchOnFall = event.getPitch();
                wasDownedLastFrame = true;
            }
            if (downedCameraProgress < 1.0f) {
                downedCameraProgress += 0.07f;
            }
        } else {
            wasDownedLastFrame = false;
            if (downedCameraProgress > 0.0f) {
                downedCameraProgress -= 0.05f;
            }
        }
        if (downedCameraProgress > 0.0f) {
            float p = downedCameraProgress;
            float stumble = 0.0f;
            if (p < 0.4f) {
                stumble = Mth.sin((float)(p * (float)Math.PI * 2.5f)) * 12.0f;
            }
            float targetPitch = -60.0f;
            float smoothPitch = Mth.lerp((float)p, (float)initialPitchOnFall, (float)targetPitch) + stumble;
            event.setPitch(smoothPitch);
            event.setRoll(p * 35.0f);
            if (isDowned && p > 0.8f) {
                float force = (p - 0.8f) * 10.0f;
                event.setYaw(event.getYaw() + (mc.level.random.nextFloat() - 0.5f) * force);
                event.setPitch(event.getPitch() + (mc.level.random.nextFloat() - 0.5f) * force);
            }
            if (p >= 1.0f && isDowned) {
                float breathing = Mth.sin((float)((float)mc.level.getGameTime() * 0.06f)) * 1.5f;
                event.setPitch(targetPitch + breathing);
            }
        }
    }
}

