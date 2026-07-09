/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 */
package com.example.aas.item;

import com.example.aas.client.ClientHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class KitSetupItem
extends Item {
    public KitSetupItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && player.isCreative()) {
            DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> ClientHooks::openKitTeamSelect);
        }
        return InteractionResultHolder.success((Object)player.getItemInHand(hand));
    }
}

