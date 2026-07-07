package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketTeamSelect;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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

       int cardW = 160;
       int cardH = 140;
       int gap = 30;

       boolean blueFull = ClientData.BLUE_PLAYER_COUNT >= ClientData.RED_PLAYER_COUNT + 2;
       boolean redFull = ClientData.RED_PLAYER_COUNT >= ClientData.BLUE_PLAYER_COUNT + 2;

       // Title
       gui.drawCenteredString(this.font, PWPTheme.Icons.SWORDS + " " + Component.translatable("gui.pwpwarfare.team_select.title").getString(),
          centerX, 20, PWPTheme.Colors.TEXT_ACCENT);
       gui.fill(centerX - 80, 30, centerX + 80, 31, PWPTheme.Colors.ACCENT);

       // Blue team card
       int blueX = centerX - gap - cardW;
       int blueY = centerY - cardH / 2;
       boolean isHoveringBlue = !blueFull && mouseX >= blueX && mouseX <= blueX + cardW && mouseY >= blueY && mouseY <= blueY + cardH;
       int blueBg = blueFull ? PWPTheme.Colors.SURFACE : (isHoveringBlue ? PWPTheme.Colors.SURFACE_LIGHT : PWPTheme.Colors.SURFACE);
       int blueBorder = blueFull ? PWPTheme.Colors.BORDER : (isHoveringBlue ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.BORDER_ACCENT);

       gui.fill(blueX, blueY, blueX + cardW, blueY + cardH, blueBg);
       gui.fill(blueX, blueY, blueX + cardW, blueY + 1, blueBorder);
       gui.fill(blueX, blueY + cardH - 1, blueX + cardW, blueY + cardH, blueBorder);
       gui.fill(blueX, blueY, blueX + 1, blueY + cardH, blueBorder);
       gui.fill(blueX + cardW - 1, blueY, blueX + cardW, blueY + cardH, blueBorder);

       String blueTeamName = ClientData.customBlueName;
       if (ClientData.BLUE_FACTION != null && !ClientData.BLUE_FACTION.equals("none") && !ClientData.BLUE_FACTION.equals("bluefor")) {
          blueTeamName = ClientData.BLUE_FACTION.replace("_", " ").toUpperCase();
       }
       gui.drawCenteredString(this.font, Component.literal(blueTeamName).withStyle(ChatFormatting.BLUE),
          blueX + cardW / 2, blueY + 8, PWPTheme.Colors.TEAM_BLUE);

       ResourceLocation blueFlag = this.getFlagTexture(ClientData.BLUE_FACTION);
       if (blueFlag != null) {
          float bAlpha = blueFull ? 0.3F : 1.0F;
          RenderSystem.setShaderColor(bAlpha, bAlpha, bAlpha, bAlpha);
          RenderSystem.enableBlend();
          RenderSystem.defaultBlendFunc();
          gui.blit(blueFlag, blueX + (cardW - 80) / 2, blueY + 22, 0.0F, 0.0F, 80, 45, 80, 45);
          RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
       } else {
          gui.fill(blueX + (cardW - 80) / 2, blueY + 22, blueX + (cardW - 80) / 2 + 80, blueY + 22 + 45,
             blueFull ? 0x66404040 : PWPTheme.Colors.TEAM_BLUE_DARK);
       }

       String blueJoinText = blueFull ? "TOO MANY PLAYERS" : "JOIN";
       int blueJoinColor = blueFull ? PWPTheme.Colors.TEXT_DIM : (isHoveringBlue ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_ACCENT);
       gui.drawCenteredString(this.font, Component.literal(blueJoinText).withStyle(ChatFormatting.GOLD),
          blueX + cardW / 2, blueY + 78, blueJoinColor);

       gui.drawCenteredString(this.font, Component.literal(ClientData.BLUE_PLAYER_COUNT + " players").withStyle(blueFull ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY),
          blueX + cardW / 2, blueY + 92, blueFull ? PWPTheme.Colors.TEXT_DIM : PWPTheme.Colors.TEXT_SECONDARY);

       // Red team card
       int redX = centerX + gap;
       int redY = centerY - cardH / 2;
       boolean isHoveringRed = !redFull && mouseX >= redX && mouseX <= redX + cardW && mouseY >= redY && mouseY <= redY + cardH;
       int redBg = redFull ? PWPTheme.Colors.SURFACE : (isHoveringRed ? PWPTheme.Colors.SURFACE_LIGHT : PWPTheme.Colors.SURFACE);
       int redBorder = redFull ? PWPTheme.Colors.BORDER : (isHoveringRed ? PWPTheme.Colors.TEAM_RED : PWPTheme.Colors.BORDER_ACCENT);

       gui.fill(redX, redY, redX + cardW, redY + cardH, redBg);
       gui.fill(redX, redY, redX + cardW, redY + 1, redBorder);
       gui.fill(redX, redY + cardH - 1, redX + cardW, redY + cardH, redBorder);
       gui.fill(redX, redY, redX + 1, redY + cardH, redBorder);
       gui.fill(redX + cardW - 1, redY, redX + cardW, redY + cardH, redBorder);

       String redTeamName = ClientData.customRedName;
       if (ClientData.RED_FACTION != null && !ClientData.RED_FACTION.equals("none") && !ClientData.RED_FACTION.equals("redfor")) {
          redTeamName = ClientData.RED_FACTION.replace("_", " ").toUpperCase();
       }
       gui.drawCenteredString(this.font, Component.literal(redTeamName).withStyle(ChatFormatting.RED),
          redX + cardW / 2, redY + 8, PWPTheme.Colors.TEAM_RED);

       ResourceLocation redFlag = this.getFlagTexture(ClientData.RED_FACTION);
       if (redFlag != null) {
          float rAlpha = redFull ? 0.3F : 1.0F;
          RenderSystem.setShaderColor(rAlpha, rAlpha, rAlpha, rAlpha);
          RenderSystem.enableBlend();
          RenderSystem.defaultBlendFunc();
          gui.blit(redFlag, redX + (cardW - 80) / 2, redY + 22, 0.0F, 0.0F, 80, 45, 80, 45);
          RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
       } else {
          gui.fill(redX + (cardW - 80) / 2, redY + 22, redX + (cardW - 80) / 2 + 80, redY + 22 + 45,
             redFull ? 0x66404040 : PWPTheme.Colors.TEAM_RED_DARK);
       }

       String redJoinText = redFull ? "TOO MANY PLAYERS" : "JOIN";
       int redJoinColor = redFull ? PWPTheme.Colors.TEXT_DIM : (isHoveringRed ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_ACCENT);
       gui.drawCenteredString(this.font, Component.literal(redJoinText).withStyle(ChatFormatting.GOLD),
          redX + cardW / 2, redY + 78, redJoinColor);

       gui.drawCenteredString(this.font, Component.literal(ClientData.RED_PLAYER_COUNT + " players").withStyle(redFull ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY),
          redX + cardW / 2, redY + 92, redFull ? PWPTheme.Colors.TEXT_DIM : PWPTheme.Colors.TEXT_SECONDARY);

       super.render(gui, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
       if (button == 0) {
          int centerX = this.width / 2;
          int centerY = this.height / 2;
          int cardW = 160;
          int cardH = 140;
          int gap = 30;
          boolean blueFull = ClientData.BLUE_PLAYER_COUNT >= ClientData.RED_PLAYER_COUNT + 2;
          boolean redFull = ClientData.RED_PLAYER_COUNT >= ClientData.BLUE_PLAYER_COUNT + 2;

          int blueX = centerX - gap - cardW;
          int blueY = centerY - cardH / 2;
          if (!blueFull && mouseX >= blueX && mouseX <= blueX + cardW && mouseY >= blueY && mouseY <= blueY + cardH) {
             ClientData.teamSelectSent = true;
             ClientData.teamSelectSentTime = System.currentTimeMillis();
             PacketHandler.INSTANCE.sendToServer(new PacketTeamSelect("BLUE"));
             this.onClose();
             return true;
          }

          int redX = centerX + gap;
          int redY = centerY - cardH / 2;
          if (!redFull && mouseX >= redX && mouseX <= redX + cardW && mouseY >= redY && mouseY <= redY + cardH) {
             ClientData.teamSelectSent = true;
             ClientData.teamSelectSentTime = System.currentTimeMillis();
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

    public void onClose() {
       super.onClose();
    }

   private ResourceLocation getFlagTexture(String faction) {
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
      } else {
         return null;
      }
   }
}
