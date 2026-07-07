package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

public class WarfareDeathScreen extends DeathScreen {
   private int respawnTimeTotal = 10;
   private long deathTimestamp;
   private final Component cause;
   private Button mainSpawnButton;
   private Button rallySpawnButton;
   private Button hubSpawnButton;
   private Button disconnectButton;
   private boolean isHubListOpen = false;
   private List<Button> hubButtons = new ArrayList<>();
   private float scrollAmount = 0.0F;
   private boolean isScrolling = false;
   private static final int VISIBLE_ITEMS = 4;
   private static final int ITEM_HEIGHT = 25;
   private static final int LIST_HEIGHT = 100;
   private int listLeft;
   private int listRight;
   private int listTop;
   private int listBottom;

   public WarfareDeathScreen(Component cause, boolean hardcore) {
      super(cause, hardcore);
      this.cause = cause;
      this.deathTimestamp = System.currentTimeMillis();
      this.respawnTimeTotal = ClientData.RESPAWN_TIME > 0 ? ClientData.RESPAWN_TIME : 10;
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      this.clearWidgets();
      this.hubButtons.clear();
      if (!this.isHubListOpen) {
         this.scrollAmount = 0.0F;
      }

      int centerX = this.width / 2;
      int centerY = this.height / 2;
      this.listLeft = centerX - 115;
      this.listRight = centerX + 115;
      this.listTop = centerY + 58;
      this.listBottom = this.listTop + 100;
      Team team = this.minecraft.player.getTeam();
      String currentDim = this.minecraft.level.dimension().location().toString();
      boolean hasSpecificSpawn = false;
      if (team != null) {
         String myTeam = team.getName();
         if (myTeam.equalsIgnoreCase("Blue")) {
            hasSpecificSpawn = ClientData.blueSpawns.containsKey(currentDim) || ClientData.blueSpawns.containsKey("minecraft:overworld");
         } else if (myTeam.equalsIgnoreCase("Red")) {
            hasSpecificSpawn = ClientData.redSpawns.containsKey(currentDim) || ClientData.redSpawns.containsKey("minecraft:overworld");
         }
      } else {
         hasSpecificSpawn = ClientData.neutralSpawns.containsKey(currentDim);
      }

      String mainText = hasSpecificSpawn ? "MAIN BASE" : "SPAWN";
      this.mainSpawnButton = Button.builder(Component.literal(mainText), button -> {
         PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest("MAIN"));
         this.minecraft.player.respawn();
         this.minecraft.setScreen(null);
      }).bounds(centerX - 115, centerY + 5, 105, 22).build();
      this.rallySpawnButton = Button.builder(Component.literal("SQUAD RALLY"), button -> {
         PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest("RALLY"));
         this.minecraft.player.respawn();
         this.minecraft.setScreen(null);
      }).bounds(centerX + 10, centerY + 5, 105, 22).build();
      this.mainSpawnButton.active = false;
      this.addRenderableWidget(this.mainSpawnButton);
      this.rallySpawnButton.active = false;
      this.addRenderableWidget(this.rallySpawnButton);
      boolean hubsExist = false;
      if (team != null) {
         String myTeam = team.getName();
         hubsExist = ClientData.clientHubs.stream().anyMatch(h -> h.team.equalsIgnoreCase(myTeam) && h.constructed && h.dimension.equals(currentDim));
      }

      String hubBtnText = hubsExist ? "HUB SPAWN \u25BC" : "NO HUBS";
      this.hubSpawnButton = Button.builder(Component.literal(hubBtnText), button -> {
         this.isHubListOpen = !this.isHubListOpen;
         if (!this.isHubListOpen) {
            this.scrollAmount = 0.0F;
         }

         if (this.isHubListOpen) {
            this.createHubButtons();
         }
      }).bounds(centerX - 55, centerY + 32, 110, 22).build();
      this.addRenderableWidget(this.hubSpawnButton);
      this.disconnectButton = Button.builder(Component.literal("Disconnect"), button -> {
         this.minecraft.level.disconnect();
         this.minecraft.clearLevel();
         this.minecraft.setScreen(new TitleScreen());
      }).bounds(centerX - 55, this.height - 32, 110, 22).build();
      this.addRenderableWidget(this.disconnectButton);
      if (this.isHubListOpen) {
         this.createHubButtons();
      }
   }

   public void tick() {
      super.tick();
      if (this.minecraft.level != null && this.minecraft.level.getGameTime() % 20L == 0L) {
         this.init();
      }
   }

   private void createHubButtons() {
      this.hubButtons.clear();
      Team team = this.minecraft.player.getTeam();
      if (team != null) {
         String myTeam = team.getName();
         String myDimension = this.minecraft.level.dimension().location().toString();
         int centerX = this.width / 2;
         boolean spawnCosts = ClientData.serverHubSpawnCosts;
         int spawnCost = ClientData.serverHubSpawnCostAmount;

         for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (hub.team.equalsIgnoreCase(myTeam) && hub.constructed && hub.dimension.equals(myDimension)) {
               boolean isBlocked = hub.isBlocked;
               boolean notEnoughMats = spawnCosts && hub.materials < spawnCost;
               String coordText = "X:" + hub.pos.getX() + " Z:" + hub.pos.getZ();
               String pointInfo = this.getNearestPointInfo(hub.pos);
               String status = "";
               if (isBlocked) {
                  status = " [!] BLOCKED";
               } else if (spawnCosts) {
                  status = " [" + hub.materials + "/" + spawnCost + " Mats]";
               }

               Component btnTextComp = Component.literal(coordText + " | " + pointInfo + status);
               if (!isBlocked && !notEnoughMats) {
                  btnTextComp = btnTextComp.copy().withStyle(ChatFormatting.WHITE);
               } else {
                  btnTextComp = btnTextComp.copy().withStyle(ChatFormatting.RED);
               }

               Button btn = Button.builder(btnTextComp, b -> {
                  String payload = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
                  PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest(payload));
                  this.minecraft.player.respawn();
                  this.minecraft.setScreen(null);
               }).bounds(centerX - 105, 0, 210, 22).build();
               if (isBlocked || notEnoughMats) {
                  btn.active = false;
               }

               btn.visible = false;
               this.hubButtons.add(btn);
               this.addRenderableWidget(btn);
            }
         }
      }
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      gui.fill(0, 0, this.width, this.height, 0xCC06080A);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 30, PWPTheme.Colors.TEXT_PRIMARY);
      if (this.cause != null) {
         gui.drawCenteredString(this.font, this.cause, this.width / 2, 48, PWPTheme.Colors.TEXT_SECONDARY);
      }

      // Timer
      long secondsLeft = this.respawnTimeTotal - (System.currentTimeMillis() - this.deathTimestamp) / 1000L;
      if (secondsLeft < 0L) {
         secondsLeft = 0L;
      }
      int timerColor = secondsLeft > 0L ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Colors.SUCCESS_LIGHT;
      int cx = this.width / 2;
      int cy = this.height / 2;

      gui.drawCenteredString(this.font, "RESPAWN IN: " + secondsLeft, cx, cy - 25, timerColor);

      // Timer bar
      int barW = 200;
      int barH = 5;
      int barX = cx - barW / 2;
      int barY = cy - 14;
      float pct = (float) secondsLeft / respawnTimeTotal;
      gui.fill(barX, barY, barX + barW, barY + barH, PWPTheme.Styles.Progress.BG);
      gui.fill(barX, barY, barX + (int)(barW * pct), barY + barH,
         secondsLeft > 0 ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.SUCCESS);

      boolean timerFinished = secondsLeft == 0L;
      this.mainSpawnButton.active = timerFinished;
      boolean hasAtLeastOneHub = false;
      Team team = this.minecraft.player.getTeam();
      if (team != null) {
         String myTeam = team.getName();
         String myDimension = this.minecraft.level.dimension().location().toString();

         for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (hub.team.equalsIgnoreCase(myTeam) && hub.constructed && hub.dimension.equals(myDimension) && !hub.isBlocked) {
               hasAtLeastOneHub = true;
               break;
            }
         }
      }

      this.hubSpawnButton.active = timerFinished && hasAtLeastOneHub;
      boolean hasSquadRally = false;
      boolean currentSquadBlocked = false;
      String currentDim = this.minecraft.level.dimension().location().toString();
      String myName = this.minecraft.player.getScoreboardName();

      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) {
            if (s.rallyPos != null && s.rallyDimension != null && s.rallyDimension.equals(currentDim)) {
               hasSquadRally = true;
               currentSquadBlocked = s.isRallyBlocked;
            }
            break;
         }
      }

      if (hasSquadRally) {
         if (currentSquadBlocked) {
            this.rallySpawnButton.active = false;
            this.rallySpawnButton.setMessage(Component.literal("RALLY BLOCKED").withStyle(ChatFormatting.RED));
         } else {
            this.rallySpawnButton.active = timerFinished;
            this.rallySpawnButton.setMessage(Component.literal("SQUAD RALLY"));
         }
      } else {
         this.rallySpawnButton.active = false;
         this.rallySpawnButton.setMessage(Component.literal("NO RALLY"));
      }

      for (Button b : this.hubButtons) {
         b.visible = false;
      }

      if (this.isHubListOpen && timerFinished && !this.hubButtons.isEmpty()) {
         gui.fill(this.listLeft, this.listTop, this.listRight, this.listBottom, 0xCC0E1117);
         gui.renderOutline(this.listLeft, this.listTop, this.listRight - this.listLeft, this.listBottom - this.listTop, PWPTheme.Colors.BORDER);
         gui.enableScissor(this.listLeft, this.listTop, this.listRight, this.listBottom);
         int currentY = (int)(this.listTop - this.scrollAmount);

         for (Button btn : this.hubButtons) {
            btn.setY(currentY);
            if (currentY + 22 >= this.listTop && currentY <= this.listBottom) {
               btn.visible = true;
               btn.render(gui, mouseX, mouseY, partialTick);
            } else {
               btn.visible = false;
            }
            currentY += 25;
         }

         gui.disableScissor();
         int contentHeight = this.hubButtons.size() * 25;
         if (contentHeight > LIST_HEIGHT) {
            int scrollBarX = this.listLeft - 7;
            int scrollBarWidth = 5;
            gui.fill(scrollBarX, this.listTop, scrollBarX + scrollBarWidth, this.listTop + LIST_HEIGHT, PWPTheme.Styles.ScrollBar.TRACK);
            float ratio = (float) LIST_HEIGHT / contentHeight;
            int thumbHeight = Math.max(10, (int)(LIST_HEIGHT * ratio));
            float maxScroll = contentHeight - LIST_HEIGHT;
            int thumbY = this.listTop + (int)(this.scrollAmount / maxScroll * (LIST_HEIGHT - thumbHeight));
            gui.fill(scrollBarX, thumbY, scrollBarX + scrollBarWidth, thumbY + thumbHeight, PWPTheme.Styles.ScrollBar.THUMB);
            gui.fill(scrollBarX, thumbY, scrollBarX + scrollBarWidth - 1, thumbY + thumbHeight - 1, PWPTheme.Styles.ScrollBar.THUMB_HOVER);
         }
      }

      this.mainSpawnButton.render(gui, mouseX, mouseY, partialTick);
      this.rallySpawnButton.render(gui, mouseX, mouseY, partialTick);
      this.hubSpawnButton.render(gui, mouseX, mouseY, partialTick);
      this.disconnectButton.render(gui, mouseX, mouseY, partialTick);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (this.isHubListOpen && !this.hubButtons.isEmpty()) {
         int contentHeight = this.hubButtons.size() * 25;
         if (contentHeight > LIST_HEIGHT) {
            float maxScroll = contentHeight - LIST_HEIGHT;
            this.scrollAmount -= (float)(delta * 15.0);
            this.scrollAmount = Mth.clamp(this.scrollAmount, 0.0F, maxScroll);
            return true;
         }
      }
      return super.mouseScrolled(mouseX, mouseY, delta);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isHubListOpen && !this.hubButtons.isEmpty()) {
         int scrollBarX = this.listLeft - 7;
         if (mouseX >= scrollBarX - 2 && mouseX <= scrollBarX + 9 && mouseY >= this.listTop && mouseY <= this.listBottom) {
            this.isScrolling = true;
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.isScrolling = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.isScrolling && this.isHubListOpen) {
         int contentHeight = this.hubButtons.size() * 25;
         if (contentHeight > LIST_HEIGHT) {
            float maxScroll = contentHeight - LIST_HEIGHT;
            int barHeight = LIST_HEIGHT;
            float ratio = (float) LIST_HEIGHT / contentHeight;
            int thumbHeight = Math.max(10, (int)(barHeight * ratio));
            float scrollPerPixel = maxScroll / (barHeight - thumbHeight);
            this.scrollAmount += (float)(dragY * scrollPerPixel);
            this.scrollAmount = Mth.clamp(this.scrollAmount, 0.0F, maxScroll);
            return true;
         }
      }
      return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   private String getNearestPointInfo(BlockPos hubPos) {
      if (ClientData.allCapturePoints.isEmpty()) {
         return "Wilderness";
      }
      WarfareWorldData.CapturePoint nearest = null;
      double minDistanceSq = Double.MAX_VALUE;
      for (WarfareWorldData.CapturePoint cp : ClientData.allCapturePoints) {
         Vec3 center = cp.area.getCenter();
         double dSq = hubPos.distToCenterSqr(center.x, center.y, center.z);
         if (dSq < minDistanceSq) {
            minDistanceSq = dSq;
            nearest = cp;
         }
      }
      if (nearest != null) {
         int distMeters = (int)Math.sqrt(minDistanceSq);
         return nearest.name + " (" + distMeters + "m)";
      } else {
         return "Unknown";
      }
   }
}
