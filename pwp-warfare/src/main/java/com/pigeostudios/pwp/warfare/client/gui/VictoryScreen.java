package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
    private final int matchDamageDealt, matchHealingDone, matchSuppliesDelivered;

    private PWPButton continueButton;

    public VictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner,
                         int matchKills, int matchDeaths,
                         int matchVehicleKills, int matchVehiclesDestroyed,
                         int matchAirVehiclesDestroyed, int matchCaptures,
                         int matchRevives, int matchHeadshots, int matchScore,
                         int matchDurationSec, int matchDamageDealt, int matchHealingDone, int matchSuppliesDelivered) {
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
        this.matchDamageDealt = matchDamageDealt;
        this.matchHealingDone = matchHealingDone;
        this.matchSuppliesDelivered = matchSuppliesDelivered;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        continueButton = addRenderableWidget(new PWPButton(cx - 70, height / 2 + 145, 140, 24,
            Component.literal("Продолжить"),
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
            String winText = winnerName + " — победа!";
            gui.drawString(PWPTheme.Fonts.display(), Component.literal(winText), -PWPTheme.Fonts.display().width(winText) / 2, 0, a << 24 | 0xFFFFFF, false);
            gui.pose().popPose();

            gui.drawString(PWPTheme.Fonts.display(), Component.literal(subText), cx - PWPTheme.Fonts.display().width(subText) / 2, cy - 22, a << 24 | 0xAAAAAA, false);

            int statsX = cx - 240;
            int statsW = 480;
            int panelH = 124;
            int statsY = cy + 5;

            gui.fill(statsX, statsY, statsX + statsW, statsY + panelH, a << 24 | 0x88181C24);
            gui.fill(statsX, statsY, statsX + statsW, statsY + 1, a << 24 | 0xFF1E222A);
            gui.fill(statsX, statsY + panelH - 1, statsX + statsW, statsY + panelH, a << 24 | 0xFF1E222A);
            gui.fill(statsX, statsY, statsX + 1, statsY + panelH, a << 24 | 0xFF1E222A);
            gui.fill(statsX + statsW - 1, statsY, statsX + statsW, statsY + panelH, a << 24 | 0xFF1E222A);

            Font font = PWPTheme.Fonts.display();
            int accent = a << 24 | PWPTheme.Colors.TEXT_ACCENT;
            int labelCol = a << 24 | PWPTheme.Colors.TEXT_SECONDARY;
            int textCol = a << 24 | PWPTheme.Colors.TEXT_PRIMARY;
            int dividerCol = a << 24 | PWPTheme.Colors.BORDER;

            int padX = 14;
            int contentW = statsW - padX * 2;
            int contentTop = statsY + 10;

            String statsTitle = "СТАТИСТИКА БОЯ";
            gui.drawString(font, statsTitle, cx - font.width(statsTitle) / 2, contentTop, accent, false);
            gui.fill(statsX + padX, contentTop + 15, statsX + statsW - padX, contentTop + 16, dividerCol);

            int headerY = contentTop + 22;
            int colW = contentW / 3;
            String[] groupHeaders = {"БОЙ", "ТЕХНИКА", "ВКЛАД"};
            for (int c = 0; c < groupHeaders.length; c++) {
                gui.drawString(font, groupHeaders[c], statsX + padX + c * colW + 2, headerY, labelCol, false);
            }
            gui.fill(statsX + padX, headerY + 15, statsX + statsW - padX, headerY + 16, dividerCol);

            int rowY0 = headerY + 20;
            int rowH = 16;
            String kd = matchDeaths == 0 ? String.valueOf(matchKills)
                    : String.format("%.2f", (double) matchKills / matchDeaths);

            drawStatCell(gui, font, "Убийств", formatNum(String.valueOf(matchKills)), statsX + padX, colW, rowY0, labelCol, accent);
            drawStatCell(gui, font, "Техникой", formatNum(String.valueOf(matchVehicleKills)), statsX + padX + colW, colW, rowY0, labelCol, textCol);
            drawStatCell(gui, font, "Захватов", formatNum(String.valueOf(matchCaptures)), statsX + padX + colW * 2, colW, rowY0, labelCol, textCol);
            gui.fill(statsX + padX, rowY0 + rowH, statsX + statsW - padX, rowY0 + rowH + 1, dividerCol);

            drawStatCell(gui, font, "Смертей", formatNum(String.valueOf(matchDeaths)), statsX + padX, colW, rowY0 + rowH, labelCol, textCol);
            drawStatCell(gui, font, "Уничтожено", formatNum(String.valueOf(matchVehiclesDestroyed)), statsX + padX + colW, colW, rowY0 + rowH, labelCol, textCol);
            drawStatCell(gui, font, "Реанимаций", formatNum(String.valueOf(matchRevives)), statsX + padX + colW * 2, colW, rowY0 + rowH, labelCol, textCol);
            gui.fill(statsX + padX, rowY0 + rowH * 2, statsX + statsW - padX, rowY0 + rowH * 2 + 1, dividerCol);

            drawStatCell(gui, font, "K/D", kd, statsX + padX, colW, rowY0 + rowH * 2, labelCol, accent);
            drawStatCell(gui, font, "В воздухе", formatNum(String.valueOf(matchAirVehiclesDestroyed)), statsX + padX + colW, colW, rowY0 + rowH * 2, labelCol, textCol);
            drawStatCell(gui, font, "Припасов", formatNum(String.valueOf(matchSuppliesDelivered)), statsX + padX + colW * 2, colW, rowY0 + rowH * 2, labelCol, textCol);

            int footerTop = rowY0 + rowH * 3 + 2;
            gui.fill(statsX + padX, footerTop, statsX + statsW - padX, footerTop + 1, dividerCol);
            int fY = footerTop + 6;
            int footW = contentW / 4;
            String[] footLabels = {"УРОН", "ЛЕЧЕНИЕ", "СЧЁТ", "ВРЕМЯ"};
            String[] footVals = {
                formatNum(String.valueOf(matchDamageDealt)),
                formatNum(String.valueOf(matchHealingDone)),
                String.format("%,d", matchScore),
                formatDuration(matchDurationSec)
            };
            for (int i = 0; i < footLabels.length; i++) {
                drawStatCell(gui, font, footLabels[i], footVals[i], statsX + padX + i * footW, footW, fY, labelCol, accent);
            }
        }

        continueButton.setAnimAlpha(contentAlpha);
        if (contentAlpha >= 1f && !continueButton.active) continueButton.active = true;

        super.render(gui, mx, my, pt);
    }

    private void drawStatCell(GuiGraphics gui, Font font, String label, String value, int x, int colW,
                              int y, int labelColor, int valueColor) {
        int pad = 8;
        int gap = 6;
        int valueW = font.width(value);
        int valueX = x + colW - pad - valueW;
        int labelMax = Math.max(8, valueX - gap - x - pad);
        String showLabel = label;
        if (font.width(showLabel) > labelMax) {
            showLabel = font.plainSubstrByWidth(label, Math.max(8, labelMax - 4)) + "...";
        }
        gui.drawString(font, showLabel, x + pad, y, labelColor, false);
        gui.drawString(font, value, valueX, y, valueColor, false);
    }

    private static String formatNum(String raw) {
        try {
            double d = Double.parseDouble(raw);
            if (d >= 1000000) return String.format("%.1fM", d / 1000000);
            if (d >= 1000) return String.format("%.1fK", d / 1000);
            if (d == (long) d) return String.valueOf((long) d);
            return raw;
        } catch (NumberFormatException e) {
            return raw;
        }
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
