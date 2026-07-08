package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.TacticalMapRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.WarfareMapRenderer;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.network.PacketSquadChat;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

public class WarfareDeathScreen extends DeathScreen {
   private static final int SIDEBAR_WIDTH = 170;
   private static final ResourceLocation ARROW_DOWN = new ResourceLocation("pwpwarfare", "textures/gui/arrow_down.png");
   private static final ResourceLocation ARROW_UP = new ResourceLocation("pwpwarfare", "textures/gui/arrow_up.png");
   private static final ResourceLocation LOCK_ICON = new ResourceLocation("pwpwarfare", "textures/gui/squad_lock.png");
   private static final ResourceLocation TICKET_ICON = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");
   private final long deathTimestamp;
   private final int respawnTimeTotal;
   private EditBox chatInput;
   private Button chatModeButton;
   private int chatMode = 1;
   private final Set<Integer> expandedSquads = new HashSet<>();
   private final WarfareMapRenderer mapRenderer = new WarfareMapRenderer();
   private Button applyCmdButton;
   private EditBox nameInput;
   private Button createButton;
   private boolean showContextMenu = false;
   private int contextMenuX = 0;
   private int contextMenuY = 0;
   private String contextTargetPlayer = "";
   private int contextTargetSquadId = -1;
   private String selectedSpawnType = "";
   private Button deployButton;
   private String selectedDisplayName = "NONE";

   public WarfareDeathScreen(Component cause, boolean hardcore) {
      super(cause != null ? cause : Component.literal(""), hardcore);
      if (ClientData.globalDeathTimestamp == 0L) {
         ClientData.globalDeathTimestamp = System.currentTimeMillis();
      }
      this.deathTimestamp = ClientData.globalDeathTimestamp;
      this.respawnTimeTotal = ClientData.RESPAWN_TIME > 0 ? ClientData.RESPAWN_TIME : 10;
      String myName = Minecraft.getInstance().getUser().getName();
      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) this.expandedSquads.add(s.id);
      }
   }

   public boolean isPauseScreen() { return false; }

   protected void init() {
      super.clearWidgets();
      int h = this.height;
      int w = this.width;
      int mapSize = h;
      int mapX = w - mapSize;
      this.mapRenderer.init(mapX, 0, mapSize);

      this.applyCmdButton = this.addRenderableWidget(Button.builder(Component.literal("APPLY FOR CMD"), b -> {
         PacketHandler.INSTANCE.sendToServer(new PacketRequestCMD());
         b.visible = false;
      }).bounds(10, 10, 150, 20).build());

      boolean isInSquad = this.isPlayerInSquad();
      this.nameInput = new EditBox(this.font, 10, h - 90, 150, 20, Component.literal("Squad Name"));
      this.nameInput.setMaxLength(12);
      this.nameInput.setVisible(!isInSquad);
      this.addRenderableWidget(this.nameInput);

      this.createButton = this.addRenderableWidget(Button.builder(Component.literal("Create Squad"), b -> {
         PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, this.nameInput.getValue()));
      }).bounds(10, h - 65, 150, 20).build());
      this.createButton.visible = !isInSquad;

      this.deployButton = this.addRenderableWidget(Button.builder(Component.literal("DEPLOY"), b -> {
         if (!this.selectedSpawnType.isEmpty()) {
            ClientData.globalDeathTimestamp = 0L;
            ClientData.deathFadeStartTime = 0L;
            ClientData.deathFadePlayed = false;
            PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest(this.selectedSpawnType));
            this.minecraft.player.respawn();
            this.minecraft.setScreen(null);
         }
      }).bounds(10, h - 35, 100, 25).build());

      int mapOriginX = w - h;
      int chatX = 180;
      int chatWidth = mapOriginX - chatX - 10;
      this.chatModeButton = this.addRenderableWidget(Button.builder(this.getChatModeText(), b -> {
         this.chatMode = (this.chatMode + 1) % 3;
         b.setMessage(this.getChatModeText());
      }).bounds(chatX, h - 40, 60, 20).build());

      this.chatInput = new EditBox(this.font, chatX + 64, h - 40, chatWidth - 64, 20, Component.literal("Chat"));
      this.chatInput.setMaxLength(100);
      this.addRenderableWidget(this.chatInput);

      this.addRenderableWidget(Button.builder(Component.literal("Quit"), b -> {
         if (this.minecraft.level != null) this.minecraft.level.disconnect();
         this.minecraft.setScreen(new TitleScreen());
      }).bounds(w - 45, 5, 40, 20).build());
   }

   public void tick() {
      super.tick();
      if (this.nameInput != null) this.nameInput.tick();
      if (this.applyCmdButton != null) this.applyCmdButton.visible = this.isApplyCmdVisible();
      boolean isInSquad = this.isPlayerInSquad();
      if (this.nameInput != null && this.nameInput.isVisible() == isInSquad) {
         this.nameInput.setVisible(!isInSquad);
         if (this.createButton != null) this.createButton.visible = !isInSquad;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 257 || keyCode == 335) && this.chatInput.isFocused()) {
         String msg = this.chatInput.getValue().trim();
         if (!msg.isEmpty()) {
            PacketHandler.INSTANCE.sendToServer(new PacketSquadChat(msg, this.chatMode));
            this.chatInput.setValue("");
         }
         return true;
      }
      if ((keyCode == 257 || keyCode == 335) && this.nameInput.isFocused() && this.nameInput.isVisible()) {
         PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, this.nameInput.getValue()));
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      gui.fill(0, 0, this.width, this.height, 0xFF000000);
      this.mapRenderer.render(gui, mx, my, pt);
      gui.fill(0, 0, 310, this.height, 0xAA0E1117);
      gui.fill(0, 0, 170, this.height, 0x22FFFFFF);

      long elapsed = (System.currentTimeMillis() - this.deathTimestamp) / 1000L;
      int secondsLeft = (int)(this.respawnTimeTotal - elapsed);
      if (secondsLeft > 0) {
         this.deployButton.active = false;
         this.deployButton.setMessage(Component.literal("WAIT " + secondsLeft + "s"));
      } else {
         this.deployButton.active = !this.selectedSpawnType.isEmpty();
         this.deployButton.setMessage(Component.literal("DEPLOY"));
      }

      this.renderSquadList(gui, mx, my);
      this.renderTeamHeader(gui);
      this.renderSpawnSelection(gui, mx, my);
      this.renderVoiceActivity(gui);
      this.renderChatArea(gui);

      for (var r : this.renderables) {
         r.render(gui, mx, my, pt);
      }
      if (this.showContextMenu) this.renderContextMenu(gui, mx, my);

      if (ClientData.deathFadeStartTime != 0L) {
         long fadeElapsed = System.currentTimeMillis() - ClientData.deathFadeStartTime;
         float alpha = 0.0F;
         if (fadeElapsed < 1000L) alpha = 1.0F;
         else if (fadeElapsed < 2000L) alpha = 1.0F - (float)(fadeElapsed - 1000L) / 1000.0F;
         else ClientData.deathFadeStartTime = 0L;
         if (alpha > 0.0F) {
            int a = (int)(alpha * 255.0F);
            gui.pose().pushPose();
            gui.pose().translate(0.0F, 0.0F, 1000.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.fill(0, 0, this.width, this.height, a << 24 | 0);
            RenderSystem.disableBlend();
            gui.pose().popPose();
         }
      }
   }

   private void renderChatArea(GuiGraphics gui) {
      int mapOriginX = this.width - this.height;
      int chatX = 180;
      int chatW = mapOriginX - chatX - 10;
      int chatBottom = this.height - 45;
      int maxMsg = 15;
      gui.fill(chatX, chatBottom - maxMsg * 10, chatX + chatW, chatBottom, 0x70000000);
      gui.renderOutline(chatX, chatBottom - maxMsg * 10, chatW, maxMsg * 10, PWPTheme.Colors.BORDER);
      int count = 0;
      for (Component msg : ClientData.menuChatHistory) {
         if (count >= maxMsg) break;
         int y = chatBottom - 10 - count * 10;
         gui.drawString(this.font, msg, chatX + 3, y, PWPTheme.Colors.TEXT_PRIMARY, true);
         count++;
      }
   }

   private void renderVoiceActivity(GuiGraphics gui) {
      long now = System.currentTimeMillis();
      int x = 5;
      int y = this.height / 2 - 40;
      for (Map.Entry<String, Long> e : ClientData.RADIO_SPEAKERS.entrySet()) {
         if (now - e.getValue() >= 500L) continue;
         this.renderSpeakerRow(gui, x, y, e.getKey(), PWPTheme.Colors.ACCENT, new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_radio.png"));
         y += 14;
      }
      for (Map.Entry<String, Long> e : ClientData.SQUAD_SPEAKERS.entrySet()) {
         if (now - e.getValue() >= 500L) continue;
         this.renderSpeakerRow(gui, x, y, e.getKey(), PWPTheme.Colors.TEXT_SECONDARY, new ResourceLocation("pwpwarfare", "textures/gui/voice_icon.png"));
         y += 14;
      }
   }

   private void renderSpeakerRow(GuiGraphics gui, int x, int y, String name, int color, ResourceLocation icon) {
      int tw = this.font.width(name) + 15;
      gui.fill(x, y - 2, x + tw + 4, y + 10, 0x80000000);
      RenderSystem.enableBlend();
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      RenderSystem.setShaderColor(r, g, b, 1.0F);
      gui.blit(icon, x + 3, y, 0.0F, 0.0F, 8, 8, 8, 8);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      gui.drawString(this.font, name, x + 14, y, color, false);
   }

   private void renderTeamHeader(GuiGraphics gui) {
      int startX = 180;
      int startY = 10;
      String teamName = this.getPlayerTeam().toUpperCase();
      int tickets = teamName.contains("BLUE") ? ClientData.BLUE_TICKETS : ClientData.RED_TICKETS;
      String faction = teamName.contains("BLUE") ? ClientData.BLUE_FACTION : ClientData.RED_FACTION;
      String customName = teamName.contains("BLUE") ? ClientData.customBlueName : ClientData.customRedName;
      ResourceLocation flagTex = this.getFlagTexture(faction);
      if (flagTex != null) {
         RenderSystem.enableBlend();
         gui.blit(flagTex, startX, startY, 32, 18, 0.0F, 0.0F, 64, 36, 64, 36);
      }
      gui.drawString(this.font, customName, startX + 38, startY, teamName.contains("BLUE") ? 0x5555FF : 0xFF5555, true);
      RenderSystem.enableBlend();
      gui.blit(TICKET_ICON, startX + 38, startY + 11, 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
      gui.drawString(this.font, String.valueOf(tickets), startX + 50, startY + 11, PWPTheme.Colors.TEXT_ACCENT, true);
   }

   private void renderSpawnSelection(GuiGraphics gui, int mx, int my) {
      int startX = 180;
      int startY = 55;
      gui.drawString(this.font, "SELECT SPAWN POINT:", startX, startY - 15, 0xFFAA00, false);
      this.drawSpawnOption(gui, startX, startY, 110, 24, "MAIN BASE", "MAIN", mx, my, true, false);
      boolean rallyBlocked = this.isMyRallyBlocked();
      boolean rallyValid = this.hasValidRally() && !rallyBlocked;
      this.drawSpawnOption(gui, startX, startY += 30, 110, 24, "SQUAD RALLY", "RALLY", mx, my, rallyValid, rallyBlocked);
      gui.drawString(this.font, "AVAILABLE HUBS:", startX, (startY += 40) - 12, 0xAAAAAA, false);
      Team team = this.minecraft.player.getTeam();
      if (team != null) {
         String myTeam = team.getName();
         String myDim = this.minecraft.level.dimension().location().toString();
         int hubIdx = 1;
         for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || !hub.dimension.equals(myDim)) continue;
            String id = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
            boolean hubBlocked = hub.isBlocked;
            boolean canAfford = !ClientData.serverHubSpawnCosts || hub.materials >= ClientData.serverHubSpawnCostAmount;
            this.drawSpawnOption(gui, startX, startY, 110, 20, "HUBS " + hubIdx, id, mx, my, !hubBlocked && canAfford, hubBlocked);
            startY += 24;
            hubIdx++;
         }
      }
   }

   private void drawSpawnOption(GuiGraphics gui, int x, int y, int w, int h, String label, String id, int mx, int my, boolean active, boolean isBlocked) {
      boolean hovered = active && mx >= x && mx <= x + w && my >= y && my <= y + h;
      boolean selected = this.selectedSpawnType.equals(id);
      int color = isBlocked ? 0xFF5555 : (active ? (selected ? PWPTheme.Colors.ACCENT : (hovered ? PWPTheme.Colors.TEXT_PRIMARY : 0xBBBBBB)) : 0x555555);
      int bg = isBlocked ? 0x60FF0000 : (selected ? 0x4455FF55 : (active ? 0x22FFFFFF : 0x11000000));
      String finalLabel = isBlocked ? label + " BLOCKED" : label;
      gui.fill(x, y, x + w, y + h, bg);
      gui.renderOutline(x, y, w, h, color);
      gui.drawCenteredString(this.font, finalLabel, x + w / 2, y + (h - 8) / 2, color);
   }

   private void renderSquadList(GuiGraphics gui, int mouseX, int mouseY) {
      String myName = this.minecraft.player.getScoreboardName();
      boolean amIInSquad = this.isPlayerInSquad();
      int teamCMDId = this.getPlayerTeam().toUpperCase().contains("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
      int currentY = this.isApplyCmdVisible() ? 35 : 10;
      List<WarfareWorldData.Squad> squads = this.getFilteredSquads();
      int index = 1;
      for (WarfareWorldData.Squad squad : squads) {
         boolean isMySquad = squad.members.contains(myName);
         boolean amILeader = squad.leader.equals(myName);
         boolean isExpanded = this.expandedSquads.contains(squad.id);
         boolean isCMD = squad.id == teamCMDId && teamCMDId != -1;
         String prefix = isCMD ? "[CMD] " : "";
         gui.drawString(this.font, index + ".", 5, currentY, PWPTheme.Colors.TEXT_PRIMARY, false);
         gui.drawString(this.font, prefix + squad.name + " (" + squad.members.size() + "/9)", 25, currentY, isCMD ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_SECONDARY, false);
         String action = "";
         int actionColor = -1;
         boolean clickable = true;
         if (isMySquad) { action = "LEAVE"; actionColor = PWPTheme.Colors.SUCCESS; }
         else if (!amIInSquad) {
            if (squad.isLocked) { action = "LOCKED"; actionColor = PWPTheme.Colors.WARNING; clickable = false; }
            else if (squad.members.size() >= 9) { action = "FULL"; actionColor = PWPTheme.Colors.TEXT_DIM; clickable = false; }
            else { action = "JOIN"; actionColor = PWPTheme.Colors.ACCENT; }
         }
         int actionX = 0;
         if (!action.isEmpty()) {
            int aw = this.font.width(action);
            actionX = SIDEBAR_WIDTH - aw - 10;
            boolean hover = mouseX >= actionX && mouseX <= actionX + aw && mouseY >= currentY && mouseY <= currentY + 9;
            gui.drawString(this.font, action, actionX, currentY, hover && clickable ? PWPTheme.Colors.TEXT_PRIMARY : actionColor, false);
         }
         int arrowX = (actionX > 0 ? actionX : 160) - 12;
         RenderSystem.enableBlend();
         if (isExpanded) { RenderSystem.setShaderColor(1, 0.8F, 0.2F, 1); gui.blit(ARROW_DOWN, arrowX, currentY + 1, 0, 0, 8, 8, 8, 8); }
         else { RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 1); gui.blit(ARROW_UP, arrowX, currentY + 1, 0, 0, 8, 8, 8, 8); }
         RenderSystem.setShaderColor(1, 1, 1, 1);
         if (amILeader || squad.isLocked) {
            int lockX = arrowX - 12;
            if (squad.isLocked) RenderSystem.setShaderColor(1, 0.8F, 0.2F, 1); else RenderSystem.setShaderColor(0.6F, 0.6F, 0.6F, 1);
            gui.blit(LOCK_ICON, lockX, currentY + 1, 0, 0, 8, 8, 8, 8);
            RenderSystem.setShaderColor(1, 1, 1, 1);
         }
         currentY += 12;
         if (isExpanded) {
            for (String member : this.getSortedMembers(squad)) {
               boolean isOnline = this.minecraft.getConnection().getPlayerInfo(member) != null;
               int col = PWPTheme.Colors.TEXT_PRIMARY;
               if (member.equals(squad.leader)) col = PWPTheme.Colors.TEXT_SECONDARY;
               else if (member.equals(squad.bravoLeader)) col = 0x55FF55;
               else if (squad.bravoMembers.contains(member)) col = 0x55AA55;
               else if (member.equals(squad.charlieLeader)) col = PWPTheme.Colors.ACCENT;
               else if (squad.charlieMembers.contains(member)) col = 0x999900;
               if (!isOnline) col = 0x666666;
               int xOff = 30;
               if (isMySquad && member.equals(myName)) {
                  boolean btnH = mouseX >= xOff && mouseX <= xOff + 10 && mouseY >= currentY && mouseY <= currentY + 10;
                  gui.fill(xOff, currentY, xOff + 10, currentY + 10, btnH ? 0xFF666666 : 0xFF444444);
                  gui.drawString(this.font, "K", xOff + 2, currentY + 1, PWPTheme.Colors.TEXT_PRIMARY, false);
                  xOff += 14;
               }
               String kName = ClientData.playerKits.getOrDefault(member, "Unassigned");
               if (!kName.equals("Unassigned") && !kName.isEmpty()) {
                  ResourceLocation kitI = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + kName.toLowerCase().replace(" ", "_") + ".png");
                  gui.blit(kitI, xOff, currentY, 0, 0, 10, 10, 10, 10);
                  xOff += 12;
               }
               gui.drawString(this.font, member, xOff, currentY + 1, col, false);
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
      for (WarfareWorldData.Squad sq : ClientData.clientSquads) { if (sq.id == this.contextTargetSquadId) { s = sq; break; } }
      if (s == null) { this.showContextMenu = false; return; }
      boolean amISL = s.leader.equals(myName), amIBFTL = s.bravoLeader.equals(myName), amICFTL = s.charlieLeader.equals(myName);
      List<String> opts = new ArrayList<>();
      if (amISL) {
         opts.add("Promote to SL");
         if (!s.bravoLeader.equals(this.contextTargetPlayer) && !s.charlieLeader.equals(this.contextTargetPlayer)) { opts.add("Set FTL Bravo"); opts.add("Set FTL Charlie"); }
         opts.add("Add to Bravo"); opts.add("Add to Charlie"); opts.add("Remove from FT"); opts.add("Kick from Squad");
      } else if (amIBFTL) {
         if (!s.leader.equals(this.contextTargetPlayer) && !s.charlieLeader.equals(this.contextTargetPlayer)) opts.add("Pass FTL Bravo");
         opts.add("Add to Bravo"); opts.add("Remove from FT");
      } else if (amICFTL) {
         if (!s.leader.equals(this.contextTargetPlayer) && !s.bravoLeader.equals(this.contextTargetPlayer)) opts.add("Pass FTL Charlie");
         opts.add("Add to Charlie"); opts.add("Remove from FT");
      }
      if (opts.isEmpty()) { this.showContextMenu = false; return; }
      int w = 100, h = opts.size() * 12 + 4;
      gui.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + w, this.contextMenuY + h, 0xAA111111);
      gui.renderOutline(this.contextMenuX, this.contextMenuY, w, h, PWPTheme.Colors.BORDER);
      for (int i = 0; i < opts.size(); i++) {
         int y = this.contextMenuY + 2 + i * 12;
         boolean hover = mx >= this.contextMenuX && mx <= this.contextMenuX + w && my >= y && my < y + 12;
         if (hover) gui.fill(this.contextMenuX + 1, y, this.contextMenuX + w - 1, y + 12, 0x44444444);
         gui.drawString(this.font, opts.get(i), this.contextMenuX + 4, y + 2, hover ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY, false);
      }
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (this.showContextMenu) {
         if (btn == 0) this.processContextMenuClick(mx, my);
         this.showContextMenu = false;
         return true;
      }
      if (super.mouseClicked(mx, my, btn)) {
         this.mapRenderer.selectedSpawnId = this.selectedSpawnType;
         return true;
      }
      if (mx < 170.0 && my < (double)(this.height - 100)) {
         this.handleSquadListInteraction(mx, my, btn);
         return true;
      }
      if (mx >= 180.0 && mx <= 300.0 && btn == 0 && this.handleSpawnButtons(mx, my)) return true;
      if (this.mapRenderer.isMouseOver(mx, my)) {
         if (btn == 0) {
            String spawn = this.getSpawnPointUnderMouse(mx, my);
            if (spawn != null) { this.selectedSpawnType = spawn; this.mapRenderer.selectedSpawnId = spawn; this.playClickSound(); return true; }
         }
         if (btn == 1) { this.handleMapRightClick(mx, my); return true; }
         return this.mapRenderer.mouseClicked(mx, my, btn);
      }
      return false;
   }

   private String getSpawnPointUnderMouse(double mx, double my) {
      Minecraft mc = Minecraft.getInstance();
      String myName = mc.player.getScoreboardName();
      double bpp = this.mapRenderer.getBlocksPerPixel();
      double cx = this.mapRenderer.getCenterX(mc.player);
      double cz = this.mapRenderer.getCenterZ(mc.player);
      String myTeam = this.getPlayerTeam().toUpperCase();
      String cd = mc.level.dimension().location().toString();
      BlockPos ms = myTeam.equals("BLUE") ? ClientData.blueSpawns.get(cd) : ClientData.redSpawns.get(cd);
      if (ms != null && this.isIconHit(ms, mx, my, cx, cz, bpp)) return "MAIN";
      for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
         if (sq.members.contains(myName) && sq.rallyPos != null && !sq.isRallyBlocked && this.isIconHit(sq.rallyPos, mx, my, cx, cz, bpp)) return "RALLY";
      }
      for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
         if (hub.team.equalsIgnoreCase(myTeam) && hub.constructed && !hub.isBlocked && this.isIconHit(hub.pos, mx, my, cx, cz, bpp))
            return "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
      }
      return null;
   }

   private boolean isIconHit(BlockPos pos, double mx, double my, double cx, double cz, double bpp) {
      int mapOriginX = this.width - this.height;
      double dx = (pos.getX() + 0.5 - cx) / bpp;
      int px = (int)(mapOriginX + this.height / 2.0 + dx);
      double dz = (pos.getZ() + 0.5 - cz) / bpp;
      int py = (int)(this.height / 2.0 + dz);
      double distSq = (mx - px) * (mx - px) + (my - py) * (my - py);
      return distSq < 144.0;
   }

   private void handleSquadListInteraction(double mx, double my, int btn) {
      String myName = this.minecraft.player.getScoreboardName();
      String myTeam = this.getPlayerTeam().toUpperCase();
      String myDim = this.minecraft.level.dimension().location().toString();
      boolean amIInSquad = this.isPlayerInSquad();
      int currentY = this.isApplyCmdVisible() ? 35 : 10;
      List<WarfareWorldData.Squad> squads = this.getFilteredSquads();
      for (WarfareWorldData.Squad squad : squads) {
         boolean isMySquad = squad.members.contains(myName);
         boolean amILeader = squad.leader.equals(myName);
         boolean isExpanded = this.expandedSquads.contains(squad.id);
         if (my >= currentY && my <= currentY + 11) {
            if (btn == 0) {
               int actionX = SIDEBAR_WIDTH - 50;
               if (mx >= actionX) {
                  if (isMySquad) PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(2, squad.id, ""));
                  else if (!squad.isLocked && squad.members.size() < 9 && !amIInSquad) PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(1, squad.id, ""));
               } else {
                  int arrowX = 115;
                  if (mx >= arrowX - 15 && mx <= arrowX + 15) {
                     if (isExpanded) this.expandedSquads.remove(squad.id); else this.expandedSquads.add(squad.id);
                  } else if (amILeader && mx >= arrowX - 30 && mx < arrowX - 15) {
                     PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(5, squad.id, ""));
                  }
               }
               this.playClickSound();
            }
            return;
         }
         currentY += 12;
         if (isExpanded) {
            for (String member : this.getSortedMembers(squad)) {
               if (my >= currentY && my <= currentY + 11) {
                  if (btn == 1) {
                     boolean amFTL = squad.bravoLeader.equals(myName) || squad.charlieLeader.equals(myName);
                     if ((amILeader || amFTL) && isMySquad && !member.equals(myName)) {
                        this.contextTargetPlayer = member; this.contextTargetSquadId = squad.id;
                        this.contextMenuX = (int)mx; this.contextMenuY = (int)my;
                        this.showContextMenu = true; this.playClickSound();
                     }
                  } else if (btn == 0 && isMySquad && member.equals(myName) && mx >= 30 && mx <= 45) {
                     PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu()); this.playClickSound();
                  }
                  return;
               }
               currentY += 12;
            }
            currentY += 4;
         }
         currentY += 4;
      }
   }

   private boolean handleSpawnButtons(double mx, double my) {
      int y = 55;
      if (my >= y && my <= y + 24) {
         this.selectedSpawnType = "MAIN"; this.mapRenderer.selectedSpawnId = "MAIN"; this.playClickSound();
         return true;
      }
      if (my >= (y += 30) && my <= y + 24) {
         if (this.hasValidRally() && !this.isMyRallyBlocked()) {
            this.selectedSpawnType = "RALLY"; this.mapRenderer.selectedSpawnId = "RALLY"; this.playClickSound();
         }
         return true;
      }
      y += 40;
      Team team = this.minecraft.player.getTeam();
      if (team != null) {
         String myTeam = team.getName();
         String myDim = this.minecraft.level.dimension().location().toString();
         for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || !hub.dimension.equals(myDim)) continue;
            if (my >= y && my <= y + 20) {
               if (!hub.isBlocked) {
                  String id = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
                  this.selectedSpawnType = id; this.mapRenderer.selectedSpawnId = id; this.playClickSound();
               }
               return true;
            }
            y += 24;
         }
      }
      return false;
   }

   private void handleMapRightClick(double mx, double my) {
      if (!this.isSquadLeaderOrFTL(this.minecraft.player)) return;
      double bpp = this.mapRenderer.getBlocksPerPixel();
      double cx = this.mapRenderer.getCenterX(this.minecraft.player);
      double cz = this.mapRenderer.getCenterZ(this.minecraft.player);
      int mapOriginX = this.width - this.height;
      int wx = (int)(cx + (mx - (mapOriginX + this.height / 2.0)) * bpp);
      int wz = (int)(cz + (my - this.height / 2.0) * bpp);
      this.minecraft.setScreen(new TacticalMapRadialScreen(wx, wz));
   }

   private void processContextMenuClick(double mx, double my) {
      WarfareWorldData.Squad s = null;
      for (WarfareWorldData.Squad sq : ClientData.clientSquads) { if (sq.id == this.contextTargetSquadId) { s = sq; break; } }
      if (s == null) return;
      String myName = this.minecraft.player.getScoreboardName();
      boolean amISL = s.leader.equals(myName), amIBFTL = s.bravoLeader.equals(myName), amICFTL = s.charlieLeader.equals(myName);
      List<String> opts = new ArrayList<>();
      if (amISL) {
         opts.add("Promote to SL");
         if (!s.bravoLeader.equals(this.contextTargetPlayer) && !s.charlieLeader.equals(this.contextTargetPlayer)) { opts.add("Set FTL Bravo"); opts.add("Set FTL Charlie"); }
         opts.add("Add to Bravo"); opts.add("Add to Charlie"); opts.add("Remove from FT"); opts.add("Kick from Squad");
      } else if (amIBFTL) {
         if (!s.leader.equals(this.contextTargetPlayer) && !s.charlieLeader.equals(this.contextTargetPlayer)) opts.add("Pass FTL Bravo");
         opts.add("Add to Bravo"); opts.add("Remove from FT");
      } else if (amICFTL) {
         if (!s.leader.equals(this.contextTargetPlayer) && !s.bravoLeader.equals(this.contextTargetPlayer)) opts.add("Pass FTL Charlie");
         opts.add("Add to Charlie"); opts.add("Remove from FT");
      }
      int w = 100, h = opts.size() * 12 + 4;
      if (mx < this.contextMenuX || mx > this.contextMenuX + w || my < this.contextMenuY || my > this.contextMenuY + h) return;
      int idx = (int)((my - this.contextMenuY - 2) / 12);
      if (idx < 0 || idx >= opts.size()) return;
      String opt = opts.get(idx);
      int action = 0;
      if (opt.equals("Promote to SL")) action = 4;
      else if (opt.equals("Set FTL Bravo") || opt.equals("Pass FTL Bravo")) action = 6;
      else if (opt.equals("Set FTL Charlie") || opt.equals("Pass FTL Charlie")) action = 7;
      else if (opt.equals("Add to Bravo")) action = 8;
      else if (opt.equals("Add to Charlie")) action = 9;
      else if (opt.equals("Remove from FT")) action = 10;
      else if (opt.equals("Kick from Squad")) action = 3;
      if (action > 0) PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(action, s.id, this.contextTargetPlayer));
      this.playClickSound();
   }

   public boolean mouseScrolled(double mx, double my, double delta) {
      return this.mapRenderer.isMouseOver(mx, my) ? this.mapRenderer.mouseScrolled(mx, my, delta) : super.mouseScrolled(mx, my, delta);
   }
   public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
      return this.mapRenderer.mouseDragged(mx, my, btn, dx, dy) || super.mouseDragged(mx, my, btn, dx, dy);
   }
   public boolean mouseReleased(double mx, double my, int btn) {
      this.mapRenderer.mouseReleased(btn); return super.mouseReleased(mx, my, btn);
   }

   private boolean isMyRallyBlocked() {
      String myName = this.minecraft.player.getScoreboardName();
      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) return s.isRallyBlocked;
      }
      return false;
   }

   private boolean isApplyCmdVisible() {
      String myName = this.minecraft.player.getScoreboardName();
      String myTeam = this.getPlayerTeam().toUpperCase();
      boolean isBlue = myTeam.contains("BLUE");
      int cmdId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
      boolean voteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
      boolean isLeader = ClientData.clientSquads.stream().anyMatch(s -> s.leader.equals(myName));
      return isLeader && cmdId == -1 && !voteActive;
   }

   private List<WarfareWorldData.Squad> getFilteredSquads() {
      String myTeam = this.getPlayerTeam().toUpperCase();
      String myDim = this.minecraft.level.dimension().location().toString();
      int cmdId = myTeam.contains("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
      List<WarfareWorldData.Squad> list = ClientData.clientSquads.stream()
         .filter(s -> s.team.equalsIgnoreCase(myTeam) && s.dimension != null && s.dimension.equals(myDim))
         .collect(Collectors.toList());
      list.sort((a, b) -> {
         if (a.id == cmdId && cmdId != -1) return -1;
         return b.id == cmdId && cmdId != -1 ? 1 : Integer.compare(a.id, b.id);
      });
      return list;
   }

   private List<String> getSortedMembers(WarfareWorldData.Squad squad) {
      List<String> r = new ArrayList<>();
      if (!squad.leader.isEmpty() && squad.members.contains(squad.leader)) r.add(squad.leader);
      for (String m : squad.members) { if (!m.equals(squad.leader) && !squad.bravoMembers.contains(m) && !squad.charlieMembers.contains(m)) r.add(m); }
      if (!squad.bravoLeader.isEmpty() && squad.members.contains(squad.bravoLeader)) r.add(squad.bravoLeader);
      for (String m : squad.bravoMembers) { if (!m.equals(squad.bravoLeader) && squad.members.contains(m)) r.add(m); }
      if (!squad.charlieLeader.isEmpty() && squad.members.contains(squad.charlieLeader)) r.add(squad.charlieLeader);
      for (String m : squad.charlieMembers) { if (!m.equals(squad.charlieLeader) && squad.members.contains(m)) r.add(m); }
      return r;
   }

   private boolean hasValidRally() {
      String myName = this.minecraft.player.getScoreboardName();
      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.members.contains(myName)) return s.rallyPos != null && !s.isRallyBlocked;
      }
      return false;
   }

   private boolean isSquadLeaderOrFTL(Player player) {
      String n = player.getScoreboardName();
      for (WarfareWorldData.Squad s : ClientData.clientSquads) {
         if (s.leader.equals(n) || s.bravoLeader.equals(n) || s.charlieLeader.equals(n)) return true;
      }
      return false;
   }

   private boolean isPlayerInSquad() {
      String n = this.minecraft.player.getScoreboardName();
      for (WarfareWorldData.Squad s : ClientData.clientSquads) { if (s.members.contains(n)) return true; }
      return false;
   }

   private String getPlayerTeam() {
      return this.minecraft.player.getTeam() != null ? this.minecraft.player.getTeam().getName() : "NEUTRAL";
   }

   private ResourceLocation getFlagTexture(String faction) {
      if (faction == null || faction.equalsIgnoreCase("none")) return null;
      return new ResourceLocation("pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png");
   }

   private Component getChatModeText() {
      switch (this.chatMode) {
         case 0: return Component.literal("ALL").withStyle(ChatFormatting.LIGHT_PURPLE);
         case 2: return Component.literal("SQUAD").withStyle(ChatFormatting.GREEN);
         default: return Component.literal("TEAM").withStyle(ChatFormatting.BLUE);
      }
   }

   private void playClickSound() {
      this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }
}
