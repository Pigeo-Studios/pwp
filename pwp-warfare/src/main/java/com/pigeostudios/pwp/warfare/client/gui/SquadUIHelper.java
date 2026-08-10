package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.components.PWPContextMenu;
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

/**
 * Список отрядов в Squad-стиле: строка = цветной кружок с номером + название + счётчик n/9
 * (красный при locked), лидер мельче под названием, замок и шеврон справа. Свой отряд
 * разворачивается: члены с иконкой роли и бейджами SL/FTL. JOIN/LEAVE — кнопки строки.
 */
public class SquadUIHelper {

    private static int actualWidth = 170;
    private static final int ROW_H = 20;
    private static final int MEMBER_H = 13;
    /** Прокрутка списка отрядов (общая для всех экранов, где рендерится список). */
    private static int listScroll = 0;
    private static final ResourceLocation ARROW_DOWN = new ResourceLocation("pwpwarfare", "textures/gui/arrow_down.png");
    private static final ResourceLocation ARROW_UP = new ResourceLocation("pwpwarfare", "textures/gui/arrow_up.png");
    private static final ResourceLocation LOCK_ICON = new ResourceLocation("pwpwarfare", "textures/gui/squad_lock.png");

    /** Палитра цветов кружков отрядов (по номеру). */
    private static final int[] SQUAD_COLORS = {
        0xFF4CAF50, 0xFF3D6FA5, 0xFF26C6DA, 0xFFC8812A, 0xFFAB47BC,
        0xFFFDD835, 0xFFEC407A, 0xFF8D9AA5
    };

    private SquadUIHelper() {}

    public static void setWidth(int w) { actualWidth = Math.max(100, w); }
    public static int getSidebarWidth() { return actualWidth; }

public static void renderSquadList(GuiGraphics gui, int mx, int my, Set<Integer> expandedSquads, boolean applyCmdVisible) {
        String myName = Minecraft.getInstance().player.getScoreboardName();
        boolean amIInSquad = isPlayerInSquad();
        boolean isBlue = getPlayerTeam().toUpperCase().contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;

        int currentY = 0;
        List<WarfareWorldData.Squad> squads = getSortedSquads();
        int idx = 1;

        for (WarfareWorldData.Squad squad : squads) {
            boolean isMySquad = squad.members.contains(myName);
            boolean amILeader = squad.leader.equals(myName);
            boolean isExpanded = expandedSquads.contains(squad.id);
            boolean isCMD = squad.id == teamCMDId && teamCMDId != -1;

            renderSquadRow(gui, mx, my, currentY - listScroll, idx, squad, isMySquad, amILeader, isExpanded, isCMD, amIInSquad);

            currentY += ROW_H;
            if (isExpanded) {
                currentY = renderMembers(gui, mx, my, currentY, squad, isMySquad, myName);
                currentY += 2;
            }
            currentY += 2;
            idx++;
        }
    }

    /** Полная высота списка (свернутые группы — строки без членов). */
    public static int listContentHeight() {
        int h = 0;
        for (WarfareWorldData.Squad squads : getSortedSquads()) {
            h += ROW_H + 2;
        }
        return h;
    }

    /** Колесо мыши над списком: прокрутка. Возвращает true, если прокрутил. */
    public static boolean scrollSquads(double mx, double my, double delta, int viewH) {
        if (mx < 0 || mx > actualWidth) return false;
        if (viewH <= 0) return false;
        int maxScroll = Math.max(0, listContentHeight() - viewH);
        listScroll = (int) Math.max(0, Math.min(maxScroll, listScroll - delta * 16));
        return true;
    }

    /** Автопрокрутка: свой отряд всегда виден в списке (M-карта/деплой). */
    public static void autoRevealMySquad(int viewH) {
        String myName = Minecraft.getInstance().player.getScoreboardName();
        int y = 0;
        int myY = -1;
        for (WarfareWorldData.Squad squad : getSortedSquads()) {
            if (squad.members.contains(myName)) { myY = y; break; }
            y += ROW_H + 2;
        }
        if (myY < 0) return;
        int maxScroll = Math.max(0, listContentHeight() - viewH);
        if (myY < listScroll) listScroll = Math.max(0, myY);
        else if (myY + ROW_H > listScroll + viewH) listScroll = Math.max(0, Math.min(maxScroll, myY + ROW_H - viewH));
    }

    private static void renderSquadRow(GuiGraphics gui, int mx, int my, int y, int idx,
                                       WarfareWorldData.Squad squad, boolean isMySquad, boolean amILeader,
                                       boolean isExpanded, boolean isCMD, boolean amIInSquad) {
        var f = PWPTheme.Fonts.display();
        boolean hover = mx >= 0 && mx <= actualWidth && my >= y && my <= y + ROW_H;

        // Кружок с номером
        int c = SQUAD_COLORS[(idx - 1) % SQUAD_COLORS.length];
        if (isMySquad) c = PWPTheme.Colors.ACCENT;
        gui.fill(2, y + 1, 14, y + 13, c);
        gui.drawCenteredString(f, String.valueOf(idx), 8, y + 2, 0xFF0A0C0E);

        // Название + счётчик
        String prefix = isCMD ? "[CMD] " : "";
        int nameColor = isMySquad ? PWPTheme.Colors.TEXT_ACCENT : (isCMD ? PWPTheme.Colors.TEXT_ACCENT : 0xFFFFFFFF);
        String display = prefix + squad.name;
        int maxNameW = actualWidth - 92;
        if (f.width(display) > maxNameW) display = f.plainSubstrByWidth(display, maxNameW - 4) + "\u2026";
        gui.drawString(f, display, 22, y + 1, nameColor, false);

        int countColor = squad.isLocked ? PWPTheme.Colors.DANGER : PWPTheme.Colors.TEXT_SECONDARY;
        gui.drawString(f, squad.members.size() + "/9", 22, y + 10, countColor, false);

        // Кнопка действия (Вступить/Покинуть/Закрыт/Полный)
        String actionText = "";
        boolean isJoin = false, isLeave = false, isDisabled = false;
        if (isMySquad) { actionText = "Покинуть"; isLeave = true; }
        else if (!amIInSquad) {
            if (squad.isLocked) { actionText = "Закрыт"; isDisabled = true; }
            else if (squad.members.size() >= 9) { actionText = "Полный"; isDisabled = true; }
            else { actionText = "Вступить"; isJoin = true; }
        }

        int actionX = 0;
        if (!actionText.isEmpty()) {
            int aw = f.width(actionText) + 12;
            int ah = 13;
            actionX = actualWidth - aw - 24;
            int actionY = y + 3;
            boolean bHover = mx >= actionX && mx <= actionX + aw && my >= actionY && my <= actionY + ah && !isDisabled;

            if (isJoin) {
                int bg = bHover ? PWPTheme.Styles.Button.DARK_HOVER : PWPTheme.Styles.Button.DARK_BG;
                int border = bHover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Styles.Button.DARK_BORDER;
                RoundedRect.fill(gui, actionX, actionY, aw, ah, 3, bg);
                RoundedRect.border(gui, actionX, actionY, aw, ah, 3, 1, border);
                gui.drawString(f, actionText, actionX + 2, actionY + 3, PWPTheme.Colors.TEXT_PRIMARY, false);
            } else if (isLeave) {
                int bg = bHover ? 0x22FF0000 : 0;
                if (bHover) RoundedRect.fill(gui, actionX, actionY, aw, ah, 3, bg);
                gui.drawString(f, actionText, actionX + 2, actionY + 3,
                    bHover ? PWPTheme.Colors.DANGER : PWPTheme.Colors.TEXT_DIM, false);
            } else if (isDisabled) {
                gui.drawString(f, actionText, actionX + 2, actionY + 3, PWPTheme.Colors.TEXT_DIM, false);
            }
        }

        // Шеврон + замок справа
        int arrowX = actionX > 0 ? actionX - 14 : actualWidth - 16;
        int lockX = arrowX - 12;
        RenderSystem.enableBlend();
        if (isExpanded) {
            RenderSystem.setShaderColor(1f, 0.8f, 0.2f, 1f);
            gui.blit(ARROW_DOWN, arrowX, y + 5, 0, 0, 8, 8, 8, 8);
        } else {
            RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 1f);
            gui.blit(ARROW_UP, arrowX, y + 5, 0, 0, 8, 8, 8, 8);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (squad.isLocked || amILeader) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 0.75f, 0.3f, 1f);
            gui.blit(LOCK_ICON, lockX, y + 5, 0, 0, 8, 8, 8, 8);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        if (hover) {
            gui.fill(0, y, actualWidth, y + ROW_H, 0x08FFFFFF);
        }
    }

    private static int renderMembers(GuiGraphics gui, int mx, int my, int currentY,
                                     WarfareWorldData.Squad squad, boolean isMySquad, String myName) {
        var f = PWPTheme.Fonts.display();
        for (String member : getSortedMembers(squad)) {
            boolean isOnline = Minecraft.getInstance().getConnection().getPlayerInfo(member) != null;
            int col = getMemberColor(squad, member, isOnline);
            int xOffset = 26;
            int y = currentY - listScroll;

            // Иконка кита
            String kName = ClientData.playerKits.getOrDefault(member, "Unassigned");
            if (!kName.equals("Unassigned") && !kName.isEmpty()) {
                try {
                    ResourceLocation kitIcon = new ResourceLocation("pwpwarfare", "textures/gui/kits/" + kName.toLowerCase().replace(" ", "_") + ".png");
                    gui.blit(kitIcon, xOffset, y + 2, 0, 0, 8, 8, 8, 8);
                } catch (Exception ignored) {}
                xOffset += 11;
            }

            // Бейдж SL/FTL + секция
            String badge = member.equals(squad.leader) ? "\u2605 " : "";
            if (badge.isEmpty() && (member.equals(squad.bravoLeader) || member.equals(squad.charlieLeader))) badge = "\u25C6 ";
            int badgeCol = member.equals(squad.leader) ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.INFO;

            String displayMember = badge + member;
            int maxMNameW = Math.max(1, actualWidth - xOffset - 4);
            if (f.width(displayMember) > maxMNameW) {
                displayMember = f.plainSubstrByWidth(displayMember, maxMNameW - 4) + "\u2026";
            }
            if (!badge.isEmpty()) {
                gui.drawString(f, badge, xOffset, y + 1, badgeCol, false);
                gui.drawString(f, member, xOffset + f.width(badge), y + 1, col, false);
            } else {
                gui.drawString(f, displayMember, xOffset, y + 1, col, false);
            }
            currentY += MEMBER_H;
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
        boolean isBlue = getPlayerTeam().toUpperCase().contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;

        int currentY = 0;
        int idx = 1;
        for (WarfareWorldData.Squad squad : getSortedSquads()) {
            boolean isMySquad = squad.members.contains(myName);
            boolean amILeader = squad.leader.equals(myName);
            boolean isExpanded = expandedSquads.contains(squad.id);
            boolean isCMD = squad.id == teamCMDId && teamCMDId != -1;

            // Зона строки
            if (my >= currentY - listScroll && my <= currentY - listScroll + ROW_H) {
                handleRowClick(mx, my, currentY, idx, squad, isMySquad, amILeader, isExpanded, isCMD, amIInSquad, expandedSquads);
                return;
            }
            currentY += ROW_H;
            if (isExpanded) {
                for (String member : getSortedMembers(squad)) {
                    if (my >= currentY - listScroll && my <= currentY - listScroll + MEMBER_H - 1) {
                        if (isMySquad && !member.equals(myName)) {
                            List<String> options = buildContextOptions(squad, myName);
                            if (!options.isEmpty()) {
                                contextMenu.show((int) mx, (int) my, options, i -> handleContextAction(i, options, squad.id, member));
                            }
                        }
                        return;
                    }
                    currentY += MEMBER_H;
                }
                currentY += 2;
            }
            currentY += 2;
            idx++;
        }
    }

    private static void handleRowClick(double mx, double my, int yIn, int idx, WarfareWorldData.Squad squad,
                                       boolean isMySquad, boolean amILeader, boolean isExpanded,
                                       boolean isCMD, boolean amIInSquad, Set<Integer> expandedSquads) {
        var f = PWPTheme.Fonts.display();
        int y = yIn - listScroll;

        // Кнопка действия
        String actionText = "";
        if (isMySquad) actionText = "Покинуть";
        else if (!amIInSquad) {
            if (squad.isLocked) actionText = "Закрыт";
            else if (squad.members.size() >= 9) actionText = "Полный";
            else actionText = "Вступить";
        }
        int actionX = 0;
        if (!actionText.isEmpty()) {
            int aw = f.width(actionText) + 12;
            actionX = actualWidth - aw - 24;
            if (mx >= actionX && mx <= actionX + aw && my >= y + 3 && my <= y + 16) {
                if (isMySquad) PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(2, squad.id, ""));
                else if (!squad.isLocked && squad.members.size() < 9) PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(1, squad.id, ""));
                return;
            }
        }

        // Шеврон
        int arrowX = actionX > 0 ? actionX - 14 : actualWidth - 16;
        if (mx >= arrowX && mx <= arrowX + 10) {
            if (isExpanded) expandedSquads.remove(squad.id);
            else expandedSquads.add(squad.id);
            return;
        }

        // Замок (только лидер)
        int lockX = arrowX - 12;
        if (amILeader && mx >= lockX && mx <= lockX + 10) {
            PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(5, squad.id, ""));
            return;
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
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        return ClientData.clientSquads.stream()
            .filter(s -> s.team.equalsIgnoreCase(myTeam))
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
}
