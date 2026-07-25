package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketTeamSelect;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.PWPCard;
import com.pwp.coreclient.gui.screens.PWPSelectionScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class TeamSelectionScreen extends PWPSelectionScreen {

    private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("pwpwarfare", "textures/gui/flags/ukraine.png");
    private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("pwpwarfare", "textures/gui/flags/russia.png");
    private static final ResourceLocation FLAG_USA = new ResourceLocation("pwpwarfare", "textures/gui/flags/usa.png");
    private static final ResourceLocation FLAG_NATO = new ResourceLocation("pwpwarfare", "textures/gui/flags/nato.png");
    private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
    private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
    private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("pwpwarfare", "textures/gui/flags/insurgency.png");
    private static final ResourceLocation FLAG_PMC = new ResourceLocation("pwpwarfare", "textures/gui/flags/pmc.png");

    public TeamSelectionScreen() {
        super(
            Component.translatable("gui.pwpwarfare.team_select.title").getString(),
            null,
            List.of("BLUE", "RED"),
            TeamSelectionScreen::renderTeamCard,
            TeamSelectionScreen::onTeamSelected,
            -1,
            160, 140, 30,
            TeamSelectionScreen::isTeamDisabled,
            true
        );
    }

    private static boolean isTeamDisabled(int index) {
        return index == 0
            ? ClientData.BLUE_PLAYER_COUNT >= ClientData.RED_PLAYER_COUNT + 2
            : ClientData.RED_PLAYER_COUNT >= ClientData.BLUE_PLAYER_COUNT + 2;
    }

    private static void renderTeamCard(GuiGraphics gui, SelectionItem si) {
        boolean isBlue = si.index == 0;
        int cx = si.x + si.w / 2;

        String teamName = isBlue
            ? formatFactionName(ClientData.BLUE_FACTION, ClientData.customBlueName)
            : formatFactionName(ClientData.RED_FACTION, ClientData.customRedName);
        int teamColor = isBlue ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.TEAM_RED;

        gui.drawString(PWPTheme.Fonts.display(), Component.literal(teamName)
            .withStyle(isBlue ? ChatFormatting.BLUE : ChatFormatting.RED), cx - PWPTheme.Fonts.display().width(teamName) / 2, si.y + 8, teamColor, false);

        ResourceLocation flag = getFlagTexture(isBlue ? ClientData.BLUE_FACTION : ClientData.RED_FACTION);
        int flagX = si.x + (si.w - 80) / 2;
        int flagY = si.y + 22;

        if (flag != null) {
            float alpha = si.disabled ? 0.3F : 1.0F;
            RenderSystem.setShaderColor(alpha, alpha, alpha, alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            gui.blit(flag, flagX, flagY, 0.0F, 0.0F, 80, 45, 80, 45);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            int fillColor = si.disabled ? PWPTheme.Colors.SURFACE_DIM
                : (isBlue ? PWPTheme.Colors.TEAM_BLUE_DARK : PWPTheme.Colors.TEAM_RED_DARK);
            gui.fill(flagX, flagY, flagX + 80, flagY + 45, fillColor);
        }

        int playerCount = isBlue ? ClientData.BLUE_PLAYER_COUNT : ClientData.RED_PLAYER_COUNT;

        String joinText;
        int joinColor;
        if (si.disabled) {
            joinText = "TOO MANY PLAYERS";
            joinColor = PWPTheme.Colors.TEXT_DIM;
        } else if (si.hovered) {
            joinText = "JOIN";
            joinColor = PWPTheme.Colors.TEXT_PRIMARY;
        } else {
            joinText = "JOIN";
            joinColor = PWPTheme.Colors.TEXT_ACCENT;
        }
        gui.drawString(PWPTheme.Fonts.display(),
            Component.literal(joinText).withStyle(ChatFormatting.GOLD), cx - PWPTheme.Fonts.display().width(joinText) / 2, si.y + 78, joinColor, false);

        String playersText = playerCount + " players";
        gui.drawString(PWPTheme.Fonts.display(),
            Component.literal(playersText)
                .withStyle(si.disabled ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY),
            cx - PWPTheme.Fonts.display().width(playersText) / 2, si.y + 92, si.disabled ? PWPTheme.Colors.TEXT_DIM : PWPTheme.Colors.TEXT_SECONDARY, false);
    }

    private static void onTeamSelected(int index) {
        String team = index == 0 ? "BLUE" : "RED";
        ClientData.teamSelectSent = true;
        ClientData.teamSelectSentTime = System.currentTimeMillis();
        PacketHandler.INSTANCE.sendToServer(new PacketTeamSelect(team));
        Minecraft.getInstance().setScreen(null);
    }

    private static String formatFactionName(String faction, String customName) {
        if (customName != null && !customName.isEmpty()) return customName;
        if (faction != null && !faction.equals("none") && !faction.equals("bluefor") && !faction.equals("redfor")) {
            return faction.replace("_", " ").toUpperCase();
        }
        return faction != null ? faction.toUpperCase() : "";
    }

    private static ResourceLocation getFlagTexture(String faction) {
        if (faction != null && !faction.equals("none")) {
            switch (faction.toLowerCase()) {
                case "ukraine": return FLAG_UKRAINE;
                case "russia": return FLAG_RUSSIA;
                case "usa": return FLAG_USA;
                case "bluefor": return FLAG_BLUEFOR;
                case "redfor": return FLAG_REDFOR;
                case "nato": return FLAG_NATO;
                case "insurgency": return FLAG_INSURGENCY;
                case "pmc": return FLAG_PMC;
                default: return null;
            }
        }
        return null;
    }
}
