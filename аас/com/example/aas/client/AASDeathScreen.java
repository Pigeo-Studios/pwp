/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.Button$OnPress
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.Renderable
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.DeathScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.TitleScreen
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.scores.Team
 */
package com.example.aas.client;

import com.example.aas.client.ClientData;
import com.example.aas.client.gui.AASMapRenderer;
import com.example.aas.client.gui.TacticalMapRadialScreen;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketRequestCMD;
import com.example.aas.network.PacketRequestKitMenu;
import com.example.aas.network.PacketRespawnRequest;
import com.example.aas.network.PacketSquadAction;
import com.example.aas.network.PacketSquadChat;
import com.example.aas.world.AASWorldData;
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
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

public class AASDeathScreen
extends DeathScreen {
    private final long deathTimestamp;
    private final int respawnTimeTotal;
    private EditBox chatInput;
    private Button chatModeButton;
    private int chatMode = 1;
    private final Set<Integer> expandedSquads = new HashSet<Integer>();
    private final AASMapRenderer mapRenderer = new AASMapRenderer();
    private static final int SIDEBAR_WIDTH = 170;
    private SquadButton applyCmdButton;
    private EditBox nameInput;
    private Button createButton;
    private boolean showContextMenu = false;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private String contextTargetPlayer = "";
    private int contextTargetSquadId = -1;
    private String selectedSpawnType = "";
    private String selectedDisplayName = "NONE";
    private Button deployButton;
    private static final ResourceLocation ARROW_DOWN = new ResourceLocation("aas", "textures/gui/arrow_down.png");
    private static final ResourceLocation ARROW_UP = new ResourceLocation("aas", "textures/gui/arrow_up.png");
    private static final ResourceLocation LOCK_ICON = new ResourceLocation("aas", "textures/gui/squad_lock.png");
    private static final ResourceLocation TICKET_ICON = new ResourceLocation("aas", "textures/gui/minimap_tickets.png");
    private static final ResourceLocation VOICE_ICON = new ResourceLocation("aas", "textures/gui/voice_icon.png");
    private static final ResourceLocation RADIO_ICON = new ResourceLocation("aas", "textures/gui/voice_icon_radio.png");

    public AASDeathScreen(Component cause, boolean hardcore) {
        super((Component)(cause != null ? cause : Component.m_237119_()), hardcore);
        if (ClientData.globalDeathTimestamp == 0L) {
            ClientData.globalDeathTimestamp = System.currentTimeMillis();
        }
        this.deathTimestamp = ClientData.globalDeathTimestamp;
        this.respawnTimeTotal = ClientData.RESPAWN_TIME > 0 ? ClientData.RESPAWN_TIME : 10;
        String myName = Minecraft.m_91087_().m_91094_().m_92546_();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            this.expandedSquads.add(s.id);
        }
    }

    protected void m_7856_() {
        this.m_169413_();
        int mapSize = this.f_96544_;
        int mapX = this.f_96543_ - mapSize;
        this.mapRenderer.init(mapX, 0, mapSize);
        this.applyCmdButton = (SquadButton)this.m_142416_((GuiEventListener)new SquadButton(10, 10, 150, 20, (Component)Component.m_237113_((String)"APPLY FOR CMD"), b -> {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestCMD());
            b.f_93624_ = false;
        }));
        boolean isInSquad = this.isPlayerInSquad();
        this.nameInput = (EditBox)this.m_142416_((GuiEventListener)new EditBox(this.f_96547_, 10, this.f_96544_ - 90, 150, 20, (Component)Component.m_237113_((String)"Squad Name")));
        this.nameInput.m_94199_(12);
        this.nameInput.m_94194_(!isInSquad);
        this.createButton = (Button)this.m_142416_((GuiEventListener)new SquadButton(10, this.f_96544_ - 65, 150, 20, (Component)Component.m_237113_((String)"Create Squad"), b -> PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(0, 0, this.nameInput.m_94155_()))));
        this.createButton.f_93624_ = !isInSquad;
        this.deployButton = (Button)this.m_142416_((GuiEventListener)new DeployButton(10, this.f_96544_ - 35, 100, 25, (Component)Component.m_237113_((String)"DEPLOY"), b -> {
            if (!this.selectedSpawnType.isEmpty()) {
                ClientData.globalDeathTimestamp = 0L;
                ClientData.deathFadeStartTime = 0L;
                ClientData.deathFadePlayed = false;
                PacketHandler.INSTANCE.sendToServer((Object)new PacketRespawnRequest(this.selectedSpawnType));
                this.f_96541_.f_91074_.m_7583_();
                this.f_96541_.m_91152_(null);
            }
        }));
        int mapOriginX = this.f_96543_ - this.f_96544_;
        int chatX = 180;
        int chatAvailableWidth = mapOriginX - chatX - 10;
        int modeBtnWidth = 60;
        int gap = 4;
        this.chatModeButton = (Button)this.m_142416_((GuiEventListener)new SquadButton(chatX, this.f_96544_ - 40, modeBtnWidth, 20, this.getChatModeText(), b -> {
            this.chatMode = (this.chatMode + 1) % 3;
            b.m_93666_(this.getChatModeText());
        }));
        this.chatInput = new EditBox(this.f_96547_, chatX + modeBtnWidth + gap, this.f_96544_ - 40, chatAvailableWidth - modeBtnWidth - gap, 20, (Component)Component.m_237113_((String)"Chat"));
        this.chatInput.m_94199_(100);
        this.m_142416_((GuiEventListener)this.chatInput);
        this.m_142416_((GuiEventListener)new SquadButton(this.f_96543_ - 45, 5, 40, 20, (Component)Component.m_237113_((String)"Quit"), b -> {
            if (this.f_96541_.f_91073_ != null) {
                this.f_96541_.f_91073_.m_7462_();
            }
            this.f_96541_.m_91152_((Screen)new TitleScreen());
        }));
    }

    private Component getChatModeText() {
        switch (this.chatMode) {
            case 0: {
                return Component.m_237113_((String)"ALL").m_130940_(ChatFormatting.LIGHT_PURPLE);
            }
            case 2: {
                return Component.m_237113_((String)"SQUAD").m_130940_(ChatFormatting.GREEN);
            }
        }
        return Component.m_237113_((String)"TEAM").m_130940_(ChatFormatting.BLUE);
    }

    public void m_86600_() {
        super.m_86600_();
        if (this.nameInput != null) {
            this.nameInput.m_94120_();
        }
        if (this.applyCmdButton != null) {
            this.applyCmdButton.f_93624_ = this.isApplyCmdVisible();
        }
        boolean isInSquad = this.isPlayerInSquad();
        if (this.nameInput != null && this.nameInput.m_94213_() == isInSquad) {
            this.nameInput.m_94194_(!isInSquad);
            this.createButton.f_93624_ = !isInSquad;
        }
    }

    private boolean isMyRallyBlocked() {
        String myName = this.f_96541_.f_91074_.m_6302_();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            return s.isRallyBlocked;
        }
        return false;
    }

    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && this.chatInput.m_93696_()) {
            String msg = this.chatInput.m_94155_().trim();
            if (!msg.isEmpty()) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadChat(msg, this.chatMode));
                this.chatInput.m_94144_("");
            }
            return true;
        }
        return super.m_7933_(keyCode, scanCode, modifiers);
    }

    private void renderChatArea(GuiGraphics gui) {
        int mapOriginX = this.f_96543_ - this.f_96544_;
        int chatX = 180;
        int chatAvailableWidth = mapOriginX - chatX - 10;
        int chatBottomY = this.f_96544_ - 45;
        int maxMessages = 15;
        int boxLeft = chatX;
        int boxTop = chatBottomY - maxMessages * 10;
        int boxRight = chatX + chatAvailableWidth;
        int boxBottom = chatBottomY;
        gui.m_280509_(boxLeft, boxTop, boxRight, boxBottom, 0x70000000);
        gui.m_280588_(boxLeft, boxTop, boxRight, boxBottom);
        int count = 0;
        for (Component msg : ClientData.menuChatHistory) {
            if (count >= maxMessages) break;
            int y = chatBottomY - 10 - count * 10;
            gui.m_280614_(this.f_96547_, msg, chatX + 3, y, -1, true);
            ++count;
        }
        gui.m_280618_();
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        gui.m_280509_(0, 0, this.f_96543_, this.f_96544_, -16777216);
        this.mapRenderer.render(gui, mx, my, pt);
        gui.m_280509_(0, 0, 310, this.f_96544_, -1442840576);
        gui.m_280509_(0, 0, 170, this.f_96544_, 0x22FFFFFF);
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - this.deathTimestamp) / 1000L;
        int secondsLeft = (int)((long)this.respawnTimeTotal - elapsedSeconds);
        if (secondsLeft > 0) {
            this.deployButton.f_93623_ = false;
            this.deployButton.m_93666_((Component)Component.m_237113_((String)("WAIT " + secondsLeft + "s")));
        } else {
            this.deployButton.f_93623_ = !this.selectedSpawnType.isEmpty();
            this.deployButton.m_93666_((Component)Component.m_237113_((String)"DEPLOY"));
        }
        this.renderSquadList(gui, mx, my);
        this.renderTeamHeader(gui);
        this.renderSpawnSelection(gui, mx, my);
        this.renderVoiceActivity(gui);
        this.renderChatArea(gui);
        for (Renderable renderable : this.f_169369_) {
            renderable.m_88315_(gui, mx, my, pt);
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
                gui.m_280168_().m_85836_();
                gui.m_280168_().m_252880_(0.0f, 0.0f, 1000.0f);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                gui.m_280509_(0, 0, this.f_96543_, this.f_96544_, alphaInt << 24 | 0);
                RenderSystem.disableBlend();
                gui.m_280168_().m_85849_();
            }
        }
    }

    private void renderVoiceActivity(GuiGraphics gui) {
        long now = System.currentTimeMillis();
        int x = 5;
        int y = this.f_96544_ / 2 - 40;
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
        int tw = this.f_96547_.m_92895_(name) + 15;
        gui.m_280509_(x, y - 2, x + tw + 4, y + 10, Integer.MIN_VALUE);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)1.0f);
        gui.m_280163_(icon, x + 3, y, 0.0f, 0.0f, 8, 8, 8, 8);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        gui.m_280056_(this.f_96547_, name, x + 14, y, color, false);
    }

    private String getSpawnPointUnderMouse(double mouseX, double mouseY) {
        BlockPos myMainPos;
        Minecraft mc = Minecraft.m_91087_();
        String myName = mc.f_91074_.m_6302_();
        double bpp = this.mapRenderer.getBlocksPerPixel();
        double cx = this.mapRenderer.getCenterX(mc.f_91074_);
        double cz = this.mapRenderer.getCenterZ(mc.f_91074_);
        String myTeam = this.getPlayerTeam().toUpperCase();
        String currentDim = mc.f_91073_.m_46472_().m_135782_().toString();
        BlockPos blockPos = myMainPos = myTeam.equals("BLUE") ? ClientData.blueSpawns.get(currentDim) : ClientData.redSpawns.get(currentDim);
        if (myMainPos != null && this.isIconHit(myMainPos, mouseX, mouseY, cx, cz, bpp)) {
            return "MAIN";
        }
        for (AASWorldData.Squad squad : ClientData.clientSquads) {
            if (!squad.members.contains(myName) || squad.rallyPos == null || squad.isRallyBlocked || !this.isIconHit(squad.rallyPos, mouseX, mouseY, cx, cz, bpp)) continue;
            return "RALLY";
        }
        for (AASWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || hub.isBlocked || !this.isIconHit(hub.pos, mouseX, mouseY, cx, cz, bpp)) continue;
            return "HUB:" + hub.pos.m_123341_() + ":" + hub.pos.m_123342_() + ":" + hub.pos.m_123343_();
        }
        return null;
    }

    private boolean isIconHit(BlockPos pos, double mx, double my, double cx, double cz, double bpp) {
        double dz;
        int py;
        int mapOriginX = this.f_96543_ - this.f_96544_;
        double dx = ((double)pos.m_123341_() + 0.5 - cx) / bpp;
        int px = (int)((double)mapOriginX + (double)this.f_96544_ / 2.0 + dx);
        double distSq = (mx - (double)px) * (mx - (double)px) + (my - (double)(py = (int)((double)this.f_96544_ / 2.0 + (dz = ((double)pos.m_123343_() + 0.5 - cz) / bpp)))) * (my - (double)py);
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
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            gui.m_280411_(flagTex, startX, startY, 32, 18, 0.0f, 0.0f, 64, 36, 64, 36);
        }
        gui.m_280056_(this.f_96547_, customName, startX + 38, startY, teamName.contains("BLUE") ? 0x5555FF : 0xFF5555, true);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        gui.m_280163_(TICKET_ICON, startX + 38, startY + 11, 0.0f, 0.0f, 8, 8, 8, 8);
        gui.m_280056_(this.f_96547_, String.valueOf(tickets), startX + 50, startY + 11, 16766720, true);
    }

    private void renderSpawnSelection(GuiGraphics gui, int mx, int my) {
        int startX = 180;
        int startY = 55;
        gui.m_280488_(this.f_96547_, "SELECT SPAWN POINT:", startX, startY - 15, 0xFFAA00);
        this.drawSpawnOption(gui, startX, startY, 110, 24, "MAIN BASE", "MAIN", mx, my, true, false);
        boolean rallyBlocked = this.isMyRallyBlocked();
        boolean rallyValid = this.hasValidRally() && !rallyBlocked;
        this.drawSpawnOption(gui, startX, startY += 30, 110, 24, "SQUAD RALLY", "RALLY", mx, my, rallyValid, rallyBlocked);
        gui.m_280488_(this.f_96547_, "AVAILABLE HUBS:", startX, (startY += 40) - 12, 0xAAAAAA);
        Team team = this.f_96541_.f_91074_.m_5647_();
        if (team != null) {
            String myTeam = team.m_5758_();
            String myDim = this.f_96541_.f_91073_.m_46472_().m_135782_().toString();
            int hubIdx = 1;
            for (AASWorldData.HubInfo hub : ClientData.clientHubs) {
                if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || !hub.dimension.equals(myDim)) continue;
                String id = "HUB:" + hub.pos.m_123341_() + ":" + hub.pos.m_123342_() + ":" + hub.pos.m_123343_();
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
        int color;
        boolean hovered = active && mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean selected = this.selectedSpawnType.equals(id);
        int n = isBlocked ? -43691 : (active ? (selected ? -11141291 : (hovered ? -1 : 0xBBBBBB)) : (color = 0x555555));
        int bg = isBlocked ? 0x60FF0000 : (selected ? 0x4455FF55 : (active ? 0x22FFFFFF : 0x11000000));
        Object finalLabel = isBlocked ? label + " BLOCKED" : label;
        gui.m_280509_(x, y, x + w, y + h, bg);
        gui.m_280637_(x, y, w, h, color);
        gui.m_280137_(this.f_96547_, (String)finalLabel, x + w / 2, y + (h - 8) / 2, color);
    }

    private boolean isApplyCmdVisible() {
        String myName = this.f_96541_.f_91074_.m_6302_();
        String myTeam = this.getPlayerTeam().toUpperCase();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        boolean amISquadLeaderAnywhere = false;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(myName)) continue;
            amISquadLeaderAnywhere = true;
            break;
        }
        return amISquadLeaderAnywhere && teamCMDId == -1 && !myTeamVoteActive;
    }

    private List<AASWorldData.Squad> getMyTeamSquadsSorted() {
        String myTeam = this.getPlayerTeam().toUpperCase();
        String myDim = this.f_96541_.f_91073_.m_46472_().m_135782_().toString();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        List<AASWorldData.Squad> list = ClientData.clientSquads.stream().filter(s -> s.team.equalsIgnoreCase(myTeam)).filter(s -> s.dimension != null && s.dimension.equals(myDim)).collect(Collectors.toList());
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

    private List<String> getSortedMembers(AASWorldData.Squad squad) {
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
        String myName = this.f_96541_.f_91074_.m_6302_();
        boolean amIInSquad = this.isPlayerInSquad();
        boolean isBlue = this.getPlayerTeam().toUpperCase().contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        int currentY = this.isApplyCmdVisible() ? 35 : 10;
        List<AASWorldData.Squad> myTeamSquads = this.getMyTeamSquadsSorted();
        int index = 1;
        for (AASWorldData.Squad squad : myTeamSquads) {
            boolean isMySquad = squad.members.contains(myName);
            boolean amILeader = squad.leader.equals(myName);
            boolean isExpanded = this.expandedSquads.contains(squad.id);
            boolean isCMD = squad.id == teamCMDId && teamCMDId != -1;
            String prefix = isCMD ? "[CMD] " : "";
            int squadNameColor = isCMD ? -11141291 : -10496;
            gui.m_280056_(this.f_96547_, index + ".", 5, currentY, -1, false);
            String squadDisplayName = prefix + squad.name + " (" + squad.members.size() + "/9)";
            gui.m_280056_(this.f_96547_, squadDisplayName, 25, currentY, squadNameColor, false);
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
                int actionWidth = this.f_96547_.m_92895_(actionText);
                actionX = 170 - actionWidth - 10;
                boolean hover = mouseX >= actionX && mouseX <= actionX + actionWidth && mouseY >= currentY && mouseY <= currentY + 9;
                int finalColor = hover && clickable ? -1 : actionColor;
                gui.m_280056_(this.f_96547_, actionText, actionX, currentY, finalColor, false);
            }
            int arrowX = (actionX > 0 ? actionX : 160) - 12;
            RenderSystem.enableBlend();
            if (isExpanded) {
                RenderSystem.setShaderColor((float)1.0f, (float)0.8f, (float)0.2f, (float)1.0f);
                gui.m_280163_(ARROW_DOWN, arrowX, currentY + 1, 0.0f, 0.0f, 8, 8, 8, 8);
            } else {
                RenderSystem.setShaderColor((float)0.7f, (float)0.7f, (float)0.7f, (float)1.0f);
                gui.m_280163_(ARROW_UP, arrowX, currentY + 1, 0.0f, 0.0f, 8, 8, 8, 8);
            }
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            if (amILeader || squad.isLocked) {
                int lockX = arrowX - 12;
                if (squad.isLocked) {
                    RenderSystem.setShaderColor((float)1.0f, (float)0.8f, (float)0.2f, (float)1.0f);
                } else {
                    RenderSystem.setShaderColor((float)0.6f, (float)0.6f, (float)0.6f, (float)1.0f);
                }
                gui.m_280163_(LOCK_ICON, lockX, currentY + 1, 0.0f, 0.0f, 8, 8, 8, 8);
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            }
            currentY += 12;
            if (isExpanded) {
                List<String> sortedMembers = this.getSortedMembers(squad);
                for (String member : sortedMembers) {
                    String kName;
                    boolean isLeaderMember = member.equals(squad.leader);
                    boolean isOnline = this.f_96541_.m_91403_().m_104938_(member) != null;
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
                        gui.m_280509_(btnX, currentY, btnX + 10, currentY + 10, btnHover ? -10066330 : -12303292);
                        gui.m_280056_(this.f_96547_, "K", btnX + 2, currentY + 1, -1, false);
                        xOffset += 14;
                    }
                    if (!(kName = ClientData.playerKits.getOrDefault(member, "Unassigned")).equals("Unassigned") && !kName.isEmpty()) {
                        ResourceLocation kitIcon = new ResourceLocation("aas", "textures/gui/kits/" + kName.toLowerCase().replace(" ", "_") + ".png");
                        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                        gui.m_280163_(kitIcon, xOffset, currentY, 0.0f, 0.0f, 10, 10, 10, 10);
                        xOffset += 12;
                    }
                    gui.m_280056_(this.f_96547_, member, xOffset, currentY + 1, col, false);
                    currentY += 12;
                }
                currentY += 4;
            }
            currentY += 4;
            ++index;
        }
    }

    private void handleSquadListClicks(double mouseX, double mouseY) {
        String myName = this.f_96541_.f_91074_.m_6302_();
        boolean amIInSquad = this.isPlayerInSquad();
        int currentY = this.isApplyCmdVisible() ? 35 : 10;
        List<AASWorldData.Squad> myTeamSquads = this.getMyTeamSquadsSorted();
        for (AASWorldData.Squad squad : myTeamSquads) {
            boolean isMySquad = squad.members.contains(myName);
            boolean amILeader = squad.leader.equals(myName);
            boolean isExpanded = this.expandedSquads.contains(squad.id);
            String actionText = "";
            if (isMySquad) {
                actionText = "LEAVE";
            } else if (!amIInSquad) {
                actionText = squad.isLocked ? "LOCKED" : (squad.members.size() >= 9 ? "FULL" : "JOIN");
            }
            int actionWidth = actionText.isEmpty() ? 0 : this.f_96547_.m_92895_(actionText);
            int actionX = actionWidth > 0 ? 170 - actionWidth - 10 : 0;
            int arrowX = (actionX > 0 ? actionX : 160) - 12;
            int lockX = arrowX - 12;
            if (actionWidth > 0 && mouseX >= (double)actionX && mouseX <= (double)(actionX + actionWidth) && mouseY >= (double)currentY && mouseY <= (double)(currentY + 9)) {
                if (isMySquad) {
                    PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(2, squad.id, ""));
                } else if (!squad.isLocked && squad.members.size() < 9) {
                    PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(1, squad.id, ""));
                }
                this.playClickSound();
                return;
            }
            if (mouseX >= (double)arrowX && mouseX <= (double)(arrowX + 10) && mouseY >= (double)currentY && mouseY <= (double)(currentY + 10)) {
                if (isExpanded) {
                    this.expandedSquads.remove(squad.id);
                } else {
                    this.expandedSquads.add(squad.id);
                }
                this.playClickSound();
                return;
            }
            if (amILeader && mouseX >= (double)lockX && mouseX <= (double)(lockX + 10) && mouseY >= (double)currentY && mouseY <= (double)(currentY + 10)) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(5, squad.id, ""));
                this.playClickSound();
                return;
            }
            currentY += 12;
            if (isExpanded) {
                List<String> sortedMembers = this.getSortedMembers(squad);
                for (String member : sortedMembers) {
                    if (isMySquad && member.equals(myName) && mouseX >= 30.0 && mouseX <= 40.0 && mouseY >= (double)currentY && mouseY <= (double)(currentY + 10)) {
                        PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestKitMenu());
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

    private void renderContextMenu(GuiGraphics gui, int mx, int my) {
        String myName = this.f_96541_.f_91074_.m_6302_();
        AASWorldData.Squad s = null;
        for (AASWorldData.Squad sq : ClientData.clientSquads) {
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
        gui.m_280509_(this.contextMenuX, this.contextMenuY, this.contextMenuX + w, this.contextMenuY + h, -300871407);
        gui.m_280637_(this.contextMenuX, this.contextMenuY, w, h, -11184811);
        for (int i = 0; i < options.size(); ++i) {
            boolean hover;
            int y = this.contextMenuY + 2 + i * 12;
            boolean bl = hover = mx >= this.contextMenuX && mx <= this.contextMenuX + w && my >= y && my < y + 12;
            if (hover) {
                gui.m_280509_(this.contextMenuX + 1, y, this.contextMenuX + w - 1, y + 12, -12303292);
            }
            gui.m_280056_(this.f_96547_, (String)options.get(i), this.contextMenuX + 4, y + 2, hover ? 0xFFFFFF : 0xAAAAAA, false);
        }
    }

    public boolean m_6375_(double mx, double my, int btn) {
        if (this.showContextMenu) {
            if (btn == 0) {
                this.processContextMenuClick(mx, my);
            }
            this.showContextMenu = false;
            return true;
        }
        if (super.m_6375_(mx, my, btn)) {
            this.mapRenderer.selectedSpawnId = this.selectedSpawnType;
            return true;
        }
        if (mx < 170.0 && my < (double)(this.f_96544_ - 100)) {
            this.handleSquadListInteraction(mx, my, btn);
            return true;
        }
        if (mx >= 180.0 && mx <= 300.0 && btn == 0 && this.handleSpawnButtons(mx, my)) {
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
        String myName = this.f_96541_.f_91074_.m_6302_();
        String myTeam = this.getPlayerTeam().toUpperCase();
        String myDim = this.f_96541_.f_91073_.m_46472_().m_135782_().toString();
        boolean amIInSquad = this.isPlayerInSquad();
        int currentY = this.isApplyCmdVisible() ? 35 : 10;
        List<AASWorldData.Squad> squads = this.getMyTeamSquadsSorted();
        for (AASWorldData.Squad squad : squads) {
            boolean isMySquad = squad.members.contains(myName);
            boolean amILeader = squad.leader.equals(myName);
            boolean isExpanded = this.expandedSquads.contains(squad.id);
            if (my >= (double)currentY && my <= (double)(currentY + 11)) {
                if (btn == 0) {
                    int actionWidth = 40;
                    int actionX = 170 - actionWidth - 10;
                    if (mx >= (double)actionX) {
                        if (isMySquad) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(2, squad.id, ""));
                        } else if (!squad.isLocked && squad.members.size() < 9 && !amIInSquad) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(1, squad.id, ""));
                        }
                    } else {
                        int arrowX = 115;
                        if (mx >= (double)(arrowX - 15) && mx <= (double)(arrowX + 15)) {
                            if (isExpanded) {
                                this.expandedSquads.remove(squad.id);
                            } else {
                                this.expandedSquads.add(squad.id);
                            }
                        } else if (amILeader && mx >= (double)(arrowX - 30) && mx < (double)(arrowX - 15)) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(5, squad.id, ""));
                        }
                    }
                    this.playClickSound();
                }
                return;
            }
            currentY += 12;
            if (isExpanded) {
                for (String member : this.getSortedMembers(squad)) {
                    if (my >= (double)currentY && my <= (double)(currentY + 11)) {
                        if (btn == 1) {
                            boolean amFTL;
                            boolean bl = amFTL = squad.bravoLeader.equals(myName) || squad.charlieLeader.equals(myName);
                            if ((amILeader || amFTL) && isMySquad && !member.equals(myName)) {
                                this.contextTargetPlayer = member;
                                this.contextTargetSquadId = squad.id;
                                this.contextMenuX = (int)mx;
                                this.contextMenuY = (int)my;
                                this.showContextMenu = true;
                                this.playClickSound();
                            }
                        } else if (btn == 0 && isMySquad && member.equals(myName) && mx >= 30.0 && mx <= 45.0) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestKitMenu());
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
        AASWorldData.Squad s = null;
        for (AASWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.id != this.contextTargetSquadId) continue;
            s = sq;
            break;
        }
        if (s == null) {
            return;
        }
        String myName = this.f_96541_.f_91074_.m_6302_();
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
            String opt = (String)options.get(clickedIdx);
            if (opt.equals("Promote to SL")) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(4, s.id, this.contextTargetPlayer));
            } else if (opt.equals("Set FTL Bravo")) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(6, s.id, this.contextTargetPlayer));
            } else if (opt.equals("Set FTL Charlie")) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(7, s.id, this.contextTargetPlayer));
            } else if (opt.equals("Add to Bravo")) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(8, s.id, this.contextTargetPlayer));
            } else if (opt.equals("Add to Charlie")) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(9, s.id, this.contextTargetPlayer));
            } else if (opt.equals("Remove from FT")) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(10, s.id, this.contextTargetPlayer));
            } else if (opt.equals("Kick from Squad")) {
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(3, s.id, this.contextTargetPlayer));
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
        Team team = this.f_96541_.f_91074_.m_5647_();
        if (team != null) {
            String myTeam = team.m_5758_();
            String myDim = this.f_96541_.f_91073_.m_46472_().m_135782_().toString();
            for (AASWorldData.HubInfo hub : ClientData.clientHubs) {
                if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || !hub.dimension.equals(myDim)) continue;
                if (my >= (double)y && my <= (double)(y + 20)) {
                    if (!hub.isBlocked) {
                        String id = "HUB:" + hub.pos.m_123341_() + ":" + hub.pos.m_123342_() + ":" + hub.pos.m_123343_();
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
        if (!this.isSquadLeaderOrFTL((Player)this.f_96541_.f_91074_)) {
            return;
        }
        double bpp = this.mapRenderer.getBlocksPerPixel();
        double centerX = this.mapRenderer.getCenterX(this.f_96541_.f_91074_);
        double centerZ = this.mapRenderer.getCenterZ(this.f_96541_.f_91074_);
        int mapOriginX = this.f_96543_ - this.f_96544_;
        int worldX = (int)(centerX + (mouseX - ((double)mapOriginX + (double)this.f_96544_ / 2.0)) * bpp);
        int worldZ = (int)(centerZ + (mouseY - (double)this.f_96544_ / 2.0) * bpp);
        this.f_96541_.m_91152_((Screen)new TacticalMapRadialScreen(worldX, worldZ));
    }

    public boolean m_6050_(double mx, double my, double delta) {
        if (this.mapRenderer.isMouseOver(mx, my)) {
            return this.mapRenderer.mouseScrolled(mx, my, delta);
        }
        return super.m_6050_(mx, my, delta);
    }

    public boolean m_7979_(double mx, double my, int btn, double dx, double dy) {
        if (this.mapRenderer.mouseDragged(mx, my, btn, dx, dy)) {
            return true;
        }
        return super.m_7979_(mx, my, btn, dx, dy);
    }

    public boolean m_6348_(double mx, double my, int btn) {
        this.mapRenderer.mouseReleased(btn);
        return super.m_6348_(mx, my, btn);
    }

    public boolean m_6913_() {
        return false;
    }

    private boolean isSquadLeaderOrFTL(Player player) {
        String pName = player.m_6302_();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(pName) && !s.bravoLeader.equals(pName) && !s.charlieLeader.equals(pName)) continue;
            return true;
        }
        return false;
    }

    private boolean hasValidRally() {
        String myName = this.f_96541_.f_91074_.m_6302_();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            return s.rallyPos != null && !s.isRallyBlocked;
        }
        return false;
    }

    private String getPlayerTeam() {
        if (this.f_96541_.f_91074_.m_5647_() != null) {
            return this.f_96541_.f_91074_.m_5647_().m_5758_();
        }
        return "NEUTRAL";
    }

    private boolean isPlayerInSquad() {
        String myName = this.f_96541_.f_91074_.m_6302_();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            return true;
        }
        return false;
    }

    private ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equalsIgnoreCase("none")) {
            return null;
        }
        return new ResourceLocation("aas", "textures/gui/flags/" + faction.toLowerCase() + ".png");
    }

    private void playClickSound() {
        this.f_96541_.m_91106_().m_120367_((SoundInstance)SimpleSoundInstance.m_263171_((Holder)SoundEvents.f_12490_, (float)1.0f));
    }

    private static class SquadButton
    extends Button {
        public SquadButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, f_252438_);
        }

        protected void m_87963_(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
            int borderColor;
            if (!this.f_93624_) {
                return;
            }
            int n = borderColor = this.m_274382_() ? -1 : -6710887;
            if (!this.f_93623_) {
                borderColor = -12303292;
            }
            gui.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, -871296751);
            gui.m_280637_(this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_, borderColor);
            int textColor = this.f_93623_ ? -1 : -8947849;
            gui.m_280653_(Minecraft.m_91087_().f_91062_, this.m_6035_(), this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + (this.f_93619_ - 8) / 2, textColor);
            if (this.f_93623_ && this.m_274382_()) {
                gui.m_280509_(this.m_252754_(), this.m_252907_() + this.f_93619_ - 2, this.m_252754_() + 2, this.m_252907_() + this.f_93619_, -1);
            }
        }
    }

    private static class DeployButton
    extends Button {
        public DeployButton(int x, int y, int w, int h, Component msg, Button.OnPress press) {
            super(x, y, w, h, msg, press, f_252438_);
        }

        protected void m_87963_(GuiGraphics gui, int mx, int my, float pt) {
            int borderColor;
            int n = borderColor = this.m_274382_() && this.f_93623_ ? -1 : -6710887;
            if (!this.f_93623_) {
                borderColor = -12303292;
            }
            gui.m_280509_(this.m_252754_(), this.m_252907_(), this.m_252754_() + this.f_93618_, this.m_252907_() + this.f_93619_, -871296751);
            gui.m_280637_(this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_, borderColor);
            int textColor = this.f_93623_ ? -1 : -8947849;
            gui.m_280653_(Minecraft.m_91087_().f_91062_, this.m_6035_(), this.m_252754_() + this.f_93618_ / 2, this.m_252907_() + (this.f_93619_ - 8) / 2, textColor);
        }
    }
}

