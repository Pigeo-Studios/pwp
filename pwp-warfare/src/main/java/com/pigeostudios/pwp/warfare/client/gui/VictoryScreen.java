package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class VictoryScreen extends Screen {

    private final String winnerName;
    private final String winnerFaction;
    private final String subText;
    private final boolean isBlueWinner;
    private final long openTime;

    private final int matchKills, matchDeaths;
    private final int matchVehicleKills, matchVehiclesDestroyed, matchAirVehiclesDestroyed;
    private final int matchCaptures, matchRevives, matchHeadshots, matchScore;
    private final int matchDurationSec;

    private PWPButton continueButton;

    public VictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner,
                         int matchKills, int matchDeaths,
                         int matchVehicleKills, int matchVehiclesDestroyed,
                         int matchAirVehiclesDestroyed, int matchCaptures,
                         int matchRevives, int matchHeadshots, int matchScore,
                         int matchDurationSec) {
        super(Component.translatable("gui.pwpwarfare.victory.title"));
        this.winnerName = winnerName;
        this.winnerFaction = winnerFaction;
        this.subText = subText;
        this.isBlueWinner = isBlueWinner;
        this.matchKills = matchKills;
        this.matchDeaths = matchDeaths;
        this.matchVehicleKills = matchVehicleKills;
        this.matchVehiclesDestroyed = matchVehiclesDestroyed;
        this.matchAirVehiclesDestroyed = matchAirVehiclesDestroyed;
        this.matchCaptures = matchCaptures;
        this.matchRevives = matchRevives;
        this.matchHeadshots = matchHeadshots;
        this.matchScore = matchScore;
        this.matchDurationSec = matchDurationSec;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        continueButton = addRenderableWidget(new PWPButton(cx - 70, height / 2 + 90, 140, 24,
            Component.literal("РџСЂРѕРґРѕР»Р¶РёС‚СЊ"),
            b -> onClose(), PWPButton.Style.ACCENT));
        continueButton.setAnimAlpha(0f);
        continueButton.active = false;
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        long elapsed = System.currentTimeMillis() - openTime;
        float bgAlpha = Mth.clamp((float) elapsed / 400f, 0f, 1f);
        float contentAlpha = Mth.clamp((float) (elapsed - 400L) / 800f, 0f, 1f);

        int topBg = (int) (bgAlpha * 100) << 24;
        int bottomBg = (int) (bgAlpha * 140) << 24;
        gui.fillGradient(0, 0, width, height, topBg, bottomBg);

        if (contentAlpha > 0.01f) {
            int cx = width / 2;
            int cy = height / 2;
            int a = (int) (contentAlpha * 255);
            int frameColor = a << 24 | 0xFFFFFF;

            ResourceLocation flagTex = getFlagTexture(winnerFaction);
            int flagW = 80, flagH = 45;
            int flagX = cx - flagW / 2;
            int flagY = cy - 100;

            if (flagTex != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, contentAlpha);
                RenderSystem.enableBlend();
                gui.blit(flagTex, flagX, flagY, 0, 0, flagW, flagH, flagW, flagH);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            } else {
                int fallback = isBlueWinner ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.TEAM_RED;
                gui.fill(flagX, flagY, flagX + flagW, flagY + flagH, a << 24 | fallback);
            }
            gui.renderOutline(flagX - 1, flagY - 1, flagW + 2, flagH + 2, frameColor);

            gui.pose().pushPose();
            gui.pose().translate(cx, cy - 42, 0);
            gui.pose().scale(1.5f, 1.5f, 1f);
            gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal(winnerName + " вЂ” РїРѕР±РµРґР°!"), 0, 0, a << 24 | 0xFFFFFF);
            gui.pose().popPose();

            gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal(subText), cx, cy - 22, a << 24 | 0xAAAAAA);

            int statsY = cy + 5;
            int statsX = cx - 160;
            int statsW = 320;
            int panelH = 80;

            gui.fill(statsX, statsY, statsX + statsW, statsY + panelH, a << 24 | 0x88181C24);
            gui.fill(statsX, statsY, statsX + statsW, statsY + 1, a << 24 | 0xFF1E222A);
            gui.fill(statsX, statsY + panelH - 1, statsX + statsW, statsY + panelH, a << 24 | 0xFF1E222A);
            gui.fill(statsX, statsY, statsX + 1, statsY + panelH, a << 24 | 0xFF1E222A);
            gui.fill(statsX + statsW - 1, statsY, statsX + statsW, statsY + panelH, a << 24 | 0xFF1E222A);

            int accent = a << 24 | PWPTheme.Colors.ACCENT;
            gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal("\u00a7lРЎС‚Р°С‚РёСЃС‚РёРєР°"), cx, statsY + 4, accent);

            int rowY = statsY + 16;
            int rowH = 13;
            int cw = 106;
            int labelCol = a << 24 | 0xFF7A7D84;
            int textCol = a << 24 | 0xFFC8CBCE;

            drawStat(gui, "РЈР±РёР№СЃС‚РІ", String.valueOf(matchKills), statsX + 8, rowY, labelCol, accent);
            drawStat(gui, "РЎРјРµСЂС‚РµР№", String.valueOf(matchDeaths), statsX + 8 + cw, rowY, labelCol, textCol);
            drawStat(gui, "Р РµР°РЅРёРјР°С†РёР№", String.valueOf(matchRevives), statsX + 8 + cw * 2, rowY, labelCol, textCol);
            rowY += rowH;

            String kd = matchDeaths == 0 ? String.valueOf(matchKills) : String.format("%.2f", (double) matchKills / matchDeaths);
            drawStat(gui, "K/D", kd, statsX + 8, rowY, labelCol, accent);
            drawStat(gui, "РЎС‡С‘С‚", String.format("%,d", matchScore), statsX + 8 + cw, rowY, labelCol, textCol);
            drawStat(gui, "Р’СЂРµРјСЏ", formatDuration(matchDurationSec), statsX + 8 + cw * 2, rowY, labelCol, textCol);
            rowY += rowH;

            drawStat(gui, "РўРµС…РЅРёРєР°", String.valueOf(matchVehicleKills), statsX + 8, rowY, labelCol, textCol);
            drawStat(gui, "РЈРЅРёС‡С‚РѕР¶РµРЅРѕ", String.valueOf(matchVehiclesDestroyed), statsX + 8 + cw, rowY, labelCol, textCol);
            drawStat(gui, "Р’ РІРѕР·РґСѓС…Рµ", String.valueOf(matchAirVehiclesDestroyed), statsX + 8 + cw * 2, rowY, labelCol, textCol);
            rowY += rowH;

            drawStat(gui, "Р—Р°С…РІР°С‚РѕРІ", String.valueOf(matchCaptures), statsX + 8, rowY, labelCol, textCol);
            drawStat(gui, "Р’ РіРѕР»РѕРІСѓ", String.valueOf(matchHeadshots), statsX + 8 + cw, rowY, labelCol, textCol);
        }

        continueButton.setAnimAlpha(contentAlpha);
        if (contentAlpha >= 1f && !continueButton.active) continueButton.active = true;

        super.render(gui, mx, my, pt);
    }

    private void drawStat(GuiGraphics gui, String label, String value, int x, int y, int labelColor, int valueColor) {
        gui.drawString(PWPTheme.Fonts.display(), Component.literal(label + ":"), x, y, labelColor);
        gui.drawString(PWPTheme.Fonts.display(), Component.literal(value), x + 60, y, valueColor);
    }

    private static String formatDuration(int secs) {
        if (secs < 60) return secs + "s";
        int m = secs / 60;
        int s = secs % 60;
        if (m >= 60) return (m / 60) + "h " + (m % 60) + "m";
        return m + "m " + s + "s";
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equals("none")) return null;
        return switch (faction.toLowerCase()) {
            case "ukraine" -> new ResourceLocation("pwpwarfare", "textures/gui/flags/ukraine.png");
            case "russia" -> new ResourceLocation("pwpwarfare", "textures/gui/flags/russia.png");
            case "usa" -> new ResourceLocation("pwpwarfare", "textures/gui/flags/usa.png");
            case "nato" -> new ResourceLocation("pwpwarfare", "textures/gui/flags/nato.png");
            case "bluefor" -> new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
            case "redfor" -> new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
            case "insurgency" -> new ResourceLocation("pwpwarfare", "textures/gui/flags/insurgency.png");
            case "pmc" -> new ResourceLocation("pwpwarfare", "textures/gui/flags/pmc.png");
            default -> null;
        };
    }
}
