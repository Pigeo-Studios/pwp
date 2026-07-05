package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

   private final int matchKills, matchDeaths, matchAssists;
   private final int matchVehicleKills, matchVehiclesDestroyed, matchAirVehiclesDestroyed;
   private final int matchCaptures, matchRevives, matchHeadshots, matchScore;
   private final int matchDurationSec;

   private SquadButton continueButton;

   private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("pwpwarfare", "textures/gui/flags/ukraine.png");
   private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("pwpwarfare", "textures/gui/flags/russia.png");
   private static final ResourceLocation FLAG_USA = new ResourceLocation("pwpwarfare", "textures/gui/flags/usa.png");
   private static final ResourceLocation FLAG_NATO = new ResourceLocation("pwpwarfare", "textures/gui/flags/nato.png");
   private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
   private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
   private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("pwpwarfare", "textures/gui/flags/insurgency.png");
   private static final ResourceLocation FLAG_PMC = new ResourceLocation("pwpwarfare", "textures/gui/flags/pmc.png");

   public VictoryScreen(String winnerName, String winnerFaction, String subText, boolean isBlueWinner,
                        int matchKills, int matchDeaths, int matchAssists,
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
      this.matchAssists = matchAssists;
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
      int cx = this.width / 2;
      int cy = this.height / 2;
      this.continueButton = new SquadButton(cx - 70, cy + 80, 140, 24,
         Component.translatable("gui.pwpwarfare.victory.continue"), b -> this.onClose());
      this.continueButton.active = false;
      this.addRenderableWidget(this.continueButton);
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      long elapsed = System.currentTimeMillis() - this.openTime;
      float bgAlpha = Mth.clamp((float)elapsed / 1500.0F, 0.0F, 1.0F);
      float contentAlpha = Mth.clamp((float)(elapsed - 1500L) / 2000.0F, 0.0F, 1.0F);
      int topAlpha = (int)(bgAlpha * 100.0F);
      int bottomAlpha = (int)(bgAlpha * 140.0F);
      int topBg = topAlpha << 24 | 0;
      int bottomBg = bottomAlpha << 24 | 0;
      gui.fillGradient(0, 0, this.width, this.height, topBg, bottomBg);

      if (contentAlpha > 0.01F) {
         int cx = this.width / 2;
         int cy = this.height / 2;
         int alphaInt = (int)(contentAlpha * 255.0F);
         int frameColor = alphaInt << 24 | 16777215;

         ResourceLocation flagTex = this.getFlagTexture(this.winnerFaction);
         int flagW = 64;
         int flagH = 36;
         int flagX = cx - flagW / 2;
         int flagY = cy - 95;

         if (flagTex != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, contentAlpha);
            RenderSystem.enableBlend();
            gui.blit(flagTex, flagX, flagY, 0.0F, 0.0F, flagW, flagH, flagW, flagH);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            gui.renderOutline(flagX - 1, flagY - 1, flagW + 2, flagH + 2, frameColor);
         } else {
            int fallbackBase = this.isBlueWinner ? 3368652 : 13382451;
            gui.fill(flagX, flagY, flagX + flagW, flagY + flagH, alphaInt << 24 | fallbackBase);
            gui.renderOutline(flagX - 1, flagY - 1, flagW + 2, flagH + 2, frameColor);
         }

         RenderSystem.enableBlend();
         int titleColor = alphaInt << 24 | 16777215;
         gui.pose().pushPose();
         gui.pose().translate(cx, cy - 40, 0.0F);
         gui.pose().scale(1.5F, 1.5F, 1.0F);
         gui.drawCenteredString(this.font, this.winnerName + " WINS!", 0, 0, titleColor);
         gui.pose().popPose();

         int subColor = alphaInt << 24 | 11184810;
         gui.drawCenteredString(this.font, this.subText, cx, cy - 20, subColor);

         // Match stats panel
         int statsY = cy + 5;
         int statsX = cx - 150;
         int statsW = 300;
         int panelH = 75;
         int pCol = alphaInt << 24 | 0x88181C24;
         int pBorder = alphaInt << 24 | 0xFF1E222A;
         int pText = alphaInt << 24 | 0xFFC8CBCE;
         int pLabel = alphaInt << 24 | 0xFF7A7D84;
         int pAccent = alphaInt << 24 | 0xFFC8812A;

         gui.fill(statsX, statsY, statsX + statsW, statsY + panelH, pCol);
         gui.fill(statsX, statsY, statsX + statsW, statsY + 1, pBorder);
         gui.fill(statsX, statsY + panelH - 1, statsX + statsW, statsY + panelH, pBorder);
         gui.fill(statsX, statsY, statsX + 1, statsY + panelH, pBorder);
         gui.fill(statsX + statsW - 1, statsY, statsX + statsW, statsY + panelH, pBorder);

         gui.drawCenteredString(this.font, Component.literal("\u00a7lMATCH STATS").withStyle(s -> s.withColor(pAccent)),
            cx, statsY + 3, pAccent);

         int rowY = statsY + 16;
         int rowH = 13;
         int cw = 100;
         int labelW = 50;
         int valW = 46;

         drawStat(gui, "\u2694 Kills", String.valueOf(matchKills), statsX + 6, rowY, labelW, valW, pLabel, pAccent);
         drawStat(gui, "\u2620 Deaths", String.valueOf(matchDeaths), statsX + 6 + cw, rowY, labelW, valW, pLabel, pText);
         drawStat(gui, "Revives", String.valueOf(matchRevives), statsX + 6 + cw * 2, rowY, labelW, valW, pLabel, pText);
         rowY += rowH;

         drawStat(gui, "K/D", matchDeaths == 0 ? String.valueOf(matchKills) : String.format("%.2f", (double) matchKills / matchDeaths),
            statsX + 6, rowY, labelW, valW, pLabel, pAccent);
         drawStat(gui, "Score", String.format("%,d", matchScore), statsX + 6 + cw, rowY, labelW, valW, pLabel, pText);
         drawStat(gui, "Time", formatDuration(matchDurationSec), statsX + 6 + cw * 2, rowY, labelW, valW, pLabel, pText);
         rowY += rowH;

         drawStat(gui, "V.Kills", String.valueOf(matchVehicleKills), statsX + 6, rowY, labelW, valW, pLabel, pText);
         drawStat(gui, "V.Destr.", String.valueOf(matchVehiclesDestroyed), statsX + 6 + cw, rowY, labelW, valW, pLabel, pText);
         drawStat(gui, "Air D.", String.valueOf(matchAirVehiclesDestroyed), statsX + 6 + cw * 2, rowY, labelW, valW, pLabel, pText);
         rowY += rowH;

         drawStat(gui, "Captures", String.valueOf(matchCaptures), statsX + 6, rowY, labelW, valW, pLabel, pText);
         drawStat(gui, "Headshots", String.valueOf(matchHeadshots), statsX + 6 + cw, rowY, labelW, valW, pLabel, pText);
         drawStat(gui, "", "", statsX + 6 + cw * 2, rowY, labelW, valW, pLabel, pText);

         RenderSystem.disableBlend();
      }

      this.continueButton.currentAlpha = contentAlpha;
      if (contentAlpha >= 1.0F && !this.continueButton.active) {
         this.continueButton.active = true;
      }

      super.render(gui, mouseX, mouseY, partialTick);
   }

   private void drawStat(GuiGraphics gui, String label, String value, int x, int y, int labelW, int valW, int labelColor, int valueColor) {
      if (!label.isEmpty()) {
         gui.drawString(this.font, Component.literal(label + ":"), x, y, labelColor);
      }
      if (!value.isEmpty()) {
         gui.drawString(this.font, Component.literal(value), x + labelW + 4, y, valueColor);
      }
   }

   private String formatDuration(int secs) {
      if (secs < 60) return secs + "s";
      int m = secs / 60;
      int s = secs % 60;
      if (m >= 60) return (m / 60) + "h " + (m % 60) + "m";
      return m + "m " + s + "s";
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private ResourceLocation getFlagTexture(String faction) {
      if (faction != null && !faction.equals("none")) {
         switch (faction.toLowerCase()) {
            case "ukraine": return FLAG_UKRAINE;
            case "russia": return FLAG_RUSSIA;
            case "usa": return FLAG_USA;
            case "nato": return FLAG_NATO;
            case "bluefor": return FLAG_BLUEFOR;
            case "redfor": return FLAG_REDFOR;
            case "insurgency": return FLAG_INSURGENCY;
            case "pmc": return FLAG_PMC;
            default: return null;
         }
      }
      return null;
   }

   private static class SquadButton extends Button {
      public float currentAlpha = 0.0F;

      public SquadButton(int x, int y, int width, int height, Component message, OnPress onPress) {
         super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
      }

      @Override
      protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
         if (this.visible && !(this.currentAlpha <= 0.02F)) {
            int a = (int)(this.currentAlpha * 255.0F);
            int bgCol = a << 24 | 1118481;
            int borderBase = this.isHovered() ? 16777215 : 10066329;
            if (!this.active) borderBase = 4473924;
            int borderCol = a << 24 | borderBase;
            RenderSystem.enableBlend();
            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgCol);
            gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderCol);
            int textBase = this.active ? 16777215 : 7829367;
            int textCol = a << 24 | textBase;
            gui.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
               this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textCol);
            if (this.active && this.isHovered()) {
               gui.fill(this.getX(), this.getY() + this.height - 2, this.getX() + 2, this.getY() + this.height,
                  a << 24 | 16777215);
            }
            RenderSystem.disableBlend();
         }
      }
   }
}