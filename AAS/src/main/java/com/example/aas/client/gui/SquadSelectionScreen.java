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
    static private final int SIDEBAR_WIDTH = 170;
    static private final int TOP_BAR_HEIGHT = 30;
    static private final ResourceLocation LOCK_ICON = new ResourceLocation("aas", "textures/gui/squad_lock.png");
    static private final ResourceLocation ARROW_DOWN = new ResourceLocation("aas", "textures/gui/arrow_down.png");
    static private final ResourceLocation ARROW_UP = new ResourceLocation("aas", "textures/gui/arrow_up.png");
    static private final ResourceLocation CENTER_ICON = new ResourceLocation("minecraft", "textures/item/compass_16.png");
    static private final ResourceLocation FLAG_UKRAINE = new ResourceLocation("aas", "textures/gui/flags/ukraine.png");
    static private final ResourceLocation FLAG_RUSSIA = new ResourceLocation("aas", "textures/gui/flags/russia.png");
    static private final ResourceLocation FLAG_USA = new ResourceLocation("aas", "textures/gui/flags/usa.png");
    static private final ResourceLocation FLAG_NATO = new ResourceLocation("aas", "textures/gui/flags/nato.png");
    static private final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("aas", "textures/gui/flags/bluefor.png");
    static private final ResourceLocation FLAG_REDFOR = new ResourceLocation("aas", "textures/gui/flags/redfor.png");
    static private final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("aas", "textures/gui/flags/insurgency.png");
    static private final ResourceLocation FLAG_PMC = new ResourceLocation("aas", "textures/gui/flags/pmc.png");
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
        super((Component)Component.literal((String)"Squad Selection"));
    }

    protected void init() {
        super.init();
        String myName = this.minecraft.player.getScoreboardName();
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
        this.applyCmdButton = new SquadButton(10, 10, 150, 20, (Component)Component.literal((String)"APPLY FOR CMD"), b -> {
            PacketHandler.INSTANCE.sendToServer((Object)new PacketRequestCMD());
            b.visible = false;
        });
        this.addRenderableWidget((GuiEventListener)this.applyCmdButton);
        this.nameInput = new EditBox(this.font, 10, this.height - 55, 150, 20, (Component)Component.literal((String)"Squad Name"));
        this.nameInput.setMaxLength(12);
        this.nameInput.setVisible(!isInSquad);
        this.addRenderableWidget((GuiEventListener)this.nameInput);
        this.createButton = (Button)this.addRenderableWidget((GuiEventListener)new SquadButton(10, this.height - 30, 150, 20, (Component)Component.literal((String)"Create Squad"), button -> PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(0, 0, this.nameInput.getValue()))));
        this.createButton.visible = !isInSquad;
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
        this.chatModeButton = (Button)this.addRenderableWidget((GuiEventListener)new SquadButton(chatX, inputY, 50, 20, this.getChatModeText(), button -> {
            ++this.chatMode;
            if (this.chatMode > 2) {
                this.chatMode = 0;
            }
            button.setMessage(this.getChatModeText());
        }));
        this.chatInput = new EditBox(this.font, chatX + 55, inputY, chatWidth - 55, 20, (Component)Component.literal((String)"Chat"));
        this.chatInput.setMaxLength(256);
        this.addRenderableWidget((GuiEventListener)this.chatInput);
    }

    private Component getChatModeText() {
        switch (this.chatMode) {
            case 0: {
                return Component.literal((String)"ALL").withStyle(ChatFormatting.LIGHT_PURPLE);
            }
            case 1: {
                return Component.literal((String)"TEAM").withStyle(ChatFormatting.BLUE);
            }
            case 2: {
                return Component.literal((String)"SQUAD").withStyle(ChatFormatting.GREEN);
            }
        }
        return Component.literal((String)"???");
    }

    public void tick() {
        super.tick();
        this.nameInput.tick();
        this.chatInput.tick();
        if (this.applyCmdButton != null) {
            boolean isBlue;
            String myName = this.minecraft.player.getScoreboardName();
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
                    PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadChat(msg, this.chatMode));
                    this.chatInput.setValue("");
                }
                return true;
            }
            if (this.nameInput.isFocused() && this.nameInput.isVisible()) {
                String name = this.nameInput.getValue();
                PacketHandler.INSTANCE.sendToServer((Object)new PacketSquadAction(0, 0, name));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        if (this.mapRenderer.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.showContextMenu) {
            if (button == 0) {
                String myName = this.minecraft.player.getScoreboardName();
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
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 1 && mouseX < 170.0 && mouseY < (double)(this.height - 60)) {
            String myName = this.minecraft.player.getScoreboardName();
            String myTeam = this.getPlayerTeam().toUpperCase();
            String myDim = this.minecraft.level.dimension().location().toString();
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
        if (button == 0 && mouseX < 170.0 && mouseY < (double)(this.height - 60)) {
            this.handleSquadListClicks(mouseX, mouseY);
            return true;
        }
        return false;
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
            int actionWidth = actionText.isEmpty() ? 0 : this.font.width(actionText);
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
        String myName = this.minecraft.player.getScoreboardName();
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
        String myDim = this.minecraft.level.dimension().location().toString();
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
        if (!this.isSquadLeaderOrFTL((Player)this.minecraft.player)) {
            this.minecraft.player.displayClientMessage((Component)Component.literal((String)"Only SL and FTLs can place markers!").withStyle(ChatFormatting.RED), true);
            return;
        }
        double bpp = this.mapRenderer.getBlocksPerPixel();
        double centerX = this.mapRenderer.getCenterX(this.minecraft.player);
        double centerZ = this.mapRenderer.getCenterZ(this.minecraft.player);
        int targetX = (int)(centerX + (mouseX - ((double)this.mapX + (double)this.mapSize / 2.0)) * bpp);
        int targetZ = (int)(centerZ + (mouseY - ((double)this.mapY + (double)this.mapSize / 2.0)) * bpp);
        this.minecraft.setScreen((Screen)new TacticalMapRadialScreen(targetX, targetZ));
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.mapRenderer.mouseReleased(button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.mapRenderer.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isSquadLeaderOrFTL(Player player) {
        String pName = player.getScoreboardName();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.leader.equals(pName) && !s.bravoLeader.equals(pName) && !s.charlieLeader.equals(pName)) continue;
            return true;
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
            if (y < chatTopY) break;
            gui.drawString(this.font, msg, chatX, y, -1, true);
            ++count;
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
                        ResourceLocation kitIcon = new ResourceLocation("aas", "textures/gui/kits/" + kName.toLowerCase().replace(" ", "_") + ".png");
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
        gui.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + w, this.contextMenuY + h, -300871407);
        gui.renderOutline(this.contextMenuX, this.contextMenuY, w, h, -11184811);
        for (int i = 0; i < options.size(); ++i) {
            boolean hover;
            int y = this.contextMenuY + 2 + i * 12;
            boolean bl = hover = mx >= this.contextMenuX && mx <= this.contextMenuX + w && my >= y && my < y + 12;
            if (hover) {
                gui.fill(this.contextMenuX + 1, y, this.contextMenuX + w - 1, y + 12, -12303292);
            }
            gui.drawString(this.font, (String)options.get(i), this.contextMenuX + 4, y + 2, hover ? 0xFFFFFF : 0xAAAAAA, false);
        }
    }

    private void renderTopBar(GuiGraphics gui) {
        String myName = this.minecraft.player.getScoreboardName();
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
        this.applyCmdButton.visible = isLeader && myTeamCMDId == -1 && !myTeamVoteActive;
        String ticketText = String.valueOf(tickets);
        int textWidth = this.font.width(ticketText);
        int iconSize = 12;
        int gap = 5;
        int flagWidth = 32;
        int flagHeight = 18;
        int totalContentWidth = textWidth + gap + iconSize;
        if (flag != null) {
            totalContentWidth += flagWidth + gap;
        }
        int topBarCenterX = this.width / 2;
        int currentX = topBarCenterX - totalContentWidth / 2;
        if (flag != null) {
            gui.pose().pushPose();
            gui.pose().translate(0.0f, 0.0f, 100.0f);
            int flagY = (30 - flagHeight) / 2;
            gui.blit(flag, currentX, flagY, 0.0f, 0.0f, flagWidth, flagHeight, flagWidth, flagHeight);
            gui.pose().popPose();
            currentX += flagWidth + gap;
        }
        ResourceLocation ticketIcon = new ResourceLocation("aas", "textures/gui/minimap_tickets.png");
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int iconY = (30 - iconSize) / 2;
        gui.blit(ticketIcon, currentX, iconY, iconSize, iconSize, 0.0f, 0.0f, 16, 16, 16, 16);
        int textY = 12;
        gui.drawString(this.font, ticketText, currentX += iconSize + gap, textY, -1, false);
    }

    private void playClickSound() {
        this.minecraft.getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((Holder)SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private boolean isPlayerInSquad() {
        String myName = this.minecraft.player.getScoreboardName();
        for (AASWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName)) continue;
            return true;
        }
        return false;
    }

    private String getPlayerTeam() {
        if (this.minecraft.player.getTeam() != null) {
            return this.minecraft.player.getTeam().getName();
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

    public boolean isPauseScreen() {
        return false;
    }

    private static class SquadButton
    extends Button {
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
}

