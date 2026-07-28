package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import com.pigeostudios.pwp.warfare.client.ClientSkinManager;
import com.pigeostudios.pwp.warfare.client.gui.DeployScreen;
import com.pigeostudios.pwp.warfare.client.gui.DownedScreen;
import com.pigeostudios.pwp.warfare.client.gui.SquadMapScreen;
import com.pigeostudios.pwp.warfare.client.gui.SquadSelectionScreen;
import com.pigeostudios.pwp.warfare.client.gui.TeamSelectionScreen;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import com.pigeostudios.pwp.warfare.item.SupplyTruckMarkerItem;
import com.pigeostudios.pwp.warfare.item.VehicleMarkerItem;
import com.pigeostudios.pwp.warfare.network.PacketApplyMarker;
import com.pigeostudios.pwp.warfare.network.PacketCMDVote;
import com.pigeostudios.pwp.warfare.network.PacketVoiceChannelState;
import com.pigeostudios.pwp.warfare.network.PacketConfirmArtStrike;
import com.pigeostudios.pwp.warfare.network.PacketDebugFill;
import com.pigeostudios.pwp.warfare.network.PacketDropCrate;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlacePing;
import com.pigeostudios.pwp.warfare.network.PacketToggleAim;
import com.pigeostudios.pwp.warfare.network.PacketVehicleShoot;
import com.pigeostudios.pwp.warfare.network.PacketVoteAction;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
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
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import net.minecraftforge.client.event.InputEvent.MouseButton.Pre;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.client.event.ScreenEvent.Opening;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT)
// РћР±СЂР°Р±РѕС‚С‡РёРє РєР»РёРµРЅС‚СЃРєРёС… СЃРѕР±С‹С‚РёР№ Forge: РІРІРѕРґ СЃ РєР»Р°РІРёР°С‚СѓСЂС‹/РјС‹С€Рё,
// СЂРµРЅРґРµСЂ РјР°СЂРєРµСЂРѕРІ РІ 3D-РјРёСЂРµ, FOV Рё РѕР±СЂР°Р±РѕС‚РєР° РєР°РјРµСЂС‹ РїСЂРё РЅРѕРєР°СѓС‚Рµ
public class ClientEvents {
   private static final ResourceLocation PING_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye.png");
   private static final ResourceLocation MOVE_TEXTURE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move.png");
   private static final ResourceLocation MOVE_TEX_BRAVO = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move_bravo.png");
   private static final ResourceLocation PING_TEX_BRAVO = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye_bravo.png");
   private static final ResourceLocation MOVE_TEX_CHARLIE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move_charlie.png");
   private static final ResourceLocation PING_TEX_CHARLIE = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye_charlie.png");
   private static boolean lastSquadState = false;
   private static boolean lastCommandState = false;
   private static float downedCameraProgress = 0.0F;
   private static float initialPitchOnFall = 0.0F;
   private static boolean wasDownedLastFrame = false;

   @SubscribeEvent
   public static void onChatReceived(ClientChatReceivedEvent event) {
      ClientData.addChatMessage(event.getMessage());
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            if (ClientData.voteActive && ClientData.voteTimer > 0 && mc.level.getGameTime() % 20L == 0L) {
               ClientData.voteTimer--;
            }

            RecoilHandler.clientTick();
            Entity vehicle = mc.player.getVehicle();
            if (vehicle instanceof M2BrowningEntity || vehicle instanceof AGS30Entity) {
               long windowId = mc.getWindow().getWindow();
               boolean isLeftBtnDown = GLFW.glfwGetMouseButton(windowId, 0) == 1;
               if (isLeftBtnDown && mc.screen == null) {
                  PacketHandler.INSTANCE.sendToServer(new PacketVehicleShoot());

                  while (mc.options.keyAttack.consumeClick()) {
                  }
               }
            }

             if (mc.level.getGameTime() % 40L == 0L) {
                ClientSkinManager.applyAllSkins();
             }

             if (mc.player.getPersistentData().getBoolean("WARFARE_IsDowned") && mc.player.isAlive()) {
               mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
               if (mc.screen == null) {
                  mc.setScreen(new DownedScreen());
               }
         } else if (mc.player.getPersistentData().contains("WARFARE_DownedYaw")) {
                mc.player.getPersistentData().remove("WARFARE_DownedYaw");
                mc.player.getPersistentData().remove("WARFARE_DownedPitch");
             }

             if (ClientData.currentVoiceChannel != PacketVoiceChannelState.Channel.LOCAL) {
                KeyMapping svc = getSvcPttKey();
                if (svc != null) svc.setDown(true);
             }
          }
       }
    }

   @SubscribeEvent
   public static void onMouseScroll(MouseScrollingEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (ClientData.isMapOpen && mc.screen == null) {
         double delta = event.getScrollDelta();
         if (delta != 0.0) {
            ClientData.zoomMap(delta);
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            String myName = mc.player.getScoreboardName();
            WarfareWorldData.Squad mySquad = null;

            for (WarfareWorldData.Squad s : ClientData.clientSquads) {
               if (s.members.contains(myName)) {
                  mySquad = s;
                  break;
               }
            }

             PoseStack poseStack = event.getPoseStack();
             Vec3 cameraPos = event.getCamera().getPosition();
             if (mySquad != null) {
                boolean isSL = mySquad.leader.equals(myName);
                boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
                boolean isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
               long gameTime = mc.level.getGameTime();
               float blinkAlpha = 0.45F + (float)Math.sin((float)gameTime * 0.4F) * 0.25F;
               if (mySquad.pingPos != null && gameTime < mySquad.pingExpiry) {
                  render3DMarker(poseStack, cameraPos, mySquad.pingPos, PING_TEXTURE, mc, blinkAlpha, 0.5);
               }

               if (mySquad.bravoPingPos != null && gameTime < mySquad.bravoPingExpiry && (isSL || isBravo)) {
                  render3DMarker(poseStack, cameraPos, mySquad.bravoPingPos, PING_TEX_BRAVO, mc, blinkAlpha, 0.5);
               }

               if (mySquad.charliePingPos != null && gameTime < mySquad.charliePingExpiry && (isSL || isCharlie)) {
                  render3DMarker(poseStack, cameraPos, mySquad.charliePingPos, PING_TEX_CHARLIE, mc, blinkAlpha, 0.5);
               }

               if (mySquad.marker != null && mySquad.marker.type == 0 && gameTime < mySquad.marker.expiryTick) {
                  BlockPos mPos = new BlockPos(mySquad.marker.x, mySquad.marker.y, mySquad.marker.z);
                  render3DMarker(poseStack, cameraPos, mPos, MOVE_TEXTURE, mc, 1.0F, 0.5);
               }

               if (mySquad.bravoMarker != null && mySquad.bravoMarker.type == 0 && gameTime < mySquad.bravoMarker.expiryTick) {
                  BlockPos mPos = new BlockPos(mySquad.bravoMarker.x, mySquad.bravoMarker.y, mySquad.bravoMarker.z);
                  render3DMarker(poseStack, cameraPos, mPos, MOVE_TEX_BRAVO, mc, 1.0F, 0.5);
               }

                if (mySquad.charlieMarker != null && mySquad.charlieMarker.type == 0 && gameTime < mySquad.charlieMarker.expiryTick) {
                   BlockPos mPos = new BlockPos(mySquad.charlieMarker.x, mySquad.charlieMarker.y, mySquad.charlieMarker.z);
                   render3DMarker(poseStack, cameraPos, mPos, MOVE_TEX_CHARLIE, mc, 1.0F, 0.5);
                }
             }

          }
       }
    }

   private static void tryPlacePingLogic(Minecraft mc) {
      String myName = mc.player.getScoreboardName();
      boolean canPing = false;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.leader.equals(myName) || s.bravoLeader.equals(myName) || s.charlieLeader.equals(myName)) {
            canPing = true;
            break;
         }
      }

      if (canPing || mc.player.isCreative()) {
         boolean isShift = Screen.hasShiftDown();
         PacketHandler.INSTANCE.sendToServer(new PacketPlacePing(isShift));
         mc.player.playSound((SoundEvent)SoundEvents.UI_BUTTON_CLICK.get(), 0.3F, 1.5F);
      }
   }

   private static void render3DMarker(
      PoseStack poseStack, Vec3 cameraPos, BlockPos pos, ResourceLocation texture, Minecraft mc, float alpha, double heightOffset
   ) {
      poseStack.pushPose();
      double renderX = pos.getX() + 0.5 - cameraPos.x;
      double renderY = pos.getY() + heightOffset - cameraPos.y;
      double renderZ = pos.getZ() + 0.5 - cameraPos.z;
      poseStack.translate(renderX, renderY, renderZ);
      poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
      float scale = 0.025F;
      double distSq = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      if (distSq > 100.0) {
         scale *= (float)(Math.sqrt(distSq) / 10.0);
      }

      scale = Math.min(scale, 0.25F);
      poseStack.scale(-scale, -scale, scale);
      Matrix4f matrix = poseStack.last().pose();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.setShaderTexture(0, texture);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buf = tesselator.getBuilder();
      buf.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      int a = (int)(alpha * 255.0F);
      buf.vertex(matrix, -8.0F, -8.0F, 0.0F).uv(0.0F, 0.0F).color(255, 255, 255, a).endVertex();
      buf.vertex(matrix, -8.0F, 8.0F, 0.0F).uv(0.0F, 1.0F).color(255, 255, 255, a).endVertex();
      buf.vertex(matrix, 8.0F, 8.0F, 0.0F).uv(1.0F, 1.0F).color(255, 255, 255, a).endVertex();
      buf.vertex(matrix, 8.0F, -8.0F, 0.0F).uv(1.0F, 0.0F).color(255, 255, 255, a).endVertex();
      tesselator.end();
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }



   @SubscribeEvent
   public static void onMouseInput(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         Entity vehicle = mc.player.getVehicle();
         if (event.getButton() == 0 && (vehicle instanceof M2BrowningEntity || vehicle instanceof AGS30Entity)) {
            event.setCanceled(true);
         }

         if (ModKeyBindings.PLACE_PING_KEY.matchesMouse(event.getButton()) && event.getAction() == 1 && mc.screen == null) {
            tryPlacePingLogic(mc);
            event.setCanceled(true);
         }

         if (event.getButton() != 1 || event.getAction() != 1 || !(vehicle instanceof M2BrowningEntity) && !(vehicle instanceof AGS30Entity)) {
            if (event.getButton() == 2 && event.getAction() == 1) {
               ItemStack stack = mc.player.getMainHandItem();
               if (stack.getItem() instanceof VehicleMarkerItem || stack.getItem() instanceof SupplyTruckMarkerItem) {
                  HitResult result = mc.hitResult;
                  if (result != null && result.getType() == Type.ENTITY) {
                     EntityHitResult entityResult = (EntityHitResult)result;
                     Entity target = entityResult.getEntity();
                     PacketHandler.INSTANCE.sendToServer(new PacketApplyMarker(target.getId()));
                  }
               }
            }
         } else {
            PacketHandler.INSTANCE.sendToServer(new PacketToggleAim());
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onComputeFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         Entity vehicle = mc.player.getVehicle();
         if (vehicle instanceof M2BrowningEntity m2 && m2.isAiming()) {
            event.setNewFovModifier(event.getFovModifier() * 0.6666667F);
         } else if (vehicle instanceof AGS30Entity ags && ags.isAiming()) {
            event.setNewFovModifier(event.getFovModifier() * 0.33333334F);
         }
      }
   }

     @SubscribeEvent
     public static void onOpenGui(Opening event) {
         if (ClientData.deployRequested) {
            ClientData.deployRequested = false;
            if (event.getScreen() instanceof DeathScreen) {
               event.setNewScreen(null);
            }
            return;
         }
        if (event.getScreen() instanceof DeathScreen && !(event.getScreen() instanceof WarfareDeathScreen)) {
          Component cause = null;
          if (Minecraft.getInstance().player != null) {
             cause = Minecraft.getInstance().player.getCombatTracker().getDeathMessage();
          }

          event.setNewScreen(new WarfareDeathScreen(cause, false));
       }
    }

   @SubscribeEvent
   public static void onKeyInput(Key event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
          if (ModKeyBindings.SQUAD_PTT_KEY.matches(event.getKey(), event.getScanCode())) {
             boolean pressed = event.getAction() == 1;
             if (!pressed) {
                KeyMapping svc = getSvcPttKey();
                if (svc != null) svc.setDown(false);
             }
             PacketHandler.INSTANCE.sendToServer(new PacketVoiceChannelState(
                pressed ? PacketVoiceChannelState.Channel.SQUAD : PacketVoiceChannelState.Channel.LOCAL
             ));
             ClientData.currentVoiceChannel = pressed ? PacketVoiceChannelState.Channel.SQUAD : PacketVoiceChannelState.Channel.LOCAL;
          }
          if (ModKeyBindings.COMMAND_PTT_KEY.matches(event.getKey(), event.getScanCode())) {
             boolean pressed = event.getAction() == 1;
             if (!pressed) {
                KeyMapping svc = getSvcPttKey();
                if (svc != null) svc.setDown(false);
             }
             PacketHandler.INSTANCE.sendToServer(new PacketVoiceChannelState(
                pressed ? PacketVoiceChannelState.Channel.COMMAND : PacketVoiceChannelState.Channel.LOCAL
             ));
             ClientData.currentVoiceChannel = pressed ? PacketVoiceChannelState.Channel.COMMAND : PacketVoiceChannelState.Channel.LOCAL;
          }
         if (event.getKey() == 266 && event.getAction() == 1) {
            PacketHandler.INSTANCE.sendToServer(new PacketConfirmArtStrike(true));
         }

         if (event.getKey() == 267 && event.getAction() == 1) {
            PacketHandler.INSTANCE.sendToServer(new PacketConfirmArtStrike(false));
         }

         String myTeam = mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";
         boolean isBlue = myTeam.equals("BLUE");
         boolean myTeamCmdActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
         String myCandidateName = isBlue ? ClientData.blueCmdCandidateName : ClientData.redCmdCandidateName;
         if (myTeamCmdActive && mc.screen == null) {
            boolean isCandidate = mc.player.getScoreboardName().equals(myCandidateName);
            if (!isCandidate) {
               if (event.getKey() == 296 && event.getAction() == 1) {
                  PacketHandler.INSTANCE.sendToServer(new PacketCMDVote(true));
                  mc.player.displayClientMessage(Component.literal("Voted: YES").withStyle(ChatFormatting.GREEN), true);
                  return;
               }

               if (event.getKey() == 297 && event.getAction() == 1) {
                  PacketHandler.INSTANCE.sendToServer(new PacketCMDVote(false));
                  mc.player.displayClientMessage(Component.literal("Voted: NO").withStyle(ChatFormatting.RED), true);
                  return;
               }
            }
         }

         if (ModKeyBindings.PLACE_PING_KEY.matches(event.getKey(), event.getScanCode()) && event.getAction() == 1) {
            tryPlacePingLogic(mc);
         } else {
            if (ClientData.voteActive && mc.screen == null && event.getAction() == 1) {
               if (event.getKey() == 298) {
                  PacketHandler.INSTANCE.sendToServer(new PacketVoteAction(true));
                  return;
               }

               if (event.getKey() == 299) {
                  PacketHandler.INSTANCE.sendToServer(new PacketVoteAction(false));
                  return;
               }
            }

            if (event.getAction() == 1 && ModKeyBindings.DROP_SUPPLY_KEY.matches(event.getKey(), event.getScanCode()) && mc.player.getVehicle() != null) {
               PacketHandler.INSTANCE.sendToServer(new PacketDropCrate());
            }

            if (event.getAction() == 1 && ModKeyBindings.OPEN_SQUAD_MENU_KEY.matches(event.getKey(), event.getScanCode()) && mc.screen == null) {
                String teamName = mc.player.getTeam() != null ? mc.player.getTeam().getName() : "";
                boolean isValidTeam = teamName.equalsIgnoreCase("Blue") || teamName.equalsIgnoreCase("Red");
                if (!isValidTeam) {
                    mc.setScreen(new TeamSelectionScreen());
                } else {
                    mc.setScreen(new DeployScreen());
                }
            }

            if (event.getKey() == 77 && event.getAction() == 1 && mc.screen == null && !ClientData.isMapOpen) {
                mc.setScreen(new SquadMapScreen());
            }

            if (ModKeyBindings.SHOW_MAP_KEY.matches(event.getKey(), event.getScanCode())) {
               if (event.getAction() == 1) {
                  ClientData.isMapOpen = true;
               } else if (event.getAction() == 0) {
                  ClientData.isMapOpen = false;
               }
            }

            if (event.getKey() == 298 && event.getAction() == 1 && mc.player.isCreative() && mc.screen == null) {
               PacketHandler.INSTANCE.sendToServer(new PacketDebugFill());
            }
         }
      }
   }

   @SubscribeEvent
   public static void onGuiOpen(Opening event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if ((Boolean)WarfareConfig.PREVENT_VEHICLE_INVENTORY_ACCESS.get()
            && !mc.player.isCreative()
            && mc.player.getVehicle() != null
            && (event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof AbstractContainerScreen)) {
            event.setCanceled(true);
            mc.player.displayClientMessage(Component.literal("Inventory is disabled while inside a vehicle!").withStyle(ChatFormatting.RED), true);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onComputeCameraAngles(ComputeCameraAngles event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         boolean isDowned = mc.player.getPersistentData().getBoolean("WARFARE_IsDowned") && mc.player.isAlive();
         if (isDowned) {
            if (!wasDownedLastFrame) {
               initialPitchOnFall = event.getPitch();
               wasDownedLastFrame = true;
            }

            if (downedCameraProgress < 1.0F) {
               downedCameraProgress += 0.07F;
            }
         } else {
            wasDownedLastFrame = false;
            if (downedCameraProgress > 0.0F) {
               downedCameraProgress -= 0.05F;
            }
         }

         if (downedCameraProgress > 0.0F) {
            float p = downedCameraProgress;
            float stumble = 0.0F;
            if (p < 0.4F) {
               stumble = Mth.sin(p * (float) Math.PI * 2.5F) * 12.0F;
            }

            float targetPitch = -60.0F;
            float smoothPitch = Mth.lerp(p, initialPitchOnFall, targetPitch) + stumble;
            event.setPitch(smoothPitch);
            event.setRoll(p * 35.0F);
            if (isDowned && p > 0.8F) {
               float force = (p - 0.8F) * 10.0F;
               event.setYaw(event.getYaw() + (mc.level.random.nextFloat() - 0.5F) * force);
               event.setPitch(event.getPitch() + (mc.level.random.nextFloat() - 0.5F) * force);
            }

            if (p >= 1.0F && isDowned) {
               float breathing = Mth.sin((float)mc.level.getGameTime() * 0.06F) * 1.5F;
               event.setPitch(targetPitch + breathing);
            }
          }
       }
    }

   private static KeyMapping svcPttKey = null;
   private static boolean svcPttSearched = false;

   private static KeyMapping getSvcPttKey() {
      if (!svcPttSearched) {
         svcPttSearched = true;
         for (KeyMapping k : Minecraft.getInstance().options.keyMappings) {
            if (k.getName().equals("key.push_to_talk")) {
               svcPttKey = k;
               break;
            }
         }
      }
      return svcPttKey;
   }
}
