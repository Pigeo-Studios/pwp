/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.PauseScreen
 *  net.minecraft.client.renderer.ItemBlockRenderTypes
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.InputEvent$MouseButton$Pre
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.client.event.ScreenEvent$Opening
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 */
package com.example.aas.client;

import com.example.aas.block.AGSConstructionBlock;
import com.example.aas.block.BarbedWireBlock;
import com.example.aas.block.HubBlock;
import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.M2ConstructionBlock;
import com.example.aas.block.ModBlocks;
import com.example.aas.block.MortarConstructionBlock;
import com.example.aas.block.TOWConstructionBlock;
import com.example.aas.block.WallBlock;
import com.example.aas.client.ClientData;
import com.example.aas.config.AASConfig;
import com.example.aas.entity.SupplyCrateEntity;
import com.example.aas.item.ModItems;
import com.example.aas.network.PacketBuildRequest;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="aas", value={Dist.CLIENT})
public class ClientPlacementHandler {
    static private boolean isPlacing = false;
    static private int structureId = -1;
    static private float rotationY = 0.0f;

    public static boolean isPlacing() {
        return isPlacing;
    }

    public static void startPlacing(int id) {
        isPlacing = true;
        structureId = id;
        rotationY = structureId >= 20 && structureId <= 23 ? 90.0f : 0.0f;
        Minecraft.getInstance().setScreen(null);
    }

    public static void stopPlacing() {
        isPlacing = false;
        structureId = -1;
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (isPlacing && event.getScreen() instanceof PauseScreen) {
            event.setCanceled(true);
            ClientPlacementHandler.stopPlacing();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        boolean holdingRadio;
        if (!isPlacing || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        boolean bl = holdingRadio = mc.player.getMainHandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get() || mc.player.getOffhandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get();
        if (!holdingRadio) {
            ClientPlacementHandler.stopPlacing();
            return;
        }
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult blockHit = (BlockHitResult)hit;
        BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
        boolean isValid = ClientPlacementHandler.validatePlacement(mc, placePos);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        Vec3 camPos = event.getCamera().getPosition();
        pose.translate((double)placePos.getX() - camPos.x, (double)placePos.getY() - camPos.y, (double)placePos.getZ() - camPos.z);
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(rotationY));
        pose.translate(-0.5, 0.0, -0.5);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (isValid) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.6f);
        } else {
            RenderSystem.setShaderColor(1.0f, 0.5f, 0.5f, 0.6f);
        }
        try {
            if (structureId == 20) {
                BlockState m2State = (BlockState)((BlockState)((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)M2ConstructionBlock.FACING, (Comparable)Direction.NORTH)).setValue((Property)M2ConstructionBlock.VALID, (Comparable)Boolean.valueOf(isValid));
                ClientPlacementHandler.renderGhostBlock(mc, m2State, pose, 0, 0, 0);
            } else if (structureId == 21) {
                BlockState agsState = (BlockState)((BlockState)((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)AGSConstructionBlock.FACING, (Comparable)Direction.NORTH)).setValue((Property)AGSConstructionBlock.VALID, (Comparable)Boolean.valueOf(isValid));
                ClientPlacementHandler.renderGhostBlock(mc, agsState, pose, 0, 0, 0);
            } else if (structureId == 22) {
                BlockState mortarState = (BlockState)((BlockState)((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)MortarConstructionBlock.FACING, (Comparable)Direction.NORTH)).setValue((Property)MortarConstructionBlock.VALID, (Comparable)Boolean.valueOf(isValid));
                ClientPlacementHandler.renderGhostBlock(mc, mortarState, pose, 0, 0, 0);
            } else if (structureId == 23) {
                BlockState towState = (BlockState)((BlockState)((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue((Property)TOWConstructionBlock.FACING, (Comparable)Direction.NORTH)).setValue((Property)TOWConstructionBlock.VALID, (Comparable)Boolean.valueOf(isValid));
                ClientPlacementHandler.renderGhostBlock(mc, towState, pose, 0, 0, 0);
            } else if (structureId == 13) {
                BlockState wireState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.BARBED_WIRE_BLOCK.get()).defaultBlockState().setValue((Property)BarbedWireBlock.FACING, (Comparable)Direction.NORTH)).setValue((Property)BarbedWireBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(false))).setValue((Property)BarbedWireBlock.VALID, (Comparable)Boolean.valueOf(isValid));
                for (int x = 0; x < 3; ++x) {
                    ClientPlacementHandler.renderGhostBlock(mc, wireState, pose, x, 0, 0);
                }
            } else {
                BlockState wallState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get()).defaultBlockState().setValue((Property)WallBlock.FACING, (Comparable)Direction.NORTH)).setValue((Property)WallBlock.CONSTRUCTED, (Comparable)Boolean.valueOf(false))).setValue((Property)WallBlock.VALID, (Comparable)Boolean.valueOf(isValid));
                if (structureId == 11) {
                    ClientPlacementHandler.renderGhostBlock(mc, wallState, pose, 0, 0, 0);
                    ClientPlacementHandler.renderGhostBlock(mc, wallState, pose, 1, 0, 0);
                    ClientPlacementHandler.renderGhostBlock(mc, wallState, pose, 0, 1, 0);
                    ClientPlacementHandler.renderGhostBlock(mc, wallState, pose, 1, 1, 0);
                } else if (structureId == 12) {
                    for (int x = 0; x < 3; ++x) {
                        for (int y = 0; y < 3; ++y) {
                            ClientPlacementHandler.renderGhostBlock(mc, wallState, pose, x, y, 0);
                        }
                    }
                } else {
                    ClientPlacementHandler.renderGhostBlock(mc, wallState, pose, 0, 0, 0);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        pose.popPose();
    }

    private static void renderGhostBlock(Minecraft mc, BlockState state, PoseStack pose, int xOff, int yOff, int zOff) {
        pose.pushPose();
        pose.translate((float)xOff, (float)yOff, (float)zOff);
        mc.getBlockRenderer().renderSingleBlock(state, pose, (MultiBufferSource)mc.renderBuffers().bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY);
        mc.renderBuffers().bufferSource().endBatch(ItemBlockRenderTypes.getChunkRenderType((BlockState)state));
        pose.popPose();
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (!isPlacing) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        if (event.getAction() == 1) {
            if (event.getButton() == 0) {
                event.setCanceled(true);
                if (structureId >= 20 && structureId <= 23) {
                    return;
                }
                if ((rotationY -= 90.0f) <= -360.0f) {
                    rotationY = 0.0f;
                }
            } else if (event.getButton() == 1) {
                HitResult hit = mc.hitResult;
                if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult)hit;
                    BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
                    if (ClientPlacementHandler.validatePlacement(mc, placePos)) {
                        PacketHandler.INSTANCE.sendToServer((Object)new PacketBuildRequest(structureId, placePos, (int)rotationY));
                        ClientPlacementHandler.stopPlacing();
                    } else {
                        mc.player.displayClientMessage((Component)Component.literal((String)"Not enough materials or out of range!").withStyle(ChatFormatting.RED), true);
                    }
                }
                event.setCanceled(true);
            }
        }
    }

    private static boolean validatePlacement(Minecraft mc, BlockPos pos) {
        if (mc.player == null) {
            return false;
        }
        String playerTeam = "NEUTRAL";
        if (mc.player.getTeam() != null) {
            playerTeam = mc.player.getTeam().getName();
        }
        if (structureId == 14 && ((Boolean)AASConfig.HUB_PLACEMENT_REQUIRES_CRATE.get()).booleanValue() && !mc.player.isCreative()) {
            double crateRad = 50.0;
            AABB area = new AABB(pos).inflate(crateRad);
            List nearbyCrates = mc.level.getEntitiesOfClass(SupplyCrateEntity.class, area);
            String team = playerTeam;
            boolean hasValidCrate = nearbyCrates.stream().anyMatch(c -> c.getTeamOwner().equals("NEUTRAL") || c.getTeamOwner().equalsIgnoreCase(team));
            if (!hasValidCrate) {
                return false;
            }
        }
        int cost = 5;
        if (structureId == 11) {
            cost = 10;
        }
        if (structureId == 12) {
            cost = 15;
        }
        if (structureId == 13) {
            cost = 25;
        }
        if (structureId == 20) {
            cost = 100;
        }
        if (structureId == 21) {
            cost = 100;
        }
        if (structureId == 22) {
            cost = 200;
        }
        if (structureId == 23) {
            cost = 200;
        }
        if (playerTeam.equals("NEUTRAL") && !mc.player.isCreative()) {
            return false;
        }
        String currentDimension = mc.level.dimension().location().toString();
        int hubRadius = (Integer)AASConfig.HUB_BUILD_RADIUS.get();
        double maxDistSqHub = hubRadius * hubRadius;
        int crateRadius = (Integer)AASConfig.CRATE_BUILD_RADIUS.get();
        double maxDistSqCrate = crateRadius * crateRadius;
        int totalMaterials = 0;
        boolean isInRange = false;
        if (ClientData.clientHubs != null) {
            for (AASWorldData.HubInfo hubInfo : ClientData.clientHubs) {
                HubBlockEntity hub;
                BlockPos hubPos;
                if (hubInfo.dimension != null && !hubInfo.dimension.equals(currentDimension) || !((hubPos = hubInfo.pos).distSqr((Vec3i)pos) <= maxDistSqHub) || !mc.level.isLoaded(hubPos)) continue;
                BlockEntity be = mc.level.getBlockEntity(hubPos);
                BlockState hubState = mc.level.getBlockState(hubPos);
                if (!(be instanceof HubBlockEntity) || !(hub = (HubBlockEntity)be).getTeam().equalsIgnoreCase(playerTeam) && !hub.getTeam().equals("NEUTRAL") || hubState.hasProperty((Property)HubBlock.CONSTRUCTED) && !((Boolean)hubState.getValue((Property)HubBlock.CONSTRUCTED)).booleanValue()) continue;
                isInRange = true;
                totalMaterials += hub.getMaterials();
            }
        }
        AABB searchArea = new AABB(pos).inflate((double)crateRadius);
        List crates = mc.level.getEntitiesOfClass(SupplyCrateEntity.class, searchArea);
        for (SupplyCrateEntity crate : crates) {
            if (!crate.getTeamOwner().equalsIgnoreCase(playerTeam) && !crate.getTeamOwner().equals("NEUTRAL") || !(crate.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) <= maxDistSqCrate)) continue;
            isInRange = true;
            totalMaterials += crate.getMaterials();
        }
        if (!isInRange) {
            return false;
        }
        if (mc.player.isCreative()) {
            return true;
        }
        return totalMaterials >= cost;
    }
}

