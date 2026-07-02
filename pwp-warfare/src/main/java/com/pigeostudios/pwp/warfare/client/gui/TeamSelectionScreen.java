package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketTeamSelect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// Экран выбора команды (Синие vs Красные)
// Отображает флаги фракций и позволяет присоединиться к одной из сторон
public class TeamSelectionScreen extends Screen {
   private static final ResourceLocation FLAG_UKRAINE = new ResourceLocation("pwpwarfare", "textures/gui/flags/ukraine.png");
   private static final ResourceLocation FLAG_RUSSIA = new ResourceLocation("pwpwarfare", "textures/gui/flags/russia.png");
   private static final ResourceLocation FLAG_USA = new ResourceLocation("pwpwarfare", "textures/gui/flags/usa.png");
   private static final ResourceLocation FLAG_NATO = new ResourceLocation("pwpwarfare", "textures/gui/flags/nato.png");
   private static final ResourceLocation FLAG_BLUEFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
   private static final ResourceLocation FLAG_REDFOR = new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
   private static final ResourceLocation FLAG_INSURGENCY = new ResourceLocation("pwpwarfare", "textures/gui/flags/insurgency.png");
   private static final ResourceLocation FLAG_PMC = new ResourceLocation("pwpwarfare", "textures/gui/flags/pmc.png");

   public TeamSelectionScreen() {
      super(Component.translatable("gui.pwpwarfare.team_select.title"));
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
      gui.fill(0, 0, this.width, this.height, Integer.MIN_VALUE);
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int flagWidth = 64;
      int flagHeight = 36;
      int offset = 60;
      int blueX = centerX - offset - flagWidth;
      int blueY = centerY - flagHeight / 2;
      boolean isHoveringBlue = mouseX >= blueX && mouseX <= blueX + flagWidth && mouseY >= blueY && mouseY <= blueY + flagHeight + 20;
      ResourceLocation blueFlag = this.getFlagTexture(ClientData.BLUE_FACTION);
      if (blueFlag != null) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         gui.blit(blueFlag, blueX, blueY, 0.0F, 0.0F, flagWidth, flagHeight, flagWidth, flagHeight);
      } else {
         gui.fill(blueX, blueY, blueX + flagWidth, blueY + flagHeight, -16777046);
      }

      if (isHoveringBlue) {
         gui.renderOutline(blueX - 1, blueY - 1, flagWidth + 2, flagHeight + 2, -1);
      }

      String blueTeamName = ClientData.customBlueName;
      if (ClientData.BLUE_FACTION != null && !ClientData.BLUE_FACTION.equals("none") && !ClientData.BLUE_FACTION.equals("bluefor")) {
         blueTeamName = ClientData.BLUE_FACTION.replace("_", " ").toUpperCase();
      }

      int blueJoinColor = isHoveringBlue ? -1 : -22016;
      gui.drawCenteredString(this.font, Component.translatable("gui.pwpwarfare.team_select.join").withStyle(ChatFormatting.GOLD), blueX + flagWidth / 2, blueY + flagHeight + 10, blueJoinColor);
      gui.drawCenteredString(this.font, Component.literal(blueTeamName).withStyle(ChatFormatting.BLUE), blueX + flagWidth / 2, blueY - 15, 16777215);
      int redX = centerX + offset;
      int redY = centerY - flagHeight / 2;
      boolean isHoveringRed = mouseX >= redX && mouseX <= redX + flagWidth && mouseY >= redY && mouseY <= redY + flagHeight + 20;
      ResourceLocation redFlag = this.getFlagTexture(ClientData.RED_FACTION);
      if (redFlag != null) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         gui.blit(redFlag, redX, redY, 0.0F, 0.0F, flagWidth, flagHeight, flagWidth, flagHeight);
      } else {
         gui.fill(redX, redY, redX + flagWidth, redY + flagHeight, -5636096);
      }

      if (isHoveringRed) {
         gui.renderOutline(redX - 1, redY - 1, flagWidth + 2, flagHeight + 2, -1);
      }

      String redTeamName = ClientData.customRedName;
      if (ClientData.RED_FACTION != null && !ClientData.RED_FACTION.equals("none") && !ClientData.RED_FACTION.equals("redfor")) {
         redTeamName = ClientData.RED_FACTION.replace("_", " ").toUpperCase();
      }

      int redJoinColor = isHoveringRed ? -1 : -22016;
      gui.drawCenteredString(this.font, Component.translatable("gui.pwpwarfare.team_select.join").withStyle(ChatFormatting.GOLD), redX + flagWidth / 2, redY + flagHeight + 10, redJoinColor);
      gui.drawCenteredString(this.font, Component.literal(redTeamName).withStyle(ChatFormatting.RED), redX + flagWidth / 2, redY - 15, 16777215);
      super.render(gui, mouseX, mouseY, partialTick);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int centerX = this.width / 2;
         int centerY = this.height / 2;
         int flagWidth = 64;
         int flagHeight = 36;
         int offset = 60;
         int blueX = centerX - offset - flagWidth;
         int blueY = centerY - flagHeight / 2;
         if (mouseX >= blueX && mouseX <= blueX + flagWidth && mouseY >= blueY && mouseY <= blueY + flagHeight + 20) {
            PacketHandler.INSTANCE.sendToServer(new PacketTeamSelect("BLUE"));
            this.onClose();
            return true;
         }

         int redX = centerX + offset;
         int redY = centerY - flagHeight / 2;
         if (mouseX >= redX && mouseX <= redX + flagWidth && mouseY >= redY && mouseY <= redY + flagHeight + 20) {
            PacketHandler.INSTANCE.sendToServer(new PacketTeamSelect("RED"));
            this.onClose();
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private ResourceLocation getFlagTexture(String faction) {
      if (faction != null && !faction.equals("none")) {
         switch (faction.toLowerCase()) {
            case "ukraine":
               return FLAG_UKRAINE;
            case "russia":
               return FLAG_RUSSIA;
            case "usa":
               return FLAG_USA;
            case "bluefor":
               return new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
            case "redfor":
               return new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
            case "nato":
               return FLAG_NATO;
            case "insurgency":
               return FLAG_INSURGENCY;
            case "pmc":
               return FLAG_PMC;
            default:
               return null;
         }
      } else {
         return null;
      }
   }
}
