package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.block.AGSConstructionBlock;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlock;
import com.pigeostudios.pwp.warfare.block.HubBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.M2ConstructionBlock;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.MortarConstructionBlock;
import com.pigeostudios.pwp.warfare.block.RebConstructionBlock;
import com.pigeostudios.pwp.warfare.block.TOWConstructionBlock;
import com.pigeostudios.pwp.warfare.block.VehicleStationBlock;
import com.pigeostudios.pwp.warfare.block.WallBlock;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.PacketBuildRequest;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.InputEvent.MouseButton.Pre;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.client.event.ScreenEvent.Opening;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT)
// Обработчик режима размещения построек на клиенте
// Отвечает за предпросмотр, вращение и подтверждение установки
public class ClientPlacementHandler {
   private static boolean isPlacing = false;
   private static int structureId = -1;
   private static float rotationY = 0.0F;

   public static boolean isPlacing() {
      return isPlacing;
   }

   public static void startPlacing(int id) {
      isPlacing = true;
      structureId = id;
      if (structureId >= 20 && structureId <= 23) {
         rotationY = 90.0F;
      } else {
         rotationY = 0.0F;
      }

      Minecraft.getInstance().setScreen(null);
   }

   public static void stopPlacing() {
      isPlacing = false;
      structureId = -1;
   }

   @SubscribeEvent
   public static void onScreenOpen(Opening event) {
      if (isPlacing && event.getScreen() instanceof PauseScreen) {
         event.setCanceled(true);
         stopPlacing();
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (isPlacing && event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            boolean holdingRadio = mc.player.getMainHandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get()
               || mc.player.getOffhandItem().getItem() == ModItems.SQUAD_LEADER_RADIO.get();
            if (!holdingRadio) {
               stopPlacing();
            } else {
               HitResult hit = mc.hitResult;
               if (hit != null && hit.getType() == Type.BLOCK) {
                  BlockHitResult blockHit = (BlockHitResult)hit;
                  BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
                  boolean isValid = validatePlacement(mc, placePos);
                  PoseStack pose = event.getPoseStack();
                  pose.pushPose();
                  Vec3 camPos = event.getCamera().getPosition();
                  pose.translate(placePos.getX() - camPos.x, placePos.getY() - camPos.y, placePos.getZ() - camPos.z);
                  pose.translate(0.5, 0.0, 0.5);
                  pose.mulPose(Axis.YP.rotationDegrees(rotationY));
                  pose.translate(-0.5, 0.0, -0.5);
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  if (isValid) {
                     RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.6F);
                  } else {
                     RenderSystem.setShaderColor(1.0F, 0.5F, 0.5F, 0.6F);
                  }

                  try {
                     if (structureId == 20) {
                        BlockState m2State = (BlockState)((BlockState)((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get())
                              .defaultBlockState()
                              .setValue(M2ConstructionBlock.FACING, Direction.NORTH))
                           .setValue(M2ConstructionBlock.VALID, isValid);
                        renderGhostBlock(mc, m2State, pose, 0, 0, 0);
                     } else if (structureId == 21) {
                        BlockState agsState = (BlockState)((BlockState)((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get())
                              .defaultBlockState()
                              .setValue(AGSConstructionBlock.FACING, Direction.NORTH))
                           .setValue(AGSConstructionBlock.VALID, isValid);
                        renderGhostBlock(mc, agsState, pose, 0, 0, 0);
                     } else if (structureId == 22) {
                        BlockState mortarState = (BlockState)((BlockState)((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get())
                              .defaultBlockState()
                              .setValue(MortarConstructionBlock.FACING, Direction.NORTH))
                           .setValue(MortarConstructionBlock.VALID, isValid);
                        renderGhostBlock(mc, mortarState, pose, 0, 0, 0);
                     } else if (structureId == 23) {
                        BlockState towState = (BlockState)((BlockState)((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get())
                              .defaultBlockState()
                              .setValue(TOWConstructionBlock.FACING, Direction.NORTH))
                           .setValue(TOWConstructionBlock.VALID, isValid);
                        renderGhostBlock(mc, towState, pose, 0, 0, 0);
                     } else if (structureId == 13) {
                        BlockState wireState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.BARBED_WIRE_BLOCK.get())
                                 .defaultBlockState()
                                 .setValue(BarbedWireBlock.FACING, Direction.NORTH))
                              .setValue(BarbedWireBlock.CONSTRUCTED, false))
                           .setValue(BarbedWireBlock.VALID, isValid);

                        for (int x = 0; x < 3; x++) {
                           renderGhostBlock(mc, wireState, pose, x, 0, 0);
                        }
                     } else if (structureId == 15) {
                        BlockState wallState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get())
                                 .defaultBlockState()
                                 .setValue(WallBlock.FACING, Direction.NORTH))
                              .setValue(WallBlock.CONSTRUCTED, false))
                           .setValue(WallBlock.VALID, isValid);
                        BlockState wireState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.BARBED_WIRE_BLOCK.get())
                                 .defaultBlockState()
                                 .setValue(BarbedWireBlock.FACING, Direction.NORTH))
                              .setValue(BarbedWireBlock.CONSTRUCTED, false))
                           .setValue(BarbedWireBlock.VALID, isValid);

                        for (int x = -1; x <= 1; x++) {
                           renderGhostBlock(mc, wallState, pose, x, 0, 0);
                           renderGhostBlock(mc, wallState, pose, x, 0, -1);
                           renderGhostBlock(mc, wallState, pose, x, 1, -1);
                           renderGhostBlock(mc, wireState, pose, x, 0, -2);
                        }
                     } else if (structureId == 16) {
                        BlockState wallState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get())
                                 .defaultBlockState()
                                 .setValue(WallBlock.FACING, Direction.NORTH))
                              .setValue(WallBlock.CONSTRUCTED, false))
                           .setValue(WallBlock.VALID, isValid);
                        BlockState slabState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_SLAB_BLOCK.get())
                                 .defaultBlockState()
                                 .setValue(WallBlock.FACING, Direction.NORTH))
                              .setValue(WallBlock.CONSTRUCTED, false))
                           .setValue(WallBlock.VALID, isValid);

                        for (int x = -1; x <= 1; x++) {
                           for (int y = 0; y < 3; y++) {
                              renderGhostBlock(mc, x == 0 && y == 1 ? slabState : wallState, pose, x, y, 0);
                           }
                        }
                     } else if (structureId == 17) {
                        BlockState wallState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get())
                                 .defaultBlockState()
                                 .setValue(WallBlock.FACING, Direction.NORTH))
                              .setValue(WallBlock.CONSTRUCTED, false))
                           .setValue(WallBlock.VALID, isValid);
                        BlockState slabState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_SLAB_BLOCK.get())
                                 .defaultBlockState()
                                 .setValue(WallBlock.FACING, Direction.NORTH))
                              .setValue(WallBlock.CONSTRUCTED, false))
                           .setValue(WallBlock.VALID, isValid);

                        for (int x = -1; x <= 1; x++) {
                           for (int y = 0; y <= 2; y++) {
                              for (int z = -1; z <= 1; z++) {
                                 if (x == 0 && z == 0 && y < 2) {
                                    continue;
                                 }
                                 renderGhostBlock(mc, y == 2 ? slabState : wallState, pose, x, y, z);
                              }
                           }
                        }
      } else if (structureId == 18) {
         BlockState stationState = (BlockState)((BlockState)((Block)ModBlocks.VEHICLE_STATION_BLOCK.get())
               .defaultBlockState()
               .setValue(VehicleStationBlock.FACING, Direction.NORTH))
            .setValue(VehicleStationBlock.VALID, isValid);
         renderGhostBlock(mc, stationState, pose, 0, 0, 0);
      } else if (structureId == 24 || structureId == 25) {
         BlockState rebState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.REB_CONSTRUCTION_BLOCK.get())
                  .defaultBlockState()
                  .setValue(RebConstructionBlock.FACING, Direction.NORTH))
               .setValue(RebConstructionBlock.VALID, isValid))
            .setValue(RebConstructionBlock.MINI, structureId == 25);
         renderGhostBlock(mc, rebState, pose, 0, 0, 0);
      } else {
                        BlockState wallState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.WALL_BLOCK.get())
                                 .defaultBlockState()
                                 .setValue(WallBlock.FACING, Direction.NORTH))
                              .setValue(WallBlock.CONSTRUCTED, false))
                           .setValue(WallBlock.VALID, isValid);
                        if (structureId == 11) {
                           renderGhostBlock(mc, wallState, pose, 0, 0, 0);
                           renderGhostBlock(mc, wallState, pose, 1, 0, 0);
                           renderGhostBlock(mc, wallState, pose, 0, 1, 0);
                           renderGhostBlock(mc, wallState, pose, 1, 1, 0);
                        } else if (structureId == 12) {
                           for (int x = 0; x < 3; x++) {
                              for (int y = 0; y < 3; y++) {
                                 renderGhostBlock(mc, wallState, pose, x, y, 0);
                              }
                           }
                        } else {
                           renderGhostBlock(mc, wallState, pose, 0, 0, 0);
                        }
                     }
                  } catch (Exception var12) {
                  }

                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  pose.popPose();
               }
            }
         }
      }
   }

   private static void renderGhostBlock(Minecraft mc, BlockState state, PoseStack pose, int xOff, int yOff, int zOff) {
      pose.pushPose();
      pose.translate(xOff, yOff, zOff);
      mc.getBlockRenderer().renderSingleBlock(state, pose, mc.renderBuffers().bufferSource(), 15728880, OverlayTexture.NO_OVERLAY);
      mc.renderBuffers().bufferSource().endBatch(ItemBlockRenderTypes.getChunkRenderType(state));
      pose.popPose();
   }

   @SubscribeEvent
   public static void onMouseInput(Pre event) {
      if (isPlacing) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.screen == null) {
            if (event.getAction() == 1) {
               if (event.getButton() == 0) {
                  event.setCanceled(true);
                  if (structureId >= 20 && structureId <= 23) {
                     return;
                  }

                  rotationY -= 90.0F;
                  if (rotationY <= -360.0F) {
                     rotationY = 0.0F;
                  }
               } else if (event.getButton() == 1) {
                  HitResult hit = mc.hitResult;
                  if (hit != null && hit.getType() == Type.BLOCK) {
                     BlockHitResult blockHit = (BlockHitResult)hit;
                     BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
                     if (validatePlacement(mc, placePos)) {
                        PacketHandler.INSTANCE.sendToServer(new PacketBuildRequest(structureId, placePos, (int)rotationY));
                        stopPlacing();
                     } else {
                         mc.player.displayClientMessage(Component.literal("Недостаточно материалов или вне досягаемости!").withStyle(ChatFormatting.RED), true);
                     }
                  }

                  event.setCanceled(true);
               }
            }
         }
      }
   }

   private static boolean validatePlacement(Minecraft mc, BlockPos pos) {
      if (mc.player == null) {
         return false;
      }

      // Место занято (или любая часть multi-структуры занята) — макет красный,
      // как серверный canPlaceAt. Без этого макет зелёный поверх построек,
      // а сервер потом отвечает «Место занято!».
      if (!allPartsFree(mc, pos)) {
         return false;
      }

      String playerTeam = "NEUTRAL";
      if (mc.player.getTeam() != null) {
         playerTeam = mc.player.getTeam().getName();
      }

      if (structureId == 14 && (Boolean)WarfareConfig.HUB_PLACEMENT_REQUIRES_CRATE.get() && !mc.player.isCreative()) {
         double crateRad = 50.0;
         AABB area = new AABB(pos).inflate(crateRad);
         List<SupplyCrateEntity> nearbyCrates = mc.level.getEntitiesOfClass(SupplyCrateEntity.class, area);
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
         cost = 300;
      }

      if (structureId == 23) {
         cost = 200;
      }

      if (structureId == 15) {
         cost = 25;
      }

      if (structureId == 16) {
         cost = 20;
      }

      if (structureId == 17) {
         cost = 50;
      }

      if (structureId == 18) {
         cost = 100;
      }

      if (structureId == 24) {
         cost = (Integer)WarfareConfig.REB_BUILD_COST.get();
      }

      if (structureId == 25) {
         cost = (Integer)WarfareConfig.REB_MINI_BUILD_COST.get();
      }

      if (playerTeam.equals("NEUTRAL") && !mc.player.isCreative()) {
         return false;
      }

      String currentDimension = mc.level.dimension().location().toString();
      int hubRadius = (Integer)WarfareConfig.HUB_BUILD_RADIUS.get();
      double maxDistSqHub = hubRadius * hubRadius;
      int crateRadius = (Integer)WarfareConfig.CRATE_BUILD_RADIUS.get();
      double maxDistSqCrate = crateRadius * crateRadius;
      int totalMaterials = 0;
      boolean isInRange = false;
      if (ClientData.clientHubs != null) {
         for (WarfareWorldData.HubInfo hubInfo : ClientData.clientHubs) {
            if (hubInfo.dimension == null || hubInfo.dimension.equals(currentDimension)) {
               BlockPos hubPos = hubInfo.pos;
                if (hubPos.distSqr(pos) <= maxDistSqHub && mc.level.isLoaded(hubPos)) {
                   BlockEntity be = mc.level.getBlockEntity(hubPos);
                   if (be instanceof HubBlockEntity hub
                      && (hub.getTeam().equalsIgnoreCase(playerTeam) || hub.getTeam().equals("NEUTRAL"))) {
                      isInRange = true;
                      totalMaterials += hub.getMaterials();
                   }
                }
            }
         }
      }

      AABB searchArea = new AABB(pos).inflate(crateRadius);

      for (SupplyCrateEntity crate : mc.level.getEntitiesOfClass(SupplyCrateEntity.class, searchArea)) {
         if ((crate.getTeamOwner().equalsIgnoreCase(playerTeam) || crate.getTeamOwner().equals("NEUTRAL"))
            && crate.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= maxDistSqCrate) {
            isInRange = true;
            totalMaterials += crate.getMaterials();
         }
      }

      if (!isInRange) {
         return false;
      } else {
         return mc.player.isCreative() ? true : totalMaterials >= cost;
      }
   }

   // Проверка занятости: базовая позиция и все части multi-структур
   // (как серверный canPlaceAt). Части считаются с учётом поворота —
   // та же формула, что в PacketBuildRequest.getRelativePos.
   private static boolean allPartsFree(Minecraft mc, BlockPos pos) {
      java.util.List<BlockPos> parts = new java.util.ArrayList<>();
      parts.add(pos);
      int rot = (int) rotationY;
      switch (structureId) {
         case 11 -> {
            parts.add(pos.above());
            parts.add(rel(pos, rot, 1, 0, 0));
            parts.add(rel(pos, rot, 1, 1, 0));
         }
         case 12 -> {
            for (int x = 0; x < 3; x++) {
               for (int y = 0; y < 3; y++) {
                  parts.add(rel(pos, rot, x, y, 0));
               }
            }
         }
         case 13 -> {
            for (int h = 0; h < 3; h++) {
               parts.add(rel(pos, rot, h, 0, 0));
            }
         }
         case 15 -> {
            for (int x = -1; x <= 1; x++) {
               parts.add(rel(pos, rot, x, 0, 0));
               parts.add(rel(pos, rot, x, 0, -1));
               parts.add(rel(pos, rot, x, 1, -1));
               parts.add(rel(pos, rot, x, 0, -2));
            }
         }
         case 16 -> {
            for (int x = -1; x <= 1; x++) {
               for (int y = 0; y < 3; y++) {
                  parts.add(rel(pos, rot, x, y, 0));
               }
            }
         }
         case 17 -> {
            for (int x = -1; x <= 1; x++) {
               for (int y = 0; y <= 2; y++) {
                  for (int z = -1; z <= 1; z++) {
                     if (x == 0 && z == 0 && y < 2) continue;
                     parts.add(rel(pos, rot, x, y, z));
                  }
               }
            }
         }
         default -> {
         }
      }

      for (BlockPos p : parts) {
         if (!mc.level.getBlockState(p).canBeReplaced()) {
            return false;
         }
      }
      return true;
   }

   private static BlockPos rel(BlockPos base, int rotation, int xOff, int yOff, int zOff) {
      int rot = (rotation % 360 + 360) % 360;
      if (rot == 90) return base.offset(zOff, yOff, -xOff);
      if (rot == 180) return base.offset(-xOff, yOff, -zOff);
      if (rot == 270) return base.offset(-zOff, yOff, xOff);
      return base.offset(xOff, yOff, zOff);
   }
}
