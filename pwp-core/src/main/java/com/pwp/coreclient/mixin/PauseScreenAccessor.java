package com.pwp.coreclient.mixin;

import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Доступ к {@code PauseScreen.showPauseMenu} (публичного геттера в 1.20.1 нет; F3+Esc = false). */
@Mixin(PauseScreen.class)
public interface PauseScreenAccessor {

    @Accessor("showPauseMenu")
    boolean getShowPauseMenu();
}
