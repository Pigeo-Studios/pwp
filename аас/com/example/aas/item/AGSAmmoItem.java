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

public class AGSAmmoItem
extends Item {
    public static final int MAX_AMMO = 30;

    public AGSAmmoItem() {
        super(new Item.Properties().m_41487_(1));
    }

    public static int getAmmo(ItemStack stack) {
        CompoundTag tag = stack.m_41783_();
        if (tag == null || !tag.m_128441_("Ammo")) {
            return 30;
        }
        return tag.m_128451_("Ammo");
    }

    public static void setAmmo(ItemStack stack, int amount) {
        if (amount > 30) {
            amount = 30;
        }
        if (amount < 0) {
            amount = 0;
        }
        stack.m_41784_().m_128405_("Ammo", amount);
    }

    public boolean m_142522_(ItemStack stack) {
        return true;
    }

    public int m_142158_(ItemStack stack) {
        return Math.round(13.0f * ((float)AGSAmmoItem.getAmmo(stack) / 30.0f));
    }

    public int m_142159_(ItemStack stack) {
        return Mth.m_14169_((float)(Math.max(0.0f, (float)AGSAmmoItem.getAmmo(stack) / 30.0f) / 3.0f), (float)1.0f, (float)1.0f);
    }

    public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((Component)Component.m_237113_((String)("Grenades: " + AGSAmmoItem.getAmmo(stack) + " / 30")));
    }
}

