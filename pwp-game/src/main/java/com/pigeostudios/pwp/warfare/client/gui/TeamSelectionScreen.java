package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketTeamSelect;
import com.pwp.coreclient.gui.screens.PWPSelectionScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TeamSelectionScreen extends PWPSelectionScreen {

    private static final ResourceLocation FLAG_UKRAINE =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/ukraine.png");
    private static final ResourceLocation FLAG_RUSSIA =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/russia.png");
    private static final ResourceLocation FLAG_USA = new ResourceLocation("pwpwarfare", "textures/gui/flags/usa.png");
    private static final ResourceLocation FLAG_NATO = new ResourceLocation("pwpwarfare", "textures/gui/flags/nato.png");
    private static final ResourceLocation FLAG_BLUEFOR =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
    private static final ResourceLocation FLAG_REDFOR =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
    private static final ResourceLocation FLAG_INSURGENCY =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/insurgency.png");
    private static final ResourceLocation FLAG_PMC = new ResourceLocation("pwpwarfare", "textures/gui/flags/pmc.png");

    /** Тест-стенд: клики не шлют пакетов. */
    private boolean preview;

    public TeamSelectionScreen preview() {
        this.preview = true;
        return this;
    }

    public TeamSelectionScreen() {
        super(
                Component.translatable("gui.pwpwarfare.team_select.title").getString(),
                null,
                List.of("BLUE", "RED", "AUTO"),
                TeamSelectionScreen::renderTeamCard,
                TeamSelectionScreen::onTeamSelected,
                -1,
                130,
                140,
                16,
                TeamSelectionScreen::isTeamDisabled,
                true);
    }

    /** Ошибка последнего выбора (S2C TeamResult) — показывается в самом экране, а не в чате. */
    private Component error;

    public void showError(Component c) {
        this.error = c;
    }

    /** Без команды экран не закрывается (Esc) — иначе тупик до следующего sync-пакета. */
    @Override
    public boolean shouldCloseOnEsc() {
        return com.pigeostudios.pwp.warfare.client.ClientStaging.hasTeam();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        var font = PWPTheme.Fonts.display();
        Component line = error != null
                ? error
                : Component.translatable(
                        "gui.pwpwarfare.team_select.hint",
                        com.pigeostudios.pwp.warfare.client.ModKeyBindings.OPEN_SQUAD_MENU_KEY
                                .getTranslatedKeyMessage());
        int color = error != null ? PWPTheme.Text.DANGER : PWPTheme.Text.MUTED;
        int y = height - PWPTheme.Spacing.XXL - font.lineHeight;
        gui.drawString(font, line, (width - font.width(line)) / 2, y, color, true);
    }

    private static boolean isTeamDisabled(int index) {
        if (index == 2) return false; // АВТО — всегда в меньшую
        return index == 0
                ? ClientData.BLUE_PLAYER_COUNT >= ClientData.RED_PLAYER_COUNT + 2
                : ClientData.RED_PLAYER_COUNT >= ClientData.BLUE_PLAYER_COUNT + 2;
    }

    private static void renderTeamCard(GuiGraphics gui, SelectionItem si) {
        if (si.index == 2) {
            renderAutoCard(gui, si);
            return;
        }
        boolean isBlue = si.index == 0;
        int cx = si.x + si.w / 2;

        String teamName = isBlue
                ? formatFactionName(ClientData.BLUE_FACTION, ClientData.customBlueName)
                : formatFactionName(ClientData.RED_FACTION, ClientData.customRedName);
        int teamColor = isBlue ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.TEAM_RED;

        gui.drawString(
                PWPTheme.Fonts.display(),
                Component.literal(teamName).withStyle(isBlue ? ChatFormatting.BLUE : ChatFormatting.RED),
                cx - PWPTheme.Fonts.display().width(teamName) / 2,
                si.y + 8,
                teamColor,
                false);

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
            int fillColor = si.disabled
                    ? PWPTheme.Colors.SURFACE_DIM
                    : (isBlue ? PWPTheme.Colors.TEAM_BLUE_DARK : PWPTheme.Colors.TEAM_RED_DARK);
            gui.fill(flagX, flagY, flagX + 80, flagY + 45, fillColor);
        }

        int playerCount = isBlue ? ClientData.BLUE_PLAYER_COUNT : ClientData.RED_PLAYER_COUNT;

        String joinText;
        int joinColor;
        if (si.disabled) {
            joinText = Component.translatable("gui.pwpwarfare.team_select.too_many")
                    .getString();
            joinColor = PWPTheme.Colors.TEXT_DIM;
        } else if (si.hovered) {
            joinText = Component.translatable("gui.pwpwarfare.team_select.join").getString();
            joinColor = PWPTheme.Colors.TEXT_PRIMARY;
        } else {
            joinText = Component.translatable("gui.pwpwarfare.team_select.join").getString();
            joinColor = PWPTheme.Colors.TEXT_ACCENT;
        }
        gui.drawString(
                PWPTheme.Fonts.display(),
                Component.literal(joinText).withStyle(ChatFormatting.GOLD),
                cx - PWPTheme.Fonts.display().width(joinText) / 2,
                si.y + 78,
                joinColor,
                false);

        String playersText = Component.translatable("gui.pwpwarfare.team_select.players", playerCount)
                .getString();
        gui.drawString(
                PWPTheme.Fonts.display(),
                Component.literal(playersText).withStyle(si.disabled ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY),
                cx - PWPTheme.Fonts.display().width(playersText) / 2,
                si.y + 92,
                si.disabled ? PWPTheme.Colors.TEXT_DIM : PWPTheme.Colors.TEXT_SECONDARY,
                false);
    }

    /** Карточка «АВТО»: в меньшую команду (баланс решает сервер). */
    private static void renderAutoCard(GuiGraphics gui, SelectionItem si) {
        var font = PWPTheme.Fonts.display();
        int cx = si.x + si.w / 2;
        Component t = Component.translatable("gui.pwpwarfare.team_select.auto");
        gui.drawString(font, t, cx - font.width(t) / 2, si.y + 8, PWPTheme.Colors.TEXT_ACCENT, false);
        Component d = Component.translatable("gui.pwpwarfare.team_select.auto_desc");
        int lw = si.w - 2 * PWPTheme.Spacing.SM;
        int y = si.y + 30;
        for (var line : font.split(d, lw)) {
            gui.drawString(font, line, cx - font.width(line) / 2, y, PWPTheme.Colors.TEXT_SECONDARY, false);
            y += font.lineHeight + 1;
        }
        Component j = Component.translatable("gui.pwpwarfare.team_select.join");
        gui.drawString(
                font,
                j,
                cx - font.width(j) / 2,
                si.y + 78,
                si.hovered ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_ACCENT,
                false);
    }

    /** Экран не закрывается сам: ответ сервера (TeamResult) откроет меню K или покажет ошибку здесь. */
    private static void onTeamSelected(int index) {
        String team = index == 0 ? "BLUE" : index == 1 ? "RED" : "AUTO";
        ClientData.teamSelectSent = true;
        ClientData.teamSelectSentTime = System.currentTimeMillis();
        if (Minecraft.getInstance().screen instanceof TeamSelectionScreen ts) {
            ts.error = null;
            if (ts.preview) {
                org.slf4j.LoggerFactory.getLogger(TeamSelectionScreen.class)
                        .info("[PWPUI] team_select: {} (preview — без пакета)", team);
                return;
            }
        }
        PacketHandler.INSTANCE.sendToServer(new PacketTeamSelect(team));
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
                case "ukraine":
                    return FLAG_UKRAINE;
                case "russia":
                    return FLAG_RUSSIA;
                case "usa":
                    return FLAG_USA;
                case "bluefor":
                    return FLAG_BLUEFOR;
                case "redfor":
                    return FLAG_REDFOR;
                case "nato":
                    return FLAG_NATO;
                case "insurgency":
                    return FLAG_INSURGENCY;
                case "pmc":
                    return FLAG_PMC;
                default:
                    return null;
            }
        }
        return null;
    }
}
