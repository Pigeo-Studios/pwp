package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.SquadList;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class SquadUIHelper {

    private static final int SIDEBAR_WIDTH = 170;
    private static final ResourceLocation ARROW_DOWN =
            new ResourceLocation("pwpwarfare", "textures/gui/arrow_down.png");
    private static final ResourceLocation ARROW_UP = new ResourceLocation("pwpwarfare", "textures/gui/arrow_up.png");
    private static final ResourceLocation LOCK_ICON = new ResourceLocation("pwpwarfare", "textures/gui/squad_lock.png");

    private SquadUIHelper() {}

    /** Старые экраны: список в своих координатах (x от 0, ширина {@link #getSidebarWidth}). */
    private static Rect legacyArea() {
        return Rect.of(0, 0, SIDEBAR_WIDTH, Short.MAX_VALUE);
    }

    /** Отступ сверху под кнопку «стать командиром» (как раньше: 35 / 10). */
    private static int legacyInset(boolean applyCmdVisible) {
        return applyCmdVisible ? 35 : 10;
    }

    /** Список отрядов в координатах сайдбара; новые экраны вызывают {@link SquadList} с областью. */
    public static void renderSquadList(
            GuiGraphics gui, int mx, int my, Set<Integer> expandedSquads, boolean applyCmdVisible) {
        SquadList.render(gui, legacyArea(), legacyInset(applyCmdVisible), mx, my, expandedSquads);
    }

    public static void handleSquadClick(
            double mx, double my, Set<Integer> expandedSquads, PWPContextMenu contextMenu, boolean applyCmdVisible) {
        SquadList.click(legacyArea(), legacyInset(applyCmdVisible), mx, my, expandedSquads, contextMenu);
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
        ResourceLocation radioIcon = com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/voice_icon_radio.png");
        ResourceLocation voiceIcon = com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/voice_icon.png");

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
        ResourceLocation ticketIcon = com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/minimap_tickets.png");

        if (flagTex != null) {
            RenderSystem.enableBlend();
            gui.blit(flagTex, startX, startY, 32, 18, 0, 0, 64, 36, 64, 36);
        }
        int teamColor = isBlue ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.TEAM_RED;
        gui.drawString(PWPTheme.Fonts.display(), customName, startX + 38, startY, teamColor, false);
        RenderSystem.enableBlend();
        gui.blit(ticketIcon, startX + 38, startY + 11, 0, 0, 8, 8, 8, 8);
        gui.drawString(
                PWPTheme.Fonts.display(),
                String.valueOf(tickets),
                startX + 50,
                startY + 11,
                PWPTheme.Colors.TEXT_ACCENT,
                false);
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
                })
                .collect(Collectors.toList());
    }

    public static List<String> getSortedMembers(WarfareWorldData.Squad squad) {
        List<String> sorted = new ArrayList<>();
        if (!squad.leader.isEmpty() && squad.members.contains(squad.leader)) sorted.add(squad.leader);
        for (String m : squad.members) {
            if (!m.equals(squad.leader) && !squad.bravoMembers.contains(m) && !squad.charlieMembers.contains(m))
                sorted.add(m);
        }
        if (!squad.bravoLeader.isEmpty() && squad.members.contains(squad.bravoLeader)) sorted.add(squad.bravoLeader);
        for (String m : squad.bravoMembers) {
            if (!m.equals(squad.bravoLeader) && squad.members.contains(m)) sorted.add(m);
        }
        if (!squad.charlieLeader.isEmpty() && squad.members.contains(squad.charlieLeader))
            sorted.add(squad.charlieLeader);
        for (String m : squad.charlieMembers) {
            if (!m.equals(squad.charlieLeader) && squad.members.contains(m)) sorted.add(m);
        }
        return sorted;
    }

    private static ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equalsIgnoreCase("none")) return null;
        return com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/flags/" + faction.toLowerCase() + ".png");
    }

    public static int getSidebarWidth() {
        return SIDEBAR_WIDTH;
    }
}
