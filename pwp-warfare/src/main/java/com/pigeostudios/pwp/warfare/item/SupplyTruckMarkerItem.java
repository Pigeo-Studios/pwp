package com.pigeostudios.pwp.warfare.item;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// Предмет-маркер для вызова грузовика снабжения
// Содержит информацию о команде, типе техники и штрафе за потерю
public class SupplyTruckMarkerItem extends Item {
   private final String team;
   private final int penalty;
   private final String vehicleType;
   private final int maxMats;

   public SupplyTruckMarkerItem(String team, int penalty, String vehicleType, int maxMats) {
      super(new Properties().stacksTo(1));
      this.team = team;
      this.penalty = penalty;
      this.vehicleType = vehicleType;
      this.maxMats = maxMats;
   }

   public String getTeam() {
      return this.team;
   }

   public int getPenalty() {
      return this.penalty;
   }

   public String getVehicleType() {
      return this.vehicleType;
   }

   public int getMaxMats() {
      return this.maxMats;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      ChatFormatting color = this.team.equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
      tooltip.add(Component.literal("Team: " + this.team).withStyle(color));
      tooltip.add(Component.literal("Type: " + this.vehicleType).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.literal("Max " + WarfareConfig.SUPPLY_TRUCK_CRATES.get() + " Crates. Press X to drop.").withStyle(ChatFormatting.YELLOW));
      if (this.maxMats > 0) {
         tooltip.add(Component.literal("Contains: " + this.maxMats + " Materials").withStyle(ChatFormatting.YELLOW));
      }

      tooltip.add(Component.literal("Loss Penalty: -" + this.penalty + " Tickets").withStyle(ChatFormatting.RED));
      tooltip.add(Component.literal("thank exactly").withStyle(ChatFormatting.DARK_PURPLE).withStyle(ChatFormatting.ITALIC));
   }
}
