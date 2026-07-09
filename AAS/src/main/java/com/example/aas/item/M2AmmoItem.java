/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package com.example.aas.item;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class M2AmmoItem
extends Item {
    static final public int MAX_AMMO = 200;

    public M2AmmoItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static int getAmmo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Ammo")) {
            return 200;
        }
        return tag.getInt("Ammo");
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
        return Math.round(13.0f * ((float)M2AmmoItem.getAmmo(stack) / 200.0f));
    }

    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float)(Math.max(0.0f, (float)M2AmmoItem.getAmmo(stack) / 200.0f) / 3.0f), 1.0f, 1.0f);
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((Component)Component.literal((String)("Ammo: " + M2AmmoItem.getAmmo(stack) + " / 200")));
    }
}

