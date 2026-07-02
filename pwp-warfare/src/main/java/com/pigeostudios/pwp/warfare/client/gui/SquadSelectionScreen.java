package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.network.PacketSquadChat;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

// Экран управления отрядами и картой
// Позволяет создавать/покидать отряды, назначать командиров, чат и карту
public class SquadSelectionScreen extends Screen {
   private SquadSelectionScreen.SquadButton applyCmdButton;
   private static final int SIDEBAR_WIDTH = 170;
   private static final int TOP_BAR_HEIGHT = 30;
   private static final ResourceLocation LOCK_ICON = new ResourceLocation("pwpwarfare", "textures/gui/squad_lock.png");
   private static final ResourceLocation ARROW_DOWN = new ResourceLocation("pwpwarfare", "textures/gui/arrow_down.png");
   private static final ResourceLocation ARROW_UP = new ResourceLocation("pwpwarfare", "textures/gui/arrow_up.png");
   private static final ResourceLocation CENTER_ICON = new ResourceLocation("minecraft", "textures/item/compass_16.png");
   private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("pwpwarfare", "textures/gui/flags/ukraine.png");
   private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("pwpwarfare", "textures/gui/flags/russia.png");
   private static final ResourceLocation FLAG_USA = new ResourceLocation("pwpwarfare", "textures/gui/flags/usa.png");
   private static final ResourceLocation FLAG_NATO = new ResourceLocation("pwpwarfare", "textures/gui/flags/nato.png");
   private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
   private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
   private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("pwpwarfare", "textures/gui/flags/insurgency.png");
   private static final ResourceLocation FLAG_PMC = new ResourceLocation("pwpwarfare", "textures/gui/flags/pmc.png");
   private EditBox nameInput;
   private Button createButton;
   private EditBox chatInput;
   private Button chatModeButton;
   private int chatMode = 1;
   private boolean showContextMenu = false;
   private int contextMenuX = 0;
   private int contextMenuY = 0;
   private String contextTargetPlayer = "";
   private int contextTargetSquadId = -1;
   private int mapX;
   private int mapY;
   private int mapSize;
   private final Set<Integer> expandedSquads = new HashSet<>();
   private final WarfareMapRenderer mapRenderer = new WarfareMapRenderer();

   public SquadSelectionScreen() {
      super(Component.translatable("gui.pwpwarfare.squad_select.title"));
   }

   protected void init() {
      super.init();
      String myName = this.minecraft.player.getScoreboardName();
      String myTeam = this.getPlayerTeam().toUpperCase();
      boolean isInSquad = this.isPlayerInSquad();
      WarfareWorldData.Squad mySquad = null;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) {
            this.expandedSquads.add(s.id);
            if (s.leader.equals(myName)) {
               ;
            }
         }
      }

      int teamCMDId_init = myTeam.contains("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
      this.applyCmdButton = new SquadSelectionScreen.SquadButton(10, 10, 150, 20, Component.translatable("gui.pwpwarfare.squad_select.apply_cmd"), b -> {
         PacketHandler.INSTANCE.sendToServer(new PacketRequestCMD());
         b.visible = false;
      });
      this.addRenderableWidget(this.applyCmdButton);
      this.nameInput = new EditBox(this.font, 10, this.height - 55, 150, 20, Component.translatable("gui.pwpwarfare.squad_select.squad_name"));
      this.nameInput.setMaxLength(12);
      this.nameInput.setVisible(!isInSquad);
      this.addRenderableWidget(this.nameInput);
      this.createButton = Button.builder(
            Component.translatable("gui.pwpwarfare.squad_select.create"), button -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, this.nameInput.getValue()))
         )
         .bounds(10, this.height - 30, 150, 20)
         .build();
      this.createButton.visible = !isInSquad;
      this.addRenderableWidget(this.createButton);
      int rightAreaWidth = this.width - 170;
      int mapMargin = 2;
      int availableHeight = this.height - 30 - 50;
      this.mapSize = Math.min(rightAreaWidth - mapMargin * 2, availableHeight);
      this.mapX = this.width - this.mapSize - mapMargin;
      this.mapY = 30 + mapMargin;
      this.mapRenderer.init(this.mapX, this.mapY, this.mapSize);
      int inputY = this.height - 25;
      int chatX = 175;
      int chatWidth = this.width - 170 - 10;
      this.chatModeButton = Button.builder(this.getChatModeText(), button -> {
         this.chatMode++;
         if (this.chatMode > 2) {
            this.chatMode = 0;
         }

         button.setMessage(this.getChatModeText());
      }).bounds(chatX, inputY, 50, 20).build();
      this.addRenderableWidget(this.chatModeButton);
      this.chatInput = new EditBox(this.font, chatX + 55, inputY, chatWidth - 55, 20, Component.translatable("gui.pwpwarfare.squad_select.chat"));
      this.chatInput.setMaxLength(256);
      this.addRenderableWidget(this.chatInput);
   }

   private Component getChatModeText() {
      switch (this.chatMode) {
         case 0:
            return Component.translatable("gui.pwpwarfare.squad_select.channel_all").withStyle(ChatFormatting.LIGHT_PURPLE);
         case 1:
            return Component.translatable("gui.pwpwarfare.squad_select.channel_team").withStyle(ChatFormatting.BLUE);
         case 2:
            return Component.translatable("gui.pwpwarfare.squad_select.channel_squad").withStyle(ChatFormatting.GREEN);
         default:
            return Component.translatable("gui.pwpwarfare.squad_select.channel_unknown");
      }
   }

   public void tick() {
      super.tick();
      this.nameInput.tick();
      this.chatInput.tick();
      if (this.applyCmdButton != null) {
         String myName = this.minecraft.player.getScoreboardName();
         String myTeam = this.getPlayerTeam().toUpperCase();
         WarfareWorldData.Squad mySquad = null;

         for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) {
               mySquad = s;
               break;
            }
         }

         boolean isBlue = myTeam.contains("BLUE");
         int myTeamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
         boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
         boolean isLeader = mySquad != null && mySquad.leader.equals(myName);
         this.applyCmdButton.visible = isLeader && myTeamCMDId == -1 && !myTeamVoteActive;
      }

      boolean isInSquad = this.isPlayerInSquad();
      if (this.nameInput.isVisible() == isInSquad) {
         this.nameInput.setVisible(!isInSquad);
         this.createButton.visible = !isInSquad;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 257 || keyCode == 335) {
         if (this.chatInput.isFocused()) {
            String msg = this.chatInput.getValue().trim();
            if (!msg.isEmpty()) {
               PacketHandler.INSTANCE.sendToServer(new PacketSquadChat(msg, this.chatMode));
               this.chatInput.setValue("");
            }

            return true;
         }

         if (this.nameInput.isFocused() && this.nameInput.isVisible()) {
            String name = this.nameInput.getValue();
            PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, name));
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private List<String> getSortedMembers(WarfareWorldData.Squad squad) {
      List<String> sorted = new ArrayList<>();
      if (!squad.leader.isEmpty() && squad.members.contains(squad.leader)) {
         sorted.add(squad.leader);
      }

      for (String m : squad.members) {
         if (!m.equals(squad.leader) && !squad.bravoMembers.contains(m) && !squad.charlieMembers.contains(m)) {
            sorted.add(m);
         }
      }

      if (!squad.bravoLeader.isEmpty() && squad.members.contains(squad.bravoLeader)) {
         sorted.add(squad.bravoLeader);
      }

      for (String m : squad.bravoMembers) {
         if (!m.equals(squad.bravoLeader) && squad.members.contains(m)) {
            sorted.add(m);
         }
      }

      if (!squad.charlieLeader.isEmpty() && squad.members.contains(squad.charlieLeader)) {
         sorted.add(squad.charlieLeader);
      }

      for (String m : squad.charlieMembers) {
         if (!m.equals(squad.charlieLeader) && squad.members.contains(m)) {
            sorted.add(m);
         }
      }

      return sorted;
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      gui.fill(0, 0, 170, this.height, -872415232);
      gui.fill(170, 0, this.width, 30, -872415232);
      gui.fill(170, 30, this.width, this.height, -1795162112);
      this.renderSquadList(gui, mouseX, mouseY);
      this.mapRenderer.render(gui, mouseX, mouseY, partialTick);
      this.renderChatHistory(gui, this.mapY, this.mapSize);
      this.renderTopBar(gui);
      super.render(gui, mouseX, mouseY, partialTick);
      if (this.showContextMenu) {
         this.renderContextMenu(gui, mouseX, mouseY);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      return this.mapRenderer.mouseScrolled(mouseX, mouseY, delta) ? true : super.mouseScrolled(mouseX, mouseY, delta);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.showContextMenu) {
         if (button == 0) {
            String myName = this.minecraft.player.getScoreboardName();
            WarfareWorldData.Squad s = null;

            for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
               if (sq.id == this.contextTargetSquadId) {
                  s = sq;
                  break;
               }
            }

            if (s != null) {
               boolean amISL = s.leader.equals(myName);
               boolean amIBravoFTL = s.bravoLeader.equals(myName);
               boolean amICharlieFTL = s.charlieLeader.equals(myName);
               List<String> options = new ArrayList<>();
               if (amISL) {
                  options.add("Promote to SL");
                  if (!s.bravoLeader.equals(this.contextTargetPlayer) && !s.charlieLeader.equals(this.contextTargetPlayer)) {
                     options.add("Set FTL Bravo");
                     options.add("Set FTL Charlie");
                  }

                  options.add("Add to Bravo");
                  options.add("Add to Charlie");
                  options.add("Remove from FT");
                  options.add("Kick from Squad");
               } else if (amIBravoFTL) {
                  if (!s.leader.equals(this.contextTargetPlayer) && !s.charlieLeader.equals(this.contextTargetPlayer)) {
                     options.add("Pass FTL Bravo");
                  }

                  options.add("Add to Bravo");
                  options.add("Remove from FT");
               } else if (amICharlieFTL) {
                  if (!s.leader.equals(this.contextTargetPlayer) && !s.bravoLeader.equals(this.contextTargetPlayer)) {
                     options.add("Pass FTL Charlie");
                  }

                  options.add("Add to Charlie");
                  options.add("Remove from FT");
               }

               int w = 100;
               int h = options.size() * 12 + 4;
               if (mouseX >= this.contextMenuX && mouseX <= this.contextMenuX + w && mouseY >= this.contextMenuY && mouseY <= this.contextMenuY + h) {
                  int clickedIdx = (int)(mouseY - this.contextMenuY - 2.0) / 12;
                  if (clickedIdx >= 0 && clickedIdx < options.size()) {
                     String opt = options.get(clickedIdx);
                     if (opt.equals("Promote to SL")) {
                        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(4, s.id, this.contextTargetPlayer));
                     } else if (opt.equals("Set FTL Bravo") || opt.equals("Pass FTL Bravo")) {
                        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(6, s.id, this.contextTargetPlayer));
                     } else if (opt.equals("Set FTL Charlie") || opt.equals("Pass FTL Charlie")) {
                        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(7, s.id, this.contextTargetPlayer));
                     } else if (opt.equals("Add to Bravo")) {
                        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(8, s.id, this.contextTargetPlayer));
                     } else if (opt.equals("Add to Charlie")) {
                        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(9, s.id, this.contextTargetPlayer));
                     } else if (opt.equals("Remove from FT")) {
                        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(10, s.id, this.contextTargetPlayer));
                     } else if (opt.equals("Kick from Squad")) {
                        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(3, s.id, this.contextTargetPlayer));
                     }

                     this.playClickSound();
                  }
               }
            }
         }

         this.showContextMenu = false;
         return true;
      } else if (this.mapRenderer.isMouseOver(mouseX, mouseY) && this.mapRenderer.mouseClicked(mouseX, mouseY, button)) {
         if (button == 1) {
            this.handleMapRightClick(mouseX, mouseY);
         }

         return true;
      } else {
         if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
         }

         if (button == 1 && mouseX < 170.0 && mouseY < this.height - 60) {
            String myName = this.minecraft.player.getScoreboardName();
            String myTeam = this.getPlayerTeam().toUpperCase();
            String myDim = this.minecraft.level.dimension().location().toString();
            boolean isBlue = myTeam.contains("BLUE");
            int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
            boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
            boolean amISL_Anywhere = false;

            for (WarfareWorldData.Squad s : ClientData.clientSquads) {
               if (s.leader.equals(myName)) {
                  amISL_Anywhere = true;
                  break;
               }
            }

            int currentY = amISL_Anywhere && teamCMDId == -1 && !myTeamVoteActive ? 35 : 10;
            List<WarfareWorldData.Squad> myTeamSquads = ClientData.clientSquads
               .stream()
               .filter(s -> s.team.equalsIgnoreCase(myTeam))
               .filter(s -> s.dimension != null && s.dimension.equals(myDim))
               .collect(Collectors.toList());
            myTeamSquads.sort((s1, s2) -> {
               if (s1.id == teamCMDId && teamCMDId != -1) {
                  return -1;
               } else {
                  return s2.id == teamCMDId && teamCMDId != -1 ? 1 : Integer.compare(s1.id, s2.id);
               }
            });

            for (WarfareWorldData.Squad squad : myTeamSquads) {
               currentY += 12;
               if (this.expandedSquads.contains(squad.id)) {
                  if (squad.members.contains(myName)) {
                     for (String member : this.getSortedMembers(squad)) {
                        if (mouseY >= currentY && mouseY <= currentY + 12 && mouseX >= 30.0 && !member.equals(myName)) {
                           boolean amISL = squad.leader.equals(myName);
                           boolean amIBravo = squad.bravoLeader.equals(myName);
                           boolean amICharlie = squad.charlieLeader.equals(myName);
                           if (amISL || amIBravo || amICharlie) {
                              this.showContextMenu = true;
                              this.contextMenuX = (int)mouseX;
                              this.contextMenuY = (int)mouseY;
                              this.contextTargetPlayer = member;
                              this.contextTargetSquadId = squad.id;
                              this.playClickSound();
                              return true;
                           }
                        }

                        currentY += 12;
                     }
                  } else {
                     currentY += squad.members.size() * 12;
                  }

                  currentY += 4;
               }

               currentY += 4;
            }
         }

         if (button == 0 && mouseX < 170.0 && mouseY < this.height - 60) {
            this.handleSquadListClicks(mouseX, mouseY);
            return true;
         } else {
            return false;
         }
      }
   }

   private void handleSquadListClicks(double mouseX, double mouseY) {
      String myName = this.minecraft.player.getScoreboardName();
      String myTeam = this.getPlayerTeam().toUpperCase();
      String myDim = this.minecraft.level.dimension().location().toString();
      boolean amIInSquad = this.isPlayerInSquad();
      boolean isBlue = myTeam.contains("BLUE");
      int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
      boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
      boolean amISquadLeaderAnywhere = false;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.leader.equals(myName)) {
            amISquadLeaderAnywhere = true;
            break;
         }
      }

      int currentY = amISquadLeaderAnywhere && teamCMDId == -1 && !myTeamVoteActive ? 35 : 10;
      List<WarfareWorldData.Squad> myTeamSquads = ClientData.clientSquads
         .stream()
         .filter(s -> s.team.equalsIgnoreCase(myTeam))
         .filter(s -> s.dimension != null && s.dimension.equals(myDim))
         .collect(Collectors.toList());
      myTeamSquads.sort((s1, s2) -> {
         if (s1.id == teamCMDId && teamCMDId != -1) {
            return -1;
         } else {
            return s2.id == teamCMDId && teamCMDId != -1 ? 1 : Integer.compare(s1.id, s2.id);
         }
      });

      for (WarfareWorldData.Squad squad : myTeamSquads) {
         boolean isMySquad = squad.members.contains(myName);
         boolean amILeader = squad.leader.equals(myName);
         boolean isExpanded = this.expandedSquads.contains(squad.id);
         String actionText = "";
         if (isMySquad) {
            actionText = "LEAVE";
         } else if (!amIInSquad) {
            if (squad.isLocked) {
               actionText = "LOCKED";
            } else if (squad.members.size() >= 9) {
               actionText = "FULL";
            } else {
               actionText = "JOIN";
            }
         }

         int actionWidth = actionText.isEmpty() ? 0 : this.font.width(actionText);
         int actionX = actionWidth > 0 ? 170 - actionWidth - 10 : 0;
         int arrowX = (actionX > 0 ? actionX : 160) - 12;
         int lockX = arrowX - 12;
         if (actionWidth > 0 && mouseX >= actionX && mouseX <= actionX + actionWidth && mouseY >= currentY && mouseY <= currentY + 9) {
            if (isMySquad) {
               PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(2, squad.id, ""));
            } else if (!squad.isLocked && squad.members.size() < 9) {
               PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(1, squad.id, ""));
            }

            this.playClickSound();
            return;
         }

         if (mouseX >= arrowX && mouseX <= arrowX + 10 && mouseY >= currentY && mouseY <= currentY + 10) {
            if (isExpanded) {
               this.expandedSquads.remove(squad.id);
            } else {
               this.expandedSquads.add(squad.id);
            }

            this.playClickSound();
            return;
         }

         if (amILeader && mouseX >= lockX && mouseX <= lockX + 10 && mouseY >= currentY && mouseY <= currentY + 10) {
            PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(5, squad.id, ""));
            this.playClickSound();
            return;
         }

         currentY += 12;
         if (isExpanded) {
            for (String member : this.getSortedMembers(squad)) {
               if (isMySquad && member.equals(myName) && mouseX >= 30.0 && mouseX <= 40.0 && mouseY >= currentY && mouseY <= currentY + 10) {
                  PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu());
                  this.playClickSound();
                  return;
               }

               currentY += 12;
            }

            currentY += 4;
         }

         currentY += 4;
      }
   }

   private void setupContextMenu(double mx, double my) {
      String myName = this.minecraft.player.getScoreboardName();
      String myTeam = this.getPlayerTeam().toUpperCase();
      boolean isBlue = myTeam.contains("BLUE");
      int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
      boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
      boolean amISL_Global = false;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.leader.equals(myName)) {
            amISL_Global = true;
            break;
         }
      }

      boolean buttonVisible = amISL_Global && teamCMDId == -1 && !myTeamVoteActive;
      int currentY = buttonVisible ? 35 : 10;

      for (WarfareWorldData.Squad squad : this.getSortedSquads()) {
         currentY += 12;
         if (this.expandedSquads.contains(squad.id)) {
            for (String member : this.getSortedMembers(squad)) {
               if (my >= currentY
                  && my < currentY + 12
                  && mx > 30.0
                  && !member.equals(myName)
                  && squad.members.contains(myName)
                  && (squad.leader.equals(myName) || squad.bravoLeader.equals(myName) || squad.charlieLeader.equals(myName))) {
                  this.contextTargetPlayer = member;
                  this.contextTargetSquadId = squad.id;
                  this.contextMenuX = (int)mx;
                  this.contextMenuY = (int)my;
                  this.showContextMenu = true;
                  this.playClickSound();
                  return;
               }

               currentY += 12;
            }

            currentY += 4;
         }

         currentY += 4;
      }
   }

   private List<WarfareWorldData.Squad> getSortedSquads() {
      String myTeam = this.getPlayerTeam();
      String myDim = this.minecraft.level.dimension().location().toString();
      int teamCMDId = myTeam.equalsIgnoreCase("Blue") ? ClientData.blueCMDId : ClientData.redCMDId;
      List<WarfareWorldData.Squad> list = ClientData.clientSquads
         .stream()
         .filter(s -> s.team.equalsIgnoreCase(myTeam))
         .filter(s -> s.dimension != null && s.dimension.equals(myDim))
         .collect(Collectors.toList());
      list.sort((s1, s2) -> {
         if (s1.id == teamCMDId && teamCMDId != -1) {
            return -1;
         } else {
            return s2.id == teamCMDId && teamCMDId != -1 ? 1 : Integer.compare(s1.id, s2.id);
         }
      });
      return list;
   }

   private void handleMapRightClick(double mouseX, double mouseY) {
      if (!this.isSquadLeaderOrFTL(this.minecraft.player)) {
         this.minecraft.player.displayClientMessage(Component.translatable("gui.pwpwarfare.map_marker.error").withStyle(ChatFormatting.RED), true);
      } else {
         double bpp = this.mapRenderer.getBlocksPerPixel();
         double centerX = this.mapRenderer.getCenterX(this.minecraft.player);
         double centerZ = this.mapRenderer.getCenterZ(this.minecraft.player);
         int targetX = (int)(centerX + (mouseX - (this.mapX + this.mapSize / 2.0)) * bpp);
         int targetZ = (int)(centerZ + (mouseY - (this.mapY + this.mapSize / 2.0)) * bpp);
         this.minecraft.setScreen(new TacticalMapRadialScreen(targetX, targetZ));
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.mapRenderer.mouseReleased(button);
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return this.mapRenderer.mouseDragged(mouseX, mouseY, button, dragX, dragY) ? true : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   private boolean isSquadLeaderOrFTL(Player player) {
      String pName = player.getScoreboardName();

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.leader.equals(pName) || s.bravoLeader.equals(pName) || s.charlieLeader.equals(pName)) {
            return true;
         }
      }

      return false;
   }

   private void renderChatHistory(GuiGraphics gui, int mapY, int mapSize) {
      int chatX = 180;
      int chatBottomY = this.height - 35;
      int chatTopY = mapY + mapSize + 10;
      int count = 0;

      for (Component msg : ClientData.menuChatHistory) {
         int y = chatBottomY - count * 10;
         if (y < chatTopY) {
            break;
         }

         gui.drawString(this.font, msg, chatX, y, -1, true);
         count++;
      }
   }

   private void renderSquadList(GuiGraphics gui, int mouseX, int mouseY) {
      String myName = this.minecraft.player.getScoreboardName();
      String myTeam = this.getPlayerTeam().toUpperCase();
      String myDim = this.minecraft.level.dimension().location().toString();
      boolean amIInSquad = this.isPlayerInSquad();
      boolean isBlue = myTeam.contains("BLUE");
      int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
      boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
      boolean amISquadLeaderAnywhere = false;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.leader.equals(myName)) {
            amISquadLeaderAnywhere = true;
            break;
         }
      }

      int currentY = amISquadLeaderAnywhere && teamCMDId == -1 && !myTeamVoteActive ? 35 : 10;
      List<WarfareWorldData.Squad> myTeamSquads = ClientData.clientSquads
         .stream()
         .filter(s -> s.team.equalsIgnoreCase(myTeam))
         .filter(s -> s.dimension != null && s.dimension.equals(myDim))
         .collect(Collectors.toList());
      myTeamSquads.sort((s1, s2) -> {
         if (s1.id == teamCMDId && teamCMDId != -1) {
            return -1;
         } else {
            return s2.id == teamCMDId && teamCMDId != -1 ? 1 : Integer.compare(s1.id, s2.id);
         }
      });
      int index = 1;

      for (WarfareWorldData.Squad squad : myTeamSquads) {
         boolean isMySquad = squad.members.contains(myName);
         boolean amILeader = squad.leader.equals(myName);
         boolean isExpanded = this.expandedSquads.contains(squad.id);
         boolean isCMD = squad.id == teamCMDId && teamCMDId != -1;
         String prefix = isCMD ? "[CMD] " : "";
         int squadNameColor = isCMD ? -11141291 : -10496;
         gui.drawString(this.font, index + ".", 5, currentY, -1, false);
         String squadDisplayName = prefix + squad.name + " (" + squad.members.size() + "/9)";
         gui.drawString(this.font, squadDisplayName, 25, currentY, squadNameColor, false);
         String actionText = "";
         int actionColor = -1;
         boolean clickable = true;
         if (isMySquad) {
            actionText = "LEAVE";
            actionColor = -43691;
         } else if (!amIInSquad) {
            if (squad.isLocked) {
               actionText = "LOCKED";
               actionColor = -22016;
               clickable = false;
            } else if (squad.members.size() >= 9) {
               actionText = "FULL";
               actionColor = -7829368;
               clickable = false;
            } else {
               actionText = "JOIN";
               actionColor = -11141291;
            }
         }

         int actionX = 0;
         if (!actionText.isEmpty()) {
            int actionWidth = this.font.width(actionText);
            actionX = 170 - actionWidth - 10;
            boolean hover = mouseX >= actionX && mouseX <= actionX + actionWidth && mouseY >= currentY && mouseY <= currentY + 9;
            int finalColor = hover && clickable ? -1 : actionColor;
            gui.drawString(this.font, actionText, actionX, currentY, finalColor, false);
         }

         int arrowX = (actionX > 0 ? actionX : 160) - 12;
         RenderSystem.enableBlend();
         if (isExpanded) {
            RenderSystem.setShaderColor(1.0F, 0.8F, 0.2F, 1.0F);
            gui.blit(ARROW_DOWN, arrowX, currentY + 1, 0.0F, 0.0F, 8, 8, 8, 8);
         } else {
            RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 1.0F);
            gui.blit(ARROW_UP, arrowX, currentY + 1, 0.0F, 0.0F, 8, 8, 8, 8);
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         if (amILeader || squad.isLocked) {
            int lockX = arrowX - 12;
            if (squad.isLocked) {
               RenderSystem.setShaderColor(1.0F, 0.8F, 0.2F, 1.0F);
            } else {
               RenderSystem.setShaderColor(0.6F, 0.6F, 0.6F, 1.0F);
            }

            gui.blit(LOCK_ICON, lockX, currentY + 1, 0.0F, 0.0F, 8, 8, 8, 8);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         currentY += 12;
         if (isExpanded) {
            for (String member : this.getSortedMembers(squad)) {
               boolean isLeaderMember = member.equals(squad.leader);
               boolean isOnline = this.minecraft.getConnection().getPlayerInfo(member) != null;
               int col = -1;
               if (isLeaderMember) {
                  col = -10496;
               } else if (member.equals(squad.bravoLeader)) {
                  col = -43521;
               } else if (squad.bravoMembers.contains(member)) {
                  col = -5635926;
               } else if (member.equals(squad.charlieLeader)) {
                  col = -11141291;
               } else if (squad.charlieMembers.contains(member)) {
                  col = -16733696;
               }

               if (!isOnline) {
                  col = -5592406;
               }

               int xOffset = 30;
               if (isMySquad && member.equals(myName)) {
                  int btnX = xOffset;
                  boolean btnHover = mouseX >= btnX && mouseX <= btnX + 10 && mouseY >= currentY && mouseY <= currentY + 10;
                  gui.fill(btnX, currentY, btnX + 10, currentY + 10, btnHover ? -10066330 : -12303292);
                  gui.drawString(this.font, "K", btnX + 2, currentY + 1, -1, false);
                  xOffset += 14;
               }

               String kName = ClientData.playerKits.getOrDefault(member, "Unassigned");
               if (!kName.equals("Unassigned") && !kName.isEmpty()) {
                  ResourceLocation kitIcon = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + kName.toLowerCase().replace(" ", "_") + ".png");
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  gui.blit(kitIcon, xOffset, currentY, 0.0F, 0.0F, 10, 10, 10, 10);
                  xOffset += 12;
               }

               gui.drawString(this.font, member, xOffset, currentY + 1, col, false);
               currentY += 12;
            }

            currentY += 4;
         }

         currentY += 4;
         index++;
      }
   }

   private void renderContextMenu(GuiGraphics gui, int mx, int my) {
      String myName = this.minecraft.player.getScoreboardName();
      WarfareWorldData.Squad s = null;

      for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
         if (sq.id == this.contextTargetSquadId) {
            s = sq;
         }
      }

      if (s == null) {
         this.showContextMenu = false;
      } else {
         boolean amISL = s.leader.equals(myName);
         boolean amIBravoFTL = s.bravoLeader.equals(myName);
         boolean amICharlieFTL = s.charlieLeader.equals(myName);
         List<String> options = new ArrayList<>();
         if (amISL) {
            options.add("Promote to SL");
            if (!s.bravoLeader.equals(this.contextTargetPlayer) && !s.charlieLeader.equals(this.contextTargetPlayer)) {
               options.add("Set FTL Bravo");
               options.add("Set FTL Charlie");
            }

            options.add("Add to Bravo");
            options.add("Add to Charlie");
            options.add("Remove from FT");
            options.add("Kick from Squad");
         } else if (amIBravoFTL) {
            if (!s.leader.equals(this.contextTargetPlayer) && !s.charlieLeader.equals(this.contextTargetPlayer)) {
               options.add("Pass FTL Bravo");
            }

            options.add("Add to Bravo");
            options.add("Remove from FT");
         } else if (amICharlieFTL) {
            if (!s.leader.equals(this.contextTargetPlayer) && !s.bravoLeader.equals(this.contextTargetPlayer)) {
               options.add("Pass FTL Charlie");
            }

            options.add("Add to Charlie");
            options.add("Remove from FT");
         }

         if (options.isEmpty()) {
            this.showContextMenu = false;
         } else {
            int w = 100;
            int h = options.size() * 12 + 4;
            gui.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + w, this.contextMenuY + h, -300871407);
            gui.renderOutline(this.contextMenuX, this.contextMenuY, w, h, -11184811);

            for (int i = 0; i < options.size(); i++) {
               int y = this.contextMenuY + 2 + i * 12;
               boolean hover = mx >= this.contextMenuX && mx <= this.contextMenuX + w && my >= y && my < y + 12;
               if (hover) {
                  gui.fill(this.contextMenuX + 1, y, this.contextMenuX + w - 1, y + 12, -12303292);
               }

               gui.drawString(this.font, options.get(i), this.contextMenuX + 4, y + 2, hover ? 16777215 : 11184810, false);
            }
         }
      }
   }

   private void renderTopBar(GuiGraphics gui) {
      String myName = this.minecraft.player.getScoreboardName();
      String myTeam = this.getPlayerTeam();
      String teamUpper = myTeam.toUpperCase();
      WarfareWorldData.Squad mySquad = null;

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) {
            mySquad = s;
            break;
         }
      }

      int tickets = 0;
      ResourceLocation flag = null;
      if (teamUpper.contains("BLUE")) {
         tickets = ClientData.BLUE_TICKETS;
         flag = this.getFlagTexture(ClientData.BLUE_FACTION);
      } else if (teamUpper.contains("RED")) {
         tickets = ClientData.RED_TICKETS;
         flag = this.getFlagTexture(ClientData.RED_FACTION);
      }

      boolean isBlue = teamUpper.contains("BLUE");
      int myTeamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
      boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
      boolean isLeader = mySquad != null && mySquad.leader.equals(myName);
      this.applyCmdButton.visible = isLeader && myTeamCMDId == -1 && !myTeamVoteActive;
      int rightEdge = this.width - 10;
      String ticketText = String.valueOf(tickets);
      int textWidth = this.font.width(ticketText);
      int iconSize = 12;
      int gap = 5;
      gui.drawString(this.font, ticketText, rightEdge - textWidth, 11, -1, false);
      ResourceLocation ticketIcon = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      gui.blit(ticketIcon, rightEdge - textWidth - gap - iconSize, 10, iconSize, iconSize, 0.0F, 0.0F, 16, 16, 16, 16);
      if (flag != null) {
         gui.pose().pushPose();
         gui.pose().translate(0.0F, 0.0F, 100.0F);
         int flagX = rightEdge - textWidth - gap - iconSize - 15 - 32;
         int flagY = 6;
         gui.blit(flag, flagX, flagY, 0.0F, 0.0F, 32, 18, 32, 18);
         gui.pose().popPose();
      }
   }

   private void playClickSound() {
      this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   private boolean isPlayerInSquad() {
      String myName = this.minecraft.player.getScoreboardName();

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) {
            return true;
         }
      }

      return false;
   }

   private String getPlayerTeam() {
      return this.minecraft.player.getTeam() != null ? this.minecraft.player.getTeam().getName() : "NEUTRAL";
   }

   private ResourceLocation getFlagTexture(String faction) {
      if (faction != null && !faction.equalsIgnoreCase("none")) {
         switch (faction.toLowerCase()) {
            case "ukraine":
               return FLAG_UKRAINE;
            case "russia":
               return FLAG_RUSSIA;
            case "usa":
               return FLAG_USA;
            case "nato":
               return FLAG_NATO;
            case "bluefor":
               return FLAG_BLUEFOR;
            case "redfor":
               return FLAG_REDFOR;
            case "insurgency":
               return FLAG_INSURGENCY;
            case "pmc":
               return FLAG_PMC;
            default:
               return null;
         }
      } else {
         return null;
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static class SquadButton extends Button {
      public SquadButton(int x, int y, int width, int height, Component message, OnPress onPress) {
         super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
         if (this.visible) {
            int borderColor = this.isHovered() ? -1 : -6710887;
            if (!this.active) {
               borderColor = -12303292;
            }

            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -871296751);
            gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
            int textColor = this.active ? -1 : -8947849;
            gui.drawCenteredString(
               Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor
            );
            if (this.active && this.isHovered()) {
               gui.fill(this.getX(), this.getY() + this.height - 2, this.getX() + 2, this.getY() + this.height, -1);
            }
         }
      }
   }
}
