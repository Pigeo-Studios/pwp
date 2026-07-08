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
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.Holder
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.entity.player.Player
 */
package com.example.aas.client.gui;

import com.example.aas.client.ClientData;
import com.example.aas.client.gui.AASMapRenderer;
import com.example.aas.client.gui.TacticalMapRadialScreen;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketRequestCMD;
import com.example.aas.network.PacketRequestKitMenu;
import com.example.aas.network.PacketSquadAction;
import com.example.aas.network.PacketSquadChat;
import com.example.aas.world.AASWorldData;
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
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

public class SquadSelectionScreen
extends Screen {
    private SquadButton applyCmdButton;
    private static final int SIDEBAR_WIDTH = 170;
    private static final int TOP_BAR_HEIGHT = 30;
    private static final ResourceLocation LOCK_ICON = new ResourceLocation("aas", "textures/gui/squad_lock.png");
    private static final ResourceLocation ARROW_DOWN = new ResourceLocation("aas", "textures/gui/arrow_down.png");
    private static final ResourceLocation ARROW_UP = new ResourceLocation("aas", "textures/gui/arrow_up.png");
    private static final ResourceLocation CENTER_ICON = new ResourceLocation("minecraft", "textures/item/compass_16.png");
    private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("aas", "textures/gui/flags/ukraine.png");
    private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("aas", "textures/gui/flags/russia.png");
    private static final ResourceLocation FLAG_USA = new ResourceLocation("aas", "textures/gui/flags/usa.png");
    private static final ResourceLocation FLAG_NATO = new ResourceLocation("aas", "textures/gui/flags/nato.png");
    private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("aas", "textures/gui/flags/bluefor.png");
    private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("aas", "textures/gui/flags/redfor.png");
    private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("aas", "textures/gui/flags/insurgency.png");
    private static final ResourceLocation FLAG_PMC = new ResourceLocation("aas", "textures/gui/flags/pmc.png");
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
    private final Set<Integer> expandedSquads = new HashSet<Integer>();
    private final AASMapRenderer mapRenderer = new AASMapRenderer();

    public SquadSelectionScreen() {
        super((Component)Component.m_237113_((String)"Squad Selection"));
    }

    protected void m_7856_() {
        super.m_7856_();
        String myName = this.f_96541_.f_91074_.m_6302_();
        String myTeam = this.getPlayerTeam().toUpperCase();
        boolean isInSquad = this.isPlayerInSquad();
        AASWorldData.Squad mySquad = null;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            this.expandedSquads.add(s.id);
            if (!s.leader.equals(myName)) continue;
            mySquad = s;
        }
        int teamCMDId_init = myTeam.contains("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
        this.applyCmdButton = new SquadButton(10, 10, 150, 20, (Component)Component.m_237113_((String)"APPLY FOR CMD"), b -> {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestCMD());
            b.f_93624_ = false;
        });
        this.m_142416_((GuiEventListener)this.applyCmdButton);
        this.nameInput = new EditBox(this.f_96547_, 10, this.f_96544_ - 55, 150, 20, (Component)Component.m_237113_((String)"Squad Name"));
        this.nameInput.m_94199_(12);
        this.nameInput.m_94194_(!isInSquad);
        this.m_142416_((GuiEventListener)this.nameInput);
        this.createButton = (Button)this.m_142416_((GuiEventListener)new SquadButton(10, this.f_96544_ - 30, 150, 20, (Component)Component.m_237113_((String)"Create Squad"), button -> PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(0, 0, this.nameInput.m_94155_()))));
        this.createButton.f_93624_ = !isInSquad;
        int rightAreaWidth = this.f_96543_ - 170;
        int mapMargin = 2;
        int availableHeight = this.f_96544_ - 30 - 50;
        this.mapSize = Math.min(rightAreaWidth - mapMargin * 2, availableHeight);
        this.mapX = this.f_96543_ - this.mapSize - mapMargin;
        this.mapY = 30 + mapMargin;
        this.mapRenderer.init(this.mapX, this.mapY, this.mapSize);
        int inputY = this.f_96544_ - 25;
        int chatX = 175;
        int chatWidth = this.f_96543_ - 170 - 10;
        this.chatModeButton = (Button)this.m_142416_((GuiEventListener)new SquadButton(chatX, inputY, 50, 20, this.getChatModeText(), button -> {
            ++this.chatMode;
            if (this.chatMode > 2) {
                this.chatMode = 0;
            }
            button.m_93666_(this.getChatModeText());
        }));
        this.chatInput = new EditBox(this.f_96547_, chatX + 55, inputY, chatWidth - 55, 20, (Component)Component.m_237113_((String)"Chat"));
        this.chatInput.m_94199_(256);
        this.m_142416_((GuiEventListener)this.chatInput);
    }

    private Component getChatModeText() {
        switch (this.chatMode) {
            case 0: {
                return Component.m_237113_((String)"ALL").m_130940_(ChatFormatting.LIGHT_PURPLE);
            }
            case 1: {
                return Component.m_237113_((String)"TEAM").m_130940_(ChatFormatting.BLUE);
            }
            case 2: {
                return Component.m_237113_((String)"SQUAD").m_130940_(ChatFormatting.GREEN);
            }
        }
        return Component.m_237113_((String)"???");
    }

    public void m_86600_() {
        super.m_86600_();
        this.nameInput.m_94120_();
        this.chatInput.m_94120_();
        if (this.applyCmdButton != null) {
            boolean isBlue;
            String myName = this.f_96541_.f_91074_.m_6302_();
            String myTeam = this.getPlayerTeam().toUpperCase();
            AASWorldData.Squad mySquad = null;
            for (AASWorldData.Squad s : ClientData.clientSquads) {
                if (!s.members.contains(myName)) continue;
                mySquad = s;
                break;
            }
            int myTeamCMDId = (isBlue = myTeam.contains("BLUE")) ? ClientData.blueCMDId : ClientData.redCMDId;
            boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
            boolean isLeader = mySquad != null && mySquad.leader.equals(myName);
            this.applyCmdButton.f_93624_ = isLeader && myTeamCMDId == -1 && !myTeamVoteActive;
        }
        boolean isInSquad = this.isPlayerInSquad();
        if (this.nameInput.m_94213_() == isInSquad) {
            this.nameInput.m_94194_(!isInSquad);
            this.createButton.f_93624_ = !isInSquad;
        }
    }

    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            if (this.chatInput.m_93696_()) {
                String msg = this.chatInput.m_94155_().trim();
                if (!msg.isEmpty()) {
                    PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadChat(msg, this.chatMode));
                    this.chatInput.m_94144_("");
                }
                return true;
            }
            if (this.nameInput.m_93696_() && this.nameInput.m_94213_()) {
                String name = this.nameInput.m_94155_();
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(0, 0, name));
                return true;
            }
        }
        return super.m_7933_(keyCode, scanCode, modifiers);
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

    public void m_88315_(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        gui.m_280509_(0, 0, 170, this.f_96544_, -872415232);
        gui.m_280509_(170, 0, this.f_96543_, 30, -872415232);
        gui.m_280509_(170, 30, this.f_96543_, this.f_96544_, -1795162112);
        this.renderSquadList(gui, mouseX, mouseY);
        this.mapRenderer.render(gui, mouseX, mouseY, partialTick);
        this.renderChatHistory(gui, this.mapY, this.mapSize);
        this.renderTopBar(gui);
        super.m_88315_(gui, mouseX, mouseY, partialTick);
        if (this.showContextMenu) {
            this.renderContextMenu(gui, mouseX, mouseY);
        }
    }

    public boolean m_6050_(double mouseX, double mouseY, double delta) {
        if (this.mapRenderer.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.m_6050_(mouseX, mouseY, delta);
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (this.showContextMenu) {
            if (button == 0) {
                String myName = this.f_96541_.f_91074_.m_6302_();
                AASWorldData.Squad s3 = null;
                for (AASWorldData.Squad sq : ClientData.clientSquads) {
                    if (sq.id != this.contextTargetSquadId) continue;
                    s3 = sq;
                    break;
                }
                if (s3 != null) {
                    int clickedIdx;
                    boolean amISL = s3.leader.equals(myName);
                    boolean amIBravoFTL = s3.bravoLeader.equals(myName);
                    boolean amICharlieFTL = s3.charlieLeader.equals(myName);
                    ArrayList<String> options = new ArrayList<String>();
                    if (amISL) {
                        options.add("Promote to SL");
                        if (!s3.bravoLeader.equals(this.contextTargetPlayer) && !s3.charlieLeader.equals(this.contextTargetPlayer)) {
                            options.add("Set FTL Bravo");
                            options.add("Set FTL Charlie");
                        }
                        options.add("Add to Bravo");
                        options.add("Add to Charlie");
                        options.add("Remove from FT");
                        options.add("Kick from Squad");
                    } else if (amIBravoFTL) {
                        if (!s3.leader.equals(this.contextTargetPlayer) && !s3.charlieLeader.equals(this.contextTargetPlayer)) {
                            options.add("Pass FTL Bravo");
                        }
                        options.add("Add to Bravo");
                        options.add("Remove from FT");
                    } else if (amICharlieFTL) {
                        if (!s3.leader.equals(this.contextTargetPlayer) && !s3.bravoLeader.equals(this.contextTargetPlayer)) {
                            options.add("Pass FTL Charlie");
                        }
                        options.add("Add to Charlie");
                        options.add("Remove from FT");
                    }
                    int w = 100;
                    int h = options.size() * 12 + 4;
                    if (mouseX >= (double)this.contextMenuX && mouseX <= (double)(this.contextMenuX + w) && mouseY >= (double)this.contextMenuY && mouseY <= (double)(this.contextMenuY + h) && (clickedIdx = (int)(mouseY - (double)this.contextMenuY - 2.0) / 12) >= 0 && clickedIdx < options.size()) {
                        String opt = (String)options.get(clickedIdx);
                        if (opt.equals("Promote to SL")) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(4, s3.id, this.contextTargetPlayer));
                        } else if (opt.equals("Set FTL Bravo") || opt.equals("Pass FTL Bravo")) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(6, s3.id, this.contextTargetPlayer));
                        } else if (opt.equals("Set FTL Charlie") || opt.equals("Pass FTL Charlie")) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(7, s3.id, this.contextTargetPlayer));
                        } else if (opt.equals("Add to Bravo")) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(8, s3.id, this.contextTargetPlayer));
                        } else if (opt.equals("Add to Charlie")) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(9, s3.id, this.contextTargetPlayer));
                        } else if (opt.equals("Remove from FT")) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(10, s3.id, this.contextTargetPlayer));
                        } else if (opt.equals("Kick from Squad")) {
                            PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(3, s3.id, this.contextTargetPlayer));
                        }
                        this.playClickSound();
                    }
                }
            }
            this.showContextMenu = false;
            return true;
        }
        if (this.mapRenderer.isMouseOver(mouseX, mouseY) && this.mapRenderer.mouseClicked(mouseX, mouseY, button)) {
            if (button == 1) {
                this.handleMapRightClick(mouseX, mouseY);
            }
            return true;
        }
        if (super.m_6375_(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 1 && mouseX < 170.0 && mouseY < (double)(this.f_96544_ - 60)) {
            String myName = this.f_96541_.f_91074_.m_6302_();
            String myTeam = this.getPlayerTeam().toUpperCase();
            String myDim = this.f_96541_.f_91073_.m_46472_().m_135782_().toString();
            boolean isBlue = myTeam.contains("BLUE");
            int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
            boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
            boolean amISL_Anywhere = false;
            for (AASWorldData.Squad s4 : ClientData.clientSquads) {
                if (!s4.leader.equals(myName)) continue;
                amISL_Anywhere = true;
                break;
            }
            int currentY = amISL_Anywhere && teamCMDId == -1 && !myTeamVoteActive ? 35 : 10;
            List myTeamSquads = ClientData.clientSquads.stream().filter(s -> s.team.equalsIgnoreCase(myTeam)).filter(s -> s.dimension != null && s.dimension.equals(myDim)).collect(Collectors.toList());
            myTeamSquads.sort((s1, s2) -> {
                if (s1.id == teamCMDId && teamCMDId != -1) {
                    return -1;
                }
                if (s2.id == teamCMDId && teamCMDId != -1) {
                    return 1;
                }
                return Integer.compare(s1.id, s2.id);
            });
            for (AASWorldData.Squad squad : myTeamSquads) {
                currentY += 12;
                if (this.expandedSquads.contains(squad.id)) {
                    if (squad.members.contains(myName)) {
                        List<String> sortedMembers = this.getSortedMembers(squad);
                        for (String member : sortedMembers) {
                            if (mouseY >= (double)currentY && mouseY <= (double)(currentY + 12) && mouseX >= 30.0 && !member.equals(myName)) {
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
        if (button == 0 && mouseX < 170.0 && mouseY < (double)(this.f_96544_ - 60)) {
            this.handleSquadListClicks(mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void handleSquadListClicks(double mouseX, double mouseY) {
        String myName = this.f_96541_.f_91074_.m_6302_();
        String myTeam = this.getPlayerTeam().toUpperCase();
        String myDim = this.f_96541_.f_91073_.m_46472_().m_135782_().toString();
        boolean amIInSquad = this.isPlayerInSquad();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        boolean amISquadLeaderAnywhere = false;
        for (AASWorldData.Squad s3 : ClientData.clientSquads) {
            if (!s3.leader.equals(myName)) continue;
            amISquadLeaderAnywhere = true;
            break;
        }
        int currentY = amISquadLeaderAnywhere && teamCMDId == -1 && !myTeamVoteActive ? 35 : 10;
        List myTeamSquads = ClientData.clientSquads.stream().filter(s -> s.team.equalsIgnoreCase(myTeam)).filter(s -> s.dimension != null && s.dimension.equals(myDim)).collect(Collectors.toList());
        myTeamSquads.sort((s1, s2) -> {
            if (s1.id == teamCMDId && teamCMDId != -1) {
                return -1;
            }
            if (s2.id == teamCMDId && teamCMDId != -1) {
                return 1;
            }
            return Integer.compare(s1.id, s2.id);
        });
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

    private void setupContextMenu(double mx, double my) {
        String myName = this.f_96541_.f_91074_.m_6302_();
        String myTeam = this.getPlayerTeam().toUpperCase();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        boolean amISL_Global = false;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(myName)) continue;
            amISL_Global = true;
            break;
        }
        boolean buttonVisible = amISL_Global && teamCMDId == -1 && !myTeamVoteActive;
        int currentY = buttonVisible ? 35 : 10;
        List<AASWorldData.Squad> squads = this.getSortedSquads();
        for (AASWorldData.Squad squad : squads) {
            currentY += 12;
            if (this.expandedSquads.contains(squad.id)) {
                for (String member : this.getSortedMembers(squad)) {
                    if (my >= (double)currentY && my < (double)(currentY + 12) && mx > 30.0 && !member.equals(myName) && squad.members.contains(myName) && (squad.leader.equals(myName) || squad.bravoLeader.equals(myName) || squad.charlieLeader.equals(myName))) {
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

    private List<AASWorldData.Squad> getSortedSquads() {
        String myTeam = this.getPlayerTeam();
        String myDim = this.f_96541_.f_91073_.m_46472_().m_135782_().toString();
        int teamCMDId = myTeam.equalsIgnoreCase("Blue") ? ClientData.blueCMDId : ClientData.redCMDId;
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

    private void handleMapRightClick(double mouseX, double mouseY) {
        if (!this.isSquadLeaderOrFTL((Player)this.f_96541_.f_91074_)) {
            this.f_96541_.f_91074_.m_5661_((Component)Component.m_237113_((String)"Only SL and FTLs can place markers!").m_130940_(ChatFormatting.RED), true);
            return;
        }
        double bpp = this.mapRenderer.getBlocksPerPixel();
        double centerX = this.mapRenderer.getCenterX(this.f_96541_.f_91074_);
        double centerZ = this.mapRenderer.getCenterZ(this.f_96541_.f_91074_);
        int targetX = (int)(centerX + (mouseX - ((double)this.mapX + (double)this.mapSize / 2.0)) * bpp);
        int targetZ = (int)(centerZ + (mouseY - ((double)this.mapY + (double)this.mapSize / 2.0)) * bpp);
        this.f_96541_.m_91152_((Screen)new TacticalMapRadialScreen(targetX, targetZ));
    }

    public boolean m_6348_(double mouseX, double mouseY, int button) {
        this.mapRenderer.mouseReleased(button);
        return super.m_6348_(mouseX, mouseY, button);
    }

    public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.mapRenderer.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isSquadLeaderOrFTL(Player player) {
        String pName = player.m_6302_();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(pName) && !s.bravoLeader.equals(pName) && !s.charlieLeader.equals(pName)) continue;
            return true;
        }
        return false;
    }

    private void renderChatHistory(GuiGraphics gui, int mapY, int mapSize) {
        int chatX = 180;
        int chatBottomY = this.f_96544_ - 35;
        int chatTopY = mapY + mapSize + 10;
        int count = 0;
        for (Component msg : ClientData.menuChatHistory) {
            int y = chatBottomY - count * 10;
            if (y < chatTopY) break;
            gui.m_280614_(this.f_96547_, msg, chatX, y, -1, true);
            ++count;
        }
    }

    private void renderSquadList(GuiGraphics gui, int mouseX, int mouseY) {
        String myName = this.f_96541_.f_91074_.m_6302_();
        String myTeam = this.getPlayerTeam().toUpperCase();
        String myDim = this.f_96541_.f_91073_.m_46472_().m_135782_().toString();
        boolean amIInSquad = this.isPlayerInSquad();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        boolean myTeamVoteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        boolean amISquadLeaderAnywhere = false;
        for (AASWorldData.Squad s3 : ClientData.clientSquads) {
            if (!s3.leader.equals(myName)) continue;
            amISquadLeaderAnywhere = true;
            break;
        }
        int currentY = amISquadLeaderAnywhere && teamCMDId == -1 && !myTeamVoteActive ? 35 : 10;
        List myTeamSquads = ClientData.clientSquads.stream().filter(s -> s.team.equalsIgnoreCase(myTeam)).filter(s -> s.dimension != null && s.dimension.equals(myDim)).collect(Collectors.toList());
        myTeamSquads.sort((s1, s2) -> {
            if (s1.id == teamCMDId && teamCMDId != -1) {
                return -1;
            }
            if (s2.id == teamCMDId && teamCMDId != -1) {
                return 1;
            }
            return Integer.compare(s1.id, s2.id);
        });
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

    private void renderTopBar(GuiGraphics gui) {
        String myName = this.f_96541_.f_91074_.m_6302_();
        String myTeam = this.getPlayerTeam();
        String teamUpper = myTeam.toUpperCase();
        AASWorldData.Squad mySquad = null;
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            mySquad = s;
            break;
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
        this.applyCmdButton.f_93624_ = isLeader && myTeamCMDId == -1 && !myTeamVoteActive;
        String ticketText = String.valueOf(tickets);
        int textWidth = this.f_96547_.m_92895_(ticketText);
        int iconSize = 12;
        int gap = 5;
        int flagWidth = 32;
        int flagHeight = 18;
        int totalContentWidth = textWidth + gap + iconSize;
        if (flag != null) {
            totalContentWidth += flagWidth + gap;
        }
        int topBarCenterX = this.f_96543_ / 2;
        int currentX = topBarCenterX - totalContentWidth / 2;
        if (flag != null) {
            gui.m_280168_().m_85836_();
            gui.m_280168_().m_252880_(0.0f, 0.0f, 100.0f);
            int flagY = (30 - flagHeight) / 2;
            gui.m_280163_(flag, currentX, flagY, 0.0f, 0.0f, flagWidth, flagHeight, flagWidth, flagHeight);
            gui.m_280168_().m_85849_();
            currentX += flagWidth + gap;
        }
        ResourceLocation ticketIcon = new ResourceLocation("aas", "textures/gui/minimap_tickets.png");
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        int iconY = (30 - iconSize) / 2;
        gui.m_280411_(ticketIcon, currentX, iconY, iconSize, iconSize, 0.0f, 0.0f, 16, 16, 16, 16);
        int textY = 12;
        gui.m_280056_(this.f_96547_, ticketText, currentX += iconSize + gap, textY, -1, false);
    }

    private void playClickSound() {
        this.f_96541_.m_91106_().m_120367_((SoundInstance)SimpleSoundInstance.m_263171_((Holder)SoundEvents.f_12490_, (float)1.0f));
    }

    private boolean isPlayerInSquad() {
        String myName = this.f_96541_.f_91074_.m_6302_();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            return true;
        }
        return false;
    }

    private String getPlayerTeam() {
        if (this.f_96541_.f_91074_.m_5647_() != null) {
            return this.f_96541_.f_91074_.m_5647_().m_5758_();
        }
        return "NEUTRAL";
    }

    private ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equalsIgnoreCase("none")) {
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

    public boolean m_7043_() {
        return false;
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
}

