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
import net.minecraft.client.gui.components.Renderable;
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
    private final long deathTimestamp;
    private final int respawnTimeTotal;
    private EditBox chatInput;
    private Button chatModeButton;
    private int chatMode = 1;
    private final Set<Integer> expandedSquads = new HashSet<>();
    private final WarfareMapRenderer mapRenderer = new WarfareMapRenderer();
    private static final int SIDEBAR_WIDTH = 170;
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
    private static final ResourceLocation ARROW_DOWN = new ResourceLocation("pwpwarfare", "textures/gui/arrow_down.png");
    private static final ResourceLocation ARROW_UP = new ResourceLocation("pwpwarfare", "textures/gui/arrow_up.png");
    private static final ResourceLocation LOCK_ICON = new ResourceLocation("pwpwarfare", "textures/gui/squad_lock.png");
    private static final ResourceLocation TICKET_ICON = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");
    private static final ResourceLocation VOICE_ICON = new ResourceLocation("pwpwarfare", "textures/gui/voice_icon.png");
    private static final ResourceLocation RADIO_ICON = new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_radio.png");

    public WarfareDeathScreen(Component cause, boolean hardcore) {
        super(cause != null ? cause : Component.literal(""), hardcore);
        ClientData.globalDeathTimestamp = System.currentTimeMillis();
        this.deathTimestamp = ClientData.globalDeathTimestamp;
        this.respawnTimeTotal = ClientData.RESPAWN_TIME > 0 ? ClientData.RESPAWN_TIME : 10;
        String myName = Minecraft.getInstance().getUser().getName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            this.expandedSquads.add(s.id);
        }
    }

    private int getMapSize() {
        int sidebarTarget = Math.min(310, this.width - 170);
        sidebarTarget = Math.max(280, sidebarTarget);
        int maxMapWidth = this.width - sidebarTarget;
        int maxMapHeight = this.height - 80;
        return Math.max(100, Math.min(maxMapHeight, maxMapWidth));
    }

    private int getMapY() {
        return 30;
    }

    private int getMapOriginX() {
        return this.width - this.getMapSize();
    }

    private int getChatMaxMessages() {
        return 3;
    }

    protected void init() {
        this.clearWidgets();
        int mapSize = this.getMapSize();
        int mapX = this.getMapOriginX();
        int mapY = this.getMapY();
        this.mapRenderer.init(mapX, mapY, mapSize);
        this.applyCmdButton = this.addRenderableWidget(new SquadButton(10, 10, 150, 20, Component.literal("APPLY FOR CMD"), b -> {
            PacketHandler.INSTANCE.sendToServer(new PacketRequestCMD());
            b.visible = false;
        }));
        boolean isInSquad = this.isPlayerInSquad();
        this.nameInput = new EditBox(this.font, 10, this.height - 90, 150, 20, Component.literal("Squad Name"));
        this.nameInput.setMaxLength(12);
        this.nameInput.setVisible(!isInSquad);
        this.addRenderableWidget(this.nameInput);
        this.createButton = this.addRenderableWidget(new SquadButton(10, this.height - 65, 150, 20, Component.literal("Create Squad"), b -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, this.nameInput.getValue()))));
        this.createButton.visible = !isInSquad;
        this.deployButton = this.addRenderableWidget(new DeployButton(10, this.height - 35, 100, 25, Component.literal("DEPLOY"), b -> {
            if (!this.selectedSpawnType.isEmpty()) {
                ClientData.globalDeathTimestamp = 0L;
                ClientData.deathFadeStartTime = 0L;
                ClientData.deathFadePlayed = false;
                PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest(this.selectedSpawnType));
                this.minecraft.player.respawn();
                this.minecraft.setScreen(null);
            }
        }));
        int mapOriginX = this.getMapOriginX();
        int chatX = 180;
        int chatAvailableWidth = Math.max(60, mapOriginX - chatX - 10);
        int modeBtnWidth = 60;
        int gap = 4;
        int inputY = this.height - 25;
        this.chatModeButton = this.addRenderableWidget(new SquadButton(chatX, inputY, modeBtnWidth, 20, this.getChatModeText(), b -> {
            this.chatMode = (this.chatMode + 1) % 3;
            b.setMessage(this.getChatModeText());
        }));
        this.chatInput = new EditBox(this.font, chatX + modeBtnWidth + gap, inputY, chatAvailableWidth - modeBtnWidth - gap, 20, Component.literal("Chat"));
        this.chatInput.setMaxLength(100);
        this.addRenderableWidget(this.chatInput);
        this.addRenderableWidget(new SquadButton(this.width - 45, 5, 40, 20, Component.literal("Quit"), b -> {
            if (this.minecraft.level != null) {
                this.minecraft.level.disconnect();
            }
            this.minecraft.setScreen(new TitleScreen());
        }));
    }

    private Component getChatModeText() {
        switch (this.chatMode) {
            case 0: {
                return Component.literal("ALL").withStyle(ChatFormatting.LIGHT_PURPLE);
            }
            case 2: {
                return Component.literal("SQUAD").withStyle(ChatFormatting.GREEN);
            }
        }
        return Component.literal("TEAM").withStyle(ChatFormatting.BLUE);
    }

    public void tick() {
        super.tick();
        if (this.nameInput != null) {
            this.nameInput.tick();
        }
        if (this.applyCmdButton != null) {
            this.applyCmdButton.visible = this.isApplyCmdVisible();
        }
        boolean isInSquad = this.isPlayerInSquad();
        if (this.nameInput != null && this.nameInput.isVisible() == isInSquad) {
            this.nameInput.setVisible(!isInSquad);
            this.createButton.visible = !isInSquad;
        }
    }

    private boolean isMyRallyBlocked() {
        String myName = this.minecraft.player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            return s.isRallyBlocked;
        }
        return false;
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderChatArea(GuiGraphics gui) {
        int mapOriginX = this.getMapOriginX();
        int chatX = 180;
        int chatAvailableWidth = Math.max(60, mapOriginX - chatX - 10);
        int inputY = this.height - 25;
        int chatBottomY = inputY - 5;
        int maxMessages = this.getChatMaxMessages();
        int boxLeft = chatX;
        int boxTop = chatBottomY - maxMessages * 10;
        int boxRight = chatX + chatAvailableWidth;
        int boxBottom = chatBottomY;
        gui.fill(boxLeft, boxTop, boxRight, boxBottom, 0x70000000);
        gui.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, 0xFFFFFFFF);
        int count = 0;
        for (Component msg : ClientData.menuChatHistory) {
            if (count >= maxMessages) break;
            int y = chatBottomY - 10 - count * 10;
            gui.drawString(this.font, msg, chatX + 3, y, -1, true);
            ++count;
        }
    }

    public void render(GuiGraphics gui, int mx, int my, float pt) {
        gui.fill(0, 0, this.width, this.height, -16777216);
        this.mapRenderer.render(gui, mx, my, pt);
        int sidebarEnd = this.getMapOriginX();
        gui.fill(0, 0, sidebarEnd, this.height, -1442840576);
        gui.fill(0, 0, 170, this.height, 0x22FFFFFF);
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - this.deathTimestamp) / 1000L;
        int secondsLeft = (int)((long)this.respawnTimeTotal - elapsedSeconds);
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
        for (Renderable renderable : this.renderables) {
            renderable.render(gui, mx, my, pt);
        }
        if (this.showContextMenu) {
            this.renderContextMenu(gui, mx, my);
        }
        if (ClientData.deathFadeStartTime != 0L) {
            long fadeElapsed = System.currentTimeMillis() - ClientData.deathFadeStartTime;
            float alpha = 0.0f;
            if (fadeElapsed < 1000L) {
                alpha = 1.0f;
            } else if (fadeElapsed < 2000L) {
                alpha = 1.0f - (float)(fadeElapsed - 1000L) / 1000.0f;
            } else {
                ClientData.deathFadeStartTime = 0L;
            }
            if (alpha > 0.0f) {
                int alphaInt = (int)(alpha * 255.0f);
                gui.pose().pushPose();
                gui.pose().translate(0.0f, 0.0f, 1000.0f);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                gui.fill(0, 0, this.width, this.height, alphaInt << 24 | 0);
                RenderSystem.disableBlend();
                gui.pose().popPose();
            }
        }
    }

    private void renderVoiceActivity(GuiGraphics gui) {
        long now = System.currentTimeMillis();
        int x = 5;
        int y = this.height / 2 - 40;
        for (Map.Entry<String, Long> entry : ClientData.RADIO_SPEAKERS.entrySet()) {
            if (now - entry.getValue() >= 500L) continue;
            this.renderSpeakerRow(gui, x, y, entry.getKey(), -256, RADIO_ICON);
            y += 14;
        }
        for (Map.Entry<String, Long> entry : ClientData.SQUAD_SPEAKERS.entrySet()) {
            if (now - entry.getValue() >= 500L) continue;
            this.renderSpeakerRow(gui, x, y, entry.getKey(), -11141291, VOICE_ICON);
            y += 14;
        }
    }

    private void renderSpeakerRow(GuiGraphics gui, int x, int y, String name, int color, ResourceLocation icon) {
        int tw = this.font.width(name) + 15;
        gui.fill(x, y - 2, x + tw + 4, y + 10, Integer.MIN_VALUE);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)1.0f);
        gui.blit(icon, x + 3, y, 0.0f, 0.0f, 8, 8, 8, 8);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        gui.drawString(this.font, name, x + 14, y, color, false);
    }

    private String getSpawnPointUnderMouse(double mouseX, double mouseY) {
        BlockPos myMainPos;
        Minecraft mc = Minecraft.getInstance();
        String myName = mc.player.getScoreboardName();
        double bpp = this.mapRenderer.getBlocksPerPixel();
        double cx = this.mapRenderer.getCenterX(mc.player);
        double cz = this.mapRenderer.getCenterZ(mc.player);
        String myTeam = this.getPlayerTeam().toUpperCase();
        String currentDim = mc.level.dimension().location().toString();
        BlockPos blockPos = myMainPos = myTeam.equals("BLUE") ? ClientData.blueSpawns.get(currentDim) : ClientData.redSpawns.get(currentDim);
        if (myMainPos != null && this.isIconHit(myMainPos, mouseX, mouseY, cx, cz, bpp)) {
            return "MAIN";
        }
        for (WarfareWorldData.Squad squad : ClientData.clientSquads) {
            if (!squad.members.contains(myName) || squad.rallyPos == null || squad.isRallyBlocked || !this.isIconHit(squad.rallyPos, mouseX, mouseY, cx, cz, bpp)) continue;
            return "RALLY";
        }
        for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || hub.isBlocked || !this.isIconHit(hub.pos, mouseX, mouseY, cx, cz, bpp)) continue;
            return "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
        }
        return null;
    }

    private boolean isIconHit(BlockPos pos, double mx, double my, double cx, double cz, double bpp) {
        int mapSize = this.getMapSize();
        int mapOriginX = this.getMapOriginX();
        int mapY = this.getMapY();
        double dx = ((double)pos.getX() + 0.5 - cx) / bpp;
        int px = (int)((double)mapOriginX + (double)mapSize / 2.0 + dx);
        double dz = ((double)pos.getZ() + 0.5 - cz) / bpp;
        int py = (int)((double)mapY + (double)mapSize / 2.0 + dz);
        double distSq = (mx - (double)px) * (mx - (double)px) + (my - (double)py) * (my - (double)py);
        return distSq < 144.0;
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
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            gui.blit(flagTex, startX, startY, 32, 18, 0.0f, 0.0f, 64, 36, 64, 36);
        }
        gui.drawString(this.font, customName, startX + 38, startY, teamName.contains("BLUE") ? 0x5555FF : 0xFF5555, true);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        gui.blit(TICKET_ICON, startX + 38, startY + 11, 0.0f, 0.0f, 8, 8, 8, 8);
        gui.drawString(this.font, String.valueOf(tickets), startX + 50, startY + 11, 16766720, true);
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
                boolean active = !hubBlocked && canAfford;
                this.drawSpawnOption(gui, startX, startY, 110, 20, "HUBS " + hubIdx, id, mx, my, active, hubBlocked);
                startY += 24;
                ++hubIdx;
            }
        }
    }

    private void drawSpawnOption(GuiGraphics gui, int x, int y, int w, int h, String label, String id, int mx, int my, boolean active, boolean isBlocked) {
        boolean hovered = active && mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean selected = this.selectedSpawnType.equals(id);
        int color = isBlocked ? -43691 : (active ? (selected ? -11141291 : (hovered ? -1 : 0xBBBBBB)) : 0x555555);
        int bg = isBlocked ? 0x60FF0000 : (selected ? 0x4455FF55 : (active ? 0x22FFFFFF : 0x11000000));
        String finalLabel = isBlocked ? label + " BLOCKED" : label;
        gui.fill(x, y, x + w, y + h, bg);
        gui.renderOutline(x, y, w, h, color);
        gui.drawCenteredString(this.font, finalLabel, x + w / 2, y + (h - 8) / 2, color);
    }

    private boolean isApplyCmdVisible() {
        String myName = this.minecraft.player.getScoreboardName();
        String myTeam = this.getPlayerTeam().toUpperCase();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        boolean amISquadLeaderAnywhere = false;
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(myName)) continue;
            amISquadLeaderAnywhere = true;
            break;
        }
        return amISquadLeaderAnywhere && teamCMDId == -1 && !myTeamVoteActive;
    }

    private List<WarfareWorldData.Squad> getMyTeamSquadsSorted() {
        String myTeam = this.getPlayerTeam().toUpperCase();
        String myDim = this.minecraft.level.dimension().location().toString();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        List<WarfareWorldData.Squad> list = ClientData.clientSquads.stream().filter(s -> s.team.equalsIgnoreCase(myTeam)).filter(s -> s.dimension != null && s.dimension.equals(myDim)).collect(Collectors.toList());
        list.sort((s1, s2) -> {
            if (s1.id == teamCMDId && teamCMDId != -1) {
                return -1;
            }
            if (s2.id == teamCMDId && teamCMDId != -1) {
                return 1;
            }
            return Integer.compare(s1.id, s2.id);
        });
        return list;
    }

    private List<String> getSortedMembers(WarfareWorldData.Squad squad) {
        ArrayList<String> sorted = new ArrayList<String>();
        if (!squad.leader.isEmpty() && squad.members.contains(squad.leader)) {
            sorted.add(squad.leader);
        }
        for (String m : squad.members) {
            if (m.equals(squad.leader) || squad.bravoMembers.contains(m) || squad.charlieMembers.contains(m)) continue;
            sorted.add(m);
        }
        if (!squad.bravoLeader.isEmpty() && squad.members.contains(squad.bravoLeader)) {
            sorted.add(squad.bravoLeader);
        }
        for (String m : squad.bravoMembers) {
            if (m.equals(squad.bravoLeader) || !squad.members.contains(m)) continue;
            sorted.add(m);
        }
        if (!squad.charlieLeader.isEmpty() && squad.members.contains(squad.charlieLeader)) {
            sorted.add(squad.charlieLeader);
        }
        for (String m : squad.charlieMembers) {
            if (m.equals(squad.charlieLeader) || !squad.members.contains(m)) continue;
            sorted.add(m);
        }
        return sorted;
    }

    private void renderSquadList(GuiGraphics gui, int mouseX, int mouseY) {
        String myName = this.minecraft.player.getScoreboardName();
        boolean amIInSquad = this.isPlayerInSquad();
        boolean isBlue = this.getPlayerTeam().toUpperCase().contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        int currentY = this.isApplyCmdVisible() ? 35 : 10;
        List<WarfareWorldData.Squad> myTeamSquads = this.getMyTeamSquadsSorted();
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
                RenderSystem.setShaderColor(1.0f, 0.8f, 0.2f, 1.0f);
                gui.blit(ARROW_DOWN, arrowX, currentY + 1, 0.0f, 0.0f, 8, 8, 8, 8);
            } else {
                RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1.0f);
                gui.blit(ARROW_UP, arrowX, currentY + 1, 0.0f, 0.0f, 8, 8, 8, 8);
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (amILeader || squad.isLocked) {
                int lockX = arrowX - 12;
                if (squad.isLocked) {
                    RenderSystem.setShaderColor(1.0f, 0.8f, 0.2f, 1.0f);
                } else {
                    RenderSystem.setShaderColor(0.6f, 0.6f, 0.6f, 1.0f);
                }
                gui.blit(LOCK_ICON, lockX, currentY + 1, 0.0f, 0.0f, 8, 8, 8, 8);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            currentY += 12;
            if (isExpanded) {
                List<String> sortedMembers = this.getSortedMembers(squad);
                for (String member : sortedMembers) {
                    String kName;
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
                    if (!(kName = ClientData.playerKits.getOrDefault(member, "Unassigned")).equals("Unassigned") && !kName.isEmpty()) {
                        ResourceLocation kitIcon = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + kName.toLowerCase().replace(" ", "_") + ".png");
                        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                        gui.blit(kitIcon, xOffset, currentY, 0.0f, 0.0f, 10, 10, 10, 10);
                        xOffset += 12;
                    }
                    gui.drawString(this.font, member, xOffset, currentY + 1, col, false);
                    currentY += 12;
                }
                currentY += 4;
            }
            currentY += 4;
            ++index;
        }
    }

    private void renderContextMenu(GuiGraphics gui, int mx, int my) {
        String myName = this.minecraft.player.getScoreboardName();
        WarfareWorldData.Squad s = null;
        for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.id != this.contextTargetSquadId) continue;
            s = sq;
        }
        if (s == null) {
            this.showContextMenu = false;
            return;
        }
        boolean amISL = s.leader.equals(myName);
        boolean amIBravoFTL = s.bravoLeader.equals(myName);
        boolean amICharlieFTL = s.charlieLeader.equals(myName);
        ArrayList<String> options = new ArrayList<String>();
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
            return;
        }
        int w = 100;
        int h = options.size() * 12 + 4;
        gui.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + w, this.contextMenuY + h, -300871407);
        gui.renderOutline(this.contextMenuX, this.contextMenuY, w, h, -11184811);
        for (int i = 0; i < options.size(); ++i) {
            boolean hover;
            int y = this.contextMenuY + 2 + i * 12;
            boolean bl = hover = mx >= this.contextMenuX && mx <= this.contextMenuX + w && my >= y && my < y + 12;
            if (hover) {
                gui.fill(this.contextMenuX + 1, y, this.contextMenuX + w - 1, y + 12, -12303292);
            }
            gui.drawString(this.font, options.get(i), this.contextMenuX + 4, y + 2, hover ? 0xFFFFFF : 0xAAAAAA, false);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (this.showContextMenu) {
            if (btn == 0) {
                this.processContextMenuClick(mx, my);
            }
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
        if (mx >= 180.0 && mx <= (double)this.getMapOriginX() && btn == 0 && this.handleSpawnButtons(mx, my)) {
            return true;
        }
        if (this.mapRenderer.isMouseOver(mx, my)) {
            String clickedSpawn;
            if (btn == 0 && (clickedSpawn = this.getSpawnPointUnderMouse(mx, my)) != null) {
                this.selectedSpawnType = clickedSpawn;
                this.mapRenderer.selectedSpawnId = clickedSpawn;
                this.playClickSound();
                return true;
            }
            if (btn == 1) {
                this.handleMapRightClick(mx, my);
                return true;
            }
            return this.mapRenderer.mouseClicked(mx, my, btn);
        }
        return false;
    }

    private void handleSquadListInteraction(double mx, double my, int btn) {
        String myName = this.minecraft.player.getScoreboardName();
        boolean amIInSquad = this.isPlayerInSquad();
        int currentY = this.isApplyCmdVisible() ? 35 : 10;
        List<WarfareWorldData.Squad> squads = this.getMyTeamSquadsSorted();
        for (WarfareWorldData.Squad squad : squads) {
            boolean isMySquad = squad.members.contains(myName);
            boolean amILeader = squad.leader.equals(myName);
            boolean isExpanded = this.expandedSquads.contains(squad.id);
            String actionText = "";
            if (isMySquad) {
                actionText = "LEAVE";
            } else if (!amIInSquad) {
                actionText = squad.isLocked ? "LOCKED" : (squad.members.size() >= 9 ? "FULL" : "JOIN");
            }
            int actionWidth = actionText.isEmpty() ? 0 : this.font.width(actionText);
            int actionX = actionWidth > 0 ? 170 - actionWidth - 10 : 0;
            int arrowX = (actionX > 0 ? actionX : 160) - 12;
            int lockX = arrowX - 12;
            if (my >= (double)currentY && my <= (double)(currentY + 11)) {
                if (btn == 0) {
                    if (actionWidth > 0 && mx >= (double)actionX && mx <= (double)(actionX + actionWidth)) {
                        if (isMySquad) {
                            PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(2, squad.id, ""));
                        } else if (!squad.isLocked && squad.members.size() < 9) {
                            PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(1, squad.id, ""));
                        }
                    } else if (mx >= (double)arrowX && mx <= (double)(arrowX + 10)) {
                        if (isExpanded) {
                            this.expandedSquads.remove(squad.id);
                        } else {
                            this.expandedSquads.add(squad.id);
                        }
                    } else if (amILeader && mx >= (double)lockX && mx <= (double)(lockX + 10)) {
                        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(5, squad.id, ""));
                    } else {
                        return;
                    }
                    this.playClickSound();
                }
                return;
            }
            currentY += 12;
            if (isExpanded) {
                List<String> sortedMembers = this.getSortedMembers(squad);
                for (String member : sortedMembers) {
                    if (my >= (double)currentY && my <= (double)(currentY + 11)) {
                        if (btn == 1) {
                            boolean amFTL = squad.bravoLeader.equals(myName) || squad.charlieLeader.equals(myName);
                            if ((amILeader || amFTL) && isMySquad && !member.equals(myName)) {
                                this.contextTargetPlayer = member;
                                this.contextTargetSquadId = squad.id;
                                this.contextMenuX = (int)mx;
                                this.contextMenuY = (int)my;
                                this.showContextMenu = true;
                                this.playClickSound();
                            }
                        } else if (btn == 0 && isMySquad && member.equals(myName) && mx >= 30.0 && mx <= 45.0) {
                            PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu());
                            this.playClickSound();
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

    private void processContextMenuClick(double mx, double my) {
        int clickedIdx;
        WarfareWorldData.Squad s = null;
        for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.id != this.contextTargetSquadId) continue;
            s = sq;
            break;
        }
        if (s == null) {
            return;
        }
        String myName = this.minecraft.player.getScoreboardName();
        boolean amISL = s.leader.equals(myName);
        boolean amIBravoFTL = s.bravoLeader.equals(myName);
        boolean amICharlieFTL = s.charlieLeader.equals(myName);
        ArrayList<String> options = new ArrayList<String>();
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
            options.add("Add to Bravo");
            options.add("Remove from FT");
        } else if (amICharlieFTL) {
            options.add("Add to Charlie");
            options.add("Remove from FT");
        }
        int w = 100;
        int h = options.size() * 12 + 4;
        if (mx >= (double)this.contextMenuX && mx <= (double)(this.contextMenuX + w) && my >= (double)this.contextMenuY && my <= (double)(this.contextMenuY + h) && (clickedIdx = (int)(my - (double)this.contextMenuY - 2.0) / 12) >= 0 && clickedIdx < options.size()) {
            String opt = options.get(clickedIdx);
            if (opt.equals("Promote to SL")) {
                PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(4, s.id, this.contextTargetPlayer));
            } else if (opt.equals("Set FTL Bravo")) {
                PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(6, s.id, this.contextTargetPlayer));
            } else if (opt.equals("Set FTL Charlie")) {
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

    private boolean handleSpawnButtons(double mx, double my) {
        int y = 55;
        if (my >= (double)y && my <= (double)(y + 24)) {
            this.selectedSpawnType = "MAIN";
            this.mapRenderer.selectedSpawnId = "MAIN";
            this.playClickSound();
            return true;
        }
        if (my >= (double)(y += 30) && my <= (double)(y + 24)) {
            if (this.hasValidRally() && !this.isMyRallyBlocked()) {
                this.selectedSpawnType = "RALLY";
                this.mapRenderer.selectedSpawnId = "RALLY";
                this.playClickSound();
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
                if (my >= (double)y && my <= (double)(y + 20)) {
                    if (!hub.isBlocked) {
                        String id = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
                        this.selectedSpawnType = id;
                        this.mapRenderer.selectedSpawnId = id;
                        this.playClickSound();
                    }
                    return true;
                }
                y += 24;
            }
        }
        return false;
    }

    private void handleMapRightClick(double mouseX, double mouseY) {
        if (!this.isSquadLeaderOrFTL(this.minecraft.player)) {
            return;
        }
        double bpp = this.mapRenderer.getBlocksPerPixel();
        double centerX = this.mapRenderer.getCenterX(this.minecraft.player);
        double centerZ = this.mapRenderer.getCenterZ(this.minecraft.player);
        int mapSize = this.getMapSize();
        int mapOriginX = this.getMapOriginX();
        int mapY = this.getMapY();
        int worldX = (int)(centerX + (mouseX - ((double)mapOriginX + (double)mapSize / 2.0)) * bpp);
        int worldZ = (int)(centerZ + (mouseY - ((double)mapY + (double)mapSize / 2.0)) * bpp);
        this.minecraft.setScreen(new TacticalMapRadialScreen(worldX, worldZ, this));
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (this.mapRenderer.isMouseOver(mx, my)) {
            return this.mapRenderer.mouseScrolled(mx, my, delta);
        }
        return super.mouseScrolled(mx, my, delta);
    }

    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (this.mapRenderer.mouseDragged(mx, my, btn, dx, dy)) {
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    public boolean mouseReleased(double mx, double my, int btn) {
        this.mapRenderer.mouseReleased(btn);
        return super.mouseReleased(mx, my, btn);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private boolean isSquadLeaderOrFTL(Player player) {
        String pName = player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(pName) && !s.bravoLeader.equals(pName) && !s.charlieLeader.equals(pName)) continue;
            return true;
        }
        return false;
    }

    private boolean hasValidRally() {
        String myName = this.minecraft.player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            return s.rallyPos != null && !s.isRallyBlocked;
        }
        return false;
    }

    private String getPlayerTeam() {
        if (this.minecraft.player.getTeam() != null) {
            return this.minecraft.player.getTeam().getName();
        }
        return "NEUTRAL";
    }

    private boolean isPlayerInSquad() {
        String myName = this.minecraft.player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            return true;
        }
        return false;
    }

    private ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equalsIgnoreCase("none")) {
            return null;
        }
        return new ResourceLocation("pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png");
    }

    private void playClickSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private static class SquadButton extends Button {
        public SquadButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
            int borderColor;
            if (!this.visible) {
                return;
            }
            int n = borderColor = this.isHovered() ? -1 : -6710887;
            if (!this.active) {
                borderColor = -12303292;
            }
            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -871296751);
            gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
            int textColor = this.active ? -1 : -8947849;
            gui.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
            if (this.active && this.isHovered()) {
                gui.fill(this.getX(), this.getY() + this.height - 2, this.getX() + 2, this.getY() + this.height, -1);
            }
        }
    }

    private static class DeployButton extends Button {
        public DeployButton(int x, int y, int w, int h, Component msg, Button.OnPress press) {
            super(x, y, w, h, msg, press, DEFAULT_NARRATION);
        }

        protected void renderWidget(GuiGraphics gui, int mx, int my, float pt) {
            int borderColor;
            int n = borderColor = this.isHovered() && this.active ? -1 : -6710887;
            if (!this.active) {
                borderColor = -12303292;
            }
            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -871296751);
            gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);
            int textColor = this.active ? -1 : -8947849;
            gui.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
        }
    }
}
