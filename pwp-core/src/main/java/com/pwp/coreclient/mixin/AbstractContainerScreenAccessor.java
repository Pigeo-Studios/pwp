package com.pwp.coreclient.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Подпись инвентаря игрока у контейнеров (поля protected, публичных геттеров в 1.20.1 нет). */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("inventoryLabelX")
    int pwp_getInventoryLabelX();

    @Accessor("inventoryLabelY")
    int pwp_getInventoryLabelY();

    @Accessor("playerInventoryTitle")
    Component pwp_getPlayerInventoryTitle();
}
