package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SquadUIHelper {

    private static final int SIDEBAR_WIDTH = 170;
    private static final ResourceLocation ARROW_DOWN = new ResourceLocation("pwpwarfare", "textures/gui/arrow_down.png");
    private static final ResourceLocation ARROW_UP = new ResourceLocation("pwpwarfare", "textures/gui/arrow_up.png");
    private static final ResourceLocation LOCK_ICON = new ResourceLocation("pwpwarfare", "textures/gui/squad_lock.png");

    private SquadUIHelper() {}

    public static void renderSquadList(GuiGraphics gui, int mx, int my, Set<Integer> expandedSquads, boolean applyCmdVisible) {
        String myName = Minecraft.getInstance().player.getScoreboardName();
        String myTeam = getPlayerTeam().toUpperCase();
        String myDim = Minecraft.getInstance().level.dimension().location().toString();
        boolean amIInSquad = isPlayerInSquad();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;

        int currentY = applyCmdVisible ? 35 : 10;
        List<WarfareWorldData.Squad> squads = getSortedSquads();
        int idx = 1;

        for (WarfareWorldData.Squad squad : squads) {
            boolean isMySquad = squad.members.contains(myName);
            boolean amILeader = squad.leader.equals(myName);
            boolean isExpanded = expandedSquads.contains(squad.id);
            boolean isCMD = squad.id == teamCMDId && teamCMDId != -1;

            String prefix = isCMD ? "[CMD] " : "";
            int squadNameColor = isCMD ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Colors.SUCCESS;
            gui.drawString(PWPTheme.Fonts.display(), idx + ".", 5, currentY, 0xFFFFFF, false);
            String display = prefix + squad.name + " (" + squad.members.size() + "/9)";
            int nameEndX = 25 + PWPTheme.Fonts.display().width(display);
            int minActionX = nameEndX + 28;
            gui.drawString(PWPTheme.Fonts.display(), display, 25, currentY, squadNameColor, false);

            String actionText = "";
            boolean isJoin = false, isLeave = false, isDisabled = false;
            if (isMySquad) {
                actionText = "Покинуть";
                isLeave = true;
            } else if (!amIInSquad) {
                if (squad.isLocked) { actionText = "Закрыт"; isDisabled = true; }
                else if (squad.members.size() >= 9) { actionText = "Полный"; isDisabled = true; }
                else { actionText = "Вступить"; isJoin = true; }
            }

            int actionX = 0;
            if (!actionText.isEmpty()) {
                int aw = PWPTheme.Fonts.display().width(actionText) + 12;
                int ah = 12;
                actionX = Math.max(SIDEBAR_WIDTH - aw - 6, minActionX);
                int actionY = currentY - 1;
                boolean hover = mx >= actionX && mx <= actionX + aw && my >= actionY && my <= actionY + ah && !isDisabled;
                int r = PWPTheme.Spacing.RADIUS_SMALL;

                if (isJoin) {
                    int bg = hover ? PWPTheme.Styles.Button.DARK_HOVER : PWPTheme.Styles.Button.DARK_BG;
                    int border = hover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Styles.Button.DARK_BORDER;
                    RoundedRect.fill(gui, actionX, actionY, aw, ah, r, bg);
                    RoundedRect.border(gui, actionX, actionY, aw, ah, r, 1, border);
                    gui.drawString(PWPTheme.Fonts.display(), actionText, actionX + aw / 2 - PWPTheme.Fonts.display().width(actionText) / 2, actionY + 2, PWPTheme.Colors.TEXT_PRIMARY, false);
                } else if (isLeave) {
                    if (hover) {
                        RoundedRect.fill(gui, actionX, actionY, aw, ah, r, 0x22FF0000);
                    }
                    int textCol = hover ? PWPTheme.Colors.DANGER : PWPTheme.Colors.TEXT_DIM;
                    gui.drawString(PWPTheme.Fonts.display(), actionText, actionX + aw / 2 - PWPTheme.Fonts.display().width(actionText) / 2, actionY + 2, textCol, false);
                } else if (isDisabled) {
                    gui.drawString(PWPTheme.Fonts.display(), actionText, actionX + aw / 2 - PWPTheme.Fonts.display().width(actionText) / 2, actionY + 2, PWPTheme.Colors.TEXT_DIM, false);
                }
            } else {
                // No action button, shift icons right if name is long
                if (minActionX > 160) actionX = minActionX;
            }

            renderSquadIcons(gui, squad, amILeader, isExpanded, actionX, currentY);

            currentY += 12;
            if (isExpanded) {
                currentY = renderMembers(gui, mx, my, currentY, squad, isMySquad, myName);
                currentY += 4;
            }
            currentY += 4;
            idx++;
        }
    }

    private static void renderSquadIcons(GuiGraphics gui, WarfareWorldData.Squad squad, boolean amILeader, boolean isExpanded, int actionX, int currentY) {
        RenderSystem.enableBlend();
        int arrowX = (actionX > 0 ? actionX : 160) - 12;
        if (isExpanded) {
            RenderSystem.setShaderColor(1f, 0.8f, 0.2f, 1f);
            gui.blit(ARROW_DOWN, arrowX, currentY + 1, 0, 0, 8, 8, 8, 8);
        } else {
            RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1f);
            gui.blit(ARROW_UP, arrowX, currentY + 1, 0, 0, 8, 8, 8, 8);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (amILeader || squad.isLocked) {
            int lockX = arrowX - 12;
            RenderSystem.setShaderColor(squad.isLocked ? 1f : 0.6f, squad.isLocked ? 0.8f : 0.6f, 0.2f, 1f);
            gui.blit(LOCK_ICON, lockX, currentY + 1, 0, 0, 8, 8, 8, 8);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    private static int renderMembers(GuiGraphics gui, int mx, int my, int currentY, WarfareWorldData.Squad squad, boolean isMySquad, String myName) {
        for (String member : getSortedMembers(squad)) {
            boolean isOnline = Minecraft.getInstance().getConnection().getPlayerInfo(member) != null;
            int col = getMemberColor(squad, member, isOnline);
            int xOffset = 30;

            String kName = ClientData.playerKits.getOrDefault(member, "Unassigned");
            if (!kName.equals("Unassigned") && !kName.isEmpty()) {
                ResourceLocation kitIcon = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + kName.toLowerCase().replace(" ", "_") + ".png");
                gui.blit(kitIcon, xOffset, currentY, 0, 0, 10, 10, 10, 10);
                xOffset += 12;
            }

            String displayMember = member;
            int maxMNameW = Math.max(1, SIDEBAR_WIDTH - xOffset - 4);
            if (PWPTheme.Fonts.display().width(displayMember) > maxMNameW) {
                displayMember = PWPTheme.Fonts.display().plainSubstrByWidth(displayMember, maxMNameW - 4) + "\u2026";
            }
            gui.drawString(PWPTheme.Fonts.display(), displayMember, xOffset, currentY + 1, col, false);
            currentY += 12;
        }
        return currentY;
    }

    private static int getMemberColor(WarfareWorldData.Squad squad, String member, boolean isOnline) {
        if (!isOnline) return 0xFFAAAAAA;
        if (member.equals(squad.leader)) return PWPTheme.Colors.SUCCESS;
        if (member.equals(squad.bravoLeader)) return PWPTheme.Colors.INFO;
        if (squad.bravoMembers.contains(member)) return 0xFFAAD4AA;
        if (member.equals(squad.charlieLeader)) return PWPTheme.Colors.TEXT_ACCENT;
        if (squad.charlieMembers.contains(member)) return 0xFF88BBFF;
        return 0xFFFFFF;
    }

    public static void handleSquadClick(double mx, double my, Set<Integer> expandedSquads, PWPContextMenu contextMenu, boolean applyCmdVisible) {
        String myName = Minecraft.getInstance().player.getScoreboardName();
        boolean amIInSquad = isPlayerInSquad();
        String myTeam = getPlayerTeam().toUpperCase();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        int currentY = applyCmdVisible ? 35 : 10;

        for (WarfareWorldData.Squad squad : getSortedSquads()) {
            boolean isMySquad = squad.members.contains(myName);
            boolean amILeader = squad.leader.equals(myName);
            boolean isExpanded = expandedSquads.contains(squad.id);
            boolean isCMD = squad.id == teamCMDId && teamCMDId != -1;

            // Use SAME action text and width calc as renderSquadList
            String actionText = "";
            if (isMySquad) actionText = "Покинуть";
            else if (!amIInSquad) {
                if (squad.isLocked) actionText = "Закрыт";
                else if (squad.members.size() >= 9) actionText = "Полный";
                else actionText = "Вступить";
            }

            String prefix2 = isCMD ? "[CMD] " : "";
            String display2 = prefix2 + squad.name + " (" + squad.members.size() + "/9)";
            int nameEndX2 = 25 + PWPTheme.Fonts.display().width(display2);
            int minActionX2 = nameEndX2 + 28;

            int aw = actionText.isEmpty() ? 0 : PWPTheme.Fonts.display().width(actionText) + 12;
            int actionX = aw > 0 ? Math.max(SIDEBAR_WIDTH - aw - 6, minActionX2) : (minActionX2 > 160 ? minActionX2 : 0);
            int arrowX = (actionX > 0 ? actionX : 160) - 12;
            int lockX = arrowX - 12;

            if (my >= currentY && my <= currentY + 11) {
                if (aw > 0 && mx >= actionX && mx <= actionX + aw) {
                    if (isMySquad) PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(2, squad.id, ""));
                    else if (!squad.isLocked && squad.members.size() < 9) PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(1, squad.id, ""));
                } else if (mx >= arrowX && mx <= arrowX + 10) {
                    if (isExpanded) expandedSquads.remove(squad.id);
                    else expandedSquads.add(squad.id);
                } else if (amILeader && mx >= lockX && mx <= lockX + 10) {
                    PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(5, squad.id, ""));
                }
                return;
            }

            currentY += 12;
            if (isExpanded) {
                for (String member : getSortedMembers(squad)) {
                    if (my >= currentY && my <= currentY + 11) {
                        if (isMySquad && !member.equals(myName)) {
                            List<String> options = buildContextOptions(squad, myName);
                            if (!options.isEmpty()) {
                                contextMenu.show((int) mx, (int) my, options, idx -> handleContextAction(idx, options, squad.id, member));
                            }
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

    private static List<String> buildContextOptions(WarfareWorldData.Squad squad, String myName) {
        List<String> options = new ArrayList<>();
        boolean amISL = squad.leader.equals(myName);
        boolean amIBravo = squad.bravoLeader.equals(myName);
        boolean amICharlie = squad.charlieLeader.equals(myName);

        if (amISL) {
            options.add("Promote to SL");
            options.add("Set FTL Bravo");
            options.add("Set FTL Charlie");
            options.add("Add to Bravo");
            options.add("Add to Charlie");
            options.add("Remove from FT");
            options.add("Kick from Squad");
            options.add("Disband Squad");
        } else if (amIBravo) {
            options.add("Pass FTL Bravo");
            options.add("Add to Bravo");
            options.add("Remove from FT");
        } else if (amICharlie) {
            options.add("Pass FTL Charlie");
            options.add("Add to Charlie");
            options.add("Remove from FT");
        }
        return options;
    }

    private static void handleContextAction(int idx, List<String> options, int squadId, String target) {
        if (idx < 0 || idx >= options.size()) return;
        String opt = options.get(idx);
        switch (opt) {
            case "Promote to SL" -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(4, squadId, target));
            case "Set FTL Bravo", "Pass FTL Bravo" -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(6, squadId, target));
            case "Set FTL Charlie", "Pass FTL Charlie" -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(7, squadId, target));
            case "Add to Bravo" -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(8, squadId, target));
            case "Add to Charlie" -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(9, squadId, target));
            case "Remove from FT" -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(10, squadId, target));
            case "Kick from Squad" -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(3, squadId, target));
            case "Disband Squad" -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(11, squadId, ""));
        }
    }

    public static void renderChatHistory(GuiGraphics gui, int chatX, int mapBottomY, int height) {
        int chatBottomY = height - 35;
        int chatTopY = mapBottomY + 10;
        int count = 0;
        for (Component msg : ClientData.menuChatHistory) {
            int y = chatBottomY - count * 10;
            if (y < chatTopY) break;
            gui.drawString(PWPTheme.Fonts.display(), msg, chatX, y, 0xFFFFFF, false);
            count++;
        }
    }

    public static void renderVoiceActivity(GuiGraphics gui, int x, int y) {
        long now = System.currentTimeMillis();
        ResourceLocation radioIcon = new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_radio.png");
        ResourceLocation voiceIcon = new ResourceLocation("pwpwarfare", "textures/gui/voice_icon.png");

        for (var entry : ClientData.RADIO_SPEAKERS.entrySet()) {
            if (now - entry.getValue() >= 500) continue;
            renderSpeakerRow(gui, x, y, entry.getKey(), PWPTheme.Colors.TEXT_ACCENT, radioIcon);
            y += 14;
        }
        for (var entry : ClientData.SQUAD_SPEAKERS.entrySet()) {
            if (now - entry.getValue() >= 500) continue;
            renderSpeakerRow(gui, x, y, entry.getKey(), PWPTheme.Colors.INFO, voiceIcon);
            y += 14;
        }
    }

    private static void renderSpeakerRow(GuiGraphics gui, int x, int y, String name, int color, ResourceLocation icon) {
        int tw = PWPTheme.Fonts.display().width(name) + 15;
        gui.fill(x, y - 2, x + tw + 4, y + 10, 0xCC000000);
        RenderSystem.enableBlend();
        float r = (color >> 16 & 0xFF) / 255f, g = (color >> 8 & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1f);
        gui.blit(icon, x + 3, y, 0, 0, 8, 8, 8, 8);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        gui.drawString(PWPTheme.Fonts.display(), name, x + 14, y, color, false);
    }

    public static void renderTeamHeader(GuiGraphics gui, int startX, int startY, boolean isBlue) {
        String teamName = getPlayerTeam().toUpperCase();
        int tickets = teamName.contains("BLUE") ? ClientData.BLUE_TICKETS : ClientData.RED_TICKETS;
        String faction = teamName.contains("BLUE") ? ClientData.BLUE_FACTION : ClientData.RED_FACTION;
        String customName = teamName.contains("BLUE") ? ClientData.customBlueName : ClientData.customRedName;
        ResourceLocation flagTex = getFlagTexture(faction);
        ResourceLocation ticketIcon = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");

        if (flagTex != null) {
            RenderSystem.enableBlend();
            gui.blit(flagTex, startX, startY, 32, 18, 0, 0, 64, 36, 64, 36);
        }
        int teamColor = isBlue ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.TEAM_RED;
        gui.drawString(PWPTheme.Fonts.display(), customName, startX + 38, startY, teamColor, false);
        RenderSystem.enableBlend();
        gui.blit(ticketIcon, startX + 38, startY + 11, 0, 0, 8, 8, 8, 8);
        gui.drawString(PWPTheme.Fonts.display(), String.valueOf(tickets), startX + 50, startY + 11, PWPTheme.Colors.TEXT_ACCENT, false);
    }

    public static boolean isApplyCmdVisible() {
        String myName = Minecraft.getInstance().player.getScoreboardName();
        boolean isBlue = getPlayerTeam().toUpperCase().contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        boolean voteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        if (teamCMDId != -1 || voteActive) return false;
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.leader.equals(myName)) return true;
        }
        return false;
    }

    public static boolean isPlayerInSquad() {
        String myName = Minecraft.getInstance().player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) return true;
        }
        return false;
    }

    public static boolean isSquadLeaderOrFTL(Player player) {
        String pName = player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.leader.equals(pName) || s.bravoLeader.equals(pName) || s.charlieLeader.equals(pName)) return true;
        }
        return false;
    }

    public static String getPlayerTeam() {
        Player p = Minecraft.getInstance().player;
        return p != null && p.getTeam() != null ? p.getTeam().getName() : "NEUTRAL";
    }

    public static List<WarfareWorldData.Squad> getSortedSquads() {
        String myTeam = getPlayerTeam().toUpperCase();
        String myDim = Minecraft.getInstance().level.dimension().location().toString();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        return ClientData.clientSquads.stream()
            .filter(s -> s.team.equalsIgnoreCase(myTeam))
            .filter(s -> s.dimension != null && s.dimension.equals(myDim))
            .sorted((a, b) -> {
                if (a.id == teamCMDId) return -1;
                if (b.id == teamCMDId) return 1;
                return Integer.compare(a.id, b.id);
            }).collect(Collectors.toList());
    }

    public static List<String> getSortedMembers(WarfareWorldData.Squad squad) {
        List<String> sorted = new ArrayList<>();
        if (!squad.leader.isEmpty() && squad.members.contains(squad.leader)) sorted.add(squad.leader);
        for (String m : squad.members) {
            if (!m.equals(squad.leader) && !squad.bravoMembers.contains(m) && !squad.charlieMembers.contains(m)) sorted.add(m);
        }
        if (!squad.bravoLeader.isEmpty() && squad.members.contains(squad.bravoLeader)) sorted.add(squad.bravoLeader);
        for (String m : squad.bravoMembers) {
            if (!m.equals(squad.bravoLeader) && squad.members.contains(m)) sorted.add(m);
        }
        if (!squad.charlieLeader.isEmpty() && squad.members.contains(squad.charlieLeader)) sorted.add(squad.charlieLeader);
        for (String m : squad.charlieMembers) {
            if (!m.equals(squad.charlieLeader) && squad.members.contains(m)) sorted.add(m);
        }
        return sorted;
    }

    private static ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equalsIgnoreCase("none")) return null;
        return new ResourceLocation("pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png");
    }

    public static int getSidebarWidth() { return SIDEBAR_WIDTH; }
}
