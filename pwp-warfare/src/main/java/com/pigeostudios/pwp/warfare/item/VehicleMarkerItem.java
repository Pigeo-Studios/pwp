package com.pigeostudios.pwp.warfare.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// Предмет-маркер для вызова техники
// Содержит информацию о команде, типе, штрафе за потерю и количестве материалов
public class VehicleMarkerItem extends Item {
   private final String team;
   private final String type;
   private final int penalty;
   private final int maxMats;

   public VehicleMarkerItem(String team, String type, int penalty, int maxMats) {
      super(new Properties().stacksTo(1));
      this.team = team;
      this.type = type;
      this.penalty = penalty;
      this.maxMats = maxMats;
   }

   public String getTeam() {
      return this.team;
   }

   public String getType() {
      return this.type;
   }

   public int getPenalty() {
      return this.penalty;
   }

   public int getMaxMats() {
      return this.maxMats;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      ChatFormatting color = this.team.equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
      tooltip.add(Component.literal("Team: " + this.team).withStyle(color));
      tooltip.add(Component.literal("Type: " + this.type).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.literal("Loss Penalty: -" + this.penalty + " Tickets").withStyle(ChatFormatting.DARK_RED));
      if (this.maxMats > 0) {
         tooltip.add(Component.literal("Contains: " + this.maxMats + " Materials").withStyle(ChatFormatting.YELLOW));
      }
   }
}
