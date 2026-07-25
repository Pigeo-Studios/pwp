package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import com.pwp.coreclient.gui.components.PWPButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

public class WarfareConfigScreen extends Screen {
   private final Screen parentScreen;
   private EditBox hubSpawnCostBox;
   private EditBox hubResupplyBox;
   private EditBox maxHubsBox;
   private EditBox downedTimeBox;
   private EditBox hubDistBox;
   private EditBox rallyDistBox;
   private EditBox hubSoundRadBox;
   private EditBox voteTimeBox;
   private EditBox hubBlockRadBox;
   private EditBox rallyBlockRadBox;
   private EditBox hubBuildRadBox;
   private EditBox votePercentBox;
   private EditBox blueNameBox;
   private EditBox redNameBox;
   private EditBox reviveItemBox;
   private ForgeSlider diggingSpeedSlider;

   public WarfareConfigScreen(Screen parentScreen) {
      super(Component.translatable("gui.pwpwarfare.config.title"));
      this.parentScreen = parentScreen;
   }

   protected void init() {
      super.init();
      int cx = this.width / 2;
      int y = 20;
      int col1 = cx - 160;
      int col2 = cx + 10;

      // Section: Toggles
      guiLabel("GAME RULES", cx - 160, y - 2);

      this.addToggle(col1, y, 150, "gui.pwpwarfare.config.ags_destruct", WarfareConfig.AGS_PROJECTILE_DESTRUCTION);
      this.addToggle(col2, y, 150, "gui.pwpwarfare.config.ammo_explosion", WarfareConfig.AMMO_STACK_DESTRUCTION);
      y += 22;
      this.addToggle(col1, y, 150, "gui.pwpwarfare.config.prevent_break", WarfareConfig.PREVENT_BLOCK_BREAKING);
      this.addToggle(col2, y, 150, "gui.pwpwarfare.config.allow_break_def", WarfareConfig.ALLOW_BREAKING_DEFENSES);
      y += 22;
      this.addToggle(col1, y, 150, "gui.pwpwarfare.config.prevent_drops", WarfareConfig.PREVENT_ALL_ITEM_DROPS);
      this.addToggle(col2, y, 150, "gui.pwpwarfare.config.low_tix_siren", WarfareConfig.LOW_TICKETS_SIREN);
      y += 22;
      this.addToggle(col1, y, 150, "gui.pwpwarfare.config.auto_sl_radio", WarfareConfig.AUTO_GIVE_SL_RADIO);
      this.addToggle(col2, y, 150, "gui.pwpwarfare.config.fob_needs_crate", WarfareConfig.HUB_PLACEMENT_REQUIRES_CRATE);
      y += 22;
      this.addToggle(col1, y, 150, "gui.pwpwarfare.config.require_officer", WarfareConfig.REQUIRE_OFFICER_FOR_SL);
      this.addToggle(col2, y, 150, "gui.pwpwarfare.config.lock_enemy_veh", WarfareConfig.PREVENT_ENEMY_VEHICLE_ENTRY);
      y += 22;
      this.addToggle(col1, y, 150, "gui.pwpwarfare.config.spec_driving", WarfareConfig.REQUIRE_SPECIALIST_TO_DRIVE);
      this.addToggle(col2, y, 150, "gui.pwpwarfare.config.block_veh_inv", WarfareConfig.PREVENT_VEHICLE_INVENTORY_ACCESS);
      y += 22;
      this.addToggle(col1, y, 150, "gui.pwpwarfare.config.enable_medic", WarfareConfig.ENABLE_KNOCKOUT);
      this.addToggle(col2, y, 150, "gui.pwpwarfare.config.base_healing", WarfareConfig.MAIN_SUPPLY_HEALING);

      y += 28;

      // Section: Slider
      guiLabel("MULTIPLIERS", cx - 160, y - 2);
      this.diggingSpeedSlider = new ForgeSlider(
         cx - 160, y, 320, 20,
         Component.translatable("gui.pwpwarfare.config.dig_speed"),
         Component.literal("x"),
         0.1, 5.0,
         (Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get(),
         0.1, 1, true
      );
      this.addRenderableWidget(this.diggingSpeedSlider);
      y += 30;

      // Section: Numeric fields
      guiLabel("NUMERIC SETTINGS", cx - 160, y - 2);
      int x1 = cx - 175;
      int x2 = cx - 85;
      int x3 = cx + 5;
      int x4 = cx + 95;
      this.hubSpawnCostBox = this.createIntBox(x1, y, (Integer)WarfareConfig.HUB_SPAWN_MATERIAL_COST.get());
      this.hubResupplyBox = this.createIntBox(x2, y, (Integer)WarfareConfig.HUB_RESUPPLY_COST.get());
      this.maxHubsBox = this.createIntBox(x3, y, (Integer)WarfareConfig.MAX_HUBS_PER_TEAM.get());
      this.downedTimeBox = this.createIntBox(x4, y, (Integer)WarfareConfig.MAX_DOWNED_TIME_SECONDS.get());
      y += 30;
      this.hubDistBox = this.createIntBox(x1, y, (Integer)WarfareConfig.MIN_HUB_DISTANCE.get());
      this.rallyDistBox = this.createIntBox(x2, y, (Integer)WarfareConfig.MIN_RALLY_POINT_DISTANCE.get());
      this.hubSoundRadBox = this.createIntBox(x3, y, (Integer)WarfareConfig.HUB_SOUND_RADIUS.get());
      this.voteTimeBox = this.createIntBox(x4, y, (Integer)WarfareConfig.VOTE_AUTO_START_TIME.get());
      y += 30;
      this.hubBlockRadBox = this.createIntBox(x1, y, (Integer)WarfareConfig.HUB_BLOCK_RADIUS.get());
      this.rallyBlockRadBox = this.createIntBox(x2, y, (Integer)WarfareConfig.RALLY_BLOCK_RADIUS.get());
      this.hubBuildRadBox = this.createIntBox(x3, y, (Integer)WarfareConfig.HUB_BUILD_RADIUS.get());
      this.votePercentBox = this.createIntBox(x4, y, (Integer)WarfareConfig.VOTE_REQUIRED_PERCENTAGE.get());
      y += 30;

      // Section: Text fields
      guiLabel("TEAM SETTINGS", cx - 160, y - 2);
      this.blueNameBox = this.createStringBox(cx - 160, y, 145, (String)WarfareConfig.BLUE_TEAM_CUSTOM_NAME.get());
      this.redNameBox = this.createStringBox(cx + 10, y, 145, (String)WarfareConfig.RED_TEAM_CUSTOM_NAME.get());
      y += 28;
      this.reviveItemBox = this.createStringBox(cx - 160, y, 300, (String)WarfareConfig.REVIVE_ITEM.get());

      this.addRenderableWidget(new PWPButton(cx - 80, this.height - 28, 160, 22, Component.translatable("gui.pwpwarfare.config.save"), b -> {
         this.saveValues();
         this.onClose();
      }, PWPButton.Style.ACCENT));
   }

   private void guiLabel(String text, int x, int y) {
      addRenderableWidget(new PWPButton(x, y, 1, 1, Component.literal(""), b -> {}, PWPButton.Style.PRIMARY));
   }

   private void addToggle(int x, int y, int w, String labelKey, BooleanValue val) {
      this.addRenderableWidget(CycleButton.onOffBuilder((Boolean)val.get()).create(x, y, w, 20, Component.translatable(labelKey), (b, v) -> val.set(v)));
   }

   private EditBox createIntBox(int x, int y, int val) {
      EditBox box = new EditBox(PWPTheme.Fonts.display(), x, y, 65, 18, Component.empty());
      box.setValue(String.valueOf(val));
      box.setFilter(s -> s.matches("\\d*"));
      this.addRenderableWidget(box);
      return box;
   }

   private EditBox createStringBox(int x, int y, int w, String val) {
      EditBox box = new EditBox(PWPTheme.Fonts.display(), x, y, w, 18, Component.empty());
      box.setValue(val);
      this.addRenderableWidget(box);
      return box;
   }

   private void saveValues() {
      try {
         WarfareConfig.DIGGING_SPEED_MULTIPLIER.set(this.diggingSpeedSlider.getValue());
         WarfareConfig.HUB_SPAWN_MATERIAL_COST.set(Integer.parseInt(this.hubSpawnCostBox.getValue()));
         WarfareConfig.HUB_RESUPPLY_COST.set(Integer.parseInt(this.hubResupplyBox.getValue()));
         WarfareConfig.MAX_HUBS_PER_TEAM.set(Integer.parseInt(this.maxHubsBox.getValue()));
         WarfareConfig.MAX_DOWNED_TIME_SECONDS.set(Integer.parseInt(this.downedTimeBox.getValue()));
         WarfareConfig.MIN_HUB_DISTANCE.set(Integer.parseInt(this.hubDistBox.getValue()));
         WarfareConfig.MIN_RALLY_POINT_DISTANCE.set(Integer.parseInt(this.rallyDistBox.getValue()));
         WarfareConfig.HUB_SOUND_RADIUS.set(Integer.parseInt(this.hubSoundRadBox.getValue()));
         WarfareConfig.VOTE_AUTO_START_TIME.set(Integer.parseInt(this.voteTimeBox.getValue()));
         WarfareConfig.HUB_BLOCK_RADIUS.set(Integer.parseInt(this.hubBlockRadBox.getValue()));
         WarfareConfig.RALLY_BLOCK_RADIUS.set(Integer.parseInt(this.rallyBlockRadBox.getValue()));
         WarfareConfig.HUB_BUILD_RADIUS.set(Integer.parseInt(this.hubBuildRadBox.getValue()));
         WarfareConfig.VOTE_REQUIRED_PERCENTAGE.set(Integer.parseInt(this.votePercentBox.getValue()));
         WarfareConfig.BLUE_TEAM_CUSTOM_NAME.set(this.blueNameBox.getValue());
         WarfareConfig.RED_TEAM_CUSTOM_NAME.set(this.redNameBox.getValue());
         WarfareConfig.REVIVE_ITEM.set(this.reviveItemBox.getValue());
         WarfareConfig.SPEC.save();
      } catch (Exception var2) {
      }
   }

   public void render(GuiGraphics gui, int mx, int my, float pt) {
      this.renderBackground(gui);
      int cx = this.width / 2;
      gui.drawCenteredString(PWPTheme.Fonts.display(), Component.literal(this.title.getString()), cx, 8, PWPTheme.Colors.TEXT_ACCENT);

      int x1 = cx - 175;
      int x2 = cx - 85;
      int x3 = cx + 5;
      int x4 = cx + 95;
      int color = PWPTheme.Colors.TEXT_SECONDARY;
      int ly1 = 158;
      int ly2 = 188;
      int ly3 = 218;

      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.fob_mat"), x1, ly1, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.resup"), x2, ly1, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.max_fob"), x3, ly1, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.nok_sec"), x4, ly1, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.fob_dist"), x1, ly2, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.ral_dist"), x2, ly2, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.snd_rad"), x3, ly2, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.vote_min"), x4, ly2, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.fob_blk"), x1, ly3, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.ral_blk"), x2, ly3, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.bld_rad"), x3, ly3, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.vote_percent"), x4, ly3, color);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.blue_team_name"), cx - 160, 248, PWPTheme.Colors.TEAM_BLUE);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.red_team_name"), cx + 10, 248, PWPTheme.Colors.TEAM_RED);
      gui.drawString(PWPTheme.Fonts.display(), Component.translatable("gui.pwpwarfare.config.revive_item_id"), cx - 160, 278, color);
      super.render(gui, mx, my, pt);
   }
}
