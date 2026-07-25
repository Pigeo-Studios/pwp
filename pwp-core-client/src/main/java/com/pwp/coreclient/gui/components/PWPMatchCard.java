package com.pwp.coreclient.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PWPMatchCard {

    public enum Status {
        PLAYING, STARTING, FULL, WAITING, FINISHED,
        CLOSED, MAINTENANCE, RECONNECT, VOTING
    }

    private PWPMatchCard() {}

    public static void render(GuiGraphics gui, int x, int y, int w, int h,
                              String title, String mode, String duration,
                              int currentPlayers, int maxPlayers,
                              String blueFaction, String redFaction,
                              int blueTickets, int redTickets,
                              Status status, ResourceLocation preview,
                              boolean isCurrent, boolean hovered) {
        int r = PWPTheme.Spacing.RADIUS_MEDIUM;
        int bg = isCurrent ? 0xFF1A180E : (hovered ? PWPTheme.Colors.SURFACE_LIGHT : PWPTheme.Colors.SURFACE);
        int border = switch (status) {
            case PLAYING -> PWPTheme.Colors.SUCCESS;
            case STARTING, VOTING -> PWPTheme.Colors.WARNING;
            case FULL, CLOSED -> PWPTheme.Colors.DANGER;
            case MAINTENANCE -> PWPTheme.Colors.INFO;
            default -> hovered ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER;
        };

        if (isCurrent) {
            RoundedRect.glow(gui, x, y, w, h, r, 2, PWPTheme.Shadows.ACCENT_GLOW_SMALL);
        }

        RoundedRect.fill(gui, x, y, w, h, r, bg);
        RoundedRect.border(gui, x, y, w, h, r, 1, border);

        var font = PWPTheme.Fonts.display();
        int imgSize = 48;
        int imgX = x + 8;
        int imgY = y + (h - imgSize) / 2;
        gui.fill(imgX, imgY, imgX + imgSize, imgY + imgSize, 0xFF000000);

        if (preview != null) {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            gui.blit(preview, imgX + 1, imgY + 1, 0, 0, imgSize - 2, imgSize - 2, imgSize - 2, imgSize - 2);
        }

        int textX = imgX + imgSize + 12;
        int textMaxW = x + w - textX - 8;
        int ly = y + 6;

        gui.drawString(font, Component.literal(title), textX, ly, 0xFFFFFF, false);
        ly += 11;

        String meta = mode + "  |  " + duration + "  |  " + currentPlayers + "/" + maxPlayers;
        gui.drawString(font, Component.literal(meta), textX, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
        ly += 11;

        String fStr = "\u00a79" + blueFaction + " \u00a77vs \u00a7c" + redFaction;
        if (font.width(fStr) > textMaxW) {
            fStr = font.plainSubstrByWidth(fStr, textMaxW - 4) + "...";
        }
        gui.drawString(font, Component.literal(fStr), textX, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
        ly += 11;

        String tStr = "\u00a79" + blueTickets + " \u00a77| \u00a7c" + redTickets;
        if (status == Status.PLAYING || status == Status.FINISHED) {
            gui.drawString(font, Component.literal(tStr), textX, ly, PWPTheme.Colors.TEXT_SECONDARY, false);
        }

        String actionText;
        int actionColor;
        switch (status) {
            case PLAYING -> { actionText = "\u25B6 \u00a7eР’РѕР№С‚Рё"; actionColor = PWPTheme.Colors.TEXT_ACCENT; }
            case STARTING -> { actionText = "\u23F3 Р—Р°РїСѓСЃРє..."; actionColor = PWPTheme.Colors.TEXT_DIM; }
            case FULL -> { actionText = "\u2716 РџРћР›РќР«Р™"; actionColor = PWPTheme.Colors.DANGER; }
            case CLOSED -> { actionText = "\u2716 Р—Р°РєСЂС‹С‚"; actionColor = PWPTheme.Colors.DANGER; }
            case MAINTENANCE -> { actionText = "\u2699 РћР±СЃР»СѓР¶РёРІР°РЅРёРµ"; actionColor = PWPTheme.Colors.INFO; }
            case RECONNECT -> { actionText = "\u21BA РџРµСЂРµРїРѕРґРєР»СЋС‡РµРЅРёРµ..."; actionColor = PWPTheme.Colors.INFO; }
            case VOTING -> { actionText = "\u2714 Р“РѕР»РѕСЃРѕРІР°РЅРёРµ"; actionColor = PWPTheme.Colors.WARNING; }
            case WAITING -> { actionText = "\u23F3 РћР¶РёРґР°РЅРёРµ..."; actionColor = PWPTheme.Colors.TEXT_DIM; }
            default -> { actionText = ""; actionColor = PWPTheme.Colors.TEXT_DIM; }
        }

        if (!actionText.isEmpty()) {
            gui.drawString(font, actionText, textX, y + h - 13, actionColor, false);
        }
    }

    public static boolean isHovered(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static int cardHeight() {
        return 68;
    }
}
