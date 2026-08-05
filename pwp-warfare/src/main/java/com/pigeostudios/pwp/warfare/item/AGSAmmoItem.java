package com.pigeostudios.pwp.warfare.item;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// Предмет — коробка с патронами для АГС-30
// Хранит количество гранат в NBT и используется для перезарядки гранатомёта
public class AGSAmmoItem extends Item {
   public static final int MAX_AMMO = 30;

   public AGSAmmoItem() {
      super(new Properties().stacksTo(1));
   }

   public static int getAmmo(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("Ammo") ? tag.getInt("Ammo") : 30;
   }

   public static void setAmmo(ItemStack stack, int amount) {
      if (amount > 30) {
         amount = 30;
      }

      if (amount < 0) {
         amount = 0;
      }

      stack.getOrCreateTag().putInt("Ammo", amount);
   }

   public boolean isBarVisible(ItemStack stack) {
      return true;
   }

   public int getBarWidth(ItemStack stack) {
      return Math.round(13.0F * (getAmmo(stack) / 30.0F));
   }

   public int getBarColor(ItemStack stack) {
      return Mth.hsvToRgb(Math.max(0.0F, getAmmo(stack) / 30.0F) / 3.0F, 1.0F, 1.0F);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.literal("Гранаты: " + getAmmo(stack) + " / 30"));
   }
}
