/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderGuiOverlayEvent$Post
 *  net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.example.aas.client;

import com.example.aas.block.AGSConstructionBlockEntity;
import com.example.aas.block.BarbedWireBlock;
import com.example.aas.block.BarbedWireBlockEntity;
import com.example.aas.block.HubBlock;
import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.M2ConstructionBlockEntity;
import com.example.aas.block.MainSupplyBlock;
import com.example.aas.block.MortarConstructionBlockEntity;
import com.example.aas.block.TOWConstructionBlockEntity;
import com.example.aas.block.WallBlock;
import com.example.aas.block.WallBlockEntity;
import com.example.aas.client.ClientData;
import com.example.aas.client.ClientPlacementHandler;
import com.example.aas.client.gui.AASMapRenderer;
import com.example.aas.config.AASConfig;
import com.example.aas.entity.AGS30Entity;
import com.example.aas.entity.M2BrowningEntity;
import com.example.aas.entity.SupplyCrateEntity;
import com.example.aas.item.ModItems;
import com.example.aas.network.MapPlayerInfo;
import com.example.aas.world.AASWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="aas", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class AASOverlay {
    private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("aas", "textures/gui/flags/ukraine.png");
    private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("aas", "textures/gui/flags/russia.png");
    private static final ResourceLocation FLAG_USA = new ResourceLocation("aas", "textures/gui/flags/usa.png");
    private static final ResourceLocation FLAG_NATO = new ResourceLocation("aas", "textures/gui/flags/nato.png");
    private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("aas", "textures/gui/flags/bluefor.png");
    private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("aas", "textures/gui/flags/redfor.png");
    private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("aas", "textures/gui/flags/insurgency.png");
    private static final ResourceLocation FLAG_PMC = new ResourceLocation("aas", "textures/gui/flags/pmc.png");
    private static final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("aas", "textures/misc/vignette.png");
    private static final ResourceLocation ARROW_TEX = new ResourceLocation("aas", "textures/gui/capture_arrow.png");
    private static final ResourceLocation BUILD_ICON = new ResourceLocation("aas", "textures/gui/build_icon.png");
    private static final ResourceLocation DIG_ICON = new ResourceLocation("aas", "textures/gui/dig_icon.png");
    private static final ResourceLocation SHOVEL_ICON = new ResourceLocation("aas", "textures/gui/shovel_icon.png");
    private static final ResourceLocation ICON_PLUS = new ResourceLocation("aas", "textures/gui/map_icons/medic_plus.png");
    private static final ResourceLocation ICON_PING = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye.png");
    private static final ResourceLocation MOVE_TEXTURE = new ResourceLocation("aas", "textures/gui/map_icons/marker_move.png");
    private static final ResourceLocation MOVE_TEX_BRAVO = new ResourceLocation("aas", "textures/gui/map_icons/marker_move_bravo.png");
    private static final ResourceLocation PING_TEX_BRAVO = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye_bravo.png");
    private static final ResourceLocation MOVE_TEX_CHARLIE = new ResourceLocation("aas", "textures/gui/map_icons/marker_move_charlie.png");
    private static final ResourceLocation PING_TEX_CHARLIE = new ResourceLocation("aas", "textures/gui/map_icons/ping_eye_charlie.png");
    private static AASMapRenderer HUD_SIDE_MAP;
    private static final ResourceLocation MOUSE_LEFT;
    private static final ResourceLocation MOUSE_RIGHT;
    private static final ResourceLocation ICON_ROTATE;
    private static final ResourceLocation ICON_CONFIRM;
    private static final ResourceLocation VOICE_ICON_TEX;
    private static final ResourceLocation VOICE_ICON_RADIO_TEX;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        String currentKit;
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) {
            return;
        }
        GuiGraphics gui = event.getGuiGraphics();
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null) {
            return;
        }
        int width = mc.m_91268_().m_85445_();
        int height = mc.m_91268_().m_85446_();
        AASOverlay.renderTickets(gui, mc, width);
        AASOverlay.renderVotePanel(gui, mc, height);
        if (ClientData.allCapturePoints != null) {
            for (AASWorldData.CapturePoint cp : ClientData.allCapturePoints) {
                if (!mc.f_91074_.m_20191_().m_82381_(cp.area)) continue;
                AASOverlay.renderCapturePoint(gui, mc, width, height);
                break;
            }
        }
        AASOverlay.renderBuildProgress(gui, mc, width, height);
        AASOverlay.renderHubMaterials(gui, mc, width, height);
        AASOverlay.renderVehicleAmmo(gui, mc, width, height);
        AASOverlay.renderSupplyTruckInfo(gui, mc, width, height);
        AASOverlay.renderPlacementHints(gui, mc, width, height);
        AASOverlay.renderCaptureNotifications(gui, mc, width, height);
        AASOverlay.renderCompass(gui, mc, width, event.getPartialTick());
        AASOverlay.renderCMDVotePanel(gui, mc, width);
        AASOverlay.renderArtStrikeRequest(gui, mc, width);
        if (mc.f_91074_.getPersistentData().m_128471_("AAS_IsDowned")) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor((float)0.5f, (float)0.0f, (float)0.0f, (float)0.9f);
            gui.m_280163_(VIGNETTE_TEXTURE, 0, 0, 0.0f, 0.0f, width, height, width, height);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableDepthTest();
        }
        if ("Medic".equalsIgnoreCase(currentKit = mc.f_91074_.getPersistentData().m_128461_("AAS_CurrentKit"))) {
            AASOverlay.renderMedicUI(gui, mc, width);
        }
        AASOverlay.renderRadioSpeakers(gui, mc, height);
        AASOverlay.renderVoiceSpeakers(gui, mc, height);
        if (ClientData.isMapOpen || ClientData.mapTransition > 0.0f) {
            AASOverlay.renderSideMap(gui, mc, width, height, event.getPartialTick());
        }
    }

    private static void renderCompass(GuiGraphics gui, Minecraft mc, int screenWidth, float partialTick) {
        if (mc.f_91066_.f_92062_) {
            return;
        }
        int configVal = (Integer)AASConfig.COMPASS_SCALE.get();
        float scaleFactor = 1.0f;
        if (configVal == 1) {
            scaleFactor = 0.6666667f;
        } else if (configVal == 3) {
            scaleFactor = 1.5f;
        }
        float centerX = (float)screenWidth / 2.0f;
        float y = 8.0f;
        float baseWidth = 320.0f;
        float scaledWidth = baseWidth * scaleFactor;
        float visibleRange = 70.0f;
        float pixelsPerDegree = scaledWidth / visibleRange;
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_252880_(centerX, y, 0.0f);
        gui.m_280168_().m_85841_(scaleFactor, scaleFactor, 1.0f);
        gui.m_280168_().m_252880_(-centerX, -y, 0.0f);
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_252880_(centerX, y + 2.0f, 100.0f);
        gui.m_280168_().m_85841_(0.8f, 0.8f, 1.0f);
        AASOverlay.drawOutlinedString(gui, mc, "\u25bc", -(mc.f_91062_.m_92895_("\u25bc") / 2), 0, -1);
        gui.m_280168_().m_85849_();
        float playerYaw = (mc.f_91074_.m_5675_(partialTick) % 360.0f + 360.0f) % 360.0f;
        String bearing = String.valueOf((int)playerYaw);
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_252880_(centerX, y + 26.0f, 100.0f);
        AASOverlay.drawOutlinedString(gui, mc, bearing, -(mc.f_91062_.m_92895_(bearing) / 2), 0, -1);
        gui.m_280168_().m_85849_();
        gui.m_280588_((int)(centerX - scaledWidth / 2.0f), 0, (int)(centerX + scaledWidth / 2.0f), AASOverlay.screenHeight());
        for (int i = (int)(playerYaw - visibleRange / 2.0f) - 1; i <= (int)(playerYaw + visibleRange / 2.0f) + 1; ++i) {
            int degree = (i % 360 + 360) % 360;
            float xPos = centerX + ((float)i - playerYaw) * pixelsPerDegree;
            float diffFromCenter = Math.abs(xPos - centerX);
            float edgeFading = 1.0f - (float)Math.pow(diffFromCenter / (scaledWidth / 2.0f), 2.0);
            if (edgeFading <= 0.02f) continue;
            int alpha = (int)(edgeFading * 255.0f);
            if (degree % 15 == 0) {
                AASOverlay.drawSmoothLine(gui, xPos, y + 10.0f, 1.2f, 6.0f, alpha << 24 | 0xFFFFFF);
                String label = AASOverlay.getDirectionLabel(degree);
                int txtCol = !Character.isDigit(label.charAt(0)) ? alpha << 24 | 0x88CCFF : alpha << 24 | 0xFFFFFF;
                gui.m_280168_().m_85836_();
                gui.m_280168_().m_252880_(xPos, y + 18.0f, 0.0f);
                gui.m_280168_().m_85841_(0.75f, 0.75f, 1.0f);
                AASOverlay.drawOutlinedStringWithAlpha(gui, mc, label, -(mc.f_91062_.m_92895_(label) / 2), 0, txtCol, alpha);
                gui.m_280168_().m_85849_();
                continue;
            }
            if (degree % 5 == 0) {
                AASOverlay.drawSmoothLine(gui, xPos, y + 12.0f, 1.0f, 4.0f, alpha << 24 | 0xFFFFFF);
                continue;
            }
            AASOverlay.drawSmoothLine(gui, xPos, y + 14.0f, 0.8f, 1.5f, alpha << 24 | 0xFFFFFF);
        }
        AASOverlay.renderMarkersOnCompass(gui, mc, playerYaw, centerX, y, pixelsPerDegree, scaledWidth, visibleRange);
        AASOverlay.renderDownedOnCompass(gui, mc, playerYaw, centerX, y, pixelsPerDegree, scaledWidth, visibleRange);
        AASOverlay.renderSquadPingOnCompass(gui, mc, playerYaw, centerX, y, pixelsPerDegree, scaledWidth, visibleRange);
        AASOverlay.renderSquadMarkersOnCompass(gui, mc, playerYaw, centerX, y, pixelsPerDegree, scaledWidth, visibleRange);
        gui.m_280618_();
        gui.m_280168_().m_85849_();
    }

    private static void renderSquadMarkersOnCompass(GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange) {
        boolean isCharlie;
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
        boolean bl = isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
        if (mySquad.marker != null && mySquad.marker.type == 0) {
            AASOverlay.drawSquadCompassMarker(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.marker, MOVE_TEXTURE);
        }
        if (mySquad.bravoMarker != null && mySquad.bravoMarker.type == 0) {
            AASOverlay.drawSquadCompassMarker(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.bravoMarker, MOVE_TEX_BRAVO);
        }
        if (mySquad.charlieMarker != null && mySquad.charlieMarker.type == 0) {
            AASOverlay.drawSquadCompassMarker(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.charlieMarker, MOVE_TEX_CHARLIE);
        }
    }

    private static void drawSquadCompassMarker(GuiGraphics gui, Minecraft mc, float pYaw, float cX, float y, float ppd, float wPix, float vRange, AASWorldData.SquadMarker m, ResourceLocation icon) {
        double dist = Math.sqrt(mc.f_91074_.m_20275_((double)m.x + 0.5, mc.f_91074_.m_20186_(), (double)m.z + 0.5));
        if (dist > 5000.0) {
            return;
        }
        double dx = (double)m.x + 0.5 - mc.f_91074_.m_20185_();
        double dz = (double)m.z + 0.5 - mc.f_91074_.m_20189_();
        float angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float diff = Mth.m_14177_((float)(angle - pYaw));
        if (Math.abs(diff) < vRange / 2.0f + 5.0f) {
            float xPos = cX + diff * ppd;
            float edgeFading = 1.0f - (float)Math.pow(Math.abs(xPos - cX) / (wPix / 2.0f), 4.0);
            AASOverlay.renderCompassIcon(gui, icon, xPos, y - 2.0f, 14, Math.max(0.0f, edgeFading));
        }
    }

    private static ResourceLocation getSquadMarkerIcon(int type) {
        switch (type) {
            case 1: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_attack.png");
            }
            case 2: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_defend.png");
            }
            case 3: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_build.png");
            }
            case 5: {
                return new ResourceLocation("aas", "textures/gui/map_icons/marker_attack.png");
            }
        }
        return new ResourceLocation("aas", "textures/gui/map_icons/marker_move.png");
    }

    private static void renderMarkersOnCompass(GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange) {
        String myTeam = mc.f_91074_.m_5647_() != null ? mc.f_91074_.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
        for (AASWorldData.MapMarker m : ClientData.activeMarkers) {
            float edgeFading;
            float finalAlpha;
            double dist;
            if (!m.team.equalsIgnoreCase(myTeam) && !mc.f_91074_.m_7500_() || (dist = Math.sqrt(mc.f_91074_.m_20275_((double)m.pos.m_123341_() + 0.5, mc.f_91074_.m_20186_(), (double)m.pos.m_123343_() + 0.5))) > 250.0) continue;
            double dx = (double)m.pos.m_123341_() + 0.5 - mc.f_91074_.m_20185_();
            double dz = (double)m.pos.m_123343_() + 0.5 - mc.f_91074_.m_20189_();
            float angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            float diff = Mth.m_14177_((float)(angle - playerYaw));
            if (!(Math.abs(diff) < visibleRange / 2.0f + 5.0f)) continue;
            float xPos = centerX + diff * pixelsPerDegree;
            float distAlpha = 1.0f;
            if (dist > 150.0) {
                distAlpha = 0.35f;
            } else if (dist > 50.0) {
                distAlpha = 0.7f;
            }
            if (!((finalAlpha = distAlpha * Math.max(0.0f, edgeFading = 1.0f - (float)Math.pow(Math.abs(xPos - centerX) / (widthInPixels / 2.0f), 4.0))) > 0.05f)) continue;
            ResourceLocation icon = AASOverlay.getMarkerIconLocal(m.type);
            AASOverlay.renderCompassIcon(gui, icon, xPos, y - 2.0f, 12, finalAlpha);
        }
    }

    private static void renderDownedOnCompass(GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange) {
        if (ClientData.mapPlayers == null) {
            return;
        }
        String myName = mc.f_91074_.m_6302_();
        String myTeam = "NEUTRAL";
        if (mc.f_91074_.m_5647_() != null) {
            myTeam = mc.f_91074_.m_5647_().m_5758_().toUpperCase();
        }
        for (MapPlayerInfo info : ClientData.mapPlayers.values()) {
            float angle;
            float diff;
            double dz;
            double dx;
            double dist;
            if (info.name.equals(myName) || !info.isDowned || myTeam.equals("NEUTRAL") || !info.team.equalsIgnoreCase(myTeam) || (dist = Math.sqrt((dx = info.x - mc.f_91074_.m_20185_()) * dx + (dz = info.z - mc.f_91074_.m_20189_()) * dz)) > 50.0 || !(Math.abs(diff = Mth.m_14177_((float)((angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f) - playerYaw))) < visibleRange / 2.0f + 5.0f)) continue;
            float xPos = centerX + diff * pixelsPerDegree;
            float edgeFading = 1.0f - (float)Math.pow(Math.abs(xPos - centerX) / (widthInPixels / 2.0f), 4.0);
            float blink = 0.8f + (float)Math.sin((double)System.currentTimeMillis() / 200.0) * 0.2f;
            float finalAlpha = Math.max(0.0f, edgeFading) * blink;
            if (!(finalAlpha > 0.05f)) continue;
            AASOverlay.renderCompassIcon(gui, ICON_PLUS, xPos, y - 2.0f, 10, finalAlpha);
        }
    }

    private static void renderSquadPingOnCompass(GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange) {
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
        long time = mc.f_91073_.m_46467_();
        if (mySquad.pingPos != null && time < mySquad.pingExpiry) {
            AASOverlay.drawCompassPing(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.pingPos, ICON_PING);
        }
        if (mySquad.bravoPingPos != null && time < mySquad.bravoPingExpiry && (isSL || isBravo)) {
            AASOverlay.drawCompassPing(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.bravoPingPos, PING_TEX_BRAVO);
        }
        if (mySquad.charliePingPos != null && time < mySquad.charliePingExpiry && (isSL || isCharlie)) {
            AASOverlay.drawCompassPing(gui, mc, playerYaw, centerX, y, pixelsPerDegree, widthInPixels, visibleRange, mySquad.charliePingPos, PING_TEX_CHARLIE);
        }
    }

    private static void drawCompassPing(GuiGraphics gui, Minecraft mc, float playerYaw, float centerX, float y, float pixelsPerDegree, float widthInPixels, float visibleRange, BlockPos pingPos, ResourceLocation icon) {
        double dx = (double)pingPos.m_123341_() + 0.5 - mc.f_91074_.m_20185_();
        double dz = (double)pingPos.m_123343_() + 0.5 - mc.f_91074_.m_20189_();
        float angle = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float diff = Mth.m_14177_((float)(angle - playerYaw));
        if (Math.abs(diff) < visibleRange / 2.0f + 5.0f) {
            float xPos = centerX + diff * pixelsPerDegree;
            float edgeFading = 1.0f - (float)Math.pow(Math.abs(xPos - centerX) / (widthInPixels / 2.0f), 4.0);
            float blink = 0.8f + (float)Math.sin((float)mc.f_91073_.m_46467_() * 0.4f) * 0.2f;
            float finalAlpha = Math.max(0.0f, edgeFading) * blink;
            if (finalAlpha > 0.05f) {
                AASOverlay.renderCompassIcon(gui, icon, xPos, y - 2.0f, 12, finalAlpha);
            }
        }
    }

    private static void renderCompassIcon(GuiGraphics gui, ResourceLocation icon, float x, float y, int size, float alpha) {
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_252880_(x - (float)size / 2.0f, y - (float)size / 2.0f, 50.0f);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)alpha);
        gui.m_280163_(icon, 0, 0, 0.0f, 0.0f, size, size, size, size);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        gui.m_280168_().m_85849_();
    }

    private static ResourceLocation getMarkerIconLocal(String type) {
        String path = type.toLowerCase().replace("enemy ", "").replace(" ", "_");
        return new ResourceLocation("aas", "textures/gui/map_icons/" + path + "_marker.png");
    }

    private static int screenHeight() {
        return Minecraft.m_91087_().m_91268_().m_85446_();
    }

    private static void drawSmoothLine(GuiGraphics gui, float x, float y, float width, float height, int color) {
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_252880_(x - width / 2.0f, y, 0.0f);
        gui.m_280509_(0, 0, Math.max(1, (int)width), (int)height, color);
        gui.m_280168_().m_85849_();
    }

    private static void drawOutlinedStringWithAlpha(GuiGraphics gui, Minecraft mc, String text, int x, int y, int color, int alpha) {
        int black = alpha << 24;
        gui.m_280056_(mc.f_91062_, text, x - 1, y, black, false);
        gui.m_280056_(mc.f_91062_, text, x + 1, y, black, false);
        gui.m_280056_(mc.f_91062_, text, x, y - 1, black, false);
        gui.m_280056_(mc.f_91062_, text, x, y + 1, black, false);
        gui.m_280056_(mc.f_91062_, text, x, y, color, false);
    }

    private static String getDirectionLabel(int degree) {
        switch (degree) {
            case 0: {
                return "S";
            }
            case 45: {
                return "SW";
            }
            case 90: {
                return "W";
            }
            case 135: {
                return "NW";
            }
            case 180: {
                return "N";
            }
            case 225: {
                return "NE";
            }
            case 270: {
                return "E";
            }
            case 315: {
                return "SE";
            }
        }
        return String.valueOf(degree);
    }

    private static void renderCMDVotePanel(GuiGraphics gui, Minecraft mc, int width) {
        boolean active;
        String myTeam;
        String string = myTeam = mc.f_91074_.m_5647_() != null ? mc.f_91074_.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
        if (myTeam.equals("NEUTRAL")) {
            return;
        }
        boolean isBlue = myTeam.equals("BLUE");
        boolean bl = active = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        if (!active) {
            return;
        }
        String candidateName = isBlue ? ClientData.blueCmdCandidateName : ClientData.redCmdCandidateName;
        Map<UUID, Boolean> currentVotes = isBlue ? ClientData.blueCmdVotes : ClientData.redCmdVotes;
        List<MapPlayerInfo> slPlayers = ClientData.mapPlayers.values().stream().filter(info -> info.team.equalsIgnoreCase(myTeam)).filter(info -> info.isLeader && !info.name.equals(candidateName)).toList();
        int headerH = 32;
        int rowH = 12;
        int footerH = 15;
        int pWidth = 150;
        int pHeight = headerH + slPlayers.size() * rowH + footerH;
        int x = width - pWidth - 10;
        int y = 70;
        gui.m_280509_(x, y, x + pWidth, y + pHeight, -1442840576);
        gui.m_280637_(x, y, pWidth, pHeight, -11141291);
        gui.m_280137_(mc.f_91062_, "CMD VOTE: " + candidateName, x + pWidth / 2, y + 5, -10496);
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_85841_(0.75f, 0.75f, 1.0f);
        int sx = (int)((float)(x + pWidth / 2) / 0.75f);
        gui.m_280137_(mc.f_91062_, "Needs 50% SL votes to pass", sx, (int)((float)(y + 16) / 0.75f), -5592406);
        gui.m_280168_().m_85849_();
        gui.m_280509_(x + 5, y + 28, x + pWidth - 5, y + 29, 0x55FFFFFF);
        int curY = y + headerH;
        for (MapPlayerInfo info2 : slPlayers) {
            Boolean vote = currentVotes.get(info2.uuid);
            String icon = "\u25cb";
            int iconCol = -5592406;
            if (vote != null) {
                icon = vote != false ? "\u2714" : "\u2718";
                iconCol = vote != false ? -11141291 : -43691;
            }
            gui.m_280056_(mc.f_91062_, icon, x + 8, curY, iconCol, true);
            int nameCol = info2.name.equals(mc.f_91074_.m_6302_()) ? -171 : -1;
            gui.m_280056_(mc.f_91062_, info2.name, x + 22, curY, nameCol, true);
            curY += rowH;
        }
        if (!mc.f_91074_.m_6302_().equals(candidateName)) {
            gui.m_280137_(mc.f_91062_, "F7: YES | F8: NO", x + pWidth / 2, y + pHeight - 12, -4473925);
        } else {
            gui.m_280137_(mc.f_91062_, "WAITING FOR VOTES", x + pWidth / 2, y + pHeight - 12, -11141291);
        }
    }

    private static void renderArtStrikeRequest(GuiGraphics gui, Minecraft mc, int width) {
        int teamCmdId;
        BlockPos pos;
        String myTeam;
        if (mc.f_91074_ == null) {
            return;
        }
        String string = myTeam = mc.f_91074_.m_5647_() != null ? mc.f_91074_.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
        if (myTeam.equals("NEUTRAL")) {
            return;
        }
        int timer = myTeam.equals("BLUE") ? ClientData.blueArtTimer : ClientData.redArtTimer;
        String requester = myTeam.equals("BLUE") ? ClientData.blueArtReqName : ClientData.redArtReqName;
        BlockPos blockPos = pos = myTeam.equals("BLUE") ? ClientData.blueArtPos : ClientData.redArtPos;
        if (timer <= 0 || requester.isEmpty() || pos == null) {
            return;
        }
        int mySquadId = mc.f_91074_.getPersistentData().m_128451_("AAS_SquadID");
        boolean isSL = mc.f_91074_.getPersistentData().m_128471_("AAS_IsSquadLeader");
        int n = teamCmdId = myTeam.equals("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
        if (!(mc.f_91074_.m_7500_() || isSL && mySquadId != -1 && mySquadId == teamCmdId)) {
            return;
        }
        int x = width - 160;
        int y = 140;
        gui.m_280509_(x, y, x + 150, y + 45, -1442840576);
        gui.m_280637_(x, y, 150, 45, -43691);
        gui.m_280137_(mc.f_91062_, "ARTILLERY REQUEST", x + 75, y + 5, -43691);
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_85841_(0.8f, 0.8f, 1.0f);
        int sx = (int)((float)(x + 75) / 0.8f);
        gui.m_280137_(mc.f_91062_, "From: " + requester, sx, (int)((float)(y + 18) / 0.8f), -1);
        gui.m_280137_(mc.f_91062_, "Pos: " + pos.m_123341_() + ", " + pos.m_123343_(), sx, (int)((float)(y + 28) / 0.8f), -5592406);
        gui.m_280168_().m_85849_();
        gui.m_280137_(mc.f_91062_, "PgUp: CONFIRM | PgDn: DENY", x + 75, y + 35, -171);
        float progress = Math.max(0.0f, (float)timer / 200.0f);
        gui.m_280509_(x + 5, y + 43, x + 5 + (int)(140.0f * progress), y + 44, -1);
    }

    private static void renderCaptureNotifications(GuiGraphics gui, Minecraft mc, int screenWidth, int screenHeight) {
        if (ClientData.captureNotifications.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int flagW = 54;
        int flagH = 30;
        int x = (screenWidth - flagW) / 2;
        int y = 65;
        for (ClientData.CaptureNotification note : new ArrayList<ClientData.CaptureNotification>(ClientData.captureNotifications)) {
            int teamColor;
            long elapsed = now - note.startTime;
            long displayTime = note.duration + 2000L;
            long fadeTime = 1000L;
            if (elapsed > displayTime + fadeTime) {
                ClientData.captureNotifications.remove(note);
                continue;
            }
            float overallAlpha = 1.0f;
            if (elapsed > displayTime) {
                overallAlpha = 1.0f - (float)(elapsed - displayTime) / (float)fadeTime;
            }
            overallAlpha = Mth.m_14036_((float)overallAlpha, (float)0.0f, (float)1.0f);
            float progress = Math.min(1.0f, (float)elapsed / (float)note.duration);
            int n = teamColor = note.team.equalsIgnoreCase("BLUE") ? -13408564 : -3394765;
            boolean showTeamFlag = note.isNeutralized ? progress < 1.0f : progress >= 1.0f;
            int bgAlpha = (int)(overallAlpha * 170.0f) << 24;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)overallAlpha);
            if (showTeamFlag) {
                String faction;
                ResourceLocation tex;
                String flagTeam = note.team;
                if (note.isNeutralized) {
                    flagTeam = note.team.equalsIgnoreCase("BLUE") ? "RED" : "BLUE";
                }
                if ((tex = AASOverlay.getFlagTexture(faction = flagTeam.equalsIgnoreCase("BLUE") ? ClientData.BLUE_FACTION : ClientData.RED_FACTION)) != null) {
                    gui.m_280411_(tex, x, y, flagW, flagH, 0.0f, 0.0f, 64, 36, 64, 36);
                } else {
                    AASOverlay.renderSolidWithAlpha(gui, x, y, flagW, flagH, teamColor, overallAlpha);
                }
            } else {
                AASOverlay.renderSolidWithAlpha(gui, x, y, flagW, flagH, -1, overallAlpha);
            }
            AASOverlay.renderNotificationArrows(gui, x, y, flagW, flagH, teamColor, overallAlpha);
            AASOverlay.renderWrappingLine(gui, x, y, flagW, flagH, progress, teamColor, overallAlpha);
            String teamDisplayName = note.team.equalsIgnoreCase("BLUE") ? ClientData.customBlueName : ClientData.customRedName;
            String status = note.isNeutralized ? " neutralized " : " captured ";
            String msg = (teamDisplayName + status + note.pointName).toUpperCase();
            int textAlpha = (int)(overallAlpha * 255.0f) << 24;
            int textColor = textAlpha | 0xFFFFFF;
            gui.m_280168_().m_85836_();
            gui.m_280168_().m_252880_((float)x + (float)flagW / 2.0f, (float)(y + flagH + 8), 50.0f);
            gui.m_280168_().m_85841_(0.9f, 0.9f, 1.0f);
            gui.m_280137_(mc.f_91062_, msg, 0, 0, textColor);
            gui.m_280168_().m_85849_();
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            break;
        }
    }

    private static void renderNotificationArrows(GuiGraphics gui, int x, int y, int w, int h, int color, float overallAlpha) {
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float pulse = 0.7f + (float)Math.sin((double)System.currentTimeMillis() / 120.0) * 0.3f;
        RenderSystem.setShaderColor((float)r, (float)g, (float)b, (float)(overallAlpha * pulse));
        int arrowSize = 12;
        int centerY = y + h / 2 - arrowSize / 2;
        for (int i = 0; i < 3; ++i) {
            gui.m_280163_(ARROW_TEX, x - 18 - i * 10, centerY, 0.0f, 0.0f, arrowSize, arrowSize, arrowSize, arrowSize);
            gui.m_280168_().m_85836_();
            int rx = x + w + 18 + i * 10;
            gui.m_280168_().m_85837_((double)rx + (double)arrowSize / 2.0, (double)centerY + (double)arrowSize / 2.0, 0.0);
            gui.m_280168_().m_252781_(Axis.f_252403_.m_252977_(180.0f));
            gui.m_280163_(ARROW_TEX, -arrowSize / 2, -arrowSize / 2, 0.0f, 0.0f, arrowSize, arrowSize, arrowSize, arrowSize);
            gui.m_280168_().m_85849_();
        }
    }

    private static void renderSolidWithAlpha(GuiGraphics gui, int x, int y, int w, int h, int color, float alpha) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = (int)(alpha * 255.0f);
        gui.m_280509_(x, y, x + w, y + h, a << 24 | r << 16 | g << 8 | b);
    }

    private static void renderWrappingLine(GuiGraphics gui, int x, int y, int w, int h, float progress, int color, float alpha) {
        float totalLen = w + 2 + (h + 2) + (w + 2) + (h + 2);
        float cur = totalLen * progress;
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = (int)(alpha * 255.0f);
        int finalColor = a << 24 | r << 16 | g << 8 | b;
        float s1 = h + 2;
        float d1 = Math.min(cur, s1);
        if (d1 > 0.0f) {
            gui.m_280509_(x - 2, (int)((float)(y + h + 2) - d1), x, y + h + 2, finalColor);
        }
        float s2 = w + 2;
        if (cur > s1) {
            float d2 = Math.min(cur - s1, s2);
            gui.m_280509_(x - 2, y - 2, (int)((float)(x - 2) + d2), y, finalColor);
        }
        float s3 = h + 2;
        if (cur > s1 + s2) {
            float d3 = Math.min(cur - s1 - s2, s3);
            gui.m_280509_(x + w, y - 2, x + w + 2, (int)((float)(y - 2) + d3), finalColor);
        }
        float s4 = w + 2;
        if (cur > s1 + s2 + s3) {
            float d4 = Math.min(cur - (s1 + s2 + s3), s4);
            gui.m_280509_((int)((float)(x + w + 2) - d4), y + h, x + w + 2, y + h + 2, finalColor);
        }
    }

    private static void renderPlacementHints(GuiGraphics gui, Minecraft mc, int width, int height) {
        if (!ClientPlacementHandler.isPlacing()) {
            return;
        }
        int uiWidth = 140;
        int uiHeight = 44;
        int xStart = (width - uiWidth) / 2;
        int yStart = height - uiHeight - 60;
        int goldLight = -10496;
        gui.m_280509_(xStart, yStart, xStart + uiWidth, yStart + uiHeight, -1879048192);
        gui.m_280509_(xStart, yStart, xStart + 2, yStart + uiHeight, goldLight);
        gui.m_280137_(mc.f_91062_, "Build Mode", xStart + uiWidth / 2, yStart + 4, goldLight);
        RenderSystem.enableBlend();
        int row1Y = yStart + 16;
        gui.m_280163_(MOUSE_LEFT, xStart + 8, row1Y, 0.0f, 0.0f, 12, 12, 12, 12);
        gui.m_280056_(mc.f_91062_, "Rotate", xStart + 26, row1Y + 2, -1, true);
        gui.m_280163_(ICON_ROTATE, xStart + uiWidth - 20, row1Y, 0.0f, 0.0f, 12, 12, 12, 12);
        int row2Y = yStart + 30;
        gui.m_280163_(MOUSE_RIGHT, xStart + 8, row2Y, 0.0f, 0.0f, 12, 12, 12, 12);
        gui.m_280056_(mc.f_91062_, "Confirm", xStart + 26, row2Y + 2, -1, true);
        gui.m_280163_(ICON_CONFIRM, xStart + uiWidth - 20, row2Y, 0.0f, 0.0f, 12, 12, 12, 12);
        RenderSystem.disableBlend();
    }

    private static void renderDownedUI(GuiGraphics gui, Minecraft mc, int width) {
        String currentKit = mc.f_91074_.getPersistentData().m_128461_("AAS_CurrentKit");
        boolean amIMedic = "Medic".equalsIgnoreCase(ClientData.myCurrentKit);
        long now = mc.f_91073_.m_46467_();
        int yOffset = 60;
        for (Integer id : ClientData.DOWNED_PLAYERS) {
            int distance;
            boolean isShouting;
            Player downed;
            Entity entity = mc.f_91073_.m_6815_(id.intValue());
            if (!(entity instanceof Player) || (downed = (Player)entity) == mc.f_91074_ || mc.f_91074_.m_5647_() == null || downed.m_5647_() == null || !mc.f_91074_.m_5647_().m_83536_(downed.m_5647_())) continue;
            long lastShout = downed.getPersistentData().m_128454_("AAS_LastMedicShoutTimeMS");
            boolean bl = isShouting = now - lastShout < 3000L;
            if (!amIMedic && !isShouting || (distance = (int)mc.f_91074_.m_20270_((Entity)downed)) >= 150) continue;
            String name = downed.m_6302_();
            String text = "\u271a " + name + " [" + distance + "m]";
            int x = width - mc.f_91062_.m_92895_(text) - 10;
            int bgColor = isShouting ? -1426128896 : -2136342528;
            gui.m_280509_(x - 2, yOffset - 1, width - 5, yOffset + 9, bgColor);
            gui.m_280056_(mc.f_91062_, text, x, yOffset, 0xFFFFFF, false);
            yOffset += 12;
        }
    }

    private static void renderRadioSpeakers(GuiGraphics gui, Minecraft mc, int height) {
        if (ClientData.RADIO_SPEAKERS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int yOffset = height / 2 - 40;
        int xOffset = 5;
        for (Map.Entry<String, Long> entry : new ArrayList<Map.Entry<String, Long>>(ClientData.RADIO_SPEAKERS.entrySet())) {
            if (now - entry.getValue() > 300L) {
                ClientData.RADIO_SPEAKERS.remove(entry.getKey());
                continue;
            }
            String speakerName = entry.getKey();
            int color = -256;
            int textWidth = mc.f_91062_.m_92895_(speakerName) + 15;
            gui.m_280509_(xOffset, yOffset - 2, xOffset + 5 + textWidth, yOffset + 10, Integer.MIN_VALUE);
            RenderSystem.enableBlend();
            float r = (float)(color >> 16 & 0xFF) / 255.0f;
            float g = (float)(color >> 8 & 0xFF) / 255.0f;
            float b = (float)(color & 0xFF) / 255.0f;
            RenderSystem.setShaderColor((float)r, (float)g, (float)b, (float)1.0f);
            gui.m_280163_(VOICE_ICON_RADIO_TEX, xOffset + 3, yOffset, 0.0f, 0.0f, 8, 8, 8, 8);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            gui.m_280056_(mc.f_91062_, speakerName, xOffset + 14, yOffset, color, false);
            yOffset += 14;
        }
    }

    private static void renderVoiceSpeakers(GuiGraphics gui, Minecraft mc, int height) {
        if (ClientData.SQUAD_SPEAKERS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int radioLines = ClientData.RADIO_SPEAKERS.size();
        int yOffset = height / 2 - 40 + radioLines * 14;
        int xOffset = 5;
        for (Map.Entry<String, Long> entry : new ArrayList<Map.Entry<String, Long>>(ClientData.SQUAD_SPEAKERS.entrySet())) {
            if (now - entry.getValue() > 300L) {
                ClientData.SQUAD_SPEAKERS.remove(entry.getKey());
                continue;
            }
            String speakerName = entry.getKey();
            int color = -11141291;
            int textWidth = mc.f_91062_.m_92895_(speakerName) + 15;
            gui.m_280509_(xOffset, yOffset - 2, xOffset + 5 + textWidth, yOffset + 10, Integer.MIN_VALUE);
            RenderSystem.enableBlend();
            float r = (float)(color >> 16 & 0xFF) / 255.0f;
            float g = (float)(color >> 8 & 0xFF) / 255.0f;
            float b = (float)(color & 0xFF) / 255.0f;
            RenderSystem.setShaderColor((float)r, (float)g, (float)b, (float)1.0f);
            gui.m_280163_(VOICE_ICON_TEX, xOffset + 3, yOffset, 0.0f, 0.0f, 8, 8, 8, 8);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            gui.m_280056_(mc.f_91062_, speakerName, xOffset + 14, yOffset, color, false);
            yOffset += 14;
        }
    }

    private static void renderHubMaterials(GuiGraphics gui, Minecraft mc, int width, int height) {
        BlockState state;
        BlockHitResult hit;
        if (mc.f_91077_ == null) {
            return;
        }
        int mats = -1;
        String team = "NEUTRAL";
        if (mc.f_91077_.m_6662_() == HitResult.Type.BLOCK) {
            hit = (BlockHitResult)mc.f_91077_;
            BlockPos pos = hit.m_82425_();
            state = mc.f_91073_.m_8055_(pos);
            BlockEntity be = mc.f_91073_.m_7702_(pos);
            if (be instanceof HubBlockEntity) {
                HubBlockEntity hub = (HubBlockEntity)be;
                if (state.m_61138_((Property)HubBlock.CONSTRUCTED) && !((Boolean)state.m_61143_((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
                    return;
                }
                mats = hub.getMaterials();
                team = hub.getTeam();
            }
        } else if (mc.f_91077_.m_6662_() == HitResult.Type.ENTITY && (state = (hit = (EntityHitResult)mc.f_91077_).m_82443_()) instanceof SupplyCrateEntity) {
            SupplyCrateEntity crate = (SupplyCrateEntity)state;
            mats = crate.getMaterials();
            team = crate.getTeamOwner();
        }
        if (mats != -1) {
            int teamColor = -1;
            if (team.equalsIgnoreCase("BLUE")) {
                teamColor = -11184641;
            } else if (team.equalsIgnoreCase("RED")) {
                teamColor = -43691;
            }
            String text = "Materials: " + mats;
            int textWidth = mc.f_91062_.m_92895_(text);
            int textX = (width - textWidth) / 2;
            int textY = height - 70;
            PoseStack pose = gui.m_280168_();
            pose.m_85836_();
            pose.m_252880_((float)width / 2.0f, (float)(textY - 12), 0.0f);
            pose.m_252781_(Axis.f_252403_.m_252977_(45.0f));
            gui.m_280509_(-5, -5, 5, 5, -16777216);
            gui.m_280509_(-4, -4, 4, 4, teamColor);
            pose.m_85849_();
            AASOverlay.drawOutlinedString(gui, mc, text, textX, textY, -22016);
        }
    }

    private static void renderBuildProgress(GuiGraphics gui, Minecraft mc, int width, int height) {
        boolean holdingTool;
        ItemStack mainHand = mc.f_91074_.m_21205_();
        ItemStack offHand = mc.f_91074_.m_21206_();
        boolean bl = holdingTool = mainHand.m_41720_() == ModItems.ENTRENCHING_TOOL.get() || offHand.m_41720_() == ModItems.ENTRENCHING_TOOL.get();
        if (!holdingTool) {
            return;
        }
        if (mc.f_91077_ == null || mc.f_91077_.m_6662_() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult blockHit = (BlockHitResult)mc.f_91077_;
        BlockEntity be = mc.f_91073_.m_7702_(blockHit.m_82425_());
        BlockState state = mc.f_91073_.m_8055_(blockHit.m_82425_());
        if (be == null) {
            return;
        }
        float progress = -1.0f;
        String structureTeam = "NEUTRAL";
        boolean finished = false;
        if (be instanceof HubBlockEntity) {
            HubBlockEntity hub = (HubBlockEntity)be;
            progress = hub.getPercentage();
            structureTeam = hub.getTeam();
            finished = (Boolean)state.m_61143_((Property)HubBlock.CONSTRUCTED);
        } else if (be instanceof WallBlockEntity) {
            WallBlockEntity wall = (WallBlockEntity)be;
            progress = wall.getPercentage();
            structureTeam = wall.getTeam();
            finished = (Boolean)state.m_61143_((Property)WallBlock.CONSTRUCTED);
        } else if (be instanceof BarbedWireBlockEntity) {
            BarbedWireBlockEntity wire = (BarbedWireBlockEntity)be;
            progress = wire.getPercentage();
            structureTeam = wire.getTeam();
            finished = (Boolean)state.m_61143_((Property)BarbedWireBlock.CONSTRUCTED);
        } else if (be instanceof M2ConstructionBlockEntity) {
            M2ConstructionBlockEntity m2 = (M2ConstructionBlockEntity)be;
            progress = m2.getPercentage();
            structureTeam = m2.getTeam();
        } else if (be instanceof AGSConstructionBlockEntity) {
            AGSConstructionBlockEntity ags = (AGSConstructionBlockEntity)be;
            progress = ags.getPercentage();
            structureTeam = ags.getTeam();
        } else if (be instanceof MortarConstructionBlockEntity) {
            MortarConstructionBlockEntity mortar = (MortarConstructionBlockEntity)be;
            progress = mortar.getPercentage();
            structureTeam = mortar.getTeam();
        } else if (be instanceof TOWConstructionBlockEntity) {
            TOWConstructionBlockEntity tow = (TOWConstructionBlockEntity)be;
            progress = tow.getPercentage();
            structureTeam = tow.getTeam();
        }
        if (progress < 0.0f) {
            return;
        }
        String playerTeam = mc.f_91074_.m_5647_() != null ? mc.f_91074_.m_5647_().m_5758_() : "NEUTRAL";
        boolean isEnemy = !structureTeam.equals("NEUTRAL") && !structureTeam.equalsIgnoreCase(playerTeam) && !mc.f_91074_.m_7500_();
        int uiWidth = 120;
        int uiHeight = isEnemy || finished ? 65 : 54;
        int xStart = width / 2 - uiWidth / 2;
        int yStart = height - 110;
        int goldLight = -10496;
        int goldDark = -4026112;
        gui.m_280509_(xStart, yStart, xStart + uiWidth, yStart + uiHeight, -1879048192);
        gui.m_280509_(xStart, yStart, xStart + 2, yStart + uiHeight, goldLight);
        RenderSystem.enableBlend();
        gui.m_280163_(BUILD_ICON, xStart + 8, yStart + 8, 0.0f, 0.0f, 12, 12, 12, 12);
        gui.m_280056_(mc.f_91062_, "Build", xStart + 26, yStart + 10, -1, true);
        gui.m_280163_(DIG_ICON, xStart + 8, yStart + 24, 0.0f, 0.0f, 12, 12, 12, 12);
        gui.m_280056_(mc.f_91062_, "Destroy", xStart + 26, yStart + 26, -1, true);
        int barX = xStart + 26;
        int barY = yStart + 42;
        int barWidth = 85;
        gui.m_280163_(SHOVEL_ICON, xStart + 8, yStart + 39, 0.0f, 0.0f, 12, 12, 12, 12);
        gui.m_280509_(barX, barY, barX + barWidth, barY + 5, 0x40FFFFFF);
        int currentBarWidth = (int)((float)barWidth * progress);
        if (currentBarWidth > 0) {
            gui.m_280024_(barX, barY, barX + currentBarWidth, barY + 5, goldDark, goldLight);
        }
        if (finished) {
            gui.m_280137_(mc.f_91062_, "Structure finished!", xStart + uiWidth / 2, yStart + 52, -256);
        } else if (isEnemy) {
            gui.m_280137_(mc.f_91062_, "Enemy structure!", xStart + uiWidth / 2, yStart + 52, -43691);
        }
        RenderSystem.disableBlend();
    }

    private static void renderTickets(GuiGraphics gui, Minecraft mc, int width) {
        if (!mc.f_91074_.m_7500_() && !mc.f_91074_.m_5833_()) {
            return;
        }
        int boxWidth = 32;
        int boxHeight = 18;
        int gap = 6;
        int topOffset = 5;
        int centerX = width / 2;
        boolean blinkOn = System.currentTimeMillis() / 500L % 2L == 0L;
        int normalWhite = -1;
        int alarmRed = -43691;
        int blueX = centerX - boxWidth - gap / 2;
        ResourceLocation blueFlag = AASOverlay.getFlagTexture(ClientData.BLUE_FACTION);
        gui.m_280509_(blueX - 1, topOffset - 1, blueX + boxWidth + 1, topOffset + boxHeight + 1, -16777216);
        if (blueFlag != null) {
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.enableBlend();
            gui.m_280411_(blueFlag, blueX, topOffset, boxWidth, boxHeight, 0.0f, 0.0f, boxWidth, boxHeight, boxWidth, boxHeight);
        } else {
            gui.m_280509_(blueX, topOffset, blueX + boxWidth, topOffset + boxHeight, -869046580);
        }
        String blueText = String.valueOf(ClientData.BLUE_TICKETS);
        int blueTextColor = ClientData.blueBleeding ? (blinkOn ? alarmRed : normalWhite) : normalWhite;
        int blueTextX = blueX + (boxWidth - mc.f_91062_.m_92895_(blueText)) / 2;
        int blueTextY = topOffset + (boxHeight - 8) / 2;
        AASOverlay.drawOutlinedString(gui, mc, blueText, blueTextX, blueTextY, blueTextColor);
        int redX = centerX + gap / 2;
        ResourceLocation redFlag = AASOverlay.getFlagTexture(ClientData.RED_FACTION);
        gui.m_280509_(redX - 1, topOffset - 1, redX + boxWidth + 1, topOffset + boxHeight + 1, -16777216);
        if (redFlag != null) {
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.enableBlend();
            gui.m_280411_(redFlag, redX, topOffset, boxWidth, boxHeight, 0.0f, 0.0f, boxWidth, boxHeight, boxWidth, boxHeight);
        } else {
            gui.m_280509_(redX, topOffset, redX + boxWidth, topOffset + boxHeight, -859032781);
        }
        String redText = String.valueOf(ClientData.RED_TICKETS);
        int redTextColor = ClientData.redBleeding ? (blinkOn ? alarmRed : normalWhite) : normalWhite;
        int redTextX = redX + (boxWidth - mc.f_91062_.m_92895_(redText)) / 2;
        int redTextY = topOffset + (boxHeight - 8) / 2;
        AASOverlay.drawOutlinedString(gui, mc, redText, redTextX, redTextY, redTextColor);
    }

    private static void drawOutlinedString(GuiGraphics gui, Minecraft mc, String text, int x, int y, int color) {
        int black = -16777216;
        gui.m_280056_(mc.f_91062_, text, x - 1, y, black, false);
        gui.m_280056_(mc.f_91062_, text, x + 1, y, black, false);
        gui.m_280056_(mc.f_91062_, text, x, y - 1, black, false);
        gui.m_280056_(mc.f_91062_, text, x, y + 1, black, false);
        gui.m_280056_(mc.f_91062_, text, x, y, color, false);
    }

    private static void renderCapturePoint(GuiGraphics gui, Minecraft mc, int width, int height) {
        if (!ClientData.isInsidePoint) {
            return;
        }
        int flagW = 48;
        int flagH = 27;
        int xStart = width - flagW - 15;
        int yStart = 20;
        int teamColor = -1;
        String faction = "none";
        if (ClientData.pointOwner.equals("BLUE")) {
            teamColor = -13408564;
            faction = ClientData.BLUE_FACTION;
        } else if (ClientData.pointOwner.equals("RED")) {
            teamColor = -3394765;
            faction = ClientData.RED_FACTION;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_85841_(0.8f, 0.8f, 1.0f);
        int scaledX = (int)((float)(xStart + flagW / 2) / 0.8f);
        int scaledY = (int)((float)(yStart - 9) / 0.8f);
        gui.m_280137_(mc.f_91062_, ClientData.pointName, scaledX, scaledY, -1);
        gui.m_280168_().m_85849_();
        int barStartX = xStart - 1;
        gui.m_280509_(barStartX, yStart - 1, xStart + flagW + 1, yStart + flagH + 1, -16777216);
        ResourceLocation flagTexture = AASOverlay.getFlagTexture(faction);
        if (flagTexture != null && !faction.equals("none")) {
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            gui.m_280411_(flagTexture, xStart, yStart, flagW, flagH, 0.0f, 0.0f, 64, 36, 64, 36);
        } else {
            gui.m_280509_(xStart, yStart, xStart + flagW, yStart + flagH, teamColor);
        }
        int barsY = yStart + flagH + 6;
        int segH = 4;
        int gap = 1;
        int[] segmentWidths = new int[]{12, 12, 12, 11};
        int currentX = barStartX;
        for (int i = 0; i < 4; ++i) {
            int sW = segmentWidths[i];
            gui.m_280509_(currentX, barsY, currentX + sW, barsY + segH, -1876811230);
            float threshold = (float)i * 0.25f;
            if (ClientData.pointProgress > threshold) {
                float boxFill = Math.min(1.0f, (ClientData.pointProgress - threshold) / 0.25f);
                int fillW = (int)((float)sW * boxFill);
                int fillColor = teamColor;
                if (ClientData.pointOwner.equals("NEUTRAL")) {
                    if (ClientData.pointCapturingTeam.equals("BLUE")) {
                        fillColor = -13408564;
                    } else if (ClientData.pointCapturingTeam.equals("RED")) {
                        fillColor = -3394765;
                    }
                }
                gui.m_280509_(currentX, barsY, currentX + fillW, barsY + segH, fillColor);
            }
            currentX += sW + gap;
        }
        int rate = ClientData.pointCaptureRate;
        if (rate != 0) {
            int absRate = Math.min(Math.abs(rate), 4);
            boolean isForward = rate > 0;
            String capTeam = ClientData.pointCapturingTeam;
            int arrowCol = -1;
            if (capTeam.equalsIgnoreCase("BLUE")) {
                arrowCol = -13408564;
            } else if (capTeam.equalsIgnoreCase("RED")) {
                arrowCol = -3394765;
            }
            float ar = (float)(arrowCol >> 16 & 0xFF) / 255.0f;
            float ag = (float)(arrowCol >> 8 & 0xFF) / 255.0f;
            float ab = (float)(arrowCol & 0xFF) / 255.0f;
            double baseX = isForward ? (double)(barStartX + 5) : (double)(barStartX + 45);
            double arrowY = (double)barsY + (double)segH / 2.0;
            for (int j = 0; j < absRate; ++j) {
                gui.m_280168_().m_85836_();
                double offsetX = isForward ? (double)(j * 6) : (double)(-j * 6);
                gui.m_280168_().m_85837_(baseX + offsetX, arrowY, 10.0);
                gui.m_280168_().m_85841_(1.05f, 1.05f, 1.0f);
                if (!isForward) {
                    gui.m_280168_().m_252781_(Axis.f_252403_.m_252977_(180.0f));
                }
                RenderSystem.setShaderColor((float)ar, (float)ag, (float)ab, (float)1.0f);
                gui.m_280163_(ARROW_TEX, -6, -6, 0.0f, 0.0f, 12, 12, 12, 12);
                gui.m_280168_().m_85849_();
            }
        }
        if (ClientData.isLocked) {
            gui.m_280168_().m_85836_();
            gui.m_280168_().m_85841_(0.7f, 0.7f, 1.0f);
            int lockX = (int)((float)barStartX / 0.71f);
            int lockY = (int)((float)(barsY + 10) / 0.7f);
            gui.m_280056_(mc.f_91062_, "BLOCKED", lockX, lockY, -43691, true);
            if (!ClientData.nextObjectiveName.isEmpty()) {
                gui.m_280056_(mc.f_91062_, "Need: " + ClientData.nextObjectiveName, lockX, lockY + 10, -3355444, true);
            }
            gui.m_280168_().m_85849_();
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private static void renderVotePanel(GuiGraphics gui, Minecraft mc, int width) {
        float speed = 0.05f;
        if ((ClientData.voteTransition = Mth.m_14179_((float)speed, (float)ClientData.voteTransition, (float)(ClientData.voteActive ? 1.0f : 0.0f))) <= 0.001f) {
            return;
        }
        int xPos = (int)Mth.m_14179_((float)ClientData.voteTransition, (float)-180.0f, (float)10.0f);
        int yPos = 60;
        String myTeam = mc.f_91074_.m_5647_() != null ? mc.f_91074_.m_5647_().m_5758_().toUpperCase() : "NEUTRAL";
        List<MapPlayerInfo> teamPlayers = ClientData.mapPlayers.values().stream().filter(info -> info.team.equalsIgnoreCase(myTeam)).toList();
        int panelWidth = 165;
        int rowHeight = 12;
        int headerHeight = 32;
        int statusHeight = 28;
        int footerHeight = 15;
        int panelHeight = headerHeight + statusHeight + teamPlayers.size() * rowHeight + footerHeight;
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_252880_(0.0f, 0.0f, 500.0f);
        gui.m_280509_(xPos, yPos, xPos + panelWidth, yPos + panelHeight, -1442840576);
        gui.m_280637_(xPos, yPos, panelWidth, panelHeight, -1);
        gui.m_280137_(mc.f_91062_, "VOTE TO START", xPos + panelWidth / 2, yPos + 5, -10496);
        int seconds = Math.max(0, ClientData.voteTimer);
        String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);
        gui.m_280137_(mc.f_91062_, timeStr, xPos + panelWidth / 2, yPos + 16, -1);
        gui.m_280509_(xPos + 5, yPos + 28, xPos + panelWidth - 5, yPos + 29, 0x55FFFFFF);
        String blueName = ClientData.customBlueName;
        String redName = ClientData.customRedName;
        int blueColor = ClientData.blueReady ? -11141291 : -43691;
        int redColor = ClientData.redReady ? -11141291 : -43691;
        gui.m_280056_(mc.f_91062_, blueName + ": " + (ClientData.blueReady ? "READY" : "WAITING"), xPos + 8, yPos + 32, blueColor, true);
        gui.m_280056_(mc.f_91062_, redName + ": " + (ClientData.redReady ? "READY" : "WAITING"), xPos + 8, yPos + 44, redColor, true);
        int currentY = yPos + headerHeight + statusHeight;
        for (MapPlayerInfo info2 : teamPlayers) {
            Boolean vote = ClientData.votes.get(info2.uuid);
            String icon = "\u25cb";
            int iconColor = -5592406;
            if (vote != null) {
                icon = vote != false ? "\u2714" : "\u2718";
                iconColor = vote != false ? -11141291 : -43691;
            }
            gui.m_280056_(mc.f_91062_, icon, xPos + 8, currentY, iconColor, true);
            int nameColor = info2.name.equals(mc.f_91074_.m_6302_()) ? -171 : -1;
            gui.m_280056_(mc.f_91062_, info2.name, xPos + 22, currentY, nameColor, true);
            currentY += rowHeight;
        }
        gui.m_280137_(mc.f_91062_, "F9: YES | F10: NO", xPos + panelWidth / 2, yPos + panelHeight - 12, -4473925);
        gui.m_280168_().m_85849_();
    }

    private static void renderProgressBars(GuiGraphics gui, Minecraft mc, int centerX, int y, int teamColor) {
        int barsTotalWidth = 80;
        int barHeight = 4;
        int barsStartX = centerX - barsTotalWidth / 2;
        int barGap = 2;
        int singleBarWidth = (barsTotalWidth - barGap * 3) / 4;
        for (int i = 0; i < 4; ++i) {
            int currentBarX = barsStartX + i * (singleBarWidth + barGap);
            gui.m_280509_(currentBarX, y, currentBarX + singleBarWidth, y + barHeight, -12303292);
            float threshold = (float)i * 0.25f;
            if (!(ClientData.pointProgress > threshold)) continue;
            float fillAmount = Math.min(1.0f, (ClientData.pointProgress - threshold) / 0.25f);
            int fillWidth = (int)((float)singleBarWidth * fillAmount);
            int finalBarColor = teamColor;
            if (ClientData.pointOwner.equals("NEUTRAL")) {
                if (ClientData.pointCapturingTeam.equals("BLUE")) {
                    finalBarColor = -13408564;
                } else if (ClientData.pointCapturingTeam.equals("RED")) {
                    finalBarColor = -3394765;
                }
            }
            gui.m_280509_(currentBarX, y, currentBarX + fillWidth, y + barHeight, finalBarColor);
        }
    }

    private static void renderCaptureArrows(GuiGraphics gui, Minecraft mc, int centerX, int y) {
        int rate = ClientData.pointCaptureRate;
        if (rate == 0) {
            return;
        }
        int absRate = Math.min(Math.abs(rate), 4);
        boolean isForward = rate > 0;
        String capTeam = ClientData.pointCapturingTeam;
        int color = -1;
        if (capTeam.equalsIgnoreCase("BLUE")) {
            color = -13408564;
        } else if (capTeam.equalsIgnoreCase("RED")) {
            color = -3394765;
        }
        RenderSystem.enableBlend();
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        RenderSystem.setShaderColor((float)r, (float)g, (float)b, (float)1.0f);
        int arrowSize = 12;
        int barsStartX = centerX - 40;
        int segmentWidth = 20;
        for (int i = 0; i < absRate; ++i) {
            int segmentIndex = isForward ? i : 3 - i;
            int xPos = barsStartX + segmentIndex * segmentWidth + 2;
            int yPos = y - 4;
            if (isForward) {
                gui.m_280163_(ARROW_TEX, xPos, yPos, 0.0f, 0.0f, arrowSize, arrowSize, arrowSize, arrowSize);
                continue;
            }
            gui.m_280168_().m_85836_();
            gui.m_280168_().m_85837_((double)xPos + (double)arrowSize / 2.0, (double)yPos + (double)arrowSize / 2.0, 0.0);
            gui.m_280168_().m_252781_(Axis.f_252403_.m_252977_(180.0f));
            gui.m_280163_(ARROW_TEX, -arrowSize / 2, -arrowSize / 2, 0.0f, 0.0f, arrowSize, arrowSize, arrowSize, arrowSize);
            gui.m_280168_().m_85849_();
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private static void renderSupplyTruckInfo(GuiGraphics gui, Minecraft mc, int width, int height) {
        Entity ridingEntity = mc.f_91074_.m_20202_();
        if (ridingEntity == null) {
            return;
        }
        Entity supplyTruck = AASOverlay.getSupplyTruckEntity(ridingEntity);
        if (supplyTruck != null) {
            int crates = supplyTruck.getPersistentData().m_128451_("AAS_SupplyAmmo");
            int maxCrates = (Integer)AASConfig.SUPPLY_TRUCK_CRATES.get();
            boolean isCharging = false;
            if (crates < maxCrates) {
                BlockPos vPos = supplyTruck.m_20183_();
                for (BlockPos pos : BlockPos.m_121940_((BlockPos)vPos.m_7918_(-5, -2, -5), (BlockPos)vPos.m_7918_(5, 2, 5))) {
                    if (!(mc.f_91073_.m_8055_(pos).m_60734_() instanceof MainSupplyBlock)) continue;
                    isCharging = true;
                    break;
                }
            }
            int color = -1;
            if (isCharging) {
                color = System.currentTimeMillis() / 250L % 2L == 0L ? -16711936 : -256;
            } else if (crates == 0) {
                color = -43691;
            }
            String text = "Supplies: " + crates + " / " + maxCrates;
            int textWidth = mc.f_91062_.m_92895_(text);
            int x = width - textWidth - 10;
            int y = height - 25;
            AASOverlay.drawOutlinedString(gui, mc, text, x, y, color);
            if (isCharging) {
                String reloadText = "RELOADING...";
                AASOverlay.drawOutlinedString(gui, mc, reloadText, width - mc.f_91062_.m_92895_(reloadText) - 10, y - 10, -11141291);
            }
        }
    }

    private static Entity getSupplyTruckEntity(Entity entity) {
        if (entity.getPersistentData().m_128471_("AAS_IsSupplyTruck")) {
            return entity;
        }
        Entity parent = entity.m_20202_();
        if (parent != null && parent.getPersistentData().m_128471_("AAS_IsSupplyTruck")) {
            return parent;
        }
        return null;
    }

    private static void renderVehicleAmmo(GuiGraphics gui, Minecraft mc, int width, int height) {
        Entity vehicle = mc.f_91074_.m_20202_();
        int currentAmmo = -1;
        int maxAmmo = -1;
        if (vehicle instanceof M2BrowningEntity) {
            M2BrowningEntity m2 = (M2BrowningEntity)vehicle;
            currentAmmo = m2.getAmmoCount();
            maxAmmo = 200;
        } else if (vehicle instanceof AGS30Entity) {
            AGS30Entity ags = (AGS30Entity)vehicle;
            currentAmmo = ags.getAmmoCount();
            maxAmmo = 30;
        }
        if (currentAmmo != -1) {
            String text = "Ammo: " + currentAmmo + " / " + maxAmmo;
            int x = 10;
            int y = height - 40;
            int color = currentAmmo == 0 ? -43691 : -1;
            AASOverlay.drawOutlinedString(gui, mc, text, x, y, color);
        }
    }

    private static void renderMedicUI(GuiGraphics gui, Minecraft mc, int width) {
        int yOffset = 60;
        for (Integer id : ClientData.DOWNED_PLAYERS) {
            int distance;
            Player downed;
            Entity entity = mc.f_91073_.m_6815_(id.intValue());
            if (!(entity instanceof Player) || (downed = (Player)entity) == mc.f_91074_ || (distance = (int)mc.f_91074_.m_20270_((Entity)downed)) >= 100) continue;
            String name = downed.m_6302_();
            String text = "\u271a " + name + " [" + distance + "m]";
            int x = width - mc.f_91062_.m_92895_(text) - 10;
            gui.m_280509_(x - 2, yOffset - 1, width - 5, yOffset + 9, -2130771968);
            gui.m_280056_(mc.f_91062_, text, x, yOffset, 0xFFFFFF, false);
            yOffset += 12;
        }
    }

    private static void renderSideMap(GuiGraphics gui, Minecraft mc, int screenWidth, int screenHeight, float partialTick) {
        if (HUD_SIDE_MAP == null) {
            HUD_SIDE_MAP = new AASMapRenderer();
        }
        float speed = 0.12f;
        float target = ClientData.isMapOpen ? 1.0f : 0.0f;
        ClientData.mapTransition = Mth.m_14179_((float)speed, (float)ClientData.mapTransition, (float)target);
        if (!ClientData.isMapOpen && ClientData.mapTransition < 0.001f) {
            ClientData.mapTransition = 0.0f;
            return;
        }
        int mapSize = (int)((float)(screenHeight - 60) / 1.3f);
        int topBarHeight = 35;
        int sidePadding = 15;
        int bottomPadding = 5;
        int containerWidth = mapSize + sidePadding * 2;
        int containerHeight = mapSize + topBarHeight + bottomPadding;
        float hiddenX = (float)screenWidth + 20.0f;
        float visibleX = (float)screenWidth - (float)containerWidth - 10.0f;
        int xPos = (int)Mth.m_14179_((float)ClientData.mapTransition, (float)hiddenX, (float)visibleX);
        int yPos = (screenHeight - containerHeight) / 2;
        gui.m_280509_(xPos, yPos, xPos + containerWidth, yPos + containerHeight, -1442840576);
        AASOverlay.renderMinimapStatus(gui, mc, xPos, yPos, containerWidth, topBarHeight);
        HUD_SIDE_MAP.init(xPos + sidePadding, yPos + topBarHeight, mapSize);
        HUD_SIDE_MAP.render(gui, -1, -1, partialTick);
    }

    private static void renderMinimapStatus(GuiGraphics gui, Minecraft mc, int x, int y, int containerWidth, int topBarHeight) {
        int tickets;
        String team = "NEUTRAL";
        if (mc.f_91074_.m_5647_() != null) {
            team = mc.f_91074_.m_5647_().m_5758_().toUpperCase();
        }
        int n = team.equals("BLUE") ? ClientData.BLUE_TICKETS : (tickets = team.equals("RED") ? ClientData.RED_TICKETS : 0);
        String faction = team.equals("BLUE") ? ClientData.BLUE_FACTION : (team.equals("RED") ? ClientData.RED_FACTION : "none");
        String tText = String.valueOf(tickets);
        int flagW = 22;
        int flagH = 13;
        int iconSize = 12;
        int textW = mc.f_91062_.m_92895_(tText);
        int gap = 6;
        int totalContentWidth = flagW + gap + iconSize + gap + textW;
        int startX = x + containerWidth / 2 - totalContentWidth / 2;
        int contentY = y + topBarHeight / 2 - flagH / 2;
        ResourceLocation flagTex = AASOverlay.getFlagTexture(faction);
        if (flagTex != null) {
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            gui.m_280411_(flagTex, startX, contentY, flagW, flagH, 0.0f, 0.0f, 64, 36, 64, 36);
        }
        ResourceLocation ticketIcon = new ResourceLocation("aas", "textures/gui/minimap_tickets.png");
        int iconX = startX + flagW + gap;
        RenderSystem.enableBlend();
        gui.m_280411_(ticketIcon, iconX, contentY, iconSize, iconSize, 0.0f, 0.0f, 16, 16, 16, 16);
        int textX = iconX + iconSize + gap;
        gui.m_280056_(mc.f_91062_, tText, textX, contentY + 2, -1, true);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private static ResourceLocation getFlagTexture(String faction) {
        if (faction == null) {
            return null;
        }
        switch (faction.toLowerCase()) {
            case "ukraine": {
                return FLAG_UKRAINE;
            }
            case "russia": {
                return FLAG_RUSSIA;
            }
            case "usa": {
                return FLAG_USA;
            }
            case "nato": {
                return FLAG_NATO;
            }
            case "bluefor": {
                return FLAG_BLUEFOR;
            }
            case "redfor": {
                return FLAG_REDFOR;
            }
            case "insurgency": {
                return FLAG_INSURGENCY;
            }
            case "pmc": {
                return FLAG_PMC;
            }
        }
        return null;
    }

    static {
        MOUSE_LEFT = new ResourceLocation("aas", "textures/gui/dig_icon.png");
        MOUSE_RIGHT = new ResourceLocation("aas", "textures/gui/build_icon.png");
        ICON_ROTATE = new ResourceLocation("aas", "textures/gui/icon_rotate.png");
        ICON_CONFIRM = new ResourceLocation("aas", "textures/gui/icon_confirm.png");
        VOICE_ICON_TEX = new ResourceLocation("aas", "textures/gui/voice_icon.png");
        VOICE_ICON_RADIO_TEX = new ResourceLocation("aas", "textures/gui/voice_icon_radio.png");
    }
}

