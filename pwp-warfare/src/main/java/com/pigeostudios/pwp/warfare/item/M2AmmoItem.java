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

// Предмет — коробка с патронами для M2 Browning
// Хранит количество патронов в NBT и используется для перезарядки пулемёта
public class M2AmmoItem extends Item {
   public static final int MAX_AMMO = 200;

   public M2AmmoItem() {
      super(new Properties().stacksTo(1));
   }

   public static int getAmmo(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("Ammo") ? tag.getInt("Ammo") : 200;
   }

   public static void setAmmo(ItemStack stack, int amount) {
      if (amount > 200) {
         amount = 200;
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
      return Math.round(13.0F * (getAmmo(stack) / 200.0F));
   }

   public int getBarColor(ItemStack stack) {
      return Mth.hsvToRgb(Math.max(0.0F, getAmmo(stack) / 200.0F) / 3.0F, 1.0F, 1.0F);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.literal("Ammo: " + getAmmo(stack) + " / 200"));
   }
}
