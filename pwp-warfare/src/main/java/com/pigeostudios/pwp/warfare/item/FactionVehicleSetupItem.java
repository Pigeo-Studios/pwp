package com.pigeostudios.pwp.warfare.item;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class FactionVehicleSetupItem extends Item {
    public FactionVehicleSetupItem() {
        super(new Properties().stacksTo(1));
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && player.isCreative()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openFactionVehicleSelect);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
