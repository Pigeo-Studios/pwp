package com.pwp.coreclient.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Позиция мыши для куклы игрока ({@code xMouse/yMouse}; Curios тоже пишет в эти поля). */
@Mixin(InventoryScreen.class)
public interface InventoryScreenAccessor {

    @Accessor("xMouse")
    float pwp_getXMouse();

    @Accessor("yMouse")
    float pwp_getYMouse();
}
