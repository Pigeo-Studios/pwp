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
    private static final ResourceLocation PING_TEXTURE = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye.png");
    private static final ResourceLocation MOVE_TEXTURE = new ResourceLocation("aas", "textures/gui/map_icons/marker_move.png");
    private static final ResourceLocation MOVE_TEX_BRAVO = new ResourceLocation("aas", "textures/gui/map_icons/marker_move_bravo.png");
    private static final ResourceLocation PING_TEX_BRAVO = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye_bravo.png");
    private static final ResourceLocation MOVE_TEX_CHARLIE = new ResourceLocation("aas", "textures/gui/map_icons/marker_move_charlie.png");
    private static final ResourceLocation PING_TEX_CHARLIE = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye_charlie.png");
    private static boolean lastSquadState = false;
    private static boolean lastCommandState = false;
    private static float downedCameraProgress = 0.0f;
    private static float initialPitchOnFall = 0.0f;
    private static boolean wasDownedLastFrame = false;

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        ClientData.addChatMessage(event.getMessage());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null) {
            return;
        }
        if (!(!mc.f_91074_.m_6084_() || mc.f_91074_.getPersistentData().m_128471_("AAS_IsDowned") || mc.f_91080_ instanceof AASDeathScreen || ClientData.globalDeathTimestamp == 0L || mc.f_91080_ instanceof PlayerKitSelectScreen || mc.f_91080_ instanceof MapMarkerGridScreen)) {
            ClientData.globalDeathTimestamp = 0L;
            ClientData.deathFadePlayed = false;
        }
        if (ClientData.voteActive && ClientData.voteTimer > 0 && mc.f_91073_.m_46467_() % 20L == 0L) {
            --ClientData.voteTimer;
        }
        RecoilHandler.clientTick();
        Entity vehicle = mc.f_91074_.m_20202_();
        if (vehicle instanceof M2BrowningEntity || vehicle instanceof AGS30Entity) {
            boolean isLeftBtnDown;
            long windowId = mc.m_91268_().m_85439_();
            boolean bl = isLeftBtnDown = GLFW.glfwGetMouseButton((long)windowId, (int)0) == 1;
            if (isLeftBtnDown && mc.f_91080_ == null) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketVehicleShoot());
                while (mc.f_91066_.f_92096_.m_90859_()) {
                }
            }
        }
        if (mc.f_91074_.getPersistentData().m_128471_("AAS_IsDowned") && mc.f_91074_.m_6084_()) {
            mc.f_91074_.m_20334_(0.0, mc.f_91074_.m_20184_().f_82480_, 0.0);
            if (mc.f_91080_ == null) {
                mc.m_91152_((Screen)new DownedScreen());
            }
        } else if (mc.f_91074_.getPersistentData().m_128441_("AAS_DownedYaw")) {
            mc.f_91074_.getPersistentData().m_128473_("AAS_DownedYaw");
            mc.f_91074_.getPersistentData().m_128473_("AAS_DownedPitch");
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        double delta;
        Minecraft mc = Minecraft.m_91087_();
        if (ClientData.isMapOpen && mc.f_91080_ == null && (delta = event.getScrollDelta()) != 0.0) {
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
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null) {
            return;
        }
        String myName = mc.f_91074_.m_6302_();
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
        Vec3 cameraPos = event.getCamera().m_90583_();
        long gameTime = mc.f_91073_.m_46467_();
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
        String myName = mc.f_91074_.m_6302_();
        boolean canPing = false;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(myName) && !s.bravoLeader.equals(myName) && !s.charlieLeader.equals(myName)) continue;
            canPing = true;
            break;
        }
        if (canPing || mc.f_91074_.m_7500_()) {
            boolean isShift = Screen.m_96638_();
            PacketHandler.INSTANCE.sendToServer((Object)new PacketPlacePing(isShift));
            mc.f_91074_.m_5496_((SoundEvent)SoundEvents.f_12490_.get(), 0.3f, 1.5f);
        }
    }

    private static void render3DMarker(PoseStack poseStack, Vec3 cameraPos, BlockPos pos, ResourceLocation texture, Minecraft mc, float alpha, double heightOffset) {
        poseStack.m_85836_();
        double renderX = (double)pos.m_123341_() + 0.5 - cameraPos.f_82479_;
        double renderY = (double)pos.m_123342_() + heightOffset - cameraPos.f_82480_;
        double renderZ = (double)pos.m_123343_() + 0.5 - cameraPos.f_82481_;
        poseStack.m_85837_(renderX, renderY, renderZ);
        poseStack.m_252781_(mc.m_91290_().m_253208_());
        float scale = 0.025f;
        double distSq = mc.f_91074_.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5);
        if (distSq > 100.0) {
            scale *= (float)(Math.sqrt(distSq) / 10.0);
        }
        scale = Math.min(scale, 0.25f);
        poseStack.m_85841_(-scale, -scale, scale);
        Matrix4f matrix = poseStack.m_85850_().m_252922_();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)texture);
        RenderSystem.setShader(GameRenderer::m_172820_);
        Tesselator tesselator = Tesselator.m_85913_();
        BufferBuilder buf = tesselator.m_85915_();
        buf.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85819_);
        int a = (int)(alpha * 255.0f);
        buf.m_252986_(matrix, -8.0f, -8.0f, 0.0f).m_7421_(0.0f, 0.0f).m_6122_(255, 255, 255, a).m_5752_();
        buf.m_252986_(matrix, -8.0f, 8.0f, 0.0f).m_7421_(0.0f, 1.0f).m_6122_(255, 255, 255, a).m_5752_();
        buf.m_252986_(matrix, 8.0f, 8.0f, 0.0f).m_7421_(1.0f, 1.0f).m_6122_(255, 255, 255, a).m_5752_();
        buf.m_252986_(matrix, 8.0f, -8.0f, 0.0f).m_7421_(1.0f, 0.0f).m_6122_(255, 255, 255, a).m_5752_();
        tesselator.m_85914_();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        poseStack.m_85849_();
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        HitResult result;
        ItemStack stack;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null) {
            return;
        }
        Entity vehicle = mc.f_91074_.m_20202_();
        if (event.getButton() == 0 && (vehicle instanceof M2BrowningEntity || vehicle instanceof AGS30Entity)) {
            event.setCanceled(true);
        }
        if (ModKeyBindings.PLACE_PING_KEY.m_90830_(event.getButton()) && event.getAction() == 1 && mc.f_91080_ == null) {
            ClientEvents.tryPlacePingLogic(mc);
            event.setCanceled(true);
        }
        if (event.getButton() == 1 && event.getAction() == 1 && (vehicle instanceof M2BrowningEntity || vehicle instanceof AGS30Entity)) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketToggleAim());
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == 2 && event.getAction() == 1 && ((stack = mc.f_91074_.m_21205_()).m_41720_() instanceof VehicleMarkerItem || stack.m_41720_() instanceof SupplyTruckMarkerItem) && (result = mc.f_91077_) != null && result.m_6662_() == HitResult.Type.ENTITY) {
            EntityHitResult entityResult = (EntityHitResult)result;
            Entity target = entityResult.m_82443_();
            PacketHandler.INSTANCE.sendToServer((Object)new PacketApplyMarker(target.m_19879_()));
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ != null) {
            AGS30Entity ags;
            M2BrowningEntity m2;
            Entity vehicle = mc.f_91074_.m_20202_();
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
            if (Minecraft.m_91087_().f_91074_ != null) {
                cause = Minecraft.m_91087_().f_91074_.m_21231_().m_19293_();
            }
            event.setNewScreen((Screen)new AASDeathScreen(cause, false));
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        boolean isCandidate;
        String myCandidateName;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null) {
            return;
        }
        if (event.getKey() == 266 && event.getAction() == 1) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketConfirmArtStrike(true));
        }
        if (event.getKey() == 267 && event.getAction() == 1) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketConfirmArtStrike(false));
        }
        String myTeam = mc.f_91074_.m_5647_() != null ? mc.f_91074_.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
        boolean isBlue = myTeam.equals("BLUE");
        boolean myTeamCmdActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        String string = myCandidateName = isBlue ? ClientData.blueCmdCandidateName : ClientData.redCmdCandidateName;
        if (myTeamCmdActive && mc.f_91080_ == null && !(isCandidate = mc.f_91074_.m_6302_().equals(myCandidateName))) {
            if (event.getKey() == 296 && event.getAction() == 1) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketCMDVote(true));
                mc.f_91074_.m_5661_((Component)Component.m_237113_((String)"Voted: YES").m_130940_(ChatFormatting.GREEN), true);
                return;
            }
            if (event.getKey() == 297 && event.getAction() == 1) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketCMDVote(false));
                mc.f_91074_.m_5661_((Component)Component.m_237113_((String)"Voted: NO").m_130940_(ChatFormatting.RED), true);
                return;
            }
        }
        if (ModKeyBindings.PLACE_PING_KEY.m_90832_(event.getKey(), event.getScanCode()) && event.getAction() == 1) {
            ClientEvents.tryPlacePingLogic(mc);
            return;
        }
        if (ClientData.voteActive && mc.f_91080_ == null && event.getAction() == 1) {
            if (event.getKey() == 298) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketVoteAction(true));
                return;
            }
            if (event.getKey() == 299) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketVoteAction(false));
                return;
            }
        }
        if (event.getAction() == 1 && ModKeyBindings.DROP_SUPPLY_KEY.m_90832_(event.getKey(), event.getScanCode()) && mc.f_91074_.m_20202_() != null) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketDropCrate());
        }
        if (event.getAction() == 1 && ModKeyBindings.OPEN_SQUAD_MENU_KEY.m_90832_(event.getKey(), event.getScanCode()) && mc.f_91080_ == null) {
            boolean isValidTeam;
            String teamName = mc.f_91074_.m_5647_() != null ? mc.f_91074_.m_5647_().m_5758_() : "";
            boolean bl = isValidTeam = teamName.equalsIgnoreCase("Blue") || teamName.equalsIgnoreCase("Red");
            if (!isValidTeam) {
                mc.m_91152_((Screen)new TeamSelectionScreen());
            } else {
                mc.m_91152_((Screen)new SquadSelectionScreen());
            }
        }
        if (ModKeyBindings.SHOW_MAP_KEY.m_90832_(event.getKey(), event.getScanCode())) {
            if (event.getAction() == 1) {
                ClientData.isMapOpen = true;
            } else if (event.getAction() == 0) {
                ClientData.isMapOpen = false;
            }
        }
        if (event.getKey() == 298 && event.getAction() == 1 && mc.f_91074_.m_7500_() && mc.f_91080_ == null) {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketDebugFill());
            return;
        }
    }

    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null) {
            return;
        }
        if (((Boolean)AASConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()).booleanValue() && !mc.f_91074_.m_7500_() && mc.f_91074_.m_20202_() != null && (event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof AbstractContainerScreen)) {
            event.setCanceled(true);
            mc.f_91074_.m_5661_((Component)Component.m_237113_((String)"Inventory is disabled while inside a vehicle!").m_130940_(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        boolean isDowned;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null) {
            return;
        }
        boolean bl = isDowned = mc.f_91074_.getPersistentData().m_128471_("AAS_IsDowned") && mc.f_91074_.m_6084_();
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
                stumble = Mth.m_14031_((float)(p * (float)Math.PI * 2.5f)) * 12.0f;
            }
            float targetPitch = -60.0f;
            float smoothPitch = Mth.m_14179_((float)p, (float)initialPitchOnFall, (float)targetPitch) + stumble;
            event.setPitch(smoothPitch);
            event.setRoll(p * 35.0f);
            if (isDowned && p > 0.8f) {
                float force = (p - 0.8f) * 10.0f;
                event.setYaw(event.getYaw() + (mc.f_91073_.f_46441_.m_188501_() - 0.5f) * force);
                event.setPitch(event.getPitch() + (mc.f_91073_.f_46441_.m_188501_() - 0.5f) * force);
            }
            if (p >= 1.0f && isDowned) {
                float breathing = Mth.m_14031_((float)((float)mc.f_91073_.m_46467_() * 0.06f)) * 1.5f;
                event.setPitch(targetPitch + breathing);
            }
        }
    }
}

